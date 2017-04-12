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

import static com.sony.rochefort10.gardenmonitor.LogUtil.TAG;

public class MainActivity extends AppCompatActivity {

    boolean bPeriodicServiceStarted = false ;
    Intent mMonitorServiceIntent ;

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

        Button button_service = (Button) findViewById(R.id.button_start_service);
        button_service.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Button button = (Button) v;
                if (bPeriodicServiceStarted == false) {
                    startPeriodicService() ;
                    bPeriodicServiceStarted = true ;
                } else {
                    stopPeriodicService() ;
                    bPeriodicServiceStarted = false ;

                }
            }

        });

        // Start monitor service
        mMonitorServiceIntent = new Intent(MainActivity.this, GMMonitorService.class) ;
        startService(mMonitorServiceIntent);
    }

    @Override
    protected  void onDestroy() {
        super.onDestroy();

        // Terminate monitor service
        if (mMonitorServiceIntent != null) {
            stopService(mMonitorServiceIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Resume comminication between monitor service and me
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction("action_light");
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiver,filter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Pause comminication between monitor service and me
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

    private void startPeriodicService() {
        Log.d(TAG(this),"startPeriodicService") ;

        // Send to GMMonitor service
        Intent i = new Intent();
        i.setAction("action1");
        i.putExtra("periodic_service","start");
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(i);
    }
    private void stopPeriodicService() {
        Log.d(TAG(this),"stopPeriodicService") ;

        // Send to GMMonitor service
        Intent i = new Intent();
        i.setAction("action1");
        i.putExtra("periodic_service","stop");
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(i);

    }

}
