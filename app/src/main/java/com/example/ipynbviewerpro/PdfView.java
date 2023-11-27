package com.example.ipynbviewerpro;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.print.PdfPrint;
import android.print.PrintAttributes;
import android.webkit.WebView;
import java.io.File;


public class PdfView {

    private static final int REQUEST_CODE=101;

    /**
     * convert webview content into to pdf file
     * @param activity pass the current activity context
     * @param webView webview
     * @param directory directory path where pdf file will be saved
     * @param fileName name of the pdf file.
     * */
    public static void createWebPrintJob(Activity activity, WebView webView, File directory, String fileName, final Callback callback) {

        //check the marshmallow permission
        if (Build.VERSION.SDK_INT>= 23 && Build.VERSION.SDK_INT < 29) {
            if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
                callback.failure();
                return;
            }
        }

        String jobName = activity.getString(R.string.app_name) + " Document";
        PrintAttributes attributes = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            attributes = new PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(new PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS).build();
        }
        PdfPrint pdfPrint = new PdfPrint(attributes);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            pdfPrint.print(webView.createPrintDocumentAdapter(jobName), directory, fileName, new PdfPrint.CallbackPrint() {
                @Override
                public void success(String path) {
                    callback.success(path);
                }

                @Override
                public void onFailure() {
                    callback.failure();
                }
            });
        }else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                pdfPrint.print(webView.createPrintDocumentAdapter(), directory, fileName, new PdfPrint.CallbackPrint() {
                    @Override
                    public void success(String path) {
                        callback.success(path);
                    }

                    @Override
                    public void onFailure() {
                        callback.failure();
                    }
                });
            }
        }
    }


    /** callback interface to get the result back after created pdf file*/
    public interface Callback{
        void success(String path);
        void failure();
    }
}
