package com.example.ipynbviewerpro;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class Webview extends AppCompatActivity {

    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        String encodedData = DataHolder.getInstance().getEncodedData();
        Log.e("TESTING",encodedData);

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
                webView.evaluateJavascript("javascript:processData('" + encodedData + "')", null);
            }
        });

        webView.loadUrl(render2);



    }

    //URL Overloading, blocks url in Webview
    private class xWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            //view.loadUrl(url);
            return true;
        }
    }
}

