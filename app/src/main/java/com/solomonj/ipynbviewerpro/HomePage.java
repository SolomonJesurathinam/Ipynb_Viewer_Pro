package com.solomonj.ipynbviewerpro;

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
import androidx.recyclerview.widget.RecyclerView;
import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import androidx.recyclerview.widget.LinearLayoutManager;

public class HomePage extends AppCompatActivity {

    Button choosefile, retrieveAll, convertOnline;
    private ActivityResultLauncher<String[]> mGetContent;
    SharedPreferences homePagePref;
    RadioGroup radioRender;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    RecyclerView recyclerView;
    private ActivityResultLauncher<Intent> manageExternalStorageActivityResultLauncher;
    LinearLayout recyclerLayout;
    ImageView feedback, convertedFiles;
    SearchView searchIpynb;
    private FileAdapter adapter;
    View closeButton;
    TextView homeTextLocal;
    private FolderListAdapter adapter1;

    //new changes
    private static final int REQUEST_CODE = 1;
    private ArrayList<Uri> selectedFolderUris = new ArrayList<>();
    private ArrayList<String> selectedFolderNames = new ArrayList<>();

    private final ActivityResultLauncher<Intent> folderPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri folderUri = result.getData().getData();
                    if (!selectedFolderUris.contains(folderUri)) {
                        selectedFolderUris.add(folderUri);

                        // Retrieve and store the folder name
                        String folderName = getFolderName(folderUri);
                        selectedFolderNames.add(folderName);

                        // Persist permission
                        int takeFlags = result.getData().getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        getContentResolver().takePersistableUriPermission(folderUri, takeFlags);

                        saveUrisToSharedPreferences();
                    }

