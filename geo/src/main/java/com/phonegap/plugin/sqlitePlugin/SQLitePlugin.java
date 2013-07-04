/*
 * PhoneGap is available under *either* the terms of the modified BSD license *or* the
 * MIT License (2008). See http://opensource.org/licenses/alphabetical for full text.
 *
 * Copyright (c) 2005-2010, Nitobi Software Inc.
 * Copyright (c) 2010, IBM Corporation
 */
package com.phonegap.plugin.sqlitePlugin;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.util.Base64;
import android.util.Log;
import org.apache.cordova.api.CallbackContext;
import org.apache.cordova.api.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;

/**
 * SQLitePlugin was downloaded from https://github.com/pgsqlite/PG-SQLitePlugin-Android.
 *
 * However, this file has been modified to mitigate some performance issues. Specifically,
 * I determined that most of the time was spent in JSONArray.toString() in the results2string
 * method. It turns out JSONArray.toString() processed the object graph one character at a time
 * and this was very inefficient. I modified this to use a very simplified approach using a
 * StringBuilder and this resulted in noticeable improvement in rendering speed. I still feel
 * the rendering speed is WAY too slow considering the fact that we're loading the map tiles
 * from a local database rather than making network calls, but this is a good start.
 */
@SuppressWarnings("unused")
public class SQLitePlugin extends CordovaPlugin {
    public static final char ARRAY_START = '[';
    public static final char OBJ_START = '{';
    public static final char QUOTE = '\'';
    public static final char COLON = ':';
    public static final char COMMA = ',';
    public static final char ARRAY_END = ']';
    public static final char OBJECT_END = '}';
    /**
     * Multiple database map.
     */
    HashMap<String, SQLiteDatabase> dbmap;

    /**
     * Constructor.
     */
    @SuppressWarnings("unused")
    public SQLitePlugin() {
        dbmap = new HashMap<String, SQLiteDatabase>();
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action The action to execute.
     * @param args   JSONArry of arguments for the plugin.
     * @param cbc    Callback context from Cordova API (not used here)
     */
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext cbc) {
        try {
            if (action.equals("open")) {
                JSONObject o = args.getJSONObject(0);
                String dbname = o.getString("name");

                openDatabase(dbname, null, args.optBoolean(1));
            } else if (action.equals("close")) {
                closeDatabase(args.getString(0));
            } else if (action.equals("executePragmaStatement")) {
                String dbName = args.getString(0);
                String query = args.getString(1);

                Cursor myCursor = getDatabase(dbName).rawQuery(query, null);
                processPragmaResults(myCursor, id);
            } else if (action.equals("executeSqlBatch")) {
                String[] queries;
                String[] queryIDs = null;
                String trans_id = null;
                JSONObject a;
                JSONArray jsonArr;
                JSONArray[] jsonparams = null;

                String dbName = args.getString(0);
                JSONArray txargs = args.getJSONArray(1);

                if (txargs.isNull(0)) {
                    queries = new String[0];
                } else {
                    int len = txargs.length();
                    queries = new String[len];
                    queryIDs = new String[len];
                    jsonparams = new JSONArray[len];

                    for (int i = 0; i < len; i++) {
                        a = txargs.getJSONObject(i);
                        queries[i] = a.getString("query");
                        queryIDs[i] = a.getString("query_id");
                        trans_id = a.getString("trans_id");
                        jsonArr = a.getJSONArray("params");
                        jsonparams[i] = jsonArr;
                    }
                }
                if (trans_id != null)
                    executeSqlBatch(dbName, queries, jsonparams, queryIDs, trans_id);
                else
                    Log.v("error", "null trans_id");
            } else if (action.equals("getDatabasePath")) {
                if (args.length() != 1) {
                    Log.v("error", "getDatabasePath called with no database name");
                    cbc.error("getDatabasePath called with no database name!");
                }
                String dbName = args.getString(0);
                getDatabasePath(dbName, cbc);
            } else if (action.equals("databaseExists")) {
                if (args.length() != 1) {
                    Log.v("error", "databaseExists called with no database name");
                    cbc.error("databaseExists called with no database name!");
                }
                String dbName = args.getString(0);
                databaseExists(dbName, cbc);
            } else {
                return false;
            }

            return true;
        } catch (JSONException e) {
            cbc.error("JSON error: " + e.getMessage());

            return false;
        }
    }

    private void getDatabasePath(String dbname, CallbackContext cbc) {
        File path = cordova.getActivity().getDatabasePath(dbname);
        final String pathStr;
        if (path == null) {
            pathStr = null;
        } else {
            pathStr = path.getAbsolutePath().substring(0, path.getAbsolutePath().indexOf(dbname));
        }
        cbc.success(pathStr);
    }

