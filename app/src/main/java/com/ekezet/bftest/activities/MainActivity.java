package com.ekezet.bftest.activities;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import com.ekezet.bftest.Config;
import com.ekezet.bftest.R;
import com.ekezet.bftest.network.HTTP;
import com.ekezet.bftest.network.HTTPDownloader;
import com.ekezet.bftest.network.HTTPUploader;
import com.ekezet.bftest.views.ConsoleTextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends Activity
{
    private JSONObject mSubmitResponse;
    private JSONArray mDocsResponse;

    private class HTTPGet extends AsyncTask<String, Void, HTTP.Response>
    {
        @Override
        protected HTTP.Response doInBackground(String... urls)
        {
            String url = urls[0];
            addLine("<strong>GET </strong>" + url);
            return HTTP.get(url, true);
        }

        @Override
        protected void onPostExecute(HTTP.Response response)
        {
            addLine("<strong>Result: </strong>" + response.getResult());
        }
    }

    private class HTTPPost extends AsyncTask<HTTP.PostParams, Void, HTTP.Response>
    {
        @Override
        protected HTTP.Response doInBackground(HTTP.PostParams... params)
        {
            HTTP.PostParams p = params[0];
            addLine(String.format("<strong>%s </strong>%s", p.getMethod(), p.getUrl()));
            return HTTP.post(p.getUrl(), p);
        }

        @Override
        protected void onPostExecute(HTTP.Response response)
        {
            addLine("<strong>Result: </strong>" + response.getResult());
        }
    }

    private class Downloader extends HTTPDownloader
    {
        @Override
        protected HTTP.Response doInBackground(HTTP.DownloadParams... params)
        {
            HTTP.DownloadParams arg = params[0];
            addLine("<strong>SAVING </strong>" + arg.getUrl());
            addLine("");
            return downloadFile(arg.getUrl(), arg.getDestFile());
        }

        @Override
        protected void onPostExecute(HTTP.Response response)
        {
            addLine("<strong>Result: </strong>" + response.getResult());
        }

        @Override
        protected void onProgressUpdate(Integer... values)
        {
            int length = getContentLength();
            float percent = Float.valueOf(values[0]) / Float.valueOf(length) * 100f;
            replaceLine(String.format("%d/%d (<strong>%.2f%%</strong>)", values[0], length, percent));
        }
    }

    private class Uploader extends HTTPUploader
    {
        @Override
        protected HTTP.Response doInBackground(HTTP.UploadParams... params)
        {
            addLine("<strong>Uploading...</strong>");
            HTTP.UploadParams arg = params[0];
            addLine("<strong>POST </strong>" + arg.getUrl());
            addLine("");
            return uploadFiles(arg.getUrl(), arg.getFiles());
        }

        @Override
        protected void onPostExecute(HTTP.Response response)
        {
            addLine("<strong>Result: </strong>" + response.getResult());
        }

        @Override
        protected void onProgressUpdate(Integer... values)
        {
            int length = getContentLength();
            float percent = Float.valueOf(values[0]) / Float.valueOf(length) * 100f;
            replaceLine(String.format("%d/%d (<strong>%.2f%%</strong>)", values[0], length, percent));
        }
    }

    private ConsoleTextView mConsole;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        mConsole = (ConsoleTextView) findViewById(R.id.console);

        addLine("<strong>User-Agent: </strong>" + Config.getUserAgent());
        signUp();
    }

    private void signUp()
    {
        String url = Config.URL_API_BASE + "/me";

        HTTPPost client = new HTTPPost()
        {
            @Override
            protected void onPreExecute()
            {
                addLine(String.format("<strong>SIGN UP: </strong>%s:%s", Config.USER_EMAIL, Config.USER_PASSWORD));
                super.onPreExecute();
            }

            @Override
            protected void onPostExecute(HTTP.Response response)
            {
                super.onPostExecute(response);
                // ha már fel van iratkozva, az nem hiba
                if (null != response.getErrorMessage() && HttpURLConnection.HTTP_NOT_ACCEPTABLE != response.getCode())
                {
                    return;
                } else if (HttpURLConnection.HTTP_NOT_ACCEPTABLE != response.getCode())
                {
                    downloadClientSource();
                } else
                {
                    // már létezik a felhasználó, kérjük le a profilt
                    getProfile();
                }
            }
        };

        HashMap<String, String> params = new HashMap();
        params.put("email", Config.USER_EMAIL);
        params.put("pswd", Config.USER_PASSWORD);
        HTTP.PostParams postParams = new HTTP.PostParams(url, params);
        client.execute(postParams);
    }

    private void getProfile()
    {
        String url = Config.URL_API_BASE + "/me";

        HTTPGet client = new HTTPGet()
        {
            @Override
            protected void onPostExecute(HTTP.Response response)
            {
                super.onPostExecute(response);
                if (null != response.getErrorMessage())
                {
                    return;
                }
                downloadClientSource();
            }
        };

        client.execute(url);
    }

    private void downloadClientSource()
    {
        String url = Config.URL_CLIENT_SOURCE;

        Downloader client = new Downloader()
        {
            @Override
            protected void onPostExecute(HTTP.Response response)
            {
                super.onPostExecute(response);
                if (null != response.getErrorMessage())
                {
                    return;
                }
                downloadServerSource();
            }
        };
        File destFile = new File(Config.dataCachePath, Config.FILENAME_CLIENT_SOURCE);
        HTTP.DownloadParams params = new HTTP.DownloadParams(url, destFile);
        client.execute(params);
    }

    private void downloadServerSource()
    {
        String url = Config.URL_SERVER_SOURCE;

        Downloader client = new Downloader()
        {
            @Override
            protected void onPostExecute(HTTP.Response response)
            {
                super.onPostExecute(response);
                if (null != response.getErrorMessage())
                {
                    return;
                }
                getDocs();
            }
        };
        File destFile = new File(Config.dataCachePath, Config.FILENAME_SERVER_SOURCE);
        HTTP.DownloadParams params = new HTTP.DownloadParams(url, destFile);
        client.execute(params);
    }

    private void getDocs()
    {
        String url = Config.URL_API_BASE + "/doc";

        HTTPGet client = new HTTPGet()
        {
            @Override
            protected void onPostExecute(HTTP.Response response)
            {
                super.onPostExecute(response);
                if (null != response.getErrorMessage())
                {
                    return;
                }
                try
                {
                    mDocsResponse = new JSONArray(response.getData());
                } catch (JSONException e)
                {
                    e.printStackTrace();
                    addLine("<strong>ERROR: </strong>Invalid JSON");
                    return;
                }
                deleteDocs();
            }
        };

        client.execute(url);
    }

    private void deleteDocs()
    {
        if (null == mDocsResponse)
        {
            Log.e(getClass().getSimpleName(), "List of docs is unavailable");
            return;
        }

        if (0 == mDocsResponse.length()) {
            Log.i(getClass().getSimpleName(), "No existing docs");
            addLine("Nothing to delete.");
            uploadDocs();
            return;
        }

        String url = Config.URL_API_BASE + "/doc";

        HTTPPost client = new HTTPPost()
        {
            @Override
            protected void onPostExecute(HTTP.Response response)
            {
                super.onPostExecute(response);
                if (null != response.getErrorMessage())
                {
                    return;
                }
                uploadDocs();
            }
        };

        List<HashMap<String, String>> params = new ArrayList();
        HashMap<String, String> doc;
        for (int i = 0, I = mDocsResponse.length(); i < I; i++) {
            doc = new HashMap();
            try
            {
                doc.put("uuid", mDocsResponse.getJSONObject(i).getString("uuid"));
            } catch (JSONException e)
            {
                e.printStackTrace();
            }
            params.add(doc);
        }

        HTTP.PostParams postParams = new HTTP.PostParams(url, params, "DELETE");
        client.execute(postParams);
    }

    private void uploadDocs()
    {
        String url = Config.URL_API_BASE + "/doc";

        Uploader client = new Uploader()
        {
            @Override
            protected void onPostExecute(HTTP.Response response)
            {
                super.onPostExecute(response);
                if (null != response.getErrorMessage())
                {
                    return;
                }
                getSubmitToken();
            }
        };

        List<File> files = new ArrayList();
        files.add(new File(Config.dataCachePath, Config.FILENAME_CLIENT_SOURCE));
        files.add(new File(Config.dataCachePath, Config.FILENAME_SERVER_SOURCE));
        HTTP.UploadParams params = new HTTP.UploadParams(url, files);
        client.execute(params);
    }

    private void getSubmitToken()
    {
        String url = Config.URL_API_BASE + "/submit";

        HTTPGet client = new HTTPGet()
        {
            @Override
            protected void onPostExecute(HTTP.Response response)
            {
                super.onPostExecute(response);
                if (null != response.getErrorMessage())
                {
                    return;
                }
                try
                {
                    mSubmitResponse = new JSONObject(response.getData());
                } catch (JSONException e)
                {
                    e.printStackTrace();
                    addLine("<strong>ERROR: </strong>Invalid JSON");
                    return;
                }
                postSubmitToken();
            }
        };

        client.execute(url);
    }

    private void postSubmitToken()
    {
        if (!Config.FINALIZE || null == mSubmitResponse)
        {
            if (null == mSubmitResponse)
            {
                Log.e(getClass().getSimpleName(), "Submit token is unavailable");
            }
            return;
        }

        String url = Config.URL_API_BASE + "/submit";

        HTTPPost client = new HTTPPost()
        {
            @Override
            protected void onPostExecute(HTTP.Response response)
            {
                super.onPostExecute(response);
                if (null != response.getErrorMessage())
                {
                    return;
                }
                addLine("<strong>Token successfully sent.</strong>");
            }
        };

        HashMap<String, String> params = new HashMap();
        try
        {
            params.put("uuid", mSubmitResponse.getString("uuid"));
        } catch (JSONException e)
        {
            e.printStackTrace();
            addLine("<strong>ERROR: </strong>Invalid JSON");
            return;
        }
        HTTP.PostParams postData = new HTTP.PostParams(url, params);
        client.execute(postData);
    }

    private void addLine(final String s)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                mConsole.addLine(s);
            }
        });
    }

    private void replaceLine(final String s)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                mConsole.replaceLine(s);
            }
        });
    }
}
