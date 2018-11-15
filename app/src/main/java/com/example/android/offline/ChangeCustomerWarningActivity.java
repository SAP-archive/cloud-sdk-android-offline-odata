package com.example.android.offline;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

public class ChangeCustomerWarningActivity extends AppCompatActivity {

    public static final String CHANGE_CUSTOMER_WARNING_CLASS_NAME = "CHANGE_CUSTOMER_WARNING_ACTIVITY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_customer_warning);

        TextView warningContent = findViewById(R.id.warning_content);
        Bundle extras = getIntent().getExtras();
        String customerName = extras.getString("firstName") + " " + extras.getString("lastName");
        warningContent.setText("The customer: " + customerName + " was deleted by another user since your last sync, do you want to re-create the customer?");
    }

    public void onDiscard(View view){
        finish();
    }

    public void onContinue(View view){
        Bundle extras = getIntent().getExtras();
        if (extras != null){
            String firstName = extras.getString("firstName");
            String lastName = extras.getString("lastName");
            String email = extras.getString("email");
            String street = extras.getString("street");
            String postalNumber = extras.getString("postalNumber");
            String phoneNumber = extras.getString("phoneNumber");
            String city = extras.getString("city");
            String dob = extras.getString("dob");
            String houseNumber = extras.getString("houseNumber");
            String country = extras.getString("country");
            Intent i = new Intent(this, CreateCustomerActivity.class);
            i.putExtra("parent_activity", CHANGE_CUSTOMER_WARNING_CLASS_NAME);
            i.putExtra("city", city);
            i.putExtra("country", country);
            i.putExtra("email", email);
            i.putExtra("firstName", firstName);
            i.putExtra("lastName", lastName);
            i.putExtra("phoneNumber", phoneNumber);
            i.putExtra("postalNumber", postalNumber);
            i.putExtra("street", street);
            i.putExtra("dob", dob);
            i.putExtra("houseNumber", houseNumber);
            this.startActivity(i);
        }
        finish();
    }
}
