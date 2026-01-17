package at.vintagestory.modelcreator;

import java.awt.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;

import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;
import org.newdawn.slick.opengl.renderer.SGL;

import at.vintagestory.modelcreator.gui.right.ElementTree;
import at.vintagestory.modelcreator.gui.right.RightPanel;
import at.vintagestory.modelcreator.interfaces.IDrawable;
import at.vintagestory.modelcreator.interfaces.IElementManager;
import at.vintagestory.modelcreator.model.*;

public class Project
{
	// Persistent project data
	public boolean AmbientOcclusion;
	public ArrayList<PendingTexture> PendingTextures = new ArrayList<PendingTexture>();
	public LinkedHashMap<String, TextureEntry> TexturesByCode = new LinkedHashMap<String, TextureEntry>();
	public LinkedHashMap<String, int[]> TextureSizes = new LinkedHashMap<String, int[]>();
	public LinkedHashMap<String, String> MissingTexturesByCode = new LinkedHashMap<String, String>();
	
	public ArrayList<Element> rootElements = new ArrayList<Element>();
	public ArrayList<Animation> Animations = new ArrayList<Animation>();
	
	public int TextureWidth = 16;
	public int TextureHeight = 16;
	public boolean EntityTextureMode;
	public boolean AllAngles;
	public String backDropShape;
	public String mountBackDropShape;
	
	// Non-persistent project data
	public AttachmentPoint SelectedAttachmentPoint;
	public Element SelectedElement;
	public ArrayList<Element> SelectedElements = new ArrayList<Element>();
	public Animation SelectedAnimation;
	public boolean PlayAnimation = false;
	public ElementTree tree;
	public boolean needsSaving;
	
	public static int nextAttachmentPointNumber = 1;
	
	public String filePath;
	public String collapsedPaths;
	
	
	public Project(String filePath) {
		this.filePath = filePath;
		
		if (ModelCreator.rightTopPanel != null) {
			tree = ((RightPanel)ModelCreator.rightTopPanel).tree;	
		}	
	}
	
	
	public void LoadIntoEditor(IElementManager manager)
	{
		for (Animation anim : Animations) {
			anim.ResolveRelations(this);
		}
		
		for (PendingTexture ptex : PendingTextures) {
			ModelCreator.Instance.AddPendingTexture(ptex);	
		}
		
		ModelCreator.ignoreValueUpdates = true;
		tree.clearElements();
		if (collapsedPaths != null) tree.loadCollapsedPaths(collapsedPaths);
		ModelCreator.ignoreValueUpdates = false;
		
		for (Element elem : rootElements) {
			tree.addRootElement(elem);
		}
		
		tree.selectElement(SelectedElement);
		
		if (Animations.size() > 0) {
			SelectedAnimation = Animations.get(0);
			SelectedAnimation.SetFramesDirty();
		}
		
		if (backDropShape != null) {
			if (new File(backDropShape + ".json").exists()) {
				ModelCreator.Instance.LoadBackdropFile(backDropShape + ".json");
				
				copyKeyFrameElementsToBackDrop();
				
			} else {
				String shapeBasePath = ModelCreator.prefs.get("shapePath", ".");
				String path = shapeBasePath + File.separator + backDropShape + ".json";
				if (new File(path).exists()) {
					ModelCreator.Instance.LoadBackdropFile(path);
				} else {
					
					JFileChooser chooser = new JFileChooser(ModelCreator.prefs.get("shapePath", "."));
					chooser.setDialogTitle("Back drop shape file not found, select desired back drop shape file");
					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					
					int returnVal = chooser.showOpenDialog(null);
					if (returnVal == JFileChooser.APPROVE_OPTION)
					{
						ModelCreator.Instance.LoadBackdropFile(chooser.getSelectedFile().getAbsolutePath());
					}					
				}
			}
		}
		
		if (mountBackDropShape != null) {
			if (new File(mountBackDropShape + ".json").exists()) {
				ModelCreator.Instance.LoadMountBackdropFile(mountBackDropShape + ".json");
			} else {
				String shapeBasePath = ModelCreator.prefs.get("shapePath", ".");
				String path = shapeBasePath + File.separator + mountBackDropShape + ".json";
				if (new File(path).exists()) {
					ModelCreator.Instance.LoadMountBackdropFile(path);
				} else {
					
					JFileChooser chooser = new JFileChooser(ModelCreator.prefs.get("shapePath", "."));
					chooser.setDialogTitle("Back drop shape file not found, select desired back drop shape file");
					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					
					int returnVal = chooser.showOpenDialog(null);
					if (returnVal == JFileChooser.APPROVE_OPTION)
					{
						ModelCreator.Instance.LoadMountBackdropFile(chooser.getSelectedFile().getAbsolutePath());
					}					
				}
			}
		}
	}	
	
	public void copyKeyFrameElementsToBackDrop()
	{
		ArrayList<Animation> anims = Animations;

		for (Animation anim : anims) {
			Animation bdanim = ModelCreator.currentBackdropProject.findAnimation(anim.getName());
			if (bdanim == null) continue;
			
			for (AnimationFrame keyframe : anim.keyframes) {
				AnimationFrame bdframe = bdanim.GetKeyFrame(keyframe.getFrameNumber());
				if (bdframe == null) break;
				
				copyKeyFrameElementsToBackDrop(keyframe.getFrameNumber(), keyframe.Elements, bdanim);
			}
		}
		
	}
	
