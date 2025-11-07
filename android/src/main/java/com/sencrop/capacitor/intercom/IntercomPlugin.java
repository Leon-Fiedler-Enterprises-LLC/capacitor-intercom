package com.sencrop.capacitor.intercom;

import android.app.Application;
import android.content.Context;
import androidx.annotation.NonNull;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import io.intercom.android.sdk.Intercom;
import io.intercom.android.sdk.IntercomContent;
import io.intercom.android.sdk.IntercomError;
import io.intercom.android.sdk.IntercomSpace;
import io.intercom.android.sdk.IntercomStatusCallback;
import io.intercom.android.sdk.UserAttributes;
import io.intercom.android.sdk.identity.Registration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

@CapacitorPlugin(name = "Intercom")
public class IntercomPlugin extends Plugin {

    private static volatile boolean isInitialized = false;

    public static void markInitialized() {
        isInitialized = true;
    }

    @Override
    public void load() {
        // Do not auto-initialize; allow explicit initialize() to control timing
    }

    private static Map<String, Object> mapFromJSON(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        Iterator<String> keysIter = jsonObject.keys();
        while (keysIter.hasNext()) {
            String key = keysIter.next();
            Object value = getObject(jsonObject.opt(key));
            if (value != null) {
                map.put(key, value);
            }
        }
        return map;
    }

    private static Object getObject(Object value) {
        if (value instanceof JSONObject) {
            value = mapFromJSON((JSONObject) value);
        } else if (value instanceof JSONArray) {
            value = listFromJSON((JSONArray) value);
        }
        return value;
    }

    private static List<Object> listFromJSON(JSONArray jsonArray) {
        List<Object> list = new ArrayList<>();
        for (int i = 0, count = jsonArray.length(); i < count; i++) {
            Object value = getObject(jsonArray.opt(i));
            if (value != null) {
                list.add(value);
            }
        }
        return list;
    }

    @PluginMethod
    public void initialize(PluginCall call) {
        if (!ensureInitialized(call)) {
            return;
        }

        try {
            Intercom.client().handlePushMessage();
        } catch (Throwable ignored) {}
        try {
            Context context = getContext();
            if (context != null) {
                IntercomComponentController.enableAutoComponents(context);
            }
        } catch (Throwable ignored) {}

        call.resolve();
    }

    @PluginMethod
    public void loginIdentifiedUser(PluginCall call) {
        if (!ensureInitialized(call)) {
            return;
        }
        String email = call.getString("email");
        String userId = call.getString("userId");
        String userHash = call.getString("userHash");

        if (userHash != null && userHash.length() > 0) {
            Intercom.client().setUserHash(userHash);
        }

        Registration registration = Registration.create();

        if (email != null && email.length() > 0) {
            registration = registration.withEmail(email);
        }
        if (userId != null && userId.length() > 0) {
            registration = registration.withUserId(userId);
        }

        Intercom
            .client()
            .loginIdentifiedUser(
                registration,
                new IntercomStatusCallback() {
                    @Override
                    public void onSuccess() {
                        call.resolve();
                    }

                    @Override
                    public void onFailure(@NonNull IntercomError intercomError) {
                        call.reject("Intercom error : " + intercomError.getErrorMessage());
                    }
                }
            );
    }

    @PluginMethod
    public void loginUnidentifiedUser(PluginCall call) {
        if (!ensureInitialized(call)) {
            return;
        }
        Intercom
            .client()
            .loginUnidentifiedUser(
                new IntercomStatusCallback() {
                    @Override
                    public void onSuccess() {
                        call.resolve();
                    }

                    @Override
                    public void onFailure(@NonNull IntercomError intercomError) {
                        call.reject("Intercom error : " + intercomError.getErrorMessage());
                    }
                }
            );
    }

    @PluginMethod
    public void updateUser(PluginCall call) {
        if (!ensureInitialized(call)) {
            return;
        }
        String email = call.getString("email");
        String phone = call.getString("phone");
        String name = call.getString("name");
        String language = call.getString("language");

        UserAttributes userAttributes = new UserAttributes.Builder()
            .withName(name)
            .withEmail(email)
            .withPhone(phone)
            .withLanguageOverride(language)
            .build();
        Intercom
            .client()
            .updateUser(
                userAttributes,
                new IntercomStatusCallback() {
                    @Override
                    public void onSuccess() {
                        call.resolve();
                    }

                    @Override
                    public void onFailure(@NonNull IntercomError intercomError) {
                        call.reject("Intercom error : " + intercomError.getErrorMessage());
                    }
                }
            );
    }

