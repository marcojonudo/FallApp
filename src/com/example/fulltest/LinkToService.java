package com.example.fulltest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LinkToService extends CordovaPlugin {

    private final String USERNAME = "userName",
                    TELNUMBERS = "telNumbers",
                    RANGEVALUE = "rangeValue",
                    NEWSENSIBILITY = "newSensibility",
                    NEWTELNUMBERS = "newTelNumbers";

    private JSONObject argsObject;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        try {
            if (action.equals("startService")) {
                startService(args);
                return true;
            }
            else if (action.equals("getVariables")) {
                getVariables(callbackContext);
                return true;
            }
            else if (action.equals("updateSensibility")) {
                updateSensibility(args);
                return true;
            }
            else if (action.equals("updateTelNumbers")) {
                updateTelNumbers(args, callbackContext);
                return true;
            }
            else if (action.equals("stopService")) {
                stopService();
                return true;
            }
            else if (action.equals("reStartService")) {
                reStartService();
                return true;
            }
            else if (action.equals("sendAlert")) {
                sendAlert(args);
                return true;
            }
            else if (action.equals("returnToMainPage")) {
                launchMainPage();
                return true;
            }
            return false;
        }
        catch (JSONException e) {
            return false;
        }
    }


    private void startService(JSONArray args) {
        Intent fallServiceIntent = new Intent(this.cordova.getActivity().getApplicationContext(), FallService.class);

        boolean argumentsOK = getArguments(fallServiceIntent, args);

        if (!argumentsOK) {
            String text = "Ha habido un problema con los argumentos introducidos. Por favor, actualizalos desde la pagina principal.";
            Toast.makeText(this.cordova.getActivity().getApplicationContext(), text, Toast.LENGTH_LONG).show();
        }

        SharedPreferences JSONPreferences = this.cordova.getActivity().getSharedPreferences("JSONObject", Context.MODE_PRIVATE);
        SharedPreferences.Editor JSONPreferencesEditor = JSONPreferences.edit();

        JSONPreferencesEditor.putString("argsObject", argsObject.toString());
        JSONPreferencesEditor.commit();

        if (!FallService.isRunning()) {
            this.cordova.getActivity().startService(fallServiceIntent);
        }
        else {
            String text = "El servicio ya está en ejecución";
            Toast.makeText(this.cordova.getActivity().getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }

        launchMainPage();
    }

    private boolean getArguments(Intent fallServiceIntent, JSONArray argsArray) {
        try {
            argsObject = argsArray.getJSONObject(0);

            fallServiceIntent.putExtra(USERNAME, argsObject.getString(USERNAME));

            String number;
            for (int i=0; i<argsObject.getJSONArray(TELNUMBERS).length(); i++) {
                number = "number" + String.valueOf(i+1);
                fallServiceIntent.putExtra(number, argsObject.getJSONArray(TELNUMBERS).getJSONObject(i).getString(number));
            }

            fallServiceIntent.putExtra(RANGEVALUE, argsObject.getString(RANGEVALUE));
        }
        catch (JSONException e) {
            return false;
        }
        return true;
    }

    private void getVariables(CallbackContext callbackContext) throws JSONException{
        SharedPreferences JSONPreferences = this.cordova.getActivity().getSharedPreferences("JSONObject", Context.MODE_PRIVATE);

        String stringJSONObject = JSONPreferences.getString("argsObject", "JSONObject");

        if (!stringJSONObject.equals("JSONObject")) {
            JSONObject recoveredArgsObject = new JSONObject(stringJSONObject);
            callbackContext.success(recoveredArgsObject);
        }
    }

    private void updateSensibility(JSONArray args) throws JSONException{
        JSONObject sensibilityJSON = args.getJSONObject(0);

        Intent newSensibilityIntent = new Intent(this.cordova.getActivity().getApplicationContext(), FallService.class);
        newSensibilityIntent.putExtra(NEWSENSIBILITY, sensibilityJSON.getString(NEWSENSIBILITY));

        this.cordova.getActivity().startService(newSensibilityIntent);
    }

    private void updateTelNumbers(JSONArray args, CallbackContext callbackContext) throws JSONException {
        JSONObject telNumbersJSON = args.getJSONObject(0);

        SharedPreferences JSONPreferences = this.cordova.getActivity().getSharedPreferences("JSONObject", Context.MODE_PRIVATE);
        String stringJSONObject = JSONPreferences.getString("argsObject", "JSONObject");
        JSONObject savedJSONObject = new JSONObject(stringJSONObject);

        Intent newTelNumbersIntent = new Intent(this.cordova.getActivity().getApplicationContext(), FallService.class);

        String number;
        for (int i=0; i<telNumbersJSON.getJSONArray(NEWTELNUMBERS).length(); i++) {
            number = "number" + String.valueOf(i+1);
            savedJSONObject.getJSONArray(TELNUMBERS).getJSONObject(i).put(number, telNumbersJSON.getJSONArray(NEWTELNUMBERS).getJSONObject(i).getString(number));
            newTelNumbersIntent.putExtra(number, telNumbersJSON.getJSONArray(NEWTELNUMBERS).getJSONObject(i).getString(number));
        }

        SharedPreferences.Editor JSONPreferencesEditor = JSONPreferences.edit();

        JSONPreferencesEditor.putString("argsObject", savedJSONObject.toString());
        JSONPreferencesEditor.commit();

        this.cordova.getActivity().startService(newTelNumbersIntent);

        getVariables(callbackContext);
    }

    private void stopService() {
        Intent stopServiceIntent = new Intent(this.cordova.getActivity().getApplicationContext(), FallService.class);
        this.cordova.getActivity().stopService(stopServiceIntent);
    }

    private void reStartService() {
        Intent startServiceIntent = new Intent(this.cordova.getActivity().getApplicationContext(), FallService.class);
        this.cordova.getActivity().startService(startServiceIntent);
    }

    private void sendAlert(JSONArray args) throws JSONException{
        Intent sendAlertIntent = new Intent(this.cordova.getActivity().getApplicationContext(), FallService.class);
        sendAlertIntent.putExtra("sendAlert", "sendAlert");

        this.cordova.getActivity().startService(sendAlertIntent);

        if (args.getJSONObject(0).length() == 1) {
            launchMainPage();
        }
    }

    private void launchMainPage() {
        SharedPreferences preferences = this.cordova.getActivity().getSharedPreferences("launcherActivity", Context.MODE_PRIVATE);
        SharedPreferences.Editor preferencesEditor = preferences.edit();

        preferencesEditor.putString("launchClass", "MainPage");
        preferencesEditor.commit();

        this.cordova.getActivity().finish();
        Intent mainPageIntent = new Intent(this.cordova.getActivity().getApplicationContext(), CordovaApp.class);
        this.cordova.getActivity().startActivity(mainPageIntent);
    }
}