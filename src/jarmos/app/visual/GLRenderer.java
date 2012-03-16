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

package jarmos.app.visual;

import jarmos.visual.OpenGLBase;
import jarmos.visual.VisualizationData;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView.Renderer;

/**
 * Changes made by
 * 
 * @author Daniel Wirtz
 * @date Aug 29, 2011
 * 
 */
public class GLRenderer extends OpenGLBase implements Renderer {

	public GLRenderer(VisualizationData vData) {
		super(vData);
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

		if (!is2D()) // enable depth test for 3D rendering
			gl.glEnable(GL10.GL_DEPTH_TEST);

		if ((isFrontFace) || (is2D())) {
			// enable blending (for rendering wireframe)
			if (!is2D())
				gl.glDisable(GL10.GL_CULL_FACE);
			gl.glFrontFace(GL10.GL_CCW);
		} else {
			gl.glEnable(GL10.GL_CULL_FACE);
			gl.glFrontFace(GL10.GL_CW);
		}

		// reset transformation matrix
		gl.glLoadIdentity();

		// setup Light
		if (!is2D()) {
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
			float[] lightPosition = { -getBoxSize(), -getBoxSize(), 0.0f, 0.0f };
			gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, lightPosition, 0);
			// light direction
			float[] lightDirection = { getBoxSize(), getBoxSize(), getBoxSize(), 0.0f };
			gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPOT_DIRECTION, lightDirection, 0);
			// 90 degree FOV
			gl.glLightf(GL10.GL_LIGHT0, GL10.GL_SPOT_CUTOFF, 45.0f);

			// using our normal data
			floatBuf.position(getCurrentNormalsOffset());
			gl.glNormalPointer(GL10.GL_FLOAT, 0, floatBuf);
		}

		// zoom in/out the model
		gl.glScalef(getScalingFactor(), getScalingFactor(), getScalingFactor());

		/*
		 * touchscreen control Rotation, zoom etc
		 */
		if (is2D()) {
			gl.glTranslatef(getXTranslation(), getYTranslation(), 0);
		} else {
			gl.glMultMatrixf(getRotationMatrix(), 0);
		}

		/*
		 * Set pointer to vertex data Always uses currentFrame, which is zero in
		 * case of no animation.
		 */
		floatBuf.position(getCurrentVertexOffset());
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, floatBuf);

		/*
		 * specify the color data for the current frame Four values each: R, G,
		 * B, Alpha
		 */
		floatBuf.position(getCurrentColorOffset());
		gl.glColorPointer(4, GL10.GL_FLOAT, 0, floatBuf);

		/*
		 * Draw the elements using the above declared nodes and color data
		 */

		shortBuf.position(getFaceOffset());
		gl.glDrawElements(GL10.GL_TRIANGLES, getNumFaces() * 3, GL10.GL_UNSIGNED_SHORT, shortBuf);

		// Draw the wireframe for a n field object
		// if ((vData.isConstantFeature(currentColorField)) |
		// (!fGeoData.is2D())) {
		// // Draw the wireframe mesh
		// gl.glColor4f(0.1f, 0.1f, 0.1f, 0.5f);
		// shortBuf.position(_indexwf_off);
		// gl.glDrawElements(GL10.GL_LINES, fGeoData.faces * 6,
		// GL10.GL_UNSIGNED_SHORT, shortBuf);
		// }

		frameRendered();
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

		// define the color we want to be displayed as the "clipping wall"
		gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		gl.glMatrixMode(GL10.GL_PROJECTION);

		// orthographic view
		float[] o = getOrtographicProj();
		gl.glOrthof(o[0], o[1], o[2], o[3], o[4], o[5]);

		gl.glViewport(0, 0, getWidth(), getHeight());
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
		if (!is2D())
			gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
	}

	// @Test
	// public void testGL() {
	// FileModelManager f = new FileModelManager("models");
	// try {
	// f.useModel("demo1");
	// } catch (ModelManagerException e) {
	// e.printStackTrace();
	// fail(e.getMessage());
	// }
	//
	// RBContainer rb = new RBContainer();
	// assertTrue(rb.loadModel(f));
	//
	// // Perform the solve
	// RBSystem s=rb.mRbSystem;
	// double[] par = s.getParams().getRandomParam();
	// // double[] par = new double[]{.5, .5};
	// s.getParams().setCurrent(par);
	// s.solveRB(s.getNBF()/2);
	//
	// SimulationResult res = s.getSimulationResults();
	// GeometryData g = rb.mRbSystem.getGeometry();
	// VisualizationData v = new VisualizationData(g);
	// v.useResult(res);
	//
	// v.computeVisualFeatures(new ColorGenerator());
	//
	// GLRenderer gl = new GLRenderer(v);
	// gl.initRendering();
	// }
}
