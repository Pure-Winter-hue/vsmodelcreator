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
// Center in window coordinates (origin bottom-left, same as Mouse.getY()).
// Keep both float (for stable drawing) and int (for hit-testing).
public float viewportMoveGizmoCenterXf = 0;
public float viewportMoveGizmoCenterYf = 0;
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
		drawViewportMoveGizmoOverlay(leftSidebarWidth);
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
		
		float d1 = 0.8f;
		float d2 = 0.6f;
		if (ModelCreator.darkMode) { d1 = 1.2f; d2 = 1.4f; }
		
		glPushMatrix();
		{
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
			
			// Thin half transparent line to show block size
			glLineWidth(1F);
			glColor3f(0.55F / d1, 0.55F / d1, 0.55F / d1);
			
			
			glBegin(GL_LINES);
			{
				glVertex3i(0, 0, 0);
				glVertex3i(0, 16, 0);
				
				glVertex3i(16, 0, 0);
				glVertex3i(16, 16, 0);

				glVertex3i(16, 0, 16);
				glVertex3i(16, 16, 16);
				
				glVertex3i(0, 0, 16);
				glVertex3i(0, 16, 16);
				
				glVertex3i(0, 16, 0);
				glVertex3i(16, 16, 0);
				
				glVertex3i(16, 16, 0);
				glVertex3i(16, 16, 16);
				
				glVertex3i(16, 16, 16);
				glVertex3i(0, 16, 16);
				
				glVertex3i(0, 16, 16);
				glVertex3i(0, 16, 0);
			}
			glEnd();
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
		Project project = ModelCreator.currentProject;
		if (project == null || project.SelectedElement == null) return;
		if (ModelCreator.renderAttachmentPoints) return;
		// Hide in UV/Face tab (this overlay gizmo is for viewport translate)
		if (ModelCreator.currentRightTab == 1) return;

		List<Element> elems;
		if (ModelCreator.viewportGroupMoveEnabled && project.SelectedElements != null && project.SelectedElements.size() > 1) {
			elems = project.SelectedElements;
		} else {
			elems = java.util.Collections.singletonList(project.SelectedElement);
		}

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

			double sx = e.getStartX();
			double sy = e.getStartY();
			double sz = e.getStartZ();

			if (keyframeTab && curFrame != null) {
				AnimFrameElement afe = curFrame.GetAnimFrameElementRec(e);
				if (afe == null) afe = curFrame.GetAnimFrameElementRecByName(e.getName());
				if (afe != null) {
					sx = e.getStartX() + afe.getOffsetX();
					sy = e.getStartY() + afe.getOffsetY();
					sz = e.getStartZ() + afe.getOffsetZ();
				}
			}

			minX = Math.min(minX, sx);
			minY = Math.min(minY, sy);
			minZ = Math.min(minZ, sz);
			maxX = Math.max(maxX, sx + e.getWidth());
			maxY = Math.max(maxY, sy + e.getHeight());
			maxZ = Math.max(maxZ, sz + e.getDepth());
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
		if (wz < 0f || wz > 1f) return;

		int ix = Math.round(wx);
		int iy = Math.round(wy);

		if (ix < leftSidebarWidth || ix > width) return;
		if (iy < 0 || iy > height) return;

		viewportMoveGizmoCenterX = ix;
		viewportMoveGizmoCenterY = iy;
		viewportMoveGizmoVisible = true;
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