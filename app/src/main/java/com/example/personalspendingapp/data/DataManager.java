package com.example.personalspendingapp.data;

import android.util.Log;
import com.example.personalspendingapp.models.Category;
import com.example.personalspendingapp.models.Transaction;
import com.example.personalspendingapp.models.UserData;
import com.example.personalspendingapp.models.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DataManager {
    private static final String TAG = "DataManager";
    private static DataManager instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private UserData userData;
    private boolean isDataLoaded = false;
    private boolean isLoading = false;
    private OnDataLoadedListener dataLoadedListener;

    public interface OnDataLoadedListener {
        void onDataLoaded();
        void onError(String error);
    }

    private DataManager() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    public void setDataLoadedListener(OnDataLoadedListener listener) {
        this.dataLoadedListener = listener;
    }

    public void loadUserData() {
        if (isLoading) {
            Log.d(TAG, "Already loading data, skipping...");
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "User not logged in");
            if (dataLoadedListener != null) {
                dataLoadedListener.onError("User not logged in");
            }
            return;
        }

        isLoading = true;
        isDataLoaded = false;
        String userId = currentUser.getUid();
        Log.d(TAG, "Loading user data for userId: " + userId);
        
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    userData = documentSnapshot.toObject(UserData.class);
                    isDataLoaded = true;
                    Log.d(TAG, "User data loaded successfully");
                    if (dataLoadedListener != null) {
                        dataLoadedListener.onDataLoaded();
                    }
                } else {
                    Log.d(TAG, "Creating new user data");
                    createNewUserData(userId, currentUser.getEmail());
                }
                isLoading = false;
            })
            .addOnFailureListener(e -> {
                isLoading = false;
                isDataLoaded = false;
                Log.e(TAG, "Error loading user data", e);
                if (dataLoadedListener != null) {
                    dataLoadedListener.onError(e.getMessage());
                }
            });
    }

    private void createNewUserData(String userId, String email) {
        try {
            UserData userData = new UserData(userId);
            
            // Tạo profile mặc định
            UserProfile profile = new UserProfile();
            profile.setEmail(email);
            profile.setCurrency("VND");
            profile.setLanguage("vi");
            userData.setProfile(profile);

            // Thêm danh mục mặc định
            userData.setCategories(createDefaultCategories());

            // Lưu dữ liệu vào Firestore
            db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    this.userData = userData;
                    isDataLoaded = true;
                    isLoading = false;
                    Log.d(TAG, "New user data created successfully");
                    if (dataLoadedListener != null) {
                        dataLoadedListener.onDataLoaded();
                    }
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    isDataLoaded = false;
                    Log.e(TAG, "Error creating new user data", e);
                    if (dataLoadedListener != null) {
                        dataLoadedListener.onError(e.getMessage());
                    }
                });
        } catch (Exception e) {
            isLoading = false;
            isDataLoaded = false;
            Log.e(TAG, "Error creating new user data", e);
            if (dataLoadedListener != null) {
                dataLoadedListener.onError(e.getMessage());
            }
        }
    }

    public void saveUserData() {
        if (userData == null) {
            Log.e(TAG, "Cannot save: userData is null");
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Cannot save: user is not logged in");
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Saving user data for userId: " + userId);

        db.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "User data saved successfully");
                isDataLoaded = true;
                if (dataLoadedListener != null) {
                    dataLoadedListener.onDataLoaded();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error saving user data", e);
                if (dataLoadedListener != null) {
                    dataLoadedListener.onError(e.getMessage());
                }
            });
    }

    public void addTransaction(Transaction transaction) {
        if (userData != null) {
            userData.addTransaction(transaction);
            saveUserData();
        }
    }

    public void addCategory(Category category) {
        if (userData != null) {
            userData.addCategory(category);
            saveUserData();
        }
    }

    public List<Transaction> getTransactionsByType(String type) {
        if (userData == null) return new ArrayList<>();
        return userData.getTransactionsByType(type);
    }

    public List<Transaction> getTransactionsByDateRange(Date startDate, Date endDate) {
        if (userData == null) return new ArrayList<>();
        List<Transaction> filteredTransactions = new ArrayList<>();
        for (Transaction transaction : userData.getTransactions()) {
            Date transactionDate = transaction.getDate();
            if (!transactionDate.before(startDate) && !transactionDate.after(endDate)) {
                filteredTransactions.add(transaction);
            }
        }
        return filteredTransactions;
    }

    public Map<String, List<Category>> getCategories() {
        return userData != null ? userData.getCategories() : new HashMap<>();
    }

    public boolean isDataLoaded() {
        return isDataLoaded;
    }

    public UserData getUserData() {
        return userData;
    }

    public void clearData() {
        userData = null;
        isDataLoaded = false;
    }

    public boolean isLoading() {
        return isLoading;
    }

    private Map<String, List<Category>> createDefaultCategories() {
        Map<String, List<Category>> categories = new HashMap<>();
        
        // Tạo danh mục thu nhập mặc định
        List<Category> incomeCategories = new ArrayList<>();
        incomeCategories.add(new Category("cat_income_1", "Lương", "💰", "income"));
        incomeCategories.add(new Category("cat_income_2", "Đầu tư", "📈", "income"));
        incomeCategories.add(new Category("cat_income_3", "Thưởng", "🎁", "income"));
        
        // Tạo danh mục chi tiêu mặc định
        List<Category> expenseCategories = new ArrayList<>();
        expenseCategories.add(new Category("cat_expense_1", "Ăn uống", "🍔", "expense"));
        expenseCategories.add(new Category("cat_expense_2", "Di chuyển", "🚗", "expense"));
        expenseCategories.add(new Category("cat_expense_3", "Giải trí", "🎮", "expense"));
        expenseCategories.add(new Category("cat_expense_4", "Khác", "❓", "expense"));
        
        categories.put("income", incomeCategories);
        categories.put("expense", expenseCategories);
        
        return categories;
    }

    // Method to get a category by its ID and type
    public Category getCategoryById(String categoryId, String type) {
        if (userData != null && userData.getCategories() != null) {
            Map<String, List<Category>> categoriesMap = userData.getCategories();
            if (categoriesMap.containsKey(type)) {
                List<Category> categoryList = categoriesMap.get(type);
                if (categoryList != null) {
                    for (Category category : categoryList) {
                        if (category.getId().equals(categoryId)) {
                            return category;
                        }
                    }
                }
            }
        }
        return null; // Return null if category not found
    }
} 