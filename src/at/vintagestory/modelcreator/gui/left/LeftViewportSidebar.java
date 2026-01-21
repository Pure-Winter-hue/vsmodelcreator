package at.vintagestory.modelcreator.gui.left;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.input.Mouse;
import org.newdawn.slick.Color;
import org.newdawn.slick.opengl.TextureImpl;

import at.vintagestory.modelcreator.ModelCreator;
import at.vintagestory.modelcreator.enums.EnumFonts;

/**
 * Small left-side tool panel for the 3D viewport.
 * Hosts multi-selection move scope (THIS/ALL) and Floor Snap toggle.
 */
public class LeftViewportSidebar extends LeftSidebar
{
	private int lastCanvasHeight;
	private boolean lmbWasDown;

	// Button layout (top-left coordinate space: y grows downward)
	private int btnScopeX, btnScopeY, btnScopeW, btnScopeH;
	private int btnSnapX, btnSnapY, btnSnapW, btnSnapH;
	private int btnFreedomX, btnFreedomY, btnFreedomW, btnFreedomH;

	// Axis thickness slider
	private int sliderAxisX, sliderAxisY, sliderAxisW, sliderAxisH;
	private boolean axisSliderDrag;
	private final int axisThickMin = 2;
	private final int axisThickMax = 40;

	// Axis length slider
	private int sliderAxisLenX, sliderAxisLenY, sliderAxisLenW, sliderAxisLenH;
	private boolean axisLenSliderDrag;
	private final int axisLenMin = 40;
	private final int axisLenMax = 260;


	// Center circle handle toggle
	private int btnCenterX, btnCenterY, btnCenterW, btnCenterH;

	// Center circle size slider (radius in px)
	private int sliderCenterX, sliderCenterY, sliderCenterW, sliderCenterH;
	private boolean centerSliderDrag;
	private final int centerRadMin = 6;
	private final int centerRadMax = 60;

	public LeftViewportSidebar() {
		super("Viewport");
	}

