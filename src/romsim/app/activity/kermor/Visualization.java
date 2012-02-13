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
import rmcommon.geometry.GeometryData;
import rmcommon.visual.ColorGenerator;
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
public class Visualization extends Activity {

	private GLView glView;

	private SensorManager myManager;
	private List<Sensor> sensors;
	private Sensor accSensor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ReducedModel rm = SimulationActivity.rm;
		GeometryData geoData = rm.geo;
		VisualizationData vData = new VisualizationData(geoData);

		// Bundle extras = getIntent().getExtras();

		float[][] res = rm.getOutput();

		// TODO: Export this mapping as "fieldmapping" class for the 
		// model to allow generic ODE - to - node transfer.
		int len = res[0].length;
		float[] xDispl = new float[len];
		float[] yDispl = new float[len];
		float[] zDispl = new float[len];
		for (int step = 0; step < len; step++) {
			for (int i = 0; i < res.length; i += 6) {
				xDispl[step] = res[i][step];
				yDispl[step] = res[i+1][step];
				zDispl[step] = res[i+2][step];
			}
		}
		geoData.setDisplacementData(xDispl, yDispl, zDispl);

		/*
		 * Add colors to the data!
		 */
		vData.computeColorData(new ColorGenerator());

		// Set Sensor + Manager
		myManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sensors = myManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if (sensors.size() > 0) {
			accSensor = sensors.get(0);
		}

		glView = new GLView(this, vData);
		setContentView(glView);
	}

	private final SensorEventListener mySensorListener = new SensorEventListener() {
		public void onSensorChanged(SensorEvent event) {
			// send data
			glView.setSensorParam(event.values[0], event.values[1],
					event.values[2]);
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
		myManager.registerListener(mySensorListener, accSensor,
				SensorManager.SENSOR_DELAY_GAME);
	}

	@Override
	protected void onStop() {
		myManager.unregisterListener(mySensorListener);
		super.onStop();
	}

}
