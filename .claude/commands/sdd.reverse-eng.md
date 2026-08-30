---
name: sdd.reverse-eng
description: Reverse engineer existing codebase to generate SDD specifications. Use when user wants to create specs from existing code.
model: opus
argument-hint: "[scope]"
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

# Command: /sdd.reverse-eng

**Version**: 2.6.2
**Last Updated**: 2026-01-21
**Description**: Reverse engineer an existing codebase to generate specs for spec-driven evolution.

**Usage**:
- `/sdd.reverse-eng` → Analyze current directory
- `/sdd.reverse-eng [path]` → Analyze specific path
- `/sdd.reverse-eng --focus api,database` → Focus on specific areas
- `/sdd.reverse-eng --focus --audio` → Record component focus via microphone

---

## Quick Help

> `/sdd.reverse-eng help` → Shows this summary

**Syntax**: `/sdd.reverse-eng [path] [flags]`

| Flag | Description |
|------|-------------|
| (none) | Analyze current directory |
| `[path]` | Analyze specific path |
| `--focus <component>` | Deep-dive into specific component, enriching existing specs |
| `--focus --audio` | Record component focus description via microphone |

**Note on `--focus`**: This flag enriches existing specs with more detail about a specific component.
It does NOT create separate spec files - it updates `functional-spec.md` and `technical-spec.md` directly.

Use cases:
- General extraction first, then `--focus PaymentService` for more detail
- Re-extract specific component that was too shallow
- Add detail to existing brownfield specs

**Examples**:
```bash
/sdd.reverse-eng                            # Analyze current directory
/sdd.reverse-eng ./src                      # Analyze specific path
/sdd.reverse-eng --focus PaymentService     # Deep-dive into PaymentService
/sdd.reverse-eng --focus --audio            # Describe component to focus via voice
```

**See also**: `/sdd.help reverse-eng` for detailed documentation

---

CRITICAL: USER INTERACTION RULES
When this skill shows JSON for AskUserQuestion, you MUST:
  1. CALL the AskUserQuestion TOOL with that exact JSON
  2. DO NOT print options using Bash (no echo, cat, printf)
  3. DO NOT ask "Which option?" as text
  4. Tables marked "REFERENCE ONLY" are for docs - do NOT print

## ⚡ First Step: Mode Selection (MANDATORY)

> **🚨 CRITICAL**: Before ANY extraction work, ALWAYS present mode selection to user.

```
┌─────────────────────────────────────────────────────────────────┐
│  /sdd.reverse-eng - Mode Selection                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Detected state:                                                 │
│  • sdd/extracted/ exists: [YES/NO]                              │
│  • sdd/specs/ exists: [YES/NO]                                  │
│                                                                  │
│  Select mode:                                                    │
│  1. FULL EXTRACTION - Complete analysis from scratch             │
│  2. UPDATE MODE - Re-analyze and merge with existing specs       │
│  3. VIEW STATUS - Show current extraction summary                │
│                                                                  │
│  [If sdd/specs/ exists but sdd/extracted/ doesn't]:            │
│  4. ENHANCE SPECS - Add missing details to existing specs        │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**⛔ INVOKE TOOL (do not print this, CALL the tool):**

```
AskUserQuestion(
  questions=[{
    "question": "Select the reverse-engineering mode for this codebase.",
    "header": "Mode Selection",
    "options": [
      {"label": "FULL EXTRACTION", "description": "Complete analysis from scratch"},
      {"label": "UPDATE MODE", "description": "Re-analyze and merge with existing specs"},
      {"label": "VIEW STATUS", "description": "Show current extraction summary"},
      {"label": "ENHANCE SPECS", "description": "Add missing details to existing specs (only if sdd/specs/ exists but sdd/extracted/ doesn't)"}
    ],
    "multiSelect": false
  }]
)
```

### Mode Behavior

| Mode | Condition | Behavior |
|------|-----------|----------|
| **FULL EXTRACTION** | Any state | Delete existing `sdd/extracted/`, create fresh, run Phase 0-7 |
| **UPDATE MODE** | `sdd/extracted/` exists | Re-run extraction, compare diffs, update ALL files |
| **UPDATE MODE + --focus** | `sdd/extracted/` exists + `--focus` flag | **Enrich** existing specs with focused component detail |
| **VIEW STATUS** | `sdd/extracted/` exists | Show summary of current extraction, no changes |
| **ENHANCE SPECS** | `sdd/specs/` exists, `sdd/extracted/` missing | Analyze code to add missing details to existing specs |

### `--focus` Behavior (CRITICAL)

> **RULE**: `--focus` ALWAYS updates base spec files. It NEVER creates `-{component}.md` suffixed files.

| Scenario | Files Updated |
|----------|---------------|
| Fresh repo + `--focus ComponentA` | Creates `functional-spec.md`, `technical-spec.md` |
| Existing specs + `--focus ComponentB` | Updates existing `functional-spec.md`, `technical-spec.md` |
| Re-run with same `--focus` | Updates same files (UPDATE MODE) |
| Re-run with different `--focus` | Enriches same files with new component detail |

**What `--focus` does**:
1. Extracts deep detail about the specified component
2. **Merges** that detail into existing specs (or creates if none exist)
3. Marks sections as `[Focused: ComponentName]` for traceability

**What `--focus` does NOT do**:
- Create `functional-spec-{component}.md` files
- Create parallel spec versions
- Delete existing content from other components

### Files to Update on Re-extraction

> **CRITICAL**: When re-running `/sdd.reverse-eng`, ALL these files MUST be **REPLACED** (not suffixed):

| File | What to Update | Location |
|------|----------------|----------|
| `functional-spec.md` | Use cases, actors, business rules | `sdd/extracted/` AND `sdd/specs/` |
| `technical-spec.md` | Architecture, APIs, integrations | `sdd/extracted/` AND `sdd/specs/` |
| `DISCREPANCIES_REPORT.md` | New/resolved discrepancies | `sdd/extracted/` |
| `DOCUMENTATION_GAPS.md` | Coverage analysis | `sdd/extracted/` |
| `raw/README.md` | Extraction date, sources, metadata | `sdd/extracted/raw/` |
| `PATTERNS.md` | Discovered project patterns | `sdd/extracted/` AND `sdd/specs/` |

### ⚠️ ANTI-PATTERN: No `-UPDATED` Suffixes

> **WRONG**: Creating `functional-spec-UPDATED.md` alongside original
> **CORRECT**: Replace `functional-spec.md` directly

```
❌ WRONG (creates confusion):
sdd/specs/
├── functional-spec.md           # Old version
├── functional-spec-UPDATED.md   # New version - user must manually rename

