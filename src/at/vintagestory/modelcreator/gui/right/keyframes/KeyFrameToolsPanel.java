package at.vintagestory.modelcreator.gui.right.keyframes;

import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import at.vintagestory.modelcreator.ModelCreator;
import at.vintagestory.modelcreator.Start;
import at.vintagestory.modelcreator.gui.FrameSelectionDialogDuplicateOnlyThis;
import at.vintagestory.modelcreator.gui.Icons;
import at.vintagestory.modelcreator.interfaces.IValueUpdater;
import at.vintagestory.modelcreator.model.Element;

public class KeyFrameToolsPanel extends JPanel implements IValueUpdater
{
    private static final long serialVersionUID = 1L;

    private JButton btnDuplicateOnlyThis;

    public KeyFrameToolsPanel()
    {
        setLayout(new GridLayout(1, 1, 0, 2));
        setBorder(BorderFactory.createTitledBorder(Start.Border, "<html><b>Tools</b></html>"));
        setMaximumSize(new Dimension(186, 60));

        btnDuplicateOnlyThis = new JButton("Duplicate Only This");
        btnDuplicateOnlyThis.setIcon(Icons.copy);
        btnDuplicateOnlyThis.setToolTipText("<html>Duplicates the current frame to a target frame, but only for the selected element.<br>\nIf a keyframe already exists at the target frame, it will merge/overwrite only that element's keyframe data.</html>");
        btnDuplicateOnlyThis.addActionListener(e -> FrameSelectionDialogDuplicateOnlyThis.show(ModelCreator.Instance));
        add(btnDuplicateOnlyThis);

        updateValues(null);
    }

    @Override
    public void updateValues(JComponent byGuiElem)
    {
        Element elem = ModelCreator.currentProject == null ? null : ModelCreator.currentProject.SelectedElement;
        boolean enabled = elem != null && ModelCreator.currentProject != null && ModelCreator.currentProject.SelectedAnimation != null && !ModelCreator.AnimationPlaying();
        btnDuplicateOnlyThis.setEnabled(enabled);
    }
}
