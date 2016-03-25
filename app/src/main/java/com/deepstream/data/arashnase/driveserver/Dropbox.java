package com.deepstream.data.arashnase.driveserver;

import android.content.Context;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;


/**
 * Created by arash.nase on 2/3/2016.
 * Stand-alone Dropbox class for easier access to Dropbox functionalities
 * Dropbox app should be installed on the device.
 *
 * More instructions:
 * You should have dropbox SDK and library included in your app
 * Download the SDK from here:
 * https://www.dropbox.com/developers-v1/core/sdks/android
 * Then extract the file and go to \dropbox-android-sdk-1.6.3\lib
 * Copy the two .jar files
 * Go to libs folder of the project, then paste the jar files there:
 * (eg. \AndroidStudioProjects\DeepstreamDataManager\app\libs"
 * Now you can use dropbox api in your app
 *
 * You may need to use your own APP_KEY and APP_SECRET specific to your own app, we shall see...
 */



/* dd the following line of code to your AndroidManifest.xml file, in between <application> tag:
        <!-- Opens up drobpox sign in activity -->
        <activity
            android:name="com.dropbox.client2.android.AuthActivity"
            android:configChanges="orientation|keyboard"
            android:launchMode="singleTask" >
            <intent-filter>
                <data android:scheme="db-sanx2iyhwhduilr" />
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.BROWSABLE" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>

        <!-- and of course, the line below should be in your manifest to connect to the internet -->
        <uses-permission android:name="android.permission.INTERNET"></uses-permission>
 */



/*  add this line to build.grade(Module:app) under dependencies, sync the gradle after you add it:
    compile files('libs/dropbox-android-sdk-1.6.3.jar')
*/



public class DropboxServer {



    private static final String DEBUG_TAG = "DropboxServer";

    private Context context;

    final static private String APP_KEY = "sanx2iyhwhduilr";
    final static private String APP_SECRET = "2ozscj9oyix19eq";
    private DropboxAPI<AndroidAuthSession> dropboxAPI;

    public static String accessToken;



    // Init the Dropbox Server object in your onCreate MainActivity method
    public DropboxServer(Context context) {
        this.context = context;
        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys);
        dropboxAPI = new DropboxAPI<AndroidAuthSession>(session);
    }



    // Starts Dropbox Signing in activity
    public void signIn(){
        dropboxAPI.getSession().startOAuth2Authentication(context);
    }



    // Call this method in onResume method of the Activity
    public void resumeAfterSignIn(){
        if (dropboxAPI.getSession().authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                dropboxAPI.getSession().finishAuthentication();
                accessToken = dropboxAPI.getSession().getOAuth2AccessToken();
            } catch (IllegalStateException e) {
                Log.i(DEBUG_TAG, "Error authenticating dropbox account", e);
                e.printStackTrace();
            }
        }
    }




    // Uploads a File Object to dropbox server
    public void upload(String sFilePath) {

        String localPath = getLocalPath(sFilePath);
        File file = new File(sFilePath);

        try {
            // Making an InputStream out of file to give it to putFile method
            InputStream fileInputStream = new FileInputStream(sFilePath);
            // uploading to dropbox
            DropboxAPI.Entry response = dropboxAPI.putFile("/" + localPath, fileInputStream, file.length(), null, null);
        }
        catch(FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (DropboxException e) {
            e.printStackTrace();
        }
    }




    // Returns the canonical path of the file, given its absolute path
    private String getLocalPath(String sFilePath) {
        return sFilePath.substring(sFilePath.indexOf('0') + 1, sFilePath.length()).trim();
    }


    // Returns the extension of the file
    private String getFileExtension(String sFilePath) {
        return sFilePath.substring(sFilePath.indexOf('.') + 1, sFilePath.length()).trim();
    }


    // Returns a String array of folder names
    private String[] getFoldersList(String sFilePath) {
        return this.getLocalPath(sFilePath).substring(1, this.getLocalPath(sFilePath).lastIndexOf('/')).trim().split("/");
    }


    // Returns the name of the file
    private String getFileName(String sFilePath) {
        return sFilePath.substring(sFilePath.lastIndexOf('/') + 1, sFilePath.indexOf('.')).trim();
    }


    // Returns the full name of the file (name + extension)
    private String getFullName(String sFilePath) {
        return getFileName(sFilePath) + "." + getFileExtension(sFilePath);
    }


}
