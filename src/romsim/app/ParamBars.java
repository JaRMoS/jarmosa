/**
 * 
 */
package romsim.app;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import rmcommon.Parameters;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
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

/**
 * @author CreaByte
 * 
 */
public class ParamBars {

	private Activity activity;

	private List<Button> buttons;
	private List<SeekBar> bars;
	private List<TextView> labels;

	private Parameters p;

	private int mSweepIndex = -1;

	public ParamBars(Activity activity, Parameters p) {
		this.activity = activity;
		this.p = p;
	}

	public int getSweepIndex() {
		return mSweepIndex;
	}

	public void createBars(TableLayout parent) {
		int np = p.getNumParams();

		// Create String array of parameters to store in the ListView
		try {

			// Clear the paramLayout in case we're doing a new problem
			parent.removeAllViews();

			buttons = new ArrayList<Button>(np);
			bars = new ArrayList<SeekBar>(np);
			labels = new ArrayList<TextView>(np);

			for (int i = 0; i < np; i++) {
				TableRow row = new TableRow(activity);
				row.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
						LayoutParams.FILL_PARENT));

				// First add the text label
				TextView t = new TextView(activity);
				t.setTextSize(15); // Size is in scaled pixels
				t.setLayoutParams(new TableRow.LayoutParams(
						TableRow.LayoutParams.WRAP_CONTENT,
						TableRow.LayoutParams.WRAP_CONTENT));
				t.setPadding(0, 0, 4, 0);
				labels.add(t);
				row.addView(t);

				// Next add the SeekBar
				SeekBar b = new SeekBar(activity);
				b.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
						LayoutParams.WRAP_CONTENT));
				b.setPadding(10, 10, 10, 0); // Set 10px padding on
				// Also set param bars to match current param
				int prog = (int) Math.round(100 * p.getCurrent()[i]
						/ (p.getMaxValue(i) - p.getMinValue(i)));
				b.setProgress(prog);
				// left and right
				bars.add(b);
				row.addView(b);

				// Finally add the parameter value text
				Button btn = new Button(activity);
				btn.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						showParamValueSetDialog(buttons.indexOf(v));
					}
				});
				buttons.add(btn);
				row.addView(btn);
				
				displayParamValue(i, p.getCurrent()[i]);

				parent.addView(row, new TableLayout.LayoutParams(
						LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			}
		} catch (Exception e) {
			Log.e("RBActivity", "Failed init param bars", e);
			e.printStackTrace();
		}

		addParamBarListeners();
	}

	public void createSweepButton(LinearLayout parent) {
		parent.removeAllViews();
		Button sweepButton = new Button(activity);
		sweepButton.setText("\u00B5 Sweep");
		sweepButton.setTextSize(22);
		sweepButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				showParamSweepDialog();
			}
		});
		parent.addView(sweepButton, new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
	}

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

		labels.get(index).setText(Html.fromHtml(p.getLabel(index)));
		buttons.get(index).setText(Html.fromHtml(current_param_str));
	}

	private void addParamBarListeners() {
		// Add a listener to each SeekBar
		for (int i = 0; i < p.getNumParams(); i++) {
			bars.get(i).setOnSeekBarChangeListener(
					new SeekBar.OnSeekBarChangeListener() {

						public void onProgressChanged(SeekBar seekBar,
								int progress, boolean fromUser) {
							if (fromUser) {
								int index = bars.indexOf(seekBar);

								double param_range = p.getMaxValue(index)
										- p.getMinValue(index);

								double current_param = p.getMinValue(index)
										+ param_range * progress
										/ seekBar.getMax();

								p.setCurrent(index, current_param);

								displayParamValue(index, current_param);
							}
						}

						public void onStartTrackingTouch(SeekBar seekBar) {
						}

						public void onStopTrackingTouch(SeekBar seekBar) {
						}
					});
		}

	}

	private void showParamValueSetDialog(final int index) {
		final Dialog dialog = new Dialog(activity);
		dialog.setContentView(R.layout.rb_param_dialog);
		dialog.setTitle("Minimum: " + p.getMinValue(index) + " Maximum: "
				+ p.getMaxValue(index));
		dialog.setCancelable(false);

		final EditText paramInputField = (EditText) dialog
				.findViewById(R.id.param_input_textview);

		// field should accept signed doubles only
		paramInputField.setInputType(InputType.TYPE_CLASS_NUMBER
				| InputType.TYPE_NUMBER_FLAG_DECIMAL
				| InputType.TYPE_NUMBER_FLAG_SIGNED);

		// user-submitted parameter value will be handled when the ok button
		// is pressed
		Button okButton2 = (Button) dialog.findViewById(R.id.param_okButton);
		okButton2.setOnClickListener(new View.OnClickListener() {

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
					userParam = p.getMinValue(index) - 1;
				}

				if (userParam <= p.getMaxValue(index)
						&& userParam >= p.getMinValue(index)) {
					// update parameter bars
					double slopeVal = (100 / (p.getMaxValue(index) - p
							.getMinValue(index)));
					Double progressVal = Double.valueOf((slopeVal * userParam)
							- (p.getMinValue(index) * slopeVal));
					bars.get(index).setProgress(progressVal.intValue());

					// call displayParamValue to update parameter value
					displayParamValue(index, userParam);
				} else {
					Toast.makeText(activity.getApplicationContext(),
							"Invalid Value", Toast.LENGTH_SHORT).show();
				}

				dialog.dismiss();
			}

		});
		dialog.show();
	}

	private void showParamSweepDialog() {
		final int np = p.getNumParams();
		try {
			final String[] paramStrings = new String[np + 1];

			paramStrings[0] = "No Sweep";
			for (int i = 0; i < paramStrings.length; i++) {
				if (i > 0) {
					paramStrings[i] = "Parameter " + i;
				}
			}

			Builder builder = new AlertDialog.Builder(activity);
			builder.setTitle("Pick sweep parameter");
			builder.setItems(paramStrings,
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int item) {
							// Show a Toast for the selected item
							Toast.makeText(activity.getApplicationContext(),
									paramStrings[item], Toast.LENGTH_SHORT)
									.show();
							mSweepIndex = item - 1;

							// disable selected slider, enable all others
							// set disabled slider's progress to 0, all
							// others to old values
							for (int i = 0; i < np; i++) {
								bars.get(i).setEnabled(true);
								double slopeVal = (100 / (p.getMaxValue(i) - p
										.getMinValue(i)));
								Double progressVal = Double
										.valueOf((slopeVal * p.getCurrent()[i])
												- (p.getMinValue(i) * slopeVal));
								bars.get(i).setProgress(progressVal.intValue());
							}
							if (mSweepIndex > -1) {
								bars.get(mSweepIndex).setProgress(0);
								bars.get(mSweepIndex).setEnabled(false);
							}
							for (int i = 0; i < np; i++) {
								displayParamValue(i, p.getCurrent()[i]);
								buttons.get(i).setEnabled(true);
							}
							if (mSweepIndex > -1) {
								buttons.get(mSweepIndex).setText("Sweep");
								buttons.get(mSweepIndex).setEnabled(false);
							}
							dialog.dismiss();
						}
					});

			final Dialog dialog = builder.create();
			dialog.show();
		} catch (Exception e) {
			Log.e("ParamBars",
					"Exception thrown during creation of Sweep dialog");
		}
	}
}
