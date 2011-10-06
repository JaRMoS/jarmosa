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

package romsim.app.activity.kermor;

import java.util.List;

import kermor.java.ReducedModel;

import rb.java.RBContainer;
import rmcommon.Log;
import rmcommon.geometry.GeometryData;
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
public class Visualization extends Activity {

	private GLView glView;

	private SensorManager myManager;
	private List<Sensor> sensors;
	private Sensor accSensor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//
//		ReducedModel rm = SimulationActivity.rm;
//		GeometryData glData = rm.geo;
//
//		Bundle extras = getIntent().getExtras();
//
//		/*
//		 * Standard case: Normal display.
//		 */
//		if (!extras.getBoolean("isSweep")) {
//			if (!rb.mRbSystem.is_custom_mesh_transform()) {
//				float[][] LT = rb.mRbSystem.get_tranformation_data();
//				float[][][] LTfunc_array = new float[1][LT.length][LT[0].length];
//				LTfunc_array[0] = LT;
//				glData.set_LTfunc(LTfunc_array);
//			} else {
//				double[][] p = new double[1][];
//				p[0] = rb.mRbSystem.getParams().getCurrent().clone();
//				mesh_transform_custom(p, glData);
//			}
//
//			float[][][] truth_sol = rm.getOutput();
//
//			/*
//			 * System has real data, so only [*][0][*] is used
//			 */
//			if (rb.mRbSystem.isReal)
//				/*
//				 * Check which solution field is to display.
//				 */
//				switch (rb.mRbSystem.getNumFields()) {
//				// One field variable
//				case 1:
//					glData.set1FieldData(truth_sol[0][0]);
//					break;
//				
//			
//		} else {
////			/*
////			 * Parameter sweep case
////			 */
////			if (!rb.mRbSystem.is_custom_mesh_transform()) {
////				glData.set_LTfunc(glData.vLTfunc);
////			} else {
////				mesh_transform_custom(RBActivity.mSweepParam, glData);
////			}
////			
////			float[][][] truth_sol = rb.mRbSystem.get_sweep_truth_sol();
////
////			if (rb.mRbSystem.isReal) {
////				switch (rb.mRbSystem.getNumFields()) {
////				case 1:
////					glData.set1FieldData(truth_sol[0][0]);
////					break;
////				case 2:
////					glData.set2FieldData(truth_sol[0][0], truth_sol[1][0]);
////					break;
////				case 3:
////					glData.set3FieldDeformationData(truth_sol[0][0], truth_sol[1][0], truth_sol[2][0]);
////					break;
////				case 4:
////					glData.set4FieldData(truth_sol[0][0], truth_sol[1][0], truth_sol[2][0], truth_sol[3][0]);
////					break;
////				}
////			} else {
////				switch (rb.mRbSystem.getNumFields()) {
////				case 1:
////					glData.set3FieldData(truth_sol[0][0], truth_sol[0][1], truth_sol[0][2]);
////					break;
////				}
////			}
//		}
//		
//		/*
//		 * Add colors to the data!
//		 */
//		glData.computeColorData(RBActivity.cg);
//
//		// Set Sensor + Manager
//		myManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//		sensors = myManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
//		if (sensors.size() > 0) {
//			accSensor = sensors.get(0);
//		}
//
//		glView = new GLView(this, glData);
//		setContentView(glView);
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
