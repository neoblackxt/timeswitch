package com.github.ghmxr.timeswitch.activities;

import com.github.ghmxr.timeswitch.R;
import com.github.ghmxr.timeswitch.adapters.MainListAdapter;
import com.github.ghmxr.timeswitch.data.v2.PublicConsts;
import com.github.ghmxr.timeswitch.data.v2.SQLConsts;
import com.github.ghmxr.timeswitch.TaskItem;
import com.github.ghmxr.timeswitch.data.v2.TriggerTypeConsts;
import com.github.ghmxr.timeswitch.services.TimeSwitchService;
import com.github.ghmxr.timeswitch.data.v2.MySQLiteOpenHelper;
import com.github.ghmxr.timeswitch.utils.ProcessTaskItem;
import com.github.ghmxr.timeswitch.utils.ValueUtils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.transition.TransitionManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * @author mxremail@qq.com  https://github.com/ghmxr/timeswitch
 */
public class MainActivity extends BaseActivity implements AdapterView.OnItemClickListener,AdapterView.OnItemLongClickListener{
    /**
     * @deprecated
     */
    ListView listview;
    FloatingActionButton fab;

    /**
     * @deprecated
     */
	public MainListAdapter listview_adapter;
	SwipeRefreshLayout swrlayout;
	RecyclerView recyclerView;

	boolean isMultiSelectMode=false;
	Menu menu;
	private boolean ifRefresh=true;


	public static final int MESSAGE_GETLIST_COMPLETE   =   1;

	public static final int MESSAGE_SHOW_INDICATOR      =   3;
	public static final int MESSAGE_HIDE_INDICATOR      =   4;
	public static final int MESSAGE_REQUEST_UPDATE_LIST =5;
	public static final int MESSAGE_OPEN_MULTI_SELECT_MODE=6;
	public static final int MESSAGE_ON_ICON_FOLDED_PROCESS_COMPLETE=7;

	private static final int MESSAGE_DELETE_SELECTED_ITEMS_COMPLETE  =10;

	private static final int REQUEST_CODE_ACTIVITY_ADD=100;
	private static final int REQUEST_CODE_ACTIVITY_EDIT=101;
	private static final int REQUEST_CODE_ACTIVITY_SETTINGS=102;
	private static final int REQUEST_CODE_ACTIVITY_PROFILE=103;

	public static final int MENU_FOLD=0;
	public static final int MENU_DELETE=1;
	public static final int MENU_SELECT_ALL=2;
	public static final int MENU_DESELCT_ALL=3;
	public static final int MENU_PROFILE=4;
	public static final int MENU_SETTINGS=5;

