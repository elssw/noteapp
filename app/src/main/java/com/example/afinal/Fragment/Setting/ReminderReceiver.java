package com.example.afinal.Fragment.Setting;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.afinal.R;

import java.util.Calendar;
import static com.example.afinal.Fragment.Setting.Constants.*;


public class ReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderDebug";

    @Override
    public void onReceive(Context context, Intent intent) {
        //
        int type = intent.getIntExtra("reminder_type", -1);
        Log.d(TAG, "reminder_type = " + type);

        if (type == REQUEST_CODE_TEST) {
            Log.d(TAG, "ReminderReceiver _ Testing!");
        }
        //
        Log.d(TAG, "Receiver triggered at: " + System.currentTimeMillis());
        String channelId = "reminder_channel";
        String title = "記帳提醒";
        String message = "再不記帳，我就要跳舞了喔！";

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Android 8+ 需建立 Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "提醒通知", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        // 點擊通知後要開啟的APP畫面
        Intent openIntent = new Intent(context, com.example.afinal.MainActivity2.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_head_svg) // 替換為你實際的 icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        manager.notify(REQUEST_CODE_DAILY, builder.build());

        // 讀取提醒類型並重新排程
        //int type = intent.getIntExtra("reminder_type", -1);

        Log.d(TAG, "reminder_type = " + type);  // 補充：印出類型以利偵錯

        Calendar calendar = Calendar.getInstance();

        switch (type) {
            case REQUEST_CODE_DAILY: // 每日
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case REQUEST_CODE_WEEKLY: // 每週
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case REQUEST_CODE_MONTHLY: // 每月
                calendar.add(Calendar.MONTH, 1);
                break;
            default:
                return;
        }

        // 設定時間為原本時間
        calendar.set(Calendar.HOUR_OF_DAY, Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, Calendar.getInstance().get(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, 0);

        // 重新設定 Alarm
        Intent repeatIntent = new Intent(context, ReminderReceiver.class);
        repeatIntent.putExtra("reminder_type", type);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, type, repeatIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }
}

