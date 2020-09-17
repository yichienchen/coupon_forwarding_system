package com.example.ad_forwarding_system;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

import java.util.Calendar;

import static com.example.ad_forwarding_system.MainActivity.TAG;
import static com.example.ad_forwarding_system.MainActivity.last_received_time;
import static com.example.ad_forwarding_system.MainActivity.list_device;
import static com.example.ad_forwarding_system.MainActivity.regroup_data;
import static com.example.ad_forwarding_system.Service_scan_function.add_database;
import static com.example.ad_forwarding_system.Service_scan_function.compare_database;
import static com.example.ad_forwarding_system.Service_scan_function.time_difference_;

//檢查是否有超過一天仍未完成的資料，並且刪除
public class JOBservice_event_check extends JobService {
    public JOBservice_event_check() {
        Log.e(TAG,"JOBservice_event_check");
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Calendar c = Calendar.getInstance();
        for(int i=0;i<list_device.size();i++){
            if(!regroup_data.contains("0")) {
                Log.e(TAG, i + " time_difference: " + time_difference_(last_received_time.get(i), c));
                if (time_difference_(last_received_time.get(i), c) > 10*1000 && !compare_database(list_device.get(i) + regroup_data.get(i))) {
                    add_database(list_device.get(i), regroup_data.get(i));
                    Log.e(TAG, "regroup_data: " + regroup_data.size());
                }
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
