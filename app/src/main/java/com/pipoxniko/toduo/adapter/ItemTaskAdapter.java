package com.pipoxniko.toduo.adapter;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Color;
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
    private Context context;
    private DatabaseReference databaseReference;
    private DatabaseReference pendingTasksReference;
    private String coupleId;
    private String currentUserId;
    private PopupWindow categoryPopupWindow;
    private PopupWindow assignmentPopupWindow;
    private ItemCategory selectedCategory;
    private String selectedAssignment;
    private String selectedDate;
    private String selectedTime;
    private List<ItemCategory> cachedCategories;
    private List<String> cachedAssignments;

    public ItemTaskAdapter(List<ItemTask> taskList, Context context) {
        this.taskList = taskList;
        this.context = context;
        this.databaseReference = FirebaseDatabase.getInstance().getReference("TODUO").child("tasks");
        this.pendingTasksReference = FirebaseDatabase.getInstance().getReference("TODUO").child("pending_tasks");
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.coupleId = "";
        this.cachedCategories = new ArrayList<>();
        this.cachedAssignments = new ArrayList<>();
        loadCategories();
        loadAssignments();
    }

    public void setCoupleId(String coupleId) {
        this.coupleId = coupleId;
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
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(task.isCompleted());
        holder.taskText.setText(task.getTitle() != null ? task.getTitle() : "Không có tiêu đề");

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setCompleted(isChecked);
            databaseReference.child(task.getId()).child("completed").setValue(isChecked)
                    .addOnSuccessListener(aVoid -> Toast.makeText(context, "Cập nhật trạng thái thành công", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        holder.checkBox.setChecked(!isChecked);
                        task.setCompleted(!isChecked);
                    });
        });

        holder.taskOptions.setOnClickListener(v -> showTaskOptionsPopup(holder.taskOptions, task));
    }

    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    private void showTaskOptionsPopup(View anchorView, ItemTask task) {
        View popupView = LayoutInflater.from(context).inflate(R.layout.todolist_task_options_popup, null);
        PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(8f);
        popupWindow.setOutsideTouchable(true);

        TextView optionViewDetails = popupView.findViewById(R.id.task_option_view_details);
        TextView optionEdit = popupView.findViewById(R.id.task_option_edit);
        TextView optionDelete = popupView.findViewById(R.id.task_option_delete);

        if ("pending_edit".equals(task.getStatus()) || "pending_delete".equals(task.getStatus())) {
            optionViewDetails.setVisibility(View.GONE);
            optionEdit.setVisibility(View.GONE);
            optionDelete.setVisibility(View.GONE);

            if ("pending_edit".equals(task.getStatus())) {
                optionViewDetails.setText("Xem yêu cầu sửa");
                optionViewDetails.setVisibility(View.VISIBLE);
                optionViewDetails.setOnClickListener(v -> showEditRequestDialog(task));
            } else if ("pending_delete".equals(task.getStatus())) {
                optionViewDetails.setText("Xem yêu cầu xóa");
                optionViewDetails.setVisibility(View.VISIBLE);
                optionViewDetails.setOnClickListener(v -> showDeleteRequestDialog(task));
            }
        } else {
            optionViewDetails.setVisibility(View.VISIBLE);
            optionEdit.setVisibility(View.VISIBLE);
            optionDelete.setVisibility(View.VISIBLE);

            optionViewDetails.setOnClickListener(v -> showTaskDetailsDialog(task));
            optionEdit.setOnClickListener(v -> showEditTaskDialog(task));
            optionDelete.setOnClickListener(v -> showDeleteConfirmationDialog(task));
        }

        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        popupWindow.showAtLocation(anchorView, android.view.Gravity.NO_GRAVITY, location[0], location[1] - popupView.getHeight());
    }

    private void showTaskDetailsDialog(ItemTask task) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.todolist_dialog_task_details, null);
        AlertDialog dialog = new AlertDialog.Builder(context).setView(dialogView).create();
        setupHideKeyboard(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.task_detail_txt_title);
        TextView tvDescription = dialogView.findViewById(R.id.task_detail_txt_description);
        TextView tvDeadline = dialogView.findViewById(R.id.task_detail_txt_deadline);
        TextView tvCategory = dialogView.findViewById(R.id.task_detail_txt_category);
        TextView tvAssignment = dialogView.findViewById(R.id.task_detail_txt_assignment);
        Button btnClose = dialogView.findViewById(R.id.task_detail_btn_close);

        // Hiển thị dữ liệu từ task
        tvTitle.setText("Mục tiêu: " + (task.getTitle() != null ? task.getTitle() : "Không có"));
        tvDescription.setText("Mô tả: " + (task.getDescription() != null ? task.getDescription() : "Không có"));
        tvAssignment.setText("Phân công: " + (task.getAssignment() != null ? task.getAssignment() : "Không có"));

        String deadlineText = "Hạn chót: Không có";
        if (task.getDeadline() != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
                deadlineText = "Hạn chót: " + outputFormat.format(inputFormat.parse(task.getDeadline()));
            } catch (Exception e) {
                deadlineText = "Hạn chót: Lỗi định dạng";
            }
        }
        tvDeadline.setText(deadlineText);

        if (task.getCategoryId() != null) {
            FirebaseDatabase.getInstance().getReference("TODUO").child("task_categories").child(task.getCategoryId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
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
        AlertDialog dialog = new AlertDialog.Builder(context).setView(dialogView).create();
        setupHideKeyboard(dialogView);

        // Phần thông tin hiện tại
        TextView tvCurrentTitle = dialogView.findViewById(R.id.task_edit_txt_title);
        TextView tvCurrentDescription = dialogView.findViewById(R.id.task_edit_txt_description);
        TextView tvCurrentDeadline = dialogView.findViewById(R.id.task_edit_txt_deadline);
        TextView tvCurrentCategory = dialogView.findViewById(R.id.task_edit_txt_category);
        TextView tvCurrentAssignment = dialogView.findViewById(R.id.task_edit_txt_assignment);

        // Phần chỉnh sửa
        EditText editTitle = dialogView.findViewById(R.id.task_edit_edit_title);
        EditText editDescription = dialogView.findViewById(R.id.task_edit_edit_description);
        ImageView editCategoryIcon = dialogView.findViewById(R.id.task_edit_edit_category);
        ImageView editAssignmentIcon = dialogView.findViewById(R.id.task_edit_edit_assignment);
        ImageView editDeadlineIcon = dialogView.findViewById(R.id.task_edit_edit_deadline);
        Button btnCancel = dialogView.findViewById(R.id.task_edit_btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.task_edit_btn_save);

        // Hiển thị thông tin hiện tại
        tvCurrentTitle.setText("Mục tiêu: " + (task.getTitle() != null ? task.getTitle() : "Không có"));
        tvCurrentDescription.setText("Mô tả: " + (task.getDescription() != null ? task.getDescription() : "Không có"));
        tvCurrentAssignment.setText("Phân công: " + (task.getAssignment() != null ? task.getAssignment() : "Không có"));

        String deadlineText = "Hạn chót: Không có";
        if (task.getDeadline() != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
                deadlineText = "Hạn chót: " + outputFormat.format(inputFormat.parse(task.getDeadline()));
            } catch (Exception e) {
                deadlineText = "Hạn chót: Lỗi định dạng";
            }
        }
        tvCurrentDeadline.setText(deadlineText);

        if (task.getCategoryId() != null) {
            FirebaseDatabase.getInstance().getReference("TODUO").child("task_categories").child(task.getCategoryId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String categoryName = snapshot.child("name").getValue(String.class);
                                tvCurrentCategory.setText("Phân loại: " + (categoryName != null ? categoryName : "Không có"));
                            } else {
                                tvCurrentCategory.setText("Phân loại: Không có");
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            tvCurrentCategory.setText("Phân loại: Lỗi khi lấy dữ liệu");
                        }
                    });
        } else {
            tvCurrentCategory.setText("Phân loại: Không có");
        }

        // Điền dữ liệu vào phần chỉnh sửa
        editTitle.setText(task.getTitle() != null ? task.getTitle() : "");
        editDescription.setText(task.getDescription() != null ? task.getDescription() : "");

        // Khởi tạo giá trị ban đầu
        selectedCategory = null;
        selectedAssignment = task.getAssignment();
        selectedDate = null;
        selectedTime = null;
        if (task.getDeadline() != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                Date date = inputFormat.parse(task.getDeadline());
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                selectedDate = dateFormat.format(date);
                selectedTime = timeFormat.format(date);
            } catch (Exception e) {
                Toast.makeText(context, "Lỗi định dạng thời gian: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

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

            String pendingTaskId = pendingTasksReference.push().getKey();
            if (pendingTaskId != null) {
                Map<String, Object> updatedData = new HashMap<>();
                updatedData.put("title", newTitle);
                updatedData.put("description", newDescription.isEmpty() ? null : newDescription);
                updatedData.put("category_id", selectedCategory != null ? selectedCategory.getId() : task.getCategoryId());
                updatedData.put("assignment", selectedAssignment != null ? selectedAssignment : task.getAssignment());
                updatedData.put("deadline", getFormattedDeadline() != null ? getFormattedDeadline() : task.getDeadline());

                Map<String, Object> pendingTask = new HashMap<>();
                pendingTask.put("task_id", task.getId());
                pendingTask.put("couple_id", coupleId);
                pendingTask.put("action", "pending_edit");
                pendingTask.put("requested_by", currentUserId);
                pendingTask.put("updated_data", updatedData);
                pendingTask.put("requested_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

                pendingTasksReference.child(pendingTaskId).setValue(pendingTask)
                        .addOnSuccessListener(aVoid -> {
                            databaseReference.child(task.getId()).child("status").setValue("pending_edit")
                                    .addOnSuccessListener(aVoid2 -> {
                                        Toast.makeText(context, "Yêu cầu chỉnh sửa đã được gửi", Toast.LENGTH_SHORT).show();
                                        notifyDataSetChanged();
                                    });
                        })
                        .addOnFailureListener(e -> Toast.makeText(context, "Lỗi khi gửi yêu cầu: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
            dialog.dismiss();
        });

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    private void showDeleteConfirmationDialog(ItemTask task) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.todolist_dialog_confirm_delete, null);
        AlertDialog dialog = new AlertDialog.Builder(context).setView(dialogView).create();
        setupHideKeyboard(dialogView);

        Button btnNo = dialogView.findViewById(R.id.task_delete_btn_no);
        Button btnYes = dialogView.findViewById(R.id.task_delete_btn_yes);

        btnNo.setOnClickListener(v -> dialog.dismiss());

        btnYes.setOnClickListener(v -> {
            String pendingTaskId = pendingTasksReference.push().getKey();
            if (pendingTaskId != null) {
                Map<String, Object> pendingTask = new HashMap<>();
                pendingTask.put("task_id", task.getId());
                pendingTask.put("couple_id", coupleId);
                pendingTask.put("action", "pending_delete");
                pendingTask.put("requested_by", currentUserId);
                pendingTask.put("requested_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

                pendingTasksReference.child(pendingTaskId).setValue(pendingTask)
                        .addOnSuccessListener(aVoid -> {
                            databaseReference.child(task.getId()).child("status").setValue("pending_delete")
                                    .addOnSuccessListener(aVoid2 -> {
                                        Toast.makeText(context, "Yêu cầu xóa đã được gửi", Toast.LENGTH_SHORT).show();
                                        notifyDataSetChanged();
                                    });
                        })
                        .addOnFailureListener(e -> Toast.makeText(context, "Lỗi khi gửi yêu cầu: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
            dialog.dismiss();
        });

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    private void showEditRequestDialog(ItemTask task) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.todolist_dialog_pending_edit, null);
        AlertDialog dialog = new AlertDialog.Builder(context).setView(dialogView).create();
        setupHideKeyboard(dialogView);

        TextView currentTitle = dialogView.findViewById(R.id.task_pending_edit_current_title);
        TextView currentDescription = dialogView.findViewById(R.id.task_pending_edit_current_description);
        TextView currentDeadline = dialogView.findViewById(R.id.task_pending_edit_current_deadline);
        TextView currentCategory = dialogView.findViewById(R.id.task_pending_edit_current_category);
        TextView currentAssignment = dialogView.findViewById(R.id.task_pending_edit_current_assignment);
        TextView newTitle = dialogView.findViewById(R.id.task_pending_edit_new_title);
        TextView newDescription = dialogView.findViewById(R.id.task_pending_edit_new_description);
        TextView newDeadline = dialogView.findViewById(R.id.task_pending_edit_new_deadline);
        TextView newCategory = dialogView.findViewById(R.id.task_pending_edit_new_category);
        TextView newAssignment = dialogView.findViewById(R.id.task_pending_edit_new_assignment);
        Button btnConfirm = dialogView.findViewById(R.id.task_pending_edit_btn_confirm);
        Button btnReject = dialogView.findViewById(R.id.task_pending_edit_btn_reject);

        // Hiển thị dữ liệu hiện tại
        currentTitle.setText("Mục tiêu: " + (task.getTitle() != null ? task.getTitle() : "Không có"));
        currentDescription.setText("Mô tả: " + (task.getDescription() != null ? task.getDescription() : "Không có"));
        currentAssignment.setText("Phân công: " + (task.getAssignment() != null ? task.getAssignment() : "Không có"));

        String currentDeadlineText = "Hạn chót: Không có";
        if (task.getDeadline() != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
                currentDeadlineText = "Hạn chót: " + outputFormat.format(inputFormat.parse(task.getDeadline()));
            } catch (Exception e) {
                currentDeadlineText = "Hạn chót: Lỗi định dạng";
            }
        }
        currentDeadline.setText(currentDeadlineText);

        if (task.getCategoryId() != null) {
            FirebaseDatabase.getInstance().getReference("TODUO").child("task_categories").child(task.getCategoryId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot categorySnapshot) {
                            if (categorySnapshot.exists()) {
                                String categoryName = categorySnapshot.child("name").getValue(String.class);
                                currentCategory.setText("Phân loại: " + (categoryName != null ? categoryName : "Không có"));
                            } else {
                                currentCategory.setText("Phân loại: Không có");
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            currentCategory.setText("Phân loại: Lỗi khi lấy dữ liệu");
                        }
                    });
        } else {
            currentCategory.setText("Phân loại: Không có");
        }

        // Lấy và hiển thị dữ liệu mới từ pending_tasks
        pendingTasksReference.orderByChild("task_id").equalTo(task.getId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Map<String, Object> updatedData = (Map<String, Object>) data.child("updated_data").getValue();
                            if (updatedData != null) {
                                newTitle.setText("Mục tiêu: " + (updatedData.get("title") != null ? (String) updatedData.get("title") : "Không thay đổi"));
                                newDescription.setText("Mô tả: " + (updatedData.get("description") != null ? (String) updatedData.get("description") : "Không thay đổi"));
                                newAssignment.setText("Phân công: " + (updatedData.get("assignment") != null ? (String) updatedData.get("assignment") : "Không thay đổi"));

                                String newDeadlineText = "Hạn chót: Không thay đổi";
                                if (updatedData.get("deadline") != null) {
                                    try {
                                        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                        SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
                                        newDeadlineText = "Hạn chót: " + outputFormat.format(inputFormat.parse((String) updatedData.get("deadline")));
                                    } catch (Exception e) {
                                        newDeadlineText = "Hạn chót: Lỗi định dạng";
                                    }
                                }
                                newDeadline.setText(newDeadlineText);

                                if (updatedData.get("category_id") != null) {
                                    FirebaseDatabase.getInstance().getReference("TODUO").child("task_categories").child((String) updatedData.get("category_id"))
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot categorySnapshot) {
                                                    String categoryName = categorySnapshot.child("name").getValue(String.class);
                                                    newCategory.setText("Phân loại: " + (categoryName != null ? categoryName : "Không thay đổi"));
                                                }
                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {
                                                    newCategory.setText("Phân loại: Lỗi khi lấy dữ liệu");
                                                }
                                            });
                                } else {
                                    newCategory.setText("Phân loại: Không thay đổi");
                                }
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(context, "Lỗi khi lấy dữ liệu yêu cầu: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

        btnConfirm.setOnClickListener(v -> {
            pendingTasksReference.orderByChild("task_id").equalTo(task.getId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                Map<String, Object> updatedData = (Map<String, Object>) data.child("updated_data").getValue();
                                if (updatedData != null) {
                                    databaseReference.child(task.getId()).updateChildren(updatedData)
                                            .addOnSuccessListener(aVoid -> {
                                                databaseReference.child(task.getId()).child("status").setValue("normal");
                                                data.getRef().removeValue();
                                                Toast.makeText(context, "Chỉnh sửa được xác nhận", Toast.LENGTH_SHORT).show();
                                                notifyDataSetChanged();
                                            });
                                }
                            }
                            dialog.dismiss();
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(context, "Lỗi xác nhận: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        btnReject.setOnClickListener(v -> {
            pendingTasksReference.orderByChild("task_id").equalTo(task.getId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                databaseReference.child(task.getId()).child("status").setValue("normal");
                                data.getRef().removeValue();
                                Toast.makeText(context, "Chỉnh sửa bị từ chối", Toast.LENGTH_SHORT).show();
                                notifyDataSetChanged();
                            }
                            dialog.dismiss();
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(context, "Lỗi từ chối: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    private void showDeleteRequestDialog(ItemTask task) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.todolist_dialog_pending_delete, null);
        AlertDialog dialog = new AlertDialog.Builder(context).setView(dialogView).create();
        setupHideKeyboard(dialogView);

        TextView title = dialogView.findViewById(R.id.task_pending_delete_title);
        TextView description = dialogView.findViewById(R.id.task_pending_delete_description);
        TextView deadline = dialogView.findViewById(R.id.task_pending_delete_deadline);
        TextView category = dialogView.findViewById(R.id.task_pending_delete_category);
        TextView assignment = dialogView.findViewById(R.id.task_pending_delete_assignment);
        Button btnConfirm = dialogView.findViewById(R.id.task_pending_delete_btn_confirm);
        Button btnReject = dialogView.findViewById(R.id.task_pending_delete_btn_reject);

        title.setText("Mục tiêu: " + (task.getTitle() != null ? task.getTitle() : "Không có"));
        description.setText("Mô tả: " + (task.getDescription() != null ? task.getDescription() : "Không có"));
        assignment.setText("Phân công: " + (task.getAssignment() != null ? task.getAssignment() : "Không có"));

        String deadlineText = "Hạn chót: Không có";
        if (task.getDeadline() != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
                deadlineText = "Hạn chót: " + outputFormat.format(inputFormat.parse(task.getDeadline()));
            } catch (Exception e) {
                deadlineText = "Hạn chót: Lỗi định dạng";
            }
        }
        deadline.setText(deadlineText);

        if (task.getCategoryId() != null) {
            FirebaseDatabase.getInstance().getReference("TODUO").child("task_categories").child(task.getCategoryId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String categoryName = snapshot.child("name").getValue(String.class);
                                category.setText("Phân loại: " + (categoryName != null ? categoryName : "Không có"));
                            } else {
                                category.setText("Phân loại: Không có");
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            category.setText("Phân loại: Lỗi khi lấy dữ liệu");
                        }
                    });
        } else {
            category.setText("Phân loại: Không có");
        }

        btnConfirm.setOnClickListener(v -> {
            databaseReference.child(task.getId()).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        pendingTasksReference.orderByChild("task_id").equalTo(task.getId())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        for (DataSnapshot data : snapshot.getChildren()) {
                                            data.getRef().removeValue();
                                        }
                                        taskList.remove(task);
                                        notifyDataSetChanged();
                                        Toast.makeText(context, "Task đã được xóa", Toast.LENGTH_SHORT).show();
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Toast.makeText(context, "Lỗi xóa: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    });
            dialog.dismiss();
        });

        btnReject.setOnClickListener(v -> {
            databaseReference.child(task.getId()).child("status").setValue("normal")
                    .addOnSuccessListener(aVoid -> {
                        pendingTasksReference.orderByChild("task_id").equalTo(task.getId())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        for (DataSnapshot data : snapshot.getChildren()) {
                                            data.getRef().removeValue();
                                        }
                                        notifyDataSetChanged();
                                        Toast.makeText(context, "Yêu cầu xóa bị từ chối", Toast.LENGTH_SHORT).show();
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Toast.makeText(context, "Lỗi từ chối: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    });
            dialog.dismiss();
        });

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    private void loadCategories() {
        FirebaseDatabase.getInstance().getReference("TODUO").child("task_categories")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        cachedCategories.clear();
                        for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                            String categoryId = categorySnapshot.getKey();
                            String categoryCoupleId = categorySnapshot.child("couple_id").getValue(String.class);
                            String name = categorySnapshot.child("name").getValue(String.class);
                            if (categoryCoupleId != null && categoryCoupleId.equals(coupleId)) {
                                cachedCategories.add(new ItemCategory(categoryId, categoryCoupleId, name));
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(context, "Lỗi khi lấy danh sách phân loại", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loadAssignments() {
        FirebaseDatabase.getInstance().getReference("TODUO").child("users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        cachedAssignments.clear();
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

                        cachedAssignments.add(user1Name);
                        cachedAssignments.add(user2Name);
                        cachedAssignments.add("Cả hai");
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(context, "Lỗi khi lấy danh sách người dùng", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showCategoryDialog(View dialogView, View anchorView) {
        View popupView = LayoutInflater.from(context).inflate(R.layout.todolist_dialog_select_category, null);
        RecyclerView categoryList = popupView.findViewById(R.id.todolist_category_list);

        categoryPopupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        categoryPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        categoryPopupWindow.setElevation(8f);
        categoryPopupWindow.setOutsideTouchable(true);

        categoryList.setLayoutManager(new LinearLayoutManager(context));
        CategoryAdapter adapter = new CategoryAdapter(cachedCategories, category -> {
            selectedCategory = category;
            categoryPopupWindow.dismiss();
        });
        adapter.setSelectedCategory(selectedCategory);
        categoryList.setAdapter(adapter);

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupHeight = popupView.getMeasuredHeight();
        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        categoryPopupWindow.showAtLocation(anchorView, android.view.Gravity.NO_GRAVITY, location[0] + anchorView.getWidth(), location[1] - popupHeight - anchorView.getHeight());
    }

    private void showAssignmentDialog(View dialogView, View anchorView) {
        View popupView = LayoutInflater.from(context).inflate(R.layout.todolist_dialog_select_assignment, null);
        RecyclerView assignmentList = popupView.findViewById(R.id.assignment_list);

        assignmentPopupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        assignmentPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        assignmentPopupWindow.setElevation(8f);
        assignmentPopupWindow.setOutsideTouchable(true);

        assignmentList.setLayoutManager(new LinearLayoutManager(context));
        AssignmentAdapter adapter = new AssignmentAdapter(cachedAssignments, assignment -> {
            selectedAssignment = assignment;
            assignmentPopupWindow.dismiss();
        });
        adapter.setSelectedAssignment(selectedAssignment);
        assignmentList.setAdapter(adapter);

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupHeight = popupView.getMeasuredHeight();
        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        assignmentPopupWindow.showAtLocation(anchorView, android.view.Gravity.NO_GRAVITY, location[0] + anchorView.getWidth(), location[1] - popupHeight - anchorView.getHeight());
    }

    private void showDeadlineDialog(View dialogView) {
        View deadlineView = LayoutInflater.from(context).inflate(R.layout.todolist_dialog_set_deadline, null);
        AlertDialog deadlineDialog = new AlertDialog.Builder(context).setView(deadlineView).create();
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
            datePickerContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                tvSelectedDate.setText("Chưa chọn");
                selectedDate = null;
            }
        });

        switchSelectTime.setOnCheckedChangeListener((buttonView, isChecked) -> {
            timePickerContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                tvSelectedTime.setText("Chưa chọn");
                selectedTime = null;
            }
        });

        btnSelectDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(context, (view, year, month, day) -> {
                selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year);
                tvSelectedDate.setText(selectedDate);
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnSelectTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new TimePickerDialog(context, (view, hour, minute) -> {
                selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                tvSelectedTime.setText(selectedTime);
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        });

        btnCancel.setOnClickListener(v -> deadlineDialog.dismiss());
        btnConfirm.setOnClickListener(v -> deadlineDialog.dismiss());

        deadlineDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        deadlineDialog.show();
    }

    private String getFormattedDeadline() {
        if (selectedDate != null) {
            try {
                SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date date = inputDateFormat.parse(selectedDate);
                String time = (selectedTime != null) ? selectedTime : "07:00";
                SimpleDateFormat inputTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                Date fullDateTime = inputTimeFormat.parse(selectedDate + " " + time);
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                return outputFormat.format(fullDateTime);
            } catch (Exception e) {
                Toast.makeText(context, "Lỗi định dạng thời gian: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
        return null;
    }

    private void setupHideKeyboard(View view) {
        if (!(view instanceof EditText)) {
            view.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) hideKeyboard(v);
                return false;
            });
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setupHideKeyboard(((ViewGroup) view).getChildAt(i));
            }
        }
    }

    private void setupEditTextHideKeyboardOnEnter(EditText editText) {
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard(editText);
                return true;
            }
            return false;
        });
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
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
}