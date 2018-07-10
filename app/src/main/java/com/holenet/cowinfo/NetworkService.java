package com.holenet.cowinfo;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.GsonBuilder;
import com.holenet.cowinfo.item.Cow;
import com.holenet.cowinfo.item.Record;
import com.holenet.cowinfo.item.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class NetworkService {
    private static OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
    private static Retrofit.Builder builder = new Retrofit.Builder()
            .baseUrl(API.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(
                    new GsonBuilder().serializeNulls().create())
            );
    private static API api;

    private static class AuthenticationInterceptor implements Interceptor {
        private String authToken;

        AuthenticationInterceptor(String authToken) {
            this.authToken = authToken;
        }

        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            Request.Builder builder = original.newBuilder().header("Authorization", authToken);
            Request request = builder.build();
            return chain.proceed(request);
        }
    }

    private static void init() {
        if (api == null) {
            api = builder.build().create(API.class);
        }
    }

    private static void setAuthToken(String authToken) {
        AuthenticationInterceptor interceptor = new AuthenticationInterceptor(authToken);
        if (!httpClient.interceptors().contains(interceptor)) {
            httpClient.addInterceptor(interceptor);
            builder.client(httpClient.build());
            api = builder.build().create(API.class);
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
        Call<User> call = api.signIn(user);
        Result<User> result = request(call, "signIn");
        if (result != null && result.isSuccessful()) {
            setAuthToken("Token " + result.result.auth_token);
        }
        return result;
    }

    public static Result<User> signUp(User user) {
        init();
        Call<User> call = api.signUp(user);
        Result<User> result = request(call, "signUp");
        if (result != null && result.isSuccessful()) {
            setAuthToken("Token " + result.result.auth_token);
        }
        return result;
    }

    public static Result<List<Cow>> getCowList(boolean deleted) {
        init();
        Call<List<Cow>> call = api.getCowList(stringify(deleted));
        return request(call, "getCowList");
    }

    public static Result<Cow> getCow(int cowId) {
        init();
        Call<Cow> call = api.getCow(cowId);
        return request(call, "getCow");
    }

    public static Result<Cow> createCow(Cow cow) {
        init();
        Call<Cow> call = api.createCow(cow);
        return request(call, "createCow");
    }

    public static Result<Cow> updateCow(Cow cow) {
        init();
        Call<Cow> call = api.updateCow(cow.id, cow);
        return request(call, "updateCow");
    }

    public static Result<Void> destroyCow(int cowId) {
        init();
        Call<Void> call = api.destroyCow(cowId);
        return request(call, "destroyCow");
    }

    public static Result<List<Record>> getRecordList(boolean cowDeleted) {
        init();
        Call<List<Record>> call = api.getRecordList(stringify(cowDeleted));
        return request(call, "getRecordList");
    }

    public static Result<List<Record>> getRecordList(boolean cowDeleted, String day) {
        init();
        Call<List<Record>> call = api.getRecordList(stringify(cowDeleted), day);
        return request(call, "getRecordList");
    }

    public static Result<Record> createRecord(Record record) {
        init();
        Call<Record> call = api.createRecord(record);
        return request(call, "createRecord");
    }

    public static Result<Record> updateRecord(Record record) {
        init();
        Call<Record> call = api.updateRecord(record.id, record);
        return request(call, "updateRecord");
    }

    public static Result<Void> destroyRecord(int recordId) {
        init();
        Call<Void> call = api.destroyRecord(recordId);
        return request(call, "destroyRecord");
    }

    public interface API {
//        String BASE_URL = "http://13.125.31.214";
        String BASE_URL = "http://10.0.2.2:8000";   // development environment
        String USERS_URL = BASE_URL + "/users/";
        String COWS_URL = BASE_URL + "/cows/";
        String RECORDS_URL = BASE_URL + "/records/";

        @POST(USERS_URL + "auth-token/")
        Call<User> signIn(@Body User user);

        @POST(USERS_URL + "new/")
        Call<User> signUp(@Body User user);

        @GET(COWS_URL)
        Call<List<Cow>> getCowList(@Query("deleted") String deleted);

        @GET(COWS_URL+"{id}/")
        Call<Cow> getCow(@Path("id") int id);

        @POST(COWS_URL)
        Call<Cow> createCow(@Body Cow cow);

        @PATCH(COWS_URL+"{id}/")
        Call<Cow> updateCow(@Path("id") int id, @Body Cow cow);

        @DELETE(COWS_URL+"{id}/")
        Call<Void> destroyCow(@Path("id") int id);

        @GET(RECORDS_URL)
        Call<List<Record>> getRecordList(@Query("cow__deleted") String cowDeleted);

        @GET(RECORDS_URL)
        Call<List<Record>> getRecordList(@Query("cow__deleted") String cowDeleted, @Query("day") String day);

        @POST(RECORDS_URL)
        Call<Record> createRecord(@Body Record record);

        @PATCH(RECORDS_URL+"{id}/")
        Call<Record> updateRecord(@Path("id") int id, @Body Record record);

        @DELETE(RECORDS_URL+"{id}/")
        Call<Void> destroyRecord(@Path("id") int id);
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
                    while (iterator.hasNext()) {
                        String key = iterator.next();
                        ArrayList<String> strings = new ArrayList<>();
                        JSONArray errorList;
                        try {
                            errorList = json.getJSONArray(key);
                        } catch (JSONException e) {
                            errorList = new JSONArray("[\""+json.getString(key)+"\"]");
                        }
                        for(int i=0; i<errorList.length(); i++) {
                            strings.add((String)errorList.get(i));
                        }
                        errors.put(key, TextUtils.join("\n", strings));
                    }
                } catch (Exception e) {
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
            Log.i(tag+" Response", "body:"+(response.body() != null ? response.body().toString() : null));
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

    public static int[] destructDate(String date) {
        String[] days = date.split("-");
        int year = Integer.parseInt(days[0]);
        int month = Integer.parseInt(days[1]);
        int day = Integer.parseInt(days[2]);
        return new int[] {year, month, day};
    }

    public static String stringify(boolean bool) {
        return bool ? "True" : "False";
    }
}
