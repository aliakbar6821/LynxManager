package com.lynx.manager;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.PreferenceFragmentCompat;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class LynxPreferenceFragment extends PreferenceFragmentCompat {

    private String currentType = "";

    // The Engine that handles the file picker result
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleFileSelection(result.getData().getData());
                }
            }
    );

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.lynx_preferences, rootKey);

        setupPref("pref_add_pif", p -> { 
            openFilePicker("application/json", "pif"); 
            return true; 
        });
        
        setupPref("pref_add_keybox", p -> { 
            openFilePicker("text/xml", "keybox"); 
            return true; 
        });
        
        setupPref("pref_reset_spoofing", p -> { resetSpoofing(); return true; });
        setupPref("pref_refresh_spoof", p -> { refreshSpoof(); return true; });
    }

    private void setupPref(String key, androidx.preference.Preference.OnPreferenceClickListener l) {
        androidx.preference.Preference p = findPreference(key);
        if (p != null) p.setOnPreferenceClickListener(l);
    }

    private void openFilePicker(String mimeType, String type) {
        this.currentType = type;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        filePickerLauncher.launch(intent);
    }

    private void handleFileSelection(Uri uri) {
        try {
            InputStream is = getContext().getContentResolver().openInputStream(uri);
            String content = new BufferedReader(new InputStreamReader(is))
                    .lines().collect(Collectors.joining("\n"));
            
            String dbKey = currentType.equals("pif") ? "lynx_pif_data" : "lynx_keybox_data";
            Settings.Secure.putString(getContext().getContentResolver(), dbKey, content);
            
            Toast.makeText(getContext(), "✅ " + currentType.toUpperCase() + " Applied", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "❌ Error reading file", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshSpoof() {
        Context ctx = getContext();
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        try {
            // Send the GMS Sync signal
            Intent intent = new Intent("com.google.android.gms.phenotype.FLAG_UPDATE");
            intent.setPackage("com.google.android.gms");
            ctx.sendBroadcast(intent);

            // Kill background processes
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
