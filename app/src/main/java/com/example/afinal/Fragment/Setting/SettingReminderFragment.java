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
import static com.example.afinal.Fragment.Setting.Constants.*;

public class SettingReminderFragment extends Fragment {
    private RadioGroup reminderType;
    private TimePicker dailyPicker;
    private View reminderWeekly;
    private View reminderMonthly;
    private Button btnReminderConfirm;
    private Button btnReminderCancel;
    private TextView tvReminderState;
    int requestCode;

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
        btnReminderConfirm = view.findViewById(R.id.btn_reminder_confirm);
        btnReminderCancel = view.findViewById(R.id.btn_reminder_cancel);
        tvReminderState = view.findViewById(R.id.tv_reminder_state);

        // Reminder State
        prefs = requireContext().getSharedPreferences(PREF_REMINDER_STATE, 0);
        String mode = prefs.getString("reminder_mode", "尚未設定");
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

        // 取消提醒
        btnReminderCancel.setOnClickListener(v -> {
            Log.d(TAG, "SettingReminderFragment _ Testing Cancel");
            clearAllReminders();

            // 跳回 SettingFragment
            navigateToSettingFragmentDelayed(800);
        });

        // 設定提醒
        btnReminderConfirm.setOnClickListener(v -> {
            Log.d(TAG, "SettingReminderFragment _ Testing Confirm");

            Calendar calendar = Calendar.getInstance();
            int hour, minute;

            int checkedId = reminderType.getCheckedRadioButtonId();
            if (checkedId == R.id.rb_daily) {
                dailyPicker.clearFocus();
                hour = dailyPicker.getHour();
                minute = dailyPicker.getMinute();
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                setReminder(REQUEST_CODE_DAILY, calendar, "每日", String.format("%02d:%02d", hour, minute));
            } else if (checkedId == R.id.rb_weekly) {
                TimePicker picker = getView().findViewById(R.id.time_picker_weekly);
                picker.clearFocus();
                hour = picker.getHour();
                minute = picker.getMinute();
                int dayOfWeek = ((Spinner) getView().findViewById(R.id.spinner_weekday)).getSelectedItemPosition() + 1;
                calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                String weekday = getResources().getStringArray(R.array.weekdays)[dayOfWeek - 1];
                setReminder(REQUEST_CODE_WEEKLY, calendar, "每週" + weekday, String.format("%02d:%02d", hour, minute));
            } else if (checkedId == R.id.rb_monthly) {
                TimePicker picker = getView().findViewById(R.id.time_picker_monthly);
                picker.clearFocus();
                hour = picker.getHour();
                minute = picker.getMinute();
                String dayStr = ((EditText) getView().findViewById(R.id.edit_day_of_month)).getText().toString();
                if (dayStr.isEmpty()) {
                    Toast.makeText(getContext(), "請輸入日期", Toast.LENGTH_SHORT).show();
                    return;
                }
                int day = Integer.parseInt(dayStr);
                calendar.set(Calendar.DAY_OF_MONTH, day);
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                setReminder(REQUEST_CODE_MONTHLY, calendar, "每月 " + dayStr + " 號", String.format("%02d:%02d", hour, minute));
            }
        /*    else {
                updateReminderState(null, null);
            }   */
            // 跳回 SettingFragment
            navigateToSettingFragmentDelayed(800);
        });

    }


    // 設定 Alarm
    private void scheduleAlarm(Calendar calendar, int requestCode) {
        Log.d(TAG, "SettingReminderFragment _ Alarm set at: " + calendar.getTime() + " for requestCode: " + requestCode);

        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);

        // Android 12 (API 31) 以上需要額外權限
        // 開啟設定允許提醒，才不會讓程式崩潰
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "SCHEDULE_EXACT_ALARM 權限未授予，導引使用者開啟權限");
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(android.net.Uri.parse("package:" + requireContext().getPackageName()));
                startActivity(intent);
                return;  // 中止 alarm 設定，等使用者授權
            }
        }

        // 指定觸發目標接收器 BroadcastReceiver
        Intent intent = new Intent(requireContext(), ReminderReceiver.class);
        // 表示提醒的類型，額外資訊（extra）到 Intent 中
        intent.putExtra("reminder_type", requestCode); // 1001=每日, 1002=每週, 1003=每月
        // 包裝 Intent，讓系統未來某個時間點可以代替執行 broadcast 操作
        PendingIntent pendingIntent = PendingIntent.getBroadcast(requireContext(), requestCode, intent, PendingIntent.FLAG_IMMUTABLE);


        long triggerTime = calendar.getTimeInMillis();
        // 若時間早於現在，補到下週、下月或明天
        if (triggerTime < System.currentTimeMillis()) {
            if (requestCode == REQUEST_CODE_DAILY) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);  // 每日
            } else if (requestCode == REQUEST_CODE_WEEKLY) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1); // 每週
            } else if (requestCode == REQUEST_CODE_MONTHLY) {
                calendar.add(Calendar.MONTH, 1);        // 每月
            }
            triggerTime = calendar.getTimeInMillis();
        }

        // 根據 Android 版本，正確地設定 Alarm（鬧鐘）來準時觸發通知或任務
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // API 23+
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

    // 設定提醒
    private void setReminder(int requestCode, Calendar calendar, String modeText, String timeText) {
        cancelAllAlarms();  // 不允許同時存在多個提醒
        scheduleAlarm(calendar, requestCode);
        updateReminderState(modeText, timeText);
        Toast.makeText(getContext(), "提醒設定成功", Toast.LENGTH_SHORT).show();
    }

    // 更新提醒狀態
    private void updateReminderState(@Nullable String mode, @Nullable String time) {
        SharedPreferences.Editor editor = prefs.edit();

        if (mode != null && time != null) {
            editor.putString("reminder_mode", mode);
            editor.putString("reminder_time", time);
            tvReminderState.setText("提醒時間：" + mode + " " + time);
        } else {
            editor.remove("reminder_mode");
            editor.remove("reminder_time");
            tvReminderState.setText("提醒時間：尚未設定");
        }

        editor.apply();
    }

    // 僅取消 Alarm（不清除資料或畫面）
    private void cancelAllAlarms() {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);

        int[] allRequestCodes = {REQUEST_CODE_DAILY, REQUEST_CODE_WEEKLY, REQUEST_CODE_MONTHLY, REQUEST_CODE_TEST};

        for (int code : allRequestCodes) {
            Intent intent = new Intent(requireContext(), ReminderReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(), code, intent, PendingIntent.FLAG_IMMUTABLE);

            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Canceled alarm for requestCode: " + code);
        }
    }

    // 取消 Alarm + 清除資料 + 清除畫面提醒時間
    private void clearAllReminders() {
        // 僅取消 Alarm（不清除資料或畫面）
        cancelAllAlarms();
        // 更新提醒狀態
        updateReminderState(null, null);

        // 小提示
        Toast.makeText(getContext(), "取消所有提醒", Toast.LENGTH_SHORT).show();
    }

    // 切換 Fragment
    private void navigateToSettingFragmentDelayed(long delayMillis) {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            Log.d(TAG, "Back to SettingFragment.");
            SettingFragment settingFragment = new SettingFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_main, settingFragment)
                    .addToBackStack(null)
                    .commit();
        }, delayMillis);
    }
}