package com.shahar.quickcontacts

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import java.util.Calendar

object DateTimePicker {
    fun pick(activity: Activity, initial: Long = System.currentTimeMillis() + 3600000L, onPicked: (Long) -> Unit) {
        val c=Calendar.getInstance().apply{timeInMillis=initial}
        DatePickerDialog(activity,{_,y,m,d-> pickTime(activity,y,m,d,c,onPicked)},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show()
    }

    fun pickTodayDefault(activity: Activity, onPicked: (Long) -> Unit) {
        AlertDialog.Builder(activity)
            .setTitle("מתי להזכיר?")
            .setMessage("ברירת המחדל היא היום. צריך יום אחר? אפשר לבחור תאריך אחר.")
            .setNegativeButton("ביטול",null)
            .setNeutralButton("תאריך אחר") { _,_ -> pick(activity,onPicked=onPicked) }
            .setPositiveButton("היום · בחר שעה") { _,_ ->
                val c=Calendar.getInstance(); pickTime(activity,c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH),c,onPicked,true)
            }.show()
    }

    private fun pickTime(activity: Activity,y:Int,m:Int,d:Int,seed:Calendar,onPicked:(Long)->Unit, rejectPast:Boolean=false) {
        TimePickerDialog(activity,{_,h,min->
            val out=Calendar.getInstance().apply{set(y,m,d,h,min,0);set(Calendar.MILLISECOND,0)}
            if(rejectPast && out.timeInMillis <= System.currentTimeMillis()) Toast.makeText(activity,"השעה הזאת כבר עברה היום",Toast.LENGTH_SHORT).show()
            else onPicked(out.timeInMillis)
        },seed.get(Calendar.HOUR_OF_DAY),seed.get(Calendar.MINUTE),true).show()
    }
}
