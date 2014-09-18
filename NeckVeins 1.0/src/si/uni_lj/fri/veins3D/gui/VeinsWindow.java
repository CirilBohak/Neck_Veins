package si.uni_lj.fri.veins3D.gui;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;
import java.util.Timer;

import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;

import com.leapmotion.leap.Controller;

import si.uni_lj.fri.veins3D.exceptions.ShaderLoadException;
import si.uni_lj.fri.veins3D.gui.render.VeinsRenderer;
import si.uni_lj.fri.veins3D.gui.settings.NeckVeinsSettings;
import si.uni_lj.fri.veins3D.math.Quaternion;
import si.uni_lj.fri.veins3D.utils.Mouse3D;
import si.uni_lj.fri.veins3D.utils.RayUtil;
import si.uni_lj.fri.veins3D.utils.LeapMotion;
import si.uni_lj.fri.veins3D.utils.SettingsUtil;
import de.matthiasmann.twl.Container;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.theme.ThemeManager;

/**
 * This is the root pane of the program.
 * 
 */
public class VeinsWindow extends Container {
	public final static int CLICKED_ON_NOTHING = 0;
	public final static int CLICKED_ON_VEINS_MODEL = 1;
	public final static int CLICKED_ON_ROTATION_CIRCLE = 2;
	public final static int CLICKED_ON_MOVE_CIRCLE = 3;
	public final static int CLICKED_ON_ROTATION_ELLIPSE = 4;
	public final static int CLICKED_ON_MOVE_ELLIPSE = 5;
	public final static int CLICKED_ON_BUTTONS = 6;

	public static NeckVeinsSettings settings;

	private VeinsFrame frame;
	private VeinsRenderer renderer;
	private HUD hud;
	private boolean isRunning;
	private String title;
	private int fps;
	private long timePastFrame;
	private long timePastFps;
	private int fpsToDisplay;
	private int clickedOn;
	private GUI gui;
	public static ThemeManager themeManager;
	private DisplayMode[] displayModes;
	private DisplayMode currentDisplayMode;
	public static Mouse3D joystick;
	LeapMotion leap;
    Controller leapController;

    /**
     * Used for assesment of controllers
     * 
     */
    //TODO: FOR USABILITY TEST - ADD ZOOM VARIABLE
    private final double[][] rotations={
    		{-90,90,0},
    		{0,180,0},
    		{0,0,90},
    		{45,0,0},
    		{0,55,0},
    		{0,50,15},
    		{80,0,0},
    		{0,60,0},
    		{0,180,0},
    		{128,45,30}
    };
    
    private long[][][] results;
    
    private String[] names={"Miska","3dMiska","LeapMotion"};
    
    private int selectedDevice=0;
    private int displayedRotation=0;
    private String userId="";
    private boolean showHud=false;
    private boolean getReady=false;
    private Date timer;
    
	/**
	 * 
	 */
	public VeinsWindow() {
		isRunning = true;
		title = "Veins3D - Usability test";
		loadSettings(title);
		createDisplay();
		initWindowElements();
		setupWindow();

		renderer.loadModelObj("res/models/teapot.obj");
		frame.setButtonsEnabled(true);
	}

	/**
	 * @param title
	 */
	public VeinsWindow(String title, String fileName) {
		isRunning = true;
		this.title = title;
		loadSettings(fileName);
		createDisplay();
		initWindowElements();
		setupWindow();
	}

	/**
	 * 
	 */
	/*
	 * private void setMouseSettings(NeckVeinsSettings settings){
	 * joystick.rotLock=settings.mRot; joystick.axisLock=settings.mTrans;
	 * joystick.strong=settings.mStrong; joystick.selected=settings.mSelected;
	 * joystick.setSensitivity(settings.sensitivity); }
	 */

