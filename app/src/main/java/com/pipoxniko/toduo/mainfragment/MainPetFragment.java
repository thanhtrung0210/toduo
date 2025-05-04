package com.pipoxniko.toduo.mainfragment;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.pipoxniko.toduo.R;
import com.pipoxniko.toduo.model.FoodItem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainPetFragment extends Fragment {

    private EditText editTextFoodName, editTextDescription, editTextDate;
    private Button buttonPickDate, buttonPickImage, buttonSave, buttonViewList;
    private ImageView imageViewPreview;
    private String imageBase64 = "";
    private DatabaseReference databaseReference;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private View mView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_main_pet, container, false);
        initViews();
        setupFirebase();
        setupImagePicker();
        setupListeners();
        return mView;
    }

    // Ánh xa view
    private void initViews() {
        editTextFoodName = mView.findViewById(R.id.editTextFoodName);
        editTextDescription = mView.findViewById(R.id.editTextDescription);
        editTextDate = mView.findViewById(R.id.editTextDate);
        buttonPickDate = mView.findViewById(R.id.buttonPickDate);
        buttonPickImage = mView.findViewById(R.id.buttonPickImage);
        buttonSave = mView.findViewById(R.id.buttonSave);
        buttonViewList = mView.findViewById(R.id.buttonViewList);
        imageViewPreview = mView.findViewById(R.id.imageViewPreview);
    }

    private void setupFirebase() {
        databaseReference = FirebaseDatabase.getInstance().getReference("Thi");
    }

    // Khởi tạo image picker
    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
                    imageViewPreview.setImageBitmap(bitmap);
                    imageViewPreview.setVisibility(View.VISIBLE);
                    imageBase64 = bitmapToBase64(bitmap);
                } catch (IOException e) {
                    Toast.makeText(getContext(), "Lỗi khi chọn ảnh", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Khởi tạo listener
    private void setupListeners() {
        buttonPickDate.setOnClickListener(v -> showDatePicker());
        buttonPickImage.setOnClickListener(v -> pickImage());
        buttonSave.setOnClickListener(v -> saveFoodItem());
        buttonViewList.setOnClickListener(v -> navigateToListFragment());
    }

    // Hiển thị date picker
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    editTextDate.setText(sdf.format(selectedDate.getTime()));
                }, year, month, day);
        datePickerDialog.show();
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    // Chuyển ảnh sang base64
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    // Lưu dữ liệu vào database
    private void saveFoodItem() {
        String name = editTextFoodName.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String date = editTextDate.getText().toString().trim();

        if (name.isEmpty() || description.isEmpty() || date.isEmpty() || imageBase64.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        String id = databaseReference.push().getKey();
        FoodItem foodItem = new FoodItem(id, name, description, date, imageBase64);
        databaseReference.child(id).setValue(foodItem)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Lưu thành công", Toast.LENGTH_SHORT).show();
                    clearInputs();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void clearInputs() {
        editTextFoodName.setText("");
        editTextDescription.setText("");
        editTextDate.setText("");
        imageViewPreview.setVisibility(View.GONE);
        imageBase64 = "";
    }

    private void navigateToListFragment() {
        Fragment listFragment = new FoodListFragment();
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.main_activity, listFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}