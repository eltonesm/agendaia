# AI Agent Guidelines - Preventing Hallucinations

**Version**: 2.5.1
**Last Updated**: 2026-01-29
**Applies To**: All AI agents executing SDD Kit commands

---

## ⛔ THE #1 RULE: code review tool is NON-NEGOTIABLE

```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃                                                                          ┃
┃   AFTER ANY CODE CHANGE (build, fix, or any modification):               ┃
┃                                                                          ┃
┃   1. Call code review tool on ALL modified files                          ┃
┃   2. Fix EVERY finding (critical, major, AND minor)                      ┃
┃   3. Re-run until ZERO findings                                          ┃
┃   4. ONLY THEN commit or mark complete                                   ┃
┃                                                                          ┃
┃   ❌ SKIPPING = CRITICAL FAILURE                                         ┃
┃   ❌ "No time" = NOT AN EXCUSE                                           ┃
┃   ❌ "Minor issues" = MUST BE FIXED                                      ┃
┃                                                                          ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
```

---

## Changelog (Recent)

- **v2.5.1** (2026-01-29): **Command Continuity - Interactive Next Steps**
  - Added Command Continuity Rule requiring AskUserQuestion at end of all `/sdd.*` commands
  - Interactive flow: user selects next action, command auto-invokes via Skill()
  - Express Mode exception: auto-continue without prompting
  - Reference table for all command → next steps mappings
- **v2.5.0** (2026-01-28): **Claude Code Plan Mode Integration**
  - Added Plan Mode documentation for `/sdd.fix`, `/sdd.spec technical`, `/sdd.build`
  - Platform availability table (Claude Code vs Cursor)
  - Fallback behavior using AskUserQuestion for non-Claude Code platforms
  - Configuration via `plan_mode` settings in PROJECT.md
  - Post-Plan Mode safeguards for artifact consistency
- **v2.4.3** (2026-01-27): **Expert-Validated Snippets Protocol**
  - Added validation markers for expert-reviewed snippets
  - New section defining detection, warning, and confirmation rules
  - Sync validation script for fw.check
- **v2.4.2** (2026-01-23): **Quality Gate Hooks Policy** documentation
  - Clarified that hooks are intentional framework design
  - Added section explaining hook purpose and safe design
- **v2.4.0** (2026-01-12): **Context Steward System** + **Validator Independence**
  - Context Guardian skill for monitoring usage
  - Context Compactor subagent for state compression
  - Lazy-loading standards (pre-execution-checks, mandatory-standards, core-principles)
  - Validator runs in isolated context (sdd-validator-runner)
  - New subagents: sdd-project-wizard, 
- **v2.3.4** (2026-01-09): Smart Questioning Protocol enhancements
- **v2.3.3** (2026-01-08): When to ask vs. infer guidelines

*Full changelog in version control history*

---

## Purpose

Ensure AI agents provide accurate, reliable outputs. **Prevent hallucinations** - specs, tasks, and code must be based on facts, not assumptions.

---

## Shell Command Rules (Avoid Permission Prompts)

Claude Code has built-in safety checks that trigger permission prompts for certain patterns. **Follow these rules to avoid unnecessary prompts:**

1. **NEVER use multi-line Bash commands** — join with `;` or `&&` on a single line
   ```bash
   # ❌ WRONG (triggers "newlines that could separate multiple commands")
   approver=$(git config user.name)
   timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

   # ✅ CORRECT
   approver=$(git config user.name); timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
   ```

2. **Prefer `if/then/fi` over `&&`/`||` for file checks** — avoids "ambiguous syntax" warning
   ```bash
   # ❌ WRONG (triggers "ambiguous syntax with command separators")
   [ -f "sdd/PROJECT.md" ] && echo "found" || echo "not found"

   # ✅ CORRECT
   if [ -f "sdd/PROJECT.md" ]; then echo "found"; else echo "not found"; fi
   ```

3. **Framework tools are pre-approved** — call them directly, no need for complex wrapping

---

## Quick Reference

### Standards Files (Lazy-Loaded)

| File | Content | Loaded By |
|------|---------|-----------|
| `standards/pre-execution-checks.md` | Critical validation before commands | `/sdd.start`, `/sdd.check` |
| `standards/mandatory-standards.md` | Quality & compliance standards | `/sdd.build`, `/sdd.finish` |
| `standards/core-principles.md` | Fundamental AI behavior rules | Reference (rarely needed) |

### Key Protocols

