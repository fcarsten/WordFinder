/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder.util

import org.carstenf.wordfinder.dictionary.Dictionary


/**
 * @author carsten.friedrich@gmail.com
 */
class Result(val result: Dictionary.WordInfoData) {
    var isHighlighted: Boolean = false

    override fun toString(): String {
        return result.displayText
    }
}
