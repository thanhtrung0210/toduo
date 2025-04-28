package com.pipoxniko.toduo.activity;

import static com.google.android.material.textfield.TextInputLayout.END_ICON_NONE;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.pipoxniko.toduo.R;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText editTextEmailOrPhone, editTextNickname, editTextBirthdate, birthdateEditText, editTextPassword;
    private ImageView iconPasswordToggle, birthdatePickerIcon;
    private ImageView icEmailOrPhoneWarning, icNicknameWarning, icPasswordWarning;
    private TextInputLayout emailOrPhoneLayout, nicknameLayout, birthdateLayout, passwordLayout;
    private Button registerButton;
    private TextView conditionLength, conditionLetter, conditionNumberSpecial;
    private TextView loginText;
    private View rootView; // View gốc để bắt sự kiện chạm
    private boolean isPasswordVisible = false;
    private FirebaseAuth auth;
    private DatabaseReference databaseReference;
    private String verificationId; // Lưu verificationId cho OTP SMS
    private PhoneAuthProvider.ForceResendingToken resendToken; // Lưu token để gửi lại OTP
    private String emailOrPhone, password, nickname, birthdate; // Lưu thông tin người dùng
    private Handler handler; // Để kiểm tra định kỳ trạng thái email verification và đếm ngược
    private Runnable checkEmailVerificationRunnable, checkOtpVerificationRunnable;
    private AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.register_activity), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Khởi tạo Firebase Auth và Database
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("TODUO/users");

        // Khởi tạo Handler để kiểm tra email verification và đếm ngược
        handler = new Handler(Looper.getMainLooper());

        // Ánh xạ View gốc
        rootView = findViewById(R.id.register_activity);

        // Bắt sự kiện chạm trên View gốc để ẩn bàn phím
        rootView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                hideKeyboard();
            }
            return false; // Trả về false để không chặn các sự kiện chạm khác
        });

        // Ánh xạ các thành phần giao diện
        editTextEmailOrPhone = findViewById(R.id.register_txt_emailorphone);
        editTextNickname = findViewById(R.id.register_txt_nickname);
        editTextBirthdate = findViewById(R.id.register_txt_birthdate);
        editTextPassword = findViewById(R.id.register_txt_password);
        iconPasswordToggle = findViewById(R.id.register_ic_password_toggle);
        birthdatePickerIcon = findViewById(R.id.register_ic_birthdate_picker);
        icEmailOrPhoneWarning = findViewById(R.id.register_ic_emailorphone_warning);
        icNicknameWarning = findViewById(R.id.register_ic_nickname_warning);
        icPasswordWarning = findViewById(R.id.register_ic_password_warning);
        conditionLength = findViewById(R.id.register_password_condition_length);
        conditionLetter = findViewById(R.id.register_password_condition_letter);
        conditionNumberSpecial = findViewById(R.id.register_password_condition_number_special);
        emailOrPhoneLayout = findViewById(R.id.register_txt_emailorphone_layout);
        nicknameLayout = findViewById(R.id.register_txt_nickname_layout);
        birthdateLayout = findViewById(R.id.register_txt_birthdate_layout);
        birthdateEditText = findViewById(R.id.register_txt_birthdate);
        registerButton = findViewById(R.id.register_btn_register);
        passwordLayout = findViewById(R.id.register_txt_password_layout);
        loginText = findViewById(R.id.register_login_text);

        // Khởi tạo màn hình loading
        View loadingView = getLayoutInflater().inflate(R.layout.custom_loading_dialog, null);
        loadingDialog = new AlertDialog.Builder(this)
                .setView(loadingView)
                .setCancelable(false)
                .create();

        // Xóa background mặc định của dialog
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Hiển thị ngày hôm nay làm giá trị mặc định
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String todayDate = String.format("%02d/%02d/%d", day, month + 1, year);
        birthdateEditText.setText(todayDate);

        // Phương thức hiển thị DatePickerDialog
        View.OnClickListener datePickerListener = v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    RegisterActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        // Kiểm tra ngày sinh không được là ngày trong tương lai
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(selectedYear, selectedMonth, selectedDay);

                        Calendar today = Calendar.getInstance();
                        if (selectedDate.after(today)) {
                            Toast.makeText(RegisterActivity.this,
                                    "Ngày sinh không được là ngày trong tương lai",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String selectedDateStr = String.format("%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear);
                        birthdateEditText.setText(selectedDateStr);
                    },
                    year, month, day
            );
            // Đặt ngày tối đa là ngày hiện tại
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
        };

