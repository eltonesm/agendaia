---
name: sdd.fix
description: Fix validation errors and horizontal consistency issues across spec layers. Use when /sdd.check reports errors, specs are misaligned, or tasks don't match the technical spec.
model: opus
argument-hint: "[feature-name]"
---

### HOW TO READ THIS SKILL

When you see a block like this:

⛔ INVOKE TOOL (do not print this, CALL the tool):
AskUserQuestion(questions=[{...}])

This is a TOOL CALL you must execute, not content to display.

| WRONG | CORRECT |
|-------|---------|
| Bash(echo "1. Option A") | Directly call the AskUserQuestion tool |
| Print the JSON to terminal | Pass the parameters shown to the tool |

# Command: /sdd.fix

**Purpose**: Fix errors by analyzing output and propagating changes across ALL artifacts (specs, tasks, code) to maintain consistency.

---

## Usage

```bash
/sdd.fix                           # Interactive: paste error output
/sdd.fix "error message or output" # Direct: pass error inline
/sdd.fix --file ./error.log        # From file: read error from file
/sdd.fix --audio                   # Record error description via microphone
/sdd.fix --batch                   # Batch: one subagent per fix (prevents context exhaustion)
```

---

## Quick Help

> `/sdd.fix help` → Shows this summary

**Syntax**: `/sdd.fix [input] [flags]`

| Flag | Description |
|------|-------------|
| (none) | Interactive: paste error output |
| `"error message"` | Direct: pass error inline |
| `--file <path>` | Read error from file |
| `--audio` | Record error description via microphone |
| `--batch` | Batch mode: one subagent per fix (prevents context exhaustion) |
| `--layer <N>` | Target specific layer only |
| `--auto` | Auto-apply recommended fixes |

**Examples**:
```bash
/sdd.fix                    # Interactive mode
/sdd.fix "NullPointer..."   # Direct error message
/sdd.fix --file error.log   # From file
/sdd.fix --audio            # Describe the error via voice
/sdd.fix --batch            # Multiple fixes, each in fresh context
```

**See also**: `/sdd.help fix` for detailed documentation

---

CRITICAL: USER INTERACTION RULES
When this skill shows JSON for AskUserQuestion, you MUST:
  1. CALL the AskUserQuestion TOOL with that exact JSON
  2. DO NOT print options using Bash (no echo, cat, printf)
  3. DO NOT ask "Which option?" as text
  4. Tables marked "REFERENCE ONLY" are for docs - do NOT print


## Context Safety Protocol ⭐ v1.7.0

> **ROOT CAUSE OF SHALLOW FIXES**: When multiple `/sdd.fix` calls accumulate in one session, context fills to 100%. An agent with full context loses depth — it stops generating hypotheses, skips evidence gathering, and resolves superficially. This section prevents that.

### Rule: Check Context Before EVERY Fix

**BEFORE Step 0**, invoke the context guard:

```
Skill("context-guardian")
```

Then act on the result:

| Context Level | Action |
|---------------|--------|
| 0–50% | ✅ Proceed normally |
| 51–70% | ⚠️ Warn user: "Context is at X%. Investigation may be limited. Consider `/sdd.fix` in a fresh session for complex bugs." Proceed. |
| 71–89% | 🔴 Warn user: "Context at X% — deep investigation WILL be compromised. Strongly recommend fresh session." Ask to continue or abort. |
| 90–100% | ⛔ BLOCK: "Context exhausted. Fix quality cannot be guaranteed. Start a new Claude session." Do NOT proceed. |

### Rule: --batch MUST Use Subagents

The `--batch` flag MUST spawn one subagent per fix. Processing multiple fixes inline in the same session is PROHIBITED because:
- Each fix reads many files → context accumulates fast
- By fix #3, context is typically >70% → depth drops
- Fixes #4+ are often resolved superficially or incorrectly