| Protocol | Summary | Full Details |
|----------|---------|--------------|
| **Validator Independence** | Validation in isolated context | Below |
| **Context Budget** | Monitor & delegate before exhaustion | Below |
| **Smart Questioning** | Ask critical only, infer the rest | `core-principles.md` |
| **Anti-Invention** | Never invent data - mark UNKNOWN | `mandatory-standards.md` |
| **Boundaries** | Three-tier permission system (✅/⚠️/🚫) | `standards/boundaries.md` |

---

## 🌐 Agent Response Language

> **REGLA**: Responder SIEMPRE en el idioma del usuario.

### Detección de Idioma

| Indicador | Acción |
|-----------|--------|
| Usuario escribe en español | Responder en español |
| Usuario escribe en inglés | Responder en inglés |
| Usuario escribe en portugués | Responder en portugués |
| Código/comandos (neutro) | Mantener idioma de la conversación previa |

### Reglas

1. **Detectar idioma** del primer mensaje del usuario
2. **Mantener consistencia** durante toda la conversación
3. **Código y comandos** siempre en inglés (es universal)
4. **Mensajes y explicaciones** en el idioma del usuario

### Ejemplo

```
Usuario: "Quiero crear una feature de pagos"
AI: "Perfecto, voy a inicializar la feature de pagos..."  ← Español

Usuario: "I want to create a payment feature"
AI: "Perfect, I'll initialize the payment feature..."  ← English
```

---

## 📝 Spec Document Language (Independent from Response Language)

> **IMPORTANT**: Spec language and response language are INDEPENDENT settings.

### Two Language Settings

| Setting | Purpose | Source | Example |
|---------|---------|--------|---------|
| **Response language** | Agent's conversational replies | User's message language (auto-detected) | User writes Spanish → agent replies in Spanish |
| **Spec document language** | Written spec content (functional, technical) | `PROJECT.md` → `language.specs` | Specs in English regardless of chat language |

### Resolution Order for Spec Language

1. `meta.md` → `spec_language` field (feature-level override)
2. `PROJECT.md` → `language.specs` field (project-level setting)
3. Fallback → `en` (English)

### Key Rules

- A user **can chat in Spanish** but have specs written in **English** — these are independent
- A user **can chat in English** but have specs written in **Spanish** — equally valid
- **Technical terms** (API, REST, CRUD, BigQueue, KVS, OAuth, JWT, UUID, SDK, MCP) always stay in English regardless of spec language
- **Section headers** in specs follow the template structure (English)
- **User stories, descriptions, acceptance criteria** → written in the resolved spec language
- **Code identifiers** (function names, variables, class names) → always English
- **NEVER mix languages** within a single spec document

### Supported Languages

| Code | Language | Name in Language |
|------|----------|-----------------|
| `en` | English | English |
| `es` | Spanish | Español |
| `pt` | Portuguese | Português |

---

## 📖 Acronym Expansion Rules

> **MANDATORY**: First occurrence of any acronym MUST be expanded.

### Format

- **First use**: `Full Name (ACRONYM)` → "Create a Pull Request (PR) for review"
- **Subsequent**: Just `ACRONYM` → "The PR should include..."

### Rules

1. **Always expand on first occurrence** in any conversation/output
2. **Language-aware**: Expand in user's language
   - English user → "Pull Request (PR)"
   - Spanish user → "Solicitud de cambios (PR)"
3. **If unsure about an acronym**: DO NOT guess - ASK the user
   - "I see the term 'XYZ' - could you clarify what this means?"

### Common Acronyms

| Acronym | English | Spanish |
|---------|---------|---------|
| PR | Pull Request | Solicitud de cambios |
| LTP | Large Testing Platform | Plataforma de pruebas E2E de  |
| SDD | Spec-Driven Development | Desarrollo guiado por especificaciones |
| MCP | Model Context Protocol | MCP |
| CDC | Change Data Capture | Captura de cambios de datos |
| ADR | Architecture Decision Record | Registro de decisión arquitectónica |
| E2E | End-to-End | De punta a punta |
| KVS | Key-Value Store | KVS |
| TTL | Time To Live | Tiempo de vida |

**Full reference**: See `GLOSSARY.md` for complete list.

---

## 🔄 Command Continuity Rule

> **MANDATORY**: At the end of EVERY `/sdd.*` command, offer interactive selection for next steps.

### The Rule

```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃                                                                          ┃
┃   AFTER ANY /sdd.* COMMAND COMPLETES:                                   ┃
┃                                                                          ┃
┃   1. Show success message (brief)                                        ┃
┃   2. Use AskUserQuestion with relevant next options                      ┃
┃   3. On selection → Invoke the corresponding Skill automatically         ┃
┃                                                                          ┃
┃   ❌ NEVER just end with text "Next Steps" - offer interactive selection ┃
┃   ❌ NEVER require user to copy/paste commands                           ┃
┃   ✅ ALWAYS include context-appropriate options                          ┃
┃   ✅ "Other" option is automatic (user can type anything)                ┃
┃                                                                          ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
```

