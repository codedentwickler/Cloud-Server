package com.deepstream.data.arashnase.driveserver;

/**
 * Created by arash.nase on 3/23/2016.
 */

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.client.util.ExponentialBackOff;

import com.google.api.services.drive.DriveScopes;

import com.google.api.services.drive.model.*;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity {

    public static final String DEBUG_TAG = "MainActivity";

    private Button mChooseAccount;
    private Button mSignInButton;
    private Button mStartServiceButton;
    private TextView mOutputText;

    /*************************************************************************************/
    /*
    The following variables should be declared in the activity that asks the user to choose
    his google account
     */
    // For startActvityForResult
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;

    // Manages authorization and account selection for Google accounts.
    public GoogleAccountCredential mCredential;

    // The features you need to use in Google Drive (access to metadata, changing drive files...)
    private static final String[] SCOPES = {DriveScopes.DRIVE_METADATA, DriveScopes.DRIVE, DriveScopes.DRIVE_FILE};

    /**************************************************************************************/



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**************************************************************************************/
        /*
        Include the following lines of credential and authorization process in onCreate()
        */

        // Initialize credentials and service object.
        SharedPreferences settings =
                getSharedPreferences(GoogleDrive.DATA_STORE_FACTORY, Context.MODE_PRIVATE);

        mCredential = GoogleAccountCredential
                .usingOAuth2(getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(settings.getString(GoogleDrive.ACCOUNT_NAME, null));

        /**************************************************************************************/

        mChooseAccount = (Button) findViewById(R.id.choose_account_button);
        mSignInButton = (Button)findViewById(R.id.authorize_button);
        mStartServiceButton = (Button)findViewById(R.id.start_service_button);
        mOutputText = (TextView)findViewById(R.id.textView);

        mChooseAccount.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // if the account is not specified yet, start the AccountPicker dialogbox
                if (mCredential.getSelectedAccountName() == null) {
                    chooseAccount();
                }
            }
        });


        // Authorize the app to use the account in the background
        mSignInButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mCredential.getSelectedAccountName() != null) {
                    new AuthorizationRequest(mCredential).execute();
                }
            }
        });


        // starts the intent service
        mStartServiceButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent startServiceIntent = new Intent(getApplicationContext(), SimpleIntentService.class);
                startService(startServiceIntent);
            }
        });
    }



    @Override
    protected void onResume() {
        super.onResume();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /*******************************************************************************************
     * Include the following methods in the activity that start AccountPicker
     */

    /**
     * This method should be added to activity that starts the AccountPicker dialogbox
     *
     * Called when an AccountPicker and authorization) exits, giving you the requestCode
     * you started it with, the resultCode it returned, and any additional data from it.
     *
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming activity result.
     * @param data Intent (containing result data) returned by incoming activity result.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {

            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {  // you could put RESULT_CANCELED instead of RESULT_OK, but we want to capture
                    // all the situations where operation did not succeed, not just when we are sure
                    // operation failed.
                    Log.v(DEBUG_TAG, "Google Play Service is not running. Calling isGooglePlayServicesAvailable()");
                    isGooglePlayServicesAvailable();
                }
                break;

            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    Log.v(DEBUG_TAG, "Account specified.");
                    // Receive the account name the user chose on AccountPicker
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

                    if (accountName != null) {
                        // Set the account name on credentials object
                        mCredential.setSelectedAccountName(accountName);

                        // Save the account name in sharedPreferences to be accessed by all instances of GoogleDrive.java
                        SharedPreferences settings =
                                getSharedPreferences(GoogleDrive.DATA_STORE_FACTORY, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(GoogleDrive.ACCOUNT_NAME, accountName);
                        editor.apply();
                    }
                } else if (resultCode != RESULT_OK) {
                    Log.v(DEBUG_TAG, "Account unspecified, calling chooseAccount().");
                    chooseAccount();
                }
                break;

            case REQUEST_AUTHORIZATION:
                if ((resultCode == RESULT_OK)) {
                    Log.v(DEBUG_TAG, "Account authorized the app. (sending API requests is possible)");
                }
                else {
                    Log.v(DEBUG_TAG, "Account did not authorize the app, choosing another account.");
                    chooseAccount();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    /**
     * Starts an activity in android (AccountPicker so the user can pick an account.
     */
    private void chooseAccount() {
        startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }



    /**
     * Check that Google Play services APK is installed and up to date. Will
     * launch an error dialog for the user to update Google Play Services if
     * possible.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        final int connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        } else if (connectionStatusCode != ConnectionResult.SUCCESS ) {
            return false;
        }
        return true;
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                connectionStatusCode,
                MainActivity.this,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }



    /**
     * An asynchronous task that handles the Drive API call. This is needed to make a quick
     * API call in order to trigger OAuth authorization flow...
     * TODO: will come up with a better way to trigger OAuth
     */
    private class AuthorizationRequest extends AsyncTask<Void, Void, List<String>> {

        private com.google.api.services.drive.Drive mService = null;
        private Exception mLastError = null;
        private GoogleDrive googleDrive;

        public AuthorizationRequest(GoogleAccountCredential credential) {
            Log.v(DEBUG_TAG, "AuthorizationRequest: Making a quick API call to trigger the OAuth flow");
            googleDrive = new GoogleDrive(getApplicationContext());
        }

        /**
         * Background task to call Drive API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected ArrayList<String> doInBackground(Void... params) {

            try {
                googleDrive.logOn();
                ArrayList<File> driveFiles = googleDrive.getDriveFiles();
                ArrayList<String> fileInfo = new ArrayList<String>();
                if (driveFiles != null) {
                    for (File file : driveFiles) {
                        fileInfo.add(String.format("%s (%s)\n",
                                file.getTitle(), file.getId()));
                    }
                }
                return fileInfo;

            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<String> output) {
            if (output == null || output.size() == 0) {
                mOutputText.setText("No results returned.");
            } else {
                output.add(0, "Files on Google Drive: ");
                mOutputText.setText(TextUtils.join("\n", output));
            }
        }

        @Override
        protected void onCancelled() {
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(((GooglePlayServicesAvailabilityIOException) mLastError).getConnectionStatusCode());
                }
                else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(((UserRecoverableAuthIOException) mLastError).getIntent(), MainActivity.REQUEST_AUTHORIZATION);
                }
                else {
                    Log.v(DEBUG_TAG, "The following error occurred:"  + mLastError.getMessage());
                }
            }
            else {
                Log.v(DEBUG_TAG, "Request cancelled.");
            }
        }
    }



}