package com.goldtea.sales.ui.settings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.goldtea.sales.R;
import com.goldtea.sales.data.model.Pricing;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PricingAdapter extends RecyclerView.Adapter<PricingAdapter.PricingViewHolder> {
    
    private List<Pricing> pricingList;
    private OnPricingActionListener listener;
    
    public interface OnPricingActionListener {
        void onEditClick(Pricing pricing);
    }
    
    public PricingAdapter(OnPricingActionListener listener) {
        this.pricingList = new ArrayList<>();
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public PricingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pricing, parent, false);
        return new PricingViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PricingViewHolder holder, int position) {
        Pricing pricing = pricingList.get(position);
        holder.bind(pricing);
    }
    
    @Override
    public int getItemCount() {
        return pricingList.size();
    }
    
    public void setPricing(List<Pricing> pricingList) {
        this.pricingList = new ArrayList<>(pricingList);
        notifyDataSetChanged();
    }
    
    class PricingViewHolder extends RecyclerView.ViewHolder {
        TextView packageNameText, packageRateText;
        
        PricingViewHolder(@NonNull View itemView) {
            super(itemView);
            packageNameText = itemView.findViewById(R.id.packageNameText);
            packageRateText = itemView.findViewById(R.id.packageRateText);
            
            itemView.findViewById(R.id.editPricingButton).setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onEditClick(pricingList.get(position));
                }
            });
        }
        
        void bind(Pricing pricing) {
            packageNameText.setText(pricing.getPackage());
            packageRateText.setText(String.format(Locale.getDefault(), "₹ %d", pricing.getRate()));
        }
    }
}
