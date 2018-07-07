package com.holenet.cowinfo.notice;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;

import com.holenet.cowinfo.MainActivity;
import com.holenet.cowinfo.NetworkService;
import com.holenet.cowinfo.R;
import com.holenet.cowinfo.RecordDateActivity;
import com.holenet.cowinfo.item.Record;
import com.holenet.cowinfo.item.User;
import com.prolificinteractive.materialcalendarview.CalendarDay;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.holenet.cowinfo.NetworkService.destructDate;

public class NoticeService extends IntentService {
    private static final String ACTION_NOTIFY = "com.holenet.cowinfo.notice.action.NOTIFY";
    private static final String PARAM_DATE = "com.holenet.cowinfo.notice.param.DATE";

    public NoticeService() {
        super("NoticeService");
    }

    public static void startService(Context context, String date) {
        Intent intent = new Intent(context, NoticeService.class);
        intent.setAction(ACTION_NOTIFY);
        intent.putExtra(PARAM_DATE, date);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null)
            return;

        if (ACTION_NOTIFY.equals(intent.getAction())) {
            final String date = intent.getStringExtra(PARAM_DATE);

            Context context = getApplicationContext();
            SharedPreferences pref = context.getSharedPreferences("sign_in", 0);
            boolean autoSignIn = pref.getBoolean(context.getString(R.string.pref_key_auto_sign_in), false);
            if (!autoSignIn) {
                NoticeManager.disableNotice(context);
                notifyWithContent("자동 로그인이 설정되어 있지 않아 알림이 해제 되었습니다.");
                return;
            }
            String username = pref.getString(context.getString(R.string.pref_key_username), "");
            String password = pref.getString(context.getString(R.string.pref_key_password), "");

            User user = new User(username, password);
            if (!NetworkService.signIn(user).isSuccessful()) {
                notifyWithContent("로그인에 실패하였습니다.");
                return;
            }

            NetworkService.Result<List<Record>> result = NetworkService.getRecordList(false, date);
            if (!result.isSuccessful()) {
                notifyWithContent("이력을 가져오는 데에 실패하였습니다.");
                return;
            }

            List<Record> records = result.getResult();
            if (records.size() == 0)
                return;

            Set<String> cows = new HashSet<>();
            for (Record record : records) {
                cows.add(record.cow_number);
            }

            int[] days = destructDate(date);
            notifyWithContent(TextUtils.join(", ", cows), (ArrayList<Record>) records, CalendarDay.from(days[0], days[1] - 1, days[2]));
        }
    }

    private void notifyWithContent(String content) {
        notifyWithContent(content, null, null);
    }

    private void notifyWithContent(String content, ArrayList<Record> records, CalendarDay date) {
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("position", 1);
        stackBuilder.addNextIntent(intent);

        if (records != null) {
            intent = new Intent(this, RecordDateActivity.class);
            intent.putExtra("record_list", records);
            intent.putExtra("date", date);
            stackBuilder.addNextIntent(intent);
        }

        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "notice")
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentTitle(getApplicationContext().getString(R.string.app_name))
                .setContentText(content)
//                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }
}
