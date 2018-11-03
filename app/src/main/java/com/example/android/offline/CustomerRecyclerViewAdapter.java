package com.example.android.offline;

import android.arch.paging.PagedList;
import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.view.View;
import android.view.ViewGroup;

import com.sap.cloud.android.odata.espmcontainer.Customer;

public class CustomerRecyclerViewAdapter extends BaseAdapter<CustomerListItem> {

    private RecyclerCallback callback;
    private static final DiffUtil.ItemCallback<CustomerListItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CustomerListItem>() {
                @Override
                public boolean areItemsTheSame(CustomerListItem oldItem, CustomerListItem newItem) {
                    return oldItem == newItem;
                }

                @Override
                public boolean areContentsTheSame(CustomerListItem oldItem, CustomerListItem newItem) {
                    return oldItem.customer.getCustomerID().equals(newItem.customer.getCustomerID());
                }
            };

    // this allows the finishLoading method from the MainActivity to be called within the adapter
    interface RecyclerCallback {
        void finishLoading();
    }

    CustomerRecyclerViewAdapter(Context context, RecyclerCallback callback) {
        super(context, DIFF_CALLBACK);
        this.callback = callback;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.rv_row, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // Get the customer bound to the recycler view cell
        CustomerListItem customerListItem = getItem(position);
        Customer customer = customerListItem.customer;
        boolean isUpdated = customerListItem.isUpdated;

        // Get ready to update the corresponding object cell's image
        holder.myObjectCell.prepareDetailImageView().setVisibility(View.VISIBLE);
        holder.myObjectCell.setPreserveDetailImageSpacing(false);

        // Change the image shown in the object cell's detailed image section based on the customer's state
        if (customer.getInErrorState()) {
            // Display a red exclamation image if the customer is in the "Error" state
            holder.myObjectCell.setIcon(R.mipmap.ic_error_state, 0, R.string.red_dot);
        }
        else if (customer.isLocal()) {
            // Display an orange refresh symbol if the customer needs to be sync'd to the backend
            holder.myObjectCell.setIcon(R.mipmap.ic_local_state, 0, R.string.orange_dot);
        }
        else if (isUpdated) {
            // Display a yellow exclamation mark if the customer has been updated since the app was opened
            holder.myObjectCell.setIcon(R.mipmap.ic_yellow_bang_two, 0, R.string.yellow_dot);
        }
        else {
            holder.myObjectCell.setIcon(R.mipmap.ic_empty_dot, 0, R.string.white_dot);
        }
        holder.myObjectCell.setDetailImage(R.mipmap.ic_person_icon);
        holder.myObjectCell.setHeadline(customer.getLastName() + ", " + customer.getFirstName());
        holder.myObjectCell.setSubheadline(customer.getStreet() + ", " + customer.getCity() + ", " + customer.getCountry());
    }

    // dismisses the loading spinner once the recycler view has been updated after a sync
    @Override
    public void onCurrentListChanged(@Nullable PagedList<CustomerListItem> currentList) {
        super.onCurrentListChanged(currentList);
        callback.finishLoading();
    }
}