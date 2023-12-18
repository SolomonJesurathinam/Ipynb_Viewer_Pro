package com.solomonj.ipynbviewerpro;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

public class    StreamlitActivity extends AppCompatActivity {

    private WebView webView;
    private ValueCallback<Uri[]> uploadMessage;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streamlit);

        setupSystemBars();

        String url = "https://nbtopdf.streamlit.app/";

        progressBar = findViewById(R.id.progressBar);
        progresssBarDisplay("visible");

        webView = (WebView) findViewById(R.id.webview);
        webViewSettings();
        activityLauncherCode();
        fileSelection();
        download();
        webView.loadUrl(url);

    }

    public void webViewSettings(){
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient(){
            public void onPageFinished(WebView view, String url) {
                progresssBarDisplay("gone");
            }
        });
    }

    public void activityLauncherCode(){
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (uploadMessage == null) return;

                    Uri[] results = null;
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (result.getData() != null) {
                            results = new Uri[]{result.getData().getData()};
                        }
                    }
                    uploadMessage.onReceiveValue(results);
                    uploadMessage = null;
                });
    }

    public void fileSelection(){
        webView.setWebChromeClient(new WebChromeClient() {
            // For Lollipop 5.0+ Devices
            @Override
            public boolean onShowFileChooser(WebView mWebView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                uploadMessage = filePathCallback;
                openFileChooser();
                return true;
            }

            private void openFileChooser() {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                activityResultLauncher.launch(Intent.createChooser(intent, "File Chooser"));
            }
        });
    }

    public void download(){
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

                // Correct MIME type if necessary
                if (mimetype.equalsIgnoreCase("application/octet-stream") || mimetype.equalsIgnoreCase("bin")) {
                    mimetype = "application/pdf"; // Assuming the file is a PDF
                }
                request.setMimeType(mimetype);

                // Get cookies from WebView
                String cookies = CookieManager.getInstance().getCookie(url);

                // Add cookie and User-Agent to request
                request.addRequestHeader("Cookie", cookies);
                request.addRequestHeader("User-Agent", userAgent);

                // Use the DownloadManager to download the file
                request.setDescription("Downloading file...");
                String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
                request.setTitle(fileName);
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                // Specify the destination directory and file name
                String directoryPath = "IpynbViewer/" + fileName;
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, directoryPath);

                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(request);
            }
        });
    }

    public void progresssBarDisplay(String display){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(display.equalsIgnoreCase("visible")){
                    progressBar.setVisibility(View.VISIBLE);
                }else if(display.equalsIgnoreCase("gone")){
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    private void setupSystemBars() {
        Window window = getWindow();

        // Set the status bar color
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#F5F5F5"));

        // Set the navigation bar color
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            window.setNavigationBarColor(Color.parseColor("#F5F5F5"));
        }

        // For light status bar icons (dark icons for better visibility)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = window.getDecorView().getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            window.getDecorView().setSystemUiVisibility(flags);
        }

        // For light navigation bar icons (available from API 29)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int flags = window.getDecorView().getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            window.getDecorView().setSystemUiVisibility(flags);
        }
    }

}