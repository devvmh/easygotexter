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
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class EasyGoTexter extends ListActivity {
    private static final int ACTIVITY_CREATE = 0;
    private static final int ACTIVITY_EDIT = 1;
    //private static final int ACTIVITY_SETTINGS = 2;

    private static final int DELETE_ID = Menu.FIRST;
    private static final int EDIT_ID = Menu.FIRST + 1;
    private static final int TEXT_ID = Menu.FIRST + 2;

    private DbAdapter mDbHelper;
    
    //Preferences - fields and updater function
    String text_to;
    private void getPrefs () {
    	SharedPreferences prefs = PreferenceManager
        	.getDefaultSharedPreferences(getBaseContext());
    	text_to = prefs.getString("text_to_pref", "57555");
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notes_list);
        mDbHelper = new DbAdapter(this);
        mDbHelper.open();
        fillData();
        registerForContextMenu(getListView());
        
        //update preferences
        getPrefs();
    }

    private void sendText (String message) {
    	getPrefs ();
    	String recipient = text_to;
    	
        if (message.length() < 4 || message.length() > 160) {
            return;
        }
        SmsManager sm = SmsManager.getDefault();
        sm.sendTextMessage(recipient, null, message, null, null);
        
        AlertDialog alert = new AlertDialog.Builder(this).create();
        alert.setTitle ("Text sent!");
        alert.setMessage ("Recipient: " + recipient + "\nMessage: " + message);
        alert.setButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				return;
			}
		});
        alert.show ();
    }

    private void fillData() {
        Cursor notesCursor = mDbHelper.fetchAllNotes();
        startManagingCursor(notesCursor);

        // Create an array to specify the fields we want to display in the list (only TITLE)
        String[] from = new String[]{DbAdapter.KEY_DESC};

        // and an array of the fields we want to bind those fields to (in this case just text1)
        int[] to = new int[]{R.id.text1};

        // Now create a simple cursor adapter and set it to display
        SimpleCursorAdapter notes = 
            new SimpleCursorAdapter(this, R.layout.notes_row, notesCursor, from, to);
        setListAdapter(notes);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater ();
    	inflater.inflate(R.menu.options_menu, menu);
    	
    	Intent prefsIntent = new Intent(this.getApplicationContext(),
    	        PrefsActivity.class);
    	 
    	MenuItem preferences = menu.findItem(R.id.settings_option_item);
    	preferences.setIntent(prefsIntent);
    	
    	return true;
    }

    public boolean onOptionsItemSelected (MenuItem item) {
    	switch (item.getItemId()) {
    		case R.id.add_note:
    	        Intent addIntent = new Intent(this, Editor.class);
    	        startActivityForResult(addIntent, ACTIVITY_CREATE);
    			return true;
    		case R.id.settings_option_item:
    			//Intent prefsIntent = new Intent (this, PrefsActivity.class);
    			//startActivityForResult (prefsIntent, ACTIVITY_SETTINGS);
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
    	AdapterContextMenuInfo info;
        switch(item.getItemId()) {
            case DELETE_ID:
                info = (AdapterContextMenuInfo) item.getMenuInfo();
                mDbHelper.deleteNote(info.id);
                fillData();
                return true;
            case EDIT_ID:
                Intent i = new Intent(this, Editor.class);
                info = (AdapterContextMenuInfo) item.getMenuInfo();
                i.putExtra(DbAdapter.KEY_ROWID, info.id);
                startActivityForResult(i, ACTIVITY_EDIT);
                return true;
            case TEXT_ID:
            	info = (AdapterContextMenuInfo) item.getMenuInfo();
                Cursor c = mDbHelper.fetchNote (info.id);
                String number = c.getString(c.getColumnIndexOrThrow(DbAdapter.KEY_NUMBER));
                sendText (number);
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        
        //send a text to EasyGo
        Cursor c = mDbHelper.fetchNote (id);
        String number = c.getString(c.getColumnIndexOrThrow(DbAdapter.KEY_NUMBER));
        sendText (number);
    }//onListitemClick
    
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        fillData ();
    }//onActivityResult
}
