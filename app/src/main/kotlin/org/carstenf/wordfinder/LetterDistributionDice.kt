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
    charArrayOf('a', 'd', 'f', 'h', 'n', 't'),
    charArrayOf('a', 'd', 'l', 'r', 's', 's'),
    charArrayOf('a', 'e', 'l', 'o', 's', 'u'),
    charArrayOf('a', 'e', 'n', 'o', 'q', 'v'),
    charArrayOf('a', 'g', 'l', 'o', 'o', 'y'),
    charArrayOf('b', 'c', 'e', 'i', 'm', 'z'),
    charArrayOf('b', 'c', 'e', 'n', 's', 't'),
    charArrayOf('c', 'l', 'r', 's', 't', 't'),
    charArrayOf('d', 'i', 'p', 'r', 't', 't'),
    charArrayOf('e', 'e', 'h', 'h', 'i', 'y'),
    charArrayOf('e', 'f', 'n', 'p', 'r', 'u'),
    charArrayOf('e', 'i', 'j', 'n', 'o', 't'),
    charArrayOf('e', 'n', 'o', 'r', 's', 's'),
    charArrayOf('e', 'r', 't', 'v', 'w', 'x'),
    charArrayOf('g', 'i', 'r', 's', 't', 'w'),
    charArrayOf('k', 'm', 'r', 's', 's', 'u')
)

private val germanDice = arrayOf(
    charArrayOf('a', 'a', 'h', 'n', 'n', 'y'),
    charArrayOf('a', 'a', 'm', 'o', 's', 's'),
    charArrayOf('a', 'c', 'e', 'n', 'u', 'u'),
    charArrayOf('a', 'd', 'i', 'i', 'k', 'r'),
    charArrayOf('b', 'c', 'i', 'o', 'r', 'w'),
    charArrayOf('b', 'e', 'e', 'e', 'r', 'v'),
    charArrayOf('b', 'e', 'e', 'h', 'n', 'w'),
    charArrayOf('c', 'e', 'h', 'l', 'n', 'n'),
    charArrayOf('d', 'e', 'e', 'p', 't', 't'),
    charArrayOf('d', 'f', 'i', 's', 't', 'u'),
    charArrayOf('e', 'e', 'e', 'l', 'r', 'x'),
    charArrayOf('e', 'g', 'h', 'r', 's', 'z'),
    charArrayOf('e', 'k', 'r', 's', 't', 'u'),
    charArrayOf('f', 'i', 'l', 'm', 't', 'v'),
    charArrayOf('g', 'g', 'h', 'k', 's', 's'),
    charArrayOf('j', 'l', 'q', 'r', 't', 'u'),
)