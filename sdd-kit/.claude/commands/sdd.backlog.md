---
name: sdd.backlog
description: Manage technical backlog with TODO, DEBT, and IDEA categories. Use when user wants to track, add, or manage backlog items.
model: sonnet
argument-hint: "[action] [item]"
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

# Command: /sdd.backlog

> **Note**: Previously known as `/sdd.todos`. The old name still works as an alias.

**Description**: Manage technical backlog (TODOs, Technical Debt, Ideas)

**Usage**:
- `/sdd.backlog` → List all backlog items
- `/sdd.backlog add` → Add new item interactively
- `/sdd.backlog add --audio` → Add item via voice description
- `/sdd.backlog pick` → Select item and create feature
- `/sdd.backlog resolve <ID>` → Mark item as resolved

---

## Quick Help

> `/sdd.backlog help` → Shows this summary

**Syntax**: `/sdd.backlog [action] [options]`

| Flag | Description |
|------|-------------|
| (none) | List all backlog items |
| `add` | Add new item interactively |
| `add --audio` | Add new item via voice description |
| `pick` | Select item and create feature |
| `resolve <ID>` | Mark item as resolved |
| `--type <T>` | Filter by type (TODO/DEBT/IDEA) |
| `--priority <P>` | Filter by priority |

**Examples**:
```bash
/sdd.backlog               # List all items
/sdd.backlog add           # Add new item
/sdd.backlog add --audio   # Describe item via voice
/sdd.backlog pick TODO-003 # Create feature from item
```

**See also**: `/sdd.help backlog` for detailed documentation

---

CRITICAL: USER INTERACTION RULES
When this skill shows JSON for AskUserQuestion, you MUST:
  1. CALL the AskUserQuestion TOOL with that exact JSON
  2. DO NOT print options using Bash (no echo, cat, printf)
  3. DO NOT ask "Which option?" as text
  4. Tables marked "REFERENCE ONLY" are for docs - do NOT print

---

## Purpose

Centralized system for capturing and managing:
1. **TODOs** - Pending technical tasks
2. **DEBT** - Consciously documented technical debt
3. **IDEAS** - Future improvements and suggestions

**Location**: `sdd/backlog.md` (centralized global file)

---

## Quick Reference

| Command | What it does |
|---------|--------------|
| `/sdd.backlog` | List items sorted by priority |
| `/sdd.backlog add` | Interactive flow to add item |
| `/sdd.backlog pick` | Create feature from selected item |
| `/sdd.backlog resolve <ID>` | Mark item as resolved |

---

## `/sdd.backlog` - List Backlog

### Standard Mode

```
/sdd.backlog
```

Shows all items sorted by priority:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📋 Technical Backlog
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Summary: 5 items (2 TODO, 1 DEBT, 2 IDEA)

HIGH PRIORITY:
  TODO-001  [High]   Refactor payment validation        (M)

MEDIUM PRIORITY:
  TODO-002  [Medium] Add retry logic to API calls       (S)
  IDEA-001  [Medium] Cache search results               (M)

LOW PRIORITY:
  DEBT-001  [Low]    Migrate callbacks to async/await   (L)
  IDEA-002  [Low]    Metrics dashboard                  (M)

IN PROGRESS:
  TODO-003  [High]   Optimize DB queries                (M)
            └── Feature: sdd/wip/db-optimization/

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Use /sdd.backlog pick to create feature from item
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### If no backlog exists:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📋 Technical Backlog
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

No backlog items found.

Use /sdd.backlog add to create your first item.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## `/sdd.backlog add` - Add Item

Interactive flow to add a new item to the backlog:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
➕ Add Item to Backlog
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Type? [TODO / DEBT / IDEA]: TODO

Title: Add structured logging

Priority? [High / Medium / Low]: Medium

Context (why is this needed?):
> Current logging is plain text, hard to parse in DataDog

Affected files (optional, comma-separated):
> src/utils/logger.ts, src/index.ts

Complexity? [S / M / L / XL]: M

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Added: TODO-003 - Add structured logging

Item saved to: sdd/backlog.md
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### Fields by Type

**TODO**:
- Title, Priority, Context, Affected Files, Complexity

**DEBT**:
- Title, Priority, Context, Affected Files, Complexity
- Risk if Ignored (what happens if not resolved)

**IDEA**:
- Title, Priority, Context
- Potential Impact (Performance, UX, Maintainability, etc.)
- Notes (additional details)

---

## `/sdd.backlog pick` - Create Feature from Item

