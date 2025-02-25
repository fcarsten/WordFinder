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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AssetDbOpenHelper(
    val context: Context, val dbName: String,
    val dbFileName: String
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

    private val createDbMutex = Mutex()

    /**
     * Creates a empty database on the system and rewrites it with your own
     * database.
     */
    suspend fun getOrCreateDataBase(): SQLiteDatabase? {
        createDbMutex.withLock {

            val dbExist = checkDataBaseExists(context, dbName)

            if (!dbExist) {
                Log.d(WordFinder.TAG, "Creating DB")
                dbHelperDelegate.readableDatabase
                dbHelperDelegate.close()
                copyDataBase(context, dbName, dbFileName)
                Log.d(WordFinder.TAG, "Finished creating DB")
            }
            return dbHelperDelegate.readableDatabase
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
        val dbPath = context.getDatabasePath(dbName).path

        Log.d(WordFinder.TAG, "Copying DB")

        try {
            // Open the compressed database from the assets subfolder
            val inputStream = context.assets.open(dbFileName)

            val tempFile = File.createTempFile("temp_db", ".7z", context.cacheDir)
            tempFile.deleteOnExit()

            // Copy the 7z file to a temporary file
            val outputStream = FileOutputStream(tempFile)
            inputStream.copyTo(outputStream)
            outputStream.close()
            inputStream.close()

            // Decompress the 7z file
            val sevenZFile = SevenZFile(tempFile)
            val entries = sevenZFile.entries
            Log.d(WordFinder.TAG, "Number of entries in 7z file: $entries")
            var entry = sevenZFile.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    // Write the decompressed database to internal storage
                    val dbOutputStream = FileOutputStream(dbPath)
                    val buffer = ByteArray(8192)
                    var length: Int
                    while (sevenZFile.read(buffer).also { length = it } > 0) {
                        dbOutputStream.write(buffer, 0, length)
                    }
                    dbOutputStream.flush()
                    dbOutputStream.close()
                    Log.d(WordFinder.TAG, "Database decompressed and copied to internal storage")
                    break
                }
                entry = sevenZFile.nextEntry
            }

            sevenZFile.close()
            tempFile.delete()
            Log.d(WordFinder.TAG, "Finished copying DB")
        } catch (e: IOException) {
            Log.e(WordFinder.TAG, "Error copying and decompressing database", e)
        }

    }
//
//    override fun close() {
//        runBlocking {
//            createDbMutex.withLock {
//                dbHelperDelegate.close()
//            }
//        }
//    }

}