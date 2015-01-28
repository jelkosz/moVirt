package org.ovirt.mobile.movirt.rest;

import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.res.StringRes;
import org.androidannotations.api.rest.RestErrorHandler;
import org.ovirt.mobile.movirt.MoVirtApp;
import org.ovirt.mobile.movirt.R;
import org.ovirt.mobile.movirt.auth.MovirtAuthenticator;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

@EBean
public class ErrorHandler implements RestErrorHandler {

    private static final String TAG = ErrorHandler.class.getSimpleName();

    @StringRes(R.string.rest_request_failed)
    String errorMsg;

    @RootContext
    Context context;

    private UnauthorizedCallback unauthorizedCallback;

    @Override
    public void onRestClientExceptionThrown(NestedRuntimeException e) {
        Log.e(TAG, "Error during calling REST: '" + e.getMessage() + "'");

        final String msg = String.format(errorMsg, e.getMessage());

        HttpStatus statusCode = ((HttpClientErrorException) e).getStatusCode();
        if (statusCode == HttpStatus.UNAUTHORIZED) {
            if (unauthorizedCallback != null) {
                unauthorizedCallback.onUnauthorized();
            } else {
                fireConnectionError(msg);
            }
        } else {
            fireConnectionError(msg);
        }
    }

    private void fireConnectionError(String msg) {
        Intent intent = new Intent(MoVirtApp.CONNECTION_FAILURE);
        intent.putExtra(MoVirtApp.CONNECTION_FAILURE_REASON, msg);
        context.sendBroadcast(intent);
    }

    public void setUnauthorizedCallback(UnauthorizedCallback unauthorizedCallback) {
        this.unauthorizedCallback = unauthorizedCallback;
    }

    public static interface UnauthorizedCallback {
        void onUnauthorized();
    }
}
