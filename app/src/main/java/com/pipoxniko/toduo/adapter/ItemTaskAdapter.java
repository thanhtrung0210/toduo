package com.pipoxniko.toduo.adapter;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pipoxniko.toduo.R;
import com.pipoxniko.toduo.model.ItemTask;

import java.util.List;

public class ItemTaskAdapter extends  RecyclerView.Adapter<ItemTaskAdapter.TaskViewHolder>{

    private List<ItemTask> taskList;

    public ItemTaskAdapter(List<ItemTask> taskList) {
        this.taskList = taskList;
    }

    // Tạo ViewHolder cho mỗi item
    @NonNull
    @Override
    public ItemTaskAdapter.TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    // Gắn dữ liệu vào ViewHolder
    @Override
    public void onBindViewHolder(@NonNull ItemTaskAdapter.TaskViewHolder holder, int position) {
        ItemTask task = taskList.get(position);
        holder.checkBox.setOnCheckedChangeListener(null);   // Xóa sự kiện cũ (Ngăn  trigger khi recyle)
        holder.checkBox.setChecked(task.isChecked());
        holder.taskText.setText(task.getContent());
        holder.checkBox.setButtonTintList(null); // Ngăn tint từ theme

        // Áp dụng gạch chân khi checkbox được chọn
        applyStrikeThrough(holder.taskText, task.isChecked());

        // Xử lý sự kiện khi check/uncheck
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setChecked(isChecked);
            applyStrikeThrough(holder.taskText, isChecked);
        });

    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    // Lớp con dùng để giữ view của mỗi item
    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView taskText;
        ImageView taskOptions;;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.task_checkbox);
            taskText = itemView.findViewById(R.id.task_text);
            taskOptions = itemView.findViewById(R.id.task_options);
        }
    }

    // Hàm xử lý sự kiện check/uncheck (gạch chân/bỏ gạch chân)
    private void applyStrikeThrough(TextView textView, boolean isChecked) {
        if (isChecked) {
            textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            textView.setPaintFlags(textView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
    }

}
