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

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import rmcommon.geometry.GeometryData;
import android.opengl.GLSurfaceView;
import android.util.Log;

/**
 * Changes made by
 * 
 * @author Daniel Wirtz
 * @date Aug 29, 2011
 * 
 */
public class GLRenderer implements GLSurfaceView.Renderer {

	/*--------------------------------------------------*/
	/* Camera class using quaternion */
	

	private int _anivertex_off = 0;

	/**
	 * Offset for the color data in the float buffer, for each field
	 */
	private int[] _color_off;
	private float _height = 800f;

	/**
	 * Offset for the faces data in short buffer
	 */
	private int _index_off = 0;

	/**
	 * Offset for the wireframe (edges) data in float buffer
	 */
	private int _indexwf_off = 0;

	/**
	 * Offset in the float buffer for the normal data
	 */
	private int _normal_off = 0;

	/**
	 * Offset for the node data in float buffer
	 */
	private int _vertex_off = 0;

	private float _width = 480f;

	private float[] AR = { 1f, 1f }; // aspect ratio

	// Camera control
	private Camera camera;
	private float current_framef = 0f;
	private int currentField = 0;

	private int currentFrame = 0, oldFrame = 0;

	GeometryData fGeoData;

	private FloatBuffer floatBuf;
	boolean isconstant = false;
	boolean isContinuousRotation = true;

	boolean isFrontFace = true;
	boolean ispaused = false;
	private float pos[] = { 0f, 0f, 0f }; // touchscreeen control data
	/**
	 * scaling ratio (for zooming)
	 */
	public float scale_rat = 1.0f;

	private ShortBuffer shortBuf;

	/**
	 * Creates a new OpenGL renderer using the GLObject as geometry/field value
	 * data source
	 * 
	 * @param globj
	 */
	public GLRenderer(GeometryData globj) {
		fGeoData = globj;
	}

	/**
	 * @return The current rendering field
	 */
	public int getcField() {
		return currentField;
	}

	/**
	 * delayed frame increasing, only update animation after 5 frames
	 * 
	 * @param fdelay
	 */
	public void increase_frame(float fdelay) {
		oldFrame = currentFrame;

		current_framef += fdelay * fGeoData.frame_num[currentField];
		currentFrame = Math.round(current_framef);
		if (currentFrame >= fGeoData.frame_num[currentField]) {
			currentFrame = 0;
			current_framef = 0;
		}
		if (currentFrame < 0) {
			currentFrame = fGeoData.frame_num[currentField] - 1;
			current_framef = fGeoData.frame_num[currentField] - 1;
		}
	}

	/**
	 * nondelayed frame increasing
	 * 
	 * @param fdelay
	 */
	public void increase_ndframe(float fdelay) {
		oldFrame = currentFrame;

		current_framef += fdelay;
		currentFrame = Math.round(current_framef);
		if (currentFrame >= fGeoData.frame_num[currentField]) {
			currentFrame = 0;
			current_framef = 0;
		}
		if (currentFrame < 0) {
			currentFrame = fGeoData.frame_num[currentField] - 1;
			current_framef = fGeoData.frame_num[currentField] - 1;
		}
	}

