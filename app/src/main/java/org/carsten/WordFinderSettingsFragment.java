package org.carsten;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.takisoft.preferencex.EditTextPreference;
import com.takisoft.preferencex.PreferenceFragmentCompat;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WordFinderSettingsFragment extends PreferenceFragmentCompat {
	@Override
	public void onCreatePreferencesFix(Bundle savedInstanceState, String rootKey) {
		setPreferencesFromResource(R.xml.preferences, rootKey);
		EditTextPreference timeEdit = (EditTextPreference) findPreference("countdown_time_pref");

		if(timeEdit!=null)
			timeEdit.setOnBindEditTextListener( (@NonNull EditText editText) -> {
					List<InputFilter> filters = new ArrayList<>(Arrays.asList(editText.getFilters()));
					filters.add((CharSequence source, int start, int end, Spanned dest, int dstart,
								 int dend) -> {
							SpannableStringBuilder builder = new SpannableStringBuilder(dest);
							builder.replace(dstart, dend, source, start, end);
							if( builder.toString().matches("[0-9]+(:(|[0-5]|[0-5][0-9]))?")) //     :?( [ ?[0-5]?[0-9]?"))
								return null;
							return "";
					});
					editText.setFilters(filters.toArray(new InputFilter[0]));
			});
	}
}
