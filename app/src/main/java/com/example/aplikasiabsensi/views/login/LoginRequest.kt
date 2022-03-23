package com.example.aplikasiabsensi.views.login

import com.google.gson.annotations.SerializedName

/*Contoh request menggunakan raw-> JSON, dan menambahkan Content-Type & Accept di headers berupa application/json
{
    "email": "test@test.com",
    "password": "12345678",
    "device_name": "mobile"
}
 */

data class LoginRequest(

	@field:SerializedName("password")
	val password: String? = null,

	@field:SerializedName("device_name")
	val deviceName: String? = null,

	@field:SerializedName("email")
	val email: String? = null
)
