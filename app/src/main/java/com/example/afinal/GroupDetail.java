package com.example.afinal;

import android.content.Intent;
import android.os.Bundle;
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

    // 成員收支資料
    private Map<String, Float> balances = new LinkedHashMap<>();
    private Set<String> allMembers = new LinkedHashSet<>(Arrays.asList("我", "小a", "陳啟瑋", "A", "B", "C", "D", "E"));

    // 歷史紀錄
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
        if (groupName != null) {
            group_name.setText(groupName);
        }

        btnBack.setOnClickListener(v -> finish());

        // 初始化模擬資料
        records.add(new String[]{"2025-05-26", "小a 買飲料 $40"});
        records.add(new String[]{"2025-05-20", "陳啟瑋 買便當 $65"});
        records.add(new String[]{"2025-04-18", "小a 買車票 $120"});

        balances.put("小a", -65f);
        balances.put("陳啟瑋", 65f);

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
                    String payerName = null;
                    float total = 0;

                    for (String line : summaryLines) {
                        if (line.startsWith("總金額")) {
                            String[] temp = line.split("NT\\$");
                            if (temp.length == 2) {
                                total = Float.parseFloat(temp[1].trim());
                            }
                        }
                        if (line.contains("已付 NT$")) {
                            String[] tokens = line.split(" ");
                            payerName = tokens[0];  // e.g. A 已付 NT$72
                        }
                    }

                    if (payerName != null && total > 0) {
                        List<String> splitMembers = new ArrayList<>();
                        for (String line : summaryLines) {
                            if (line.contains("應付 NT$")) {
                                splitMembers.add(line.split(" ")[0]);
                            }
                        }

                        float perPerson = total / splitMembers.size();

                        // 更新收支
                        balances.put(payerName, balances.getOrDefault(payerName, 0f) + total);
                        updateBubbleView();
                        for (String member : splitMembers) {
                            balances.put(member, balances.getOrDefault(member, 0f) - perPerson);
                        }

                        // 新增紀錄，避免重複
                        boolean duplicate = false;
                        for (String[] record : records) {
                            if (record[0].equals(date) && record[1].equals(content)) {
                                duplicate = true;
                                break;
                            }
                        }
                        if (!duplicate) {
                            records.add(new String[]{date, content});
                        }

                        updateDisplayItems();
                        adapter.notifyDataSetChanged();
                        listView.post(() -> listView.setSelection(displayItems.size() - 1));
                    }
                }
            }
        }
    }

    private void updateDisplayItems() {
        displayItems.clear();
        updateBubbleView();

        // 第一區：計算誰應付誰
        displayItems.add(generateSummaryText());

        // 第二區 + 第三區：紀錄資料依月份分類
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
            amounts.add(balances.get(name));
        }
        bubbleView.setData(names, amounts);
    }

    private String generateSummaryText() {
        Map<String, Float> tempBalances = new LinkedHashMap<>(balances);
        StringBuilder summary = new StringBuilder();

        for (Map.Entry<String, Float> payer : tempBalances.entrySet()) {
            if (payer.getValue() < 0) {
                for (Map.Entry<String, Float> receiver : tempBalances.entrySet()) {
                    if (receiver.getValue() > 0) {
                        float transfer = Math.min(-payer.getValue(), receiver.getValue());
                        if (transfer > 0.01f) {
                            summary.append(payer.getKey()).append(" 應付 $")
                                    .append(String.format("%.0f", transfer))
                                    .append(" 給 ").append(receiver.getKey()).append("\n");
                            tempBalances.put(payer.getKey(), payer.getValue() + transfer);
                            tempBalances.put(receiver.getKey(), receiver.getValue() - transfer);
                        }
                    }
                }
            }
        }

        return summary.length() > 0 ? summary.toString().trim() : "目前無需清算";
    }
}