package at.vintagestory.modelcreator.gui.middle;

import static org.lwjgl.opengl.GL11.*;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.input.Mouse;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.newdawn.slick.Color;
import at.vintagestory.modelcreator.Camera;
import at.vintagestory.modelcreator.ModelCreator;
import at.vintagestory.modelcreator.Project;
import at.vintagestory.modelcreator.enums.EnumFonts;
import at.vintagestory.modelcreator.gui.left.LeftSidebar;
import at.vintagestory.modelcreator.interfaces.IDrawable;
import at.vintagestory.modelcreator.interfaces.IElementManager;
import at.vintagestory.modelcreator.model.Element;
import at.vintagestory.modelcreator.model.AnimationFrame;
import at.vintagestory.modelcreator.model.AnimFrameElement;
import at.vintagestory.modelcreator.util.GameMath;
import at.vintagestory.modelcreator.util.Mat4f;
import org.lwjgl.util.glu.Sphere;

public class ModelRenderer
{
	public Camera camera;
	private int width = 990, height = 700;
	public LeftSidebar renderedLeftSidebar = null;
	
	public IElementManager manager;
	
	Sphere sphere = new Sphere();
	
	public boolean renderDropTagets = false;
	public Point dropLocation;

	// Viewport toolbar (top right)
	// (Viewport toolbar removed: move scope toggle lives in the left sidebar now.)

	// Viewport marquee selection (Shift + drag in 3D viewport)
	public boolean viewportMarqueeActive = false;
	public int viewportMarqueeX1, viewportMarqueeY1, viewportMarqueeX2, viewportMarqueeY2;

	public void setViewportMarquee(boolean active, int x1, int y1, int x2, int y2) {
		this.viewportMarqueeActive = active;
		this.viewportMarqueeX1 = x1;
		this.viewportMarqueeY1 = y1;
		this.viewportMarqueeX2 = x2;
		this.viewportMarqueeY2 = y2;
	}


	
// Screen-aligned translate gizmo (overlay)
public boolean viewportMoveGizmoVisible = false;
// True when the shared gizmo pivot (center of selection bounds) is available.
public boolean viewportGizmoPivotValid = false;
// Center in window coordinates (origin bottom-left, same as Mouse.getY()).
// Keep both float (for stable drawing) and int (for hit-testing).
public float viewportMoveGizmoCenterXf = 0;
public float viewportMoveGizmoCenterYf = 0;
public float viewportMoveGizmoCenterDepth = 0;
public int viewportMoveGizmoCenterX = 0;
public int viewportMoveGizmoCenterY = 0;
// Layout sizes (pixels)
public int viewportMoveGizmoAxisLen = 110;
public int viewportMoveGizmoAxisThick = 10;
public int viewportMoveGizmoCenterRadius = 18;
// World-space anchor (center of selection bounds)
public float viewportMoveGizmoWorldX;
public float viewportMoveGizmoWorldY;
public float viewportMoveGizmoWorldZ;
// World-axis rotation gizmo (gimbal rings) overlay
public boolean viewportRotateGizmoVisible = false;
// Screen-aligned scale gizmo (overlay). Uses the same pivot as move/rotate.
public boolean viewportScaleGizmoVisible = false;
// Ring visual sizes (pixels, set from prefs each frame)
public int viewportRotateGizmoRingRadiusPx = 80;
public int viewportRotateGizmoRingThickPx = 6;

// Temporary buffers for projecting ring points
private final FloatBuffer viewportRotateGizmoTmpWinPos = BufferUtils.createFloatBuffer(3);
private final FloatBuffer viewportRotateGizmoUnprojA = BufferUtils.createFloatBuffer(3);
private final FloatBuffer viewportRotateGizmoUnprojB = BufferUtils.createFloatBuffer(3);

// Cached matrices/viewport from the 3D pass so we can project axes for world-axis mode
private final FloatBuffer viewportMoveGizmoModelMat = BufferUtils.createFloatBuffer(16);
private final FloatBuffer viewportMoveGizmoProjMat = BufferUtils.createFloatBuffer(16);
// LWJGL BufferChecks requires capacity >= 16 for glGetInteger(), even though GL_VIEWPORT returns 4 values.
// We only use the first 4 ints.
private final IntBuffer viewportMoveGizmoViewport = BufferUtils.createIntBuffer(16);

// Scratch buffers for unproject (avoid per-call allocations)
private final FloatBuffer viewportMoveGizmoUnprojNear = BufferUtils.createFloatBuffer(3);
private final FloatBuffer viewportMoveGizmoUnprojFar = BufferUtils.createFloatBuffer(3);

// Last known good axis directions (window space, origin bottom-left) to avoid jitter when projection degenerates
private final double[] viewportMoveGizmoLastAxisDirX = new double[] {0, 1, 0, -1};
private final double[] viewportMoveGizmoLastAxisDirY = new double[] {0, 0, 1, 0};

	/** Returns a normalized 2D direction in window coordinates (origin bottom-left). */
	
/** Returns a normalized 2D direction in window coordinates (origin bottom-left). */
public double[] getViewportMoveGizmoAxisDirWindow(int mode) {
	if (!viewportMoveGizmoVisible) return new double[] {0, 0};
	if (ModelCreator.viewportFreedomModeEnabled) {
		switch (mode) {
			case 1: return new double[] {1, 0};
			case 2: return new double[] {0, 1};
			case 3: return new double[] {0, 0}; // Z axis disabled in freedom mode
			default: return new double[] {0, 0};
		}
	}

	// World-axis mode: project world axes into screen space.
	float x0 = viewportMoveGizmoCenterXf;
	float y0 = viewportMoveGizmoCenterYf;
	FloatBuffer winPos = BufferUtils.createFloatBuffer(3);

	// Use a larger delta so we don't fall into tiny-vector precision issues at far zoom.
	final float delta = 4f;

	viewportMoveGizmoModelMat.rewind();
	viewportMoveGizmoProjMat.rewind();
	viewportMoveGizmoViewport.rewind();

	boolean ok;
	switch (mode) {
		case 1: // World X
			ok = GLU.gluProject(viewportMoveGizmoWorldX + delta, viewportMoveGizmoWorldY, viewportMoveGizmoWorldZ,
					viewportMoveGizmoModelMat, viewportMoveGizmoProjMat, viewportMoveGizmoViewport, winPos);
			break;
		case 2: // World Y
			ok = GLU.gluProject(viewportMoveGizmoWorldX, viewportMoveGizmoWorldY + delta, viewportMoveGizmoWorldZ,
					viewportMoveGizmoModelMat, viewportMoveGizmoProjMat, viewportMoveGizmoViewport, winPos);
			break;
		case 3: // World Z
			ok = GLU.gluProject(viewportMoveGizmoWorldX, viewportMoveGizmoWorldY, viewportMoveGizmoWorldZ + delta,
					viewportMoveGizmoModelMat, viewportMoveGizmoProjMat, viewportMoveGizmoViewport, winPos);
			break;
		default:
			return new double[] {0, 0};
	}

	// If projection fails, keep the last stable direction.
	if (!ok) {
		return new double[] { viewportMoveGizmoLastAxisDirX[mode], viewportMoveGizmoLastAxisDirY[mode] };
	}

	double x1 = winPos.get(0);
	double y1 = winPos.get(1);
	double dx = x1 - x0;
	double dy = y1 - y0;
	double len = Math.sqrt(dx*dx + dy*dy);

	// When an axis aligns with the view direction, the projection can collapse and "flip".
	// Clamp by reusing last known good direction instead of falling back to arbitrary constants.
	if (!Double.isFinite(len) || len < 2.0) {
		return new double[] { viewportMoveGizmoLastAxisDirX[mode], viewportMoveGizmoLastAxisDirY[mode] };
	}

	dx /= len;
	dy /= len;
	viewportMoveGizmoLastAxisDirX[mode] = dx;
	viewportMoveGizmoLastAxisDirY[mode] = dy;
	return new double[] { dx, dy };
}

