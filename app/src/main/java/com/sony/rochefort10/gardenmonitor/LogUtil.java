package com.sony.rochefort10.gardenmonitor;

/**
 * Created by 0000920402 on 2017/04/12.
 */

public class LogUtil {
    private static final int MAX_TAG_SIZE = 23;
    // Objのクラス名をMAX_TAG_SIZEの文字数以内で出力してくれる。
    public static String TAG(Object obj) {
        String objName = obj.getClass().getSimpleName();
        return objName.length() > MAX_TAG_SIZE ? objName.substring(0, MAX_TAG_SIZE) : objName;
    }
}