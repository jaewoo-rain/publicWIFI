package com.example.publicwifi;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.publicwifi.model.WifiData;

import java.util.ArrayList;
import java.util.List;

public class MyDBHelper extends SQLiteOpenHelper {

    public MyDBHelper(Context context) {
        super(context, "myWifi", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE if not exists myDiary (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "wifiName TEXT," +
                "password TEXT," +
                "latitude REAL," +
                "longitude REAL," +
                "descript TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS myDiary");
        onCreate(db);
    }

    /**
     * 저장하기
     */
    public void saveWifi(String wifiName, String password, double latitude, double longitude, String descript) {
        SQLiteDatabase db = getWritableDatabase();
        String sql = "INSERT INTO myDiary (wifiName, password, latitude, longitude, descript) VALUES (?, ?, ?, ?, ?)";
        db.execSQL(sql, new Object[]{wifiName, password, latitude, longitude, descript});
        db.close();
    }

    /**
     * 아이디 이용해서 삭제
     */
    public void deleteWifi(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("myDiary", "id = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    /**
     * 전체 삭제
     */
    public void deleteAllWifi() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("myDiary", null, null);
        db.close();
    }

    /**
     * 전체 와이파이 리스트 조회
     */
    public List<WifiData> getAllWifi() {
        List<WifiData> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, wifiName, descript, latitude, longitude, password FROM myDiary", null);
        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String name = cursor.getString(1);
            String desc = cursor.getString(2);
            double lat = cursor.getDouble(3);
            double lng = cursor.getDouble(4);
            String password = cursor.getString(5);
            list.add(new WifiData(id, name, desc, lat, lng, password));
        }
        cursor.close();
        db.close();
        return list;
    }
}
