package com.example.android.offline;

import com.sap.cloud.android.odata.espmcontainer.Customer;

public class CustomerListItem {
    Customer customer;
    boolean isUpdated;

    CustomerListItem(Customer customer, boolean isUpdated) {
        this.customer = customer;
        this.isUpdated = isUpdated;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof CustomerListItem)) {
            return false;
        }
        CustomerListItem other = (CustomerListItem) o;
        return this.customer.equals(other.customer);
    }
}
