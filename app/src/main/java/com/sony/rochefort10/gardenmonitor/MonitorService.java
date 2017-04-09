package com.sony.rochefort10.gardenmonitor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.parse.ParseFile;
import com.parse.ParseObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Timer;
import java.util.TimerTask;


public class MonitorService extends Service implements SensorEventListener {

    Handler mHandler ;
//    static final int period_ms = 1000*60*5;
    static final int period_ms = 1000;

    private SensorManager mSensorManager;
    private Sensor mLightSensor;
    private int mBatteryScale ;
    private int mBatteryLevel ;

    private float mLightValue ;

    private String dataFilePath = "logdata000.txt";
    static final String BR = System.getProperty("line.separator");
    private int fileNameIndex = 0;

    private Timer mTimer ;
    private TimerTask mTimerTask ;

    private BroadcastReceiver mReceiver;



    public MonitorService() {
    }

    private void createDataFilePath() {
        Formatter fm = new Formatter();
        fm.format("logdata%03d.txt", fileNameIndex);
        dataFilePath = fm.toString() ;
    }

    @Override
    public void onCreate() {

        Log.i("TestService", "onCreate");

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        mSensorManager.registerListener (this,
                mLightSensor,
                SensorManager.SENSOR_DELAY_NORMAL);


        IntentFilter filter=new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mBatteryChargerStatusReceiver,filter);

        Intent intent = new Intent(this, GMBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP,
               System.currentTimeMillis(), 1000*60, pendingIntent);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("Received",intent.getAction());
                IntentFilter filter=new IntentFilter();
                filter.addAction(Intent.ACTION_BATTERY_CHANGED);
                registerReceiver(mBatteryChargerStatusReceiver,filter);

            }
        };

//        IntentFilter filter = new IntentFilter();
        filter = new IntentFilter();
        filter.addAction("action1");
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("TestService", "onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i("TestService", "onDestroy");
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this, mLightSensor);
        }
        unregisterReceiver(mBatteryChargerStatusReceiver);

        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
        return null ;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // accuracy に変更があった時の処理
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        Sensor sensor = event.sensor;
        float[] values = event.values;
        long timestamp = event.timestamp;

        // 温度センサー
        if(sensor.getType() == Sensor.TYPE_LIGHT){
            // 温度

            float newValue = values[0] ;

            Intent i = new Intent();
            i.setAction("action_light");
            i.putExtra("value_light",newValue) ;
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(i);

            float delta = Math.abs(newValue - mLightValue) * 100.0f / mLightValue ;
            if (delta < 10.0) {
                return ;
            }
            mLightValue = values[0] ;
//            Log.d("SENSOR_DATA", "TYPE_LIGHT = " + String.valueOf(values[0]));

            // Write file
            String string = getCurrentDate() + ',' +
                            String.valueOf(mLightValue) + ',' +
                            String.valueOf(mBatteryLevel) + BR ;
            writeData(string);
        }
    }

    public BroadcastReceiver mBatteryChargerStatusReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                // 電池残量の最大値
                mBatteryScale = intent.getIntExtra("scale", 0);
                mBatteryLevel = intent.getIntExtra("level", 0);

                String string = String.valueOf(mBatteryLevel) + "/" + String.valueOf(mBatteryScale) ;
                Log.d("BAT",string);

                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(intent);

            }
        }
    };

    private void writeData(String string) {
        try {
            FileOutputStream fileOutputStream = openFileOutput(dataFilePath, MODE_PRIVATE|MODE_APPEND);
            fileOutputStream.write(string.getBytes());
        } catch (FileNotFoundException e) {

        } catch (IOException e) {
        }
    }

    public static String getCurrentDate (){
        final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        final Date date = new Date(System.currentTimeMillis());
        return df.format(date);
    }

}