	@Override
	public void draw(int sidebarWidth, int canvasWidth, int canvasHeight, int frameHeight)
	{
		this.lastCanvasHeight = canvasHeight;
		super.draw(sidebarWidth, canvasWidth, canvasHeight, frameHeight);

		// Font rendering relies on blending for alpha in the glyph atlas.
		boolean blendWasEnabled = glIsEnabled(GL_BLEND);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		int pad = 10;
		int x = pad;
		int y = 34;
		int w = Math.max(60, GetSidebarWidth() - pad * 2);
		int h = 28;

		// --- Move scope ---
		EnumFonts.BEBAS_NEUE_16.drawString(x, y + 2, "MOVE", new Color(0.75f, 0.75f, 0.82f));
		y += 18;
		btnScopeX = x;
		btnScopeY = y;
		btnScopeW = w;
		btnScopeH = h;
		drawToggleButton(btnScopeX, btnScopeY, btnScopeW, btnScopeH,
				ModelCreator.viewportGroupMoveEnabled,
				ModelCreator.viewportGroupMoveEnabled ? "ALL" : "THIS");

		// --- Floor snap ---
		y += h + 18;
		EnumFonts.BEBAS_NEUE_16.drawString(x, y + 2, "FLOOR SNAP", new Color(0.75f, 0.75f, 0.82f));
		y += 18;
		btnSnapX = x;
		btnSnapY = y;
		btnSnapW = w;
		btnSnapH = h;
		drawToggleButton(btnSnapX, btnSnapY, btnSnapW, btnSnapH,
				ModelCreator.viewportFloorSnapEnabled,
				ModelCreator.viewportFloorSnapEnabled ? "ON" : "OFF");

		// --- Freedom mode (legacy camera-relative gizmo) ---
		y += h + 18;
		EnumFonts.BEBAS_NEUE_16.drawString(x, y + 2, "FREEDOM MODE", new Color(0.75f, 0.75f, 0.82f));
		y += 18;
		btnFreedomX = x;
		btnFreedomY = y;
		btnFreedomW = w;
		btnFreedomH = h;
		drawToggleButton(btnFreedomX, btnFreedomY, btnFreedomW, btnFreedomH,
				ModelCreator.viewportFreedomModeEnabled,
				ModelCreator.viewportFreedomModeEnabled ? "ON" : "OFF");

		// --- Axis thickness ---
		y += h + 18;
		EnumFonts.BEBAS_NEUE_16.drawString(x, y + 2, "AXIS THICKNESS", new Color(0.75f, 0.75f, 0.82f));
		String vtxt = ModelCreator.viewportAxisLineThickness + "px";
		float vtw = EnumFonts.BEBAS_NEUE_16.getWidth(vtxt);
		EnumFonts.BEBAS_NEUE_16.drawString(x + w - (int)vtw, y + 2, vtxt, new Color(0.75f, 0.75f, 0.82f));
		y += 18;
		sliderAxisX = x;
		sliderAxisY = y;
		sliderAxisW = w;
		sliderAxisH = 18;
		drawSlider(sliderAxisX, sliderAxisY, sliderAxisW, sliderAxisH, ModelCreator.viewportAxisLineThickness, axisThickMin, axisThickMax);

		// --- Axis length ---
		y += sliderAxisH + 18;
		EnumFonts.BEBAS_NEUE_16.drawString(x, y + 2, "AXIS LENGTH", new Color(0.75f, 0.75f, 0.82f));
		String vtxt2 = ModelCreator.viewportAxisLineLength + "px";
		float vtw2 = EnumFonts.BEBAS_NEUE_16.getWidth(vtxt2);
		EnumFonts.BEBAS_NEUE_16.drawString(x + w - (int)vtw2, y + 2, vtxt2, new Color(0.75f, 0.75f, 0.82f));
		y += 18;
		sliderAxisLenX = x;
		sliderAxisLenY = y;
		sliderAxisLenW = w;
		sliderAxisLenH = 18;
		drawSlider(sliderAxisLenX, sliderAxisLenY, sliderAxisLenW, sliderAxisLenH, ModelCreator.viewportAxisLineLength, axisLenMin, axisLenMax);

		// --- Center circle handle ---
		y += sliderAxisLenH + 18;
		EnumFonts.BEBAS_NEUE_16.drawString(x, y + 2, "CENTER CIRCLE", new Color(0.75f, 0.75f, 0.82f));
		y += 18;
		btnCenterX = x;
		btnCenterY = y;
		btnCenterW = w;
		btnCenterH = h;
		drawToggleButton(btnCenterX, btnCenterY, btnCenterW, btnCenterH,
				ModelCreator.viewportCenterCircleEnabled,
				ModelCreator.viewportCenterCircleEnabled ? "ON" : "OFF");

		// --- Center circle size ---
		y += h + 18;
		EnumFonts.BEBAS_NEUE_16.drawString(x, y + 2, "CENTER SIZE", new Color(0.75f, 0.75f, 0.82f));
		String vtxt3 = ModelCreator.viewportCenterCircleRadius + "px";
		float vtw3 = EnumFonts.BEBAS_NEUE_16.getWidth(vtxt3);
		EnumFonts.BEBAS_NEUE_16.drawString(x + w - (int)vtw3, y + 2, vtxt3, new Color(0.75f, 0.75f, 0.82f));
		y += 18;
		sliderCenterX = x;
		sliderCenterY = y;
		sliderCenterW = w;
		sliderCenterH = 18;
		drawSlider(sliderCenterX, sliderCenterY, sliderCenterW, sliderCenterH, ModelCreator.viewportCenterCircleRadius, centerRadMin, centerRadMax);

		// Restore blend state for the rest of the render pipeline
		if (!blendWasEnabled) glDisable(GL_BLEND);

	}

