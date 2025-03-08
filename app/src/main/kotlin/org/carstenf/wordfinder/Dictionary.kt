/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder

import android.database.sqlite.SQLiteQueryBuilder
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

class Dictionary internal constructor(wordFinder: WordFinder) {
    private val mDatabaseOpenHelperMap = HashMap<String, AssetDbOpenHelper>()

    private val builder: SQLiteQueryBuilder

    suspend fun lookup(word: String, db: String?): String? {
        return dbMutex.withLock {
            if (db == null) {
                Log.e(WordFinder.TAG, "Dictionary name null")
                return null
            }

            var s = word
            s = s.trim { it <= ' ' }.uppercase(Locale.getDefault())

            val helper = mDatabaseOpenHelperMap[db.trim { it <= ' ' }
                .uppercase(Locale.getDefault())]
            if (helper == null) return null

            var col = TEXT_COLUMN
            if(db == "german_wiki") {
                col = DISPLAY_TEXT_COLUMN
            }

            helper.getOrCreateDataBase().use { sqlite ->
                builder.query(
                    sqlite,
                    arrayOf(col), "text = ?",
                    arrayOf(s), null, null, null
                ).use { cursor ->
                    if (cursor == null) {
                        return null
                    } else if (!cursor.moveToFirst()) {
                        return null
                    }
                    cursor.getString(0)
                }
            }
        }
    }

    /**
     * Constructor
     *
     * The Context within which to work, used to create the DB
     */
    init {
        val am = wordFinder.assets
        val fileArray = am.list(DB_ASSET_PATH)
        if (fileArray != null) {
            for (file in fileArray) {
                val dbName =
                    file.trim { it <= ' ' }.uppercase(Locale.getDefault()).replace(".DB.7Z", "")
                mDatabaseOpenHelperMap[dbName] = AssetDbOpenHelper(
                    wordFinder.applicationContext, dbName,
                    "$DB_ASSET_PATH/$file"
                )
            }
        }
        builder = SQLiteQueryBuilder()
        builder.tables = "words"
    }

    private val dbMutex = Mutex()

    data class AllWordResult(val text: String, val displayText: String)

    suspend fun getAllWords(prefix: String, db: String?): Collection<AllWordResult>? {
        return dbMutex.withLock {
            if (db == null) return null
            val result: MutableCollection<AllWordResult> = ArrayList()
            val dbHelper = mDatabaseOpenHelperMap[db.trim { it <= ' ' }
                .uppercase(Locale.getDefault())]
            if (dbHelper == null) return null
            val supportsDisplayText = db == "german_wiki"

            var col = arrayOf(TEXT_COLUMN)
            if(supportsDisplayText) {
                col = arrayOf(TEXT_COLUMN, DISPLAY_TEXT_COLUMN)
            }

            dbHelper.getOrCreateDataBase().use { sqlite ->
                builder.query(
                    sqlite,
                    col, "prefix = ?",
                    arrayOf(prefix), null, null, null
                ).use { cursor ->
                    if (cursor == null) {
                        return null
                    } else if (!cursor.moveToFirst()) {
                        return null
                    }
                    do {

                        val displayText = if(supportsDisplayText) cursor.getString(1) else cursor.getString(0)
                        val text = cursor.getString(0)
                        result.add(AllWordResult(text, displayText))
                    } while (cursor.moveToNext())
                }
            }
            result
        }
    }

    internal companion object {
        private const val DB_ASSET_PATH = "dicts"
        private val TEXT_COLUMN = "text"
        private val DISPLAY_TEXT_COLUMN = "display_text"
    }
}