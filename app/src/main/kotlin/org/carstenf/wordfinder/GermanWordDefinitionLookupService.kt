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
            .replace("ae", "ä") // NON-NLS
            .replace("ue", "ü") // NON-NLS
            .replace("oe", "ö") // NON-NLS
    }

    private fun replaceWithSz(input: String): String {
        return input
            .replace("ss", "ß") // NON-NLS
    }

    override fun lookupWordDefinition(
        lookupManager: WordDefinitionLookupManager,
        task: WordLookupTask
    ) {
        var searchTerm: String
        var prefix = "${task.word.text}: "
        var url = "https://de.wiktionary.org/wiki/${task.word.displayText}"

        if(task.word.text != task.word.displayText) {
            if(task.word.displayText != task.word.lemma){
                prefix = "${task.word.displayText} (von ${task.word.lemma}): " // NON-NLS
                searchTerm = task.word.lemma
            } else {
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
            url = "https://de.wiktionary.org/w/index.php?title=Special:Search&search=${task.word.displayText}"
        }

        val wiktionaryLookup = WiktionaryLookup()
        wiktionaryLookup.getMeaningAsync(searchTerm, object : WiktionaryCallback {
            override fun onResult(meaning: String?) {
                if (!meaning.isNullOrBlank()) {
                    var meaningClean = meaning
                    if(! meaning.contains("[2]")) { // No need to make a list if only 1 element
                        meaningClean = meaning.replace("\\[1]\\s*".toRegex(), "") // NON-NLS
                    }

                    lookupManager.processWordLookupResult(
                        task,
                        WordInfo(
                            task.word.displayText,
                            language,
                            "${prefix}\n$meaningClean", url)
                    )
                } else {
                    lookupManager.processWordLookupResult(
                        task,
                        WordInfo(
                            task.word.displayText,
                            language, null, url)
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

    override val language: String = "D" // NON-NLS
}
