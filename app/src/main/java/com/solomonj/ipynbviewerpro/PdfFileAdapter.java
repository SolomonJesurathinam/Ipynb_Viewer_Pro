package com.solomonj.ipynbviewerpro;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class PdfFileAdapter extends RecyclerView.Adapter<PdfFileAdapter.ViewHolder> {

    private final List<Object> pdfSources;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    PdfFileAdapter(Context context, List<Object> data) {
        this.mInflater = LayoutInflater.from(context);
        this.pdfSources = data;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recycler_file, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Object source = pdfSources.get(position);
        if (source instanceof File) {
            holder.myTextView.setText(((File) source).getName());
        } else if (source instanceof Uri) {
            holder.myTextView.setText(DocumentFile.fromSingleUri(holder.myTextView.getContext(), (Uri) source).getName());
        }
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return pdfSources.size();
    }

    // convenience method for getting data at click position
    Object getItem(int id) {
        return pdfSources.get(id);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView myTextView;
        ImageView menuIcon;


        ViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.tvPdfFileName);
            menuIcon = itemView.findViewById(R.id.share_icon);
            itemView.setOnClickListener(this);
            menuIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPopupMenu(v, getAdapterPosition());
                }
            });
        }

        private void showPopupMenu(View view, int position) {
            Context wrapper = new ContextThemeWrapper(view.getContext(), R.style.PopupMenuStyle);
            PopupMenu popup = new PopupMenu(wrapper, view);
            popup.getMenuInflater().inflate(R.menu.recycler_item_menu, popup.getMenu());

            // Reflection to enable icons display
            try {
                Field fieldPopup = popup.getClass().getDeclaredField("mPopup");
                fieldPopup.setAccessible(true);
                Object menuPopupHelper = fieldPopup.get(popup);
                Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                Method setForceShowIcon = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                setForceShowIcon.invoke(menuPopupHelper, true);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Registering popup with OnMenuItemClickListener
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == R.id.action_share) {
                        sharePdfFile(position);
                        return true;
                    }
                    if(item.getItemId() == R.id.action_view){
                        Object pdfSource = pdfSources.get(position);
                        if(pdfSource != null){
                            ((ConvertedFiles) mInflater.getContext()).loadPdfInBackground(pdfSource);
                        }
                        return true;
                    }
                    return false;
                }
            });

            popup.show(); // Showing the popup menu
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }

        // Method to handle sharing of PDF file
        private void sharePdfFile(int position) {
            Object pdfSource = pdfSources.get(position);
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");

            if (pdfSource instanceof File) {
                // Convert File to content URI
                File file = (File) pdfSource;
                Uri contentUri = FileProvider.getUriForFile(mInflater.getContext(),
                        mInflater.getContext().getApplicationContext().getPackageName() + ".provider",
                        file);

                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Grant temporary read permission
            } else if (pdfSource instanceof Uri) {
                // Handle Uri if necessary
                shareIntent.putExtra(Intent.EXTRA_STREAM, (Uri) pdfSource);
            }

            if (shareIntent.resolveActivity(mInflater.getContext().getPackageManager()) != null) {
                mInflater.getContext().startActivity(Intent.createChooser(shareIntent, "Share PDF"));
            }
        }
    }
}
