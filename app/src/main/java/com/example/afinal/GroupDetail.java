package com.example.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import android.graphics.Color;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class GroupDetail extends AppCompatActivity {

    private ListView listView;
    private FloatingActionButton fabAdd;
    private PieChart pieChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail);

        listView = findViewById(R.id.listViewMembers);
        fabAdd = findViewById(R.id.fabAddExpense);
        pieChart = findViewById(R.id.pieChart);

        PieChart pieChart = findViewById(R.id.pieChart);
        List<PieEntry> entries = new ArrayList<>();

        entries.add(new PieEntry(300f, "小明"));
        entries.add(new PieEntry(150f, "小美"));
        entries.add(new PieEntry(150f, "小張"));

        pieChart.setUsePercentValues(true);

        PieDataSet dataSet = new PieDataSet(entries, "分帳情況");
        dataSet.setColors(Color.parseColor("#4CAF50"), Color.parseColor("#FF9800"), Color.parseColor("#03A9F4"));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(14f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setCenterText("群組分帳");
        pieChart.setCenterTextSize(16f);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);

        Description desc = new Description();
        desc.setText("");
        pieChart.setDescription(desc);

        pieChart.invalidate(); // 刷新圖表


        String[] members = {
                "小明：應收 $300",
                "小美：應付 $150",
                "小張：應付 $150"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, members);
        listView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(GroupDetail.this, GroupCharge.class);
            startActivityForResult(intent, 100);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            String summary = data.getStringExtra("summary");
            if (summary != null) {
                String[] updatedMembers = summary.split("\n");
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_list_item_1, updatedMembers);
                listView.setAdapter(adapter);

                // 更新 PieChart
                List<PieEntry> updatedEntries = new ArrayList<>();
                for (String line : updatedMembers) {
                    String[] parts = line.split("：|\\$");
                    if (parts.length >= 3) {
                        String name = parts[0].trim();
                        float amount = Float.parseFloat(parts[2].trim());
                        updatedEntries.add(new PieEntry(amount, name));
                    }
                }
                PieDataSet newDataSet = new PieDataSet(updatedEntries, "分帳情況");
                newDataSet.setColors(Color.parseColor("#4CAF50"), Color.parseColor("#FF9800"), Color.parseColor("#03A9F4"));
                newDataSet.setValueTextColor(Color.BLACK);
                newDataSet.setValueTextSize(14f);

                PieData newData = new PieData(newDataSet);
                pieChart.setData(newData);
                pieChart.invalidate();
            }

        }
    }
}