	/**
	 * Initializes the rendering process (Vertex, face, color and normal openGL
	 * buffers)
	 * 
	 * Fills the short/float buffers and records the offsets for certain parts.
	 * 
	 */
	private void initRendering() {

		/*
		 * Clear and init the buffers
		 */
		shortBuf = fGeoData._shortBuffer;
		shortBuf.clear();
		int curShortBufOffset = 0;

		floatBuf = fGeoData._floatBuffer;
		floatBuf.clear();
		int curFloatBufOffset = 0;

		/*
		 * Node float buffer
		 */
		_vertex_off = curFloatBufOffset;
		floatBuf.put(fGeoData.node);
		curFloatBufOffset += fGeoData.node.length;
		Log.d("GLRenderer", "float_count (vertex) = " + curFloatBufOffset + "/"
				+ floatBuf.capacity());

		/*
		 * Faces buffer
		 */
		_index_off = curShortBufOffset;
		shortBuf.put(fGeoData.face);
		curShortBufOffset += fGeoData.face.length;
		Log.d("GLRenderer", "short_count (index) = " + curShortBufOffset + "/"
				+ shortBuf.capacity());

		/*
		 * Edges buffer
		 */
		_indexwf_off = curShortBufOffset;
		shortBuf.put(fGeoData.face_wf);
		curShortBufOffset += fGeoData.face_wf.length;
		Log.d("GLRenderer", "short_count (indexwf) = " + curShortBufOffset
				+ "/" + shortBuf.capacity());

		/*
		 * Colors for each solution field.
		 * 
		 * Animation color buffer contains RBSystem.getVisualNumTimesteps()
		 * times the color data for a single solution.
		 */
		_color_off = new int[fGeoData.fields];
		for (int i = 0; i < fGeoData.fields; i++) {
			_color_off[i] = curFloatBufOffset;
			floatBuf.put(fGeoData.getFieldColors(i));
			curFloatBufOffset += fGeoData.getFieldColors(i).length;
			Log.d("GLRenderer", "float_count (color[" + i + "]) = "
					+ curFloatBufOffset + "/" + floatBuf.capacity());
		}

		// init array for 3D object
		if (!fGeoData.is2D()) {
			_normal_off = curFloatBufOffset;
			floatBuf.put(fGeoData.normal);
			curFloatBufOffset += fGeoData.normal.length;
			Log.d("GLRenderer", "float_count (normal) = " + curFloatBufOffset
					+ "/" + floatBuf.capacity());
		}

		// init vertex animation buffer
		if (fGeoData.isgeoani) {
			_anivertex_off = curFloatBufOffset;
			floatBuf.put(fGeoData.vnode);
			curFloatBufOffset += fGeoData.vnode.length;
			Log.d("GLRenderer", "float_count (anivertex) = "
					+ curFloatBufOffset + "/" + floatBuf.capacity());
			// } else {
			// This piece of assignment is actually never used (same flag
			// isgeoani)
			// _anivertex_off = float_count;
			// _floatBuffer.put(fGeoData.node);
			// float_count += fGeoData.node.length;
			// Log.d("GLRenderer", "float_count (anivertex) = " + float_count
			// + "/" + _floatBuffer.capacity());
		}

	}

