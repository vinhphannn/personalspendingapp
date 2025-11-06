# Ứng dụng Quản lý Chi tiêu Cá nhân (Personal Spending App)

![Logo](app/src/main/res/drawable/personal_spending_app_logo_512x512.png)

## 📝 Giới thiệu

**Personal Spending App** là một ứng dụng Android được xây dựng bằng Java, giúp người dùng theo dõi và quản lý các khoản thu nhập và chi tiêu hàng ngày một cách hiệu quả. Ứng dụng sử dụng Firebase làm backend để lưu trữ và đồng bộ hóa dữ liệu real-time, đảm bảo người dùng có thể truy cập dữ liệu của mình từ bất kỳ thiết bị Android nào.

---

## ✨ Tính năng nổi bật

- **✅ Quản lý Giao dịch:** Dễ dàng thêm, sửa, xóa các giao dịch thu nhập và chi tiêu.
- **📊 Phân loại thông minh:** Gán giao dịch vào các danh mục (Ăn uống, Di chuyển, Lương,...) để tiện theo dõi.
- **📅 Lịch sử G[object Object] Báo cáo & Thống kê:** Biểu đồ tròn và biểu đồ cột giúp hình dung rõ ràng về tình hình tài chính.
- **📄 Xuất báo cáo PDF:** Xuất báo cáo chi tiêu ra file PDF để lưu trữ hoặc chia sẻ.
- **🔔 Thông báo thông minh:**
  - Nhắc nhở nhập liệu hàng ngày.
  - Gửi tổng kết chi tiêu hàng tuần.
  - Cảnh báo khi có chi tiêu lớn bất thường.
- **☁️ Đồng bộ hóa Real-time:** Dữ liệu được lưu trữ an toàn trên Firebase và đồng bộ hóa ngay lập tức.
- **⚙️ Tùy chỉnh:** Người dùng có thể tự tạo và quản lý các danh mục chi tiêu/thu nhập của riêng mình.
- **🔐 Xác thực người dùng:** Đăng nhập và đăng ký an toàn bằng Firebase Authentication.

---

## 🚀 Công nghệ sử dụng

- **Ngôn ngữ:** Java
- **Nền tảng:** Android (Min SDK 24)
- **Backend & Database:**
  - **Firebase Firestore:** Lưu trữ dữ liệu NoSQL.
  - **Firebase Authentication:** Xác thực người dùng.
  - **Firebase Cloud Messaging:** Gửi thông báo đẩy.
- **Thư viện:**
  - **MPAndroidChart:** Vẽ biểu đồ thống kê.
  - **iTextPDF:** Xuất file báo cáo PDF.
  - **AndroidX WorkManager:** Lên lịch các tác vụ nền (thông báo).
  - **Material Design 3:** Thiết kế giao diện người dùng hiện đại.
  - **ViewPager2 & RecyclerView:** Hiển thị danh sách và các tab.

---

## 🔥 Cấu trúc Firebase

Dữ liệu người dùng được tổ chức trong Firestore theo cấu trúc sau:

```json
{
  "users": {
    "{userId}": {
      "profile": {
        "email": "user@example.com",
        "currency": "VND",
        "language": "vi"
      },
      "transactions": [
        {
          "id": "tran_123",
          "amount": 50000,
          "type": "expense",
          "categoryId": "cat_expense_1",
          "note": "Cà phê với bạn bè",
          "date": 1672531200000
        }
      ],
      "categories": {
        "income": [
          { "id": "cat_income_[object Object]{ "id": "cat_expense_1", "name": "Ăn uống", "icon": "🍔" }
        ]
      }
    }
  }
}
```

---

## 🛠️ Hướng dẫn cài đặt

1. **Clone a repository:**

   ```bash
   git clone https://your-repository-url.git
   ```
2. **Mở dự án bằng Android Studio.**
3. **Kết nối với Firebase:**

   - đã kết nối sẵn
4. **Build và chạy ứng dụng.**

---

## 🔮 Hướng phát triển trong tương lai

- [ ] **Hỗ trợ Offline:** Cho phép người dùng xem và thêm giao dịch khi không có mạng.
- [ ] **Thêm Unit Tests:** Đảm bảo sự ổn định của ứng dụng.
- [ ] **Tối ưu hóa hiệu năng:** Cải thiện tốc độ tải cho người dùng có lượng giao dịch lớn.
- [ ] **Thêm tính năng Ngân sách (Budgeting):** Cho phép người dùng đặt ra hạn mức chi tiêu cho từng danh mục.
- [ ] **Giao diện Tablet:** Tối ưu hóa giao diện cho các thiết bị màn hình lớn.
- [ ] **Đa ngôn ngữ:** Hỗ trợ nhiều ngôn ngữ khác nhau.
