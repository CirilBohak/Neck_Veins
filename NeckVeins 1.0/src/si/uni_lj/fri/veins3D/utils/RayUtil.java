package si.uni_lj.fri.veins3D.utils;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import com.tpxl.GL.Transform;

import si.uni_lj.fri.veins3D.gui.VeinsWindow;
import si.uni_lj.fri.veins3D.gui.XRayProjectionModule;
import si.uni_lj.fri.veins3D.gui.render.Camera;
import si.uni_lj.fri.veins3D.gui.render.VeinsRenderer;
import si.uni_lj.fri.veins3D.math.Vector;

public class RayUtil {

	/**
	 * @param x
	 * @param y
	 * @param renderer
	 * @return
	 */
	public static double[] getRaySphereIntersection(int x, int y, VeinsRenderer renderer, Quaternion orientation, Vector3f position, Vector3f modelPosition, double radius, XRayProjectionModule xRayProjectionModule) {
		// figure out if the click on the screen intersects the circle that
		// surrounds the veins model
		double veinsRadius = radius;
		System.out.println("Rad: " + radius);
		// get the direction of the "ray" cast from the camera location
		double[] d = getRayDirection(x, y, orientation, renderer, xRayProjectionModule);
		System.out.println("d: " + d[0] + " " +d[1] + " " + d[2]);
		d = Vector.normalize(d);
		// a vector representing the camera location
		double[] e = new double[] { position.x , position.y, position.z};

		// the location of the sphere is the zero vector
		double[] c = new double[] { modelPosition.x, modelPosition.y, modelPosition.z};
		// partial calculations
		double[] eSc = Vector.subtraction(e, c);
		System.out.println("eSc: " + eSc[0] + " " + eSc[1] + " " + eSc[2]);
		double dDPeSc = Vector.dotProduct(d, eSc);
		System.out.println("dDPeSc: " + dDPeSc);
		System.out.println("Dot eSc " + Vector.dotProduct(eSc, eSc));
		double discriminant = dDPeSc * dDPeSc - Vector.dotProduct(eSc, eSc) + veinsRadius * veinsRadius;
		System.out.println("Discriminant modded: " + discriminant);
		// in this case the mouse is not pressed near the veins sphere
		if (discriminant < 0) {
			return null;
			// in this case we hold the mouse on the sphere surrounding the
			// veins model in some way
		} else {
			// partial calculation
			double[] Sd = Vector.subtraction(new double[3], d);
			// t1 and t2 are the parameter values for vor "ray" expression e+t*d
			//double t1 = (Vector.dotProduct(Sd, eSc) + Math.sqrt(discriminant)) / Vector.dotProduct(d, d);
			//double t2 = (Vector.dotProduct(Sd, eSc) - Math.sqrt(discriminant)) / Vector.dotProduct(d, d);
			double t1 = -dDPeSc + Math.sqrt(discriminant);
			double t2 = -dDPeSc - Math.sqrt(discriminant);
			
			if (t2 < 0)
			{
				double ret[] = Vector.sum(e, Vector.vScale(d, t1));
				System.out.println("Intersection modded: " + (ret[0]+position.x) + " " + (ret[1]+position.y) + " " + (ret[2]+position.z));
				return ret;
			}
			else
			{
				double ret[] = Vector.sum(e, Vector.vScale(d, t2));
				System.out.println("Intersection modded: " + (ret[0]+position.x) + " " + (ret[1]+position.y) + " " + (ret[2]+position.z));
				return ret;
			}
		}
	}

	/**
	 * @param x
	 * @param y
	 * @param renderer
	 * @return
	 */
	public static double[] getRaySphereIntersection(int x, int y, VeinsRenderer renderer) {
		// figure out if the click on the screen intersects the circle that
		// surrounds the veins model
		Camera cam = renderer.getCamera();
		double veinsRadius = renderer.getVeinsModel().veinsGrabRadius;

		// get the direction of the "ray" cast from the camera location
		double[] d = getRayDirection(x, y, renderer);

		// a vector representing the camera location
		double[] e = new double[] { cam.cameraX, cam.cameraY, cam.cameraZ };

		// the location of the sphere is the zero vector
		double[] c = new double[3];
		// partial calculations
		double[] eSc = Vector.subtraction(e, c);
		double dDPeSc = Vector.dotProduct(d, eSc);
		double discriminant = dDPeSc * dDPeSc - Vector.dotProduct(d, d)
				* (Vector.dotProduct(eSc, eSc) - veinsRadius * veinsRadius);

		// in this case the mouse is not pressed near the veins sphere
		if (discriminant < 0) {
			System.out.println("Discriminant orig < 0");
			return null;
			// in this case we hold the mouse on the sphere surrounding the
			// veins model in some way
		} else {
			// partial calculation
			double[] Sd = Vector.subtraction(new double[3], d);
			// t1 and t2 are the parameter values for vor "ray" expression e+t*d
			double t1 = (Vector.dotProduct(Sd, eSc) + Math.sqrt(discriminant)) / Vector.dotProduct(d, d);
			double t2 = (Vector.dotProduct(Sd, eSc) - Math.sqrt(discriminant)) / Vector.dotProduct(d, d);

			if (t2 < 0)
			{
				double ret[] = Vector.sum(e, Vector.vScale(d, t1));
				System.out.println("Intersection orig: " + ret[0] + " " + ret[1] + " " + ret[2]);
				return ret;
			}
			else
			{
				double ret[] = Vector.sum(e, Vector.vScale(d, t2));
				System.out.println("Intersection orig: " + ret[0] + " " + ret[1] + " " + ret[2]);
				return ret;
			}
		}
	}

