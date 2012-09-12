package com.callysto.devin.easygotexter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.zip.DataFormatException;

import com.callysto.devin.easygotexter.util.DbAdapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;

public class ImportExportActivity extends Activity {

	//regular fields
    private DbAdapter mDbHelper;
    String notes;
    String compatVersion; //compatibility version of export/imports
    
    //Views
    EditText exportText, importText;
    AlertDialog alert;
    AlertDialog alertWithWait; //has two buttons, one which does the import
    
    //applicable preference values
    boolean deleteOld;
    boolean warningEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.importexport_layout);
        setTitle(R.string.importexport);
        
    	//sets up alerts
    	alert = new AlertDialog.Builder(this).create();
    	alertWithWait = new AlertDialog.Builder(this).create ();
        
        mDbHelper = new DbAdapter(this);
        mDbHelper.open();
        exportText = (EditText) findViewById(R.id.export_text);
        importText = (EditText) findViewById(R.id.import_text);
        
    	Resources res = getResources ();
    	compatVersion = res.getString(R.string.export_compat_version);
    }//onCreate
    
    public void onResume () {
    	super.onResume ();
    	
    	getPrefs ();
        if (warningEnabled) {
        	Resources res = getResources ();
        	String helpmessage = res.getString (R.string.importexport_help, 
        			res.getString(R.string.export_compat_version));
        	String title = res.getString (R.string.importexport_title);
        	alert (title, helpmessage);
        }//if
    }//onResume

    private void getPrefs () {
    	getPrefs ("warning");
    	getPrefs ("deleteold");
    }
    private void getPrefs (String pref) {
    	if (pref.equals("warning")) {
    		SharedPreferences prefs = PreferenceManager
        		.getDefaultSharedPreferences(getBaseContext());
    		warningEnabled = prefs.getBoolean("importexport_warning", true);
    	} else if (pref.equals("deleteold")) {
    		SharedPreferences prefs = PreferenceManager
    			.getDefaultSharedPreferences(getBaseContext ());
    		deleteOld = prefs.getBoolean("delete_old_on_import", false);
    	}
    		
    }//getPrefs
    
    public String getNotes () {
    	//get file compatibility version in case I update the export format
    	String retval = "EasyGoTexter numbers file version " + compatVersion + "\n";
    	
    	//get values and store them in a string
    	//fetchAllNotes *should* order them by _order
    	Cursor c = mDbHelper.fetchAllNotes ();
        for (c.moveToFirst (); c.isAfterLast() == false; c.moveToNext ()) {
        	retval += c.getString(c.getColumnIndexOrThrow(DbAdapter.KEY_NUMBER)) + " ";
        	retval += c.getString(c.getColumnIndexOrThrow(DbAdapter.KEY_RECIP)) + " ";
        	retval += c.getString(c.getColumnIndexOrThrow(DbAdapter.KEY_DESC)) + "\n";
        }
        c.close();
    	return retval;
    }//getNotes
    
    public String getPath (String type) {
    	if (type.equals("export")) {
    		return exportText.getText().toString();
    	} else if (type.equals ("import")) {
    		return importText.getText().toString();
    	} else {
    		return null;
    	}//if
    }//getPath
    
    //called when the export button is clicked
    public void export (View view) {
    	String path = getPath ("export");
    	if (! path.substring (0,7).equals("/sdcard")) {
    		alert ("Error", "Please begin your filename with '/sdcard'");
    		return;
    	}
    	
    	File file = new File (path);
    	if (file.exists()) {
    		alert ("Error", "Please choose a file that doesn't already exist.");
    		return;
    	}
    	
    	String notes = getNotes ();
    	
    	ExportTask exporter = new ExportTask ();
    	exporter.execute(notes, path);
    	return;
    }//export
    
    private class ExportTask extends AsyncTask<String, String, Void> {
		@Override
		protected Void doInBackground(String... params) {
			String s = params [0];
			String path = params [1];
			
	    	final String toWrite = s;
	    	
			BufferedWriter writer = null;
				try {
					//the true makes sure to append to trace.txt
					writer = new BufferedWriter (new FileWriter (path, false));
					writer.write(toWrite);
				} catch (Exception e) {
					publishProgress ("Error", "error writing data to " + path);
				} finally {
			           try {
			                if (writer != null) {
			                    writer.flush();
			                    writer.close();
			                }
			            } catch (IOException e) {
			                publishProgress ("Error", "Error - IOException when closing file");
			            }//try-catch
				}//try-catch
				publishProgress ("Success", "Your numbers have been exported with version code " + compatVersion);
				return null;
	    }//doInBackground
	    
	    public void onProgressUpdate (String... update) {
	    	alert (update[0], update[1]);
	    }//onProgressUpdate
    }//ExportTask class
    
    public void importNotes (View view) {
        alertWithWait.setTitle("About to import");
        alertWithWait.setMessage (deleteOrNot ());
        alertWithWait.setButton(AlertDialog.BUTTON_POSITIVE, "Import", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				actuallyImportNotes ();
			}
		});
        alertWithWait.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int which) {
        		return;
        	}
        });
        alertWithWait.show ();
    }
    
    //string representation of whether messages will be deleted
    public String deleteOrNot () {
    	return (deleteOld)  ? "Your old notes will be deleted." 
    						: "Your old notes will not be deleted.";
    }//deleteOrNot
    
    public void actuallyImportNotes () {
    	String path = getPath ("import");
    	File file = new File (path);
    	if (! file.exists()) {
    		alert ("Error", "File not found on sdcard.");
    	}//if
    	
    	ImportTask importer = new ImportTask ();
    	importer.execute (path);
    }//importNotes
    
    private class ImportTask extends AsyncTask<String, String, Void> {
		@Override
		protected Void doInBackground(String... params) {
			String path = params [0];
			BufferedReader reader = null;
			try {
				reader = new BufferedReader (new FileReader (path));
			} catch (FileNotFoundException e) {
				publishProgress ("Error", "File doesn't exist");
				return null;
			}//try-catch
			
			//verify first line is formatted right and has correct version number
			try {
				verifyVersion (reader.readLine ());
			} catch(DataFormatException e) {
				publishProgress ("Error", "File has incorrect version number or malformed first line");
				return null;
			} catch (IOException e) {
				publishProgress ("Error", "Couldn't read that file!");
			}//try-catch
			
			if (deleteOld) {
				mDbHelper.deleteAllNotes ();
			}//if
			
			try {
				String line;
				while ((line = reader.readLine ()) != null) {
					String [] tokens = tokenize (line);
					String number = tokens [0];
					String recipient = tokens [1];
					
					String desc = tokens [2];
					for (int i = 3; i < tokens.length; i += 1) {
						desc += " ";
						desc += tokens [i];
					}//for
					
					mDbHelper.createNote (number, desc, recipient);
				}//while
			} catch (IOException e) {
				publishProgress ("Error", "Error reading file, some numbers may not have been read.");
			}//try-catch
			publishProgress ("Success", "Your numbers have been imported!");
			return null;
		}//doInBackground
		
		public void verifyVersion (String line) throws DataFormatException {
			//if anything goes wrong in parsing, throw e
			DataFormatException e = new DataFormatException ();
			
			
			//tokenize () returns null if too few/too many tokens
			String [] tokens = tokenize (line);
			if (tokens.length < 5 || 
				tokens.length > 5 ||
				! tokens [0].equals("EasyGoTexter") ||
				! tokens [1].equals("numbers") ||
				! tokens [2].equals("file") ||
				! tokens [3].equals("version") ||
				! tokens [4].equals(compatVersion)) throw e;
			return;
		}//verifyVersion
		
		public String [] tokenize (String line) {
			StringTokenizer st = new StringTokenizer (line);
			String [] returnArray = new String [st.countTokens()];
			
			for (int i = 0; st.hasMoreTokens (); i += 1) {
				returnArray [i] = st.nextToken ();
			}

			return returnArray;
		}//tokenize
	    
	    public void onProgressUpdate (String... update) {
	    	alert (update[0], update [1]);
	    }//onProgressUpdate
    }//ExportTask class
    
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
}//ImportExportActivity
