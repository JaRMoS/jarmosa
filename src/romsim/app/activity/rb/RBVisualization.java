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

import java.util.List;

import rb.java.RBContainer;
import rmcommon.Log;
import rmcommon.geometry.GeometryData;
import rmcommon.visual.VisualizationData;
import romsim.app.visual.GLView;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

/**
 * @author David J. Knezevic and Phuong Huynh
 * @date 2010
 * 
 */
public class RBVisualization extends Activity {

	private GLView glView;

	private SensorManager myManager;
	private List<Sensor> sensors;
	private Sensor accSensor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		RBContainer rb = RBActivity.rb;
		GeometryData geoData = rb.mRbSystem.getGeometry();
		VisualizationData visData = new VisualizationData(geoData);

		Bundle extras = getIntent().getExtras();

		/*
		 * Standard case: Normal display.
		 */
		if (!extras.getBoolean("isSweep")) {
			if (!rb.mRbSystem.is_custom_mesh_transform()) {
				float[][] LT = rb.mRbSystem.get_tranformation_data();
				float[][][] LTfunc_array = new float[1][LT.length][LT[0].length];
				LTfunc_array[0] = LT;
				geoData.set_LTfunc(LTfunc_array);
			} else {
				double[][] p = new double[1][];
				p[0] = rb.mRbSystem.getParams().getCurrent().clone();
				mesh_transform_custom(p, geoData);
			}

			float[][][] truth_sol = rb.mRbSystem.getFullSolution();

			/*
			 * System has real data, so only [*][0][*] is used
			 */
			if (rb.mRbSystem.isReal)
				/*
				 * Check which solution field is to display.
				 */
				switch (rb.mRbSystem.getNumOutputVisualizationFields()) {
				// One field variable
				case 1:
					visData.set1FieldData(truth_sol[0][0]);
					break;
				// Two field variables
				case 2:
					if (truth_sol[0][0].length == geoData.nodes) {
						// The solution data is node displacement data in x and
						// y directions
						geoData.setXYDeformationData(truth_sol[0][0], truth_sol[1][0]);
						// Set the solution field to one with all zeros (formerly merged into the code for set3FieldData) 
						float[] zeros = new float[truth_sol[0][0].length];
						visData.set1FieldData(zeros);
					} else
						// The solution data are normal field values
						visData.set2FieldData(truth_sol[0][0], truth_sol[1][0]);
					break;
				case 3:
					if (truth_sol[0][0].length == geoData.nodes) {
						geoData.setXYZDeformationData(truth_sol[0][0], truth_sol[1][0], truth_sol[2][0]);
					// 	Set the solution field to one with all zeros (formerly merged into the code for set3FieldData) 
						float[] zeros = new float[truth_sol[0][0].length];
						visData.set1FieldData(zeros);
					} else {
						visData.set3FieldData(truth_sol[0][0], truth_sol[1][0], truth_sol[2][0]);
					}
					break;
				case 4:
					geoData.setXYZDeformationData(truth_sol[0][0], truth_sol[1][0], truth_sol[2][0]);
					visData.set1FieldData(truth_sol[3][0]);
					break;
				}
			/*
			 * System has complex data, so [*][0][*] and [*][1][*] are real and complex parts (?)
			 */
			else
				switch (rb.mRbSystem.getNumOutputVisualizationFields()) {
				/*
				 * Seems to be the only case: one field variable, but complex.
				 * but why three fields?
				 */
				case 1:
					visData.set3FieldData(truth_sol[0][0], truth_sol[0][1], truth_sol[0][2]);
					break;
				}
		} else {
			/*
			 * Parameter sweep case
			 */
			if (!rb.mRbSystem.is_custom_mesh_transform()) {
				geoData.set_LTfunc(geoData.vLTfunc);
			} else {
				mesh_transform_custom(RBActivity.mSweepParam, geoData);
			}
			
			float[][][] truth_sol = rb.mRbSystem.get_sweep_truth_sol();

			if (rb.mRbSystem.isReal) {
				switch (rb.mRbSystem.getNumOutputVisualizationFields()) {
				case 1:
					visData.set1FieldData(truth_sol[0][0]);
					break;
				case 2:
					visData.set2FieldData(truth_sol[0][0], truth_sol[1][0]);
					break;
				case 3:
					geoData.setXYZDeformationData(truth_sol[0][0], truth_sol[1][0], truth_sol[2][0]);
					// Set the solution field to one with all zeros (formerly merged into the code for set3FieldData) 
					float[] zeros = new float[truth_sol[0][0].length];
					visData.set1FieldData(zeros);
					break;
				case 4:
					geoData.setXYZDeformationData(truth_sol[0][0], truth_sol[1][0], truth_sol[2][0]);
					visData.set1FieldData(truth_sol[3][0]);
					break;
				}
			} else {
				switch (rb.mRbSystem.getNumOutputVisualizationFields()) {
				case 1:
					visData.set3FieldData(truth_sol[0][0], truth_sol[0][1], truth_sol[0][2]);
					break;
				}
			}
		}
		
		/*
		 * Add colors to the data!
		 */
		visData.computeColorData(RBActivity.cg);

		// Set Sensor + Manager
		myManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sensors = myManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if (sensors.size() > 0) {
			accSensor = sensors.get(0);
		}

		glView = new GLView(this, visData);
		setContentView(glView);
	}

	/**
	 * @param mu
	 * @param data
	 */
	public void mesh_transform_custom(double[][] mu, GeometryData data) {
		data.vframe_num = mu.length;
		if (data.vframe_num == 1)
			data.isgeoani = false;
		else
			data.isgeoani = true;
		data.vnode = new float[data.vframe_num * data.nodes * 3];
		for (int i = 0; i < data.vframe_num; i++) {
			// get current nodal data
			float[] tmpnode = RBActivity.rb.mRbSystem.mesh_transform(mu[i], data.reference_node.clone());
			Log.d("GLRenderer", mu[i][0] + " " + mu[i][1]);
			Log.d("GLRenderer", tmpnode[4] + " " + data.node[4]);
			data.node = tmpnode.clone();
			// copy current nodal data into animation list
			for (int j = 0; j < data.nodes; j++)
				for (int k = 0; k < 3; k++) {
					data.vnode[i * data.nodes * 3 + j * 3 + k] = tmpnode[j * 3
							+ k];
				}
		}
	}

	private final SensorEventListener mySensorListener = new SensorEventListener(){
		public void onSensorChanged(SensorEvent event) {
			// send data
			glView.setSensorParam(event.values[0], event.values[1], event.values[2]);
			// update (commented out since not used)
			/*
			 * oldX = event.values[0]; oldY = event.values[1]; oldZ =
			 * event.values[2];
			 */
		}

		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
		myManager.registerListener(mySensorListener, accSensor, SensorManager.SENSOR_DELAY_GAME);
	}

	@Override
	protected void onStop() {
		myManager.unregisterListener(mySensorListener);
		super.onStop();
	}

}
