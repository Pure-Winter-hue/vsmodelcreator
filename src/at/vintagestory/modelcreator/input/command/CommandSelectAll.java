package at.vintagestory.modelcreator.input.command;

import java.awt.Component;
import java.awt.KeyboardFocusManager;

import javax.swing.text.JTextComponent;

import org.lwjgl.input.Keyboard;

import at.vintagestory.modelcreator.ModelCreator;
import at.vintagestory.modelcreator.model.Element;

/**
 * Select all elements.
 *
 * 'A' key (no modifiers) now selects all, in Cube (modeling) and Keyframe (animation) modes.
 * 
 */
public class CommandSelectAll implements ProjectCommand
{
	@Override
	public void execute()
	{
		// Only react to plain 'A' (no modifiers)
		if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) return;
		if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) return;
		if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) return;

		// If user is typing in a Swing text field, don't hijack the key
		Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		if (focus instanceof JTextComponent) return;

		// Only in Cube and Keyframe tabs
		if (ModelCreator.currentRightTab != 0 && ModelCreator.currentRightTab != 2) return;
		if (ModelCreator.currentProject == null || ModelCreator.currentProject.tree == null) return;

		// Select all tree elements and update the project's selection state
		ModelCreator.ignoreValueUpdates = true;
		ModelCreator.currentProject.tree.selectAllElements();
		ModelCreator.ignoreValueUpdates = false;

		Element lead = ModelCreator.currentProject.tree.getSelectedElement();
		ModelCreator.currentProject.SelectedElement = lead;
		ModelCreator.currentProject.SelectedElements = new java.util.ArrayList<Element>(ModelCreator.currentProject.tree.getSelectedElements());
		if (lead != null) lead.elementWasSelected();
		ModelCreator.updateValues(ModelCreator.currentProject.tree.jtree);
	}
}
