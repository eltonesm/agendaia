---
name: sdd.hub
description: Orchestrate multi-app hub features across apps. Coordinates specs, planning, and build across member apps. Use when working in a hub repo with multiple collaborating apps.
argument-hint: "<action> [args]"
---

# Command: /sdd.hub

**Purpose**: Orchestrate features across multiple apps in a hub workspace.

> A hub is a repo that coordinates multiple apps (defined in `sdd/PROJECT.md` via `## Hub members`).
> Each app has its own `sdd/` directory and works with standard `/sdd.*` commands.
> This skill adds the cross-app coordination layer on top.

---

## Sub-command Routing

Parse the user's input and route to the correct reference file.

| Input | Action |
|-------|--------|
| `/sdd.hub start <name>` | Read `references/hub-start.md` and follow it |
| `/sdd.hub spec functional [flags]` | Read `references/hub-spec.md` and follow the **functional** section |
| `/sdd.hub spec technical [flags]` | Read `references/hub-spec.md` and follow the **technical** section |
| `/sdd.hub plan [flags]` | Read `references/hub-plan.md` and follow it |
| `/sdd.hub build [flags]` | Read `references/hub-build.md` and follow it |
| `/sdd.hub check [flags]` | Read `references/hub-check.md` and follow it |
| `/sdd.hub finish [flags]` | Read `references/hub-finish.md` and follow it |
| `/sdd.hub list` | Read `references/hub-list.md` and follow it |
| `/sdd.hub cancel` | Read `references/hub-cancel.md` and follow it |
| `/sdd.hub go "description"` | Read `references/hub-go.md` and follow it |
| `/sdd.hub sync [--pull]` | Read `references/hub-sync.md` and follow it |
| `/sdd.hub add <app-name>` | Read `references/hub-add.md` and follow it |
| `/sdd.hub` (no action) | Show help below |

### Flag parsing

When the user provides flags, parse them and pass to the reference file handler. Supported flags per sub-command:

| Sub-command | Supported flags |
|-------------|----------------|
| `spec` | `--approve`, `--summary`, `--audio`, `--iterate "desc"` |
| `plan` | `--approve` |
| `build` | `--resume` |
| `check` | `--sync` |
| `finish` | `--force` |
| `sync` | `--pull`, `--members name1,name2` |

---

## Help (no sub-command)

If the user runs `/sdd.hub` without an action, show:

```
Hub Orchestrator — Multi-app feature coordination

Usage: /sdd.hub <action> [args] [flags]

Actions:
  start <name>      Initialize a cross-app feature
  spec functional    Functional spec (cross-app user stories)
  spec technical     Technical spec (architecture + per-app scope)
  plan               Export child specs and generate tasks per app
  build              Implement across apps respecting dependency layers
  check              Drift detection and status per app
  list               Show hub members and feature status
  finish             Archive feature across hub and all apps
  cancel             Cancel feature and clean up child specs
  go "description"   Express mode — full hub workflow in one command
  sync               Verify git status of member repos (branch, dirty, behind)
  add <app-name>     Add a app to the hub members table in PROJECT.md

Flags:
  spec --approve     Auto-approve spec without prompting
  spec --summary     Show quick overview of existing spec
  spec --audio       Record voice description as seed
  spec --iterate     Refine existing spec with changes
  plan --approve     Auto-approve plan without prompting
  build --resume     Resume from last incomplete member
  check --sync       Run cross-layer consistency per app
  finish --force     Skip completion checks
  sync --pull        Auto-pull behind members

Prerequisite: sdd/PROJECT.md must contain a ## Hub members table.
```

---

## Hub Detection Guard

Before executing ANY sub-command, verify this is a hub:

1. Run: `Bash: bash sdd-kit/framework/tools/detection/detect-stack.sh --level`
2. If result is NOT `hub`, stop and show:
   > "This directory is not a hub. A hub requires `sdd/PROJECT.md` with a `## Hub members` table.
   > For single-app features, use `/sdd.start`, `/sdd.spec`, `/sdd.plan`, `/sdd.build`."

---

## Skill Hook Integration

All sub-commands invoke hooks via `sdd.skill` at defined trigger points.
Hub phases are:

| Phase | Triggers |
|-------|----------|
| `hub-start` | `before-start` |
| `hub-spec-functional` | `before-start`, `before-approval` |
| `hub-spec-technical` | `before-start`, `before-approval` |
| `hub-plan` | `before-start`, `before-approval` |
| `hub-build` | `before-start`, `after-implementation` |
| `hub-finish` | `before-start`, `after-implementation` |

To invoke hooks, check if any skills are connected to the phase:
```bash
# Read skill-hooks.json (repo or user layer)
# If a hook exists for the phase+trigger, invoke that skill
```

---

## Key Principles

1. **Standard kit compatibility**: Child specs exported to apps are fully compatible with `/sdd.plan` and `/sdd.build`.
2. **Specs are source of truth**: The hub tech spec owns cross-app decisions. Child specs reference it.
3. **No sub-hubs**: Hub -> apps is flat. All members are leaves.
4. **Structure is free**: The framework resolves member locations from the `Path` column in the members table.
5. **Git is not managed**: The kit does not clone/pull repos. The user manages that.
