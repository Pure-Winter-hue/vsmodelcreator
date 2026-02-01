package at.vintagestory.modelcreator.input.command;

import java.awt.Component;
import java.awt.KeyboardFocusManager;

import javax.swing.text.JTextComponent;

import org.lwjgl.input.Keyboard;

import at.vintagestory.modelcreator.ModelCreator;

/**
 * Toggle viewport transform gizmo between Move and Rotate.
 * Bound to plain 'Q' by default.
 */
public class CommandToggleViewportGizmoMode implements ProjectCommand
{
	@Override
	public void execute()
	{
		// Only react to plain 'Q' (no modifiers)
		if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) return;
		if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) return;
		if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) return;

		// If user is typing in a Swing text field, don't hijack the key
		Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		if (focus instanceof JTextComponent) return;

		ModelCreator.toggleViewportGizmoMode();
	}
}
