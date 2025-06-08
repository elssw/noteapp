package com.example.afinal;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.afinal.model.Member;
import com.google.android.flexbox.FlexboxLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.util.*;

public class GroupChargeEdit extends AppCompatActivity {

    // 主要 UI 元件
    private ImageView imgPreview; // 顯示選取或拍攝的圖片
    private EditText etAmount, etNote; // 使用者輸入金額與備註欄位
    private LinearLayout payerAmountContainer; // 動態產生每位付款人輸入欄位的容器
    private FlexboxLayout memberSelectionContainer; // 勾選每位成員是否參與分帳的區塊

    // 群組成員清單（實際使用時應改為從後端/Intent 取得）
    private List<Member> members = new ArrayList<>();
    private ArrayList<String> originalPayers;
    private ArrayList<Double> payerAmounts;
    private ArrayList<String> originalParticipants;
    private String groupId;
    private boolean isMembersLoaded = false;
    // 記錄付款人對應的輸入框（姓名 => 金額輸入欄）
    private Map<String, EditText> payerInputs = new HashMap<>();

    // 記錄圖片 Uri，若為 null 表示尚未選擇圖片
    private Uri selectedImageUri = null;

    // 用於記錄哪些人被選為付款人
    private boolean[] selectedPayers;
    private List<Member> chosenPayers = new ArrayList<>();

    private TextView tvSelectPayers; // 顯示「請選擇付款人」或已選清單
    private TextView tvDate; // 顯示消費日期

    // 使用者選取的日期
    private Calendar selectedDate = Calendar.getInstance();

    // 顯示 nickname 並建立對應
    private Map<String, String> nicknameToEmail = new HashMap<>();
    private Map<String, String> emailToNickname = new HashMap<>();

    private String myEmail; // eamil
    private String myNickname; // 暱稱


    // ----------------------------- 圖片選擇相關功能 -----------------------------

    // 使用者從相簿選圖片後的結果處理
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

