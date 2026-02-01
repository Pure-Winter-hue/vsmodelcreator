package at.vintagestory.modelcreator;

import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glViewport;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
import javax.swing.JTextField;
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
import at.vintagestory.modelcreator.input.command.CommandToggleViewportGizmoMode;
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
import at.vintagestory.modelcreator.util.QUtil;
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
	public static boolean mirrorMode = false;
	public static boolean showShade = true;
	public static boolean transparent = true;
	public static boolean renderTexture = true;
	public static boolean autoreloadTexture = true;
	public static boolean autofixZFighting = false;
	public static boolean childrenInheritRenderPass = true;
	public static final int MAX_RECENT_FILES = 12;
	/**
	 * Nudge distance used by the "Autofix Z-Fighting" feature.
	 *
	 * The modeler uses a 0..16-ish coordinate space (aka "pixel" units). The requested
	 * +0.001 is intended in block units, which corresponds to 0.001 * 16 = 0.016 here.
	 * This also makes the adjustment large enough to be depth-buffer effective.
	 */
	// Amount to nudge along the detected axis when fixing coplanar faces.
	// Uses the model's coordinate space (0..16 per block).
	public static final double autofixZFightingEpsilon = 0.001;
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

	// Viewport transform gizmo mode: Move / Rotate / Scale (toggle with 'Q')
	public static final int VIEWPORT_GIZMO_MODE_MOVE = 0;
	public static final int VIEWPORT_GIZMO_MODE_ROTATE = 1;
	public static final int VIEWPORT_GIZMO_MODE_SCALE = 2;
	public static int viewportGizmoMode = VIEWPORT_GIZMO_MODE_MOVE;
	// When true, hides the viewport transform gizmo overlays and disables gizmo interaction (persisted)
	public static boolean viewportGizmoHidden = false;

	// Whether the viewport translate gizmo center circle handle is enabled (persisted)
	public static boolean viewportCenterCircleEnabled = true;
	// Radius (px) of the viewport translate gizmo center circle (persisted)
	public static int viewportCenterCircleRadius = 18;

	// Floor grid tiling for the 3D viewport (1 = original single 16x16 tile)
	public static int viewportFloorGridRows = 1;
	public static int viewportFloorGridCols = 1;

	// Optional block-visualization overlays for the floor grid
	public static boolean viewportShowBlocksFloor = false;
	public static boolean viewportShowBlocksAir = false;
	// How many block layers to outline above (>=0) and below (>=0) the floor plane
	public static int viewportAirBlocksAbove = 0;
	public static int viewportAirBlocksBelow = 0;

	// Canvas Variables
	private final static AtomicReference<Dimension> newCanvasSize = new AtomicReference<Dimension>();
	public static Canvas canvas;
	// HiDPI UI scale factor used for the OpenGL canvas (canvas pixel size = panel size * scale).
	public static double viewportUiScale = 1.0;
	// Container panel that owns the OpenGL canvas (null layout). Used for placing temporary overlay widgets.
	public static JPanel canvasContainerPanel;
	// Inline editor used for numeric entry in viewport sidebar (created lazily).
	private static JTextField viewportInlineNumberField;
	private static java.util.function.Consumer<String> viewportInlineNumberCommit;
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
	public at.vintagestory.modelcreator.gui.left.LeftViewportSidebar viewportSidebar;
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

	public static void setViewportGizmoMode(int mode)
	{
		if (mode != VIEWPORT_GIZMO_MODE_MOVE && mode != VIEWPORT_GIZMO_MODE_ROTATE && mode != VIEWPORT_GIZMO_MODE_SCALE) {
			mode = VIEWPORT_GIZMO_MODE_MOVE;
		}
		viewportGizmoMode = mode;
		prefs.putInt("viewportGizmoMode", viewportGizmoMode);
	}

	public static void toggleViewportGizmoMode()
	{
		int next = viewportGizmoMode + 1;
		if (next > VIEWPORT_GIZMO_MODE_SCALE) next = VIEWPORT_GIZMO_MODE_MOVE;
		setViewportGizmoMode(next);
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
		autofixZFighting = prefs.getBoolean("autofixZFighting", false);
		childrenInheritRenderPass = prefs.getBoolean("childrenInheritRenderPass", true);
		repositionWhenReparented = prefs.getBoolean("repositionWhenReparented", true);
		elementTreeHeight = prefs.getInt("elementTreeHeight", 240);
		viewportAxisLineThickness = prefs.getInt("viewportAxisLineThickness", 10);
		viewportAxisLineLength = prefs.getInt("viewportAxisLineLength", 110);
		viewportFreedomModeEnabled = prefs.getBoolean("viewportFreedomModeEnabled", false);
		viewportGizmoMode = prefs.getInt("viewportGizmoMode", VIEWPORT_GIZMO_MODE_MOVE);
		viewportGizmoHidden = prefs.getBoolean("viewportGizmoHidden", false);
		if (viewportGizmoMode != VIEWPORT_GIZMO_MODE_MOVE && viewportGizmoMode != VIEWPORT_GIZMO_MODE_ROTATE && viewportGizmoMode != VIEWPORT_GIZMO_MODE_SCALE) {
			viewportGizmoMode = VIEWPORT_GIZMO_MODE_MOVE;
		}
		viewportAxisLineThickness = Math.max(2, Math.min(40, viewportAxisLineThickness));
		viewportAxisLineLength = Math.max(40, Math.min(260, viewportAxisLineLength));

		viewportCenterCircleEnabled = prefs.getBoolean("viewportCenterCircleEnabled", true);
		viewportCenterCircleRadius = prefs.getInt("viewportCenterCircleRadius", 18);
		viewportCenterCircleRadius = Math.max(6, Math.min(60, viewportCenterCircleRadius));
		viewportFloorGridRows = prefs.getInt("viewportFloorGridRows", 1);
		viewportFloorGridCols = prefs.getInt("viewportFloorGridCols", 1);
		viewportFloorGridRows = Math.max(1, Math.min(10, viewportFloorGridRows));
		viewportFloorGridCols = Math.max(1, Math.min(10, viewportFloorGridCols));
		viewportShowBlocksFloor = prefs.getBoolean("viewportShowBlocksFloor", false);
		viewportShowBlocksAir = prefs.getBoolean("viewportShowBlocksAir", false);
		viewportAirBlocksAbove = prefs.getInt("viewportAirBlocksAbove", 0);
		viewportAirBlocksBelow = prefs.getInt("viewportAirBlocksBelow", 0);
		viewportAirBlocksAbove = Math.max(0, Math.min(10, viewportAirBlocksAbove));
		viewportAirBlocksBelow = Math.max(0, Math.min(10, viewportAirBlocksBelow));

		viewportGridSnapEnabled = prefs.getBoolean("viewportGridSnapEnabled", false);
		viewportVertexSnapEnabled = prefs.getBoolean("viewportVertexSnapEnabled", false);
			viewportGridSnapStepQ = prefs.getInt("viewportGridSnapStepQ", 4);
		viewportGridSnapStepQ = Math.max(1, Math.min(80, viewportGridSnapStepQ));

		viewportGroupRotateEnabled = prefs.getBoolean("viewportGroupRotateEnabled", false);
		viewportRotateIndividualOriginEnabled = prefs.getBoolean("viewportRotateIndividualOriginEnabled", true);
		viewportRotateGroupPivotEnabled = prefs.getBoolean("viewportRotateGroupPivotEnabled", false);
		
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
				// Persist window + panel layout before exit.
				saveWindowLayoutToPrefs();
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
		ProjectCommand toggleGizmoMode = new CommandToggleViewportGizmoMode();
		ProjectCommand frameSelection = new at.vintagestory.modelcreator.input.command.CommandFrameSelection(this);
		
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
		// Toggle viewport gizmo between Move/Rotate
		// Cycles Move -> Rotate -> Scale
		manager.subscribe(new ListenerKeyPressOnce(toggleGizmoMode, Keyboard.KEY_Q));
		// Frame selected element(s) in the viewport (Unity/Unreal-style)
		manager.subscribe(new ListenerKeyPressOnce(frameSelection, Keyboard.KEY_F));
		
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

		// Restore window size/position/state (including maximized) so the UI layout comes back as the user left it.
		boolean restoredBounds = restoreWindowLayoutFromPrefs();
		setVisible(true);
		// Only center on first launch / no saved bounds.
		if (!restoredBounds) {
			setLocationRelativeTo(null);
		}

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
	
	

/**
 * When enabled, "Mirror Mode" automatically applies keyframe edits made to an element
 * to its left/right partner element (based on name matching: same name except L/R).
 */
public static Element FindMirrorPartner(Element source, Project project)
{
    if (source == null || project == null) return null;
    String name = source.getName();
    if (name == null || name.length() == 0) return null;

    // Common full-word patterns
    if (name.contains("Left")) {
        Element e = project.findElement(name.replace("Left", "Right"));
        if (e != null) return e;
    }
    if (name.contains("Right")) {
        Element e = project.findElement(name.replace("Right", "Left"));
        if (e != null) return e;
    }

    // Single-character swap candidates (try swapping one occurrence at a time)
    java.util.LinkedHashSet<String> candidates = new java.util.LinkedHashSet<String>();
    for (int i = 0; i < name.length(); i++) {
        char ch = name.charAt(i);
        if (ch == 'L') candidates.add(name.substring(0, i) + 'R' + name.substring(i + 1));
        else if (ch == 'R') candidates.add(name.substring(0, i) + 'L' + name.substring(i + 1));
        else if (ch == 'l') candidates.add(name.substring(0, i) + 'r' + name.substring(i + 1));
        else if (ch == 'r') candidates.add(name.substring(0, i) + 'l' + name.substring(i + 1));
    }

    for (String cand : candidates) {
        if (cand == null || cand.equals(name)) continue;
        Element e = project.findElement(cand);
        if (e != null) return e;
    }

    return null;
}

public static at.vintagestory.modelcreator.enums.EnumAxis GetMirrorAxis(Project project) {
    // In entity texture mode, left/right mirroring in the editor corresponds to Z (entities face +/-X).
    return (project != null && project.EntityTextureMode) ? at.vintagestory.modelcreator.enums.EnumAxis.Z : at.vintagestory.modelcreator.enums.EnumAxis.X;
}

/**
 * Apply the current keyframe values of {@code sourceKf} to its L/R partner, reflected across the mirror axis.
 * If {@code skipIfContains} includes the partner element, nothing happens (prevents fighting with multi-select edits).
 *
 * @return the partner keyframe element if one was updated, otherwise null
 */
public static AnimFrameElement SyncMirrorKeyframe(AnimFrameElement sourceKf, java.util.Collection<Element> skipIfContains)
{
    if (!mirrorMode) return null;
    if (sourceKf == null || sourceKf.AnimatedElement == null) return null;

    Project project = CurrentAnimProject();
    if (project == null || project.SelectedAnimation == null) return null;

    Element partner = FindMirrorPartner(sourceKf.AnimatedElement, project);
    if (partner == null) return null;
    if (skipIfContains != null && skipIfContains.contains(partner)) return null;

    Animation anim = project.SelectedAnimation;
    int frame = anim.currentFrame;
    AnimFrameElement pkf = anim.GetOrCreateKeyFrameElement(partner, frame);
    if (pkf == null) return null;

    at.vintagestory.modelcreator.enums.EnumAxis axis = GetMirrorAxis(project);

    // Keep channel flags in lockstep
    pkf.PositionSet = sourceKf.PositionSet;
    pkf.RotationSet = sourceKf.RotationSet;
    pkf.StretchSet = sourceKf.StretchSet;
    pkf.OriginSet = sourceKf.OriginSet;
    pkf.RotShortestDistanceX = sourceKf.RotShortestDistanceX;
    pkf.RotShortestDistanceY = sourceKf.RotShortestDistanceY;
    pkf.RotShortestDistanceZ = sourceKf.RotShortestDistanceZ;

    // Position (mirror across axis)
    if (sourceKf.PositionSet) {
        if (axis == at.vintagestory.modelcreator.enums.EnumAxis.X) {
            pkf.setOffsetX(-sourceKf.getOffsetX());
            pkf.setOffsetY(sourceKf.getOffsetY());
            pkf.setOffsetZ(sourceKf.getOffsetZ());
        } else {
            pkf.setOffsetX(sourceKf.getOffsetX());
            pkf.setOffsetY(sourceKf.getOffsetY());
            pkf.setOffsetZ(-sourceKf.getOffsetZ());
        }
    }

    // Rotation (reflection: invert the components that change handedness)
    if (sourceKf.RotationSet) {
        if (axis == at.vintagestory.modelcreator.enums.EnumAxis.X) {
            pkf.setRotationX(sourceKf.getRotationX());
            pkf.setRotationY(-sourceKf.getRotationY());
            pkf.setRotationZ(-sourceKf.getRotationZ());
        } else {
            pkf.setRotationX(-sourceKf.getRotationX());
            pkf.setRotationY(-sourceKf.getRotationY());
            pkf.setRotationZ(sourceKf.getRotationZ());
        }
    }

    // Scale (same values on both sides)
    if (sourceKf.StretchSet) {
        pkf.setStretchX(sourceKf.getStretchX());
        pkf.setStretchY(sourceKf.getStretchY());
        pkf.setStretchZ(sourceKf.getStretchZ());
    }

    // Keyframeable origin offset (mirror along the axis)
    if (sourceKf.OriginSet) {
        if (axis == at.vintagestory.modelcreator.enums.EnumAxis.X) {
            pkf.setOriginX(-sourceKf.getOriginX());
            pkf.setOriginY(sourceKf.getOriginY());
            pkf.setOriginZ(sourceKf.getOriginZ());
        } else {
            pkf.setOriginX(sourceKf.getOriginX());
            pkf.setOriginY(sourceKf.getOriginY());
            pkf.setOriginZ(-sourceKf.getOriginZ());
        }
    }

    // If we turned everything off, allow cleanup like normal.
    if (!pkf.PositionSet && !pkf.RotationSet && !pkf.StretchSet && !pkf.OriginSet
            && !pkf.RotShortestDistanceX && !pkf.RotShortestDistanceY && !pkf.RotShortestDistanceZ) {
        anim.RemoveKeyFramesIfUseless(pkf);
    }

    // Ensure preview updates immediately while editing.
    anim.SetFramesDirty();

    return pkf;
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

	/**
	 * Persist the current window bounds and extended state.
	 * This ensures that things like maximized fullscreen and the user's preferred layout
	 * (along with other per-panel prefs) survive restarts.
	 */
	private void saveWindowLayoutToPrefs()
	{
		try {
			// Save bounds
			java.awt.Rectangle b = getBounds();
			prefs.putInt("windowX", b.x);
			prefs.putInt("windowY", b.y);
			prefs.putInt("windowW", b.width);
			prefs.putInt("windowH", b.height);

			// Save extended state (maximized, etc)
			prefs.putInt("windowExtendedState", getExtendedState());
		} catch (Throwable t) {
			// Ignore prefs issues.
		}
	}

	/**
	 * Restore window bounds/state from prefs.
	 * @return true if saved bounds were found and applied
	 */
	private boolean restoreWindowLayoutFromPrefs()
	{
		try {
			int w = prefs.getInt("windowW", -1);
			int h = prefs.getInt("windowH", -1);
			if (w <= 0 || h <= 0) return false;

			int x = prefs.getInt("windowX", Integer.MIN_VALUE);
			int y = prefs.getInt("windowY", Integer.MIN_VALUE);
			if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE) return false;

			// Apply bounds first.
			setBounds(x, y, w, h);

			// Then extended state (maximized, etc)
			int state = prefs.getInt("windowExtendedState", 0);
			if (state != 0) setExtendedState(state);
			return true;
		} catch (Throwable t) {
			return false;
		}
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
		viewportUiScale = viewScale;
		
		// Create a container for our canvas (a simple JPanel) without a layout
		// so that our canvas would not get affected by the base component's layout.
		JPanel panel = new JPanel(null);
		canvasContainerPanel = panel;
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
		// Let the viewport sidebar receive keyboard events for inline numeric entry.
		try { manager.subscribe(viewportSidebar); } catch (Throwable t) { }


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

	/**
	 * Shows an inline Swing text field over the OpenGL viewport for numeric entry.
	 * Coordinates are in canvas pixel space (same coordinate space as the left viewport sidebar rendering).
	 */
	public static void showViewportInlineNumberEditor(final int canvasPxX, final int canvasPxY, final int canvasPxW, final int canvasPxH,
			final String initialValue, final java.util.function.Consumer<String> onCommit)
	{
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (canvasContainerPanel == null) return;
				viewportInlineNumberCommit = onCommit;

				if (viewportInlineNumberField == null) {
					viewportInlineNumberField = new JTextField();
					viewportInlineNumberField.setVisible(false);
					viewportInlineNumberField.setBorder(BorderFactory.createLineBorder(new Color(190, 190, 210), 1));
					viewportInlineNumberField.setBackground(new Color(55, 55, 62));
					viewportInlineNumberField.setForeground(new Color(235, 235, 245));
					viewportInlineNumberField.setCaretColor(new Color(235, 235, 245));
					viewportInlineNumberField.setFont(new Font("Dialog", Font.PLAIN, 12));
					viewportInlineNumberField.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							commitViewportInlineNumber(false);
						}
					});
					viewportInlineNumberField.addFocusListener(new FocusAdapter() {
						@Override
						public void focusLost(FocusEvent e) {
							commitViewportInlineNumber(false);
						}
					});
					viewportInlineNumberField.addKeyListener(new KeyAdapter() {
						@Override
						public void keyPressed(KeyEvent e) {
							if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
								commitViewportInlineNumber(true);
								e.consume();
							}
						}
					});
					canvasContainerPanel.add(viewportInlineNumberField);
				}

				// Convert canvas pixel coords to panel coords (HiDPI).
				double s = viewportUiScale;
				int x = (int)Math.round(canvasPxX / s);
				int y = (int)Math.round(canvasPxY / s);
				int w = Math.max(20, (int)Math.round(canvasPxW / s));
				int h = Math.max(18, (int)Math.round(canvasPxH / s));

				viewportInlineNumberField.setText(initialValue);
				viewportInlineNumberField.setBounds(x, y, w, h);
				viewportInlineNumberField.setVisible(true);
				viewportInlineNumberField.selectAll();
				viewportInlineNumberField.requestFocusInWindow();
			}
		});
	}

	private static void commitViewportInlineNumber(boolean cancel)
	{
		if (viewportInlineNumberField == null) return;
		if (!viewportInlineNumberField.isVisible()) return;
		String txt = viewportInlineNumberField.getText();
		viewportInlineNumberField.setVisible(false);
		// Return focus back to the GL canvas.
		try { if (canvas != null) canvas.requestFocus(); } catch (Exception ignored) {}

		if (cancel) return;
		java.util.function.Consumer<String> cb = viewportInlineNumberCommit;
		viewportInlineNumberCommit = null;
		if (cb != null) {
			try { cb.accept(txt); } catch (Exception ignored) {}
		}
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

	/**
	 * Unity/Unreal-style "Frame Selection" in the 3D viewport.
	 * Moves the camera so the current selection sits nicely on screen without changing camera rotation.
	 */
	public void frameSelectionInViewport()
	{
		try {
			if (currentProject == null || modelrenderer == null || modelrenderer.camera == null) return;
			Project project = currentProject;
			if (project.SelectedElement == null && (project.SelectedElements == null || project.SelectedElements.size() == 0)) return;

			// Use multi-selection bounds when available.
			java.util.List<Element> elems;
			if (project.SelectedElements != null && project.SelectedElements.size() > 0) {
				elems = project.SelectedElements;
			} else {
				elems = java.util.Collections.singletonList(project.SelectedElement);
			}

			// If in keyframe mode, include animation offsets (same logic as viewport gizmo pivot).
			boolean keyframeTab = (currentRightTab == 2 && project.SelectedAnimation != null && leftKeyframesPanel != null && leftKeyframesPanel.isVisible());
			AnimationFrame curFrame = null;
			if (keyframeTab) {
				int cf = project.SelectedAnimation.currentFrame;
				if (project.SelectedAnimation.allFrames != null && cf >= 0 && cf < project.SelectedAnimation.allFrames.size()) {
					curFrame = project.SelectedAnimation.allFrames.get(cf);
				}
			}

			double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
			double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
			for (Element e : elems) {
				if (e == null) continue;
				double[] b = computeElementRootBoundsForFraming(e, curFrame);
				if (b == null) continue;
				minX = Math.min(minX, b[0]);
				minY = Math.min(minY, b[1]);
				minZ = Math.min(minZ, b[2]);
				maxX = Math.max(maxX, b[3]);
				maxY = Math.max(maxY, b[4]);
				maxZ = Math.max(maxZ, b[5]);
			}
			if (!Double.isFinite(minX) || !Double.isFinite(maxX)) return;

			double cx = (minX + maxX) * 0.5;
			double cy = (minY + maxY) * 0.5;
			double cz = (minZ + maxZ) * 0.5;

				// Important: the renderer shifts the whole model by (-8, 0, -8) before drawing
				// (Minecraft-style 0..16 space centered around X/Z = 8). The bounds above are in
				// project/root space, so apply the same shift so the camera frames the *rendered* position.
				cx -= 8.0;
				cz -= 8.0;

			double dx = (maxX - minX);
			double dy = (maxY - minY);
			double dz = (maxZ - minZ);
			double radius = 0.5 * Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (!Double.isFinite(radius) || radius < 0.001) radius = 0.5;

			int ls = leftSidebarWidth();
			int vw = Math.max(1, canvWidth - ls);
			int vh = Math.max(1, canvHeight);
			double aspect = (double) vw / (double) vh;

			// FOV is fixed at 60 in the renderer.
			double fovy = Math.toRadians(60.0);
			double tanHalfVert = Math.tan(fovy * 0.5);
			double halfHoriz = Math.atan(tanHalfVert * aspect);
			double tanHalfHoriz = Math.tan(halfHoriz);
			double tanMin = Math.min(tanHalfVert, tanHalfHoriz);
			if (tanMin < 1e-6) tanMin = tanHalfVert;

			// Distance required to fit the selection (sphere) into view.
			double dist = radius / tanMin;
			// A little breathing room so it isn't kissing the edges.
			dist = dist * 1.15 + 0.5;
			// Respect near plane.
			double near = 0.3;
			dist = Math.max(dist, near + radius + 0.5);

			double zoff = -dist;

			// Compute camera translation so that R*center + t = (0,0,zoff)
			double rx = Math.toRadians(modelrenderer.camera.getRX());
			double ry = Math.toRadians(modelrenderer.camera.getRY());

			// Apply rotation order: Ry then Rx (matches GL calls: Rx then Ry, applied to vertices in reverse).
			double cosY = Math.cos(ry), sinY = Math.sin(ry);
			double x1 = cx * cosY + cz * sinY;
			double y1 = cy;
			double z1 = -cx * sinY + cz * cosY;

			double cosX = Math.cos(rx), sinX = Math.sin(rx);
			double x2 = x1;
			double y2 = y1 * cosX - z1 * sinX;
			double z2 = y1 * sinX + z1 * cosX;

			float tx = (float) (-x2);
			float ty = (float) (-y2);
			float tz = (float) (-z2 + zoff);

			modelrenderer.camera.setX(tx);
			modelrenderer.camera.setY(ty);
			modelrenderer.camera.setZ(tz);
		} catch (Throwable t) {
			// Never crash the main loop over a convenience feature.
		}
	}

	/**
	 * Computes an element's axis-aligned bounds in ROOT space by applying the full parent->child
	 * transform chain. When a keyframe frame is provided, animation offsets are applied.
	 */
	private double[] computeElementRootBoundsForFraming(Element elem, AnimationFrame curFrame)
	{
		if (elem == null) return null;
		float[] mat = Mat4f.Identity_(new float[16]);
		java.util.List<Element> path = elem.GetParentPath();
		if (path != null) {
			for (Element p : path) {
				applyTransformWithAnimOffsetForFraming(mat, p, curFrame);
			}
		}
		applyTransformWithAnimOffsetForFraming(mat, elem, curFrame);

		double w = elem.getWidth();
		double h = elem.getHeight();
		double d = elem.getDepth();

		double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

		double[][] corners = new double[][] {
			{0, 0, 0}, {w, 0, 0}, {0, h, 0}, {0, 0, d},
			{w, h, 0}, {w, 0, d}, {0, h, d}, {w, h, d}
		};

		for (double[] c : corners) {
			float[] out = Mat4f.MulWithVec4(mat, new float[] { (float)c[0], (float)c[1], (float)c[2], 1f });
			double x = out[0];
			double y = out[1];
			double z = out[2];
			minX = Math.min(minX, x);
			minY = Math.min(minY, y);
			minZ = Math.min(minZ, z);
			maxX = Math.max(maxX, x);
			maxY = Math.max(maxY, y);
			maxZ = Math.max(maxZ, z);
		}

		if (!Double.isFinite(minX) || !Double.isFinite(maxX)) return null;
		return new double[] { minX, minY, minZ, maxX, maxY, maxZ };
	}

	private void applyTransformWithAnimOffsetForFraming(float[] mat, Element elem, AnimationFrame curFrame)
	{
		if (elem == null) return;
		double offX = 0, offY = 0, offZ = 0;
		if (curFrame != null) {
			AnimFrameElement afe = curFrame.GetAnimFrameElementRec(elem);
			if (afe == null) afe = curFrame.GetAnimFrameElementRecByName(elem.getName());
			if (afe != null) {
				offX = afe.getOffsetX();
				offY = afe.getOffsetY();
				offZ = afe.getOffsetZ();
			}
		}

		Mat4f.Translate(mat, mat, new float[] { (float)elem.getOriginX(), (float)elem.getOriginY(), (float)elem.getOriginZ() });
		Mat4f.RotateX(mat, mat, (float)elem.getRotationX() * GameMath.DEG2RAD);
		Mat4f.RotateY(mat, mat, (float)elem.getRotationY() * GameMath.DEG2RAD);
		Mat4f.RotateZ(mat, mat, (float)elem.getRotationZ() * GameMath.DEG2RAD);
		Mat4f.Translate(mat, mat, new float[] { (float)-elem.getOriginX(), (float)-elem.getOriginY(), (float)-elem.getOriginZ() });

		Mat4f.Translate(mat, mat, new float[] { (float)(elem.getStartX() + offX), (float)(elem.getStartY() + offY), (float)(elem.getStartZ() + offZ) });
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

/** When enabled, viewport rotate operations will apply to all currently selected elements instead of only the active element. */
public static boolean viewportGroupRotateEnabled = false;
/** When enabled, multi-rotate uses each element's own origin/pivot (creature-friendly). */
public static boolean viewportRotateIndividualOriginEnabled = true;
/** When enabled, multi-rotate orbits the whole selection around the shared gizmo pivot (also rotates elements). */
public static boolean viewportRotateGroupPivotEnabled = false;
	/**
	 * When enabled, viewport translate operations will not allow moving any selected element
	 * below its Y value at the start of the current drag.
	 */
	public static boolean viewportFloorSnapEnabled = false;


	/** When enabled, movement will snap to fixed increments (grid). */
	public static boolean viewportGridSnapEnabled = false;
	/** When enabled, movement will snap selection vertices to other element vertices while moving. */
	public static boolean viewportVertexSnapEnabled = false;
	/** Grid snap step as quarter-units (step = value/4). Range: 1..80 => 0.25..20. */
	public static int viewportGridSnapStepQ = 4;

	/**
	 * The 3D viewport renders selection using { Project#SelectedElements}. In some workflows the
	 * Swing JTree selection can temporarily get out of sync with that list (for example when the user
	 * performs viewport-driven selection while focus isn't on the hierarchy). For transforms we treat
	 * { SelectedElements} as authoritative whenever it contains elements not present in the tree selection.
	 */
	public static boolean isViewportSelectionOutOfSync(Project project)
	{
		if (project == null || project.SelectedElements == null || project.SelectedElements.size() <= 1) return false;
		if (project.tree == null) return true;
		try {
			java.util.List<Element> treeSel = project.tree.getSelectedElements();
			if (treeSel == null || treeSel.size() == 0) return true;
			java.util.HashSet<Element> treeSet = new java.util.HashSet<Element>(treeSel);
			for (Element e : project.SelectedElements) {
				if (e != null && !treeSet.contains(e)) return true;
			}
			return false;
		} catch (Throwable t) {
			return true;
		}
	}


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

	// Drag-start gizmo pivot in WORLD space (used for snapping).
	private double viewportGizmoDragPivotWorldX, viewportGizmoDragPivotWorldY, viewportGizmoDragPivotWorldZ;

	// Vertex snap cached targets (world-space corners of non-selected elements) and base bounds of the moved group.
	private java.util.ArrayList<double[]> viewportVertexSnapTargets;
	private double[][] viewportVertexSnapBaseCorners;
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

// Mirror Mode (modeling tab) drag support: cached partner elements and their drag-start bases
private java.util.HashMap<Element, Element> viewportGizmoMirrorPartners;
private java.util.HashMap<Element, Double> viewportGizmoMirrorBaseX;
private java.util.HashMap<Element, Double> viewportGizmoMirrorBaseY;
private java.util.HashMap<Element, Double> viewportGizmoMirrorBaseZ;


// Viewport rotate gizmo drag state (gimbal rings)
private boolean viewportRotateDragActive;
private int viewportRotateDragAxis; // 0 none, 1 X, 2 Y, 3 Z
private boolean viewportRotateSkipSelection;
private double viewportRotatePivotX, viewportRotatePivotY, viewportRotatePivotZ;
private double viewportRotateStartVx, viewportRotateStartVy, viewportRotateStartVz;
private java.util.List<Element> viewportRotateElems;
private java.util.HashMap<Element, Double> viewportRotateBaseRotX;
private java.util.HashMap<Element, Double> viewportRotateBaseRotY;
private java.util.HashMap<Element, Double> viewportRotateBaseRotZ;
private java.util.HashMap<Element, Double> viewportRotateBaseStartX;
private java.util.HashMap<Element, Double> viewportRotateBaseStartY;
private java.util.HashMap<Element, Double> viewportRotateBaseStartZ;
private java.util.HashMap<Element, double[]> viewportRotateWorldAxisToLocal;
// Keyframe (animation tab) rotate drag support
private boolean viewportRotateDragInKeyframeMode;
private int viewportRotateDragAnimVersion;
private java.util.HashMap<Element, AnimFrameElement> viewportRotateKeyframeElems;
private java.util.HashMap<Element, Double> viewportRotateBaseKfRotX;
private java.util.HashMap<Element, Double> viewportRotateBaseKfRotY;
private java.util.HashMap<Element, Double> viewportRotateBaseKfRotZ;
private java.util.HashMap<Element, Double> viewportRotateBaseKfPosX;
private java.util.HashMap<Element, Double> viewportRotateBaseKfPosY;
private java.util.HashMap<Element, Double> viewportRotateBaseKfPosZ;
private java.util.HashMap<Element, double[]> viewportRotateBaseCenterWorld;


// Viewport scale gizmo drag state (Scale handles)
private boolean viewportScaleDragActive;
private int viewportScaleDragMode; // 0 none, 1 X, 2 Y, 3 Z, 4 UNIFORM
private boolean viewportScaleSkipSelection;
private double viewportScaleAxisDirX;
private double viewportScaleAxisDirY;
// For world-axis mode, use the same ray-to-axis constraint as Move to make scaling stable regardless of zoom.
private boolean viewportScaleUseRayConstraint;
private double viewportScaleAxisOriginX, viewportScaleAxisOriginY, viewportScaleAxisOriginZ;
private double viewportScaleAxisWorldDirX, viewportScaleAxisWorldDirY, viewportScaleAxisWorldDirZ;
private double viewportScaleAxisParamStart;
private java.util.List<Element> viewportScaleElems;
// Modeling tab (resize) bases
private java.util.HashMap<Element, Double> viewportScaleBaseStartX;
private java.util.HashMap<Element, Double> viewportScaleBaseStartY;
private java.util.HashMap<Element, Double> viewportScaleBaseStartZ;
private java.util.HashMap<Element, Double> viewportScaleBaseW;
private java.util.HashMap<Element, Double> viewportScaleBaseH;
private java.util.HashMap<Element, Double> viewportScaleBaseD;
// Keyframe (animation tab) stretch bases
private boolean viewportScaleDragInKeyframeMode;
private int viewportScaleDragAnimVersion;
private java.util.HashMap<Element, AnimFrameElement> viewportScaleKeyframeElems;
private java.util.HashMap<Element, Double> viewportScaleBaseKfSX;
private java.util.HashMap<Element, Double> viewportScaleBaseKfSY;
private java.util.HashMap<Element, Double> viewportScaleBaseKfSZ;
// We may need to adjust offsets while scaling in animation mode (e.g. floor guard),
// so cache the drag-start offsets as well.
private java.util.HashMap<Element, Double> viewportScaleBaseKfPosX;
private java.util.HashMap<Element, Double> viewportScaleBaseKfPosY;
private java.util.HashMap<Element, Double> viewportScaleBaseKfPosZ;

// Mirror Mode (modeling) scale drag support
private java.util.HashMap<Element, Element> viewportScaleMirrorPartners;
private java.util.HashMap<Element, Double> viewportScaleMirrorBaseStartX;
private java.util.HashMap<Element, Double> viewportScaleMirrorBaseStartY;
private java.util.HashMap<Element, Double> viewportScaleMirrorBaseStartZ;
private java.util.HashMap<Element, Double> viewportScaleMirrorBaseW;
private java.util.HashMap<Element, Double> viewportScaleMirrorBaseH;
private java.util.HashMap<Element, Double> viewportScaleMirrorBaseD;


// Mirror Mode (modeling tab) rotate drag support
private java.util.HashMap<Element, Element> viewportRotateMirrorPartners;
private java.util.HashMap<Element, Double> viewportRotateMirrorBaseRotX;
private java.util.HashMap<Element, Double> viewportRotateMirrorBaseRotY;
private java.util.HashMap<Element, Double> viewportRotateMirrorBaseRotZ;
private java.util.HashMap<Element, Double> viewportRotateMirrorBaseStartX;
private java.util.HashMap<Element, Double> viewportRotateMirrorBaseStartY;
private java.util.HashMap<Element, Double> viewportRotateMirrorBaseStartZ;

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

		private void tryStartViewportRotateGizmoDrag()
{
	if (viewportRotateDragActive || currentProject == null) return;
	Element selected = currentProject.SelectedElement;
	if (selected == null) return;

	int axis = modelrenderer.hitTestViewportRotateGizmo(mouseDownX, mouseDownY);
	if (axis == 0) return;

	viewportRotateDragAxis = axis;
	viewportRotateDragActive = true;
	viewportRotateSkipSelection = true;
	grabbedElem = selected;

	viewportRotatePivotX = modelrenderer.viewportMoveGizmoWorldX;
	viewportRotatePivotY = modelrenderer.viewportMoveGizmoWorldY;
	viewportRotatePivotZ = modelrenderer.viewportMoveGizmoWorldZ;

	boolean effectiveGroupRotate = viewportGroupRotateEnabled || isViewportSelectionOutOfSync(currentProject);

	// When rotating multiple selected elements by *individual origins*, rotate *all* selected elements
	// (including children). This allows parent+child rotations to accumulate (e.g. tail chains).
	// For group-pivot/orbit behavior we keep roots-only selection to avoid double-applying transforms.
	if (effectiveGroupRotate
			&& viewportRotateIndividualOriginEnabled
			&& currentProject.SelectedElements != null
			&& currentProject.SelectedElements.size() > 1)
	{
		viewportRotateElems = new java.util.ArrayList<Element>(currentProject.SelectedElements);
	}
	else
	{
		viewportRotateElems = getSelectedRootElementsForMove(selected, effectiveGroupRotate);
	}

	// Keyframe tab: rotate should animate when in Animation tab, not edit the base model.
	viewportRotateDragInKeyframeMode = (ModelCreator.currentRightTab == 2 && currentProject != null && currentProject.SelectedAnimation != null);
	viewportRotateDragAnimVersion = viewportRotateDragInKeyframeMode ? currentProject.CurrentAnimVersion() : 0;
	viewportRotateKeyframeElems = viewportRotateDragInKeyframeMode ? new java.util.HashMap<Element, AnimFrameElement>() : null;

	if (viewportRotateDragInKeyframeMode && currentProject.tree != null && viewportRotateElems != null && viewportRotateElems.size() > 0) {
		try {
			java.util.HashSet<Element> rootSet = new java.util.HashSet<Element>(viewportRotateElems);
			Element driver = selected;
			Element p = selected;
			while (p != null && !rootSet.contains(p)) {
				p = p.ParentElement;
			}
			if (p != null) driver = p;
			currentProject.tree.setLeadSelectionElement(driver, true);
		} catch (Throwable t) {
			// ignore selection/UI issues
		}
	}

	AnimationFrame curAnimFrame = null;
	if (viewportRotateDragInKeyframeMode && currentProject.SelectedAnimation != null) {
		int cf = currentProject.SelectedAnimation.currentFrame;
		if (currentProject.SelectedAnimation.allFrames != null && cf >= 0 && cf < currentProject.SelectedAnimation.allFrames.size()) {
			curAnimFrame = currentProject.SelectedAnimation.allFrames.get(cf);
		}
	}

	viewportRotateBaseRotX = new java.util.HashMap<Element, Double>();
	viewportRotateBaseRotY = new java.util.HashMap<Element, Double>();
	viewportRotateBaseRotZ = new java.util.HashMap<Element, Double>();
	viewportRotateBaseStartX = new java.util.HashMap<Element, Double>();
	viewportRotateBaseStartY = new java.util.HashMap<Element, Double>();
	viewportRotateBaseStartZ = new java.util.HashMap<Element, Double>();
	viewportRotateWorldAxisToLocal = new java.util.HashMap<Element, double[]>();
	viewportRotateBaseCenterWorld = new java.util.HashMap<Element, double[]>();

	viewportRotateBaseKfRotX = viewportRotateDragInKeyframeMode ? new java.util.HashMap<Element, Double>() : null;
	viewportRotateBaseKfRotY = viewportRotateDragInKeyframeMode ? new java.util.HashMap<Element, Double>() : null;
	viewportRotateBaseKfRotZ = viewportRotateDragInKeyframeMode ? new java.util.HashMap<Element, Double>() : null;
	viewportRotateBaseKfPosX = viewportRotateDragInKeyframeMode ? new java.util.HashMap<Element, Double>() : null;
	viewportRotateBaseKfPosY = viewportRotateDragInKeyframeMode ? new java.util.HashMap<Element, Double>() : null;
	viewportRotateBaseKfPosZ = viewportRotateDragInKeyframeMode ? new java.util.HashMap<Element, Double>() : null;

	for (Element e : viewportRotateElems) {
		if (e == null) continue;

		viewportRotateBaseRotX.put(e, e.getRotationX());
		viewportRotateBaseRotY.put(e, e.getRotationY());
		viewportRotateBaseRotZ.put(e, e.getRotationZ());
		viewportRotateBaseStartX.put(e, e.getStartX());
		viewportRotateBaseStartY.put(e, e.getStartY());
		viewportRotateBaseStartZ.put(e, e.getStartZ());

		double kfOffX = 0, kfOffY = 0, kfOffZ = 0;
		if (viewportRotateDragInKeyframeMode && currentProject.SelectedAnimation != null) {
			AnimFrameElement prevKfRot = currentProject.SelectedAnimation.GetKeyFrameElement(e, currentProject.SelectedAnimation.currentFrame);
			boolean prevRotSet = prevKfRot != null && prevKfRot.RotationSet;

			AnimFrameElement kfRot = currentProject.SelectedAnimation.ToggleRotation(e, true);
			// If we just enabled rotation on this element, seed the keyframe with the current (lerped) frame values
			// so the pose doesn't snap back to the base model on first edit.
			if (!prevRotSet && curAnimFrame != null && kfRot != null) {
				AnimFrameElement frameElem = curAnimFrame.GetAnimFrameElementRec(e);
				if (frameElem != null) {
					kfRot.setRotationX(frameElem.getRotationX());
					kfRot.setRotationY(frameElem.getRotationY());
					kfRot.setRotationZ(frameElem.getRotationZ());
				}
			}
			if (kfRot != null) {
				viewportRotateKeyframeElems.put(e, kfRot);
				viewportRotateBaseKfRotX.put(e, kfRot.getRotationX());
				viewportRotateBaseKfRotY.put(e, kfRot.getRotationY());
				viewportRotateBaseKfRotZ.put(e, kfRot.getRotationZ());
			} else {
				viewportRotateBaseKfRotX.put(e, 0d);
				viewportRotateBaseKfRotY.put(e, 0d);
				viewportRotateBaseKfRotZ.put(e, 0d);
			}

			if (viewportRotateGroupPivotEnabled) {
				AnimFrameElement kfPos = currentProject.SelectedAnimation.TogglePosition(e, true);
				if (kfPos != null) {
					viewportRotateBaseKfPosX.put(e, kfPos.getOffsetX());
					viewportRotateBaseKfPosY.put(e, kfPos.getOffsetY());
					viewportRotateBaseKfPosZ.put(e, kfPos.getOffsetZ());
					kfOffX = kfPos.getOffsetX();
					kfOffY = kfPos.getOffsetY();
					kfOffZ = kfPos.getOffsetZ();
				} else {
					viewportRotateBaseKfPosX.put(e, 0d);
					viewportRotateBaseKfPosY.put(e, 0d);
					viewportRotateBaseKfPosZ.put(e, 0d);
				}
			}

			viewportRotateWorldAxisToLocal.put(e, computeWorldAxisToLocalMapAnimated(e, curAnimFrame, viewportRotateDragAnimVersion));
		} else {
			viewportRotateWorldAxisToLocal.put(e, computeWorldAxisToLocalMap(e));
		}

		// Reference point for group-pivot orbit: center of the cuboid box in world/model coords
		double cx = e.getStartX() + kfOffX + e.getWidth() / 2.0;
		double cy = e.getStartY() + kfOffY + e.getHeight() / 2.0;
		double cz = e.getStartZ() + kfOffZ + e.getDepth() / 2.0;
		viewportRotateBaseCenterWorld.put(e, new double[] { cx, cy, cz });
	}


	// Mirror Mode (modeling): cache partner drag-start bases so we can mirror rotation/translation
	viewportRotateMirrorPartners = null;
	viewportRotateMirrorBaseRotX = null;
	viewportRotateMirrorBaseRotY = null;
	viewportRotateMirrorBaseRotZ = null;
	viewportRotateMirrorBaseStartX = null;
	viewportRotateMirrorBaseStartY = null;
	viewportRotateMirrorBaseStartZ = null;
	if (!viewportRotateDragInKeyframeMode && ModelCreator.mirrorMode && viewportRotateElems != null && viewportRotateElems.size() > 0) {
		java.util.HashSet<Element> selSet = new java.util.HashSet<Element>(viewportRotateElems);
		viewportRotateMirrorPartners = new java.util.HashMap<Element, Element>();
		viewportRotateMirrorBaseRotX = new java.util.HashMap<Element, Double>();
		viewportRotateMirrorBaseRotY = new java.util.HashMap<Element, Double>();
		viewportRotateMirrorBaseRotZ = new java.util.HashMap<Element, Double>();
		viewportRotateMirrorBaseStartX = new java.util.HashMap<Element, Double>();
		viewportRotateMirrorBaseStartY = new java.util.HashMap<Element, Double>();
		viewportRotateMirrorBaseStartZ = new java.util.HashMap<Element, Double>();
		for (Element e : viewportRotateElems) {
			Element partner = FindMirrorPartner(e, currentProject);
			if (partner != null && !selSet.contains(partner)) {
				viewportRotateMirrorPartners.put(e, partner);
				viewportRotateMirrorBaseRotX.put(partner, partner.getRotationX());
				viewportRotateMirrorBaseRotY.put(partner, partner.getRotationY());
				viewportRotateMirrorBaseRotZ.put(partner, partner.getRotationZ());
				viewportRotateMirrorBaseStartX.put(partner, partner.getStartX());
				viewportRotateMirrorBaseStartY.put(partner, partner.getStartY());
				viewportRotateMirrorBaseStartZ.put(partner, partner.getStartZ());
			}
		}
	}

	// Compute the starting direction vector on the rotation plane from mouse ray intersection
	double[] ray = modelrenderer.getMouseRayWorld(mouseDownX, mouseDownY);
	if (ray == null) {
		viewportRotateDragActive = false;
		viewportRotateSkipSelection = false;
		return;
	}
	double r0x = ray[0], r0y = ray[1], r0z = ray[2];
	double rdx = ray[3], rdy = ray[4], rdz = ray[5];

	double nx = 0, ny = 0, nz = 0;
	if (axis == 1) nx = 1;
	if (axis == 2) ny = 1;
	if (axis == 3) nz = 1;

	double denom = nx*rdx + ny*rdy + nz*rdz;
	if (Math.abs(denom) < 1e-9) {
		viewportRotateDragActive = false;
		viewportRotateSkipSelection = false;
		return;
	}

	double t = (nx*(viewportRotatePivotX - r0x) + ny*(viewportRotatePivotY - r0y) + nz*(viewportRotatePivotZ - r0z)) / denom;
	double ix = r0x + rdx * t;
	double iy = r0y + rdy * t;
	double iz = r0z + rdz * t;

	double vx = ix - viewportRotatePivotX;
	double vy = iy - viewportRotatePivotY;
	double vz = iz - viewportRotatePivotZ;

	// Normalize (and ensure on plane)
	double dot = vx*nx + vy*ny + vz*nz;
	vx -= dot * nx;
	vy -= dot * ny;
	vz -= dot * nz;

	double vlen = Math.sqrt(vx*vx + vy*vy + vz*vz);
	if (!Double.isFinite(vlen) || vlen < 1e-9) {
		viewportRotateStartVx = 0;
		viewportRotateStartVy = 0;
		viewportRotateStartVz = 0;
	} else {
		viewportRotateStartVx = vx / vlen;
		viewportRotateStartVy = vy / vlen;
		viewportRotateStartVz = vz / vlen;
	}
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

			// Cache the drag-start gizmo pivot (world space) for snapping.
			viewportGizmoDragPivotWorldX = modelrenderer.viewportMoveGizmoWorldX;
			viewportGizmoDragPivotWorldY = modelrenderer.viewportMoveGizmoWorldY;
			viewportGizmoDragPivotWorldZ = modelrenderer.viewportMoveGizmoWorldZ;


			boolean effectiveGroupMove = viewportGroupMoveEnabled || isViewportSelectionOutOfSync(currentProject);
			viewportGizmoElems = getSelectedRootElementsForMove(selected, effectiveGroupMove);

			// In Keyframe tab, viewport translate should animate (write keyframe position offsets), not edit the base model.
			viewportGizmoDragInKeyframeMode = (ModelCreator.currentRightTab == 2 && currentProject != null && currentProject.SelectedAnimation != null);
			viewportGizmoDragAnimVersion = viewportGizmoDragInKeyframeMode ? currentProject.CurrentAnimVersion() : 0;
			viewportGizmoKeyframeElems = viewportGizmoDragInKeyframeMode ? new java.util.HashMap<Element, AnimFrameElement>() : null;
				if (viewportGizmoDragInKeyframeMode && currentProject.tree != null && viewportGizmoElems != null && viewportGizmoElems.size() > 0) {
					try {
						java.util.HashSet<Element> rootSet = new java.util.HashSet<Element>(viewportGizmoElems);
						Element driver = selected;
						Element p = selected;
						while (p != null && !rootSet.contains(p)) {
							p = p.ParentElement;
						}
						if (p != null) driver = p;
						currentProject.tree.setLeadSelectionElement(driver, true);
					} catch (Throwable t) {
						// ignore selection/UI issues; movement should still work
					}
				}

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

			// Mirror Mode (modeling): cache partner drag-start bases so we can mirror movement
			viewportGizmoMirrorPartners = null;
			viewportGizmoMirrorBaseX = null;
			viewportGizmoMirrorBaseY = null;
			viewportGizmoMirrorBaseZ = null;
			if (!viewportGizmoDragInKeyframeMode && ModelCreator.mirrorMode && viewportGizmoElems != null && viewportGizmoElems.size() > 0) {
				java.util.HashSet<Element> selSet = new java.util.HashSet<Element>(viewportGizmoElems);
				viewportGizmoMirrorPartners = new java.util.HashMap<Element, Element>();
				viewportGizmoMirrorBaseX = new java.util.HashMap<Element, Double>();
				viewportGizmoMirrorBaseY = new java.util.HashMap<Element, Double>();
				viewportGizmoMirrorBaseZ = new java.util.HashMap<Element, Double>();
				for (Element e : viewportGizmoElems) {
					Element partner = FindMirrorPartner(e, currentProject);
					if (partner != null && !selSet.contains(partner)) {
						viewportGizmoMirrorPartners.put(e, partner);
						viewportGizmoMirrorBaseX.put(partner, partner.getStartX());
						viewportGizmoMirrorBaseY.put(partner, partner.getStartY());
						viewportGizmoMirrorBaseZ.put(partner, partner.getStartZ());
					}
				}
			}

			// Precompute vertex snap targets (world-space corners) and base corners of the moved group.
			viewportVertexSnapTargets = null;
			viewportVertexSnapBaseCorners = null;
			if (ModelCreator.viewportVertexSnapEnabled && currentProject != null) {
				try {
					viewportVertexSnapBaseCorners = computeMovedGroupBaseCorners(viewportGizmoElems, curAnimFrame, viewportGizmoDragInKeyframeMode, viewportGizmoKeyframeElems);
					viewportVertexSnapTargets = computeVertexSnapTargets(currentProject, viewportGizmoElems, curAnimFrame, viewportGizmoDragInKeyframeMode, viewportGizmoKeyframeElems);
				} catch (Throwable t) {
					viewportVertexSnapTargets = null;
					viewportVertexSnapBaseCorners = null;
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

		private void tryStartViewportScaleGizmoDrag()
		{
			if (viewportScaleDragActive || currentProject == null) return;
			Element selected = currentProject.SelectedElement;
			if (selected == null) return;

			int mode = modelrenderer.hitTestViewportScaleGizmo(mouseDownX, mouseDownY);
			if (mode == 0) return;

			viewportScaleDragMode = mode;
			viewportScaleDragActive = true;
			viewportScaleSkipSelection = true;
			grabbedElem = selected;

			// Cache a stable screen-space axis direction to interpret mouse delta.
			double[] dir = modelrenderer.getViewportMoveGizmoAxisDirWindow(Math.max(1, Math.min(3, mode)));
			viewportScaleAxisDirX = dir != null ? dir[0] : 0;
			viewportScaleAxisDirY = dir != null ? dir[1] : 0;
			if (mode == 4) {
				// Uniform: use a diagonal drag gesture (right and up grows, left and down shrinks)
				viewportScaleAxisDirX = 0.70710678118;
				viewportScaleAxisDirY = 0.70710678118;
			}

			// In world-axis mode, use a precise ray-to-axis constraint so scaling feels stable regardless of zoom.
			viewportScaleUseRayConstraint = false;
			if (!viewportFreedomModeEnabled && (mode == 1 || mode == 2 || mode == 3)) {
				viewportScaleAxisOriginX = modelrenderer.viewportMoveGizmoWorldX;
				viewportScaleAxisOriginY = modelrenderer.viewportMoveGizmoWorldY;
				viewportScaleAxisOriginZ = modelrenderer.viewportMoveGizmoWorldZ;
				switch (mode) {
					case 1: viewportScaleAxisWorldDirX = 1; viewportScaleAxisWorldDirY = 0; viewportScaleAxisWorldDirZ = 0; break;
					case 2: viewportScaleAxisWorldDirX = 0; viewportScaleAxisWorldDirY = 1; viewportScaleAxisWorldDirZ = 0; break;
					case 3: viewportScaleAxisWorldDirX = 0; viewportScaleAxisWorldDirY = 0; viewportScaleAxisWorldDirZ = 1; break;
				}
				// NOTE: this is the Scale gizmo, so use the Scale axis (not the Move axis).
				Double t0 = computeScaleAxisParamFromMouse(mouseDownX, mouseDownY);
				if (t0 != null && Double.isFinite(t0)) {
					viewportScaleAxisParamStart = t0;
					viewportScaleUseRayConstraint = true;
				}
			}

			// Keyframe tab: scale should animate (stretch) when in Animation tab, not resize the base model.
			viewportScaleDragInKeyframeMode = (ModelCreator.currentRightTab == 2 && currentProject != null && currentProject.SelectedAnimation != null);
			viewportScaleDragAnimVersion = viewportScaleDragInKeyframeMode ? currentProject.CurrentAnimVersion() : 0;

			boolean effectiveGroup = viewportGroupMoveEnabled || isViewportSelectionOutOfSync(currentProject);

			// Modeling tab: if multiple elements are selected, resize all of them together.
			if (!viewportScaleDragInKeyframeMode && currentProject.SelectedElements != null && currentProject.SelectedElements.size() > 1) {
				viewportScaleElems = new java.util.ArrayList<Element>(currentProject.SelectedElements);
			} else {
				viewportScaleElems = getSelectedRootElementsForMove(selected, effectiveGroup);
			}
			viewportScaleKeyframeElems = viewportScaleDragInKeyframeMode ? new java.util.HashMap<Element, AnimFrameElement>() : null;
			viewportScaleBaseKfSX = viewportScaleDragInKeyframeMode ? new java.util.HashMap<Element, Double>() : null;
			viewportScaleBaseKfSY = viewportScaleDragInKeyframeMode ? new java.util.HashMap<Element, Double>() : null;
			viewportScaleBaseKfSZ = viewportScaleDragInKeyframeMode ? new java.util.HashMap<Element, Double>() : null;
			viewportScaleBaseKfPosX = viewportScaleDragInKeyframeMode ? new java.util.HashMap<Element, Double>() : null;
			viewportScaleBaseKfPosY = viewportScaleDragInKeyframeMode ? new java.util.HashMap<Element, Double>() : null;
			viewportScaleBaseKfPosZ = viewportScaleDragInKeyframeMode ? new java.util.HashMap<Element, Double>() : null;

			AnimationFrame curAnimFrame = null;
			if (viewportScaleDragInKeyframeMode && currentProject.SelectedAnimation != null) {
				int cf = currentProject.SelectedAnimation.currentFrame;
				if (currentProject.SelectedAnimation.allFrames != null && cf >= 0 && cf < currentProject.SelectedAnimation.allFrames.size()) {
					curAnimFrame = currentProject.SelectedAnimation.allFrames.get(cf);
				}
			}

			viewportScaleBaseStartX = new java.util.HashMap<Element, Double>();
			viewportScaleBaseStartY = new java.util.HashMap<Element, Double>();
			viewportScaleBaseStartZ = new java.util.HashMap<Element, Double>();
			viewportScaleBaseW = new java.util.HashMap<Element, Double>();
			viewportScaleBaseH = new java.util.HashMap<Element, Double>();
			viewportScaleBaseD = new java.util.HashMap<Element, Double>();

			for (Element e : viewportScaleElems) {
				if (e == null) continue;
				viewportScaleBaseStartX.put(e, e.getStartX());
				viewportScaleBaseStartY.put(e, e.getStartY());
				viewportScaleBaseStartZ.put(e, e.getStartZ());
				viewportScaleBaseW.put(e, e.getWidth());
				viewportScaleBaseH.put(e, e.getHeight());
				viewportScaleBaseD.put(e, e.getDepth());

				if (viewportScaleDragInKeyframeMode && currentProject.SelectedAnimation != null) {
					AnimFrameElement prevKf = currentProject.SelectedAnimation.GetKeyFrameElement(e, currentProject.SelectedAnimation.currentFrame);
					boolean prevStretchSet = prevKf != null && prevKf.StretchSet;

					AnimFrameElement kf = currentProject.SelectedAnimation.ToggleStretch(e, true);
					AnimFrameElement frameElem = (curAnimFrame != null) ? curAnimFrame.GetAnimFrameElementRec(e) : null;

					// If we just enabled Stretch, seed it from the current frame so there is no pose snap.
					if (!prevStretchSet && frameElem != null && kf != null) {
						kf.setStretchX(frameElem.getStretchX());
						kf.setStretchY(frameElem.getStretchY());
						kf.setStretchZ(frameElem.getStretchZ());
					}

					// Cache drag-start offsets too (needed for floor guard when stretching Y).
					// If the keyframe doesn't have PositionSet, fall back to the evaluated current frame offsets.
					double baseOffX = (prevKf != null && prevKf.PositionSet) ? prevKf.getOffsetX() : (frameElem != null ? frameElem.getOffsetX() : 0d);
					double baseOffY = (prevKf != null && prevKf.PositionSet) ? prevKf.getOffsetY() : (frameElem != null ? frameElem.getOffsetY() : 0d);
					double baseOffZ = (prevKf != null && prevKf.PositionSet) ? prevKf.getOffsetZ() : (frameElem != null ? frameElem.getOffsetZ() : 0d);
					if (viewportScaleBaseKfPosX != null) viewportScaleBaseKfPosX.put(e, baseOffX);
					if (viewportScaleBaseKfPosY != null) viewportScaleBaseKfPosY.put(e, baseOffY);
					if (viewportScaleBaseKfPosZ != null) viewportScaleBaseKfPosZ.put(e, baseOffZ);

					if (kf != null) {
						viewportScaleKeyframeElems.put(e, kf);
						viewportScaleBaseKfSX.put(e, kf.getStretchX());
						viewportScaleBaseKfSY.put(e, kf.getStretchY());
						viewportScaleBaseKfSZ.put(e, kf.getStretchZ());
					} else {
						viewportScaleBaseKfSX.put(e, 1d);
						viewportScaleBaseKfSY.put(e, 1d);
						viewportScaleBaseKfSZ.put(e, 1d);
						if (viewportScaleBaseKfPosX != null) viewportScaleBaseKfPosX.put(e, baseOffX);
						if (viewportScaleBaseKfPosY != null) viewportScaleBaseKfPosY.put(e, baseOffY);
						if (viewportScaleBaseKfPosZ != null) viewportScaleBaseKfPosZ.put(e, baseOffZ);
					}
				}
			}

			// Mirror Mode (modeling): cache partner drag-start bases so we can mirror resize changes.
			viewportScaleMirrorPartners = null;
			viewportScaleMirrorBaseStartX = null;
			viewportScaleMirrorBaseStartY = null;
			viewportScaleMirrorBaseStartZ = null;
			viewportScaleMirrorBaseW = null;
			viewportScaleMirrorBaseH = null;
			viewportScaleMirrorBaseD = null;
			if (!viewportScaleDragInKeyframeMode && ModelCreator.mirrorMode && viewportScaleElems != null && viewportScaleElems.size() > 0) {
				java.util.HashSet<Element> selSet = new java.util.HashSet<Element>(viewportScaleElems);
				viewportScaleMirrorPartners = new java.util.HashMap<Element, Element>();
				viewportScaleMirrorBaseStartX = new java.util.HashMap<Element, Double>();
				viewportScaleMirrorBaseStartY = new java.util.HashMap<Element, Double>();
				viewportScaleMirrorBaseStartZ = new java.util.HashMap<Element, Double>();
				viewportScaleMirrorBaseW = new java.util.HashMap<Element, Double>();
				viewportScaleMirrorBaseH = new java.util.HashMap<Element, Double>();
				viewportScaleMirrorBaseD = new java.util.HashMap<Element, Double>();
				for (Element e : viewportScaleElems) {
					Element partner = FindMirrorPartner(e, currentProject);
					if (partner != null && !selSet.contains(partner)) {
						viewportScaleMirrorPartners.put(e, partner);
						viewportScaleMirrorBaseStartX.put(partner, partner.getStartX());
						viewportScaleMirrorBaseStartY.put(partner, partner.getStartY());
						viewportScaleMirrorBaseStartZ.put(partner, partner.getStartZ());
						viewportScaleMirrorBaseW.put(partner, partner.getWidth());
						viewportScaleMirrorBaseH.put(partner, partner.getHeight());
						viewportScaleMirrorBaseD.put(partner, partner.getDepth());
					}
				}
			}
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

		/** Same as {@link #computeAxisParamFromMouse(int, int)} but for the Scale gizmo. */
		private Double computeScaleAxisParamFromMouse(int mouseX, int mouseY)
		{
			double[] ray = modelrenderer.getMouseRayWorld(mouseX, mouseY);
			if (ray == null) return null;
			return closestParamRayToAxis(
				ray[0], ray[1], ray[2],
				ray[3], ray[4], ray[5],
				viewportScaleAxisOriginX, viewportScaleAxisOriginY, viewportScaleAxisOriginZ,
				viewportScaleAxisWorldDirX, viewportScaleAxisWorldDirY, viewportScaleAxisWorldDirZ
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

	// ------------------------
	// Viewport snapping helpers
	// ------------------------

	private static double getViewportGridSnapStep()
	{
		double step = ModelCreator.viewportGridSnapStepQ / 4.0;
		if (!Double.isFinite(step) || step <= 0) step = 1.0;
		return Math.max(0.25, step);
	}

	/**
	 * Applies grid snap and/or vertex snap to a desired WORLD delta, relative to the drag-start gizmo pivot.
	 */
	private double[] applyViewportMoveSnapping(double dwx, double dwy, double dwz, boolean allowX, boolean allowY, boolean allowZ)
	{
		double sx = dwx, sy = dwy, sz = dwz;

		if (ModelCreator.viewportGridSnapEnabled) {
			double step = getViewportGridSnapStep();
			if (allowX) {
				double target = viewportGizmoDragPivotWorldX + sx;
				double snapped = Math.round(target / step) * step;
				sx = snapped - viewportGizmoDragPivotWorldX;
			}
			if (allowY) {
				double target = viewportGizmoDragPivotWorldY + sy;
				double snapped = Math.round(target / step) * step;
				sy = snapped - viewportGizmoDragPivotWorldY;
			}
			if (allowZ) {
				double target = viewportGizmoDragPivotWorldZ + sz;
				double snapped = Math.round(target / step) * step;
				sz = snapped - viewportGizmoDragPivotWorldZ;
			}
		}

		if (ModelCreator.viewportVertexSnapEnabled && viewportVertexSnapTargets != null && viewportVertexSnapBaseCorners != null && viewportVertexSnapTargets.size() > 0) {
			// Vertex snapping needs to feel "magnetic" rather than "pixel-perfect".
			// In ModelCreator space (0..16 per block), ~1 unit is a nice, usable radius.
			double baseThresh = 1.0;
			if (ModelCreator.viewportGridSnapEnabled) baseThresh = Math.max(baseThresh, getViewportGridSnapStep() * 1.25);
			double thresh2 = baseThresh * baseThresh;

			double bestD2 = Double.POSITIVE_INFINITY;
			double bestOffX = 0, bestOffY = 0, bestOffZ = 0;

			for (int i = 0; i < viewportVertexSnapBaseCorners.length; i++) {
				double[] bc = viewportVertexSnapBaseCorners[i];
				if (bc == null || bc.length < 3) continue;
				double mx = bc[0] + sx;
				double my = bc[1] + sy;
				double mz = bc[2] + sz;

				for (int j = 0; j < viewportVertexSnapTargets.size(); j++) {
					double[] tv = viewportVertexSnapTargets.get(j);
					if (tv == null || tv.length < 3) continue;
					double dx = tv[0] - mx;
					double dy = tv[1] - my;
					double dz = tv[2] - mz;
					double d2 = 0;
					if (allowX) d2 += dx*dx;
					if (allowY) d2 += dy*dy;
					if (allowZ) d2 += dz*dz;
					if (!allowX && !allowY && !allowZ) d2 = dx*dx + dy*dy + dz*dz;
					if (d2 < bestD2) {
						bestD2 = d2;
						bestOffX = dx;
						bestOffY = dy;
						bestOffZ = dz;
					}
				}
			}

			if (bestD2 <= thresh2) {
				if (!allowX) bestOffX = 0;
				if (!allowY) bestOffY = 0;
				if (!allowZ) bestOffZ = 0;
				sx += bestOffX;
				sy += bestOffY;
				sz += bestOffZ;
			}
		}

		return new double[] { sx, sy, sz };
	}

		private static void collectDescendants(Element root, java.util.HashSet<Element> out)
		{
			if (root == null || out == null) return;
			java.util.ArrayDeque<Element> st = new java.util.ArrayDeque<Element>();
			st.push(root);
			while (!st.isEmpty()) {
				Element e = st.pop();
				if (e == null) continue;
				if (!out.add(e)) continue;
				if (e.ChildElements != null) {
					for (int i = 0; i < e.ChildElements.size(); i++) {
						st.push(e.ChildElements.get(i));
					}
				}
			}
		}

		/** Returns true if {@code candidate} is {@code root} or a descendant of {@code root}. */
		private static boolean isInSubtree(Element root, Element candidate)
		{
			if (root == null || candidate == null) return false;
			Element p = candidate;
			while (p != null) {
				if (p == root) return true;
				p = p.ParentElement;
			}
			return false;
		}
		// -------------------------
		// Snapping helpers (vertex/grid)
		// -------------------------

		private double[][] computeMovedGroupBaseCorners(java.util.List<Element> movedRoots, AnimationFrame curFrame, boolean keyframeMode, java.util.HashMap<Element, AnimFrameElement> kfMap)
	{
		if (movedRoots == null || movedRoots.size() == 0) return null;

		// Collect *actual* element corners (not just the group's AABB) so vertex snapping can
		// truly snap "corner to corner" even with rotations and multi-element selections.
		java.util.ArrayList<double[]> corners = new java.util.ArrayList<double[]>(64);

		for (Element r : movedRoots) {
			if (r == null) continue;
			java.util.ArrayDeque<Element> stack = new java.util.ArrayDeque<Element>();
			stack.push(r);
			while (!stack.isEmpty()) {
				Element e = stack.pop();
				if (e == null) continue;

				double[][] cs = computeElementRootCornersWithAnim(e, curFrame, keyframeMode, kfMap);
				if (cs != null) {
					for (int i = 0; i < cs.length; i++) {
						corners.add(cs[i]);
						if (corners.size() > 4096) break; // sanity cap
					}
				}

				if (e.ChildElements != null) {
					for (int i = 0; i < e.ChildElements.size(); i++) {
						stack.push(e.ChildElements.get(i));
					}
				}
			}
		}

		if (corners.isEmpty()) return null;
		double[][] out = new double[corners.size()][3];
		for (int i = 0; i < corners.size(); i++) {
			out[i] = corners.get(i);
		}
		return out;
	}


	private java.util.ArrayList<double[]> computeVertexSnapTargets(Project project, java.util.List<Element> movedRoots, AnimationFrame curFrame, boolean keyframeMode, java.util.HashMap<Element, AnimFrameElement> kfMap)
	{
		if (project == null) return null;
		java.util.HashSet<Element> exclude = new java.util.HashSet<Element>();
		if (movedRoots != null) {
			for (Element r : movedRoots) collectDescendants(r, exclude);
		}

		java.util.ArrayList<double[]> out = new java.util.ArrayList<double[]>();
		java.util.ArrayDeque<Element> stack = new java.util.ArrayDeque<Element>();
		if (project.rootElements != null) {
			for (int i = 0; i < project.rootElements.size(); i++) stack.push(project.rootElements.get(i));
		}

		while (!stack.isEmpty()) {
			Element e = stack.pop();
			if (e == null) continue;
			if (!exclude.isEmpty() && exclude.contains(e)) {
				// Still traverse to mark children as excluded (already in set), but no target corners.
			} else {
				double[][] corners = computeElementRootCornersWithAnim(e, curFrame, keyframeMode, kfMap);
				if (corners != null) {
					for (int i = 0; i < corners.length; i++) {
						double[] c = corners[i];
						if (c != null && c.length >= 3) out.add(new double[] { c[0], c[1], c[2] });
					}
				}
			}
			if (e.ChildElements != null) {
				for (int i = 0; i < e.ChildElements.size(); i++) stack.push(e.ChildElements.get(i));
			}
		}

		return out;
	}

	private double[] computeElementRootBoundsWithAnim(Element elem, AnimationFrame curFrame, boolean keyframeMode, java.util.HashMap<Element, AnimFrameElement> kfMap)
	{
		double[][] corners = computeElementRootCornersWithAnim(elem, curFrame, keyframeMode, kfMap);
		if (corners == null) return null;

		double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < corners.length; i++) {
			double[] c = corners[i];
			if (c == null || c.length < 3) continue;
			minX = Math.min(minX, c[0]);
			minY = Math.min(minY, c[1]);
			minZ = Math.min(minZ, c[2]);
			maxX = Math.max(maxX, c[0]);
			maxY = Math.max(maxY, c[1]);
			maxZ = Math.max(maxZ, c[2]);
		}
		if (!Double.isFinite(minX) || !Double.isFinite(maxX)) return null;
		return new double[] { minX, minY, minZ, maxX, maxY, maxZ };
	}

	private double[][] computeElementRootCornersWithAnim(Element elem, AnimationFrame curFrame, boolean keyframeMode, java.util.HashMap<Element, AnimFrameElement> kfMap)
	{
		if (elem == null) return null;

		float[] mat = Mat4f.Identity_(new float[16]);

		java.util.List<Element> path = elem.GetParentPath();
		if (path != null) {
			for (Element p : path) {
				applyTransformWithAnimOffset(mat, p, curFrame, keyframeMode, kfMap);
			}
		}
		applyTransformWithAnimOffset(mat, elem, curFrame, keyframeMode, kfMap);

		double w = elem.getWidth();
		double h = elem.getHeight();
		double d = elem.getDepth();

		double[][] local = new double[][] {
			{0, 0, 0}, {w, 0, 0}, {0, h, 0}, {0, 0, d},
			{w, h, 0}, {w, 0, d}, {0, h, d}, {w, h, d}
		};

		double[][] out = new double[8][3];
		for (int i = 0; i < local.length; i++) {
			double[] c = local[i];
			float[] v = Mat4f.MulWithVec4(mat, new float[] { (float)c[0], (float)c[1], (float)c[2], 1f });
			out[i][0] = v[0];
			out[i][1] = v[1];
			out[i][2] = v[2];
		}
		return out;
	}

	private void applyTransformWithAnimOffset(float[] mat, Element elem, AnimationFrame curFrame, boolean keyframeMode, java.util.HashMap<Element, AnimFrameElement> kfMap)
	{
		if (elem == null) return;
		double offX = 0, offY = 0, offZ = 0;

		AnimFrameElement afe = null;
		if (keyframeMode && kfMap != null) {
			afe = kfMap.get(elem);
		}
		if (afe == null && curFrame != null) {
			afe = curFrame.GetAnimFrameElementRec(elem);
			if (afe == null) afe = curFrame.GetAnimFrameElementRecByName(elem.getName());
		}
		if (afe != null) {
			offX = afe.getOffsetX();
			offY = afe.getOffsetY();
			offZ = afe.getOffsetZ();
		}

		Mat4f.Translate(mat, mat, new float[] { (float)elem.getOriginX(), (float)elem.getOriginY(), (float)elem.getOriginZ() });
		Mat4f.RotateX(mat, mat, (float)elem.getRotationX() * GameMath.DEG2RAD);
		Mat4f.RotateY(mat, mat, (float)elem.getRotationY() * GameMath.DEG2RAD);
		Mat4f.RotateZ(mat, mat, (float)elem.getRotationZ() * GameMath.DEG2RAD);
		Mat4f.Translate(mat, mat, new float[] { (float)-elem.getOriginX(), (float)-elem.getOriginY(), (float)-elem.getOriginZ() });

		Mat4f.Translate(mat, mat, new float[] { (float)(elem.getStartX() + offX), (float)(elem.getStartY() + offY), (float)(elem.getStartZ() + offZ) });
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
			viewportGizmoMirrorPartners = null;
			viewportGizmoMirrorBaseX = null;
			viewportGizmoMirrorBaseY = null;
			viewportGizmoMirrorBaseZ = null;
					viewportGizmoSkipSelection = false;
viewportRotateDragActive = false;
viewportRotateDragAxis = 0;
viewportRotateElems = null;
viewportRotateBaseRotX = null;
viewportRotateBaseRotY = null;
viewportRotateBaseRotZ = null;
viewportRotateBaseStartX = null;
viewportRotateBaseStartY = null;
viewportRotateBaseStartZ = null;
viewportRotateWorldAxisToLocal = null;
viewportRotateDragInKeyframeMode = false;
viewportRotateDragAnimVersion = 0;
viewportRotateKeyframeElems = null;
viewportRotateBaseKfRotX = null;
viewportRotateBaseKfRotY = null;
viewportRotateBaseKfRotZ = null;
viewportRotateBaseKfPosX = null;
viewportRotateBaseKfPosY = null;
viewportRotateBaseKfPosZ = null;
viewportRotateBaseCenterWorld = null;
viewportRotateSkipSelection = false;

				// Scale gizmo drag state
				viewportScaleDragActive = false;
				viewportScaleDragMode = 0;
				viewportScaleAxisDirX = 0;
				viewportScaleAxisDirY = 0;
				viewportScaleUseRayConstraint = false;
				viewportScaleAxisOriginX = viewportScaleAxisOriginY = viewportScaleAxisOriginZ = 0;
				viewportScaleAxisWorldDirX = viewportScaleAxisWorldDirY = viewportScaleAxisWorldDirZ = 0;
				viewportScaleAxisParamStart = 0;
				viewportScaleElems = null;
				viewportScaleBaseStartX = null;
				viewportScaleBaseStartY = null;
				viewportScaleBaseStartZ = null;
				viewportScaleBaseW = null;
				viewportScaleBaseH = null;
				viewportScaleBaseD = null;
				viewportScaleDragInKeyframeMode = false;
				viewportScaleDragAnimVersion = 0;
				viewportScaleKeyframeElems = null;
				viewportScaleBaseKfSX = null;
				viewportScaleBaseKfSY = null;
				viewportScaleBaseKfSZ = null;
				viewportScaleBaseKfPosX = null;
				viewportScaleBaseKfPosY = null;
				viewportScaleBaseKfPosZ = null;
				viewportScaleMirrorPartners = null;
				viewportScaleMirrorBaseStartX = null;
				viewportScaleMirrorBaseStartY = null;
				viewportScaleMirrorBaseStartZ = null;
				viewportScaleMirrorBaseW = null;
				viewportScaleMirrorBaseH = null;
				viewportScaleMirrorBaseD = null;
				viewportScaleSkipSelection = false;
				grabbing = true;
			}
			
			if (!mouseDownOnCenterPanel && !mouseDownOnLeftPanel && !mouseDownOnRightPanel) {
				mouseDownOnLeftPanel = isOnLeftPanel;
				mouseDownOnCenterPanel = !isOnLeftPanel && !isOnRightPanel;
				mouseDownOnRightPanel = isOnRightPanel;
			}

				// Try start a viewport gizmo drag when clicking in the viewport
				if (!ModelCreator.viewportGizmoHidden && mouseDownOnCenterPanel && mouseDownButton == 0 && !mouseDownWithShift) {
					if (ModelCreator.viewportGizmoMode == ModelCreator.VIEWPORT_GIZMO_MODE_ROTATE) {
						tryStartViewportRotateGizmoDrag();
					} else if (ModelCreator.viewportGizmoMode == ModelCreator.VIEWPORT_GIZMO_MODE_SCALE) {
						tryStartViewportScaleGizmoDrag();
					} else {
						tryStartViewportGizmoDrag();
					}
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
			if (grabbing && mouseDownOnCenterPanel && mouseDownButton == 0 && !mouseDownWithCtrl && !viewportGizmoSkipSelection && !viewportRotateSkipSelection && !viewportScaleSkipSelection) {
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
							boolean hitIsPartOfMultiSelection = (viewportGroupMoveEnabled || isViewportSelectionOutOfSync(currentProject))
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
						} else {
							// Clicked empty space in the 3D viewport: clear selection.
							// (Keeps Shift+click semantics intact for multi-select workflows.)
							currentProject.selectElement(null);
							currentProject.SelectedElements = new ArrayList<Element>();
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

			// Optional: auto-fix Z-fighting after dropping/moving elements (performance-friendly: run once on drop)
			if (autofixZFighting && currentRightTab == 0 && currentProject != null) {
				// Capture moved elements before clearing drag state
				java.util.List<Element> moved = null;
				if (viewportGizmoElems != null && viewportGizmoElems.size() > 0 && !viewportGizmoDragInKeyframeMode) {
					moved = new java.util.ArrayList<Element>(viewportGizmoElems);
				}
				// Fallback: selected element (covers non-gizmo changes like duplicate + immediate release)
				if ((moved == null || moved.size() == 0) && currentProject.SelectedElement != null) {
					moved = java.util.Arrays.asList(currentProject.SelectedElement);
				}
				if (moved != null && moved.size() > 0) {
					currentProject.autoFixZFighting(moved, autofixZFightingEpsilon, true);
				}
			}

			viewportGizmoDragActive = false;
			viewportGizmoDragMode = 0;
			viewportGizmoUseRayConstraint = false;
			viewportGizmoElems = null;
			viewportGizmoBaseY = null;
			viewportGizmoBaseX = null;
			viewportGizmoBaseZ = null;
			viewportGizmoSkipSelection = false;

viewportRotateDragActive = false;
viewportRotateDragAxis = 0;
viewportRotateElems = null;
viewportRotateBaseRotX = null;
viewportRotateBaseRotY = null;
viewportRotateBaseRotZ = null;
viewportRotateBaseStartX = null;
viewportRotateBaseStartY = null;
viewportRotateBaseStartZ = null;
viewportRotateWorldAxisToLocal = null;
viewportRotateDragInKeyframeMode = false;
viewportRotateDragAnimVersion = 0;
viewportRotateKeyframeElems = null;
viewportRotateBaseKfRotX = null;
viewportRotateBaseKfRotY = null;
viewportRotateBaseKfRotZ = null;
viewportRotateBaseKfPosX = null;
viewportRotateBaseKfPosY = null;
viewportRotateBaseKfPosZ = null;
viewportRotateBaseCenterWorld = null;
viewportRotateMirrorPartners = null;
viewportRotateMirrorBaseRotX = null;
viewportRotateMirrorBaseRotY = null;
viewportRotateMirrorBaseRotZ = null;
viewportRotateMirrorBaseStartX = null;
viewportRotateMirrorBaseStartY = null;
viewportRotateMirrorBaseStartZ = null;
viewportRotateSkipSelection = false;

// Scale gizmo drag state
viewportScaleDragActive = false;
viewportScaleDragMode = 0;
viewportScaleAxisDirX = 0;
viewportScaleAxisDirY = 0;
viewportScaleUseRayConstraint = false;
viewportScaleAxisOriginX = viewportScaleAxisOriginY = viewportScaleAxisOriginZ = 0;
viewportScaleAxisWorldDirX = viewportScaleAxisWorldDirY = viewportScaleAxisWorldDirZ = 0;
viewportScaleAxisParamStart = 0;
viewportScaleElems = null;
viewportScaleBaseStartX = null;
viewportScaleBaseStartY = null;
viewportScaleBaseStartZ = null;
viewportScaleBaseW = null;
viewportScaleBaseH = null;
viewportScaleBaseD = null;
viewportScaleDragInKeyframeMode = false;
viewportScaleDragAnimVersion = 0;
viewportScaleKeyframeElems = null;
viewportScaleBaseKfSX = null;
viewportScaleBaseKfSY = null;
viewportScaleBaseKfSZ = null;
viewportScaleBaseKfPosX = null;
viewportScaleBaseKfPosY = null;
viewportScaleBaseKfPosZ = null;
viewportScaleMirrorPartners = null;
viewportScaleMirrorBaseStartX = null;
viewportScaleMirrorBaseStartY = null;
viewportScaleMirrorBaseStartZ = null;
viewportScaleMirrorBaseW = null;
viewportScaleMirrorBaseH = null;
viewportScaleMirrorBaseD = null;
viewportScaleSkipSelection = false;

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


// Viewport rotate gizmo drag (gimbal rings)
if (grabbing && viewportRotateDragActive && mouseDownOnCenterPanel && mouseDownButton == 0 && Mouse.isButtonDown(0) && viewportRotateElems != null)
{
	int newMouseX = Mouse.getX();
	int newMouseY = Mouse.getY();
	int dx = newMouseX - lastMouseX;
	int dy = newMouseY - lastMouseY;

	if (dx != 0 || dy != 0)
	{
		ModelCreator.changeHistory.beginMultichangeHistoryState();
					lastMouseX = newMouseX;
					lastMouseY = newMouseY;

		double[] ray = modelrenderer.getMouseRayWorld(newMouseX, newMouseY);
		if (ray != null) {
			double r0x = ray[0], r0y = ray[1], r0z = ray[2];
			double rdx = ray[3], rdy = ray[4], rdz = ray[5];

			double nx = 0, ny = 0, nz = 0;
			if (viewportRotateDragAxis == 1) nx = 1;
			if (viewportRotateDragAxis == 2) ny = 1;
			if (viewportRotateDragAxis == 3) nz = 1;

			double denom = nx*rdx + ny*rdy + nz*rdz;
			if (Math.abs(denom) > 1e-9) {
				double t = (nx*(viewportRotatePivotX - r0x) + ny*(viewportRotatePivotY - r0y) + nz*(viewportRotatePivotZ - r0z)) / denom;
				double ix = r0x + rdx * t;
				double iy = r0y + rdy * t;
				double iz = r0z + rdz * t;

				double vx = ix - viewportRotatePivotX;
				double vy = iy - viewportRotatePivotY;
				double vz = iz - viewportRotatePivotZ;

				double dot = vx*nx + vy*ny + vz*nz;
				vx -= dot * nx;
				vy -= dot * ny;
				vz -= dot * nz;

				double vlen = Math.sqrt(vx*vx + vy*vy + vz*vz);
				if (Double.isFinite(vlen) && vlen > 1e-9) {
					vx /= vlen;
					vy /= vlen;
					vz /= vlen;

					// Signed angle from start vector to current vector around the chosen axis
					double cxv = viewportRotateStartVy * vz - viewportRotateStartVz * vy;
					double cyv = viewportRotateStartVz * vx - viewportRotateStartVx * vz;
					double czv = viewportRotateStartVx * vy - viewportRotateStartVy * vx;
					double sin = cxv*nx + cyv*ny + czv*nz;
					double cos = viewportRotateStartVx*vx + viewportRotateStartVy*vy + viewportRotateStartVz*vz;
					double ang = Math.atan2(sin, cos);
					double deg = ang * 180.0 / Math.PI;

					for (Element e : viewportRotateElems) {
						if (e == null) continue;

						Double brx = viewportRotateBaseRotX != null ? viewportRotateBaseRotX.get(e) : null;
						Double bry = viewportRotateBaseRotY != null ? viewportRotateBaseRotY.get(e) : null;
						Double brz = viewportRotateBaseRotZ != null ? viewportRotateBaseRotZ.get(e) : null;
						if (brx == null || bry == null || brz == null) continue;

						AnimFrameElement kf = (viewportRotateDragInKeyframeMode && viewportRotateKeyframeElems != null) ? viewportRotateKeyframeElems.get(e) : null;

						// Rotation
						if (viewportRotateDragInKeyframeMode && kf != null) {
							Double kx = viewportRotateBaseKfRotX != null ? viewportRotateBaseKfRotX.get(e) : 0d;
							Double ky = viewportRotateBaseKfRotY != null ? viewportRotateBaseKfRotY.get(e) : 0d;
							Double kz = viewportRotateBaseKfRotZ != null ? viewportRotateBaseKfRotZ.get(e) : 0d;
							if (kx == null) kx = 0d;
							if (ky == null) ky = 0d;
							if (kz == null) kz = 0d;

							switch (viewportRotateDragAxis) {
								case 1: kf.setRotationX(kx + deg); break;
								case 2: kf.setRotationY(ky + deg); break;
								case 3: kf.setRotationZ(kz + deg); break;
								default: break;
							}
						} else {
							switch (viewportRotateDragAxis) {
								case 1: e.setRotationX(brx + deg); break;
								case 2: e.setRotationY(bry + deg); break;
								case 3: e.setRotationZ(brz + deg); break;
								default: break;
							}
						}

							// Mirror Mode (modeling): apply mirrored rotation to L/R partner
							if (!viewportRotateDragInKeyframeMode && ModelCreator.mirrorMode && viewportRotateMirrorPartners != null) {
								Element partner = viewportRotateMirrorPartners.get(e);
								if (partner != null && viewportRotateMirrorBaseRotX != null && viewportRotateMirrorBaseRotY != null && viewportRotateMirrorBaseRotZ != null) {
									double mdeg = deg;
									at.vintagestory.modelcreator.enums.EnumAxis maxis = GetMirrorAxis(currentProject);
									if (maxis == at.vintagestory.modelcreator.enums.EnumAxis.X) {
										if (viewportRotateDragAxis != 1) mdeg = -deg;
									} else if (maxis == at.vintagestory.modelcreator.enums.EnumAxis.Z) {
										if (viewportRotateDragAxis != 3) mdeg = -deg;
									}
									Double pbrx = viewportRotateMirrorBaseRotX.get(partner);
									Double pbry = viewportRotateMirrorBaseRotY.get(partner);
									Double pbrz = viewportRotateMirrorBaseRotZ.get(partner);
									if (pbrx == null) pbrx = partner.getRotationX();
									if (pbry == null) pbry = partner.getRotationY();
									if (pbrz == null) pbrz = partner.getRotationZ();
									switch (viewportRotateDragAxis) {
										case 1: partner.setRotationX(pbrx + mdeg); break;
										case 2: partner.setRotationY(pbry + mdeg); break;
										case 3: partner.setRotationZ(pbrz + mdeg); break;
										default: break;
									}
								}
							}

						// Group-pivot orbit: rotate the reference center around the shared pivot and apply as translation delta.
						if (viewportRotateGroupPivotEnabled) {
							double[] baseCenter = viewportRotateBaseCenterWorld != null ? viewportRotateBaseCenterWorld.get(e) : null;
							if (baseCenter != null && baseCenter.length >= 3) {
								double ox = baseCenter[0], oy = baseCenter[1], oz = baseCenter[2];
								double nxp = ox, nyp = oy, nzp = oz;
								double s = Math.sin(ang);
								double c = Math.cos(ang);
								switch (viewportRotateDragAxis) {
									case 1: {
										double dyc = oy - viewportRotatePivotY;
										double dzc = oz - viewportRotatePivotZ;
										nyp = viewportRotatePivotY + dyc * c - dzc * s;
										nzp = viewportRotatePivotZ + dyc * s + dzc * c;
										break;
									}
									case 2: {
										double dxc = ox - viewportRotatePivotX;
										double dzc = oz - viewportRotatePivotZ;
										nxp = viewportRotatePivotX + dxc * c + dzc * s;
										nzp = viewportRotatePivotZ - dxc * s + dzc * c;
										break;
									}
									case 3: {
										double dxc = ox - viewportRotatePivotX;
										double dyc = oy - viewportRotatePivotY;
										nxp = viewportRotatePivotX + dxc * c - dyc * s;
										nyp = viewportRotatePivotY + dxc * s + dyc * c;
										break;
									}
									default: break;
								}

								double dwx = nxp - ox;
								double dwy = nyp - oy;
								double dwz = nzp - oz;

								double[] map = viewportRotateWorldAxisToLocal != null ? viewportRotateWorldAxisToLocal.get(e) : null;
								if (map != null && map.length >= 9) {
									double ldx = dwx * map[0] + dwy * map[3] + dwz * map[6];
									double ldy = dwx * map[1] + dwy * map[4] + dwz * map[7];
									double ldz = dwx * map[2] + dwy * map[5] + dwz * map[8];

									if (viewportRotateDragInKeyframeMode && kf != null) {
										Double bx = viewportRotateBaseKfPosX != null ? viewportRotateBaseKfPosX.get(e) : 0d;
										Double by = viewportRotateBaseKfPosY != null ? viewportRotateBaseKfPosY.get(e) : 0d;
										Double bz = viewportRotateBaseKfPosZ != null ? viewportRotateBaseKfPosZ.get(e) : 0d;
										if (bx == null) bx = 0d;
										if (by == null) by = 0d;
										if (bz == null) bz = 0d;
										kf.setOffsetX(bx + ldx);
										kf.setOffsetY(by + ldy);
										kf.setOffsetZ(bz + ldz);
									} else {
										Double bsx = viewportRotateBaseStartX != null ? viewportRotateBaseStartX.get(e) : null;
										Double bsy = viewportRotateBaseStartY != null ? viewportRotateBaseStartY.get(e) : null;
										Double bsz = viewportRotateBaseStartZ != null ? viewportRotateBaseStartZ.get(e) : null;
										if (bsx != null && bsy != null && bsz != null) {
											e.setStartX(bsx + ldx);
											e.setStartY(bsy + ldy);
											e.setStartZ(bsz + ldz);

											// Mirror Mode (modeling): apply mirrored orbit translation to L/R partner
											if (!viewportRotateDragInKeyframeMode && ModelCreator.mirrorMode && viewportRotateMirrorPartners != null && viewportRotateMirrorBaseStartX != null) {
												Element partner = viewportRotateMirrorPartners.get(e);
												if (partner != null) {
													at.vintagestory.modelcreator.enums.EnumAxis maxis = GetMirrorAxis(currentProject);
													double pdx = ldx, pdy = ldy, pdz = ldz;
													if (maxis == at.vintagestory.modelcreator.enums.EnumAxis.X) pdx = -ldx;
													if (maxis == at.vintagestory.modelcreator.enums.EnumAxis.Z) pdz = -ldz;
													Double pbsx = viewportRotateMirrorBaseStartX.get(partner);
													Double pbsy = viewportRotateMirrorBaseStartY != null ? viewportRotateMirrorBaseStartY.get(partner) : null;
													Double pbsz = viewportRotateMirrorBaseStartZ != null ? viewportRotateMirrorBaseStartZ.get(partner) : null;
													if (pbsx == null) pbsx = partner.getStartX();
													if (pbsy == null) pbsy = partner.getStartY();
													if (pbsz == null) pbsz = partner.getStartZ();
													partner.setStartX(pbsx + pdx);
													partner.setStartY(pbsy + pdy);
													partner.setStartZ(pbsz + pdz);
												}
											}
										}
									}
								}

							}
						}
						if (viewportRotateDragInKeyframeMode && kf != null) { SyncMirrorKeyframe(kf, viewportRotateElems); }
					}
				}



						// Ensure animation preview updates in real time while dragging in Keyframe mode.
						if (viewportRotateDragInKeyframeMode && currentProject != null && currentProject.SelectedAnimation != null) {
							currentProject.SelectedAnimation.SetFramesDirty();
						}
			}
		}
	}
}

			// Viewport scale gizmo drag (axis handles + center uniform)
			if (grabbing && viewportScaleDragActive && mouseDownOnCenterPanel && mouseDownButton == 0 && Mouse.isButtonDown(0) && viewportScaleElems != null)
			{
				int newMouseX = Mouse.getX();
				int newMouseY = Mouse.getY();
				int ddx = newMouseX - mouseDownX;
				int ddy = newMouseY - mouseDownY;

				if (ddx != 0 || ddy != 0)
				{
					ModelCreator.changeHistory.beginMultichangeHistoryState();

					// Determine drag delta in world units when possible.
					double delta = 0;
					boolean haveDelta = false;
					if (viewportScaleDragMode != 4 && viewportScaleUseRayConstraint && !viewportFreedomModeEnabled)
					{
						Double tcur = computeScaleAxisParamFromMouse(newMouseX, newMouseY);
						if (tcur != null && Double.isFinite(tcur)) {
							delta = tcur - viewportScaleAxisParamStart;
							haveDelta = true;
						}
					}
					if (!haveDelta) {
						// Fallback: interpret mouse delta in screen space along the axis direction.
						if (viewportScaleDragMode == 4) {
							// Uniform: diagonal gesture (right+up grows).
							delta = (ddx + ddy) / 40.0;
						} else {
							delta = (ddx * viewportScaleAxisDirX + ddy * viewportScaleAxisDirY) / 20.0;
						}
					}

					if (viewportScaleDragInKeyframeMode && currentProject != null && currentProject.SelectedAnimation != null)
					{
						// Animation tab: Scale gizmo drives Stretch (multiplicative)
						double factor = 1.0 + delta / 10.0;
						if (!Double.isFinite(factor)) factor = 1.0;
						factor = Math.max(0.01, Math.min(100.0, factor));

						// We may need current evaluated offsets (for seeding PositionSet when floor guard engages)
						AnimationFrame curAnimFrame = null;
						int curFrameIndex = currentProject.SelectedAnimation.currentFrame;
						if (currentProject.SelectedAnimation.allFrames != null && curFrameIndex >= 0 && curFrameIndex < currentProject.SelectedAnimation.allFrames.size()) {
							curAnimFrame = currentProject.SelectedAnimation.allFrames.get(curFrameIndex);
						}

						for (Element e : viewportScaleElems) {
							if (e == null) continue;
							AnimFrameElement kf = viewportScaleKeyframeElems != null ? viewportScaleKeyframeElems.get(e) : null;
							if (kf == null) continue;
							Double bsx = viewportScaleBaseKfSX != null ? viewportScaleBaseKfSX.get(e) : null;
							Double bsy = viewportScaleBaseKfSY != null ? viewportScaleBaseKfSY.get(e) : null;
							Double bsz = viewportScaleBaseKfSZ != null ? viewportScaleBaseKfSZ.get(e) : null;
							if (bsx == null) bsx = 1d;
							if (bsy == null) bsy = 1d;
							if (bsz == null) bsz = 1d;

							double nsx = bsx, nsy = bsy, nsz = bsz;
							switch (viewportScaleDragMode) {
								case 1: nsx = bsx * factor; break;
								case 2: nsy = bsy * factor; break;
								case 3: nsz = bsz * factor; break;
								case 4: nsx = bsx * factor; nsy = bsy * factor; nsz = bsz * factor; break;
								default: break;
							}
							nsx = Math.max(0.01, Math.min(100.0, nsx));
							nsy = Math.max(0.01, Math.min(100.0, nsy));
							nsz = Math.max(0.01, Math.min(100.0, nsz));
							kf.setStretchX(nsx);
							kf.setStretchY(nsy);
							kf.setStretchZ(nsz);

							// Floor guard in animation mode: stretching around a non-bottom origin can push the element below its
							// drag-start "floor". When enabled, we compensate by nudging OffsetY up (in a single undo step).
							if (viewportFloorSnapEnabled && (viewportScaleDragMode == 2 || viewportScaleDragMode == 4)) {
								Double baseOffX = viewportScaleBaseKfPosX != null ? viewportScaleBaseKfPosX.get(e) : null;
								Double baseOffY = viewportScaleBaseKfPosY != null ? viewportScaleBaseKfPosY.get(e) : null;
								Double baseOffZ = viewportScaleBaseKfPosZ != null ? viewportScaleBaseKfPosZ.get(e) : null;
								if (baseOffX == null) baseOffX = 0d;
								if (baseOffY == null) baseOffY = 0d;
								if (baseOffZ == null) baseOffZ = 0d;

								// Total origin in animation draw() is element origin + keyframe origin offset
								double originY = e.getOriginY() + kf.getOriginY();
								double baseBottomY = e.getStartY() + baseOffY + originY * (1.0 - bsy);
								double newBottomY = e.getStartY() + baseOffY + originY * (1.0 - nsy);
								if (Double.isFinite(baseBottomY) && Double.isFinite(newBottomY) && newBottomY < baseBottomY) {
									double neededLift = baseBottomY - newBottomY;
									double newOffY = baseOffY + neededLift;

									// Ensure PositionSet is enabled so the offset takes effect; seed from the evaluated frame so we don't snap.
									AnimFrameElement prevKf = currentProject.SelectedAnimation.GetKeyFrameElement(e, curFrameIndex);
									boolean prevPosSet = prevKf != null && prevKf.PositionSet;
									AnimFrameElement kfPos = currentProject.SelectedAnimation.TogglePosition(e, true);
									if (kfPos != null) {
										// If we just enabled Position, seed from the current evaluated offsets (or our cached bases).
										if (!prevPosSet) {
											AnimFrameElement fe = curAnimFrame != null ? curAnimFrame.GetAnimFrameElementRec(e) : null;
											if (fe != null) {
												kfPos.setOffsetX(fe.getOffsetX());
												kfPos.setOffsetY(fe.getOffsetY());
												kfPos.setOffsetZ(fe.getOffsetZ());
											} else {
												kfPos.setOffsetX(baseOffX);
												kfPos.setOffsetY(baseOffY);
												kfPos.setOffsetZ(baseOffZ);
											}
										}

										// Apply the Y compensation (keep X/Z at drag-start values).
										kfPos.setOffsetX(baseOffX);
										kfPos.setOffsetY(newOffY);
										kfPos.setOffsetZ(baseOffZ);

										// Keep our local ref consistent for mirror syncing.
										kf = kfPos;
										if (viewportScaleKeyframeElems != null) viewportScaleKeyframeElems.put(e, kfPos);
									}
								}
							}
							SyncMirrorKeyframe(kf, viewportScaleElems);
						}

						currentProject.SelectedAnimation.SetFramesDirty();
					}
					else
					{
						// Modeling tab: Scale gizmo drives Resize (width/height/depth), keeping element centered.
						final double minSize = 0.01;
						for (Element e : viewportScaleElems)
						{
							if (e == null) continue;
							Double bx = viewportScaleBaseStartX != null ? viewportScaleBaseStartX.get(e) : null;
							Double by = viewportScaleBaseStartY != null ? viewportScaleBaseStartY.get(e) : null;
							Double bz = viewportScaleBaseStartZ != null ? viewportScaleBaseStartZ.get(e) : null;
							Double bw = viewportScaleBaseW != null ? viewportScaleBaseW.get(e) : null;
							Double bh = viewportScaleBaseH != null ? viewportScaleBaseH.get(e) : null;
							Double bd = viewportScaleBaseD != null ? viewportScaleBaseD.get(e) : null;
							if (bx == null) bx = e.getStartX();
							if (by == null) by = e.getStartY();
							if (bz == null) bz = e.getStartZ();
							if (bw == null) bw = e.getWidth();
							if (bh == null) bh = e.getHeight();
							if (bd == null) bd = e.getDepth();

							double newW = bw, newH = bh, newD = bd;
							double shiftX = 0, shiftY = 0, shiftZ = 0;
							switch (viewportScaleDragMode) {
								case 1:
									newW = Math.max(minSize, bw + 2 * delta);
									shiftX = -(newW - bw) / 2.0;
									break;
								case 2:
									newH = Math.max(minSize, bh + 2 * delta);
									shiftY = -(newH - bh) / 2.0;
									break;
								case 3:
									newD = Math.max(minSize, bd + 2 * delta);
									shiftZ = -(newD - bd) / 2.0;
									break;
								case 4:
									newW = Math.max(minSize, bw + 2 * delta);
									newH = Math.max(minSize, bh + 2 * delta);
									newD = Math.max(minSize, bd + 2 * delta);
									shiftX = -(newW - bw) / 2.0;
									shiftY = -(newH - bh) / 2.0;
									shiftZ = -(newD - bd) / 2.0;
									break;
								default: break;
							}

							double newStartX = bx + shiftX;
							double newStartY = by + shiftY;
							double newStartZ = bz + shiftZ;
							// Floor guard: never push the element below its drag-start floor level.
							if (viewportFloorSnapEnabled && newStartY < by) {
								newStartY = by;
							}

							e.setWidth(newW);
							e.setHeight(newH);
							e.setDepth(newD);
							e.setStartX(newStartX);
							e.setStartY(newStartY);
							e.setStartZ(newStartZ);

							// Mirror Mode: apply the same resize deltas to the L/R partner when it isn't selected.
							if (ModelCreator.mirrorMode && viewportScaleMirrorPartners != null)
							{
								Element partner = viewportScaleMirrorPartners.get(e);
								if (partner != null)
								{
									Double pbx = viewportScaleMirrorBaseStartX != null ? viewportScaleMirrorBaseStartX.get(partner) : null;
									Double pby = viewportScaleMirrorBaseStartY != null ? viewportScaleMirrorBaseStartY.get(partner) : null;
									Double pbz = viewportScaleMirrorBaseStartZ != null ? viewportScaleMirrorBaseStartZ.get(partner) : null;
									Double pbw = viewportScaleMirrorBaseW != null ? viewportScaleMirrorBaseW.get(partner) : null;
									Double pbh = viewportScaleMirrorBaseH != null ? viewportScaleMirrorBaseH.get(partner) : null;
									Double pbd = viewportScaleMirrorBaseD != null ? viewportScaleMirrorBaseD.get(partner) : null;
									if (pbx == null) pbx = partner.getStartX();
									if (pby == null) pby = partner.getStartY();
									if (pbz == null) pbz = partner.getStartZ();
									if (pbw == null) pbw = partner.getWidth();
									if (pbh == null) pbh = partner.getHeight();
									if (pbd == null) pbd = partner.getDepth();

									double changeW = (viewportScaleDragMode == 1 || viewportScaleDragMode == 4) ? (newW - bw) : 0;
									double changeH = (viewportScaleDragMode == 2 || viewportScaleDragMode == 4) ? (newH - bh) : 0;
									double changeD = (viewportScaleDragMode == 3 || viewportScaleDragMode == 4) ? (newD - bd) : 0;

									double pNewW = Math.max(minSize, pbw + changeW);
									double pNewH = Math.max(minSize, pbh + changeH);
									double pNewD = Math.max(minSize, pbd + changeD);
									double pShiftX = (viewportScaleDragMode == 1 || viewportScaleDragMode == 4) ? (-(pNewW - pbw) / 2.0) : 0;
									double pShiftY = (viewportScaleDragMode == 2 || viewportScaleDragMode == 4) ? (-(pNewH - pbh) / 2.0) : 0;
									double pShiftZ = (viewportScaleDragMode == 3 || viewportScaleDragMode == 4) ? (-(pNewD - pbd) / 2.0) : 0;

									double pStartX = pbx + pShiftX;
									double pStartY = pby + pShiftY;
									double pStartZ = pbz + pShiftZ;
									if (viewportFloorSnapEnabled && pStartY < pby) {
										pStartY = pby;
									}

									partner.setWidth(pNewW);
									partner.setHeight(pNewH);
									partner.setDepth(pNewD);
									partner.setStartX(pStartX);
									partner.setStartY(pStartY);
									partner.setStartZ(pStartZ);
								}
							}
						}
					}

					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() { updateValues(null); }
					});
					lastMouseX = newMouseX;
					lastMouseY = newMouseY;
					return;
				}

				lastMouseX = newMouseX;
				lastMouseY = newMouseY;
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

											// Desired WORLD delta along the active axis.
											double dwx = 0, dwy = 0, dwz = 0;
											switch (viewportGizmoDragMode) {
												case 1: dwx = delta; break;
												case 2: dwy = delta; break;
												case 3: dwz = delta; break;
												default: break;
											}
											// Apply optional snapping (grid / vertex) relative to drag-start pivot.
											double[] snapped = applyViewportMoveSnapping(dwx, dwy, dwz, viewportGizmoDragMode == 1, viewportGizmoDragMode == 2, viewportGizmoDragMode == 3);
											dwx = snapped[0]; dwy = snapped[1]; dwz = snapped[2];
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
																					if (kf != null) { kf.setOffsetX(bx + dwx); kf.setOffsetY(by); kf.setOffsetZ(bz); }
																					else { e.setStartX(bx + dwx); e.setStartY(by); e.setStartZ(bz); }
																					break;
																			case 2:
																			{
																				double ny = by + dwy;
																				if (viewportFloorSnapEnabled && ny < by) ny = by;
																				if (kf != null) { kf.setOffsetX(bx); kf.setOffsetY(ny); kf.setOffsetZ(bz); } else { e.setStartX(bx); e.setStartY(ny); e.setStartZ(bz); }
																				break;
																			}
																			case 3:
																					if (kf != null) { kf.setOffsetX(bx); kf.setOffsetY(by); kf.setOffsetZ(bz + dwz); }
																					else { e.setStartX(bx); e.setStartY(by); e.setStartZ(bz + dwz); }
																					break;
																			default: break;
																		}
																	} else {
																		double ldx = dwx * map[0] + dwy * map[3] + dwz * map[6];
																		double ldy = dwx * map[1] + dwy * map[4] + dwz * map[7];
																		double ldz = dwx * map[2] + dwy * map[5] + dwz * map[8];
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
													// Mirror Mode (modeling): apply mirrored movement to L/R partner
											if (!viewportGizmoDragInKeyframeMode && ModelCreator.mirrorMode && viewportGizmoMirrorPartners != null && viewportGizmoMirrorBaseX != null) {
												Element partner = viewportGizmoMirrorPartners.get(e);
												if (partner != null) {
													double ldx = e.getStartX() - bx;
													double ldy = e.getStartY() - by;
													double ldz = e.getStartZ() - bz;
													at.vintagestory.modelcreator.enums.EnumAxis maxis = GetMirrorAxis(currentProject);
													double pdx = ldx, pdy = ldy, pdz = ldz;
													if (maxis == at.vintagestory.modelcreator.enums.EnumAxis.X) pdx = -ldx;
													if (maxis == at.vintagestory.modelcreator.enums.EnumAxis.Z) pdz = -ldz;
													Double pbx = viewportGizmoMirrorBaseX.get(partner);
													Double pby = viewportGizmoMirrorBaseY != null ? viewportGizmoMirrorBaseY.get(partner) : null;
													Double pbz = viewportGizmoMirrorBaseZ != null ? viewportGizmoMirrorBaseZ.get(partner) : null;
													if (pbx == null) pbx = partner.getStartX();
													if (pby == null) pby = partner.getStartY();
													if (pbz == null) pbz = partner.getStartZ();
													partner.setStartX(pbx + pdx);
													partner.setStartY(pby + pdy);
													partner.setStartZ(pbz + pdz);
												}
											}

if (viewportGizmoDragInKeyframeMode && kf != null) { SyncMirrorKeyframe(kf, viewportGizmoElems); }
										}

										updateValues(null);
										lastMouseX = newMouseX;
										lastMouseY = newMouseY;
										return;
									}
								}

							// Freedom mode: camera-relative movement, but computed ABSOLUTELY from drag start so snapping can work.
							if (viewportFreedomModeEnabled
									&& viewportGizmoBaseX != null && viewportGizmoBaseY != null && viewportGizmoBaseZ != null)
							{
								int ddx = newMouseX - mouseDownX;
								int ddy = newMouseY - mouseDownY;
								double dwx = 0, dwy = 0, dwz = 0;
								boolean allowX = false, allowY = false, allowZ = false;

								switch (viewportGizmoDragMode) {
									case 1: // X (camera-right on ground)
									{
										double s = (ddx * viewportGizmoAxisDirX + ddy * viewportGizmoAxisDirY) / 20.0;
										dwx = s * viewportGizmoFreeRightX;
										dwz = s * viewportGizmoFreeRightZ;
										allowX = true; allowZ = true;
										break;
									}
									case 2: // Y (world up)
									{
										double s = (ddx * viewportGizmoAxisDirX + ddy * viewportGizmoAxisDirY) / 20.0;
										dwy = s;
										allowY = true;
										break;
									}
									case 3: // Z (camera-forward on ground)
									{
										double s = (ddx * viewportGizmoAxisDirX + ddy * viewportGizmoAxisDirY) / 20.0;
										// Camera-forward on XZ plane is perpendicular to camera-right.
										double forwardX = -viewportGizmoFreeRightZ;
										double forwardZ = viewportGizmoFreeRightX;
										dwx = s * forwardX;
										dwz = s * forwardZ;
										allowX = true; allowZ = true;
										break;
									}
									case 4: // FREE (camera-right + world up)
									{
										double sx = ddx / 20.0;
										double sy = ddy / 20.0;
										dwx = sx * viewportGizmoFreeRightX + sy * viewportGizmoFreeUpX;
										dwy = sx * viewportGizmoFreeRightY + sy * viewportGizmoFreeUpY;
										dwz = sx * viewportGizmoFreeRightZ + sy * viewportGizmoFreeUpZ;
										allowX = true; allowY = true; allowZ = true;
										break;
									}
									default: break;
								}

								// Apply snapping in WORLD space relative to the drag-start pivot.
								double[] snapped = applyViewportMoveSnapping(dwx, dwy, dwz, allowX, allowY, allowZ);
								dwx = snapped[0]; dwy = snapped[1]; dwz = snapped[2];
								for (Element e : viewportGizmoElems)
								{
									if (e == null) continue;
									Double bx = viewportGizmoBaseX.get(e);
									Double by = viewportGizmoBaseY.get(e);
									Double bz = viewportGizmoBaseZ.get(e);
									if (bx == null || by == null || bz == null) continue;

									AnimFrameElement kf = (viewportGizmoDragInKeyframeMode && viewportGizmoKeyframeElems != null) ? viewportGizmoKeyframeElems.get(e) : null;
									double ldx = dwx, ldy = dwy, ldz = dwz;
									double[] map = viewportGizmoWorldAxisToLocal != null ? viewportGizmoWorldAxisToLocal.get(e) : null;
									if (map != null && map.length >= 9) {
										ldx = dwx * map[0] + dwy * map[3] + dwz * map[6];
										ldy = dwx * map[1] + dwy * map[4] + dwz * map[7];
										ldz = dwx * map[2] + dwy * map[5] + dwz * map[8];
									}

									double nx = bx + ldx;
									double ny = by + ldy;
									double nz = bz + ldz;

									// Optional floor snap (same semantics as before, but absolute).
									if (viewportFloorSnapEnabled && allowY && dwy < 0 && ny < by) {
										ny = by;
									}

									if (kf != null) { kf.setOffsetX(nx); kf.setOffsetY(ny); kf.setOffsetZ(nz); }
									else { e.setStartX(nx); e.setStartY(ny); e.setStartZ(nz); }
												if (viewportGizmoDragInKeyframeMode && kf != null) { SyncMirrorKeyframe(kf, viewportGizmoElems); }
								}

								// Ensure animation preview updates in real time while dragging in Keyframe mode.
								if (viewportGizmoDragInKeyframeMode && currentProject != null && currentProject.SelectedAnimation != null) {
									currentProject.SelectedAnimation.SetFramesDirty();
								}

								updateValues(null);
								lastMouseX = newMouseX;
								lastMouseY = newMouseY;
								return;
							}

								// (If freedom mode is on but we can't compute bases, fall through to legacy.)
								// Reuse the movement accumulators declared earlier in this method.
								moveX = moveY = moveZ = 0;

							// Legacy incremental movement (used as a safety fallback).
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
																if (viewportGizmoDragInKeyframeMode) { SyncMirrorKeyframe(kf, viewportGizmoElems); }
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
							// Ensure animation preview updates in real time while dragging in Keyframe mode.
							if (viewportGizmoDragInKeyframeMode && currentProject != null && currentProject.SelectedAnimation != null) {
								currentProject.SelectedAnimation.SetFramesDirty();
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
					boolean hitIsPartOfMultiSelection = (viewportGroupMoveEnabled || isViewportSelectionOutOfSync(currentProject))
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

			final int wheel = Mouse.getDWheel();
			if (wheel != 0)
			{
				boolean handledByUi = false;
				try {
					if (modelrenderer != null && modelrenderer.renderedLeftSidebar != null) {
						int mx = Mouse.getX();
						int my = canvHeight - Mouse.getY();
						handledByUi = modelrenderer.renderedLeftSidebar.handleMouseWheel(wheel, mx, my, canvHeight);
					}
				} catch (Throwable t) {
					handledByUi = false;
				}

				if (!handledByUi) {
					modelrenderer.camera.addZ(wheel * (cameraMod / 2500F));
				}
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
 * Like {@link #getElementGLNameAtPos(int, int)} but ignores any faces belonging to {@code ignoreElem}.
	 * This is important during face snapping: the moving element often sits in front of the target,
 * so the nearest pick hit would otherwise always be the moving element itself.
 */
private int getElementGLNameAtPosIgnoringElement(int x, int y, Element ignoreElem)
{
	IntBuffer selBuffer = ByteBuffer.allocateDirect(2048).order(ByteOrder.nativeOrder()).asIntBuffer();
	int[] buffer = new int[512];

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
		GLU.gluPickMatrix(x, y, 1, 1, IntBuffer.wrap(viewport));
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
	int hits = GL11.glRenderMode(GL11.GL_RENDER);

	selBuffer.get(buffer);
	if (hits <= 0) return -1;

	int bestName = -1;
	int bestDepth = Integer.MAX_VALUE;

	for (int i = 0; i < hits; i++)
	{
		int name = buffer[i * 4 + 3];
		if (name == 0) continue;

			// Resolve the owning element so we can ignore the moving element (and its children).
		Element owner = findElementByFaceOpenGlName(name);
		if (owner == null) continue;
			if (ignoreElem != null && isInSubtree(ignoreElem, owner)) continue;

		int depth = buffer[i * 4 + 1];
		if (depth < bestDepth) {
			bestDepth = depth;
			bestName = name;
		}
	}

	return bestName;
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
			// Track recent files (used by File -> Open Recent)
			addRecentFile(filePath);
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

	/**
	 * Returns the list of recent files (most recent first).
	 * Stored as a single pref key ("recentFiles") with newline-separated absolute paths.
	 */
	public static java.util.List<String> getRecentFiles()
	{
		String raw = prefs.get("recentFiles", "");
		java.util.ArrayList<String> list = new java.util.ArrayList<String>();
		if (raw == null || raw.length() == 0) return list;
		String[] parts = raw.split("\\n");
		for (String p : parts) {
			if (p == null) continue;
			String s = p.trim();
			if (s.length() == 0) continue;
			list.add(s);
		}
		return list;
	}

	/**
	 * Returns the list of recently saved files (most recent first).
	 * Stored as a single pref key ("recentlySavedFiles") with newline-separated absolute paths.
	 */
	public static java.util.List<String> getRecentlySavedFiles()
	{
		String raw = prefs.get("recentlySavedFiles", "");
		java.util.ArrayList<String> list = new java.util.ArrayList<String>();
		if (raw == null || raw.length() == 0) return list;
		String[] parts = raw.split("\\n");
		for (String p : parts) {
			if (p == null) continue;
			String s = p.trim();
			if (s.length() == 0) continue;
			list.add(s);
		}
		return list;
	}

	public static void addRecentlySavedFile(String filePath)
	{
		if (filePath == null) return;
		String path;
		try {
			path = new File(filePath).getAbsolutePath();
		} catch (Exception e) {
			path = filePath;
		}
		// Only track JSON model files
		String lower = path.toLowerCase();
		if (!lower.endsWith(".json")) return;

		boolean isWindows = java.io.File.separatorChar == '\\';
		java.util.List<String> list = getRecentlySavedFiles();
		// De-dup (case-insensitive on Windows)
		for (int i = list.size() - 1; i >= 0; i--) {
			String existing = list.get(i);
			if (existing == null) continue;
			boolean same = isWindows ? existing.equalsIgnoreCase(path) : existing.equals(path);
			if (same) list.remove(i);
		}
		list.add(0, path);
		while (list.size() > MAX_RECENT_FILES) list.remove(list.size() - 1);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			if (i > 0) sb.append("\n");
			sb.append(list.get(i));
		}
		prefs.put("recentlySavedFiles", sb.toString());
	}

	public static void addRecentFile(String filePath)
	{
		if (filePath == null) return;
		String path;
		try {
			path = new File(filePath).getAbsolutePath();
		} catch (Exception e) {
			path = filePath;
		}
		boolean isWindows = java.io.File.separatorChar == '\\';
		java.util.List<String> list = getRecentFiles();
		// De-dup (case-insensitive on Windows)
		for (int i = list.size() - 1; i >= 0; i--) {
			String existing = list.get(i);
			if (existing == null) continue;
			boolean same = isWindows ? existing.equalsIgnoreCase(path) : existing.equals(path);
			if (same) list.remove(i);
		}
		list.add(0, path);
		while (list.size() > MAX_RECENT_FILES) list.remove(list.size() - 1);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			if (i > 0) sb.append("\n");
			sb.append(list.get(i));
		}
		prefs.put("recentFiles", sb.toString());
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
		// Track recently saved files (used by File -> Open Recent -> Open Recently Saved)
		addRecentlySavedFile(file.getAbsolutePath());
		
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
