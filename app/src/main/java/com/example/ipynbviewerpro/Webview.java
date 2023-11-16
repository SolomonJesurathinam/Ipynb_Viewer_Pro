package com.example.ipynbviewerpro;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Webview extends AppCompatActivity {

    WebView webView;
    SharedPreferences webPref;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        webPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Uri uri = Uri.parse(getIntent().getStringExtra("filePath"));
                String fileContent = null;
                try {
                    fileContent = readFileContent(uri);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                String finalFileContent = fileContent;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(finalFileContent != null){
                            progressBar.setVisibility(View.GONE);
                            openWebview(finalFileContent);
                        }
                    }
                });
            }
        }).start();

    }

    public static void largeLog(String tag, String content) {
        final int chunkSize = 4000;
        for (int i = 0; i < content.length(); i += chunkSize) {
            int end = Math.min(content.length(), i + chunkSize);
            Log.d(tag, content.substring(i, end));
        }
    }

    public void openWebview(String data){
        String render1 ="file:///android_asset/Render1/ipynbviewer.html";
        String render2 ="file:///android_asset/Render2/index.html";
        webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUseWideViewPort(false);
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT); //fix for render1 loading time
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(true);
        webView.setInitialScale(100);
        webView.addJavascriptInterface(new WebAppInterface(this), "AndroidMessage");
        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                // Call JavaScript function here
                sendDataToWebView(webView,data);
            }
        });

        if(webPref.getString("renderKey","Real").equalsIgnoreCase("Real")){
            webView.loadUrl(render1);
        }
        else if(webPref.getString("renderKey","Real").equalsIgnoreCase("Basic")){
            webView.loadUrl(render2);
        }
    }

    private void sendTextDataToWebView(WebView webView1, String data, int chunkSize) {
        for (int i = 0; i < data.length(); i += chunkSize) {
            final String chunk = data.substring(i, Math.min(data.length(), i + chunkSize));
            webView1.post(() -> webView1.evaluateJavascript("javascript:addDataChunk(" + JSONObject.quote(chunk) + ")", null));
        }
        webView1.post(() -> webView1.evaluateJavascript("javascript:processData()", null));
    }

    private void sendDataToWebView(WebView webView, String data) {
        int chunkSize = 4000; // Adjust this size as needed
        sendTextDataToWebView(webView, data, chunkSize);
    }

    private String readFileContent(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        inputStream.close();
        return stringBuilder.toString();
    }

    //URL Overloading, blocks url in Webview
    private class xWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            //view.loadUrl(url);
            return true;
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeAllViews();
            webView.destroy();
        }
        super.onDestroy();
    }
}

