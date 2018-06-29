package com.holenet.cowinfo;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.TextView;

import com.holenet.cowinfo.item.Cow;
import com.holenet.cowinfo.item.Record;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        if (id == R.id.mIdelete) {
            // TODO: delete cow instance
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public static class CowDetailFragment extends Fragment {
        public static final int REQUEST_UPDATE_COW = 400;
        public static final int REQUEST_CREATE_RECORD = 401;
        public static final int REQUEST_UPDATE_RECORD = 402;
        public static final int MENU_UPDATE_RECORD = 300;
        public static final int MENU_DELETE_RECORD = 301;

        private static final String ARG_COW = "cow";
        private Cow cow;
        private RecordRecyclerAdapter adapter;

        private TextView tVnumber, tVmotherNumber, tVbirthday;

        private GetCowTask getCowTask;

        private OnCowUpdatedListener onCowUpdatedListener;

        public static CowDetailFragment newInstance(Cow cow, OnCowUpdatedListener onCowUpdatedListener) {
            CowDetailFragment fragment = new CowDetailFragment();
            Bundle args = new Bundle();
            args.putSerializable(ARG_COW, cow);
            fragment.setArguments(args);
            fragment.onCowUpdatedListener = onCowUpdatedListener;
            return fragment;
        }

        @Override
        public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_cow_detail, container, false);
            Context context = view.getContext();

            Cow cow = (Cow) getArguments().getSerializable(ARG_COW);

            tVnumber = view.findViewById(R.id.tVnumber);
            tVmotherNumber = view.findViewById(R.id.tVmotherNumber);
            tVbirthday = view.findViewById(R.id.tVbirthday);

            adapter = new RecordRecyclerAdapter(cow.id, new ArrayList<Record>());
            RecyclerView rVrecordList = view.findViewById(R.id.rVrecordList);
            rVrecordList.setLayoutManager(new LinearLayoutManager(context));
            rVrecordList.setAdapter(adapter);

            Button bTeditInfo = view.findViewById(R.id.bTeditInfo);
            bTeditInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(CowDetailFragment.this.getContext(), EditCowActivity.class);
                    intent.putExtra("edit_mode", EditCowActivity.MODE_UPDATE);
                    intent.putExtra("cow", CowDetailFragment.this.cow);
                    startActivityForResult(intent, REQUEST_UPDATE_COW);
                }
            });

            Button bTaddRecord = view.findViewById(R.id.bTaddRecord);
            bTaddRecord.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(CowDetailFragment.this.getContext(), EditRecordActivity.class);
                    intent.putExtra("cow_id", CowDetailFragment.this.cow.id);
                    intent.putExtra("edit_mode", EditRecordActivity.MODE_CREATE);
                    startActivityForResult(intent, REQUEST_CREATE_RECORD);
                }
            });

            updateInfo(cow);

            return view;
        }

        private void attemptGetCow() {
            if (getCowTask != null) {
                return;
            }

            getCowTask = new GetCowTask(this);
            getCowTask.execute(cow.id);
        }

        private void updateInfo(Cow cow) {
            this.cow = cow;
            if (onCowUpdatedListener != null) {
                onCowUpdatedListener.onCowUpdated(cow);
            }

            tVnumber.setText(cow.number);
            tVmotherNumber.setText(cow.mother_number);
            tVbirthday.setText(cow.getKoreanBirthday());

            adapter.setItems(cow.records);
        }

        @Override
        public boolean onContextItemSelected(MenuItem item) {
            if (item.getGroupId() != cow.id)
                return false;
            final int index = item.getIntent().getIntExtra("index", -1);
            if (index == -1)
                return false;
            final Record record = adapter.getItem(index);
            final int itemId = item.getItemId();
            if (itemId == MENU_UPDATE_RECORD) {
                Intent intent = new Intent(CowDetailFragment.this.getContext(), EditRecordActivity.class);
                intent.putExtra("cow_id", cow.id);
                intent.putExtra("edit_mode", EditRecordActivity.MODE_UPDATE);
                intent.putExtra("record", record);
                startActivityForResult(intent, REQUEST_UPDATE_RECORD);
            } else if (itemId == MENU_DELETE_RECORD) {
                // TODO: show confirmation dialog and request delete record
            } else {
                return false;
            }
            return true;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == REQUEST_CREATE_RECORD ||
                    requestCode == REQUEST_UPDATE_COW ||
                    requestCode == REQUEST_UPDATE_RECORD) {
                if (resultCode == RESULT_OK) {
                    attemptGetCow();
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }

        static class RecordRecyclerAdapter extends RecyclerView.Adapter<RecordRecyclerAdapter.ViewHolder> {
            private final int cowId;
            private final List<Record> records;

            public RecordRecyclerAdapter(int cowId, List<Record> items) {
                this.cowId = cowId;
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

            public Record getItem(int index) {
                return records.get(index);
            }

            public void setItems(List<Record> records) {
                this.records.clear();
                this.records.addAll(records);
                notifyDataSetChanged();
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
                    view.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                        @Override
                        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                            Intent intent = new Intent();
                            intent.putExtra("index", getAdapterPosition());
                            menu.add(cowId, MENU_UPDATE_RECORD, Menu.NONE, "이력 수정").setIntent(intent);
                            menu.add(cowId, MENU_DELETE_RECORD, Menu.NONE, "이력 삭제").setIntent(intent);
                        }
                    });

                    tVcontent = view.findViewById(R.id.tVcontent);
                    tVetc = view.findViewById(R.id.tVetc);
                    tVday = view.findViewById(R.id.tVday);
                }
            }
        }

        private static class GetCowTask extends NetworkService.Task<CowDetailFragment, Integer, Cow> {
            public GetCowTask(CowDetailFragment holder) {
                super(holder);
            }

            @Override
            protected NetworkService.Result<Cow> request(Integer cowId) {
                return NetworkService.getCow(cowId);
            }

            @Override
            protected void responseInit(boolean isSuccessful) {
                getHolder().getCowTask = null;
            }

            @Override
            protected void responseSuccess(Cow cow) {
                getHolder().updateInfo(cow);
            }

            @Override
            protected void responseFail(Map<String, String> errors) {
                existErrors(errors, getHolder().getContext());
            }
        }

        public interface OnCowUpdatedListener {
            void onCowUpdated(Cow cow);
        }
    }

    public class PagerAdapter extends FragmentStatePagerAdapter {
        PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(final int position) {
            return CowDetailFragment.newInstance(cows.get(position), new CowDetailFragment.OnCowUpdatedListener() {
                @Override
                public void onCowUpdated(Cow cow) {
                    cows.set(position, cow);
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public int getCount() {
            return cows.size();
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return cows.get(position).getSummary();
        }
    }
}
