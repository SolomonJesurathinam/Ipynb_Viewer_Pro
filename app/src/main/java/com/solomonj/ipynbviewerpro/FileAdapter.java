package com.solomonj.ipynbviewerpro;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {
    private ArrayList<File> mFiles;
    private ArrayList<File> mFilesFiltered;
    private OnItemClickListener mListener;

    public FileAdapter(ArrayList<File> files, OnItemClickListener listener) {
        mFiles = files;
        mListener = listener;
        mFilesFiltered = new ArrayList<>(files); // Initialize with all files
    }

    public interface OnItemClickListener {
        void onItemClick(File file);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.file_item, parent, false);
        return new ViewHolder(view,mListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Log.e("TESTING",mFilesFiltered.get(position).getName());
        File file = mFilesFiltered.get(position);
        holder.itemView.setTag(file);
        holder.fileName.setText(file.getName());
        Log.e("TESTING", file.getName());
    }

    @Override
    public int getItemCount() {
        return mFilesFiltered.size();
    }

    public void filter(String text) {
        mFilesFiltered.clear();
        if (text.isEmpty()) {
            mFilesFiltered.addAll(mFiles);
        } else {
            text = text.toLowerCase();
            for (File file : mFiles) {
                if (file.getName().toLowerCase().contains(text)) {
                    mFilesFiltered.add(file);
                }
            }
        }
        notifyDataSetChanged();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView fileName;

        public ViewHolder(View itemView, final OnItemClickListener listener) {
            super(itemView);
            fileName = itemView.findViewById(R.id.file_name);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick((File) itemView.getTag());
                        }
                    }
                }
            });
        }
    }
}
