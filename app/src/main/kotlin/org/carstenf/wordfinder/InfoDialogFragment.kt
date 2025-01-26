package org.carstenf.wordfinder
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class InfoDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Info")
        builder.setPositiveButton(R.string.OK) { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()

        val infoText = getString(R.string.InfoText)
        val versionText = infoText.replace("X.X", BuildConfig.VERSION_NAME)

        // Use the newer Html.fromHtml method with FROM_HTML_MODE_LEGACY
        val markup = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(versionText, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(versionText) // Fallback for older versions
        }

        val textView = TextView(requireContext()).apply {
            movementMethod = LinkMovementMethod.getInstance()
            text = markup
            linksClickable = true
        }

        val margin = resources.getDimensionPixelSize(R.dimen.dialog_margin)
        dialog.setView(textView, margin, 0, margin, 0)

        return dialog
    }

    companion object {
        const val TAG = "InfoDialogFragment"

        fun showInfo(fragmentManager: androidx.fragment.app.FragmentManager) {
            val dialogFragment = InfoDialogFragment()
            dialogFragment.show(fragmentManager, TAG)
        }
    }
}