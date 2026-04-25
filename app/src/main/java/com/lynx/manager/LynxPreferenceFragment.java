package com.lynx.manager;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

        setupPref("pref_add_pif", p -> { openFilePicker("application/json", "pif"); return true; });
        setupPref("pref_add_keybox", p -> { openFilePicker("text/xml", "keybox"); return true; });
        setupPref("pref_refresh_spoof", p -> { refreshSpoof(); return true; });
        setupPref("pref_reset_spoofing", p -> { resetSpoofing(); return true; });

        updateSummaries();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSummaries();
    }

    private void setupPref(String key, Preference.OnPreferenceClickListener l) {
        Preference p = findPreference(key);
        if (p != null) p.setOnPreferenceClickListener(l);
    }

    private void updateSummaries() {
        Context ctx = getContext();
        if (ctx == null) return;
        ContentResolver cr = ctx.getContentResolver();

        String pif = Settings.Secure.getString(cr, "lynx_pif_data");
        String keybox = Settings.Secure.getString(cr, "lynx_keybox_data");

        Preference pifPref = findPreference("pref_add_pif");
        Preference kbPref = findPreference("pref_add_keybox");

        if (pifPref != null) {
            pifPref.setSummary((pif != null && !pif.trim().isEmpty())
                ? "✅ PIF active — tap to replace"
                : "⚠️ No PIF — using hardware fingerprint");
        }
        if (kbPref != null) {
            kbPref.setSummary((keybox != null && !keybox.trim().isEmpty())
                ? "✅ Keybox active — tap to replace"
                : "⚠️ No Keybox — using hardware attestation");
        }
    }

    private void openFilePicker(String mimeType, String type) {
        currentType = type;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        filePickerLauncher.launch(intent);
    }

    private void handleFileSelection(Uri uri) {
        Context ctx = getContext();
        if (ctx == null) return;
        try (InputStream is = ctx.getContentResolver().openInputStream(uri)) {
            if (is == null) {
                Toast.makeText(ctx, "❌ Cannot open file", Toast.LENGTH_SHORT).show();
                return;
            }
            String content = new BufferedReader(new InputStreamReader(is))
                    .lines().collect(Collectors.joining("\n"));

            if (content.trim().isEmpty()) {
                Toast.makeText(ctx, "❌ File is empty", Toast.LENGTH_SHORT).show();
                return;
            }

            String key = currentType.equals("pif") ? "lynx_pif_data" : "lynx_keybox_data";
            boolean ok = Settings.Secure.putString(ctx.getContentResolver(), key, content);

            if (ok) {
                Toast.makeText(ctx, "✅ " + currentType.toUpperCase() + " saved — tap Refresh to activate", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(ctx, "❌ Write failed — check WRITE_SECURE_SETTINGS", Toast.LENGTH_LONG).show();
            }
            updateSummaries();
        } catch (Exception e) {
            Toast.makeText(ctx, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void resetSpoofing() {
        Context ctx = getContext();
        if (ctx == null) return;

        // null removes the key entirely from Settings.Secure database
        // When PropImitationHooks reads null, it falls back to resource array (hardware)
        Settings.Secure.putString(ctx.getContentResolver(), "lynx_pif_data", null);
        Settings.Secure.putString(ctx.getContentResolver(), "lynx_keybox_data", null);

        Toast.makeText(ctx, "🗑️ Spoofing data deleted", Toast.LENGTH_SHORT).show();
        updateSummaries();

        // Auto-refresh so GMS picks up the change immediately
        refreshSpoof();
    }

    /**
     * How refresh works:
     *
     * 1. Kill GMS background processes
     * 2. GMS restarts automatically (Android keeps it alive)
     * 3. On restart, Instrumentation.newApplication() fires
     * 4. PropImitationHooks.setProps(Context) is called
     * 5. setPlayIntegrityProps() reads Settings.Secure fresh
     * 6. sCertifiedProps.clear() runs first (framework patch)
     * 7. New data (or null = hardware fallback) is loaded
     *
     * No broadcasts needed — there is no observer in the engine.
     * The hook in Instrumentation handles everything on app restart.
     */
    private void refreshSpoof() {
        Context ctx = getContext();
        if (ctx == null) return;

        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return;

        String[] targets = {
            "com.google.android.gms",
            "com.google.android.gms.unstable",
            "com.android.vending"
        };

        // First kill wave
        for (String pkg : targets) {
            try { am.killBackgroundProcesses(pkg); } catch (Exception ignored) {}
        }

        Toast.makeText(ctx, "⏳ Restarting GMS...", Toast.LENGTH_SHORT).show();

        // Second kill after 2 seconds — GMS respawns very fast
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            for (String pkg : targets) {
                try { am.killBackgroundProcesses(pkg); } catch (Exception ignored) {}
            }
            Toast.makeText(ctx, "🔄 Done — GMS restarted with fresh data", Toast.LENGTH_SHORT).show();
            updateSummaries();
        }, 2000);
    }
}
