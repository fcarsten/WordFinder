package org.carstenf.wordfinder

class WordInfo(
    val word: String,
    private val language: String,
    var wordDefinition: String?,
    @Suppress("unused")
    var referenceUrl: String?
) {
    // Ensure that 'word' and 'language' are never null
    init {
        require(word.isNotEmpty()) { "Word cannot be empty" }
        require(language.isNotEmpty()) { "Language cannot be empty" }
    }

    val key: String get() = "$word|$language"
}