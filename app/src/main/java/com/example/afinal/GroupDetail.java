package com.example.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.*;

public class GroupDetail extends AppCompatActivity {

    private ListView listView;
    private FloatingActionButton fabAdd;
    private BubbleView bubbleView;
    private TextView group_name;
    private ImageButton btnBack;

    private List<String> displayItems = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private Map<String, Float> balances = new LinkedHashMap<>();
    private List<String[]> records = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail);

        listView = findViewById(R.id.listViewMembers);
        fabAdd = findViewById(R.id.fabAddExpense);
        bubbleView = findViewById(R.id.bubbleView);
        group_name = findViewById(R.id.tvGroupName);
        btnBack = findViewById(R.id.btnBack);

        String groupName = getIntent().getStringExtra("groupName");
        if (groupName != null) group_name.setText(groupName);

        btnBack.setOnClickListener(v -> finish());

        updateDisplayItems();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayItems);
        listView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(GroupDetail.this, GroupCharge2.class);
            startActivityForResult(intent, 100);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            String newRecord = data.getStringExtra("record");
            String summary = data.getStringExtra("summary");

            if (newRecord != null && !newRecord.isEmpty()) {
                String[] parts = newRecord.split(" - ");
                if (parts.length >= 2) {
                    String date = parts[0];
                    String content = parts[1];

                    String[] summaryLines = summary.split("\n");
                    float total = 0;
                    Set<String> involved = new HashSet<>();
                    List<String> splitMembers = new ArrayList<>();
                    Map<String, Float> payments = new HashMap<>();

                    // 解析摘要資訊
                    for (String line : summaryLines) {
                        if (line.startsWith("總金額")) {
                            String[] temp = line.split("NT\\$");
                            if (temp.length == 2) {
                                total = Float.parseFloat(temp[1].trim());
                            }
                        }

                        if (line.contains("已付 NT$") && line.contains("→")) {
                            String[] tokens = line.split(" ");
                            String name = tokens[0];
                            String paidStr = tokens[2].replace("NT$", "");
                            try {
                                float paid = Float.parseFloat(paidStr);
                                payments.put(name, paid);
                                involved.add(name);

                                if (line.contains("→ 應付")) {
                                    splitMembers.add(name); // 明確應付者
                                } else if (line.contains("→ 收回")) {
                                    int index = line.indexOf("→ 收回 NT$");
                                    if (index != -1) {
                                        String receiveStr = line.substring(index + "→ 收回 NT$".length()).trim();
                                        try {
                                            float receive = Float.parseFloat(receiveStr);
                                            if (receive < paid) {
                                                splitMembers.add(name); // 有分帳但有實際付款 → 需扣平均
                                            }
                                        } catch (NumberFormatException e) {
                                            Log.e("parse-error", "收回金額轉換失敗：" + receiveStr);
                                        }
                                    }
                                }

                            } catch (NumberFormatException e) {
                                Log.e("parse-error", "金額轉換失敗：" + paidStr);
                            }
                        }
                    }

                    // 將參與分帳者平均扣除金額
                    float perPerson = total / splitMembers.size();
                    for (String member : splitMembers) {
                        balances.put(member, balances.getOrDefault(member, 0f) - perPerson);
                    }

                    // 將付款金額加入 balances
                    for (Map.Entry<String, Float> entry : payments.entrySet()) {
                        String name = entry.getKey();
                        float paid = entry.getValue();
                        balances.put(name, balances.getOrDefault(name, 0f) + paid);
                    }

                    // 確保所有參與者在 balances 裡都有值
                    for (String name : involved) {
                        balances.putIfAbsent(name, balances.getOrDefault(name, 0f));
                    }

                    // 避免新增重複紀錄
                    boolean duplicate = false;
                    for (String[] record : records) {
                        if (record[0].equals(date) && record[1].equals(content)) {
                            duplicate = true;
                            break;
                        }
                    }
                    if (!duplicate) records.add(new String[]{date, content});

                    // 更新 UI 顯示
                    updateDisplayItems();
                    adapter.notifyDataSetChanged();
                    listView.post(() -> listView.setSelection(displayItems.size() - 1));
                }
            }
        }
    }

    private void updateDisplayItems() {
        displayItems.clear();
        updateBubbleView();

        displayItems.add(generateSummaryText());

        Collections.sort(records, (a, b) -> b[0].compareTo(a[0]));
        Map<String, List<String>> monthMap = new LinkedHashMap<>();

        for (String[] record : records) {
            String date = record[0];
            String content = record[1];
            String month = date.substring(0, 7);
            monthMap.computeIfAbsent(month, k -> new ArrayList<>()).add(date + " - " + content);
        }

        for (Map.Entry<String, List<String>> entry : monthMap.entrySet()) {
            displayItems.add("📅 " + entry.getKey());
            displayItems.addAll(entry.getValue());
        }
    }

    private void updateBubbleView() {
        List<String> names = new ArrayList<>(balances.keySet());
        List<Float> amounts = new ArrayList<>();
        for (String name : names) {
            Float value = balances.getOrDefault(name, 0f);
            Log.d("bubble-log", name + " → " + value);
            amounts.add(value);
        }
        bubbleView.setData(names, amounts);
    }

    private String generateSummaryText() {
        Map<String, Float> tempBalances = new LinkedHashMap<>(balances);
        StringBuilder summary = new StringBuilder();

        // 收集應付與應收成員
        List<Map.Entry<String, Float>> debtors = new ArrayList<>();
        List<Map.Entry<String, Float>> creditors = new ArrayList<>();
        for (Map.Entry<String, Float> entry : tempBalances.entrySet()) {
            float value = entry.getValue();
            if (value < -0.01f) debtors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), value));
            else if (value > 0.01f) creditors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), value));
        }

        // 清算流程：每個應付者優先還錢給最大應收者
        for (Map.Entry<String, Float> debtor : debtors) {
            String debtorName = debtor.getKey();
            float amountToPay = -debtor.getValue(); // 轉為正值

            for (Map.Entry<String, Float> creditor : creditors) {
                if (amountToPay <= 0) break;

                String creditorName = creditor.getKey();
                float creditorAmount = creditor.getValue();

                if (creditorAmount <= 0) continue;

                float transfer = Math.min(amountToPay, creditorAmount);

                summary.append(debtorName).append(" 應付 $")
                        .append(String.format("%.0f", transfer))
                        .append(" 給 ").append(creditorName).append("\n");

                amountToPay -= transfer;
                creditor.setValue(creditorAmount - transfer);
            }
        }

        return summary.length() > 0 ? summary.toString().trim() : "目前無需清算";
    }
}