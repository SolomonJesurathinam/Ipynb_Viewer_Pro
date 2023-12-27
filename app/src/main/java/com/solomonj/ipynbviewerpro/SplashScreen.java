package com.solomonj.ipynbviewerpro;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.List;

public class SplashScreen extends AppCompatActivity {

    private static final int SPLASH_SCREEN_TIME_OUT = 2000;
    private static final int SPLASH_DISPLAY_LENGTH = 1500;
    private Handler handler = new Handler(Looper.getMainLooper());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        splashScreenLogic();
    }

    public void splashScreenLogic(){
        if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_VIEW)) {
            Uri fileUri = getIntent().getData();
            handler.postDelayed(() -> {
                if (fileUri != null) {
                    if (!isFinishing()) {
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
                }
            }, SPLASH_DISPLAY_LENGTH);
        }else{
            handler.postDelayed(() -> {
                if (!isFinishing()) { // Add this check
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null); // This will cancel the scheduled Runnable
    }
}