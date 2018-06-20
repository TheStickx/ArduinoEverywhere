package com.example.alexa.arduinoeverywhere;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.CheckBoxPreference;

public class SettingNetwork extends PreferenceActivity {
    SharedPreferences settings = null;
    Preference Preference_AdressIP = null;
    Preference Preference_Port = null;
    Preference Preference_Password = null;
    CheckBoxPreference Preference_MultiCon = null;
    ListPreference USBspeedValue = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // initie les shared prefs
        addPreferencesFromResource(R.xml.setting_network);
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        Preference_AdressIP = findPreference("pref_AdressIP");
        Preference_AdressIP.setSummary(settings.getString("pref_AdressIP", "192.168.1.20"));
        Preference_AdressIP.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((String)newValue);
                return true;
            }
        });

        Preference_Port = findPreference("pref_Port");
        Preference_Port.setSummary(settings.getString("pref_Port", "13000"));
        Preference_Port.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((String)newValue);
                return true;
            }
        });

        Preference_Password = findPreference("pref_Password");
        Preference_Password.setSummary(settings.getString("pref_Password", ""));
        Preference_Password.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((String)newValue);
                return true;
            }
        });

        Preference_MultiCon = (CheckBoxPreference) findPreference("pref_MultiCon");
        Preference_MultiCon.setChecked (settings.getBoolean("pref_MultiCon", true));
        Preference_MultiCon.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((String)newValue);
                return true;
            }
        });

        USBspeedValue = (ListPreference) findPreference("USBspeed");
        USBspeedValue.setSummary (settings.getString("USBspeed", "9600"));
        USBspeedValue.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((String)newValue);
                return true;
            }
        });
    }



    @Override
    public void onBackPressed() {
        super.onBackPressed();

        finish();

    }
}