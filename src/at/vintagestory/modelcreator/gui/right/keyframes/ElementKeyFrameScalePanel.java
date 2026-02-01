package at.vintagestory.modelcreator.gui.right.keyframes;

import java.awt.BorderLayout;
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
import at.vintagestory.modelcreator.model.Animation;
import at.vintagestory.modelcreator.model.AnimationFrame;
import at.vintagestory.modelcreator.model.Element;
import at.vintagestory.modelcreator.model.FocusListenerImpl;
import at.vintagestory.modelcreator.util.AwtUtil;
import at.vintagestory.modelcreator.util.Parser;

/**
 * Keyframe editor for element scale.
 *
 * Internally the model/animation format calls this "stretch" (stretchX/Y/Z),
 * but in the UI it represents per-axis scale factors.
 */
public class ElementKeyFrameScalePanel extends JPanel implements IValueUpdater
{
	private static final long serialVersionUID = 1L;

	private RightKeyFramesPanel keyFramesPanel;

	private JButton btnPlusX;
	private JButton btnPlusY;
	private JButton btnPlusZ;
	private JTextField xScaleField;
	private JTextField yScaleField;
	private JTextField zScaleField;
	private JButton btnNegX;
	private JButton btnNegY;
	private JButton btnNegZ;

	private JButton btnCenterPivot;
	private JPanel gridPanel;

	private DecimalFormat df = new DecimalFormat("#.##");
	/**
	 * A scale of 0 collapses the geometry into a degenerate surface and will
	 * effectively disappear in the renderer. Keep a tiny epsilon away from 0.
	 */
	private static final double MIN_ABS_SCALE = 0.0001;

	private static double clampScale(double value) {
		if (Double.isNaN(value) || Double.isInfinite(value)) return 1;
		if (Math.abs(value) < MIN_ABS_SCALE) {
			// Preserve sign when possible; but 0 has no sign.
			return value < 0 ? -MIN_ABS_SCALE : MIN_ABS_SCALE;
		}
		return value;
	}

	public boolean enabled = true;

	protected boolean xScaleFieldListenEdit;
	protected boolean yScaleFieldListenEdit;
	protected boolean zScaleFieldListenEdit;

	public ElementKeyFrameScalePanel(RightKeyFramesPanel keyFramesPanel)
	{
		this.keyFramesPanel = keyFramesPanel;
		setLayout(new BorderLayout());
		gridPanel = new JPanel(new GridLayout(3, 3, 4, 0));
		setBorder(BorderFactory.createTitledBorder(Start.Border, "<html><b>Scale</b></html>"));
		setMaximumSize(new Dimension(186, 132));
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
		xScaleField = new JTextField();
		yScaleField = new JTextField();
		zScaleField = new JTextField();
		btnNegX = new JButton(Icons.arrow_down_x);
		btnNegY = new JButton(Icons.arrow_down_y);
		btnNegZ = new JButton(Icons.arrow_down_z);
		btnCenterPivot = new JButton("Center pivot");
	}

