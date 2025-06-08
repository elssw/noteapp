    package com.example.afinal.Fragment;

    import android.app.Activity;
    import android.app.AlertDialog;
    import android.content.Intent;
    import android.os.Bundle;
    import android.util.Log;
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
    import com.example.afinal.model.LocalRecord;
    import com.example.afinal.model.Record;
    import com.google.android.material.button.MaterialButton;
    import com.google.android.material.floatingactionbutton.FloatingActionButton;
    import com.google.firebase.firestore.FirebaseFirestore;
    import com.google.gson.Gson;
    import com.google.gson.reflect.TypeToken;
    import com.google.firebase.firestore.DocumentSnapshot;

    import android.content.SharedPreferences;
    import android.content.Context;

    import java.util.ArrayList;
    import java.util.Calendar;
    import java.util.Collections;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;

    public class UserFragment extends Fragment {
        private static final int REQ_CHARGE = 100;
        private static final String PREF_NAME = "month_pref";
        private static final String KEY_YEAR = "selected_year";
        private static final String KEY_MONTH = "selected_month";

        private MaterialButton btnMonthPicker;
        private TextView tvOutcome;
        private RecyclerView rvRecords;
        private RecordAdapter adapter;
        private int selectedYear, selectedMonth;
        private final List<Record> allRecords = new ArrayList<>();

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_user, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main), (v, insets) -> {
                Insets s = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(s.left, s.top, s.right, s.bottom);
                return insets;
            });

            btnMonthPicker = view.findViewById(R.id.btnMonthPicker);
            tvOutcome = view.findViewById(R.id.outcome);
            rvRecords = view.findViewById(R.id.rvRecords);
            rvRecords.setLayoutManager(new LinearLayoutManager(requireContext()));

            adapter = new RecordAdapter(new ArrayList<>(), new RecordAdapter.OnRecordActionListener() {
                @Override
                public void onRecordClick(Record record, int position) {
                    Intent i = new Intent(requireContext(), Charge.class);
                    i.putExtra("edit", true);
                    i.putExtra("iconRes", record.getIconResId());
                    i.putExtra("price", record.getPrice());
                    i.putExtra("note", record.getNote());
                    i.putExtra("date", record.getDate());
                    i.putExtra("categoryName", record.getCategoryName());
                    i.putExtra("location", record.getLocation());
                    i.putExtra("position", position);
                    i.putExtra("userId", getUserId());
                    startActivityForResult(i, REQ_CHARGE);
                }

                @Override
                public void onRecordDelete(Record record, int position) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("刪除確認")
                            .setMessage("確定要刪除這筆記帳紀錄嗎？")
                            .setPositiveButton("刪除", (d, w) -> {
                                allRecords.remove(position);
                                removeFromStorage(record, UserFragment.this::refreshList);
                            })
                            .setNegativeButton("取消", null)
                            .show();
                }
            });
            rvRecords.setAdapter(adapter);

            SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            selectedYear = prefs.getInt(KEY_YEAR, Calendar.getInstance().get(Calendar.YEAR));
            selectedMonth = prefs.getInt(KEY_MONTH, Calendar.getInstance().get(Calendar.MONTH) + 1);
            btnMonthPicker.setText(String.format("%d-%02d", selectedYear, selectedMonth));
            btnMonthPicker.setOnClickListener(v -> showMonthPickerDialog());

            FloatingActionButton fab = view.findViewById(R.id.fabAdd);
            fab.setOnClickListener(v -> {
                Intent i = new Intent(requireContext(), Charge.class);
                i.putExtra("userId", getUserId());
                startActivityForResult(i, REQ_CHARGE);
            });


            loadRecords();
        }

        private String getUserId() {
            SharedPreferences prefs = requireContext().getSharedPreferences("login", Context.MODE_PRIVATE);
            return prefs.getString("userid", "0");
        }

        private void showMonthPickerDialog() {
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

                SharedPreferences.Editor editor = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
                editor.putInt(KEY_YEAR, selectedYear);
                editor.putInt(KEY_MONTH, selectedMonth);
                editor.apply();

                refreshList();
                dlg.dismiss();
            });
            dlg.show();
        }

        @Override
        public void onResume() {
            super.onResume();
            loadRecords();
        }

        private void loadRecords() {
            SharedPreferences prefs = requireContext().getSharedPreferences("login", Context.MODE_PRIVATE);
            String userId = prefs.getString("userid", "0");

            if (userId.equals("0")) {
                allRecords.clear();
                allRecords.addAll(loadLocalRecords());
                refreshList();
            } else {
                syncLocalIfExists(userId);
                loadFirebaseRecords(userId);
            }
        }

        private Record convertToRecord(LocalRecord lr) {
            String cleanAmount = lr.amount.replaceFirst("^0+(?!$)", "");
            return new Record(
                    lr.iconRes,
                    "NT$" + cleanAmount,
                    lr.note,
                    lr.date,
                    lr.categoryName,
                    lr.location
            );
        }


        private List<Record> loadLocalRecords() {
            SharedPreferences prefs = requireContext().getSharedPreferences("local_records", Context.MODE_PRIVATE);
            String json = prefs.getString("records", "[]");
            List<LocalRecord> locals = new Gson().fromJson(json, new TypeToken<List<LocalRecord>>(){}.getType());
            List<Record> results = new ArrayList<>();
            for (LocalRecord lr : locals) results.add(convertToRecord(lr));
            return results;
        }

        private void loadFirebaseRecords(String userId) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(userId).collection("records")
                    .get()
                    .addOnSuccessListener(query -> {
                        allRecords.clear();
                        for (DocumentSnapshot doc : query.getDocuments()) {
                            String amount = doc.getString("amount");
                            if (amount != null) amount = amount.replaceFirst("^0+(?!$)", "");
                            String note = doc.getString("note");
                            String date = doc.getString("date");
                            String cat = doc.getString("category");
                            String location = doc.getString("location");
                            Long iconResLong = doc.getLong("iconRes");
                            int iconRes = (iconResLong != null) ? iconResLong.intValue() : R.drawable.ic_add;
                            allRecords.add(new Record(iconRes, "NT$" + amount, note, date, cat, location));
                        }
                        refreshList();
                    })
                    .addOnFailureListener(e -> Log.e("LoadFirebase", "錯誤: " + e.getMessage()));
        }

        private void syncLocalIfExists(String userId) {
            SharedPreferences prefs = requireContext().getSharedPreferences("local_records", Context.MODE_PRIVATE);
            String json = prefs.getString("records", "[]");
            List<LocalRecord> locals = new Gson().fromJson(json, new TypeToken<List<LocalRecord>>(){}.getType());

            for (LocalRecord lr : locals) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                Map<String, Object> data = new HashMap<>();
                data.put("amount", lr.amount);
                data.put("note", lr.note);
                data.put("category", lr.categoryName);
                data.put("iconRes", lr.iconRes);
                data.put("location", lr.location);
                data.put("date", lr.date);
                data.put("imageUrls", lr.imageUris);
                db.collection("users").document(userId)
                        .collection("records").add(data);
            }
            prefs.edit().remove("records").apply();
        }

        private void refreshList() {
            String prefix = String.format("%d-%02d", selectedYear, selectedMonth);
            List<Record> filtered = new ArrayList<>();
            double sum = 0;
            for (Record r : allRecords) {
                if (r.getDate().startsWith(prefix)) {
                    filtered.add(r);
                    String p = r.getPrice().replaceAll("[^\\d.]", "");
                    if (!p.isEmpty()) sum += Double.parseDouble(p);
                }
            }

            adapter.updateData(filtered);

            tvOutcome.setTextColor(0xFFFF0000);
            tvOutcome.setText("本月支出: NT$" + Math.round(sum));
        }



        @Override
        public void onActivityResult(int req, int res, Intent data) {
            super.onActivityResult(req, res, data);
            if (req == REQ_CHARGE && res == Activity.RESULT_OK && data != null) {
                int icon = data.getIntExtra("iconRes", R.drawable.ic_add);
                String cat = data.getStringExtra("categoryName");
                String price = data.getStringExtra("price");
                String note = data.getStringExtra("note");
                String date = data.getStringExtra("date");
                String location = data.getStringExtra("location");
                boolean isEdit = data.getBooleanExtra("edit", false);
                int position = data.getIntExtra("position", -1);

                Record newRecord = new Record(icon, price, note, date, cat, location);

                SharedPreferences prefs = requireContext().getSharedPreferences("login", Context.MODE_PRIVATE);
                String userId = prefs.getString("userid", "0");

                if (isEdit && position >= 0 && position < allRecords.size()) {
                    Record oldRecord = allRecords.get(position);
                    if (userId.equals("0")) {
                        allRecords.set(position, newRecord);
                        removeFromStorage(oldRecord);
                        saveToLocal(newRecord);
                        loadRecords();
                    } else {
                        FirebaseFirestore.getInstance()
                                .collection("users").document(userId)
                                .collection("records")
                                .whereEqualTo("date", oldRecord.getDate())
                                .whereEqualTo("amount", oldRecord.getPrice().replaceAll("[^\\d.]", ""))
                                .whereEqualTo("note", oldRecord.getNote())
                                .get()
                                .addOnSuccessListener(q -> {
                                    for (DocumentSnapshot doc : q.getDocuments()) {
                                        doc.getReference().delete();
                                    }
                                    allRecords.set(position, newRecord);
                                    saveToFirebase(newRecord, userId, this::loadRecords);
                                })
                                .addOnFailureListener(e -> Log.e("EditFirebase", "刪除原紀錄失敗: " + e.getMessage()));
                    }
                } else {
                    allRecords.add(newRecord);
                    if (userId.equals("0")) saveToLocal(newRecord);
                    else saveToFirebase(newRecord, userId, this::refreshList);
                }
            }
        }


        private void removeFromStorage(Record r, Runnable onDone) {
            SharedPreferences prefs = requireContext().getSharedPreferences("login", Context.MODE_PRIVATE);
            String userId = prefs.getString("userid", "0");
            if (userId.equals("0")) {
                SharedPreferences sp = requireContext().getSharedPreferences("local_records", Context.MODE_PRIVATE);
                String json = sp.getString("records", "[]");
                List<LocalRecord> list = new Gson().fromJson(json, new TypeToken<List<LocalRecord>>(){}.getType());
                list.removeIf(lr -> lr.date.equals(r.getDate()) && lr.amount.equals(r.getPrice().replaceAll("[^\\d.]", "")));
                sp.edit().putString("records", new Gson().toJson(list)).apply();
                onDone.run(); // 本地刪除完馬上 refresh
            } else {
                FirebaseFirestore.getInstance()
                        .collection("users").document(userId)
                        .collection("records")
                        .whereEqualTo("date", r.getDate())
                        .whereEqualTo("amount", r.getPrice().replaceAll("[^\\d.]", ""))
                        .whereEqualTo("note", r.getNote())
                        .get()
                        .addOnSuccessListener(q -> {
                            List<DocumentSnapshot> docs = q.getDocuments();
                            if (docs.isEmpty()) {
                                onDone.run(); // 沒有資料可以刪也要 refresh
                                return;
                            }

                            final int total = docs.size();
                            final int[] count = {0};

                            for (DocumentSnapshot doc : docs) {
                                doc.getReference().delete()
                                        .addOnSuccessListener(aVoid -> {
                                            count[0]++;
                                            if (count[0] == total) onDone.run(); // 等全部刪除後才 refresh
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("FirebaseDelete", "刪除失敗：" + e.getMessage());
                                            count[0]++;
                                            if (count[0] == total) onDone.run(); // 即使有刪除失敗也不能卡住
                                        });
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("FirebaseQuery", "查詢失敗：" + e.getMessage());
                            onDone.run(); // 查詢失敗也讓畫面刷新避免卡死
                        });

            }
        }




        private void saveToLocal(Record r) {
            SharedPreferences prefs = requireContext().getSharedPreferences("local_records", Context.MODE_PRIVATE);
            String json = prefs.getString("records", "[]");
            List<LocalRecord> list = new Gson().fromJson(json, new TypeToken<List<LocalRecord>>(){}.getType());
            list.add(new LocalRecord(
                    r.getIconResId(),
                    r.getPrice().replaceAll("[^\\d.]", ""),
                    r.getNote(),
                    r.getDate(),
                    r.getCategoryName(),
                    r.getLocation(),
                    Collections.emptyList()
            ));
            prefs.edit().putString("records", new Gson().toJson(list)).apply();
        }

        private void removeFromStorage(Record r) {
            SharedPreferences prefs = requireContext().getSharedPreferences("login", Context.MODE_PRIVATE);
            String userId = prefs.getString("userid", "0");
            if (userId.equals("0")) {
                // 本地刪除
                SharedPreferences sp = requireContext().getSharedPreferences("local_records", Context.MODE_PRIVATE);
                String json = sp.getString("records", "[]");
                List<LocalRecord> list = new Gson().fromJson(json, new TypeToken<List<LocalRecord>>(){}.getType());
                list.removeIf(lr -> lr.date.equals(r.getDate()) && lr.amount.equals(r.getPrice().replaceAll("[^\\d.]", "")));
                sp.edit().putString("records", new Gson().toJson(list)).apply();
            } else {
                // Firebase 刪除
                FirebaseFirestore.getInstance()
                        .collection("users").document(userId)
                        .collection("records")
                        .whereEqualTo("date", r.getDate())
                        .whereEqualTo("amount", r.getPrice().replaceAll("[^\\d.]", ""))
                        .whereEqualTo("note", r.getNote())
                        .get()
                        .addOnSuccessListener(q -> {
                            for (DocumentSnapshot doc : q.getDocuments()) {
                                doc.getReference().delete();
                            }
                        });
            }
        }

        private void saveToFirebase(Record r, String userId, Runnable onDone) {
            Map<String, Object> data = new HashMap<>();
            data.put("iconRes", r.getIconResId());
            data.put("amount", r.getPrice().replaceAll("[^\\d.]", ""));
            data.put("note", r.getNote());
            data.put("date", r.getDate());
            data.put("category", r.getCategoryName());
            data.put("location", r.getLocation());
            data.put("imageUrls", new ArrayList<String>());

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .collection("records")
                    .add(data)
                    .addOnSuccessListener(d -> onDone.run())
                    .addOnFailureListener(e -> {
                        Log.e("FirebaseSave", "上傳失敗：" + e.getMessage());
                        onDone.run(); // 即使失敗也不要卡住 UI
                    });
        }
    }
