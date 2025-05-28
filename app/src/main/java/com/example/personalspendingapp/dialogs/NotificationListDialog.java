package com.example.personalspendingapp.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.personalspendingapp.R;
import com.example.personalspendingapp.adapters.NotificationAdapter;
import com.example.personalspendingapp.models.Notification;
import com.example.personalspendingapp.utils.NotificationManager;
import java.util.ArrayList;
import java.util.List;

public class NotificationListDialog extends DialogFragment implements NotificationAdapter.OnNotificationClickListener {
    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private NotificationManager notificationManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_notification_list, container, false);
        
        rvNotifications = view.findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new NotificationAdapter(new ArrayList<>(), this);
        rvNotifications.setAdapter(adapter);
        
        notificationManager = new NotificationManager(requireContext());
        loadNotifications();
        
        return view;
    }

    private void loadNotifications() {
        notificationManager.getNotifications(notifications -> {
            adapter.updateNotifications(notifications);
        });
    }

    @Override
    public void onNotificationClick(Notification notification) {
        if (!notification.isRead()) {
            notificationManager.markAsRead(notification.getId());
            notification.setRead(true);
            adapter.notifyDataSetChanged();
        }
    }
} 