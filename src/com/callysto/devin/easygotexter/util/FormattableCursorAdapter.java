package com.callysto.devin.easygotexter.util;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.callysto.devin.easygotexter.R;
import com.ericharlow.DragNDrop.DropListener;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.custom.CursorAdapter;


public class FormattableCursorAdapter extends CursorAdapter implements DropListener {
	
	private Vector<String> keys;
	private String format;
	private int textViewId;
	private int layoutId;
	private LayoutInflater inflater;
	DbAdapter mDb;
	Context context;

	/**
	 * @param context the context, for views
	 * @param c the cursor we're adapting
	 * @param keys the database keys - suppose there are n of them
	 * @param format you must have n fields in this string, denoted as {} or {i} where 0 <= i < n
	 * @param textViewId the id of the text field found in the layout
	 * @param the id of the layout
	 */
	public FormattableCursorAdapter(Context context, Cursor c, String [] keys, String format, 
			int textViewId, int layoutId, DbAdapter db) {
		super(context, c);
		this.keys = new Vector<String>();
		for (String s : keys) {
			this.keys.add(s);
		}
		this.format = format;
		this.textViewId = textViewId;
		this.layoutId = layoutId;
    	this.inflater = LayoutInflater.from(context);
    	this.context = context;
    	this.mDb = db;
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
		
		String key = "_id";
		long rowid = c.getInt(c.getColumnIndexOrThrow(key));
		RowidImageView iv = (RowidImageView)view.findViewById(R.id.image);
		iv.setRowid(rowid);
		
		TextView tv = (TextView) view.findViewById(textViewId);
		tv.setText(message);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View row =  inflater.inflate(layoutId, null);
		return row;
	}

	public void onDrop(View dragged, View replaced) {
		long fromRowid = ((RowidImageView)dragged.findViewById(R.id.image)).getRowid();
		long toRowid = ((RowidImageView)replaced.findViewById(R.id.image)).getRowid();
		Cursor c = mDb.fetchNote(fromRowid);
		String fromNum = c.getString(c.getColumnIndexOrThrow(DbAdapter.KEY_NUMBER));
		String fromDesc = c.getString(c.getColumnIndexOrThrow(DbAdapter.KEY_DESC));
		String fromRecip = c.getString(c.getColumnIndexOrThrow(DbAdapter.KEY_RECIP));
		int fromOrder = c.getInt(c.getColumnIndexOrThrow(DbAdapter.KEY_ORDER));

		c = mDb.fetchNote(toRowid);
		String toNum = c.getString(c.getColumnIndexOrThrow(DbAdapter.KEY_NUMBER));
		String toDesc = c.getString(c.getColumnIndexOrThrow(DbAdapter.KEY_DESC));
		String toRecip = c.getString(c.getColumnIndexOrThrow(DbAdapter.KEY_RECIP));
		int toOrder = c.getInt(c.getColumnIndexOrThrow(DbAdapter.KEY_ORDER));
		
		//swap the two rowids
		mDb.updateNote(toRowid, toNum, toDesc, toRecip, fromOrder);
		mDb.updateNote(fromRowid, fromNum, fromDesc, fromRecip, toOrder);
		//mDb.updateNote(toRowid, toNum, toDesc, toRecip, fromOrder);
	}
}
