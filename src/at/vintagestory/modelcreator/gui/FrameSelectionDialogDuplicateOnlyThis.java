package at.vintagestory.modelcreator.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import at.vintagestory.modelcreator.ModelCreator;
import at.vintagestory.modelcreator.Project;
import at.vintagestory.modelcreator.model.AnimFrameElement;
import at.vintagestory.modelcreator.model.Animation;
import at.vintagestory.modelcreator.model.AnimationFrame;
import at.vintagestory.modelcreator.model.Element;

/**
 * Like {@link FrameSelectionDialog}, but only duplicates the selected element's
 * keyframe data. If the target frame already has a keyframe, it will update or
 * add the selected element there instead of aborting.
 */
public class FrameSelectionDialogDuplicateOnlyThis
{
	public static void show(JFrame parent)
	{
		JDialog dialog = new JDialog(parent, "Duplicate Selected Element to Target Frame", false);
		
		JPanel container = new JPanel(new BorderLayout(20, 10));
		container.setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel panelRow1 = new JPanel(new GridLayout(5, 1, 5, 0));
		JLabel label = new JLabel("For Target frame");
		label.setPreferredSize(new Dimension(200, 20));
		panelRow1.add(label);
				
		JTextField frameTextFiel = new JTextField();
		frameTextFiel.setPreferredSize(new Dimension(50, 20));

		if (ModelCreator.currentProject != null && ModelCreator.currentProject.SelectedAnimation != null) {
			if (ModelCreator.currentProject.SelectedAnimation.IsCurrentFrameKeyFrame()) {
				frameTextFiel.setText("" + (ModelCreator.currentProject.SelectedAnimation.currentFrame + 1));
			} else {
				frameTextFiel.setText("" + ModelCreator.currentProject.SelectedAnimation.currentFrame);
			}
		}
			
		panelRow1.add(frameTextFiel);
		container.add(panelRow1, BorderLayout.CENTER);

		JPanel panelRow2 = new JPanel(new GridLayout(1, 2, 15, 0));
		
		JButton btnCancel = new JButton("Cancel");
		btnCancel.setIcon(Icons.clear);
		btnCancel.addActionListener(a -> dialog.dispose());
		panelRow2.add(btnCancel);

		JButton btnDuplicate = new JButton("Duplicate");
		btnDuplicate.setIcon(Icons.copy);
		btnDuplicate.addActionListener(a ->
		{
			int forFrame;
			try {
				forFrame = Integer.parseInt(frameTextFiel.getText());
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(null, "Invalid frame number");
				return;
			}
			
			if (forFrame < 0) {
				JOptionPane.showMessageDialog(null, "Must be a positive frame number");
				return;
			}
			
			Project project = ModelCreator.currentProject;
			if (project == null || project.SelectedAnimation == null) {
				JOptionPane.showMessageDialog(null, "No animation selected");
				return;
			}
			
			Element selectedElem = project.SelectedElement;
			if (selectedElem == null) {
				JOptionPane.showMessageDialog(null, "No element selected");
				return;
			}

			Animation anim = project.SelectedAnimation;
			
			// Ensure we have up-to-date interpolated frames to copy from
			if (anim.framesDirty || anim.allFrames.size() == 0) {
				anim.calculateAllFrames(project);
			}
			
			int curFrame = anim.currentFrame;
			if (curFrame < 0 || curFrame >= anim.allFrames.size()) {
				JOptionPane.showMessageDialog(null, "Current frame is out of bounds");
				return;
			}
			
			AnimationFrame sourceFrame = anim.allFrames.get(curFrame);
			AnimFrameElement sourceElem = sourceFrame.GetAnimFrameElementRec(selectedElem);
			if (sourceElem == null) {
				JOptionPane.showMessageDialog(null, "Selected element not found in current frame");
				return;
			}
			
			// Create or update only the selected element in the target keyframe
			AnimFrameElement targetElem = anim.GetOrCreateKeyFrameElement(selectedElem, forFrame);
			targetElem.setFrom(sourceElem);
			
			ModelCreator.DidModify();
			ModelCreator.updateValues(btnDuplicate);
			dialog.dispose();
		});
		
		panelRow2.add(btnDuplicate);
		container.add(panelRow2, BorderLayout.SOUTH);

		dialog.setResizable(false);
		dialog.add(container);
		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
		dialog.requestFocusInWindow();
	}
}
