package com.holenet.cowinfo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.holenet.cowinfo.item.Cow;
import com.holenet.cowinfo.item.User;

public class MainActivity extends AppCompatActivity {
    final static int REQUEST_SIGN_IN = 101;
    final static int REQUEST_CREATE_COW = 102;

    private ViewPager vPmain;

    private CowListFragment cowListFragment;
//    private CalendarFragment calendarFragment;
    private PagerAdapter adapter;

    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        cowListFragment = CowListFragment.newInstance(false);

        adapter = new PagerAdapter(getSupportFragmentManager());
        vPmain = findViewById(R.id.vPmain);
        vPmain.setAdapter(adapter);

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

    Menu menu;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
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
        } else if (id == R.id.mIrefresh) {
            if (cowListFragment != null) {
                cowListFragment.attemptGetCowList();
            }
        } else if (id == R.id.mIdeleted) {
            cowListFragment.setIsDeletedList(true);
            menu.findItem(R.id.mIdeleted).setVisible(false);
            adapter.notifyDataSetChanged();
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
        if (cowListFragment != null && cowListFragment.isDeletedList()) {
            cowListFragment.setIsDeletedList(false);
            menu.findItem(R.id.mIdeleted).setVisible(true);
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
//                case 1: return calendarFragment;
                default: return null;
            }
        }

        @Override
        public int getCount() {
//            return 2;
            return 1;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            switch(position) {
                case 0:
                    if (cowListFragment != null && cowListFragment.isDeletedList())
                        return "휴지통 개체 목록";
                    return "개체 목록";
                case 1: return "달력";
                default: return null;
            }
        }
    }
}
