package com.solomonj.ipynbviewerpro;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnRenderListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConvertedFiles extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    SharedPreferences convertPagePref;
    private RecyclerView recyclerView;
    private PdfFileAdapter adapter;
    FloatingActionButton searchFloating;
    ActivityResultLauncher<Intent> openDocumentTree;
    TextView scanMessage,convertText;
    private PDFView pdfView;
    private LinearLayout recyclerLayout;
    private ExecutorService executorService;
    Object selectedSource;
    private int currentPage = 0;
    SearchView searchPdf;
    View closeBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_converted_files);

        //Shared Pref
        convertPagePref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        //Find the widgets
        recyclerView = findViewById(R.id.rvPdfFiles);
        scanMessage = findViewById(R.id.scanMessage);
        searchFloating = findViewById(R.id.searchFloating);
        pdfView = findViewById(R.id.pdfView);
        recyclerLayout = findViewById(R.id.recyclerLayout);
        searchPdf = findViewById(R.id.searchView);
        closeBtn = searchPdf.findViewById(androidx.appcompat.R.id.search_close_btn);
        convertText = findViewById(R.id.convertText);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //Methods to perform actions
        executorService = Executors.newSingleThreadExecutor();
        activityLauncherAndroid13();
        floatingButtonLogic();
        checkPermissionAndDisplay();
        searchAndFilterfiles();

        // Back Pressed Logic
        backPressedLogic();
    }

    public void activityLauncherAndroid13(){
        openDocumentTree = registerForActivityResult(
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
    }

    public void floatingButtonLogic(){
        searchFloating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //clear state
                clearActiveUriState();
                getPermissionAndDisplay();
            }
        });
    }

    public void backPressedLogic(){
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled */) {
            @Override
            public void handleOnBackPressed() {
                if (pdfView.getVisibility() == View.VISIBLE) {
                    pdfView.recycle();
                    // If PDFView is visible, return to the RecyclerView
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pdfView.setVisibility(View.GONE);
                            recyclerLayout.setVisibility(View.VISIBLE);
                            searchFloating.setVisibility(View.VISIBLE);
                        }
                    });
                    selectedSource = null;
                }
                else {
                    // Otherwise, call the default back action
                    setEnabled(false);
                    finish(); //change made
                }

            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    public void getPermissionAndDisplay(){
        if (Build.VERSION.SDK_INT >= 28 && Build.VERSION.SDK_INT <33) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(ConvertedFiles.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            } else {
                File downloadsFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "IpynbViewer");
                displayRecyclerView(Uri.fromFile(downloadsFolder));
                Toast.makeText(this, "Scan Complete", Toast.LENGTH_SHORT).show();
            }
        }
        else if(Build.VERSION.SDK_INT >= 33){
            if(convertPagePref.getString("treeUri",null) != null){
                showSearchAgainDialog();
            }else{
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                openDocumentTree.launch(intent);
                Toast.makeText(this,"Select 'Documents/IpynbViewer' folder manually",Toast.LENGTH_LONG).show();
            }
        }
    }

    public void checkPermissionAndDisplay(){
        clearActiveUriState();
        if(Build.VERSION.SDK_INT >=28 && Build.VERSION.SDK_INT <33){
            if(ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                    File downloadsFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "IpynbViewer");
                    displayRecyclerView(Uri.fromFile(downloadsFolder));
                }
            }
        else if(Build.VERSION.SDK_INT >= 33){
            Uri existingDirectoryUri = checkForExistingDirectoryAccess();
            if (existingDirectoryUri != null) {
                displayRecyclerView(existingDirectoryUri);
            }
        }
    }

    private void showSearchAgainDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Select Folder")
                .setMessage("Do you need to reselect the folder (Documents/IpynbViewer)")
                .setPositiveButton("Yes", (dialogInterface, which) -> {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    openDocumentTree.launch(intent);
                    Toast.makeText(this,"Select 'Documents/IpynbViewer' folder manually",Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("No", (dialogInterface, which) -> {
                    dialogInterface.dismiss();
                    Uri existingDirectoryUri = checkForExistingDirectoryAccess();
                    if (existingDirectoryUri != null) {
                        displayRecyclerView(existingDirectoryUri);
                        Toast.makeText(this, "Scan Complete", Toast.LENGTH_SHORT).show();
                    }
                })
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

    private Uri checkForExistingDirectoryAccess() {
        List<UriPermission> uriPermissions = getContentResolver().getPersistedUriPermissions();
        for (UriPermission permission : uriPermissions) {
            if (permission.isReadPermission() && permission.isWritePermission()) {
                return permission.getUri();
            }
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted
                    File downloadsFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "IpynbViewer");
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
        // Scan for PDF files or Uris using the updated findPdfFiles method
        List<Object> pdfSources = findPdfFiles(directoryUri);
        Log.e("TESTINGG", String.valueOf(pdfSources.size()));

        // Set up the adapter
        adapter = new PdfFileAdapter(this, pdfSources);  // Ensure your adapter can handle both File and Uri objects
        adapter.setClickListener(new PdfFileAdapter.ItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                // Handle the item click here
                selectedSource = adapter.getItem(position);
                if(selectedSource != null){
                    loadPdfInBackground(selectedSource);
                }
            }
        });
        recyclerView.setAdapter(adapter);

        //check for size and invisible the text
        if(pdfSources.size()>0){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    scanMessage.setVisibility(View.GONE);
                    searchPdf.setVisibility(View.VISIBLE);
                }
            });
        }else if(pdfSources.size()==0){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                   scanMessage.setText("No Pdf Files found, convert and scan again");
                }
            });
        }
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

    private List<Object> findPdfFiles(Uri directoryUri) {
        List<Object> pdfSourceList = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33) {
            // Use SAF for Android 13 (API level 33) and above
            DocumentFile directory = DocumentFile.fromTreeUri(this, directoryUri);
            if (directory != null && directory.isDirectory()) {
                for (DocumentFile file : directory.listFiles()) {
                    if (file.isFile() && file.getName() != null && file.getName().toLowerCase().endsWith(".pdf")) {
                        // Add the Uri for Android 13 and above
                        pdfSourceList.add(file.getUri());
                    }
                }
            }
        } else {
            // Traditional file access for versions below Android 13
            File directory = new File(directoryUri.getPath());
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().toLowerCase().endsWith(".pdf")) {
                        // Add the File for older versions
                        pdfSourceList.add(file);
                    }
                }
            }
        }
        return pdfSourceList;
    }

    public void loadPdfInBackground(Object pdfSource) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerLayout.setVisibility(View.GONE);
                searchFloating.setVisibility(View.GONE);
                pdfView.setVisibility(View.VISIBLE);
            }
        });

        executorService.execute(() -> {
            runOnUiThread(() -> {
                if (pdfSource instanceof File) {
                    loadPdfFromFile((File) pdfSource);
                } else if (pdfSource instanceof Uri && Build.VERSION.SDK_INT >= 33) {
                    loadPdfFromUri((Uri) pdfSource);
                }
            });
        });
    }

    private void loadPdfFromFile(File file) {
        pdfView.fromFile(file)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .defaultPage(currentPage)
                .onRender(new OnRenderListener() {
                    @Override
                    public void onInitiallyRendered(int nbPages) {
                        pdfView.fitToWidth(currentPage);
                    }
                })
                .load();
    }

    private void loadPdfFromUri(Uri uri) {
        pdfView.fromUri(uri)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .defaultPage(currentPage)
                .onRender(new OnRenderListener() {
                    @Override
                    public void onInitiallyRendered(int nbPages) {
                        pdfView.fitToWidth(currentPage);
                    }
                })
                .load();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Save the current page number
        if (pdfView != null) {
            currentPage = pdfView.getCurrentPage();
        }

        if(selectedSource != null){
            // Reload the PDF to fit the new orientation
            executorService.execute(() -> {
                runOnUiThread(() -> {
                    if (selectedSource instanceof File) {
                        loadPdfFromFile((File) selectedSource);
                    } else if (selectedSource instanceof Uri && Build.VERSION.SDK_INT >= 33) {
                        loadPdfFromUri((Uri) selectedSource);
                    }
                });
            });

        }
    }

    private void clearActiveUriState() {
        List<UriPermission> uriPermissions = getContentResolver().getPersistedUriPermissions();
        Log.d("URI Permissions", "Before clearing: " + uriPermissions.size());

        // Get the set of saved URIs
        Set<Uri> savedUris = getSavedUris();

        // Retrieve the additional Uri
        String treeUriString = convertPagePref.getString("treeUri", null);
        Uri treeUri = treeUriString != null ? Uri.parse(treeUriString) : null;

        for (UriPermission permission : uriPermissions) {
            Log.e("URI Permissions", permission.toString());
            Log.e("URI Permissions", permission.getUri().toString());

            // Check if the current URI is in the saved URIs set or it is the treeUri
            if (!savedUris.contains(permission.getUri()) && !permission.getUri().equals(treeUri)) {
                getContentResolver().releasePersistableUriPermission(
                        permission.getUri(),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                );
            } else {
                Log.e("URI Permissions", "Retaining permission for: " + permission.getUri().toString());
            }
        }

        List<UriPermission> uriPermissionsAfter = getContentResolver().getPersistedUriPermissions();
        Log.d("URI Permissions", "After clearing: " + uriPermissionsAfter.size());
    }

    private Set<Uri> getSavedUris() {
        // Retrieve the set of URI strings from SharedPreferences
        Set<String> uriStrings = convertPagePref.getStringSet("selectedUris", new HashSet<>());

        // Create a new set to store the converted URIs
        Set<Uri> savedUris = new HashSet<>();

        // Iterate over the string set and convert each string to a Uri
        for (String uriString : uriStrings) {
            savedUris.add(Uri.parse(uriString));
        }

        return savedUris;
    }


    public void searchAndFilterfiles(){
        //filter adapter
        searchPdf.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });

        //display changes
        searchPdf.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            searchPdf.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
                            convertText.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });

        //close display changes
        searchPdf.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        searchPdf.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT));
                        convertText.setVisibility(View.VISIBLE);
                    }
                });
                return false;
            }
        });

        if(closeBtn != null){
            closeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    searchPdf.setQuery("", false);
                    searchPdf.setIconified(true);
                }
            });
        }
    }

}