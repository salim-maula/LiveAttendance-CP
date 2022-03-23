package com.example.aplikasiabsensi.hawkstorage

import android.content.Context
import com.example.aplikasiabsensi.model.User
import com.orhanobut.hawk.Hawk

class HawkStorage {
    companion object {
        private const val USER_KEY = "user_key"
        private const val TOKEN_KEY = "token_key"
        //2 karna kita menggunakan kelas baru maka kita harus menginisialisasi kelas tersebut
        private val hawkStorage = HawkStorage()

        fun instance(context: Context?) : HawkStorage{
            //1untuk mengkonfigurasi hawk storage kita memerlukan Hawk.init
            Hawk.init(context).build()
            return hawkStorage
        }
    }

    fun setUser(user: User){
        //untuk memasukan data kita cukup memerlukan Hawk.put
        Hawk.put(USER_KEY, user)
    }

    fun getUser(): User{
        return Hawk.get(USER_KEY)
    }

    fun setToken(token: String){
        Hawk.put(TOKEN_KEY, token)
    }

    fun getToken(): String{
        val rawToken = Hawk.get<String>(TOKEN_KEY)
        // tanda | akan memisahkan satu string menjadi dua array
        val token = rawToken.split("|")
        //karna kita memerlukan tokennya saja maka kita akan mengambil index ke 1
        return token[1]
    }

    fun isLogin(): Boolean{
        if (Hawk.contains(USER_KEY)){
            return true
        }
        return false
    }

    fun deleteAll(){
        Hawk.deleteAll()
    }
}