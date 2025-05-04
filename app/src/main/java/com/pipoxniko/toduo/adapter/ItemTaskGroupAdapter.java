package com.pipoxniko.toduo.adapter;

import android.content.Context;
import android.util.Log;
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
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        ItemTaskGroup group = groupList.get(position);
        holder.groupTitle.setText(group.getGroupName());

        // Log số lượng task trong nhóm
        int taskCount = (group.getTaskList() != null) ? group.getTaskList().size() : 0;
        Log.d("ItemTaskGroupAdapter", "Binding group: " + group.getGroupName() + ", Task count: " + taskCount);

        // Thiết lập RecyclerView con cho danh sách task
        ItemTaskAdapter taskAdapter = new ItemTaskAdapter(group.getTaskList(), context);
        holder.taskRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        holder.taskRecyclerView.setAdapter(taskAdapter);

        // Cập nhật trạng thái mở/đóng
        updateExpandState(holder, group);

        // Xử lý sự kiện nhấn vào tiêu đề nhóm để mở/đóng
        holder.groupHeader.setOnClickListener(v -> {
            group.setExpanded(!group.isExpanded());
            updateExpandState(holder, group);
        });
    }

    private void updateExpandState(GroupViewHolder holder, ItemTaskGroup group) {
        if (group.getTaskList() == null || group.getTaskList().isEmpty()) {
            // Ẩn RecyclerView nếu nhóm không có task
            holder.taskRecyclerView.setVisibility(View.GONE);
            Log.d("ItemTaskGroupAdapter", "Group " + group.getGroupName() + " has no tasks, hiding RecyclerView");
        } else {
            // Hiển thị hoặc ẩn RecyclerView dựa trên trạng thái isExpanded
            if (group.isExpanded()) {
                holder.taskRecyclerView.setVisibility(View.VISIBLE);
                holder.expandIcon.setImageResource(R.drawable.todolist_close_task_group); // Mũi tên lên
                Log.d("ItemTaskGroupAdapter", "Group " + group.getGroupName() + " is expanded, showing tasks");
            } else {
                holder.taskRecyclerView.setVisibility(View.GONE);
                holder.expandIcon.setImageResource(R.drawable.todolist_open_task_group); // Mũi tên xuống
                Log.d("ItemTaskGroupAdapter", "Group " + group.getGroupName() + " is collapsed, hiding tasks");
            }
            holder.expandIcon.setVisibility(View.VISIBLE); // Hiển thị icon nếu có task
        }
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    public static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView groupTitle;
        ImageView expandIcon;
        RecyclerView taskRecyclerView;
        View groupHeader;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            groupHeader = itemView.findViewById(R.id.task_group_header);
            groupTitle = itemView.findViewById(R.id.task_group_title);
            expandIcon = itemView.findViewById(R.id.task_group_toggle);
            taskRecyclerView = itemView.findViewById(R.id.task_recycler);
        }
    }
}