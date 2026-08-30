---
name: sdd.list
description: List all features in the meli workspace with their status and phase. Use when user wants to see current features.
model: haiku
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

# Command: /sdd.list

**Description**: List all features (WIP and completed)

**Usage**: `/sdd.list`

---

## Quick Help

> `/sdd.list help` → Shows this summary

**Syntax**: `/sdd.list [flags]`

| Flag | Description |
|------|-------------|
| (none) | List all features (WIP + completed) |
| `--status <S>` | Filter by status |
| `--format <F>` | Output format (table/json) |

**Example**:
```bash
/sdd.list                # List all features
```

**See also**: `/sdd.help list` for detailed documentation

---

CRITICAL: USER INTERACTION RULES
When this skill shows JSON for AskUserQuestion, you MUST:
  1. CALL the AskUserQuestion TOOL with that exact JSON
  2. DO NOT print options using Bash (no echo, cat, printf)
  3. DO NOT ask "Which option?" as text
  4. Tables marked "REFERENCE ONLY" are for docs - do NOT print

## Purpose

Shows overview of all features in the project:
- Features in progress (from `sdd/wip/`)
- Completed features (from `sdd/features/`)
- Quick stats for each

---

## Workflow

### 1. Check for Features Without Date Prefix

Before listing, check if any features use the legacy NNN- prefix format:

```bash
# Find features without date prefix (don't start with 8 digits)
unnumbered_wip=$(ls -1 sdd/wip/ 2>/dev/null | grep -vE '^[0-9]{8}-')
unnumbered_features=$(ls -1 sdd/features/ 2>/dev/null | grep -vE '^[0-9]{8}-')
```

**If legacy features are found**: Display them normally in the list. Do NOT rename or migrate them automatically. Legacy NNN-prefixed features continue to work with all commands.

Show a one-line note at the end of the output:
```
ℹ️  Some features use legacy numbering (NNN-). New features use date prefix (YYYYMMDD-). Both formats are supported.
```

---

### 2. Scan Features (Deterministic)

Use scripts for comprehensive feature and spec scanning.

```bash
# Scan all features with detailed metadata
features_result=$(bash sdd-kit/framework/tools/extraction/scan-features.sh sdd --json)

# Extract counts
wip_count=$(echo "$features_result" | grep -o '"wip_count":[0-9]*' | cut -d: -f2)
completed_count=$(echo "$features_result" | grep -o '"completed_count":[0-9]*' | cut -d: -f2)
total_count=$(echo "$features_result" | grep -o '"total_count":[0-9]*' | cut -d: -f2)

# Extract feature lists with metadata
wip_features=$(echo "$features_result" | grep -o '"wip":\[[^]]*\]')
completed_features=$(echo "$features_result" | grep -o '"completed":\[[^]]*\]')

echo "📊 Features scanned: $total_count total ($wip_count WIP, $completed_count completed)"
```

**Feature metadata extracted**:
- Feature number and name
- Current stage (functional/technical/tasks/implementation)
- Task progress (completed/total)
- Creation and completion dates
- Project type (prototype/mvp/production)

```bash
# Scan specs for status overview
specs_result=$(bash sdd-kit/framework/tools/extraction/scan-specs.sh sdd --json)

# Extract spec statistics
functional_specs=$(echo "$specs_result" | grep -o '"functional_count":[0-9]*' | cut -d: -f2)
technical_specs=$(echo "$specs_result" | grep -o '"technical_count":[0-9]*' | cut -d: -f2)
approved_count=$(echo "$specs_result" | grep -o '"approved_count":[0-9]*' | cut -d: -f2)
pending_count=$(echo "$specs_result" | grep -o '"pending_count":[0-9]*' | cut -d: -f2)

echo "📋 Specs: $functional_specs functional, $technical_specs technical"
echo "   Approved: $approved_count, Pending: $pending_count"
```

**Spec metadata extracted**:
- Spec type (functional/technical)
- Approval status
- User story count
- E2E scenario count
- Conflict status

---

### 4. Display WIP Features

For each feature in `sdd/wip/`:
- Extract feature date and name from directory name
- Read `meta.md` for current stage
- Read task progress if in implementation
- Show summary ordered by number

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔄 Features In Progress
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

