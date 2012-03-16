/**
 * 
 */
package jarmos.app.activity.kermor;

import jarmos.app.Const;
import jarmos.app.ModelManagerProgressHandler;
import jarmos.app.ParamBars;
import jarmos.app.R;
import jarmos.io.AModelManager;
import jarmos.io.AModelManager.ModelManagerException;
import kermor.java.KerMorException;
import kermor.java.ReducedModel;

import org.apache.commons.math.linear.RealMatrix;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.Toast;

/**
 * @author Daniel Wirtz @date 2011-09-24
 * 
 */
public class SimulationActivity extends Activity {

	public static ReducedModel rm;

	/**
	 * ProgressDialog to display while downloading data.
	 */
	private ProgressDialog pd;

	private AModelManager mng = null;
	private ParamBars pb = null;
	private RealMatrix res = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.kermor_main);

		// Create model manager instance to use
		try {
			mng = Const.getModelManager(getApplicationContext(), getIntent());
		} catch (ModelManagerException e) {
			Log.e("SimulationActivity", "Creation of ModelManager failed", e);
			finish();
			return;
		}

		pd = ProgressDialog.show(SimulationActivity.this, "Loading model data",
				"", true, true, new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						// delete_downloaded_files();
						setResult(0);
						finish();
					}
				});

		final Handler sh = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				pd.dismiss();
				
				Toast.makeText(SimulationActivity.this,
						"Model successfully simulated!", Toast.LENGTH_LONG).show();
			}
		};
		
		// Add listener to the Solve button
		Button solveButton = (Button) findViewById(R.id.solveButton);
		solveButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				pd = ProgressDialog.show(SimulationActivity.this, "",
						"Solving...");
				new Thread() {
					public void run() {
						try {
							rm.simulate(rm.params.getCurrent(),rm.system.currentInput());
							sh.sendEmptyMessage(0);
						} catch (KerMorException e) {
							Log.e("SimulationActivity", "Error simulating", e);
						}
					}
				}.start();
			}

		});

		final ModelManagerProgressHandler progressHandler = new ModelManagerProgressHandler() {
			public void handleMessage(Message msg) {
				pd.setMessage(msg.getData().getString("file") + "...");
			}
		};
		mng.addMessageHandler(progressHandler);

		final Handler h = new Handler() {
			@Override
			public void handleMessage(Message msg) {

				pb = new ParamBars(SimulationActivity.this, rm.params);
				pb.createBars((TableLayout) findViewById(R.id.paramLayout));

				pd.dismiss();
				mng.removeMessageHandler(progressHandler);

				Toast.makeText(SimulationActivity.this,
						"Model successfully loaded!", Toast.LENGTH_LONG).show();

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

		(new Thread() {

			@Override
			public void run() {
				try {
					rm = new ReducedModel();
					rm.loadOfflineData(mng);
				} catch (Exception e) {
					Log.e("SimulationActivity", "Error loading reduced model.",
							e);
				}
				h.sendEmptyMessage(0);
			}

		}).start();

	}

}
