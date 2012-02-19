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

package romsim.app.activity.rb;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.text.DecimalFormat;
import java.util.Arrays;

import rb.java.Const;
import rb.java.RBContainer;
import rb.java.RBSystem;
import rb.java.SystemType;
import rb.java.TransientRBSystem;
import rmcommon.Parameters;
import rmcommon.io.AModelManager;
import rmcommon.io.AModelManager.ModelManagerException;
import rmcommon.io.CachingModelManager;
import rmcommon.io.WebModelManager;
import rmcommon.visual.ColorGenerator;
import romsim.app.ModelManagerProgressHandler;
import romsim.app.ParamBars;
import romsim.app.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TextView;

/**
 * This is the main Activity class for the app. This Activity handles
 * downloading the stored data, initializing the systems and performing the RB
 * solve.
 * 
 * Copyright (C) 2010 David J. Knezevic and Phuong Huynh, Author made changes to
 * the code.
 * 
 * @author Daniel Wirtz
 * @date Aug 10, 2011
 * 
 */
public class RBActivity extends Activity {

	// Dialog IDs
	static final int DOWNLOAD_DIALOG_ID = 0;
	static final int PROGRESS_DIALOG_ID = 1;
	static final int DOWNLOAD_FAILED_DIALOG_ID = 2;
	static final int RB_SOLVE_DIALOG_ID = 3;
	static final int LOAD_DEMO_DIALOG_ID = 4;
	static final int SWEEP_DIALOG_ID = 5;
	static final int PARAM_DIALOG_ID = 6;

	// Activity ID
	static final int SELECT_PROBLEM_TYPE = 0;

	// String for log printing
	static final String DEBUG_TAG = "RBActivity";

	/**
	 * ProgressDialog to display while downloading data.
	 */
	private ProgressDialog pd;

	/**
	 * Array of TextViews and SeekBars for constructing the parameter selection
	 */
	// private TextView[] mParamLabels;
	// private SeekBar[] mParamBars;
	// private Button[] mParamButtons;

	/**
	 * Member variable to store index number of currently selected parameter
	 * button
	 */
	// private int paramButtonIndex;
	// private TextView paramInputField;

	/**
	 * The online N constructed by the GUI. We use this value when we call
	 * RB_solve.
	 * 
	 * Changed the default value to 1 as simulations with zero N makes little
	 * sense. Up to now, an error was thrown when N=1 from the model and thus
	 * the seekBar could not be moved, never invoking the onProgressChanged
	 * event and hence leaving this value at zero.
	 */
	public static int mOnlineNForGui = 1;

	/**
	 * The current parameter constructed by the GUI. We set the RBSystem's
	 * current parameter to this before performing a solve.
	 */
	// public static double[] mCurrentParamForGUI;

	/**
	 * The index for the parameter sweep, -1 implies no sweep.
	 */
	// public static int mSweepIndex;

	/**
	 * The name of the jar file (containing compiled files in .dex form) that we
	 * download from the server.
	 */
	protected String jarFileName = "AffineFunctions.jar";
	/**
	 * The corresponding dex file we create locally.
	 */
	private String dexFileName = "AffineFunctions.dex";

	/**
	 * The RB Container with all the system and model data (from JRB)
	 */
	public static RBContainer rb;

	/**
	 * The color generator used to color the field values
	 */
	public static ColorGenerator cg;

	private AModelManager mng;

	private ParamBars pb;

	public static FloatBuffer floatBuf;
	public static ShortBuffer shortBuf;
	
	private Bundle bundle = null;

	/**
	 * Allocates short and float buffers for the rendering process and sets the
	 * position to zero.
	 * 
	 */
	private void allocateBuffer() {
		int SHORT_MAX = 250000;
		int FLOAT_MAX = 1000000;

		Log.d("RBActivity", "Allocating GL short buffer:" + SHORT_MAX * 2 + " bytes");
		ByteBuffer vbb = ByteBuffer.allocateDirect(SHORT_MAX * 2);
		vbb.order(ByteOrder.nativeOrder());
		shortBuf = vbb.asShortBuffer();
		shortBuf.position(0);

		Log.d("RBActivity", "Allocating GL float buffer:" + FLOAT_MAX * 4 + " bytes");
		ByteBuffer fbb = ByteBuffer.allocateDirect(FLOAT_MAX * 4);
		fbb.order(ByteOrder.nativeOrder());
		floatBuf = fbb.asFloatBuffer();
		floatBuf.position(0);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rb_main);

