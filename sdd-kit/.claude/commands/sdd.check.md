---
name: sdd.check
description: View feature status, progress, and validation results. Use when user wants to check feature health or run validations.
model: sonnet
argument-hint: "[feature-name] [--sync]"
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

# Command: /sdd.check

**Description**: View feature status, run consistency checks, and validate compliance

**Usage**:
- `/sdd.check` → Current feature status overview
- `/sdd.check [feature]` → Specific feature status (by name, number, or full name)
- `/sdd.check --sync` → Check consistency between specs/tasks/code + propose fixes
- `/sdd.check --compliance` → Check tests/lint compliance + propose fixes
- `/sdd.check --project` → Validate PROJECT.md against framework standards
- `/sdd.check --version` → Check framework version compatibility
- `/sdd.check task TASK-XXX` → Specific task details
- `/sdd.check --resume` → List all resumable sessions across features
- `/sdd.check --resume --last` → Resume last interrupted session

**Feature Reference Formats**:
- By name: `/sdd.check user-auth`
- By full name: `/sdd.check 20260120-user-auth`

---

## Quick Help

> `/sdd.check help` → Shows this summary

**Syntax**: `/sdd.check [target] [flags]`

| Flag | Description |
|------|-------------|
| (none) | Current feature status overview |
| `[feature]` | Specific feature status |
| `--sync` | Check specs/tasks/code consistency |
| `--compliance` | Check tests/lint compliance |
| `--project` | Validate PROJECT.md against standards |
| `--version` | Check framework version compatibility |
| `task TASK-XXX` | Specific task details |
| `--resume` | List resumable sessions |

**Examples**:
```bash
/sdd.check                 # Current feature status
/sdd.check --sync          # Check consistency + propose fixes
/sdd.check 003             # Check feature 003 status
```

**See also**: `/sdd.help check` for detailed documentation

---

CRITICAL: USER INTERACTION RULES
When this skill shows JSON for AskUserQuestion, you MUST:
  1. CALL the AskUserQuestion TOOL with that exact JSON
  2. DO NOT print options using Bash (no echo, cat, printf)
  3. DO NOT ask "Which option?" as text
  4. Tables marked "REFERENCE ONLY" are for docs - do NOT print


## Subagent Delegation (MANDATORY for --sync)

