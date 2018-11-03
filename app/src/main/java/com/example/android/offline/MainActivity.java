package com.example.android.offline;

import android.arch.lifecycle.Observer;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PagedList;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sap.cloud.android.odata.espmcontainer.ESPMContainer;
import com.sap.cloud.android.odata.espmcontainer.ESPMContainerMetadata;
import com.sap.cloud.android.odata.espmcontainer.SalesOrderItem;
import com.sap.cloud.android.odata.espmcontainer.Supplier;
import com.sap.cloud.mobile.foundation.authentication.OAuth2Configuration;
import com.sap.cloud.mobile.foundation.authentication.OAuth2Interceptor;
import com.sap.cloud.mobile.foundation.authentication.OAuth2WebViewProcessor;
import com.sap.cloud.mobile.foundation.common.ClientProvider;
import com.sap.cloud.mobile.foundation.common.SettingsParameters;
import com.sap.cloud.mobile.foundation.logging.Logging;
import com.sap.cloud.mobile.foundation.networking.AppHeadersInterceptor;
import com.sap.cloud.mobile.foundation.networking.WebkitCookieJar;
import com.sap.cloud.mobile.foundation.user.UserInfo;
import com.sap.cloud.mobile.foundation.user.UserRoles;
import com.sap.cloud.mobile.odata.DataQuery;
import com.sap.cloud.mobile.odata.EntitySet;
import com.sap.cloud.mobile.odata.EntityType;
import com.sap.cloud.mobile.odata.EntityValue;
import com.sap.cloud.mobile.odata.EntityValueList;
import com.sap.cloud.mobile.odata.core.AndroidSystem;
import com.sap.cloud.mobile.odata.offline.OfflineODataDefiningQuery;
import com.sap.cloud.mobile.odata.offline.OfflineODataException;
import com.sap.cloud.mobile.odata.offline.OfflineODataParameters;
import com.sap.cloud.mobile.odata.offline.OfflineODataProvider;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.example.android.offline.ChangeCustomerDetailActivity.customer;
import static com.example.android.offline.StorageManager.adapter;

public class MainActivity extends AppCompatActivity implements CustomerRecyclerViewAdapter.ItemClickListener {
    private final static String ZERO_QUANTITY_ERROR_MESSAGE = "The value of quantity should be greater than zero";

    private boolean firstOpen;
    private final static String AUTH_END_POINT = "https://oauthasservices-i821234trial.hanatrial.ondemand.com/oauth2/api/v1/authorize";
    private final static String TOKEN_END_POINT = "https://oauthasservices-i821234trial.hanatrial.ondemand.com/oauth2/api/v1/token";
    private final static String OAUTH_CLIENT_ID = "cbd336b4-0abc-4232-ae97-8cf6d7000bb1";
    private final static String OAUTH_REDIRECT_URL = "https://oauthasservices-i821234trial.hanatrial.ondemand.com";
    private String deviceID;
    private MenuItem registerMenuItem;
    private MenuItem unRegisterMenuItem;
    private TextView loginTextView;
    private LinearLayout loadingSpinnerParent;
    private RecyclerView recyclerView;
    private SettingsParameters settingsParameters;

    public final static String myTag = "myDebuggingTag";
    public final static String appID = "com.sap.offline";
    public final static String serviceURL = "https://hcpms-i821234trial.hanatrial.ondemand.com";
    public final static String connectionID = "com.sap.edm.sampleservice.v2";
    public static String currentUser;
    public static CustomerDataSourceFactory factory;
    public StorageManager storageManager = StorageManager.getInstance();

    protected static Toast mToast = null;

    // Don't attempt to unbind from the service unless the client has received some
    // information about the service's state.
    private boolean shouldUnbind;
    // To invoke the bound service, first make sure that this value
    // is not null.
    private OfflineODataForegroundService boundService;

