package com.pipoxniko.toduo.todolistfragment;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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

public class TodolistCategoryFragment extends Fragment {

    private View mView;
    private RecyclerView recyclerGroup;
    private TextView emptyMessage;
    private List<ItemTaskGroup> groupList = new ArrayList<>();
    private ItemTaskGroupAdapter groupAdapter;
    private DatabaseReference databaseReference;
    private String coupleId;
    private AlertDialog loadingDialog;
    private ValueEventListener categoriesListener;
    private ValueEventListener tasksListener;

    public static TodolistCategoryFragment newInstance(String coupleId) {
        TodolistCategoryFragment fragment = new TodolistCategoryFragment();
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_todolist_category, container, false);

        recyclerGroup = mView.findViewById(R.id.todolist_category_recycler_group);

        emptyMessage = new TextView(getContext());
        emptyMessage.setText("Chưa có công việc hoặc phân loại nào");
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
            recyclerGroup.setNestedScrollingEnabled(false);
        }

        groupAdapter = new ItemTaskGroupAdapter(groupList, getContext());
        recyclerGroup.setAdapter(groupAdapter);

        if (getActivity() != null) {
            View loadingView = inflater.inflate(R.layout.custom_loading_dialog, null);
            loadingDialog = new AlertDialog.Builder(getActivity())
                    .setView(loadingView)
                    .setCancelable(false)
                    .create();
            loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        if (coupleId == null) {
            Toast.makeText(getActivity(), "Bạn chưa ghép đôi, hiển thị danh sách mặc định", Toast.LENGTH_LONG).show();
            showEmptyMessage();
        } else {
            loadCategoriesAndTasks();
        }

        return mView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (coupleId != null) {
            loadCategoriesAndTasks();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
        if (categoriesListener != null) {
            databaseReference.child("task_categories").removeEventListener(categoriesListener);
        }
        if (tasksListener != null) {
            databaseReference.child("tasks").removeEventListener(tasksListener);
        }
        loadingDialog = null;
    }

    private void showEmptyMessage() {
        groupList.clear();
        groupAdapter.notifyDataSetChanged();
        emptyMessage.setVisibility(View.VISIBLE);
        recyclerGroup.setVisibility(View.VISIBLE);
    }

    private void loadCategoriesAndTasks() {
        if (loadingDialog != null) {
            loadingDialog.show();
        }

        if (categoriesListener != null) {
            databaseReference.child("task_categories").removeEventListener(categoriesListener);
        }
        if (tasksListener != null) {
            databaseReference.child("tasks").removeEventListener(tasksListener);
        }

        categoriesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, String> categoryMap = new HashMap<>();
                for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                    String categoryId = categorySnapshot.getKey();
                    String categoryCoupleId = categorySnapshot.child("couple_id").getValue(String.class);
                    String categoryName = categorySnapshot.child("name").getValue(String.class);

                    if (categoryId != null && categoryCoupleId != null && categoryCoupleId.equals(coupleId) && categoryName != null) {
                        categoryMap.put(categoryId, categoryName);
                        Log.d("TodolistCategoryFragment", "Category found: " + categoryName + " (ID: " + categoryId + ")");
                    } else {
                        Log.d("TodolistCategoryFragment", "Bỏ qua category với ID: " + categoryId + " do không khớp coupleId hoặc dữ liệu không đầy đủ");
                    }
                }

                if (categoryMap.isEmpty()) {
                    Log.d("TodolistCategoryFragment", "Không tìm thấy phân loại nào cho coupleId: " + coupleId);
                    if (loadingDialog != null && loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }
                    showEmptyMessage();
                    return;
                }

                if (tasksListener != null) {
                    databaseReference.child("tasks").removeEventListener(tasksListener);
                }
                tasksListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (loadingDialog != null && loadingDialog.isShowing()) {
                            loadingDialog.dismiss();
                        }

                        Map<String, List<ItemTask>> tasksByCategory = new HashMap<>();
                        List<ItemTask> otherTasks = new ArrayList<>();
                        int taskCount = 0;

                        for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                            String taskId = taskSnapshot.getKey();
                            String taskCoupleId = taskSnapshot.child("couple_id").getValue(String.class);
                            String title = taskSnapshot.child("title").getValue(String.class);
                            String description = taskSnapshot.child("description").getValue(String.class);
                            String assignment = taskSnapshot.child("assignment").getValue(String.class);
                            String deadline = taskSnapshot.child("deadline").getValue(String.class);
                            String categoryId = taskSnapshot.child("category_id").getValue(String.class);
                            Boolean completed = taskSnapshot.child("completed").getValue(Boolean.class);
                            String status = taskSnapshot.child("status").getValue(String.class);
                            String createdAt = taskSnapshot.child("created_at").getValue(String.class);

                            if (taskId != null && taskCoupleId != null && coupleId != null && coupleId.equals(taskCoupleId)) {
                                if (status == null || (!status.equals("normal") && !status.equals("completed"))) {
                                    Log.d("TodolistCategoryFragment", "Bỏ qua task " + taskId + " do trạng thái không phù hợp: " + status);
                                    continue;
                                }

                                ItemTask task = new ItemTask();
                                task.setId(taskId);
                                task.setCoupleId(taskCoupleId);
                                task.setTitle(title != null ? title : "Không có tiêu đề");
                                task.setDescription(description != null ? description : null);
                                task.setAssignment(assignment != null ? assignment : null);
                                task.setDeadline(deadline != null ? deadline : null);
                                task.setCategoryId(categoryId != null ? categoryId : null);
                                task.setCompleted(completed != null ? completed : false);
                                task.setStatus(status != null ? status : "normal");
                                task.setCreatedAt(createdAt != null ? createdAt : null);
                                task.setChecked(task.isCompleted());

                                taskCount++;
                                Log.d("TodolistCategoryFragment", "Task found: " + task.getTitle() + ", Description: " + task.getDescription() +
                                        ", Assignment: " + task.getAssignment() + ", CategoryId: " + task.getCategoryId() +
                                        ", Completed: " + task.isCompleted());

                                if (categoryId != null && categoryMap.containsKey(categoryId)) {
                                    tasksByCategory.computeIfAbsent(categoryId, k -> new ArrayList<>()).add(task);
                                } else {
                                    otherTasks.add(task);
                                    Log.d("TodolistCategoryFragment", "Task " + task.getTitle() + " được thêm vào nhóm Khác");
                                }
                            } else {
                                Log.d("TodolistCategoryFragment", "Bỏ qua task với ID: " + taskId + " do không khớp coupleId hoặc dữ liệu không đầy đủ");
                            }
                        }

                        Log.d("TodolistCategoryFragment", "Tổng số task tìm thấy: " + taskCount);

                        groupList.clear();
                        for (Map.Entry<String, String> entry : categoryMap.entrySet()) {
                            String categoryId = entry.getKey();
                            String categoryName = entry.getValue();
                            List<ItemTask> tasks = tasksByCategory.getOrDefault(categoryId, new ArrayList<>());
                            groupList.add(new ItemTaskGroup(categoryName, tasks));
                            Log.d("TodolistCategoryFragment", "Group created: " + categoryName + ", Task count: " + tasks.size());
                        }

                        if (!otherTasks.isEmpty()) {
                            groupList.add(new ItemTaskGroup("Khác", otherTasks));
                            Log.d("TodolistCategoryFragment", "Group created: Khác, Task count: " + otherTasks.size());
                        }

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
                        Log.e("TodolistCategoryFragment", "Lỗi khi lấy danh sách task: " + error.getMessage());
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(), "Lỗi khi lấy danh sách task: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        }
                        showEmptyMessage();
                    }
                };
                databaseReference.child("tasks").addValueEventListener(tasksListener);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
                Log.e("TodolistCategoryFragment", "Lỗi khi lấy danh sách phân loại: " + error.getMessage());
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi khi lấy danh sách phân loại: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
                showEmptyMessage();
            }
        };
        databaseReference.child("task_categories").addValueEventListener(categoriesListener);
    }
}