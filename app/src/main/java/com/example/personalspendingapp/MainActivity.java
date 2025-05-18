package com.example.personalspendingapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.personalspendingapp.fragments.ExpenseFragment;
import com.example.personalspendingapp.fragments.IncomeFragment;
import com.example.personalspendingapp.fragments.OtherFragment;
import com.example.personalspendingapp.fragments.CalendarFragment;
import com.example.personalspendingapp.fragments.ReportFragment;

import com.example.personalspendingapp.adapters.TabPagerAdapter;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Date;

public class MainActivity extends AppCompatActivity implements CalendarFragment.OnDaySelectedListener {
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;
    private LinearLayout inputTabsContainer;
    private TextView tvTabExpense;
    private TextView tvTabIncome;
    private FragmentManager fragmentManager;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

    private ExpenseFragment expenseFragment;
    private IncomeFragment incomeFragment;
    private CalendarFragment calendarFragment;
    private ReportFragment reportFragment;
    private OtherFragment otherFragment;

    private Date selectedDateForInputTabs;

    // Interface for callback to the hosting Activity
    public interface OnDaySelectedListener {
        void onDaySelected(Date selectedDate);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        viewPager = findViewById(R.id.viewPager);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        inputTabsContainer = findViewById(R.id.inputTabsContainer);
        tvTabExpense = findViewById(R.id.tvTabExpense);
        tvTabIncome = findViewById(R.id.tvTabIncome);
        fragmentManager = getSupportFragmentManager();

        // Initialize Fragments - Only keep references for input fragments if needed to update date
        // The actual fragment instances are created by the TabPagerAdapter
         expenseFragment = new ExpenseFragment(); // Initialize to keep reference
         incomeFragment = new IncomeFragment(); // Initialize to keep reference
        // Removed other fragment initializations
        // calendarFragment = new CalendarFragment();
        // reportFragment = new ReportFragment();
        // otherFragment = new OtherFragment();

        // Setup ViewPager2
        setupViewPager();

        // Setup BottomNavigationView
        setupBottomNavigationView();

        // Set default selected item
        bottomNavigationView.setSelectedItemId(R.id.navigation_input);

        // Setup Auth State Listener
        setupAuthStateListener();

