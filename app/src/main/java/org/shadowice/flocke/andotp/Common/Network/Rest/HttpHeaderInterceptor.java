package org.shadowice.flocke.andotp.Common.Network.Rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class HttpHeaderInterceptor implements Interceptor {

    Map<String, String> headers;

    public HttpHeaderInterceptor() {
        super();
        headers = new HashMap<>();
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request.Builder builder = chain.request().newBuilder();
        for (String key : headers.keySet()) {
            builder.addHeader(key, headers.get(key));
        }
        Response response;
        try {
            response = chain.proceed(builder.build());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return response;
    }
}
