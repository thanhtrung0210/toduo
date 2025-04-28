package com.pipoxniko.toduo.mainfragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pipoxniko.toduo.R;
import com.pipoxniko.toduo.activity.LoginActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MainProfileFragment extends Fragment {

    private TextView coupleNameText, daysTogetherText;
    private LinearLayout coupleInfoOption, pairOption;
    private FirebaseAuth auth;
    private DatabaseReference databaseReference;
    private String currentUserId;
    private AlertDialog loadingDialog, coupleInfoDialog, personalInfoDialog,
            pairingOptionsDialog, codeGeneratedDialog, enterCodeDialog;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_profile, container, false);

        // Khởi tạo Firebase
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("TODUO");
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // Nếu không có người dùng đăng nhập, chuyển về LoginActivity
            if (getActivity() != null) {
                startActivity(new Intent(getActivity(), LoginActivity.class));
                getActivity().finish();
            }
            return view;
        }
        currentUserId = currentUser.getUid();

        // Khởi tạo dialog loading
        if (getActivity() != null) {
            View loadingView = inflater.inflate(R.layout.custom_loading_dialog, null);
            loadingDialog = new AlertDialog.Builder(getActivity())
                    .setView(loadingView)
                    .setCancelable(false)
                    .create();

            // Xóa background mặc định của dialog
            loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Ánh xạ view
        coupleNameText = view.findViewById(R.id.profile_couple_name);
        daysTogetherText = view.findViewById(R.id.profile_days_together);
        coupleInfoOption = view.findViewById(R.id.profile_option_couple_info);
        pairOption = view.findViewById(R.id.profile_option_pair);

        // Lấy thông tin người dùng
        loadUserProfile();

        // Xử lý sự kiện click cho các tùy chọn
        coupleInfoOption.setOnClickListener(v -> showCoupleInfoDialog());

        pairOption.setOnClickListener(v -> showPairingOptionsDialog());

        view.findViewById(R.id.profile_option_personal_info).setOnClickListener(v -> showPersonalInfoDialog());

        view.findViewById(R.id.profile_option_settings).setOnClickListener(v -> {
            // TODO: Cài đặt (phát triển sau)
            if (getActivity() != null) {
                Toast.makeText(getActivity(), "Cài đặt", Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.profile_option_logout).setOnClickListener(v -> {
            // Đăng xuất
            auth.signOut();
            if (getActivity() != null) {
                startActivity(new Intent(getActivity(), LoginActivity.class));
                getActivity().finish();
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Đóng tất cả dialog nếu chúng đang hiển thị
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
        if (coupleInfoDialog != null && coupleInfoDialog.isShowing()) {
            coupleInfoDialog.dismiss();
        }
        if (personalInfoDialog != null && personalInfoDialog.isShowing()) {
            personalInfoDialog.dismiss();
        }
        if (pairingOptionsDialog != null && pairingOptionsDialog.isShowing()) {
            pairingOptionsDialog.dismiss();
        }
        if (codeGeneratedDialog != null && codeGeneratedDialog.isShowing()) {
            codeGeneratedDialog.dismiss();
        }
        if (enterCodeDialog != null && enterCodeDialog.isShowing()) {
            enterCodeDialog.dismiss();
        }

        // Đặt các dialog về null để giải phóng tài nguyên
        loadingDialog = null;
        coupleInfoDialog = null;
        personalInfoDialog = null;
        pairingOptionsDialog = null;
        codeGeneratedDialog = null;
        enterCodeDialog = null;
    }

    private void loadUserProfile() {
        databaseReference.child("users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String userNickname = snapshot.child("nickname").getValue(String.class);
                    String coupleId = snapshot.child("coupleId").getValue(String.class);

                    if (coupleId != null) {
                        // Đã ghép nối, lấy thông tin cặp đôi
                        databaseReference.child("couples").child(coupleId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot coupleSnapshot) {
                                if (coupleSnapshot.exists()) {
                                    String user1Id = coupleSnapshot.child("user1_id").getValue(String.class);
                                    String user2Id = coupleSnapshot.child("user2_id").getValue(String.class);
                                    String anniversaryDate = coupleSnapshot.child("anniversary").getValue(String.class);

                                    // Kiểm tra user1Id và user2Id trước khi sử dụng
                                    if (user1Id != null && user2Id != null) {
                                        // Lấy thông tin của cả hai người dùng
                                        fetchUserNickname(user1Id, user2Id, userNickname, anniversaryDate);
                                        // Hiển thị tùy chọn "Xem thông tin cặp đôi", ẩn "Ghép đôi"
                                        coupleInfoOption.setVisibility(View.VISIBLE);
                                        pairOption.setVisibility(View.GONE);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                if (getActivity() != null) {
                                    Toast.makeText(getActivity(), "Lỗi khi lấy thông tin cặp đôi: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    } else {
                        // Chưa ghép nối
                        coupleNameText.setText(userNickname != null ? userNickname : "Người dùng");
                        daysTogetherText.setText("Bạn chưa kết nối với ai");

                        // Ẩn tùy chọn "Xem thông tin cặp đôi", hiển thị "Ghép đôi"
                        coupleInfoOption.setVisibility(View.GONE);
                        pairOption.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(), "Không tìm thấy thông tin người dùng", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi khi lấy thông tin: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void fetchUserNickname(String user1Id, String user2Id, String currentUserNickname, String anniversaryDate) {
        String partnerId = user1Id.equals(currentUserId) ? user2Id : user1Id;
        databaseReference.child("users").child(partnerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot partnerSnapshot) {
                if (partnerSnapshot.exists()) {
                    String partnerNickname = partnerSnapshot.child("nickname").getValue(String.class);
                    if (partnerNickname == null) {
                        partnerNickname = "Không có";
                    }
                    if (user1Id.equals(currentUserId)) {
                        coupleNameText.setText(currentUserNickname + " ♥ " + partnerNickname);
                    } else {
                        coupleNameText.setText(partnerNickname + " ♥ " + currentUserNickname);
                    }

                    // Tính số ngày bên nhau
                    if (anniversaryDate != null) {
                        long days = calculateDaysTogether(anniversaryDate);
                        daysTogetherText.setText("Đã bên nhau được " + days + " ngày");
                    } else {
                        daysTogetherText.setText("Đã bên nhau được 0 ngày");
                    }
                } else {
                    coupleNameText.setText(currentUserNickname + " ♥ " + "Không có");
                    daysTogetherText.setText("Đã bên nhau được 0 ngày");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi khi lấy thông tin đối tác: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private long calculateDaysTogether(String anniversaryDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date startDate = sdf.parse(anniversaryDate);
            Date currentDate = new Date();
            long diffInMillies = Math.abs(currentDate.getTime() - startDate.getTime());
            return TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private String formatDate(String date) {
        try {
            SimpleDateFormat sdfInput;
            // Kiểm tra định dạng của date
            if (date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                sdfInput = new SimpleDateFormat("yyyy-MM-dd");
            } else if (date.matches("\\d{2}/\\d{2}/\\d{4}")) {
                sdfInput = new SimpleDateFormat("dd/MM/yyyy");
            } else {
                return date; // Nếu định dạng không nhận diện được, trả về nguyên gốc
            }
            SimpleDateFormat sdfOutput = new SimpleDateFormat("dd-MM-yyyy");
            Date parsedDate = sdfInput.parse(date);
            return sdfOutput.format(parsedDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return date;
        }
    }

    private void showCoupleInfoDialog() {
        if (getActivity() == null) return;

        // Hiển thị loading
        if (loadingDialog != null) {
            loadingDialog.show();
        }

        databaseReference.child("users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                if (userSnapshot.exists()) {
                    String userNickname = userSnapshot.child("nickname").getValue(String.class);
                    String userBirthDate = userSnapshot.child("birthdate").getValue(String.class);
                    String coupleId = userSnapshot.child("coupleId").getValue(String.class);

                    Log.d("ProfileFragment", "Current user data: nickname=" + userNickname + ", birthdate=" + userBirthDate + ", coupleId=" + coupleId);

                    if (coupleId != null) {
                        databaseReference.child("couples").child(coupleId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot coupleSnapshot) {
                                if (coupleSnapshot.exists()) {
                                    String user1Id = coupleSnapshot.child("user1_id").getValue(String.class);
                                    String user2Id = coupleSnapshot.child("user2_id").getValue(String.class);
                                    String anniversaryDate = coupleSnapshot.child("anniversary").getValue(String.class);

                                    if (user1Id != null && user2Id != null) {
                                        String partnerId = user1Id.equals(currentUserId) ? user2Id : user1Id;
                                        databaseReference.child("users").child(partnerId).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot partnerSnapshot) {
                                                // Ẩn loading
                                                if (loadingDialog != null && loadingDialog.isShowing()) {
                                                    loadingDialog.dismiss();
                                                }

                                                if (partnerSnapshot.exists()) {
                                                    String partnerNickname = partnerSnapshot.child("nickname").getValue(String.class);
                                                    String partnerBirthDate = partnerSnapshot.child("birthdate").getValue(String.class);

                                                    Log.d("ProfileFragment", "Partner data: nickname=" + partnerNickname + ", birthdate=" + partnerBirthDate);

                                                    // Tạo dialog hiển thị thông tin
                                                    View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.profile_dialog_couple_infor, null);
                                                    TextView userInfoText = dialogView.findViewById(R.id.profile_dialog_infor_couple_user_infor);
                                                    TextView partnerInfoText = dialogView.findViewById(R.id.profile_dialog_infor_couple_partner_infor);
                                                    TextView anniversaryText = dialogView.findViewById(R.id.profile_dialog_infor_anniversary);
                                                    Button closeButton = dialogView.findViewById(R.id.profile_dialog_couple_info_btn_close);

                                                    userInfoText.setText("Biệt danh: " + (userNickname != null ? userNickname : "Không có") +
                                                            "\nNgày sinh: " + (userBirthDate != null ? formatDate(userBirthDate) : "Không có"));
                                                    partnerInfoText.setText("Biệt danh: " + (partnerNickname != null ? partnerNickname : "Không có") +
                                                            "\nNgày sinh: " + (partnerBirthDate != null ? formatDate(partnerBirthDate) : "Không có"));
                                                    anniversaryText.setText("Ngày kỷ niệm: " + (anniversaryDate != null ? formatDate(anniversaryDate) : "Không có"));

                                                    coupleInfoDialog = new AlertDialog.Builder(getActivity())
                                                            .setView(dialogView)
                                                            .create();

                                                    // Xóa background mặc định của dialog
                                                    coupleInfoDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                                                    closeButton.setOnClickListener(v -> coupleInfoDialog.dismiss());

                                                    coupleInfoDialog.show();
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                if (loadingDialog != null && loadingDialog.isShowing()) {
                                                    loadingDialog.dismiss();
                                                }
                                                if (getActivity() != null) {
                                                    Toast.makeText(getActivity(), "Lỗi khi lấy thông tin đối tác: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        });
                                    } else {
                                        if (loadingDialog != null && loadingDialog.isShowing()) {
                                            loadingDialog.dismiss();
                                        }
                                    }
                                } else {
                                    if (loadingDialog != null && loadingDialog.isShowing()) {
                                        loadingDialog.dismiss();
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                if (loadingDialog != null && loadingDialog.isShowing()) {
                                    loadingDialog.dismiss();
                                }
                                if (getActivity() != null) {
                                    Toast.makeText(getActivity(), "Lỗi khi lấy thông tin cặp đôi: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    } else {
                        if (loadingDialog != null && loadingDialog.isShowing()) {
                            loadingDialog.dismiss();
                        }
                    }
                } else {
                    if (loadingDialog != null && loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(), "Không tìm thấy thông tin người dùng", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi khi lấy thông tin: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showPersonalInfoDialog() {
        if (getActivity() == null) return;

        // Hiển thị loading
        if (loadingDialog != null) {
            loadingDialog.show();
        }

        databaseReference.child("users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Ẩn loading
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }

                if (snapshot.exists()) {
                    String nickname = snapshot.child("nickname").getValue(String.class);
                    String birthday = snapshot.child("birthdate").getValue(String.class);
                    String account = snapshot.child("account").getValue(String.class);

                    // Log để kiểm tra dữ liệu
                    Log.d("ProfileFragment", "Personal info: nickname=" + nickname + ", birthdate=" + birthday + ", account=" + account);

                    // Tạo dialog hiển thị thông tin cá nhân
                    View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.profile_dialog_personal_infor, null);
                    TextView nicknameText = dialogView.findViewById(R.id.profile_dialog_personal_info_nickname);
                    TextView birthdayText = dialogView.findViewById(R.id.profile_dialog_personal_info_birthday);
                    TextView accountText = dialogView.findViewById(R.id.profile_dialog_personal_info_account);
                    Button closeButton = dialogView.findViewById(R.id.profile_dialog_personal_info_btn_close);

                    nicknameText.setText("Biệt danh: " + (nickname != null ? nickname : "Không có"));
                    birthdayText.setText("Ngày sinh: " + (birthday != null ? formatDate(birthday) : "Không có"));
                    accountText.setText("Tài khoản: " + (account != null ? account : "Không có"));

                    personalInfoDialog = new AlertDialog.Builder(getActivity())
                            .setView(dialogView)
                            .create();

                    // Xóa background mặc định của dialog
                    personalInfoDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                    closeButton.setOnClickListener(v -> personalInfoDialog.dismiss());

                    personalInfoDialog.show();
                } else {
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(), "Không tìm thấy thông tin người dùng", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi khi lấy thông tin: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showPairingOptionsDialog() {
        if (getActivity() == null) return;

        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.profile_dialog_connect_option, null);
        Button generateCodeButton = dialogView.findViewById(R.id.profile_dialog_btn_code_generate);
        Button enterCodeButton = dialogView.findViewById(R.id.profile_dialog_btn_code_enter);

        pairingOptionsDialog = new AlertDialog.Builder(getActivity())
                .setView(dialogView)
                .create();

        // Xóa background mặc định của dialog
        pairingOptionsDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        generateCodeButton.setOnClickListener(v -> {
            pairingOptionsDialog.dismiss();
            checkAndGenerateConnectionCode();
        });

        enterCodeButton.setOnClickListener(v -> {
            pairingOptionsDialog.dismiss();
            showEnterCodeDialog();
        });

        pairingOptionsDialog.show();
    }

    private void checkAndGenerateConnectionCode() {
        // Kiểm tra xem người dùng đã có mã kết nối chưa
        databaseReference.child("connection_codes").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String existingCode = null;
                Long expiredAt = null;

                // Tìm mã kết nối của người dùng hiện tại
                for (DataSnapshot codeSnapshot : snapshot.getChildren()) {
                    String userId = codeSnapshot.child("user_id").getValue(String.class);
                    if (userId != null && userId.equals(currentUserId)) {
                        existingCode = codeSnapshot.getKey();
                        expiredAt = codeSnapshot.child("expiredAt").getValue(Long.class);
                        break;
                    }
                }

                long currentTime = System.currentTimeMillis();
                if (existingCode != null && expiredAt != null && currentTime < expiredAt) {
                    // Nếu mã vẫn còn hạn, hiển thị lại mã cũ
                    showCodeGeneratedDialog(existingCode);
                } else {
                    // Nếu không có mã hoặc mã đã hết hạn, tạo mã mới
                    if (existingCode != null) {
                        // Xóa mã cũ đã hết hạn
                        databaseReference.child("connection_codes").child(existingCode).removeValue();
                    }
                    generateConnectionCode();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi khi kiểm tra mã kết nối: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void generateConnectionCode() {
        // Tạo mã 6 chữ số ngẫu nhiên
        Random random = new Random();
        StringBuilder codeBuilder = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            codeBuilder.append(random.nextInt(10));
        }
        String code = codeBuilder.toString();

        // Tính thời gian hết hạn (10 phút từ bây giờ)
        long currentTime = System.currentTimeMillis();
        long expiredAt = currentTime + 10 * 60 * 1000; // 10 phút

        // Lưu mã vào database
        DatabaseReference connectionCodesRef = databaseReference.child("connection_codes").child(code);
        connectionCodesRef.child("user_id").setValue(currentUserId);
        connectionCodesRef.child("expiredAt").setValue(expiredAt).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Hiển thị dialog với mã
                showCodeGeneratedDialog(code);
            } else {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi khi tạo mã kết nối", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showCodeGeneratedDialog(String code) {
        if (getActivity() == null) return;

        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.profile_dialog_code_generated, null);
        TextView messageText = dialogView.findViewById(R.id.profile_dialog_title_code_generated);
        EditText[] codeDigits = new EditText[6];
        codeDigits[0] = dialogView.findViewById(R.id.dialog_code_digit_1);
        codeDigits[1] = dialogView.findViewById(R.id.dialog_code_digit_2);
        codeDigits[2] = dialogView.findViewById(R.id.dialog_code_digit_3);
        codeDigits[3] = dialogView.findViewById(R.id.dialog_code_digit_4);
        codeDigits[4] = dialogView.findViewById(R.id.dialog_code_digit_5);
        codeDigits[5] = dialogView.findViewById(R.id.dialog_code_digit_6);
        Button closeButton = dialogView.findViewById(R.id.profile_dialog_btn_close);

        messageText.setText("Mã kết nối của bạn đã sẵn sàng! Hãy gửi mã này cho người ấy để cùng ghép đôi nhé 💖");

        // Hiển thị mã trong 6 ô (không cho phép chỉnh sửa)
        for (int i = 0; i < 6; i++) {
            codeDigits[i].setText(String.valueOf(code.charAt(i)));
            codeDigits[i].setEnabled(false);
        }

        codeGeneratedDialog = new AlertDialog.Builder(getActivity())
                .setView(dialogView)
                .create();

        // Xóa background mặc định của dialog
        codeGeneratedDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        closeButton.setOnClickListener(v -> codeGeneratedDialog.dismiss());

        codeGeneratedDialog.show();
    }

    private void showEnterCodeDialog() {
        if (getActivity() == null) return;

        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.profile_dialog_enter_code, null);
        TextView messageText = dialogView.findViewById(R.id.dialog_code_message);
        EditText[] codeDigits = new EditText[6];
        codeDigits[0] = dialogView.findViewById(R.id.dialog_code_digit_1);
        codeDigits[1] = dialogView.findViewById(R.id.dialog_code_digit_2);
        codeDigits[2] = dialogView.findViewById(R.id.dialog_code_digit_3);
        codeDigits[3] = dialogView.findViewById(R.id.dialog_code_digit_4);
        codeDigits[4] = dialogView.findViewById(R.id.dialog_code_digit_5);
        codeDigits[5] = dialogView.findViewById(R.id.dialog_code_digit_6);
        Button submitButton = dialogView.findViewById(R.id.dialog_submit_code_button);
        Button closeButton = dialogView.findViewById(R.id.profile_dialog_btn_close);

        messageText.setText("Hãy nhập mã kết nối");

        // Tự động chuyển focus giữa các ô nhập mã
        for (int i = 0; i < codeDigits.length - 1; i++) {
            final int nextIndex = i + 1;
            codeDigits[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1) {
                        codeDigits[nextIndex].requestFocus();
                    }
                }
            });
        }

        enterCodeDialog = new AlertDialog.Builder(getActivity())
                .setView(dialogView)
                .create();

        // Xóa background mặc định của dialog
        enterCodeDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        submitButton.setOnClickListener(v -> {
            StringBuilder codeBuilder = new StringBuilder();
            for (EditText digit : codeDigits) {
                String digitText = digit.getText().toString().trim();
                if (digitText.isEmpty()) {
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(), "Vui lòng nhập đầy đủ mã kết nối", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                codeBuilder.append(digitText);
            }
            String code = codeBuilder.toString();
            verifyConnectionCode(code);
            enterCodeDialog.dismiss();
        });

        closeButton.setOnClickListener(v -> enterCodeDialog.dismiss());

        enterCodeDialog.show();
    }

    private void verifyConnectionCode(String code) {
        DatabaseReference connectionCodesRef = databaseReference.child("connection_codes").child(code);
        connectionCodesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long expiredAt = snapshot.child("expiredAt").getValue(Long.class);
                    String creatorUserId = snapshot.child("user_id").getValue(String.class);

                    if (expiredAt == null || creatorUserId == null) {
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(), "Mã không hợp lệ", Toast.LENGTH_LONG).show();
                        }
                        return;
                    }

                    long currentTime = System.currentTimeMillis();
                    if (currentTime > expiredAt) {
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(), "Mã đã hết hạn", Toast.LENGTH_LONG).show();
                        }
                        // Xóa mã đã hết hạn
                        connectionCodesRef.removeValue();
                        return;
                    }

                    if (creatorUserId.equals(currentUserId)) {
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(), "Bạn không thể ghép nối với chính mình", Toast.LENGTH_LONG).show();
                        }
                        return;
                    }

                    // Ghép nối thành công
                    pairUsers(creatorUserId);
                    // Xóa mã sau khi sử dụng
                    connectionCodesRef.removeValue();
                } else {
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(), "Mã không tồn tại", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi khi kiểm tra mã: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void pairUsers(String creatorUserId) {
        // Tạo một bản ghi mới trong couples
        String coupleId = databaseReference.child("couples").push().getKey();
        if (coupleId == null) {
            if (getActivity() != null) {
                Toast.makeText(getActivity(), "Lỗi khi tạo cặp đôi", Toast.LENGTH_LONG).show();
            }
            return;
        }

        // Ngày kỷ niệm là ngày hiện tại
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String anniversaryDate = sdf.format(new Date());

        // Lưu thông tin cặp đôi
        DatabaseReference coupleRef = databaseReference.child("couples").child(coupleId);
        coupleRef.child("user1_id").setValue(creatorUserId);
        coupleRef.child("user2_id").setValue(currentUserId);
        coupleRef.child("anniversary").setValue(anniversaryDate);

        // Cập nhật coupleId cho cả hai người dùng
        databaseReference.child("users").child(currentUserId).child("coupleId").setValue(coupleId);
        databaseReference.child("users").child(creatorUserId).child("coupleId").setValue(coupleId).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Ghép đôi thành công!", Toast.LENGTH_LONG).show();
                }
                // Tải lại profile để cập nhật giao diện
                loadUserProfile();
            } else {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Lỗi khi ghép đôi", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}