package com.holenet.cowinfo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.holenet.cowinfo.item.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SignInActivity extends AppCompatActivity {
    final static int RESULT_SUCCESS = 101;
    final static int RESULT_EXIT = 103;

    final static int MODE_SIGN_IN = 201;
    final static int MODE_SIGN_UP = 202;
    private int mode = MODE_SIGN_IN;

    private SignInTask signInTask;
    private SignUpTask signUpTask;

    private EditText eTusername, eTpassword, eTpassword2;
    private CheckBox cBautoSignIn, cBsaveUsername, cBsavePassword;
    private Button bTsignIn, bTsignUp;
    private LinearLayout lLForSignUp;

    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        setResult(RESULT_EXIT);

        pref = getSharedPreferences("sign_in", 0);

        eTusername = findViewById(R.id.eTusername);
        eTpassword = findViewById(R.id.eTpassword);
        eTpassword2 = findViewById(R.id.eTpassword2);

        cBautoSignIn = findViewById(R.id.cBautoSignIn);
        cBsaveUsername = findViewById(R.id.cBsaveUsername);
        cBsavePassword = findViewById(R.id.cBSavePassword);

        bTsignIn = findViewById(R.id.bTsignIn);
        bTsignUp = findViewById(R.id.bTsignUp);

        lLForSignUp = findViewById(R.id.lLForSignUp);

        if (pref.getBoolean(getString(R.string.pref_key_save_username), false))
            eTusername.setText(pref.getString(getString(R.string.pref_key_username), ""));
        if (pref.getBoolean(getString(R.string.pref_key_save_password), false))
            eTpassword.setText(pref.getString(getString(R.string.pref_key_password), ""));

        cBautoSignIn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    cBsaveUsername.setChecked(true);
                    cBsavePassword.setChecked(true);
                }
                cBsaveUsername.setEnabled(!isChecked);
                cBsavePassword.setEnabled(!isChecked);
            }
        });
        cBsaveUsername.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    cBsavePassword.setChecked(false);
                }
                cBsavePassword.setEnabled(isChecked);
            }
        });
        cBsavePassword.setChecked(pref.getBoolean(getString(R.string.pref_key_save_password), false));
        cBsaveUsername.setChecked(pref.getBoolean(getString(R.string.pref_key_save_username), false));
        cBautoSignIn.setChecked(pref.getBoolean(getString(R.string.pref_key_auto_sign_in), false));

        bTsignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptSignIn();
            }
        });
        bTsignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mode == MODE_SIGN_UP)
                    attemptSignUp();
                else
                    changeMode(true);
            }
        });

        lLForSignUp.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                maxHeight = lLForSignUp.getHeight();
                lLForSignUp.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) lLForSignUp.getLayoutParams();
                layoutParams.height = 0;
                lLForSignUp.setLayoutParams(layoutParams);
            }
        });
    }

    private void attemptSignIn() {
        if (signInTask != null) {
            return;
        }

        eTusername.setError(null);
        eTpassword.setError(null);

        String username = eTusername.getText().toString();
        String password = eTpassword.getText().toString();

        User user = new User(username, password);
        signInTask = new SignInTask(this);
        signInTask.execute(user);
    }

    private void attemptSignUp() {
        if (signUpTask != null) {
            return;
        }

        eTusername.setError(null);
        eTpassword.setError(null);
        eTpassword2.setError(null);

        String username = eTusername.getText().toString();
        String password = eTpassword.getText().toString();
        String password2 = eTpassword2.getText().toString();

        if (!password.equals(password2)) {
            eTpassword2.setError("비밀번호가 일치하지 않습니다.");
            eTpassword2.requestFocus();
            return;
        }

        User user = new User(username, password);
        signUpTask = new SignUpTask(this);
        signUpTask.execute(user);
    }

    private void onFinishSignInOrUp(User user) {
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(getString(R.string.pref_key_auto_sign_in), cBautoSignIn.isChecked());
        editor.putBoolean(getString(R.string.pref_key_save_username), cBsaveUsername.isChecked());
        editor.putBoolean(getString(R.string.pref_key_save_password), cBsavePassword.isChecked());
        editor.putString(getString(R.string.pref_key_username), eTusername.getText().toString());
        editor.putString(getString(R.string.pref_key_password), eTpassword.getText().toString());
        editor.apply();

        Intent intent = new Intent();
        intent.putExtra("user", user);
        setResult(RESULT_SUCCESS, intent);
        finish();
    }

    ValueAnimator anim;
    int maxHeight;

    private void changeMode(final boolean beSignUp) {
        final int animTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
        mode = beSignUp ? MODE_SIGN_UP : MODE_SIGN_IN;
        getSupportActionBar().setTitle(beSignUp ? "회원가입" : "로그인");

        eTusername.setError(null);
        eTpassword.setError(null);
        eTpassword2.setError(null);

        if (anim != null && anim.isRunning())
            anim.cancel();
        final float weight = ((LinearLayout.LayoutParams) bTsignIn.getLayoutParams()).weight;
        ValueAnimator.setFrameDelay(24);
        anim = ValueAnimator.ofFloat(1- weight, beSignUp ? 1 : 0);
        anim.setDuration((long) (animTime * (beSignUp ? weight : 1 - weight)));
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (!beSignUp) {
                    eTpassword2.setText("");
                }
            }
        });
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float val = (float) animation.getAnimatedValue();
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) lLForSignUp.getLayoutParams();
                layoutParams.height = (int) (val * maxHeight);
                lLForSignUp.setLayoutParams(layoutParams);

                layoutParams = (LinearLayout.LayoutParams) bTsignUp.getLayoutParams();
                layoutParams.weight = 1 + val;
                bTsignUp.setLayoutParams(layoutParams);

                layoutParams = (LinearLayout.LayoutParams) bTsignIn.getLayoutParams();
                layoutParams.weight = 1 - val;
                bTsignIn.setLayoutParams(layoutParams);
            }
        });
        anim.start();

        if (beSignUp) {
            for (EditText editText : new EditText[]{eTusername, eTpassword, eTpassword2}) {
                if (editText.getText().length() == 0) {
                    editText.requestFocus();
                    break;
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mode == MODE_SIGN_UP) {
            changeMode(false);
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setErrors(Map<String, String> errors) {
        List<String> others = new ArrayList<>();
        for (Map.Entry<String, String> entry: errors.entrySet()) {
            String field = entry.getKey();
            String error = entry.getValue();
            switch (field) {
                case "username":
                    eTusername.setError(error);
                    break;
                case "password":
                    eTpassword.setError(error);
                    break;
                case "detail":
                    others.add(error);
                    break;
                default:
                    others.add(field + ": "+ error);
            }
        }

        for (EditText editText : new EditText[]{eTusername, eTpassword}) {
            if (editText.getError() != null) {
                editText.requestFocus();
                break;
            }
        }

        if (others.size() > 0) {
            Toast.makeText(this, TextUtils.join("\n", others), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sign_in, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.mIexit) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class SignInTask extends NetworkService.Task<SignInActivity, User, User> {
        public SignInTask(SignInActivity holder) {
            super(holder);
        }

        @Override
        protected NetworkService.Result<User> request(User user) {
            return NetworkService.signIn(user);
        }

        @Override
        protected void responseInit(boolean isSuccessful) {
            super.responseInit(isSuccessful);
            getHolder().signInTask = null;
        }

        @Override
        protected void responseSuccess(User user) {
            getHolder().onFinishSignInOrUp(user);
        }

        @Override
        protected void responseFail(Map<String, String> errors) {
            if (existErrors(errors, getHolder())) {
                getHolder().setErrors(errors);
            }
        }
    }

    private static class SignUpTask extends NetworkService.Task<SignInActivity, User, User> {
        public SignUpTask(SignInActivity holder) {
            super(holder);
        }

        @Override
        protected NetworkService.Result<User> request(User user) {
            return NetworkService.signUp(user);
        }

        @Override
        protected void responseInit(boolean isSuccessful) {
            super.responseInit(isSuccessful);
            getHolder().signUpTask = null;
        }

        @Override
        protected void responseSuccess(User user) {
            getHolder().onFinishSignInOrUp(user);
        }

        @Override
        protected void responseFail(Map<String, String> errors) {
            if (existErrors(errors, getHolder())) {
                getHolder().setErrors(errors);
            }
        }
    }
}
