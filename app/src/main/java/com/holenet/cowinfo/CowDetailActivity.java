package com.holenet.cowinfo;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import com.holenet.cowinfo.item.Record;

import java.util.List;

public class CowDetailActivity extends AppCompatActivity {
    private ViewPager vPcowDetail;
    private PagerAdapter adapter;

    private List<Cow> cows;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cow_detail);

        Intent intent = getIntent();
        cows = (List<Cow>) intent.getSerializableExtra("cow_list");
        int initPosition = intent.getIntExtra("position", 0);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        adapter = new PagerAdapter(getSupportFragmentManager());
        vPcowDetail = findViewById(R.id.vPcowDetail);
        vPcowDetail.setAdapter(adapter);
        vPcowDetail.setCurrentItem(initPosition);
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
        private RecordRecyclerAdapter adapter;

        public static CowDetailFragment newInstance(Cow cow) {
            CowDetailFragment fragment = new CowDetailFragment();
            Bundle args = new Bundle();
            args.putSerializable(ARG_COW, cow);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (getArguments() != null) {
                cow = (Cow) getArguments().getSerializable(ARG_COW);
            }
            if (cow != null) {
                adapter = new RecordRecyclerAdapter(cow.records);
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_cow_detail, container, false);
            Context context = view.getContext();

            TextView tVnumber = view.findViewById(R.id.tVnumber);
            TextView tVmotherNumber = view.findViewById(R.id.tVmotherNumber);
            TextView tVbirthday = view.findViewById(R.id.tVbirthday);
            tVnumber.setText(cow.number);
            tVmotherNumber.setText(cow.mother_number);
            tVbirthday.setText(cow.getKoreanBirthday());

            RecyclerView rVrecordList = view.findViewById(R.id.rVrecordList);
            rVrecordList.setLayoutManager(new LinearLayoutManager(context));
            rVrecordList.setAdapter(adapter);

            return view;
        }

        static class RecordRecyclerAdapter extends RecyclerView.Adapter<RecordRecyclerAdapter.ViewHolder> {
            private final List<Record> records;

            public RecordRecyclerAdapter(List<Record> items) {
                records = items;
            }

            @NonNull
            @Override
            public RecordRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_record, parent, false);
                return new ViewHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull RecordRecyclerAdapter.ViewHolder holder, int position) {
                final Record record = records.get(position);
                holder.tVcontent.setText(record.content);
                holder.tVetc.setText(record.etc);
                holder.tVday.setText(record.getKoreanDay());
            }

            @Override
            public int getItemCount() {
                return records.size();
            }

            public class ViewHolder extends RecyclerView.ViewHolder {
                public final View view;
                final TextView tVcontent, tVetc, tVday;

                ViewHolder(View view) {
                    super(view);
                    this.view = view;
                    tVcontent = view.findViewById(R.id.tVcontent);
                    tVetc = view.findViewById(R.id.tVetc);
                    tVday = view.findViewById(R.id.tVday);
                }
            }
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
