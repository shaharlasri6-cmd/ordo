package com.shahar.quickcontacts

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.util.Calendar

object DateTimePicker {
    fun pick(activity: Activity, initial: Long = System.currentTimeMillis() + 3600000L, onPicked: (Long) -> Unit) {
        val c = Calendar.getInstance().apply { timeInMillis = initial }
        DatePickerDialog(activity, { _, y, m, d ->
            TimePickerDialog(activity, { _, h, min ->
                val out = Calendar.getInstance().apply {
                    set(Calendar.YEAR,y); set(Calendar.MONTH,m); set(Calendar.DAY_OF_MONTH,d)
                    set(Calendar.HOUR_OF_DAY,h); set(Calendar.MINUTE,min)
                    set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0)
                }
                onPicked(out.timeInMillis)
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }
}
