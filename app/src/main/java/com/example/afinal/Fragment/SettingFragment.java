package com.example.afinal.Fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.afinal.Fragment.Setting.SettingReminderFragment;
import com.example.afinal.MainActivity;
import com.example.afinal.R;
import com.example.afinal.SignInActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class SettingFragment extends Fragment {

    private ImageView ivAvatar;
    private ImageButton btnCamera, btnEdit;
    private TextView tvNickname;
    private TextView tvReminderAccounting;
    private TextView tvReminderState;
    private TextView tvaccountswitch;
    // 儲存使用者偏好設定
    private SharedPreferences prefs;

    private static final String PREF_NAME = "setting_prefs";
    private static final String KEY_NICKNAME = "nickname";
    private static final String KEY_AVATAR_URI = "avatar_uri";
    private static final String PREF_REMINDER_STATE = "reminder_state_prefs";

    // 拍照後儲存相片的 Uri 路徑
    private Uri cameraImageUri;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            //註冊從相簿選擇圖片的處理器
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    // 取得圖片的 Uri
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        // 儲存圖片路徑到 SharedPreferences
                        prefs.edit().putString(KEY_AVATAR_URI, imageUri.toString()).apply();
                        try {
                            // 讀取並顯示圖片
                            loadAvatar(imageUri);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "圖片載入失敗", Toast.LENGTH_SHORT).show();
                        }

                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        ivAvatar = view.findViewById(R.id.iv_avatar);
        btnCamera = view.findViewById(R.id.btn_camera);
        tvNickname = view.findViewById(R.id.tv_nickname);
        btnEdit = view.findViewById(R.id.btn_edit);
        tvReminderAccounting = view.findViewById(R.id.tv_accounting_reminder);
        tvReminderState = view.findViewById(R.id.tv_reminder_state);
        tvaccountswitch=view.findViewById(R.id.tv_account_switch);
        prefs = requireContext().getSharedPreferences(PREF_NAME, 0);

        // Reminder State
        prefs = requireContext().getSharedPreferences(PREF_REMINDER_STATE, 0);
        String mode = prefs.getString("reminder_mode", "未設定");
        String time = prefs.getString("reminder_time", "");
        tvReminderState.setText("提醒時間：" + mode + " " + time);

        // 顯示底部選單，選擇相簿/相機
        btnCamera.setOnClickListener(v -> {
            showBottomSheet();
        });
        tvaccountswitch.setOnClickListener(v-> {

            accountswitch();
        });

        // Edit 圖示功能：修改暱稱
        btnEdit.setOnClickListener(v -> {
            EditText input = new EditText(requireContext());
            input.setHint("輸入新暱稱");

            new AlertDialog.Builder(requireContext())
                    .setTitle("修改暱稱")
                    .setView(input)
                    .setPositiveButton("確認", (dialog, which) -> {
                        String newName = input.getText().toString().trim();
                        if (!newName.isEmpty()) {
                            tvNickname.setText(newName);
                            prefs.edit().putString(KEY_NICKNAME, newName).apply();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        // 讀取KEY_NICKNAME，default：暱稱
        String savedName = prefs.getString(KEY_NICKNAME, "暱稱");
        tvNickname.setText(savedName);

        // 讀取大頭貼 URI
        String uriStr = prefs.getString(KEY_AVATAR_URI, null);
        if (uriStr != null) {
            try {
                Uri uri = Uri.parse(uriStr);
                loadAvatar(uri);  // 此處若無法讀取，catch 會處理
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "無法載入頭像，已載入預設圖檔", Toast.LENGTH_SHORT).show();
                prefs.edit().remove(KEY_AVATAR_URI).apply();
                ivAvatar.setImageResource(R.drawable.ic_head_svg); // 預設圖示
            }
        }


        // 記帳提醒
        tvReminderAccounting.setOnClickListener(v -> {
            // 創建提醒 Fragment 實例
            SettingReminderFragment reminderFragment = new SettingReminderFragment();

            // 替換目前 Fragment 為提醒設定頁面
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_main, reminderFragment) // R.id.fragment_container 替換成實際 MainActivity的 FrameLayout ID
                    .addToBackStack(null) // 加入返回堆疊
                    .commit();
        });

        return view;
    }

    //BottomSheet
    private void showBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.avatar_bottom_sheet, null);
        dialog.setContentView(sheetView);

        TextView pickGallery = sheetView.findViewById(R.id.tv_pick_gallery);
        TextView takeCamera = sheetView.findViewById(R.id.tv_take_camera);

        pickGallery.setOnClickListener(v -> {
            dialog.dismiss();
            //相簿選取程式
            imagePickerLauncher.launch(new Intent(Intent.ACTION_PICK).setType("image/*"));
        });

        takeCamera.setOnClickListener(v -> {
            dialog.dismiss();
            //拍照程式
            takePhoto();
        });

        dialog.show();
    }

    private void takePhoto() {
        File photoFile = new File(requireContext().getCacheDir(), "camera_photo.jpg");
        cameraImageUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".provider",
                photoFile
        );

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        startActivityForResult(intent, 1002); // 你已經在 onActivityResult 有處理 UCrop 啦！
    }

    private void  accountswitch(){
        startActivity(new Intent(requireActivity(), SignInActivity.class));


    }

    private void loadAvatar(Uri uri) throws IOException {
        if (!"file".equals(uri.getScheme()) && !"content".equals(uri.getScheme())) {
            throw new SecurityException("Unsupported URI scheme");
        }

        if (uri == null) throw new IllegalArgumentException("URI 為 null");

        Bitmap bitmap;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.Source source = ImageDecoder.createSource(requireContext().getContentResolver(), uri);
            bitmap = ImageDecoder.decodeBitmap(source);
        } else {
            bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
        }
        ivAvatar.setImageBitmap(bitmap);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == getActivity().RESULT_OK) {
            if (requestCode == 1001 && data != null) {
                // 選圖成功 → 啟動裁切
                Uri sourceUri = data.getData();
                Uri destUri = Uri.fromFile(new File(requireContext().getCacheDir(), "cropped.jpg"));

                UCrop.Options options = new UCrop.Options();
                options.setFreeStyleCropEnabled(true);
                options.setCircleDimmedLayer(true); // 預覽為圓形
                options.setCompressionQuality(90);

                UCrop.of(sourceUri, destUri)
                        .withAspectRatio(1, 1)
                        .withMaxResultSize(600, 600)
                        .withOptions(options)
                        .start(requireActivity(), this);
            } else if (requestCode == UCrop.REQUEST_CROP && data != null) {
                Uri resultUri = UCrop.getOutput(data);
                if (resultUri != null) {
                    try {
                        Bitmap square = loadBitmapFromUri(resultUri);
                        Bitmap circle = cropToCircle(square);
                        ivAvatar.setImageBitmap(circle);

                        // 可選：儲存至快取並記錄 URI
                        Uri saved = saveBitmapToCache(circle);
                        prefs.edit().putString(KEY_AVATAR_URI, saved.toString()).apply();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private Bitmap loadBitmapFromUri(Uri uri) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.Source source = ImageDecoder.createSource(requireContext().getContentResolver(), uri);
            return ImageDecoder.decodeBitmap(source);
        } else {
            return MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
        }
    }

    private Bitmap cropToCircle(Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        BitmapShader shader = new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        Matrix matrix = new Matrix();
        float dx = (source.getWidth() - size) / 2f;
        float dy = (source.getHeight() - size) / 2f;
        matrix.setTranslate(-dx, -dy);
        shader.setLocalMatrix(matrix);

        paint.setShader(shader);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        return output;
    }


    private Uri saveBitmapToCache(Bitmap bitmap) throws IOException {
        File file = new File(requireContext().getCacheDir(), "avatar_cropped.jpg");
        FileOutputStream out = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        out.close();
        return Uri.fromFile(file);
    }

}