	private void copyKeyFrameElementsToBackDrop(int frame, List<IDrawable> elements, Animation bdanim)
	{
		for (IDrawable animframele : elements) {
			AnimFrameElement afe = (AnimFrameElement)animframele;
				
			AnimFrameElement backFrameElem = bdanim.GetOrCreateKeyFrameElement(afe.AnimatedElement, frame);
			backFrameElem.setFrom(afe);
			
			if (afe.ChildElements != null) {
				copyKeyFrameElementsToBackDrop(frame, afe.ChildElements, bdanim);
			}
		}
	}


	public void copyKeyFrameElemToBackdrop(Animation anim, Animation bdanim, Element elem)
	{
		AnimFrameElement mainFrameElem = anim.GetKeyFrameElement(elem, anim.currentFrame);
		if (mainFrameElem == null) return;
		
		AnimFrameElement backFrameElem = bdanim.GetOrCreateKeyFrameElement(elem, anim.currentFrame);
		backFrameElem.setFrom(mainFrameElem);		
	}


	public int getSelectedAnimationIndex()
	{
		for (int i = 0; i < Animations.size(); i++) {
			if (Animations.get(i) == SelectedAnimation) return i;
		}
		
		return -1;
	}
	
	public AnimationFrame GetKeyFrame(int frameIndex) {
		if (SelectedAnimation == null) return null;
		return SelectedAnimation.keyframes[frameIndex];
	}
	
	public AnimationFrame GetKeyFrameForFrame(int frameNumber) {
		if (SelectedAnimation == null) return null;
		
		AnimationFrame[] kfs = ModelCreator.currentProject.SelectedAnimation.keyframes;
		for (int i = 0; i < kfs.length; i++) {
			if (kfs[i].getFrameNumber() == frameNumber) {
				return kfs[i];
			}
		}
		 
		return null;
	}
	
	
	public int[] GetFrameNumbers() {
		if (SelectedAnimation == null) return null;
		return SelectedAnimation.frameNumbers;
	}
	
	
	public int GetKeyFrameCount() {
		if (SelectedAnimation == null) return 0;
		return SelectedAnimation.frameNumbers == null ? 0 : SelectedAnimation.frameNumbers.length;
	}

	
	public int GetFrameCount()
	{
		if (SelectedAnimation == null) return 0;
		return SelectedAnimation.GetQuantityFrames();
	}



	public void calculateCurrentFrameElements() {
		if (SelectedAnimation == null) return;
		AnimationFrame keyFrame = SelectedAnimation.keyframes[SelectedAnimation.currentFrame];
		if (keyFrame == null) return;
	}

	public List<IDrawable> getCurrentFrameRootElements()
	{
		if (SelectedAnimation == null || SelectedAnimation.keyframes.length == 0) return new ArrayList<IDrawable>(rootElements);
		
		if (SelectedAnimation.allFrames.size() == 0 || SelectedAnimation.currentFrame >= SelectedAnimation.allFrames.size()) {
			SelectedAnimation.SetFramesDirty();
			SelectedAnimation.currentFrame = Math.max(0, Math.min(SelectedAnimation.currentFrame - 1, SelectedAnimation.allFrames.size()));
			ModelCreator.updateFrame();
		}
		
		return SelectedAnimation.allFrames.get(SelectedAnimation.currentFrame).Elements;
	}
	
	public int CurrentAnimVersion() {
		if (SelectedAnimation == null || SelectedAnimation.keyframes.length == 0) return 0;
		
		return SelectedAnimation.version;
	}
	
	

	public void addElementAsChild(Element elem)
	{
		elem.ParentElement = SelectedElement;
		EnsureUniqueElementName(elem);
		
		if (elem.ParentElement == null) {
			rootElements.add(elem);
		}
		
		tree.addElementAsChild(elem);
		SelectedElement = elem;
		
		ModelCreator.DidModify();
		ModelCreator.updateValues(null);
		tree.updateUI();
	}
	
	
	public void addRootElement(Element e)
	{
		e.ParentElement = null;
		EnsureUniqueElementName(e);
		
		rootElements.add(e);
		tree.addRootElement(e);
		SelectedElement = tree.getSelectedElement();
		
		ModelCreator.DidModify();
		ModelCreator.updateValues(null);
		tree.jtree.updateUI();
	}
	
	
	public void duplicateCurrentElement() {
		ModelCreator.ignoreDidModify++;
		
		if (SelectedElement != null) {
			Element newElem = new Element(SelectedElement);
			newElem.ParentElement = SelectedElement.ParentElement;
			if (newElem.ParentElement != null) {
				newElem.ParentElement.ChildElements.add(newElem);
			}
												// Special case: Step parented elements will be in the root list, but also have a parent
			if (newElem.ParentElement == null || (SelectedElement.stepparentName != null && ModelCreator.currentProject.rootElements.contains(SelectedElement))) {
				rootElements.add(newElem);
			}
			
			EnsureUniqueElementName(newElem);
			
			tree.addElementAsSibling(newElem);
			tree.selectElement(newElem);
			
		}
		
		ModelCreator.ignoreDidModify--;
		
		SelectedElement = tree.getSelectedElement();
		ModelCreator.DidModify();
		ModelCreator.updateValues(null);
		
		ModelCreator.reloadStepparentRelationShips();
	}
	

	/**
	 * Computes the absolute X start translation of an element in root space,
	 * by summing startX along the parent chain (ignores rotations).
	 */
	private double getAbsoluteStartX(at.vintagestory.modelcreator.model.Element elem)
	{
		double x = 0;
		while (elem != null) {
			x += elem.getStartX();
			elem = elem.ParentElement;
		}
		return x;
	}

