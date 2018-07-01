package com.holenet.cowinfo;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.holenet.cowinfo.item.Record;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static android.app.Activity.RESULT_OK;
import static com.holenet.cowinfo.NetworkService.destructDate;

public class CalendarFragment extends Fragment {
    public static final int REQUEST_RECORD_DATE = 601;

    private List<Record> records = new ArrayList<>();
    private HashSet<CalendarDay> dateSet = new HashSet<>();

    private GetRecordListTask getRecordListTask;

    private MaterialCalendarView mCVrecords;
    private DayViewDecorator recordDecorator;

    public static CalendarFragment newInstance() {
        CalendarFragment fragment = new CalendarFragment();
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        recordDecorator = new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                return dateSet.contains(day);
            }

            @Override
            public void decorate(DayViewFacade view) {
                view.addSpan(new DotSpan(10, Color.MAGENTA));
            }
        };

        attemptGetRecordList();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        mCVrecords = view.findViewById(R.id.mCVrecords);
        mCVrecords.addDecorators(new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                return day.getCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
            }

            @Override
            public void decorate(DayViewFacade view) {
                view.addSpan(new ForegroundColorSpan(Color.RED));
            }
        }, new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                return day.getCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY;
            }

            @Override
            public void decorate(DayViewFacade view) {
                view.addSpan(new ForegroundColorSpan(Color.BLUE));
            }
        }, new DayViewDecorator() {
            CalendarDay today = CalendarDay.today();

            @Override
            public boolean shouldDecorate(CalendarDay day) {
                return day.equals(today);
            }

            @Override
            public void decorate(DayViewFacade view) {
                view.addSpan(new ForegroundColorSpan(Color.rgb(0, 175, 50)));
            }
        });
        mCVrecords.addDecorator(recordDecorator);
        mCVrecords.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                if (!dateSet.contains(date)) {
                    mCVrecords.clearSelection();
                    return;
                }
                Intent intent = new Intent(getContext(), RecordDateActivity.class);
                intent.putExtra("record_list", (ArrayList<Record>) records);
                intent.putExtra("date", date);
                startActivityForResult(intent, REQUEST_RECORD_DATE);
            }
        });

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_calendar, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.mIrefresh) {
            attemptGetRecordList();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public void attemptGetRecordList() {
        if (getRecordListTask != null) {
            return;
        }

        getRecordListTask = new GetRecordListTask(this);
        getRecordListTask.execute();
    }

    private void setRecordList(List<Record> records) {
        this.records.clear();
        this.records.addAll(records);

        dateSet.clear();
        for (Record record : records) {
            int[] date = destructDate(record.day);
            dateSet.add(CalendarDay.from(date[0], date[1] - 1, date[2]));
        }

        if (mCVrecords != null)
            mCVrecords.invalidateDecorators();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RECORD_DATE) {
            attemptGetRecordList();
            if (resultCode == RESULT_OK) {
                CalendarDay date = data.getParcelableExtra("date");
                mCVrecords.setSelectedDate(date);
                mCVrecords.setCurrentDate(date);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private static class GetRecordListTask extends NetworkService.Task<CalendarFragment, Void, List<Record>> {
        public GetRecordListTask(CalendarFragment holder) {
            super(holder);
        }

        @Override
        protected NetworkService.Result<List<Record>> request(Void aVoid) {
            return NetworkService.getRecordList(false);
        }

        @Override
        protected void responseInit(boolean isSuccessful) {
            getHolder().getRecordListTask = null;
        }

        @Override
        protected void responseSuccess(List<Record> records) {
            getHolder().setRecordList(records);
        }

        @Override
        protected void responseFail(Map<String, String> errors) {
            existErrors(errors, getHolder().getContext());
        }
    }
}
