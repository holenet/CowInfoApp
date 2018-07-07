package com.holenet.cowinfo;

import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.holenet.cowinfo.item.Cow;
import com.holenet.cowinfo.item.User;
import com.holenet.cowinfo.notice.NoticeManager;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    final static int REQUEST_SIGN_IN = 101;
    final static int REQUEST_CREATE_COW = 102;

    private ViewPager vPmain;

    private CowListFragment cowListFragment;
    private CalendarFragment calendarFragment;
    private PagerAdapter adapter;

    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        cowListFragment = CowListFragment.newInstance(false);
        calendarFragment = CalendarFragment.newInstance();

        adapter = new PagerAdapter(getSupportFragmentManager());
        vPmain = findViewById(R.id.vPmain);
        vPmain.setAdapter(adapter);
        vPmain.setCurrentItem(getIntent().getIntExtra("position", 0));
        vPmain.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                cowListFragment.attemptGetCowList();
                calendarFragment.attemptGetRecordList();
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        Intent intent = new Intent(MainActivity.this, SignInActivity.class);
        startActivityForResult(intent, REQUEST_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                user = (User) data.getSerializableExtra("user");
            } else if (resultCode == RESULT_CANCELED) {
                finish();
            }
        } else if (requestCode == REQUEST_CREATE_COW) {
            if (resultCode == RESULT_OK) {
                Cow cow = (Cow) data.getSerializableExtra("cow");
                cowListFragment.attemptGetCowList();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.mIcreateCow) {
            Intent intent = new Intent(MainActivity.this, EditCowActivity.class);
            intent.putExtra("edit_mode", EditCowActivity.MODE_CREATE);
            startActivityForResult(intent, REQUEST_CREATE_COW);
        } else if (id == R.id.mIdeleted) {
            if (cowListFragment != null) {
                cowListFragment.setIsDeletedList(true);
                adapter.notifyDataSetChanged();
            }
            Toast.makeText(this, "돌아가려면 뒤로 버튼을 눌러주세요.", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.mInoticeSettings) {
            final SharedPreferences prefSignIn = getSharedPreferences("sign_in", 0);
            final boolean autoSignIn = prefSignIn.getBoolean(getString(R.string.pref_key_auto_sign_in), false);
            if (!autoSignIn) {
                Toast.makeText(MainActivity.this, "자동로그인을 설정하지 않으면 알림 기능을 사옹할 수 없습니다.", Toast.LENGTH_LONG).show();
                NoticeManager.disableNotice(this);
            }

            final SharedPreferences pref = getSharedPreferences("notice", 0);
            final boolean enable = pref.getBoolean(getString(R.string.pref_key_notice_enable), false);
            final int[] time = {pref.getInt(getString(R.string.pref_key_notice_hour_of_day), 9), pref.getInt(getString(R.string.pref_key_notice_minute), 0)};

            final ConstraintLayout layout = (ConstraintLayout) (((LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dialog_notice_settings, null));
            final ConstraintLayout cLadditional = layout.findViewById(R.id.cLadditional);
            final Switch sTnotice = layout.findViewById(R.id.sTnotice);
            final TextView tVtime = layout.findViewById(R.id.tVtime);
            final Button bTeditTime = layout.findViewById(R.id.bTeditTime);

            sTnotice.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        if (!autoSignIn) {
                            Toast.makeText(MainActivity.this, "자동로그인을 설정하지 않으면 알림 기능을 사옹할 수 없습니다.", Toast.LENGTH_LONG).show();
                            sTnotice.setChecked(false);
                            return;
                        }
                    }
                    cLadditional.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
            });
            sTnotice.setChecked(enable);
            cLadditional.setVisibility(enable ? View.VISIBLE : View.GONE);

            tVtime.setText(String.format(Locale.KOREA, "%s %d시 %d분", time[0] < 12 ? "오전" : "오후", (time[0] + 11) % 12 + 1, time[1]));

            bTeditTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TimePickerDialog dialog = new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            time[0] = hourOfDay;
                            time[1] = minute;
                            tVtime.setText(String.format(Locale.KOREA, "%s %d시 %d분", time[0] < 12 ? "오전" : "오후", (time[0] + 11) % 12 + 1, time[1]));
                        }
                    }, time[0], time[1], false);
                    dialog.show();
                }
            });

            new AlertDialog.Builder(this)
                    .setTitle("알림 설정")
                    .setIcon(getDrawable(R.drawable.ic_notifications_black_24dp))
                    .setView(layout)
                    .setPositiveButton("저장", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            boolean newEnable = sTnotice.isChecked();
                            NoticeManager.disableNotice(MainActivity.this);
                            if (newEnable) {
                                NoticeManager.enableNotice(MainActivity.this, time[0], time[1]);
                                Toast.makeText(MainActivity.this, String.format(Locale.KOREA, "매일 %s %d시 %d분 알림이 뜹니다.", time[0] < 12 ? "오전" : "오후", (time[0] + 11) % 12 + 1, time[1]), Toast.LENGTH_LONG).show();
                            } else if(enable) {
                                Toast.makeText(MainActivity.this, "알림이 뜨지 않습니다.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .setNegativeButton("취소", null)
                    .show();
        } else if (id == R.id.mIsignOut) {
            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            intent.putExtra("signed_out", true);
            startActivityForResult(intent, REQUEST_SIGN_IN);
        } else if (id == R.id.mIexit) {
            finish();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (vPmain.getCurrentItem() == 0 && cowListFragment != null && cowListFragment.isDeletedList()) {
            cowListFragment.setIsDeletedList(false);
            adapter.notifyDataSetChanged();
        } else {
            super.onBackPressed();
        }
    }

    public class PagerAdapter extends FragmentPagerAdapter {
        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position) {
                case 0: return cowListFragment;
                case 1: return calendarFragment;
                default: return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            switch(position) {
                case 0:
                    if (cowListFragment != null && cowListFragment.isDeletedList())
                        return "삭제된 개체 목록";
                    return "개체 목록";
                case 1: return "달력";
                default: return null;
            }
        }
    }
}
