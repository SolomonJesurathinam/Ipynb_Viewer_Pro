package com.example.ipynbviewerpro;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OnlineActivity extends AppCompatActivity {

    TextView txtViewdown, txtPrivacy, txtTempStorage, txtBeta;
    Button btnTryItNow, btnSendFeedback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online);

        setupSystemBars();

        txtViewdown = findViewById(R.id.txtViewdown);
        txtPrivacy = findViewById(R.id.txtPrivacy);
        txtTempStorage = findViewById(R.id.txtTempStorage);
        txtBeta = findViewById(R.id.txtBeta);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtViewdown.setText(partialTextBold(" Easily view the converted file and download the PDF","* View & Download:"));
                txtPrivacy.setText(partialTextBold(" Your uploaded files are automatically deleted after conversion. We do not store any files, ensuring your data remains private.","* Privacy First: "));
                txtTempStorage.setText(partialTextBold(" The conversion runs in a container without persistent storage, meaning nothing is kept after your session ends.","* Temporary Storage:"));
                txtBeta.setText(partialTextBold(" As this is a beta feature, you might encounter some hiccups along the way.","* Beta Experience: "));
            }
        });

        btnTryItNow = findViewById(R.id.btnTryItNow);
        btnTryItNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), StreamlitActivity.class);
                startActivity(intent);
            }
        });

        btnSendFeedback = findViewById(R.id.btnSendFeedback);
        btnSendFeedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(OnlineActivity.this);
                LayoutInflater inflater = getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.dialog_feedback, null);
                builder.setView(dialogView);

                AlertDialog dialog = builder.create();
                dialog.show();

                TextInputLayout userName = dialog.findViewById(R.id.userName);
                TextInputLayout getFeedback = dialog.findViewById(R.id.getFeedback);
                Button btnGetFeedback = dialog.findViewById(R.id.btnGetFeedback);
                btnGetFeedback.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String name = userName.getEditText().getText().toString();
                        String feedback = getFeedback.getEditText().getText().toString();
                        if(!name.isBlank() && !feedback.isBlank()){
                            String url = "https://script.google.com/macros/s/AKfycbw3S9VmBv934IT_ZuyKnR8OWuV_MPS6Vu7eJB8U3eExFrvb2S4iLBMiY2TcRbEkGeo9Hw/exec";
                            String dataJson = "{\"name\":\"" + name + "\",\"feedback\":\"" + feedback + "\"}";
                            sendFeedback(url,dataJson,dialog);
                        }else{
                            Toast.makeText(getApplicationContext(),"Please enter Name and Feedback",Toast.LENGTH_SHORT).show();
                        }

                    }
                });

            }
        });
    }


    public SpannableString partialTextBold(String normalText, String boldText){
        SpannableString styledText = new SpannableString(boldText + normalText);
        styledText.setSpan(new StyleSpan(Typeface.BOLD), 0, boldText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return styledText;
    }

    private void sendFeedback(String url, String dataJson, AlertDialog dialog) {
        PostDataTask task = new PostDataTask(url, dataJson, new PostResponseCallback() {
            @Override
            public void onResponse(String response) {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "Feedback sent successfully", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "Failed to send feedback, try again", Toast.LENGTH_SHORT).show();
                });
            }
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(task);
        executor.shutdown();
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