	/**
	 * Computes the absolute Z start translation of an element in root space,
	 * by summing startZ along the parent chain (ignores rotations).
	 */
	private double getAbsoluteStartZ(at.vintagestory.modelcreator.model.Element elem)
	{
		double z = 0;
		while (elem != null) {
			z += elem.getStartZ();
			elem = elem.ParentElement;
		}
		return z;
	}

public void mirrorSelectedElementsX() {
    mirrorSelectedElementsX(false);
}

public void mirrorSelectedElementsX(boolean mirrorAnimations) {
    // In Entity Texture Mode, the model is oriented such that "front" is along +/-X
    // (see the grid label), so left/right symmetry runs along Z.
    if (EntityTextureMode) {
        mirrorSelectedElementsZ(mirrorAnimations);
        return;
    }
    mirrorSelectedElementsInternal(false, mirrorAnimations);
}

public void mirrorSelectedElementsZ() {
    mirrorSelectedElementsZ(false);
}

public void mirrorSelectedElementsZ(boolean mirrorAnimations) {
    mirrorSelectedElementsInternal(true, mirrorAnimations);
}

private void mirrorSelectedElementsInternal(boolean mirrorOnZ, boolean mirrorAnimations) {
    if (tree == null) return;
    java.util.List<Element> selected = tree.getSelectedElements();
    if (selected == null || selected.size() == 0) return;

    // Only mirror topmost selected elements (avoid double-mirroring children of already selected parents)
    java.util.HashSet<Element> selset = new java.util.HashSet<Element>(selected);
    java.util.ArrayList<Element> top = new java.util.ArrayList<Element>();
    for (Element e : selected) {
        boolean hasSelectedParent = false;
        Element p = e.ParentElement;
        while (p != null) {
            if (selset.contains(p)) { hasSelectedParent = true; break; }
            p = p.ParentElement;
        }
        if (!hasSelectedParent) top.add(e);
    }

    ModelCreator.ignoreDidModify++;
    ModelCreator.changeHistory.beginMultichangeHistoryState();

    java.util.ArrayList<Element> mirroredTop = new java.util.ArrayList<Element>();
    java.util.HashMap<Element, Element> mirrorMap = new java.util.HashMap<Element, Element>();

    for (Element e : top) {
        Element newElem = new Element(e);
        newElem.ParentElement = e.ParentElement;

        // Compute parent absolute translation (root space) for hierarchy-safe mirroring
        double parentAbs = 0;
        if (e.ParentElement != null) {
            parentAbs = mirrorOnZ ? getAbsoluteStartZ(e.ParentElement) : getAbsoluteStartX(e.ParentElement);
        }

        // Mirror geometry + UVs
        if (mirrorOnZ) {
            newElem.mirrorZ(16, parentAbs, parentAbs);
        } else {
            newElem.mirrorX(16, parentAbs, parentAbs);
        }

        // Rename (L <-> R) based on which side the ORIGINAL element is on.
        renameMirroredSubtree(e, newElem, mirrorOnZ);
        ensureUniqueNameIfUsedRec(newElem);

        // Root list handling (step-parented elements can appear in root list and also have a parent)
        if (newElem.ParentElement == null || (e.stepparentName != null && rootElements.contains(e))) {
            rootElements.add(newElem);
        }

        mirroredTop.add(newElem);
        buildMirrorMapRec(e, newElem, mirrorMap);

        // Insert into tree
        if (newElem.ParentElement == null) {
            tree.addRootElement(newElem);
        } else {
            javax.swing.tree.DefaultMutableTreeNode parentNode = tree.getNodeFor(newElem.ParentElement);
            tree.addElement(parentNode, newElem, true);
        }
    }

    // Optionally mirror animation keyframes
    if (mirrorAnimations && mirrorMap.size() > 0) {
        mirrorAnimationsForMap(mirrorMap, mirrorOnZ);
        if (SelectedAnimation != null) {
            SelectedAnimation.SetFramesDirty();
        }
    }

    ModelCreator.ignoreDidModify--;

    // Select mirrored top-level elements
    try {
        javax.swing.JTree jtree = tree.jtree;
        javax.swing.tree.TreePath[] paths = new javax.swing.tree.TreePath[mirroredTop.size()];
        for (int i = 0; i < mirroredTop.size(); i++) {
            javax.swing.tree.DefaultMutableTreeNode node = tree.getNodeFor(mirroredTop.get(i));
            paths[i] = node == null ? null : new javax.swing.tree.TreePath(node.getPath());
        }
        java.util.ArrayList<javax.swing.tree.TreePath> valid = new java.util.ArrayList<javax.swing.tree.TreePath>();
        for (javax.swing.tree.TreePath p : paths) if (p != null) valid.add(p);
        jtree.setSelectionPaths(valid.toArray(new javax.swing.tree.TreePath[0]));
    } catch (Throwable t) {
        // ignore selection issues
    }

    SelectedElement = tree.getSelectedElement();
    SelectedElements = new ArrayList<Element>(tree.getSelectedElements());
    ModelCreator.DidModify();
    ModelCreator.updateValues(null);
    tree.jtree.updateUI();

    ModelCreator.changeHistory.endMultichangeHistoryState(ModelCreator.currentProject);
    ModelCreator.reloadStepparentRelationShips();
}


	/**
	 * Builds a mapping of original element -> mirrored element for the full subtree.
	 * Assumes the mirrored element was created via the Element copy constructor, so child ordering matches.
	 */
	private void buildMirrorMapRec(Element orig, Element mirrored, java.util.Map<Element, Element> map)
	{
		if (orig == null || mirrored == null) return;
		map.put(orig, mirrored);
		int cnt = Math.min(orig.ChildElements.size(), mirrored.ChildElements.size());
		for (int i = 0; i < cnt; i++) {
			buildMirrorMapRec(orig.ChildElements.get(i), mirrored.ChildElements.get(i), map);
		}
	}

