package com.pipoxniko.toduo.mainfragment;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pipoxniko.toduo.R;
import com.pipoxniko.toduo.model.PetItem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PetListFragment extends Fragment {

    private RecyclerView petRecyclerView;
    private PetAdapter petAdapter;
    private List<PetItem> petList;
    private DatabaseReference databaseReference;

    private String selectedImageBase64;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Khởi tạo giao diện từ file XML
        View view = inflater.inflate(R.layout.fragment_pet_list, container, false);

        // Khởi tạo RecyclerView và danh sách
        petRecyclerView = view.findViewById(R.id.pet_recycler_view);
        petRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        petList = new ArrayList<>();
        petAdapter = new PetAdapter(petList);
        petRecyclerView.setAdapter(petAdapter);

        // Kết nối Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("Thi");
        loadPetData();

        return view;
    }

    // Tải dữ liệu từ Firebase
    private void loadPetData() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                petList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    PetItem petItem = dataSnapshot.getValue(PetItem.class);
                    petItem.setId(dataSnapshot.getKey());
                    petList.add(petItem);
                }
                petAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Lỗi khi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Adapter cho RecyclerView
    private class PetAdapter extends RecyclerView.Adapter<PetAdapter.PetViewHolder> {

        private List<PetItem> petList;

        public PetAdapter(List<PetItem> petList) {
            this.petList = petList;
        }

        @NonNull
        @Override
        public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pet, parent, false);
            return new PetViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PetViewHolder holder, int position) {
            PetItem petItem = petList.get(position);
            holder.petName.setText(petItem.getName());
            holder.petDescription.setText(petItem.getDescription());
            holder.petDate.setText(petItem.getDate());

            // Decode Base64 thành Bitmap để hiển thị ảnh
            byte[] decodedString = Base64.decode(petItem.getImageBase64(), Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            holder.petImage.setImageBitmap(bitmap);

            // Xử lý nút sửa
            holder.editButton.setOnClickListener(v -> showEditDialog(petItem));

            // Xử lý nút xóa
            holder.deleteButton.setOnClickListener(v -> showDeleteConfirmDialog(petItem));
        }

        @Override
        public int getItemCount() {
            return petList.size();
        }

        class PetViewHolder extends RecyclerView.ViewHolder {
            ImageView petImage;
            TextView petName, petDescription, petDate;
            Button editButton, deleteButton;

            public PetViewHolder(@NonNull View itemView) {
                super(itemView);
                petImage = itemView.findViewById(R.id.item_image_view);
                petName = itemView.findViewById(R.id.item_name_text_view);
                petDescription = itemView.findViewById(R.id.item_description_text_view);
                petDate = itemView.findViewById(R.id.item_date_text_view);
                editButton = itemView.findViewById(R.id.edit_button);
                deleteButton = itemView.findViewById(R.id.delete_button);
            }
        }
    }

    // Hiển thị dialog sửa thông tin
    private void showEditDialog(PetItem petItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_pet, null);
        builder.setView(dialogView);

        EditText editPetName = dialogView.findViewById(R.id.edit_pet_name);
        EditText editPetDescription = dialogView.findViewById(R.id.edit_pet_description);
        EditText editPetDate = dialogView.findViewById(R.id.edit_pet_date);
        ImageView editPetImage = dialogView.findViewById(R.id.edit_pet_image);
        Button editSelectImageButton = dialogView.findViewById(R.id.edit_select_image_button);
        Button saveEditButton = dialogView.findViewById(R.id.save_edit_button);

        // Điền dữ liệu hiện tại
        editPetName.setText(petItem.getName());
        editPetDescription.setText(petItem.getDescription());
        editPetDate.setText(petItem.getDate());
        byte[] decodedString = Base64.decode(petItem.getImageBase64(), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        editPetImage.setImageBitmap(bitmap);

        // Thiết lập DatePicker
        editPetDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year, month, dayOfMonth);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        editPetDate.setText(sdf.format(selectedDate.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        // Xử lý chọn ảnh mới
        editSelectImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 2);
        });

        // Xử lý lưu thay đổi
        saveEditButton.setOnClickListener(v -> {
            String name = editPetName.getText().toString().trim();
            String description = editPetDescription.getText().toString().trim();
            String date = editPetDate.getText().toString().trim();

            if (name.isEmpty() || description.isEmpty() || date.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            // Cập nhật dữ liệu
            petItem.setName(name);
            petItem.setDescription(description);
            petItem.setDate(date);
            if (selectedImageBase64 != null) {
                petItem.setImageBase64(selectedImageBase64); // Cập nhật ảnh mới nếu có
            }

            // Cập nhật Firebase
            databaseReference.child(petItem.getId()).setValue(petItem).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(requireContext(), "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    selectedImageBase64 = null; // Xóa tạm sau khi lưu
                } else {
                    Toast.makeText(requireContext(), "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                }
            });
        });

        builder.create().show();
    }

    // Hiển thị dialog xác nhận xóa
    private void showDeleteConfirmDialog(PetItem petItem) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa mục này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    databaseReference.child(petItem.getId()).removeValue().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(requireContext(), "Xóa thành công", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "Xóa thất bại", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2 && resultCode == getActivity().RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), imageUri);
                // Cập nhật ImageView trong dialog (nếu dialog vẫn đang mở)
                ImageView editPetImage = getView().getRootView().findViewById(R.id.edit_pet_image);
                if (editPetImage != null) {
                    editPetImage.setImageBitmap(bitmap);
                }
                // Chuyển bitmap thành Base64 để lưu
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                String imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
                // Lưu Base64 tạm thời (sẽ cập nhật vào petItem khi nhấn Lưu)
                this.selectedImageBase64 = imageBase64; // Thêm biến tạm trong class
            } catch (IOException e) {
                Toast.makeText(requireContext(), "Lỗi khi tải ảnh", Toast.LENGTH_SHORT).show();
            }
        }
    }
}