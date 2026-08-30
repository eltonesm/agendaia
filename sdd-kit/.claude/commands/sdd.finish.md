---
name: sdd.finish
description: Complete feature implementation, run final validations, and archive. Use when all tasks are done, CI passes, and you're ready to move the feature from wip/ to features/.
model: sonnet
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

# Command: /sdd.finish

**Description**: Validate, finalize, and archive completed feature

**Usage**:
- `/sdd.finish` → Validate and archive (behavior based on mode)

---

## Quick Help

> `/sdd.finish help` → Shows this summary

**Syntax**: `/sdd.finish [flags]`

| Flag | Description |
|------|-------------|
| (none) | Validate and archive completed feature |
| `--force` | Skip certain validation checks |
| `--skip-tests` | Skip test re-run (not recommended) |

**Pre-requisite**: `/sdd.build` FINAL VALIDATION must pass first.

**Example**:
```bash
/sdd.finish            # Validate, archive, move to features/
```

**See also**: `/sdd.help finish` for detailed documentation

---

CRITICAL: USER INTERACTION RULES
When this skill shows JSON for AskUserQuestion, you MUST:
  1. CALL the AskUserQuestion TOOL with that exact JSON
  2. DO NOT print options using Bash (no echo, cat, printf)
  3. DO NOT ask "Which option?" as text
  4. Tables marked "REFERENCE ONLY" are for docs - do NOT print


## ⛔ PRE-REQUISITE: /sdd.build FINAL VALIDATION Must Be Complete (v1.1.12)

> See `standards/PREREQ-VALIDATION.md` for complete validation checklist.

**Required steps from /sdd.build**:
- ✅ Step A: Platform Compliance —  (backend/web) or Mobile build validation (Android/iOS)
- ✅ Step B: Layer 3 Quality Gates (performance, security, code review — all findings fixed)
- ✅ Step C: Code Pattern Validation
- ✅ Step D: Local CI Pipeline (Release Process MCP — backend/web only; mobile: `./gradlew test` or `xcodebuild test`)

**⚠️ If ANY step did NOT pass**: Go back to `/sdd.build` first. The validation in `/sdd.finish` is just a DOUBLE-CHECK.

---

## Validation Delegation (MANDATORY)

> **CRITICAL**: Validation MUST use Skills (universal) and Subagents (Claude Code).

```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃  🔧 MANDATORY VALIDATIONS FOR /sdd.finish                               ┃
┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
┃                                                                          ┃
┃  SKILLS (Work on ALL tools: Claude Code, Cursor, Codex):           ┃
┃    sdd-validator       → Build, tests, coverage, ┃
┃    sdd-code-reviewer → Security Rules & Vulns Review                          ┃
┃    sdd-code-reviewer   → Final code review verification (BLOCKING)      ┃
┃                                                                          ┃
┃  SUBAGENTS (Claude Code only):                                           ┃
┃    sdd-layer-analyzer  → Final spec-task-code consistency check         ┃
┃    NOTE: /sdd.finish uses subagent (NOT GenAI) for blocking validation  ┃
┃                                                                          ┃
┃  WORKFLOW:                                                               ┃
┃  1. Skill(skill="sdd-validator")         → Run all validations          ┃
┃  2. Skill(skill="sdd-code-reviewer")   → Security Rules & Vulns Review ┃
┃  3. Skill(skill="sdd-code-reviewer")     → Final code review            ┃
┃  4. Task(subagent_type="sdd-layer-analyzer", ...) → Final consistency   ┃
┃     (Skip step 4 if not on Claude Code)                                  ┃
┃                                                                          ┃
┃  STEPS 1-3 MUST PASS before archiving.                                   ┃
┃  Step 4 is additional validation on Claude Code.                         ┃
┃                                                                          ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
```

---

## Context Advisory

> **Before finalizing**: Finish phase should require minimal context if build was completed properly.

