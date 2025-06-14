package com.example.afinal;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public class GroupCharge2 extends AppCompatActivity {

    // 主要 UI 元件
    private ImageView imgPreview; // 顯示選取或拍攝的圖片
    private EditText etAmount, etNote; // 使用者輸入金額與備註欄位
    private LinearLayout payerAmountContainer; // 動態產生每位付款人輸入欄位的容器
    private FlexboxLayout memberSelectionContainer; // 勾選每位成員是否參與分帳的區塊
    private String base64Image;
    // 群組成員清單（實際使用時應改為從後端/Intent 取得）
    private List<Member> members = new ArrayList<>();

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
        setContentView(R.layout.activity_group_charge2);

        // 綁定 UI 元件
        imgPreview = findViewById(R.id.imgPreview);
        etAmount = findViewById(R.id.etAmount);
        etNote = findViewById(R.id.etNote);
        payerAmountContainer = findViewById(R.id.payerAmountContainer);
        memberSelectionContainer = findViewById(R.id.memberSelectionContainer);
        tvDate = findViewById(R.id.tvDate);
        tvSelectPayers = findViewById(R.id.tvSelectPayers);

        // 返回鍵
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 點圖片 → 選擇相片或拍照
        imgPreview.setOnClickListener(v -> showImageOptions());

        // 選擇日期 → 使用 DatePickerDialog
        tvDate.setOnClickListener(v -> {
            int year = selectedDate.get(Calendar.YEAR);
            int month = selectedDate.get(Calendar.MONTH);
            int day = selectedDate.get(Calendar.DAY_OF_MONTH);

            new DatePickerDialog(this, (view, y, m, d) -> {
                selectedDate.set(y, m, d);
                tvDate.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d));
            }, year, month, day).show();
        });

        // 使用者未設置暱稱時，預設“我”
        SharedPreferences settingPrefs = getSharedPreferences("setting_prefs", MODE_PRIVATE);
        myNickname = settingPrefs.getString("nickname", "我");

        // 判斷是否是當前使用者，用 email 比對，改為暱稱
        SharedPreferences loginPrefs = getSharedPreferences("login", MODE_PRIVATE);
        myEmail = loginPrefs.getString("userid", ""); // 這是目前登入者的 email

        // Firebase 成功載入完後會呼叫：
        setupMembers();

        // 從 FireBase 抓取群組人員資料
        Intent intent = getIntent();
        String groupName = intent.getStringExtra("groupName");

        SharedPreferences prefs = getSharedPreferences("login", MODE_PRIVATE);
        String userId = prefs.getString("userid", "0");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(userId)
                .collection("group")
                .document(groupName)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> memberEmails = (List<String>) documentSnapshot.get("members");
                        if (memberEmails != null && !memberEmails.isEmpty()) {
                            members.clear();
                            for (String email : memberEmails) {
                                db.collection("users")
                                        .document(email)
                                        .get()
                                        .addOnSuccessListener(userSnap -> {
                                            String nickname = userSnap.getString("nickname");
                                            if (nickname == null || nickname.isEmpty()) nickname = email;
                                            members.add(new Member(email, nickname));

                                            if (members.size() == memberEmails.size()) {
                                                setupMembers(); // 所有 nickname 都取得後再顯示 UI
                                            }
                                        });
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "載入群組成員失敗：" + e.getMessage()));


        // 預設不顯示付款金額區塊，除非有選付款人
        payerAmountContainer.setVisibility(View.GONE);

        // 點選「選擇付款人」 → 顯示多選清單
        tvSelectPayers.setOnClickListener(v -> {
            if (members.isEmpty()) {
                Toast.makeText(this, "尚未載入群組成員", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] names = new String[members.size()];
            for (int i = 0; i < members.size(); i++) {
                Member m = members.get(i);
                if (m.getEmail().equals(myEmail)) {
                    names[i] = myNickname;
                } else {
                    names[i] = m.getNickname();
                }
            }

            selectedPayers = new boolean[members.size()];

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

        });

        // 點擊確認後計算分帳邏輯
        findViewById(R.id.btnConfirm).setOnClickListener(v -> calculateSplit());
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

        // 檢查欄位是否空白
        if (note.isEmpty() || totalStr.isEmpty()) {
            Toast.makeText(this, "請輸入金額與備註", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> selectedMembers = new ArrayList<>();
        for (int i = 0; i < memberSelectionContainer.getChildCount(); i++) {
            CheckBox cb = (CheckBox) memberSelectionContainer.getChildAt(i);
            if (cb.isChecked()) {
                String nickname = cb.getText().toString();
                String email = nicknameToEmail.get(nickname);
                if (email != null) selectedMembers.add(email);
            }
        }

        double totalAmount;
        try {
            totalAmount = Double.parseDouble(totalStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "金額格式錯誤", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedMembers.isEmpty()) {
            Toast.makeText(this, "請選擇至少一位分帳對象", Toast.LENGTH_SHORT).show();
            return;
        }

        // 收集每位付款人實際輸入的付款金額
        Map<String, Double> actualPayments = new HashMap<>();
        for (Member payer : chosenPayers) {
            String email = payer.getEmail();
            EditText input = payerInputs.get(email);
            String value = input.getText().toString().trim();
            double paid = value.isEmpty() ? 0 : Double.parseDouble(value);
            actualPayments.put(email, paid);
        }

        double perPerson = totalAmount / selectedMembers.size();

        String dateText = tvDate.getText().toString().trim();
        if (dateText.equals("請選擇日期")) {
            Toast.makeText(this, "請選擇消費日期", Toast.LENGTH_SHORT).show();
            return;
        }

        // 結果彙整為文字形式（也可改為 JSON 結構傳給後端）
        StringBuilder result = new StringBuilder("消費日期：" + dateText + "\n");
        result.append("備註：「").append(note).append("」\n總金額 NT$").append(totalAmount).append("\n\n");

        // 付款人與參與者合併為所有相關人員
        Set<String> allInvolved = new LinkedHashSet<>();
        allInvolved.addAll(selectedMembers);
        allInvolved.addAll(actualPayments.keySet());

        for (String email : allInvolved) {
            double paid = actualPayments.getOrDefault(email, 0.0);
            boolean isParticipant = selectedMembers.contains(email);double balance;
            if (isParticipant) {
                balance = paid - perPerson; // 有參與分帳者
            } else if (paid > 0) {
                balance = paid; // 只付款但沒參與 → 應收全額
            } else {
                continue; // 沒有付款也沒參與 → 不顯示
            }

            String nameToShow;
            if (email.equals(myEmail)) {
                nameToShow = myNickname;
            } else {
                nameToShow = emailToNickname.getOrDefault(email, email);
            }

            result.append(nameToShow)
                    .append(" 已付 NT$").append(paid)
                    .append(" → ")
                    .append(balance >= 0 ? "收回" : "應付")
                    .append(" NT$").append(String.format("%.2f", Math.abs(balance)))
                    .append("\n");
        }


        // 彈出結果對話框，並回傳資料到 GroupDetail（record 與 summary）
        new AlertDialog.Builder(this)
                .setTitle("分帳結果")
                .setMessage(result.toString())
                .setPositiveButton("確認", (dialog, which) -> {
                    String record = String.format("%s - NT$%.0f %s", dateText, totalAmount, note);
                    Map<String, Float> balancesToSend = new HashMap<>();

                    for (String email : allInvolved) {
                        double paid = actualPayments.getOrDefault(email, 0.0);
                        boolean isParticipant = selectedMembers.contains(email);
                        double balance;
                        if (isParticipant) {
                            balance = paid - perPerson;
                        } else if (paid > 0) {
                            balance = paid;
                        } else {
                            continue;
                        }
                        balancesToSend.put(email, (float) balance);
                    }

                    // Firebase 寫入
                    SharedPreferences prefs = getSharedPreferences("login", MODE_PRIVATE);
                    String userId = prefs.getString("userid", "0");
                    String groupName = getIntent().getStringExtra("groupName");

                    List<String> payerEmails = new ArrayList<>();
                    List<Double> payerAmounts = new ArrayList<>();
                    for (Member payer : chosenPayers) {
                        String email = payer.getEmail();
                        EditText input = payerInputs.get(email);
                        String value = input.getText().toString().trim();
                        double amount = value.isEmpty() ? 0.0 : Double.parseDouble(value);
                        payerEmails.add(email);
                        payerAmounts.add(amount);
                    }

                    Map<String, Object> recordData = new HashMap<>();
                    recordData.put("content", note);
                    recordData.put("summary", result.toString());
                    recordData.put("date", dateText);
                    recordData.put("note", note);
                    recordData.put("amount", totalAmount);
                    recordData.put("payers", payerEmails);
                    recordData.put("payerAmounts", payerAmounts);
                    recordData.put("participants", selectedMembers);
                    recordData.put("balances", balancesToSend);
                    recordData.put("timestamp", new Date());
                    Drawable drawable = imgPreview.getDrawable();
                    if (drawable == null) {
                        // 沒有選圖片，設為 "123"
                        base64Image = "123";
                    } else {
                        // 將圖片轉為 Base64
                        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 10, stream);
                        base64Image = android.util.Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT);
                    }
                    recordData.put("image", base64Image);
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("users")
                            .document(userId)
                            .collection("group")
                            .document(groupName)
                            .collection("records")
                            .add(recordData)
                            .addOnSuccessListener(docRef -> {
                                Toast.makeText(this, "新增成功", Toast.LENGTH_SHORT).show();

                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("summary", result.toString());
                                resultIntent.putExtra("record", record);
                                resultIntent.putExtra("balances", new com.google.gson.Gson().toJson(balancesToSend));
                                setResult(RESULT_OK, resultIntent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "儲存失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e("Firestore", "新增紀錄失敗", e);
                            });
                })

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
            String displayName = payer.getEmail().equals(myEmail) ? myNickname : payer.getNickname();
            payerNames.add(displayName);
        }
        tvSelectPayers.setText("付款人：" + String.join(", ", payerNames));

        for (Member payer : chosenPayers) {
            String email = payer.getEmail();
            String displayName = email.equals(myEmail) ? myNickname : payer.getNickname();

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 8, 0, 8);

            TextView tv = new TextView(this);
            tv.setText(displayName + "：");
            tv.setTextSize(16);
            tv.setWidth(120);

            EditText input = new EditText(this);
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setHint("付款金額");
            input.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            payerInputs.put(email, input); // 用 email 當 key 確保唯一
            row.addView(tv);
            row.addView(input);
            payerAmountContainer.addView(row);
        }
    }

    // 動態載入人員
    private void setupMembers() {
        memberSelectionContainer.removeAllViews();
        payerInputs.clear();
        selectedPayers = new boolean[members.size()];
        chosenPayers.clear();
        nicknameToEmail.clear();
        emailToNickname.clear();

        for (int i = 0; i < members.size(); i++) {
            Member m = members.get(i);

            String displayName = m.getEmail().equals(myEmail) ? myNickname : m.getNickname();

            nicknameToEmail.put(displayName, m.getEmail());  // 用顯示名稱當 key 才對！
            emailToNickname.put(m.getEmail(), displayName);  // 也一併更新為統一使用 displayName

            CheckBox cb = new CheckBox(this);
            cb.setText(displayName);
            cb.setChecked(true);
            memberSelectionContainer.addView(cb);
        }
    }
}
