package com.shahar.quickcontacts
import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class RemindersActivity:Activity(){
 private lateinit var list:LinearLayout;private var items= mutableListOf<ReminderItem>();private val fmt=SimpleDateFormat("dd/MM/yyyy  HH:mm",Locale.getDefault())
 override fun onCreate(b:Bundle?){super.onCreate(b);items=PersonalStore.loadReminders(this);build();if(intent.getBooleanExtra("autoAdd",false))window.decorView.post{ensureExactAlarmAccessThenCreate()}}
 private fun build(){window.statusBarColor=UiKit.bg;window.navigationBarColor=UiKit.bg;val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;setBackgroundColor(UiKit.bg);setPadding(UiKit.dp(this@RemindersActivity,18),UiKit.dp(this@RemindersActivity,38),UiKit.dp(this@RemindersActivity,18),UiKit.dp(this@RemindersActivity,18))};root.addView(UiKit.header(this,"תזכורות","ברירת המחדל היא היום — בוחרים רק שעה. אפשר גם לבחור יום אחר."));val add=UiKit.moduleCard(this,"＋","תזכורת חדשה","היום · שעה · צליל / רטט / שקט",UiKit.mint);add.setOnClickListener{ensureExactAlarmAccessThenCreate()};root.addView(add);val sc=ScrollView(this);list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};sc.addView(list);root.addView(sc,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));setContentView(root);render()}
 private fun ensureExactAlarmAccessThenCreate(){if(android.os.Build.VERSION.SDK_INT>=31){val a=getSystemService(ALARM_SERVICE) as android.app.AlarmManager;if(!a.canScheduleExactAlarms()){AlertDialog.Builder(this).setTitle("תזכורות בזמן מדויק").setMessage("כדי ש-Ordo תתריע בדיוק בזמן, צריך לאשר ל-Android תזכורות מדויקות.").setNegativeButton("לא עכשיו",null).setPositiveButton("אפשר"){_,_->try{startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:$packageName")))}catch(_:Exception){}}.show();return}};createReminder()}
 private fun createReminder(){OrdoDialogs.input(this,"תזכורת חדשה","כתוב מה להזכיר ואז בחר זמן מהיר","מה להזכיר?","המשך"){title->chooseQuickTime(title)}}
 private fun chooseQuickTime(title:String){val labels=arrayOf("עוד 15 דקות","עוד 30 דקות","עוד שעה","עוד 5 שעות","עוד 10 שעות","זמן אחר");val offsets=longArrayOf(15*60_000L,30*60_000L,60*60_000L,5*60*60_000L,10*60*60_000L,-1L);OrdoDialogs.choices(this,"מתי להזכיר?","בחר זמן מוכן או זמן אחר",labels){i->if(offsets[i]>0L){chooseMode(title,System.currentTimeMillis()+offsets[i])}else{DateTimePicker.pickTodayDefault(this){at->chooseMode(title,at)}}}}
 private fun chooseMode(title:String,at:Long){OrdoDialogs.choices(this,"איך להתריע?","בחר איך Ordo תמשוך את תשומת הלב שלך",arrayOf("🔔  צליל","📳  רטט","◌  שקט")){w->val mode=when(w){0->"sound";1->"vibrate";else->"silent"};val item=ReminderItem(System.currentTimeMillis(),title,at,mode);items.add(item);PersonalStore.saveReminders(this,items);ReminderScheduler.schedule(this,item);QuickContactsWidget.refreshAll(this);render();Toast.makeText(this,"התזכורת נשמרה",Toast.LENGTH_SHORT).show()}}
 private fun render(){list.removeAllViews();val now=System.currentTimeMillis();val active=items.filter{it.atMillis>now}.sortedBy{it.atMillis};active.forEach{item->val mode=when(item.alertMode){"sound"->"צליל";"vibrate"->"רטט";else->"שקט"};val row=ListRows.row(this,item.title,"${fmt.format(Date(item.atMillis))} · $mode","⏰");row.setOnLongClickListener{ReminderScheduler.cancel(this,item);items.removeAll{it.id==item.id};PersonalStore.saveReminders(this,items);QuickContactsWidget.refreshAll(this);render();true};list.addView(row)};if(active.isEmpty())list.addView(UiKit.subtitle(this,"אין תזכורות קרובות"))}
}
