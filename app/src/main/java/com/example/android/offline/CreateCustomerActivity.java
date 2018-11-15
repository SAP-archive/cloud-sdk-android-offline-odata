package com.example.android.offline;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.sap.cloud.android.odata.espmcontainer.Customer;
import com.sap.cloud.android.odata.espmcontainer.SalesOrderHeader;
import com.sap.cloud.mobile.odata.DataQuery;
import com.sap.cloud.mobile.odata.LocalDateTime;
import com.sap.cloud.mobile.odata.SortOrder;

import static com.example.android.offline.MainActivity.factory;
import static com.example.android.offline.MainActivity.mToast;
import static com.example.android.offline.StorageManager.adapter;

public class CreateCustomerActivity extends AppCompatActivity {
    EditText addressField;
    EditText postalCodeField;
    EditText phoneField;
    EditText cityField;
    EditText countryField;
    EditText dobField;
    EditText houseNumField;
    EditText firstNameField;
    EditText lastNameField;
    EditText emailField;
    StorageManager storageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_customer);
        setTitle("Create Customer");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        storageManager = StorageManager.getInstance();

        firstNameField = findViewById(R.id.firstname_edittext);
        lastNameField = findViewById(R.id.lastname_edittext);
        emailField = findViewById(R.id.email_edittext);
        addressField = findViewById(R.id.address_edittext);
        postalCodeField = findViewById(R.id.postal_code_edittext);
        phoneField = findViewById(R.id.phone_edittext);
        cityField = findViewById(R.id.city_edittext);
        countryField = findViewById(R.id.country_edittext);
        dobField = findViewById(R.id.dob_edittext);
        houseNumField = findViewById(R.id.house_num_edittext);

        Bundle extras = getIntent().getExtras();
        if (extras != null){
            firstNameField.setText(extras.getString("firstName"));
            lastNameField.setText(extras.getString("lastName"));
            emailField.setText(extras.getString("email"));
            addressField.setText(extras.getString("street"));
            postalCodeField.setText(extras.getString("postalNumber"));
            phoneField.setText(extras.getString("phoneNumber"));
            cityField.setText(extras.getString("city"));
            dobField.setText(extras.getString("dob"));
            houseNumField.setText(extras.getString("houseNumber"));
            countryField.setText(extras.getString("country"));
        }
    }

    public void createMessageDialog(String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(CreateCustomerActivity.this).create();
        alertDialog.setTitle(message);
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();
        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorPrimary, null));
    }

    public void onCreateCustomer() {
        // First we create a customer with default properties
        Customer newCustomer = new Customer();

        // Next we set all the customer's properties using various data validation tactics to
        // ensure that incorrect or invalid information has not been entered
        newCustomer.setFirstName(firstNameField.getText().toString());
        newCustomer.setLastName(lastNameField.getText().toString());
        // Emails must follow a certain xxx@yyy.zzz form
        if (emailField.getText().toString().matches("[^ ]*@[^ ]*\\.[^ ]*")) {
            newCustomer.setEmailAddress(emailField.getText().toString());
        }
        else {
            createMessageDialog("Please input a valid email address.");
            return;
        }
        newCustomer.setStreet(addressField.getText().toString());
        newCustomer.setPostalCode(postalCodeField.getText().toString());
        // Phone numbers can only contain numbers
        if (phoneField.getText().toString().matches("[0-9()\\- ]+")) {
            newCustomer.setPhoneNumber(String.valueOf(phoneField.getText().toString()));
        }
        else {
            createMessageDialog("Please input a valid phone number (digits, parentheses, dashes).");
            return;
        }
        newCustomer.setCity(cityField.getText().toString());
        // Country codes must be length 2, i.e. CA or US
        if (countryField.getText().toString().length() == 2) {
            newCustomer.setCountry(countryField.getText().toString().toUpperCase());
        }
        else {
            createMessageDialog("Country code must be 2 characters.");
            return;
        }
        // Date of birth must not be in the future
        if (!dobField.getText().toString().isEmpty() && LocalDateTime.parse(dobField.getText().toString()) != null) {
            LocalDateTime dob = LocalDateTime.parse(dobField.getText().toString());
            if (dob.lessEqual(LocalDateTime.now())) {
                newCustomer.setDateOfBirth(dob);
            }
            else {
                createMessageDialog("Date of birth cannot be in the future.");
                return;
            }
        }
        newCustomer.setHouseNumber(houseNumField.getText().toString());
        newCustomer.unsetDataValue(Customer.customerID);
        storageManager.getOfflineODataProvider().createEntity(newCustomer, null, null);
        Log.d(MainActivity.TAG, "Read Link: " + newCustomer.getReadLink());
        Log.d(MainActivity.TAG, "Edit Link: " + newCustomer.getEditLink());
        // Before uploading, the customer does not have an ID (it's generated by the server)
        Log.d(MainActivity.TAG, "Customer ID is set: " + newCustomer.hasDataValue(Customer.customerID));
        if (getIntent().getStringExtra("parent_activity") == null) {
            adapter.notifyDataSetChanged();
        }
        else {
            factory.postLiveData.getValue().invalidate();
        }
        Log.d("myDebuggingTag", "Successfully created the new customer locally.");
        mToast = Toast.makeText(this, "Successfully created the new customer locally.", Toast.LENGTH_LONG);
        mToast.show();
        onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.create_customer_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        else if (id == R.id.action_save) {
            onCreateCustomer();
        }
        return super.onOptionsItemSelected(item);
    }
}
