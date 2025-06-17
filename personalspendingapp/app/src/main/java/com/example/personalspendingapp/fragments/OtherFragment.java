package com.example.personalspendingapp.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import androidx.core.content.FileProvider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;

import com.example.personalspendingapp.LoginActivity;
import com.example.personalspendingapp.R;
import com.example.personalspendingapp.data.DataManager;
import com.example.personalspendingapp.models.UserData;
import com.example.personalspendingapp.models.UserProfile;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.personalspendingapp.utils.NotificationHelper;
import com.example.personalspendingapp.models.Transaction;
import com.example.personalspendingapp.models.Category;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.example.personalspendingapp.utils.ReportExporter;

import java.io.File;
import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;
import android.net.Uri;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import android.app.Activity;
import java.util.HashMap;
import java.util.Map;

public class OtherFragment extends Fragment {
    private static final String TAG = "OtherFragment";
    private View view;
    private TextView tvProfileName, tvProfileDescription;
    private MaterialCardView cardProfile;
    private MaterialButton btnLogout;
    private MaterialButton btnChangePassword;
    private MaterialButton btnNotificationSettings;
    private MaterialButton btnCategoryManagement;
    private MaterialButton btnEditProfile;
    private MaterialButton btnExportReport;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private DataManager dataManager;
    private File tempReportFile; // Thêm biến tạm để lưu file báo cáo đã tạo

    // Danh sách tiền tệ và ngôn ngữ
    private final String[] currencies = {"VND", "USD", "EUR", "GBP", "JPY", "KRW", "CNY"};
    private final String[] languages = {"Tiếng Việt", "English", "中文", "日本語", "한국어"};

    private NotificationHelper notificationHelper;

