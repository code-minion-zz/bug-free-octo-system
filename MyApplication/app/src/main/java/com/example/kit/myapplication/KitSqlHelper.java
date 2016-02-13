package com.example.kit.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

/**
 * Created by Kit on 13/02/2016.
 */
public class KitSqlHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "KitMapApp.db";
    private static final String TABLE_LOCATIONS = "locations";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_LOCATIONSTRING = "locationstring";

    public KitSqlHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_LOCATIONS_VISITED_TABLE = "CREATE TABLE "+ TABLE_LOCATIONS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY," + COLUMN_LOCATIONSTRING
            + " TEXT)";
        db.execSQL(CREATE_LOCATIONS_VISITED_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);
        onCreate(db);
    }

    public void addEntry(String entry)
    {
        ContentValues values = new ContentValues();
        values.put(COLUMN_LOCATIONSTRING, entry);

        SQLiteDatabase db = this.getWritableDatabase();

        db.insert(TABLE_LOCATIONS, null, values);
        db.close();
    }

    public void saveAllEntries(ArrayList<String> entries)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        for (String entry : entries) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_LOCATIONSTRING, entry);
            db.insert(TABLE_LOCATIONS, null, values);
        }

        db.close();
    }

    public ArrayList<String> getAllEntries()
    {
        String query = "SELECT * FROM " + TABLE_LOCATIONS;
        ArrayList<String> result = new ArrayList<>();

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if (!cursor.moveToFirst())
        {
            return result;
        }

        do
        {
            result.add(cursor.getString(1));
        }
        while(cursor.moveToNext());

        cursor.close();

        return result;
    }

    /// we probably wont need this
    public boolean deleteEntry(String entry)
    {
        boolean result = false;

        String query = "Select * FROM " + TABLE_LOCATIONS + " WHERE " + COLUMN_LOCATIONSTRING + " =  \"" + entry + "\"";

        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.rawQuery(query, null);

        int id;

        if (cursor.moveToFirst()) {
            id = Integer.parseInt(cursor.getString(0));
            db.delete(TABLE_LOCATIONS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
            cursor.close();
            result = true;
        }
        db.close();

        return result;
    }

    public void recreateTable()
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);
        onCreate(db);
    }
}
