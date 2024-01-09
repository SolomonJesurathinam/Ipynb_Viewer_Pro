package com.solomonj.ipynbviewerpro;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {
    private final List<Object> mOriginalItems;
    private final List<Object> mFilteredItems;
    private final OnItemClickListener mListener;
    private final Context mContext;

    public FileAdapter(Context context, List<Object> items, OnItemClickListener listener) {
        mContext = context;
        mOriginalItems = items;
        mListener = listener;
        mFilteredItems = new ArrayList<>(items); // Initialize with all items
    }

    public interface OnItemClickListener {
        void onItemClick(Object item);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.file_item, parent, false);
        return new ViewHolder(view, mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Object item = mFilteredItems.get(position);
        holder.itemView.setTag(item);

        String fileName = "";
        if (item instanceof File) {
            fileName = ((File) item).getName();
        } else if (item instanceof Uri) {
            DocumentFile docFile = DocumentFile.fromSingleUri(mContext, (Uri) item);
            if (docFile != null) {
                fileName = docFile.getName();
            }
        }
        holder.fileName.setText(fileName);
    }

    @Override
    public int getItemCount() {
        return mFilteredItems.size();
    }

    public void filter(String text, Runnable callback) {
        final String finalText = text.toLowerCase(); // Create a final copy of text

        new Thread(() -> {
            mFilteredItems.clear();
            if (finalText.isEmpty()) {
                mFilteredItems.addAll(mOriginalItems);
            } else {
                for (Object item : mOriginalItems) {
                    String fileName = "";
                    if (item instanceof File) {
                        fileName = ((File) item).getName().toLowerCase();
                    } else if (item instanceof Uri) {
                        DocumentFile docFile = DocumentFile.fromSingleUri(mContext, (Uri) item);
                        if (docFile != null) {
                            fileName = docFile.getName().toLowerCase();
                        }
                    }
                    if (fileName.contains(finalText)) {
                        mFilteredItems.add(item);
                    }
                }
            }
            callback.run();
        }).start();
    }



    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView fileName;

        public ViewHolder(View itemView, final OnItemClickListener listener) {
            super(itemView);
            fileName = itemView.findViewById(R.id.file_name);
            itemView.setOnClickListener(view -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(itemView.getTag());
                    }
                }
            });
        }
    }
}
