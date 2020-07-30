package com.example.coupon_forwarding_system;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import static com.example.coupon_forwarding_system.DBHelper.TB1;
import static com.example.coupon_forwarding_system.Function.byte2HexStr;
import static com.example.coupon_forwarding_system.Function.hexToAscii;
import static com.example.coupon_forwarding_system.MainActivity.DH;
import static com.example.coupon_forwarding_system.MainActivity.TAG;
import static com.example.coupon_forwarding_system.MainActivity.data_list;
import static com.example.coupon_forwarding_system.MainActivity.last_received_time;
import static com.example.coupon_forwarding_system.MainActivity.list_device;
import static com.example.coupon_forwarding_system.MainActivity.notificationManager;
import static com.example.coupon_forwarding_system.MainActivity.num_list;
import static com.example.coupon_forwarding_system.MainActivity.peripheralTextView;
import static com.example.coupon_forwarding_system.MainActivity.regroup_data;



public class Service_scan_function {
    private static SimpleDateFormat f = new SimpleDateFormat("YYYY-MM-dd,HH:mm:ss.SS");

    static String name, phone, email, company, position, other;

//    static ScanCallback leScanCallback = new ScanCallback() {
//        @Override
//        public void onScanResult(int callbackType, ScanResult result){
//            //todo Notification
//            String id;
//            int total,order,forward_num;
//            String received_data = byte2HexStr(Objects.requireNonNull(Objects.requireNonNull(result.getScanRecord()).getManufacturerSpecificData(0xffff)));
//            String received_data_rsp = byte2HexStr(Objects.requireNonNull(Objects.requireNonNull(result.getScanRecord()).getManufacturerSpecificData(0xfff1)));
//            received_data = received_data + received_data_rsp;
//
////            Log.e(TAG,"received_data: "+ received_data);
//            order = Array.getByte(Objects.requireNonNull(result.getScanRecord().getManufacturerSpecificData(0xffff)), 0);
//            total = Array.getByte(Objects.requireNonNull(result.getScanRecord().getManufacturerSpecificData(0xffff)), 1);
//
//            id = received_data.subSequence(2,12).toString();
//
////            Log.e(TAG,"order: "+ order + " ; " + "total: " + total + " ; " + "id: " + id );
//
//            received_data = received_data.subSequence(12,received_data.length()).toString();
////            Log.e(TAG,"received_data: "+ received_data);
//
//
//
//            Calendar a = Calendar.getInstance();
//            String currentTime = f.format(a.getTime());
//            long TimestampMillis = result.getTimestampNanos()/1000000; //單位:ms
//
//
//            /*------------------------------------------------------------message-------------------------------------------------------------------------*/
//            String msg;
//
//            result.getTimestampNanos();
//            msg="order: "+ order + " ; " + "total: " + total + " ; " + "id: " + id + "\n";
//
//            peripheralTextView.append(msg);
//
//            // auto scroll for text view
//            final int scrollAmount = peripheralTextView.getLayout().getLineTop(peripheralTextView.getLineCount()) - peripheralTextView.getHeight();
//            // if there is no need to scroll, scrollAmount will be <=0
//            if (scrollAmount > 0)
//                peripheralTextView.scrollTo(0, scrollAmount);
//
//            /*----------------------------------------------------------message END-----------------------------------------------------------------------*/
//
//
//
//            /*-------------------------------------------------------interval-----------------------------------------------------------------------------*/
//
//
//            Calendar c = Calendar.getInstance();
//            if(!list_device.contains(id)){
//                list_device.add(id);
//                last_received_time.add(c);
//                data_list.add(new ArrayList<String>());
//                num_list.add(new ArrayList<Long>());
//                regroup_data.add("0");
//            }
//
//            int index = list_device.indexOf(id);
//
//
//            String regroup ="";
//            //重組segmentation
//            if (data_list.get(index).isEmpty()) {
//                for (int i = 0; i < total; i++) {
//                    data_list.get(index).add("0");
//                    num_list.get(index).add((long) 0);
//                }
//            }
//            if (!data_list.get(index).get(order - 1).equals(received_data)) {
//                data_list.get(index).set(order - 1, received_data);
//            }
//            if (!data_list.get(index).contains("0") && !data_list.get(index).contains("finish")) {
//                data_list.get(index).add("finish");
//                for (int i = 0; i < total; i++) {
//                    regroup = regroup + data_list.get(index).get(i);
//                    Log.e(TAG,"regroup: "+hexToAscii(regroup));
//                    regroup_data.set(index,hexToAscii(regroup));
//                }
////                Log.e(TAG, "regroup:" + hexToAscii(regroup));
//                Log.e(TAG, "regroup_data:" + regroup_data);
//
//            }
//
//            if(!data_list.get(index).contains(received_data)){
//                data_list.get(index).set(order - 1, received_data);
//                data_list.get(index).remove("finish");
//            }
//
//            for(int i=0;i<list_device.size();i++){
//                if(!regroup_data.contains("0")){
//                    Log.e(TAG,i + " time_difference: "+time_difference_(last_received_time.get(i),c));
//                    if(time_difference_(last_received_time.get(i),c)>10000 && !compare_database(list_device.get(i) + regroup_data.get(i))){
//                        add_database(list_device.get(i),regroup_data.get(i));
////                        list_device.remove(i);
////                        last_received_time.remove(i);
////                        data_list.remove(i);
////                        num_list.remove(i);
////                        regroup_data.remove(i);
//                        Log.e(TAG,"regroup_data: "+ regroup_data.size());
//                    }else {
//                        last_received_time.set(index,c);
//                    }
//                }else {
//                    last_received_time.set(index,c);
//                }
//            }
//
//
//            //重組結束
//
//
//            /*-------------------------------------------------------interval END--------------------------------------------------------------------------*/
//
//        }
//
//
//        @Override
//        public void onScanFailed(int errorCode) {
//            super.onScanFailed(errorCode);
//            Log.d("onScanFailed: " , String.valueOf(errorCode));
//        }
//    };

