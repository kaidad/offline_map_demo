package com.kaidad.geo.plugins;

import android.util.Log;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.FileTransfer;
import org.apache.cordova.api.CallbackContext;
import org.apache.cordova.api.CordovaInterface;
import org.apache.cordova.api.CordovaPlugin;
import org.apache.cordova.api.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;

/**
 * Used to fetch SQLite Database Files
 */
public class DatabaseFetcherPlugin extends CordovaPlugin {

    public static final String FETCH = "fetch";
    private FileTransfer fileTransfer = new FileTransfer();


    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        fileTransfer.initialize(cordova, webView);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (FETCH.equals(action)) {
            if (args.length() != 5) {
                callbackContext.error("Expected exactly two arguments - DbName, DbUrl");
            }

            String dbName = args.getString(0);

            String urlString = args.getString(1);
            if (urlString == null || urlString.trim().length() == 0) {
                callbackContext.error("Found blank URL (null or zero-length)");
                return false;
            }

            fetchDatabaseFromUrl(urlString, dbName, callbackContext, args);
        }
        return true;
    }

    public void fetchDatabaseFromUrl(final String url, final String dbName, final CallbackContext callbackContext, final JSONArray args) {
        cordova.getThreadPool().execute(new Runnable() {

            @Override
            public void run() {
                Log.v("info", "Checking to see if DB exists: " + dbName);
                File dbFile = cordova.getActivity().getDatabasePath(dbName);

                if (dbFile.exists()) {
                    Log.v("info", "DB already exists: " + dbName);
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, true));
                }

                try {
                    JSONArray fileTransferArgs = new JSONArray();
                    fileTransferArgs.put(url);
                    fileTransferArgs.put(dbFile.getAbsolutePath());
                    fileTransferArgs.put(args.get(2)); //trust everyone
                    fileTransferArgs.put(args.get(3)); //object ID
                    fileTransferArgs.put(args.get(4)); //headers
                    Log.v("info", "Calling FileTransfer::download with args: " + fileTransferArgs);
                    fileTransfer.execute("download", fileTransferArgs, callbackContext);
                } catch (JSONException e) {
                    callbackContext.error(e.getMessage());
                }
            }
        });


    }
}
