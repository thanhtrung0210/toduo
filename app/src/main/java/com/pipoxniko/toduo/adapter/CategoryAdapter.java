package com.pipoxniko.toduo.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.pipoxniko.toduo.R;
import com.pipoxniko.toduo.model.ItemCategory;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<ItemCategory> categories;
    private OnCategoryClickListener listener;
    private ItemCategory selectedCategory;

    public interface OnCategoryClickListener {
        void onCategoryClick(ItemCategory category);
    }

    public CategoryAdapter(List<ItemCategory> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    public void setSelectedCategory(ItemCategory selectedCategory) {
        this.selectedCategory = selectedCategory;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_popup_option, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        ItemCategory category = categories.get(position);
        holder.textView.setText(category.getName());

        if (selectedCategory != null && selectedCategory.getId().equals(category.getId())) {
            holder.textView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.color_default_secondary));
        } else {
            holder.textView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.black));
        }

        holder.itemView.setOnClickListener(v -> {
            selectedCategory = category;
            notifyDataSetChanged();
            listener.onCategoryClick(category);
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.option_text);
        }
    }
}