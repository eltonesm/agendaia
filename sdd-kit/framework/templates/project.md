# Project Configuration

<!--
This file defines your team's conventions and preferences.
Uncomment and modify the sections you need to customize.
These configurations apply to ALL features in this project.

Hierarchy of precedence:
1. meta.md (feature-specific) - Highest priority
2. PROJECT.md (project-wide) - Defaults for all features
3. ~/.sdd-kit/standards/ (framework) - Base standards

Stack-specific standards:
- Backend: meli_sdd_kit/framework/standards/coding-standards.md
- Frontend Web: Use `Skill(meli-frontender-web)` as source of truth
-->

---

## Project Vision

<!--
Define your project's vision here. This vision guides all functional requirements
and ensures features align with the product's purpose and goals.

The agent will reference this vision when writing functional specs to ensure
user stories, objectives, and scope decisions are aligned.
-->

<!-- Uncomment and customize:

```yaml
vision:
  # One-liner describing what your product does
  summary: "A platform that helps users [achieve X] by [doing Y]"

  # Who is this product for?
  target_users:
    - "[Primary user type]: [Their main need]"
    - "[Secondary user type]: [Their main need]"

  # Why does this product exist? What value does it provide?
  value_proposition: |
    [Describe the core value your product delivers.
    What problem does it solve? Why should users care?]

  # Guiding principles for all features (2-5 principles)
  principles:
    - "[Principle 1]: [Brief explanation]"
    - "[Principle 2]: [Brief explanation]"
    - "[Principle 3]: [Brief explanation]"

  # What this product is NOT (helps scope decisions)
  anti_goals:
    - "[What we explicitly won't do]"
    - "[Another thing out of scope for this product]"
```

**Example**:
```yaml
vision:
  summary: "A CLI tool that helps developers implement features using spec-driven development"

  target_users:
    - "Software developers: Need structured approach to feature development"
    - "Tech leads: Want consistency and documentation across team"

  value_proposition: |
    Reduces cognitive load by breaking complex features into small, validated steps.
    Ensures alignment between business requirements and implementation through specs.

  principles:
    - "Elegance over completeness: Minimal specs that capture intent, not obvious details"
    - "Human in the loop: AI assists but humans approve critical decisions"
    - "Single source of truth: Specs are the contract, code follows specs"

  anti_goals:
    - "Not a code generator that bypasses human judgment"
    - "Not a project management tool"
```

-->

---

## Plan Mode Settings

<!--
┌─────────────────────────────────────────────────────────────────────────────┐
│ IMPORTANT: Plan Mode settings are now stored in USER PROFILE               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│ Plan Mode preferences are configured per-USER (not per-project):           │
│                                                                             │
│   Location: ~/.sdd-kit/user-profile.yaml                               │
│   Manage:   /sdd.project profile        → View settings                    │
│             /sdd.project profile --edit → Update settings                  │
│                                                                             │
│ This section in PROJECT.md is for PROJECT-LEVEL OVERRIDES only.            │
│ Use it when your project needs different Plan Mode behavior than the       │
│ user's personal preference.                                                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘

Hierarchy of Plan Mode settings (highest to lowest priority):
1. meta.md (feature-specific override)
2. PROJECT.md (project-level override) ← This file
3. ~/.sdd-kit/user-profile.yaml (user preference)
4. Framework defaults

Plan Mode uses Claude Code's native EnterPlanMode/ExitPlanMode for user approval
before complex operations. On non-Claude Code platforms, falls back to AskUserQuestion.

Uncomment ONLY if you need PROJECT-LEVEL overrides:

```yaml
plan_mode:
  # Override user's personal settings for THIS PROJECT ONLY
  # Example: Force plan mode for all technical specs in a critical project

  # /sdd.fix - Enabled by default for complex bugs
  fix_complex_bugs: true          # DESIGN_FLAW, FEATURE_GAP, multi-component errors

  # /sdd.spec technical - Enabled for brownfield + technical users
  # Note: Automatically disabled for non-technical users regardless of this setting
  spec_technical_brownfield: true # Explore code/specs before architecture decisions

  # /sdd.build - All opt-in (disabled by default)
  # Most users want uninterrupted implementation flow
  build_complex_tasks: false      # High complexity, >5 files, Layer 2, many acceptance criteria
  build_layer_transitions: false  # L1→L2 transitions, context >50%, 10+ tasks in next layer
  build_test_recovery: false # Ambiguous fury test failures, 2+ failed fix attempts
```

