/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carsten;

import java.io.IOException;
import java.util.HashMap;

import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

class Dictionary {

	private final HashMap<String, AssetDbOpenHelper> mDatabaseOpenHelperMap = new HashMap<>();

	private final SQLiteQueryBuilder builder;

	private final static String DB_ASSET_PATH = "dicts";
	/**
	 * Constructor
	 * 
	 * @param wordFinder
	 *            The Context within which to work, used to create the DB
	 */
    Dictionary(@NotNull WordFinder wordFinder) throws IOException {
		AssetManager am = wordFinder.getAssets();
		String[] fileArray = am.list(DB_ASSET_PATH);
		if(fileArray!=null) {
            for (String file : fileArray) {
                String dbName = file.trim().toUpperCase().replace(".MP3", "");
                mDatabaseOpenHelperMap.put(dbName, new AssetDbOpenHelper(wordFinder, dbName,
                        DB_ASSET_PATH + "/" + file));
            }
        }
		builder = new SQLiteQueryBuilder();
		builder.setTables("words");
	}

	@Nullable
    String lookup(@NonNull String s, @NonNull String db) {
		s = s.trim().toUpperCase();

        AssetDbOpenHelper helper = mDatabaseOpenHelperMap.get(db.trim().toUpperCase());
        if(helper==null) return null;
        
        SQLiteDatabase sqlite = helper.getReadableDatabase();
        if(sqlite==null)
            return null;

        try (Cursor cursor = builder.query(
                sqlite,
                TEXT_COLUMN, "text = ?",
                new String[]{s}, null, null, null)) {
            if (cursor == null) {
                return null;
            } else if (!cursor.moveToFirst()) {
                return null;
            }
            return cursor.getString(0);
        }
	}
	
	final static private String[] TEXT_COLUMN= new String[] { "text" };

	@Nullable
    Cursor getAllWords(@NonNull String prefix, @NonNull String db) {

        AssetDbOpenHelper dbHelper = mDatabaseOpenHelperMap.get(db.trim().toUpperCase());
        if(dbHelper==null)
            return null;

        SQLiteDatabase sqlite = dbHelper.getReadableDatabase();
        if(sqlite==null)
            return null;

        Cursor cursor = builder.query(
    				sqlite,
					TEXT_COLUMN, "prefix = ?",
					new String[] { prefix }, null, null, null);

		if (cursor == null) {
			return null;
		} else if (!cursor.moveToFirst()) {
			cursor.close();
			return null;
		}
		return cursor;
	}

}