	private double getAbsCenterFor(Element elem, boolean mirrorOnZ)
	{
		double abs = mirrorOnZ ? getAbsoluteStartZ(elem) : getAbsoluteStartX(elem);
		double size = mirrorOnZ ? elem.getDepth() : elem.getWidth();
		return abs + size / 2.0;
	}

	private char detectSideMarker(String name)
	{
		if (name == null) return 0;
		if (name.startsWith("Right")) return 'R';
		if (name.startsWith("Left")) return 'L';
		if (name.length() >= 2) {
			char c0 = name.charAt(0);
			char c1 = name.charAt(1);
			if ((c0 == 'R' || c0 == 'L') && Character.isUpperCase(c1)) return c0;
		}
		// Common delimiters: _R, -R, .R, spaceR
		if (name.matches(".*(?:^|[_\\-\\.\\s])R(?:$|[_\\-\\.\\s].*)")) return 'R';
		if (name.matches(".*(?:^|[_\\-\\.\\s])L(?:$|[_\\-\\.\\s].*)")) return 'L';
		return 0;
	}

	/**
	 * Swap left/right tokens in a name (RFemur <-> LFemur, RightArm <-> LeftArm, _R <-> _L, etc.)
	 */
	private String swapLeftRightInName(String name)
	{
		if (name == null) return null;
		String s = name;
		// Word swaps first
		s = s.replace("Left", "__TMPLEFT__");
		s = s.replace("Right", "__TMPRIGHT__");
		// Prefix swaps (RThing/LThing)
		s = s.replaceAll("^R(?=[A-Z])", "__TMPR__");
		s = s.replaceAll("^L(?=[A-Z])", "__TMPL__");
		// Delimited swaps (_RThing / _LThing / .RThing / -RThing / spaceRThing)
		s = s.replaceAll("(^|[_\\-\\.\\s])R(?=[A-Z])", "$1__TMPR__");
		s = s.replaceAll("(^|[_\\-\\.\\s])L(?=[A-Z])", "$1__TMPL__");
		// Suffix swaps (_R / _L / .R / -R)
		s = s.replaceAll("([_\\-\\.])R$", "$1__TMPR__");
		s = s.replaceAll("([_\\-\\.])L$", "$1__TMPL__");

		// Apply placeholders
		s = s.replace("__TMPR__", "L");
		s = s.replace("__TMPL__", "R");
		s = s.replace("__TMPLEFT__", "Right");
		s = s.replace("__TMPRIGHT__", "Left");
		return s;
	}

	/**
	 * Renames the mirrored subtree so left/right naming stays sane.
	 *
	 * - If the original name contains a clear side marker (L/R/Left/Right), we swap it.
	 * - Otherwise, if the element is clearly off-center, we prefix the mirrored element with the opposite side.
	 * - Centered elements just get "_mirror".
	 */
	private void renameMirroredSubtree(Element orig, Element mirrored, boolean mirrorOnZ)
	{
		if (orig == null || mirrored == null) return;
		double c = getAbsCenterFor(orig, mirrorOnZ);
		double dist = Math.abs(c - 8.0);
		boolean centered = dist < 0.0001;
		char marker = detectSideMarker(orig.getName());
		boolean origRight = marker == 'R' ? true : marker == 'L' ? false : c > 8.0;
		char mirroredSide = origRight ? 'L' : 'R';

		String newName;
		if (centered) {
			newName = orig.getName() + "_mirror";
		} else {
			String swapped = swapLeftRightInName(orig.getName());
			if (swapped != null && !swapped.equals(orig.getName())) {
				newName = swapped;
			} else {
				// No detectable marker, add one based on mirrored side
				newName = mirroredSide + orig.getName();
			}
		}

		mirrored.setName(newName);

		int cnt = Math.min(orig.ChildElements.size(), mirrored.ChildElements.size());
		for (int i = 0; i < cnt; i++) {
			renameMirroredSubtree(orig.ChildElements.get(i), mirrored.ChildElements.get(i), mirrorOnZ);
		}
	}

	private String makeUniqueNameIfUsed(String desired)
	{
		if (desired == null) return null;
		if (!IsElementNameUsed(desired, null)) return desired;

		String base = desired;
		int num = 2;
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(.*?)(\\d+)$").matcher(desired);
		if (m.matches()) {
			base = m.group(1);
			try {
				num = Integer.parseInt(m.group(2)) + 1;
			} catch (Exception ex) {
				num = 2;
			}
		}

		String cand = base + num;
		while (IsElementNameUsed(cand, null)) {
			num++;
			cand = base + num;
		}
		return cand;
	}

	private void ensureUniqueNameIfUsedRec(Element elem)
	{
		if (elem == null) return;
		elem.setName(makeUniqueNameIfUsed(elem.getName()));
		for (Element child : elem.ChildElements) {
			ensureUniqueNameIfUsedRec(child);
		}
	}

