package si.uni_lj.fri.veins3D.gui;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;

import com.tpxl.GL.exception.GLFramebufferException;
import com.tpxl.GL.exception.GLProgramLinkException;
import com.tpxl.GL.exception.GLShaderCompileException;

import si.uni_lj.fri.veins3D.exceptions.ShaderLoadException;
import si.uni_lj.fri.veins3D.gui.render.VeinsRenderer;
import si.uni_lj.fri.veins3D.gui.render.VeinsRendererInterface;
import si.uni_lj.fri.veins3D.gui.settings.NeckVeinsSettings;
import si.uni_lj.fri.veins3D.math.Quaternion;
import si.uni_lj.fri.veins3D.utils.Mouse3D;
import si.uni_lj.fri.veins3D.utils.LeapMotion;
import si.uni_lj.fri.veins3D.utils.RayUtil;
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
	private VeinsRendererInterface currentRenderer;
	public ArrayList<VeinsRendererInterface> renderers;
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
	public static LeapMotion leap;

	public int getClickedOn()
	{
		return clickedOn;
	}
	
	public void setClickedOn(int clickedOn)
	{
		this.clickedOn = clickedOn;
	}
	
	/**
	 * 
	 */
	public VeinsWindow() {
		clickedOn = 0;
		isRunning = true;
		title = "Veins3D";
		loadSettings(title);
		createDisplay();
		initWindowElements();
		setupWindow();
	}

	/**
	 * @param title
	 */
	public VeinsWindow(String title, String fileName) {
		clickedOn = 0;
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
			renderers = new ArrayList<VeinsRendererInterface>();
			renderers.add(new VeinsRenderer());
			renderers.add(new XRayProjectionModule());
			currentRenderer = renderers.get(0);
			frame = new VeinsFrame(this);
			gui = new GUI(frame, currentRenderer);
			add(gui);
			setTheme("mainframe");
			joystick = new Mouse3D(settings);
			leap = new LeapMotion(settings);
	        
		} catch (LWJGLException e) {
			e.printStackTrace();
			exitProgram(1);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GLShaderCompileException e) {
			System.out.println(e.getMessage());
			System.out.println(e.getInfoLog());
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
	 * 
	 */
	private void setupWindow() {
		try {
			// Theme setup
			themeManager = ThemeManager.createThemeManager(VeinsFrame.class.getResource("/xml/simple.xml"), currentRenderer);
			gui.applyTheme(themeManager);

			// OpenGL setup
			GL11.glClearStencil(0);
			currentRenderer.setupView();
			currentRenderer.loadShaders();

			// Sync
			currentRenderer.syncViewportSize();
			frame.invalidateLayout();
		} catch (IOException e) {
			e.printStackTrace();
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
		currentRenderer.setupView();
		while (!Display.isCloseRequested() && isRunning) {
			/* Reset view */
			currentRenderer.resetView();

			/* Handle input and inform HUD about it */
			pollInput();
			hud.setClickedOn(clickedOn);

			/* Render */
			currentRenderer.render();
			hud.drawHUD();
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
			if(currentRenderer instanceof VeinsRenderer)
			{
				((VeinsRenderer)(currentRenderer)).getCamera().normalizeCameraOrientation();
				if (((VeinsRenderer)(currentRenderer)).getVeinsModel() != null) {
					((VeinsRenderer)(currentRenderer)).getVeinsModel().normalizeAddedOrientation();
					((VeinsRenderer)(currentRenderer)).getVeinsModel().normalizeCurrentOrientation();
				}
			}
		}
		timePastFrame = time;
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
		pollMouseInput();
		poll3DMouseInput();
		pollLeapMotionInput();
	}

	/**
	 * 
	 */
	private void pollKeyboardInput() {
		while (Keyboard.next()) {
			// if a key was pressed (vs.// released)
			currentRenderer.handleKeyboardInputPresses();
			if (Keyboard.getEventKeyState()) {
				if (Keyboard.getEventKey() == Keyboard.KEY_TAB) {
					settings.isFpsShown = !settings.isFpsShown;
				} else if (Keyboard.getEventKey() == Keyboard.KEY_L) {
					if (settings.locale.getLanguage().equals("sl"))
						settings.locale = new Locale("en", "US");
					else
						settings.locale = new Locale("sl", "SI");
					frame.setLanguageSpecific();
				}
			}
		}
		currentRenderer.handleKeyboardInputContinuous();
		if (!frame.isDialogOpened())
			return;


	}

	private void pollMouseInput() {
		if (!frame.isDialogOpened())
			return;
		
		int dx = Mouse.getDX();
		int dy = Mouse.getDY();
		int dz = Mouse.getDWheel();
		/*
		if (Mouse.isButtonDown(0)) {
			calculateClickedOn();
		}*/
		currentRenderer.handleMouseInput(dx, dy, dz, hud, this);


	}

	private void poll3DMouseInput() {
/*
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
		}*/
	}

	private void pollLeapMotionInput(){
		/*
		double sensitivity=(double)settings.leapSensitivity;
		leap.poll();
		if(!leap.isPalm()){
			float[] translations=leap.getAxisTranslations();
			double[] rotations=leap.getAxisRotations();
			renderer.getCamera().moveCamera3D(
					new double[] { translations[0], translations[2], -translations[1] },
					new double[] { 0, 0, 0 });
			
			renderer.getVeinsModel().rotateModel3D(rotations, renderer);
		}*/
	}
	
	/**
	 * Calculates on which element mouse click was performed - on HUD element or
	 * on veins model
	 */
	public void calculateClickedOn() {
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

			} else if (distanceToRotationCircle <= hud.r * hud.r) {
				clickedOn = CLICKED_ON_ROTATION_CIRCLE;

			} else if (distanceToMoveCircle <= hud.r * hud.r) {
				clickedOn = CLICKED_ON_MOVE_CIRCLE;

			} else if (distanceToRotationFoci <= hud.r * 3f) {
				clickedOn = CLICKED_ON_ROTATION_ELLIPSE;

			} else if (distanceToMoveFoci <= hud.r * 3f) {
				clickedOn = CLICKED_ON_MOVE_ELLIPSE;

			} else if(currentRenderer instanceof VeinsRenderer){
				((VeinsRenderer)currentRenderer).getVeinsModel().veinsGrabbedAt = RayUtil.getRaySphereIntersection(Mouse.getX(), Mouse.getY(),	(VeinsRenderer)currentRenderer);
				// renderer.getVeinsModel().setAddedOrientation(new
				// Quaternion());
				if (((VeinsRenderer)currentRenderer).getVeinsModel().veinsGrabbedAt != null)
					clickedOn = CLICKED_ON_VEINS_MODEL;
			}
		}
	}

	/**
	 * @param n
	 */
	public void exitProgram(int n) {
		SettingsUtil.writeSettingsFile(settings, title);
		currentRenderer.cleanup();
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
