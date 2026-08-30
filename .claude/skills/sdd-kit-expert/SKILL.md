---
name: sdd-kit-expert
description: Expert on SDD Kit framework for Spec-Driven Development. This is a SKILL (invoke via Skill tool, NOT Task/subagent). Use when user invokes /sdd.* commands, asks about spec-driven development, functional/technical specifications, task planning, or feature implementation workflow. **TRIGGER ON** meli, spec, functional spec, technical spec, SDD, feature workflow.
---

# SDD Kit Expert

> **SKILL**: Framework knowledge base for SDD workflow. Invoke with `Skill("sdd-kit-expert")`. Do NOT use `Task(subagent_type=...)` — this is a Skill, not a subagent.

You are an expert on the SDD Kit framework for Spec-Driven Development (SDD).

---

## ⛔ CRITICAL: Structure Requirements (NEVER VIOLATE)

> **These rules are MANDATORY for ALL commands. Violating them = broken workflow.**

### 1. Folder Structure (EXACT)

```
meli/
├── wip/                              # Work In Progress (active features)
│   └── YYYYMMDD-feature-name/             # Feature folder with date prefix
│       ├── 1-functional/             # Phase 1: WHAT
│       │   └── spec.md               # Functional spec file
│       ├── 2-technical/              # Phase 2: HOW
│       │   └── spec.md               # Technical spec file
│       ├── 3-tasks/                  # Phase 3: Tasks
│       │   └── tasks.json            # Task list
│       ├── 4-implementation/         # Phase 4: Code
│       │   └── progress.md           # Progress tracking
│       └── meta.md                   # Feature metadata
├── features/                         # Completed features (archived)
├── PROJECT.md                        # Project configuration
└── backlog.md                        # TODO/DEBT/IDEA items
```

### 2. Feature Naming (MANDATORY)

- Format: `YYYYMMDD-feature-name` where YYYYMMDD is the creation date
- Example: `20260120-payment-gateway`, `20260203-user-auth`, `20260325-notifications`
- Date prefix is organizational (for ordering), feature name is the identifier
- `/sdd.start` MUST create: `sdd/wip/YYYYMMDD-feature-name/`

### 3. File Naming (EXACT)

| Phase | File Path | File Name |
|-------|-----------|-----------|
| Functional | `sdd/wip/YYYYMMDD-feature/1-functional/` | `spec.md` |
| Technical | `sdd/wip/YYYYMMDD-feature/2-technical/` | `spec.md` |
| Tasks | `sdd/wip/YYYYMMDD-feature/3-tasks/` | `tasks.json` |
| Progress | `sdd/wip/YYYYMMDD-feature/4-implementation/` | `progress.md` |
| Metadata | `sdd/wip/YYYYMMDD-feature/` | `meta.md` |

**❌ WRONG**: `functional-spec.md`, `technical-spec.md`, `feature-spec.md`
**✅ CORRECT**: `spec.md` inside the numbered phase folder

### 4. Branch Creation (MANDATORY)

- `/sdd.start` MUST create branch: `feature/feature-name`
- Branch created BEFORE any file creation
- Example: `git checkout -b feature/payment-gateway`

### 5. Language (Respect PROJECT.md)

- Read `sdd/PROJECT.md` field `language.specs`
- If `es` → Generate specs in Spanish
- If `en` → Generate specs in English
- If `pt` → Generate specs in Portuguese
- If missing → Default to English (`en`)

### 6. Phased Workflow (NEVER SKIP)

**Standard Mode** (manual control):
```
/sdd.start → /sdd.spec functional → /sdd.spec technical → /sdd.plan → /sdd.build → /sdd.finish
```

**Express Mode** (orchestrated):
```
/sdd.go "feature-name"  ← Orchestrates ALL phases automatically
```

**❌ WRONG**: Doing everything in `/sdd.start` (start only creates folder + branch)
**✅ CORRECT**: Each command does ONE phase, then waits for next command
**✅ ALSO CORRECT**: `/sdd.go` orchestrates all phases in express mode

---

## Framework Overview

SDD Kit is a command-based framework that helps teams build software predictably using AI coding assistants. It enforces a 4-phase workflow:

