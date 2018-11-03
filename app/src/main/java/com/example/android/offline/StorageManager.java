package com.example.android.offline;

import android.arch.lifecycle.LiveData;
import android.arch.paging.PagedList;

import com.sap.cloud.android.odata.espmcontainer.ESPMContainer;
import com.sap.cloud.mobile.odata.offline.OfflineODataProvider;

public class StorageManager {
    private static OfflineODataProvider offlineODataProvider;
    private static ESPMContainer espmContainer;
    public static CustomerRecyclerViewAdapter adapter;
    // storing the customer paged list in a live data object to allow it to be observed and update the recycler view accordingly
    public static LiveData<PagedList<CustomerListItem>> customersListToDisplay;

    private static final StorageManager INSTANCE = new StorageManager();

    public static StorageManager getInstance() {
        return INSTANCE;
    }

    public LiveData<PagedList<CustomerListItem>> getCustomersListToDisplay() {
        return customersListToDisplay;
    }

    public void setCustomersListToDisplay(LiveData<PagedList<CustomerListItem>> list) {
        customersListToDisplay = list;
    }

    public OfflineODataProvider getOfflineODataProvider() {
        return offlineODataProvider;
    }

    public void setOfflineODataProvider(OfflineODataProvider o) {
        offlineODataProvider = o;
    }

    public ESPMContainer getESPMContainer() {
        return espmContainer;
    }

    public void setESPMContainer(ESPMContainer ec) {
        espmContainer = ec;
    }

}
