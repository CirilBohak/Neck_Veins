package si.uni_lj.fri.veins3D.gui;

import java.io.File;
import java.util.ResourceBundle;

import org.lwjgl.LWJGLException;
import org.lwjgl.opencl.OpenCLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.util.glu.Project;

import si.uni_lj.fri.veins3D.gui.render.VeinsRenderer;
import si.uni_lj.fri.veins3D.gui.render.VeinsRendererInterface;
import si.uni_lj.fri.veins3D.gui.render.models.VeinsModel;
import de.matthiasmann.twl.BorderLayout;
import de.matthiasmann.twl.BorderLayout.Location;
import de.matthiasmann.twl.BoxLayout;
import de.matthiasmann.twl.BoxLayout.Direction;
import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.FileSelector;
import de.matthiasmann.twl.FileSelector.Callback2;
import de.matthiasmann.twl.FileTable.Entry;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.ListBox;
import de.matthiasmann.twl.PopupWindow;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.Scrollbar;
import de.matthiasmann.twl.TextArea;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.JavaFileSystemModel;
import de.matthiasmann.twl.model.SimpleChangableListModel;
import de.matthiasmann.twl.renderer.MouseCursor;
import de.matthiasmann.twl.textarea.SimpleTextAreaModel;

public class VeinsFrame extends Widget {
	private enum Message {
		FALLBACK, IMPORT, LOADING;
	}

	private FileSelector fileSelector;
	private Scrollbar gaussFileOptionsScroll;
	private Scrollbar threshFileOptionsScroll;
	private boolean isDialogOpened;
	private Button openButton;
	private Button exitButton;
	private Scrollbar stereoScrollbar;
	private ToggleButton stereoToggleButton;
	private ToggleButton helpButton;
	private TextArea helpTextArea;
	private ScrollPane helpScrollPane;
	private SimpleTextAreaModel stamHelp;
	private ToggleButton creditsButton;
	private SimpleTextAreaModel stamCredits;
	private Button displayModesButton;
	private int selectedResolution;
	private Button okayVideoSettingButton;
	private Button cancelVideoSettingButton;
	private ToggleButton fullscreenToggleButton;
	private ListBox<String> displayModeListBox;
	private String[] displayModeStrings;
	private DisplayMode[] displayModes;
	private DisplayMode currentDisplayMode;
	
	// 3Dmouse
	private static ToggleButton mouse3d;
	private Scrollbar sensitivityScrollbar;
	private ToggleButton strong;
	private ToggleButton lockRot;
	private ToggleButton lockTrans;
	private Button camObj;
	
	//Leap
	private Scrollbar leapSensitivityScrollbar;
	private ToggleButton leapShowIcon;

	// Threshold scroll
	private BorderLayout thresholdLayout;
	private Label thresholdLabel;
	private Scrollbar thresholdScrollbar;
	private Label thresholdLevel;
	private Button applyThreshBtn;
	private Button exportObjBtn;

	// Min Triangles Scroll
	private BorderLayout minTriangelsLayout;
	private Scrollbar minTrianglesScrollbar;
	private Label minTriangelsValue;

	// Error Popup
	private PopupWindow errorPop;
	private Label errorPopLabel;

	// Window backref
	VeinsWindow veinsWindow;
	
	public VeinsFrame(VeinsWindow veinsWindow) throws LWJGLException {
		this.veinsWindow = veinsWindow;
		getDisplayModes();
		initGUI();
		setLanguageSpecific();
		setTheme("mainframe");
	}

	private void getDisplayModes() throws LWJGLException {
		displayModes = Display.getAvailableDisplayModes();
		currentDisplayMode = Display.getDisplayMode();
	}

	private void initGUI() {
		initMinTrianglesScroll();
		initThresholdScroll();
		initFileSelector();
		initOpenButton();
		initExitButton();
		initStereoScrollBar();
		initStereoToggleButton();
		initHelpToggleButton();
		initCreditsToggleButton();
		initTextArea();
		initDisplayModesButton();
		initVideoSettingsButtons();
		initDisplayModeListBox();
		init3DmouseButtons();
		initErrorPopup();
	}

	private void initMinTrianglesScroll() {
		Label minTriangelsLabel = new Label("Min triangles per component:");
		minTriangelsValue = new Label("0");
		minTriangelsValue.setTheme("value-label");

		minTrianglesScrollbar = new Scrollbar(Scrollbar.Orientation.HORIZONTAL);
		minTrianglesScrollbar.setMinMaxValue(0, 1000);
		minTrianglesScrollbar.setValue(0);
		minTrianglesScrollbar.addCallback(new Runnable() {
			@Override
			public void run() {
				setWaitCursor();
				applyMinTrianglesValue();
			}
		});

		minTriangelsLayout = new BorderLayout();
		minTriangelsLayout.add(minTrianglesScrollbar, Location.WEST);
		minTriangelsLayout.add(minTriangelsValue, Location.EAST);
		minTriangelsLayout.add(minTriangelsLabel, Location.NORTH);
		minTriangelsLayout.setVisible(false);
		add(minTriangelsLayout);
	}

