package com.pipoxniko.toduo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.pipoxniko.toduo.R;
import com.pipoxniko.toduo.model.ItemTaskGroup;

import java.util.ArrayList;
import java.util.List;

public class ItemTaskGroupAdapter extends RecyclerView.Adapter<ItemTaskGroupAdapter.GroupViewHolder> {

    private List<ItemTaskGroup> groupList;
    private Context context;

    public ItemTaskGroupAdapter(List<ItemTaskGroup> groupList, Context context) {
        this.groupList = groupList;
        this.context = context;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_group, parent, false);
        return new GroupViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        ItemTaskGroup group = groupList.get(position);
        holder.title.setText(group.getGroupTitle());
        holder.taskAdapter.updateTasks(group.getTasks());
        holder.recyclerView.setVisibility(group.isExpanded() ? View.VISIBLE : View.GONE);
        holder.toggleIcon.setImageResource(group.isExpanded() ? R.drawable.todolist_close_task_group : R.drawable.todolist_open_task_group);

        holder.header.setOnClickListener(v -> {
            group.setExpanded(!group.isExpanded());
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    public static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView toggleIcon;
        RecyclerView recyclerView;
        View header;
        ItemTaskAdapter taskAdapter;
        LinearLayoutManager layoutManager;

        public GroupViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            title = itemView.findViewById(R.id.task_group_title);
            toggleIcon = itemView.findViewById(R.id.task_group_toggle);
            recyclerView = itemView.findViewById(R.id.task_recycler);
            header = itemView.findViewById(R.id.task_group_header);

            layoutManager = new LinearLayoutManager(itemView.getContext());
            recyclerView.setLayoutManager(layoutManager);
            taskAdapter = new ItemTaskAdapter(new ArrayList<>(), context);
            recyclerView.setAdapter(taskAdapter);
        }
    }
}