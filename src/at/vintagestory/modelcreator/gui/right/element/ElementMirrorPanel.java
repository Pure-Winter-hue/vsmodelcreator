
package at.vintagestory.modelcreator.gui.right.element;

import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
	private JCheckBox chkMirrorAnimations;

    public ElementMirrorPanel(IElementManager manager)
    {
        this.manager = manager;
        setLayout(new GridLayout(2, 1, 0, 0));
        setBorder(BorderFactory.createTitledBorder(Start.Border, "<html><b>Tools</b></html>"));
        setMaximumSize(new Dimension(186, 92));

		btnMirror = new JButton("Mirror (L/R)");
		btnMirror.setToolTipText("<html>Duplicate the selected element(s) mirrored to the opposite side (left/right).<br>Also mirrors UVs.<br><br>Note: In Entity Texture Mode, left/right corresponds to the Z axis (because entities face +X/-X).</html>");
		btnMirror.addActionListener(e -> {
            if (ModelCreator.currentProject != null) {
				ModelCreator.currentProject.mirrorSelectedElementsX(chkMirrorAnimations.isSelected());
				ModelCreator.updateValues(btnMirror);
            }
        });

		chkMirrorAnimations = new JCheckBox("Mirror animations");
		chkMirrorAnimations.setToolTipText("<html>If enabled, mirrors keyframe data for the mirrored elements as well.<br>Position/rotation/origin offsets are reflected to match the mirrored side.</html>");
		chkMirrorAnimations.setOpaque(false);
		chkMirrorAnimations.setSelected(ModelCreator.prefs.getBoolean("mirrorAnimations", false));
		chkMirrorAnimations.addActionListener(e -> {
			ModelCreator.prefs.putBoolean("mirrorAnimations", chkMirrorAnimations.isSelected());
		});

		add(btnMirror);
		add(chkMirrorAnimations);
    }

    @Override
    public void updateValues(JComponent byGuiElem)
    {
        Element cube = manager.getCurrentElement();
		btnMirror.setEnabled(cube != null);
    }
}
