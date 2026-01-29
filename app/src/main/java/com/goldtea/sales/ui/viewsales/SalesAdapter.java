package com.goldtea.sales.ui.viewsales;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.goldtea.sales.R;
import com.goldtea.sales.data.model.Sale;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SalesAdapter extends RecyclerView.Adapter<SalesAdapter.SaleViewHolder> {
    
    private List<Sale> sales;
    private List<Sale> salesFiltered;
    private OnSaleClickListener listener;
    private SimpleDateFormat dateFormat;
    
    public interface OnSaleClickListener {
        void onSaleClick(Sale sale);
        void onSaleLongClick(Sale sale);
    }
    
    public SalesAdapter(OnSaleClickListener listener) {
        this.sales = new ArrayList<>();
        this.salesFiltered = new ArrayList<>();
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }
    
    @NonNull
    @Override
    public SaleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sale, parent, false);
        return new SaleViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SaleViewHolder holder, int position) {
        Sale sale = salesFiltered.get(position);
        holder.bind(sale);
    }
    
    @Override
    public int getItemCount() {
        return salesFiltered.size();
    }
    
    public void setSales(List<Sale> sales) {
        this.sales = new ArrayList<>(sales);
        this.salesFiltered = new ArrayList<>(sales);
        notifyDataSetChanged();
    }

    public void addSales(List<Sale> newSales) {
        int startPos = this.sales.size();
        this.sales.addAll(newSales);
        this.salesFiltered.addAll(newSales);
        notifyItemRangeInserted(startPos, newSales.size());
    }

    public void clear() {
        this.sales.clear();
        this.salesFiltered.clear();
        notifyDataSetChanged();
    }
    
    public void filter(String query, String paymentFilter) {
        salesFiltered.clear();

        for (Sale sale : sales) {
            if (sale == null) continue;

            String customerName = sale.getCustomer_name() != null ? sale.getCustomer_name() : "";
            String village = sale.getVillage() != null ? sale.getVillage() : "";
            String paymentStatus = sale.getPayment_status() != null ? sale.getPayment_status() : "";

            boolean matchesQuery = query.isEmpty() ||
                    customerName.toLowerCase().contains(query.toLowerCase()) ||
                    village.toLowerCase().contains(query.toLowerCase());

            boolean matchesPayment = paymentFilter.equals("All") ||
                    paymentStatus.equals(paymentFilter);

            if (matchesQuery && matchesPayment) {
                salesFiltered.add(sale);
            }
        }

        notifyDataSetChanged();
    }
    
    class SaleViewHolder extends RecyclerView.ViewHolder {
        TextView customerNameText, paymentStatusText, villageText, dateText;
        TextView teaDetailsText, quantityText, totalAmountText, balanceText;
        LinearLayout balanceLayout;
        
        SaleViewHolder(@NonNull View itemView) {
            super(itemView);
            
            customerNameText = itemView.findViewById(R.id.customerNameText);
            paymentStatusText = itemView.findViewById(R.id.paymentStatusText);
            villageText = itemView.findViewById(R.id.villageText);
            dateText = itemView.findViewById(R.id.dateText);
            teaDetailsText = itemView.findViewById(R.id.teaDetailsText);
            quantityText = itemView.findViewById(R.id.quantityText);
            totalAmountText = itemView.findViewById(R.id.totalAmountText);
            balanceText = itemView.findViewById(R.id.balanceText);
            balanceLayout = itemView.findViewById(R.id.balanceLayout);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSaleClick(salesFiltered.get(position));
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSaleLongClick(salesFiltered.get(position));
                    return true;
                }
                return false;
            });
        }
        
        void bind(Sale sale) {
            if (sale == null) return;

            customerNameText.setText(sale.getCustomer_name() != null ? sale.getCustomer_name() : "Unknown");
            villageText.setText(sale.getVillage() != null ? sale.getVillage() : "Unknown");

            // Safely format date
            if (sale.getDate() != null) {
                dateText.setText(dateFormat.format(sale.getDate()));
            } else {
                dateText.setText("-");
            }

            String teaDetails = String.format("%s %s - %s",
                    sale.getBrand() != null ? sale.getBrand() : "",
                    sale.getTea_type() != null ? sale.getTea_type() : "",
                    sale.getPackaging() != null ? sale.getPackaging() : "");
            teaDetailsText.setText(teaDetails);

            quantityText.setText(String.format(Locale.getDefault(), "Qty: %.1f", sale.getQuantity()));
            totalAmountText.setText(String.format(Locale.getDefault(), "₹ %.2f", sale.getTotal_amount()));

            // Payment status badge - handle null
            String paymentStatus = sale.getPayment_status() != null ? sale.getPayment_status() : "Pending";
            paymentStatusText.setText(paymentStatus);
            if ("Paid".equals(paymentStatus)) {
                paymentStatusText.setBackgroundResource(R.drawable.status_paid_bg);
                balanceLayout.setVisibility(View.GONE);
            } else {
                paymentStatusText.setBackgroundResource(R.drawable.status_pending_bg);
                balanceLayout.setVisibility(View.VISIBLE);
                balanceText.setText(String.format(Locale.getDefault(), "₹ %.2f", sale.getBalance()));
            }
        }
    }
}
