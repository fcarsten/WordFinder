/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder

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

    override fun lookupWordDefinition(gameState: GameState, task: WordLookupTask) {
        val lowercaseWord = task.word.lowercase(Locale.getDefault())
        val capitalizedWord =
            lowercaseWord[0].uppercaseChar().toString() + lowercaseWord.substring(1)
        var searchTerm = "$capitalizedWord|$lowercaseWord"
        searchTerm = searchTerm+"|"+ replaceWithUmlauts(searchTerm)
        searchTerm = searchTerm+"|"+ replaceWithSz(searchTerm)

        val wiktionaryLookup = WiktionaryLookup()
        wiktionaryLookup.getMeaningAsync(searchTerm) { meaning: String? ->
            if (!meaning.isNullOrBlank()) {
                gameState.processWordLookupResult(
                    task,
                    WordInfo(
                        task.word,
                        language,
                        "${task.word}:\n$meaning",
                        null
                    )
                )
            } else {
                gameState.processWordLookupError(
                    task, language,
                    "Definition not found for: ${task.word}"
                )
            }
        }
    }

    override val language: String = "D"
}