### Express Mode Exception

> In **Express Mode** (`/sdd.go`), skip the AskUserQuestion and auto-continue to the next command.
> Express mode prioritizes speed over interaction - the flow continues automatically.

### Implementation Pattern

```python
# After command completes successfully:
AskUserQuestion(
    questions=[{
        "question": "What would you like to do next?",
        "header": "Next",
        "options": [
            {"label": "/sdd.next-command (Recommended)", "description": "Description here"},
            {"label": "/sdd.alternate", "description": "Alternative action"},
            {"label": "/sdd.check", "description": "View status"}
        ],
        "multiSelect": False
    }]
)

# On user selection:
if selection == "/sdd.next-command (Recommended)":
    Skill(skill="sdd.next-command")
elif selection == "/sdd.alternate":
    Skill(skill="sdd.alternate")
elif selection == "/sdd.check":
    Skill(skill="sdd.check")
# "Other" → User types custom input, handle accordingly
```

### Why This Matters

- **Reduces friction**: One click instead of typing commands
- **Prevents typos**: No manual command entry
- **Maintains flow**: Seamless transition between phases
- **Better UX**: Users stay in the flow without context switching

### Reference Table

| Command | Next Steps Options |
|---------|-------------------|
| `/sdd.start` | `/sdd.spec`, `/sdd.spec --audio`, `/sdd.check` |
| `/sdd.spec` (functional approved) | `/sdd.spec technical`, `/sdd.spec --iterate`, `/sdd.check` |
| `/sdd.spec` (both approved) | `/sdd.plan`, `/sdd.spec --iterate`, `/sdd.check` |
| `/sdd.plan` | `/sdd.build`, `/sdd.build --layer 1`, `/sdd.check` |
| `/sdd.build` (layer complete) | `/sdd.build`, `/sdd.check --compact`, `/sdd.check --sync` |
| `/sdd.build` (all tasks done) | `/sdd.finish`, `/sdd.check --sync`, `/sdd.build --layer 3` |
| `/sdd.finish` | `/sdd.start`, `/sdd.backlog list`, `/sdd.list` |
| `/sdd.check` | Phase-dependent (see check.md) |
| `/sdd.import` | `/sdd.spec`, `/sdd.check` |
| `/sdd.list` | `/sdd.start`, `/sdd.check <feature>`, `/sdd.backlog` |

---

## 👤 Profile-Aware Content Display

> **RULE**: Adapt ALL output based on user profile (technical vs non-technical).

### Profile Detection (Check in Order)

```bash
# 1. Current feature's meta.md (highest priority)
grep "type:" sdd/wip/*/meta.md | grep -o 'technical\|non-technical'

# 2. Project's PROJECT.md defaults
grep "user_profile:" sdd/PROJECT.md | grep -o 'technical\|non-technical'

# 3. Global user preference (persistent)
cat ~/.sdd-kit/user-profile.yaml | grep "^profile:" | cut -d: -f2

# 4. Default: non-technical (broadest audience)
```

### Display Rules by Profile

| Aspect | Technical Profile | Non-Technical Profile |
|--------|-------------------|----------------------|
| **Layers** | "Layer 1", "Layer 2", "Layer 3" | "Implementation", "Integration", "Review" |
| **Complexity** | "Low", "Medium", "High" | "Sencillo", "Moderado", "Complejo" |
| **Services** | "KVS", "BigQueue", "Cache" | "Data storage", "Message system", "Fast storage" |
| **Database** | "MySQL", "PostgreSQL", "NoSQL" | "Database" |
| **Dockerfile** | Show Dockerfile details | "Platform configuration ✓" |
| **SDK/API** | Show code snippets | Hide (agent implements) |
| **Errors** | Full stack trace + fix | "Issue detected - fixing..." |
| **Strategy** | Ask Sequential/Batched/Parallel | Auto-select Batched |

> **NEVER give time or duration estimates** (e.g., "~30 min", "~2 hrs", "3.5 hours total").
> Time predictions are unreliable and misleading. Instead use effort labels:
> - **Technical**: "Low", "Medium", "High" complexity
> - **Non-Technical**: "Sencillo", "Moderado", "Complejo"
>
> For cost awareness, reference **token estimates** when relevant (e.g., "~80K tokens for sequential strategy").

### Term Mapping (Non-Technical)

