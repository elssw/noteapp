package com.example.afinal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class GroupDetail extends AppCompatActivity {

    // UI 元件
    private ListView listView; // 顯示收支結算與紀錄的清單
    private FloatingActionButton fabAdd; // 新增記帳按鈕
    private BubbleView bubbleView; // 自定義的泡泡圖表，用來呈現每個成員的收支情況
    private TextView group_name; // 群組名稱顯示欄位
    private ImageButton btnBack; // 返回上一頁的按鈕
    private String groupName; // 從上一頁傳來的群組名稱
    private String userId; // 成員
    private ImageView imageView;
    private FirebaseFirestore db; // 資料庫
    private Map<String, String> groupIdMap;
    // 顯示用資料
    private List<String> displayItems = new ArrayList<>(); // ListView 要顯示的所有項目（含結算摘要與記錄）
    private ArrayAdapter<String> adapter; // 對應的 Adapter 物件

    // 成員餘額（正數表示應收金額，負數表示應付款項）
    private Map<String, Float> balances = new LinkedHashMap<>();

    // 每筆紀錄，格式為 [日期, 顯示內容]，用來顯示在畫面上
    private List<String[]> records = new ArrayList<>();

    // email 轉暱稱
    private Map<String, String> emailToNickname = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail);

        // 綁定 UI 元件
        listView = findViewById(R.id.listViewMembers);
        fabAdd = findViewById(R.id.fabAddExpense);
        bubbleView = findViewById(R.id.bubbleView);
        group_name = findViewById(R.id.tvGroupName);
        btnBack = findViewById(R.id.btnBack);
        imageView = findViewById(R.id.imageView);
        imageView = findViewById(R.id.imageView);
        SharedPreferences pref = getSharedPreferences("gIdnameMap", MODE_PRIVATE);
        String jsonString = pref.getString("groupIdMap", null);

        groupIdMap = new HashMap<>();
        if (jsonString != null) {
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = jsonObject.getString(key);
                    groupIdMap.put(key, value);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // 顯示從上一頁傳來的群組名稱
        groupName = getIntent().getStringExtra("groupID");
        String groupName2 = groupIdMap.get(groupName);
        for (String k : groupIdMap.keySet()) {
            Log.d("mine", "key=" + k + ", value=" + groupIdMap.get(k));
        }
        Log.d("Debug", "groupName2原始值=[" + groupName2 + "]");
        Log.d("Debug", "groupName原始值=[" + groupName + "]");
//        String groupName= groupIdMap.get(oldid);
        if (groupName != null) group_name.setText(groupName2);

        // 點擊返回按鈕：結束目前頁面，回上一頁
        btnBack.setOnClickListener(v -> finish());

        // 從 Firestore 載入歷史紀錄
        db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = getSharedPreferences("login", MODE_PRIVATE);
        userId = prefs.getString("userid", "0"); // 儲存為類別成員變數

        startRealtimeListener();

        db.collection("users")
                .document(userId)
                .collection("group")
                .document(groupName)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<String> memberEmails = (List<String>) documentSnapshot.get("members");
                    if (memberEmails != null && !memberEmails.isEmpty()) {
                        int total = memberEmails.size();
                        int[] loadedCount = {0};

                        for (String email : memberEmails) {
                            db.collection("users")
                                    .document(email)
                                    .get()
                                    .addOnSuccessListener(userDoc -> {
                                        String nickname = userDoc.getString("nickname");

                                        // 加入 fallback 處理
                                        if (nickname != null && !nickname.trim().isEmpty()) {
                                            emailToNickname.put(email, nickname);
                                        } else {
                                            emailToNickname.put(email, email); // fallback 顯示 email
                                        }

                                        loadedCount[0]++;
                                        if (loadedCount[0] == total) {
                                            updateDisplayItems();
                                            adapter.notifyDataSetChanged();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        // 失敗也 fallback，避免 map 沒有 key
                                        emailToNickname.put(email, email);
                                        loadedCount[0]++;
                                        if (loadedCount[0] == total) {
                                            updateDisplayItems();
                                            adapter.notifyDataSetChanged();
                                        }
                                    });
                        }
                    } else {
                        updateDisplayItems();
                        adapter.notifyDataSetChanged();
                    }
                });

        // 初始化畫面：更新 ListView 顯示內容 (讀取 FireBase 時註解)
        updateDisplayItems();

        // 建立 Adapter 並連結至 ListView
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayItems);
        listView.setAdapter(adapter);

        // 刪除單筆分帳紀錄
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String item = displayItems.get(position);

            // 排除非分帳紀錄的項目
            if (item.startsWith("📅") || item.startsWith("目前無需清算") || item.startsWith("A 應付") || item.trim().isEmpty()) {
                return; // 不處理月份標題、摘要
            }

            if (item.contains(" - ")) {
                String[] parts = item.split(" - ", 2);
                if (parts.length == 2) {
                    String date = parts[0];
                    String content = parts[1];

                    // 從 Firebase 查出該筆 recordId
                    db.collection("users")
                            .document(userId)
                            .collection("group")
                            .document(groupName)
                            .collection("records")
                            .whereEqualTo("date", date)
                            .whereEqualTo("content", content)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                for (QueryDocumentSnapshot doc : querySnapshot) {
                                    String recordId = doc.getId();
//                                    String groupName2= groupIdMap.get(groupName);

                                    Log.d("mine2", "key=" + recordId + ", value=" + content);

                                    Intent intent = new Intent(GroupDetail.this, GroupChargeEdit.class);
                                    intent.putExtra("groupId", groupName); // ← 加上這行
                                    intent.putExtra("groupName", groupName2);
                                    intent.putExtra("recordId", recordId);
                                    intent.putExtra("date", date);
                                    intent.putExtra("content", content);
                                    intent.putExtra("summary", doc.getString("summary"));
                                    startActivityForResult(intent, 101);
                                    break;
                                }
                            });
                }
            }
        });


        // 點擊「新增記帳」按鈕，開啟記帳畫面
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(GroupDetail.this, GroupCharge2.class);
            intent.putExtra("groupName", groupName);
            startActivityForResult(intent, 100);
        });
        imageView.setOnClickListener(v -> {
            Intent intent = new Intent(GroupDetail.this, SingleGroupManageActivity.class);
            intent.putExtra("groupId", groupName); // ← 加上這行
            intent.putExtra("groupName", groupName2);
            startActivity(intent);
//            startActivityForResult(intent, 102);
        });
    }

    // 處理從 GroupCharge2 回傳的結果，並更新收支邏輯與畫面
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 處理 GroupCharge2 回傳資料
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            String newRecord = data.getStringExtra("record");  // 格式如：2025-06-05 - A NT$450
            String summary = data.getStringExtra("summary");
            String balancesJson = data.getStringExtra("balances");


        }

        // 處理從 GroupChargeEdit 返回的更新
        if (requestCode == 101 && resultCode == RESULT_OK) {
            db.collection("users")
                    .document(userId)
                    .collection("group")
                    .document(groupName)
                    .collection("records")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        records.clear();
                        balances.clear();

                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            String date = doc.getString("date");
                            String content = doc.getString("content");
                            if (date == null || content == null) continue;
                            records.add(new String[]{date, content});

                            Map<String, Object> bal = (Map<String, Object>) doc.get("balances");
                            if (bal != null) {
                                for (Map.Entry<String, Object> entry : bal.entrySet()) {
                                    String name = entry.getKey();
                                    Object val = entry.getValue();
                                    if (val instanceof Number) {
                                        float amount = ((Number) val).floatValue();
                                        balances.put(name, balances.getOrDefault(name, 0f) + amount);
                                    }
                                }
                            }
                        }

                        updateDisplayItems();
                        adapter.notifyDataSetChanged();
                    });
        }
    }


    // 根據 balances 和紀錄更新畫面顯示
    private void updateDisplayItems() {
        displayItems.clear();
        updateBubbleView(); // 更新泡泡圖顯示

        // 新增結算區塊
        displayItems.add(generateSummaryText());

        // 依照年月整理紀錄
        Collections.sort(records, (a, b) -> b[0].compareTo(a[0]));
        Map<String, List<String>> monthMap = new LinkedHashMap<>();

        for (String[] record : records) {
            String date = record[0];
            String content = record[1];

            // 替換 email 為 nickname（只在畫面顯示用）
            for (Map.Entry<String, String> entry : emailToNickname.entrySet()) {
                content = content.replace(entry.getKey(), entry.getValue());
            }

            String month = date.substring(0, 7); // 取 yyyy-MM 作為 key
            monthMap.computeIfAbsent(month, k -> new ArrayList<>()).add(date + " - " + content);
        }

        // 加入 ListView 顯示內容
        for (Map.Entry<String, List<String>> entry : monthMap.entrySet()) {
            displayItems.add("📅 " + entry.getKey()); // 月份標題
            displayItems.addAll(entry.getValue());   // 每筆記錄
        }
    }

    // 更新泡泡圖資料：名稱與餘額
    private void updateBubbleView() {
        List<String> names = new ArrayList<>();
        List<Float> amounts = new ArrayList<>();

        for (Map.Entry<String, Float> entry : balances.entrySet()) {
            String email = entry.getKey();
            Float amount = entry.getValue();

            // 轉換為暱稱（若找不到就顯示 email）
            String nickname = emailToNickname.getOrDefault(email, email);

            Log.d("bubble-log", nickname + " → " + amount); // debug 用
            names.add(nickname);
            amounts.add(amount);
        }

        bubbleView.setData(names, amounts); // 傳入自定義 view 更新泡泡
    }

    // 產生目前應收應付的文字說明，如「A 應付 $100 給 B」
    private String generateSummaryText() {
        // 建立新的 map，不會影響原本的 balances
        Map<String, Float> tempBalances = new LinkedHashMap<>(balances);
        StringBuilder summary = new StringBuilder();

        List<Map.Entry<String, Float>> debtors = new ArrayList<>();
        List<Map.Entry<String, Float>> creditors = new ArrayList<>();

        for (Map.Entry<String, Float> entry : tempBalances.entrySet()) {
            float value = entry.getValue();
            if (value < -0.01f) {
                debtors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), value));
            } else if (value > 0.01f) {
                creditors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), value));
            }
        }

        for (Map.Entry<String, Float> debtor : debtors) {
            String debtorName = emailToNickname.getOrDefault(debtor.getKey(), debtor.getKey());
            float amountToPay = -debtor.getValue();

            for (int i = 0; i < creditors.size(); i++) {
                Map.Entry<String, Float> creditor = creditors.get(i);
                String creditorName = emailToNickname.getOrDefault(creditor.getKey(), creditor.getKey());
                float creditorAmount = creditor.getValue();

                if (creditorAmount <= 0) continue;

                float transfer = Math.min(amountToPay, creditorAmount);

                summary.append(debtorName).append(" 應付 $")
                        .append(String.format("%.0f", transfer))
                        .append(" 給 ").append(creditorName).append("\n");

                amountToPay -= transfer;
                creditors.set(i, new AbstractMap.SimpleEntry<>(creditor.getKey(), creditorAmount - transfer));
                if (amountToPay <= 0) break;
            }
        }

        return summary.length() > 0 ? summary.toString().trim() : "目前無需清算";
    }


    // 刪除單筆分帳紀錄
    private void deleteRecord(String target) {
        String[] parts = target.split(" - ");
        if (parts.length < 2) return;

        String date = parts[0];
        String content = parts[1];

        // 刪除 Firestore 中對應紀錄
        db.collection("users")
                .document(userId)
                .collection("group")
                .document(groupName)
                .collection("records")
                .whereEqualTo("date", date)
                .whereEqualTo("content", content)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        doc.getReference().delete();
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "刪除失敗：" + e.getMessage()));

        // 預先從畫面暫時移除這筆紀錄，避免使用者感覺沒刪成功
        Iterator<String[]> iterator = records.iterator();
        while (iterator.hasNext()) {
            String[] record = iterator.next();
            if (record[0].equals(date) && record[1].equals(content)) {
                iterator.remove();
                break;
            }
        }
        updateDisplayItems();
        adapter.notifyDataSetChanged();
    }

    // 即時監聽
    private void startRealtimeListener() {
        db.collection("users")
                .document(userId)
                .collection("group")
                .document(groupName)
                .collection("records")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null || querySnapshot == null) {
                        Log.w("Firestore", "即時監聽失敗", e);
                        return;
                    }

                    records.clear();
                    balances.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String date = doc.getString("date");
                        String content = doc.getString("content");
                        if (date == null || content == null) continue;
                        records.add(new String[]{date, content});

                        Map<String, Object> bal = (Map<String, Object>) doc.get("balances");
                        if (bal != null) {
                            for (Map.Entry<String, Object> entry : bal.entrySet()) {
                                String name = entry.getKey();
                                Object val = entry.getValue();
                                if (val instanceof Number) {
                                    float amount = ((Number) val).floatValue();
                                    balances.put(name, balances.getOrDefault(name, 0f) + amount);
                                }
                            }
                        }
                    }

                    updateDisplayItems();
                    adapter.notifyDataSetChanged();
                });
        updateBubbleView();
    }
}