		// Create model manager instance to use
		try {
			mng = romsim.app.Const.getModelManager(getApplicationContext(), getIntent());
		} catch (ModelManagerException e) {
			Log.e("RBActivity", "Creation of ModelManager failed", e);
			finish();
			return;
		}

		rb = new RBContainer();
		cg = new ColorGenerator();
		allocateBuffer();

		// Add listener to the Solve button
		Button solveButton = (Button) findViewById(R.id.solveButton);
		solveButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				pd = ProgressDialog.show(RBActivity.this, "", "Solving...");
				new SolveThread().start();
			}

		});

		// Attach a listener to onlineNSeekBar
		SeekBar onlineNSeekBar = (SeekBar) findViewById(R.id.onlineNSeekbar);
		onlineNSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				mOnlineNForGui = (progress + 1);
				TextView currentOnlineNView = (TextView) findViewById(R.id.currentOnlineN);
				currentOnlineNView.setText("Online N =  " + mOnlineNForGui);
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		// Now, call the ListActivity to select problem
		// Intent intent = new Intent(RBActivity.this,
		// ProbSelectionActivity.class);
		// RBActivity.this.startActivityForResult(intent, SELECT_PROBLEM_TYPE);

		AModelManager m = mng;
		m.addMessageHandler(new ModelManagerProgressHandler() {

			/**
			 * @see android.os.Handler#handleMessage(android.os.Message)
			 */
			@Override
			public void handleMessage(Message msg) {
				pd.setMessage(msg.getData().getString("file") + "...");
			}

		});

		Log.d("DEBUG_TAG", "Loading model " + m.getModelDir());
		String op = "Loading";
		if (m instanceof CachingModelManager) {
			op = "Caching";
		} else if (m instanceof WebModelManager) {
			op = "Downloading";
		}
		String title = op + " " + m.getModelDir() + "...";

		pd = ProgressDialog.show(RBActivity.this, title, "", true, true, new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				delete_downloaded_files();
				setResult(0);
				finish();
			}
		});
		// Start the model loading
		new ModelLoader(downloadHandler).start();
	}

	/**
	 * @see android.app.Activity#onBackPressed()
	 */
	public void onBackPressed() {
		// need to tell parent activity to close all activities
		getParent().onBackPressed();
	}

	/** Called when the activity is destroyed. */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(DEBUG_TAG, "onDestroy() called");
		// Clean up the files that were downloaded
		delete_downloaded_files();
	}

	/**
	 * This function takes care of constructing the dialogs that pop up.
	 */
	protected Dialog onCreateDialog(int id) {

		Dialog dialog;
		AlertDialog.Builder builder;
		RBSystem s = rb.mRbSystem;

		switch (id) {

		case DOWNLOAD_FAILED_DIALOG_ID: {

			String title = "Failed loading the model.";
			Log.d(DEBUG_TAG, "Error loading model, modeldir: " + mng.getModelDir());
			builder = new AlertDialog.Builder(this);
			builder.setMessage(title).setCancelable(false)
					.setNeutralButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							RBActivity.this.finish();
						}
					});
			dialog = builder.create();
		}
			break;

		case RB_SOLVE_DIALOG_ID:

			dialog = new Dialog(this);
			dialog.setContentView(R.layout.rb_result_dialog);
			dialog.setTitle("RB Solve Results");
			dialog.setCancelable(false);

			Button okButton = (Button) dialog.findViewById(R.id.okButton);
			okButton.setOnClickListener(new View.OnClickListener() {

				public void onClick(View view) {
					dismissDialog(RB_SOLVE_DIALOG_ID);
					removeDialog(RB_SOLVE_DIALOG_ID);
				}

			});

			Button visButton = (Button) dialog.findViewById(R.id.steadyVisButton);
			visButton.setOnClickListener(new View.OnClickListener() {

				public void onClick(View view) {
					if (rb.mRbSystem.getNumOutputVisualizationFields() > 0) {
						Intent intent = new Intent(RBActivity.this, RBVisualization.class);
						// The bundle was filled in onCreate / SolveThread!
						intent.putExtras(bundle);
						RBActivity.this.startActivity(intent);
					}
				}
			});

			// Create the output string
			String rb_solve_message = "Online N = " + mOnlineNForGui + "\n\u00B5 = [ "
					+ Arrays.toString(s.getParams().getCurrent()) + "]\n\n";

			DecimalFormat twoPlaces = new DecimalFormat("0.###E0");

			// Create a string that shows each output and error bound
			if (s.isReal)
				for (int i = 0; i < s.getNumOutputs(); i++) {

					double output_i = s.RB_outputs[i];
					double output_bound_i = s.RB_output_error_bounds[i];

					rb_solve_message += "Output " + (i + 1) + ":\n" + "Value = " + twoPlaces.format(output_i) + "\n"
							+ "Error bound = " + twoPlaces.format(output_bound_i) + "\n\n";
				}
			else {
				for (int i = 0; i < s.getNumOutputs(); i++) {

					double output_i_r = s.get_RB_output(i, true);
					double output_bound_i_r = s.get_RB_output_error_bound(i, true);
					double output_i_i = s.get_RB_output(i, false);
					double output_bound_i_i = s.get_RB_output_error_bound(i, false);

					rb_solve_message += "Output " + (i + 1) + ":\n" + "Value = " + twoPlaces.format(output_i_r) + " + "
							+ twoPlaces.format(output_i_i) + "i\n" + "Error bound = "
							+ twoPlaces.format(output_bound_i_r) + " + " + twoPlaces.format(output_bound_i_i) + "i\n\n";
				}
			}

			TextView outputView = (TextView) dialog.findViewById(R.id.rb_solve_output);
			outputView.setText(rb_solve_message);

			break;

		
		default:
			dialog = null;
		}
		return dialog;
	}

	protected void delete_downloaded_files() {
		deleteFile(Const.parameters_filename);
		deleteFile(jarFileName);
		deleteFile(dexFileName);
	}

	/**
	 * A Helper function to display the value of the currently selected
	 * parameter in the TextView.
	 * 
	 * @param current_param
	 *            The parameter value to display
	 */
	// private void displayParamValue(int index, double current_param) {
	// String current_param_str;
	// double abs = Math.abs(current_param);
	// if ((abs < 0.1) && (current_param != 0.)) {
	// DecimalFormat decimal_format = new DecimalFormat("0.###E0");
	// current_param_str = decimal_format.format(current_param);
	// } else if ((abs < 1) && (abs >= 0)) {
	// DecimalFormat decimal_format = new DecimalFormat("@@@");
	// current_param_str = decimal_format.format(current_param);
	// } else {
	// DecimalFormat decimal_format = new DecimalFormat("@@@@");
	// current_param_str = decimal_format.format(current_param);
	// }
	//
	// // Make sure we set the parameter to be the same as what the TextView
	// // shows
	// mCurrentParamForGUI[index] = Double.parseDouble(current_param_str);
	//
	// mParamLabels[index].setText(Html.fromHtml(s.getParams()
	// .getLabel(index)));
	// mParamButtons[index].setText(Html.fromHtml(current_param_str));
	//
	// // Set title
	// TextView problemTitleView = (TextView) findViewById(R.id.problemTitle);
	// problemTitleView.setText(rb.problemTitle);
	// }

	/**
	 * Initialize the SeekBar that allows us to select Online N
	 */
	private void initializeOnlineNBar() {
		// Set max/min of online N seekbar
		SeekBar onlineNSeekBar = (SeekBar) findViewById(R.id.onlineNSeekbar);
		onlineNSeekBar.setMax(rb.mRbSystem.getNBF() - 1);

		// Change the progress state so that online N gets initialized
		// to 1
		// If we don't change away from 0, then onProgressChanged
		// doesn't get called
		onlineNSeekBar.setProgress(1);
		onlineNSeekBar.setProgress(0);
	}

	// /**
	// * Initialize the list view depending on the number of parameters in the
	// * system and on the parameter ranges.
	// */
	// private void initializeParamBars() {
	//
	// Parameters p = rb.mRbSystem.getParams();
	// int np = p.getNumParams();
	//
	// // Create String array of parameters to store in the ListView
	// try {
	// TableLayout paramLayout = (TableLayout) findViewById(R.id.paramLayout);
	//
	// // Clear the paramLayout in case we're doing a new problem
	// paramLayout.removeAllViews();
	//
	// mParamLabels = new TextView[np];
	// mParamBars = new SeekBar[np];
	// mParamButtons = new Button[np];
	//
	// for (int i = 0; i < np; i++) {
	// TableRow row = new TableRow(this);
	// row.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
	// LayoutParams.FILL_PARENT));
	//
	// // First add the text label
	// mParamLabels[i] = new TextView(this);
	// mParamLabels[i].setTextSize(15); // Size is in scaled pixels
	// mParamLabels[i].setLayoutParams(new TableRow.LayoutParams(
	// TableRow.LayoutParams.WRAP_CONTENT,
	// TableRow.LayoutParams.WRAP_CONTENT));
	// mParamLabels[i].setPadding(0, 0, 4, 0);
	// row.addView(mParamLabels[i]);
	//
	// // Next add the SeekBar
	// mParamBars[i] = new IndexedSeekBar(this);
	// ((IndexedSeekBar) mParamBars[i]).setIndex(i);
	// mParamBars[i].setLayoutParams(new LayoutParams(
	// LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
	// mParamBars[i].setPadding(10, 10, 10, 0); // Set 10px padding on
	// // left and right
	// row.addView(mParamBars[i]);
	//
	// // Finally add the parameter value text
	// mParamButtons[i] = new IndexedButton(this);
	// ((IndexedButton) mParamButtons[i]).setIndex(i);
	// row.addView(mParamButtons[i]);
	//
	// paramLayout.addView(row, new TableLayout.LayoutParams(
	// LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
	// }
	//
	// // Initialize mCurrentParamForGUI to min_parameter
	// mCurrentParamForGUI = p.getCurrent();
	// for (int i = 0; i < np; i++) {
	// displayParamValue(i, mCurrentParamForGUI[i]);
	//
	// // Also set param bars to match current param
	// int prog = (int) Math.round(100 * mCurrentParamForGUI[i]
	// / (p.getMaxValue(i) - p.getMinValue(i)));
	// mParamBars[i].setProgress(prog);
	// }
	// } catch (Exception e) {
	// Log.e("RBActivity", "Failed init param bars", e);
	// e.printStackTrace();
	// }
	//
	// addParamBarListeners();
	//
	// addParamButtonListeners();
	//
	// }

	// /**
	// * Add a new button to perform a parameter sweep
	// */
	// private void initializeParamSweep() {
	//
	// try {
	//
	// LinearLayout sweepLayout = (LinearLayout)
	// findViewById(R.id.sweepButtonHolder);
	//
	// Button sweepButton = new Button(this);
	// sweepButton.setText("\u00B5 Sweep");
	// sweepButton.setTextSize(22);
	// sweepButton.setOnClickListener(new View.OnClickListener() {
	//
	// public void onClick(View view) {
	//
	// // Create an alert dialog with radio buttons for
	// // selecting the sweep parameter
	// showDialog(SWEEP_DIALOG_ID);
	// }
	// });
	//
	// sweepLayout.addView(sweepButton, new LinearLayout.LayoutParams(
	// LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
	//
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// }

	// // This helper function adds listeners to
	// // the parameter value buttons
	// private void addParamButtonListeners() {
	// for (int i = 0; i < rb.mRbSystem.getParams().getNumParams(); i++) {
	// mParamButtons[i].setOnClickListener(new View.OnClickListener() {
	// public void onClick(View v) {
	// paramButtonIndex = ((IndexedButton) v).getIndex();
	// showDialog(PARAM_DIALOG_ID);
	// }
	// });
	// }
	// }

	// // This helper function adds the listeners
	// // to the newly built parameter SeekBars
	// private void addParamBarListeners() {
	// final Parameters p = rb.mRbSystem.getParams();
	// // Add a listener to each SeekBar
	// for (int i = 0; i < p.getNumParams(); i++) {
	// mParamBars[i]
	// .setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
	//
	// public void onProgressChanged(SeekBar seekBar,
	// int progress, boolean fromUser) {
	//
	// if (fromUser) {
	// IndexedSeekBar isb = (IndexedSeekBar) seekBar;
	// int index = isb.getIndex();
	//
	// if (rb.mRbSystem != null) {
	// double param_range = p.getMaxValue(index)
	// - p.getMinValue(index);
	//
	// double current_param = p.getMinValue(index)
	// + param_range * progress
	// / seekBar.getMax();
	//
	// displayParamValue(index, current_param);
	// }
	//
	// }
	// }
	//
	// public void onStartTrackingTouch(SeekBar seekBar) {
	// }
	//
	// public void onStopTrackingTouch(SeekBar seekBar) {
	// }
	// });
	// }
	//
	// }

	// final Handler sweepHandler = new Handler() {
	// public void handleMessage(Message msg) {
	// mSweepIndex = msg.what;
	// Log.d(DEBUG_TAG, "Sweep index set to " + mSweepIndex);
	// }
	// };

	// Define the Handler that receives messages from the thread and updates the
	// progress
	// handler is a final member object of the RBActivity class
	final Handler downloadHandler = new Handler() {
		public void handleMessage(Message msg) {
			// Now check if there was a problem or not
			boolean downloadSuccessful = msg.getData().getBoolean("loadsuccess");
			Log.d(DEBUG_TAG, "Model loading successful = " + downloadSuccessful + ", model dir: " + mng.getModelDir());

			if (!downloadSuccessful) {
				pd.dismiss();
				showDialog(DOWNLOAD_FAILED_DIALOG_ID);
				delete_downloaded_files();
			} else {

				// Initialize the SeekBar for Online N
				RBActivity.this.initializeOnlineNBar();

				// Initialize the ListView for the parameters
				pb = new ParamBars(RBActivity.this, rb.mRbSystem.getParams());
				pb.createBars((TableLayout) findViewById(R.id.paramLayout));

				// // Set link to problem info page
				// TextView linkView = (TextView) findViewById(R.id.link_view);
				// linkView.setText
				// ("http://augustine.mit.edu/methodology.htm"); // re-write
				// this to be
				// // problem-specific

				if ((rb.getSystemType() == SystemType.LINEAR_STEADY)
						|| (rb.getSystemType() == SystemType.LINEAR_COMPLEX_STEADY)) {
					pb.createSweepButton((LinearLayout) findViewById(R.id.sweepButtonHolder));
				}

				// Set title
				TextView problemTitleView = (TextView) findViewById(R.id.problemTitle);
				problemTitleView.setText(rb.problemTitle);

				pd.dismiss();
			}
		}
	};

	/** Nested class that performs progress calculations (counting) */
	private class ModelLoader extends Thread {

		// The model's directory
		private final AModelManager m = mng;

		// Handler to interact with this thread
		private Handler mHandler;

		// Boolean to indicate if we download from server
		// or load from the assets directory
		// boolean isDownload;

		// The index of the demo we have chosen
		// int mDemoIndex;

		/**
		 * Constructor takes a handler and a String specifying the URL of the
		 * Offline data directory
		 */
		ModelLoader(Handler h) {
			mHandler = h;
		}

		/**
		 * This function gets called when we execute Thread.start()
		 */
		public void run() {

			boolean success = true;

			// Call the main model loading method
			success &= rb.loadModel(m);

			// Clean up if the model fails to load
			if (!success && m instanceof CachingModelManager) {
				((CachingModelManager) m).deleteCachedFiles();
			}

			Message msg = mHandler.obtainMessage();
			Bundle b = new Bundle();
			b.putBoolean("loadsuccess", success);
			msg.setData(b);
			mHandler.sendMessage(msg);
		}
	}

	private class SolveThread extends Thread {
		public void run() {
			RBSystem s = rb.mRbSystem;
			Parameters p = s.getParams();
			
			// Create the bundle and initialize it
		    bundle = new Bundle();
		    
			
		    switch (rb.getSystemType()) {

			case LINEAR_STEADY:
			case LINEAR_COMPLEX_STEADY:

				if (pb.getSweepIndex() == -1) {

					s.solveRB(mOnlineNForGui);
					bundle.putBoolean("isSweep", false);

					handler.sendEmptyMessage(0);
				} else { // We need to perform a sweep

					/**
					 * Perform sweep. Also updates the model geometry!
					 */
					int pts = s.performSweep(pb.getSweepIndex(), mOnlineNForGui);
					bundle.putBoolean("isSweep", true);

					bundle.putInt("sweepIndex", pb.getSweepIndex());
					bundle.putString("title", "Online N = " + mOnlineNForGui);
					bundle.putDouble("dt", s.getSweepIncrement());
					bundle.putDouble("xMin", p.getMinValue(pb.getSweepIndex()));
					bundle.putDouble("xMax", p.getMaxValue(pb.getSweepIndex()));
					bundle.putString("xLabel", Integer.toString(pb.getSweepIndex() + 1));
					bundle.putInt("n_time_steps", pts);
					bundle.putInt("n_outputs", s.getNumOutputs());
					for (int i = 0; i < s.getNumOutputs(); i++) {
						bundle.putDoubleArray("output_data_" + i, s.getSweepOutputs()[i]);
						bundle.putDoubleArray("output_bound_" + i, s.getSweepOutputBounds()[i]);
					}
					// Add this bundle to the intent and plot
					Intent intent = new Intent(RBActivity.this, OutputPlotterActivity.class);
					intent.putExtras(bundle);
					RBActivity.this.startActivity(intent);
				}

				break;
			case LINEAR_UNSTEADY:
			case QN_UNSTEADY:

				// Perform the solve
				s.solveRB(mOnlineNForGui);

				bundle.putBoolean("isReal", s.isReal);
				/**
				 * No sweeps for unsteady systems so far!
				 */
				bundle.putBoolean("isSweep", false);
				bundle.putString("title",
						"Online N = " + mOnlineNForGui + ", parameter = " + Arrays.toString(p.getCurrent()));
				bundle.putDouble("dt", ((TransientRBSystem) s).getdt());
				bundle.putDouble("xMin", 0);
				bundle.putDouble("xMax", ((TransientRBSystem) s).getdt() * s.getTotalTimesteps());
				bundle.putString("xLabel", "time");
				bundle.putInt("n_time_steps", ((TransientRBSystem) s).n_plotting_steps);
				bundle.putInt("n_outputs", s.getNumOutputs());
				
				for (int i = 0; i < s.getNumOutputs(); i++) {
					bundle.putDoubleArray("output_data_" + i, s.RB_outputs_all_k[i]);
					bundle.putDoubleArray("output_bound_" + i, s.RB_output_error_bounds_all_k[i]);
				}
				
				// Add this bundle to the intent and plot
				Intent intent = new Intent(RBActivity.this, OutputPlotterActivity.class);
				intent.putExtras(bundle);
				RBActivity.this.startActivity(intent);
				
				break;
			default:
				throw new RuntimeException("Invalid/unknown RB system type for solve: " + rb.getSystemType());
			}

			// Dismiss progress dialog
			handler.sendEmptyMessage(-1);
		}

		private final Handler handler = new Handler() {
			public void handleMessage(Message msg) {
				pd.dismiss();
				if (msg.what == 0)
					RBActivity.this.showDialog(RB_SOLVE_DIALOG_ID);
			}
		};

	}

}