	/** Mirrors animation keyframe data for all mapped elements (orig -> mirrored). */
	private void mirrorAnimationsForMap(java.util.Map<Element, Element> mirrorMap, boolean mirrorOnZ)
	{
		if (mirrorMap == null || mirrorMap.size() == 0) return;

		for (Animation anim : Animations) {
			boolean changed = false;
			for (AnimationFrame kf : anim.keyframes) {
				int frameNo = kf.getFrameNumber();
				for (java.util.Map.Entry<Element, Element> ent : mirrorMap.entrySet()) {
					Element orig = ent.getKey();
					Element mir = ent.getValue();
					AnimFrameElement oldK = kf.GetKeyFrameElementFlat(orig);
					if (oldK == null) continue;

					AnimFrameElement newK = kf.GetOrCreateKeyFrameElementFlat(frameNo, mir);
					newK.setFrom(oldK);
					newK.AnimatedElement = mir;
					// Project is outside the model package; use the public accessor.
					newK.AnimatedElementName = mir.getName();

					// Reflect offsets/origins and rotations
					if (!mirrorOnZ) {
						// Mirror across X: X flips, pitch stays, yaw/roll invert
						if (newK.PositionSet) {
							newK.setOffsetX(-oldK.getOffsetX());
							newK.setOffsetY(oldK.getOffsetY());
							newK.setOffsetZ(oldK.getOffsetZ());
							newK.setOriginX(-oldK.getOriginX());
							newK.setOriginY(oldK.getOriginY());
							newK.setOriginZ(oldK.getOriginZ());
						}
						if (newK.RotationSet) {
							newK.setRotationX(oldK.getRotationX());
							newK.setRotationY(-oldK.getRotationY());
							newK.setRotationZ(-oldK.getRotationZ());
						}
					} else {
						// Mirror across Z: Z flips, roll stays, pitch/yaw invert
						if (newK.PositionSet) {
							newK.setOffsetX(oldK.getOffsetX());
							newK.setOffsetY(oldK.getOffsetY());
							newK.setOffsetZ(-oldK.getOffsetZ());
							newK.setOriginX(oldK.getOriginX());
							newK.setOriginY(oldK.getOriginY());
							newK.setOriginZ(-oldK.getOriginZ());
						}
						if (newK.RotationSet) {
							newK.setRotationX(-oldK.getRotationX());
							newK.setRotationY(-oldK.getRotationY());
							newK.setRotationZ(oldK.getRotationZ());
						}
					}

					changed = true;
				}
			}
			if (changed) anim.SetFramesDirty();
		}
	}

	

public void removeCurrentElement() {
	ModelCreator.ignoreDidModify++;

	java.util.List<Element> selected = tree == null ? null : tree.getSelectedElements();
	if (selected == null || selected.size() == 0) { ModelCreator.ignoreDidModify--; return; }

	// If multiple are selected, remove topmost selected elements
	java.util.HashSet<Element> selset = new java.util.HashSet<Element>(selected);
	java.util.ArrayList<Element> top = new java.util.ArrayList<Element>();
	for (Element e : selected) {
		boolean hasSelectedParent = false;
		Element p = e.ParentElement;
		while (p != null) {
			if (selset.contains(p)) { hasSelectedParent = true; break; }
			p = p.ParentElement;
		}
		if (!hasSelectedParent) top.add(e);
	}

	Element nextElem = tree.getNextSelectedElement();

	for (Element curElem : top) {
		tree.removeElement(curElem);

		if (curElem.ParentElement == null) {
			rootElements.remove(curElem);
		}

		for (int i = 0; i < Animations.size(); i++) {
			Animations.get(i).RemoveKeyFrameElement(curElem);
			if (Animations.get(i) == SelectedAnimation) {
				Animations.get(i).SetFramesDirty();
			}
		}

		curElem.onRemoved();
	}

	ModelCreator.ignoreDidModify--;

	if (nextElem != null) {
		tree.selectElement(nextElem);
	}

	SelectedElement = tree.getSelectedElement();
	SelectedElements = new ArrayList<Element>(tree.getSelectedElements());
	ModelCreator.DidModify();
	ModelCreator.updateValues(null);

	ModelCreator.reloadStepparentRelationShips();
}

	

	public void clear()
	{
		ModelCreator.ignoreValueUpdates = true;
		AmbientOcclusion = true;
		rootElements.clear();
		Animations.clear();
		SelectedElement = null;
		PendingTextures.clear();
		for (TextureEntry entry : TexturesByCode.values()) {
			entry.Dispose();
		}
		TexturesByCode.clear();
		tree.clearElements();
		SelectedAnimation = null;
		ModelCreator.ignoreValueUpdates = false;
		ModelCreator.updateValues(null);
	}
	
	
	public void selectElementAndFaceByOpenGLName(int openGlName)
	{
		tree.selectElementByOpenGLName(openGlName);
		SelectedElement = tree.getSelectedElement();
		SelectedElements = new ArrayList<Element>(tree.getSelectedElements());
		
		if (SelectedElement != null) {
			for (int i = 0; i < SelectedElement.getAllFaces().length; i++) {
				if (SelectedElement.getAllFaces()[i].openGlName == openGlName) {
					SelectedElement.setSelectedFace(i);
					break;
				}
			}
		}
		
		ModelCreator.updateValues(null);
	}
	
	/**
	 * Select an element by the OpenGL name of one of its faces, without changing the currently selected face.
	 * Used for click-to-select in Cube/Keyframe mode.
	 */
	public void selectElementByOpenGLName(int openGlName)
	{
		tree.selectElementByOpenGLName(openGlName);
		SelectedElement = tree.getSelectedElement();
		SelectedElements = new ArrayList<Element>(tree.getSelectedElements());
		ModelCreator.updateValues(null);
	}
	
	/**
	 * Add the element matching the OpenGL name (face id) to the current selection
	 * without clearing existing selection. Used for Shift + click in the 3D viewport.
	 */
	public void addElementSelectionByOpenGLName(int openGlName)
	{
		tree.addElementSelectionByOpenGLName(openGlName);
		SelectedElement = tree.getSelectedElement();
		SelectedElements = new ArrayList<Element>(tree.getSelectedElements());
		ModelCreator.updateValues(null);
	}
	
