package at.vintagestory.modelcreator.gui.left;

import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glTexCoord2f;
import static org.lwjgl.opengl.GL11.glTranslatef;
import static org.lwjgl.opengl.GL11.glVertex2d;
import static org.lwjgl.opengl.GL11.glVertex2i;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Stack;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.newdawn.slick.Color;
import org.newdawn.slick.opengl.TextureImpl;

import at.vintagestory.modelcreator.ModelCreator;
import at.vintagestory.modelcreator.enums.EnumFonts;
import at.vintagestory.modelcreator.interfaces.IElementManager;
import at.vintagestory.modelcreator.model.Element;
import at.vintagestory.modelcreator.model.Face;
import at.vintagestory.modelcreator.model.RenderFaceTask;
import at.vintagestory.modelcreator.model.Sized;
import at.vintagestory.modelcreator.model.TextureEntry;

public class LeftUVSidebar extends LeftSidebar
{
	private IElementManager manager;

	private static int DEFAULT_WIDTH = 4*32;

	private final Color BLACK_ALPHA = new Color(0, 0, 0, 0.75F);

	private int[] startX = { 0, 0, 0, 0, 0, 0 };
	private int[] startY = { 0, 0, 0, 0, 0, 0 };

	private static class UvSelection {
		final Element elem;
		final int faceIndex;
		final String textureCode;

