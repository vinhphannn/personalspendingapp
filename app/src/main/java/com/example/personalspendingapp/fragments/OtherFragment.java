package com.example.personalspendingapp.fragments;

import android.app.AlertDialog;
import android.content.Intent;
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
import com.example.personalspendingapp.utils.NotificationHelper;
import com.example.personalspendingapp.models.Transaction;
import com.example.personalspendingapp.models.Category;

import java.util.Date;

public class OtherFragment extends Fragment {
    private static final String TAG = "OtherFragment";
    private View view;
    private ImageView ivProfileAvatar;
    private TextView tvProfileName, tvProfileDescription;
    private MaterialCardView cardProfile;

    private FirebaseAuth mAuth;
    private DataManager dataManager;

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

        // Ánh xạ các nút test thông báo
        Button btnTestDailyReminder = view.findViewById(R.id.btnTestDailyReminder);
        Button btnTestBudgetExceeded = view.findViewById(R.id.btnTestBudgetExceeded);
        Button btnTestUnusualExpense = view.findViewById(R.id.btnTestUnusualExpense);
        Button btnTestWeeklySummary = view.findViewById(R.id.btnTestWeeklySummary);
        Button btnTestNewIncome = view.findViewById(R.id.btnTestNewIncome);

        // Thiết lập listener cho các nút test thông báo
        btnTestDailyReminder.setOnClickListener(v -> {
            notificationHelper.showDailyReminder();
            Toast.makeText(getContext(), "Đã gửi thông báo nhắc nhở hàng ngày", Toast.LENGTH_SHORT).show();
        });

        btnTestBudgetExceeded.setOnClickListener(v -> {
            // Sử dụng giá trị mẫu cho test
            notificationHelper.showBudgetExceededNotification(8000000, 10000000);
            Toast.makeText(getContext(), "Đã gửi thông báo cảnh báo vượt ngân sách", Toast.LENGTH_SHORT).show();
        });

        btnTestUnusualExpense.setOnClickListener(v -> {
            // Sử dụng dữ liệu mẫu cho test
            Transaction testTransaction = new Transaction();
            testTransaction.setAmount(5000000);
            testTransaction.setType("expense");
            testTransaction.setCategoryId("food"); // Cần đảm bảo categoryId này tồn tại hoặc xử lý null trong NotificationHelper
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
            // Sử dụng dữ liệu mẫu cho test
            Transaction incomeTransaction = new Transaction();
            incomeTransaction.setAmount(15000000);
            incomeTransaction.setType("income");
            incomeTransaction.setCategoryId("salary"); // Cần đảm bảo categoryId này tồn tại hoặc xử lý null trong NotificationHelper
            incomeTransaction.setNote("Lương tháng 13");
            incomeTransaction.setDate(new Date());
            notificationHelper.showNewIncomeNotification(incomeTransaction);
             Toast.makeText(getContext(), "Đã gửi thông báo thu nhập mới", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void initViews(View view) {
        Log.d(TAG, "initViews: start");
        ivProfileAvatar = view.findViewById(R.id.ivProfileAvatar);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileDescription = view.findViewById(R.id.tvProfileDescription);
        if (tvProfileDescription == null) {
            Log.e(TAG, "initViews: tvProfileDescription not found with ID R.id.tvProfileDescription");
        }

        cardProfile = view.findViewById(R.id.cardProfile);
        if (cardProfile == null) {
            Log.e(TAG, "initViews: cardProfile not found with ID R.id.cardProfile");
        }

        if (cardProfile != null) {
            cardProfile.setOnClickListener(v -> {
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

        // Generate and set the initial drawable
        Drawable initialDrawable = generateInitialDrawable(name);
        ivProfileAvatar.setImageDrawable(initialDrawable);

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

    // Helper method to generate a circular drawable with an initial
    private Drawable generateInitialDrawable(String name) {
        String initial = (name == null || name.trim().isEmpty() || name.equals("default")) ? "D" : name.substring(0, 1).toUpperCase();

        int size = (int) getResources().getDimension(R.dimen.avatar_size);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Draw background circle
        Paint backgroundPaint = new Paint();
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, backgroundPaint);

        // Draw text (initial)
        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(size * 0.4f);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Calculate text position
        float x = size / 2f;
        float y = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f);

        canvas.drawText(initial, x, y, textPaint);

        return new BitmapDrawable(getResources(), bitmap);
    }
} 