	/**
	 * @see android.opengl.GLSurfaceView.Renderer#onDrawFrame(javax.microedition.khronos.opengles.GL10)
	 */
	@Override
	public void onDrawFrame(GL10 gl) {
		// clear the screen to black (0,0,0) color
		// gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		if (!fGeoData.is2D()) // enable depth test for 3D rendering
			gl.glEnable(GL10.GL_DEPTH_TEST);

		// gl.glEnable(GL10.GL_CULL_FACE);
		if ((isFrontFace) || (fGeoData.is2D())) {
			// enable blending (for rendering wireframe)
			if (!fGeoData.is2D())
				gl.glDisable(GL10.GL_CULL_FACE);
			gl.glFrontFace(GL10.GL_CCW);
		} else {
			gl.glEnable(GL10.GL_CULL_FACE);
			gl.glFrontFace(GL10.GL_CW);
		}

		// reset transformation matrix
		gl.glLoadIdentity();

		// setup Light
		if (!fGeoData.is2D()) {
			gl.glEnable(GL10.GL_LIGHTING); // Enable light
			gl.glEnable(GL10.GL_LIGHT0); // turn on the light
			gl.glEnable(GL10.GL_COLOR_MATERIAL); // turn on color lighting

			// material shininess
			gl.glMaterialx(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, 128);
			// ambient light
			float lightAmbient[] = { 0.5f, 0.5f, 0.5f, 1.0f };
			gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, lightAmbient, 0);
			// diffuse light
			float lightDiffuse[] = { 0.8f, 0.8f, 0.8f, 1.0f };
			gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, lightDiffuse, 0);
			// specular light
			float[] lightSpecular = { 0.7f, 0.7f, 0.7f, 1.0f };
			gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPECULAR, lightSpecular, 0);
			// light position
			float[] lightPosition = { -fGeoData.boxsize, -fGeoData.boxsize,
					0.0f, 0.0f };
			gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, lightPosition, 0);
			// light direction
			float[] lightDirection = { fGeoData.boxsize, fGeoData.boxsize,
					fGeoData.boxsize, 0.0f };
			gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPOT_DIRECTION,
					lightDirection, 0);
			// 90 degree FOV
			gl.glLightf(GL10.GL_LIGHT0, GL10.GL_SPOT_CUTOFF, 45.0f);

			// using our normal data
			floatBuf.position(_normal_off);
			gl.glNormalPointer(GL10.GL_FLOAT, 0, floatBuf);
		}

		// zoom in/out the model
		gl.glScalef(scale_rat, scale_rat, scale_rat);

		/*
		 * touchscreen control Rotation, zoom etc
		 */
		if (fGeoData.is2D()) {// we just move the object around in 2D cases
			gl.glTranslatef(pos[0] * fGeoData.boxsize / 20f, pos[1]
					* fGeoData.boxsize / 20f, pos[2] * fGeoData.boxsize / 20f);
		} else { // but we rotate the object in 3D cases
					// set yawing/pitching rotation angles and update camera
			camera.SetRotation(-pos[0] * 8f, -pos[1] * 8f);
			// update rotation matrix
			gl.glMultMatrixf(camera.M, 0);
			// update rotation parameters
			if (isContinuousRotation) {
				float minrot = 0.02f / scale_rat;
				// delay the rotation parameters...
				pos[0] = pos[0] * (1 - (float) Math.exp(-Math.abs(pos[0])));
				pos[1] = pos[1] * (1 - (float) Math.exp(-Math.abs(pos[1])));
				pos[0] = Math.abs(pos[0]) > 3.00f ? Math.signum(pos[0]) * 3.00f
						: pos[0];
				pos[1] = Math.abs(pos[1]) > 3.00f ? Math.signum(pos[1]) * 3.00f
						: pos[1];
				pos[0] = Math.abs(pos[0]) > minrot ? pos[0] : Math
						.signum(pos[0]) * minrot;
				pos[1] = Math.abs(pos[1]) > minrot ? pos[1] : Math
						.signum(pos[1]) * minrot;
			} else {
				// reset the rotation parameters
				pos[0] = 0.0f;
				pos[1] = 0.0f;
			}

			// gl.glTranslatef(-camera.Position[0],-camera.Position[1],-camera.Position[2]);
		}

		// using our vertex data
		if (fGeoData.isgeoani) {
			/*
			 * Animated plot: Use offset for animation vertexes and access the
			 * set for the current frame
			 */
			floatBuf.position(_anivertex_off + currentFrame
					* (fGeoData.nodes * 3));
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, floatBuf);
		} else {
			/*
			 * Normal plot: Use plain position in float buffer for vertex data
			 */
			floatBuf.position(_vertex_off);
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, floatBuf);
		}

		/*
		 * specify the color data for the current frame Four values each: R, G,
		 * B, Alpha
		 */
		floatBuf.position(_color_off[currentField] + currentFrame
				* (fGeoData.nodes * 4));
		if (oldFrame != currentFrame) {
			int oldpos = floatBuf.position();
			Log.d("GLRenderer", "Plotting frame " + currentFrame
					+ " with pointer pos " + oldpos);
			float[] data = new float[100];
			floatBuf.get(data, 0, 100);
			Log.d("GLRenderer", "Floats read for current frame: "+Arrays.toString(data));
			floatBuf.position(oldpos);
		}
		gl.glColorPointer(4, GL10.GL_FLOAT, 0, floatBuf);

		/*
		 * Draw the elements using the above declared nodes and color data
		 */
		shortBuf.position(_index_off);
		gl.glDrawElements(GL10.GL_TRIANGLES, fGeoData.faces * 3,
				GL10.GL_UNSIGNED_SHORT, shortBuf);

		// Draw the wireframe for a n field object
		if ((fGeoData.isConstantField(currentField)) | (!fGeoData.is2D())) {
			// Draw the wireframe mesh
			gl.glColor4f(0.1f, 0.1f, 0.1f, 0.5f);
			shortBuf.position(_indexwf_off);
			gl.glDrawElements(GL10.GL_LINES, fGeoData.faces * 6,
					GL10.GL_UNSIGNED_SHORT, shortBuf);
		}

		// Draw next animation frame
		if (!ispaused)
			increase_frame(0.01f);
	}

	/**
	 * @see android.opengl.GLSurfaceView.Renderer#onSurfaceChanged(javax.microedition.khronos.opengles.GL10,
	 *      int, int)
	 */
	@Override
	public void onSurfaceChanged(GL10 gl, int w, int h) {
		gl.glViewport(0, 0, w, h);
	}

	/**
	 * @see android.opengl.GLSurfaceView.Renderer#onSurfaceCreated(javax.microedition.khronos.opengles.GL10,
	 *      javax.microedition.khronos.egl.EGLConfig)
	 */
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {

		/*
		 * Calls internal rendering preparations involving data from the
		 * GLObject
		 */
		initRendering();

		camera = new Camera();
		// set initial position away from the model in the y-direction
		// looking toward the center of the model (0,0,0) horizontally
		camera.setCamera(0f, -fGeoData.boxsize, 0f, 0f, 1f, 0f, 0f, 0f, 1f);

		// define the color we want to be displayed as the "clipping wall"
		gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		gl.glMatrixMode(GL10.GL_PROJECTION);

		float exrat; // marginal extension ratio
		if (fGeoData.is2D())
			exrat = 0.65f;
		else
			exrat = 0.95f;
		// orthographic view
		gl.glOrthof(-exrat * fGeoData.boxsize / AR[0], exrat * fGeoData.boxsize
				/ AR[0], -exrat * fGeoData.boxsize / AR[1], exrat
				* fGeoData.boxsize / AR[1], -100, 100);

		gl.glViewport(0, 0, (int) _width, (int) _height);
		gl.glMatrixMode(GL10.GL_MODELVIEW);

		// define the color we want to be displayed as the "clipping wall"
		// gl.glClearColor(0f, 0f, 0f, 1.0f);
		gl.glClearColor(1f, 1f, 1f, 1.0f);

		// enable the differentiation of which side may be visible
		gl.glEnable(GL10.GL_CULL_FACE);
		// which is the front? the one which is drawn counter clockwise
		gl.glFrontFace(GL10.GL_CCW);
		// which one should NOT be drawn
		gl.glCullFace(GL10.GL_BACK);

		// Switch on client states in order to make GL10 use the vertex and
		// color data
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

		// Enable normal for 3D object
		if (!fGeoData.is2D())
			gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
	}

	/**
	 * pause the animation if there is any
	 */
	public void pause() {
		ispaused = true;
	}

	/**
	 * reset zoom parameter
	 */
	public void resetZoom() {
		scale_rat = 1.0f;
	}

	/**
	 * Sets the current rendering field
	 * 
	 * @param cField
	 */
	public void setcField(int cField) {
		if (cField > (fGeoData.fields - 1))
			cField = 0;
		currentField = cField;
	}

	/**
	 * Sets the current orientation
	 * 
	 * @param pmode
	 */
	public void setOrientation(boolean pmode) {
		if (pmode) { // portrait mode
			_width = 480f;
			_height = 762f;
			AR[0] = 1.0f;
			AR[1] = _width / _height;
		} else { // landscape mode
			_width = 762f;
			_height = 480f;
			AR[0] = _height / _width;
			AR[1] = 1f;
		}
	}

	// pass touchscreeen control data
	/**
	 * @param iCR
	 * @param posx
	 * @param posy
	 * @param posz
	 */
	public void setPos(boolean iCR, float posx, float posy, float posz) {
		pos[0] += posx;
		pos[1] += posy;
		pos[2] += posz;
		isContinuousRotation = iCR;
	}

	/**
	 * resume animation
	 */
	public void unpause() {
		ispaused = false;
	}

	/**
	 * @param pzoom
	 */
	public void zoom(float pzoom) {
		pzoom = (pzoom < 1) ? 1 : pzoom;
		scale_rat = pzoom;
	}

	/**
	 * zoom in
	 */
	public void zoomin() {
		scale_rat += 0.1f;
	}

	/**
	 * zoom out
	 */
	public void zoomout() {
		scale_rat -= 0.1f;
		if (scale_rat < 0.25f)
			scale_rat = 0.25f;
	}
}
