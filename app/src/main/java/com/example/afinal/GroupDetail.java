package com.example.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail);

        // 綁定介面元件
        listView = findViewById(R.id.listViewMembers);
        fabAdd = findViewById(R.id.fabAddExpense);
        bubbleView = findViewById(R.id.bubbleView);

        // 模擬資料：每個成員的分帳金額，正數代表應收，負數代表應付
        List<String> names = Arrays.asList("小a", "陳啟瑋");
        List<Float> amounts = Arrays.asList(-65f, 65f); // 小a 應付，陳啟瑋 應收
        bubbleView.setData(names, amounts); // 設定給 BubbleView 顯示為圓形氣泡

        // 模擬支出紀錄，每筆含日期與描述
        List<String[]> records = new ArrayList<>();
        records.add(new String[]{"2025-05-26", "小a 買飲料 $40"});
        records.add(new String[]{"2025-05-20", "陳啟瑋 買便當 $65"});
        records.add(new String[]{"2025-04-18", "小a 買車票 $120"});

        // 將支出紀錄依照日期由新到舊排序
        Collections.sort(records, (a, b) -> b[0].compareTo(a[0]));

        // 將支出紀錄依月份（yyyy-MM）分組，方便 ListView 顯示
        Map<String, List<String>> monthMap = new LinkedHashMap<>();
        for (String[] record : records) {
            String date = record[0];
            String content = record[1];
            String month = date.substring(0, 7); // 取前 7 字元作為月份 key
            monthMap.computeIfAbsent(month, k -> new ArrayList<>()).add(date + " - " + content);
        }

        // 組合 ListView 顯示的文字內容（清算資訊 + 每月支出）
        List<String> displayItems = new ArrayList<>();
        displayItems.add("小a 應付 $65 給 陳啟瑋"); // 結算結果顯示第一行

        for (Map.Entry<String, List<String>> entry : monthMap.entrySet()) {
            displayItems.add("📅 " + entry.getKey()); // 月份小標題
            displayItems.addAll(entry.getValue());   // 當月所有支出
        }

        // 將資料用 ArrayAdapter 顯示在 ListView 上
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, displayItems);
        listView.setAdapter(adapter);

        // 點擊新增支出按鈕，跳轉到 GroupCharge2 畫面
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(GroupDetail.this, GroupCharge2.class);
            startActivityForResult(intent, 100);
        });
    }

    // 預留接收新增支出資料結果的處理（目前尚未使用）
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // TODO: 可根據返回資料更新氣泡圖與支出紀錄
    }
}