> **⚠️ MANDATORY**: See [warning-hierarchy.md](../standards/warning-hierarchy.md#subagent-delegation) for the central principle.
> The `--sync` flag MUST delegate analysis to specialized subagents.

```bash
# Cross-layer analysis via GenAI Gateway (advisory)
result=$(bash sdd-kit/framework/tools/genai/genai-analyze-layers.sh sdd/wip/[feature])
if [ $? -ne 0 ]; then
    # Fallback to deterministic analysis
    result=$(bash sdd-kit/framework/tools/extraction/analyze-layers.sh sdd/wip/[feature] --json)
fi
```

**Skill for --compliance**:
| Check Type | Skill |
|------------|-------|
| Build/lint/coverage | `sdd-validator` |
| | `sdd-validator` |

---

## Purpose

Unified command for:
1. **Status** - View feature progress and metrics
2. **Sync** - Validate consistency between all framework layers (specs ↔ tasks ↔ code)
3. **Compliance** - Validate technical requirements (, tests, linting)

---

## Quick Reference

| Command | What it does |
|---------|--------------|
| `/sdd.check` | Status overview (read-only) |
| `/sdd.check --sync` | Consistency validation + fixes (y/n) |
| `/sdd.check --compliance` | Technical validation + fixes (y/n) |
| `/sdd.check --project` | PROJECT.md validation against standards |
| `/sdd.check --version` | Framework version compatibility |
| `/sdd.check task TASK-XXX` | Task details |
| `/sdd.check --resume` | List all resumable sessions |
| `/sdd.check --resume --last` | Resume last interrupted session |

---

## `/sdd.check` - Status Overview

### Express Mode

```
/sdd.check
```

Shows compact status:
```
Feature: payment-gateway
Stage: implementation (Phase 4/4)
Progress: 72% (13/18 tasks)
ETA: ~4h remaining

Next: Continue with /sdd.build
```

---

### Standard Mode (default)

```
/sdd.check
```

Shows detailed status with metrics:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 Feature Status: payment-gateway
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Feature ID: feat-042
Feature Number: 042
Created: 2025-11-20
Current Stage: implementation (Phase 4/4)
Mode: standard
Framework: v1.2.1 ✅   # or "v1.1.18 → v1.2.1 ⚠️ (run --version)"

────────────────────────────────────────
📈 Phase Progress
────────────────────────────────────────

Phase 1: Functional Spec    ✅ Completed (2025-11-20)
Phase 2: Technical Spec     ✅ Completed (2025-11-21)
Phase 3: Task Planning      ✅ Completed (2025-11-22)
Phase 4: Implementation     🔄 In Progress

────────────────────────────────────────
⚙️ Implementation Progress
────────────────────────────────────────

Progress: ████████████░░░░░░░░ 72% (13/18 tasks)

By Status:
✅ Completed:    13 tasks
🔄 In Progress:  2 tasks
⏸️ Blocked:      0 tasks
⏳ Pending:      3 tasks

────────────────────────────────────────
⏱️ Time Metrics
────────────────────────────────────────

• Time spent: 12.4h
• Estimated remaining: 4.2h
• Velocity: 8% faster than estimates

────────────────────────────────────────
📊 Quality Metrics
────────────────────────────────────────

• Tests: 67/67 passing (100%)
• Coverage: 89%
• Linter: 0 errors

────────────────────────────────────────
🎯 Next Actions
────────────────────────────────────────

1. Complete in-progress tasks (TASK-014, TASK-015)
2. Start pending tasks (TASK-016, TASK-017, TASK-018)
3. When done: /sdd.finish

────────────────────────────────────────
📋 Backlog Summary
────────────────────────────────────────

5 items in backlog:
  └── 2 High priority pending
  └── Use /sdd.backlog to view details
```

> **Note**: Backlog summary only shown if `sdd/backlog.md` exists and has items.

---

## `/sdd.check --sync` - Consistency Validation

Validates bidirectional consistency between all framework layers based on current phase.

### Phase-Aware Validation

| Current Phase | Layers Checked |
|---------------|----------------|
| `functional` | Only Functional Spec (nothing to compare) |
| `technical` | Functional ↔ Technical |
| `tasks` | Functional ↔ Technical ↔ Tasks |
| `implementation` | Functional ↔ Technical ↔ Tasks ↔ Code |

### Workflow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         /sdd.check --sync                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. DETECT CURRENT PHASE                                                    │
│     └── Read meta.md to determine phase                                     │
│                                                                             │
│  2. IDENTIFY EXISTING LAYERS                                                │
│     └── functional, technical, tasks, code (based on phase)                 │
│                                                                             │
│  3. VALIDATE BIDIRECTIONAL CONSISTENCY (per layer pair)                     │
│     ├── Functional → Technical: Every requirement has implementation?       │
│     ├── Technical → Functional: Everything traced to requirement?           │
│     ├── Technical → Tasks: Every contract has task?                         │
│     ├── Tasks → Technical: Every task maps to spec?                         │
│     ├── Tasks → Code: Every AC is implemented?                              │
│     └── Code → Tasks: Every code is documented?                             │
│                                                                             │
│  4. GENERATE INCONSISTENCY REPORT                                           │
│     └── List gaps with evidence                                             │
│                                                                             │
│  5. PROPOSE FIXES                                                           │
│     └── Specific changes to restore consistency                             │
│                                                                             │
│  6. APPLY FIXES (with y/n confirmation)                                     │
│     └── Update documents atomically                                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Deterministic Pre-Validation (Step 0)

Run deterministic validators before subagent analysis for efficiency.

```bash
# Step 0a: Validate spec alignment (functional ↔ technical)
alignment_result=$(bash sdd-kit/framework/tools/validation/validate-spec-alignment.sh sdd/wip/[feature] --json)
alignment_valid=$(echo "$alignment_result" | grep -o '"aligned":[^,}]*' | cut -d: -f2)
drift_count=$(echo "$alignment_result" | grep -o '"drift_count":[0-9]*' | cut -d: -f2)

if [ "$alignment_valid" != "true" ]; then
    echo "⚠️ Spec alignment issues detected ($drift_count drifts):"
    echo "$alignment_result" | grep -o '"drifts":\[[^]]*\]'
fi

# Step 0b: Run cross-layer analysis via GenAI Gateway (offloaded)
layers_result=$(bash sdd-kit/framework/tools/genai/genai-analyze-layers.sh sdd/wip/[feature])
genai_exit=$?

if [ "$genai_exit" -eq 0 ]; then
    # GenAI analysis succeeded - use enriched results
    verdict=$(echo "$layers_result" | grep -o '"verdict":"[^"]*"' | cut -d'"' -f4)
    echo "📊 Cross-layer analysis: $verdict"
elif [ "$genai_exit" -eq 2 ]; then
    # GenAI unavailable - fallback to subagent
    echo "🤖 Delegating to sdd-layer-analyzer for deep analysis..."
    # Use subagent for complex analysis
fi
```

**Deterministic checks**:
- **validate-spec-alignment.sh**: Detects drift between functional and technical specs
- **analyze-layers.sh**: Cross-layer consistency (functional → technical → tasks → code)

**Benefits**:
- Saves ~2,000-3,000 tokens vs LLM parsing
- Provides structured input for subagent
- Fast feedback on common issues

### Consistency Checks by Layer Pair

#### Functional ↔ Technical

**Functional → Technical:**
- Each User Story has endpoint/service implementing it
- Each Acceptance Criteria has technical validation/behavior
- Each NFR has implementation strategy

**Technical → Functional:**
- Each endpoint traces to a User Story (detect scope creep)
- Each data model traces to a requirement
- Each external integration is mentioned in functional

#### Technical ↔ Tasks

**Technical → Tasks:**
- Each endpoint has task(s) to implement it
- Each model has task to create it
- Each integration has configuration task

**Tasks → Technical:**
- Each task has technical spec backing it
- No orphan tasks without spec

#### Tasks ↔ Code (implementation phase only)

**Tasks → Code:**
- Each acceptance criteria has implementing code
- Each completed task has modified files

**Code → Tasks:**
- Each new function/class is documented in tasks
- No undocumented code

> **Lazy-loaded**: When `--sync` flag is used, Read `references/output-examples.md` section "## --sync examples" for output format reference.

---

## `/sdd.check --compliance` - Technical Validation

Validates technical compliance (, tests, linting) and proposes fixes.

### What It Checks

1. ** Compliance** (type-aware based on `.fury` file)
   - Dockerfile exists and uses valid  base image
   - Dockerfile.runtime exists (only for web services, NOT for CLIs)
   - Version consistency across files
   - /ping endpoint implemented (only for web services, NOT for CLIs)

   > **IMPORTANT**: Read `.fury` file to determine application type. If `type: cli`, skip Dockerfile.runtime and /ping checks.

2. **🔐 Secrets Compliance** (BLOCKER)
   - No hardcoded secrets in source code
   - No secrets in configuration files
   - No credentials in Dockerfiles
   - Secrets documented in technical spec

   #### Dockerfile Image Validation CRITICAL

   **Validation Rule**: ALL `FROM` statements MUST start with `your-registry/base-image

   ```bash
   # Check: Extract FROM statements and verify prefix
   grep -E "^FROM " Dockerfile Dockerfile.runtime 2>/dev/null | \
     grep -v "your-registry/base-image" && echo "❌ INVALID IMAGE" || echo "✅ OK"
   ```

   **Allowed Images** (ONLY THESE):

   | Language | Build Image | Runtime Image |
   |----------|-------------|---------------|
   | Java 21 | `your-registry/base-image | `your-registry/base-image |
   | Node.js 24 | `your-registry/base-image | `your-registry/base-image |
   | Go 1.25 | `your-registry/base-image | `your-registry/base-image |
   | Python 3.13 | `your-registry/base-image | `your-registry/base-image |

   **Example Error Output**:
   ```
   ❌ Dockerfile: INVALID BASE IMAGE
      Found: FROM eclipse-temurin:21-jdk
      Required: Image MUST start with "your-registry/base-image"

      Fix: Use your-registry/base-image
   ```

3. **Tests**
   - All tests passing
   - Coverage meets threshold (default: 80%)
   - No skipped tests without justification

4. **Linting**
   - No linter errors
   - No linter warnings (configurable)

5. **Dependencies**
   - No vulnerable dependencies
   - No outdated critical dependencies

> **Lazy-loaded**: When `--compliance` flag is used, Read `references/output-examples.md` section "## --compliance examples" for output format reference.

---

## `/sdd.check --project` - PROJECT.md Validation

Validates `sdd/PROJECT.md` against framework standards and manages override registration.

```bash
# Validate PROJECT.md via GenAI Gateway
result=$(bash sdd-kit/framework/tools/genai/genai-validate-project.sh .)
if [ $? -ne 0 ]; then
    # Fallback to deterministic validation
    result=$(bash sdd-kit/framework/tools/validation/validate-project.sh sdd/PROJECT.md --json)
fi
```

### What It Checks

1. **Coverage vs testing-strategy.md**
   - If `min_coverage < 80`: Requires registered override

2. **Technology vs tech-stack.md**
   - If `forbidden` contains framework-recommended libs: Requires override
   - If `orm` is not JPA/Hibernate: Warning for  compatibility

3. ** Compliance vs fury-guidelines.md**
   - Mandatory requirements cannot be overridden

4. **Project Type**
   - Must be: `prototype`, `mvp`, or `production`

> **Lazy-loaded**: When `--project` flag is used, Read `references/output-examples.md` section "## --project examples" for output format reference.

---

## `/sdd.check --version` - Framework Version & Spec Compatibility

Scans ALL specs in the project and validates them against current framework standards.

### Purpose

When users upgrade the framework mid-project, existing specs (system specs, completed features, WIP features) may have:
- Missing required sections (added in newer versions)
- Deprecated formats or patterns
- Incompatible structure with current templates

This check **scans the entire project** and validates all specs against current standards.

### What Gets Scanned

```
meli/
├── PROJECT.md                ← Validated (project config)
├── PATTERNS.md               ← Validated (cross-feature patterns)
├── backlog.md                ← Validated (TODO/DEBT/IDEA items)
├── specs/                    # System specs (brownfield base)
│   ├── architecture.md       ← Validated
│   ├── api-contracts/*.yaml  ← Validated
│   └── components.md         ← Validated
├── features/                 # Completed features
│   ├── 20250101-user-auth/
│   │   ├── 1-functional/spec.md  ← Validated
│   │   ├── 2-technical/spec.md   ← Validated
│   │   ├── 3-tasks/tasks.json      ← Validated
│   │   └── meta.md               ← Version checked
│   └── 20250115-payments/
│       └── ...               ← Validated
└── wip/                      # Work in progress
    └── 20250203-refunds/
        └── ...               ← Validated
```

### Workflow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         /sdd.check --version                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. READ CURRENT FRAMEWORK VERSION                                          │
│     └── cat sdd-kit/framework/VERSION                                         │
│                                                                             │
│  2. SCAN ALL SPEC LOCATIONS                                                 │
│     ├── sdd/PROJECT.md                                                     │
│     ├── sdd/PATTERNS.md (if exists)                                        │
│     ├── sdd/backlog.md (if exists)                                         │
│     ├── sdd/specs/**/*.md, *.yaml                                          │
│     ├── sdd/features/*/1-functional/spec.md                                │
│     ├── sdd/features/*/2-technical/spec.md                                 │
│     ├── sdd/features/*/3-tasks/tasks.json                                    │
│     ├── sdd/features/*/meta.md                                             │
│     ├── sdd/wip/*/1-functional/spec.md                                     │
│     ├── sdd/wip/*/2-technical/spec.md                                      │
│     ├── sdd/wip/*/3-tasks/tasks.json                                         │
│     └── sdd/wip/*/meta.md                                                  │
│                                                                             │
│  3. VALIDATE EACH SPEC AGAINST CURRENT TEMPLATES                            │
│     ├── Load current template from sdd-kit/framework/templates/               │
│     ├── Extract required sections from template                             │
│     ├── Check if spec has all required sections                             │
│     ├── Detect deprecated patterns (from CHANGELOG breaking changes)        │
│     └── Record issues per file                                              │
│                                                                             │
│  4. CHECK VERSION METADATA                                                  │
│     ├── Read framework.version_created from each meta.md                    │
│     ├── Flag features without version (pre-v1.2.1)                          │
│     └── Calculate version drift per feature                                 │
│                                                                             │
│  5. GENERATE COMPATIBILITY REPORT                                           │
│     ├── Group issues by severity (Critical/Warning/Info)                    │
│     ├── List affected files with specific issues                            │
│     └── Provide actionable fixes                                            │
│                                                                             │
│  6. OFFER AUTOMATED FIXES (where possible)                                  │
│     ├── Add missing sections with placeholders                              │
│     ├── Update deprecated patterns                                          │
│     └── Add version tracking to legacy meta.md files                        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Validation Rules

#### Functional Spec Validation

| Section | Required Since | Check |
|---------|---------------|-------|
| `## Problem Statement` | v1.0.0 | Must exist, non-empty |
| `## User Stories` | v1.0.0 | At least one story |
| `## Acceptance Criteria` | v1.0.0 | At least one AC per story |
| `## Out of Scope` | v1.0.0 | Must exist |
| `## E2E Scenarios` | v1.1.0 | Required if `ltp.enabled: true` |
| `## Spec Reference Annotations` | v1.0.2 | Required for brownfield |

#### Technical Spec Validation

| Section | Required Since | Check |
|---------|---------------|-------|
| `## Architecture` | v1.0.0 | Must exist |
| `## API Contracts` | v1.0.0 | Must exist if feature has endpoints |
| `## Data Model` | v1.0.0 | Must exist if feature has persistence |
| `##  Services` | v1.1.4 | Must exist (-first) |
| `## Security Considerations` | v1.0.0 | Must exist |
| `## Environment Variables` | v1.2.1 | Separate from Secrets |

#### meta.md Validation

| Field | Required Since | Check |
|-------|---------------|-------|
| `framework.version_created` | v1.2.1 | Should exist |
| `project_type` | v1.1.4 | Must be prototype/mvp/production |
| `ltp.enabled` | v1.1.0 | Must be true/false |

#### tasks.json Validation (per feature)

| Field | Required Since | Check |
|-------|---------------|-------|
| Task ID format | v1.0.0 | `TASK-XXX` or `AUTO-TASK-*` |
| `layer` field | v1.1.0 | Must be 1, 2, or 3 |
| `complexity` field | v1.1.4 | Must be Low/Medium/High (not Duration) |
| `depends_on` | v1.0.0 | Referenced tasks must exist |
| `acceptance_criteria` | v1.0.0 | At least one AC per task |
| No `duration` or `hours` | v1.1.4 | Deprecated: use Complexity |

#### PROJECT.md Validation

| Section | Required Since | Check |
|---------|---------------|-------|
| `## Project Overview` | v1.0.0 | Must exist |
| `## Tech Stack` | v1.0.0 | Must define language, framework |
| `## Team Conventions` | v1.1.0 | Should exist |
| `## Repository` | v1.0.0 | Must have repo URL |

#### PATTERNS.md Validation (if exists)

| Section | Required Since | Check |
|---------|---------------|-------|
| `## Patterns` | v1.1.17 | At least one pattern if file exists |
| Pattern format | v1.1.17 | Each must have: title, context, decision |
| `source_feature` | v1.1.17 | Must reference originating feature |

#### backlog.md Validation (if exists)

| Section | Required Since | Check |
|---------|---------------|-------|
| `## TODOs` | v1.1.10 | Valid section header |
| `## DEBT` | v1.1.10 | Valid section header |
| `## IDEAS` | v1.1.10 | Valid section header |
| Item format | v1.1.10 | ID format: `TODO-XXX`, `DEBT-XXX`, `IDEA-XXX` |
| No `sdd/backlog/` directory | v1.1.10 | Deprecated: use file, not directory |

> **Lazy-loaded**: When `--version` flag is used, Read `references/output-examples.md` section "## --version examples" for output format reference.

### Deprecated Patterns Detection

The scan also detects deprecated patterns from previous versions:

| Pattern | Deprecated In | Current Standard | Auto-Fix |
|---------|--------------|------------------|----------|
| `Duration: X hours` in tasks | v1.1.4 | `Complexity: Low/Medium/High` | Yes |
| MySQL endpoint in Secrets table | v1.2.1 | Environment Variables table | Yes |
| `sdd/backlog/` directory | v1.1.10 | `sdd/backlog.md` file | Manual |
| Local `sdd-kit/framework/` folder | v1.1.15 | Global `sdd-kit/framework/` | Manual |
| Missing `project_type` in meta | v1.1.4 | Required field | Yes |

### Integration with Default Check

The version check shows a compact summary in the default `/sdd.check` output:

```
Feature: payment-gateway
Stage: implementation (Phase 4/4)
Progress: 72% (13/18 tasks)
Framework: v1.2.1 ✅ (3 specs need updates - run --version)
```

---

## `/sdd.check task TASK-XXX` - Task Details

Shows detailed information about a specific task.

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📋 Task Details: TASK-007
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Title: Frontend components
Status: ✅ completed
Phase: 3 (Frontend)

Estimation: 2.0h
Actual: 1.8h
Variance: -10% (under estimate)

Dependencies:
• TASK-001 ✅ (setup)
• TASK-003 ✅ (project structure)

Blocks:
• TASK-009 (integration)
• TASK-012 (E2E tests)

────────────────────────────────────────
Acceptance Criteria
────────────────────────────────────────

✅ PaymentForm component created
✅ PaymentList component created
✅ Unit tests passing (12/12)
✅ Storybook stories added

────────────────────────────────────────
Files Created/Modified
────────────────────────────────────────

+ src/components/PaymentForm.tsx
+ src/components/PaymentForm.test.tsx
+ src/components/PaymentList.tsx
+ src/components/PaymentList.test.tsx

Commit: xyz789 (2025-11-23 14:30)
```

---

## Status by Phase

Output adapts based on current phase:

### Phase 1 (Functional)
```
Current Stage: functional (Phase 1/4)

Progress:
• Spec drafted: ✅
• Sections: 6/7 complete
• Missing: Success Metrics

Next: Complete spec, then /sdd.spec --approve
```

### Phase 2 (Technical)
```
Current Stage: technical (Phase 2/4)

Progress:
• Functional: ✅ approved
• Technical drafted: ✅
•  services: 3 identified

Next: Review technical, then /sdd.spec --approve
```

### Phase 3 (Tasks)
```
Current Stage: tasks (Phase 3/4)

Progress:
• Tasks generated: ✅ (18 tasks)
• Refined: 2 iterations
• Strategy: Not chosen

Next: /sdd.plan --approve
```

### Phase 4 (Implementation)
Full implementation progress as shown above.

---

## Multiple Features

If multiple features in WIP:

```
User: /sdd.check

AI: Multiple features in progress:

    #     Feature                 Phase              Progress
    ───────────────────────────────────────────────────────────
    003   payment-gateway         implementation     72%
    004   user-auth               technical          Spec draft
    005   dark-mode               functional         Draft complete

    Specify feature by name:
      /sdd.check payment-gateway
      /sdd.check 20260120-payment-gateway
```

### Feature Resolution Logic

When user provides a feature reference:

```bash
# Resolution order:
# 1. Exact match on full name (e.g., "20260120-user-auth")
# 2. Name suffix match (e.g., "user-auth" → finds "20260120-user-auth")

resolve_feature() {
    local ref="$1"

    # Try exact match first (handles full names like "20260120-user-auth")
    if [ -d "sdd/wip/$ref" ]; then
        echo "sdd/wip/$ref"
        return
    fi

    # Try name suffix match (e.g., "user-auth" → "20260120-user-auth")
    local match=$(ls -1 sdd/wip/ 2>/dev/null | grep -E "^[0-9]{8}-.*${ref}$" | head -1)
    if [ -n "$match" ]; then
        echo "sdd/wip/$match"
        return
    fi

    # Fallback: legacy NNN- prefix
    match=$(ls -1 sdd/wip/ 2>/dev/null | grep -E "^[0-9]{3}-.*${ref}$" | head -1)
    if [ -n "$match" ]; then
        echo "sdd/wip/$match"
        return
    fi

    # Not found
    return 1
}
```

---

## Telemetry Section

Shows in standard mode:

```
────────────────────────────────────────
📊 Telemetry
────────────────────────────────────────

Feature Started: 2025-11-20
Elapsed: 3d 4h

Phase Breakdown:
• Functional:     8 interactions (complete)
• Technical:      6 interactions (complete)
• Tasks:          4 interactions (complete)
• Implementation: in progress

Session History:
• Session 1: 2025-11-20 (12 interactions)
• Session 2: 2025-11-21 (15 interactions)
• Session 3: 2025-11-23 (22 interactions)
```

---

## Alerts and Warnings

Shows issues requiring attention:

```
⚠️ Alerts
────────────────────────────────────────

🔴 TASK-010 blocked for 2 days
   Action: Follow up on DevOps ticket

🟡 2 tests failing
   Files: PaymentService.test.ts

🟡 Coverage dropped to 78%
   Action: Add tests for new code
```

---

## `/sdd.check --resume` - Session Management

View and manage all resumable sessions across features.

### List Resumable Sessions

```bash
/sdd.check --resume
```

Shows all features with interrupted sessions:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📋 Resumable Sessions
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Feature              Command              Interrupted          Progress
─────────────────────────────────────────────────────────────────────────
payment-gateway      /sdd.build          2 hours ago          TASK-003 (5/8)
user-auth            /sdd.spec technical 1 day ago            Section 3/5
notification-service /sdd.go             3 days ago           Step 4/5

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔄 To Resume
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Resume specific feature:
  /sdd.build --resume           (payment-gateway - most recent)
  /sdd.spec technical --resume  (user-auth)
  /sdd.go --resume              (notification-service)

Or resume last session:
  /sdd.check --resume --last    (payment-gateway)
```

### Resume Last Session

```bash
/sdd.check --resume --last
```

Immediately resumes the most recently interrupted session:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔄 RESUMING: Last Session
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Feature: payment-gateway
Command: /sdd.build
Interrupted: 2 hours ago

Loading context...
Resuming from TASK-003...

[Continues with /sdd.build --resume behavior]
```

### State File Location

Each feature's state is stored in:
```
sdd/wip/<feature-name>/state.json
```

### When Sessions are Cleared

State files are automatically deleted when:
- Feature is completed (`/sdd.finish`)
- Feature is cancelled (`/sdd.cancel`)
- Session completes successfully

---

## AI Agent Instructions


### Help Flag Detection

**WHEN** the user runs `/sdd.check help`:
1. Output ONLY the "Quick Help" section (not full documentation)
2. Do NOT execute check logic
3. Keep response concise (~15 lines)

### Command Behavior Summary

| Command | Behavior |
|---------|----------|
| `/sdd.check` | Read-only status, no changes |
| `/sdd.check --sync` | Detect + propose fixes (confirm before applying) |
| `/sdd.check --compliance` | Detect + propose fixes (confirm before applying) |
| `/sdd.check --project` | Validate PROJECT.md + propose override registration |
| `/sdd.check --version` | Compare feature/framework versions + suggest migrations |
| `/sdd.check task X` | Read-only task details |
| `/sdd.check --resume` | List resumable sessions (read-only) |
| `/sdd.check --resume --last` | Resume last session (delegates to appropriate command) |

### Key Rules

1. **Auto-detect current phase** from meta.md
2. **Show relevant information** for current phase
3. **Highlight blockers** prominently
4. **Suggest next actions** clearly
5. **For --sync, --compliance, and --project**: Always ask for confirmation before applying fixes

### --sync Specific Rules

1. **Phase-aware**: Only check layers that exist at current phase
2. **Bidirectional**: Check both directions (Spec→Code AND Code→Spec)
3. **Evidence-based**: Provide specific line numbers and quotes
4. **Atomic fixes**: Apply all related fixes together

### --compliance Specific Rules

1. **Run actual commands**: Execute test suite, linter, etc.
2. **Check  requirements**: Dockerfile, /ping, version consistency
3. **Actionable fixes**: Provide specific code changes, not just descriptions

### --project Specific Rules

1. **Validate via GenAI**: Use `genai-validate-project.sh`, fallback to `validate-project.sh`
2. **Compare against standards**: Check all `sdd-kit/framework/standards/` files
3. **Track overrides**: Distinguish between registered and unregistered overrides
4. **Assist registration**: Help user register overrides with proper documentation

### --version Specific Rules

1. **Scan ALL specs**: Not just current feature - scan `sdd/specs/`, `sdd/features/`, `sdd/wip/`
2. **Validate against current templates**: Load templates from `sdd-kit/framework/templates/`
3. **Check required sections**: Each spec type has required sections per version
4. **Detect deprecated patterns**: Look for patterns that were valid in old versions

5. **Handle legacy features**: If `framework.version_created` missing, flag for update
6. **Offer auto-fixes**: Where possible, offer to add missing sections/update patterns

### Validation Implementation

```bash
# Scan all spec locations
spec_locations=(
    "sdd/specs"
    "sdd/features"
    "sdd/wip"
)

for location in "${spec_locations[@]}"; do
    if [ -d "$location" ]; then
        # Find all spec files
        find "$location" -name "spec.md" -o -name "*.yaml" | while read spec_file; do
            validate_spec "$spec_file"
        done

        # Find all meta.md files
        find "$location" -name "meta.md" | while read meta_file; do
            check_version_tracking "$meta_file"
        done
    fi
done
```

### Section Extraction from Templates

```bash
# Extract required sections from current template
extract_required_sections() {
    template_file="$1"
    grep -E "^##+ " "$template_file" | \
        grep -v "Optional" | \
        grep -v "Conditional" | \
        sed 's/^#* //'
}

# Compare spec sections vs template
validate_sections() {
    spec_file="$1"
    template_file="$2"

    required=$(extract_required_sections "$template_file")
    actual=$(grep -E "^##+ " "$spec_file" | sed 's/^#* //')

    # Find missing sections
    echo "$required" | while read section; do
        if ! echo "$actual" | grep -q "$section"; then
            echo "MISSING: $section"
        fi
    done
}
```

---

## Related Commands

- `/sdd.start` - Initialize feature
- `/sdd.spec` - Specifications phase
- `/sdd.plan` - Planning phase
- `/sdd.build` - Implementation phase
- `/sdd.finish` - Completion phase
- `/sdd.fix` - Fix specific errors

---

## Interactive Next Steps (After Status Display)

> **MANDATORY**: Always offer phase-appropriate interactive selection after showing status.
> **NOTE**: This applies to basic `/sdd.check` (status overview), not to `--sync`, `--compliance`, or `--version` flags.

**Determine options based on current phase**:

| Current Phase | Options to Offer |
|---------------|------------------|
| functional | `/sdd.spec`, `/sdd.spec --approve`, `/sdd.start --rename` |
| technical | `/sdd.spec technical`, `/sdd.spec technical --approve`, `/sdd.check --sync` |
| tasks | `/sdd.plan`, `/sdd.plan --approve`, `/sdd.check --sync` |
| implementation | `/sdd.build`, `/sdd.build --next`, `/sdd.finish` |
| complete | `/sdd.start`, `/sdd.list`, `/sdd.backlog list` |

**⛔ INVOKE TOOL (do not print this, CALL the tool)** - options vary by phase:

```
AskUserQuestion(
  questions=[{
    "question": "What would you like to do next?",
    "header": "Next",
    "options": [
      {"label": "/sdd.build", "description": "Continue implementation"},
      {"label": "/sdd.build --next", "description": "Start next task"},
      {"label": "/sdd.finish", "description": "Complete feature"}
    ],
    "multiSelect": false
  }]
)
```

> **Note**: Options above are for `implementation` phase. Use phase table above to determine actual options.

**On user selection**:

| Example Selection | Action |
|-------------------|--------|
| /sdd.spec | `Skill(skill="sdd.spec")` |
| /sdd.build | `Skill(skill="sdd.build")` |
| /sdd.build --next | `Skill(skill="sdd.build", args="--next")` |
| /sdd.finish | `Skill(skill="sdd.finish")` |
| Other | User types custom input |

---
