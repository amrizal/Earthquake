package com.example.amrizalzainuddin.earthquake;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViews;

/**
 * Created by amrizal.zainuddin on 24/7/2015.
 */
public class EarthquakeWidget extends AppWidgetProvider {

    public void updateQuake(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds){
        Cursor lastEarthquake;
        ContentResolver cr = context.getContentResolver();
        lastEarthquake = cr.query(EarthquakeProvider.CONTENT_URI, null, null, null, null);

        String magnitude = "--";
        String details = "-- None --";

        if(lastEarthquake != null){
            try{
                if(lastEarthquake.moveToFirst()){
                    int magColumn = lastEarthquake.getColumnIndexOrThrow(EarthquakeProvider.KEY_MAGNITUDE);
                    int detailsColumn = lastEarthquake.getColumnIndexOrThrow(EarthquakeProvider.KEY_DETAILS);

                    magnitude = lastEarthquake.getString(magColumn);
                    details = lastEarthquake.getString(detailsColumn);
                }
            }finally {
                lastEarthquake.close();
            }
        }

        final int N = appWidgetIds.length;
        for(int i =0; i<N; i++){
            int appWidgetId = appWidgetIds[i];
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.quake_widget);
            views.setTextViewText(R.id.widget_magnitude, magnitude);
            views.setTextViewText(R.id.widget_details, details);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        //create a pending intent that will open the main Activity
        Intent intent = new Intent(context, Earthquake.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        //apply the on click listener to both text views
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.quake_widget);

        views.setOnClickPendingIntent(R.id.widget_magnitude, pendingIntent);
        views.setOnClickPendingIntent(R.id.widget_details, pendingIntent);

        //update the widget UI with the latest Earthquake details
        updateQuake(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if(EarthquakeUpdateService.QUAKES_REFRESHED.equals(intent.getAction()))
            updateQuake(context);
    }

    public  void updateQuake(Context context){
        ComponentName thisWidget = new ComponentName(context, EarthquakeWidget.class);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        updateQuake(context, appWidgetManager, appWidgetIds);
    }
}
