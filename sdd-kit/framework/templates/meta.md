# Feature Metadata

**Feature Name**: {{FEATURE_NAME}}
**Feature ID**: feat-{{FEATURE_DATE}}-{{FEATURE_NAME}}
**Mode**: greenfield | brownfield
**Project Type**: {{PROJECT_TYPE}}
**Platform**: {{PLATFORM}}
**User Profile**: {{USER_PROFILE}}
**Created**: {{DATE}}
**Last Updated**: {{DATE}}
**Current Stage**: {{STAGE}}

---

## Framework Version

```yaml
framework:
  version_created: "{{FRAMEWORK_VERSION}}"  # Version when feature was created
  version_current: null                      # Updated each time /sdd.check runs
  last_compatibility_check: null             # ISO-8601 timestamp
  migration_notes: []                        # Any migration actions taken
```

> **Note**: This field tracks which framework version was used when creating the feature.
> If you upgrade the framework mid-feature, `/sdd.check` will detect version drift and
> suggest any migration actions needed for compatibility.

---

## Project Type Configuration

```yaml
project_type:
  type: null  # prototype | mvp | production
  decision_date: null

  # Testing configuration based on project type
  testing:
    unit_tests: null      # disabled | critical_only | full_coverage
    ltp_enabled: null     # false | false | opt-in
    coverage_target: null # 0% | varies | 80%
```

> **Note**: Project type is selected during `/sdd.start` and affects test generation in `/sdd.build`:
> - **prototype**: No unit tests, no LTP (focus on speed)
> - **mvp**: Unit tests for critical paths only, no LTP by default
> - **production**: Full test coverage (80%+), LTP opt-in

---

## User Profile Configuration

```yaml
user_profile:
  type: null  # technical | non-technical
  source: null  # global | project | feature | selected
  selected_at: null  # ISO-8601 timestamp
```

> **Note**: User profile affects how information is displayed and what decisions are automated:
> - **technical**: Full control - see layers, complexity ratings,  services, code snippets
> - **non-technical**: Business focus - simplified display, agent handles technical decisions automatically
>
> **Source hierarchy** (highest to lowest priority):
> 1. `feature` - Override in this feature's meta.md
> 2. `project` - Default from PROJECT.md
> 3. `global` - User preference in `~/.sdd-kit/user-profile.yaml`
> 4. `selected` - First-time selection during `/sdd.start`

---

## Spec Language

```yaml
spec_language: en  # en | es | pt - Inherited from PROJECT.md language.specs during /sdd.start
```

> **Note**: This field determines the language for all specification documents in this feature.
> Inherited from `PROJECT.md` `language.specs` during `/sdd.start`. Can be overridden per-feature.
> This is independent of the agent's response language (which follows the user's conversation language).

---

## LTP Configuration

```yaml
ltp:
  enabled: null           # true | false - Set during /sdd.spec functional
  decision_date: null     # ISO-8601 timestamp when decision was made
  decision_reason: null   # Optional: why user chose yes/no
```

> **Note**: This field is set during `/sdd.spec` functional phase when the user answers the LTP E2E Testing question. If `enabled: false`, the E2E Scenarios section is skipped and AUTO-TASK-E2E is not generated.

---

## Database Migrations

```yaml
migration:
  detected: false                  # true | false - Set during /sdd.spec technical (Step 5.5)
  service_name: null               #  DB service name from technical spec
  service_type: null               # mysql | postgresql
  branch_name: null                # Set by /sdd.build after creation (e.g., migration/feat-name)
  branch_status: null              # pending | created | pushed | applied
  migration_files: []              # Populated by /sdd.build with created .sql file paths
```

> **Note**: This field is set during `/sdd.spec` technical phase (Step 5.5) when database migrations are detected. If `detected: false`, `/sdd.build` Step 3.5 is skipped entirely. If `detected: true`, `/sdd.build` will orchestrate a `migration/*` branch from master before proceeding with application code.

---

## Scaffolding Context

<!--
⚠️ CONDITIONAL SECTION:
- If app was created via  or scaffolded interactively → Keep and fill in
- If existing repo (no scaffolding) → DELETE THIS SECTION
-->

```yaml
scaffolding:
  created_via: null         #  | fury-get-interactive | none
  technology: null          # java | go | python | node
  platform: null            # backend | web | android | ios | (auto-detect via detect-stack.sh)
  scaffolding_template: null # spring-maven | spring-gradle | fastapi | express | etc.
  scaffolded_at: null       # ISO-8601 timestamp
  cleanup_performed: false  # true after example code removed

  essential_files_verified:
    dockerfile: false
    dockerfile_runtime: false
    file: false
    dependency_file: false  # pom.xml | go.mod | pyproject.toml | package.json
    entry_point: false      # Application.java | main.go | __main__.py | index.ts
    ping_endpoint: false    # PingController | /ping handler
```

> **Note**: This section tracks  scaffolding performed during `/sdd.start`. Scaffolded projects remain "greenfield" because they have infrastructure but no implementation code.

---

## Backlog Workflow

<!--
⚠️ CONDITIONAL SECTION:
- If feature was created via /sdd.backlog pick → Keep and fill in
- If feature was NOT created from backlog → DELETE THIS SECTION
-->

```yaml
from_backlog: null              # Backlog item ID (e.g., TODO-001, DEBT-003)
workflow_mode: full             # full | technical-only | tasks-only
auto_generated:
  functional: false             # true if functional spec was auto-generated
  technical: false              # true if technical spec was auto-generated
```

