package com.callysto.devin.easygotexter.forthcoming;

import android.graphics.Bitmap;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

public class MyWebViewClient extends WebViewClient {
	//pointer back to TextView that resides at the top of the screen
	private TextView tvPointer;
	
	public MyWebViewClient (TextView tv) {
		super ();
		tvPointer = tv;
	}//constructor
	
     public void onPageStarted(WebView view, String url, Bitmap favicon) {
    	 tvPointer.setText(url);
     }//onPageStarted
}
