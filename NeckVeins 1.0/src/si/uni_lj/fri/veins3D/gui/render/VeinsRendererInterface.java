package si.uni_lj.fri.veins3D.gui.render;
import java.io.IOException;

import org.lwjgl.LWJGLException;

import si.uni_lj.fri.veins3D.gui.HUD;
import si.uni_lj.fri.veins3D.gui.VeinsWindow;
import si.uni_lj.fri.veins3D.gui.render.models.VeinsModel;
import de.matthiasmann.twl.renderer.lwjgl.LWJGLRenderer;

public abstract class VeinsRendererInterface extends LWJGLRenderer{
	public VeinsRendererInterface() throws LWJGLException {
		super();
	}
	public abstract void render();
	public abstract void handleKeyboardInputPresses();
	public abstract void handleKeyboardInputContinuous();
	public abstract void handleMouseInput(int dx, int dy, int dz, HUD hud, VeinsWindow veinsWindow);
	public abstract void setupView();
	public abstract void resetView();
	public abstract void loadShaders() throws IOException;
	public abstract void cleanup();
	public abstract void setVeinsModel(VeinsModel veinsModel);
}
