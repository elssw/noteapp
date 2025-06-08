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
    import com.google.firebase.firestore.Query;
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
    import java.util.UUID;

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
        private String userId;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_user, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            userId = getUserId();
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
                    if (!userId.equals("0")) {
                        i.putExtra("userId", userId);
                    }
                    startActivityForResult(i, REQ_CHARGE);
                }

                @Override
                public void onRecordDelete(Record record, int position) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("刪除確認")
                            .setMessage("確定要刪除這筆記帳紀錄嗎？")
                            .setPositiveButton("刪除", (d, w) -> {
                                allRecords.remove(position);
                                if (userId.equals("0")) {
                                    // 本地刪除
                                    removeFromStorage(record, UserFragment.this::refreshList);
                                } else {
                                    // 直接用 docId 刪除單一文件
                                    FirebaseFirestore.getInstance()
                                            .collection("users").document(userId)
                                            .collection("records")
                                            .document(record.getDocId())
                                            .delete()
                                            .addOnSuccessListener(a -> refreshList());
                                }
                            }).setNegativeButton("取消", null).show();
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
                if (!userId.equals("0")) {
                    i.putExtra("userId", userId); //登入時才傳
                }
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
                syncLocalIfExists(userId, () -> loadFirebaseRecords(userId)); // ✅ 同步完成後再載入 Firebase
            }
        }


        private Record convertToRecord(LocalRecord lr) {
            String clean = lr.amount.replaceFirst("^0+(?!$)", "");
            return new Record(
                    lr.id,               // 用 localRecord.id
                    lr.iconRes,
                    "NT$" + clean,
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
            results.sort((a, b) -> b.getDate().compareTo(a.getDate()));
            return results;
        }

        private void loadFirebaseRecords(String userId) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(userId).collection("records")
                    .orderBy("date", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(query -> {
                        allRecords.clear();
                        for (DocumentSnapshot doc : query.getDocuments()) {
                            String id = doc.getId();
                            String amount = doc.getString("amount");
                            if (amount != null) amount = amount.replaceFirst("^0+(?!$)", "");
                            String note = doc.getString("note");
                            String date = doc.getString("date");
                            String cat = doc.getString("category");
                            String location = doc.getString("location");
                            Long iconResLong = doc.getLong("iconRes");
                            int iconRes = (iconResLong != null) ? iconResLong.intValue() : R.drawable.ic_add;
                            allRecords.add(new Record(
                                    id,
                                    iconRes,
                                    "NT$" + amount,
                                    note,
                                    date,
                                    cat,
                                    location
                            ));
                        }
                        refreshList();
                    })
                    .addOnFailureListener(e -> Log.e("LoadFirebase", "錯誤: " + e.getMessage()));
        }


        private void syncLocalIfExists(String userId, Runnable onSynced) {
            SharedPreferences prefs = requireContext().getSharedPreferences("local_records", Context.MODE_PRIVATE);

            // ✅ 如果已經同步過，就跳過
            if (prefs.getBoolean("has_synced", false)) {
                onSynced.run();
                return;
            }

            prefs.edit().putBoolean("has_synced", true).apply();

            String json = prefs.getString("records", "[]");
            List<LocalRecord> locals = new Gson().fromJson(json, new TypeToken<List<LocalRecord>>() {}.getType());

            if (locals.isEmpty()) {
                prefs.edit().putBoolean("has_synced", true).apply(); // 雖然是空的也標記已同步
                onSynced.run();
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            final int total = locals.size();
            final int[] count = {0};

            for (LocalRecord lr : locals) {
                Map<String, Object> data = new HashMap<>();
                data.put("amount", lr.amount);
                data.put("note", lr.note);
                data.put("category", lr.categoryName);
                data.put("iconRes", lr.iconRes);
                data.put("location", lr.location);
                data.put("date", lr.date);
                data.put("imageUrls", lr.imageUris);

                db.collection("users").document(userId)
                        .collection("records").add(data)
                        .addOnSuccessListener(d -> {
                            count[0]++;
                            if (count[0] == total) {
                                prefs.edit()
                                        .remove("records") // ✅ 清除本地資料
                                        .putBoolean("has_synced", true) // ✅ 標記已上傳
                                        .apply();
                                onSynced.run();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("Sync", "上傳失敗：" + e.getMessage());
                            count[0]++;
                            if (count[0] == total) {
                                prefs.edit()
                                        .remove("records")
                                        .putBoolean("has_synced", true)
                                        .apply();
                                onSynced.run();
                            }
                        });
            }
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

                Record newRecord = new Record(
                        "",       // 本地新建的資料先傳空 docId
                        icon,
                        price,
                        note,
                        date,
                        cat,
                        location
                );


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
                    if (userId.equals("0")) saveToLocal(newRecord);
                    else saveToFirebase(newRecord, userId, this::refreshList);
                }
            }
        }


        private void removeFromStorage(Record r, Runnable onDone) {
            SharedPreferences prefsLogin = requireContext()
                    .getSharedPreferences("login", Context.MODE_PRIVATE);
            String userId = prefsLogin.getString("userid", "0");

            if (userId.equals("0")) {
                // 未登入 → 刪本地
                SharedPreferences sp = requireContext()
                        .getSharedPreferences("local_records", Context.MODE_PRIVATE);
                String json = sp.getString("records", "[]");
                List<LocalRecord> list = new Gson().fromJson(
                        json, new TypeToken<List<LocalRecord>>(){}.getType()
                );

                // 用 docId（也就是 LocalRecord.id）精準刪除
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).id.equals(r.getDocId())) {
                        list.remove(i);
                        break;
                    }
                }

                sp.edit()
                        .putString("records", new Gson().toJson(list))
                        .apply();

                // 刪完馬上刷新
                onDone.run();

            } else {
                // 已登入 → 刪 Firestore 裡的單一 document
                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .collection("records")
                        .document(r.getDocId())
                        .delete()
                        .addOnSuccessListener(aVoid -> onDone.run())
                        .addOnFailureListener(e -> {
                            Log.e("FirestoreDelete", "刪除失敗：" + e.getMessage());
                            onDone.run();
                        });
            }
        }





        private void saveToLocal(Record r) {
            SharedPreferences prefs = requireContext()
                    .getSharedPreferences("local_records", Context.MODE_PRIVATE);
            String json = prefs.getString("records", "[]");
            List<LocalRecord> list = new Gson().fromJson(
                    json, new TypeToken<List<LocalRecord>>(){}.getType()
            );

            // 1. 先產生一個唯一 id
            String uuid = UUID.randomUUID().toString();

            // 2. 用帶 id 的建構子建立 LocalRecord
            LocalRecord lr = new LocalRecord(
                    uuid,
                    r.getIconResId(),
                    r.getPrice().replaceAll("[^\\d.]", ""),
                    r.getNote(),
                    r.getDate(),
                    r.getCategoryName(),
                    r.getLocation(),
                    Collections.emptyList()  // 如果有圖片 URI，再從 r 拿進來
            );

            list.add(lr);

            // 3. 寫回 prefs
            prefs.edit()
                    .putString("records", new Gson().toJson(list))
                    .apply();
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
