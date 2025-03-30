package org.carstenf.wordfinder

import kotlin.math.pow


enum class LANGUAGE {
    EN,
    DE
}

fun pickRandomLetter(letterRandomDist: GameState.LETTER_RANDOM_DIST, letterCounts: IntArray, languageCode: LANGUAGE): Char {
    return when (letterRandomDist) {
        GameState.LETTER_RANDOM_DIST.UNIFORM -> pickRandomLetterUniformRandom()
        GameState.LETTER_RANDOM_DIST.LETTER_FREQUENCY -> pickRandomLetterDictionary(languageCode)
        GameState.LETTER_RANDOM_DIST.MULTI_LETTER_FREQUENCY -> pickRandomLetter2DDistributionF(letterCounts, languageCode) {
                x : Int -> x.toDouble().pow(0.75).toInt()
        }
    }
}

fun pickRandomLetterUniformRandom(): Char {
    return ('A'.code + (Math.random()*26).toInt()).toChar()
}
