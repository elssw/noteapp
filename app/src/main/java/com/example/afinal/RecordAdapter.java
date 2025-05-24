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
        ImageView iv;
        TextView tvPrice, tvNote, tvTime;
        VH(@NonNull View itemView) {
            super(itemView);
            iv      = itemView.findViewById(R.id.ivCategoryIcon);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvNote  = itemView.findViewById(R.id.tvNote);
            tvTime  = itemView.findViewById(R.id.tvTime);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_record, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Record r = data.get(pos);
        h.iv.setImageResource(r.getIconResId());
        h.tvPrice.setText(r.getPrice());
        h.tvNote .setText(r.getNote());
        h.tvTime .setText(r.getTime());
    }

    @Override
    public int getItemCount() { return data.size(); }
}
