package com.callysto.devin.easygotexter.util;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * stores an integer row id in a view that can be hidden in each row
 * @author Devin
 *
 */

public class RowidImageView extends ImageView {
	public RowidImageView(Context context) {
		super(context);
	}
	public RowidImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	public RowidImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	private long rowid;
	public long getRowid() {
		return rowid;
	}
	public void setRowid(long val) {
		rowid = val;
	}
}
