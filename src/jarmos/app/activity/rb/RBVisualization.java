package jarmos.app.activity.rb;

import jarmos.SimulationResult;
import jarmos.app.visual.GLView;
import jarmos.visual.VisualizationData;
import rb.RBContainer;
import rb.RBSystem;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

/**
 * Main RB visualization activity
 * 
 * This class has been taken from the original @ref rbappmit package and modified to fit into the current JaRMoS
 * framework.
 * 
 * @author Daniel Wirtz @date 2013-08-07
 * 
 */
public class RBVisualization extends Activity {

	private GLView glView;

	// private SensorManager myManager;
	// private List<Sensor> sensors;
	// private Sensor accSensor;

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

		// // Set Sensor + Manager
		// myManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		// sensors = myManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		// if (sensors.size() > 0) {
		// accSensor = sensors.get(0);
		// }

		glView = new GLView(this, visData);
		setContentView(glView);
	}

	// private final SensorEventListener mySensorListener = new SensorEventListener() {
	// public void onSensorChanged(SensorEvent event) {
	// // send data
	// glView.setSensorParam(event.values[0], event.values[1], event.values[2]);
	// // update (commented out since not used)
	// /*
	// * oldX = event.values[0]; oldY = event.values[1]; oldZ =
	// * event.values[2];
	// */
	// }
	//
	// public void onAccuracyChanged(Sensor sensor, int accuracy) {
	// }
	// };

	// @Override
	// protected void onResume() {
	// super.onResume();
	// myManager.registerListener(mySensorListener, accSensor, SensorManager.SENSOR_DELAY_GAME);
	// }
	//
	// @Override
	// protected void onStop() {
	// myManager.unregisterListener(mySensorListener);
	// super.onStop();
	// }
}
