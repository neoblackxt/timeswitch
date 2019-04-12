package com.github.ghmxr.timeswitch.adapters;

import android.content.Context;
import android.graphics.Color;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.ghmxr.timeswitch.R;
import com.github.ghmxr.timeswitch.activities.Main;
import com.github.ghmxr.timeswitch.data.v2.ActionConsts;
import com.github.ghmxr.timeswitch.data.v2.ExceptionConsts;
import com.github.ghmxr.timeswitch.TaskItem;
import com.github.ghmxr.timeswitch.data.v2.TriggerTypeConsts;
import com.github.ghmxr.timeswitch.services.TimeSwitchService;
import com.github.ghmxr.timeswitch.ui.ActionDisplayValue;
import com.github.ghmxr.timeswitch.utils.ValueUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mxremail@qq.com  https://github.com/ghmxr/timeswitch
 */
public class MainListAdapter extends BaseAdapter {

    private Context context;
    //private LayoutInflater inflater;
    private List<TaskItem> list;
    private SwitchChangedListener listener_switch;
    private FoldingStatusChangedListener listener_folding;
    private boolean isMultiSelectMode=false;
    private boolean [] isSelected;
    private int taskCount;
    //private List<TaskItem> list_refreshes=new ArrayList<>();
    private Map<Integer,TextView> list_refresh_textviews=new HashMap<>();
    //private SparseArray<TextView> list_refresh_textviews=new SparseArray<>();

    public MainListAdapter (Context context, @NonNull List<TaskItem> list){
        this.context=context;
        //inflater=LayoutInflater.from(context);
        this.list= list;
        taskCount=list.size();
        isSelected=new boolean[taskCount];
        /*list_refreshes.clear();
        for(TaskItem i:list){
            if(i.trigger_type==PublicConsts.TRIGGER_TYPE_LOOP_BY_CERTAIN_TIME) list_refreshes.add(i);
        }*/
    }

