package com.callysto.devin.easygotexter;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.callysto.devin.easygotexter.util.DbAdapter;

public class EditorActivity extends Activity {

    private EditText mNumberText;
    private EditText mDescText;
    private EditText mRecipText;
    private Button confirmButton;
    private Long mRowId;
    private DbAdapter mDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDbHelper = new DbAdapter(this);
        mDbHelper.open();

        setContentView(R.layout.note_edit);
        setTitle(R.string.edit_note);

        mNumberText = (EditText) findViewById (R.id.number);
        mDescText = (EditText) findViewById (R.id.description);
        mRecipText = (EditText) findViewById (R.id.recip);

        confirmButton = (Button) findViewById(R.id.confirm);

        mRowId = (savedInstanceState == null) ? null :
            (Long) savedInstanceState.getSerializable(DbAdapter.KEY_ROWID);
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();

			//quicktext function will send the number as an extra
			String number = extras.getString(DbAdapter.KEY_NUMBER);
			if (number != null) {
				mNumberText.setText(number);
			}//if

			mRowId = extras.getLong(DbAdapter.KEY_ROWID);
			if (mRowId == 0) {
				mRowId = null;
			}//if
	    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
	    			getBaseContext());
			mRecipText.setText(prefs.getString("default_recip_pref", "57555"));
		}

		populateFields();

		//When the confirm button is pressed...
        confirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	setResult(RESULT_OK);
                finish();
            }
        });
        
        mDescText.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
				if (s.toString().trim().length() < 1) {
					confirmButton.setText("Cancel");
				} else {
					confirmButton.setText("Confirm");
				}
			}
			public void beforeTextChanged(CharSequence a0, int a1, int a2, int a3) {}
			public void onTextChanged(CharSequence a0, int a1, int a2, int arg3) {}
        });
    }

    private void populateFields() {
        if (mRowId != null) {
        	//set up
            Cursor note = mDbHelper.fetchNote(mRowId);
            startManagingCursor(note);
            
            //fill the fields
            mNumberText.setText(note.getString(
                    note.getColumnIndexOrThrow(DbAdapter.KEY_NUMBER)));
            mDescText.setText(note.getString(
                    note.getColumnIndexOrThrow(DbAdapter.KEY_DESC)));
            mRecipText.setText(note.getString(
            		note.getColumnIndexOrThrow(DbAdapter.KEY_RECIP)));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState();
        outState.putSerializable(DbAdapter.KEY_ROWID, mRowId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveState();
        mDbHelper.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDbHelper.open();
        populateFields();
    }

    private void saveState() {
        String number = mNumberText.getText().toString();
        String description = mDescText.getText().toString();
        String recipient = mRecipText.getText().toString();

        if (mRowId == null) {
            long id = mDbHelper.createNote(number, description, recipient);
            if (id > 0) {
                mRowId = id;
            }
        } else {
            mDbHelper.updateNote(mRowId, number, description, recipient);
        }
        mDbHelper.close();
    }//saveState

}//Editor class
