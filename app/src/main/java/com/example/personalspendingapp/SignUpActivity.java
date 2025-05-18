package com.example.personalspendingapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.personalspendingapp.models.Category;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";
    private FirebaseAuth mAuth;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Khởi tạo Firebase Auth và Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Ánh xạ các view
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        Button signUpButton = findViewById(R.id.signUpButton);

        // Xử lý sự kiện nhấn nút Sign Up
        signUpButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "Please enter email and password",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            createAccount(email, password);
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

    private void createAccount(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Đăng ký thành công
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                createDefaultCategories(user.getUid());
                                updateUI(user);
                            } else {
                                // Xử lý trường hợp user là null sau khi đăng ký thành công (hiếm khi xảy ra)
                                Log.e(TAG, "User is null after successful registration");
                                Toast.makeText(SignUpActivity.this, "Registration successful, but user data is missing.",
                                        Toast.LENGTH_SHORT).show();
                                updateUI(null);
                            }
                        } else {
                            // Đăng ký thất bại
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(SignUpActivity.this, "Sign up failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    }
                });
    }

    private void createDefaultCategories(String userId) {
        // Default Expense Categories
        String[] defaultExpenseCategories = {
                "Ăn uống", "Chi tiêu hàng ngày", "Quần áo", "Mỹ phẩm",
                "Giao lưu", "Y tế", "Đi lại", "Giáo dục", "Tiện ích", "Điện", "Phí liên lạc"
        };
        for (String categoryName : defaultExpenseCategories) {
            String categoryId = db.collection("categories").document().getId();
            Category category = new Category(
                    categoryId,
                    userId,
                    categoryName,
                    "expense",
                    true
            );
            db.collection("categories").document(categoryId).set(category)
                    .addOnSuccessListener(aVoid ->
                            Log.d(TAG, "Expense Category created: " + categoryName))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Error creating expense category", e));
        }

        // Default Income Categories
        String[] defaultIncomeCategories = {
                "Tiền lương", "Tiền phụ cấp", "Tiền thưởng", "Thu nhập phụ",
                "Đầu tư", "Thu nhập tạm thời"
        };
        for (String categoryName : defaultIncomeCategories) {
            String categoryId = db.collection("categories").document().getId();
            Category category = new Category(
                    categoryId,
                    userId,
                    categoryName,
                    "income",
                    true
            );
            db.collection("categories").document(categoryId).set(category)
                    .addOnSuccessListener(aVoid ->
                            Log.d(TAG, "Income Category created: " + categoryName))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Error creating income category", e));
        }
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Toast.makeText(this, "Sign up successful: " + user.getEmail(), Toast.LENGTH_SHORT).show();
            // Sau khi đăng ký thành công và tạo danh mục, chuyển hướng đến LoginActivity để đăng nhập
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else {
            // Không làm gì thêm nếu user là null
        }
    }
}