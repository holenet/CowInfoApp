package com.holenet.cowinfo;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.style.ForegroundColorSpan;
import android.text.style.LineBackgroundSpan;
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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.app.Activity.RESULT_OK;
import static com.holenet.cowinfo.NetworkService.destructDate;

public class CalendarFragment extends Fragment {
    public static final int REQUEST_RECORD_DATE = 601;
    public int[] recordColors;

    private List<Record> records = new ArrayList<>();
    @SuppressWarnings("unchecked")
    private Set<CalendarDay>[] dateSets = (Set<CalendarDay>[]) Array.newInstance(Set.class, 16);

    private GetRecordListTask getRecordListTask;

    private MaterialCalendarView mCVrecords;
    private DayViewDecorator sunDecorator, satDecorator, todayDecorator;

    public static CalendarFragment newInstance() {
        CalendarFragment fragment = new CalendarFragment();
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        sunDecorator = new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                return day.getCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
            }

            @Override
            public void decorate(DayViewFacade view) {
                view.addSpan(new ForegroundColorSpan(Color.RED));
            }
        };
        satDecorator = new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                return day.getCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY;
            }

            @Override
            public void decorate(DayViewFacade view) {
                view.addSpan(new ForegroundColorSpan(Color.BLUE));
            }
        };
        todayDecorator = new DayViewDecorator() {
            CalendarDay today = CalendarDay.today();

            @Override
            public boolean shouldDecorate(CalendarDay day) {
                return day.equals(today);
            }

            @Override
            public void decorate(DayViewFacade view) {
                view.addSpan(new ForegroundColorSpan(Color.rgb(0, 175, 50)));
            }
        };

        for (int i = 0; i < 16; ++i) {
            dateSets[i] = new HashSet<>();
        }

        recordColors = getResources().getIntArray(R.array.recordColors);

        attemptGetRecordList();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        mCVrecords = view.findViewById(R.id.mCVrecords);
        mCVrecords.addDecorators(sunDecorator, satDecorator, todayDecorator);
        for (int i = 1; i < 16; ++i) {
            mCVrecords.addDecorator(new RecordDecorator(i));
        }
        mCVrecords.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                if (!dateSets[0].contains(date)) {
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

        for (int i = 0; i < 16; ++i)
            dateSets[i].clear();
        Map<CalendarDay, Integer> dateIndex = new HashMap<>();

        for (Record record : records) {
            int[] date = destructDate(record.day);
            CalendarDay day = CalendarDay.from(date[0], date[1] - 1, date[2]);

            int index = 0;
            if ("수정".equals(record.content)) {
                index = 1;
            } else if ("재발".equals(record.content)) {
                index = 2;
            } else if ("분만".equals(record.content)) {
                index = 4;
            } else if ("기타".equals(record.content)) {
                index = 8;
            }

            if (dateIndex.containsKey(day)) {
                index |= dateIndex.get(day);
            }
            if (index < 0 | index > 15) {
                throw new IllegalStateException("Date index should be 1~15");
            }
            dateIndex.put(day, index);
        }

        for (Map.Entry<CalendarDay, Integer> entry : dateIndex.entrySet()) {
            CalendarDay day = entry.getKey();
            dateSets[0].add(day);
            int index = entry.getValue();
            dateSets[index].add(day);
        }

        if (mCVrecords != null) {
            mCVrecords.invalidateDecorators();
        }
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

    public class ColorfulCakeSpan implements LineBackgroundSpan {
        private final float radius;
        private final int[] colors;

        public ColorfulCakeSpan(int[] colors) {
            this(DotSpan.DEFAULT_RADIUS, colors);
        }

        public ColorfulCakeSpan(float radius, int[] colors) {
            if (colors.length == 0) {
                throw new IllegalArgumentException("At least one color should be provided.");
            }
            this.radius = radius;
            this.colors = colors;
        }

        @Override
        public void drawBackground(Canvas canvas, Paint paint, int left, int right, int top, int baseline, int bottom, CharSequence charSequence, int start, int end, int lineNum) {
            final float piece = 360f / colors.length;
            final float cx = (left + right) / 2.f;
            final float cy = bottom + radius;

            int oldColor = paint.getColor();
            canvas.save();
            canvas.rotate(-90, cx, cy);

            for (int i = 0; i < colors.length; ++i) {
                paint.setColor(colors[i]);
                canvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius, i * piece, piece, true, paint);
            }

            canvas.restore();
            paint.setColor(oldColor);
        }
    }

    class RecordDecorator implements DayViewDecorator {
        private final int index;
        private final ColorfulCakeSpan span;

        public RecordDecorator(int index) {
            this.index = index;
            List<Integer> colorList = new ArrayList<>();
            int cnt = 0;
            while (index > 0) {
                if (index % 2 == 1) {
                    colorList.add(recordColors[cnt]);
                }
                index /= 2;
                cnt++;
            }
            int[] colors = new int[colorList.size()];
            for (int i = 0; i < colors.length; ++i) {
                colors[i] = colorList.get(i);
            }
            this.span = new ColorfulCakeSpan(15, colors);
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return dateSets[index].contains(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(span);
        }
    }
}
