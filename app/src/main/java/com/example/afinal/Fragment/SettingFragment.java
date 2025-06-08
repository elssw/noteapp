package com.example.afinal.Fragment;

import android.app.AlertDialog;
import android.content.Context;
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
import android.util.Log;
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
import androidx.fragment.app.FragmentTransaction;

import com.example.afinal.Fragment.Setting.SettingReminderFragment;
import com.example.afinal.Fragment.Setting.SettingResetFragment;
import com.example.afinal.R;
import com.example.afinal.SignInActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.example.afinal.Fragment.Setting.Constants.*;

public class SettingFragment extends Fragment {

    private ImageView ivAvatar;
    private ImageButton btnCamera, btnEdit;
    private TextView tvNickname;
    private TextView tvReminderAccounting;
    private TextView tvReminderState;
    private TextView tvAccountSwitch;
    private TextView reset;
    // 儲存使用者偏好設定
    private SharedPreferences prefs;

    // 與 SettingReminderFragment 之 PREF_SETTING 相同
    private static final String PREF_SETTING = "setting_prefs";
    private static final String KEY_NICKNAME = "nickname";
    private static final String KEY_AVATAR_URI = "avatar_uri";

    // 拍照後儲存相片的 Uri 路徑
    private Uri cameraImageUri;

    private static final String TAG = "SettingDebug";

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
//        tvReminderState = view.findViewById(R.id.tv_reminder_state);
        tvAccountSwitch=view.findViewById(R.id.tv_account_switch);
        reset=view.findViewById(R.id.tv_reset_accounting_setting);
        prefs = requireContext().getSharedPreferences(PREF_SETTING, 0);


        // 載入頭像
        String uriStr = prefs.getString(KEY_AVATAR_URI, null);
        Log.d(TAG, "prefs URI = " + uriStr);
        if (uriStr != null && !uriStr.isEmpty()) {
            Log.d(TAG, "uriStr");

            try {
                Uri uri = Uri.parse(uriStr);
                loadAvatar(uri);
            } catch (Exception e) {
                Log.d(TAG, "ic_head_svg");
                e.printStackTrace();
                Toast.makeText(getContext(), "無法載入頭像，已載入預設圖檔", Toast.LENGTH_SHORT).show();
                prefs.edit().remove(KEY_AVATAR_URI).apply();
                // 若 uriStr 為 null 或空字串，給預設圖
                ivAvatar.setImageResource(R.drawable.ic_head_svg);
            }
        } else {
            // 若 uriStr 為 null 或空字串，給預設圖
            Log.d(TAG, "No, ic_head_svg");
            ivAvatar.setImageResource(R.drawable.ic_head_svg);
        }


