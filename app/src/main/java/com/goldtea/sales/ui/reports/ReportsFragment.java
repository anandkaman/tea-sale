package com.goldtea.sales.ui.reports;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.goldtea.sales.R;
import com.goldtea.sales.data.firestore.FirestoreManager;
import com.goldtea.sales.data.model.Sale;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportsFragment extends Fragment {

    // UI Components
    private ChipGroup filterChipGroup;
    private TextView dateRangeText;
    private TextView totalSalesCount;
    private TextView totalRevenue;
    private TextView amountCollected;
    private TextView pendingAmount;
    private PieChart teaTypePieChart;
    private BarChart salesTrendChart;
    private PieChart paymentStatusChart;
    private RecyclerView topCustomersRecyclerView;
    private TextView noCustomersText;

    // Data
    private FirestoreManager firestoreManager;
    private List<Sale> allSales = new ArrayList<>();
    private List<Sale> filteredSales = new ArrayList<>();
    private TopCustomerAdapter topCustomerAdapter;

    // Filter period
    private enum Period { TODAY, WEEK, MONTH, YEAR }
    private Period currentPeriod = Period.TODAY;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupCharts();
        setupRecyclerView();
        setupFilters();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void initializeViews(View view) {
        if (getContext() == null) return;
        firestoreManager = FirestoreManager.getInstance(getContext());

        // Filter
        filterChipGroup = view.findViewById(R.id.filterChipGroup);
        dateRangeText = view.findViewById(R.id.dateRangeText);

        // Summary cards
        totalSalesCount = view.findViewById(R.id.totalSalesCount);
        totalRevenue = view.findViewById(R.id.totalRevenue);
        amountCollected = view.findViewById(R.id.amountCollected);
        pendingAmount = view.findViewById(R.id.pendingAmount);

        // Charts
        teaTypePieChart = view.findViewById(R.id.teaTypePieChart);
        salesTrendChart = view.findViewById(R.id.salesTrendChart);
        paymentStatusChart = view.findViewById(R.id.paymentStatusChart);

        // Top customers
        topCustomersRecyclerView = view.findViewById(R.id.topCustomersRecyclerView);
        noCustomersText = view.findViewById(R.id.noCustomersText);
    }

    private void setupCharts() {
        // Tea Type Pie Chart
        teaTypePieChart.setUsePercentValues(true);
        teaTypePieChart.getDescription().setEnabled(false);
        teaTypePieChart.setDrawHoleEnabled(true);
        teaTypePieChart.setHoleColor(Color.TRANSPARENT);
        teaTypePieChart.setTransparentCircleRadius(58f);

        // Sales Trend Bar Chart
        salesTrendChart.getDescription().setEnabled(false);
        salesTrendChart.setDrawGridBackground(false);
        salesTrendChart.setDrawBarShadow(false);
        salesTrendChart.setHighlightFullBarEnabled(false);
        salesTrendChart.setPinchZoom(false);
        salesTrendChart.setDrawValueAboveBar(true);

        XAxis xAxis = salesTrendChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        salesTrendChart.getAxisLeft().setDrawGridLines(false);
        salesTrendChart.getAxisRight().setEnabled(false);
        salesTrendChart.getLegend().setEnabled(false);

        // Payment Status Pie Chart
        paymentStatusChart.setUsePercentValues(true);
        paymentStatusChart.getDescription().setEnabled(false);
        paymentStatusChart.setDrawHoleEnabled(true);
        paymentStatusChart.setHoleColor(Color.TRANSPARENT);
        paymentStatusChart.setTransparentCircleRadius(58f);
    }

    private void setupRecyclerView() {
        if (!isAdded() || getContext() == null) return;
        topCustomerAdapter = new TopCustomerAdapter();
        topCustomersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        topCustomersRecyclerView.setAdapter(topCustomerAdapter);
    }

    private void setupFilters() {
        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipToday) {
                currentPeriod = Period.TODAY;
            } else if (checkedId == R.id.chipWeek) {
                currentPeriod = Period.WEEK;
            } else if (checkedId == R.id.chipMonth) {
                currentPeriod = Period.MONTH;
            } else if (checkedId == R.id.chipYear) {
                currentPeriod = Period.YEAR;
            }

            loadData(); // Re-fetch for the new date range
        });
    }

    private void loadData() {
        if (!isAdded()) return;
        
        Date startDate = getStartDate(currentPeriod);
        
        // Use end of day for cache stability
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        Date endDate = calendar.getTime();
        
        updateDateRangeText(startDate, endDate);

        firestoreManager.getSalesByDateRange(startDate, endDate, new FirestoreManager.OnSalesLoadedListener() {
            @Override
            public void onSalesLoaded(List<Sale> sales) {
                if (!isAdded()) return;
                
                allSales = sales;
                filteredSales = new ArrayList<>(sales); // In this optimized mode, they are the same
                updateUI();
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                updateUI();
            }
        });
    }

    private void filterSales() {
        // No longer needed as loadData fetches exactly what we need
    }

    private Date getStartDate(Period period) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        switch (period) {
            case TODAY:
                // Already set to start of today
                break;
            case WEEK:
                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                break;
            case MONTH:
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                break;
            case YEAR:
                calendar.set(Calendar.DAY_OF_YEAR, 1);
                break;
        }

        return calendar.getTime();
    }

    private void updateDateRangeText(Date startDate, Date endDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String range = sdf.format(startDate) + " - " + sdf.format(endDate);
        dateRangeText.setText(range);
    }

    private void updateUI() {
        if (!isAdded()) return; // Fragment not attached, skip update
        
        updateSummaryCards();
        updateTeaTypeChart();
        updateSalesTrendChart();
        updatePaymentStatusChart();
        updateTopCustomers();
    }

    private void updateSummaryCards() {
        int salesCount = filteredSales.size();
        double revenue = 0;
        double collected = 0;
        double pending = 0;

        for (Sale sale : filteredSales) {
            revenue += sale.getTotal_amount();
            collected += sale.getAmount_paid();
            pending += sale.getBalance(); // Use balance field directly
        }

        totalSalesCount.setText(String.valueOf(salesCount));
        totalRevenue.setText(String.format(Locale.getDefault(), "₹%.0f", revenue));
        amountCollected.setText(String.format(Locale.getDefault(), "₹%.0f", collected));
        pendingAmount.setText(String.format(Locale.getDefault(), "₹%.0f", pending));
    }

    private void updateTeaTypeChart() {
        if (!isAdded()) return;

        Map<String, Integer> teaTypeCounts = new HashMap<>();

        for (Sale sale : filteredSales) {
            String teaType = sale.getTea_type();
            if (teaType == null || teaType.isEmpty()) teaType = "Unknown";
            teaTypeCounts.put(teaType, teaTypeCounts.getOrDefault(teaType, 0) + 1);
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : teaTypeCounts.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        if (entries.isEmpty()) {
            teaTypePieChart.clear();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(getResources().getColor(R.color.tea_green), 
                         getResources().getColor(R.color.gold));
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f%%", value);
            }
        });

        teaTypePieChart.setData(data);
        teaTypePieChart.invalidate();
    }

    private void updateSalesTrendChart() {
        if (!isAdded()) return;

        Map<String, Float> dailySales = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());

        for (Sale sale : filteredSales) {
            Date saleDate = sale.getDate();
            if (saleDate != null) {
                String dateKey = sdf.format(saleDate);
                dailySales.put(dateKey, dailySales.getOrDefault(dateKey, 0f) + (float) sale.getTotal_amount());
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        final List<String> labels = new ArrayList<>(dailySales.keySet());
        
        for (int i = 0; i < labels.size(); i++) {
            entries.add(new BarEntry(i, dailySales.get(labels.get(i))));
        }

        if (entries.isEmpty()) {
            salesTrendChart.clear();
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Sales");
        dataSet.setColor(getResources().getColor(R.color.tea_green));
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.9f);

        salesTrendChart.setData(data);
        salesTrendChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < labels.size()) {
                    return labels.get(index);
                }
                return "";
            }
        });
        salesTrendChart.invalidate();
    }

    private void updatePaymentStatusChart() {
        if (!isAdded()) return;

        double totalPaid = 0;
        double totalPending = 0;

        for (Sale sale : filteredSales) {
            totalPaid += sale.getAmount_paid();
            totalPending += sale.getBalance();
        }

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        if (totalPaid > 0) {
            entries.add(new PieEntry((float) totalPaid, "Collected"));
            colors.add(getResources().getColor(R.color.status_paid)); // Green
        }
        if (totalPending > 0) {
            entries.add(new PieEntry((float) totalPending, "Pending"));
            colors.add(getResources().getColor(R.color.status_pending)); // Orange
        }

        if (entries.isEmpty()) {
            paymentStatusChart.clear();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f%%", value);
            }
        });

        paymentStatusChart.setData(data);
        paymentStatusChart.invalidate();
    }

    private void updateTopCustomers() {
        if (!isAdded()) return;

        Map<String, TopCustomerAdapter.CustomerStats> customerMap = new HashMap<>();

        for (Sale sale : filteredSales) {
            String customerName = sale.getCustomer_name() != null ? sale.getCustomer_name() : "Unknown";
            String village = sale.getVillage() != null ? sale.getVillage() : "Unknown";
            String key = customerName + "|" + village;
            
            if (customerMap.containsKey(key)) {
                TopCustomerAdapter.CustomerStats stats = customerMap.get(key);
                stats.totalAmount += sale.getTotal_amount();
                stats.salesCount++;
            } else {
                customerMap.put(key, new TopCustomerAdapter.CustomerStats(
                    customerName,
                    village,
                    sale.getTotal_amount(),
                    1
                ));
            }
        }

        List<TopCustomerAdapter.CustomerStats> customerList = new ArrayList<>(customerMap.values());
        customerList.sort((c1, c2) -> Double.compare(c2.totalAmount, c1.totalAmount));

        // Get top 5
        List<TopCustomerAdapter.CustomerStats> topCustomers = customerList.subList(0, Math.min(5, customerList.size()));

        if (topCustomers.isEmpty()) {
            topCustomersRecyclerView.setVisibility(View.GONE);
            noCustomersText.setVisibility(View.VISIBLE);
        } else {
            topCustomersRecyclerView.setVisibility(View.VISIBLE);
            noCustomersText.setVisibility(View.GONE);
            topCustomerAdapter.setCustomers(topCustomers);
        }
    }
}
