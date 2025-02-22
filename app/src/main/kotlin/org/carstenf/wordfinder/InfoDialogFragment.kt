package org.carstenf.wordfinder

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.viewpager2.widget.ViewPager2

class InfoDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = requireActivity().layoutInflater.inflate(R.layout.dialog_info, null)
        builder.setView(dialogView)

        val viewPager: ViewPager2 = dialogView.findViewById(R.id.view_pager)
        val prevButton: Button = dialogView.findViewById(R.id.prev_button)
        val nextButton: Button = dialogView.findViewById(R.id.next_button)
        val closeButton: Button = dialogView.findViewById(R.id.close_button)

        // Page data (replace with your actual data)
        val pages = listOf(
            PageData(getString(R.string.InfoText), R.drawable.key_wide),
            PageData(getString(R.string.InfoText), R.drawable.key_wide),
            PageData(getString(R.string.InfoText), null)
        )

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

data class PageData(val textResId: String, val imageResId: Int?)

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
        private val imageView: ImageView = view.findViewById(R.id.info_image)

        fun bind(page: PageData) {
            val t = page.textResId
            // Use the newer Html.fromHtml method with FROM_HTML_MODE_LEGACY
            val markup = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(t, Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(t) // Fallback for older versions
            }

            textView.apply{
                movementMethod = LinkMovementMethod.getInstance()
                text = markup
                linksClickable = true
            }

            page.imageResId?.let {
                imageView.setImageResource(it)
                imageView.visibility = View.VISIBLE
            } ?: run {
                imageView.visibility = View.GONE
            }
        }
    }
}
