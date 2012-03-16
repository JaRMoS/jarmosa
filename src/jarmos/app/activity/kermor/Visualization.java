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

package jarmos.app.activity.kermor;

import jarmos.SimulationResult;
import jarmos.app.visual.GLView;
import jarmos.geometry.GeometryData;
import jarmos.visual.ColorGenerator;
import jarmos.visual.VisualizationData;
import kermor.java.ReducedModel;
import android.app.Activity;
import android.os.Bundle;

/**
 * @author David J. Knezevic and Phuong Huynh
 * @date 2010
 * 
 */
public class Visualization extends Activity {

	private GLView glView;

//	private SensorManager myManager;
//	private List<Sensor> sensors;
//	private Sensor accSensor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ReducedModel rm = SimulationActivity.rm;
		GeometryData geoData = rm.getGeometry();
		VisualizationData vData = new VisualizationData(geoData);

		// Bundle extras = getIntent().getExtras();

		SimulationResult res = rm.getSimulationResult();
		vData.useResult(res);

		/*
		 * Add colors to the data!
		 */
		vData.computeVisualFeatures(new ColorGenerator());

//		// Set Sensor + Manager
//		myManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//		sensors = myManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
//		if (sensors.size() > 0) {
//			accSensor = sensors.get(0);
//		}

		glView = new GLView(this, vData);
		setContentView(glView);
	}

//	private final SensorEventListener mySensorListener = new SensorEventListener() {
//		public void onSensorChanged(SensorEvent event) {
//			// send data
//			glView.setSensorParam(event.values[0], event.values[1],
//					event.values[2]);
//			// update (commented out since not used)
//			/*
//			 * oldX = event.values[0]; oldY = event.values[1]; oldZ =
//			 * event.values[2];
//			 */
//		}
//
//		public void onAccuracyChanged(Sensor sensor, int accuracy) {
//		}
//	};
//
//	@Override
//	protected void onResume() {
//		super.onResume();
//		myManager.registerListener(mySensorListener, accSensor,
//				SensorManager.SENSOR_DELAY_GAME);
//	}
//
//	@Override
//	protected void onStop() {
//		myManager.unregisterListener(mySensorListener);
//		super.onStop();
//	}

}
