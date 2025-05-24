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


public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ReminderReceiver", "Alarm received!");

        String channelId = "reminder_channel";
        String title = "記帳提醒";
        String message = "再不記帳，我就要開始跳舞了喔！";

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Android 8+ 需建立 Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "提醒通知", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_head_svg) // 替換為你實際的 icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify(1001, builder.build());

        // 讀取提醒類型並重新排程
        int type = intent.getIntExtra("reminder_type", -1);
        Calendar calendar = Calendar.getInstance();

        switch (type) {
            case 1001: // 每日
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case 1002: // 每週
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case 1003: // 每月
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

