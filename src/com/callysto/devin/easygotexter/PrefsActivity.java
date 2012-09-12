package com.callysto.devin.easygotexter;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.View;

public class PrefsActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
    }//onCreate
}//PrefsActivity