    private static void split(String string){
        String[] parts = string.split("\\:");
        name = parts[0];
        phone = parts[1];
        email = parts[2];
        company = parts[3];
        position = parts[4];
        other = parts[5];

        Log.e(TAG,"name: "+ name + "\n"
                + "phone: " + phone + "\n"
                + "email: " + email + "\n"
                + "company: " + company + "\n"
                + "position: " + position + "\n"
                + "other: " + other );

        StringBuilder resultData = new StringBuilder("");
        resultData.append(name).append(phone).append(email).append(company).append(position).append(other);
        if(compare_database(resultData.toString())){
//            add(id,rec,email,company,position,other);
        }

    }

    public static long time_difference_(Calendar first, Calendar last){
        Date first_time = first.getTime();
        Date last_time = last.getTime();

        long different = last_time.getTime() - first_time.getTime();

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = different / daysInMilli;
        different = different % daysInMilli;
        long elapsedHours = different / hoursInMilli;
        different = different % hoursInMilli;
        long elapsedMinutes = different / minutesInMilli;
        different = different % minutesInMilli;
        long elapsedSeconds = different / secondsInMilli;
//        Log.e(TAG,"different: "+elapsedDays +"days, " + elapsedHours + "hours, " + elapsedMinutes +"minutes, " + elapsedSeconds +"seconds. ");
        return different;
    }


    public static void add_database(String ID,String data) {
        SQLiteDatabase db = DH.getReadableDatabase();
        ContentValues values = new ContentValues();

        values.put("ID",ID);
        values.put("DATA",data);
        db.insert(TB1,null,values);
        show(db);
    }

    public static void show(SQLiteDatabase db){
        Cursor cursor = db.query(TB1,new String[]{"_id","ID","DATA"},
                null,null,null,null,null);

        StringBuilder resultData = new StringBuilder("RESULT: \n");
        while(cursor.moveToNext()){
            int _id = cursor.getInt(0);
            String ID = cursor.getString(1);
            String data = cursor.getString(2);

            resultData.append(_id);
            resultData.append(" ID: ").append(ID);
            resultData.append(" data: ").append(data).append("\n");
        }

        Log.e(TAG,"resultData: " + resultData );

        cursor.close();
    }

    public static boolean compare_database(String data){
        SQLiteDatabase db = DH.getReadableDatabase();
        Cursor cursor = db.query(TB1,new String[]{"_id","ID","DATA"},
                null,null,null,null,null);

        boolean b =false;

        while(cursor.moveToNext()){
            StringBuilder resultData = new StringBuilder("");
            String ID = cursor.getString(1);
            String DATA = cursor.getString(2);
            resultData.append(ID).append(DATA);
//            Log.e(TAG,"data: "+data);
//            Log.e(TAG,"result Data: " + resultData);
            if(data.equals(resultData.toString())){
                b=true;
                Log.e(TAG,"一樣");
            }
        }
        cursor.close();
        return b;
    }


}
