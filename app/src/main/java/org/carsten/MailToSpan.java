/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carsten;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.MailTo;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.text.ParcelableSpan;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.Toast;

public class MailToSpan extends ClickableSpan implements ParcelableSpan {

	private final String mURL;
	private final int spanTypeId;

	MailToSpan(URLSpan urlSpan) {
		mURL = urlSpan.getURL();
		spanTypeId = urlSpan.getSpanTypeId();
	}

	public int getSpanTypeId() {
		return spanTypeId;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(@NonNull Parcel dest, int flags) {
		dest.writeString(mURL);
	}

	@org.jetbrains.annotations.Contract(pure = true)
	private String getURL() {
		return mURL;
	}

	@Override
	public void onClick(@NonNull View widget) {
		Context context = widget.getContext();
		MailTo mt = MailTo.parse(getURL());
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("plain/text");
		i.putExtra(Intent.EXTRA_EMAIL, new String[] { mt.getTo() });
		i.putExtra(Intent.EXTRA_SUBJECT, mt.getSubject());
		i.putExtra(Intent.EXTRA_CC, mt.getCc());
		i.putExtra(Intent.EXTRA_TEXT, mt.getBody());
		try {
			context.startActivity(i);
		} catch (ActivityNotFoundException e) {
      Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show();
		}
	}

}
