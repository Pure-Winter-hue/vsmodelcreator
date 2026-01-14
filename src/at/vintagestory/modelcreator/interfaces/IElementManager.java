package at.vintagestory.modelcreator.interfaces;

import at.vintagestory.modelcreator.ModelCreator;
import at.vintagestory.modelcreator.model.Element;

import java.util.Collections;
import java.util.List;

public interface IElementManager
{
	ModelCreator getCreator();

	/**
	 * The primary (lead) selected element.
	 *
	 * Older code uses this as the single selected element. With multi-select,
	 * this should return the lead selection.
	 */
	Element getCurrentElement();

	/**
	 * Alias used by some newer panels.
	 */
	default Element getSelectedElement()
	{
		return getCurrentElement();
	}

	/**
	 * Multi-selection API.
	 *
	 * Implementers that don't support multi-select can rely on this default,
	 * which simply returns a singleton list containing the current element.
	 */
	default List<Element> getSelectedElements()
	{
		Element e = getCurrentElement();
		if (e == null) return Collections.emptyList();
		return Collections.singletonList(e);
	}
}