	/**
	 * Returns a world-space mouse ray as {ox, oy, oz, dx, dy, dz}.
	 * Uses matrices captured in {@link #updateViewportMoveGizmo(int)}.
	 *
	 * Mouse coordinates must be in window space with origin bottom-left (LWJGL Mouse.getX/Y).
	 */
	public double[] getMouseRayWorld(int mouseX, int mouseY) {
		// Matrices/viewport are populated during the 3D render pass when the gizmo is updated.
		viewportMoveGizmoModelMat.rewind();
		viewportMoveGizmoProjMat.rewind();
		viewportMoveGizmoViewport.rewind();

		viewportMoveGizmoUnprojNear.clear();
		viewportMoveGizmoUnprojFar.clear();

		boolean ok1 = GLU.gluUnProject(mouseX, mouseY, 0f,
				viewportMoveGizmoModelMat, viewportMoveGizmoProjMat, viewportMoveGizmoViewport, viewportMoveGizmoUnprojNear);
		// Rewind because GLU reads position from the buffers
		viewportMoveGizmoModelMat.rewind();
		viewportMoveGizmoProjMat.rewind();
		viewportMoveGizmoViewport.rewind();
		boolean ok2 = GLU.gluUnProject(mouseX, mouseY, 1f,
				viewportMoveGizmoModelMat, viewportMoveGizmoProjMat, viewportMoveGizmoViewport, viewportMoveGizmoUnprojFar);

		if (!ok1 || !ok2) return null;

		viewportMoveGizmoUnprojNear.rewind();
		viewportMoveGizmoUnprojFar.rewind();
		double ox = viewportMoveGizmoUnprojNear.get(0);
		double oy = viewportMoveGizmoUnprojNear.get(1);
		double oz = viewportMoveGizmoUnprojNear.get(2);

		double fx = viewportMoveGizmoUnprojFar.get(0);
		double fy = viewportMoveGizmoUnprojFar.get(1);
		double fz = viewportMoveGizmoUnprojFar.get(2);

		double dx = fx - ox;
		double dy = fy - oy;
		double dz = fz - oz;
		double len = Math.sqrt(dx*dx + dy*dy + dz*dz);
		if (!Double.isFinite(len) || len < 1e-9) return null;
		dx /= len;
		dy /= len;
		dz /= len;
		return new double[] { ox, oy, oz, dx, dy, dz };
	}

	/** Hit test in window coordinates (origin bottom-left). Returns 0 none, 1 X, 2 Y, 3 Z, 4 center/free. */
	public int hitTestViewportMoveGizmo(int mouseX, int mouseY) {
		if (!viewportMoveGizmoVisible) return 0;
		int cx = viewportMoveGizmoCenterX;
		int cy = viewportMoveGizmoCenterY;
		if (ModelCreator.viewportFreedomModeEnabled && ModelCreator.viewportCenterCircleEnabled) {
			int r = viewportMoveGizmoCenterRadius + 4;
			int dx = mouseX - cx;
			int dy = mouseY - cy;
			if (dx*dx + dy*dy <= r*r) return 4;
		}

		int halfT = viewportMoveGizmoAxisThick / 2;
		int len = viewportMoveGizmoAxisLen;
		if (ModelCreator.viewportFreedomModeEnabled) {
			// Horizontal bar (X)
			if (mouseY >= cy - halfT && mouseY <= cy + halfT && mouseX >= cx - len && mouseX <= cx + len) {
				return 1;
			}
			// Vertical bar (Y)
			if (mouseX >= cx - halfT && mouseX <= cx + halfT && mouseY >= cy - len && mouseY <= cy + len) {
				return 2;
			}
			return 0;
		}

		// World-axis mode: project world axes and hit-test distance to their screen-space segments.
		double bestDist2 = Double.POSITIVE_INFINITY;
		int bestMode = 0;
		for (int m = 1; m <= 3; m++) {
			double[] dir = getViewportMoveGizmoAxisDirWindow(m);
			double dirx = dir[0];
			double diry = dir[1];
			// Segment endpoints
			double x1 = cx - dirx * len;
			double y1 = cy - diry * len;
			double x2 = cx + dirx * len;
			double y2 = cy + diry * len;
			double dist2 = distPointToSegmentSquared(mouseX, mouseY, x1, y1, x2, y2);
			if (dist2 <= (halfT * halfT) && dist2 < bestDist2) {
				bestDist2 = dist2;
				bestMode = m;
			}
		}
		return bestMode;
	}

	private static double distPointToSegmentSquared(double px, double py, double x1, double y1, double x2, double y2) {
		double vx = x2 - x1;
		double vy = y2 - y1;
		double wx = px - x1;
		double wy = py - y1;
		double c1 = vx * wx + vy * wy;
		if (c1 <= 0) {
			double dx = px - x1;
			double dy = py - y1;
			return dx*dx + dy*dy;
		}
		double c2 = vx * vx + vy * vy;
		if (c2 <= c1) {
			double dx = px - x2;
			double dy = py - y2;
			return dx*dx + dy*dy;
		}
		double b = c1 / c2;
		double bx = x1 + b * vx;
		double by = y1 + b * vy;
		double dx = px - bx;
		double dy = py - by;
		return dx*dx + dy*dy;
	}


	public ModelRenderer(IElementManager manager) {
		this.manager = manager;
	}

	public void Render(int leftSidebarWidth, int width, int height, int frameHeight) {
		this.width = width;
		this.height = height;

		// Keep viewport gizmo visuals in sync with user preferences
		viewportMoveGizmoAxisThick = Math.max(1, Math.min(80, ModelCreator.viewportAxisLineThickness));
		viewportMoveGizmoAxisLen = Math.max(20, Math.min(500, ModelCreator.viewportAxisLineLength));
		viewportMoveGizmoCenterRadius = Math.max(4, Math.min(80, ModelCreator.viewportCenterCircleRadius));

		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		GLU.gluPerspective(60F, (float) (width - leftSidebarWidth) / (float) height, 0.3F, 1000F);

		prepareDraw();
		
		drawGridAndElements();
		
		drawTreadmillGrid();

		// Update screen-space translate gizmo anchor (uses the current 3D matrices)
		updateViewportMoveGizmo(leftSidebarWidth);
		
		glDisable(GL_DEPTH_TEST);
		glDisable(GL_CULL_FACE);
		glDisable(GL_TEXTURE_2D);
		glDisable(GL_LIGHTING);

		glViewport(0, 0, width, height);
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		GLU.gluOrtho2D(0, width, height, 0);
		
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();

		renderLeftPane(leftSidebarWidth, frameHeight);	
		if (!ModelCreator.viewportGizmoHidden) {
			drawViewportMoveGizmoOverlay(leftSidebarWidth);
			drawViewportScaleGizmoOverlay(leftSidebarWidth);
			drawViewportRotateGizmoOverlay(leftSidebarWidth);
		}
		drawCompass();
		drawViewportMarquee(leftSidebarWidth);
		
		if (renderDropTagets) {
			renderDropTargets(width, height);
		}
	}

		// (viewport toolbar removed)
	
	
	public void prepareDraw()
	{
		glMatrixMode(GL_MODELVIEW);
		glEnable(GL_DEPTH_TEST);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		camera.useView();

		if (ModelCreator.darkMode){
			glClearColor(0.22F, 0.22F, 0.22F, 1.0F);
		} else {
			glClearColor(0.92F, 0.92F, 0.93F, 1.0F);
		}
	}

