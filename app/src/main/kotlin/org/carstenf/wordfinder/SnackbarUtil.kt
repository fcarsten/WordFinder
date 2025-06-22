package org.carstenf.wordfinder

import android.content.Intent
import android.os.Build
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
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColorInt
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

val columnWeights = floatArrayOf(0.5f, 0.5f)

private fun addTableRow(tableLayout: TableLayout, rowColor: Int, vararg cells: String) {
    val row = TableRow(tableLayout.context).apply {
        layoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )
        background = ContextCompat.getDrawable(context, R.drawable.cell_border)
    }

    cells.forEachIndexed { index, cellText ->
        TextView(tableLayout.context).apply {
            text = cellText
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setTextAppearance(R.style.TableBodyText)
            } else {
                setTextColor(ResourcesCompat.getColor(resources, R.color.md_theme_onPrimary, null))
            }
            setBackgroundColor(rowColor)
            layoutParams = TableRow.LayoutParams(
                0, // Will be weighted
                TableRow.LayoutParams.WRAP_CONTENT,
                columnWeights[index]
            ).apply {
//                setMargins(4, 4, 4, 4)
                setPadding(8,2,8,2)
            }
            row.addView(this)
        }
    }

    tableLayout.addView(row)
}

private fun addTableHeader(tableLayout: TableLayout, vararg headers: String) {
    val headerBackground = "#202020".toColorInt()
    val headerRow = TableRow(tableLayout.context).apply {
        layoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.WRAP_CONTENT
        )
        background = ContextCompat.getDrawable(context, R.drawable.cell_border)
    }

    headers.forEachIndexed { index, headerText ->
        TextView(tableLayout.context).apply {
            text = headerText
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setTextAppearance(R.style.TableHeaderText)
            }
            setBackgroundColor(headerBackground)
            layoutParams = TableRow.LayoutParams(
                0, // Will be weighted
                TableRow.LayoutParams.WRAP_CONTENT,
                columnWeights[index]
            ).apply {
                setMargins(1, 1, 1, 1)
            }
            headerRow.addView(this)
        }
    }

    tableLayout.addView(headerRow)
}

fun showTableSnackbar(view: View, description: String, tableHeader: List<String>,
                      tableData: List<List<String>>, displayTime: Long) {
    val evenRowColor = "#FFFFFF".toColorInt()
    val oddRowColor = "#B5B5B5".toColorInt()

    // Create a Snackbar with empty text
    val snackbar = Snackbar.make(view, "", Snackbar.LENGTH_INDEFINITE)

    // Get the Snackbar layout and remove padding
    val snackbarLayout = snackbar.view as ViewGroup
    snackbarLayout.setPadding(0, 0, 0, 0)

    val snackView = LayoutInflater.from(view.context).inflate(R.layout.table_snackbar,
        snackbarLayout, false)
    // Inflate custom layout
    val snackbarText = snackView.findViewById<TextView>(R.id.snackbar_text)
    val snackbarAction = snackView.findViewById<TextView>(R.id.snackbar_action)
    val snackbarTable = snackView.findViewById<TableLayout>(R.id.snackbar_table)


    addTableHeader(snackbarTable, tableHeader.getOrNull(0) ?: "", tableHeader.getOrNull(1) ?: "")

    tableData.forEachIndexed { index, rowData ->
        addTableRow(
            snackbarTable,
            if (index % 2 == 0) evenRowColor else oddRowColor,
            rowData.getOrNull(0) ?: "", rowData.getOrNull(1) ?: ""
        )
    }
//
//    for (rowData in tableData) {
//        addTableRowWithCardStyle(snackbarTable, rowData.getOrNull(0) ?: "", rowData.getOrNull(1) ?: "")
//    }

    // Apply to TextView
    snackbarText.text = description

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
