---
name: sdd.import
description: Import external specifications or existing code into the SDD workflow. Use when user has existing specs or wants to onboard existing code.
model: sonnet
argument-hint: "[source]"
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

# Command: /sdd.import

**Description**: Import existing specifications (OpenAPI, architecture docs) into a new feature

**Usage**:
- `/sdd.import [path]` → Import specs from path
- `/sdd.import` → Interactive mode, prompts for path

---

## Quick Help

> `/sdd.import help` → Shows this summary

**Syntax**: `/sdd.import [path] [flags]`

| Flag | Description |
|------|-------------|
| (none) | Interactive mode, prompts for path |
| `[path]` | Import specs from specific path |
| `--from <url>` | Import from URL |
| `--type <T>` | Force type (openapi/markdown) |

**Supported**: OpenAPI 3.x, Markdown, JSON Schema

**Example**:
```bash
/sdd.import ./api-spec.yaml    # Import OpenAPI spec
```

**See also**: `/sdd.help import` for detailed documentation

---

CRITICAL: USER INTERACTION RULES
When this skill shows JSON for AskUserQuestion, you MUST:
  1. CALL the AskUserQuestion TOOL with that exact JSON
  2. DO NOT print options using Bash (no echo, cat, printf)
  3. DO NOT ask "Which option?" as text
  4. Tables marked "REFERENCE ONLY" are for docs - do NOT print

## Purpose

Allows teams to import existing specifications (OpenAPI specs, architecture documents, PRDs) into the SDD Kit framework, pre-populating feature structure and enabling brownfield workflow for existing systems.

---

## Supported Formats

| Format | Extensions | Auto-Detection |
|--------|------------|----------------|
| OpenAPI 3.x | `.yaml`, `.yml`, `.json` | Yes (looks for `openapi:`) |
| Markdown | `.md` | Yes |
| JSON Schema | `.json` | Yes (looks for `$schema`) |
| Plain text | `.txt` | Manual classification |

---

## Workflow

### 1. Detect Import Source

Supports file, directory, or URL.

### 2. Analyze and Classify Specs

Auto-detects:
- OpenAPI specs
- Functional documentation (PRDs, requirements)
- Technical documentation (architecture, design)
- Configuration files (skipped)

### 3. Get Feature Information

Prompts for feature name (kebab-case).

### 4. Determine Mode

- If `sdd/specs/` exists → Brownfield mode available
- Can optionally create system specs from imports

### 5. Import and Transform

- OpenAPI → Extracts endpoints, schemas
- PRD/Functional → Merges into functional-spec.md
- Architecture → Merges into technical-spec.md

### 6. Create Feature Structure

Creates standard folder structure with imported content.

---

## Output Example

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📥 Import External Specs
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Source: ./docs/

🔍 Scanning directory...

Found 4 files:
  1. [OpenAPI] api-spec.yaml (Orders API)
  2. [Technical] architecture.md
  3. [Functional] requirements.md
  4. [Config] database.yaml (skip)

[AskUserQuestion: "Import files 1-3?"] → User: Yes

Creating feature: orders-integration

✅ Import Complete!

📁 Feature created: sdd/wip/orders-integration/

Mode: greenfield

Imported:
  OpenAPI specs: 1
  Documentation: 2

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🎯 Interactive Next Steps
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

[AskUserQuestion: "Specs imported. Review and continue?"]
Options:
  ○ /sdd.spec (Recommended) - Review and complete specs
  ○ /sdd.check - View imported structure
  ○ Other...

On selection → Invoke corresponding Skill
```

---

## Examples

### Example 1: Import Single OpenAPI Spec

```
User: /sdd.import ./api/orders.yaml

AI: 📥 Importing specifications...

📄 Detected: OpenAPI 3.0.3 specification
   Title: Orders API
   Version: 2.1.0
   Endpoints: 12

Feature name: orders-api-integration

✅ Import complete!
```

### Example 2: Import Directory

```
User: /sdd.import ./docs/

AI: 🔍 Scanning directory...

Found 3 spec files.
Import all? [Y/n/select]
```

---

## Telemetry (Automatic)

> Session stats are captured automatically via hooks. No manual logging required.

---

## AI Agent Instructions


### Help Flag Detection

**WHEN** the user runs `/sdd.import help`:
1. Output ONLY the "Quick Help" section (not full documentation)
2. Do NOT execute import logic
3. Keep response concise (~15 lines)

### Key Behaviors

1. **Auto-detect spec types** - Don't ask user what type each file is
2. **Preserve original content** - Don't modify imported content
3. **Add structure** - Wrap in proper SDD Kit structure
4. **Validate after import** - Run validators to check completeness
5. **Interactive next steps** - Use AskUserQuestion (see below)

### Interactive Next Steps (After Import Complete)

> **MANDATORY**: Always offer interactive selection after import completes.

**⛔ INVOKE TOOL (do not print this, CALL the tool):**

```
AskUserQuestion(
  questions=[{
    "question": "Specs imported. Review and continue?",
    "header": "Next",
    "options": [
      {"label": "/sdd.spec (Recommended)", "description": "Review and complete specs"},
      {"label": "/sdd.check", "description": "View imported structure"}
    ],
    "multiSelect": false
  }]
)
```

**On user selection**:

| Selection | Action |
|-----------|--------|
| /sdd.spec (Recommended) | `Skill(skill="sdd.spec")` |
| /sdd.check | `Skill(skill="sdd.check")` |
| Other | User types custom input |

---

## Related Commands

- `/sdd.start` - Start new feature
- `/sdd.spec` - Continue after import
- `/sdd.reverse-eng` - Full codebase reverse engineering

---
