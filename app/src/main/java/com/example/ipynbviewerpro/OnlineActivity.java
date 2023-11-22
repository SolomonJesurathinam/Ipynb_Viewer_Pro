package com.example.ipynbviewerpro;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.widget.TextView;

public class OnlineActivity extends AppCompatActivity {

    TextView txtViewdown, txtPrivacy, txtTempStorage, txtBeta;

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
    }


    public SpannableString partialTextBold(String normalText, String boldText){
        SpannableString styledText = new SpannableString(boldText + normalText);
        styledText.setSpan(new StyleSpan(Typeface.BOLD), 0, boldText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return styledText;
    }
}