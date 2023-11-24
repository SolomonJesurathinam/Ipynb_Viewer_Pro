package com.example.ipynbviewerpro;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;

public class OnlineActivity extends AppCompatActivity {

    TextView txtViewdown, txtPrivacy, txtTempStorage, txtBeta;
    Button btnTryItNow, btnSendFeedback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online);

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
                        Log.e("TESTINGG",userName.getEditText().getText().toString());
                        Log.e("TESTINGG",getFeedback.getEditText().getText().toString());
                        dialog.dismiss();
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
}