package at.vintagestory.modelcreator.gui.right.element;

import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import at.vintagestory.modelcreator.ModelCreator;
import at.vintagestory.modelcreator.interfaces.IElementManager;
import at.vintagestory.modelcreator.interfaces.IValueUpdater;

public class ElementMirrorModePanel extends JPanel implements IValueUpdater
{
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private IElementManager manager;

    private JCheckBox mirrorMode;

    public ElementMirrorModePanel(IElementManager manager)
    {
        this.manager = manager;

        setLayout(new GridLayout(2, 1));
        setMaximumSize(new Dimension(186, 44));
        setAlignmentX(JPanel.CENTER_ALIGNMENT);

        mirrorMode = new JCheckBox();
        mirrorMode.setToolTipText("<html>When enabled: elements named identically except for L/R are treated as partners.<br>Edits on one side are mirrored onto the other.</html>");
        mirrorMode.addChangeListener(e -> {
            ModelCreator.mirrorMode = mirrorMode.isSelected();
        });

        add(new JLabel("Mirror Mode"));
        add(mirrorMode);
    }

    @Override
    public void updateValues(JComponent byGuiElem)
    {
        mirrorMode.setSelected(ModelCreator.mirrorMode);
    }
}
