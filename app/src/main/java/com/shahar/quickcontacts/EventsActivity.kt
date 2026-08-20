package com.shahar.quickcontacts
import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class EventsActivity:Activity(){
 private lateinit var list:LinearLayout;private var items= mutableListOf<EventItem>();private val fmt=SimpleDateFormat("EEE dd/MM  HH:mm",Locale.getDefault())
 override fun onCreate(b:Bundle?){super.onCreate(b);items=PersonalStore.loadEvents(this);build()}
 private fun build(){window.statusBarColor=UiKit.bg;window.navigationBarColor=UiKit.bg;val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;setBackgroundColor(UiKit.bg);setPadding(UiKit.dp(this@EventsActivity,18),UiKit.dp(this@EventsActivity,38),UiKit.dp(this@EventsActivity,18),UiKit.dp(this@EventsActivity,18))};root.addView(UiKit.header(this,"אירועים קרובים","בחר מתי האירוע וגם כמה זמן לפניו Ordo תתריע"));val add=UiKit.moduleCard(this,"＋","אירוע חדש","זמן + התראה מראש לבחירתך",UiKit.mint);add.setOnClickListener{createEvent()};root.addView(add);val sc=ScrollView(this);list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};sc.addView(list);root.addView(sc,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));setContentView(root);render()}
 private fun createEvent(){OrdoDialogs.twoInputs(this,"אירוע חדש","שם האירוע והערה קצרה אם צריך","שם האירוע","הערה (אופציונלי)","בחר זמן"){title,note->DateTimePicker.pick(this){at->chooseLead(title,note,at)}}}
 private fun chooseLead(title:String,note:String,at:Long){val labels=arrayOf("בלי התראה","בזמן האירוע","5 דקות לפני","10 דקות לפני","15 דקות לפני","30 דקות לפני","שעה לפני","שעתיים לפני","יום לפני");val vals=intArrayOf(-1,0,5,10,15,30,60,120,1440);OrdoDialogs.choices(this,"מתי להתריע?","בחר כמה זמן לפני האירוע לקבל התראה",labels){i->val item=EventItem(System.currentTimeMillis(),title,at,note,vals[i]);items.add(item);PersonalStore.saveEvents(this,items);EventReminderScheduler.schedule(this,item);render()}}
 private fun leadText(m:Int)=when(m){-1->"בלי התראה";0->"בזמן האירוע";60->"שעה לפני";120->"שעתיים לפני";1440->"יום לפני";else->"$m דקות לפני"}
 private fun render(){list.removeAllViews();val now=System.currentTimeMillis();items.sortedBy{it.atMillis}.forEach{item->val sub=buildString{append(fmt.format(Date(item.atMillis)));append(" · ${leadText(item.reminderMinutes)}");if(item.note.isNotBlank())append(" · ${item.note}")};val row=ListRows.row(this,item.title,sub,if(item.atMillis<now)"✓" else "◫");row.alpha=if(item.atMillis<now).5f else 1f;row.setOnLongClickListener{EventReminderScheduler.cancel(this,item);items.removeAll{it.id==item.id};PersonalStore.saveEvents(this,items);render();true};list.addView(row)};if(items.isEmpty())list.addView(UiKit.subtitle(this,"אין אירועים"))}
}
