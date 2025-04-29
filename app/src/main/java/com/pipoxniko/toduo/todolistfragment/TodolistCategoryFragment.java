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

public class TodolistCategoryFragment extends Fragment {

    private View mView;
    private RecyclerView recyclerGroup;
    private TextView emptyMessage;
    private List<ItemTaskGroup> groupList = new ArrayList<>();
    private ItemTaskGroupAdapter groupAdapter;
    private DatabaseReference databaseReference;
    private String coupleId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_todolist_category, container, false);

        // Gắn RecyclerView
        recyclerGroup = mView.findViewById(R.id.todolist_category_recycler_group);

        // Tạo TextView để hiển thị thông báo "Chưa có công việc hoặc phân loại nào"
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
        }

        // Khởi tạo adapter
        groupAdapter = new ItemTaskGroupAdapter(groupList, getContext());
        recyclerGroup.setAdapter(groupAdapter);

        // Khởi tạo Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("TODUO");
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Lấy coupleId của người dùng
        databaseReference.child("users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    coupleId = snapshot.child("coupleId").getValue(String.class);
                    Log.d("TodolistCategoryFragment", "CoupleId: " + coupleId);
                    if (coupleId != null) {
                        loadCategoriesAndTasks();
                    } else {
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(), "Bạn chưa ghép đôi, vui lòng ghép đôi để sử dụng tính năng này", Toast.LENGTH_LONG).show();
                        }
                        showEmptyMessage();
                    }
                } else {
                    Log.e("TodolistCategoryFragment", "Không tìm thấy thông tin người dùng");
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(), "Không tìm thấy thông tin người dùng", Toast.LENGTH_LONG).show();
                    }
                    showEmptyMessage();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TodolistCategoryFragment", "Lỗi khi lấy coupleId: " + error.getMessage());
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi khi lấy thông tin: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
                showEmptyMessage();
            }
        });

        return mView;
    }

    private void showEmptyMessage() {
        groupList.clear();
        groupAdapter.notifyDataSetChanged();
        emptyMessage.setVisibility(View.VISIBLE);
        recyclerGroup.setVisibility(View.VISIBLE);
    }

    private void loadCategoriesAndTasks() {
        // Bước 1: Lấy danh sách các phân loại từ task_categories
        databaseReference.child("task_categories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, String> categoryMap = new HashMap<>(); // categoryId -> categoryName
                for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                    String categoryId = categorySnapshot.getKey();
                    String categoryCoupleId = categorySnapshot.child("couple_id").getValue(String.class);
                    String categoryName = categorySnapshot.child("name").getValue(String.class);

                    if (categoryCoupleId != null && categoryCoupleId.equals(coupleId) && categoryName != null) {
                        categoryMap.put(categoryId, categoryName);
                        Log.d("TodolistCategoryFragment", "Category found: " + categoryName + " (ID: " + categoryId + ")");
                    }
                }

                if (categoryMap.isEmpty()) {
                    Log.d("TodolistCategoryFragment", "Không tìm thấy phân loại nào cho coupleId: " + coupleId);
                    showEmptyMessage();
                    return;
                }

                // Bước 2: Lấy danh sách task và nhóm theo phân loại
                databaseReference.child("tasks").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, List<ItemTask>> tasksByCategory = new HashMap<>(); // categoryId -> List<ItemTask>
                        int taskCount = 0;

                        for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                            ItemTask task = taskSnapshot.getValue(ItemTask.class);
                            if (task != null && coupleId.equals(task.getCoupleId())) {
                                taskCount++;
                                String categoryId = task.getCategoryId();
                                if (categoryId != null && categoryMap.containsKey(categoryId)) {
                                    tasksByCategory.computeIfAbsent(categoryId, k -> new ArrayList<>()).add(task);
                                    Log.d("TodolistCategoryFragment", "Task found: " + task.getTitle() + ", Category ID: " + categoryId);
                                }
                            }
                        }

                        Log.d("TodolistCategoryFragment", "Tổng số task tìm thấy: " + taskCount);

                        // Bước 3: Tạo các nhóm task theo phân loại (hiển thị tất cả phân loại, kể cả không có task)
                        groupList.clear();
                        for (Map.Entry<String, String> entry : categoryMap.entrySet()) {
                            String categoryId = entry.getKey();
                            String categoryName = entry.getValue();
                            List<ItemTask> tasks = tasksByCategory.getOrDefault(categoryId, new ArrayList<>());
                            groupList.add(new ItemTaskGroup(categoryName, tasks)); // Hiển thị tất cả phân loại
                        }

                        groupAdapter.notifyDataSetChanged();

                        // Không cần hiển thị thông báo "Chưa có công việc hoặc phân loại nào" vì đã có ít nhất 6 phân loại
                        emptyMessage.setVisibility(View.GONE);
                        recyclerGroup.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("TodolistCategoryFragment", "Lỗi khi lấy danh sách task: " + error.getMessage());
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(), "Lỗi khi lấy danh sách task: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        }
                        showEmptyMessage();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TodolistCategoryFragment", "Lỗi khi lấy danh sách phân loại: " + error.getMessage());
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi khi lấy danh sách phân loại: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
                showEmptyMessage();
            }
        });
    }
}