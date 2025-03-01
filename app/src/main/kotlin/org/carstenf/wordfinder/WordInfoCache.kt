/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder

class WordInfoCache {
    private val cache: MutableMap<String, WordInfo> = mutableMapOf()

    // Add or update a WordInfo object in the cache
    fun put(wordInfo: WordInfo) {
        cache[wordInfo.key] = wordInfo
    }

    // Retrieve a WordInfo object by word and language
    fun get(word: String, language: String): WordInfo? {
        val key = "$word|$language"
        return cache[key]
    }

    // Remove a WordInfo object by word and language
    @Suppress("unused")
    fun remove(word: String, language: String) {
        val key = "$word|$language"
        cache.remove(key)
    }

    // Clear the cache
    @Suppress("unused")
    fun clear() {
        cache.clear()
    }

    // Get all cached WordInfo objects (for debugging or inspection)
    @Suppress("unused")
    fun getAll(): List<WordInfo> {
        return cache.values.toList()
    }
}