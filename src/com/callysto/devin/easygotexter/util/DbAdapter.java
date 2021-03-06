/*
 * Copyright (C) 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.callysto.devin.easygotexter.util;

import java.util.Vector;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbAdapter {

    public static final String KEY_NUMBER = "_number";
    public static final String KEY_DESC = "_description";
    public static final String KEY_RECIP = "_recipient";
    public static final String KEY_ROWID = "_id";
    public static final String KEY_ORDER = "_order";
    public static int highestOrderIndex = 0;
    
    /**
     * Note: _order could have any value - it's not an orderly list of indices.
     * The only thing it communicates is the order of the rows.
     * E.g. values could be 1, 2, 9, 16, 17, 18
     */
    
    private static final String TAG = "DbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    /**
     * Database creation SQL statement
     */
    private static final String DATABASE_CREATE =
        "create table notes (_id integer primary key autoincrement, "
        + "_number text not null, _description text not null, "
        + "_recipient text not null , _order integer not null);";

    private static final String DATABASE_NAME = "data";
    private static final String DATABASE_TABLE = "notes";
    private static final int DATABASE_VERSION = 6;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        	if (oldVersion == 5 ) {
        		Log.w(TAG, "Ugrading database from version 5 to " + newVersion + ", notes should survive.");
        		Cursor c = db.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_NUMBER,
                        KEY_DESC, KEY_RECIP}, null, null, null, null, null);
        		
        		//save old DB
        		Vector<String> numbers = new Vector<String>();
        		Vector<String> recipients = new Vector<String>();
        		Vector<String> descriptions = new Vector<String>();
        		c.moveToFirst();
        		while (c.isAfterLast() == false) {
        		    numbers.add(c.getString(c.getColumnIndexOrThrow(KEY_NUMBER)));
        		    recipients.add(c.getString(c.getColumnIndexOrThrow(KEY_RECIP)));
        		    descriptions.add(c.getString(c.getColumnIndexOrThrow(KEY_DESC)));
        		    c.moveToNext();
        		}
        		
        		db.execSQL("DROP TABLE IF EXISTS notes");
        		onCreate(db);
        		
        		int i = 0;
        		while (i < numbers.size()) {
        	        ContentValues initialValues = new ContentValues();
        	        initialValues.put(KEY_NUMBER, numbers.get(i));
        	        initialValues.put(KEY_DESC, descriptions.get(i));
        	        initialValues.put(KEY_RECIP, recipients.get(i));
        	        initialValues.put(KEY_ORDER, highestOrderIndex);

        	        db.insert(DATABASE_TABLE, null, initialValues);
        	        highestOrderIndex += 1;
        	        i+= 1;
        		}
        	} else {
        		Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
        				+ newVersion + ", which will destroy all old data.");
        		db.execSQL("DROP TABLE IF EXISTS notes");
        		onCreate(db);
        	}//if
        }//onUpgrade
        
        public void deleteAll (SQLiteDatabase db) {
        	db.execSQL ("DROP TABLE IF EXISTS notes");
        	onCreate (db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public DbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the notes database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public DbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        
        //get highest order integer
        Cursor c = mDb.query(DATABASE_TABLE, new String[] {KEY_ORDER}, null, null, null, null, KEY_ORDER + " DESC");
        c.moveToFirst();
        if (! c.isAfterLast()) {
        	highestOrderIndex = c.getInt(c.getColumnIndexOrThrow(KEY_ORDER));
        }
        
        return this;
    }

    public void close() {
        mDbHelper.close();
    }


    /**
     * Create a new note using the title and body provided. If the note is
     * successfully created return the new rowId for that note, otherwise return
     * a -1 to indicate failure.
     * 
     * @param title the title of the note
     * @param body the body of the note
     * @return rowId or -1 if failed
     */
    public long createNote(String number, String description, String recipient) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_NUMBER, number);
        initialValues.put(KEY_DESC, description);
        initialValues.put(KEY_RECIP, recipient);
        
        //should be incrementing before because this was set in the open() method
        //so it's currently set to the curent highest "_order" value
        highestOrderIndex += 1;
        initialValues.put(KEY_ORDER, highestOrderIndex);

        //only create if it's not blank
        if (description.equals ("")) {
        	return -1;
        } else {
        	return mDb.insert(DATABASE_TABLE, null, initialValues);
        }//if
    }//createNote

    /**
     * Delete the note with the given rowId
     * 
     * @param rowId id of note to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteNote(long rowId) {
        return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    public void deleteAllNotes () {
        mDbHelper.deleteAll (mDb);
    }

    /**
     * Return a Cursor over the list of all notes in the database
     * 
     * @return Cursor over all notes
     */
    public Cursor fetchAllNotes() {
        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_NUMBER,
                KEY_DESC, KEY_RECIP, KEY_ORDER}, null, null, null, null, KEY_ORDER);
    }

    /**
     * Return a Cursor positioned at the note that matches the given rowId
     * 
     * @param rowId id of note to retrieve
     * @return Cursor positioned to matching note, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchNote(long rowId) throws SQLException {

        Cursor mCursor =

            mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                    KEY_NUMBER, KEY_DESC, KEY_RECIP, KEY_ORDER}, KEY_ROWID + "=" + rowId, null,
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }
    
    /**
     * Return a Cursor positioned at the note that matches the given order
     * 
     * @param rowId id of note to retrieve
     * @return Cursor positioned to matching note, if found
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchNoteFromOrder(long order) throws SQLException {

        Cursor mCursor =

            mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                    KEY_NUMBER, KEY_DESC, KEY_RECIP, KEY_ORDER}, KEY_ORDER + "=" + order, null,
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    /**
     * Update the note using the details provided. The note to be updated is
     * specified using the rowId, and it is altered to use the title and body
     * values passed in
     * 
     * @param rowId id of note to update
     * @param title value to set note title to
     * @param body value to set note body to
     * @param order value to set order index to
     * @return true if the note was successfully updated, false otherwise
     */
    public boolean updateNote(long rowId, String number, String description, String recipient) {
    	return updateNote(rowId, number, description, recipient, null);
    }
    public boolean updateNote(long rowId, String number, String description, 
    		String recipient, Integer newOrder) {
        ContentValues args = new ContentValues();
        args.put(KEY_NUMBER, number);
        args.put(KEY_DESC, description);
        args.put(KEY_RECIP, recipient);
        if (newOrder != null) {
        	args.put(KEY_ORDER,  newOrder);
        }

        //delete the note if it's blank, else update it as normal
        if (description.equals ("")) {
        	return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
        } else {
        	return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
        }//if
    }//updateNote
}
