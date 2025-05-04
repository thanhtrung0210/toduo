package com.pipoxniko.toduo.adapter;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pipoxniko.toduo.R;
import com.pipoxniko.toduo.model.ItemCategory;
import com.pipoxniko.toduo.model.ItemTask;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ItemTaskAdapter extends RecyclerView.Adapter<ItemTaskAdapter.TaskViewHolder> {

    private List<ItemTask> taskList;
    private DatabaseReference databaseReference;
    private DatabaseReference categoriesReference;
    private DatabaseReference pendingTasksReference;
    private String coupleId;
    private String currentUserId;
    private Context context;
    private PopupWindow categoryPopupWindow;
    private PopupWindow assignmentPopupWindow;
    private String selectedDate;
    private String selectedTime;
    private ItemCategory selectedCategory;
    private String selectedAssignment;

    public ItemTaskAdapter(List<ItemTask> taskList, Context context) {
        this.taskList = taskList;
        this.context = context;
        this.databaseReference = FirebaseDatabase.getInstance().getReference("TODUO").child("tasks");
        this.categoriesReference = FirebaseDatabase.getInstance().getReference("TODUO").child("task_categories");
        this.pendingTasksReference = FirebaseDatabase.getInstance().getReference("TODUO").child("pending_tasks");
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Lấy coupleId
        DatabaseReference usersReference = FirebaseDatabase.getInstance().getReference("TODUO").child("users");
        usersReference.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    coupleId = snapshot.child("coupleId").getValue(String.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Lỗi khi lấy thông tin người dùng: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void updateTasks(List<ItemTask> newTaskList) {
        this.taskList = newTaskList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        ItemTask task = taskList.get(position);
        holder.checkBox.setOnCheckedChangeListener(null); // Ngăn listener cũ gây lỗi
        holder.checkBox.setChecked(task.isChecked());
        holder.taskText.setText(task.getContent());
        holder.checkBox.setButtonTintList(null);

        applyStrikeThrough(holder.taskText, task.isChecked());

        // Đồng bộ checkbox với trạng thái từ Firebase
        if (task.getStatus() != null) {
            boolean isCompleted = task.getStatus().equals("completed");
            holder.checkBox.setChecked(isCompleted);
            task.setChecked(isCompleted);
            applyStrikeThrough(holder.taskText, isCompleted);
        }

        // Xử lý click checkbox
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setChecked(isChecked);
            applyStrikeThrough(holder.taskText, isChecked);

            // Cập nhật trạng thái trên Firebase
            String newStatus = isChecked ? "completed" : "normal";
            task.setStatus(newStatus); // Cập nhật trạng thái trong đối tượng task
            databaseReference.child(task.getId()).child("status").setValue(newStatus)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Cập nhật trạng thái thành công: " + (isChecked ? "Đã hoàn thành" : "Bình thường"), Toast.LENGTH_SHORT).show();
                        android.util.Log.d("ItemTaskAdapter", "Cập nhật trạng thái task " + task.getTitle() + " thành: " + newStatus);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Lỗi khi cập nhật trạng thái: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        android.util.Log.e("ItemTaskAdapter", "Lỗi khi cập nhật trạng thái task " + task.getTitle() + ": " + e.getMessage());
                        // Hoàn tác nếu có lỗi
                        holder.checkBox.setChecked(!isChecked);
                        task.setChecked(!isChecked);
                        applyStrikeThrough(holder.taskText, !isChecked);
                    });
        });

        // Xử lý nút ba chấm
        holder.taskOptions.setOnClickListener(v -> showTaskOptionsPopup(holder.taskOptions, task));
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    private void showTaskOptionsPopup(View anchorView, ItemTask task) {
        View popupView = LayoutInflater.from(context).inflate(R.layout.todolist_task_options_popup, null);
        PopupWindow popupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(8f);
        popupWindow.setOutsideTouchable(true);

        TextView optionViewDetails = popupView.findViewById(R.id.task_option_view_details);
        TextView optionEdit = popupView.findViewById(R.id.task_option_edit);
        TextView optionDelete = popupView.findViewById(R.id.task_option_delete);

        optionViewDetails.setOnClickListener(v -> {
            showTaskDetailsDialog(task);
            popupWindow.dismiss();
        });

        optionEdit.setOnClickListener(v -> {
            showEditTaskDialog(task);
            popupWindow.dismiss();
        });

        optionDelete.setOnClickListener(v -> {
            showDeleteConfirmationDialog(task);
            popupWindow.dismiss();
        });

        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        int anchorX = location[0];
        int anchorY = location[1];
        popupWindow.showAtLocation(anchorView, android.view.Gravity.NO_GRAVITY, anchorX, anchorY - popupView.getHeight());
    }

    private void showTaskDetailsDialog(ItemTask task) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.todolist_dialog_task_details, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        // Thêm logic ẩn bàn phím khi chạm ngoài
        setupHideKeyboard(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.task_detail_txt_title);
        TextView tvDescription = dialogView.findViewById(R.id.task_detail_txt_description);
        TextView tvDeadline = dialogView.findViewById(R.id.task_detail_txt_deadline);
        TextView tvCategory = dialogView.findViewById(R.id.task_detail_txt_category);
        TextView tvAssignment = dialogView.findViewById(R.id.task_detail_txt_assignment);
        Button btnClose = dialogView.findViewById(R.id.task_detail_btn_close);

        tvTitle.setText("Mục tiêu: " + task.getTitle());
        tvDescription.setText("Mô tả: " + (task.getDescription() != null ? task.getDescription() : "Không có"));
        tvAssignment.setText("Phân công: " + task.getAssignment());

        // Chuyển đổi định dạng deadline
        String deadlineText = "Hạn chót: Không có";
        if (task.getDeadline() != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
                Date deadlineDate = inputFormat.parse(task.getDeadline());
                deadlineText = "Hạn chót: " + outputFormat.format(deadlineDate);
            } catch (Exception e) {
                deadlineText = "Hạn chót: Lỗi định dạng";
            }
        }
        tvDeadline.setText(deadlineText);

        // Lấy tên phân loại từ category_id
        if (task.getCategoryId() != null) {
            categoriesReference.child(task.getCategoryId()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String categoryName = snapshot.child("name").getValue(String.class);
                        tvCategory.setText("Phân loại: " + (categoryName != null ? categoryName : "Không có"));
                    } else {
                        tvCategory.setText("Phân loại: Không có");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    tvCategory.setText("Phân loại: Lỗi khi lấy dữ liệu");
                }
            });
        } else {
            tvCategory.setText("Phân loại: Không có");
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    private void showEditTaskDialog(ItemTask task) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.todolist_dialog_edit_task, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        // Thêm logic ẩn bàn phím khi chạm ngoài
        setupHideKeyboard(dialogView);

        // Phần 1: Hiển thị thông tin hiện tại
        TextView tvTitle = dialogView.findViewById(R.id.task_edit_txt_title);
        TextView tvDescription = dialogView.findViewById(R.id.task_edit_txt_description);
        TextView tvDeadline = dialogView.findViewById(R.id.task_edit_txt_deadline);
        TextView tvCategory = dialogView.findViewById(R.id.task_edit_txt_category);
        TextView tvAssignment = dialogView.findViewById(R.id.task_edit_txt_assignment);

        tvTitle.setText("Mục tiêu: " + task.getTitle());
        tvDescription.setText("Mô tả: " + (task.getDescription() != null ? task.getDescription() : "Không có"));
        tvAssignment.setText("Phân công: " + task.getAssignment());

        String deadlineText = "Hạn chót: Không có";
        if (task.getDeadline() != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
                Date deadlineDate = inputFormat.parse(task.getDeadline());
                deadlineText = "Hạn chót: " + outputFormat.format(deadlineDate);

                // Phân tách ngày và giờ để điền vào selectedDate và selectedTime
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                selectedDate = dateFormat.format(deadlineDate);
                selectedTime = timeFormat.format(deadlineDate);
            } catch (Exception e) {
                deadlineText = "Hạn chót: Lỗi định dạng";
            }
        }
        tvDeadline.setText(deadlineText);

        // Lấy tên phân loại từ category_id
        if (task.getCategoryId() != null) {
            categoriesReference.child(task.getCategoryId()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String categoryName = snapshot.child("name").getValue(String.class);
                        tvCategory.setText("Phân loại: " + (categoryName != null ? categoryName : "Không có"));
                        selectedCategory = new ItemCategory(task.getCategoryId(), coupleId, categoryName);
                    } else {
                        tvCategory.setText("Phân loại: Không có");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    tvCategory.setText("Phân loại: Lỗi khi lấy dữ liệu");
                }
            });
        } else {
            tvCategory.setText("Phân loại: Không có");
        }

        // Phần 2: Chỉnh sửa
        EditText editTitle = dialogView.findViewById(R.id.task_edit_edit_title);
        EditText editDescription = dialogView.findViewById(R.id.task_edit_edit_description);
        ImageView editCategoryIcon = dialogView.findViewById(R.id.task_edit_edit_category);
        ImageView editAssignmentIcon = dialogView.findViewById(R.id.task_edit_edit_assignment);
        ImageView editDeadlineIcon = dialogView.findViewById(R.id.task_edit_edit_deadline);
        Button btnCancel = dialogView.findViewById(R.id.task_edit_btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.task_edit_btn_save);

        // Điền dữ liệu hiện tại
        editTitle.setText(task.getTitle());
        editDescription.setText(task.getDescription());
        selectedAssignment = task.getAssignment();

        // Thêm logic ẩn bàn phím khi nhấn "Enter"
        setupEditTextHideKeyboardOnEnter(editTitle);
        setupEditTextHideKeyboardOnEnter(editDescription);

        editCategoryIcon.setOnClickListener(v -> showCategoryDialog(dialogView, editCategoryIcon));
        editAssignmentIcon.setOnClickListener(v -> showAssignmentDialog(dialogView, editAssignmentIcon));
        editDeadlineIcon.setOnClickListener(v -> showDeadlineDialog(dialogView));

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newTitle = editTitle.getText().toString().trim();
            String newDescription = editDescription.getText().toString().trim();

            if (newTitle.isEmpty()) {
                Toast.makeText(context, "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show();
                return;
            }

            // Tạo dữ liệu cập nhật
            Map<String, Object> updatedData = new HashMap<>();
            updatedData.put("title", newTitle);
            updatedData.put("description", newDescription);
            updatedData.put("category_id", selectedCategory != null ? selectedCategory.getId() : null);
            updatedData.put("assignment", selectedAssignment != null ? selectedAssignment : "Cả hai");

            // Xử lý deadline
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
                    Toast.makeText(context, "Lỗi khi xử lý thời gian: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
            updatedData.put("deadline", deadline);

            // Lưu vào bảng pending_tasks
            String pendingTaskId = pendingTasksReference.push().getKey();
            if (pendingTaskId != null) {
                Map<String, Object> pendingTask = new HashMap<>();
                pendingTask.put("task_id", task.getId());
                pendingTask.put("couple_id", task.getCoupleId());
                pendingTask.put("action", "pending_edit");
                pendingTask.put("requested_by", currentUserId);
                pendingTask.put("updated_data", updatedData);
                pendingTask.put("requested_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

                pendingTasksReference.child(pendingTaskId).setValue(pendingTask)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "Yêu cầu chỉnh sửa đã được gửi", Toast.LENGTH_SHORT).show();
                            // Cập nhật trạng thái trên bảng tasks
                            databaseReference.child(task.getId()).child("status").setValue("pending_edit")
                                    .addOnSuccessListener(aVoid2 -> {
                                        android.util.Log.d("ItemTaskAdapter", "Task " + task.getTitle() + " đã chuyển sang trạng thái pending_edit");
                                    })
                                    .addOnFailureListener(e -> {
                                        android.util.Log.e("ItemTaskAdapter", "Lỗi khi cập nhật trạng thái pending_edit cho task " + task.getTitle() + ": " + e.getMessage());
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "Lỗi khi gửi yêu cầu chỉnh sửa: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            }

            dialog.dismiss();
        });

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    private void showCategoryDialog(View dialogView, View anchorView) {
        View popupView = LayoutInflater.from(context).inflate(R.layout.todolist_dialog_select_category, null);
        RecyclerView categoryList = popupView.findViewById(R.id.todolist_category_list);

        categoryPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);

        categoryPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        categoryPopupWindow.setElevation(8f);
        categoryPopupWindow.setOutsideTouchable(true);
        categoryPopupWindow.setTouchable(true);

        categoryList.setLayoutManager(new LinearLayoutManager(context));
        List<ItemCategory> categories = new ArrayList<>();

        categoriesReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categories.clear();
                for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                    String categoryId = categorySnapshot.getKey();
                    String categoryCoupleId = categorySnapshot.child("couple_id").getValue(String.class);
                    String name = categorySnapshot.child("name").getValue(String.class);

                    if (categoryCoupleId != null && categoryCoupleId.equals(coupleId)) {
                        categories.add(new ItemCategory(categoryId, categoryCoupleId, name));
                    }
                }

                CategoryAdapter adapter = new CategoryAdapter(categories, category -> {
                    selectedCategory = category;
                    categoryPopupWindow.dismiss();
                });
                adapter.setSelectedCategory(selectedCategory);
                categoryList.setAdapter(adapter);

                popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int popupHeight = popupView.getMeasuredHeight();

                int[] location = new int[2];
                anchorView.getLocationOnScreen(location);
                int anchorX = location[0];
                int anchorY = location[1];
                int offsetX = anchorView.getWidth();
                int offsetY = -popupHeight - anchorView.getHeight();
                categoryPopupWindow.showAtLocation(anchorView, android.view.Gravity.NO_GRAVITY, anchorX + offsetX, anchorY + offsetY);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Lỗi khi lấy danh sách phân loại: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showAssignmentDialog(View dialogView, View anchorView) {
        View popupView = LayoutInflater.from(context).inflate(R.layout.todolist_dialog_select_assignment, null);
        RecyclerView assignmentList = popupView.findViewById(R.id.assignment_list);

        assignmentPopupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);

        assignmentPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        assignmentPopupWindow.setElevation(8f);
        assignmentPopupWindow.setOutsideTouchable(true);
        assignmentPopupWindow.setTouchable(true);

        assignmentList.setLayoutManager(new LinearLayoutManager(context));
        List<String> assignments = new ArrayList<>();

        DatabaseReference usersReference = FirebaseDatabase.getInstance().getReference("TODUO").child("users");
        usersReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String user1Name = "User 1";
                String user2Name = "User 2";

                if (snapshot.child(currentUserId).exists()) {
                    user1Name = snapshot.child(currentUserId).child("nickname").getValue(String.class);
                    if (user1Name == null || user1Name.isEmpty()) user1Name = "User 1";
                }

                if (coupleId != null) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String userId = userSnapshot.getKey();
                        String userCoupleId = userSnapshot.child("coupleId").getValue(String.class);
                        if (userId != null && !userId.equals(currentUserId) && coupleId.equals(userCoupleId)) {
                            user2Name = userSnapshot.child("nickname").getValue(String.class);
                            if (user2Name == null || user2Name.isEmpty()) user2Name = "User 2";
                            break;
                        }
                    }
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

                popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int popupHeight = popupView.getMeasuredHeight();

                int[] location = new int[2];
                anchorView.getLocationOnScreen(location);
                int anchorX = location[0];
                int anchorY = location[1];
                int offsetX = anchorView.getWidth();
                int offsetY = -popupHeight - anchorView.getHeight();
                assignmentPopupWindow.showAtLocation(anchorView, android.view.Gravity.NO_GRAVITY, anchorX + offsetX, anchorY + offsetY);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Lỗi khi lấy danh sách người dùng: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showDeadlineDialog(View dialogView) {
        View deadlineView = LayoutInflater.from(context).inflate(R.layout.todolist_dialog_set_deadline, null);
        AlertDialog deadlineDialog = new AlertDialog.Builder(context)
                .setView(deadlineView)
                .create();

        // Thêm logic ẩn bàn phím khi chạm ngoài
        setupHideKeyboard(deadlineView);

        Switch switchSelectDate = deadlineView.findViewById(R.id.todolist_set_deadline_switch_select_date);
        LinearLayout datePickerContainer = deadlineView.findViewById(R.id.todolist_set_deadline_date_picker_container);
        Button btnSelectDate = deadlineView.findViewById(R.id.todolist_set_deadline_btn_select_date);
        TextView tvSelectedDate = deadlineView.findViewById(R.id.todolist_set_deadline_txt_selected_date);

        Switch switchSelectTime = deadlineView.findViewById(R.id.todolist_set_deadline_switch_select_time);
        LinearLayout timePickerContainer = deadlineView.findViewById(R.id.todolist_set_deadline_time_picker_container);
        Button btnSelectTime = deadlineView.findViewById(R.id.todolist_set_deadline_btn_select_time);
        TextView tvSelectedTime = deadlineView.findViewById(R.id.todolist_set_deadline_tv_selected_time);

        Button btnCancel = deadlineView.findViewById(R.id.todolist_set_deadline_btn_cancel);
        Button btnConfirm = deadlineView.findViewById(R.id.todolist_set_deadline_btn_confirm);

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

            DatePickerDialog datePickerDialog = new DatePickerDialog(context,
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

            TimePickerDialog timePickerDialog = new TimePickerDialog(context,
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

    private void showDeleteConfirmationDialog(ItemTask task) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.todolist_dialog_confirm_delete, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        // Thêm logic ẩn bàn phím khi chạm ngoài
        setupHideKeyboard(dialogView);

        Button btnNo = dialogView.findViewById(R.id.task_delete_btn_no);
        Button btnYes = dialogView.findViewById(R.id.task_delete_btn_yes);

        btnNo.setOnClickListener(v -> dialog.dismiss());

        btnYes.setOnClickListener(v -> {
            String pendingTaskId = pendingTasksReference.push().getKey();
            if (pendingTaskId != null) {
                Map<String, Object> pendingTask = new HashMap<>();
                pendingTask.put("task_id", task.getId());
                pendingTask.put("couple_id", task.getCoupleId());
                pendingTask.put("action", "pending_delete");
                pendingTask.put("requested_by", currentUserId);
                pendingTask.put("requested_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

                pendingTasksReference.child(pendingTaskId).setValue(pendingTask)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "Yêu cầu xóa đã được gửi", Toast.LENGTH_SHORT).show();
                            // Cập nhật trạng thái trên bảng tasks
                            databaseReference.child(task.getId()).child("status").setValue("pending_delete")
                                    .addOnSuccessListener(aVoid2 -> {
                                        android.util.Log.d("ItemTaskAdapter", "Task " + task.getTitle() + " đã chuyển sang trạng thái pending_delete");
                                    })
                                    .addOnFailureListener(e -> {
                                        android.util.Log.e("ItemTaskAdapter", "Lỗi khi cập nhật trạng thái pending_delete cho task " + task.getTitle() + ": " + e.getMessage());
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "Lỗi khi gửi yêu cầu xóa: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            }
            dialog.dismiss();
        });

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    // Phương thức để ẩn bàn phím khi chạm ngoài EditText
    private void setupHideKeyboard(View view) {
        if (!(view instanceof EditText)) {
            view.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    hideKeyboard(v);
                }
                return false;
            });
        }

        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupHideKeyboard(innerView);
            }
        }
    }

    // Phương thức để ẩn bàn phím khi nhấn "Enter"
    private void setupEditTextHideKeyboardOnEnter(EditText editText) {
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO ||
                    actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getAction() == android.view.KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
                hideKeyboard(editText);
                return true;
            }
            return false;
        });
    }

    // Phương thức ẩn bàn phím
    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView taskText;
        ImageView taskOptions;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.task_checkbox);
            taskText = itemView.findViewById(R.id.task_text);
            taskOptions = itemView.findViewById(R.id.task_options);
        }
    }

    private void applyStrikeThrough(TextView textView, boolean isChecked) {
        if (isChecked) {
            textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            textView.setPaintFlags(textView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
    }
}