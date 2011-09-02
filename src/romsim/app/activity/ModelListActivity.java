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

package romsim.app.activity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

import rmcommon.ModelDescriptor;
import rmcommon.io.AModelManager;
import rmcommon.io.AModelManager.ModelManagerException;
import rmcommon.io.FileModelManager;
import romsim.app.Const;
import romsim.app.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This activity generates a simple ListView to allow selection of a model.
 * 
 * Renamed the former CaptionImageAdapter to ModelsGridViewAdapter.
 * 
 * Depending on the selected Source (passed in the Intent-IntExtra "Source") a
 * list (compiled at runtime) from the asset or sd card folder contents is
 * displayed. The Source ID's are defined in the ModelManager class.
 * 
 * In difference to the former version (which worked with "demo+position"
 * folders) the GridView items are now of a small private class containing the
 * model folder name, the title and a Drawable as thumbnail. Those items are
 * used to populate the GridView in the ModelsGridViewAdapter.getView() method.
 * This way, the models can be loaded dynamically at runtime. This allows to add
 * models to the SD-Card (or exchange it) and run them straightaway. Also, the
 * implementation here might be easily extended to simply give the app an online
 * folder of models to choose from. (i'll look into that, hell it's fun!)
 * 
 */
public class ModelListActivity extends Activity {

	/**
	 * Populates the gridview using the Activities "items" List of
	 * ModelDescriptors.
	 */
	private class ModelsGridViewAdapter extends BaseAdapter {

		private Drawable[] imgs;

		ModelsGridViewAdapter() {
			imgs = new BitmapDrawable[items.size()];
			for (int i = 0; i < items.size(); i++) {
				ModelDescriptor md = items.get(i);
				if (md.image != null) {
					imgs[i] = new BitmapDrawable(getResources(), md.image);
					try {
						md.image.close();
					} catch (IOException io) {
						Log.e("ProbSelectionActivity",
								"Failed closing image stream", io);
					}
					// Take red cross if image cannot be read
					if (((BitmapDrawable) imgs[i]).getBitmap() == null) {
						Log.w("ROMSim:ModelListActivity",
								"Could not read image file for model "
										+ md.title + " in folder "
										+ md.modeldir);
						imgs[i] = getResources().getDrawable(
								R.drawable.notfound);
					}
				} else
					imgs[i] = null;
			}
		}

		@Override
		public int getCount() {
			// return getItems().size();
			return items.size();
		}

		@Override
		public Object getItem(int pos) {
			// return getItems().get(pos);
			return items.get(pos);
		}

		@Override
		public long getItemId(int position) {
			return getItem(position).hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v;
			LayoutInflater li = getLayoutInflater();

			v = li.inflate(R.layout.grid_item, null);

			TextView title = (TextView) v.findViewById(R.id.gve_title);
			TextView date = (TextView) v.findViewById(R.id.gve_date);
			TextView mtype = (TextView) v.findViewById(R.id.gve_modeltype);
			TextView descr = (TextView) v.findViewById(R.id.gve_description);
			ImageView iv = (ImageView) v.findViewById(R.id.gve_icon);

			ModelDescriptor i = (ModelDescriptor) getItem(position);
			title.setText(i.title);
			date.setText("Created: "
					+ SimpleDateFormat.getDateInstance().format(i.created));
			mtype.setText("Model type: " + i.type.toString());
			descr.setText(i.shortDescription == null ? "" : i.shortDescription);
			if (imgs[position] != null) {
				iv.setImageDrawable(imgs[position]);
			}
			// Scale images to uniform size (max height/width)
			iv.setLayoutParams(new LinearLayout.LayoutParams(gridView
					.getWidth() / 4, gridView.getHeight() / 5));

			Log.d("adapter", "getview called pos: " + position);
			return v;
		}
	}

	/**
	 * Dialog ID for the dialog that tells the user there are no models for the
	 * selected source.
	 */
	public static final int NO_MODELS_DIALOG_ID = 2;

	/**
	 * Dialog ID for the case of an invalid or nonexistent Source ID
	 */
	public static final int NOSRC_DIALOG_ID = 1;

	private GridView gridView;

