package com.example.afinal.Fragment.Setting;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.util.Log;

import com.example.afinal.Fragment.SettingFragment;
import com.example.afinal.R;

import java.util.Calendar;

public class SettingReminderFragment extends Fragment {
    private RadioGroup reminderType;
    private TimePicker dailyPicker;
    private View reminderWeekly;
    private View reminderMonthly;
    private Button btnConfirm;
    private TextView tvReminderState;

    private ActivityResultLauncher<String> requestPermissionLauncher;

    // 儲存使用者偏好設定
    private SharedPreferences prefs;

    private static final String PREF_REMINDER_STATE = "reminder_state_prefs";
    // Example Tag for filtering
    private static final String TAG = "ReminderDebug";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_setting_reminder, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        reminderType = view.findViewById(R.id.reminder_type);
        dailyPicker = view.findViewById(R.id.time_picker_daily);
        reminderWeekly = view.findViewById(R.id.reminder_weekly);
        reminderMonthly = view.findViewById(R.id.reminder_monthly);
        btnConfirm = view.findViewById(R.id.btn_reminder_confirm);
        tvReminderState = view.findViewById(R.id.tv_reminder_state);

        // Reminder State
        prefs = requireContext().getSharedPreferences(PREF_REMINDER_STATE, 0);
        String mode = prefs.getString("reminder_mode", "未設定");
        String time = prefs.getString("reminder_time", "");
        tvReminderState.setText("提醒時間：" + mode + " " + time);

        // 授予通知權限
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Toast.makeText(requireContext(), "通知權限已授予", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "未授予通知權限，提醒可能無法顯示", Toast.LENGTH_LONG).show();
                    }
                }
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (requireContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // 選擇通知時間週期
        reminderType.setOnCheckedChangeListener((group, checkedId) -> {
            dailyPicker.setVisibility(View.GONE);
            reminderWeekly.setVisibility(View.GONE);
            reminderMonthly.setVisibility(View.GONE);

            if (checkedId == R.id.rb_daily) {
                dailyPicker.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.rb_weekly) {
                reminderWeekly.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.rb_monthly) {
                reminderMonthly.setVisibility(View.VISIBLE);
            }
        });


        // 送出通知時程
        btnConfirm.setOnClickListener(v -> {
            Log.d(TAG, "User Pushed btnConfirm.");

            if (isAdded() && getActivity() != null) {
                Toast.makeText(getContext(), "Remind Finished.", Toast.LENGTH_SHORT).show();
            }

            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(requireContext(), ReminderReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);

            // 建立時間物件
            Calendar calendar = Calendar.getInstance();
            int hour, minute;
            SharedPreferences.Editor editor = prefs.edit();
            // 判斷提醒類型
            int checkedId = reminderType.getCheckedRadioButtonId();

            Log.d(TAG, "Remind Type choise ID:" + checkedId);

            if (checkedId == R.id.rb_daily) {
                dailyPicker.clearFocus();
                hour = dailyPicker.getHour();
                minute = dailyPicker.getMinute();

                Log.d(TAG, "Remind Dayly:" + hour + ":" + minute);

                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                scheduleAlarm(calendar, 1001);
                editor.putString("reminder_mode", "EveryDay");
                editor.putString("reminder_time", String.format("%02d:%02d", hour, minute));
            } else if (checkedId == R.id.rb_weekly) {
                TimePicker picker = view.findViewById(R.id.time_picker_weekly);
                picker.clearFocus();
                hour = picker.getHour();
                minute = picker.getMinute();

                int dayOfWeek = ((Spinner) view.findViewById(R.id.spinner_weekday)).getSelectedItemPosition() + 1;

                Log.d(TAG, "Remind Weekly:" + hour + ":" + minute + "，dayOfWeek:" + dayOfWeek);

                calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                scheduleAlarm(calendar, 1002);
                String[] weekdays = getResources().getStringArray(R.array.weekdays); // 確保你有此 array
                String weekday = weekdays[((Spinner) view.findViewById(R.id.spinner_weekday)).getSelectedItemPosition()];
                editor.putString("reminder_mode", "Every  " + weekday);
                editor.putString("reminder_time", String.format("%02d:%02d", hour, minute));
            } else if (checkedId == R.id.rb_monthly) {
                TimePicker picker = view.findViewById(R.id.time_picker_monthly);
                picker.clearFocus();
                hour = picker.getHour();
                minute = picker.getMinute();

                String dayStr = ((EditText) view.findViewById(R.id.edit_day_of_month)).getText().toString();
                if (dayStr.isEmpty()) {
                    Log.e(TAG, "User didn't input date.");
                    Toast.makeText(getContext(), "Please input reminder date.", Toast.LENGTH_SHORT).show();
                    return;
                }
                int day = Integer.parseInt(dayStr);

                Log.d(TAG, "Remind Monthly:" + hour + ":" + minute + "，date:" + day);

                calendar.set(Calendar.DAY_OF_MONTH, day);
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                scheduleAlarm(calendar, 1003);
                editor.putString("reminder_mode", "Every " + dayStr);
                editor.putString("reminder_time", String.format("%02d:%02d", hour, minute));
            }
            editor.apply();  // 寫入

            Log.d(TAG, "Have stored in SharedPreferences.");

            // 替換目前 Fragment 為提醒設定頁面
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {

                Log.d(TAG, "Back to SettingFragment.");

                // 創建SettingFragment
                SettingFragment settingFragment = new SettingFragment();
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_main, settingFragment)
                        .addToBackStack(null)
                        .commit();
            }, 800); // 延遲 800ms 等 Toast 顯示完

        });
    }

    private void scheduleAlarm(Calendar calendar, int requestCode) {
        Intent intent = new Intent(requireContext(), ReminderReceiver.class);
        intent.putExtra("reminder_type", requestCode); // 1001=每日, 1002=每週, 1003=每月
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(), requestCode, intent, PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);

        long triggerTime = calendar.getTimeInMillis();
        if (triggerTime < System.currentTimeMillis()) {
            // 若時間早於現在，補到下週、下月或明天
            if (requestCode == 1001) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);  // 每日
            } else if (requestCode == 1002) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1); // 每週
            } else if (requestCode == 1003) {
                calendar.add(Calendar.MONTH, 1);        // 每月
            }
            triggerTime = calendar.getTimeInMillis();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );
        }
    }
}