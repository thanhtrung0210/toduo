package com.pipoxniko.toduo.mainfragment;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.pipoxniko.toduo.R;
import com.pipoxniko.toduo.model.PetItem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class PetInputFragment extends Fragment {

    private EditText petNameInput, petDescriptionInput, petDateInput;
    private ImageView petImagePreview;
    private Button selectImageButton, saveButton;
    private Bitmap selectedImageBitmap;
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Khởi tạo giao diện từ file XML
        View view = inflater.inflate(R.layout.fragment_pet_input, container, false);

        // Ánh xạ các thành phần giao diện
        petNameInput = view.findViewById(R.id.pet_name_input);
        petDescriptionInput = view.findViewById(R.id.pet_description_input);
        petDateInput = view.findViewById(R.id.pet_date_input);
        petImagePreview = view.findViewById(R.id.pet_image_preview);
        selectImageButton = view.findViewById(R.id.select_image_button);
        saveButton = view.findViewById(R.id.save_button);

        // Thiết lập DatePicker cho trường ngày
        petDateInput.setOnClickListener(v -> showDatePickerDialog());

        // Xử lý chọn ảnh từ thư viện
        selectImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        // Xử lý lưu dữ liệu vào Firebase
        saveButton.setOnClickListener(v -> savePetData());

        return view;
    }

    // Hiển thị dialog chọn ngày
    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    petDateInput.setText(sdf.format(selectedDate.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    // Xử lý kết quả chọn ảnh
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                selectedImageBitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), imageUri);
                petImagePreview.setImageBitmap(selectedImageBitmap);
            } catch (IOException e) {
                Toast.makeText(requireContext(), "Lỗi khi tải ảnh", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Lưu dữ liệu vào Firebase
    private void savePetData() {
        String name = petNameInput.getText().toString().trim();
        String description = petDescriptionInput.getText().toString().trim();
        String date = petDateInput.getText().toString().trim();

        // Kiểm tra dữ liệu đầu vào
        if (name.isEmpty() || description.isEmpty() || date.isEmpty() || selectedImageBitmap == null) {
            Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // Chuyển ảnh thành Base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        selectedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        String imageBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

        // Tạo đối tượng PetItem
        PetItem petItem = new PetItem(name, description, date, imageBase64);

        // Lưu vào Firebase
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Thi");
        String petId = databaseReference.push().getKey();
        databaseReference.child(petId).setValue(petItem).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(requireContext(), "Lưu thành công", Toast.LENGTH_SHORT).show();
                // Xóa dữ liệu sau khi lưu
                petNameInput.setText("");
                petDescriptionInput.setText("");
                petDateInput.setText("");
                petImagePreview.setImageResource(R.drawable.pet_image);
                selectedImageBitmap = null;
            } else {
                Toast.makeText(requireContext(), "Lưu thất bại", Toast.LENGTH_SHORT).show();
            }
        });
    }
}