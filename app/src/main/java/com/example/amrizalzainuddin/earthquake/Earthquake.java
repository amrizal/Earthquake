package com.example.amrizalzainuddin.earthquake;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;


//public class Earthquake extends ActionBarActivity {
public class Earthquake extends Activity {

    public int minimumMagnitude = 0;
    public boolean autoUpdatedChecked = false;
    public  int updateFreq = 0;

    TabListener<EarthquakeListFragment> listTabListener;
    TabListener<EarthquakeMapFragment> mapTabListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_earthquake);

        updateFromPreferences();

        //use the search manager to find the searchableinfo related to this
        //activity
        SearchManager searchManager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
        SearchableInfo searchableInfo = searchManager.getSearchableInfo(getComponentName());

        //bind the activity's searchableinfo to the search view
        SearchView searchView = (SearchView)findViewById(R.id.searchView);
        searchView.setSearchableInfo(searchableInfo);

        ActionBar actionBar = getActionBar();

        View fragmentContainer = findViewById(R.id.EarthquakeFragmentContainer);

        //use the tablet navigation if the list and map fragments are both available
        boolean tabletLayout = fragmentContainer == null;

        //if it's not a tablet, use the tab Action Bar navigation
        if(!tabletLayout){
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            actionBar.setDisplayShowTitleEnabled(false);

            //create and add the list tab
            ActionBar.Tab listTab = actionBar.newTab();

            listTabListener = new TabListener<EarthquakeListFragment>
                    (this, R.id.EarthquakeFragmentContainer, EarthquakeListFragment.class);

            listTab.setText("List")
                    .setContentDescription("List of earthquakes")
                    .setTabListener(listTabListener);

            actionBar.addTab(listTab);

            //create and add the map tab
            ActionBar.Tab mapTab = actionBar.newTab();

            mapTabListener = new TabListener<EarthquakeMapFragment>
                    (this, R.id.EarthquakeFragmentContainer, EarthquakeMapFragment.class);

            mapTab.setText("Map")
                    .setContentDescription("Map of earthquakes")
                    .setTabListener(mapTabListener);

            actionBar.addTab(mapTab);
        }
    }

    private static String ACTION_BAR_INDEX = "ACTION_BAR_INDEX";

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        View fragmentContainer = findViewById(R.id.EarthquakeFragmentContainer);
        boolean tabletLayout = fragmentContainer == null;

        if(!tabletLayout){
            //save the current action bar tab selection
            int actionBarIndex = getActionBar().getSelectedTab().getPosition();
            SharedPreferences.Editor editor = getPreferences(Activity.MODE_PRIVATE).edit();
            editor.putInt(ACTION_BAR_INDEX, actionBarIndex);
            editor.apply();

            //detach each of the fragments
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            if(mapTabListener.fragment != null)
                ft.detach(mapTabListener.fragment);

            if(listTabListener.fragment != null)
                ft.detach(listTabListener.fragment);

            ft.commit();
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        View fragementContainer = findViewById(R.id.EarthquakeFragmentContainer);
        boolean tabletLayout = fragementContainer == null;

        if(!tabletLayout){
            //find the recreated fragemtns and assign them to their associated tab listeners
            listTabListener.fragment = getFragmentManager().findFragmentByTag(EarthquakeListFragment.class.getName());
            mapTabListener.fragment = getFragmentManager().findFragmentByTag(EarthquakeMapFragment.class.getName());

            //restore the previous action bar selection
            SharedPreferences sp = getPreferences(Activity.MODE_PRIVATE);
            int actionBarIndex = sp.getInt(ACTION_BAR_INDEX, 0);
            getActionBar().setSelectedNavigationItem(actionBarIndex);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        View fragmentContainer = findViewById(R.id.EarthquakeFragmentContainer);
        boolean tabletLayout = fragmentContainer == null;

        if(!tabletLayout){
            SharedPreferences sp = getPreferences(Activity.MODE_PRIVATE);
            int actionBarIndex = sp.getInt(ACTION_BAR_INDEX, 0);
            getActionBar().setSelectedNavigationItem(actionBarIndex);
        }
    }

    static final private  int MENU_PREFERENCES = Menu.FIRST + 1;
    static final private  int MENU_UPDATE = Menu.FIRST + 2;

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        super.onCreateOptionsMenu(menu);

        menu.add(0, MENU_PREFERENCES, Menu.NONE, R.string.menu_preferences);
        return true;
    }

    private static final int SHOW_PREFERENCES = -1;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId())
        {
            case (MENU_PREFERENCES):
                Class c = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB ? PreferencesActivity.class : FragmentPreferences.class;
                Intent i = new Intent(this, c);
                startActivityForResult(i, SHOW_PREFERENCES);
                return true;
        }

        return false;
    }

    private void updateFromPreferences(){
        Context context = getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        minimumMagnitude = Integer.parseInt(prefs.getString(PreferencesActivity.PREF_MIN_MAG, "3"));
        updateFreq = Integer.parseInt(prefs.getString(PreferencesActivity.PREF_UPDATE_FREQ, "60"));

        autoUpdatedChecked = prefs.getBoolean(PreferencesActivity.PREF_AUTO_UPDATE, false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == SHOW_PREFERENCES){
            updateFromPreferences();
            startService(new Intent(this, EarthquakeUpdateService.class));
        }
    }

    public static class TabListener<T extends Fragment> implements ActionBar.TabListener{

        private Fragment fragment;
        private Activity activity;
        private Class<T> fragmentClass;
        private int fragmentContainer;

        public TabListener(Activity activity, int fragmentContainer, Class<T> fragmentClass) {
            this.activity = activity;
            this.fragmentContainer = fragmentContainer;
            this.fragmentClass = fragmentClass;
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            if(fragment == null){
                String fragmentName = fragmentClass.getName();
                fragment = Fragment.instantiate(activity, fragmentName);
                ft.add(fragmentContainer, fragment, fragmentName);
            }else
                ft.attach(fragment);
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            if(fragment != null)
                ft.detach(fragment);
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
            if(fragment != null)
                ft.attach(fragment);
        }
    }
}
