package com.goldtea.sales.ui.newsale;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.goldtea.sales.R;
import com.goldtea.sales.data.model.Customer;
import com.goldtea.sales.data.model.Pricing;
import com.goldtea.sales.data.model.Sale;
import com.goldtea.sales.data.model.Village;
import com.goldtea.sales.data.firestore.FirestoreManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NewSaleFragment extends Fragment {

    private TextInputEditText dateEditText, brandEditText, rateEditText;
    private TextInputEditText quantityEditText, totalEditText, amountPaidEditText, balanceEditText;
    private AutoCompleteTextView villageAutoComplete, customerAutoComplete;
    private AutoCompleteTextView teaTypeAutoComplete, packagingAutoComplete;
    private RadioGroup paymentStatusRadioGroup;
    private TextInputLayout amountPaidInputLayout, balanceInputLayout;
    private MaterialButton incrementButton, decrementButton;

    // Fixed dropdown values
    private static final String[] TEA_TYPES = {"Mix", "Barik"};
    private static final String[] PACKAGING_OPTIONS = {"100gm", "250gm", "500gm", "1kg"};

    private FirestoreManager firestoreManager;
    private Calendar selectedDate;
    private SimpleDateFormat dateFormat;

    private List<Village> villages;
    private List<Customer> allCustomers;
    private String currentVillage;
    // Pricing map: key = "teaType_package" (e.g., "Mix_100gm"), value = rate
    private Map<String, Integer> pricingMap;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_new_sale, container, false);

        initializeViews(view);
        setupListeners(view);
        loadData();
        setDefaultValues();

        return view;
    }

    private void initializeViews(View view) {
        if (getContext() == null) return;
        firestoreManager = FirestoreManager.getInstance(getContext());
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        selectedDate = Calendar.getInstance();

        dateEditText = view.findViewById(R.id.dateEditText);
        villageAutoComplete = view.findViewById(R.id.villageAutoComplete);
        customerAutoComplete = view.findViewById(R.id.customerAutoComplete);
        brandEditText = view.findViewById(R.id.brandEditText);
        teaTypeAutoComplete = view.findViewById(R.id.teaTypeAutoComplete);
        packagingAutoComplete = view.findViewById(R.id.packagingAutoComplete);
        rateEditText = view.findViewById(R.id.rateEditText);
        quantityEditText = view.findViewById(R.id.quantityEditText);
        totalEditText = view.findViewById(R.id.totalEditText);
        paymentStatusRadioGroup = view.findViewById(R.id.paymentStatusRadioGroup);
        amountPaidInputLayout = view.findViewById(R.id.amountPaidInputLayout);
        amountPaidEditText = view.findViewById(R.id.amountPaidEditText);
        balanceInputLayout = view.findViewById(R.id.balanceInputLayout);
        balanceEditText = view.findViewById(R.id.balanceEditText);
        incrementButton = view.findViewById(R.id.incrementButton);
        decrementButton = view.findViewById(R.id.decrementButton);

        villages = new ArrayList<>();
        allCustomers = new ArrayList<>();
        pricingMap = new HashMap<>();

        // Set threshold for autocomplete to show suggestions after 1 character
        customerAutoComplete.setThreshold(1);
    }

    private void setupListeners(View view) {
        // Date picker
        dateEditText.setOnClickListener(v -> showDatePicker());

        // Village selection - load customers for that village
        villageAutoComplete.setOnItemClickListener((parent, v, position, id) -> {
            String selectedVillage = (String) parent.getItemAtPosition(position);
            currentVillage = selectedVillage;
            loadCustomersForVillage(selectedVillage);
        });

        // Tea type selection - update pricing
        teaTypeAutoComplete.setOnItemClickListener((parent, v, position, id) -> {
            updateRateBasedOnSelection();
        });

        // Packaging selection - update pricing based on tea type + package
        packagingAutoComplete.setOnItemClickListener((parent, v, position, id) -> {
            updateRateBasedOnSelection();
        });

        // Quantity increment/decrement
        incrementButton.setOnClickListener(v -> incrementQuantity());
        decrementButton.setOnClickListener(v -> decrementQuantity());

        // Quantity change - recalculate total
        quantityEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateTotal();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Rate change - recalculate total
        rateEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateTotal();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Payment status change
        paymentStatusRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioPending) {
                amountPaidInputLayout.setVisibility(View.VISIBLE);
                balanceInputLayout.setVisibility(View.VISIBLE);
            } else {
                amountPaidInputLayout.setVisibility(View.GONE);
                balanceInputLayout.setVisibility(View.GONE);
                amountPaidEditText.setText("");
                balanceEditText.setText("");
            }
        });

        // Amount paid change - recalculate balance
        amountPaidEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateBalance();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Save button
        view.findViewById(R.id.saveSaleButton).setOnClickListener(v -> saveSale());

        // Clear button
        view.findViewById(R.id.clearButton).setOnClickListener(v -> clearForm());
    }

    private void loadData() {
        // Load villages
        firestoreManager.getAllVillages(new FirestoreManager.OnVillagesLoadedListener() {
            @Override
            public void onVillagesLoaded(List<Village> loadedVillages) {
                if (!isAdded() || getContext() == null) return;
                villages = loadedVillages;
                List<String> villageNames = new ArrayList<>();
                for (Village village : villages) {
                    villageNames.add(village.getName());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_dropdown_item_1line, villageNames);
                villageAutoComplete.setAdapter(adapter);
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                Log.e("NewSaleFragment", "Error loading villages: " + error);
            }
        });

        // Load all pricing (with tea types)
        firestoreManager.getAllPricing(new FirestoreManager.OnPricingLoadedListener() {
            @Override
            public void onPricingLoaded(List<Pricing> pricingList) {
                if (!isAdded() || getContext() == null) return;
                pricingMap.clear();
                for (Pricing pricing : pricingList) {
                    // Key format: "TeaType_Package" (e.g., "Mix_100gm")
                    String teaType = pricing.getTea_type() != null ? pricing.getTea_type() : "Mix";
                    String key = teaType + "_" + pricing.getPackage();
                    pricingMap.put(key, pricing.getRate());
                }

                // Setup packaging dropdown
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_dropdown_item_1line, PACKAGING_OPTIONS);
                packagingAutoComplete.setAdapter(adapter);
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                Log.e("NewSaleFragment", "Error loading pricing: " + error);
            }
        });

        // Load all customers for autocomplete
        firestoreManager.getAllCustomers(new FirestoreManager.OnCustomersLoadedListener() {
            @Override
            public void onCustomersLoaded(List<Customer> customers) {
                if (!isAdded()) return;
                allCustomers = customers;
            }

            @Override
            public void onError(String error) {
                // Ignore - customers will be loaded when village is selected
            }
        });

        // Setup tea type dropdown with fixed values
        if (isAdded() && getContext() != null) {
            ArrayAdapter<String> teaTypeAdapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_dropdown_item_1line, TEA_TYPES);
            teaTypeAutoComplete.setAdapter(teaTypeAdapter);
        }
    }

    private void loadCustomersForVillage(String village) {
        firestoreManager.getCustomersByVillage(village, new FirestoreManager.OnCustomersLoadedListener() {
            @Override
            public void onCustomersLoaded(List<Customer> customers) {
                if (!isAdded() || getContext() == null) return;

                // Extract customer names
                List<String> customerNames = new ArrayList<>();
                for (Customer customer : customers) {
                    customerNames.add(customer.getCustomer_name());
                }

                Log.d("NewSaleFragment", "Loaded " + customerNames.size() + " customers for village: " + village);

                // Use simple ArrayAdapter
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        getContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        customerNames);
                customerAutoComplete.setAdapter(adapter);
                customerAutoComplete.setThreshold(1); // Show suggestions after 1 character
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                Log.e("NewSaleFragment", "Error loading customers: " + error);
                // Setup empty adapter, user can still type new name
                customerAutoComplete.setAdapter(new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_dropdown_item_1line, new ArrayList<>()));
            }
        });
    }

    private void updateRateBasedOnSelection() {
        String teaType = teaTypeAutoComplete.getText().toString().trim();
        String packaging = packagingAutoComplete.getText().toString().trim();

        if (!teaType.isEmpty() && !packaging.isEmpty()) {
            String key = teaType + "_" + packaging;
            Integer rate = pricingMap.get(key);

            if (rate != null) {
                rateEditText.setText(String.valueOf(rate));
            } else {
                // Fallback to generic pricing without tea type
                Integer genericRate = pricingMap.get("Mix_" + packaging);
                if (genericRate != null) {
                    rateEditText.setText(String.valueOf(genericRate));
                }
            }
            calculateTotal();
        }
    }

    private void incrementQuantity() {
        String currentQty = quantityEditText.getText().toString();
        int qty = currentQty.isEmpty() ? 0 : Integer.parseInt(currentQty);
        qty++;
        quantityEditText.setText(String.valueOf(qty));
    }

    private void decrementQuantity() {
        String currentQty = quantityEditText.getText().toString();
        int qty = currentQty.isEmpty() ? 1 : Integer.parseInt(currentQty);
        if (qty > 1) {
            qty--;
            quantityEditText.setText(String.valueOf(qty));
        }
    }

    private void setDefaultValues() {
        // Set today's date
        dateEditText.setText(dateFormat.format(selectedDate.getTime()));

        // Set default brand
        brandEditText.setText("GOLD");

        // Set default quantity
        quantityEditText.setText("1");
    }

    private void showDatePicker() {
        if (!isAdded() || getContext() == null) return;
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    dateEditText.setText(dateFormat.format(selectedDate.getTime()));
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void calculateTotal() {
        try {
            String rateStr = rateEditText.getText().toString();
            String quantityStr = quantityEditText.getText().toString();

            if (!rateStr.isEmpty() && !quantityStr.isEmpty()) {
                double rate = Double.parseDouble(rateStr);
                double quantity = Double.parseDouble(quantityStr);
                double total = rate * quantity;
                totalEditText.setText(String.format(Locale.getDefault(), "%.2f", total));
                calculateBalance();
            }
        } catch (NumberFormatException e) {
            // Invalid input, ignore
        }
    }

    private void calculateBalance() {
        try {
            String totalStr = totalEditText.getText().toString();
            String paidStr = amountPaidEditText.getText().toString();

            if (!totalStr.isEmpty() && !paidStr.isEmpty()) {
                double total = Double.parseDouble(totalStr);
                double paid = Double.parseDouble(paidStr);
                double balance = total - paid;
                balanceEditText.setText(String.format(Locale.getDefault(), "%.2f", balance));
            }
        } catch (NumberFormatException e) {
            // Invalid input, ignore
        }
    }

    private boolean validateForm() {
        if (!isAdded()) return false;
        boolean isValid = true;

        if (villageAutoComplete.getText().toString().trim().isEmpty()) {
            villageAutoComplete.setError("Village is required");
            isValid = false;
        }

        if (customerAutoComplete.getText().toString().trim().isEmpty()) {
            customerAutoComplete.setError("Customer name is required");
            isValid = false;
        }

        if (teaTypeAutoComplete.getText().toString().trim().isEmpty()) {
            teaTypeAutoComplete.setError("Tea type is required");
            isValid = false;
        }

        if (packagingAutoComplete.getText().toString().trim().isEmpty()) {
            packagingAutoComplete.setError("Packaging is required");
            isValid = false;
        }

        if (quantityEditText.getText().toString().trim().isEmpty()) {
            quantityEditText.setError("Quantity is required");
            isValid = false;
        } else {
            try {
                double quantity = Double.parseDouble(quantityEditText.getText().toString());
                if (quantity <= 0) {
                    quantityEditText.setError("Quantity must be greater than 0");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                quantityEditText.setError("Invalid quantity");
                isValid = false;
            }
        }

        // Validate amount paid if pending
        if (paymentStatusRadioGroup.getCheckedRadioButtonId() == R.id.radioPending) {
            String paidStr = amountPaidEditText.getText().toString().trim();
            if (!paidStr.isEmpty()) {
                try {
                    double paid = Double.parseDouble(paidStr);
                    double total = Double.parseDouble(totalEditText.getText().toString());
                    if (paid > total) {
                        amountPaidEditText.setError("Amount paid cannot exceed total");
                        isValid = false;
                    }
                } catch (NumberFormatException e) {
                    amountPaidEditText.setError("Invalid amount");
                    isValid = false;
                }
            }
        }

        return isValid;
    }

    private void saveSale() {
        if (!isAdded() || getContext() == null) return;

        if (!validateForm()) {
            Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Sale sale = new Sale();
            sale.setDate(selectedDate.getTime());
            sale.setDay(new SimpleDateFormat("EEEE", Locale.getDefault()).format(selectedDate.getTime()));
            sale.setVillage(villageAutoComplete.getText().toString().trim());
            sale.setCustomer_name(customerAutoComplete.getText().toString().trim());
            sale.setBrand(brandEditText.getText().toString().trim());
            sale.setTea_type(teaTypeAutoComplete.getText().toString().trim());
            sale.setPackaging(packagingAutoComplete.getText().toString().trim());
            sale.setRate(Double.parseDouble(rateEditText.getText().toString()));
            sale.setQuantity(Double.parseDouble(quantityEditText.getText().toString()));
            sale.setTotal_amount(Double.parseDouble(totalEditText.getText().toString()));

            boolean isPaid = paymentStatusRadioGroup.getCheckedRadioButtonId() == R.id.radioPaid;
            sale.setPayment_status(isPaid ? "Paid" : "Pending");

            if (isPaid) {
                sale.setAmount_paid(sale.getTotal_amount());
                sale.setBalance(0);
            } else {
                String paidStr = amountPaidEditText.getText().toString().trim();
                double paid = paidStr.isEmpty() ? 0 : Double.parseDouble(paidStr);
                sale.setAmount_paid(paid);
                sale.setBalance(sale.getTotal_amount() - paid);
            }

            // Save to database
            firestoreManager.addSale(sale, new FirestoreManager.OnSaleAddedListener() {
                @Override
                public void onSuccess() {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Sale saved successfully!", Toast.LENGTH_SHORT).show();
                        clearForm();
                    }
                }

                @Override
                public void onError(String error) {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Error saving sale: " + error, Toast.LENGTH_LONG).show();
                    }
                }
            });

        } catch (Exception e) {
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void saveCustomerIfNew(String village, String customerName) {
        Customer customer = new Customer();
        customer.setVillage(village);
        customer.setCustomer_name(customerName);
        customer.setAdded_on(new Date());

        // This will check if customer exists before adding
        firestoreManager.addCustomer(customer, null);
    }

    private void clearForm() {
        villageAutoComplete.setText("");
        customerAutoComplete.setText("");
        brandEditText.setText("GOLD");
        teaTypeAutoComplete.setText("");
        packagingAutoComplete.setText("");
        rateEditText.setText("");
        quantityEditText.setText("1");
        totalEditText.setText("");
        amountPaidEditText.setText("");
        balanceEditText.setText("");
        paymentStatusRadioGroup.check(R.id.radioPaid);
        selectedDate = Calendar.getInstance();
        dateEditText.setText(dateFormat.format(selectedDate.getTime()));
        currentVillage = null;
    }

    /**
     * Custom adapter for customer autocomplete that filters both village-specific
     * and all customers as user types
     */
    private static class CustomerAutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {
        private List<String> villageCustomers;
        private List<Customer> allCustomers;
        private String village;
        private List<String> filteredSuggestions;

        public CustomerAutoCompleteAdapter(android.content.Context context, List<String> villageCustomers,
                                           List<Customer> allCustomers, String village) {
            super(context, android.R.layout.simple_dropdown_item_1line);
            this.villageCustomers = villageCustomers;
            this.allCustomers = allCustomers;
            this.village = village;
            this.filteredSuggestions = new ArrayList<>(villageCustomers);
        }

        @Override
        public int getCount() {
            return filteredSuggestions.size();
        }

        @Override
        public String getItem(int position) {
            return filteredSuggestions.get(position);
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    List<String> suggestions = new ArrayList<>();

                    if (constraint != null && constraint.length() > 0) {
                        String filterPattern = constraint.toString().toLowerCase().trim();

                        // First add matching customers from current village
                        for (String name : villageCustomers) {
                            if (name.toLowerCase().contains(filterPattern)) {
                                suggestions.add(name);
                            }
                        }

                        // Then add matching customers from other villages (marked with village name)
                        for (Customer customer : allCustomers) {
                            if (!customer.getVillage().equals(village) &&
                                customer.getCustomer_name().toLowerCase().contains(filterPattern)) {
                                String displayName = customer.getCustomer_name() + " (" + customer.getVillage() + ")";
                                if (!suggestions.contains(displayName) && !suggestions.contains(customer.getCustomer_name())) {
                                    suggestions.add(displayName);
                                }
                            }
                        }
                    } else {
                        suggestions.addAll(villageCustomers);
                    }

                    results.values = suggestions;
                    results.count = suggestions.size();
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    filteredSuggestions.clear();
                    if (results.values != null) {
                        filteredSuggestions.addAll((List<String>) results.values);
                    }
                    notifyDataSetChanged();
                }
            };
        }
    }
}
