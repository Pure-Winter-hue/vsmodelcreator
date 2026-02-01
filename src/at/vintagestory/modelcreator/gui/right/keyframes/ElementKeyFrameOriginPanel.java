package at.vintagestory.modelcreator.gui.right.keyframes;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import at.vintagestory.modelcreator.ModelCreator;
import at.vintagestory.modelcreator.Start;
import at.vintagestory.modelcreator.enums.EnumAxis;
import at.vintagestory.modelcreator.gui.Icons;
import at.vintagestory.modelcreator.interfaces.IValueUpdater;
import at.vintagestory.modelcreator.model.AnimFrameElement;
import at.vintagestory.modelcreator.model.FocusListenerImpl;
import at.vintagestory.modelcreator.util.AwtUtil;
import at.vintagestory.modelcreator.util.Parser;

/**
 * Keyframe editor for element origin offsets.
 *
 * The UI displays the absolute origin (pivot) in model coordinates.
 * Internally keyframes store a delta added to the element's base origin.
 */
public class ElementKeyFrameOriginPanel extends JPanel implements IValueUpdater
{
	private static final long serialVersionUID = 1L;

	private RightKeyFramesPanel keyFramesPanel;

	private JButton btnPlusX;
	private JButton btnPlusY;
	private JButton btnPlusZ;
	private JTextField xOriginField;
	private JTextField yOriginField;
	private JTextField zOriginField;
	private JButton btnNegX;
	private JButton btnNegY;
	private JButton btnNegZ;

	private DecimalFormat df = new DecimalFormat("#.##");

	public boolean enabled = true;

	protected boolean xOriginFieldListenEdit;
	protected boolean yOriginFieldListenEdit;
	protected boolean zOriginFieldListenEdit;

	public ElementKeyFrameOriginPanel(RightKeyFramesPanel keyFramesPanel)
	{
		this.keyFramesPanel = keyFramesPanel;
		setLayout(new GridLayout(3, 3, 4, 0));
		setBorder(BorderFactory.createTitledBorder(Start.Border, "<html><b>Origin</b></html>"));
		setMaximumSize(new Dimension(186, 104));
		setAlignmentX(JPanel.CENTER_ALIGNMENT);
		initComponents();
		initProperties();
		addComponents();
	}

	private void initComponents()
	{
		btnPlusX = new JButton(Icons.arrow_up_x);
		btnPlusY = new JButton(Icons.arrow_up_y);
		btnPlusZ = new JButton(Icons.arrow_up_z);
		xOriginField = new JTextField();
		yOriginField = new JTextField();
		zOriginField = new JTextField();
		btnNegX = new JButton(Icons.arrow_down_x);
		btnNegY = new JButton(Icons.arrow_down_y);
		btnNegZ = new JButton(Icons.arrow_down_z);
	}

