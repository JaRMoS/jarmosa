/**
 * 
 */
package kermor.app;

import java.io.IOException;

import org.apache.commons.math.linear.RealMatrix;

import kermor.java.KerMorException;
import kermor.java.ReducedModel;
import kermor.java.io.MathObjectReader.MathReaderException;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

/**
 * @author Ernst
 * 
 */
public class SimulationActivity extends Activity {

	private ReducedModel rm;

	/**
	 * ProgressDialog to display while downloading data.
	 */
	private ProgressDialog pd;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mainpage);

		pd = ProgressDialog.show(SimulationActivity.this, "Loading model data",
				"", true, true, new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						// delete_downloaded_files();
						setResult(0);
						finish();
					}
				});

		final ModelManagerProgressHandler progressHandler = new ModelManagerProgressHandler() {
			public void handleMessage(Message msg) {
				pd.setMessage(msg.getData().getString("file") + "...");
			}
		};
		KerMORDSAppActivity.modelmng.addProgressHandler(progressHandler);
		
		final Handler h = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				pd.dismiss();
				KerMORDSAppActivity.modelmng.removeProgressHandler(progressHandler);
				
				Toast.makeText(SimulationActivity.this, "Model successfully loaded!", Toast.LENGTH_LONG).show();

//				RealMatrix res = null;
//				try {
//					 res = rm.simulate(null);
//				} catch (KerMorException e) {
//					Log.e("SimulationActivity","Error simulating.",e);
//					//finish();
//				}
//				Log.d("SimulationActivity", res.toString());
				// Display stuff!
			}

		};

		(new Thread() {

			@Override
			public void run() {
				try {
				rm = ReducedModel.load(KerMORDSAppActivity.modelmng);
				} catch (Exception e) {
					Log.e("SimulationActivity","Error loading reduced model.",e);
				}
				h.sendEmptyMessage(0);
			}

		}).start();

	}

}
