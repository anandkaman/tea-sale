package com.goldtea.sales.ui.settings;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.goldtea.sales.R;
import com.goldtea.sales.data.model.Pricing;
import com.goldtea.sales.data.model.Village;
import com.goldtea.sales.data.firestore.FirestoreManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "GoldTeaSalesPrefs";
    private static final String KEY_DARK_MODE = "dark_mode";

    // Mix tea pricing fields
    private TextInputEditText mixPrice100gmEditText, mixPrice250gmEditText, mixPrice500gmEditText, mixPrice1kgEditText;
    // Barik tea pricing fields
    private TextInputEditText barikPrice100gmEditText, barikPrice250gmEditText, barikPrice500gmEditText, barikPrice1kgEditText;
    private RecyclerView villagesRecyclerView;
    private MaterialButton savePricingButton;
    private View connectionStatusIndicator;
    private TextView connectionStatusText;
    private MaterialSwitch darkModeSwitch;

    private FirestoreManager firestoreManager;
    private VillageAdapter villageAdapter;
    private Map<String, String> priceMap;
    private SharedPreferences sharedPreferences;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        initializeViews(view);
        setupRecyclerViews();
        setupListeners(view);
        loadData();
        
        return view;
    }
    
    private void initializeViews(View view) {
        if (getContext() == null) return;
        firestoreManager = FirestoreManager.getInstance(getContext());
        priceMap = new HashMap<>();
        sharedPreferences = getContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);

        // Mix tea pricing
        mixPrice100gmEditText = view.findViewById(R.id.mixPrice100gmEditText);
        mixPrice250gmEditText = view.findViewById(R.id.mixPrice250gmEditText);
        mixPrice500gmEditText = view.findViewById(R.id.mixPrice500gmEditText);
        mixPrice1kgEditText = view.findViewById(R.id.mixPrice1kgEditText);
        // Barik tea pricing
        barikPrice100gmEditText = view.findViewById(R.id.barikPrice100gmEditText);
        barikPrice250gmEditText = view.findViewById(R.id.barikPrice250gmEditText);
        barikPrice500gmEditText = view.findViewById(R.id.barikPrice500gmEditText);
        barikPrice1kgEditText = view.findViewById(R.id.barikPrice1kgEditText);
        villagesRecyclerView = view.findViewById(R.id.villagesRecyclerView);
        savePricingButton = view.findViewById(R.id.savePricingButton);
        connectionStatusIndicator = view.findViewById(R.id.connectionStatusIndicator);
        connectionStatusText = view.findViewById(R.id.connectionStatusText);
        darkModeSwitch = view.findViewById(R.id.darkModeSwitch);

        // Set up dark mode switch
        setupDarkModeSwitch();

        checkConnectionStatus();
    }

    private void setupDarkModeSwitch() {
        // Load current setting
        boolean isDarkMode = sharedPreferences.getBoolean(KEY_DARK_MODE, false);
        darkModeSwitch.setChecked(isDarkMode);

        // Set listener
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save preference
            sharedPreferences.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();

            // Apply theme
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
    }
    
    private void setupRecyclerViews() {
        if (getContext() == null) return;
        villageAdapter = new VillageAdapter(new VillageAdapter.OnVillageActionListener() {
            @Override
            public void onDeleteClick(Village village) {
                deleteVillage(village);
            }
        });
        villagesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        villagesRecyclerView.setAdapter(villageAdapter);
    }
    
    private void setupListeners(View view) {
        // Add Village
        view.findViewById(R.id.addVillageButton).setOnClickListener(v -> showAddVillageDialog());
        
        // Save Pricing
        savePricingButton.setOnClickListener(v -> savePricing());
    }
    
    private void loadData() {
        loadVillages();
        loadPricing();
    }
    
    private void loadVillages() {
        firestoreManager.getAllVillages(new FirestoreManager.OnVillagesLoadedListener() {
            @Override
            public void onVillagesLoaded(List<Village> villages) {
                if (!isAdded()) return;
                villageAdapter.setVillages(villages);
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                // Silently fail for offline - data may load from cache
            }
        });
    }

    private void loadPricing() {
        firestoreManager.getAllPricing(new FirestoreManager.OnPricingLoadedListener() {
            @Override
            public void onPricingLoaded(List<Pricing> pricingList) {
                if (!isAdded()) return;
                for (Pricing pricing : pricingList) {
                    // Use pricing key: tea_type + "_" + package (e.g., "Mix_100gm")
                    String key = pricing.getPricingKey();
                    priceMap.put(key, String.valueOf(pricing.getRate()));
                }
                updatePriceFields();
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                // Silently fail for offline - data may load from cache
            }
        });
    }

    private void updatePriceFields() {
        if (!isAdded()) return;
        // Mix tea pricing
        mixPrice100gmEditText.setText(priceMap.getOrDefault("Mix_100gm", ""));
        mixPrice250gmEditText.setText(priceMap.getOrDefault("Mix_250gm", ""));
        mixPrice500gmEditText.setText(priceMap.getOrDefault("Mix_500gm", ""));
        mixPrice1kgEditText.setText(priceMap.getOrDefault("Mix_1kg", ""));
        // Barik tea pricing
        barikPrice100gmEditText.setText(priceMap.getOrDefault("Barik_100gm", ""));
        barikPrice250gmEditText.setText(priceMap.getOrDefault("Barik_250gm", ""));
        barikPrice500gmEditText.setText(priceMap.getOrDefault("Barik_500gm", ""));
        barikPrice1kgEditText.setText(priceMap.getOrDefault("Barik_1kg", ""));
    }

    private void savePricing() {
        if (!isAdded() || getContext() == null) return;

        String[] packages = {"100gm", "250gm", "500gm", "1kg"};
        String[] teaTypes = {"Mix", "Barik"};
        TextInputEditText[] mixEditTexts = {mixPrice100gmEditText, mixPrice250gmEditText, mixPrice500gmEditText, mixPrice1kgEditText};
        TextInputEditText[] barikEditTexts = {barikPrice100gmEditText, barikPrice250gmEditText, barikPrice500gmEditText, barikPrice1kgEditText};
        TextInputEditText[][] allEditTexts = {mixEditTexts, barikEditTexts};

        for (int t = 0; t < teaTypes.length; t++) {
            String teaType = teaTypes[t];
            TextInputEditText[] editTexts = allEditTexts[t];

            for (int i = 0; i < packages.length; i++) {
                String priceStr = editTexts[i].getText().toString().trim();
                if (!priceStr.isEmpty()) {
                    try {
                        int price = Integer.parseInt(priceStr);
                        Pricing pricing = new Pricing();
                        pricing.setPackage(packages[i]);
                        pricing.setTea_type(teaType);
                        pricing.setRate(price);

                        firestoreManager.updatePricing(pricing, new FirestoreManager.OnPricingUpdatedListener() {
                            @Override
                            public void onSuccess() {
                                // Background update and cleanup handled by FirestoreManager
                            }

                            @Override
                            public void onError(String error) {
                                // Silently fail - will retry when online
                            }
                        });
                    } catch (NumberFormatException e) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Invalid price for " + teaType + " " + packages[i], Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                }
            }
        }

        if (getContext() != null) {
            Toast.makeText(getContext(), "Pricing saved!", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showAddVillageDialog() {
        if (!isAdded() || getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add Village");

        final EditText input = new EditText(getContext());
        input.setHint("Village Name");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String villageName = input.getText().toString().trim();
            if (!villageName.isEmpty()) {
                addVillage(villageName);
            } else if (getContext() != null) {
                Toast.makeText(getContext(), "Please enter village name", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void addVillage(String villageName) {
        Village village = new Village();
        village.setName(villageName);

        firestoreManager.addVillage(village, new FirestoreManager.OnVillageAddedListener() {
            @Override
            public void onSuccess() {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Village added!", Toast.LENGTH_SHORT).show();
                loadVillages();
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteVillage(Village village) {
        if (!isAdded() || getContext() == null) return;

        new AlertDialog.Builder(getContext())
            .setTitle("Delete Village")
            .setMessage("Are you sure you want to delete " + village.getName() + "?")
            .setPositiveButton("Delete", (dialog, which) -> {
                firestoreManager.deleteVillage(village.getName(), new FirestoreManager.OnVillageDeletedListener() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded() || getContext() == null) return;
                        Toast.makeText(getContext(), "Village deleted", Toast.LENGTH_SHORT).show();
                        loadVillages();
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded() || getContext() == null) return;
                        Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void checkConnectionStatus() {
        firestoreManager.setOnConnectionStatusChangedListener(isOnline -> {
            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> updateConnectionStatus(isOnline));
            }
        });
    }

    private void updateConnectionStatus(boolean isOnline) {
        if (!isAdded()) return;
        if (connectionStatusIndicator != null && connectionStatusText != null) {
            if (isOnline) {
                connectionStatusIndicator.setBackgroundResource(R.drawable.status_indicator_online);
                connectionStatusText.setText("Online");
            } else {
                connectionStatusIndicator.setBackgroundResource(R.drawable.status_indicator_offline);
                connectionStatusText.setText("Offline (changes will sync later)");
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove listener to prevent memory leaks and callbacks after fragment is destroyed
        firestoreManager.setOnConnectionStatusChangedListener(null);
    }
}