	private void applyMinTrianglesValue() {
		VeinsRenderer renderer = (VeinsRenderer) VeinsFrame.this.getGUI().getRenderer();
		VeinsModel model = renderer.getVeinsModel();
		int scrollMaxValue = minTrianglesScrollbar.getMaxValue();
		int scrollCurrentValue = minTrianglesScrollbar.getValue();
		int minTriangels = (int) (model.maxTriangels * ((float) scrollCurrentValue / (float) (scrollMaxValue)));
		model.changeMinTriangles(minTriangels);
		minTriangelsValue.setText(Integer.toString(minTriangels));
	}

	private void initThresholdScroll() {
		thresholdLayout = new BorderLayout();
		thresholdLayout.setTheme("borderlayout");

		thresholdLabel = new Label("Threshold");
		thresholdScrollbar = new Scrollbar(Scrollbar.Orientation.VERTICAL);
		thresholdScrollbar.setTheme("vscrollbar");
		thresholdScrollbar.setTooltipContent("Change threshold value.");
		thresholdScrollbar.setMinMaxValue(1, 100);
		thresholdScrollbar.setValue(50);
		thresholdScrollbar.addCallback(new Runnable() {
			@Override
			public void run() {
				thresholdLevel.setText(Float.toString(thresholdScrollbar.getValue() / 100.0f));
			}
		});
		applyThreshBtn = new Button("Apply");
		applyThreshBtn.addCallback(new Runnable() {
			@Override
			public void run() {
				try {
					setWaitCursor();
					applyModelThreshold();
					minTrianglesScrollbar.setValue(0);
				} catch (LWJGLException | OpenCLException e) {
					e.printStackTrace();
					handleLWJGLException(false);
				}
			}

		});
		exportObjBtn = new Button("Export");
		thresholdLevel = new Label("0.5");
		thresholdLevel.setTheme("value-label");

		BorderLayout buttonHolder = new BorderLayout();
		buttonHolder.add(thresholdLevel, Location.NORTH);
		buttonHolder.add(exportObjBtn, Location.SOUTH);
		buttonHolder.add(applyThreshBtn, Location.CENTER);
		thresholdLayout.add(thresholdLabel, Location.NORTH);
		thresholdLayout.add(buttonHolder, Location.WEST);
		thresholdLayout.add(thresholdScrollbar, Location.EAST);
		thresholdLayout.setVisible(false);
		this.add(thresholdLayout);
	}

	private void applyModelThreshold() throws LWJGLException {
		VeinsRenderer renderer = (VeinsRenderer) VeinsFrame.this.getGUI().getRenderer();
		renderer.changeModel(thresholdScrollbar.getValue() / 100.0f);
	}

	private void initFileSelector() {
		fileSelector = new FileSelector();
		fileSelector.setTheme("fileselector");
		fileSelector.setVisible(false);
		JavaFileSystemModel fsm = JavaFileSystemModel.getInstance();
		fileSelector.setFileSystemModel(fsm);
		fileSelector.setUserWidgetBottom(initSegmentationOptions());
		fileSelector.getUserWidgetBottom().setEnabled(false);
		fileSelector.setCurrentFolder(new File(VeinsWindow.settings.workingDirectory));
		Callback2 cb = new Callback2() {
			@Override
			public void filesSelected(Object[] files) {
				setWaitCursor();
				fileSelector.setVisible(false);
				setButtonsEnabled(true);
				openFile((File) files[0]);
			}

			@Override
			public void canceled() {
				setButtonsEnabled(true);
				fileSelector.setVisible(false);
			}

			@Override
			public void folderChanged(Object arg0) {
				if (arg0 instanceof File)
					VeinsWindow.settings.workingDirectory = ((File) arg0).getAbsolutePath();
			}

			@Override
			public void selectionChanged(Entry[] arg0) {
				if (arg0[0].getExtension().equals("mhd")) {
					fileSelector.getUserWidgetBottom().setEnabled(true);
				} else {
					fileSelector.getUserWidgetBottom().setEnabled(false);
				}
			}
		};
		fileSelector.addCallback(cb);
		add(fileSelector);
	}

	private void setWaitCursor() {
		//VeinsRenderer renderer = (VeinsRenderer) VeinsFrame.this.getGUI().getRenderer();
		//renderer.setCursor((MouseCursor) VeinsWindow.themeManager.getCursor("cursor.wait"));
	}

	private void openFile(File file) {
		System.out.println("\nOpening file: " + file.getAbsolutePath());
		try {
			if (fileExtensionEquals(file, "mhd")) {
				showThresholdOptions(true);
				openMhd(file);
				VeinsRenderer renderer = (VeinsRenderer) VeinsFrame.this.getGUI().getRenderer();
				thresholdScrollbar.setValue((int) (renderer.getVeinsModel().threshold * 100));
			} else if(fileExtensionEquals(file, "obj")){
				showThresholdOptions(false);
				openObj(file);
			} else if(fileExtensionEquals(file, "jpg") || fileExtensionEquals(file, "png")){
				veinsWindow.renderers.stream().filter(r->r instanceof VeinsRenderer).forEach((VeinsRendererInterface r)->{((VeinsRenderer)r).xRayProjectionModule.loadProjectionTexture(file.getAbsolutePath());});
			
			}
			
		} catch (LWJGLException | OpenCLException e) {
			e.printStackTrace();
			handleLWJGLException(true);
		}
	}