	/**
	 * @param x
	 * @param y
	 * @param renderer
	 * @return
	 */
	public static double[] getRayDirection(int x, int y, VeinsRenderer renderer) {
		Camera cam = renderer.getCamera();
		double[] tempUpperLeft = cam.cameraOrientation.rotateVector3d(renderer.screenPlaneInitialUpperLeft);
		double[] tempLowerLeft = cam.cameraOrientation.rotateVector3d(renderer.screenPlaneInitialLowerLeft);
		double[] tempLowerRight = cam.cameraOrientation.rotateVector3d(renderer.screenPlaneInitialLowerRight);

		double[] leftToRight = Vector.subtraction(tempLowerRight, tempLowerLeft);
		leftToRight = Vector.vScale(leftToRight, (0.5d + x) / (double) VeinsWindow.settings.resWidth);
		double[] rayD = Vector.sum(tempLowerLeft, leftToRight);

		double[] downToUp = Vector.subtraction(tempUpperLeft, tempLowerLeft);
		downToUp = Vector.vScale(downToUp, (0.5d + y) / (double) VeinsWindow.settings.resHeight);

		rayD = Vector.sum(rayD, downToUp);
		System.out.println("ray direction original: " + rayD[0] + " " + rayD[1] + " " + rayD[2]);
		
		return rayD;
	}	
	/**
	 * @param x
	 * @param y
	 * @param renderer
	 * @return
	 */
	public static double[] getRayDirection(int x, int y, Quaternion orientation, VeinsRenderer renderer, XRayProjectionModule xRayProjectionModule) {
		
		Matrix4f rot = Transform.quatToMatrix(orientation);

		Matrix4f proj = xRayProjectionModule.activeCamera.getProjectionMatrix();	//Assuming ortho
		Vector4f tul = Matrix4f.transform(rot, new Vector4f(0f, 2/proj.m11, 0f, 0f), null);
		Vector4f tll = Matrix4f.transform(rot, new Vector4f(0f, 0f, 0f, 0f), null);
		Vector4f tlr = Matrix4f.transform(rot, new Vector4f(2/proj.m00, 0f, 0f, 0f), null);
		//Vector4f dir = Matrix4f.transform(proj, new Vector4f(0f, 0f, -1f, 0f), null);
		//if(true)
		//	return new double[]{dir.x, dir.y, dir.z};
		//double nx = ((x-VeinsWindow.settings.resWidth/2)*(2d/proj.m00))/VeinsWindow.settings.resWidth;
		//double ny = ((y-VeinsWindow.settings.resHeight/2)*(2d/proj.m11))/VeinsWindow.settings.resHeight;
		
		//System.out.println("nxy: " + nx + " " + ny + " " + x + " " + y);
		
		double[] tempUpperLeft = new double[]{tul.x, tul.y, tul.z};
		double[] tempLowerLeft = new double[]{tll.x, tll.y, tll.z};
		double[] tempLowerRight = new double[]{tlr.x, tlr.y, tlr.z};

		double[] leftToRight = Vector.subtraction(tempLowerRight, tempLowerLeft);
		leftToRight = Vector.vScale(leftToRight, (0.5d + x) / (double) VeinsWindow.settings.resWidth-0.5d);
		//leftToRight = Vector.vScale(leftToRight, (0.5d + nx));
		//leftToRight = Vector.vScale(leftToRight, 0.5d);
		double[] rayD = Vector.sum(tempLowerLeft, leftToRight);

		double[] downToUp = Vector.subtraction(tempUpperLeft, tempLowerLeft);
		downToUp = Vector.vScale(downToUp, (0.5d + y) / (double) VeinsWindow.settings.resHeight-0.5d);
		//downToUp = Vector.vScale(downToUp, (0.5d + ny));
		//downToUp = Vector.vScale(downToUp, 0.5d);
		rayD = Vector.sum(rayD, downToUp);
		rayD[2] = -100;
		System.out.println("XY/WH: " + ((0.5d + x) / (double) VeinsWindow.settings.resWidth-0.5d) + " " + ((0.5d + y) / (double) VeinsWindow.settings.resHeight-0.5d));
		System.out.println("tultll " + tempUpperLeft[0] + " " + tempUpperLeft[1] + " " + tempUpperLeft[2] + " " + tempLowerLeft[0] + " " + tempLowerLeft[1] + " " + tempLowerLeft[2]);
		System.out.println("ray direction modded: " + rayD[0] + " " + rayD[1] + " " + rayD[2]);
		return rayD;
	}

}
