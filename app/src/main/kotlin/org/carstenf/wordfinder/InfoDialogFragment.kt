package org.carstenf.wordfinder

import android.app.Dialog
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.viewpager2.widget.ViewPager2
import java.io.InputStream
import java.util.Locale

class InfoDialogFragment : DialogFragment() {

    private fun loadHtmlFromAssets(fileName: String): String {
        return try {
            val inputStream: InputStream = requireContext().assets.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val rawText = String(buffer, Charsets.UTF_8)
            rawText.replace("X.X", BuildConfig.VERSION_NAME)
        } catch (e: Exception) {
            Log.e(TAG, e.message,e )
            "Error loading HTML content: ${e.message}"
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = requireActivity().layoutInflater.inflate(R.layout.dialog_info, null)
        builder.setView(dialogView)

        val viewPager: ViewPager2 = dialogView.findViewById(R.id.view_pager)
        val prevButton: Button = dialogView.findViewById(R.id.prev_button)
        val nextButton: Button = dialogView.findViewById(R.id.next_button)
        val closeButton: Button = dialogView.findViewById(R.id.close_button)

        val locale = Locale.getDefault().language

        val htmlAssetPath  = when (locale) {
            "de" -> "html/de"
            else -> "html/en" // Default to English
        }

        val pages = mutableListOf <PageData>()
        val assets = context?.assets?.list(htmlAssetPath)
        if (assets != null) {
            val sortedAssets = assets.filterNotNull().sortedBy { it.lowercase() }
            for (page in sortedAssets) {
                if (page != "images") {
                    val htmlFileName = "$htmlAssetPath/$page"
                    val htmlContent = loadHtmlFromAssets(htmlFileName)
                    pages.add(PageData(htmlContent, htmlAssetPath))
                }
            }
        }

        val adapter = InfoPagerAdapter(pages)
        viewPager.adapter = adapter

        // Handle button clicks
        nextButton.setOnClickListener {
            val nextIndex = viewPager.currentItem + 1
            if (nextIndex < pages.size) {
                viewPager.currentItem = nextIndex
            }
        }

        prevButton.setOnClickListener {
            val prevIndex = viewPager.currentItem - 1
            if (prevIndex >= 0) {
                viewPager.currentItem = prevIndex
            }
        }

        closeButton.setOnClickListener {
            dismiss()
        }

        // Update button states based on page position
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                prevButton.isEnabled = position > 0
                nextButton.isEnabled = position < pages.size - 1
            }
        })

        return builder.create()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    companion object {
        private const val TAG = "WordFinder InfoDialog"

        fun showInfo(fragmentManager: androidx.fragment.app.FragmentManager) {
            val dialogFragment = InfoDialogFragment()
            dialogFragment.show(fragmentManager, TAG)
        }
    }
}

data class PageData(val htmlText: String, val htmlAssetPath: String)

class InfoPagerAdapter(private val pages: List<PageData>) :
    androidx.recyclerview.widget.RecyclerView.Adapter<InfoPagerAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_info_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    override fun getItemCount(): Int = pages.size

    class PageViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        private val textView: TextView = view.findViewById(R.id.info_text)

        fun bind(page: PageData) {
            // Use the newer Html.fromHtml method with FROM_HTML_MODE_LEGACY
            val markup = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(page.htmlText, Html.FROM_HTML_MODE_LEGACY, { source ->
                    try {
                        val inputStream: InputStream = textView.context.assets.open(page.htmlAssetPath+"/"+source)
                        val drawable = Drawable.createFromStream(inputStream, null)
                        drawable?.setBounds(0, 0, drawable.intrinsicWidth*4, drawable.intrinsicHeight*4)
                        drawable
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null // Return null if the image cannot be loaded
                    }
                }, null)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(page.htmlText, { source ->
                    try {
                        val inputStream: InputStream = textView.context.assets.open(source)
                        val drawable = Drawable.createFromStream(inputStream, null)
                        drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                        drawable
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null // Return null if the image cannot be loaded
                    }
                }, null) // Fallback for older versions
            }

            textView.apply{
                movementMethod = LinkMovementMethod.getInstance()
                text = markup
                linksClickable = true
            }
        }
    }
}
