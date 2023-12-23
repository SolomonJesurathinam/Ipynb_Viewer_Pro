package com.solomonj.ipynbviewerpro;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.blankj.utilcode.util.PathUtils;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import androidx.activity.OnBackPressedCallback;

public class Webview extends AppCompatActivity {

    WebView webView;
    SharedPreferences webPref;  
    private ProgressBar progressBar;
    Toolbar toolbar;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        //set status and navigation colors
        setupSystemBars();

        //Toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        //webpref
        webPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        //progress bar and webview
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        webView = (WebView) findViewById(R.id.webView);
        extractDataAndDisplay();

        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                clearActiveUriState();
                // If you want to delegate the back press event to the system after your handling,
                // you can call:
                // setEnabled(false);
                // requireActivity().onBackPressed();

                // Or, if you just want to finish the current activity, you can use:
                finish();
            }
        };
    }

    //extract data
    public void extractDataAndDisplay(){
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

    //open webview
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

    //data in chunks
    private void sendTextDataToWebView(WebView webView1, String data, int chunkSize) {
        for (int i = 0; i < data.length(); i += chunkSize) {
            final String chunk = data.substring(i, Math.min(data.length(), i + chunkSize));
            webView1.post(() -> webView1.evaluateJavascript("javascript:addDataChunk(" + JSONObject.quote(chunk) + ")", null));
        }
        webView1.post(() -> webView1.evaluateJavascript("javascript:processData()", null));
    }

    //send chunks data to webview
    private void sendDataToWebView(WebView webView, String data) {
        int chunkSize = 4000; // Adjust this size as needed
        sendTextDataToWebView(webView, data, chunkSize);
    }

    //read data before splitting to chunks
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

    //onDestory close all webview and clear
    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeAllViews();
            webView.destroy();
        }
        clearActiveUriState();
        super.onDestroy();
    }

    //Options for menu list (Download and Print)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_webview, menu);
        return true;
    }

    //On click on DDownload and Print
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_download) {
            if(Build.VERSION.SDK_INT<29){
                if(ContextCompat.checkSelfPermission(Webview.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(Webview.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                }else{
                    downloadSteps();
                }
            }else {
                downloadSteps();
            }
            return true;
        } else if (id == R.id.action_print) {
            Uri uri = Uri.parse(getIntent().getStringExtra("filePath"));
            if(uri != null){
                String fname = getFilename(getApplicationContext(),uri);
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

    public void downloadSteps(){
        Uri uri = Uri.parse(getIntent().getStringExtra("filePath"));
        if(uri != null){
            String fname = getFilename(getApplicationContext(),uri);
            if(fname.endsWith(".ipynb")){
                fname = fname.replace(".ipynb","");
                try{
                    saveAutomatically(fname);
                }catch(Exception e){
                    setProgressBar("Gone");
                    Toast.makeText(Webview.this, "Failed to save pdf, opening default Print method", Toast.LENGTH_LONG).show();
                    Log.e("TESTINGG",e.toString());
                    createWebPrintJob(webView, Webview.this, fname);
                }
            }
        }
    }

    //Default Print Job method for saving the pdf
    public void createWebPrintJob(android.webkit.WebView webView, Context context, String filename) {
        PrintManager printManager = (PrintManager) context
                .getSystemService(Context.PRINT_SERVICE);
        PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(filename);
        printManager.print(filename, printAdapter,
                new PrintAttributes.Builder().build());
    }

    //File name from uri
    public String getFilename(Context context, Uri uri){
        String fileName = null;
        if(uri.getScheme().equalsIgnoreCase("content")){
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (fileName == null || fileName.contains("/")) {
                String path = uri.getPath();
                if (path != null) {
                    int cut = path.lastIndexOf('/');
                    if (cut != -1) {
                        fileName = path.substring(cut + 1);
                    }
                }
            }
        }else if(uri.getScheme().equalsIgnoreCase("file")){
                fileName = uri.getLastPathSegment();
        }
        //checking if the url is encoded
        try{
            fileName = checkURLEncoded(fileName);
        }catch(Exception e){
            e.printStackTrace();
        }
        return fileName;
    }

    public String checkURLEncoded(String input){
        boolean isEncoded = input.contains("%");
        if(isEncoded){
            try{
                String decodedFilePath = URLDecoder.decode(input.replaceAll("%25", "%"), StandardCharsets.UTF_8.toString());
                String[] pathSegments = decodedFilePath.split("/");
                return pathSegments[pathSegments.length - 1];
            }catch(UnsupportedEncodingException e){
                return input;
            }
        }else{
            return input;
        }
    }

    //Save Automatically (Download logic)
    public void saveAutomatically(String fname) {
        setProgressBar("Visible");
        PdfView.createWebPrintJob(Webview.this, webView, getDirectory(), fname + ".pdf", new PdfView.Callback() {
            @Override
            public void success(String s) {
                if(Build.VERSION.SDK_INT>= 29){ //Targetting Android 10 and above
                    File file = new File(getDirectory(), fname+".pdf");
                    if (file.exists()){
                        ContentResolver resolver = getApplicationContext().getContentResolver();
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fname);
                        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + File.separator + "IpynbViewer");

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
                            setProgressBar("Gone");
                        } catch (IOException e) {
                            Toast.makeText(Webview.this,"Something happened, Please download again",Toast.LENGTH_SHORT).show();
                            setProgressBar("Gone");
                        }
                        file.delete();
                        Toast.makeText(Webview.this, "PDF Downloaded at Documents/IpynbViewer", Toast.LENGTH_LONG).show();
                        setProgressBar("Gone");
                    }else{
                        Toast.makeText(Webview.this,"Something happened, Please download again",Toast.LENGTH_SHORT).show();
                        setProgressBar("Gone");
                    }
                }else{//android 9
                    Toast.makeText(Webview.this, "PDF Downloaded at Documents/IpynbViewer", Toast.LENGTH_LONG).show();
                    setProgressBar("Gone");
                }
            }

            @Override
            public void failure() {
                Toast.makeText(Webview.this, "Failed to save pdf, opening default Print method", Toast.LENGTH_LONG).show();
                setProgressBar("Gone");
                createWebPrintJob(webView, Webview.this, fname);
            }
        });
    }

    //Dialog box
    private void showPermissionSettingsDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Allow Permission Manually")
                .setMessage("Need Storage Permission to download pdf to devices, please allow manually from settings.")
                .setPositiveButton("App Settings", (dialogInterface, which) -> {
                    // Intent to open app settings
                    openAppSettings();
                })
                .setNegativeButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss())
                .create();
        dialog.setOnShowListener(dialogInterface -> {
            // Change the positive button color
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.black));

            // Change the negative button color
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            negativeButton.setTextColor(ContextCompat.getColor(this, R.color.black));
        });
        dialog.show();
    }

    //Open settings to provide manual storage access
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    //handling permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted
                    downloadSteps();
                } else {
                    // Permission denied, show a Toast
                    Toast.makeText(this, "Storage access is required to download files automatically", Toast.LENGTH_SHORT).show();
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        showPermissionSettingsDialog();
                    }
                }
                return;
            }
            // Other 'case' lines to check for other permissions this app might request
        }
    }

    //Get file directory for downloading in all android versions
    public File getDirectory(){
        File directory;
        if(Build.VERSION.SDK_INT>= 29){
            directory = new File(PathUtils.getExternalAppDocumentsPath().concat("/IpynbViewer/"));
        }else{
            directory = new File(PathUtils.getExternalDocumentsPath().concat("/IpynbViewer/"));
        }
        return directory;
    }

    //progress bar hide/show
    public void setProgressBar(String status){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(status.equalsIgnoreCase("Visible")){
                    progressBar.setVisibility(View.VISIBLE);
                    findViewById(R.id.action_download).setEnabled(false);
                    findViewById(R.id.action_print).setEnabled(false);

                }else if(status.equalsIgnoreCase("Gone")){
                    progressBar.setVisibility(View.GONE);
                    findViewById(R.id.action_download).setEnabled(true);
                    findViewById(R.id.action_print).setEnabled(true);
                }

            }
        });
    }

    //status and navigation theming
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

    private void clearActiveUriState() {
        List<UriPermission> uriPermissions = getContentResolver().getPersistedUriPermissions();
        Log.d("URI Permissions", "Before clearing: " + uriPermissions.size());
        String treeUri = webPref.getString("treeUri",null);
        Uri savedUri = treeUri != null ? Uri.parse(treeUri) :null;
        for (UriPermission permission : uriPermissions) {
            Log.e("URI Permissions",permission.toString());
            Log.e("URI Permissions",permission.getUri().toString());
            if(savedUri != null && savedUri.equals(permission.getUri())){
                Log.e("URI Permissions",savedUri.toString());
                continue;
            }
            getContentResolver().releasePersistableUriPermission(
                    permission.getUri(),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );
        }
        List<UriPermission> uriPermissionsAfter = getContentResolver().getPersistedUriPermissions();
        Log.d("URI Permissions", "After clearing: " + uriPermissionsAfter.size());
    }


    //Orientation Change configuration to initial scales --> Orientation Save state is handled in Manifest file
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            webView.setInitialScale(100);
            webView.getSettings().setUseWideViewPort(true);
        } else {
            webView.setInitialScale(100);
            webView.getSettings().setUseWideViewPort(false);
        }
    }
}

