package com.example.personalspendingapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.personalspendingapp.data.DataManager;
import com.example.personalspendingapp.models.Category;
import com.example.personalspendingapp.models.UserData;
import com.example.personalspendingapp.models.UserProfile;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";
    private TextInputEditText etEmail, etPassword, etConfirmPassword;
    private Button btnSignUp;
    private TextView tvLogin;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);

        // Set click listeners
        btnSignUp.setOnClickListener(v -> signUp());
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Kiểm tra nếu người dùng đã đăng nhập
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Nếu đã đăng nhập, chuyển về MainActivity thay vì LoginActivity để tránh vòng lặp
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void signUp() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email không được để trống");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Mật khẩu không được để trống");
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            return;
        }

        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);
        btnSignUp.setEnabled(false);

        // Create user with email and password
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                    // Sign up success
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                        createUserData(user.getUid(), email);
                            } else {
                        handleSignUpError("Không thể tạo tài khoản");
                            }
                        } else {
                    // Sign up failed
                    handleSignUpError(task.getException() != null ? 
                        task.getException().getMessage() : "Đăng ký thất bại");
                    }
                });
    }

    private void handleSignUpError(String errorMessage) {
        progressBar.setVisibility(View.GONE);
        btnSignUp.setEnabled(true);
        Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
        Log.e(TAG, "Sign up error: " + errorMessage);
    }

    private void createUserData(String userId, String email) {
        try {
            UserData userData = new UserData(userId);
            
            // Tạo profile mặc định
            UserProfile profile = new UserProfile();
            profile.setEmail(email);
            profile.setCurrency("VND");
            profile.setLanguage("vi");
            userData.setProfile(profile);

            // Thêm danh mục mặc định
            userData.setCategories(createDefaultCategories());

            // Lưu dữ liệu vào Firestore
            db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    btnSignUp.setEnabled(true);
                    Toast.makeText(SignUpActivity.this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    handleSignUpError("Lỗi khi tạo dữ liệu người dùng: " + e.getMessage());
                });
        } catch (Exception e) {
            handleSignUpError("Lỗi không xác định: " + e.getMessage());
        }
    }

    private Map<String, List<Category>> createDefaultCategories() {
        Map<String, List<Category>> categories = new HashMap<>();
        
        // Danh mục thu nhập
        List<Category> incomeCategories = new ArrayList<>();
        incomeCategories.add(new Category("cat_income_1", "Lương", "💰", "income"));
        incomeCategories.add(new Category("cat_income_2", "Đầu tư", "📈", "income"));
        incomeCategories.add(new Category("cat_income_3", "Thưởng", "🎁", "income"));
        categories.put("income", incomeCategories);

        // Danh mục chi tiêu
        List<Category> expenseCategories = new ArrayList<>();
        expenseCategories.add(new Category("cat_expense_1", "Ăn uống", "🍔", "expense"));
        expenseCategories.add(new Category("cat_expense_2", "Di chuyển", "🚗", "expense"));
        expenseCategories.add(new Category("cat_expense_3", "Giải trí", "🎮", "expense"));
        expenseCategories.add(new Category("cat_expense_4", "Khác", "❓", "expense"));
        categories.put("expense", expenseCategories);

        return categories;
    }
}