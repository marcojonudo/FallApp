package com.example.fulltest;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;


public class FallService extends Service implements SensorEventListener {

    private static Service instance = null;

    private SensorManager accelerometerSensor;
    private HashMap<String, Action> actionMap = new HashMap<String, Action>();

    private String[] states;
    private String state;
    private Action[] actions;


    private int constantIterations = 0, totalConstantIterations = 0, zIterations = 0, totalZIterations = 0;

    private boolean runAction = true;
    private boolean locationFound = false;
    private boolean GPSEnabled = false, networkEnabled = false, locationServiceEnabled = false;
    private double latitude, longitude;
    private boolean firstMessageSent = false, positionSent = false, fineLocation = false;
    private int tableCounter = 0;
    private boolean onTable = false;
    private Vibrator v;

    private int freeFallToFalseCounter = 0;


    private String userName;
    private String[] telNumbers = new String[]{"", "", "", "", ""};
    private double sensibility;

    private static double valueX = 0, valueY = 0, valueZ = 0;

    private boolean freeFall;

    private Vector<Double> moduleVector = new Vector<Double>();
    private Vector<Double> zVector = new Vector<Double>();
    private Vector<Double> whileImpactZVector = new Vector<Double>();

    Uri ringtone;
    Ringtone ring;

    ConnectivityManager connectivity;
    NetworkInfo activeNetwork;
    TelephonyManager tel;

    /*----------------------CONSTANTS----------------------*/

    private final double G = 9.80665;
    private final double BASE_FREE_FALL_VALUE = 0.1;
    private final double BASE_MODULE_VALUE = 1.5;
    private final double BASE_CONSTANCY_VALUE = 0.01;
    private final double BASE_Z_CONSTANCY_VALUE = 0.96;
    private final long WAIT_WHILE_IMPACT_TIME = 2000;
    private final int NO_SENSOR_CHANGE_INTERVAL = 40;
    private final long CONSTANT_TIMER_TIME = 3000;
    private final double ESTIMATED_ZVECTOR_SIZE = CONSTANT_TIMER_TIME / NO_SENSOR_CHANGE_INTERVAL;
    private final long TRY_SEND_SMS_TIME = 900000;
    private final long TRY_SEND_SMS_INTERVAL = 20000;
    private final long RING_ALERT_TIME = 1200000;
    private final long RING_ALERT_INTERVAL = 30000;

    private final int ON_TABLE_COUNTER_THRESHOLD = 650;
    private final double ON_TABLE_THRESHOLD = 0.98;
    private final double NOT_ON_TABLE_THRESHOLD = 0.95;
    private final int FREE_FALL_TO_FALSE_THRESHOLD = 1000;


    private double FREE_FALL_THRESHOLD = BASE_FREE_FALL_VALUE + (sensibility*0.2);
    private double IMPACT_THRESHOLD = BASE_MODULE_VALUE + (1-sensibility);
    private double CONSTANCY_THRESHOLD = BASE_CONSTANCY_VALUE + (sensibility*0.01);
    private double Z_CONSTANCY_THRESHOLD = BASE_Z_CONSTANCY_VALUE + (sensibility*0.02);
    /*-----------------------------------------------------------------------------------------------------*/

