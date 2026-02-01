package at.vintagestory.modelcreator;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import javax.imageio.ImageIO;

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
		
		if (ModelCreator.autofixZFighting && ModelCreator.currentRightTab == 0) {
			autoFixZFighting(java.util.Arrays.asList(elem), ModelCreator.autofixZFightingEpsilon, false);
		}
		
		ModelCreator.DidModify();
		ModelCreator.updateValues(null);
		tree.updateUI();
	}

	/**
	 * Add a new cube either as a child of the current selection or as a new root element.
	 *
	 * Root cubes spawn at the center of the 16x16 floor grid (the editor renders elements with
	 * a -8,-8 translation, so model-space grid center is (8, 0, 8)).
	 *
	 * Note: Some new-project states can have a stale SelectedElement reference even when there
	 * are no root elements yet. In that case we still treat this as a root insert.
	 */
	public void addCubeSmartCentered(Element elem)
	{
		boolean treatAsRoot = (SelectedElement == null) || (rootElements.size() == 0);

		if (treatAsRoot) {
			double cx = 8 - elem.getWidth() / 2.0;
			double cz = 8 - elem.getDepth() / 2.0;
			elem.setStartX(cx);
			elem.setStartY(0);
			elem.setStartZ(cz);

			addRootElement(elem);
			return;
		}

		addElementAsChild(elem);
	}
	
	
	public void addRootElement(Element e)
	{
		e.ParentElement = null;
		EnsureUniqueElementName(e);
		
		rootElements.add(e);
		tree.addRootElement(e);
		SelectedElement = tree.getSelectedElement();
		
		if (ModelCreator.autofixZFighting && ModelCreator.currentRightTab == 0) {
			autoFixZFighting(java.util.Arrays.asList(e), ModelCreator.autofixZFightingEpsilon, false);
		}
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
		// Auto-fix Z-fighting for the duplicated element (once, after creation)
		if (ModelCreator.autofixZFighting && ModelCreator.currentRightTab == 0 && SelectedElement != null) {
			autoFixZFighting(java.util.Arrays.asList(SelectedElement), ModelCreator.autofixZFightingEpsilon, false);
		}
		ModelCreator.DidModify();
		ModelCreator.updateValues(null);
		
		ModelCreator.reloadStepparentRelationShips();
	
	}

	/**
	 * Auto-fix Z-fighting by nudging one of the elements by a tiny amount when
	 * perfectly overlapping faces are detected (axis-aligned elements only).
	 *
	 * Performance friendly: intended to be called only after create/duplicate/drop events,
	 * not every frame.
	 *
	 * @param focusElems Elements to check against the rest of the model
	 * @param epsilon Amount to nudge along the detected axis
	 * @param commit If true, issues a single DidModify() after applying the fix
	 */
	public boolean autoFixZFighting(java.util.List<Element> focusElems, double epsilon, boolean commit)
	{
		if (focusElems == null || focusElems.size() == 0) return false;
		// Only for Modeling tab
		if (ModelCreator.currentRightTab != 0) return false;
		
		java.util.List<Element> all = collectAllElements();
		if (all == null || all.size() < 2) return false;
		
		boolean didFix = false;
		int prevIgnore = ModelCreator.ignoreDidModify;
		try {
			// Suppress repeated history snapshots while we apply multiple tiny nudges
			ModelCreator.ignoreDidModify++;
			for (Element focus : focusElems) {
				if (focus == null) continue;
				didFix |= autoFixZFightingForElement(focus, all, epsilon);
			}
		} finally {
			ModelCreator.ignoreDidModify = prevIgnore;
		}
		
		if (didFix && commit) {
			ModelCreator.DidModify();
			ModelCreator.updateValues(null);
		}
		return didFix;
	}
	
	private static class AABB {
		double minX, minY, minZ;
		double maxX, maxY, maxZ;
	}
	
	private java.util.List<Element> collectAllElements()
	{
		java.util.ArrayList<Element> list = new java.util.ArrayList<Element>();
		for (Element root : rootElements) {
			collectAllElementsRec(root, list);
		}
		return list;
	}
	
	private void collectAllElementsRec(Element elem, java.util.List<Element> list)
	{
		if (elem == null) return;
		list.add(elem);
		if (elem.ChildElements != null) {
			for (Element child : elem.ChildElements) collectAllElementsRec(child, list);
		}
		if (elem.StepChildElements != null) {
			for (Element child : elem.StepChildElements) collectAllElementsRec(child, list);
		}
	}
	
	private boolean autoFixZFightingForElement(Element focus, java.util.List<Element> all, double epsilon)
	{
		if (focus == null) return false;

		// Two-stage approach:
		// 1) Fast path for fully axis-aligned elements (no rotations in chain)
		// 2) Robust path that supports rotations (common case: 90-degree rotations)
		if (!hasRotationInChain(focus)) {
			AABB a = getWorldAABB(focus);
			if (a != null) {
				final double tol = 1e-4;
				double bestArea = 0;
				int bestAxis = 0;
				Element bestMover = null;

				for (Element other : all) {
					if (other == null || other == focus) continue;
					if (hasRotationInChain(other)) continue;
					AABB b = getWorldAABB(other);
					if (b == null) continue;

					// X faces
					double oy = overlapLen(a.minY, a.maxY, b.minY, b.maxY);
					double oz = overlapLen(a.minZ, a.maxZ, b.minZ, b.maxZ);
					if (oy > tol && oz > tol) {
						if (Math.abs(a.minX - b.minX) < tol || Math.abs(a.minX - b.maxX) < tol || Math.abs(a.maxX - b.minX) < tol || Math.abs(a.maxX - b.maxX) < tol) {
							double area = oy * oz;
							if (area > bestArea) { bestArea = area; bestAxis = 1; bestMover = chooseMover(focus, other); }
						}
					}

					// Y faces
					double ox = overlapLen(a.minX, a.maxX, b.minX, b.maxX);
					if (ox > tol && oz > tol) {
						if (Math.abs(a.minY - b.minY) < tol || Math.abs(a.minY - b.maxY) < tol || Math.abs(a.maxY - b.minY) < tol || Math.abs(a.maxY - b.maxY) < tol) {
							double area = ox * oz;
							if (area > bestArea) { bestArea = area; bestAxis = 2; bestMover = chooseMover(focus, other); }
						}
					}

					// Z faces
					double oy2 = overlapLen(a.minY, a.maxY, b.minY, b.maxY);
					double ox2 = overlapLen(a.minX, a.maxX, b.minX, b.maxX);
					if (ox2 > tol && oy2 > tol) {
						if (Math.abs(a.minZ - b.minZ) < tol || Math.abs(a.minZ - b.maxZ) < tol || Math.abs(a.maxZ - b.minZ) < tol || Math.abs(a.maxZ - b.maxZ) < tol) {
							double area = ox2 * oy2;
							if (area > bestArea) { bestArea = area; bestAxis = 3; bestMover = chooseMover(focus, other); }
						}
					}
				}

				if (bestAxis != 0 && bestMover != null) {
					// Axis-aligned elements: direct start offset works as expected
					switch (bestAxis) {
						case 1: bestMover.setStartX(bestMover.getStartX() + epsilon); return true;
						case 2: bestMover.setStartY(bestMover.getStartY() + epsilon); return true;
						case 3: bestMover.setStartZ(bestMover.getStartZ() + epsilon); return true;
						default: break;
					}
				}
			}
		}

		// Robust face-plane based detection (supports rotated elements).
		// We intentionally keep this fairly lightweight: 6 faces per element.
		final double planeTol = 1e-4;
		final double overlapTol = 1e-6;
		java.util.List<FaceRect> focusFaces = getWorldFaceRects(focus);
		if (focusFaces == null || focusFaces.size() == 0) return false;

		double bestArea = 0;
		int bestAxis = 0;
		Element bestMover = null;
		for (Element other : all) {
			if (other == null || other == focus) continue;
			java.util.List<FaceRect> otherFaces = getWorldFaceRects(other);
			if (otherFaces == null || otherFaces.size() == 0) continue;

			for (FaceRect fa : focusFaces) {
				for (FaceRect fb : otherFaces) {
					if (fa.axis != fb.axis) continue;
					if (Math.abs(fa.plane - fb.plane) > planeTol) continue;
					double ou = overlapLen(fa.umin, fa.umax, fb.umin, fb.umax);
					double ov = overlapLen(fa.vmin, fa.vmax, fb.vmin, fb.vmax);
					if (ou <= overlapTol || ov <= overlapTol) continue;
					double area = ou * ov;
					if (area > bestArea) {
						bestArea = area;
						bestAxis = fa.axis;
						bestMover = chooseMover(focus, other);
					}
				}
			}
		}

		if (bestAxis == 0 || bestMover == null) return false;
		applyWorldAxisNudge(bestMover, bestAxis, epsilon);
		return true;
	}

	private static class FaceRect {
		// Axis of the face normal in world space: 1=X, 2=Y, 3=Z
		int axis;
		// Plane coordinate along axis (world)
		double plane;
		// Bounds on the other two world axes
		double umin, umax;
		double vmin, vmax;
	}

	private java.util.List<FaceRect> getWorldFaceRects(Element elem)
	{
		if (elem == null) return null;
		float[] mat = getWorldMatrix(elem);
		if (mat == null) return null;

		double w = elem.getWidth();
		double h = elem.getHeight();
		double d = elem.getDepth();

		// Local vertices (after ApplyTransform, geometry is at 0..w/h/d)
		double[][] lv = new double[][] {
			{0, 0, 0}, {w, 0, 0}, {w, h, 0}, {0, h, 0},
			{0, 0, d}, {w, 0, d}, {w, h, d}, {0, h, d}
		};
		double[][] v = new double[8][3];
		for (int i = 0; i < 8; i++) {
			float[] out = at.vintagestory.modelcreator.util.Mat4f.MulWithVec4(mat, new float[] { (float)lv[i][0], (float)lv[i][1], (float)lv[i][2], 1 });
			v[i][0] = out[0];
			v[i][1] = out[1];
			v[i][2] = out[2];
		}

		// Faces as vertex indices (quads)
		int[][] faces = new int[][] {
			{0, 3, 7, 4}, // -X
			{1, 5, 6, 2}, // +X
			{0, 4, 5, 1}, // -Y
			{3, 2, 6, 7}, // +Y
			{0, 1, 2, 3}, // -Z
			{4, 7, 6, 5}  // +Z
		};

		java.util.ArrayList<FaceRect> rects = new java.util.ArrayList<FaceRect>(6);
		for (int fi = 0; fi < faces.length; fi++) {
			int i0 = faces[fi][0];
			int i1 = faces[fi][1];
			int i2 = faces[fi][2];
			int i3 = faces[fi][3];

			// Normal from first 3 verts
			double ax = v[i1][0] - v[i0][0];
			double ay = v[i1][1] - v[i0][1];
			double az = v[i1][2] - v[i0][2];
			double bx = v[i2][0] - v[i0][0];
			double by = v[i2][1] - v[i0][1];
			double bz = v[i2][2] - v[i0][2];
			double nx = ay * bz - az * by;
			double ny = az * bx - ax * bz;
			double nz = ax * by - ay * bx;
			double nlen = Math.sqrt(nx*nx + ny*ny + nz*nz);
			if (nlen < 1e-9) continue;
			nx /= nlen; ny /= nlen; nz /= nlen;

			// Quantize to axis-aligned normals only (covers 90-degree rotations)
			double anx = Math.abs(nx), any = Math.abs(ny), anz = Math.abs(nz);
			int axis;
			if (anx >= any && anx >= anz && anx > 0.9) axis = 1;
			else if (any >= anx && any >= anz && any > 0.9) axis = 2;
			else if (anz > 0.9) axis = 3;
			else continue;

			FaceRect fr = new FaceRect();
			fr.axis = axis;

			// Plane is average coordinate along axis
			fr.plane = 0.25 * (
				(axis == 1 ? v[i0][0] + v[i1][0] + v[i2][0] + v[i3][0] :
				 axis == 2 ? v[i0][1] + v[i1][1] + v[i2][1] + v[i3][1] :
				            v[i0][2] + v[i1][2] + v[i2][2] + v[i3][2])
			);

			// Bounds on remaining axes
			double[] u = new double[4];
			double[] vv = new double[4];
			for (int k = 0; k < 4; k++) {
				int vi = faces[fi][k];
				if (axis == 1) { u[k] = v[vi][1]; vv[k] = v[vi][2]; }
				else if (axis == 2) { u[k] = v[vi][0]; vv[k] = v[vi][2]; }
				else { u[k] = v[vi][0]; vv[k] = v[vi][1]; }
			}
			fr.umin = Math.min(Math.min(u[0], u[1]), Math.min(u[2], u[3]));
			fr.umax = Math.max(Math.max(u[0], u[1]), Math.max(u[2], u[3]));
			fr.vmin = Math.min(Math.min(vv[0], vv[1]), Math.min(vv[2], vv[3]));
			fr.vmax = Math.max(Math.max(vv[0], vv[1]), Math.max(vv[2], vv[3]));

			rects.add(fr);
		}

		return rects;
	}

	private float[] getWorldMatrix(Element elem)
	{
		if (elem == null) return null;
		java.util.ArrayList<Element> chain = new java.util.ArrayList<Element>();
		Element cur = elem;
		while (cur != null) {
			chain.add(cur);
			cur = cur.ParentElement;
		}
		java.util.Collections.reverse(chain);
		float[] mat = at.vintagestory.modelcreator.util.Mat4f.Create();
		for (Element e : chain) {
			e.ApplyTransform(mat);
		}
		return mat;
	}

	private void applyWorldAxisNudge(Element mover, int worldAxis, double epsilon)
	{
		if (mover == null) return;
		float[] mat = getWorldMatrix(mover);
		if (mat == null) return;

		// World-space delta
		double wx = (worldAxis == 1 ? epsilon : 0);
		double wy = (worldAxis == 2 ? epsilon : 0);
		double wz = (worldAxis == 3 ? epsilon : 0);

		// Extract rotation (column-major):
		// [0 4 8]
		// [1 5 9]
		// [2 6 10]
		double r00 = mat[0], r01 = mat[4], r02 = mat[8];
		double r10 = mat[1], r11 = mat[5], r12 = mat[9];
		double r20 = mat[2], r21 = mat[6], r22 = mat[10];

		// local = R^T * world
		double lx = r00 * wx + r10 * wy + r20 * wz;
		double ly = r01 * wx + r11 * wy + r21 * wz;
		double lz = r02 * wx + r12 * wy + r22 * wz;

		mover.setStartX(mover.getStartX() + lx);
		mover.setStartY(mover.getStartY() + ly);
		mover.setStartZ(mover.getStartZ() + lz);
	}
	
	private Element chooseMover(Element a, Element b)
	{
		// Prefer moving the child when elements are parented
		if (isAncestor(b, a)) return a;
		if (isAncestor(a, b)) return b;
		if (a.ParentElement != null) return a;
		if (b.ParentElement != null) return b;
		return a;
	}
	
	private boolean isAncestor(Element ancestor, Element child)
	{
		Element cur = child != null ? child.ParentElement : null;
		while (cur != null) {
			if (cur == ancestor) return true;
			cur = cur.ParentElement;
		}
		return false;
	}
	
	private boolean hasRotationInChain(Element elem)
	{
		Element cur = elem;
		while (cur != null) {
			if (Math.abs(cur.getRotationX()) > 1e-9 || Math.abs(cur.getRotationY()) > 1e-9 || Math.abs(cur.getRotationZ()) > 1e-9) return true;
			cur = cur.ParentElement;
		}
		return false;
	}
	
	private AABB getWorldAABB(Element elem)
	{
		if (elem == null) return null;
		double sx = 0, sy = 0, sz = 0;
		Element cur = elem;
		while (cur != null) {
			sx += cur.getStartX();
			sy += cur.getStartY();
			sz += cur.getStartZ();
			cur = cur.ParentElement;
		}
		AABB box = new AABB();
		box.minX = sx;
		box.minY = sy;
		box.minZ = sz;
		box.maxX = sx + elem.getWidth();
		box.maxY = sy + elem.getHeight();
		box.maxZ = sz + elem.getDepth();
		return box;
	}
	
	private double overlapLen(double amin, double amax, double bmin, double bmax)
	{
		double min = Math.max(amin, bmin);
		double max = Math.min(amax, bmax);
		return max - min;
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

	


	// ------------------------
	// Centering tools (Modeling tab)
	// ------------------------
	public static enum CenterMode {
		ON_FLOOR,
		KEEP_HEIGHT,
		LITERAL
	}

	/**
	 * Centers the current selection on the floor grid.
	 *
	 * Rules:
	 * - If a single element is selected, it is centered.
	 * - If multiple elements are selected:
	 *   - If they share a common parent/ancestor, we center the top common ancestor (so children come along).
	 *   - Otherwise we center all topmost selected hierarchies as a group.
	 * - Center is computed from the geometric bounds (AABB in root space). Rotation origin is NOT used.
	 */
	public void centerSelectedElements(CenterMode mode)
	{
		if (tree == null) return;
		java.util.List<Element> selected = tree.getSelectedElements();
		if (selected == null || selected.size() == 0) return;

		// Determine move targets and bounds basis.
		java.util.ArrayList<Element> moveTargets = new java.util.ArrayList<Element>();
		java.util.ArrayList<Element> boundsRoots = new java.util.ArrayList<Element>();

		if (selected.size() > 1) {
			Element lca = findLowestCommonAncestor(selected);
			if (lca != null) {
				// All selected live under one top object: move that object and use its full volume.
				moveTargets.add(lca);
				boundsRoots.add(lca);
			} else {
				// Multiple independent hierarchies: move only the topmost selected elements and use their union.
				java.util.HashSet<Element> selset = new java.util.HashSet<Element>(selected);
				for (Element e : selected) {
					boolean hasSelectedParent = false;
					Element p = e.ParentElement;
					while (p != null) {
						if (selset.contains(p)) { hasSelectedParent = true; break; }
						p = p.ParentElement;
					}
					if (!hasSelectedParent) {
						moveTargets.add(e);
						boundsRoots.add(e);
					}
				}
			}
		} else {
			moveTargets.add(selected.get(0));
			boundsRoots.add(selected.get(0));
		}

		if (moveTargets.size() == 0 || boundsRoots.size() == 0) return;

		// Compute root-space bounds (union of hierarchy volumes).
		double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

		for (Element r : boundsRoots) {
			double[] b = computeHierarchyRootBounds(r);
			if (b == null) continue;
			minX = Math.min(minX, b[0]);
			minY = Math.min(minY, b[1]);
			minZ = Math.min(minZ, b[2]);
			maxX = Math.max(maxX, b[3]);
			maxY = Math.max(maxY, b[4]);
			maxZ = Math.max(maxZ, b[5]);
		}

		if (!Double.isFinite(minX) || !Double.isFinite(maxX)) return;

		double cx = (minX + maxX) / 2.0;
		double cy = (minY + maxY) / 2.0;
		double cz = (minZ + maxZ) / 2.0;

		// Grid center is at (8, 0, 8) in model space.
		double dx = 8.0 - cx;
		double dz = 8.0 - cz;
		double dy = 0.0;

		switch (mode) {
			case ON_FLOOR:
				dy = 0.0 - minY;
				break;
			case LITERAL:
				dy = 0.0 - cy;
				break;
			case KEEP_HEIGHT:
			default:
				dy = 0.0;
				break;
		}

		// Apply translation in WORLD space to each move target, converting into local Start deltas.
		for (Element e : moveTargets) {
			if (e == null) continue;
			double[] map = computeWorldAxisToLocalMap(e);
			if (map == null || map.length < 9) {
				// Fallback: treat local as world.
				e.setStartX(e.getStartX() + dx);
				e.setStartY(e.getStartY() + dy);
				e.setStartZ(e.getStartZ() + dz);
			} else {
				double ldx = dx * map[0] + dy * map[3] + dz * map[6];
				double ldy = dx * map[1] + dy * map[4] + dz * map[7];
				double ldz = dx * map[2] + dy * map[5] + dz * map[8];
				e.setStartX(e.getStartX() + ldx);
				e.setStartY(e.getStartY() + ldy);
				e.setStartZ(e.getStartZ() + ldz);
			}
		}

		SelectedElement = tree.getSelectedElement();
		SelectedElements = new ArrayList<Element>(tree.getSelectedElements());
		ModelCreator.DidModify();
	}

	private static Element findLowestCommonAncestor(java.util.List<Element> elems)
	{
		if (elems == null || elems.size() < 2) return null;

		java.util.ArrayList<Element> basePath = new java.util.ArrayList<Element>();
		basePath.addAll(elems.get(0).GetParentPath());
		basePath.add(elems.get(0)); // include self

		int commonLen = basePath.size();
		for (int i = 1; i < elems.size(); i++) {
			Element e = elems.get(i);
			java.util.ArrayList<Element> path = new java.util.ArrayList<Element>();
			path.addAll(e.GetParentPath());
			path.add(e);

			int n = Math.min(commonLen, path.size());
			int j = 0;
			for (; j < n; j++) {
				if (basePath.get(j) != path.get(j)) break;
			}
			commonLen = j;
			if (commonLen == 0) return null;
		}

		Element lca = basePath.get(commonLen - 1);
		// If the LCA is null or if it does not actually cover all selected (shouldn't happen), return null.
		return lca;
	}

	private static double[] computeHierarchyRootBounds(Element root)
	{
		if (root == null) return null;

		double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

		java.util.ArrayDeque<Element> stack = new java.util.ArrayDeque<Element>();
		stack.push(root);
		while (!stack.isEmpty()) {
			Element e = stack.pop();
			if (e == null) continue;

			double[] b = computeElementRootBounds(e);
			if (b != null) {
				minX = Math.min(minX, b[0]);
				minY = Math.min(minY, b[1]);
				minZ = Math.min(minZ, b[2]);
				maxX = Math.max(maxX, b[3]);
				maxY = Math.max(maxY, b[4]);
				maxZ = Math.max(maxZ, b[5]);
			}

			if (e.ChildElements != null) {
				for (int i = 0; i < e.ChildElements.size(); i++) {
					stack.push(e.ChildElements.get(i));
				}
			}
		}

		if (!Double.isFinite(minX) || !Double.isFinite(maxX)) return null;
		return new double[] { minX, minY, minZ, maxX, maxY, maxZ };
	}

	private static double[] computeElementRootBounds(Element elem)
	{
		if (elem == null) return null;

		float[] mat = at.vintagestory.modelcreator.util.Mat4f.Identity_(new float[16]);

		// Apply parent transforms first (root -> parent)
		java.util.List<Element> path = elem.GetParentPath();
		if (path != null) {
			for (Element p : path) {
				applyTransformNoAnim(mat, p);
			}
		}
		// Apply this element's transform
		applyTransformNoAnim(mat, elem);

		double w = elem.getWidth();
		double h = elem.getHeight();
		double d = elem.getDepth();

		double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

		double[][] corners = new double[][] {
			{0, 0, 0}, {w, 0, 0}, {0, h, 0}, {0, 0, d},
			{w, h, 0}, {w, 0, d}, {0, h, d}, {w, h, d}
		};

		for (double[] c : corners) {
			float[] out = at.vintagestory.modelcreator.util.Mat4f.MulWithVec4(mat, new float[] { (float)c[0], (float)c[1], (float)c[2], 1f });
			double x = out[0];
			double y = out[1];
			double z = out[2];
			minX = Math.min(minX, x);
			minY = Math.min(minY, y);
			minZ = Math.min(minZ, z);
			maxX = Math.max(maxX, x);
			maxY = Math.max(maxY, y);
			maxZ = Math.max(maxZ, z);
		}

		if (!Double.isFinite(minX) || !Double.isFinite(maxX)) return null;
		return new double[] { minX, minY, minZ, maxX, maxY, maxZ };
	}

	private static void applyTransformNoAnim(float[] mat, Element elem)
	{
		if (elem == null) return;
		at.vintagestory.modelcreator.util.Mat4f.Translate(mat, mat, new float[] { (float)elem.getOriginX(), (float)elem.getOriginY(), (float)elem.getOriginZ() });
		at.vintagestory.modelcreator.util.Mat4f.RotateX(mat, mat, (float)elem.getRotationX() * at.vintagestory.modelcreator.util.GameMath.DEG2RAD);
		at.vintagestory.modelcreator.util.Mat4f.RotateY(mat, mat, (float)elem.getRotationY() * at.vintagestory.modelcreator.util.GameMath.DEG2RAD);
		at.vintagestory.modelcreator.util.Mat4f.RotateZ(mat, mat, (float)elem.getRotationZ() * at.vintagestory.modelcreator.util.GameMath.DEG2RAD);
		at.vintagestory.modelcreator.util.Mat4f.Translate(mat, mat, new float[] { (float)-elem.getOriginX(), (float)-elem.getOriginY(), (float)-elem.getOriginZ() });

		at.vintagestory.modelcreator.util.Mat4f.Translate(mat, mat, new float[] { (float)elem.getStartX(), (float)elem.getStartY(), (float)elem.getStartZ() });
	}

	private static double[] computeWorldAxisToLocalMap(Element elem)
	{
		float[] m = buildRotationMatrixToElement(elem);
		double c0x = m[0],  c0y = m[1],  c0z = m[2];
		double c1x = m[4],  c1y = m[5],  c1z = m[6];
		double c2x = m[8],  c2y = m[9],  c2z = m[10];

		double[] out = new double[9];
		out[0] = c0x; out[1] = c1x; out[2] = c2x;
		out[3] = c0y; out[4] = c1y; out[5] = c2y;
		out[6] = c0z; out[7] = c1z; out[8] = c2z;
		return out;
	}

	private static float[] buildRotationMatrixToElement(Element elem)
	{
		float[] mat = at.vintagestory.modelcreator.util.Mat4f.Identity_(new float[16]);
		if (elem == null) return mat;
		java.util.ArrayList<Element> chain = new java.util.ArrayList<Element>();
		Element cur = elem;
		while (cur != null) {
			chain.add(0, cur);
			cur = cur.ParentElement;
		}
		for (Element e : chain) {
			float ox = (float)e.getOriginX();
			float oy = (float)e.getOriginY();
			float oz = (float)e.getOriginZ();
			at.vintagestory.modelcreator.util.Mat4f.Translate(mat, mat, new float[] { ox, oy, oz });
			at.vintagestory.modelcreator.util.Mat4f.RotateX(mat, mat, (float)e.getRotationX() * at.vintagestory.modelcreator.util.GameMath.DEG2RAD);
			at.vintagestory.modelcreator.util.Mat4f.RotateY(mat, mat, (float)e.getRotationY() * at.vintagestory.modelcreator.util.GameMath.DEG2RAD);
			at.vintagestory.modelcreator.util.Mat4f.RotateZ(mat, mat, (float)e.getRotationZ() * at.vintagestory.modelcreator.util.GameMath.DEG2RAD);
			at.vintagestory.modelcreator.util.Mat4f.Translate(mat, mat, new float[] { -ox, -oy, -oz });
		}
		return mat;
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
		// Keep multi-selection in sync even when clearing selection.
		SelectedElements = new ArrayList<Element>(tree.getSelectedElements());
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
		String path = entry.getFilePath();
		String lc = path == null ? "" : path.toLowerCase(Locale.ROOT);
		boolean isRef = "refimage".equals(entry.code);
		
		// Normal textures in VSMC are PNG-only. The reference image supports PNG/JPG.
		String format;
		if (lc.endsWith(".jpg") || lc.endsWith(".jpeg")) {
			format = isRef ? "JPG" : null;
		} else {
			format = "PNG";
		}
		
		if (format == null) return;
		
		FileInputStream is = new FileInputStream(path);
		Texture texture = null;
		try {
			texture = TextureLoader.getTexture(format, is);
		} finally {
			try { is.close(); } catch (Exception e) {}
		}
		
		// Keep the original size constraints for normal textures, but don't block reference images.
		if (!isRef && (texture.getImageHeight() % 8 != 0 || texture.getImageWidth() % 8 != 0))
		{
			texture.release();
			return;
		}
		
		texture.setTextureFilter(SGL.GL_NEAREST);
		entry.icon = upscaleIcon(new ImageIcon(path), 256);
		entry.texture = texture;
		entry.Width = texture.getImageWidth();
		entry.Height = texture.getImageHeight();
	}

	/**
	 * Creates/updates a "Ref_Image" plane element that shows the already-loaded
	 * reference image texture stored under <code>texCode</code> (usually "refimage").
	 *
	 * NOTE: Texture upload must happen on the render thread. This method is safe to
	 * call from the Swing thread after the texture has been loaded via PendingTexture.
	 */
	public String applyReferenceImagePlane(String texCode)
	{
		if (texCode == null || texCode.length() == 0) texCode = "refimage";

		TextureEntry entry = TexturesByCode.get(texCode);
		if (entry == null) return "Reference image texture was not loaded.";

		int imgW = Math.max(1, entry.Width);
		int imgH = Math.max(1, entry.Height);

		int[] dims = pickBestAspectDims(imgW, imgH, 16);
		int planeW = dims[0];
		int planeH = dims[1];

		// Tell the UV system that this texture is planeW x planeH "voxels" large,
		// so that a 0..planeW/0..planeH UV covers the whole image.
		TextureSizes.put(texCode, new int[] { planeW, planeH });

		ModelCreator.changeHistory.beginMultichangeHistoryState();

		Element refElem = findRootElementByName("Ref_Image");
		boolean created = false;
		if (refElem == null) {
			refElem = new Element(planeW, planeH);
			refElem.setName("Ref_Image");
			refElem.setStartX(0);
			refElem.setStartY(0);
			refElem.setStartZ(-10.5);
			refElem.setDepth(1);
			// Add without auto-renaming (EnsureUniqueElementName) so we can reliably find/update it later
			refElem.ParentElement = null;
			rootElements.add(refElem);
			if (tree != null) {
				tree.addRootElement(refElem);
			}
			created = true;
		} else {
			refElem.setWidth(planeW);
			refElem.setHeight(planeH);
			refElem.setStartX(0);
			refElem.setStartY(0);
			refElem.setStartZ(-10.5);
			refElem.setDepth(1);
		}

		// Ensure this is a single-face plane (south face only)
		Face[] faces = refElem.getAllFaces();
		for (int i = 0; i < faces.length; i++) {
			faces[i].setEnabled(i == 2);
		}
		Face south = faces[2];
		south.setTextureCode(texCode);
		south.setStartU(0);
		south.setStartV(0);
		south.setRotation(0);
		// Keep auto UV so it follows the element size
		south.setAutoUVEnabled(true);
		refElem.updateUV();

		// Select the reference plane when first created, otherwise keep the current selection.
		if (created && tree != null) {
			tree.selectElement(refElem);
			SelectedElement = refElem;
		}

		ModelCreator.DidModify();
		ModelCreator.updateValues(null);
		if (tree != null) tree.updateUI();
		ModelCreator.changeHistory.endMultichangeHistoryState(this);

		return null;
	}

	private Element findRootElementByName(String name)
	{
		for (Element elem : rootElements) {
			if (name.equals(elem.getName())) return elem;
		}
		return null;
	}

	private static int[] pickBestAspectDims(int imgW, int imgH, int maxDim)
	{
		double target = (double)imgW / (double)imgH;
		int bestW = 16;
		int bestH = 16;
		double bestErr = Double.MAX_VALUE;

		for (int w = 1; w <= maxDim; w++) {
			for (int h = 1; h <= maxDim; h++) {
				double ratio = (double)w / (double)h;
				double err = Math.abs(ratio - target);
				if (err < bestErr) {
					bestErr = err;
					bestW = w;
					bestH = h;
				}
			}
		}

		// Prefer exact reductions when possible (e.g. 1920x1080 -> 16x9)
		int g = gcd(imgW, imgH);
		int rw = imgW / g;
		int rh = imgH / g;
		int scale = Math.max(rw, rh) == 0 ? 1 : (maxDim / Math.max(rw, rh));
		if (scale >= 1) {
			rw *= scale;
			rh *= scale;
			if (rw >= 1 && rh >= 1 && rw <= maxDim && rh <= maxDim) {
				bestW = rw;
				bestH = rh;
			}
		}

		return new int[] { bestW, bestH };
	}

	private static int gcd(int a, int b)
	{
		a = Math.abs(a);
		b = Math.abs(b);
		while (b != 0) {
			int t = a % b;
			a = b;
			b = t;
		}
		return a == 0 ? 1 : a;
	}

	public String loadReferenceTexture(String textureCode, File image, BooleanParam isNew, String projectType) throws IOException
	{
		String path = image.getAbsolutePath();
		String lc = path.toLowerCase(Locale.ROOT);
		String format = lc.endsWith(".jpg") || lc.endsWith(".jpeg") ? "JPG" : "PNG";

		Texture texture = null;
		FileInputStream is = new FileInputStream(image);
		try {
			texture = TextureLoader.getTexture(format, is);
		} catch (Throwable e) {
			// Fallback: decode through ImageIO, then encode as PNG and feed the PNG decoder.
			// This makes JPG support reliable even on setups where Slick's JPG loader is missing.
			try {
				BufferedImage bi = ImageIO.read(image);
				if (bi == null) {
					return "Unable to load this image. Supported: PNG, JPG.";
				}
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(bi, "png", baos);
				baos.flush();
				ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
				texture = TextureLoader.getTexture("PNG", bais);
			} catch (Throwable e2) {
				return "Unable to load this image. Supported: PNG, JPG.";
			}
		} finally {
			try { is.close(); } catch (Exception e) {}
		}

		texture.setTextureFilter(SGL.GL_NEAREST);
		ImageIcon icon = upscaleIcon(new ImageIcon(image.getAbsolutePath()), 256);

		if (textureCode == null || textureCode.length() == 0) {
			textureCode = "refimage";
		}

		TextureEntry prev = TexturesByCode.get(textureCode);
		if (prev != null) {
			try { prev.Dispose(); } catch (Exception e) {}
		}

		TexturesByCode.put(textureCode, new TextureEntry(textureCode, texture, icon, image.getAbsolutePath(), projectType));
		isNew.Value = prev == null;
		return null;
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
