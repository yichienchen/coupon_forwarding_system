package com.example.ad_forwarding_system;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;

import static com.example.ad_forwarding_system.MainActivity.TAG;
import static com.example.ad_forwarding_system.MainActivity.data_list;
import static com.example.ad_forwarding_system.MainActivity.last_received_time;
import static com.example.ad_forwarding_system.MainActivity.list_device;
import static com.example.ad_forwarding_system.MainActivity.num_list;
import static com.example.ad_forwarding_system.MainActivity.regroup_data;
import static com.example.ad_forwarding_system.Service_scan_function.time_difference_;

//檢查是否有已完成的資料
public class Jobservice_data_check extends JobService {
    static ArrayList<Integer> list = new ArrayList<>();
    public Jobservice_data_check() {
        Log.e(TAG,"Jobservice_data_check");
    }


    @Override
    public boolean onStartJob(JobParameters params) {
        Calendar c = Calendar.getInstance();
        for(int i=0;i<list_device.size();i++){
            Log.e(TAG, i + " time_difference: " + time_difference_(last_received_time.get(i), c));
            if (time_difference_(last_received_time.get(i), c) > 60*1000*60*24) {
                list.add(i);
            }

        }
        if(!list.isEmpty()){
            for(int i =0 ; i < list.size() ; i++){
                int indexx = list.get(i);
                list_device.remove(indexx);
                last_received_time.remove(indexx);
                data_list.remove(indexx);
                num_list.remove(indexx);
                regroup_data.remove(indexx);
            }
        }

        this.jobFinished(params,false);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
