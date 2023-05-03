package com.example.chat_app.PushNotifications;

import static com.example.chat_app.PushNotifications.ValuesClass.CONTENT_TYPE;
import static com.example.chat_app.PushNotifications.ValuesClass.SERVER_KEY;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ApiInterface {
    @Headers({"Authorization:" + SERVER_KEY, "Content-Type:" + CONTENT_TYPE})
    @POST("fcm/send")
    Call<PushNotification> sendNotification(@Body PushNotification pushNotification);

}
