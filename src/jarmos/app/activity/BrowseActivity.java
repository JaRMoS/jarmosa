package jarmos.app.activity;

import jarmos.app.Const;
import jarmos.io.AModelManager;
import jarmos.io.AModelManager.ModelManagerException;

import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

/**
 * An activity which displays URLs in a simple browser
 * 
 * This class has been taken from the original @ref rbappmit package and modified to fit into the current JaRMoS
 * framework, this was the former SimpleBrowserActivity class in @ref rbappmit.
 * 
 * @author Daniel Wirtz
 * @date Aug 23, 2011
 * 
 */
public class BrowseActivity extends Activity {
	private WebView browser;

	/**
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		browser = new WebView(this);
		setContentView(browser);

		// Create model manager instance to use
		AModelManager m = null;
		try {
			m = Const.getModelManager(getApplicationContext(), getIntent());
		} catch (ModelManagerException e) {
			Log.e("BrowseActivity", "Creation of ModelManager failed", e);
			finish();
			return;
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

		String file = m.getModelXMLTagValue("infohtml");
		if (file != null) {
			browser.loadUrl(m.getModelURI() + "/" + file);
			Log.d("browser", m.getModelURI() + "/" + file);
		} else {
			String img = m.getModelXMLTagValue("model.description.image");
			img = img != null ? "<img src='" + m.getModelURI() + "/" + img + "' alt=''/>" : "";
			String html = "<html><body bgcolor='Black'><center><font color='white'>Sorry, this model does not have any information page associated with it."
					+ img + "</font></center>" + "</body></html>";
			try {
				FileOutputStream f = openFileOutput("noinfo.html", Context.MODE_PRIVATE);
				f.write(html.getBytes());
				f.close();
			} catch (IOException e) {
				Log.e("BrowseActivity","Error saving a temporary html file", e);
				return;
			}
			browser.loadUrl("file://"+Const.APP_DATA_DIRECTORY + "/noinfo.html");
//			browser.loadData(html, "text/html", "utf8");
		}
	}

	/**
	 * @see android.app.Activity#onBackPressed()
	 */
	public void onBackPressed() {
		// need to tell parent activity to close all activities
		getParent().onBackPressed();
	}

}