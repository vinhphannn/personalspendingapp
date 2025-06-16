package com.example.personalspendingapp.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Environment;
import android.util.Log;

import com.example.personalspendingapp.data.DataManager;
import com.example.personalspendingapp.models.Category;
import com.example.personalspendingapp.models.Transaction;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.BaseFont;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportExporter {
    private static final String TAG = "ReportExporter";
    private Context context;
    private DataManager dataManager;
    private Document document;
    private String fileName;
    private Font unicodeFont;
    private Font unicodeBoldFont;
    private Font unicodeTitleFont;

    public ReportExporter(Context context) {
        this.context = context;
        this.dataManager = DataManager.getInstance();
        try {
            // Đảm bảo Roboto-Regular.ttf nằm trong app/src/main/assets
            String fontPath = "Roboto-Regular.ttf";
            InputStream fontStream = context.getAssets().open(fontPath);
            byte[] fontBytes = new byte[fontStream.available()];
            fontStream.read(fontBytes);
            fontStream.close();

            // Tạo BaseFont từ mảng byte của font
            BaseFont baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, BaseFont.CACHED, fontBytes, null);

            unicodeFont = new Font(baseFont, 12, Font.NORMAL, BaseColor.BLACK);
            unicodeBoldFont = new Font(baseFont, 12, Font.BOLD, BaseColor.BLACK);
            unicodeTitleFont = new Font(baseFont, 24, Font.BOLD, BaseColor.BLACK);

        } catch (IOException | DocumentException e) {
            Log.e(TAG, "Lỗi tải font từ assets: " + e.getMessage());
            // Fallback về font mặc định nếu tải font tùy chỉnh thất bại
            unicodeFont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.NORMAL, BaseColor.BLACK);
            unicodeBoldFont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD, BaseColor.BLACK);
            unicodeTitleFont = new Font(Font.FontFamily.TIMES_ROMAN, 24, Font.BOLD, BaseColor.BLACK);
        }
    }

    public File exportFinancialReport() {
        try {
            // Tạo tên file với timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            fileName = "Financial_Report_" + timestamp + ".pdf";
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);

            // Khởi tạo document
            document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Thêm header
            addHeader();
            
            // Thêm thông tin tổng quan
            addOverview();
            
            // Thêm biểu đồ phân bố thu chi
            addPieCharts();
            
            // Thêm danh sách giao dịch theo tháng
            addMonthlyTransactions();
            
            // Thêm thông tin ứng dụng
            addAppInfo();

            document.close();
            return file;
        } catch (Exception e) {
            Log.e(TAG, "Error exporting report: " + e.getMessage());
            return null;
        }
    }

    private void addHeader() throws DocumentException {
        Paragraph title = new Paragraph("BÁO CÁO TÀI CHÍNH", unicodeTitleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        Paragraph dateParagraph = new Paragraph("Ngày xuất báo cáo: " + date, unicodeFont);
        dateParagraph.setAlignment(Element.ALIGN_CENTER);
        dateParagraph.setSpacingAfter(30);
        document.add(dateParagraph);
    }

    private void addOverview() throws DocumentException {
        Paragraph sectionTitle = new Paragraph("TỔNG QUAN TÀI CHÍNH", unicodeBoldFont);
        sectionTitle.setSpacingBefore(20);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        // Tính toán tổng thu chi
        double totalIncome = 0;
        double totalExpense = 0;
        List<Transaction> transactions = dataManager.getUserData().getTransactions();
        for (Transaction transaction : transactions) {
            if (transaction.getType().equals("income")) {
                totalIncome += transaction.getAmount();
            } else {
                totalExpense += transaction.getAmount();
            }
        }

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        // Thêm header
        PdfPCell headerCell = new PdfPCell(new Phrase("CHỈ SỐ", unicodeBoldFont));
        headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell.setPadding(5);
        table.addCell(headerCell);

        headerCell = new PdfPCell(new Phrase("GIÁ TRỊ", unicodeBoldFont));
        headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell.setPadding(5);
        table.addCell(headerCell);

        // Thêm dữ liệu với màu sắc
        addTableRow(table, "Tổng thu nhập:", String.format("%,.0f VNĐ", totalIncome), unicodeFont, unicodeBoldFont, BaseColor.GREEN);
        addTableRow(table, "Tổng chi tiêu:", String.format("%,.0f VNĐ", totalExpense), unicodeFont, unicodeBoldFont, BaseColor.RED);
        addTableRow(table, "Số dư:", String.format("%,.0f VNĐ", totalIncome - totalExpense), unicodeFont, unicodeBoldFont, 
            (totalIncome - totalExpense >= 0) ? BaseColor.BLUE : BaseColor.RED);

        document.add(table);
    }

    private void addPieCharts() throws DocumentException, IOException {
        // Thêm biểu đồ thu nhập
        Paragraph incomeTitle = new Paragraph("PHÂN BỐ THU NHẬP", unicodeBoldFont);
        incomeTitle.setSpacingBefore(20);
        incomeTitle.setSpacingAfter(10);
        document.add(incomeTitle);

        Bitmap incomeChart = createPieChart("income");
        Image incomeImage = Image.getInstance(bitmapToByteArray(incomeChart));
        incomeImage.setAlignment(Element.ALIGN_CENTER);
        incomeImage.scaleToFit(400, 400);
        document.add(incomeImage);

        // Thêm bảng chi tiết thu nhập
        addCategoryDetailsTable("income", unicodeFont, unicodeBoldFont);

        // Thêm biểu đồ chi tiêu
        Paragraph expenseTitle = new Paragraph("PHÂN BỐ CHI TIÊU", unicodeBoldFont);
        expenseTitle.setSpacingBefore(20);
        expenseTitle.setSpacingAfter(10);
        document.add(expenseTitle);

        Bitmap expenseChart = createPieChart("expense");
        Image expenseImage = Image.getInstance(bitmapToByteArray(expenseChart));
        expenseImage.setAlignment(Element.ALIGN_CENTER);
        expenseImage.scaleToFit(400, 400);
        document.add(expenseImage);

        // Thêm bảng chi tiết chi tiêu
        addCategoryDetailsTable("expense", unicodeFont, unicodeBoldFont);
    }

    private void addCategoryDetailsTable(String type, Font normalFont, Font valueFont) throws DocumentException {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(20);

        // Thêm header
        String[] headers = {"Danh mục", "Số tiền", "Tỷ lệ"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, unicodeBoldFont));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }

        // Tính toán tổng và tỷ lệ
        Map<Category, Double> categoryAmounts = new HashMap<>();
        double total = 0;
        for (Transaction transaction : dataManager.getUserData().getTransactions()) {
            if (transaction.getType().equals(type)) {
                Category category = dataManager.getCategoryById(transaction.getCategoryId(), transaction.getType());
                if (category != null) {
                    double amount = transaction.getAmount();
                    categoryAmounts.put(category, categoryAmounts.getOrDefault(category, 0.0) + amount);
                    total += amount;
                }
            }
        }

        // Thêm dữ liệu
        for (Map.Entry<Category, Double> entry : categoryAmounts.entrySet()) {
            table.addCell(new Phrase(entry.getKey().getName(), unicodeFont));
            table.addCell(new Phrase(String.format("%,.0f VNĐ", entry.getValue()), unicodeBoldFont));
            table.addCell(new Phrase(String.format("%.1f%%", (entry.getValue() / total * 100)), unicodeBoldFont));
        }

        document.add(table);
    }

    private void addMonthlyTransactions() throws DocumentException {
        Paragraph sectionTitle = new Paragraph("CHI TIẾT GIAO DỊCH THEO THÁNG", unicodeBoldFont);
        sectionTitle.setSpacingBefore(20);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        // Nhóm giao dịch theo tháng
        Map<String, List<Transaction>> monthlyTransactions = new HashMap<>();
        SimpleDateFormat monthFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
        
        for (Transaction transaction : dataManager.getUserData().getTransactions()) {
            String month = monthFormat.format(transaction.getDate());
            if (!monthlyTransactions.containsKey(month)) {
                monthlyTransactions.put(month, new ArrayList<>());
            }
            monthlyTransactions.get(month).add(transaction);
        }

        // Tạo bảng cho từng tháng
        for (Map.Entry<String, List<Transaction>> entry : monthlyTransactions.entrySet()) {
            String month = entry.getKey();
            List<Transaction> transactions = entry.getValue();

            Paragraph monthTitle = new Paragraph("Tháng " + month, unicodeBoldFont);
            monthTitle.setSpacingBefore(15);
            monthTitle.setSpacingAfter(10);
            document.add(monthTitle);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setSpacingAfter(10);

            // Thêm header
            String[] headers = {"Ngày", "Danh mục", "Số tiền", "Ghi chú"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, unicodeBoldFont));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }

            // Thêm dữ liệu
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            for (Transaction transaction : transactions) {
                // Ngày
                PdfPCell dateCell = new PdfPCell(new Phrase(dateFormat.format(transaction.getDate()), unicodeFont));
                dateCell.setPadding(5);
                table.addCell(dateCell);

                // Danh mục
                Category category = dataManager.getCategoryById(transaction.getCategoryId(), transaction.getType());
                PdfPCell categoryCell = new PdfPCell(new Phrase(category != null ? category.getName() : "Không xác định", unicodeFont));
                categoryCell.setPadding(5);
                table.addCell(categoryCell);

                // Số tiền
                String amount = String.format("%,.0f VNĐ", transaction.getAmount());
                PdfPCell amountCell = new PdfPCell(new Phrase(amount, unicodeBoldFont));
                amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                amountCell.setPadding(5);
                amountCell.setBackgroundColor(transaction.getType().equals("income") ? BaseColor.GREEN : BaseColor.ORANGE);
                table.addCell(amountCell);

                // Ghi chú
                PdfPCell noteCell = new PdfPCell(new Phrase(transaction.getNote(), unicodeFont));
                noteCell.setPadding(5);
                table.addCell(noteCell);
            }

            document.add(table);
        }
    }

    private void addAppInfo() throws DocumentException {
        Paragraph sectionTitle = new Paragraph("THÔNG TIN ỨNG DỤNG", unicodeBoldFont);
        sectionTitle.setSpacingBefore(20);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        // Thêm header
        PdfPCell headerCell = new PdfPCell(new Phrase("THÔNG TIN", unicodeBoldFont));
        headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell.setPadding(5);
        table.addCell(headerCell);

        headerCell = new PdfPCell(new Phrase("CHI TIẾT", unicodeBoldFont));
        headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell.setPadding(5);
        table.addCell(headerCell);

        // Thêm dữ liệu
        addTableRow(table, "Tên ứng dụng:", "Personal Spending App", unicodeFont, unicodeBoldFont, null);
        addTableRow(table, "Phiên bản:", "1.0.0", unicodeFont, unicodeBoldFont, null);
        addTableRow(table, "Nhà phát hành:", "N5 SV CNTT k64_CNTT", unicodeFont, unicodeBoldFont, null);
        addTableRow(table, "Website:", "https://github.com/vinhphannn/personalspendingapp", unicodeFont, unicodeBoldFont, null);
        addTableRow(table, "Email:", "info.vinhphan@gmail.com", unicodeFont, unicodeBoldFont, null);

        document.add(table);
    }

    private void addTableRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont, BaseColor valueColor) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setPadding(5);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        if (valueColor != null) {
            valueCell.setBackgroundColor(valueColor);
        }
        table.addCell(valueCell);
    }

    private Bitmap createPieChart(String type) {
        int width = 400;
        int height = 400;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Vẽ nền trắng
        paint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, width, height, paint);

        // Lấy dữ liệu cho biểu đồ
        Map<Category, Double> categoryAmounts = new HashMap<>();
        double total = 0;
        for (Transaction transaction : dataManager.getUserData().getTransactions()) {
            if (transaction.getType().equals(type)) {
                Category category = dataManager.getCategoryById(transaction.getCategoryId(), transaction.getType());
                if (category != null) {
                    double amount = transaction.getAmount();
                    categoryAmounts.put(category, categoryAmounts.getOrDefault(category, 0.0) + amount);
                    total += amount;
                }
            }
        }

        // Vẽ biểu đồ tròn
        float centerX = width / 2f;
        float centerY = height / 2f;
        float radius = Math.min(width, height) / 3f;
        RectF rect = new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

        float startAngle = 0;
        int[] colors = {Color.rgb(255, 99, 132), Color.rgb(54, 162, 235), Color.rgb(255, 206, 86),
                       Color.rgb(75, 192, 192), Color.rgb(153, 102, 255), Color.rgb(255, 159, 64)};
        int colorIndex = 0;

        for (Map.Entry<Category, Double> entry : categoryAmounts.entrySet()) {
            float sweepAngle = (float) (entry.getValue() / total * 360);
            
            // Vẽ phần biểu đồ
            paint.setColor(colors[colorIndex % colors.length]);
            canvas.drawArc(rect, startAngle, sweepAngle, true, paint);
            
            // Vẽ nhãn
            float labelAngle = startAngle + sweepAngle / 2;
            float labelX = centerX + (float) Math.cos(Math.toRadians(labelAngle)) * (radius * 0.7f);
            float labelY = centerY + (float) Math.sin(Math.toRadians(labelAngle)) * (radius * 0.7f);
            
            paint.setColor(Color.BLACK);
            paint.setTextSize(20);
            paint.setTextAlign(Paint.Align.CENTER);
            String label = entry.getKey().getName() + "\n" + String.format("%.1f%%", (entry.getValue() / total * 100));
            canvas.drawText(label, labelX, labelY, paint);
            
            startAngle += sweepAngle;
            colorIndex++;
        }

        return bitmap;
    }

    private byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }
} 