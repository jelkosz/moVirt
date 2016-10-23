package org.ovirt.mobile.movirt.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.SystemService;
import org.ovirt.mobile.movirt.rest.client.LoginClient;
import org.ovirt.mobile.movirt.ui.AuthenticatorActivity;
import org.ovirt.mobile.movirt.ui.AuthenticatorActivity_;
import org.ovirt.mobile.movirt.ui.CertHandlingStrategy;
import org.ovirt.mobile.movirt.util.JsonUtils;
import org.ovirt.mobile.movirt.util.Version;
import org.ovirt.mobile.movirt.util.message.MessageHelper;


@EBean
public class MovirtAuthenticator extends AbstractAccountAuthenticator {

    public static final String ACCOUNT_NAME = "oVirt";

    public static final String ACCOUNT_TYPE = "org.ovirt.mobile.movirt.authenticator";

    public static final String AUTH_TOKEN_TYPE = "org.ovirt.mobile.movirt.token";

    public static final String USER_NAME = "org.ovirt.mobile.movirt.username";

    public static final String API_URL = "org.ovirt.mobile.movirt.apiurl";

    public static final String API_VERSION = "org.ovirt.mobile.movirt.apiversion";

    public static final String CERT_HANDLING_STRATEGY = "org.ovirt.mobile.movirt.certhandlingstrategy";

    public static final String HAS_ADMIN_PERMISSIONS = "org.ovirt.mobile.movirt.adminpermissionsm";

    public static final String CUSTOM_CERTIFICATE = "org.ovirt.mobile.movirt.customCertificate";

    public static final Account MOVIRT_ACCOUNT = new Account(MovirtAuthenticator.ACCOUNT_NAME, MovirtAuthenticator.ACCOUNT_TYPE);

    @Bean
    LoginClient loginClient;

    @SystemService
    AccountManager accountManager;

    @Bean
    MessageHelper messageHelper;

    private Context context;

    public MovirtAuthenticator(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse accountAuthenticatorResponse, String s) {
        return null;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse accountAuthenticatorResponse, String s, String s2, String[] strings, Bundle options) throws NetworkErrorException {
        if (accountConfigured()) {
            messageHelper.showToast("Only one moVirt account is allowed per device");
            return null;
        } else {
            return createAccountActivity(accountAuthenticatorResponse);
        }
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, Bundle bundle) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String authTokenType, Bundle bundle) throws NetworkErrorException {
        String authToken = accountManager.peekAuthToken(account, authTokenType);

        if (TextUtils.isEmpty(authToken)) {
            final String username = getUserName();
            final String password = getPassword();
            if (username != null && password != null) {
                try {
                    if (!AuthenticatorActivity.isInUserLogin()) { // do not attempt to login while user tries
                        authToken = loginClient.login(username, password);
                    }
                } catch (Exception x) { // do not fail on bad login info
                }

                if (!TextUtils.isEmpty(authToken)) {
                    accountManager.setAuthToken(account, authTokenType, authToken);
                }
            }
        }

        if (!TextUtils.isEmpty(authToken)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return result;
        }
        return createAccountActivity(accountAuthenticatorResponse);
    }

    private Bundle createAccountActivity(AccountAuthenticatorResponse accountAuthenticatorResponse) {
        final Intent intent = new Intent(context, AuthenticatorActivity_.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, accountAuthenticatorResponse);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String s) {
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String s, Bundle bundle) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse accountAuthenticatorResponse, Account account, String[] strings) throws NetworkErrorException {
        return null;
    }

    public CertHandlingStrategy getCertHandlingStrategy() {
        String strategy = read(CERT_HANDLING_STRATEGY);
        if (TextUtils.isEmpty(strategy)) {
            return CertHandlingStrategy.TRUST_SYSTEM;
        }

        try {
            return CertHandlingStrategy.from(Long.valueOf(strategy));
        } catch (NumberFormatException e) {
            return CertHandlingStrategy.TRUST_SYSTEM;
        }
    }

    public String getApiUrl() {
        return read(API_URL);
    }

    public String getBaseUrl() {
        return getApiUrl().replaceFirst("/api$", "");
    }


    /**
     * Intended to be used only by VersionManager
     *
     * @return api version
     */
    public Version getApiVersion() {
        Version result = null;
        try {
            result = JsonUtils.stringToObject(read(API_VERSION), Version.class);
        } catch (Exception ignored) {
        }

        if (result == null) {
            result = new Version();
        }
        return result;
    }

    /**
     * Intended to be used only by VersionManager
     *
     * @param version api version
     */
    public void setApiVersion(Version version) {
        accountManager.setUserData(MOVIRT_ACCOUNT, API_VERSION, JsonUtils.objectToString(version));
    }

    public String getUserName() {
        return read(USER_NAME);
    }

    public String getPassword() {
        return accountManager.getPassword(MOVIRT_ACCOUNT);
    }

    public Boolean hasAdminPermissions() {
        return read(HAS_ADMIN_PERMISSIONS, false);
    }

    public boolean accountConfigured() {
        return accountManager.getAccountsByType(ACCOUNT_TYPE).length > 0;
    }

    private Boolean read(String id, boolean defRes) {
        String res = accountManager.getUserData(MOVIRT_ACCOUNT, id);
        if (TextUtils.isEmpty(res)) {
            return defRes;
        }

        return Boolean.valueOf(res);
    }

    private String read(String id, String defRes) {
        String res = accountManager.getUserData(MOVIRT_ACCOUNT, id);
        if (TextUtils.isEmpty(res)) {
            return defRes;
        }

        return res;
    }

    private String read(String id) {
        return accountManager.getUserData(MOVIRT_ACCOUNT, id);
    }

}
