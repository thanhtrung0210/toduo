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
    private TextView emptyMessage;
    private List<ItemTaskGroup> groupList = new ArrayList<>();
    private ItemTaskGroupAdapter groupAdapter;
    private DatabaseReference databaseReference;
    private String coupleId;
    private AlertDialog loadingDialog; // Thêm dialog loading

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_todolist_time, container, false);

        // Gắn RecyclerView
        recyclerGroup = mView.findViewById(R.id.todolist_time_recycler_group);

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
            recyclerGroup.setNestedScrollingEnabled(false); // Tắt cuộn của RecyclerView để NestedScrollView xử lý
        }

        // Khởi tạo adapter
        groupAdapter = new ItemTaskGroupAdapter(groupList, getContext());
        recyclerGroup.setAdapter(groupAdapter);

        // Khởi tạo dialog loading
        if (getActivity() != null) {
            View loadingView = inflater.inflate(R.layout.custom_loading_dialog, null);
            loadingDialog = new AlertDialog.Builder(getActivity())
                    .setView(loadingView)
                    .setCancelable(false)
                    .create();
            loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Khởi tạo Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("TODUO");
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Lấy coupleId của người dùng
        databaseReference.child("users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    coupleId = snapshot.child("coupleId").getValue(String.class);
                    Log.d("TodolistTimeFragment", "CoupleId: " + coupleId);
                    if (coupleId != null) {
                        loadTasksFromFirebase();
                    } else {
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(), "Bạn chưa ghép đôi, vui lòng ghép đôi để sử dụng tính năng này", Toast.LENGTH_LONG).show();
                        }
                        initializeEmptyGroups();
                    }
                } else {
                    Log.e("TodolistTimeFragment", "Không tìm thấy thông tin người dùng");
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(), "Không tìm thấy thông tin người dùng", Toast.LENGTH_LONG).show();
                    }
                    initializeEmptyGroups();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TodolistTimeFragment", "Lỗi khi lấy coupleId: " + error.getMessage());
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi khi lấy thông tin: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
                initializeEmptyGroups();
            }
        });

        return mView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Đóng dialog loading nếu đang hiển thị
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
        loadingDialog = null;
    }

    private void initializeEmptyGroups() {
        groupList.clear();
        groupList.add(new ItemTaskGroup("Hôm nay", new ArrayList<>()));
        groupList.add(new ItemTaskGroup("Tuần này", new ArrayList<>()));
        groupList.add(new ItemTaskGroup("Tháng này", new ArrayList<>()));
        groupList.add(new ItemTaskGroup("Tương lai", new ArrayList<>()));
        groupList.add(new ItemTaskGroup("Đã qua", new ArrayList<>()));
        groupAdapter.notifyDataSetChanged();
        emptyMessage.setVisibility(View.VISIBLE);
        recyclerGroup.setVisibility(View.VISIBLE);
    }

    private void loadTasksFromFirebase() {
        // Hiển thị dialog loading
        if (loadingDialog != null) {
            loadingDialog.show();
        }

        databaseReference.child("tasks").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Ẩn dialog loading
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }

                List<ItemTask> todayTasks = new ArrayList<>();
                List<ItemTask> thisWeekTasks = new ArrayList<>();
                List<ItemTask> thisMonthTasks = new ArrayList<>();
                List<ItemTask> futureTasks = new ArrayList<>();
                List<ItemTask> pastTasks = new ArrayList<>();

                int taskCount = 0;
                Calendar todayCal = Calendar.getInstance();
                todayCal.set(Calendar.HOUR_OF_DAY, 0);
                todayCal.set(Calendar.MINUTE, 0);
                todayCal.set(Calendar.SECOND, 0);
                todayCal.set(Calendar.MILLISECOND, 0);

                // Cuối ngày hôm nay
                Calendar endOfTodayCal = Calendar.getInstance();
                endOfTodayCal.setTime(todayCal.getTime());
                endOfTodayCal.set(Calendar.HOUR_OF_DAY, 23);
                endOfTodayCal.set(Calendar.MINUTE, 59);
                endOfTodayCal.set(Calendar.SECOND, 59);
                endOfTodayCal.set(Calendar.MILLISECOND, 999);

                // Cuối tuần này
                Calendar endOfThisWeekCal = Calendar.getInstance();
                endOfThisWeekCal.setTime(todayCal.getTime());
                endOfThisWeekCal.set(Calendar.DAY_OF_WEEK, endOfThisWeekCal.getFirstDayOfWeek());
                endOfThisWeekCal.add(Calendar.WEEK_OF_YEAR, 1);
                endOfThisWeekCal.add(Calendar.DAY_OF_YEAR, -1);
                endOfThisWeekCal.set(Calendar.HOUR_OF_DAY, 23);
                endOfThisWeekCal.set(Calendar.MINUTE, 59);
                endOfThisWeekCal.set(Calendar.SECOND, 59);
                endOfThisWeekCal.set(Calendar.MILLISECOND, 999);

                // Cuối tháng này
                Calendar endOfThisMonthCal = Calendar.getInstance();
                endOfThisMonthCal.set(Calendar.DAY_OF_MONTH, endOfThisMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH));
                endOfThisMonthCal.set(Calendar.HOUR_OF_DAY, 23);
                endOfThisMonthCal.set(Calendar.MINUTE, 59);
                endOfThisMonthCal.set(Calendar.SECOND, 59);
                endOfThisMonthCal.set(Calendar.MILLISECOND, 999);

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

                for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                    String taskId = taskSnapshot.getKey();
                    String taskCoupleId = taskSnapshot.child("couple_id").getValue(String.class);
                    String title = taskSnapshot.child("title").getValue(String.class);
                    String deadline = taskSnapshot.child("deadline").getValue(String.class);
                    String status = taskSnapshot.child("status").getValue(String.class);
                    String createdAt = taskSnapshot.child("created_at").getValue(String.class);

                    // Kiểm tra dữ liệu trước khi tạo đối tượng ItemTask
                    if (taskId != null && taskCoupleId != null && coupleId.equals(taskCoupleId)) {
                        ItemTask task = new ItemTask();
                        task.setId(taskId);
                        task.setCoupleId(taskCoupleId);
                        task.setTitle(title != null ? title : "Không có tiêu đề");
                        task.setDeadline(deadline);
                        task.setStatus(status != null ? status : "normal");
                        task.setCreatedAt(createdAt);

                        taskCount++;
                        Log.d("TodolistTimeFragment", "Task found: " + task.getTitle() + ", Deadline: " + task.getDeadline());

                        // Nếu task không có deadline, đưa vào nhóm "Tương lai"
                        if (task.getDeadline() == null) {
                            futureTasks.add(task);
                            Log.d("TodolistTimeFragment", "Added to Tương lai (no deadline): " + task.getTitle());
                            continue;
                        }

                        try {
                            Date deadlineDate = dateFormat.parse(task.getDeadline());
                            Calendar deadlineCal = Calendar.getInstance();
                            deadlineCal.setTime(deadlineDate);

                            // Task đã qua (deadline trước ngày hiện tại)
                            if (deadlineCal.getTimeInMillis() < todayCal.getTimeInMillis()) {
                                pastTasks.add(task);
                                Log.d("TodolistTimeFragment", "Added to Đã qua: " + task.getTitle());
                            }
                            // Task hôm nay (deadline trong ngày hiện tại)
                            else if (deadlineCal.getTimeInMillis() <= endOfTodayCal.getTimeInMillis()) {
                                todayTasks.add(task);
                                Log.d("TodolistTimeFragment", "Added to Hôm nay: " + task.getTitle());
                            }
                            // Task tuần này (deadline từ sau hôm nay đến cuối tuần này)
                            else if (deadlineCal.getTimeInMillis() <= endOfThisWeekCal.getTimeInMillis()) {
                                thisWeekTasks.add(task);
                                Log.d("TodolistTimeFragment", "Added to Tuần này: " + task.getTitle());
                            }
                            // Task tháng này (deadline từ sau tuần này đến cuối tháng này)
                            else if (deadlineCal.getTimeInMillis() <= endOfThisMonthCal.getTimeInMillis()) {
                                thisMonthTasks.add(task);
                                Log.d("TodolistTimeFragment", "Added to Tháng này: " + task.getTitle());
                            }
                            // Task tương lai (deadline ngoài tháng này)
                            else {
                                futureTasks.add(task);
                                Log.d("TodolistTimeFragment", "Added to Tương lai: " + task.getTitle());
                            }
                        } catch (Exception e) {
                            Log.e("TodolistTimeFragment", "Lỗi khi phân tích deadline của task: " + task.getTitle(), e);
                            futureTasks.add(task); // Nếu lỗi phân tích, đưa vào nhóm "Tương lai"
                            Log.d("TodolistTimeFragment", "Added to Tương lai (parse error): " + task.getTitle());
                        }
                    } else {
                        Log.d("TodolistTimeFragment", "Bỏ qua task với ID: " + taskId + " do không khớp coupleId hoặc dữ liệu không đầy đủ");
                    }
                }

                Log.d("TodolistTimeFragment", "Tổng số task tìm thấy: " + taskCount);
                Log.d("TodolistTimeFragment", "Hôm nay: " + todayTasks.size() + " tasks");
                Log.d("TodolistTimeFragment", "Tuần này: " + thisWeekTasks.size() + " tasks");
                Log.d("TodolistTimeFragment", "Tháng này: " + thisMonthTasks.size() + " tasks");
                Log.d("TodolistTimeFragment", "Tương lai: " + futureTasks.size() + " tasks");
                Log.d("TodolistTimeFragment", "Đã qua: " + pastTasks.size() + " tasks");

                // Luôn hiển thị 5 nhóm, kể cả khi không có task con
                groupList.clear();
                groupList.add(new ItemTaskGroup("Hôm nay", todayTasks));
                groupList.add(new ItemTaskGroup("Tuần này", thisWeekTasks));
                groupList.add(new ItemTaskGroup("Tháng này", thisMonthTasks));
                groupList.add(new ItemTaskGroup("Tương lai", futureTasks));
                groupList.add(new ItemTaskGroup("Đã qua", pastTasks));

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
                // Ẩn dialog loading nếu có lỗi
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
                Log.e("TodolistTimeFragment", "Lỗi khi lấy danh sách task: " + error.getMessage());
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi khi lấy danh sách task: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
                initializeEmptyGroups();
            }
        });
    }
}