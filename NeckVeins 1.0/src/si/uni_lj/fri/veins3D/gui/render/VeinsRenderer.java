package si.uni_lj.fri.veins3D.gui.render;

import static org.lwjgl.opengl.GL11.GL_AMBIENT;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_DIFFUSE;
import static org.lwjgl.opengl.GL11.GL_EQUAL;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_FRONT;
import static org.lwjgl.opengl.GL11.GL_LIGHT0;
import static org.lwjgl.opengl.GL11.GL_LIGHTING;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_POSITION;
import static org.lwjgl.opengl.GL11.GL_SHININESS;
import static org.lwjgl.opengl.GL11.GL_SMOOTH;
import static org.lwjgl.opengl.GL11.GL_SPECULAR;
import static org.lwjgl.opengl.GL11.GL_STENCIL_TEST;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glLight;
import static org.lwjgl.opengl.GL11.glMaterial;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glMultMatrix;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glShadeModel;
import static org.lwjgl.opengl.GL11.glStencilFunc;
import static org.lwjgl.opengl.GL11.glTranslatef;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glDetachShader;
import static org.lwjgl.opengl.GL20.glGetShader;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform4f;
import static org.lwjgl.opengl.GL20.glValidateProgram;
import static si.uni_lj.fri.veins3D.utils.Tools.allocFloats;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector3f;

import com.tpxl.GL.exception.GLFramebufferException;
import com.tpxl.GL.exception.GLProgramLinkException;
import com.tpxl.GL.exception.GLShaderCompileException;

import si.uni_lj.fri.veins3D.exceptions.ShaderLoadException;
import si.uni_lj.fri.veins3D.gui.HUD;
import si.uni_lj.fri.veins3D.gui.VeinsWindow;
import si.uni_lj.fri.veins3D.gui.XRayProjectionModule;
import si.uni_lj.fri.veins3D.gui.render.models.VeinsModel;
import si.uni_lj.fri.veins3D.math.Quaternion;
import si.uni_lj.fri.veins3D.math.Vector;
import si.uni_lj.fri.veins3D.utils.RayUtil;

public class VeinsRenderer extends VeinsRendererInterface{
	public static final int FIXED_PIPELINE = -1;
	public static final int SIMPLE_SHADER = 0;
	public static final int SIMPLE_SHADER_NORM_INTERP = 1;
	public static final int SIMPLE_SHADER_NORM_INTERP_AMBIENT_L = 2;
	public static final int SIMPLE_SHADER_NORM_INTERP_AMBIENT_L_SPEC_BLINN_PHONG = 3;
	public static final int SIMPLE_SHADER_NORM_INTERP_AMBIENT_L_SPEC_PHONG = 4;
	public static final int SHADER_6 = 5;
	public static final int SHADER_7 = 6;
	public static final int SHADER_8 = 7;
	public static final int SHADER_9 = 8;
	public static final int NUMBER_OF_SHADER_PROGRAMS = 8;
	public static final float FOV_Y = 45;
	public static final float Z_NEAR = 10;
	public static final float Z_FAR = 10000;

	private int activeShaderProgram;
	private boolean isWireframeOn;
	private boolean isAAEnabled;

	private Camera cam;

	private VeinsModel veinsModel;

	public XRayProjectionModule xRayProjectionModule;
	
	public double[] screenPlaneInitialUpperLeft = new double[3];
	public double[] screenPlaneInitialUpperRight = new double[3];
	public double[] screenPlaneInitialLowerLeft = new double[3];
	public double[] screenPlaneInitialLowerRight = new double[3];

	private int[] shaderPrograms;
	private int[] vertexShaders;
	private int[] fragmentShaders;