    public OtherFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_other, container, false);
        
        initViews(view);
        setupFirebase();
        setupListeners();
        loadUserProfile();
        
        notificationHelper = new NotificationHelper(getContext());

        // Thiết lập listener cho nút đăng xuất
        btnLogout.setOnClickListener(v -> {
            showLogoutConfirmationDialog();
        });

        // Thiết lập listener cho nút đổi mật khẩu
        btnChangePassword.setOnClickListener(v -> {
            showChangePasswordDialog();
        });

        return view;
    }

    private void initViews(View view) {
        Log.d(TAG, "initViews: start");
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileDescription = view.findViewById(R.id.tvProfileDescription);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnNotificationSettings = view.findViewById(R.id.btnNotificationSettings);
        btnCategoryManagement = view.findViewById(R.id.btnCategoryManagement);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnExportReport = view.findViewById(R.id.btnExportReport);
        if (btnEditProfile == null) {
            Log.e(TAG, "initViews: btnEditProfile not found with ID R.id.btnEditProfile");
        } else {
            Log.d(TAG, "initViews: btnEditProfile found");
        }
        progressBar = view.findViewById(R.id.progressBar);
        
        if (tvProfileDescription == null) {
            Log.e(TAG, "initViews: tvProfileDescription not found with ID R.id.tvProfileDescription");
        }

        cardProfile = view.findViewById(R.id.cardProfile);
        if (cardProfile == null) {
            Log.e(TAG, "initViews: cardProfile not found with ID R.id.cardProfile");
        }

        // Lắng nghe sự kiện click cho nút chỉnh sửa hồ sơ
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                Log.d(TAG, "btnEditProfile: Click event detected");
                showEditProfileDialog();
            });
        }
        Log.d(TAG, "initViews: end");
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        dataManager = DataManager.getInstance();
    }

    private void setupListeners() {
        dataManager.setDataLoadedListener(new DataManager.OnDataLoadedListener() {
            @Override
            public void onDataLoaded() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        loadUserProfile();
                    });
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading data for profile update: " + error);
            }
        });

        // Thêm listener cho nút QUẢN LÝ DANH MỤC
        if (btnCategoryManagement != null) {
            btnCategoryManagement.setOnClickListener(v -> {
                Log.d(TAG, "btnCategoryManagement: Click event detected");
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new CategoryManagementFragment())
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        // Thêm listener cho nút CÀI ĐẶT THÔNG BÁO
        if (btnNotificationSettings != null) {
            btnNotificationSettings.setOnClickListener(v -> {
                showNotificationSettingsDialog();
            });
        }

        btnExportReport.setOnClickListener(v -> {
            // Kiểm tra quyền ghi file
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return;
            }

            // Lưu báo cáo vào thiết bị
            saveReportToDevice();
        });
    }

    private void loadUserProfile() {
        UserData userData = dataManager.getUserData();
        String name = "default"; // Default name
        String description = "Chưa có thông tin";

        if (userData != null && userData.getProfile() != null) {
            String userName = userData.getProfile().getName();
            if (userName != null && !userName.trim().isEmpty()) {
                name = userName;
            }
            String userEmail = userData.getProfile().getEmail();
            if (userEmail != null && !userEmail.trim().isEmpty()) {
                description = userEmail;
            }
        }

        tvProfileName.setText(name);
        tvProfileDescription.setText(description);

        Log.d(TAG, "loadUserProfile: Profile data loaded or default set");
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        builder.setView(dialogView);

        // Khởi tạo các view trong dialog
        TextInputEditText etName = dialogView.findViewById(R.id.etName);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etPhone);
        AutoCompleteTextView actvCurrency = dialogView.findViewById(R.id.actvCurrency);
        AutoCompleteTextView actvLanguage = dialogView.findViewById(R.id.actvLanguage);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);

        // Lấy thông tin hiện tại
        UserData userData = dataManager.getUserData();
        if (userData != null && userData.getProfile() != null) {
            UserProfile profile = userData.getProfile();
            etName.setText(profile.getName());
            etPhone.setText(profile.getPhone());
            actvCurrency.setText(profile.getCurrency());
            actvLanguage.setText(profile.getLanguage());
        }

        // Thiết lập adapter cho dropdown
        ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, currencies);
        ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, languages);
        actvCurrency.setAdapter(currencyAdapter);
        actvLanguage.setAdapter(languageAdapter);

        // Tạo dialog
        AlertDialog dialog = builder.create();

        // Xử lý sự kiện nút Hủy
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Xử lý sự kiện nút Lưu
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String currency = actvCurrency.getText().toString().trim();
            String language = actvLanguage.getText().toString().trim();

            // Kiểm tra dữ liệu
            if (name.isEmpty()) {
                etName.setError("Vui lòng nhập tên");
                return;
            }

            // Cập nhật thông tin
            if (userData != null && userData.getProfile() != null) {
                UserProfile profile = userData.getProfile();
                profile.setName(name);
                profile.setPhone(phone);
                profile.setCurrency(currency);
                profile.setLanguage(language);

                // Lưu vào database
                dataManager.saveUserData();
                dialog.dismiss();
                Toast.makeText(getContext(), "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc chắn muốn đăng xuất?")
            .setPositiveButton("Đăng xuất", (dialog, which) -> {
                logout();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void logout() {
        if (mAuth != null) {
            mAuth.signOut();
            // Chuyển về màn hình đăng nhập
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        }
    }

    private void showChangePasswordDialog() {
        try {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
            TextInputEditText etCurrentPassword = dialogView.findViewById(R.id.etCurrentPassword);
            TextInputEditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
            TextInputEditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);
            ProgressBar dialogProgressBar = dialogView.findViewById(R.id.progressBar);

            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Đổi mật khẩu")
                .setView(dialogView)
                .setPositiveButton("Đổi mật khẩu", null)
                .setNegativeButton("Hủy", null)
                .create();

            dialog.setOnShowListener(dialogInterface -> {
                final Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                final ProgressBar dialogProgressBarFinal = dialogProgressBar;
                button.setOnClickListener(view -> {
                    try {
                        // Lấy text từ EditText một cách an toàn
                        String currentPassword = "";
                        String newPassword = "";
                        String confirmPassword = "";

                        if (etCurrentPassword.getText() != null) {
                            currentPassword = etCurrentPassword.getText().toString().trim();
                        }
                        if (etNewPassword.getText() != null) {
                            newPassword = etNewPassword.getText().toString().trim();
                        }
                        if (etConfirmPassword.getText() != null) {
                            confirmPassword = etConfirmPassword.getText().toString().trim();
                        }

                        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                            Toast.makeText(getContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (!newPassword.equals(confirmPassword)) {
                            Toast.makeText(getContext(), "Mật khẩu mới không khớp", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (newPassword.length() < 6) {
                            Toast.makeText(getContext(), "Mật khẩu mới phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        dialogProgressBarFinal.setVisibility(View.VISIBLE);
                        button.setEnabled(false);

                        final String newPasswordFinal = newPassword;
                        // Lấy user hiện tại
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null && user.getEmail() != null) {
                            // Tạo credential với email và mật khẩu hiện tại
                            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

                            // Reauthenticate user
                            user.reauthenticate(credential)
                                .addOnCompleteListener(task -> {
                                    if (getActivity() == null) return;
                                    
                                    if (task.isSuccessful()) {
                                        // Cập nhật mật khẩu mới
                                        user.updatePassword(newPasswordFinal)
                                            .addOnCompleteListener(updateTask -> {
                                                if (getActivity() == null) return;
                                                
                                                getActivity().runOnUiThread(() -> {
                                                    dialogProgressBarFinal.setVisibility(View.GONE);
                                                    button.setEnabled(true);
                                                    
                                                    if (updateTask.isSuccessful()) {
                                                        Toast.makeText(getContext(), "Đã đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
                                                        dialog.dismiss();
                                                    } else {
                                                        String errorMessage = updateTask.getException() != null ? 
                                                            updateTask.getException().getMessage() : "Lỗi không xác định";
                                                        Toast.makeText(getContext(), "Lỗi khi đổi mật khẩu: " + errorMessage, Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            });
                                    } else {
                                        getActivity().runOnUiThread(() -> {
                                            dialogProgressBarFinal.setVisibility(View.GONE);
                                            button.setEnabled(true);
                                            Toast.makeText(getContext(), "Mật khẩu hiện tại không đúng", Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                });
                        } else {
                            dialogProgressBarFinal.setVisibility(View.GONE);
                            button.setEnabled(true);
                            Toast.makeText(getContext(), "Không thể xác thực người dùng", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in password change", e);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                dialogProgressBarFinal.setVisibility(View.GONE);
                                button.setEnabled(true);
                                Toast.makeText(getContext(), "Có lỗi xảy ra: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                });
            });

            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing password change dialog", e);
            Toast.makeText(getContext(), "Có lỗi xảy ra khi mở dialog", Toast.LENGTH_SHORT).show();
        }
    }

    private void authenticateAndChangePassword(String currentPassword, String newPassword) {
        // Implementation of authenticateAndChangePassword method
    }

    private void reauthenticateUser(FirebaseUser user, String currentPassword, Runnable onSuccess) {
        // Implementation of reauthenticateUser method
    }

    private void updatePassword(FirebaseUser user, String newPassword) {
        // Implementation of updatePassword method
    }

    private void displayMessage(String message) {
        // Implementation of displayMessage method
    }

    private void updateProgressBar(boolean show) {
        // Implementation of updateProgressBar method
    }

    private void showNotificationSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_notification_settings, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // Khởi tạo các nút test thông báo trong dialog
        MaterialButton btnTestDailyReminder = dialogView.findViewById(R.id.btnTestDailyReminder);
        MaterialButton btnTestBudgetExceeded = dialogView.findViewById(R.id.btnTestBudgetExceeded);
        MaterialButton btnTestUnusualExpense = dialogView.findViewById(R.id.btnTestUnusualExpense);
        MaterialButton btnTestWeeklySummary = dialogView.findViewById(R.id.btnTestWeeklySummary);
        MaterialButton btnTestNewIncome = dialogView.findViewById(R.id.btnTestNewIncome);

        // Thiết lập listener cho các nút test thông báo
        btnTestDailyReminder.setOnClickListener(v -> {
            notificationHelper.showDailyReminder();
            Toast.makeText(getContext(), "Đã gửi thông báo nhắc nhở hàng ngày", Toast.LENGTH_SHORT).show();
        });

        btnTestBudgetExceeded.setOnClickListener(v -> {
            notificationHelper.showBudgetExceededNotification(8000000, 10000000);
            Toast.makeText(getContext(), "Đã gửi thông báo cảnh báo vượt ngân sách", Toast.LENGTH_SHORT).show();
        });

        btnTestUnusualExpense.setOnClickListener(v -> {
            Transaction testTransaction = new Transaction();
            testTransaction.setAmount(5000000);
            testTransaction.setType("expense");
            testTransaction.setCategoryId("food");
            testTransaction.setNote("Ăn uống nhà hàng sang trọng");
            testTransaction.setDate(new Date());
            notificationHelper.showUnusualExpenseNotification(testTransaction);
            Toast.makeText(getContext(), "Đã gửi thông báo chi tiêu bất thường", Toast.LENGTH_SHORT).show();
        });

        btnTestWeeklySummary.setOnClickListener(v -> {
            notificationHelper.showWeeklySummaryNotification();
            Toast.makeText(getContext(), "Đã gửi thông báo tổng kết tuần", Toast.LENGTH_SHORT).show();
        });

        btnTestNewIncome.setOnClickListener(v -> {
            Transaction incomeTransaction = new Transaction();
            incomeTransaction.setAmount(15000000);
            incomeTransaction.setType("income");
            incomeTransaction.setCategoryId("salary");
            incomeTransaction.setNote("Lương tháng 13");
            incomeTransaction.setDate(new Date());
            notificationHelper.showNewIncomeNotification(incomeTransaction);
            Toast.makeText(getContext(), "Đã gửi thông báo thu nhập mới", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void saveReportToDevice() {
        // Hiển thị dialog đang xử lý
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Đang tạo báo cáo...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Chạy trong thread riêng để không block UI
        new Thread(() -> {
            ReportExporter exporter = new ReportExporter(requireContext());
            tempReportFile = exporter.exportFinancialReport(); // Lưu file tạm vào biến
            requireActivity().runOnUiThread(() -> {
                progressDialog.dismiss();
                if (tempReportFile != null) {
                    // Tạo intent để chọn thư mục lưu
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/pdf");
                    intent.putExtra(Intent.EXTRA_TITLE, "BaoCaoTaiChinh_" + 
                        new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf");
                    
                    try {
                        startActivityForResult(intent, 1);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(requireContext(),
                            "Không tìm thấy ứng dụng quản lý file",
                            Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), 
                        "Có lỗi xảy ra khi xuất báo cáo", 
                        Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            try {
                // Lấy URI của file đã chọn
                Uri uri = data.getData();
                if (uri != null && tempReportFile != null) { // Đảm bảo tempReportFile tồn tại
                    // Hiển thị dialog đang xử lý
                    ProgressDialog progressDialog = new ProgressDialog(requireContext());
                    progressDialog.setMessage("Đang lưu báo cáo...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    // Chạy trong thread riêng để không block UI
                    new Thread(() -> {
                        try {
                            // Sao chép nội dung file đã tạo vào vị trí đã chọn
                            try (InputStream in = new FileInputStream(tempReportFile);
                                 OutputStream out = requireContext().getContentResolver().openOutputStream(uri)) {
                                if (out != null) {
                                    byte[] buffer = new byte[1024];
                                    int read;
                                    while ((read = in.read(buffer)) != -1) {
                                        out.write(buffer, 0, read);
                                    }
                                    requireActivity().runOnUiThread(() -> {
                                        progressDialog.dismiss();
                                        Toast.makeText(requireContext(),
                                            "Đã lưu báo cáo thành công",
                                            Toast.LENGTH_SHORT).show();
                                    });
                                }
                            } finally {
                                // Xóa file tạm sau khi đã sao chép xong
                                tempReportFile.delete();
                                tempReportFile = null; // Đặt lại về null
                            }
                        } catch (Exception e) {
                            requireActivity().runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(requireContext(),
                                    "Có lỗi xảy ra khi lưu báo cáo: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            });
                        }
                    }).start();
                }
            } catch (Exception e) {
                Toast.makeText(requireContext(),
                    "Có lỗi xảy ra: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendReportViaEmail() {
        // Hiển thị dialog đang xử lý
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Đang tạo báo cáo...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Chạy trong thread riêng để không block UI
        new Thread(() -> {
            try {
                ReportExporter exporter = new ReportExporter(requireContext());
                File reportFile = exporter.exportFinancialReport();

                if (reportFile != null) {
                    // Lấy email người dùng hiện tại
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null && currentUser.getEmail() != null) {
                        String userEmail = currentUser.getEmail();
                        
                        // Tạo nội dung email
                        String emailSubject = "Báo cáo tài chính cá nhân - " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
                        String emailBody = "Kính gửi " + currentUser.getDisplayName() + ",\n\n" +
                            "Đính kèm là báo cáo tài chính cá nhân của bạn được tạo vào " + 
                            new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()).format(new Date()) + ".\n\n" +
                            "Báo cáo bao gồm:\n" +
                            "- Tổng quan tài chính\n" +
                            "- Biểu đồ phân bổ thu chi\n" +
                            "- Chi tiết giao dịch theo tháng\n" +
                            "- Thông tin ứng dụng\n\n" +
                            "Nếu bạn có bất kỳ thắc mắc nào, vui lòng liên hệ với chúng tôi qua email support@personalspendingapp.com\n\n" +
                            "Trân trọng,\n" +
                            "Đội ngũ Personal Spending App";

                        // Tạo URI cho file PDF
                        Uri pdfUri = FileProvider.getUriForFile(requireContext(),
                            requireContext().getPackageName() + ".provider",
                            reportFile);

                        // Tạo intent để gửi email
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("application/pdf");
                        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{userEmail});
                        intent.putExtra(Intent.EXTRA_SUBJECT, emailSubject);
                        intent.putExtra(Intent.EXTRA_TEXT, emailBody);
                        intent.putExtra(Intent.EXTRA_STREAM, pdfUri);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        requireActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            try {
                                startActivity(Intent.createChooser(intent, "Gửi báo cáo qua email"));
                            } catch (ActivityNotFoundException e) {
                                Toast.makeText(requireContext(),
                                    "Không tìm thấy ứng dụng email",
                                    Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        requireActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(requireContext(),
                                "Không tìm thấy email người dùng",
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    requireActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(requireContext(),
                            "Có lỗi xảy ra khi tạo báo cáo",
                            Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(requireContext(),
                        "Có lỗi xảy ra: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
} 