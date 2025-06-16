package com.example.personalspendingapp.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.personalspendingapp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class CategoryManagementFragment extends Fragment {

    private static final String TAG = "CategoryManagementFragment";
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private MaterialButton btnBack;

    public CategoryManagementFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        View view = inflater.inflate(R.layout.fragment_category_management, container, false);

        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);
        btnBack = view.findViewById(R.id.btnBack);

        CategoryPagerAdapter adapter = new CategoryPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Khoản Thu");
            } else {
                tab.setText("Khoản Chi");
            }
        }).attach();

        // Xử lý sự kiện nút thoát
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        return view;
    }

    private static class CategoryPagerAdapter extends FragmentStateAdapter {
        public CategoryPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new IncomeCategoryFragment();
            } else {
                return new ExpenseCategoryFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2; // Two tabs: Income and Expense
        }
    }
} 