---
name: sdd.go
description: Express mode - orchestrates start, spec, plan, build, and finish in one command. Use for rapid feature development when you want the full workflow automated end-to-end with minimal interaction.
model: opus
argument-hint: "[feature-description]"
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

# Command: /sdd.go

**Description**: Express mode - orchestrates the complete workflow using standard commands

**Usage**:
- `/sdd.go "feature-name"` → Full automatic workflow with explicit name
- `/sdd.go "feature description"` → Auto-derives name from description
- `/sdd.go --audio` → Record feature description via microphone
- `/sdd.go --resume` → Resume interrupted express workflow

---

## Quick Help

> `/sdd.go help` → Shows this summary

**Syntax**: `/sdd.go "feature-name" [flags]`

| Flag | Description |
|------|-------------|
| `"feature-name"` | Full automatic workflow with explicit name |
| `"description"` | Auto-derives name from description |
| `--audio` | Record feature description via microphone |
| `--resume` | Resume interrupted express workflow |

**Flow**: start → spec → plan → build → finish (3-5 questions total)

**Examples**:
```bash
/sdd.go "payment-gateway"    # Full express workflow
/sdd.go --audio              # Voice-driven express workflow
```

**See also**: `/sdd.help go` for detailed documentation

---

CRITICAL: USER INTERACTION RULES
When this skill shows JSON for AskUserQuestion, you MUST:
  1. CALL the AskUserQuestion TOOL with that exact JSON
  2. DO NOT print options using Bash (no echo, cat, printf)
  3. DO NOT ask "Which option?" as text
  4. Tables marked "REFERENCE ONLY" are for docs - do NOT print


## Architecture: Orchestrator Pattern

> **CRITICAL**: `/sdd.go` is an **orchestrator**, NOT a standalone implementation.
> It invokes standard commands with express mode rules. **DO NOT duplicate logic here.**

**Flow**: `/sdd.start --express` → `/sdd.spec` → `/sdd.plan` → `/sdd.build` → `/sdd.finish`

**Express Rules**:
- 3-5 critical questions only
- Auto-advance between steps
- Predefined defaults
- Same LTP rules as standard

---

## Purpose

One-command feature development for simple, well-understood features.

**Good for**: Simple features, quick prototypes, clear requirements
**Not for**: Complex integrations, extensive design decisions, unclear requirements

---

## Express Rules

### 1. Consolidated Questions (3-5 only)

| # | Question | Purpose | Triggers |
|---|----------|---------|----------|
| 1 | What's the main feature? | Problem statement | Always |
| 2 | Who uses it? | User context | Always |
| 3 | Technical constraints? | Architecture decisions | Always |
| 4 | External integrations? | Dependencies | If mentioned → auto-discovery |
| 5 | Security requirements? | Security design | If sensitive data |
| 6 | LTP E2E Testing? [Y/N] | E2E test generation | Always (user chooses) |

### 2. Predefined Defaults

| Decision | Express Default |
|----------|-----------------|
| Execution strategy | Batched |
| Test coverage target | 80% |
| Template | Full (not Lite) |
| JVM language | Java (not Kotlin) |
| | Always enforced |

### 3. Auto-Advance Behavior

- No confirmation prompts between steps
- No "proceed?" questions - just continue
- Pause only on: errors, missing env vars, security decisions, consolidated questions

### 4. LTP E2E Question

Same rules as standard mode. Reference: `spec.md` → "LTP E2E Testing Decision"

---

## Execution Flow

| Step | Command | Reference | Override |
|------|---------|-----------|----------|
| 0 | Input validation | - | Derive name if description |
| 1 | `/sdd.start "<name>" --express` | `start.md` → "Express Mode" | - |
| 2 | `/sdd.spec` | `spec.md` → "Express Mode" | Consolidated questions |
| 3 | `/sdd.plan` | `plan.md` → "Express Mode" | Auto-select Batched |
| 4 | `/sdd.build` | `build.md` → "Express Mode" | Auto-retry 2x max |
| 5 | `/sdd.finish` | `finish.md` → "Express Mode" | All validations mandatory |

---

## AI Agent Instructions

### Help Flag Detection

**WHEN** the user runs `/sdd.go help`:
1. Output ONLY the "Quick Help" section (not full documentation)
2. Do NOT execute go logic
3. Keep response concise (~15 lines)

### Hub Guard

**WHEN** `/sdd.go` is invoked, **BEFORE** any other step:
1. Run `detect-stack.sh --level` in the current directory
2. If result is `hub`:
   - Output: "`/sdd.go` does not support hubs. Use `/sdd.hub go \"description\"` for express mode, or `/sdd.hub start|spec|plan|build|finish` for standard mode."
   - **STOP** — do NOT proceed with the express workflow
3. If result is `app` or `unknown`: continue normally

### CRITICAL: Orchestrator Implementation

When `/sdd.go` is invoked:
1. **DO NOT implement each step from scratch**
2. **DO reference and execute the standard commands**
3. **DO apply express rules as overrides**

### --audio Flag Detection

### Step 0: Input Validation

If input is valid kebab-case name → use directly
If input is description → derive name (extract key nouns, kebab-case), DO NOT ask confirmation

### Step 1: Initialize

Execute `/sdd.start "<feature-name>" --express`
→ Reference: `start.md` → "Express Mode (`--express`)" section

Includes: app verification,  creation if needed, scaffolding cleanup, git branch, meta.md with `execution_mode: express`

### Step 2: Specifications

Execute `/sdd.spec` (reads `execution_mode: express` from meta.md)
→ Reference: `spec.md` → "Express Mode" section

**Override**: Use consolidated questions instead of full interview.

### Step 3: Task Planning

Execute `/sdd.plan` (reads mode from meta.md)
→ Reference: `plan.md` → "Express Mode" section

**Override**: Auto-select "Batched" strategy, no confirmation.

### Step 4: Implementation

Execute `/sdd.build` (reads mode from meta.md)
→ Reference: `build.md` → "Express Mode" section

**Override**: Auto-retry failures (max 2x), then pause.

### Step 5: Finalization

Execute `/sdd.finish` (reads mode from meta.md)
→ Reference: `finish.md` → "Express Mode" section

All validations mandatory (, tests, code review, security, performance).

---

## Error Handling

If any step fails, show error details and options:
- (a) Fix and retry with appropriate command
- (b) Continue in standard mode: `/sdd.check`
- (c) Abort: `/sdd.cancel`

---

## Resume

`/sdd.go --resume` checks meta.md progress and continues from last incomplete step.

---

## Comparison with Standard

| Aspect | /sdd.go | Standard |
|--------|----------|----------|
| Commands | 1 (orchestrates 5) | 5 separate |
| Questions | 3-5 critical | Full interviews |
| Confirmations | None | At each phase |
| Time | ~10-20 min | ~1-2 hours |

---

## FURY-ONLY Constraint

Inherited from standard commands - see `spec.md`. Map external tech to  services.

---

## Key Principle

> **Standard commands are the single source of truth.**
> `/sdd.go` only defines: express rules, orchestration flow, error handling.
> For implementation details, read the corresponding command file.