1. **Functional Spec** (WHAT) - User experience, user stories, acceptance criteria
2. **Technical Spec** (HOW) - Architecture, APIs, data models,  services
3. **Tasks** - Granular implementation tasks with dependencies (tasks.json)
4. **Implementation** - Code generation with mandatory quality gates

## Key Commands

| Command | Purpose |
|---------|---------|
| `/sdd.go` | **Express mode** - orchestrates start→spec→plan→build→finish |
| `/sdd.start` | Initialize new feature (also `--reopen` for completed features) |
| `/sdd.spec` | Create functional/technical specs |
| `/sdd.plan` | Generate implementation tasks |
| `/sdd.build` | Implement tasks with quality gates |
| `/sdd.finish` | Validate and archive |
| `/sdd.check` | View progress and consistency |
| `/sdd.fix` | Fix errors across all layers |
| `/sdd.list` | List all features |
| `/sdd.cancel` | Cancel current feature |
| `/sdd.rollback` | Rollback to previous state |
| `/sdd.backlog` | Manage TODO/DEBT/IDEA backlog |
| `/sdd.import` | Import existing specs |
| `/sdd.reverse-eng` | Document existing codebase |
| `/sdd.help` | Get framework help |
| `/sdd.project` | View/edit PROJECT.md, `--view` opens framework viewer |
| `/sdd.hub` | Multi-app hub orchestrator (start, spec, plan, build, check, list, finish, cancel, go, sync) |

## Execution Modes

- **Express** (`/sdd.go` or `--express`): Minimal interaction, auto-advance, 3-5 critical questions
- **Standard** (default): Balanced control, confirmations at key points

## Layer-Based Execution

Tasks are organized into layers for proper sequencing:

| Layer | Name | What | When |
|-------|------|------|------|
| 1 | Local | Code, unit tests | First |
| 2 |  | Service integration, CI Pipeline (RP MCP) | After L1 |
| 3 | Quality | Code review, security, performance | After L2 |

##  Platform Integration

The framework is tightly integrated with your platform services:

### Plugins (canonical source for  SDK content)
- **fury-services plugin** - SDK docs (`sdd-implementer`) and architecture decisions (`sdd-system-designer`)

### MCP Servers
- **** - Service discovery (KVS containers, BigQ topics, etc.) and API specs for cross-app integration
- **code review tool** - Code review (mandatory after every task)
- **E2E test framework** - E2E test generation
- **** - Create/manage  applications
- **TeamsMCP** - Team and project discovery
- **MeliSystemMCP** - Internal system integrations
- **security scanner** - Security issue detection, fix suggestions, dependency safety checks

###  Services Categories (via fury-services-architect)
- **Database** (12): KVS, QKVS, Cache, MySQL, PostgreSQL, NoSQL, NewSQL, GraphDB, TSMetrics, Oracle, BigQuery, DS
- **Communication** (9): BigQueue, Streams, Workqueues, Director, Jobs, Schedule Engine, Verdi Flows, Mails, Template Processing
- **Storage** (4): Object Storage, Audits, Media Storage, Entity Tracing
- **Config** (6): Config Service, Secrets, Feature Flags, Experiments, Rules Engine, Business Configs
- **Runtime** (7): Lock, Quotas, Rate Limit,  Schemas, CKaaS, Sequences, Event Sourcing
- **AI** (4): GenIA Gateway (LLM inference + tool calling), GenIA Embeddings, VectorDB (vector storage + KNN search), Ask To Repo (code/repo analysis)

### Language-Specific Core Libraries
- **fury-go-core-expert** - Go core libraries (go-core, httpclient, telemetry)
- **fury-java-core-expert** - Java/Kotlin core libraries (meli-restclient, routing)
- **fury-node-core-expert** - Node.js core libraries (melitk-otel, restclient)
- **fury-python-core-expert** - Python core libraries (melitk-*)
- **fury-rust-core-expert** - Rust patterns (EXPERIMENTAL - REST API only, no SDK)

> **FURY-ONLY Constraint**: Never suggest non- alternatives (MongoDB → NoSQL, Redis → Cache, Kafka → BigQueue)

