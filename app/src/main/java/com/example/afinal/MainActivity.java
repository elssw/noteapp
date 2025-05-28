package com.example.afinal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.afinal.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import java.util.Map;
import  com.example.afinal.place;
//import com.google.android.gms.

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private SupportMapFragment mapFragment ;
    private RecyclerView recyclerView;
    private GoogleMap mMap;
    private EditText Comment;
    private ActivityMapsBinding binding;
    private ImageView imagePreview;
    private Button uploadButton;
    private Button editButton;
    private RestaurantAdapter adapter;
    //    private SharedPreferences prefs;
    private List<Uri> imageUris = new ArrayList<>();
    private FirebaseFirestore db;
    private  int number = 1;
    private String placeId;
    private static final int REQUEST_IMAGE_PICK = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        ImageButton imb= findViewById(R.id.btn_location);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this); // **確保地圖載入完成後才初始化 mMap**
        }
//        LinearLayout botom=findViewById(R.id.bottom_sheet);
//        BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(botom);
//        bottomSheetBehavior.setPeekHeight(200);
//        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        LinearLayout bottomSheet = findViewById(R.id.bottom_sheet);
        BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();
// 設定初始高度
        bottomSheetBehavior.setPeekHeight(200);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_EXPANDED:
                        System.out.println("Bottom Sheet 完全展開");
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        System.out.println("Bottom Sheet 收起");
                        break;
                    case BottomSheetBehavior.STATE_HALF_EXPANDED:
                        System.out.println("Bottom Sheet 半展開");
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        System.out.println("Bottom Sheet 拖動中");
                        break;
                }
            }
            public void onSlide(View bottomSheet, float slideOffset) {
                System.out.println("滑動進度: " + slideOffset);
            }
        });
        // imagePreview = findViewById(R.id.image_preview);
        uploadButton = findViewById(R.id.upload_button);
        editButton=findViewById(R.id.edit_button);
        Comment=findViewById(R.id.textView);
        recyclerView = findViewById(R.id.image_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapter = new RestaurantAdapter(this,imageUris);
        recyclerView.setAdapter(adapter);

//        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImagePicker();
            }
        });
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadfirebase();
            }
        });

        imb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveToCurrentLocation();
            }
        });

    }
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnPoiClickListener(new GoogleMap.OnPoiClickListener() {
            @Override
            public void onPoiClick(PointOfInterest poi) {
                // 取得 POI 資訊
                String name = poi.name;
                placeId = poi.placeId;
                LatLng latLng = poi.latLng;
                uploadButton.setVisibility(View.VISIBLE);
                editButton.setVisibility(View.VISIBLE);
                // 你可以印出來或顯示在 Bottom Sheet、Dialog 中
                TextView placeName = findViewById(R.id.place_name);
                TextView placeAddress = findViewById(R.id.place_address);
                placeName.setText(poi.name);
                placeAddress.setText("Place ID: " + poi.placeId); // 若你沒地點地址，可以先放 placeId

                BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet));
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);

                SharedPreferences prefs = getSharedPreferences("login", MODE_PRIVATE);
                String userid = prefs.getString("userid", "0");
                // 例如放一個標記
                mMap.clear();
                mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(name));
                imageUris.clear();
                adapter.notifyDataSetChanged();  // 通知 RecyclerView 整體更新
                Comment.setText("");
                DocumentReference placeRef = db.collection("users").document(userid)
                        .collection("map").document(placeId);

                placeRef.get().addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Comment.setText((String) snapshot.get("comment"));
                        List<String> imageUrls = (List<String>) snapshot.get("images");  // 讀取 images 陣列
                        if (imageUrls != null) {
                            for (String url : imageUrls) {
                                imageUris.add(Uri.parse(url));  // 轉成 Uri 並加入
                            }

                            // ✅ 通知 RecyclerView 更新
                            adapter.notifyItemInserted(imageUris.size() - 1);
                        }
                    }});
            }
        });
        // **設定地圖樣式**
        boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map));

        if (!success) {
            System.out.println("地圖樣式載入失敗");
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            enableUserLocation();
            System.out.println("mmmap");
        }


    }
    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15));
                        }
                    });
        }
    }
    private void uploadfirebase(){
        String comments = Comment.getText().toString().trim();
        SharedPreferences prefs = getSharedPreferences("login", MODE_PRIVATE);
        String userid = prefs.getString("userid", "0");

        if(userid.equals(0)){


        }
        else{
            DocumentReference placeRef = db.collection("users").document(userid)
                    .collection("map").document(placeId);

            placeRef.get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    placeRef.update("comment", comments);
                } else {
                    Map<String, Object> data = new HashMap<>();
                    data.put("place_id", placeId);
                    data.put("comment", comments);
                    db.collection("users").document(userid).collection("map").document(placeId).set(data);
                }
            });
        }


    }

    private void openImagePicker() {
//        Intent intent = new Intent(Intent.ACTION_PICK);
//        intent.setType("image/*");
//        startActivityForResult(intent, REQUEST_IMAGE_PICK);
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // ✅ 加上這行
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,  Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SharedPreferences prefs = getSharedPreferences("login", MODE_PRIVATE);
        String userid = prefs.getString("userid", "0");

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            //final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(imageUri, takeFlags);

            if (imageUri != null) {
                if(userid.equals(0)){


                }
                else{

                    DocumentReference placeRef = db.collection("users").document(userid)
                            .collection("map").document(placeId);

                    placeRef.get().addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            placeRef.update("images", FieldValue.arrayUnion(imageUri.toString()));
                        } else {
                            Map<String, Object> value = new HashMap<>();
                            value.put("place_id", placeId);
                            value.put("images", Collections.singletonList(imageUri.toString()));
                            db.collection("users").document(userid).collection("map").document(placeId).set(value);
                        }
                    });
//                    placeRef.get().addOnSuccessListener(snapshot -> {
//                        if (snapshot.exists()) {
//                            List<String> imageUrls = (List<String>) snapshot.get("images");  // 讀取 images 陣列
//                            if (imageUrls != null) {
//
//                                for (String url : imageUrls) {
//                                    imageUris.add(Uri.parse(url));  // 轉成 Uri 並加入
//                                }
//                                adapter.notifyDataSetChanged();
                    // ✅ 通知 RecyclerView 更新
                    imageUris.add(imageUri);
                    adapter.notifyItemInserted(imageUris.size() - 1);
//                            }
//                        }});



                }

            }
        }
    }

    private void moveToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
            }
        });
    }

}