See [--batch Flag Detection](#--batch-flag-detection-v170) for the subagent implementation.

---

## When to Use /sdd.fix (Decision Guide)

> **Key Insight**: `/sdd.fix` is for errors that reveal **gaps in your specifications**, not for simple code typos.

### Decision Tree: Which Command Should I Use?

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    ERROR OCCURRED - WHAT SHOULD I DO?                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Q1: Is this a spec/task/code CONSISTENCY issue?                             │
│      (e.g., code does something not in specs)                                │
│      │                                                                       │
│      ├── YES ──► /sdd.check --sync                                          │
│      │           Detects drift between layers, proposes fixes                │
│      │                                                                       │
│      └── NO ──► Q2: Is there an ERROR OUTPUT (test fail, crash, etc.)?      │
│                     │                                                        │
│                     ├── YES ──► /sdd.fix                                    │
│                     │           Analyzes error, propagates fix horizontally  │
│                     │                                                        │
│                     └── NO ──► Manual fix or /sdd.build                     │
│                                 Just make the code change                    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### /sdd.fix vs /sdd.check --sync

| Aspect | `/sdd.fix` | `/sdd.check --sync` |
|--------|-------------|----------------------|
| **Trigger** | Error output (test fail, crash) | Suspected inconsistency |
| **Input** | Error message or output | None (scans all layers) |
| **Primary action** | Diagnose root cause, classify problem | Detect drift between layers |
| **Output** | Fix proposal for affected layers | Inconsistency report with fixes |
| **When to use** | "Tests are failing, what's wrong?" | "Did I forget to update the spec?" |

### /sdd.fix vs Manual Code Fix

| Scenario | Use `/sdd.fix` | Fix Manually |
|----------|-----------------|--------------|
| Simple typo in code | ❌ | ✅ |
| Missing semicolon | ❌ | ✅ |
| Test fails due to new behavior not in specs | ✅ | ❌ |
| API returns wrong error code | ✅ | ❌ |
| Edge case reveals missing requirement | ✅ | ❌ |
| NullPointerException in existing logic | ⚠️ Check if it reveals a spec gap | ✅ if pure code bug |

### When NOT to Use /sdd.fix

| Error Type | Why Not /sdd.fix | What To Do Instead |
|------------|-------------------|-------------------|
| Configuration/env errors | Not a spec issue | Fix config, redeploy |
| Infrastructure issues | Not a code issue | Check infra, ops |
| Typos/syntax errors | No spec impact | Fix directly |
| Import/dependency errors | Build issue | Fix dependencies |
| Authentication failures | Runtime issue | Check credentials |

---

## Core Principle: Horizontal Consistency

**CRITICAL**: `/sdd.fix` is NOT just a code fix. It ensures the ENTIRE solution remains consistent:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     HORIZONTAL FIX PROPAGATION                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Error Found ──► Analyze Impact ──► Update ALL Affected Layers             │
│                                                                             │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐   ┌─────────────┐    │
│  │ FUNCTIONAL  │   │  TECHNICAL  │   │    TASKS    │   │    CODE     │    │
│  │    SPEC     │◄──│    SPEC     │◄──│             │◄──│             │    │
│  └─────────────┘   └─────────────┘   └─────────────┘   └─────────────┘    │
│        │                 │                 │                 │             │
│        ▼                 ▼                 ▼                 ▼             │
│  [Update if      [Update API       [Update task      [Fix code]           │
│   requirement     contracts,        descriptions,                          │
│   was wrong]      data model]       add new tasks]                        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### ⚠️ Phase-Aware Constraint (v1.6.0)

> **IMPORTANT**: You can only fix layers that EXIST at the current phase. See [Step 0: Detect Current Phase](#step-0-detect-current-phase-mandatory--v160) for the full reference table.

### Terminology: Horizontal vs Bidirectional

| Term | What It Means | When It Applies |
|------|---------------|-----------------|
| **Horizontal Consistency** | All layers tell the same story | During fix propagation (Step 5) |
| **Horizontal Fix Propagation** | Update ALL affected layers atomically | When applying fixes |
| **Bidirectional Consistency** | Verify in BOTH directions | During verification (Step 7) |

**Horizontal = Propagation** (Action)
- "I changed code, so I update specs+tasks too"
- Direction: Code → Tasks → Technical → Functional (backwards propagation)
- Goal: Ensure all layers reflect the fix

**Bidirectional = Verification** (Check)
- "Does Spec→Code match AND does Code→Spec match?"
- Direction 1: Specs → Code (is everything implemented?)
- Direction 2: Code → Specs (is everything documented?)
- Goal: Catch any drift in either direction

---

## Subagent Delegation

> **💡 RECOMMENDED**: See [warning-hierarchy.md](../standards/warning-hierarchy.md#subagent-delegation) for the central principle.
> For complex bugs, delegate analysis to specialized subagents.

> **Lazy-loaded**: During problem classification/delegation, Read `references/classification-guide.md` for decision trees and classification rules.

### Decision Tree: When to Delegate

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    SHOULD I DELEGATE TO A SUBAGENT?                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Q1: Is the bug OBVIOUS (typo, missing import, clear logic error)?          │
│      │                                                                       │
│      ├── YES ──► Fix directly (no subagent needed)                          │
│      │                                                                       │
│      └── NO ──► Q2: Is this about SPEC/CODE INCONSISTENCY?                  │
│                     (e.g., code does X but spec says Y)                      │
│                     │                                                        │
│                     ├── YES ──► Use sdd-layer-analyzer                      │
│                     │           Purpose: Detect drift, align layers          │
│                     │                                                        │
│                     └── NO ──► Q3: Is this a DEEP TECHNICAL BUG?            │
│                                    (race condition, memory leak, perf)       │
│                                    │                                         │
│                                    ├── YES ──► Use sdd-debugger             │
│                                    │           Purpose: Deep root cause      │
│                                    │                                         │
│                                    └── NO ──► Fix with /sdd.fix directly   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Subagent Reference

| Subagent | When to Use | Example Error |
|----------|-------------|---------------|
| **sdd-debugger** | Race conditions, deadlocks | "Request hangs intermittently" |
| **sdd-debugger** | Memory leaks | "OOM after running for 2 hours" |
| **sdd-debugger** | Performance regressions | "API now takes 10s instead of 100ms" |
| **sdd-debugger** | Subtle logic errors | "Wrong result only for edge case X" |
| **sdd-layer-analyzer** | Spec/code mismatch | "Code returns 400 but spec says 422" |
| **sdd-layer-analyzer** | Undocumented features | "This API parameter isn't in specs" |
| **sdd-layer-analyzer** | Missing tasks | "This code exists but no task covers it" |

### Invocation

```
Task(subagent_type="sdd-debugger", prompt="Analyze: [error details]")
Task(subagent_type="sdd-layer-analyzer", prompt="Check: [feature name]")
```

---

## Workflow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      /sdd.fix WORKFLOW (v1.7.0)                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  -1. MULTI-ISSUE DETECTION ⭐ v1.7.0                                        │
│      └── Count how many distinct issues were passed                        │
│      └── IF N > 1: STOP inline processing, switch to SUBAGENT MODE        │
│      └── IF N = 1: run context guard (Skill "context-guardian")            │
│                    51-70%: warn; 71-89%: ask abort; ≥90%: BLOCK            │
│                                                                             │
│  0. DETECT CURRENT PHASE (MANDATORY) ⭐ v1.6.0                              │
│     └── Read meta.md to determine current phase                            │
│     └── Only layers that EXIST can be assessed/updated                     │
│     └── Scan for recurring fix patterns (fixes-log.md)                     │
│                                                                             │
│  1. RECEIVE ERROR                                                           │
│     └── User provides error output                                          │
│                                                                             │
│  1.5. CLASSIFY PROBLEM (MANDATORY)                                          │
│       └── Determine: FEATURE_GAP / DESIGN_FLAW / MISSING_TASK / IMPL_BUG   │
│       └── This determines which layers MUST be updated                      │
│       └── ⚠️ Only consider layers that EXIST at current phase              │
│                                                                             │
│  2. DEEP INVESTIGATION ⭐ v1.7.0                                            │
│     └── Generate ≥2 hypotheses for the root cause                         │
│     └── Gather evidence for each hypothesis (read/grep/run tests)          │
│     └── Eliminate hypotheses with evidence                                 │
│     └── State confirmed root cause with confidence %                       │
│                                                                             │
│  3. ASSESS IMPACT ACROSS LAYERS                                             │
│     ├── Does functional spec need update? (requirement was wrong?)          │
│     ├── Does technical spec need update? (API/data model incorrect?)        │
│     ├── Do tasks need update? (missing task? wrong task?)                   │
│     └── Does code need fix? (implementation bug?)                           │
│                                                                             │
│  3.5. ANTI-SHORTCUT VERIFICATION (MANDATORY)                                │
│       └── For each "No Change" declaration, provide EVIDENCE (quote)        │
│       └── No evidence = must update the layer                               │
│                                                                             │
│  4. PROPOSE CHANGES (ALL LAYERS)                                            │
│     └── Show what changes in each artifact                                  │
│                                                                             │
│  4.5. IMPLEMENTATION PLAN ⭐ v1.7.0                                         │
│       └── Ordered list of changes with dependencies                        │
│       └── Test checkpoints between steps                                   │
│       └── Rollback strategy per step                                       │
│                                                                             │
│  4.6. CREATE FIX RECORD DRAFT ⭐ v1.7.0                                     │
│       └── After full diagnosis (root cause + impact + plan)               │
│       └── Create FIX-NNN-DATE.md [IN_PROGRESS] with all conclusions       │
│       └── Append row to fixes-log.md — BEFORE first code change           │
│                                                                             │
│  4.7. SPECIFICATION TESTS (RED-GREEN + MUTATION) ⭐ v1.7.0                 │
│       └── Write tests from spec (NOT from implementation)                 │
│       └── Phase 2: run before fix → must FAIL (red proof)                 │
│       └── Phase 3: apply mutation → tests must catch it                   │
│       └── Phase 4 gate: GREEN/YELLOW/RED — recorded in Fix Record         │
│                                                                             │
│  5. APPLY FIX HORIZONTALLY                                                  │
│     └── Update all affected artifacts atomically                            │
│                                                                             │
│  6. RE-RUN TESTS                                                            │
│     └── Execute tests to confirm code fix works                             │
│                                                                             │
│  6.5. CODE REVIEW (MANDATORY)                                               │
│       └── Call code review tool, fix ALL findings (including minor)        │
│                                                                             │
│  7. BIDIRECTIONAL CONSISTENCY CHECK (MANDATORY)                             │
│     └── Verify in BOTH directions:                                          │
│         ├── Direction 1: Specs → Code (is everything implemented?)          │
│         └── Direction 2: Code → Specs (is everything documented?)           │
│                                                                             │
│  8. FINALIZE FIX RECORD ⭐ v1.7.0                                           │
│     └── Update FIX-NNN-DATE.md: add result, layers, tests → RESOLVED      │
│     └── Update fixes-log.md row: IN_PROGRESS → RESOLVED                   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**New in v1.5.0**: Steps 1.5, 3.5, and bidirectional Step 7 address the observed issue where agents fix code without updating specs.

---

## Execution Flow

### Step -1: Multi-Issue Detection ⭐ v1.7.0

> **CRITICAL**: This step runs BEFORE anything else. It is the architectural guard that prevents context exhaustion.

#### Why This Exists

When an agent processes multiple fixes inline, context fills after fix #2–3. Subsequent fixes get progressively less investigation depth — hypothesis generation stops, evidence quotes are skipped, fix records aren't created. The result is **superficial fixes that mask the real issue**.

The fix: each issue MUST run in a fresh context via a subagent.

#### Execution

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🛡️ MULTI-ISSUE DETECTION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Count how many distinct issues/fixes are in the input:

IF N > 1:
  ⛔ STOP. Do NOT process any fix inline.
  ⛔ Do NOT use TodoWrite to list issues and process them sequentially in this session.
  ⛔ Do NOT start investigating Issue 1 in this context.

  The ONLY correct action is:

    for each issue in issues (sequentially, one at a time):
      Task(
        subagent_type="general-purpose",
        description="Fix: [short name]",
        prompt="Working dir: {DIR}\nInvoke Skill('sdd.fix') for this single issue:\n{ISSUE_DESCRIPTION}\nReport: classification, root cause, layers, fix record path, status."
      )
      → wait for Task result before spawning next

  After all Tasks complete → show consolidated batch summary.

  ❌ WRONG: TodoWrite with N items + process each inline in this session
  ✅ CORRECT: N × Task() calls, one per fix, each subagent gets fresh context
  → Full template: see [--batch Flag Detection](#--batch-flag-detection-v170)

IF N = 1:
  Check context: Skill("context-guardian")
  → IF context 51–70%: warn user: "Context at X%. Investigation may be limited. Consider a fresh session for complex bugs." Proceed.
  → IF context 71–89%: warn user: "Context at X% — deep investigation WILL be compromised." Ask to abort or continue with reduced depth.
  → IF context ≥ 90%: BLOCK, refuse to proceed: "Context exhausted. Start a new session."
  → IF context ≤ 50%: continue to Step 0
```

#### What Counts as "Multiple Issues"?

| Input Pattern | Count |
|---------------|-------|
| `"Fix X"` (single string) | 1 issue |
| `"Fix X" / "Fix Y"` (two separate descriptions) | 2 issues |
| A numbered list: `1. Fix X\n2. Fix Y\n3. Fix Z` | 3 issues |
| Multiple `/sdd.fix` calls in one message | N calls = N issues |
| `--batch` flag | Triggers subagent mode immediately |

> **Rule**: When in doubt, treat as multiple issues and use subagent mode. A single subagent for one issue costs nothing extra; inline processing of many issues costs quality.

---

### Step 0: Detect Current Phase (MANDATORY) ⭐ v1.6.0

> **CRITICAL**: Before analyzing any error, you MUST determine the current phase to know which layers EXIST.

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 PHASE DETECTION + PATTERN SCAN
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Reading meta.md to determine current phase...

Current Phase: [functional / technical / tasks / implementation]
```

After detecting the phase, **scan for recurring fix patterns** ⭐ v1.7.0:

```
IF fixes-log.md exists (at sdd/wip/{feature}/fixes/ OR meli/fixes/):

  1. Count fixes per file in the last 30 days:
     - grep "Files" in all FIX-*.md files
     - group by filename, count occurrences

  2. IF any file has ≥3 fixes in 30 days:
     ⚠️  RECURRING FIX WARNING
     ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
     File [X] has been fixed N times in the last 30 days.
     This may indicate a structural design issue.
     Consider adding a DEBT item to sdd/backlog.md after this fix.
     ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  3. IF the same root cause pattern appears ≥2 times:
     (compare "Root Cause (summary)" column in fixes-log.md)
     ⚠️  SAME ROOT CAUSE SEEN BEFORE
     Last occurrence: FIX-NNN (DATE) — [summary]
     This fix may not address the underlying issue.
```

#### Layers Available by Phase

| Current Phase | Functional Spec | Technical Spec | Tasks | Code |
|---------------|-----------------|----------------|-------|------|
| **functional** | ✅ EXISTS | ❌ NOT YET | ❌ NOT YET | ❌ NOT YET |
| **technical** | ✅ EXISTS | ✅ EXISTS | ❌ NOT YET | ❌ NOT YET |
| **tasks** | ✅ EXISTS | ✅ EXISTS | ✅ EXISTS | ❌ NOT YET |
| **implementation** | ✅ EXISTS | ✅ EXISTS | ✅ EXISTS | ✅ EXISTS |

#### Phase-Aware Assessment Rules

**IF current phase = functional**:
- Only assess: Functional Spec
- DO NOT mention: Technical Spec, Tasks, Code (they don't exist yet)
- Fix can only update functional spec

**IF current phase = technical**:
- Only assess: Functional Spec, Technical Spec
- DO NOT mention: Tasks, Code (they don't exist yet)
- Fix can update functional and/or technical specs

**IF current phase = tasks**:
- Only assess: Functional Spec, Technical Spec, Tasks
- DO NOT mention: Code (it doesn't exist yet)
- Fix can update specs and/or tasks

**IF current phase = implementation**:
- Assess ALL layers: Functional, Technical, Tasks, Code
- Full horizontal fix propagation available

> **WARNING**: Suggesting updates to layers that don't exist yet is INCORRECT and confusing to users.

---

### Step 1: Receive Error Input

```
AI: 🔧 Fix Mode
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Current Phase: [detected phase]
Available Layers: [list of layers that exist]

Paste the error output or describe the issue:

> [User pastes error]
```

---

### Step 1.5: Problem Classification (MANDATORY)

> **CRITICAL**: Before ANY analysis, classify the problem type. This determines which layers MUST be updated.

> **Lazy-loaded**: During problem classification/delegation, Read `references/classification-guide.md` for decision trees and classification rules.

---

## Plan Mode for Complex Bugs ⭐ v1.2.7

> **ENABLED BY DEFAULT**: For complex bugs (DESIGN_FLAW, FEATURE_GAP), Plan Mode helps
> plan investigation strategy before diving in.

### Platform Availability

| Platform | Plan Mode Available |
|----------|---------------------|
| Claude Code (CLI) | ✅ Yes (`EnterPlanMode`/`ExitPlanMode`) |
| Cursor | ❌ No (use fallback) |
| Windsurf | ❌ No (use fallback) |
| MeliCli | ❌ No (use fallback) |

### Configuration

Plan Mode for `/sdd.fix` is **enabled by default**. Disable via `PROJECT.md`:

```yaml
# In PROJECT.md or sdd-kit/framework/config.yaml
plan_mode:
  fix_complex_bugs: true  # Default: true (enabled)
```

### Trigger Conditions

Enter Plan Mode when **ANY** of these are true:
- Problem classified as `DESIGN_FLAW` or `FEATURE_GAP`
- Error spans multiple components (3+ files affected)
- Previous fix attempts failed (2+ attempts)
- Symptoms indicate systemic issues (performance, concurrency, race conditions)

> **Lazy-loaded**: During impact assessment/plan mode, Read `references/fix-templates.md` for assessment templates and workflow safeguards.

### Plan Mode Flow

```
AFTER Step 1.5 (Classification), BEFORE Step 2 (Root Cause):

  IF config.plan_mode.fix_complex_bugs AND complex_bug_detected:

    IF EnterPlanMode available (Claude Code):
      1. EnterPlanMode()

      2. EXPLORE: Map affected components
         - Identify all files involved in the error path
         - Collect evidence (logs, stack traces, test failures)
         - Document current behavior vs expected behavior

      3. DESIGN: Investigation strategy
         - Hypothesis priority list
         - Test sequence to validate hypotheses
         - Delegate decision (inline vs sdd-debugger for deep issues)

      4. Present plan to user
         - Show investigation strategy
         - List hypotheses in priority order

      5. ExitPlanMode() (user approves strategy)

    ELSE (Fallback for non-Claude Code):
      1. EXPLORE: Same read-only exploration
      2. DESIGN: Same investigation planning
      3. Display plan inline in chat
      4. AskUserQuestion: "Approve this investigation approach?"
         - Options: "Approve", "Modify", "Skip planning"

    6. Continue with Step 2 using approved strategy
```

### Post-Plan Mode Safeguards (MANDATORY)

```
┌─────────────────────────────────────────────────────────────────────┐
│  POST-PLANMODE SAFEGUARDS (MANDATORY)                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  AFTER ExitPlanMode, BEFORE applying any fix:                        │
│                                                                      │
│  1. Plan output is READ-ONLY guidance (not executable)               │
│     → Plan Mode exploration does NOT modify files                    │
│     → Only implementation phase modifies files                       │
│                                                                      │
│  2. Any fix MUST follow horizontal consistency:                      │
│     → IF fix touches code → check if specs need update               │
│     → IF fix touches specs → check if code needs update              │
│     → ALWAYS run /sdd.check --sync after fixes                      │
│                                                                      │
│  3. Artifact validation before marking fix complete:                 │
│     → specs are internally consistent                                │
│     → code matches specs                                             │
│     → tasks.json reflects current state                              │
│     → meta.md is accurate                                            │
│                                                                      │
│  4. IF Plan Mode suggests breaking change:                           │
│     → Show warning to user                                           │
│     → Explain which artifacts will be affected                       │
│     → Get explicit approval before proceeding                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### When to Skip Plan Mode

Skip Plan Mode for:
- `IMPLEMENTATION_BUG` (pure code bug, no spec impact)
- `MISSING_TASK` (clear scope, just add task)
- Simple typos, missing imports, obvious logic errors
- User explicitly requests `--auto` flag

---

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔍 PROBLEM CLASSIFICATION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Analyzing problem type...

┌─────────────────────────────────────────────────────────────────┐
│                    CLASSIFICATION DECISION TREE                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Q1: Does the fix require NEW FUNCTIONALITY not in specs?       │
│      │                                                           │
│      ├── YES ──► FEATURE GAP                                     │
│      │           Must update: Functional + Technical + Tasks     │
│      │                                                           │
│      └── NO ──► Q2: Does the fix require changing HOW           │
│                     something works (API, data model)?           │
│                     │                                            │
│                     ├── YES ──► DESIGN FLAW                      │
│                     │           Must update: Technical + Tasks   │
│                     │                                            │
│                     └── NO ──► Q3: Does the fix add work        │
│                                    not captured in tasks?        │
│                                    │                             │
│                                    ├── YES ──► MISSING TASK      │
│                                    │           Must update: Tasks│
│                                    │                             │
│                                    └── NO ──► IMPLEMENTATION BUG │
│                                                Code only         │
└─────────────────────────────────────────────────────────────────┘

**Classification Result**: [FEATURE_GAP / DESIGN_FLAW / MISSING_TASK / IMPLEMENTATION_BUG]

**Current Phase**: [from Step 0]

**Layers that MUST be updated** (only layers that EXIST at current phase):
- [ ] Functional Spec: [Yes/No - with reason] ← Always available
- [ ] Technical Spec: [Yes/No - with reason] ← Only if phase ≥ technical
- [ ] Tasks: [Yes/No - with reason] ← Only if phase ≥ tasks
- [ ] Code: [Yes/No - with reason] ← Only if phase = implementation

⚠️ **Phase Constraint**: If current phase is "technical", Tasks and Code sections above should show "N/A - layer does not exist yet"
```

#### Classification Examples

| Problem Description | Classification | Layers to Update |
|---------------------|----------------|------------------|
| "JaCoCo reports not being generated" | **FEATURE_GAP** | Functional + Technical + Tasks + Code |
| "Severity breakdown not showing per-type counts" | **FEATURE_GAP** | Functional + Technical + Tasks + Code |
| "API returns 500 instead of 400 for validation" | **DESIGN_FLAW** | Technical + Tasks + Code |
| "Missing error handling for null input" | **DESIGN_FLAW** | Technical + Tasks + Code |
| "Test coverage not reaching threshold" | **MISSING_TASK** | Tasks + Code |
| "NullPointerException in line 45" | **IMPLEMENTATION_BUG** | Code only |

> **WARNING**: If you classify as IMPLEMENTATION_BUG but the fix adds new behavior, you have misclassified. Re-evaluate.

---

### Step 2: Deep Investigation ⭐ v1.7.0

> **CRITICAL**: Hypothesis-driven investigation. Never jump to a single conclusion — always generate alternatives and eliminate with evidence.

> After Steps 2.1–2.3, continue through Steps 3–4.5 (full diagnosis). The fix record draft is created at **Step 4.6** — after all analysis, before the first code change.

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔬 DEEP INVESTIGATION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

**Error Type**: [Runtime/Compilation/Test/Integration]
**Location**: [File:Line if available]
**Symptoms**: [Observable behavior vs expected behavior]
```

#### Step 2.1: Generate Hypotheses

Generate at least **2 alternative hypotheses** before reading any code:

```markdown
### Hypotheses

| # | Hypothesis | What to Look For | Confidence (initial) |
|---|------------|-----------------|----------------------|
| H1 | [Possible cause A] | [File/function/pattern to check] | [Low/Medium/High] |
| H2 | [Alternative cause B] | [File/function/pattern to check] | [Low/Medium/High] |
| H3 | [Another alternative] | [File/function/pattern to check] | [Low/Medium/High] |
```

**Minimum 2 hypotheses required.** If you can only think of one, force yourself to ask:
- "What else could produce this symptom?"
- "Could this be a data problem instead of a logic problem?"
- "Could this be a timing/order problem?"

#### Step 2.2: Gather Evidence Per Hypothesis

For each hypothesis, actively read/grep/run to find evidence:

```markdown
### Evidence Gathering

**H1 — [Hypothesis A]**
- Read: [file path] → [what I found / what I expected to find]
- Grep: [pattern] → [result]
- Conclusion: [supports / eliminates H1]

**H2 — [Hypothesis B]**
- Read: [file path] → [what I found]
- Conclusion: [supports / eliminates H2]
```

#### Step 2.3: Eliminate and Confirm

```markdown
### Elimination

| Hypothesis | Status | Evidence |
|------------|--------|---------|
| H1 | ❌ ELIMINATED | "[Quote from code/log that rules it out]" |
| H2 | ✅ CONFIRMED | "[Quote from code/log that proves it]" |

### Root Cause Statement

**Root Cause**: [Precise statement of what is broken]
**Confidence**: [%]
**Evidence Chain**: [H2 confirmed because X, which causes Y, which produces the observed error Z]
```

#### Root Cause Category (after investigation)

| Category | Description | Layers Affected |
|----------|-------------|-----------------|
| **Requirement Gap** | Spec didn't account for this case | Functional → Technical → Tasks → Code |
| **Design Flaw** | Technical approach was wrong | Technical → Tasks → Code |
| **Missing Task** | Work wasn't planned | Tasks → Code |
| **Implementation Bug** | Code error only | Code only |

---

### Step 3: Assess Impact Across ALL Layers

> **Lazy-loaded**: During impact assessment/plan mode, Read `references/fix-templates.md` for assessment templates and workflow safeguards.

```markdown
## Impact Assessment

### 📋 Functional Spec Impact
**Status**: [No Change / Update Required]

If update required:
- **Section**: [Which section]
- **Issue**: [What was missing/wrong]
- **Change**: [What to add/modify]

### 🔧 Technical Spec Impact
**Status**: [No Change / Update Required]

If update required:
- **API Changes**: [Endpoints affected]
- **Data Model Changes**: [Schema updates]
- **Architecture Changes**: [Component updates]

### 📝 Tasks Impact
**Status**: [No Change / Update Required]

If update required:
- **Modified Tasks**: [TASK-XXX: what changes]
- **New Tasks**: [TASK-NEW: description]
- **Removed Tasks**: [TASK-XXX: why removed]

### 💻 Code Impact
**Status**: [Update Required]
- **Files**: [List of files to modify]
- **Changes**: [Description of code changes]
```

---

### Step 3.5: Anti-Shortcut Protocol (MANDATORY)

> **CRITICAL**: Before declaring "No Change" for ANY layer, you MUST complete this verification.

#### The "No Change" Trap

**PROBLEM OBSERVED**: Agents declare "No Change" without verifying, leading to:
- Code has features not documented in specs
- Tasks don't reflect actual work done
- Accumulated inconsistencies across multiple fixes

#### Mandatory Verification Before "No Change"

For EACH layer where you plan to declare "No Change", you MUST:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🛡️ ANTI-SHORTCUT VERIFICATION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

For each "No Change" declaration, verify:

📋 FUNCTIONAL SPEC - Declaring "No Change"?
   □ I READ the relevant section of functional spec
   □ The fix does NOT add new user-facing behavior
   □ The fix does NOT change acceptance criteria
   □ Quote from spec that covers this: "[quote]"

🔧 TECHNICAL SPEC - Declaring "No Change"?
   □ I READ the relevant section of technical spec
   □ The fix does NOT change API contracts
   □ The fix does NOT add new data models
   □ The fix does NOT change external tool integration
   □ Quote from spec that covers this: "[quote]"

📝 TASKS - Declaring "No Change"?
   □ I READ the relevant tasks
   □ The fix does NOT add new acceptance criteria
   □ The task description already covers this fix
   □ Quote from task that covers this: "[quote]"
```

#### Evidence Requirement

**You MUST provide a quote** from each spec/task when declaring "No Change":

```markdown
### 📋 Functional Spec
**Status**: No Change
**Evidence**: Section "Error Handling" already states:
> "System displays user-friendly error messages for all validation failures"
This covers the email validation error we're fixing.

### 📝 Tasks
**Status**: No Change
**Evidence**: TASK-005 acceptance criteria already includes:
> "Validate all user input fields before submission"
This covers email validation.
```

#### Red Flags That Indicate Spec Update Needed

| If the fix involves... | Then you MUST update... |
|------------------------|-------------------------|
| Adding a new command/trigger | Functional Spec |
| Adding new output/display | Functional Spec |
| Changing user-visible behavior | Functional Spec |
| Adding API parameters | Technical Spec |
| Adding new models/entities | Technical Spec |
| Adding external tool calls | Technical Spec |
| Adding new acceptance criteria | Tasks |
| Changing task scope | Tasks |

---

### Step 4: Propose Horizontal Fix

```markdown
## Proposed Fix (All Layers)

### Option A: [Fix Description] (Recommended)

**1. Functional Spec Changes** (`1-functional/spec.md`):
```diff
### Acceptance Criteria
- User can submit form with valid data
+ - User can submit form with valid data
+ - System validates email format before submission
+ - Invalid email shows error message
```

**2. Technical Spec Changes** (`2-technical/spec.md`):
```diff
### API Contract
POST /users
Request:
  - name: string (required)
  - email: string (required)
+   - email must match RFC 5322 format
+   - Returns 400 if email invalid

### Error Responses
+ | 400 | INVALID_EMAIL | Email format is invalid |
```

**3. Task Changes** (`3-tasks/tasks.json`):
```diff
+ ### TASK-015: Add email validation
+ **Description**: Implement email format validation
+ **Acceptance Criteria**:
+ - Validate email on form submission
+ - Show user-friendly error message
+ **Estimate**: 2h
```

**4. Code Changes**:
```diff
// src/validators/user.js
+ function validateEmail(email) {
+   const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
+   if (!emailRegex.test(email)) {
+     throw new ValidationError('INVALID_EMAIL', 'Email format is invalid');
+   }
+ }

// src/services/UserService.js
async createUser(data) {
+  validateEmail(data.email);
   return this.repository.create(data);
}
```

**Confidence**: High
**Risk**: Low

---

Apply this fix? (y/n)
```

---

### Step 4.5: Implementation Plan ⭐ v1.7.0

> **PURPOSE**: Before writing a single line of code, define the exact order of changes. This prevents applying code changes that depend on spec updates that haven't happened yet.

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📋 IMPLEMENTATION PLAN
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Steps (ordered by dependency):

[1] [SPEC/TASK/CODE] [Description] — no dependencies
[2] [SPEC/TASK/CODE] [Description] — depends on [1]
[3] [SPEC/TASK/CODE] [Description] — depends on [2]
...

Test checkpoints:
- After [N]: run [specific test command]
- After [M]: run full suite

Rollback:
- If [step N] fails: revert [which files]
- Risk level: [Low / Medium / High]
```

#### Plan Format Rules

- **Order specs before code** — specs are the source of truth; code follows
- **Order tasks before code** — tasks must exist before implementation
- **Mark dependencies explicitly** — `depends on [N]` prevents out-of-order execution
- **Add test checkpoints** — don't wait until the end to validate
- **Risk = High** if the fix changes API contracts or data models

#### Example Plan

```markdown
## Implementation Plan

[1] SPEC: Update functional spec — add email validation acceptance criteria
    → No dependencies

[2] SPEC: Update technical spec — add INVALID_EMAIL error response to API contract
    → Depends on [1]

[3] TASK: Add TASK-015 "Implement email validation"
    → Depends on [2]

[4] CODE: Create src/validators/user.js with validateEmail()
    → Depends on [3]

[5] CODE: Modify UserService.createUser() to call validateEmail()
    → Depends on [4]

[6] TEST: Add unit tests for validateEmail() + integration test for POST /users
    → Depends on [5]

Test checkpoints:
- After [4]: run `npm test src/validators/user.test.js`
- After [6]: run full suite `npm test`

Rollback:
- If [5] breaks existing tests: revert UserService.js only (keep validator)
- Risk level: Low (additive change, no existing behavior modified)
```

---

### Step 4.6: Create Fix Record Draft ⭐ v1.7.0

> **MANDATORY — execute this BEFORE Step 5 (before the first Edit/Write to any code or spec file).**
>
> **Why here (after 4.5, not after 2)?** The draft captures the FULL diagnosis: root cause + impact assessment + implementation plan. If context exhausts mid-implementation, you have a complete record of what was found and what was planned — enough to resume or escalate.

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📝 CREATING FIX RECORD DRAFT (pre-implementation)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1. Determine target path:
   CASE A: sdd/wip/{feature}/fixes/   (if all modified files belong to one active feature)
   CASE B: meli/fixes/                 (default — brownfield, multi-feature, uncertain)

   Rule: When in doubt, use meli/fixes/. It is always correct.

2. Determine fix ID:
   - Read fixes-log.md → count data rows → ID = count + 1
   - If no fixes-log.md: ID = 001
   - Format: FIX-001, FIX-002, ...

3. Ensure target directory exists (create if needed):
   ```bash
   mkdir -p {target_path}
   ```

4. Create FIX-NNN-YYYY-MM-DD.md at target path:

# FIX-NNN — YYYY-MM-DD [IN_PROGRESS]

## Error Reported
[Original error input from user]

## Investigation

### Hypotheses
| # | Hypothesis | Status | Evidence |
|---|------------|--------|---------|
| H1 | [cause A] | ❌ Eliminated | "[quote from code/log]" |
| H2 | [cause B] | ✅ Confirmed | "[quote from code/log]" |

### Root Cause
[Precise statement] — Confidence: [%]
**Evidence Chain**: [H2 confirmed because X → causes Y → produces error Z]

## Classification
[FEATURE_GAP / DESIGN_FLAW / MISSING_TASK / IMPLEMENTATION_BUG]

## Impact Assessment
- **Functional Spec**: [No change / Update required — reason]
- **Technical Spec**: [No change / Update required — reason]
- **Tasks**: [No change / Task NNN added/modified]
- **Code**: [Files to be modified]

## Implementation Plan
[1] [SPEC/TASK/CODE] [Description] — no dependencies
[2] [SPEC/TASK/CODE] [Description] — depends on [1]
...
Test checkpoints: [after step N: run X]
Risk level: [Low / Medium / High]

## Status: IN_PROGRESS
_Implementation not yet started. This file will be updated when complete._

5. Append row to fixes-log.md (create if not exists):
   | FIX-NNN | YYYY-MM-DD | [Classification] | [Root cause summary] | - | IN_PROGRESS |

✅ Draft created: {path}/FIX-NNN-DATE.md
✅ Log updated: {path}/fixes-log.md

→ NOW proceed to Step 4.7 (spec-based tests — BEFORE any code change)
```

> **If the fix is interrupted** (context exhausts, crash, user cancels): the draft remains as complete evidence. Next session reads it and continues from the planned Step N.

---

### Step 4.7: Specification Tests (Red-Green + Mutation Validation)

> **MANDATORY before Step 5** — unless the applicability gate below marks it as N/A.

#### Applicability Gate

```
IF fix_classification == "MISSING_TASK" (tasks.json only, no code change):
    → SKIP Step 4.7. Document in Fix Record: "N/A — task-only fix, no executable tests"
    → Continue to Step 5.

IF current phase != "implementation" (e.g., spec-only fix, no runnable code yet):
    → SKIP Step 4.7. Document in Fix Record: "N/A — pre-implementation phase"
    → Continue to Step 5.

IF no test runner available locally (missing env, Docker required, infra unavailable):
    → SKIP phases 2–3. Document reason in Fix Record under "Specification Tests".
    → Write spec-based tests (Phase 1) and note: "Red phase deferred — run in CI"
    → Continue to Step 5 with YELLOW gate status.

OTHERWISE: proceed through all 4 phases below.
```

The existing test suite is often biased — tests were written to pass the current (buggy) implementation, so they encode the bug, not the spec. This step breaks that cycle by writing tests against the **spec** first, verifying they fail against the current code, then validating their sensitivity via targeted mutation.

#### Phase 1 — Write Spec-Based Tests

Write new tests based on the **confirmed root cause** and the **spec** (functional/technical). These tests encode what SHOULD happen, not what currently happens.

Rules for spec-based tests:
- Derived from the spec, NOT from reading the current implementation
- At minimum: 1 test for the correct behavior (happy path per spec), 1 test that directly triggers the bug scenario
- Do NOT look at the existing test file while writing — read the spec section that the bug violates
- Place in the same test file, clearly marked: `// SPEC-TEST: FIX-NNN`

#### Phase 2 — Red Validation (run BEFORE fix)

Run ONLY the new spec-based tests against the current (unmodified) code:

```
IF tests FAIL (red):
  ✅ Tests are real and non-biased → proceed to Phase 3

IF tests PASS (green):
  🚨 ALERT: tests do not catch the bug
  Options:
    A) Hypothesis was wrong → return to Step 2
    B) Tests are still biased → rewrite and retry Phase 2
    C) Acknowledge with justification → document in Fix Record and proceed (exceptional)
```

> **The red phase is a proof**: a test that passes before the fix is not testing the fix. Never skip it.

#### Phase 3 — Mutation Validation (targeted)

Validate that the new tests are **sensitive** to the specific code path being fixed. Apply a **targeted mutation** — a deliberate wrong change to the root cause location (different from the fix, but in the same code area):

```
1. Identify exact root cause location (from Step 2):
   e.g., src/validators/input.go:142

2. Apply a targeted mutation (do NOT apply the fix):
   Bug:      missing nil check → code panics on nil
   Mutation: add a check that always returns early (hides the problem differently)
   —— OR ——
   Bug:      wrong operator `>` instead of `>=`
   Mutation: change to `<` (opposite wrong, not the fix)

3. Run spec-based tests against the mutation:
   IF tests FAIL (catch mutation):
     ✅ High quality — tests are sensitive to this code path
   IF tests PASS (miss mutation):
     ⚠️  Weak coverage — tests don't reach the path
     → Add a more targeted test, then retry Phase 3

4. Revert mutation (restore original buggy code)
```

> **Mutation is not exhaustive** — only 1-2 targeted mutations per fix. Goal is confidence, not completeness.

#### Phase 4 — Quality Gate + Fix Record Update

```
GREEN LIGHT  → red phase = FAIL  AND mutation caught = FAIL  ✅  proceed to Step 5
YELLOW LIGHT → red phase = FAIL  AND mutation missed = PASS  ⚠️  proceed with caution (tests may miss edge cases)
RED LIGHT    → red phase = PASS                              🚨  BLOCK Step 5 — resolve Phase 2 first
```

Update Fix Record draft (`FIX-NNN.md`) with:
```
## Specification Tests (Step 4.7)
Tests created: [list of test names]
Red phase:     FAIL ✅ / PASS 🚨 (reason: ...)
Mutation:      CAUGHT ✅ / MISSED ⚠️  (mutation applied: ...)
Gate:          GREEN / YELLOW / RED
```

---

### Step 5: Apply Fix Horizontally

```
AI: 🔧 Applying horizontal fix...

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📋 FUNCTIONAL SPEC
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Updated: sdd/wip/user-registration/1-functional/spec.md
   - Added acceptance criteria for email validation

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔧 TECHNICAL SPEC
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Updated: sdd/wip/user-registration/2-technical/spec.md
   - Added email validation requirement to API contract
   - Added INVALID_EMAIL error response

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📝 TASKS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Updated: sdd/wip/user-registration/3-tasks/tasks.json
   - Added TASK-015: Email validation

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💻 CODE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Created: src/validators/user.js
✅ Modified: src/services/UserService.js
✅ Created: src/validators/user.test.js
```

---

### Step 6: Re-run Tests

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🧪 RUNNING TESTS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Running test suite...

✅ All tests passing (47 passed, 0 failed)
✅ New tests for email validation: 3 passed
✅ Coverage: 87%
```

---

### Step 6.5: Code Review (MANDATORY)

> **CRITICAL**: After applying code fixes, you MUST call the code review tool and fix ALL findings before proceeding.

See [AI_AGENT_GUIDELINES.md](../AI_AGENT_GUIDELINES.md#mandatory-code-review-protocol--v140) for full protocol.

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔍 CODE REVIEW
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Calling code review tool...

Review Results:
• Critical: 0
• Major: 0
• Minor: 2
  - src/validators/user.js:5 - Missing JSDoc comment
  - src/validators/user.js:8 - Variable 'emailRegex' could be const

Fixing minor findings...

✅ Re-running code review...
✅ All findings resolved (0 critical, 0 major, 0 minor)
```

**IMPORTANT**: Fix is NOT complete until code review shows ZERO findings (including minor).

---

### Step 7: Bidirectional Consistency Check (MANDATORY)

> **CRITICAL**: After applying the fix, validate all layers remain consistent.

Run the sync validation:

```
/sdd.check --sync
```

This validates:
- **Functional ↔ Technical ↔ Tasks ↔ Code** consistency
- Detects any drift introduced by the fix
- Proposes additional fixes if gaps found

**Verdict Handling**:

| Verdict | Action |
|---------|--------|
| `APPROVED` | Fix complete, proceed |
| `CAN_PROCEED_WITH_WARNINGS` | Fix complete, note warnings in task |
| `CANNOT_PROCEED` | Apply suggested fixes before marking task complete |

**If sync finds issues**: Apply the suggested fixes, then re-run `/sdd.check --sync` until it passes.

---

### Consistency Check Failure

If consistency check fails, the fix is NOT complete:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔍 CONSISTENCY CHECK
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📋 Functional Spec ↔ Technical Spec
   ✅ All acceptance criteria have corresponding API contracts

🔧 Technical Spec ↔ Tasks
   ⚠️ INCONSISTENCY FOUND:
   - Technical spec mentions INVALID_EMAIL error (400)
   - No task exists for error handling implementation

📝 Tasks ↔ Code
   ✅ All tasks have corresponding code changes

💻 Code ↔ Tests
   ⚠️ INCONSISTENCY FOUND:
   - validateEmail() function has no test coverage

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
❌ CONSISTENCY CHECK FAILED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Issues to resolve:
1. Add task for error handling implementation
2. Add tests for validateEmail() function

Would you like me to fix these inconsistencies? (y/n)
```

---

## Impact Categories

| Error Type | Typical Impact | Layers to Update |
|------------|----------------|------------------|
| **Missing validation** | Requirement gap | All 4 layers |
| **Wrong API response** | Design flaw | Technical + Tasks + Code |
| **Edge case not handled** | Requirement gap | All 4 layers |
| **Type mismatch** | Design flaw | Technical + Code |
| **Null reference** | Implementation bug | Code only (usually) |
| **Missing error handling** | Design flaw | Technical + Tasks + Code |
| **Performance issue** | Design flaw | Technical + Tasks + Code |
| **Integration failure** | Design flaw | Technical + Code |

---

## Behavior by Mode

| Mode | Behavior |
|------|----------|
| **Express** | Auto-assess impact, auto-apply to all layers |
| **Standard** | Show impact assessment, confirm, apply all layers |
| **Expert** | Detailed analysis, choose which layers to update, manual control |

### Mode Impact on Steps

| Step | Express | Standard | Expert |
|------|---------|----------|--------|
| Step 0 (Phase Detection) | Auto | Auto | Auto |
| Step 1.5 (Classification) | Auto | Auto | User confirms |
| Step 2 (Deep Investigation) | 1 hypothesis | ≥2 hypotheses | ≥2 hypotheses, user reviews |
| Step 3 (Impact Assessment) | Auto | Shown, auto-proceed | User reviews each |
| Step 3.5 (Anti-shortcut) | Skipped | Auto-verify | User provides evidence |
| Step 4 (Propose Changes) | Skipped | Shown, confirm once | Review each layer |
| Step 4.5 (Implementation Plan) | Skipped | Auto | User reviews order |
| Step 4.6 (Fix Record Draft) | Auto | Auto | Auto |
| Step 4.7 (Spec Tests) | Skipped | Full red-green + mutation | User controls phases |
| Step 5 (Apply Fix) | Auto | After confirm | Per-layer control |
| Step 6 (Tests) | Auto | Auto | User triggers |
| Step 6.5 (Code Review) | Auto | Auto | User reviews findings |
| Step 7 (Consistency) | Auto | Auto | User verifies each |
| Step 8 (Fix Record) | Auto | Auto | Auto |

**Recommendation**: Use **Standard** mode for most fixes. Use **Expert** only when you need fine-grained control over which layers to update.

---

## Example: Full Horizontal Fix

```bash
User: /sdd.fix "Error: User created without email validation.
Production issue: invalid emails in database."

AI: 🔧 Error Analysis
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

**Error Type**: Requirement Gap (Production Issue)
**Root Cause**: Email validation was never specified or implemented

## Impact Assessment

### 📋 Functional Spec
**Status**: ⚠️ UPDATE REQUIRED
- Missing acceptance criteria for email validation
- No user story for invalid input handling

### 🔧 Technical Spec
**Status**: ⚠️ UPDATE REQUIRED
- API contract missing email format validation
- No error response defined for invalid email
- Data model should specify email constraints

### 📝 Tasks
**Status**: ⚠️ UPDATE REQUIRED
- No task existed for email validation
- Need to add validation task
- Need to add data cleanup task (existing invalid data)

### 💻 Code
**Status**: ⚠️ UPDATE REQUIRED
- No validation in UserService
- No validation in API layer
- No tests for email validation

---

## Proposed Horizontal Fix

This fix will update ALL 4 layers to ensure consistency:

1. **Functional Spec**: Add email validation acceptance criteria
2. **Technical Spec**: Add API validation rules and error responses
3. **Tasks**: Add TASK-015 (validation) and TASK-016 (data cleanup)
4. **Code**: Implement validation + tests

Estimated additional work: 4 hours

Apply this horizontal fix? (y/n)
```

---

## Flags and Options

| Flag | Description | Example |
|------|-------------|---------|
| `--file` | Read error from file | `/sdd.fix --file ./error.log` |
| `--dry-run` | Show fix plan without applying | `/sdd.fix --dry-run "error"` |
| `--batch` | One subagent per fix (prevents context exhaustion) | `/sdd.fix --batch` |
| `--code-only` | ⚠️ DANGEROUS: Fix code only | `/sdd.fix --code-only "error"` |
| `--layer` | ⚠️ DANGEROUS: Fix specific layer only | `/sdd.fix --layer technical "error"` |

### ⚠️ Dangerous Flags Warning

**`--code-only` and `--layer` flags can cause spec drift:**

| Flag | What Happens | Risk |
|------|--------------|------|
| `--code-only` | Only updates code, skips all specs | Specs become outdated, future features built on wrong assumptions |
| `--layer technical` | Only updates technical spec | Functional spec and tasks become inconsistent |

**When are these flags "safe"?**
- **Almost never.** If your fix changes behavior, specs should be updated.
- The ONLY safe use case: pure implementation bug with no spec impact (e.g., fix a typo in variable name)

**If you're tempted to use these flags**, ask yourself:
1. Does this fix add ANY new behavior? → If yes, don't use these flags
2. Does this fix change ANY API response? → If yes, don't use these flags
3. Does this fix affect ANY user-visible output? → If yes, don't use these flags

**Consequences of misuse:**
- `/sdd.check --sync` will later detect inconsistencies
- Future developers will be confused by undocumented behavior
- Tests may fail when specs are eventually updated

---

## Output Format

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔧 FIX: [error-type]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📍 Root Cause: [explanation]

📊 Impact Assessment:
   📋 Functional: [No Change / Update]
   🔧 Technical:  [No Change / Update]
   📝 Tasks:      [No Change / Update]
   💻 Code:       [Update Required]

📝 Proposed Changes:
   [Summary of changes per layer]

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## AI Agent Instructions

> **CRITICAL**: Read [AI_AGENT_GUIDELINES.md](../AI_AGENT_GUIDELINES.md) before executing.

### Help Flag Detection

**WHEN** the user runs `/sdd.fix help`:
1. Output ONLY the "Quick Help" section (not full documentation)
2. Do NOT execute fix logic
3. Keep response concise (~15 lines)

### --batch Flag Detection ⭐ v1.7.0

### Key Rules

1. **DETECT PHASE FIRST** ⭐ v1.6.0 - Run Step 0 to know which layers EXIST before any assessment
2. **ONLY ASSESS EXISTING LAYERS** - NEVER suggest updating Tasks/Code if phase < tasks/implementation
3. **Think horizontally** - Every fix must consider impact on ALL **existing** layers
4. **Trace to root cause** - Don't just patch symptoms, fix the source
5. **Update specs FIRST** - Specs are the source of truth
6. **Maintain consistency** - All artifacts must tell the same story
7. **Document the gap** - Explain what was missing and why
8. **MANDATORY CODE REVIEW** - After applying code fixes, call code review tool and fix ALL findings

9. **CLASSIFY BEFORE FIXING** - Always run Step 1.5 (Problem Classification) first
10. **NEVER SHORTCUT "No Change"** - Always provide evidence when declaring No Change
11. **VERIFY BIDIRECTIONALLY** - Check both specs→code AND code→specs (only for existing layers)
12. **INVESTIGATE WITH HYPOTHESES** ⭐ v1.7.0 - Generate ≥2 hypotheses and eliminate with evidence before concluding
13. **PLAN BEFORE APPLYING** ⭐ v1.7.0 - Define ordered implementation steps (Step 4.5) before touching any file
14. **PERSIST FIX RECORD EARLY** ⭐ v1.7.0 - Create FIX-NNN.md DRAFT at Step 4.6 (before any code). Finalize at Step 8. Even if fix is interrupted, the investigation record must exist.
15. **ONE ISSUE PER CONTEXT** ⭐ v1.7.0 - If N>1 issues, ALWAYS spawn subagents (one per fix). NEVER process multiple fixes inline. Context exhaustion = shallow analysis.

> **Telemetry**: Captured automatically by hooks - no manual logging required.

---

### Enforcement Rules (v1.5.0)

> These rules prevent the common failure pattern of fixing code without updating specs.

#### Rule 1: Classification BEFORE Analysis

```
WRONG:
  1. Read error → 2. Fix code → 3. Check if specs need update

CORRECT:
  1. Read error → 2. CLASSIFY problem type → 3. Determine required layers → 4. Update ALL required layers
```

#### Rule 2: Evidence-Based "No Change" Only

```
WRONG:
  Status: No Change (already documented)

CORRECT:
  Status: No Change
  Evidence: Section "API Contracts" line 145 states:
  > "POST /users validates email format and returns 400 INVALID_EMAIL"
  This covers the validation we're fixing.
```

#### Rule 3: Code Changes Trigger Spec Review

If you modify code to add ANY of these, specs MUST be updated:

| Code Change | Spec Update Required |
|-------------|---------------------|
| New function/method with business logic | Technical Spec + Tasks |
| New API parameter or response field | Technical Spec + Tasks |
| New error type or validation | Technical Spec + Tasks |
| New external tool/service call | Technical Spec + Tasks |
| New user-visible output or behavior | Functional Spec + Technical Spec + Tasks |
| New configuration option | Technical Spec + Tasks |

#### Rule 4: Post-Fix Verification is NOT Optional

```
WRONG:
  ✅ Code fixed, tests pass, done!

CORRECT:
  ✅ Code fixed
  ✅ Tests pass
  ✅ Code review: 0 findings
  ✅ Bidirectional consistency check:
     - Code→Specs: All new behaviors documented
     - Specs→Code: All spec requirements implemented
```

---

### Analysis Strategy

Use the classification decision tree from **Step 1.5** above, then:
1. Classify problem type
2. Determine required layers based on classification
3. For each layer: provide evidence quote (no change) or show diff (updating)
4. Verify bidirectionally: list NEW behaviors, verify each is documented

---

### Common Failure Patterns to Avoid

#### Pattern 1: "It Works So Specs Are Fine"

**WRONG**: "Tests pass, so functional spec is correct"
**WHY WRONG**: The fix may have added behavior not in specs
**CORRECT**: "Tests pass. Now verifying: does the new behavior exist in specs?"

#### Pattern 2: "Only Code Changed"

**WRONG**: Classify as IMPLEMENTATION_BUG when adding new logging/validation/behavior
**WHY WRONG**: New behavior = new specs needed, regardless of how "minor"
**CORRECT**: If fix adds ANY new capability, it's at least MISSING_TASK, not IMPLEMENTATION_BUG

#### Pattern 3: "Specs Are Vague Enough"

**WRONG**: "Spec says 'handle errors' so my new error type is covered"
**WHY WRONG**: Vague specs don't document specific behaviors
**CORRECT**: If you add a specific error type, add it to technical spec

#### Pattern 4: "I'll Update Specs Later"

**WRONG**: Fix code now, update specs in another pass
**WHY WRONG**: Specs and code drift apart, causing future confusion
**CORRECT**: Update specs IN THE SAME /sdd.fix execution, atomically

#### Pattern 5: "I'll Process Multiple Fixes Inline" ⭐ v1.7.0

**WRONG**: Receive 7 fixes → process all 7 in the same agent session
**WHY WRONG**: After fix #2–3, context is >70%. The agent can still write code, but:
- Hypothesis generation is skipped or reduced to 1
- Evidence quotes are omitted
- Fix records (Step 8) are never created (last steps dropped)
- The result LOOKS like a fix but misses root cause

**CORRECT**: Detect N>1 issues at Step -1 → spawn N subagents → each fix gets fresh context
**EVIDENCE**: Monitoring shows code files modified but zero FIX-*.md records created after a 7-fix batch session (verified 2026-04-03)

---

### Pre-Fix Checklist

Before declaring fix complete:
- [ ] Problem classified (Step 1.5)
- [ ] All required layers identified
- [ ] ≥2 hypotheses generated and eliminated with evidence (Step 2)
- [ ] Implementation plan created with ordered steps (Step 4.5)
- [ ] **Fix record DRAFT created BEFORE any code change** (Step 4.6) ⭐ v1.7.0
- [ ] For each "No Change": Evidence quote provided
- [ ] For each update: Diff shown and applied
- [ ] Code review: 0 findings
- [ ] Bidirectional consistency verified
- [ ] **Fix record finalized** (Step 8) — `FIX-NNN-DATE.md` RESOLVED + `fixes-log.md` updated ⭐ v1.7.0

---

## Failure Scenarios and Recovery

### What If Fix Doesn't Work?

| Scenario | Symptom | Recovery |
|----------|---------|----------|
| **Wrong Classification** | Problem persists after fix | `/sdd.rollback --task` → Re-run `/sdd.fix` with fresh analysis |
| **Incomplete Fix** | Tests still fail | Run `/sdd.fix` again (builds on previous), consider `sdd-debugger` |
| **Code Review Issues** | Critical findings | Address ALL findings before proceeding - this is a quality gate |
| **Consistency Fails** | Layer inconsistencies | Update specs/tasks for new behaviors, re-run check |
| **Too Complex** | Multiple attempts fail | Delegate to `sdd-debugger` subagent for deep analysis |

### Exit Criteria: When Is a Fix Complete?

A fix is complete when ALL of the following are true:

| Checkpoint | Verification |
|------------|--------------|
| ✅ Tests pass | Run test suite, 100% pass |
| ✅ Code review clean | code review tool shows 0 findings |
| ✅ Specs updated | All new behavior documented |
| ✅ Tasks updated | All new work captured in tasks |
| ✅ Consistency verified | Bidirectional check passes |
| ✅ **Fix record persisted** ⭐ v1.7.0 | `sdd/fixes/FIX-NNN-DATE.md` (or `sdd/wip/{feature}/fixes/`) exists AND `fixes-log.md` has a new row |

**⛔ DO NOT declare the fix complete without the fix record file.** If the `fixes/` directory does not exist yet, CREATE IT and the fix record before marking done.

If ANY checkpoint fails, the fix is NOT complete. Return to the relevant step and iterate.

---

## Step 8: Finalize Fix Record ⭐ v1.7.0

> **MANDATORY**: After every successful fix, finalize the draft record created at Step 4.6. Update status from IN_PROGRESS → RESOLVED and fill in the result sections.
>
> **If Step 4.6 was skipped** (e.g. first run with this version of the skill): create the full record now using the template below.

### Directory Structure

```
meli/fixes/                         ← default for brownfield / multi-feature
  fixes-log.md
  FIX-001-2026-04-03.md
  FIX-002-2026-04-10.md
  ...

sdd/wip/{feature}/fixes/           ← optional: feature-scoped fixes
  fixes-log.md
  FIX-001-2026-04-03.md
  ...
```

### Fix ID Generation

Format: `FIX-NNN` where NNN = count of existing entries in `fixes-log.md` + 1.
If `fixes-log.md` doesn't exist, start at `FIX-001`.

### FIX-NNN-DATE.md Template

```markdown
# FIX-NNN — YYYY-MM-DD

## Error Reported
[Paste or summarize the original error input from the user]

## Investigation

### Hypotheses
| # | Hypothesis | Status | Evidence |
|---|------------|--------|---------|
| H1 | [cause A] | ❌ Eliminated | "[quote]" |
| H2 | [cause B] | ✅ Confirmed | "[quote]" |

### Root Cause
[Precise statement] — Confidence: [%]

**Evidence Chain**: [H2 confirmed because X, which causes Y, which produced error Z]

## Classification
[FEATURE_GAP / DESIGN_FLAW / MISSING_TASK / IMPLEMENTATION_BUG]

## Implementation Plan Executed
[1] [SPEC/TASK/CODE] [Description]
[2] ...

## Layers Changed
- **Functional Spec**: [No change / Updated — what changed]
- **Technical Spec**: [No change / Updated — what changed]
- **Tasks**: [No change / TASK-XXX added/modified]
- **Code**: [Files modified/created]

## Result
- Tests: [N passed / N failed]
- Code review: [0 findings / N fixed]
- Consistency check: [APPROVED / CAN_PROCEED_WITH_WARNINGS]
- Status: RESOLVED
```

### fixes-log.md Template (append-only)

```markdown
# Fixes Log — {feature or "project"}

| ID | Date | Classification | Root Cause (summary) | Layers | Status |
|----|------|----------------|---------------------|--------|--------|
| FIX-001 | 2026-04-03 | DESIGN_FLAW | Missing email validation | Tech+Tasks+Code | RESOLVED |
```

### Agent Instructions for Step 8

```
After Step 7 passes (APPROVED or CAN_PROCEED_WITH_WARNINGS):

IF Step 4.6 was executed (draft exists):
  1. Open FIX-NNN-DATE.md created at Step 4.6
  2. Replace "[IN_PROGRESS]" in title with "[RESOLVED]"
  3. Fill in "## Layers Changed" and "## Result" sections
  4. Update "## Status: IN_PROGRESS" → "## Status: RESOLVED"
  5. Update fixes-log.md row: change "IN_PROGRESS" → "RESOLVED", fill Layers column

IF Step 4.6 was NOT executed (no draft — recover):
  1. Determine target path using this decision tree:

   CASE A — All modified files belong to ONE active wip feature:
     - Find sdd/wip/*/meta.md where status IN (in-progress, implemented)
     - Check each feature's tasks.json "files" list
     - If ALL modified files match one feature: Path = sdd/wip/{feature}/fixes/

   CASE B — Default fallback (use in ALL other cases):
     - Fix touches files from multiple features
     - Fix is a side-effect in unrelated code
     - Fix is brownfield (no active feature)
     - Uncertain which feature owns the fix
     - Path: meli/fixes/  ← SAFE DEFAULT, always valid
     - Add field "Feature: (none)" or "Feature: cross-feature side-effect"

   > **Rule of thumb**: When in doubt, use meli/fixes/. It's always correct.
   > Feature-scoped fixes/ are an optimization for traceability, not a requirement.

2. Determine fix ID:
   - If fixes-log.md exists at target path: count data rows, next ID = count + 1
   - If not: ID = 001
   - Format: FIX-001, FIX-002, ...

3. Create FIX-NNN-YYYY-MM-DD.md at target path using the template above
   - Fill all sections from the current session's analysis
   - CASE B records: add field "Feature: (none — brownfield fix)"

4. Append row to fixes-log.md at target path (create if not exists)

5. Confirm:
   ✅ Fix record created: {target_path}/FIX-NNN-DATE.md
   ✅ History updated: {target_path}/fixes-log.md
```

### When Step 8 is NOT Required

Skip Step 8 **only** when the fix was applied via `--dry-run` (nothing was actually changed).

**All other fixes — including IMPLEMENTATION_BUG — MUST produce a fix record.** Even a one-line typo fix should have a minimal record. History must be complete.

> **Why no exception for IMPLEMENTATION_BUG?** Recurring "trivial" bugs in the same file often signal a deeper design issue. The fix log makes this pattern visible over time.

---

## Backlog Integration ⭐ v1.7.0

After Step 8, if the fix meets ANY of these conditions, **suggest creating a backlog item**:

| Condition | Backlog Category | Why |
|-----------|-----------------|-----|
| Classification = `DESIGN_FLAW` AND brownfield fix | `DEBT` | Underlying design needs rework |
| Classification = `FEATURE_GAP` AND brownfield fix | `DEBT` | Gap in spec should be formalized |
| Recurring fix warning triggered (≥3 in 30 days) | `DEBT` | Structural problem, not a one-off |
| Fix took >2 hours OR touched >5 files | `DEBT` | Complexity suggests systemic issue |

### Suggestion Format

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📋 BACKLOG SUGGESTION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
This fix reveals a design debt that should be tracked.

Suggested backlog item:
  Category: DEBT
  Title: [concise title describing the underlying issue]
  Description: [what needs to be addressed properly]
  Origin: FIX-NNN (DATE)

Add to sdd/backlog.md? (y/n)
```

If the user confirms, call `/sdd.backlog add` with the suggested item.

> **Rule**: This is a suggestion, NOT mandatory. The user decides if the debt is worth tracking. Do NOT auto-add without confirmation.

---

## Related Commands

- `/sdd.build` - Implementation (may trigger fix)
- `/sdd.check --sync` - Verify consistency between layers
- `/sdd.check --compliance` - Verify tests/lint compliance
- `/sdd.rollback` - Revert if fix doesn't work
- `/sdd.backlog` - Manage backlog (debts, ideas, todos)

---
