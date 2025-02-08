/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder

import android.database.sqlite.SQLiteQueryBuilder
import java.util.Locale

class Dictionary internal constructor(wordFinder: WordFinder) {
    private val mDatabaseOpenHelperMap = HashMap<String, AssetDbOpenHelper>()

    private val builder: SQLiteQueryBuilder

    fun lookup(s: String, db: String): String? {
        var s = s
        s = s.trim { it <= ' ' }.uppercase(Locale.getDefault())

        val helper = mDatabaseOpenHelperMap[db.trim { it <= ' ' }
            .uppercase(Locale.getDefault())]
        if (helper == null) return null

        helper.readableDatabase.use { sqlite ->
            if (sqlite == null) return null
            builder.query(
                sqlite,
                TEXT_COLUMN, "text = ?",
                arrayOf(s), null, null, null
            ).use { cursor ->
                if (cursor == null) {
                    return null
                } else if (!cursor.moveToFirst()) {
                    return null
                }
                return cursor.getString(0)
            }
        }
    }

    /**
     * Constructor
     *
     * @param wordFinder
     * The Context within which to work, used to create the DB
     */
    init {
        val am = wordFinder.assets
        val fileArray = am.list(DB_ASSET_PATH)
        if (fileArray != null) {
            for (file in fileArray) {
                val dbName =
                    file.trim { it <= ' ' }.uppercase(Locale.getDefault()).replace(".MP3", "")
                mDatabaseOpenHelperMap[dbName] = AssetDbOpenHelper(
                    wordFinder, dbName,
                    DB_ASSET_PATH + "/" + file
                )
            }
        }
        builder = SQLiteQueryBuilder()
        builder.tables = "words"
    }

    fun getAllWords(prefix: String, db: String?): Collection<String>? {
        if(db == null) return null
        val result: MutableCollection<String> = ArrayList()
        val dbHelper = mDatabaseOpenHelperMap[db.trim { it <= ' ' }
            .uppercase(Locale.getDefault())]
        if (dbHelper == null) return null

        dbHelper.readableDatabase.use { sqlite ->
            builder.query(
                sqlite,
                TEXT_COLUMN, "prefix = ?",
                arrayOf(prefix), null, null, null
            ).use { cursor ->
                if (cursor == null) {
                    return null
                } else if (!cursor.moveToFirst()) {
                    return null
                }
                do {
                    result.add(cursor.getString(0))
                } while (cursor.moveToNext())
            }
        }
        return result
    }

    companion object {
        private const val DB_ASSET_PATH = "dicts"
        private val TEXT_COLUMN = arrayOf("text")
    }
}