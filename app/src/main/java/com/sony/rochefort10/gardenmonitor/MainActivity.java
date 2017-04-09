package com.sony.rochefort10.gardenmonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private SensorManager manager;

    boolean bStarted = false ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initParse();

        Button button0 = (Button) findViewById(R.id.button0);
        // ボタンがクリックされた時に呼び出されるコールバックリスナーを登録します
        button0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button button = (Button) v;

            }
        });

        startService(new Intent(MainActivity.this, MonitorService.class));

        Button button_service = (Button) findViewById(R.id.button_start_service);
        // ボタンがクリックされた時に呼び出されるコールバックリスナーを登録します
        button_service.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button button = (Button) v;
            }

        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction("action_light");
//        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiver,filter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiver);
    }

    private void initParse() {
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId(BuildConfig.PARSE_API_ID)
                .clientKey(BuildConfig.PARSE_API_KEY)
                .server("https://parseapi.back4app.com/")
                .build()
        );

        ParseACL defaultACL = new ParseACL();
        defaultACL.setPublicReadAccess(true);
        ParseACL.setDefaultACL(defaultACL, true);
        ParseInstallation.getCurrentInstallation().saveInBackground();
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //           Log.d("Main.received",intent.getAction());
            if (intent.getAction().equals("action_light")) {
                float f = intent.getFloatExtra("value_light",0.0f) ;
                TextView textview = (TextView) findViewById(R.id.text_light);
                textview.setText(String.valueOf(f));
            }

            if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                int scale = intent.getIntExtra("scale", 0);
                int level = intent.getIntExtra("level", 0);

                String string = String.valueOf(level) + "/" + String.valueOf(scale) ;
                TextView textview = (TextView) findViewById(R.id.text_battery);
                textview.setText(string);
            }
        }
    };

    private void sendSyncBroadcast() {

        Intent i = new Intent();
        i.setAction("action1");
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(i);
    }
}