		UvSelection(Element elem, int faceIndex, String textureCode) {
			this.elem = elem;
			this.faceIndex = faceIndex;
			this.textureCode = textureCode;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof UvSelection)) return false;
			UvSelection other = (UvSelection)o;
			return elem == other.elem && faceIndex == other.faceIndex && Objects.equals(textureCode, other.textureCode);
		}

		@Override
		public int hashCode() {
			return Objects.hash(System.identityHashCode(elem), faceIndex, textureCode);
		}
	}

	// Multi-select state
	private final HashSet<UvSelection> selectedUvs = new HashSet<>();
	private final boolean[] selectedBlockFaces = new boolean[] { false, false, false, false, false, false };

	// UV editor tools
	private enum UvTool { SELECT, MOVE }
	private UvTool uvTool = UvTool.SELECT;

	// Move scope in entity UV editor: move only the grabbed/selected face(s) vs translating the whole auto-unwrap layout
	private boolean uvMoveAll = true;

	// Marquee selection behavior
	private boolean marqueeAdditive = false;

	// Simple tool buttons (top-right)
	// Larger and centered for readability.
	private static final int TOOLBTN_W = 60;
	private static final int TOOLBTN_H = 24;
	private static final int TOOLBTN_Y = 5;
	private static final int TOOLBTN_PAD = 8;

	// Debounce: onMouseDownOnPanel is called repeatedly while the mouse is held.
	// Without an edge trigger, tool buttons would toggle multiple times per click.
	private boolean toolButtonLatch = false;

	// Shift + drag marquee selection
	private boolean marqueeActive = false;
	private boolean marqueeInEntityMode = false;
	private String marqueeTextureCode = null;
	private int marqueeStartX, marqueeStartY;
	private int marqueeCurX, marqueeCurY;
	private int marqueeBoxW, marqueeBoxH;
	private int marqueeStartMouseXRaw, marqueeStartMouseYRaw;
	private int marqueeCurMouseXRaw, marqueeCurMouseYRaw;
	
	
	private int texBoxWidth, texBoxHeight;
	
	float[] brightnessByFace = new float[] { 1, 1, 1, 1, 1, 1 }; 
	
	int canvasHeight;
	
	Stack<RenderFaceTask> renderLastStack = new Stack<RenderFaceTask>();
	Stack<RenderFaceTask> renderLastStack2 = new Stack<RenderFaceTask>();
	
	
	public LeftUVSidebar(String title, IElementManager manager)
	{
		super(title);
		this.manager = manager;
		
		blockFaceTextureSize = DEFAULT_WIDTH;
	}
	
	@Override
	public void onResized()
	{
		/*if (!ModelCreator.currentProject.EntityTextureMode) {
			int extraWidth = GetSidebarWidth() - DEFAULT_WIDTH + 20;
			if (canvasHeight < 6*(10 + DEFAULT_WIDTH + extraWidth)) {
				extraWidth /= 2;
			}
			
			blockFaceTextureWidth = DEFAULT_WIDTH + extraWidth;
		}*/
	}

	@Override
	public void draw(int sidebarWidth, int canvasWidth, int canvasHeight, int frameHeight)
	{
		super.draw(sidebarWidth, canvasWidth, canvasHeight, frameHeight);

		this.canvasHeight = canvasHeight;

		drawToolButtons();

		if (ModelCreator.transparent) {
			GL11.glEnable(GL11.GL_BLEND);
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		}

		
		if (ModelCreator.currentProject.rootElements.size() == 0) return;

		if (ModelCreator.currentProject.EntityTextureMode) {
			drawRectsEntityTextureMode(canvasHeight);
		} else {
			drawRectsBlockTextureMode(canvasHeight);
		}


		if (ModelCreator.transparent) {
			GL11.glDisable(GL11.GL_BLEND);
		}
	}
	
	ArrayList<String> getTextureCodes() {
		ArrayList<String> codes = new ArrayList<String>();
		
		String prioTextureCode=null;
		if (ModelCreator.currentProject.SelectedElement != null) {
			Face face = ModelCreator.currentProject.SelectedElement.getSelectedFace();
			if (face != null) prioTextureCode = face.getTextureCode();
			if (prioTextureCode != null && ModelCreator.currentProject.TexturesByCode.containsKey(prioTextureCode)) {
				codes.add(prioTextureCode);
			}
		}
		
		for (String textureCode : ModelCreator.currentProject.TexturesByCode.keySet()) {
			if (prioTextureCode != null && prioTextureCode.equals(textureCode)) continue;
			codes.add(textureCode);
		}
		
		return codes;
	}

	private boolean isShiftDown() {
		return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
	}

	private void clearBlockFaceSelection() {
		for (int i = 0; i < selectedBlockFaces.length; i++) selectedBlockFaces[i] = false;
	}

	private void setSingleBlockFaceSelection(int faceIndex) {
		clearBlockFaceSelection();
		if (faceIndex >= 0 && faceIndex < selectedBlockFaces.length) selectedBlockFaces[faceIndex] = true;
	}

	private void setSingleEntityUvSelection(Element elem, int faceIndex, String textureCode) {
		selectedUvs.clear();
		if (elem != null && faceIndex >= 0) {
			selectedUvs.add(new UvSelection(elem, faceIndex, textureCode));
		}
	}

	private boolean isEntityUvSelected(Element elem, int faceIndex, String textureCode) {
		return selectedUvs.contains(new UvSelection(elem, faceIndex, textureCode));
	}

	private static class TexArea {
		final String textureCode;
		final int localX;
		final int localY;
		final int boxW;
		final int boxH;
		TexArea(String textureCode, int localX, int localY, int boxW, int boxH) {
			this.textureCode = textureCode;
			this.localX = localX;
			this.localY = localY;
			this.boxW = boxW;
			this.boxH = boxH;
		}
	}

	private int[] calcEntityTexBoxSize(String textureCode) {
		double texWidth = ModelCreator.currentProject.TextureWidth;
		double texHeight = ModelCreator.currentProject.TextureHeight;
		if (textureCode != null) {
			int[] size = ModelCreator.currentProject.TextureSizes.get(textureCode);
			if (size != null) {
				texWidth = size[0];
				texHeight = size[1];
			}
		}
		TextureEntry texEntry = textureCode == null ? null : ModelCreator.currentProject.TexturesByCode.get(textureCode);
		Sized scale = Face.getVoxel2PixelScale(ModelCreator.currentProject, texEntry);
		texWidth *= scale.W / 2;
		texHeight *= scale.H / 2;
		int boxW = (int)Math.max(0, (GetSidebarWidth() - 20));
		int boxH = (int)(boxW * texHeight / texWidth);
		return new int[] { boxW, boxH };
	}

	private ArrayList<String> getTextureCodesOrNull() {
		ArrayList<String> codes = getTextureCodes();
		if (codes.size() == 0) codes.add(null);
		return codes;
	}

	private TexArea getTexAreaAtMouse() {
		int mouseX = Mouse.getX() - 10;
		int mouseY = (canvasHeight - Mouse.getY()) - 30;
		if (mouseX < 0) return null;

		for (String textureCode : getTextureCodesOrNull()) {
			int[] wh = calcEntityTexBoxSize(textureCode);
			int boxW = wh[0];
			int boxH = wh[1];
			if (boxW <= 0 || boxH <= 0) return null;
			if (mouseY >= 0 && mouseY < boxH) {
				return new TexArea(textureCode, Clamp(mouseX, 0, boxW), Clamp(mouseY, 0, boxH), boxW, boxH);
			}
			mouseY -= boxH + 20;
		}
		return null;
	}

	private TexArea getMouseLocalInTexture(String targetTextureCode, int fallbackBoxW, int fallbackBoxH) {
		int mouseX = Mouse.getX() - 10;
		int mouseY = (canvasHeight - Mouse.getY()) - 30;
		if (mouseX < 0) return null;

		for (String textureCode : getTextureCodesOrNull()) {
			int[] wh = calcEntityTexBoxSize(textureCode);
			int boxW = wh[0];
			int boxH = wh[1];
			if (Objects.equals(textureCode, targetTextureCode)) {
				return new TexArea(textureCode, Clamp(mouseX, 0, boxW), Clamp(mouseY, 0, boxH), boxW, boxH);
			}
			mouseY -= boxH + 20;
		}
		// If we can't resolve it anymore (e.g. textures list changed), clamp to last known size
		return new TexArea(targetTextureCode, Clamp(mouseX, 0, fallbackBoxW), Clamp(mouseY, 0, fallbackBoxH), fallbackBoxW, fallbackBoxH);
	}

	private void collectEntitySelectionsInRect(String textureCode, int rx1, int ry1, int rx2, int ry2, ArrayList<Element> elems, int boxW, int boxH, HashSet<UvSelection> out) {
		for (Element elem : elems) {
			if (!elem.getRenderInEditor()) continue;
			Face[] faces = elem.getAllFaces();
			for (int i = 0; i < 6; i++) {
				Face face = faces[i];
				if (face == null || !face.isEnabled()) continue;
				if (textureCode != null && !textureCode.equals(face.getTextureCode())) continue;

				Sized uv = face.translateVoxelPosToUvPos(face.getStartU(), face.getStartV(), true);
				Sized uvend = face.translateVoxelPosToUvPos(face.getEndU(), face.getEndV(), true);
				int fx1 = (int)Math.round(uv.W * boxW);
				int fy1 = (int)Math.round(uv.H * boxH);
				int fx2 = (int)Math.round(uvend.W * boxW);
				int fy2 = (int)Math.round(uvend.H * boxH);

				boolean intersects = fx1 <= rx2 && fx2 >= rx1 && fy1 <= ry2 && fy2 >= ry1;
				if (intersects) {
					out.add(new UvSelection(elem, i, face.getTextureCode()));
				}
			}
			collectEntitySelectionsInRect(textureCode, rx1, ry1, rx2, ry2, elem.ChildElements, boxW, boxH, out);
		}
	}

	private UvSelection findEntityUvAt(String textureCode, double mouseU, double mouseV, ArrayList<Element> elems) {
		for (Element elem : elems) {
			if (!elem.getRenderInEditor()) continue;
			Face[] faces = elem.getAllFaces();
			for (int i = 0; i < 6; i++) {
				Face face = faces[i];
				if (face == null || !face.isEnabled()) continue;
				if (textureCode != null && !textureCode.equals(face.getTextureCode())) continue;
				Sized uv = face.translateVoxelPosToUvPos(face.getStartU(), face.getStartV(), true);
				Sized uvend = face.translateVoxelPosToUvPos(face.getEndU(), face.getEndV(), true);
				if (mouseU >= uv.W && mouseV >= uv.H && mouseU <= uvend.W && mouseV <= uvend.H) {
					return new UvSelection(elem, i, face.getTextureCode());
				}
			}
			UvSelection found = findEntityUvAt(textureCode, mouseU, mouseV, elem.ChildElements);
			if (found != null) return found;
		}
		return null;
	}

	private void finalizeMarqueeSelection() {
		if (!marqueeActive) return;

		if (marqueeInEntityMode) {
			if (marqueeBoxW <= 0 || marqueeBoxH <= 0) return;
			int x1 = Math.min(marqueeStartX, marqueeCurX);
			int y1 = Math.min(marqueeStartY, marqueeCurY);
			int x2 = Math.max(marqueeStartX, marqueeCurX);
			int y2 = Math.max(marqueeStartY, marqueeCurY);
			int w = x2 - x1;
			int h = y2 - y1;

			// Tiny drag counts as click
			if (w < 3 && h < 3) {
				double mouseU = (double)marqueeStartX / marqueeBoxW;
				double mouseV = (double)marqueeStartY / marqueeBoxH;
				UvSelection hit = findEntityUvAt(marqueeTextureCode, mouseU, mouseV, ModelCreator.currentProject.rootElements);

				if (!marqueeAdditive) {
					selectedUvs.clear();
				}

				if (hit != null) {
					if (marqueeAdditive) {
						if (selectedUvs.contains(hit)) selectedUvs.remove(hit);
						else selectedUvs.add(hit);
					} else {
						selectedUvs.add(hit);
					}
					ModelCreator.currentProject.selectElement(hit.elem);
					hit.elem.setSelectedFace(hit.faceIndex);
				}
				return;
			}

			HashSet<UvSelection> found = new HashSet<>();
			collectEntitySelectionsInRect(marqueeTextureCode, x1, y1, x2, y2, ModelCreator.currentProject.rootElements, marqueeBoxW, marqueeBoxH, found);
			if (!marqueeAdditive) {
				selectedUvs.clear();
			}
			if (!found.isEmpty()) {
				selectedUvs.addAll(found);
				UvSelection lead = found.iterator().next();
				ModelCreator.currentProject.selectElement(lead.elem);
				lead.elem.setSelectedFace(lead.faceIndex);
			}
			return;
		}

		// Block texture mode: select face boxes on the left
		Element elem = manager.getCurrentElement();
		if (elem == null) return;
		int x1 = Math.min(marqueeStartX, marqueeCurX);
		int y1 = Math.min(marqueeStartY, marqueeCurY);
		int x2 = Math.max(marqueeStartX, marqueeCurX);
		int y2 = Math.max(marqueeStartY, marqueeCurY);
		int w = x2 - x1;
		int h = y2 - y1;

		if (w < 3 && h < 3) {
			int faceIdx = getGrabbedFace(elem, canvasHeight, marqueeStartMouseXRaw, marqueeStartMouseYRaw);
			if (!marqueeAdditive) {
				for (int i = 0; i < 6; i++) selectedBlockFaces[i] = false;
			}
			if (faceIdx >= 0) {
				if (marqueeAdditive) {
					selectedBlockFaces[faceIdx] = !selectedBlockFaces[faceIdx];
				} else {
					selectedBlockFaces[faceIdx] = true;
				}
				if (selectedBlockFaces[faceIdx]) {
					elem.setSelectedFace(faceIdx);
				}
			}
			return;
		}

		if (!marqueeAdditive) {
			for (int i = 0; i < 6; i++) selectedBlockFaces[i] = false;
		}

		for (int i = 0; i < 6; i++) {
			Face face = elem.getAllFaces()[i];
			if (face == null || !face.isEnabled()) continue;
			int bx1 = startX[i];
			int by1 = startY[i];
			int bx2 = startX[i] + blockFaceTextureMaxWidth;
			int by2 = startY[i] + blockFaceTextureSize;
			boolean intersects = bx1 <= x2 && bx2 >= x1 && by1 <= y2 && by2 >= y1;
			if (intersects) {
				selectedBlockFaces[i] = true;
				elem.setSelectedFace(i);
			}
		}
	}

	void drawRectsEntityTextureMode(int canvasHeight) {
		
		if (!Mouse.isButtonDown(0) && !Mouse.isButtonDown(1)) {
			grabbedElement = currentHoveredElementEntityTextureMode(ModelCreator.currentProject.rootElements);
		}			
		
		if (ModelCreator.currentProject.TexturesByCode.size() > 0) {
			int offsetY = 0;
			
			glPushMatrix();
			
			for (String textureCode : getTextureCodes()) {
				offsetY = drawRectsAndTexture(textureCode);				
				glTranslatef(0, offsetY, 0);
			}
			
			glPopMatrix();
			
		} else {
			
			drawRectsAndTexture(null);
		}
	}
	
	
	int drawRectsAndTexture(String textureCode) {
		
		double texWidth = ModelCreator.currentProject.TextureWidth;
		double texHeight = ModelCreator.currentProject.TextureHeight;
		
		int[] size = ModelCreator.currentProject.TextureSizes.get(textureCode);
		if (size != null) {
			texWidth = size[0];
			texHeight = size[1];
		}
		
		Sized scale;
		
		TextureEntry texEntry = null;
		if (textureCode != null) {
			texEntry = ModelCreator.currentProject.TexturesByCode.get(textureCode);
		}
		
		scale = Face.getVoxel2PixelScale(ModelCreator.currentProject, texEntry);
		
		texWidth *= scale.W / 2;
		texHeight *= scale.H / 2;
		
		texBoxWidth = (int)Math.max(0, (GetSidebarWidth() - 20));
		texBoxHeight = (int)(texBoxWidth * texHeight / texWidth);
		
		glPushMatrix();
		{
			glTranslatef(10, 30, 0);
			glPushMatrix(); {
				
				glColor3f(1, 1, 1);
				
				Face.bindTexture(texEntry);
				
				float endu = 1f;
				float endv = 1f;
				if (texEntry != null) {
					endu = (float)texEntry.Width / texEntry.texture.getTextureWidth();
					endv = (float)texEntry.Height / texEntry.texture.getTextureHeight();
				}
				
				// Background
				glLineWidth(1F);
				glBegin(GL_QUADS);
				{
					glTexCoord2f(0, endv);
					glVertex2i(0, texBoxHeight);
					
					glTexCoord2f(endu, endv);
					glVertex2i(texBoxWidth, texBoxHeight);
					
					glTexCoord2f(endu, 0);
					glVertex2i(texBoxWidth, 0);

					glTexCoord2f(0, 0);
					glVertex2i(0, 0);
				}
				glEnd();
				TextureImpl.bindNone();
				
				
				// Pixel grid
				if (texEntry == null) {
					glBegin(GL_LINES);
					{
						glColor3f(0.9f, 0.9f, 0.9f);
						int pixelsW = (int)(ModelCreator.currentProject.TextureWidth * scale.W);
						int pixelsH = (int)(ModelCreator.currentProject.TextureHeight * scale.H);
						
						if (pixelsW <= 64 && pixelsH <= 64) {
							double sectionWidth = (double)texBoxWidth / pixelsW;
							for (double i = 0; i <= pixelsW; i++) {
								glVertex2d(i * sectionWidth, 0);
								glVertex2d(i * sectionWidth, texBoxHeight);	
							}
							
							double sectionHeight = (double)texBoxHeight / pixelsH;
							for (double i = 0; i <= pixelsH; i++) {
								glVertex2d(0, i * sectionHeight);
								glVertex2d(texBoxWidth, i * sectionHeight);	
							}
						}
					}
					glEnd();
				}
				
				drawElementList(textureCode, ModelCreator.currentProject.rootElements, texBoxWidth, texBoxHeight, canvasHeight);
				
				// selected face at the very top
				while (renderLastStack2.size() > 0) {
					RenderFaceTask rft = renderLastStack2.pop();
					drawFace(rft.elem, rft.face);
				}

				
					// Marquee selection overlay
					if (marqueeActive && marqueeInEntityMode && Objects.equals(marqueeTextureCode, textureCode)) {
						TextureImpl.bindNone();
						GL11.glEnable(GL11.GL_BLEND);
						glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

						int x1 = Math.min(marqueeStartX, marqueeCurX);
						int y1 = Math.min(marqueeStartY, marqueeCurY);
						int x2 = Math.max(marqueeStartX, marqueeCurX);
						int y2 = Math.max(marqueeStartY, marqueeCurY);

						GL11.glColor4f(1f, 1f, 0f, 0.15f);
						glBegin(GL_QUADS);
						{
							glVertex2d(x1, y2);
							glVertex2d(x2, y2);
							glVertex2d(x2, y1);
							glVertex2d(x1, y1);
						}
						glEnd();

						GL11.glColor4f(1f, 1f, 0f, 0.9f);
						glBegin(GL_LINES);
						{
							glVertex2d(x1, y1);
							glVertex2d(x1, y2);

							glVertex2d(x1, y2);
							glVertex2d(x2, y2);

							glVertex2d(x2, y2);
							glVertex2d(x2, y1);

							glVertex2d(x2, y1);
							glVertex2d(x1, y1);
						}
						glEnd();
					}
					glPopMatrix();
			}
		}
		
		glPopMatrix();
		
		return texBoxHeight + 20;
	}
	
	
	

	private void drawElementList(String textureCode, ArrayList<Element> elems, double texBoxWidth, double texBoxHeight, int canvasHeight)
	{
		Element selectedElem = ModelCreator.currentProject.SelectedElement;
		
		for (Element elem : elems) {
			if (!elem.getRenderInEditor()) continue;
			
			Face[] faces = elem.getAllFaces();
			
			for (int i = 0; i < 6; i++) {
				Face face = faces[i];
				if (!face.isEnabled() || (textureCode != null && !textureCode.equals(face.getTextureCode()))) continue;
				
				if (elem == selectedElem) {
					if (face == selectedElem.getSelectedFace()) {
						renderLastStack2.push(new RenderFaceTask(face, elem));
						continue;
					}
						
					renderLastStack.push(new RenderFaceTask(face, elem));
					continue;
				}
				
				if (elem == grabbedElement && face.isAutoUVEnabled()) {
					renderLastStack.push(new RenderFaceTask(face, elem));
					continue;
				}
				
				if (elem == grabbedElement && !face.isAutoUVEnabled() && i == grabbedFaceIndex) {
					renderLastStack.push(new RenderFaceTask(face, elem));
					continue;
				}
				
				drawFace(elem, face);
			}
			
			
			drawElementList(textureCode, elem.ChildElements, texBoxWidth, texBoxHeight, canvasHeight);
		}
		
		// Needs to be drawn at the end so the selected highlight is always visible
		while (renderLastStack.size() > 0) {
			RenderFaceTask rft = renderLastStack.pop();
			drawFace(rft.elem, rft.face);
		}	
	}
	
	private void drawFace(Element elem, Face face) {
		Element selectedElem = ModelCreator.currentProject.SelectedElement;
		Face selectedFace = selectedElem == null ? null : selectedElem.getSelectedFace();
		
		Sized uv = face.translateVoxelPosToUvPos(face.getStartU(), face.getStartV(), true);
		Sized uvend = face.translateVoxelPosToUvPos(face.getEndU(), face.getEndV(), true);
		
		int sidei = face.getSide();
		
		Color color = Face.getFaceColour(sidei);
		
		
		GL11.glColor4f(color.r * elem.brightnessByFace[sidei], color.g * elem.brightnessByFace[sidei], color.b * elem.brightnessByFace[sidei], 0.3f);

		glBegin(GL_QUADS);
		{
			glTexCoord2f(0, 1);
			glVertex2d(uv.W * texBoxWidth, uvend.H * texBoxHeight);
			
			glTexCoord2f(1, 1);
			glVertex2d(uvend.W * texBoxWidth, uvend.H * texBoxHeight);
			
			glTexCoord2f(1, 0);
			glVertex2d(uvend.W * texBoxWidth, uv.H * texBoxHeight);

			glTexCoord2f(0, 0);
			glVertex2d(uv.W * texBoxWidth, uv.H * texBoxHeight);
		}
		glEnd();

		
		glColor3f(0.5f, 0.5f, 0.5f);

		
		if (elem == selectedElem) {
			glColor3f(0f, 0f, 1f);			
			if (face == selectedFace) {
				glColor3f(0f, 1f, 0.5f);
			}
		}
		
		if (elem == grabbedElement && face.isAutoUVEnabled()) {
			glColor3f(0f, 0.75f, 1f);
		}
		
		if (elem == grabbedElement && !face.isAutoUVEnabled() && sidei == grabbedFaceIndex) {
			glColor3f(0f, 1f, 0.75f);
		}
		
		glBegin(GL_LINES);
		{
			glVertex2d(uv.W * texBoxWidth, uv.H * texBoxHeight);
			glVertex2d(uv.W * texBoxWidth, uvend.H * texBoxHeight);

			glVertex2d(uv.W * texBoxWidth, uvend.H * texBoxHeight);
			glVertex2d(uvend.W * texBoxWidth, uvend.H * texBoxHeight);

			glVertex2d(uvend.W * texBoxWidth, uvend.H * texBoxHeight);
			glVertex2d(uvend.W * texBoxWidth, uv.H * texBoxHeight);

			glVertex2d(uvend.W * texBoxWidth, uv.H * texBoxHeight);
			glVertex2d(uv.W * texBoxWidth, uv.H * texBoxHeight);
		}
		
		glEnd();

		// Marquee multi-selection highlight
		if (isEntityUvSelected(elem, sidei, face.getTextureCode())) {
			TextureImpl.bindNone();
			glLineWidth(2F);
			GL11.glColor4f(1f, 1f, 0f, 1f);
			glBegin(GL_LINES);
			{
				glVertex2d(uv.W * texBoxWidth, uv.H * texBoxHeight);
				glVertex2d(uv.W * texBoxWidth, uvend.H * texBoxHeight);

				glVertex2d(uv.W * texBoxWidth, uvend.H * texBoxHeight);
				glVertex2d(uvend.W * texBoxWidth, uvend.H * texBoxHeight);

				glVertex2d(uvend.W * texBoxWidth, uvend.H * texBoxHeight);
				glVertex2d(uvend.W * texBoxWidth, uv.H * texBoxHeight);

				glVertex2d(uvend.W * texBoxWidth, uv.H * texBoxHeight);
				glVertex2d(uv.W * texBoxWidth, uv.H * texBoxHeight);
			}
			glEnd();
			glLineWidth(1F);
		}

		boolean renderName = (ModelCreator.uvShowNames && (!elem.isAutoUnwrapEnabled() || (elem.getUnwrapMode() <= 0 && sidei ==0) || sidei == elem.getUnwrapMode() - 1));
		
		if (renderName) {
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			GL11.glEnable(GL11.GL_BLEND);
			
			float width = EnumFonts.BEBAS_NEUE_12.getWidth(elem.getName());
			float height = EnumFonts.BEBAS_NEUE_12.getHeight(elem.getName());
			
			int x = (int)((uv.W + (uvend.W - uv.W) / 2) * texBoxWidth - width/2);
			int y = (int)((uv.H + (uvend.H - uv.H) / 2) * texBoxHeight - height/2);
			
			EnumFonts.BEBAS_NEUE_12.drawString(x, y, elem.getName(), BLACK_ALPHA);
			
			TextureImpl.bindNone();
		}
	}


	private int getToolBtnX(int indexFromRight) {
		int w = GetSidebarWidth();
		return w - 10 - TOOLBTN_W - indexFromRight * (TOOLBTN_W + TOOLBTN_PAD);
	}

	private int getMoveBtnX() {
		return getToolBtnX(0);
	}

	private int getSelectBtnX() {
		return getToolBtnX(1);
	}

	private int getMoveScopeBtnX() {
		return getToolBtnX(2);
	}

	private int getFlipUdBtnX() {
		return getToolBtnX(3);
	}

	private int getFlipLrBtnX() {
		return getToolBtnX(4);
	}

	private void drawToolButtons() {
		int moveX = getMoveBtnX();
		int selectX = getSelectBtnX();
		int scopeX = getMoveScopeBtnX();
		int flipUdX = getFlipUdBtnX();
		int flipLrX = getFlipLrBtnX();

		GL11.glEnable(GL11.GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		// Convenience duplicates (same operations as the Tools panel)
		drawToolButton(flipLrX, TOOLBTN_Y, "LR", false);
		drawToolButton(flipUdX, TOOLBTN_Y, "UD", false);

		// Move scope toggle (only meaningful for auto-unwrap in entity texture mode)
		drawToolButton(scopeX, TOOLBTN_Y, uvMoveAll ? "ALL" : "THIS", true);

		drawToolButton(selectX, TOOLBTN_Y, "SEL", uvTool == UvTool.SELECT);
		drawToolButton(moveX, TOOLBTN_Y, "MOVE", uvTool == UvTool.MOVE);

		GL11.glDisable(GL11.GL_BLEND);
	}

	private void drawToolButton(int x, int y, String label, boolean active) {
		TextureImpl.bindNone();
		if (active) GL11.glColor4f(0.25f, 0.35f, 0.85f, 0.9f);
		else GL11.glColor4f(0f, 0f, 0f, 0.25f);

		glBegin(GL_QUADS);
		{
			glVertex2i(x, y + TOOLBTN_H);
			glVertex2i(x + TOOLBTN_W, y + TOOLBTN_H);
			glVertex2i(x + TOOLBTN_W, y);
			glVertex2i(x, y);
		}
		glEnd();

		GL11.glColor4f(0.8f, 0.8f, 0.9f, 0.9f);
		glBegin(GL_LINES);
		{
			glVertex2i(x, y);
			glVertex2i(x, y + TOOLBTN_H);

			glVertex2i(x, y + TOOLBTN_H);
			glVertex2i(x + TOOLBTN_W, y + TOOLBTN_H);

			glVertex2i(x + TOOLBTN_W, y + TOOLBTN_H);
			glVertex2i(x + TOOLBTN_W, y);

			glVertex2i(x + TOOLBTN_W, y);
			glVertex2i(x, y);
		}
		glEnd();

		// Center label for better legibility on different UI scales.
		float tw = EnumFonts.BEBAS_NEUE_16.getWidth(label);
		float th = EnumFonts.BEBAS_NEUE_16.getHeight(label);
		int tx = x + (int)((TOOLBTN_W - tw) / 2f);
		int ty = y + (int)((TOOLBTN_H - th) / 2f);
		EnumFonts.BEBAS_NEUE_16.drawString(tx, ty, label, new Color(0.95F, 0.95F, 1F, 1F));
		TextureImpl.bindNone();
	}

	private void flipSelectedFaceU() {
		Element el = manager.getCurrentElement();
		if (el == null) return;
		Face face = el.getSelectedFace();
		if (face == null) return;

		if (face.isAutoUVEnabled()) face.setAutoUVEnabled(false);
		double su = face.getStartU();
		double eu = face.getEndU();
		face.setStartU(eu);
		face.setEndU(su);
		face.updateUV();
		ModelCreator.updateValues(null);
	}

	private void flipSelectedFaceV() {
		Element el = manager.getCurrentElement();
		if (el == null) return;
		Face face = el.getSelectedFace();
		if (face == null) return;

		if (face.isAutoUVEnabled()) face.setAutoUVEnabled(false);
		double sv = face.getStartV();
		double ev = face.getEndV();
		face.setStartV(ev);
		face.setEndV(sv);
		face.updateUV();
		ModelCreator.updateValues(null);
	}

	private boolean handleToolButtonClick() {
		int mx = Mouse.getX();
		int my = canvasHeight - Mouse.getY() + 10;
		if (my < TOOLBTN_Y || my > TOOLBTN_Y + TOOLBTN_H) return false;

		int moveX = getMoveBtnX();
		int selectX = getSelectBtnX();
		int scopeX = getMoveScopeBtnX();
		int flipUdX = getFlipUdBtnX();
		int flipLrX = getFlipLrBtnX();

		if (mx >= flipLrX && mx <= flipLrX + TOOLBTN_W) {
			flipSelectedFaceU();
			return true;
		}
		if (mx >= flipUdX && mx <= flipUdX + TOOLBTN_W) {
			flipSelectedFaceV();
			return true;
		}
		if (mx >= scopeX && mx <= scopeX + TOOLBTN_W) {
			uvMoveAll = !uvMoveAll;
			return true;
		}
		if (mx >= selectX && mx <= selectX + TOOLBTN_W) {
			uvTool = UvTool.SELECT;
			return true;
		}
		if (mx >= moveX && mx <= moveX + TOOLBTN_W) {
			uvTool = UvTool.MOVE;
			return true;
		}
		return false;
	}

	private void moveFaceTexNoRewrap(Face face, double du, double dv) {
		if (face == null) return;
		if (du == 0 && dv == 0) return;
		face.textureU += du;
		face.textureUEnd += du;
		face.textureV += dv;
		face.textureVEnd += dv;
		ModelCreator.DidModify();
	}


	int blockFaceTextureSize;
	int blockFaceTextureMaxWidth;

	void drawRectsBlockTextureMode(int canvasHeight) {
		Element elem = manager.getCurrentElement();
		if (elem == null) return;
		
		float[] bright = elem != null ? elem.brightnessByFace : brightnessByFace;
		
		
		
		Face[] faces = elem.getAllFaces();

		if(!Mouse.isButtonDown(0) && !Mouse.isButtonDown(1)) {
			grabbedFaceIndex = getGrabbedFace(elem, canvasHeight, Mouse.getX(), Mouse.getY());
		}
		
		
		int topPadding = 30;
		int leftPadding = 5;
		int rightpadding = 5;
		int bottompadding = 10;

		int betweenpadding = 10;

		double horizontalSpace = GetSidebarWidth() - rightpadding - leftPadding;
		double verticalSpace = canvasHeight - topPadding - bottompadding - 5*betweenpadding;
		
		boolean doDoubleColumn = horizontalSpace / (verticalSpace/6) >= 2;
		
		boolean onlyTallTextures = true;
		
		blockFaceTextureMaxWidth = 0;
		
		for (int i = 0; i < 6; i++) {
			
			Face face = faces[i];
			if (!face.isEnabled()) continue;
			
			Sized texSize = GetBlockTextureModeTextureSize(face.getTextureCode(), blockFaceTextureSize);
			
			
			onlyTallTextures &=(texSize.H / texSize.W) >= 2;
			doDoubleColumn |= (texSize.H / texSize.W) >= 2;
			
			blockFaceTextureMaxWidth = Math.max(blockFaceTextureMaxWidth, (int)texSize.W);
		}
		
		
		if (doDoubleColumn) {
			horizontalSpace = GetSidebarWidth() - leftPadding - rightpadding - betweenpadding;
			verticalSpace = canvasHeight - topPadding - bottompadding - 2*betweenpadding;
			
			blockFaceTextureSize = (int)Math.min(horizontalSpace / 2, verticalSpace / 3);	
		} else {
			blockFaceTextureSize = (int)Math.min(horizontalSpace, verticalSpace / 6);
		}
		
		if (onlyTallTextures) {
			blockFaceTextureSize *=2;
			blockFaceTextureMaxWidth *= 2;
		}
		
		
		glPushMatrix();
		{
			glTranslatef(leftPadding, topPadding, 0);

			
			int countleft = 0;
			int countright = 0;
			int faceCnt=0;

			for (int i = 0; i < 6; i++) {
				
				Face face = faces[i];
				if (!face.isEnabled()) continue;
				faceCnt++;
				Sized texSize = GetBlockTextureModeTextureSize(face.getTextureCode(), blockFaceTextureSize);
				
				glPushMatrix(); {
					if (faceCnt >= 3 && doDoubleColumn) {
						glTranslatef(betweenpadding + blockFaceTextureMaxWidth, countright * (blockFaceTextureSize + betweenpadding), 0);
						startX[i] = leftPadding + betweenpadding + blockFaceTextureMaxWidth;
						startY[i] = countright * (blockFaceTextureSize + betweenpadding) + topPadding;
						countright++;
					}
					else
					{
						glTranslatef(0, countleft * (blockFaceTextureSize + betweenpadding), 0);
						startX[i] = leftPadding;
						startY[i] = countleft * (blockFaceTextureSize + betweenpadding) + topPadding;
						countleft++;
					}

					Color color = Face.getFaceColour(i);
					glColor3f(color.r * bright[i], color.g * bright[i], color.b * bright[i]);

					// Texture
					face.bindTexture();
					
					float u=1, v=1;
					
					TextureEntry entry = face.getTextureEntry();
					if (entry != null) {
						u = entry.LwJglFuckeryScaleW();
						v = entry.LwJglFuckeryScaleH();
					}

					glBegin(GL_QUADS);
					{
						glTexCoord2f(0, v);
						glVertex2d(0, texSize.H);
						
						glTexCoord2f(u, v);
						glVertex2d(texSize.W, texSize.H);
						
						glTexCoord2f(u, 0);
						glVertex2d(texSize.W, 0);
		
						glTexCoord2f(0, 0);
						glVertex2d(0, 0);
					}
					glEnd();
					
					
					// Pixel grid
					TextureEntry texEntry = face.getTextureEntry();
					if (texEntry == null) {
						Sized scale = Face.getVoxel2PixelScale(ModelCreator.currentProject, texEntry);
						glLineWidth(1F);
						glBegin(GL_LINES);
						{
							GL11.glColor4f(0.9f, 0.9f, 0.9f, 0.3f);
							int pixelsW = (int)(ModelCreator.currentProject.TextureWidth * scale.W);
							int pixelsH = (int)(ModelCreator.currentProject.TextureHeight * scale.H);
							
							double height = blockFaceTextureSize * pixelsH/pixelsW;
							
							if (pixelsW <= 64 && pixelsH <= 64) {
								double sectionWidth = (double)blockFaceTextureSize / pixelsW;
								for (double k= 0; k <= pixelsW; k++) {
									glVertex2d(k * sectionWidth, 0);
									glVertex2d(k * sectionWidth, height);	
								}
								
								double sectionHeight = (double)height / pixelsH;
								for (double k = 0; k <= pixelsH; k++) {
									glVertex2d(0, k * sectionHeight);
									glVertex2d(blockFaceTextureSize, k * sectionHeight);	
								}
							}
							
		
						}
						glEnd();
					}					
					
					
					glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
					EnumFonts.BEBAS_NEUE_20.drawString(5, 5, Face.getFaceName(i), BLACK_ALPHA);
				}
			
				glPopMatrix();
			}
			
			TextureImpl.bindNone();
			
			countleft = 0;
			countright = 0;
			faceCnt=0;
			for (int i = 0; i < 6; i++) {
				
				Face face = faces[i];
				if (!face.isEnabled()) continue;
				faceCnt++;
				
				Sized texSize = GetBlockTextureModeTextureSize(face.getTextureCode(), blockFaceTextureSize);
				
				glPushMatrix(); {
					if (faceCnt >= 3 && doDoubleColumn) {
						glTranslatef(betweenpadding + blockFaceTextureMaxWidth, countright * (blockFaceTextureSize + betweenpadding), 0);
						startX[i] = leftPadding + betweenpadding + blockFaceTextureMaxWidth;
						startY[i] = countright * (blockFaceTextureSize + betweenpadding) + topPadding;
						countright++;
					}
					else
					{
						glTranslatef(0, countleft * (blockFaceTextureSize + betweenpadding), 0);
						startX[i] = leftPadding;
						startY[i] = countleft * (blockFaceTextureSize + betweenpadding) + topPadding;
						countleft++;
					}	

					Sized uv = face.translateVoxelPosToUvPos(face.getStartU(), face.getStartV(), true);
					Sized uvend = face.translateVoxelPosToUvPos(face.getEndU(), face.getEndV(), true);					
					
					glColor3f(1, 1, 1);
					
					if (selectedBlockFaces[i]) {
						glColor3f(1f, 1f, 0f);
					}
					
					if (grabbedFaceIndex == i) {
						glColor3f(0f, 1f, 0.75f);
					}

					glBegin(GL_LINES);
					{
						glVertex2d(uv.W * texSize.W, uv.H * texSize.H);
						glVertex2d(uv.W * texSize.W, uvend.H * texSize.H);
		
						glVertex2d(uv.W * texSize.W, uvend.H * texSize.H);
						glVertex2d(uvend.W * texSize.W, uvend.H * texSize.H);
		
						glVertex2d(uvend.W * texSize.W, uvend.H * texSize.H);
						glVertex2d(uvend.W * texSize.W, uv.H * texSize.H);
		
						glVertex2d(uvend.W * texSize.W, uv.H * texSize.H);
						glVertex2d(uv.W * texSize.W, uv.H * texSize.H);
					}
					glEnd();

				}
			
				glPopMatrix();
			}
		}
		glPopMatrix();

		// Marquee selection overlay (block texture mode)
		if (marqueeActive && !marqueeInEntityMode) {
			TextureImpl.bindNone();
			GL11.glEnable(GL11.GL_BLEND);
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

			int x1 = Math.min(marqueeStartX, marqueeCurX);
			int y1 = Math.min(marqueeStartY, marqueeCurY);
			int x2 = Math.max(marqueeStartX, marqueeCurX);
			int y2 = Math.max(marqueeStartY, marqueeCurY);

			GL11.glColor4f(1f, 1f, 0f, 0.15f);
			glBegin(GL_QUADS);
			{
				glVertex2d(x1, y2);
				glVertex2d(x2, y2);
				glVertex2d(x2, y1);
				glVertex2d(x1, y1);
			}
			glEnd();

			GL11.glColor4f(1f, 1f, 0f, 0.9f);
			glBegin(GL_LINES);
			{
				glVertex2d(x1, y1);
				glVertex2d(x1, y2);

				glVertex2d(x1, y2);
				glVertex2d(x2, y2);

				glVertex2d(x2, y2);
				glVertex2d(x2, y1);

				glVertex2d(x2, y1);
				glVertex2d(x1, y1);
			}
			glEnd();
		}
	}

	// Track the last mouse position with sub-pixel precision so UV dragging can be smooth even
	// when the texture preview is heavily scaled (e.g. 16x16 shown large).
	private double lastMouseX, lastMouseY;
	private boolean grabbing = false;
	Element grabbedElement;
	int grabbedFaceIndex = -1;

	@Override
	public void mouseUp() {
		super.mouseUp();
		toolButtonLatch = false;
		
		if (marqueeActive) {
			finalizeMarqueeSelection();
			marqueeActive = false;
		}

		if (grabbing) {
			ModelCreator.changeHistory.endMultichangeHistoryState(ModelCreator.currentProject);
		}
		grabbing = false;
	}
	
	@Override
	public void onMouseDownOnPanel()
	{
		boolean lmbDown = Mouse.isButtonDown(0);
		boolean rmbDown = Mouse.isButtonDown(1);
		boolean anyDown = lmbDown || rmbDown;

		if (!grabbing && !ModelCreator.currentProject.EntityTextureMode && anyDown) {
			int width = GetSidebarWidth();
			int nowMouseX = Mouse.getX();
			if (Math.abs(nowMouseX - width) < 4) {
				grabbedElement = null;
			}
		}

		if (grabbedElement == null) {
			super.onMouseDownOnPanel();
		}

		if (nowResizingSidebar) return;

		// Tool buttons (top-right)
		// onMouseDownOnPanel is called continuously while held; latch so a click toggles only once.
		if (!marqueeActive && !grabbing && lmbDown && !toolButtonLatch && handleToolButtonClick()) {
			toolButtonLatch = true;
			return;
		}

		// Marquee selection (Select tool, or Shift-drag while in Move tool)
		if (!marqueeActive && !grabbing && lmbDown && !rmbDown && (uvTool == UvTool.SELECT || isShiftDown())) {
			marqueeStartMouseXRaw = Mouse.getX();
			marqueeStartMouseYRaw = Mouse.getY();
			marqueeCurMouseXRaw = marqueeStartMouseXRaw;
			marqueeCurMouseYRaw = marqueeStartMouseYRaw;
			marqueeAdditive = isShiftDown();

			if (ModelCreator.currentProject.EntityTextureMode) {
				TexArea area = getTexAreaAtMouse();
				if (area != null) {
					marqueeActive = true;
					marqueeInEntityMode = true;
					marqueeTextureCode = area.textureCode;
					marqueeBoxW = area.boxW;
					marqueeBoxH = area.boxH;
					marqueeStartX = area.localX;
					marqueeStartY = area.localY;
					marqueeCurX = area.localX;
					marqueeCurY = area.localY;
				}
			} else {
				marqueeActive = true;
				marqueeInEntityMode = false;
				marqueeTextureCode = null;
				marqueeBoxW = 0;
				marqueeBoxH = 0;
				marqueeStartX = marqueeStartMouseXRaw;
				marqueeStartY = canvasHeight - marqueeStartMouseYRaw + 10;
				marqueeCurX = marqueeStartX;
				marqueeCurY = marqueeStartY;
			}

			if (marqueeActive) {
				return;
			}
		}

		if (marqueeActive) {
			marqueeCurMouseXRaw = Mouse.getX();
			marqueeCurMouseYRaw = Mouse.getY();
			if (marqueeInEntityMode) {
				TexArea area = getMouseLocalInTexture(marqueeTextureCode, marqueeBoxW, marqueeBoxH);
				if (area != null) {
					marqueeCurX = area.localX;
					marqueeCurY = area.localY;
					marqueeBoxW = area.boxW;
					marqueeBoxH = area.boxH;
				}
			} else {
				marqueeCurX = marqueeCurMouseXRaw;
				marqueeCurY = canvasHeight - marqueeCurMouseYRaw + 10;
			}
			return;
		}

		// Only drag-edit in Move tool (LMB) or with RMB (resize)
		boolean nowGrabbing = rmbDown || (uvTool == UvTool.MOVE && lmbDown && !isShiftDown());

		if (ModelCreator.currentProject.EntityTextureMode && !nowGrabbing) {
			grabbedElement = currentHoveredElementEntityTextureMode(ModelCreator.currentProject.rootElements);
		}

		if (!ModelCreator.currentProject.EntityTextureMode && !nowGrabbing) {
			grabbedElement = ModelCreator.currentProject.SelectedElement;
		}

		if (!grabbing && nowGrabbing) {
			if (ModelCreator.currentProject.EntityTextureMode) {
				grabbedElement = currentHoveredElementEntityTextureMode(ModelCreator.currentProject.rootElements);
			} else {
				grabbedElement = ModelCreator.currentProject.SelectedElement;
			}

			if (grabbedElement == null) return;

			this.lastMouseX = Mouse.getX();
			this.lastMouseY = Mouse.getY();

			if (!ModelCreator.currentProject.EntityTextureMode) {
				grabbedFaceIndex = getGrabbedFace(grabbedElement, canvasHeight, (int)lastMouseX, (int)lastMouseY);
			}

			if (grabbedElement.getSelectedFaceIndex() != grabbedFaceIndex) {
				grabbedElement.setSelectedFace(grabbedFaceIndex);
			}

			// In Move tool: clicking an unselected UV should select it (single) before moving
			if (uvTool == UvTool.MOVE && lmbDown && !isShiftDown()) {
				if (ModelCreator.currentProject.EntityTextureMode) {
					Face f = (grabbedFaceIndex >= 0) ? grabbedElement.getAllFaces()[grabbedFaceIndex] : null;
					if (f != null && !isEntityUvSelected(grabbedElement, grabbedFaceIndex, f.getTextureCode())) {
						setSingleEntityUvSelection(grabbedElement, grabbedFaceIndex, f.getTextureCode());
					}
				} else {
					if (grabbedFaceIndex >= 0 && !selectedBlockFaces[grabbedFaceIndex]) {
						setSingleBlockFaceSelection(grabbedFaceIndex);
					}
				}
			}

			ModelCreator.currentProject.selectElement(grabbedElement);
		}

		grabbing = nowGrabbing;
		if (grabbedElement == null) return;

		if (grabbing)
		{
			ModelCreator.changeHistory.beginMultichangeHistoryState();

			int newMouseX = Mouse.getX();
			int newMouseY = Mouse.getY();
			int xMovement = 0;
			int yMovement = 0;
			TextureEntry texEntry = null;

			boolean doMove = (uvTool == UvTool.MOVE) && Mouse.isButtonDown(0) && !isShiftDown();
			boolean doResize = Mouse.isButtonDown(1);

			if (grabbedFaceIndex >= 0) {
				texEntry = grabbedElement.getAllFaces()[grabbedFaceIndex].getTextureEntry();
			}

			if (!ModelCreator.currentProject.EntityTextureMode && grabbedFaceIndex >= 0) {
				Sized texSize = GetBlockTextureModeTextureSize(texEntry == null ? null : texEntry.code, blockFaceTextureSize);
				texBoxWidth = (int)texSize.W;
				texBoxHeight = (int)texSize.H;
			}

			Sized scale = Face.getVoxel2PixelScale(ModelCreator.currentProject, texEntry);

			double mousedx = (newMouseX - this.lastMouseX);
			double mousedy = (newMouseY - this.lastMouseY);

			int pixelsW = (int)(ModelCreator.currentProject.TextureWidth * scale.W);
			int pixelsH = (int)(ModelCreator.currentProject.TextureHeight * scale.H);

			if (texEntry != null) {
				int[] size = ModelCreator.currentProject.TextureSizes.get(texEntry.code);
				if (size != null) {
					pixelsW = (int)(size[0] * scale.W);
					pixelsH = (int)(size[1] * scale.H);
				}
			}

			double sectionWidth = (double)texBoxWidth / pixelsW;
			double sectionHeight = (double)texBoxHeight / pixelsH;

			double xMoveF = mousedx / sectionWidth;
			double yMoveF = mousedy / sectionHeight;

			// Keep integer movements for RMB resize (pixel-accurate), but allow smooth fractional
			// movements for LMB drag in the Move tool.
			xMovement = (int)(xMoveF);
			yMovement = (int)(yMoveF);

			float du = (float)(xMoveF / scale.W);
			float dv = (float)(-yMoveF / scale.H);

			if (ModelCreator.currentProject.EntityTextureMode) {
				if ((mousedx != 0 || mousedy != 0) && grabbedElement != null && doMove)
				{
					Face face = null;
					if (grabbedFaceIndex >= 0) face = grabbedElement.getAllFaces()[grabbedFaceIndex];

					boolean moveGroup = false;
					if (face != null) {
						moveGroup = isEntityUvSelected(grabbedElement, grabbedFaceIndex, face.getTextureCode()) && selectedUvs.size() > 1;
					}

					if (moveGroup) {
						HashSet<Element> movedElems = new HashSet<Element>();
						for (UvSelection sel : selectedUvs) {
							Element e = sel.elem;
							if (e == null) continue;
							Face sf = (sel.faceIndex >= 0 && sel.faceIndex < 6) ? e.getAllFaces()[sel.faceIndex] : null;
							if (sf == null) continue;

							if (!e.isAutoUnwrapEnabled()) {
								sf.moveTextureU(du);
								sf.moveTextureV(dv);
							} else {
								if (uvMoveAll) {
									if (movedElems.add(e)) {
										e.moveTexUVStartNoRewrap(du, dv);
									}
								} else {
									moveFaceTexNoRewrap(sf, du, dv);
								}
							}
						}
					} else {
						if (face == null) {
							// Fallback: translate the whole layout
							grabbedElement.moveTexUVStartNoRewrap(du, dv);
						} else if (!grabbedElement.isAutoUnwrapEnabled()) {
							face.moveTextureU(du);
							face.moveTextureV(dv);
						} else {
							if (uvMoveAll) {
								grabbedElement.moveTexUVStartNoRewrap(du, dv);
							} else {
								moveFaceTexNoRewrap(face, du, dv);
							}
						}
					}
				}
			} else {
				if (grabbedFaceIndex == -1) return;

				// Move/resize one or many selected faces
				int selectedCount = 0;
				for (int i = 0; i < 6; i++) if (selectedBlockFaces[i]) selectedCount++;
				boolean moveGroup = selectedCount > 1 && selectedBlockFaces[grabbedFaceIndex];

				if (doMove)
				{
					if (moveGroup) {
						for (int i = 0; i < 6; i++) {
							if (!selectedBlockFaces[i]) continue;
							Face f = grabbedElement.getAllFaces()[i];
							if (f == null || !f.isEnabled()) continue;
							f.moveTextureU(du);
							f.moveTextureV(dv);
							f.updateUV();
						}
					} else {
						Face face = grabbedElement.getAllFaces()[grabbedFaceIndex];
						if (face != null) {
							face.moveTextureU(du);
							face.moveTextureV(dv);
							face.updateUV();
						}
					}
				}
				else if (doResize)
				{
					if (moveGroup) {
						for (int i = 0; i < 6; i++) {
							if (!selectedBlockFaces[i]) continue;
							Face f = grabbedElement.getAllFaces()[i];
							if (f == null || !f.isEnabled()) continue;
							f.setAutoUVEnabled(false);

							f.addTextureUEnd(xMovement / scale.W);
							f.addTextureVEnd(-yMovement / scale.H);

							f.setAutoUVEnabled(false);
							f.updateUV();
						}
					} else {
						Face face = grabbedElement.getAllFaces()[grabbedFaceIndex];
						if (face != null) {
							face.setAutoUVEnabled(false);

							face.addTextureUEnd(xMovement / scale.W);
							face.addTextureVEnd(-yMovement / scale.H);

							face.setAutoUVEnabled(false);
							face.updateUV();
						}
					}
				}
				else {
					return;
				}
			}

			if (doMove) {
				// Move tool: keep the mouse lockstep with the UVs, including fractional movement.
				this.lastMouseX = newMouseX;
				this.lastMouseY = newMouseY;
				if (mousedx != 0 || mousedy != 0) {
					ModelCreator.updateValues(null);
				}
			} else {
				// Resize tool (RMB): keep pixel-accurate snapping like before.
				if (xMovement != 0) {
					this.lastMouseX += xMovement * sectionWidth;   // Add *sectionWidth because otherwise the rect moves too quickly
				}

				if (yMovement != 0) {
					this.lastMouseY += yMovement * sectionHeight;
				}

				if (xMovement != 0 || yMovement != 0) {
					ModelCreator.updateValues(null);
				}
			}
		}
	}

	public Sized GetBlockTextureModeTextureSize(String textureCode, float maxHeight) {
		double texWidth = ModelCreator.currentProject.TextureWidth;
		double texHeight = ModelCreator.currentProject.TextureHeight;
		
		if (textureCode != null) {
			int[] size = ModelCreator.currentProject.TextureSizes.get(textureCode);
			if (size != null) {
				texWidth = size[0];
				texHeight = size[1];
			}
		}
		
		int texBoxWidth = (int)(blockFaceTextureSize);
		int texBoxHeight = (int)(texBoxWidth * texHeight / texWidth);
		

		if (texBoxHeight > blockFaceTextureSize) {
			double div = texBoxHeight / blockFaceTextureSize;
			texBoxWidth /= div;
			texBoxHeight /= div;
		}

		return new Sized(texBoxWidth, texBoxHeight);
	}
	

	public int getGrabbedFace(Element elem, int canvasHeight, int mouseX, int mouseY)
	{
		if (elem == null) return -1;
		Face[] faces = elem.getAllFaces();
		if (faces == null) return -1;
		
		for (int i = 0; i < 6; i++)
		{
			if (faces[i] == null || !faces[i].isEnabled()) {
				continue;
			}
			
			if (mouseX >= startX[i] && mouseX <= startX[i] + blockFaceTextureMaxWidth)
			{
				if ((canvasHeight - mouseY + 10) >= startY[i] && (canvasHeight - mouseY + 10) <= startY[i] + blockFaceTextureSize)
				{
					return i;
				}
			}
		}
		
		return -1;
	}
	
	
	public int Clamp(int val, int min, int max) {
		return Math.min(Math.max(val, min), max);
	}
	
	private Element currentHoveredElementEntityTextureMode(ArrayList<Element> elems)
	{
		if (texBoxHeight == 0 || texBoxWidth == 0) return null;
		
		int mouseX = Mouse.getX() - 10;
		int mouseY = (canvasHeight - Mouse.getY()) - 30;
		String currentHoveredTextureCode = null;
		boolean found=false;
		
		for (String textureCode : getTextureCodes()) {
			double texWidth = ModelCreator.currentProject.TextureWidth;
			double texHeight = ModelCreator.currentProject.TextureHeight;
			Sized scale;
			
			TextureEntry texEntry = null;
			if (textureCode != null) {
				texEntry = ModelCreator.currentProject.TexturesByCode.get(textureCode);
				int[] size = ModelCreator.currentProject.TextureSizes.get(textureCode);
				if (size != null) {
					texWidth = size[0];
					texHeight = size[1];
				}
			}
			
			scale = Face.getVoxel2PixelScale(ModelCreator.currentProject, texEntry);
			
			texWidth *= scale.W / 2;
			texHeight *= scale.H / 2;
			
			texBoxWidth = (int)(GetSidebarWidth() - 20);
			texBoxHeight = (int)(texBoxWidth * texHeight / texWidth);
			
			if (mouseY >= 0 && mouseY < texBoxHeight) {
				currentHoveredTextureCode = textureCode;
				found=true;
				break;
			}
			
			mouseY -= texBoxHeight + 20;
		}
		
		if (!found) return null; 
		
		
		double mouseU = (double)mouseX / texBoxWidth;
		double mouseV = (double)mouseY / texBoxHeight; 
				
		for (Element elem : elems) {
			Face[] faces = elem.getAllFaces();
			if (!elem.getRenderInEditor()) continue;
			
			for (int i = 0; i < 6; i++) {
				Face face = faces[i];
				if (!face.isEnabled() || (currentHoveredTextureCode != null && !currentHoveredTextureCode.equals(face.getTextureCode()))) continue;
				
				Sized uv = face.translateVoxelPosToUvPos(face.getStartU(), face.getStartV(), true);
				Sized uvend = face.translateVoxelPosToUvPos(face.getEndU(), face.getEndV(), true);

				if (mouseU >= uv.W && mouseV >= uv.H && mouseU <= uvend.W && mouseV <= uvend.H) {
					grabbedFaceIndex = i;
					return elem;
				}
			}
			
			Element foundElem = currentHoveredElementEntityTextureMode(elem.ChildElements);
			if (foundElem != null) return foundElem;
		}
		
		return null;
	}

	
}