	public VeinsRenderer() throws LWJGLException {
		super();
		cam = new Camera();
		activeShaderProgram = 4;
		isWireframeOn = false;
		isAAEnabled = false;
		try {
			xRayProjectionModule = new XRayProjectionModule();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GLShaderCompileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GLProgramLinkException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GLFramebufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @since 0.1
	 * @version 0.2
	 */
	@Override
	public void setupView() {
		glEnable(GL_CULL_FACE);
		glEnable(GL_DEPTH_TEST);
		glViewport(0, 0, VeinsWindow.settings.resWidth, VeinsWindow.settings.resHeight);
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GLU.gluPerspective(FOV_Y, VeinsWindow.settings.resWidth / (float) VeinsWindow.settings.resHeight, Z_NEAR, Z_FAR);
		glShadeModel(GL_SMOOTH);
		setCameraAndLight(0);
		xRayProjectionModule.setupView();
	}

	/**
	 * @since 0.1
	 * @version 0.1
	 */
	@Override
	public void resetView() {
		glClearColor(0.5f, 0.5f, 0.5f, 0.0f);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
	}

	/**
	 * @since 0.1
	 * @version 0.4
	 */
	@Override
	public void render() {
		if (VeinsWindow.settings.stereoEnabled) {
			float offset = VeinsWindow.settings.stereoValue / 10f;
			StencilMask.initStencil();
			glStencilFunc(GL_EQUAL, 0, 0x01);
			setCameraAndLight(offset);
			renderVeins();
			glStencilFunc(GL_EQUAL, 1, 0x01);
			setCameraAndLight(-offset);
			renderVeins();
			glDisable(GL_STENCIL_TEST);
		} else {
			setCameraAndLight(0);
			renderVeins();
		}
	}

	private void renderVeins() {
		if (veinsModel != null) {
			
			if(activeShaderProgram >= -1 && activeShaderProgram <= 7)
			{
				if (activeShaderProgram == -1) {
					GL20.glUseProgram(0);
				} else {
					GL20.glUseProgram(shaderPrograms[activeShaderProgram]);
					int myUniformLocation = glGetUniformLocation(shaderPrograms[activeShaderProgram], "bloodColor");
					glUniform4f(myUniformLocation, 0.8f, 0.06667f, 0.0f, 1);
				}
	
				glEnable(GL_LIGHTING);
				glColor4f(0.8f, 0.06667f, 0.0f, 1);
				glMaterial(GL_FRONT, GL_AMBIENT, allocFloats(new float[] { 0.8f, 0.06667f, 0.0f, 1 }));
				glMaterial(GL_FRONT, GL_DIFFUSE, allocFloats(new float[] { 0.8f, 0.06667f, 0.0f, 1 }));
				glMaterial(GL_FRONT, GL_SPECULAR, allocFloats(new float[] { 0.66f, 0.66f, 0.66f, 1f }));
				glMaterial(GL_FRONT, GL_SHININESS, allocFloats(new float[] { 100f, 256.0f, 256.0f, 256.0f }));
	
				veinsModel.render();
	
				GL20.glUseProgram(0);
				glPopMatrix();
			}else if(activeShaderProgram == 8)
			{
				xRayProjectionModule.render();
			}
		}
	}

	/**
	 * @since 0.1
	 * @version 0.1
	 */
	private void setCameraAndLight(float offset) {
		double v[] = new double[] { offset, 0, 0 };
		v = cam.cameraOrientation.rotateVector3d(v);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		Quaternion worldOrientation = Quaternion.quaternionReciprocal(cam.cameraOrientation);
		glMultMatrix(worldOrientation.getRotationMatrix(false));
		glTranslatef(-cam.cameraX + (float) v[0], -cam.cameraY + (float) v[1], -cam.cameraZ + (float) v[2]);

		glEnable(GL_LIGHTING);
		glEnable(GL_LIGHT0);
		glLight(GL_LIGHT0, GL_POSITION, allocFloats(new float[] { 0.0f, 1000.0f, 0.0f, 0.0f }));
		glLight(GL_LIGHT0, GL_DIFFUSE, allocFloats(new float[] { 1f, 1f, 1f, 1 }));
		glLight(GL_LIGHT0, GL_AMBIENT, allocFloats(new float[] { 0.3f, 0.3f, 0.3f, 1 }));
		glLight(GL_LIGHT0, GL_SPECULAR, allocFloats(new float[] { 1.0f, 1.0f, 1.0f, 1.0f }));
	}

	/**
	 * 
	 * @throws ShaderLoadException
	 */
	public void loadShaders() throws IOException {
		shaderPrograms = new int[NUMBER_OF_SHADER_PROGRAMS];
		vertexShaders = new int[NUMBER_OF_SHADER_PROGRAMS];
		fragmentShaders = new int[NUMBER_OF_SHADER_PROGRAMS];

		String path = "/shaders/";
		for (int i = 0; i < NUMBER_OF_SHADER_PROGRAMS; i++) {
			shaderPrograms[i] = GL20.glCreateProgram();
			vertexShaders[i] = glCreateShader(GL_VERTEX_SHADER);
			fragmentShaders[i] = glCreateShader(GL_FRAGMENT_SHADER);
			StringBuilder vertexShaderSource = new StringBuilder();
			StringBuilder fragmentShaderSource = new StringBuilder();

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					VeinsWindow.class.getResourceAsStream(path + "shader" + i + ".vert")));
			String line;
			while ((line = reader.readLine()) != null) {
				vertexShaderSource.append(line).append("\n");
			}
			reader.close();
			reader = new BufferedReader(new InputStreamReader(
					VeinsRenderer.class.getResourceAsStream(path + "shader" + i + ".frag")));
			while ((line = reader.readLine()) != null) {
				fragmentShaderSource.append(line).append("\n");
			}
			reader.close();
			

			glShaderSource(vertexShaders[i], vertexShaderSource);
			glCompileShader(vertexShaders[i]);
			if (glGetShader(vertexShaders[i], GL_COMPILE_STATUS) == GL_FALSE) {
				System.err.println("Vertex shader" + i + " not compiled correctly");
			}

			glShaderSource(fragmentShaders[i], fragmentShaderSource);
			glCompileShader(fragmentShaders[i]);
			if (glGetShader(fragmentShaders[i], GL_COMPILE_STATUS) == GL_FALSE) {
				System.err.println("Fragment shader" + i + " not compiled correctly");
			}

			glAttachShader(shaderPrograms[i], vertexShaders[i]);
			glAttachShader(shaderPrograms[i], fragmentShaders[i]);
			glLinkProgram(shaderPrograms[i]);
			glValidateProgram(shaderPrograms[i]);

			System.out.println("Vertex shader" + i + " info: " + glGetShaderInfoLog(vertexShaders[i], 999));
			System.out.println("Fragment shader" + i + " info: " + glGetShaderInfoLog(fragmentShaders[i], 999));
			System.out.println("Shader program" + i + " info: " + glGetShaderInfoLog(shaderPrograms[i], 999));
		}
	}

