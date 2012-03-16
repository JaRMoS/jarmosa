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

package romsim.app.visual;

import rmcommon.visual.OpenGLBase.Orientation;
import rmcommon.visual.VisualizationData;
import android.content.Context;
import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * Changes made by:
 * 
 * @author Daniel Wirtz
 * @date Aug 23, 2011
 * 
 */
public class GLView extends GLSurfaceView {

	@SuppressWarnings("unused")
	private static final String LOG_TAG = GLView.class.getSimpleName();

	private GLRenderer glRend;
	private VisualizationData visData;

	private float x = 0;
	private float y = 0;
//	private float _dist = 1.0f;
	// private float old_zoom = 1.0f;

	boolean ismTouch = false;

	// boolean isSensorCtrl = false;
	boolean current_paused = true;

	/**
	 * @param context
	 * @param geoData
	 */
	public GLView(Context context, VisualizationData visData) {
		super(context);
		setFocusableInTouchMode(true);
		this.visData = visData;
		glRend = new GLRenderer(visData);

		Configuration c = getResources().getConfiguration();
		if (c.orientation == Configuration.ORIENTATION_PORTRAIT) {
			glRend.setOrientation(Orientation.PORTRAIT);
		} else if (c.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			glRend.setOrientation(Orientation.LANDSCAPE);
		}

		setRenderer(glRend);
	}

	/**
	 * @see android.view.View#onTouchEvent(android.view.MotionEvent)
	 */
	public boolean onTouchEvent(final MotionEvent event) {
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			ismTouch = false;
			x = event.getX();
			y = event.getY();
			current_paused = glRend.isPaused();
			glRend.pause();
			break;

		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			if (current_paused)
				glRend.pause();
			else
				glRend.unpause();
			break;
		case MotionEvent.ACTION_MOVE:
			// pass touchscreen data to the renderer
			if (!ismTouch) {
				final float xdiff = (x - event.getX());
				final float ydiff = (y - event.getY());
				queueEvent(new Runnable() {
					public void run() {
						glRend.isContinuousRotation = true;
						glRend.addPos(-xdiff / 20.0f, ydiff / 20.0f);
					}
				});
				x = event.getX();
				y = event.getY();
			} else {
				final boolean in = y < event.getY(1) - event.getY(0);
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
		case MotionEvent.ACTION_POINTER_DOWN:
			ismTouch = true;
			// _x = event.getX(0) - event.getX(0);
			y = event.getY(1) - event.getY(0);
			// _dist = (float) Math.sqrt(_x * _x + _y * _y);
			// old_zoom = glRend.scaleFactor;
			current_paused = glRend.isPaused();
			glRend.pause();
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
				glRend.pause();
				glRend.isContinuousRotation = false;
				// glRend.increase_ndframe(-1f);
				return true;
			case 25: // KEYCODE_VOLUME_DOWN
				glRend.pause();
				glRend.isContinuousRotation = false;
				// glRend.increase_ndframe(-1f);
				return true;
			case 82: // KEYCODE_MENU
				if ((glRend.is2D()) || (visData.getNumVisFeatures() > 0))
					glRend.nextColorField();
				else
					// swap face rendering
					glRend.isFrontFace = !glRend.isFrontFace;
				return true;
			case 83: // KEYCODE_HOME
				// do nothing!
				return true;
			case 84: // KEYCODE_SEARCH
				if (glRend.isPaused())
					glRend.unpause();
				else
					glRend.pause();
				if (!glRend.is2D())
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
			if ((TBx >= 0) & (TBy <= 0)) // zoom in if trackball is moving in
											// the 2D "positive" direction
				glRend.zoomIn();
			else
				glRend.zoomOut(); // and zoom out if not
		}
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			glRend.resetZoom(); // reset to original status when users push
								// the "pearl"
			glRend.isContinuousRotation = true;
			glRend.addPos(0.0f, 0.0f);
			glRend.unpause();
			glRend.isContinuousRotation = true;
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