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
import android.view.WindowManager
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.viewpager2.widget.ViewPager2
import org.carstenf.wordfinder.InfoDialogFragment.Companion.TAG
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
    var dialogView : View? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val lDialogView = requireActivity().layoutInflater.inflate(R.layout.dialog_info, null)
        builder.setView(lDialogView)

        val viewPager: ViewPager2 = lDialogView.findViewById(R.id.view_pager)
        val prevButton: Button = lDialogView.findViewById(R.id.prev_button)
        val nextButton: Button = lDialogView.findViewById(R.id.next_button)
        val closeButton: Button = lDialogView.findViewById(R.id.close_button)

        dialogView = lDialogView
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
//                val scrollView = viewPager.findViewById<ScrollView>(R.id.info_scrollview)
//                scrollView.post {
//                    scrollView.smoothScrollBy(0, 50) // Subtle nudge down
//                }
            }
        })

        val res = builder.create()
        return res
    }

    override fun onResume() {
        super.onResume()
        val height = (resources.displayMetrics.heightPixels * 0.8).toInt()
        val lDialogView = dialogView
        if(lDialogView != null) {
            lDialogView.layoutParams.height = height
            lDialogView.requestLayout()
            lDialogView.invalidate()
            lDialogView.forceLayout()
            lDialogView.bringToFront()
        }
    }

    override fun onStart() {
        super.onStart()
        val height = (resources.displayMetrics.heightPixels * 0.8).toInt()
        val lWindow = dialog?.window
        if( lWindow !=null ){
            lWindow.setLayout((resources.displayMetrics.widthPixels * 0.95).toInt(), height)
            lWindow.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }

    companion object {
        const val TAG = "WordFinder InfoDialog"

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
                Html.fromHtml(page.htmlText, Html.FROM_HTML_MODE_LEGACY, DialogImageHandler(page, textView),null)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(page.htmlText, DialogImageHandler(page, textView), null) // Fallback for older versions
            }

            textView.apply{
                movementMethod = LinkMovementMethod.getInstance()
                text = markup
                linksClickable = true
            }
        }
    }
}

class DialogImageHandler(val page: PageData, val textView: TextView): Html.ImageGetter {

    override fun getDrawable(source: String?): Drawable? {
        return try {
            val inputStream: InputStream =
                textView.context.assets.open(page.htmlAssetPath + "/" + source)
            val drawable = Drawable.createFromStream(inputStream, null)
            if(drawable==null) {
                Log.e(TAG, "Drawable is null")
                return null
            }
            val originalWidth = drawable.intrinsicWidth
            val originalHeight = drawable.intrinsicHeight

//            val textViewWidth = textView.width // Get the width of the TextView
            val desiredWidth = originalWidth * 4

            // Calculate the height to maintain the aspect ratio
            val desiredHeight = originalHeight * 4

            drawable.setBounds(
                0,
                0,
                desiredWidth,
                desiredHeight
            )
            drawable
        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
            return null
        }

    }

}