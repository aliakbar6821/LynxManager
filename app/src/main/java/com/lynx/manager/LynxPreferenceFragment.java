package com.lynx.manager;

import android.app.Activity;
import android.content.ContentResolver;
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

    // The Launcher for the File Picker (DocumentUI)
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

        // 1. Add PIF Click
        findPreference("pref_add_pif").setOnPreferenceClickListener(pref -> {
            openFilePicker("application/json", "pif");
            return true;
        });

        // 2. Add Keybox Click
        findPreference("pref_add_keybox").setOnPreferenceClickListener(pref -> {
            openFilePicker("text/xml", "keybox");
            return true;
        });

        // 3. Reset Spoofing Click
        findPreference("pref_reset_spoofing").setOnPreferenceClickListener(pref -> {
            resetSpoofing();
            return true;
        });

        // 4. Check Integrity Click
        findPreference("pref_check_integrity").setOnPreferenceClickListener(pref -> {
            launchPlayStoreIntegrity();
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
            String content = new BufferedReader(new InputStreamReader(inputStream))
                    .lines().collect(Collectors.joining("\n"));

            ContentResolver resolver = getContext().getContentResolver();
            if (currentType.equals("pif")) {
                Settings.Secure.putString(resolver, "lynx_pif_data", content);
                Toast.makeText(getContext(), "✅ PIF Loaded Successfully", Toast.LENGTH_SHORT).show();
            } else {
                Settings.Secure.putString(resolver, "lynx_keybox_data", content);
                Toast.makeText(getContext(), "✅ Keybox Loaded Successfully", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "❌ Error loading file", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetSpoofing() {
        ContentResolver resolver = getContext().getContentResolver();
        Settings.Secure.putString(resolver, "lynx_pif_data", null);
        Settings.Secure.putString(resolver, "lynx_keybox_data", null);
        Toast.makeText(getContext(), "🗑️ Spoofing Data Cleared", Toast.LENGTH_SHORT).show();
    }

    private void launchPlayStoreIntegrity() {
        try {
            Intent intent = new Intent("com.google.android.gms.PLAY_INTEGRITY_CHECK");
            intent.setPackage("com.android.vending");
            startActivity(intent);
        } catch (Exception e) {
            // Fallback to Play Store home if the shortcut fails
            Intent intent = getContext().getPackageManager().getLaunchIntentForPackage("com.android.vending");
            startActivity(intent);
        }
    }
}