	public void cleanShaders() {
		for (int i = 0; i < NUMBER_OF_SHADER_PROGRAMS; i++) {
			glDetachShader(shaderPrograms[i], vertexShaders[i]);
			glDeleteShader(vertexShaders[i]);
			glDetachShader(shaderPrograms[i], fragmentShaders[i]);
			glDeleteShader(fragmentShaders[i]);
			glDeleteProgram(shaderPrograms[i]);
		}
	}

	/**
	 * Loads obj file
	 * 
	 * @param fileName
	 * @throws LWJGLException
	 *//*
	public void loadModelObj(String fileName) {
		if (veinsModel != null)
			veinsModel.deleteMeshes();
		veinsModel = new VeinsModel();
		veinsModel.constructVBOFromObjFile(fileName);
		setDefaultViewOptions();
	}*/

	/**
	 * Loads Mhd with raw file using OpenCL
	 * 
	 * @param fileName
	 * @param sigma
	 * @param threshold
	 * @throws LWJGLException
	 */
	public void loadModelRaw(String fileName, double sigma, double threshold) throws LWJGLException {
		if (veinsModel != null)
			veinsModel.deleteMeshes();
		veinsModel = new VeinsModel();
		veinsModel.constructVBOFromRawFile(fileName, sigma, threshold);
		setDefaultViewOptions();
	}