| Technical Term | Non-Technical Alternative |
|----------------|---------------------------|
| Layer 1, 2, 3 | Paso 1, 2, 3 / Implementation, Integration, Review |
| Low/Medium/High complexity | Sencillo / Moderado / Complejo |
| KVS | Almacenamiento de datos |
| Cache | Almacenamiento rápido |
| BigQueue | Sistema de mensajes |
| MySQL/PostgreSQL | Base de datos |
| Dockerfile | Configuración de plataforma |
| /ping endpoint | Health check |
| REST API contracts | Estructura del API |
| Context budget | (hide completely) |
| code compliance | Requisitos de plataforma |
| SDK | Herramientas de plataforma |

### Auto-Decisions for Non-Technical Profile

| Decision | Auto-Select Logic |
|----------|-------------------|
| Database type | Relational needs → MySQL; Flexible → NoSQL; Key-value → KVS |
| Container setup | Always "Recommended" (container) |
| Execution strategy | Always "Batched" |
| API design | REST with standard patterns |
| Test framework | Technology default |

### Questions to SKIP for Non-Technical

| Question (DO NOT ASK) | Auto-Decision |
|-----------------------|---------------|
| "Which  service?" | Auto-detect from requirements |
| "Container or Testcontainers?" | Container |
| "Sequential or Parallel?" | Batched |
| "REST or GraphQL?" | REST |
| "KVS or Cache?" | Auto-detect by TTL needs |

### Content Markers (for command files)

Use these markers in command .md files for conditional content:

```markdown
<!-- PROFILE: ALL -->
This content shows for everyone

<!-- PROFILE: TECHNICAL_ONLY -->
This content only shows for technical profile
(layers, complexity ratings, code snippets, etc.)

<!-- PROFILE: NON_TECHNICAL_ONLY -->
This content only shows for non-technical profile
(simplified terms, time estimates, etc.)
```

### Example Outputs

**Task Display - Technical**:
```
| ID | Title | Layer | Complexity | Dependencies |
|----|-------|-------|------------|--------------|
| TASK-001 | Setup project structure | 1 | Low | - |
| TASK-002 | Implement KVS integration | 2 | Medium | TASK-001 |
```

**Task Display - Non-Technical**:
```
| Paso | Qué se hace | Esfuerzo |
|------|-------------|----------|
| 1 | Configuración inicial | Sencillo |
| 2 | Conexión a almacenamiento | Moderado |
```

**Error - Technical**:
```
❌ Build failed: Wrong SDK signature
   getSecret(ctx, name, fallback) should be getSecret(name)
   File: src/config/SecretsConfig.java:45
```

**Error - Non-Technical**:
```
⚠️ Se detectó un problema de configuración - corrigiendo...
   ✓ Corregido automáticamente
```

---

## Agent Boundaries (Three-Tier System)

> Full details: `standards/boundaries.md`

| Tier | Meaning | Action |
|------|---------|--------|
| ✅ **Always Do** | Safe operations | Execute without asking (tests, builds, reads) |
| ⚠️ **Ask First** | Potentially destructive | Show confirmation dialog (delete, deps, schema) |
| 🚫 **Never Do** | Hard stops | BLOCKED always (secrets, force push, rm -rf) |

**Quick Decision Tree**:
```
Is it in "Never Do"? ────────► STOP
Is it destructive? ──────────► ASK FIRST
Is it standard dev work? ────► DO IT
```

---

## ⛔ Pre-Execution Checks (Summary)

> Full details: `standards/pre-execution-checks.md`

Before ANY `/sdd.*` command:

| Check | Rule |
|-------|------|
| **Working Directory** | NEVER inside `.sdd-kit/` |
| ** Images** | ONLY `your-registry/base-image |
| **Dockerfiles** | ONLY FROM line, nothing else |
| **** | ALWAYS query when "" mentioned |
| **Dependencies** | NEVER invent - query  for exact coordinates |
| **SDK Methods** | NEVER invent - query  for exact API |
| **Tech Stack** | NEVER suggest different tech (Go→Java, Python→Node) - see `core-principles.md` |
| **LLM Calls** | NEVER call LLM providers directly - ALWAYS use GenIA Gateway |

---

## 📋 Claude Code Plan Mode Integration

> **RULE**: Use Claude Code's native Plan Mode for complex operations requiring user approval.

### Platform Availability

| Platform | Plan Mode Available | Mechanism |
|----------|---------------------|-----------|
| Claude Code (CLI) | ✅ Yes | `EnterPlanMode`/`ExitPlanMode` tools |
| Cursor | ❌ No | Use AskUserQuestion fallback |

### When to Use Plan Mode

| Command | Default | Trigger Conditions |
|---------|---------|-------------------|
| `/sdd.fix` | **Enabled** | `DESIGN_FLAW`, `FEATURE_GAP`, multi-component errors, systemic issues |
| `/sdd.spec technical` | **Enabled** (brownfield + technical) | Brownfield mode + technical user profile |
| `/sdd.build` | **Disabled** (opt-in) | `build_complex_tasks`, `build_layer_transitions`, `build_test_recovery` enabled |

### Plan Mode vs AskUserQuestion Comparison

| Aspect | Plan Mode (Claude Code) | AskUserQuestion (Fallback) |
|--------|-------------------------|---------------------------|
| **Enforcement** | System IMPEDES edits during planning | Convention the agent follows |
| **Persistence** | Plan file in `~/.claude/plans/` | Only text in chat |
| **Phase separation** | Clear (planning mode vs implementation) | Implicit |
| **UX** | User knows they're in "planning phase" | Less clear |
| **Platforms** | Claude Code only | All platforms |

### Detection and Fallback

```
IF EnterPlanMode tool available:
    → Use Plan Mode (better UX, system-enforced guarantees)
