package com.example.afinal;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
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

public class GroupCharge extends AppCompatActivity {

    private ImageView imgPreview;
    private EditText etAmount, etNote;
    private LinearLayout payerAmountContainer;
    private FlexboxLayout memberSelectionContainer;
    private List<String> members = Arrays.asList("我", "A", "B");
    private Map<String, EditText> payerInputs = new HashMap<>();
    private Uri selectedImageUri = null;
    private boolean[] selectedPayers;
    private List<String> chosenPayers = new ArrayList<>();
    private TextView tvSelectPayers;


    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                        imgPreview.setImageBitmap(bitmap);
                    } catch (IOException e) {
                        Toast.makeText(this, "圖片讀取失敗", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private final ActivityResultLauncher<Intent> takePhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                    imgPreview.setImageBitmap(photo);
                    selectedImageUri = null;
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_charge);

        imgPreview = findViewById(R.id.imgPreview);
        etAmount = findViewById(R.id.etAmount);
        etNote = findViewById(R.id.etNote);
        payerAmountContainer = findViewById(R.id.payerAmountContainer);
        memberSelectionContainer = findViewById(R.id.memberSelectionContainer);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        imgPreview.setOnClickListener(v -> showImageOptions());

        // 初始隱藏付款欄位
        payerAmountContainer.setVisibility(View.GONE);

        // Spinner 初始化
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, members);

        tvSelectPayers = findViewById(R.id.tvSelectPayers);
        selectedPayers = new boolean[members.size()];

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
                        updatePayerInputFields();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        // 建立分帳對象勾選框
        for (String name : members) {
            CheckBox cb = new CheckBox(this);
            cb.setText(name);
            cb.setChecked(true);
            memberSelectionContainer.addView(cb);
        }

        findViewById(R.id.btnConfirm).setOnClickListener(v -> calculateSplit());
    }

    private void showImageOptions() {
        new AlertDialog.Builder(this)
                .setTitle("選擇圖片來源")
                .setItems(new CharSequence[]{"拍照", "從相簿選取"}, (dialog, which) -> {
                    if (which == 0) {
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

        List<String> selectedMembers = new ArrayList<>();
        for (int i = 0; i < memberSelectionContainer.getChildCount(); i++) {
            CheckBox cb = (CheckBox) memberSelectionContainer.getChildAt(i);
            if (cb.isChecked()) selectedMembers.add(cb.getText().toString());
        }

        if (selectedMembers.isEmpty()) {
            Toast.makeText(this, "請選擇至少一位分帳對象", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Double> actualPayments = new HashMap<>();
        for (String name : chosenPayers) {
            EditText input = payerInputs.get(name);
            String value = input.getText().toString().trim();
            double paid = value.isEmpty() ? 0 : Double.parseDouble(value);
            actualPayments.put(name, paid);
        }

        double perPerson = totalAmount / selectedMembers.size();

        StringBuilder result = new StringBuilder("備註：「" + note + "」\n總金額 NT$" + totalAmount + "\n\n");
        for (String name : selectedMembers) {
            double paid = actualPayments.getOrDefault(name, 0.0);
            double balance = paid - perPerson;
            result.append(name)
                    .append(" 已付 NT$").append(paid)
                    .append(" → ")
                    .append(balance >= 0 ? "收回" : "應付")
                    .append(" NT$").append(String.format("%.2f", Math.abs(balance)))
                    .append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("分帳結果")
                .setMessage(result.toString())
                .setPositiveButton("確認", (dialog, which) -> {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("summary", result.toString());
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .show();
    }

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
