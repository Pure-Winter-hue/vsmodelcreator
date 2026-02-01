package at.vintagestory.modelcreator.gui.right;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.swing.DropMode;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import at.vintagestory.modelcreator.ModelCreator;
import at.vintagestory.modelcreator.model.Element;

public class ElementTree
{
	public JTree jtree;
	public DefaultMutableTreeNode rootNode;
	public DefaultTreeModel treeModel;
	private Toolkit toolkit = Toolkit.getDefaultToolkit();
	
	public HashSet<String> collapsedPaths = new HashSet<String>();
	
	boolean ignoreExpandCollapse;
	
	public void loadCollapsedPaths(String values) {
		String[] parts = values.split(",");
		collapsedPaths.clear();
		for (int i = 0; i < parts.length; i++) {
			collapsedPaths.add(parts[i]);
		}
	}
	
	public String saveCollapsedPaths() {
		String p = "";
		int i=0;
		for (String path : collapsedPaths) {
			if (i > 0) p+=",";
			p += path;
			i++;
		}
		
		return p;
	}
		
	public ElementTree() {
		rootNode = new DefaultMutableTreeNode("Root");
		treeModel = new DefaultTreeModel(rootNode);
        jtree = new FixedJTree(treeModel);
        jtree.setEditable(false);
        jtree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        jtree.setShowsRootHandles(true);
        jtree.setFocusable(true);
        jtree.setCellRenderer(new ElementTreeCellRenderer());
        jtree.setDragEnabled(true);
        jtree.setDropMode(DropMode.ON_OR_INSERT);
        
		jtree.addTreeSelectionListener(new TreeSelectionListener()
		{
			@Override
			public void valueChanged(TreeSelectionEvent e)
			{
				
				if (!ModelCreator.ignoreValueUpdates) {
					ModelCreator.currentProject.SelectedElement = getSelectedElement();
					ModelCreator.currentProject.SelectedElements = new java.util.ArrayList<Element>(getSelectedElements());
					if (ModelCreator.currentProject.SelectedElement != null) {
						ModelCreator.currentProject.SelectedElement.elementWasSelected();

						// Auto-fix Z-fighting on selection (runs once per selection change, not per frame)
						if (ModelCreator.autofixZFighting && ModelCreator.currentRightTab == 0) {
							// Commit so users can see the transform update immediately
							ModelCreator.currentProject.autoFixZFighting(
								java.util.Arrays.asList(ModelCreator.currentProject.SelectedElement),
								ModelCreator.autofixZFightingEpsilon,
								true
							);
						}
					}
					ModelCreator.updateValues(jtree);
					}
			}	
		});
		
		// does not work, wtf? <- I don't know who left this comment, but I'm scared to remove it, Load bearing comment! -PW
		jtree.addKeyListener(new KeyListener()
		{
			
			@Override
			public void keyTyped(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_UP) {
					jtree.setSelectionRow(Math.max(0, jtree.getSelectionRows()[0] - 1));
				}
				if (e.getKeyCode() == KeyEvent.VK_DOWN) {
					jtree.setSelectionRow(Math.max(0, jtree.getSelectionRows()[0] + 1));
				}
				if (e.getKeyCode() == KeyEvent.VK_LEFT) {
					DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)jtree.getLastSelectedPathComponent();
					jtree.collapsePath(new TreePath(selectedNode.getPath()));
				}
				if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
					DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)jtree.getLastSelectedPathComponent();
					jtree.expandPath(new TreePath(selectedNode.getPath()));
				}
				
			}
			
			@Override
			public void keyReleased(KeyEvent e)
			{
				// TODO Auto-generated method stub | PW: <_< 
				
			}
			
			@Override
			public void keyPressed(KeyEvent e)
			{
				
				
				
			}
		});
		
		jtree.addTreeExpansionListener(new TreeExpansionListener()
		{
			
			@Override
			public void treeExpanded(TreeExpansionEvent arg0)
			{
				if (ignoreExpandCollapse) return;
				TreePath path = arg0.getPath();
				collapsedPaths.remove(path.toString().substring(1).replace("]", "").replace(", ", "/"));
				
				ModelCreator.currentProject.collapsedPaths = saveCollapsedPaths();
			}
			
			@Override
			public void treeCollapsed(TreeExpansionEvent arg0)
			{
				if (ignoreExpandCollapse) return;
				
				TreePath path = arg0.getPath();
				String strpath=path.toString().substring(1).replace("]", "").replace(", ", "/");
		        collapsedPaths.add(strpath);
		        
		        ModelCreator.currentProject.collapsedPaths = saveCollapsedPaths();
			}
		});
		
		jtree.addMouseListener(new MouseListener()
		{
			
			@Override
			public void mouseReleased(MouseEvent arg0)
			{
				// TODO Auto-generated method stub PW: <_< 
				
			}
			
			@Override
			public void mousePressed(MouseEvent arg0)
			{
				TreePath path = jtree.getPathForLocation(arg0.getX(), arg0.getY());
				TreePath pathLeft = jtree.getPathForLocation(arg0.getX() - 17, arg0.getY());

				// Shift-click toggles selection (multi select)
				if (path != null && pathLeft != null && (arg0.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
					if (jtree.isPathSelected(path)) {
						jtree.removeSelectionPath(path);
						// If we removed the lead selection, move the lead to another selected path (if any)
						TreePath lead = jtree.getLeadSelectionPath();
						if (lead != null && lead.equals(path)) {
							TreePath[] remaining = jtree.getSelectionPaths();
							if (remaining != null && remaining.length > 0) {
								jtree.setLeadSelectionPath(remaining[remaining.length - 1]);
							}
						}
					} else {
						jtree.addSelectionPath(path);
						jtree.setLeadSelectionPath(path);
					}
					arg0.consume();
					return;
				}

				// Click on the visibility icon (left of the row) toggles renderInEditor
				if (path != null && pathLeft == null) {
					Object userObj = ((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
					if (userObj instanceof Element) {
						((Element)userObj).setRenderInEditor(!((Element)userObj).getRenderInEditor());
						jtree.updateUI();
						arg0.consume();
					}
				}
			}

			@Override
			public void mouseExited(MouseEvent arg0)
			{
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseEntered(MouseEvent arg0)
			{
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseClicked(MouseEvent arg0)
			{
				// TODO Auto-generated method stub
				
			}
		});
		
		jtree.setTransferHandler(new TreeTransferHandler(this));
		
	}
	

	public void clearElements()
	{
		rootNode.removeAllChildren();
		jtree.removeAll();
		jtree.updateUI();
		jtree.clearSelection();
		collapsedPaths.clear();
	}

	
	public Element getSelectedElement() {
		TreePath currentSelection = jtree.getLeadSelectionPath();
		if (currentSelection == null) currentSelection = jtree.getSelectionPath();
        if (currentSelection != null) {
        	Object userObj = ((DefaultMutableTreeNode)currentSelection.getLastPathComponent()).getUserObject(); 
        	if (userObj instanceof Element) {
        		return (Element)userObj;
        	}
        }
        return null;
	}

	public java.util.List<Element> getSelectedElements()
	{
		java.util.ArrayList<Element> elems = new java.util.ArrayList<Element>();
		TreePath[] paths = jtree.getSelectionPaths();
		if (paths == null) return elems;
		for (TreePath p : paths) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)p.getLastPathComponent();
			Object obj = node.getUserObject();
			if (obj instanceof Element) elems.add((Element)obj);
		}
		return elems;
	}

	
	public Element getNextSelectedElement() {
		TreePath currentSelection = jtree.getLeadSelectionPath();
		if (currentSelection == null) currentSelection = jtree.getSelectionPath();
		if (currentSelection != null) {
        	DefaultMutableTreeNode node = ((DefaultMutableTreeNode)currentSelection.getLastPathComponent());
        	if (node.getNextSibling() != null) { 
        		return (Element)node.getNextSibling().getUserObject();
        	}
        	if (node.getPreviousSibling() != null) { 
        		return (Element)node.getPreviousSibling().getUserObject();
        	}
        }
		
		return null;
	}
	
	
	public void selectElement(Element elem) {
		jtree.clearSelection();
		
		Enumeration<TreeNode> enumer = rootNode.breadthFirstEnumeration();
		while (enumer.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)enumer.nextElement();
			if (node.getUserObject().equals(elem)) {
				jtree.setSelectionPath(new TreePath(node.getPath()));
				jtree.scrollPathToVisible(new TreePath(node.getPath()));
				break;
			}
		}
	}

	/**
	 * Select all element nodes (including those inside collapsed branches).
	 *
	 * Note: JTree selection operates on TreePaths, so we walk the full tree model
	 * rather than using row indices (which would only include visible rows).
	 */
	public void selectAllElements()
	{
		// Preserve previous lead selection when possible
		TreePath prevLead = jtree.getLeadSelectionPath();
		Object prevLeadObj = null;
		if (prevLead != null && prevLead.getLastPathComponent() instanceof DefaultMutableTreeNode) {
			prevLeadObj = ((DefaultMutableTreeNode)prevLead.getLastPathComponent()).getUserObject();
		}

		jtree.clearSelection();

		TreePath firstPath = null;
		TreePath leadPath = null;

		@SuppressWarnings("unchecked")
		Enumeration<TreeNode> enumer = rootNode.breadthFirstEnumeration();
		while (enumer.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)enumer.nextElement();
			Object obj = node.getUserObject();
			if (!(obj instanceof Element)) continue;

			TreePath path = new TreePath(node.getPath());
			jtree.addSelectionPath(path);
			if (firstPath == null) firstPath = path;
			if (prevLeadObj != null && prevLeadObj.equals(obj)) {
				leadPath = path;
			}
		}

		if (leadPath == null) leadPath = firstPath;
		if (leadPath != null) {
			jtree.setLeadSelectionPath(leadPath);
			jtree.scrollPathToVisible(leadPath);
		}
	}

	/**
	 * Sets the lead/primary selection to the given element without clearing existing selection.
	 * Useful for viewport multi-select workflows where you want to transform a group but
	 * still change which element is considered the "active" one.
	 */
	public void setLeadSelectionElement(Element elem, boolean ensureSelected)
	{
		if (elem == null) return;
		Enumeration<TreeNode> enumer = rootNode.breadthFirstEnumeration();
		while (enumer.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)enumer.nextElement();
			if (node.getUserObject().equals(elem)) {
				TreePath path = new TreePath(node.getPath());
				if (ensureSelected && !jtree.isPathSelected(path)) {
					jtree.addSelectionPath(path);
				}
				jtree.setLeadSelectionPath(path);
				jtree.scrollPathToVisible(path);
				break;
			}
		}
	}
	
	public void selectElementByOpenGLName(int opengglname) {
		jtree.clearSelection();
		
		Enumeration<TreeNode> enumer = rootNode.breadthFirstEnumeration();
		
		while (enumer.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)enumer.nextElement();
			
			if (node.getUserObject() instanceof Element) {
				Element elem = (Element)node.getUserObject();
				for (int i = 0; i < elem.getAllFaces().length; i++) {
					if (elem.getAllFaces()[i].openGlName == opengglname) {
						jtree.setSelectionPath(new TreePath(node.getPath()));
						return;
					}
				}
			}
		}
	}
	
	
	
	/**
	 * Add the element matching one of the given OpenGL face ids to the current selection.
	 * Used for Shift + click and Shift + drag marquee selection in the 3D viewport.
	 */
	public void addElementSelectionByOpenGLName(int opengglname)
	{
		TreePath path = findElementPathByOpenGLName(opengglname);
		if (path != null) {
			jtree.addSelectionPath(path);
			jtree.scrollPathToVisible(path);
		}
	}

	/**
	 * Add all elements whose faces were hit by OpenGL selection in the given name set.
	 */
	public void addElementsSelectionByOpenGLNames(Set<Integer> openglNames)
	{
		if (openglNames == null || openglNames.isEmpty()) return;
		for (Integer name : openglNames) {
			if (name == null) continue;
			TreePath path = findElementPathByOpenGLName(name);
			if (path != null) {
				jtree.addSelectionPath(path);
			}
		}
	}

	private TreePath findElementPathByOpenGLName(int opengglname)
	{
		Enumeration<TreeNode> enumer = rootNode.breadthFirstEnumeration();
		while (enumer.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)enumer.nextElement();
			if (node.getUserObject() instanceof Element) {
				Element elem = (Element)node.getUserObject();
				for (int i = 0; i < elem.getAllFaces().length; i++) {
					if (elem.getAllFaces()[i].openGlName == opengglname) {
						return new TreePath(node.getPath());
					}
				}
			}
		}
		return null;
	}

    /** Remove the currently selected node. */
    public boolean removeCurrentElement() {
        TreePath currentSelection = jtree.getLeadSelectionPath();
		if (currentSelection == null) currentSelection = jtree.getSelectionPath();
        if (currentSelection != null) {
            DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)(currentSelection.getLastPathComponent());
            
            MutableTreeNode parent = (MutableTreeNode)(currentNode.getParent());
            
            if (parent != null) {
                treeModel.removeNodeFromParent(currentNode);
                
                Element elem = (Element)currentNode.getUserObject();
                if (elem.ParentElement != null) {
                	elem.ParentElement.ChildElements.remove(elem);
                	elem.ParentElement = null;	
                }
                
                return true;
            }
            return true;
        } 

        // Either there was no selection, or the root was selected.
        toolkit.beep();
        
        return false;
    }

    public DefaultMutableTreeNode addElementAsSibling(Element child) {
        DefaultMutableTreeNode parentNode = null;
        
        TreePath parentPath = jtree.getSelectionPath();

        if (parentPath == null) {
            parentNode = rootNode;
        } else {
            parentNode = (DefaultMutableTreeNode) ((DefaultMutableTreeNode)(parentPath.getLastPathComponent())).getParent();
        }

        DefaultMutableTreeNode node = addElement(parentNode, child, true);
        
        selectElement(child);
        return node;
    }

    
    /** Add child to the currently selected node. */
    public DefaultMutableTreeNode addElementAsChild(Element child) {
        DefaultMutableTreeNode parentNode = null;
        
        TreePath parentPath = jtree.getSelectionPath();

        if (parentPath == null) {
            parentNode = rootNode;            
        } else {
            parentNode = (DefaultMutableTreeNode)(parentPath.getLastPathComponent());
        }

        DefaultMutableTreeNode node = addElement(parentNode, child, true);
        selectElement(child);
        return node;
    }
    
    public void addRootElement(Element elem)
	{
		addElement(rootNode, elem, true);
	}
    
    

    public DefaultMutableTreeNode addObject(DefaultMutableTreeNode parent, Element child) {
        return addElement(parent, child, false);
    }
    
    public DefaultMutableTreeNode addElement(DefaultMutableTreeNode parent, Element elem,  boolean shouldBeVisible) {
        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(elem);

        if (parent == null) {
            parent = rootNode;
        }
        
        if (parent.getUserObject() instanceof Element) {
        	Element parentElem = (Element)parent.getUserObject();
        	
        	elem.ParentElement = parentElem;
        	if (!parentElem.ChildElements.contains(elem)) {
        		parentElem.ChildElements.add(elem);	
        	}
        }
        
        ignoreExpandCollapse = true;
                
        // It is key to invoke this on the TreeModel, and NOT DefaultMutableTreeNode
        treeModel.insertNodeInto(childNode, parent, parent.getChildCount());

        //Make sure the user can see the lovely new node.
        if (shouldBeVisible) {
            jtree.scrollPathToVisible(new TreePath(childNode.getPath()));
        }
        
        for (Element child : elem.ChildElements) {
        	addElement(childNode, child, shouldBeVisible);
        }
        
        String path = new TreePath(childNode.getPath()).toString();
        String strpath=path.toString().substring(1).replace("]", "").replace(", ", "/");
        
        if (collapsedPaths.contains(strpath)) {
        	jtree.collapsePath(new TreePath(childNode.getPath()));
        } else {
            jtree.expandPath(new TreePath(childNode.getPath()));
        }
        
        ignoreExpandCollapse = false;
        
        
        return childNode;
    }


	public void updateUI()
	{
		jtree.updateUI();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) jtree.getModel().getRoot();

		for (int i = 0; i < root.getChildCount(); i++) {
			DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) root.getChildAt(i);
			updateTreeTextColor(childNode, Color.WHITE);
		}
	}

	public void updateTreeTextColor(DefaultMutableTreeNode node, Color color) {
		Object userObject = node.getUserObject();
		if (userObject instanceof Element) {
			Element elem = (Element) userObject;
			Color configColor = ElementTreeCellRenderer.colorConfig.get(elem.getName().toLowerCase());
			if (configColor != null) {
				color = configColor;
			}
			elem.TextColor = color;

			for (int i = 0; i < node.getChildCount(); i++) {
				DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
				updateTreeTextColor(childNode, color);
			}
		}
	}

	public static class FixedJTree extends JTree {
		
		private static final long serialVersionUID = 1L;
		
		public FixedJTree(TreeModel arg0)
		{
			super(arg0);
		}

		@Override
	    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
	        // filter property change of "dropLocation" with newValue==null, 
	        // since this will result in a NPE in BasicTreeUI.getDropLineRect(...)
	        if(newValue!=null || !"dropLocation".equals(propertyName)) {
	            super.firePropertyChange(propertyName, oldValue, newValue);
	        }
	    }
		
	}




public DefaultMutableTreeNode getNodeFor(Element element) {
	DefaultMutableTreeNode root = (DefaultMutableTreeNode)treeModel.getRoot();
	java.util.Enumeration<?> e = root.breadthFirstEnumeration();
	while (e.hasMoreElements()) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.nextElement();
		if (node.getUserObject() == element) return node;
	}
	return null;
}

public boolean removeElement(Element element) {
	DefaultMutableTreeNode node = getNodeFor(element);
	if (node == null) return false;

	DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
	if (parentNode == null) return false;

	Object parentObj = parentNode.getUserObject();
	if (parentObj instanceof Element) {
		Element parentElem = (Element)parentObj;
		parentElem.ChildElements.remove(element);
		parentElem.StepChildElements.remove(element);
		if (element.ParentElement == parentElem) element.ParentElement = null;
	}

	treeModel.removeNodeFromParent(node);
	return true;
}
}
