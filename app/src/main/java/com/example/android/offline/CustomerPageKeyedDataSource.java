package com.example.android.offline;

import android.arch.paging.PageKeyedDataSource;
import android.support.annotation.NonNull;
import android.util.Log;

import com.sap.cloud.android.odata.espmcontainer.Customer;
import com.sap.cloud.android.odata.espmcontainer.ESPMContainer;
import com.sap.cloud.android.odata.espmcontainer.ESPMContainerMetadata;
import com.sap.cloud.mobile.odata.DataQuery;
import com.sap.cloud.mobile.odata.QueryResult;
import com.sap.cloud.mobile.odata.offline.OfflineODataQueryFunction;

import java.util.ArrayList;
import java.util.List;

public class CustomerPageKeyedDataSource extends PageKeyedDataSource<DataQuery, CustomerListItem> {
    private ESPMContainer espmContainer;

    CustomerPageKeyedDataSource(ESPMContainer espmContainer) {
        this.espmContainer = espmContainer;
    }

    // this method is called to load the first page of data
    @Override
    public void loadInitial(@NonNull LoadInitialParams<DataQuery> params, @NonNull LoadInitialCallback<DataQuery, CustomerListItem> callback) {
        Log.d("myDebuggingTag", "Load Initial called.");
        DataQuery pageQuery = new DataQuery().from(ESPMContainerMetadata.EntitySets.customers).orderBy(Customer.lastName);  // .page(10) not needed here as set in setupOfflineStore
        QueryResult result = espmContainer.executeQuery(pageQuery);
        List<Customer> customersList = Customer.list(result.getEntityList());
        DataQuery changedCustomersQuery = new DataQuery().orderBy(Customer.lastName).filter(OfflineODataQueryFunction.upsertedLastDownload());
        List<Customer> changedCustomers = espmContainer.getCustomers(changedCustomersQuery);
        List<CustomerListItem> customerListItems = new ArrayList<>();
        for (Customer customer : customersList) {
            boolean isUpdated = changedCustomers.contains(customer);
            customerListItems.add(new CustomerListItem(customer, isUpdated));
        }
        callback.onResult(customerListItems, null, result.getNextQuery());
    }

    // TODO unclear when this method would be called, currently is not called
    @Override
    public void loadBefore(@NonNull LoadParams<DataQuery> params, @NonNull LoadCallback<DataQuery, CustomerListItem> callback) {
        Log.d("myDebuggingTag", "Load Before called.");
        DataQuery pageQuery = params.key;
        QueryResult result = espmContainer.executeQuery(pageQuery);
        List<Customer> customersList = Customer.list(result.getEntityList());
        if(pageQuery.getUrl() != null) {
            DataQuery changedCustomersQuery = new DataQuery().withURL(pageQuery.getUrl() + "&$filter=sap.upsertedlastdownload()");
            List<Customer> changedCustomers = espmContainer.getCustomers(changedCustomersQuery);
            List<CustomerListItem> customerListItems = new ArrayList<>();
            for (Customer customer : customersList) {
                boolean isUpdated = changedCustomers.contains(customer);
                customerListItems.add(new CustomerListItem(customer, isUpdated));
            }
            callback.onResult(customerListItems, result.getNextQuery());
        }
    }

    // this method is called to load the next page of data as the recycler view is scrolled
    @Override
    public void loadAfter(@NonNull LoadParams<DataQuery> params, @NonNull LoadCallback<DataQuery, CustomerListItem> callback) {
        Log.d("myDebuggingTag", "Load After called.");
        DataQuery pageQuery = params.key;
        QueryResult result = espmContainer.executeQuery(pageQuery);
        List<Customer> customersList = Customer.list(result.getEntityList());
        // the query's url is null once the last page has been loaded, this stops the list from being infinite
        if (pageQuery.getUrl() != null) {
            // query here had to be created straight from url since the pageQuery from the nextQuery is a custom query and does not support adding filter through the filter method
            // https://help.sap.com/doc/c2d571df73104f72b9f1b73e06c5609a/Latest/en-US/docs/user-guide/odata/Using_OData_API.html#custom-query
            DataQuery changedCustomersQuery = new DataQuery().withURL(pageQuery.getUrl() + "&$filter=sap.upsertedlastdownload()");
            List<Customer> changedCustomers = espmContainer.getCustomers(changedCustomersQuery);
            List<CustomerListItem> customerListItems = new ArrayList<>();
            for (Customer customer : customersList) {
                boolean isUpdated = changedCustomers.contains(customer);
                customerListItems.add(new CustomerListItem(customer, isUpdated));
            }
            // nextQuery passed in to retrieve the next page of data
            callback.onResult(customerListItems, result.getNextQuery());
        }
    }
}