    public static boolean isRunning() {
        return instance != null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        states = new String[]{"waiting", "omitting", "constant"};
        actions = new Action[]{waitingAction, omittingAction, constantAction};

        for (int i=0; i<states.length; i++) {
            actionMap.put(states[i], actions[i]);
        }

        state = states[0];

        String title = "Servicio iniciado";
        String text = "¡La detección de caídas está activada!";
        notification("persistant", title, text, null, 0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Bundle variables = intent.getExtras();

        if (variables != null) {
            if (variables.getString("sendAlert") == null) {
                getVariables(variables);
            }
            else {
                alert();
            }
        }

        if (userName != null) {
            accelerometerSensor = (SensorManager) getSystemService(SENSOR_SERVICE);
            accelerometerSensor.registerListener(this, accelerometerSensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
        }

        return Service.START_REDELIVER_INTENT;
    }


    private void getVariables(Bundle variables) {
        SharedPreferences parametersPreferences = getSharedPreferences("userParameters", Context.MODE_PRIVATE);
        SharedPreferences.Editor parametersPreferencesEditor = parametersPreferences.edit();

        //Si userName!=null, los parametros se envian por primera vez desde el index
        if (variables.getString("userName") != null) {
            parametersPreferencesEditor.putString("userName", variables.getString("userName"));
            parametersPreferencesEditor.commit();

            userName = variables.getString("userName");

            String number;
            for (int i=0; i<variables.size()-2; i++) {
                number = "number" + String.valueOf(i+1);
                parametersPreferencesEditor.putString(number, variables.getString(number));
                parametersPreferencesEditor.commit();

                telNumbers[i] = variables.getString("number" + String.valueOf(i+1));
            }

            parametersPreferencesEditor.putString("sensibility", variables.getString("rangeValue"));
            parametersPreferencesEditor.commit();

            sensibility = Double.parseDouble(variables.getString("rangeValue"))/100;
        }
        else if (variables.getString("newSensibility") != null) {
            parametersPreferencesEditor.putString("sensibility", variables.getString("newSensibility"));
            parametersPreferencesEditor.commit();

            sensibility = Integer.parseInt(variables.getString("newSensibility"));
        }
        else {
            String number;
            for (int i=0; i<variables.size(); i++) {
                number = "number" + String.valueOf(i+1);
                parametersPreferencesEditor.putString(number, variables.getString(number));
                parametersPreferencesEditor.commit();

                telNumbers[i] = variables.getString("number" + String.valueOf(i+1));
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        accelerometerSensor.unregisterListener(this);

        String title = "Servicio detenido";
        String text = "¡La detección de caídas se ha desactivado!";
        notification("expandable", title, text, null, 0);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }



    /*----------------------SENSOR----------------------*/

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            processValues(event);
        }
    }

    private void processValues(SensorEvent event) {
        storeValues(event);

        if (runAction) {
            actionMap.get(state).runAction();
        }
        else {
            whileImpactZVector.add(Math.abs(valueZ));
        }
    }

    private void storeValues(SensorEvent event) {
        valueX = event.values[0] / G;
        valueY = event.values[1] / G;
        valueZ = event.values[2] / G;
    }



    /*----------------------STATE ACTIONS----------------------*/

    Action waitingAction = new Action() {

        @Override
        public void runAction() {

            checkOnTable();

            double module = calculateModule(valueX, valueY, valueZ);

            if (freeFall) {
                freeFallToFalseCounter++;
                if (freeFallToFalseCounter > FREE_FALL_TO_FALSE_THRESHOLD) {
                    freeFall = false;
                    freeFallToFalseCounter = 0;
                }
            }

            if (module < FREE_FALL_THRESHOLD) {
                freeFall = true;
                freeFallToFalseCounter = 0;
            }
            else if (module > IMPACT_THRESHOLD) {
                state = states[1];
            }

        }

    };


    private void checkOnTable() {
        if (!onTable) {
            if (valueZ > ON_TABLE_THRESHOLD) {

                tableCounter++;

                if (tableCounter > ON_TABLE_COUNTER_THRESHOLD) {
                    String title = "¡Smartphone inmóvil!";
                    String expandableText = "Se ha detectado que el smartphone está inmóvil. ¡Acuérdate de llevarlo siempre encima!";
                    notification("expandable", title, expandableText, new long[]{0, 300}, 1);

                    onTable = true;
                }
            }
            else {
                tableCounter = 0;
            }
        }
        else {
            if (Math.abs(valueZ) < NOT_ON_TABLE_THRESHOLD) {
                onTable = false;
                tableCounter = 0;
            }
        }
    }

    Action omittingAction = new Action() {

        @Override
        public void runAction() {
            waitWhileImpact.start();

            runAction = false;
        }

    };

    Action constantAction = new Action() {

        @Override
        public void runAction() {
            double module = calculateModule(valueX, valueY, valueZ);

            moduleVector.add(module);
            zVector.add(valueZ);
        }

    };




    /*----------------------TIMERS----------------------*/

    CountDownTimer waitWhileImpact = new CountDownTimer(WAIT_WHILE_IMPACT_TIME, WAIT_WHILE_IMPACT_TIME) {

        @Override
        public void onTick(long l) {}

        @Override
        public void onFinish() {
            state = states[2];

            runAction = true;

            constantTimer.start();
        }
    };

    CountDownTimer constantTimer = new CountDownTimer(CONSTANT_TIMER_TIME, CONSTANT_TIMER_TIME) {

        @Override
        public void onTick(long l) {}

        @Override
        public void onFinish() {
            boolean motionless, zConstancy = false;
            /*
            Si el tamaño del vector es inferior a esta cifra, quiere decir que el acelerómetro no refrescó los valorse por estar totalmente
            inmóvil. Por lo tanto, se puede considerar motionless = true.
             */
            if (zVector.size() < (ESTIMATED_ZVECTOR_SIZE *0.2)) {
                motionless = true;

                if ((whileImpactZVector.get(whileImpactZVector.size()-1)) > Z_CONSTANCY_THRESHOLD) {
                    zConstancy = true;
                }
            }

            /*
            Si hay valores suficinetes en zVector, quiere decir que o el móvil se estaba moviendo, o sí que refersacaba aunque estuviera inmóvil.
            Por lo tanto, se comprueba.
             */
            else {
                motionless = checkConstancy();

                if (motionless) {
                    zConstancy = checkZAxis();
                }
            }

            runAction = false;

            decisor(freeFall, motionless, zConstancy);

            state = states[0];
        }
    };

    CountDownTimer trySendSMS = new CountDownTimer(TRY_SEND_SMS_TIME, TRY_SEND_SMS_INTERVAL) {

        @Override
        public void onTick(long l) {

            if ((tel.getNetworkOperator() != null)&&(tel.getNetworkOperatorName() == "")) {
                Toast.makeText(getApplicationContext(), "No hay red, esperando cobertura...", Toast.LENGTH_SHORT).show();
            }
            else {
                SmsManager sms = SmsManager.getDefault();

                if (locationServiceEnabled) {

                    if (!firstMessageSent) {
                        String textMessage = "Caída de " + userName + " a las " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
                        for (int i=0; i<telNumbers.length; i++) {
                            if (!telNumbers[i].equals("")) {
                                sms.sendTextMessage(telNumbers[i], null, textMessage, null, null);
                            }
                        }

                        firstMessageSent = true;

                        notification("expandable", "Alerta enviada", "se ha enviado el mensaje informativo", null, 3);
                    }

                    if (locationFound) {
                        String googleMapsLink = "https://www.google.es/maps/@" + latitude +"," + longitude + ",15z";
                        String positionTextMessage = "Latitud: " + latitude + " Longitud: " + longitude + " | Maps: " + googleMapsLink;
                        if (fineLocation) {
                            String textMessage = "POSICION PRECISA - " + positionTextMessage;
                            for (int i=0; i<telNumbers.length; i++) {
                                if (!telNumbers[i].equals("")) {
                                    sms.sendTextMessage(telNumbers[i], null, textMessage, null, null);
                                }
                            }

                            notification("expandable", "Alerta enviada", "Se han enviado los mensajes con la posición precisa", null, 5);

                            //Se ha enviado el mensaje más preciso posible, por lo que se deja de enviar
                            v.cancel();
                            ring.stop();
                            ringAlert.cancel();
                            trySendSMS.cancel();
                            setDefaultValues();

                        }
                        else if (!positionSent) {
                            notification("expandable", "Alerta enviada", "Se han enviado los mensajes con la posición aproximada", null, 4);
                            String textMessage = "POSICION IMPRECISA - " + positionTextMessage;

                            for (int i=0; i<telNumbers.length; i++) {
                                if (!telNumbers[i].equals("")) {
                                    sms.sendTextMessage(telNumbers[i], null, textMessage, null, null);
                                }
                            }

                            positionSent = true;
                        }
                    }
                }
                /*
                Cuando no se tenga la posición, se manda un sms indicando la caida
                 */
                else if (!firstMessageSent){
                    String textMessage = "Caída de " + userName + " a las " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
                    for (int i=0; i<telNumbers.length; i++) {
                        if (!telNumbers[i].equals("")) {
                            sms.sendTextMessage(telNumbers[i], null, textMessage, null, null);
                        }
                    }

                    firstMessageSent = true;

                    notification("expandable", "Alerta enviada", "se ha enviado el mensaje informativo", null, 3);
                }
                else {
                    v.cancel();
                    ring.stop();
                    ringAlert.cancel();
                    trySendSMS.cancel();
                    setDefaultValues();
                }
            }
        }

        @Override
        public void onFinish() {}
    };

    CountDownTimer ringAlert = new CountDownTimer(RING_ALERT_TIME, RING_ALERT_INTERVAL) {

        @Override
        public void onTick(long l) {
            if (v.hasVibrator()) {
                long[] pattern = {0, 500, 1000};
                v.vibrate(pattern, 0);
            }

            if (ring != null) {
                ring.play();
            }

        }

        @Override
        public void onFinish() {
            v.cancel();
            ring.stop();
        }
    };



    /*----------------------FUNCTIONS----------------------*/

    private boolean checkConstancy() {

        constantIterations = totalConstantIterations = 0;

        for (int i=0; i< moduleVector.size(); i++) {
            for (int j=0; j<i; j++) {
                totalConstantIterations++;
                if (Math.abs(moduleVector.get(j) - moduleVector.get(i)) < CONSTANCY_THRESHOLD){
                    constantIterations++;
                }
            }
        }

        if (constantIterations > totalConstantIterations * 0.7) {
            return true;
        }

        return false;
    }

    private boolean checkZAxis() {

        if (zVector.size() < 10) {
            if ((whileImpactZVector.get(whileImpactZVector.size()-1)) > Z_CONSTANCY_THRESHOLD) {
                return true;
            }
        }
        else {

        }

        for (int i=0; i<zVector.size(); i++) {
            totalZIterations++;
            if (Math.abs(zVector.get(i)) > Z_CONSTANCY_THRESHOLD) {
                zIterations++;
            }
        }

        if (zIterations > totalZIterations * 0.85) {
            return true;
        }
        return false;
    }

    private void decisor(boolean freeFall, boolean motionless, boolean zConstancy) {

        //Móvil constante
        if (motionless) {
            //Movil en horizontal
            if (zConstancy) {
                //Si el movil esta en horizontal y ademas hubo caida libre, se entiende que cayó él solo
                if (freeFall) {
                    String title = "¡Móvil al suelo!";
                    String expandableText = "El móvil se ha caido a las " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()) + ". ¡Ten más cuidado!";
                    notification("expandable", title, expandableText, new long[]{0, 200, 500, 200, 500, 200, 500}, 2);
                }
                /*
                Si el movil esta en horizontal pero no hubo caida libre, se pregunta. Una caida sin usuario debería
                implicar caida libre. Un posible casi es que al car el paisano se le caiga el móvil del bolsillo
                 */
                else {
                    launchView("AlertView");
                }
            }
            //Si el movil no esta en horizontal
            else {
                /*
                Si hubo caida libre, tal vez se cayó y quedó apoyado en algún sitio. De todas formas, reúne las suficientes condiciones
                como para, al menos, preguntar. Una buena opcion sería que una vez pasaran los 45 segundos del temporizador Javasript,
                en este caso sí que se envie la alerta, mientras que en el otro no
                */
                if (freeFall) {
                    launchView("AlertView");
                }
                //Si no hubo caida libre, caída del paisano
                else {
                    alert();
                }
            }
        }

        setDefaultValues();
    }

    private void setDefaultValues() {
        runAction = true;

        freeFall = false;

        moduleVector.clear();
        zVector.clear();
        whileImpactZVector.clear();

        locationFound = false;
        GPSEnabled = false;
        fineLocation = false;
        positionSent = false;
        firstMessageSent = false;

        state = states[0];
    }

    private void alert() {
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        ring = RingtoneManager.getRingtone(getApplicationContext(), ringtone);

        ringAlert.start();


        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            GPSEnabled = true;
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            networkEnabled = true;
        }

        if (GPSEnabled || networkEnabled) {
            locationServiceEnabled = true;
            startLocation(locationManager);
        }

        connectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        activeNetwork = connectivity.getActiveNetworkInfo();
        tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        trySendSMS.start();
    }

