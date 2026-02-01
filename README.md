# VSMC Feature / Experimental Branch (Hard Winter)

This repository is a **divergent branch** of **VS Model Creator (VSMC)** with many new features.  
It was created by (expanded existing VSMC with new features by) Pure Winter after working on various collaborative projects and gathering long term feedback to identify areas to improve VSMC functionality.

- Hard Winter edition is still in development with many more QOL features to come, see bottom for planned list.

---

## Table of Contents

- [Links](#links)
- [New Features in Hard Winter:](#new-features-in-hard-winter)
- [Carried over from bug-fixing and development-branch:](#carried-over-from-bug-fixing-and-development-branch)
- [Compatibility](#compatibility)
- [Planned Features](#planned-features)
- [Suggested but Unplanned features](#suggested-but-unplanned-features)
- [Suggestions?](#suggestions)
- [How to Use](#how-to-use)
- [Special Thanks](#special-thanks)

---

## Links

- **Upstream (main VSMC):** https://github.com/anegostudios/vsmodelcreator  
- **My vanilla bug-fixing branch:** https://github.com/Pure-Winter-hue/vsmodelcreator  
- **PW Development Branch:** https://github.com/Pure-Winter-hue/vsmodelcreator/tree/pw-development-branch  

---

## New Features in Hard Winter:

- **Animateable scale on every axis:** Update- added gizmo to make this easier in modeling/animation.
- **Keyframeable, instead of, fixed-origins**
- **Left click select model pieces or faces**
- **Control Changes:** A to select all. F to frame the viewport to your object. Pan with middle mouse or alt + right click.
- **Drag resize panels:** Saves your preferences between loads.
- **UV Multi Selection:** Marquee select UVs via holding shift + mouse drag, then multi move them all at once.
- **Model Marquee Selection:** You can now multi select elements in the viewport the same way, or in the hiearchy itself.
- **Floor Guard:** Gets location at the bottom of the model when this button is pressed. When moving the model, your selected model piece(s) cannot go below that location.
- **Free Move:** A circular handle appears in the move gizmo allow you to drag it around freely and move the selection free form.
- **Element Multi Rotate** You can now select multiple parent groups or individual pieces and rotate them by individual origin or center pivot. (Auto keyframes if in animation mode.)
- **Q Toggles between Move and Rotate**
- **UI:** Remembers user's prefs for personal window scales, gizmo thickness, etc. Renamed Cube to Modeling, face to UV, keyframe to Animation, p to Attachment Points and made their tabs large and clear.
- **Cube Creation:** Cubes are now created at the grid center.
- **Custom Grid Size:** Grid is now adjustable in width with toggleable visuals to aid in measurement vertically (positive or negative height) and on the floor.
- **Center:** Modeling tab, tools section now has buttons to center element(s) on the grid in 3 different ways. Works with hierarchies and multi select.
- **Autofix Z-Fighting:** Project toggle that offsets element by .001 to prevent z-fighting. Now renders to 3 decimal places by default.
- **Children Inherit Render Pass:** Project toggle that makes it so if you change a parent's render pass, everything below it inherits it! 1 Click transparency. :)
- **Grid Snap:** Set a custom 'move by' incriment.
- **Vertex Snap:** Snap elements to other elements when modeling.
- **Open Recent:** File / Open Recent will open a list of your last 12 files opened in the software, most recent first. (Also File -> Open recently saved.)
- **Mirror Animation and Modeling:** Model Pieces with identical names other than L and R like L_UpperWing and R_UpperWing will mirror your move/rotate/scale in animation and modeling to each part.
- **Import Reference Image** File / Setreference image.

---

## Carried over from bug-fixing and development-branch:

- **Hierarchy multi-select:** **Shift + Click** to select multiple elements (panels edit the lead selection). <- precursor to multi part editing
- **Mirror tool (geometry + UVs):**
  - Mirrors selected elements (supports hierarchies). Stacks UV's.
  - Uses the correct mirror axis for entity/block space.
  - **Auto L/R renaming:** e.g. `RFemur` → `LFemur`.
  - Optional **Mirror animations** toggle (mirrors keyframes for mirrored elements).
- **Face UV tools:** Face tab includes **Tools** with **Flip UV (L/R)** and **Flip UV (U/D)** for the selected face.
- **Duplicate Only This:** Select part, select frame, and duplicate to another frame (but not the ENTIRE MODEL'S KEYFRAME DATA) Only that part.
- **Move to Frame:** Type the frame number to jump to it!
- **Shortest Distance Rotation Buttons:** Click the buttons instead of saving, opening json, typing them, reopening.

---

## Compatibility

Models saved from Hard Winter are game-compatible, but not backwards VSMC compatible.  
Meaning if you save a model from Hard Winter, you will need to modify it on a Hard Winter version,  
due to the addition of scale/orientation keyframe support.

---

## Planned Features

- **Handles for scale tool**
- **Del deletes selection.**
- **Ctrl click removes an item from selection.**
- **H hides model selection.**
- **UV Snapping** Make stacking, sliding, and snapping UV groups way easier.
- **Solo Mode:** Manipulate a parent in modeling without affecting the children. Rotate, scale, move it around, children stay put.
- **IK**
- **Mirroring Tool Tuning** Add more 'use case' clauses for mirror's auto naming.
- **Curve-Array and Rot Snap** Make circles using existing objects and adjust them, rotation snapping with the new gizmo multi rotation tool.
- **Orient to View** Click Axis to center the view directly from the side, top down, etc.
- **Measuring** Markers on air cube boxes, not just floor grid and perhaps a vertex to vertex measuring tool.
- **Shading/Normal Revisit** Flip shading of objects that are shaded upside down. (Way, way less likely to happen thanks to the mirror tools but still, worth looking at since it was a huge problem in the past.)
- **UI Themes and Overhaul** Probably the last thing to arrive, that final 'make this a modern software' polish.

---

## Suggested but Unplanned features

- **Play Multiple Animations at Once** Reason: It would just be an approximation, it would be better to create a mod in game that simulates options you type (ovveride json) real time. A little in game 'animation preview studio' would be so nice. :)
- **Face Snapping** Difficult to impliment faces snapping flush to other faces and taking their rotation- may revisit after core is out.

---

## Suggestions?

- **Ping PureWinter in mods-general on main VS Discord.**

---

## How to Use

- Java Runtime (JRE) 8 aka 1.8 (Java 8).
- Explainer why the weird naming: https://stackoverflow.com/questions/53642402/why-do-i-see-open-jdk-1-8-instead-of-java-8
- Where to download: https://adoptium.net/temurin/releases/?version=8 | If you are VS dev using the Amazon 1.8 it should work. xD
- This is something Pure Winter works on free of charge as a love letter to the modding community, "just something to do."
- Because it's a passion hobby, I don't know everything! Like uh, how to compile this to install on your machine! But I plan to learn soon, when Hard Winter is actually done-done.
- To use it (the same way I do) just download free InteliJ, download the files from here, click File / Open and select the project base folder.

If it asks to exclude folders click yes, let it finish loading it all in, point to the JRE (once) if it needs to, then simply double click start and then press the green play arrow.

![InteliJ screenshot 1](https://i.imgur.com/WHcarGt.png)

![InteliJ screenshot 2](https://i.imgur.com/3ZEUosH.png)

---

## Special Thanks

- Mr Crayfish, Tyron, Blockbench Contributors, VSMC Github Contributors who came way before me and hammered out VSMC to begin with.
- Shintharo, Freakyuser396, Suavedoggo, Rivvion, Hyperion, Dana, Wulk | Either suggesting, streaming, lending use case and model examples, sharing expertise and struggles, that infinitely made this a better and more feature complete software.