┌───────┬──────────────────────┬─────────────┬───────────────────┐
│ #     │ Feature              │ Stage       │ Progress          │
├───────┼──────────────────────┼─────────────┼───────────────────┤
│ 003   │ user-authentication  │ impl        │ 42% (7/17 tasks)  │
│ 004   │ payment-integration  │ technical   │ Spec in review    │
│ 005   │ notification-system  │ functional  │ Draft complete    │
└───────┴──────────────────────┴─────────────┴───────────────────┘

Total WIP: 3 features
```

---

### 5. Display Completed Features

For each feature in `sdd/features/`:
- Extract feature date and name from directory name
- Read `meta.md` for completion date
- Show key metrics

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Completed Features
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

┌───────┬──────────────────────┬──────────────┬────────┬───────────┐
│ #     │ Feature              │ Completed    │ Tasks  │ Duration  │
├───────┼──────────────────────┼──────────────┼────────┼───────────┤
│ 001   │ initial-setup        │ 2025-01-02   │ 5      │ 2 days    │
│ 002   │ api-versioning       │ 2025-01-05   │ 8      │ 3 days    │
└───────┴──────────────────────┴──────────────┴────────┴───────────┘

Total Completed: 2 features
Average Duration: 2.5 days
Average Tasks: 6.5 tasks/feature
```

---

### 6. Project Statistics

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 Project Statistics
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Total Features: 6 (3 in progress, 3 completed)

Velocity Trends:
• Average completion: 5 days
• Average tasks: 11.7/feature
• Estimation accuracy: 90% (avg)

Active Work:
• Features in functional phase: 1
• Features in technical phase: 1
• Features in implementation: 1

Next to Complete:
• payment-integration (42% done, ETA: 2 days)
```

---

### 7. Show Recommendations

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📝 Recommendations
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🎯 Focus on: payment-integration (closest to completion)
   Next: /sdd.check payment-integration

⚠️  Action needed:
   • user-authentication stuck in technical phase for 3 days
     Consider: /sdd.spec technical --include

💡 Consider starting:
   • Only 3 features active (team capacity for more?)
```

---

## Output Modes

### Compact Mode

Just the essentials:

```
WIP (3):
• payment-integration (impl, 42%)
• user-authentication (technical)
• dark-mode (functional)

Completed (3):
• email-notifications (2025-11-20)
• api-versioning (2025-11-15)
• logging-system (2025-11-10)

Total: 6 features
```

### Detailed Mode (Default)

Full tables with statistics (shown above).

---

## Context Loading

Read:
- All `meta.md` files in `sdd/wip/*/`
- All `meta.md` files in `sdd/features/*/`
- Progress files if in implementation

---

## Output Expected

**User sees**: Complete overview of project

**Can identify**: Which features need attention

**Next actions**: Suggested based on status

## AI Agent Instructions


### Help Flag Detection

**WHEN** the user runs `/sdd.list help`:
1. Output ONLY the "Quick Help" section (not full documentation)
2. Do NOT execute list logic
3. Keep response concise (~15 lines)

### Key Rules
1. **Say "I don't know"** when uncertain - Don't make up information
2. **Offer options** instead of assuming - Present 2-4 alternatives when valid approaches exist
3. **Think before acting** - Use `<thinking>` blocks for complex decisions
4. **Only answer if confident** - Ask for clarification when confidence is low
5. **Verify before generating** - Confirm understanding before creating content

### Interactive Next Steps (After List Display)

> **MANDATORY**: Always offer interactive selection after displaying the list.

**⛔ INVOKE TOOL (do not print this, CALL the tool):**

```
AskUserQuestion(
  questions=[{
    "question": "What would you like to do?",
    "header": "Action",
    "options": [
      {"label": "/sdd.start", "description": "Create a new feature"},
      {"label": "/sdd.check <feature>", "description": "Check specific feature status"},
      {"label": "/sdd.backlog", "description": "View backlog items"}
    ],
    "multiSelect": false
  }]
)
```

**On user selection**:

| Selection | Action |
|-----------|--------|
| /sdd.start | `Skill(skill="sdd.start")` |
| /sdd.check <feature> | Ask which feature, then `Skill(skill="sdd.check", args="<feature>")` |
| /sdd.backlog | `Skill(skill="sdd.backlog")` |
| Other | User types custom input |

> **NOTE**: For "/sdd.check <feature>", if user selects this option, prompt them to select which feature from the displayed list before invoking.

---

## Related Commands

- `/sdd.check` - Detailed status of specific feature
- `/sdd.start` - Start new feature
- `/sdd.build` - Continue implementation

---
