package com.example.personalspendingapp.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.personalspendingapp.LoginActivity;
import com.example.personalspendingapp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class OtherFragment extends Fragment {

    private LinearLayout llPersonalInfo;
    private LinearLayout llChangePassword;
    private LinearLayout llCurrency;
    private LinearLayout llLanguage;
    private LinearLayout llExportReport;
    private LinearLayout llNotificationSettings;
    private LinearLayout llLogout;
    private TextView tvCurrency;
    private TextView tvLanguage;
    private MaterialButton btnCategoryManagement;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_other, container, false);

        // Ánh xạ các view
        llPersonalInfo = view.findViewById(R.id.llPersonalInfo);
        llChangePassword = view.findViewById(R.id.llChangePassword);
        llCurrency = view.findViewById(R.id.llCurrency);
        llLanguage = view.findViewById(R.id.llLanguage);
        llExportReport = view.findViewById(R.id.llExportReport);
        llNotificationSettings = view.findViewById(R.id.llNotificationSettings);
        llLogout = view.findViewById(R.id.llLogout);
        tvCurrency = view.findViewById(R.id.tvCurrency);
        tvLanguage = view.findViewById(R.id.tvLanguage);
        btnCategoryManagement = view.findViewById(R.id.btnCategoryManagement);

        // Thiết lập sự kiện click
        llPersonalInfo.setOnClickListener(v -> showEditProfileDialog());
        llChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        llCurrency.setOnClickListener(v -> showCurrencyDialog());
        llLanguage.setOnClickListener(v -> showLanguageDialog());
        llExportReport.setOnClickListener(v -> showExportReportDialog());
        llNotificationSettings.setOnClickListener(v -> showNotificationSettingsDialog());
        llLogout.setOnClickListener(v -> handleLogout());
        btnCategoryManagement.setOnClickListener(v -> navigateToCategoryManagement());

        return view;
    }

    private void showEditProfileDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_edit_profile);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextInputEditText etName = dialog.findViewById(R.id.etName);
        TextInputEditText etEmail = dialog.findViewById(R.id.etEmail);
        TextInputEditText etPhone = dialog.findViewById(R.id.etPhone);

        // TODO: Load user data and set to views

        dialog.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String name = etName.getText().toString();
            String email = etEmail.getText().toString();
            String phone = etPhone.getText().toString();

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            // TODO: Save user data
            Toast.makeText(requireContext(), "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void navigateToCategoryManagement() {
        CategoryManagementFragment categoryManagementFragment = new CategoryManagementFragment();
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, categoryManagementFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void showChangePasswordDialog() {
        Dialog dialog = new Dialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        dialog.setContentView(dialogView);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextInputEditText etCurrentPassword = dialogView.findViewById(R.id.etCurrentPassword);
        TextInputEditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);

        dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String currentPassword = etCurrentPassword.getText().toString();
            String newPassword = etNewPassword.getText().toString();
            String confirmPassword = etConfirmPassword.getText().toString();

            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(requireContext(), "Mật khẩu mới không khớp", Toast.LENGTH_SHORT).show();
                return;
            }

            // TODO: Implement password change logic
            Toast.makeText(requireContext(), "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showCurrencyDialog() {
        Dialog dialog = new Dialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_currency, null);
        dialog.setContentView(dialogView);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        String[] currencies = {"VND", "USD", "EUR", "GBP", "JPY"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, currencies);

        AutoCompleteTextView actvCurrency = dialogView.findViewById(R.id.actvCurrency);
        actvCurrency.setAdapter(adapter);
        actvCurrency.setText(tvCurrency.getText().toString(), false);

        dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String selectedCurrency = actvCurrency.getText().toString();
            if (!selectedCurrency.isEmpty()) {
                tvCurrency.setText(selectedCurrency);
                // TODO: Save currency preference
                Toast.makeText(requireContext(), "Đã cập nhật tiền tệ", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "Vui lòng chọn tiền tệ", Toast.LENGTH_SHORT).show();
            }
        });

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showLanguageDialog() {
        Dialog dialog = new Dialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_language, null);
        dialog.setContentView(dialogView);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        String[] languages = {"Tiếng Việt", "English", "中文", "日本語", "한국어"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, languages);

        AutoCompleteTextView actvLanguage = dialogView.findViewById(R.id.actvLanguage);
        actvLanguage.setAdapter(adapter);
        actvLanguage.setText(tvLanguage.getText().toString(), false);

        dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String selectedLanguage = actvLanguage.getText().toString();
            if (!selectedLanguage.isEmpty()) {
                tvLanguage.setText(selectedLanguage);
                // TODO: Save language preference
                Toast.makeText(requireContext(), "Đã cập nhật ngôn ngữ", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "Vui lòng chọn ngôn ngữ", Toast.LENGTH_SHORT).show();
            }
        });

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showExportReportDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_export_report);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        dialog.findViewById(R.id.btnExport).setOnClickListener(v -> {
            // TODO: Implement export report logic
            Toast.makeText(requireContext(), "Đang xuất báo cáo...", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showNotificationSettingsDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_notification_settings);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        dialog.findViewById(R.id.btnSave).setOnClickListener(v -> {
            // TODO: Save notification settings
            Toast.makeText(requireContext(), "Đã lưu cài đặt thông báo", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void handleLogout() {
        // TODO: Clear user session and preferences
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
} 