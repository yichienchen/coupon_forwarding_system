package com.example.ad_forwarding_system;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;

import static com.example.ad_forwarding_system.DBHelper.TB1;
import static com.example.ad_forwarding_system.MainActivity.DH;
import static com.example.ad_forwarding_system.MainActivity.TAG;


public class Service_scan_function {
    static String name, phone, email, company, position, other;

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

    }

    public static long time_difference_(Calendar first, Calendar last){
        Date first_time = first.getTime();
        Date last_time = last.getTime();

        long different = last_time.getTime() - first_time.getTime();

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        different = different % daysInMilli;
        different = different % hoursInMilli;
        different = different % minutesInMilli;
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

            if(data.equals(resultData.toString())){
                b=true;
                Log.e(TAG,"一樣");
            }
        }
        cursor.close();
        return b;
    }


}
