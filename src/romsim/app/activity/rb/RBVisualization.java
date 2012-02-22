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
import rb.java.RBSystem;
import rmcommon.SimulationResult;
import rmcommon.visual.VisualizationData;
import romsim.app.visual.GLView;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

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

		Bundle extras = getIntent().getExtras();
		SimulationResult simRes = null;
		RBSystem s = rb.mRbSystem;
		/*
		 * Standard case: Normal display.
		 */
		if (extras.getBoolean("isSweep")) {
			Log.d("RBVisualization", "Visualizing parameter sweep");
			simRes = s.getSweepSimResults();
		} else {
			Log.d("RBVisualization", "Visualizing normal RB results");
			simRes = s.getSimulationResults();
		}

		VisualizationData visData = new VisualizationData(s.getGeometry(), RBActivity.floatBuf, RBActivity.shortBuf);

		/*
		 * Assign the result to the VisualizationData
		 */
		visData.useResult(simRes);

		/*
		 * Add colors to the data!
		 */
		visData.computeVisualFeatures(RBActivity.cg);

		// Set Sensor + Manager
		myManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sensors = myManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if (sensors.size() > 0) {
			accSensor = sensors.get(0);
		}

		glView = new GLView(this, visData);
		setContentView(glView);
	}

	private final SensorEventListener mySensorListener = new SensorEventListener() {
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
