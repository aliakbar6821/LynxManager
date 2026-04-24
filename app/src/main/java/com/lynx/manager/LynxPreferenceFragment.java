package com.lynx.manager;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LynxPreferenceFragment extends PreferenceFragmentCompat {

    private static final String TAG = "LynxManager";
    private static final String PIF_KEY = "lynx_pif_data";
    private static final String KEYBOX_KEY = "lynx_keybox_data";

    private ActivityResultLauncher<Intent> pifPickerLauncher;
    private ActivityResultLauncher<Intent> keyboxPickerLauncher;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.lynx_preferences, rootKey);

        pifPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        saveFileToDatabase(PIF_KEY, result.getData().getData(), "PIF.json");
                    }
                });

        keyboxPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        saveFileToDatabase(KEYBOX_KEY, result.getData().getData(), "Keybox");
                    }
                });

        findPreference("pref_add_pif").setOnPreferenceClickListener(preference -> {
            launchFilePicker(pifPickerLauncher, "application/json");
            return true;
        });

        findPreference("pref_add_keybox").setOnPreferenceClickListener(preference -> {
            launchFilePicker(keyboxPickerLauncher, "text/xml");
            return true;
        });

        findPreference("pref_reset_spoofing").setOnPreferenceClickListener(preference -> {
            Settings.Secure.putString(requireContext().getContentResolver(), PIF_KEY, null);
            Settings.Secure.putString(requireContext().getContentResolver(), KEYBOX_KEY, null);
            Toast.makeText(requireContext(), "Spoofing data wiped. System will use default.", Toast.LENGTH_LONG).show();
            return true;
        });

        findPreference("pref_check_integrity").setOnPreferenceClickListener(preference -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setComponent(new ComponentName("com.android.vending", "com.google.android.finsky.activities.SettingsActivity"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Could not open Play Store Settings", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to launch Finsky Settings", e);
            }
            return true;
        });
    }

    private void launchFilePicker(ActivityResultLauncher<Intent> launcher, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"*/*"});
        launcher.launch(intent);
    }

    private void saveFileToDatabase(String dbKey, Uri uri, String friendlyName) {
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            is.close();

            Settings.Secure.putString(requireContext().getContentResolver(), dbKey, sb.toString());
            Toast.makeText(requireContext(), friendlyName + " loaded into Secure Settings!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to read file", e);
            Toast.makeText(requireContext(), "Error parsing file", Toast.LENGTH_SHORT).show();
        }
    }
}
