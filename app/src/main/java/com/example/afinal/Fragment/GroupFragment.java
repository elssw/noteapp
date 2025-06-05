package com.example.afinal.Fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.afinal.GroupDetail;
import com.example.afinal.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO:即時更新群組
public class GroupFragment extends Fragment {

    private LinearLayout groupContainer;
    private EditText etSearchGroup;
    private Context context;

    private ArrayList<String> groupNames;
    private ArrayList<String> allGroupNames;
    private HashMap<String, Integer> groupMembers = new HashMap<>();
    private HashMap<String, Integer> groupRecords = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_group, container, false);
        context = getContext();

        groupContainer = view.findViewById(R.id.groupContainer);
        etSearchGroup = view.findViewById(R.id.etSearchGroup);

        groupNames = new ArrayList<>();
        allGroupNames = new ArrayList<>();

//        addDummyGroups();

        etSearchGroup.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterGroups(s.toString());
            }
        });

        refreshGroupItems();
        return view;
    }

    private void addDummyGroups() {
        groupNames.add("旅遊基金");
        groupNames.add("家庭帳本");
        groupNames.add("專題組");

        groupMembers.put("旅遊基金", 4);
        groupMembers.put("家庭帳本", 2);
        groupMembers.put("專題組", 3);

        groupRecords.put("旅遊基金", 10);
        groupRecords.put("家庭帳本", 5);
        groupRecords.put("專題組", 8);
    }

    private void refreshGroupItems() {
        allGroupNames.clear();
        allGroupNames.addAll(groupNames);
        filterGroups(etSearchGroup.getText().toString());
    }

    private void filterGroups(String keyword) {
        groupContainer.removeAllViews();
//        addGroupItem("新增群組", true,"");

        int matchedCount = 0;
        for (String name : allGroupNames) {
            if (name.contains(keyword)) {
          //      addGroupItem(name, false);
                matchedCount++;
            }
        }

        if (matchedCount == 0) {
            TextView emptyText = new TextView(context);
            emptyText.setText("沒有符合的群組");
            emptyText.setTextSize(16f);
            emptyText.setTextColor(Color.GRAY);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, 48, 0, 0);
            groupContainer.addView(emptyText);
        }
    }

    private void addGroupItem(String groupName, boolean isAddButton, String base64Image) {
        LinearLayout rowLayout = new LinearLayout(context);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setPadding(24, 24, 24, 24);
        rowLayout.setBackgroundResource(R.drawable.group_item_background);
        rowLayout.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, 24);
        rowLayout.setLayoutParams(layoutParams);

        ImageView image = new ImageView(context);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(180, 180);
        image.setLayoutParams(imageParams);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        rowLayout.addView(image);

        LinearLayout infoLayout = new LinearLayout(context);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        infoLayout.setPadding(24, 0, 0, 0);
        infoLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView tvName = new TextView(context);
        tvName.setTextSize(18f);

        TextView tvMembers = new TextView(context);
        TextView tvRecords = new TextView(context);

        if (isAddButton) {
            image.setImageResource(android.R.drawable.ic_input_add);
            tvName.setText("新增群組");
            tvName.setTextColor(Color.parseColor("#2E7D32"));
            tvMembers.setText("點擊建立新群組");
            tvMembers.setTextColor(Color.GRAY);
            tvRecords.setVisibility(View.GONE);
        } else {
//            image.setImageResource(R.drawable.group_default_photo);
            if (base64Image != null && !base64Image.isEmpty()) {
                try {
                    byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                    Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    image.setImageBitmap(decodedBitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                    image.setImageResource(R.drawable.group_default_photo); // 解碼失敗 fallback
                }
            } else {
                image.setImageResource(R.drawable.group_default_photo); // 沒有圖片 fallback
            }
            tvName.setText(groupName);
            tvName.setTextColor(Color.BLACK);
            int memberCount = groupMembers.getOrDefault(groupName, 0);
            int recordCount = groupRecords.getOrDefault(groupName, 0);
            tvMembers.setText("組員：" + memberCount + "人");
            tvMembers.setTextColor(Color.DKGRAY);
            tvRecords.setText("記帳筆數：" + recordCount);
            tvRecords.setTextColor(Color.DKGRAY);
            tvRecords.setVisibility(View.VISIBLE);
        }

        infoLayout.addView(tvName);
        infoLayout.addView(tvMembers);
        infoLayout.addView(tvRecords);
        rowLayout.addView(infoLayout);

        rowLayout.setOnLongClickListener(v -> {
            if (!isAddButton) {
                showEditDeleteDialog(groupName);
            }
            return true;
        });

        rowLayout.setOnClickListener(v -> {
            if (isAddButton) {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_main, new GroupCreateFragment())
                        .addToBackStack(null)
                        .commit();
            } else {
                Toast.makeText(context, "進入 " + groupName, Toast.LENGTH_SHORT).show();
                // 🔽 原本跳到群組管理 Fragment
                // SingleGroupManageFragment fragment = SingleGroupManageFragment.newInstance(groupName);
                // requireActivity().getSupportFragmentManager()
                //         .beginTransaction()
                //         .replace(R.id.fragment_main, fragment)
                //         .addToBackStack(null)
                //         .commit();

                // ✅ 改成跳到分帳情況 Activity
                Intent intent = new Intent(context, GroupDetail.class);
                intent.putExtra("groupName", groupName); // 若你要傳值可加上
                startActivity(intent);
            }
        });


        groupContainer.addView(rowLayout);
    }

    private void showEditDeleteDialog(String groupName) {//TODO:刪除群組
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);
        builder.setTitle("選擇操作")
                .setItems(new CharSequence[]{"編輯群組名稱", "刪除群組"}, (dialog, which) -> {
                    if (which == 0) {
                        showRenameDialog(groupName);
                    } else if (which == 1) {
                        groupNames.remove(groupName);
                        groupMembers.remove(groupName);
                        groupRecords.remove(groupName);
                        refreshGroupItems();
                    }
                })
                .show();
    }

