package com.example.aplikasiabsensi.views.splash

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.example.aplikasiabsensi.R
import com.example.aplikasiabsensi.hawkstorage.HawkStorage
import com.example.aplikasiabsensi.views.login.LoginActivity
import com.example.aplikasiabsensi.views.main.MainActivity
import org.jetbrains.anko.startActivity

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        afterDelayGoToLogin()
    }

    private fun afterDelayGoToLogin() {
        Handler(Looper.getMainLooper()).postDelayed({
            checkIsLogin()
        },1200)
    }

    private fun checkIsLogin() {
        val isLogin = HawkStorage.instance(this).isLogin()
        if (isLogin){
            startActivity<MainActivity>()
            //membuat root activity akan di destroy
            finishAffinity()
        }else{
            startActivity<LoginActivity>()
            //membuat root activity akan di destroy
            finishAffinity()
        }
    }
}