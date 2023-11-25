package com.example.ipynbviewerpro;

import android.util.Log;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PostDataTask implements Runnable{
    private final String urlStr;
    private final String data;
    private final PostResponseCallback callback;

    public PostDataTask(String urlStr, String data, PostResponseCallback callback) {
        this.urlStr = urlStr;
        this.data = data;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(data.getBytes("UTF-8"));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                callback.onResponse("POST request sent successfully");
            } else {
                callback.onError(new Exception("POST request failed with response code: " + responseCode));
            }
        } catch (Exception e) {
            callback.onError(e);
        } finally {
            // Make sure to close any resources like HttpURLConnection
        }
    }
}

