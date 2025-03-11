/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder

import java.io.IOException
import java.util.Locale

class GermanWordDefinitionLookupService : WordDefinitionLookupService {
    private fun replaceWithUmlauts(input: String): String {
        return input
            .replace("ae", "ä")
            .replace("ue", "ü")
            .replace("oe", "ö")
    }

    private fun replaceWithSz(input: String): String {
        return input
            .replace("ss", "ß")
    }

    override fun lookupWordDefinition(
        lookupManager: WordDefinitionLookupManager,
        task: WordLookupTask
    ) {
        var searchTerm: String
        var word = task.word.text
        var prefix = "$word: "
        if(task.word.text != task.word.displayText) {
            if(task.word.displayText != task.word.lemma){
                word = task.word.text
                prefix = "${task.word.displayText} (von ${task.word.lemma}): "
                searchTerm = task.word.lemma
            } else {
                word = task.word.displayText
                prefix = "${task.word.displayText}: "
                searchTerm = task.word.displayText
            }
        } else {
            val lowercaseWord = task.word.displayText.lowercase(Locale.getDefault())
            val capitalizedWord =
                lowercaseWord[0].uppercaseChar().toString() + lowercaseWord.substring(1)
            searchTerm = "$capitalizedWord|$lowercaseWord"
            searchTerm = searchTerm + "|" + replaceWithUmlauts(searchTerm)
            searchTerm = searchTerm + "|" + replaceWithSz(searchTerm)
        }

        val wiktionaryLookup = WiktionaryLookup()
        wiktionaryLookup.getMeaningAsync(searchTerm, object : WiktionaryCallback {
            override fun onResult(meaning: String?) {
                if (!meaning.isNullOrBlank()) {
                    lookupManager.processWordLookupResult(
                        task,
                        WordInfo(
                            word,
                            language,
                            "${prefix}\n$meaning",
                            null
                        )
                    )
                } else {
                    lookupManager.processWordLookupError(
                        task, language,
                        "Definition not found for: ${task.word}"
                    )
                }
            }

            override fun onError(e: IOException) {
                lookupManager.processWordLookupError(
                    task, language,
                    "Lookup error for ${task.word}: ${e.message}"
                )
            }
        })

    }

    override val language: String = "D"
}
