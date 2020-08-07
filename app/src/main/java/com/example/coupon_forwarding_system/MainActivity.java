package com.example.coupon_forwarding_system;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import java.util.TreeMap;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static com.example.coupon_forwarding_system.DBHelper.TB1;
import static com.example.coupon_forwarding_system.Function.hexToAscii;
import static com.example.coupon_forwarding_system.Service_Adv.get_id_num;
import static com.example.coupon_forwarding_system.Service_Adv.hexToBytes;

public class MainActivity extends AppCompatActivity {

    static int ManufacturerData_size = 24;  //ManufacturerData長度
    static String TAG = "chien";

    public static byte[][] data_legacy;
    public static byte[][] data_extended;

//    public static byte[] id_byte = new byte[] {0x22, 0x6c, 0x74, 0x52};

    static boolean version = true;  //true: 4.0 , false:5.0

//    static byte[] id_byte = new byte[4];


    static List<String> list_device = new ArrayList<>();

    static List<Calendar> last_received_time = new ArrayList<>();
    static List<String> regroup_data = new ArrayList<>();

    static ArrayList<ArrayList<Long>> num_list = new ArrayList<>();
    static  ArrayList<ArrayList<String>> data_list = new ArrayList<>();

    static Map<Integer, AdvertiseCallback> AdvertiseCallbacks_map;
    static Map<Integer, AdvertisingSetCallback> extendedAdvertiseCallbacks_map;


    static BluetoothManager mBluetoothManager;
    static BluetoothAdapter mBluetoothAdapter;
    static BluetoothLeScanner mBluetoothLeScanner;
    static AdvertiseCallback mAdvertiseCallback;
    static BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    static ImageButton startScanningButton;
    static ImageButton stopScanningButton;
    static ImageButton scan_list;
    static ImageButton startAdvButton;
    static ImageButton stopAdvButton;
    public static TextView peripheralTextView;
    static TextView sql_Text;

    //    default mode: low power

    static NotificationManager notificationManager;
    static NotificationChannel mChannel;
    Intent intentMainActivity;
    static PendingIntent pendingIntent;
    static Notification notification;
    static Intent received_id;

    Intent adv_service;
    Intent scan_service;

    public static DBHelper DH=null;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DH = new DBHelper(this,"COUPON_DB",null,1);

        initialize();
        permission();
        element();
        mjobScheduler();

