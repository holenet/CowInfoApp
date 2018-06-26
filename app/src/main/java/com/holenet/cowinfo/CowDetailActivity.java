package com.holenet.cowinfo;

import android.content.Intent;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import com.holenet.cowinfo.item.Cow;

import java.util.List;

public class CowDetailActivity extends AppCompatActivity {
    private ViewPager vPcowDetail;
    private PagerAdapter adapter;

    private List<Cow> cows;
    private int currentPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cow_detail);

        Intent intent = getIntent();
        cows = (List<Cow>) intent.getSerializableExtra("cow_list");
        currentPosition = intent.getIntExtra("position", 0);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        adapter = new PagerAdapter(getSupportFragmentManager());
        vPcowDetail = findViewById(R.id.vPcowDetail);
        vPcowDetail.setAdapter(adapter);
        vPcowDetail.setCurrentItem(currentPosition);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_cow_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.mIedit) {
            // TODO: start CowEditActivity
        } else if (id == R.id.mIdelete) {
            // TODO: delete cow instance
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public static class CowDetailFragment extends Fragment {
        private static final String ARG_COW = "cow";
        private Cow cow;

        public static CowDetailFragment newInstance(Cow cow) {
            CowDetailFragment fragment = new CowDetailFragment();
            Bundle args = new Bundle();
            args.putSerializable(ARG_COW, cow);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            cow = (Cow) getArguments().getSerializable(ARG_COW);

            View rootView = inflater.inflate(R.layout.fragment_cow_detail, container, false);
            TextView tVnumber = rootView.findViewById(R.id.tVnumber);
            tVnumber.setText(cow.number);

            return rootView;
        }
    }

    public class PagerAdapter extends FragmentStatePagerAdapter {
        PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return CowDetailFragment.newInstance(cows.get(position));
        }

        @Override
        public int getCount() {
            return cows.size();
        }
    }
}
