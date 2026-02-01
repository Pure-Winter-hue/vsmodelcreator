package at.vintagestory.modelcreator.gui;

import javax.swing.plaf.basic.BasicTabbedPaneUI;

/**
 * Prevents Swing's WRAP_TAB_LAYOUT from "rotating" tab runs when the selection changes.
 *
 * Default {@link BasicTabbedPaneUI} behavior (when wrapping into multiple rows) moves the
 * selected tab's row to the top, which makes the tab order appear to shuffle.
 */
public class NonRotatingTabbedPaneUI extends BasicTabbedPaneUI {
	@Override
	protected boolean shouldRotateTabRuns(int tabPlacement) {
		return false;
	}
}
