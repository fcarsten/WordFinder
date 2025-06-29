package org.carstenf.wordfinder

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment

class HyperlinkDialogFragment : DialogFragment() {

    private var displayTime: Long = 0L
    private var dialogDismissHandler: Handler? = null
    private var dismissRunnable: Runnable? = null
    private var definitionStr: String? = null
    private var linkString: String? = null
    private var linkUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            definitionStr = it.getString(ARG_DEFINITION_STR)
            linkString = it.getString(ARG_LINK_STRING)
            linkUrl = it.getString(ARG_LINK_URL)
            displayTime = it.getLong(ARG_DISPLAY_TIME)
        }
        dialogDismissHandler = Handler(Looper.getMainLooper())
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.hyperlink_snackbar, container, false)
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        val dialogText =
            view?.findViewById<TextView>(R.id.snackbar_text) // Ensure this ID exists in your new dialog layout
        val dialogAction =
            view?.findViewById<TextView>(R.id.snackbar_action) // Ensure this ID exists

        // Create a SpannableString with a clickable part
        val currentDefinitionStr = definitionStr ?: ""
        val spannableString = SpannableString(currentDefinitionStr)
        val currentLinkUrl = linkUrl
        val currentLinkString = linkString

        if (currentLinkUrl != null && currentLinkString != null) {
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(Intent.ACTION_VIEW, currentLinkUrl.toUri())
                    widget.context.startActivity(intent)
                    dismiss() // Dismiss dialog after click
                }
            }

            // Set linkString as clickable
            val start = currentDefinitionStr.indexOf(currentLinkString)
            val end = start + currentLinkString.length

            if (start >= 0 && end <= currentDefinitionStr.length) {
                spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        // Apply to TextView
        dialogText?.text = spannableString
        dialogText?.movementMethod = LinkMovementMethod.getInstance()

        // Handle Dialog action button click
        dialogAction?.setOnClickListener {
            dismissRunnable?.let { dialogDismissHandler?.removeCallbacks(it) }
            dismissAllowingStateLoss()
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (requireContext().resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        if (displayTime > 0) {
            dismissRunnable = Runnable {
                if (isAdded && dialog?.isShowing == true) {
                    try {
                        dismissAllowingStateLoss()
                    } catch (e: Exception) {
                        Log.e(WordFinder.TAG, "Error dismissing dialog fragment on timer", e)  // NON-NLS
                    }
                }
            }
            dialogDismissHandler?.postDelayed(dismissRunnable!!, displayTime * 1000L)
        }
    }

    override fun onStop() {
        super.onStop()
        dismissRunnable?.let { dialogDismissHandler?.removeCallbacks(it) }
    }

    companion object {
        private const val ARG_DEFINITION_STR = "definition_str" // NON-NLS
        private const val ARG_LINK_STRING = "link_string" // NON-NLS
        private const val ARG_LINK_URL = "link_url" // NON-NLS
        private const val ARG_DISPLAY_TIME = "display_time" // NON-NLS

        fun newInstance(
            definitionStr: String,
            linkString: String?,
            linkUrl: String?,
            displayTime: Long
        ): HyperlinkDialogFragment {
            val args = Bundle().apply {
                putString(ARG_DEFINITION_STR, definitionStr)
                putString(ARG_LINK_STRING, linkString)
                putString(ARG_LINK_URL, linkUrl)
                putLong(ARG_DISPLAY_TIME, displayTime)

            }
            val fragment = HyperlinkDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}