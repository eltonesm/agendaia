---
name: sdd.help
description: Show help and documentation for SDD Kit commands and workflow. Use when user asks about available commands or how to use the framework.
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

# Command: /sdd.help

**Description**: Show available commands and quick reference

**Usage**:
- `/sdd.help` → Show all commands organized by category
- `/sdd.help [command]` → Show detailed help for specific command
- `/sdd.help workflow` → Show workflow diagram

---

## Quick Help

> `/sdd.help help` → Shows this summary

**Syntax**: `/sdd.help [command]`

| Flag | Description |
|------|-------------|
| (none) | Show all commands organized by category |
| `[command]` | Show detailed help for specific command |
| `workflow` | Show workflow diagram |

**Example**:
```bash
/sdd.help              # Show all commands
/sdd.help spec         # Help for /sdd.spec
/sdd.help workflow     # Show workflow diagram
```

**See also**: Each command also supports `help` flag (e.g., `/sdd.spec help`)

---

CRITICAL: USER INTERACTION RULES
When this skill shows JSON for AskUserQuestion, you MUST:
  1. CALL the AskUserQuestion TOOL with that exact JSON
  2. DO NOT print options using Bash (no echo, cat, printf)
  3. DO NOT ask "Which option?" as text
  4. Tables marked "REFERENCE ONLY" are for docs - do NOT print

## Purpose

Quick reference for all SDD Kit commands. Shows commands organized by workflow phase and utility functions.

---

## Output: Command List

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📚 SDD Kit - Command Reference
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🚀 WORKFLOW COMMANDS (in order)
────────────────────────────────────────
/sdd.start "name"     Initialize new feature
/sdd.start --reopen feature-name Reopen completed feature for iteration
/sdd.spec             Create functional & technical specs
/sdd.plan             Generate and approve tasks
/sdd.build            Implement tasks
/sdd.finish           Validate and archive feature

⚡ EXPRESS MODE
────────────────────────────────────────
/sdd.go "name"        Full workflow in one command

📊 STATUS & INFO
────────────────────────────────────────
/sdd.check            View feature status & progress
/sdd.check --sync     Verify consistency between layers
/sdd.check --compliance  Verify tests/lint compliance
/sdd.list             List all features (wip/completed)
/sdd.help             Show this help

🔧 UTILITIES
────────────────────────────────────────
/sdd.fix              Fix errors across all layers
/sdd.backlog            Manage backlog (TODOs, Debt, Ideas)
/sdd.doctor           Diagnose project config health for kit usage
/sdd.rollback [phase] Rollback to previous phase
/sdd.cancel           Cancel feature (archive)

🏗️ SETUP
────────────────────────────────────────
/sdd.import           Import existing specs
/sdd.reverse-eng      Reverse engineer from code

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

💡 Quick Start:
   /sdd.start feature-name  → then follow prompts

📖 Detailed help:
   /sdd.help [command]      → e.g., /sdd.help spec

📁 Full docs:
   sdd-kit/framework/README.md

```

---

## Output: Specific Command Help

When user requests help for specific command:

```
/sdd.help spec
```

Shows:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📖 /sdd.spec
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Create functional and technical specifications

USAGE:
  /sdd.spec                    Auto-detect phase, behavior by mode
  /sdd.spec functional         Functional spec only
  /sdd.spec technical          Technical spec only
  /sdd.spec --audio            Voice-enabled spec creation (record description)
  /sdd.spec --include "url"    Add external context (Jira, Confluence, file)
  /sdd.spec --iterate "change" Modify/update spec after creation
  /sdd.spec functional --approve   Approve functional

COMMON SCENARIOS:
  Add Jira ticket context:    /sdd.spec --include "https://jira.../PROJ-123"
  Update spec requirements:   /sdd.spec --iterate "add rate limiting"
  Include local file:         /sdd.spec --include "docs/requirements.md"
  Add inline text:            /sdd.spec --include "forgot: we need X"
  Multiple contexts:          /sdd.spec --include "text 1" --include "text 2"

BEHAVIOR BY MODE:
  Express:  3-5 questions, auto-generates both, auto-approves
  Standard: Interactive interview, "Ready to approve?" prompts

OUTPUT:
  sdd/wip/[feature]/1-functional/spec.md
  sdd/wip/[feature]/2-technical/spec.md

NEXT: /sdd.plan

Full docs: sdd-kit/framework/skills/sdd.spec/SKILL.md
```

---

## Output: Workflow Diagram

```
/sdd.help workflow
```

