package com.example.afinal;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.afinal.model.Record;
import java.util.List;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.VH> {
    private final List<Record> data;
    public RecordAdapter(List<Record> data) { this.data = data; }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvPrice, tvCategoryName, tvNote, tvDate;
        VH(@NonNull View itemView) {
            super(itemView);
            ivIcon          = itemView.findViewById(R.id.ivCategoryIcon);
            tvPrice         = itemView.findViewById(R.id.tvPrice);
            tvCategoryName  = itemView.findViewById(R.id.tvCategoryName);
            tvNote          = itemView.findViewById(R.id.tvNote);
            tvDate          = itemView.findViewById(R.id.tvTime);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_record, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Record r = data.get(pos);
        h.ivIcon.setImageResource(r.getIconResId());
        h.tvPrice.setText(r.getPrice());
        h.tvCategoryName.setText(r.getCategoryName());
        h.tvNote.setText(r.getNote());
        h.tvDate.setText(r.getDate());
    }

    @Override public int getItemCount() { return data.size(); }
}
