/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class WordFinderPreferences : AppCompatActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable the up button in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Create a FrameLayout to host the fragment
        val frame = FrameLayout(this)
        frame.id = R.id.content
        setContentView(frame)

        // Load the PreferenceFragment
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.content, WordFinderSettingsFragment())
            .commit()

        frame.fitsSystemWindows = true
    }
    // Handle the up button press
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

}