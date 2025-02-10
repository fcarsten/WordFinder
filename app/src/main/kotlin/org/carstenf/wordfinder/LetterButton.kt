/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder

import android.widget.Button

internal class LetterButton(val pos: Int, private val button: Button) {
    fun setText(string: String?) {
        button.text = string
    }

    private var pressed = false

    var isEnabled: Boolean
        get() = pressed
        set(b) {
            pressed = b
            button.isPressed = !b
        }

    fun setContentDescription(s: String?) {
        button.contentDescription = s
    }
}
