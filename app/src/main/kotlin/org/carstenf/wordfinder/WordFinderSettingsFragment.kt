package org.carstenf.wordfinder

import android.os.Bundle
import android.text.InputFilter
import android.text.SpannableStringBuilder
import androidx.preference.PreferenceFragmentCompat
import com.takisoft.preferencex.EditTextPreference

class WordFinderSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val timeEdit: EditTextPreference? = findPreference("countdown_time_pref")

        timeEdit?.setOnBindEditTextListener { editText ->
            val filters = editText.filters.toMutableList()
            filters.add(InputFilter { source, start, end, dest, dstart, dend ->
                val builder = SpannableStringBuilder(dest)
                builder.replace(dstart, dend, source, start, end)
                if (builder.toString().matches(Regex("[0-9]+(:([0-5]|[0-5][0-9])?)?"))) {
                    null
                } else {
                    ""
                }
            })
            editText.filters = filters.toTypedArray()
        }
    }
}