**Platform availability**:
- Claude Code CLI: Full Plan Mode support
- Cursor: Fallback to AskUserQuestion flow

**User profile vs PROJECT.md**:
| Setting Location | Scope | When to use |
|------------------|-------|-------------|
| ~/.sdd-kit/user-profile.yaml | All projects | Personal preference |
| PROJECT.md | This project | Team/project-specific override |

**Default behavior by command**:
| Command | Default | Why |
|---------|---------|-----|
| /sdd.fix | Enabled | Complex bugs benefit from planned investigation |
| /sdd.spec technical | Enabled (brownfield + technical) | Prevents architecture rework |
| /sdd.build | Disabled | Most want uninterrupted implementation |

-->

---

## Platform Configuration

<!--
Define the platform type for this project. This controls which agents and workflows
are used during /sdd.spec technical and /sdd.build.

Uncomment and set:
```yaml
platform:
  type: frontend-web   # backend (default) | frontend-web
```

- `backend` (default):  services — Java, Go, Python, Node.js BFF
- `frontend-web`: Nordic + Andes web apps — delegates to meli-frontend-web-* agents
-->

---

## Branching Strategy ( Standard — not configurable)

<!--
⚠️ AI INSTRUCTION: This section is a MANDATORY reference. When generating PROJECT.md,
you MUST include ALL branch types listed below — never omit entries or show only a sample.
These are defined by 's release-process and are not configurable at project level.
See: meli_sdd_kit/framework/standards/coding-standards.md#git-standards-fury-gitflow
-->

### Gitflow (applications)

| Branch | Purpose | Protected | Releasable | PR target |
|--------|---------|-----------|------------|-----------|
| `master` | Production branch | Yes | Yes | — |
| `develop` | Integration/development branch | Yes | No (test/beta only) | — |
| `feature/*` | New functionality | No | No | `develop` |
| `enhancement/*` | Technical improvements to existing features | No | No | `develop` |
| `fix/*` | Bug fixes | No | No | `develop` or `release/*` |
| `bugfix/*` | Alias for `fix/*` | No | No | `develop` |
| `hotfix/*` | Emergency production fix | No | Yes | `master` |
| `release/*` | Release preparation and freeze | No | No | `master` |
| `migration/*` | Database structural changes (DDL) | No | No | `master` |
| `revert-*` | Revert previous changes | No | No | `master` or `develop` |
| `backport/master_*` | Auto-propagates master changes to develop and open releases | No | No | `develop` and `release/*` (automatic) |

### Libflow (libraries)

| Branch | Purpose | Protected | Releasable | PR target |
|--------|---------|-----------|------------|-----------|
| `master` | Latest library version | Yes | Yes | — |
| `vN.x.x` | Stable major version (e.g., `v1.x.x`) | Yes | Yes | — |
| `feature/*` | New functionality | No | No | `master` or `vN.x.x` |
| `enhancement/*` | Technical improvements | No | No | `master` or `vN.x.x` |
| `fix/*` | Bug fixes | No | No | `master` or `vN.x.x` |
| `revert-*` | Revert previous changes | No | No | `master` or `vN.x.x` |


---

## Team Conventions

```yaml
language:
  specs: en           # en | es | pt - Language for writing specs
  comments: en        # Language for code comments
```

<!-- Uncomment to customize:

```yaml
naming:
  feature_prefix: ""           # Prefix for features (e.g., "payment-")
  branch_pattern: "feature/{name}"  # Git branch pattern

communication:
  slack_channel: null          # Team Slack channel
  oncall_rotation: null        # Oncall rotation link
  documentation_url: null      # Team documentation URL
```

-->

---

## Technology Preferences

<!-- Uncomment to customize:

