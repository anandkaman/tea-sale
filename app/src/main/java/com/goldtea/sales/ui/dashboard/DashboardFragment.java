package com.goldtea.sales.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.goldtea.sales.R;
import com.goldtea.sales.data.model.Sale;
import com.goldtea.sales.data.firestore.FirestoreManager;
import com.goldtea.sales.ui.viewsales.SalesAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {
    
    private TextView totalSalesText, todaySalesText, pendingPaymentsText, totalCountText;
    private RecyclerView recentSalesRecyclerView;
    private LinearLayout emptyStateLayout;
    private SalesAdapter salesAdapter;
    private FirestoreManager FirestoreManager;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        
        initializeViews(view);
        setupRecyclerView();
        loadDashboardData();
        
        return view;
    }
    
    private void initializeViews(View view) {
        if (getContext() == null) return;
        FirestoreManager = FirestoreManager.getInstance(getContext());

        totalSalesText = view.findViewById(R.id.totalSalesText);
        todaySalesText = view.findViewById(R.id.todaySalesText);
        pendingPaymentsText = view.findViewById(R.id.pendingPaymentsText);
        totalCountText = view.findViewById(R.id.totalCountText);
        recentSalesRecyclerView = view.findViewById(R.id.recentSalesRecyclerView);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
    }

    private void setupRecyclerView() {
        if (getContext() == null) return;
        salesAdapter = new SalesAdapter(new SalesAdapter.OnSaleClickListener() {
            @Override
            public void onSaleClick(Sale sale) {
                showSaleOptions(sale);
            }

            @Override
            public void onSaleLongClick(Sale sale) {
                // Delete logic if needed
            }
        });
        recentSalesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recentSalesRecyclerView.setAdapter(salesAdapter);
    }

    private void showSaleOptions(Sale sale) {
        if (!isAdded() || getContext() == null) return;

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle("Sale Options");

        String paymentStatus = sale.getPayment_status();
        String[] options;
        if (paymentStatus != null && !paymentStatus.equalsIgnoreCase("Paid")) {
            options = new String[]{"Collect Payment", "Cancel"};
        } else {
            options = new String[]{"View Details", "Cancel"};
        }

        builder.setItems(options, (dialog, which) -> {
            String choice = options[which];
            if (choice.equals("Collect Payment")) {
                showPaymentDialog(sale);
            }
        });
        builder.show();
    }

    private void showPaymentDialog(Sale sale) {
        if (!isAdded() || getContext() == null) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_payment, null);
        com.google.android.material.textfield.TextInputEditText amountInput = dialogView.findViewById(R.id.amountInput);
        android.widget.TextView balanceInfoText = dialogView.findViewById(R.id.balanceInfoText);

        double currentBalance = sale.getBalance();
        balanceInfoText.setText(String.format(Locale.getDefault(), "Current Balance: ₹%.2f", currentBalance));
        amountInput.setText(String.valueOf(currentBalance));

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Collect Payment")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String amountStr = amountInput.getText().toString();
                    if (amountStr.isEmpty()) return;

                    try {
                        double amountPaidThisTime = Double.parseDouble(amountStr);
                        if (amountPaidThisTime > 0 && amountPaidThisTime <= currentBalance) {
                            updateSalePayment(sale, amountPaidThisTime);
                        } else {
                            android.widget.Toast.makeText(getContext(), "Invalid amount", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        android.widget.Toast.makeText(getContext(), "Invalid amount format", android.widget.Toast.LENGTH_SHORT).show();
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
                android.widget.Toast.makeText(getContext(), "Payment updated successfully", android.widget.Toast.LENGTH_SHORT).show();
                loadDashboardData();
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getContext() == null) return;
                android.widget.Toast.makeText(getContext(), "Error updating payment: " + error, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadDashboardData() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date monthStart = calendar.getTime();
        
        // Use end of day for cache stability
        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        Date endOfDay = calendar.getTime();

        FirestoreManager.getSalesByDateRange(monthStart, endOfDay, new FirestoreManager.OnSalesLoadedListener() {
            @Override
            public void onSalesLoaded(List<Sale> sales) {
                if (isAdded()) {
                    calculateStatistics(sales);
                    displayRecentSales(sales);
                }
            }
            
            @Override
            public void onError(String error) {
                if (isAdded()) {
                    // Show empty state
                    emptyStateLayout.setVisibility(View.VISIBLE);
                    recentSalesRecyclerView.setVisibility(View.GONE);
                }
            }
        });
    }
    
    private void calculateStatistics(List<Sale> sales) {
        if (!isAdded()) return;

        if (sales == null || sales.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recentSalesRecyclerView.setVisibility(View.GONE);
            return;
        }

        double totalSales = 0;
        double todaySales = 0;
        double pendingPayments = 0;
        int totalCount = sales.size();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String today = dateFormat.format(new Date());

        for (Sale sale : sales) {
            if (sale == null) continue;

            totalSales += sale.getTotal_amount();

            // Safely handle null dates
            Date saleDate = sale.getDate();
            if (saleDate != null) {
                String saleDateStr = dateFormat.format(saleDate);
                if (saleDateStr.equals(today)) {
                    todaySales += sale.getTotal_amount();
                }
            }

            String paymentStatus = sale.getPayment_status();
            if (paymentStatus != null && paymentStatus.equals("Pending")) {
                pendingPayments += sale.getBalance();
            }
        }

        totalSalesText.setText(String.format(Locale.getDefault(), "₹ %.0f", totalSales));
        todaySalesText.setText(String.format(Locale.getDefault(), "₹ %.0f", todaySales));
        pendingPaymentsText.setText(String.format(Locale.getDefault(), "₹ %.0f", pendingPayments));
        totalCountText.setText(String.valueOf(totalCount));
    }
    
    private void displayRecentSales(List<Sale> sales) {
        if (!isAdded()) return;

        if (sales == null || sales.isEmpty()) {
            return;
        }

        // Show only the 5 most recent sales
        List<Sale> recentSales = new ArrayList<>();
        int count = Math.min(5, sales.size());
        for (int i = 0; i < count; i++) {
            recentSales.add(sales.get(i));
        }

        salesAdapter.setSales(recentSales);
        emptyStateLayout.setVisibility(View.GONE);
        recentSalesRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload data when fragment becomes visible again
        loadDashboardData();
    }
}
