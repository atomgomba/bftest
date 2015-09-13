package com.ekezet.bftest.network;

import android.util.Log;

import com.ekezet.bftest.Config;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HTTP
{
    public final static String BOUNDARY = "--xxxxxxx";

    public static class Response
    {
        private String mErrorMessage = null;
        private String mData = null;
        private int mCode = 200;

        public Response(String data, String error, int code)
        {
            mData = data;
            mCode = code;
            mErrorMessage = error;
        }

        public Response(String data, String error)
        {
            mData = data;
            mErrorMessage = error;
        }

        public Response(String data)
        {
            mData = data;
        }

        public Response()
        {
        }

        public String getResult()
        {
            if (mErrorMessage != null)
            {
                return mErrorMessage;
            } else if (mData == null || 0 == String.valueOf(mData).length())
            {
                return String.valueOf(mCode);
            }
            return mData;
        }

        public String getErrorMessage()
        {
            return mErrorMessage;
        }

        public int getCode()
        {
            return mCode;
        }

        public String getData()
        {
            return mData;
        }
    }

    public static class PostParams
    {
        private String mUrl;
        private Object mParams;
        private String mMethod;

        public PostParams(String url, Object params)
        {
            mUrl = url;
            mParams = params;
            mMethod = "POST";
        }

        public PostParams(String url, Object params, String method)
        {
            mUrl = url;
            mParams = params;
            mMethod = method;
        }

        public String getUrl()
        {
            return mUrl;
        }

        public Object getParams()
        {
            return mParams;
        }

        public String getMethod()
        {
            return mMethod;
        }
    }

    public static class UploadParams
    {
        private String mUrl;
        private List<File> mFiles;

        public UploadParams(String url, List<File> files)
        {
            mUrl = url;
            mFiles = files;
        }

        public List<File> getFiles()
        {
            return mFiles;
        }

        public String getUrl()
        {
            return mUrl;
        }
    }

    public static class DownloadParams
    {
        private String mUrl;
        private File mDestFile;

        public DownloadParams(String url, File destFile)
        {
            mUrl = url;
            mDestFile = destFile;
        }

        public String getUrl()
        {
            return mUrl;
        }

        public File getDestFile()
        {
            return mDestFile;
        }
    }

    public static Response get(String url, boolean json)
    {
        Log.d("HTTP", "GET " + url);
        StringBuffer buffer = new StringBuffer("");
        try
        {
            URL _url = new URL(url);
            HttpURLConnection connection = getBaseConnection(_url);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", json ? "application/json" : "*/*");
            connection.connect();
            InputStream is;
            try
            {
                is = connection.getInputStream();
            } catch (FileNotFoundException e)
            {
                String error = String.format("%s %s", connection.getResponseCode(), connection.getResponseMessage());
                Log.d("HTTP", error);
                connection.disconnect();
                return new Response(null, error, connection.getResponseCode());
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null)
            {
                buffer.append(line);
            }
            Log.d("HTTP", buffer.toString());
            storeCookies(connection);
            connection.disconnect();
        } catch (IOException e)
        {
            e.printStackTrace();
            return new Response(null, e.getMessage());
        }
        return new Response(buffer.toString());
    }

    public static Response post(String url, PostParams params)
    {
        Log.d("HTTP", "POST " + url);
        StringBuffer buffer = new StringBuffer("");
        try
        {
            URL _url = new URL(url);
            HttpURLConnection connection = getBaseConnection(_url);
            connection.setRequestMethod(params.getMethod());
            connection.setDoInput(true);
            connection.setDoOutput(true);
            String body;
            connection.setRequestProperty("Content-Type", "application/json");
            body = encodeJSON(params.getParams());
            connection.connect();

            OutputStream os = connection.getOutputStream();
            Log.d("HTTP", "Request body: " + body);
            os.write(body.getBytes("UTF-8"));
            os.flush();
            os.close();

            InputStream is;
            try
            {
                is = connection.getInputStream();
            } catch (FileNotFoundException e)
            {
                String error = String.format("%s %s", connection.getResponseCode(), connection.getResponseMessage());
                connection.disconnect();
                return new Response(null, error, connection.getResponseCode());
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null)
            {
                buffer.append(line);
            }
            storeCookies(connection);
            connection.disconnect();
        } catch (IOException e)
        {
            e.printStackTrace();
            return new Response(null, e.getMessage());
        }
        return new Response(buffer.toString());
    }

    /*
    public static StringBuffer upload(String url)
    {
        StringBuffer buffer = new StringBuffer("");
        try
        {
            URL _url = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) _url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", Config.getUserAgent());
            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + BOUNDARY);
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.connect();

            OutputStream os = connection.getOutputStream();

            InputStream is = connection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null)
            {
                buffer.append(line);
            }

        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return buffer;
    }
    */

    public static HttpURLConnection getBaseConnection(URL url) throws IOException
    {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", Config.getUserAgent());
        connection.setUseCaches(false);
        return connection;
    }

    private static String encodeJSON(Object params)
    {
        if (params instanceof HashMap)
        {
            HashMap<String, String> map = (HashMap) params;
            JSONObject obj = new JSONObject(map);
            return obj.toString();
        }
        if (params instanceof List) {
            JSONArray obj = new JSONArray();
            List<HashMap<String, String>> list = (ArrayList) params;
            for (HashMap map : list) {
                obj.put(new JSONObject(map));
            }
            return obj.toString();
        }
        if ((params instanceof JSONObject) || (params instanceof JSONArray)) {
            return params.toString();
        }
        return "";
    }

    public static void storeCookies(URLConnection connection)
    {
        CookieManager cookieMan = Config.getCookieManager();
        Map<String, List<String>> headers = connection.getHeaderFields();
        List<String> cookies = headers.get("Set-Cookie");
        if (cookies != null)
        {
            Log.d("HTTP", "Processing cookies...");
            for (String cookie : cookies)
            {
                HttpCookie hc = HttpCookie.parse(cookie).get(0);
                if (cookieMan.getCookieStore().getCookies().contains(cookie))
                {
                    Log.d("HTTP", "Skipping cookie: " + hc);
                    continue;
                }
                Log.d("HTTP", "Storing cookie: " + hc);
                cookieMan.getCookieStore().add(null, hc);
            }
        }
    }
}