✅ CORRECT (clean update):
sdd/specs/
├── functional-spec.md           # Replaced with new version
```

**Update Mode MUST**:
1. Show diff summary of changes
2. Ask for confirmation if >20% changes detected
3. **REPLACE files directly** - no suffixes, no side-by-side versions
4. Update both `sdd/extracted/` AND `sdd/specs/`

---

## Subagent Delegation (MANDATORY)

> **⚠️ MANDATORY**: See [warning-hierarchy.md](../standards/warning-hierarchy.md#subagent-delegation) for the central principle.
> This command MUST delegate exploration work to the `sdd-explorer` subagent.

```
┌─────────────────────────────────────────────────────────────────────┐
│  MANDATORY SUBAGENT: sdd-explorer                                   │
│                                                                      │
│  Use Task(subagent_type="sdd-explorer") for: Phase 0-3             │
│                                                                      │
│  WHY: Reduces tokens 30-40%, isolates read-only operations,          │
│       preserves main context for synthesis.                          │
│                                                                      │
│  WORKFLOW:                                                           │
│  1. Main agent coordinates phases and writes final specs             │
│  2. sdd-explorer subagent performs all read-only exploration        │
│  3. Results returned to main agent for synthesis (Phase 4)           │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Purpose

Performs comprehensive reverse engineering in **eight phases** (0-7):

| Phase | Name | Purpose |
|-------|------|---------|
| **0** | Repository State Detection | Identify existing specs/frameworks before extraction |
| **1** | Parallel Extraction | Extract data from  AND code (both mandatory) |
| **2** | Basic Cross-Validation | Compare sources, calculate coverage |
| **3** | Deep Cross-Validation | Field-by-field comparison, detect phantom endpoints |
| **4** | Synthesis | Generate specs with 6-level confidence indicators |
| **5** | Generate PATTERNS.md | Extract established patterns from codebase |
| **6** | Consistency Check | Validate functional ↔ technical alignment |
| **7** | Spec Promotion | **Copy specs to `sdd/specs/`** for brownfield mode |

**Use Cases**:
1. **Onboarding**: Document existing system for new team members
2. **Evolution**: Prepare system for spec-driven feature development
3. **Migration**: Create specs before technology migration
4. **Compliance**: Generate architecture documentation for audits

---

## Output Structure (CANONICAL)

> **⚠️ CRITICAL**: This is the ONLY valid output structure. Any file outside these locations is an error.

```
meli/
├── extracted/                        # WORKING directory (Phases 0-5)
│   ├── raw/                          # Phase 0-1: Source data
│   │   ├── existing-specs/           # MUST CREATE if any specs detected
│   │   │   └── DETECTION_REPORT.md   # What frameworks/specs were found
│   │   ├── mcpfury/                  # MUST CREATE
│   │   │   ├── functional-docs.md
│   │   │   ├── technical-docs.md
│   │   │   └── openapi.yaml
│   │   ├── code-analysis/            # MUST CREATE always
│   │   │   ├── architecture/
│   │   │   ├── api-specs/
│   │   │   ├── fury-services/
│   │   │   ├── database/
│   │   │   └── deployment/
│   │   └── README.md                 # Extraction metadata
│   │
│   ├── DOCUMENTATION_GAPS.md         # Phase 2: Cross-validation report
│   ├── DISCREPANCIES_REPORT.md       # Phase 3: Field-level validation
│   ├── functional-spec.md            # Phase 4: Synthesized spec
│   ├── technical-spec.md             # Phase 4: Synthesized spec
│   ├── PATTERNS.md                   # Phase 5: Discovered project patterns
│   └── README.md                     # Index and metadata
│
├── specs/                            # FINAL location (Phase 7)
│   ├── functional-spec.md            # ← PROMOTED from extracted/
│   └── technical-spec.md             # ← PROMOTED from extracted/
│
└── PATTERNS.md                       # ← PROMOTED from extracted/ (root level)
```

**KEY POINTS**:
- `sdd/extracted/` = Working directory with all extraction artifacts
- `sdd/specs/` = **Final location** for global specs (created in Phase 7)
- Phase 7 **PROMOTES** specs from `extracted/` to `specs/`
- Both  AND code analysis are **mandatory**
- **NEVER write files to `meli/` root except `PATTERNS.md`**

---

## Directory Creation Rules (MANDATORY)

> **🚨 CRITICAL**: Before writing ANY file, ensure parent directories exist.

**Phase 0 Pre-step** - Create complete structure:

