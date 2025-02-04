/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;
import android.util.Log;

class AssetDbOpenHelper {

	@NonNull
    final private SQLiteOpenHelper dbHelperDelegate;

	AssetDbOpenHelper(@NonNull final Context context, @NonNull final String dbName,
                      @NonNull final String dbFileName) {

		dbHelperDelegate = new SQLiteOpenHelper(context, dbName, null, 1) {
			@Override
			public void onCreate(SQLiteDatabase db) {
				Log.i(WordFinder.TAG, "db onCreate");
			}

			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion,
					int newVersion) {
				Log.i(WordFinder.TAG, "db onUpdate");
			}
		};
		(new Thread(() -> {
            synchronized (createDbLock) {
                Log.i(WordFinder.TAG, "Started creating db");
                try {
                    createDataBase(context, dbName, dbFileName);
                } catch (IOException e) {
                    Log.e(WordFinder.TAG, e.getMessage(), e);
                }
                Log.i(WordFinder.TAG, "Finished creating db");
            }
        }, "DB Installer")).start();
	}

	private final Object createDbLock = new Object();

	/**
	 * Creates a empty database on the system and rewrites it with your own
	 * database.
	 * */
	private void createDataBase(@NonNull Context context, @NonNull String dbName,
                                @NonNull String dbFileName) throws IOException {
		boolean dbExist = checkDataBaseExists(context, dbName);

		if (!dbExist) {
			Log.d(WordFinder.TAG, "Creating DB");
			dbHelperDelegate.getReadableDatabase();
			dbHelperDelegate.close();
			copyDataBase(context, dbName, dbFileName);
			Log.d(WordFinder.TAG, "Finished creating DB");
		}
	}

	/**
	 * Check if the database already exist to avoid re-copying the file each
	 * time you open the application.
	 * 
	 * @return true if it exists, false if it doesn't
	 */
	private boolean checkDataBaseExists(@NonNull Context context, @NonNull String dbName) {
		File path = context.getDatabasePath(dbName);
		return path.exists();
	}

	/**
	 * Copies your database from your local assets-folder to the just created
	 * empty database in the system folder, from where it can be accessed and
	 * handled. This is done by transferring byte stream.
	 * */
	private void copyDataBase(@NonNull Context context, @NonNull String dbName,
							  @NonNull String dbFileName) throws IOException {
		Log.d(WordFinder.TAG, "Copying DB");

		// Open your local db as the input stream
		InputStream myInput = context.getAssets().open(dbFileName);

		// Open the empty db as the output stream
		OutputStream myOutput = new FileOutputStream(
				context.getDatabasePath(dbName), false);

		// transfer bytes from the input file to the output file
		byte[] buffer = new byte[4 * 1024];
		int length;
		while ((length = myInput.read(buffer)) > 0) {
			myOutput.write(buffer, 0, length);
		}

		// Close the streams
		myOutput.flush();
		myOutput.close();
		myInput.close();
		Log.d(WordFinder.TAG, "Finished copying DB");
	}

	public synchronized void close() {
		synchronized (createDbLock) {
			dbHelperDelegate.close();
		}
	}

	SQLiteDatabase getReadableDatabase() {
		synchronized (createDbLock) {
			return dbHelperDelegate.getReadableDatabase();
		}
	}
}