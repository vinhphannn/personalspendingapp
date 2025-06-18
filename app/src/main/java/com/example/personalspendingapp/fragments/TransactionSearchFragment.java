package com.example.personalspendingapp.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personalspendingapp.R;
import com.example.personalspendingapp.data.DataManager;
import com.example.personalspendingapp.models.Category;
import com.example.personalspendingapp.models.Transaction;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionSearchFragment extends Fragment {

    private View rootView;
    private EditText etSearch;
    private Button btnSearch;
    private ProgressBar progressBarSearch;
    private RecyclerView rvTransactions;
    private FloatingActionButton fabBack;
    private View viewEdgeBack;
    
    private List<Transaction> allTransactions;
    private List<Transaction> filteredTransactions;
    private TransactionAdapter adapter;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private float downX = 0;
    private boolean isSwiping = false;
    private static final int SWIPE_THRESHOLD = 120;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_transaction_search, container, false);
        
        initViews();
        setupRecyclerView();
        setupSearch();
        setupSwipeBack();

        return rootView;
    }

    private void initViews() {
        etSearch = rootView.findViewById(R.id.etSearch);
        btnSearch = rootView.findViewById(R.id.btnSearch);
        progressBarSearch = rootView.findViewById(R.id.progressBarSearch);
        rvTransactions = rootView.findViewById(R.id.rvTransactions);
        fabBack = rootView.findViewById(R.id.fabBack);
        viewEdgeBack = rootView.findViewById(R.id.viewEdgeBack);
    }

    private void setupRecyclerView() {
        allTransactions = DataManager.getInstance().getUserData().getTransactions();
        filteredTransactions = new ArrayList<>(allTransactions);
        adapter = new TransactionAdapter(filteredTransactions);
        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTransactions.setAdapter(adapter);
    }

    private void setupSearch() {
        btnSearch.setVisibility(View.GONE);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                showLoading(true);
            }

            @Override public void afterTextChanged(Editable s) {
                if (searchRunnable != null) handler.removeCallbacks(searchRunnable);
                searchRunnable = () -> performSearch(s.toString());
                handler.postDelayed(searchRunnable, 300);
            }
        });
    }

    private void performSearch(String query) {
        String keyword = query.trim().toLowerCase();
        filteredTransactions.clear();

        if (TextUtils.isEmpty(keyword)) {
            filteredTransactions.addAll(allTransactions);
        } else {
            for (Transaction t : allTransactions) {
                Category category = DataManager.getInstance().getCategoryById(t.getCategoryId(), t.getType());
                String categoryName = category != null ? category.getName().toLowerCase() : "";
                String note = t.getNote() != null ? t.getNote().toLowerCase() : "";
                if (categoryName.contains(keyword) || note.contains(keyword)) {
                    filteredTransactions.add(t);
                }
            }
        }

        adapter.notifyDataSetChanged();
        showLoading(false);

        if (filteredTransactions.isEmpty()) {
            Toast.makeText(getContext(), "Không tìm thấy giao dịch phù hợp", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading(boolean show) {
        progressBarSearch.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void setupSwipeBack() {
        if (viewEdgeBack != null) {
            viewEdgeBack.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getX();
                        isSwiping = true;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (isSwiping && event.getX() - downX > SWIPE_THRESHOLD) {
                            isSwiping = false;
                            if (isAdded()) requireActivity().onBackPressed();
                            return true;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        isSwiping = false;
                        break;
                }
                return false;
            });
        }

        rvTransactions.setOnTouchListener((v, event) -> false);
        
        // Thêm click listener cho nút X (FloatingActionButton)
        fabBack.setOnClickListener(v -> {
            if (isAdded()) {
                requireActivity().onBackPressed();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        rootView = null;
    }

    // ----------------------------
    // INNER ADAPTER CLASS
    // ----------------------------
    private static class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
        private final List<Transaction> transactions;
        private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());

        TransactionAdapter(List<Transaction> transactions) {
            this.transactions = transactions;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Transaction transaction = transactions.get(position);
            Category category = DataManager.getInstance().getCategoryById(transaction.getCategoryId(), transaction.getType());

            holder.tvCategoryName.setText(category != null ? category.getName() : "Không rõ");
            holder.tvTime.setText(sdf.format(transaction.getDate()));
            holder.tvAmount.setText(String.format(Locale.getDefault(), "%,.0f đ", transaction.getAmount()));

            int color = holder.itemView.getContext().getResources().getColor(
                    transaction.getType().equals("income") ? R.color.green_500 : R.color.red_500
            );
            holder.tvAmount.setTextColor(color);

            if (!TextUtils.isEmpty(transaction.getNote())) {
                holder.tvNote.setText(transaction.getNote());
                holder.tvNote.setVisibility(View.VISIBLE);
            } else {
                holder.tvNote.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return transactions.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCategoryName, tvTime, tvAmount, tvNote;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
                tvTime = itemView.findViewById(R.id.tvTime);
                tvAmount = itemView.findViewById(R.id.tvAmount);
                tvNote = itemView.findViewById(R.id.tvNote);
            }
        }
    }
}
