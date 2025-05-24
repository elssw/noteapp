package com.example.afinal;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.afinal.model.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {
    private final List<Category> data;
    public CategoryAdapter(List<Category> data) {
        this.data = data;
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView iv;
        TextView tv;
        VH(@NonNull View itemView) {
            super(itemView);
            iv = itemView.findViewById(R.id.ivIcon);
            tv = itemView.findViewById(R.id.tvName);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Category cat = data.get(position);
        holder.iv.setImageResource(cat.getIconResId());
        holder.tv.setText(cat.getName());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