```bash
# ALWAYS execute at start of extraction
mkdir -p sdd/extracted/raw/existing-specs/
mkdir -p sdd/extracted/raw/mcpfury/
mkdir -p sdd/extracted/raw/code-analysis/architecture/
mkdir -p sdd/extracted/raw/code-analysis/api-specs/
mkdir -p sdd/extracted/raw/code-analysis/fury-services/
mkdir -p sdd/extracted/raw/code-analysis/database/
mkdir -p sdd/extracted/raw/code-analysis/deployment/
mkdir -p sdd/specs/
mkdir -p sdd/wip/
```

**Validation Rule**: If any directory creation fails, STOP and report error.

---

## Forbidden File Names

> **🚨 NEVER generate these files** - they indicate unstructured output.

| Forbidden Pattern | Why It's Wrong |
|-------------------|----------------|
| `FOCUSED_ANALYSIS_*.md` | Analysis should go into standard phase outputs |
| `*_DEEP_DIVE.md` | Deep dives belong in PATTERNS.md or specs |
| `*_USE_CASE_*.md` (standalone) | Use cases go IN functional-spec.md |
| `*_FLOW_DIAGRAMS.md` | Diagrams go IN the relevant spec file |
| `*_RESERVE.md` | No ad-hoc reserve files |
| Files in `meli/` root | Only `PATTERNS.md` allowed at root |

**If analysis is needed**: It MUST go into the appropriate phase output file:
- Use case analysis → `functional-spec.md`
- Technical deep dive → `technical-spec.md`
- Pattern analysis → `PATTERNS.md`
- Gaps/issues → `DOCUMENTATION_GAPS.md` or `DISCREPANCIES_REPORT.md`

---

## Eight-Phase Workflow

### Phase 0 Pre-step: Ensure Standard Structure (MANDATORY)

> **PURPOSE**: Create the meli/ directory structure if it doesn't exist.

**ALWAYS execute BEFORE Phase 0 detection**:

```bash
# Check if meli/ exists
if [ ! -d "sdd" ]; then
    # Create standard structure
    mkdir -p sdd/specs
    mkdir -p sdd/extracted/raw/code-analysis
    mkdir -p sdd/wip

    echo "✅ Created meli/ directory structure. This repo is now ready for SDD workflow."
fi
```

**Structure Created**:
```
meli/
├── specs/           # For promoted global specs
├── extracted/
│   └── raw/
│       └── code-analysis/
└── wip/             # For feature work-in-progress
```

**Why This Matters**: Users reported confusion when `meli/` didn't exist. This ensures a consistent starting point regardless of repo state.

---

### Phase 0 Pre-step 2: Project Configuration Check (MANDATORY)

> **PURPOSE**: Ensure PROJECT.md and CLAUDE.md exist before extraction, so specs are written in the correct language.

**ALWAYS execute AFTER directory structure creation, BEFORE Phase 0 detection**:

```pseudocode
# 1. Ensure PROJECT.md exists (needed for language resolution)
IF NOT EXISTS sdd/PROJECT.md:
    → Invoke Skill("sdd.project") — runs wizard, creates PROJECT.md with language
    → Wait for wizard completion before continuing

# 2. Ensure CLAUDE.md exists (Claude Code session bootstrap)
IF .claude/ directory exists:
    # Resolve language from PROJECT.md
    spec_lang = read language.specs from sdd/PROJECT.md (fallback: "en")
    lang_names = { "en": "English", "es": "Spanish (Español)", "pt": "Portuguese (Português)" }
    lang_name = lang_names[spec_lang] or "English"

    IF NOT EXISTS CLAUDE.md:
        → Generate CLAUDE.md with SDD Kit section
           (language, workflow, links to PROJECT.md and PATTERNS.md)
        → Note: /init is a built-in CLI command and CANNOT be invoked programmatically
        → User can run /init later to enrich CLAUDE.md; framework re-injects Meli section on next run
    ELSE IF CLAUDE.md exists but missing "## SDD Kit" section:
        → Append SDD Kit section (preserve existing content)
    ELSE:
        → Update existing "## SDD Kit" section (e.g., language changed)

# 3. Continue Phase 0 with language resolved from PROJECT.md
```

**SDD Kit section** (same template as `/sdd.start` Step 9.5):
```markdown
## SDD Kit

This project uses **SDD Kit** for spec-driven development.

### Spec Language
All specifications MUST be written in **[lang_name]** (`[spec_lang]`).
Do not mix languages in specs. Technical terms (API, REST, CRUD) stay in English.

### Quick Reference
- Framework expert: `Skill("sdd-kit-expert")`
- Workflow: `/sdd.start` → `/sdd.spec` → `/sdd.plan` → `/sdd.build` → `/sdd.finish`
- Project conventions: `sdd/PROJECT.md`
- Discovered patterns: `sdd/PATTERNS.md`

### Rules
- Never create files under `sdd/specs/`, `sdd/wip/`, or `sdd/features/` manually
- Always go through the `/sdd.start` workflow
- Respect the phased workflow — don't skip phases
```

> **CONDITIONAL — Mobile Implementation Rule** (append ONLY when `platform = android` or `platform = ios`):
>
> After the base template above, if the detected platform (from `detect-stack.sh` or project files) is
> `android` or `ios`, append a **platform-specific** section to CLAUDE.md.
> Do NOT append for backend, web, or empty platform.
> Do NOT include the other platform's skill name — keep it project-specific.

**If platform = android**, append:

```markdown
## Mobile Implementation Rule

This project is **Android** — MANDATORY before any Kotlin/Android code:

1. Invoke `Skill("meli-frontender-android")`
2. Read `$SKILL_PATH/SKILL.md` — single source of truth for all documentation navigation
3. Follow the documentation navigation workflows referenced in SKILL.md for Everest and Andes
4. Build Confirmed Imports Registry from the skill docs before writing any code

**When it applies**: spec creation, task planning, implementation, code review — any step that touches Kotlin/Android.

**For subagents**: include as step 0 in the prompt of any subagent that works on Android code:
```
⚠️ STEP 0 — MANDATORY:
Skill("meli-frontender-android")
cat "$SKILL_PATH/SKILL.md"
Follow the documentation navigation workflows referenced in SKILL.md for Everest libraries and Andes components.
Build Confirmed Imports Registry. Only then read the task and write code.
```

`meli-frontender-android` is the ONLY authoritative source for Everest library APIs and Andes
component APIs. Pre-training knowledge about Android libraries MUST be overridden by the skill docs.
```

