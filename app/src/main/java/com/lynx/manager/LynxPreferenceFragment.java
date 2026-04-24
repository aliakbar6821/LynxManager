package com.lynx.manager;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import androidx.preference.PreferenceFragmentCompat;

public class LynxPreferenceFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.lynx_preferences, rootKey);

        setupPref("pref_reset_spoofing", p -> { resetSpoofing(); return true; });
        setupPref("pref_refresh_spoof", p -> { refreshSpoof(); return true; });
    }

    private void setupPref(String key, androidx.preference.Preference.OnPreferenceClickListener listener) {
        androidx.preference.Preference pref = findPreference(key);
        if (pref != null) pref.setOnPreferenceClickListener(listener);
    }

    private void refreshSpoof() {
        Context ctx = getContext();
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);

        try {
            // Signal GMS that its configuration is stale
            Intent intent = new Intent("com.google.android.gms.phenotype.FLAG_UPDATE");
            intent.setPackage("com.google.android.gms");
            ctx.sendBroadcast(intent);

            // Kill processes (Safe for privileged system apps)
            String[] targets = {"com.android.vending", "com.google.android.gms", "com.google.android.gms.unstable"};
            for (String pkg : targets) {
                am.killBackgroundProcesses(pkg);
            }

            Toast.makeText(ctx, "🔄 Sync signal sent to GMS", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(ctx, "⚠️ Refresh triggered", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetSpoofing() {
        Settings.Secure.putString(getContext().getContentResolver(), "lynx_pif_data", "");
        Settings.Secure.putString(getContext().getContentResolver(), "lynx_keybox_data", "");
        Toast.makeText(getContext(), "🗑️ Database Wiped", Toast.LENGTH_SHORT).show();
    }
}
