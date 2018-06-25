package com.holenet.cowinfo;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.holenet.cowinfo.item.Cow;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EditCowActivity extends AppCompatActivity {
    public static int MODE_CREATE = 301;
    public static int MODE_MODIFY = 302;

    private SaveCowTask saveCowTask;

    private RadioButton rBfemale;
    private EditText eTnumber, eTmotherNumber;
    private TextView tVbirthday;
    private int year, month, day;

    private Cow cow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_cow);

        rBfemale = findViewById(R.id.rBfemale);
        eTnumber = findViewById(R.id.eTnumber);
        eTmotherNumber = findViewById(R.id.eTmotherNumber);
        tVbirthday = findViewById(R.id.tVbirthday);
        Button bTeditBirthday = findViewById(R.id.bTeditBirthday);
        Button bTremoveBirthday = findViewById(R.id.bTremoveBirthday);
        Button bTcancel = findViewById(R.id.bTcancel);
        Button bTconfirm = findViewById(R.id.bTconfirm);

        eTnumber.setSelection(eTnumber.getText().length());
        eTmotherNumber.setSelection(eTmotherNumber.getText().length());

        eTnumber.addTextChangedListener(new ControlledTextWatcher(eTnumber));
        eTmotherNumber.addTextChangedListener(new ControlledTextWatcher(eTmotherNumber));

        connect(eTnumber, eTmotherNumber);

        Calendar today = Calendar.getInstance();
        year = today.get(Calendar.YEAR);
        month = today.get(Calendar.MONTH);
        day = today.get(Calendar.DAY_OF_MONTH);

        bTeditBirthday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog dialog = new DatePickerDialog(EditCowActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        EditCowActivity.this.year = year;
                        EditCowActivity.this.month = month + 1;
                        EditCowActivity.this.day = dayOfMonth;
                        tVbirthday.setText(String.format(Locale.KOREA, "%d년 %d월 %d일", year, month + 1, dayOfMonth));
                    }
                }, year, month, day);
                dialog.show();
            }
        });
        bTremoveBirthday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tVbirthday.setText("지정 안함");
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
        int edit_mode = intent.getIntExtra("edit_mode", 0);
        if (edit_mode == 0)
            finish();
        if (edit_mode == MODE_CREATE) {
            getSupportActionBar().setTitle("새로운 개체 추가");
            eTnumber.requestFocus();
            eTnumber.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(eTnumber, 0);
                }
            }, 100);

            setResult(RESULT_CANCELED);
        } else if (edit_mode == MODE_MODIFY) {
            getSupportActionBar().setTitle("개체 정보 수정");
            // TODO: Load cow instance from intent and apply onto UI
        } else {
            finish();
        }
    }

    private void attemptSave() {
        if (saveCowTask != null) {
            return;
        }

        String sex = rBfemale.isChecked() ? "female" : "male";

        String number = eTnumber.getText().toString();
        if (number.length() != 15) {
            eTnumber.setError("식별번호가 유효하지 않습니다.");
            return;
        }

        String motherNumber = eTmotherNumber.getText().toString();
        if (motherNumber.length() == 4) {
            motherNumber = "";
        }
        if (motherNumber.length() != 0 && motherNumber.length() != 15) {
            eTmotherNumber.setError("모개체 식별변호가 유효하지 않습니다.");
            return;
        }

        String birthday;
        if (tVbirthday.getText().toString().equals("지정 안함"))
            birthday = null;
        else
            birthday = String.format(Locale.KOREA, "%d-%d-%d", year, month, day);

        Cow cow;
        if (this.cow == null) {
            cow = new Cow(sex, number, motherNumber, birthday);
        } else {
            // TODO: make updated cow instance
            cow = this.cow;
        }

        saveCowTask = new SaveCowTask(this);
        saveCowTask.execute(cow);
    }

    private void setErrors(Map<String, String> errors) {
        List<String> others = new ArrayList<>();
        for (Map.Entry<String, String> entry: errors.entrySet()) {
            String field = entry.getKey();
            String error = entry.getValue();
            switch (field) {
                case "number":
                    eTnumber.setError(error);
                    break;
                case "mother_number":
                    eTmotherNumber.setError(error);
                    break;
                case "detail":
                    others.add(error);
                    break;
                default:
                    others.add(field + ": "+ error);
            }
        }

        for (EditText editText : new EditText[]{eTnumber, eTmotherNumber}) {
            if (editText.getError() != null) {
                editText.requestFocus();
                break;
            }
        }

        if (others.size() > 0) {
            Toast.makeText(this, TextUtils.join("\n", others), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     *  helper function for auto focusing on end of the prev EditText.
     */
    private void connect(final EditText eT1, final EditText eT2) {
        int maxLength = -1;
        for(InputFilter filter : eT1.getFilters()) {
            if(filter instanceof InputFilter.LengthFilter) {
                try {
                    Field maxLengthField = filter.getClass().getDeclaredField("mMax");
                    maxLengthField.setAccessible(true);
                    if(maxLengthField.isAccessible()) {
                        maxLength = maxLengthField.getInt(filter);
                    }
                } catch (Exception e) {}
            }
        }
        final int finalMaxLength = maxLength;
        eT1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                if(eT1.getText().toString().length()== finalMaxLength) {
                    eT2.requestFocus();
                }
            }
        });
    }

    /**
     * Custom TextWatcher for controlling text of the cow number EditText
     */
    private class ControlledTextWatcher implements TextWatcher {
        private EditText view;
        private int selection = 0;
        private boolean controlled = false;

        ControlledTextWatcher(EditText view) {
            this.view = view;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (!controlled)
                selection = start - count + after;
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (controlled) return;
            String raw = s.toString();
            StringBuilder result = new StringBuilder();
            int selection = -1;
            for (int i = 0; i < raw.length(); ++i) {
                char c = raw.charAt(i);
                if (c != '-') {
                    result.append(c);
                }
                int len = result.length();
                if (len == 3 || len == 8 || len == 13) {
                    result.append('-');
                }
                if (this.selection == i) {
                    selection = len;
                }
            }
            if (result.length() > 15) {
                result = new StringBuilder(result.substring(0, 15));
            }
            if (selection == -1)
                selection = result.length();
            if (!result.toString().equals(raw)) {
                controlled = true;
                view.setText(result.toString());
                view.setSelection(selection);
                controlled = false;
            }
        }
    }

    private static class SaveCowTask extends NetworkService.Task<EditCowActivity, Cow, Cow> {
        public SaveCowTask(EditCowActivity holder) {
            super(holder);
        }

        @Override
        protected NetworkService.Result<Cow> request(Cow cow) {
            if (cow.id == null) {
                return NetworkService.createCow(cow);
            } else {
                // TODO: request update cow instance
                return null;
            }
        }

        @Override
        protected void responseInit(boolean isSuccessful) {
            super.responseInit(isSuccessful);
            getHolder().saveCowTask = null;
        }

        @Override
        protected void responseSuccess(Cow cow) {
            getHolder().finish();
        }

        @Override
        protected void responseFail(Map<String, String> errors) {
            if (existErrors(errors, getHolder())) {
                getHolder().setErrors(errors);
            }
        }
    }
}
