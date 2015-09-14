package com.ekezet.bftest.network;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by kiri on 2015.09.12..
 */
public abstract class HTTPDownloader extends AsyncTask<HTTP.DownloadParams, Integer, HTTP.Response>
{
    private int mContentLength = 0;

    protected HTTP.Response downloadFile(String url, File destFile)
    {
        Log.d(getClass().getSimpleName(), String.format("SAVING %s TO %s", url, destFile.getAbsolutePath()));
        try
        {
            HttpURLConnection connection = HTTP.getBaseConnection(new URL(url));
            //connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            connection.connect();

            int statusCode = connection.getResponseCode();
            /*
            if (statusCode == 301 || statusCode == 302) {
                String newLocation = connection.getHeaderField("Location");
                Log.d(getClass().getSimpleName(), "Redirect: " + newLocation);
                connection.disconnect();
                connection = HTTP.getBaseConnection(new URL(newLocation));
                connection.setRequestMethod("GET");
                connection.setInstanceFollowRedirects(false);
                connection.connect();
            }
            */

            FileOutputStream fos = new FileOutputStream(destFile);
            InputStream is;

            try
            {
                is = connection.getInputStream();
            } catch (FileNotFoundException e)
            {
                e.printStackTrace();
                String error = String.format("%s %s", statusCode, connection.getResponseMessage());
                Log.e("HTTP", error);
                connection.disconnect();
                return new HTTP.Response(null, error, statusCode);
            }

            mContentLength = connection.getContentLength();
            if (mContentLength < 1) {
                is.close();
                connection.disconnect();
                return new HTTP.Response(null, "Invalid content length");
            }

            int totalRead = 0;
            int len;
            final byte buffer[] = new byte[16 * 1024];
            while ((len = is.read(buffer)) > 0) {
                totalRead += len;
                publishProgress(totalRead);
                fos.write(buffer, 0, len);
            }
            fos.flush();
            fos.close();
            HTTP.storeCookies(connection);
            connection.disconnect();
        } catch (IOException e)
        {
            e.printStackTrace();
            return new HTTP.Response(null, e.getMessage());
        }
        return new HTTP.Response();
    }

    protected int getContentLength() {
        return mContentLength;
    }

    protected abstract HTTP.Response doInBackground(HTTP.DownloadParams... params);
}
