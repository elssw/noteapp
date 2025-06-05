package com.example.afinal.Fragment;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
                            // 🔽 圖片處理
                            Bitmap bitmap;
                            if (imgGroupPhoto.getDrawable() instanceof BitmapDrawable) {
                                bitmap = ((BitmapDrawable) imgGroupPhoto.getDrawable()).getBitmap();
                            } else {
                                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.group_default_photo);
                            }
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 10, stream);
                            String imageBase64 = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT);

                            // 🔽 建立群組資料
                            Map<String, Object> groupData = new HashMap<>();
                            groupData.put("group_name", groupName);
                            invitedUsers.add(userId);  // 把建立者也加進去 members
                            groupData.put("members", invitedUsers);
                            groupData.put("group_image", (chocie == 1) ? imageBase64 : "123");

                            // 建立者資料夾下新增
                            db.collection("users")
                                    .document(userId)
                                    .collection("group")
                                    .document(groupName)
                                    .set(groupData);

                            // 被邀請者也新增
                            for (String invitedUid : invitedUsers) {
                                db.collection("users")
                                        .document(invitedUid)
                                        .get()
                                        .addOnSuccessListener(documentSnapshot -> {
                                            if (documentSnapshot.exists()) {
                                                // ✅ 該 invitedUid 存在才新增群組資料
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
            if (!invitee.isEmpty()) {
                if (!invitedUsers.contains(invitee)) {
                                invitedUsers.add(invitee);           // ✅ 加入清單
                                updateInvitedList();                 // ✅ 更新畫面
                }
                Toast.makeText(getContext(), "已送出邀請給：" + invitee, Toast.LENGTH_SHORT).show();

                dialog.dismiss();
            } else {
                etInviteAccount.setError("請輸入帳號");
            }
        });

        tvCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }


    private void updateInvitedList() {
        if (invitedUsers.isEmpty()) {
            tvInvitedUsers.setText("已邀請：無");
        } else {
            StringBuilder sb = new StringBuilder("已邀請：");
            for (String user : invitedUsers) {
                sb.append("\n• ").append(user);
            }
            tvInvitedUsers.setText(sb.toString());
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
//                                invitedUsers.add(invitee);           // ✅ 加入清單
//                                updateInvitedList();                 // ✅ 更新畫面
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