	private void drawToggleButton(int x, int y, int w, int h, boolean active, String label)
	{
		// Avoid GL texture-state bleed (can render as stray gray squares on some drivers)
		boolean texWasEnabled = glIsEnabled(GL_TEXTURE_2D);
		glDisable(GL_TEXTURE_2D);

		TextureImpl.bindNone();
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		if (active) glColor4f(0.25f, 0.35f, 0.85f, 0.90f);
		else glColor4f(0f, 0f, 0f, 0.25f);

		glBegin(GL_QUADS);
		{
			glVertex2i(x, y + h);
			glVertex2i(x + w, y + h);
			glVertex2i(x + w, y);
			glVertex2i(x, y);
		}
		glEnd();

		glColor4f(0.80f, 0.80f, 0.90f, 0.90f);
		glBegin(GL_LINES);
		{
			glVertex2i(x, y);
			glVertex2i(x, y + h);

			glVertex2i(x, y + h);
			glVertex2i(x + w, y + h);

			glVertex2i(x + w, y + h);
			glVertex2i(x + w, y);

			glVertex2i(x + w, y);
			glVertex2i(x, y);
		}
		glEnd();

		// Restore texture state for font rendering
		glEnable(GL_TEXTURE_2D);
		// Center label for legibility
		float tw = EnumFonts.BEBAS_NEUE_16.getWidth(label);
		float th = EnumFonts.BEBAS_NEUE_16.getHeight(label);
		int tx = x + (int)((w - tw) / 2f);
		int ty = y + (int)((h - th) / 2f);
		EnumFonts.BEBAS_NEUE_16.drawString(tx, ty, label, new Color(0.95F, 0.95F, 1F, 1F));
		TextureImpl.bindNone();

		if (!texWasEnabled) glDisable(GL_TEXTURE_2D);
	}

	private void drawSlider(int x, int y, int w, int h, int value, int min, int max)
	{
		boolean texWasEnabled = glIsEnabled(GL_TEXTURE_2D);
		glDisable(GL_TEXTURE_2D);
		TextureImpl.bindNone();
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		// Track
		glColor4f(0f, 0f, 0f, 0.25f);
		glBegin(GL_QUADS);
		{
			glVertex2i(x, y + h);
			glVertex2i(x + w, y + h);
			glVertex2i(x + w, y);
			glVertex2i(x, y);
		}
		glEnd();

		// Knob
		int knobW = 12;
		float t = (max == min) ? 0f : (value - min) / (float)(max - min);
		t = Math.max(0f, Math.min(1f, t));
		int kx = x + (int)((w - knobW) * t);
		glColor4f(0.25f, 0.35f, 0.85f, 0.90f);
		glBegin(GL_QUADS);
		{
			glVertex2i(kx, y + h);
			glVertex2i(kx + knobW, y + h);
			glVertex2i(kx + knobW, y);
			glVertex2i(kx, y);
		}
		glEnd();

		// Border
		glColor4f(0.80f, 0.80f, 0.90f, 0.90f);
		glBegin(GL_LINES);
		{
			glVertex2i(x, y);
			glVertex2i(x, y + h);

			glVertex2i(x, y + h);
			glVertex2i(x + w, y + h);

			glVertex2i(x + w, y + h);
			glVertex2i(x + w, y);

			glVertex2i(x + w, y);
			glVertex2i(x, y);
		}
		glEnd();

		glEnable(GL_TEXTURE_2D);
		TextureImpl.bindNone();
		if (!texWasEnabled) glDisable(GL_TEXTURE_2D);
	}

	private void setAxisThicknessFromMouse(int mx)
	{
		int x0 = sliderAxisX;
		int x1 = sliderAxisX + sliderAxisW;
		if (x1 <= x0) return;
		float t = (mx - x0) / (float)(x1 - x0);
		t = Math.max(0f, Math.min(1f, t));
		int val = axisThickMin + Math.round(t * (axisThickMax - axisThickMin));
		val = Math.max(axisThickMin, Math.min(axisThickMax, val));
		if (val != ModelCreator.viewportAxisLineThickness) {
			ModelCreator.viewportAxisLineThickness = val;
			ModelCreator.prefs.putInt("viewportAxisLineThickness", val);
		}
	}

	private void setAxisLengthFromMouse(int mx)
	{
		int x0 = sliderAxisLenX;
		int x1 = sliderAxisLenX + sliderAxisLenW;
		if (x1 <= x0) return;
		float t = (mx - x0) / (float)(x1 - x0);
		t = Math.max(0f, Math.min(1f, t));
		int val = axisLenMin + Math.round(t * (axisLenMax - axisLenMin));
		val = Math.max(axisLenMin, Math.min(axisLenMax, val));
		if (val != ModelCreator.viewportAxisLineLength) {
			ModelCreator.viewportAxisLineLength = val;
			ModelCreator.prefs.putInt("viewportAxisLineLength", val);
		}
	}



