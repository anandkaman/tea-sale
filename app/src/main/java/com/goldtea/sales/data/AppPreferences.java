package com.goldtea.sales.data;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {
    private static final String PREF_NAME = "GoldTeaSalesPrefs";
    private static final String KEY_MONGODB_URL = "mongodb_url";
    private static final String KEY_DB_NAME = "db_name";
    private static final String KEY_IS_AUTHENTICATED = "is_authenticated";
    
    // Default values - Using standard mongodb:// format (Android compatible)
    // Resolved from: mongodb+srv://...@teasale.uln5vpt.mongodb.net/
    public static final String DEFAULT_MONGODB_URL = "mongodb://sakshimorti4162_db_user:sakshimorti@ac-l7glrey-shard-00-00.uln5vpt.mongodb.net:27017,ac-l7glrey-shard-00-01.uln5vpt.mongodb.net:27017,ac-l7glrey-shard-00-02.uln5vpt.mongodb.net:27017/?authSource=admin&replicaSet=atlas-g6kerq-shard-0&ssl=true";
    public static final String DEFAULT_DB_NAME = "teasale";
    
    private final SharedPreferences prefs;
    
    public AppPreferences(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    // MongoDB URL
    public String getMongoDbUrl() {
        return prefs.getString(KEY_MONGODB_URL, DEFAULT_MONGODB_URL);
    }
    
    public void setMongoDbUrl(String url) {
        prefs.edit().putString(KEY_MONGODB_URL, url).apply();
    }
    
    // Database Name
    public String getDbName() {
        return prefs.getString(KEY_DB_NAME, DEFAULT_DB_NAME);
    }
    
    public void setDbName(String dbName) {
        prefs.edit().putString(KEY_DB_NAME, dbName).apply();
    }
    
    // Authentication
    public boolean isAuthenticated() {
        return prefs.getBoolean(KEY_IS_AUTHENTICATED, false);
    }
    
    public void setAuthenticated(boolean authenticated) {
        prefs.edit().putBoolean(KEY_IS_AUTHENTICATED, authenticated).apply();
    }
    
    // Clear all preferences
    public void clear() {
        prefs.edit().clear().apply();
    }
}
