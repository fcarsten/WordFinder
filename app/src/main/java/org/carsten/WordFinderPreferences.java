/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carsten;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.support.annotation.Nullable;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

public class WordFinderPreferences extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
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
