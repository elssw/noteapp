package com.example.afinal.Fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GroupCreateFragment extends Fragment {

    private ImageView imgGroupPhoto;
    private EditText etGroupName;
    private TextView tvInvitedUsers;
    private ArrayList<String> invitedUsers = new ArrayList<>();
    private Uri selectedImageUri;

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

        btnCreateGroup.setOnClickListener(v -> {
            String groupName = etGroupName.getText().toString().trim();
            if (groupName.isEmpty()) {
                etGroupName.setError("群組名稱不能為空");
                return;
            }

            Bitmap bitmap;
            if (imgGroupPhoto.getDrawable() instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) imgGroupPhoto.getDrawable()).getBitmap();
            } else {
                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.group_default_photo);
            }

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
            String imageBase64 = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT);

            Bundle result = new Bundle();
            result.putString("new_group_name", groupName);
            result.putString("new_group_image", imageBase64);

            Fragment groupFragment = new GroupFragment();
            groupFragment.setArguments(result);

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
                Toast.makeText(getContext(), "已送出邀請給：" + invitee, Toast.LENGTH_SHORT).show();
                // TODO: 呼叫邀請 API
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
}
