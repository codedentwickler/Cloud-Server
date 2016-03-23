package com.deepstream.data.arashnase.driveserver;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.ExponentialBackOff;

import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.*;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


/**
 * Created by arash.nase on 3/2/2016.
 * A class to help developer manage user's Google Drive account, it encapsulates Google Drive features
 * Such as logging in, downloading files, uploading files...
 * Based on Google Drive and Client REST API
 */


public class GoogleDrive {

    private Context context;
    private static String DEBUG_TAG = "GoogleDrive";  // Logging tag
    public static String APPLICATION_NAME = "Deepstream Data";     // Application name

    // These are for sharedPreferences file to access variables across activities and services
    public static final String ACCOUNT_NAME = null;
    public static final String DATA_STORE_FACTORY = "GoogleAccountCredentials";

    // Global instance of the JSON factory.
    private static JsonFactory JSON_FACTORY;
    // Global instance of the HTTP transport.
    private static HttpTransport HTTP_TRANSPORT;
    // Global instance of the scopes required by this app (features you need)
    private static final String[] SCOPES =
            {DriveScopes.DRIVE_METADATA, DriveScopes.DRIVE, DriveScopes.DRIVE_FILE};

    // The interface to interact with Google Drive
    private Drive service;
    private static GoogleAccountCredential credential; // could be defined to be static, the same across all instances of the class
    private String accountName;

    // TODO: A HashMap of files on the drive for faster access to what is on the drive.
    // When calling upload/remove, you should add/delete corresponding key-value pair from this var
    // In this way, we won't need to call get list all the time (but what if user change the add a file through googleDrive?
    private HashMap<String, File> filesHashMap;


