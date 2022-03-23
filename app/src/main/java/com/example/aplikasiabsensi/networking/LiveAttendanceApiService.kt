package com.example.aplikasiabsensi.networking

import com.example.aplikasiabsensi.model.ForgotPasswordResponse
import com.example.aplikasiabsensi.model.LoginResponse
import com.example.aplikasiabsensi.views.forgotpass.ForgotPasswordRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface LiveAttendanceApiService {
    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("auth/login")
    fun loginRequest(@Body body: String): Call<LoginResponse>

    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("auth/password/forgot")
    fun forgotPasswordRequest(@Body body: String): Call<ForgotPasswordResponse>
}