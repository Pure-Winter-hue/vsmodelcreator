package at.vintagestory.modelcreator.gui;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;

import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicTabbedPaneUI;


public class FixedTwoRowTabbedPaneUI extends BasicTabbedPaneUI {
	@Override
	protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex,
			String title, Rectangle textRect, boolean isSelected) {
		int oldY = textRect.y;
		textRect.y = oldY - 1; 
		super.paintText(g, tabPlacement, font, metrics, tabIndex, title, textRect, isSelected);
		textRect.y = oldY;
	}

	@Override
	protected LayoutManager createLayoutManager() {
		return new FixedTwoRowTabbedPaneLayout();
	}

	@Override
	protected boolean shouldRotateTabRuns(int tabPlacement) {
		return false;
	}

	@Override
	protected int calculateTabAreaHeight(int tabPlacement, int horizRunCount, int maxTabHeight) {
		int tabCount = (tabPane != null) ? tabPane.getTabCount() : 0;
		int rows = tabCount <= 2 ? 1 : (tabCount + 1) / 2;
		Insets insets = getTabAreaInsets(tabPlacement);
		return rows * maxTabHeight + insets.top + insets.bottom;
	}

	/**
	 * Custom layout to force a fixed 2-column grid of tabs:
	 * Top row: Modeling, UVs
	 * Bottom row: Animation, Attachment Points
	 */
	protected class FixedTwoRowTabbedPaneLayout extends TabbedPaneLayout {
		@Override
		protected void calculateTabRects(int tabPlacement, int tabCount) {
			if (tabPlacement != JTabbedPane.TOP || tabCount == 0) {
				super.calculateTabRects(tabPlacement, tabCount);
				return;
			}

			if (rects == null || rects.length < tabCount) {
				rects = new Rectangle[tabCount];
				for (int i = 0; i < tabCount; i++) rects[i] = new Rectangle();
			} else {
				for (int i = 0; i < tabCount; i++) {
					if (rects[i] == null) rects[i] = new Rectangle();
				}
			}

			Insets tabAreaInsets = getTabAreaInsets(tabPlacement);
			int availW = tabPane.getWidth() - tabAreaInsets.left - tabAreaInsets.right;
			if (availW < 0) availW = 0;

			int tabH = calculateMaxTabHeight(tabPlacement);
			// Force exactly 2 tabs per row, splitting the full available width.
			int tabW = Math.max(1, availW / 2);

			for (int i = 0; i < tabCount; i++) {
				int row = i / 2;
				int col = i % 2;
				Rectangle r = rects[i];
				r.x = tabAreaInsets.left + (col * tabW);
				r.y = tabAreaInsets.top + (row * tabH);
				// Give the last column any remaining pixels so there's no gap.
				r.width = (col == 1) ? (availW - tabW) : tabW;
				r.height = tabH;
			}

			// Define runs so the UI knows there are multiple rows (and doesn't try to rotate them).
			runCount = (tabCount <= 2) ? 1 : (tabCount + 1) / 2;
			if (tabRuns == null || tabRuns.length < runCount) {
				tabRuns = new int[runCount];
			}
			for (int r = 0; r < runCount; r++) {
				tabRuns[r] = r * 2;
			}
			selectedRun = getRunForTab(tabCount, tabPane.getSelectedIndex());

			// Keep these sane for any painting logic that references them.
			maxTabWidth = tabW;
			maxTabHeight = tabH;
		}
	}
}
