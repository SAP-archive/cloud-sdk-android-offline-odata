package com.example.android.offline;

import android.net.Uri;

import com.sap.cloud.mobile.foundation.authentication.OAuth2Token;
import com.sap.cloud.mobile.foundation.authentication.OAuth2TokenStore;
import com.sap.cloud.mobile.foundation.securestore.SecureKeyValueStore;

import java.util.ArrayList;

/**
 * Singleton class which is used for store of OAuth tokens. The tokens are persisted simple
 * in the application's secure store.
 */
public class SAPOAuthTokenStore implements OAuth2TokenStore {

    private static class SingletonHolder {
        private static final SAPOAuthTokenStore INSTANCE = new SAPOAuthTokenStore();
    }

    public static SAPOAuthTokenStore getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public void storeToken(OAuth2Token oAuth2Token, String url) {
        SecureKeyValueStore store = MyApplication.getApplication().getStore();
        if (store != null && store.isOpen()) {
            store.put(this.key(url), oAuth2Token);
        }
    }

    @Override
    public OAuth2Token getToken(String url) {
        SecureKeyValueStore store = MyApplication.getApplication().getStore();
        OAuth2Token token = null;
        if (store != null && store.isOpen()) {
            token = (OAuth2Token) store.getSerializable(key(url));
        }
        return  token;
    }

    @Override
    public void deleteToken(String url) {
        SecureKeyValueStore store = MyApplication.getApplication().getStore();
        if (store != null && store.isOpen()) {
            store.remove(key(url));
        }
    }

    private String key(String url) {
        return Uri.parse(url).getHost();
    }
}