	/**
	 * Loads Mhd with raw file using Java implemenentation
	 * 
	 * @param fileName
	 * @param sigma
	 * @param threshold
	 */
	public void loadModelRawSafeMode(String fileName, double sigma, double threshold) {
		if (veinsModel != null)
			veinsModel.deleteMeshes();
		veinsModel = new VeinsModel();
		veinsModel.constructVBOFromRawFileSafeMode(fileName, sigma, threshold);
		setDefaultViewOptions();
	}

	public void changeModel(double threshold) throws LWJGLException {
		veinsModel.deleteMeshes();
		veinsModel = new VeinsModel(threshold, veinsModel.currentOrientation);
		double d = calculateCameraDistance(veinsModel);
		veinsModel.veinsGrabRadius = d / Math.sqrt(2);
	}

	private void setDefaultViewOptions() {
		double fovMin = (VeinsWindow.settings.resWidth < VeinsWindow.settings.resHeight) ? FOV_Y
				* VeinsWindow.settings.resWidth / (double) VeinsWindow.settings.resHeight : FOV_Y;
		fovMin = Math.toRadians(fovMin); // Math.PI * fovMin / 180
		double d = calculateCameraDistance(veinsModel);
		veinsModel.veinsGrabRadius = d / Math.sqrt(2);
		setCameraPositionAndOrientation(d, fovMin);
		setScreenPlanes(d, fovMin);
	}

	/**
	 * Calculate the appropriate camera distance: The following code takes the
	 * most extreme values on each coordinate of all the specified vertices in
	 * the VeinsModel (from .obj file). It uses the bigger distance (of two)
	 * from the average location on each axis to calculate the radius of a
	 * circle that would surely enclose every vertex, although allowing the
	 * radius to be slightly bigger than necessary.
	 * 
	 * @param veinsModel
	 * @return
	 */
	private double calculateCameraDistance(VeinsModel veinsModel) {
		double d1 = veinsModel.minX - veinsModel.centerx;
		double d2 = veinsModel.maxX - veinsModel.centerx;
		double d3 = veinsModel.minY - veinsModel.centery;
		double d4 = veinsModel.maxY - veinsModel.centery;
		double d5 = veinsModel.minZ - veinsModel.centerz;
		double d6 = veinsModel.maxZ - veinsModel.centerz;
		d1 *= d1;
		d2 *= d2;
		d3 *= d3;
		d4 *= d4;
		d5 *= d5;
		d6 *= d6;
		d1 = Math.max(d1, d2);
		d2 = Math.max(d3, d4);
		d3 = Math.max(d5, d6);
		d1 = Math.sqrt(Math.max(Math.max(d1 + d2, d2 + d3), d1 + d3));

		return d1;
	}

	private void setCameraPositionAndOrientation(double d, double fovMin) {
		cam.cameraZ = (float) (d / Math.tan(fovMin / 2));
		cam.cameraX = 0;
		cam.cameraY = 0;
		cam.cameraOrientation = new Quaternion();
	}

	private void setScreenPlanes(double d1, double fovMin) {
		double yAngle = Math.PI * FOV_Y / 360d;
		double xAngle = yAngle * (double) VeinsWindow.settings.resWidth / (double) VeinsWindow.settings.resHeight;
		double screenPlaneZ = -d1 / Math.tan(fovMin / 2);
		double screenPlaneY = Math.tan(yAngle) * (-screenPlaneZ);
		double screenPlaneX = Math.tan(xAngle) * (-screenPlaneZ);

		screenPlaneInitialUpperLeft = new double[] { -screenPlaneX, screenPlaneY, screenPlaneZ };
		screenPlaneInitialUpperRight = new double[] { screenPlaneX, screenPlaneY, screenPlaneZ };
		screenPlaneInitialLowerLeft = new double[] { -screenPlaneX, -screenPlaneY, screenPlaneZ };
		screenPlaneInitialLowerRight = new double[] { screenPlaneX, -screenPlaneY, screenPlaneZ };

	}

	public void switchWireframe() {
		if (isWireframeOn)
			GL11.glPolygonMode(GL11.GL_FRONT, GL11.GL_FILL);
		else
			GL11.glPolygonMode(GL11.GL_FRONT, GL11.GL_LINE);

		isWireframeOn = !isWireframeOn;
	}

	public void switchAA() {
		isAAEnabled = !isAAEnabled;
	}

