package at.vintagestory.modelcreator.gui.right.keyframes;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import at.vintagestory.modelcreator.ModelCreator;
import at.vintagestory.modelcreator.Project;
import at.vintagestory.modelcreator.interfaces.IValueUpdater;
import at.vintagestory.modelcreator.model.Animation;
import at.vintagestory.modelcreator.model.AnimationFrame;
import at.vintagestory.modelcreator.model.Element;
import at.vintagestory.modelcreator.model.AnimFrameElement;

public class RightKeyFramesPanel extends JPanel implements IValueUpdater
{
	private static final long serialVersionUID = 1L;
	
	private ElementKeyFrameOffsetPanel panelPosition;
	private ElementKeyFrameRotationPanel panelRotation;
	private ElementKeyFrameScalePanel panelScale;
	private ElementKeyFrameOriginPanel panelOrigin;
	private KeyFrameToolsPanel panelTools;
	JToggleButton btnPos;
	JToggleButton btnRot;
	JToggleButton btnScale;
	JToggleButton btnOrigin;
	
	public RightKeyFramesPanel()
	{
		initComponents();
		addComponents();
	}

	private void initComponents()
	{
		panelPosition = new ElementKeyFrameOffsetPanel(this);
		panelRotation = new ElementKeyFrameRotationPanel(this);
		panelScale = new ElementKeyFrameScalePanel(this);
		panelOrigin = new ElementKeyFrameOriginPanel(this);
		panelTools = new KeyFrameToolsPanel();
	}


