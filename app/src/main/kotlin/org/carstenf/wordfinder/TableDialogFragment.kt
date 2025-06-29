package org.carstenf.wordfinder // Make sure this matches your project's package name

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.fragment.app.DialogFragment

class TableDialogFragment : DialogFragment() {
    private val columnWeights = floatArrayOf(0.5f, 0.5f)

    private var description: String? = null
    private var tableHeader: ArrayList<String>? = null
    private var tableData: ArrayList<ArrayList<String>>? = null
    private var displayTime: Long = 0L

    private var dialogDismissHandler: Handler? = null
    private var dismissRunnable: Runnable? = null

    companion object {
        private const val ARG_DESCRIPTION = "description" // NON-NLS
        private const val ARG_TABLE_HEADER = "table_header" // NON-NLS
        private const val ARG_TABLE_DATA = "table_data" // NON-NLS
        private const val ARG_DISPLAY_TIME = "display_time" // NON-NLS

        fun newInstance(
            description: String,
            tableHeader: List<String>,
            tableData: List<List<String>>,
            displayTime: Long
        ): TableDialogFragment {
            val args = Bundle().apply {
                putString(ARG_DESCRIPTION, description)
                putStringArrayList(ARG_TABLE_HEADER, ArrayList(tableHeader))
                val serializableTableData = ArrayList(tableData.map { ArrayList(it) })
                putSerializable(ARG_TABLE_DATA, serializableTableData)
                putLong(ARG_DISPLAY_TIME, displayTime)
            }
            val fragment = TableDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            description = it.getString(ARG_DESCRIPTION)
            tableHeader = it.getStringArrayList(ARG_TABLE_HEADER)
            @Suppress("UNCHECKED_CAST", "DEPRECATION")  // NON-NLS
            tableData = it.getSerializable(ARG_TABLE_DATA) as? ArrayList<ArrayList<String>>
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
        val view = inflater.inflate(R.layout.table_snackbar, container, false)
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        val snackbarText = view.findViewById<TextView>(R.id.snackbar_text)
        val snackbarAction = view.findViewById<TextView>(R.id.snackbar_action)
        val snackbarTable = view.findViewById<TableLayout>(R.id.snackbar_table)

        description?.let { snackbarText.text = it }
        snackbarAction.setOnClickListener {
            dismissRunnable?.let { dialogDismissHandler?.removeCallbacks(it) }
            dismissAllowingStateLoss()
        }

        tableHeader?.let {
            addTableHeader(snackbarTable, requireContext(), it.getOrNull(0) ?: "", it.getOrNull(1) ?: "")
        }

        val evenRowColor = "#FFFFFF".toColorInt() // NON-NLS
        val oddRowColor = "#B5B5B5".toColorInt() // NON-NLS

        tableData?.forEachIndexed { index, rowData ->
            addTableRow(
                snackbarTable,
                requireContext(),
                if (index % 2 == 0) evenRowColor else oddRowColor,
                rowData.getOrNull(0) ?: "", rowData.getOrNull(1) ?: ""
            )
        }
        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (requireContext().resources.displayMetrics.widthPixels * 0.7).toInt(),
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

    // Helper function to add a table header (adapted to take context)
    private fun addTableHeader(tableLayout: TableLayout, context: Context, vararg headers: String) {
        val headerBackground = "#202020".toColorInt() // NON-NLS
        val headerRow = TableRow(context).apply {
            layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            )
            background = ContextCompat.getDrawable(context, R.drawable.cell_border)
        }

        headers.forEachIndexed { index, headerText ->
            TextView(context).apply {
                text = headerText
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setTextAppearance(R.style.TableHeaderText)
                } else {
                    setTextColor(ResourcesCompat.getColor(resources, R.color.md_theme_onPrimary, null)) // Adjusted to match original
                }
                setBackgroundColor(headerBackground)
                layoutParams = TableRow.LayoutParams(
                    0, // Will be weighted
                    TableRow.LayoutParams.WRAP_CONTENT,
                    columnWeights.getOrElse(index) { 0.5f } // Explicitly reference columnWeights from your package
                ).apply {
                    setMargins(1, 1, 1, 1)
                }
                headerRow.addView(this)
            }
        }
        tableLayout.addView(headerRow)
    }

    // Helper function to add a table row (adapted to take context)
    private fun addTableRow(tableLayout: TableLayout, context: Context, rowColor: Int, vararg cells: String) {
        val row = TableRow(context).apply {
            layoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT
            )
            background = ContextCompat.getDrawable(context, R.drawable.cell_border)
        }

        cells.forEachIndexed { index, cellText ->
            TextView(context).apply {
                text = cellText
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setTextAppearance(R.style.TableBodyText)
                } else {
                    setTextColor(ResourcesCompat.getColor(resources, R.color.md_theme_onPrimary, null)) // Adjusted to match original
                }
                setBackgroundColor(rowColor)
                layoutParams = TableRow.LayoutParams(
                    0, // Will be weighted
                    TableRow.LayoutParams.WRAP_CONTENT,
                    columnWeights.getOrElse(index) { 0.5f } // Explicitly reference columnWeights from your package
                ).apply {
                    setPadding(8, 2, 8, 2)
                }
                row.addView(this)
            }
        }
        tableLayout.addView(row)
    }
}