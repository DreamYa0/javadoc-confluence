package com.g7.framework.javadoc.util;

import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class HTTPUtils {

    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder()
            .connectionPool(new ConnectionPool())
            // 设置连接超时
            .connectTimeout(15, TimeUnit.SECONDS)
            // 设置读超时
            .readTimeout(20, TimeUnit.SECONDS)
            // 设置写超时
            .writeTimeout(20, TimeUnit.SECONDS)
            // 是否自动重连
            .retryOnConnectionFailure(true)
            .build();

    public static String post(String urlStr, String content, String username, String password) throws IOException {
        return request(urlStr, content, username, password, "POST");
    }

    public static String put(String urlStr, String content, String username, String password) throws IOException {
        return request(urlStr, content, username, password, "PUT");
    }

    private static String request(String url, String content, String username, String password, String mode) throws IOException {
        String author = "Basic " + MyBase64.encode((username + ":" + password).getBytes());
        Request.Builder builder = new Request.Builder();
        Request request;
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=UTF-8"), content);
        if ("POST".equalsIgnoreCase(mode)) {
            request = builder.url(url).post(requestBody).addHeader("Authorization", author).build();
        } else {
            request = builder.url(url).put(requestBody).addHeader("Authorization", author).build();
        }

        return execute(request);
    }

    public static String get(String url, String username, String password) throws IOException {
        String author = "Basic " + MyBase64.encode((username + ":" + password).getBytes());
        Request.Builder builder = new Request.Builder();
        Request request = builder.url(url).get().addHeader("Authorization", author)
                .addHeader("Content-Type", "application/json; charset=utf-8").build();
        return execute(request);
    }

    private static String execute(Request request) throws IOException {
        try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                if (Objects.nonNull(body)) {
                    return body.string();
                }
            }
        }
        return "";
    }
}
