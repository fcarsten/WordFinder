/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder

internal class LetterButton(val pos: Int, private val button: AppCompatLetterButton) {
    fun setText(string: String?) {
        button.text = string
    }

    private var checked = false

    var isChecked: Boolean
        get() = checked
        set(b) {
            checked = b
            button.isChecked = !b
        }

    fun setContentDescription(s: String?) {
        button.contentDescription = s
    }
}
