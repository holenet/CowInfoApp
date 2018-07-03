package com.holenet.cowinfo;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.holenet.cowinfo.item.Record;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.holenet.cowinfo.NetworkService.constructDate;
import static com.holenet.cowinfo.NetworkService.destructDate;

public class EditRecordActivity extends AppCompatActivity {
    public static int MODE_CREATE = 301;
    public static int MODE_UPDATE = 302;

    public static String[] contents = new String[] {
            "수정",
            "재발",
            "분만",
            "기타",
    };

    /**
     * Helper class for saving multiple Records at the same time.
     */
    private class SaveRecordTaskInfo {
        private List<Record> records;
        private int count;
        private boolean isSuccessFul;

        SaveRecordTaskInfo() {
            this.records = new ArrayList<>();
        }

        public void addRecord(Record record) {
            records.add(record);
        }

        public void execute(EditRecordActivity holder) {
            count = records.size();
            isSuccessFul = true;
            for (Record record: records) {
                SaveRecordTask task = new SaveRecordTask(holder);
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, record);
            }
        }

        public boolean notifyResponse(boolean isSuccessFul) {
            if (!isSuccessFul) {
                this.isSuccessFul = false;
            }
            return (--count) == 0;
        }

        public boolean isSuccessFul() {
            return isSuccessFul;
        }
    }

    private SaveRecordTaskInfo saveRecordTaskInfo;

    private TextView tVcow;
    private Spinner sPcontent;
    private EditText eTetc;
    private TextView tVday;
    private CheckBox cBaddJB, cBaddBM;
    private TextView tVJB, tVBM;
    private int year, month, day;
    private int yearJB, monthJB, dayJB;
    private int yearBM, monthBM, dayBM;

    private int cowId;
    private Record record;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_record);

        setResult(RESULT_CANCELED);

        tVcow = findViewById(R.id.tVcow);
        sPcontent = findViewById(R.id.sPcontent);
        eTetc = findViewById(R.id.eTetc);
        tVday = findViewById(R.id.tVday);
        cBaddJB = findViewById(R.id.cBaddJB);
        cBaddBM = findViewById(R.id.cBaddBM);
        tVJB = findViewById(R.id.tVJB);
        tVBM = findViewById(R.id.tVBM);
        Button bTeditDay = findViewById(R.id.bTeditDay);
        Button bTcancel = findViewById(R.id.bTcancel);
        Button bTconfirm = findViewById(R.id.bTconfirm);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, contents);
        sPcontent.setAdapter(adapter);
        sPcontent.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            ColorStateList defaultColor = tVJB.getTextColors();

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                cBaddJB.setChecked(position <= 1 && cBaddJB.isChecked());
                cBaddJB.setEnabled(position <= 1);
                tVJB.setTextColor(position <= 1 ? defaultColor : ColorStateList.valueOf(Color.LTGRAY));
                cBaddBM.setChecked(position <= 0 && cBaddBM.isChecked());
                cBaddBM.setEnabled(position <= 0);
                tVBM.setTextColor(position <= 0 ? defaultColor : ColorStateList.valueOf(Color.LTGRAY));
                eTetc.requestFocus();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        Calendar today = Calendar.getInstance();
        setDates(today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH));

        bTeditDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog dialog = new DatePickerDialog(EditRecordActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        setDates(year, month + 1, dayOfMonth);
                    }
                }, year, month - 1, day);
                dialog.show();
            }
        });

        bTcancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        bTconfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptSave();
            }
        });

        Intent intent = getIntent();
        cowId = intent.getIntExtra("cow_id", -1);
        if (cowId == -1) {
            finish();
        }
        int edit_mode = intent.getIntExtra("edit_mode", 0);
        if (edit_mode == MODE_CREATE) {
            getSupportActionBar().setTitle("새로운 이력 추가");

            tVcow.setText(intent.getStringExtra("cow_summary"));
        } else if (edit_mode == MODE_UPDATE) {
            getSupportActionBar().setTitle("이력 정보 수정");
            record = (Record) intent.getSerializableExtra("record");

            tVcow.setText(record.cow_summary);

            sPcontent.setSelection(Arrays.asList(contents).indexOf(record.content));

            if (record.etc != null) {
                eTetc.setText(record.etc);
            }

            int[] days = destructDate(record.day);
            setDates(days[0], days[1], days[2]);
        } else {
            finish();
        }
    }

    private void setDates(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
        tVday.setText(String.format(Locale.KOREA, "%d년 %d월 %d일", year, month, day));

        Calendar calJB = Calendar.getInstance();
        calJB.set(year, month - 1, day);
        calJB.add(Calendar.DAY_OF_MONTH, 21);
        this.yearJB = calJB.get(Calendar.YEAR);
        this.monthJB = calJB.get(Calendar.MONTH) + 1;
        this.dayJB = calJB.get(Calendar.DAY_OF_MONTH);
        tVJB.setText(String.format(Locale.KOREA, "%d년 %d월 %d일", yearJB, monthJB, dayJB));

        Calendar calBM = Calendar.getInstance();
        calBM.set(year, month - 1, day);
        calBM.add(Calendar.DAY_OF_MONTH, 285);
        this.yearBM = calBM.get(Calendar.YEAR);
        this.monthBM = calBM.get(Calendar.MONTH) + 1;
        this.dayBM = calBM.get(Calendar.DAY_OF_MONTH);
        tVBM.setText(String.format(Locale.KOREA, "%d년 %d월 %d일", yearBM, monthBM, dayBM));
    }

    private void attemptSave() {
        if (saveRecordTaskInfo != null) {
            return;
        }
        saveRecordTaskInfo = new SaveRecordTaskInfo();

        int position = sPcontent.getSelectedItemPosition();
        String content = contents[position];

        String etc = eTetc.getText().toString();

        if (cBaddJB.isChecked()) {
            Record recordJB = new Record("재발", "(추정)", constructDate(yearJB, monthJB, dayJB), cowId);
            saveRecordTaskInfo.addRecord(recordJB);
        }
        if (cBaddBM.isChecked()) {
            Record recordBM = new Record("분만", "(추정)", constructDate(yearBM, monthBM, dayBM), cowId);
            saveRecordTaskInfo.addRecord(recordBM);
        }

        Record record;
        if (this.record == null) {
            record = new Record(content, etc, constructDate(year, month, day), cowId);
        } else {
            record = new Record(this.record, content, etc, constructDate(year, month, day), cowId);
        }
        saveRecordTaskInfo.addRecord(record);

        saveRecordTaskInfo.execute(this);
    }

    private static class SaveRecordTask extends NetworkService.Task<EditRecordActivity, Record, Record> {
        public SaveRecordTask(EditRecordActivity holder) {
            super(holder);
        }

        @Override
        protected NetworkService.Result<Record> request(Record record) {
            if (record.id == null) {
                return NetworkService.createRecord(record);
            } else {
                return NetworkService.updateRecord(record);
            }
        }

        @Override
        protected void responseInit(boolean isSuccessful) {
            if (getHolder().saveRecordTaskInfo.notifyResponse(isSuccessful)) {
                if (getHolder().saveRecordTaskInfo.isSuccessFul()) {
                    getHolder().setResult(RESULT_OK);
                    getHolder().finish();
                } else {
                    Toast.makeText(getHolder(), "저장에 실패하였습니다. 잠시 후 다시 시도해 주세요", Toast.LENGTH_SHORT).show();
                    getHolder().saveRecordTaskInfo = null;
                }
            }
        }

        @Override
        protected void responseSuccess(Record record) {
        }

        @Override
        protected void responseFail(Map<String, String> errors) {
        }
    }
}
