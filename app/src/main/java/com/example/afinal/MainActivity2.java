package com.example.afinal;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.afinal.Fragment.GroupFragment;
import com.example.afinal.Fragment.MapFragment;
import com.example.afinal.Fragment.SettingFragment;
import com.example.afinal.Fragment.UserFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity2 extends AppCompatActivity {
    private ImageButton imb;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String userId = getIntent().getStringExtra("userId");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // 預設載入 UserFragment
        if (savedInstanceState == null) {
            String target = getIntent().getStringExtra("navigate_to");
            Log.d("IntentDebug", "MainActivity2 received navigate_to = " + target);

            if (target == null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_main, new UserFragment())
                        .commit();
            } else {
                handleNavigation(getIntent());
            }
        }


        imb=findViewById(R.id.button);
        imb.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity2.this, MainActivity.class);
            startActivity(intent);
        });
        // BottomNavigation 切換邏輯（改為 if-else，避免 constant expression error）
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            // 根據點按的 bottom_nav 切換 fragment_main
            if (id == R.id.menu_user) {
                selectedFragment = new UserFragment();
            } else if (id == R.id.menu_group) {
                selectedFragment = new GroupFragment();
            }
             else if (id == R.id.menu_setting) {
                selectedFragment = new SettingFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_main, selectedFragment)
                        .commit();
            }

            return true;
        });


    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // 更新 Intent，讓 getIntent() 取得最新的
        handleNavigation(intent);
    }
    private void handleNavigation(Intent intent) {
        if (intent != null) {
            String target = intent.getStringExtra("navigate_to");
            Log.d("IntentDebug", "handleNavigation received navigate_to = " + target);

            if ("group".equals(target)) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_main, new GroupFragment())
                        .commit();
            }
        }
    }



}
