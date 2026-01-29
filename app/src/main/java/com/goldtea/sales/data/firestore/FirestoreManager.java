package com.goldtea.sales.data.firestore;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;
import androidx.annotation.NonNull;

import com.goldtea.sales.data.model.Customer;
import com.goldtea.sales.data.model.Pricing;
import com.goldtea.sales.data.model.Sale;
import com.goldtea.sales.data.model.Village;
import com.goldtea.sales.data.model.Note;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Firebase Firestore Manager - Replaces MongoDBManager
 * Provides all database operations using Firebase Firestore
 */
public class FirestoreManager {
    private static final String TAG = "FirestoreManager";
    private static FirestoreManager instance;
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Context context;
    
    // Collection names
    private static final String SALES_COLLECTION = "sales";
    private static final String VILLAGES_COLLECTION = "villages";
    private static final String PRICING_COLLECTION = "pricing";
    private static final String CUSTOMERS_COLLECTION = "customers";
    private static final String NOTES_COLLECTION = "notes";

    private boolean isOnline = false;
    private OnConnectionStatusChangedListener connectionStatusListener;

    // In-memory cache for performance optimization
    private List<Sale> cachedSales = null;
    private List<Village> cachedVillages = null;
    private List<Pricing> cachedPricing = null;
    private List<Customer> cachedCustomers = null;
    private List<Note> cachedNotes = null;
    private List<Sale> lastRangeSales = null;
    private Date lastRangeStart = null;
    private Date lastRangeEnd = null;
    private long salesCacheTime = 0;
    private long villagesCacheTime = 0;
    private long pricingCacheTime = 0;
    private long customersCacheTime = 0;
    private long notesCacheTime = 0;
    private long rangeCacheTime = 0;
    private static final int NOTE_CACHE_SIZE = 5;
    private final Map<String, Note> noteCache = new LinkedHashMap<String, Note>(NOTE_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Note> eldest) {
            return size() > NOTE_CACHE_SIZE;
        }
    };
    private final Map<String, Long> noteCacheTimes = new HashMap<>();
    private static final long CACHE_VALIDITY_MS = 240000; // 4 minutes cache validity
    
    private FirestoreManager(Context context) {
        this.context = context.getApplicationContext();
        
        // 1. Initialize Firestore immediately to enable offline persistence
        try {
            db = FirebaseFirestore.getInstance();
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
            db.setFirestoreSettings(settings);
            Log.d(TAG, "Firestore initialized with persistence enabled");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firestore", e);
        }

        // 2. Initialize Auth
        auth = FirebaseAuth.getInstance();

        // 3. Setup Connectivity Listener
        setupConnectivityListener();
    }
    
    private void setupConnectivityListener() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            // Check initial state
            isOnline = checkCurrentNetworkStatus(connectivityManager);
            Log.d(TAG, "Initial connectivity status: " + (isOnline ? "ONLINE" : "OFFLINE"));

            try {
                // Use registerDefaultNetworkCallback for simpler tracking of the active network (API 24+)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                        @Override
                        public void onAvailable(@NonNull Network network) {
                            Log.d(TAG, "Network became available: " + network);
                            updateOnlineStatus(true);
                        }

                        @Override
                        public void onLost(@NonNull Network network) {
                            Log.d(TAG, "Network lost: " + network);
                            updateOnlineStatus(false);
                        }
                    });
                } else {
                    // Fallback for API 21-23
                    NetworkRequest request = new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build();
                    
                    connectivityManager.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
                        @Override
                        public void onAvailable(@NonNull Network network) {
                            Log.d(TAG, "Network available (API < 24): " + network);
                            updateOnlineStatus(true);
                        }

                        @Override
                        public void onLost(@NonNull Network network) {
                            Log.d(TAG, "Network lost (API < 24): " + network);
                            updateOnlineStatus(false);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error registering network callback", e);
            }
        }
    }

    private boolean checkCurrentNetworkStatus(ConnectivityManager cm) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
                return capabilities != null && 
                       capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            }
        } else {
            android.net.NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private void updateOnlineStatus(boolean online) {
        if (this.isOnline != online) {
            this.isOnline = online;
            Log.i(TAG, "Connectivity status updated to: " + (online ? "ONLINE" : "OFFLINE"));
            if (connectionStatusListener != null) {
                connectionStatusListener.onStatusChanged(online);
            }
        }
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnConnectionStatusChangedListener(OnConnectionStatusChangedListener listener) {
        this.connectionStatusListener = listener;
        // Immediate callback with current state
        if (listener != null) {
            listener.onStatusChanged(isOnline);
        }
    }

    public interface OnConnectionStatusChangedListener {
        void onStatusChanged(boolean isOnline);
    }
    
    public static synchronized FirestoreManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirestoreManager(context);
        }
        return instance;
    }
    
    /**
     * Initialize Firestore with offline persistence
     */
    public void initialize(final OnInitializeListener listener) {
        // Check if already signed in
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Already signed in as: " + currentUser.getUid());
            if (listener != null) listener.onSuccess();
            return;
        }
        
        // Sign in anonymously for database access
        auth.signInAnonymously()
            .addOnSuccessListener(authResult -> {
                Log.d(TAG, "Anonymous authentication successful");
                if (listener != null) listener.onSuccess();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Anonymous authentication failed", e);
                // We still call onSuccess because Firestore works offline
                if (listener != null) listener.onSuccess();
            });
    }
    
    
    // ============================================
    // SALES OPERATIONS
    // ============================================
    

    /**
     * Get paginated sales records
     */
    public void getSalesPaginated(int limit, DocumentSnapshot lastDocument, final OnSalesLoadedListener listener) {
        getSalesPaginated(limit, lastDocument, listener, false);
    }

    public void getSalesPaginated(int limit, DocumentSnapshot lastDocument, final OnSalesLoadedListener listener, boolean forceRefresh) {
        if (db == null) {
            listener.onSalesLoaded(new ArrayList<>());
            return;
        }

        // Return cached data for the FIRST page if valid
        if (lastDocument == null && !forceRefresh && cachedSales != null &&
            System.currentTimeMillis() - salesCacheTime < CACHE_VALIDITY_MS) {
            Log.d(TAG, "Returning first page of sales from in-memory cache");
            if (listener instanceof OnSalesPaginatedListener) {
                // If we cache, we should also track the last snapshot, but for simplicity
                // we'll just allow the first page to be cached and re-fetch snapshots if they scroll.
                // However, the cleanest way is to just let the cache handle the initial load.
                ((OnSalesPaginatedListener) listener).onSalesLoaded(new ArrayList<>(cachedSales), null); 
            } else {
                listener.onSalesLoaded(new ArrayList<>(cachedSales));
            }
            return;
        }

        Query query = db.collection(SALES_COLLECTION)
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(limit);

        if (lastDocument != null) {
            query = query.startAfter(lastDocument);
        }

        query.get().addOnSuccessListener(querySnapshot -> {
            List<Sale> sales = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Sale sale = doc.toObject(Sale.class);
                if (sale != null) {
                    sale.setSale_id(doc.getId());
                    sales.add(sale);
                }
            }

            // Update in-memory cache ONLY for the first page
            if (lastDocument == null) {
                cachedSales = new ArrayList<>(sales);
                salesCacheTime = System.currentTimeMillis();
                Log.d(TAG, "Updated first-page sales cache");
            }

            if (listener instanceof OnSalesPaginatedListener) {
                DocumentSnapshot nextLast = null;
                if (!querySnapshot.isEmpty()) {
                    nextLast = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
                }
                ((OnSalesPaginatedListener) listener).onSalesLoaded(sales, nextLast);
            } else {
                listener.onSalesLoaded(sales);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading paginated sales", e);
            listener.onError(e.getMessage());
        });
    }

    /**
     * Get sales by date range for Reports
     */
    public void getSalesByDateRange(Date startDate, Date endDate, final OnSalesLoadedListener listener) {
        if (db == null) {
            listener.onSalesLoaded(new ArrayList<>());
            return;
        }

        // Check range cache
        if (lastRangeSales != null && startDate.equals(lastRangeStart) && endDate.equals(lastRangeEnd) &&
            System.currentTimeMillis() - rangeCacheTime < CACHE_VALIDITY_MS) {
            Log.d(TAG, "Returning sales by date range from in-memory cache");
            listener.onSalesLoaded(new ArrayList<>(lastRangeSales));
            return;
        }

        db.collection(SALES_COLLECTION)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Sale> sales = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Sale sale = doc.toObject(Sale.class);
                        if (sale != null) {
                            sale.setSale_id(doc.getId());
                            sales.add(sale);
                        }
                    }
                    
                    // Update range cache
                    lastRangeSales = new ArrayList<>(sales);
                    lastRangeStart = startDate;
                    lastRangeEnd = endDate;
                    rangeCacheTime = System.currentTimeMillis();
                    
                    listener.onSalesLoaded(sales);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading sales by date range", e);
                    listener.onError(e.getMessage());
                });
    }
    
    /**
     * Add new sale
     */
    public void addSale(final Sale sale, final OnSaleAddedListener listener) {
        if (db == null) {
            listener.onError("Firestore not initialized");
            return;
        }

        // Invalidate cache since we're adding new data
        invalidateSalesCache();

        // 1. Instantly write to local cache (Persistence is enabled)
        db.collection(SALES_COLLECTION)
            .document(sale.getSale_id())
            .set(sale)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Sale synced with server: " + sale.getSale_id());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Server sync failed for sale: " + sale.getSale_id(), e);
            });

        // 2. Also save customer for autocomplete (Optimistic)
        Customer customer = new Customer();
        customer.setVillage(sale.getVillage());
        customer.setCustomer_name(sale.getCustomer_name());
        addCustomer(customer, null);

        // 3. Trigger success IMMEDIATELY for the UI
        Log.d(TAG, "Sale queued locally: " + sale.getSale_id());
        listener.onSuccess();
    }
    
    /**
     * Update existing sale
     */
    public void updateSale(final Sale sale, final OnSaleUpdatedListener listener) {
        if (db == null) {
            listener.onError("Firestore not initialized");
            return;
        }

        if (sale.getSale_id() == null) {
            listener.onError("Cannot update sale: Sale ID is null");
            return;
        }

        // Invalidate cache since we're modifying data
        invalidateSalesCache();

        Log.d(TAG, "Attempting to update sale with ID: " + sale.getSale_id() + " for " + sale.getCustomer_name());

        // Refresh updated_at timestamp
        sale.setUpdated_at(new Date());

        // Use document ID directly for update
        db.collection(SALES_COLLECTION)
            .document(sale.getSale_id())
            .set(sale)
            .addOnSuccessListener(aVoid -> {
                Log.i(TAG, "Sale updated successfully: " + sale.getSale_id());
                listener.onSuccess();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating sale: " + sale.getSale_id(), e);
                listener.onError(e.getMessage());
            });
    }
    
    /**
     * Delete sale
     */
    public void deleteSale(final String saleId, final OnSaleDeletedListener listener) {
        if (db == null) {
            listener.onError("Firestore not initialized");
            return;
        }

        if (saleId == null) {
            listener.onError("Cannot delete sale: Sale ID is null");
            return;
        }

        // Invalidate cache since we're deleting data
        invalidateSalesCache();

        // Use document ID directly for delete
        db.collection(SALES_COLLECTION)
            .document(saleId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Sale deleted successfully");
                listener.onSuccess();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error deleting sale", e);
                listener.onError(e.getMessage());
            });
    }

    /**
     * Invalidate sales cache - call when sales data is modified
     */
    public void invalidateSalesCache() {
        cachedSales = null;
        salesCacheTime = 0;
        lastRangeSales = null;
        lastRangeStart = null;
        lastRangeEnd = null;
        rangeCacheTime = 0;
        Log.d(TAG, "Sales and Range caches invalidated");
    }
    
    // ============================================
    // VILLAGE OPERATIONS
    // ============================================
    
    /**
     * Get all villages (with caching)
     */
    public void getAllVillages(final OnVillagesLoadedListener listener) {
        getAllVillages(listener, false);
    }

    /**
     * Get all villages with option to force refresh
     */
    public void getAllVillages(final OnVillagesLoadedListener listener, boolean forceRefresh) {
        if (db == null) {
            Log.w(TAG, "Firestore not initialized. Returning empty list.");
            listener.onVillagesLoaded(new ArrayList<>());
            return;
        }

        // Return cached data if valid and not forcing refresh
        if (!forceRefresh && cachedVillages != null &&
            System.currentTimeMillis() - villagesCacheTime < CACHE_VALIDITY_MS) {
            Log.d(TAG, "Returning " + cachedVillages.size() + " villages from cache");
            listener.onVillagesLoaded(new ArrayList<>(cachedVillages));
            return;
        }

        db.collection(VILLAGES_COLLECTION)
            .orderBy("name", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                Map<String, Village> uniqueVillages = new HashMap<>();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    Village village = doc.toObject(Village.class);
                    if (village != null) {
                        String name = village.getName() != null ? village.getName().trim() : "";
                        if (!name.isEmpty()) {
                            // Prioritize standardized IDs (where ID == name)
                            if (!uniqueVillages.containsKey(name) || doc.getId().equals(name)) {
                                uniqueVillages.put(name, village);
                            }
                        }
                    }
                }
                List<Village> villagesList = new ArrayList<>(uniqueVillages.values());
                // Sort the consolidated list as well
                Collections.sort(villagesList, (v1, v2) -> v1.getName().compareToIgnoreCase(v2.getName()));
                // Update cache
                cachedVillages = new ArrayList<>(villagesList);
                villagesCacheTime = System.currentTimeMillis();
                Log.d(TAG, "Loaded and cached " + villagesList.size() + " unique villages");
                listener.onVillagesLoaded(villagesList);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading villages", e);
                // Return cached data if available on error
                if (cachedVillages != null) {
                    Log.d(TAG, "Returning cached villages on error");
                    listener.onVillagesLoaded(new ArrayList<>(cachedVillages));
                } else {
                    listener.onError(e.getMessage());
                }
            });
    }
    
    /**
     * Add new village
     */
    public void addVillage(final Village village, final OnVillageAddedListener listener) {
        if (db == null) {
            listener.onError("Firestore not initialized");
            return;
        }

        // Invalidate cache since we're adding new data
        invalidateVillagesCache();

        String villageId = village.getName().trim();
        db.collection(VILLAGES_COLLECTION)
            .document(villageId)
            .set(village)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Village synced with server: " + villageId);
                // Cleanup legacy villages with different IDs but same name
                cleanupLegacyVillages(village.getName(), villageId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Server sync failed for village: " + villageId, e);
            });

        // Trigger success IMMEDIATELY for the UI (Optimistic writing)
        Log.d(TAG, "Village queued locally: " + villageId);
        listener.onSuccess();
    }
    
    /**
     * Delete village
     */
    public void deleteVillage(final String villageName, final OnVillageDeletedListener listener) {
        if (db == null) {
            listener.onError("Firestore not initialized");
            return;
        }

        // Invalidate cache since we're deleting data
        invalidateVillagesCache();

        String villageId = villageName.trim();
        db.collection(VILLAGES_COLLECTION)
            .document(villageId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Village deleted successfully: " + villageId);
                listener.onSuccess();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error deleting village: " + villageId, e);
                listener.onError(e.getMessage());
            });
    }

    /**
     * Invalidate villages cache - call when village data is modified
     */
    public void invalidateVillagesCache() {
        cachedVillages = null;
        villagesCacheTime = 0;
        Log.d(TAG, "Villages cache invalidated");
    }
    
    // ============================================
    // PRICING OPERATIONS
    // ============================================
    
    /**
     * Get all pricing records (with caching)
     */
    public void getAllPricing(final OnPricingLoadedListener listener) {
        getAllPricing(listener, false);
    }

    /**
     * Get all pricing records with option to force refresh
     */
    public void getAllPricing(final OnPricingLoadedListener listener, boolean forceRefresh) {
        if (db == null) {
            Log.w(TAG, "Firestore not initialized. Returning empty list.");
            listener.onPricingLoaded(new ArrayList<>());
            return;
        }

        // Return cached data if valid and not forcing refresh
        if (!forceRefresh && cachedPricing != null &&
            System.currentTimeMillis() - pricingCacheTime < CACHE_VALIDITY_MS) {
            Log.d(TAG, "Returning " + cachedPricing.size() + " pricing from cache");
            listener.onPricingLoaded(new ArrayList<>(cachedPricing));
            return;
        }

        db.collection(PRICING_COLLECTION)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                Map<String, Pricing> simplifiedMap = new HashMap<>();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    Pricing pricing = doc.toObject(Pricing.class);
                    if (pricing != null) {
                        String key = pricing.getPricingKey();
                        // Prioritize standardized IDs (e.g., "Mix_100gm") over random ones
                        if (!simplifiedMap.containsKey(key) || doc.getId().equals(key)) {
                            simplifiedMap.put(key, pricing);
                        }
                    }
                }
                List<Pricing> pricingList = new ArrayList<>(simplifiedMap.values());
                // Sort pricing for consistent UI display (Mix first, then by package size)
                Collections.sort(pricingList, (p1, p2) -> p1.getPricingKey().compareToIgnoreCase(p2.getPricingKey()));
                // Update cache
                cachedPricing = new ArrayList<>(pricingList);
                pricingCacheTime = System.currentTimeMillis();
                Log.d(TAG, "Loaded and cached " + pricingList.size() + " unique pricing records");
                listener.onPricingLoaded(pricingList);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading pricing", e);
                // Return cached data if available on error
                if (cachedPricing != null) {
                    Log.d(TAG, "Returning cached pricing on error");
                    listener.onPricingLoaded(new ArrayList<>(cachedPricing));
                } else {
                    listener.onError(e.getMessage());
                }
            });
    }
    
    /**
     * Update pricing - now supports tea_type (Mix/Barik)
     */
    public void updatePricing(final Pricing pricing, final OnPricingUpdatedListener listener) {
        if (db == null) {
            listener.onError("Firestore not initialized");
            return;
        }

        String pricingId = pricing.getPricingKey();
        pricing.setUpdated_on(new Date());
        
        Log.d(TAG, "Enforcing standardized pricing ID: " + pricingId);

        // 1. Instantly write to the standardized ID (Local cache first)
        db.collection(PRICING_COLLECTION)
            .document(pricingId)
            .set(pricing)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Pricing synced with server: " + pricingId);
                // 2. Cleanup: find and delete any legacy documents in background
                cleanupLegacyPricing(pricing.getTea_type(), pricing.getPackage(), pricingId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Server sync failed for pricing: " + pricingId, e);
            });
            
        // Invalidate cache since we're updating data
        invalidatePricingCache();

        // 3. Trigger success IMMEDIATELY for the UI
        Log.d(TAG, "Pricing queued locally: " + pricingId);
        listener.onSuccess();
    }

    /**
     * Invalidate pricing cache - call when pricing data is modified
     */
    public void invalidatePricingCache() {
        cachedPricing = null;
        pricingCacheTime = 0;
        Log.d(TAG, "Pricing cache invalidated");
    }

    /**
     * Finds and deletes documents with non-standard IDs for a given tea type and package.
     */
    private void cleanupLegacyPricing(String teaType, String packageName, String standardizedId) {
        db.collection(PRICING_COLLECTION)
            .whereEqualTo("tea_type", teaType)
            .whereEqualTo("package", packageName)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    if (!doc.getId().equals(standardizedId)) {
                        Log.i(TAG, "Deleting legacy pricing doc: " + doc.getId());
                        db.collection(PRICING_COLLECTION).document(doc.getId()).delete();
                    }
                }
            });
    }

    /**
     * Get pricing by tea type
     */
    public void getPricingByTeaType(final String teaType, final OnPricingLoadedListener listener) {
        if (db == null) {
            listener.onPricingLoaded(new ArrayList<>());
            return;
        }

        db.collection(PRICING_COLLECTION)
            .whereEqualTo("tea_type", teaType)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Pricing> pricingList = new ArrayList<>();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    Pricing pricing = doc.toObject(Pricing.class);
                    if (pricing != null) {
                        pricingList.add(pricing);
                    }
                }
                listener.onPricingLoaded(pricingList);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading pricing by tea type", e);
                listener.onError(e.getMessage());
            });
    }

    // ============================================
    // CUSTOMER OPERATIONS
    // ============================================

    /**
     * Get all customers (with caching)
     */
    public void getAllCustomers(final OnCustomersLoadedListener listener) {
        getAllCustomers(listener, false);
    }

    /**
     * Get all customers with option to force refresh
     */
    public void getAllCustomers(final OnCustomersLoadedListener listener, boolean forceRefresh) {
        if (db == null) {
            listener.onCustomersLoaded(new ArrayList<>());
            return;
        }

        // Return cached data if valid and not forcing refresh
        if (!forceRefresh && cachedCustomers != null &&
            System.currentTimeMillis() - customersCacheTime < CACHE_VALIDITY_MS) {
            Log.d(TAG, "Returning " + cachedCustomers.size() + " customers from cache");
            listener.onCustomersLoaded(new ArrayList<>(cachedCustomers));
            return;
        }

        db.collection(CUSTOMERS_COLLECTION)
            .orderBy("customer_name", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Customer> customers = new ArrayList<>();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    Customer customer = doc.toObject(Customer.class);
                    if (customer != null) {
                        customers.add(customer);
                    }
                }
                // Update cache
                cachedCustomers = new ArrayList<>(customers);
                customersCacheTime = System.currentTimeMillis();
                Log.d(TAG, "Loaded and cached " + customers.size() + " customers");
                listener.onCustomersLoaded(customers);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading customers", e);
                // Return cached data if available on error
                if (cachedCustomers != null) {
                    Log.d(TAG, "Returning cached customers on error");
                    listener.onCustomersLoaded(new ArrayList<>(cachedCustomers));
                } else {
                    listener.onError(e.getMessage());
                }
            });
    }

    /**
     * Get customers by village
     */
    public void getCustomersByVillage(final String village, final OnCustomersLoadedListener listener) {
        if (db == null) {
            listener.onCustomersLoaded(new ArrayList<>());
            return;
        }

        db.collection(CUSTOMERS_COLLECTION)
            .whereEqualTo("village", village)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Customer> customers = new ArrayList<>();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    Customer customer = doc.toObject(Customer.class);
                    if (customer != null) {
                        customers.add(customer);
                    }
                }
                
                // Sort in app to avoid Firestore index requirement
                Collections.sort(customers, (c1, c2) -> 
                    c1.getCustomer_name().compareToIgnoreCase(c2.getCustomer_name()));
                
                Log.d(TAG, "Loaded " + customers.size() + " customers for village: " + village);
                listener.onCustomersLoaded(customers);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading customers by village", e);
                listener.onError(e.getMessage());
            });
    }

    /**
     * Add new customer (called when a sale is made with a new customer)
     */
    public void addCustomer(final Customer customer, final OnCustomerAddedListener listener) {
        if (db == null) {
            if (listener != null) listener.onError("Firestore not initialized");
            return;
        }

        // Invalidate cache since we're adding new data
        invalidateCustomersCache();

        // Use a predictable ID for better offline support
        String customerId = (customer.getVillage() + "_" + customer.getCustomer_name())
                .replaceAll("[^a-zA-Z0-9_]", "_");

        db.collection(CUSTOMERS_COLLECTION)
            .document(customerId)
            .set(customer)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Customer synced with server: " + customer.getCustomer_name());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Server sync failed for customer: " + customer.getCustomer_name(), e);
            });

        // Trigger success IMMEDIATELY for the UI
        if (listener != null) listener.onSuccess();
    }

    /**
     * Invalidate customers cache - call when customer data is modified
     */
    public void invalidateCustomersCache() {
        cachedCustomers = null;
        customersCacheTime = 0;
        Log.d(TAG, "Customers cache invalidated");
    }

    // ============================================
    // NOTE OPERATIONS
    // ============================================

    public void getNotesPaginated(int limit, DocumentSnapshot lastDocument, final OnNotesLoadedListener listener) {
        getNotesPaginated(limit, lastDocument, listener, false);
    }

    public void getNotesPaginated(int limit, DocumentSnapshot lastDocument, final OnNotesLoadedListener listener, boolean forceRefresh) {
        if (db == null) {
            listener.onNotesLoaded(new ArrayList<>());
            return;
        }

        if (lastDocument == null && !forceRefresh && cachedNotes != null &&
            System.currentTimeMillis() - notesCacheTime < CACHE_VALIDITY_MS) {
            Log.d(TAG, "Returning first page of notes from in-memory cache");
            if (listener instanceof OnNotesPaginatedListener) {
                ((OnNotesPaginatedListener) listener).onNotesLoaded(new ArrayList<>(cachedNotes), null);
            } else {
                listener.onNotesLoaded(new ArrayList<>(cachedNotes));
            }
            return;
        }

        Query query = db.collection(NOTES_COLLECTION)
                .orderBy("updated_at", Query.Direction.DESCENDING)
                .limit(limit);

        if (lastDocument != null) {
            query = query.startAfter(lastDocument);
        }

        query.get().addOnSuccessListener(querySnapshot -> {
            List<Note> notes = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Note note = doc.toObject(Note.class);
                if (note != null) {
                    notes.add(note);
                }
            }

            if (lastDocument == null) {
                cachedNotes = new ArrayList<>(notes);
                notesCacheTime = System.currentTimeMillis();
                Log.d(TAG, "Updated first-page notes cache");
            }

            if (listener instanceof OnNotesPaginatedListener) {
                DocumentSnapshot nextLast = null;
                if (!querySnapshot.isEmpty()) {
                    nextLast = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
                }
                ((OnNotesPaginatedListener) listener).onNotesLoaded(notes, nextLast);
            } else {
                listener.onNotesLoaded(notes);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading paginated notes", e);
            listener.onError(e.getMessage());
        });
    }

    public void getNoteById(String noteId, final OnNoteLoadedListener listener) {
        if (db == null || noteId == null) {
            listener.onError("Invalid ID or database not initialized");
            return;
        }

        // Check LRU note cache
        synchronized (noteCache) {
            if (noteCache.containsKey(noteId)) {
                Long fetchTime = noteCacheTimes.get(noteId);
                if (fetchTime != null && System.currentTimeMillis() - fetchTime < CACHE_VALIDITY_MS) {
                    Log.d(TAG, "Returning note from 5-item LRU cache: " + noteId);
                    listener.onNoteLoaded(noteCache.get(noteId));
                    return;
                } else {
                    noteCache.remove(noteId);
                    noteCacheTimes.remove(noteId);
                }
            }
        }

        db.collection(NOTES_COLLECTION)
                .document(noteId)
                .get()
                .addOnSuccessListener(doc -> {
                    Note note = doc.toObject(Note.class);
                    if (note != null) {
                        // Update LRU cache
                        synchronized (noteCache) {
                            noteCache.put(noteId, note);
                            noteCacheTimes.put(noteId, System.currentTimeMillis());
                        }
                        listener.onNoteLoaded(note);
                    } else {
                        listener.onError("Note not found");
                    }
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public void addNote(final Note note, final OnNoteAddedListener listener) {
        if (db == null) {
            if (listener != null) listener.onError("Firestore not initialized");
            return;
        }

        invalidateNotesCache();
        note.setUpdated_at(new Date());

        db.collection(NOTES_COLLECTION)
            .document(note.getNote_id())
            .set(note)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Note synced with server: " + note.getNote_id());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Server sync failed for note: " + note.getNote_id(), e);
            });

        if (listener != null) listener.onSuccess();
    }

    public void deleteNote(final String noteId, final OnNoteDeletedListener listener) {
        if (db == null) {
            if (listener != null) listener.onError("Firestore not initialized");
            return;
        }

        invalidateNotesCache();

        db.collection(NOTES_COLLECTION)
            .document(noteId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Note deleted from server: " + noteId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Server deletion failed for note: " + noteId, e);
            });

        if (listener != null) listener.onSuccess();
    }

    public void invalidateNotesCache() {
        cachedNotes = null;
        notesCacheTime = 0;
        synchronized (noteCache) {
            noteCache.clear();
            noteCacheTimes.clear();
        }
        Log.d(TAG, "Notes and LRU cache invalidated");
    }

    /**
     * Invalidate all caches - useful for force refresh scenarios
     */
    public void invalidateAllCaches() {
        invalidateSalesCache();
        invalidateVillagesCache();
        invalidatePricingCache();
        invalidateCustomersCache();
        invalidateNotesCache();
        Log.d(TAG, "All caches invalidated");
    }
    
    // ============================================
    // LISTENER INTERFACES
    // ============================================
    
    public interface OnInitializeListener {
        void onSuccess();
        void onError(String error);
    }
    
    public interface OnSalesLoadedListener {
        void onSalesLoaded(List<Sale> sales);
        void onError(String error);
    }
    
    public interface OnSaleAddedListener {
        void onSuccess();
        void onError(String error);
    }
    
    public interface OnSaleUpdatedListener {
        void onSuccess();
        void onError(String error);
    }
    
    public interface OnSaleDeletedListener {
        void onSuccess();
        void onError(String error);
    }
    
    public interface OnVillagesLoadedListener {
        void onVillagesLoaded(List<Village> villages);
        void onError(String error);
    }
    
    public interface OnVillageAddedListener {
        void onSuccess();
        void onError(String error);
    }
    
    public interface OnVillageDeletedListener {
        void onSuccess();
        void onError(String error);
    }
    
    public interface OnPricingLoadedListener {
        void onPricingLoaded(List<Pricing> pricingList);
        void onError(String error);
    }
    
    public interface OnPricingUpdatedListener {
        void onSuccess();
        void onError(String error);
    }

    public interface OnCustomersLoadedListener {
        void onCustomersLoaded(List<Customer> customers);
        void onError(String error);
    }

    public interface OnCustomerAddedListener {
        void onSuccess();
        void onError(String error);
    }

    public interface OnNotesLoadedListener {
        void onNotesLoaded(List<Note> notes);
        void onError(String error);
    }

    public interface OnNoteAddedListener {
        void onSuccess();
        void onError(String error);
    }

    public interface OnNoteDeletedListener {
        void onSuccess();
        void onError(String error);
    }

    public interface OnNoteLoadedListener {
        void onNoteLoaded(Note note);
        void onError(String error);
    }

    public interface OnSalesPaginatedListener extends OnSalesLoadedListener {
        void onSalesLoaded(List<Sale> sales, DocumentSnapshot lastVisible);
    }

    public interface OnNotesPaginatedListener extends OnNotesLoadedListener {
        void onNotesLoaded(List<Note> notes, DocumentSnapshot lastVisible);
    }

    /**
     * Finds and deletes legacy villages with non-standard IDs.
     */
    private void cleanupLegacyVillages(String villageName, String standardizedId) {
        db.collection(VILLAGES_COLLECTION)
            .whereEqualTo("name", villageName)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    if (!doc.getId().equals(standardizedId)) {
                        Log.i(TAG, "Deleting legacy village doc: " + doc.getId());
                        db.collection(VILLAGES_COLLECTION).document(doc.getId()).delete();
                    }
                }
            });
    }
}