        Notify();
    }


    @Override
    public void onDestroy() {
        stopService(adv_service);
        stopService(scan_service);
        Log.e(TAG, "onDestroy() called");
        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onResume() {
        super.onResume();
//        Log.e(TAG, "onResume() called");
        permission();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initialize() {
        if (mBluetoothLeScanner == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager != null) {
                BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
                if (bluetoothAdapter != null) {
                    mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                }
            }
        }
        if (mBluetoothLeAdvertiser == null) {
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null) {
                BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
                if (bluetoothAdapter != null) {
                    mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
                }
            }
        }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void permission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "!!!!!!!");
            requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 10);
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.VIBRATE}, 1);
        }

    }

    private void element() {
        /*---------------------------------------scan-----------------------------------------*/
        startScanningButton = findViewById(R.id.StartScanButton);
        startScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startService(scan_service);
            }
        });
        stopScanningButton = findViewById(R.id.StopScanButton);
        stopScanningButton.setVisibility(View.INVISIBLE);
        scan_list = findViewById(R.id.scan_list);
        scan_list.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final SQLiteDatabase db = DH.getReadableDatabase();


                if (v.getId() == R.id.scan_list) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Coupon List")
                            .setItems(card_list(db)[3], new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, final int which) {
                                    StringBuilder resultData = new StringBuilder("");
                                    resultData.append("date: ").append(card_list(db)[2][which]).append("\n");
                                    resultData.append("contents: ").append(card_list(db)[4][which]).append("\n");
                                    Toast.makeText(getApplicationContext(), resultData, Toast.LENGTH_SHORT).show();

                                    new AlertDialog.Builder(MainActivity.this)
                                            .setTitle(card_list(db)[3][which])
                                            .setMessage(resultData)
                                            .setPositiveButton("close", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int w) {

                                                }
                                            })
                                            .setNegativeButton("delete", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int w) {
                                                    Log.e(TAG,"刪掉");
                                                    delete(card_list(db)[0][which]);
                                                }
                                            })
                                            .show();

                                }
                            })
                            .setPositiveButton("close", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            })
                            .show();

                }
            }
        });

        /*--------------------------------------advertise----------------------------------------*/
        final SQLiteDatabase db = DH.getReadableDatabase();
        startAdvButton = findViewById(R.id.StartAdvButton);
        startAdvButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(get_id_num(db)!=0){
                    startService(adv_service);
                } else {
                    Toast.makeText(MainActivity.this,"There is no coupons!",Toast.LENGTH_SHORT).show();
                }



            }
        });
        stopAdvButton = findViewById(R.id.StopAdvButton);
        stopAdvButton.setVisibility(View.INVISIBLE);

        /*--------------------------------------intent----------------------------------------*/
        adv_service = new Intent(MainActivity.this, Service_Adv.class);
        scan_service = new Intent(MainActivity.this, Service_Scan.class);

        /*-------------------------------------Receiver---------------------------------------*/
        received_id = new Intent();

        /*--------------------------------------others----------------------------------------*/
        peripheralTextView = findViewById(R.id.PeripheralTextView);
        peripheralTextView.setMovementMethod(new ScrollingMovementMethod()); //垂直滾動
        AdvertiseCallbacks_map = new TreeMap<>();
        extendedAdvertiseCallbacks_map = new TreeMap<>();

    }

    private static String[][] card_list(SQLiteDatabase db){
        Cursor cursor = db.query(TB1,new String[]{"_id","ID","DATA"},
                null,null,null,null,null);

        String[][] list = new String[5][cursor.getCount()];


        while(cursor.moveToNext()){

            String _id = cursor.getString(0);
            String ID = cursor.getString(1);
            String DATA = cursor.getString(2);

            byte[] bytes = hexToBytes(DATA.substring(2,10));

            String date,title,data_;

            date = (bytes[0] * 256 + (bytes[1] & 0xff)) +"."+ String.valueOf(bytes[2]) +"."+ String.valueOf(bytes[3]);
            Log.e(TAG,"DATA: "+ date);
            String[] parts = hexToAscii(DATA.substring(10)).split(":");
            title = parts[0];
            data_ = parts[1];

//            Log.e(TAG,"date: "+ date);
//            Log.e(TAG,"title: "+ title);
//            Log.e(TAG,"data_: "+ data_);

            list[0][cursor.getPosition()] = _id;
            list[1][cursor.getPosition()] = ID;
            list[2][cursor.getPosition()] = date;
            list[3][cursor.getPosition()] = title;
            list[4][cursor.getPosition()] = data_;

        }
        cursor.close();

        return list;
    }

    public static void delete(String _id){
        SQLiteDatabase db = DH.getWritableDatabase();
        db.delete(TB1,"_id=?",new String[]{_id});
    }

    public void mjobScheduler(){
        Log.e(TAG,"mjobScheduler");
        JobScheduler scheduler = (JobScheduler)getSystemService(Context.JOB_SCHEDULER_SERVICE);

        ComponentName componentName = new ComponentName(this, JOBservice_event_check.class);
        ComponentName componentName1 = new ComponentName(this, Jobservice_data_check.class);

        JobInfo job = new JobInfo.Builder(1, componentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true) // 重開機後是否執行
                .setPeriodic(1000*60*15)
                .build();

        JobInfo job1 = new JobInfo.Builder(2, componentName1)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true) // 重開機後是否執行
                .setPeriodic(1000*60*60*12)
                .build();

//調用schedule
        assert scheduler != null;
        scheduler.schedule(job);
        scheduler.schedule(job1);

    }

    private void Notify() {
        Intent intent1 = new Intent(this,MainActivity.class);
        PendingIntent pendingIntent1 = PendingIntent.getActivity(this,0,intent1,PendingIntent.FLAG_UPDATE_CURRENT);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mChannel = new NotificationChannel("Coupon" , "優惠卷" , NotificationManager.IMPORTANCE_HIGH) ;
        notification = new Notification.Builder(this,"Coupon")
                .setContentTitle("COUPON")
//                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentText("open")
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setContentIntent(pendingIntent1)
                .build();
//        mChannel.setImportance(NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(mChannel);
    }

}