	private final BroadcastReceiver batteryReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent==null||intent.getAction()==null||!intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) return;
            refreshIndicatorOfBatteryInfo(intent);
        }
    };

    private long first_click_delete=0;

    public void onCreate(Bundle mybundle){
		super.onCreate(mybundle);
		setContentView(R.layout.layout_main);
        SharedPreferences settings=getSharedPreferences(PublicConsts.PREFERENCES_NAME,Activity.MODE_PRIVATE);
        Toolbar toolbar =findViewById(R.id.toolbar);
        listview=findViewById(R.id.main_listview);
        swrlayout=findViewById(R.id.main_swrlayout);
        recyclerView=(RecyclerView)findViewById(R.id.main_recyclerview);
        findViewById(R.id.main_indicator).setVisibility(settings.getBoolean(PublicConsts.PREFERENCES_MAINPAGE_INDICATOR,PublicConsts.PREFERENCES_MAINPAGE_INDICATOR_DEFAULT)?View.VISIBLE:View.GONE);
		setSupportActionBar(toolbar);
		String color=settings.getString(PublicConsts.PREFERENCES_THEME_COLOR,PublicConsts.PREFERENCES_THEME_COLOR_DEFAULT);
		setToolBarAndStatusBarColor(toolbar,color);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isMultiSelectMode) closeMultiSelectMode();
                startActivityForResult(new Intent(MainActivity.this,AddTask.class),REQUEST_CODE_ACTIVITY_ADD);
            }
        });

        swrlayout.setColorSchemeColors(Color.parseColor(getSharedPreferences(PublicConsts.PREFERENCES_NAME,Context.MODE_PRIVATE)
                .getString(PublicConsts.PREFERENCES_THEME_COLOR,PublicConsts.PREFERENCES_THEME_COLOR_DEFAULT)));
        swrlayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
               startService2Refresh();
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                TransitionManager.endTransitions(((ViewGroup)recyclerView));
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        startRefreshingIndicator();

        /*myHandler.post(new Runnable() {
            @Override
            public void run() {
                if(ifRefresh){
                    myHandler.postDelayed(this,500);
                    if(listview_adapter!=null) listview_adapter.refreshAllCertainTimeTaskItems();
                    //Log.d("Thread_refrfesh_B","Thread sleep!!!");
                }

            }
        }); */

        TimeSwitchService.startService(this);
	}

	@Override
    public void onResume(){
        super.onResume();
        //startService2Refresh();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQUEST_CODE_ACTIVITY_ADD){
            if(resultCode==RESULT_OK){
                startService2Refresh();
                //sendEmptyMessage(MESSAGE_GETLIST_COMPLETE);
            }
        }else if(requestCode==REQUEST_CODE_ACTIVITY_EDIT){
            if(resultCode==RESULT_OK){
                startService2Refresh();
                //sendEmptyMessage(MESSAGE_GETLIST_COMPLETE);
            }
        }else if(requestCode==REQUEST_CODE_ACTIVITY_PROFILE){
            if(resultCode==RESULT_OK){
                listview_adapter =null;
                listview.setAdapter(null);
                startService2Refresh();
            }
        }else if(requestCode==REQUEST_CODE_ACTIVITY_SETTINGS){
            if(resultCode==Settings.RESULT_CHAGED_INDICATOR_STATE){
                SharedPreferences settings=getSharedPreferences(PublicConsts.PREFERENCES_NAME,Activity.MODE_PRIVATE);
                findViewById(R.id.main_indicator).setVisibility(settings.getBoolean(PublicConsts.PREFERENCES_MAINPAGE_INDICATOR,PublicConsts.PREFERENCES_MAINPAGE_INDICATOR_DEFAULT)?View.VISIBLE:View.GONE);
            }
        }
    }


    private void checkIfHasServiceInstanceAndRun(){
        if(TimeSwitchService.service==null){
            startService2Refresh();
        }else{
            sendEmptyMessage(MESSAGE_GETLIST_COMPLETE);
        }
    }

    public void startService2Refresh(){
        swrlayout.setRefreshing(true);
        ((ListAdapter)recyclerView.getAdapter()).removeTouchHelper();
        recyclerView.setAdapter(null);
        TimeSwitchService.startService(this);
    }


    /**
     * 主界面任务列表 listview 的Item点击回调
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if(!isMultiSelectMode) {
            startEditTaskActivity(i);
        }else{
            listview_adapter.onMultiSelectModeItemClicked(i);
            /*boolean ifHasSelectedItem=false;
            boolean isSelected [] =listview_adapter.getIsSelected();
            for(int j=0;j<isSelected.length;j++){
                if(isSelected[j]) ifHasSelectedItem=true;
            }
            this.menu.getItem(MENU_DELETE).setEnabled(ifHasSelectedItem);*/
        }
    }

    private void startEditTaskActivity(int position){
        Intent intent = new Intent();
        intent.putExtra(EditTask.TAG_SELECTED_ITEM_POSITION,position);
        intent.setClass(this, EditTask.class);
        startActivityForResult(intent,REQUEST_CODE_ACTIVITY_EDIT);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        //openMultiSelectMode(i);
        return true;
    }


    public void processMessage(Message msg) {
        switch (msg.what){
            case MESSAGE_REQUEST_UPDATE_LIST:{
                recyclerView.getAdapter().notifyDataSetChanged();
            }
            break;
            case MESSAGE_GETLIST_COMPLETE:{
                //this.list=TimeSwitchService.list;
                LinearLayoutManager manager=new LinearLayoutManager(this);
                GridLayoutManager gmanager=new GridLayoutManager(this,3);
                manager.setOrientation(LinearLayoutManager.VERTICAL);
               recyclerView.setLayoutManager(manager);
                ListAdapter adapter=new ListAdapter(TimeSwitchService.list);
               recyclerView.setAdapter(adapter);
                adapter.itemTouchHelper.attachToRecyclerView(recyclerView);

                this.swrlayout.setRefreshing(false);
            }
            break;
            case MESSAGE_DELETE_SELECTED_ITEMS_COMPLETE:{
                startService2Refresh();
                try{
                    menu.getItem(MENU_DELETE).setEnabled(true);
                }catch (Exception e){e.printStackTrace();}
            }
            break;
            case MESSAGE_HIDE_INDICATOR:{
                findViewById(R.id.main_indicator).setVisibility(View.GONE);
            }
            break;
            case MESSAGE_SHOW_INDICATOR:{
                findViewById(R.id.main_indicator).setVisibility(View.VISIBLE);
            }
            break;
            case MESSAGE_ON_ICON_FOLDED_PROCESS_COMPLETE:{
                try{
                    swrlayout.setRefreshing(false);
                    menu.getItem(MENU_FOLD).setEnabled(true);
                    recyclerView.setAdapter(new ListAdapter(TimeSwitchService.list));
                }catch (Exception e){e.printStackTrace();}
            }
            break;
        }
    }

    private void setFabVisibility(boolean isVisible){
        if(fab==null) return;
        fab.setAnimation(isVisible?AnimationUtils.loadAnimation(MainActivity.this,R.anim.anim_fab_enter):AnimationUtils.loadAnimation(MainActivity.this,R.anim.anim_fab_exit));
        fab.setVisibility(isVisible?View.VISIBLE:View.GONE);
        //isFabVisible=isVisible;
    }

    private void openMultiSelectMode(){
        this.isMultiSelectMode=true;
        this.swrlayout.setEnabled(false);
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.menu.getItem(MENU_DELETE).setVisible(true);
        this.menu.getItem(MENU_DELETE).setEnabled(true);
        this.menu.getItem(MENU_SELECT_ALL).setVisible(true);
        this.menu.getItem(MENU_DESELCT_ALL).setVisible(true);
        this.menu.getItem(MENU_PROFILE).setVisible(false);
        this.menu.getItem(MENU_SETTINGS).setVisible(false);
        setFabVisibility(false);
    }

    private void closeMultiSelectMode(){
        this.isMultiSelectMode=false;
        this.swrlayout.setEnabled(true);
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        ((ListAdapter)recyclerView.getAdapter()).closeMultiSelectMode();
        this.menu.getItem(MENU_DELETE).setVisible(false);
        this.menu.getItem(MENU_SELECT_ALL).setVisible(false);
        this.menu.getItem(MENU_DESELCT_ALL).setVisible(false);
        this.menu.getItem(MENU_PROFILE).setVisible(true);
        this.menu.getItem(MENU_SETTINGS).setVisible(true);
        setFabVisibility(true);
    }

    private void startRefreshingIndicator(){
        this.ifRefresh=true;
        try{
            registerReceiver(batteryReceiver,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }catch (Exception e){
            e.printStackTrace();
        }

       myHandler.post(new Runnable() {
           @Override
           public void run() {
               if(Build.VERSION.SDK_INT>=21&&ifRefresh){
                   myHandler.postDelayed(this,1000);
                   BatteryManager batteryManager=(BatteryManager) getSystemService(Activity.BATTERY_SERVICE);
                   try{
                       refreshIndicatorOfBatteryCurrent(batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW));
                   }catch (java.lang.NullPointerException ne){
                        refreshIndicatorOfBatteryCurrent(0);
                   }catch (Exception e){
                       e.printStackTrace();
                   }
               }
           }
       });

        myHandler.post(new Runnable() {
            @Override
            public void run() {
                if (ifRefresh){
                    myHandler.postDelayed(this,500);
                     Calendar calendar=Calendar.getInstance();
                     calendar.setTimeInMillis(System.currentTimeMillis());
                     int month=calendar.get(Calendar.MONTH)+1;
                    // BatteryManager batteryManager=(BatteryManager) getSystemService(Activity.BATTERY_SERVICE);
                     TextView tv_time=findViewById(R.id.main_indicator_time_value);
                     tv_time.setText(ValueUtils.format(calendar.get(Calendar.YEAR))
                             +"/"+ ValueUtils.format(month)
                             +"/"+ ValueUtils.format(calendar.get(Calendar.DAY_OF_MONTH))
                             +"("+ValueUtils.getDayOfWeek(calendar.getTimeInMillis())
                             +")/" +ValueUtils.format(calendar.get(Calendar.HOUR_OF_DAY))
                             +":"+ValueUtils.format(calendar.get(Calendar.MINUTE))
                            +":"+ValueUtils.format(calendar.get(Calendar.SECOND)));

                    //Log.e("mhandler","activated!!!");
                }
            }
        });
    }

    private void refreshIndicatorOfBatteryInfo(Intent intent){
        int level=intent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
        int temperature=intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,-1);
        float voltage=(float)intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE,-1000)/1000;
        boolean isCharging=intent.getIntExtra(BatteryManager.EXTRA_STATUS,-1)==BatteryManager.BATTERY_STATUS_CHARGING;

        ImageView img_level=findViewById(R.id.main_indicator_battery_percentage_img);
        TextView tv_percentage=findViewById(R.id.main_indicator_battery_percentage_value);
        TextView tv_temperature=findViewById(R.id.main_indicator_battery_temperature_value);
        TextView tv_voltage=findViewById(R.id.main_indicator_battery_voltage_value);
        TextView tv_current=findViewById(R.id.main_indicator_battery_current_value);
        if(isCharging) {
            tv_current.setTextColor(getResources().getColor(R.color.color_current_charge));
            img_level.setImageResource(R.drawable.icon_battery_charging);
        }
        else {
            tv_current.setTextColor(getResources().getColor(R.color.color_current_discharge));

            if(level>=90) img_level.setImageResource(R.drawable.icon_battery_full);
            else if(level>70) img_level.setImageResource(R.drawable.icon_battery_high);
            else if(level>45) img_level.setImageResource(R.drawable.icon_battery_medium);
            else if(level>30) img_level.setImageResource(R.drawable.icon_battery_low);
            else if(level>=0)img_level.setImageResource(R.drawable.icon_battery_low_alarm);
        }

        if(level>0) tv_percentage.setText(level+"%");
        if(temperature!=-1)tv_temperature.setText((double)temperature/10+"℃");
        if(voltage!=-1)tv_voltage.setText(voltage+"V");
    }

    private void refreshIndicatorOfBatteryCurrent(int current){
        TextView tv_current=findViewById(R.id.main_indicator_battery_current_value);
        if(current<0) current=0-current;
        tv_current.setText(current/1000+"mA");
    }

    @Override
    public void finish(){
        super.finish();
        this.ifRefresh=false;
        ((ListAdapter)recyclerView.getAdapter()).removeTouchHelper();
        recyclerView.setAdapter(null);
        try{
            unregisterReceiver(batteryReceiver);
        }catch (Exception e){
            e.printStackTrace();
        }
        try{
            while (queue.size()>0) queue.getLast().finish();
        }catch (Exception e){e.printStackTrace();}
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            if(isMultiSelectMode) closeMultiSelectMode();
            else finish();
            return true;
        }
        return super.onKeyDown(keyCode,event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main,menu);
        this.menu=menu;
        setIconEnable(menu,true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            default:break;
            case android.R.id.home:{
                if(isMultiSelectMode){
                    closeMultiSelectMode();
                }
            }
            break;
            case R.id.action_selectall:{
                //listview_adapter.selectAll();
                menu.getItem(MENU_DELETE).setEnabled(true);
            }
            break;
            case R.id.action_deselectall:{
                //listview_adapter.deselectAll();
                this.menu.getItem(MENU_DELETE).setEnabled(false);
            }
            break;
            case R.id.action_delete_selected:{
                long clickedTime=System.currentTimeMillis();
                if(clickedTime-first_click_delete>1000){
                    first_click_delete=clickedTime;
                    Snackbar.make(fab,getResources().getString(R.string.dialog_profile_delete_confirm),Snackbar.LENGTH_SHORT).show();
                    return false;
                }

                try{
                    menu.getItem(MENU_DELETE).setEnabled(false);
                    closeMultiSelectMode();
                    swrlayout.setRefreshing(true);
                }catch (Exception e){e.printStackTrace();}

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            SQLiteDatabase database=MySQLiteOpenHelper.getInstance(MainActivity.this).getWritableDatabase();
                            boolean isSelected[]=MainActivity.this.listview_adapter.getIsSelected();
                            for(int i=0;i<isSelected.length;i++){
                                if(isSelected[i]){
                                    int key=TimeSwitchService.list.get(i).id;
                                    TimeSwitchService.list.get(i).cancelTask();
                                    //list.remove(i);
                                    database.delete(MySQLiteOpenHelper.getCurrentTableName(MainActivity.this),SQLConsts.SQL_TASK_COLUMN_ID +"="+key,null);
                                }
                            }
                            sendEmptyMessage(MESSAGE_DELETE_SELECTED_ITEMS_COMPLETE);
                        }catch (Exception e){e.printStackTrace();}
                    }
                }).start();
            }
            break;
            case R.id.action_settings:{
                Intent i=new Intent();
                i.setClass(this,Settings.class);
                i.putExtra(EXTRA_TITLE_COLOR,getSharedPreferences(PublicConsts.PREFERENCES_NAME,Activity.MODE_PRIVATE).getString(PublicConsts.PREFERENCES_THEME_COLOR,PublicConsts.PREFERENCES_THEME_COLOR_DEFAULT));
                startActivityForResult(i,REQUEST_CODE_ACTIVITY_SETTINGS);
            }
            break;
            case R.id.action_profile:{
                Intent i=new Intent();
                i.setClass(this,Profile.class);
                i.putExtra(EXTRA_TITLE_COLOR,getSharedPreferences(PublicConsts.PREFERENCES_NAME,Activity.MODE_PRIVATE).getString(PublicConsts.PREFERENCES_THEME_COLOR,PublicConsts.PREFERENCES_THEME_COLOR_DEFAULT));
                startActivityForResult(i,REQUEST_CODE_ACTIVITY_PROFILE);
            }
            break;
            case R.id.action_fold:{
                try{
                    menu.getItem(MENU_FOLD).setEnabled(false);
                    recyclerView.setAdapter(null);
                    swrlayout.setRefreshing(true);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                boolean isAllFolded=true;
                                for(TaskItem i:TimeSwitchService.list){
                                    if(!i.addition_isFolded) {
                                        isAllFolded=false;
                                        break;
                                    }
                                }
                                if(isAllFolded){
                                    for(int i=0;i<TimeSwitchService.list.size();i++){
                                        //TimeSwitchService.list.get(i).addition_isFolded=false;
                                        try{
                                            ProcessTaskItem.setTaskFolded(MainActivity.this, TimeSwitchService.list.get(i).id,false);
                                        }catch (Exception e){e.printStackTrace();}
                                    }
                                }else{
                                    for(int i=0;i<TimeSwitchService.list.size();i++){
                                        //TimeSwitchService.list.get(i).addition_isFolded=true;
                                        try{
                                            ProcessTaskItem.setTaskFolded(MainActivity.this, TimeSwitchService.list.get(i).id,true);
                                        } catch (Exception e){e.printStackTrace();}
                                    }
                                }
                                sendEmptyMessage(MESSAGE_ON_ICON_FOLDED_PROCESS_COMPLETE);
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 此类中的列表数据类型为TaskItem List
     */
   private class ListAdapter extends RecyclerView.Adapter<ViewHolder>{
        private boolean [] isSelected;
        private boolean isMultiSelectMode=false;
        private final List<TaskItem> list;
        ListAdapter(@NonNull List<TaskItem> list){
            this.list=list;
            isSelected=new boolean[list.size()];
            RelativeLayout area=findViewById(R.id.main_no_task_att);
            if(area!=null) area.setVisibility(list.size()>0?View.GONE:View.VISIBLE);

        }
       @NonNull
       @Override
       public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
           return new ViewHolder(LayoutInflater.from(MainActivity.this).inflate(R.layout.item_task,parent,false));
       }

        @Override
       public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
           if(holder.getAdapterPosition()>=list.size()) {
               holder.root.setVisibility(View.INVISIBLE);
               return;
           }else {
               holder.root.setVisibility(View.VISIBLE);
           }

           TaskItem item=null;
           try{
               item=list.get(position);
           }catch (Exception e){
               e.printStackTrace();
           }

           if(item==null) {
               holder.root.setVisibility(View.GONE);
               return;
           }

           holder.tv_name.setText(item.name);
           int color_value = Color.parseColor(item.addition_title_color);
           holder.title.setBackgroundColor(color_value);
           holder.task_area.setVisibility(item.addition_isFolded?View.GONE:View.VISIBLE);
           holder.title_arrow.setRotation(item.addition_isFolded?0:90);
           if(ValueUtils.isHighLightRGB(color_value)){
               holder.tv_name.setTextColor(getResources().getColor(R.color.color_black));
               holder.title_arrow.setImageResource(R.drawable.arrow_right_item_folding_black);
           }

           holder.title.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   if(holder.task_area.getVisibility()!=View.VISIBLE){
                       TransitionManager.beginDelayedTransition(((ViewGroup)recyclerView));
                       holder.task_area.setVisibility(View.VISIBLE);
                       holder.title_arrow.setRotation(90);
                       try{
                           ProcessTaskItem.setTaskFolded(MainActivity.this,TimeSwitchService.list.get(holder.getAdapterPosition()).id,false);
                       }catch (Exception e){e.printStackTrace();}
                   }else{
                       TransitionManager.beginDelayedTransition(((ViewGroup)recyclerView));
                       holder.task_area.setVisibility(View.GONE);
                       holder.title_arrow.setRotation(0);
                       try{
                           ProcessTaskItem.setTaskFolded(MainActivity.this,TimeSwitchService.list.get(holder.getAdapterPosition()).id,true);
                       }catch (Exception e){e.printStackTrace();}
                   }
               }
           });

           holder.img_trigger.setImageResource(item.display_trigger_icon_res);
           holder.tv_trigger.setText(item.display_trigger);
           holder.tv_exception.setText(item.display_exception);
           holder.tv_action.setText(item.display_actions);
           holder.tv_addition.setText(item.display_additions);
           if(item.autodelete||item.autoclose){
               holder.addition.setVisibility(View.VISIBLE);
               if(!item.autodelete&&item.trigger_type== TriggerTypeConsts.TRIGGER_TYPE_SINGLE) holder.addition.setVisibility(View.GONE);
           }else holder.addition.setVisibility(View.GONE);

           CheckBox cb = holder.cb;
           SwitchCompat switchCompat = holder.switch_enabled;
           if (isMultiSelectMode) {
               switchCompat.setVisibility(View.GONE);
               cb.setVisibility(View.VISIBLE);
               cb.setOnCheckedChangeListener(null);
               cb.setChecked(isSelected[position]);
               cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                   @Override
                   public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                       isSelected[holder.getAdapterPosition()] = isChecked;
                   }
               });
               holder.title.setOnLongClickListener(null);
               holder.task_area.setOnClickListener(new View.OnClickListener() {
                   @Override
                   public void onClick(View v) {
                       isSelected[holder.getAdapterPosition()]=!isSelected[holder.getAdapterPosition()];
                       holder.cb.setChecked(isSelected[holder.getAdapterPosition()]);
                   }
               });
               holder.task_area.setOnLongClickListener(null);
           } else {
               switchCompat.setVisibility(View.VISIBLE);
               cb.setVisibility(View.GONE);
               switchCompat.setOnCheckedChangeListener(null);
               switchCompat.setChecked(item.isenabled);
               switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                   @Override
                   public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                       if(b&&list.get(holder.getAdapterPosition()).trigger_type == TriggerTypeConsts.TRIGGER_TYPE_SINGLE&&(list.get(holder.getAdapterPosition()).time<System.currentTimeMillis())){
                           Snackbar.make(fab,getResources().getString(R.string.activity_main_toast_task_invalid),Snackbar.LENGTH_SHORT)
                                   .setAction(getResources().getString(R.string.activity_main_toast_task_invalid_action), new View.OnClickListener() {
                                       @Override
                                       public void onClick(View view) {
                                           startEditTaskActivity(holder.getAdapterPosition());
                                       }
                                   }).show();

                           return;
                       }
                       try{
                           ProcessTaskItem.setTaskEnabled(TimeSwitchService.service,list.get(holder.getAdapterPosition()).id,b);
                       }catch (Exception e){
                           e.printStackTrace();
                       }
                   }
               });
               View.OnLongClickListener longClickListener=new View.OnLongClickListener() {
                   @Override
                   public boolean onLongClick(View v) {
                       openMultiSelectMode(holder.getAdapterPosition());
                       return true;
                   }
               };
               //holder.title.setOnLongClickListener(longClickListener);
               holder.task_area.setOnLongClickListener(longClickListener);
               holder.task_area.setOnClickListener(new View.OnClickListener() {
                   @Override
                   public void onClick(View v) {
                       startEditTaskActivity(holder.getAdapterPosition());
                   }
               });
           }
       }

       @Override
       public int getItemCount() {
           return list.size()+2;
       }

        private void openMultiSelectMode(int position){
            this.isMultiSelectMode=true;
            isSelected=new boolean[list.size()];
            isSelected[position]=true;
            itemTouchHelper.attachToRecyclerView(null);
            notifyDataSetChanged();
            MainActivity.this.openMultiSelectMode();
       }

       private void closeMultiSelectMode(){
            this.isMultiSelectMode=false;
            itemTouchHelper.attachToRecyclerView(recyclerView);
            notifyDataSetChanged();
       }

       private boolean[] getIsSelected () {return isSelected;}

       void removeTouchHelper(){itemTouchHelper.attachToRecyclerView(null);}

        private final ItemTouchHelper itemTouchHelper =new ItemTouchHelper(new ItemTouchHelper.Callback(){
            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
                    final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN |
                            ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                    final int swipeFlags = 0;
                    return makeMovementFlags(dragFlags, swipeFlags);
                } else {
                    final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                    final int swipeFlags = 0;
                    return makeMovementFlags(dragFlags, swipeFlags);
                }
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();

                int toPosition = target.getAdapterPosition();

                if(toPosition>=list.size()) return false;

                if (fromPosition < toPosition) {
                    for (int i = fromPosition; i < toPosition; i++) {
                        Collections.swap(list, i, i + 1);
                    }
                } else {
                    for (int i = fromPosition; i > toPosition; i--) {
                        Collections.swap(list, i, i - 1);
                    }
                }
                recyclerView.getAdapter().notifyItemMoved(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

            }
        } );

    }

   private static class ViewHolder extends RecyclerView.ViewHolder{
       View root;
       View title;
       ImageView title_arrow;
       SwitchCompat switch_enabled;
       View task_area;
       CheckBox cb;
       TextView tv_name;
       ImageView img_trigger;
       TextView tv_trigger,tv_exception,tv_action,tv_addition;
       View addition;

       public ViewHolder(View view) {
           super(view);
           title=view.findViewById(R.id.item_task_title);
           title_arrow=view.findViewById(R.id.item_task_title_arrow);
           switch_enabled=view.findViewById(R.id.item_task_switch);
           cb=view.findViewById(R.id.item_task_checkbox);
           task_area=view.findViewById(R.id.item_task_info);
           tv_name=view.findViewById(R.id.item_task_name);
           img_trigger=view.findViewById(R.id.item_task_trigger_icon);
           tv_trigger=view.findViewById(R.id.item_task_trigger_value);
           tv_exception=view.findViewById(R.id.item_task_exception);
           tv_action=view.findViewById(R.id.item_task_action);
           tv_addition=view.findViewById(R.id.item_task_addition);
           addition=view.findViewById(R.id.item_task_addition_area);
           root=view;
       }

   }

}
