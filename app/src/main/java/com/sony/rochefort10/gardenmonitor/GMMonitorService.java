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

import static com.sony.rochefort10.gardenmonitor.LogUtil.TAG;


public class GMMonitorService extends Service implements SensorEventListener {

//    Handler mHandler ;

    private SensorManager mSensorManager;
    private Sensor mLightSensor;

    private float mLightValue ;
    private int mBatteryScale ;
    private int mBatteryLevel ;

    private String dataFilePath = "logdata000.txt";
    static final String BR = System.getProperty("line.separator");
    private PendingIntent mPendingIntent ;
    private BroadcastReceiver mReceiver;
    private boolean bIsSensorInfoShown = false ;

    public GMMonitorService() {
        bIsSensorInfoShown = false ;
    }

    @Override
    public void onCreate() {

        Log.d(TAG(this),"onCreate");

        // Sensor listener
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorManager.registerListener (this,
                mLightSensor,
                SensorManager.SENSOR_DELAY_NORMAL);

        // Battery monitor
        IntentFilter filter=new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mBatteryChargerStatusReceiver,filter);

        // Message receiver
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG(this), "onRecieve:" + intent.getAction());

                if (intent.getStringExtra("periodic_service").equals("start")) {
                    startPeriodicService();
                } else if (intent.getStringExtra("periodic_service").equals("stop")) {
                    stopPeriodicService();
                }
            }
        };
        filter = new IntentFilter();
        filter.addAction("action1");
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiver, filter);

    }

    @Override
    public void onDestroy() {
        Log.d(TAG(this), "onDestroy");

        // Sensor listener
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this, mLightSensor);
        }

        // Battery Monitor
        unregisterReceiver(mBatteryChargerStatusReceiver);

        // Message receiver
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiver);

        stopPeriodicService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG(this), "onStartCommand");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
        return null ;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG(this), "onAccuracyChanged to " + Integer.toString(accuracy));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        Sensor sensor = event.sensor;
        float[] values = event.values;
        long timestamp = event.timestamp;

        if(sensor.getType() == Sensor.TYPE_LIGHT){
            float newValue = values[0] ;
            Log.d(TAG(this), "TYPE_LIGHT = " + String.valueOf(values[0]));

            // Send value to MainActivity
            Intent i = new Intent();
            i.setAction("action_light");
            i.putExtra("value_light",newValue) ;
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(i);

            float delta = Math.abs(newValue - mLightValue) * 100.0f / mLightValue ;
            if (delta < 10.0) {
                return ;
            }
            mLightValue = values[0] ;

            writeData();
            if (!bIsSensorInfoShown) {
                showSensorInfo(event);
                bIsSensorInfoShown = true;
            }
        }
    }

    public BroadcastReceiver mBatteryChargerStatusReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {

                mBatteryScale = intent.getIntExtra("scale", 0);
                mBatteryLevel = intent.getIntExtra("level", 0);

                String string = String.valueOf(mBatteryLevel) + "/" + String.valueOf(mBatteryScale) ;
                Log.d(TAG(this),"Battery:" + string);

                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(intent);

                // Write out at this timing
                writeData();
            }
        }
    };

    private void writeData() {
        String string = getCurrentDate() + ',' +
                String.valueOf(mLightValue) + ',' +
                String.valueOf(mBatteryLevel) + BR ;

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

    private void startPeriodicService() {
        Log.d(TAG(this),"startPeriodicService") ;

        // Alarm manager
        Intent intent = new Intent(this, GMBroadcastReceiver.class);
        mPendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(), 1000*60, mPendingIntent);

    }
    private void stopPeriodicService() {
        Log.d(TAG(this),"stopPeriodicService") ;

        // Alarm manager
        if (mPendingIntent != null) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            am.cancel(mPendingIntent);
        }
    }

    private void showSensorInfo(SensorEvent event){
        String info = "Name: " + event.sensor.getName() + "\n";
        info += "Vendor: " + event.sensor.getVendor() + "\n";
        info += "Type: " + event.sensor.getType() + "\n";
        info += "StringType: " + event.sensor.getStringType()+ "\n";

        int data = event.sensor.getMinDelay();
        info += "Mindelay: "+String.valueOf(data) +" usec\n";

        data = event.sensor.getMaxDelay();
        info += "Maxdelay: "+String.valueOf(data) +" usec\n";

        data = event.sensor.getReportingMode();
        String stinfo = "unknown";
        if(data == 0){
            stinfo = "REPORTING_MODE_CONTINUOUS";
        }else if(data == 1){
            stinfo = "REPORTING_MODE_ON_CHANGE";
        }else if(data == 2){
            stinfo = "REPORTING_MODE_ONE_SHOT";
        }
        info += "ReportingMode: "+stinfo +" \n";

        float fData = event.sensor.getMaximumRange();
        info += "MaxRange: "+String.valueOf(fData) +" \n";

        fData = event.sensor.getResolution();
        info += "Resolution: "+String.valueOf(fData) +" m/s^2 \n";

        fData = event.sensor.getPower();
        info += "Power: "+String.valueOf(fData) +" mA\n";

        Log.d(TAG(this),info) ;
    }
}