    // 使用者拍照後的結果處理
    private final ActivityResultLauncher<Intent> takePhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                    imgPreview.setImageBitmap(photo);
                    selectedImageUri = null; // 若是拍照則不保留 Uri
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_charge_edit);

        imgPreview = findViewById(R.id.imgPreview);
        etAmount = findViewById(R.id.etAmount);
        etNote = findViewById(R.id.etNote);
        payerAmountContainer = findViewById(R.id.payerAmountContainer);
        memberSelectionContainer = findViewById(R.id.memberSelectionContainer);
        tvDate = findViewById(R.id.tvDate);
        tvSelectPayers = findViewById(R.id.tvSelectPayers);

        Intent intent = getIntent();
        groupId = intent.getStringExtra("groupId");
        String recordId = intent.getStringExtra("recordId");

        SharedPreferences prefs = getSharedPreferences("login", MODE_PRIVATE);
        String userId = prefs.getString("userid", "0");

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 🔹 1. 先讀取該筆紀錄（amount, note, date, payers, payerAmounts, participants）
        db.collection("users")
                .document(userId)
                .collection("group")
                .document(groupId)
                .collection("records")
                .document(recordId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        double amount = documentSnapshot.getDouble("amount");
                        String note = documentSnapshot.getString("note");
                        String date = documentSnapshot.getString("date");

                        originalPayers = (ArrayList<String>) documentSnapshot.get("payers");
                        payerAmounts = (ArrayList<Double>) documentSnapshot.get("payerAmounts");
                        originalParticipants = (ArrayList<String>) documentSnapshot.get("participants");

                        etAmount.setText(String.valueOf((int) amount));
                        etNote.setText(note);
                        tvDate.setText(date);

                        // 🔹 2. 載入成員後再還原畫面
                        db.collection("users")
                                .document(userId)
                                .collection("group")
                                .document(groupId)
                                .get()
                                .addOnSuccessListener(groupDoc -> {
                                    List<String> memberEmails = (List<String>) groupDoc.get("members");
                                    if (memberEmails != null && !memberEmails.isEmpty()) {
                                        members.clear();
                                        for (String email : memberEmails) {
                                            db.collection("users").document(email).get().addOnSuccessListener(userSnap -> {
                                                String nickname = userSnap.getString("nickname");
                                                if (nickname == null || nickname.isEmpty()) nickname = email;
                                                members.add(new Member(email, nickname));

                                                // 成員都抓完後
                                                if (members.size() == memberEmails.size()) {
                                                    restoreUI();
                                                }
                                            });
                                        }
                                    }
                                });
                    }
                });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        imgPreview.setOnClickListener(v -> showImageOptions());
        tvDate.setOnClickListener(v -> {
            int year = selectedDate.get(Calendar.YEAR);
            int month = selectedDate.get(Calendar.MONTH);
            int day = selectedDate.get(Calendar.DAY_OF_MONTH);
            new DatePickerDialog(this, (view, y, m, d) -> {
                selectedDate.set(y, m, d);
                tvDate.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d));
            }, year, month, day).show();
        });
        tvSelectPayers.setOnClickListener(v -> showPayerSelector());

        // 點擊確認後計算分帳邏輯
        findViewById(R.id.btnConfirm).setOnClickListener(v -> calculateSplit());

        // 刪除此筆分帳紀錄
        findViewById(R.id.btnDelete).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("確認刪除")
                    .setMessage("確定要刪除此筆紀錄？")
                    .setPositiveButton("刪除", (dialog, which) -> {
                        // 這些變數在 onCreate 上方已經宣告過
                        // 所以這裡直接用，不要再重複加型別
                        db.collection("users")
                                .document(userId)
                                .collection("group")
                                .document(groupId)
                                .collection("records")
                                .document(recordId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "刪除成功", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "刪除失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }


    // 顯示使用者選擇圖片來源（相簿 or 拍照）
    private void showImageOptions() {
        new AlertDialog.Builder(this)
                .setTitle("選擇圖片來源")
                .setItems(new CharSequence[]{"拍照", "從相簿選取"}, (dialog, which) -> {
                    if (which == 0) {
                        // 拍照需要相機權限
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.CAMERA}, 1001);
                        } else {
                            takePhotoLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
                        }
                    } else {
                        Intent intent = new Intent(Intent.ACTION_PICK);
                        intent.setType("image/*");
                        pickImageLauncher.launch(intent);
                    }
                })
                .show();
    }

    // 執行分帳邏輯，組合結算結果文字並回傳至上一頁
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

        // 收集有勾選的分帳成員（nickname → email）
        List<String> selectedEmails = new ArrayList<>();
        for (int i = 0; i < memberSelectionContainer.getChildCount(); i++) {
            CheckBox cb = (CheckBox) memberSelectionContainer.getChildAt(i);
            if (cb.isChecked()) {
                String nickname = cb.getText().toString();
                for (Member member : members) {
                    if (nickname.equals(member.getNickname())) {
                        selectedEmails.add(member.getEmail());
                        break;
                    }
                }
            }
        }

        if (selectedEmails.isEmpty()) {
            Toast.makeText(this, "請選擇至少一位分帳對象", Toast.LENGTH_SHORT).show();
            return;
        }

        // 收集付款人金額（nickname → 金額）
        Map<String, Double> actualPayments = new HashMap<>();
        for (Member payer : chosenPayers) {
            String nickname = payer.getNickname();
            EditText input = payerInputs.get(nickname);
            String value = input.getText().toString().trim();
            double paid = value.isEmpty() ? 0 : Double.parseDouble(value);
            actualPayments.put(nickname, paid);
        }

        double perPerson = totalAmount / selectedEmails.size();
        String dateText = tvDate.getText().toString().trim();
        if (dateText.equals("請選擇日期")) {
            Toast.makeText(this, "請選擇消費日期", Toast.LENGTH_SHORT).show();
            return;
        }

        // 建立 email 對應的 balances
        Set<String> allInvolvedNicknames = new HashSet<>(actualPayments.keySet());
        for (String email : selectedEmails) {
            for (Member m : members) {
                if (email.equals(m.getEmail())) {
                    allInvolvedNicknames.add(m.getNickname());
                    break;
                }
            }
        }

        Map<String, Float> balancesToSend = new HashMap<>();
        StringBuilder result = new StringBuilder("消費日期：" + dateText + "\n");
        result.append("備註：「").append(note).append("」\n總金額 NT$").append(totalAmount).append("\n\n");

        for (String nickname : allInvolvedNicknames) {
            double paid = actualPayments.getOrDefault(nickname, 0.0);
            double balance = 0;
            String email = "";

            for (Member m : members) {
                if (m.getNickname().equals(nickname)) {
                    email = m.getEmail();
                    if (selectedEmails.contains(email)) {
                        balance = paid - perPerson;
                    } else {
                        balance = paid;
                    }
                    break;
                }
            }

            balancesToSend.put(email, (float) balance);
            result.append(nickname)
                    .append(" 已付 NT$").append(paid)
                    .append(" → ")
                    .append(balance >= 0 ? "收回" : "應付")
                    .append(" NT$").append(String.format("%.2f", Math.abs(balance)))
                    .append("\n");
        }

        // 彈出確認視窗
        new AlertDialog.Builder(this)
                .setTitle("分帳結果")
                .setMessage(result.toString())
                .setPositiveButton("確認", (dialog, which) -> {
                    String recordId = getIntent().getStringExtra("recordId");
                    SharedPreferences prefs = getSharedPreferences("login", MODE_PRIVATE);
                    String userId = prefs.getString("userid", "0");
                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    // 付款人資訊（轉為 email）
                    List<String> payerEmails = new ArrayList<>();
                    List<Double> payerAmounts = new ArrayList<>();
                    for (Member payer : chosenPayers) {
                        String nickname = payer.getNickname();
                        EditText input = payerInputs.get(nickname);
                        String value = input.getText().toString().trim();
                        double amount = value.isEmpty() ? 0.0 : Double.parseDouble(value);
                        payerEmails.add(payer.getEmail());
                        payerAmounts.add(amount);
                    }

                    Map<String, Object> updatedRecord = new HashMap<>();
                    updatedRecord.put("content", note);
                    updatedRecord.put("summary", result.toString());
                    updatedRecord.put("date", dateText);
                    updatedRecord.put("note", note);
                    updatedRecord.put("amount", totalAmount);
                    updatedRecord.put("payers", payerEmails);
                    updatedRecord.put("payerAmounts", payerAmounts);
                    updatedRecord.put("participants", selectedEmails);
                    updatedRecord.put("balances", balancesToSend);

                    db.collection("users")
                            .document(userId)
                            .collection("group")
                            .document(groupId)
                            .collection("records")
                            .document(recordId)
                            .update(updatedRecord)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "修改成功", Toast.LENGTH_SHORT).show();
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("summary", result.toString());
                                resultIntent.putExtra("record", String.format("%s - NT$%.0f %s", dateText, totalAmount, note));
                                setResult(RESULT_OK, resultIntent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "修改失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("取消", null)
                .show();
    }


    // 處理相機權限結果
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

    // 根據付款人動態產生輸入欄位
    private void updatePayerInputFields() {
        payerAmountContainer.removeAllViews();
        payerInputs.clear();

        if (chosenPayers.isEmpty()) {
            payerAmountContainer.setVisibility(View.GONE);
            tvSelectPayers.setText("請選擇付款人");
            return;
        }

        payerAmountContainer.setVisibility(View.VISIBLE);

        List<String> payerNames = new ArrayList<>();
        for (Member payer : chosenPayers) {
            payerNames.add(payer.getNickname());
        }
        tvSelectPayers.setText("付款人：" + String.join(", ", payerNames));

        for (Member payer : chosenPayers) {
            String nickname = payer.getNickname();

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 8, 0, 8);

            TextView tv = new TextView(this);
            tv.setText(nickname + "：");
            tv.setTextSize(16);
            tv.setWidth(120);

            EditText input = new EditText(this);
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setHint("付款金額");
            input.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            payerInputs.put(nickname, input);

            row.addView(tv);
            row.addView(input);
            payerAmountContainer.addView(row);
        }
    }

    private void restoreUI() {
        memberSelectionContainer.removeAllViews();
        chosenPayers.clear();
        payerInputs.clear();

        // 顯示 checkbox，還原參與者勾選
        for (Member member : members) {
            String nickname = member.getNickname();
            CheckBox cb = new CheckBox(this);
            cb.setText(nickname);
            cb.setChecked(originalParticipants != null && originalParticipants.contains(member.getEmail()));
            memberSelectionContainer.addView(cb);
        }

        // 還原付款人清單
        if (originalPayers != null) {
            for (String payerEmail : originalPayers) {
                for (Member m : members) {
                    if (m.getEmail().equals(payerEmail)) {
                        chosenPayers.add(m);
                        break;
                    }
                }
            }
            updatePayerInputFields(); // 建立輸入框

            // 填入付款金額
            for (int i = 0; i < originalPayers.size(); i++) {
                String email = originalPayers.get(i);
                Double amt = payerAmounts.get(i);
                for (Member m : members) {
                    if (m.getEmail().equals(email)) {
                        EditText et = payerInputs.get(m.getNickname());
                        if (et != null) {
                            et.setText(String.valueOf(amt));
                        }
                        break;
                    }
                }
            }
        }
    }
    private void showPayerSelector() {
        if (members.isEmpty()) {
            Toast.makeText(this, "尚未載入群組成員", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[members.size()];
        selectedPayers = new boolean[members.size()];
        for (int i = 0; i < members.size(); i++) {
            names[i] = members.get(i).getNickname();
        }

        new AlertDialog.Builder(this)
                .setTitle("選擇付款人")
                .setMultiChoiceItems(names, selectedPayers, (dialog, which, isChecked) -> {
                    Member selected = members.get(which);
                    if (isChecked) {
                        if (!chosenPayers.contains(selected)) chosenPayers.add(selected);
                    } else {
                        chosenPayers.remove(selected);
                    }
                })
                .setPositiveButton("確定", (dialog, which) -> updatePayerInputFields())
                .setNegativeButton("取消", null)
                .show();
    }

}