	private void setCenterRadiusFromMouse(int mx)
	{
		int x0 = sliderCenterX;
		int x1 = sliderCenterX + sliderCenterW;
		if (x1 <= x0) return;
		float t = (mx - x0) / (float)(x1 - x0);
		t = Math.max(0f, Math.min(1f, t));
		int val = centerRadMin + Math.round(t * (centerRadMax - centerRadMin));
		val = Math.max(centerRadMin, Math.min(centerRadMax, val));
		if (val != ModelCreator.viewportCenterCircleRadius) {
			ModelCreator.viewportCenterCircleRadius = val;
			ModelCreator.prefs.putInt("viewportCenterCircleRadius", val);
		}
	}


	@Override
	public void onMouseDownOnPanel()
	{
		super.onMouseDownOnPanel();
		if (isResizingSidebar()) return;

		boolean lmb = Mouse.isButtonDown(0);
		if (lmb) {
			int mx = Mouse.getX();
			int my = lastCanvasHeight - Mouse.getY();

			// Begin slider drag if clicking a slider
			if (!axisSliderDrag && !axisLenSliderDrag && !centerSliderDrag && inside(mx, my, sliderAxisX, sliderAxisY, sliderAxisW, sliderAxisH)) {
				axisSliderDrag = true;
				setAxisThicknessFromMouse(mx);
			}
			if (!axisSliderDrag && !axisLenSliderDrag && !centerSliderDrag && inside(mx, my, sliderAxisLenX, sliderAxisLenY, sliderAxisLenW, sliderAxisLenH)) {
				axisLenSliderDrag = true;
				setAxisLengthFromMouse(mx);
			}
			if (!axisSliderDrag && !axisLenSliderDrag && !centerSliderDrag && inside(mx, my, sliderCenterX, sliderCenterY, sliderCenterW, sliderCenterH)) {
				centerSliderDrag = true;
				setCenterRadiusFromMouse(mx);
			}

			if (!axisSliderDrag && !axisLenSliderDrag && !centerSliderDrag && !lmbWasDown) {
				if (inside(mx, my, btnScopeX, btnScopeY, btnScopeW, btnScopeH)) {
					ModelCreator.viewportGroupMoveEnabled = !ModelCreator.viewportGroupMoveEnabled;
				}
				if (inside(mx, my, btnSnapX, btnSnapY, btnSnapW, btnSnapH)) {
					ModelCreator.viewportFloorSnapEnabled = !ModelCreator.viewportFloorSnapEnabled;
				}
				if (inside(mx, my, btnFreedomX, btnFreedomY, btnFreedomW, btnFreedomH)) {
					ModelCreator.viewportFreedomModeEnabled = !ModelCreator.viewportFreedomModeEnabled;
					ModelCreator.prefs.putBoolean("viewportFreedomModeEnabled", ModelCreator.viewportFreedomModeEnabled);
				}
				if (inside(mx, my, btnCenterX, btnCenterY, btnCenterW, btnCenterH)) {
					ModelCreator.viewportCenterCircleEnabled = !ModelCreator.viewportCenterCircleEnabled;
					ModelCreator.prefs.putBoolean("viewportCenterCircleEnabled", ModelCreator.viewportCenterCircleEnabled);
				}
			}

			if (axisSliderDrag) {
				setAxisThicknessFromMouse(mx);
			}
			if (axisLenSliderDrag) {
				setAxisLengthFromMouse(mx);
			}
			if (centerSliderDrag) {
				setCenterRadiusFromMouse(mx);
			}
		} else {
			axisSliderDrag = false;
			axisLenSliderDrag = false;
			centerSliderDrag = false;
		}
		lmbWasDown = lmb;
	}


	@Override
	public void mouseUp()
	{
		super.mouseUp();
		axisSliderDrag = false;
		axisLenSliderDrag = false;
		centerSliderDrag = false;
		lmbWasDown = false;
	}

	private boolean inside(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx <= x + w && my >= y && my <= y + h;
	}
}
