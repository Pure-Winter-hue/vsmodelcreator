package at.vintagestory.modelcreator.gui.left;

import static org.lwjgl.opengl.GL11.*;

import java.util.Locale;

import org.lwjgl.input.Mouse;
import org.newdawn.slick.Color;
import org.newdawn.slick.opengl.TextureImpl;

import at.vintagestory.modelcreator.ModelCreator;
import at.vintagestory.modelcreator.input.InputListener;
import at.vintagestory.modelcreator.input.key.InputKeyEvent;
import at.vintagestory.modelcreator.enums.EnumFonts;

/**
 * Small left-side tool panel for the 3D viewport.
 * Hosts multi-selection move scope (THIS/ALL) and Floor Snap toggle.
 */
public class LeftViewportSidebar extends LeftSidebar implements InputListener
{
	private int lastCanvasHeight;
	private boolean lmbWasDown;
	private boolean moveControlsExpanded = true;

	// Vertical scrolling for small viewports (no visible scrollbar)
	private int scrollOffsetPx = 0;
	private int contentHeightPx = 0;

	// Button layout (top-left coordinate space: y grows downward)
	private int btnGizmoModeX, btnGizmoModeY, btnGizmoModeW, btnGizmoModeH;
	private int btnScopeX, btnScopeY, btnScopeW, btnScopeH;
	private int btnMoveControlsX, btnMoveControlsY, btnMoveControlsW, btnMoveControlsH;
	private int btnGizmoHideX, btnGizmoHideY, btnGizmoHideW, btnGizmoHideH;

	private int btnGridSnapX, btnGridSnapY, btnGridSnapW, btnGridSnapH;
	private int btnVertexSnapX, btnVertexSnapY, btnVertexSnapW, btnVertexSnapH;
	// Grid step input field rect (in canvas pixel coords)
	private int gridStepValueX, gridStepValueY, gridStepValueW, gridStepValueH;
	private final int gridStepQMin = 1;   // 0.25
	private final int gridStepQMax = 80;  // 20.0
// Grid step editing (typed input rendered in OpenGL)
private boolean gridStepEditing = false;
private StringBuilder gridStepEditBuf = new StringBuilder();
// Slider presets for user quick selection: 0.25, 0.5, 1, 2, 3, 4, 5
private final int[] gridStepPresetQ = new int[] { 1, 2, 4, 8, 12, 16, 20 };
private int gridStepSliderX, gridStepSliderY, gridStepSliderW, gridStepSliderH;
private boolean gridStepSliderDrag = false;


	private int btnSnapX, btnSnapY, btnSnapW, btnSnapH;
	private int btnFreedomX, btnFreedomY, btnFreedomW, btnFreedomH;


private int btnRotateScopeX, btnRotateScopeY, btnRotateScopeW, btnRotateScopeH;
private int btnRotateIndX, btnRotateIndY, btnRotateIndW, btnRotateIndH;
private int btnRotateGroupPivotX, btnRotateGroupPivotY, btnRotateGroupPivotW, btnRotateGroupPivotH;

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

	// Floor grid tiling sliders (1..10)
	private int sliderGridRowsX, sliderGridRowsY, sliderGridRowsW, sliderGridRowsH;
	private int sliderGridColsX, sliderGridColsY, sliderGridColsW, sliderGridColsH;
	private boolean gridRowsSliderDrag;
	private boolean gridColsSliderDrag;
	private final int gridTilesMin = 1;
	private final int gridTilesMax = 10;

	// Show Blocks overlays
	private int btnShowBlocksFloorX, btnShowBlocksFloorY, btnShowBlocksFloorW, btnShowBlocksFloorH;
	private int btnShowBlocksAirX, btnShowBlocksAirY, btnShowBlocksAirW, btnShowBlocksAirH;
	private int sliderAirAboveX, sliderAirAboveY, sliderAirAboveW, sliderAirAboveH;
	private int sliderAirBelowX, sliderAirBelowY, sliderAirBelowW, sliderAirBelowH;
	private boolean airAboveSliderDrag;
	private boolean airBelowSliderDrag;
	private final int airLayersMin = 0;
	private final int airLayersMax = 10;

