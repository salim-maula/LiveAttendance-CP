package com.example.aplikasiabsensi.networking

object ApiServices {
    fun getLiveAttendance(): LiveAttendanceApiService{
        return RetrofitClient
            .getClient()
            .create(LiveAttendanceApiService::class.java)
    }
}