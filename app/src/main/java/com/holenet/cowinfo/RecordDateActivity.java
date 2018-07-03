package com.holenet.cowinfo;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.holenet.cowinfo.item.Record;
import com.prolificinteractive.materialcalendarview.CalendarDay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.holenet.cowinfo.NetworkService.destructDate;

public class RecordDateActivity extends AppCompatActivity {
    private ViewPager vPrecordDate;
    private PagerAdapter adapter;

    private List<CalendarDay> dateList;
    private List<List<Record>> recordsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_date);

        Intent intent = getIntent();
        List<Record> records = (List<Record>) intent.getSerializableExtra("record_list");
        constructDateMap(records);

        CalendarDay initDate = intent.getParcelableExtra("date");
        int position;
        for (position = 0; position < dateList.size(); ++position) {
            if (dateList.get(position).equals(initDate)) {
                break;
            }
        }
        if (position == dateList.size()) {
            finish();
        }

        adapter = new PagerAdapter(getSupportFragmentManager());
        vPrecordDate = findViewById(R.id.vPrecordDate);
        vPrecordDate.setAdapter(adapter);
        vPrecordDate.setCurrentItem(position);
        vPrecordDate.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                Intent intent = new Intent();
                intent.putExtra("date", dateList.get(position));
                setResult(RESULT_OK, intent);
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });
    }

    private void constructDateMap(List<Record> records) {
        HashMap<CalendarDay, List<Record>> dateMap = new HashMap<>();
        for (Record record : records) {
            int[] date = destructDate(record.day);
            CalendarDay key = CalendarDay.from(date[0], date[1] - 1, date[2]);
            if (!dateMap.containsKey(key)) {
                dateMap.put(key, new ArrayList<Record>());
            }
            dateMap.get(key).add(record);
        }

        dateList = new ArrayList<>(dateMap.keySet());
        Collections.sort(dateList, new Comparator<CalendarDay>() {
            @Override
            public int compare(CalendarDay o1, CalendarDay o2) {
                return o1.hashCode() - o2.hashCode();
            }
        });

        recordsList = new ArrayList<>();
        for (CalendarDay date : dateList) {
            recordsList.add(dateMap.get(date));
        }
    }

    public class PagerAdapter extends FragmentStatePagerAdapter {
        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(final int position) {
            RecordDateFragment fragment = RecordDateFragment.newInstance(dateList.get(position), recordsList.get(position));
            fragment.setOnRecordsUpdatedListener(new RecordDateFragment.OnRecordsUpdatedListener() {
                @Override
                public void onRecordsUpdated(List<Record> records) {
                    recordsList.set(position, records);
                }
            });
            return fragment;
        }

        @Override
        public int getCount() {
            return dateList.size();
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            CalendarDay date = dateList.get(position);
            return String.format(Locale.KOREA, "%d월 %d일", date.getMonth() + 1, date.getDay());
        }
    }

    public static class RecordDateFragment extends Fragment {
        public static final int REQUEST_COW_DETAIL = 401;
        public static final int REQUEST_UPDATE_RECORD = 402;
        public static final int MENU_UPDATE_RECORD = 300;
        public static final int MENU_DELETE_RECORD = 301;
        public static final int MENU_COW_DETAIL = 302;

        private static final String ARG_DATE = "date";
        private static final String ARG_RECORDS = "records";
        private CalendarDay date;
        private List<Record> records;
        private RecordRecyclerAdapter adapter;

        private GetRecordsTask getRecordsTask;

        private OnRecordsUpdatedListener onRecordsUpdatedListener;

        public static RecordDateFragment newInstance(CalendarDay date, List<Record> records) {
            RecordDateFragment fragment = new RecordDateFragment();
            Bundle args = new Bundle();
            args.putParcelable(ARG_DATE, date);
            args.putSerializable(ARG_RECORDS, (ArrayList<Record>) records);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setHasOptionsMenu(true);

            Bundle arguments = getArguments();
            if (arguments != null) {
                date = arguments.getParcelable(ARG_DATE);
                records = (List<Record>) arguments.getSerializable(ARG_RECORDS);
            }
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_record_date, container, false);

            TextView tVdate = view.findViewById(R.id.tVdate);
            tVdate.setText(String.format(Locale.KOREA, "%d년 %d월 %d일", date.getYear(), date.getMonth() + 1, date.getDay()));

            adapter = new RecordRecyclerAdapter(date, records);
            RecyclerView rVrecordList = view.findViewById(R.id.rVrecordList);
            rVrecordList.setLayoutManager(new LinearLayoutManager(getContext()));
            rVrecordList.setAdapter(adapter);

            return view;
        }

        private void attemptGetRecords() {
            if (getRecordsTask != null) {
                return;
            }

            getRecordsTask = new GetRecordsTask(this);
            getRecordsTask.execute(String.format(Locale.KOREA, "%d-%d-%d",  date.getYear(), date.getMonth() + 1, date.getDay()));
        }

        private void attemptDestroyRecord(Record record) {
            DestroyRecordTask task = new DestroyRecordTask(this);
            task.execute(record.id);
        }

        private void updateInfo(List<Record> records) {
            this.records.clear();
            this.records.addAll(records);
            if (onRecordsUpdatedListener != null) {
                onRecordsUpdatedListener.onRecordsUpdated(records);
            }

            adapter.notifyDataSetChanged();
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.menu_record_date, menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            final int id = item.getItemId();
            if (id == R.id.mIrefresh) {
                attemptGetRecords();
            } else {
                return super.onOptionsItemSelected(item);
            }
            return true;
        }

        @Override
        public boolean onContextItemSelected(MenuItem item) {
            if (item.getGroupId() != date.hashCode())
                return false;
            final int index = item.getIntent().getIntExtra("index", -1);
            if (index == -1)
                return false;
            final Record record = adapter.getItem(index);
            final int itemId = item.getItemId();
            if (itemId == MENU_UPDATE_RECORD) {
                Intent intent = new Intent(getContext(), EditRecordActivity.class);
                intent.putExtra("cow_id", record.cow);
                intent.putExtra("edit_mode", EditRecordActivity.MODE_UPDATE);
                intent.putExtra("record", record);
                startActivityForResult(intent, REQUEST_UPDATE_RECORD);
            } else if (itemId == MENU_DELETE_RECORD) {
                new AlertDialog.Builder(this.getContext())
                        .setTitle(record.cow_number)
                        .setMessage(String.format("%s\n%s\n%s\n삭제하시겠습니까?", record.getKoreanDay(), record.content, record.etc))
                        .setPositiveButton("네", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                attemptDestroyRecord(record);
                            }
                        })
                        .setNegativeButton("아니오", null)
                        .show();
            } else if (itemId == MENU_COW_DETAIL) {
                Intent intent = new Intent(getContext(), CowDetailActivity.class);
                intent.putExtra("cow_id", record.cow);
                startActivityForResult(intent, REQUEST_COW_DETAIL);
            } else {
                return false;
            }
            return true;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == REQUEST_UPDATE_RECORD) {
                if (resultCode == RESULT_OK) {
                    attemptGetRecords();
                }
            } else if (requestCode == REQUEST_COW_DETAIL) {
                attemptGetRecords();
            }
            super.onActivityResult(requestCode, resultCode, data);
        }

        static class RecordRecyclerAdapter extends RecyclerView.Adapter<RecordRecyclerAdapter.ViewHolder> {
            private final int date;
            private final List<Record> records;

            public RecordRecyclerAdapter(CalendarDay date, List<Record> records) {
                this.date = date.hashCode();
                this.records = records;
            }

            @NonNull
            @Override
            public RecordRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_record_date, parent, false);
                return new ViewHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull RecordRecyclerAdapter.ViewHolder holder, int position) {
                final Record record = records.get(position);
                holder.tVcontent.setText(record.content);
                holder.tVetc.setText(record.etc);
                holder.tVcow.setText(record.cow_summary);
            }

            public Record getItem(int index) {
                return records.get(index);
            }

            @Override
            public int getItemCount() {
                return records.size();
            }

            public class ViewHolder extends RecyclerView.ViewHolder {
                public final View view;
                final TextView tVcontent, tVetc, tVcow;

                ViewHolder(View view) {
                    super(view);
                    this.view = view;
                    view.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                        @Override
                        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                            Intent intent = new Intent();
                            intent.putExtra("index", getAdapterPosition());
                            menu.add(date, MENU_UPDATE_RECORD, Menu.NONE, "이력 수정").setIntent(intent);
                            menu.add(date, MENU_DELETE_RECORD, Menu.NONE, "이력 삭제").setIntent(intent);
                            menu.add(date, MENU_COW_DETAIL, Menu.NONE, "개체 정보").setIntent(intent);
                        }
                    });

                    tVcontent = view.findViewById(R.id.tVcontent);
                    tVetc = view.findViewById(R.id.tVetc);
                    tVcow = view.findViewById(R.id.tVcow);
                }
            }
        }

        private static class GetRecordsTask extends NetworkService.Task<RecordDateFragment, String, List<Record>> {
            public GetRecordsTask(RecordDateFragment holder) {
                super(holder);
            }

            @Override
            protected NetworkService.Result<List<Record>> request(String day) {
                return NetworkService.getRecordList(false, day);
            }

            @Override
            protected void responseInit(boolean isSuccessful) {
                getHolder().getRecordsTask = null;
            }

            @Override
            protected void responseSuccess(List<Record> records) {
                getHolder().updateInfo(records);
            }

            @Override
            protected void responseFail(Map<String, String> errors) {
                existErrors(errors, getHolder().getContext());
            }
        }

        private static class DestroyRecordTask extends NetworkService.Task<RecordDateFragment, Integer, Void> {
            public DestroyRecordTask(RecordDateFragment holder) {
                super(holder);
            }

            @Override
            protected NetworkService.Result<Void> request(Integer recordId) {
                return NetworkService.destroyRecord(recordId);
            }

            @Override
            protected void responseInit(boolean isSuccessful) {}

            @Override
            protected void responseSuccess(Void aVoid) {
                getHolder().attemptGetRecords();
            }

            @Override
            protected void responseFail(Map<String, String> errors) {
                if (existErrors(errors, getHolder().getContext())) {
                    Toast.makeText(getHolder().getContext(), "삭제에 실패하였습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                }
            }
        }

        public interface OnRecordsUpdatedListener {
            void onRecordsUpdated(List<Record> records);
        }

        public void setOnRecordsUpdatedListener(OnRecordsUpdatedListener onRecordsUpdatedListener) {
            this.onRecordsUpdatedListener = onRecordsUpdatedListener;
        }
    }
}
