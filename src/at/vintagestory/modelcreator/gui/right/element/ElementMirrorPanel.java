package at.vintagestory.modelcreator.gui.right.element;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import at.vintagestory.modelcreator.ModelCreator;
import at.vintagestory.modelcreator.Project;
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

	private JLabel lblCenterHeader;
	private JButton btnCenterOnFloor;
	private JButton btnCenterKeepHeight;
	private JButton btnCenterLiteral;

	public ElementMirrorPanel(IElementManager manager)
	{
		this.manager = manager;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createTitledBorder(Start.Border, "<html><b>Tools</b></html>"));
		setMaximumSize(new Dimension(186, 180));

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

		lblCenterHeader = new JLabel("<html><b>Center</b></html>");
		lblCenterHeader.setAlignmentX(LEFT_ALIGNMENT);

		btnCenterOnFloor = new JButton("On Floor");
		btnCenterOnFloor.setToolTipText("<html>Center the selected element(s) on the grid and place the bottom on the floor plane.<br><br>If multiple elements are selected, the tool uses the top hierarchy and moves children along.</html>");
		btnCenterOnFloor.addActionListener(e -> runCenter(Project.CenterMode.ON_FLOOR));

		btnCenterKeepHeight = new JButton("Keep Height");
		btnCenterKeepHeight.setToolTipText("<html>Center the selected element(s) on the grid, but do not change their vertical (Y) position.</html>");
		btnCenterKeepHeight.addActionListener(e -> runCenter(Project.CenterMode.KEEP_HEIGHT));

		btnCenterLiteral = new JButton("Literal");
		btnCenterLiteral.setToolTipText("<html>Center the selected element(s) so the model's CENTER sits on the grid center (Y=0).<br>May place parts below the floor plane.</html>");
		btnCenterLiteral.addActionListener(e -> runCenter(Project.CenterMode.LITERAL));

		add(btnMirror);
		add(chkMirrorAnimations);

		add(Box.createVerticalStrut(6));
		add(lblCenterHeader);
		add(Box.createVerticalStrut(2));
		add(btnCenterOnFloor);
		add(btnCenterKeepHeight);
		add(btnCenterLiteral);
	}

	private void runCenter(Project.CenterMode mode)
	{
		if (ModelCreator.currentProject == null) return;

		ModelCreator.changeHistory.beginMultichangeHistoryState();
		ModelCreator.currentProject.centerSelectedElements(mode);
		ModelCreator.changeHistory.endMultichangeHistoryState(ModelCreator.currentProject);

		ModelCreator.updateValues(btnCenterOnFloor);
	}

	@Override
	public void updateValues(JComponent byGuiElem)
	{
		Element cube = manager.getCurrentElement();
		boolean hasSel = (ModelCreator.currentProject != null && ModelCreator.currentProject.SelectedElements != null && ModelCreator.currentProject.SelectedElements.size() > 0);
		btnMirror.setEnabled(cube != null);
		chkMirrorAnimations.setEnabled(cube != null);

		btnCenterOnFloor.setEnabled(hasSel);
		btnCenterKeepHeight.setEnabled(hasSel);
		btnCenterLiteral.setEnabled(hasSel);
	}
}
