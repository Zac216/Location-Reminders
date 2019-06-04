package gocrew.locationreminders;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);


        Fragment fragment = new SettingsScreen();
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        if (savedInstanceState == null) {
            fragmentTransaction.add(R.id.relative_layout, fragment, "settings_fragment");
            fragmentTransaction.commit();
        }
        else {
            fragment = getFragmentManager().findFragmentByTag("settings_fragment");
        }
    }






    public static class SettingsScreen extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener{
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);

            SwitchPreference switchPreference = (SwitchPreference) findPreference("switch_active");
            switchPreference.setEnabled(false);
            EditTextPreference editTextPreference = (EditTextPreference) findPreference("user_name");
            editTextPreference.setSummary(editTextPreference.getText());



                final RingtonePreference ringPref = (RingtonePreference) findPreference("ringtone_preference_1");
                ringPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference,
                                                      Object newValue) {
                        Log.i("***", "Changed " + newValue.toString());
                        Ringtone ringtone = RingtoneManager.getRingtone(
                                getActivity(), Uri.parse((String) newValue));
                        ringPref.setSummary(ringtone.getTitle(getActivity()));
                        return true;
                    }
                });
                String ringtonePath=findPreference("ringtone_preference_1").getSharedPreferences().getString(findPreference("ringtone_preference_1").getKey(), "defValue");
                Ringtone ringtone = RingtoneManager.getRingtone(
                        getActivity(), Uri.parse((String) ringtonePath));
                ringPref.setSummary(ringtone.getTitle(getActivity()));



            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);



        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference pref = findPreference(key);

            if (pref instanceof EditTextPreference) {
                EditTextPreference editTextPreference = (EditTextPreference) pref;
                pref.setSummary(editTextPreference.getText());
            }
            if (pref instanceof RingtonePreference) {
                final RingtonePreference ringPref = (RingtonePreference) pref;
                ringPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference,
                                                      Object newValue) {
                        Log.i("***", "Changed " + newValue.toString());
                        Ringtone ringtone = RingtoneManager.getRingtone(
                                getActivity(), Uri.parse((String) newValue));
                        ringPref.setSummary(ringtone.getTitle(getActivity()));
                        return true;
                    }
                });
                String ringtonePath=pref.getSharedPreferences().getString(pref.getKey(), "defValue");
                Ringtone ringtone = RingtoneManager.getRingtone(
                        getActivity(), Uri.parse((String) ringtonePath));
                ringPref.setSummary(ringtone.getTitle(getActivity()));

            }


        }
    }

}
