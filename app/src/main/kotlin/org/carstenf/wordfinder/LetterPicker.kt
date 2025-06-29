package org.carstenf.wordfinder

import org.carstenf.wordfinder.dictionary.pickRandomLetter2DDistributionF
import org.carstenf.wordfinder.dictionary.pickRandomLetterDice
import org.carstenf.wordfinder.dictionary.pickRandomLetterDictionary
import kotlin.math.pow


enum class LANGUAGE {
    EN,
    DE
}

fun pickRandomLetter(letterRandomDist: GameState.LETTER_RANDOM_DIST, letterCounts: IntArray,
                     languageCode: LANGUAGE, fieldNumber: Int): Char {
    return when (letterRandomDist) {
        GameState.LETTER_RANDOM_DIST.UNIFORM -> pickRandomLetterUniformRandom()
        GameState.LETTER_RANDOM_DIST.LETTER_FREQUENCY -> pickRandomLetterDictionary(languageCode)
        GameState.LETTER_RANDOM_DIST.MULTI_LETTER_FREQUENCY -> pickRandomLetter2DDistributionF(
            letterCounts,
            languageCode
        ) { x: Int ->
            x.toDouble().pow(0.75).toInt()
        }

        GameState.LETTER_RANDOM_DIST.LETTER_DICE -> pickRandomLetterDice(fieldNumber, languageCode)
    }
}

fun pickRandomLetterUniformRandom(): Char {
    return ('A'.code + (Math.random()*26).toInt()).toChar()
}
