package at.vintagestory.modelcreator.gui.right;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.*;

import org.lwjgl.input.Mouse;

import at.vintagestory.modelcreator.ModelCreator;
import at.vintagestory.modelcreator.gui.CuboidTabbedPane;
import at.vintagestory.modelcreator.gui.Icons;
import at.vintagestory.modelcreator.gui.right.attachmentpoints.AttachmentPointsPanel;
import at.vintagestory.modelcreator.gui.right.element.ElementPanel;
import at.vintagestory.modelcreator.gui.right.face.FacePanel;
import at.vintagestory.modelcreator.gui.right.keyframes.RightKeyFramesPanel;
import at.vintagestory.modelcreator.interfaces.IElementManager;
import at.vintagestory.modelcreator.interfaces.IValueUpdater;
import at.vintagestory.modelcreator.model.Element;

public class RightPanel extends JPanel implements IElementManager, IValueUpdater {
	private static final long serialVersionUID = 1L;

	private ModelCreator creator;
	
	// Swing Variables
	private SpringLayout layout;
	public JScrollPane scrollPane;
	private JPanel btnContainer;
	private JButton btnAdd;
	private JButton btnRemove;
	private JButton btnDuplicate;
	private JTextField nameField;
	private CuboidTabbedPane tabbedPane;
	
	RightKeyFramesPanel rightKeyFramesPanel;

	public ElementTree tree = new ElementTree();
	
	public int dy = 60;
	
	public RightPanel(ModelCreator creator)
	{
		this.creator = creator;
		int width = ModelCreator.prefs.getInt("rightBarWidth", 215);
		setPreferredSize(new Dimension(width, 1450));
		initComponents();
	}

