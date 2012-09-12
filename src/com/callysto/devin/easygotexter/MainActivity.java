/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.callysto.devin.easygotexter;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.custom.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.callysto.devin.easygotexter.util.DbAdapter;
import com.callysto.devin.easygotexter.util.FormattableCursorAdapter;
import com.callysto.devin.easygotexter.util.RowidImageView;
import com.ericharlow.DragNDrop.DragListener;
import com.ericharlow.DragNDrop.DragNDropListView;
import com.ericharlow.DragNDrop.DropListener;

public class MainActivity extends ListActivity {
	public final static String TAG = "com.callysto.devin.easygotexter";
    private static final int ACTIVITY_CREATE = 0;
    private static final int ACTIVITY_EDIT = 1;

    private static final int DELETE_ID = Menu.FIRST;
    private static final int EDIT_ID = Menu.FIRST + 1;
    private static final int TEXT_ID = Menu.FIRST + 2;
    
    private EditText quickTextBox;
    private AlertDialog alert;
    private DbAdapter mDbHelper;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notes_list);
        mDbHelper = new DbAdapter(this);
        mDbHelper.open();
        fillData();
        registerForContextMenu(getListView());
        
        quickTextBox = (EditText) findViewById (R.id.quicktextbox);
        
    	//sets up alerts
    	alert = new AlertDialog.Builder(this).create();
    	
    	setUpDragNDrop();
        
        //update preferences
        getPrefs();
    }
    
    public void onResume () {
    	super.onResume();
    	//so preferences will update
    	getPrefs();
    	fillData();
    }//onResume
    
    //Preferences - fields and updater function
    String default_recipient;
    boolean showNumberToo;
    private void getPrefs () {
    	SharedPreferences prefs = PreferenceManager
        	.getDefaultSharedPreferences(getBaseContext());
    	default_recipient = prefs.getString("default_recip_pref", "57555");
    	showNumberToo = prefs.getBoolean("show_num_in_desc", true);	
    }

    private void sendText (String message, String recipient) {
    	getPrefs ();
    	
        if (message.length() < 4 || message.length() > 160) {
            return;
        }
        SmsManager sm = SmsManager.getDefault();
        sm.sendTextMessage(recipient, null, message, null, null);
        
        alert ("Text sent!", "Recipient:" + recipient + "\nMessage: " + message);
    }

    private void fillData() {
        Cursor notesCursor = mDbHelper.fetchAllNotes();
        startManagingCursor(notesCursor);
        CursorAdapter notes;

        //showing just visible description, or number too?
        String [] from;
        String format;
        if (showNumberToo) {
        	// Create an array to specify the fields we want to display in the list
        	from = new String[]{DbAdapter.KEY_DESC, DbAdapter.KEY_NUMBER};
        	
        	//put DESC in the first {} and NUMBER in the second {}
        	format = "{} - {}";
        	
        } else {
        	//analagous to the above, but only showing the visible description
        	from = new String [] {DbAdapter.KEY_DESC};
        	format = "{}";
    	}//if
    	notes = new FormattableCursorAdapter (this, notesCursor, from, format, R.id.text1, R.layout.notes_row, mDbHelper);
        setListAdapter(notes);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater ();
    	inflater.inflate(R.menu.options_menu, menu);
    	
    	//prefs intent
    	Intent prefsIntent = new Intent(this.getApplicationContext(),
    	        PrefsActivity.class);
    	MenuItem preferences = menu.findItem(R.id.settings_option_item);
    	preferences.setIntent(prefsIntent);
    	
    	//import/export intent
    	Intent importExportIntent = new Intent (this.getApplicationContext(),
    			ImportExportActivity.class);
    	MenuItem importexport = menu.findItem(R.id.importexport_option_item);
    	importexport.setIntent(importExportIntent);
    		
    	return true;
    }

    public boolean onOptionsItemSelected (MenuItem item) {
    	switch (item.getItemId()) {
    		case R.id.add_note:
    	        Intent i = new Intent(this, EditorActivity.class);
    		    i.putExtra (DbAdapter.KEY_NUMBER, "");
    	        startActivityForResult(i, ACTIVITY_CREATE);
    			return true;
    		case R.id.settings_option_item:
    		case R.id.importexport_option_item:
    		//case R.id.grtmobile_option_item:
    			this.startActivity(item.getIntent());
    			return true;
    	}//switch
		return false;
    }//onOptionsItemSelected - options menu

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, DELETE_ID, 0, R.string.menu_delete);
        menu.add(0, EDIT_ID, 0, R.string.edit_note);
        menu.add(0, TEXT_ID, 0, R.string.send_text);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    	Cursor c = mDbHelper.fetchNoteFromOrder(info.id);
    	long rowid = c.getLong(c.getColumnIndexOrThrow(DbAdapter.KEY_ROWID));
        switch(item.getItemId()) {
            case DELETE_ID:
                info = (AdapterContextMenuInfo) item.getMenuInfo();
                mDbHelper.deleteNote(rowid);
                fillData();
                return true;
            case EDIT_ID:
                Intent i = new Intent(this, EditorActivity.class);
                i.putExtra(DbAdapter.KEY_ROWID, rowid);
                startActivityForResult(i, ACTIVITY_EDIT);
                return true;
            case TEXT_ID:
            	//c declared above
                String number = c.getString(c.getColumnIndexOrThrow(
                		DbAdapter.KEY_NUMBER));
                String recipient = c.getString(c.getColumnIndexOrThrow(
                		DbAdapter.KEY_RECIP));
                sendText (number, recipient);
        }//switch
        return super.onContextItemSelected(item);
    }//onContextItemSelected

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v , position, id);

        //its actually the order...
        Cursor c = mDbHelper.fetchNoteFromOrder (id);
        
        String number = c.getString(c.getColumnIndexOrThrow(DbAdapter.KEY_NUMBER));
        String recipient = c.getString(c.getColumnIndexOrThrow(DbAdapter.KEY_RECIP));
        
        //send a text to EasyGo
        sendText (number, recipient);
    }//onListitemClick
    
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        fillData ();
    }//onActivityResult
	
    //helper to simply pop up alert dialogs
    public void alert (String title, String message) {
        alert.setTitle(title);
        alert.setMessage (message);
        alert.setButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				return;
			}
		});
        alert.show ();
    }//alert helper function
	
	//called if you click the quick text button
	public void quickText(View view) {
		String text = quickTextBox.getText().toString();
		if (text.length () == 0) return;
	    sendText (text, default_recipient);
	    
	    //now start an intent to add an entry
	    Intent i = new Intent(this, EditorActivity.class);
	    i.putExtra (DbAdapter.KEY_NUMBER, text);
	    startActivityForResult(i, ACTIVITY_CREATE);
	 }
	
	public void setUpDragNDrop() {
        ListView listView = getListView();
        
        if (listView instanceof DragNDropListView) {
        	((DragNDropListView) listView).setDropListener(new DropListener() {
    	        public void onDrop(View dragged, View replaced) {
    	        	ListAdapter adapter = getListAdapter();
    	        	if (adapter instanceof DropListener) {
    	        		((DropListener)adapter).onDrop(dragged, replaced);
    	        		Log.v(TAG, "Invalidating: " + getListView().getClass().getName());
    	        		fillData();
    	        	}
    	        }
        	});
        	((DragNDropListView) listView).setDragListener(new DragListener() {

	        	int backgroundColor = 0xe0103010;
	        	int defaultBackgroundColor;
	        	
	    			public void onDrag(int x, int y, ListView listView) {
	    				// TODO Auto-generated method stub
	    			}

	    			public void onStartDrag(View itemView) {
	    				itemView.setVisibility(View.INVISIBLE);
	    				defaultBackgroundColor = itemView.getDrawingCacheBackgroundColor();
	    				itemView.setBackgroundColor(backgroundColor);
	    				ImageView iv = (ImageView)itemView.findViewById(R.id.image);
	    				if (iv != null) iv.setVisibility(View.INVISIBLE);
	    			}

	    			public void onStopDrag(View itemView) {
	    				itemView.setVisibility(View.VISIBLE);
	    				itemView.setBackgroundColor(defaultBackgroundColor);
	    				RowidImageView iv = (RowidImageView)itemView.findViewById(R.id.image);
	    				if (iv != null) iv.setVisibility(View.VISIBLE);
	    			}
	        });
        }//if it's actually a drag 'n' drop list view!
	}//set up drag and drop
}
