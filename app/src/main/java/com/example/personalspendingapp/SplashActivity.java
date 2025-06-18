package com.example.personalspendingapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DELAY = 300; // 1 giây
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        progressBar = findViewById(R.id.progressBar);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Kiểm tra trạng thái đăng nhập sau một khoảng thời gian
        new Handler().postDelayed(this::checkAuthState, SPLASH_DELAY);
    }

    private void checkAuthState() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Người dùng đã đăng nhập, kiểm tra dữ liệu trong Firestore
            db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Dữ liệu người dùng tồn tại, chuyển đến MainActivity
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    } else {
                        // Dữ liệu người dùng không tồn tại, chuyển đến LoginActivity
                        Toast.makeText(SplashActivity.this, 
                            "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Có lỗi khi kiểm tra dữ liệu, chuyển đến LoginActivity
                    Toast.makeText(SplashActivity.this, 
                        "Lỗi khi kiểm tra dữ liệu: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                    finish();
                });
        } else {
            // Người dùng chưa đăng nhập, chuyển đến LoginActivity
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish();
        }
    }
} 