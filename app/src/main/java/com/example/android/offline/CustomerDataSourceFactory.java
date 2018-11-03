package com.example.android.offline;

import android.arch.lifecycle.MutableLiveData;
import android.arch.paging.DataSource;

import com.sap.cloud.mobile.odata.DataQuery;

// This class handles the creation of the data source
public class CustomerDataSourceFactory extends DataSource.Factory<DataQuery, CustomerListItem> {
    private StorageManager storageManager = StorageManager.getInstance();
    public MutableLiveData<CustomerPageKeyedDataSource> postLiveData = new MutableLiveData<>();

    @Override
    public DataSource<DataQuery, CustomerListItem> create() {
        CustomerPageKeyedDataSource dataSource = new CustomerPageKeyedDataSource(storageManager.getESPMContainer());

        postLiveData.postValue(dataSource);

        return dataSource;
    }
}

