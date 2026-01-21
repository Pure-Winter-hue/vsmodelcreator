package at.vintagestory.modelcreator;

import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glViewport;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;

import at.vintagestory.modelcreator.gui.right.ElementTreeCellRenderer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import at.vintagestory.modelcreator.gui.GuiMenu;
import at.vintagestory.modelcreator.gui.Icons;
import at.vintagestory.modelcreator.gui.left.LeftKeyFramesPanel;
import at.vintagestory.modelcreator.gui.left.LeftSidebar;
import at.vintagestory.modelcreator.gui.left.LeftUVSidebar;
import at.vintagestory.modelcreator.gui.middle.ModelRenderer;
import at.vintagestory.modelcreator.gui.right.RightPanel;
import at.vintagestory.modelcreator.gui.right.face.FaceTexturePanel;
import at.vintagestory.modelcreator.input.InputManager;
import at.vintagestory.modelcreator.input.command.FactoryProjectCommand;
import at.vintagestory.modelcreator.input.command.CommandSelectAll;
import at.vintagestory.modelcreator.input.command.ProjectCommand;
import at.vintagestory.modelcreator.input.key.InputKeyEvent;
import at.vintagestory.modelcreator.input.listener.ListenerKeyPressInterval;
import at.vintagestory.modelcreator.input.listener.ListenerKeyPressOnce;
import at.vintagestory.modelcreator.interfaces.IElementManager;
import at.vintagestory.modelcreator.interfaces.ITextureCallback;
import at.vintagestory.modelcreator.model.Animation;
import at.vintagestory.modelcreator.model.AnimationFrame;
import at.vintagestory.modelcreator.model.AnimFrameElement;
import at.vintagestory.modelcreator.model.AttachmentPoint;
import at.vintagestory.modelcreator.model.Face;
import at.vintagestory.modelcreator.model.Element;
import at.vintagestory.modelcreator.model.PendingTexture;
import at.vintagestory.modelcreator.model.TextureEntry;
import at.vintagestory.modelcreator.util.GameMath;
import at.vintagestory.modelcreator.util.Mat4f;
import at.vintagestory.modelcreator.util.screenshot.AnimationCapture;
import at.vintagestory.modelcreator.util.screenshot.PendingScreenshot;
import at.vintagestory.modelcreator.util.screenshot.ScreenshotCapture;

import java.util.prefs.Preferences;

public class ModelCreator extends JFrame implements ITextureCallback
{
	public static String windowTitle = "Vintage Story Model Creator"; 
	private static final long serialVersionUID = 1L;
	
	public static ModelCreator Instance;
	public static Preferences prefs;
	public static Project currentProject;
	public static Project currentBackdropProject;
	public static Project currentMountBackdropProject;
	public static ProjectChangeHistory changeHistory = new ProjectChangeHistory();	
	public static int ignoreDidModify = 0;	
	
	
	public static boolean saveDisabledFaces = false;	
	public static boolean ignoreValueUpdates = false;
	public static boolean ignoreFrameUpdates = false;
	public static boolean showGrid = true;
	public static boolean showTreadmill = false;
	public static boolean showShade = true;
	public static boolean transparent = true;
	public static boolean renderTexture = true;
	public static boolean autoreloadTexture = true;
	public static boolean repositionWhenReparented = true;
	public static boolean darkMode = false;
	public static boolean saratyMode = false;
	public static boolean uvShowNames = false;
	public static boolean backdropAnimationsMode = true;
	public static int elementTreeHeight = 240;
	public static float TreadMillSpeed = 1f;
	public static float noTexScale = 2;
	public static int currentRightTab;
	// Thickness (px) of the viewport translate gizmo axis lines (persisted)
	public static int viewportAxisLineThickness = 10;
	// Length (px) of the viewport translate gizmo axis lines (persisted)
	public static int viewportAxisLineLength = 110;
	// When true, the translate gizmo uses camera-relative movement (legacy behavior). When false, it uses world axes.
	public static boolean viewportFreedomModeEnabled = false;

	// Whether the viewport translate gizmo center circle handle is enabled (persisted)
	public static boolean viewportCenterCircleEnabled = true;
	// Radius (px) of the viewport translate gizmo center circle (persisted)
	public static int viewportCenterCircleRadius = 18;

	// Canvas Variables
	private final static AtomicReference<Dimension> newCanvasSize = new AtomicReference<Dimension>();
	public static Canvas canvas;
	public int canvWidth = 990, canvHeight = 700;

	// Swing Components
	public JScrollPane scroll;
	public static RightPanel rightTopPanel;
	private Element grabbedElem = null;

	// Texture Loading Cache
	List<PendingTexture> pendingTextures = Collections.synchronizedList(new ArrayList<PendingTexture>());
	private PendingScreenshot screenshot = null;
	public static AnimationCapture animCapture = null;
	
	
	

	private int lastMouseX, lastMouseY;
	// Mouse click tracking (for click-to-select without requiring CTRL)
	private int mouseDownX, mouseDownY;
	private int mouseDownButton = -1;
	private boolean mouseDownWithCtrl;
	private boolean mouseDownWithShift;
	// Viewport marquee selection (Shift + drag)
	private boolean viewportMarqueeActive;
	private int marqueeStartX, marqueeStartY;
	private int marqueeCurX, marqueeCurY;

	boolean mouseDownOnLeftPanel;
	boolean mouseDownOnCenterPanel;
	boolean mouseDownOnRightPanel;
	
	private boolean grabbing = false;
	private boolean closeRequested = false;
	

	
	public LeftSidebar uvSidebar;
	public LeftSidebar viewportSidebar;
	public static GuiMenu guiMain;
	public static LeftKeyFramesPanel leftKeyframesPanel;
	
	public static javax.swing.JScrollPane leftKeyframesScroll;
public static boolean renderAttachmentPoints;

	
	public ModelRenderer modelrenderer;
	// RightPanel may call setSidebar() during UI initialization before modelrenderer exists.
	private LeftSidebar pendingLeftSidebar;
	
	public long prevFrameMillisec;
	
	
	public static double WindWaveCounter;
	public static int WindPreview;
	
	static {
		prefs = Preferences.userRoot().node("ModelCreator");
	}
	
	// Input listener class
	private InputManager manager = new InputManager();
	
	
	public static Project GetProject(String type) {
		if (type == "backdrop") return currentBackdropProject;
		if (type == "mountbackdrop") return currentMountBackdropProject;
		return currentProject;
	}
	