    @PluginMethod
    public void setCustomAttributes(PluginCall call) {
        if (!ensureInitialized(call)) {
            return;
        }
        Map<String, Object> attributes = mapFromJSON(call.getObject("attributes"));

        UserAttributes.Builder userAttributesBuilder = new UserAttributes.Builder();

        for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
            String key = attribute.getKey();
            Object value = attribute.getValue();
            userAttributesBuilder = userAttributesBuilder.withCustomAttribute(key, value);
        }

        UserAttributes userAttributes = userAttributesBuilder.build();
        Intercom
            .client()
            .updateUser(
                userAttributes,
                new IntercomStatusCallback() {
                    @Override
                    public void onSuccess() {
                        call.resolve();
                    }

                    @Override
                    public void onFailure(@NonNull IntercomError intercomError) {
                        call.reject("Intercom error : " + intercomError.getErrorMessage());
                    }
                }
            );
    }

    @PluginMethod
    public void logout(PluginCall call) {
        if (!ensureInitialized(call)) {
            return;
        }
        Intercom.client().logout();
        call.resolve();
    }

    @PluginMethod
    public void logEvent(PluginCall call) {
        if (!ensureInitialized(call)) {
            return;
        }
        String eventName = call.getString("name");
        Map<String, Object> metaData = mapFromJSON(call.getObject("data"));

        if (metaData == null) {
            Intercom.client().logEvent(eventName);
        } else {
            Intercom.client().logEvent(eventName, metaData);
        }

        call.resolve();
    }

    @PluginMethod
    public void displayMessenger(PluginCall call) {
        if (!ensureInitialized(call)) {
            return;
        }
        Intercom.client().present();
        call.resolve();
    }

    @PluginMethod
    public void displayMessageComposer(PluginCall call) {
        if (!ensureInitialized(call)) {
            return;
        }
        String messageContent = call.getString("content");
        if (messageContent == null) {
            Intercom.client().displayMessageComposer();
        } else {
            Intercom.client().displayMessageComposer(messageContent);
        }
        call.resolve();
    }

    @PluginMethod
    public void displayHelpCenter(PluginCall call) {
        if (!ensureInitialized(call)) {
            return;
        }
        Intercom.client().present(IntercomSpace.HelpCenter);
        call.resolve();
    }

    @PluginMethod
    public void hideMessenger(PluginCall call) {
        if (!ensureInitialized(call)) {
            return;
        }
        Intercom.client().hideIntercom();
        call.resolve();
    }

    @PluginMethod
    public void displayLauncher(PluginCall call) {
        if (!ensureInitialized(call)) {
            return;
        }
        Intercom.client().setLauncherVisibility(Intercom.VISIBLE);
        call.resolve();
    }

    @PluginMethod
    public void hideLauncher(PluginCall call) {
        if (!ensureInitialized(call)) {
            return;
        }
        Intercom.client().setLauncherVisibility(Intercom.GONE);
        call.resolve();
    }

    @PluginMethod
    public void displayArticle(PluginCall call) {
        if (!ensureInitialized(call)) {
            return;
        }
        String articleId = call.getString("id");
        Intercom.client().presentContent(new IntercomContent.Article(articleId));
        call.resolve();
    }

    @PluginMethod
    public void displaySurvey(PluginCall call) {
        if (!ensureInitialized(call)) {
            return;
        }
        String surveyId = call.getString("id");
        Intercom.client().presentContent(new IntercomContent.Survey(surveyId));
        call.resolve();
    }

    private boolean ensureInitialized(PluginCall call) {
        if (isInitialized) {
            return true;
        }

        Context context = getContext();
        if (context == null) {
            if (call != null) {
                call.reject("No application context available for Intercom");
            }
            return false;
        }

        String apiKey = getConfig().getString("android_apiKey", null);
        String appId = getConfig().getString("appId", getConfig().getString("androidAppId", null));

        if (apiKey == null || appId == null) {
            if (call != null) {
                call.reject("Missing Intercom configuration");
            }
            return false;
        }

        try {
            Application application = (Application) context.getApplicationContext();
            Intercom.initialize(application, apiKey, appId);
            isInitialized = true;
            IntercomComponentController.enableAutoComponents(context);
            return true;
        } catch (Exception e) {
            if (call != null) {
                call.reject("Could not initialize Intercom: " + e.getMessage());
            }
            return false;
        }
    }
}