	public Camera getCamera() {
		return cam;
	}

	public VeinsModel getVeinsModel() {
		return veinsModel;
	}
	public void resetScene(){
		setDefaultViewOptions();
		veinsModel.resetOrientation();
	}
	public void setActiveShaderProgram(int shaderProgram) {
		activeShaderProgram = shaderProgram;
	}

	@Override
	public void handleKeyboardInputPresses() {
		xRayProjectionModule.handleKeyboardInputPresses();
		if (Keyboard.getEventKeyState()) {
			if (Keyboard.getEventKey() == Keyboard.KEY_1) {
				setActiveShaderProgram(VeinsRenderer.SIMPLE_SHADER);
			} else if (Keyboard.getEventKey() == Keyboard.KEY_2) {
				setActiveShaderProgram(VeinsRenderer.SIMPLE_SHADER_NORM_INTERP);
			} else if (Keyboard.getEventKey() == Keyboard.KEY_3) {
				setActiveShaderProgram(VeinsRenderer.SIMPLE_SHADER_NORM_INTERP_AMBIENT_L);
			} else if (Keyboard.getEventKey() == Keyboard.KEY_4) {
				setActiveShaderProgram(VeinsRenderer.SIMPLE_SHADER_NORM_INTERP_AMBIENT_L_SPEC_BLINN_PHONG);
			} else if (Keyboard.getEventKey() == Keyboard.KEY_5) {
				setActiveShaderProgram(VeinsRenderer.SIMPLE_SHADER_NORM_INTERP_AMBIENT_L_SPEC_PHONG);
			} else if (Keyboard.getEventKey() == Keyboard.KEY_6) {
				setActiveShaderProgram(VeinsRenderer.SHADER_6);
			} else if (Keyboard.getEventKey() == Keyboard.KEY_7) {
				setActiveShaderProgram(VeinsRenderer.SHADER_7);
			} else if (Keyboard.getEventKey() == Keyboard.KEY_8) {
				setActiveShaderProgram(VeinsRenderer.SHADER_8);
			} else if (Keyboard.getEventKey() == Keyboard.KEY_0) {
				setActiveShaderProgram(VeinsRenderer.FIXED_PIPELINE);
			} else if (Keyboard.getEventKey() == Keyboard.KEY_9) {
				switchWireframe();
			} else if (Keyboard.getEventKey() == Keyboard.KEY_ADD && getVeinsModel() != null) {
				getVeinsModel().increaseSubdivisionDepth();
			} else if (Keyboard.getEventKey() == Keyboard.KEY_SUBTRACT && getVeinsModel() != null) {
				getVeinsModel().decreaseSubdivisionDepth();
			} else if (Keyboard.getEventKey() == Keyboard.KEY_9) {
				switchAA();
			} else if (Keyboard.getEventKey() == Keyboard.KEY_P){
				if(activeShaderProgram == VeinsRenderer.SHADER_9)
					xRayProjectionModule.flipCamera();
				else
					setActiveShaderProgram(VeinsRenderer.SHADER_9);
			}
		}
	}

