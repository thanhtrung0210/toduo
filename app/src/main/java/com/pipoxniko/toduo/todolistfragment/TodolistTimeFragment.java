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
    private AlertDialog loadingDialog;
    private ValueEventListener tasksListener;

    public static TodolistTimeFragment newInstance(String coupleId) {
        TodolistTimeFragment fragment = new TodolistTimeFragment();
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
        mView = inflater.inflate(R.layout.fragment_todolist_time, container, false);

        recyclerGroup = mView.findViewById(R.id.todolist_time_recycler_group);

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
            initializeEmptyGroups();
        } else {
            loadTasksFromFirebase();
        }

        return mView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (coupleId != null) {
            loadTasksFromFirebase();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
        if (tasksListener != null) {
            databaseReference.child("tasks").removeEventListener(tasksListener);
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
        if (loadingDialog != null) {
            loadingDialog.show();
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

                Calendar endOfTodayCal = Calendar.getInstance();
                endOfTodayCal.setTime(todayCal.getTime());
                endOfTodayCal.set(Calendar.HOUR_OF_DAY, 23);
                endOfTodayCal.set(Calendar.MINUTE, 59);
                endOfTodayCal.set(Calendar.SECOND, 59);
                endOfTodayCal.set(Calendar.MILLISECOND, 999);

                Calendar endOfThisWeekCal = Calendar.getInstance();
                endOfThisWeekCal.setTime(todayCal.getTime());
                endOfThisWeekCal.set(Calendar.DAY_OF_WEEK, endOfThisWeekCal.getFirstDayOfWeek());
                endOfThisWeekCal.add(Calendar.WEEK_OF_YEAR, 1);
                endOfThisWeekCal.add(Calendar.DAY_OF_YEAR, -1);
                endOfThisWeekCal.set(Calendar.HOUR_OF_DAY, 23);
                endOfThisWeekCal.set(Calendar.MINUTE, 59);
                endOfThisWeekCal.set(Calendar.SECOND, 59);
                endOfThisWeekCal.set(Calendar.MILLISECOND, 999);

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
                    Boolean completed = taskSnapshot.child("completed").getValue(Boolean.class);
                    String status = taskSnapshot.child("status").getValue(String.class);
                    String createdAt = taskSnapshot.child("created_at").getValue(String.class);

                    if (taskId != null && taskCoupleId != null && coupleId != null && coupleId.equals(taskCoupleId)) {
                        if (status == null || (!status.equals("normal") && !status.equals("completed"))) {
                            Log.d("TodolistTimeFragment", "Bỏ qua task " + taskId + " do trạng thái không phù hợp: " + status);
                            continue;
                        }

                        ItemTask task = new ItemTask();
                        task.setId(taskId);
                        task.setCoupleId(taskCoupleId);
                        task.setTitle(title != null ? title : "Không có tiêu đề");
                        task.setDeadline(deadline);
                        task.setCompleted(completed != null ? completed : false);
                        task.setStatus(status != null ? status : "normal");
                        task.setCreatedAt(createdAt);
                        task.setChecked(task.isCompleted());

                        taskCount++;
                        Log.d("TodolistTimeFragment", "Task found: " + task.getTitle() + ", Deadline: " + task.getDeadline() + ", Completed: " + task.isCompleted());

                        if (task.getDeadline() == null) {
                            futureTasks.add(task);
                            Log.d("TodolistTimeFragment", "Added to Tương lai (no deadline): " + task.getTitle());
                            continue;
                        }

                        try {
                            Date deadlineDate = dateFormat.parse(task.getDeadline());
                            Calendar deadlineCal = Calendar.getInstance();
                            deadlineCal.setTime(deadlineDate);

                            if (deadlineCal.getTimeInMillis() < todayCal.getTimeInMillis()) {
                                pastTasks.add(task);
                                Log.d("TodolistTimeFragment", "Added to Đã qua: " + task.getTitle());
                            } else if (deadlineCal.getTimeInMillis() <= endOfTodayCal.getTimeInMillis()) {
                                todayTasks.add(task);
                                Log.d("TodolistTimeFragment", "Added to Hôm nay: " + task.getTitle());
                            } else if (deadlineCal.getTimeInMillis() <= endOfThisWeekCal.getTimeInMillis()) {
                                thisWeekTasks.add(task);
                                Log.d("TodolistTimeFragment", "Added to Tuần này: " + task.getTitle());
                            } else if (deadlineCal.getTimeInMillis() <= endOfThisMonthCal.getTimeInMillis()) {
                                thisMonthTasks.add(task);
                                Log.d("TodolistTimeFragment", "Added to Tháng này: " + task.getTitle());
                            } else {
                                futureTasks.add(task);
                                Log.d("TodolistTimeFragment", "Added to Tương lai: " + task.getTitle());
                            }
                        } catch (Exception e) {
                            Log.e("TodolistTimeFragment", "Lỗi khi phân tích deadline của task: " + task.getTitle(), e);
                            futureTasks.add(task);
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

                groupList.clear();
                groupList.add(new ItemTaskGroup("Hôm nay", todayTasks));
                groupList.add(new ItemTaskGroup("Tuần này", thisWeekTasks));
                groupList.add(new ItemTaskGroup("Tháng này", thisMonthTasks));
                groupList.add(new ItemTaskGroup("Tương lai", futureTasks));
                groupList.add(new ItemTaskGroup("Đã qua", pastTasks));

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
                Log.e("TodolistTimeFragment", "Lỗi khi lấy danh sách task: " + error.getMessage());
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi khi lấy danh sách task: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
                initializeEmptyGroups();
            }
        };

        databaseReference.child("tasks").addValueEventListener(tasksListener);
    }
}