Interactive selection to create a feature from a backlog item:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🎯 Pick Item to Create Feature
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Select item (enter number or ID):

  [1] TODO-001  Refactor payment validation  (High, M)
  [2] TODO-002  Add retry logic              (Medium, S)
  [3] DEBT-001  Migrate callbacks to async   (Low, L)
  [4] IDEA-001  Search cache                 (Medium, M)
  [5] IDEA-002  Metrics dashboard            (Low, M)

Choice: 1

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Creating feature from TODO-001...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Feature name suggestion: refactor-payment-validation
Accept? [Y/n/custom]: Y

✅ Feature created: sdd/wip/refactor-payment-validation/

Pre-populated context:
  • Problem Statement: from TODO-001 context
  • Affected Files: src/validators/payment.ts
  • Complexity: Medium

TODO-001 marked as: in-progress
  └── Linked to: sdd/wip/refactor-payment-validation/
```

### Workflow Mode Detection (DEBT/TODO only)

> **Lazy-loaded**: When processing DEBT/TODO items (not IDEA), Read `references/workflow-modes.md` for detailed workflow mode definitions and processing rules.

### Auto-Generated Functional Spec Template

> **Lazy-loaded**: When using modes 2/3 (auto-generation), Read `references/auto-spec-template.md` for the auto-specification generation template.

### What Gets Pre-populated

When creating a feature from backlog:

1. **meta.md** includes:
   ```yaml
   from_backlog: TODO-001
   workflow_mode: full | technical-only | tasks-only
   auto_generated:
     functional: false | true
     technical: false | true
   ```

2. **Initial context** for `/sdd.spec`:
   - Problem Statement: from item's Context field
   - Suggested Files: from item's Affected Files field
   - Complexity: from item's Complexity field

---

## `/sdd.backlog resolve <ID>` - Resolve Item

```
/sdd.backlog resolve TODO-001
```

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Resolving TODO-001
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Item: Refactor payment validation
Status: in-progress
Linked Feature: sdd/wip/refactor-payment-validation/

Resolution type?
  [1] Completed - Implemented in a feature
  [2] Won't Do - No longer relevant
  [3] Duplicate - Merged with another item

Choice: 1

Resolved in feature (optional): refactor-payment-validation

✅ TODO-001 marked as RESOLVED
   Resolution: Completed
   Resolved in: refactor-payment-validation
   Date: 2025-12-10

Item moved to "Resolved Items" section in backlog.md
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Auto-capture During Build

During `/sdd.build` and `/sdd.fix`, the agent can detect potential improvements:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💡 Potential Improvement Detected
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

While implementing TASK-005, I noticed:
"The error handling here is minimal - should add proper retry logic"
```

**⛔ INVOKE TOOL (do not print this, CALL the tool):**

```
AskUserQuestion(
  questions=[{
    "question": "What would you like to do with this improvement?",
    "header": "Action",
    "options": [
      {"label": "Fix now", "description": "Address immediately in current task"},
      {"label": "Add as TODO", "description": "Track for later implementation"},
      {"label": "Add as DEBT", "description": "Document as technical debt"},
      {"label": "Skip", "description": "Not important, ignore"}
    ],
    "multiSelect": false
  }]
)
```

Note: For "IDEA" (future improvement suggestion), user can select "Other" and specify.

### If "Fix now" is chosen:

```
User selected: Fix now

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔧 Fixing Now
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Implementing retry logic in src/services/payment-api.ts...

[Agent implements the fix as part of current task]

✅ Fixed: Added retry logic with exponential backoff
   Files modified: src/services/payment-api.ts

Continuing with TASK-005...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### If adding to backlog:

```
Choice: T

Title suggestion: Add retry logic to payment API calls
Accept? [Y/n/custom]: Y

✅ Added TODO-004 to backlog
   Origin: feature/payment-gateway (TASK-005)

Continuing with TASK-005...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### Criteria for "Fix Now" vs "Add to Backlog"

| Criterion | Fix Now | Add to Backlog |
|----------|---------|----------------|
| Estimated effort | Low (trivial change) | Medium/High |
| Related to current task | Yes | Not directly |
| Risk if not done | High (bugs, security) | Low (improvement) |
| Complexity | Low | High |
| Scope creep | Does not expand scope | Would expand scope |

**Golden rule**: If the fix is small and directly related to what you're doing, offer "Fix Now" as the first option.

---

## Backlog File Format

### Location: `sdd/backlog.md`

