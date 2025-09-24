package com.sencrop.capacitor.intercom;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Utility to enable/disable Intercom SDK components that could be auto-started by the system
 * (e.g., Activities or Services declared by the SDK), effectively deferring their launch
 * until the app explicitly initializes Intercom.
 */
class IntercomComponentController {

    static void disableAutoComponents(Context context) {
        setComponentEnabled(context, "io.intercom.android.sdk.activities.IntercomPostActivity", false);
        setComponentEnabled(context, "io.intercom.android.sdk.push.IntercomPushService", false);
        setComponentEnabled(context, "io.intercom.android.sdk.push.RegistrationIntentService", false);
        setComponentEnabled(context, "io.intercom.android.sdk.push.fcm.IntercomFcmMessengerService", false);
        setComponentEnabled(context, "io.intercom.android.sdk.push.SystemNotificationService", false);
    }

    static void enableAutoComponents(Context context) {
        setComponentEnabled(context, "io.intercom.android.sdk.activities.IntercomPostActivity", true);
        setComponentEnabled(context, "io.intercom.android.sdk.push.IntercomPushService", true);
        setComponentEnabled(context, "io.intercom.android.sdk.push.RegistrationIntentService", true);
        setComponentEnabled(context, "io.intercom.android.sdk.push.fcm.IntercomFcmMessengerService", true);
        setComponentEnabled(context, "io.intercom.android.sdk.push.SystemNotificationService", true);
    }

    private static void setComponentEnabled(Context context, String className, boolean enabled) {
        try {
            ComponentName componentName = new ComponentName(context, Class.forName(className));
            int newState = enabled
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            context.getPackageManager().setComponentEnabledSetting(
                componentName,
                newState,
                PackageManager.DONT_KILL_APP
            );
        } catch (Throwable ignored) {
        }
    }
}


