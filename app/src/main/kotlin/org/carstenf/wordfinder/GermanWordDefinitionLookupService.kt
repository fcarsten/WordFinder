package org.carstenf.wordfinder

import java.util.Locale

class GermanWordDefinitionLookupService : WordDefinitionLookupService {
    override fun lookupWordDefinition(gameState: GameState, word: String) {
        val lowercaseWord = word.lowercase(Locale.getDefault())
        val capitalizedWord =
            lowercaseWord[0].uppercaseChar().toString() + lowercaseWord.substring(1)
        val searchTerm = "$capitalizedWord|$lowercaseWord"

        val wiktionaryLookup = WiktionaryLookup()
        wiktionaryLookup.getMeaningAsync(searchTerm) { meaning: String? ->
            if (!meaning.isNullOrBlank()) {
                gameState.processWordLookupResult(
                    WordInfo(
                        word,
                        language,
                        "$word:\n$meaning",
                        null
                    )
                )
            } else {
                gameState.processWordLookupError(
                    word, language,
                    "Definition not found for: $word"
                )
            }
        }
    }

    override val language: String = "D"
}
