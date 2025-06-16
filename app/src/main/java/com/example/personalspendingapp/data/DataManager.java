package com.example.personalspendingapp.data;

import android.util.Log;
import com.example.personalspendingapp.models.Category;
import com.example.personalspendingapp.models.Transaction;
import com.example.personalspendingapp.models.UserData;
import com.example.personalspendingapp.models.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
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
    private List<Transaction> transactions;
    private List<Category> categories;
    private OnDataChangeListener dataChangeListener;

    public interface OnDataLoadedListener {
        void onDataLoaded();
        void onError(String error);
    }

    public interface OnDataChangeListener {
        void onDataChanged();
    }

    private DataManager() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        transactions = new ArrayList<>();
        categories = new ArrayList<>();
        initializeDefaultCategories();
    }

    private void initializeDefaultCategories() {
        Log.d(TAG, "Initializing default categories");
        // Danh mục chi tiêu mặc định
        categories.add(new Category("1", "Ăn uống", "expense", "#FF5722"));
        categories.add(new Category("2", "Di chuyển", "expense", "#2196F3"));
        categories.add(new Category("3", "Mua sắm", "expense", "#9C27B0"));
        categories.add(new Category("4", "Giải trí", "expense", "#4CAF50"));
        categories.add(new Category("5", "Hóa đơn", "expense", "#F44336"));

        // Danh mục thu nhập mặc định
        categories.add(new Category("7", "Lương", "income", "#4CAF50"));
        categories.add(new Category("8", "Thưởng", "income", "#FFC107"));
        categories.add(new Category("9", "Đầu tư", "income", "#2196F3"));
        categories.add(new Category("10", "Khác", "income", "#757575"));
        Log.d(TAG, "Default categories initialized: " + categories.size());
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

    public void setDataChangeListener(OnDataChangeListener listener) {
        this.dataChangeListener = listener;
    }

    private void notifyDataChanged() {
        if (dataChangeListener != null) {
            dataChangeListener.onDataChanged();
        }
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
                    if (userData != null) {
                        // Cập nhật danh sách categories từ userData
                        Map<String, List<Category>> userCategories = userData.getCategories();
                        if (userCategories != null) {
                            categories.clear();
                            // Thêm danh mục mặc định
                            initializeDefaultCategories();
                            // Thêm danh mục của user
                            for (List<Category> typeCategories : userCategories.values()) {
                                categories.addAll(typeCategories);
                            }
                        }
                        // Cập nhật danh sách transactions
                        transactions.clear();
                        transactions.addAll(userData.getTransactions());
                    }
                    isDataLoaded = true;
                    Log.d(TAG, "User data loaded successfully. Categories: " + categories.size() + ", Transactions: " + transactions.size());
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

        // Cập nhật lại danh sách giao dịch trong DataManager
        if (userData.getTransactions() != null) {
            this.transactions = new ArrayList<>(userData.getTransactions());
        }

        db.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "User data saved successfully");
                isDataLoaded = true;
                if (dataLoadedListener != null) {
                    dataLoadedListener.onDataLoaded();
                }
                // Thông báo thay đổi dữ liệu sau khi lưu thành công
                notifyDataChanged();
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
            notifyDataChanged();
        }
    }

    public void updateTransaction(Transaction transaction) {
        if (userData != null && userData.getTransactions() != null) {
            List<Transaction> transactions = userData.getTransactions();
            for (int i = 0; i < transactions.size(); i++) {
                if (transactions.get(i).getId().equals(transaction.getId())) {
                    transactions.set(i, transaction);
                    saveUserData();
                    notifyDataChanged();
                    break;
                }
            }
        }
    }

    public void deleteTransaction(String transactionId) {
        Log.d(TAG, "Deleting transaction with ID: " + transactionId);
        if (userData != null && userData.getTransactions() != null) {
            List<Transaction> transactions = userData.getTransactions();
            Log.d(TAG, "Current transactions count: " + transactions.size());
            for (int i = 0; i < transactions.size(); i++) {
                if (transactions.get(i).getId().equals(transactionId)) {
                    transactions.remove(i);
                    // Cập nhật lại danh sách giao dịch trong DataManager
                    this.transactions = new ArrayList<>(transactions);
                    Log.d(TAG, "Transaction deleted. New transactions count: " + transactions.size());
                    saveUserData();
                    notifyDataChanged();
                    break;
                }
            }
        } else {
            Log.e(TAG, "Cannot delete transaction: userData is null or transactions list is null");
        }
    }

    private void syncCategoriesFromUserData() {
        categories.clear();
        if (userData != null && userData.getCategories() != null) {
            for (List<Category> typeCategories : userData.getCategories().values()) {
                categories.addAll(typeCategories);
            }
        }
    }

    public void addCategory(Category category) {
        Log.d(TAG, "Adding new category: " + category.getName());
        
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Cannot add category: user is not logged in");
            return;
        }

        String userId = currentUser.getUid();
        
        // Thêm vào danh sách local
        categories.add(category);
        
        // Thêm vào userData
        if (userData != null) {
            // Lấy danh sách categories hiện tại
            Map<String, List<Category>> userCategories = userData.getCategories();
            if (userCategories == null) {
                userCategories = new HashMap<>();
                // Khởi tạo mảng expense và income nếu chưa có
                userCategories.put("expense", new ArrayList<>());
                userCategories.put("income", new ArrayList<>());
                userData.setCategories(userCategories);
            }

            // Lấy danh sách categories theo type
            List<Category> typeCategories = userCategories.get(category.getType());
            if (typeCategories == null) {
                typeCategories = new ArrayList<>();
                userCategories.put(category.getType(), typeCategories);
            }

            // Thêm category mới vào danh sách local
            typeCategories.add(category);
            
            // Lưu vào Firestore - thêm vào mảng trong Map categories
            db.collection("users").document(userId)
                .update("categories." + category.getType(), FieldValue.arrayUnion(category))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Category added successfully to database");
                    syncCategoriesFromUserData(); // Đồng bộ lại danh sách categories
                    notifyDataChanged();
                    if (dataLoadedListener != null) {
                        dataLoadedListener.onDataLoaded();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding category to database", e);
                    // Rollback local changes
                    categories.remove(category);
                    // Tải lại dữ liệu từ database để đồng bộ
                    loadUserData(); 
                    if (dataLoadedListener != null) {
                        dataLoadedListener.onError(e.getMessage());
                    }
                });
        } else {
            Log.e(TAG, "Cannot add category: userData is null");
        }
    }

    public void deleteCategory(Category category) {
        Log.d(TAG, "Deleting category: " + category.getName());
        
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Cannot delete category: user is not logged in");
            return;
        }

        String userId = currentUser.getUid();
        
        if (userData != null && userData.getCategories() != null) {
            List<Category> categoriesByType = userData.getCategories().get(category.getType());
            if (categoriesByType != null) {
                categoriesByType.remove(category);
                
                // Xóa khỏi Firestore
                db.collection("users").document(userId)
                    .update("categories." + category.getType(), FieldValue.arrayRemove(category))
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Category deleted successfully from database");
                        syncCategoriesFromUserData(); // Đồng bộ lại danh sách categories
                        notifyDataChanged();
                        if (dataLoadedListener != null) {
                            dataLoadedListener.onDataLoaded();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error deleting category from database", e);
                        // Rollback local changes
                        categoriesByType.add(category);
                        // Tải lại dữ liệu từ database để đồng bộ
                        loadUserData();
                        if (dataLoadedListener != null) {
                            dataLoadedListener.onError(e.getMessage());
                        }
                    });
            }
        }
    }

    public List<Transaction> getTransactionsByType(String type) {
        if (userData == null) return new ArrayList<>();
        return userData.getTransactionsByType(type);
    }

    public List<Transaction> getTransactionsByDateRange(Date startDate, Date endDate) {
        List<Transaction> filteredTransactions = new ArrayList<>();
        for (Transaction transaction : transactions) {
            if (!transaction.getDate().before(startDate) && !transaction.getDate().after(endDate)) {
                filteredTransactions.add(transaction);
            }
        }
        Log.d(TAG, "Getting transactions by date range: " + filteredTransactions.size());
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
        for (Category category : categories) {
            if (category.getId().equals(categoryId) && category.getType().equals(type)) {
                return category;
            }
        }
        return null; // Return null if category not found
    }

    public List<Category> getCategoriesByType(String type) {
        List<Category> filteredCategories = new ArrayList<>();
        for (Category category : categories) {
            if (category.getType().equals(type)) {
                filteredCategories.add(category);
            }
        }
        Log.d(TAG, "Getting categories by type " + type + ": " + filteredCategories.size());
        return filteredCategories;
    }
} 