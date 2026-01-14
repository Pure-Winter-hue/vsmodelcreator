
package at.vintagestory.modelcreator.gui.right.element;

import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import at.vintagestory.modelcreator.ModelCreator;
import at.vintagestory.modelcreator.Start;
import at.vintagestory.modelcreator.interfaces.IElementManager;
import at.vintagestory.modelcreator.interfaces.IValueUpdater;
import at.vintagestory.modelcreator.model.Element;

public class ElementMirrorPanel extends JPanel implements IValueUpdater
{
    private static final long serialVersionUID = 1L;

    private IElementManager manager;
	private JButton btnMirror;

    public ElementMirrorPanel(IElementManager manager)
    {
        this.manager = manager;
        setLayout(new GridLayout(1, 1, 0, 0));
        setBorder(BorderFactory.createTitledBorder(Start.Border, "<html><b>Tools</b></html>"));
        setMaximumSize(new Dimension(186, 62));

		btnMirror = new JButton("Mirror (L/R)");
		btnMirror.setToolTipText("<html>Duplicate the selected element(s) mirrored to the opposite side (left/right).<br>Also mirrors UVs.<br><br>Note: In Entity Texture Mode, left/right corresponds to the Z axis (because entities face +X/-X).</html>");
		btnMirror.addActionListener(e -> {
            if (ModelCreator.currentProject != null) {
                ModelCreator.currentProject.mirrorSelectedElementsX();
				ModelCreator.updateValues(btnMirror);
            }
        });

		add(btnMirror);
    }

    @Override
    public void updateValues(JComponent byGuiElem)
    {
        Element cube = manager.getCurrentElement();
		btnMirror.setEnabled(cube != null);
    }
}
