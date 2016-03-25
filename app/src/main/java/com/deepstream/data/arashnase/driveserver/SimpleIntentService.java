package com.deepstream.data.arashnase.driveserver;


import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import java.io.IOException;


/**
 * Created by arash.nase on 3/2/2016.
 * An intent service that uploads a file to Google Drive, given for demonstration
 */
public class SimpleIntentService extends IntentService {

    public SimpleIntentService() {
        super("SimpleIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v("SimpleIntentService", "Intent service started");

        GoogleDrive googleDrive = new GoogleDrive(getApplicationContext());

        Log.v("SimpleIntentService", "Account: " + googleDrive.getCredentials().getSelectedAccountName());


        googleDrive.logOn();


        //Log.v("SimpleIntentService", "Trying to upload from the intentservice.........");
        // A file on my tablet
        java.io.File dcimFileDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        java.io.File cameraFileDirectory = new java.io.File(dcimFileDirectory, "Camera");
        java.io.File image = new java.io.File(cameraFileDirectory, "IMG_20160324_182754.jpg"); //mine
        //java.io.File image = new java.io.File(cameraFileDirectory, "IMG_20160314_181159.jpg");  //shawn's

        // Uploads the file to Google Drive
        googleDrive.uploadFile(image.getAbsolutePath());

        Log.v("SimpleIntentService", image.getAbsolutePath());

        System.out.println();
        Log.v("SimpleIntentService", "Check if file exists: ");
        System.out.println(googleDrive.fileExists(image.getAbsolutePath()));

    }

}
