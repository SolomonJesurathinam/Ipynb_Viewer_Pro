package com.example.ipynbviewerpro;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import androidx.recyclerview.widget.LinearLayoutManager;

public class HomePage extends AppCompatActivity {

    Button choosefile, retrieveAll, convertOnline;
    private ActivityResultLauncher<String[]> mGetContent;
    SharedPreferences homePagePref;
    RadioGroup radioRender;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_CODE_MANAGE_STORAGE = 1;
    RecyclerView recyclerView;
    private ActivityResultLauncher<Intent> manageExternalStorageActivityResultLauncher;
    LinearLayout recyclerLayout;

    ImageView feedback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homepage);

        setupSystemBars();

        recyclerLayout = findViewById(R.id.recyclerLayout);

        //Shared Prefs
        homePagePref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        homePagePref.getString("renderKey","Real");
        homePagePref.getInt("storageDeny",0);

        choosefile = findViewById(R.id.choosefile);
        fileHandling();

        radioRender = findViewById(R.id.radioRender);
        radiobuttonLogic();

        //Android 13
        storagePermissionandroid11();

        //Storage access
        retrieveAll = findViewById(R.id.retrieveAll);
        scanFilesLogic();

        //displaying all the files
        recyclerView = findViewById(R.id.recyclerViewFiles);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        displayRecyclerView();

        //testing
        feedback = findViewById(R.id.feedback);
        feedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("market://details?id="+getPackageName());
                Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    startActivity(myAppLinkToMarket);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getApplicationContext(),"Unable to find market app",Toast.LENGTH_LONG).show();
                }
            }
        });

        //online features
        convertOnline = findViewById(R.id.convertOnline);
        convertOnline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),OnlineActivity.class);
                startActivity(intent);
            }
        });
    }


    //get file and share to webactivity
    public void fileHandling(){
        mGetContent = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                new androidx.activity.result.ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if(uri != null){
                            getContentResolver().takePersistableUriPermission(
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            );
                            Toast.makeText(getApplicationContext(),uri.getPath(),Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(getApplicationContext(), Webview.class);
                            intent.putExtra("filePath",uri.toString());
                            startActivity(intent);
                        }
                    }
                });

        choosefile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGetContent.launch(new String[]{"application/*"});
            }
        });
    }

    //Render radio logic
    private void radiobuttonLogic(){
        if(homePagePref.getString("renderKey","Real").equalsIgnoreCase("Real")){
            radioRender.check(R.id.radioReal);
        }else if(homePagePref.getString("renderKey","Real").equalsIgnoreCase("Basic")){
            radioRender.check(R.id.radioBasic);
        }
        radioRender.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                SharedPreferences.Editor editor = homePagePref.edit();
                if(checkedId == R.id.radioReal){
                    editor.putString("renderKey","Real");
                }else if(checkedId == R.id.radioBasic){
                    editor.putString("renderKey","Basic");
                }
                editor.apply();
                editor.commit();
            }
        });
    }


    //handling storage permission result android <11
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted
                    displayRecyclerView();
                } else {
                    // Permission denied, show a Toast
                    Toast.makeText(this, "Storage access is required to display all jupyter files", Toast.LENGTH_SHORT).show();
                    SharedPreferences.Editor editor = homePagePref.edit();
                    editor.putInt("storageDeny",homePagePref.getInt("storageDeny",0)+1);
                    editor.apply();
                    editor.commit();
                    Log.e("TESTING","DENY"+String.valueOf(homePagePref.getInt("storageDeny",0)));
                }
                return;
            }
            // Other 'case' lines to check for other permissions this app might request
        }
    }


    //handling storage permission result Android>11
    public void storagePermissionandroid11(){
        manageExternalStorageActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Handle the result
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            displayRecyclerView();
                        }
                    }
                });
    }


    //displaying recycler view
    public void displayRecyclerView(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            retrieveIpynbFiles();
            if(homePagePref.getInt("storageDeny",0) != 0){
                SharedPreferences.Editor editor = homePagePref.edit();
                editor.putInt("storageDeny",0);
                editor.apply();
                editor.commit();
            }
        }
        else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            retrieveIpynbFiles();
            if(homePagePref.getInt("storageDeny",0) != 0){
                SharedPreferences.Editor editor = homePagePref.edit();
                editor.putInt("storageDeny",0);
                editor.apply();
                editor.commit();
            }
        }
    }


    //Get al ipynb files and display in the recycler view
    public void retrieveIpynbFiles(){
        // You have the permission, start file scan
        ArrayList<File> ipynbFiles = findIpynbFiles(Environment.getExternalStorageDirectory());
        FileAdapter adapter = new FileAdapter(ipynbFiles, new FileAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(File file) {
                String javaURI = file.toURI().toString();
                Uri uri = Uri.parse(javaURI);
                Intent intent = new Intent(getApplicationContext(), Webview.class);
                intent.putExtra("filePath",uri.toString());
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerLayout.setVisibility(View.VISIBLE);
                //retrieveAll.setVisibility(View.INVISIBLE);
            }
        });
    }


    //Logic for scan files button
    public void scanFilesLogic(){
        retrieveAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if(!Environment.isExternalStorageManager()){
                        showPrivacycontentfullStorage();
                    }
                }
                else{
                    if(ContextCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(HomePage.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                    }else{
                        displayRecyclerView();
                    }

                    if(homePagePref.getInt("storageDeny",0)>2){
                        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                            // Permission denied
                            if (!ActivityCompat.shouldShowRequestPermissionRationale(HomePage.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                                // Direct the user to app settings
                                showPermissionSettingsDialog();
                            } else {
                                // Show rationale and request permission again
                                ActivityCompat.requestPermissions(HomePage.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                            }
                        }
                    }
                }

            }
        });
    }


    //Dialog box
    private void showPermissionSettingsDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Allow Permission Manually")
                .setMessage("Please allow storage access manually from settings.")
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

    //Dialog box
    private void showPrivacycontentfullStorage() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_custom, null);
        builder.setView(dialogView);

        TextView tvPermissionsMessage = dialogView.findViewById(R.id.tvPermissionsMessage);
        TextView tvPrivacyPolicyMessage = dialogView.findViewById(R.id.tvPrivacyPolicyMessage);

        // Create a SpannableString to style the text
        String normalText = " For devices running Android 11 and above, our app requires permission to access storage to scan for and display .ipynb files. This permission is critical for the app’s functionality, allowing you to manage and view your notebooks.";
        SpannableString styledText = new SpannableString("Full Storage Access:" + normalText);
        styledText.setSpan(new StyleSpan(Typeface.BOLD), 0, "Full Storage Access:".length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvPermissionsMessage.setText(styledText);

        // Create a SpannableString to style the text
        String normalText1 = " We value your privacy. Our app does not collect or transmit any personal data. All permissions requested are solely for enhancing your user experience and ensuring the app's core features function correctly.";
        SpannableString styledText1 = new SpannableString("Your Privacy Matters:" + normalText1);
        styledText1.setSpan(new StyleSpan(Typeface.BOLD), 0, "Your Privacy Matters:".length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvPrivacyPolicyMessage.setText(styledText1);

        builder.setPositiveButton("Grant Permission", (dialog, id) -> {
            openFullStorageSettings();
        });
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            // Get the positive button and change its color to black
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.black));

            // Get the negative button and change its color to black
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            negativeButton.setTextColor(ContextCompat.getColor(this, R.color.black));
        });
        dialog.show();
    }


    //Open settings to provide manual storage access
    private void openFullStorageSettings() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        manageExternalStorageActivityResultLauncher.launch(intent);
    }


    //get List of ipynb files
    private ArrayList<File> findIpynbFiles(File root) {
        ArrayList<File> fileList = new ArrayList<>();
        // Create a FilenameFilter to filter for .ipynb files
        FilenameFilter ipynbFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".ipynb");
            }
        };

        File[] files = root.listFiles(); // Here you will list all the files in the directory.
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // Recursively search the directory for .ipynb files.
                    fileList.addAll(findIpynbFiles(file));
                } else if (file.isFile() && ipynbFilter.accept(file.getParentFile(), file.getName())) {
                    fileList.add(file);
                }
            }
        }
        return fileList;
    }

    private void setupSystemBars() {
        Window window = getWindow();

        // Set the status bar color
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#F5F5F5"));

        // Set the navigation bar color
        window.setNavigationBarColor(Color.parseColor("#F5F5F5"));

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