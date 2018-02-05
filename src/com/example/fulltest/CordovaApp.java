/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package com.example.fulltest;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import org.apache.cordova.*;

public class CordovaApp extends CordovaActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        super.init();

        launchView();
    }

    @Override
    protected void onStart() {
        super.onStart();

        launchView();
    }

    private void launchView() {
        SharedPreferences preferences = getSharedPreferences("launcherActivity", Context.MODE_PRIVATE);

        if (preferences.getString("launchClass", "Class").equals("MainPage")) {

            if (!preferences.getString("locationService", "").equals("notFirstTime")) {
                checkLocationService();
            }
            SharedPreferences.Editor preferencesEditor = preferences.edit();
            preferencesEditor.putString("locationService", "notFirstTime");
            preferencesEditor.commit();

            loadUrl("file:///android_asset/www/mainPage.html");
        }
        else if (preferences.getString("launchClass", "Class").equals("AlertView")) {
            loadUrl("file:///android_asset/www/alertView.html");
        }
        else {
            loadUrl(launchUrl);
        }
    }

    private void checkLocationService() {
        boolean GPSEnabled = false;
        boolean networkEnabled = false;

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            GPSEnabled = true;
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            networkEnabled = true;
        }

        if (!GPSEnabled && !networkEnabled) {
            String title = "Servicio de localización desactivado";
            String text = "¿Quieres ir a los ajustes para activarlo?";
            askLocationService(title, text);
        }
        else if (!GPSEnabled && networkEnabled) {
            String title = "Servicio de localización por GPS desactivado";
            String text = "Sólo tienes activado el servicio de localización por red. Activar el GPS conlleva una mayor precisión " +
                    "en el posicionamiento. ¿Quieres activarlo desde los ajustes?";
            askLocationService(title, text);
        }
        else if (GPSEnabled && !networkEnabled) {
            String title = "Servicio de localización por red desactivado";
            String text = "Sólo tienes activado el servicio de localización por GPS. La localización por red implica un mejor posicionamiento " +
                    "en interiores. ¿Quieres activarlo desde los ajustes?";
            askLocationService(title, text);
        }
    }

    private void askLocationService(String title, String text) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(title);
        alert.setMessage(text);

        alert.setPositiveButton("Sí", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        });

        alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        alert.show();
    }
}
