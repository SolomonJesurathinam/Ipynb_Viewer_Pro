package com.solomonj.ipynbviewerpro;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.UriPermission;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.util.List;

public class SplashScreen extends AppCompatActivity {

    private static final int SPLASH_SCREEN_TIME_OUT = 2000;
    private static final int SPLASH_DISPLAY_LENGTH = 1500;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        setupSystemBars();

        if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_VIEW)) {
            Uri fileUri = getIntent().getData();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (fileUri != null) {
                    Log.e("TESTINGG",fileUri.getScheme().toString());
                    if(hasPersistableUriPermission(fileUri)){
                        getContentResolver().takePersistableUriPermission(
                                fileUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        );
                    }
                    // Start WebActivity and pass the file URI
                    Intent webIntent = new Intent(this, Webview.class);
                    webIntent.putExtra("filePath", fileUri.toString());
                    startActivity(webIntent);
                    finish(); // Close the SplashActivity
                }
            }, SPLASH_DISPLAY_LENGTH);
        }else{
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(getApplicationContext(), HomePage.class);
                    startActivity(i);
                    finish();
                }
            }, SPLASH_SCREEN_TIME_OUT);
        }
    }

    private boolean hasPersistableUriPermission(Uri uri) {
        List<UriPermission> uriPermissions = getContentResolver().getPersistedUriPermissions();
        for (UriPermission permission : uriPermissions) {
            if (permission.getUri().equals(uri)) {
                return true;
            }
        }
        return false;
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