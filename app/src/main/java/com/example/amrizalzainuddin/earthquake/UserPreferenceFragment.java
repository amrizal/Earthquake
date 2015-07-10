package com.example.amrizalzainuddin.earthquake;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by amrizal.zainuddin on 10/7/2015.
 */
public class UserPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.userpreferences);
    }
}
