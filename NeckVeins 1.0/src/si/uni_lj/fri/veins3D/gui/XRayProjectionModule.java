package si.uni_lj.fri.veins3D.gui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.newdawn.slick.opengl.GLUtils;

import si.uni_lj.fri.veins3D.gui.render.models.Mesh;
import si.uni_lj.fri.veins3D.gui.render.models.VeinsModel;
import si.uni_lj.fri.veins3D.utils.Tools;
import si.uni_lj.fri.veins3D.gui.render.VeinsRendererInterface;
import si.uni_lj.fri.veins3D.math.Quaternion;

import com.tpxl.GL.ArrayBuffer;
import com.tpxl.GL.Camera;
import com.tpxl.GL.ElementBuffer;
import com.tpxl.GL.FragmentShader;
import com.tpxl.GL.Framebuffer;
import com.tpxl.GL.OrthoCamera;
import com.tpxl.GL.PerspectiveCamera;
import com.tpxl.GL.Program;
import com.tpxl.GL.Shader;
import com.tpxl.GL.Texture;
import com.tpxl.GL.Transform;
import com.tpxl.GL.Utility;
import com.tpxl.GL.VertexArrayObject;
import com.tpxl.GL.VertexShader;
import com.tpxl.GL.exception.GLFramebufferException;
import com.tpxl.GL.exception.GLProgramLinkException;
import com.tpxl.GL.exception.GLShaderCompileException;

public class XRayProjectionModule extends VeinsRendererInterface{
	private static final String resourceLocation = "res//";
	
	public boolean showWireframe,
					dirtyProjectionCamera,
					dirtyViewCamera,
					showScreen;
	private boolean lockProjection;
	
	private float screenTransparency;
	public Camera 	projectionCamera,	//Make private + getters/setters
				  	viewCamera;
	
	public Camera activeCamera;
	
	private VertexArrayObject model,
							  screen;
	public Transform 	modelTransform,	//Make private + getters/setters
						screenTransform;
	public Program 	programProject,	//Make private, no need for setters/getters
					programTextureProject,
					programDepth;
	
	private Framebuffer depthBuffer;
	private Texture projectionTexture;
	private FloatBuffer floatBuffer;
	
	private ArrayBuffer screenVertexBuffer,
						screenNormalBuffer,
						screenUVBuffer,
						modelVertexBuffer,
						modelNormalBuffer;
	private ElementBuffer 	screenElementBuffer,
							modelElementBuffer;
	
	public VeinsModel veinsModel;
	
	public boolean getLockProjection(){
		return lockProjection;
	}
	
	public void flipCamera()
	{
		if(activeCamera == viewCamera)
			activeCamera = projectionCamera;
		else
			activeCamera = viewCamera;
		lockProjection = !lockProjection;
	}
	
