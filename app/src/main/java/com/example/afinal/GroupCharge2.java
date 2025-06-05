package com.example.afinal;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.flexbox.FlexboxLayout;

import java.io.IOException;
import java.util.*;

public class GroupCharge2 extends AppCompatActivity {

    // 宣告 UI 元件與資料結構
    private ImageView imgPreview;
    private EditText etAmount, etNote;
    private LinearLayout payerAmountContainer;
    private FlexboxLayout memberSelectionContainer;

    // 預設所有群組成員名稱
    private List<String> members = Arrays.asList("我", "A", "B", "C", "D", "E");

    // 用於記錄付款人與其對應的金額輸入框
    private Map<String, EditText> payerInputs = new HashMap<>();

    // 圖片 Uri（可選擇或拍照）
    private Uri selectedImageUri = null;

    // 用於記錄選擇的付款人
    private boolean[] selectedPayers;
    private List<String> chosenPayers = new ArrayList<>();

    private TextView tvSelectPayers;
    private TextView tvDate;

    // 紀錄使用者選擇的日期
    private Calendar selectedDate = Calendar.getInstance();

    // 相簿選取圖片
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                        imgPreview.setImageBitmap(bitmap); // 預覽圖片
                    } catch (IOException e) {
                        Toast.makeText(this, "圖片讀取失敗", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    // 拍照取得圖片
    private final ActivityResultLauncher<Intent> takePhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                    imgPreview.setImageBitmap(photo); // 預覽相片
                    selectedImageUri = null;
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_charge2);

        // 初始化 UI 元件
        imgPreview = findViewById(R.id.imgPreview);
        etAmount = findViewById(R.id.etAmount);
        etNote = findViewById(R.id.etNote);
        payerAmountContainer = findViewById(R.id.payerAmountContainer);
        memberSelectionContainer = findViewById(R.id.memberSelectionContainer);
        tvDate = findViewById(R.id.tvDate);
        tvSelectPayers = findViewById(R.id.tvSelectPayers);
        selectedPayers = new boolean[members.size()];

        // 返回上一頁
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 點擊圖片可選擇或拍照
        imgPreview.setOnClickListener(v -> showImageOptions());

        // 點擊選擇消費日期
        tvDate.setOnClickListener(v -> {
            int year = selectedDate.get(Calendar.YEAR);
            int month = selectedDate.get(Calendar.MONTH);
            int day = selectedDate.get(Calendar.DAY_OF_MONTH);

            new DatePickerDialog(GroupCharge2.this, (view, y, m, d) -> {
                selectedDate.set(y, m, d);
                tvDate.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d));
            }, year, month, day).show();
        });

        // 初始不顯示付款金額欄位
        payerAmountContainer.setVisibility(View.GONE);

        // 點擊選擇付款人，彈出多選 Dialog
        tvSelectPayers.setOnClickListener(v -> {
            String[] names = members.toArray(new String[0]);
            new AlertDialog.Builder(this)
                    .setTitle("選擇付款人")
                    .setMultiChoiceItems(names, selectedPayers, (dialog, which, isChecked) -> {
                        if (isChecked) {
                            if (!chosenPayers.contains(names[which])) chosenPayers.add(names[which]);
                        } else {
                            chosenPayers.remove(names[which]);
                        }
                    })
                    .setPositiveButton("確定", (dialog, which) -> {
                        updatePayerInputFields(); // 根據選擇更新輸入欄位
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        // 為每個成員建立勾選框（是否要參與分帳）
        for (String name : members) {
            CheckBox cb = new CheckBox(this);
            cb.setText(name);
            cb.setChecked(true); // 預設全選
            memberSelectionContainer.addView(cb);
        }

        // 點擊確認計算分帳
        findViewById(R.id.btnConfirm).setOnClickListener(v -> calculateSplit());
    }

    // 顯示選擇圖片來源的對話框
    private void showImageOptions() {
        new AlertDialog.Builder(this)
                .setTitle("選擇圖片來源")
                .setItems(new CharSequence[]{"拍照", "從相簿選取"}, (dialog, which) -> {
                    if (which == 0) {
                        // 拍照需要相機權限
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.CAMERA}, 1001);
                        } else {
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            takePhotoLauncher.launch(intent);
                        }
                    } else {
                        Intent intent = new Intent(Intent.ACTION_PICK);
                        intent.setType("image/*");
                        pickImageLauncher.launch(intent);
                    }
                })
                .show();
    }

    // 計算每人應付金額，並顯示分帳結果
    private void calculateSplit() {
        String note = etNote.getText().toString().trim();
        String totalStr = etAmount.getText().toString().trim();

        if (note.isEmpty() || totalStr.isEmpty()) {
            Toast.makeText(this, "請輸入金額與備註", Toast.LENGTH_SHORT).show();
            return;
        }

        double totalAmount;
        try {
            totalAmount = Double.parseDouble(totalStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "金額格式錯誤", Toast.LENGTH_SHORT).show();
            return;
        }

        // 蒐集有被勾選的成員（參與分帳）
        List<String> selectedMembers = new ArrayList<>();
        for (int i = 0; i < memberSelectionContainer.getChildCount(); i++) {
            CheckBox cb = (CheckBox) memberSelectionContainer.getChildAt(i);
            if (cb.isChecked()) selectedMembers.add(cb.getText().toString());
        }

        if (selectedMembers.isEmpty()) {
            Toast.makeText(this, "請選擇至少一位分帳對象", Toast.LENGTH_SHORT).show();
            return;
        }

        // 建立每位付款人的實際付款金額資料
        Map<String, Double> actualPayments = new HashMap<>();
        for (String name : chosenPayers) {
            EditText input = payerInputs.get(name);
            String value = input.getText().toString().trim();
            double paid = value.isEmpty() ? 0 : Double.parseDouble(value);
            actualPayments.put(name, paid);
        }

        // 計算每人應付金額（平均分攤）
        double perPerson = totalAmount / selectedMembers.size();

        // 檢查是否已選擇日期
        String dateText = tvDate.getText().toString().trim();
        if (dateText.equals("請選擇日期")) {
            Toast.makeText(this, "請選擇消費日期", Toast.LENGTH_SHORT).show();
            return;
        }

        // 組合結果文字
        StringBuilder result = new StringBuilder("消費日期：" + dateText + "\n");
        result.append("備註：「").append(note).append("」\n總金額 NT$").append(totalAmount).append("\n\n");

        // 將 allInvolved 改為付款人與分帳人總合
        Set<String> allInvolved = new LinkedHashSet<>();
        allInvolved.addAll(selectedMembers);
        allInvolved.addAll(actualPayments.keySet());

        for (String name : allInvolved) {
            double paid = actualPayments.getOrDefault(name, 0.0);
            double balance = selectedMembers.contains(name) ? (paid - perPerson) : paid;
            result.append(name)
                    .append(" 已付 NT$").append(paid)
                    .append(" → ")
                    .append(balance >= 0 ? "收回" : "應付")
                    .append(" NT$").append(String.format("%.2f", Math.abs(balance)))
                    .append("\n");
        }

        // 顯示分帳結果對話框，並回傳至前一頁（或結束）
        new AlertDialog.Builder(this)
                .setTitle("分帳結果")
                .setMessage(result.toString())
                .setPositiveButton("確認", (dialog, which) -> {
                    // 建立單行紀錄文字，例如：2025-05-28 - 我 NT$200
                    String record = String.format("%s - %s NT$%.0f", dateText,
                            chosenPayers.isEmpty() ? "未知付款人" : chosenPayers.get(0), totalAmount);
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("summary", result.toString());
                    resultIntent.putExtra("record", record);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .show();
    }

    // 拍照權限結果處理
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 1001 && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePhotoLauncher.launch(intent);
        } else {
            Toast.makeText(this, "未取得相機權限", Toast.LENGTH_SHORT).show();
        }
    }

    // 更新付款金額欄位（根據選擇的付款人）
    private void updatePayerInputFields() {
        payerAmountContainer.removeAllViews();
        payerInputs.clear();

        if (chosenPayers.isEmpty()) {
            payerAmountContainer.setVisibility(View.GONE);
            tvSelectPayers.setText("請選擇付款人");
            return;
        }

        payerAmountContainer.setVisibility(View.VISIBLE);
        tvSelectPayers.setText("付款人：" + String.join(", ", chosenPayers));

        for (String name : chosenPayers) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 8, 0, 8);

            TextView tv = new TextView(this);
            tv.setText(name + "：");
            tv.setTextSize(16);
            tv.setWidth(120);

            EditText input = new EditText(this);
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setHint("付款金額");
            input.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            payerInputs.put(name, input);

            row.addView(tv);
            row.addView(input);
            payerAmountContainer.addView(row);
        }
    }
}