```yaml
preferences:
  # Java preferences
  orm: jpa                     # jpa | hibernate | mybatis
  testing_framework: junit5    # junit5 | testng
  logging: slf4j               # slf4j | log4j2
  http_client: restclient      # restclient | webclient | feign

  # Node.js preferences
  package_manager: npm         # npm | yarn | pnpm
  test_runner: jest            # jest | mocha | vitest

  # Go preferences
  web_framework: gin           # gin | echo | fiber

forbidden:
  # Libraries NOT allowed in this project
  # Example: - lombok
  # Example: - mapstruct
```

-->

---

## Quality Gates

<!-- Uncomment to customize:

```yaml
coverage:
  min_coverage: 80             # Minimum test coverage percentage
  critical_paths_only: false   # true = only test critical paths

reviews:
  code_review: mandatory       # mandatory | optional
  spec_approval: mandatory     # mandatory | optional

  # Specialized reviews (list of change types that trigger review)
  dba_review_for:
    - schema_changes
    - migrations

  security_review_for:
    - auth
    - payments
    - pii
```

-->

---

## Frontend Web Configuration (Nordic/Andes)

<!--
Set automatically by /sdd.project when Nordic is detected.
andes_version and nordic_version are detected from package.json — modify only if incorrect.

Import format by version:
  - Andes 9: import { Button } from '@andes/button';
  - Andes X:   import { Button } from '@andes/react';
-->

<!-- Uncomment if auto-detection was incorrect:

```yaml
frontend:
  andes_version: "9"   # "9" | "X"
  nordic_version: "9"  # "9"
```

-->

---

## Default Feature Settings

<!-- Uncomment to set defaults for new features (meta.md):

```yaml
defaults:
  project_type: production     # prototype | mvp | production
  ltp_enabled: false           # true | false - E2E testing with LTP
  atlassian_mcp_enabled: false # true = Enable Jira/Confluence auto-fetch via AtlassianMCP
  execution_strategy: sequential  # sequential | batched | parallel
  user_profile: non-technical  # technical | non-technical - See below
```

> **User Profile Settings**:
> - `technical`: Full control - see layers, complexity, services, code snippets, architecture details
> - `non-technical`: Business focus - agent handles technical decisions, simplified UI, no jargon

-->

---

## Team Info

<!-- Uncomment to add team information:

```yaml
team:
  name: null                   # Team name
  email: null                  # Team email
  manager: null                # Team manager
  tech_lead: null              # Technical lead
```

-->

---

## Registered Overrides

<!--
This section is automatically updated when /sdd.check --project detects
conflicts between your settings and framework standards.

Each override documents WHY your project deviates from a standard.

Example:
```yaml
overrides:
  - standard: testing-strategy.md
    rule: "Minimum coverage 80%"
    project_value: 50
    reason: "Legacy codebase, incrementally improving coverage"
    registered_at: 2025-12-30
    registered_by: username
```
-->

```yaml
overrides: []
```

---

## Hub members

<!--
⚠️ CONDITIONAL SECTION — Suite projects only.
If this is a single-app project, DELETE THIS ENTIRE SECTION.

This section declares the apps that belong to this suite.
The framework resolves member locations exclusively from the Path column below.
Organize your folder structure however you prefer — the framework does not enforce it.

All members are apps (leaves). Sub-suites are not supported.

| Column   | Description                                           |
|----------|-------------------------------------------------------|
| Member   | Short name used in specs, tasks, and CLI output       |
| Path     | Relative path from suite root to the app directory    |
| Git URL  | Clone URL for the app repo (informational)            |
| Stack    | Primary technology (Java, Go, Node, React, etc.)      |
| Summary  | One-line description of what the app does             |

Example:

| Member | Path | Git URL | Stack | Summary |
|--------|------|---------|-------|---------|
| campaign-api | fury-apps/backend/campaign-api | git@github.com:your-org/ads-campaign-api.git | Java | Manages campaign lifecycle. Exposes POST/GET /campaigns. |
| campaign-web | fury-apps/frontend/campaign-web | git@github.com:your-org/ads-campaign-web.git | React | Campaign management UI. MFE remote. |

-->

<!-- Uncomment and fill in your members:

| Member | Path | Git URL | Stack | Summary |
|--------|------|---------|-------|---------|
| app-name | path/to/app | git@github.com:your-org/app-name.git | Java | Brief description of the app. |

-->
