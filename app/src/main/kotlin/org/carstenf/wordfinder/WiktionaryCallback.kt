package org.carstenf.wordfinder

fun interface WiktionaryCallback {
    fun onResult(meaning: String?)
}