Shows:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔄 SDD Kit Workflow
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Standard Flow:
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  /sdd.start ──► /sdd.spec ──► /sdd.plan ──► /sdd.build │
│       │              │              │              │        │
│       ▼              ▼              ▼              ▼        │
│   [meta.md]    [functional]    [tasks.json]    [code +       │
│                [technical]                    tests]        │
│                                                     │       │
│                                                     ▼       │
│                                              /sdd.finish   │
│                                                     │       │
│                                                     ▼       │
│                                            [archived to     │
│                                             sdd/features/] │
└─────────────────────────────────────────────────────────────┘

Express Flow (one command):
┌─────────────────────────────────────────────────────────────┐
│  /sdd.go "feature" ──► [All steps automated] ──► Done!    │
└─────────────────────────────────────────────────────────────┘

Recovery & Validation Commands:
┌─────────────────────────────────────────────────────────────┐
│  /sdd.check --sync      → Validate layer consistency      │
│  /sdd.check --compliance → Validate tests/lint       │
│  /sdd.fix               → Fix errors, update all layers   │
│  /sdd.rollback          → Go back to previous phase       │
│  /sdd.cancel            → Archive and stop feature        │
└─────────────────────────────────────────────────────────────┘

Execution Modes:
┌─────────────────────────────────────────────────────────────┐
│  express   → Minimal interaction, auto-advance             │
│  standard  → Balanced control (DEFAULT)                    │
│                                                             │
│  Set at start: /sdd.start "feature" --express             │
└─────────────────────────────────────────────────────────────┘
```

---

## Command Quick Reference Table

| Command | Purpose | Typical Usage |
|---------|---------|---------------|
| `/sdd.start` | Initialize feature | `/sdd.start "user-auth"` |
| `/sdd.start --reopen` | Reopen completed feature | `/sdd.start --reopen user-auth --phase 2` |
| `/sdd.spec` | Create specifications | `/sdd.spec` |
| `/sdd.spec --audio` | Voice-enabled spec creation | `/sdd.spec --audio` |
| `/sdd.spec --include` | Add external context (Jira/Confluence/file) | `/sdd.spec --include "url"` |
| `/sdd.spec --iterate` | Modify/update spec after creation | `/sdd.spec --iterate "change"` |
| `/sdd.plan` | Generate tasks | `/sdd.plan` |
| `/sdd.build` | Implement | `/sdd.build` |
| `/sdd.finish` | Complete & archive | `/sdd.finish` |
| `/sdd.go` | Full auto workflow | `/sdd.go "feature"` |
| `/sdd.check` | View status | `/sdd.check` |
| `/sdd.check --sync` | Verify layer consistency | `/sdd.check --sync` |
| `/sdd.check --compliance` | Verify tests/lint | `/sdd.check --compliance` |
| `/sdd.list` | List features | `/sdd.list` |
| `/sdd.fix` | Fix errors | `/sdd.fix "error msg"` |
| `/sdd.backlog` | Manage backlog | `/sdd.backlog list` |
| `/sdd.doctor` | Diagnose project config | `/sdd.doctor` |
| `/sdd.doctor --apply` | Apply config fixes | `/sdd.doctor --apply` |
| `/sdd.rollback` | Undo/revert to previous phase | `/sdd.rollback 2` |
| `/sdd.cancel` | Cancel feature | `/sdd.cancel` |
| `/sdd.import` | Import specs | `/sdd.import` |
| `/sdd.reverse-eng` | From code | `/sdd.reverse-eng` |
| `/sdd.help` | Show help | `/sdd.help` |

---

## AI Agent Instructions


### Help Flag Detection

**WHEN** the user runs `/sdd.help help`:
1. Output ONLY the "Quick Help" section (not full documentation)
2. Do NOT execute full help listing
3. Keep response concise (~15 lines)

### Key Behaviors

1. **No arguments**: Show full command list
2. **With command name**: Show detailed help for that command (read from `sdd-kit/framework/skills/sdd.[command]/SKILL.md`)
3. **With "workflow"**: Show workflow diagram
4. **Unknown command**: Suggest similar commands

### Implementation

```python
def handle_help(args):
    if not args:
        # Show full command list
        show_command_list()
    elif args == "workflow":
        # Show workflow diagram
        show_workflow_diagram()
    elif args in VALID_COMMANDS:
        # Read and summarize specific command doc
        show_command_help(args)
    else:
        # Suggest similar
        suggest_similar(args)
```

### Response Style

- Keep output concise and scannable
- Use consistent formatting
- Always show "next steps" or related commands
- Point to full docs for detailed information

---

## Related Commands

- All commands listed above
- `sdd-kit/framework/README.md` for full documentation
- `sdd-kit/framework/TUTORIAL.md` for guided walkthrough

---