	private void openMhd(File file) throws LWJGLException {
		double sigma = gaussFileOptionsScroll.getValue() / (double) gaussFileOptionsScroll.getMaxValue();
		sigma = (gaussFileOptionsScroll.isEnabled()) ? sigma : -1;
		double threshold = threshFileOptionsScroll.getValue() / (double) threshFileOptionsScroll.getMaxValue();
		threshold = (threshFileOptionsScroll.isEnabled()) ? threshold : -1;
		VeinsRenderer renderer = (VeinsRenderer) VeinsFrame.this.getGUI().getRenderer();
		renderer.loadModelRaw(file.getAbsolutePath(), sigma, threshold);
	}

	private void openMhdSafeMode(File file) {
		double sigma = gaussFileOptionsScroll.getValue() / (double) gaussFileOptionsScroll.getMaxValue();
		sigma = (gaussFileOptionsScroll.isEnabled()) ? sigma : -1;
		double threshold = threshFileOptionsScroll.getValue() / (double) threshFileOptionsScroll.getMaxValue();
		threshold = (threshFileOptionsScroll.isEnabled()) ? threshold : -1;
		VeinsRenderer renderer = (VeinsRenderer) VeinsFrame.this.getGUI().getRenderer();
		renderer.loadModelRawSafeMode(file.getAbsolutePath(), sigma, threshold);
	}

	private void openObj(File file) {
		//VeinsRendererInterface renderer = (VeinsRendererInterface) VeinsFrame.this.getGUI().getRenderer();
		//renderer.loadModelObj(file.getAbsolutePath());
		VeinsModel veinsModel = new VeinsModel();
		veinsModel.constructVBOFromObjFile(file.getAbsolutePath());
		veinsWindow.renderers.forEach((VeinsRendererInterface vri)->vri.setVeinsModel(veinsModel));
	}

	// TODO
	private void handleLWJGLException(boolean isFallBack) {
		showErrorPopupOkBtn(isFallBack);
		showThresholdOptions(false);
		setErrorPopLabel((isFallBack) ? Message.FALLBACK : Message.IMPORT);
		errorPop.openPopupCentered();
	}

	private void showErrorPopupOkBtn(boolean show) {
		BorderLayout errorLayout = (BorderLayout) errorPop.getChild(0);
		errorLayout.getChild(Location.EAST).getChild(0).setVisible(show);
	}

	private boolean fileExtensionEquals(File file, String ext) {
		String[] tokens = file.getAbsolutePath().split("\\.(?=[^\\.]+$)");
		return tokens[tokens.length - 1].equals(ext);
	}

	private void showThresholdOptions(boolean show) {
		thresholdLayout.setVisible(show);
		minTriangelsLayout.setVisible(show);
	}

	private Widget initSegmentationOptions() {
		/* Gauss Options */
		final Label sigmaValue = new Label("0.50");
		sigmaValue.setTheme("value-label");
		gaussFileOptionsScroll = new Scrollbar(Scrollbar.Orientation.HORIZONTAL);
		gaussFileOptionsScroll.setTheme("hscrollbar");
		gaussFileOptionsScroll.setTooltipContent("Sets Gaussian filter sigma.");
		gaussFileOptionsScroll.setMinMaxValue(1, 100);
		gaussFileOptionsScroll.setValue(50);
		gaussFileOptionsScroll.addCallback(new Runnable() {
			public void run() {
				sigmaValue.setText(Float.toString(gaussFileOptionsScroll.getValue() / 100.0f));
			}
		});
		final Button gaussEnableBtn = new Button("Disable");
		gaussEnableBtn.addCallback(new Runnable() {
			public void run() {
				gaussFileOptionsScroll.setEnabled(gaussEnableBtn.getText().equals("Enable"));
				gaussEnableBtn.setText(gaussEnableBtn.getText().equals("Enable") ? "Disable" : "Enable");
			}
		});
		Label gaussLabel = new Label("Sets Gaussian filter sigma.");
		gaussLabel.setTheme("title-label");
		BorderLayout gaussOptions = new BorderLayout();
		gaussOptions.add(gaussFileOptionsScroll, Location.WEST);
		gaussOptions.add(sigmaValue, Location.CENTER);
		gaussOptions.add(gaussEnableBtn, Location.EAST);
		gaussOptions.add(gaussLabel, Location.NORTH);

		/* Threshold options */
		final Label threshValue = new Label("0.50");
		threshValue.setTheme("value-label");
		threshFileOptionsScroll = new Scrollbar(Scrollbar.Orientation.HORIZONTAL);
		threshFileOptionsScroll.setTheme("hscrollbar");
		threshFileOptionsScroll.setTooltipContent("Sets the sigma value.");
		threshFileOptionsScroll.setMinMaxValue(1, 100);
		threshFileOptionsScroll.setValue(50);
		threshFileOptionsScroll.addCallback(new Runnable() {
			public void run() {
				threshValue.setText(Float.toString(threshFileOptionsScroll.getValue() / 100.0f));
			}
		});
		final Button autoThreshBtn = new Button("Auto");
		autoThreshBtn.addCallback(new Runnable() {
			public void run() {
				threshFileOptionsScroll.setEnabled(autoThreshBtn.getText().equals("Manual"));
				autoThreshBtn.setText(autoThreshBtn.getText().equals("Auto") ? "Manual" : "Auto");
			}
		});
		Label thresholdLabel = new Label("Set threshold level:");
		thresholdLabel.setTheme("title-label");
		BorderLayout thresholdOptions = new BorderLayout();
		thresholdOptions.add(threshFileOptionsScroll, Location.WEST);
		thresholdOptions.add(threshValue, Location.CENTER);
		thresholdOptions.add(autoThreshBtn, Location.EAST);
		thresholdOptions.add(thresholdLabel, Location.NORTH);

		BorderLayout userBottomWidget = new BorderLayout();
		userBottomWidget.add(gaussOptions, Location.NORTH);
		userBottomWidget.add(thresholdOptions, Location.SOUTH);

		return userBottomWidget;
	}

