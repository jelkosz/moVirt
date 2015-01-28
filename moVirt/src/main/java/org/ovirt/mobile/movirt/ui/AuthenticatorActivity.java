package org.ovirt.mobile.movirt.ui;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.ovirt.mobile.movirt.R;
import org.ovirt.mobile.movirt.auth.MovirtAuthenticator;
import org.ovirt.mobile.movirt.rest.OVirtClient;
import org.w3c.dom.Text;

@EActivity(R.layout.authenticator_activity)
public class AuthenticatorActivity extends AccountAuthenticatorActivity {

    @SystemService
    AccountManager accountManager;

    @Bean
    OVirtClient client;

    @ViewById
    EditText txtEndpoint;

    @ViewById
    EditText txtUsername;

    @ViewById
    EditText txtPassword;

    @ViewById
    CheckBox chkAdminPriv;

    @ViewById
    CheckBox chkDisableHttps;

    @ViewById
    ProgressBar authProgress;

    @Bean
    MovirtAuthenticator authenticator;

    @AfterViews
    void init() {
        txtEndpoint.setText(authenticator.getApiUrl());
        txtUsername.setText(authenticator.getUserName());
        txtPassword.setText(authenticator.getPassword());

        chkAdminPriv.setChecked(authenticator.hasAdminPermissions());
        chkDisableHttps.setChecked(authenticator.disableHttps());

    }

    @Click(R.id.btnCreate)
    void addNew() {
        String endpoint = txtEndpoint.getText().toString();
        String username = txtUsername.getText().toString();
        String password = txtPassword.getText().toString();

        Boolean adminPriv = chkAdminPriv.isChecked();
        Boolean disableHttps = chkDisableHttps.isChecked();

        finishLogin(endpoint, username, password, adminPriv, disableHttps);
    }

    @Background
    void finishLogin(String apiUrl, String name, String password, Boolean hasAdminPermissions, Boolean disableHttps) {
        accountManager.addAccountExplicitly(MovirtAuthenticator.MOVIRT_ACCOUNT, password, null);

        setUserData(MovirtAuthenticator.MOVIRT_ACCOUNT, apiUrl, name, password, hasAdminPermissions, disableHttps);

        changeProgressVisibilityTo(View.VISIBLE);
        String token = "";
        boolean success = true;
        try {
            token = client.login(apiUrl, name, password, disableHttps, hasAdminPermissions);
        } catch (Exception e) {
            showToast("Error logging in: " + e.getMessage());
            success = false;
            return;
        } finally {
            changeProgressVisibilityTo(View.GONE);
            if (success) {
                if (TextUtils.isEmpty(token)) {
                    showToast("Error: the returned token is empty");
                    return;
                } else {
                    showToast("Login successful");
                }
            } else {
                return;
            }
        }

        accountManager.setAuthToken(MovirtAuthenticator.MOVIRT_ACCOUNT, MovirtAuthenticator.AUTH_TOKEN_TYPE, token);

        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, MovirtAuthenticator.ACCOUNT_NAME);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, MovirtAuthenticator.ACCOUNT_TYPE);
        intent.putExtra(AccountManager.KEY_AUTHTOKEN, token);

        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }

    @UiThread
    void changeProgressVisibilityTo(int visibility) {
        authProgress.setVisibility(visibility);
    }

    @UiThread
    void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void setUserData(Account account, String apiUrl, String name, String password, Boolean hasAdminPermissions, Boolean disableHttps) {
        accountManager.setUserData(account, MovirtAuthenticator.API_URL, apiUrl);
        accountManager.setUserData(account, MovirtAuthenticator.USER_NAME, name);
        accountManager.setUserData(account, MovirtAuthenticator.HAS_ADMIN_PERMISSIONS, Boolean.toString(hasAdminPermissions));
        accountManager.setUserData(account, MovirtAuthenticator.DISABLE_HTTPS, Boolean.toString(disableHttps));
        accountManager.setPassword(account, password);
    }
}
