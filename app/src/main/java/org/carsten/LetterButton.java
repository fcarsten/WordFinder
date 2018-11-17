/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carsten;

import android.widget.Button;

class LetterButton {

	final private int pos;
	final private Button button;

	LetterButton(int pos, Button button) {
		this.pos=pos;
		this.button=button;
	}

	public void setText(String string) {
		button.setText(string);
	}

	void setEnabled(boolean b) {
		button.setEnabled(b);		
	}

	int getPos() {
		return pos;
	}

}
