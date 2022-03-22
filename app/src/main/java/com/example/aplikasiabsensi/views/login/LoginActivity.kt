package com.example.aplikasiabsensi.views.login

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.aplikasiabsensi.R
import com.example.aplikasiabsensi.databinding.ActivityLoginBinding
import com.example.aplikasiabsensi.databinding.ActivityMainBinding
import com.example.aplikasiabsensi.views.forgotpass.ForgotPasswordActivity
import com.example.aplikasiabsensi.views.main.MainActivity
import org.jetbrains.anko.startActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onClick()
    }

    private fun onClick() {
        binding.btnLogin.setOnClickListener {
            startActivity<MainActivity>()
        }

        binding.btnForgotPassword.setOnClickListener {
            startActivity<ForgotPasswordActivity>()
        }
    }
}