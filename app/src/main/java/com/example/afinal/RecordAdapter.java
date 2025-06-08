package com.example.afinal;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.afinal.model.Record;

import java.util.ArrayList;
import java.util.List;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.VH> {

    public interface OnRecordActionListener {
        void onRecordClick(Record record, int position);
        void onRecordDelete(Record record, int position);
    }

    private final List<Record> data;
    private final OnRecordActionListener actionListener;

    public RecordAdapter(List<Record> data, OnRecordActionListener listener) {
        this.data = new ArrayList<>(data);
        this.actionListener = listener;
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvPrice, tvCategoryName, tvNote, tvDate, tvLocation;

        VH(@NonNull View itemView) {
            super(itemView);
            ivIcon         = itemView.findViewById(R.id.ivCategoryIcon);
            tvPrice        = itemView.findViewById(R.id.tvPrice);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvNote         = itemView.findViewById(R.id.tvNote);
            tvDate         = itemView.findViewById(R.id.tvTime);
            tvLocation     = itemView.findViewById(R.id.tvLocation);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_record, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Record r = data.get(pos);
        h.ivIcon.setImageResource(r.getIconResId());
        h.tvPrice.setText(r.getPrice());
        h.tvCategoryName.setText(r.getCategoryName());
        h.tvNote.setText(r.getNote());
        h.tvDate.setText(r.getDate().length() >= 16 ? r.getDate().substring(11, 16) : r.getDate());
        h.tvLocation.setText(r.getLocation());

        h.itemView.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onRecordClick(r, h.getAdapterPosition());
        });

        h.itemView.setOnLongClickListener(v -> {
            if (actionListener != null) {
                actionListener.onRecordDelete(r, h.getAdapterPosition());
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }


    public void updateData(List<Record> newData) {
        this.data.clear();
        this.data.addAll(newData);
        notifyDataSetChanged();
    }

}
