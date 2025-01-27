/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder;


import androidx.annotation.NonNull;

/**
 * @author carsten.friedrich@gmail.com
 *
 */
class Result {

	@NonNull
	public String getResult() {
		return result;
	}

	@NonNull
    final private String result;

	Result(	@NonNull String result) {
		this.result=result;
	}

	@NonNull
	@Override
	public String toString() {
		return result;
	}
}
