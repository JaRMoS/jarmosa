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

import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.DecimalFormat;

import rb.java.Const;
import rb.java.InconsistentStateException;
import rb.java.Parameter;
import rb.java.RBContainer;
import rb.java.RBEnums;
import rb.java.TransientRBSystem;
import rmcommon.io.AModelManager;
import rmcommon.io.AModelManager.ModelManagerException;
import romsim.app.ModelManagerProgressHandler;
import romsim.app.R;
import romsim.app.misc.rb.IndexedButton;
import romsim.app.misc.rb.IndexedSeekBar;
import romsim.app.visual.GLObject;
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
import android.text.Html;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;
import dalvik.system.DexClassLoader;

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
	private TextView[] mParamLabels;
	private SeekBar[] mParamBars;
	private Button[] mParamButtons;

	/**
	 * Member variable to store index number of currently selected parameter
	 * button
	 */
	private int paramButtonIndex;
	private TextView paramInputField;

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
	public static Parameter mCurrentParamForGUI;

	/**
	 * The base parameter for a parameter sweep in one dimension.
	 */
	public static Parameter[] mSweepParam;

	/**
	 * The index for the parameter sweep, -1 implies no sweep.
	 */
	public static int mSweepIndex;

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
	 * Graphics object - still to be renamed to something better i guess
	 */
	public static GLObject mRbModel;

	/**
	 * The RB Container with all the system and model data (from JRB)
	 */
	public static RBContainer rb;

	private AModelManager mng;

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

		mRbModel = null;

		mRbModel = new GLObject(); // RBActivity.this
		mRbModel.allocateBuffer();

		rb = new RBContainer();

		// initialize sweep index to -1
		mSweepIndex = -1;

		// Add listener to the Solve button
		Button solveButton = (Button) findViewById(R.id.solveButton);
		solveButton.setOnClickListener(new View.OnClickListener(){
			public void onClick(View view) {
				pd = ProgressDialog.show(RBActivity.this, "", "Solving...");
				SolveThread st = new SolveThread();
				st.start();
			}

		});

		// Attach a listener to onlineNSeekBar
		SeekBar onlineNSeekBar = (SeekBar) findViewById(R.id.onlineNSeekbar);
		onlineNSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

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
		m.addMessageHandler(new ModelManagerProgressHandler(){

			/**
			 * @see android.os.Handler#handleMessage(android.os.Message)
			 */
			@Override
			public void handleMessage(Message msg) {
				pd.setMessage(msg.getData().getString("file") + "...");
			}

		});

		Log.d("DEBUG_TAG", "Loading model " + m.getModelDir());
		String title = "Loading " + m.getModelDir() + "...";

		pd = ProgressDialog.show(RBActivity.this, title, "", true, true, new OnCancelListener(){
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

		switch (id) {

		case DOWNLOAD_FAILED_DIALOG_ID: {

			// String title = null;
			// switch (m.getSource()) {
			// case Asset:
			// title = "Error loading model from application assets.";
			// break;
			// case SDCard:
			// title = "Error loading model from SD card.";
			// break;
			// case Web:
			// title = "Download error, try again";
			// }
			String title = "Failed loading the model.";
			Log.d(DEBUG_TAG, "Error loading model, modeldir: "
					+ mng.getModelDir());
			builder = new AlertDialog.Builder(this);
			builder.setMessage(title).setCancelable(false).setNeutralButton("OK", new DialogInterface.OnClickListener(){
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
			okButton.setOnClickListener(new View.OnClickListener(){

				public void onClick(View view) {
					dismissDialog(RB_SOLVE_DIALOG_ID);
					removeDialog(RB_SOLVE_DIALOG_ID);
				}

			});

			Button visButton = (Button) dialog.findViewById(R.id.steadyVisButton);
			visButton.setOnClickListener(new View.OnClickListener(){

				public void onClick(View view) {

					// mRbModel.nodal_transform(rb.mRbSystem.get_tranformation_data());
					if (rb.mRbSystem.get_mfield() > 0) {
						// Next create the bundle and initialize it
						Bundle bundle = new Bundle();
						/*
						 * bundle.putFloatArray("node",
						 * mRbModel.get_node_data());
						 * bundle.putShortArray("face",
						 * mRbModel.get_face_data()); bundle.putInt("nField",
						 * rb.mRbSystem.get_mfield());
						 * bundle.putBoolean("isReal", rb.mRbSystem.isReal);
						 */
						/*
						 * if (rb.mRbSystem.isReal) for (int i = 0;
						 * i<rb.mRbSystem.get_mfield(); i++)
						 * bundle.putFloatArray("field"+String.valueOf(i),
						 * rb.mRbSystem.get_truth_sol(i)); else for (int i = 0;
						 * i<rb.mRbSystem.get_mfield(); i++){ float[][]
						 * truth_sol = rb.mRbSystem.get_complex_truth_sol(i);
						 * bundle.putFloatArray("field"+String.valueOf(i)+"R",
						 * truth_sol[0]);
						 * bundle.putFloatArray("field"+String.valueOf(i)+"I",
						 * truth_sol[1]); }
						 */
						Intent intent = new Intent(RBActivity.this, RBVisualization.class);
						intent.putExtras(bundle);
						RBActivity.this.startActivity(intent);
					}
				}
			});

			// Create the output string
			String rb_solve_message = "Online N = " + mOnlineNForGui
					+ "\n\u00B5 = [ " + rb.mRbSystem.getCurrentParameters()
					+ "]\n\n";

			DecimalFormat twoPlaces = new DecimalFormat("0.###E0");

			// Create a string that shows each output and error bound
			if (rb.mRbSystem.isReal)
				for (int i = 0; i < rb.mRbSystem.get_n_outputs(); i++) {

					double output_i = rb.mRbSystem.RB_outputs[i];
					double output_bound_i = rb.mRbSystem.RB_output_error_bounds[i];

					rb_solve_message += "Output " + (i + 1) + ":\n"
							+ "Value = " + twoPlaces.format(output_i) + "\n"
							+ "Error bound = "
							+ twoPlaces.format(output_bound_i) + "\n\n";
				}
			else
				for (int i = 0; i < rb.mRbSystem.get_n_outputs(); i++) {

					double output_i_r = rb.mRbSystem.get_RB_output(i, true);
					double output_bound_i_r = rb.mRbSystem.get_RB_output_error_bound(i, true);
					double output_i_i = rb.mRbSystem.get_RB_output(i, false);
					double output_bound_i_i = rb.mRbSystem.get_RB_output_error_bound(i, false);

					rb_solve_message += "Output " + (i + 1) + ":\n"
							+ "Value = " + twoPlaces.format(output_i_r) + " + "
							+ twoPlaces.format(output_i_i) + "i\n"
							+ "Error bound = "
							+ twoPlaces.format(output_bound_i_r) + " + "
							+ twoPlaces.format(output_bound_i_i) + "i\n\n";
				}

			TextView outputView = (TextView) dialog.findViewById(R.id.rb_solve_output);
			outputView.setText(rb_solve_message);

			break;

		case SWEEP_DIALOG_ID:

			try {
				final String[] paramStrings = new String[rb.mRbSystem.get_n_params() + 1];

				paramStrings[0] = "No Sweep";
				for (int i = 0; i < paramStrings.length; i++) {
					if (i > 0) {
						paramStrings[i] = "Parameter " + i;
					}
				}

				builder = new AlertDialog.Builder(RBActivity.this);
				builder.setTitle("Pick sweep parameter");
				builder.setItems(paramStrings, new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int item) {
						// Show a Toast for the selected item
						Toast.makeText(getApplicationContext(), paramStrings[item], Toast.LENGTH_SHORT).show();
						// Send a message that indicates which parameter
						// was chosen
						sweepHandler.sendEmptyMessage(item - 1);

						// disable selected slider, enable all others
						// set disabled slider's progress to 0, all
						// others to old values
						try {
							for (int i = 0; i < rb.mRbSystem.get_n_params(); i++) {
								mParamBars[i].setEnabled(true);
								double slopeVal = (100 / (rb.mRbSystem.getParameterMax(i) - rb.mRbSystem.getParameterMin(i)));
								Double progressVal = Double.valueOf((slopeVal * mCurrentParamForGUI.getEntry(i))
										- (rb.mRbSystem.getParameterMin(i) * slopeVal));
								mParamBars[i].setProgress(progressVal.intValue());
							}
						} catch (Exception e) {
						}
						if (item >= 1) {
							mParamBars[item - 1].setProgress(0);
							mParamBars[item - 1].setEnabled(false);
						}

						// disable selected parameter button, enable all
						// others
						// set disabled button to "sweep", all others to
						// old values
						try {
							for (int i = 0; i < rb.mRbSystem.get_n_params(); i++) {
								displayParamValue(i, mCurrentParamForGUI.getEntry(i));
								mParamButtons[i].setEnabled(true);
							}
						} catch (Exception e) {
						}
						if (item >= 1) {
							mParamButtons[item - 1].setText("Sweep");
							mParamButtons[item - 1].setEnabled(false);
						}
					}
				});
				dialog = builder.create();
			} catch (Exception e) {
				Log.e(DEBUG_TAG, "Exception thrown during creation of Sweep dialog");
				dialog = null;
			}

			break;

		case PARAM_DIALOG_ID:
			dialog = new Dialog(this);
			dialog.setContentView(R.layout.rb_param_dialog);
			dialog.setTitle("Minimum: "
					+ rb.mRbSystem.getParameterMin(paramButtonIndex)
					+ " Maximum: "
					+ rb.mRbSystem.getParameterMax(paramButtonIndex));
			dialog.setCancelable(false);

			paramInputField = (EditText) dialog.findViewById(R.id.param_input_textview);

			// field should accept signed doubles only
			paramInputField.setInputType(InputType.TYPE_CLASS_NUMBER
					| InputType.TYPE_NUMBER_FLAG_DECIMAL
					| InputType.TYPE_NUMBER_FLAG_SIGNED);

			// user-submitted parameter value will be handled when the ok button
			// is pressed
			Button okButton2 = (Button) dialog.findViewById(R.id.param_okButton);
			okButton2.setOnClickListener(new View.OnClickListener(){

				public void onClick(View view) {
					// determine if value in input field is within acceptable
					// range
					String userParamString = paramInputField.getText().toString();
					double userParam;
					try {
						userParam = Double.parseDouble(userParamString);
					} catch (NumberFormatException e) {
						// if user submits non-double, default value is out of
						// bounds to trigger toast
						userParam = rb.mRbSystem.getParameterMin(paramButtonIndex) - 1;
					}

					if (userParam <= rb.mRbSystem.getParameterMax(paramButtonIndex)
							&& userParam >= rb.mRbSystem.getParameterMin(paramButtonIndex)) {
						// update parameter bars
						double slopeVal = (100 / (rb.mRbSystem.getParameterMax(paramButtonIndex) - rb.mRbSystem.getParameterMin(paramButtonIndex)));
						Double progressVal = Double.valueOf((slopeVal * userParam)
								- (rb.mRbSystem.getParameterMin(paramButtonIndex) * slopeVal));
						mParamBars[paramButtonIndex].setProgress(progressVal.intValue());

						// call displayParamValue to update parameter value
						displayParamValue(paramButtonIndex, userParam);
					} else {
						Toast.makeText(getApplicationContext(), "Invalid Value", Toast.LENGTH_SHORT).show();
					}

					dismissDialog(PARAM_DIALOG_ID);
					removeDialog(PARAM_DIALOG_ID);
				}

			});

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

	protected void attach_affine_functions(InputStream in) throws Exception {

		// Create a local copy of AffineFunctions.jar
		FileOutputStream f = openFileOutput("AffineFunctions.jar", MODE_WORLD_READABLE);

		byte[] buffer = new byte[1024];
		int len1 = 0;
		while ((len1 = in.read(buffer)) > 0) {
			f.write(buffer, 0, len1);
		}
		in.close();

		Log.d(DEBUG_TAG, "Finished copying jar file");

		DexClassLoader cl = new DexClassLoader("/data/data/romsim.app/files/"
				+ jarFileName, "/data/data/romsim.app/files/", null, ClassLoader.getSystemClassLoader());

		Log.d(DEBUG_TAG, "Created local dex file");

		if (rb.mRbSystem != null) {
			rb.mRbSystem.mAffineFnsClass = cl.loadClass("AffineFunctions");
			rb.mRbSystem.mTheta = rb.mRbSystem.mAffineFnsClass.newInstance();

			// Set Q_a, Q_f and n_outputs from the loaded class
			rb.mRbSystem.read_in_Q_a();
			Log.d(DEBUG_TAG, "Q_a = " + rb.mRbSystem.get_Q_a());

			rb.mRbSystem.read_in_Q_f();
			Log.d(DEBUG_TAG, "Q_f = " + rb.mRbSystem.get_Q_f());

			rb.mRbSystem.read_in_n_outputs();
			Log.d(DEBUG_TAG, "n_outputs = " + rb.mRbSystem.get_n_outputs());

			rb.mRbSystem.read_in_Q_uL();

			if (rb.mSystemType == RBEnums.SystemTypeEnum.LINEAR_UNSTEADY
					|| rb.mSystemType == RBEnums.SystemTypeEnum.QN_UNSTEADY) {
				TransientRBSystem trans_rb = (TransientRBSystem) rb.mRbSystem;
				trans_rb.read_in_Q_m();
				Log.d(DEBUG_TAG, "Q_m = " + trans_rb.get_Q_m());
			}
		}

		if (rb.mRbScmSystem != null) {
			rb.mRbScmSystem.mAffineFnsClass = cl.loadClass("AffineFunctions");
			rb.mRbScmSystem.mTheta = rb.mRbSystem.mAffineFnsClass.newInstance();

			// set Q_a
			rb.mRbScmSystem.read_in_Q_a();
		}
		if (rb.mSecondRbScmSystem != null) {
			rb.mSecondRbScmSystem.mAffineFnsClass = cl.loadClass("AffineFunctions");
			rb.mSecondRbScmSystem.mTheta = rb.mRbSystem.mAffineFnsClass.newInstance();

			// set Q_a
			rb.mSecondRbScmSystem.read_in_Q_a();
		}

	}

	/**
	 * A Helper function to display the value of the currently selected
	 * parameter in the TextView.
	 * 
	 * @param current_param
	 *            The parameter value to display
	 */
	private void displayParamValue(int index, double current_param) {
		String current_param_str;
		double abs = Math.abs(current_param);
		if ((abs < 0.1) && (current_param != 0.)) {
			DecimalFormat decimal_format = new DecimalFormat("0.###E0");
			current_param_str = decimal_format.format(current_param);
		} else if ((abs < 1) && (abs >= 0)) {
			DecimalFormat decimal_format = new DecimalFormat("@@@");
			current_param_str = decimal_format.format(current_param);
		} else {
			DecimalFormat decimal_format = new DecimalFormat("@@@@");
			current_param_str = decimal_format.format(current_param);
		}

		// Make sure we set the parameter to be the same as what the TextView
		// shows
		mCurrentParamForGUI.setEntry(index, Double.parseDouble(current_param_str));

		mParamLabels[index].setText(Html.fromHtml(rb.paramLabels[index]));
		mParamButtons[index].setText(Html.fromHtml(current_param_str));

		// Set title
		TextView problemTitleView = (TextView) findViewById(R.id.problemTitle);
		problemTitleView.setText(rb.problemTitle);
	}

	/**
	 * Initialize the SeekBar that allows us to select Online N
	 */
	private void initializeOnlineNBar() {
		// Set max/min of online N seekbar
		SeekBar onlineNSeekBar = (SeekBar) findViewById(R.id.onlineNSeekbar);
		onlineNSeekBar.setMax(rb.mRbSystem.get_n_basis_functions() - 1);

		// Change the progress state so that online N gets initialized
		// to 1
		// If we don't change away from 0, then onProgressChanged
		// doesn't get called
		onlineNSeekBar.setProgress(1);
		onlineNSeekBar.setProgress(0);
	}

	/**
	 * Initialize the list view depending on the number of parameters in the
	 * system and on the parameter ranges.
	 */
	private void initializeParamBars() {

		// Create String array of parameters to store in the ListView
		try {
			TableLayout paramLayout = (TableLayout) findViewById(R.id.paramLayout);

			// Clear the paramLayout in case we're doing a new problem
			paramLayout.removeAllViews();

			mParamLabels = new TextView[rb.mRbSystem.get_n_params()];
			mParamBars = new SeekBar[rb.mRbSystem.get_n_params()];
			mParamButtons = new Button[rb.mRbSystem.get_n_params()];

			for (int i = 0; i < rb.mRbSystem.get_n_params(); i++) {
				TableRow row = new TableRow(this);
				row.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

				// First add the text label
				mParamLabels[i] = new TextView(this);
				mParamLabels[i].setTextSize(15); // Size is in scaled pixels
				mParamLabels[i].setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
				mParamLabels[i].setPadding(0, 0, 4, 0);
				row.addView(mParamLabels[i]);

				// Next add the SeekBar
				mParamBars[i] = new IndexedSeekBar(this);
				((IndexedSeekBar) mParamBars[i]).setIndex(i);
				mParamBars[i].setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
				mParamBars[i].setPadding(10, 10, 10, 0); // Set 10px padding on
				// left and right
				row.addView(mParamBars[i]);

				// Finally add the parameter value text
				mParamButtons[i] = new IndexedButton(this);
				((IndexedButton) mParamButtons[i]).setIndex(i);
				row.addView(mParamButtons[i]);

				paramLayout.addView(row, new TableLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			}

			// Initialize mCurrentParamForGUI to min_parameter
			mCurrentParamForGUI = new Parameter(rb.mRbSystem.get_n_params());
			for (int i = 0; i < rb.mRbSystem.get_n_params(); i++) {
				double min_param = rb.mRbSystem.getParameterMin(i);

				mCurrentParamForGUI.setEntry(i, min_param);
				displayParamValue(i, min_param);

				// Also set param bars to zero to match min_param
				mParamBars[i].setProgress(0);
			}
		} catch (InconsistentStateException e) {
			Log.e(DEBUG_TAG, e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			addParamBarListeners();
		} catch (InconsistentStateException e) {
			Log.e(DEBUG_TAG, "Exception occurred when adding listeners to the parameter SeekBars");
		}

		try {
			addParamButtonListeners();
		} catch (InconsistentStateException e) {
		}

	}

	/**
	 * Add a new button to perform a parameter sweep
	 */
	private void initializeParamSweep() {

		try {

			LinearLayout sweepLayout = (LinearLayout) findViewById(R.id.sweepButtonHolder);

			Button sweepButton = new Button(this);
			sweepButton.setText("\u00B5 Sweep");
			sweepButton.setTextSize(22);
			sweepButton.setOnClickListener(new View.OnClickListener(){

				public void onClick(View view) {

					// Create an alert dialog with radio buttons for
					// selecting the sweep parameter
					showDialog(SWEEP_DIALOG_ID);
				}
			});

			sweepLayout.addView(sweepButton, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// This helper function adds listeners to
	// the parameter value buttons
	private void addParamButtonListeners() throws InconsistentStateException {

		for (int i = 0; i < rb.mRbSystem.get_n_params(); i++) {
			mParamButtons[i].setOnClickListener(new View.OnClickListener(){
				public void onClick(View v) {
					paramButtonIndex = ((IndexedButton) v).getIndex();
					showDialog(PARAM_DIALOG_ID);
				}
			});
		}
	}

	// This helper function adds the listeners
	// to the newly built parameter SeekBars
	private void addParamBarListeners() throws InconsistentStateException {

		// Add a listener to each SeekBar
		for (int i = 0; i < rb.mRbSystem.get_n_params(); i++) {
			mParamBars[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

					if (fromUser) {
						IndexedSeekBar isb = (IndexedSeekBar) seekBar;
						int index = isb.getIndex();

						if (rb.mRbSystem != null) {
							double param_range = rb.mRbSystem.getParameterMax(index)
									- rb.mRbSystem.getParameterMin(index);

							double current_param = rb.mRbSystem.getParameterMin(index)
									+ param_range * progress / seekBar.getMax();

							displayParamValue(index, current_param);
						}

					}
				}

				public void onStartTrackingTouch(SeekBar seekBar) {
				}

				public void onStopTrackingTouch(SeekBar seekBar) {
				}
			});
		}

	}

	final Handler sweepHandler = new Handler(){
		public void handleMessage(Message msg) {
			mSweepIndex = msg.what;
			Log.d(DEBUG_TAG, "Sweep index set to " + mSweepIndex);
		}
	};

	// Define the Handler that receives messages from the thread and updates the
	// progress
	// handler is a final member object of the RBActivity class
	final Handler downloadHandler = new Handler(){
		public void handleMessage(Message msg) {
			pd.dismiss();

			// Now check if there was a problem or not
			boolean downloadSuccessful = msg.getData().getBoolean("loadsuccess");
			Log.d(DEBUG_TAG, "Model loading successful = " + downloadSuccessful
					+ ", model dir: " + mng.getModelDir());

			if (!downloadSuccessful) {
				showDialog(DOWNLOAD_FAILED_DIALOG_ID);
				delete_downloaded_files();
			} else {

				// Initialize the SeekBar for Online N
				RBActivity.this.initializeOnlineNBar();

				// Initialize the ListView for the parameters
				RBActivity.this.initializeParamBars();

				// // Set link to problem info page
				// TextView linkView = (TextView) findViewById(R.id.link_view);
				// linkView.setText
				// ("http://augustine.mit.edu/methodology.htm"); // re-write
				// this to be
				// // problem-specific

				if ((rb.mSystemType == RBEnums.SystemTypeEnum.LINEAR_STEADY)
						|| (rb.mSystemType == RBEnums.SystemTypeEnum.LINEAR_COMPLEX_STEADY)) {
					RBActivity.this.initializeParamSweep();
				}
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

			// // Now copy AffineFunctions.jar to data/data/files so that we can
			// // unpack it
			// try {
			// InputStream in = m.getInStream(jarFileName);
			// attach_affine_functions(in);
			// } catch (Exception e) {
			// Log.e(DEBUG_TAG,
			// "Exception occurred while loading affine functions",e);
			// success = false;
			// }

			// Call the main model loading method
			success &= rb.loadModel(m);

			// Load GLObject data
			try {
				if (mRbModel != null) {
					mRbModel.read_offline_data(m);
					Log.d(DEBUG_TAG, "Finished reading offline data for RBModel.");
				}
			} catch (Exception e) {
				Log.e(DEBUG_TAG, "Exception occurred while reading offline data: "
						+ e.getMessage(), e);
				success = false;
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
			switch (rb.mSystemType) {

			case LINEAR_STEADY:
			case LINEAR_COMPLEX_STEADY:

				if (mSweepIndex == -1) {

					rb.mRbSystem.setCurrentParameters(mCurrentParamForGUI);
					rb.mRbSystem.RB_solve(mOnlineNForGui);

					handler.sendEmptyMessage(0);
				} else { // We need to perform a sweep
					int numSweepPts = 10;
					numSweepPts = Math.round(100000 / (rb.mRbSystem.get_mfield() * rb.mRbSystem.get_calN()));
					if (!rb.mRbSystem.isReal) numSweepPts /= 3;
					numSweepPts = numSweepPts > 10 ? 10 : numSweepPts;
					// numSweepPts = 50;
					int n_outputs = rb.mRbSystem.get_n_outputs();

					mSweepParam = new Parameter[numSweepPts];

					double[][][] RB_sweep_sol = null;
					if (rb.mRbSystem.isReal) {
						RB_sweep_sol = new double[numSweepPts][1][rb.mRbSystem.get_N()];
					} else {
						RB_sweep_sol = new double[numSweepPts][2][rb.mRbSystem.get_N()];
						n_outputs *= 2;
					}

					double[][] sweepOutputs = new double[n_outputs][numSweepPts];
					double[][] sweepOutputBounds = new double[n_outputs][numSweepPts];

					// Create the bundle and initialize it
					Bundle bundle = new Bundle();

					double sweepParamRange = rb.mRbSystem.getParameterMax(mSweepIndex)
							- rb.mRbSystem.getParameterMin(mSweepIndex);
					double sweepIncrement = sweepParamRange / (numSweepPts - 1);

					float[][][] vLTfunc = new float[numSweepPts][][];

					for (int i = 0; i < numSweepPts; i++) {
						double new_param = rb.mRbSystem.getParameterMin(mSweepIndex)
								+ i * sweepIncrement;
						mCurrentParamForGUI.setEntry(mSweepIndex, new_param);
						rb.mRbSystem.setCurrentParameters(mCurrentParamForGUI);
						mSweepParam[i] = mCurrentParamForGUI.clone();
						Log.d(DEBUG_TAG, "Set new param " + mCurrentParamForGUI);
						rb.mRbSystem.RB_solve(mOnlineNForGui);

						if (rb.mRbSystem.isReal)
							for (int n = 0; n < n_outputs; n++) {
								sweepOutputs[n][i] = rb.mRbSystem.RB_outputs[n];
								sweepOutputBounds[n][i] = rb.mRbSystem.RB_output_error_bounds[n];
							}
						else
							for (int n = 0; n < n_outputs / 2; n++) {
								sweepOutputs[n][i] = rb.mRbSystem.get_RB_output(n, true);
								sweepOutputs[n + n_outputs / 2][i] = rb.mRbSystem.get_RB_output(n, false);
								sweepOutputBounds[n][i] = rb.mRbSystem.get_RB_output_error_bound(n, true);
								sweepOutputBounds[n + n_outputs / 2][i] = rb.mRbSystem.get_RB_output_error_bound(n, false);
							}

						if (rb.mRbSystem.get_mfield() > 0) {
							RB_sweep_sol[i] = rb.mRbSystem.get_RBsolution();
							vLTfunc[i] = rb.mRbSystem.get_tranformation_data();
						}
					}
					mRbModel.vLTfunc = vLTfunc;
					rb.mRbSystem.set_sweep_sol(RB_sweep_sol);

					bundle.putBoolean("isSweep", true);
					bundle.putString("title", "Online N = " + mOnlineNForGui);
					bundle.putDouble("dt", sweepIncrement);
					bundle.putDouble("xMin", rb.mRbSystem.getParameterMin(mSweepIndex));
					bundle.putDouble("xMax", rb.mRbSystem.getParameterMax(mSweepIndex));
					bundle.putString("xLabel", Integer.toString(mSweepIndex + 1));
					bundle.putInt("n_time_steps", numSweepPts);
					bundle.putInt("n_outputs", n_outputs);
					for (int i = 0; i < n_outputs; i++) {
						bundle.putDoubleArray("output_data_" + i, sweepOutputs[i]);
						bundle.putDoubleArray("output_bound_" + i, sweepOutputBounds[i]);
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
				rb.mRbSystem.setCurrentParameters(mCurrentParamForGUI);
				rb.mRbSystem.RB_solve(mOnlineNForGui);

				// Next create the bundle and initialize it
				Bundle bundle = new Bundle();
				bundle.putBoolean("isReal", rb.mRbSystem.isReal);
				bundle.putBoolean("isSweep", false);
				bundle.putString("title", "Online N = " + mOnlineNForGui
						+ ", parameter = " + mCurrentParamForGUI.toString());
				bundle.putDouble("dt", rb.mRbSystem.get_dt());
				bundle.putDouble("xMin", 0);
				bundle.putDouble("xMax", rb.mRbSystem.get_dt()
						* rb.mRbSystem.get_K());
				bundle.putString("xLabel", "time");
				bundle.putInt("n_time_steps", rb.mRbSystem.n_plotting_steps); // rb.mRbSystem.get_K()
																				// +
																				// 1
				bundle.putInt("n_outputs", rb.mRbSystem.get_n_outputs());
				for (int i = 0; i < rb.mRbSystem.get_n_outputs(); i++) {
					bundle.putDoubleArray("output_data_" + i, rb.mRbSystem.RB_outputs_all_k[i]);
					bundle.putDoubleArray("output_bound_" + i, rb.mRbSystem.RB_output_error_bounds_all_k[i]);
				}

				// Add this bundle to the intent
				Intent intent = new Intent(RBActivity.this, OutputPlotterActivity.class);
				intent.putExtras(bundle);
				RBActivity.this.startActivity(intent);

				break;
			default:
				throw new RuntimeException("Invalid RB system type for solve");
			}

			handler.sendEmptyMessage(-1);
		}

		private Handler handler = new Handler(){
			public void handleMessage(Message msg) {
				pd.dismiss();
				if (msg.what == 0) showDialog(RB_SOLVE_DIALOG_ID);
			}
		};

	}

}
