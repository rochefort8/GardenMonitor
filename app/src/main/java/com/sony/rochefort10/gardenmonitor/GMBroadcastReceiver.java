package com.sony.rochefort10.gardenmonitor;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

/**
 * Created by rochefort10 on 2017/04/08.
 */

public class GMBroadcastReceiver extends WakefulBroadcastReceiver {
    private int index = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("GMB",String.valueOf(index)) ;
        index++;
        Intent serviceIntent = new Intent(context,GMPeriodicService.class);
        startWakefulService(context,serviceIntent);
    }
}
