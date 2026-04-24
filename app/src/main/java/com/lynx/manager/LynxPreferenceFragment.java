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
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class LynxPreferenceFragment extends PreferenceFragmentCompat {

    private String currentType = "";
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

        findPreference("pref_add_pif").setOnPreferenceClickListener(p -> { openFilePicker("application/json", "pif"); return true; });
        findPreference("pref_add_keybox").setOnPreferenceClickListener(p -> { openFilePicker("text/xml", "keybox"); return true; });
        findPreference("pref_reset_spoofing").setOnPreferenceClickListener(p -> { resetSpoofing(); return true; });
        
        findPreference("pref_refresh_spoof").setOnPreferenceClickListener(p -> {
            refreshSpoof();
            return true;
        });
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
            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            String content = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
            Settings.Secure.putString(getContext().getContentResolver(), currentType.equals("pif") ? "lynx_pif_data" : "lynx_keybox_data", content);
            Toast.makeText(getContext(), "✅ " + currentType.toUpperCase() + " Applied", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "❌ Error reading file", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshSpoof() {
        // The System Way: No 'su' required for privileged apps
        ActivityManager am = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            try {
                am.forceStopPackage("com.android.vending");
                am.forceStopPackage("com.google.android.gms");
                Toast.makeText(getContext(), "🔄 Spoofing Refreshed", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getContext(), "❌ Failed to refresh: System permission missing", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void resetSpoofing() {
        Settings.Secure.putString(getContext().getContentResolver(), "lynx_pif_data", null);
        Settings.Secure.putString(getContext().getContentResolver(), "lynx_keybox_data", null);
        Toast.makeText(getContext(), "🗑️ Database Wiped", Toast.LENGTH_SHORT).show();
    }
}
