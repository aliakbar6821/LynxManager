package com.lynx.manager;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import androidx.preference.PreferenceFragmentCompat;
import java.lang.reflect.Method;

public class LynxPreferenceFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.lynx_preferences, rootKey);

        setupPref("pref_add_pif", p -> { /* File picker logic remains the same */ return true; });
        setupPref("pref_add_keybox", p -> { /* File picker logic remains the same */ return true; });
        
        setupPref("pref_reset_spoofing", p -> {
            resetSpoofing();
            return true;
        });
        
        setupPref("pref_refresh_spoof", p -> {
            refreshSpoof();
            return true;
        });
    }

    private void setupPref(String key, androidx.preference.Preference.OnPreferenceClickListener listener) {
        androidx.preference.Preference pref = findPreference(key);
        if (pref != null) pref.setOnPreferenceClickListener(listener);
    }

    private void refreshSpoof() {
        ActivityManager am = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            try {
                // We use Reflection to find the hidden 'forceStopPackage' method
                Method forceStop = am.getClass().getMethod("forceStopPackage", String.class);
                
                // Kill the triple-threat of GMS caching
                forceStop.invoke(am, "com.android.vending");
                forceStop.invoke(am, "com.google.android.gms");
                forceStop.invoke(am, "com.google.android.gms.unstable");
                
                Toast.makeText(getContext(), "🔄 Processes Killed (Syncing...)", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getContext(), "❌ System call failed via Reflection", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void resetSpoofing() {
        try {
            // Using empty strings instead of null forces the database to overwrite
            Settings.Secure.putString(getContext().getContentResolver(), "lynx_pif_data", "");
            Settings.Secure.putString(getContext().getContentResolver(), "lynx_keybox_data", "");
            Toast.makeText(getContext(), "🗑️ Spoofing Reset to Hardware", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "❌ Database wipe failed", Toast.LENGTH_SHORT).show();
        }
    }
}