    private void databaseExists(String dbname, CallbackContext cbc) {
        File path = cordova.getActivity().getDatabasePath(dbname);
        JSONArray ret = new JSONArray();
        ret.put(path.exists());
        cbc.success(ret);
    }

    /**
     * Clean up and close all open databases.
     */
    @Override
    public void onDestroy() {
        while (!dbmap.isEmpty()) {
            String dbname = dbmap.keySet().iterator().next();
            closeDatabase(dbname);
            dbmap.remove(dbname);
        }
    }

    // --------------------------------------------------------------------------
    // LOCAL METHODS
    // --------------------------------------------------------------------------

    /**
     * Open a database.
     *
     * @param dbname   The name of the database-NOT including its extension.
     * @param password The database password or null.
     */
    private void openDatabase(String dbname, String password, boolean create) {
        if (getDatabase(dbname) != null) closeDatabase(dbname);

        File dbfile = cordova.getActivity().getDatabasePath(dbname);

        Log.v("info", "Open sqlite db: " + dbfile.getAbsolutePath());

        SQLiteDatabase mydb = SQLiteDatabase.openOrCreateDatabase(dbfile, null);

        Cursor c = mydb.rawQuery("SELECT name FROM sqlite_master where type='table' order by name", null);
        if (c != null) {
            if (c.moveToFirst()) {
                Log.v("info", "Database contains the following tables");
                do {
                    String tableName = c.getString(c.getColumnIndex("name"));
                    Log.v("info", "table: " + tableName);
                } while (c.moveToNext());
            }
        }
        dbmap.put(dbname, mydb);
    }

    /**
     * Close a database.
     *
     * @param dbName The name of the database-NOT including its extension.
     */
    private void closeDatabase(String dbName) {
        SQLiteDatabase mydb = getDatabase(dbName);

        if (mydb != null) {
            mydb.close();
            dbmap.remove(dbName);
        }
    }

    /**
     * Get a database from the db map.
     *
     * @param dbname The name of the database.
     */
    private SQLiteDatabase getDatabase(String dbname) {
        return dbmap.get(dbname);
    }

    /**
     * Executes a batch request and sends the results via sendJavascriptCB().
     *
     * @param dbname     The name of the database.
     * @param queryarr   Array of query strings
     * @param jsonparams Array of JSON query parameters
     * @param queryIDs   Array of query ids
     * @param tx_id      Transaction id
     */
    private void executeSqlBatch(String dbname, String[] queryarr, JSONArray[] jsonparams, String[] queryIDs, String tx_id) {
        SQLiteDatabase mydb = getDatabase(dbname);

        if (mydb == null) return;

        try {
            mydb.beginTransaction();

            String query;
            String query_id;
            int len = queryarr.length;

            for (int i = 0; i < len; i++) {
                query = queryarr[i];
                query_id = queryIDs[i];
                if (query.toLowerCase().startsWith("insert") && jsonparams != null) {
                    SQLiteStatement myStatement = mydb.compileStatement(query);
                    for (int j = 0; j < jsonparams[i].length(); j++) {
                        if (jsonparams[i].get(j) instanceof Float || jsonparams[i].get(j) instanceof Double) {
                            myStatement.bindDouble(j + 1, jsonparams[i].getDouble(j));
                        } else if (jsonparams[i].get(j) instanceof Number) {
                            myStatement.bindLong(j + 1, jsonparams[i].getLong(j));
                        } else if (jsonparams[i].isNull(j)) {
                            myStatement.bindNull(j + 1);
                        } else {
                            myStatement.bindString(j + 1, jsonparams[i].getString(j));
                        }
                    }
                    long insertId = myStatement.executeInsert();

                    String result = "{'insertId':'" + insertId + "'}";
                    sendJavascriptCB("window.SQLitePluginTransactionCB.queryCompleteCallback('" +
                            tx_id + "','" + query_id + "', " + result + ");");
                } else {
                    String[] params = null;

                    if (jsonparams != null) {
                        params = new String[jsonparams[i].length()];

                        for (int j = 0; j < jsonparams[i].length(); j++) {
                            if (jsonparams[i].isNull(j))
                                params[j] = "";
                            else
                                params[j] = jsonparams[i].getString(j);
                        }
                    }

                    Cursor myCursor = mydb.rawQuery(query, params);

                    if (query_id.length() > 0)
                        processResults(myCursor, query_id, tx_id);

                    myCursor.close();
                }
            }
            mydb.setTransactionSuccessful();
        } catch (SQLiteException ex) {
            ex.printStackTrace();
            Log.v("executeSqlBatch", "SQLitePlugin.executeSql(): Error=" + ex.getMessage());
            sendJavascriptCB("window.SQLitePluginTransactionCB.txErrorCallback('" + tx_id + "', '" + ex.getMessage() + "');");
        } catch (JSONException ex) {
            ex.printStackTrace();
            Log.v("executeSqlBatch", "SQLitePlugin.executeSql(): Error=" + ex.getMessage());
            sendJavascriptCB("window.SQLitePluginTransactionCB.txErrorCallback('" + tx_id + "', '" + ex.getMessage() + "');");
        } finally {
            mydb.endTransaction();
            Log.v("executeSqlBatch", tx_id);
            sendJavascriptCB("window.SQLitePluginTransactionCB.txCompleteCallback('" + tx_id + "');");

        }
    }

