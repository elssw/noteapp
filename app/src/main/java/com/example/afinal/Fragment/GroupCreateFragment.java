package com.example.afinal.Fragment;

import static android.content.Context.MODE_PRIVATE;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.afinal.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class GroupCreateFragment extends Fragment {

    private ImageView imgGroupPhoto;
    private EditText etGroupName;
    private TextView tvInvitedUsers;
    private ArrayList<String> invitedUsers = new ArrayList<>();
    private Uri selectedImageUri;
    private int chocie=0;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    public GroupCreateFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        selectedImageUri = imageUri;
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), imageUri);
                            chocie=1;
                            imgGroupPhoto.setImageBitmap(bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "選取圖片失敗", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_create, container, false);

        imgGroupPhoto = view.findViewById(R.id.imgGroupPhoto);
        etGroupName = view.findViewById(R.id.etGroupName);
        tvInvitedUsers = view.findViewById(R.id.tvInvitedUsers);
        ImageButton btnInvite = view.findViewById(R.id.btnInvite);
        Button btnCreateGroup = view.findViewById(R.id.btnCreateGroup);

        imgGroupPhoto.setOnClickListener(v -> openImagePicker());

        btnInvite.setOnClickListener(v -> showInviteDialog());

//        btnCreateGroup.setOnClickListener(v -> {
//            String groupName = etGroupName.getText().toString().trim();
//            if (groupName.isEmpty()) {
//                etGroupName.setError("群組名稱不能為空");
//                return;
//            }
//
//            Bitmap bitmap;
//            if (imgGroupPhoto.getDrawable() instanceof BitmapDrawable) {
//                bitmap = ((BitmapDrawable) imgGroupPhoto.getDrawable()).getBitmap();
//            } else {
//                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.group_default_photo);
//            }
//
//            ByteArrayOutputStream stream = new ByteArrayOutputStream();
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 10, stream);
//            String imageBase64 = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT);
//            FirebaseFirestore db = FirebaseFirestore.getInstance();
//            SharedPreferences prefs = requireContext().getSharedPreferences("login",MODE_PRIVATE);
//            String userId = prefs.getString("userid", "0");
//
//            if (!userId.equals("0")) {
//                Map<String, Object> groupData = new HashMap<>();
//                groupData.put("group_name", groupName);
//                invitedUsers.add(userId);
//                groupData.put("members", invitedUsers);
//                if (chocie==1) {
//                    groupData.put("group_image", imageBase64);
//                } else {
//                    groupData.put("group_image","123");
//                }
//
//                db.collection("users")
//                        .document(userId)
//                        .collection("group")
//                        .document(groupName)  // 以群組名作為 document ID
//                        .set(groupData)
//                        .addOnSuccessListener(unused -> Log.d("Firestore", "群組建立成功"))
//                        .addOnFailureListener(e -> Log.e("Firestore", "群組建立失敗：" + e.getMessage()));
//
//                for (String invitedUid : invitedUsers) {
//                    db.collection("users")
//                            .document(invitedUid)  // 直接用 userId 存取
//                            .collection("group")
//                            .document(groupName)
//                            .set(groupData)
//                            .addOnSuccessListener(aVoid -> Log.d("Firestore", "已新增群組給：" + invitedUid))
//                            .addOnFailureListener(e -> Log.e("Firestore", "新增群組失敗：" + invitedUid ));
//                }
//
//
//
//            }
//
//            Fragment groupFragment = new GroupFragment();
//
//            requireActivity().getSupportFragmentManager()
//                    .beginTransaction()
//                    .replace(R.id.fragment_main, groupFragment)
//                    .commit();
//        });
        btnCreateGroup.setOnClickListener(v -> {
            String groupName = etGroupName.getText().toString().trim();
            if (groupName.isEmpty()) {
                etGroupName.setError("群組名稱不能為空");
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            SharedPreferences prefs = requireContext().getSharedPreferences("login", MODE_PRIVATE);
            String userId = prefs.getString("userid", "0");

            if (userId.equals("0")) return;

            // 檢查是否已存在相同群組名稱
            db.collection("users")
                    .document(userId)
                    .collection("group")
                    .document(groupName)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            etGroupName.setError("群組名稱已存在");
                        } else {
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
                            /*
                            invitedUsers.add(userId);  // 把建立者也加進去 members
                            groupData.put("members", invitedUsers);
                             */
                            // TODO:只加入建立者自己
                            ArrayList<String> creatorOnly = new ArrayList<>();
                            creatorOnly.add(userId);
                            groupData.put("members", creatorOnly);

                            groupData.put("group_image", (chocie == 1) ? imageBase64 : "123");

                            // 建立者資料夾下新增
                            db.collection("users")
                                    .document(userId)
                                    .collection("group")
                                    .document(groupName)
                                    .set(groupData);
/*
                            // 被邀請者也新增
                            for (String invitedUid : invitedUsers) {
                                db.collection("users")
                                        .document(invitedUid)
                                        .get()
                                        .addOnSuccessListener(documentSnapshot -> {
                                            if (documentSnapshot.exists()) {
                                                // 該 invitedUid 存在才新增群組資料
                                                db.collection("users")
                                                        .document(invitedUid)
                                                        .collection("group")
                                                        .document(groupName)
                                                        .set(groupData)
                                                        .addOnSuccessListener(aVoid ->
                                                                Log.d("Firestore", "已新增群組給：" + invitedUid)
                                                        )
                                                        .addOnFailureListener(e ->
                                                                Log.e("Firestore", "新增群組失敗：" + invitedUid + " 原因：" + e.getMessage())
                                                        );
                                            } else {
                                                Log.w("Firestore", "找不到使用者：" + invitedUid + "，未新增群組");
                                            }
                                        })
                                        .addOnFailureListener(e ->
                                                Log.e("Firestore", "檢查使用者失敗：" + invitedUid + " 原因：" + e.getMessage())
                                        );
                            }
*/
                            //TODO: 加入 Email 邀請信發送程式
                            List<String> successList = new ArrayList<>();
                            List<String> failList = new ArrayList<>();
                            AtomicInteger completedCount = new AtomicInteger(0);  // 統計完成數
                            int totalInvites = invitedUsers.size() - 1;  // 扣掉建立者自己
                            for (String invitedUid : invitedUsers) {
                                if (!invitedUid.equals(userId)) {
                                    db.collection("users")
                                            .document(invitedUid)
                                            .get()
                                            .addOnSuccessListener(snapshot -> {
                                                if (snapshot.exists()) {
                                                    String email = invitedUid;  //暫定，方便正常運作
                                                    if (email != null) {
                                                        String baseDynamicLink = "https://fcunoteapp.page.link/";
                                                        String deepLink = "https://fcunoteapp.page.link/joinGroup?group=" + groupName + "&uid=" + invitedUid;

                                                        String inviteLink = baseDynamicLink +
                                                                "?link=" + Uri.encode(deepLink) +
                                                                "&apn=com.example.afinal" +
                                                                "&afl=https://example.com/fallback";

                                                        // 寫入 invitations 資料
                                                        Map<String, Object> invitation = new HashMap<>();
                                                        invitation.put("group", groupName);
                                                        invitation.put("invited_by", userId); // 建立者
                                                        invitation.put("timestamp", System.currentTimeMillis());

                                                        db.collection("invitations")
                                                                .document(invitedUid + "_" + groupName)
                                                                .set(invitation)
                                                                .addOnSuccessListener(aVoid -> {
                                                                    sendEmailInvite(email, groupName, inviteLink);
                                                                    Log.d("EmailJS", "Sent invitation to: " + email);
                                                                    successList.add(invitedUid);
                                                                    checkCompletion(completedCount.incrementAndGet(), totalInvites, successList, failList, getContext());
                                                                })
                                                                .addOnFailureListener(e -> {
                                                                    Log.e("Firestore", "invite false:" + invitedUid + " Because:" + e.getMessage());
                                                                    failList.add(invitedUid);
                                                                    checkCompletion(completedCount.incrementAndGet(), totalInvites, successList, failList, getContext());
                                                                });
                                                    }else {
                                                        Log.w("EmailJS", "No email field found for user: " + invitedUid);
                                                        failList.add(invitedUid);
                                                        checkCompletion(completedCount.incrementAndGet(), totalInvites, successList, failList, getContext());
                                                    }
                                                }else {
                                                    Log.w("Firestore", "User does not exist: " + invitedUid);
                                                    failList.add(invitedUid);
                                                    checkCompletion(completedCount.incrementAndGet(), totalInvites, successList, failList, getContext());
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("Firestore", "Failed to fetch user: " + invitedUid + ", Reason: " + e.getMessage());
                                                failList.add(invitedUid);
                                                checkCompletion(completedCount.incrementAndGet(), totalInvites, successList, failList, getContext());
                                            });
                                }
                            }

                            Toast.makeText(getContext(), "群組建立成功", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "檢查群組名稱時錯誤：" + e.getMessage());
                        Toast.makeText(getContext(), "檢查群組時發生錯誤", Toast.LENGTH_SHORT).show();
                    });
            Fragment groupFragment = new GroupFragment();

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_main, groupFragment)
                    .commit();
        });
        updateInvitedList();

        return view;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void showInviteDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.invite_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext()).setView(dialogView).create();

        EditText etInviteAccount = dialogView.findViewById(R.id.etInviteAccount);
        TextView tvSend = dialogView.findViewById(R.id.tvSend);
        TextView tvCancel = dialogView.findViewById(R.id.tvCancel);

        tvSend.setOnClickListener(v -> {
            String invitee = etInviteAccount.getText().toString().trim();
            if (invitee.isEmpty()) {
                etInviteAccount.setError("請輸入帳號");
                return;
            }

            // 查詢 Firestore 確認該帳號是否存在
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .document(invitee)  // 注意這裡是直接用帳號當 document ID
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            if (!invitedUsers.contains(invitee)) {
                                invitedUsers.add(invitee);
                                updateInvitedList();
                                Toast.makeText(getContext(), "已將 " + invitee + " 加入邀請列", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), invitee + " 已在邀請列中", Toast.LENGTH_SHORT).show();
                            }
                            dialog.dismiss();
                        } else {
                            etInviteAccount.setError("查無此帳號");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "查詢失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        });
        tvCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }


    private void updateInvitedList() {
        if (invitedUsers.isEmpty()) {
            tvInvitedUsers.setText("邀請列：無");
        } else {
            StringBuilder sb = new StringBuilder("邀請列：");
            for (String user : invitedUsers) {
                sb.append("\n• ").append(user);
            }
            tvInvitedUsers.setText(sb.toString());
        }
    }


    public void sendEmailInvite(String email, String groupName, String link) {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.emailjs.com/api/v1.0/email/send");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("origin", "http://localhost"); // EmailJS 需要設定 origin
                conn.setDoOutput(true);

                // 這裡請替換成你自己在 EmailJS 控制台上的資料
                String json = "{"
                        + "\"service_id\":\"Note\","
                        + "\"template_id\":\"template_Note_123456789\","
                        + "\"user_id\":\"U01lo_BOb5hFWje4D\","
                        + "\"template_params\":{"
                        +     "\"email\":\"" + email + "\","
                        +     "\"group_name\":\"" + groupName + "\","
                        +     "\"link\":\"" + link + "\""
                        + "}"
                        + "}";

                Log.d("EmailJS", "Email length: " + email.length() + ", payload: " + json);

                // 寫出 request body
                OutputStream os = conn.getOutputStream();
                os.write(json.getBytes(StandardCharsets.UTF_8));
                os.close();

                int responseCode = conn.getResponseCode();

                // 讀取回應內容（無論成功或錯誤）
                InputStream is = (responseCode == HttpURLConnection.HTTP_OK)
                        ? conn.getInputStream() : conn.getErrorStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("EmailJS", "邀請信成功寄出！\n回應內容：" + response.toString());
                } else {
                    Log.e("EmailJS", "邀請信寄送失敗，代碼：" + responseCode + "\n錯誤回應：" + response.toString());
                }

            } catch (Exception e) {
                Log.e("EmailJS", "發送過程發生錯誤：" + e.getMessage(), e);
            }
        }).start();

    }


    // 放在 Fragment 類別中
    private void checkCompletion(int completed, int total, List<String> successList, List<String> failList, Context context) {
        if (completed == total) {
            Log.d("InviteResult", "成功邀請：" + successList);
            Log.d("InviteResult", "失敗邀請：" + failList);
            Toast.makeText(context, "成功：" + successList + "\n失敗：" + failList, Toast.LENGTH_LONG).show();
        }
    }