	public void initComponents()
	{
		removeAll();
		
		btnAdd = new JButton();
		btnRemove = new JButton();
		btnDuplicate = new JButton();
		nameField = new JTextField();
		tabbedPane = new CuboidTabbedPane(this);

		ModelCreator.currentProject.tree = tree;
		
		add(tree.jtree);
		scrollPane = new JScrollPane(tree.jtree);
		int width = ModelCreator.prefs.getInt("rightBarWidth", 215);
		scrollPane.setPreferredSize(new Dimension(width-10, ModelCreator.elementTreeHeight + dy));
		add(scrollPane);
		initElementTreeResize();

		
		Font defaultFont = new Font("SansSerif", Font.BOLD, 14);
		btnContainer = new JPanel(new GridLayout(1, 3, 4, 0));
		btnContainer.setPreferredSize(new Dimension(205, 30));

		btnAdd.setIcon(Icons.cube);
		btnAdd.setToolTipText("New Element");
		btnAdd.addActionListener(e -> { 
			Element elem = new Element(1,1,1);
			
			if (ModelCreator.currentProject.TexturesByCode.size() > 0) {
				elem.setTextureCode(ModelCreator.currentProject.TexturesByCode.values().iterator().next().code, false);
			}
			
			ModelCreator.currentProject.addElementAsChild(elem); 
		});
		btnAdd.setPreferredSize(new Dimension(30, 30));
		btnContainer.add(btnAdd);

		btnRemove.setIcon(Icons.bin);
		btnRemove.setToolTipText("Remove Element");
		btnRemove.addActionListener(e -> { ModelCreator.currentProject.removeCurrentElement(); });
		btnRemove.setPreferredSize(new Dimension(30, 30));
		btnContainer.add(btnRemove);

		btnDuplicate.setIcon(Icons.copy);
		btnDuplicate.setToolTipText("Duplicate Element");
		btnDuplicate.addActionListener(e -> { ModelCreator.currentProject.duplicateCurrentElement(); });
		btnDuplicate.setFont(defaultFont);
		btnDuplicate.setPreferredSize(new Dimension(30, 30));
		btnContainer.add(btnDuplicate);
		add(btnContainer);

		nameField.setPreferredSize(new Dimension(205, 25));
		nameField.setToolTipText("Element Name");
		nameField.setEnabled(false);

		nameField.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyReleased(KeyEvent e)
			{
				Element elem = tree.getSelectedElement();
				if (elem != null) {
					if (ModelCreator.currentProject.IsElementNameUsed(nameField.getText(), elem)) {
						nameField.setBackground(new Color(50, 0, 0));
					} else {
						elem.setName(nameField.getText().replaceAll(",", ""));
						nameField.setBackground(getBackground());
					}
				}
				
				tree.updateUI();
			}
		});
		add(nameField);
		



		tabbedPane.add("Cube", new ElementPanel(this));
		tabbedPane.add("Face", new FacePanel(this));
		tabbedPane.add("Keyframe", rightKeyFramesPanel = new RightKeyFramesPanel());
		tabbedPane.add("P", new AttachmentPointsPanel());
		
		tabbedPane.setPreferredSize(new Dimension(205, 1150));
		tabbedPane.setTabPlacement(JTabbedPane.TOP);
		
		tabbedPane.addChangeListener(c ->
		{
			ModelCreator.currentRightTab = tabbedPane.getSelectedIndex();
			
			if (tabbedPane.getSelectedIndex() == 1)
			{
				creator.setSidebar(creator.uvSidebar);
				
			} else {
				creator.setSidebar(null);
			}
			
			boolean keyframeMode = tabbedPane.getSelectedIndex() == 2;
			ModelCreator.leftKeyframesPanel.setVisible(keyframeMode);
			if (ModelCreator.leftKeyframesScroll != null) ModelCreator.leftKeyframesScroll.setVisible(keyframeMode);
			if (keyframeMode) {
				ModelCreator.leftKeyframesPanel.Load();
			}
			// Force a relayout, otherwise the left pane sometimes only appears after a manual window resize
			creator.revalidate();
			creator.repaint();
			
			ModelCreator.renderAttachmentPoints = tabbedPane.getSelectedIndex() == 3;
			ModelCreator.guiMain.itemSaveGifAnimation.setEnabled(tabbedPane.getSelectedIndex() == 2 && ModelCreator.currentProject != null && ModelCreator.currentProject.SelectedAnimation != null);
			ModelCreator.guiMain.itemSavePngAnimation.setEnabled(tabbedPane.getSelectedIndex() == 2 && ModelCreator.currentProject != null && ModelCreator.currentProject.SelectedAnimation != null);
			
			ModelCreator.ignoreValueUpdates = true;
			updateValues(tabbedPane);
			ModelCreator.ignoreValueUpdates = false;
		});
		
		add(tabbedPane);
		setLayout(dy);
		revalidate();
	}

	public void setLayout(int dy)
	{
		layout = new SpringLayout();
		
		int my = ModelCreator.elementTreeHeight - 240;
		
		layout.putConstraint(SpringLayout.NORTH, nameField, 212 + 70 + dy + my, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.NORTH, btnContainer, 176 + 70 + dy + my, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.NORTH, tabbedPane, 250 + 70 + dy + my, SpringLayout.NORTH, this);
		setLayout(layout);
	}

	@Override
	public Element getCurrentElement()
	{
		return ModelCreator.currentProject.SelectedElement;
	}

	
	public java.util.List<Element> getSelectedElements()
	{
		return tree.getSelectedElements();
	}