	public XRayProjectionModule() throws FileNotFoundException, IOException, GLShaderCompileException, GLProgramLinkException, GLFramebufferException, LWJGLException
	{
		super();
		
		lockProjection = true;
		showWireframe = false;
		dirtyProjectionCamera = true;
		dirtyViewCamera = true;
		showScreen = true;
		
		screenTransparency = 0.5f;
		
		screenTransform = new Transform();
		modelTransform = new Transform();
		
		//viewCamera = new PerspectiveCamera();
		float ratio = (float)VeinsWindow.settings.resWidth/(float)VeinsWindow.settings.resHeight;
		viewCamera = new OrthoCamera(-200*ratio, 200*ratio, -200, 200, -200, 200);
		viewCamera.translate(new Vector3f(0, 0, 100));
		
		//projectionCamera = new OrthoCamera(-100, 100, -100, 100, -100, 100);
		//projectionCamera = new PerspectiveCamera(viewCamera);
		projectionCamera = new OrthoCamera((OrthoCamera)viewCamera);
		activeCamera = viewCamera;
		//model = new VertexArrayObject();
		//TODO: this needs to adapt to screen resolution
		depthBuffer = Framebuffer.getDepthFramebuffer(VeinsWindow.settings.resWidth, VeinsWindow.settings.resHeight);
		//depthBuffer = Framebuffer.getDepthFramebuffer(1920, 1080);

		
		//projectionTexture = XRayProjectionModule.getTexture(resourceLocation + "imgs//Pat12_2D_DSA_AP.jpg");
		loadProjectionTexture(resourceLocation + "imgs//Pat12_2D_DSA_AP.jpg");
		
		//projectionTexture = XRayProjectionModule.getTexture("resources//tex.bmp");
		floatBuffer = BufferUtils.createFloatBuffer(16);
		
		veinsModel = new VeinsModel();
		veinsModel.constructVBOFromObjFile(resourceLocation + "obj//square.obj");
		System.out.println("Veins model: " + veinsModel.centerx + " " + veinsModel.centery + " " + veinsModel.centerz);
		screen = new VertexArrayObject();
		model = new VertexArrayObject();
		
		modelVertexBuffer = new ArrayBuffer(GL15.GL_STATIC_DRAW);
		modelNormalBuffer = new ArrayBuffer(GL15.GL_STATIC_DRAW);
		modelElementBuffer = new ElementBuffer(GL15.GL_STATIC_DRAW);
		
		screenVertexBuffer = new ArrayBuffer(GL15.GL_STATIC_DRAW);
		screenNormalBuffer = new ArrayBuffer(GL15.GL_STATIC_DRAW);
		screenUVBuffer = new ArrayBuffer(GL15.GL_STATIC_DRAW);
		screenElementBuffer = new ElementBuffer(GL15.GL_STATIC_DRAW);

		Mesh m = veinsModel.getMeshes().get(0);
		m.getVertices().forEach(System.out::println);
		FloatBuffer tmpFloatBuffer = Tools.arrayListToBuffer(m.getVertices(), null);
		System.out.println("pos: " + tmpFloatBuffer.position());
		for(int i=0; i < tmpFloatBuffer.limit(); i+=3)
		{
			System.out.println(tmpFloatBuffer.get(i) + " " + tmpFloatBuffer.get(i+1) + " " + tmpFloatBuffer.get(i+2));
		}
		screenVertexBuffer.setData(tmpFloatBuffer);
		tmpFloatBuffer.clear();
		tmpFloatBuffer.put(Mesh.getNormals(m.getVertices(), m.getFaces()));
		tmpFloatBuffer.rewind();
		System.out.println("pos: " + tmpFloatBuffer.position());
		for(int i=0; i < tmpFloatBuffer.limit(); i+=3)
		{
			System.out.println(tmpFloatBuffer.get(i) + " " + tmpFloatBuffer.get(i+1) + " " + tmpFloatBuffer.get(i+2));
		}
		screenNormalBuffer.setData(tmpFloatBuffer);
		
		
		IntBuffer tmpIBuffer = BufferUtils.createIntBuffer(m.getFaces().size());
		m.getFaces().forEach((Integer i)->{tmpIBuffer.put(i-1);});
		tmpIBuffer.rewind();
		
		System.out.println("pos: " + tmpIBuffer.position());
		for(int i=0; i < tmpIBuffer.limit(); i+=3)
		{
			System.out.println(tmpIBuffer.get(i) + " " + tmpIBuffer.get(i+1) + " " + tmpIBuffer.get(i+2));
		}
		
		screenElementBuffer.setData(tmpIBuffer);
		
		screen.bind();
		screen.setElementBuffer(screenElementBuffer);
		screen.enableVertexAttrib(screenVertexBuffer, 0, 3, GL11.GL_FLOAT, false, 0, 0);
		screen.enableVertexAttrib(screenNormalBuffer, 1, 3, GL11.GL_FLOAT, false, 0, 0);
		screen.setCount(m.getFaces().size());
		screen.setType(GL11.GL_UNSIGNED_INT);
		screen.unbind();
		screenElementBuffer.unbind();
		screenNormalBuffer.unbind();
		
		model.bind();
		model.setElementBuffer(modelElementBuffer);
		model.enableVertexAttrib(modelVertexBuffer, 0, 3, GL11.GL_FLOAT, false, 0, 0);
		model.enableVertexAttrib(modelNormalBuffer, 1, 3, GL11.GL_FLOAT, false, 0, 0);
		model.setCount(0);
		model.setType(GL11.GL_UNSIGNED_INT);
		model.unbind();
		
		programDepth = loadShader(resourceLocation + "shaders//depth");
		
		programProject = loadShader(resourceLocation + "shaders//projection");
		GL20.glUseProgram(programProject.getProgramID());
		programProject.setUniform1i("depthmap", 0);
		programProject.setUniform1i("projectionTexture", 1);
		
		programTextureProject = loadShader(resourceLocation + "shaders//simpleProject");
		GL20.glUseProgram(programTextureProject.getProgramID());
		programTextureProject.setUniform1i("projectionTexture", 1);
		//programTextureProject.setUniform1i("projectionTexture", 0);	//depthmap
		GL20.glUseProgram(0);
	}
	
