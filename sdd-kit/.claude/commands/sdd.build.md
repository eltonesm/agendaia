---
name: sdd.build
description: Implement feature tasks following approved strategy. Use when tasks are approved and user is ready to code. Handles layer-by-layer execution, infrastructure creation, database migrations, frontend builds, and CI validation.
model: opus
argument-hint: "[task-id|--next|--all]"
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

---
hooks:
  TaskCompleted:
    - hooks:
        - type: command
          command: "sdd-kit/framework/tools/shared/check-quality-task.sh"
---

# Command: /sdd.build

**Description**: Implement feature tasks following approved execution strategy

**Usage**:
- `/sdd.build` → Implement all tasks (behavior based on mode)
- `/sdd.build task TASK-XXX` → Implement specific task
- `/sdd.build phase N` → Implement specific phase
- `/sdd.build --layer N` → Implement up to layer N
- `/sdd.build --resume` → Resume interrupted build session
- `/sdd.build --next` → Auto-continue with next pending task

---

## Quick Help

> `/sdd.build help` → Shows this summary

**Syntax**: `/sdd.build [target] [flags]`

| Flag | Description |
|------|-------------|
| (none) | Implement all tasks based on mode |
| `task TASK-XXX` | Implement specific task only |
| `phase N` | Implement specific phase only |
| `--layer N` | Implement up to layer N |
| `--resume` | Resume interrupted session |
| `--next` | Auto-continue with next pending task |

**Examples**:
```bash
/sdd.build                 # Implement all pending tasks
/sdd.build task TASK-005   # Implement only TASK-005
/sdd.build --layer 2       # Implement layers 1 and 2 only
```

**See also**: `/sdd.help build` for detailed documentation

---

CRITICAL: USER INTERACTION RULES
When this skill shows JSON for AskUserQuestion, you MUST:
  1. CALL the AskUserQuestion TOOL with that exact JSON
  2. DO NOT print options using Bash (no echo, cat, printf)
  3. DO NOT ask "Which option?" as text
  4. Tables marked "REFERENCE ONLY" are for docs - do NOT print


## Plan Mode Integration (Opt-In)

> **CRITICAL**: Claude Code Plan Mode for complex tasks. **OPT-IN** - disabled by default.
> Most users want uninterrupted implementation flow.

### Platform Availability

| Platform | Plan Mode Available |
|----------|---------------------|
| Claude Code (CLI) | ✅ Yes (`EnterPlanMode`/`ExitPlanMode`) |
| Cursor | ❌ No (use fallback) |

### Configuration

Plan Mode is **disabled by default**. Enable via `PROJECT.md` or `sdd-kit/framework/config.yaml`:

```yaml
# In PROJECT.md or sdd-kit/framework/config.yaml
plan_mode:
  build_complex_tasks: false      # Default: false (opt-in)
  build_layer_transitions: false  # Default: false (opt-in)
  build_fury_test_recovery: false # Default: false (opt-in)
```

### Trigger Conditions

| Trigger | Condition | When `plan_mode.build_*: true` |
|---------|-----------|--------------------------------|
| **Complex Tasks** | `task.complexity == "High"` OR `files_affected > 5` OR Layer 2 task OR `acceptance_criteria > 4` | `build_complex_tasks: true` |
| **Layer Transitions** | Completing Layer N → Layer N+1, context > 50%, 10+ tasks in next layer | `build_layer_transitions: true` |
| **CI Pipeline Recovery** | First failure with ambiguous error, classification confidence < 70%, or 2+ failed fix attempts | `build_fury_test_recovery: true` |

### Plan Mode Flow

```
IF config.plan_mode.build_* AND trigger_conditions_met AND EnterPlanMode available:
    1. EnterPlanMode()

    # Exploration phase (read-only)
    - Read related files
    - Analyze patterns
    - Identify dependencies

    # Design phase
    - Create implementation approach
    - List files to modify
    - Identify risks

    # Present to user
    - Show plan summary
    - Wait for approval

    2. ExitPlanMode()

    # Implementation phase
    - Execute approved plan
ELSE:
    # Fallback: proceed without Plan Mode
    - Implement directly (Express/Standard behavior)
```

### Fallback for Non-Claude Code Platforms

When `EnterPlanMode` is not available:

```
┌─────────────────────────────────────────────────────────────────────┐
│  PLAN MODE FALLBACK (Non-Claude Code)                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  INSTEAD OF:                                                         │
│    EnterPlanMode → Explore → Design → ExitPlanMode                   │
│                                                                      │
│  USE:                                                                │
│    1. Explore codebase (same read-only exploration)                  │
│    2. Design implementation plan                                     │
│    3. Display plan inline in chat                                    │
│    4. Use AskUserQuestion: "Approve this approach?"                  │
│       - Options: "Approve", "Modify", "Cancel"                       │
│    5. Continue with approved plan                                    │
│                                                                      │
│  DETECTION:                                                          │
│    IF EnterPlanMode tool not available:                              │
│      → Use fallback flow automatically                               │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Mode-Based Behavior

| Mode | Plan Mode Behavior |
|------|-------------------|
| Express | Skip Plan Mode (auto-implement) |
| Standard | Use Plan Mode if config enabled + triggers met |

---

## Quality Checks (MANDATORY)

> **BLOCKING**: Quality checks after EACH task, not just at the end.

**Per-Task Cycle**:
1. Implement → Write production code
2. Test → Run unit/integration tests (skip for prototype)
3. Quality (delegate to agent for context efficiency):
   ```python
   Task(
       subagent_type="sdd-validator-runner",
       prompt="""
       Validate files: [modified_files]
       Run Layer 3 quality gates: performance, security, code-review
       Return unified JSON verdict.
       """
   )
   ```
   This consolidates 3 quality skills (~6000 tokens) into single verdict (~300 tokens).
4. Fix → ALL findings (critical, major, AND minor)
5. Re-check → Re-run until ZERO findings
6. Complete → Mark done, commit

**Verdict Files**: Written to `sdd/wip/<feature>/verdicts/` by sdd-validator-runner. Do not commit.

> **v2.8.0 Token Optimization**: Layer 3 quality gates now delegate to `sdd-validator-runner` instead of 3 inline Skill() calls. Saves ~5700 tokens per task cycle.

### Dependency Scanning

**MANDATORY before adding any library**: Run vulnerability check via `dependency security scanner`.

```python
mcp__dependency security scanner__safe_add_dependency(
  technology="java",
  ecosystem="maven",
  name_user="<user>",
  name_repository="<repo>",
  dependencies=[{"name": "new-library", "version": "1.0.0"}]
)
```

**Action on vulnerability**: Try latest version. If still vulnerable, warn user and block.

---

## Mandatory Code Review Protocol

> **BLOCKING**: Code review is not optional. ALL findings must be fixed, including minor issues.

```
TASK COMPLETION CYCLE (per task):

