/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

internal class AssetDbOpenHelper(
    context: Context, dbName: String,
    dbFileName: String
) {
    private val dbHelperDelegate: SQLiteOpenHelper =
        object : SQLiteOpenHelper(context, dbName, null, 1) {
            override fun onCreate(db: SQLiteDatabase) {
                Log.i(WordFinder.TAG, "db onCreate")
            }

            override fun onUpgrade(
                db: SQLiteDatabase, oldVersion: Int,
                newVersion: Int
            ) {
                Log.i(WordFinder.TAG, "db onUpdate")
            }
        }

    private val createDbLock = Any()

    init {
        (Thread({
            synchronized(createDbLock) {
                Log.i(WordFinder.TAG, "Started creating db")
                try {
                    createDataBase(context, dbName, dbFileName)
                } catch (e: IOException) {
                    Log.e(WordFinder.TAG, e.message, e)
                }
                Log.i(WordFinder.TAG, "Finished creating db")
            }
        }, "DB Installer")).start()
    }

    /**
     * Creates a empty database on the system and rewrites it with your own
     * database.
     */
    @Throws(IOException::class)
    private fun createDataBase(
        context: Context, dbName: String,
        dbFileName: String
    ) {
        val dbExist = checkDataBaseExists(context, dbName)

        if (!dbExist) {
            Log.d(WordFinder.TAG, "Creating DB")
            dbHelperDelegate.readableDatabase
            dbHelperDelegate.close()
            copyDataBase(context, dbName, dbFileName)
            Log.d(WordFinder.TAG, "Finished creating DB")
        }
    }

    /**
     * Check if the database already exist to avoid re-copying the file each
     * time you open the application.
     *
     * @return true if it exists, false if it doesn't
     */
    private fun checkDataBaseExists(context: Context, dbName: String): Boolean {
        val path = context.getDatabasePath(dbName)
        return path.exists()
    }

    /**
     * Copies your database from your local assets-folder to the just created
     * empty database in the system folder, from where it can be accessed and
     * handled. This is done by transferring byte stream.
     */
    @Throws(IOException::class)
    private fun copyDataBase(
        context: Context, dbName: String,
        dbFileName: String
    ) {
        Log.d(WordFinder.TAG, "Copying DB")

        // Open your local db as the input stream
        val myInput = context.assets.open(dbFileName)

        // Open the empty db as the output stream
        val myOutput: OutputStream = FileOutputStream(
            context.getDatabasePath(dbName), false
        )

        // transfer bytes from the input file to the output file
        val buffer = ByteArray(4 * 1024)
        var length: Int
        while ((myInput.read(buffer).also { length = it }) > 0) {
            myOutput.write(buffer, 0, length)
        }

        // Close the streams
        myOutput.flush()
        myOutput.close()
        myInput.close()
        Log.d(WordFinder.TAG, "Finished copying DB")
    }

    @Synchronized
    fun close() {
        synchronized(createDbLock) {
            dbHelperDelegate.close()
        }
    }

    val readableDatabase: SQLiteDatabase
        get() {
            synchronized(createDbLock) {
                return dbHelperDelegate.readableDatabase
            }
        }
}