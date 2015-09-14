package com.ekezet.bftest.network;

import android.os.AsyncTask;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Created by kiri on 2015.09.13..
 */
public abstract class HTTPUploader extends AsyncTask<HTTP.UploadParams, Integer, HTTP.Response>
{
    private abstract class DataOutputListener
    {
        public abstract void update(int written);
    }

    private class ListeningDataOutputStream extends DataOutputStream
    {
        private int mWritten = 0;

        private DataOutputListener mListener;

        public ListeningDataOutputStream(OutputStream out)
        {
            super(out);
        }

        @Override
        public void write(byte[] buffer, int offset, int count) throws IOException
        {
            super.write(buffer, offset, count);
            mWritten += count;
            mListener.update(mWritten);
        }

        @Override
        public void write(int oneByte) throws IOException
        {
            super.write(oneByte);
            mWritten += 1;
            mListener.update(mWritten);
        }

        public void setListener(DataOutputListener listener) {
            mListener = listener;
        }
    }

    private int mContentLength;

    protected HTTP.Response uploadFiles(String url, List<File> files)
    {
        final String CRLF = "\r\n";
        String boundary = "--boundary-" + String.valueOf(System.currentTimeMillis());
        String dispoHeader = "Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"" + CRLF;

        Log.d(getClass().toString(), "Uploading to: " + url);

        int total = 0;
        for (File file : files)
        {
            total += file.length();
        }
        mContentLength = total;

        try
        {
            HttpURLConnection connection = HTTP.getBaseConnection(new URL(url));
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setRequestProperty("Content-Type",
                    String.format("multipart/form-data; boundary=\"%s\"", boundary.substring(2)));
            connection.connect();

            final ListeningDataOutputStream ldos = new ListeningDataOutputStream(connection.getOutputStream());
            ldos.setListener(new DataOutputListener() {
                @Override
                public void update(int written)
                {
                    publishProgress(written);
                }
            });
            DataInputStream dis;
            byte[] data;

            for (File file : files)
            {
                Log.d(getClass().toString(), "Processing file: " + file.getAbsolutePath());
                if (!file.exists() || !file.isFile())
                {
                    Log.w(getClass().getSimpleName(), "File not found: " + file.getAbsolutePath());
                    continue;
                }
                dis = new DataInputStream(new FileInputStream(file));
                data = new byte[(int) file.length()];
                dis.readFully(data);
                dis.close();
                ldos.writeBytes(boundary + CRLF);
                ldos.writeBytes(String.format(dispoHeader, file.getName(), file.getName()));
                ldos.writeBytes("Content-Type: application/octet-stream" + CRLF);
                ldos.writeBytes("Content-Transfer-Encoding: binary" + CRLF);
                ldos.writeBytes(CRLF);
                ldos.write(data);
                ldos.writeBytes(CRLF);
                ldos.writeBytes(boundary + CRLF);
            }

            ldos.writeBytes(boundary + "--" + CRLF);
            ldos.flush();
            ldos.close();

            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK && status != HttpURLConnection.HTTP_CREATED)
            {
                connection.disconnect();
                return new HTTP.Response(null, connection.getResponseMessage(), connection.getResponseCode());
            }

            HTTP.storeCookies(connection);
            connection.disconnect();
        } catch (IOException e)
        {
            e.printStackTrace();
            return new HTTP.Response(null, e.getMessage());
        }

        return new HTTP.Response();
    }

    protected abstract HTTP.Response doInBackground(HTTP.UploadParams... params);

    protected int getContentLength()
    {
        return mContentLength;
    }
}