	/**
	 * Add all elements whose faces were hit by OpenGL selection inside the marquee rectangle
	 * without clearing existing selection. Used for Shift + drag in the 3D viewport.
	 */
	public void addElementsSelectionByOpenGLNames(Set<Integer> openGlNames)
	{
		tree.addElementsSelectionByOpenGLNames(openGlNames);
		SelectedElement = tree.getSelectedElement();
		SelectedElements = new ArrayList<Element>(tree.getSelectedElements());
		ModelCreator.updateValues(null);
	}
	

	
	public void selectElement(Element element)
	{
		tree.selectElement(element);
		SelectedElement = tree.getSelectedElement();
		ModelCreator.updateValues(null);
	}
	
	
	void EnsureUniqueElementName(Element elem) {
		String numberStr = "";
		int pos = elem.getName().length() - 1;
		while (pos > 0) {
			if (Character.isDigit(elem.getName().charAt(pos))) {
				numberStr = elem.getName().charAt(pos) + numberStr;
			} else break;
			pos--;
		}
		String baseName = elem.getName().substring(0, elem.getName().length() - numberStr.length());

		if (numberStr.length() == 0) numberStr = "1";
		int nextNumber = Integer.parseInt(numberStr) + 1;

		String newName = baseName + nextNumber;
		while (IsElementNameUsed(newName, elem)) {
			newName = baseName + nextNumber;
			nextNumber++;
		}
		elem.setName(newName);
		
		for (Element childElem : elem.ChildElements) {
			EnsureUniqueElementName(childElem);
		}
	}

	
	public boolean IsElementNameUsed(String name, Element exceptElement) {
		return IsElementNameUsed(name, rootElements, exceptElement);
	}
	
	public boolean IsAttachmentPointCodeUsed(String code, AttachmentPoint exceptPoint)
	{
		return IsAttachmentPointCodeUsed(code, rootElements, exceptPoint);	
	}
	
	public boolean IsAttachmentPointCodeUsed(String code, ArrayList<Element> elems, AttachmentPoint exceptPoint) {
		for (Element elem : elems) {
			if (elem.isAttachmentPointCodeUsed(code, exceptPoint)) return true;
		}
		return false;
	}

	
	boolean IsElementNameUsed(String name, ArrayList<Element> elems, Element exceptElement) {
		for (Element elem : elems) {
			if (elem == exceptElement) continue;
			
			if (elem.getName().equals(name)) return true;
			if (IsElementNameUsed(name, elem.ChildElements, exceptElement)) return true;
		}
		
		return false;
	}
	
	
	int TotalQuantityElements() {
		return TotalQuantityElements(rootElements);
	}
	
	
	int TotalQuantityElements(ArrayList<Element> elems) {
		int quantity = 0;
		for (Element elem : elems) {
			quantity++;
			quantity+= TotalQuantityElements(elem.ChildElements);
		}
		return quantity;
	}
	
	
	public AttachmentPoint findAttachmentPoint(String elementName) {
		return findAttachmentPoint(elementName, rootElements);
	}
	
	public AttachmentPoint findAttachmentPoint(String elementName, List<Element> list) {
		for (Element elem : list) {
			AttachmentPoint point = elem.findAttachmentPoint(elementName);
			if (point != null) return point;
		}
		
		return null;
	}
	
	
	public Element findElement(String elementName) {
		return findElement(elementName, rootElements);
	}
	
	public Element findElement(String elementName, List<Element> list) {
		for (Element elem : list) {
			if (elem.getName().equals(elementName)) return elem;
			
			Element foundElem = findElement(elementName, elem.ChildElements);
			if (foundElem != null) return foundElem;
		}
		
		return null;
	}
	
	


	public Project clone()
	{
		Project cloned = new Project(filePath);
		cloned.AmbientOcclusion = AmbientOcclusion;
		cloned.TexturesByCode = TexturesByCode;
		cloned.AllAngles = AllAngles;
		cloned.EntityTextureMode = EntityTextureMode;
		cloned.TextureWidth = TextureWidth;
		cloned.TextureHeight = TextureHeight;
		cloned.collapsedPaths = collapsedPaths;
		cloned.backDropShape = backDropShape;
		
		for (String key : TextureSizes.keySet()) {
			cloned.TextureSizes.put(key, TextureSizes.get(key));
		}
		
		for (Element elem : rootElements) {
			cloned.rootElements.add(elem.clone());
		}
		
		for (Animation anim : Animations) {
			Animation clonedAninm = anim.clone();
			cloned.Animations.add(clonedAninm);
			clonedAninm.ResolveRelations(cloned);
		}
		
		cloned.needsSaving = needsSaving;
		
		
		return cloned;
	}


	public Animation findAnimation(String name)
	{
		for (int i = 0; i < Animations.size(); i++) {
			if (Animations.get(i).getName().equals(name)) return Animations.get(i);
		}
		return null;
	}
		
	

	public void UpdateTextureCode(String oldCode, String newCode) {
		TextureEntry entry = TexturesByCode.get(oldCode);
		if (entry == null) return;
		
		TexturesByCode.remove(oldCode);
		TexturesByCode.put(newCode, entry);
		entry.code = newCode;
		UpdateTextureCode(rootElements, oldCode, newCode);
		
		ModelCreator.DidModify();
		ModelCreator.updateTitle();
	}
	
	void UpdateTextureCode(ArrayList<Element> elems, String oldCode, String newCode) {
		for (int i = 0; i < elems.size(); i++) {
			Element elem = elems.get(i);
			
			for (Face face : elem.getAllFaces()) {
				if (oldCode.equals(face.getTextureCode())) {
					face.setTextureCode(newCode);
				}
			}
			
			if (elem.ChildElements != null) {
				UpdateTextureCode(elem.ChildElements, oldCode, newCode);
			}
			
		}
	}

	public TextureEntry getTextureEntryByCode(String code)
	{
		return TexturesByCode.get(code);
	}

