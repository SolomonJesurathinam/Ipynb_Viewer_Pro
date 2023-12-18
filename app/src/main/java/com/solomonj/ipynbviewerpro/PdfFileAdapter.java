package com.solomonj.ipynbviewerpro;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
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

        ViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.tvPdfFileName);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }
}
