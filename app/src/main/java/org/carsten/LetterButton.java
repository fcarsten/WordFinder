/**
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carsten;

import android.widget.Button;

public class LetterButton {

	private int id;
	public int getId() {
		return id;
	}

	private int pos;
	private Button button;

	public LetterButton(int id, int pos, Button button) {
		this.id =id;
		this.pos=pos;
		this.button=button;
	}

	public void setText(String string) {
		button.setText(string);
	}

	public CharSequence getText() {
		return button.getText();
	}

	public void setEnabled(boolean b) {
		button.setEnabled(b);		
	}

	public int getPos() {
		return pos;
	}

}