	public Texture getTextureByCode(String code)
	{
		TextureEntry entry = getTextureEntryByCode(code);
		if (entry == null) return null;
		
		return entry.getTexture();
	}
	
	

	public String getTextureFilepathByCode(String code)
	{
		TextureEntry entry = getTextureEntryByCode(code);
		if (entry == null) return null;
		return entry.getFilePath();
	}
	

	public ImageIcon getIconByCode(String code)
	{
		TextureEntry entry = getTextureEntryByCode(code);
		if (entry == null) return null;
		return entry.getIcon();
	}
	

	
	public void reloadTextures(ModelCreator creator) {
		for (TextureEntry entry : TexturesByCode.values()) {
			try {
				creator.pendingTextures.add(new PendingTexture(entry, 0));
			} catch (Exception e) {}
		}
	}
	
	
	public void reloadExternalTexture(TextureEntry entry) throws IOException {
		FileInputStream is = new FileInputStream(entry.getFilePath());
		Texture texture = TextureLoader.getTexture("PNG", is);
		is.close();
		
		if (texture.getImageHeight() % 8 != 0 | texture.getImageWidth() % 8 != 0)
		{
			texture.release();
			return;
		}
		
		entry.icon = upscaleIcon(new ImageIcon(entry.getFilePath()), 256);
		entry.texture = texture;
	}
	

