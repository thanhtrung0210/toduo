package com.pipoxniko.toduo.todolistfragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pipoxniko.toduo.R;
import com.pipoxniko.toduo.adapter.ItemTaskGroupAdapter;
import com.pipoxniko.toduo.model.ItemTask;
import com.pipoxniko.toduo.model.ItemTaskGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TodolistStatusFragment extends Fragment {

    private View mView;
    private RecyclerView recyclerGroup;
    private TextView emptyMessage;
    private List<ItemTaskGroup> groupList = new ArrayList<>();
    private ItemTaskGroupAdapter groupAdapter;
    private DatabaseReference databaseReference;
    private String coupleId;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_todolist_status, container, false);

        // Gắn RecyclerView
        recyclerGroup = mView.findViewById(R.id.todolist_status_recycler_group);

        // Tạo TextView để hiển thị thông báo "Chưa có công việc nào"
        emptyMessage = new TextView(getContext());
        emptyMessage.setText("Chưa có công việc nào");
        emptyMessage.setTextSize(16);
        emptyMessage.setTextColor(getResources().getColor(R.color.color_black));
        emptyMessage.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        emptyMessage.setGravity(android.view.Gravity.CENTER);
        emptyMessage.setVisibility(View.GONE);
        ((ViewGroup) recyclerGroup.getParent()).addView(emptyMessage);

        if (getContext() != null) {
            recyclerGroup.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        // Khởi tạo adapter
        groupAdapter = new ItemTaskGroupAdapter(groupList, getContext());
        recyclerGroup.setAdapter(groupAdapter);

        // Khởi tạo Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("TODUO");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Lấy coupleId
        databaseReference.child("users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    coupleId = snapshot.child("coupleId").getValue(String.class);
                    Log.d("TodolistStatusFragment", "CoupleId: " + coupleId);
                    if (coupleId != null) {
                        loadTasksByStatus();
                    } else {
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(), "Bạn chưa ghép đôi, vui lòng ghép đôi để sử dụng tính năng này", Toast.LENGTH_LONG).show();
                        }
                        initializeEmptyGroups();
                    }
                } else {
                    Log.e("TodolistStatusFragment", "Không tìm thấy thông tin người dùng");
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(), "Không tìm thấy thông tin người dùng", Toast.LENGTH_LONG).show();
                    }
                    initializeEmptyGroups();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TodolistStatusFragment", "Lỗi khi lấy coupleId: " + error.getMessage());
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi khi lấy thông tin: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
                initializeEmptyGroups();
            }
        });

        return mView;
    }

    private void initializeEmptyGroups() {
        groupList.clear();
        groupList.add(new ItemTaskGroup("Mục tiêu bình thường", new ArrayList<>()));
        groupList.add(new ItemTaskGroup("Đã hoàn thành", new ArrayList<>()));
        groupList.add(new ItemTaskGroup("Đang chờ sửa", new ArrayList<>()));
        groupList.add(new ItemTaskGroup("Đang chờ xóa", new ArrayList<>()));
        groupAdapter.notifyDataSetChanged();
        emptyMessage.setVisibility(View.VISIBLE);
        recyclerGroup.setVisibility(View.VISIBLE);
    }

    private void loadTasksByStatus() {
        // Bước 1: Lấy danh sách các task đang chờ sửa/xóa từ pending_tasks
        databaseReference.child("pending_tasks").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot pendingSnapshot) {
                Map<String, ItemTask> pendingEditTasks = new HashMap<>(); // taskId -> task (pending_edit)
                Map<String, ItemTask> pendingDeleteTasks = new HashMap<>(); // taskId -> task (pending_delete)

                for (DataSnapshot pendingTaskSnapshot : pendingSnapshot.getChildren()) {
                    String taskId = pendingTaskSnapshot.child("task_id").getValue(String.class);
                    String action = pendingTaskSnapshot.child("action").getValue(String.class);
                    String pendingCoupleId = pendingTaskSnapshot.child("couple_id").getValue(String.class);

                    if (taskId != null && pendingCoupleId != null && pendingCoupleId.equals(coupleId)) {
                        // Tạo một ItemTask để hiển thị trong nhóm
                        ItemTask task = new ItemTask();
                        task.setId(taskId);
//                        task.setPendingTaskId(pendingTaskSnapshot.getKey()); // Lưu pending_task_id để xử lý xác nhận/từ chối

                        if ("pending_edit".equals(action)) {
                            pendingEditTasks.put(taskId, task);
                        } else if ("pending_delete".equals(action)) {
                            pendingDeleteTasks.put(taskId, task);
                        }
                    }
                }

                // Bước 2: Lấy danh sách task từ bảng tasks và phân loại
                databaseReference.child("tasks").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<ItemTask> normalTasks = new ArrayList<>();
                        List<ItemTask> completedTasks = new ArrayList<>();
                        List<ItemTask> pendingEditTasksList = new ArrayList<>();
                        List<ItemTask> pendingDeleteTasksList = new ArrayList<>();
                        int taskCount = 0;

                        for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                            ItemTask task = taskSnapshot.getValue(ItemTask.class);
                            if (task != null && coupleId.equals(task.getCoupleId())) {
                                taskCount++;
                                String taskId = task.getId();

//                                // Kiểm tra xem task có nằm trong pending_tasks không
//                                if (pendingEditTasks.containsKey(taskId)) {
//                                    task.setPendingTaskId(pendingEditTasks.get(taskId).getPendingTaskId());
//                                    pendingEditTasksList.add(task);
//                                } else if (pendingDeleteTasks.containsKey(taskId)) {
//                                    task.setPendingTaskId(pendingDeleteTasks.get(taskId).getPendingTaskId());
//                                    pendingDeleteTasksList.add(task);
//                                } else {
//                                    // Nếu không có trong pending_tasks, phân loại theo status
//                                    String status = task.getStatus();
//                                    if ("normal".equals(status)) {
//                                        normalTasks.add(task);
//                                    } else if ("completed".equals(status)) {
//                                        completedTasks.add(task);
//                                    }
//                                }
                            }
                        }

                        Log.d("TodolistStatusFragment", "Tổng số task tìm thấy: " + taskCount);

                        // Bước 3: Tạo các nhóm trạng thái
                        groupList.clear();
                        groupList.add(new ItemTaskGroup("Mục tiêu bình thường", normalTasks));
                        groupList.add(new ItemTaskGroup("Đã hoàn thành", completedTasks));
                        groupList.add(new ItemTaskGroup("Đang chờ sửa", pendingEditTasksList));
                        groupList.add(new ItemTaskGroup("Đang chờ xóa", pendingDeleteTasksList));

                        groupAdapter.notifyDataSetChanged();

                        // Hiển thị thông báo nếu không có task nào
                        if (taskCount == 0) {
                            emptyMessage.setVisibility(View.VISIBLE);
                            recyclerGroup.setVisibility(View.VISIBLE);
                        } else {
                            emptyMessage.setVisibility(View.GONE);
                            recyclerGroup.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("TodolistStatusFragment", "Lỗi khi lấy danh sách task: " + error.getMessage());
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(), "Lỗi khi lấy danh sách task: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        }
                        initializeEmptyGroups();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TodolistStatusFragment", "Lỗi khi lấy danh sách pending_tasks: " + error.getMessage());
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi khi lấy danh sách pending_tasks: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
                initializeEmptyGroups();
            }
        });
    }
}