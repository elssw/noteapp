package com.example.afinal;

import android.content.Context;
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
    public interface OnItemClickListener {
        void onItemClick(Category category);
    }

    private final Context context;
    private final List<Category> data;
    private OnItemClickListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public CategoryAdapter(Context context, List<Category> data) {
        this.context = context;
        this.data = data;
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.listener = l;
    }

    class VH extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        View root;

        VH(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvName = itemView.findViewById(R.id.tvName);
            root = itemView;
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_category, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Category c = data.get(pos);
        h.ivIcon.setImageResource(c.getIconResId());
        h.tvName.setText(c.getName());

        // 背景變色邏輯
        if (pos == selectedPosition) {
            h.root.setBackgroundColor(0xFFE0F7FA); // 淺藍色
        } else {
            h.root.setBackgroundColor(0x00000000); // 透明
        }

        h.root.setOnClickListener(v -> {
            int prevSelected = selectedPosition;
            selectedPosition = h.getAdapterPosition();
            notifyItemChanged(prevSelected);
            notifyItemChanged(selectedPosition);
            if (listener != null) listener.onItemClick(c);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
