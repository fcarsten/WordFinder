package org.carstenf.wordfinder.fireworks

import android.os.Handler
import android.os.Looper
import androidx.fragment.app.FragmentManager

object FireworksPlayer {

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Displays the firework effect overlay.
     * Can be called from any thread.
     *
     * @param fragmentManager The FragmentManager from the Activity or Fragment context.
     * @param durationSeconds The duration in seconds for the effect to last.
     */
    fun show(fragmentManager: FragmentManager, durationSeconds: Int = 5) {
        // Ensure the dialog is shown on the main thread
        mainHandler.post {
            // Check if a dialog is already showing to prevent duplicates (optional)
            if (fragmentManager.findFragmentByTag(FireworkDialog.TAG) == null) {
                val dialog = FireworkDialog.newInstance(durationSeconds)
                dialog.show(fragmentManager, FireworkDialog.TAG)
            }
        }
    }

    /**
     * Dismisses the firework effect if it's currently showing.
     * Can be called from any thread.
     *
     * @param fragmentManager The FragmentManager from the Activity or Fragment context.
     */
    @Suppress("unused") // NON-NLS
    fun dismiss(fragmentManager: FragmentManager) {
        mainHandler.post {
            val dialog = fragmentManager.findFragmentByTag(FireworkDialog.TAG) as? FireworkDialog
            dialog?.dismissAllowingStateLoss()
        }
    }
}