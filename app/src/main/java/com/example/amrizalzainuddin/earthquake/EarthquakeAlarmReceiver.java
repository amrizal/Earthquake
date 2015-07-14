package com.example.amrizalzainuddin.earthquake;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by amrizal.zainuddin on 14/7/2015.
 */
public class EarthquakeAlarmReceiver extends BroadcastReceiver {

    public static final String ACTION_REFRESH_EARTHQUAKE_ALARM = "com.example.amrizalzainuddin.earthquake.ACTION_REFRESH_EARTHQUAKE_ALARM";
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent startIntent = new Intent(context, EarthquakeUpdateService.class);
        context.startService(startIntent);
    }
}
