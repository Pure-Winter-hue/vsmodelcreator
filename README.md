# VSMC Feature / Experimental Branch (Pure-Winter-hue)

This repository is a **divergent branch** of **VS Model Creator (VSMC)** with experimental new features.

- Use the **upstream/main VSMC** for the official baseline.
- I use my **bug-fixing branch** primarily for fixes for the upstream.
- I use **feature branch** for stable candidate features for VSMC. 🙂 [pw-development-brnach]
-I use **this branch, Hard Winter Edition** to add completely new or experimental features!

## Links

- **Upstream (main VSMC):** https://github.com/anegostudios/vsmodelcreator  
- **My bug-fixing branch:** https://github.com/Pure-Winter-hue/vsmodelcreator  
- **PW Development Branch:** https://github.com/Pure-Winter-hue/vsmodelcreator/tree/pw-development-branch


## New Features in Hard Winter:
- **Animateable scale on every axis**
- **Keyframeable instead of fixed origins**
- **Left click select model pieces or faces:** Pan with middle mouse or alt + right click.
- **New left side scrollbar**: Supports middle mouse scroll, useful for the expanded features fitting on the panel.
- **Drag resize elements panel**

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

## Notes
Models saved from Hard Winter (
