/**
 * 
 */
package romsim.app.activity.kermor;

import kermor.java.ReducedModel;
import rmcommon.io.AModelManager;
import rmcommon.io.AModelManager.ModelManagerException;
import romsim.app.Const;
import romsim.app.ModelManagerProgressHandler;
import romsim.app.R;
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

	private AModelManager mng = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mainpage);

		// Create model manager instance to use
		try {
			mng = Const.getModelManager(getApplicationContext(), getIntent());
		} catch (ModelManagerException e) {
			Log.e("SimulationActivity", "Creation of ModelManager failed", e);
			finish();
			return;
		}

		pd = ProgressDialog.show(SimulationActivity.this, "Loading model data", "", true, true, new OnCancelListener(){
			@Override
			public void onCancel(DialogInterface dialog) {
				// delete_downloaded_files();
				setResult(0);
				finish();
			}
		});

		final ModelManagerProgressHandler progressHandler = new ModelManagerProgressHandler(){
			public void handleMessage(Message msg) {
				pd.setMessage(msg.getData().getString("file") + "...");
			}
		};
		mng.addMessageHandler(progressHandler);

		final Handler h = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				pd.dismiss();
				mng.removeMessageHandler(progressHandler);

				Toast.makeText(SimulationActivity.this, "Model successfully loaded!", Toast.LENGTH_LONG).show();

				// RealMatrix res = null;
				// try {
				// res = rm.simulate(null);
				// } catch (KerMorException e) {
				// Log.e("SimulationActivity","Error simulating.",e);
				// //finish();
				// }
				// Log.d("SimulationActivity", res.toString());
				// Display stuff!
			}

		};

		(new Thread(){

			@Override
			public void run() {
				try {
					rm = ReducedModel.load(mng);
				} catch (Exception e) {
					Log.e("SimulationActivity", "Error loading reduced model.", e);
				}
				h.sendEmptyMessage(0);
			}

		}).start();

	}

}
