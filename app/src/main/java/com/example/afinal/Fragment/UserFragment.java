package com.example.afinal.Fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
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
import com.example.afinal.model.Record;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class UserFragment extends Fragment {
    private static final int REQ_CHARGE = 100;
    private MaterialButton btnMonthPicker;
    private TextView tvOutcome;
    private RecyclerView rvRecords;
    private int selectedYear, selectedMonth;

    private final List<Record> allRecords = new ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user, container, false);
    }

    @Override public void onViewCreated(@NonNull View view,
                                        @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewCompat.setOnApplyWindowInsetsListener(
                view.findViewById(R.id.main),
                (v,insets)->{
                    Insets s=insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(s.left,s.top,s.right,s.bottom);
                    return insets;
                });

        btnMonthPicker = view.findViewById(R.id.btnMonthPicker);
        tvOutcome      = view.findViewById(R.id.outcome);
        rvRecords      = view.findViewById(R.id.rvRecords);

        rvRecords.setLayoutManager(new LinearLayoutManager(requireContext()));

        Calendar cal = Calendar.getInstance();
        selectedYear  = cal.get(Calendar.YEAR);
        selectedMonth = cal.get(Calendar.MONTH)+1;
        btnMonthPicker.setText(String.format("%d-%02d", selectedYear, selectedMonth));

        btnMonthPicker.setOnClickListener(v -> showMonthPickerDialog());

        FloatingActionButton fab = view.findViewById(R.id.fabAdd);
        fab.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), Charge.class))
        );


        allRecords.add(new Record(R.drawable.ic_add,"NT$123","超難吃","2025-05-17","午餐"));
        allRecords.add(new Record(R.drawable.ic_add,"NT$ 45","太快","2025-05-03","交通"));
        refreshList();
    }

    private void showMonthPickerDialog(){
        View dv = getLayoutInflater().inflate(R.layout.dialog_month_picker,null);
        NumberPicker y = dv.findViewById(R.id.yearPicker);
        NumberPicker m = dv.findViewById(R.id.monthPicker);
        int cy=Calendar.getInstance().get(Calendar.YEAR);
        y.setMinValue(cy-10); y.setMaxValue(cy+10); y.setValue(selectedYear);
        m.setMinValue(1); m.setMaxValue(12); m.setValue(selectedMonth);

        AlertDialog dlg = new AlertDialog.Builder(requireContext())
                .setView(dv).create();
        dv.findViewById(R.id.btnCancel).setOnClickListener(v->dlg.dismiss());
        dv.findViewById(R.id.btnOK).setOnClickListener(v->{
            selectedYear  = y.getValue();
            selectedMonth = m.getValue();
            btnMonthPicker.setText(String.format("%d-%02d", selectedYear, selectedMonth));
            refreshList();
            dlg.dismiss();
        });
        dlg.show();
    }

    private void refreshList(){
        String prefix = String.format("%d-%02d", selectedYear, selectedMonth);
        List<Record> filtered = new ArrayList<>();
        double sum = 0;
        for(Record r: allRecords){
            if(r.getDate().startsWith(prefix)){
                filtered.add(r);
                String p = r.getPrice().replaceAll("[^\\d.]","");
                if(!p.isEmpty()) sum += Double.parseDouble(p);
            }
        }
        rvRecords.setAdapter(new RecordAdapter(filtered));
        tvOutcome.setTextColor(0xFFFF0000);
        tvOutcome.setText("本月支出: NT$"+ Math.round(sum));
    }

    @Override public void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if(req==REQ_CHARGE && res==Charge.RESULT_OK && data!=null){
            int icon    = data.getIntExtra("iconRes", R.drawable.ic_add);
            String cat  = data.getStringExtra("categoryName");
            String price= data.getStringExtra("price");
            String note = data.getStringExtra("note");
            String date = data.getStringExtra("date");
            allRecords.add(new Record(icon, price, note, date, cat));
            refreshList();
        }
    }
}