```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃                    📊 CONTEXT CHECK FOR /sdd.finish                      ┃
┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
┃                                                                          ┃
┃  Threshold: 60% before starting /sdd.finish                             ┃
┃                                                                          ┃
┃  /sdd.finish is lightweight:                                            ┃
┃    • Validation already done in /sdd.build                              ┃
┃    • Just runs final double-check                                        ┃
┃    • Generates summary and archives                                      ┃
┃                                                                          ┃
┃  IF context > 60%:                                                       ┃
┃    → Still OK to proceed (finish is lightweight)                         ┃
┃    → Use sdd-layer-analyzer subagent for consistency check              ┃
┃                                                                          ┃
┃  IF context > 80%:                                                       ┃
┃    → Consider compacting first (optional)                                ┃
┃    → Or proceed if confident validation passed in build                  ┃
┃                                                                          ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
```

### Context Note

`/sdd.finish` should typically be run with plenty of context remaining because:
- If `/sdd.build` completed all tasks, you likely have fresh context
- If resuming after break, `/sdd.build --resume` should have clean context

If arriving at `/sdd.finish` with high context, the workflow may have skipped compaction checkpoints. Consider this a learning for next feature.

---

## Skill Hooks (Extension Points)

This skill supports external skill hooks at 3 trigger points.

**Resolution steps** (at each extension point):
1. Read `.claude/skill-hooks.json` and `sdd-kit/framework/skill-hooks.json`
2. Scan installed skills in `~/.claude/skills/*/SKILL.md` for `metadata` with `sdd-kit-*` keys
3. Merge with precedence: user override > repo config > auto-declaration
4. For each enabled hook matching phase=`finish` and the current trigger, ordered by priority:
   - If `hook.mode == "required"`: invoke `Skill("<hook.skill>")` with current feature context
   - If `hook.mode == "available"` (default): evaluate if the hook is relevant to the current feature. Only invoke if the feature context suggests it adds value. Skip silently if irrelevant.

| Trigger | When | Location in workflow |
|---------|------|---------------------|
| `before-start` | Before validations | Before phase verification |
| `after-implementation` | After validations pass | After all checks pass, before archiving |
| `before-approval` | Before archive confirmation | Before asking user to confirm archive |

---

### Extension point: before-start

> Resolve and invoke hooks for phase=`finish`, trigger=`before-start`.

## Purpose

Final step in feature workflow. Runs comprehensive validation, generates summary documentation, and archives the feature from `sdd/wip/` to `sdd/features/`.

---

## Behavior by Mode

### Express Mode

```
/sdd.finish
```

**What happens**:
1. Runs all validators automatically
2. Auto-generates summary documentation
3. Archives feature without confirmation
4. Shows brief success message

**Interaction**: None (unless validation fails)

---

### Standard Mode (default)

```
/sdd.finish
```

**What happens**:
1. Runs all validators
2. Shows validation results
3. Asks for confirmation before archiving
4. Generates documentation
5. Archives and shows summary

**Standard Mode Flow** (Profile-Aware):

> **Lazy-loaded**: When `PROFILE == TECHNICAL_ONLY` or `NON_TECHNICAL_ONLY`, Read `references/output-examples-by-profile.md` for profile-specific output format examples.

────────────────────────────────────────
📚 Knowledge Management
────────────────────────────────────────

Checking progress.md for generalizable learnings...

Found patterns that may apply to other features:
  • KVS TTL must be > Streams retention for CDC
  • BigQueue events needed for data-modifying endpoints

[AskUserQuestion: "Promote these to sdd/PATTERNS.md?"] → User: Yes

✅ Learnings promoted to sdd/PATTERNS.md
   (Future features will benefit from these patterns)

[AskUserQuestion: "Ready to archive feature?"] → User: Yes

Generating documentation...
Archiving to sdd/features/...

> **Lazy-loaded**: When `PROFILE == TECHNICAL_ONLY` or `NON_TECHNICAL_ONLY`, Read `references/output-examples-by-profile.md` § Completion Output for profile-specific completion format.

```
────────────────────────────────────────
📋 Backlog Resolution
────────────────────────────────────────

This feature addressed these backlog items:
  • TODO-001: Refactor payment validation

[AskUserQuestion: "Mark as resolved?"] → User: Yes

✅ TODO-001 → RESOLVED
   Resolution: Completed
   Resolved in: payment-gateway
```

> **Note**: Backlog resolution only shown if feature has `from_backlog` in meta.md or mentions backlog items.

