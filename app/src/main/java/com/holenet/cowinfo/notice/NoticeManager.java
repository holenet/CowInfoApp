package com.holenet.cowinfo.notice;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.holenet.cowinfo.R;

import java.util.Calendar;

public class NoticeManager {
    public static final String ACTION_CHECK = "com.holenet.cowinfo.notice.action.CHECK";

    private static AlarmManager getAlarmManager(Context context) {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    private static Intent getIntent() {
        return new Intent(ACTION_CHECK);
    }

    private static void setPreference(Context context, boolean enable, int hourOfDay, int minute) {
        SharedPreferences pref = context.getSharedPreferences("notice", 0);
        SharedPreferences.Editor editor = pref.edit();

        editor.putBoolean(context.getString(R.string.pref_key_notice_enable), enable);
        if (enable) {
            editor.putInt(context.getString(R.string.pref_key_notice_hour_of_day), hourOfDay);
            editor.putInt(context.getString(R.string.pref_key_notice_minute), minute);
        }
        editor.apply();
    }

    public static void enableNotice(Context context, int hourOfDay, int minute) {
        Calendar calendar = Calendar.getInstance();
        long current = System.currentTimeMillis();
        calendar.setTimeInMillis(current);
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() < current) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, getIntent(), PendingIntent.FLAG_UPDATE_CURRENT);
        getAlarmManager(context).setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
        setPreference(context, true, hourOfDay, minute);
    }

    public static void disableNotice(Context context) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, getIntent(), PendingIntent.FLAG_NO_CREATE);
        getAlarmManager(context).cancel(pendingIntent);
        setPreference(context, false, 0, 0);
    }
}
