package org.carsten;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import com.takisoft.fix.support.v7.preference.EditTextPreference;
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WordFinderSettingsFragment extends PreferenceFragmentCompat {
	@Override
	public void onCreatePreferencesFix(Bundle savedInstanceState, String rootKey) {
		setPreferencesFromResource(R.xml.preferences, rootKey);
		EditTextPreference timeEdit = (EditTextPreference) findPreference("countdown_time_pref");
		List<InputFilter> filters = new ArrayList<>(Arrays.asList(timeEdit.getEditText().getFilters()));
		filters.add(new InputFilter() {
			@Nullable
            @Override
			public CharSequence filter(CharSequence source, int start, int end,
                                       Spanned dest, int dstart, int dend) {
				SpannableStringBuilder builder = new SpannableStringBuilder(dest);
				builder.replace(dstart, dend, source, start, end);
				if( builder.toString().matches("[0-9]+(:(|[0-5]|[0-5][0-9]))?")) //     :?( [ ?[0-5]?[0-9]?"))
					return null;
				return "";
			}
		});
		timeEdit.getEditText().setFilters(filters.toArray(new InputFilter[0]));
	}
}