	private void initOpenButton() {
		openButton = new Button("Open...");
		openButton.setTheme("button");
		openButton.setTooltipContent("Open the dialog with the file chooser to select an .obj file.");
		openButton.addCallback(new Runnable() {
			public void run() {
				showFileSelector();
			}
		});
		add(openButton);

	}

	private void initExitButton() {
		exitButton = new Button("Exit");
		exitButton.setTheme("button");
		exitButton.setTooltipContent("Terminates this program.");
		exitButton.addCallback(new Runnable() {
			public void run() {
				((VeinsWindow) VeinsFrame.this.getGUI().getParent()).exitProgram(0);
			}
		});
		add(exitButton);
	}

	private void initStereoScrollBar() {
		stereoScrollbar = new Scrollbar(Scrollbar.Orientation.HORIZONTAL);
		stereoScrollbar.setTheme("hscrollbar");
		stereoScrollbar.setTooltipContent("Sets the distance between eyes.");
		stereoScrollbar.setMinMaxValue(-1000, 1000);
		stereoScrollbar.setValue(VeinsWindow.settings.stereoValue);
		stereoScrollbar.addCallback(new Runnable() {
			public void run() {
				VeinsWindow.settings.stereoValue = stereoScrollbar.getValue();
			}
		});
		add(stereoScrollbar);
	}

	private void initStereoToggleButton() {
		stereoToggleButton = new ToggleButton("Stereo (3D)");
		stereoToggleButton.setTheme("togglebutton");
		stereoToggleButton.setTooltipContent("Toggles interlaced 3D picture. Requires an appropriate display.");
		stereoToggleButton.addCallback(new Runnable() {
			public void run() {
				VeinsWindow.settings.stereoEnabled = true;
				stereoScrollbar.setVisible(true);
				invalidateLayout();
				if (stereoToggleButton.isActive()) {
				} else {
					VeinsWindow.settings.stereoEnabled = false;
					stereoScrollbar.setVisible(false);
					invalidateLayout();
				}
			}
		});
		add(stereoToggleButton);

		if (VeinsWindow.settings.stereoEnabled) {
			stereoScrollbar.setVisible(true);
			stereoToggleButton.setActive(true);
		} else {
			stereoScrollbar.setVisible(false);
			stereoToggleButton.setActive(false);
		}
	}

	private void initHelpToggleButton() {
		helpButton = new ToggleButton("Help");
		helpButton.setTheme("togglebutton");
		helpButton.setTooltipContent("Shows controls.");
		helpButton.addCallback(new Runnable() {
			public void run() {
				if (helpButton.isActive()) {
					helpTextArea.setModel(stamHelp);
					helpScrollPane.setVisible(true);
					setButtonsEnabled(false);
					helpButton.setEnabled(true);
				} else {
					helpScrollPane.setVisible(false);
					setButtonsEnabled(true);
				}
			}
		});
		add(helpButton);
	}

	private void initCreditsToggleButton() {
		creditsButton = new ToggleButton("Licensing");
		creditsButton.setTheme("togglebutton");
		creditsButton.setTooltipContent("Shows authorship and licensing information.");
		creditsButton.addCallback(new Runnable() {
			public void run() {
				if (creditsButton.isActive()) {
					helpTextArea.setModel(stamCredits);
					helpScrollPane.setVisible(true);
					setButtonsEnabled(false);
					creditsButton.setEnabled(true);
				} else {
					helpScrollPane.setVisible(false);
					setButtonsEnabled(true);
				}
			}
		});
		add(creditsButton);
	}

	private void initTextArea() {
		helpTextArea = new TextArea();
		helpTextArea.setTheme("textarea");
		stamHelp = new SimpleTextAreaModel();
		stamCredits = new SimpleTextAreaModel();
		helpTextArea.setModel(stamHelp);

		helpScrollPane = new ScrollPane();
		helpScrollPane.setTheme("scrollpane");
		helpScrollPane.setVisible(false);
		add(helpScrollPane);
		helpScrollPane.setContent(helpTextArea);
	}

	/**
	 * 
	 */
	private void initDisplayModesButton() {
		displayModesButton = new Button("Display Modes...");
		displayModesButton.setTheme("button");
		displayModesButton.setTooltipContent("Open the list with the available display modes.");
		displayModesButton.addCallback(new Runnable() {
			public void run() {
				listDisplayModes();
			}
		});
		add(displayModesButton);
	}

