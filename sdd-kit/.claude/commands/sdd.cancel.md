---
name: sdd.cancel
description: Gracefully cancel a feature in progress, preserving work for potential future resumption. Use when user wants to abandon or pause a feature.
model: haiku
argument-hint: "[feature-name] [reason]"
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

# Command: /sdd.cancel

**Purpose**: Gracefully cancel a feature in progress, preserving work for potential future resumption.

---

## Usage

```
/sdd.cancel [feature-name] [reason]
/sdd.cancel --audio                   # Record cancellation reason via voice
```

**Parameters**:
- `feature-name`: Name of the feature to cancel (required)
- `reason`: Brief explanation for cancellation (required)
- `--audio`: Record feature name and cancellation reason via microphone

---

## Quick Help

> `/sdd.cancel help` → Shows this summary

**Syntax**: `/sdd.cancel [feature-name] [reason]`

| Argument | Description |
|----------|-------------|
| `feature-name` | Name of feature to cancel (required) |
| `reason` | Cancellation reason (required) |
| `--audio` | Record feature name and reason via voice |

**Valid Reasons**: `priorities-changed`, `requirements-invalid`, `technical-blocker`, `scope-too-large`

**Examples**:
```bash
/sdd.cancel user-auth priorities-changed
/sdd.cancel --audio    # Speak the feature name and reason
```

**See also**: `/sdd.help cancel` for detailed documentation

---

CRITICAL: USER INTERACTION RULES
When this skill shows JSON for AskUserQuestion, you MUST:
  1. CALL the AskUserQuestion TOOL with that exact JSON
  2. DO NOT print options using Bash (no echo, cat, printf)
  3. DO NOT ask "Which option?" as text
  4. Tables marked "REFERENCE ONLY" are for docs - do NOT print

## When to Use

### Valid Cancellation Reasons

| Reason | Description |
|--------|-------------|
| `priorities-changed` | Business priorities shifted |
| `requirements-invalid` | Requirements no longer valid |
| `technical-blocker` | Insurmountable technical obstacle |
| `resource-constraints` | Team/time/budget constraints |
| `scope-too-large` | Feature needs to be split |
| `duplicate-effort` | Similar feature exists/in-progress |
| `stakeholder-decision` | Explicit stakeholder decision |

### NOT Valid for Cancellation

