package at.vintagestory.modelcreator.gui.right.face;

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
import at.vintagestory.modelcreator.model.Face;

public class FaceToolsPanel extends JPanel implements IValueUpdater
{
	private static final long serialVersionUID = 1L;

	private IElementManager manager;
	private JButton btnFlipUvLR;
	private JButton btnFlipUvUD;

	public FaceToolsPanel(IElementManager manager)
	{
		this.manager = manager;
		setLayout(new GridLayout(2, 1, 0, 2));
		setBorder(BorderFactory.createTitledBorder(Start.Border, "<html><b>Tools</b></html>"));
		// In the UV tab the parent uses a BoxLayout. A plain JPanel has a huge max height, so BoxLayout
		// stretches it to fill the remaining space, turning these two buttons into skyscrapers.
		// Cap the height to something reasonable so buttons stay "normal" sized.
		Dimension pref = new Dimension(10, 95);
		setPreferredSize(pref);
		setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));

		btnFlipUvLR = new JButton("Flip UV (L/R)");
		btnFlipUvLR.setToolTipText("<html>Flips the selected face UV horizontally by swapping U start/end.<br>Also disables Auto UV for this face so the change persists.</html>");
		btnFlipUvLR.addActionListener(e -> flipU(btnFlipUvLR));

		btnFlipUvUD = new JButton("Flip UV (U/D)");
		btnFlipUvUD.setToolTipText("<html>Flips the selected face UV vertically by swapping V start/end.<br>Also disables Auto UV for this face so the change persists.</html>");
		btnFlipUvUD.addActionListener(e -> flipV(btnFlipUvUD));

		add(btnFlipUvLR);
		add(btnFlipUvUD);
	}

	private void flipU(JComponent source)
	{
		Element el = manager.getCurrentElement();
		if (el == null) return;
		Face face = el.getSelectedFace();
		if (face == null) return;

		if (face.isAutoUVEnabled()) face.setAutoUVEnabled(false);

		double su = face.getStartU();
		double eu = face.getEndU();
		face.setStartU(eu);
		face.setEndU(su);

		face.updateUV();
		ModelCreator.updateValues(source);
	}

	private void flipV(JComponent source)
	{
		Element el = manager.getCurrentElement();
		if (el == null) return;
		Face face = el.getSelectedFace();
		if (face == null) return;

		if (face.isAutoUVEnabled()) face.setAutoUVEnabled(false);

		double sv = face.getStartV();
		double ev = face.getEndV();
		face.setStartV(ev);
		face.setEndV(sv);

		face.updateUV();
		ModelCreator.updateValues(source);
	}

	@Override
	public void updateValues(JComponent byGuiElem)
	{
		Element el = manager.getCurrentElement();
		boolean enabled = el != null && el.getSelectedFace() != null;
		btnFlipUvLR.setEnabled(enabled);
		btnFlipUvUD.setEnabled(enabled);
	}
}
