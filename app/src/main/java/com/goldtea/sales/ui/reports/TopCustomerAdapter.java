package com.goldtea.sales.ui.reports;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.goldtea.sales.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TopCustomerAdapter extends RecyclerView.Adapter<TopCustomerAdapter.ViewHolder> {

    private List<CustomerStats> customers;

    public TopCustomerAdapter() {
        this.customers = new ArrayList<>();
    }

    public void setCustomers(List<CustomerStats> customers) {
        this.customers = customers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_top_customer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CustomerStats customer = customers.get(position);
        holder.bind(customer, position + 1);
    }

    @Override
    public int getItemCount() {
        return customers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView rankText;
        TextView customerNameText;
        TextView villageText;
        TextView totalAmountText;

        ViewHolder(View itemView) {
            super(itemView);
            rankText = itemView.findViewById(R.id.rankText);
            customerNameText = itemView.findViewById(R.id.customerNameText);
            villageText = itemView.findViewById(R.id.villageText);
            totalAmountText = itemView.findViewById(R.id.totalAmountText);
        }

        void bind(CustomerStats customer, int rank) {
            rankText.setText(String.valueOf(rank));
            customerNameText.setText(customer.customerName);
            villageText.setText(customer.village);
            totalAmountText.setText(String.format(Locale.getDefault(), "₹%.0f", customer.totalAmount));
        }
    }

    // Data class for customer statistics
    public static class CustomerStats {
        public String customerName;
        public String village;
        public double totalAmount;
        public int salesCount;

        public CustomerStats(String customerName, String village, double totalAmount, int salesCount) {
            this.customerName = customerName;
            this.village = village;
            this.totalAmount = totalAmount;
            this.salesCount = salesCount;
        }
    }
}