//    private void showInviteDialog() {
//        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.invite_dialog, null);
//        AlertDialog dialog = new AlertDialog.Builder(getContext()).setView(dialogView).create();
//
//        EditText etInviteAccount = dialogView.findViewById(R.id.etInviteAccount);
//        TextView tvSend = dialogView.findViewById(R.id.tvSend);
//        TextView tvCancel = dialogView.findViewById(R.id.tvCancel);
//
//        tvSend.setOnClickListener(v -> {
//            String invitee = etInviteAccount.getText().toString().trim();
//            if (invitee.isEmpty()) {
//                etInviteAccount.setError("請輸入帳號");
//                return;
//            }
//
//            // 驗證帳號是否存在於 users 集合中
//            FirebaseFirestore db = FirebaseFirestore.getInstance();
//            db.collection("users")
//                    .whereEqualTo("account", invitee)
//                    .get()
//                    .addOnSuccessListener(querySnapshot -> {
//                        if (!querySnapshot.isEmpty()) {
//                            if (!invitedUsers.contains(invitee)) {
//                                invitedUsers.add(invitee);           // 加入清單
//                                updateInvitedList();                 // 更新畫面
//                            }
//                            Toast.makeText(getContext(), "已送出邀請給：" + invitee, Toast.LENGTH_SHORT).show();
//                            dialog.dismiss();
//                        } else {
//                            etInviteAccount.setError("查無此帳號");
//                        }
//                    })
//                    .addOnFailureListener(e -> {
//                        Toast.makeText(getContext(), "查詢失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
//                    });
//        });
//
//        tvCancel.setOnClickListener(v -> ());
//
//        dialog.show();
//    }


}
