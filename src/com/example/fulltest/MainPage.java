package com.example.fulltest;

import android.os.Bundle;
import android.widget.Toast;
import org.apache.cordova.CordovaActivity;


public class MainPage extends CordovaActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.init();

        Toast.makeText(getApplicationContext(), "Main Page", Toast.LENGTH_SHORT).show();
        loadUrl("file:///android_asset/www/mainPage.html");
    }
}
