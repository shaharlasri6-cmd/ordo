package com.shahar.quickcontacts
import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity:Activity(){
 private val updateManager by lazy{UpdateManager(this)}; private lateinit var summary:TextView
 override fun onCreate(b:Bundle?){super.onCreate(b);requestNotificationPermission();buildUi();window.decorView.postDelayed({updateManager.check(false)},900)}
 override fun onResume(){super.onResume();updateManager.resumePendingInstaller();if(::summary.isInitialized) refreshSummary()}
 private fun buildUi(){
  window.statusBarColor=UiKit.bg;window.navigationBarColor=UiKit.bg
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;setBackgroundColor(UiKit.bg);setPadding(UiKit.dp(this@MainActivity,18),UiKit.dp(this@MainActivity,18),UiKit.dp(this@MainActivity,18),UiKit.dp(this@MainActivity,18))}
  root.setOnApplyWindowInsetsListener{v,i->val bars=if(Build.VERSION.SDK_INT>=30)i.getInsets(WindowInsets.Type.systemBars()) else null;v.setPadding(UiKit.dp(this,18),(bars?.top?:0)+UiKit.dp(this,15),UiKit.dp(this,18),(bars?.bottom?:0)+UiKit.dp(this,15));i}
  val scroll=ScrollView(this);val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL}
  c.addView(UiKit.title(this,"Ordo",34f));c.addView(UiKit.subtitle(this,"היום שלך. מסודר במקום אחד.").apply{setPadding(0,4,0,18)})
  summary=TextView(this).apply{textSize=15f;setTextColor(UiKit.ink);background=UiKit.rounded(UiKit.surface2,UiKit.dp(this@MainActivity,22),android.graphics.Color.rgb(50,59,86));setPadding(UiKit.dp(this@MainActivity,17),UiKit.dp(this@MainActivity,16),UiKit.dp(this@MainActivity,17),UiKit.dp(this@MainActivity,16))}
  c.addView(summary,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT).apply{bottomMargin=UiKit.dp(this@MainActivity,17)})
  val contacts=UiKit.moduleCard(this,"☎","אנשי קשר","חיוג מהיר והווידג'ט השקוף",UiKit.mint);contacts.setOnClickListener{startActivity(Intent(this,ContactsActivity::class.java))};c.addView(contacts)
  val tasks=UiKit.moduleCard(this,"✓","משימות","רשימת הדברים שצריך לסיים");tasks.setOnClickListener{startActivity(Intent(this,TasksActivity::class.java))};c.addView(tasks)
  val rem=UiKit.moduleCard(this,"⏰","תזכורות","היום כברירת מחדל, עם צליל או רטט",UiKit.mint);rem.setOnClickListener{startActivity(Intent(this,RemindersActivity::class.java))};c.addView(rem)
  val ev=UiKit.moduleCard(this,"◫","אירועים קרובים","אירוע + התראה מראש בזמן שתבחר");ev.setOnClickListener{startActivity(Intent(this,EventsActivity::class.java))};c.addView(ev)
  val widget=UiKit.moduleCard(this,"▦","ווידג\'ט Ordo","אנשי קשר, משימות ותזכורות במסך הבית",UiKit.mint);widget.setOnClickListener{pinOrdoWidget()};c.addView(widget)
  val up=UiKit.compactButton(this,"↻  בדוק עדכונים");up.setOnClickListener{updateManager.check(true)};c.addView(up,LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT).apply{topMargin=UiKit.dp(this@MainActivity,7)})
  scroll.addView(c);root.addView(scroll,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));setContentView(root);refreshSummary()
 }
 private fun pinOrdoWidget(){
  val manager=AppWidgetManager.getInstance(this)
  val provider=ComponentName(this,QuickContactsWidget::class.java)
  val existing=manager.getAppWidgetIds(provider)
  if(existing.isNotEmpty()){
   android.widget.Toast.makeText(this,"הווידג'ט של Ordo כבר קיים במסך הבית",android.widget.Toast.LENGTH_SHORT).show()
   return
  }
  if(android.os.Build.VERSION.SDK_INT>=26&&manager.isRequestPinAppWidgetSupported){
   manager.requestPinAppWidget(provider,null,null)
  }else{
   android.widget.Toast.makeText(this,"במסך הבית: לחיצה ארוכה → ווידג'טים → Ordo",android.widget.Toast.LENGTH_LONG).show()
  }
 }
 private fun refreshSummary(){val open=PersonalStore.loadTasks(this).count{!it.done};val now=System.currentTimeMillis();val nr=PersonalStore.loadReminders(this).filter{it.atMillis>=now}.minByOrNull{it.atMillis};val ne=PersonalStore.loadEvents(this).filter{it.atMillis>=now}.minByOrNull{it.atMillis};val f=SimpleDateFormat("HH:mm",Locale.getDefault());summary.text=buildString{append("היום שלך  •  $open משימות פתוחות");nr?.let{append("\nהתזכורת הבאה: ${it.title} · ${f.format(Date(it.atMillis))}")};ne?.let{append("\nהאירוע הבא: ${it.title} · ${f.format(Date(it.atMillis))}")}}}
 private fun requestNotificationPermission(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),701)}
}