        // 顯示底部選單，選擇相簿/相機
        btnCamera.setOnClickListener(v -> {
            Log.d(TAG, "btnCamera");
            showBottomSheet();
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
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            SharedPreferences prefs = requireContext().getSharedPreferences("login", Context.MODE_PRIVATE);
                            String userId = prefs.getString("userid", "0");
                            if (!userId.equals("0")) {
                                db.collection("users")
                                        .document(userId)
                                        .update("nickname", newName)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d("Firestore", "second_name 更新成功");
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("Firestore", "更新 second_name 失敗: " + e.getMessage());
                                        });
                            }
                            tvNickname.setText(newName);
                            prefs.edit().putString(KEY_NICKNAME, newName).apply();


                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
        FirebaseFirestore dba = FirebaseFirestore.getInstance();
        SharedPreferences prefs = requireContext().getSharedPreferences("login", Context.MODE_PRIVATE);
        String userId = prefs.getString("userid", "0");
        if (!userId.equals("0")) {
            dba.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("nickname");
                            if (name != null) {
                                tvNickname.setText(name);
                            } else {
                                Log.d("Firestore", "second_name 欄位不存在");
                            }
                        } else {
                            Log.d("Firestore", "文件不存在");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "讀取 second_name 失敗: " + e.getMessage());
                    });
        }
        else {
            // 讀取 KEY_NICKNAME，default：暱稱
            String savedName = prefs.getString(KEY_NICKNAME, "暱稱");
            tvNickname.setText(savedName);
        }

        // 提醒狀態
        String mode = prefs.getString(KEY_REMINDER_MODE, "尚未設定");
        String time = prefs.getString(KEY_REMINDER_TIME, "");
       // tvReminderState.setText("提醒時間：" + mode + " " + time);

        // 帳號切換
        tvAccountSwitch.setOnClickListener(v-> {
            accountSwitch();
        });
        reset.setOnClickListener(v -> {
            SettingResetFragment re= new SettingResetFragment ();

            // 替換目前 Fragment 為提醒設定頁面
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_main, re) // R.id.fragment_container 替換成實際 MainActivity的 FrameLayout ID
                    .addToBackStack(null) // 加入返回堆疊
                    .commit();

        });

        // 記帳提醒
        tvReminderAccounting.setOnClickListener(v -> {
            Log.d(TAG, "Go to SettingReminderFragment.");

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

    // 從相簿選取圖片後該做什麼？
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            // 註冊從相簿選擇圖片的處理器
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    // 取得圖片的 Uri
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        startCrop(imageUri);
                    }
                }
            });

    // BottomSheet: from photo or camera
    private void showBottomSheet() {
        Log.d(TAG, "showBottomSheet");

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.avatar_bottom_sheet, null);
        dialog.setContentView(sheetView);

        TextView tvPickGallery = sheetView.findViewById(R.id.tv_pick_gallery);
        TextView tvTakeCamera = sheetView.findViewById(R.id.tv_take_camera);

        tvPickGallery.setOnClickListener(v -> {
            dialog.dismiss();
            //相簿
            imagePickerLauncher.launch(new Intent(Intent.ACTION_PICK).setType("image/*"));
        });

        tvTakeCamera.setOnClickListener(v -> {
            dialog.dismiss();
            //拍照
            takePhoto();
        });

        dialog.show();
    }

    // camera
    private void takePhoto() {
        Log.d(TAG, "takePhoto");

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

    // 根據 API 版本，使用不同方式載入圖片
    private void loadAvatar(Uri uri) throws IOException {
        Log.d(TAG, "loadAvatar");

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

    //處理從相簿選圖後的裁切
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (resultCode == getActivity().RESULT_OK) {
            if (requestCode == 1001 && data != null) {
                Log.d(TAG, "1001 photo get, cutting");
                Uri sourceUri = data.getData();
                startCrop(sourceUri);

            } else if (requestCode == 1002) {
                Log.d(TAG, "1002 photo get, cutting");
                Uri sourceUri = cameraImageUri;
                startCrop(sourceUri);
            } else if (requestCode == UCrop.REQUEST_CROP && data != null) {
                Uri resultUri = UCrop.getOutput(data);
                Log.d(TAG, "cut finished, URI = " + resultUri);
                if (resultUri != null) {
                    try {
                        Bitmap square = loadBitmapFromUri(resultUri);
                        Bitmap circle = cropToCircle(square);
                        ivAvatar.setImageBitmap(circle);

                        Uri saved = saveBitmapToCache(circle);
                        prefs.edit().putString(KEY_AVATAR_URI, saved.toString()).apply();
                        Log.d(TAG, "cut finished, store to " + saved.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "false", e);
                    }
                }
            }
        }else {
            Log.w(TAG, "onActivityResult: false");
        }
    }

    // 開始裁切
    private void startCrop(Uri sourceUri) {
        Uri destUri = Uri.fromFile(new File(requireContext().getCacheDir(), "cropped.jpg"));
        UCrop.of(sourceUri, destUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(600, 600)
                .withOptions(getCropOptions())
                .start(requireContext(), this);
    }

    // 裁切樣式設定函式
    private UCrop.Options getCropOptions() {
        UCrop.Options options = new UCrop.Options();
        options.setFreeStyleCropEnabled(true);
        options.setCircleDimmedLayer(true); // 圓形預覽
        options.setCompressionQuality(90);
        return options;
    }


    private Bitmap loadBitmapFromUri(Uri uri) throws IOException {
        Log.d(TAG, "loadBitmapFromUri");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.Source source = ImageDecoder.createSource(requireContext().getContentResolver(), uri);
            return ImageDecoder.decodeBitmap(source);
        } else {
            return MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
        }
    }

    // 把方形圖片裁切成圓形的樣式
    private Bitmap cropToCircle(Bitmap source) {
        Log.d(TAG, "cropToCircle");

        // 修正關鍵：避免硬體加速 bitmap 導致崩潰
        if (source.getConfig() == Bitmap.Config.HARDWARE) {
            source = source.copy(Bitmap.Config.ARGB_8888, true);
        }

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

    // 把裁切後的圖片暫時存到快取中並回傳 URI
    private Uri saveBitmapToCache(Bitmap bitmap) throws IOException {
        Log.d(TAG, "saveBitmapToCache");

        File file = new File(requireContext().getCacheDir(), "avatar_cropped.png");
        FileOutputStream out = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
        out.close();
        return Uri.fromFile(file);
    }

    // 切換帳號介面
    private void  accountSwitch(){
        startActivity(new Intent(requireActivity(), SignInActivity.class));
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: update reminder time");

        String mode = prefs.getString(KEY_REMINDER_MODE, "尚未設定");
        String time = prefs.getString(KEY_REMINDER_TIME, "");
        //tvReminderState.setText("提醒時間：" + mode + " " + time);
    }

}