	private void initVideoSettingsButtons() {
		okayVideoSettingButton = new Button("Okay");
		cancelVideoSettingButton = new Button("Cancel");
		fullscreenToggleButton = new ToggleButton("Toggle Fullscreen");

		okayVideoSettingButton.setVisible(false);
		cancelVideoSettingButton.setVisible(false);
		fullscreenToggleButton.setVisible(false);

		okayVideoSettingButton.setTheme("button");
		cancelVideoSettingButton.setTheme("button");
		fullscreenToggleButton.setTheme("togglebutton");
		okayVideoSettingButton.addCallback(new Runnable() {
			@Override
			public void run() {
				confirmVideoSetting();
			}
		});
		cancelVideoSettingButton.addCallback(new Runnable() {
			@Override
			public void run() {
				cancelVideoSetting();
			}
		});
		fullscreenToggleButton.addCallback(new Runnable() {
			@Override
			public void run() {
				if (fullscreenToggleButton.isActive()) {
					try {
						Display.setFullscreen(true);
						Display.setVSyncEnabled(true);
						VeinsWindow.settings.fullscreen = true;
					} catch (LWJGLException e) {
						e.printStackTrace();
					}
				} else {
					try {
						VeinsWindow.settings.fullscreen = false;
						Display.setFullscreen(false);
					} catch (LWJGLException e) {
						e.printStackTrace();
					}
				}
			}
		});
		if (VeinsWindow.settings.fullscreen) {
			try {
				Display.setFullscreen(true);
				Display.setVSyncEnabled(true);
				fullscreenToggleButton.setActive(true);
			} catch (LWJGLException e) {
				e.printStackTrace();
			}
		} else {
			try {
				Display.setFullscreen(false);
				fullscreenToggleButton.setActive(false);
			} catch (LWJGLException e) {
				e.printStackTrace();
			}
		}
		add(okayVideoSettingButton);
		add(cancelVideoSettingButton);
		add(fullscreenToggleButton);
	}

	private void initDisplayModeListBox() {
		displayModeStrings = new String[displayModes.length];
		displayModeListBox = new ListBox<String>();
		displayModeListBox.setTheme("listbox");
		displayModeListBox.setVisible(false);
		add(displayModeListBox);

		int i = 0;
		selectedResolution = -1;
		for (DisplayMode displayMode : displayModes) {
			if (displayMode.getWidth() == currentDisplayMode.getWidth()
					&& displayMode.getHeight() == currentDisplayMode.getHeight()
					&& displayMode.getBitsPerPixel() == currentDisplayMode.getBitsPerPixel()
					&& displayMode.getFrequency() == currentDisplayMode.getFrequency()) {
				selectedResolution = i;
			}
			displayModeStrings[i++] = String.format("%1$5d x%2$5d x%3$3dbit x%4$3dHz", displayMode.getWidth(),
					displayMode.getHeight(), displayMode.getBitsPerPixel(), displayMode.getFrequency());
		}

		SimpleChangableListModel<String> scListModel = new SimpleChangableListModel<String>();
		for (String str : displayModeStrings) {
			scListModel.addElement(str);
		}

		displayModeListBox.setModel(scListModel);
		if (selectedResolution != -1)
			displayModeListBox.setSelected(selectedResolution);
	}

	private void init3DmouseButtons() {
		mouse3d = new ToggleButton("3d Mouse / Leap");
		mouse3d.setTheme("togglebutton");
		mouse3d.setTooltipContent("3d Mouse settings");
		mouse3d.addCallback(new Runnable() {
			public void run() {
				mouse3d.setEnabled(VeinsWindow.joystick.connected()|| VeinsWindow.leap.isConnected());
				mouseSettingsVisible(mouse3d.isActive() && mouse3d.isEnabled());
			}
		});
		add(mouse3d);

		sensitivityScrollbar = new Scrollbar(Scrollbar.Orientation.HORIZONTAL);
		sensitivityScrollbar.setTheme("hscrollbar");
		sensitivityScrollbar.setTooltipContent("Sets the sensitivity of the mouse.");
		sensitivityScrollbar.setMinMaxValue(1, 200);
		sensitivityScrollbar.setValue(201 - VeinsWindow.settings.sensitivity);
		sensitivityScrollbar.addCallback(new Runnable() {
			public void run() {
				VeinsWindow.settings.sensitivity = (201 - sensitivityScrollbar.getValue());
			}
		});
		add(sensitivityScrollbar);
		
		if (VeinsWindow.settings.mSelected)
			camObj = new Button("Camera active");
		else
			camObj = new Button("Object active");
		camObj.setTheme("button");
		camObj.setTooltipContent("Toggle between camera or object");
		camObj.addCallback(new Runnable() {
			public void run() {
				if (VeinsWindow.settings.mSelected) {
					VeinsWindow.settings.mSelected = false;
					camObj.setText("Object active");
				} else {
					VeinsWindow.settings.mSelected = true;
					camObj.setText("Camera active");
				}
			}
		});
		add(camObj);

		strong = new ToggleButton("Strong axis");
		strong.setActive(VeinsWindow.settings.mStrong);
		strong.setTheme("togglebutton");
		strong.setTooltipContent("React only on strong axis of the mouse");
		strong.addCallback(new Runnable() {
			public void run() {
				VeinsWindow.settings.mStrong = strong.isActive();
			}
		});
		add(strong);

		lockRot = new ToggleButton("Rotations");
		lockRot.setActive(VeinsWindow.settings.mRot);
		lockRot.setTheme("togglebutton");
		lockRot.setTooltipContent("Locks rotational axis");
		lockRot.addCallback(new Runnable() {
			public void run() {
				VeinsWindow.settings.mRot = lockRot.isActive();
			}
		});
		add(lockRot);

		lockTrans = new ToggleButton("Translations");
		lockTrans.setActive(VeinsWindow.settings.mTrans);
		lockTrans.setTheme("togglebutton");
		lockTrans.setTooltipContent("Locks translationals axis");
		lockTrans.addCallback(new Runnable() {
			public void run() {
				VeinsWindow.settings.mTrans = lockTrans.isActive();
			}
		});
		add(lockTrans);

		Label leapSensitivityLabel = new Label("Leap Motion sensitivity");
		leapSensitivityScrollbar = new Scrollbar(Scrollbar.Orientation.HORIZONTAL);
		leapSensitivityScrollbar.setTheme("hscrollbar");
		leapSensitivityScrollbar.setTooltipContent("Sets the sensitivity of the leap.");
		leapSensitivityScrollbar.setMinMaxValue(1, 150);
		leapSensitivityScrollbar.setValue(151 - VeinsWindow.settings.leapSensitivity);
		leapSensitivityScrollbar.addCallback(new Runnable() {
			public void run() {
				VeinsWindow.settings.leapSensitivity = (151 - leapSensitivityScrollbar.getValue());
			}
		});
		add(leapSensitivityScrollbar);
		
		leapShowIcon = new ToggleButton("Show hand icon");
		lockRot.setActive(VeinsWindow.settings.showLeapIcon);
		leapShowIcon.setTheme("togglebutton");
		leapShowIcon.setTooltipContent("Show if hand is held, released");
		leapShowIcon.addCallback(new Runnable() {
			public void run() {
				VeinsWindow.settings.showLeapIcon=leapShowIcon.isActive();
			}
		});
		add(leapShowIcon);
	}

