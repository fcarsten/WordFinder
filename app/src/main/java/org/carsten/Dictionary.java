/**
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
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

public class Dictionary {

	private final HashMap<String, AssetDbOpenHelper> mDatabaseOpenHelperMap = new HashMap<String, AssetDbOpenHelper>();

	private SQLiteQueryBuilder builder;

	final static String DB_ASSET_PATH = "dicts";
	/**
	 * Constructor
	 * 
	 * @param wordFinder
	 *            The Context within which to work, used to create the DB
	 * @throws IOException 
	 */
	public Dictionary(WordFinder wordFinder) throws IOException {
		AssetManager am = wordFinder.getAssets();
		String fileArray[] = am.list(DB_ASSET_PATH);
		for (String file : fileArray) {
			String dbName = file.trim().toUpperCase().replace(".MP3", "");
			mDatabaseOpenHelperMap.put(dbName, new AssetDbOpenHelper(wordFinder, dbName, DB_ASSET_PATH+"/"+file));
		}
		builder = new SQLiteQueryBuilder();
		builder.setTables("words");
	}

	public String lookup(String s, String db) {
		if (s == null || s.length() < 3)
			return null;

		s = s.toUpperCase();
		String res = null;

		Cursor cursor;
		try {
			cursor = builder.query(
					mDatabaseOpenHelperMap.get(db.trim().toUpperCase()).getReadableDatabase(),
					TEXT_COLUMN, "text = ?",
					new String[] { s }, null, null, null);
			if (cursor == null) {
				return null;
			} else if (!cursor.moveToFirst()) {
				cursor.close();
				return null;
			}
			res = cursor.getString(0);
			cursor.close();
		} catch (IOException e) {
			Log.e(WordFinder.TAG, e.getMessage(),e);
		}
		return res;
	}
	
	final static private String[] TEXT_COLUMN= new String[] { "text" };

	public Cursor getAllWords(String prefix, String db) {
		Cursor cursor=null;
		try {
			cursor = builder.query(
					mDatabaseOpenHelperMap.get(db.trim().toUpperCase()).getReadableDatabase(),
					TEXT_COLUMN, "prefix = ?",
					new String[] { prefix }, null, null, null);
		} catch (IOException e) {
			Log.e(WordFinder.TAG, "Error opening DB", e);
		}
		if (cursor == null) {
			return null;
		} else if (!cursor.moveToFirst()) {
			cursor.close();
			return null;
		}
		return cursor;
	}

}