// Gắn sự kiện click cho ô Ngày sinh và biểu tượng lịch
        birthdateEditText.setOnClickListener(datePickerListener);
        birthdatePickerIcon.setOnClickListener(datePickerListener);

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

        // Hiển thị/ẩn biểu tượng Con mắt và kiểm tra điều kiện mật khẩu
        editTextPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String password = s.toString();

                // Kiểm tra điều kiện mật khẩu
                // Điều kiện 1: Từ 8 ký tự trở lên
                if (password.length() >= 8) {
                    conditionLength.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                } else {
                    conditionLength.setTextColor(getResources().getColor(android.R.color.darker_gray));
                }

                // Điều kiện 2: Có chữ cái viết thường và viết hoa
                if (password.matches(".*[a-z].*") && password.matches(".*[A-Z].*")) {
                    conditionLetter.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                } else {
                    conditionLetter.setTextColor(getResources().getColor(android.R.color.darker_gray));
                }

                // Điều kiện 3: Có số và ký tự đặc biệt
                if (password.matches(".*[0-9].*") && password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
                    conditionNumberSpecial.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                } else {
                    conditionNumberSpecial.setTextColor(getResources().getColor(android.R.color.darker_gray));
                }

                // Hiển thị/ẩn biểu tượng Con mắt và icon warning
                if (password.isEmpty()) {
                    iconPasswordToggle.setVisibility(View.GONE);
                    icPasswordWarning.setVisibility(View.VISIBLE);
                } else {
                    iconPasswordToggle.setVisibility(View.VISIBLE);
                    icPasswordWarning.setVisibility(View.GONE);
                    passwordLayout.setError(null); // Xóa lỗi khi có nội dung
                }
            }
        });

        // Thêm TextWatcher cho ô Email hoặc Số điện thoại
        editTextEmailOrPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String emailOrPhoneInput = s.toString().trim();
                if (emailOrPhoneInput.isEmpty()) {
                    icEmailOrPhoneWarning.setVisibility(View.VISIBLE);
                } else {
                    icEmailOrPhoneWarning.setVisibility(View.GONE);
                    emailOrPhoneLayout.setError(null); // Xóa lỗi khi có nội dung
                }
            }
        });

        // Thêm TextWatcher cho ô Biệt danh
        editTextNickname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String nicknameInput = s.toString().trim();
                if (nicknameInput.isEmpty()) {
                    icNicknameWarning.setVisibility(View.VISIBLE);
                } else {
                    icNicknameWarning.setVisibility(View.GONE);
                    nicknameLayout.setError(null); // Xóa lỗi khi có nội dung
                }
            }
        });

        // Xử lý sự kiện click nút Đăng ký
        registerButton.setOnClickListener(v -> {
            // Lấy dữ liệu từ các ô nhập liệu
            emailOrPhone = editTextEmailOrPhone.getText().toString().trim();
            nickname = editTextNickname.getText().toString().trim();
            birthdate = editTextBirthdate.getText().toString().trim();
            password = editTextPassword.getText().toString().trim();

            // Kiểm tra dữ liệu nhập vào và hiển thị icon warning nếu có lỗi
            boolean hasError = false;

            if (emailOrPhone.isEmpty()) {
                emailOrPhoneLayout.setError("Email hoặc số điện thoại không được để trống");
                emailOrPhoneLayout.setEndIconMode(TextInputLayout.END_ICON_NONE); // Tắt endIcon
                emailOrPhoneLayout.setErrorIconDrawable(null); // Tắt icon lỗi mặc định
                icEmailOrPhoneWarning.setVisibility(View.VISIBLE);
                hasError = true;
            } else {
                icEmailOrPhoneWarning.setVisibility(View.GONE);
                emailOrPhoneLayout.setError(null);
            }

            if (password.isEmpty()) {
                passwordLayout.setError("Mật khẩu không được để trống");
                passwordLayout.setEndIconMode(TextInputLayout.END_ICON_NONE); // Tắt endIcon
                passwordLayout.setErrorIconDrawable(null); // Tắt icon lỗi mặc định
                icPasswordWarning.setVisibility(View.VISIBLE);
                hasError = true;
            } else {
                // Kiểm tra 3 điều kiện mật khẩu
                boolean isLengthValid = password.length() >= 8;
                boolean hasUpperAndLowerCase = password.matches(".*[a-z].*") && password.matches(".*[A-Z].*");
                boolean hasNumberAndSpecialChar = password.matches(".*[0-9].*") && password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");

                if (!isLengthValid || !hasUpperAndLowerCase || !hasNumberAndSpecialChar) {
                    passwordLayout.setError("Mật khẩu chưa đủ điều kiện");
                    passwordLayout.setEndIconMode(TextInputLayout.END_ICON_NONE); // Tắt endIcon
                    passwordLayout.setErrorIconDrawable(null); // Tắt icon lỗi mặc định
                    icPasswordWarning.setVisibility(View.VISIBLE);
                    hasError = true;
                } else {
                    icPasswordWarning.setVisibility(View.GONE);
                    passwordLayout.setError(null);
                }
            }

            if (nickname.isEmpty()) {
                nicknameLayout.setError("Biệt danh không được để trống");
                nicknameLayout.setEndIconMode(TextInputLayout.END_ICON_NONE); // Tắt endIcon
                nicknameLayout.setErrorIconDrawable(null); // Tắt icon lỗi mặc định
                icNicknameWarning.setVisibility(View.VISIBLE);
                hasError = true;
            } else {
                icNicknameWarning.setVisibility(View.GONE);
                nicknameLayout.setError(null);
            }

            if (hasError) {
                return;
            }

            // Hiển thị ProgressDialog và vô hiệu hóa nút Đăng ký
            loadingDialog.show();
            registerButton.setEnabled(false);

            // Kiểm tra định dạng Email hoặc Số điện thoại
            if (Patterns.EMAIL_ADDRESS.matcher(emailOrPhone).matches()) {
                // Đăng ký bằng email
                handleEmailRegistration();
            } else if (isValidPhoneNumber(emailOrPhone)) {
                // Đăng ký bằng số điện thoại
                handlePhoneRegistration();
            } else {
                emailOrPhoneLayout.setError("Vui lòng nhập email hoặc số điện thoại hợp lệ");
                emailOrPhoneLayout.setEndIconMode(TextInputLayout.END_ICON_NONE); // Tắt endIcon
                emailOrPhoneLayout.setErrorIconDrawable(null);
                icEmailOrPhoneWarning.setVisibility(View.VISIBLE);
                // Ẩn ProgressDialog và kích hoạt lại nút Đăng ký nếu có lỗi
                loadingDialog.dismiss();
                registerButton.setEnabled(true);
            }
        });

        // Lấy màu colorPrimary từ theme
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.colorPrimary, typedValue, true);
        int colorPrimary = typedValue.data;

        // Xử lý dòng chữ "Đã có tài khoản? Đăng nhập ngay"
        String fullText = "Đã có tài khoản? Đăng nhập ngay";
        SpannableString spannableString = new SpannableString(fullText);

        // Tô màu và thêm sự kiện click cho phần "Đăng nhập ngay"
        int start = fullText.indexOf("Đăng nhập ngay");
        int end = start + "Đăng nhập ngay".length();
        spannableString.setSpan(new ForegroundColorSpan(colorPrimary), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                // Chuyển về trang đăng nhập
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Áp dụng SpannableString cho TextView
        loginText.setText(spannableString);
        loginText.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
    }

    // Kiểm tra định dạng số điện thoại (chỉ chấp nhận bắt đầu bằng 0, 10-11 chữ số)
    private boolean isValidPhoneNumber(String phone) {
        String phonePattern = "^0\\d{9,10}$"; // Chỉ chấp nhận số bắt đầu bằng 0, tổng cộng 10-11 chữ số
        return Pattern.compile(phonePattern).matcher(phone).matches();
    }

    // Xử lý đăng ký bằng Email
    private void handleEmailRegistration() {
        auth.createUserWithEmailAndPassword(emailOrPhone, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification()
                                    .addOnCompleteListener(verificationTask -> {
                                        if (verificationTask.isSuccessful()) {
                                            showEmailVerificationDialog(user);
                                        } else {
                                            Toast.makeText(RegisterActivity.this,
                                                    "Không thể gửi email xác thực: " + verificationTask.getException().getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                            user.delete(); // Xóa tài khoản nếu không gửi được email xác thực
                                            auth.signOut();
                                            // Ẩn loadingDialog và kích hoạt lại nút Đăng ký
                                            loadingDialog.dismiss();
                                            registerButton.setEnabled(true);
                                        }
                                    });
                        }
                    } else {
                        // Kiểm tra lỗi cụ thể
                        if (task.getException() instanceof FirebaseAuthException) {
                            FirebaseAuthException e = (FirebaseAuthException) task.getException();
                            if (e.getErrorCode().equals("ERROR_EMAIL_ALREADY_IN_USE")) {
                                Toast.makeText(RegisterActivity.this,
                                        "Email đã được sử dụng. Vui lòng dùng email khác hoặc đăng nhập.",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(RegisterActivity.this,
                                        "Đăng ký thất bại: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(RegisterActivity.this,
                                    "Đăng ký thất bại: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                        // Ẩn loadingDialog và kích hoạt lại nút Đăng ký
                        loadingDialog.dismiss();
                        registerButton.setEnabled(true);
                    }
                });
    }

    // Xử lý đăng ký bằng Số điện thoại
    private void handlePhoneRegistration() {
        String phoneNumber = emailOrPhone;
        // Thay 0 thành +84
        if (phoneNumber.startsWith("0")) {
            phoneNumber = "+84" + phoneNumber.substring(1);
        }

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        signInWithPhoneAuthCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(com.google.firebase.FirebaseException e) {
                        Log.e("FirebaseAuth", "Verification failed: " + e.getMessage());
                        // Kiểm tra lỗi cụ thể
                        if (e instanceof FirebaseAuthException) {
                            FirebaseAuthException authException = (FirebaseAuthException) e;
                            if (authException.getErrorCode().equals("ERROR_PHONE_ALREADY_IN_USE") ||
                                    authException.getMessage().contains("phone number is already in use")) {
                                Toast.makeText(RegisterActivity.this,
                                        "Số điện thoại đã được sử dụng. Vui lòng dùng số khác hoặc đăng nhập.",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(RegisterActivity.this,
                                        "Gửi mã OTP thất bại: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(RegisterActivity.this,
                                    "Gửi mã OTP thất bại: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                        // Ẩn loadingDialog và kích hoạt lại nút Đăng ký
                        loadingDialog.dismiss();
                        registerButton.setEnabled(true);
                    }

                    @Override
                    public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                        RegisterActivity.this.verificationId = verificationId;
                        RegisterActivity.this.resendToken = token; // Lưu token để gửi lại OTP
                        showOtpVerificationDialog();
                        // Ẩn loadingDialog và kích hoạt lại nút Đăng ký
                        loadingDialog.dismiss();
                        registerButton.setEnabled(true);
                    }
                });
    }

    // Hiển thị popup xác thực Email
    private void showEmailVerificationDialog(FirebaseUser user) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.login_dialog_email_verification);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button btnConfirmEmail = dialog.findViewById(R.id.dialog_verifi_email_btn_confirm);
        Button btnCancel = dialog.findViewById(R.id.dialog_verifi_email_btn_cancel);
        TextView tvResendEmail = dialog.findViewById(R.id.dialog_verifi_email_resend_txt);

        // Lấy màu primary từ theme
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.colorPrimary, typedValue, true);
        int colorPrimary = typedValue.data;

        // Ban đầu vô hiệu hóa nút "Đã xác nhận" và để màu xám
        btnConfirmEmail.setEnabled(false);
        btnConfirmEmail.setAlpha(0.5f); // Làm mờ nút để biểu thị trạng thái vô hiệu hóa

        // Kiểm tra định kỳ trạng thái email verification
        checkEmailVerificationRunnable = new Runnable() {
            @Override
            public void run() {
                user.reload().addOnCompleteListener(task -> {
                    if (user.isEmailVerified()) {
                        // Email đã được xác thực, kích hoạt nút "Đã xác nhận"
                        btnConfirmEmail.setEnabled(true);
                        btnConfirmEmail.setAlpha(1.0f);
                        btnConfirmEmail.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorPrimary)); // Đổi sang màu primary
                    } else {
                        // Tiếp tục kiểm tra sau 2 giây
                        handler.postDelayed(this, 2000);
                    }
                });
            }
        };
        handler.post(checkEmailVerificationRunnable);

        // Xử lý đếm ngược cho "Gửi lại email"
        final int[] countdownSeconds = {30};
        tvResendEmail.setText("Không nhận được email? Gửi lại sau " + countdownSeconds[0] + "s");
        tvResendEmail.setEnabled(false);

        Runnable countdownRunnable = new Runnable() {
            @Override
            public void run() {
                countdownSeconds[0]--;
                if (countdownSeconds[0] > 0) {
                    tvResendEmail.setText("Không nhận được email? Gửi lại sau " + countdownSeconds[0] + "s");
                    handler.postDelayed(this, 1000);
                } else {
                    // Khi đếm ngược xong, hiển thị "Không nhận được email? Gửi lại"
                    String resendText = "Không nhận được email? Gửi lại";
                    SpannableString spannableResendText = new SpannableString(resendText);
                    int resendStart = resendText.indexOf("Gửi lại");
                    int resendEnd = resendStart + "Gửi lại".length();
                    spannableResendText.setSpan(new ForegroundColorSpan(colorPrimary), resendStart, resendEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    tvResendEmail.setText(spannableResendText);
                    tvResendEmail.setEnabled(true);
                    tvResendEmail.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
                }
            }
        };
        handler.post(countdownRunnable);

        // Xử lý sự kiện click "Gửi lại email"
        tvResendEmail.setOnClickListener(v -> {
            if (tvResendEmail.isEnabled()) {
                user.sendEmailVerification()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(RegisterActivity.this,
                                        "Đã gửi lại email xác thực", Toast.LENGTH_SHORT).show();
                                // Reset đếm ngược
                                countdownSeconds[0] = 30;
                                tvResendEmail.setText("Không nhận được email? Gửi lại sau " + countdownSeconds[0] + "s");
                                tvResendEmail.setEnabled(false);
                                tvResendEmail.setTextColor(getResources().getColor(android.R.color.black));
                                handler.post(countdownRunnable);
                            } else {
                                Toast.makeText(RegisterActivity.this,
                                        "Không thể gửi lại email: " + task.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });

        // Xử lý sự kiện click nút "Đã xác nhận"
        btnConfirmEmail.setOnClickListener(v -> {
            if (user.isEmailVerified()) {
                saveUserToDatabase(user.getUid()); // Chỉ lưu vào database khi email đã được xác thực
                dialog.dismiss();
                handler.removeCallbacks(checkEmailVerificationRunnable); // Dừng kiểm tra định kỳ
                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                finish();
            }
        });

        // Xử lý sự kiện click nút "Hủy"
        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            handler.removeCallbacks(checkEmailVerificationRunnable); // Dừng kiểm tra định kỳ
            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this,
                            "Đã hủy đăng ký", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(RegisterActivity.this,
                            "Không thể hủy tài khoản: " + task.getException().getMessage(),
                            Toast.LENGTH_LONG).show();
                }
                // Ẩn loadingDialog và kích hoạt lại nút Đăng ký
                loadingDialog.dismiss();
                registerButton.setEnabled(true);
            });
            auth.signOut(); // Đăng xuất user
        });

        dialog.setOnDismissListener(d -> {
            handler.removeCallbacks(checkEmailVerificationRunnable); // Dừng kiểm tra khi dialog bị đóng
            // Ẩn loadingDialog và kích hoạt lại nút Đăng ký nếu dialog bị đóng mà chưa xác thực
            if (!user.isEmailVerified()) {
                loadingDialog.dismiss();
                registerButton.setEnabled(true);
            }
        });

        dialog.show();
    }

    // Hiển thị popup xác thực OTP
    private void showOtpVerificationDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.login_dialog_otp_verification);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button btnConfirmOtp = dialog.findViewById(R.id.dialog_verify_otp_btn_confirm);
        Button btnCancel = dialog.findViewById(R.id.dialog_verify_otp_btn_cancel);
        TextView tvResendOtp = dialog.findViewById(R.id.dialog_verify_otp_resend_txt);

        // Lấy màu primary từ theme
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.colorPrimary, typedValue, true);
        int colorPrimary = typedValue.data;

        // Ban đầu vô hiệu hóa nút "Đã xác nhận" và để màu xám
        btnConfirmOtp.setEnabled(false);
        btnConfirmOtp.setAlpha(0.5f); // Làm mờ nút để biểu thị trạng thái vô hiệu hóa

        // Kiểm tra định kỳ trạng thái xác thực OTP
        checkOtpVerificationRunnable = new Runnable() {
            @Override
            public void run() {
                FirebaseUser user = auth.getCurrentUser();
                if (user != null) {
                    // OTP đã được xác thực (user đã đăng nhập thành công)
                    btnConfirmOtp.setEnabled(true);
                    btnConfirmOtp.setAlpha(1.0f);
                    btnConfirmOtp.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorPrimary)); // Đổi sang màu primary
                } else {
                    // Tiếp tục kiểm tra sau 2 giây
                    handler.postDelayed(this, 2000);
                }
            }
        };
        handler.post(checkOtpVerificationRunnable);

        // Xử lý đếm ngược cho "Gửi lại mã OTP"
        final int[] countdownSeconds = {30};
        tvResendOtp.setText("Không nhận được mã OTP? Gửi lại sau " + countdownSeconds[0] + "s");
        tvResendOtp.setEnabled(false);

        Runnable countdownRunnable = new Runnable() {
            @Override
            public void run() {
                countdownSeconds[0]--;
                if (countdownSeconds[0] > 0) {
                    tvResendOtp.setText("Không nhận được mã OTP? Gửi lại sau " + countdownSeconds[0] + "s");
                    handler.postDelayed(this, 1000);
                } else {
                    // Khi đếm ngược xong, hiển thị "Không nhận được mã OTP? Gửi lại"
                    String resendText = "Không nhận được mã OTP? Gửi lại";
                    SpannableString spannableResendText = new SpannableString(resendText);
                    int resendStart = resendText.indexOf("Gửi lại");
                    int resendEnd = resendStart + "Gửi lại".length();
                    spannableResendText.setSpan(new ForegroundColorSpan(colorPrimary), resendStart, resendEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    tvResendOtp.setText(spannableResendText);
                    tvResendOtp.setEnabled(true);
                    tvResendOtp.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
                }
            }
        };
        handler.post(countdownRunnable);

        // Xử lý sự kiện click "Gửi lại mã OTP"
        tvResendOtp.setOnClickListener(v -> {
            if (tvResendOtp.isEnabled()) {
                String phoneNumber = emailOrPhone;
                if (phoneNumber.startsWith("0")) {
                    phoneNumber = "+84" + phoneNumber.substring(1);
                }

                PhoneAuthProvider.getInstance().verifyPhoneNumber(
                        phoneNumber,
                        60,
                        TimeUnit.SECONDS,
                        RegisterActivity.this,
                        new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(PhoneAuthCredential credential) {
                                signInWithPhoneAuthCredential(credential);
                            }

                            @Override
                            public void onVerificationFailed(com.google.firebase.FirebaseException e) {
                                Toast.makeText(RegisterActivity.this,
                                        "Gửi lại mã OTP thất bại: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                                // Ẩn loadingDialog và kích hoạt lại nút Đăng ký
                                loadingDialog.dismiss();
                                registerButton.setEnabled(true);
                            }

                            @Override
                            public void onCodeSent(String newVerificationId, PhoneAuthProvider.ForceResendingToken token) {
                                RegisterActivity.this.verificationId = newVerificationId;
                                RegisterActivity.this.resendToken = token;
                                Toast.makeText(RegisterActivity.this,
                                        "Đã gửi lại mã OTP", Toast.LENGTH_SHORT).show();
                                // Reset đếm ngược
                                countdownSeconds[0] = 30;
                                tvResendOtp.setText("Không nhận được mã OTP? Gửi lại sau " + countdownSeconds[0] + "s");
                                tvResendOtp.setEnabled(false);
                                tvResendOtp.setTextColor(getResources().getColor(android.R.color.black));
                                handler.post(countdownRunnable);
                            }
                        },
                        resendToken);
            }
        });

        // Xử lý sự kiện click nút "Đã xác nhận"
        btnConfirmOtp.setOnClickListener(v -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                saveUserToDatabase(user.getUid()); // Lưu thông tin vào database
                dialog.dismiss();
                handler.removeCallbacks(checkOtpVerificationRunnable); // Dừng kiểm tra định kỳ
                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                finish();
            }
        });

        // Xử lý sự kiện click nút "Hủy"
        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            handler.removeCallbacks(checkOtpVerificationRunnable); // Dừng kiểm tra định kỳ
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                user.delete().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this,
                                "Đã hủy đăng ký", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(RegisterActivity.this,
                                "Không thể hủy tài khoản: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                    // Ẩn loadingDialog và kích hoạt lại nút Đăng ký
                    loadingDialog.dismiss();
                    registerButton.setEnabled(true);
                });
            } else {
                // Nếu không có user (chưa đăng nhập), vẫn cần ẩn loadingDialog và kích hoạt lại nút Đăng ký
                loadingDialog.dismiss();
                registerButton.setEnabled(true);
            }
            auth.signOut(); // Đăng xuất user
        });

        dialog.setOnDismissListener(d -> {
            handler.removeCallbacks(checkOtpVerificationRunnable); // Dừng kiểm tra khi dialog bị đóng
            // Ẩn loadingDialog và kích hoạt lại nút Đăng ký nếu dialog bị đóng mà chưa xác thực
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) { // Nếu user đã bị đăng xuất (chưa xác thực OTP)
                loadingDialog.dismiss();
                registerButton.setEnabled(true);
            }
        });

        dialog.show();
    }

    // Đăng nhập bằng PhoneAuthCredential (OTP)
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            saveUserToDatabase(user.getUid()); // Lưu thông tin vào database
                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                            finish();
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this,
                                "Xác thực OTP thất bại: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                    // Ẩn loadingDialog và kích hoạt lại nút Đăng ký
                    loadingDialog.dismiss();
                    registerButton.setEnabled(true);
                });
    }

    // Lưu thông tin người dùng vào Realtime Database
    private void saveUserToDatabase(String uid) {
        HashMap<String, Object> userData = new HashMap<>();
        userData.put("account", emailOrPhone); // Lưu email hoặc số điện thoại vào trường account
        userData.put("nickname", nickname);
        userData.put("birthdate", birthdate);
        userData.put("coupleId", null); // Ban đầu coupleId là null

        databaseReference.child(uid).setValue(userData)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "Lưu thông tin thất bại: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            if (checkEmailVerificationRunnable != null) {
                handler.removeCallbacks(checkEmailVerificationRunnable);
            }
            if (checkOtpVerificationRunnable != null) {
                handler.removeCallbacks(checkOtpVerificationRunnable);
            }
        }
    }
}