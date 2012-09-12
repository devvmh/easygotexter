package com.callysto.devin.easygotexter.forthcoming;

import com.callysto.devin.easygotexter.R;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.TextView;

public class GRTMobileActivity extends Activity {
	public TextView urlViewer;
	public WebView viewPage;
	public String url;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.grtmobile);
        
        Resources res = getResources ();
        
        viewPage = (WebView) findViewById(R.id.webView);
        urlViewer = (TextView) findViewById (R.id.urlTextView);
        url = res.getString(R.string.grtmobile_url);

        
        viewPage.setWebViewClient(new MyWebViewClient(urlViewer));
        viewPage.getSettings().setJavaScriptEnabled(true);
        viewPage.loadUrl(url);
    }
}