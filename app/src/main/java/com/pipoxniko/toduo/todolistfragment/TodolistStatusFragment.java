package com.pipoxniko.toduo.todolistfragment;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.util.List;

public class TodolistStatusFragment extends Fragment {

    private DatabaseReference databaseReference;
    private String coupleId;
    private List<ItemTaskGroup> groupList;
    private ItemTaskGroupAdapter groupAdapter;
    private RecyclerView recyclerGroup;
    private TextView emptyMessage;
    private AlertDialog loadingDialog; // Sửa kiểu thành AlertDialog

    public TodolistStatusFragment() {
        // Required empty public constructor
    }

    public static TodolistStatusFragment newInstance(String coupleId) {
        TodolistStatusFragment fragment = new TodolistStatusFragment();
        Bundle args = new Bundle();
        args.putString("coupleId", coupleId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            coupleId = getArguments().getString("coupleId");
        }
        databaseReference = FirebaseDatabase.getInstance().getReference("TODUO");
        groupList = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_todolist_status, container, false);

        recyclerGroup = view.findViewById(R.id.todolist_status_recycler_group);

        recyclerGroup.setLayoutManager(new LinearLayoutManager(getContext()));
        groupAdapter = new ItemTaskGroupAdapter(groupList, getContext());
        recyclerGroup.setAdapter(groupAdapter);

        // Khởi tạo dialog loading
        if (getActivity() != null) {
            View loadingView = inflater.inflate(R.layout.custom_loading_dialog, null);
            loadingDialog = new AlertDialog.Builder(getActivity())
                    .setView(loadingView)
                    .setCancelable(false)
                    .create();

            // Xóa background mặc định của dialog
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }

        loadTasksFromFirebase();

        return view;
    }

    private void loadTasksFromFirebase() {
        if (loadingDialog != null) {
            loadingDialog.show();
        }

        databaseReference.child("tasks").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }

                List<ItemTask> normalTasks = new ArrayList<>();
                List<ItemTask> completedTasks = new ArrayList<>();
                List<ItemTask> pendingEditTasks = new ArrayList<>();
                List<ItemTask> pendingDeleteTasks = new ArrayList<>();

                int taskCount = 0;
                for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                    String taskId = taskSnapshot.getKey();
                    String taskCoupleId = taskSnapshot.child("couple_id").getValue(String.class);
                    String title = taskSnapshot.child("title").getValue(String.class);
                    String deadline = taskSnapshot.child("deadline").getValue(String.class);
                    String status = taskSnapshot.child("status").getValue(String.class);
                    String createdAt = taskSnapshot.child("created_at").getValue(String.class);
                    String description = taskSnapshot.child("description").getValue(String.class);
                    String categoryId = taskSnapshot.child("category_id").getValue(String.class);
                    String assignment = taskSnapshot.child("assignment").getValue(String.class);

                    if (taskId != null && taskCoupleId != null && coupleId.equals(taskCoupleId)) {
                        ItemTask task = new ItemTask();
                        task.setId(taskId);
                        task.setCoupleId(taskCoupleId);
                        task.setTitle(title != null ? title : "Không có tiêu đề");
                        task.setDeadline(deadline);
                        task.setStatus(status != null ? status : "normal");
                        task.setCreatedAt(createdAt);
                        task.setDescription(description);
                        task.setCategoryId(categoryId);
                        task.setAssignment(assignment != null ? assignment : "Cả hai");
                        task.setChecked("completed".equals(task.getStatus()));

                        taskCount++;
                        Log.d("TodolistStatusFragment", "Task found: " + task.getTitle() + ", Status: " + task.getStatus());

                        switch (task.getStatus()) {
                            case "normal":
                                normalTasks.add(task);
                                Log.d("TodolistStatusFragment", "Added to Bình thường: " + task.getTitle());
                                break;
                            case "completed":
                                completedTasks.add(task);
                                Log.d("TodolistStatusFragment", "Added to Đã hoàn thành: " + task.getTitle());
                                break;
                            case "pending_edit":
                                pendingEditTasks.add(task);
                                Log.d("TodolistStatusFragment", "Added to Đang chờ sửa: " + task.getTitle());
                                break;
                            case "pending_delete":
                                pendingDeleteTasks.add(task);
                                Log.d("TodolistStatusFragment", "Added to Đang chờ xóa: " + task.getTitle());
                                break;
                            default:
                                normalTasks.add(task);
                                Log.d("TodolistStatusFragment", "Trạng thái không xác định, mặc định thêm vào Bình thường: " + task.getTitle());
                                break;
                        }
                    } else {
                        Log.d("TodolistStatusFragment", "Bỏ qua task với ID: " + taskId + " do không khớp coupleId hoặc dữ liệu không đầy đủ");
                    }
                }

                Log.d("TodolistStatusFragment", "Tổng số task tìm thấy: " + taskCount);
                Log.d("TodolistStatusFragment", "Bình thường: " + normalTasks.size() + " tasks");
                Log.d("TodolistStatusFragment", "Đã hoàn thành: " + completedTasks.size() + " tasks");
                Log.d("TodolistStatusFragment", "Đang chờ sửa: " + pendingEditTasks.size() + " tasks");
                Log.d("TodolistStatusFragment", "Đang chờ xóa: " + pendingDeleteTasks.size() + " tasks");

                groupList.clear();
                groupList.add(new ItemTaskGroup("Bình thường", normalTasks));
                groupList.add(new ItemTaskGroup("Đã hoàn thành", completedTasks));
                groupList.add(new ItemTaskGroup("Đang chờ sửa", pendingEditTasks));
                groupList.add(new ItemTaskGroup("Đang chờ xóa", pendingDeleteTasks));

                groupAdapter.notifyDataSetChanged();

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
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
                Log.e("TodolistStatusFragment", "Lỗi khi lấy danh sách task: " + error.getMessage());
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi khi lấy danh sách task: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
                initializeEmptyGroups();
            }
        });
    }

    private void initializeEmptyGroups() {
        groupList.clear();
        groupList.add(new ItemTaskGroup("Bình thường", new ArrayList<>()));
        groupList.add(new ItemTaskGroup("Đã hoàn thành", new ArrayList<>()));
        groupList.add(new ItemTaskGroup("Đang chờ sửa", new ArrayList<>()));
        groupList.add(new ItemTaskGroup("Đang chờ xóa", new ArrayList<>()));
        groupAdapter.notifyDataSetChanged();
        emptyMessage.setVisibility(View.VISIBLE);
        recyclerGroup.setVisibility(View.VISIBLE);
    }
}