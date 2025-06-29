/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder.dictionary

class WordInfo(
    val word: String,
    private val language: String,
    var wordDefinition: String?,
    var referenceUrl: String?
) {
    val key: String get() = "$word|$language"
}