---

## Validation Checks

> See `standards/PREREQ-VALIDATION.md` for complete validation checklist and commands.

### Required Validations (BLOCKING)

#### 0. Phase Verification (Deterministic)

> **Use script for deterministic phase detection** - Saves ~500-1000 tokens vs manual parsing.

```bash
# Deterministic phase detection (FIRST - verify we're in correct phase)
phase_result=$(bash sdd-kit/framework/tools/detection/detect-phase.sh sdd/wip/[feature] --json)
current_stage=$(echo "$phase_result" | grep -o '"stage":"[^"]*"' | cut -d'"' -f4)

# Verify we're in implementation phase
if [ "$current_stage" != "implementation" ]; then
    echo "❌ Feature not in implementation phase. Run /sdd.build first."
    exit 1
fi

# Detect platform — mobile skips all  steps
stack_result=$(bash sdd-kit/framework/tools/detection/detect-stack.sh . --json 2>/dev/null)
platform=$(echo "$stack_result" | grep -o '"platform":"[^"]*"' | cut -d'"' -f4)
IS_MOBILE=false
if [ "$platform" = "android" ] || [ "$platform" = "ios" ]; then
    IS_MOBILE=true
    echo "📱 Mobile project detected ($platform) — steps will be skipped"
fi
```

#### 0.5. CI Validation (MANDATORY - FIRST CHECK)

> **CRITICAL**: CI must pass first. All other validations are meaningless if pipeline fails.

**If `IS_MOBILE = true`** — run mobile tests instead of RP MCP:
```bash
# Android
./gradlew test  # must pass; same as /sdd.build Step A for mobile

# iOS
xcodebuild test -workspace *.xcworkspace -scheme <scheme> \
  -destination 'platform=iOS Simulator,name=iPhone 16'
```
If mobile tests already passed in `/sdd.build` this session → **Skip** (already validated).

**If `IS_MOBILE = false`** — use fury-release-process skill (same as build.md Step 6D):
- If build.md Step 6D already passed in this session → **Skip** (already validated)
- If not yet run → invoke `fury-release-process` skill for quick re-validation
- If skill unavailable → **STOP** — user must install the plugin first

---

#### 1. Task Completion
- All tasks must be "completed" status
- No tasks can be "in_progress" or "blocked"

#### 1.5. Feature Completion Validation (Deterministic)

Comprehensive feature completion check before proceeding.

```bash
# Validate feature is ready for completion
completion_result=$(bash sdd-kit/framework/tools/validation/validate-complete.sh sdd/wip/[feature] --json)
is_complete=$(echo "$completion_result" | grep -o '"complete":[^,}]*' | cut -d: -f2)
blocking_issues=$(echo "$completion_result" | grep -o '"blocking_count":[0-9]*' | cut -d: -f2)

if [ "$is_complete" != "true" ]; then
    echo "❌ Feature not ready for completion ($blocking_issues blocking issues):"
    echo "$completion_result" | grep -o '"blocking_issues":\[[^]]*\]'
    # BLOCKING - fix issues before /sdd.finish can proceed
    exit 1
fi
```

**Completion checks include**:
- All tasks completed (none pending/blocked/in_progress)
- Required files exist (spec.md, tasks.json, meta.md)
- Technical spec approved
- Implementation files present
- No orphan tasks without implementation
- Build artifacts generated (if applicable)

#### 2. Platform Compliance (MANDATORY)

> **Mobile projects (`platform = android | ios`) run mobile validation — NOT .**
> Mobile apps don't have Dockerfiles, /ping endpoints, or  services.

**If `IS_MOBILE = true`** — run mobile compliance via `sdd-validator` skill:
```
Skill(skill="sdd-validator")
```
The validator auto-detects mobile and runs: build check (`./gradlew assembleDebug` or `xcodebuild build`), unit tests, and Andes/Everest compliance scan (no banned libs).

**If `IS_MOBILE = false`** — run  platform compliance:
```bash
bash sdd-kit/framework/tools/validation/validate-code.sh .
```

Checks:
- [ ] Dockerfile exists with -approved image
- [ ] Dockerfile.runtime exists with -approved image
- [ ] Version consistency between Dockerfiles
- [ ] /ping endpoint implemented