	/**
	 * 
	 */
	private void loadSettings(String fileName) {
		try {
			displayModes = Display.getAvailableDisplayModes();
			// displayModeStrings = new String[displayModes.length];
			currentDisplayMode = Display.getDesktopDisplayMode();
			settings = SettingsUtil.readSettingsFile(fileName);
			if (settings != null) {
				for (DisplayMode mode : displayModes) {
					if (mode.getWidth() == settings.resWidth && mode.getHeight() == settings.resHeight
							&& mode.getBitsPerPixel() == settings.bitsPerPixel
							&& mode.getFrequency() == settings.frequency) {
						currentDisplayMode = mode;
					}
				}
				// setMouseSettings(settings);
			} else {
				settings = new NeckVeinsSettings();
				settings.isFpsShown = false;
				settings.fullscreen = true;
				settings.stereoEnabled = false;
				settings.stereoValue = 0;
				settings.locale = Locale.getDefault();
				settings.workingDirectory = "";
			}
			settings.resWidth = currentDisplayMode.getWidth();
			settings.resHeight = currentDisplayMode.getHeight();
			settings.bitsPerPixel = currentDisplayMode.getBitsPerPixel();
			settings.frequency = currentDisplayMode.getFrequency();
		} catch (LWJGLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	private void createDisplay() {
		try {
			Display.setDisplayMode(currentDisplayMode);
			Display.setTitle(title);
			Display.setVSyncEnabled(true);
			Display.create(new PixelFormat().withStencilBits(1));
			// Display.create();
		} catch (LWJGLException e) {
			e.printStackTrace();
			exitProgram(1);
		}
	}

	/**
	 * 
	 */
	private void initWindowElements() {
		try {
			hud = new HUD();
			renderer = new VeinsRenderer();
			frame = new VeinsFrame();
			gui = new GUI(frame, renderer);
			add(gui);
			setTheme("mainframe");
			joystick = new Mouse3D(settings);
			
			leap = new LeapMotion();
	        leapController = new Controller();
	        leapController.addListener(leap);
	        
			//Initalize user ID
			Date now=new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("ddMMhh");
			String uID=sdf.format(now);
			hud.userId=uID;
			
			results=new long[3][7][2]; //3 devices, 7 tests, 2 values
	        
		} catch (LWJGLException e) {
			e.printStackTrace();
			exitProgram(1);
		}
	}

	/**
	 * 
	 */
	private void setupWindow() {
		try {
			// Theme setup
			themeManager = ThemeManager.createThemeManager(VeinsFrame.class.getResource("/xml/simple.xml"), renderer);
			gui.applyTheme(themeManager);

			// OpenGL setup
			GL11.glClearStencil(0);
			renderer.setupView();
			renderer.prepareShaders();

			// Sync
			renderer.syncViewportSize();
			frame.invalidateLayout();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ShaderLoadException e) {
			e.printStackTrace();
			exitProgram(1);
		}
	}
	
	/**
	 * 
	 */
	public void mainLoop() {
		fps = 0;
		timePastFrame = (Sys.getTime() * 1000) / Sys.getTimerResolution();
		timePastFps = timePastFrame;
		fpsToDisplay = 0;
		renderer.setupView();
		
		
		
		while (!Display.isCloseRequested() && isRunning) {
			/* Reset view */
			renderer.resetView();

			/* Handle input and inform HUD about it */
			pollInput();
			hud.setClickedOn(clickedOn);

			/* Render */
			renderer.render();
			if(showHud)hud.drawHUD();
			hud.drawText();
			
			setTitle();

			/* Update */
			gui.update();
			Display.update();
			logic();
			Display.sync(settings.frequency);
		}
		
	}

	/**
	 * 
	 */
	private void logic() {
		// update framerate and calculate time that passed since last frame
		long time = (Sys.getTime() * 1000) / Sys.getTimerResolution();
		fps++;
		if (time - timePastFps >= 1000) {
			fpsToDisplay = fps;
			fps = 0;
			timePastFps = time;
			renderer.getCamera().normalizeCameraOrientation();
			if (renderer.getVeinsModel() != null) {
				renderer.getVeinsModel().normalizeAddedOrientation();
				renderer.getVeinsModel().normalizeCurrentOrientation();
			}
		}
		timePastFrame = time;
		
		if(getReady){

			long elapsed=0;
			Date now=new Date();
			Date until=new Date(timer.getTime()+300000);
			elapsed=until.getTime()- now.getTime();
			
			
			DateFormat formatter = new SimpleDateFormat("mm:ss");
			String dateFormatted = formatter.format(elapsed);
			
			hud.data=String.format("%s", dateFormatted);
			
			if(elapsed<0)endTesting();
		}
	}

	/**
	 * 
	 */
	private void setTitle() {
		if (settings.isFpsShown)
			Display.setTitle(title + " - FPS: " + fpsToDisplay);
		else
			Display.setTitle(title);
	}

	/**
	 * 
	 */
	public void pollInput() {
		pollKeyboardInput();
		
		if(selectedDevice==0)pollMouseInput();
		if(selectedDevice==1)poll3DMouseInput();
		if(selectedDevice==2)pollLeapMotionInput();
	}

	/**
	 * 
	 */
	private void pollKeyboardInput() {
		while (Keyboard.next()) {
			// if a key was pressed (vs.// released)
			if (Keyboard.getEventKeyState()) {
				if (Keyboard.getEventKey() == Keyboard.KEY_TAB) {
					settings.isFpsShown = !settings.isFpsShown;
				} else if (Keyboard.getEventKey() == Keyboard.KEY_1) {
					renderer.setActiveShaderProgram(VeinsRenderer.SIMPLE_SHADER);
				} else if (Keyboard.getEventKey() == Keyboard.KEY_2) {
					renderer.setActiveShaderProgram(VeinsRenderer.SIMPLE_SHADER_NORM_INTERP);
				} else if (Keyboard.getEventKey() == Keyboard.KEY_3) {
					renderer.setActiveShaderProgram(VeinsRenderer.SIMPLE_SHADER_NORM_INTERP_AMBIENT_L);
				} else if (Keyboard.getEventKey() == Keyboard.KEY_4) {
					renderer.setActiveShaderProgram(VeinsRenderer.SIMPLE_SHADER_NORM_INTERP_AMBIENT_L_SPEC_BLINN_PHONG);
				} else if (Keyboard.getEventKey() == Keyboard.KEY_5) {
					renderer.setActiveShaderProgram(VeinsRenderer.SIMPLE_SHADER_NORM_INTERP_AMBIENT_L_SPEC_PHONG);
				} else if (Keyboard.getEventKey() == Keyboard.KEY_6) {
					renderer.setActiveShaderProgram(VeinsRenderer.SHADER_6);
				} else if (Keyboard.getEventKey() == Keyboard.KEY_7) {
					renderer.setActiveShaderProgram(VeinsRenderer.SHADER_7);
				} else if (Keyboard.getEventKey() == Keyboard.KEY_8) {
					renderer.setActiveShaderProgram(VeinsRenderer.SHADER_8);
				} else if (Keyboard.getEventKey() == Keyboard.KEY_0) {
					renderer.setActiveShaderProgram(VeinsRenderer.FIXED_PIPELINE);
				} else if (Keyboard.getEventKey() == Keyboard.KEY_9) {
					renderer.switchWireframe();
				} else if (Keyboard.getEventKey() == Keyboard.KEY_ADD && renderer.getVeinsModel() != null) {
					renderer.getVeinsModel().increaseSubdivisionDepth();
				} else if (Keyboard.getEventKey() == Keyboard.KEY_SUBTRACT && renderer.getVeinsModel() != null) {
					renderer.getVeinsModel().decreaseSubdivisionDepth();
				} else if (Keyboard.getEventKey() == Keyboard.KEY_9) {
					renderer.switchAA();
				} else if (Keyboard.getEventKey() == Keyboard.KEY_L) {
					if (settings.locale.getLanguage().equals("sl"))
						settings.locale = new Locale("en", "US");
					else
						settings.locale = new Locale("sl", "SI");
					frame.setLanguageSpecific();
				} else if (Keyboard.getEventKey() == Keyboard.KEY_RETURN) {
					double [] rot={-90,180,0};
					renderer.getVeinsModel().setOrientation(rot);
				} else if (Keyboard.getEventKey() == Keyboard.KEY_F1) {
					selectedDevice=0;
					hud.title="Primer "+displayedRotation+" - "+names[selectedDevice];
				}else if (Keyboard.getEventKey() == Keyboard.KEY_F2) {
					selectedDevice=1;
					hud.title="Primer "+displayedRotation+" - "+names[selectedDevice];
				}else if (Keyboard.getEventKey() == Keyboard.KEY_F3) {
					selectedDevice=2;
					hud.title="Primer "+displayedRotation+" - "+names[selectedDevice];
				} else if (Keyboard.getEventKey() == Keyboard.KEY_NUMPAD0) {
					displayedRotation=0;
					hud.title="Primer "+displayedRotation+" - "+names[selectedDevice];
					hud.tooltip="Pritisnite presledek za zacetek testiranja";
					getReady=false;
					renderer.getDummyModel().setOrientation(rotations[displayedRotation]);
					
				}  else if (Keyboard.getEventKey() == Keyboard.KEY_NUMPAD1) {
					displayedRotation=1;
					hud.title="Primer "+displayedRotation+" - "+names[selectedDevice];
					hud.tooltip="Pritisnite presledek za zacetek testiranja";
					getReady=false;
					renderer.getDummyModel().setOrientation(rotations[displayedRotation]);
					
				}   else if (Keyboard.getEventKey() == Keyboard.KEY_NUMPAD2) {
					displayedRotation=2;
					hud.title="Primer "+displayedRotation+" - "+names[selectedDevice];
					hud.tooltip="Pritisnite presledek za zacetek testiranja";
					getReady=false;
					renderer.getDummyModel().setOrientation(rotations[displayedRotation]);
					
				}   else if (Keyboard.getEventKey() == Keyboard.KEY_NUMPAD3) {
					displayedRotation=3;
					hud.title="Primer "+displayedRotation+" - "+names[selectedDevice];
					hud.tooltip="Pritisnite presledek za zacetek testiranja";
					getReady=false;
					renderer.getDummyModel().setOrientation(rotations[displayedRotation]);
					
				}   else if (Keyboard.getEventKey() == Keyboard.KEY_NUMPAD4) {
					displayedRotation=4;
					hud.title="Primer "+displayedRotation+" - "+names[selectedDevice];
					hud.tooltip="Pritisnite presledek za zacetek testiranja";
					getReady=false;
					renderer.getDummyModel().setOrientation(rotations[displayedRotation]);
					
				}   else if (Keyboard.getEventKey() == Keyboard.KEY_NUMPAD5) {
					displayedRotation=5;
					hud.title="Primer "+displayedRotation+" - "+names[selectedDevice];
					hud.tooltip="Pritisnite presledek za zacetek testiranja";
					getReady=false;
					renderer.getDummyModel().setOrientation(rotations[displayedRotation]);
					
				}   else if (Keyboard.getEventKey() == Keyboard.KEY_NUMPAD6) {
					displayedRotation=6;
					hud.title="Primer "+displayedRotation+" - "+names[selectedDevice];
					hud.tooltip="Pritisnite presledek za zacetek testiranja";
					getReady=false;
					renderer.getDummyModel().setOrientation(rotations[displayedRotation]);
					
				}
				
				else if (Keyboard.getEventKey() == Keyboard.KEY_SPACE) {
					if(!getReady){
						//Start timer
						timer=new Date();
						getReady=true;

						hud.tooltip="Pritisni presledek ko koncaste z nalogo";
					}
					else{
						endTesting();
					}
				} 
			}
		}

		if (!frame.isDialogOpened())
			return;

		// moving the camera
		if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
			renderer.getCamera().lookUp();
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
			renderer.getCamera().lookDown();
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
			renderer.getCamera().lookRight();
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
			renderer.getCamera().lookLeft();
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_Q)) {
			renderer.getCamera().rotateCounterClockwise();
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_E)) {
			renderer.getCamera().rotateClockwise();
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
			renderer.getCamera().moveForward();
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
			renderer.getCamera().moveBackwards();
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
			renderer.getCamera().moveRight();
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
			renderer.getCamera().moveLeft();
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_R)) {
			renderer.getCamera().moveUp();
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_F)) {
			renderer.getCamera().moveDown();
		}

	}
		
	double normalizeAngle(double angle)
	{
		double newAngle = angle;
	    while (newAngle <= 0) newAngle += 1;
	    while (newAngle > 1) newAngle -= 1;
	    return newAngle;
	}
	
	private void endTesting(){
		//Calculate accuracy of rotations
		// TODO: Add timer functionality and Control with space, which task is currently assigned
		double calculatedAccuracy=0;
		double[] curOrientation=renderer.getVeinsModel().currentOrientation.getVectorPart();			
		double[] plannedOrientation=renderer.getDummyModel().currentOrientation.getVectorPart();
		System.out.println(renderer.getVeinsModel().currentOrientation.toString());
		System.out.println(renderer.getDummyModel().currentOrientation.toString());
		//System.out.println("Dot: "+renderer.getVeinsModel().currentOrientation.);

		//System.out.printf("\tVeins:[%.3f,%.3f,%.3f]",curOrientation[0],curOrientation[1],curOrientation[2]);
		//System.out.printf("\n\tGoal:[%.3f,%.3f,%.3f]",plannedOrientation[0],plannedOrientation[1],plannedOrientation[2]);
		
		
		//To absolute
		double a11=curOrientation[0];
		double a12=curOrientation[1];
		double a13=curOrientation[2];
		double a21=plannedOrientation[0];
		double a22=plannedOrientation[1];
		double a23=plannedOrientation[2];
		
		
		//System.out.printf("\tVeins:[%.3f,%.3f,%.3f]",a11,a12,a13);
		//System.out.printf("\n\tGoal:[%.3f,%.3f,%.3f]",a21,a22,a23);
		
		
		
		
		calculatedAccuracy=Math.abs(curOrientation[0]-plannedOrientation[0])+
				Math.abs(curOrientation[1]-plannedOrientation[1])+
				Math.abs(curOrientation[2]-plannedOrientation[2]);
		
		System.out.println("\n\tCalculated accuracy for Example "+displayedRotation+" is: "+calculatedAccuracy);
		
		
		long elapsed=0;
		Date now=new Date();
		elapsed=now.getTime()-timer.getTime();
		
		DateFormat formatter = new SimpleDateFormat("mm:ss:SSS");
		String dateFormatted = formatter.format(elapsed);
		
		hud.tooltip="Za naslednjo nalogo pritisnte Numpad "+(displayedRotation+1);
		if(displayedRotation+1==10)hud.tooltip="Testiranje zakljuceno";
		hud.data=String.format("Cas: %s\tNatancnost: %.2f", dateFormatted,calculatedAccuracy);
		
		getReady=false;
		
	}

	private void pollMouseInput() {
		if (!frame.isDialogOpened() || renderer.getVeinsModel() == null)
			return;
		
		int z = Mouse.getDWheel();
		if (z > 0) {
			//renderer.getCamera().zoomIn();
			renderer.getCamera().cameraZ-=100;
			System.out.println(renderer.getCamera().cameraZ);
			if(renderer.getCamera().cameraZ<500)renderer.getCamera().cameraZ=500;
		} else if (z < 0) {
//			renderer.getCamera().zoomOut();
			renderer.getCamera().cameraZ+=100;
			System.out.println(renderer.getCamera().cameraZ);
			if(renderer.getCamera().cameraZ>1500)renderer.getCamera().cameraZ=1500;
		}

		if (Mouse.isButtonDown(0)) {
			calculateClickedOn();

			if (clickedOn == CLICKED_ON_VEINS_MODEL) {
				renderer.getVeinsModel().changeAddedOrientation(renderer);
			}

			if (clickedOn == CLICKED_ON_ROTATION_CIRCLE || clickedOn == CLICKED_ON_MOVE_CIRCLE) {
				float x = (clickedOn == VeinsWindow.CLICKED_ON_ROTATION_CIRCLE) ? hud.x1 : hud.x2;
				float y = (clickedOn == VeinsWindow.CLICKED_ON_ROTATION_CIRCLE) ? hud.y1 : hud.y2;

				float clickToCircleDistance = (float) Math.sqrt((x - Mouse.getX()) * (x - Mouse.getX())
						+ (y - Mouse.getY()) * (y - Mouse.getY()));
				float upRotation = (Mouse.getY() - y)
						/ ((clickToCircleDistance > hud.r) ? clickToCircleDistance : hud.r);
				float rightRotation = (Mouse.getX() - x)
						/ ((clickToCircleDistance > hud.r) ? clickToCircleDistance : hud.r);

				hud.clickToCircleDistance = Math.min(clickToCircleDistance, hud.r) / hud.r;
				hud.clickOnCircleAngle = (float) Math.atan2(Mouse.getY() - y, Mouse.getX() - x);

				if (clickedOn == CLICKED_ON_ROTATION_CIRCLE) {
					renderer.getCamera().rotate(upRotation, rightRotation);
				} else {
					renderer.getCamera().move(upRotation, rightRotation);
				}

			}

			if (clickedOn == CLICKED_ON_ROTATION_ELLIPSE) {
				if (hud.x1 - Mouse.getX() <= 0) {
					hud.ellipseSide = 0;
					renderer.getCamera().rotateClockwise();
				} else {
					hud.ellipseSide = 1;
					renderer.getCamera().rotateCounterClockwise();
				}
			}

			if (clickedOn == CLICKED_ON_MOVE_ELLIPSE) {
				if (hud.x2 - Mouse.getX() <= 0) {
					hud.ellipseSide = 0;
					renderer.getCamera().moveDown();
				} else {
					hud.ellipseSide = 1;
					renderer.getCamera().moveUp();
				}
			}

		} else {
			clickedOn = CLICKED_ON_NOTHING;
			renderer.getVeinsModel().veinsGrabbedAt = null;
			renderer.getVeinsModel().saveCurrentOrientation();
			renderer.getVeinsModel().setAddedOrientation(new Quaternion());
		}

	}

	private void poll3DMouseInput() {

		if (joystick.connected() && renderer.getVeinsModel() != null) {
			joystick.pollMouse();
			if (settings.mSelected)
				renderer.getCamera().moveCamera3D(joystick.getAxis(), joystick.getRot());
			else {
				renderer.getCamera().moveCamera3D(
						new double[] { -joystick.getAxisX(), -joystick.getAxisY(), -joystick.getAxisZ() },
						new double[] { 0, 0, 0 });
				renderer.getVeinsModel().rotateModel3D(joystick.getRot(), renderer);
			}
			if(renderer.getCamera().cameraZ>1500)renderer.getCamera().cameraZ=1500;
			if(renderer.getCamera().cameraZ<500)renderer.getCamera().cameraZ=500;
		}
	}
	float[] lastPosition={0,0,0,0}; //X,Y,tilt,Camera
	boolean openedPalm=false;
	private void pollLeapMotionInput(){
		double sensitivity=(double)settings.leapSensitivity;
		if(leap.isPalm()){
			//TODO: Leap
				
			float[] axis=leap.getAxis();
			float tilt=leap.getTilt();
			if(!openedPalm){
				lastPosition[0]=axis[0];
				lastPosition[1]=axis[1];
				lastPosition[2]=tilt;
				lastPosition[3]=axis[2];
			}
				double[] addedRotation=new double[3];
				addedRotation[2]=(lastPosition[0]-axis[0])/sensitivity;
				addedRotation[0]=(lastPosition[1]-axis[1])/sensitivity;
				addedRotation[1]=-(lastPosition[2]-tilt)/sensitivity;
				
				lastPosition[0]=axis[0];
				lastPosition[1]=axis[1];
				lastPosition[2]=tilt;
				
				//System.out.println(Arrays.toString(addedRotation));
				
				renderer.getCamera().cameraZ= -axis[2]+200; //in/out
				renderer.getVeinsModel().rotateModel3D(addedRotation, renderer);

				if(renderer.getCamera().cameraZ>1500)renderer.getCamera().cameraZ=1500;
				if(renderer.getCamera().cameraZ<500)renderer.getCamera().cameraZ=500;
		}
		openedPalm=leap.isPalm();
	}
	
	/**
	 * Calculates on which element mouse click was performed - on HUD element or
	 * on veins model
	 */
	private void calculateClickedOn() {
		float distanceToRotationCircle = (hud.x1 - Mouse.getX()) * (hud.x1 - Mouse.getX()) + (hud.y1 - Mouse.getY())
				* (hud.y1 - Mouse.getY());

		float distanceToMoveCircle = (hud.x2 - Mouse.getX()) * (hud.x2 - Mouse.getX()) + (hud.y2 - Mouse.getY())
				* (hud.y2 - Mouse.getY());

		float distanceToRotationFoci = (float) (Math.sqrt((hud.x1 - hud.f - Mouse.getX())
				* (hud.x1 - hud.f - Mouse.getX()) + (hud.y1 - Mouse.getY()) * (hud.y1 - Mouse.getY())) + Math
				.sqrt((hud.x1 + hud.f - Mouse.getX()) * (hud.x1 + hud.f - Mouse.getX()) + (hud.y1 - Mouse.getY())
						* (hud.y1 - Mouse.getY())));

		float distanceToMoveFoci = (float) (Math.sqrt((hud.x2 - hud.f - Mouse.getX()) * (hud.x2 - hud.f - Mouse.getX())
				+ (hud.y2 - Mouse.getY()) * (hud.y2 - Mouse.getY())) + Math.sqrt((hud.x2 + hud.f - Mouse.getX())
				* (hud.x2 + hud.f - Mouse.getX()) + (hud.y2 - Mouse.getY()) * (hud.y2 - Mouse.getY())));

		if (clickedOn == CLICKED_ON_NOTHING) {
			if (settings.resHeight - Mouse.getY() < settings.resHeight / 18) {
				clickedOn = CLICKED_ON_BUTTONS;

			} else if (distanceToRotationCircle <= hud.r * hud.r && showHud) {
				clickedOn = CLICKED_ON_ROTATION_CIRCLE;

			} else if (distanceToMoveCircle <= hud.r * hud.r && showHud) {
				clickedOn = CLICKED_ON_MOVE_CIRCLE;

			} else if (distanceToRotationFoci <= hud.r * 3f && showHud) {
				clickedOn = CLICKED_ON_ROTATION_ELLIPSE;

			} else if (distanceToMoveFoci <= hud.r * 3f && showHud) {
				clickedOn = CLICKED_ON_MOVE_ELLIPSE;

			} else {
				renderer.getVeinsModel().veinsGrabbedAt = RayUtil.getRaySphereIntersection(Mouse.getX(), Mouse.getY(),
						renderer);
//				System.out.println(renderer.getVeinsModel().veinsGrabbedAt[0] + " " + renderer.getVeinsModel().veinsGrabbedAt[1]);
				// renderer.getVeinsModel().setAddedOrientation(new
				// Quaternion());
				if (renderer.getVeinsModel().veinsGrabbedAt != null)
					clickedOn = CLICKED_ON_VEINS_MODEL;
			}
		}
	}

	/**
	 * @param n
	 */
	public void exitProgram(int n) {
		//TODO: write CSV writer!
		
		SettingsUtil.writeSettingsFile(settings, title);
		leapController.removeListener(leap);
		renderer.cleanShaders();
		gui.destroy();
		if (themeManager != null)
			themeManager.destroy();
		Display.destroy();
		System.exit(n);

	}

	/**
	 * @return
	 */
	public DisplayMode[] getDisplayModes() {
		return displayModes;
	}

	/**
	 * @return
	 */
	public DisplayMode getCurrentDisplayMode() {
		return currentDisplayMode;
	}

	/**
	 * @return
	 */
	public HUD getHUD() {
		return hud;
	}

}