    // Constructor: inits the class variables
    public GoogleDrive(Context context) {
        this.context = context;
        JSON_FACTORY = JacksonFactory.getDefaultInstance();

        try {
            HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        // Getting the accountName from sharedPreferences, initially set by the activity
        // that called AccountPicker
        SharedPreferences settings =
                this.context.getSharedPreferences(DATA_STORE_FACTORY, Context.MODE_PRIVATE);
        accountName = settings.getString(ACCOUNT_NAME, null);
        //accountName = "shawn.numecent@gmail.com";

        // Initializing the Google credential object for Drive access with  Account Name
        this.credential = GoogleAccountCredential
                .usingOAuth2(this.context, Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(accountName);
    }



    // Method to log into the account, builds an authorized Drive client service.
    public void logOn() throws IOException {
        // Initializes the Drive API by making a call to Google Client Server
        this.service = new Drive.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, this.credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }


    // Abstract method to log off from the account
    public boolean logOff(){
        return true;
        // TODO
    }


    // Abstract method to upload the file to the account
    public void uploadFile(String sFilePath) {
        java.io.File file = new java.io.File(sFilePath);
        File googleFile = this.insertFile(sFilePath);
        Log.v(DEBUG_TAG, googleFile.getId() + " was uploaded successfully.");
    }


    // Abstract method to download the file from the account
    public boolean downloadFile(String sFilePath, String sDestinationFilePath) {
        return true;
        // TODO
    }


    // Abstract method to remove a file from the account
    // Returns true if successfully deletes the file, false otherwise
    public boolean removeFile(String sFilePath) {
        ArrayList<File> files = null;
        try {
            files = this.getDriveFiles();
        }
        catch(IOException e){
            Log.v(DEBUG_TAG, "removeFile: Cannot access drive.");
            e.printStackTrace();
            return false;
        }

        if (files == null || files.size() == 0) {
            Log.v(DEBUG_TAG, "No files found.");
        }
        else {
            for (File file : files) {
                if (file.getTitle().equals(sFilePath)) {
                    deleteFile(file.getId());
                    return true;
                }
            }
        }
        return false;
    }


    // Abstract method to check to see if a file exists in the account
    public boolean fileExists(String sFilePath) {
        // TODO: use file.getParents which returns a list of parentreferences to check if a file exists
        String localPath = getLocalPath(sFilePath);
        String fileName = getFileName(sFilePath);
        String fileExt = getFileExtension(sFilePath);
        String[] parents = getFoldersList(sFilePath);

        ArrayList<File> files = null;
        try {
            files = this.getDriveFiles();
        } catch (IOException e) {
            Log.v(DEBUG_TAG, "fileExists: Cannot access drive.");
            e.printStackTrace();
        }

        if (files == null || files.size() == 0) {
            return false;
        } else {
            for (int i = 0; i < parents.length; i++) {
                for (File file : files) {
                    //System.out.println(file.getTitle() + " : " + file.getMimeType() + " parent: " + file.getParents());
                    if (file.getTitle().equals(parents[i])) {

                    }

                }
            }
            return false;
        }
    }




	/*
	 * ************************************************************
	 *  				Getters and Setters
	 * ************************************************************
	 */

    public GoogleAccountCredential getCredentials(){
        return credential;
    }

    public void setCredentials(GoogleAccountCredential credentials){
        this.credential = credentials;
    }

    public String getAccountName(){
        return accountName;
    }

    /**
     * Returns a ArrayList of Google.File objects in the drive account
     * @return ArrayList<Google.File>
     * @throws IOException
     */
    public ArrayList<File> getDriveFiles() throws IOException{
        FileList result = service.files().list().execute();
        ArrayList<File> files = (ArrayList<File>) result.getItems();
        return files;
    }




	/*
	 * ************************************************************
	 *  				Private helper methods
	 * ************************************************************
	 */

    /**
     * Insert new file to Drive
     * @param filePath Absolute path of the file to insert
     * @return Inserted file metadata if successful, {@code null} otherwise.
     */
    private File insertFile(String filePath) {
        // Set file's metadata on the server
        File body = new File();
        // Title of the file to insert, including the extension.
        body.setTitle(getFileName(filePath) + "." + getFileExtension(filePath));
        body.setMimeType(getMimeType(getFileExtension(filePath)));
        body.setFileExtension(getFileExtension(filePath));

        String[] folderList = getFoldersList(filePath);

        // Set the parent folder of the file
        body.setParents(Arrays.asList(new ParentReference().setId(makeParentFolders(folderList))));

        // Set file's content.
        java.io.File fileContent = new java.io.File(filePath);
        // Set file's type
        FileContent mediaContent = new FileContent(null, fileContent);

        try {
            File file = this.service.files().insert(body, mediaContent).execute();
            return file;
        } catch (IOException e) {
            Log.v(DEBUG_TAG, "An error occurred while uploading.");
            e.printStackTrace();
            return null;
        }
    }

    // This function gets an array of Strings representing a file's parent folders
    // And returns the most inner parent folder's ID
    private String makeParentFolders(String[] parentList) {
        File folder = null;
        String folderId = null;
        for(int i = 0; i < parentList.length; i++) {
            folder = new File();
            folder.setMimeType(getMimeType("folder"));
            folder.setTitle(parentList[i]);
            if (i > 0) {
                folder.setParents(Arrays.asList(new ParentReference().setId(folderId)));
            }
            try {
                folder = this.service.files().insert(folder).execute();
                folderId = folder.getId();
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
        return folder.getId();
    }


    /**
     * Permanently delete a file, skipping the trash.
     * @param fileId ID of the file to delete.
     */
    private void deleteFile(String fileId) {
        try {
            this.service.files().delete(fileId).execute();
        } catch (IOException e) {
            Log.v(DEBUG_TAG, "An error occurred while deleting.");
            e.printStackTrace();
        }
    }


    /**
     * Download a file's content.
     * @param file Drive File instance.
     * @return InputStream containing the file's content if successful,
     *         {@code null} otherwise.
     */
    private InputStream downloadFile(File file) {
        if (file.getDownloadUrl() != null && file.getDownloadUrl().length() > 0) {
            try {
                HttpResponse resp = this.service.getRequestFactory()
                        .buildGetRequest(new GenericUrl(file.getDownloadUrl()))
                        .execute();
                return resp.getContent();
            } catch (IOException e) {
                Log.v(DEBUG_TAG, "An error occurred while downloading.");
                e.printStackTrace();
                return null;
            }
        } else {
            // The file doesn't have any content stored on Drive.
            return null;
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


    private String getMimeType(String extension) {
        String mime;
        switch (extension) {
            case "xls":
                mime = "application/vnd.ms-excel";
                break;
            case "xlsx":
                mime = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                break;
            case "csv":
                mime = "text/plain";
                break;
            case "pdf":
                mime = "application/pdf";
                break;
            case "jpg":
                mime = "image/jpeg";
                break;
            case "xml":
                mime = "text/xml";
                break;
            case "txt":
                mime = "text/plain";
                break;
            case "doc":
                mime = "application/msword";
                break;
            case "mp3":
                mime = "audio/mpeg";
                break;
            case "zip":
                mime = "application/zip";
                break;
            case "rar":
                mime = "application/rar";
                break;
            case "html":
                mime = "text/html";
                break;
            case "folder":
                mime = "application/vnd.google-apps.folder";
                break;
            default:
                mime = null;
                break;
        }
        return mime;
    }

}
