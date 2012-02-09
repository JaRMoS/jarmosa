/**
 * 
 */
package romsim.app.visual;

/**
 * Extracted the Camera class from GLRenderer
 * @author CreaByte
 * 
 */
public class Camera {

	public float[] M = new float[16]; // resulted rotation matrix
	// View position, focus points and view, up and right vectors
	// view vector pointing from view position to the focus point
	// up vector perpendicular to the view vector and current horizontal
	// plane
	// right vector lies on the current horizontal plane
	public float[] Position, Center, View, Up, Right;

	// initialize data
	public Camera() {
		Position = new float[3];
		Center = new float[3];
		View = new float[3];
		Up = new float[3];
		Right = new float[3];
	}

	// calculate the rotation matrix from the current information
	// equivalent to gluLookat
	public void cal_M() {
		float[] s, u, f;
		// Calculate M for gluLookat
		f = NormVec(MinusVec(Center, Position));
		s = CrossVec(f, NormVec(Up));
		u = CrossVec(s, f);
		// Get transformation Matrix
		M[0] = s[0];
		M[4] = s[1];
		M[8] = s[2];
		M[12] = 0.0f;
		M[1] = u[0];
		M[5] = u[1];
		M[9] = u[2];
		M[13] = 0.0f;
		M[2] = -f[0];
		M[6] = -f[1];
		M[10] = -f[2];
		M[14] = 0.0f;
		M[3] = 0.0f;
		M[7] = 0.0f;
		M[11] = 0.0f;
		M[15] = 1.0f;
	}

	// return cross product of two vectors
	public float[] CrossVec(float[] A, float[] B) {
		float[] C = new float[3];
		C[0] = A[1] * B[2] - A[2] * B[1];
		C[1] = A[2] * B[0] - A[0] * B[2];
		C[2] = A[0] * B[1] - A[1] * B[0];
		return C;
	}

	// return the subtraction from two vectors
	public float[] MinusVec(float[] A, float[] B) {
		float[] C = new float[3];
		C[0] = A[0] - B[0];
		C[1] = A[1] - B[1];
		C[2] = A[2] - B[2];
		return C;
	}

	// quaternion multiplication
	public float[] MultQuat(float[] A, float[] B) {
		float[] C = new float[4];
		C[0] = A[3] * B[0] + A[0] * B[3] + A[1] * B[2] - A[2] * B[1];
		C[1] = A[3] * B[1] - A[0] * B[2] + A[1] * B[3] + A[2] * B[0];
		C[2] = A[3] * B[2] + A[0] * B[1] - A[1] * B[0] + A[2] * B[3];
		C[3] = A[3] * B[3] - A[0] * B[0] - A[1] * B[1] - A[2] * B[2];
		return C;
	}

	// normalize a vector
	public float[] NormVec(float[] A) {
		float[] C = new float[3];
		float length = (float) Math.sqrt(A[0] * A[0] + A[1] * A[1] + A[2]
				* A[2]);
		C[0] = A[0] / length;
		C[1] = A[1] / length;
		C[2] = A[2] / length;
		return C;
	}

	// rotate current view vector an "angle" degree about an aribitrary
	// vector [x,y,z]
	public void RotateCamera(float angle, float x, float y, float z) {
		float[] temp = new float[4];
		float[] conjtemp = new float[4];
		float[] quat_view = new float[4];
		float[] result = new float[4];
		// temp is the rotation quaternion
		float deg = (float) ((angle / 180f) * Math.PI);
		float sinhtheta = (float) Math.sin(deg / 2f);
		temp[0] = x * sinhtheta;
		temp[1] = y * sinhtheta;
		temp[2] = z * sinhtheta;
		temp[3] = (float) Math.cos(deg / 2f);
		// the conjugate rotation quaternion
		conjtemp[0] = -temp[0];
		conjtemp[1] = -temp[1];
		conjtemp[2] = -temp[2];
		conjtemp[3] = temp[3];
		// convert view vector into quaternion
		quat_view[0] = View[0];
		quat_view[1] = View[1];
		quat_view[2] = View[2];
		quat_view[3] = 0.0f;
		// rotate by quaternion temp'*quat_view*temp
		result = MultQuat(MultQuat(temp, quat_view), conjtemp);
		// retrieve the new view vector from the resulted quaternion
		View[0] = result[0];
		View[1] = result[1];
		View[2] = result[2];
	}

	// set position, focus points and the up vector
	public void setCamera(float px, float py, float pz, float vx, float vy,
			float vz, float ux, float uy, float uz) {
		Position[0] = px;
		Position[1] = py;
		Position[2] = pz;
		Center[0] = vx;
		Center[1] = vy;
		Center[2] = vz;
		Up[0] = ux;
		Up[1] = uy;
		Up[2] = uz;
		// calculate View and Right vectors
		View = MinusVec(Center, Position); // pointing from the looking
											// point toward the focus point
		Right = NormVec(CrossVec(Up, View)); // right vector perpendicular
												// to the the up and view
												// vectors
	}

	// calculate the new position if we rotate rot1 degree about the current
	// up vectors (yaw)
	// and rot2 degree about the right vector (pitch)
	public void SetRotation(float rot1, float rot2) {
		// rotate rot1 about Up
		RotateCamera(rot1, Up[0], Up[1], Up[2]);
		// recalculae Right
		Right = NormVec(CrossVec(Up, View));
		// rotate rot2 about Right
		RotateCamera(rot2, Right[0], Right[1], Right[2]);
		// recalculate Up
		Up = NormVec(CrossVec(View, Right));
		// recalculate Position
		Position = MinusVec(Center, View);
		// Get transformation matrix
		cal_M();
	}
}
