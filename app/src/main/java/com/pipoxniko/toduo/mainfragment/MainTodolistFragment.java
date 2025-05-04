package com.pipoxniko.toduo.mainfragment;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pipoxniko.toduo.R;
import com.pipoxniko.toduo.adapter.AssignmentAdapter;
import com.pipoxniko.toduo.adapter.CategoryAdapter;
import com.pipoxniko.toduo.adapter.TodolistViewPagerAdapter;
import com.pipoxniko.toduo.model.ItemCategory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainTodolistFragment extends Fragment {

    private View mView;

    // Các view cho tab và ViewPager2
    private TextView tabTime, tabCategory, tabAssign, tabStatus;
    private ViewPager2 viewPager2;

    // Các biến cho logic thêm task
    private FirebaseAuth auth;
    private DatabaseReference databaseReference;
    private String currentUserId;
    private String coupleId;
    private FloatingActionButton addTaskButton;
    private AlertDialog addTaskDialog;
    private PopupWindow categoryPopupWindow;
    private PopupWindow assignmentPopupWindow;
    private ItemCategory selectedCategory;
    private String selectedAssignment;
    private String selectedDate;
    private String selectedTime;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_main_todolist, container, false);

        // Ánh xạ view
        viewPager2 = mView.findViewById(R.id.tab_view_pager);
        tabTime = mView.findViewById(R.id.todolist_tab_time);
        tabCategory = mView.findViewById(R.id.todolist_tab_category);
        tabAssign = mView.findViewById(R.id.todolist_tab_assign);
        tabStatus = mView.findViewById(R.id.todolist_tab_status);

        // Mặc định sẽ là tab Thời gian
        setTabSelected("time");

        // Khởi tạo Firebase
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("TODUO");
        currentUserId = auth.getCurrentUser().getUid();

        // Ánh xạ nút thêm task
        addTaskButton = mView.findViewById(R.id.todolist_btn_add_task);

        // Lấy coupleId của người dùng trước khi khởi tạo ViewPager
        databaseReference.child("users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    coupleId = snapshot.child("coupleId").getValue(String.class);
                    if (coupleId == null) {
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(), "Bạn chưa ghép đôi, vui lòng ghép đôi để sử dụng tính năng này", Toast.LENGTH_LONG).show();
                        }
                        // Hiển thị ViewPager với coupleId là null (sẽ hiển thị danh sách mặc định)
                        setupViewPager();
                    } else {
                        // Kiểm tra và thêm các phân loại mặc định
                        addDefaultCategories();
                        // Khởi tạo ViewPager sau khi có coupleId
                        setupViewPager();
                    }
                } else {
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(), "Không tìm thấy thông tin người dùng", Toast.LENGTH_LONG).show();
                    }
                    // Hiển thị ViewPager với coupleId là null (sẽ hiển thị danh sách mặc định)
                    setupViewPager();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi khi lấy thông tin: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
                // Hiển thị ViewPager với coupleId là null (sẽ hiển thị danh sách mặc định)
                setupViewPager();
            }
        });

        // Sự kiện nhấn nút "Thêm task"
        addTaskButton.setOnClickListener(v -> showAddTaskDialog());

        return mView;
    }

    private void setupViewPager() {
        // Set adapter cho ViewPager2
        TodolistViewPagerAdapter adapter = new TodolistViewPagerAdapter(getActivity(), coupleId);
        viewPager2.setAdapter(adapter);

        // Xử lý sự kiện khi click vào các tab
        tabTime.setOnClickListener(v -> {
            setTabSelected("time");
            viewPager2.setCurrentItem(0, true);
        });
        tabCategory.setOnClickListener(v -> {
            setTabSelected("category");
            viewPager2.setCurrentItem(1, true);
        });
        tabAssign.setOnClickListener(v -> {
            setTabSelected("assign");
            viewPager2.setCurrentItem(2, true);
        });
        tabStatus.setOnClickListener(v -> {
            setTabSelected("status");
            viewPager2.setCurrentItem(3, true);
        });
    }

    private void showAddTaskDialog() {
        if (getActivity() == null || coupleId == null) {
            Toast.makeText(getActivity(), "Bạn chưa ghép đôi, vui lòng ghép đôi để sử dụng tính năng này", Toast.LENGTH_LONG).show();
            return;
        }

        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.todolist_dialog_add_task, null);
        EditText titleEditText = dialogView.findViewById(R.id.todolist_add_task_title);
        EditText descriptionEditText = dialogView.findViewById(R.id.todolist_add_task_description);
        ImageView categoryIcon = dialogView.findViewById(R.id.todolist_add_task_category);
        ImageView assignmentIcon = dialogView.findViewById(R.id.todolist_add_task_assignment);
        ImageView deadlineIcon = dialogView.findViewById(R.id.todolist_add_task_deadline);
        FloatingActionButton saveButton = dialogView.findViewById(R.id.todolist_add_task_save);

        addTaskDialog = new AlertDialog.Builder(getActivity())
                .setView(dialogView)
                .create();

        addTaskDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        addTaskDialog.getWindow().setGravity(Gravity.BOTTOM);
        addTaskDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        dialogView.setOnTouchListener((v, event) -> {
            hideKeyboard(v);
            return false;
        });

        categoryIcon.setOnClickListener(v -> showCategoryDialog(categoryIcon));
        assignmentIcon.setOnClickListener(v -> showAssignmentDialog(assignmentIcon));
        deadlineIcon.setOnClickListener(v -> showDeadlineDialog());

        saveButton.setOnClickListener(v -> {
            String title = titleEditText.getText().toString().trim();
            String description = descriptionEditText.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(getActivity(), "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show();
                return;
            }

            hideKeyboard(saveButton);
            saveTaskToFirebase(title, description);
            addTaskDialog.dismiss();
        });

        addTaskDialog.show();
    }

    private void showDeadlineDialog() {
        if (getActivity() == null) return;

        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.todolist_dialog_set_deadline, null);
        AlertDialog deadlineDialog = new AlertDialog.Builder(getActivity())
                .setView(dialogView)
                .create();

        Switch switchSelectDate = dialogView.findViewById(R.id.todolist_set_deadline_switch_select_date);
        LinearLayout datePickerContainer = dialogView.findViewById(R.id.todolist_set_deadline_date_picker_container);
        Button btnSelectDate = dialogView.findViewById(R.id.todolist_set_deadline_btn_select_date);
        TextView tvSelectedDate = dialogView.findViewById(R.id.todolist_set_deadline_txt_selected_date);

        Switch switchSelectTime = dialogView.findViewById(R.id.todolist_set_deadline_switch_select_time);
        LinearLayout timePickerContainer = dialogView.findViewById(R.id.todolist_set_deadline_time_picker_container);
        Button btnSelectTime = dialogView.findViewById(R.id.todolist_set_deadline_btn_select_time);
        TextView tvSelectedTime = dialogView.findViewById(R.id.todolist_set_deadline_tv_selected_time);

        Button btnCancel = dialogView.findViewById(R.id.todolist_set_deadline_btn_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.todolist_set_deadline_btn_confirm);

        if (selectedDate != null) {
            switchSelectDate.setChecked(true);
            datePickerContainer.setVisibility(View.VISIBLE);
            tvSelectedDate.setText(selectedDate);
        }
        if (selectedTime != null) {
            switchSelectTime.setChecked(true);
            timePickerContainer.setVisibility(View.VISIBLE);
            tvSelectedTime.setText(selectedTime);
        }

        switchSelectDate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                datePickerContainer.setVisibility(View.VISIBLE);
            } else {
                datePickerContainer.setVisibility(View.GONE);
                tvSelectedDate.setText("Chưa chọn");
                selectedDate = null;
            }
        });

        switchSelectTime.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                timePickerContainer.setVisibility(View.VISIBLE);
            } else {
                timePickerContainer.setVisibility(View.GONE);
                tvSelectedTime.setText("Chưa chọn");
                selectedTime = null;
            }
        });

        btnSelectDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(),
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear);
                        tvSelectedDate.setText(selectedDate);
                    }, year, month, day);
            datePickerDialog.show();
        });

        btnSelectTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(getActivity(),
                    (view, selectedHour, selectedMinute) -> {
                        selectedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                        tvSelectedTime.setText(selectedTime);
                    }, hour, minute, true);
            timePickerDialog.show();
        });

        btnCancel.setOnClickListener(v -> deadlineDialog.dismiss());

        btnConfirm.setOnClickListener(v -> deadlineDialog.dismiss());

        deadlineDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        deadlineDialog.show();
    }

    private void showCategoryDialog(View anchorView) {
        if (getActivity() == null) return;

        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.todolist_dialog_select_category, null);
        RecyclerView categoryList = dialogView.findViewById(R.id.todolist_category_list);

        categoryPopupWindow = new PopupWindow(dialogView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);

        categoryPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        categoryPopupWindow.setElevation(8f);
        categoryPopupWindow.setOutsideTouchable(true);
        categoryPopupWindow.setTouchable(true);

        categoryList.setLayoutManager(new LinearLayoutManager(getActivity()));
        List<ItemCategory> categories = new ArrayList<>();

        databaseReference.child("task_categories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categories.clear();
                for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                    String categoryId = categorySnapshot.getKey();
                    String coupleId = categorySnapshot.child("couple_id").getValue(String.class);
                    String name = categorySnapshot.child("name").getValue(String.class);

                    if (coupleId != null && coupleId.equals(MainTodolistFragment.this.coupleId)) {
                        categories.add(new ItemCategory(categoryId, coupleId, name));
                    }
                }

                CategoryAdapter adapter = new CategoryAdapter(categories, category -> {
                    selectedCategory = category;
                    categoryPopupWindow.dismiss();
                });
                adapter.setSelectedCategory(selectedCategory);
                categoryList.setAdapter(adapter);

                dialogView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int popupHeight = dialogView.getMeasuredHeight();

                int[] location = new int[2];
                anchorView.getLocationOnScreen(location);
                int anchorX = location[0];
                int anchorY = location[1];

                int offsetX = anchorView.getWidth();
                int offsetY = -popupHeight - anchorView.getHeight();

                categoryPopupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, anchorX + offsetX, anchorY + offsetY);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), "Lỗi khi lấy danh sách phân loại: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showAssignmentDialog(View anchorView) {
        if (getActivity() == null) return;

        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.todolist_dialog_select_assignment, null);
        RecyclerView assignmentList = dialogView.findViewById(R.id.assignment_list);

        assignmentPopupWindow = new PopupWindow(dialogView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);

        assignmentPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        assignmentPopupWindow.setElevation(8f);
        assignmentPopupWindow.setOutsideTouchable(true);
        assignmentPopupWindow.setTouchable(true);

        assignmentList.setLayoutManager(new LinearLayoutManager(getActivity()));
        List<String> assignments = new ArrayList<>();

        databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String user1Name = "User 1";
                String user2Name = "User 2";

                DataSnapshot currentUserSnapshot = snapshot.child(currentUserId);
                if (currentUserSnapshot.exists()) {
                    user1Name = currentUserSnapshot.child("nickname").getValue(String.class);
                    if (user1Name == null || user1Name.isEmpty()) {
                        user1Name = "User 1";
                    }
                } else {
                    Toast.makeText(getActivity(), "Không tìm thấy thông tin người dùng hiện tại", Toast.LENGTH_LONG).show();
                }

                if (coupleId != null) {
                    boolean foundPartner = false;
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String userId = userSnapshot.getKey();
                        String userCoupleId = userSnapshot.child("coupleId").getValue(String.class);
                        if (userId != null && !userId.equals(currentUserId) && coupleId.equals(userCoupleId)) {
                            user2Name = userSnapshot.child("nickname").getValue(String.class);
                            if (user2Name == null || user2Name.isEmpty()) {
                                user2Name = "User 2";
                            }
                            foundPartner = true;
                            break;
                        }
                    }
                    if (!foundPartner) {
                        Toast.makeText(getActivity(), "Không tìm thấy người dùng còn lại trong cặp đôi", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getActivity(), "Không tìm thấy coupleId", Toast.LENGTH_LONG).show();
                }

                assignments.add(user1Name);
                assignments.add(user2Name);
                assignments.add("Cả hai");

                AssignmentAdapter adapter = new AssignmentAdapter(assignments, assignment -> {
                    selectedAssignment = assignment;
                    assignmentPopupWindow.dismiss();
                });
                adapter.setSelectedAssignment(selectedAssignment);
                assignmentList.setAdapter(adapter);

                dialogView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int popupHeight = dialogView.getMeasuredHeight();

                int[] location = new int[2];
                anchorView.getLocationOnScreen(location);
                int anchorX = location[0];
                int anchorY = location[1];

                int offsetX = anchorView.getWidth();
                int offsetY = -popupHeight - anchorView.getHeight();

                assignmentPopupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, anchorX + offsetX, anchorY + offsetY);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), "Lỗi khi lấy danh sách người dùng: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveTaskToFirebase(String title, String description) {
        String taskId = databaseReference.child("tasks").push().getKey();
        if (taskId == null) {
            Toast.makeText(getActivity(), "Lỗi khi tạo task", Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> task = new HashMap<>();
        task.put("id", taskId);
        task.put("couple_id", coupleId);
        task.put("title", title);
        task.put("description", description);

        String deadline = null;
        if (selectedDate != null) {
            try {
                SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date date = inputDateFormat.parse(selectedDate);

                String time = (selectedTime != null) ? selectedTime : "07:00";
                SimpleDateFormat inputTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                Date fullDateTime = inputTimeFormat.parse(selectedDate + " " + time);

                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                deadline = outputFormat.format(fullDateTime);
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Lỗi khi xử lý thời gian: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
        task.put("deadline", deadline);

        task.put("category_id", selectedCategory != null ? selectedCategory.getId() : null);
        task.put("assignment", selectedAssignment != null ? selectedAssignment : "Cả hai");
        task.put("completed", false);
        task.put("status", "normal");
        task.put("created_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

        databaseReference.child("tasks").child(taskId).setValue(task)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), "Thêm task thành công", Toast.LENGTH_SHORT).show();
                    selectedCategory = null;
                    selectedAssignment = null;
                    selectedDate = null;
                    selectedTime = null;
                })
                .addOnFailureListener(e -> Toast.makeText(getActivity(), "Lỗi khi thêm task: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void addDefaultCategories() {
        List<String> defaultCategories = Arrays.asList(
                "Tài chính",
                "Giải trí",
                "Sức khỏe",
                "Học tập và phát triển",
                "Nhà cửa và sinh hoạt",
                "Du lịch"
        );

        databaseReference.child("task_categories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean hasCategoriesForCouple = false;
                for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                    String categoryCoupleId = categorySnapshot.child("couple_id").getValue(String.class);
                    if (coupleId.equals(categoryCoupleId)) {
                        hasCategoriesForCouple = true;
                        break;
                    }
                }

                if (!hasCategoriesForCouple) {
                    for (String categoryName : defaultCategories) {
                        String categoryId = databaseReference.child("task_categories").push().getKey();
                        if (categoryId != null) {
                            Map<String, Object> category = new HashMap<>();
                            category.put("couple_id", coupleId);
                            category.put("name", categoryName);
                            databaseReference.child("task_categories").child(categoryId).setValue(category);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi khi kiểm tra phân loại: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void hideKeyboard(View view) {
        if (getActivity() == null) return;
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void setTabSelected(String selected) {
        resetAllTabs();

        TextView selectedTab;
        switch (selected) {
            case "time":     selectedTab = tabTime;     break;
            case "category": selectedTab = tabCategory; break;
            case "assign":   selectedTab = tabAssign;   break;
            case "status":   selectedTab = tabStatus;   break;
            default:         selectedTab = tabTime;     break;
        }

        selectedTab.setBackgroundResource(R.drawable.main_tab_bg_selected_tab);
        selectedTab.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_default_secondary));
    }

    private void resetAllTabs() {
        TextView[] tabs = {tabTime, tabCategory, tabAssign, tabStatus};
        for (TextView tab : tabs) {
            tab.setBackgroundColor(Color.TRANSPARENT);
            tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_default_primary));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (addTaskDialog != null && addTaskDialog.isShowing()) {
            addTaskDialog.dismiss();
        }
        if (categoryPopupWindow != null && categoryPopupWindow.isShowing()) {
            categoryPopupWindow.dismiss();
        }
        if (assignmentPopupWindow != null && assignmentPopupWindow.isShowing()) {
            assignmentPopupWindow.dismiss();
        }
        addTaskDialog = null;
        categoryPopupWindow = null;
        assignmentPopupWindow = null;
    }
}