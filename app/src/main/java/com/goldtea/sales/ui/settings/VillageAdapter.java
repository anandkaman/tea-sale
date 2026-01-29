package com.goldtea.sales.ui.settings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.goldtea.sales.R;
import com.goldtea.sales.data.model.Village;

import java.util.ArrayList;
import java.util.List;

public class VillageAdapter extends RecyclerView.Adapter<VillageAdapter.VillageViewHolder> {
    
    private List<Village> villages;
    private OnVillageActionListener listener;
    
    public interface OnVillageActionListener {
        void onDeleteClick(Village village);
    }
    
    public VillageAdapter(OnVillageActionListener listener) {
        this.villages = new ArrayList<>();
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public VillageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_village, parent, false);
        return new VillageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull VillageViewHolder holder, int position) {
        Village village = villages.get(position);
        holder.bind(village);
    }
    
    @Override
    public int getItemCount() {
        return villages.size();
    }
    
    public void setVillages(List<Village> villages) {
        this.villages = new ArrayList<>(villages);
        notifyDataSetChanged();
    }
    
    class VillageViewHolder extends RecyclerView.ViewHolder {
        TextView villageNameText, villageDayText;
        
        VillageViewHolder(@NonNull View itemView) {
            super(itemView);
            villageNameText = itemView.findViewById(R.id.villageNameText);
            villageDayText = itemView.findViewById(R.id.villageDayText);
            
            itemView.findViewById(R.id.deleteVillageButton).setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDeleteClick(villages.get(position));
                }
            });
        }
        
        void bind(Village village) {
            villageNameText.setText(village.getName());
            String day = village.getDay() != null ? village.getDay() : "Not set";
            villageDayText.setText("Market Day: " + day);
        }
    }
}