                    if (adapter1 != null) {
                        adapter1.notifyDataSetChanged();
                    }
                    showFolderSelectionPopup();
                }else {
                    showFolderSelectionPopup();
                }
            }
    );

    private String getFolderName(Uri folderUri) {
        DocumentFile documentFile = DocumentFile.fromTreeUri(this, folderUri);
        if (documentFile != null && documentFile.isDirectory()) {
            return documentFile.getName(); // This should return the display name of the folder
        }
        return "Unknown Folder";
    }

    private void saveUrisToSharedPreferences() {
        Set<String> uriStrings = new HashSet<>();
        for (Uri uri : selectedFolderUris) {
            uriStrings.add(uri.toString());
        }

        SharedPreferences.Editor editor = homePagePref.edit();
        editor.putStringSet("selectedUris", uriStrings);
        editor.apply();
    }

    private void loadUrisFromSharedPreferences() {
        Set<String> uriStrings = homePagePref.getStringSet("selectedUris", new HashSet<>());
        selectedFolderUris.clear();
        for (String uriString : uriStrings) {
            selectedFolderUris.add(Uri.parse(uriString));
        }

        // Update folder names based on URIs
        updateFolderNames();
    }

    private void updateFolderNames() {
        selectedFolderNames.clear();
        for (Uri uri : selectedFolderUris) {
            selectedFolderNames.add(getFolderName(uri));
        }
        if (adapter1 != null) {
            adapter1.notifyDataSetChanged();
        }
    }


    public void showFolderSelectionPopup(){

        // Inflate the popup layout
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_folder_selection, null);

        // Create the PopupWindow
        PopupWindow popupWindow = new PopupWindow(popupView, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Initialize the RecyclerView in the PopupWindow
        RecyclerView recyclerViewPopup = popupView.findViewById(R.id.recyclerViewPopup);
        if (adapter1 != null) {
            adapter1 = new FolderListAdapter(selectedFolderNames, selectedFolderUris, this, this::deleteFolder);
        }

        recyclerViewPopup.setAdapter(adapter1);
        recyclerViewPopup.setLayoutManager(new LinearLayoutManager(this));

        // Handle the button click inside the popup
        Button btnDone = popupView.findViewById(R.id.btnDone);
        btnDone.setOnClickListener(v -> popupWindow.dismiss());

        // Handle the close button click
        Button btnClose = popupView.findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> popupWindow.dismiss());

        //Handle for Select More
        Button selectMore = popupView.findViewById(R.id.selectMore);
        selectMore.setOnClickListener(v -> {
            popupWindow.dismiss();
            // Launch the folder picker intent
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            folderPickerLauncher.launch(intent);
        });

        // Show the PopupWindow
        popupWindow.showAtLocation(findViewById(R.id.homePage), Gravity.CENTER, 0, 0);
    }

    private void deleteFolder(String folderName, int position) {
        selectedFolderNames.remove(position);
        selectedFolderUris.remove(position);

        // Update SharedPreferences
        saveUrisToSharedPreferences();

        if (adapter1 != null) {
            adapter1.notifyItemRemoved(position);
            adapter1.notifyItemRangeChanged(position, selectedFolderNames.size());
        }
    }


    private void selectAnotherFolder(DialogInterface dialog, int which) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        folderPickerLauncher.launch(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homepage);

        //Shared Preference
        homePagePref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        homePagePref.getString("renderKey","Real");

        //Locators
        recyclerLayout = findViewById(R.id.recyclerLayout);
        choosefile = findViewById(R.id.choosefile);
        radioRender = findViewById(R.id.radioRender);
        retrieveAll = findViewById(R.id.retrieveAll);
        recyclerView = findViewById(R.id.recyclerViewFiles);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        feedback = findViewById(R.id.feedback);
        convertOnline = findViewById(R.id.convertOnline);
        convertedFiles = findViewById(R.id.convertedFiles);
        searchIpynb = findViewById(R.id.searchIpynb);
        closeButton = searchIpynb.findViewById(androidx.appcompat.R.id.search_close_btn);
        homeTextLocal = findViewById(R.id.homeTextLocal);


        //Functions
        fileHandling();
        radiobuttonLogic();
        //Android 13
        storagePermissionandroid11();
        //Storage access
        scanFilesLogic();
        //displaying all the files
        displayRecyclerView();
        feedbackLogic();
        convertOnlinebutton();
        convertedFilesButton();
        searchIpynbfilesLogic();

        loadUrisFromSharedPreferences();

        // Initialize the adapter here with empty lists or existing data
        adapter1 = new FolderListAdapter(new ArrayList<>(), new ArrayList<>(), this, this::deleteFolder);
    }

    public void feedbackLogic(){
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
    }

    public void convertOnlinebutton(){
        convertOnline.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(),OnlineActivity.class);
            startActivity(intent);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.slide_up, R.anim.stay);
            } else {
                overridePendingTransition(R.anim.slide_up, R.anim.stay);
            }
        }
    });
    }

    public void convertedFilesButton(){
        convertedFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(),ConvertedFiles.class);
                startActivity(intent);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.slide_up, R.anim.stay);
                } else {
                    overridePendingTransition(R.anim.slide_up, R.anim.stay);
                }
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
                            String fileName = getFilename(getApplicationContext(),uri);
                            if (fileName != null && fileName.endsWith(".ipynb")) {
                                getContentResolver().takePersistableUriPermission(
                                        uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                );
                                //Toast.makeText(getApplicationContext(),uri.getPath(),Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(getApplicationContext(), Webview.class);
                                intent.putExtra("filePath",uri.toString());
                                startActivity(intent);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                    overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.slide_up, R.anim.stay);
                                } else {
                                    overridePendingTransition(R.anim.slide_up, R.anim.stay);
                                }
                            } else {
                                Toast.makeText(getApplicationContext(),"Only ipynb files are allowed",Toast.LENGTH_LONG).show();
                            }

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
                    Toast.makeText(getApplicationContext(),"Scan Complete",Toast.LENGTH_SHORT).show();
                } else {
                    // Permission denied, show a Toast
                    Toast.makeText(this, "Storage access is required to display all jupyter files", Toast.LENGTH_SHORT).show();
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        showPermissionSettingsDialog();
                    }
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
                            Toast.makeText(getApplicationContext(),"Scan Complete",Toast.LENGTH_SHORT).show();

                        }
                    }
                });
    }

    //displaying recycler view
    public void displayRecyclerView(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            retrieveIpynbFiles();
        }
        else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            retrieveIpynbFiles();
        }
    }


    //Get al ipynb files and display in the recycler view
    public void retrieveIpynbFiles(){
        // You have the permission, start file scan
        ArrayList<File> ipynbFiles = findIpynbFiles(Environment.getExternalStorageDirectory());
        adapter = new FileAdapter(ipynbFiles, new FileAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(File file) {
                String javaURI = file.toURI().toString();
                Uri uri = Uri.parse(javaURI);
                Intent intent = new Intent(getApplicationContext(), Webview.class);
                intent.putExtra("filePath",uri.toString());
                startActivity(intent);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.slide_up, R.anim.stay);
                } else {
                    overridePendingTransition(R.anim.slide_up, R.anim.stay);
                }
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
        if(ipynbFiles.size() >0){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    searchIpynb.setVisibility(View.VISIBLE);
                }
            });
        }
    }


    //Logic for scan files button
    public void scanFilesLogic(){
        retrieveAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if(selectedFolderUris.size() <=0){
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        folderPickerLauncher.launch(intent);
                    }else{
                        showFolderSelectionPopup();
                    }
                }
                else{
                    if(ContextCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(HomePage.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                    }else{
                        displayRecyclerView();
                        Toast.makeText(getApplicationContext(),"Scan Complete",Toast.LENGTH_SHORT).show();
                    }
                }

            }
        });
    }


    //Dialog box
    private void showPermissionSettingsDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Allow Permission Manually")
                .setMessage("Need Storage Permission to display all the jupyter files, please allow manually from settings.")
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

    public String getFilename(Context context, Uri uri){
        String fileName = null;
        if(uri.getScheme().equalsIgnoreCase("content")){
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (fileName == null) {
                fileName = uri.getLastPathSegment();
            }
        }else if(uri.getScheme().equalsIgnoreCase("file")){
            fileName = uri.getLastPathSegment();
        }
        return fileName;
    }

    public void searchIpynbfilesLogic(){
        searchIpynb.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return false;
            }
        });

        //focus
        searchIpynb.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            searchIpynb.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
                            homeTextLocal.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });

        if(closeButton != null){
            closeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    searchIpynb.setQuery("", false);
                    searchIpynb.setIconified(true);
                }
            });
        }

        //onclose
        searchIpynb.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        searchIpynb.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT));
                        homeTextLocal.setVisibility(View.VISIBLE);
                    }
                });
                return false;
            }
        });
    }
}