	private void renderDropTargets(int width, int height)
	{
		GL11.glPushMatrix();			
		int mouseX = -10;
		int mouseY = -10;
		
		if (dropLocation != null) {
			mouseX = dropLocation.x;
			mouseY = dropLocation.y - 40;
		}
		
		String texts[] = new String[] {
			"Only Load Texture",
			"Load Texture and Apply to Selected Element",
			"Load Texture and Apply to all Elements",
		};
		
		float[][] colors = new float[][] {
			new float[] { 0.8f, 0.8f, 1f,   0.6f, 0.6f, 0.9f },
			new float[] { 0.8f, 1.0f, 1f,   0.6f, 0.9f, 0.9f },
			new float[] { 1.0f, 0.8f, 1f,   0.9f, 0.6f, 0.9f },
		};
		
		for (int i = 0; i < 3; i++) {
			GL11.glEnable(GL11.GL_BLEND);
			
			float[] color = colors[i]; 
			
			if (mouseX < width && mouseY > 0 && mouseY >= 0 && mouseY < height / 3) {
				glColor4f(color[0], color[1], color[2], 0.85f);
			} else {
				glColor4f(color[3], color[4], color[5], 0.5f);	
			}
			
			glBegin(GL_QUADS);
			{
				glVertex2i(0, 0);
				glVertex2i(width, 0);
				glVertex2i(width, height/3);
				glVertex2i(0, height/3);
			}
			glEnd();
			
			
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			
			String text = texts[i];
			float strwdt = EnumFonts.BEBAS_NEUE_50.getWidth(text);
			float strhgt = EnumFonts.BEBAS_NEUE_50.getHeight(text);
			EnumFonts.BEBAS_NEUE_50.drawString((int)(width/2 - strwdt/2), (int)(height/3/2 - strhgt/2), text, new Color(0.3F, 0.3F, 0.3F));
			
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			
			GL11.glTranslated(0, height/3, 0);
			mouseY -= height/3;
			
		}
				
		GL11.glPopMatrix();
	}
	
	public static List<IDrawable> rootelems;
	public static boolean isMountRender;

	public void drawGridAndElements()
	{
		drawPerspectiveGrid();

		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glAlphaFunc(GL11.GL_GREATER, 0.05f);
		
		glTranslatef(-8, 0, -8);

		rootelems = getRootElementsForRender(ModelCreator.currentProject);
		if (rootelems == null) return;
		Element selectedElem = manager.getCurrentElement();

		isMountRender=false;
		if (ModelCreator.currentMountBackdropProject != null) {
			isMountRender=true;
			List<IDrawable> elems = getRootElementsForRender(ModelCreator.currentMountBackdropProject);
			int version = ModelCreator.currentMountBackdropProject.CurrentAnimVersion();
			for (int i = 0; i < elems.size(); i++) {
				elems.get(i).draw(selectedElem, false, version);
			}	
		} 
		else	
		{	
			int version = ModelCreator.currentProject.CurrentAnimVersion();
			for (int i = 0; i < rootelems.size(); i++)
			{
				rootelems.get(i).draw(selectedElem, false, version);
			}
		}
		
		
		
		if (ModelCreator.currentBackdropProject != null) {
			List<IDrawable> elems = getRootElementsForRender(ModelCreator.currentBackdropProject);
			int version = ModelCreator.currentBackdropProject.CurrentAnimVersion();
			for (int i = 0; i < elems.size(); i++) {
				elems.get(i).draw(selectedElem, false, version);
			}
		}
		
		
		GL11.glDisable(GL11.GL_ALPHA_TEST);

		
		if (!ModelCreator.showGrid) return;
		
		GL11.glPushMatrix();
		{
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glShadeModel(GL11.GL_SMOOTH);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

			GL11.glPushMatrix();
			{
				GL11.glTranslated(0, 0, 16);
				GL11.glScaled(0.018, 0.018, 0.018);
				GL11.glRotated(90, 1, 0, 0);
				EnumFonts.BEBAS_NEUE_50.drawString(8, 0, "VS Model Creator", new Color(0.5F, 0.5F, 0.6F));
			}
			GL11.glPopMatrix();
			
			if (ModelCreator.currentProject.EntityTextureMode) {
				GL11.glTranslated(-1, 0, 8 + 2);
				GL11.glScaled(0.018, 0.018, 0.018);
				GL11.glRotated(90, 0, 1, 0);
				GL11.glRotated(90, 1, 0, 0);
				EnumFonts.BEBAS_NEUE_50.drawString(8, 0, "Entity front", new Color(0.5F, 0.5F, 0.6F));
			}

			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glShadeModel(GL11.GL_SMOOTH);
			GL11.glDisable(GL11.GL_BLEND);
		}
		GL11.glPopMatrix();
	}
	


	public void drawPerspectiveGrid()
	{
		if (!ModelCreator.showGrid) return;
		if (ModelCreator.showTreadmill) return;

		// User-adjustable tiling: 1 = original single 16x16 tile
		int tilesX = Math.max(1, Math.min(10, ModelCreator.viewportFloorGridCols));
		int tilesZ = Math.max(1, Math.min(10, ModelCreator.viewportFloorGridRows));
		int sizeX = 16 * tilesX;
		int sizeZ = 16 * tilesZ;
		int cx = sizeX / 2;
		int cz = sizeZ / 2;
		
		float d1 = 0.8f;
		float d2 = 0.6f;
		if (ModelCreator.darkMode) { d1 = 1.2f; d2 = 1.4f; }
		
		glPushMatrix();
		{
			glColor3f(0.55F, 0.55F, 0.60F);
			glTranslatef(-cx, 0, -cz);

			// Thin inside lines
			glLineWidth(1F);
			glBegin(GL_LINES);
			{
				for (int i = 1; i <= sizeX; i++)
				{
					glVertex3i(i, 0, 0);
					glVertex3i(i, 0, sizeZ);
				}

				for (int i = 1; i <= sizeZ; i++)
				{
					glVertex3i(0, 0, i);
					glVertex3i(sizeX, 0, i);
				}
			}
			glEnd();
			
			
			glColor3f(0.55F * d2, 0.55F * d2, 0.60F * d2);
			
			// Bold outside lines
			glLineWidth(2F);
			glBegin(GL_LINES);
			{
				glVertex3i(0, 0, 0);
				glVertex3i(0, 0, sizeZ);
				glVertex3i(sizeX, 0, 0);
				glVertex3i(sizeX, 0, sizeZ);
				glVertex3i(0, 0, sizeZ);
				glVertex3i(sizeX, 0, sizeZ);
				glVertex3i(0, 0, 0);
				glVertex3i(sizeX, 0, 0);
			}
			glEnd();

			glColor3f(0.55F * d1, 0.55F * d1, 0.60F * d1);
			
			// Bold center cross
			glLineWidth(2F);
			glBegin(GL_LINES);
			{
				glVertex3i(cx, 0, 0);
				glVertex3i(cx, 0, sizeZ);
				glVertex3i(0, 0, cz);
				glVertex3i(sizeX, 0, cz);
			}
			glEnd();

			glColor3f(0.55F, 0.55F, 0.60F);
			
			// Thin half transparent line to show block size
			glLineWidth(1F);
			glColor3f(0.55F / d1, 0.55F / d1, 0.55F / d1);
			
			
			glBegin(GL_LINES);
			{
				glVertex3i(0, 0, 0);
				glVertex3i(0, 16, 0);
				
				glVertex3i(sizeX, 0, 0);
				glVertex3i(sizeX, 16, 0);

				glVertex3i(sizeX, 0, sizeZ);
				glVertex3i(sizeX, 16, sizeZ);
				
				glVertex3i(0, 0, sizeZ);
				glVertex3i(0, 16, sizeZ);
				
				glVertex3i(0, 16, 0);
				glVertex3i(sizeX, 16, 0);
				
				glVertex3i(sizeX, 16, 0);
				glVertex3i(sizeX, 16, sizeZ);
				
				glVertex3i(sizeX, 16, sizeZ);
				glVertex3i(0, 16, sizeZ);
				
				glVertex3i(0, 16, sizeZ);
				glVertex3i(0, 16, 0);
			}
			glEnd();
		}

// Optional overlays: show VS block boundaries on the floor and in the air
boolean showBlocksFloor = ModelCreator.viewportShowBlocksFloor;
boolean showBlocksAir = ModelCreator.viewportShowBlocksAir;
int airAbove = Math.max(0, Math.min(10, ModelCreator.viewportAirBlocksAbove));
int airBelow = Math.max(0, Math.min(10, ModelCreator.viewportAirBlocksBelow));

if (showBlocksFloor)
{
	// Bold 16x16 block outlines on the floor plane
	glLineWidth(2F);
	glColor3f(0.55F * d1, 0.55F * d1, 0.60F * d1);
	glBegin(GL_LINES);
	{
		for (int x = 0; x <= sizeX; x += 16)
		{
			glVertex3i(x, 0, 0);
			glVertex3i(x, 0, sizeZ);
		}
		for (int z = 0; z <= sizeZ; z += 16)
		{
			glVertex3i(0, 0, z);
			glVertex3i(sizeX, 0, z);
		}
	}
	glEnd();
}

if (showBlocksAir && (airAbove > 0 || airBelow > 0))
{
	// Slight Y offset to reduce z-fighting with the main grid lines
	float eps = 0.01f;

	// Above-grid outlines (same theme color)
	if (airAbove > 0)
	{
		int maxY = 16 * airAbove;
		glLineWidth(1.2F);
		glColor3f(0.55F * d1, 0.55F * d1, 0.60F * d1);

		glBegin(GL_LINES);
		{
			// Vertical edges at block corners
			for (int x = 0; x <= sizeX; x += 16)
			{
				for (int z = 0; z <= sizeZ; z += 16)
				{
					glVertex3f(x, 0 + eps, z);
					glVertex3f(x, maxY + eps, z);
				}
			}

			// Horizontal outlines per block layer
			for (int layer = 0; layer <= airAbove; layer++)
			{
				float y = 16 * layer + eps;

				for (int x = 0; x <= sizeX; x += 16)
				{
					glVertex3f(x, y, 0);
					glVertex3f(x, y, sizeZ);
				}
				for (int z = 0; z <= sizeZ; z += 16)
				{
					glVertex3f(0, y, z);
					glVertex3f(sizeX, y, z);
				}
			}
		}
		glEnd();
	}

	// Below-grid outlines (orange/red tint for "below ground")
	if (airBelow > 0)
	{
		int minY = -16 * airBelow;
		glLineWidth(1.2F);
		glColor3f(0.90F, 0.45F, 0.25F);

		glBegin(GL_LINES);
		{
			// Vertical edges at block corners
			for (int x = 0; x <= sizeX; x += 16)
			{
				for (int z = 0; z <= sizeZ; z += 16)
				{
					glVertex3f(x, 0 - eps, z);
					glVertex3f(x, minY - eps, z);
				}
			}

			// Horizontal outlines per block layer
			for (int layer = 0; layer <= airBelow; layer++)
			{
				float y = -16 * layer - eps;

				for (int x = 0; x <= sizeX; x += 16)
				{
					glVertex3f(x, y, 0);
					glVertex3f(x, y, sizeZ);
				}
				for (int z = 0; z <= sizeZ; z += 16)
				{
					glVertex3f(0, y, z);
					glVertex3f(sizeX, y, z);
				}
			}
		}
		glEnd();
	}
}


		glPopMatrix();
	}