	private void initProperties()
	{
		Font defaultFont = new Font("SansSerif", Font.BOLD, 20);

		xOriginField.setSize(new Dimension(62, 30));
		xOriginField.setFont(defaultFont);
		xOriginField.setHorizontalAlignment(JTextField.CENTER);
		AwtUtil.addChangeListener(xOriginField, e -> {
			if (!xOriginFieldListenEdit) return;
			keyFramesPanel.ensureAnimationExists();
			AnimFrameElement element = keyFramesPanel.getCurrentElement();
			if (element == null) return;

			String text = xOriginField.getText();
			if (text.length() == 0) return;
			if (!Parser.isDouble(text)) return;
			double newValue = Parser.parseDouble(text, 0);
			if (newValue != element.getOriginX()) {
				element.setOriginX(newValue);
				ModelCreator.updateValues(xOriginField);
			}
			keyFramesPanel.copyKeyFrameElemToBackdrop(element.AnimatedElement);
			AnimFrameElement mirrorKf = ModelCreator.SyncMirrorKeyframe(element, null);
			if (mirrorKf != null) keyFramesPanel.copyKeyFrameElemToBackdrop(mirrorKf.AnimatedElement);

		});
		xOriginField.addFocusListener(new FocusListenerImpl() {
			@Override
			public void focusGained(java.awt.event.FocusEvent e) {
				xOriginFieldListenEdit = true;
			}
			@Override
			public void focusLost(java.awt.event.FocusEvent e) {
				xOriginFieldListenEdit = false;
			}
		});
		xOriginField.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				modifyOrigin(EnumAxis.X, (e.getWheelRotation() > 0 ? 1 : -1), e.getModifiers());
			}
		});

		yOriginField.setSize(new Dimension(62, 30));
		yOriginField.setFont(defaultFont);
		yOriginField.setHorizontalAlignment(JTextField.CENTER);
		AwtUtil.addChangeListener(yOriginField, e -> {
			if (!yOriginFieldListenEdit) return;
			keyFramesPanel.ensureAnimationExists();
			AnimFrameElement element = keyFramesPanel.getCurrentElement();
			if (element == null) return;

			String text = yOriginField.getText();
			if (text.length() == 0) return;
			if (!Parser.isDouble(text)) return;
			double newValue = Parser.parseDouble(text, 0);
			if (newValue != element.getOriginY()) {
				element.setOriginY(newValue);
				ModelCreator.updateValues(yOriginField);
			}
			keyFramesPanel.copyKeyFrameElemToBackdrop(element.AnimatedElement);
			AnimFrameElement mirrorKf = ModelCreator.SyncMirrorKeyframe(element, null);
			if (mirrorKf != null) keyFramesPanel.copyKeyFrameElemToBackdrop(mirrorKf.AnimatedElement);

		});
		yOriginField.addFocusListener(new FocusListenerImpl() {
			@Override
			public void focusGained(java.awt.event.FocusEvent e) {
				yOriginFieldListenEdit = true;
			}
			@Override
			public void focusLost(java.awt.event.FocusEvent e) {
				yOriginFieldListenEdit = false;
			}
		});
		yOriginField.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				modifyOrigin(EnumAxis.Y, (e.getWheelRotation() > 0 ? 1 : -1), e.getModifiers());
			}
		});

		zOriginField.setSize(new Dimension(62, 30));
		zOriginField.setFont(defaultFont);
		zOriginField.setHorizontalAlignment(JTextField.CENTER);
		AwtUtil.addChangeListener(zOriginField, e -> {
			if (!zOriginFieldListenEdit) return;
			keyFramesPanel.ensureAnimationExists();
			AnimFrameElement element = keyFramesPanel.getCurrentElement();
			if (element == null) return;

			String text = zOriginField.getText();
			if (text.length() == 0) return;
			if (!Parser.isDouble(text)) return;
			double newValue = Parser.parseDouble(text, 0);
			if (newValue != element.getOriginZ()) {
				element.setOriginZ(newValue);
				ModelCreator.updateValues(zOriginField);
			}
			keyFramesPanel.copyKeyFrameElemToBackdrop(element.AnimatedElement);
			AnimFrameElement mirrorKf = ModelCreator.SyncMirrorKeyframe(element, null);
			if (mirrorKf != null) keyFramesPanel.copyKeyFrameElemToBackdrop(mirrorKf.AnimatedElement);

		});
		zOriginField.addFocusListener(new FocusListenerImpl() {
			@Override
			public void focusGained(java.awt.event.FocusEvent e) {
				zOriginFieldListenEdit = true;
			}
			@Override
			public void focusLost(java.awt.event.FocusEvent e) {
				zOriginFieldListenEdit = false;
			}
		});
		zOriginField.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				modifyOrigin(EnumAxis.Z, (e.getWheelRotation() > 0 ? 1 : -1), e.getModifiers());
			}
		});

		btnPlusX.addActionListener(e -> modifyOrigin(EnumAxis.X, 1, e.getModifiers()));
		btnPlusX.setPreferredSize(new Dimension(62, 20));
		btnPlusX.setToolTipText("<html>Increase the X origin offset.<br><b>Hold shift for decimals</b></html>");

		btnPlusY.addActionListener(e -> modifyOrigin(EnumAxis.Y, 1, e.getModifiers()));
		btnPlusY.setPreferredSize(new Dimension(62, 20));
		btnPlusY.setToolTipText("<html>Increase the Y origin offset.<br><b>Hold shift for decimals</b></html>");

		btnPlusZ.addActionListener(e -> modifyOrigin(EnumAxis.Z, 1, e.getModifiers()));
		btnPlusZ.setPreferredSize(new Dimension(62, 20));
		btnPlusZ.setToolTipText("<html>Increase the Z origin offset.<br><b>Hold shift for decimals</b></html>");

		btnNegX.addActionListener(e -> modifyOrigin(EnumAxis.X, -1, e.getModifiers()));
		btnNegX.setPreferredSize(new Dimension(62, 20));
		btnNegX.setToolTipText("<html>Decrease the X origin offset.<br><b>Hold shift for decimals</b></html>");

		btnNegY.addActionListener(e -> modifyOrigin(EnumAxis.Y, -1, e.getModifiers()));
		btnNegY.setPreferredSize(new Dimension(62, 20));
		btnNegY.setToolTipText("<html>Decrease the Y origin offset.<br><b>Hold shift for decimals</b></html>");

		btnNegZ.addActionListener(e -> modifyOrigin(EnumAxis.Z, -1, e.getModifiers()));
		btnNegZ.setPreferredSize(new Dimension(62, 20));
		btnNegZ.setToolTipText("<html>Decrease the Z origin offset.<br><b>Hold shift for decimals</b></html>");
	}

	private void modifyOrigin(EnumAxis axis, int direction, int modifiers)
	{
		keyFramesPanel.ensureAnimationExists();
		AnimFrameElement elem = keyFramesPanel.getCurrentElement();
		if (elem == null) return;

		float delta = direction * ((modifiers & ActionEvent.SHIFT_MASK) == 1 ? 0.1f : 1f);

		// AnimatedElement is an Element; its origin fields are protected, so use accessors.
		double baseX = elem.AnimatedElement.getOriginX();
		double baseY = elem.AnimatedElement.getOriginY();
		double baseZ = elem.AnimatedElement.getOriginZ();

		switch (axis) {
		case X: {
			double abs = baseX + elem.getOriginX() + delta;
			elem.setOriginX(abs - baseX);
			xOriginField.setText(df.format(abs));
			break;
		}
		case Y: {
			double abs = baseY + elem.getOriginY() + delta;
			elem.setOriginY(abs - baseY);
			yOriginField.setText(df.format(abs));
			break;
		}
		default: {
			double abs = baseZ + elem.getOriginZ() + delta;
			elem.setOriginZ(abs - baseZ);
			zOriginField.setText(df.format(abs));
			break;
		}
		}
		ModelCreator.updateValues(null);
		keyFramesPanel.copyKeyFrameElemToBackdrop(elem.AnimatedElement);
	}

	private void addComponents()
	{
		add(btnPlusX);
		add(btnPlusY);
		add(btnPlusZ);
		add(xOriginField);
		add(yOriginField);
		add(zOriginField);
		add(btnNegX);
		add(btnNegY);
		add(btnNegZ);
	}

	@Override
	public void updateValues(JComponent byGuiElem)
	{
		AnimFrameElement cube = keyFramesPanel.getCurrentElement();
		toggleFields(cube, byGuiElem);
	}

	public void setAnimActive(boolean active)
	{
		AnimFrameElement cube = keyFramesPanel.getCurrentElement();
		boolean enabled = cube != null && this.enabled;
		setEditable(!active && enabled);
	}

	protected void setEditable(boolean enabled)
	{
		btnPlusX.setEnabled(enabled);
		btnPlusY.setEnabled(enabled);
		btnPlusZ.setEnabled(enabled);
		btnNegX.setEnabled(enabled);
		btnNegY.setEnabled(enabled);
		btnNegZ.setEnabled(enabled);
		xOriginField.setEnabled(enabled);
		yOriginField.setEnabled(enabled);
		zOriginField.setEnabled(enabled);
	}

	public void toggleFields(AnimFrameElement cube, JComponent byGuiElem)
	{
		boolean enabled = cube != null && this.enabled;
		setEditable(enabled);

		if (enabled) {
			double absX = cube.AnimatedElement.getOriginX() + cube.getOriginX();
			double absY = cube.AnimatedElement.getOriginY() + cube.getOriginY();
			double absZ = cube.AnimatedElement.getOriginZ() + cube.getOriginZ();
			if (byGuiElem != xOriginField) xOriginField.setText(df.format(absX));
			if (byGuiElem != yOriginField) yOriginField.setText(df.format(absY));
			if (byGuiElem != zOriginField) zOriginField.setText(df.format(absZ));
		} else {
			if (byGuiElem != xOriginField) xOriginField.setText("");
			if (byGuiElem != yOriginField) yOriginField.setText("");
			if (byGuiElem != zOriginField) zOriginField.setText("");
		}
		}
}
