package com.example.aplikasiabsensi.model

import com.google.gson.annotations.SerializedName

/*Response
{
    "message": "success",
    "data": {
        "id": 1,
        "name": "Admin",
        "email": "test@test.com",
        "email_verified_at": null,
        "is_admin": 1,
        "photo": null,
        "created_at": "2022-03-13T11:14:42.000000Z",
        "updated_at": "2022-03-13T11:14:42.000000Z"
    },
    "meta": {
        "token": "1|olCfBUptxY3wPTSWsdgwaJx0ha7lWY8J0nJ9f9if"
    }
}
 */

data class LoginResponse(

	@field:SerializedName("data")
	val user: User? = null,

	@field:SerializedName("meta")
	val meta: Meta? = null,

	@field:SerializedName("message")
	val message: String? = null
)

data class User(

	@field:SerializedName("is_admin")
	val isAdmin: Int? = null,

	@field:SerializedName("updated_at")
	val updatedAt: String? = null,

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("photo")
	val photo: String? = null,

	@field:SerializedName("created_at")
	val createdAt: String? = null,

	@field:SerializedName("email_verified_at")
	val emailVerifiedAt: Any? = null,

	@field:SerializedName("id")
	val id: Int? = null,

	@field:SerializedName("email")
	val email: String? = null
)

data class Meta(

	@field:SerializedName("token")
	val token: String? = null
)
