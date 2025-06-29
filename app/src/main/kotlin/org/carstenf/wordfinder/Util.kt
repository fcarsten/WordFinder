/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.Insets
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowInsets
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.carstenf.wordfinder.WordFinder.Companion.TAG

// Consider renaming this function to reflect it shows a Dialog now
fun showHyperlinkDialog(
    fragmentManager: FragmentManager,
    definitionStr: String,
    displayTime: Long,
    linkString: String?,
    linkUrl: String?
) {
    val dialogFragment = HyperlinkDialogFragment.newInstance(definitionStr, linkString, linkUrl, displayTime)
    if (!fragmentManager.isStateSaved) {
        dialogFragment.show(fragmentManager, TAG)
    } else {
        Log.w(TAG, "FragmentManager state saved, cannot show HyperlinkDialogFragment.") // NON-NLS
    }
}

/**
 * Shows a dialog with a table using DialogFragment for lifecycle safety.
 *
 * @param fragmentManager The FragmentManager to use for showing the dialog.
 * @param description Text to display above the table.
 * @param tableHeader List of strings for the table header (expects 2 items).
 * @param tableData List of rows, where each row is a list of strings for table cells (expects 2 cells per row).
 * @param displayTime How long the dialog should be displayed in seconds before auto-dismissing.
 */
fun showTableDialog(
    fragmentManager: FragmentManager,
    description: String,
    tableHeader: List<String>,
    tableData: List<List<String>>,
    displayTime: Long
) {
    val dialogFragment = TableDialogFragment.newInstance(
        description,
        tableHeader,
        tableData,
        displayTime
    )
    // It's good practice to check if the fragment manager can still commit transactions
    if (!fragmentManager.isStateSaved) {
        dialogFragment.show(fragmentManager, TAG)
    } else {
        Log.w(TAG, "FragmentManager state saved, cannot show TableDialogFragment.") // NON-NLS
    }
}

fun slideUpAndHide(toHide: View, toReveal: View) {
    val animator = ObjectAnimator.ofFloat(toHide, "translationY", 0f, -toHide.height.toFloat())
    toReveal.visibility = View.VISIBLE
    animator.duration = 200 // Animation duration in milliseconds
    animator.start()

    animator.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            toHide.visibility = View.GONE // Hide the view after animation
            toReveal.visibility= View.VISIBLE
            toHide.translationY = 0f
        }
    })
}

fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return true // Can't really check so assume yes
    }
    val capabilities =
        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    return capabilities != null &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

fun showUnsolvableDialog(app: WordFinder) {
    val builder = AlertDialog.Builder(app)
    builder.setMessage(R.string.board_unsolvable)
        .setTitle(R.string.unsolvable_confirm_title)
        .setPositiveButton(
            R.string.shuffle_ok_text
        ) { _: DialogInterface?, _: Int ->
            if(app.gameState.gameLifecycleState.value != GameState.GameLifeCycleState.GAME_OVER) {
                app.gameState.gameLifecycleState.postValue(GameState.GameLifeCycleState.GAME_OVER)
            }
            app.shuffle()
        } .setNegativeButton(
            R.string.shuffle_cancle_text
        ) { _: DialogInterface?, _: Int ->
            if(app.gameState.gameLifecycleState.value != GameState.GameLifeCycleState.GAME_OVER) {
                app.gameState.gameLifecycleState.postValue(GameState.GameLifeCycleState.GAME_OVER)
            }
        }

    val dialog = builder.create()
    dialog.setCanceledOnTouchOutside(false)
    dialog.show()
}

fun showGameWonDialog(app: WordFinder) {
    val builder = AlertDialog.Builder(app)
    builder.setMessage(R.string.game_won_message)
        .setTitle(R.string.game_won_title)
        .setPositiveButton(
            R.string.shuffle_ok_text
        ) { _: DialogInterface?, _: Int ->
            if(app.gameState.gameLifecycleState.value != GameState.GameLifeCycleState.GAME_OVER) {
                app.gameState.gameLifecycleState.postValue(GameState.GameLifeCycleState.GAME_OVER)
            }
            app.shuffle()
        } .setNegativeButton(
            R.string.shuffle_cancle_text
        ) { _: DialogInterface?, _: Int ->
            if(app.gameState.gameLifecycleState.value != GameState.GameLifeCycleState.GAME_OVER) {
                app.gameState.gameLifecycleState.postValue(GameState.GameLifeCycleState.GAME_OVER)
            }
        }

    val dialog = builder.create()
    dialog.setCanceledOnTouchOutside(false)
    dialog.show()
}

fun parseTime(timeStr: String): Long {
    if (timeStr.contains(":")) {
        val c = timeStr.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var res = c[0].toInt() *60L
        if(c.size>1) {
            res +=  c[1].toInt()
        }
        return 1000 * res
    } else {
        return timeStr.toInt() * 1000L
    }
}


fun showTimeIsUpDialog(app: WordFinder) {
    val builder = AlertDialog.Builder(app)
    builder.setMessage(R.string.time_up_dialog_msg)
        .setTitle(R.string.time_up_dialog_title)
        .setPositiveButton(
            R.string.time_up_dialog_ok
        ) { _: DialogInterface?,
            _: Int ->
            if(app.gameState.gameLifecycleState.value != GameState.GameLifeCycleState.GAME_OVER) {
                app.gameState.gameLifecycleState.postValue(GameState.GameLifeCycleState.GAME_OVER)
            }
        }

    val dialog = builder.create()
    dialog.setCanceledOnTouchOutside(false)
    dialog.show()
}