	private void initProperties()
	{
		Font defaultFont = new Font("SansSerif", Font.BOLD, 20);
		xScaleField.setSize(new Dimension(62, 30));
		xScaleField.setFont(defaultFont);
		xScaleField.setHorizontalAlignment(JTextField.CENTER);

		AwtUtil.addChangeListener(xScaleField, e -> {
			if (!xScaleFieldListenEdit) return;
			keyFramesPanel.ensureAnimationExists();
			AnimFrameElement element = keyFramesPanel.getCurrentElement();
			if (element == null) return;

			String text = xScaleField.getText();
			if (text.length() == 0) return;
			if (!Parser.isDouble(text)) return;
			double newValue = clampScale(Parser.parseDouble(text, 1));
			if (newValue != element.getStretchX()) {
				element.setStretchX(newValue);
				ModelCreator.updateValues(xScaleField);
			}

			keyFramesPanel.copyKeyFrameElemToBackdrop(element.AnimatedElement);
			AnimFrameElement mirrorKf = ModelCreator.SyncMirrorKeyframe(element, null);
			if (mirrorKf != null) keyFramesPanel.copyKeyFrameElemToBackdrop(mirrorKf.AnimatedElement);

		});

		xScaleField.addFocusListener(new FocusListenerImpl() {
			@Override
			public void focusGained(java.awt.event.FocusEvent e) {
				xScaleFieldListenEdit = true;
			}

			@Override
			public void focusLost(java.awt.event.FocusEvent e) {
				xScaleFieldListenEdit = false;
			}
		});

		xScaleField.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				int notches = e.getWheelRotation();
				modifyScale(EnumAxis.X, (notches > 0 ? 1 : -1), e.getModifiers());
			}
		});

		yScaleField.setSize(new Dimension(62, 30));
		yScaleField.setFont(defaultFont);
		yScaleField.setHorizontalAlignment(JTextField.CENTER);

		AwtUtil.addChangeListener(yScaleField, e -> {
			if (!yScaleFieldListenEdit) return;
			keyFramesPanel.ensureAnimationExists();
			AnimFrameElement element = keyFramesPanel.getCurrentElement();
			if (element == null) return;

			String text = yScaleField.getText();
			if (text.length() == 0) return;
			if (!Parser.isDouble(text)) return;
			double newValue = clampScale(Parser.parseDouble(text, 1));
			if (newValue != element.getStretchY()) {
				element.setStretchY(newValue);
				ModelCreator.updateValues(yScaleField);
			}

			keyFramesPanel.copyKeyFrameElemToBackdrop(element.AnimatedElement);
			AnimFrameElement mirrorKf = ModelCreator.SyncMirrorKeyframe(element, null);
			if (mirrorKf != null) keyFramesPanel.copyKeyFrameElemToBackdrop(mirrorKf.AnimatedElement);

		});

		yScaleField.addFocusListener(new FocusListenerImpl() {
			@Override
			public void focusGained(java.awt.event.FocusEvent e) {
				yScaleFieldListenEdit = true;
			}

			@Override
			public void focusLost(java.awt.event.FocusEvent e) {
				yScaleFieldListenEdit = false;
			}
		});

		yScaleField.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				int notches = e.getWheelRotation();
				modifyScale(EnumAxis.Y, (notches > 0 ? 1 : -1), e.getModifiers());
			}
		});

		zScaleField.setSize(new Dimension(62, 30));
		zScaleField.setFont(defaultFont);
		zScaleField.setHorizontalAlignment(JTextField.CENTER);

		AwtUtil.addChangeListener(zScaleField, e -> {
			if (!zScaleFieldListenEdit) return;
			keyFramesPanel.ensureAnimationExists();
			AnimFrameElement element = keyFramesPanel.getCurrentElement();
			if (element == null) return;

			String text = zScaleField.getText();
			if (text.length() == 0) return;
			if (!Parser.isDouble(text)) return;
			double newValue = clampScale(Parser.parseDouble(text, 1));
			if (newValue != element.getStretchZ()) {
				element.setStretchZ(newValue);
				ModelCreator.updateValues(zScaleField);
			}

			keyFramesPanel.copyKeyFrameElemToBackdrop(element.AnimatedElement);
			AnimFrameElement mirrorKf = ModelCreator.SyncMirrorKeyframe(element, null);
			if (mirrorKf != null) keyFramesPanel.copyKeyFrameElemToBackdrop(mirrorKf.AnimatedElement);

		});

		zScaleField.addFocusListener(new FocusListenerImpl() {
			@Override
			public void focusGained(java.awt.event.FocusEvent e) {
				zScaleFieldListenEdit = true;
			}

			@Override
			public void focusLost(java.awt.event.FocusEvent e) {
				zScaleFieldListenEdit = false;
			}
		});

		zScaleField.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				int notches = e.getWheelRotation();
				modifyScale(EnumAxis.Z, (notches > 0 ? 1 : -1), e.getModifiers());
			}
		});

		btnPlusX.addActionListener(e -> modifyScale(EnumAxis.X, 1, e.getModifiers()));
		btnPlusX.setPreferredSize(new Dimension(62, 20));
		btnPlusX.setFont(defaultFont);
		btnPlusX.setToolTipText("<html>Increases the X scale.<br><b>Hold shift for decimals</b></html>");

		btnPlusY.addActionListener(e -> modifyScale(EnumAxis.Y, 1, e.getModifiers()));
		btnPlusY.setPreferredSize(new Dimension(62, 20));
		btnPlusY.setFont(defaultFont);
		btnPlusY.setToolTipText("<html>Increases the Y scale.<br><b>Hold shift for decimals</b></html>");

		btnPlusZ.addActionListener(e -> modifyScale(EnumAxis.Z, 1, e.getModifiers()));
		btnPlusZ.setPreferredSize(new Dimension(62, 20));
		btnPlusZ.setFont(defaultFont);
		btnPlusZ.setToolTipText("<html>Increases the Z scale.<br><b>Hold shift for decimals</b></html>");

		btnNegX.addActionListener(e -> modifyScale(EnumAxis.X, -1, e.getModifiers()));
		btnNegX.setPreferredSize(new Dimension(62, 20));
		btnNegX.setFont(defaultFont);
		btnNegX.setToolTipText("<html>Decreases the X scale.<br><b>Hold shift for decimals</b></html>");

		btnNegY.addActionListener(e -> modifyScale(EnumAxis.Y, -1, e.getModifiers()));
		btnNegY.setPreferredSize(new Dimension(62, 20));
		btnNegY.setFont(defaultFont);
		btnNegY.setToolTipText("<html>Decreases the Y scale.<br><b>Hold shift for decimals</b></html>");

		btnNegZ.addActionListener(e -> modifyScale(EnumAxis.Z, -1, e.getModifiers()));
		btnNegZ.setPreferredSize(new Dimension(62, 20));
		btnNegZ.setFont(defaultFont);
		btnNegZ.setToolTipText("<html>Decreases the Z scale.<br><b>Hold shift for decimals</b></html>");

		btnCenterPivot.setPreferredSize(new Dimension(186, 22));
		btnCenterPivot.setToolTipText("<html>Sets the animation <b>Origin</b> (pivot) to the element's geometric center for this frame.<br>"
				+ "This helps Scale/Rotation happen in-place.<br><b>Note:</b> This creates/enables an Origin keyframe.</html>");
		btnCenterPivot.addActionListener(e -> {
			keyFramesPanel.ensureAnimationExists();
			if (ModelCreator.rightTopPanel == null) return;
			Element elem = ModelCreator.rightTopPanel.getCurrentElement();
			if (elem == null) return;

			ModelCreator.changeHistory.beginMultichangeHistoryState();
			Animation anim = ModelCreator.currentProject.SelectedAnimation;
			int currentFrame = anim.currentFrame;
			AnimationFrame aframe = anim.allFrames.get(currentFrame);
			// Ensure Origin keyframe exists/enabled, then set pivot to element center (absolute).
			AnimFrameElement keyFrameElem = anim.ToggleOrigin(elem, true);

			double cx = elem.getStartX() + elem.getWidth() / 2.0;
			double cy = elem.getStartY() + elem.getHeight() / 2.0;
			double cz = elem.getStartZ() + elem.getDepth() / 2.0;

			keyFrameElem.setOriginX(cx - elem.getOriginX());
			keyFrameElem.setOriginY(cy - elem.getOriginY());
			keyFrameElem.setOriginZ(cz - elem.getOriginZ());

			ModelCreator.updateValues(btnCenterPivot);
			ModelCreator.changeHistory.endMultichangeHistoryState(ModelCreator.currentProject);

			keyFramesPanel.copyKeyFrameElemToBackdrop(elem);
			AnimFrameElement mirrorKf = ModelCreator.SyncMirrorKeyframe(keyFrameElem, null);
			if (mirrorKf != null) keyFramesPanel.copyKeyFrameElemToBackdrop(mirrorKf.AnimatedElement);
		});

	}

	public void modifyScale(EnumAxis axis, int direction, int modifiers)
	{
		keyFramesPanel.ensureAnimationExists();
		AnimFrameElement elem = keyFramesPanel.getCurrentElement();
		if (elem == null) return;

		float delta = direction * ((modifiers & ActionEvent.SHIFT_MASK) == 1 ? 0.1f : 1f);

		switch (axis) {
		case X:
			elem.setStretchX(clampScale(elem.getStretchX() + delta));
			xScaleField.setText(df.format(elem.getStretchX()));
			break;
		case Y:
			elem.setStretchY(clampScale(elem.getStretchY() + delta));
			yScaleField.setText(df.format(elem.getStretchY()));
			break;
		default:
			elem.setStretchZ(clampScale(elem.getStretchZ() + delta));
			zScaleField.setText(df.format(elem.getStretchZ()));
			break;
		}

		ModelCreator.updateValues(null);
		keyFramesPanel.copyKeyFrameElemToBackdrop(elem.AnimatedElement);
		AnimFrameElement mirrorKf = ModelCreator.SyncMirrorKeyframe(elem, null);
		if (mirrorKf != null) keyFramesPanel.copyKeyFrameElemToBackdrop(mirrorKf.AnimatedElement);
	}

	private void addComponents()
	{
		add(gridPanel, BorderLayout.CENTER);
		gridPanel.add(btnPlusX);
		gridPanel.add(btnPlusY);
		gridPanel.add(btnPlusZ);
		gridPanel.add(xScaleField);
		gridPanel.add(yScaleField);
		gridPanel.add(zScaleField);
		gridPanel.add(btnNegX);
		gridPanel.add(btnNegY);
		gridPanel.add(btnNegZ);

		add(btnCenterPivot, BorderLayout.SOUTH);
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
		xScaleField.setEnabled(enabled);
		yScaleField.setEnabled(enabled);
		zScaleField.setEnabled(enabled);
		btnCenterPivot.setEnabled(enabled);
	}

	public void toggleFields(AnimFrameElement cube, JComponent byGuiElem)
	{
		boolean enabled = cube != null && this.enabled;
		setEditable(enabled);

		if (byGuiElem != xScaleField) xScaleField.setText(enabled ? df.format(cube.getStretchX()) : "");
		if (byGuiElem != yScaleField) yScaleField.setText(enabled ? df.format(cube.getStretchY()) : "");
		if (byGuiElem != zScaleField) zScaleField.setText(enabled ? df.format(cube.getStretchZ()) : "");
	}

	// clampScale(...) is defined near the top of this file.
}
