package com.lynx.manager;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import androidx.preference.PreferenceFragmentCompat;

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
        Context ctx = getContext();
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        
        try {
            // Method 1: Public API for killing background processes
            am.killBackgroundProcesses("com.android.vending");
            am.killBackgroundProcesses("com.google.android.gms");
            
            // Method 2: System Intent to notify GMS that its data is "stale"
            // This often forces a full reload of the Integrity state
            Intent intent = new Intent("com.google.android.gms.phenotype.FLAG_UPDATE");
            intent.setPackage("com.google.android.gms");
            ctx.sendBroadcast(intent);

            Toast.makeText(ctx, "🔄 Spoof Refreshed", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(ctx, "⚠️ Refresh sent via Broadcast", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetSpoofing() {
        Settings.Secure.putString(getContext().getContentResolver(), "lynx_pif_data", "");
        Settings.Secure.putString(getContext().getContentResolver(), "lynx_keybox_data", "");
        Toast.makeText(getContext(), "🗑️ Database Wiped", Toast.LENGTH_SHORT).show();
    }
}
