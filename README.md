# VSMC Feature / Experimental Branch (Pure-Winter-hue)

This repository is a **divergent branch** of **VS Model Creator (VSMC)** with experimental features that may or may not end up in the upstream project.

- I use the **upstream/main VSMC** for the official baseline.
- I use my **bug-fixing branch** primarily for fixes intended for the main project.
- I use this **feature branch** to prototype and add new tools/features. 🙂

## Links

- **Upstream (main VSMC):** https://github.com/anegostudios/vsmodelcreator  
- **My bug-fixing branch:** https://github.com/Pure-Winter-hue/vsmodelcreator  

## What’s different in this branch?

This version of VSMC includes the following additions:

### ✅ Multiselect (Hierarchy)
Select multiple hierarchy elements using **Shift + Click**.

**How to use:**
1. Click an element in the hierarchy  
2. Hold **Shift**
3. Click additional elements to add them to the selection

> This is a precursor to multi-part editing without needing to parent everything.

### ✅ Mirror Button
Mirrors:
- **Everything currently multi-selected**, OR
- If a single parent is selected, **the selected item and all its children** (the full subtree)

### ✅ UV Tools
- **Neatly stacks UVs**
- **UV Flip buttons**:
  - Left / Right
  - Up

---

## Notes
This branch may drift from upstream over time as features are developed and tested. If you're looking for the canonical stable version, use the upstream repository linked above.
