package com.example.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GroupDetail extends AppCompatActivity {

    private ListView listView; // 顯示清算結果與每月紀錄
    private FloatingActionButton fabAdd; // 新增支出按鈕
    private BubbleView bubbleView; // 自定義的氣泡圓形分帳圖
    private TextView group_name; // 顯示群組名稱
    private ImageButton btnBack; // 返回鍵

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail);

        // 綁定元件
        listView = findViewById(R.id.listViewMembers);
        fabAdd = findViewById(R.id.fabAddExpense);
        bubbleView = findViewById(R.id.bubbleView);
        group_name = findViewById(R.id.tvGroupName);
        btnBack = findViewById(R.id.btnBack);

        // 顯示群組名稱（從上一頁傳進來）
        String groupName = getIntent().getStringExtra("groupName");
        if (groupName != null) {
            group_name.setText(groupName);
        }

        // 返回上一頁
        btnBack.setOnClickListener(v -> finish());

        // 模擬資料：每個成員的分帳金額，正數代表應收，負數代表應付
        List<String> names = Arrays.asList("小a", "陳啟瑋");
        List<Float> amounts = Arrays.asList(-65f, 65f);
        bubbleView.setData(names, amounts);

        // 模擬支出紀錄
        List<String[]> records = new ArrayList<>();
        records.add(new String[]{"2025-05-26", "小a 買飲料 $40"});
        records.add(new String[]{"2025-05-20", "陳啟瑋 買便當 $65"});
        records.add(new String[]{"2025-04-18", "小a 買車票 $120"});

        // 排序支出資料（新→舊）
        Collections.sort(records, (a, b) -> b[0].compareTo(a[0]));

        // 按月份分組
        Map<String, List<String>> monthMap = new LinkedHashMap<>();
        for (String[] record : records) {
            String date = record[0];
            String content = record[1];
            String month = date.substring(0, 7); // yyyy-MM
            monthMap.computeIfAbsent(month, k -> new ArrayList<>()).add(date + " - " + content);
        }

        // 合併顯示資料（清算 + 每月紀錄）
        List<String> displayItems = new ArrayList<>();
        displayItems.add("小a 應付 $65 給 陳啟瑋");

        for (Map.Entry<String, List<String>> entry : monthMap.entrySet()) {
            displayItems.add("📅 " + entry.getKey());
            displayItems.addAll(entry.getValue());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, displayItems);
        listView.setAdapter(adapter);

        // 新增支出按鈕事件
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(GroupDetail.this, GroupCharge2.class);
            startActivityForResult(intent, 100);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 預留給新增支出後更新資料
    }
}