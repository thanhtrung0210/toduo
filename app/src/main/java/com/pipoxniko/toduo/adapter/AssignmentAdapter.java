package com.pipoxniko.toduo.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.pipoxniko.toduo.R;
import java.util.List;

public class AssignmentAdapter extends RecyclerView.Adapter<AssignmentAdapter.AssignmentViewHolder> {

    private List<String> assignments;
    private OnAssignmentClickListener listener;
    private String selectedAssignment;

    public interface OnAssignmentClickListener {
        void onAssignmentClick(String assignment);
    }

    public AssignmentAdapter(List<String> assignments, OnAssignmentClickListener listener) {
        this.assignments = assignments;
        this.listener = listener;
    }

    public void setSelectedAssignment(String selectedAssignment) {
        this.selectedAssignment = selectedAssignment;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AssignmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_popup_option, parent, false);
        return new AssignmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AssignmentViewHolder holder, int position) {
        String assignment = assignments.get(position);
        holder.textView.setText(assignment);

        if (assignment.equals(selectedAssignment)) {
            holder.textView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.color_default_secondary));
        } else {
            holder.textView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.black));
        }

        holder.itemView.setOnClickListener(v -> {
            selectedAssignment = assignment;
            notifyDataSetChanged();
            listener.onAssignmentClick(assignment);
        });
    }

    @Override
    public int getItemCount() {
        return assignments.size();
    }

    static class AssignmentViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public AssignmentViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.option_text);
        }
    }
}