        // Set initial state of custom tabs
        updateCustomTabsState(viewPager.getCurrentItem());
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authStateListener != null) {
            mAuth.removeAuthStateListener(authStateListener);
        }
    }

    private void setupAuthStateListener() {
        authStateListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user == null) {
                // User is signed out
                Toast.makeText(MainActivity.this, "User signed out", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                // User is signed in
                // Optional: Update UI based on signed-in user
                // Set default selected item if this is the first time loading after login
                 if (inputTabsContainer.getVisibility() != View.VISIBLE || fragmentManager.getFragments().isEmpty()) {
                     bottomNavigationView.setSelectedItemId(R.id.navigation_input);
                 }
            }
        };
    }

    private void setupViewPager() {
        // Use the TabPagerAdapter from the adapters package
        TabPagerAdapter tabPagerAdapter = new TabPagerAdapter(this); // Create adapter
        viewPager.setAdapter(tabPagerAdapter); // Set adapter

        // Add a listener to ViewPager2 to update BottomNavigationView and TabLayout visibility
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                 // Update BottomNavigationView selection
                 // Important: This must be done first to ensure correct tab is selected before checking its type
                 int bottomNavItemId = -1;
                 if (position == 0 || position == 1) bottomNavItemId = R.id.navigation_input;
                 else if (position == 2) bottomNavItemId = R.id.navigation_calendar;
                 else if (position == 3) bottomNavItemId = R.id.navigation_report;
                 else if (position == 4) bottomNavItemId = R.id.navigation_other;

                 if (bottomNavItemId != -1) {
                     // Remove listener temporarily to prevent infinite loop
                     bottomNavigationView.setOnNavigationItemSelectedListener(null);
                     bottomNavigationView.setSelectedItemId(bottomNavItemId);
                     setupBottomNavigationView(); // Re-attach listener
                 }

                 // Handle showing/hiding TabLayout based on current tab
                 if (position >= 0 && position < 2) { // Input tabs (Expense and Income)
                     showInputTabs(); // Ensure TabLayout is visible
                 } else { // Other tabs (Calendar, Report, Other)
                     hideInputTabs(); // Ensure TabLayout is hidden
                 }

                 // When switching to Input tabs (Expense or Income) after selecting date from Calendar,
                 // ensure the selected date is set on the fragment
                 if (selectedDateForInputTabs != null) { 
                     // We need to get the currently active fragment from the ViewPager
                     // Use findFragmentByTag with ViewPager2 fragment tag format: "f" + adapter.getItemId(position)
                     String fragmentTag = "f" + tabPagerAdapter.getItemId(position);
                     Fragment activeFragment = getSupportFragmentManager().findFragmentByTag(fragmentTag);

                     if (position == 0 && activeFragment instanceof ExpenseFragment) { // Expense tab
                         ((ExpenseFragment) activeFragment).setSelectedDate(selectedDateForInputTabs);
                     } else if (position == 1 && activeFragment instanceof IncomeFragment) { // Income tab
                         ((IncomeFragment) activeFragment).setSelectedDate(selectedDateForInputTabs);
                     }
                 }
            }
        });

         // Add click listeners for custom tabs
        tvTabExpense.setOnClickListener(v -> viewPager.setCurrentItem(0, false)); // Switch to Expense fragment
        tvTabIncome.setOnClickListener(v -> viewPager.setCurrentItem(1, false)); // Switch to Income fragment
    }

    private void setupBottomNavigationView() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            int targetViewPagerPosition = -1;

            if (itemId == R.id.navigation_input) {
                // When selecting input, default to the currently selected tab in TabLayout if available,
                // otherwise default to Expense (position 0). This ensures we return to the last viewed input tab.
                 targetViewPagerPosition = viewPager.getCurrentItem();
                 if (targetViewPagerPosition < 0 || targetViewPagerPosition > 1) {
                      targetViewPagerPosition = 0; // Default to Expense
                 }
                showInputTabs(); // Ensure TabLayout is visible
                updateCustomTabsState(targetViewPagerPosition); // Update the visual state of custom tabs
            } else if (itemId == R.id.navigation_calendar) {
                targetViewPagerPosition = 2; // Calendar is at position 2
                hideInputTabs(); // Ensure TabLayout is hidden
            } else if (itemId == R.id.navigation_report) {
                targetViewPagerPosition = 3; // Report is at position 3
                hideInputTabs(); // Ensure TabLayout is hidden
            } else if (itemId == R.id.navigation_other) {
                targetViewPagerPosition = 4; // Other is at position 4
                hideInputTabs(); // Ensure TabLayout is hidden
            }

            if (targetViewPagerPosition != -1) {
                // Sync ViewPager2 with BottomNavigationView selection
                // Use smoothScroll=false for instant switching
                viewPager.setCurrentItem(targetViewPagerPosition, false);

                 // If switching to input tabs and a date was selected from Calendar, update the date on the fragment
                 if (targetViewPagerPosition >= 0 && targetViewPagerPosition < 2 && selectedDateForInputTabs != null) {
                     // We need to get the currently active fragment from the ViewPager
                     // Use findFragmentByTag with ViewPager2 fragment tag format: "f" + adapter.getItemId(position)
                     String fragmentTag = "f" + viewPager.getAdapter().getItemId(targetViewPagerPosition);
                     Fragment activeFragment = getSupportFragmentManager().findFragmentByTag(fragmentTag);

                     if (targetViewPagerPosition == 0 && activeFragment instanceof ExpenseFragment) { // Expense tab
                         ((ExpenseFragment) activeFragment).setSelectedDate(selectedDateForInputTabs);
                     } else if (targetViewPagerPosition == 1 && activeFragment instanceof IncomeFragment) { // Income tab
                         ((IncomeFragment) activeFragment).setSelectedDate(selectedDateForInputTabs);
                     }
                 }

                return true; // Indicate that the selection has been handled
            }

            return false; // Indicate that the selection was not handled
        });
    }

    private void showInputTabs() {
        inputTabsContainer.setVisibility(View.VISIBLE);
    }

    private void hideInputTabs() {
        inputTabsContainer.setVisibility(View.GONE);
    }

    // Helper method to update the visual state of custom tabs
    private void updateCustomTabsState(int position) {
        if (position == 0) { // Expense tab is selected
            tvTabExpense.setTextColor(getResources().getColor(R.color.purple_500)); // Highlight Expense
            tvTabExpense.setTypeface(null, android.graphics.Typeface.BOLD); // Make text bold
            tvTabIncome.setTextColor(getResources().getColor(android.R.color.tab_indicator_text)); // Dim Income
            tvTabIncome.setTypeface(null, android.graphics.Typeface.NORMAL); // Make text normal
            // You might want to add a visual indicator like an underline here as well
        } else if (position == 1) { // Income tab is selected
            tvTabExpense.setTextColor(getResources().getColor(android.R.color.tab_indicator_text)); // Dim Expense
            tvTabExpense.setTypeface(null, android.graphics.Typeface.NORMAL); // Make text normal
            tvTabIncome.setTextColor(getResources().getColor(R.color.purple_500)); // Highlight Income
            tvTabIncome.setTypeface(null, android.graphics.Typeface.BOLD); // Make text bold
            // You might want to add a visual indicator like an underline here as well
        }
         // For other positions, the inputTabsContainer is hidden, so no need to update state
    }

    // Implement OnDaySelectedListener interface
    @Override
    public void onDaySelected(Date selectedDate) {
        this.selectedDateForInputTabs = selectedDate; // Store selected date
        // Set the selected item in BottomNavigationView, which will trigger tab switch via listener
        bottomNavigationView.setSelectedItemId(R.id.navigation_input);
        // The date will be set on the appropriate fragment by the BottomNavigationView listener after the tab switch
    }
}