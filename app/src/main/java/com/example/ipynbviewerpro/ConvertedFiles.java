package com.example.ipynbviewerpro;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ConvertedFiles extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    SharedPreferences convertPagePref;

    private RecyclerView recyclerView;
    private PdfFileAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_converted_files);

        setupSystemBars();

        convertPagePref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        recyclerView = findViewById(R.id.rvPdfFiles);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ActivityResultLauncher<Intent> openDocumentTree = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri directoryUri = result.getData().getData();

                        // Persist the permission flags.
                        int takeFlags = result.getData().getFlags();
                        takeFlags &= (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        getContentResolver().takePersistableUriPermission(directoryUri, takeFlags);

                        // Proceed with scanning for PDF files
                        displayRecyclerView(directoryUri);

                        //sharing the uri to not get deleted
                        SharedPreferences.Editor editor = convertPagePref.edit();
                        editor.putString("treeUri",directoryUri.toString());
                        editor.commit();
                        editor.apply();
                    }
                }
        );

        //clear state
        clearActiveUriState();

        if (Build.VERSION.SDK_INT >= 30) {
            if (Build.VERSION.SDK_INT >= 30 && Build.VERSION.SDK_INT <33) {
                if (Environment.isExternalStorageManager()) {
                    File downloadsFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "IpynbViewer");
                    displayRecyclerView(Uri.fromFile(downloadsFolder));
                }
                else if(Build.VERSION.SDK_INT >=30 && Build.VERSION.SDK_INT <33){
                    if(ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(ConvertedFiles.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                    }else{
                        File downloadsFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "IpynbViewer");
                        displayRecyclerView(Uri.fromFile(downloadsFolder));
                    }
                }
            }
            else if(Build.VERSION.SDK_INT >= 33){
                Log.e("TESTINGG","test1");
                Uri existingDirectoryUri = checkForExistingDirectoryAccess();
                Log.e("TESTINGG","test2");
                //Log.e("TESTINGG",existingDirectoryUri.toString());
                if (existingDirectoryUri != null) {
                    Log.e("TESTINGG",existingDirectoryUri.toString());
                    displayRecyclerView(existingDirectoryUri);
                }else{
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    openDocumentTree.launch(intent);
                }
            }
        }
        else if(Build.VERSION.SDK_INT >= 28 && Build.VERSION.SDK_INT <=29){
            if(ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(ConvertedFiles.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }else{
                File downloadsFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "IpynbViewer");
                displayRecyclerView(Uri.fromFile(downloadsFolder));
            }
        }
    }

    private Uri checkForExistingDirectoryAccess() {
        List<UriPermission> uriPermissions = getContentResolver().getPersistedUriPermissions();
        for (UriPermission permission : uriPermissions) {
            if(permission != null){
                //Log.e("TESTINGG",permission.toString());
            }
            if (permission.isReadPermission() && permission.isWritePermission()) {
                return permission.getUri();
            }
        }
        return null;
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted
                    File downloadsFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "IpynbViewer");
                    displayRecyclerView(Uri.fromFile(downloadsFolder));
                } else {
                    // Permission denied, show a Toast
                    Toast.makeText(this, "Storage access is required to display all converted pdf files", Toast.LENGTH_SHORT).show();
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        showPermissionSettingsDialog();
                    }
                }
                return;
            }
            // Other 'case' lines to check for other permissions this app might request
        }
    }

    public void displayRecyclerView(Uri directoryUri) {
        // Scan for PDF files using the updated findPdfFiles method
        List<File> pdfFiles = findPdfFiles(directoryUri);
        Log.e("TESTINGG", String.valueOf(pdfFiles.size()));

        // Set up the adapter
        adapter = new PdfFileAdapter(this, pdfFiles);
        adapter.setClickListener(new PdfFileAdapter.ItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                // Handle the item click here
                File selectedFile = adapter.getItem(position);
                openPdfFile(selectedFile);
            }
        });
        recyclerView.setAdapter(adapter);
    }





    //Dialog box
    private void showPermissionSettingsDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Allow Permission Manually")
                .setMessage("Need Storage Permission to display all converted pdf, please allow manually from settings.")
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

    private List<File> findPdfFiles(Uri directoryUri) {
        List<File> pdfFileList = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33) {
            // Use SAF for Android 10 (API level 29) and above
            DocumentFile directory = DocumentFile.fromTreeUri(this, directoryUri);
            if (directory != null && directory.isDirectory()) {
                for (DocumentFile file : directory.listFiles()) {
                    if (file.isFile() && file.getName() != null && file.getName().toLowerCase().endsWith(".pdf")) {
                        // Convert Uri to File, or directly use Uri for your purpose
                        File pdfFile = new File(file.getUri().getPath());
                        pdfFileList.add(pdfFile);
                    }
                }
            }
        } else {
            // Traditional file access for versions below Android 10
            File directory = new File(directoryUri.getPath());
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().toLowerCase().endsWith(".pdf")) {
                        pdfFileList.add(file);
                    }
                }
            }
        }
        return pdfFileList;
    }


    private void openPdfFile(File file) {
        // Open PDF file with an external app
        Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    private void clearActiveUriState() {
        List<UriPermission> uriPermissions = getContentResolver().getPersistedUriPermissions();
        Log.d("URI Permissions", "Before clearing: " + uriPermissions.size());
        String treeUri = convertPagePref.getString("treeUri",null);
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
}