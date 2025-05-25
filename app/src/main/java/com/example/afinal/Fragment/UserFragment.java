package com.example.afinal.Fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.afinal.Charge;
import com.example.afinal.R;
import com.example.afinal.RecordAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.afinal.model.Record;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class UserFragment extends Fragment {

    private Spinner spinnerYear, spinnerMonth;
    private RecyclerView rvRecords;
    private TextView tvOutcome;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user, container, false);
    }

    @Override public void onViewCreated(@NonNull View view,
                                        @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main),
                (v, insets) -> {
                    Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(b.left, b.top, b.right, b.bottom);
                    return insets;
                });

        spinnerYear  = view.findViewById(R.id.spinnerYear);
        spinnerMonth = view.findViewById(R.id.spinnerMonth);
        tvOutcome    = view.findViewById(R.id.outcome);

        tvOutcome.setTextColor(Color.RED);

        ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.years_array,
                android.R.layout.simple_spinner_item
        );
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);

        ArrayAdapter<CharSequence> monthAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.months_array,
                android.R.layout.simple_spinner_item
        );
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);

        Calendar cal = Calendar.getInstance();
        String thisYear  = String.valueOf(cal.get(Calendar.YEAR));
        String thisMonth = String.format("%02d", cal.get(Calendar.MONTH) + 1);
        spinnerYear.setSelection(yearAdapter.getPosition(thisYear));
        spinnerMonth.setSelection(monthAdapter.getPosition(thisMonth));

        AdapterView.OnItemSelectedListener listener =
                new AdapterView.OnItemSelectedListener() {
                    @Override public void onItemSelected(AdapterView<?> p, View v,
                                                         int pos, long id) {
                        loadTransactions(
                                spinnerYear.getSelectedItem().toString(),
                                spinnerMonth.getSelectedItem().toString()
                        );
                    }
                    @Override public void onNothingSelected(AdapterView<?> p) {}
                };
        spinnerYear.setOnItemSelectedListener(listener);
        spinnerMonth.setOnItemSelectedListener(listener);

        FloatingActionButton fab = view.findViewById(R.id.fabAdd);
        fab.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), Charge.class))
        );

        rvRecords = view.findViewById(R.id.rvRecords);
        rvRecords.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadTransactions(thisYear, thisMonth);
    }

    private void loadTransactions(String year, String month) {
        List<Record> all = new ArrayList<>();
        all.add(new Record(R.drawable.ic_add, "NT$123", "午餐", "2025-05-17 12:34"));
        all.add(new Record(R.drawable.ic_add, "NT$ 45", "公車", "2025-03-17 08:10"));
        all.add(new Record(R.drawable.ic_add, "NT$200", "電影", "2025-05-16 20:20"));
        all.add(new Record(R.drawable.ic_add, "NT$350", "購物", "2025-05-15 15:45"));
        all.add(new Record(R.drawable.ic_add, "NT$300", "早餐", "2025-04-10 07:20"));
        all.add(new Record(R.drawable.ic_add, "NT$350", "購物", "2025-05-15 15:45"));

        String prefix = year + "-" + month;  // e.g. "2025-05"
        List<Record> filtered = new ArrayList<>();
        double sum = 0;
        for (Record r : all) {
            if (r.getTime().startsWith(prefix)) {
                filtered.add(r);
                String p = r.getPrice().replaceAll("[^\\d.]", "");
                if (!p.isEmpty()) {
                    sum += Double.parseDouble(p);
                }
            }
        }

        rvRecords.setAdapter(new RecordAdapter(filtered));

        long total = Math.round(sum);
        tvOutcome.setText("本月支出: NT$" + total);
    }
}