**If platform = ios**, append:

```markdown
## Mobile Implementation Rule

This project is **iOS** — MANDATORY before any Swift/iOS code:

1. Invoke `Skill("meli-frontender-ios")`
2. Read `$SKILL_PATH/SKILL.md` — single source of truth for all documentation navigation
3. Follow the documentation navigation workflows referenced in SKILL.md for Everest and Andes
4. Build Confirmed Imports Registry from the skill docs before writing any code

**When it applies**: spec creation, task planning, implementation, code review — any step that touches Swift/iOS.

**For subagents**: include as step 0 in the prompt of any subagent that works on iOS code:
```
⚠️ STEP 0 — MANDATORY:
Skill("meli-frontender-ios")
cat "$SKILL_PATH/SKILL.md"
Follow the documentation navigation workflows referenced in SKILL.md for Everest libraries and Andes components.
Build Confirmed Imports Registry. Only then read the task and write code.
```

`meli-frontender-ios` is the ONLY authoritative source for Everest library APIs and Andes
component APIs. Pre-training knowledge about iOS libraries MUST be overridden by the skill docs.
```

---

### Phase 0: Repository State Detection

> **PURPOSE**: Identify if the repository already has specifications or follows an established spec framework.

**Detection Matrix** (execute in order):

| Framework | Detection Patterns | Confidence |
|-----------|-------------------|------------|
| **SDD Kit** | `sdd/specs/*.md`, `sdd/wip/*/spec.md` | 🟢 High |
| **OpenSpec** | `openspec/specs/`, `openspec/project.md` | 🟢 High |
| **GitHub Spec-Kit** | `memory/`, `.markdownlint-cli2.jsonc` | 🟢 High |
| **Kiro** | `.kiro/` folder OR triplet with Kiro markers | 🟡 Medium |
| **Tessl** | `.tessl/framework/`, `@generate`/`@test` tags | 🟢 High |
| **Cursor Rules** | `.cursor/rules/*.md`, `.cursorrules` | 🟡 Medium |
| **Claude Code** | `CLAUDE.md`, `.claude/settings.json` | 🟡 Medium |
| **Codex** | `.codex/instructions.md`, `.codex/AGENTS.md` | 🟡 Medium |
| **SpecStory** | SpecFlow methodology, captured conversation specs | 🟡 Medium |
| **OpenAPI/Swagger** | `openapi.yaml`, `swagger.json` | 🟢 High |
| **ADR/RFC** | `docs/adr/`, `docs/rfc/` | 🟡 Medium |
| **Plain Docs** | `ARCHITECTURE.md`, `DESIGN.md` | 🟡 Medium |

> **Reference**: See `sdd-explorer` agent for complete detection commands.

**Optimization Strategies** (based on detected frameworks):

| Strategy | When to Use | Expected Speedup |
|----------|-------------|------------------|
| **INCREMENTAL** | SDD Kit detected | 60-80% faster |
| **AUGMENTED** | OpenSpec, Spec-Kit, Kiro detected | 40-60% faster |
| **API_ANCHORED** | OpenAPI/Tessl detected | 30-50% faster |
| **ASSISTED** | Cursor Rules, ADR, Plain Docs | 10-20% faster |
| **FULL** | No frameworks detected | Baseline |

**Output**: `DETECTION_REPORT.md` with findings + selected `optimization_strategy`

**DETECTION_REPORT.md Template**:
```markdown
# Detection Report

**Generated**: [ISO-8601 timestamp]
**Repository**: [repo name]

## Extraction Scope

**Mode**: [FULL | UPDATE | ENHANCE]
**Focus Component**: [component name if --focus used, "Full Repository" otherwise]

## Detected Frameworks

| Framework | Confidence | Files Found |
|-----------|------------|-------------|
| [name] | 🟢 High / 🟡 Medium | [file list] |

## Selected Strategy

**Strategy**: [INCREMENTAL | AUGMENTED | API_ANCHORED | ASSISTED | FULL]
**Rationale**: [why this strategy was chosen]

## Detected Specs Summary

| Spec Type | Location | Last Modified |
|-----------|----------|---------------|
| [type] | [path] | [date] |

## Extraction History

| Date | Mode | Focus | Summary |
|------|------|-------|---------|
| [ISO-8601] | [mode] | [component or "-"] | [brief description] |

## Recommendations

- [Action items based on findings]
```

---

### Phase 1: Parallel Extraction

Extract data from **both sources simultaneously**. No interpretation, just facts.

```
┌─────────────────────────────────────────────────────────────────┐
│                    PARALLEL EXTRACTION                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Queries ─────────────┐                                 │
│    • get_app_documentation    ├──► mcpfury/                     │
│    • search_api_specs         │                                 │
│                               │                                 │
│  Code Analysis ───────────────┼──► code-analysis/               │
│    • Stack detection          │                                 │
│    • API extraction           │                                 │
│    • Database extraction      │                                 │
└───────────────────────────────┴─────────────────────────────────┘
```

**Step 0: Determine App Name** (CRITICAL)

```bash
# ALWAYS read from .fury file first (100% reliable)
APP_NAME=$(grep "^application_name:" .fury | sed 's/application_name: *//')
```

