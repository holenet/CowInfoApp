package com.holenet.cowinfo.notice;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.holenet.cowinfo.MainActivity;
import com.holenet.cowinfo.NetworkService;
import com.holenet.cowinfo.R;
import com.holenet.cowinfo.RecordDateActivity;
import com.holenet.cowinfo.item.Record;
import com.holenet.cowinfo.item.User;
import com.prolificinteractive.materialcalendarview.CalendarDay;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.holenet.cowinfo.NetworkService.destructDate;

public class NoticeService extends IntentService {
    private static final String ACTION_NOTIFY = "com.holenet.cowinfo.notice.action.NOTIFY";
    private static final String PARAM_DATE = "com.holenet.cowinfo.notice.param.DATE";
    private static final String CHANNEL_ID = "notice_channel";

    public NoticeService() {
        super("NoticeService");
    }

    public static void startService(Context context, String date) {
        Intent intent = new Intent(context, NoticeService.class);
        intent.setAction(ACTION_NOTIFY);
        intent.putExtra(PARAM_DATE, date);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(intent);
        else
            context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        createChannel();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder builder = getBasicNotificationBuilder("작업 중...");
            startForeground(1, builder.build());
        }

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
            NetworkService.Result<User> userResult = NetworkService.signIn(user);
            if (userResult == null) {
                notifyWithContent("서버 통신에 실패하였습니다.");
                return;
            }
            if (!userResult.isSuccessful()) {
                notifyWithContent("로그인에 실패하였습니다.");
                return;
            }

            NetworkService.Result<List<Record>> result = NetworkService.getRecordList(false, date);
            if (!result.isSuccessful()) {
                notifyWithContent("이력을 가져오는 데에 실패하였습니다.");
                return;
            }

            List<Record> records = new ArrayList<>();
            for (Record record : result.getResult()) {
                if ("재발".equals(record.content) || "분만".equals(record.content)) {
                    records.add(record);
                }
            }

            if (records.size() == 0)
                return;

            int[] days = destructDate(date);
            notifyWithContent((ArrayList<Record>) records, CalendarDay.from(days[0], days[1] - 1, days[2]));
        }
    }

    private NotificationCompat.Builder getBasicNotificationBuilder(String content) {
        return new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentTitle(getApplicationContext().getString(R.string.app_name)+" 재발/분만 알림")
                .setContentText(content)
                .setAutoCancel(true);
    }

    private TaskStackBuilder getBasicStackBuilder() {
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("position", 1);
        stackBuilder.addNextIntent(intent);

        return stackBuilder;
    }

    private void createChannel() {
        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "한우이력정보 재발/분만 알림", NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }
    }

    private void notifyNotification(NotificationCompat.Builder builder) {
        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(2, builder.build());
    }

    private void notifyWithContent(String content) {
        NotificationCompat.Builder builder = getBasicNotificationBuilder(content);
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(content));

        TaskStackBuilder stackBuilder = getBasicStackBuilder();
        builder.setContentIntent(stackBuilder.getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT));

        notifyNotification(builder);
    }

    private void notifyWithContent(ArrayList<Record> records, CalendarDay date) {
        String content;
        if (records.size() > 1)
            content = String.format(Locale.KOREA, "%d월 %d일 재발/분만 %d건", date.getMonth() + 1, date.getDay(), records.size());
        else
            content = String.format(Locale.KOREA, "%d월 %d일 %s %s", date.getMonth() + 1, date.getDay(), records.get(0).cow_summary, records.get(0).content);
        NotificationCompat.Builder builder = getBasicNotificationBuilder(content);

        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
        for (Record record : records) {
            style.addLine(record.toString());
        }
        style.setSummaryText(content);
        builder.setStyle(style);

        TaskStackBuilder stackBuilder = getBasicStackBuilder();
        Intent intent = new Intent(this, RecordDateActivity.class);
        intent.putExtra("record_list", records);
        intent.putExtra("date", date);
        stackBuilder.addNextIntent(intent);
        builder.setContentIntent(stackBuilder.getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT));

        notifyNotification(builder);
    }
}
