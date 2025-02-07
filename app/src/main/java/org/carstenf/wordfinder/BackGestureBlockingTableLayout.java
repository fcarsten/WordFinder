package org.carstenf.wordfinder;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TableLayout;

import java.util.Collections;
import java.util.List;

public class BackGestureBlockingTableLayout extends TableLayout {

    public BackGestureBlockingTableLayout(Context context) {
        super(context);
    }

    public BackGestureBlockingTableLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    final Rect exclusionRect = new Rect();
    final List<Rect> exclusionRects = Collections.singletonList(exclusionRect);

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exclusionRect.set(0, 0, this.getWidth(), this.getHeight());
            // Prevent swipe gestures from being recognized
            this.setSystemGestureExclusionRects(exclusionRects);
        }
    }


}