** Queries**:
1. `get_app_documentation(app_name)` - General documentation
2. `search_api_specs(app_name)` - OpenAPI specs
3. Conditional queries if coverage < 70%

**Code Analysis** (delegate to sdd-explorer):
1. Stack Detection - Java, Node, Go, Python
2. API Extraction - Endpoints from annotations/routes
3. Database Extraction - Entities, migrations
4.  Services Extraction - SDK usage, integrations
5. **Actor Discovery** - System consumers and integrations

> **Reference**: See `sdd-explorer` agent for stack-specific extraction commands.

---

#### Actor Discovery (v2.6.1)

> **PURPOSE**: Identify real system actors using authoritative architecture data.

**Source Hierarchy** (use in order):

| Priority | Source | What it provides | Reliability |
|----------|--------|------------------|-------------|
| 1. PRIMARY | **MeliSystemMCP** | Clients, Dependencies, Platform Services | Authoritative |
| 2. SECONDARY |  | Documentation, BigQueue consumers | High |
| 3. FALLBACK | Code analysis | Inferred from patterns | Medium |

---

**Step 1: Query MeliSystemMCP (PRIMARY)**

> **REQUIRES**: `MeliSystemMCP` configured in `.mcp.json`. See [MCP_SETUP_GUIDE.md](../MCP_SETUP_GUIDE.md#MeliSystemMCP-system-architecture).

```
# Inbound clients (who calls this app)
mcp__MeliSystemMCP__clients(app_name)

# Outbound dependencies (what this app calls)
mcp__MeliSystemMCP__dependencies(app_name)

# Platform services owned by this app
mcp__MeliSystemMCP__platform_services(app_name)
```

**What each returns**:

| Tool | Returns | Use for |
|------|---------|---------|
| `clients` | Services that call this app (inbound) | Actor identification |
| `dependencies` | Services + datastores this app calls (outbound) | Integration discovery |
| `platform_services` | KVS, BigQueue, OS resources owned | Platform resource mapping |

**If MeliSystemMCP unavailable**: Skip to Step 2 (). Note in `DOCUMENTATION_GAPS.md`:
```markdown
## Actor Discovery Limitations

- MeliSystemMCP unavailable - actors discovered via fallback methods
- Recommended: Configure MeliSystemMCP for authoritative data
```

---

**Step 2: Supplement with API docs and `fury-services-operations` skill (SECONDARY)**

```
# API documentation
mcp____fury__search_api_docs(app_name, query="architecture consumers integrations")

# BigQueue consumer discovery
Skill("fury-services-operations") → "list consumers of topic <topic_name> for <app_name>"
```

Use these to:
- Fill gaps not covered by MeliSystemMCP
- Get documentation context for discovered actors (via `search_api_docs`)
- Identify BigQueue consumers (via `Skill("fury-services-operations")`)

---

**Step 3: Code Analysis (FALLBACK)**

Scan for known actor patterns in code:

| Actor Type | Detection Pattern |
|------------|-------------------|
| **API Callers** | Swagger consumers, API gateway configs |
| **Message Consumers** | BigQueue/Streams subscribers |
| **Scheduled Jobs** | Cron configs,  Jobs, Director |
| **External Integrations** | REST client configs, external URLs |
| **Internal Services** | Service-to-service calls |

---

**Step 4: Consolidate in functional-spec.md**

```markdown
## System Context

### Inbound Clients (who calls us)

| Client | Type | Interaction | Source |
|--------|------|-------------|--------|
| [name] | Internal/External | [description] | MeliSystemMCP /  / code |

### Outbound Dependencies (what we call)

| Dependency | Type | Purpose | Source |
|------------|------|---------|--------|
| [name] | Service/Datastore | [description] | MeliSystemMCP /  / code |

### Platform Services Owned

| Service | Type | Purpose |
|---------|------|---------|
| [name] | KVS/BigQueue/OS/etc | [description] |

## Actors

| Actor | Type | Interaction | Evidence |
|-------|------|-------------|----------|
| [name] | Internal/External/System | [description] | [source] |

### Actor Details

#### [Actor Name]
- **Type**: [Human | System | External Service]
- **Authentication**: [How they authenticate]
- **Endpoints Used**: [Which endpoints they call]
- **Frequency**: [If known - high/medium/low volume]
```

---

**Step 5: Handle Missing Actor Data**

If all sources fail to provide complete actor data:
1. Note gap in `DOCUMENTATION_GAPS.md`:
   ```markdown
   ## Missing Actor Information

   - MeliSystemMCP: [unavailable / no data returned]
   - : [no architecture data found]
   - Code analysis: [limited patterns detected]
   - Recommended: Verify application exists in  Systems model
   ```
2. Use code analysis to infer actors from:
   - Access log patterns (if available)
   - Security/auth configurations
   - API documentation comments

---

### Exhaustive Endpoint Discovery

> **CRITICAL**: Do NOT rely solely on . You MUST scan ALL controllers in code.

**Verification Rule**: If code has N controllers, output MUST document N controllers.

**Endpoint Categories**:

| Category | Marker | Criteria |
|----------|--------|----------|
| **Verified** | ✅✅ | In  AND code, schemas match |
| **Partial** | ✅⚠️ | In both, but schemas differ |
| **Code Only** | 🔸 | In code only (undocumented) |
| **Docs Only** | ⚠️ | In  only (PHANTOM - verify!) |
| **Internal** | 🔸 INTERNAL | `/internal/*`, `/admin/*` paths |

---

### Phase 2: Cross-Validation (Basic Coverage)

Compare **THREE sources** and generate initial coverage report.

```
┌─────────────────────────────────────────────────────────────────┐
│                 THREE-WAY CROSS-VALIDATION                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  existing-specs/ ───┐                                           │
│                     │                                           │
│  mcpfury/ ──────────┼──► Compare EXISTENCE ──► Coverage %       │
│                     │                                           │
│  code-analysis/ ────┘                                           │
│                                                                 │
│  Source Priority (for conflicts):                               │
│    1. CODE (source of truth)                                    │
│    2. existing-specs (pre-validated)                            │
│    3.  (may be stale)                                    │
└─────────────────────────────────────────────────────────────────┘
```

**Output**: `DOCUMENTATION_GAPS.md` with coverage percentages per category.

---

### Phase 3: Deep Cross-Validation (Field-by-Field)

> **CRITICAL**: This phase catches the most dangerous errors - phantom endpoints, missing enum values.

**Validation Checks**:

| Check Type | What to Compare | Output |
|------------|-----------------|--------|
| **Entity Fields** |  schema vs code struct/class | Field diff table |
| **Endpoint Existence** |  routes vs code annotations | Missing routes list |
| **Enum Values** |  enum vs code enum/constants | Value diff |
| ** Services** | Mentioned services vs actual dependencies | Services diff |

**Output**: `DISCREPANCIES_REPORT.md` with prioritized action items.

---

### Phase 4: Synthesis with Confidence Indicators

Transform data into specs, marking the **origin** of each piece of information.

#### Six-Level Confidence System (CRITICAL)

| Level | Icon | Meaning | Action |
|-------|------|---------|--------|
| **THREE_WAY** | ✅✅✅ | Found in code + existing-specs + , ALL MATCH | Highest confidence |
| **VERIFIED** | ✅✅ | Found in code + one other source, fields MATCH | High confidence |
| **PARTIAL** | ✅⚠️ | Found in multiple sources, but fields DIFFER | Review diff first |
| **CODE_ONLY** | 🔸 | Found only in code (reliable, undocumented) | Consider documenting |
| **DOCS_ONLY** | ⚠️ | Found only in specs/ (NOT in code) | VERIFY before using |
| **UNKNOWN** | ❓ | Insufficient information | DO NOT USE without verification |

**Source Priority for Conflicts**:
1. **CODE** - Always the source of truth
2. **existing-specs** - Pre-validated, higher trust than 
3. **** - May be stale, lowest priority
4. **Plain Docs (README)** - HINTS ONLY, never trust without verification

#### Override Rules for Plain Docs

> **CRITICAL**: When discrepancies are found between Plain Docs (README) and CODE,
> the generated specs MUST reflect CODE reality, NOT README claims.

**Never Trust README For**:
- Storage technology choices (verify against service imports)
-  service integrations (verify against pom.xml/package.json)
- Processing architectures (verify against actual implementations)
- API versions (verify against controller annotations)

**Generates**:
- `functional-spec.md` - From use cases, capabilities (with confidence)
- `technical-spec.md` - From architecture, APIs (with confidence)
- `DISCREPANCIES_REPORT.md` - Field-level validation results
- `PATTERNS.md` - Discovered project patterns

#### Focused Extraction Merge Strategy (v2.6.2)

When `--focus <component>` is used with existing specs:

**Step 1**: Load existing specs
```bash
if [ -f "sdd/extracted/functional-spec.md" ]; then
    EXISTING_FUNCTIONAL=$(cat sdd/extracted/functional-spec.md)
fi
```

**Step 2**: Extract focused component detail
- Deep analysis of specified component
- More detailed use cases, edge cases, error flows
- Implementation specifics

**Step 3**: Merge strategy

| Spec Section | Merge Behavior |
|--------------|----------------|
| **System Context** | Preserve existing, add focused component relationships |
| **Actors** | Preserve existing, add actors relevant to focused component |
| **Use Cases** | **ADD** detailed use cases for focused component with marker |
| **Data Models** | Preserve existing, expand models used by focused component |
| **API Endpoints** | Preserve existing, add detail for focused component endpoints |
| **Patterns** | Add patterns specific to focused component |

**Step 4**: Mark focused sections for traceability

```markdown
### UC-005: Extract Products from Page
<!-- Focused: ExtractPageProductsUseCase -->

[Detailed use case from focused extraction...]

<!-- End Focus -->
```

**Step 5**: Update DETECTION_REPORT.md

```markdown
## Extraction History

| Date | Mode | Focus | Changes |
|------|------|-------|---------|
| 2026-01-24 | FULL | - | Initial extraction |
| 2026-01-25 | UPDATE | ExtractPageProductsUseCase | Added UC-005, UC-006, expanded DM-003 |
```

---

### Phase 4.5: Code Ownership Mapping

> **Lazy-loaded**: When mode is FULL or ENHANCE (not UPDATE), Read `references/code-ownership.md` for code ownership analysis guidelines.

---

### Phase 5: Generate PATTERNS.md

> **PURPOSE**: Extract established patterns from the codebase to accelerate future development.

#### PATTERNS.md Content Guidelines (CRITICAL)

**What IS a Pattern (INCLUDE)**:
| Criteria | Example |
|----------|---------|
| Reusable code structure used **3+ times** in codebase | Error handling wrapper |
| Established convention with clear evidence | Naming conventions |
| Configuration pattern (env vars, feature flags) | Config loading strategy |
| Error handling strategy | Custom error types |
| Testing approach | Mock patterns |

**What is NOT a Pattern (EXCLUDE)**:
| Anti-Pattern | Where It Should Go |
|--------------|-------------------|
| One-time implementation details | `technical-spec.md` |
| Business logic specific to one use case | `functional-spec.md` |
| "Deep dive" analysis of a single flow | `technical-spec.md` |
| Architectural decisions | `technical-spec.md` |
| Use case descriptions | `functional-spec.md` |

#### Pattern Format Requirements

Each pattern MUST have:

```markdown
### [Pattern Name]

**Category**: [HTTP/API | Database | Messaging | Error Handling | Testing |  Services | Security]

**Evidence**: Used in:
- `path/to/file1.go:42`
- `path/to/file2.go:87`
- `path/to/file3.go:123`

**Example**:
```[language]
// Max 20 lines of code showing the pattern
```

**When to use**: [1-2 sentences explaining when to apply this pattern]
```

#### Max Patterns by Repository Size

| Repo Size | Max Patterns | Rationale |
|-----------|--------------|-----------|
| Small (<10k LOC) | **Max 10 patterns** | Small repos have limited patterns |
| Medium (10-50k LOC) | **Max 20 patterns** | Balanced coverage |
| Large (>50k LOC) | **Max 30 patterns** | Focus on most important |

> **⚠️ NEVER create "deep dive" documents** - all analysis goes into standard output files.

#### Extract from code analysis

| Category | What to Extract |
|----------|-----------------|
| **HTTP/API** | Client library, retry patterns, timeout configs |
| **Database** | Query patterns, ORM usage, migration style |
| **Messaging** | BigQueue/Streams patterns, ACK mode, idempotency |
| **Error Handling** | Custom error types, error wrapping, logging |
| **Testing** | Test framework, mocking strategy, coverage |
| ** Services** | KVS key patterns, TTL defaults, segment usage |
| **Security** | Auth patterns, input validation, secrets access |

**IMPORTANT**: Only document patterns that are:
- ✅ Actually used in the codebase (evidence in code)
- ✅ Non-obvious (not just standard framework usage)
- ✅ Reusable for future features
- ✅ Have **minimum 2 evidence locations** in code

---

### Phase 6: Functional ↔ Technical Consistency (MANDATORY)

> **🚨 BLOCKING**: After generating both specs, validate consistency between them.

See `standards/spec-consistency.md` for validation rules.

---

### Phase 7: Spec Promotion with Merge Confirmation (MANDATORY)

> **PURPOSE**: Copy synthesized specs to `sdd/specs/` - the canonical location for global specs.

**CRITICAL**: This phase ensures specs are in the **correct location** for the SDD workflow.

#### Step 1: Present Promotion Dialog (ALWAYS)

After Phase 6 completes, **ALWAYS** ask user before promoting:

```
┌─────────────────────────────────────────────────────────────────┐
│  📋 EXTRACTION COMPLETE - Ready to Promote                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Generated specs in sdd/extracted/:                             │
│  • functional-spec.md (X lines, Y use cases detected)            │
│  • technical-spec.md (X lines, Y endpoints documented)           │
│  • PATTERNS.md (X patterns discovered)                           │
│                                                                  │
│  [If sdd/specs/ already exists]:                                │
│  ⚠️  Current specs will be REPLACED.                             │
│                                                                  │
│  Options:                                                        │
│  1. PROMOTE NOW - Copy to sdd/specs/ (Recommended)              │
│  2. REVIEW FIRST - Let me review extracted/ manually             │
│  3. SHOW DIFF - Show what changed vs existing specs              │
│  4. SKIP PROMOTION - Keep only in extracted/                     │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**⛔ INVOKE TOOL (do not print this, CALL the tool):**

```
AskUserQuestion(
  questions=[{
    "question": "How would you like to proceed with the extracted specs?",
    "header": "Spec Promotion",
    "options": [
      {"label": "PROMOTE NOW", "description": "Copy to sdd/specs/ (Recommended)"},
      {"label": "REVIEW FIRST", "description": "Let me review extracted/ manually"},
      {"label": "SHOW DIFF", "description": "Show what changed vs existing specs"},
      {"label": "SKIP PROMOTION", "description": "Keep only in extracted/"}
    ],
    "multiSelect": false
  }]
)
```

#### Step 2: Execute Promotion

```
┌─────────────────────────────────────────────────────────────────────┐
│  SPEC PROMOTION                                                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  FROM: sdd/extracted/                                               │
│    functional-spec.md                                                │
│    technical-spec.md                                                 │
│    PATTERNS.md                                                       │
│                                                                      │
│  TO: sdd/specs/                                                     │
│    functional-spec.md    ← Global functional spec                    │
│    technical-spec.md     ← Global technical spec                     │
│                                                                      │
│  TO: meli/ (root)                                                    │
│    PATTERNS.md           ← Reference doc (not a spec)                │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

**Promotion Steps** (same for standard and focused extractions):

1. **Create directory** if not exists:
   ```bash
   mkdir -p sdd/specs
   ```

2. **Copy/Replace specs** (ALWAYS the base files):
   ```bash
   cp sdd/extracted/functional-spec.md sdd/specs/functional-spec.md
   cp sdd/extracted/technical-spec.md sdd/specs/technical-spec.md
   ```

   > **Note**: Even with `--focus`, these are the only spec files.
   > Focused detail is merged INTO these files, not stored separately.

3. **Copy PATTERNS.md** to meli root (reference doc, not a spec):
   ```bash
   cp sdd/extracted/PATTERNS.md sdd/PATTERNS.md
   ```

4. **Confirm to user**:
   ```
   ✅ Specs promoted:
      - sdd/specs/functional-spec.md
      - sdd/specs/technical-spec.md
      - sdd/PATTERNS.md

   Next steps:
   - Review specs with: /sdd.check
   - Start feature work with: /sdd.start
   ```

#### Update Mode Behavior

When `sdd/specs/` already exists (re-extraction):
- **REPLACE** existing files directly (no `-UPDATED` suffix)
- Show diff summary before replacing
- Ask for confirmation if >20% changes detected

---

## Anti-Truncation Protocol (CRITICAL)

> **NEVER TRUNCATE entity fields, enum values, or endpoint lists.**

```
┌─────────────────────────────────────────────────────────────────────┐
│  ANTI-TRUNCATION RULE                                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  If an entity has 48 fields → Document ALL 48 fields                │
│  If an enum has 15 values → List ALL 15 values                      │
│  If there are 44 endpoints → Document ALL 44 endpoints              │
│                                                                     │
│  DO NOT:                                                            │
│  - Use "..." to truncate                                            │
│  - Say "and X more"                                                 │
│  - Summarize instead of listing                                     │
│                                                                     │
│  WHY: Truncated specs cause integration failures                    │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## AI Agent Instructions


### Help Flag Detection

**WHEN** the user runs `/sdd.reverse-eng help`:
1. Output ONLY the "Quick Help" section (not full documentation)
2. Do NOT execute reverse-eng logic
3. Keep response concise (~15 lines)

### --audio Flag Detection (with --focus)

### Phase 0: Repository State Detection Rules

1. **Execute Detection FIRST** - Before any extraction
2. **Delegate to sdd-explorer** for scanning
3. **Copy detected specs** to `sdd/extracted/raw/existing-specs/[framework]/`
4. **Generate DETECTION_REPORT.md** with findings
5. **Select optimization strategy** based on detected frameworks
6. **Communicate strategy to user** before proceeding to Phase 1

### Phase 1: Parallel Extraction Rules

1. Read `optimization_strategy` from DETECTION_REPORT.md
2. Apply strategy-specific behavior
3. Execute  queries (ALL primary queries)
4. Delegate code analysis to sdd-explorer
5. Store results in `sdd/extracted/raw/`

### Phase 2: Cross-Validation Rules

1. Compare THREE sources: existing-specs vs  vs Code
2. Calculate coverage percentage per source
3. If coverage < 70%, execute conditional  queries
4. **Source Priority**: CODE > existing-specs > 

### Phase 3: Deep Cross-Validation Rules

1. For EACH entity/model: Compare fields across ALL THREE sources
2. For EACH endpoint: Verify existence in ALL THREE sources
3. For EACH enum: Compare ALL values across ALL THREE sources
4. Generate `DISCREPANCIES_REPORT.md`

### Phase 4: Synthesis Rules

1. Mark EVERY item with 6-level confidence indicator
2. **NEVER invent data** - if unknown, mark as ❓ UNKNOWN
3. Transform implementation details to capabilities
4. Use technology-agnostic language
5. Follow framework template structure
6. Follow Anti-Invention Protocol: never invent APIs, endpoints, or config that doesn't exist in the codebase

### Phase 5: PATTERNS.md Generation Rules

1. Extract patterns ONLY from actual code (not from documentation)
2. Include code evidence (file path + line reference) for each pattern
3. Categorize by: HTTP/API, Database, Messaging, Error Handling, Testing,  Services, Security
4. Only document patterns that are:
   - ✅ Actually used in the codebase (evidence in code)
   - ✅ Non-obvious (not just standard framework usage)
   - ✅ Reusable for future features
5. Output to `sdd/extracted/PATTERNS.md`

### Phase 6: Consistency Check Rules

1. After generating BOTH specs, run consistency validation
2. Check all items in `standards/spec-consistency.md`:
   - Every use case in functional → has implementation path in technical
   - Every endpoint in technical → traces to a user story
   - Data models match between specs
3. If inconsistencies found:
   - List each with severity (CRITICAL, WARNING, INFO)
   - CRITICAL blocks completion
   - WARNING requires user acknowledgment
4. Generate consistency report in synthesis output

### Phase 7: Spec Promotion Rules

1. **ALWAYS execute** - This phase is mandatory, not optional
2. **Create `sdd/specs/`** if it doesn't exist
3. **Copy specs** from `sdd/extracted/` to `sdd/specs/`:
   - `functional-spec.md`
   - `technical-spec.md`
4. **Copy PATTERNS.md** to `meli/` root
5. **On Update Mode**:
   - **REPLACE files directly** - no `-UPDATED` suffix
   - Show diff summary before replacing
   - If >20% changes, ask confirmation
6. **Confirm to user** with final file locations

### Framework Conflict Resolution (Multi-Framework Repos)

When Phase 0 detects MULTIPLE spec frameworks:

```
┌─────────────────────────────────────────────────────────────────────┐
│  MULTI-FRAMEWORK RESOLUTION                                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. IDENTIFY all detected frameworks and their coverage areas        │
│                                                                      │
│  2. DETERMINE precedence:                                            │
│     - SDD Kit > OpenSpec > Other frameworks                     │
│     - More specific > More general                                   │
│     - Newer timestamp > Older (check file dates)                     │
│                                                                      │
│  3. ASK USER which framework is authoritative:                       │
│     "Multiple spec frameworks detected. Which should be primary?"    │
│                                                                      │
│  4. MERGE non-conflicting content from secondary frameworks          │
│                                                                      │
│  5. FLAG conflicts in DETECTION_REPORT.md for manual resolution      │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Telemetry

Telemetry is captured **automatically by hooks** during reverse-engineering. No manual tracking required.

**Supported Tools**:
| Tool | Support |
|------|---------|
| Claude Code | ✅ |
| Cursor | ✅ |
| Meli Agent CLI | ✅ |

---

## Related Commands

- `/sdd.start` - Start new feature after reverse engineering
- `/sdd.import` - Import specific specs

---

## References

- **Detection commands**: `sdd-explorer` agent
- **Spec templates**: `templates/reverse-eng/`
- **Anti-Invention Protocol**: Never invent APIs, endpoints, or config
- **Consistency validation**: `standards/spec-consistency.md`
