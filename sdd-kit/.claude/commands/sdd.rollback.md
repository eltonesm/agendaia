---
name: sdd.rollback
description: Rollback feature to a previous workflow phase, preserving git history. Use when user needs to redo a phase.
model: sonnet
argument-hint: "[feature-name] [target-phase]"
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

# Command: /sdd.rollback

**Description**: Safely revert a feature to a previous phase, preserving work history.

**Usage**:
- `/sdd.rollback [phase]` → Rollback current feature to phase
- `/sdd.rollback` → Show current phase, ask which to rollback to
- `/sdd.rollback --task TASK-XXX` → Revert changes from specific task
- `/sdd.rollback --phase N` → Revert to end of specific phase

**Phase Numbers**:
| Phase | Name |
|-------|------|
| 1 | Functional Specification |
| 2 | Technical Specification |
| 3 | Task Planning |
| 4 | Implementation |

---

## Quick Help

> `/sdd.rollback help` → Shows this summary

**Syntax**: `/sdd.rollback [target]`

| Flag | Description |
|------|-------------|
| (none) | Show current phase, ask which to rollback to |
| `[1-4]` | Rollback to specific phase number |
| `--task TASK-XXX` | Revert commits from specific task only |
| `--phase N` | Revert all commits after phase N |

**Phases**: 1=Functional, 2=Technical, 3=Tasks, 4=Implementation

**Examples**:
```bash
/sdd.rollback 2              # Rollback to end of technical spec
/sdd.rollback --task TASK-005  # Revert only TASK-005 commits
```

**See also**: `/sdd.help rollback` for detailed documentation

---

CRITICAL: USER INTERACTION RULES
When this skill shows JSON for AskUserQuestion, you MUST:
  1. CALL the AskUserQuestion TOOL with that exact JSON
  2. DO NOT print options using Bash (no echo, cat, printf)
  3. DO NOT ask "Which option?" as text
  4. Tables marked "REFERENCE ONLY" are for docs - do NOT print

## When to Use

### Valid Rollback Scenarios

| Scenario | Rollback To | Reason |
|----------|-------------|--------|
| Requirements changed fundamentally | Phase 1 | Need new functional spec |
| Architecture needs redesign | Phase 2 | Technical approach wrong |
| Tasks don't match implementation reality | Phase 3 | Need to re-plan tasks |
| Implementation has critical issues | Phase 4 (restart) | Start implementation fresh |

### NOT Valid for Rollback

