/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder.gui

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
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import org.carstenf.wordfinder.BuildConfig
import org.carstenf.wordfinder.GameState
import org.carstenf.wordfinder.R
import org.carstenf.wordfinder.gui.InfoDialogFragment.Companion.TAG
import java.io.InputStream
import java.util.Locale
import kotlin.math.min

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
            getString(R.string.error_loading_html_content, e.message)
        }
    }
    private var dialogView : View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Obtain the GameState ViewModel scoped to the hosting Activity
        // This ensures the same instance is used across configuration changes
        // and is shared with the Activity.
        gameState = ViewModelProvider(requireActivity())[GameState::class.java]
    }

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
            "de" -> "html/de" // NON-NLS
            else -> "html/en" // Default to English // NON-NLS
        }

        val pages = mutableListOf <PageData>()
        val assets = context?.assets?.list(htmlAssetPath)
        if (assets != null) {
            val sortedAssets = assets.filterNotNull().sortedBy { it.lowercase() }
            for (page in sortedAssets) {
                if (page != "images") { // NON-NLS
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

        val res = builder.create()
        return res
    }

    override fun onDetach() {
        super.onDetach()
        gameState?.onResume()
        if(gameState==null) {
            Log.e(TAG, "gameState is null in InfoDialogFragment#onDetach") // NON-NLS
        }
    }


    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            // It's best to get insets on the decorView
            val decorView = window.decorView
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(decorView) { v, insets ->
                val systemBars = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars() or
                            androidx.core.view.WindowInsetsCompat.Type.displayCutout()
                )

                val displayMetrics = resources.displayMetrics
                val availableWidth = displayMetrics.widthPixels - systemBars.left - systemBars.right
                val availableHeight = displayMetrics.heightPixels - systemBars.top - systemBars.bottom

                val dialogWidth = (availableWidth * 0.90).toInt()
                val dialogHeight = (availableHeight * 0.80).toInt()

                window.setLayout(dialogWidth, dialogHeight)
                window.setGravity(android.view.Gravity.CENTER)
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND
                )

                // If you only need to apply once, you can remove the listener
                // androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(v, null)

                insets // Return the insets
            }
            // Request insets to trigger the listener
            androidx.core.view.ViewCompat.requestApplyInsets(decorView)
        }
    }
    private var gameState: GameState? = null

    companion object {
        const val TAG = "WordFinder InfoDialog" // NON-NLS

        fun showInfo(fragmentManager: FragmentManager, state: GameState) {
            val dialogFragment = InfoDialogFragment()
            state.onPause()
            dialogFragment.show(fragmentManager, TAG)
        }
    }
}

data class PageData(val htmlText: String, val htmlAssetPath: String)

class InfoPagerAdapter(private val pages: List<PageData>) :
    RecyclerView.Adapter<InfoPagerAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_info_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    override fun getItemCount(): Int = pages.size

    class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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

class DialogImageHandler(private val page: PageData, private val textView: TextView): Html.ImageGetter {

    override fun getDrawable(source: String?): Drawable? {
        return try {
            val inputStream: InputStream =
                textView.context.assets.open(page.htmlAssetPath + "/" + source)
            val drawable = Drawable.createFromStream(inputStream, null)
            if(drawable==null) {
                Log.e(TAG, "Drawable is null in DialogImageHandler#getDrawable")  // NON-NLS
                return null
            }
            val originalWidth = drawable.intrinsicWidth
            val originalHeight = drawable.intrinsicHeight

            val metrics = textView.resources.displayMetrics
            val screenWidth = metrics.widthPixels // Get the width of the TextView
            val density = metrics.density
            val maxWidth = (500 * density).toInt() // Convert dp to px. We want the dialog to be at least 300dp

            val desiredWidth = min ((screenWidth*0.8).toInt(), maxWidth)

            // Calculate the height to maintain the aspect ratio
            val desiredHeight = originalHeight * ( desiredWidth*1.0 / originalWidth)

            drawable.setBounds(
                0,
                0,
                desiredWidth,
                desiredHeight.toInt()
            )
            drawable
        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
            return null
        }

    }

}