	@Override
	public void handleKeyboardInputContinuous() {
		xRayProjectionModule.handleKeyboardInputContinuous();
		Vector3f offset = new Vector3f();
		Vector3f eulerAngles = new Vector3f();
		if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
			getCamera().lookUp();
			Vector3f.add(eulerAngles, new Vector3f(-0.05f, 0, 0), eulerAngles);
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
			getCamera().lookDown();
			Vector3f.add(eulerAngles, new Vector3f(0.05f, 0, 0), eulerAngles);
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
			getCamera().lookRight();
			Vector3f.add(eulerAngles, new Vector3f(0, -0.05f, 0), eulerAngles);
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
			getCamera().lookLeft();
			Vector3f.add(eulerAngles, new Vector3f(0, 0.05f, 0), eulerAngles);
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_Q)) {
			getCamera().rotateCounterClockwise();
			Vector3f.add(eulerAngles, new Vector3f(0, 0, -0.05f), eulerAngles);
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_E)) {
			getCamera().rotateClockwise();
			Vector3f.add(eulerAngles, new Vector3f(0, 0, 0.05f), eulerAngles);
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
			getCamera().moveForward();
			Vector3f.add(offset, new Vector3f(0, 0, -0.1f), offset);
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
			getCamera().moveBackwards();
			Vector3f.add(offset, new Vector3f(0, 0, 0.1f), offset);
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
			getCamera().moveRight();
			Vector3f.add(offset, new Vector3f(0.1f, 0, 0), offset);
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
			getCamera().moveLeft();
			Vector3f.add(offset, new Vector3f(-0.1f, 0, 0), offset);
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_R)) {
			getCamera().moveUp();
			Vector3f.add(offset, new Vector3f(0, 0.1f, 0), offset);
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_F)) {
			getCamera().moveDown();
			Vector3f.add(offset, new Vector3f(0, -0.1f, 0), offset);
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_BACK)) {
			resetScene();
			xRayProjectionModule.resetScene();
		}

		if(offset.length() > 0.f || eulerAngles.length() > 0.f){
			offset.scale(10);
			eulerAngles.scale((float) (20 * Camera.CAMERA_ROTATION_SPEED));
			if(!xRayProjectionModule.getLockProjection()){
				//modelTransform.translate(offset);
				//modelTransform.rotateLocal(eulerAngles);
				xRayProjectionModule.translateProjectionCamera(offset);
				xRayProjectionModule.rotateProjectionCamera(eulerAngles);
				//translateViewCamera(offset);
				//rotateViewCamera(eulerAngles);
				xRayProjectionModule.screenTransform.rotate(eulerAngles);
			}else{
				xRayProjectionModule.translateViewCamera(offset);
				xRayProjectionModule.rotateViewCamera(eulerAngles);
				
				//modelTransform.translate(offset);
				//modelTransform.rotateLocal(eulerAngles);
				//screenTransform.translate(offset);
				//screenTransform.rotateLocal(eulerAngles);
				//projectionCamera.translate(offset);
				//projectionCamera.rotate(eulerAngles);
			}
		}
	}
	
	double lastHeldAt[];
	
	@Override
	public void handleMouseInput(int dx, int dy, int dz, HUD hud, VeinsWindow veinsWindow) {
		if(getVeinsModel() == null)
			return;
		xRayProjectionModule.handleMouseInput(dx, dy, dz, hud, veinsWindow);
		if (dz > 0) {
			getCamera().zoomIn();
			if(!xRayProjectionModule.getLockProjection())
				xRayProjectionModule.projectionCamera.scale(0.8f);
			xRayProjectionModule.viewCamera.scale(1.25f);
		} else if (dz < 0) {
			getCamera().zoomOut();
			if(!xRayProjectionModule.getLockProjection()){
				xRayProjectionModule.projectionCamera.scale(1.25f);
			}
			xRayProjectionModule.viewCamera.scale(0.8f);
		}

		if (veinsWindow.getClickedOn() == VeinsWindow.CLICKED_ON_VEINS_MODEL) {
			getVeinsModel().changeAddedOrientation(this);
		}
		
		if(Mouse.isButtonDown(0) && 
				veinsWindow.getClickedOn() != VeinsWindow.CLICKED_ON_BUTTONS && 
				veinsWindow.getClickedOn() != VeinsWindow.CLICKED_ON_MOVE_CIRCLE &&
				veinsWindow.getClickedOn() != VeinsWindow.CLICKED_ON_MOVE_ELLIPSE &&
				veinsWindow.getClickedOn() != VeinsWindow.CLICKED_ON_ROTATION_CIRCLE &&
				veinsWindow.getClickedOn() != VeinsWindow.CLICKED_ON_ROTATION_ELLIPSE){
			/*xRayProjectionModule vvvv*/
			//double[] veinsHeldAt = RayUtil.getRaySphereIntersection(Mouse.getX(), Mouse.getY(), this, xRayProjectionModule.activeCamera.getRotation(), xRayProjectionModule.activeCamera.getPosition(), xRayProjectionModule.modelTransform.getPosition());
			double[] veinsHeldAt = RayUtil.getRaySphereIntersection(Mouse.getX(), Mouse.getY(), this, new org.lwjgl.util.vector.Quaternion(), new Vector3f(-xRayProjectionModule.activeCamera.getPosition().x, -xRayProjectionModule.activeCamera.getPosition().y, -xRayProjectionModule.activeCamera.getPosition().z) , new Vector3f(0, 0, 0), getVeinsModel().veinsGrabRadius * xRayProjectionModule.viewCamera.getScale().x, xRayProjectionModule);
			System.out.println("Cam pos: " + xRayProjectionModule.activeCamera.getPosition());
			if (veinsHeldAt != null) {
				double[] rotationAxis;
				double angle = 0;
				if(lastHeldAt != null){
					rotationAxis = Vector.crossProduct(lastHeldAt, veinsHeldAt);
					angle = Math.acos(Vector.dotProduct(lastHeldAt, veinsHeldAt) / (Vector.length(lastHeldAt) * Vector.length(veinsHeldAt)));
					lastHeldAt = veinsHeldAt;
				}else{
					rotationAxis = Vector.crossProduct(veinsHeldAt, veinsHeldAt);
					lastHeldAt = veinsHeldAt;
				}
				if (Vector.length(rotationAxis) > 0) {
					Vector3f rotation = new Vector3f((float)rotationAxis[0], (float)rotationAxis[1], (float)rotationAxis[2]);
					rotation = (Vector3f)rotation.normalise().scale((float) angle);
					if(!xRayProjectionModule.getLockProjection())
					{
						xRayProjectionModule.rotateProjectionCamera(rotation);
						xRayProjectionModule.screenTransform.rotate(rotation);
					}else{
						xRayProjectionModule.rotateViewCamera(rotation);
						
					}
				}
			}
			else{
				lastHeldAt = null;
			}
		}else{
			lastHeldAt = null;
		}

		if (Mouse.isButtonDown(0)) {
			veinsWindow.calculateClickedOn();
			if (veinsWindow.getClickedOn() == VeinsWindow.CLICKED_ON_ROTATION_CIRCLE || veinsWindow.getClickedOn() == VeinsWindow.CLICKED_ON_MOVE_CIRCLE) {
				float x = (veinsWindow.getClickedOn() == VeinsWindow.CLICKED_ON_ROTATION_CIRCLE) ? hud.x1 : hud.x2;
				float y = (veinsWindow.getClickedOn() == VeinsWindow.CLICKED_ON_ROTATION_CIRCLE) ? hud.y1 : hud.y2;

				float clickToCircleDistance = (float) Math.sqrt((x - Mouse.getX()) * (x - Mouse.getX())
						+ (y - Mouse.getY()) * (y - Mouse.getY()));
				float upRotation = (Mouse.getY() - y)
						/ ((clickToCircleDistance > hud.r) ? clickToCircleDistance : hud.r);
				float rightRotation = (Mouse.getX() - x)
						/ ((clickToCircleDistance > hud.r) ? clickToCircleDistance : hud.r);

				hud.clickToCircleDistance = Math.min(clickToCircleDistance, hud.r) / hud.r;
				hud.clickOnCircleAngle = (float) Math.atan2(Mouse.getY() - y, Mouse.getX() - x);

				if (veinsWindow.getClickedOn() == VeinsWindow.CLICKED_ON_ROTATION_CIRCLE) {
					getCamera().rotate(upRotation, rightRotation);
					if(!xRayProjectionModule.getLockProjection())
					{
						xRayProjectionModule.screenTransform.rotate(new Vector3f((float) (-upRotation/180*Math.PI), 0, 0));
						xRayProjectionModule.screenTransform.rotate(new Vector3f(0, (float) (rightRotation/180*Math.PI), 0));
						xRayProjectionModule.rotateProjectionCamera(new Vector3f((float) (-upRotation/180*Math.PI), 0, 0));
						xRayProjectionModule.rotateProjectionCamera(new Vector3f(0, (float) (rightRotation/180*Math.PI), 0));
					}else{
						xRayProjectionModule.rotateViewCamera(new Vector3f((float) (-upRotation/180*Math.PI), 0, 0));
						xRayProjectionModule.rotateViewCamera(new Vector3f(0, (float) (rightRotation/180*Math.PI), 0));
					}
				} else {
					getCamera().move(upRotation, rightRotation);
					if(!xRayProjectionModule.getLockProjection())
					{
						xRayProjectionModule.screenTransform.translate(new Vector3f(rightRotation, 0, upRotation));
						xRayProjectionModule.translateProjectionCamera(new Vector3f(rightRotation, 0, upRotation));
					}else{
						xRayProjectionModule.translateViewCamera(new Vector3f(rightRotation, 0, upRotation));
					}
				}
			}

			if (veinsWindow.getClickedOn() == VeinsWindow.CLICKED_ON_ROTATION_ELLIPSE) {
				if (hud.x1 - Mouse.getX() <= 0) {
					hud.ellipseSide = 0;
					getCamera().rotateClockwise();
					if(!xRayProjectionModule.getLockProjection())
					{
						xRayProjectionModule.screenTransform.rotate(new Vector3f(0, 0, (float) Camera.CAMERA_ROTATION_SPEED));
						xRayProjectionModule.rotateProjectionCamera(new Vector3f(0, 0, (float) Camera.CAMERA_ROTATION_SPEED));
					}else{
						xRayProjectionModule.rotateViewCamera(new Vector3f(0, 0, (float) Camera.CAMERA_ROTATION_SPEED));
					}
				} else {
					hud.ellipseSide = 1;
					getCamera().rotateCounterClockwise();
					if(!xRayProjectionModule.getLockProjection())
					{
						xRayProjectionModule.screenTransform.rotate(new Vector3f(0, 0, (float) -Camera.CAMERA_ROTATION_SPEED));
						xRayProjectionModule.rotateProjectionCamera(new Vector3f(0, 0, (float) -Camera.CAMERA_ROTATION_SPEED));
					}else{
						xRayProjectionModule.rotateViewCamera(new Vector3f(0, 0, (float) -Camera.CAMERA_ROTATION_SPEED));
					}
				}
			}

			if (veinsWindow.getClickedOn() == VeinsWindow.CLICKED_ON_MOVE_ELLIPSE) {
				if (hud.x2 - Mouse.getX() <= 0) {
					hud.ellipseSide = 0;
					getCamera().moveDown();
					if(!xRayProjectionModule.getLockProjection())
					{
						//xRayProjectionModule.screenTransform.translate(new Vector3f(0, -Camera.CAMERA_MOVE_SPEED, 0));
						xRayProjectionModule.translateProjectionCamera(new Vector3f(0, -Camera.CAMERA_MOVE_SPEED, 0));
					}else{
						xRayProjectionModule.translateViewCamera(new Vector3f(0, -Camera.CAMERA_MOVE_SPEED, 0));
					}
				} else {
					hud.ellipseSide = 1;
					getCamera().moveUp();
					if(!xRayProjectionModule.getLockProjection())
					{
						//xRayProjectionModule.screenTransform.translate(new Vector3f(0, Camera.CAMERA_MOVE_SPEED, 0));
						xRayProjectionModule.translateProjectionCamera(new Vector3f(0, Camera.CAMERA_MOVE_SPEED, 0));
					}else{
						xRayProjectionModule.translateViewCamera(new Vector3f(0, Camera.CAMERA_MOVE_SPEED, 0));
					}
				}
			}

		} else {
			veinsWindow.setClickedOn(VeinsWindow.CLICKED_ON_NOTHING);
			getVeinsModel().veinsGrabbedAt = null;
			getVeinsModel().saveCurrentOrientation();
			getVeinsModel().setAddedOrientation(new Quaternion());
			lastHeldAt = null;
		}
		
	}

	@Override
	public void cleanup() {
		cleanShaders();
	}

	@Override
	public void setVeinsModel(VeinsModel veinsModel) {
		if(this.veinsModel != null)
			this.veinsModel.deleteMeshes();
		this.veinsModel = veinsModel;
		xRayProjectionModule.setVeinsModel(veinsModel);
		setDefaultViewOptions();
	}


}
