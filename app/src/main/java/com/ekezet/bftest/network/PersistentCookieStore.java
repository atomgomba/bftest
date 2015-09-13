package com.ekezet.bftest.network;

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.ekezet.bftest.Config;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Global persistent cookie store
 */
public class PersistentCookieStore implements CookieStore
{
    public final String PREFS_PREFIX = "cookie_";

    private List<HttpCookie> mCookies = new ArrayList();

    public PersistentCookieStore()
    {
        loadCookies();
    }

    @Override
    public void add(URI uri, HttpCookie httpCookie)
    {
        if (uri != null)
        {
            return;
        }
        mCookies.add(httpCookie);
        saveCookies();
    }

    @Override
    public List<HttpCookie> get(URI uri)
    {
        return mCookies;
    }

    @Override
    public List<HttpCookie> getCookies()
    {
        return mCookies;
    }

    @Override
    public List<URI> getURIs()
    {
        return new ArrayList();
    }

    @Override
    public boolean remove(URI uri, HttpCookie httpCookie)
    {
        boolean result = mCookies.remove(httpCookie);
        if (result)
        {
            saveCookies();
        }
        return result;
    }

    @Override
    public boolean removeAll()
    {
        mCookies.clear();
        saveCookies();
        return true;
    }

    private void loadCookies()
    {
        SharedPreferences prefs = Config.getPrefs();
        Set<String> cookiesSet;
        String[] pair;
        HashMap<String, HttpCookie> httpCookies;
        cookiesSet = prefs.getStringSet(PREFS_PREFIX + "cookies", null);
        if (null == cookiesSet || 0 == cookiesSet.size())
        {
            return;
        }
        String[] cookies = new String[cookiesSet.size()];
        cookiesSet.toArray(cookies);
        httpCookies = new HashMap();
        for (int n = 0, N = cookies.length; n < N; n++)
        {
            pair = TextUtils.split(cookies[n], "=");
            if (2 != pair.length || 0 == pair[0].length() || 0 == pair[1].length())
            {
                continue;
            }
            Log.d(getClass().getSimpleName(), String.format("Loading cookie: %s", cookies[n]));
            httpCookies.put(pair[0], new HttpCookie(pair[0], pair[1]));
        }
        mCookies.addAll(httpCookies.values());
    }

    private boolean saveCookies()
    {
        if (0 == mCookies.size())
        {
            return false;
        }
        SharedPreferences.Editor edit = Config.getPrefs().edit();
        Set<String> sCookies = new HashSet();
        String pair;
        for (HttpCookie cookie : mCookies)
        {
            pair = String.format("%s=%s", cookie.getName(), cookie.getValue());
            Log.d(getClass().getSimpleName(), String.format("Persisting cookie: %s", pair));
            sCookies.add(pair);
        }
        edit.putStringSet(PREFS_PREFIX + "cookies", sCookies);
        return edit.commit();
    }

}