	public ModelCreator(String title, String[] args) throws LWJGLException
	{
		super(title);
		
		EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
		queue.push(new EventQueueProxy());
		
		showGrid = prefs.getBoolean("showGrid", true);
		saratyMode = prefs.getBoolean("uvRotateRename", true);

		saveDisabledFaces = prefs.getBoolean("saveDisabledFaces", true);
		uvShowNames = prefs.getBoolean("uvShowNames", true);
		darkMode = prefs.getBoolean("darkMode", false);
		noTexScale = prefs.getFloat("noTexScale", 2);
		autoreloadTexture = prefs.getBoolean("autoreloadTexture", true);
		repositionWhenReparented = prefs.getBoolean("repositionWhenReparented", true);
		elementTreeHeight = prefs.getInt("elementTreeHeight", 240);
		viewportAxisLineThickness = prefs.getInt("viewportAxisLineThickness", 10);
		viewportAxisLineLength = prefs.getInt("viewportAxisLineLength", 110);
		viewportFreedomModeEnabled = prefs.getBoolean("viewportFreedomModeEnabled", false);
		viewportAxisLineThickness = Math.max(2, Math.min(40, viewportAxisLineThickness));
		viewportAxisLineLength = Math.max(40, Math.min(260, viewportAxisLineLength));

		viewportCenterCircleEnabled = prefs.getBoolean("viewportCenterCircleEnabled", true);
		viewportCenterCircleRadius = prefs.getInt("viewportCenterCircleRadius", 18);
		viewportCenterCircleRadius = Math.max(6, Math.min(60, viewportCenterCircleRadius));
		
		Instance = this;
		
		currentProject = new Project(null);
		changeHistory.addHistoryState(currentProject);
		
		setDropTarget(getCustomDropTarget());
		setPreferredSize(new Dimension(1200, 780));
		setMinimumSize(new Dimension(800, 500));
		setLayout(new BorderLayout(10, 0));
		setIconImages(getIcons());
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		String loadFile = null;
		for (int i = 0; i < args.length; i++) {
			if (Objects.equals(args[i], "-t") && args.length > i + 1) {
				ModelCreator.prefs.put("texturePath", args[i + 1]);
				i++;
			} else if (Objects.equals(args[i], "-s") && args.length > i + 1) {
				ModelCreator.prefs.put("shapePath", args[i + 1]);
				i++;
			} else if (Objects.equals(args[i], "-f") && args.length > i + 1) {
				loadFile = args[i + 1];
				i++;
			}
		}

		String colorPath = prefs.get("colorPath", null);
		if (colorPath != null) {
			LoadColorConfig(colorPath);
		}

		initComponents();
		

		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				if (currentProject.needsSaving) {
					int	returnVal = JOptionPane.showConfirmDialog(null, "You have not saved your changes yet, would you like to save now?", "Warning", JOptionPane.YES_NO_CANCEL_OPTION);
					
					if (returnVal == JOptionPane.YES_OPTION) {
						if (ModelCreator.currentProject.filePath == null) {
							SaveProjectAs();
						} else {
							SaveProject(new File(ModelCreator.currentProject.filePath));	
						}
						
					}
					
					if (returnVal == JOptionPane.CANCEL_OPTION || returnVal == JOptionPane.CLOSED_OPTION) {
						return;
					}

				}
				
				
				
				closeRequested = true;
			}
		});
		
		// Commands
		FactoryProjectCommand factoryCommand = new FactoryProjectCommand();
		ProjectCommand undo = factoryCommand.CreateUndoCommand();
		ProjectCommand redo = factoryCommand.CreateRedoCommand();
		ProjectCommand save = factoryCommand.CreateSaveCommand(this);
		
		ProjectCommand texReload = factoryCommand.CreateReloadTextureCommand();
		ProjectCommand texToggle = factoryCommand.CreateToggleTextureCommand();
		ProjectCommand texRandom = factoryCommand.CreateRandomizeTextureCommand();
		ProjectCommand selectAll = new CommandSelectAll();
		
		ProjectCommand elementUp = factoryCommand.CreateMoveSelectedElementCommandUp(this);
		ProjectCommand elementForward = factoryCommand.CreateMoveSelectedElementCommandForward(this);
		ProjectCommand elementRight = factoryCommand.CreateMoveSelectedElementCommandRight(this);
		ProjectCommand elementBackward = factoryCommand.CreateMoveSelectedElementCommandBackward(this);
		ProjectCommand elementLeft = factoryCommand.CreateMoveSelectedElementCommandLeft(this);
		ProjectCommand elementDown = factoryCommand.CreateMoveSelectedElementCommandDown(this);

		// Add key input listeners
		manager.subscribe(new ListenerKeyPressOnce(undo, Keyboard.KEY_LCONTROL, Keyboard.KEY_Z));
		manager.subscribe(new ListenerKeyPressOnce(redo, Keyboard.KEY_LCONTROL, Keyboard.KEY_Y));
		manager.subscribe(new ListenerKeyPressOnce(save, Keyboard.KEY_LCONTROL, Keyboard.KEY_S));
		manager.subscribe(new ListenerKeyPressOnce(texReload, Keyboard.KEY_LCONTROL, Keyboard.KEY_R));
		manager.subscribe(new ListenerKeyPressOnce(texToggle, Keyboard.KEY_LCONTROL, Keyboard.KEY_T));
		manager.subscribe(new ListenerKeyPressOnce(texRandom, Keyboard.KEY_LCONTROL, Keyboard.KEY_B));
		// Plain 'A' should select all in Cube (modeling) and Keyframe (animation) modes
		manager.subscribe(new ListenerKeyPressOnce(selectAll, Keyboard.KEY_A));
		
		manager.subscribe(new ListenerKeyPressInterval(elementUp, Keyboard.KEY_PRIOR));
		manager.subscribe(new ListenerKeyPressInterval(elementForward, Keyboard.KEY_UP));
		manager.subscribe(new ListenerKeyPressInterval(elementRight, Keyboard.KEY_RIGHT));
		manager.subscribe(new ListenerKeyPressInterval(elementBackward, Keyboard.KEY_DOWN));
		manager.subscribe(new ListenerKeyPressInterval(elementLeft, Keyboard.KEY_LEFT));
		manager.subscribe(new ListenerKeyPressInterval(elementDown, Keyboard.KEY_NEXT));
		
		// Enable repeat events, grants more fine-grained control over keyboard input
		Keyboard.enableRepeatEvents(true);
		
		// Seriously man, fuck java. Mouse listeners on a canvas are just plain not working. 
		// canvas.addMouseListener(ml);
		
		
		pack();
		setVisible(true);
		setLocationRelativeTo(null);

		initDisplay();
		
		
		currentProject.LoadIntoEditor(getElementManager());
		updateValues(null);

		prevFrameMillisec = System.currentTimeMillis();

		if (loadFile != null) {
			LoadFile(loadFile);
		}

		try
		{
			Display.create();

			loop();

			Display.destroy();
			//dispose();
			System.exit(0);
		}
		catch (Exception e1)
		{
			e1.printStackTrace();
			JOptionPane.showMessageDialog(
				null, 
				"Main loop crashed, please make a screenshot of this message and report it, program will exit now. Sorry about that :(\nException: " + e1 + "\n" + stackTraceToString(e1), 
				"Crash!", 
				JOptionPane.ERROR_MESSAGE, 
				null
			);
			System.exit(0);
		}
	}
	
	public static Project CurrentAnimProject() {
		return ModelCreator.backdropAnimationsMode && ModelCreator.currentBackdropProject != null ? ModelCreator.currentBackdropProject : ModelCreator.currentProject;
	}
	
	public static boolean AnimationPlaying() {
		return currentProject != null && currentProject.PlayAnimation; 
	}
	
	public static String stackTraceToString(Throwable e) {
	    StringBuilder sb = new StringBuilder();
	    for (StackTraceElement element : e.getStackTrace()) {
	        sb.append(element.toString());
	        sb.append("\n");
	    }
	    return sb.toString();
	}
	

	public static void DidModify() {
		if (ignoreDidModify > 0) return;
		if (currentProject == null) return;
		
		currentProject.needsSaving = true;
		
		changeHistory.addHistoryState(currentProject);
		
		updateTitle();
	}

	
	public void initComponents()
	{
		Icons.init(getClass());
		
		rightTopPanel = new RightPanel(this);

		leftKeyframesPanel = new LeftKeyFramesPanel(rightTopPanel);
		leftKeyframesScroll = new JScrollPane(leftKeyframesPanel);
		leftKeyframesScroll.setBorder(BorderFactory.createEmptyBorder());
		leftKeyframesScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		leftKeyframesScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		leftKeyframesScroll.getVerticalScrollBar().setUnitIncrement(16);
		// Put the scrollbar on the left, but keep the panel's contents LTR
		leftKeyframesScroll.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
		leftKeyframesScroll.getViewport().setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

		leftKeyframesScroll.setWheelScrollingEnabled(true);

		// Make mouse wheel scrolling work anywhere over the left panel (not only when hovering the scrollbar).
		Toolkit.getDefaultToolkit().addAWTEventListener(new java.awt.event.AWTEventListener() {
			@Override
			public void eventDispatched(AWTEvent event) {
				if (!(event instanceof java.awt.event.MouseWheelEvent)) return;
				if (!leftKeyframesScroll.isShowing()) return;

				java.awt.event.MouseWheelEvent mwe = (java.awt.event.MouseWheelEvent) event;
				Component src = mwe.getComponent();
				if (src == null) return;

				// Only handle wheel events originating from the left panel subtree
				if (!SwingUtilities.isDescendingFrom(src, leftKeyframesScroll)) return;

				// Let nested scroll panes (e.g. keyframe table) handle their own wheel scrolling.
				JScrollPane nested = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, src);
				if (nested != null && nested != leftKeyframesScroll) return;

				// If the wheel is over the scrollbar itself, let the scrollbar handle it.
				if (SwingUtilities.isDescendingFrom(src, leftKeyframesScroll.getVerticalScrollBar())) return;

				javax.swing.JScrollBar bar = leftKeyframesScroll.getVerticalScrollBar();
				int units = mwe.getUnitsToScroll();
				int delta = units * bar.getUnitIncrement(units);
				bar.setValue(bar.getValue() + delta);
				mwe.consume();
			}
		}, AWTEvent.MOUSE_WHEEL_EVENT_MASK);

		leftKeyframesPanel.setVisible(false);
		leftKeyframesScroll.setVisible(false);
		add(leftKeyframesScroll, BorderLayout.WEST);
		
		// Canvas stuff
		canvas = new Canvas();
		canvas.setFocusable(true);
		canvas.setVisible(true);
		canvas.requestFocus();
		
		canvas.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				newCanvasSize.set(canvas.getSize());
			}
		});
		
		//== Canvas trickery for larger uiScales ==//
		// kinda messy, but works
		final double viewScale = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getDefaultTransform().getScaleX();
		
		// Create a container for our canvas (a simple JPanel) without a layout
		// so that our canvas would not get affected by the base component's layout.
		JPanel panel = new JPanel(null);
		panel.add(canvas);
		add(panel, BorderLayout.CENTER);
		
		// Inherit size from JPanel, apply the ui scale factor
		panel.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				canvas.setSize((int)(panel.getWidth()*viewScale), (int)(panel.getHeight()*viewScale));
			}
		});
		
		//== end Canvas trickery ==//
		
		modelrenderer = new ModelRenderer(rightTopPanel);
		// Apply any sidebar requested during earlier UI initialization.
		if (pendingLeftSidebar != null) {
			LeftSidebar s = pendingLeftSidebar;
			pendingLeftSidebar = null;
			setSidebar(s);
		}
		
		scroll = new JScrollPane((JPanel) rightTopPanel);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scroll.getVerticalScrollBar().setUnitIncrement(16);

		add(scroll, BorderLayout.EAST);
		
		uvSidebar = new LeftUVSidebar("UV Editor", rightTopPanel);
		viewportSidebar = new at.vintagestory.modelcreator.gui.left.LeftViewportSidebar();

		// If no sidebar has been selected yet (startup), default to the viewport sidebar.
		if (modelrenderer.renderedLeftSidebar == null) {
			setSidebar(viewportSidebar);
		}
		
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		setJMenuBar(guiMain = new GuiMenu(this));
	}

	private List<Image> getIcons()
	{
		List<Image> icons = new ArrayList<Image>();
		icons.add(Toolkit.getDefaultToolkit().getImage("assets/appicon_16x.png"));
		icons.add(Toolkit.getDefaultToolkit().getImage("assets/appicon_32x.png"));
		icons.add(Toolkit.getDefaultToolkit().getImage("assets/appicon_64x.png"));
		icons.add(Toolkit.getDefaultToolkit().getImage("assets/appicon_128x.png"));
		return icons;
	}

	
	
	public static void updateValues(JComponent byGuiElem)
	{
		if (currentProject == null) return;
		if (ignoreValueUpdates) return;
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() { 
				ignoreValueUpdates = true;
				
				if (currentProject.SelectedAnimation != null) {
					currentProject.SelectedAnimation.SetFramesDirty();
				}

				if (currentBackdropProject != null && currentBackdropProject.SelectedAnimation != null) {
					currentBackdropProject.SelectedAnimation.SetFramesDirty();
				}
				
				guiMain.updateValues(byGuiElem);
			 	((RightPanel)rightTopPanel).updateValues(byGuiElem);
			 	leftKeyframesPanel.updateValues(byGuiElem);
			 	updateFrame(false);
			 	updateTitle();
			 	
			 	ignoreValueUpdates = false;
			}
		});
	}
	
	static void updateTitle() {
	 	String dash = currentProject.needsSaving ? " * " : " - ";
	 	if (currentProject.filePath == null) {
	 		Instance.setTitle("(untitled)" + dash + windowTitle);
		} else {
			Instance.setTitle(new File(currentProject.filePath).getName() + dash + windowTitle);
		}		
	}
	
	public static void updateFrame() {
		if (backdropAnimationsMode && currentBackdropProject != null && currentProject.SelectedAnimation != null && currentBackdropProject.SelectedAnimation != null) {
			currentProject.SelectedAnimation.currentFrame = currentBackdropProject.SelectedAnimation.currentFrame;
		}
		
		updateFrame(true);
	}
	
	public static void updateFrame(boolean later) {
		if (currentProject == null) return;
		if (ignoreFrameUpdates) return;
		
		if (later) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() { 
					ignoreFrameUpdates = true;
					
					leftKeyframesPanel.updateFrame();
					((RightPanel)rightTopPanel).updateFrame(null);
					updateTitle();
					guiMain.updateFrame();
					
					ignoreFrameUpdates = false;
				}
			});
		} else {
			ignoreFrameUpdates = true;
			
			leftKeyframesPanel.updateFrame();
			((RightPanel)rightTopPanel).updateFrame(null);
			updateTitle();
			guiMain.updateFrame();
			
			ignoreFrameUpdates = false;
		}
	}


	
	public void AddPendingTexture(PendingTexture texture) {
		synchronized (pendingTextures)
		{
			pendingTextures.add(texture);
		}
	}
	
	public void initDisplay() throws LWJGLException
	{
		Display.setParent(canvas);
		Display.setVSyncEnabled(true);
		Display.setInitialBackground(0.92F, 0.92F, 0.93F);
	}

	
	ArrayList<PendingTexture> notLoadedPendingTexs = new ArrayList<PendingTexture>();
	int frameCounter;
	
	private void loop() throws Exception
	{
		modelrenderer.camera = new Camera(60F, (float) Display.getWidth() / (float) Display.getHeight(), 0.3F, 1000F);		

		Dimension newDim;
		
		while (!Display.isCloseRequested() && !getCloseRequested())
		{
			Project project = ModelCreator.currentProject;
			
			if (project == null) {
				Thread.sleep(5);
				continue;
			}
			
			frameCounter++;
			
			WindWaveCounter = (frameCounter / 60.0) % 2000;
			
			synchronized (pendingTextures)
			{
				for (PendingTexture texture : pendingTextures)
				{
					if (texture.LoadDelay > 0) {
						texture.LoadDelay--;
						notLoadedPendingTexs.add(texture);
						continue;
					}
					
					texture.load();
				}
				
				pendingTextures.clear();	
				pendingTextures.addAll(notLoadedPendingTexs);
				notLoadedPendingTexs.clear();
			}
			
			
			if (project.SelectedAnimation != null && project.SelectedAnimation.framesDirty) {
				project.SelectedAnimation.calculateAllFrames(project);
			}

			Project bdp = currentBackdropProject;
			if (bdp != null && bdp.SelectedAnimation != null && bdp.SelectedAnimation.framesDirty) {
				bdp.SelectedAnimation.calculateAllFrames(bdp);
			}
			
			Project mountbdp = currentMountBackdropProject;
			if (mountbdp != null && mountbdp.SelectedAnimation != null && mountbdp.SelectedAnimation.framesDirty) {
				mountbdp.SelectedAnimation.calculateAllFrames(mountbdp);
			}

			
			
			newDim = newCanvasSize.getAndSet(null);

			if (newDim != null)
			{
				canvWidth = newDim.width;
				canvHeight = newDim.height;
			}

			// glViewPort view must not go negative 
			int leftSidebarWidth = leftSidebarWidth();
			if (canvWidth - leftSidebarWidth < 0)  {
				if (modelrenderer.renderedLeftSidebar != null) {
				 	modelrenderer.renderedLeftSidebar.nowSidebarWidth = canvWidth - 10;
				 	leftSidebarWidth = leftSidebarWidth();
				}
			}
			
			glViewport(leftSidebarWidth, 0, canvWidth - leftSidebarWidth, canvHeight);
			
			handleInputKeyboard();
			handleInputMouse(leftSidebarWidth);
			
			
			if (animCapture != null && !animCapture.isComplete()) {
				animCapture.PrepareFrame();
			}
			
			
			if (ModelCreator.transparent) {
				GL11.glEnable(GL11.GL_BLEND);
				glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			}

			modelrenderer.Render(leftSidebarWidth, canvWidth, canvHeight, getHeight());
			rightTopPanel.Draw();

			if (ModelCreator.transparent) {
				GL11.glDisable(GL11.GL_BLEND);
			}

			Display.update();

			if (screenshot != null)
			{
				if (screenshot.getFile() != null)
					ScreenshotCapture.getScreenshot(canvWidth, canvHeight, screenshot.getCallback(), screenshot.getFile());
				else
					ScreenshotCapture.getScreenshot(canvWidth, canvHeight, screenshot.getCallback());
				screenshot = null;
			}
			
			
			if (animCapture != null && !animCapture.isComplete()) {
				animCapture.CaptureFrame(canvWidth, canvHeight);
				
				if (animCapture.isComplete()) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run()
						{
							JOptionPane.showMessageDialog(null, "Animation export complete");							
						}
					});
					
					animCapture = null;
				}
			} else {
				
				if (project != null && project.SelectedAnimation != null && project.PlayAnimation) {
					if (frameCounter % 2 == 0) {
						project.SelectedAnimation.NextFrame();
						updateFrame(true);
					}
				}
				
				
				if (bdp != null && bdp.SelectedAnimation != null && bdp.PlayAnimation) {
					if (frameCounter % 2 == 0) {
						bdp.SelectedAnimation.NextFrame();
						updateFrame(true);
					}
				}

				if (mountbdp != null && mountbdp.SelectedAnimation != null && project.PlayAnimation) {
					if (frameCounter % 2 == 0) {
						mountbdp.SelectedAnimation.NextFrame();
					}
				}


				// Don't run faster than ~60 FPS (1000 / 60 = 16.67ms)
				long duration = System.currentTimeMillis() - prevFrameMillisec; 
				Thread.sleep(Math.max(16 - duration, 0));
				prevFrameMillisec = System.currentTimeMillis();
				
			}
			
			
		}
	}
	
	public int leftSidebarWidth() {
		int leftSpacing = 0;
		if (modelrenderer.renderedLeftSidebar != null) {
		 	leftSpacing = modelrenderer.renderedLeftSidebar.GetSidebarWidth();
			
		}
		
		return leftSpacing;
	}

	
	
	public void handleInputKeyboard()
	{
		// Poll the keyboard for input
		Keyboard.poll();
		
		// Process keypresses
		while (Keyboard.next()){
			
			// Read all events from keyboard
			int keyCode = Keyboard.getEventKey();
			char keyChar = Keyboard.getEventCharacter();
			boolean pressed = Keyboard.getEventKeyState();
			boolean down = Keyboard.isRepeatEvent();
			long nano = Keyboard.getEventNanoseconds();
			
			// Create new KeyEvent
			InputKeyEvent event = new InputKeyEvent(keyCode, keyChar, pressed, down, nano);
			
			// Notify all key listeners
			manager.notifyListeners(event);
			
			// To retain compatibility with existing code
			if(event.keyCode() == Keyboard.KEY_LCONTROL)
				leftControl = event.pressed();
		}
	}
	
	public boolean leftControl;
	public boolean isOnRightPanel;

	/**
	 * When enabled, viewport transforms (Ctrl+drag) will apply to all currently selected elements
	 * instead of only the active element.
	 */
	public static boolean viewportGroupMoveEnabled = false;
	/**
	 * When enabled, viewport translate operations will not allow moving any selected element
	 * below its Y value at the start of the current drag.
	 */
	public static boolean viewportFloorSnapEnabled = false;

	// Viewport gizmo drag state
	private boolean viewportGizmoDragActive;
	private int viewportGizmoDragMode; // 0 none, 1 X, 2 Y, 3 Z, 4 FREE
	private double viewportGizmoAxisDirX;
	private double viewportGizmoAxisDirY;
	// For world-axis mode, use a ray-to-axis constraint for precise axis movement (prevents drift).
	private boolean viewportGizmoUseRayConstraint;
	private double viewportGizmoAxisOriginX, viewportGizmoAxisOriginY, viewportGizmoAxisOriginZ;
	private double viewportGizmoAxisWorldDirX, viewportGizmoAxisWorldDirY, viewportGizmoAxisWorldDirZ;
	private double viewportGizmoAxisParamStart;
	/**
	 * Cached mapping from world-axis delta (X/Y/Z) to the element's local StartX/StartY/StartZ delta.
	 * This prevents axis-drag drift when elements (or their parents) are rotated.
	 *
	 * Layout per element: [wx->(dx,dy,dz), wy->(dx,dy,dz), wz->(dx,dy,dz)] as 9 doubles.
	 */
	private java.util.HashMap<Element, double[]> viewportGizmoWorldAxisToLocal;
	private java.util.HashMap<Element, Double> viewportGizmoBaseX;
	private java.util.HashMap<Element, Double> viewportGizmoBaseZ;
	private double viewportGizmoFreeRightX, viewportGizmoFreeRightY, viewportGizmoFreeRightZ;
	private double viewportGizmoFreeUpX, viewportGizmoFreeUpY, viewportGizmoFreeUpZ;
	private java.util.List<Element> viewportGizmoElems;
	private java.util.HashMap<Element, Double> viewportGizmoBaseY;
	private boolean viewportGizmoSkipSelection;
	// Keyframe (animation tab) gizmo drag support
	private boolean viewportGizmoDragInKeyframeMode;
	private int viewportGizmoDragAnimVersion;
	private java.util.HashMap<Element, AnimFrameElement> viewportGizmoKeyframeElems;

		private java.util.List<Element> getSelectedRootElementsForMove(Element lead, boolean moveAll)
		{
			if (!moveAll || currentProject == null || currentProject.SelectedElements == null || currentProject.SelectedElements.size() <= 1) {
				return java.util.Collections.singletonList(lead);
			}

			java.util.HashSet<Element> selected = new java.util.HashSet<Element>(currentProject.SelectedElements);
			java.util.ArrayList<Element> roots = new java.util.ArrayList<Element>();
			for (Element e : currentProject.SelectedElements) {
				if (e == null) continue;
				boolean hasSelectedAncestor = false;
				Element p = e.ParentElement;
				while (p != null) {
					if (selected.contains(p)) { hasSelectedAncestor = true; break; }
					p = p.ParentElement;
				}
				if (!hasSelectedAncestor) roots.add(e);
			}
			if (roots.isEmpty()) {
				return java.util.Collections.singletonList(lead);
			}
			return roots;
		}

		private void tryStartViewportGizmoDrag()
		{
			if (viewportGizmoDragActive || currentProject == null) return;
			Element selected = currentProject.SelectedElement;
			if (selected == null) return;

			// Screen-space hit test against the overlay gizmo (cannot be occluded by geometry)
			int mode = modelrenderer.hitTestViewportMoveGizmo(mouseDownX, mouseDownY);
			if (mode == 0) return;
			// The center handle is only meaningful in freedom mode.
			if (mode == 4 && !viewportFreedomModeEnabled) return;


			viewportGizmoDragMode = mode;
			viewportGizmoDragActive = true;
			viewportGizmoSkipSelection = true;
			grabbedElem = selected;

			viewportGizmoUseRayConstraint = false;
			viewportGizmoAxisParamStart = 0;
			viewportGizmoBaseX = null;
			viewportGizmoBaseZ = null;

			// Screen-space axis direction used to interpret mouse delta.
			// Mouse coordinates are window-space with origin bottom-left.
			double[] dir = modelrenderer.getViewportMoveGizmoAxisDirWindow(mode);
			viewportGizmoAxisDirX = dir != null ? dir[0] : 0;
			viewportGizmoAxisDirY = dir != null ? dir[1] : 0;

			viewportGizmoElems = getSelectedRootElementsForMove(selected, viewportGroupMoveEnabled);

			// In Keyframe tab, viewport translate should animate (write keyframe position offsets), not edit the base model.
			viewportGizmoDragInKeyframeMode = (ModelCreator.currentRightTab == 2 && currentProject != null && currentProject.SelectedAnimation != null);
			viewportGizmoDragAnimVersion = viewportGizmoDragInKeyframeMode ? currentProject.CurrentAnimVersion() : 0;
			viewportGizmoKeyframeElems = viewportGizmoDragInKeyframeMode ? new java.util.HashMap<Element, AnimFrameElement>() : null;

			AnimationFrame curAnimFrame = null;
			if (viewportGizmoDragInKeyframeMode && currentProject.SelectedAnimation != null) {
				int cf = currentProject.SelectedAnimation.currentFrame;
				if (currentProject.SelectedAnimation.allFrames != null && cf >= 0 && cf < currentProject.SelectedAnimation.allFrames.size()) {
					curAnimFrame = currentProject.SelectedAnimation.allFrames.get(cf);
				}
			}

			viewportGizmoBaseY = new java.util.HashMap<Element, Double>();
			viewportGizmoBaseX = new java.util.HashMap<Element, Double>();
			viewportGizmoBaseZ = new java.util.HashMap<Element, Double>();
			viewportGizmoWorldAxisToLocal = new java.util.HashMap<Element, double[]>();

			for (Element e : viewportGizmoElems) {
				if (e == null) continue;

				if (viewportGizmoDragInKeyframeMode && currentProject.SelectedAnimation != null) {
					// Ensure a keyframe element exists and that Position is enabled for this element.
					AnimFrameElement kf = currentProject.SelectedAnimation.TogglePosition(e, true);
					if (kf != null) {
						viewportGizmoKeyframeElems.put(e, kf);
						viewportGizmoBaseY.put(e, kf.getOffsetY());
						viewportGizmoBaseX.put(e, kf.getOffsetX());
						viewportGizmoBaseZ.put(e, kf.getOffsetZ());
					} else {
						viewportGizmoBaseY.put(e, 0d);
						viewportGizmoBaseX.put(e, 0d);
						viewportGizmoBaseZ.put(e, 0d);
					}
					// Keep world-axis drags straight even with animated rotations in the parent chain.
					viewportGizmoWorldAxisToLocal.put(e, computeWorldAxisToLocalMapAnimated(e, curAnimFrame, viewportGizmoDragAnimVersion));
				} else {
					viewportGizmoBaseY.put(e, e.getStartY());
					viewportGizmoBaseX.put(e, e.getStartX());
					viewportGizmoBaseZ.put(e, e.getStartZ());
					// Only needed for world-axis mode (non-freedom) to keep motion straight in world space,
					// even when elements/parents are rotated.
					viewportGizmoWorldAxisToLocal.put(e, computeWorldAxisToLocalMap(e));
				}
			}
			// In world-axis mode, use a precise ray-to-axis constraint so dragging stays locked to the axis.
			if (!viewportFreedomModeEnabled && (mode == 1 || mode == 2 || mode == 3)) {
				viewportGizmoAxisOriginX = modelrenderer.viewportMoveGizmoWorldX;
				viewportGizmoAxisOriginY = modelrenderer.viewportMoveGizmoWorldY;
				viewportGizmoAxisOriginZ = modelrenderer.viewportMoveGizmoWorldZ;
				switch (mode) {
					case 1: viewportGizmoAxisWorldDirX = 1; viewportGizmoAxisWorldDirY = 0; viewportGizmoAxisWorldDirZ = 0; break;
					case 2: viewportGizmoAxisWorldDirX = 0; viewportGizmoAxisWorldDirY = 1; viewportGizmoAxisWorldDirZ = 0; break;
					case 3: viewportGizmoAxisWorldDirX = 0; viewportGizmoAxisWorldDirY = 0; viewportGizmoAxisWorldDirZ = 1; break;
				}
				Double t0 = computeAxisParamFromMouse(mouseDownX, mouseDownY);
				if (t0 != null && Double.isFinite(t0)) {
					viewportGizmoAxisParamStart = t0;
					viewportGizmoUseRayConstraint = true;
				}
			}

			// Precompute drag basis from the camera orientation at drag start (freedom mode only).
			// Option A: horizontal movement follows camera-right projected onto the ground plane (XZ).
			double ry = Math.toRadians(modelrenderer.camera.getRY());
			viewportGizmoFreeRightX = Math.cos(ry);
			viewportGizmoFreeRightY = 0;
			viewportGizmoFreeRightZ = Math.sin(ry);

			viewportGizmoFreeUpX = 0;
			viewportGizmoFreeUpY = 1;
			viewportGizmoFreeUpZ = 0;
		}

		/** Returns the axis parameter t for the closest point on the gizmo axis to the current mouse ray. */
		private Double computeAxisParamFromMouse(int mouseX, int mouseY)
		{
			double[] ray = modelrenderer.getMouseRayWorld(mouseX, mouseY);
			if (ray == null) return null;
			return closestParamRayToAxis(
				ray[0], ray[1], ray[2],
				ray[3], ray[4], ray[5],
				viewportGizmoAxisOriginX, viewportGizmoAxisOriginY, viewportGizmoAxisOriginZ,
				viewportGizmoAxisWorldDirX, viewportGizmoAxisWorldDirY, viewportGizmoAxisWorldDirZ
			);
		}

		/**
		 * Computes the axis line parameter t at the closest point between a ray and an axis line.
		 * Ray: R0 + s*Rd, Axis: A0 + t*Ad. Returns t, or null if nearly parallel.
		 */
		private static Double closestParamRayToAxis(
			double r0x, double r0y, double r0z,
			double rdx, double rdy, double rdz,
			double a0x, double a0y, double a0z,
			double adx, double ady, double adz)
		{
			// Normalize directions defensively
			double rlen = Math.sqrt(rdx*rdx + rdy*rdy + rdz*rdz);
			double alen = Math.sqrt(adx*adx + ady*ady + adz*adz);
			if (!Double.isFinite(rlen) || rlen < 1e-9) return null;
			if (!Double.isFinite(alen) || alen < 1e-9) return null;
			rdx /= rlen; rdy /= rlen; rdz /= rlen;
			adx /= alen; ady /= alen; adz /= alen;

			double w0x = r0x - a0x;
			double w0y = r0y - a0y;
			double w0z = r0z - a0z;
			double a = 1.0; // rdir normalized
			double b = rdx*adx + rdy*ady + rdz*adz;
			double c = 1.0; // adir normalized
			double d = rdx*w0x + rdy*w0y + rdz*w0z;
			double e = adx*w0x + ady*w0y + adz*w0z;
			double den = a*c - b*b;
			if (Math.abs(den) < 1e-6) return null; // nearly parallel
			// s = (b*e - c*d)/den
			// t = (a*e - b*d)/den
			double t = (a*e - b*d) / den;
			return t;
		}

		/**
		 * Builds a world-rotation matrix (4x4) for the given element, including all parent rotations,
		 * but excluding any StartX/StartY/StartZ translations. Translation does not affect the rotation part,
		 * but omitting it keeps the math clearer.
		 */
		private static float[] buildRotationMatrixToElement(Element elem)
		{
			float[] mat = Mat4f.Identity_(new float[16]);
			if (elem == null) return mat;
			java.util.ArrayList<Element> chain = new java.util.ArrayList<Element>();
			Element cur = elem;
			while (cur != null) {
				chain.add(0, cur);
				cur = cur.ParentElement;
			}
			for (Element e : chain) {
				float ox = (float)e.getOriginX();
				float oy = (float)e.getOriginY();
				float oz = (float)e.getOriginZ();
				Mat4f.Translate(mat, mat, new float[] { ox, oy, oz });
				Mat4f.RotateX(mat, mat, (float)e.getRotationX() * GameMath.DEG2RAD);
				Mat4f.RotateY(mat, mat, (float)e.getRotationY() * GameMath.DEG2RAD);
				Mat4f.RotateZ(mat, mat, (float)e.getRotationZ() * GameMath.DEG2RAD);
				Mat4f.Translate(mat, mat, new float[] { -ox, -oy, -oz });
			}
			return mat;
		}

		/**
		 * Precomputes a mapping so we can convert a desired WORLD delta (along X/Y/Z)
		 * into the local StartX/StartY/StartZ delta that produces that world motion.
		 */
		private static double[] computeWorldAxisToLocalMap(Element elem)
		{
			float[] m = buildRotationMatrixToElement(elem);
			// Column-major basis vectors for local axes in world space
			// col0 = local X in world, col1 = local Y in world, col2 = local Z in world
			double c0x = m[0],  c0y = m[1],  c0z = m[2];
			double c1x = m[4],  c1y = m[5],  c1z = m[6];
			double c2x = m[8],  c2y = m[9],  c2z = m[10];

			// For pure rotation matrices, inverse is transpose, so:
			// local = R^T * world.
			// The local delta for a unit WORLD axis is simply the world basis projected onto local axes.
			double[] out = new double[9];
			// world X -> local delta (dx,dy,dz)
			out[0] = c0x; out[1] = c1x; out[2] = c2x;
			// world Y -> local delta
			out[3] = c0y; out[4] = c1y; out[5] = c2y;
			// world Z -> local delta
			out[6] = c0z; out[7] = c1z; out[8] = c2z;
			return out;
		}

		/**
		 * Variant of {@link #computeWorldAxisToLocalMap(Element)} for animation/keyframe mode.
		 * Includes animated rotations from the current frame so world-axis drags stay straight
		 * even when parent rotations come from keyframes.
		 *
		 * Note: Animation version > 0 applies position offsets *after* local rotate/scale, so the
		 * moved element's own rotation does not affect its translation. In that case we only
		 * include parent rotations when building the mapping.
		 */
		private static float[] buildRotationMatrixToElementAnimated(Element elem, AnimationFrame curAnimFrame, int animVersion)
		{
			float[] mat = Mat4f.Identity_(new float[16]);
			if (elem == null) return mat;

			java.util.ArrayList<Element> chain = new java.util.ArrayList<Element>();
			Element cur = elem;
			while (cur != null) {
				chain.add(0, cur);
				cur = cur.ParentElement;
			}

			boolean includeSelfRotation = animVersion <= 0;
			int end = chain.size();
			if (!includeSelfRotation && end > 0) end--; // parents only

			for (int i = 0; i < end; i++) {
				Element e = chain.get(i);
				double rx = e.getRotationX();
				double ry = e.getRotationY();
				double rz = e.getRotationZ();

				if (curAnimFrame != null) {
					AnimFrameElement ae = curAnimFrame.GetAnimFrameElementRec(e);
					if (ae == null) ae = curAnimFrame.GetAnimFrameElementRecByName(e.getName());
					if (ae != null) {
						rx += ae.getRotationX();
						ry += ae.getRotationY();
						rz += ae.getRotationZ();
					}
				}

				Mat4f.RotateX(mat, mat, (float)rx * GameMath.DEG2RAD);
				Mat4f.RotateY(mat, mat, (float)ry * GameMath.DEG2RAD);
				Mat4f.RotateZ(mat, mat, (float)rz * GameMath.DEG2RAD);
			}

			return mat;
		}

		private static double[] computeWorldAxisToLocalMapAnimated(Element elem, AnimationFrame curAnimFrame, int animVersion)
		{
			float[] m = buildRotationMatrixToElementAnimated(elem, curAnimFrame, animVersion);
			// Column-major basis vectors for local axes in world space
			double c0x = m[0],  c0y = m[1],  c0z = m[2];
			double c1x = m[4],  c1y = m[5],  c1z = m[6];
			double c2x = m[8],  c2y = m[9],  c2z = m[10];

			double[] out = new double[9];
			// world X -> local delta
			out[0] = c0x; out[1] = c1x; out[2] = c2x;
			// world Y -> local delta
			out[3] = c0y; out[4] = c1y; out[5] = c2y;
			// world Z -> local delta
			out[6] = c0z; out[7] = c1z; out[8] = c2z;
			return out;
		}



	/**
	 * Gizmo-only OpenGL pick pass.
	 *
	 * The default getElementGLNameAtPos() draws the entire scene for selection, which
	 * means the closest face under the cursor will often "win" over the gizmo when
	 * the gizmo is inside (or behind) model geometry. For gizmo interactions we want
	 * the opposite: if you're clicking on a handle, that handle should be selectable.
	 */
	public int getViewportGizmoGLNameAtPos(int x, int y)
	{
		if (currentProject == null || currentProject.SelectedElement == null) return -1;

		IntBuffer selBuffer = ByteBuffer.allocateDirect(1024).order(ByteOrder.nativeOrder()).asIntBuffer();
		int[] buffer = new int[256];

		IntBuffer viewBuffer = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asIntBuffer();
		int[] viewport = new int[4];

		GL11.glGetInteger(GL11.GL_VIEWPORT, viewBuffer);
		viewBuffer.get(viewport);

		GL11.glSelectBuffer(selBuffer);
		GL11.glRenderMode(GL11.GL_SELECT);
		GL11.glInitNames();
		GL11.glPushName(0);
		GL11.glPushMatrix();
		{
			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GL11.glLoadIdentity();
			GLU.gluPickMatrix(x, y, 3, 3, IntBuffer.wrap(viewport));
			// Same negative-width safeguard as getElementGLNameAtPos
			int leftSidebarWidth = leftSidebarWidth();
			if (canvWidth - leftSidebarWidth < 0)  {
				if (modelrenderer.renderedLeftSidebar != null) {
					modelrenderer.renderedLeftSidebar.nowSidebarWidth = canvWidth - 10;
					leftSidebarWidth = leftSidebarWidth();
				}
			}
			GLU.gluPerspective(60F, (float)(canvWidth - leftSidebarWidth) / (float)canvHeight, 0.3F, 1000F);

			modelrenderer.prepareDraw();
			currentProject.SelectedElement.drawSelectionExtras();
		}
		GL11.glPopMatrix();

		int hits = GL11.glRenderMode(GL11.GL_RENDER);
		selBuffer.get(buffer);
		if (hits > 0)
		{
			int name = buffer[3];
			int depth = buffer[1];
			for (int i = 1; i < hits; i++)
			{
				if ((buffer[i * 4 + 1] < depth || name == 0) && buffer[i * 4 + 3] != 0)
				{
					name = buffer[i * 4 + 3];
					depth = buffer[i * 4 + 1];
				}
			}
			return name;
		}

		return -1;
	}
	private void handleInputMouse(int leftSidebarWidth) {
		
		final float cameraMod = Math.abs(modelrenderer.camera.getZ());
		
		boolean isOnLeftPanel = Mouse.getX() < leftSidebarWidth;
		
		
		
		
		// Any mouse button down starts a "grabbing" interaction (selection, transform, or camera control)
		if (Mouse.isButtonDown(0) || Mouse.isButtonDown(1) || Mouse.isButtonDown(2))
		{
			if (!grabbing)
			{
				lastMouseX = Mouse.getX();
				lastMouseY = Mouse.getY();
				// Remember initial click position so we can distinguish click-to-select from camera drags
				mouseDownX = lastMouseX;
				mouseDownY = lastMouseY;
				mouseDownButton = Mouse.isButtonDown(0) ? 0 : (Mouse.isButtonDown(1) ? 1 : (Mouse.isButtonDown(2) ? 2 : -1));
				mouseDownWithCtrl = leftControl;
				mouseDownWithShift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
					viewportGizmoDragActive = false;
					viewportGizmoDragMode = 0;
					viewportGizmoUseRayConstraint = false;
					viewportGizmoElems = null;
					viewportGizmoBaseY = null;
					viewportGizmoBaseX = null;
					viewportGizmoBaseZ = null;
					viewportGizmoWorldAxisToLocal = null;
					viewportGizmoDragInKeyframeMode = false;
					viewportGizmoDragAnimVersion = 0;
					viewportGizmoKeyframeElems = null;
					viewportGizmoSkipSelection = false;
				grabbing = true;
			}
			
			if (!mouseDownOnCenterPanel && !mouseDownOnLeftPanel && !mouseDownOnRightPanel) {
				mouseDownOnLeftPanel = isOnLeftPanel;
				mouseDownOnCenterPanel = !isOnLeftPanel && !isOnRightPanel;
				mouseDownOnRightPanel = isOnRightPanel;
			}

					// Try start a move gizmo drag (axis or center handle) when clicking in the viewport
					if (mouseDownOnCenterPanel && mouseDownButton == 0 && !mouseDownWithShift) {
						tryStartViewportGizmoDrag();
					}

					// Begin viewport marquee selection on Shift + left mouse drag
					if (!viewportGizmoDragActive && mouseDownOnCenterPanel && mouseDownButton == 0 && mouseDownWithShift && !mouseDownWithCtrl) {
					viewportMarqueeActive = true;
					marqueeStartX = mouseDownX;
					marqueeStartY = mouseDownY;
					marqueeCurX = mouseDownX;
					marqueeCurY = mouseDownY;
					modelrenderer.setViewportMarquee(true, marqueeStartX, marqueeStartY, marqueeCurX, marqueeCurY);
				}

		}
		else
		{
			// Mouse button(s) released
			if (grabbing && mouseDownOnCenterPanel && mouseDownButton == 0 && !mouseDownWithCtrl && !viewportGizmoSkipSelection) {
				int dx = Math.abs(Mouse.getX() - mouseDownX);
				int dy = Math.abs(Mouse.getY() - mouseDownY);
				// Shift + drag: marquee multi-select. Shift + click: add to selection.
				if (mouseDownWithShift) {
					if (dx > 3 || dy > 3) {
						Set<Integer> names = getElementGLNamesInRect(mouseDownX, mouseDownY, Mouse.getX(), Mouse.getY());
						if (names != null && names.size() > 0) {
							currentProject.addElementsSelectionByOpenGLNames(names);
						}
					} else {
						int openGlName = getElementGLNameAtPos(mouseDownX, mouseDownY);
						if (openGlName >= 0) {
							currentProject.addElementSelectionByOpenGLName(openGlName);
						}
					}
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() { updateValues(null); }
					});
				} else {
					// Treat small mouse movement as a click (prevents selecting while panning/rotating the camera)
					if (dx <= 3 && dy <= 3) {
						int openGlName = getElementGLNameAtPos(mouseDownX, mouseDownY);
						if (openGlName >= 0) {
							// If group-move is enabled and the user clicked an already-selected element,
							// keep the multi-selection intact: only change the active element/face.
							Element hitElem = findElementByFaceOpenGlName(openGlName);
							boolean hitIsPartOfMultiSelection = viewportGroupMoveEnabled
									&& hitElem != null
									&& currentProject.SelectedElements != null
									&& currentProject.SelectedElements.size() > 1
									&& currentProject.SelectedElements.contains(hitElem);

							if (hitIsPartOfMultiSelection) {
								int faceIndex = findFaceIndexByOpenGlName(hitElem, openGlName);
								if (faceIndex >= 0) hitElem.setSelectedFace(faceIndex);
								// Keep multi-selection, only change the lead/active element
								currentProject.tree.setLeadSelectionElement(hitElem, true);
								currentProject.SelectedElement = currentProject.tree.getSelectedElement();
								currentProject.SelectedElements = new ArrayList<Element>(currentProject.tree.getSelectedElements());
							} else {
								// Cube + Keyframe: select element. Face: select element + face.
								if (currentRightTab == 1) {
									currentProject.selectElementAndFaceByOpenGLName(openGlName);
								} else {
									currentProject.selectElementByOpenGLName(openGlName);
								}
							}
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() { updateValues(null); }
							});
						}
					}
				}
			}


			viewportMarqueeActive = false;
			modelrenderer.setViewportMarquee(false, 0, 0, 0, 0);

			viewportGizmoDragActive = false;
			viewportGizmoDragMode = 0;
			viewportGizmoUseRayConstraint = false;
			viewportGizmoElems = null;
			viewportGizmoBaseY = null;
			viewportGizmoBaseX = null;
			viewportGizmoBaseZ = null;
			viewportGizmoSkipSelection = false;

			grabbedElem = null;
			
			if (modelrenderer.renderedLeftSidebar != null) {
				modelrenderer.renderedLeftSidebar.mouseUp();
			}
			
			if (!mouseDownOnLeftPanel && grabbing) {
				ModelCreator.changeHistory.endMultichangeHistoryState(ModelCreator.currentProject);
			}
			
			grabbing = false;
			mouseDownOnLeftPanel = false;
			mouseDownOnCenterPanel = false;
			mouseDownOnRightPanel = false;
		}

		
		if (mouseDownOnLeftPanel)
		{
			modelrenderer.renderedLeftSidebar.onMouseDownOnPanel();
			return;
		}
		
		if (mouseDownOnRightPanel) {
			rightTopPanel.onMouseDownOnRightPanel();
			return;
		}

			// Update marquee rectangle while dragging
			if (viewportMarqueeActive && grabbing && mouseDownOnCenterPanel) {
				marqueeCurX = Mouse.getX();
				marqueeCurY = Mouse.getY();
				modelrenderer.setViewportMarquee(true, marqueeStartX, marqueeStartY, marqueeCurX, marqueeCurY);
			}

			// Viewport move gizmo drag (axis handles + center free-move)
			if (grabbing && viewportGizmoDragActive && mouseDownOnCenterPanel && mouseDownButton == 0 && Mouse.isButtonDown(0) && viewportGizmoElems != null)
			{
				int newMouseX = Mouse.getX();
				int newMouseY = Mouse.getY();
				int dx = newMouseX - lastMouseX;
				int dy = newMouseY - lastMouseY;

				if (dx != 0 || dy != 0)
				{
					ModelCreator.changeHistory.beginMultichangeHistoryState();

					double moveX = 0, moveY = 0, moveZ = 0;

								// Axis-only mode: move strictly along world axes (X/Y/Z) with NO leakage into other axes.
								// Use ray-to-axis constraint when possible; otherwise fall back to a stable absolute screen-projected delta.
								if (!viewportFreedomModeEnabled
										&& (viewportGizmoDragMode == 1 || viewportGizmoDragMode == 2 || viewportGizmoDragMode == 3)
										&& viewportGizmoBaseX != null && viewportGizmoBaseY != null && viewportGizmoBaseZ != null)
								{
									double delta = 0;
									boolean haveDelta = false;

									if (viewportGizmoUseRayConstraint) {
										Double tcur = computeAxisParamFromMouse(newMouseX, newMouseY);
										if (tcur != null && Double.isFinite(tcur)) {
											delta = tcur - viewportGizmoAxisParamStart;
											haveDelta = true;
										}
									}

									if (!haveDelta) {
										// Absolute from drag start (prevents accumulation drift).
										int ddx = newMouseX - mouseDownX;
										int ddy = newMouseY - mouseDownY;
										delta = (ddx * viewportGizmoAxisDirX + ddy * viewportGizmoAxisDirY) / 20.0;
										haveDelta = true;
									}

									if (haveDelta) {
										for (Element e : viewportGizmoElems) {
											if (e == null) continue;
											Double bx = viewportGizmoBaseX.get(e);
											Double by = viewportGizmoBaseY.get(e);
											Double bz = viewportGizmoBaseZ.get(e);
											if (bx == null || by == null || bz == null) continue;

																				AnimFrameElement kf = (viewportGizmoDragInKeyframeMode && viewportGizmoKeyframeElems != null) ? viewportGizmoKeyframeElems.get(e) : null;

																	// IMPORTANT:
																	// StartX/StartY/StartZ are applied BEFORE the element's rotation (see Element.draw()),
																	// so for rotated elements (or rotated parents) a pure local X/Z move will drift in world Y.
																	// Convert the desired WORLD-axis delta into the element's local Start delta.
																	double[] map = viewportGizmoWorldAxisToLocal != null ? viewportGizmoWorldAxisToLocal.get(e) : null;
																	if (map == null || map.length < 9) {
																		// Fallback: no mapping available, behave like legacy.
																		switch (viewportGizmoDragMode) {
																			case 1:
																					if (kf != null) { kf.setOffsetX(bx + delta); kf.setOffsetY(by); kf.setOffsetZ(bz); }
																					else { e.setStartX(bx + delta); e.setStartY(by); e.setStartZ(bz); }
																					break;
																			case 2:
																			{
																				double ny = by + delta;
																				if (viewportFloorSnapEnabled && ny < by) ny = by;
																				if (kf != null) { kf.setOffsetX(bx); kf.setOffsetY(ny); kf.setOffsetZ(bz); } else { e.setStartX(bx); e.setStartY(ny); e.setStartZ(bz); }
																				break;
																			}
																			case 3:
																					if (kf != null) { kf.setOffsetX(bx); kf.setOffsetY(by); kf.setOffsetZ(bz + delta); }
																					else { e.setStartX(bx); e.setStartY(by); e.setStartZ(bz + delta); }
																					break;
																			default: break;
																		}
																	} else {
																		double ldx = 0, ldy = 0, ldz = 0;
																		switch (viewportGizmoDragMode) {
																			case 1: // WORLD X
																				ldx = map[0] * delta; ldy = map[1] * delta; ldz = map[2] * delta; break;
																			case 2: // WORLD Y
																				ldx = map[3] * delta; ldy = map[4] * delta; ldz = map[5] * delta; break;
																			case 3: // WORLD Z
																				ldx = map[6] * delta; ldy = map[7] * delta; ldz = map[8] * delta; break;
																			default: break;
																		}

																		double nx = bx + ldx;
																		double ny = by + ldy;
																		double nz = bz + ldz;

																		// Optional floor snap: prevent going below the starting local Y when dragging WORLD Y downward.
																		// This keeps existing behavior and avoids surprising sideways clamps for X/Z drags.
																		if (viewportGizmoDragMode == 2 && viewportFloorSnapEnabled && ny < by) {
																			ny = by;
																		}

																		if (kf != null) { kf.setOffsetX(nx); kf.setOffsetY(ny); kf.setOffsetZ(nz); }
																					else { e.setStartX(nx); e.setStartY(ny); e.setStartZ(nz); }
																	}
										}

										updateValues(null);
										lastMouseX = newMouseX;
										lastMouseY = newMouseY;
										return;
									}
								}

								// Freedom mode: camera-relative movement (legacy behavior).
								if (viewportFreedomModeEnabled) {
									switch (viewportGizmoDragMode) {
										case 1: // X (camera-right on ground)
										{
											double s = (dx * viewportGizmoAxisDirX + dy * viewportGizmoAxisDirY) / 20.0;
											moveX = s * viewportGizmoFreeRightX;
											moveZ = s * viewportGizmoFreeRightZ;
											break;
										}
										case 2: // Y (world up)
										{
											double s = (dx * viewportGizmoAxisDirX + dy * viewportGizmoAxisDirY) / 20.0;
											moveY = s;
											break;
										}
										case 3: // Z (camera-forward on ground)
										{
											double s = (dx * viewportGizmoAxisDirX + dy * viewportGizmoAxisDirY) / 20.0;
											// Camera-forward on XZ plane is perpendicular to camera-right.
											double forwardX = -viewportGizmoFreeRightZ;
											double forwardZ = viewportGizmoFreeRightX;
											moveX = s * forwardX;
											moveZ = s * forwardZ;
											break;
										}
										case 4: // FREE (camera-right + world up)
										{
											double sx = dx / 20.0;
											double sy = dy / 20.0;
											moveX = sx * viewportGizmoFreeRightX + sy * viewportGizmoFreeUpX;
											moveY = sx * viewportGizmoFreeRightY + sy * viewportGizmoFreeUpY;
											moveZ = sx * viewportGizmoFreeRightZ + sy * viewportGizmoFreeUpZ;
											break;
										}
										default: break;
									}
								}

					for (Element e : viewportGizmoElems)
					{
						if (e == null) continue;

						AnimFrameElement kf = (viewportGizmoDragInKeyframeMode && viewportGizmoKeyframeElems != null) ? viewportGizmoKeyframeElems.get(e) : null;
						if (kf != null) {
							if (moveX != 0) kf.setOffsetX(kf.getOffsetX() + moveX);
							if (moveZ != 0) kf.setOffsetZ(kf.getOffsetZ() + moveZ);

							double applyY = moveY;
							if (applyY != 0)
							{
								if (viewportFloorSnapEnabled && applyY < 0 && viewportGizmoBaseY != null) {
									Double baseY = viewportGizmoBaseY.get(e);
									if (baseY != null) {
										double curY = kf.getOffsetY();
										if (curY + applyY < baseY) {
											applyY = baseY - curY;
										}
									}
								}
								kf.setOffsetY(kf.getOffsetY() + applyY);
							}
						} else {
							if (moveX != 0) e.addStartX(moveX);
							if (moveZ != 0) e.addStartZ(moveZ);

							double applyY = moveY;
							if (applyY != 0)
							{
								if (viewportFloorSnapEnabled && applyY < 0 && viewportGizmoBaseY != null) {
									Double baseY = viewportGizmoBaseY.get(e);
									if (baseY != null) {
										double curY = e.getStartY();
										if (curY + applyY < baseY) {
											applyY = baseY - curY;
										}
									}
								}
								e.addStartY(applyY);
							}
						}
					}

					
updateValues(null);
				}

				lastMouseX = newMouseX;
				lastMouseY = newMouseY;
				return;
			}
		
		if (leftControl)
		{	
			if (grabbedElem == null && (Mouse.isButtonDown(0) || Mouse.isButtonDown(1)))
			{
				int openGlName = getElementGLNameAtPos(Mouse.getX(), Mouse.getY());
				if (openGlName >= 0)
				{
					Element hitElem = findElementByFaceOpenGlName(openGlName);
					boolean hitIsPartOfMultiSelection = viewportGroupMoveEnabled
							&& hitElem != null
							&& currentProject.SelectedElements != null
							&& currentProject.SelectedElements.size() > 1
							&& currentProject.SelectedElements.contains(hitElem);

					if (hitIsPartOfMultiSelection) {
						// Don't clear selection: just make the hit element the active one and set face.
						int faceIndex = findFaceIndexByOpenGlName(hitElem, openGlName);
						if (faceIndex >= 0) hitElem.setSelectedFace(faceIndex);
						currentProject.SelectedElement = hitElem;
						grabbedElem = hitElem;
					} else {
						currentProject.selectElementAndFaceByOpenGLName(openGlName);
						grabbedElem = rightTopPanel.getCurrentElement();
						currentProject.selectElement(grabbedElem);
					}
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() { updateValues(null); } 
					});
				}
			}

			if (grabbing && grabbedElem != null)
			{
				ModelCreator.changeHistory.beginMultichangeHistoryState();
				
				Element element = grabbedElem;
				
				int index = element.getSelectedFaceIndex();
				
				int newMouseX = Mouse.getX();
				int newMouseY = Mouse.getY();

				double xMovement = ((double)(newMouseX - lastMouseX)) / 20.0;
				double yMovement = ((double)(newMouseY - lastMouseY)) / 20.0;

				if (xMovement != 0 || yMovement != 0)
				{				
					if (Mouse.isButtonDown(0))
					{
								// Group move (optional): move selected root elements together (avoid double-moving children).
								java.util.List<Element> elemsToMove;
								if (viewportGroupMoveEnabled
										&& currentProject.SelectedElements != null
										&& currentProject.SelectedElements.size() > 1)
								{
									java.util.HashSet<Element> selected = new java.util.HashSet<Element>(currentProject.SelectedElements);
									elemsToMove = new java.util.ArrayList<Element>();
									for (Element e : currentProject.SelectedElements) {
										if (e == null) continue;
										boolean hasSelectedAncestor = false;
										Element p = e.ParentElement;
										while (p != null) {
											if (selected.contains(p)) { hasSelectedAncestor = true; break; }
											p = p.ParentElement;
										}
										if (!hasSelectedAncestor) elemsToMove.add(e);
									}
									if (elemsToMove.isEmpty()) {
										elemsToMove = java.util.Collections.singletonList(element);
									}
								} else {
									elemsToMove = java.util.Collections.singletonList(element);
								}

						for (Element e : elemsToMove) {
							if (e == null) continue;
							switch (index) {
								case 0: // N
								case 2: // S
									e.addStartZ(xMovement);
									break;
								
								case 1: // E
								case 3: // W
									e.addStartX(xMovement);
									break;
								
								case 4:
								case 5:
									e.addStartY(yMovement);
									break;
							}
						}
					}
					else if (Mouse.isButtonDown(1))
					{
						
						switch (index) {
							case 0: // N
								element.addStartZ(-xMovement);
								element.addDepth(xMovement);
								break;
							case 2: // S
								element.addDepth(-xMovement);
								break;
							
							case 1: // E
								element.addWidth(xMovement);
								break;
							case 3: // W
								element.addStartX(-xMovement);
								element.addWidth(xMovement);
								break;
							
							case 4: // U
							case 5: // D
								element.addHeight(yMovement);
								break;
						}
					}

					// Smooth movement: always advance last mouse position
					lastMouseX = newMouseX;
					lastMouseY = newMouseY;
					
					updateValues(null);
					// Only resizing affects UV layout. Simple translation does not need to update UVs.
					if (Mouse.isButtonDown(1)) {
						element.updateUV();
					}
				}
			}
		}
		else
		{
			// Camera controls:
			// - Pan: hold Middle Mouse Button OR hold Alt + Right Mouse Button
			// - Rotate: hold Right Mouse Button
			// Left Mouse Button never moves the camera (reserved for selection/transform)
			boolean altDown = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
			if (Mouse.isButtonDown(2) || (altDown && Mouse.isButtonDown(1)))
			{
				final float modifier = (cameraMod * 0.05f);
				modelrenderer.camera.addX((float) (Mouse.getDX() * 0.01F) * modifier);
				modelrenderer.camera.addY((float) (Mouse.getDY() * 0.01F) * modifier);
			}
			else if (Mouse.isButtonDown(1))
			{
				final float modifier = applyLimit(cameraMod * 0.1f);
				modelrenderer.camera.rotateX(-(float) (Mouse.getDY() * 0.5F) * modifier);
				final float rxAbs = Math.abs(modelrenderer.camera.getRX());
				modelrenderer.camera.rotateY((rxAbs >= 90 && rxAbs < 270 ? -1 : 1) * (float) (Mouse.getDX() * 0.5F) * modifier);
			}

			final float wheel = Mouse.getDWheel();
			if (wheel != 0)
			{
				modelrenderer.camera.addZ(wheel * (cameraMod / 2500F));
			}
			
			if (Keyboard.isKeyDown(Keyboard.KEY_MINUS) || Keyboard.isKeyDown(Keyboard.KEY_SUBTRACT)) {
				modelrenderer.camera.addZ(-50 * (cameraMod / 2500F));
			}
			if (Keyboard.isKeyDown(Keyboard.KEY_ADD)) {
				modelrenderer.camera.addZ(50 * (cameraMod / 2500F));
			}
		}
		
	}
	

	public int getElementGLNameAtPos(int x, int y)
	{
		IntBuffer selBuffer = ByteBuffer.allocateDirect(1024).order(ByteOrder.nativeOrder()).asIntBuffer();
		int[] buffer = new int[256];

		IntBuffer viewBuffer = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asIntBuffer();
		int[] viewport = new int[4];

		int hits;
		GL11.glGetInteger(GL11.GL_VIEWPORT, viewBuffer);
		viewBuffer.get(viewport);

		GL11.glSelectBuffer(selBuffer);
		GL11.glRenderMode(GL11.GL_SELECT);
		GL11.glInitNames();
		GL11.glPushName(0);
		GL11.glPushMatrix();
		{
			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GL11.glLoadIdentity();
			GLU.gluPickMatrix(x, y, 1, 1, IntBuffer.wrap(viewport));
			//glViewPort view must not go negative 
			int leftSidebarWidth = leftSidebarWidth();
			if (canvWidth - leftSidebarWidth < 0)  {
				if (modelrenderer.renderedLeftSidebar != null) {
				 	modelrenderer.renderedLeftSidebar.nowSidebarWidth = canvWidth - 10;
				 	leftSidebarWidth = leftSidebarWidth();
				}
			}
			GLU.gluPerspective(60F, (float) (canvWidth - leftSidebarWidth) / (float) canvHeight, 0.3F, 1000F);

			modelrenderer.prepareDraw();
			modelrenderer.drawGridAndElements();
		}
		GL11.glPopMatrix();
		hits = GL11.glRenderMode(GL11.GL_RENDER);

		selBuffer.get(buffer);
		if (hits > 0)
		{
			int name = buffer[3];
			int depth = buffer[1];
			
			for (int i = 1; i < hits; i++)
			{
				if ((buffer[i * 4 + 1] < depth || name == 0) && buffer[i * 4 + 3] != 0)
				{
					name = buffer[i * 4 + 3];
					depth = buffer[i * 4 + 1];
				}
			}

			return name;
		}

		return -1;
	}

	/**
	 * Finds the element that owns the given OpenGL face id.
	 * This is used for viewport interactions where we want to avoid clearing an existing multi-selection.
	 */
	private Element findElementByFaceOpenGlName(int openGlName)
	{
		if (currentProject == null || currentProject.rootElements == null) return null;
		return findElementByFaceOpenGlName(openGlName, currentProject.rootElements);
	}

	private Element findElementByFaceOpenGlName(int openGlName, java.util.List<Element> elems)
	{
		if (elems == null) return null;
		for (Element elem : elems) {
			if (elem == null) continue;
			// Check this element's faces
			Face[] faces = elem.getAllFaces();
			if (faces != null) {
				for (int i = 0; i < faces.length; i++) {
					if (faces[i] != null && faces[i].openGlName == openGlName) {
						return elem;
					}
				}
			}
			// Recurse children
			Element hit = findElementByFaceOpenGlName(openGlName, elem.ChildElements);
			if (hit != null) return hit;
			hit = findElementByFaceOpenGlName(openGlName, elem.StepChildElements);
			if (hit != null) return hit;

			// Recurse into attachment point step children (these can also be selectable)
			if (elem.AttachmentPoints != null) {
				for (AttachmentPoint ap : elem.AttachmentPoints) {
					if (ap == null || ap.StepChildElements == null) continue;
					hit = findElementByFaceOpenGlName(openGlName, ap.StepChildElements);
					if (hit != null) return hit;
				}
			}
		}
		return null;
	}

	private int findFaceIndexByOpenGlName(Element elem, int openGlName)
	{
		if (elem == null) return -1;
		Face[] faces = elem.getAllFaces();
		if (faces == null) return -1;
		for (int i = 0; i < faces.length; i++) {
			if (faces[i] != null && faces[i].openGlName == openGlName) return i;
		}
		return -1;
	}

	/**
	 * Returns all OpenGL selection names (face ids) inside the given screen-space rectangle.
	 * Used for Shift + drag marquee selection in the 3D viewport.
	 */
	public Set<Integer> getElementGLNamesInRect(int x1, int y1, int x2, int y2)
	{
		// Clamp to the current 3D viewport bounds
		int left = leftSidebarWidth();
		int right = Math.max(left + 1, canvWidth);
		int top = canvHeight;
		int bottom = 0;

		x1 = Math.max(left, Math.min(right, x1));
		x2 = Math.max(left, Math.min(right, x2));
		y1 = Math.max(bottom, Math.min(top, y1));
		y2 = Math.max(bottom, Math.min(top, y2));

		int w = Math.abs(x2 - x1);
		int h = Math.abs(y2 - y1);
		if (w < 1) w = 1;
		if (h < 1) h = 1;

		int cx = (x1 + x2) / 2;
		int cy = (y1 + y2) / 2;

		// Larger selection buffer for marquee selection
		IntBuffer selBuffer = ByteBuffer.allocateDirect(8192 * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
		int[] buffer = new int[8192];

		IntBuffer viewBuffer = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asIntBuffer();
		int[] viewport = new int[4];

		GL11.glGetInteger(GL11.GL_VIEWPORT, viewBuffer);
		viewBuffer.get(viewport);

		int hits;
		GL11.glSelectBuffer(selBuffer);
		GL11.glRenderMode(GL11.GL_SELECT);
		GL11.glInitNames();
		GL11.glPushName(0);
		GL11.glPushMatrix();
		{
			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GL11.glLoadIdentity();
			GLU.gluPickMatrix(cx, cy, w, h, IntBuffer.wrap(viewport));

			// glViewPort view must not go negative
			int ls = leftSidebarWidth();
			if (canvWidth - ls < 0) {
				if (modelrenderer.renderedLeftSidebar != null) {
					modelrenderer.renderedLeftSidebar.nowSidebarWidth = canvWidth - 10;
					ls = leftSidebarWidth();
				}
			}
			GLU.gluPerspective(60F, (float)(canvWidth - ls) / (float)canvHeight, 0.3F, 1000F);

			modelrenderer.prepareDraw();
			modelrenderer.drawGridAndElements();
		}
		GL11.glPopMatrix();
		hits = GL11.glRenderMode(GL11.GL_RENDER);

		selBuffer.get(buffer, 0, Math.min(selBuffer.limit(), buffer.length));

		Set<Integer> names = new HashSet<Integer>();
		if (hits <= 0) return names;

		int idx = 0;
		for (int i = 0; i < hits; i++)
		{
			if (idx + 3 >= buffer.length) break;
			int nNames = buffer[idx];
			// int minZ = buffer[idx + 1];
			// int maxZ = buffer[idx + 2];
			for (int n = 0; n < nNames; n++) {
				int name = buffer[idx + 3 + n];
				if (name != 0) names.add(name);
			}
			idx += 3 + nNames;
		}

		return names;
	}

	public float applyLimit(float value)
	{
		if (value > 0.4F)
		{
			value = 0.4F;
		}
		else if (value < 0.15F)
		{
			value = 0.15F;
		}
		return value;
	}


	public void startScreenshot(PendingScreenshot screenshot)
	{
		this.screenshot = screenshot;
	}

	public void setSidebar(LeftSidebar s)
	{
		// During startup, RightPanel.initComponents() may call this before modelrenderer is constructed.
		if (modelrenderer == null) {
			pendingLeftSidebar = s;
			return;
		}
		modelrenderer.renderedLeftSidebar = s;
		if (s != null) s.Load();
	}
	
	


	public IElementManager getElementManager()
	{
		return rightTopPanel;
	}
	
	public void close()
	{
		this.closeRequested = true;
	}

	public boolean getCloseRequested()
	{
		return closeRequested;
	}

	

	
	

	private DropTarget getCustomDropTarget()
	{
		 return new DropTarget() {
			private static final long serialVersionUID = 1L;
			
			@Override
		    public synchronized void drop(DropTargetDropEvent evt) {
				modelrenderer.renderDropTagets = false;
				
				try {

					Transferable transferable = evt.getTransferable();
					Object obj;
					if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
						evt.acceptDrop(evt.getDropAction());
						obj = transferable.getTransferData(DataFlavor.javaFileListFlavor);
					} else {
						obj = transferable.getTransferData(transferable.getTransferDataFlavors()[0]);
					}
					

					if (obj instanceof DefaultMutableTreeNode[]) {
						//evt.rejectDrop();
						return;
					}
					
					@SuppressWarnings("rawtypes")
					List data = (List)obj;
					
					for (Object elem : data) {
						if (elem instanceof File) {
							File file = (File)elem;
							
							if (file.getName().endsWith(".json")) {		
								LoadFile(file.getAbsolutePath());
								return;
							}
							
							if (file.getName().endsWith(".png")) {
								String code = file.getName();
								code = code.substring(0, code.indexOf("."));
								PendingTexture pendingTexture = new PendingTexture(code, file, ModelCreator.Instance, 0);
								
								
								//int x = evt.getLocation().x;
								int y = evt.getLocation().y;
								pendingTexture.SetInsertTextureSizeEntry();
								if (y >= canvHeight / 3) {
									if (y >= 2 * canvHeight / 3) {
										pendingTexture.SetReplacesAllTextures();	
									} else {
										pendingTexture.SetReplacesSelectElementTextures();
									}
								}
								
								AddPendingTexture(pendingTexture);
								
								return;
							}
							
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run()
								{
									JOptionPane.showMessageDialog(null, "Huh? What file is this? I can only read .png and .json :(");							
								}
							});
							
							
							return;
						}
							
					}
					
				} catch (Exception e) {
					System.out.println("Failed reading dropped file. File is probably in an incorrect format.");
					StackTraceElement[] elems = e.getStackTrace();
					String trace = "";
					for (int i = 0; i < elems.length; i++) {
						trace += elems[i].toString() + "\n";
						if (i >= 10) break;
					}
					
		        	JOptionPane.showMessageDialog(null, "Couldn't open this file, something unexpecteded happened\n\n" + e.toString() + "\nat\n" + trace);
		        	e.printStackTrace();
				}
		        		        
		        evt.dropComplete(true);
		    }
		 
			@Override
			public synchronized void dragEnter(DropTargetDragEvent evt)
			{
				DataFlavor flavor = evt.getCurrentDataFlavors()[0];
				
				try {
					Object obj = evt.getTransferable().getTransferData(flavor);
					
					if (obj instanceof DefaultMutableTreeNode[]) {
						return;
					}
				
					@SuppressWarnings("rawtypes")
					List data = (List)obj;
					
					for (Object elem : data) {
						if (elem instanceof File) {
							File file = (File)elem;							
							if (file.getName().endsWith(".png")) {
								modelrenderer.renderDropTagets = true;
								modelrenderer.dropLocation = evt.getLocation();
							}
							return;
						}
							
					}
					
				} catch (Exception e) {
					
				}
				
				
				super.dragEnter(evt);
			}
			
			@Override
			public synchronized void dragOver(DropTargetDragEvent evt)
			{
				DataFlavor flavor = evt.getCurrentDataFlavors()[0];
				
				try {
					Object obj = evt.getTransferable().getTransferData(flavor);
					
					if (obj instanceof DefaultMutableTreeNode[]) {
						return;
					}
				
					@SuppressWarnings("rawtypes")
					List data = (List)obj;
					
					for (Object elem : data) {
						if (elem instanceof File) {
							File file = (File)elem;							
							if (file.getName().endsWith(".png")) {
								modelrenderer.renderDropTagets = true;
								modelrenderer.dropLocation = evt.getLocation();
							}
							return;
						}
							
					}
					
				} catch (Exception e) {
					
				}
				
				
				super.dragOver(evt);
			}
		
			
			@Override
			public synchronized void dragExit(DropTargetEvent dte)
			{
				modelrenderer.renderDropTagets = false;
				super.dragExit(dte);
			}
		 
		 };
	}

	
	@Override
	public void onTextureLoaded(boolean isNew, String errormessage, String texture)
	{										
		if (errormessage != null)
		{
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					JOptionPane error = new JOptionPane();
					error.setMessage(errormessage);
					JDialog dialog = error.createDialog(canvas, "Texture Error");
					dialog.setLocationRelativeTo(null);
					dialog.setModal(true);
					dialog.setAlwaysOnTop(true);
					dialog.setVisible(true);
			}});
		} else {
			
			if (FaceTexturePanel.dlg != null) {
				if (FaceTexturePanel.dlg.IsOpened()) {
					FaceTexturePanel.dlg.onTextureLoaded(isNew, errormessage, texture);
				}
			}
			
		}
		

	}

	public void LoadFile(String filePath)
	{
		ModelCreator.currentBackdropProject = null;
		
		if (ModelCreator.currentProject.rootElements.size() > 0 && currentProject.needsSaving)
		{
			int returnVal = JOptionPane.showConfirmDialog(null, "Your current unsaved project will be cleared, are you sure you want to continue?", "Warning", JOptionPane.YES_NO_OPTION);		
			if (returnVal == JOptionPane.NO_OPTION || returnVal == JOptionPane.CLOSED_OPTION) return;
		}
				
		ignoreDidModify++;
		
		if (filePath == null) {
			setTitle("(untitled) - " + windowTitle);
			currentProject = new Project(null);
			currentProject.LoadIntoEditor(ModelCreator.rightTopPanel);
			
		} else {
			prefs.put("filePath", filePath);
			Importer importer = new Importer(filePath);
			
			ignoreValueUpdates = true;
			Project project = importer.loadFromJSON("normal");
			Project oldproject = currentProject;
			currentProject = project;
			
			for (TextureEntry entry : oldproject.TexturesByCode.values()) {
				entry.Dispose();
			}
			
			ignoreValueUpdates = false;
			currentProject.LoadIntoEditor(ModelCreator.rightTopPanel);
			
			setTitle(new File(currentProject.filePath).getName() + " - " + windowTitle);
		}
		
		
		changeHistory.clear();
		changeHistory.addHistoryState(currentProject);
		
		currentProject.needsSaving = false;
		currentProject.reloadStepparentRelationShips();
		if (currentBackdropProject != null) {
			currentBackdropProject.reloadStepparentRelationShips();
			currentProject.attachToBackdropProject(currentBackdropProject);
		}
		
		
		ignoreDidModify--;

		
		ModelCreator.updateValues(null);
		currentProject.tree.jtree.updateUI();
		
		if (currentMountBackdropProject != null) {
			AttachmentPoint ap = currentMountBackdropProject.findAttachmentPoint("Rider");
			for (Element rootelem : currentProject.rootElements) {
				ap.StepChildElements.add(rootelem);
			}
		}
	}
	
	
	public void ImportFile(String filePath)
	{
		ignoreDidModify++;
		ignoreValueUpdates = true;
		
		Importer importer = new Importer(filePath);
		Project importedproject = importer.loadFromJSON("normal");
		
		for(Element elem : importedproject.rootElements) {
			currentProject.rootElements.add(elem);
		}
		
		for(Animation anim : importedproject.Animations) {
			currentProject.Animations.add(anim);
		}
		
		for (PendingTexture tex : importedproject.PendingTextures) {
			currentProject.PendingTextures.add(tex);
		}
		
		for (String key : importedproject.TextureSizes.keySet()) {
			currentProject.TextureSizes.put(key, importedproject.TextureSizes.get(key));
		}

		changeHistory.beginMultichangeHistoryState();
		changeHistory.addHistoryState(currentProject);

		ignoreValueUpdates = false;
		
		currentProject.LoadIntoEditor(ModelCreator.rightTopPanel);
		
		ignoreDidModify--;
		
		currentProject.reloadStepparentRelationShips();
		if (currentBackdropProject != null) {
			currentBackdropProject.reloadStepparentRelationShips();
			currentProject.attachToBackdropProject(currentBackdropProject);
		}
		
		currentProject.needsSaving = true;		
		ModelCreator.updateValues(null);
		currentProject.tree.jtree.updateUI();
		
		if (currentMountBackdropProject != null) {
			Element elem = currentMountBackdropProject.findElement("Rider");
			for (Element rootelem : currentProject.rootElements) {
				elem.StepChildElements.add(rootelem);
			}
		}

		changeHistory.endMultichangeHistoryState(currentProject);
	}

	public static void LoadColorConfig(String path) {

		ElementTreeCellRenderer.colorConfig.clear();
		JsonParser jp = new JsonParser();
		try {
			File f = new File(path);
			if (!f.exists()) {
				System.out.println("Color Config not found");
				return;
			}
			FileReader fr = new FileReader(path);
			JsonElement je = jp.parse(fr);
			for (Map.Entry<String, JsonElement> entry : je.getAsJsonObject().entrySet()) {
				Color col = Color.decode(entry.getValue().getAsString());
				ElementTreeCellRenderer.colorConfig.put(entry.getKey().toLowerCase(), col);
			}
			if(currentProject.tree != null) {
				currentProject.tree.updateUI();
			}
		} catch (FileNotFoundException ex) {
			System.out.println(ex.getMessage());
		}
	}

	public void LoadBackdropFile(String filePath)
	{		
		ignoreDidModify++;
		ignoreValueUpdates = true;

		Importer importer = new Importer(filePath);
		Project project = importer.loadFromJSON("backdrop");
		project.reloadStepparentRelationShips();

		currentBackdropProject = project;
		currentProject.attachToBackdropProject(currentBackdropProject);
		
		String shapeBasePath = ModelCreator.prefs.get("shapePath", ".");
		
		String subPath = filePath;
		if (filePath.contains(shapeBasePath) && shapeBasePath != ".") {
			subPath = filePath.substring(shapeBasePath.length()  + 1);
		}
		else {
			int index = filePath.indexOf("assets"+File.separator+"shapes"+File.separator);
			if (index>0) subPath = filePath.substring(index + "assets/shapes/".length());
		}
		subPath = subPath.replace('\\', '/').replace(".json", "");
		
		currentProject.backDropShape = subPath;
		ignoreValueUpdates = false;
		ignoreDidModify--;
	}
	
	
	
	
	public void LoadMountBackdropFile(String filePath)
	{		
		ignoreDidModify++;
		ignoreValueUpdates = true;

		Importer importer = new Importer(filePath);
		Project project = importer.loadFromJSON("mountbackdrop");

		currentMountBackdropProject = project;
		
		String shapeBasePath = ModelCreator.prefs.get("shapePath", ".");
		
		String subPath = filePath;
		if (filePath.contains(shapeBasePath) && shapeBasePath != ".") {
			subPath = filePath.substring(shapeBasePath.length()  + 1);
		}
		else {
			int index = filePath.indexOf("assets"+File.separator+"shapes"+File.separator);
			if (index>0) subPath = filePath.substring(index + "assets/shapes/".length());
		}
		subPath = subPath.replace('\\', '/').replace(".json", "");
		
		currentProject.mountBackDropShape = subPath;
		ignoreValueUpdates = false;
		ignoreDidModify--;

		
		AttachmentPoint ap = currentMountBackdropProject.findAttachmentPoint("Rider");
		for (Element rootelem : currentProject.rootElements) {
			ap.StepChildElements.add(rootelem);
		}
		
		ModelCreator.leftKeyframesPanel.Load();
	}
	
	
	

	public void SaveProject(File file)
	{
		Exporter exporter = new Exporter(ModelCreator.currentProject);
		exporter.export(file);
		
		ModelCreator.currentProject.filePath = file.getAbsolutePath(); 
		currentProject.needsSaving = false;
		ModelCreator.updateValues(null);
		changeHistory.didSave();
	}
	

	public void SaveProjectAs()
	{
		JFileChooser chooser = new JFileChooser(ModelCreator.prefs.get("filePath", "."));
		chooser.setDialogTitle("Output Directory");
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setApproveButtonText("Save");

		FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON (.json)", "json");
		chooser.setFileFilter(filter);

		int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			if (chooser.getSelectedFile().exists())
			{
				returnVal = JOptionPane.showConfirmDialog(null, "A file already exists with that name, are you sure you want to override?", "Warning", JOptionPane.YES_NO_OPTION);
			}
			if (returnVal != JOptionPane.NO_OPTION && returnVal != JOptionPane.CLOSED_OPTION)
			{
				String filePath = chooser.getSelectedFile().getAbsolutePath();
				ModelCreator.prefs.put("filePath", filePath);
				
				if (!filePath.endsWith(".json")) {
					chooser.setSelectedFile(new File(filePath + ".json"));
				}
				
				SaveProject(chooser.getSelectedFile());
			}
		}
	}
	
	public Camera getCamera() {
		return this.modelrenderer.camera;
	}

	public static void reloadStepparentRelationShips()
	{
		if (ModelCreator.currentBackdropProject != null) {
			ModelCreator.currentBackdropProject.reloadStepparentRelationShips();
		}
		
		ModelCreator.currentProject.reloadStepparentRelationShips();
		
	}

	
}
