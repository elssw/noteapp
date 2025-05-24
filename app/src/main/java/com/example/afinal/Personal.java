package com.example.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.afinal.model.Record;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Personal extends AppCompatActivity {

    private Spinner spinnerYear;
    private Spinner spinnerMonth;
    private RecyclerView rvRecords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_personal);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        // Spinner 年／月
        spinnerYear  = findViewById(R.id.spinnerYear);
        spinnerMonth = findViewById(R.id.spinnerMonth);
        ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(
                this, R.array.years_array, android.R.layout.simple_spinner_item
        );
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);

        ArrayAdapter<CharSequence> monthAdapter = ArrayAdapter.createFromResource(
                this, R.array.months_array, android.R.layout.simple_spinner_item
        );
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);

        // 預設當前年／月
        Calendar cal = Calendar.getInstance();
        String thisYear  = String.valueOf(cal.get(Calendar.YEAR));
        String thisMonth = String.format("%02d", cal.get(Calendar.MONTH) + 1);
        spinnerYear.setSelection(yearAdapter.getPosition(thisYear));
        spinnerMonth.setSelection(monthAdapter.getPosition(thisMonth));

        // 當 Spinner 選擇改變時重載列表
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                loadTransactions(
                        spinnerYear.getSelectedItem().toString(),
                        spinnerMonth.getSelectedItem().toString()
                );
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        };
        spinnerYear.setOnItemSelectedListener(listener);
        spinnerMonth.setOnItemSelectedListener(listener);

        // FAB：跳到 Charge 新增畫面
        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v ->
                startActivity(new Intent(Personal.this, Charge.class))
        );

        // RecyclerView 初始化
        rvRecords = findViewById(R.id.rvRecords);
        rvRecords.setLayoutManager(new LinearLayoutManager(this));

        // 初次載入當前年／月
        loadTransactions(thisYear, thisMonth);
    }

    /**
     * 載入並顯示指定年／月的交易紀錄（demo 資料，未接資料庫）
     */
    private void loadTransactions(String year, String month) {
        // 全部假資料
        List<Record> all = new ArrayList<>();
        all.add(new Record(R.drawable.ic_add,      "NT$123", "午餐", "2025-05-17 12:34"));
        all.add(new Record(R.drawable.ic_add, "NT$ 45", "公車", "2025-03-17 08:10"));
        all.add(new Record(R.drawable.ic_add,       "NT$200", "電影", "2025-05-16 20:20"));
        all.add(new Record(R.drawable.ic_add,  "NT$350", "購物", "2025-05-15 15:45"));
        all.add(new Record(R.drawable.ic_add,      "NT$300", "早餐", "2025-04-10 07:20"));
        // …更多 demo 資料

        // 過濾出符合 year-month 的項目
        String prefix = year + "-" + month; // e.g. "2025-05"
        List<Record> filtered = new ArrayList<>();
        for (Record r : all) {
            if (r.getTime().startsWith(prefix)) {  // 改用 getTime()
                filtered.add(r);
            }
        }

        // 設定給 RecyclerView
        rvRecords.setAdapter(new RecordAdapter(filtered));
    }
}
