package com.example.afinal.Fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import com.example.afinal.R;

import java.util.Arrays;
import java.util.List;

public class SingleGroupManageFragment extends Fragment {

    private static final String ARG_GROUP_NAME = "group_name";
    private String groupName;

    private TextView tvGroupTitle;
    private ImageView imgGroupPhoto;
    private EditText etGroupName;
    private LinearLayout memberContainer;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    public SingleGroupManageFragment() {}

    public static SingleGroupManageFragment newInstance(String groupName) {
        SingleGroupManageFragment fragment = new SingleGroupManageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_GROUP_NAME, groupName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            groupName = getArguments().getString(ARG_GROUP_NAME);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_single_group_manage, container, false);

        tvGroupTitle = view.findViewById(R.id.tvGroupTitle);
        imgGroupPhoto = view.findViewById(R.id.imgGroupPhoto);
        etGroupName = view.findViewById(R.id.etGroupName);
        memberContainer = view.findViewById(R.id.memberContainer);
        Button btnInviteMember = view.findViewById(R.id.btnInviteMember);
        Button btnExitGroup = view.findViewById(R.id.btnExitGroup);
        TextView btnEditPhoto = view.findViewById(R.id.btnEditPhoto);

        if (groupName != null) {
            tvGroupTitle.setText(groupName);
            etGroupName.setText(groupName);
        }

        setupImagePicker();
        loadGroupMembers();

        btnEditPhoto.setOnClickListener(v -> openImagePicker());
        btnInviteMember.setOnClickListener(v -> showInviteDialog());
        btnExitGroup.setOnClickListener(v -> showExitDialog());

        Button btnSaveSettings = view.findViewById(R.id.btnSaveSettings);

        btnSaveSettings.setOnClickListener(v -> {
            String updatedName = etGroupName.getText().toString().trim();
            if (updatedName.isEmpty()) {
                etGroupName.setError("群組名稱不可為空");
                return;
            }

            // TODO: 呼叫 API 儲存群組名稱、圖片...

            tvGroupTitle.setText(updatedName);

            // 👉 發出通知
            Bundle result = new Bundle();
            result.putBoolean("shouldRefresh", true);
            getParentFragmentManager().setFragmentResult("refreshGroupList", result);

            Toast.makeText(getContext(), "群組設定已儲存", Toast.LENGTH_SHORT).show();
        });


        return view;
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        imgGroupPhoto.setImageURI(imageUri);
                        // TODO: 可呼叫上傳 API 儲存圖片
                    }
                });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void loadGroupMembers() {
        List<String> members = Arrays.asList("Alice", "Bob", "Charlie"); // 假資料，可改成後端回傳

        memberContainer.removeAllViews();
        for (String name : members) {
            LinearLayout memberRow = new LinearLayout(getContext());
            memberRow.setOrientation(LinearLayout.HORIZONTAL);
            memberRow.setPadding(0, 8, 0, 8);

            TextView memberName = new TextView(getContext());
            memberName.setText(name);
            memberName.setTextSize(16f);
            memberName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            ImageView removeIcon = new ImageView(getContext());
            removeIcon.setImageResource(android.R.drawable.ic_delete);
            removeIcon.setPadding(16, 0, 16, 0);
            removeIcon.setOnClickListener(v -> showRemoveMemberDialog(name, memberRow));

            memberRow.addView(memberName);
            memberRow.addView(removeIcon);

            memberContainer.addView(memberRow);
        }
    }

    private void showRemoveMemberDialog(String name, View rowView) {
        new AlertDialog.Builder(getContext())
                .setTitle("移除成員")
                .setMessage("確定要將 \"" + name + "\" 移出群組嗎？")
                .setPositiveButton("確認", (dialog, which) -> {
                    memberContainer.removeView(rowView);
                    Toast.makeText(getContext(), name + " 已被移除", Toast.LENGTH_SHORT).show();
                    // TODO: 呼叫 API 通知後端移除該成員
                })
                .setNegativeButton("取消", null)
                .show();
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
                Toast.makeText(getContext(), "已送出邀請給：" + invitee, Toast.LENGTH_SHORT).show();
                // TODO: 呼叫邀請成員 API
                dialog.dismiss();
            } else {
                etInviteAccount.setError("請輸入帳號");
            }
        });

        tvCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
    
    private void showExitDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("確定要退出群組嗎？")
                .setMessage("退出後將無法查看或編輯此群組")
                .setPositiveButton("確認", (dialog, which) -> {
                    Toast.makeText(getContext(), "已退出群組", Toast.LENGTH_SHORT).show();

                    // 👉 發送通知給前一個 Fragment
                    Bundle result = new Bundle();
                    result.putBoolean("shouldRefresh", true);
                    getParentFragmentManager().setFragmentResult("refreshGroupList", result);

                    // 👉 返回上一頁
                    requireActivity().getSupportFragmentManager().popBackStack();

                    // TODO: 呼叫 API 更新狀態或從資料庫移除自己
                })
                .setNegativeButton("取消", null)
                .show();
    }

}
