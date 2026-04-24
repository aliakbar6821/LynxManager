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

        setupPref("pref_add_pif", p -> {
            openFilePicker("application/json", "pif");
            return true;
        });

        setupPref("pref_add_keybox", p -> {
            openFilePicker("text/xml", "keybox");
            return true;
        });

        setupPref("pref_refresh_spoof", p -> {
            refreshSpoof();
            return true;
        });

        setupPref("pref_reset_spoofing", p -> {
            resetSpoofing();
            return true;
        });

        // Update summaries to show current state on open
        updateSummaries();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSummaries();
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private void setupPref(String key, Preference.OnPreferenceClickListener l) {
        Preference p = findPreference(key);
        if (p != null) p.setOnPreferenceClickListener(l);
    }

    /**
     * Shows current state on each preference summary so user
     * knows at a glance what is loaded.
     */
    private void updateSummaries() {
        Context ctx = getContext();
        if (ctx == null) return;

        ContentResolver cr = ctx.getContentResolver();

        String pifData    = Settings.Secure.getString(cr, "lynx_pif_data");
        String keyboxData = Settings.Secure.getString(cr, "lynx_keybox_data");

        Preference pifPref    = findPreference("pref_add_pif");
        Preference keyboxPref = findPreference("pref_add_keybox");

        if (pifPref != null) {
            if (pifData != null && !pifData.isEmpty()) {
                pifPref.setSummary("✅ PIF loaded — tap to replace");
            } else {
                pifPref.setSummary("⚠️ No PIF loaded — using hardware fingerprint");
            }
        }

        if (keyboxPref != null) {
            if (keyboxData != null && !keyboxData.isEmpty()) {
                keyboxPref.setSummary("✅ Keybox loaded — tap to replace");
            } else {
                keyboxPref.setSummary("⚠️ No Keybox loaded — using hardware attestation");
            }
        }
    }

    // ─── File Picker ─────────────────────────────────────────────────────────

    private void openFilePicker(String mimeType, String type) {
        this.currentType = type;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        filePickerLauncher.launch(intent);
    }

    private void handleFileSelection(Uri uri) {
        Context ctx = getContext();
        if (ctx == null) return;

        try {
            InputStream is = ctx.getContentResolver().openInputStream(uri);
            if (is == null) {
                Toast.makeText(ctx, "❌ Cannot open file", Toast.LENGTH_SHORT).show();
                return;
            }

            String content = new BufferedReader(new InputStreamReader(is))
                    .lines().collect(Collectors.joining("\n"));

            if (content.isEmpty()) {
                Toast.makeText(ctx, "❌ File is empty", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate JSON for PIF
            if (currentType.equals("pif") && !content.trim().startsWith("{")) {
                Toast.makeText(ctx, "❌ Invalid JSON file", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate XML for Keybox
            if (currentType.equals("keybox") && !content.trim().startsWith("<")) {
                Toast.makeText(ctx, "❌ Invalid XML file", Toast.LENGTH_SHORT).show();
                return;
            }

            String dbKey = currentType.equals("pif") ? "lynx_pif_data" : "lynx_keybox_data";
            boolean wrote = Settings.Secure.putString(ctx.getContentResolver(), dbKey, content);

            if (wrote) {
                Toast.makeText(ctx, "✅ " + currentType.toUpperCase() + " Applied — tap Refresh to activate", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(ctx, "❌ Write failed — is WRITE_SECURE_SETTINGS granted?", Toast.LENGTH_LONG).show();
            }

            updateSummaries();

        } catch (Exception e) {
            Toast.makeText(ctx, "❌ Error reading file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ─── Reset ───────────────────────────────────────────────────────────────

    /**
     * Reset: DELETE the keys entirely (null) so the module sees
     * no entry at all — not just an empty string — and falls back
     * to hardware info cleanly.
     */
    private void resetSpoofing() {
        Context ctx = getContext();
        if (ctx == null) return;

        ContentResolver cr = ctx.getContentResolver();

        // Writing null removes the key from Settings.Secure entirely
        Settings.Secure.putString(cr, "lynx_pif_data", null);
        Settings.Secure.putString(cr, "lynx_keybox_data", null);

        Toast.makeText(ctx, "🗑️ Spoofing data cleared — tap Refresh to apply", Toast.LENGTH_LONG).show();
        updateSummaries();
    }

    // ─── Refresh ─────────────────────────────────────────────────────────────

    /**
     * Refresh flow:
     * 1. Tell GMS phenotype a flag changed
     * 2. Tell Lynx module to re-read Settings.Secure (custom action)
     * 3. Kill GMS background processes so they restart and re-read data
     * 4. Small delay then kill again (GMS sometimes respawns too fast)
     *
     * No root needed — priv-app + FORCE_STOP_PACKAGES handles process killing.
     */
    private void refreshSpoof() {
        Context ctx = getContext();
        if (ctx == null) return;

        try {
            // Step 1: Notify module to re-read data immediately
            Intent moduleRefresh = new Intent("com.lynx.module.REFRESH");
            ctx.sendBroadcast(moduleRefresh);

            // Step 2: GMS phenotype signal
            Intent phenotype = new Intent("com.google.android.gms.phenotype.FLAG_UPDATE");
            phenotype.setPackage("com.google.android.gms");
            ctx.sendBroadcast(phenotype);

            // Step 3: Kill GMS processes so they restart and read fresh data
            killGmsProcesses(ctx);

            // Step 4: Kill again after 1.5s — GMS respawns quickly
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                killGmsProcesses(ctx);
                Toast.makeText(ctx, "🔄 GMS restarted — data is now active", Toast.LENGTH_SHORT).show();
                updateSummaries();
            }, 1500);

            Toast.makeText(ctx, "⏳ Refreshing GMS...", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(ctx, "⚠️ Refresh partial: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void killGmsProcesses(Context ctx) {
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return;

        String[] targets = {
            "com.google.android.gms",           // Play Services
            "com.google.android.gms.unstable",  // DroidGuard / SafetyNet
            "com.android.vending"               // Play Store
        };

        for (String pkg : targets) {
            try {
                am.killBackgroundProcesses(pkg);
            } catch (Exception ignored) {}
        }
    }
}
