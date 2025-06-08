package com.example.afinal;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Map;

public class JoinGroupActivity extends AppCompatActivity {
    FirebaseFirestore db;
    String invitedUid, groupName;
    String inviterId;
    Map<String, Object> groupData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_group);
        db = FirebaseFirestore.getInstance();

        TextView tvInfo = findViewById(R.id.tvJoinGroupInfo);
        Button btnJoin = findViewById(R.id.btnJoinGroup);
        Button btnReject = findViewById(R.id.btnReject);

        Uri data = getIntent().getData();
        if (data != null) {
            groupName = data.getQueryParameter("group");
            invitedUid = data.getQueryParameter("uid");

            if (groupName != null && invitedUid != null) {
                tvInfo.setText("你被邀請加入群組「" + groupName + "」，是否確認加入？");
                prefetchGroupData(); // 預先讀資料等按下加入再用
            } else {
                Toast.makeText(this, "連結參數錯誤", Toast.LENGTH_SHORT).show();
            }
        }

        btnJoin.setOnClickListener(v -> {
            if (groupData == null || inviterId == null) {
                Toast.makeText(this, "資料尚未讀取完成", Toast.LENGTH_SHORT).show();
                return;
            }

            // 加入群組流程
            ArrayList<String> members;
            Object membersObj = groupData.get("members");
            if (membersObj instanceof ArrayList) {
                members = (ArrayList<String>) membersObj;
            } else {
                members = new ArrayList<>();
            }

            if (!members.contains(invitedUid)) {
                members.add(invitedUid);

                // 更新 inviter 群組 members
                db.collection("users").document(inviterId)
                        .collection("group").document(groupName)
                        .update("members", members);

                // 寫入 invited user 的 group 資料
                db.collection("users").document(invitedUid)
                        .collection("group").document(groupName)
                        .set(groupData);

                // 刪除 invitation
                db.collection("invitations")
                        .document(invitedUid + "_" + groupName)
                        .delete();

                Toast.makeText(this, "已成功加入群組", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "你已在群組中", Toast.LENGTH_SHORT).show();
            }

            closeJoinGroupActivity();
        });

        btnReject.setOnClickListener(v -> {
            Toast.makeText(this, "已拒絕加入群組", Toast.LENGTH_SHORT).show();
            db.collection("invitations")
                    .document(invitedUid + "_" + groupName)
                    .delete(); // 可選：拒絕也可清除邀請紀錄
            closeJoinGroupActivity();
        });
    }

    private void prefetchGroupData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // 狀況 1：未登入
        if (currentUser == null) {
            Toast.makeText(this, "請先登入帳號", Toast.LENGTH_SHORT).show();
            goToSignInActivity();
            return;
        }

        String currentEmail = currentUser.getEmail(); // 目前登入者的 email
        Log.d("CheckUID", "currentEmail = [" + currentEmail + "]");
        Log.d("CheckUID", "invitedUid = [" + invitedUid + "]");

        // 狀況 2：登入的 email ≠ 被邀請的 email
        if (!currentEmail.equals(invitedUid)) {
            Toast.makeText(this, "無權使用此邀請連結，請使用正確帳號登入", Toast.LENGTH_SHORT).show();
            closeJoinGroupActivity();
            return;
        }

        // 狀況 3 & 4：目前帳號合法，開始查 invitation
        db.collection("invitations")
                .document(invitedUid + "_" + groupName)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        // invitation 不存在
                        Toast.makeText(this, "找不到邀請紀錄，請確認邀請是否有效", Toast.LENGTH_SHORT).show();
                        closeJoinGroupActivity();
                        return;
                    }

                    inviterId = snapshot.getString("invited_by");
                    if (inviterId == null) {
                        Toast.makeText(this, "邀請資料錯誤", Toast.LENGTH_SHORT).show();
                        closeJoinGroupActivity();
                        return;
                    }

                    // 查詢 inviter 建立的群組資料
                    db.collection("users").document(inviterId)
                            .collection("group").document(groupName)
                            .get()
                            .addOnSuccessListener(doc -> {
                                if (!doc.exists()) {
                                    Toast.makeText(this, "無法取得群組資料", Toast.LENGTH_SHORT).show();
                                    closeJoinGroupActivity();
                                    return;
                                }

                                groupData = doc.getData();
                                Object membersObj = groupData.get("members");

                                // 狀況 4：你已在群組中
                                if (membersObj instanceof ArrayList) {
                                    ArrayList<String> members = (ArrayList<String>) membersObj;
                                    if (members.contains(invitedUid)) {
                                        Toast.makeText(this, "已在群組中", Toast.LENGTH_SHORT).show();
                                        closeJoinGroupActivity();
                                        return;
                                    }
                                }

                                // 狀況 5：一切正確，等待使用者點按「加入」按鈕
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "讀取群組資料失敗", Toast.LENGTH_SHORT).show();
                                closeJoinGroupActivity();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "讀取邀請資料失敗", Toast.LENGTH_SHORT).show();
                    closeJoinGroupActivity();
                });
    }



    private void closeJoinGroupActivity() {
        finish(); // 關閉 JoinGroupActivity
    }

    private void goToSignInActivity() {
        Intent intent = new Intent(this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // 清除返回堆疊
        startActivity(intent);
        finish(); // 關閉目前畫面
    }

}
