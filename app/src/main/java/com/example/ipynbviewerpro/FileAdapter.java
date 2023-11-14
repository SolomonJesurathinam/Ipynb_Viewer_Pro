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

    public FileAdapter(ArrayList<File> files) {
        mFiles = files;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.file_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Log.e("TESTING",mFiles.get(position).getName());
        holder.fileName.setText(mFiles.get(position).getName());
        // Set onClickListener if needed to handle file selection
    }

    @Override
    public int getItemCount() {
        return mFiles.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView fileName;

        public ViewHolder(View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.file_name);
        }
    }
}