ELSE:
    → Use AskUserQuestion fallback (compatible with all platforms)
```

### Fallback Flow

When Plan Mode is not available:

```
┌─────────────────────────────────────────────────────────────────────┐
│  PLAN MODE FALLBACK (Non-Claude Code Platforms)                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  INSTEAD OF:                                                         │
│    EnterPlanMode → Explore → Design → ExitPlanMode                   │
│                                                                      │
│  USE:                                                                │
│    1. Explore codebase (same read-only exploration)                  │
│    2. Design plan/strategy                                           │
│    3. Display plan inline in chat                                    │
│    4. Use AskUserQuestion: "Approve this approach?"                  │
│       - Options: "Approve", "Modify", "Cancel"                       │
│    5. Continue with approved plan                                    │
│                                                                      │
│  SAME OUTCOME:                                                       │
│    Both achieve "user approves before execution"                     │
│    Plan Mode offers stronger guarantees when available               │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Configuration

Read Plan Mode settings from `PROJECT.md` or `~/.sdd-kit/config.yaml`:

```yaml
plan_mode:
  fix_complex_bugs: true          # Default: true
  spec_technical_brownfield: true # Default: true (for technical users)
  build_complex_tasks: false      # Default: false (opt-in)
  build_layer_transitions: false  # Default: false (opt-in)
  build_test_recovery: false # Default: false (opt-in)
```

### Post-Plan Mode Safeguards

After exiting Plan Mode, **ALWAYS**:

1. **Plan output is guidance only** - exploration didn't modify files
2. **Follow horizontal consistency** - if fix touches code, check if specs need update
3. **Run `/sdd.check --sync`** - validate all artifacts are consistent
4. **Warn on breaking changes** - get explicit approval before destructive operations

---

## 🔒 Expert-Validated Snippets Protocol

> **RULE**: Snippets with `🔒 EXPERT-VALIDATED` marker require user confirmation before modification.

### Detection

When reading a snippet file from `fury-snippets-expert`, check for this marker at the top:

```markdown
> **🔒 EXPERT-VALIDATED** | Reviewed: YYYY-MM-DD | Author: reviewer_name | Commit: xxxxxxx
```

### Behavior

When encountering an expert-validated snippet:

1. **DETECT**: Check for the marker at the start of the file (after the title)
2. **WARN**: Display warning to user:
   ```
   ⚠️ WARNING: This snippet is EXPERT-VALIDATED
   - Reviewed: {date} by {reviewer} (commit {commit})
   - Modifying expert-validated content may introduce errors
   ```
3. **CONFIRM**: Ask user for explicit confirmation before any edit:
   ```
   Do you want to proceed with modifying this expert-validated snippet? [y/N]
   ```
4. **LOG**: If user confirms, proceed but note the override in commit message

### Exception

Validation markers themselves can be added/updated without confirmation (meta-operations on the validation system).

### Validation Registry

Expert-validated snippets are now managed via the `sdd-implementer` plugin (fury-services@tech-plugins-marketplace). The plugin is the source of truth — no local sync check is required.

---

## 📚  Documentation Priority

> **REGLA**: `sdd-implementer` (plugin) es la única fuente de  SDK docs.

### Orden de Prioridad

| Paso | Fuente | Cuándo |
|------|--------|--------|
| 1️⃣ | `sdd-implementer` plugin | **SIEMPRE primero** - cubre TODOS los servicios con docs oficiales |
| 2️⃣ | Return PARTIAL | Si el plugin no cubre el servicio o el snippet es insuficiente |

### Rationale

- **sdd-implementer**: Tiene snippets para TODOS los servicios  con documentación oficial actualizada y vive en el plugin (siempre fresco).

### Reglas

1. **SIEMPRE** empezar con `Skill("sdd-implementer")`
2. **NUNCA** usar WebSearch para documentación 
3. Si el plugin no cubre → return PARTIAL y sugerir documentación oficial

