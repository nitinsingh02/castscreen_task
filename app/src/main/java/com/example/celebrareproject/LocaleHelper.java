package com.example.celebrareproject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public class LocaleHelper {
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_LANG = "app_language";

    public static void persistLanguage(Context ctx, String language) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANG, language).apply();
    }

    public static String getPersistedLanguage(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANG, ""); // default = "" (auto)
    }

    public static Context setLocale(Context context, String language) {
        if (language == null) language = "";
        if (language.isEmpty()) {
            // "Auto" -> use device default
            return updateResources(context, Locale.getDefault());
        } else {
            Locale locale = new Locale(language);
            Locale.setDefault(locale);
            return updateResources(context, locale);
        }
    }

    private static Context updateResources(Context context, Locale locale) {
        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            context = context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            res.updateConfiguration(config, res.getDisplayMetrics());
        }
        return context;
    }

}

