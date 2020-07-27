package com.example.coupon_forwarding_system;

import android.app.Service;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import static com.example.coupon_forwarding_system.MainActivity.TAG;
import static com.example.coupon_forwarding_system.MainActivity.data_list;
import static com.example.coupon_forwarding_system.MainActivity.last_received_time;
import static com.example.coupon_forwarding_system.MainActivity.list_device;
import static com.example.coupon_forwarding_system.MainActivity.mBluetoothLeScanner;
import static com.example.coupon_forwarding_system.MainActivity.num_list;
import static com.example.coupon_forwarding_system.MainActivity.peripheralTextView;
import static com.example.coupon_forwarding_system.MainActivity.regroup_data;
import static com.example.coupon_forwarding_system.MainActivity.startScanningButton;
import static com.example.coupon_forwarding_system.MainActivity.stopScanningButton;


import static com.example.coupon_forwarding_system.Service_Adv.pdu_len;
import static com.example.coupon_forwarding_system.Service_scan_function.leScanCallback;



public class Service_Scan extends Service {

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Service_Scan() {
        Log.e(TAG,"Service_Scan start");
        startScanning();
        stopScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopScanning();
                stopSelf();
            }
        });
        startScanningButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            public void onClick(View v) {
                startScanning();
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void startScanning() {
        Log.e(TAG,"start scanning");


        list_device.clear();
        num_list.clear();
        data_list.clear();
        last_received_time.clear();
        regroup_data.clear();


        peripheralTextView.setText(null);
        startScanningButton.setVisibility(View.INVISIBLE);
        stopScanningButton.setVisibility(View.VISIBLE);

        byte[] data_all = new byte[pdu_len+6];


        byte[] data_mask = new byte[] {0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
                                       0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
                                       0x00,0x00,0x00,0x00,0x00,0x00,0x00};



        ScanFilter Mau_filter_extended = new ScanFilter.Builder().setManufacturerData(0xffff,data_all,data_all).build();
        ScanFilter Mau_filter_legacy = new ScanFilter.Builder().setManufacturerData(0xffff,data_mask,data_mask).build();

//        Log.e(TAG,"data_mask: "+byte2HexStr(data_mask));

        ArrayList<ScanFilter> filters = new ArrayList<>();
//        filters.add(Mau_filter_extended);
        filters.add(Mau_filter_legacy);


        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .setLegacy(false)
//                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)  //Fails to start power optimized scan as this feature is not supported
//                .setMatchMode(ScanSettings.)
//                .setNumOfMatches(1)
//                .setReportDelay()
                .build();
//        btScanner.flushPendingScanResults(leScanCallback);
        mBluetoothLeScanner.startScan(filters, settings, leScanCallback);
    }

    public void stopScanning() {
        Log.e(TAG,"stopping scanning");

        Log.e(TAG,"list_device: "+list_device);
        for(int i =0 ; i< list_device.size() ; i++){
//            Log.e(TAG,"time_interval: "+time_interval.get(i)+rssi_level_1.get(i)+","+rssi_level_2.get(i)+","+rssi_level_3.get(i));
        }

        peripheralTextView.append("Stopped Scanning");
        startScanningButton.setVisibility(View.VISIBLE);
        stopScanningButton.setVisibility(View.INVISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                mBluetoothLeScanner.stopScan(leScanCallback);
            }
        });
    }
}
