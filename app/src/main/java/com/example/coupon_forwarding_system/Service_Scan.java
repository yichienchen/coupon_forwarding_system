package com.example.coupon_forwarding_system;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import static com.example.coupon_forwarding_system.Function.byte2HexStr;
import static com.example.coupon_forwarding_system.Function.hexToAscii;
import static com.example.coupon_forwarding_system.MainActivity.DH;
import static com.example.coupon_forwarding_system.MainActivity.TAG;
import static com.example.coupon_forwarding_system.MainActivity.data_list;
import static com.example.coupon_forwarding_system.MainActivity.last_received_time;
import static com.example.coupon_forwarding_system.MainActivity.list_device;
import static com.example.coupon_forwarding_system.MainActivity.mBluetoothAdapter;
import static com.example.coupon_forwarding_system.MainActivity.mBluetoothLeScanner;
import static com.example.coupon_forwarding_system.MainActivity.mChannel;
import static com.example.coupon_forwarding_system.MainActivity.notification;
import static com.example.coupon_forwarding_system.MainActivity.notificationManager;
import static com.example.coupon_forwarding_system.MainActivity.num_list;
import static com.example.coupon_forwarding_system.MainActivity.peripheralTextView;
import static com.example.coupon_forwarding_system.MainActivity.regroup_data;
import static com.example.coupon_forwarding_system.MainActivity.startScanningButton;
import static com.example.coupon_forwarding_system.MainActivity.stopScanningButton;


import static com.example.coupon_forwarding_system.Service_Adv.pdu_len;
import static com.example.coupon_forwarding_system.Service_scan_function.add_database;
import static com.example.coupon_forwarding_system.Service_scan_function.compare_database;
import static com.example.coupon_forwarding_system.Service_scan_function.show;
import static com.example.coupon_forwarding_system.Service_scan_function.time_difference_;


public class Service_Scan extends Service {

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Service_Scan() {
        final SQLiteDatabase db = DH.getReadableDatabase();
        Log.e(TAG,"Service_Scan start");
        show(db);
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
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void notice(){
        notificationManager.notify(1, notification);
    }

    ScanCallback leScanCallback = new ScanCallback() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onScanResult(int callbackType, ScanResult result){
            //todo Notification
            stopScanningButton.setVisibility(View.VISIBLE);
            startScanningButton.setVisibility(View.INVISIBLE);
            stopScanningButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    stopScanning();
                    stopSelf();
                }
            });

            String id;
            int total,order;
            String received_data = byte2HexStr(Objects.requireNonNull(Objects.requireNonNull(result.getScanRecord()).getManufacturerSpecificData(0xffff)));
            String received_data_rsp = byte2HexStr(Objects.requireNonNull(Objects.requireNonNull(result.getScanRecord()).getManufacturerSpecificData(0xfff1)));
            received_data = received_data + received_data_rsp;

//            Log.e(TAG,"received_data: "+ received_data);
            order = Array.getByte(Objects.requireNonNull(result.getScanRecord().getManufacturerSpecificData(0xffff)), 0);
            total = Array.getByte(Objects.requireNonNull(result.getScanRecord().getManufacturerSpecificData(0xffff)), 1);

            id = received_data.subSequence(2,12).toString();

//            Log.e(TAG,"order: "+ order + " ; " + "total: " + total + " ; " + "id: " + id );

//            if(order==1){
//                received_data =
//            }else {
//                received_data = received_data.subSequence(12,received_data.length()).toString();
//            }

            received_data = received_data.subSequence(12,received_data.length()).toString();
//            Log.e(TAG,"received_data: "+ received_data);



            Calendar a = Calendar.getInstance();
//            String currentTime = f.format(a.getTime());
            long TimestampMillis = result.getTimestampNanos()/1000000; //單位:ms


            /*------------------------------------------------------------message-------------------------------------------------------------------------*/
            String msg;

            result.getTimestampNanos();
            msg="order: "+ order + " ; " + "total: " + total + " ; " + "id: " + id + "\n";

            peripheralTextView.append(msg);

            // auto scroll for text view
            final int scrollAmount = peripheralTextView.getLayout().getLineTop(peripheralTextView.getLineCount()) - peripheralTextView.getHeight();
            // if there is no need to scroll, scrollAmount will be <=0
            if (scrollAmount > 0)
                peripheralTextView.scrollTo(0, scrollAmount);

            /*----------------------------------------------------------message END-----------------------------------------------------------------------*/



            /*-------------------------------------------------------interval-----------------------------------------------------------------------------*/


            Calendar c = Calendar.getInstance();
            if(!list_device.contains(id)){
                notice();
                list_device.add(id);
                last_received_time.add(c);
                data_list.add(new ArrayList<String>());
                num_list.add(new ArrayList<Long>());
                regroup_data.add("0");
            }

            int index = list_device.indexOf(id);


            String regroup ="";
            //重組segmentation
            if (data_list.get(index).isEmpty()) {
                for (int i = 0; i < total; i++) {
                    data_list.get(index).add("0");
                    num_list.get(index).add((long) 0);
                }
            }
            if (!data_list.get(index).get(order - 1).equals(received_data)) {
                data_list.get(index).set(order - 1, received_data);
            }
            if (!data_list.get(index).contains("0") && !data_list.get(index).contains("finish")) {
                data_list.get(index).add("finish");
                for (int i = 0; i < total; i++) {
                    regroup = regroup + data_list.get(index).get(i);
                    Log.e(TAG,"regroup: "+ regroup );
                    regroup_data.set(index, regroup );
                }
//                Log.e(TAG, "regroup:" + hexToAscii(regroup));
//                Log.e(TAG, "regroup_data:" + regroup_data);

            }

            if(!data_list.get(index).contains(received_data)){
                data_list.get(index).set(order - 1, received_data);
                data_list.get(index).remove("finish");
            }

            for(int i=0;i<list_device.size();i++){
                if(!regroup_data.contains("0")){
                    Log.e(TAG,i + " time_difference: "+time_difference_(last_received_time.get(i),c));
                    if(time_difference_(last_received_time.get(i),c)>10000 && !compare_database(list_device.get(i) + regroup_data.get(i))){
                        add_database(list_device.get(i),regroup_data.get(i));
//                        list_device.remove(i);
//                        last_received_time.remove(i);
//                        data_list.remove(i);
//                        num_list.remove(i);
//                        regroup_data.remove(i);
                        Log.e(TAG,"regroup_data: "+ regroup_data.size());
                    }else {
                        last_received_time.set(index,c);
                    }
                }else {
                    last_received_time.set(index,c);
                }
            }


            //重組結束


            /*-------------------------------------------------------interval END--------------------------------------------------------------------------*/

        }


        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d("onScanFailed: " , String.valueOf(errorCode));
        }
    };

}