### User-Invocable Skills (Skill tool)

These skills are invoked via `Skill("name")`, NOT via `Task(subagent_type=...)`:

| Skill | Purpose |
|-------|---------|
| `sdd-kit-expert` | Framework knowledge base (this skill) |
| `fury-services-architect` |  service classification and architecture (lazy-loads from `references/`) |
| `context-guardian` | Context monitoring and token exhaustion prevention |
| `sdd-code-reviewer` | Code review via code review tool (mandatory after every task) |
| `sdd-performance-expert` | Performance anti-pattern detection |
| `sdd-code-reviewer` | Security rules and vulnerability review |
| `sdd-validator` | Build validation, test execution, code compliance |

### Subagents (Task Delegation)

| Subagent | Purpose | Used By |
|----------|---------|---------|
| `sdd-validator-runner` | Isolated quality gates execution | `/sdd.build`, `/sdd.finish` |
| `sdd-layer-analyzer` | Cross-layer consistency validation | `/sdd.check --sync`, `/sdd.fix` |
| `sdd-debugger` | Deep debugging and root cause analysis | `/sdd.fix` for complex bugs |
| `sdd-project-wizard` | Interactive PROJECT.md creation | `/sdd.start` when PROJECT.md missing |
| `` | MCP query delegation for context efficiency | All specs requiring  docs |
| `sdd-system-designer` | Architecture decisions, multi-stack options | `/sdd.spec technical` |
| `sdd-explorer` |  service discovery and configuration | `/sdd.spec technical` |
| `sdd-large-test-writer` | E2E test generation via LTP | `/sdd.build` for E2E tasks |
| `sdd-small-test-writer` | Unit and integration tests | `/sdd.build` for test tasks |
| `sdd-implementer` | Code implementation from specs | `/sdd.build` for implementation tasks |
| `sdd-backlog-manager` | Backlog CRUD operations | `/sdd.backlog` |
| `sdd-explorer` | Codebase exploration + code ownership mapping | `/sdd.reverse-eng` |

### GenAI Offloaded Tools

| Tool | Purpose | Used By |
|------|---------|---------|
| `genai-detect-gaps.sh` | Detect missing spec info by feature type | `/sdd.spec` Completeness Check |
| `genai-check-compliance.sh` | Pre-process code compliance validation | `sdd-explorer` |
| `genai-select-arch-pattern.sh` | Pre-select architecture pattern | `sdd-system-designer` |
| `genai-analyze-e2e.sh` | Analyze E2E test scenarios | `/sdd.plan` E2E planning |
| `genai-analyze-layers.sh` | Task layer classification | `/sdd.plan` layer assignment |
| `genai-compact-state.sh` | Context compaction (MINIMAL/STANDARD/FULL) | Context Budget Protocol |
| `genai-resolve-conflicts.sh` | Resolve spec cross-reference conflicts | `/sdd.spec` conflict detection |
| `genai-validate-project.sh` | Validate PROJECT.md conventions | `/sdd.project` validation |

## Quality Gates

Mandatory validations at every phase:

1. **Per-Task Code Review** - code review tool after EVERY file written
2. ** Compliance** - Dockerfile must use hub.furycloud.io images
3. **CI Pipeline (RP MCP)** - Must pass before finish
4. **Performance Analysis** - sdd-performance-expert on full codebase
5. **Security Analysis** - sdd-code-reviewer on full codebase

## Key Features

