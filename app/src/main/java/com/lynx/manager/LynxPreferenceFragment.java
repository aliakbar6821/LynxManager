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

        setupPref("pref_add_pif", p -> { /* file picker logic */ return true; });
        setupPref("pref_add_keybox", p -> { /* file picker logic */ return true; });
        setupPref("pref_reset_spoofing", p -> { resetSpoofing(); return true; });
        setupPref("pref_refresh_spoof", p -> { refreshSpoof(); return true; });
    }

    private void setupPref(String key, androidx.preference.Preference.OnPreferenceClickListener l) {
        androidx.preference.Preference p = findPreference(key);
        if (p != null) p.setOnPreferenceClickListener(l);
    }

    private void refreshSpoof() {
        try {
            // Attempt 1: Standard ActivityManager reflection
            ActivityManager am = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
            try {
                Method forceStop = am.getClass().getMethod("forceStopPackage", String.class);
                forceStop.invoke(am, "com.android.vending");
                forceStop.invoke(am, "com.google.android.gms");
                forceStop.invoke(am, "com.google.android.gms.unstable");
            } catch (Exception e) {
                // Attempt 2: Direct IActivityManager service call (Lower level)
                Object iam = Class.forName("android.app.ActivityManager")
                        .getMethod("getService").invoke(null);
                Method forceStop = iam.getClass().getMethod("forceStopPackage", String.class, int.class);
                
                // Invoke for user 0 (System/Owner)
                forceStop.invoke(iam, "com.android.vending", 0);
                forceStop.invoke(iam, "com.google.android.gms", 0);
                forceStop.invoke(iam, "com.google.android.gms.unstable", 0);
            }
            Toast.makeText(getContext(), "🔄 Processes Killed Successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // Detailed error to help us debug
            Toast.makeText(getContext(), "❌ Error: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
    }

    private void resetSpoofing() {
        Settings.Secure.putString(getContext().getContentResolver(), "lynx_pif_data", "");
        Settings.Secure.putString(getContext().getContentResolver(), "lynx_keybox_data", "");
        Toast.makeText(getContext(), "🗑️ Database Wiped", Toast.LENGTH_SHORT).show();
    }
}
