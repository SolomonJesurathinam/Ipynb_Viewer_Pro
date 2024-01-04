package com.solomonj.ipynbviewerpro;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FolderListAdapter extends RecyclerView.Adapter<FolderListAdapter.ViewHolder> {
    private List<String> folderNames;
    private List<Uri> folderUris;
    private Context context;
    private OnFolderDeleteListener deleteListener;

    public FolderListAdapter(List<String> folderNames, List<Uri> folderUris, Context context, OnFolderDeleteListener deleteListener) {
        this.folderNames = folderNames;
        this.folderUris = folderUris;
        this.context = context;
        this.deleteListener = deleteListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.folder_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String folderName = folderNames.get(position);
        holder.folderNameTextView.setText(folderName);
        holder.deleteButton.setOnClickListener(v -> {
            deleteListener.onDelete(folderName, position);
        });
    }

    @Override
    public int getItemCount() {
        return folderNames.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView folderNameTextView;
        ImageButton deleteButton;

        public ViewHolder(View itemView) {
            super(itemView);
            folderNameTextView = itemView.findViewById(R.id.folderNameTextView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

    public interface OnFolderDeleteListener {
        void onDelete(String folderName, int position);
    }
}

