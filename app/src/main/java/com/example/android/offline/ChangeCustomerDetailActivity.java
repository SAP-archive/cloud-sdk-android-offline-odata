package com.example.android.offline;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.sap.cloud.android.odata.espmcontainer.Customer;
import com.sap.cloud.android.odata.espmcontainer.SalesOrderHeader;
import com.sap.cloud.mobile.fiori.contact.ProfileHeader;
import com.sap.cloud.mobile.odata.DataQuery;
import com.sap.cloud.mobile.odata.LocalDateTime;
import com.sap.cloud.mobile.odata.SortOrder;

import static com.example.android.offline.MainActivity.mToast;
import static com.example.android.offline.StorageManager.adapter;

public class ChangeCustomerDetailActivity extends AppCompatActivity {
    EditText dobField;
    EditText cityField;
    EditText phoneField;
    EditText addressField;
    EditText houseNumField;
    EditText postalCodeField;

    StorageManager storageManager;
    static Customer customer;

    private boolean fieldChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_customer_detail);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Change Customer");
        setProfileHeader();
        dobField = findViewById(R.id.dob_edittext);
        cityField = findViewById(R.id.city_edittext);
        phoneField = findViewById(R.id.phone_edittext);
        addressField = findViewById(R.id.address_edittext);
        houseNumField = findViewById(R.id.house_num_edittext);
        storageManager = StorageManager.getInstance();
        postalCodeField = findViewById(R.id.postal_code_edittext);

        dobField.setText(customer.getDateOfBirth().toString());
        cityField.setText(customer.getCity());
        phoneField.setText(customer.getPhoneNumber());
        addressField.setText(customer.getStreet());
        houseNumField.setText(customer.getHouseNumber());
        postalCodeField.setText(customer.getPostalCode());

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                fieldChanged = true;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        };
        dobField.addTextChangedListener(textWatcher);
        cityField.addTextChangedListener(textWatcher);
        phoneField.addTextChangedListener(textWatcher);
        addressField.addTextChangedListener(textWatcher);
        houseNumField.addTextChangedListener(textWatcher);
        postalCodeField.addTextChangedListener(textWatcher);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.change_customer_detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_update) {
            onSave();
        } else if(id == R.id.action_delete) {
            onDelete();
        }
        return super.onOptionsItemSelected(item);
    }


    private void setProfileHeader() {
        ProfileHeader customerHeader = findViewById(R.id.profile_header);
        customerHeader.setHeadline(customer.getFirstName() + " " + customer.getLastName());
        customerHeader.setDetailImage(R.drawable.ic_account_circle_white_24dp);
        customerHeader.setSubheadline(customer.getEmailAddress());
    }

    public void onSave() {
        if (fieldChanged) {
            if (!dobField.getText().toString().isEmpty() && LocalDateTime.parse(dobField.getText().toString()) != null) {
                LocalDateTime dob = LocalDateTime.parse(dobField.getText().toString());
                if (dob.lessEqual(LocalDateTime.now())) {
                    customer.setDateOfBirth(dob);
                } else {
                    mToast = Toast.makeText(this, "ERROR: Date of birth cannot be in the future.", Toast.LENGTH_LONG);
                    mToast.show();
                    return;
                }
            }

            if (phoneField.getText().toString().matches("[0-9()\\- ]+")) {
                customer.setPhoneNumber(String.valueOf(phoneField.getText().toString()));
            } else {
                mToast = Toast.makeText(this, "ERROR: Phone number can only contain digits, parentheses and dashes.", Toast.LENGTH_LONG);
                mToast.show();
                return;
            }
            customer.setStreet(addressField.getText().toString());
            customer.setCity(cityField.getText().toString());
            customer.setPostalCode(postalCodeField.getText().toString());
            customer.setHouseNumber(houseNumField.getText().toString());
            adapter.notifyDataSetChanged();

            storageManager.getESPMContainer().updateEntityAsync(customer, () ->
                            Log.d("myDebuggingTag", "Successfully Changed Customer's Data")
                    , (error) ->
                            Log.d("myDebuggingTag", "Error getting customers: " + error.getMessage())
            );
            mToast = Toast.makeText(this, "Successfully updated the customer locally.", Toast.LENGTH_LONG);
            mToast.show();
            mToast = null;
            onBackPressed();
        } else {
            mToast = Toast.makeText(this, "No properties were changed.", Toast.LENGTH_LONG);
            mToast.show();
            mToast = null;
        }

    }

    public void onDelete() {
        AlertDialog alertDialog = new AlertDialog.Builder(ChangeCustomerDetailActivity.this).create();
        alertDialog.setTitle("Are you sure you want to delete the customer?");
        alertDialog.setMessage("This action cannot be undone.");
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "DELETE",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(customer.hasDataValue(Customer.customerID)) {
                            String cId = customer.getCustomerID();
                            DataQuery getAllSalesOrders = new DataQuery().orderBy(SalesOrderHeader.salesOrderID, SortOrder.ASCENDING).filter(SalesOrderHeader.customerID.equal(cId));
                            storageManager.getESPMContainer().getSalesOrderHeadersAsync(getAllSalesOrders, (salesOrders) -> {
                                for (SalesOrderHeader salesOrder : salesOrders) {
                                    storageManager.getESPMContainer().deleteEntity(salesOrder);
                                }
                                deleteCustomer();
                            }, (error) -> {
                                Log.d("myDebuggingTag", "Error getting customers: " + error.getMessage());
                            });
                        } else {
                            deleteCustomer();
                        }
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();
        alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(getResources().getColor(R.color.colorPrimary, null));
        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorPrimary, null));
    }

    public void deleteCustomer() {
        storageManager.getESPMContainer().deleteEntityAsync(customer, () -> {
            Log.d("myDebuggingTag", "Successfully Deleted Customer's Data Locally");
            MainActivity.factory.postLiveData.getValue().invalidate();
            mToast = Toast.makeText(this, "Customer successfully deleted locally.", Toast.LENGTH_LONG);
            mToast.show();
            onBackPressed();
        }, (error) -> Log.d("myDebuggingTag", "Error getting customers: " + error.getMessage()));
    }

}