┌─────────────────────────────────────────────────────────────────┐
│  1. IMPLEMENT → Write production code                           │
│         ↓                                                       │
│  2. TEST → Run unit/integration tests (skip for prototype)      │
│         ↓                                                       │
│  3. QUALITY → Invoke sdd-code-reviewer, performance, security  │
│         ↓                                                       │
│  4. FIX ALL → Critical, Major, AND Minor findings               │
│         ↓                                                       │
│  5. RE-CHECK → Re-run until ZERO findings                       │
│         ↓                                                       │
│  6. COMPLETE → Mark done, commit                                │
└─────────────────────────────────────────────────────────────────┘
```

**You MUST fix ALL findings** including minor - Minor findings are NOT optional. Minor issues accumulate into technical debt.

---

## Behavior by Mode

| Mode | Behavior |
|------|----------|
| **Express** | Implement all, minimal pauses, auto-fix errors, auto-advance |
| **Standard** | Report progress, pause on errors, ask user |

---

## Skill Hooks (Extension Points)

This skill supports external skill hooks at 3 trigger points. At each point, the agent resolves hooks from 3 layers (user override > repo config > auto-declaration) and invokes matching skills.

**Resolution steps** (at each extension point):
1. Read `.claude/skill-hooks.json` and `sdd-kit/framework/skill-hooks.json`
2. Scan installed skills in `~/.claude/skills/*/SKILL.md` for `metadata` with `sdd-kit-*` keys
3. Merge with precedence: user override > repo config > auto-declaration
4. For each enabled hook matching phase=`build` and the current trigger, ordered by priority:
   - If `hook.mode == "required"`: invoke `Skill("<hook.skill>")` with current feature context
   - If `hook.mode == "available"` (default): evaluate if the hook is relevant to the current feature. Only invoke if the feature context suggests it adds value. Skip silently if irrelevant.

| Trigger | When | Location in workflow |
|---------|------|---------------------|
| `before-start` | Before Step 1 | Before phase detection |
| `after-implementation` | After Step 5 | After all tasks implemented and quality gates passed |
| `before-approval` | Before Step 8 | Before interactive next steps / finish prompt |

---

## Workflow (Steps in Order)

### Extension point: before-start

> Resolve and invoke hooks for phase=`build`, trigger=`before-start`.

### Step 1: Context Check + Phase Detection (Deterministic)

> **Use script for deterministic phase detection** - Saves ~500-1000 tokens vs manual parsing.

```bash
# Deterministic phase detection (FIRST - verify we're in correct phase)
phase_result=$(bash sdd-kit/framework/tools/detection/detect-phase.sh sdd/wip/[feature] --json)
current_stage=$(echo "$phase_result" | grep -o '"stage":"[^"]*"' | cut -d'"' -f4)

# Verify tasks are approved (must be in phase 4 = implementation)
if [ "$current_stage" != "implementation" ]; then
    echo "❌ Tasks not approved. Run /sdd.plan --approve first."
    exit 1
fi

# Detect platform (android | ios | "")
stack_result=$(bash sdd-kit/framework/tools/detection/detect-stack.sh . --json 2>/dev/null)
platform=$(echo "$stack_result" | grep -o '"platform":"[^"]*"' | cut -d'"' -f4)

# Verify mobile skills are available (installed via Claude Marketplace by sdd-kit)
if [ "$platform" = "android" ] || [ "$platform" = "ios" ]; then
    skill_dir="meli-frontender-android"
    plugin_name="meli-frontend-android"
    [ "$platform" = "ios" ] && skill_dir="meli-frontender-ios"
    [ "$platform" = "ios" ] && plugin_name="meli-frontend-ios"
    PLUGIN_PATH="$HOME/.claude/plugins/$plugin_name/skills/$skill_dir"

    if [ ! -d "$PLUGIN_PATH" ]; then
        echo "❌ Mobile plugin not found: $plugin_name"
        echo "   Re-run: sdd-kit install claude"
        exit 1
    fi
fi
```

Then check context level:
- Normal (<40%): Proceed inline
- Elevated (40-60%): Use subagents for heavy ops
- High (60-80%): Recommend compaction
- Critical (>80%): Must compact first via `context-guardian` skill

### Step 2: Read Task Source

Read tasks from `sdd/wip/[feature]/3-tasks/tasks.json`:
```bash
jq '.tasks[] | select(.status == "pending")' tasks.json
```

### Step 3: Layer-Based Execution

Execute tasks by LAYER first, then by dependency level:

```
LAYER 1 (Local) - Parallel Execution
├─ Skill(skill="sdd-code-reviewer") → Build mode (load security rules + SDKs) [MANDATORY]
├─ Analyze task dependencies → identify independent tasks
├─ For each independent task group:
│   ├─ IF platform == "android" → Spawn sdd-implementer (isolation: "worktree")
│   ├─ IF platform == "ios"     → Spawn sdd-implementer     (isolation: "worktree")
│   ├─ ELSE                     → Spawn sdd-implementer          (isolation: "worktree")
│   └─ Each instance works on its own worktree
├─ After all complete:
│   ├─ Merge worktree changes to main branch
│   └─ Resolve any conflicts
├─ Validate gates pass (build, local tests)
├─ git commit "feat: layer 1 complete"
└─ /sdd.check --compact (if context > 50%)

LAYER 2 ()
├─ Execute all Layer 2 tasks
├─ Validate CI Pipeline (RP MCP) passes
├─ git commit "feat: layer 2 complete"
└─ /sdd.check --compact (if context > 50%)

LAYER 3 (Quality)
├─ Skill(skill="sdd-code-reviewer") → Audit mode (vulnerability review) [MANDATORY]
├─ Execute all quality tasks
├─ All experts pass (0 findings)
└─ git commit "feat: layer 3 complete"

<signal>ALL_TASKS_COMPLETE</signal>
```

#### Layer Completion Protocol

After completing all tasks in a layer:

1. **Validate layer**: All tasks pass gates
2. **Commit**: Natural checkpoint for the layer
3. **Compact context** (if needed): `/sdd.check --compact`
4. **Proceed to next layer**

**Why compact between layers**:
- Layer 1 code details not needed for Layer 2  integration
- Layer 2 service configs not needed for Layer 3 quality reviews
- Prevents context exhaustion on large features

**When to optimize context**:
- Context > 50% after completing a layer → Recommend `/clear` or compaction (show advisory below)
- Context > 70% → Strongly recommend `/clear` before next layer
- Large feature (10+ tasks per layer) → Always optimize context

**Context advisory** (when context > 50% at layer boundary):
```
╔═══════════════════════════════════════════════════════╗
║  CONTEXT ADVISORY (optional)                          ║
╠═══════════════════════════════════════════════════════╣
║                                                       ║
║  Context usage: ~[XX]%                                ║
║  Layer completed: [N]                                 ║
║                                                       ║
║  All progress is saved in specs and tasks.json.       ║
║  Options:                                             ║
║    1. /clear — fresh context (recommended if > 50%)   ║
║    2. /sdd.check --compact — compress current context║
║    3. Continue as-is                                  ║
║                                                       ║
╚═══════════════════════════════════════════════════════╝
```

#### Layer 1 Parallel Execution Strategy

When Layer 1 has multiple independent tasks, use worktree-isolated agents for parallel execution:

1. **Dependency analysis**: Identify tasks with no inter-dependencies (no shared files, no data flow between them)
2. **Parallel dispatch**: Spawn the platform-correct implementer (with `isolation: "worktree"`) for each independent task or task group:
   - `platform == "android"` → `sdd-implementer` — includes mandatory Everest/Andes docs read
   - `platform == "ios"` → `sdd-implementer` — includes mandatory Everest/Andes docs read
   - backend/web → `sdd-implementer`
3. **Merge**: After all instances complete, merge worktree changes back to the main branch and resolve conflicts
4. **Validate**: Run build + local tests on the merged result

**When NOT to parallelize**:
- Tasks that modify the same files
- Tasks with data dependencies (task B needs output from task A)
- Less than 3 independent tasks (overhead not worth it)
- Layer 2 tasks ( services have side effects — always sequential)
- Layer 3 tasks (quality reviews need full codebase context)

#### After Layer Completion - Interactive Next Steps

> **MANDATORY (Standard mode only)**: Check context, then offer interactive selection after each layer.
> **EXPRESS MODE**: Check context, show advisory only if > 70%, then auto-continue to next layer.

**Context check**: Estimate context usage. If > 50%, show advisory before presenting options (tasks.json is already up-to-date on disk from Step 5, and layer commit includes it):

```
╔═══════════════════════════════════════════════════════╗
║  CONTEXT ADVISORY                                     ║
╠═══════════════════════════════════════════════════════╣
║                                                       ║
║  Context usage: ~[XX]%                                ║
║  Layer completed: [N]                                 ║
║                                                       ║
║  All progress is saved in tasks.json (committed).     ║
║  Primary recommendation:                              ║
║    /clear then /sdd.build --resume                   ║
║  Fresh context (~187K tokens) outperforms              ║
║  compaction (~140K degraded tokens).                   ║
║                                                       ║
╚═══════════════════════════════════════════════════════╝
```

**⛔ INVOKE TOOL (do not print this, CALL the tool)** (only in Standard mode):

```
AskUserQuestion(
  questions=[{
    "question": "Layer [N] complete. What next?",
    "header": "Next",
    "options": [
      {"label": "/clear + /sdd.build --resume (Recommended)", "description": "Fresh context, resume from next layer"},
      {"label": "/sdd.build", "description": "Continue in current context"},
      {"label": "/sdd.check --compact", "description": "Compact context if /clear not possible"},
      {"label": "/sdd.check --sync", "description": "Verify spec-code consistency"}
    ],
    "multiSelect": false
  }]
)
```

> **Note**: Replace `[N]` with the actual layer number in the question.

**On user selection**:

| Selection | Action |
|-----------|--------|
| /clear + /sdd.build --resume (Recommended) | Inform user to run `/clear`, then `/sdd.build --resume` |
| /sdd.build | `Skill(skill="sdd.build")` |
| /sdd.check --compact | `Skill(skill="sdd.check", args="--compact")` |
| /sdd.check --sync | `Skill(skill="sdd.check", args="--sync")` |
| Other | User types custom input |

> **MODE BEHAVIOR**: In Express mode, check context and show advisory only if > 70%, then automatically continue to next layer.

### Step 3.3: Infrastructure Creation (CONDITIONAL)

> **WHEN**: `tasks.json` contains `INFRA-TASK-*` entries (generated by `/sdd.plan` from services marked `(NEW)` in technical spec).
>
> **SKIP IF**: No `INFRA-TASK-*` entries in tasks.json.
>
> **RUNS BEFORE**: Step 3.5 (Database Migration Branch).

#### ⛔ MANDATORY — Skill-First Protocol for  Service Creation

**The `fury-services-operations` skill is the canonical path to create  infrastructure** (KVS, Cache, BigQueue, NoSQL, MySQL, Streams, VectorDB, Object Storage, Secrets, Scopes, Config, Audits, Workqueues, Locks, Sequences, Jobs, Quotas, Schemas, Rules Engine, Template Processing, Feature Flags, Experiments, and 30+ more). It supports CRUD on services that the `fury` shell CLI does NOT — the CLI exposes only a subset of services (mostly read/list for many service types).

**For EACH `INFRA-TASK`, you MUST follow this order — no exceptions, no shortcuts:**

```
TIER A — Skill-first (REQUIRED FIRST ATTEMPT):
  1. Invoke Skill("fury-services-operations") with a natural-language prompt
     describing the create intent, e.g.:
       "create KVS container <name> for app <app>, ttl <s>, ..."
       "create BigQueue topic <name> for app <app>, partitions <n>, ..."
       "create object storage <name> for app <app>, ..."
       "create config service <name> for app <app>, ..."
       "create secret <name> for scope <scope>, ..."
  2. Pass the exact parameters from the technical spec (service name, app,
     TTL, partitions, scope, segments, etc.). The skill auto-loads the
     service-specific reference and handles auth/env vars/HTTP calls.
  3. If the skill creates the service → run the verify list command
     (Skill or CLI) → mark INFRA-TASK completed → continue.

TIER B — Direct fury CLI fallback (only if Tier A reports the service is
unsupported by the skill):
  4. Run the `fury services <type> create` command from the task description.
  5. Verify with `fury services <type> list`.

TIER C — Manual / Web (LAST RESORT):
  6. If neither Tier A nor Tier B can create the service, inform the user,
     point to web.furycloud.io, and AskUserQuestion: Retry / Mark manual /
     Abort. NEVER mark `skipped` without explicit user approval.
```

**Pre-flight** (run before Tier A as a sanity check, not as a gate to skip the skill):

```bash
# fury CLI presence (informational — the SKILL handles its own auth)
command -v fury &>/dev/null || echo "INFO: fury CLI absent — Tier B fallback unavailable"

# Logged in (informational)
fury get-token &>/dev/null 2>&1 || echo "INFO: not logged in — run '' if Tier A also fails"

# .fury file (informational)
[ -f ".fury" ] || echo "WARN: no .fury file — supply app name explicitly to the skill"
```

**Execution loop**:

```
FOR EACH pending INFRA-TASK in tasks.json:
  1. Read service type, name, and parameters from the task description AND
     the linked Infrastructure Creation row in 2-technical/spec.md
  2. Invoke Skill("fury-services-operations") with a create prompt
     (Tier A) — this is MANDATORY, not optional
  3. If Tier A creates → verify → mark completed → next task
  4. If Tier A reports "service type not supported by skill" → run the
     fury CLI create command (Tier B) → verify → mark completed
  5. If Tier B unsupported (CLI lacks create for this type) → AskUserQuestion
     (Tier C): Retry skill / Open web.furycloud.io / Mark manual / Abort
  6. If any tier fails with an error (auth, VPN, permission, etc.) →
     show error, AskUserQuestion: Retry / Skip / Abort
```

#### ⛔ ANTI-PATTERNS — Do NOT do these

| Anti-pattern | Why it's wrong | What to do instead |
|--------------|----------------|--------------------|
| Run `fury services <x> --help`, see only read/list commands, mark INFRA-TASK as `skipped` | The `fury-services-operations` skill creates many services the CLI cannot. `--help` output does NOT define the universe of available create paths. | Invoke `Skill("fury-services-operations")` first. The skill is the source of truth for what is creatable. |
| Mark task `skipped` because "the CLI is read-only for this service" | Same as above — read-only CLI ≠ uncreatable service. | Try Tier A. Only mark `skipped` after Tier A explicitly returns "unsupported" AND the user approves. |
| Run the CLI command from the task description verbatim without first trying the skill | The task description was generated assuming a single CLI path; it does not capture skill capability. | Treat the CLI command in the task description as a Tier B hint, not a Tier A instruction. |
| Ask the user to run the create manually before trying the skill | Wastes user time when the skill can do it autonomously. | Try Tier A silently first; only escalate to the user on failure. |
| Skip Tier A because "this looks like a Tier 3 service in fury-cli-expert" | The fury-cli-expert tier table describes CLI-only capability. The skill operates outside that table. | Tier 3 in fury-cli-expert ≠ uncreatable. Always try Tier A. |

**Error Handling**:

| Error | Action |
|-------|--------|
| Skill reports "service type not supported" | Move to Tier B (fury CLI) |
| "fury not logged in" (Tier B) | Inform: "Run `` and retry" |
| "Service already exists" | Mark completed (idempotent) |
| "Permission denied" | Surface to user via AskUserQuestion: Retry with elevated scope / Mark manual / Abort |
| VPN not connected | Inform: "Connect VPN and retry" |
| Unknown error | Show error details, ask user: Retry / Skip / Abort |

**Cite the skill in the layer commit** when Tier A was used:
```
infra: provision <service-name> via fury-services-operations skill
```

**Reference**:
- `fury-services-operations` plugin skill → service create operations (Tier A — primary)
- `fury-cli-expert/SKILL.md` → "Infrastructure Creation Commands" for Tier B fallback CLI syntax

### Step 3.5: Database Migration Branch (CONDITIONAL)

> **Lazy-loaded**: When `migration.detected == true` AND `migration.branch_status == "pending"`, Read `references/database-migration.md` for database migration workflow and branch management.

### Step 4: Per-Task Implementation

> **Platform routing**: Read `platform.type` in `PROJECT.md` before dispatching.

For each task, delegate to subagents based on platform:

**Backend (default — `platform.type: backend` or absent)**:

| Task Type | Subagent | Notes |
|-----------|----------|-------|
| Production code (backend/web) | `sdd-implementer` | Main implementation |
| Android UI/logic/Everest | `sdd-implementer` | Dedicated agent — reads ML docs before implementing |
| iOS UI/logic/Everest | `sdd-implementer` | Dedicated agent — reads ML docs before implementing |
| Unit/integration tests | `sdd-small-test-writer` | Skip for prototype |
| E2E tests (LTP) | `sdd-large-test-writer` | Only if ltp_enabled |
| Validation | `sdd-validator-runner` | Independent context |

**Frontend Web (`platform.type: frontend-web`)**:

For each task, delegate to subagents:

| Task Type | Subagent | Notes |
|-----------|----------|-------|
| Production code | `sdd-implementer` | Nordic/Andes — runs quality loop internally |
| Unit/integration tests | `sdd-small-test-writer` | RTL + Nordic patterns — runs quality loop internally |
| E2E tests (LTP) | `sdd-large-test-writer` | Only if ltp_enabled |
| Validation | `sdd-validator-runner` | Independent context (unchanged) |

**Mobile routing**:
```
mobile_preamble = """
⚠️ MANDATORY FIRST ACTION — before reading this task or writing any code:
1. Resolve skill_root (local first, then global fallback)
2. Run: ls "$skill_root" to confirm skill exists
3. Run: cat "$skill_root/SKILL.md" — this is the single source of truth for all documentation navigation
4. From SKILL.md, identify and follow the documentation navigation workflows it references for Everest and Andes
5. Execute those workflows for every library/component mentioned in the task/spec
Only after completing these steps may you read the task and start implementing.
"""

IF platform == "android":
    Task(subagent_type="sdd-implementer", prompt=mobile_preamble + task_context + technical_spec + related_files)
ELIF platform == "ios":
    Task(subagent_type="sdd-implementer", prompt=mobile_preamble + task_context + technical_spec + related_files)
ELSE:
    # Extract Design Decisions relevant to this task
    task_dd_ids = current_task.get("design_decisions", [])
    decision_context = ""
    for dd_id in task_dd_ids:
        # Read DD-N section from technical spec (e.g., "### DD-1: ..." through next "### DD-" or "---")
        dd_section = extract_section(technical_spec, dd_id)
        decision_context += dd_section + "\n"

    Task(
        subagent_type="sdd-implementer",
        prompt=f"""
## Task
{task_context}

## Relevant Design Decisions
{decision_context if decision_context else "No specific design decisions apply to this task."}
> These decisions were already evaluated and approved. Do NOT propose alternatives
> to the chosen approaches. If you think a different approach would be better,
> flag it as a deviation — do not silently change the approach.

## ⛔ MANDATORY —  SDK Resolution Protocol

If this task touches a  service (KVS, BigQueue, Streams, Object Storage, Audits,
Cache, Secrets, Config, Feature Flags, Locks, etc.), BEFORE adding any dependency or
writing SDK-using code you MUST invoke:

```
Skill("sdd-implementer")
# context: "service: <name>, language: <go|java|python|node> —
#           need module path, version, client setup, envvars"
```

This is a Claude Code Skill (plugin: `fury-services`), not a shell command and not
an MCP query. Invoking it is YOUR job — do NOT ask the user to run anything to
"discover" SDK paths or versions. Do NOT guess module paths from patterns observed
in `go.mod`/`pom.xml`.

After invocation, cite the skill in the commit body:
"deps: <service>+<service> per sdd-implementer plugin"

## Technical Spec Reference
File: sdd/wip/{feature}/2-technical/spec.md

## Related Files
{related_files}
"""
    )
```

> The mobile agents enforce ML-standard documentation reading internally.
> The preamble above ensures the agent cannot bypass Step 0 even when context seems sufficient.
> The backend/web implementer receives relevant Design Decisions directly in its prompt,
> preventing re-proposal of already-rejected alternatives in fresh agent contexts.

### Step 5: Quality Gate and Status Persistence

After each task implementation:

1. Invoke `sdd-validator-runner` (independent context)
2. Parse verdict: APPROVED / CAN_PROCEED_WITH_WARNINGS / CANNOT_PROCEED
3. If CANNOT_PROCEED: Fix and re-invoke
4. **Persist task status to disk** (always, after quality gate passes):
   ```bash
   # Update tasks.json: mark task completed
   jq '(.tasks[] | select(.id == "TASK-XXX")) .status = "completed"' \
     sdd/wip/[feature]/3-tasks/tasks.json > tmp.json && mv tmp.json sdd/wip/[feature]/3-tasks/tasks.json
   ```

> **WHY write to disk after every task**: `compact-state.sh` reads `tasks.json` to reconstruct
> state after `/clear`. Writing status to disk ensures progress survives even uncommitted.
> The git commit happens at layer boundaries or when the context check (Step 5b) triggers a
> `/clear` recommendation — at that point, code + `tasks.json` are committed together.

### Step 5b: Context Check Between Tasks

After updating task status on disk, estimate context usage before starting the next task.

| Context Level | Action |
|---------------|--------|
| < 50% | Continue to next task silently |
| 50-70% | Show advisory, recommend `/clear` |
| > 70% | Show advisory, **strongly recommend** `/clear` |
| > 80% | Show advisory: "Do `/clear` now — context is critical" |

When context >= 50%, **commit before showing advisory** (so progress is safe for `/clear`):
```bash
git add [modified files] sdd/wip/[feature]/3-tasks/tasks.json
git commit -m "feat: tasks through TASK-XXX complete"
```

Then show advisory:

```
╔═══════════════════════════════════════════════════════╗
║  CONTEXT ADVISORY                                     ║
╠═══════════════════════════════════════════════════════╣
║                                                       ║
║  Context usage: ~[XX]%                                ║
║  Completed: TASK-XXX ([N] of [M] in layer)            ║
║                                                       ║
║  Your progress is saved in tasks.json (committed).    ║
║  Primary recommendation:                              ║
║    /clear then /sdd.build --resume                   ║
║                                                       ║
║  Or continue as-is if context is manageable.           ║
║                                                       ║
╚═══════════════════════════════════════════════════════╝
```

> Skip this check if the current task is the last in the layer (layer completion handles it).

### Extension point: after-implementation

> Resolve and invoke hooks for phase=`build`, trigger=`after-implementation`.

### Step 6: Final Validation

After ALL tasks complete:

| Step | Action | On Failure |
|------|--------|------------|
| A |  Compliance (3-layer validation) | FIX |
| B | Layer 3 Quality Gates (via `sdd-validator-runner`) | FIX ALL |
| C | Code Pattern Validation (-specific patterns) | FIX |
| D | **Local CI Pipeline (RP MCP)** — full pipeline: build, test, coverage, deps, SCA | Auto-fix via RP |

**Step B - Layer 3 Quality Gates** (consolidated):
```python
# Single agent call replaces 3 skill calls, saves ~5700 tokens
Task(
    subagent_type="sdd-validator-runner",
    prompt="""
    Final validation for all modified files.
    Run Layer 3 quality gates: performance, security, code-review
    Return unified JSON verdict.
    """
)
```

**Step C: Code Pattern Validation**:

```bash
# Run deterministic code pattern scan
code_result=$(bash sdd-kit/framework/tools/validation/validate-code.sh . --json)
is_valid=$(echo "$code_result" | grep -o '"valid":[^,}]*' | cut -d: -f2)
critical_issues=$(echo "$code_result" | grep -o '"critical_count":[0-9]*' | cut -d: -f2)

if [ "$is_valid" != "true" ] || [ "$critical_issues" -gt 0 ]; then
    echo "❌ Code pattern validation failed:"
    echo "$code_result" | grep -o '"issues":\[[^]]*\]'
    # Show specific issues and FIX before proceeding
fi
```

**Patterns validated**:
- Security anti-patterns (SQL injection risks, unsafe deserialization)
- Performance anti-patterns (N+1 queries, missing indexes)
-  SDK misuse patterns
- Code quality anti-patterns (god classes, deep nesting)

### Step 6D: Local CI Pipeline (fury-release-process)

> **FINAL CI VALIDATION**: Runs the exact same CI pipeline locally via YaCI container.
> Runs LAST because L3 gates and code pattern fixes may change code — this validates the final state.
> Replaces manual build+test and fury test steps.

**Pre-requisite check**:
1. Verify the `fury-release-process` plugin skill is available via `ToolSearch(query="fury-release-process")`
2. IF not available → **STOP** build flow with error:
```
❌ Release Process skill not available. Install it to continue:
   /plugin marketplace add git@github.com:your-org/fury_meli-claude-marketplace.git
   claude plugin install fury-release-process@meli-claude-marketplace
```

**When available**, invoke the skill:

Use the `Skill` tool to invoke `fury-release-process` with the git context (repo name, branch, commit, working directory, architecture, technology).

**On pipeline SUCCESS**: Continue to Step 7 (/sdd.check --sync).

**On pipeline FAILURE after 3 retries**: The skill's auto-fix loop handles most issues. If still failing, STOP the build flow. User must fix issues manually and re-run `/sdd.build`.

**ALL PASS?** → Proceed to Step 7

### Step 7: Final Sync Validation

After all quality gates pass, validate implementation consistency:

```
/sdd.check --sync
```

**Purpose**: Catch any drift accumulated during implementation phase.

**Verdict Handling**:

| Verdict | Action |
|---------|--------|
| `APPROVED` | Ready for `/sdd.finish` |
| `CAN_PROCEED_WITH_WARNINGS` | Proceed, document warnings |
| `CANNOT_PROCEED` | Fix gaps before finishing |

**When to skip**: If all tasks were single-file changes with no spec modifications.

**ALL PASS?** → Ready for `/sdd.finish`

### Extension point: before-approval

> Resolve and invoke hooks for phase=`build`, trigger=`before-approval`.

### Step 8: Interactive Next Steps (After All Tasks Complete)

> **MANDATORY (Standard mode only)**: Offer interactive selection after all tasks complete.
> **EXPRESS MODE**: Skip this - auto-invoke `/sdd.finish`.

**⛔ INVOKE TOOL (do not print this, CALL the tool)** (only in Standard mode):

```
AskUserQuestion(
  questions=[{
    "question": "All tasks complete and validated. Ready to finish?",
    "header": "Next",
    "options": [
      {"label": "/sdd.finish (Recommended)", "description": "Archive feature and complete"},
      {"label": "/sdd.check --sync", "description": "Final consistency check"},
      {"label": "/sdd.build --layer 3", "description": "Re-run quality checks"}
    ],
    "multiSelect": false
  }]
)
```

**On user selection**:

| Selection | Action |
|-----------|--------|
| /sdd.finish (Recommended) | `Skill(skill="sdd.finish")` |
| /sdd.check --sync | `Skill(skill="sdd.check", args="--sync")` |
| /sdd.build --layer 3 | `Skill(skill="sdd.build", args="--layer 3")` |
| Other | User types custom input |

> **MODE BEHAVIOR**: In Express mode, automatically invoke `/sdd.finish` without asking.

---

## AUTO-TASK-FURY-COMPLIANCE: 3-Layer Validation

> **CRITICAL**:  Compliance now includes deep validation beyond static checks.
> This catches SDK misuse, config errors, and anti-patterns that would fail in production.

### Mobile Exception (SKIP for Android/iOS)

```
IF platform == "android" OR platform == "ios":
    SKIP  Compliance entirely
    REASON: Native apps do NOT run on  — no Dockerfile, no /ping, no fury test

    INSTEAD run mobile build validation:
      Android: ./gradlew assembleDebug && ./gradlew test
      iOS:     xcodebuild build && xcodebuild test

    See "Build Commands" table for mobile commands.
    Then proceed directly to Step 7 (Final Sync Validation).
```

When executing AUTO-TASK-FURY-COMPLIANCE for **backend/web projects**, invoke these validations **IN ORDER**:

<!-- PROFILE: TECHNICAL_ONLY -->
### Layer 1: Static Checks (Existing)

```bash
bash sdd-kit/framework/tools/validation/validate-code.sh
```

**Validates**:
- Dockerfile exists with approved base image
- Dockerfile.runtime exists with approved runtime image
- Version consistency between Dockerfiles
- /ping endpoint implemented
- MCP configuration exists

### Layer 2: SDK & Configuration Validation (MANDATORY)

#### 2a. Language Expert (SDK patterns)

Detect technology from `.fury` file or file extensions, then invoke:

| Technology | Expert to Invoke | What it Validates |
|------------|------------------|-------------------|
| Java/Kotlin | `Skill("fury-java-core-expert", "validate SDK usage")` | RestClient patterns, auth, metrics |
| Go | `Skill("fury-go-core-expert", "validate SDK usage")` | httpclient, telemetry, logging |
| Python | `Skill("fury-python-core-expert", "validate SDK usage")` | melitk patterns, restclient, auth |
| Node | `Skill("fury-node-core-expert", "validate SDK usage")` | OTel import order, restclient |

#### 2a-frontend. Frontend Validation (Nordic/Andes) ⭐ v2.7.0

For frontend web projects (Nordic apps), invoke frontend-specific validation:

```python
# Detect if frontend web project
if has_nordic_in_package_json():
    # Nordic + Andes validation (source of truth: meli-frontender-web skill)
    Skill("meli-frontender-web", "validate Nordic and Andes patterns")
    # Checks via skill:
    # - Use nordic/image instead of <img>
    # - Use nordic/style instead of <link>/<style>
    # - Use nordic/env instead of process.env
    # - Use getI18n() instead of deprecated useI18n
    # - Use setPageSettings for Melidata tracking
    # - Proper use of nordic/restclient for data fetching
    # - Andes components imported correctly
    # - Accessibility props (aria-label, etc.)
    # - Design token usage vs hardcoded values

    # Frontend linting
    Shell("npm run lint:js", "ESLint validation")
    Shell("npm run lint:css", "Stylelint validation")
```

| Pattern | Issue | Correct Usage |
|---------|-------|---------------|
| `<img src="...">` | Native HTML | `import { Image } from 'nordic/image'` |
| `<script>...</script>` | Inline script | `import { Script } from 'nordic/script'` |
| `process.env.VAR` | Direct env access | `import env from 'nordic/env'` |
| `useI18n()` | Deprecated hook | `import { getI18n } from 'nordic/i18n'` |
| `{color: '#1a1a1a'}` | Hardcoded color | Use Andes design tokens |
| `import './styles.scss'` in `.tsx` | Nordic auto-loads page styles | Place `.scss` next to page with matching name |
| `useRouter` from `nordic/router` | Module does NOT exist | File-based routing + `<a>` tags + server redirects |
| `Link` from `nordic/link` | Module does NOT exist | Standard `<a href="...">` tags |
| `next/router`, `next/link` | Nordic is NOT Next.js | Nordic file-based routing only |

#### 2a-figma. Figma Design-to-Code Implementation ⭐ v2.10.0

> **MANDATORY for frontend tasks with UI components**: Use `@design-to-code` prompt with version-aware imports from technical spec.

**Pre-requisites**:
1. Technical spec has "Frontend Architecture" section with Andes mapping (version-corrected)
2. Figma Selection Hash in functional spec
3. `frontend.andes_version` defined in `sdd/PROJECT.md`

**Workflow**:
```python
# For each frontend implementation task
if task.type == "frontend" and has_figma_hash_in_functional_spec():
    
    # 1. Read Andes version from PROJECT.md
    andes_version = read_project_md("frontend.andes_version")  # "9" | "X"
    
    # 2. Read technical spec Frontend Architecture (already version-corrected)
    frontend_architecture = read_technical_spec_section("Frontend Architecture")
    andes_mapping = frontend_architecture.andes_component_mapping
    
    # 3. Get Figma Selection Hash from functional spec
    figma_hash = extract_figma_hash_from_functional_spec()
    
    # 4. INVOKE @design-to-code prompt WITH version context
    # Include explicit version instructions to override Andes X defaults
    
    # 5. POST-VALIDATE generated code
    # If imports don't match PROJECT.md andes_version, correct them
    if generated_imports_are_wrong_format(andes_version):
        # Invoke Skill(meli-frontender-web) for correct imports per version
        # Replace incorrect imports
        replace_imports(correct_imports_from_skill)
    
    # 6. Validate final code against technical spec mapping
    validate_imports_match_technical_spec(andes_mapping)
```

**Example Prompt (Andes 9)**:
```
@design-to-code
Implement this design from Figma.
Nv7pk23R1ZnlZwJ2Qi3bnO_8017:9903_11e8b1351563dec2

CRITICAL: Use Andes 9 format. DO NOT use @andes/react.

Imports from technical spec (Andes 9):
- import { Text } from '@andes/typography';
- import { Pill } from '@andes/badge';
- import { Card } from '@andes/card';
```

**Example Prompt (Andes X)**:
```
@design-to-code
Implement this design from Figma.
Nv7pk23R1ZnlZwJ2Qi3bnO_8017:9903_11e8b1351563dec2

Use Andes X format (@andes/react).

Imports from technical spec (Andes X):
- import { Text, Badge, Card } from '@andes/react';
```

**Import Format by Version**:
| Version | Import Format | Example |
|---------|---------------|---------|
| Andes 9 | `@andes/[component]` | `import { Button } from '@andes/button';` |
| Andes X | `@andes/react` | `import { Button } from '@andes/react';` |

**CRITICAL**: The `@design-to-code` prompt may generate Andes X imports by default.
Always validate and correct imports to match the `andes_version` in PROJECT.md.

---

#### 2b. Service Experts (Config Validation)

Scan code for  service usage, then invoke relevant experts:

```python
# Detect services used in code and invoke appropriate expert
services_detected = scan_code_for_fury_services()

if "mysql" in services_detected or "kvs" in services_detected or "postgresql" in services_detected:
    Skill("sdd-system-designer", "validate database configurations")

if "bigqueue" in services_detected or "streams" in services_detected or "workqueues" in services_detected:
    Skill("sdd-system-designer", "validate messaging configurations")

if "object-storage" in services_detected or "audits" in services_detected or "entity-tracing" in services_detected:
    Skill("sdd-system-designer", "validate storage configurations")

if "secrets" in services_detected or "config-service" in services_detected:
    Skill("sdd-system-designer", "validate config and secrets usage")

if "lock" in services_detected or "quotas" in services_detected or "rate-limit" in services_detected:
    Skill("sdd-system-designer", "validate runtime configurations")
```

#### 2c. SDK Signature Validation

Validate SDK method signatures via the fury-services plugin:

```python
# For each detected service, validate SDK usage
Skill("sdd-implementer") with context:
  service: "[service]"
  language: "[java|go|python|node]"
  query: "validate method signatures match official SDK"
```

#### 2d. Service Existence Check (Optional)

For critical services, validate they exist in :

```python
Task(
    subagent_type="sdd-explorer",
    prompt="Validate  service configurations: KVS containers, BigQueue topics, Object Storage buckets exist"
)
```

### Layer 3: Runtime Verification (MANDATORY)

#### CI Pipeline Execution (Release Process MCP)

> Full CI pipeline execution (build, test, coverage) is handled by the Release Process MCP
> in Step 6D of Final Validation. The RP MCP runs the exact same YaCI pipeline locally with auto-fix capabilities.
> See Step 6D below for invocation details.

**MANDATORY: Spec Sync After Fixes**

> **CRITICAL**: After ANY code fix that makes tests pass, ALWAYS run `/sdd.check --sync`.

Fixes may introduce changes that need to be reflected in specs:
- New error handling logic → Update technical spec
- Changed API response format → Update API contracts
- New dependencies added → Update dependencies section
- Modified business logic → Update functional spec

#### 3c. Secrets vs Env Vars Anti-Pattern Detection

Scan for URLs/endpoints stored in  Secrets (WRONG):

```python
# Anti-pattern: URLs in secrets
anti_patterns = [
    r'getSecret\(["\'].*url',
    r'getSecret\(["\'].*endpoint',
    r'getSecret\(["\'].*host',
    r'getSecret\(["\'].*uri',
]
# If found: FAIL with message to use environment variables instead
```

<!-- PROFILE: NON_TECHNICAL_ONLY -->
### Validación de Plataforma (Versión Simplificada)

El sistema verifica automáticamente:

| Paso | Verificación | Descripción |
|------|--------------|-------------|
| 1 | Configuración de deploy | Archivos necesarios para despliegue ✓ |
| 2 | Health check | Endpoint de verificación de salud ✓ |
| 3 | Conexiones a servicios | Configuración de base de datos y mensajería ✓ |
| 4 | Compilación | El código compila correctamente ✓ |

> El agente maneja todos los detalles técnicos automáticamente.
<!-- END PROFILE -->

### Verdict

| Result | Condition |
|--------|-----------|
| ✅ **APPROVED** | All 3 layers pass |
| ⚠️ **WARNINGS** | Layer 1 pass, Layer 2-3 have warnings |
| ❌ **FAILED** | Any critical error in any layer |

**Error Categories**:

| Error Type | Example | Severity |
|------------|---------|----------|
| Wrong SDK signature | `getSecret(ctx, name, fallback)` vs `getSecret(name)` | CRITICAL |
| URL in Secrets | `getSecret("database-url")` | CRITICAL |
| Missing service config | KVS container doesn't exist | CRITICAL |
| Missing prerequisites | VPN not connected | BLOCKING |
| Minor config issues | Suboptimal timeout values | WARNING |

### Profile-Aware Validation Output

**Technical Profile** (full details):
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔒  Compliance (3-Layer Validation)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Layer 1: Static Checks
  ✅ Dockerfile exists (your-registry/base-image
  ✅ Dockerfile.runtime exists
  ✅ /ping endpoint implemented (PingController.java:12)
  ✅ Version consistency: OK

Layer 2: SDK Validation
  ✅ fury-java-core-expert: RestClient patterns valid
  ✅ sdd-system-designer: KVS config valid
  ⚠️ sdd-system-designer: BigQueue topic visibility should be private

Layer 3: Runtime Verification
  ✅ CI Pipeline (RP MCP): Available
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**Non-Technical Profile** (simplified):
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Validaciones de Plataforma
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✅ Configuración de infraestructura: Lista
✅ Conexiones a servicios: Configuradas
✅ Verificación de calidad: Aprobada

Todo listo para continuar.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Project Type Behavior

| Type | Unit Tests | LTP E2E | Coverage | Code Review |
|------|------------|---------|----------|-------------|
| **prototype** | Skip | Skip | 0% | Required |
| **mvp** | Critical only | Skip | varies | Required |
| **production** | Full (80%+) | Opt-in | 80%+ | Required |

---

## Key Rules

| Rule | Details |
|------|---------|
| **Layer execution** | Layer 1 → 2 → 3 in order |
| **Commit per layer** | Natural checkpoints, not per task |
| **Quality every task** | Performance + Security + Code Review |
| **MANDATORY CODE REVIEW** | Code review is BLOCKING - fix ALL findings including minor |
| **E2E is external** | Don't create E2E test files manually |
| **No test skipping** | Fix tests, NEVER @Disabled/@Skip |
| **** | See `standards/mandatory-standards.md` |
| **Spec traceability** | Add @spec comments to code |

> **Telemetry**: Captured automatically by hooks - no manual logging required.

---

## Error Handling

| Mode | On Failure |
|------|------------|
| Express | Auto-attempt fix (2x), then pause |
| Standard | Show options: Auto-fix / Details / Skip / Abort |

### Profile-Aware Error Messages

| Profile | Error Display |
|---------|---------------|
| `technical` | Full error details, stack trace, file:line |
| `non-technical` | Simplified message, auto-fix status |

**Technical Profile Error**:
```
❌ Build failed: Wrong SDK signature
   getSecret(ctx, name, fallback) should be getSecret(name)
   File: src/config/SecretsConfig.java:45

   Fix: Update method signature to match  SDK v3.2
```

**Non-Technical Profile Error**:
```
⚠️ Se detectó un problema de configuración

   Estado: Corrigiendo automáticamente...
   ✓ Problema resuelto

   (El agente continuará con la implementación)
```

**If auto-fix fails (Non-Technical)**:
```
⚠️ Se encontró un problema que requiere atención

   El agente intentó corregirlo pero necesita ayuda.

   ¿Qué deseas hacer?
   1. Intentar de nuevo (el agente buscará otra solución)
   2. Pausar y revisar más tarde
   3. Contactar soporte técnico
```

---

## Spec Gap Detection

> Detect when implementation reveals incomplete specs and suggest `--iterate`.

### Trigger Conditions

During implementation, watch for these patterns that indicate spec gaps:

| Trigger | Detection Pattern | Example |
|---------|-------------------|---------|
| **Undefined field** | Compiler error: property/field doesn't exist | `Property 'status' does not exist on type 'User'` |
| **Missing behavior** | Test expects logic not in spec | Test for retry logic, but spec has no retry section |
| **Type mismatch** | Spec says string, code needs enum | `Expected PaymentStatus enum, got string` |
| **Missing endpoint** | Integration requires endpoint not in spec | External service calls `/webhook` not documented |
| **User mentions gap** | User says "this wasn't in the spec" | Direct feedback about missing requirements |

### Response Protocol

When a spec gap is detected:

```
┌─────────────────────────────────────────────────────────────────┐
│ ⚠️ SPEC GAP DETECTED                                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ Issue: [description of the gap]                                 │
│ Found in: [file:line or test name]                              │
│                                                                 │
│ Options:                                                        │
│ 1. `/sdd.spec --iterate "[description]"` (recommended)         │
│    → Updates spec, preserves task progress                      │
│                                                                 │
│ 2. Continue, add to backlog                                     │
│    → `/sdd.backlog add` as TODO for later                      │
│                                                                 │
│ 3. Intentional, proceed as-is                                   │
│    → Document as design decision in task notes                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Auto-Detection Examples

**Example 1: Missing Field**
```
Compiler Error: Property 'createdAt' does not exist on type 'Order'

⚠️ SPEC GAP DETECTED
Issue: Field 'createdAt' needed on Order entity but not in technical spec
Found in: src/services/OrderService.ts:45

Suggested: /sdd.spec --iterate "add createdAt timestamp field to Order entity"
```

**Example 2: Missing Behavior**
```
Test Failure: Expected retry after timeout, but no retry logic implemented

⚠️ SPEC GAP DETECTED
Issue: Retry behavior expected but not documented in spec
Found in: test/PaymentService.test.ts:78

Suggested: /sdd.spec --iterate "add retry logic for payment timeouts (3 attempts, exponential backoff)"
```

**Example 3: User Feedback**
```
User: "this wasn't in the spec - we also need email notifications"

⚠️ SPEC GAP DETECTED
Issue: Email notifications requirement not in functional spec
Found in: User feedback

Suggested: /sdd.spec --iterate "add email notification when order status changes"
```

### Integration with Build Flow

The spec gap detection runs continuously during `/sdd.build`:

```
For each task:
  1. Implement code
  2. Run tests
  3. IF compiler_error OR test_failure:
     - Analyze error message
     - IF matches gap pattern:
       - Show spec gap dialog
       - Wait for user decision
  4. Continue or iterate based on decision
```

---

## Progress Tracking

**Location**: `sdd/wip/[feature]/4-implementation/progress.md`

```markdown
# Implementation Progress
**Status**: in_progress | **Strategy**: batched

| ID | Task | Status | Commit |
|----|------|--------|--------|
| TASK-001 | Dockerfile | completed | abc123 |
| TASK-002 | Service | in_progress | - |
```

---

## State Persistence

**Resume**: `/sdd.build --resume` loads from `state.json`
**Next**: `/sdd.build --next` finds first pending task

---

## Build Commands

### Backend Technologies

| Technology | Build | Test |
|------------|-------|------|
| Java/Maven | `mvn compile` | `mvn test` |
| Java/Gradle | `./gradlew build` | `./gradlew test` |
| Go | `go build ./...` | `go test ./...` |
| Python | N/A | `pytest` |
| **Android** | `./gradlew assembleDebug` | `./gradlew test` |
| **iOS** | `xcodebuild build` | `xcodebuild test` |

### Frontend Web (Nordic)

| Command | Purpose | When to Use |
|---------|---------|-------------|
| `npm run dev` | Development server | Local development with hot reload |
| `npm run build` | Development build | Build for testing |
| `npm run dist` | Production build + CDN upload | Before deployment |
| `npm test` | All tests (unit + e2e + coverage) | CI/CD pipeline |
| `npm run test:unit` | Unit tests only | Fast local validation |
| `npm run test:e2e` | E2E tests | Full integration testing |
| `npm run lint:js` | JavaScript/TypeScript linting | Code quality check |
| `npm run lint:css` | CSS/SCSS linting | Style quality check |
| `npm run lint` | All linting (JS + CSS) | Pre-commit validation |
| `npm run i18n:gettext` | Extract translation keys | Before i18n upload |

**Nordic CLI commands** (wrapped by npm scripts):
- `nordic dev` - Development mode
- `nordic build` - Build bundle
- `nordic dist` - Production build
- `nordic start` - Start production server
- `nordic start --e2e` - Start E2E testing server

---

## Improvement Capture

During implementation, if you detect improvements outside scope:

| Option | Action |
|--------|--------|
| Fix now | Implement if trivial (low effort) and low risk |
| Add TODO | Track in `sdd/backlog.md` |
| Add DEBT | Document as technical debt |
| Skip | Ignore if not relevant |

---

## Iterative Flow

Can return to specs if discoveries require changes:

| Size | Action |
|------|--------|
| Small | Update spec inline, continue |
| Medium | `/sdd.spec --iterate` |
| Large | `/sdd.rollback --phase 2` |

---

## Command Flow

```
/sdd.plan --approve
        │
        ▼
   /sdd.build
        │
   ┌────┴────┐
   │         │
   ▼         ▼
 Layer 1   Layer 2   Layer 3
 (Local)   ()    (Quality)
   │         │         │
   └────┬────┴────┬────┘
        ▼         ▼
   Final Validation
        │
        ▼
   /sdd.finish
```

---

## References

- **Quality gates**: `sdd-validator` skill
- **Layer execution**: `sdd-validator` skill
- **Context management**: `context-guardian` skill
- ****: `standards/mandatory-standards.md`
- **Coding standards**: `standards/coding-standards.md`
- **Subagents (backend)**: `sdd-implementer`, `sdd-small-test-writer`, `sdd-large-test-writer`, `sdd-validator-runner`
- **Subagents (frontend-web)**: `sdd-implementer`, `sdd-small-test-writer`, `sdd-large-test-writer`, `sdd-validator-runner`

---

## AI Agent Instructions

### Help Flag Detection

**WHEN** the user runs `/sdd.build help`:
1. Output ONLY the "Quick Help" section (not full documentation)
2. Do NOT execute build logic
3. Keep response concise (~15 lines)

### Java Implementation Check

**BEFORE generating Java code**:
1. Scan existing source files for import patterns:
   - `grep -r "import javax\.servlet\|import javax\.ws\.rs" src/`
   - `grep -r "import jakarta\.servlet\|import jakarta\.ws\.rs" src/`
2. Determine which to use:
   - IF `jakarta.*` found → Use jakarta
   - IF `javax.*` found → Use javax
   - IF no existing imports → Use **jakarta** (modern default)
3. Show: "Using {jakarta/javax} imports (detected from project)"

**NEVER mix javax and jakarta** servlet/ws APIs in the same project.