public ModelCreator getCreator()
	{
		return creator;
	}

	@Override
	public void updateValues(JComponent byGuiElem)
	{
		tabbedPane.updateValues(byGuiElem);
		
		Element cube = getCurrentElement();
		if (cube != null)
		{
			nameField.setText(cube.getName());
			if (ModelCreator.currentProject.IsElementNameUsed(nameField.getText(), cube)) {
				nameField.setBackground(new Color(50, 0, 0));
			} else {
				nameField.setBackground(getBackground());
			}
		}
		
		nameField.setEnabled(cube != null);
		btnRemove.setEnabled(cube != null);
		btnDuplicate.setEnabled(cube != null);
		
		ModelCreator.guiMain.itemSaveGifAnimation.setEnabled(tabbedPane.getSelectedIndex() == 2 && ModelCreator.currentProject != null && ModelCreator.currentProject.SelectedAnimation != null);
		ModelCreator.guiMain.itemSavePngAnimation.setEnabled(tabbedPane.getSelectedIndex() == 2 && ModelCreator.currentProject != null && ModelCreator.currentProject.SelectedAnimation != null);
		
	}
	
	
	public void updateFrame(JComponent byGuiElem) {
		rightKeyFramesPanel.updateFrame(byGuiElem);
	}

	private void initElementTreeResize()
	{
		// A small draggable "grip" in the lower-right corner of the element tree scrollpane
		// (the gray square between scrollbars). Dragging resizes the element tree height.
		JPanel corner = new JPanel();
		corner.setOpaque(false);
		corner.setPreferredSize(new Dimension(16, 16));
		elementTreeResizeCorner = corner;
		scrollPane.setCorner(JScrollPane.LOWER_RIGHT_CORNER, corner);

		MouseAdapter mouseAdapter = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (!SwingUtilities.isLeftMouseButton(e)) return;
				if (!isOverElementTreeResizeGrip(e)) return;
				nowResizingElementTree = true;
				elementTreeGrabMouseY = e.getYOnScreen();
				elementTreeGrabHeight = ModelCreator.elementTreeHeight;
				updateElementTreeResizeCursor(e, true);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				nowResizingElementTree = false;
				updateElementTreeResizeCursor(e, false);
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				if (!nowResizingElementTree) return;
				int delta = e.getYOnScreen() - elementTreeGrabMouseY;
				int newHeight = elementTreeGrabHeight + delta;
				setElementTreeHeight(newHeight);
				updateElementTreeResizeCursor(e, true);
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				updateElementTreeResizeCursor(e, false);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				if (!nowResizingElementTree) {
					if (overElementTreeResizeGrip) {
						setElementTreeResizeCursor(Cursor.getDefaultCursor());
						overElementTreeResizeGrip = false;
					}
				}
			}
		};

		// Attach to multiple components so the grip works even when hovering over the content area.
		scrollPane.addMouseListener(mouseAdapter);
		scrollPane.addMouseMotionListener(mouseAdapter);
		scrollPane.getViewport().addMouseListener(mouseAdapter);
		scrollPane.getViewport().addMouseMotionListener(mouseAdapter);
		tree.jtree.addMouseListener(mouseAdapter);
		tree.jtree.addMouseMotionListener(mouseAdapter);
		corner.addMouseListener(mouseAdapter);
		corner.addMouseMotionListener(mouseAdapter);
	}

	private boolean isOverElementTreeResizeGrip(MouseEvent e)
	{
		if (elementTreeResizeCorner != null && e.getComponent() == elementTreeResizeCorner) return true;
		if (scrollPane == null) return false;
		Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), scrollPane);
		int grip = 6;
		return p.y >= scrollPane.getHeight() - grip;
	}

	private void updateElementTreeResizeCursor(MouseEvent e, boolean dragging)
	{
		boolean overGrip = dragging || isOverElementTreeResizeGrip(e);
		if (overGrip) {
			int cursor = (elementTreeResizeCorner != null && e.getComponent() == elementTreeResizeCorner) ? Cursor.SE_RESIZE_CURSOR : Cursor.S_RESIZE_CURSOR;
			setElementTreeResizeCursor(Cursor.getPredefinedCursor(cursor));
			overElementTreeResizeGrip = true;
		} else if (overElementTreeResizeGrip) {
			setElementTreeResizeCursor(Cursor.getDefaultCursor());
			overElementTreeResizeGrip = false;
		}
	}

	private void setElementTreeResizeCursor(Cursor cursor)
	{
		// Set it on the subtree so it feels consistent while moving between inner components.
		if (scrollPane != null) scrollPane.setCursor(cursor);
		if (scrollPane != null && scrollPane.getViewport() != null) scrollPane.getViewport().setCursor(cursor);
		if (tree != null && tree.jtree != null) tree.jtree.setCursor(cursor);
		if (elementTreeResizeCorner != null) elementTreeResizeCorner.setCursor(cursor);
	}

	public void setElementTreeHeight(int newHeight)
	{
		// Keep it within a reasonable range to avoid UI breakage.
		newHeight = Math.max(140, Math.min(1100, newHeight));
		if (newHeight == ModelCreator.elementTreeHeight) return;
		ModelCreator.elementTreeHeight = newHeight;
		ModelCreator.prefs.putInt("elementTreeHeight", newHeight);

		SwingUtilities.invokeLater(() -> {
			int width = ModelCreator.prefs.getInt("rightBarWidth", 215);
			scrollPane.setPreferredSize(new Dimension(width - 10, ModelCreator.elementTreeHeight + dy));
			setLayout(dy);
			revalidate();
			repaint();
			ModelCreator.Instance.revalidate();
		});
	}
	
	
	

	boolean nowResizingSidebar;
	int lastGrabMouseX;
	boolean overSidebar;

	// Element tree height resizing (drag bottom edge or corner between scrollbars)
	private boolean nowResizingElementTree;
	private int elementTreeGrabMouseY;
	private int elementTreeGrabHeight;
	private boolean overElementTreeResizeGrip;
	private JComponent elementTreeResizeCorner;

	public void onMouseDownOnRightPanel()
	{
		int width = getSize().width;
		int nowMouseX = MouseInfo.getPointerInfo().getLocation().x - ModelCreator.Instance.getX();
		int edgeX = -42 + ModelCreator.Instance.getRootPane().getWidth() - scrollPane.getWidth(); // ModelCreator.Instance.leftSidebarWidth() + 2 + ModelCreator.canvas.getWidth();
		
		if (Math.abs(edgeX - nowMouseX) < 8) {
			if (Mouse.isButtonDown(0)) {
				if (!nowResizingSidebar) {
					lastGrabMouseX = nowMouseX; 
				}
				
				nowResizingSidebar = true;
			}
			
			overSidebar = true;
		}
		
		if (nowResizingSidebar) {
			final int newwidth = Math.min(600, Math.max(215, width + (lastGrabMouseX - nowMouseX)));
			setSidebarWidth(newwidth);
			
			lastGrabMouseX = nowMouseX;
		}	
	}

	public void setSidebarWidth(int newwidth) {
		final int prevheight = getSize().height;
	
		ModelCreator.prefs.putInt("rightBarWidth", newwidth);
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				setPreferredSize(new Dimension(newwidth, prevheight));
				int my = ModelCreator.elementTreeHeight;
				scrollPane.setPreferredSize(new Dimension(newwidth - 10, my + dy));
				
				invalidate();
				ModelCreator.Instance.revalidate();
			}
		});		
	}
	
	public void Draw()
	{
		PointerInfo pinfo = MouseInfo.getPointerInfo(); 

		if (!Mouse.isButtonDown(0)) {
			nowResizingSidebar=false;
		}
		
		if (pinfo != null) {
		
			int nowMouseX = pinfo.getLocation().x - ModelCreator.Instance.getX();
			int edgeX = -42 + ModelCreator.Instance.getRootPane().getWidth() - scrollPane.getWidth(); // ModelCreator.Instance.leftSidebarWidth() + 2 + ModelCreator.canvas.getWidth();
			
			//System.out.println(edgeX + " vs. " + nowMouseX);
			
			if (Math.abs(edgeX - nowMouseX) < 8) {
				ModelCreator.Instance.isOnRightPanel=true;
				ModelCreator.canvas.setCursor(new java.awt.Cursor(Cursor.E_RESIZE_CURSOR));
				overSidebar = true;
			} else {
				ModelCreator.Instance.isOnRightPanel=false;
				if (overSidebar) {
					ModelCreator.canvas.setCursor(java.awt.Cursor.getDefaultCursor());				
					overSidebar = false;
				}
			}
		}
	}
}
