package com.example.amrizalzainuddin.earthquake;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.NetworkOnMainThreadException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by amrizal.zainuddin on 14/7/2015.
 */
public class EarthquakeUpdateService extends IntentService {

    public static String TAG = "EARTHQUAKE_UPDATE_SERVICE";
    public static String QUAKES_REFRESHED = "com.example.amrizalzainuddin.earthquake.QUAKES_REFRESHED";

    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;
    private Notification.Builder earthquakeNotificationBuilder;
    public  static final int NOTIFICATION_ID = 1;

    public EarthquakeUpdateService(){
        super("EarthquakeUpdateService");
    }

    public EarthquakeUpdateService(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

        String ALARM_ACTION = EarthquakeAlarmReceiver.ACTION_REFRESH_EARTHQUAKE_ALARM;

        Intent intentToFire = new Intent(ALARM_ACTION);
        alarmIntent = PendingIntent.getBroadcast(this, 0, intentToFire, 0);

        earthquakeNotificationBuilder = new Notification.Builder(this);
        earthquakeNotificationBuilder
                .setAutoCancel(true)
                .setTicker("Earthquake detected")
                .setSmallIcon(R.mipmap.ic_launcher);

    }

    private void broadcastNotification(Quake quake){
        Intent startActivityIntent = new Intent(this, Earthquake.class);
        PendingIntent launchIntent = PendingIntent.getActivity(this, 0, startActivityIntent, 0);

        earthquakeNotificationBuilder
                .setContentIntent(launchIntent)
                .setWhen(quake.getDate().getTime())
                .setContentTitle("M:" + quake.getMagnitude())
                .setContentText(quake.getDetails());

        if(quake.getMagnitude() > 6){
            Uri ringURI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            earthquakeNotificationBuilder.setSound(ringURI);
        }

        double vibrateLength = 100*Math.exp(0.53*quake.getMagnitude());
        long[] vibrate = new long[]{100, 100, (long)vibrateLength};
        earthquakeNotificationBuilder.setVibrate(vibrate);

        int color;
        if(quake.getMagnitude() < 5.4){
            color = Color.GREEN;
        }
        else if(quake.getMagnitude() < 6){
            color = Color.YELLOW;
        }
        else {
            color = Color.RED;
        }

        earthquakeNotificationBuilder.setLights(color, (int)vibrateLength, (int)vibrateLength);

        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(NOTIFICATION_ID, earthquakeNotificationBuilder.getNotification());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //retrieve the shared preferences
        Context context = getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        int updateFreq = Integer.parseInt(prefs.getString(PreferencesActivity.PREF_UPDATE_FREQ, "60"));
        boolean autoUpdateChecked = prefs.getBoolean(PreferencesActivity.PREF_AUTO_UPDATE, false);

        if(autoUpdateChecked){
            int alarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;
            long timeToRefresh = SystemClock.elapsedRealtime() + updateFreq*60*1000;
            alarmManager.setInexactRepeating(alarmType, timeToRefresh, updateFreq*60*1000, alarmIntent);
        }
        else{
            alarmManager.cancel(alarmIntent);
        }

        refreshEarthquakes();
        sendBroadcast(new Intent(QUAKES_REFRESHED));
    }

    private void addNewQuake(Quake _quake) {
        ContentResolver cr = getContentResolver();
        //Construct a where clause to make sure we don't already have this
        //earthquake in the provide
        String w = EarthquakeProvider.KEY_DATE + " = " + _quake.getDate().getTime();

        //if the earthquake is new, insert it into the provider.
        Cursor query = cr.query(EarthquakeProvider.CONTENT_URI, null, w, null, null);
        if(query.getCount() == 0){
            ContentValues values = new ContentValues();

            values.put(EarthquakeProvider.KEY_DATE, _quake.getDate().getTime());
            values.put(EarthquakeProvider.KEY_DETAILS, _quake.getDetails());
            values.put(EarthquakeProvider.KEY_SUMMARY, _quake.toString());

            double lat = _quake.getLocation().getLatitude();
            double lng = _quake.getLocation().getLongitude();
            values.put(EarthquakeProvider.KEY_LOCATION_LAT, lat);
            values.put(EarthquakeProvider.KEY_LOCATION_LNG, lng);
            values.put(EarthquakeProvider.KEY_LINK, _quake.getLink());
            values.put(EarthquakeProvider.KEY_MAGNITUDE, _quake.getMagnitude());

            //trigger a notification
            broadcastNotification(_quake);

            cr.insert(EarthquakeProvider.CONTENT_URI, values);
        }

        query.close();
    }

    public void refreshEarthquakes(){
        URL url = null;
        try{
            String quakeFeed = getString(R.string.quake_feed);
            url = new URL(quakeFeed);

            URLConnection connection;
            connection = url.openConnection();

            HttpURLConnection httpConnection = (HttpURLConnection)connection;
            int responseCode = httpConnection.getResponseCode();

            if(responseCode == HttpURLConnection.HTTP_OK){

                InputStream in = httpConnection.getInputStream();

                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();

                Document dom = db.parse(in);
                Element docEle = dom.getDocumentElement();

                NodeList nl = docEle.getElementsByTagName("entry");
                if (nl != null && nl.getLength() > 0) {
                    for (int i = 0; i < nl.getLength(); i++) {
                        Element entry = (Element) nl.item(i);
                        Element title = (Element) entry.getElementsByTagName("title").item(0);
                        Element g = (Element)entry.getElementsByTagName("georss:point").item(0);
                        Element when = (Element) entry.getElementsByTagName("updated").item(0);
                        Element link = (Element) entry.getElementsByTagName("link").item(0);

                        String details = title.getFirstChild().getNodeValue();
                        String hostname = "http://earthquake.usgs.gov";
                        String linkString = hostname + link.getAttribute("href");

                        if(g == null)
                            continue;

                        String point = g.getFirstChild().getNodeValue();
                        String dt = when.getFirstChild().getNodeValue();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd'T'hh:mm:ss.SSS'Z'");
                        Date gdate = new GregorianCalendar(0, 0, 0).getTime();
                        try {
                            gdate = sdf.parse(dt);
                        } catch (ParseException e) {
                            Log.d(TAG, "Date parsing exception,", e);
                        }

                        String[] location = point.split(" ");
                        Location l = new Location("dummyGPS");
                        l.setLatitude(Double.parseDouble(location[0]));
                        l.setLongitude(Double.parseDouble(location[1]));

                        String magnitudeString = details.split(" ")[1];
                        int end = magnitudeString.length() - 1;
                        double magnitude = Double.parseDouble(magnitudeString.substring(0, end));

                        if(details.indexOf(",") < 0)
                            continue;

                        details = details.split(",")[1].trim();

                        final Quake quake = new Quake(gdate, details, l, magnitude, linkString);

                        addNewQuake(quake);
                    }
                }
            }
        }catch (MalformedURLException e){
            Log.d(TAG, "MalformedURLException");
        }catch(IOException e){
            Log.d(TAG, "IOException");
        }catch (ParserConfigurationException e){
            Log.d(TAG, "ParserConfigurationException");
        }catch (SAXException e){
            Log.d(TAG, "SAXException");
        }catch (NetworkOnMainThreadException e){
            Log.d(TAG, "NetworkOnMainThreadException");
        }
        finally{
        }
    }
}
