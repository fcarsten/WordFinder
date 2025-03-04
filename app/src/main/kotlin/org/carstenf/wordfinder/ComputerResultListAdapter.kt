package org.carstenf.wordfinder

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

open class ComputerResultListAdapter(context: Context) :
    ArrayAdapter<Result>(context, R.layout.list_item, R.id.resultText) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view =
            convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)

        val result = getItem(position)

        // Set the text
        view.findViewById<TextView>(R.id.resultText).text = result?.result

        // Change the background color if the item is highlighted
        if (result != null) {
            if (result.isHighlighted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    view.setBackgroundColor(context.getColor(R.color.md_theme_secondaryContainer))
                } else {
                    @Suppress("DEPRECATION")
                    view.setBackgroundColor(context.resources.getColor(R.color.md_theme_secondaryContainer))
                }
            } else {
                view.setBackgroundColor(Color.TRANSPARENT) // Reset to default
            }
        }

        return view
    }
}