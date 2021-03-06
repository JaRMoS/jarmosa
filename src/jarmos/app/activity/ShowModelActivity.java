package jarmos.app.activity;

import jarmos.Log;
import jarmos.ModelType;
import jarmos.app.activity.kermor.SimulationActivity;
import jarmos.app.activity.rb.RBActivity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Main model options display activity
 * 
 * This class has been taken from the original @ref rbappmit package and modified to fit into the current JaRMoS
 * framework, this was the former RBGroupActivity class from @ref rbappmit code.
 * 
 * @author Daniel Wirtz
 * @date Aug 23, 2011
 * 
 */
public class ShowModelActivity extends TabActivity {

	static final int ABOUT_DIALOG_ID = 0;
	static final int HELP_DIALOG_ID = 1;

	/**
	 * @see android.app.ActivityGroup#onCreate(android.os.Bundle)
	 */
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final TabHost tabHost = getTabHost();

		tabHost.setup();

		// set up formatting for text labels
		TextView label1 = new TextView(this);
		label1.setGravity(android.view.Gravity.CENTER);
		label1.setTextSize(16);
		label1.setLines(4);
		label1.setTextColor(new ColorStateList(new int[][] { new int[] { android.R.attr.state_selected }, new int[0] },
				new int[] { Color.WHITE, Color.GRAY, }));
		label1.setSelected(true);

		TextView label2 = new TextView(this);
		label2.setGravity(android.view.Gravity.CENTER);
		label2.setTextSize(16);
		label2.setTextColor(new ColorStateList(new int[][] { new int[] { android.R.attr.state_selected }, new int[0] },
				new int[] { Color.WHITE, Color.GRAY, }));
		label2.setSelected(false);

		// add tabs
		TabHost.TabSpec one = tabHost.newTabSpec("tab 1");
		label1.setText("About ");
		one.setIndicator(label1);
		Intent intentOne = new Intent(ShowModelActivity.this, BrowseActivity.class);
		intentOne.putExtras(getIntent().getExtras());
		one.setContent(intentOne);
		tabHost.addTab(one);

		TabHost.TabSpec two = tabHost.newTabSpec("tab 2");
		label2.setText("Solve problem");
		two.setIndicator(label2);
		Intent intentTwo = null;

		// Using the intent field "ModelType" here avoids instantiation of a new AModelManager..
		ModelType mt = (ModelType) getIntent().getSerializableExtra("ModelType");
		if (mt == ModelType.JRB || mt == ModelType.rbappmit) {
			intentTwo = new Intent(ShowModelActivity.this, RBActivity.class);
		} else if (mt == ModelType.JKerMor) {
			intentTwo = new Intent(ShowModelActivity.this, SimulationActivity.class);
		} else {
			Log.e("ShowModelActitity", "Unknown model type: " + mt);
			Toast.makeText(this, "Unknown model type: " + mt, Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		intentTwo.putExtras(getIntent().getExtras());
		two.setContent(intentTwo);
		tabHost.addTab(two);

		// set which tab has initial focus
		tabHost.setCurrentTab(0);
	}

	/**
	 * @see android.app.Activity#onBackPressed()
	 */
	public void onBackPressed() {
		setResult(0);
		getLocalActivityManager().removeAllActivities();
		finish();
	}

	// /**
	// * Re-populates the main menu every time it is brought up depending on
	// * whether help should be displayed
	// */
	// public boolean onPrepareOptionsMenu(Menu menu) {
	//
	// menu.clear();
	// MenuInflater inflater = getMenuInflater();
	// if (getTabHost().getCurrentTab() == 0)
	// inflater.inflate(R.menu.info_main_menu, menu);
	// else
	// inflater.inflate(R.menu.main_menu, menu); // don't want help text
	// // for info screen
	// return true;
	// }

	// /** Handles item selection in the main menu */
	// public boolean onOptionsItemSelected(MenuItem item) {
	// switch (item.getItemId()) {
	// case R.id.about:
	// showDialog(ABOUT_DIALOG_ID);
	// return true;
	// case R.id.help:
	// showDialog(HELP_DIALOG_ID);
	// return true;
	// case R.id.new_problem:
	// setResult(0);
	// getLocalActivityManager().removeAllActivities();
	// finish();
	// return true;
	// case R.id.quit:
	// setResult(-1);
	// getLocalActivityManager().removeAllActivities();
	// finish();
	// return true;
	// }
	// return false;
	// }

	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case ABOUT_DIALOG_ID:
			AlertDialog.Builder about_builder = new AlertDialog.Builder(ShowModelActivity.this);
			about_builder
					.setTitle("rbAPPmit version 0.1")
					.setMessage(
							"rbAPPmit:\nAn Android front-end for the Reduced Basis Method\n\n"
									+ "Copyright (C) 2010\nDavid Knezevic\nPhuong Huynh\n\n" + "Implementation by:\n"
									+ "David Knezevic\n" + "Phuong Huynh\n" + "Mark Wittels\n\n"
									+ "This is free software released under the GPLv3 license")
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dismissDialog(ABOUT_DIALOG_ID);
						}
					});
			dialog = about_builder.create();

			break;
		case HELP_DIALOG_ID:
			final TextView message = new TextView(ShowModelActivity.this);

			final SpannableString s = new SpannableString(
					"Set RB dimension slider to specify the number of basis functions in the approximation.\n\n"
							+ "Set the parameters at which the solve is performed via the parameter sliders or by pressing the adjacent parameter buttons.\n\n"
							+ "Hit solve to perform the reduced basis solve and visualize output(s) and field variable(s).\n\n"
							+ "For more information see http://augustine.mit.edu/methodology.htm");
			Linkify.addLinks(s, Linkify.WEB_URLS);
			message.setText(s);
			message.setTextSize(18); // 18 sp
			message.setPadding(5, 5, 5, 5); // 5 pixels of padding on all sides
			message.setMovementMethod(LinkMovementMethod.getInstance());

			AlertDialog.Builder help_builder = new AlertDialog.Builder(ShowModelActivity.this);
			help_builder.setTitle("rbAPPmit help").setView(message)
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dismissDialog(HELP_DIALOG_ID);
						}
					});

			dialog = help_builder.create();

			break;
		default:
			dialog = null;
		}
		return dialog;
	}

}
