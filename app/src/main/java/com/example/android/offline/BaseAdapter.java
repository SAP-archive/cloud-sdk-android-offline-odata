package com.example.android.offline;

import android.arch.paging.PagedListAdapter;
import android.content.Context;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sap.cloud.mobile.fiori.contact.ContactCell;
import com.sap.cloud.mobile.fiori.object.ObjectCell;

// Generically typed class to handle the common portions of the adapters
public class BaseAdapter<T> extends PagedListAdapter<T, BaseAdapter<T>.ViewHolder> {

    protected LayoutInflater mInflater;
    protected ItemClickListener mClickListener;

    // data is passed into the constructor
    BaseAdapter(Context context, DiffUtil.ItemCallback<T> diffCallback) {
        super(diffCallback);
        this.mInflater = LayoutInflater.from(context);
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(new View(mInflater.getContext()));
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(BaseAdapter<T>.ViewHolder holder, int position) {

    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ObjectCell myObjectCell;

        ViewHolder(View itemView) {
            super(itemView);
            myObjectCell = itemView.findViewById(R.id.objectCell);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    // allows click events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }
}