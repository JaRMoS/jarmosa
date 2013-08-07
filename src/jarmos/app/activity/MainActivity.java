package jarmos.app.activity;

import jarmos.app.Const;
import jarmos.app.R;
import jarmos.app.io.AssetModelManager;
import jarmos.app.io.SDModelManager;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * This is the main activity class and entry point of the app
 * 
 * @author Daniel Wirtz
 * @date Aug 23, 2011
 * 
 * @todo Check out-of-memory errors
 * @todo New orientation causes model to be re-loaded in RBActivity
 * @todo model list as single list with descriptions
 * @todo fix visualization black moments for rb models
 */
public class MainActivity extends Activity {

	/**
	 * Dialog ID for the model download url dialog
	 */
	public static final int DOWNLOAD_DIALOG_ID = 1;

	/**
	 * Dialog ID for the "no sd card access" dialog
	 */
	public static final int NO_SD_ID = 2;

	/**
	 * Options dialog ID
	 */
	public static final int OPTIONS_ID = 3;

	/**
	 * The ModelManager created for the current MainActivity.
	 */
	// public static AModelManager modelmng;

	private Dialog downloadDialog;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mainpage);

		Const.APP_DATA_DIRECTORY = getApplicationInfo().dataDir + "/files";

		// testwas();

		// Add listener to the Solve button
		Button btn = (Button) findViewById(R.id.btnAssets);
		btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				Intent intent = new Intent(MainActivity.this, ModelListActivity.class);
				intent.putExtra(Const.EXTRA_MODELMANAGER_CLASSNAME, "AssetModelManager");
				// intent.putExtras(getIntent().getExtras());
				startActivityForResult(intent, 0);
			}
		});
		btn = (Button) findViewById(R.id.btnSDCard);
		btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {

				if (!SDModelManager.ensureSDDir()) {
					showDialog(NO_SD_ID);
					return;
				}

				Intent intent = new Intent(MainActivity.this, ModelListActivity.class);
				intent.putExtra(Const.EXTRA_MODELMANAGER_CLASSNAME, "SDModelManager");
				startActivityForResult(intent, 0);
			}
		});
		Button ibtn = (Button) findViewById(R.id.btnDownload);
		ibtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				showDialog(DOWNLOAD_DIALOG_ID);
			}
		});
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		switch (id) {
		case DOWNLOAD_DIALOG_ID:
			downloadDialog = new Dialog(this);
			downloadDialog.setContentView(R.layout.download_dialog);
			downloadDialog.setTitle("Connect to server");
			downloadDialog.setCancelable(false);

			// When the download button is pressed, we read the offline_data
			// files from the specified URL
			Button downloadButton = (Button) downloadDialog.findViewById(R.id.downloadButton);
			downloadButton.setOnClickListener(new View.OnClickListener() {

				public void onClick(View view) {

					// Get the directory_name from the EditText object
					EditText urlEntry = (EditText) downloadDialog.findViewById(R.id.urlEntry);

					URL u = null;
					try {
						// Add forwardslash if not entered
						// if (!url.endsWith("/")) url += "/";
						u = new URL(urlEntry.getText().toString().trim());
					} catch (MalformedURLException e) {
						Toast.makeText(MainActivity.this, R.string.invalidURL, Toast.LENGTH_SHORT);
						return;
					}

					// Dismiss the URL specification dialog
					dismissDialog(DOWNLOAD_DIALOG_ID);

					Intent intent = new Intent(MainActivity.this, ModelListActivity.class);
					intent.putExtra(Const.EXTRA_MODELMANAGER_CLASSNAME, "WebModelManager");
					intent.putExtra("URL", u);
					intent.putExtra("modelCaching", true);

					startActivityForResult(intent, 0);
				}
			});

			// Add listener to cancel download
			Button quitDownloadButton = (Button) downloadDialog.findViewById(R.id.quitDownloadButton);
			quitDownloadButton.setOnClickListener(new View.OnClickListener() {

				public void onClick(View view) {
					dismissDialog(1);
				}

			});

			dialog = downloadDialog;
			break;
		case NO_SD_ID:
			builder.setMessage("Could not ensure rbappmit-SDcard folder.").setCancelable(false)
					.setNeutralButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.dismiss();
						}
					});
			dialog = builder.create();
			break;
		case OPTIONS_ID:
			final SharedPreferences p = getSharedPreferences(Const.PREFERENCES_FILENAME, 0);
			boolean[] bools = new boolean[] { p.getBoolean(Const.PREF_MODELCACHING, false),
					p.getBoolean(Const.PREF_MODELCACHING_OVERWRITE, false) };

			builder = new AlertDialog.Builder(MainActivity.this);
			builder.setTitle("JaRMoSA options");
			builder.setMultiChoiceItems(new String[] { "Enable model caching", "Overwrite existing models" }, bools,
					new DialogInterface.OnMultiChoiceClickListener() {
						public void onClick(DialogInterface dialog, int item, boolean checked) {
							if (item == 0) {
								p.edit().putBoolean(Const.PREF_MODELCACHING, checked).commit();
							} else {
								p.edit().putBoolean(Const.PREF_MODELCACHING_OVERWRITE, checked).commit();
							}
						}
					});
			builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					dialog.dismiss();
				}
			});
			dialog = builder.create();
			break;
		default:
			dialog = null;
			break;
		}
		return dialog;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId() == R.id.mm_settings) {
			showDialog(OPTIONS_ID);
			return true;
		} else {
			return super.onMenuItemSelected(featureId, item);
		}
	}

	// populate main menu
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@SuppressWarnings("unused")
	private void testwas() {
		AssetModelManager m = new AssetModelManager(getApplicationContext());
		try {
			m.useModel("aghdemo");

			ClassLoader cl = m.getClassLoader();
			Class<?> c = cl.loadClass("AffineFunctions");
			c = cl.loadClass("AffineFunctions");

			Object ci = c.newInstance();
			int a = 0;

			Method meth = c.getMethod("get_n_F_functions");
			Object res = meth.invoke(ci);
			a = (Integer) res;
			System.out.println("a=" + a);

			m.useModel("demo1");
			ClassLoader cl2 = m.getClassLoader();

			c = cl.loadClass("AffineFunctions");
			ci = c.newInstance();
			res = meth.invoke(ci);
			a = (Integer) res;
			System.out.println("a=" + a);

			c = cl2.loadClass("AffineFunctions");
			meth = c.getMethod("get_n_F_functions");
			ci = c.newInstance();
			res = meth.invoke(ci);
			a = (Integer) res;
			System.out.println("a=" + a);

		} catch (Exception e) {
			Log.e("testwas", "DOOMED!", e);
			e.printStackTrace();
			finish();
		}
	}

	// private void startMMService(String classname, String extra) {
	// Intent si = new Intent(getApplicationContext(),
	// ModelManagerService.class);
	// si.putExtra(ModelManagerService.CLASSNAME_EXTRA, classname);
	// if (extra != null) {
	// si.putExtra("URL", extra);
	// }
	// if (startService(si) == null) {
	// Log.e("MainActivity", "Starting the ModelManagerService failed.");
	// }
	// }
}