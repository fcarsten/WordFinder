package org.carstenf.wordfinder

fun pickRandomLetterDice(fieldNumber: Int, countryCode: LANGUAGE): Char {
    val r = Math.random()
    val side = (r*6).toInt()
    return when (countryCode) {
        LANGUAGE.EN -> englishDice[fieldNumber][side]
        LANGUAGE.DE -> germanDice[fieldNumber][side]
    }
}

val englishDice = arrayOf(
    charArrayOf('a', 'a', 'c', 'd', 'm', 'r'),
    charArrayOf('a', 'b', 'i', 'r', 'u', 'v'),
    charArrayOf('a', 'c', 'i', 'p', 's', 'x'),
    charArrayOf('a', 'e', 'f', 'o', 'q', 't'),
    charArrayOf('a', 'e', 'i', 'n', 'r', 's'),
    charArrayOf('a', 'e', 'l', 'p', 't', 'v'),
    charArrayOf('b', 'e', 'l', 'n', 'o', 'p'),
    charArrayOf('c', 'e', 'i', 'o', 't', 'w'),
    charArrayOf('d', 'd', 'e', 'n', 'o', 't'),
    charArrayOf('e', 'e', 'h', 's', 't', 'z'),
    charArrayOf('e', 'h', 'k', 'l', 'o', 't'),
    charArrayOf('e', 'h', 's', 's', 's', 'y'),
    charArrayOf('f', 'g', 'g', 'r', 'r', 's'),
    charArrayOf('i', 'i', 'i', 'n', 'u', 'u'),
    charArrayOf('j', 'n', 'n', 'n', 't', 'y'),
    charArrayOf('l', 'm', 'o', 'r', 's', 'w')
)


private val germanDice = arrayOf(
    charArrayOf('a', 'b', 'e', 'o', 'r', 'z'),
    charArrayOf('a', 'd', 'f', 's', 't', 'u'),
    charArrayOf('a', 'e', 'e', 'g', 'm', 'r'),
    charArrayOf('a', 'e', 'e', 'i', 'l', 'w'),
    charArrayOf('a', 'e', 'g', 'l', 'p', 's'),
    charArrayOf('a', 'e', 'n', 'q', 'r', 'y'),
    charArrayOf('a', 'h', 'k', 'k', 's', 't'),
    charArrayOf('b', 'c', 'd', 'e', 't', 'u'),
    charArrayOf('b', 'd', 'e', 'g', 'h', 't'),
    charArrayOf('c', 'f', 'h', 'h', 'n', 'u'),
    charArrayOf('c', 'i', 'j', 'l', 's', 't'),
    charArrayOf('e', 'e', 'i', 'k', 'n', 'n'),
    charArrayOf('e', 'i', 'l', 'n', 'r', 'u'),
    charArrayOf('e', 'k', 'n', 'r', 'r', 's'),
    charArrayOf('h', 'n', 'r', 't', 'v', 'x'),
    charArrayOf('i', 'm', 'n', 'o', 'o', 'w')
)