	public void renderLeftPane(int sidebarWidth, int frameHeight)
	{
		
		glPushMatrix();
		{
			if (ModelCreator.darkMode) {
				glColor3f(0.08F, 0.08F, 0.08F);	
			} else {
				glColor3f(0.58F, 0.58F, 0.58F);
			}
			
			glLineWidth(2F);
			glBegin(GL_LINES);
			{
				glVertex2i(sidebarWidth, 0);
				glVertex2i(width, 0);
				glVertex2i(width, 0);
				glVertex2i(width, height);
				glVertex2i(sidebarWidth, height);
				glVertex2i(sidebarWidth, 0);
				glVertex2i(sidebarWidth, height);
				glVertex2i(width, height);
			}
			glEnd();
		}
		glPopMatrix();

		if (renderedLeftSidebar != null) {
			renderedLeftSidebar.draw(sidebarWidth, width, height, frameHeight);
		}
	}
	
	


	private void updateViewportMoveGizmo(int leftSidebarWidth)
	{
		viewportMoveGizmoVisible = false;
		viewportRotateGizmoVisible = false;
		viewportScaleGizmoVisible = false;
		viewportGizmoPivotValid = false;
		Project project = ModelCreator.currentProject;
		if (project == null || project.SelectedElement == null) return;
		if (ModelCreator.renderAttachmentPoints) return;
		// Hide in UV/Face tab (this overlay gizmo is for viewport translate)
		if (ModelCreator.currentRightTab == 1) return;

		boolean useMulti = project.SelectedElements != null && project.SelectedElements.size() > 1
						&& (ModelCreator.viewportGroupMoveEnabled
																	|| ModelCreator.isViewportSelectionOutOfSync(project)
																	|| ModelCreator.viewportGizmoMode == ModelCreator.VIEWPORT_GIZMO_MODE_SCALE);
		List<Element> elems = useMulti ? project.SelectedElements : java.util.Collections.singletonList(project.SelectedElement);

		// IMPORTANT: Elements may be parented and/or rotated. Computing the gizmo pivot purely from local
		// StartX/StartY/StartZ causes the gizmo to drift or get "left behind" when moving multiple
		// elements with different parent transforms. We therefore compute bounds in ROOT space by
		// applying each element's full transform chain (including keyframe offsets when applicable).
		double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

		boolean keyframeTab = (ModelCreator.currentRightTab == 2 && project.SelectedAnimation != null && ModelCreator.leftKeyframesPanel != null && ModelCreator.leftKeyframesPanel.isVisible());
		AnimationFrame curFrame = null;
		if (keyframeTab) {
			int cf = project.SelectedAnimation.currentFrame;
			if (project.SelectedAnimation.allFrames != null && cf >= 0 && cf < project.SelectedAnimation.allFrames.size()) {
				curFrame = project.SelectedAnimation.allFrames.get(cf);
			}
		}

		for (Element e : elems) {
			if (e == null) continue;
			double[] b = computeElementRootBounds(e, curFrame);
			if (b == null) continue;
			minX = Math.min(minX, b[0]);
			minY = Math.min(minY, b[1]);
			minZ = Math.min(minZ, b[2]);
			maxX = Math.max(maxX, b[3]);
			maxY = Math.max(maxY, b[4]);
			maxZ = Math.max(maxZ, b[5]);
		}
		if (!Double.isFinite(minX) || !Double.isFinite(maxX)) return;

		float cx = (float)((minX + maxX) / 2.0);
		float cy = (float)((minY + maxY) / 2.0);
		float cz = (float)((minZ + maxZ) / 2.0);

		viewportMoveGizmoWorldX = cx;
		viewportMoveGizmoWorldY = cy;
		viewportMoveGizmoWorldZ = cz;

		// Project into window coordinates (cache matrices for world-axis projection).
		viewportMoveGizmoModelMat.clear();
		viewportMoveGizmoProjMat.clear();
		glGetFloat(GL_MODELVIEW_MATRIX, viewportMoveGizmoModelMat);
		glGetFloat(GL_PROJECTION_MATRIX, viewportMoveGizmoProjMat);

		viewportMoveGizmoViewport.clear();
		GL11.glGetInteger(GL11.GL_VIEWPORT, viewportMoveGizmoViewport);
		viewportMoveGizmoViewport.rewind();

		FloatBuffer winPos = BufferUtils.createFloatBuffer(3);
		viewportMoveGizmoModelMat.rewind();
		viewportMoveGizmoProjMat.rewind();
		viewportMoveGizmoViewport.rewind();
		boolean ok = GLU.gluProject(cx, cy, cz, viewportMoveGizmoModelMat, viewportMoveGizmoProjMat, viewportMoveGizmoViewport, winPos);
		if (!ok) return;

		float wx = winPos.get(0);
		float wy = winPos.get(1);
		float wz = winPos.get(2);
		viewportMoveGizmoCenterXf = wx;
		viewportMoveGizmoCenterYf = wy;
		viewportMoveGizmoCenterDepth = wz;
		if (wz < 0f || wz > 1f) return;

		int ix = Math.round(wx);
		int iy = Math.round(wy);

		if (ix < leftSidebarWidth || ix > width) return;
		if (iy < 0 || iy > height) return;

		viewportMoveGizmoCenterX = ix;
		viewportMoveGizmoCenterY = iy;
		viewportGizmoPivotValid = true;


		// Only show one gizmo overlay at a time (Move or Rotate), unless the user has hidden the gizmo.
		if (ModelCreator.viewportGizmoHidden) {
			viewportMoveGizmoVisible = false;
			viewportRotateGizmoVisible = false;
			return;
		}
		if (ModelCreator.viewportGizmoMode == ModelCreator.VIEWPORT_GIZMO_MODE_ROTATE) {
			viewportRotateGizmoVisible = true;
			viewportMoveGizmoVisible = false;
			viewportScaleGizmoVisible = false;
		} else if (ModelCreator.viewportGizmoMode == ModelCreator.VIEWPORT_GIZMO_MODE_SCALE) {
			viewportScaleGizmoVisible = true;
			viewportMoveGizmoVisible = false;
			viewportRotateGizmoVisible = false;
		} else {
			viewportMoveGizmoVisible = true;
			viewportRotateGizmoVisible = false;
			viewportScaleGizmoVisible = false;
		}
	}

