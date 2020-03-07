package org.shadowice.flocke.andotp.Common.Network.Rest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.shadowice.flocke.andotp.Common.Utilities.NetworkConnectivity;
import org.shadowice.flocke.andotp.Services.API.Models.DipDeviceModel;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public class RestClient {

    private static final Interceptor REWRITE_CACHE_CONTROL_INTERCEPTOR =
            new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Response originalResponse = chain.proceed(chain.request());
                    if (NetworkConnectivity.isConnected()) {
                        int maxAge = 60; // read from cache for 1 minute
                        return originalResponse
                                .newBuilder()
                                .header("Cache-Control", "public, max-age=" + maxAge)
                                .build();
                    } else {
                        int maxStale = 60 * 60 * 24 * 28; // tolerate 4-weeks stale
                        return originalResponse
                                .newBuilder()
                                .header("Cache-Control", "public, only-if-cached, max-stale=" + maxStale)
                                .build();
                    }
                }
            };

    private static String BASE_URL = "https://webservice.dip.cloud/";

    public static Retrofit getEcombidService() {

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        HttpHeaderInterceptor headers = new HttpHeaderInterceptor();
        headers.addHeader(
                HttpCommon.HTTPRequestHeaderNameAccept, HttpCommon.HTTPURLRequestContentTypeJSON);

        OkHttpClient client =
                new OkHttpClient.Builder()
                        .connectTimeout(7000, TimeUnit.SECONDS)
                        .readTimeout(600, TimeUnit.SECONDS)
                        .addInterceptor(headers)
                        .addInterceptor(interceptor)
                        .addNetworkInterceptor(REWRITE_CACHE_CONTROL_INTERCEPTOR)
                        .build();

        Retrofit retrofit;
        retrofit =
                new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .client(client)
                        .build();

        return retrofit;
    }

    public interface ServiceInterface {

        @Headers({
                "accept: application/json",
                "Content-Type: application/json-patch+json"
        })
        @POST("device.php")
        Call<DipDeviceModel> sendDipDevice(@Body JsonObject object);

        @Headers({
                "accept: application/json",
                "Content-Type: application/json-patch+json"
        })
        @POST("digits.php")
        Call<ResponseBody> sendDipDigit(@Body JsonArray jsonArray);

        @Headers({
                "accept: application/json",
                "Content-Type: application/json-patch+json"
        })
        @POST("delete.php")
        Call<ResponseBody> deleteDipDevice(@Body JsonObject object);

        @Headers({
                "accept: application/json",
                "Content-Type: application/json-patch+json"
        })
        @POST("generate_seedkey.php")
        Call<ResponseBody> generateSeedkey();

        @Headers({
                "accept: application/json",
                "Content-Type: application/json-patch+json"
        })
        @POST("check_plan.php")
        Call<ResponseBody> checkPlan(@Body JsonObject jsonObject);

        @Headers({
                "accept: application/json",
                "Content-Type: application/json-patch+json"
        })
        @POST("add_deviceid.php")
        Call<ResponseBody> addDeviceId(@Body JsonObject jsonObject);

        @Headers({
                "accept: application/json",
                "Content-Type: application/json-patch+json"
        })
        @POST("plan_submit.php")
        Call<ResponseBody> submitPlan(@Body JsonObject jsonObject);

        @Headers({
                "accept: application/json",
                "Content-Type: application/json-patch+json"
        })
        @POST("notifications.php")
        Call<ResponseBody> getNotification(@Body JsonObject jsonObject);

    }
}