    private ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            boundService = ((OfflineODataForegroundService.LocalBinder)service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            boundService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        doBindService();
        // Setup the toolbar options
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Attempt to configure OAuth
        deviceID = android.provider.Settings.Secure.getString(this.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        Logging.ConfigurationBuilder cb = new Logging.ConfigurationBuilder();
        cb.logToConsole(true);
        Logging.initialize(this.getApplicationContext(), cb);
        OAuth2Configuration oAuth2Configuration = new OAuth2Configuration.Builder(getApplicationContext())
                .clientId(OAUTH_CLIENT_ID)
                .responseType("code")
                .authUrl(AUTH_END_POINT)
                .tokenUrl(TOKEN_END_POINT)
                .redirectUrl(OAUTH_REDIRECT_URL)
                .build();
        SAPOAuthTokenStore oauthTokenStore = SAPOAuthTokenStore.getInstance();
        try {
            settingsParameters = new SettingsParameters(serviceURL, appID, deviceID, "1.0");
        } catch (MalformedURLException e) {
            Log.d(myTag, "Error creating the settings parameters: " + e.getMessage());
        }
        OkHttpClient myOkHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new AppHeadersInterceptor(appID, deviceID, "1.0"))
                .addInterceptor(new OAuth2Interceptor(new OAuth2WebViewProcessor(oAuth2Configuration), oauthTokenStore))
                .cookieJar(new WebkitCookieJar())
                .build();
        ClientProvider.set(myOkHttpClient);

        ch.qos.logback.classic.Logger myRootLogger = Logging.getRootLogger();
        myRootLogger.setLevel(Level.ERROR);  //levels in order are all, trace, debug, info, warn, error, off
        loadingSpinnerParent = findViewById(R.id.loading_spinner_parent);
        loginTextView = findViewById(R.id.login_text);
        recyclerView = findViewById(R.id.rvCustomers);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), llm.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setLayoutManager(llm);
        // Passing in the finishLoading method so that the loading spinner can be dismissed by the adapter
        adapter = new CustomerRecyclerViewAdapter(getApplicationContext(), () -> finishLoading());
        adapter.setClickListener(this);

        // Loads up the recycler view
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(15);
        recyclerView.setDrawingCacheEnabled(true);
        firstOpen = savedInstanceState == null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    void doBindService() {
        // Attempts to establish a connection with the service.  We use an
        // explicit class name because we want a specific service
        // implementation that we know will be running in our own process
        // (and thus won't be supporting component replacement by other
        // applications).
        if (bindService(new Intent(MainActivity.this, OfflineODataForegroundService.class),
                connection, Context.BIND_AUTO_CREATE)) {
            shouldUnbind = true;
        }
        else {
            Log.e(myTag, "Error: The requested service doesn't " +
                    "exist, or this client isn't allowed access to it.");
        }
    }

    void doUnbindService() {
        if (shouldUnbind) {
            // Release information about the service's state.
            unbindService(connection);
            shouldUnbind = false;
        }
    }


