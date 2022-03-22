package com.example.aplikasiabsensi.views.changepass

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.aplikasiabsensi.R
import com.example.aplikasiabsensi.databinding.ActivityChangePasswordBinding

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()
        onClick()
    }

    private fun onClick() {
        binding.tbChangePassword.setNavigationOnClickListener {
            finish()
        }
    }

    private fun init(){
        setSupportActionBar(binding.tbChangePassword)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}