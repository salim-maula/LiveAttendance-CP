package com.example.aplikasiabsensi.views.attendance

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.aplikasiabsensi.R
import com.example.aplikasiabsensi.databinding.FragmentAttendanceBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class AttendanceFragment : Fragment(), OnMapReadyCallback {

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

    private fun setUpMaps() {
        //https://developers.google.com/maps/documentation/android-sdk/map-with-marker?hl=id
        //pada dokumentasinya disitu menggunakan supportFragmentManager karna pada activity
        //sedangkan jika menggunakan fragment maka kita menggunakan childFragment
        mapAttendance = childFragmentManager.findFragmentById(R.id.map_attendance) as SupportMapFragment
        //this digunakan karena interface sudah digunakan di awal kelas
        mapAttendance?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        map = googleMap
        val myLoc = LatLng(-6.525181486714435, 107.03791879060388)
        map?.moveCamera(CameraUpdateFactory.newLatLng(myLoc))
        map?.addMarker(
            MarkerOptions()
                .position(myLoc)
                .title("Marker in Sydney")
        )
        map?.animateCamera(CameraUpdateFactory.zoomTo(20f))
    }

}