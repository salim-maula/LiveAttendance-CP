package com.example.aplikasiabsensi.views.attendance

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.location.LocationManager
import android.content.pm.PackageManager
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.IntentSender
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.nfc.Tag
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.aplikasiabsensi.BuildConfig
import com.example.aplikasiabsensi.R
import com.example.aplikasiabsensi.databinding.BottomSheetAttendanceBinding
import com.example.aplikasiabsensi.databinding.FragmentAttendanceBinding
import com.example.aplikasiabsensi.date.MyDate
import com.example.aplikasiabsensi.dialog.MyDialog
import com.example.aplikasiabsensi.hawkstorage.HawkStorage
import com.example.aplikasiabsensi.model.AttendanceResponse
import com.example.aplikasiabsensi.model.HistoryResponse
import com.example.aplikasiabsensi.networking.ApiServices
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.jetbrains.anko.toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AttendanceFragment : Fragment(), OnMapReadyCallback {

    private val mapPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private var mapAttendance: SupportMapFragment? = null
    private var map: GoogleMap? = null
    private var binding: FragmentAttendanceBinding? = null

    //1.a karna layout bottom sheet berbeda layout maka kita harus menggunakannya seperti ini
    private var bindingBottomSheet: BottomSheetAttendanceBinding? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    private var locationManager: LocationManager? = null
    private var locationRequest: LocationRequest? = null
    private var locationSettingsRequest: LocationSettingsRequest? = null
    private var settingsClient: SettingsClient? = null

    //kita gunakan untuk mendapatkan current location
    private var currentLocation: Location? = null
    private var locationCallback: LocationCallback? = null
    //digunakan untuk mencari lokasi saat ini
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var currentPhotoPath = ""

    private var isCheckIn = false

    //untuk mengakses kamera
    private val cameraPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAttendanceBinding.inflate(inflater, container, false)
        bindingBottomSheet = binding?.layoutBottomSheet
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        bindingBottomSheet = null
    }

    override fun onDestroy() {
        super.onDestroy()
        //ketika kita berganti layar lokasinya tidak terus terupdate
        if (currentLocation != null && locationCallback != null){
            fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
        }
    }

    override fun onResume() {
        super.onResume()
        checkIfAlreadyPresent()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        setUpMaps()
        onClick()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMAGE_CAPTURE){
            if (resultCode == Activity.RESULT_OK){
                if (currentPhotoPath.isNotEmpty()){
                    val uri = Uri.parse(currentPhotoPath)
                    bindingBottomSheet?.ivCapturePhoto?.setImageURI(uri)
                    bindingBottomSheet?.ivCapturePhoto?.adjustViewBounds = true
                }
            }else{
                if (currentPhotoPath.isNotEmpty()){
                    val file = File(currentPhotoPath)
                    file.delete()
                    currentPhotoPath = ""
                    context?.toast(getString(R.string.failed_to_capture_image))
                }
            }
        }
    }

    private fun onClick() {
        binding?.fabGetCurrentLocation?.setOnClickListener {
            goToCurrentLocation()
        }

        bindingBottomSheet?.ivCapturePhoto?.setOnClickListener {
            if (checkPermissionCamera()){
                openCamera()
            }else{
                setRequestPermissionCamera()
            }
        }

        bindingBottomSheet?.btnCheckIn?.setOnClickListener {
            val token = HawkStorage.instance(context).getToken()
            if (checkValidation()){
                if (isCheckIn){
                    AlertDialog.Builder(context)
                        .setTitle(getString(R.string.are_you_sure))
                        .setPositiveButton(getString(R.string.yes)){ _, _ ->
                            sendDataAttendance(token, "out")
                        }
                        .setNegativeButton(getString(R.string.no)){ dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }else{
                    AlertDialog.Builder(context)
                        .setTitle(getString(R.string.are_you_sure))
                        .setPositiveButton(getString(R.string.yes)){ _, _ ->
                            sendDataAttendance(token, "in")
                        }
                        .setNegativeButton(getString(R.string.no)){ dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }
        }
    }




    private fun init(){
        //setup location
        locationManager = context?.getSystemService(LOCATION_SERVICE) as LocationManager
        //disni terdapat perbedaan sehingga kita harus menggunakan requireContext
        settingsClient = LocationServices.getSettingsClient(requireContext())
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        locationRequest = LocationRequest()
            .setInterval(10000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest!!)
        locationSettingsRequest = builder.build()
        //Setup Bottomsheet
        //1.b agar bottom sheetnya selalu terbuka maka kita harus menggunakan behavior
        bottomSheetBehavior = BottomSheetBehavior.from(bindingBottomSheet!!.bottomSheetAttendance)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
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
            val myLoc = LatLng(-6.524572408746366, 107.04627408064717)
//            map?.addMarker(
//                MarkerOptions()
//                    .position(myLoc)
//                    .title("Marker in Sydney")
//            )
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
    //2.1 disini kita check terlebih dahulu apakah lokasi isLocationEnabled
    private fun goToCurrentLocation() {
        bindingBottomSheet?.tvCurrentLocation?.text = getString(R.string.search_your_location)
        if (checkPermission()){
            if (isLocationEnabled()){
                //ini digunakan untuk membuat titik warna biru pada map
                map?.isMyLocationEnabled = true
                //ini digunakan untuk memfokuskan kordinat kita, disini saya matikan
                // dikarenakan kita akan menggunakan Fab untuk memfokuskannya
                map?.uiSettings?.isMyLocationButtonEnabled = false

                locationCallback = object : LocationCallback(){
                    override fun onLocationResult(locationResult: LocationResult?) {
                        super.onLocationResult(locationResult)
                        currentLocation = locationResult?.lastLocation

                        if (currentLocation != null){
                            val latitude = currentLocation?.latitude
                            val longitude  = currentLocation?.longitude

                            if (latitude != null && longitude != null){
                                val latLng = LatLng(latitude, longitude)
                                map?.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                                map?.animateCamera(CameraUpdateFactory.zoomTo(20f))

                                //pada bagian ini kita akan membuat function yang
                            // akan merubah koordinat mejadi sebuah alamat
                                val address = getAddress(latitude, longitude)
                                if (address != null && address.isNotEmpty()){
                                    bindingBottomSheet?.tvCurrentLocation?.text = address
                                }
                            }
                        }
                    }
                }
                //pada bagian ini akan memberikan kita update lokasi kita secara otomatis
                fusedLocationProviderClient?.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.myLooper()
                )
            }else{
                goToTurnOnGps()
            }
        }else{
            setRequestPermission()
        }
    }

    private fun getAddress(latitude: Double, longitude: Double): String? {
        val result: String
        context?.let {
            val geoCode = Geocoder(it, Locale.getDefault())
            val address = geoCode.getFromLocation(latitude, longitude, 1)
            if (address.size > 0){
                result = address[0].getAddressLine(0)
                return result
            }
        }
        return null
    }

    //2.2 Disini kita melakukan pengecekan apakah lokasinya sudah aktif atau blm
    // dan kita menerapkan 2 pilihan gps dan network
    private fun isLocationEnabled(): Boolean{
        if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER)!! ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER)!!){
            return true
        }
        return false
    }

    //2.3 pada method ini dia akan mengaktifkan secara paksa, jika bisa dia akan masuk kedalam goToCurrentLocation
    private fun goToTurnOnGps() {
        settingsClient?.checkLocationSettings(locationSettingsRequest)
            ?.addOnSuccessListener {
                goToCurrentLocation()
            }?.addOnFailureListener {
                when((it as ApiException).statusCode){
                    //2.4 jika gagal dia akan mengaktifkan secara paksa dengan memunculkan notif
                        // tanpa harus ke settingan terlebih dahulu
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED->{
                        try {
                            val resolveApiException = it as ResolvableApiException
                            resolveApiException.startResolutionForResult(
                                activity,
                                REQUEST_CODE_LOCATION
                            )
                        }catch (ex: IntentSender.SendIntentException){
                            ex.printStackTrace()
                            Log.e(TAG, "Error: ${ex.message}")
                        }
                    }
                }
            }
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

            REQUEST_CODE_CAMERA_PERMISSIONS->{

                var isHasPermission = false
                val permissionNotGranted = StringBuilder()

                for (i in permissions.indices){
                    isHasPermission = grantResults[i] == PackageManager.PERMISSION_GRANTED

                    if (!isHasPermission){
                        permissionNotGranted.append("${permissions[i]}\n")
                    }
                }

                if (isHasPermission){
                    openCamera()
                }else{
                    val message = permissionNotGranted.toString() + "\n" + getString(R.string.not_granted)
                    MyDialog.dynamicDialog(context, getString(R.string.not_granted), message)
                }
            }
        }
    }

    private fun checkPermissionCamera(): Boolean {
        var isHasPermission = false
        context?.let {
            for (permission in cameraPermissions){
                isHasPermission = ActivityCompat.checkSelfPermission(it, permission) == PackageManager.PERMISSION_GRANTED
            }
        }
        return isHasPermission
    }

    private fun setRequestPermissionCamera() {
        requestPermissions(cameraPermissions, REQUEST_CODE_CAMERA_PERMISSIONS)
    }

    private fun openCamera() {
         context?.let { context ->
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
             //disini kita akan mengatur agar gambar bisa disimpan di storage kita
             //disini kita akan mengecek kamera agar tidak ada masalah dan bisa digunakan
            if (cameraIntent.resolveActivity(context.packageManager) != null){
                val photoFile = try {
                    //disini kita akan embuat sebuah file kosong
                    createImageFile()
                }catch (ex: IOException){
                    null
                }
                photoFile?.also {
                    val photoUri = FileProvider.getUriForFile(
                        context,
                        BuildConfig.APPLICATION_ID + ".fileprovider",
                        it
                    )
                    //untuk membuka camera cukup code ini saja
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    //kemudian hasilnya akan disimpan kedalam activity result
                    startActivityForResult(cameraIntent, REQUEST_CODE_IMAGE_CAPTURE)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }


    private fun checkValidation(): Boolean {
        if (currentPhotoPath.isEmpty()){
            MyDialog.dynamicDialog(context, getString(R.string.alert), getString(R.string.please_take_your_photo))
            return false
        }
        return true
    }

    private fun sendDataAttendance(token: String, type: String) {
        val params = HashMap<String, RequestBody>()
        MyDialog.showProgressDialog(context)
        if (currentLocation != null && currentPhotoPath.isNotEmpty()){
            val latitude = currentLocation?.latitude.toString()
            val longitude = currentLocation?.longitude.toString()
            val address = bindingBottomSheet?.tvCurrentLocation?.text.toString()

            val file = File(currentPhotoPath)
            val uri = FileProvider.getUriForFile(
                //disini untuk context kita ubah menjadi requireContext
                requireContext(),
                BuildConfig.APPLICATION_ID + ".fileprovider",
                file
            )
            val typeFile = context?.contentResolver?.getType(uri)

            val mediaTypeText = MultipartBody.FORM
            //type file disini masih berupa string dan kita mengubahnya menjadi mediatype
            val mediaTypeFile = typeFile?.toMediaType()

            //pada bagian ini biasa kita menggunakan RequestBody.create()
            // sehingga solusinya kita menggunakan .toRequestBody
            //mediaTypeText digunakan untuk mengirim data selain file seperti lat, long dan address
            val requestLatitude = latitude.toRequestBody(mediaTypeText)
            val requestLongitude = longitude.toRequestBody(mediaTypeText)
            val requestAddress = address.toRequestBody(mediaTypeText)
            val requestType = type.toRequestBody(mediaTypeText)

            //disini sesuaikan semua parameter dengan yg ada di postman
            params["lat"] = requestLatitude
            params["long"] = requestLongitude
            params["address"] = requestAddress
            params["type"] = requestType

            //untuk mengirim file kita harus menggunakan asRequestBody
            val requestPhotoFile = file.asRequestBody(mediaTypeFile)
            val multipartBody = MultipartBody.Part.createFormData("photo", file.name, requestPhotoFile)
            ApiServices.getLiveAttendance()
                .attend("Bearer $token", params, multipartBody)
                .enqueue(object : Callback<AttendanceResponse> {
                    override fun onResponse(
                        call: Call<AttendanceResponse>,
                        response: Response<AttendanceResponse>
                    ) {
                        MyDialog.hideDialog()
                        if (response.isSuccessful){
                            val attendanceResponse = response.body()
                            currentPhotoPath = ""
                            bindingBottomSheet?.ivCapturePhoto?.setImageDrawable(
                                ContextCompat.getDrawable(context!!,R.drawable.ic_baseline_add_circle_24)
                            )
                            bindingBottomSheet?.ivCapturePhoto?.adjustViewBounds = false

                            if (type == "in"){
                                MyDialog.dynamicDialog(context, getString(R.string.success_check_in), attendanceResponse?.message.toString())
                            }else{
                                MyDialog.dynamicDialog(context, getString(R.string.success_check_out), attendanceResponse?.message.toString())
                            }
                            checkIfAlreadyPresent()
                        }else{
                            MyDialog.dynamicDialog(context, getString(R.string.alert), getString(
                                R.string.something_wrong))
                        }
                    }

                    override fun onFailure(call: Call<AttendanceResponse>, t: Throwable) {
                        MyDialog.hideDialog()
                        Log.e(TAG, "Error: ${t.message}")
                    }

                })
        }
    }

    private fun checkIfAlreadyPresent() {
        val token = HawkStorage.instance(context).getToken()
        val currentDate = MyDate.getCurrentDateForServer()

        ApiServices.getLiveAttendance()
            .getHistoryAttendance("Bearer $token", currentDate, currentDate)
            .enqueue(object : Callback<HistoryResponse>{
                override fun onResponse(
                    call: Call<HistoryResponse>,
                    response: Response<HistoryResponse>
                ) {
                    if (response.isSuccessful){
                        val histories = response.body()?.histories
                        if (histories != null && histories.isNotEmpty()){
                            if (histories[0]?.status == 1){
                                isCheckIn = false
                                checkIsCheckIn()
                                bindingBottomSheet?.btnCheckIn?.isEnabled = false
                                bindingBottomSheet?.btnCheckIn?.text = getString(R.string.your_already_present)
                            }else{
                                isCheckIn = true
                                checkIsCheckIn()
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                    Log.e(TAG, "Error: ${t.message}")
                }

            })
    }

    private fun checkIsCheckIn() {
        if (isCheckIn){
            bindingBottomSheet?.btnCheckIn?.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_check_out)
            bindingBottomSheet?.btnCheckIn?.text = getString(R.string.check_out)
        }else{
            bindingBottomSheet?.btnCheckIn?.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_btn_primary)
            bindingBottomSheet?.btnCheckIn?.text = getString(R.string.check_in)
        }
    }


    companion object {
        private const val REQUEST_CODE_MAP_PERMISSION = 1000
        private const val REQUEST_CODE_LOCATION = 2000
        private val TAG = AttendanceFragment::class.java.simpleName
        private const val REQUEST_CODE_IMAGE_CAPTURE = 2001
        private const val REQUEST_CODE_CAMERA_PERMISSIONS = 1001
    }

}