	public String loadTexture(String textureCode, File image, BooleanParam isNew, String projectType, boolean doReplaceAll, boolean doReplacedForSelectedElement, boolean insertTextureSizeEntry) throws IOException
	{
		FileInputStream is = new FileInputStream(image);
		Texture texture;
		try {
			texture = TextureLoader.getTexture("PNG", is);
		} catch (Throwable e) {
			return "Unabled to load this texture, is this a valid png file?";
		}
		
		texture.setTextureFilter(SGL.GL_NEAREST);
		is.close();

		if (texture.getImageHeight() % 8 != 0 || texture.getImageWidth() % 8 != 0)
		{
			texture.release();
			return "Cannot load this texture, the width or length is not a multiple of 8 ("+texture.getImageHeight()+"x"+texture.getImageWidth()+")";
		}
				
		
		ImageIcon icon = upscaleIcon(new ImageIcon(image.getAbsolutePath()), 256);
		
		if (textureCode == null) {
			textureCode = image.getName().replace(".png", "");
		}
		
		ArrayList<String> nowFoundTextures = new ArrayList<String>(); 
		

		if (doReplaceAll) {
			this.TexturesByCode.clear();
		}
		
		if (!TexturesByCode.containsKey(textureCode)) {
			// Try and match by filename if not matched by code
			for (String key : MissingTexturesByCode.keySet()) {
				String val = MissingTexturesByCode.get(key);
				String filename = val.substring(val.lastIndexOf("/")+1);
				if (filename.equalsIgnoreCase(textureCode)) {
					TexturesByCode.put(key, new TextureEntry(key, texture, icon, image.getAbsolutePath(), projectType));
					nowFoundTextures.add(key);
				}	
			}
		}
		
		if (nowFoundTextures.size() == 0) {
			
			if (TexturesByCode.containsKey(textureCode)) {
				TextureEntry entry = TexturesByCode.get(textureCode);
				if (entry.getFilePath().equalsIgnoreCase(image.getAbsolutePath())) {
					TexturesByCode.put(textureCode, new TextureEntry(textureCode, texture, icon, image.getAbsolutePath(), projectType));
				} else {
					
					int i = 2;
					while (true) {
						
						String altimgName = textureCode + i;
						if (!TexturesByCode.containsKey(altimgName)) {
							TexturesByCode.put(altimgName, new TextureEntry(altimgName, texture, icon, image.getAbsolutePath(), projectType));
							textureCode = altimgName;
							break;
						}					
						i++;
					}
				}
				
				isNew.Value = false;
			} else {
				isNew.Value = true;
				TexturesByCode.put(textureCode, new TextureEntry(textureCode, texture, icon, image.getAbsolutePath(), projectType));	
			}
		} else {
			
			isNew.Value = true;
			
			for (String key : nowFoundTextures) {
				MissingTexturesByCode.remove(key);
			}			
		}
		
		if (insertTextureSizeEntry) {
			TextureSizes.put(textureCode, new int[] {
				(int)(texture.getImageWidth() / ModelCreator.noTexScale),
				(int)(texture.getImageHeight() / ModelCreator.noTexScale)
			});
		}
				
		
		if (doReplaceAll || (doReplacedForSelectedElement && SelectedElement != null)) {
			ModelCreator.changeHistory.beginMultichangeHistoryState();
		
			if (doReplaceAll) {
				for (Element elem : rootElements) {
					elem.setTextureCode(textureCode, true);
				}
			}
			if (doReplacedForSelectedElement && SelectedElement != null) {
				SelectedElement.setTextureCode(textureCode, true);
			}
			ModelCreator.DidModify();
			ModelCreator.changeHistory.endMultichangeHistoryState(this);
		}
		
		return null;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	public ImageIcon upscaleIcon(ImageIcon source, int length)
	{
		Image img = source.getImage();
		Image newimg = img.getScaledInstance(length, -1, java.awt.Image.SCALE_FAST);
		return new ImageIcon(newimg);
	}




	public void setProjectType(String type)
	{
		for (Element elem : rootElements) {
			elem.setProjectType(type);
		}

		for (PendingTexture tex : PendingTextures) {
			tex.SetProjectType(tex.ProjectType);
			ModelCreator.Instance.AddPendingTexture(tex);
		}
	}

	public void clearStepparentRelationShips() {
		for (Element elem : rootElements) {
			elem.clearStepparentRelationShip();
		}
	}
	
	public void reloadStepparentRelationShips() {
		for (Element elem : rootElements) {
			elem.reloadStepparentRelationShip();
		}
	}
	

	public void ReduceDecimals() {
		ModelCreator.changeHistory.beginMultichangeHistoryState();
		
		for (Element elem : rootElements) {
			elem.reduceDecimals();
		}
		
		ModelCreator.DidModify();
		ModelCreator.updateValues(null);
		ModelCreator.changeHistory.endMultichangeHistoryState(this);
	}
	

	public void TryGenSnowLayer()
	{
		ModelCreator.changeHistory.beginMultichangeHistoryState();
		
		for (Element elem : rootElements) {
			TryGenSnowLayer(elem);
		}

		if (!TexturesByCode.containsKey("snowcover")) {
			loadSnowTexture();
		}
		
		
		ModelCreator.DidModify();
		ModelCreator.updateValues(null);

		ModelCreator.ignoreValueUpdates = true;
		tree.clearElements();
		ModelCreator.ignoreValueUpdates = false;
		
		for (Element elem : rootElements) {
			tree.addRootElement(elem);
		}
		
		tree.selectElement(SelectedElement);
		
		ModelCreator.changeHistory.endMultichangeHistoryState(this);
	}

	

	private void TryGenSnowLayer(Element elem)
	{
		for (Element celem : elem.ChildElements) {
			if (celem.getName().contains("-snow")) return;
			
			TryGenSnowLayer(celem);
		}
	
		if (Math.abs(elem.getRotationX()) < 15 && Math.abs(elem.getRotationZ()) < 15) {
			if (!elem.getAllFaces()[4].isEnabled()) return;
			if (elem.getName().contains("-snow")) return;
			
			Element snowElem = elem.clone();
			snowElem.ChildElements.clear();
			snowElem.setRotationX(0);
			snowElem.setRotationY(0);
			snowElem.setRotationZ(0);
			snowElem.setStartX(0);
			snowElem.setStartZ(0);
			snowElem.setStartY(elem.getHeight() + 0.01);
			snowElem.setHeight(2);
			snowElem.setName(elem.getName() + "-snow");
			snowElem.setAutoUnwrap(true);
			snowElem.updateUV();
			
			for (int i = 0; i < 6; i++) {
				snowElem.getAllFaces()[i].setTextureCode("snowcover");
			}
			
			elem.ChildElements.add(snowElem);
		}
	}
	
	private void loadSnowTexture()
	{
		File textureFile = new File("block"+ File.separator +"snow"+ File.separator +"normal1.png");

		if (textureFile.exists() && textureFile.isFile())
		{
			synchronized (ModelCreator.Instance.pendingTextures) {
				ModelCreator.Instance.pendingTextures.add(new PendingTexture("snowcover", textureFile, 0));
			}
			return;
		}

		
		String textureBasePath = ModelCreator.prefs.get("texturePath", ".");
		File f = new File(textureBasePath + File.separator + "block"+ File.separator +"snow"+ File.separator +"normal1.png");
		
		if (f.exists())
		{
			synchronized (ModelCreator.Instance.pendingTextures) {
				ModelCreator.Instance.pendingTextures.add(new PendingTexture("snowcover", f, 0));
			}
			return;
		}
	}


	public int countTriangles()
	{
		int cnt = 0;
		for (Element elem : rootElements) {
			cnt += elem.countTriangles();
		}
		
		return cnt;
	}


	public void attachToBackdropProject(Project backDropProject)
	{
		insertStepChildren(rootElements, backDropProject);
		
		for (Animation anim : Animations) {
			anim.loadKeyFramesIntoProject(backDropProject);
		}
	}


	private void insertStepChildren(ArrayList<Element> myElements, Project backDropProject)
	{
		for (Element myElem : myElements) {
			if (myElem.stepparentName != null) {
				insertStepChild(myElem, backDropProject);
			}
			
			insertStepChildren(myElem.ChildElements, backDropProject);
		}
	}


	private void insertStepChild(Element myElem, Project backDropProject)
	{
		Element hisElem = backDropProject.findElement(myElem.stepparentName);
		if (hisElem != null) {
			myElem.ParentElement = hisElem;
			if (!hisElem.StepChildElements.contains(myElem)) {
				hisElem.StepChildElements.add(myElem);
			}
		}
	}


	public void EnsureAnimationSelected(Animation templateAnim)
	{
		if (templateAnim == null) {
			this.SelectedAnimation=null;
			return;
		}
		
		this.SelectedAnimation = findAnimation(templateAnim.getName());
		if (this.SelectedAnimation == null) {
			this.Animations.add(this.SelectedAnimation = templateAnim.shallowClone());
		}
	}


	public void clearUnusedTextures()
	{
		HashSet<String> usedCodes = new HashSet<String>();
		
		for (Element elem : rootElements) {
			elem.CollectTextureCodes(usedCodes);
		}
		
		boolean modified=false;
		
		for (String texCode : new HashSet<String>(TexturesByCode.keySet())) {
			if (!usedCodes.contains(texCode)) { TexturesByCode.remove(texCode); if (!modified) ModelCreator.changeHistory.beginMultichangeHistoryState(); modified = true; }
		}
		for (String texCode : new HashSet<String>(TextureSizes.keySet())) {
			if (!usedCodes.contains(texCode)) { TextureSizes.remove(texCode); if (!modified) ModelCreator.changeHistory.beginMultichangeHistoryState(); modified = true; }	
		}
		
		if (modified) {
			ModelCreator.changeHistory.endMultichangeHistoryState(this);
			ModelCreator.DidModify();
		}
		
	}

}
