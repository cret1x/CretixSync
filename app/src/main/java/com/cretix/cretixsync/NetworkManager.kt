package com.cretix.cretixsync

import android.database.Cursor
import io.reactivex.Completable
import io.reactivex.Observable
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit


data class RegisterResponse(val status: Int, val error: Int)
data class InfoResponse(val status: Int, val name: String)
data class RegisterData(val login: String, val password: String)
data class SyncInfo(val albums: List<String>, val login: String, val password: String)
data class SyncResponse(val status: Int, val url: String)
data class UploadResponse(val status: Int)

interface UploadService {
    @POST("/register-device")
    fun registerDevice(@Body regData: RegisterData): Observable<RegisterResponse>

    @POST("/login-device")
    fun loginDevice(@Body regData: RegisterData): Observable<RegisterResponse>

    @POST("/start-sync")
    fun startSync(@Body syncData: SyncInfo): Observable<SyncResponse>

    @POST("/pulse")
    fun pulse(@Body regData: RegisterData): Observable<UploadResponse>

    @POST("/info")
    fun getInfo(@Body regData: RegisterData): Observable<InfoResponse>

    @POST
    @Multipart
    fun upload(@Url url: String,  @Part file: MultipartBody.Part) : Call<UploadResponse>

}

object NetworkManager {
    var retrofit: Retrofit? = null

    fun getClient(baseUrl: String) : Retrofit? {
        if (retrofit == null) {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BASIC
            val client = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .client(client)
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit
    }
}