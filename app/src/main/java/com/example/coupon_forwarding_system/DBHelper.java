package com.example.coupon_forwarding_system;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    final static String TB1="coupon";

    private final static int VS=1;
    public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, null, VS);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String SQL1 = "CREATE TABLE IF NOT EXISTS "+ TB1 +
                "(_id INTEGER PRIMARY KEY AUTOINCREMENT , " +
                "ID TEXT NOT NULL, " +
                "DATA TEXT )";

        db.execSQL(SQL1);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String SQL = "DROP TABLE " + TB1;
        db.execSQL(SQL);
    }
}
