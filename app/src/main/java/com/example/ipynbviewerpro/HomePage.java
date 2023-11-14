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
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import androidx.recyclerview.widget.LinearLayoutManager;

public class HomePage extends AppCompatActivity {

    Button choosefile, retrieveAll;
    private ActivityResultLauncher<String[]> mGetContent;
    SharedPreferences homePagePref;
    RadioGroup radioRender;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_CODE_MANAGE_STORAGE = 1;
    RecyclerView recyclerView;
    private ActivityResultLauncher<Intent> manageExternalStorageActivityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homepage);

        //Shared Prefs
        homePagePref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        homePagePref.getString("renderKey","Real");
        homePagePref.getInt("storageDeny",0);

        choosefile = findViewById(R.id.choosefile);
        fileHandling();

        radioRender = findViewById(R.id.radioRender);
        radiobuttonLogic();

        //Android 13
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

        //Storage access
        retrieveAll = findViewById(R.id.retrieveAll);
        scanFilesLogic();

        recyclerView = findViewById(R.id.recyclerViewFiles);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // After getting the files
        /*
        ArrayList<File> ipynbFiles = findIpynbFiles(Environment.getExternalStorageDirectory());
        FileAdapter adapter = new FileAdapter(ipynbFiles);
        recyclerView.setAdapter(adapter);
        /*
         */

        displayRecyclerView();
    }


    public void fileHandling(){
        mGetContent = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                new androidx.activity.result.ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        Toast.makeText(getApplicationContext(),uri.getPath(),Toast.LENGTH_LONG).show();
                        try {
                            String fileContent = readFileContent(uri);
                            String base64EncodedString = Base64.encodeToString(fileContent.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                            DataHolder.getInstance().setEncodedData(base64EncodedString);
                            Intent intent = new Intent(getApplicationContext(), Webview.class);
                            startActivity(intent);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        if (uri != null) {
                            // Use the Uri to access the file
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

    //read file content
    private String readFileContent(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        inputStream.close();
        return stringBuilder.toString();
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


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted
                    displayRecyclerView();
                    if(homePagePref.getInt("storageDeny",0) != 0){
                        SharedPreferences.Editor editor = homePagePref.edit();
                        editor.putInt("storageDeny",0);
                        editor.apply();
                        editor.commit();
                    }
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

    public void displayRecyclerView(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            retrieveIpynbFiles();
        }
        else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            retrieveIpynbFiles();
        }
        if(homePagePref.getInt("storageDeny",0) != 0){
            SharedPreferences.Editor editor = homePagePref.edit();
            editor.putInt("storageDeny",0);
            editor.apply();
            editor.commit();
        }
    }

    public void retrieveIpynbFiles(){
        // You have the permission, start file scan
        ArrayList<File> ipynbFiles = findIpynbFiles(Environment.getExternalStorageDirectory());
        FileAdapter adapter = new FileAdapter(ipynbFiles);
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void scanFilesLogic(){
        retrieveAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if(!Environment.isExternalStorageManager()){
                        Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        manageExternalStorageActivityResultLauncher.launch(intent);
                    }
                }
                else{
                    if(ContextCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(HomePage.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                    }else{
                        displayRecyclerView();
                    }

                    if(homePagePref.getInt("storageDeny",0)>2){
                        Log.e("TESTING","DENY"+String.valueOf(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)));
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

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
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

}