    @Override
    public int getCount() {
        return taskCount+2;
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
    public View getView(final int i, View view, ViewGroup viewGroup) {
        //Log.d("MainListAdapter","getView called and location is "+i);
        final ViewHolder holder;
        if(view==null){
            view=LayoutInflater.from(context).inflate(R.layout.item_task,viewGroup,false);
            holder=new ViewHolder();
            holder.title=view.findViewById(R.id.item_task_title);
            holder.title_arrow=view.findViewById(R.id.item_task_title_arrow);
            holder.switch_enabled=view.findViewById(R.id.item_task_switch);
            holder.cb=view.findViewById(R.id.item_task_checkbox);
            holder.task_area=view.findViewById(R.id.item_task_info);
            holder.tv_name=view.findViewById(R.id.item_task_name);
            holder.img_trigger=view.findViewById(R.id.item_task_trigger_icon);
            holder.tv_trigger=view.findViewById(R.id.item_task_trigger_value);
            holder.tv_exception=view.findViewById(R.id.item_task_exception);
            holder.tv_action=view.findViewById(R.id.item_task_action);
            holder.tv_addition=view.findViewById(R.id.item_task_addition);
            holder.addition=view.findViewById(R.id.item_task_addition_area);
            view.setTag(holder);
        }else {
            holder=(ViewHolder) view.getTag();
        }

        if(i>=taskCount){
            view.setVisibility(View.INVISIBLE);
            return view;
        }else {
            view.setVisibility(View.VISIBLE);
        }

        TaskItem item=null;
        try{
            item=list.get(i);
        }catch (Exception e){
            e.printStackTrace();
        }

        if(item==null){
            view.setVisibility(View.GONE);
            return view;
        }

        try{
            try{
                Collection<TextView> values=list_refresh_textviews.values();
                values.remove(holder.tv_trigger);
            }catch (Exception e){e.printStackTrace();}
            if(item.trigger_type== TriggerTypeConsts.TRIGGER_TYPE_LOOP_BY_CERTAIN_TIME){
                list_refresh_textviews.put(i,holder.tv_trigger);
            }
        }catch (Exception e){e.printStackTrace();}

        try {
            holder.tv_name.setText(item.name);
            int color_value = Color.parseColor(item.addition_title_color);
            holder.title.setBackgroundColor(color_value);
            holder.task_area.setVisibility(item.addition_isFolded?View.GONE:View.VISIBLE);
            holder.title_arrow.setRotation(item.addition_isFolded?0:90);
            if(ValueUtils.isHighLightRGB(color_value)){
                holder.tv_name.setTextColor(context.getResources().getColor(R.color.color_black));
                holder.title_arrow.setImageResource(R.drawable.arrow_right_item_folding_black);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        holder.title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(holder.task_area.getVisibility()!=View.VISIBLE){
                    holder.task_area.setVisibility(View.VISIBLE);
                    holder.title_arrow.setRotation(90);
                    if(listener_folding!=null)listener_folding.onFoldingStatusChanged(i,false);
                }else{
                    holder.task_area.setVisibility(View.GONE);
                    holder.title_arrow.setRotation(0);
                    if(listener_folding!=null) listener_folding.onFoldingStatusChanged(i,true);
                }
            }
        });

        try{
            holder.img_trigger.setImageResource(item.display_trigger_icon_res);
            holder.tv_trigger.setText(item.display_trigger);
            holder.tv_exception.setText(item.display_exception);
            holder.tv_action.setText(item.display_actions);
            holder.tv_addition.setText(item.display_additions);
            if(item.autodelete||item.autoclose){
                holder.addition.setVisibility(View.VISIBLE);
                if(!item.autodelete&&item.trigger_type== TriggerTypeConsts.TRIGGER_TYPE_SINGLE) holder.addition.setVisibility(View.GONE);
            }else holder.addition.setVisibility(View.GONE);
        }catch (Exception e){e.printStackTrace();}

        CheckBox cb = holder.cb;
        SwitchCompat switchCompat = holder.switch_enabled;
        if (isMultiSelectMode) {
            switchCompat.setVisibility(View.GONE);
            cb.setVisibility(View.VISIBLE);
            cb.setOnCheckedChangeListener(null);
            cb.setChecked(isSelected[i]);
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    isSelected[i] = isChecked;
                }
            });
            holder.title.setOnLongClickListener(null);
        } else {
            switchCompat.setVisibility(View.VISIBLE);
            cb.setVisibility(View.GONE);
            switchCompat.setOnCheckedChangeListener(null);
            switchCompat.setChecked(item.isenabled);
            switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (listener_switch != null) listener_switch.onCheckedChanged(i, b);
                }
            });
            holder.title.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Message msg = new Message();
                    msg.what = Main.MESSAGE_OPEN_MULTI_SELECT_MODE;
                    msg.obj = i;
                    Main.sendMessage(msg);
                    return true;
                }
            });
        }

        return view;
    }

    public void onDataSetChanged(@NonNull List<TaskItem> list){
        this.list=list;
        taskCount=list.size();
        this.isSelected=new boolean[taskCount];
        /*list_refreshes.clear();
        for(TaskItem i:list){
            if(i.trigger_type==PublicConsts.TRIGGER_TYPE_LOOP_BY_CERTAIN_TIME) list_refreshes.add(i);
        }*/
        try{list_refresh_textviews.clear();}catch (Exception e){e.printStackTrace();}
        this.notifyDataSetChanged();
    }

    public void refreshAllCertainTimeTaskItems(){
        try{
            /*for(TaskItem item:list_refreshes){
                try{
                    long remaining=item.getNextTriggeringTime()-System.currentTimeMillis();
                    if(remaining<=0) remaining=0;
                    int day=(int)(remaining/(1000*60*60*24));
                    int hour=(int)((remaining%(1000*60*60*24))/(1000*60*60));
                    int minute=(int)((remaining%(1000*60*60))/(1000*60));
                    int second=(int)((remaining%(1000*60))/1000);
                    String display;
                    if(day>0){
                        display=day+":"+ValueUtils.format(hour)+":"+ValueUtils.format(minute)+":"+ValueUtils.format(second);
                    }else if(hour>0){
                        display=ValueUtils.format(hour)+":"+ValueUtils.format(minute)+":"+ValueUtils.format(second);
                    }else if(minute>0){
                        display=ValueUtils.format(minute)+":"+ValueUtils.format(second);
                    }else{
                        display=ValueUtils.format(second)+"s";
                    }
                    if(item.isenabled) {
                        item.display_trigger=display;
                    }
                    else item.display_trigger="Off";
                    if(list_refresh_textviews.get(item.id)!=null) list_refresh_textviews.get(item.id).setText(item.display_trigger);
                }catch (Exception e){e.printStackTrace();}
            }*/
            Object[]ids= list_refresh_textviews.keySet().toArray();
            for(Object i:ids){
                list_refresh_textviews.get(i).setText(TimeSwitchService.list.get((Integer) i).display_trigger);
            }
        }catch (IndexOutOfBoundsException ie){

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void onMultiSelectModeItemClicked(int position){
        if(position>=isSelected.length||position<0) return;
        isSelected[position]=!isSelected[position];
        this.notifyDataSetChanged();
    }


    public boolean[] getIsSelected(){
        return this.isSelected;
    }

    public void setOnSwitchChangedListener(SwitchChangedListener listener){
        this.listener_switch =listener;
    }

    public void setOnFoldingStatusChangedListener(FoldingStatusChangedListener listener){
        this.listener_folding=listener;
    }

    public void openMultiSelecteMode(int longclickposition){
        this.isMultiSelectMode=true;
        isSelected=new boolean[list.size()];
        isSelected[longclickposition]=true;
        this.notifyDataSetChanged();
    }

    public void closeMultiSelectMode(){
        this.isMultiSelectMode=false;
        this.notifyDataSetChanged();
    }

    public void selectAll(){
        for(int i=0;i<isSelected.length;i++){
            isSelected[i]=true;
        }
        this.notifyDataSetChanged();
    }

    public void deselectAll(){
        for(int i=0;i<isSelected.length;i++){
            isSelected[i]=false;
        }
        this.notifyDataSetChanged();
    }

    public static String getExceptionValue(Context context,TaskItem item){
        try{
            StringBuilder builder=new StringBuilder("");
            String [] exceptions=item.exceptions;
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_LOCKEDSCREEN])==1){
                builder.append(context.getResources().getString(R.string.activity_taskgui_exception_screen_locked));
            }
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_UNLOCKEDSCREEN])==1){
                builder.append(context.getResources().getString(R.string.activity_taskgui_exception_screen_unlocked));
            }
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_WIFI_ENABLED])==1){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.activity_taskgui_exception_wifi_enabled));
            }
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_WIFI_DISABLED])==1){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.activity_taskgui_exception_wifi_disabled));
            }

            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_BLUETOOTH_ENABLED])==1){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.activity_taskgui_exception_bluetooth_enabled));
            }
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_BLUETOOTH_DISABLED])==1){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.activity_taskgui_exception_bluetooth_disabled));
            }
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_RING_VIBRATE])==1){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.activity_taskgui_exception_ring_vibrate));
            }
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_RING_OFF])==1){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.activity_taskgui_exception_ring_off));
            }
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_RING_NORMAL])==1){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.activity_taskgui_exception_ring_normal));
            }
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_NET_ENABLED])==1){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.activity_taskgui_exception_net_enabled));
            }
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_NET_DISABLED])==1){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.activity_taskgui_exception_net_disabled));
            }
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_GPS_ENABLED])==1){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.activity_taskgui_exception_gps_on));
            }
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_GPS_DISABLED])==1){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.activity_taskgui_exception_gps_off));
            }
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_AIRPLANE_MODE_ENABLED])==1){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.activity_taskgui_exception_airplanemode_on));
            }
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_AIRPLANE_MODE_DISABLED])==1){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.activity_taskgui_exception_airplanemode_off));
            }
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_HEADSET_STATUS])>0){
                if(builder.toString().length()>0) builder.append(",");
                if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_HEADSET_STATUS])== ExceptionConsts.EXCEPTION_HEADSET_PLUG_OUT){
                    builder.append(context.getResources().getString(R.string.activity_taskgui_exception_headset));
                    builder.append(context.getResources().getString(R.string.activity_taskgui_exception_headset_out));
                }
                if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_HEADSET_STATUS])== ExceptionConsts.EXCEPTION_HEADSET_PLUG_IN){
                    builder.append(context.getResources().getString(R.string.activity_taskgui_exception_headset));
                    builder.append(context.getResources().getString(R.string.activity_taskgui_exception_headset_in));
                }
            }

            String day_of_week="";
            String day_of_week_values="";
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_MONDAY])==1) {
                day_of_week_values+=context.getResources().getString(R.string.monday);
            }
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_TUESDAY])==1){
                if(day_of_week_values.trim().length()>0)day_of_week_values+=" ";
                day_of_week_values+=context.getResources().getString(R.string.tuesday);
            }
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_WEDNESDAY])==1){
                if(day_of_week_values.trim().length()>0)day_of_week_values+=" ";
                day_of_week_values+=context.getResources().getString(R.string.wednesday);
            }
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_THURSDAY])==1){
                if(day_of_week_values.trim().length()>0)day_of_week_values+=" ";
                day_of_week_values+=context.getResources().getString(R.string.thursday);
            }
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_FRIDAY])==1){
                if(day_of_week_values.trim().length()>0)day_of_week_values+=" ";
                day_of_week_values+=context.getResources().getString(R.string.friday);
            }
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_SATURDAY])==1){
                if(day_of_week_values.trim().length()>0)day_of_week_values+=" ";
                day_of_week_values+=context.getResources().getString(R.string.saturday);
            }
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_SUNDAY])==1){
                if(day_of_week_values.trim().length()>0)day_of_week_values+=" ";
                day_of_week_values+=context.getResources().getString(R.string.sunday);
            }
            if(day_of_week_values.trim().length()>0){
                day_of_week+=(context.getResources().getString(R.string.adapter_exception_day_of_week_head));
                day_of_week+=day_of_week_values;
            }
            if(builder.toString().length()>0&&day_of_week.length()>0) builder.append(",");
            builder.append(day_of_week);
            /*if(Integer.parseInt(exceptions[PublicConsts.EXCEPTION_MONDAY])==1){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.monday));
            }
            if(Integer.parseInt(exceptions[PublicConsts.EXCEPTION_TUESDAY])==1){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.tuesday));
            }
            if(Integer.parseInt(exceptions[PublicConsts.EXCEPTION_WEDNESDAY])==1){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.wednesday));
            }
            if(Integer.parseInt(exceptions[PublicConsts.EXCEPTION_THURSDAY])==1){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.thursday));
            }
            if(Integer.parseInt(exceptions[PublicConsts.EXCEPTION_FRIDAY])==1){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.friday));
            }
            if(Integer.parseInt(exceptions[PublicConsts.EXCEPTION_SATURDAY])==1){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.saturday));
            }
            if(Integer.parseInt(exceptions[PublicConsts.EXCEPTION_SUNDAY])==1){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.sunday));
            }*/


            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_START_TIME])>=0&&Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_END_TIME])>=0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.log_exceptions_period));
                builder.append(ValueUtils.timePeriodFormatValue(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_START_TIME]),Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_END_TIME])));
            }
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_BATTERY_LESS_THAN_PERCENTAGE])>=0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.log_exceptions_battery_percentage_less_than));
                builder.append(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_BATTERY_LESS_THAN_PERCENTAGE]));
                builder.append("%");
            }
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_BATTERY_MORE_THAN_PERCENTAGE])>=0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.log_exceptions_battery_percentage_more_than));
                builder.append(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_BATTERY_MORE_THAN_PERCENTAGE]));
                builder.append("%");
            }
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_BATTERY_HIGHER_THAN_TEMPERATURE])>=0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.log_exceptions_battery_temperature_higher_than));
                builder.append(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_BATTERY_HIGHER_THAN_TEMPERATURE]));
                builder.append(context.getResources().getString(R.string.degree_celsius));
            }
            if(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_BATTERY_LOWER_THAN_TEMPERATURE])>=0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.log_exceptions_battery_temperature_lower_than));
                builder.append(Integer.parseInt(exceptions[ExceptionConsts.EXCEPTION_BATTERY_LOWER_THAN_TEMPERATURE]));
                builder.append(context.getResources().getString(R.string.degree_celsius));
            }

            String returnValue=builder.toString();
            if(returnValue.equals("")) return context.getResources().getString(R.string.word_nothing);

            String exception_connector;
            if(Integer.parseInt(item.addition_exception_connector)==0) exception_connector=context.getResources().getString(R.string.adapter_exception_type_and);
            else exception_connector=context.getResources().getString(R.string.adapter_exception_type_or);

            return exception_connector+returnValue;
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    public static String getActionValue(Context context,TaskItem item){
        try{
            String[]actions=item.actions;
            StringBuilder builder=new StringBuilder("");

            /*int action_wifi=Integer.parseInt(actions[PublicConsts.ACTION_WIFI_LOCALE]);
            if(action_wifi>=0){
                if(action_wifi==0) builder.append(context.getResources().getString(R.string.action_wifi_open));
                if(action_wifi==1) builder.append(context.getResources().getString(R.string.action_wifi_close));
            }*/
            builder.append(ActionDisplayValue.ActionDisplayValueOfAdapter.getWifiDisplayValue(context,actions[ActionConsts.ActionFirstLevelLocaleConsts.ACTION_WIFI_LOCALE]));

            /*int action_bluetooth=Integer.parseInt(actions[PublicConsts.ACTION_BLUETOOTH_LOCALE]);
            if(action_bluetooth>=0){
                if(builder.toString().length()>0)builder.append(",");
                if(action_bluetooth==0) builder.append(context.getResources().getString(R.string.action_bluetooth_close));
                if(action_bluetooth==1) builder.append(context.getResources().getString(R.string.action_bluetooth_open));
            }*/
            String value_bluetooth=ActionDisplayValue.ActionDisplayValueOfAdapter.getBluetoothDisplayValue(context,actions[ActionConsts.ActionFirstLevelLocaleConsts.ACTION_BLUETOOTH_LOCALE]);
            if(value_bluetooth.length()>0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(value_bluetooth);
            }


            /*int action_ring=Integer.parseInt(actions[PublicConsts.ACTION_RING_MODE_LOCALE]);
            if(action_ring>=0){
                if(builder.toString().length()>0) builder.append(",");
                if(action_ring==PublicConsts.ACTION_RING_NORMAL) builder.append(context.getResources().getString(R.string.action_ring_mode_normal));
                if(action_ring==PublicConsts.ACTION_RING_VIBRATE)builder.append(context.getResources().getString(R.string.action_ring_mode_vibrate));
                if(action_ring==PublicConsts.ACTION_RING_OFF) builder.append(context.getResources().getString(R.string.action_ring_mode_off));
            }*/
            String value_ringmode=ActionDisplayValue.ActionDisplayValueOfAdapter.getRingModeDisplayValue(context,actions[ActionConsts.ActionFirstLevelLocaleConsts.ACTION_RING_MODE_LOCALE]);
            if(value_ringmode.length()>0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(value_ringmode);
            }


            /*String [] action_volumes=actions[PublicConsts.ACTION_RING_VOLUME_LOCALE].split(PublicConsts.SEPARATOR_SECOND_LEVEL);
            AudioManager audioManager=(AudioManager) context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            int volume_call=Integer.parseInt(action_volumes[PublicConsts.VOLUME_RING_LOCALE]);
            int volume_notification=Integer.parseInt(action_volumes[PublicConsts.VOLUME_NOTIFICATION_LOCALE]);
            int volume_media=Integer.parseInt(action_volumes[PublicConsts.VOLUME_MEDIA_LOCALE]);
            int volume_alarm=Integer.parseInt(action_volumes[PublicConsts.VOLUME_ALARM_LOCALE]);
            if(volume_call>=0) {
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.activity_taskgui_actions_ring_volume_ring));
                builder.append((int)(((double)volume_call/audioManager.getStreamMaxVolume(AudioManager.STREAM_RING))*100));
                builder.append(context.getResources().getString(R.string.percentage));
            }
            if(volume_media>=0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.activity_taskgui_actions_ring_volume_media));
                builder.append((int)(((double)volume_media/audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC))*100));
                builder.append(context.getResources().getString(R.string.percentage));
            }
            if(volume_notification>=0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.activity_taskgui_actions_ring_volume_notification));
                builder.append((int)(((double)volume_notification/audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION))*100));
                builder.append(context.getResources().getString(R.string.percentage));
            }
            if(volume_alarm>=0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.activity_taskgui_actions_ring_volume_alarm));
                builder.append((int)(((double)volume_alarm/audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM))*100));
                builder.append(context.getResources().getString(R.string.percentage));
            }*/
            String value_volume=ActionDisplayValue.ActionDisplayValueOfAdapter.getRingVolumeDisplayValue(context,actions[ActionConsts.ActionFirstLevelLocaleConsts.ACTION_RING_VOLUME_LOCALE]);
            if(value_volume.length()>0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(value_volume);
            }


            /*String[] action_ring_selections=actions[PublicConsts.ACTION_RING_SELECTION_LOCALE].split(PublicConsts.SEPARATOR_SECOND_LEVEL);
            int action_ring_selection_call=Integer.parseInt(action_ring_selections[PublicConsts.RING_SELECTION_CALL_TYPE_LOCALE]);
            int action_ring_selection_notification=Integer.parseInt(action_ring_selections[PublicConsts.RING_SELECTION_NOTIFICATION_TYPE_LOCALE]);
            //RingtoneManager ringtoneManager=RingtoneManager.
            if(action_ring_selection_notification>=0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.action_set_ringtone_notification));
                builder.append(RingtoneManager.getRingtone(context, Uri.parse(item.uri_ring_notification)).getTitle(context));
            }
            if(action_ring_selection_call>=0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.action_set_ringtone_phone));
                builder.append(RingtoneManager.getRingtone(context, Uri.parse(item.uri_ring_call)).getTitle(context));
            }*/
            String value_ringselect=ActionDisplayValue.ActionDisplayValueOfAdapter.getRingSelectionDisplayValue(context,item);
            if(value_ringselect.length()>0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(value_ringselect);
            }

            /*String [] action_vibrate_values=actions[PublicConsts.ACTION_VIBRATE_LOCALE].split(PublicConsts.SEPARATOR_SECOND_LEVEL);
            int vibrate_frequency=Integer.parseInt(action_vibrate_values[PublicConsts.VIBRATE_FREQUENCY_LOCALE]);
            if(vibrate_frequency>0){
                int vibrate_duration=Integer.parseInt(action_vibrate_values[PublicConsts.VIBRATE_DURATION_LOCALE]);
                int vibrate_interval=Integer.parseInt(action_vibrate_values[PublicConsts.VIBRATE_INTERVAL_LOCALE]);
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.adapter_action_vibrate));
                builder.append(vibrate_frequency);
                builder.append(context.getResources().getString(R.string.dialog_actions_vibrate_frequency_measure));
                builder.append(",");
                builder.append(vibrate_duration);
                builder.append(context.getResources().getString(R.string.dialog_actions_vibrate_duration_measure));
                builder.append(",");
                builder.append(vibrate_interval);
                builder.append(context.getResources().getString(R.string.dialog_actions_vibrate_interval_measure));
            }*/
            String value_vibrate=ActionDisplayValue.ActionDisplayValueOfAdapter.getVibrateDisplayValue(context,actions[ActionConsts.ActionFirstLevelLocaleConsts.ACTION_VIBRATE_LOCALE]);
            if(value_vibrate.length()>0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(value_vibrate);
            }

            String values_brightness= ActionDisplayValue.ActionDisplayValueOfAdapter.getBrightnessDisplayValue(context,actions[ActionConsts.ActionFirstLevelLocaleConsts.ACTION_BRIGHTNESS_LOCALE]);
            if(values_brightness.length()>0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(values_brightness);
            }

            /*String  action_set_wallpaper_values=actions[PublicConsts.ACTION_SET_WALL_PAPER_LOCALE];
            int action_wallpaper=Integer.parseInt(action_set_wallpaper_values);
            if(action_wallpaper>=0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.action_set_wallpaper));
                builder.append(ValueUtils.getRealPathFromUri(context,Uri.parse(item.uri_wallpaper_desktop)));
            }*/
            String value_wallpaper=ActionDisplayValue.ActionDisplayValueOfAdapter.getWallpaperDisplayValue(context,item);
            if(value_wallpaper.length()>0){
                if(builder.toString().length()>0)builder.append(",");
                builder.append(value_wallpaper);
            }

            /*String action_sms_values[]=actions[PublicConsts.ACTION_SMS_LOCALE].split(PublicConsts.SEPARATOR_SECOND_LEVEL);
            if(Integer.parseInt(action_sms_values[PublicConsts.SMS_ENABLED_LOCALE])>=0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(context.getResources().getString(R.string.adapter_action_sms));
                if(Build.VERSION.SDK_INT>=22){
                    SubscriptionInfo subinfo= SubscriptionManager.from(context).getActiveSubscriptionInfo(Integer.parseInt(action_sms_values[PublicConsts.SMS_SUBINFO_LOCALE]));
                    builder.append("(");
                    builder.append(subinfo.getDisplayName());
                    builder.append(":");
                    builder.append(subinfo.getNumber());
                    builder.append(")");
                }
                builder.append(context.getResources().getString(R.string.adapter_action_sms_receivers));
                builder.append(item.sms_address);
                builder.append(",");
                builder.append(context.getResources().getString(R.string.adapter_action_sms_message));
                builder.append(item.sms_message);
            }*/
            String value_sms=ActionDisplayValue.ActionDisplayValueOfAdapter.getSMSDisplayValue(context,item);
            if(value_sms.length()>0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(value_sms);
            }

            String value_toast=ActionDisplayValue.ActionDisplayValueOfAdapter.getToastDisplayValue(context,item);
            if(value_toast.length()>0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(value_toast);
            }

            String value_notification= ActionDisplayValue.ActionDisplayValueOfAdapter.getNotificationDisplayValue(context,item);
            if(value_notification.length()>0) {
                if(builder.toString().length()>0) builder.append(",");
                builder.append(value_notification);
            }

            String value_gps= ActionDisplayValue.ActionDisplayValueOfAdapter.getGpsDisplayValue(context,actions[ActionConsts.ActionFirstLevelLocaleConsts.ACTION_GPS_LOCALE]);
            if(value_gps.length()>0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(value_gps);
            }

            String value_net=ActionDisplayValue.ActionDisplayValueOfAdapter.getNetDisplayValue(context,actions[ActionConsts.ActionFirstLevelLocaleConsts.ACTION_NET_LOCALE]);
            if(value_net.length()>0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(value_net);
            }

            String value_airplanemode= ActionDisplayValue.ActionDisplayValueOfAdapter.getAirplaneModeDisplayValue(context,actions[ActionConsts.ActionFirstLevelLocaleConsts.ACTION_AIRPLANE_MODE_LOCALE]);
            if(value_airplanemode.length()>0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(value_airplanemode);
            }

            String value_device= ActionDisplayValue.ActionDisplayValueOfAdapter.getDeviceControlDisplayValue(context,actions[ActionConsts.ActionFirstLevelLocaleConsts.ACTION_DEVICECONTROL_LOCALE]);
            if(value_device.length()>0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(value_device);
            }

            String value_task_enable= ActionDisplayValue.ActionDisplayValueOfAdapter.getEnableTasksDisplayValue(context,actions[ActionConsts.ActionFirstLevelLocaleConsts.ACTION_ENABLE_TASKS_LOCALE]);
            if(value_task_enable.length()>0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(value_task_enable);
            }

            String value_task_disable= ActionDisplayValue.ActionDisplayValueOfAdapter.getDisableTasksDisplayValue(context,actions[ActionConsts.ActionFirstLevelLocaleConsts.ACTION_DISABLE_TASKS_LOCALE]);
            if(value_task_disable.length()>0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(value_task_disable);
            }

            String value_app_open= ActionDisplayValue.ActionDisplayValueOfAdapter.getAppLaunchDisplayValue(context,actions[ActionConsts.ActionFirstLevelLocaleConsts.ACTION_LAUNCH_APP_PACKAGES]);
            if(value_app_open.length()>0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(value_app_open);
            }

            String value_app_close= ActionDisplayValue.ActionDisplayValueOfAdapter.getAppCloseDisplayValue(context,actions[ActionConsts.ActionFirstLevelLocaleConsts.ACTION_STOP_APP_PACKAGES]);
            if(value_app_close.length()>0){
                if(builder.toString().length()>0) builder.append(",");
                builder.append(value_app_close);
            }

            String returnValue=builder.toString();
            if(returnValue.trim().equals("")) return context.getResources().getString(R.string.word_nothing);
            return returnValue;
        }catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }

    public static String getAdditionValue(Context context,TaskItem item){
        try{
            if(item.autodelete) return context.getResources().getString(R.string.activity_taskgui_additional_autodelete_cb);
            if(item.autoclose) return context.getResources().getString(R.string.activity_taskgui_additional_autoclose_cb);
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    private static class ViewHolder{
        View title;
        ImageView title_arrow;
        SwitchCompat switch_enabled;
        View task_area;
        CheckBox cb;
        TextView tv_name;
        ImageView img_trigger;
        TextView tv_trigger,tv_exception,tv_action,tv_addition;
        View addition;
    }

    public interface SwitchChangedListener{
        void onCheckedChanged(int position,boolean b);
    }

    public interface FoldingStatusChangedListener{
        void onFoldingStatusChanged(int position,boolean isFolded);
    }

}
