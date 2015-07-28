package com.example.amrizalzainuddin.earthquake;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

/**
 * Created by amrizal.zainuddin on 24/7/2015.
 */
public class EarthquakeRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new EarthquakeRemoteViewsFactory(getApplicationContext());
    }

    class EarthquakeRemoteViewsFactory implements RemoteViewsFactory{

        private Context context;

        public  EarthquakeRemoteViewsFactory(Context context){
            this.context = context;
        }

        @Override
        public void onCreate() {
            c = executeQuery();
        }

        @Override
        public void onDataSetChanged() {
            c = executeQuery();
        }

        @Override
        public void onDestroy() {
            c.close();
        }

        @Override
        public int getCount() {
            if(c != null)
                return c.getCount();
            else
                return 0;
        }

        @Override
        public RemoteViews getViewAt(int position) {
            //move the cursor to the required index
            c.moveToPosition(position);

            //extract the values for the current cursor row
            int idIdx = c.getColumnIndex(EarthquakeProvider.KEY_ID);
            int magnitudeIdx = c.getColumnIndex(EarthquakeProvider.KEY_MAGNITUDE);
            int detailsIdx = c.getColumnIndex(EarthquakeProvider.KEY_DETAILS);

            String id = c.getString(idIdx);
            String magnitude = c.getString(magnitudeIdx);
            String details = c.getString(detailsIdx);

            //create a new remove views object and use it to populate the
            //layout used to represent each earthquake in the list
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.quake_widget);
            rv.setTextViewText(R.id.widget_magnitude, magnitude);
            rv.setTextViewText(R.id.widget_details, details);

            //create the fill-in Intent that adds the URI for the current item to the template intent
            Intent fillInIntent = new Intent();
            Uri uri = Uri.withAppendedPath(EarthquakeProvider.CONTENT_URI, id);
            fillInIntent.setData(uri);

            rv.setOnClickFillInIntent(R.id.widget_magnitude, fillInIntent);
            rv.setOnClickFillInIntent(R.id.widget_details, fillInIntent);

            return rv;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            if(c != null)
                return c.getLong(c.getColumnIndex(EarthquakeProvider.KEY_ID));
            else
                return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        private Cursor c;
        private Cursor executeQuery(){
            String[] projection = new String[]{
                    EarthquakeProvider.KEY_ID,
                    EarthquakeProvider.KEY_MAGNITUDE,
                    EarthquakeProvider.KEY_DETAILS
            };

            Context appContext = getApplicationContext();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
            int minimumMagnitude = Integer.parseInt(prefs.getString(PreferencesActivity.PREF_MIN_MAG, "3"));
            String where = EarthquakeProvider.KEY_MAGNITUDE + " > " + minimumMagnitude;

            return context.getContentResolver().query(EarthquakeProvider.CONTENT_URI, projection, where, null, null);
        }
    }
}