**If fails**: Feature CANNOT be completed. See [fury-mandatory-checklist.md](../standards/mandatory-standards.md) for requirements.

#### 2.5. Security Validation (MANDATORY)

> **MANDATORY**: Security assessment must pass before archiving. Feature CANNOT complete with security findings.

##### Security Rules & Vulnerabilities Review

Run a full security assessment via the `/security-assessment` command — executes as a subagent (Audit mode) and returns a structured verdict.

**Verdict required**: `APPROVED` — any `CANNOT_PROCEED` verdict blocks archiving.

##### Secrets Management Validation

> **BLOCKER**: Hardcoded secrets will BLOCK deployment. See [fury-mandatory-checklist.md](../standards/mandatory-standards.md#6--fury-secrets---mandatory-blocker).

**Scan for hardcoded secrets**:
```bash
grep -rEn "(password|api_key|secret|token|credential)\s*[:=]\s*[\"'][^\"']+[\"']" \
  --include="*.java" --include="*.go" --include="*.ts" --include="*.js" \
  --include="*.py" --include="*.yml" --include="*.yaml" --include="*.properties" \
  src/ && echo "❌ BLOCKER: Hardcoded secrets!" && exit 1 || echo "✅ No secrets"
```

**Checklist**:
- [ ] No hardcoded passwords in code
- [ ] No API keys in source files
- [ ] No tokens in configuration
- [ ] All secrets reference `System.getenv()` or equivalent
- [ ] `fury.yml` references  Secrets paths

**If fails**: Feature CANNOT be completed. Remove hardcoded secrets and use  Secrets.

---

#### 3. Test Validation (MANDATORY)
```bash
bash sdd-kit/framework/tools/validation/validate-tests.sh sdd/wip/[feature] .
```

Checks:
- [ ] All tests passing
- [ ] Coverage >= 80%
- [ ] Unit tests exist
- [ ] Integration tests exist

**If fails**: Feature CANNOT be completed

#### 4. Code Quality
- [ ] No linter errors
- [ ] No TypeScript/type errors
- [ ] No open TODOs in code

#### 5. Spec Conflict Re-Scan (MANDATORY)

Re-scan for conflicts that may have been introduced during implementation.

**Why re-scan?**
During implementation, specs can be modified via:
- `/sdd.fix` (changes to specs for consistency)
- `/sdd.check --sync` (sync fixes)
- Manual edits to spec files

These changes might introduce NEW conflicts that weren't present during `/sdd.plan`.

**Validation command**:
```bash
bash sdd-kit/framework/tools/validation/validate-spec-conflicts.sh sdd/wip/[feature] blocking
```

**If NEW conflicts found without annotations**: Feature CANNOT be completed

**Resolution**:
1. Run `/sdd.spec` to interactively resolve new conflicts
2. Retry `/sdd.finish`

---

#### 6. Spec Reference Validation (MANDATORY)

> **v2.1.0**: Validates that all spec references point to existing files and sections.

**What to validate**:
- All `<!-- overrides: path#section -->` references in both specs
- All `<!-- extends: path#section -->` references in both specs
- All `<!-- deprecates: path#section -->` references in both specs

**Files to check**:
```
sdd/wip/[feature]/1-functional/spec.md
sdd/wip/[feature]/2-technical/spec.md
```

**Validation rules**:
1. Referenced file MUST exist
2. Referenced section (after `#`) MUST exist in the target file
3. Reference type MUST be valid (`overrides`, `extends`, or `deprecates`)

**If validation fails**: Feature CANNOT be completed

> **Lazy-loaded**: During validation phase, Read `references/output-examples-by-profile.md` § Validation examples for output format reference.

**Update meta.md on completion**:
When feature completes, record affected specs in `meta.md`:

```yaml
# In meta.md
affected_specs:
  overrides:
    - sdd/features/auth-v1/functional-spec.md#login-user-story
    - sdd/features/auth-v1/technical-spec.md#authentication-flow
  extends:
    - sdd/features/auth-v1/functional-spec.md#session-rules
  deprecates: []
```

---

#### 7. Final Consistency Check (MANDATORY)

Run full sync validation before archiving to catch any accumulated drift.

Before archiving, run full sync validation:

```
/sdd.check --sync
```

**Purpose**: Final verification that all layers (Functional ↔ Technical ↔ Tasks ↔ Code) remain consistent after the entire implementation phase.

**Verdict Handling**:

| Verdict | Action |
|---------|--------|
| `APPROVED` | Proceed to archive |
| `CAN_PROCEED_WITH_WARNINGS` | Archive with documented gaps |
| `CANNOT_PROCEED` | **BLOCKING** - Do NOT archive, fix issues first |

**If sync fails with CANNOT_PROCEED**:
1. Review the inconsistencies reported
2. Use `/sdd.fix` or manual edits to resolve
3. Re-run `/sdd.check --sync`
4. Retry `/sdd.finish` when sync passes

---

## Validation Failure Handling

> **Lazy-loaded**: During validation phase, Read `references/output-examples-by-profile.md` § Validation examples for output format reference.

---

### Extension point: after-implementation

> Resolve and invoke hooks for phase=`finish`, trigger=`after-implementation`.

### Extension point: before-approval

> Resolve and invoke hooks for phase=`finish`, trigger=`before-approval`.

## Generated Documentation

### README.md
Summary of what was built, components, APIs, test coverage.

### implementation-summary.md
Detailed metrics: timeline, effort, tasks, commits, velocity.

---

## Brownfield: System Spec Merge

For brownfield projects, offers to merge changes back to system specs:

### Brownfield Merge Validation (Deterministic)

> **MANDATORY for brownfield**: Validate readiness before attempting merge.

```bash
# Check if brownfield project
project_mode=$(grep "project_mode:" sdd/wip/[feature]/meta.md | cut -d: -f2 | tr -d ' ')

if [ "$project_mode" = "brownfield" ]; then
    # Validate merge readiness
    merge_result=$(bash sdd-kit/framework/tools/validation/validate-brownfield-merge.sh sdd/wip/[feature] --json)
    merge_ready=$(echo "$merge_result" | grep -o '"ready":[^,}]*' | cut -d: -f2)
    conflicts=$(echo "$merge_result" | grep -o '"conflict_count":[0-9]*' | cut -d: -f2)

    if [ "$merge_ready" != "true" ]; then
        echo "⚠️ Brownfield merge has $conflicts potential conflicts:"
        echo "$merge_result" | grep -o '"conflicts":\[[^]]*\]'
        # Show conflicts for user review
    fi

    # Extract affected specs
    affected_specs=$(echo "$merge_result" | grep -o '"affected_specs":\[[^]]*\]')
    echo "📋 Affected system specs:"
    echo "$affected_specs"
fi
```

**Merge validation checks**:
- Identifies all system specs that need updates
- Detects potential merge conflicts
- Validates spec reference annotations
- Checks for breaking changes to existing APIs
- Lists affected downstream systems

```
This feature modified existing system specifications.

Affected specs (from meta.md):
• sdd/specs/api-contracts/auth-api.yaml
• sdd/specs/architecture.md

Merge changes back? [Y/n/manual]
```

- **Y**: Semi-automatic merge with AI assistance
- **n**: Skip (manual later)
- **manual**: Show merge instructions

The functional and technical specs describe what changed - use them to update the system specs in `sdd/specs/`.

---

## Archive Structure

After completion (same for greenfield and brownfield):

```
sdd/features/[YYYYMMDD-feature-name]/    #: Preserves date prefix
├── README.md                  # Feature summary
├── meta.md                    # Final metadata (NEVER DELETE)
├── functional-spec.md         # What was built (or changed)
├── technical-spec.md          # How it was built (or changed)
├── architecture.md            # Architecture diagrams
├── tasks.json                   # Task list executed
└── implementation-summary.md  # Execution metrics
```

**Feature Naming**:
- The full directory name (including date prefix) is preserved when moving from `wip/` to `features/`
- Example: `sdd/wip/20260120-user-auth/` → `sdd/features/20260120-user-auth/`

**CRITICAL**: `meta.md` must be moved INTACT from `sdd/wip/` to `sdd/features/`. It contains the complete history. NEVER delete it.

> **Telemetry**: Data is captured automatically by hooks in `~/.claude/logs/` (Claude Code) or `~/.cursor/logs/` (Cursor).
---

## Examples

### Example 1: Standard Mode - Success
```
User: /sdd.finish

AI: Running validations...

    ✅ All tasks completed
    ✅ passed
    ✅ Tests passing (91% coverage)

    [AskUserQuestion: "Ready to archive?"]

User: Y

AI: 🎉 Feature complete!
    Archived to: sdd/features/payment-gateway/
```

### Example 2: Validation Failure
```
User: /sdd.finish

AI: Running validations...

    ✅ All tasks completed
    ❌ : Missing /ping endpoint
    ❌ Coverage: 78% (below 80%)

    Feature cannot be completed until issues fixed.

    How to fix:
    1. Implement /ping endpoint
    2. Add tests for coverage

    Then retry: /sdd.finish
```

### Example 3: Express Mode
```
User: /sdd.finish

AI: ⚡ Express Mode - Auto-finalizing

    ✅ Validations passed
    ✅ Documentation generated
    ✅ Feature archived

    🎉 Complete! → sdd/features/user-auth/
```

---

## AI Agent Instructions


### Help Flag Detection

**WHEN** the user runs `/sdd.finish help`:
1. Output ONLY the "Quick Help" section (not full documentation)
2. Do NOT execute finish logic
3. Keep response concise (~15 lines)

### Mode-Specific Behavior

1. **Express**: Auto-validate, auto-archive, brief output
2. **Standard**: Show validations, confirm before archiving



### Key Rules
1. **Never skip validations** -  and tests are MANDATORY
2. **Block on failures** - Do not archive incomplete features
3. **Promote learnings** - Check for patterns to add to PATTERNS.md (see below)
4. **Generate documentation** - Always create README and summary
5. **PRESERVE meta.md** - Move INTACT to sdd/features/, NEVER delete
6. **Clean up WIP** - Only remove sdd/wip/[feature]/ AFTER moving all files

> **Telemetry**: Data is captured automatically by hooks in `~/.claude/logs/` (Claude Code) or `~/.cursor/logs/` (Cursor).
---

### Archival Workflow (CRITICAL ORDER)

> **⚠️ WARNING**: The order of operations is CRITICAL. Generating files AFTER cleanup causes orphan files.

**CORRECT ORDER** (strictly follow this sequence):

```
Step 0: Clean up transient files (NOT archived)
        │
        └── rm -rf sdd/wip/[feature]/verdicts/  (quality gate verdicts)
        │
Step 1: Generate ALL documentation INSIDE wip/
        │
        ├── README.md
        ├── implementation-summary.md
        └── VALIDATION_REPORT.md (if validation ran)
        │
Step 2: Verify ALL files exist in wip/[feature]/
        │
        ├── ls -la sdd/wip/[feature]/
        └── Confirm: meta.md, specs, tasks, documentation
        │
Step 3: Move ENTIRE directory (NOT contents)
        │
        └── mv sdd/wip/[feature] sdd/features/[feature]
        │
        ⚠️ This moves the DIRECTORY ITSELF, not its contents.
        ⚠️ DO NOT use: mv sdd/wip/[feature]/* (globs can miss files!)
        │
Step 4: Verify archive completeness
        │
        └── ls -la sdd/features/[feature]/ (confirm all files present)
```

**⛔ NEVER DO THIS** (causes orphan files):
```
❌ WRONG: mv sdd/wip/[feature]/* ... (glob misses subdirs/dotfiles)
❌ WRONG: rm -rf sdd/wip/[feature]/ before verifying move
❌ WRONG: Generate files after move operation
❌ WRONG: Use separate mv commands for * and .*
```

**✅ CORRECT MOVE COMMAND** (single atomic operation):
```bash
# Move the entire directory - NOT its contents
mv sdd/wip/[feature] sdd/features/[feature]

# This is atomic and moves EVERYTHING including:
# - All files (meta.md, tasks.json, specs, etc.)
# - All subdirectories (1-functional/, 2-technical/, etc.)
# - All hidden files (.gitkeep, etc.)
```

**Verification Checklist** (run AFTER move):
```bash
FEATURE="[feature-name]"

# Verify source is GONE (not just empty - GONE)
if [ -d "sdd/wip/$FEATURE" ]; then
    echo "❌ CRITICAL: sdd/wip/$FEATURE still exists!"
    echo "   Files remaining:"
    ls -la "sdd/wip/$FEATURE"
    echo "   DO NOT proceed - investigate why move failed"
    exit 1
fi

# Verify destination has required files
FILES_REQUIRED=("meta.md" "README.md")
for file in "${FILES_REQUIRED[@]}"; do
    if [ ! -f "sdd/features/$FEATURE/$file" ]; then
        echo "❌ MISSING in archive: $file"
        exit 1
    fi
done

echo "✅ Archive complete - wip/ cleaned, features/ populated"
```

**Why `mv directory` instead of `mv directory/*`?**
- `mv dir/*` uses shell glob expansion which can fail with:
  - Subdirectories (1-functional/, 2-technical/, etc.)
  - Hidden files (dotfiles)
  - Special characters in filenames
  - Too many files (argument list too long)
- `mv dir` is atomic and moves EVERYTHING

---

### Promote Learnings to PATTERNS.md

> **PURPOSE**: Extract generalizable learnings so future features benefit.
> **NOTE**: Patterns can also be added manually via `/sdd.project patterns --add`.
> Auto-promotion adds to technology sections. Manual patterns go to "Team Conventions".

**After validations pass, BEFORE archiving**:

1. **Read** `sdd/wip/[feature]/4-implementation/progress.md`
2. **Extract** learnings from "Learnings per Task" section
3. **Filter** for generalizable patterns (see criteria below)
4. **Check for duplicates** (see deduplication below)
5. **Show** extracted patterns to user (excluding duplicates)
6. **Ask**: "Promote these to sdd/PATTERNS.md?" via AskUserQuestion
7. **If Y**: Append to `sdd/PATTERNS.md` with date stamp

**Promotion criteria** (must meet ALL):
- ✅ Applies to multiple features (not feature-specific)
- ✅ Non-obvious (not in official  docs)
- ✅ Saves significant time (avoids repeating mistakes)

### Duplicate Detection

**Before promoting each pattern**:

1. Read existing `sdd/PATTERNS.md`
2. For each extracted pattern:
   a. Check if similar pattern exists (fuzzy match on name/content)
   b. **Fuzzy match criteria**:
      - Same pattern name (case-insensitive)
      - Content similarity > 70% (key terms match)
      - Same "do/don't" directives
3. If duplicate found: Skip with note "Already in PATTERNS.md (from [section])"
4. If new: Include in promotion list

**Deduplication Output**:

```
Found 5 learnings in progress.md:

| # | Learning | Status |
|---|----------|--------|
| 1 | KVS explicit TTL | ✅ New (will promote) |
| 2 | Use fury-go-toolkit | ⏭️ Already exists (Team Conventions) |
| 3 | BigQueue manual ACK | ⏭️ Already exists (BigQueue & Messaging) |
| 4 | Context propagation | ✅ New (will promote) |
| 5 | Check N+1 queries | ✅ New (will promote) |

Promoting 3 new patterns (2 skipped as duplicates).
```

**Merge with existing patterns** (when similar but not identical):

If a pattern is similar but adds new information:
- Show both versions to user
- Ask: "Merge new content into existing pattern?"
- If Y: Update existing pattern with combined content
- If N: Skip (don't add duplicate)

**Example extraction**:
```markdown
From progress.md "Learnings per Task":

  TASK-003: Configure KVS
  Patterns discovered:
  - KVS requires explicit TTL, default is 24h
  - Use fury-go-toolkit for HTTP, not raw net/http

  Gotchas:
  - Don't use context.Background(), pass request context

→ These are GENERALIZABLE (apply to any Go + KVS feature)
```

**Promote to PATTERNS.md**:
```markdown
## Go

**KVS Usage**:
- ✅ Always set explicit TTL when writing to KVS
- ❌ Don't rely on default TTL (24h)
- Example: `client.Set(key, value, 3600)  // 1 hour TTL`

**Context Propagation**:
- ✅ Always pass request context through function calls
- ❌ Never use `context.Background()` in request handlers
- Why: Loses tracing, request ID, cancellation

**Last Updated**:
- 2026-01-13: Added KVS patterns from feature-payments
```

**DO NOT promote**:
- ❌ Feature-specific details ("Endpoint /users returns 200")
- ❌ Temporary workarounds
- ❌ Info already in  docs

**If no PATTERNS.md exists**: Create it using template from `sdd-kit/framework/templates/PATTERNS.md`

**If user says No**: Skip promotion, proceed to archiving

---

## Command Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                         /sdd.finish FLOW                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  PREREQUISITES (from /sdd.build FINAL VALIDATION):                  │
│    ✅ Step A: Platform Compliance ( OR mobile build)             │
│    ✅ Step B: Layer 3 Quality Gates (zero findings)                  │
│    ✅ Step C: Code Pattern Validation passed                         │
│    ✅ Step D: CI Pipeline (RP MCP for backend; gradlew/xcode mobile) │
│                                                                      │
│  THIS COMMAND:                                                       │
│    → Validates platform compliance ( or mobile)                  │
│    → Validates test coverage ≥80%                                   │
│    → Re-scans for spec conflicts (v2.2.0)                           │
│    → Validates spec references                                       │
│    → Generates documentation                                         │
│    → Archives feature to sdd/features/                              │
│                                                                      │
│  NEXT STEPS (after completion):                                      │
│    📁 Feature archived → sdd/features/[name]/                       │
│    🆕 Start new feature → /sdd.start                               │
│    📋 Work on backlog → /sdd.backlog pick                            │
│    🔍 Review completed → /sdd.list                                 │
│                                                                      │
│  BLOCKING CONDITIONS:                                                │
│    ❌ Any FINAL VALIDATION step not passed in /sdd.build           │
│    ❌ Build/tests failing                                           │
│    ❌ Coverage <80%                                                  │
│    ❌ missing (backend/web only)                     │
│    ❌ Mobile build/test failing (Android/iOS only)                  │
│    ❌ Banned libs detected (Material, Coil, Hilt, etc.) for mobile  │
│    ❌ Unresolved spec conflicts (v2.2.0)                            │
│    ❌ Performance/Security Rules and Vulnerabilities/Code review not run or findings remain │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Related Commands

- `/sdd.build` - Previous phase (implementation)
- `/sdd.check` - View validation status
- `/sdd.start` - Start new feature
- `/sdd.backlog` - Manage backlog for next work

---

## What's Next?

After `/sdd.finish` completes successfully:

1. **Your feature is archived** in `sdd/features/[feature-name]/`
2. **Telemetry** captured automatically in `~/.claude/logs/` or `~/.cursor/logs/` (when supported)
3. **Documentation generated** (README.md, implementation-summary.md)

### Interactive Next Steps (After Archive Complete)

> **MANDATORY (Standard mode only)**: Offer interactive selection after archiving.
> **EXPRESS MODE**: Skip this - show brief completion message only.

**⛔ INVOKE TOOL (do not print this, CALL the tool)** (only in Standard mode):

```
AskUserQuestion(
  questions=[{
    "question": "Feature archived! What's next?",
    "header": "Next",
    "options": [
      {"label": "/sdd.start (Recommended)", "description": "Start a new feature"},
      {"label": "/sdd.start --reopen", "description": "Reopen this feature later for iteration"},
      {"label": "/sdd.backlog list", "description": "Check pending backlog items"},
      {"label": "/sdd.list", "description": "View all features"}
    ],
    "multiSelect": false
  }]
)
```

**On user selection**:

| Selection | Action |
|-----------|--------|
| /sdd.start (Recommended) | `Skill(skill="sdd.start")` |
| /sdd.start --reopen | Show: "To reopen later: `/sdd.start --reopen [feature-name]`" |
| /sdd.backlog list | `Skill(skill="sdd.backlog", args="list")` |
| /sdd.list | `Skill(skill="sdd.list")` |
| Other | User types custom input |

> **MODE BEHAVIOR**: In Express mode, just show completion message without prompting.

---
