package at.vintagestory.modelcreator.gui.right.keyframes;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import at.vintagestory.modelcreator.ModelCreator;
import at.vintagestory.modelcreator.Start;
import at.vintagestory.modelcreator.gui.FrameSelectionDialogDuplicateOnlyThis;
import at.vintagestory.modelcreator.gui.Icons;
import at.vintagestory.modelcreator.interfaces.IValueUpdater;
import at.vintagestory.modelcreator.model.AnimFrameElement;
import at.vintagestory.modelcreator.model.Animation;
import at.vintagestory.modelcreator.model.Element;

public class KeyFrameToolsPanel extends JPanel implements IValueUpdater
{
    private static final long serialVersionUID = 1L;

    private JButton btnDuplicateOnlyThis;
    private JButton btnRotShortestX;
    private JButton btnRotShortestY;
    private JButton btnRotShortestZ;

    public KeyFrameToolsPanel()
    {
        setLayout(new GridLayout(2, 1, 0, 2));
        setBorder(BorderFactory.createTitledBorder(Start.Border, "<html><b>Tools</b></html>"));
        // Let this panel expand to the available sidebar width.
        // The old fixed max width (186px) could cause the "Shortest X/Y/Z" buttons to clip.
        setMinimumSize(new Dimension(0, 90));
        setPreferredSize(new Dimension(0, 90));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        btnDuplicateOnlyThis = new JButton("Duplicate Only This");
        btnDuplicateOnlyThis.setIcon(Icons.copy);
        btnDuplicateOnlyThis.setToolTipText("<html>Duplicates the current frame to a target frame, but only for the selected element.<br>\nIf a keyframe already exists at the target frame, it will merge/overwrite only that element's keyframe data.</html>");
        btnDuplicateOnlyThis.addActionListener(e -> FrameSelectionDialogDuplicateOnlyThis.show(ModelCreator.Instance));
        add(btnDuplicateOnlyThis);


        JPanel rotShortestPanel = new JPanel(new GridLayout(1, 3, 2, 0));

        btnRotShortestX = new JButton("Rot X");
        btnRotShortestX.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btnRotShortestX.setMargin(new Insets(2, 4, 2, 4));
        btnRotShortestX.setToolTipText("<html>Adds <b>\"rotShortestDistanceX\": true</b> to this keyframe element when exporting.<br>Useful for preventing long spins when interpolating rotations.</html>");
        btnRotShortestX.addActionListener(e -> addRotShortestDistance('X', btnRotShortestX));

        btnRotShortestY = new JButton("Rot Y");
        btnRotShortestY.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btnRotShortestY.setMargin(new Insets(2, 4, 2, 4));
        btnRotShortestY.setToolTipText("<html>Adds <b>\"rotShortestDistanceY\": true</b> to this keyframe element when exporting.<br>Useful for preventing long spins when interpolating rotations.</html>");
        btnRotShortestY.addActionListener(e -> addRotShortestDistance('Y', btnRotShortestY));

        btnRotShortestZ = new JButton("Rot Z");
        btnRotShortestZ.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btnRotShortestZ.setMargin(new Insets(2, 4, 2, 4));
        btnRotShortestZ.setToolTipText("<html>Adds <b>\"rotShortestDistanceZ\": true</b> to this keyframe element when exporting.<br>Useful for preventing long spins when interpolating rotations.</html>");
        btnRotShortestZ.addActionListener(e -> addRotShortestDistance('Z', btnRotShortestZ));

        rotShortestPanel.add(btnRotShortestX);
        rotShortestPanel.add(btnRotShortestY);
        rotShortestPanel.add(btnRotShortestZ);
        add(rotShortestPanel);

        updateValues(null);
    }

    private AnimFrameElement getSelectedKeyFrameElem()
    {
        if (ModelCreator.currentProject == null) return null;
        if (ModelCreator.currentProject.SelectedAnimation == null) return null;
        if (ModelCreator.rightTopPanel == null) return null;

        Element elem = ModelCreator.rightTopPanel.getCurrentElement();
        if (elem == null) return null;

        Animation anim = ModelCreator.currentProject.SelectedAnimation;
        return anim.GetKeyFrameElement(elem, anim.currentFrame);
    }

    private void addRotShortestDistance(char axis, JComponent source)
    {
        AnimFrameElement kfe = getSelectedKeyFrameElem();
        if (kfe == null) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        if (axis == 'X') kfe.RotShortestDistanceX = true;
        if (axis == 'Y') kfe.RotShortestDistanceY = true;
        if (axis == 'Z') kfe.RotShortestDistanceZ = true;

        ModelCreator.DidModify();
        ModelCreator.updateValues(source);
    }

    @Override
    public void updateValues(JComponent byGuiElem)
    {
        Element elem = ModelCreator.rightTopPanel == null ? null : ModelCreator.rightTopPanel.getCurrentElement();
        boolean enabledBase = elem != null && ModelCreator.currentProject != null && ModelCreator.currentProject.SelectedAnimation != null && !ModelCreator.AnimationPlaying();

        btnDuplicateOnlyThis.setEnabled(enabledBase);

        // Only enable when a keyframe element exists at the current frame.
        AnimFrameElement kfe = enabledBase ? getSelectedKeyFrameElem() : null;
        boolean enabledRotShortest = enabledBase && kfe != null;
        btnRotShortestX.setEnabled(enabledRotShortest);
        btnRotShortestY.setEnabled(enabledRotShortest);
        btnRotShortestZ.setEnabled(enabledRotShortest);
    }
}
