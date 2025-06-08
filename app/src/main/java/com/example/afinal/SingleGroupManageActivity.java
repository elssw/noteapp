package com.example.afinal;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SingleGroupManageActivity extends AppCompatActivity {

    private String groupName;
    private String groid;
    private int chocie=0;
    private TextView tvGroupTitle;
    private ImageView imgGroupPhoto;
    private EditText etGroupName;
    private LinearLayout memberContainer;
    private Context context;
    private ArrayList<String> invitedUsers = new ArrayList<>();
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    //private ArrayList<String> invitedUsers = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_single_group_manage);

        context = this;
        groupName = getIntent().getStringExtra("groupName");
        groid = getIntent().getStringExtra("groupId");

        tvGroupTitle = findViewById(R.id.tvGroupTitle);
        imgGroupPhoto = findViewById(R.id.imgGroupPhoto);
        etGroupName = findViewById(R.id.etGroupName);
        memberContainer = findViewById(R.id.memberContainer);
        Button btnInviteMember = findViewById(R.id.btnInviteMember);
        Button btnExitGroup = findViewById(R.id.btnExitGroup);
        TextView btnEditPhoto = findViewById(R.id.btnEditPhoto);
        Button btnSaveSettings = findViewById(R.id.btnSaveSettings);

        if (groupName != null) {
            tvGroupTitle.setText(groupName);
            etGroupName.setText(groupName);
        }

        setupImagePicker();
        loadGroupMembers();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = getSharedPreferences("login", MODE_PRIVATE);
        String userId = prefs.getString("userid", "0");

        btnEditPhoto.setOnClickListener(v -> openImagePicker());
        btnInviteMember.setOnClickListener(v -> showInviteDialog());
        btnExitGroup.setOnClickListener(v -> showExitDialog());
        btnSaveSettings.setOnClickListener(v -> {
            String updatedName = etGroupName.getText().toString().trim();
            if (updatedName.isEmpty()) {
                etGroupName.setError("群組名稱不可為空");
                return;
            }

            db.collection("users")
                    .document(userId)
                    .collection("group")
                    .document(groid)
                    .get()
                    .addOnSuccessListener(doc -> {

                            // 圖片處理
                            Bitmap bitmap;
                            if (imgGroupPhoto.getDrawable() instanceof BitmapDrawable) {
                                bitmap = ((BitmapDrawable) imgGroupPhoto.getDrawable()).getBitmap();
                            } else {
                                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.group_default_photo);
                            }
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 10, stream);
                            String imageBase64 = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT);

                            // 建立群組資料
                            Map<String, Object> groupData = new HashMap<>();
                            groupData.put("group_name", groupName);
                            groupData.put("group_image", (chocie == 1) ? imageBase64 : "123");

                            Map<String, Object> updatedData = new HashMap<>();
                            updatedData.put("group_name", updatedName);  // 新名稱
                            updatedData.put("group_image", (chocie == 1) ? imageBase64 : "123");


                            db.collection("users")
                                    .document(userId)
                                    .collection("group")
                                    .document(groid)
                                    .set(updatedData, SetOptions.merge())  // 僅更新指定欄位
                                    .addOnSuccessListener(aVoid -> Log.d("Firestore", "已更新自己群組資料"))
                                    .addOnFailureListener(e -> Log.e("Firestore", "更新自己群組失敗：" + e.getMessage()));

// 更新每個受邀使用者的同一份群組資料
                            for (String invitedUid : invitedUsers) {
                                db.collection("users")
                                        .document(invitedUid)
                                        .collection("group")
                                        .document(groid)
                                        .set(updatedData, SetOptions.merge())  // 同樣只更新 group_name 和 group_image
                                        .addOnSuccessListener(aVoid -> Log.d("Firestore", "已更新 " + invitedUid + " 的群組資料"))
                                        .addOnFailureListener(e -> Log.e("Firestore", "更新失敗：" + invitedUid + " 原因：" + e.getMessage()));
                            }

                            Toast.makeText(SingleGroupManageActivity.this, "群組更新成功", Toast.LENGTH_SHORT).show();
                            goToGroupFragment();

                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "檢查群組名稱時錯誤：" + e.getMessage());
                        Toast.makeText(SingleGroupManageActivity.this, "檢查群組時發生錯誤", Toast.LENGTH_SHORT).show();
                    });

            tvGroupTitle.setText(updatedName);
            Toast.makeText(context, "群組設定已儲存", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupImagePicker() {
//        imagePickerLauncher = registerForActivityResult(
//                new ActivityResultContracts.StartActivityForResult(),
//                result -> {
//                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
//                        Uri imageUri = result.getData().getData();
//                        imgGroupPhoto.setImageURI(imageUri);
//                    }
//                });
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                            chocie=1;
                            imgGroupPhoto.setImageBitmap(bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(SingleGroupManageActivity.this, "選取圖片失敗", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

//    private void loadGroupMembers() {
//        List<String> members = Arrays.asList("Alice", "Bob", "Charlie");
//        memberContainer.removeAllViews();
//        for (String name : members) {
//            LinearLayout memberRow = new LinearLayout(context);
//            memberRow.setOrientation(LinearLayout.HORIZONTAL);
//            memberRow.setPadding(0, 8, 0, 8);
//
//            TextView memberName = new TextView(context);
//            memberName.setText(name);
//            memberName.setTextSize(16f);
//            memberName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
//
//            ImageView removeIcon = new ImageView(context);
//            removeIcon.setImageResource(android.R.drawable.ic_delete);
//            removeIcon.setPadding(16, 0, 16, 0);
//            removeIcon.setOnClickListener(v -> showRemoveMemberDialog(name, memberRow));
//
//            memberRow.addView(memberName);
//            memberRow.addView(removeIcon);
//            memberContainer.addView(memberRow);
//        }
//    }
    private void loadGroupMembers() {
        memberContainer.removeAllViews();

        SharedPreferences prefs = getSharedPreferences("login", MODE_PRIVATE);
        String uid = prefs.getString("userid", "0");


        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(uid)
                .collection("group")
                .document(groid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> members = (List<String>) documentSnapshot.get("members");
                        String  did=documentSnapshot.getId();
                        if (members != null && !members.isEmpty()) {
                            for (String name : members) {
                                LinearLayout memberRow = new LinearLayout(context);
                                memberRow.setOrientation(LinearLayout.HORIZONTAL);
                                memberRow.setPadding(0, 8, 0, 8);

                                TextView memberName = new TextView(context);
                                memberName.setText(name);
                                memberName.setTextSize(16f);
                                memberName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

                                ImageView removeIcon = new ImageView(context);
                                removeIcon.setImageResource(android.R.drawable.ic_delete);
                                removeIcon.setPadding(16, 0, 16, 0);
                                removeIcon.setOnClickListener(v -> showRemoveMemberDialog(did,name ,memberRow,members));

                                memberRow.addView(memberName);
                                memberRow.addView(removeIcon);
                                memberContainer.addView(memberRow);
                            }
                        } else {
                            Toast.makeText(context, "目前無成員資料", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(context, "群組資料不存在", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "讀取群組成員失敗：" + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("Firestore", "loadGroupMembers: ", e);
                });
    }



    private void showRemoveMemberDialog(String did,String name, View rowView,List<String> members) {
        new AlertDialog.Builder(context)
                .setTitle("移除成員")
                .setMessage("確定要將 \"" + name + "\" 移出群組嗎？")
                .setPositiveButton("確認", (dialog, which) -> {
                    memberContainer.removeView(rowView);

                    Toast.makeText(context, name + " 已被移除", Toast.LENGTH_SHORT).show();

                    for (String memberEmail : members) {
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(memberEmail)
                                .collection("group")
                                .document(did)
                                .update("members", FieldValue.arrayRemove(name))
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("Firestore", "成功從 " + memberEmail + " 的 group " + did + " 中移除 " + name);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Firestore", "移除失敗：" + e.getMessage());
                                });
                    }

                    // 再刪除該使用者自己在 group 裡的 document
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(name)
                            .collection("group")
                            .document(did)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Log.d("Firestore", "已刪除 " + name + " 的群組 " + did);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Firestore", "刪除失敗：" + e.getMessage());
                            });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showInviteDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.invite_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(context).setView(dialogView).create();

        EditText etInviteAccount = dialogView.findViewById(R.id.etInviteAccount);
        TextView tvSend = dialogView.findViewById(R.id.tvSend);
        TextView tvCancel = dialogView.findViewById(R.id.tvCancel);

        tvSend.setOnClickListener(v -> {
            String invitee = etInviteAccount.getText().toString().trim();

            if (!invitee.isEmpty()) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                SharedPreferences prefs = getSharedPreferences("login", MODE_PRIVATE);
                String userId = prefs.getString("userid", "0");

                db.collection("users")
                        .document(invitee)
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            if (snapshot.exists()) {
                                // 確認該帳號存在
                                db.collection("users")
                                        .document(userId)
                                        .collection("group")
                                        .document(groid)
                                        .get()
                                        .addOnSuccessListener(groupSnapshot -> {
                                            if (groupSnapshot.exists()) {
                                                List<String> members = (List<String>) groupSnapshot.get("members");
                                                if (members != null && members.contains(invitee)) {
                                                    Toast.makeText(context, "該帳號已加入", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    // 邀請流程
                                                    invitedUsers.add(invitee);

                                                    String baseDynamicLink = "https://fcunoteapp.page.link/";
                                                    String deepLink = "https://fcunoteapp.page.link/joinGroup?group=" + groupName + "&uid=" + invitee;
                                                    String inviteLink = baseDynamicLink +
                                                            "?link=" + Uri.encode(deepLink) +
                                                            "&apn=com.example.afinal" +
                                                            "&afl=https://example.com/fallback";

                                                    Map<String, Object> invitation = new HashMap<>();
                                                    invitation.put("group", groupName);
                                                    invitation.put("invited_by", userId);
                                                    invitation.put("timestamp", System.currentTimeMillis());

                                                    db.collection("invitations")
                                                            .document(invitee + "_" + groupName)
                                                            .set(invitation)
                                                            .addOnSuccessListener(aVoid -> {
                                                                sendEmailInvite(invitee, groupName, inviteLink);
                                                                Toast.makeText(context, "邀請已寄出：" + invitee, Toast.LENGTH_SHORT).show();
                                                                dialog.dismiss();
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                Toast.makeText(context, "邀請儲存失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                            });
                                                }
                                            }
                                        });
                            } else {
                                etInviteAccount.setError("查無此帳號");
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "檢查帳號失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                etInviteAccount.setError("請輸入帳號");
            }
        });



        tvCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }


    private void showExitDialog() {
        new AlertDialog.Builder(context)
                .setTitle("確定要退出群組嗎？")
                .setMessage("退出後將無法查看或編輯此群組")
                .setPositiveButton("確認", (dialog, which) -> {
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    SharedPreferences prefs = getSharedPreferences("login", Context.MODE_PRIVATE);
                    String userId = prefs.getString("userid", "0");
                    if (!userId.equals("0")) {
                        DocumentReference groupDocRef = db.collection("users")
                                .document(userId)
                                .collection("group")
                                .document(groid);

                        groupDocRef.get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        Map<String, Object> groupData = documentSnapshot.getData();
                                        if (groupData != null && groupData.containsKey("members")) {
                                            List<String> members = (List<String>) groupData.get("members");
                                            members.remove(userId);
                                            groupData.put("members", members);

                                            for (String uid : members) {
                                                db.collection("users")
                                                        .document(uid)
                                                        .collection("group")
                                                        .document(groid)
                                                        .update("members", members)
                                                        .addOnSuccessListener(unused -> Log.d("Firestore", "已從 " + uid + " 的群組中移除 userId"))
                                                        .addOnFailureListener(e -> Log.e("Firestore", "移除失敗：" + e.getMessage()));
                                            }
                                        }
                                    }

                                    groupDocRef.delete()
                                            .addOnSuccessListener(unused -> {
                                                Toast.makeText(context, "成功退出群組", Toast.LENGTH_SHORT).show();
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(context, "退出群組失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(context, "讀取群組資料失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }


    private void sendEmailInvite(String email, String groupName, String inviteLink) {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.emailjs.com/api/v1.0/email/send");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("origin", "http://localhost");
                conn.setDoOutput(true);

                String json = "{"
                        + "\"service_id\":\"Note\","
                        + "\"template_id\":\"template_Note_123456789\","
                        + "\"user_id\":\"U01lo_BOb5hFWje4D\","
                        + "\"template_params\":{"
                        +     "\"email\":\"" + email + "\","
                        +     "\"group_name\":\"" + groupName + "\","
                        +     "\"link\":\"" + inviteLink + "\""
                        + "}"
                        + "}";

                OutputStream os = conn.getOutputStream();
                os.write(json.getBytes(StandardCharsets.UTF_8));
                os.close();

                int responseCode = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream()
                ));

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                if (responseCode == 200) {
                    Log.d("EmailJS", "成功寄出邀請信: " + email);
                } else {
                    Log.e("EmailJS", "寄信失敗（代碼 " + responseCode + "）：\n" + response);
                }

            } catch (Exception e) {
                Log.e("EmailJS", "寄信過程錯誤: " + e.getMessage(), e);
            }
        }).start();
    }

    private void goToGroupFragment() {
        Intent intent = new Intent(this, MainActivity2.class);
        intent.putExtra("navigate_to", "group"); // 傳送要跳轉的 fragment 資訊
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        Log.d("IntentDebug", "goToGroupFragment() called,navigate_to = group");
        startActivity(intent);
        finish(); // 結束 JoinGroupActivity
    }
}