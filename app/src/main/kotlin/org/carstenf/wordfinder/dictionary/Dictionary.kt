package org.carstenf.wordfinder.dictionary

import android.database.sqlite.SQLiteQueryBuilder
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.carstenf.wordfinder.util.AssetDbOpenHelper
import org.carstenf.wordfinder.WordFinder
import java.util.Locale

class Dictionary internal constructor(wordFinder: WordFinder) {
    private val mDatabaseOpenHelperMap = HashMap<String, AssetDbOpenHelper>()

    private val builder: SQLiteQueryBuilder

    suspend fun lookup(word: String, db: String?): WordInfoData? {
        return dbMutex.withLock {
            if (db == null) {
                Log.e(WordFinder.Companion.TAG, "Dictionary name null") // NON-NLS
                return null
            }

            var s = word
            s = s.trim { it <= ' ' }.uppercase(Locale.getDefault())

            val helper = mDatabaseOpenHelperMap[db.trim { it <= ' ' }
                .uppercase(Locale.getDefault())]
            if (helper == null) return null

            val supportsDisplayText = (db  == "german_wiki") || (db  == "german_simple") // NON-NLS
            val supportsLemma = (db  == "german_wiki") || (db  == "german_simple") // NON-NLS

            var col = arrayOf(TEXT_COLUMN)
            if(supportsDisplayText) {
                @Suppress("KotlinConstantConditions")
                col = if(supportsLemma) {
                    arrayOf(TEXT_COLUMN, DISPLAY_TEXT_COLUMN, LEMMA_COLUMN)
                } else {
                    arrayOf(TEXT_COLUMN, DISPLAY_TEXT_COLUMN)
                }
            }

            helper.getOrCreateDataBase().use { sqlite ->
                builder.query(
                    sqlite,
                    col, "text = ?", // NON-NLS
                    arrayOf(s), null, null, null
                ).use { cursor ->
                    if (cursor == null) {
                        return null
                    } else if (!cursor.moveToFirst()) {
                        return null
                    }
                    val text = cursor.getString(0)
                    val displayText = if(supportsDisplayText) cursor.getString(1) else text
                    val lemma = if(supportsLemma) cursor.getString(2) else displayText

                    WordInfoData(text, displayText, lemma)
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
                    file.trim { it <= ' ' }.uppercase(Locale.getDefault()).replace(".7Z", "")
                mDatabaseOpenHelperMap[dbName] = AssetDbOpenHelper(
                    wordFinder.applicationContext, dbName,
                    "$DB_ASSET_PATH/$file" // NON-NLS
                )
            }
        }
        builder = SQLiteQueryBuilder()
        builder.tables = "words" // NON-NLS
    }

    private val dbMutex = Mutex()

    data class WordInfoData(val text: String, val displayText: String , val lemma: String) {
        constructor(text: String): this(text, text, text)
    }

    suspend fun getAllWords(prefix: String, db: String?): Collection<WordInfoData>? {
        return dbMutex.withLock {
            if (db == null) return null
            val result: MutableCollection<WordInfoData> = ArrayList()
            val dbHelper = mDatabaseOpenHelperMap[db.trim { it <= ' ' }
                .uppercase(Locale.getDefault())]
            if (dbHelper == null) return null
            val supportsDisplayText = (db  == "german_wiki") || (db  == "german_simple") // NON-NLS
            val supportsLemma = (db  == "german_wiki") || (db  == "german_simple") // NON-NLS

            var col = arrayOf(TEXT_COLUMN)
            if(supportsDisplayText) {
                @Suppress("KotlinConstantConditions")
                col = if(supportsLemma) {
                    arrayOf(TEXT_COLUMN, DISPLAY_TEXT_COLUMN, LEMMA_COLUMN)
                } else {
                    arrayOf(TEXT_COLUMN, DISPLAY_TEXT_COLUMN)
                }
            }

            dbHelper.getOrCreateDataBase().use { sqlite ->
                builder.query(
                    sqlite,
                    col, "prefix = ?", // NON-NLS
                    arrayOf(prefix), null, null, null
                ).use { cursor ->
                    if (cursor == null) {
                        return null
                    } else if (!cursor.moveToFirst()) {
                        return null
                    }
                    do {

                        val text = cursor.getString(0)
                        val displayText = if(supportsDisplayText) cursor.getString(1) else text
                        val lemma = if(supportsLemma) cursor.getString(2) else displayText
                        result.add(WordInfoData(text, displayText, lemma))
                    } while (cursor.moveToNext())
                }
            }
            result
        }
    }

    internal companion object {
        private const val DB_ASSET_PATH = "dicts" // NON-NLS
        private const val TEXT_COLUMN = "text" // NON-NLS
        private const val DISPLAY_TEXT_COLUMN = "display_text" // NON-NLS
        private const val LEMMA_COLUMN = "lemma" // NON-NLS
    }
}