/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder.dictionary

import java.io.IOException

interface WiktionaryCallback {
    fun onResult(meaning: String?)
    fun onError(e: IOException)
}
