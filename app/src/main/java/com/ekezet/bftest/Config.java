package com.ekezet.bftest;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.util.Log;

import com.ekezet.bftest.network.PersistentCookieStore;

import java.io.File;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

public class Config
{
    public static final String USER_EMAIL = "karcsi@ekezet.com";
    public static final String USER_PASSWORD = "teszt";

    public static final String PREFS_NAME = "prefs";

    public static final String URL_API_BASE = "http://192.168.9.223:8090";
    //public static final String URL_API_BASE = "http://mobilechallenge.big.hu";

    public static final String URL_CLIENT_SOURCE = "https://github.com/kevinsawicki/http-request/archive/master.zip";

    public static final String FILENAME_CLIENT_SOURCE = "client-source.zip";

    public static final boolean FINALIZE = false;

    public static File dataCachePath = null;

    private static String sVersionName = null;
    private static int sVersionCode = -1;
    private static String sPackageName = null;
    private static boolean sIsEmulator = Build.FINGERPRINT.contains("generic");
    private static String sUserAgent;

    private static CookieManager sCookieMan;

    private static SharedPreferences sPrefs = null;
    private static boolean sInitialized = false;

    public static void initialize(final Context context)
    {
        if (sInitialized)
        {
            return;
        }
        init(context);
        sInitialized = true;
    }

    private static void init(Context context)
    {
        sPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        try
        {
            final PackageManager pm = context.getPackageManager();
            sPackageName = context.getPackageName();
            final PackageInfo inf = pm.getPackageInfo(sPackageName, 0);
            sVersionName = inf.versionName;
            sVersionCode = inf.versionCode;
            sUserAgent = String.format("%s/%d (%s)", sPackageName, sVersionCode, sVersionName);
            Log.i("Config", String.format("App version: %s (%d)", sVersionName, sVersionCode));
        } catch (NameNotFoundException e)
        {
            sVersionName = "?.?";
            e.printStackTrace();
        }
        dataCachePath = (context.getExternalCacheDir()).getAbsoluteFile();
        sCookieMan = new CookieManager(new PersistentCookieStore(), CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(sCookieMan);
    }

    public static SharedPreferences getPrefs()
    {
        return sPrefs;
    }

    public static String getVersionName()
    {
        return sVersionName;
    }

    public static int getVersionCode()
    {
        return sVersionCode;
    }

    public static String getPackageName()
    {
        return sPackageName;
    }

    public static boolean isEmulator()
    {
        return sIsEmulator;
    }

    public static String getUserAgent() {
        return sUserAgent;
    }

    public static CookieManager getCookieManager() {
        return sCookieMan;
    }
}
