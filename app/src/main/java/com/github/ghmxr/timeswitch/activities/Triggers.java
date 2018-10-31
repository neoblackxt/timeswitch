package com.github.ghmxr.timeswitch.activities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.TimePicker;

import com.github.ghmxr.timeswitch.R;
import com.github.ghmxr.timeswitch.data.PublicConsts;
import com.github.ghmxr.timeswitch.ui.BottomDialogForBattery;
import com.github.ghmxr.timeswitch.ui.BottomDialogForInterval;
import com.github.ghmxr.timeswitch.ui.CustomTimePicker;
import com.github.ghmxr.timeswitch.utils.LogUtil;
import com.github.ghmxr.timeswitch.utils.ValueUtils;

public class Triggers extends BaseActivity implements View.OnClickListener,TimePicker.OnTimeChangedListener{
    public static final String EXTRA_TRIGGER_TYPE="trigger_type";
    public static final String EXTRA_TRIGGER_VALUES="trigger_values";

    private static final int MESSAGE_GET_SSID_COMPLETE=0x00001;

    private int trigger_type=0;
   // private long time=0;
    private boolean[] week_repeat=new boolean[]{true,true,true,true,true,true,true};
    private long interval=60*1000;
    private int battery_percentage=50,battery_temperature=35;
    private String wifi_ssidinfo ="";
    private String broadcast_intent_action="android.intent.ANSWER";

    CustomTimePicker timePicker;
    Calendar calendar;

    private String checkString="";
    private long first_clicked=0;

