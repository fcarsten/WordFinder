package org.carstenf.wordfinder

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton

class AppCompatLetterButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.buttonStyle
) : AppCompatButton(context, attrs, defStyleAttr) {

    private var _isChecked = false
    var isChecked: Boolean
        get() = _isChecked
        set(value) {
            _isChecked = value
            refreshDrawableState()
        }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (isChecked) {
            mergeDrawableStates(drawableState, intArrayOf(android.R.attr.state_checked))
        }
        return drawableState
    }
}