package com.example.ipynbviewerpro;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.print.PdfPrint;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.blankj.utilcode.util.PathUtils;
import com.webviewtopdf.PdfView;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class Webview extends AppCompatActivity {

    WebView webView;
    SharedPreferences webPref;
    private ProgressBar progressBar;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        //Toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        webPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        webView = (WebView) findViewById(R.id.webView);
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

    //Options for menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_webview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_download) {
            Uri uri = Uri.parse(getIntent().getStringExtra("filePath"));
            if(uri != null){
                String fname = getFilename(uri);
                if(fname.endsWith(".ipynb")){
                    fname = fname.replace(".ipynb","");
                    try{
                        saveAutomatically(fname);
                    }catch(Exception e){
                        Toast.makeText(Webview.this, "Failed to save pdf, opening default Print method", Toast.LENGTH_LONG).show();
                        createWebPrintJob(webView, Webview.this, fname);
                    }
                }
            }
            return true;
        } else if (id == R.id.action_print) {
            Uri uri = Uri.parse(getIntent().getStringExtra("filePath"));
            if(uri != null){
                String fname = getFilename(uri);
                if(fname.endsWith(".ipynb")){
                    fname = fname.replace(".ipynb","");
                    createWebPrintJob(webView,Webview.this,fname);
                    return true;
                }
            }else{
                Toast.makeText(this, "Nothing to save, please re-select file", Toast.LENGTH_LONG).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    //Default Print Job method for saving the pdf
    public void createWebPrintJob(android.webkit.WebView webView, Context context, String filename) {
        PrintManager printManager = (PrintManager) context
                .getSystemService(Context.PRINT_SERVICE);
        PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(filename);
        printManager.print(filename, printAdapter,
                new PrintAttributes.Builder().build());
    }

    public String getFilename(Uri uri){
        String fileName = null;
        String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
        ContentResolver cr = getApplicationContext().getContentResolver();
        Cursor metaCursor = cr.query(uri, projection, null, null, null);
        if (metaCursor != null) {
            try {
                if (metaCursor.moveToFirst()) {
                    fileName = metaCursor.getString(0);
                }
            } finally {
                metaCursor.close();
            }
        }
        return fileName;
    }

    public void saveAutomatically(String fname) {
        PdfView.createWebPrintJob(Webview.this, webView, getDirectory(), fname + ".pdf", new PdfView.Callback() {
            @Override
            public void success(String s) {
                if(Build.VERSION.SDK_INT>= 29){
                    File file = new File(getDirectory(), fname+".pdf");
                    if (file.exists()){
                        Log.d("TESTINGG","FILE EXISTS");
                        ContentResolver resolver = getApplicationContext().getContentResolver();
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fname);
                        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + "IpynbViewer");

                        Uri uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues);
                        try {
                            InputStream inputStream = new FileInputStream(file);
                            OutputStream outputStream = getContentResolver().openOutputStream(uri);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                FileUtils.copy(inputStream,outputStream);
                                inputStream.close();
                                outputStream.close();
                            }
                        } catch (FileNotFoundException e) {
                            Toast.makeText(Webview.this,"Something happened, Please download again",Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Toast.makeText(Webview.this,"Something happened, Please download again",Toast.LENGTH_SHORT).show();
                        }
                        file.delete();
                        Toast.makeText(Webview.this, "PDF Downloaded at Documents/IpynbViewer", Toast.LENGTH_LONG).show();
                    }else{
                        Toast.makeText(Webview.this,"Something happened, Please download again",Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(Webview.this, "PDF Downloaded at Documents/IpynbViewer", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void failure() {
                Toast.makeText(Webview.this, "Storage access is denied, opening default Print method", Toast.LENGTH_LONG).show();
                createWebPrintJob(webView, Webview.this, fname);
            }
        });
    }

    public File getDirectory(){
        File directory;
        if(Build.VERSION.SDK_INT>= 29){
            directory = new File(PathUtils.getExternalAppDownloadPath().concat("/IpynbViewer/"));
            Log.d("TESTINGG",directory.toString());
        }else{
            directory = new File(PathUtils.getExternalDownloadsPath().concat("/IpynbViewer/"));
            Log.d("TESTINGG",directory.toString());
        }
        return directory;
    }

}