```markdown
# Technical Backlog

> Items captured during development. Use `/sdd.backlog` to manage.

**Last Updated**: 2025-12-10
**Total Items**: 5 (2 TODO, 1 DEBT, 2 IDEA)

---

## 📋 TODOs

### TODO-001: Refactor payment validation
- **Priority**: High
- **Status**: pending
- **Created**: 2025-12-01
- **Origin**: feature/payment-gateway (during /sdd.build)
- **Context**: Current validator uses fragile regex, should use library
- **Affected Files**: src/validators/payment.ts
- **Complexity**: Medium

---

## 🔧 Technical Debt

### DEBT-001: Migrate from callbacks to async/await
- **Priority**: Low
- **Status**: pending
- **Created**: 2025-11-20
- **Origin**: feature/legacy-import
- **Context**: Import module uses legacy callbacks
- **Affected Files**: src/importers/*.ts
- **Complexity**: High
- **Risk if Ignored**: Degraded maintainability

---

## 💡 Ideas

### IDEA-001: Search results cache
- **Priority**: Medium
- **Status**: pending
- **Created**: 2025-12-03
- **Origin**: feature/search-optimization
- **Context**: Could improve performance 10x
- **Potential Impact**: Performance
- **Notes**: Evaluate Redis vs in-memory

---

## ✅ Resolved Items

### TODO-002: Add structured logging
- **Priority**: Medium
- **Status**: resolved
- **Created**: 2025-11-25
- **Resolved**: 2025-12-08
- **Resolution**: Completed
- **Resolved In**: feature/observability-upgrade
```

---

## AI Agent Instructions


### Help Flag Detection

**WHEN** the user runs `/sdd.backlog help`:
1. Output ONLY the "Quick Help" section (not full documentation)
2. Do NOT execute backlog logic
3. Keep response concise (~15 lines)

### --audio Flag Detection (with add)

### Key Rules

1. **Centralized file**: Always use `sdd/backlog.md`
2. **Unique IDs**: Format `{TYPE}-{NNN}` (TODO-001, DEBT-002, IDEA-003)
3. **Auto-increment**: Next ID = max(existing IDs for type) + 1
4. **Origin tracking**: Always record which feature/context the item came from
5. **Non-blocking**: Items are informational, never block `/sdd.finish`
6. **Create file if missing**: First time `/sdd.backlog` is used

### Workflow Mode Rules (for `/sdd.backlog pick`)

7. **DEBT/TODO detection**: After feature creation, detect if item is DEBT or TODO → offer 3 workflow modes
8. **IDEA always full**: IDEA items skip the workflow mode question and use full pipeline
9. **Auto-generated spec header**: Auto-generated specs MUST include `> **Auto-generated** from backlog item [ID]` at the top
10. **Functional spec format**: Auto-generated functional specs use the lite format (problem + 1-2 stories + scope)
11. **Technical spec exploration**: Auto-generated technical specs (mode 3) REQUIRE codebase exploration before generation — use `sdd-explorer` and `sdd-system-designer` subagents to analyze affected files
12. **meta.md tracking**: Both `workflow_mode` and `auto_generated` flags MUST be set in meta.md
13. **Stage history**: Auto-approved stages must set `approved_by: auto-generated` and `approved_at` with current timestamp
14. **Backward compatibility**: Features without `workflow_mode` in meta.md default to `full` mode — all existing flows remain unchanged

### Linking with Features

When creating a feature from an item:
1. Mark item as `in-progress` with link to feature
2. In feature's `meta.md` add: `from_backlog: TODO-001`
3. When feature completes (`/sdd.finish`), offer to resolve the item

### During Build

When the agent detects a potential improvement:
1. Pause and show options to user
2. If "Fix now": implement immediately, don't add to backlog
3. If backlog: add item with origin from current feature/task
4. Continue with original task

---

## Related Commands

- `/sdd.start` - Create feature (can use `--from-backlog ID`)
- `/sdd.check` - Shows backlog summary if exists
- `/sdd.finish` - Offers to resolve items related to feature
- `/sdd.build` - Can detect and capture items automatically

---

## Examples

### Example 1: Add TODO during development

```
User: /sdd.backlog add

AI: ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    ➕ Add Item to Backlog
    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    Type? [TODO / DEBT / IDEA]:

User: TODO

AI: Title:

User: Implement API rate limiting

AI: Priority? [High / Medium / Low]:

User: High

AI: Context (why is this needed?):

User: Without rate limiting, API is vulnerable to abuse

AI: Affected files (optional):

User: src/middleware/rateLimit.ts

AI: Complexity? [S/M/L/XL]:

User: M

AI: ✅ Added: TODO-005 - Implement API rate limiting
```

### Example 2: Create feature from backlog

```
User: /sdd.backlog pick

AI: [Shows list of items]

User: 1

AI: Feature name suggestion: implement-rate-limiting
    Accept? [Y/n/custom]:

User: Y

AI: ✅ Feature created!
    Run /sdd.spec to continue.
```
