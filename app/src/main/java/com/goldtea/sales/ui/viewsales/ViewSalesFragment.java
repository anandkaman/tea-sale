package com.goldtea.sales.ui.viewsales;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.goldtea.sales.R;
import com.goldtea.sales.data.model.Sale;
import com.goldtea.sales.data.firestore.FirestoreManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.Locale;

public class ViewSalesFragment extends Fragment implements SalesAdapter.OnSaleClickListener {
    
    private RecyclerView salesRecyclerView;
    private SalesAdapter salesAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextInputEditText searchEditText;
    private ChipGroup filterChipGroup;
    private LinearLayout emptyStateLayout;
    
    private FirestoreManager FirestoreManager;
    private String currentPaymentFilter = "All";

    // Pagination variables
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private com.google.firebase.firestore.DocumentSnapshot lastVisibleSnapshot = null;
    private static final int PAGE_SIZE = 50;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_sales, container, false);
        
        initializeViews(view);
        setupRecyclerView();
        setupListeners();
        loadSales(false);
        
        return view;
    }
    
    private void initializeViews(View view) {
        if (getContext() == null) return;
        FirestoreManager = FirestoreManager.getInstance(getContext());

        salesRecyclerView = view.findViewById(R.id.salesRecyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        searchEditText = view.findViewById(R.id.searchEditText);
        filterChipGroup = view.findViewById(R.id.filterChipGroup);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
    }

    private void setupRecyclerView() {
        if (getContext() == null) return;
        salesAdapter = new SalesAdapter(this);
        salesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        salesRecyclerView.setAdapter(salesAdapter);
    }
    
    private void setupListeners() {
        // Search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSales();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Filter chips
        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chipAll) {
                    currentPaymentFilter = "All";
                } else if (checkedId == R.id.chipPaid) {
                    currentPaymentFilter = "Paid";
                } else if (checkedId == R.id.chipPending) {
                    currentPaymentFilter = "Pending";
                }
                filterSales();
            }
        });
        
        // Pull to refresh - force refresh from Firestore
        swipeRefreshLayout.setOnRefreshListener(() -> {
            resetPagination();
            loadSales(true);
        });

        // Infinite scroll listener
        salesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading && !isLastPage) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= PAGE_SIZE) {
                        loadMoreSales();
                    }
                }
            }
        });
    }

    private void resetPagination() {
        lastVisibleSnapshot = null;
        isLastPage = false;
        salesAdapter.clear();
    }

    private void loadSales() {
        loadSales(false);
    }

    private void loadSales(boolean isRefresh) {
        if (!isAdded() || isLoading) return;
        isLoading = true;
        if (isRefresh) swipeRefreshLayout.setRefreshing(true);

        FirestoreManager.getSalesPaginated(PAGE_SIZE, null, new FirestoreManager.OnSalesPaginatedListener() {
            @Override
            public void onSalesLoaded(List<Sale> sales, com.google.firebase.firestore.DocumentSnapshot lastVisible) {
                if (!isAdded()) return;
                isLoading = false;
                swipeRefreshLayout.setRefreshing(false);
                
                salesAdapter.setSales(sales);
                lastVisibleSnapshot = lastVisible;
                isLastPage = sales.size() < PAGE_SIZE;
                
                updateEmptyState(sales.isEmpty());
                filterSales(); // Apply any existing search/filter
            }

            @Override
            public void onSalesLoaded(List<Sale> sales) {
                // Not used for paginated call
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                isLoading = false;
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        }, isRefresh); // Pass the refresh flag here
    }

    private void loadMoreSales() {
        if (isLoading || isLastPage || !isAdded()) return;
        isLoading = true;

        FirestoreManager.getSalesPaginated(PAGE_SIZE, lastVisibleSnapshot, new FirestoreManager.OnSalesPaginatedListener() {
            @Override
            public void onSalesLoaded(List<Sale> sales, com.google.firebase.firestore.DocumentSnapshot lastVisible) {
                if (!isAdded()) return;
                isLoading = false;
                
                salesAdapter.addSales(sales);
                lastVisibleSnapshot = lastVisible;
                isLastPage = sales.size() < PAGE_SIZE;
                
                filterSales(); // Re-apply filters to newly added items
            }

            @Override
            public void onSalesLoaded(List<Sale> sales) {
                // Not used
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                isLoading = false;
                // Silently log or show toast
            }
        });
    }
    
    private void filterSales() {
        String query = searchEditText.getText().toString();
        salesAdapter.filter(query, currentPaymentFilter);
    }
    
    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            salesRecyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            salesRecyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }
    
    @Override
    public void onSaleClick(Sale sale) {
        if (!isAdded() || getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Sale Options");

        String paymentStatus = sale.getPayment_status();
        String[] options;
        if (paymentStatus != null && !paymentStatus.equalsIgnoreCase("Paid")) {
            options = new String[]{"Collect Payment", "Delete Sale", "Cancel"};
        } else {
            options = new String[]{"Delete Sale", "Cancel"};
        }

        builder.setItems(options, (dialog, which) -> {
            String choice = options[which];
            if (choice.equals("Collect Payment") || choice.equals("Mark as Paid")) {
                showPaymentDialog(sale);
            } else if (choice.equals("Delete Sale")) {
                onSaleLongClick(sale);
            }
        });
        builder.show();
    }

    private void showPaymentDialog(Sale sale) {
        if (!isAdded() || getContext() == null) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_payment, null);
        TextInputEditText amountInput = dialogView.findViewById(R.id.amountInput);
        TextView balanceInfoText = dialogView.findViewById(R.id.balanceInfoText);

        double currentBalance = sale.getBalance();
        balanceInfoText.setText(String.format(Locale.getDefault(), "Current Balance: ₹%.2f", currentBalance));
        amountInput.setText(String.valueOf(currentBalance)); // Default to full balance

        new AlertDialog.Builder(getContext())
                .setTitle("Collect Payment")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String amountStr = amountInput.getText().toString();
                    if (amountStr.isEmpty()) return;

                    try {
                        double amountPaidThisTime = Double.parseDouble(amountStr);
                        if (amountPaidThisTime <= 0) {
                            Toast.makeText(getContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (amountPaidThisTime > currentBalance) {
                            Toast.makeText(getContext(), "Amount cannot exceed balance", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        updateSalePayment(sale, amountPaidThisTime);

                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "Invalid amount format", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateSalePayment(Sale sale, double amountToPay) {
        if (!isAdded()) return;

        double newAmountPaid = sale.getAmount_paid() + amountToPay;
        double newBalance = sale.getTotal_amount() - newAmountPaid;

        sale.setAmount_paid(newAmountPaid);
        sale.setBalance(newBalance);
        
        if (newBalance <= 0) {
            sale.setPayment_status("Paid");
        } else {
            sale.setPayment_status("Pending");
        }

        FirestoreManager.updateSale(sale, new FirestoreManager.OnSaleUpdatedListener() {
            @Override
            public void onSuccess() {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Payment updated successfully", Toast.LENGTH_SHORT).show();
                // Since data changed, we should ideally refresh the current page, 
                // but for simplicity we'll just force a reload of the first page
                resetPagination();
                loadSales(false);
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Error updating payment: " + error, Toast.LENGTH_SHORT).show();
                loadSales(true); // Retry with a fresh load
            }
        });
    }

    @Override
    public void onSaleLongClick(Sale sale) {
        if (!isAdded() || getContext() == null) return;

        // Show delete confirmation dialog
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Sale")
                .setMessage("Are you sure you want to delete this sale for " + sale.getCustomer_name() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteSale(sale))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSale(Sale sale) {
        if (!isAdded()) return;

        FirestoreManager.deleteSale(sale.getSale_id(), new FirestoreManager.OnSaleDeletedListener() {
            @Override
            public void onSuccess() {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Sale deleted successfully", Toast.LENGTH_SHORT).show();
                resetPagination();
                loadSales(false);
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Error deleting sale: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
