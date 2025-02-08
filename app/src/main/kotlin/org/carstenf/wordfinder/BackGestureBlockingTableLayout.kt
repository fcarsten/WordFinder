package org.carstenf.wordfinder

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.widget.TableLayout

class BackGestureBlockingTableLayout : TableLayout {
    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

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
}
