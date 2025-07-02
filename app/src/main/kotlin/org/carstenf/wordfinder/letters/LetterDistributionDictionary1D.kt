package org.carstenf.wordfinder.letters

fun pickRandomLetterDictionary(countryCode: LANGUAGE): Char {
    val r = Math.random()
    var i = 0

    var letterFreqProb = letterFreqProbEnglishDictionary

    if (countryCode == LANGUAGE.DE) {
        letterFreqProb = letterFreqProbGermanDictionary
    }

    while (letterFreqProb[i] < r) i++

    return ('A'.code + i).toChar()
}


//
// English letter frequencies: http://en.wikipedia.org/wiki/Letter_frequency
//
// a 8.167%
// b 1.492%
// c 2.782%
// d 4.253%
// e 12.702%
// f 2.228%
// g 2.015%
// h 6.094%
// i 6.966%
// j 0.153%
// k 0.772%
// l 4.025%
// m 2.406%
// n 6.749%
// o 7.507%
// p 1.929%
// q 0.095%
// r 5.987%
// s 6.327%
// t 9.056%
// u 2.758%
// v 0.978%
// w 2.360%
// x 0.150%
// y 1.974%
// z 0.074%
// Source: https://en.wikipedia.org/wiki/Letter_frequency
private val letterFreqProbEnglishDictionary = doubleArrayOf(
    0.07, // a 8
    0.09, // b 2
    0.12, // c 3
    0.16, // d 4
    0.27, // e 11
    0.29, // f 2
    0.32, // g 3
    0.34, // h 2
    0.43, // i 9
    0.44, // j 1
    0.45, // k 1
    0.49, // l 4
    0.52, // m 3
    0.59, // n 6
    0.66, // o 7
    0.69, // p 3
    0.70, // q 1
    0.76, // r 6
    0.81, // s 5
    0.88, // t 7
    0.92, // u 4
    0.94, // v 2
    0.96, // w 2
    0.97, // x 1
    0.99, // y 2
    1.0   // z 1
)

private val letterFreqProbGermanDictionary = doubleArrayOf(
    0.06, // a = 6
    0.08, // b = 2
    0.11, // c = 3
    0.16, // d = 5
    0.31, // e = 15
    0.33, // f = 2
    0.36, // g = 3
    0.41, // h = 5
    0.47, // i = 6
    0.48, // j = 1
    0.50, // k = 2
    0.53, // l = 3
    0.57, // m = 4
    0.66, // n = 9
    0.68, // o = 2
    0.69, // p = 1
    0.70, // q = 1
    0.76, // r = 6
    0.83, // s = 7
    0.89, // t = 6
    0.95, // u = 6
    0.96, // v = 1
    0.97, // w = 1
    0.98, // x = 1
    0.99, // y = 1
    1.0   // z = 1
)
