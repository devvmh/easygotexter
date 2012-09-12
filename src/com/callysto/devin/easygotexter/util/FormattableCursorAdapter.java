package com.callysto.devin.easygotexter.util;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

public class FormattableCursorAdapter extends CursorAdapter {
	
	private Vector<String> keys;
	private String format;
	private int textViewId;
	private int layoutId;

	/**
	 * @param context the context, for views
	 * @param c the cursor we're adapting
	 * @param keys the database keys - suppose there are n of them
	 * @param format you must have n fields in this string, denoted as {} or {i} where 0 <= i < n
	 * @param textViewId the id of the text field found in the layout
	 * @param the id of the layout
	 */
	public FormattableCursorAdapter(Context context, Cursor c, String [] keys, String format, 
			int textViewId, int layoutId) {
		super(context, c);
		this.keys = new Vector<String>();
		for (String s : keys) {
			this.keys.add(s);
		}
		this.format = format;
		this.textViewId = textViewId;
		this.layoutId = layoutId;
	}

	@Override
	public void bindView(View view, Context context, Cursor c) {
		String message = format;
        final Pattern pat = Pattern.compile("\\{\\}");  
        
		for (int i = 0; i < keys.size(); i += 1) {
			//get the value from the database
			String key = keys.get(i);
            String val = c.getString(c.getColumnIndexOrThrow(key));
            
            //replace the next {} with the value you want
            //and update message
            Matcher mat = pat.matcher(message);  
            message = mat.replaceFirst(val);
		}
		
		TextView tv = (TextView) view.findViewById(textViewId);
		tv.setText(message);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater)context.getSystemService
			      (Context.LAYOUT_INFLATER_SERVICE);
		View row =  inflater.inflate(layoutId, null);
		return row;
	}
	
	

}
