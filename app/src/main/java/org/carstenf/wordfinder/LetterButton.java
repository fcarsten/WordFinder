/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder;

import android.view.Gravity;
import android.widget.Button;

import org.jetbrains.annotations.NotNull;

class LetterButton {

	final private int pos;
	final private Button button;

	LetterButton(int pos, @NotNull Button button) {
		this.pos=pos;
		this.button=button;
	}

	public void setText(String string) {
		button.setText(string);
	}

	void setEnabled(boolean b) {
		if(b) {
			button.setGravity(Gravity.CENTER_HORIZONTAL| Gravity.TOP);
		} else {
			button.setGravity(Gravity.CENTER_HORIZONTAL| Gravity.BOTTOM);
		}
		button.setEnabled(b);		
	}

	int getPos() {
		return pos;
	}

	public void setContentDescription(String s) {
		button.setContentDescription(s);
	}
}
