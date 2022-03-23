package com.example.aplikasiabsensi.views.attendance

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import com.example.aplikasiabsensi.R
import com.example.aplikasiabsensi.databinding.FragmentAttendanceBinding
import com.example.aplikasiabsensi.dialog.MyDialog
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class AttendanceFragment : Fragment(), OnMapReadyCallback {

    private val mapPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private var mapAttendance: SupportMapFragment? = null
    private var map: GoogleMap? = null
    private var binding: FragmentAttendanceBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAttendanceBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpMaps()
    }

    //1.fungsi yang akan pertama kali dijalankan
    private fun setUpMaps() {
        //https://developers.google.com/maps/documentation/android-sdk/map-with-marker?hl=id
        //pada dokumentasinya disitu menggunakan supportFragmentManager karna pada activity
        //sedangkan jika menggunakan fragment maka kita menggunakan childFragment
        mapAttendance =
            childFragmentManager.findFragmentById(R.id.map_attendance) as SupportMapFragment
        //this digunakan karena interface sudah digunakan di awal kelas
        //2.kemudian masuk kedalam getMapAsync
        mapAttendance?.getMapAsync(this)
    }

    //3.Setelah itu akan masuk kedalam callback nya iaitu onMapReady
    override fun onMapReady(googleMap: GoogleMap?) {
        map = googleMap
        //4.kemudian akan masuk kedalam function checkpermission jika true maka akan mendapatkan lokasi
        //jika tidak makan akan meminta untuk setRequestPermission(
        if (checkPermission()) {
            val myLoc = LatLng(-6.525181486714435, 107.03791879060388)
            map?.addMarker(
                MarkerOptions()
                    .position(myLoc)
                    .title("Marker in Sydney")
            )
            map?.moveCamera(CameraUpdateFactory.newLatLng(myLoc))
            map?.animateCamera(CameraUpdateFactory.zoomTo(20f))

            goToCurrentLocation()
        } else {
            setRequestPermission()
        }
    }

    private fun checkPermission(): Boolean {
        var isHasPermission = false
        context?.let {
            for (permission in mapPermissions) {
                isHasPermission = ActivityCompat.checkSelfPermission(
                    it,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        return isHasPermission
    }

    private fun goToCurrentLocation() {
        TODO("Not yet implemented")
    }

    //5. meminta agar memunculkan izin ke pada user
    private fun setRequestPermission() {
        requestPermissions(mapPermissions, REQUEST_CODE_MAP_PERMISSION)
    }


    //6.jika izinnya sudah di ok maka akan masuk kedalam onRequestPermissionsResult
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_MAP_PERMISSION -> {
                var isHasPermission = false
                val permissionNotGranted = StringBuilder()

                for (i in permissions.indices) {
                    isHasPermission = grantResults[i] == PackageManager.PERMISSION_GRANTED

                    if (!isHasPermission) {
                        permissionNotGranted.append("${permissions[i]}\n")
                    }
                }

                if (isHasPermission) {
                    setUpMaps()
                } else {
                    val message =
                        permissionNotGranted.toString() + "\n" + getString(R.string.not_granted)
                    MyDialog.dynamicDialog(
                        context,
                        getString(R.string.required_permission),
                        message
                    )
                }
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_MAP_PERMISSION = 1000

    }

}