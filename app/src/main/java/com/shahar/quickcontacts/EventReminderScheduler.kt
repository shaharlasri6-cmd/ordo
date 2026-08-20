package com.shahar.quickcontacts
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object EventReminderScheduler {
    fun schedule(context: Context, item: EventItem) {
        if(item.reminderMinutes < 0) return
        val at=item.atMillis-item.reminderMinutes*60_000L
        if(at<=System.currentTimeMillis()) return
        val alarm=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent=Intent(context,ReminderReceiver::class.java).apply{
            putExtra("id",item.id xor 0x45E7L); putExtra("title","אירוע מתקרב: ${item.title}"); putExtra("mode","sound")
        }
        val pi=PendingIntent.getBroadcast(context,(item.id xor 0x45E7L).hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try { if(Build.VERSION.SDK_INT>=31 && !alarm.canScheduleExactAlarms()) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi) else alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi) }
        catch(_:Exception){ alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi) }
    }
    fun cancel(context:Context,item:EventItem){
        val alarm=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi=PendingIntent.getBroadcast(context,(item.id xor 0x45E7L).hashCode(),Intent(context,ReminderReceiver::class.java),PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if(pi!=null) alarm.cancel(pi)
    }
}
