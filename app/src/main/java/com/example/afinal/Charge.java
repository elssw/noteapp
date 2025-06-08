package com.example.afinal;


import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.afinal.model.Category;

import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.example.afinal.model.LocalRecord;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.gms.common.api.Status;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;


public class Charge extends AppCompatActivity {
    private static final int MAX_LEN = 20;
    private String userId = "0";
    private static final int REQUEST_CODE_PICK_IMAGE = 1001;

    private TextView tvAmountDisplay, tvNote, tvDateDisplay;
    private Button btnImageUpload;
    private int selectedIconRes;
    private String selectedCategoryName;
    private StringBuilder amountBuilder = new StringBuilder();
    private int uploadedImageCount = 0;
    private final List<Uri> uploadedImageUris = new ArrayList<>();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_charge);

        SharedPreferences loginPrefs = getSharedPreferences("login", MODE_PRIVATE);
        userId = getIntent().getStringExtra("userId");
        if (userId == null || userId.isEmpty()) {
            userId = "0"; // 明確未登入
        }


        /*if (!userId.equals("0")) {
            syncLocalRecordsIfLoggedIn(userId);
        }*/

        Intent i = getIntent();
        boolean isEdit = i.getBooleanExtra("edit", false);
        int position = i.getIntExtra("position", -1);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (v, insets) -> {
                    Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
                    return insets;
                });

        tvAmountDisplay = findViewById(R.id.tvAmountDisplay);
        tvNote = findViewById(R.id.note);
        tvDateDisplay = findViewById(R.id.tvDateDisplay);
        tvAmountDisplay.setText("NT$0");

        setupLocateClick();

        Calendar cal = Calendar.getInstance();
        String today = String.format("%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
        tvDateDisplay.setText(today);
        tvDateDisplay.setOnClickListener(v -> showDatePickerDialog(tvDateDisplay));

        tvNote.setOnClickListener(v -> {
            TextView title = new TextView(this);
            title.setText("備註");
            title.setGravity(Gravity.CENTER);
            title.setTextSize(20);
            EditText input = new EditText(this);
            input.setHint("請輸入備註");
            AlertDialog dlg = new AlertDialog.Builder(this)
                    .setCustomTitle(title)
                    .setView(input)
                    .setPositiveButton("確定", (d, w) -> {
                        String t = input.getText().toString().trim();
                        if (!t.isEmpty()) tvNote.setText(t);
                    })
                    .setNegativeButton("取消", null)
                    .create();
            dlg.show();
        });

        RecyclerView rv = findViewById(R.id.items);
        rv.setLayoutManager(new GridLayoutManager(this, 4));
        List<Category> cats = Arrays.asList(
                new Category(R.drawable.breakfast, "早餐"),
                new Category(R.drawable.lunch, "午餐"),
                new Category(R.drawable.snack, "點心"),
                new Category(R.drawable.dinner, "晚餐"),
                new Category(R.drawable.drinking, "飲料"),
                new Category(R.drawable.eatelse, "其他吃喝"),
                new Category(R.drawable.entertain, "娛樂"),
                new Category(R.drawable.gift, "禮物"),
                new Category(R.drawable.shopping, "購物"),
                new Category(R.drawable.traffic, "交通"),
                new Category(R.drawable.life, "生活"),
                new Category(R.drawable.learning, "學習"),
                new Category(R.drawable.rent, "租金"),
                new Category(R.drawable.pet, "寵物"),
                new Category(R.drawable.borrow, "借出"),
                new Category(R.drawable.more, "其他")
        );
        CategoryAdapter catAdapter = new CategoryAdapter(this, cats);
        catAdapter.setOnItemClickListener(c -> {
            selectedIconRes = c.getIconResId();
            selectedCategoryName = c.getName();
        });
        rv.setAdapter(catAdapter);

        selectedIconRes = cats.get(0).getIconResId();
        selectedCategoryName = cats.get(0).getName();

        btnImageUpload = findViewById(R.id.image);
        btnImageUpload.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(Intent.createChooser(intent, "選擇圖片"), REQUEST_CODE_PICK_IMAGE);

        });

        int[] ids = {
                R.id.number_1, R.id.number_2, R.id.number_3,
                R.id.number_4, R.id.number_5, R.id.number_6,
                R.id.number_7, R.id.number_8, R.id.number_9,
                R.id.number_0, R.id.point,
                R.id.plus, R.id.subtract, R.id.multiply, R.id.divide
        };
        for (int id : ids) findViewById(id).setOnClickListener(this::onDigitClick);
        findViewById(R.id.clear).setOnClickListener(this::onClearClick);
        findViewById(R.id.back).setOnClickListener(this::onBackClick);
        findViewById(R.id.equal).setOnClickListener(this::onEqualClick);

        if (isEdit) {
            selectedIconRes = i.getIntExtra("iconRes", R.drawable.ic_add);
            selectedCategoryName = i.getStringExtra("categoryName");
            String price = i.getStringExtra("price");
            String note = i.getStringExtra("note");
            String date = i.getStringExtra("date");
            String location = i.getStringExtra("location");

            amountBuilder.setLength(0);
            if (price != null) {
                String clean = price.replaceAll("[^\\d.]", "");
                clean = clean.replaceFirst("^0+(?!$)", "");
                amountBuilder.append(clean);
            }
            if (amountBuilder.length() > 0)
                tvAmountDisplay.setText("NT$" + amountBuilder);

            if (note != null && !note.isEmpty()) tvNote.setText(note);
            if (date != null && !date.isEmpty()) tvDateDisplay.setText(date);
            EditText etLocate = findViewById(R.id.locate);
            if (location != null && !location.isEmpty()) etLocate.setText(location);
        }

        findViewById(R.id.confirm).setOnClickListener(v -> {
            autoEvaluateIfNeeded();

            EditText etLocate = findViewById(R.id.locate);
            final String location = etLocate.getText().toString();
            String priceRaw = tvAmountDisplay.getText().toString().replaceAll("[^\\d.]", "");
            final String price = priceRaw.replaceFirst("^0+(?!$)", "");

            List<String> imageUriStrings = new ArrayList<>();
            for (Uri uri : uploadedImageUris) {
                imageUriStrings.add(uri.toString());
            }

            sendBackResult(imageUriStrings, location, price);
        });



    }

    private void autoEvaluateIfNeeded() {
        String expr = amountBuilder.toString();
        if (expr.matches(".*[+\\-*/×÷].*")) {
            onEqualClick(null);
        }
    }

    private void sendBackResult(List<String> imageUrls, String location, String price) {
        Intent result = new Intent();
        result.putExtra("iconRes", selectedIconRes);
        result.putExtra("categoryName", selectedCategoryName);
        result.putExtra("price", "NT$" + price);
        result.putExtra("note", tvNote.getText().toString());
        result.putExtra("date", tvDateDisplay.getText().toString());
        result.putExtra("location", location);
        result.putExtra("imageCount", imageUrls.size());
        result.putStringArrayListExtra("imageUrls", new ArrayList<>(imageUrls));
        result.putExtra("edit", getIntent().getBooleanExtra("edit", false));
        result.putExtra("position", getIntent().getIntExtra("position", -1));
        setResult(RESULT_OK, result);
        finish();
    }


    private void saveLocally(LocalRecord record) {
        SharedPreferences prefs = getSharedPreferences("local_records", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("records", "[]");
        List<LocalRecord> records = new Gson().fromJson(json, new TypeToken<List<LocalRecord>>(){}.getType());
        records.add(record);
        prefs.edit().putString("records", gson.toJson(records)).apply();
    }


    private void uploadToFirebase(LocalRecord record, String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("amount", record.amount);
        data.put("note", record.note);
        data.put("category", record.categoryName);
        data.put("iconRes", record.iconRes);
        data.put("location", record.location);
        data.put("date", record.date);
        data.put("imageUrls", record.imageUris);

        db.collection("users").document(userId)
                .collection("records").add(data)
                .addOnSuccessListener(documentReference -> Log.d("Upload", "成功"))
                .addOnFailureListener(e -> Log.e("Upload", "失敗: " + e.getMessage()));
    }

    interface OnImagesUploaded {
        void onDone(List<String> downloadUrls);
    }

    private void syncLocalRecordsIfLoggedIn(String userId) {
        SharedPreferences prefs = getSharedPreferences("local_records", MODE_PRIVATE);
        String json = prefs.getString("records", "[]");

        List<LocalRecord> records = new Gson().fromJson(json, new TypeToken<List<LocalRecord>>() {}.getType());
        for (LocalRecord record : records) {
            uploadToFirebase(record, userId);
        }

        prefs.edit().remove("records").apply();
    }


    private void setupLocateClick() {
        EditText etLocate = findViewById(R.id.locate);
        etLocate.setFocusable(false);

        etLocate.setOnClickListener(v -> {
            if (!Places.isInitialized()) {
                Places.initialize(getApplicationContext(), "AIzaSyCLk_Qv6hC73teDEcVk4A_HmmTjXDkXo78", Locale.TAIWAN);
            }

            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 3000);
                return;
            }

            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    double currentLat = location.getLatitude();
                    double currentLng = location.getLongitude();

                    List<Place.Field> fields = Arrays.asList(
                            Place.Field.ID,
                            Place.Field.NAME,
                            Place.Field.LAT_LNG,
                            Place.Field.ADDRESS
                    );

                    RectangularBounds bounds = RectangularBounds.newInstance(
                            new LatLng(currentLat - 0.1, currentLng - 0.1),
                            new LatLng(currentLat + 0.1, currentLng + 0.1)
                    );

                    Intent intent = new Autocomplete.IntentBuilder(
                            AutocompleteActivityMode.OVERLAY, fields)
                            .setLocationBias(bounds)
                            .build(this);

                    startActivityForResult(intent, 2000);
                }
            });
        });
    }





    private void openGoogleMapSearch(String query) {
        String uri = "geo:0,0?q=" + Uri.encode(query);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("無法開啟 Google 地圖")
                    .setMessage("請確認你已安裝 Google Maps 應用程式")
                    .setPositiveButton("確定", null)
                    .show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            uploadedImageUris.clear();

            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    uploadedImageUris.add(uri);
                }
            } else if (data.getData() != null) {
                Uri uri = data.getData();
                uploadedImageUris.add(uri);
            }

            for (Uri uri : uploadedImageUris) {
                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                getContentResolver().takePersistableUriPermission(uri, takeFlags);
            }
        }
    }





    private void onDigitClick(View v) {
        if (amountBuilder.length() >= MAX_LEN) return;
        String key = ((TextView) v).getText().toString();
        char c = key.charAt(0);
        if ("+-*/×÷".indexOf(c) >= 0) {
            if (amountBuilder.length() == 0) return;
            char last = amountBuilder.charAt(amountBuilder.length() - 1);
            if ("+-*/×÷".indexOf(last) >= 0)
                amountBuilder.setCharAt(amountBuilder.length() - 1, c);
            else amountBuilder.append(c);
        } else if (c == '.') {
            if (amountBuilder.length() == 0) amountBuilder.append("0.");
            else {
                char last = amountBuilder.charAt(amountBuilder.length() - 1);
                if ("+-*/×÷".indexOf(last) >= 0) amountBuilder.append("0.");
                else {
                    int i = amountBuilder.length() - 1;
                    while (i >= 0 && "+-*/×÷".indexOf(amountBuilder.charAt(i)) < 0) i--;
                    String seg = amountBuilder.substring(i + 1);
                    if (!seg.contains(".")) amountBuilder.append('.');
                }
            }
        } else {
            if (c == '0' && amountBuilder.length() == 0) return;
            amountBuilder.append(c);
        }
        tvAmountDisplay.setText("NT$" + amountBuilder);
    }

    private void onClearClick(View v) {
        amountBuilder.setLength(0);
        tvAmountDisplay.setText("NT$0");
    }

    private void onBackClick(View v) {
        if (amountBuilder.length() > 0) {
            amountBuilder.deleteCharAt(amountBuilder.length() - 1);
            String t = amountBuilder.length() > 0 ? amountBuilder.toString() : "0";
            tvAmountDisplay.setText("NT$" + t);
        }
    }

    private void onEqualClick(View v) {
        try {
            while (amountBuilder.length() > 0) {
                char last = amountBuilder.charAt(amountBuilder.length() - 1);
                if ("+-*/×÷".indexOf(last) >= 0 || last == '.')
                    amountBuilder.deleteCharAt(amountBuilder.length() - 1);
                else break;
            }
            String expr = amountBuilder.toString().replace('×', '*').replace('÷', '/');
            double res = evaluate(expr);
            String s = String.valueOf(res == Math.floor(res) ? (long) res : res);
            if (s.length() > MAX_LEN) s = s.substring(0, MAX_LEN);
            tvAmountDisplay.setText("NT$" + s);
            amountBuilder.setLength(0);
            amountBuilder.append(s);
        } catch (Exception e) {
            tvAmountDisplay.setText("Error");
            amountBuilder.setLength(0);
            new Handler().postDelayed(() -> tvAmountDisplay.setText("NT$0"), 1500);
        }
    }

    private double evaluate(String expr) {
        Stack<Double> vals = new Stack<>();
        Stack<Character> ops = new Stack<>();
        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                int j = i;
                while (j < expr.length() && (Character.isDigit(expr.charAt(j)) || expr.charAt(j) == '.')) j++;
                vals.push(Double.parseDouble(expr.substring(i, j)));
                i = j;
            } else {
                while (!ops.isEmpty() && precedence(ops.peek()) >= precedence(c))
                    applyOp(vals, ops.pop());
                ops.push(c);
                i++;
            }
        }
        while (!ops.isEmpty()) applyOp(vals, ops.pop());
        return vals.isEmpty() ? 0 : vals.pop();
    }

    private int precedence(char op) {
        switch (op) {
            case '+':
            case '-':
                return 1;
            case '*':
            case '/':
                return 2;
        }
        return 0;
    }

    private void applyOp(Stack<Double> v, char op) {
        double b = v.pop(), a = v.isEmpty() ? 0 : v.pop();
        switch (op) {
            case '+':
                v.push(a + b);
                break;
            case '-':
                v.push(a - b);
                break;
            case '*':
                v.push(a * b);
                break;
            case '/':
                v.push(b != 0 ? a / b : 0);
                break;
        }
    }

    private void showDatePickerDialog(TextView dateView) {
        View dv = LayoutInflater.from(this).inflate(R.layout.dialog_day_picker, null);
        NumberPicker y = dv.findViewById(R.id.yearPicker);
        NumberPicker m = dv.findViewById(R.id.monthPicker);
        NumberPicker d = dv.findViewById(R.id.dayPicker);
        Calendar c = Calendar.getInstance();
        int cy = c.get(Calendar.YEAR), cm = c.get(Calendar.MONTH) + 1, cd = c.get(Calendar.DAY_OF_MONTH);
        y.setMinValue(2000);
        y.setMaxValue(2100);
        y.setValue(cy);
        m.setMinValue(1);
        m.setMaxValue(12);
        m.setValue(cm);
        d.setMinValue(1);
        d.setMaxValue(31);
        d.setValue(cd);
        AlertDialog dlg = new AlertDialog.Builder(this).setView(dv).create();
        Button ok = dv.findViewById(R.id.btnOK), no = dv.findViewById(R.id.btnCancel);
        ok.setOnClickListener(v -> {
            dateView.setText(String.format("%04d-%02d-%02d", y.getValue(), m.getValue(), d.getValue()));
            dlg.dismiss();
        });
        no.setOnClickListener(v -> dlg.dismiss());
        dlg.show();
    }
}