---

## Validator Independence Protocol

> **THE RULE**: You CANNOT validate your own code in the same context.

### The Problem (Self-Validation Bias)

Same agent writes code AND validates it → can rationalize failures → "OK" despite issues.

### The Solution (Context Isolation)

```python
# MANDATORY: Use subagent for validation
Task(
    subagent_type="sdd-validator-runner",
    prompt="Validate files: [list]. Run: build, tests, security, performance.",
    model="sonnet"
)
```

### Verdict Rules

| Verdict | Action |
|---------|--------|
| `APPROVED` | Proceed to next task |
| `CAN_PROCEED_WITH_WARNINGS` | Proceed, note warnings |
| `CANNOT_PROCEED` | Fix issues, re-invoke subagent |

### Prohibited

- ❌ Running validation skills directly (use subagent)
- ❌ Interpreting results ("it's probably fine")
- ❌ Overriding CANNOT_PROCEED
- ❌ Marking complete without APPROVED

---

## Context Budget Protocol

> **THE RULE**: Monitor context and delegate before exhaustion.

### Thresholds

| Usage | Status | Action |
|-------|--------|--------|
| 0-40% | `NORMAL` | Inline OK |
| 40-60% | `ELEVATED` | Prefer subagents |
| 60-80% | `DELEGATE_MODE` | MANDATORY delegation |
| 80%+ | `CRITICAL` | Compact context |

### Context Costs (Estimates)

| Operation | Tokens | Delegation |
|-----------|--------|------------|
| Small file (<100 lines) | ~200 | Inline OK |
| Large file (500+ lines) | ~10,000 | Explore agent |
| MCP SDK docs | ~1,500 |  |
| PROJECT.md wizard | ~15,000 | sdd-project-wizard |

### Monitoring

```
Skill(skill="context-guardian")  # Check status
```

### Compaction

```bash
bash ~/.sdd-kit/tools/genai/genai-compact-state.sh sdd/wip/[feature] --level STANDARD
```

**Full documentation**: `CONTEXT_STEWARD.md`

---

## 🔗 Quality Gate Hooks Policy

> **DESIGN DECISION**: Hooks are intentional framework features, not external restrictions.

### What Are Quality Gate Hooks?

The framework uses Claude Code hooks to enforce quality gates during `/sdd.build`. These hooks are:

- **Installed globally** via `sdd-kit install` → `~/.claude/settings.json`
- **Triggered on Edit tool** usage during Layer 3 task implementation
- **Purpose**: Ensure code review, performance, and security checks before task completion

### Hook Configuration

```json
// ~/.claude/settings.json (installed automatically)
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Edit",
        "hooks": [
          {
            "type": "command",
            "command": "~/.sdd-kit/tools/shared/check-quality-task.sh",
            "timeout": 5
          }
        ]
      }
    ]
  }
}
```

### Why This Is NOT a Contradiction

| Concern | Clarification |
|---------|---------------|
| "Hooks should be external only" | Quality gate hooks ARE part of the framework design |
| "Restricts agent freedom" | Hooks enforce quality standards, not arbitrary limits |
| "Could block legitimate work" | Hook exits immediately if `sdd/wip` doesn't exist |

### How It Works

1. Agent calls `Edit` tool during `/sdd.build`
2. Hook script `check-quality-task.sh` runs
3. If verdict file missing/invalid → hook warns (non-blocking)
4. Agent sees warning and runs quality checks
5. Quality gates pass → task completion allowed

### Safe Design

- **No-op outside SDD workflow**: Hook exits silently if not in a Meli project
- **Non-blocking warnings**: Hook prints warnings but doesn't block edits
- **Scoped to Layer 3**: Only relevant during implementation tasks

---

## Smart Questioning (Summary)

> Full details: `standards/core-principles.md`

### NEVER Ask (Always Infer)

- Branch names (`feature/<name>`)
- Folder structure (tech stack standard)
- Endpoint format (REST conventions)
- Test framework (tech stack standard)

### ALWAYS Ask (Critical)

- Prototype, MCP, or Production?
- Backend API or Frontend?
- Architecture pattern?
- Business acceptance criteria?
- SLAs/performance requirements?

### Rule

```
Before asking: Did user already mention this? Can I infer from context?
```

---

##  Docker Images

> **ONLY these images are allowed**

| Language | Image |
|----------|-------|
| Java 21 | `your-registry/base-image |
| Node.js 24 | `your-registry/base-image |
| Go 1.25 | `your-registry/base-image |
| Python 3.13 | `your-registry/base-image |