	private void addComponents()
	{
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(0, 0, 0, 0);
		int row = 0;

		JPanel btnContainer = new JPanel(new GridLayout(1, 4, 4, 0));
		int rightWidth = at.vintagestory.modelcreator.ModelCreator.prefs.getInt("rightBarWidth", 260);
		btnContainer.setPreferredSize(new Dimension(Math.max(196, rightWidth - 24), 30));
		
		Font defaultFont = new Font("SansSerif", Font.BOLD, 11);
		

		btnRot = new JToggleButton("Rotation");
		btnRot.setFont(defaultFont);
		btnRot.addActionListener(a ->
		{
			ensureAnimationExists();
			
			ModelCreator.changeHistory.beginMultichangeHistoryState();
			
			java.util.List<Element> selected = ModelCreator.rightTopPanel.getSelectedElements();
			if (selected == null || selected.size() == 0) {
				Element lead = ModelCreator.rightTopPanel.getCurrentElement();
				selected = lead == null ? java.util.Collections.emptyList() : java.util.Collections.singletonList(lead);
			}
			
			Animation anim = ModelCreator.currentProject.SelectedAnimation;
			if (anim != null && anim.allFrames.size() > 0) {
				int currentFrame = anim.currentFrame;
				AnimationFrame aframe = anim.allFrames.get(currentFrame);
				boolean shift = (a.getModifiers() & ActionEvent.SHIFT_MASK) == 1;
				for (Element elem : selected) {
					if (elem == null) continue;
					AnimFrameElement frameElem = aframe.GetAnimFrameElementRec(elem);
					AnimFrameElement keyFrameElem = anim.ToggleRotation(elem, btnRot.isSelected());
					
					if (!shift && frameElem != null) {
						keyFrameElem.setRotationX(frameElem.getRotationX());
						keyFrameElem.setRotationY(frameElem.getRotationY());
						keyFrameElem.setRotationZ(frameElem.getRotationZ());
					}
					
					copyKeyFrameElemToBackdrop(elem);
					AnimFrameElement mirrorKf = ModelCreator.SyncMirrorKeyframe(keyFrameElem, selected);
					if (mirrorKf != null) { copyKeyFrameElemToBackdrop(mirrorKf.AnimatedElement); }
				}
			}
			
			ModelCreator.updateValues(btnRot);
			ModelCreator.changeHistory.endMultichangeHistoryState(ModelCreator.currentProject);
		});
		btnContainer.add(btnRot);
		
		
		btnPos = new JToggleButton("Position");
		btnPos.setFont(defaultFont);
		btnPos.addActionListener(a ->
		{
			ensureAnimationExists();
			
			ModelCreator.changeHistory.beginMultichangeHistoryState();
			
			Element elem = ModelCreator.rightTopPanel.getCurrentElement();
			Animation anim = ModelCreator.currentProject.SelectedAnimation;
			int currentFrame = anim.currentFrame;
			AnimationFrame aframe = anim.allFrames.get(currentFrame);
			AnimFrameElement frameElem = aframe.GetAnimFrameElementRec(elem);

			
			AnimFrameElement keyFrameElem = ModelCreator.currentProject.SelectedAnimation.TogglePosition(ModelCreator.rightTopPanel.getCurrentElement(), btnPos.isSelected());
			
			if ((a.getModifiers() & ActionEvent.SHIFT_MASK) != 1) {
				keyFrameElem.setOffsetX(frameElem.getOffsetX());
				keyFrameElem.setOffsetY(frameElem.getOffsetY());
				keyFrameElem.setOffsetZ(frameElem.getOffsetZ());
			}
			
			ModelCreator.updateValues(btnPos);
			ModelCreator.changeHistory.endMultichangeHistoryState(ModelCreator.currentProject);
			
			copyKeyFrameElemToBackdrop(elem);
			AnimFrameElement mirrorKf = ModelCreator.SyncMirrorKeyframe(keyFrameElem, null);
			if (mirrorKf != null) { copyKeyFrameElemToBackdrop(mirrorKf.AnimatedElement); }

		});
		
		btnContainer.add(btnPos);
		
		
		
		btnScale = new JToggleButton("Scale");
		btnScale.setFont(defaultFont);
		btnScale.addActionListener(a ->
		{
			ensureAnimationExists();
			
			ModelCreator.changeHistory.beginMultichangeHistoryState();
			
			Element elem = ModelCreator.rightTopPanel.getCurrentElement();
			
			Animation anim = ModelCreator.currentProject.SelectedAnimation;
			int currentFrame = anim.currentFrame;
			AnimationFrame aframe = anim.allFrames.get(currentFrame);
			AnimFrameElement frameElem = aframe.GetAnimFrameElementRec(elem);

			
			AnimFrameElement keyFrameElem = ModelCreator.currentProject.SelectedAnimation.ToggleStretch(ModelCreator.rightTopPanel.getCurrentElement(), btnScale.isSelected());
			
			if ((a.getModifiers() & ActionEvent.SHIFT_MASK) != 1) {
				keyFrameElem.setStretchX(frameElem.getStretchX());
				keyFrameElem.setStretchY(frameElem.getStretchY());
				keyFrameElem.setStretchZ(frameElem.getStretchZ());
			}
			
			ModelCreator.updateValues(btnScale);
			ModelCreator.changeHistory.endMultichangeHistoryState(ModelCreator.currentProject);
			
			copyKeyFrameElemToBackdrop(elem);
			AnimFrameElement mirrorKf = ModelCreator.SyncMirrorKeyframe(keyFrameElem, null);
			if (mirrorKf != null) { copyKeyFrameElemToBackdrop(mirrorKf.AnimatedElement); }

		});
		btnContainer.add(btnScale);

		btnOrigin = new JToggleButton("Origin");
		btnOrigin.setFont(defaultFont);
		btnOrigin.addActionListener(a ->
		{
			ensureAnimationExists();

			ModelCreator.changeHistory.beginMultichangeHistoryState();

			Element elem = ModelCreator.rightTopPanel.getCurrentElement();
			Animation anim = ModelCreator.currentProject.SelectedAnimation;
			int currentFrame = anim.currentFrame;
			AnimationFrame aframe = anim.allFrames.get(currentFrame);
			AnimFrameElement frameElem = aframe.GetAnimFrameElementRec(elem);

			AnimFrameElement keyFrameElem = ModelCreator.currentProject.SelectedAnimation.ToggleOrigin(elem, btnOrigin.isSelected());

			if ((a.getModifiers() & ActionEvent.SHIFT_MASK) != 1) {
				keyFrameElem.setOriginX(frameElem.getOriginX());
				keyFrameElem.setOriginY(frameElem.getOriginY());
				keyFrameElem.setOriginZ(frameElem.getOriginZ());
			}

			ModelCreator.updateValues(btnOrigin);
			ModelCreator.changeHistory.endMultichangeHistoryState(ModelCreator.currentProject);

			copyKeyFrameElemToBackdrop(elem);
			AnimFrameElement mirrorKf = ModelCreator.SyncMirrorKeyframe(keyFrameElem, null);
			if (mirrorKf != null) { copyKeyFrameElemToBackdrop(mirrorKf.AnimatedElement); }

		});
		btnContainer.add(btnOrigin);
		
		
		gbc.gridy = row++;
		gbc.weighty = 0;
		add(btnContainer, gbc);

		gbc.gridy = row++;
		add(panelRotation, gbc);

		gbc.gridy = row++;
		add(panelOrigin, gbc);

		gbc.gridy = row++;
		add(panelPosition, gbc);

		gbc.gridy = row++;
		add(panelScale, gbc);

		gbc.gridy = row++;
		gbc.weighty = 1;
		add(panelTools, gbc);
		
		updateValues(null);
	}
	