fun showConfirmShuffleDialog(app: WordFinder) {
    val builder = AlertDialog.Builder(app)
    builder.setMessage(R.string.shuffle_confirm_msg)
        .setTitle(R.string.shuffle_confirm_title)
        .setPositiveButton(
            R.string.shuffle_ok_text
        ) { _: DialogInterface?, _: Int -> app.shuffle() }
        .setNegativeButton(
            R.string.shuffle_cancle_text
        ) { _: DialogInterface?, _: Int ->
            app.updateButtonEnabledStatus()
        }

    val dialog = builder.create()
    dialog.show()
}

fun showRestartRequiredDialog(app: WordFinder) {
    val builder = AlertDialog.Builder(app)
    builder.setMessage(R.string.shuffle_required_diag_msg)
        .setTitle(R.string.shuffle_required_diag_title)
        .setPositiveButton(
            R.string.shuffle_required_diag_ok
        ) { _: DialogInterface?, _: Int ->
            app.shuffle()
        }

    val dialog = builder.create()
    dialog.setCanceledOnTouchOutside(false)
    dialog.setCancelable(false)
    dialog.show()
}

fun showConfirmStartGameDialog(app: WordFinder) {
    val builder = AlertDialog.Builder(app)
    builder.setMessage(R.string.start_game_diag_msg)
        .setTitle(R.string.start_game_diag_title)
        .setPositiveButton(
            R.string.start_game_diag_ok
        ) { _: DialogInterface?, _: Int ->
            app.showConfirmStartGameDialogVisible = false
            app.shuffle()
        }

    val dialog = builder.create()
    dialog.setCanceledOnTouchOutside(false)
    dialog.setCancelable(false)
    app.showConfirmStartGameDialogVisible = true
    dialog.show()
}

fun isGestureNavigationEnabled(app: WordFinder): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val insets = app.window.decorView.rootWindowInsets ?: return false
        val gestureInsets: Insets = insets.getInsets(WindowInsets.Type.systemGestures())

        // If left or right insets are greater than zero, gesture navigation is enabled
        return gestureInsets.left > 0 || gestureInsets.right > 0
    }
    return false // Assume older versions use 3-button navigation
}

// Helper method to find the button at a specific position
private fun findButtonAtPosition(
    tableLayout: TableLayout,
    x: Int,
    y: Int,
    action: Int
): Button? {
    var touchAreaPercent = 0f

    when (action) {
        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP -> touchAreaPercent = 1f
        MotionEvent.ACTION_MOVE -> touchAreaPercent = 0.6f
    }

    for (i in 0 until tableLayout.childCount) {
        val child = tableLayout.getChildAt(i)
        if (child is TableRow) {
            for (j in 0 until child.childCount) {
                val view = child.getChildAt(j)
                if (view is Button) {
                    val location = IntArray(2)
                    view.getLocationOnScreen(location)
                    val buttonLeft = location[0]
                    val buttonTop = location[1]

                    val buttonWidth = view.width
                    val buttonHeight = view.height

                    val buttonRight = buttonLeft + buttonWidth
                    val buttonBottom = buttonTop + buttonHeight

                    val deltaWidth = buttonWidth * (1 - touchAreaPercent) / 2
                    val deltaHeight = buttonHeight * (1 - touchAreaPercent) / 2

                    // Check if the touch position is within the button's bounds
                    if (x >= buttonLeft + deltaWidth && x <= buttonRight - deltaWidth && y >= buttonTop + deltaHeight && y <= buttonBottom - deltaHeight) {
                        return view
                    }
                }
            }
        }
    }
    return null
}

@SuppressLint("ClickableViewAccessibility")
fun addGestureHandler(app: WordFinder, tableLayout: TableLayout) {
    // Iterate through all buttons in the TableLayout

    for (i in 0 until tableLayout.childCount) {
        val child = tableLayout.getChildAt(i)
        if (child is TableRow) {
            for (j in 0 until child.childCount) {
                val view = child.getChildAt(j)
                if (view is Button) {
                    // Attach the OnTouchListener to each button
                    view.setOnClickListener { app.onLetterClick(view) }

                    view.setOnTouchListener(object : OnTouchListener {
                        private var firstButtonPressed: Button? = null
                        private var lastButtonPressed: Button? = null

                        override fun onTouch(v: View, event: MotionEvent): Boolean {
                            if(app.isGameOver()) return true

                            val action = event.action
                            var x = event.x.toInt()
                            var y = event.y.toInt()

                            // Convert the touch coordinates to screen coordinates
                            val location = IntArray(2)
                            v.getLocationOnScreen(location)
                            x += location[0]
                            y += location[1]

                            // Find the button at the current touch position
                            val button = findButtonAtPosition(tableLayout, x, y, action)

                            if (button != null) {
                                when (action) {
                                    MotionEvent.ACTION_DOWN -> {
                                        firstButtonPressed = button
                                        if (button !== lastButtonPressed) {
                                            if (button.hasOnClickListeners()) {
                                                button.callOnClick()
                                            }
                                            lastButtonPressed = button
                                        }
                                    }

                                    MotionEvent.ACTION_MOVE -> if (button !== lastButtonPressed) {
                                        if (button.hasOnClickListeners()) {
                                            button.callOnClick()
                                        }
                                        lastButtonPressed = button
                                    }

                                    MotionEvent.ACTION_UP -> {
                                        if (firstButtonPressed !== lastButtonPressed)
                                            app.lifecycleScope.launch {
                                                app.okClick()
                                            }
                                        lastButtonPressed = null
                                    }
                                }
                            }

                            return true
                        }
                    })
                }
            }
        }
    }
}