	/**
	 * The list items, sources either from assets or sd card.
	 */
	private List<ModelDescriptor> items;
	private ModelsGridViewAdapter mgva = null;

	private AModelManager mng = null;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode < 0)
			finish();
	}

	/**
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.prob_selection);

		// Create model manager instance to use
		try {
			mng = Const.getModelManager(getApplicationContext(), getIntent());
		} catch (ModelManagerException e) {
			Log.e("ModelListActivity", "Creation of ModelManager failed", e);
			finish();
			return;
		}

		final ProgressDialog pd = ProgressDialog.show(this, "",
				"Loading model list", true);

		final Handler h = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				pd.dismiss();

				if (items == null || items.size() == 0) {
					showDialog(NO_MODELS_DIALOG_ID);
				} else {

					gridView = (GridView) findViewById(R.id.gridview);

					gridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
					gridView.setAdapter(mgva);
					// If the current models are served by a
					if (mng instanceof FileModelManager) {
						registerForContextMenu(gridView);
					}

					gridView.setOnItemClickListener(new GridView.OnItemClickListener() {

						public void onItemClick(AdapterView<?> av, View view,
								int position, long id) {
							Intent intent = new Intent(ModelListActivity.this,
									ShowModelActivity.class);
							// Forward extras
							intent.putExtras(getIntent().getExtras());
							ModelDescriptor i = (ModelDescriptor) av
									.getItemAtPosition(position);
							try {
								mng.setModelDir(i.modeldir);
							} catch (AModelManager.ModelManagerException me) {
								Toast.makeText(
										ModelListActivity.this,
										"Error setting the model directory "
												+ i.modeldir + ": "
												+ me.getMessage(),
										Toast.LENGTH_LONG).show();
								Log.e("ProbSelectionActivity",
										"Error setting the model directory "
												+ i.modeldir, me);
								return;
							}
							intent.putExtra("ModelType", i.type);
							intent.putExtra(Const.EXTRA_MODELMANAGER_MODELDIR,
									i.modeldir);
							ModelListActivity.this.startActivityForResult(
									intent, 0);
						}
					});
				}
			}
		};
		(new Thread() {

			@Override
			public void run() {

				try {
					items = mng.getModelDescriptors();
					mgva = new ModelsGridViewAdapter();
				} catch (AModelManager.ModelManagerException ex) {
					Log.e("ModelListActivity",
							"Failed loading model descriptors", ex);
				}

				h.sendEmptyMessage(0);
			}

		}).start();
	}

	/**
	 * Helper method that takes a list of folders and a source which creates the
	 * ModelDescriptors using the ModelManager class.
	 * 
	 * Parses the corresponding "model.xml" file which is (so far) of the
	 * structure
	 * {@literal <rbappmodel title="SomeTitle" image="someimage.png"/>} The
	 * title and image attributes are used for the GridView.
	 * 
	 * @param folders
	 *            The model folder names
	 * @param src
	 *            The Source - See ModelManager SRC_ constants.
	 * @return The ModelDescriptors for each model
	 */

	@Override
	protected Dialog onCreateDialog(int id) {
		// Invalid Source is given. Close this activity.
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		if (id == NOSRC_DIALOG_ID) {
			builder.setMessage("Invalid model source given, parsing failed.")
					.setCancelable(false)
					.setNeutralButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									ModelListActivity.this.finish();
								}
							});
			return builder.create();
		} else if (id == NO_MODELS_DIALOG_ID) {
			builder.setMessage("No models found.")
					.setCancelable(false)
					.setNeutralButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									ModelListActivity.this.finish();
									// dialog.dismiss();
								}
							});
		}
		return builder.create();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu,
	 * android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		getMenuInflater().inflate(R.menu.modellist_contextmenu, menu);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.context_deleteModelItem:
			String dir = items.get(info.position).modeldir;
			try {
				mng.setModelDir(dir);
			} catch (ModelManagerException e) {
				return false;
			}
			if (((FileModelManager) mng).clearCurrentModel()) {
				items.remove(info.position);
				gridView.invalidateViews();
			} else
				Toast.makeText(this, "Deleting model failed.",
						Toast.LENGTH_LONG).show();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

}