- Minor spec clarifications (use `/sdd.spec --include` instead)
- Single task failures (fix the task, don't rollback)
- Test failures (fix tests, don't rollback)
- Code review feedback (iterate, don't rollback)

---

## Rollback Rules

### What Gets Preserved

| Phase | Preserved | Archived |
|-------|-----------|----------|
| Rollback to 1 | Nothing | Tech spec, tasks, implementation |
| Rollback to 2 | Functional spec | Tasks, implementation |
| Rollback to 3 | Functional + Tech specs | Implementation |
| Rollback to 4 | All specs + tasks | Implementation progress |

### Rollback Matrix

```
Current Phase → Can Rollback To
─────────────────────────────────
Phase 4 (Impl)  → 3, 2, 1
Phase 3 (Tasks) → 2, 1
Phase 2 (Tech)  → 1
Phase 1 (Func)  → Cannot rollback
```

---

## Workflow

### Step 1: Validate Rollback Request

**Detect Current Phase from meta.md**:

The `meta.md` file uses `Current Stage:` field (not `current_phase:`). Map stage to phase number:

| Stage Name | Phase Number |
|------------|--------------|
| `functional` | 1 |
| `technical` | 2 |
| `tasks` | 3 |
| `implementation` | 4 |

**Also check the `stages:` YAML section** for `status: in-progress` to confirm:

```yaml
stages:
  functional:
    status: approved        # Phase 1 complete
  technical:
    status: approved        # Phase 2 complete
  tasks:
    status: approved        # Phase 3 complete
  implementation:
    status: in-progress     # ← Currently in Phase 4
    completed_tasks: 5
    total_tasks: 12
```

```bash
FEATURE_PATH="sdd/wip/[feature-name]"
# Read stage from meta.md (e.g., "implementation")
CURRENT_STAGE=$(grep "Current Stage:" "$FEATURE_PATH/meta.md" | cut -d: -f2 | tr -d ' ')

# Map stage name to phase number
case "$CURRENT_STAGE" in
    functional) CURRENT_PHASE=1 ;;
    technical)  CURRENT_PHASE=2 ;;
    tasks)      CURRENT_PHASE=3 ;;
    implementation) CURRENT_PHASE=4 ;;
    *) echo "Unknown stage"; exit 1 ;;
esac

TARGET_PHASE=$1

# Validate target is less than current
if [ "$TARGET_PHASE" -ge "$CURRENT_PHASE" ]; then
    echo "❌ Error: Can only rollback to earlier phases"
    exit 1
fi
```

### Step 2: Create Snapshot

Before making changes, snapshot current state to `.rollback-history/`.

### Step 3: Document Reason

Record rollback reason, impact assessment, and justification.

### Step 4: Archive Affected Phases

Move affected content to `.archived/` with templates reset.

### Step 5: Update meta.md

Update current phase and rollback history.

---

## Output Example

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
⏪ ROLLBACK: user-preferences
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Current Phase: 4 (Implementation)
Target Phase:  2 (Technical Specification)

📸 Creating snapshot...
   Saved to: .rollback-history/20251127_143022

⚠️  This will archive:
   • Phase 3: Task list (12 tasks)
   • Phase 4: Implementation progress (5/12 tasks completed)

   This will preserve:
   • Phase 1: Functional specification ✓
   • Phase 2: Technical specification ✓

[AskUserQuestion: "Continue with rollback?"] → User: Yes

✅ Rollback Complete

Feature 'user-preferences' is now at Phase 2

Next steps:
1. Review and update technical specification if needed
2. Run: /sdd.spec technical --include
3. When ready: /sdd.spec technical --approve
```

---

## Safety Features

### Automatic Snapshot
Every rollback creates automatic snapshot - you can always restore.

### Confirmation Required
Rollbacks require explicit confirmation for destructive operations.

### Audit Trail
All rollbacks are recorded in meta.md with timestamp, user, reason, and snapshot location.

### Archive Preservation
Archived work is never deleted - moved to `.archived/` directory.

---

## Restoring from Rollback

If rollback was a mistake, restore from snapshot:

```bash
# List available snapshots
ls -la sdd/wip/[feature-name]/.rollback-history/
```

---

## Telemetry on Rollback

> **Note**: Telemetry is captured automatically by hooks in `~/.claude/logs/` (Claude Code) or `~/.cursor/logs/` (Cursor). Not available for Meli Agent CLI.

---

## AI Agent Instructions


### Help Flag Detection

**WHEN** the user runs `/sdd.rollback help`:
1. Output ONLY the "Quick Help" section (not full documentation)
2. Do NOT execute rollback logic
3. Keep response concise (~15 lines)

### Phase Detection (MANDATORY)

**Before showing rollback options**, you MUST accurately detect the current phase:

1. **Read `meta.md`** from the feature's WIP folder
2. **Check `Current Stage:` field** for the stage name
3. **Verify with `stages:` YAML section** - look for `status: in-progress` or `status: approved`
4. **Cross-check with artifacts**:
   - Phase 1 complete: `1-functional/spec.md` exists and `stages.functional.status: approved`
   - Phase 2 complete: `2-technical/spec.md` exists and `stages.technical.status: approved`
   - Phase 3 complete: `3-tasks/tasks.json` exists and `stages.tasks.status: approved`
   - Phase 4 in progress: Any task has `status: in_progress` or `status: completed`

**Phase Detection Logic**:

```
IF stages.implementation.status = "in-progress" OR stages.implementation.completed_tasks > 0:
    CURRENT_PHASE = 4  # Implementation
ELIF stages.tasks.status = "approved":
    CURRENT_PHASE = 4  # Ready for implementation (or just started)
ELIF stages.tasks.status = "in-progress":
    CURRENT_PHASE = 3  # Task planning
ELIF stages.technical.status = "approved":
    CURRENT_PHASE = 3  # Ready for task planning
ELIF stages.technical.status = "in-progress":
    CURRENT_PHASE = 2  # Technical spec
ELIF stages.functional.status = "approved":
    CURRENT_PHASE = 2  # Ready for technical
ELSE:
    CURRENT_PHASE = 1  # Functional spec
```

**CRITICAL**: If `completed_tasks > 0` in the implementation stage, or if any task in `tasks.json` shows `status: in_progress` or `status: completed`, the feature IS in Phase 4 (Implementation).

### Rollback Options by Current Phase

| Current Phase | Available Rollback Targets | Show Options |
|---------------|---------------------------|--------------|
| Phase 4 (Implementation) | 3, 2, 1 | All 3 options |
| Phase 3 (Tasks) | 2, 1 | 2 options |
| Phase 2 (Technical) | 1 | 1 option |
| Phase 1 (Functional) | None | "Cannot rollback from Phase 1" |

### Key Rules

1. **Always create snapshot** before rollback
2. **Require confirmation** for destructive operations
3. **Document reason** in rollback record
4. **Never delete** - archive instead (applies to logs/ too!)
5. **Update telemetry** - Record rollback event and affected phases
6. **Add rollback_marker** - Never delete token log entries, add marker instead
7. **Increment runs counter** - Track re-executions in telemetry
8. **Accurate phase detection** - NEVER assume phase based on file count alone; check meta.md stages

---

## Intelligent Revert

Git-aware reverting by logical units (Task or Phase) instead of just by phase number.

### Why Intelligent Revert?

Traditional rollback (`/sdd.rollback 3`) reverts to a phase boundary. But sometimes you need finer control:
- Revert just one task that broke something
- Undo all changes from a specific phase
- Keep earlier tasks but redo one specific task

### Commit Tracking

During `/sdd.build`, each task completion records its commit(s) in `meta.md`:

```yaml
implementation:
  tasks:
    TASK-001:
      status: completed
      commits:
        - hash: "abc123"
          message: "feat(payment): TASK-001 - Create Dockerfile"
        - hash: "def456"
          message: "feat(payment): TASK-001 - Add Dockerfile.runtime"
    TASK-002:
      status: completed
      commits:
        - hash: "ghi789"
          message: "feat(payment): TASK-002 - Setup project structure"
```

### `--task TASK-XXX` - Revert Specific Task

Reverts all commits associated with a specific task.

**Usage**:
```bash
/sdd.rollback --task TASK-005
```

**What happens**:
1. Reads task commits from `meta.md`
2. Creates snapshot before reverting
3. Runs `git revert` for each commit (newest first)
4. Updates task status to `pending`
5. Updates `meta.md` with revert record

**Output**:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
⏪ INTELLIGENT REVERT: Task Level
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Feature: payment-gateway
Target: TASK-005 (Service layer implementation)

📸 Creating snapshot...
   Saved to: .rollback-history/20251222_103022

🔍 Finding commits for TASK-005...
   Found 2 commits:
   - xyz789: feat(payment): TASK-005 - Implement PaymentService
   - uvw012: feat(payment): TASK-005 - Add service tests

⚠️  This will revert these commits:
   • uvw012 (newest)
   • xyz789

[AskUserQuestion: "Continue?"] → User: Yes

Reverting commits...
   ✅ Reverted uvw012
   ✅ Reverted xyz789

Updating meta.md...
   ✅ TASK-005 status: completed → pending
   ✅ Revert record added

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Task Revert Complete
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

TASK-005 is now pending. To re-implement:
  /sdd.build task TASK-005
```

### `--phase N` - Revert to Phase End

Reverts all commits from tasks after phase N (keeps phase N intact).

**Usage**:
```bash
/sdd.rollback --phase 2
```

**What happens**:
1. Identifies all tasks in phases > N
2. Collects their commits
3. Creates snapshot
4. Reverts all commits (newest first)
5. Updates all affected task statuses

**Output**:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
⏪ INTELLIGENT REVERT: Phase Level
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Feature: payment-gateway
Target: Revert to end of Phase 2

📸 Creating snapshot...
   Saved to: .rollback-history/20251222_103522

🔍 Analyzing commits by phase...

Phase 3 (Tasks):
   ✅ Keep - No code commits in task planning

Phase 4 (Implementation):
   ⚠️  Revert - 8 tasks with 15 commits

Commits to revert (newest first):
   • aaa111 - TASK-012: E2E setup
   • bbb222 - TASK-011: Integration tests
   ... (13 more)

⚠️  This will:
   • Revert 15 commits from Phase 4
   • Reset 8 tasks to pending
   • Keep Phase 1, 2, 3 intact

[AskUserQuestion: "Continue?"] → User: Yes

Reverting commits...
   ✅ Reverted 15 commits

Updating meta.md...
   ✅ 8 tasks reset to pending
   ✅ Phase 4 status: in-progress → pending
   ✅ Revert record added

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Phase Revert Complete
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Feature is now at end of Phase 2.
To continue: /sdd.plan (re-generate tasks if needed)
         or: /sdd.build (start implementation)
```

### Comparison: Standard vs Intelligent Revert

| Aspect | Standard Rollback | Intelligent Revert |
|--------|-------------------|-------------------|
| Granularity | Phase boundaries only | Task or phase level |
| Git awareness | Archives files only | Reverts actual commits |
| Re-implementation | Must redo entire phase | Can redo single task |
| History preservation | Archives to folder | Git revert (clean history) |
| Use case | Major scope changes | Bug fixes, redo specific work |

### When to Use Each

| Scenario | Use |
|----------|-----|
| "Requirements changed completely" | `/sdd.rollback 1` (standard) |
| "TASK-005 broke the build" | `/sdd.rollback --task TASK-005` |
| "Need to redo implementation with different approach" | `/sdd.rollback --phase 3` |
| "Just one task needs fixes" | `/sdd.rollback --task TASK-XXX` |

---

## Related Commands

- `/sdd.check` - View current status
- `/sdd.cancel` - Cancel feature entirely
- `/sdd.spec` - Continue after rollback

---
