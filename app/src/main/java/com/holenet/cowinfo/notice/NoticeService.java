package com.holenet.cowinfo.notice;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.holenet.cowinfo.NetworkService;
import com.holenet.cowinfo.R;
import com.holenet.cowinfo.item.Record;
import com.holenet.cowinfo.item.User;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            notifyWithContent(TextUtils.join(", ", cows));
        }
    }

    private void notifyWithContent(String content) {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "notice")
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentTitle(getApplicationContext().getString(R.string.app_name))
                .setContentText(content)
//                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }
}
