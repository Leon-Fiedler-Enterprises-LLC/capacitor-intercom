package com.sencrop.capacitor.intercom;

import android.app.ActivityManager;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import io.intercom.android.sdk.Intercom;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONObject;

/**
 * Initializes Intercom as early as possible to avoid crashes when Intercom activities
 * are launched by the system (e.g., from notifications) before the app code runs.
 */
public class IntercomInitProvider extends ContentProvider {

    private static final String META_ANDROID_API_KEY = "com.sencrop.capacitor.intercom.ANDROID_API_KEY";
    private static final String META_APP_ID = "com.sencrop.capacitor.intercom.APP_ID";

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context == null) {
            return false;
        }

        try {
            // Run only in main process
            if (!isMainProcess(context)) {
                return true;
            }

            // Respect opt-in flag for early initialization. Default: do NOT early-init.
            if (!isEarlyInitEnabled(context)) {
                // Proactively disable Intercom auto components so they can't be launched early
                IntercomComponentController.disableAutoComponents(context);
                return true;
            }

            // 1) Try Capacitor assets config (no app manifest changes required)
            String[] keys = readKeysFromCapacitorAssets(context);
            if (keys != null && keys[0] != null && keys[1] != null) {
                Intercom.initialize((Application) context.getApplicationContext(), keys[0], keys[1]);
                IntercomPlugin.markInitialized();
                // Enable components now that Intercom is initialized
                IntercomComponentController.enableAutoComponents(context);
                return true;
            }

            // 2) Fallback to manifest meta-data if present (backward compatible)
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            if (bundle != null) {
                String apiKey = bundle.getString(META_ANDROID_API_KEY);
                String appId = bundle.getString(META_APP_ID);
                if (apiKey != null && appId != null) {
                    Intercom.initialize((Application) context.getApplicationContext(), apiKey, appId);
                    IntercomPlugin.markInitialized();
                    IntercomComponentController.enableAutoComponents(context);
                }
            }
        } catch (Throwable ignored) {
            // Swallow to avoid blocking app start; plugin's initialize() can still run later
        }

        return true;
    }

    private boolean isMainProcess(Context context) {
        try {
            int myPid = android.os.Process.myPid();
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return true;
            for (ActivityManager.RunningAppProcessInfo proc : am.getRunningAppProcesses()) {
                if (proc != null && proc.pid == myPid) {
                    return context.getPackageName().equals(proc.processName);
                }
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    private String[] readKeysFromCapacitorAssets(Context context) {
        try {
            AssetManager am = context.getAssets();
            InputStream is = am.open("capacitor.config.json");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();

            JSONObject root = new JSONObject(sb.toString());
            JSONObject plugins = root.optJSONObject("plugins");
            if (plugins == null) return null;
            JSONObject intercom = plugins.optJSONObject("Intercom");
            if (intercom == null) return null;

            String apiKey = intercom.optString("android_apiKey", null);
            String appId = intercom.optString("appId", null);
            if (apiKey == null || appId == null) {
                // Alternative field names fallback
                String altAppId = intercom.optString("androidAppId", null);
                if (apiKey != null && altAppId != null) {
                    appId = altAppId;
                }
            }
            if (apiKey != null && appId != null) {
                return new String[]{apiKey, appId};
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private boolean isEarlyInitEnabled(Context context) {
        // Default false unless explicitly enabled in capacitor.config.json at plugins.Intercom.enableEarlyInit: true
        try {
            AssetManager am = context.getAssets();
            InputStream is = am.open("capacitor.config.json");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();

            JSONObject root = new JSONObject(sb.toString());
            JSONObject plugins = root.optJSONObject("plugins");
            if (plugins == null) return false;
            JSONObject intercom = plugins.optJSONObject("Intercom");
            if (intercom == null) return false;
            return intercom.optBoolean("enableEarlyInit", false);
        } catch (Throwable ignored) {
        }
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}