    /**
     * Process query results.
     *
     * @param cur      Cursor into query results
     * @param query_id Query id
     * @param tx_id    Transaction id
     */
    private void processResults(Cursor cur, String query_id, String tx_id) {
        String result = results2string(cur);

        sendJavascriptCB("window.SQLitePluginTransactionCB.queryCompleteCallback('" +
                tx_id + "','" + query_id + "', " + result + ");");
    }

    /**
     * Process query results.
     *
     * @param cur Cursor into query results
     * @param id  Caller db id
     */
    private void processPragmaResults(Cursor cur, String id) {
        String result = results2string(cur);

        sendJavascriptCB("window.SQLitePluginCallback.p1('" + id + "', " + result + ");");
    }

    /**
     * Convert results cursor to JSON string.
     *
     * @param cur Cursor into query results
     * @return results in string form
     */
    private String results2string(Cursor cur) {
        String result = "[]";

        final StringBuilder sb = new StringBuilder(100000);
        try {
            // If query result has rows
            if (cur.moveToFirst()) {
                if (sb.length() > 0) {
                    sb.append(COMMA);
                }

                //JSONArray fullresult = new JSONArray();
                sb.append(ARRAY_START);
                String key;
                int colCount = cur.getColumnCount();

                // Build up JSON result object for each row
                do {
                    //JSONObject row = new JSONObject();
                    sb.append(OBJ_START);
                    for (int i = 0; i < colCount; ++i) {
                        key = cur.getColumnName(i);

                        if (i > 0) {
                            sb.append(COMMA);
                        }
                        sb.append(QUOTE).append(key).append(QUOTE).append(COLON).append(QUOTE);
                        // for old Android SDK remove lines from HERE:
                        if (android.os.Build.VERSION.SDK_INT >= 11) {
                            switch (cur.getType(i)) {
                                case Cursor.FIELD_TYPE_NULL:
                                    sb.append(JSONObject.NULL.toString()).append(QUOTE);
                                    //row.put(key, JSONObject.NULL);
                                    break;
                                case Cursor.FIELD_TYPE_INTEGER:
                                    sb.append(cur.getInt(i)).append(QUOTE);
                                    //row.put(key, cur.getInt(i));
                                    break;
                                case Cursor.FIELD_TYPE_FLOAT:
                                    sb.append(cur.getFloat(i)).append(QUOTE);
                                    //row.put(key, cur.getFloat(i));
                                    break;
                                case Cursor.FIELD_TYPE_STRING:
                                    sb.append(cur.getString(i)).append(QUOTE);
                                    //row.put(key, cur.getString(i));
                                    break;
                                case Cursor.FIELD_TYPE_BLOB:
                                    sb.append(new String(Base64.encode(cur.getBlob(i), Base64.NO_WRAP))).append(QUOTE);
                                    //row.put(key, new String(Base64.encode(cur.getBlob(i), Base64.DEFAULT)));
                                    break;
                            }
                        } else // to HERE.
                        {
                            String s;
                            try {
                                s = cur.getString(i);
                            } catch (Exception e) {
                                e.printStackTrace();
                                try {
                                    s = new String(Base64.encode(cur.getBlob(i), Base64.DEFAULT));
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                    throw new RuntimeException(e1);
                                }
                            }

                            //row.put(key, s);
                            sb.append(s).append(QUOTE);
                        }
                    }
                    sb.append(OBJECT_END);

                    //fullresult.put(row);


                } while (cur.moveToNext());
                sb.append(ARRAY_END);

                //result = fullresult.toString();
                result = sb.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }


        return result;
    }

    /**
     * Send Javascript callback.
     *
     * @param cb Javascript callback command to send
     */
    private void sendJavascriptCB(String cb) {
        webView.sendJavascript(cb);
    }
}
