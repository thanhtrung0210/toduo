package com.pipoxniko.toduo.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pipoxniko.toduo.R;

import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editTextEmailOrPhone, editTextPassword;
    private TextInputLayout emailOrPhoneLayout, passwordLayout;
    private ImageView iconPasswordToggle, icEmailOrPhoneWarning, icPasswordWarning;
    private TextView signUpText;
    private Button loginButton;
    private View rootView; // View gốc để bắt sự kiện chạm
    private boolean isPasswordVisible = false;
    private FirebaseAuth auth;
    private DatabaseReference databaseReference;
    private String emailOrPhone, password; // Lưu thông tin người dùng
    private AlertDialog loadingDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Khởi tạo Firebase Auth và Database
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("TODUO/users");

        // Kiểm tra trạng thái đăng nhập
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            // Nếu đã đăng nhập, chuyển thẳng sang MainActivity
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        // Ánh xạ View gốc
        rootView = findViewById(R.id.login_activity);

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Ánh xạ view
        editTextEmailOrPhone = findViewById(R.id.login_txt_emailorphone);
        editTextPassword = findViewById(R.id.register_txt_password);
        emailOrPhoneLayout = findViewById(R.id.login_txt_emailorphone_layout);
        passwordLayout = findViewById(R.id.register_txt_password_layout);
        iconPasswordToggle = findViewById(R.id.register_ic_password_toggle);
        icEmailOrPhoneWarning = findViewById(R.id.icEmailOrPhoneWarning);
        icPasswordWarning = findViewById(R.id.icPasswordWarning);
        signUpText = findViewById(R.id.login_register_text);
        loginButton = findViewById(R.id.login_btn_login);

        // Khởi tạo màn hình loading
        View loadingView = getLayoutInflater().inflate(R.layout.custom_loading_dialog, null);
        loadingDialog = new AlertDialog.Builder(this)
                .setView(loadingView)
                .setCancelable(false)
                .create();

        // Xóa background mặc định của dialog
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Bắt sự kiện chạm trên View gốc để ẩn bàn phím
        rootView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                hideKeyboard();
            }
            return false; // Trả về false để không chặn các sự kiện chạm khác
        });

        // Xử lý sự kiện click cho biểu tượng Con mắt
        iconPasswordToggle.setOnClickListener(v -> {
            if (isPasswordVisible) {
                // Ẩn mật khẩu và đổi icon thành login_password_show
                editTextPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                iconPasswordToggle.setImageResource(R.drawable.login_password_show);
                isPasswordVisible = false;
            } else {
                // Hiện mật khẩu và đổi icon thành login_password_hide
                editTextPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                iconPasswordToggle.setImageResource(R.drawable.login_password_hide);
                isPasswordVisible = true;
            }
            editTextPassword.setSelection(editTextPassword.getText().length());
        });

        // Lắng nghe sự kiện thay đổi văn bản cho ô Email hoặc Số điện thoại
        editTextEmailOrPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Không kiểm tra lỗi ở đây, chỉ xóa lỗi cũ nếu có
                icEmailOrPhoneWarning.setVisibility(View.GONE);
                emailOrPhoneLayout.setError(null);
            }
        });

        // Lắng nghe sự kiện thay đổi văn bản cho ô Mật khẩu
        editTextPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String password = s.toString();
                // Hiển thị biểu tượng ẩn/hiện mật khẩu khi có nội dung
                if (password.isEmpty()) {
                    iconPasswordToggle.setVisibility(View.GONE);
                } else {
                    iconPasswordToggle.setVisibility(View.VISIBLE);
                }
                // Xóa lỗi cũ nếu có
                icPasswordWarning.setVisibility(View.GONE);
                passwordLayout.setError(null);
            }
        });

        // Xử lý sự kiện click nút Đăng nhập
        loginButton.setOnClickListener(v -> {
            // Lấy dữ liệu từ các ô nhập liệu
            emailOrPhone = editTextEmailOrPhone.getText().toString().trim();
            password = editTextPassword.getText().toString().trim();

            // Kiểm tra dữ liệu nhập vào và hiển thị thông báo lỗi nếu cần
            boolean hasError = false;

            // Kiểm tra email hoặc số điện thoại
            if (emailOrPhone.isEmpty()) {
                emailOrPhoneLayout.setError("Email hoặc số điện thoại không được để trống");
                emailOrPhoneLayout.setEndIconMode(TextInputLayout.END_ICON_NONE); // Tắt endIcon
                emailOrPhoneLayout.setErrorIconDrawable(null); // Tắt icon lỗi mặc định
                icEmailOrPhoneWarning.setVisibility(View.VISIBLE);
                hasError = true;
            } else if (!Patterns.EMAIL_ADDRESS.matcher(emailOrPhone).matches() && !isValidPhoneNumber(emailOrPhone)) {
                emailOrPhoneLayout.setError("Email hoặc số điện thoại không hợp lệ");
                emailOrPhoneLayout.setEndIconMode(TextInputLayout.END_ICON_NONE); // Tắt endIcon
                emailOrPhoneLayout.setErrorIconDrawable(null); // Tắt icon lỗi mặc định
                icEmailOrPhoneWarning.setVisibility(View.VISIBLE);
                hasError = true;
            } else {
                icEmailOrPhoneWarning.setVisibility(View.GONE);
                emailOrPhoneLayout.setError(null);
            }

            // Kiểm tra mật khẩu
            if (password.isEmpty()) {
                passwordLayout.setError("Mật khẩu không được để trống");
                passwordLayout.setEndIconMode(TextInputLayout.END_ICON_NONE); // Tắt endIcon
                passwordLayout.setErrorIconDrawable(null); // Tắt icon lỗi mặc định
                icPasswordWarning.setVisibility(View.VISIBLE);
                hasError = true;
            } else {
                icPasswordWarning.setVisibility(View.GONE);
                passwordLayout.setError(null);
            }

            if (hasError) {
                return;
            }

            // Hiển thị loadingDialog và vô hiệu hóa nút Đăng nhập
            loadingDialog.show();
            loginButton.setEnabled(false);

            // Xử lý đăng nhập
            if (Patterns.EMAIL_ADDRESS.matcher(emailOrPhone).matches()) {
                // Đăng nhập bằng email
                handleEmailLogin(emailOrPhone);
            } else {
                // Đăng nhập bằng số điện thoại
                handlePhoneLogin();
            }
        });

        // Lấy màu colorPrimary từ theme
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.colorPrimary, typedValue, true);
        int colorPrimary = typedValue.data;

        // Xử lý dòng chữ "Chưa có tài khoản? Đăng ký ngay"
        String fullText = "Chưa có tài khoản? Đăng ký ngay";
        SpannableString spannableString = new SpannableString(fullText);

        // Tô màu và thêm sự kiện click cho phần "Đăng ký ngay"
        int start = fullText.indexOf("Đăng ký ngay");
        int end = start + "Đăng ký ngay".length();
        spannableString.setSpan(new ForegroundColorSpan(colorPrimary), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                // Chuyển sang trang đăng ký
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Áp dụng SpannableString cho TextView
        signUpText.setText(spannableString);
        signUpText.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
    }

    // Kiểm tra định dạng số điện thoại (chỉ chấp nhận bắt đầu bằng 0, 10-11 chữ số)
    private boolean isValidPhoneNumber(String phone) {
        String phonePattern = "^0\\d{9,10}$"; // Chỉ chấp nhận số bắt đầu bằng 0, tổng cộng 10-11 chữ số
        return Pattern.compile(phonePattern).matcher(phone).matches();
    }

    // Xử lý đăng nhập bằng Email
    private void handleEmailLogin(String email) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            // Đăng nhập thành công, chuyển sang MainActivity
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }
                    } else {
                        // Kiểm tra lỗi cụ thể
                        if (task.getException() != null) {
                            String errorMessage = task.getException().getMessage();
                            if (errorMessage != null) {
                                if (errorMessage.contains("The password is invalid") ||
                                        errorMessage.contains("There is no user record") || errorMessage.contains("The supplied auth credential is incorrect, malformed or has expired")) {
                                    Toast.makeText(LoginActivity.this,
                                            "Email hoặc mật khẩu không chính xác. Vui lòng thử lại.",
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(LoginActivity.this,
                                            "Đăng nhập thất bại: " + errorMessage,
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                        // Ẩn loadingDialog và kích hoạt lại nút Đăng nhập
                        loadingDialog.dismiss();
                        loginButton.setEnabled(true);
                    }
                });
    }

    // Xử lý đăng nhập bằng Số điện thoại
    private void handlePhoneLogin() {
        // Chuẩn hóa số điện thoại (thay 0 thành +84 để tra cứu)
        String phoneNumber = emailOrPhone;
        if (phoneNumber.startsWith("0")) {
            phoneNumber = "+84" + phoneNumber.substring(1);
        }

        // Tra cứu số điện thoại trong database để lấy email tương ứng
        databaseReference.orderByChild("phone").equalTo(phoneNumber)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Tìm thấy số điện thoại
                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                String email = userSnapshot.child("email").getValue(String.class);
                                if (email != null) {
                                    // Đăng nhập bằng email tương ứng
                                    handleEmailLogin(email);
                                    return;
                                }
                            }
                            // Nếu không tìm thấy email (dữ liệu không hợp lệ)
                            Toast.makeText(LoginActivity.this,
                                    "Không tìm thấy tài khoản với số điện thoại này.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            // Không tìm thấy số điện thoại
                            Toast.makeText(LoginActivity.this,
                                    "Số điện thoại chưa được đăng ký. Vui lòng đăng ký trước.",
                                    Toast.LENGTH_LONG).show();
                        }
                        // Ẩn loadingDialog và kích hoạt lại nút Đăng nhập
                        loadingDialog.dismiss();
                        loginButton.setEnabled(true);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(LoginActivity.this,
                                "Lỗi khi tra cứu số điện thoại: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        // Ẩn loadingDialog và kích hoạt lại nút Đăng nhập
                        loadingDialog.dismiss();
                        loginButton.setEnabled(true);
                    }
                });
    }

    // Phương thức để ẩn bàn phím ảo
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            currentFocus.clearFocus();
        }
    }
}