	private Program loadShader(String name) throws FileNotFoundException, IOException, GLShaderCompileException, GLProgramLinkException
	{
		Shader s1 = new FragmentShader();
		Shader s2 = new VertexShader();
		s1.load(name + ".frag");
		s2.load(name + ".vert");
		s1.compile();
		s2.compile();
		Program program = new Program();
		program.attachShader(s1);
		program.attachShader(s2);
		program.link();
		program.detachShader(s1);
		program.detachShader(s2);
		s1.delete();
		s2.delete();
		return program;
	}
	
	public void cleanup()
	{
		programProject.delete();
		programDepth.delete();
		programTextureProject.delete();
		
		depthBuffer.delete();
		projectionTexture.delete();
		
		screenVertexBuffer.delete();
		screenNormalBuffer.delete();
		screenUVBuffer.delete();
		screenElementBuffer.delete();
	}
	
	public void translateViewCamera(Vector3f offset)
	{
		dirtyViewCamera = true;
		viewCamera.translate(offset);
	}
	
	public void translateProjectionCamera(Vector3f offset)
	{
		dirtyProjectionCamera = true;
		projectionCamera.translate(offset);
	}
	
	public void rotateViewCamera(Vector3f eulerAngles)
	{
		dirtyViewCamera = true;
		viewCamera.rotate(eulerAngles);
	}
	
	public void rotateProjectionCamera(Vector3f eulerAngles)
	{
		dirtyProjectionCamera = true;
		projectionCamera.rotate(eulerAngles);
	}
	
	public void setModel(VertexArrayObject model)
	{
		this.model = model;
	}
	
	public void tick()
	{
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		dirtyProjectionCamera = true;
		if(dirtyProjectionCamera)
		{
			depthBuffer.bind();
			GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
			GL20.glUseProgram(programDepth.getProgramID());
			programDepth.setUniformMatrix4f("M", false, modelTransform.viewToFloatBuffer(floatBuffer));
			programDepth.setUniformMatrix4f("V", false, projectionCamera.viewToFloatBuffer(floatBuffer));
			programDepth.setUniformMatrix4f("P", false, projectionCamera.projectionToFloatBuffer(floatBuffer));
			
			GL11.glEnable(GL11.GL_CULL_FACE);
			GL11.glCullFace(GL11.GL_FRONT);
			model.bind();
			GL11.glDrawElements(GL11.GL_TRIANGLES, model.getCount(), model.getType(), 0);
			model.unbind();
			GL11.glCullFace(GL11.GL_BACK);
			GL11.glDisable(GL11.GL_CULL_FACE);
			GL20.glUseProgram(0);
			depthBuffer.unbind();
			dirtyProjectionCamera = false;
			dirtyViewCamera = true; //need to refresh the screen because shadows change
		}
		if(dirtyViewCamera)
		{
			
			dirtyViewCamera = false;
			GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
			
			GL20.glUseProgram(programProject.getProgramID());
			
			GL13.glActiveTexture(GL13.GL_TEXTURE0);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthBuffer.getTextureID());
			GL13.glActiveTexture(GL13.GL_TEXTURE1);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, projectionTexture.getTextureID());
			GL13.glActiveTexture(GL13.GL_TEXTURE0);
			
			programProject.setUniformMatrix4f("V_camera", false, activeCamera.viewToFloatBuffer(floatBuffer));
			programProject.setUniformMatrix4f("P_camera", false, activeCamera.projectionToFloatBuffer(floatBuffer));
			programProject.setUniformMatrix4f("V_projector", false, projectionCamera.viewToFloatBuffer(floatBuffer));
			programProject.setUniformMatrix4f("P_projector", false, projectionCamera.projectionToFloatBuffer(floatBuffer));
			programProject.setUniformMatrix4f("M", false, modelTransform.viewToFloatBuffer(floatBuffer));

