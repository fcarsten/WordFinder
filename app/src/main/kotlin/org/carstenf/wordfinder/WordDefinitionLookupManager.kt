package org.carstenf.wordfinder

import android.app.Activity
import android.view.View
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

class WordDefinitionLookupManager(private val app: Activity, private val gameState: GameState)  {
    val wordLookupTaskMap by lazy { HashMap<Long, View>() }
    private val wordLookupTaskCounter = AtomicLong(0)
    val wordLookupResult: MutableLiveData<Pair<WordLookupTask, WordInfo?>> = MutableLiveData(null)
    val wordLookupError: MutableLiveData<Pair<WordLookupTask, String?>> = MutableLiveData(null)

    fun processWordLookupError(task: WordLookupTask, language: String, error: String?) {
        wordInfoCache.put(WordInfo(task.word.displayText, language, null, null))
        wordLookupError.postValue(Pair(task, error))
    }

    fun processWordLookupResult(task: WordLookupTask, wordInfo: WordInfo) {
        wordInfoCache.put(wordInfo)
        wordLookupResult.postValue(Pair(task, wordInfo))
    }


    private fun countWords(input: String): Int {
        if (input.isBlank()) return 0 // Return 0 if the string is empty or contains only whitespace

        // Split the string by whitespace and filter out any empty strings (caused by multiple spaces)
        val words = input.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }

        return words.size
    }

    fun displayWordDefinition(wordInfo: WordInfo) {
        val wordDef = wordInfo.wordDefinition
        if (wordDef.isNullOrBlank()) return

        app.runOnUiThread {
            val numWords = countWords(wordDef)
            val displayTime = 3 + (numWords * 60.0)/200

            val view = app.findViewById<View>(android.R.id.content)
            val url = wordInfo.referenceUrl
            if(url == null) {
                showSnackbar(view, wordDef, displayTime.toLong())
            } else {
                showHyperlinkSnackbar(view, wordDef, displayTime.toLong(),
                    wordInfo.word, url )
            }
        }
    }

    fun wordDefinitionLookup(selectedItem: Result, progressBarView: View?) {

        val selectedWord = selectedItem.result

        val lookupService = getWordDefinitionLookupService(gameState.dictionaryName!!)

        if (lookupService == null) {
            app.runOnUiThread {
                Toast.makeText(
                    app,
                    R.string.word_definition_lookup_not_supported_for_this_dictionary,
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            val wordInfo = getWordInfoFromCache(
                selectedWord.displayText,
                lookupService.language
            )

            if (wordInfo != null) {
                val wordDefinition = wordInfo.wordDefinition
                if (wordDefinition.isNullOrBlank()) {
                    app.runOnUiThread {
                        Toast.makeText(app, app.getString(R.string.definition_not_found_for) + " ${selectedWord.displayText}"
                        , Toast.LENGTH_SHORT).show()
                    }
                } else {
                    displayWordDefinition(wordInfo)
                }
            } else {
                if (isNetworkAvailable(app.applicationContext)) {
                    val lookupTaskCounter = wordLookupTaskCounter.incrementAndGet()
                    if (progressBarView != null) {
                        wordLookupTaskMap[lookupTaskCounter] = progressBarView
                        progressBarView.visibility = View.VISIBLE
                    }
                    lookupService.lookupWordDefinition(
                        this,
                        WordLookupTask(lookupTaskCounter, selectedWord)
                    )
                } else {
                    app.runOnUiThread {
                        Toast.makeText(
                            app,
                            R.string.no_internet_connection_available,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun getWordDefinitionLookupService(dictionaryName: String): WordDefinitionLookupService? {
        return when (dictionaryName.lowercase(Locale.getDefault())) {
            "int_english", "2of12inf" -> EnglishWordDefinitionLookupService()
            "german", "german_simple", "german_wiki" -> GermanWordDefinitionLookupService()
            else -> null
        }
    }
    companion object {
        private val wordInfoCache: WordInfoCache = WordInfoCache()

        fun getWordInfoFromCache(word: String, language: String): WordInfo? {
            return wordInfoCache.get(word, language)
        }

    }

}