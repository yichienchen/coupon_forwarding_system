package com.example.ad_forwarding_system;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.annotation.RequiresApi;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;

import static com.example.ad_forwarding_system.DBHelper.TB1;
import static com.example.ad_forwarding_system.Function.hexToAscii;
import static com.example.ad_forwarding_system.Function.intToByte;
import static com.example.ad_forwarding_system.MainActivity.AdvertiseCallbacks_map;
import static com.example.ad_forwarding_system.MainActivity.DH;
import static com.example.ad_forwarding_system.MainActivity.TAG;
import static com.example.ad_forwarding_system.MainActivity.data_legacy;
import static com.example.ad_forwarding_system.MainActivity.extendedAdvertiseCallbacks_map;
import static com.example.ad_forwarding_system.MainActivity.mAdvertiseCallback;
import static com.example.ad_forwarding_system.MainActivity.mBluetoothLeAdvertiser;
import static com.example.ad_forwarding_system.MainActivity.startAdvButton;
import static com.example.ad_forwarding_system.MainActivity.stopAdvButton;




public class Service_Adv extends Service {
    static int packet_num;
    static int id_num;
    static int pdu_len;
    int count =0;



    @RequiresApi(api = Build.VERSION_CODES.O)
    public Service_Adv() {
        startAdvertising();
        stopAdvButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopSelf();
                for(int i =0;i<extendedAdvertiseCallbacks_map.size();i++){
                    final AdvertisingSetCallback exadvCallback = extendedAdvertiseCallbacks_map.get(i);
                    if (exadvCallback != null) {
                        try {
                            if (mBluetoothLeAdvertiser != null) {
                                mBluetoothLeAdvertiser.stopAdvertisingSet(exadvCallback);
                            } else {
                                Log.w(TAG, "Not able to stop broadcast; mBtAdvertiser is null");
                            }

                        }catch(RuntimeException e) { // Can happen if BT adapter is not in ON state
                            Log.w(TAG,"Not able to stop broadcast; BT state: {}");
                        }
                    }
                }
                stopAdvButton.setVisibility(View.INVISIBLE);
                startAdvButton.setVisibility(View.VISIBLE);
            }
        });
        startAdvButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                count=0;
                startAdvertising();
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void startAdvertising(){
        SQLiteDatabase db = DH.getReadableDatabase();
        Log.e(TAG, "Service: Starting Advertising");
        id_num = get_id_num(db);
        get_date(db);
        id_num = get_id_num(db);
        Log.e(TAG,"id_num: "+ id_num);
        show(db);

        if (mAdvertiseCallback == null) {
            if (mBluetoothLeAdvertiser != null) {

                while (count<id_num && (get_data(count,db)==null)){
                    count=count+1;
                }

                if(count<id_num){
                    data_legacy = Adv_data_seg(count);
                    for (int y=0;y<data_legacy.length;y++){
                        startBroadcast(y);
                    }
                }else {
                    Log.e(TAG,"no forwarding data");
                    stopAdvButton.setVisibility(View.INVISIBLE);
                    startAdvButton.setVisibility(View.VISIBLE);
                }
            }
        }

        startAdvButton.setVisibility(View.INVISIBLE);
        stopAdvButton.setVisibility(View.VISIBLE);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void startBroadcast(Integer order) {
        String localName =  String.valueOf(1) ;
        BluetoothAdapter.getDefaultAdapter().setName(localName);
        AdvertiseData advertiseData = buildAdvertiseData(order);
        AdvertiseData scanResponse = buildAdvertiseData_scan_response(order);
        AdvertisingSetParameters parameters = buildAdvertisingSetParameters();
        mBluetoothLeAdvertiser.startAdvertisingSet(parameters,advertiseData,scanResponse,
                null,null,500,0,new ExtendedAdvertiseCallback(order));



    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void stopAdvertising(){
        if (mBluetoothLeAdvertiser != null) {
            if(count<id_num){
                for (int q=0;q<data_legacy.length;q++){
                    stopBroadcast(q);
                }
            }

            mAdvertiseCallback = null;
        }
        stopAdvButton.setVisibility(View.INVISIBLE);
        startAdvButton.setVisibility(View.VISIBLE);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void stopBroadcast(Integer order) {
        final AdvertiseCallback adCallback = AdvertiseCallbacks_map.get(order);
        final AdvertisingSetCallback exadvCallback = extendedAdvertiseCallbacks_map.get(order);

            if (exadvCallback != null) {
                try {
                    if (mBluetoothLeAdvertiser != null) {
                        mBluetoothLeAdvertiser.stopAdvertisingSet(exadvCallback);
                    }
                    else {
                        Log.w(TAG,"Not able to stop broadcast; mBtAdvertiser is null");
                    }
                }
                catch(RuntimeException e) { // Can happen if BT adapter is not in ON state
                    Log.w(TAG,"Not able to stop broadcast; BT state: {}");
                }
                extendedAdvertiseCallbacks_map.remove(order);
                Log.e(TAG,"ADV stop , "  + count +"("+order+")");

                if(order == packet_num-1 && count == id_num-1){
                    count=-1;
                }

                SQLiteDatabase db = DH.getReadableDatabase();
                if(count!=id_num-1){
                    if(count==-1){
                        while((get_data(count+1,db)==null)){
                            count=count+1;
                        }
                    }else {
                        while((get_data(count,db)==null)){
                            count=count+1;
                        }
                    }

                }

                if (order == packet_num-1 && count < id_num-1){
                    count=count+1;
                    if(count<id_num){
                        data_legacy = Adv_data_seg(count);
                    }
                    for (int q=0;q<packet_num;q++) {  //x
                        if (count < id_num) {
                            startBroadcast(q);
                        }
                    }
                }

            }

            if (adCallback != null) {
                try {
                    if (mBluetoothLeAdvertiser != null) {
                        mBluetoothLeAdvertiser.stopAdvertising(adCallback);
                    }
                    else {
                        Log.w(TAG,"Not able to stop broadcast; mBtAdvertiser is null");
                    }
                }
                catch(RuntimeException e) { // Can happen if BT adapter is not in ON state
                    Log.w(TAG,"Not able to stop broadcast; BT state: {}");
                }
                AdvertiseCallbacks_map.remove(order);
            }
    }

    //data segment
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static byte[][] Adv_data_seg(int data_num){
        SQLiteDatabase db = DH.getReadableDatabase();
        String db_data = get_data(data_num,db);
        byte[] id_byte = get_id(data_num,db);

        pdu_len=48+5;
        if(db_data.length()%pdu_len!=0){
            packet_num = db_data.length()/pdu_len+1;
        }else {
            packet_num = db_data.length()/pdu_len;
        }

        StringBuilder data = new StringBuilder(db_data);
        for(int c=data.length();c%pdu_len!=0;c++){
            data.append("0");
        }
//        Log.e(TAG,"packet_num:"+packet_num);
        byte[] data_byte = data.toString().getBytes();
        byte[][] adv_byte = new byte[packet_num][pdu_len+id_byte.length+2];


        for (int counter = 0 ; counter <packet_num ; counter++) {
            adv_byte[counter][0]= intToByte(counter+1);
            System.arraycopy(id_byte, 0, adv_byte[counter], 1, id_byte.length);
            if((counter+1)*pdu_len<=data_byte.length){
                byte[] register = Arrays.copyOfRange(data_byte, counter*pdu_len ,(counter+1)*pdu_len);
                System.arraycopy(register, 0, adv_byte[counter], id_byte.length+2, register.length);
            }else {
                byte[] register = Arrays.copyOfRange(data_byte, counter*pdu_len ,data_byte.length);
                System.arraycopy(register, 0, adv_byte[counter], id_byte.length+2, register.length);
            }
        }
        return adv_byte;
    }

    static AdvertiseData buildAdvertiseData(Integer order) {
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeDeviceName(false);
        dataBuilder.setIncludeTxPowerLevel(false);
        dataBuilder.addManufacturerData(0xffff,Arrays.copyOfRange(data_legacy[order],0,27));
        return dataBuilder.build();
    }

    static AdvertiseData buildAdvertiseData_scan_response(Integer order) {
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.addManufacturerData(0xfff1,Arrays.copyOfRange(data_legacy[order],27,54));
        return dataBuilder.build();
    }

     @RequiresApi(api = Build.VERSION_CODES.O)
    public class ExtendedAdvertiseCallback extends AdvertisingSetCallback {
        private final Integer _order;
        ExtendedAdvertiseCallback(Integer order) {
            _order = order;
        }

        @Override
        public void onAdvertisingSetStarted(AdvertisingSet advertisingSet, int txPower, int status) {
            if (status==AdvertisingSetCallback.ADVERTISE_FAILED_ALREADY_STARTED)
                Log.e(TAG, "ADVERTISE_FAILED_ALREADY_STARTED");
            else if (status==AdvertisingSetCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED)
                Log.e(TAG, "ADVERTISE_FAILED_FEATURE_UNSUPPORTED");
            else if (status==AdvertisingSetCallback.ADVERTISE_FAILED_DATA_TOO_LARGE)
                Log.e(TAG, "ADVERTISE_FAILED_DATA_TOO_LARGE");
            else if (status==AdvertisingSetCallback.ADVERTISE_FAILED_INTERNAL_ERROR)
                Log.e(TAG, "ADVERTISE_FAILED_INTERNAL_ERROR");
            else if (status==AdvertisingSetCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS)
                Log.e(TAG, "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS");
            else if (status==AdvertisingSetCallback.ADVERTISE_SUCCESS) {

                Log.e(TAG,   "ADV start" + " , " + count + "(" + _order + ")");
                startAdvButton.setVisibility(View.INVISIBLE);
                stopAdvButton.setVisibility(View.VISIBLE);
                extendedAdvertiseCallbacks_map.put(_order,this);
            }
        }
        @Override
        public void onAdvertisingSetStopped(AdvertisingSet advertisingSet) {
//            Log.e(TAG, "onAdvertisingSetStopped:" + " , " + count + "("+ _order +")");
        }

        @Override
        public void onAdvertisingEnabled (AdvertisingSet advertisingSet, boolean enable, int status) {
            SQLiteDatabase db = DH.getReadableDatabase();
//            Log.e(TAG,"onAdvertisingEnabled: " + enable +" , "+ count + "("+ _order +")");
            stopAdvButton.setVisibility(View.INVISIBLE);
            startAdvButton.setVisibility(View.VISIBLE);

            if (mAdvertiseCallback == null) {
                if (mBluetoothLeAdvertiser != null) {
                    for (int q=0;q<data_legacy.length;q++){
                        if(get_data(count,db)!=null){
                            stopBroadcast(q);
                        }

                    }
                }

            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static AdvertisingSetParameters buildAdvertisingSetParameters() {
        AdvertisingSetParameters.Builder parametersBuilder = new AdvertisingSetParameters.Builder()
                .setScannable(true)
                .setConnectable(false)
                .setInterval(400)
                .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
                .setLegacyMode(true);
        return parametersBuilder.build();
    }


    private static void show(SQLiteDatabase db){
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

    //查看全部有幾筆ad
    public static int get_id_num(SQLiteDatabase db) {
        @SuppressLint("Recycle") Cursor cursor = db.query(TB1,new String[]{"_id","ID","DATA"},
                null,null,null,null,null);
        return cursor.getCount();
    }

    //取得ad內容，並且檢查forward number有無超過10，決定是否要轉傳
    private static String get_data(int data_num,SQLiteDatabase db) {
        Cursor cursor = db.query(TB1,new String[]{"DATA"},
                null,null,null,null,null);
        String s,s1,s2;
        int forward;
        String new_s = "";
        cursor.moveToPosition(data_num);
        s = cursor.getString(0);

        s1 = s.substring(0,10);
        s2 = hexToAscii(s.substring(10));

        forward = Integer.parseInt(s1.substring(0,2));

        if(forward>9){
            new_s = null;
        }else {
            new_s = s2;
        }

        cursor.close();
        return new_s;
    }

    //取得id
    private static byte[] get_id(int data_num,SQLiteDatabase db) {
        @SuppressLint("Recycle") Cursor cursor = db.query(TB1,new String[]{"ID","DATA"},
                null,null,null,null,null);
        cursor.moveToPosition(data_num);
        String id = cursor.getString(0);
        String data = cursor.getString(1);

        String s1 = data.substring(0,10);
        int forward = Integer.parseInt(s1.substring(0,2)) + 1;
        byte[] date = hexToBytes(s1.substring(2));
        date = byteMerger( bigIntToByteArray(forward),date);

        byte[] re;
        byte[] id_ = hexToBytes(id);
        re = byteMerger(id_,date);
        return re;
    }

    //檢查ad有效日期
    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void get_date(SQLiteDatabase db) {
        Cursor cursor = db.query(TB1,new String[]{"ID","DATA"},
                null,null,null,null,null);

        Date date1 = new Date();
        LocalDate localDate = date1.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        int y = localDate.getYear();
        int m = localDate.getMonthValue();
        int d = localDate.getDayOfMonth();
//        Log.e(TAG,"date: " + y + m + d );

        for(int i =0 ; i <id_num ; i++){
            cursor.moveToPosition(i);
            String data = cursor.getString(1);
            int year,month,day;
            byte[] date;
            date = hexToBytes(data.substring(2,10));
            year = date[0]*256+(date[1]& 0xff);
            month = date[2];
            day = date[3];

//            Log.e(TAG,"date: " + year + month + day);

            if(y>year){
                delete(cursor.getString(0));
            }else if(y==year){
                if(m>month){
                    delete(cursor.getString(0));
                }else if(m==month){
                    if(d>day){
                        delete(cursor.getString(0));
                    }
                }
            }
//            Log.e(TAG,"get_date: "  + year + month + day);
        }

        cursor.close();
    }

    //刪除過期ad
    public static void delete(String id){
        SQLiteDatabase db = DH.getWritableDatabase();
        db.delete(TB1,"ID=?",new String[]{id});
    }

    private static byte[] bigIntToByteArray(final int i) {
        BigInteger bigInt = BigInteger.valueOf(i);
        return bigInt.toByteArray();
    }

    public static byte[] hexToBytes(String hexString) {

        char[] hex = hexString.toCharArray();
        //轉rawData長度減半
        int length = hex.length / 2;
        byte[] rawData = new byte[length];
        for (int i = 0; i < length; i++) {
            //先將hex資料轉10進位數值
            int high = Character.digit(hex[i * 2], 16);
            int low = Character.digit(hex[i * 2 + 1], 16);
            //將第一個值的二進位值左平移4位,ex: 00001000 => 10000000 (8=>128)
            //然後與第二個值的二進位值作聯集ex: 10000000 | 00001100 => 10001100 (137)
            int value = (high << 4) | low;
            //與FFFFFFFF作補集
            if (value > 127)
                value -= 256;
            //最後轉回byte就OK
            rawData [i] = (byte) value;
        }
        return rawData ;
    }

    //將ad個個欄位合起來
    public static byte[] byteMerger(byte[] byte_1, byte[] byte_2) {
        byte[] byte_3 = new byte[byte_1.length + byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }
}