	// TODO edit error widget look
	private void initErrorPopup() {
		Button okBtn = new Button("Ok");
		okBtn.addCallback(new Runnable() {
			@Override
			public void run() {
				setWaitCursor();
				errorPop.closePopup();
				String lastFilePath = fileSelector.getFileTable().getSelection()[0].getPath();
				openMhdSafeMode(new File(lastFilePath));
			}
		});
		Button cancelBtn = new Button("Close");
		cancelBtn.addCallback(new Runnable() {
			@Override
			public void run() {
				errorPop.closePopup();
			}
		});

		Widget btns = new BoxLayout(Direction.HORIZONTAL);
		btns.add(okBtn);
		btns.add(cancelBtn);
		errorPopLabel = new Label();

		BorderLayout errorWidget = new BorderLayout();
		errorWidget.add(errorPopLabel, Location.NORTH);
		errorWidget.add(btns, Location.EAST);

		errorPop = new PopupWindow(this);
		errorPop.setTheme("errorPopup");
		errorPop.setCloseOnClickedOutside(false);
		errorPop.add(errorWidget);
	}

	public void setButtonsEnabled(boolean enabled) {
		isDialogOpened = enabled;
		openButton.setEnabled(enabled);
		displayModesButton.setEnabled(enabled);
		helpButton.setEnabled(enabled);
		creditsButton.setEnabled(enabled);
		thresholdLayout.setEnabled(enabled);
		minTriangelsLayout.setEnabled(enabled);
	}

	public void mouseSettingsVisible(boolean visible) {
		strong.setVisible(visible);
		lockRot.setVisible(visible);
		lockTrans.setVisible(visible);
		camObj.setVisible(visible);
		sensitivityScrollbar.setVisible(visible);
		leapSensitivityScrollbar.setVisible(visible);
		leapShowIcon.setVisible(visible);
	}

	/**
	 * @since 0.4
	 * @version 0.4
	 */
	public void showFileSelector() {
		fileSelector.setVisible(true);
		setButtonsEnabled(false);
	}

	/**
	 * @since 0.4
	 * @version 0.4
	 */
	public void listDisplayModes() {
		okayVideoSettingButton.setVisible(true);
		cancelVideoSettingButton.setVisible(true);
		displayModeListBox.setVisible(true);
		fullscreenToggleButton.setVisible(true);
		setButtonsEnabled(false);
		displayModeListBox.setSelected(selectedResolution);
	}

	/**
	 * @since 0.4
	 * @version 0.4
	 */
	public void confirmVideoSetting() {
		GUI gui = getGUI();
		VeinsRenderer renderer = (VeinsRenderer) gui.getRenderer();
		DisplayMode[] displayModes = ((VeinsWindow) gui.getParent()).getDisplayModes();
		DisplayMode currentDisplayMode = ((VeinsWindow) gui.getParent()).getCurrentDisplayMode();
		okayVideoSettingButton.setVisible(false);
		cancelVideoSettingButton.setVisible(false);
		displayModeListBox.setVisible(false);
		fullscreenToggleButton.setVisible(false);
		setButtonsEnabled(true);
		if (selectedResolution != displayModeListBox.getSelected()) {
			selectedResolution = displayModeListBox.getSelected();
			currentDisplayMode = displayModes[selectedResolution];
			try {
				Display.setDisplayMode(currentDisplayMode);
				VeinsWindow.settings.resWidth = currentDisplayMode.getWidth();
				VeinsWindow.settings.resHeight = currentDisplayMode.getHeight();
			} catch (LWJGLException e) {
				e.printStackTrace();
			}
			renderer.setupView();
			renderer.syncViewportSize();
			invalidateLayout();
		}
	}

