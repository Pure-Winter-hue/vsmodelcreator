# VSMC Feature / Experimental Branch (Pure-Winter-hue)

This repository is a **divergent branch** of **VS Model Creator (VSMC)** with experimental features that may or may not end up in the upstream project.

- Use the **upstream/main VSMC** for the official baseline.
- I use my **bug-fixing branch** primarily for fixes for the upstream.
- I use this **feature branch** to prototype and add new tools/features. 🙂

## Links

- **Upstream (main VSMC):** https://github.com/anegostudios/vsmodelcreator  
- **My bug-fixing branch:** https://github.com/Pure-Winter-hue/vsmodelcreator  


## New Features in this branch
- **Hierarchy multi-select:** **Shift + Click** to select multiple elements (panels edit the lead selection). <- precursor to multi part editing
- **Mirror tool (geometry + UVs):**
  - Mirrors selected elements (supports hierarchies). Stacks UV's.
  - Uses the correct mirror axis for entity/block space.
  - **Auto L/R renaming:** e.g. `RFemur` → `LFemur`.
  - Optional **Mirror animations** toggle (mirrors keyframes for mirrored elements).
- **Face UV tools:** Face tab includes **Tools** with **Flip UV (L/R)** and **Flip UV (U/D)** for the selected face.
- **Duplicate Only This:** Select part, select frame, and duplicate to another frame (but not the ENTIRE MODEL'S KEYFRAME DATA) Only that part.
- **Move to Frame:** Type the frame number to jump to it!

---

## Notes
This branch may drift from upstream over time as features are developed and tested. If you're looking for the canonical stable version, use the upstream repository linked above.
