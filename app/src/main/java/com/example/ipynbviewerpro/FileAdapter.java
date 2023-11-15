package com.example.ipynbviewerpro;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {
    private ArrayList<File> mFiles;
    private OnItemClickListener mListener;

    public FileAdapter(ArrayList<File> files, OnItemClickListener listener) {
        mFiles = files;
        mListener = listener;
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
        Log.e("TESTING",mFiles.get(position).getName());
        File file = mFiles.get(position);
        holder.itemView.setTag(file);
        holder.fileName.setText(file.getName());
        Log.e("TESTING", file.getName());
    }

    @Override
    public int getItemCount() {
        return mFiles.size();
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
