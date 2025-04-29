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
import java.util.List;

public class TodolistAssignmentFragment extends Fragment {

    private View mView;
    private RecyclerView recyclerGroup;
    private TextView emptyMessage;
    private List<ItemTaskGroup> groupList = new ArrayList<>();
    private ItemTaskGroupAdapter groupAdapter;
    private DatabaseReference databaseReference;
    private String coupleId;
    private String currentUserId;
    private String user1Nickname;
    private String user2Nickname;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_todolist_assignment, container, false);

        // Gắn RecyclerView
        recyclerGroup = mView.findViewById(R.id.todolist_assignment_recycler_group);

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

        // Lấy coupleId và nickname của user1, user2
        databaseReference.child("users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    coupleId = snapshot.child("coupleId").getValue(String.class);
                    user1Nickname = snapshot.child("nickname").getValue(String.class);
                    if (user1Nickname == null || user1Nickname.isEmpty()) {
                        user1Nickname = "User 1";
                    }
                    Log.d("TodolistAssignmentFragment", "CoupleId: " + coupleId + ", User1 Nickname: " + user1Nickname);

                    if (coupleId != null) {
                        // Tìm nickname của user2
                        databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot usersSnapshot) {
                                for (DataSnapshot userSnapshot : usersSnapshot.getChildren()) {
                                    String userId = userSnapshot.getKey();
                                    String userCoupleId = userSnapshot.child("coupleId").getValue(String.class);
                                    if (userId != null && !userId.equals(currentUserId) && coupleId.equals(userCoupleId)) {
                                        user2Nickname = userSnapshot.child("nickname").getValue(String.class);
                                        if (user2Nickname == null || user2Nickname.isEmpty()) {
                                            user2Nickname = "User 2";
                                        }
                                        Log.d("TodolistAssignmentFragment", "User2 Nickname: " + user2Nickname);
                                        break;
                                    }
                                }
                                if (user2Nickname == null) {
                                    user2Nickname = "User 2";
                                }
                                loadTasksFromFirebase();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("TodolistAssignmentFragment", "Lỗi khi lấy nickname user2: " + error.getMessage());
                                user2Nickname = "User 2";
                                loadTasksFromFirebase();
                            }
                        });
                    } else {
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(), "Bạn chưa ghép đôi, vui lòng ghép đôi để sử dụng tính năng này", Toast.LENGTH_LONG).show();
                        }
                        initializeEmptyGroups();
                    }
                } else {
                    Log.e("TodolistAssignmentFragment", "Không tìm thấy thông tin người dùng");
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(), "Không tìm thấy thông tin người dùng", Toast.LENGTH_LONG).show();
                    }
                    initializeEmptyGroups();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TodolistAssignmentFragment", "Lỗi khi lấy coupleId: " + error.getMessage());
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
        groupList.add(new ItemTaskGroup(user1Nickname != null ? user1Nickname : "User 1", new ArrayList<>()));
        groupList.add(new ItemTaskGroup(user2Nickname != null ? user2Nickname : "User 2", new ArrayList<>()));
        groupList.add(new ItemTaskGroup("Cả hai", new ArrayList<>()));
        groupAdapter.notifyDataSetChanged();
        emptyMessage.setVisibility(View.VISIBLE);
        recyclerGroup.setVisibility(View.VISIBLE);
    }

    private void loadTasksFromFirebase() {
        databaseReference.child("tasks").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ItemTask> user1Tasks = new ArrayList<>();
                List<ItemTask> user2Tasks = new ArrayList<>();
                List<ItemTask> bothTasks = new ArrayList<>();

                int taskCount = 0;
                for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                    ItemTask task = taskSnapshot.getValue(ItemTask.class);
                    if (task != null && coupleId.equals(task.getCoupleId())) {
                        taskCount++;
                        Log.d("TodolistAssignmentFragment", "Task found: " + task.getTitle() + ", Assignment: " + task.getAssignment());
                        String assignment = task.getAssignment();

                        if (assignment != null) {
                            if (assignment.equals(user1Nickname)) {
                                user1Tasks.add(task);
                            } else if (assignment.equals(user2Nickname)) {
                                user2Tasks.add(task);
                            } else if (assignment.equals("Cả hai")) {
                                bothTasks.add(task);
                            }
                        }
                    }
                }

                Log.d("TodolistAssignmentFragment", "Tổng số task tìm thấy: " + taskCount);

                // Luôn hiển thị 3 nhóm, kể cả khi không có task con
                groupList.clear();
                groupList.add(new ItemTaskGroup(user1Nickname, user1Tasks));
                groupList.add(new ItemTaskGroup(user2Nickname, user2Tasks));
                groupList.add(new ItemTaskGroup("Cả hai", bothTasks));

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
                Log.e("TodolistAssignmentFragment", "Lỗi khi lấy danh sách task: " + error.getMessage());
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi khi lấy danh sách task: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
                initializeEmptyGroups();
            }
        });
    }
}