package com.holenet.cowinfo;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.holenet.cowinfo.item.Cow;
import com.holenet.cowinfo.item.Record;
import com.holenet.cowinfo.item.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class NetworkService {
    private static API api;
    private static String authenticationToken;

    private static String authenticator(User user) {
        return "basic "+ Base64.encodeToString((user.username + ":" +user.password).getBytes(), Base64.NO_WRAP);
    }

    private static void init() {
        if (api == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(API.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            api = retrofit.create(API.class);
        }
    }

    private static <T> Result<T> request(Call<T> call, String tag) {
        try {
            Response<T> response  = call.execute();
            logResponse(tag, response);
            return new Result<>(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Result<User> signIn(User user) {
        init();
        authenticationToken = authenticator(user);
        Call<User> call = api.signIn(authenticationToken);
        return request(call, "signIn");
    }

    public static Result<User> signUp(User user) {
        init();
        authenticationToken = authenticator(user);
        Call<User> call = api.signUp(user);
        return request(call, "signUp");
    }

    public static Result<List<Cow>> getCowList(boolean deleted) {
        init();
        Call<List<Cow>> call = api.getCowList(authenticationToken, deleted);
        return request(call, "getCowList");
    }

    public static Result<Cow> getCow(int cowId) {
        init();
        Call<Cow> call = api.getCow(authenticationToken, cowId);
        return request(call, "getCow");
    }

    public static Result<Cow> createCow(Cow cow) {
        init();
        Call<Cow> call = api.createCow(authenticationToken, cow);
        return request(call, "createCow");
    }

    public static Result<Record> createRecord(Record record) {
        init();
        Call<Record> call = api.createRecord(authenticationToken, record);
        return request(call, "createRecord");
    }

    public interface API {
        String BASE_URL = "http://13.125.31.214";
        String USERS_URL = BASE_URL + "/users/";
        String COWS_URL = BASE_URL + "/cows/";
        String RECORDS_URL = BASE_URL + "/records/";

        @GET(USERS_URL + "my/")
        Call<User> signIn(@Header("Authorization") String authorization);

        @POST(USERS_URL + "new/")
        Call<User> signUp(@Body User user);

        @GET(COWS_URL)
        Call<List<Cow>> getCowList(@Header("Authorization") String authorization, @Query("deleted") boolean deleted);

        @GET(COWS_URL+"{id}/")
        Call<Cow> getCow(@Header("Authorization") String authorization, @Path("id") int id);

        @POST(COWS_URL)
        Call<Cow> createCow(@Header("Authorization") String authorization, @Body Cow cow);

        @POST(RECORDS_URL)
        Call<Record> createRecord(@Header("Authorization") String authorization, @Body Record record);
    }

    public static class Result<T> {
        private boolean isSuccessful;
        private T result;
        private Map<String, String> errors;

        private Result(Response<T> response) {
            isSuccessful = response.isSuccessful();
            if (isSuccessful) {
                result = response.body();
            } else {
                try {
                    errors = new HashMap<>();
                    String raw = response.errorBody().string();
                    JSONObject json = new JSONObject(raw);
                    Iterator<String> iterator = json.keys();
                    while(iterator.hasNext()) {
                        String key = iterator.next();
                        ArrayList<String> strings = new ArrayList<>();
                        JSONArray errorList;
                        try {
                            errorList = json.getJSONArray(key);
                        } catch(JSONException e) {
                            errorList = new JSONArray("[\""+json.getString(key)+"\"]");
                        }
                        for(int i=0; i<errorList.length(); i++) {
                            strings.add((String)errorList.get(i));
                        }
                        errors.put(key, TextUtils.join("\n", strings));
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public boolean isSuccessful() {
            return isSuccessful;
        }

        public T getResult() {
            return result;
        }

        public Map<String, String> getErrors() {
            return errors;
        }
    }

    public static abstract class Task<A, P, R> extends AsyncTask<P, Void, Result<R>> {
        private WeakReference<A> holderWeakReference;

        public Task(A holder) {
            this.holderWeakReference = new WeakReference<>(holder);
        }

        protected A getHolder() {
            return holderWeakReference.get();
        }

        @Override
        protected Result<R> doInBackground(P... ps) {
            if (ps.length > 0)
                return request(ps[0]);
            return request(null);
        }

        @Override
        protected void onPostExecute(Result<R> result) {
            if (result == null) {
                responseFail(null);
                return;
            }
            Log.d("NetworkService Response", result.isSuccessful() ? "Succeeded" : "Failed");
            responseInit(result.isSuccessful());
            if (result.isSuccessful()) {
                responseSuccess(result.getResult());
            } else {
                responseFail(result.getErrors());
            }
        }

        protected abstract Result<R> request(P p);
        protected abstract void responseInit(boolean isSuccessful);
        protected abstract void responseSuccess(R r);
        protected abstract void responseFail(Map<String, String> errors);
        protected boolean existErrors(Map<String, String> errors, Context context) {
            if (errors == null) {
                Toast.makeText(context, "인터넷 연결을 확인해주세요.", Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        }
    }

    private static void logResponse(String tag, Response response) {
        Log.i(tag+" Response", response.isSuccessful() ? "Success" : "Fail");
        Log.i(tag+" Response", response.code()+" "+response.message());
        if (response.isSuccessful()) {
            Log.i(tag+" Response", "body:"+response.body().toString());
        } else {
            try {
                Log.i(tag+" Response", "error: "+response.errorBody().toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static String constructDate(int year, int month, int day) {
        return String.format(Locale.KOREA, "%d-%d-%d", year, month, day);
    }
}