	public void copyKeyFrameElemToBackdrop(Element elem)
	{
		if (ModelCreator.currentBackdropProject == null) return;
		
		Animation bdanim = ModelCreator.currentBackdropProject.SelectedAnimation;
		if (bdanim == null) return;
		
		Animation anim = ModelCreator.currentProject.SelectedAnimation;
		AnimFrameElement mainFrameElem = anim.GetOrCreateKeyFrameElement(elem, anim.currentFrame);
		
		AnimFrameElement backFrameElem = bdanim.GetOrCreateKeyFrameElement(elem, anim.currentFrame);
		backFrameElem.setFrom(mainFrameElem);		
	}

	public void ensureAnimationExists()
	{
		if (ModelCreator.backdropAnimationsMode && ModelCreator.currentBackdropProject != null && ModelCreator.currentBackdropProject.SelectedAnimation != null && ModelCreator.currentProject.SelectedAnimation == null) {
			Animation anim = ModelCreator.currentBackdropProject.SelectedAnimation;
			
			Project curProj = ModelCreator.currentProject; 
			curProj.SelectedAnimation = curProj.findAnimation(anim.getCode());
			if (curProj.SelectedAnimation == null) {
				ModelCreator.ignoreDidModify++;
				curProj.Animations.add(curProj.SelectedAnimation = new Animation());
				
				curProj.SelectedAnimation.setCode(anim.getCode());
				curProj.SelectedAnimation.setName(anim.getName());
				curProj.SelectedAnimation.SetQuantityFrames(anim.GetQuantityFrames());
				ModelCreator.ignoreDidModify--;
			}
		}
		
	}

	@Override
	public void updateValues(JComponent byGuiElem)
	{
		updateFrame(byGuiElem);
	}
	
	public void updateFrame(JComponent byGuiElem) {
		if (ModelCreator.rightTopPanel == null) return;
		Element elem = ModelCreator.rightTopPanel.getCurrentElement();
		
		Project project = ModelCreator.CurrentAnimProject();
		
		boolean enabled = project.SelectedAnimation != null && elem != null && project.GetFrameCount() > 0;
		panelPosition.setAnimActive(ModelCreator.AnimationPlaying());
		panelRotation.setAnimActive(ModelCreator.AnimationPlaying());
		panelScale.setAnimActive(ModelCreator.AnimationPlaying());
		panelOrigin.setAnimActive(ModelCreator.AnimationPlaying());
		
		if (ModelCreator.AnimationPlaying()) {
			btnPos.setEnabled(false);
			btnRot.setEnabled(false);
			btnScale.setEnabled(false);
			btnOrigin.setEnabled(false);
			return;
		}
		
		ensureAnimationExists();
		
		Animation anim = ModelCreator.currentProject.SelectedAnimation;
		AnimFrameElement keyframeElem = enabled ? anim.GetKeyFrameElement(elem, anim.currentFrame) : null;
		
		panelPosition.enabled = enabled && keyframeElem != null && keyframeElem.PositionSet;
		panelRotation.enabled = enabled && keyframeElem != null && keyframeElem.RotationSet;
		panelScale.enabled = enabled && keyframeElem != null && keyframeElem.StretchSet;
		panelOrigin.enabled = enabled && keyframeElem != null && keyframeElem.OriginSet;
		
		btnPos.setSelected(panelPosition.enabled);
		btnRot.setSelected(panelRotation.enabled);
		btnScale.setSelected(panelScale.enabled);
		btnOrigin.setSelected(panelOrigin.enabled);
		
	
		panelRotation.toggleFields(keyframeElem != null ? getCurrentElement() : null, byGuiElem);
		panelOrigin.toggleFields(keyframeElem != null ? getCurrentElement() : null, byGuiElem);
		panelPosition.toggleFields(keyframeElem != null ? getCurrentElement() : null, byGuiElem);
		panelScale.toggleFields(keyframeElem != null ? getCurrentElement() : null, byGuiElem);
		
		btnPos.setEnabled(enabled);
		btnRot.setEnabled(enabled);
		btnScale.setEnabled(enabled);
		btnOrigin.setEnabled(enabled);
		panelTools.updateValues(byGuiElem);
	}
	

	public AnimFrameElement getCurrentElement()
	{
		if (ModelCreator.rightTopPanel == null) return null;
		
		ensureAnimationExists();
		Project project = ModelCreator.currentProject;
		
		Element elem = ModelCreator.rightTopPanel.getCurrentElement();
		if (elem == null || project.SelectedAnimation == null) return null;
		
		Animation anim = ModelCreator.currentProject.SelectedAnimation;
		return anim.GetKeyFrameElement(elem, anim.currentFrame);
	}

}
