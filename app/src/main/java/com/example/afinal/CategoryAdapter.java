package com.example.afinal;

import android.content.Context;
import android.graphics.Color;
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
    private final Context context;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public CategoryAdapter(Context context, List<Category> data) {
        this.context = context;
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
        VH vh = new VH(v);
        v.setOnClickListener(view -> {
            int pos = vh.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            int old = selectedPosition;
            selectedPosition = pos;
            notifyItemChanged(old);
            notifyItemChanged(selectedPosition);
        });
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Category cat = data.get(position);
        holder.iv.setImageResource(cat.getIconResId());
        holder.tv.setText(cat.getName());

        //選到ㄉ顏色提示
        if (position == selectedPosition) {
            holder.itemView.setBackgroundColor(Color.parseColor("#D0EFFF"));
        } else {
            holder.itemView.setBackgroundColor(Color.parseColor("#00000000"));
            ;
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
