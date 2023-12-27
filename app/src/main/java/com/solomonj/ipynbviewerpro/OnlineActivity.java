package com.solomonj.ipynbviewerpro;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputLayout;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OnlineActivity extends AppCompatActivity {

    TextView txtViewdown, txtPrivacy, txtTempStorage, txtBeta;
    Button btnTryItNow, btnSendFeedback;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online);

        //Locators
        txtViewdown = findViewById(R.id.txtViewdown);
        txtPrivacy = findViewById(R.id.txtPrivacy);
        txtTempStorage = findViewById(R.id.txtTempStorage);
        txtBeta = findViewById(R.id.txtBeta);
        btnTryItNow = findViewById(R.id.btnTryItNow);
        btnSendFeedback = findViewById(R.id.btnSendFeedback);

        //Functions
        displayText();
        tryItNowLogic();
        sendFeedbackLogic();
    }

    public void sendFeedbackLogic(){
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
                progressBar = dialog.findViewById(R.id.progressBar);

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

    public void tryItNowLogic(){
        btnTryItNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), StreamlitActivity.class);
                startActivity(intent);
            }
        });
    }

    public void displayText(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtViewdown.setText(partialTextBold(" Easily view the converted file and download the PDF","* View & Download:"));
                txtPrivacy.setText(partialTextBold(" Your uploaded files are automatically deleted after conversion. We do not store any files, ensuring your data remains private.","* Privacy First: "));
                txtTempStorage.setText(partialTextBold(" The conversion runs in a container without persistent storage, meaning nothing is kept after your session ends.","* Temporary Storage:"));
                txtBeta.setText(partialTextBold(" As this is a beta feature, you might encounter some hiccups along the way.","* Beta Experience: "));
            }
        });
    }

    public SpannableString partialTextBold(String normalText, String boldText){
        SpannableString styledText = new SpannableString(boldText + normalText);
        styledText.setSpan(new StyleSpan(Typeface.BOLD), 0, boldText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return styledText;
    }

    private void sendFeedback(String url, String dataJson, AlertDialog dialog) {

        runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));
        PostDataTask task = new PostDataTask(url, dataJson, new PostResponseCallback() {
            @Override
            public void onResponse(String response) {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "Feedback sent successfully", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    progressBar.setVisibility(View.GONE);
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "Failed to send feedback, try again", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
            }
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(task);
        executor.shutdown();
    }
}