    /**
     * toastAMessageFromBackground is used to produce a toast message
     * @param msg the string message to print
     */
    private void toastAMessageFromBackground(String msg) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
                    if (mToast != null) mToast.cancel();
                    mToast = Toast.makeText(getApplicationContext(),
                            msg,
                            Toast.LENGTH_SHORT);
                    mToast.show();
                }
        );
    }

    /**
     * addDefiningQueryAfterOpening is used as an example to show how to add a defining query after
     *      the offline store has already been opened with its preset list of defining queries.
     */
    private void addDefiningQueryAfterOpening() {
        try {
            OfflineODataDefiningQuery suppliersQuery = new OfflineODataDefiningQuery("Suppliers", "Suppliers", false);
            storageManager.getOfflineODataProvider().addDefiningQuery(suppliersQuery);
            storageManager.getOfflineODataProvider().download(() -> {
                List<Supplier> suppliers = storageManager.getESPMContainer().getSuppliers();
                Log.d(myTag, "Found these suppliers: ");
                for(Supplier supplier : suppliers) {
                    Log.d(myTag, supplier.getSupplierName());
                }
            }, (error) -> {
                Log.e(myTag, "Error occurred: " + error.getMessage());
            });
        } catch (OfflineODataException e) {
            e.printStackTrace();
            Log.e(myTag, "Exception encountered: " + e.getMessage());
        }
    }

    /**
     * setupOfflineStore simply sets up the offline store with a list of defining queries,
     *      which can be modified depending on the use case.
     */
    private void setupOfflineStore() {
        Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("com.sap.cloud.mobile.odata");
        logger.setLevel(Level.ALL);
        AndroidSystem.setContext(getApplicationContext());

        try {
            URL url = new URL(serviceURL + "/" + connectionID);
            OfflineODataParameters offParam = new OfflineODataParameters();
            offParam.setEnableRepeatableRequests(true);
            // by setting the page size here, we enable server side paging of the data
            offParam.setPageSize(10);
            storageManager.setOfflineODataProvider(new OfflineODataProvider(url, offParam, ClientProvider.get(), null, null));
            OfflineODataDefiningQuery productsQuery = new OfflineODataDefiningQuery("Products", "Products", false);
            OfflineODataDefiningQuery customersQuery = new OfflineODataDefiningQuery("Customers", "Customers", false);
            OfflineODataDefiningQuery salesOrderQuery = new OfflineODataDefiningQuery("SalesOrders", "SalesOrderHeaders", false);
            OfflineODataDefiningQuery salesOrderItemsQuery = new OfflineODataDefiningQuery("SalesOrderItems", "SalesOrderItems", false);
            storageManager.getOfflineODataProvider().addDefiningQuery(productsQuery);
            storageManager.getOfflineODataProvider().addDefiningQuery(customersQuery);
            storageManager.getOfflineODataProvider().addDefiningQuery(salesOrderQuery);
            storageManager.getOfflineODataProvider().addDefiningQuery(salesOrderItemsQuery);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(myTag, "Exception encountered setting up offline store: " + e.getMessage());
        }

        // Opens the offline store after defining all the "Defining Queries"
        boundService.openStore(storageManager.getOfflineODataProvider(), () -> {
            storageManager.setESPMContainer(new ESPMContainer(storageManager.getOfflineODataProvider()));
            Log.d(myTag, "Successfully opened offline store.");
            boundService.downloadStore(storageManager.getOfflineODataProvider(), () -> {
                setupPagedList();
                addUserToList();
            }, null);
        }, (error) -> {
            Log.d(myTag, "Failed to open offline store with error: " + error.toString());
        });
    }

    private void setupPagedList() {
        // placeholders are disabled to avoid the need to handle null placeholder values in the adapter
        PagedList.Config config = new PagedList.Config.Builder().setPageSize(10).setEnablePlaceholders(false).build();
        factory = new CustomerDataSourceFactory();
        storageManager.setCustomersListToDisplay(new LivePagedListBuilder<>(factory, config).build());
        storageManager.getCustomersListToDisplay().observe(this, new Observer<PagedList<CustomerListItem>>() {
            @Override
            public void onChanged(@Nullable PagedList<CustomerListItem> customerListItems) {
                // called when the paged list is updated due to data being loaded
                // this then notifies the adapter to allow the recycler view to updated
                adapter.submitList(customerListItems);
            }
        });
    }

    /**
     * onLogout attempts first to sync the user's changed data with the backend service, and then
     *      logs the user out.
     */
    public void onLogout() {
        Log.d(myTag, "In onLogout");
        storageManager.getOfflineODataProvider().upload(() -> {
            if (checkErrors()) {
                runOnUiThread(() -> adapter.notifyDataSetChanged());
                return;
            }
            Log.d(myTag, "Successfully uploaded customer data.");
            toastAMessageFromBackground("Successfully synced all changed data.");
            unRegisterLogic();
        }, error -> {
            Log.d(myTag, "Error while uploading personal store: " + error.getMessage());
            toastAMessageFromBackground("Sync failed, please check your network connection and the current backend data.");
        });
    }

    public void onRegister() {
        Log.d(myTag, "In onRegister");
        firstOpen = false;
        registerMenuItem.setEnabled(false);
        loginTextView.setVisibility(View.GONE);
        loadingSpinnerParent.setVisibility(View.VISIBLE);

        setupOfflineStore();

    }

    private void addUserToList() {
        Log.d(myTag, "In addUserToList");
        UserRoles roles = new UserRoles(ClientProvider.get(), settingsParameters);
        UserRoles.CallbackListener callbackListener = new UserRoles.CallbackListener() {
            @Override
            public void onSuccess(@NonNull UserInfo ui) {
                Log.d(myTag, "Successfully registered");
                Log.d(myTag, "Logged in User Id: " + ui.getId());
                currentUser = ui.getId();
                getSupportActionBar().setTitle("Logged in as: " + currentUser);
                finishLoading();
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                toastAMessageFromBackground("UserRoles onFailure " + throwable.getMessage());
            }
        };
        roles.load(callbackListener);
    }

    /**
     * unRegisterLogic attempts to sign the user out of the application, and removes all cookies
     */
    private void unRegisterLogic() {
        CookieManager.getInstance().removeAllCookies(null);

        Request request = new Request.Builder()
                .post(RequestBody.create(null, ""))
                .url(serviceURL + "/mobileservices/sessions/logout")
                .build();

        Callback updateUICallback = new Callback() {
            @Override
            public void onFailure(@NonNull Call call, final IOException e) {
                Log.d(myTag, "Log out failed: " + e.getLocalizedMessage());
                toastAMessageFromBackground("Log out failed, please check your network connection.");
            }

            @Override
            public void onResponse(@NonNull Call call, final Response response) {
                if (response.isSuccessful()) {
                    Log.d(myTag, "Successfully logged out");
                    runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                        registerMenuItem.setEnabled(true);
                        unRegisterMenuItem.setEnabled(false);
                        loginTextView.setVisibility(View.VISIBLE);
                        currentUser = null;
                        getSupportActionBar().setTitle("Offline");
                    });
                }
                else {
                    Log.d(myTag, "Log out failed " + response.networkResponse());
                    toastAMessageFromBackground("Log out failed " + response.networkResponse());
                }
            }
        };
        ClientProvider.get().newCall(request).enqueue(updateUICallback);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        registerMenuItem = menu.findItem(R.id.action_register);
        unRegisterMenuItem = menu.findItem(R.id.action_unregister);
        unRegisterMenuItem.setEnabled(false);

        if (firstOpen) {
            onRegister();
        }
        else if (storageManager.getOfflineODataProvider() != null) {
            registerMenuItem.setEnabled(false);
            setupPagedList();
            finishLoading();
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Determine which option was selected and handle the action accordingly
        if (id == R.id.action_register) {
            onRegister();
        }
        else if (id == R.id.action_unregister) {
            onLogout();
        }
        else if (id == R.id.action_sync) {
            onSync();
        }
        else if (id == R.id.action_create) {
            Intent intent = new Intent(this, CreateCustomerActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.action_zero_items) {
            zeroSalesOrderItems();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * checkErrors checks the ErrorArchive from the offline odata storage manager to see if the
     *      previous request returned errors
     * @return true if errors were detected and not handled, false otherwise
     */
    private boolean checkErrors() {
        // These lines access the Error Archive
        EntitySet errorArchiveSet = storageManager.getESPMContainer().getEntitySet("ErrorArchive");
        EntityType errorArchiveType = errorArchiveSet.getEntityType();
        com.sap.cloud.mobile.odata.Property methodProp = errorArchiveType.getProperty("RequestMethod");
        com.sap.cloud.mobile.odata.Property messageProp = errorArchiveType.getProperty("Message");
        com.sap.cloud.mobile.odata.Property requestBodyProp = errorArchiveType.getProperty("RequestBody");
        com.sap.cloud.mobile.odata.Property affectedEntityProp = errorArchiveType.getProperty("AffectedEntity");
        com.sap.cloud.mobile.odata.Property httpStatusCodeProp = errorArchiveType.getProperty("HTTPStatusCode");

        // We then query the error archive
        DataQuery errorArchiveQuery = new DataQuery().from(errorArchiveSet);

        // Get the list of errors in the ErrorArchive
        EntityValueList errors = storageManager.getESPMContainer().executeQuery(errorArchiveQuery).getEntityList();

        // If there is at least one error: We get the first error in list for example, and handle it
        if (errors.length() > 0) {
            // In this if statement we handle the error
            EntityValue error1 = errors.get(0);
            String body = requestBodyProp.getNullableString(error1);
            String method = methodProp.getNullableString(error1);
            String message = messageProp.getNullableString(error1);
            int statusCode = httpStatusCodeProp.getNullableInt(error1) != null ? httpStatusCodeProp.getNullableInt(error1) : 0;

            // Log the error details to the console for debugging purposes
            Log.e(myTag, "Message: " + message);
            Log.e(myTag, "Body: " + body);
            Log.e(myTag, "HTTP Status Code: " + statusCode);
            Log.e(myTag, "Method: " + method);

            // Based on the statusCode, we take different actions
            if (statusCode == 404) {
                // A status code of 404 means we need to remove the customer from the local list
                for (CustomerListItem cli : storageManager.getCustomersListToDisplay().getValue()) {
                    if (cli.customer.equals(customer)) {
                        factory.postLiveData.getValue().invalidate();
                        break;
                    }
                }
                // getInfo() gives the user the option to create a new customer according to the latest changes,
                // since the customer intended to be updated has been deleted in the back-end.
                Log.d(myTag, customer.toString());
                getInfo(customer.toString());
                storageManager.getESPMContainer().deleteEntity(errors.get(0));
                return true;
            }
            else if (statusCode == 400) {
                if (message.contains(ZERO_QUANTITY_ERROR_MESSAGE)) {
                    // A status code of 400 is a general error
                    storageManager.getESPMContainer().loadProperty(affectedEntityProp, error1, null);
                    SalesOrderItem errorSalesOrderItem = (SalesOrderItem) affectedEntityProp.getEntity(error1);
                    runOnUiThread(() -> changeQuantity(errorSalesOrderItem));
                    return false;
                }
            }
        }
        // We then check to see if the errors have been resolved
        Log.d(myTag, "Number of errors: " + errors.length());
        if (errors.length() > 0) {
            storageManager.getESPMContainer().deleteEntity(errors.get(0));
        }
        return false;
    }

    /**
     * changeQuantity allows a user who has entered 0 for a sales order quantity to amend their submission.
     * AlertDialog logic sourced from: https://stackoverflow.com/questions/10903754/input-text-dialog-android
     * @param salesOrderItem the sales order item that needs to be modified before pushed to the backend
     */
    private void changeQuantity(SalesOrderItem salesOrderItem) {
        // This dialog could also be provided by the Fiori Onboarding Library
        // We create the dialog using an AlertDialog.Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar_MinWidth);
        builder.setMessage("The quantity must be greater than 0. Update the quantity below.");
        builder.setTitle("Change Quantity");

        // Set up the input
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.quantity_edit_text_layout, findViewById(android.R.id.content), false);
        final EditText input = (EditText) viewInflated.findViewById(R.id.input);
        input.setText(String.format("%.0f", salesOrderItem.getQuantity()));
        builder.setView(viewInflated);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String inputQuantity = input.getText().toString();
                BigDecimal newQuantity = new BigDecimal(inputQuantity);
                salesOrderItem.setQuantity(newQuantity);

                // Attempt to update the entity and possibly call checkErrors()
                storageManager.getESPMContainer().updateEntity(salesOrderItem);
                onSync();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                storageManager.getESPMContainer().deleteEntity(salesOrderItem);
            }
        });

        // Finally, show the dialog
        builder.show();
    }

    /**
     * getInfo gets the info of a customer who the user is trying to edit, but has been deleted in
     *      the backend. The method allows the app user to take the customer details they were editing
     *      and create a new customer without filling in all the form data again.
     * @param myMessage the JSON string that will be parsed into a customer
     */
    private void getInfo(String myMessage) {
        try {
            JSONObject customerInfo = new JSONObject(myMessage);
            String city = customerInfo.getString("City");
            String country = customerInfo.getString("Country");
            String email = customerInfo.getString("EmailAddress");
            String firstName = customerInfo.getString("FirstName");
            String lastName = customerInfo.getString("LastName");
            String houseNumber = customerInfo.getString("HouseNumber");
            String phoneNumber = customerInfo.getString("PhoneNumber");
            String postalNumber = customerInfo.getString("PostalCode");
            String street = customerInfo.getString("Street");
            String dob = "2000-01-01T00:00:00";
            Log.e(myTag, "city: " + city);
            Log.e(myTag, "country: " + country);
            Log.e(myTag, "email: " + email);
            Log.e(myTag, "firstName： " + firstName);
            Log.e(myTag, "lastName: " + lastName);
            Log.e(myTag, "houseNumber: " + houseNumber);
            Log.e(myTag, "phoneNumber: " + phoneNumber);
            Log.e(myTag, "postalNumber: " + postalNumber);
            Log.e(myTag, "street: " + street);
            Log.e(myTag, "dob: " + dob);
            Intent i = new Intent(this, ChangeCustomerWarningActivity.class);
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
            // Pass information to customer create activity
            this.startActivity(i);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(myTag, "JSON error encountered: " + e.getMessage());
        }
    }

    /**
     * onSync method performs the logic of syncing the app user's local changes and the backend,
     *      first performs and upload of the local data, and then a download to make sure both systems
     *      are in sync.
     */
    private void onSync() {
        // Going forward we might want to attach a delegate to the OfflineODataForegroundService so we
        // can get feedback on our download progress. As of right now the backend OData service doesn't
        // send information about % downloads.


        // Tells the user the upload has started
        toastAMessageFromBackground("Starting upload");
        // Attempts to upload the local offline store
        boundService.uploadStore(storageManager.getOfflineODataProvider(), () -> {
            Log.d(myTag, "Successfully uploaded local customer data to the backend.");
            toastAMessageFromBackground("Upload completed, starting download");
            // If the upload succeeds then we attempt to download
            // This ensures that we obtain changes made by other users
            boundService.downloadStore(storageManager.getOfflineODataProvider(), () -> {
                toastAMessageFromBackground("Download completed, refreshing");
                Log.d(myTag, "Local customer data has been updated from the backend.");
                // We then check errors with the download/upload
                checkErrors();
                // Refresh the table to show us the newly downloaded data
                factory.postLiveData.getValue().invalidate();
            }, (error) -> {
                toastAMessageFromBackground("Download failed, check network connection");
                Log.d(myTag, "Error downloading customer data from the backend: " + error.getMessage());
                factory.postLiveData.getValue().invalidate();
            });
        }, (error) -> {
            toastAMessageFromBackground("Upload failed, check network connection");
            Log.d(myTag, "Error uploading the local customer data to the backend: " + error.getMessage());
        });
    }

    private void finishLoading() {
        unRegisterMenuItem.setEnabled(true);
        recyclerView.setVisibility(View.VISIBLE);
        loadingSpinnerParent.setVisibility(View.GONE);
        loginTextView.setVisibility(View.GONE);
    }

    /**
     * zeroSalesOrderItems is a hack that allows a user to create a sales order item with 0 quantity.
     *      Used only for showcasing the changeQuantity method's functionality in-app.
     */
    private void zeroSalesOrderItems() {
        // Setup the data query to get the sales order items
        DataQuery productQuery = new DataQuery().from(ESPMContainerMetadata.EntitySets.salesOrderItems).top(1);
        SalesOrderItem salesOrderItem = storageManager.getESPMContainer().getSalesOrderItem(productQuery);

        // Update the quantity of the sales order to 0 -- this will create a BACKEND DATA VIOLATION
        salesOrderItem.setQuantity(new BigDecimal(0));

        // Attempts to update the entity
        storageManager.getESPMContainer().updateEntity(salesOrderItem);
        storageManager.getOfflineODataProvider().upload(() -> checkErrors(), (error) ->
            Log.e(myTag, "Error occurred while uploading SalesOrderItem with zero quantity: " + error.getMessage()));
    }

    @Override
    public void onItemClick(View view, int position) {
        customer = storageManager.getCustomersListToDisplay().getValue().get(position).customer;
        Intent i = new Intent(this, ChangeCustomerDetailActivity.class);
        startActivity(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        recyclerView.getAdapter().notifyDataSetChanged();
    }
}