    private AlertDialog dialog_wait;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_triggers);
        Toolbar toolbar=findViewById(R.id.triggers_toolbar);
        setSupportActionBar(toolbar);

        try{getSupportActionBar().setDisplayHomeAsUpEnabled(true);}catch (Exception e){e.printStackTrace();}

        timePicker=findViewById(R.id.trigger_timepicker);
        timePicker.setIs24HourView(true);
        timePicker.setOnTimeChangedListener(this);

        calendar= Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis()+10*60*1000);
        calendar.set(Calendar.SECOND,0);

        findViewById(R.id.trigger_single).setOnClickListener(this);
        findViewById(R.id.trigger_percertaintime).setOnClickListener(this);
        findViewById(R.id.trigger_weekloop).setOnClickListener(this);
        findViewById(R.id.trigger_battery_percentage).setOnClickListener(this);
        findViewById(R.id.trigger_battery_temperature).setOnClickListener(this);
        findViewById(R.id.trigger_wifi_connected).setOnClickListener(this);
        findViewById(R.id.trigger_wifi_disconnected).setOnClickListener(this);
        findViewById(R.id.trigger_widget_changed).setOnClickListener(this);
        findViewById(R.id.trigger_received_broadcast).setOnClickListener(this);

        //initialize the values
        try{
            trigger_type=getIntent().getIntExtra(EXTRA_TRIGGER_TYPE,0);
            String trigger_values[]=getIntent().getStringArrayExtra(EXTRA_TRIGGER_VALUES);
            switch(trigger_type){
                default:break;
                case PublicConsts.TRIGGER_TYPE_SINGLE:{
                    //time=Long.parseLong(trigger_values[0]);
                    try{
                        calendar.setTimeInMillis(Long.parseLong(trigger_values[0]));
                        calendar.set(Calendar.SECOND,0);
                    }catch (Exception e){
                        LogUtil.putExceptionLog(this,e);
                        e.printStackTrace();
                    }
                }
                break;
                case PublicConsts.TRIGGER_TYPE_LOOP_BY_CERTAIN_TIME:{
                    //time=Long.parseLong(trigger_values[0]);
                    interval=Long.parseLong(trigger_values[0]);
                }
                break;
                case PublicConsts.TRIGGER_TYPE_LOOP_WEEK:{
                    //time=Long.parseLong(trigger_values[0]);
                    try{
                        calendar.setTimeInMillis(Long.parseLong(trigger_values[0]));
                        calendar.set(Calendar.SECOND,0);
                        for(int i=1;i<trigger_values.length;i++){
                            week_repeat[i-1]=Integer.parseInt(trigger_values[i])==1;
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                        LogUtil.putExceptionLog(this,e);
                    }

                }
                break;
                case PublicConsts.TRIGGER_TYPE_BATTERY_HIGHER_THAN_TEMPERATURE: case PublicConsts.TRIGGER_TYPE_BATTERY_LOWER_THAN_TEMPERATURE:{
                    battery_temperature=Integer.parseInt(trigger_values[0]);
                }
                break;
                case PublicConsts.TRIGGER_TYPE_BATTERY_MORE_THAN_PERCENTAGE: case PublicConsts.TRIGGER_TYPE_BATTERY_LESS_THAN_PERCENTAGE:{
                    battery_percentage=Integer.parseInt(trigger_values[0]);
                }
                break;

                case PublicConsts.TRIGGER_TYPE_RECEIVED_BROADTCAST:{
                    broadcast_intent_action=String.valueOf(trigger_values[0]);
                }
                break;

                case PublicConsts.TRIGGER_TYPE_WIFI_CONNECTED: case PublicConsts.TRIGGER_TYPE_WIFI_DISCONNECTED:{
                    wifi_ssidinfo=String.valueOf(trigger_values[0]);
                    //Log.d("wifi ssids" ,wifi_ssidinfo);
                }
                break;

            }
        }catch (Exception e){
            LogUtil.putExceptionLog(this,e);
            e.printStackTrace();
        }

        checkString=toCheckString();

        //set the views
        if(Build.VERSION.SDK_INT<23){
            timePicker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
            timePicker.setCurrentMinute(calendar.get(Calendar.MINUTE));
        }else{
            timePicker.setHour(calendar.get(Calendar.HOUR_OF_DAY));
            timePicker.setMinute(calendar.get(Calendar.MINUTE));
        }
        timePicker.setOnTimeChangedListener(this);

        activateTriggerType(trigger_type);
    }

    public String toCheckString() {
        return "Triggers{" +
                "trigger_type=" + trigger_type +
                ", week_repeat=" + Arrays.toString(week_repeat) +
                ", interval=" + interval +
                ", battery_percentage=" + battery_percentage +
                ", battery_temperature=" + battery_temperature +
                ", wifi_ssidinfo='" + wifi_ssidinfo + '\'' +
                ", broadcast_intent_action='" + broadcast_intent_action + '\'' +
                ", calendar=" + calendar.getTimeInMillis() +
                ", wifi_ssidinfo=" + wifi_ssidinfo +
                '}';
    }

    private void activateTriggerType(int type){
        trigger_type=type;
        refreshTriggerDisplayValues(type);
        switch (type){
            default:break;
            case PublicConsts.TRIGGER_TYPE_SINGLE:{
                ((TextView)findViewById(R.id.trigger_single_value)).setText(getSingleTimeDisplayValue(this,calendar.getTimeInMillis()));
                timePicker.setVisibility(View.VISIBLE);
            }
            break;
            case PublicConsts.TRIGGER_TYPE_LOOP_BY_CERTAIN_TIME:{
                ((TextView)findViewById(R.id.trigger_percertaintime_value)).setText(getCertainLoopTimeDisplayValue(this,interval));
                timePicker.setVisibility(View.GONE);
            }
            break;
            case PublicConsts.TRIGGER_TYPE_LOOP_WEEK:{
                ((TextView)findViewById(R.id.trigger_weekloop_value)).setText(getWeekLoopDisplayValue(this,week_repeat));
                timePicker.setVisibility(View.VISIBLE);
            }
            break;
            case PublicConsts.TRIGGER_TYPE_BATTERY_HIGHER_THAN_TEMPERATURE: case PublicConsts.TRIGGER_TYPE_BATTERY_LOWER_THAN_TEMPERATURE:{
                ((TextView)findViewById(R.id.trigger_battery_temperature_value)).setText(getBatteryTemperatureDisplayValue(this,type,battery_temperature));
                timePicker.setVisibility(View.GONE);
            }
            break;
            case PublicConsts.TRIGGER_TYPE_BATTERY_MORE_THAN_PERCENTAGE: case PublicConsts.TRIGGER_TYPE_BATTERY_LESS_THAN_PERCENTAGE:{
                ((TextView)findViewById(R.id.trigger_battery_percentage_value)).setText(getBatteryPercentageDisplayValue(this,type,battery_percentage));
                timePicker.setVisibility(View.GONE);
            }
            break;
            case PublicConsts.TRIGGER_TYPE_RECEIVED_BROADTCAST:{
                ((TextView)findViewById(R.id.trigger_received_broadcast_value)).setText(getBroadcastDisplayValue(broadcast_intent_action));
                timePicker.setVisibility(View.GONE);
            }
            break;
            case PublicConsts.TRIGGER_TYPE_WIFI_CONNECTED:{
                ((TextView)findViewById(R.id.trigger_wifi_connected_value)).setText(getWifiConnectionDisplayValue(this,wifi_ssidinfo));
                timePicker.setVisibility(View.GONE);
            }
            break;
            case PublicConsts.TRIGGER_TYPE_WIFI_DISCONNECTED:{
                ((TextView)findViewById(R.id.trigger_wifi_disconnected_value)).setText(getWifiConnectionDisplayValue(this,wifi_ssidinfo));
                timePicker.setVisibility(View.GONE);
            }
            break;
        }
    }

    @Override
    public void processMessage(Message msg) {
        switch (msg.what){
            default:break;
            case MESSAGE_GET_SSID_COMPLETE:{
                if(dialog_wait!=null) dialog_wait.cancel();
                View dialogview=LayoutInflater.from(this).inflate(R.layout.layout_dialog_with_listview,null);
                ListView wifi_list=dialogview.findViewById(R.id.layout_dialog_listview);
                final WifiInfoListAdapter adapter=new WifiInfoListAdapter((List<WifiConfiguration>)msg.obj, wifi_ssidinfo);
                //Log.d("wifi ssids ",wifi_ssidinfo);
                wifi_list.setAdapter(adapter);
                wifi_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        adapter.onItemClicked(i);
                    }
                });
               new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.activity_trigger_wifi_dialog_att))
                        .setView(dialogview)
                        .setPositiveButton(getResources().getString(R.string.dialog_button_positive), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                activateTriggerType(PublicConsts.TRIGGER_TYPE_WIFI_CONNECTED);
                                wifi_ssidinfo =adapter.getSelectedIDs();
                                ((TextView)findViewById(R.id.trigger_wifi_connected_value)).setText(getWifiConnectionDisplayValue(Triggers.this,wifi_ssidinfo));
                            }
                        })
                        .setNegativeButton(getResources().getString(R.string.dialog_button_negative), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        })
                        .show();
            }
            break;
        }
    }

    @Override
    public void onClick(View v){
        switch(v.getId()){
            default:break;
            case R.id.trigger_single:{
                activateTriggerType(PublicConsts.TRIGGER_TYPE_SINGLE);
                new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
                        calendar.set(Calendar.YEAR,year);
                        calendar.set(Calendar.MONTH,month);
                        calendar.set(Calendar.DAY_OF_MONTH,dayOfMonth);
                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
            }
            break;
            case R.id.trigger_percertaintime:{
                activateTriggerType(PublicConsts.TRIGGER_TYPE_LOOP_BY_CERTAIN_TIME);
                BottomDialogForInterval dialog=new BottomDialogForInterval(this);
                dialog.setVariables((int)(interval/(1000*60*60*24)),
                        (int)((interval%(1000*60*60*24))/(1000*60*60)),
                        (int)((interval%(1000*60*60))/(1000*60)));
                dialog.setTitle(getResources().getString(R.string.dialog_setinterval_title));
                dialog.show();
                dialog.setOnDialogConfirmedListener(new BottomDialogForInterval.OnDialogConfirmedListener() {
                    @Override
                    public void onDialogConfirmed(long millis) {
                        interval=millis;
                        ((TextView)findViewById(R.id.trigger_percertaintime_value)).setText(getCertainLoopTimeDisplayValue(Triggers.this,millis));
                        activateTriggerType(PublicConsts.TRIGGER_TYPE_LOOP_BY_CERTAIN_TIME);
                    }
                });
            }
            break;
            case R.id.trigger_weekloop:{
                activateTriggerType(PublicConsts.TRIGGER_TYPE_LOOP_WEEK);
                LayoutInflater inflater=LayoutInflater.from(this);
                View dialogview=inflater.inflate(R.layout.layout_dialog_weekloop, null);
                final AlertDialog dialog_weekloop  = new AlertDialog.Builder(this)
                        .setIcon(R.drawable.icon_repeat_weekloop)
                        .setTitle(this.getResources().getString(R.string.dialog_weekloop_title))
                        .setView(dialogview)
                        .setPositiveButton(this.getResources().getString(R.string.dialog_button_positive), null)
                        .setCancelable(true)
                        .create();

                dialog_weekloop.show();
                final CheckBox cb_mon=dialogview.findViewById(R.id.layout_dialog_weekloop_cb_mon);
                final CheckBox cb_tue=dialogview.findViewById(R.id.layout_dialog_weekloop_cb_tue);
                final CheckBox cb_wed=dialogview.findViewById(R.id.layout_dialog_weekloop_cb_wed);
                final CheckBox cb_thu=dialogview.findViewById(R.id.layout_dialog_weekloop_cb_thu);
                final CheckBox cb_fri=dialogview.findViewById(R.id.layout_dialog_weekloop_cb_fri);
                final CheckBox cb_sat=dialogview.findViewById(R.id.layout_dialog_weekloop_cb_sat);
                final CheckBox cb_sun=dialogview.findViewById(R.id.layout_dialog_weekloop_cb_sun);

                cb_sun.setChecked(week_repeat[PublicConsts.WEEK_SUNDAY]);
                cb_mon.setChecked(week_repeat[PublicConsts.WEEK_MONDAY]);
                cb_tue.setChecked(week_repeat[PublicConsts.WEEK_TUESDAY]);
                cb_wed.setChecked(week_repeat[PublicConsts.WEEK_WEDNESDAY]);
                cb_thu.setChecked(week_repeat[PublicConsts.WEEK_THURSDAY]);
                cb_fri.setChecked(week_repeat[PublicConsts.WEEK_FRIDAY]);
                cb_sat.setChecked(week_repeat[PublicConsts.WEEK_SATURDAY]);

                dialog_weekloop.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // TODO Auto-generated method stub
                        week_repeat[PublicConsts.WEEK_MONDAY]=cb_mon.isChecked();
                        week_repeat[PublicConsts.WEEK_TUESDAY]=cb_tue.isChecked();
                        week_repeat[PublicConsts.WEEK_WEDNESDAY]=cb_wed.isChecked();
                        week_repeat[PublicConsts.WEEK_THURSDAY]=cb_thu.isChecked();
                        week_repeat[PublicConsts.WEEK_FRIDAY]=cb_fri.isChecked();
                        week_repeat[PublicConsts.WEEK_SATURDAY]=cb_sat.isChecked();
                        week_repeat[PublicConsts.WEEK_SUNDAY]=cb_sun.isChecked();
                        boolean allunchecked=true;
                        for (int i=0;i<7;i++){
                            if(week_repeat[i]){
                                allunchecked=false;
                                break;
                            }
                        }
                        ((TextView)findViewById(R.id.trigger_weekloop_value)).setText(getWeekLoopDisplayValue(Triggers.this,week_repeat));
                        if(allunchecked){
                            activateTriggerType(PublicConsts.TRIGGER_TYPE_SINGLE);
                            dialog_weekloop.cancel();
                            return;
                        }
                        dialog_weekloop.cancel();
                        activateTriggerType(PublicConsts.TRIGGER_TYPE_LOOP_WEEK);
                    }
                });

                dialog_weekloop.setOnCancelListener(new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                        // TODO Auto-generated method stub
                        boolean allunchecked=true;
                        for(int i=0;i<7;i++){
                            if(week_repeat[i]){//if(TaskGui.this.weekloop[i]){
                                allunchecked=false;
                                break;
                            }
                        }
                        if(allunchecked) activateTriggerType(PublicConsts.TRIGGER_TYPE_SINGLE);
                    }
                });
            }
            break;
            case R.id.trigger_battery_percentage:{
                if(trigger_type!=PublicConsts.TRIGGER_TYPE_BATTERY_MORE_THAN_PERCENTAGE&&trigger_type!=PublicConsts.TRIGGER_TYPE_BATTERY_LESS_THAN_PERCENTAGE){
                    activateTriggerType(PublicConsts.TRIGGER_TYPE_BATTERY_MORE_THAN_PERCENTAGE);
                }else{
                    activateTriggerType(trigger_type);
                }

                final BottomDialogForBattery dialog=new BottomDialogForBattery(this);
                dialog.textview_title.setText(getResources().getString(R.string.activity_taskgui_condition_battery_percentage_att));
                dialog.checkbox_enable.setVisibility(View.GONE);
                dialog.textview_second_description.setText("%");
                String[] percentage=new String[99];
                for(int i=0;i<percentage.length;i++) {
                    int a=i+1;
                    percentage[i]=String.valueOf(a);
                }
                String[] compares={this.getResources().getString(R.string.dialog_battery_compare_more_than),this.getResources().getString(R.string.dialog_battery_compare_less_than)};
                dialog.wheelview_first.setItems(Arrays.asList(compares));
                dialog.wheelview_second.setItems(Arrays.asList(percentage));
                dialog.wheelview_first.setSeletion(trigger_type==PublicConsts.TRIGGER_TYPE_BATTERY_MORE_THAN_PERCENTAGE?0:(trigger_type==PublicConsts.TRIGGER_TYPE_BATTERY_LESS_THAN_PERCENTAGE?1:0));
                dialog.wheelview_second.setSeletion(battery_percentage-1);
                dialog.textview_confirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int position=dialog.wheelview_first.getSeletedIndex();
                        int trigger_type=(position==0?PublicConsts.TRIGGER_TYPE_BATTERY_MORE_THAN_PERCENTAGE:(position==1?PublicConsts.TRIGGER_TYPE_BATTERY_LESS_THAN_PERCENTAGE:PublicConsts.TRIGGER_TYPE_BATTERY_MORE_THAN_PERCENTAGE));
                        battery_percentage=Integer.parseInt(dialog.wheelview_second.getSeletedItem());
                        dialog.cancel();
                        activateTriggerType(trigger_type);
                        ((TextView)findViewById(R.id.trigger_battery_percentage_value)).setText(getBatteryPercentageDisplayValue(Triggers.this,trigger_type,battery_percentage));
                    }
                });
                dialog.show();
            }
            break;
            case R.id.trigger_battery_temperature:{
                if(trigger_type!=PublicConsts.TRIGGER_TYPE_BATTERY_HIGHER_THAN_TEMPERATURE &&trigger_type!=PublicConsts.TRIGGER_TYPE_BATTERY_LOWER_THAN_TEMPERATURE){
                    activateTriggerType(PublicConsts.TRIGGER_TYPE_BATTERY_HIGHER_THAN_TEMPERATURE);
                }else{
                    activateTriggerType(trigger_type);
                }

                final BottomDialogForBattery dialog=new BottomDialogForBattery(this);
                dialog.textview_title.setText(getResources().getString(R.string.activity_taskgui_condition_battery_temperature_att));
                dialog.checkbox_enable.setVisibility(View.GONE);
                dialog.textview_second_description.setText("��");
                String[] temperature=new String[66];
                for(int i=0;i<temperature.length;i++) {
                    temperature[i]=String.valueOf(i);
                }
                String[] compares={this.getResources().getString(R.string.dialog_battery_compare_higher_than),this.getResources().getString(R.string.dialog_battery_compare_lower_than)};
                dialog.wheelview_first.setItems(Arrays.asList(compares));
                dialog.wheelview_second.setItems(Arrays.asList(temperature));
                dialog.wheelview_first.setSeletion(trigger_type==PublicConsts.TRIGGER_TYPE_BATTERY_HIGHER_THAN_TEMPERATURE?0:(trigger_type==PublicConsts.TRIGGER_TYPE_BATTERY_LOWER_THAN_TEMPERATURE?1:0));
                dialog.wheelview_second.setSeletion(battery_temperature);
                dialog.textview_confirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int position=dialog.wheelview_first.getSeletedIndex();
                        int trigger_type=(position==0?PublicConsts.TRIGGER_TYPE_BATTERY_HIGHER_THAN_TEMPERATURE:(position==1?PublicConsts.TRIGGER_TYPE_BATTERY_LOWER_THAN_TEMPERATURE:PublicConsts.TRIGGER_TYPE_BATTERY_HIGHER_THAN_TEMPERATURE));
                        battery_temperature=Integer.parseInt(dialog.wheelview_second.getSeletedItem());
                        dialog.cancel();
                        activateTriggerType(trigger_type);
                        ((TextView)findViewById(R.id.trigger_battery_temperature_value)).setText(getBatteryTemperatureDisplayValue(Triggers.this,trigger_type,battery_temperature));
                    }
                });
                dialog.show();
            }
            break;
            case R.id.trigger_received_broadcast:{
                activateTriggerType(PublicConsts.TRIGGER_TYPE_RECEIVED_BROADTCAST);
                View dialogView=LayoutInflater.from(this).inflate(R.layout.layout_dialog_with_listview,null);
                final BroadcastSelectionAdapter adapter=new BroadcastSelectionAdapter(broadcast_intent_action);
                ListView listView=dialogView.findViewById(R.id.layout_dialog_listview);
                listView.setDivider(null);
                (listView).setAdapter(adapter);
                final AlertDialog dialog=new AlertDialog.Builder(this)
                        .setTitle("IntentFilter")
                        .setView(dialogView)
                        .setPositiveButton(getResources().getString(R.string.dialog_button_positive),null)
                        .setNegativeButton(getResources().getString(R.string.dialog_button_negative), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        })
                        .show();
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        adapter.onItemClicked(i);
                    }
                });
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        broadcast_intent_action=adapter.getSelectedAction();
                        activateTriggerType(PublicConsts.TRIGGER_TYPE_RECEIVED_BROADTCAST);
                        ((TextView)findViewById(R.id.trigger_received_broadcast_value)).setText(getBroadcastDisplayValue(broadcast_intent_action));
                        dialog.cancel();
                    }
                });
            }
            break;
            case R.id.trigger_wifi_connected: {
                //trigger_type=PublicConsts.TRIGGER_TYPE_WIFI_CONNECTED;
                activateTriggerType(PublicConsts.TRIGGER_TYPE_WIFI_CONNECTED);
                dialog_wait=new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.dialog_wait_att))
                        .setView(LayoutInflater.from(this).inflate(R.layout.layout_dialog_wait,null))
                        .setCancelable(false)
                        .show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Message msg=new Message();
                        msg.what=MESSAGE_GET_SSID_COMPLETE;

                        WifiManager wifiManager=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);

                        if(wifiManager==null){
                            Log.e("Triggers","WifiManager is null !!");
                            msg.obj=new ArrayList<WifiConfiguration>();
                            sendMessage(msg);
                            return;
                        }

                        msg.obj=wifiManager.getConfiguredNetworks();

                        sendMessage(msg);
                    }
                }).start();

            }
            break;
            case R.id.trigger_wifi_disconnected:{
                trigger_type=PublicConsts.TRIGGER_TYPE_WIFI_DISCONNECTED;
                activateTriggerType(PublicConsts.TRIGGER_TYPE_WIFI_DISCONNECTED);
            }
            break;
        }
    }

    @Override
    public void onTimeChanged(TimePicker timePicker, int hour, int minute) {
        calendar.set(Calendar.HOUR_OF_DAY,hour);
        calendar.set(Calendar.MINUTE,minute);
        calendar.set(Calendar.SECOND,0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.triggers,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            default:break;
            case R.id.action_triggers_confirm:{
                String trigger_values []=null;
                switch(trigger_type){
                    default:break;
                    case PublicConsts.TRIGGER_TYPE_SINGLE:{
                        trigger_values=new String [1];
                        trigger_values[0]=String.valueOf(calendar.getTimeInMillis());
                    }
                    break;
                    case PublicConsts.TRIGGER_TYPE_LOOP_BY_CERTAIN_TIME:{
                        trigger_values=new String[1];
                        trigger_values[0]=String.valueOf(interval);
                    }
                    break;
                    case PublicConsts.TRIGGER_TYPE_LOOP_WEEK:{
                        trigger_values=new String[8];
                        trigger_values[0]=String.valueOf(calendar.getTimeInMillis());
                        for(int i=1;i<trigger_values.length;i++){
                            trigger_values[i]=week_repeat[i-1]?String.valueOf(1):String.valueOf(0);
                        }
                    }
                    break;
                    case PublicConsts.TRIGGER_TYPE_BATTERY_MORE_THAN_PERCENTAGE: case PublicConsts.TRIGGER_TYPE_BATTERY_LESS_THAN_PERCENTAGE:{
                        trigger_values=new String[1];
                        trigger_values[0]=String.valueOf(battery_percentage);
                    }
                    break;
                    case PublicConsts.TRIGGER_TYPE_BATTERY_HIGHER_THAN_TEMPERATURE: case PublicConsts.TRIGGER_TYPE_BATTERY_LOWER_THAN_TEMPERATURE:{
                        trigger_values=new String[1];
                        trigger_values[0]=String.valueOf(battery_temperature);
                    }
                    break;
                    case PublicConsts.TRIGGER_TYPE_RECEIVED_BROADTCAST:{
                        trigger_values=new String[1];
                        trigger_values[0]=String.valueOf(broadcast_intent_action);
                    }
                    break;
                    case PublicConsts.TRIGGER_TYPE_WIFI_CONNECTED: case PublicConsts.TRIGGER_TYPE_WIFI_DISCONNECTED:{
                        trigger_values=new String[1];
                        trigger_values[0]= wifi_ssidinfo;
                    }
                    break;
                }
                if(trigger_values==null) return false;
                Intent i=new Intent();
                i.putExtra(EXTRA_TRIGGER_TYPE,trigger_type);
                i.putExtra(EXTRA_TRIGGER_VALUES,trigger_values);
                setResult(RESULT_OK,i);
                finish();
            }
            break;
            case android.R.id.home:{
                checkAndExit();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            checkAndExit();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public static String getSingleTimeDisplayValue(@NonNull Context context, long millis){
       // TextView tv_condition_single_value=findViewById(R.id.trigger_single_value);
        Calendar calendar=Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        int month=calendar.get(Calendar.MONTH)+1;
        return context.getResources().getString(R.string.activity_taskgui_condition_single_value)+ ValueUtils.format(calendar.get(Calendar.YEAR))+"/"+ ValueUtils.format(month)+"/"+ ValueUtils.format(calendar.get(Calendar.DAY_OF_MONTH))+"("+ValueUtils.getDayOfWeek(calendar.getTimeInMillis())+")/"+ ValueUtils.format(calendar.get(Calendar.HOUR_OF_DAY))+":"+ ValueUtils.format(calendar.get(Calendar.MINUTE));
    }


    public static String getCertainLoopTimeDisplayValue(Context context, long loopmillis){
        //TextView tv_condition_percertaintime_value=findViewById(R.id.trigger_percertaintime_value);
        return context.getResources().getString(R.string.adapter_per)+ ValueUtils.getFormatTime(context,loopmillis)+context.getResources().getString(R.string.adapter_trigger);
    }

    public static String getWeekLoopDisplayValue(Context context,boolean week_repeat []){
        if(context==null||week_repeat==null||week_repeat.length!=7) return "";
        String tv_value="";
        //TextView tv_condition_weekloop_value=findViewById(R.id.layout_taskgui_area_condition_weekloop_value);
        if(week_repeat[1]) tv_value+=context.getResources().getString(R.string.monday)+" ";//if(this.weekloop[1]) tv_value+="��һ ";
        if(week_repeat[2]) tv_value+=context.getResources().getString(R.string.tuesday)+" ";
        if(week_repeat[3]) tv_value+=context.getResources().getString(R.string.wednesday)+" ";
        if(week_repeat[4]) tv_value+=context.getResources().getString(R.string.thursday)+" ";
        if(week_repeat[5]) tv_value+=context.getResources().getString(R.string.friday)+" ";
        if(week_repeat[6]) tv_value+=context.getResources().getString(R.string.saturday)+" ";
        if(week_repeat[0]) tv_value+=context.getResources().getString(R.string.sunday);

        boolean everyday=true;
        for(int i=0;i<7;i++){
            if(week_repeat[i]) {  //if(!this.weekloop[i]) {
                everyday=false;
                break;
            }
        }
        if(everyday) return context.getResources().getString(R.string.everyday);
        return  tv_value;
    }

    public static String getBatteryPercentageDisplayValue(Context context,int trigger_type,int percentage){
       // TextView tv_battery=findViewById(R.id.layout_taskgui_area_condition_battery_percentage_value);
        StringBuilder value=new StringBuilder("");
        if(trigger_type==PublicConsts.TRIGGER_TYPE_BATTERY_MORE_THAN_PERCENTAGE){
            value.append(context.getResources().getString(R.string.more_than)+" ");
            value.append(percentage+"%");
        }else if(trigger_type==PublicConsts.TRIGGER_TYPE_BATTERY_LESS_THAN_PERCENTAGE){
            value.append(context.getResources().getString(R.string.less_than)+" ");
            value.append(percentage+"%");
        }
         return value.toString();
    }

    public static String getBatteryTemperatureDisplayValue(Context context,int trigger_type,int battery_temperature){
        //TextView tv_battery=findViewById(R.id.layout_taskgui_area_condition_battery_temperature_value);
        StringBuilder value=new StringBuilder("");
        if(trigger_type==PublicConsts.TRIGGER_TYPE_BATTERY_LOWER_THAN_TEMPERATURE){
            value.append(context.getResources().getString(R.string.lower_than)+" ");
            value.append(battery_temperature+"��");
        }else if(trigger_type==PublicConsts.TRIGGER_TYPE_BATTERY_HIGHER_THAN_TEMPERATURE){
            value.append(context.getResources().getString(R.string.higher_than)+" ");
            value.append(battery_temperature+"��");
        }
        return value.toString();
    }

    public static String getBroadcastDisplayValue(String intent_action){
       // TextView tv_broadcast=findViewById(R.id.layout_taskgui_area_condition_received_broadcast_value);
       // if(trigger_type==PublicConsts.TRIGGER_TYPE_RECEIVED_BROADTCAST){
        //    tv_broadcast.setText(taskitem.selectedAction);
       // }
        return  intent_action;
    }

    public static String getWifiConnectionDisplayValue(Context context, String ssids){
        if(context==null||ssids==null) return "";
        if(ssids.length()==0) return context.getResources().getString(R.string.activity_trigger_wifi_no_ssid_assigned);
        //WifiManager wifiManager=(WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
       // if(wifiManager==null) return "";
        return ssids;
    }

    private void refreshTriggerDisplayValues(int type){
        TextView tv_condition_single_value=findViewById(R.id.trigger_single_value);
        TextView tv_condition_percertaintime_value=findViewById(R.id.trigger_percertaintime_value);
        TextView tv_condition_weekloop_value=findViewById(R.id.trigger_weekloop_value);
        TextView tv_condition_battery_percentage_value=findViewById(R.id.trigger_battery_percentage_value);
        TextView tv_condition_battery_temperature_value=findViewById(R.id.trigger_battery_temperature_value);
        TextView tv_wifi_connected=findViewById(R.id.trigger_wifi_connected_value);
        TextView tv_wifi_disconnected=findViewById(R.id.trigger_wifi_disconnected_value);
        TextView tv_widget_changed=findViewById(R.id.trigger_widget_changed_value);
        TextView tv_condition_broadcast=findViewById(R.id.trigger_received_broadcast_value);
        //TextView tv_wifi_connected

        String unchoose=this.getResources().getString(R.string.activity_taskgui_att_unchoose);

        ((RadioButton)findViewById(R.id.trigger_single_ra)).setChecked(type==PublicConsts.TRIGGER_TYPE_SINGLE);
        ((RadioButton)findViewById(R.id.trigger_percertaintime_ra)).setChecked(type==PublicConsts.TRIGGER_TYPE_LOOP_BY_CERTAIN_TIME);
        ((RadioButton)findViewById(R.id.trigger_weekloop_ra)).setChecked(type==PublicConsts.TRIGGER_TYPE_LOOP_WEEK);
        ((RadioButton)findViewById(R.id.trigger_battery_percentage_ra)).setChecked(type==PublicConsts.TRIGGER_TYPE_BATTERY_MORE_THAN_PERCENTAGE||type==PublicConsts.TRIGGER_TYPE_BATTERY_LESS_THAN_PERCENTAGE);
        ((RadioButton)findViewById(R.id.trigger_battery_temperature_ra)).setChecked(type==PublicConsts.TRIGGER_TYPE_BATTERY_HIGHER_THAN_TEMPERATURE||type==PublicConsts.TRIGGER_TYPE_BATTERY_LOWER_THAN_TEMPERATURE);
        //((RadioButton)findViewById(R.id.trigger_wifi_connected_ra)).setChecked(false);
        //((RadioButton)findViewById(R.id.trigger_wifi_disconnected_ra)).setChecked(false);
        //((RadioButton)findViewById(R.id.trigger_widget_changed_ra)).setChecked(false);
        ((RadioButton)findViewById(R.id.trigger_received_broadcast_ra)).setChecked(type==PublicConsts.TRIGGER_TYPE_RECEIVED_BROADTCAST);
        ((RadioButton)findViewById(R.id.trigger_wifi_connected_ra)).setChecked(type==PublicConsts.TRIGGER_TYPE_WIFI_CONNECTED);
        ((RadioButton)findViewById(R.id.trigger_wifi_disconnected_ra)).setChecked(type==PublicConsts.TRIGGER_TYPE_WIFI_DISCONNECTED);

        tv_condition_single_value.setText(unchoose);
        tv_condition_percertaintime_value.setText(unchoose);
        tv_condition_weekloop_value.setText(unchoose);
        tv_condition_battery_percentage_value.setText(unchoose);
        tv_condition_battery_temperature_value.setText(unchoose);
        tv_wifi_connected.setText(unchoose);
        tv_wifi_disconnected.setText(unchoose);
        tv_widget_changed.setText(unchoose);
        tv_condition_broadcast.setText(unchoose);
        tv_wifi_connected.setText(unchoose);
        tv_wifi_disconnected.setText(unchoose);
    }

    private void checkAndExit(){
        if(!toCheckString().equals(checkString)){
            long thisTime=System.currentTimeMillis();
            if(thisTime-first_clicked>1000){
                first_clicked=thisTime;
                Snackbar.make(findViewById(R.id.trigger_root),getResources().getString(R.string.snackbar_changes_not_saved_back),Snackbar.LENGTH_SHORT).show();
                return;
            }
            finish();
        }
        finish();
    }

    private class BroadcastSelectionAdapter extends BaseAdapter {
        List<String> intent_list=new ArrayList<>();
        int selectedPosition=0;
        private BroadcastSelectionAdapter(@Nullable String selectedAction){
            intent_list.add(Intent.ACTION_ANSWER);
            intent_list.add(Intent.ACTION_BATTERY_LOW);
            intent_list.add(Intent.ACTION_MEDIA_BAD_REMOVAL);
            intent_list.add(Intent.ACTION_PACKAGE_REMOVED);
            intent_list.add(Intent.ACTION_POWER_CONNECTED);
            intent_list.add(Intent.ACTION_POWER_DISCONNECTED);
            intent_list.add(WifiManager.WIFI_STATE_CHANGED_ACTION);
            intent_list.add(Intent.ACTION_PACKAGE_CHANGED);
            intent_list.add(Intent.ACTION_SCREEN_OFF);
            intent_list.add(Intent.ACTION_SCREEN_ON);
            intent_list.add(Intent.ACTION_PACKAGE_REMOVED);
            intent_list.add(Intent.ACTION_PACKAGE_ADDED);
            intent_list.add(ConnectivityManager.CONNECTIVITY_ACTION);
            if(selectedAction==null) return;
            for(int i=0;i<intent_list.size();i++){
                if(selectedAction.equals(intent_list.get(i))) {
                    selectedPosition=i;
                    break;
                }
            }
        }
        @Override
        public int getCount() {
            return intent_list.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if(view==null){
                view=LayoutInflater.from(Triggers.this).inflate(R.layout.item_broadcast_intent,viewGroup,false);
            }
            ((RadioButton)view.findViewById(R.id.item_broadcast_ra)).setText(intent_list.get(i));
            ((RadioButton)view.findViewById(R.id.item_broadcast_ra)).setChecked(i==selectedPosition);
            return view;
        }

        public void onItemClicked(int position){
            selectedPosition=position;
            notifyDataSetChanged();
        }

        public String getSelectedAction(){
            return intent_list.get(selectedPosition);
        }
    }

    private class WifiInfoListAdapter extends BaseAdapter{
        private List<WifiConfiguration> list;
        private boolean[] isSelected;
        public WifiInfoListAdapter(List<WifiConfiguration> list,String selected_ids) {
            if(list==null||selected_ids==null) return;
            this.list=list;
            isSelected=new boolean[list.size()];
            if(selected_ids.equals("")) return;
            try{
                String[] ids=selected_ids.split(PublicConsts.SPLIT_SEPARATOR_SECOND_LEVEL);
                for(String id:ids){
                    for(int i=0;i<list.size();i++){
                        if(list.get(i).networkId==Integer.parseInt(id)) {
                            isSelected[i]=true;
                            break;
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                LogUtil.putExceptionLog(Triggers.this,e);
            }

        }

        @Override
        public int getCount() {
            return list==null?0:list.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if(list==null) return null;
            if(view==null){
                view=LayoutInflater.from(Triggers.this).inflate(R.layout.item_wifiinfo,viewGroup,false);
            }
            ((TextView)view.findViewById(R.id.item_wifiinfo_ssid)).setText(list.get(i).SSID);
            ((CheckBox)view.findViewById(R.id.item_wifiinfo_cb)).setChecked(isSelected[i]);
            return view;
        }

        public void onItemClicked(int position){
            isSelected[position]=!isSelected[position];
            notifyDataSetChanged();
        }

        public String getSelectedIDs(){
            String ids="";
            for(int i=0;i<isSelected.length;i++){
                if(isSelected[i]) {
                    if(!ids.equals("")) ids+=PublicConsts.SEPARATOR_SECOND_LEVEL;
                    ids+=list.get(i).networkId;
                }
            }
            return ids;
        }
    }

}
