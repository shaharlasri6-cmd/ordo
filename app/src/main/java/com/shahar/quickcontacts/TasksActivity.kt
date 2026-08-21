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
class TasksActivity:Activity(){
 private lateinit var list:LinearLayout;private var items= mutableListOf<TaskItem>()
 override fun onCreate(b:Bundle?){super.onCreate(b);items=PersonalStore.loadTasks(this);build();if(intent.getBooleanExtra("autoAdd",false))window.decorView.post{addTask()}}
 private fun build(){window.statusBarColor=UiKit.bg;window.navigationBarColor=UiKit.bg;val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;setBackgroundColor(UiKit.bg);setPadding(UiKit.dp(this@TasksActivity,18),UiKit.dp(this@TasksActivity,38),UiKit.dp(this@TasksActivity,18),UiKit.dp(this@TasksActivity,18))};root.addView(UiKit.header(this,"משימות","לחיצה מסמנת כהושלם · לחיצה ארוכה מוחקת"));val add=UiKit.moduleCard(this,"＋","משימה חדשה","הוסף משהו לרשימה",UiKit.mint);add.setOnClickListener{addTask()};root.addView(add);val sc=ScrollView(this);list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};sc.addView(list);root.addView(sc,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));setContentView(root);render()}
 private fun addTask(){OrdoDialogs.input(this,"משימה חדשה","משהו קצר וברור שתוכל לסיים","מה צריך לעשות?","הוסף"){title->items.add(0,TaskItem(System.currentTimeMillis(),title));save()}}
 private fun render(){list.removeAllViews();val active=items.filter{!it.done}.sortedByDescending{it.createdAt};active.forEach{item->val row=ListRows.row(this,item.title,"פתוחה","○");row.setOnClickListener{val i=items.indexOfFirst{it.id==item.id};if(i>=0)items[i]=items[i].copy(done=true);save()};row.setOnLongClickListener{items.removeAll{it.id==item.id};save();Toast.makeText(this,"המשימה נמחקה",Toast.LENGTH_SHORT).show();true};list.addView(row)};if(active.isEmpty())list.addView(UiKit.subtitle(this,"אין משימות פתוחות"))}
 private fun save(){PersonalStore.saveTasks(this,items);QuickContactsWidget.refreshAll(this);render()}
}
