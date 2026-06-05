# Herbalist Table Hitbox No-Mirror Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let selected custom `NHitBox` definitions opt out of legacy asymmetric-X mirroring, then set the herbalist table hitbox to the measured dimensions and center offset.

**Architecture:** Add one boolean flag to `NHitBox` with default legacy behavior. Add an `NHitBoxD(NHitBox, Coord2d, double)` constructor that uses the flag, and switch `CellsArray`/`NHitBoxD(Gob)` to this constructor. Only `gfx/terobjs/htable` disables mirroring.

**Tech Stack:** Java 8, Ant build, standalone `main()` regression tests under `test/`.

---

### Task 1: Add Failing No-Mirror Regression Tests

**Files:**
- Modify: `test/nurgling/pf/CellsArrayRasterizationTest.java`

- [ ] **Step 1: Add tests**

Add tests proving current asymmetric hitboxes still mirror by default and htable opts out to measured bounds.

- [ ] **Step 2: Verify red**

Run:

```bash
javac -cp "build/classes;etc/json-java.jar" -d build/test-classes test/nurgling/pf/CellsArrayRasterizationTest.java && java -cp "build/classes;build/test-classes;etc/json-java.jar" nurgling.pf.CellsArrayRasterizationTest
```

Expected: FAIL because no no-mirror constructor/flag exists yet.

### Task 2: Add NHitBox Mirror Control And Measured HTable Bounds

**Files:**
- Modify: `src/nurgling/NHitBox.java`
- Modify: `src/nurgling/pf/NHitBoxD.java`
- Modify: `src/nurgling/pf/CellsArray.java`

- [ ] **Step 1: Implement minimal flag plumbing**

Add `mirrorAsymmetric` to `NHitBox`, default it to `true`, add a constructor overload taking the flag, and add `NHitBoxD(NHitBox hb, Coord2d rc, double angle)`.

- [ ] **Step 2: Update htable hitbox**

Set `gfx/terobjs/htable` to:

```java
new NHitBox(new Coord2d(-2.965, -5.945), new Coord2d(3.965, 5.945), true, false)
```

- [ ] **Step 3: Verify green**

Run:

```bash
javac -cp "build/classes;etc/json-java.jar" -d build/classes src/nurgling/NHitBox.java src/nurgling/pf/NHitBoxD.java src/nurgling/pf/CellsArray.java && javac -cp "build/classes;etc/json-java.jar" -d build/test-classes test/nurgling/pf/CellsArrayRasterizationTest.java && java -cp "build/classes;build/test-classes;etc/json-java.jar" nurgling.pf.CellsArrayRasterizationTest
```

Expected: PASS with no output.

### Task 3: Compile And Commit

**Files:**
- Commit all files listed above plus this plan.

- [ ] **Step 1: Compile full client**

Run:

```bash
ant hafen-client
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Commit**

Run:

```bash
git add src/nurgling/NHitBox.java src/nurgling/pf/NHitBoxD.java src/nurgling/pf/CellsArray.java test/nurgling/pf/CellsArrayRasterizationTest.java docs/superpowers/plans/2026-06-05-htable-hitbox-no-mirror.md
git commit -m "fix: use measured herbalist table hitbox"
```
