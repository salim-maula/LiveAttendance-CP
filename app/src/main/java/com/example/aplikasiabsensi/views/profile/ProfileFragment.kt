package com.example.aplikasiabsensi.views.profile

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.Settings.ACTION_LOCALE_SETTINGS
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.aplikasiabsensi.BuildConfig
import com.example.aplikasiabsensi.R
import com.example.aplikasiabsensi.databinding.FragmentProfileBinding
import com.example.aplikasiabsensi.dialog.MyDialog
import com.example.aplikasiabsensi.hawkstorage.HawkStorage
import com.example.aplikasiabsensi.model.LogoutResponse
import com.example.aplikasiabsensi.networking.ApiServices
import com.example.aplikasiabsensi.views.changepass.ChangePasswordActivity
import com.example.aplikasiabsensi.views.login.LoginActivity
import com.example.aplikasiabsensi.views.main.MainActivity
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileFragment : Fragment() {

    private var binding: FragmentProfileBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
       binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onClick()
        updateView()
    }

    private fun updateView() {
        val user = HawkStorage.instance(context).getUser()
        val imageUrl = BuildConfig.BASE_IMAGE_URL + user.photo
        Glide.with(requireContext()).load(imageUrl).placeholder(android.R.color.darker_gray).into(binding!!.ivProfile)
        binding?.tvNameProfile?.text = user.name
        binding?.tvEmailProfile?.text = user.email
    }

    private fun onClick() {
        binding?.btnChangePassword?.setOnClickListener {
            context?.startActivity<ChangePasswordActivity>()
        }

        binding?.btnChangeLanguage?.setOnClickListener {
           startActivity(Intent(ACTION_LOCALE_SETTINGS))
        }

        binding?.btnLogout?.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle(getString(R.string.logout))
                .setMessage(getString(R.string.are_you_sure))
                .setPositiveButton(getString(R.string.yes)){dialog, _ ->
                    logoutRequest(dialog)
                }
                .setNegativeButton(getString(R.string.no)){dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun logoutRequest(dialog: DialogInterface?) {
        val token = HawkStorage.instance(context).getToken()
        MyDialog.showProgressDialog(context)
        ApiServices.getLiveAttendance()
            .logoutRequest("Bearer $token")
            .enqueue(object : Callback<LogoutResponse> {
                override fun onResponse(
                    call: Call<LogoutResponse>,
                    response: Response<LogoutResponse>
                ) {
                    dialog?.dismiss()
                    MyDialog.hideDialog()
                    if (response.isSuccessful){
                        HawkStorage.instance(context).deleteAll()
                        (activity as MainActivity).finishAffinity()
                        context?.startActivity<LoginActivity>()
                    }else{
                        MyDialog.dynamicDialog(context, getString(R.string.alert), getString(R.string.something_wrong))
                    }
                }

                override fun onFailure(call: Call<LogoutResponse>, t: Throwable) {
                    dialog?.dismiss()
                    MyDialog.hideDialog()
                    MyDialog.dynamicDialog(context, getString(R.string.alert), "Error: ${t.message}")
                }

            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

}