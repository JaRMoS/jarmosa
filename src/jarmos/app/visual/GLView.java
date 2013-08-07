package jarmos.app.visual;

import jarmos.visual.OpenGLBase.Orientation;
import jarmos.visual.VisualizationData;
import android.content.Context;
import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * Open GL view implementation for display of and interaction with reduced model visualizations 
 * 
 * This class has been taken from the original @ref rbappmit package and modified to fit into the current JaRMoS
 * framework
 * 
 * Current settings for interacting with the view:
 * - Volume up/down: Switch visual feature
 * - Tap on graphic: toggle pause (if there is an animation)
 * - Search key: draw wireframe for 3D objects
 * - Press trackball: reset view
 * 
 * @author Daniel Wirtz @date Aug 23, 2011
 * 
 */
public class GLView extends GLSurfaceView {
	
	/**
	 * The minimum distance a mouse movement has to be in order to trigger
	 * position change of the viewed object.
	 */
	private final float MIN_MOVE_DIST = 3f;

	@SuppressWarnings("unused")
	private static final String LOG_TAG = GLView.class.getSimpleName();

	private GLRenderer glRend;

	private float x = 0;
	private float y = 0;

	boolean ismTouch = false, togglePause = false;

	/**
	 * @param context
	 * @param geoData
	 */
	public GLView(Context context, VisualizationData visData) {
		super(context);
		setFocusableInTouchMode(true);
		glRend = new GLRenderer(visData);

		Configuration c = getResources().getConfiguration();
		if (c.orientation == Configuration.ORIENTATION_PORTRAIT) {
			glRend.setOrientation(Orientation.PORTRAIT);
		} else if (c.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			glRend.setOrientation(Orientation.LANDSCAPE);
		}

		setRenderer(glRend);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		glRend.setSize(w,h);
		Log.d("GLView", "On size changed: old w/h "+oldw+"/"+oldh+" new w/h"+w+"/"+h);
	}

	/**
	 * @see android.view.View#onTouchEvent(android.view.MotionEvent)
	 */
	public boolean onTouchEvent(final MotionEvent event) {
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			ismTouch = false;
			togglePause = true;
			x = event.getX();
			y = event.getY();
			break;

		case MotionEvent.ACTION_UP:
		//case MotionEvent.ACTION_POINTER_UP:
			if (togglePause) {
				glRend.togglePause();
				togglePause = false;
			}
			break;
		case MotionEvent.ACTION_MOVE:
			// Normal case: one touch is moving around
			if (!ismTouch) {
				final float xdiff = (x - event.getX());
				final float ydiff = (y - event.getY());
				double diff = Math.sqrt(xdiff*xdiff + ydiff*ydiff);
				if (diff > MIN_MOVE_DIST) {
					togglePause = false;
					queueEvent(new Runnable() {
						public void run() {
							glRend.isContinuousRotation = true;
							glRend.addPos(-xdiff / 20.0f, ydiff / 20.0f);
						}
					});
				}
				x = event.getX();
				y = event.getY();
				
				// Zoom case: two (or more) are down, get y difference between the first two.
			} else {
				final boolean in = y > event.getY(1) - event.getY(0);
				// final float dist = (float) Math.sqrt(_x * _x + _y * _y);
				// if (dist > 10f) {
				queueEvent(new Runnable() {
					public void run() {
						if (in)
							glRend.zoomIn();
						else
							glRend.zoomOut();
						// glRend.zoom(dist / _dist * old_zoom);
					}
				});
				// }
				// _x = event.getX(0) - event.getX(0);
				y = event.getY(1) - event.getY(0);
			}
			break;
			
			// A second pointer has been registered (for zoom)
		case MotionEvent.ACTION_POINTER_DOWN:
			ismTouch = true;
			togglePause = false;
			y = event.getY(1) - event.getY(0);
			// _x = event.getX(0) - event.getX(0);
			// _dist = (float) Math.sqrt(_x * _x + _y * _y);			
			break;
		}
		return true;
	}

	/**
	 * @see android.view.View#onKeyDown(int, android.view.KeyEvent)
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			switch (keyCode) {
			case 24: // KEYCODE_VOLUME_UP
				glRend.nextColorField();
				return true;
			case 25: // KEYCODE_VOLUME_DOWN
				glRend.prevColorField();
				return true;
			case 82: // KEYCODE_MENU
				return true;
			case 83: // KEYCODE_HOME
				return true;
			case 84: // KEYCODE_SEARCH
				glRend.isFrontFace = !glRend.isFrontFace;
				return true;
			default:
				return super.onKeyDown(keyCode, event);
			}
		}
		return true;
	}

	/**
	 * @see android.view.View#onTrackballEvent(android.view.MotionEvent)
	 */
	public boolean onTrackballEvent(MotionEvent event) {
		float TBx = event.getX();
		float TBy = event.getY();
		if (event.getAction() == MotionEvent.ACTION_MOVE) {
			// zoom in if trackball is moving in the 2D "positive" direction
			if ((TBx >= 0) & (TBy <= 0)) 
				glRend.zoomIn();
			else // and zoom out if not
				glRend.zoomOut(); 
		}
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			glRend.resetView();
		}
		return true;
	}

	// /**
	// * @param x
	// * @param y
	// * @param z
	// */
	// public void setSensorParam(float x, float y, float z) {
	// if (isSensorCtrl) {
	// glRend.addPos(false, -x / 1.50f, -y / 1.50f);
	// }
	// }
}