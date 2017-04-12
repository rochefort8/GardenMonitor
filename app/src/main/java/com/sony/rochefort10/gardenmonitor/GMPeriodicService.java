package com.sony.rochefort10.gardenmonitor;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.parse.ParseFile;
import com.parse.ParseObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;

import static com.sony.rochefort10.gardenmonitor.LogUtil.TAG;

/**
 * Created by rochefort10 on 2017/04/08.
 */

public class GMPeriodicService extends IntentService {

    private static String dataFilePath = "logdata000.txt";
    private static int fileNameIndex = 0;
    private static int mCount = 0 ;

    public GMPeriodicService() {
        super("GMPeriodicService");
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        try {
            if ((mCount % 5) == 0) {
                //Per 10 min.
                putFileToParse() ;
            } else {
                Log.d("Woken","But not time to sync");
            }
           mCount++ ;
        } finally {
            WakefulBroadcastReceiver.completeWakefulIntent(intent);
        }
    }

    private void putFileToParse() {

        Log.d(TAG(this),"Put data");

        ParseObject object = new ParseObject("MONO");

        File f = this.getFileStreamPath(dataFilePath);
        boolean isExists = f.exists();

        if (isExists == true) {

            try {
                Log.d("PATH",dataFilePath) ;
                FileInputStream fileInputStream;
                fileInputStream = openFileInput(dataFilePath);
                byte[] data = new byte[fileInputStream.available()];
                fileInputStream.read(data);

                ParseFile Pfile = new ParseFile("data.txt", data);
                Pfile.saveInBackground();
                /*
                try {
                    Pfile.save();
                } catch (com.parse.ParseException e) {
                    e.printStackTrace();
                }
                */
                object.put("data", Pfile);

                fileNameIndex++ ;
                moveFile() ;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        final DateFormat df = new SimpleDateFormat("MM/dd HH:mm:ss");
        final Date date = new Date(System.currentTimeMillis());
        object.put("alive",df.format(date));

//        object.saveInBackground();

        try {
            object.save();
        } catch (com.parse.ParseException e) {
            e.printStackTrace();
        }
        Log.d("Parse","Put data finished.");
    }

    private void moveFile() {
        final DateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
        final Date date = new Date(System.currentTimeMillis());
        String newFileNamgeString = df.format(date) + ".txt";

        File srcFile = getFileStreamPath( dataFilePath ); // Assuming it is in Internal Storage
        File dstFile = getFileStreamPath( newFileNamgeString ); // Assuming it is in Internal Storage
        srcFile.renameTo ( dstFile );
        Log.d("Moved to",newFileNamgeString);
    }




}
