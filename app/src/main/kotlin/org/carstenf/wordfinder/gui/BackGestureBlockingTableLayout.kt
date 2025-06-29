package org.carstenf.wordfinder.gui

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.TableLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import kotlin.math.max

class BackGestureBlockingTableLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : TableLayout(context, attrs) {
    private val exclusionRect: Rect = Rect()
    private val exclusionRects: List<Rect> = listOf(exclusionRect)

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exclusionRect[0, 0, this.width] = this.height
            // Prevent swipe gestures from being recognized
            this.systemGestureExclusionRects = exclusionRects
        }
    }

    init {
        // Enable edge-to-edge behavior
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            handleInsets(view, insets)
            WindowInsetsCompat.CONSUMED
        }
    }
    private fun handleInsets(view: View, insets: WindowInsetsCompat) {
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val systemGestures = insets.getInsets(WindowInsetsCompat.Type.systemGestures())
        val paddingLeft = view.paddingLeft    // or paddingStart
        val paddingTop = view.paddingTop
        val paddingRight = view.paddingRight  // or paddingEnd
        val paddingBottom = view.paddingBottom

        if (systemBars.right > 0 || systemGestures.left > 0) {
            updatePadding(
                left = max(paddingLeft, systemGestures.left),
                top = paddingTop,
                right = max(paddingRight, systemGestures.right),
                bottom = paddingBottom
            )
        }
    }

}