```dockerfile
# ✅ CORRECT - Dockerfile must be ONLY this line
FROM your-registry/base-image
```

**Full requirements**: [fury-mandatory-checklist.md](standards/mandatory-standards.md#1-dockerfile--dockerfile-runtime---mandatory)

---

## 🔐 Secrets Management (BLOCKER)

> **CRITICAL**: ALL secrets MUST use  Secrets. Hardcoded secrets BLOCK deployment.

### What MUST Use  Secrets

| Type | Examples | Env Var Pattern |
|------|----------|-----------------|
| Passwords | Database, service passwords | `SECRET_*_PASSWORD`, `SECRET_*_WPROD` |
| Usernames | Database usernames | `SECRET_*_USER`, `SECRET_*_WPROD_USER` |
| API Keys | External/internal API keys | `SECRET_*_API_KEY` |
| Tokens | JWT secrets, OAuth tokens | `SECRET_*_TOKEN` |
| Credentials | AWS, GCP service accounts | `SECRET_AWS_*` |
| Encryption Keys | AES, RSA private keys | `SECRET_*_KEY` |

### What is NOT a Secret (Environment Variables)

| Type | Examples | Env Var Pattern |
|------|----------|-----------------|
| **Endpoints/URLs** | MySQL host, API URLs | `DB_MYSQL_*_ENDPOINT`, `*_URL` |
| **Database Names** | Schema names | `DB_MYSQL_*_DATABASE` |
| **Ports** | Service ports | `*_PORT` |
| **Timeouts** | Request timeouts | `*_TIMEOUT_MS` |
| **Topic Names** | BigQueue topics | `BIGQUEUE_*_TOPIC` |

> ⚠️ **CRITICAL**: Endpoints and URLs are NEVER secrets. Only passwords, keys, and tokens are secrets.

### Correct Pattern

```java
// ✅ CORRECT: Read from environment (injected by  Secrets)
String dbPassword = System.getenv("DATABASE_PASSWORD");

// ❌ WRONG: NEVER hardcode secrets
String dbPassword = "my-secret-password";
```

### Validation

During `/sdd.build` and `/sdd.finish`, scan for hardcoded secrets:

```bash
grep -rEn "(password|api_key|secret|token)\s*[:=]\s*[\"'][^\"']+[\"']" src/
```

**Full requirements**: [fury-mandatory-checklist.md](standards/mandatory-standards.md#6--fury-secrets---mandatory-blocker)

---

## /ping Health Check (MANDATORY)

> **ONLY `/ping` endpoint** -  handles K8s probes internally.

### Correct Implementation

```
GET /ping → "pong" (status 200)
```

### 🚫 ANTI-PATTERNS (DO NOT CREATE)

| Endpoint | Why Wrong |
|----------|-----------|
| `/ping/liveness` |  maps `/ping` to liveness probe |
| `/ping/readiness` |  maps `/ping` to readiness probe |
| `/health/live` | Not  standard |
| `/health/ready` | Not  standard |
| `/healthz` | K8s style - not for  |

**Rule**: Create ONLY `/ping`.  infrastructure handles everything else.

**Full requirements**: [fury-mandatory-checklist.md](standards/mandatory-standards.md#2-ping-endpoint---mandatory)

---

## Build Validation

| Tech | Build | Test |
|------|-------|------|
| Java/Maven | `mvn compile` | `mvn test` |
| Node.js | `npm run build` | `npm test` |
| Go | `go build ./...` | `go test ./...` |
| Python | N/A | `pytest` |

**Rule**: NEVER skip tests. NEVER use `@Disabled`, `@Ignore`, `test.skip()`.

---

## Anti-Patterns Summary

| Anti-Pattern | Rule |
|--------------|------|
| **Inventing data** | Mark as UNKNOWN, never guess |
| **Truncating fields** | Document ALL fields (48 fields = 48 documented) |
| **Placeholder code** | Fail explicitly, never fake success |
| **Hardcoded URLs** | Use environment variables |
| **Skipping review** | Fix ALL findings including minor |
| **Duplicate code (DRY violation)** | Extract repeated logic into reusable functions/utils |

### Duplicate Code (DRY Violation)

**NEVER** copy-paste the same logic multiple times. Reusable code makes applications simpler and more maintainable.

**This applies to ALL code**:
- Business logic
- Validation
- Error handling
- Data transformation
- API calls
- Database queries

**Example**:

```go
// ❌ WRONG: Duplicated logic
// In handler1.go
user, err := db.Query("SELECT * FROM users WHERE id = ?", id)
if err != nil {
    log.Error("failed to get user", err)
    return nil, ErrUserNotFound
}

// In handler2.go (same code duplicated!)
user, err := db.Query("SELECT * FROM users WHERE id = ?", id)
if err != nil {
    log.Error("failed to get user", err)
    return nil, ErrUserNotFound
}

// ✅ CORRECT: Extracted into reusable repository
// internal/repository/user.go
func (r *UserRepo) GetByID(ctx context.Context, id string) (*User, error) {
    user, err := r.db.Query("SELECT * FROM users WHERE id = ?", id)
    if err != nil {
        log.Error("failed to get user", err)
        return nil, ErrUserNotFound
    }
    return user, nil
}

// In any handler - reuse the repository
user, err := userRepo.GetByID(ctx, id)
```

**Rule of thumb**: If you write similar code 2+ times, extract it into a reusable function/method.

---

## Command Help

> `/sdd.<command> --help` → Shows help for that command

---

## Spec Traceability

All generated code must include:

```typescript
/**
 * @spec feature-name#section
 * @implements US-1, US-2
 */
```

---

## Telemetry

Automatic via hooks. No manual logging required.

> **Observability Design Note**:
> - **Framework-level hooks** (quality gates during development) = Internal to the framework, used for `/sdd.build` validation
> - **Application-level observability** (metrics, tracing, logging in generated apps) = External by design, uses your platform services
>
> The "external by design" constraint in FRAMEWORK_MAINTENANCE.md refers to generated applications, not framework tooling.

| Tool | Location |
|------|----------|
| Claude Code | `~/.claude/logs/` |
| Cursor | `~/.cursor/logs/` |

---

## User Context Triggers

| Phrase | Action |
|--------|--------|
| "check context" | `Skill(skill="context-guardian")` |
| "compact context" | `bash ~/.sdd-kit/tools/genai/genai-compact-state.sh` |
| "" mentioned | Invoke `Skill("sdd-implementer")` (plugin) first |

---

## References

- `standards/boundaries.md` - Three-tier permission system (✅/⚠️/🚫)
- `standards/pre-execution-checks.md` - Full pre-execution validation
- `standards/mandatory-standards.md` - Quality standards
- `standards/core-principles.md` - AI behavior principles
- `CONTEXT_STEWARD.md` - Context management system
- `skills/*/SKILL.md` - Individual command documentation

---

## Deterministic Scripts Reference

Common utility scripts for consistent, token-efficient operations.

### Feature Resolution Utility

Use `resolve-feature.sh` to consistently resolve feature references across all commands:

```bash
# Resolve feature from various input formats
feature_path=$(bash ~/.sdd-kit/tools/state/resolve-feature.sh "user-auth")
# Returns: sdd/wip/20260120-user-auth (or sdd/features/20260120-user-auth)

# Full name also works
feature_path=$(bash ~/.sdd-kit/tools/state/resolve-feature.sh "20260120-user-auth")
# Returns: sdd/wip/20260120-user-auth
```

**Input formats supported**:
- Full name: `20260120-user-auth`
- Name only: `user-auth`
- Path: `sdd/wip/20260120-user-auth`

**Use in commands**:
```bash
# Before any command that accepts feature reference
feature_path=$(bash ~/.sdd-kit/tools/state/resolve-feature.sh "$user_input")
if [ -z "$feature_path" ]; then
    echo "❌ Feature not found: $user_input"
    exit 1
fi
```

### Script Summary Table

| Script | Purpose | Token Savings | Used By |
|--------|---------|---------------|---------|
| `detect-phase.sh` | Detect current SDD phase | ~500-1000 | All commands |
| `detect-stack.sh` | Detect technology stack | ~2000-3000 | /sdd.start |
| `generate-ids.sh` | Generate task/US/E2E IDs | ~500 | /sdd.spec, /sdd.plan |
| `read-metadata.sh` | Extract feature metadata | ~500 | All commands |
| `resolve-feature.sh` | Resolve feature references | ~300 | All commands |
| `validate-*.sh` | Various validations | ~2000-5000 each | See command docs |
| `analyze-*.sh` | Analysis operations | ~1500-3000 each | /sdd.check |
| `scan-*.sh` | List/scan operations | ~1000-2000 each | /sdd.list |
| `compact-state.sh` | State extraction for compaction | ~1000 | context-guardian |

### When to Use Scripts vs LLM

| Operation | Use Script | Use LLM |
|-----------|------------|---------|
| Phase detection | ✅ Always | ❌ Never |
| ID generation | ✅ Always | ❌ Never |
| Feature resolution | ✅ Always | ❌ Never |
| Structural validation | ✅ First | Then LLM for complex |
| Content analysis | ❌ Pre-filter only | ✅ For understanding |
| Decision making | ❌ Never | ✅ Always |

**Rule**: If the operation is deterministic and doesn't require understanding, use a script.
