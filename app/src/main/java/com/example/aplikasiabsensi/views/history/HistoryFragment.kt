package com.example.aplikasiabsensi.views.history

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.extensions.OnCalendarPageChangedListener
import com.applandeo.materialcalendarview.listeners.OnCalendarPageChangeListener
import com.applandeo.materialcalendarview.listeners.OnDayClickListener
import com.example.aplikasiabsensi.R
import com.example.aplikasiabsensi.databinding.FragmentHistoryBinding
import com.example.aplikasiabsensi.date.MyDate.fromTimeStampToDate
import com.example.aplikasiabsensi.date.MyDate.toCalendar
import com.example.aplikasiabsensi.date.MyDate.toDate
import com.example.aplikasiabsensi.date.MyDate.toDay
import com.example.aplikasiabsensi.date.MyDate.toMonth
import com.example.aplikasiabsensi.date.MyDate.toTime
import com.example.aplikasiabsensi.dialog.MyDialog
import com.example.aplikasiabsensi.hawkstorage.HawkStorage
import com.example.aplikasiabsensi.model.History
import com.example.aplikasiabsensi.model.HistoryResponse
import com.example.aplikasiabsensi.networking.ApiServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*


class HistoryFragment : Fragment() {

    private var binding: FragmentHistoryBinding? = null
    //disini kita menggunakan library untuk mendapatkan ceklis di setiap bulannya
    private val events = mutableListOf<EventDay>()
    private var dataHistories: List<History?>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //1.1 disini kita membuat init untuk memanggil beberapa fungsi
        init()
    }

    private fun init(){
        //1.2 disini kita membuar request terlebih dahulu
        //Request data history
        requestDataHistory()

        //Setup Calendar Swipe
        //1.5 disini kita akan membuat setup calendar yang berguna untuk ketika kita mengswipe calendar dan akan membaca setiap absensi kita pada bulan yg kita swipe
        setupCalendar()

        //OnClick
        //1.6 ketika kita mengklik setiap tanggal
        onClick()
    }

    private fun onClick() {
        binding?.calendarViewHistory?.setOnDayClickListener(object : OnDayClickListener{
            override fun onDayClick(eventDay: EventDay) {
                val clickedDayCalendar = eventDay.calendar
                //pada bagian ini akan menampilkan data hari dan bulannya
                binding?.tvCurrentDate?.text = clickedDayCalendar.toDate().toDay()
                binding?.tvCurrentMonth?.text = clickedDayCalendar.toDate().toMonth()


                //pada bagian ini kita akan mendapatkan data waktu setiap checkIn dan checkOut
                if (dataHistories != null){
                    for (dataHistory in dataHistories!!){
                        val checkInTime: String
                        val checkOutTime: String
                        val updateDate = dataHistory?.updatedAt

                        //pada bagian ini kita akan merubah nilai string pada api menjadi data calendar
                        val calendarUpdated = updateDate?.fromTimeStampToDate()?.toCalendar()
                        if (clickedDayCalendar.get(Calendar.DAY_OF_MONTH) == calendarUpdated?.get(Calendar.DAY_OF_MONTH)){
                            if (dataHistory.status == 1){
                                checkInTime = dataHistory.detail?.get(0)?.createdAt.toString()
                                checkOutTime = dataHistory.detail?.get(1)?.createdAt.toString()

                                binding?.tvTimeCheckIn?.text = checkInTime.fromTimeStampToDate()?.toTime()
                                binding?.tvTimeCheckOut?.text = checkOutTime.fromTimeStampToDate()?.toTime()
                                break
                            }else{
                                checkInTime = dataHistory.detail?.get(0)?.createdAt.toString()
                                binding?.tvTimeCheckIn?.text = checkInTime.fromTimeStampToDate()?.toTime()
                                break
                            }
                        }else{
                            binding?.tvTimeCheckIn?.text = getString(R.string.default_text)
                            binding?.tvTimeCheckOut?.text = getString(R.string.default_text)
                        }
                    }
                }
            }

        })
    }

    private fun setupCalendar() {
        binding?.calendarViewHistory?.setOnPreviousPageChangeListener(object : OnCalendarPageChangeListener{
            override fun onChange() {
                requestDataHistory()
            }
        })

        binding?.calendarViewHistory?.setOnForwardPageChangeListener(object : OnCalendarPageChangeListener{
            override fun onChange() {
                requestDataHistory()
            }
        })
    }



    private fun requestDataHistory() {
        //1.3 disini kita akan mendapatkan tanggal pada calendar
        val calendar = binding?.calendarViewHistory?.currentPageDate
        //disini kita akan mendapatkan jumlah bulan aktual karna disetiap tanggal ada 26,30 dan 31
        val lastDay = calendar?.getActualMaximum(Calendar.DAY_OF_MONTH)
        val month = calendar?.get(Calendar.MONTH)?.plus(1)
        val year = calendar?.get(Calendar.YEAR)

        //disini kita akan membuat format agar bisa dimasukkan kedalam post sesuai denga param nya
        val fromDate = "$year-$month-01"
        val toDate = "$year-$month-$lastDay"
        //1.4 selanjutnya kita akan masuk kedalam getDataHistory
        getDataHistory(fromDate, toDate)
    }

    private fun getDataHistory(fromDate: String, toDate: String) {
        val token = HawkStorage.instance(context).getToken()
        binding?.pbHistory?.visibility = View.VISIBLE
        ApiServices.getLiveAttendance()
            .getHistoryAttendance("Bearer $token", fromDate, toDate)
            .enqueue(object : Callback<HistoryResponse>{
                override fun onResponse(
                    call: Call<HistoryResponse>,
                    response: Response<HistoryResponse>
                ) {
                    binding?.pbHistory?.visibility = View.GONE
                    if (response.isSuccessful){
                        dataHistories = response.body()?.histories
                        if (dataHistories != null){
                            for (dataHistory in dataHistories!!){
                                val status = dataHistory?.status
                                val checkInTime: String
                                val checkOutTime: String
                                val calendarHistoryCheckIn: Calendar?
                                val calendarHistoryCheckOut: Calendar?
                                val currentDate = Calendar.getInstance()

                                if (status == 1){
                                    checkInTime = dataHistory.detail?.get(0)?.createdAt.toString()
                                    checkOutTime = dataHistory.detail?.get(1)?.createdAt.toString()

                                    calendarHistoryCheckOut = checkOutTime.fromTimeStampToDate()?.toCalendar()

                                    if (calendarHistoryCheckOut != null){
                                        events.add(EventDay(calendarHistoryCheckOut, R.drawable.ic_baseline_check_circle_primary_24))
                                    }
                                    // pada kode ini kita akan mendapatkan keterangan tanggal absensi kita
                                    if (currentDate.get(Calendar.DAY_OF_MONTH) == calendarHistoryCheckOut?.get(Calendar.DAY_OF_MONTH)){
                                        binding?.tvCurrentDate?.text = checkInTime.fromTimeStampToDate()?.toDay()
                                        binding?.tvCurrentMonth?.text = checkInTime.fromTimeStampToDate()?.toMonth()
                                        binding?.tvTimeCheckIn?.text = checkInTime.fromTimeStampToDate()?.toTime()
                                        binding?.tvTimeCheckOut?.text = checkOutTime.fromTimeStampToDate()?.toTime()
                                    }
                                }else{
                                    checkInTime = dataHistory?.detail?.get(0)?.createdAt.toString()
                                    calendarHistoryCheckIn = checkInTime.fromTimeStampToDate()?.toCalendar()

                                    if (calendarHistoryCheckIn != null){
                                        events.add(EventDay(calendarHistoryCheckIn, R.drawable.ic_baseline_check_circle_yellow_light_24))
                                    }

                                    if (currentDate.get(Calendar.DAY_OF_MONTH) == calendarHistoryCheckIn?.get(Calendar.DAY_OF_MONTH)){
                                        binding?.tvCurrentDate?.text = checkInTime.fromTimeStampToDate()?.toDay()
                                        binding?.tvCurrentMonth?.text = checkInTime.fromTimeStampToDate()?.toMonth()
                                        binding?.tvTimeCheckIn?.text = checkInTime.fromTimeStampToDate()?.toTime()
                                    }
                                }
                            }
                        }
                        binding?.calendarViewHistory?.setEvents(events)
                    }else{
                        MyDialog.dynamicDialog(context, getString(R.string.alert), getString(R.string.something_wrong))
                    }
                }

                override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                    binding?.pbHistory?.visibility = View.GONE
                    MyDialog.dynamicDialog(context, getString(R.string.alert), "${t.message}")
                    Log.e(TAG, "Error: ${t.message}")
                }

            })
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    private companion object{
        private val TAG: String = HistoryFragment::class.java.simpleName
    }

}