//    private void showRenameDialog(String oldName) {//TODO:修群組名
//        EditText input = new EditText(context);
//        input.setText(oldName);
//        input.setSelection(oldName.length());
//
//        new androidx.appcompat.app.AlertDialog.Builder(context)
//                .setTitle("重新命名群組")
//                .setView(input)
//                .setPositiveButton("確定", (dialog, which) -> {
//                    String newName = input.getText().toString().trim();
//                    if (!newName.isEmpty() && !groupNames.contains(newName)) {
//                        int member = groupMembers.getOrDefault(oldName, 0);
//                        int record = groupRecords.getOrDefault(oldName, 0);
//                        groupNames.remove(oldName);
//                        groupNames.add(newName);
//                        groupMembers.remove(oldName);
//                        groupRecords.remove(oldName);
//                        groupMembers.put(newName, member);
//                        groupRecords.put(newName, record);
//                        refreshGroupItems();
//                    } else {
//                        Toast.makeText(context, "名稱重複或為空", Toast.LENGTH_SHORT).show();
//                    }
//                })
//                .setNegativeButton("取消", null)
//                .show();
//    }

    private void showRenameDialog(String oldName) {
        EditText input = new EditText(context);
        input.setText(oldName);
        input.setSelection(oldName.length());

        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("重新命名群組")
                .setView(input)
                .setPositiveButton("確定", (dialog, which) -> {
                    String newName = input.getText().toString().trim();

                    if (newName.isEmpty() || groupNames.contains(newName)) {
                        Toast.makeText(context, "名稱重複或為空", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // UI 更新
                    int member = groupMembers.getOrDefault(oldName, 0);
                    int record = groupRecords.getOrDefault(oldName, 0);
                    groupNames.remove(oldName);
                    groupNames.add(newName);
                    groupMembers.remove(oldName);
                    groupRecords.remove(oldName);
                    groupMembers.put(newName, member);
                    groupRecords.put(newName, record);
                    refreshGroupItems();
                    SharedPreferences prefs = requireContext().getSharedPreferences("login", Context.MODE_PRIVATE);
                    String userId = prefs.getString("userid", "0");
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    if (!userId.equals("0")) {
                        // Firestore 更新邏輯
                        DocumentReference groupDoc = db.collection("users")
                                .document(userId)
                                .collection("group")
                                .document(oldName);

                        groupDoc.get().addOnSuccessListener(docSnapshot -> {
                            if (!docSnapshot.exists()) {
                                Log.e("Firestore", "找不到群組：" + oldName);
                                return;
                            }

                            Map<String, Object> oldData = docSnapshot.getData();
                            List<String> members = (List<String>) oldData.get("members");

                            if (members == null) {
                                Log.e("Firestore", "群組沒有 members 欄位");
                                return;
                            }

                            for (String uid : members) {
                                DocumentReference newDocRef = db.collection("users").document(uid)
                                        .collection("group").document(newName);

                                newDocRef.get().addOnSuccessListener(snapshot -> {
//                                    if (snapshot.exists()) {
//                                        Log.w("Firestore", "使用者 " + uid + " 已有同名群組 " + newName);
//                                        return;
//                                    }

                                    DocumentReference oldDocRef = db.collection("users").document(uid)
                                            .collection("group").document(oldName);

                                    oldDocRef.get().addOnSuccessListener(oldSnapshot -> {
                                        if (oldSnapshot.exists()) {
                                            Map<String, Object> data = oldSnapshot.getData();
                                            newDocRef.set(data)
                                                    .addOnSuccessListener(unused -> {
                                                        oldDocRef.delete();  // 刪除舊的
                                                        Log.d("Firestore", "群組重新命名成功 for " + uid);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e("Firestore", "寫入新名稱失敗 for " + uid + ": " + e.getMessage());
                                                    });
                                        }
                                    }).addOnFailureListener(e -> {
                                        Log.e("Firestore", "讀取舊群組失敗 for " + uid + ": " + e.getMessage());
                                    });

                                }).addOnFailureListener(e -> {
                                    Log.e("Firestore", "檢查新名稱失敗 for " + uid + ": " + e.getMessage());
                                });
                            }
                        }).addOnFailureListener(e -> {
                            Log.e("Firestore", "讀取原始群組失敗：" + e.getMessage());
                        });

//                        db.collection("users")
//                                .document(userId)
//                                .collection("group")
//                                .get()
//                                .addOnSuccessListener(querySnapshot -> {
//                                    groupNames.clear();  // 清空舊資料
//                                    for (QueryDocumentSnapshot doc : querySnapshot) {
//                                        String groupName = doc.getId(); // 使用 document 名稱為群組名稱
//
//                                        List<String> members = (List<String>) doc.get("members");
//                                        int count = (members != null) ? members.size() : 1;
//                                        for (String uid : members) {
//                                            DocumentReference newDocRef = db.collection("users").document(uid)
//                                                    .collection("group").document(newName);
//
//                                            newDocRef.get().addOnSuccessListener(snapshot -> {
//                                                if (snapshot.exists()) {
//                                                    Log.w("Firestore", "使用者 " + uid + " 已有同名群組 " + newName);
//                                                    return;
//                                                }
//
//                                                DocumentReference oldDocRef = db.collection("users").document(uid)
//                                                        .collection("group").document(oldName);
//
//                                                oldDocRef.get().addOnSuccessListener(oldSnapshot -> {
//                                                    if (oldSnapshot.exists()) {
//                                                        Map<String, Object> oldData = oldSnapshot.getData();
//
//                                                        newDocRef.set(oldData)  // 新名稱寫入資料
//                                                                .addOnSuccessListener(unused -> {
//                                                                    oldDocRef.delete();  // 刪除舊的
//                                                                    Log.d("Firestore", "群組重新命名成功 for " + uid);
//                                                                })
//                                                                .addOnFailureListener(e ->
//                                                                        Log.e("Firestore", "寫入新名稱失敗 for " + uid + ": " + e.getMessage())
//                                                                );
//                                                    }
//                                                }).addOnFailureListener(e ->
//                                                        Log.e("Firestore", "讀取舊群組失敗 for " + uid + ": " + e.getMessage())
//                                                );
//
//                                            }).addOnFailureListener(e ->
//                                                    Log.e("Firestore", "檢查新名稱失敗 for " + uid + ": " + e.getMessage())
//                                            );
//                                        }
//                                    }
//
//                                })
//                                .addOnFailureListener(e -> Log.e("Firestore", "讀取群組失敗：" + e.getMessage()));
                    }

                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();

        Bundle args = getArguments();
        if (args != null && args.containsKey("new_group_name")) {
            String newGroupName = args.getString("new_group_name");
            if (!groupNames.contains(newGroupName)) {
                groupNames.add(newGroupName);
                groupMembers.put(newGroupName, 1);  // 預設 1 人
                groupRecords.put(newGroupName, 0); // 預設 0 筆
            }
            setArguments(null); // 避免重複新增
        }
        loadGroupsFromFirestore();
        refreshGroupItems();
    }

    private void loadGroupsFromFirestore() {
        groupContainer.removeAllViews(); // 清空舊資料

        SharedPreferences prefs = requireContext().getSharedPreferences("login", Context.MODE_PRIVATE);
        String userId = prefs.getString("userid", "0");

        if (!userId.equals("0")) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .document(userId)
                    .collection("group")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        groupNames.clear();  // 清空舊資料
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            String groupName = doc.getId(); // 使用 document 名稱為群組名稱
                            groupNames.add(groupName);

                            // 讀取圖片（可選）
                            String base64Image = doc.getString("group_image");
                            Bitmap bitmap;
                            // 可另外處理 base64 圖片，如果你希望顯示自訂圖示
                            if(base64Image.equals("123")){
                                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.group_default_photo);
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                                base64Image= Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT);
                            }
                            List<String> members = (List<String>) doc.get("members");
                            int count = (members != null) ? members.size() : 1;
                            // 預設顯示 1 人 0 筆，可根據資料庫設計修改
                            groupMembers.put(groupName, count);
                            groupRecords.put(groupName, 0);

                            addGroupItem(groupName, false,base64Image); // 加入畫面
                        }
                        addGroupItem("新增群組", true,"");
//                        addGroupItem("", true,""); // 最後加入「新增群組」按鈕
                    })
                    .addOnFailureListener(e -> Log.e("Firestore", "讀取群組失敗：" + e.getMessage()));
        }
    }

}
