package com.example.a4200project;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class JokeDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "FavoriteJokes.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NAME = "jokes";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_SETUP = "setup";
    private static final String COLUMN_PUNCHLINE = "punchline";

    public JokeDbHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_SETUP + " TEXT NOT NULL, " +
                COLUMN_PUNCHLINE + " TEXT NOT NULL)";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public long addJoke(String setup, String punchline) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SETUP, setup);
        values.put(COLUMN_PUNCHLINE, punchline);
        long result = db.insert(TABLE_NAME, null, values);
        db.close();
        return result;
    }

    public List<Joke> getAllJokes() {
        List<Joke> jokeList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_NAME +
                " ORDER BY " + COLUMN_ID + " DESC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String setup = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SETUP));
                String punchline = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PUNCHLINE));

                jokeList.add(new Joke(setup, punchline, id));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return jokeList;
    }

    public void deleteJoke(int jokeId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME,
                COLUMN_ID + " = ?",
                new String[]{String.valueOf(jokeId)});
        db.close();
    }

    public static class Joke {
        private final String setup;
        private final String punchline;
        private final int id;

        public Joke(String setup, String punchline, int id) {
            this.setup = setup;
            this.punchline = punchline;
            this.id = id;
        }

        public String getSetup() {
            return setup;
        }

        public String getPunchline() {
            return punchline;
        }

        public int getId() {
            return id;
        }
    }
}