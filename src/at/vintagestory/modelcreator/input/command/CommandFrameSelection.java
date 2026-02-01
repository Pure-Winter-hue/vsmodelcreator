package at.vintagestory.modelcreator.input.command;

import java.awt.Component;
import java.awt.KeyboardFocusManager;

import javax.swing.text.JTextComponent;

import org.lwjgl.input.Keyboard;

import at.vintagestory.modelcreator.ModelCreator;

/**
 * Frames the currently selected element(s) in the 3D viewport.
 * Similar to Unity/Unreal "Frame Selection".
 * Bound to plain 'F'.
 */
public class CommandFrameSelection implements ProjectCommand
{
	private final ModelCreator creator;

	public CommandFrameSelection(ModelCreator creator)
	{
		this.creator = creator;
	}

	@Override
	public void execute()
	{
		// Only react to plain 'F' (no modifiers)
		if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) return;
		if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) return;
		if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) return;

		// If user is typing in a Swing text field, don't hijack the key
		Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		if (focus instanceof JTextComponent) return;

		if (creator != null) {
			creator.frameSelectionInViewport();
		}
	}
}