	/**
	 * @since 0.4
	 * @version 0.4
	 */
	public void cancelVideoSetting() {
		okayVideoSettingButton.setVisible(false);
		cancelVideoSettingButton.setVisible(false);
		displayModeListBox.setVisible(false);
		fullscreenToggleButton.setVisible(false);
		setButtonsEnabled(true);
	}

	/**
	 * @since 0.4
	 * @version 0.4
	 */
	@Override
	protected void layout() {
		openButton.adjustSize();
		displayModesButton.adjustSize();
		stereoToggleButton.adjustSize();
		helpButton.adjustSize();
		creditsButton.adjustSize();
		exitButton.adjustSize();
		mouse3d.adjustSize();
		strong.adjustSize();
		lockRot.adjustSize();
		lockTrans.adjustSize();
		leapSensitivityScrollbar.adjustSize();
		leapShowIcon.adjustSize();
		

		int openHeight = Math.max(25, VeinsWindow.settings.resHeight / 18);
		int widthBy7 = VeinsWindow.settings.resWidth / 7 + 1;
		openButton.setSize(widthBy7, openHeight);
		openButton.setPosition(0, 0);
		displayModesButton.setPosition(widthBy7, 0);
		displayModesButton.setSize(widthBy7, openHeight);

		if (VeinsWindow.settings.stereoEnabled) {
			stereoToggleButton.setPosition(widthBy7 * 2, 0);
			stereoToggleButton.setSize(widthBy7, openHeight / 2);
			stereoScrollbar.setPosition(widthBy7 * 2, openHeight / 2);
			stereoScrollbar.setSize(widthBy7, openHeight / 2);
			// stereoScrollbar.setMinSize(VeinsWindow.settings.resWidth/36,
			// openHeight);
		} else {
			stereoToggleButton.setPosition(widthBy7 * 2, 0);
			stereoToggleButton.setSize(widthBy7, openHeight);

			stereoScrollbar.setPosition(widthBy7 * 2, openHeight);
			stereoScrollbar.setSize(widthBy7, openHeight);
		}

		helpButton.setPosition(widthBy7 * 3, 0);
		helpButton.setSize(widthBy7, openHeight);
		creditsButton.setPosition(widthBy7 * 4, 0);
		creditsButton.setSize(widthBy7, openHeight);
		mouse3d.setPosition(widthBy7 * 5, 0);
		mouse3d.setSize(widthBy7, openHeight);
		exitButton.setPosition(widthBy7 * 6, 0);
		exitButton.setSize(VeinsWindow.settings.resWidth - widthBy7 * 6, openHeight);

		strong.setPosition(0, openHeight);
		strong.setSize(widthBy7 * 2, openHeight);
		lockRot.setPosition(0, openHeight * 2);
		lockRot.setSize(widthBy7, openHeight);
		lockTrans.setPosition(widthBy7, openHeight * 2);
		lockTrans.setSize(widthBy7, openHeight);
		camObj.setPosition(0, openHeight * 3);
		camObj.setSize(widthBy7 * 2, openHeight);
		sensitivityScrollbar.setPosition(0, openHeight * 4);
		sensitivityScrollbar.setSize(widthBy7 * 2, openHeight / 2);
		leapSensitivityScrollbar.setPosition(0, openHeight * 5-openHeight / 2);
		leapSensitivityScrollbar.setSize(widthBy7 * 2, openHeight / 2);
		leapShowIcon.setPosition(0, openHeight * 5);
		leapShowIcon.setSize(widthBy7*2, openHeight);
		
		mouseSettingsVisible(mouse3d.isActive());
		mouse3d.setEnabled(VeinsWindow.joystick.connected() || VeinsWindow.leap.isConnected()||true);

		int rlWidth = VeinsWindow.settings.resWidth * 8 / 10;
		int rlHeight = VeinsWindow.settings.resHeight * 6 / 10;
		displayModeListBox.setSize(rlWidth, rlHeight);
		displayModeListBox.setPosition(VeinsWindow.settings.resWidth / 2 - rlWidth / 2,
				VeinsWindow.settings.resHeight / 6);

		fullscreenToggleButton.adjustSize();
		int fullToggleWidth = Math.max(fullscreenToggleButton.getWidth(), VeinsWindow.settings.resWidth / 6);
		fullscreenToggleButton.setSize(fullToggleWidth, openHeight);
		fullscreenToggleButton.setPosition(VeinsWindow.settings.resWidth / 2 - rlWidth / 2,
				VeinsWindow.settings.resHeight * 19 / 24);

		cancelVideoSettingButton.adjustSize();
		int cancelVideoSettingWidth = Math.max(cancelVideoSettingButton.getWidth(), VeinsWindow.settings.resWidth / 6);
		cancelVideoSettingButton.setSize(cancelVideoSettingWidth, openHeight);
		cancelVideoSettingButton.setPosition(VeinsWindow.settings.resWidth / 2 + rlWidth / 2 - cancelVideoSettingWidth,
				VeinsWindow.settings.resHeight * 19 / 24);

		okayVideoSettingButton.adjustSize();
		int okayVideoSettingWidth = Math.max(okayVideoSettingButton.getWidth(), VeinsWindow.settings.resWidth / 6);
		okayVideoSettingButton.setSize(okayVideoSettingWidth, openHeight);
		okayVideoSettingButton.setPosition(VeinsWindow.settings.resWidth / 2 + rlWidth / 2 - cancelVideoSettingWidth
				- okayVideoSettingWidth, VeinsWindow.settings.resHeight * 19 / 24);

		fileSelector.adjustSize();
		int fsHeight = VeinsWindow.settings.resHeight * 19 / 24 + openHeight - VeinsWindow.settings.resWidth / 2
				+ rlWidth / 2;
		fileSelector.setSize(rlWidth, fsHeight);
		fileSelector.setPosition(VeinsWindow.settings.resWidth / 2 - rlWidth / 2, VeinsWindow.settings.resHeight / 6);
		for (int i = 0; i < fileSelector.getUserWidgetBottom().getNumChildren(); i++) {
			BorderLayout options = (BorderLayout) fileSelector.getUserWidgetBottom().getChild(i);
			Widget west = options.getChild(Location.WEST);
			west.setMinSize((int) (VeinsWindow.settings.resWidth / 1.6f), 20);
			Widget center = options.getChild(Location.CENTER);
			center.setMinSize(VeinsWindow.settings.resWidth / 10, 20);
			Widget east = options.getChild(Location.EAST);
			east.setMinSize(VeinsWindow.settings.resWidth / 10, 20);
			options.adjustSize();
		}

		helpScrollPane.setSize(rlWidth, fsHeight);
		helpScrollPane.setPosition(VeinsWindow.settings.resWidth / 2 - rlWidth / 2, VeinsWindow.settings.resHeight / 6);
		helpTextArea.setSize(rlWidth, fsHeight);

		thresholdLayout.setSize(exportObjBtn.getWidth() + thresholdScrollbar.getWidth(),
				VeinsWindow.settings.resHeight / 4);
		int positionX = VeinsWindow.settings.resWidth - thresholdLayout.getWidth() - VeinsWindow.settings.resWidth / 30;
		int positionY = VeinsWindow.settings.resHeight - thresholdLayout.getHeight() - VeinsWindow.settings.resHeight
				/ 30;
		thresholdLayout.setPosition(positionX, positionY);

		minTrianglesScrollbar.setMinSize((int) (VeinsWindow.settings.resWidth / 5), 20);
		minTriangelsLayout.getChild(Location.EAST).setMinSize(VeinsWindow.settings.resWidth / 10, 20);
		minTriangelsLayout.adjustSize();
		minTriangelsLayout.setPosition(20, positionY + thresholdLayout.getHeight() - minTriangelsLayout.getHeight());
	}

