//    rbAPPmit: An Android front-end for the Certified Reduced Basis Method
//    Copyright (C) 2010 David J. Knezevic and Phuong Huynh
//
//    This file is part of rbAPPmit
//
//    rbAPPmit is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    rbAPPmit is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with rbAPPmit.  If not, see <http://www.gnu.org/licenses/>. 

package romsim.app.activity;

import rmcommon.io.AModelManager;
import romsim.app.io.AssetModelManager;
import romsim.app.io.SDModelManager;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

/**
 * Changes by:
 * @author Daniel Wirtz
 * @date Aug 23, 2011
 * 
 * This was the former SimpleBrowserActivity class in rbappmit.
 *
 */
public class BrowseActivity extends Activity {
	private WebView browser;
	private String url;

	/**
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		browser = new WebView(this);
		setContentView(browser);

		AModelManager m = MainActivity.modelmng;
		String file = m.getModelXMLTagValue("infohtml"); 
		if (m instanceof SDModelManager) {
			url = "file://" + SDModelManager.SDModelsDir + "/" + m.getModelDir() + "/" + file;
		} else if (m instanceof AssetModelManager) {
			url = "file:///android_asset/" + m.getModelDir() + "/" + file;
		}

		// if info page was not supplied/another error occurs while loading,
		// redirect user to home site
		browser.setWebViewClient(new WebViewClient() {
			public void onReceivedError(WebView view, int errorCode,
					String description, String failingUrl) {
				Toast.makeText(
						BrowseActivity.this,
						"Sorry, no informational page was supplied with this problem",
						Toast.LENGTH_LONG).show();
			}
		});

		// removes white scrollbar from view while keeping scrolling
		// funcionality
		browser.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
		browser.loadUrl(url);

		while (browser.getUrl() == null) {

		}
		Log.d("browser", browser.getUrl());
	}

	/**
	 * @see android.app.Activity#onBackPressed()
	 */
	public void onBackPressed() {
		// need to tell parent activity to close all activities
		getParent().onBackPressed();
	}

}