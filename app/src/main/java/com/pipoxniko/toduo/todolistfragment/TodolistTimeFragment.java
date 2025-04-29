package com.pipoxniko.toduo.todolistfragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TodolistTimeFragment extends Fragment {

    private View mView;
    private RecyclerView recyclerGroup;
    private List<ItemTaskGroup> groupList = new ArrayList<>();
    private ItemTaskGroupAdapter groupAdapter;
    private DatabaseReference databaseReference;
    private String coupleId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_todolist_time, container, false);

        // Gắn RecyclerView
        recyclerGroup = mView.findViewById(R.id.todolist_time_recycler_group);
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
                    if (coupleId != null) {
                        loadTasksFromFirebase();
                    } else {
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(), "Bạn chưa ghép đôi, vui lòng ghép đôi để sử dụng tính năng này", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi khi lấy thông tin: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

        return mView;
    }

    private void loadTasksFromFirebase() {
        databaseReference.child("tasks").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ItemTask> todayTasks = new ArrayList<>();
                List<ItemTask> weekTasks = new ArrayList<>();
                List<ItemTask> monthTasks = new ArrayList<>();
                List<ItemTask> futureTasks = new ArrayList<>();
                List<ItemTask> pastTasks = new ArrayList<>();

                // Lấy ngày hiện tại
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);

                Calendar endOfWeek = Calendar.getInstance();
                endOfWeek.setTime(today.getTime());
                endOfWeek.add(Calendar.DAY_OF_YEAR, 7 - today.get(Calendar.DAY_OF_WEEK) + 1); // Cuối tuần

                Calendar endOfMonth = Calendar.getInstance();
                endOfMonth.setTime(today.getTime());
                endOfMonth.set(Calendar.DAY_OF_MONTH, endOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH));

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

                for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                    ItemTask task = taskSnapshot.getValue(ItemTask.class);
                    if (task != null && coupleId.equals(task.getCoupleId())) {
                        String deadline = task.getDeadline();
                        if (deadline == null) {
                            futureTasks.add(task); // Không có deadline -> Tương lai
                            continue;
                        }

                        try {
                            Date deadlineDate = dateFormat.parse(deadline);
                            Calendar deadlineCal = Calendar.getInstance();
                            deadlineCal.setTime(deadlineDate);

                            // So sánh deadline với các khoảng thời gian
                            if (deadlineCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                                    deadlineCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                                todayTasks.add(task); // Hôm nay
                            } else if (deadlineCal.before(today)) {
                                pastTasks.add(task); // Đã qua
                            } else if (deadlineCal.before(endOfWeek)) {
                                weekTasks.add(task); // Tuần này
                            } else if (deadlineCal.before(endOfMonth)) {
                                monthTasks.add(task); // Tháng này
                            } else {
                                futureTasks.add(task); // Tương lai
                            }
                        } catch (Exception e) {
                            futureTasks.add(task); // Nếu không parse được deadline, đưa vào Tương lai
                        }
                    }
                }

                // Cập nhật danh sách nhóm
                groupList.clear();
                if (!todayTasks.isEmpty()) {
                    groupList.add(new ItemTaskGroup("Hôm nay", todayTasks));
                }
                if (!weekTasks.isEmpty()) {
                    groupList.add(new ItemTaskGroup("Tuần này", weekTasks));
                }
                if (!monthTasks.isEmpty()) {
                    groupList.add(new ItemTaskGroup("Tháng này", monthTasks));
                }
                if (!futureTasks.isEmpty()) {
                    groupList.add(new ItemTaskGroup("Tương lai", futureTasks));
                }
                if (!pastTasks.isEmpty()) {
                    groupList.add(new ItemTaskGroup("Đã qua", pastTasks));
                }

                groupAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi khi lấy danh sách task: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}