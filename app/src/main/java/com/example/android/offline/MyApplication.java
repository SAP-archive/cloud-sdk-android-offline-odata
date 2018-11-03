package com.example.android.offline;

import android.app.Application;

import com.sap.cloud.mobile.foundation.authentication.AppLifecycleCallbackHandler;
import com.sap.cloud.mobile.foundation.securestore.SecureKeyValueStore;

public class MyApplication extends Application {

    private static MyApplication app;
    private SecureKeyValueStore appStore;

    @Override
    public void onCreate() {
        super.onCreate();

        MyApplication.app = this;

        registerActivityLifecycleCallbacks(AppLifecycleCallbackHandler.getInstance());
    }

    public static MyApplication getApplication() {
        return app;
    }

    public void setAppStore(SecureKeyValueStore keyValueStore) {
        appStore = keyValueStore;
    }

    public SecureKeyValueStore getStore() {
        return appStore;
    }
}
