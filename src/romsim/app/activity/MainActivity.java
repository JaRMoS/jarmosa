package romsim.app.activity;

import rmcommon.io.AModelManager;
import rmcommon.io.WebModelManager;
import romsim.app.R;
import romsim.app.io.AssetModelManager;
import romsim.app.io.SDModelManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

public class MainActivity extends Activity {
	
	/**
	 * Dialog ID for the model download url dialog
	 */
	public static final int DOWNLOAD_DIALOG_ID = 1;
	
	/**
	 * Dialog ID for the "no sd card access" dialog 
	 */
	public static final int NO_SD_ID = 2;
	
	public static AModelManager modelmng;

	private Dialog downloadDialog;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainpage);
        
     // Add listener to the Solve button
     		Button btn = (Button) findViewById(R.id.btnAssets);
     		btn.setOnClickListener(new View.OnClickListener() {
     			public void onClick(View view) {
     				Intent intent = new Intent(MainActivity.this,
     						ModelListActivity.class);
     				modelmng = new AssetModelManager(MainActivity.this);
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
     				Intent intent = new Intent(MainActivity.this,
     						ModelListActivity.class);
     				modelmng = new SDModelManager();
     				startActivityForResult(intent, 0);
     			}
     		});
     		ImageButton ibtn = (ImageButton) findViewById(R.id.btnDownload);
     		ibtn.setOnClickListener(new View.OnClickListener() {
     			public void onClick(View view) {
     				showDialog(DOWNLOAD_DIALOG_ID);
     			}
     		});
    }
    
    @Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case DOWNLOAD_DIALOG_ID:
			downloadDialog = new Dialog(this);
			downloadDialog.setContentView(R.layout.download_dialog);
			downloadDialog.setTitle("Connect to server");
			downloadDialog.setCancelable(false);

			// When the download button is pressed, we read the offline_data
			// files from the specified URL
			Button downloadButton = (Button) downloadDialog
					.findViewById(R.id.downloadButton);
			downloadButton.setOnClickListener(new View.OnClickListener() {

				public void onClick(View view) {

					// Get the directory_name from the EditText object
					EditText urlEntry = (EditText) downloadDialog
							.findViewById(R.id.urlEntry);

					// Dismiss the URL specification dialog
					dismissDialog(DOWNLOAD_DIALOG_ID);
					
					Intent intent = new Intent(MainActivity.this,
     						ModelListActivity.class);
     				
     				String url = urlEntry.getText().toString().trim();
					// Add forwardslash if not entered
					if (!url.endsWith("/")) url += "/";
					modelmng = new WebModelManager(url);
     				startActivityForResult(intent, 0);
				}
			});

			// Add listener to cancel download
			Button quitDownloadButton = (Button) downloadDialog
					.findViewById(R.id.quitDownloadButton);
			quitDownloadButton.setOnClickListener(new View.OnClickListener() {

				public void onClick(View view) {
					dismissDialog(1);
				}

			});

			dialog = downloadDialog;
			break;
		case NO_SD_ID:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Could not ensure rbappmit-SDcard folder.")
					.setCancelable(false)
					.setNeutralButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
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
}