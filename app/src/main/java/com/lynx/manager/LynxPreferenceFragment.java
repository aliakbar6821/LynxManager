package com.lynx.manager;

import android.app.Activity;
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

        setupPref("pref_add_pif", p -> { openFilePicker("application/json", "pif"); return true; });
        setupPref("pref_add_keybox", p -> { openFilePicker("text/xml", "keybox"); return true; });
        setupPref("pref_reset_spoofing", p -> { resetSpoofing(); return true; });
        setupPref("pref_refresh_spoof", p -> { refreshSpoof(); return true; });
    }

    private void setupPref(String key, androidx.preference.Preference.OnPreferenceClickListener listener) {
        androidx.preference.Preference pref = findPreference(key);
        if (pref != null) pref.setOnPreferenceClickListener(listener);
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
            String content = new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
            Settings.Secure.putString(getContext().getContentResolver(), currentType.equals("pif") ? "lynx_pif_data" : "lynx_keybox_data", content);
            Toast.makeText(getContext(), "✅ " + currentType.toUpperCase() + " Loaded", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "❌ File Load Failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshSpoof() {
        try {
            // Direct system command (No SU needed for System Apps)
            Runtime.getRuntime().exec("am force-stop com.android.vending");
            Runtime.getRuntime().exec("am force-stop com.google.android.gms");
            Toast.makeText(getContext(), "🔄 Spoofing Refreshed", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "❌ System call blocked", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetSpoofing() {
        Settings.Secure.putString(getContext().getContentResolver(), "lynx_pif_data", null);
        Settings.Secure.putString(getContext().getContentResolver(), "lynx_keybox_data", null);
        Toast.makeText(getContext(), "🗑️ Database Reset", Toast.LENGTH_SHORT).show();
    }
}
