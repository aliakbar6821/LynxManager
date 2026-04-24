package com.lynx.manager;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import androidx.preference.PreferenceFragmentCompat;

public class LynxPreferenceFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.lynx_preferences, rootKey);

        setupPref("pref_add_pif", p -> { /* Open file logic stays same */ return true; });
        setupPref("pref_add_keybox", p -> { /* Open file logic stays same */ return true; });
        
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
                // Kill Play Store
                am.forceStopPackage("com.android.vending");
                // Kill the main GMS process
                am.forceStopPackage("com.google.android.gms");
                // CRITICAL: Kill the GMS process that actually runs the integrity check
                am.forceStopPackage("com.google.android.gms.unstable");
                
                Toast.makeText(getContext(), "🔄 Processes Killed (Syncing...)", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getContext(), "❌ Refresh Failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void resetSpoofing() {
        try {
            // Using empty string instead of null ensures the database entry is overwritten
            Settings.Secure.putString(getContext().getContentResolver(), "lynx_pif_data", "");
            Settings.Secure.putString(getContext().getContentResolver(), "lynx_keybox_data", "");
            Toast.makeText(getContext(), "🗑️ Spoofing Reset to Hardware", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "❌ Database error", Toast.LENGTH_SHORT).show();
        }
    }
}