	public void setLanguageSpecific() {
		ResourceBundle labels = ResourceBundle.getBundle("inter/LabelsBundle", VeinsWindow.settings.locale);

		openButton.setText(labels.getString("openBtnLabel"));
		openButton.setTooltipContent(labels.getString("openBtnTooltip"));

		exitButton.setText(labels.getString("exitBtnLabel"));
		exitButton.setTooltipContent(labels.getString("exitBtnTooltip"));

		stereoScrollbar.setTooltipContent(labels.getString("stereoScrollbarTooltip"));
		stereoToggleButton.setText(labels.getString("stereoBtnLabel"));
		stereoToggleButton.setTooltipContent(labels.getString("stereoBtnTooltip"));

		helpButton.setText(labels.getString("helpBtnLabel"));
		helpButton.setTooltipContent(labels.getString("helpBtnTooltip"));
		creditsButton.setText(labels.getString("creditsBtnLabel"));
		creditsButton.setTooltipContent(labels.getString("creditsBtnTooltip"));

		displayModesButton.setText(labels.getString("displayModesBtnLabel"));
		displayModesButton.setTooltipContent(labels.getString("displayModesBtnTooltip"));

		okayVideoSettingButton.setText(labels.getString("okayBtnLabel"));
		cancelVideoSettingButton.setText(labels.getString("cancelBtnLabel"));
		fullscreenToggleButton.setText(labels.getString("fullscreenBtnLabel"));

		mouse3d.setText(labels.getString("mouse3dBtnLabel"));
		mouse3d.setTooltipContent(labels.getString("mouse3dBtnTooltip"));

		strong.setText(labels.getString("strongBtnLabel"));
		lockRot.setText(labels.getString("lockRotBtnLabel"));
		lockTrans.setText(labels.getString("lockTransBtnLabel"));
		
		leapShowIcon.setText(labels.getString("leapShowIconBtnLabel"));

		errorPopLabel.setText(labels.getString("popupErrorMsg1"));

		ResourceBundle credits = ResourceBundle.getBundle("inter/Credits", VeinsWindow.settings.locale);
		ResourceBundle help = ResourceBundle.getBundle("inter/Help", VeinsWindow.settings.locale);

		stamHelp.setText(help.getString("help"));
		stamCredits.setText(credits.getString("credits"));

		ResourceBundle.clearCache();
	}

	private void setErrorPopLabel(Message m) {
		ResourceBundle labels = ResourceBundle.getBundle("inter/LabelsBundle", VeinsWindow.settings.locale);
		String key = "";
		switch (m) {
		case FALLBACK:
			key = "popupErrorMsg1";
			break;
		case IMPORT:
			key = "popupErrorMsg2";
			break;
		case LOADING:
			key = "popupErrorMsg3";
			break;
		}
		errorPopLabel.setText(labels.getString(key));
	}

	public boolean isDialogOpened() {
		return isDialogOpened;
	}

}