	/**
	 * Computes an element's axis-aligned bounds in ROOT space by applying the full parent->child
	 * transform chain. When a keyframe frame is provided, animation offsets are applied to the
	 * Start translation for any element that has a matching AnimFrameElement.
	 */
	private double[] computeElementRootBounds(Element elem, AnimationFrame curFrame)
	{
		if (elem == null) return null;

		float[] mat = Mat4f.Identity_(new float[16]);

		// Apply parent transforms first (root -> parent)
		List<Element> path = elem.GetParentPath();
		if (path != null) {
			for (Element p : path) {
				applyTransformWithAnimOffset(mat, p, curFrame);
			}
		}
		// Apply this element's transform
		applyTransformWithAnimOffset(mat, elem, curFrame);

		double w = elem.getWidth();
		double h = elem.getHeight();
		double d = elem.getDepth();

		// Transform the 8 corners of the local AABB (0..w, 0..h, 0..d)
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

	private void applyTransformWithAnimOffset(float[] mat, Element elem, AnimationFrame curFrame)
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

	private void drawViewportMoveGizmoOverlay(int leftSidebarWidth)
	{
		if (!viewportMoveGizmoVisible) return;
		// Do not draw under the left UI
		if (viewportMoveGizmoCenterX < leftSidebarWidth) return;

		int cx = viewportMoveGizmoCenterX;
		int cyTop = height - viewportMoveGizmoCenterY; // Ortho is top-left origin

		int len = viewportMoveGizmoAxisLen;
		int thick = viewportMoveGizmoAxisThick;
		int halfT = thick / 2;
		int r = viewportMoveGizmoCenterRadius;

		glPushMatrix();
		glDisable(GL_TEXTURE_2D);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glDisable(GL_DEPTH_TEST);

		int ah = thick + 6;

		if (ModelCreator.viewportFreedomModeEnabled) {
			// Freedom mode: screen-aligned handles (camera-relative movement).
			// X axis (red)
			glColor4f(1f, 0.25f, 0.25f, 0.92f);
			glBegin(GL_QUADS);
			{
				glVertex2i(cx - len, cyTop - halfT);
				glVertex2i(cx + len, cyTop - halfT);
				glVertex2i(cx + len, cyTop + halfT);
				glVertex2i(cx - len, cyTop + halfT);
			}
			glEnd();
			glBegin(GL_TRIANGLES);
			{
				glVertex2i(cx - len - ah, cyTop);
				glVertex2i(cx - len, cyTop - ah/2);
				glVertex2i(cx - len, cyTop + ah/2);
				glVertex2i(cx + len + ah, cyTop);
				glVertex2i(cx + len, cyTop - ah/2);
				glVertex2i(cx + len, cyTop + ah/2);
			}
			glEnd();

			// Y axis (green)
			glColor4f(0.25f, 1f, 0.25f, 0.92f);
			glBegin(GL_QUADS);
			{
				glVertex2i(cx - halfT, cyTop - len);
				glVertex2i(cx + halfT, cyTop - len);
				glVertex2i(cx + halfT, cyTop + len);
				glVertex2i(cx - halfT, cyTop + len);
			}
			glEnd();
			glBegin(GL_TRIANGLES);
			{
				glVertex2i(cx, cyTop - len - ah);
				glVertex2i(cx - ah/2, cyTop - len);
				glVertex2i(cx + ah/2, cyTop - len);
			}
			glEnd();

			if (ModelCreator.viewportCenterCircleEnabled) {
				// Center free-move handle
				glColor4f(0.15f, 0.15f, 0.15f, 0.80f);
				glBegin(GL_TRIANGLE_FAN);
				{
					glVertex2i(cx, cyTop);
					int steps = 28;
					for (int i = 0; i <= steps; i++) {
						double a = (Math.PI * 2.0) * (i / (double)steps);
						int px = cx + (int)Math.round(Math.cos(a) * r);
						int py = cyTop + (int)Math.round(Math.sin(a) * r);
						glVertex2i(px, py);
					}
				}
				glEnd();
				glColor4f(0.95f, 0.95f, 1f, 0.70f);
				glLineWidth(2f);
				glBegin(GL_LINE_LOOP);
				{
					int steps = 32;
					for (int i = 0; i < steps; i++) {
						double a = (Math.PI * 2.0) * (i / (double)steps);
						int px = cx + (int)Math.round(Math.cos(a) * r);
						int py = cyTop + (int)Math.round(Math.sin(a) * r);
						glVertex2i(px, py);
					}
				}
				glEnd();
			}
		} else {
			// World-axis mode: draw world X/Y/Z axes projected into screen space.
			for (int m = 1; m <= 3; m++) {
				double[] dirWin = getViewportMoveGizmoAxisDirWindow(m);
				double dirx = dirWin[0];
				double diry = -dirWin[1]; // convert window(up) -> ortho(down)
				// Segment endpoints
				double x1 = cx - dirx * len;
				double y1 = cyTop - diry * len;
				double x2 = cx + dirx * len;
				double y2 = cyTop + diry * len;
				// Perpendicular for thickness
				double perpx = -diry;
				double perpy = dirx;
				double ox = perpx * halfT;
				double oy = perpy * halfT;

				switch (m) {
					case 1: glColor4f(1f, 0.25f, 0.25f, 0.92f); break;
					case 2: glColor4f(0.25f, 1f, 0.25f, 0.92f); break;
					case 3: glColor4f(0.25f, 0.55f, 1f, 0.92f); break;
				}

				glBegin(GL_QUADS);
				{
					glVertex2i((int)Math.round(x1 + ox), (int)Math.round(y1 + oy));
					glVertex2i((int)Math.round(x1 - ox), (int)Math.round(y1 - oy));
					glVertex2i((int)Math.round(x2 - ox), (int)Math.round(y2 - oy));
					glVertex2i((int)Math.round(x2 + ox), (int)Math.round(y2 + oy));
				}
				glEnd();

				// Arrowheads at both ends
				int tip2x = (int)Math.round(x2 + dirx * ah);
				int tip2y = (int)Math.round(y2 + diry * ah);
				int b21x = (int)Math.round(x2 + perpx * (ah / 2.0));
				int b21y = (int)Math.round(y2 + perpy * (ah / 2.0));
				int b22x = (int)Math.round(x2 - perpx * (ah / 2.0));
				int b22y = (int)Math.round(y2 - perpy * (ah / 2.0));
				int tip1x = (int)Math.round(x1 - dirx * ah);
				int tip1y = (int)Math.round(y1 - diry * ah);
				int b11x = (int)Math.round(x1 + perpx * (ah / 2.0));
				int b11y = (int)Math.round(y1 + perpy * (ah / 2.0));
				int b12x = (int)Math.round(x1 - perpx * (ah / 2.0));
				int b12y = (int)Math.round(y1 - perpy * (ah / 2.0));
				glBegin(GL_TRIANGLES);
				{
					glVertex2i(tip2x, tip2y);
					glVertex2i(b21x, b21y);
					glVertex2i(b22x, b22y);
					glVertex2i(tip1x, tip1y);
					glVertex2i(b11x, b11y);
					glVertex2i(b12x, b12y);
				}
				glEnd();
			}
		}


		glDisable(GL_BLEND);
		glPopMatrix();
	}

	/**
	 * Screen-space scale gizmo overlay. Visually similar to the move gizmo, but with
	 * square handles and a center square for uniform scaling.
	 */
	private void drawViewportScaleGizmoOverlay(int leftSidebarWidth)
	{
		if (!viewportScaleGizmoVisible) return;
		if (viewportMoveGizmoCenterX < leftSidebarWidth) return;

		int cx = viewportMoveGizmoCenterX;
		int cyTop = height - viewportMoveGizmoCenterY; // Ortho is top-left origin

		int len = viewportMoveGizmoAxisLen;
		int thick = viewportMoveGizmoAxisThick;
		int halfT = thick / 2;
		int handle = Math.max(10, thick + 8);
		int hh = handle / 2;

		glPushMatrix();
		glDisable(GL_TEXTURE_2D);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glDisable(GL_DEPTH_TEST);

		// Center uniform handle
		glColor4f(0.15f, 0.15f, 0.15f, 0.88f);
		glBegin(GL_QUADS);
		{
			glVertex2i(cx - hh, cyTop - hh);
			glVertex2i(cx + hh, cyTop - hh);
			glVertex2i(cx + hh, cyTop + hh);
			glVertex2i(cx - hh, cyTop + hh);
		}
		glEnd();
		glColor4f(0.95f, 0.95f, 1f, 0.75f);
		glLineWidth(2f);
		glBegin(GL_LINE_LOOP);
		{
			glVertex2i(cx - hh, cyTop - hh);
			glVertex2i(cx + hh, cyTop - hh);
			glVertex2i(cx + hh, cyTop + hh);
			glVertex2i(cx - hh, cyTop + hh);
		}
		glEnd();

		if (ModelCreator.viewportFreedomModeEnabled) {
			// Freedom mode: screen-aligned X/Y only.
			// X axis (red)
			glColor4f(1f, 0.25f, 0.25f, 0.78f);
			glBegin(GL_QUADS);
			{
				glVertex2i(cx - len, cyTop - halfT);
				glVertex2i(cx + len, cyTop - halfT);
				glVertex2i(cx + len, cyTop + halfT);
				glVertex2i(cx - len, cyTop + halfT);
			}
			glEnd();
			// Handle squares
			glColor4f(1f, 0.25f, 0.25f, 0.92f);
			drawScaleHandleSquare(cx - len, cyTop, hh);
			drawScaleHandleSquare(cx + len, cyTop, hh);

			// Y axis (green)
			glColor4f(0.25f, 1f, 0.25f, 0.78f);
			glBegin(GL_QUADS);
			{
				glVertex2i(cx - halfT, cyTop - len);
				glVertex2i(cx + halfT, cyTop - len);
				glVertex2i(cx + halfT, cyTop + len);
				glVertex2i(cx - halfT, cyTop + len);
			}
			glEnd();
			glColor4f(0.25f, 1f, 0.25f, 0.92f);
			drawScaleHandleSquare(cx, cyTop - len, hh);
			drawScaleHandleSquare(cx, cyTop + len, hh);
		} else {
			// World-axis mode: draw projected world axes with square handles.
			for (int m = 1; m <= 3; m++) {
				double[] dirWin = getViewportMoveGizmoAxisDirWindow(m);
				double dirx = dirWin[0];
				double diry = -dirWin[1]; // convert window(up) -> ortho(down)

				double x1 = cx - dirx * len;
				double y1 = cyTop - diry * len;
				double x2 = cx + dirx * len;
				double y2 = cyTop + diry * len;

				double perpx = -diry;
				double perpy = dirx;
				double ox = perpx * halfT;
				double oy = perpy * halfT;

				switch (m) {
					case 1: glColor4f(1f, 0.25f, 0.25f, 0.78f); break;
					case 2: glColor4f(0.25f, 1f, 0.25f, 0.78f); break;
					case 3: glColor4f(0.25f, 0.55f, 1f, 0.78f); break;
				}

				glBegin(GL_QUADS);
				{
					glVertex2i((int)Math.round(x1 + ox), (int)Math.round(y1 + oy));
					glVertex2i((int)Math.round(x1 - ox), (int)Math.round(y1 - oy));
					glVertex2i((int)Math.round(x2 - ox), (int)Math.round(y2 - oy));
					glVertex2i((int)Math.round(x2 + ox), (int)Math.round(y2 + oy));
				}
				glEnd();

				// Handles at both ends
				switch (m) {
					case 1: glColor4f(1f, 0.25f, 0.25f, 0.92f); break;
					case 2: glColor4f(0.25f, 1f, 0.25f, 0.92f); break;
					case 3: glColor4f(0.25f, 0.55f, 1f, 0.92f); break;
				}
				drawScaleHandleSquare((int)Math.round(x1), (int)Math.round(y1), hh);
				drawScaleHandleSquare((int)Math.round(x2), (int)Math.round(y2), hh);
			}
		}

		glDisable(GL_BLEND);
		glPopMatrix();
	}

	private void drawScaleHandleSquare(int x, int y, int hh)
	{
		glBegin(GL_QUADS);
		{
			glVertex2i(x - hh, y - hh);
			glVertex2i(x + hh, y - hh);
			glVertex2i(x + hh, y + hh);
			glVertex2i(x - hh, y + hh);
		}
		glEnd();
	}

	/** Hit test in window coordinates (origin bottom-left). Returns 0 none, 1 X, 2 Y, 3 Z, 4 uniform(center). */
	public int hitTestViewportScaleGizmo(int mouseX, int mouseY) {
		if (!viewportScaleGizmoVisible || !viewportGizmoPivotValid) return 0;
		int cx = viewportMoveGizmoCenterX;
		int cy = viewportMoveGizmoCenterY;
		int thick = viewportMoveGizmoAxisThick;
		int handle = Math.max(10, thick + 8);
		int hh = handle / 2;

		// Center uniform handle
		if (Math.abs(mouseX - cx) <= hh && Math.abs(mouseY - cy) <= hh) return 4;

		int len = viewportMoveGizmoAxisLen;
		if (ModelCreator.viewportFreedomModeEnabled) {
			// X handles
			if (Math.abs(mouseX - (cx - len)) <= hh && Math.abs(mouseY - cy) <= hh) return 1;
			if (Math.abs(mouseX - (cx + len)) <= hh && Math.abs(mouseY - cy) <= hh) return 1;
			// Y handles
			if (Math.abs(mouseX - cx) <= hh && Math.abs(mouseY - (cy - len)) <= hh) return 2;
			if (Math.abs(mouseX - cx) <= hh && Math.abs(mouseY - (cy + len)) <= hh) return 2;
			return 0;
		}

		// World-axis projected handles
		for (int m = 1; m <= 3; m++) {
			double[] dirWin = getViewportMoveGizmoAxisDirWindow(m);
			double dirx = dirWin[0];
			double diry = dirWin[1];
			int x1 = (int)Math.round(cx - dirx * len);
			int y1 = (int)Math.round(cy - diry * len);
			int x2 = (int)Math.round(cx + dirx * len);
			int y2 = (int)Math.round(cy + diry * len);
			if (Math.abs(mouseX - x1) <= hh && Math.abs(mouseY - y1) <= hh) return m;
			if (Math.abs(mouseX - x2) <= hh && Math.abs(mouseY - y2) <= hh) return m;
		}

		return 0;
	}

/** Hit test in window coordinates (origin bottom-left). Returns 0 none, 1 X ring, 2 Y ring, 3 Z ring. */
public int hitTestViewportRotateGizmo(int mouseX, int mouseY) {
	if (!viewportRotateGizmoVisible || !viewportGizmoPivotValid) return 0;
	// Do not interact under left UI
	if (mouseX < 0 || mouseX < 0) {}
	int cx = viewportMoveGizmoCenterX;
	int cy = viewportMoveGizmoCenterY;
	// Ignore if clicking too close to center (let move gizmo center handle win)
	int inner = Math.max(6, viewportMoveGizmoCenterRadius + 4);
	double dcx = mouseX - cx;
	double dcy = mouseY - cy;
	if (dcx*dcx + dcy*dcy <= inner*inner) return 0;

	// Ring size in world units (derived from desired pixel radius at pivot depth)
	viewportRotateGizmoRingThickPx = Math.max(2, Math.min(30, viewportMoveGizmoAxisThick / 2));
	viewportRotateGizmoRingRadiusPx = Math.max(30, Math.min(600, viewportMoveGizmoAxisLen + 22));
	double radiusWorld = getWorldRadiusForPixelRadius(viewportRotateGizmoRingRadiusPx);
	if (!Double.isFinite(radiusWorld) || radiusWorld <= 0) radiusWorld = 1;

	double bestDist2 = Double.POSITIVE_INFINITY;
	int bestAxis = 0;
	double thr = Math.max(5, viewportRotateGizmoRingThickPx + 4);
	double thr2 = thr * thr;

	for (int axis = 1; axis <= 3; axis++) {
		double minDist2 = dist2ToProjectedRing(mouseX, mouseY, axis, (float)radiusWorld);
		if (minDist2 <= thr2 && minDist2 < bestDist2) {
			bestDist2 = minDist2;
			bestAxis = axis;
		}
	}
	return bestAxis;
}

private double dist2ToProjectedRing(int mouseX, int mouseY, int axis, float radiusWorld) {
	final int steps = 64;
	double best = Double.POSITIVE_INFINITY;

	float px = viewportMoveGizmoWorldX;
	float py = viewportMoveGizmoWorldY;
	float pz = viewportMoveGizmoWorldZ;

	for (int i = 0; i < steps; i++) {
		double a0 = (Math.PI * 2.0) * (i / (double)steps);
		double a1 = (Math.PI * 2.0) * ((i + 1) / (double)steps);
		float x0, y0, z0, x1, y1, z1;
		if (axis == 1) {
			x0 = px; y0 = py + (float)Math.cos(a0) * radiusWorld; z0 = pz + (float)Math.sin(a0) * radiusWorld;
			x1 = px; y1 = py + (float)Math.cos(a1) * radiusWorld; z1 = pz + (float)Math.sin(a1) * radiusWorld;
		} else if (axis == 2) {
			x0 = px + (float)Math.cos(a0) * radiusWorld; y0 = py; z0 = pz + (float)Math.sin(a0) * radiusWorld;
			x1 = px + (float)Math.cos(a1) * radiusWorld; y1 = py; z1 = pz + (float)Math.sin(a1) * radiusWorld;
		} else {
			x0 = px + (float)Math.cos(a0) * radiusWorld; y0 = py + (float)Math.sin(a0) * radiusWorld; z0 = pz;
			x1 = px + (float)Math.cos(a1) * radiusWorld; y1 = py + (float)Math.sin(a1) * radiusWorld; z1 = pz;
		}
		float[] w0 = projectWorldToWindow(x0, y0, z0);
		float[] w1 = projectWorldToWindow(x1, y1, z1);
		if (w0 == null || w1 == null) continue;
		// Ignore segments projected behind near/far
		if (w0[2] < 0f || w0[2] > 1f || w1[2] < 0f || w1[2] > 1f) continue;

		double dist2 = distPointToSegmentSquared(mouseX, mouseY, w0[0], w0[1], w1[0], w1[1]);
		if (dist2 < best) best = dist2;
	}
	return best;
}

private float[] projectWorldToWindow(float x, float y, float z) {
	viewportMoveGizmoModelMat.rewind();
	viewportMoveGizmoProjMat.rewind();
	viewportMoveGizmoViewport.rewind();
	viewportRotateGizmoTmpWinPos.clear();
	boolean ok = GLU.gluProject(x, y, z, viewportMoveGizmoModelMat, viewportMoveGizmoProjMat, viewportMoveGizmoViewport, viewportRotateGizmoTmpWinPos);
	if (!ok) return null;
	viewportRotateGizmoTmpWinPos.rewind();
	float wx = viewportRotateGizmoTmpWinPos.get(0);
	float wy = viewportRotateGizmoTmpWinPos.get(1);
	float wz = viewportRotateGizmoTmpWinPos.get(2);
	return new float[] { wx, wy, wz };
}

private double getWorldRadiusForPixelRadius(int pixelRadius) {
	// Estimate world units per pixel at the gizmo pivot using unprojection at the pivot depth.
	float cx = viewportMoveGizmoCenterXf;
	float cy = viewportMoveGizmoCenterYf;
	float cz = viewportMoveGizmoCenterDepth;
	if (cz <= 0f || cz >= 1f) cz = 0.5f;

	viewportMoveGizmoModelMat.rewind();
	viewportMoveGizmoProjMat.rewind();
	viewportMoveGizmoViewport.rewind();
	viewportRotateGizmoUnprojA.clear();
	boolean okA = GLU.gluUnProject(cx, cy, cz, viewportMoveGizmoModelMat, viewportMoveGizmoProjMat, viewportMoveGizmoViewport, viewportRotateGizmoUnprojA);

	viewportMoveGizmoModelMat.rewind();
	viewportMoveGizmoProjMat.rewind();
	viewportMoveGizmoViewport.rewind();
	viewportRotateGizmoUnprojB.clear();
	boolean okB = GLU.gluUnProject(cx + 1f, cy, cz, viewportMoveGizmoModelMat, viewportMoveGizmoProjMat, viewportMoveGizmoViewport, viewportRotateGizmoUnprojB);

	if (!okA || !okB) return 1.0;

	viewportRotateGizmoUnprojA.rewind();
	viewportRotateGizmoUnprojB.rewind();
	double ax = viewportRotateGizmoUnprojA.get(0);
	double ay = viewportRotateGizmoUnprojA.get(1);
	double az = viewportRotateGizmoUnprojA.get(2);
	double bx = viewportRotateGizmoUnprojB.get(0);
	double by = viewportRotateGizmoUnprojB.get(1);
	double bz = viewportRotateGizmoUnprojB.get(2);
	double dx = bx - ax, dy = by - ay, dz = bz - az;
	double perPixel = Math.sqrt(dx*dx + dy*dy + dz*dz);
	if (!Double.isFinite(perPixel) || perPixel < 1e-9) perPixel = 1.0;
	return perPixel * pixelRadius;
}

private void drawViewportRotateGizmoOverlay(int leftSidebarWidth) {
	if (!viewportRotateGizmoVisible || !viewportGizmoPivotValid) return;
	if (viewportMoveGizmoCenterX < leftSidebarWidth) return;

	// Size derived from move gizmo visuals
	viewportRotateGizmoRingThickPx = Math.max(2, Math.min(30, viewportMoveGizmoAxisThick / 2));
	viewportRotateGizmoRingRadiusPx = Math.max(30, Math.min(600, viewportMoveGizmoAxisLen + 22));
	double radiusWorld = getWorldRadiusForPixelRadius(viewportRotateGizmoRingRadiusPx);
	if (!Double.isFinite(radiusWorld) || radiusWorld <= 0) radiusWorld = 1;

	int steps = 64;
	int cx = viewportMoveGizmoCenterX;
	int cyTop = height - viewportMoveGizmoCenterY;

	glPushMatrix();
	glDisable(GL_TEXTURE_2D);
	glEnable(GL_BLEND);
	glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
	glDisable(GL_DEPTH_TEST);

	glLineWidth((float)viewportRotateGizmoRingThickPx);

	float px = viewportMoveGizmoWorldX;
	float py = viewportMoveGizmoWorldY;
	float pz = viewportMoveGizmoWorldZ;

	for (int axis = 1; axis <= 3; axis++) {
		if (axis == 1) glColor4f(1f, 0.25f, 0.25f, 0.70f);
		if (axis == 2) glColor4f(0.25f, 1f, 0.25f, 0.70f);
		if (axis == 3) glColor4f(0.25f, 0.55f, 1f, 0.70f);

		glBegin(GL_LINES);
		for (int i = 0; i < steps; i++) {
			double a0 = (Math.PI * 2.0) * (i / (double)steps);
			double a1 = (Math.PI * 2.0) * ((i + 1) / (double)steps);
			float x0, y0, z0, x1, y1, z1;
			if (axis == 1) {
				x0 = px; y0 = py + (float)Math.cos(a0) * (float)radiusWorld; z0 = pz + (float)Math.sin(a0) * (float)radiusWorld;
				x1 = px; y1 = py + (float)Math.cos(a1) * (float)radiusWorld; z1 = pz + (float)Math.sin(a1) * (float)radiusWorld;
			} else if (axis == 2) {
				x0 = px + (float)Math.cos(a0) * (float)radiusWorld; y0 = py; z0 = pz + (float)Math.sin(a0) * (float)radiusWorld;
				x1 = px + (float)Math.cos(a1) * (float)radiusWorld; y1 = py; z1 = pz + (float)Math.sin(a1) * (float)radiusWorld;
			} else {
				x0 = px + (float)Math.cos(a0) * (float)radiusWorld; y0 = py + (float)Math.sin(a0) * (float)radiusWorld; z0 = pz;
				x1 = px + (float)Math.cos(a1) * (float)radiusWorld; y1 = py + (float)Math.sin(a1) * (float)radiusWorld; z1 = pz;
			}
			float[] w0 = projectWorldToWindow(x0, y0, z0);
			float[] w1 = projectWorldToWindow(x1, y1, z1);
			if (w0 == null || w1 == null) continue;
			if (w0[2] < 0f || w0[2] > 1f || w1[2] < 0f || w1[2] > 1f) continue;
			int sx0 = (int)Math.round(w0[0]);
			int sy0 = (int)Math.round(height - w0[1]);
			int sx1 = (int)Math.round(w1[0]);
			int sy1 = (int)Math.round(height - w1[1]);
			glVertex2i(sx0, sy0);
			glVertex2i(sx1, sy1);
		}
		glEnd();
	}

	glDisable(GL_BLEND);
	glPopMatrix();
}

	public void drawCompass() {
		if (!ModelCreator.showGrid) return;
		
		glPushMatrix();
		{
			glTranslatef(width - 70, height - 70, 0);
			glLineWidth(2F);
			glRotated(-camera.getRY(), 0, 0, 1);
			GL11.glScaled(0.85, 0.85, 0.85);

			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			EnumFonts.BEBAS_NEUE_20.drawString(-5, -75, "N", new Color(1, 1, 1));
			EnumFonts.BEBAS_NEUE_20.drawString(-5, 55, "S", new Color(1, 1, 1));
			EnumFonts.BEBAS_NEUE_20.drawString(-70, -10, "W", new Color(1, 1, 1));
			EnumFonts.BEBAS_NEUE_20.drawString(55, -10, "E", new Color(1, 1, 1));
			GL11.glDisable(GL11.GL_BLEND);

			glColor3d(0.6, 0.6, 0.6);
			glBegin(GL_LINES);
			{
				glVertex2i(0, -50);
				glVertex2i(0, 50);
				glVertex2i(-50, 0);
				glVertex2i(50, 0);
			}
			glEnd();

			glColor3d(0.3, 0.3, 0.6);
			
			glBegin(GL_TRIANGLES);
			{
				glVertex2i(-5, -40);
				glVertex2i(0, -53);
				glVertex2i(5, -40);

				glVertex2i(-5, 40);
				glVertex2i(0, 53);
				glVertex2i(5, 40);

				glVertex2i(-40, -5);
				glVertex2i(-53, 0);
				glVertex2i(-40, 5);

				glVertex2i(40, -5);
				glVertex2i(53, 0);
				glVertex2i(40, 5);
			}
			glEnd();
			
			GL11.glDisable(GL11.GL_TEXTURE_2D);
		}
		
		glPopMatrix();
		
	}


	
	
	
	
	
	
	

	float accum = 0f;
	
	
	public void drawTreadmillGrid()
	{
		if (!ModelCreator.showTreadmill) return;
		
		accum += 1/60f * ModelCreator.TreadMillSpeed;
		if (accum > 1) accum -= 1;
		
		float d1 = 0.8f;
		float d2 = 0.6f;
		if (ModelCreator.darkMode) { d1 = 0.8f; d2 = 1.0f; }
	
		
		for (int dx = -8; dx <= 8; dx++) {
			for (int dz = -8; dz <= 8; dz++) {
				glPushMatrix();
				glTranslated(dx * 16 + accum * 16, 0, dz * 16);
				
				glColor3f(0.55F, 0.55F, 0.60F);
				glTranslatef(-8, 0, -8);

				// Thin inside lines
				glLineWidth(1F);
				glBegin(GL_LINES);
				{
					for (int i = 1; i <= 16; i++)
					{
						glVertex3i(i, 0, 0);
						glVertex3i(i, 0, 16);
					}

					for (int i = 1; i <= 16; i++)
					{
						glVertex3i(0, 0, i);
						glVertex3i(16, 0, i);
					}
				}
				glEnd();
				
				glColor3f(0.55F * d2, 0.55F * d2, 0.60F * d2);
				
				// Bold outside lines
				glLineWidth(2F);
				glBegin(GL_LINES);
				{
					glVertex3i(0, 0, 0);
					glVertex3i(0, 0, 16);
					glVertex3i(16, 0, 0);
					glVertex3i(16, 0, 16);
					glVertex3i(0, 0, 16);
					glVertex3i(16, 0, 16);
					glVertex3i(0, 0, 0);
					glVertex3i(16, 0, 0);
				}
				glEnd();

				glColor3f(0.55F * d1, 0.55F * d1, 0.60F * d1);
				
				// Bold center cross
				glLineWidth(2F);
				glBegin(GL_LINES);
				{
					glVertex3i(8, 0, 0);
					glVertex3i(8, 0, 16);
					glVertex3i(0, 0, 8);
					glVertex3i(16, 0, 8);
				}
				glEnd();

				glColor3f(0.55F, 0.55F, 0.60F);
				
				glPopMatrix();
				
			}
		}
		
	}

	
	public static List<IDrawable> getRootElementsForRender(Project project) {
		if (project == null) return null;
		
		try {
			if (ModelCreator.leftKeyframesPanel.isVisible()) {
				return project.getCurrentFrameRootElements();
			} else {
				return new ArrayList<IDrawable>(project.rootElements);
			}
		} catch (Exception e) {
			System.out.println(e + "\n");
			e.printStackTrace();
			return new ArrayList<IDrawable>();
		}
	}
	
	
	private void drawViewportMarquee(int leftSidebarWidth)
	{
		if (!viewportMarqueeActive) return;

		int x1 = viewportMarqueeX1;
		int y1 = viewportMarqueeY1;
		int x2 = viewportMarqueeX2;
		int y2 = viewportMarqueeY2;

		int xmin = Math.min(x1, x2);
		int xmax = Math.max(x1, x2);
		int yminBL = Math.min(y1, y2);
		int ymaxBL = Math.max(y1, y2);

		// Clamp to center viewport region (avoid drawing over left panel)
		xmin = Math.max(leftSidebarWidth, xmin);
		xmax = Math.max(leftSidebarWidth, xmax);

		// Convert from bottom-left mouse coordinates to top-left ortho coordinates
		int ymin = this.height - ymaxBL;
		int ymax = this.height - yminBL;

		GL11.glPushMatrix();
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_LIGHTING);

		// Fill
		GL11.glColor4f(1f, 1f, 0f, 0.12f);
		GL11.glBegin(GL11.GL_QUADS);
		{
			GL11.glVertex2i(xmin, ymin);
			GL11.glVertex2i(xmax, ymin);
			GL11.glVertex2i(xmax, ymax);
			GL11.glVertex2i(xmin, ymax);
		}
		GL11.glEnd();

		// Outline
		GL11.glColor4f(1f, 1f, 0f, 0.85f);
		GL11.glLineWidth(1.5f);
		GL11.glBegin(GL11.GL_LINE_LOOP);
		{
			GL11.glVertex2i(xmin, ymin);
			GL11.glVertex2i(xmax, ymin);
			GL11.glVertex2i(xmax, ymax);
			GL11.glVertex2i(xmin, ymax);
		}
		GL11.glEnd();
		GL11.glLineWidth(1f);

		GL11.glDisable(GL11.GL_BLEND);
		GL11.glPopMatrix();
	}

}