package main;

import tools.Quaternion;

public class Camera {
	public static final float CAMERA_MOVE_SPEED = 1.667f;
	public static final double CAMERA_ROTATION_SPEED = (float) (72 * Math.PI / 180 / 60);

	public int cameraX;
	public int cameraY;
	public int cameraZ;
	public Quaternion cameraOrientation;

	public Camera() {
		cameraX = 0;
		cameraY = 0;
		cameraZ = 0;
		cameraOrientation = new Quaternion();
	}
}