			model.bind();
			if(showWireframe)
			{
				GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
				GL11.glDrawElements(GL11.GL_TRIANGLES, model.getCount(), model.getType(), 0);
				GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
				GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
			}
			GL11.glDrawElements(GL11.GL_TRIANGLES, model.getCount(), model.getType(), 0);
			model.unbind();
			
			if(showScreen)
			{
				GL20.glUseProgram(programTextureProject.getProgramID());
				screen.bind();
				programTextureProject.setUniform1f("transparency", screenTransparency);
				programTextureProject.setUniformMatrix4f("M", false, screenTransform.viewToFloatBuffer(floatBuffer));
				programTextureProject.setUniformMatrix4f("V_camera", false, activeCamera.viewToFloatBuffer(floatBuffer));
				programTextureProject.setUniformMatrix4f("P_camera", false, activeCamera.projectionToFloatBuffer(floatBuffer));
				programTextureProject.setUniformMatrix4f("V_projector", false, projectionCamera.viewToFloatBuffer(floatBuffer));
				programTextureProject.setUniformMatrix4f("P_projector", false, projectionCamera.projectionToFloatBuffer(floatBuffer));
				
				GL11.glDrawElements(GL11.GL_TRIANGLES, screen.getCount(), screen.getType(), 0);
				screen.unbind();
			}
			//System.out.println();
			//System.out.println("Screen transform " + screenTransform.getPosition() + " " + screenTransform.getRotation());
			//System.out.println("Model transform " + modelTransform.getPosition() + " " + modelTransform.getRotation());
			//System.out.println("Projection camera transform " + projectionCamera.getPosition() + " " + projectionCamera.getRotation());
			//System.out.println("View camera transform " + viewCamera.getPosition() + " " + viewCamera.getRotation());
			//Utility.printGLError();
			GL20.glUseProgram(0);
		}
	}
	
	private static Texture getTexture(String filename) 
	{
		BufferedImage im=null;
		try {
			im = ImageIO.read(new File(filename));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		ByteBuffer imb = BufferUtils.createByteBuffer(im.getWidth()*im.getHeight()*3);
		imb.clear();
		for(int i=0; i < im.getHeight(); i++)
			for(int j=0; j<  im.getWidth(); j++)
			{
				imb.put((byte) ((im.getRGB(j, i)>>16)&0xff));
				imb.put((byte) ((im.getRGB(j, i)>>8)&0xff));
				imb.put((byte) ((im.getRGB(j, i))&0xff));
			}
		imb.rewind();
		
		Texture ret = new Texture(im.getWidth(), im.getHeight(), GL11.GL_RGB, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, imb);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		Utility.printGLError();
		return ret;
	}

	@Override
	public void render() {
		tick();
	}

	private boolean lPressed;
	
	@Override
	public void handleKeyboardInputPresses() {
		// TODO Auto-generated method stub
		/*if(Keyboard.isKeyDown(Keyboard.KEY_L) && lPressed == false){
			lPressed = true;
			lockProjection = !lockProjection;
		}else if(!Keyboard.isKeyDown(Keyboard.KEY_L)){
			lPressed = false;
		}*/
	}

	@Override
	public void handleKeyboardInputContinuous() {
	}

	@Override
	public void handleMouseInput(int dx, int dy, int dz, HUD hud,
			VeinsWindow veinsWindow) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setupView() {
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glClearColor(0.5f, 0.5f, 0.5f, 1);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		
		try {
			depthBuffer.delete();
			depthBuffer = Framebuffer.getDepthFramebuffer(VeinsWindow.settings.resWidth, VeinsWindow.settings.resHeight);
		} catch (GLFramebufferException e) {
			e.printStackTrace();
		}

		float ratio = (float)VeinsWindow.settings.resWidth/(float)VeinsWindow.settings.resHeight;
		((OrthoCamera)viewCamera).setOrtho(-200*ratio, 200*ratio, -200, 200, -200, 200);
		//((OrthoCamera)projectionCamera).setOrtho(-200*ratio, 200*ratio, -200, 200, -200, 200);
	}

	@Override
	public void resetView() {
		// TODO Auto-generated method stub
	}

	@Override
	public void loadShaders() throws IOException {
		
	}
	
	public void resetScene(){
		modelTransform.setPosition(new Vector3f((float)-veinsModel.centerx, (float)-veinsModel.centery, (float)-veinsModel.centerz));
		modelTransform.setRotation(new org.lwjgl.util.vector.Quaternion());
		modelTransform.setScale(new Vector3f(1, 1, 1));
		viewCamera.setRotation(Transform.quatFromEuler(new Vector3f((float)Math.PI/2.f, 0, (float)Math.PI)));
		viewCamera.setPosition(new Vector3f(0, 0, 100));
		viewCamera.setScale(new Vector3f(1, 1, 1));
		screenTransform.setRotation(Transform.quatFromEuler(new Vector3f((float)Math.PI/2.f, 0, 0)));
		screenTransform.setPosition(new Vector3f(0, 0, 0));
		screenTransform.setScale(new Vector3f(1, 1, 1));
		projectionCamera.setScale(new Vector3f(1, 1, 1));
		projectionCamera.setRotation(Transform.quatFromEuler(new Vector3f((float)Math.PI/2.f, 0, 0)));
		projectionCamera.setPosition(new Vector3f(0, 0, 100));
	}

	@Override
	public void setVeinsModel(VeinsModel veinsModel) {
		dirtyProjectionCamera = true;
		dirtyViewCamera = true;
		this.veinsModel = veinsModel;
		Mesh m = veinsModel.getMeshes().get(0);
		FloatBuffer tmpFloatBuffer = Tools.arrayListToBuffer(m.getVertices(), null);
		modelVertexBuffer.setData(tmpFloatBuffer);
		tmpFloatBuffer.clear();
		tmpFloatBuffer.put(Mesh.getNormals(m.getVertices(), m.getFaces()));
		tmpFloatBuffer.rewind();
		modelNormalBuffer.setData(tmpFloatBuffer);
		IntBuffer tmpIBuffer = BufferUtils.createIntBuffer(m.getFaces().size());
		m.getFaces().forEach((Integer i)->{tmpIBuffer.put(i-1);});
		tmpIBuffer.rewind();
		
		modelElementBuffer.setData(tmpIBuffer);
		modelElementBuffer.unbind();
		modelNormalBuffer.unbind();
		System.out.println();
		model.bind();
		model.setElementBuffer(modelElementBuffer);
		model.enableVertexAttrib(modelVertexBuffer, 0, 3, GL11.GL_FLOAT, false, 0, 0);
		model.enableVertexAttrib(modelNormalBuffer, 1, 3, GL11.GL_FLOAT, false, 0, 0);
		model.unbind();
		model.setCount(m.getFaces().size());
		model.setType(GL11.GL_UNSIGNED_INT);

		modelTransform.setPosition(new Vector3f((float)-veinsModel.centerx, (float)-veinsModel.centery, (float)-veinsModel.centerz));
		//projectionCamera.translate(projectionCamera.getPosition().negate(null));
		//projectionCamera.translate(new Vector3f((float)veinsModel.centerx, (float)veinsModel.centery, (float)veinsModel.centerz));
		//modelTransform.rotate(new Vector3f((float)Math.PI/2f, 0, 0));
		
		viewCamera.setRotation(Transform.quatFromEuler(new Vector3f((float)Math.PI/2.f, 0, (float)Math.PI)));
		screenTransform.setRotation(Transform.quatFromEuler(new Vector3f((float)Math.PI/2.f, 0, 0)));
		projectionCamera.setRotation(Transform.quatFromEuler(new Vector3f((float)Math.PI/2.f, 0, 0)));
		//rotateViewCamera(Transform.eulerFromQuat(viewCamera.getRotation()));
		//rotateViewCamera(new Vector3f((float)-Math.PI/2f, 0, 0));
		//rotateProjectionCamera(Transform.eulerFromQuat(projectionCamera.getRotation()));
		
		Utility.printGLError();
		System.out.println("Model has " + m.getFaces().size() + " faces");
	}
	
	void loadProjectionTexture(String filename){
		Utility.printGLError();
		if(projectionTexture != null)
		{
			System.out.println("Deleting tex " + projectionTexture.getTextureID());
			projectionTexture.delete();
		}
		projectionTexture = XRayProjectionModule.getTexture(filename);
		System.out.println("New texture! " + projectionTexture.getTextureID() + " " + filename);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, projectionTexture.getTextureID());
		float width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
		float height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
		float ratio = width/height;
		((OrthoCamera)projectionCamera).setOrtho(-200*ratio, 200*ratio, -200, 200, -200, 200);
		
		Utility.printGLError();
	}
}
