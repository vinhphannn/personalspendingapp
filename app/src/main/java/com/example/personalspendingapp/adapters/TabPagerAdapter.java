package com.example.personalspendingapp.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.personalspendingapp.fragments.ExpenseFragment;
import com.example.personalspendingapp.fragments.IncomeFragment;
import com.example.personalspendingapp.fragments.CalendarFragment;
import com.example.personalspendingapp.fragments.ReportFragment;
import com.example.personalspendingapp.fragments.OtherFragment;

public class TabPagerAdapter extends FragmentStateAdapter {

    public TabPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return switch (position) {
            case 0 -> new ExpenseFragment();
            case 1 -> new IncomeFragment();
            case 2 -> new CalendarFragment();
            case 3 -> new ReportFragment();
            case 4 -> new OtherFragment();
            default -> throw new IllegalArgumentException("Invalid position: " + position);
        };
    }

    @Override
    public int getItemCount() {
        return 5; // Năm tab: Tiền Chi, Tiền Thu, Lịch, Báo cáo, Khác
    }
}