- ❌ Temporary blocks (use task blocking instead)
- ❌ Waiting for dependencies (pause, don't cancel)
- ❌ Code review feedback (iterate, don't cancel)
- ❌ Test failures (fix tests, don't cancel)

---

## Execution Steps

### Step 1: Validate Feature Exists

```bash
FEATURE_PATH="sdd/wip/[feature-name]"

if [ ! -d "$FEATURE_PATH" ]; then
    echo "❌ Error: Feature not found at $FEATURE_PATH"
    echo "   Use /sdd.list to see active features"
    exit 1
fi
```

### Step 2: Capture Current State

Document the current state before cancellation:

```markdown
## Cancellation Record

**Feature**: [feature-name]
**Cancelled Date**: YYYY-MM-DD
**Cancelled By**: [user]
**Reason**: [reason]

### State at Cancellation

**Phase**: [current phase from meta.md]
**Progress**: [X/Y tasks completed]
**Last Activity**: [date]

### Work Completed

- [x] Functional Spec: [status]
- [x] Technical Spec: [status]
- [ ] Tasks: [X completed, Y pending]
- [ ] Implementation: [% complete]

### Files Created

[List of files that were created during implementation]

### Decisions Made

[Key decisions documented during this feature]

### Reason Details

[Detailed explanation of why feature is being cancelled]

### Resumption Notes

[What would be needed to resume this feature later]
```

### Step 3: Update meta.md

```yaml
# Add to meta.md
status: cancelled
cancelled_date: YYYY-MM-DD
cancelled_by: [user]
cancellation_reason: [reason]
resumable: true/false
```

### Step 4: Move to Cancelled Directory

```bash
# Create cancelled directory if not exists
mkdir -p sdd/cancelled

# Move feature folder (preserving date prefix)
# Example: 20260120-user-auth → 20260120-user-auth_20260325
mv "sdd/wip/[YYYYMMDD-feature-name]" "sdd/cancelled/[YYYYMMDD-feature-name]_YYYYMMDD"
```

**Feature Naming**:
- The date prefix is preserved in the cancelled folder
- This ensures chronological ordering is maintained across all features

### Step 5: Clean Up (Optional)

If feature created branches or resources:

```bash
# List related branches
git branch --list "*[feature-name]*"

# Optionally delete feature branch (with confirmation)
# git branch -d feature/[feature-name]

# Note: Do NOT delete branches without explicit user confirmation
```

### Step 6: Generate Cancellation Report

Create `sdd/cancelled/[feature-name]_YYYYMMDD/CANCELLATION_REPORT.md`:

```markdown
# Cancellation Report: [Feature Name]

## Summary

| Field | Value |
|-------|-------|
| Feature | [feature-name] |
| Started | [start date from meta.md] |
| Cancelled | [today] |
| Duration | [X days] |
| Phase Reached | [phase] |
| Effort Invested | [estimate] |

## Reason for Cancellation

[Detailed reason]

## Impact Assessment

### Work Lost
- [Effort that cannot be reused]

### Work Preserved
- [Specs, decisions, code that can be reused]

### Dependencies Affected
- [Other features/teams affected by cancellation]

## Lessons Learned

- [What we learned from this attempt]
- [What would we do differently]

## Resumption Guide

### Prerequisites for Resumption
- [What needs to be true to resume]

### Complexity to Resume
- [Effort/scope to get back to current state]

### Recommended Changes
- [What should change if resumed]

## Sign-off

- [ ] Tech Lead notified
- [ ] Product Manager notified
- [ ] Related tickets updated
- [ ] Documentation archived
```

---

## Output Example

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🛑 CANCELLING FEATURE: user-preferences
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📋 Current State:
   Phase: Implementation (Phase 4)
   Tasks Completed: 5/12
   Implementation: ~40%

📁 Archiving to: sdd/cancelled/007-user-preferences_20251127

📝 Creating cancellation report...

✅ Cancellation Complete

Files preserved:
  • Functional Spec: ✓
  • Technical Spec: ✓
  • Task List: ✓
  • Progress Notes: ✓
  • Partial Implementation: ✓
  • Token Logs (logs/): ✓
  • Telemetry Data: ✓

⚠️  Action Required:
  • Notify stakeholders of cancellation
  • Update related tickets/issues
  • Consider splitting feature if scope was issue

📂 Archived to: sdd/cancelled/007-user-preferences_20251127/

To resume later:
  mv sdd/cancelled/007-user-preferences_20251127 sdd/wip/007-user-preferences
  /sdd.check 007
```

---

## Cancellation vs Other Actions

| Situation | Action | Command |
|-----------|--------|---------|
| Temporary block | Block task | Update progress.md |
| Need more info | Clarify | `/sdd.spec --include` |
| Wrong approach | Revise spec | Update spec + re-validate |
| Scope too big | Split feature | Create new features |
| Won't do ever | Cancel | `/sdd.cancel` |
| Pause temporarily | Pause | Update meta.md status |

---

## Resuming a Cancelled Feature

To resume a previously cancelled feature:

```bash
# 1. Move back to WIP
mv sdd/cancelled/[feature-name]_YYYYMMDD sdd/wip/[feature-name]

# 2. Update meta.md
#    - Remove cancelled status
#    - Update resumed_date
#    - Document why resuming

# 3. Review cancellation report
#    - Address issues that caused cancellation
#    - Update specs if requirements changed

# 4. Re-validate current state
/sdd.check --validate

# 5. Continue from current phase
/sdd.check
```

---

## Best Practices

1. **Always document thoroughly** - Future you will thank present you
2. **Notify stakeholders** - Don't let cancellation be a surprise
3. **Preserve learnings** - Document what was learned
4. **Consider alternatives** - Is splitting better than cancelling?
5. **Clean up responsibly** - Don't leave orphaned branches/resources

---

## AI Agent Instructions


### Help Flag Detection

**WHEN** the user runs `/sdd.cancel help`:
1. Output ONLY the "Quick Help" section (not full documentation)
2. Do NOT execute cancel logic
3. Keep response concise (~15 lines)

### --audio Flag Detection

### Key Rules
1. **Always require reason** - Don't cancel without documented reason
2. **Preserve all work** - Never delete, always archive
3. **Generate report** - Create comprehensive cancellation report
4. **Suggest alternatives** - Consider if split/pause is better

> **Telemetry**: Data is captured automatically by hooks in `~/.claude/logs/` (Claude Code) or `~/.cursor/logs/` (Cursor). Not available for Meli Agent CLI.

---

## Related Commands

- `/sdd.list` - View all features
- `/sdd.check` - Check feature status
- `/sdd.rollback` - Go back to previous phase instead

---
