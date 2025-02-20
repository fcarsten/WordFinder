package org.carstenf.wordfinder

class WordInfo(
    val word: String,
    private val language: String,
    var wordDefinition: String?,
    @Suppress("unused")
    var referenceUrl: String?
) {
    val key: String get() = "$word|$language"
}