	public LeftViewportSidebar() {
		super("Viewport");
		// Restore move-controls expanded state early so first draw reflects prefs.
		try {
			moveControlsExpanded = ModelCreator.prefs.getBoolean("viewportMoveControlsExpanded", true);
		} catch (Throwable t) {
			moveControlsExpanded = true;
		}
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
		int y = 34 - scrollOffsetPx;
		int w = Math.max(60, GetSidebarWidth() - pad * 2);
		int h = 28;

		// --- Gizmo mode (Move/Rotate/Scale) ---
		EnumFonts.BEBAS_NEUE_16.drawString(x, y + 2, "GIZMO", new Color(0.75f, 0.75f, 0.82f));
		y += 18;
		btnGizmoModeX = x;
		btnGizmoModeY = y;
		btnGizmoModeW = w;
		btnGizmoModeH = h;
		String gizmoLabel;
		if (ModelCreator.viewportGizmoMode == ModelCreator.VIEWPORT_GIZMO_MODE_ROTATE) gizmoLabel = "ROTATE";
		else if (ModelCreator.viewportGizmoMode == ModelCreator.VIEWPORT_GIZMO_MODE_SCALE) gizmoLabel = "SCALE";
		else gizmoLabel = "MOVE";
		drawToggleButton(btnGizmoModeX, btnGizmoModeY, btnGizmoModeW, btnGizmoModeH,
				ModelCreator.viewportGizmoMode != ModelCreator.VIEWPORT_GIZMO_MODE_MOVE,
				gizmoLabel);

		// Divider under Gizmo section
		y += h + 10;
		drawDivider(x, y, w);
		y += 16;

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

		// --- Floor guard (Move) ---
		y += h + 18;
		EnumFonts.BEBAS_NEUE_16.drawString(x, y + 2, "FLOOR GUARD", new Color(0.75f, 0.75f, 0.82f));
		y += 18;
		btnSnapX = x;
		btnSnapY = y;
		btnSnapW = w;
		btnSnapH = h;
		drawToggleButton(btnSnapX, btnSnapY, btnSnapW, btnSnapH,
				ModelCreator.viewportFloorSnapEnabled,
				ModelCreator.viewportFloorSnapEnabled ? "ON" : "OFF");

		// --- Freedom mode (Move) ---
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

		// Divider under Freedom Mode section
		y += h + 10;
		drawDivider(x, y, w);
		y += 16;

		// --- Controls (collapsible) ---
		EnumFonts.BEBAS_NEUE_16.drawString(x, y + 2, "CONTROLS", new Color(0.75f, 0.75f, 0.82f));
		y += 18;
		btnMoveControlsX = x;
		btnMoveControlsY = y;
		btnMoveControlsW = w;
		btnMoveControlsH = h;
		drawToggleButton(btnMoveControlsX, btnMoveControlsY, btnMoveControlsW, btnMoveControlsH,
				moveControlsExpanded,
				moveControlsExpanded ? "HIDE OPTIONS" : "SHOW OPTIONS");

		if (moveControlsExpanded) {
			// --- Gizmo visibility quick toggle ---
			y += h + 18;
			EnumFonts.BEBAS_NEUE_16.drawString(x, y + 2, "GIZMO", new Color(0.75f, 0.75f, 0.82f));
			y += 18;
			btnGizmoHideX = x;
			btnGizmoHideY = y;
			btnGizmoHideW = w;
			btnGizmoHideH = h;
			drawToggleButton(btnGizmoHideX, btnGizmoHideY, btnGizmoHideW, btnGizmoHideH,
					!ModelCreator.viewportGizmoHidden,
					ModelCreator.viewportGizmoHidden ? "GIZMO SHOW" : "GIZMO HIDE");


			// --- Snapping ---
			y += h + 18;
			EnumFonts.BEBAS_NEUE_16.drawString(x, y + 2, "SNAPPING", new Color(0.75f, 0.75f, 0.82f));
			y += 18;

			// Grid snap toggle
			EnumFonts.BEBAS_NEUE_16.drawString(x, y + 2, "GRID SNAP", new Color(0.75f, 0.75f, 0.82f));
			y += 18;
			btnGridSnapX = x;
			btnGridSnapY = y;
			btnGridSnapW = w;
			btnGridSnapH = h;
			drawToggleButton(btnGridSnapX, btnGridSnapY, btnGridSnapW, btnGridSnapH,
					ModelCreator.viewportGridSnapEnabled,
					ModelCreator.viewportGridSnapEnabled ? "ON" : "OFF");

			// Grid step (text input)
			y += h + 18;
			EnumFonts.BEBAS_NEUE_16.drawString(x, y + 2, "GRID STEP", new Color(0.75f, 0.75f, 0.82f));
			double step = ModelCreator.viewportGridSnapStepQ / 4.0;
			String stepTxt;
			if (Math.abs(step - Math.round(step)) < 1e-6) stepTxt = ((int)Math.round(step)) + "";
			else stepTxt = String.format(Locale.ROOT, "%.2f", step);

			float stw = EnumFonts.BEBAS_NEUE_16.getWidth(stepTxt);
			int stx = x + w - (int)stw;
			EnumFonts.BEBAS_NEUE_16.drawString(stx, y + 2, stepTxt, new Color(0.75f, 0.75f, 0.82f));


			y += 18;
			// Draw an inline input box in place of the old drag bar.
			gridStepValueX = x;
			gridStepValueY = y;
			gridStepValueW = w;
			gridStepValueH = 20;
			String dispTxt = stepTxt;
			if (gridStepEditing) {
				dispTxt = gridStepEditBuf.length() == 0 ? "" : gridStepEditBuf.toString();
				if (((System.currentTimeMillis() / 500) % 2) == 0) dispTxt = dispTxt + "|";
			}
			drawInputBox(gridStepValueX, gridStepValueY, gridStepValueW, gridStepValueH, dispTxt);

			// Discrete slider (0.25, 0.5, 1, 2, 3, 4, 5)
			gridStepSliderX = x;
			gridStepSliderY = y + gridStepValueH + 6;
			gridStepSliderW = w;
			gridStepSliderH = 12;
			drawGridStepPresetSlider(gridStepSliderX, gridStepSliderY, gridStepSliderW, gridStepSliderH);

			// Vertex snap toggle
			y += gridStepValueH + 18;
			EnumFonts.BEBAS_NEUE_16.drawString(x, y + 2, "VERTEX SNAP", new Color(0.75f, 0.75f, 0.82f));
			y += 18;
			btnVertexSnapX = x;
			btnVertexSnapY = y;
			btnVertexSnapW = w;
			btnVertexSnapH = h;
			drawToggleButton(btnVertexSnapX, btnVertexSnapY, btnVertexSnapW, btnVertexSnapH,
					ModelCreator.viewportVertexSnapEnabled,
					ModelCreator.viewportVertexSnapEnabled ? "ON" : "OFF");

				// Face snap removed (didn't work)

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
		} else {
			// Disable hit targets while collapsed
			sliderAxisX = sliderAxisY = sliderAxisW = sliderAxisH = 0;
			sliderAxisLenX = sliderAxisLenY = sliderAxisLenW = sliderAxisLenH = 0;
			sliderCenterX = sliderCenterY = sliderCenterW = sliderCenterH = 0;
			btnCenterX = btnCenterY = btnCenterW = btnCenterH = 0;
			btnGizmoHideX = btnGizmoHideY = btnGizmoHideW = btnGizmoHideH = 0;
			y += h; // keep spacing consistent
		}

		// Divider between Move and Rotate
		y += h + 10;
		drawDivider(x, y, w);
		y += 16;

		// --- Rotate scope ---
		EnumFonts.BEBAS_NEUE_16.drawString(x, y + 2, "ROTATE", new Color(0.75f, 0.75f, 0.82f));
		y += 18;
		btnRotateScopeX = x;
		btnRotateScopeY = y;
		btnRotateScopeW = w;
		btnRotateScopeH = h;
		drawToggleButton(btnRotateScopeX, btnRotateScopeY, btnRotateScopeW, btnRotateScopeH,
				ModelCreator.viewportGroupRotateEnabled,
				ModelCreator.viewportGroupRotateEnabled ? "ALL" : "THIS");

		y += h + 18;

		// --- Rotate mode ---
		EnumFonts.BEBAS_NEUE_16.drawString(x, y + 2, "ROTATE MODE", new Color(0.75f, 0.75f, 0.82f));
		y += 18;
		btnRotateIndX = x;
		btnRotateIndY = y;
		btnRotateIndW = w;
		btnRotateIndH = h;
		drawToggleButton(btnRotateIndX, btnRotateIndY, btnRotateIndW, btnRotateIndH,
				ModelCreator.viewportRotateIndividualOriginEnabled,
				"INDIVIDUAL ORIGINS");

		y += h + 10;
		btnRotateGroupPivotX = x;
		btnRotateGroupPivotY = y;
		btnRotateGroupPivotW = w;
		btnRotateGroupPivotH = h;
		drawToggleButton(btnRotateGroupPivotX, btnRotateGroupPivotY, btnRotateGroupPivotW, btnRotateGroupPivotH,
				ModelCreator.viewportRotateGroupPivotEnabled,
				"GROUP PIVOT");

		// Divider + Floor Grid controls
		y += h + 12;
		drawDivider(x, y, w);
		y += 16;

		// --- Floor Grid Rows ---
		EnumFonts.BEBAS_NEUE_16.drawString(x, y + 2, "FLOOR GRID ROWS", new Color(0.75f, 0.75f, 0.82f));
		String rtxt = String.valueOf(ModelCreator.viewportFloorGridRows);
		float rtw = EnumFonts.BEBAS_NEUE_16.getWidth(rtxt);
		EnumFonts.BEBAS_NEUE_16.drawString(x + w - (int)rtw, y + 2, rtxt, new Color(0.75f, 0.75f, 0.82f));
		y += 18;
		sliderGridRowsX = x;
		sliderGridRowsY = y;
		sliderGridRowsW = w;
		sliderGridRowsH = 18;
		drawSlider(sliderGridRowsX, sliderGridRowsY, sliderGridRowsW, sliderGridRowsH, ModelCreator.viewportFloorGridRows, gridTilesMin, gridTilesMax);

		// --- Floor Grid Columns ---
		y += sliderGridRowsH + 18;
		EnumFonts.BEBAS_NEUE_16.drawString(x, y + 2, "FLOOR GRID COLUMNS", new Color(0.75f, 0.75f, 0.82f));
		String ctxt = String.valueOf(ModelCreator.viewportFloorGridCols);
		float ctw = EnumFonts.BEBAS_NEUE_16.getWidth(ctxt);
		EnumFonts.BEBAS_NEUE_16.drawString(x + w - (int)ctw, y + 2, ctxt, new Color(0.75f, 0.75f, 0.82f));
		y += 18;
		sliderGridColsX = x;
		sliderGridColsY = y;
		sliderGridColsW = w;
		sliderGridColsH = 18;
		drawSlider(sliderGridColsX, sliderGridColsY, sliderGridColsW, sliderGridColsH, ModelCreator.viewportFloorGridCols, gridTilesMin, gridTilesMax);

// --- Show Blocks toggles ---
y += sliderGridColsH + 14;
drawDivider(x, y, w);

// Grid Options label
y += 8;
EnumFonts.BEBAS_NEUE_16.drawString(x, y + 2, "GRID OPTIONS", new Color(0.75f, 0.75f, 0.82f));
y += 22;

btnShowBlocksFloorX = x;
btnShowBlocksFloorY = y;
btnShowBlocksFloorW = w;
btnShowBlocksFloorH = h;
drawToggleButton(btnShowBlocksFloorX, btnShowBlocksFloorY, btnShowBlocksFloorW, btnShowBlocksFloorH,
		ModelCreator.viewportShowBlocksFloor,
		"SHOW BLOCKS - FLOOR");

y += h + 10;
btnShowBlocksAirX = x;
btnShowBlocksAirY = y;
btnShowBlocksAirW = w;
btnShowBlocksAirH = h;
drawToggleButton(btnShowBlocksAirX, btnShowBlocksAirY, btnShowBlocksAirW, btnShowBlocksAirH,
		ModelCreator.viewportShowBlocksAir,
		"SHOW BLOCKS - AIR");

// Air block layer sliders (0..10). Sliders are visually present even when AIR is off.
y += h + 14;
EnumFonts.BEBAS_NEUE_16.drawString(x, y + 2, "AIR BLOCKS ABOVE", new Color(0.75f, 0.75f, 0.82f, ModelCreator.viewportShowBlocksAir ? 1f : 0.5f));
String atxt = String.valueOf(ModelCreator.viewportAirBlocksAbove);
float atw = EnumFonts.BEBAS_NEUE_16.getWidth(atxt);
EnumFonts.BEBAS_NEUE_16.drawString(x + w - (int)atw, y + 2, atxt, new Color(0.75f, 0.75f, 0.82f, ModelCreator.viewportShowBlocksAir ? 1f : 0.5f));
y += 18;
sliderAirAboveX = x;
sliderAirAboveY = y;
sliderAirAboveW = w;
sliderAirAboveH = 18;
drawSliderEnabled(sliderAirAboveX, sliderAirAboveY, sliderAirAboveW, sliderAirAboveH, ModelCreator.viewportAirBlocksAbove, airLayersMin, airLayersMax, ModelCreator.viewportShowBlocksAir);

y += sliderAirAboveH + 18;
EnumFonts.BEBAS_NEUE_16.drawString(x, y + 2, "AIR BLOCKS BELOW", new Color(0.75f, 0.75f, 0.82f, ModelCreator.viewportShowBlocksAir ? 1f : 0.5f));
String btxt = String.valueOf(ModelCreator.viewportAirBlocksBelow);
float btw = EnumFonts.BEBAS_NEUE_16.getWidth(btxt);
EnumFonts.BEBAS_NEUE_16.drawString(x + w - (int)btw, y + 2, btxt, new Color(0.75f, 0.75f, 0.82f, ModelCreator.viewportShowBlocksAir ? 1f : 0.5f));
y += 18;
sliderAirBelowX = x;
sliderAirBelowY = y;
sliderAirBelowW = w;
sliderAirBelowH = 18;
drawSliderEnabled(sliderAirBelowX, sliderAirBelowY, sliderAirBelowW, sliderAirBelowH, ModelCreator.viewportAirBlocksBelow, airLayersMin, airLayersMax, ModelCreator.viewportShowBlocksAir);


		// Compute scroll bounds (content height excluding bottom padding)
		contentHeightPx = y + (sliderAirBelowH > 0 ? sliderAirBelowH : sliderGridColsH) + 18 + scrollOffsetPx;
		int visibleHeight = Math.max(1, canvasHeight - 34);
		int maxScroll = Math.max(0, contentHeightPx - visibleHeight);
		if (scrollOffsetPx > maxScroll) scrollOffsetPx = maxScroll;
		if (scrollOffsetPx < 0) scrollOffsetPx = 0;

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

		// Fill
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

	private void drawInputBox(int x, int y, int w, int h, String valueText)
	{
		boolean texWasEnabled = glIsEnabled(GL_TEXTURE_2D);
		glDisable(GL_TEXTURE_2D);
		TextureImpl.bindNone();
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		// Background
		glColor4f(0f, 0f, 0f, 0.25f);
		glBegin(GL_QUADS);
		{
			glVertex2i(x, y + h);
			glVertex2i(x + w, y + h);
			glVertex2i(x + w, y);
			glVertex2i(x, y);
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

		// Text
		glEnable(GL_TEXTURE_2D);
		float tw = EnumFonts.BEBAS_NEUE_16.getWidth(valueText);
		float th = EnumFonts.BEBAS_NEUE_16.getHeight(valueText);
		int tx = x + 6;
		int ty = y + (int)((h - th) / 2f);
		EnumFonts.BEBAS_NEUE_16.drawString(tx, ty, valueText, new Color(0.95F, 0.95F, 1F, 1F));
		TextureImpl.bindNone();

		if (!texWasEnabled) glDisable(GL_TEXTURE_2D);
	}


	private void drawSlider(int x, int y, int w, int h, int value, int min, int max)
	{
		drawSliderEnabled(x, y, w, h, value, min, max, true);
	}

	private void drawSliderEnabled(int x, int y, int w, int h, int value, int min, int max, boolean enabled)
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
		if (enabled) glColor4f(0.25f, 0.35f, 0.85f, 0.90f);
		else glColor4f(0.55f, 0.55f, 0.60f, 0.50f);
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


	private void drawDivider(int x, int y, int w)
	{
		boolean texWasEnabled = glIsEnabled(GL_TEXTURE_2D);
		glDisable(GL_TEXTURE_2D);
		TextureImpl.bindNone();
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		glColor4f(0.80f, 0.80f, 0.90f, 0.35f);
		glBegin(GL_LINES);
		{
			glVertex2i(x, y);
			glVertex2i(x + w, y);
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

	private void applyGridStepText(String inp)
	{
		if (inp == null) return;
		try {
			double d = Double.parseDouble(inp.trim().replace(',', '.'));
			// Clamp and quantize to quarter-units
			d = Math.max(0.25, Math.min(20.0, d));
			int q = (int)Math.round(d * 4.0);
			q = Math.max(gridStepQMin, Math.min(gridStepQMax, q));
			if (q != ModelCreator.viewportGridSnapStepQ) {
				ModelCreator.viewportGridSnapStepQ = q;
				ModelCreator.prefs.putInt("viewportGridSnapStepQ", q);
			}
		} catch (Exception ignored) { }
	}

private void commitGridStepEdit(boolean cancel)
{
	if (!gridStepEditing) return;
	String txt = gridStepEditBuf == null ? null : gridStepEditBuf.toString();
	gridStepEditing = false;
	if (cancel) return;
	applyGridStepText(txt);
}

	// --- Grid Step preset slider (discrete values)
	// Values are stored in quarter-units (Q): 1 = 0.25, 2 = 0.5, 4 = 1, ...
	private static final int[] GRID_STEP_PRESET_Q = new int[] { 1, 2, 4, 8, 12, 16, 20 }; // .25, .5, 1, 2, 3, 4, 5

	private void drawGridStepPresetSlider(int x, int y, int w, int h)
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

		// Markers + selected knob
		int n = GRID_STEP_PRESET_Q.length;
		if (n >= 2) {
			int knobW = 10;
			int usableW = Math.max(1, w - knobW);
			// pick current index by nearest preset
			int curQ = ModelCreator.viewportGridSnapStepQ;
			int curIdx = 0;
			double best = Double.POSITIVE_INFINITY;
			for (int i = 0; i < n; i++) {
				double d = Math.abs(GRID_STEP_PRESET_Q[i] - curQ);
				if (d < best) { best = d; curIdx = i; }
			}

			// markers
			glColor4f(0.80f, 0.80f, 0.90f, 0.65f);
			glBegin(GL_LINES);
			for (int i = 0; i < n; i++) {
				float t = i / (float)(n - 1);
				int mx = x + (int)(t * usableW) + knobW / 2;
				glVertex2i(mx, y);
				glVertex2i(mx, y + h);
			}
			glEnd();

			// knob
			float t = curIdx / (float)(n - 1);
			int kx = x + (int)(t * usableW);
			glColor4f(0.25f, 0.35f, 0.85f, 0.90f);
			glBegin(GL_QUADS);
			{
				glVertex2i(kx, y + h);
				glVertex2i(kx + knobW, y + h);
				glVertex2i(kx + knobW, y);
				glVertex2i(kx, y);
			}
			glEnd();
		}

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

	private void setGridStepPresetFromMouse(int mx)
	{
		int n = GRID_STEP_PRESET_Q.length;
		if (n <= 0) return;
		int x0 = gridStepSliderX;
		int x1 = gridStepSliderX + gridStepSliderW;
		if (x1 <= x0) return;
		float t = (mx - x0) / (float)(x1 - x0);
		t = Math.max(0f, Math.min(1f, t));
		int idx = Math.round(t * (n - 1));
		idx = Math.max(0, Math.min(n - 1, idx));
		int q = GRID_STEP_PRESET_Q[idx];
		q = Math.max(gridStepQMin, Math.min(gridStepQMax, q));
		if (q != ModelCreator.viewportGridSnapStepQ) {
			ModelCreator.viewportGridSnapStepQ = q;
			ModelCreator.prefs.putInt("viewportGridSnapStepQ", q);
		}
	}

	// Compatibility aliases (older typo'd method names that may exist in some branches)
	private void setGridStepPresetFromMouseUse(int mx) { setGridStepPresetFromMouse(mx); }
	private void setGridStepPresetFromMouseuse(int mx) { setGridStepPresetFromMouse(mx); }




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

	private void setGridRowsFromMouse(int mx)
	{
		int x0 = sliderGridRowsX;
		int x1 = sliderGridRowsX + sliderGridRowsW;
		if (x1 <= x0) return;
		float t = (mx - x0) / (float)(x1 - x0);
		t = Math.max(0f, Math.min(1f, t));
		int val = gridTilesMin + Math.round(t * (gridTilesMax - gridTilesMin));
		val = Math.max(gridTilesMin, Math.min(gridTilesMax, val));
		if (val != ModelCreator.viewportFloorGridRows) {
			ModelCreator.viewportFloorGridRows = val;
			ModelCreator.prefs.putInt("viewportFloorGridRows", val);
		}
	}

	private void setGridColsFromMouse(int mx)
	{
		int x0 = sliderGridColsX;
		int x1 = sliderGridColsX + sliderGridColsW;
		if (x1 <= x0) return;
		float t = (mx - x0) / (float)(x1 - x0);
		t = Math.max(0f, Math.min(1f, t));
		int val = gridTilesMin + Math.round(t * (gridTilesMax - gridTilesMin));
		val = Math.max(gridTilesMin, Math.min(gridTilesMax, val));
		if (val != ModelCreator.viewportFloorGridCols) {
			ModelCreator.viewportFloorGridCols = val;
			ModelCreator.prefs.putInt("viewportFloorGridCols", val);
		}
	}

private void setAirAboveFromMouse(int mx)
{
	int x0 = sliderAirAboveX;
	int x1 = sliderAirAboveX + sliderAirAboveW;
	if (x1 <= x0) return;
	float t = (mx - x0) / (float)(x1 - x0);
	t = Math.max(0f, Math.min(1f, t));
	int val = airLayersMin + Math.round(t * (airLayersMax - airLayersMin));
	val = Math.max(airLayersMin, Math.min(airLayersMax, val));
	if (val != ModelCreator.viewportAirBlocksAbove) {
		ModelCreator.viewportAirBlocksAbove = val;
		ModelCreator.prefs.putInt("viewportAirBlocksAbove", val);
	}
}

private void setAirBelowFromMouse(int mx)
{
	int x0 = sliderAirBelowX;
	int x1 = sliderAirBelowX + sliderAirBelowW;
	if (x1 <= x0) return;
	float t = (mx - x0) / (float)(x1 - x0);
	t = Math.max(0f, Math.min(1f, t));
	int val = airLayersMin + Math.round(t * (airLayersMax - airLayersMin));
	val = Math.max(airLayersMin, Math.min(airLayersMax, val));
	if (val != ModelCreator.viewportAirBlocksBelow) {
		ModelCreator.viewportAirBlocksBelow = val;
		ModelCreator.prefs.putInt("viewportAirBlocksBelow", val);
	}
}

	@Override
	public void onMouseDownOnPanel()
	{
		super.onMouseDownOnPanel();
		if (isResizingSidebar()) return;

		boolean lmb = Mouse.isButtonDown(0);
		// onMouseDownOnPanel() is called continuously while the button is held.
		// Use an edge-trigger so clicking controls only fires once per press.
		boolean lmbEdge = lmb && !lmbWasDown;
		if (lmb) {
			int mx = Mouse.getX();
			int my = lastCanvasHeight - Mouse.getY();

// If we're editing the grid step and click elsewhere, commit the value.
if (lmbEdge && gridStepEditing
		&& !inside(mx, my, gridStepValueX, gridStepValueY, gridStepValueW, gridStepValueH)
		&& !inside(mx, my, gridStepSliderX, gridStepSliderY, gridStepSliderW, gridStepSliderH)) {
	commitGridStepEdit(false);
}


			// Begin slider drag if clicking a slider
			if (!axisSliderDrag && !axisLenSliderDrag && !centerSliderDrag && !gridRowsSliderDrag && !gridColsSliderDrag && !airAboveSliderDrag && !airBelowSliderDrag && inside(mx, my, sliderAxisX, sliderAxisY, sliderAxisW, sliderAxisH)) {
				axisSliderDrag = true;
				setAxisThicknessFromMouse(mx);
			}
				if (!axisSliderDrag && !axisLenSliderDrag && !centerSliderDrag && !gridRowsSliderDrag && !gridColsSliderDrag && !airAboveSliderDrag && !airBelowSliderDrag && inside(mx, my, sliderAxisLenX, sliderAxisLenY, sliderAxisLenW, sliderAxisLenH)) {
				axisLenSliderDrag = true;
				setAxisLengthFromMouse(mx);
			}
				if (!axisSliderDrag && !axisLenSliderDrag && !centerSliderDrag && !gridRowsSliderDrag && !gridColsSliderDrag && !airAboveSliderDrag && !airBelowSliderDrag && inside(mx, my, sliderCenterX, sliderCenterY, sliderCenterW, sliderCenterH)) {
				centerSliderDrag = true;
				setCenterRadiusFromMouse(mx);
			}
				if (!axisSliderDrag && !axisLenSliderDrag && !centerSliderDrag && !gridRowsSliderDrag && !gridColsSliderDrag && !airAboveSliderDrag && !airBelowSliderDrag && inside(mx, my, sliderGridRowsX, sliderGridRowsY, sliderGridRowsW, sliderGridRowsH)) {
				gridRowsSliderDrag = true;
				setGridRowsFromMouse(mx);
			}
				if (!axisSliderDrag && !axisLenSliderDrag && !centerSliderDrag && !gridRowsSliderDrag && !gridColsSliderDrag && !airAboveSliderDrag && !airBelowSliderDrag && inside(mx, my, sliderGridColsX, sliderGridColsY, sliderGridColsW, sliderGridColsH)) {
				gridColsSliderDrag = true;
				setGridColsFromMouse(mx);
			}

if (ModelCreator.viewportShowBlocksAir && !axisSliderDrag && !axisLenSliderDrag && !centerSliderDrag && !gridRowsSliderDrag && !gridColsSliderDrag && !airAboveSliderDrag && !airBelowSliderDrag && inside(mx, my, sliderAirAboveX, sliderAirAboveY, sliderAirAboveW, sliderAirAboveH)) {
	airAboveSliderDrag = true;
	setAirAboveFromMouse(mx);
}
if (ModelCreator.viewportShowBlocksAir && !axisSliderDrag && !axisLenSliderDrag && !centerSliderDrag && !gridRowsSliderDrag && !gridColsSliderDrag && !airAboveSliderDrag && !airBelowSliderDrag && inside(mx, my, sliderAirBelowX, sliderAirBelowY, sliderAirBelowW, sliderAirBelowH)) {
	airBelowSliderDrag = true;
	setAirBelowFromMouse(mx);
}

				// Grid step input box (typed)
				if (lmbEdge && !axisSliderDrag && !axisLenSliderDrag && !centerSliderDrag && !gridRowsSliderDrag && !gridColsSliderDrag && !airAboveSliderDrag && !airBelowSliderDrag && inside(mx, my, gridStepValueX, gridStepValueY, gridStepValueW, gridStepValueH)) {
					gridStepEditing = true;
					gridStepEditBuf.setLength(0);
					double step = ModelCreator.viewportGridSnapStepQ / 4.0;
					String cur = (Math.abs(step - Math.round(step)) < 1e-6) ? ("" + (int)Math.round(step)) : String.format(Locale.ROOT, "%.2f", step);
					gridStepEditBuf.append(cur);
				}

				// Grid step preset slider
				if (lmbEdge && !axisSliderDrag && !axisLenSliderDrag && !centerSliderDrag && !gridRowsSliderDrag && !gridColsSliderDrag && !airAboveSliderDrag && !airBelowSliderDrag && inside(mx, my, gridStepSliderX, gridStepSliderY, gridStepSliderW, gridStepSliderH)) {
					gridStepSliderDrag = true;
					setGridStepPresetFromMouse(mx);
				}

	if (lmbEdge && !axisSliderDrag && !axisLenSliderDrag && !centerSliderDrag && !gridRowsSliderDrag && !gridColsSliderDrag && !airAboveSliderDrag && !airBelowSliderDrag) {
				if (inside(mx, my, btnGizmoModeX, btnGizmoModeY, btnGizmoModeW, btnGizmoModeH)) {
					ModelCreator.toggleViewportGizmoMode();
				}
	if (inside(mx, my, btnScopeX, btnScopeY, btnScopeW, btnScopeH)) {
		ModelCreator.viewportGroupMoveEnabled = !ModelCreator.viewportGroupMoveEnabled;
		ModelCreator.prefs.putBoolean("viewportGroupMoveEnabled", ModelCreator.viewportGroupMoveEnabled);
	}
				if (inside(mx, my, btnMoveControlsX, btnMoveControlsY, btnMoveControlsW, btnMoveControlsH)) {
					moveControlsExpanded = !moveControlsExpanded;
					ModelCreator.prefs.putBoolean("viewportMoveControlsExpanded", moveControlsExpanded);
					axisSliderDrag = false;
					axisLenSliderDrag = false;
						// no grid-step drag
					centerSliderDrag = false;
				}
				if (inside(mx, my, btnGizmoHideX, btnGizmoHideY, btnGizmoHideW, btnGizmoHideH)) {
					ModelCreator.viewportGizmoHidden = !ModelCreator.viewportGizmoHidden;
					ModelCreator.prefs.putBoolean("viewportGizmoHidden", ModelCreator.viewportGizmoHidden);
				}
				if (inside(mx, my, btnGridSnapX, btnGridSnapY, btnGridSnapW, btnGridSnapH)) {
					ModelCreator.viewportGridSnapEnabled = !ModelCreator.viewportGridSnapEnabled;
					ModelCreator.prefs.putBoolean("viewportGridSnapEnabled", ModelCreator.viewportGridSnapEnabled);
				}
				if (inside(mx, my, btnVertexSnapX, btnVertexSnapY, btnVertexSnapW, btnVertexSnapH)) {
					ModelCreator.viewportVertexSnapEnabled = !ModelCreator.viewportVertexSnapEnabled;
					ModelCreator.prefs.putBoolean("viewportVertexSnapEnabled", ModelCreator.viewportVertexSnapEnabled);
				}
	if (inside(mx, my, btnRotateScopeX, btnRotateScopeY, btnRotateScopeW, btnRotateScopeH)) {
		ModelCreator.viewportGroupRotateEnabled = !ModelCreator.viewportGroupRotateEnabled;
		ModelCreator.prefs.putBoolean("viewportGroupRotateEnabled", ModelCreator.viewportGroupRotateEnabled);
	}
	if (inside(mx, my, btnRotateIndX, btnRotateIndY, btnRotateIndW, btnRotateIndH)) {
		ModelCreator.viewportRotateIndividualOriginEnabled = !ModelCreator.viewportRotateIndividualOriginEnabled;
		if (ModelCreator.viewportRotateIndividualOriginEnabled) {
			ModelCreator.viewportRotateGroupPivotEnabled = false;
		}
		if (!ModelCreator.viewportRotateIndividualOriginEnabled && !ModelCreator.viewportRotateGroupPivotEnabled) {
			ModelCreator.viewportRotateIndividualOriginEnabled = true;
		}
		ModelCreator.prefs.putBoolean("viewportRotateIndividualOriginEnabled", ModelCreator.viewportRotateIndividualOriginEnabled);
		ModelCreator.prefs.putBoolean("viewportRotateGroupPivotEnabled", ModelCreator.viewportRotateGroupPivotEnabled);
	}
	if (inside(mx, my, btnRotateGroupPivotX, btnRotateGroupPivotY, btnRotateGroupPivotW, btnRotateGroupPivotH)) {
		ModelCreator.viewportRotateGroupPivotEnabled = !ModelCreator.viewportRotateGroupPivotEnabled;
		if (ModelCreator.viewportRotateGroupPivotEnabled) {
			ModelCreator.viewportRotateIndividualOriginEnabled = false;
		}
		if (!ModelCreator.viewportRotateGroupPivotEnabled && !ModelCreator.viewportRotateIndividualOriginEnabled) {
			ModelCreator.viewportRotateIndividualOriginEnabled = true;
		}
		ModelCreator.prefs.putBoolean("viewportRotateGroupPivotEnabled", ModelCreator.viewportRotateGroupPivotEnabled);
		ModelCreator.prefs.putBoolean("viewportRotateIndividualOriginEnabled", ModelCreator.viewportRotateIndividualOriginEnabled);
	}
	if (inside(mx, my, btnSnapX, btnSnapY, btnSnapW, btnSnapH)) {
		ModelCreator.viewportFloorSnapEnabled = !ModelCreator.viewportFloorSnapEnabled;
		ModelCreator.prefs.putBoolean("viewportFloorSnapEnabled", ModelCreator.viewportFloorSnapEnabled);
	}
	if (inside(mx, my, btnFreedomX, btnFreedomY, btnFreedomW, btnFreedomH)) {
		ModelCreator.viewportFreedomModeEnabled = !ModelCreator.viewportFreedomModeEnabled;
		ModelCreator.prefs.putBoolean("viewportFreedomModeEnabled", ModelCreator.viewportFreedomModeEnabled);
	}
	if (inside(mx, my, btnCenterX, btnCenterY, btnCenterW, btnCenterH)) {
		ModelCreator.viewportCenterCircleEnabled = !ModelCreator.viewportCenterCircleEnabled;
		ModelCreator.prefs.putBoolean("viewportCenterCircleEnabled", ModelCreator.viewportCenterCircleEnabled);
	}
	if (inside(mx, my, btnShowBlocksFloorX, btnShowBlocksFloorY, btnShowBlocksFloorW, btnShowBlocksFloorH)) {
		ModelCreator.viewportShowBlocksFloor = !ModelCreator.viewportShowBlocksFloor;
		ModelCreator.prefs.putBoolean("viewportShowBlocksFloor", ModelCreator.viewportShowBlocksFloor);
	}
	if (inside(mx, my, btnShowBlocksAirX, btnShowBlocksAirY, btnShowBlocksAirW, btnShowBlocksAirH)) {
		ModelCreator.viewportShowBlocksAir = !ModelCreator.viewportShowBlocksAir;
		ModelCreator.prefs.putBoolean("viewportShowBlocksAir", ModelCreator.viewportShowBlocksAir);
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
			if (gridRowsSliderDrag) {
				setGridRowsFromMouse(mx);
			}
			if (gridColsSliderDrag) {
				setGridColsFromMouse(mx);
			}
			if (airAboveSliderDrag) {
				setAirAboveFromMouse(mx);
			}
			if (airBelowSliderDrag) {
				setAirBelowFromMouse(mx);
			}
			if (gridStepSliderDrag) {
				setGridStepPresetFromMouse(mx);
			}
		} else {
			axisSliderDrag = false;
				axisLenSliderDrag = false;
			centerSliderDrag = false;
			gridRowsSliderDrag = false;
			gridColsSliderDrag = false;
			airAboveSliderDrag = false;
			airBelowSliderDrag = false;
			gridStepSliderDrag = false;
		}
				// Track button state for edge-triggered clicks.
				lmbWasDown = lmb;
	}



@Override
public void update(InputKeyEvent event)
{
	if (event == null) return;
	if (!gridStepEditing) return;
	if (!event.pressed()) return;

	int key = event.keyCode();
	char ch = event.keyChar();

	// Commit / cancel
	if (key == org.lwjgl.input.Keyboard.KEY_RETURN || key == org.lwjgl.input.Keyboard.KEY_NUMPADENTER) {
		commitGridStepEdit(false);
		return;
	}
	if (key == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
		commitGridStepEdit(true);
		return;
	}
	if (key == org.lwjgl.input.Keyboard.KEY_BACK) {
		if (gridStepEditBuf.length() > 0) gridStepEditBuf.setLength(gridStepEditBuf.length() - 1);
		return;
	}
	if (key == org.lwjgl.input.Keyboard.KEY_DELETE) {
		gridStepEditBuf.setLength(0);
		return;
	}

	// Accept digits and decimal separators
	if ((ch >= '0' && ch <= '9') || ch == '.' || ch == ',') {
		if (gridStepEditBuf.length() < 12) {
			gridStepEditBuf.append(ch);
		}
	}
}

	@Override
	public void mouseUp()
	{
		super.mouseUp();
		axisSliderDrag = false;
		axisLenSliderDrag = false;
		centerSliderDrag = false;
		gridRowsSliderDrag = false;
		gridColsSliderDrag = false;
			airAboveSliderDrag = false;
			airBelowSliderDrag = false;
		gridStepSliderDrag = false;
		lmbWasDown = false;
	}

	private boolean inside(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx <= x + w && my >= y && my <= y + h;
	}

	@Override
	public boolean handleMouseWheel(int dWheel, int mouseX, int mouseY, int canvasHeight)
	{
		// Only scroll when the mouse is over the sidebar area.
		if (dWheel == 0) return false;
		if (mouseX < 0 || mouseX > GetSidebarWidth()) return false;
		// Don't scroll while dragging a slider.
		if (axisSliderDrag || axisLenSliderDrag || centerSliderDrag || gridRowsSliderDrag || gridColsSliderDrag || airAboveSliderDrag || airBelowSliderDrag || gridStepSliderDrag) return true;

		// Mouse.getDWheel is typically +/-120 per notch.
		int deltaPx = (-dWheel) / 6; // ~20 px per notch
		if (deltaPx == 0) deltaPx = (dWheel < 0 ? 1 : -1);

		scrollOffsetPx += deltaPx;

		int visibleHeight = Math.max(1, canvasHeight - 34);
		int maxScroll = Math.max(0, contentHeightPx - visibleHeight);
		if (scrollOffsetPx > maxScroll) scrollOffsetPx = maxScroll;
		if (scrollOffsetPx < 0) scrollOffsetPx = 0;
		return true;
	}
}
