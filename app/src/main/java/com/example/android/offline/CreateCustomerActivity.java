package com.example.android.offline;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.sap.cloud.android.odata.espmcontainer.Customer;
import com.sap.cloud.mobile.odata.DataQuery;
import com.sap.cloud.mobile.odata.LocalDateTime;

import static com.example.android.offline.ChangeCustomerWarningActivity.CHANGE_CUSTOMER_WARNING_CLASS_NAME;
import static com.example.android.offline.MainActivity.factory;
import static com.example.android.offline.MainActivity.mToast;
import static com.example.android.offline.MainActivity.myTag;
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
    Button createButton;

    StorageManager storageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_customer);
        setTitle("Create Customer");
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
        createButton = findViewById(R.id.create_button);

        Bundle extras = getIntent().getExtras();
        if (extras != null){
            firstNameField.setText((String) extras.getString("firstName"));
            lastNameField.setText((String) extras.getString("lastName"));
            emailField.setText((String) extras.getString("email"));
            addressField.setText((String) extras.getString("street"));
            postalCodeField.setText((String) extras.getString("postalNumber"));
            phoneField.setText((String) extras.getString("phoneNumber"));
            cityField.setText((String) extras.getString("city"));
            dobField.setText((String) extras.getString("dob"));
            houseNumField.setText((String) extras.getString("houseNumber"));
            countryField.setText((String) extras.getString("country"));
        }

    }

    public void onCreateCustomer(View view) {
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
            mToast = Toast.makeText(this, "ERROR: Not a valid email address.", Toast.LENGTH_LONG);
            mToast.show();
            return;
        }
        newCustomer.setStreet(addressField.getText().toString());
        newCustomer.setPostalCode(postalCodeField.getText().toString());
        // Phone numbers can only contain numbers
        if (phoneField.getText().toString().matches("[0-9()\\- ]+")) {
            newCustomer.setPhoneNumber(String.valueOf(phoneField.getText().toString()));
        }
        else {
            mToast = Toast.makeText(this, "ERROR: Phone number can only contain digits, parentheses and dashes.", Toast.LENGTH_LONG);
            mToast.show();
            return;
        }
        newCustomer.setCity(cityField.getText().toString());
        // Country codes must be length 2, i.e. CA or US
        if(countryField.getText().toString().length() == 2) {
            newCustomer.setCountry(countryField.getText().toString().toUpperCase());
        }
        else {
            mToast = Toast.makeText(this, "ERROR: Country code must be 2 digits.", Toast.LENGTH_LONG);
            mToast.show();
            return;
        }
        // Date of birth must not be in the future
        if (!dobField.getText().toString().isEmpty() && LocalDateTime.parse(dobField.getText().toString()) != null) {
            LocalDateTime dob = LocalDateTime.parse(dobField.getText().toString());
            if (dob.lessEqual(LocalDateTime.now())) {
                newCustomer.setDateOfBirth(dob);
            }
            else {
                mToast = Toast.makeText(this, "ERROR: Date of birth cannot be in the future.", Toast.LENGTH_LONG);
                mToast.show();
                return;
            }
        }
        newCustomer.setHouseNumber(houseNumField.getText().toString());
        newCustomer.unsetDataValue(Customer.customerID);
        storageManager.getOfflineODataProvider().createEntity(newCustomer, null, null);
        Log.d(myTag, "Read Link: " + newCustomer.getReadLink());
        Log.d(myTag, "Edit Link: " + newCustomer.getEditLink());
        // Before uploading, the customer does not have an ID (it's generated by the server)
        Log.d(myTag, "Customer ID is set: " + newCustomer.hasDataValue(Customer.customerID));
        storageManager.getOfflineODataProvider().upload(() -> {
            storageManager.getOfflineODataProvider().download(() -> {
                // Using the readLink from before the upload we can refer to the same entity after the upload as well
                String originalReadLink = newCustomer.getReadLink();
                Log.d(myTag, "Readlink before upload: " + originalReadLink);
                DataQuery query = new DataQuery().withURL(originalReadLink);
                Customer afterDownload = storageManager.getESPMContainer().getCustomer(query);

                // The readLink of the entity changed after it has been redownloaded,
                // but the old local readLink is still valid
                Log.d(myTag, "Readlink after download: " + afterDownload.getReadLink());
                // Now that the customer has been uploaded, the ID has been set by the backend
                // So afterDownload.hasDataValue(Customer.customerID) will be true, and
                // Customer ID: will be a long string of letters and numbers
                Log.d(myTag, "After download, customer ID is set: " + afterDownload.hasDataValue(Customer.customerID));
                Log.d(myTag, "Customer ID: " + afterDownload.getCustomerID());
            }, (error) -> {
                Log.e(myTag, "Error during download after creation: " + error.getMessage());
            });
        }, (error) -> {
            Log.e(myTag, "Error during upload: " + error.getMessage());
        });
        if (getIntent().getStringExtra("parent_activity") == null) {
            adapter.notifyDataSetChanged();
        }
        else if (getIntent().getStringExtra("parent_activity").equals(CHANGE_CUSTOMER_WARNING_CLASS_NAME)) {
            factory.postLiveData.getValue().invalidate();
        }
        Log.d("myDebuggingTag", "Successfully created the new customer locally.");
        mToast = Toast.makeText(this, "Successfully created the new customer locally.", Toast.LENGTH_LONG);
        mToast.show();
        onBackPressed();
    }
}
