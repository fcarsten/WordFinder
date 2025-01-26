/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

public class WordFinderPreferences extends AppCompatActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Create a FrameLayout to host the fragment
		FrameLayout frame = new FrameLayout(this);
		frame.setId(R.id.content);
		setContentView(frame);

		// Load the PreferenceFragment
		getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.content, new WordFinderSettingsFragment())
				.commit();
	}
}