> **Note**: Workflow modes apply only to DEBT and TODO items picked from the backlog:
> - **full**: Standard pipeline (functional interview → technical interview → plan → build)
> - **technical-only**: Auto-generates minimal functional spec, user does technical interview
> - **tasks-only**: Auto-generates both specs, user only approves tasks
>
> IDEA items always use `full` mode (they need functional discovery).
> Auto-generated specs are marked `approved_by: auto-generated` in the stage history.

---

## Brownfield Context

<!--
⚠️ CONDITIONAL SECTION:
- If Mode = greenfield → DELETE THIS ENTIRE SECTION (from "## Brownfield Context" to the next "---")
- If Mode = brownfield → Keep and fill in the values below
-->

**Affected System Specs**:
```yaml
affected_specs:
  - path: sdd/specs/architecture.md
    sections: [Component X, Integration Y]
  - path: sdd/specs/api-contracts/users.yaml
    endpoints: [GET /users, POST /users]
  - path: sdd/specs/components.md
    components: [UserService, AuthModule]
```

**Impact Assessment**:
```yaml
impact:
  level: Critical | High | Medium | Low
  breaking_changes: true | false
  requires_migration: true | false
  affected_consumers: []  # List of downstream services/clients
```

**Note**: In brownfield mode, the functional and technical specs describe the CHANGES (delta) to the existing system. The specs themselves serve as the delta documentation.

---

## Hub Scope

<!--
⚠️ CONDITIONAL SECTION — Hub features only.
- If scope = app (normal single-app feature) → DELETE THIS ENTIRE SECTION
- If scope = hub → Keep and fill in
-->

```yaml
scope: hub
target_members:
  - name: "{{MEMBER_NAME}}"
    path: "{{MEMBER_PATH}}"
    status: "pending"   # pending | in-progress | complete
```

---

## Hub Origin

<!--
⚠️ CONDITIONAL SECTION — Child specs in app repos only.
- If this feature was NOT exported from a hub → DELETE THIS ENTIRE SECTION
- If this is a child spec created by /sdd.hub plan → Keep and fill in
-->

```yaml
hub_origin:
  parent_repo: "{{PARENT_REPO}}"
  parent_feature: "{{PARENT_FEATURE}}"
  parent_spec: "sdd/wip/{{PARENT_FEATURE}}/2-technical/spec.md"
  parent_hash: "{{HASH}}"
  exported: "{{DATE}}"
```

> **Note**: `parent_hash` is the SHA-256 of the `## {member}` section in the hub tech spec at export time.
> `/sdd.hub check` uses this to detect drift between the hub spec and child specs.

---

## Team

**Owner**: [Name/Email]
**Team Members**: [Names/Emails]

---

## Stage History

```yaml
stages:
  functional:
    started: null
    completed: null
    status: pending | in-progress | approved
    owner: null
    approved_by: null        # Username/email of person who approved
    approved_at: null        # ISO-8601 timestamp of approval
    iterations: 0

  technical:
    started: null
    completed: null
    status: pending | in-progress | approved
    owner: null
    approved_by: null        # Username/email of person who approved
    approved_at: null        # ISO-8601 timestamp of approval
    mcpqueried: false
    services_count: 0

  tasks:
    started: null
    completed: null
    status: pending | in-progress | approved
    approved_by: null        # Username/email of person who approved
    approved_at: null        # ISO-8601 timestamp of approval
    strategy_chosen_by: null # Username/email of person who chose execution strategy
    generated_tasks_count: 0
    iterations: 0
    final_tasks_count: 0

  implementation:
    started: null
    completed: null
    status: pending | in-progress | completed
    execution_strategy: null
    total_tasks: 0
    completed_tasks: 0
```

> **Note**: The `approved_by` field should capture the logged-in user (e.g., from `$USER`, `git config user.name`, or explicit input) when running `--approve` commands.

---

## Execution Strategy

```yaml
execution_strategy:
  type: null  # sequential | parallel | batched
  chosen_date: null
  estimated_agent_time: null  # e.g., "24h" for sequential, "14h" for parallel
  estimated_tokens: null      # e.g., 80000 for sequential, 140000 for parallel
  actual_agent_time: null
  rationale: null  # e.g., "Balanced approach - 25% faster, reasonable token usage"

  # If batched
  phases:
    - name: null
      duration: null
      parallel: false
      tasks: []

    # If parallel tracks within phase
      tracks:
        - name: null
          tasks: []
```

---

## Metrics

```yaml
metrics:
  timeline:
    estimated_days: null
    actual_days: null
    variance_percent: null

  effort:
    estimated_hours: null
    actual_hours: null
    variance_percent: null

  quality:
    test_coverage: null
    tests_total: null
    tests_passing: null
    linter_errors: 0
    type_errors: 0

  velocity:
    avg_hours_per_task: null
    estimation_accuracy: null
```

---

## Changes and Deviations

```yaml
changes:
  tasks_added: []
  # - task_id: TASK-018
  #   reason: "Ops requirement discovered"
  #   added_date: 2025-11-XX

  tasks_removed: []
  tasks_modified: []

  spec_changes:
    functional: []
    technical: []

  risks_materialized: []
  # - risk: "Performance degradation"
  #   impact: "Medium"
  #   mitigation: "Added database index"
  #   date: 2025-11-XX
```

---

## Validation Overrides

```yaml
overrides:
  # Record any --force approvals
  functional:
    forced: false
    reason: null
    date: null

  technical:
    forced: false
    reason: null
    date: null

  tasks:
    forced: false
    reason: null
    date: null

  complete:
    forced: false
    reason: null
    date: null
```

---

## Notes

[Any additional notes, learnings, or context for future reference]
