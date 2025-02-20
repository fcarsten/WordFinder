package org.carstenf.wordfinder

import java.util.Locale

class GermanWordDefinitionLookupService : WordDefinitionLookupService {
    override fun lookupWordDefinition(gameState: GameState, task: WordLookupTask) {
        val lowercaseWord = task.word.lowercase(Locale.getDefault())
        val capitalizedWord =
            lowercaseWord[0].uppercaseChar().toString() + lowercaseWord.substring(1)
        val searchTerm = "$capitalizedWord|$lowercaseWord"

        val wiktionaryLookup = WiktionaryLookup()
        wiktionaryLookup.getMeaningAsync(searchTerm) { meaning: String? ->
            if (!meaning.isNullOrBlank()) {
                gameState.processWordLookupResult(
                    task,
                    WordInfo(
                        task.word,
                        language,
                        task.word+":\n$meaning",
                        null
                    )
                )
            } else {
                gameState.processWordLookupError(
                    task, language,
                    "Definition not found for: "+ task.word
                )
            }
        }
    }

    override val language: String = "D"
}
