package com.pipoxniko.toduo.adapter;

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

    public ItemTaskGroupAdapter(List<ItemTaskGroup> groupList) {
        this.groupList = groupList;
    }

    // Tạo ViewHolder cho mỗi Group (accordion)
    @NonNull
    @Override
    public ItemTaskGroupAdapter.GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_group, parent, false);
        return new GroupViewHolder(view);
    }

    // Gắn dữ liệu vào mỗi nhóm (accordion)
    @Override
    public void onBindViewHolder(@NonNull ItemTaskGroupAdapter.GroupViewHolder holder, int position) {
        ItemTaskGroup group = groupList.get(position);
        holder.title.setText(group.getTitle());

        // Set adapter cho danh sách task trong mỗi nhóm
        ItemTaskAdapter adapter = new ItemTaskAdapter(group.getTaskList());
        holder.recyclerView.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        holder.recyclerView.setAdapter(adapter);

        // Hiển thị / ẩn danh sách task theo trạng thái mở, đóng
        holder.recyclerView.setVisibility(group.isExpanded() ? View.VISIBLE : View.GONE);
        holder.toggleIcon.setImageResource(group.isExpanded() ? R.drawable.todolist_close_task_group : R.drawable.todolist_open_task_group);

        //Khi click vào header sẽ mở/ đóng danh sách task
        holder.header.setOnClickListener(v -> {
            group.setExpanded(!group.isExpanded());
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    // Lớp con dùng để chứa các view bên trong
    public static class GroupViewHolder extends RecyclerView.ViewHolder {

        TextView title;
        ImageView toggleIcon;
        RecyclerView recyclerView;
        View header;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.task_group_title);
            toggleIcon = itemView.findViewById(R.id.task_group_toggle);
            recyclerView = itemView.findViewById(R.id.task_recycler);
            header = itemView.findViewById(R.id.task_group_header);
        }
    }
}