- **External API Auto-Discovery** - Automatically query  when APIs mentioned
- **LTP E2E Testing** - Opt-in E2E test generation via E2E test framework
- **Scaffolding Cleanup** - Auto-cleanup example code from  scaffolding
- **Greenfield/Brownfield Detection** - Adapts workflow based on project state
- **tasks.json** - Single source of truth for task tracking
- **LOCAL-SETUP Tasks** - Mock  services during local development
- **Secrets Management** - Mandatory section in technical specs
- **Spec Gap Detection** - Context-aware gap detection via `genai-detect-gaps.sh`
- **Audio Capture** - Record voice specs via `/sdd.spec --audio` with Whisper transcription
- **Compression Levels** - MINIMAL/STANDARD/FULL context compaction via `genai-compact-state.sh`
- **Agent Boundaries** - 3-tier system in `standards/boundaries.md`
- **Spec Reference Annotations** - Cross-feature references for brownfield projects (see below)
- **Feature Reopen** - `/sdd.start --reopen` brings completed features back to WIP (reverse dependency checking as gate)
- **Feature Rename** - `/sdd.start --rename` renames current feature (folder + meta.md)
- **Framework Viewer** - Interactive HTML viewer for project state (`/sdd.project --view`), outputs to `/tmp/meli-viewer/`
- **Multi-Stack Architecture Options** - During `/sdd.spec technical`, `sdd-system-designer` presents 2-3 architecture options with ASCII diagrams and pros/cons via `AskUserQuestion` (Standard mode + technical profile). Selected option recorded as ADR.
- **ASCII Architecture Diagrams** - Mandatory in technical spec approval: distinctive shapes per component type (cylinders for databases, segmented tubes for queues)
- **Database Migration Branch** - Auto-detects DB migrations in technical spec (Step 5.5), `/sdd.build` creates `migration/*` branch from master, runs `your-migration-tool init`, then returns to feature branch
- **Code Ownership Mapping** - During `/sdd.reverse-eng`, maps each component to primary/supporting/shared files with confidence scores (0.2-1.0) for brownfield development
- **Smart Backlog Workflow Modes** - `/sdd.backlog pick` supports 3 modes for DEBT/TODO items: full pipeline, technical-only (auto-generates functional), or tasks-only (auto-generates both specs)

### Spec Reference Annotations (v2.1.0)

For brownfield projects where features modify existing behavior:

| Annotation | Purpose | Example |
|------------|---------|---------|
| `<!-- overrides: path#section -->` | Completely replaces existing behavior | New login flow replaces old |
| `<!-- extends: path#section -->` | Adds to existing behavior (backward compatible) | New refund rules added |
| `<!-- deprecates: path#section -->` | Marks existing behavior as obsolete | Old endpoint deprecated |

**Usage in specs**:
```markdown
## User Stories

<!-- overrides: sdd/features/auth-v1/functional-spec.md#login-user-story -->
As a user, I can now log in using Google OAuth in addition to email/password.
```

**When to use**: Only when this feature intentionally modifies, extends, or deprecates functionality defined in another feature's spec. `/sdd.spec` conflict detection will suggest annotations when conflicts are found.

## Hub Workflow (Multi-app)

For teams with multiple  apps collaborating in a domain, `/sdd.hub` coordinates specs, planning, and build across apps from a central hub repo.

- **Detection**: `sdd/PROJECT.md` with `## Hub members` table → hub mode
- **Flow**: `/sdd.hub start` → `spec functional` → `spec technical` → `plan` → `build` → `finish`
- **Child specs**: Hub tech spec sections exported as standard kit specs into each app
- **Coordination manifest**: `tasks.json` with `type: coordination` and dependency layers
- **Compatibility**: App-level commands (`/sdd.start`, `/sdd.build`, etc.) work unchanged inside member apps
- **Guard**: `/sdd.go` detects hubs and redirects to `/sdd.hub`

> **Detailed reference**: Read `hub-guide.md` in this skill directory for complete hub documentation.

## Core Principles

1. **Functional = WHAT**: User experience, no technology details
2. **Technical = HOW**: Architecture, technology choices, implementation details
3. **Single Source of Truth**: Specs and tasks.json define what gets built
4. **Quality Gates**: Validation at every phase transition
5. **Horizontal Consistency**: Changes propagate across all layers
6. **Context Budget Protocol**: Monitor usage, delegate at 60%, compact at 85%
7. **Validator Independence**: Validation runs in isolated context (sdd-validator-runner)

## When to Use This Skill

- User asks about `/sdd.*` commands
- User wants to create functional or technical specs
- User needs help with spec-driven development workflow
- User asks about feature implementation phases
- User wants to understand the framework structure
- User needs guidance on  services selection

For detailed command documentation, read the skill files in `~/.sdd-kit/skills/sdd.*/SKILL.md` or the framework package.
