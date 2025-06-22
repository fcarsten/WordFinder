package org.carstenf.wordfinder.fireworks

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.carstenf.wordfinder.R

const val FIREWORK_DISMISS = "firework_dismiss" // NON-NLS

const val FIREWORK_DISMISSED = "firework_dismissed" // NON-NLS

class FireworkDialog() : DialogFragment() {

    private var fireworkView: FireworkView? = null
    private var durationMillis: Long = 5000L // Default duration
    private var dismissJob: Job? = null
    private lateinit var fanfarePlayer: VictoryFanfarePlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve duration argument
        durationMillis = arguments?.getLong(ARG_DURATION, 8000L) ?: 8000L
        // Make the dialog full screen and transparent
        setStyle(STYLE_NORMAL, R.style.FullScreenDialog) // Use a custom style
        fanfarePlayer = VictoryFanfarePlayer(requireContext())
        fanfarePlayer.playFanfare {
            // Optional callback when fanfare completes
            Log.d(TAG, "Fanfare completed") // NON-NLS
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout defined in dialog_firework.xml
        val view = inflater.inflate(R.layout.dialog_firework, container, false)
        fireworkView = view.findViewById(R.id.fireworkView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fireworkView?.startAnimation()

        // Schedule dismissal using coroutines tied to the fragment's lifecycle
        dismissJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(durationMillis)
            dismissAllowingStateLoss() // Use allowingStateLoss if dismissal might happen after state saved
        }
        view.setOnClickListener {
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        setFragmentResult(FIREWORK_DISMISS, bundleOf(FIREWORK_DISMISSED to true))
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        // Ensure the dialog takes up the full screen
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            // Optional: Dim the status/navigation bars if not handled by the theme
            // setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            // Clear flags that might add padding or background dimming handled by our layout
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setBackgroundDrawableResource(android.R.color.transparent) // Ensure no default background
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        dismissJob?.cancel() // Cancel the dismiss coroutine if the view is destroyed
        fireworkView?.stopAnimation() // Ensure animation stops
        fireworkView = null // Avoid memory leaks
    }

    // Call this if you want to stop the fanfare early
    @Suppress("UNUSED")
    private fun stopFanfare() {
        fanfarePlayer.stopFanfare()
    }

    override fun onDestroy() {
        super.onDestroy()
        fanfarePlayer.stopFanfare()
    }

    companion object {
        private const val ARG_DURATION = "duration" // NON-NLS
        const val TAG = "FireworkDialog" // Tag for finding the fragment

        fun newInstance(durationSeconds: Int): FireworkDialog {
            val fragment = FireworkDialog()
            val args = Bundle()
            args.putLong(ARG_DURATION, durationSeconds * 1000L) // Store as milliseconds
            fragment.arguments = args
            return fragment
        }
    }
}