    private void notification(String type, String title, String text, long[] pattern, int id) {
        Notification notification = null;

        if (type.equals("expandable")) {
            notification = new Notification.Builder(this)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setSmallIcon(R.drawable.icon)
                    .setAutoCancel(true)
                    .setStyle(new Notification.BigTextStyle().bigText(text))
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setVibrate(pattern)
                    .build();
        }
        else if (type.equals("persistant")) {
            notification = new Notification.Builder(this)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setSmallIcon(R.drawable.icon)
                    .setAutoCancel(true)
                    .setOngoing(true)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setVibrate(pattern)
                    .build();
        }
        NotificationManager notificationManager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);
    }

    private void launchView(String view) {
        SharedPreferences preferences = getSharedPreferences("launcherActivity", Context.MODE_PRIVATE);
        SharedPreferences.Editor preferencesEditor = preferences.edit();

        preferencesEditor.putString("launchClass", view);
        preferencesEditor.commit();

        Intent alertViewIntent = new Intent(getApplicationContext(), CordovaApp.class);
        alertViewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(alertViewIntent);
    }

    private void startLocation(LocationManager locationManager) {
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                locationFound = true;

                latitude = location.getLatitude();
                longitude = location.getLongitude();

                if (location.getAccuracy()<10) {
                    fineLocation = true;
                }
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {}

            @Override
            public void onProviderEnabled(String s) {
                String text = "Proveedor de localización " + s + " habilitado";
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onProviderDisabled(String s) {
                String text = "Proveedor de localización " + s + " deshabilitado";
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
            }
        };

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }

    private double calculateModule(double x, double y, double z) {
        return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
    }
}