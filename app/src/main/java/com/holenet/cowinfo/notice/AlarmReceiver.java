package com.holenet.cowinfo.notice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.holenet.cowinfo.NetworkService;
import com.holenet.cowinfo.R;

import java.util.Calendar;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null)
            return;

        if (NoticeManager.ACTION_CHECK.equals(intent.getAction())) {
            Calendar today = Calendar.getInstance();
            int year = today.get(Calendar.YEAR);
            int month = today.get(Calendar.MONTH) + 1;
            int day = today.get(Calendar.DAY_OF_MONTH);

            NoticeService.startService(context, NetworkService.constructDate(year, month, day));
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            final SharedPreferences pref = context.getSharedPreferences("notice", 0);
            final boolean enable = pref.getBoolean(context.getString(R.string.pref_key_notice_enable), false);
            if (!enable)
                return;
            final int[] time = {pref.getInt(context.getString(R.string.pref_key_notice_hour_of_day), 9), pref.getInt(context.getString(R.string.pref_key_notice_minute), 0)};

            NoticeManager.enableNotice(context, time[0], time[1]);
        }
    }
}
