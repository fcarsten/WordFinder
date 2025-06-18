package org.carstenf.wordfinder

import android.content.Intent
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.net.toUri
import com.google.android.material.snackbar.Snackbar
import org.carstenf.wordfinder.WordFinder.Companion.TAG

fun showSnackbar(view: View, definitionStr: String, displayTime: Long) {
    val snackbar = Snackbar.make(view, definitionStr, Snackbar.LENGTH_INDEFINITE)
    snackbar.setAction("Ok") {
        // Dismiss the Snackbar when the action button is clicked
        snackbar.dismiss()
    }

    val snackbarView = snackbar.view

    val textView =
        snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
    if (textView != null) {
        textView.maxLines = 10
    } else {
        Log.e(
            TAG,
            "TextView not found in Snackbar view to adjust number of lines"
        )
    }

    val params = snackbarView.layoutParams
    params.width = ViewGroup.LayoutParams.WRAP_CONTENT // Wrap the width to text size
    params.height = ViewGroup.LayoutParams.WRAP_CONTENT // Optional: Wrap height
    snackbarView.layoutParams = params

    val layoutParams = snackbarView.layoutParams as FrameLayout.LayoutParams
    layoutParams.gravity = Gravity.CENTER // Adjust gravity if needed
    snackbarView.layoutParams = layoutParams
    snackbar.show()

    Handler(Looper.getMainLooper()).postDelayed({
        snackbar.dismiss()
    }, (displayTime*1000L))

}

fun createTable(data: List<List<String>>): String {
    val columnWidths = IntArray(data[0].size) { 0 }

    // Calculate max width for each column
    data.forEach { row ->
        row.forEachIndexed { index, cell ->
            if (cell.length > columnWidths[index]) {
                columnWidths[index] = cell.length
            }
        }
    }

    val builder = StringBuilder()
    data.forEach { row ->
        row.forEachIndexed { index, cell ->
            builder.append(cell.padEnd(columnWidths[index] + 2))
        }
        builder.append("\n")
    }

    return builder.toString()
}

fun showMapSnackbar(view: View, description: String,
                    tableData: List<List<String>>, displayTime: Long) {
    val snackbar = Snackbar.make(view, description, Snackbar.LENGTH_INDEFINITE)
    snackbar.setAction("Ok") {
        // Dismiss the Snackbar when the action button is clicked
        snackbar.dismiss()
    }

    val snackbarView = snackbar.view

    val textView =
        snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
    if (textView != null) {
        textView.text = createTable(tableData)
        textView.typeface = Typeface.MONOSPACE
        textView.maxLines = 10
    } else {
        Log.e(
            TAG,
            "TextView not found in Snackbar view to adjust number of lines"
        )
    }

    val params = snackbarView.layoutParams
    params.width = ViewGroup.LayoutParams.WRAP_CONTENT // Wrap the width to text size
    params.height = ViewGroup.LayoutParams.WRAP_CONTENT // Optional: Wrap height
    snackbarView.layoutParams = params

    val layoutParams = snackbarView.layoutParams as FrameLayout.LayoutParams
    layoutParams.gravity = Gravity.CENTER // Adjust gravity if needed
    snackbarView.layoutParams = layoutParams
    snackbar.show()

    Handler(Looper.getMainLooper()).postDelayed({
        snackbar.dismiss()
    }, (displayTime*1000L))

}

fun showHyperlinkSnackbar(view: View,  definitionStr: String, displayTime: Long,
                          linkString: String, linkUrl: String) {
    // Create a Snackbar with empty text
    val snackbar = Snackbar.make(view, "", Snackbar.LENGTH_INDEFINITE)

    // Get the Snackbar layout and remove padding
    val snackbarLayout = snackbar.view as ViewGroup
    snackbarLayout.setPadding(0, 0, 0, 0)

    // Inflate custom layout
    val snackView = LayoutInflater.from(view.context).inflate(R.layout.hyperlink_snackbar,
        snackbarLayout, false)
    val snackbarText = snackView.findViewById<TextView>(R.id.snackbar_text)
    val snackbarAction = snackView.findViewById<TextView>(R.id.snackbar_action)

    // Create a SpannableString with a clickable part
    val spannableString = SpannableString(definitionStr)

    val clickableSpan = object : ClickableSpan() {
        override fun onClick(widget: View) {
            val intent = Intent(Intent.ACTION_VIEW, linkUrl.toUri())
            widget.context.startActivity(intent)
        }
    }

    // Set "Click here" as clickable
    val start = definitionStr.indexOf(linkString)
    val end = start + linkString.length

    if(start>=0 && end<=definitionStr.length) {
        spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    // Apply to TextView
    snackbarText.text = spannableString
    snackbarText.movementMethod = LinkMovementMethod.getInstance()

    // Handle Snackbar action button click (like setAction)
    snackbarAction.setOnClickListener {
        snackbar.dismiss()
    }

    // Add custom view to Snackbar
    snackbarLayout.addView(snackView, 0)
//    snackbarLayout.removeViewAt(1)

    val layoutParams = snackbarLayout.layoutParams as FrameLayout.LayoutParams
    layoutParams.gravity = Gravity.CENTER // Adjust gravity if needed
    snackbarLayout.layoutParams = layoutParams
    snackbar.show()

    Handler(Looper.getMainLooper()).postDelayed({
        snackbar.dismiss()
    }, (displayTime*1000L))
}
