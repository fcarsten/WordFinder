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
        val lowercaseWord = task.word.lowercase(Locale.getDefault())
        val capitalizedWord =
            lowercaseWord[0].uppercaseChar().toString() + lowercaseWord.substring(1)
        var searchTerm = "$capitalizedWord|$lowercaseWord"
        searchTerm = searchTerm + "|" + replaceWithUmlauts(searchTerm)
        searchTerm = searchTerm + "|" + replaceWithSz(searchTerm)

        val wiktionaryLookup = WiktionaryLookup()
        wiktionaryLookup.getMeaningAsync(searchTerm, object : WiktionaryCallback {
            override fun onResult(meaning: String?) {
                if (!meaning.isNullOrBlank()) {
                    lookupManager.processWordLookupResult(
                        task,
                        WordInfo(
                            task.word,
                            language,
                            "${task.word}:\n$meaning",
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
