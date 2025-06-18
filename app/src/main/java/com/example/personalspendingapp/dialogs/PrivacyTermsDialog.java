package com.example.personalspendingapp.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.example.personalspendingapp.R;

public class PrivacyTermsDialog extends Dialog {
    private final String title;
    private final String content;
    private TextView tvDialogTitle;
    private TextView tvDialogContent;
    private ImageView btnClose;

    public PrivacyTermsDialog(@NonNull Context context, String title, String content) {
        super(context);
        this.title = title;
        this.content = content;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_privacy_terms);

        // Cho phép thoát dialog khi nhấn bên ngoài
        setCanceledOnTouchOutside(true);
        // Cho phép thoát dialog khi nhấn nút back
        setCancelable(true);

        tvDialogTitle = findViewById(R.id.tvDialogTitle);
        tvDialogContent = findViewById(R.id.tvDialogContent);
        btnClose = findViewById(R.id.btnClose);

        tvDialogTitle.setText(title);
        tvDialogContent.setText(content);

        btnClose.setOnClickListener(v -> dismiss());
    }
} 