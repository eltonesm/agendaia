# Task Format Standard

**Version**: 1.2.2-beta
**Last Updated**: 2026-01-20
**Since**: v1.1.17

---

## Overview

Tasks are stored in a **single JSON file** per feature: `3-tasks/tasks.json`

> **IMPORTANT**: Prior to v1.1.17, tasks were stored in `tasks.md`. All references to `tasks.md` are deprecated.

---

## File Location

```
sdd/wip/[YYYYMMDD-feature-name]/
├── 1-functional/spec.md
├── 2-technical/spec.md
├── 3-tasks/
│   ├── tasks.json          ← Single source of truth
│   └── dependencies.md     ← Visual dependency graph (Mermaid)
└── 4-implementation/progress.md
```

---

## JSON Schema

```json
{
  "feature": "feature-name",
  "feature_number": "001",
  "version": "1.0",
  "generated_at": "2026-01-20T10:00:00Z",
  "execution_strategy": "batched",
  "tasks": [
    {
      "id": "TASK-001",
      "title": "Task title",
      "description": "What this task accomplishes",
      "priority": "High",
      "complexity": "Medium",
      "phase": 1,
      "depends_on": [],
      "status": "pending",
      "acceptance_criteria": [
        "Criterion 1",
        "Criterion 2"
      ],
      "files_affected": [
        "src/service/MyService.java"
      ],
      "tests_required": [
        "Unit test for MyService"
      ]
    }
  ],
  "summary": {
    "total": 15,
    "by_priority": { "Critical": 2, "High": 5, "Medium": 6, "Low": 2 },
    "by_complexity": { "High": 3, "Medium": 8, "Low": 4 },
    "by_status": { "pending": 15, "in_progress": 0, "completed": 0 }
  }
}
```

---

## Field Definitions

### Task Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | ✅ | Format: `TASK-NNN` (e.g., TASK-001) |
| `title` | string | ✅ | Short descriptive title |
| `description` | string | ✅ | What the task accomplishes |
| `priority` | enum | ✅ | `Critical`, `High`, `Medium`, `Low` |
| `complexity` | enum | ✅ | `High`, `Medium`, `Low` |
| `phase` | number | ✅ | Execution phase (1, 2, 3...) |
| `depends_on` | array | ✅ | List of task IDs this depends on |
| `status` | enum | ✅ | `pending`, `in_progress`, `completed`, `blocked` |
| `acceptance_criteria` | array | ✅ | List of criteria to mark as complete |
| `files_affected` | array | ⬚ | Files to create/modify |
| `tests_required` | array | ⬚ | Tests to write |

### Status Values

| Status | Description |
|--------|-------------|
| `pending` | Not started |
| `in_progress` | Currently being worked on |
| `completed` | Done and validated |
| `blocked` | Waiting on dependency or external factor |

---

## Commands That Use tasks.json

| Command | Action |
|---------|--------|
| `/sdd.plan` | **Creates** tasks.json from specs |
| `/sdd.build` | **Reads & updates** task status |
| `/sdd.check` | **Reads** for progress reporting |
| `/sdd.finish` | **Reads** to verify completion |
| `/sdd.rollback --task` | **Reads** to identify task commits |

---

## Agents That Use tasks.json

| Agent | Usage |
|-------|-------|
| `sdd-implementer` | Reads tasks during `/sdd.build` |
| `sdd-layer-analyzer` | Validates task-spec consistency |
| `genai-analyze-e2e.sh` | Detects E2E scenarios in tasks |
| `sdd-small-test-writer` | Reads test requirements |
| `genai-compact-state.sh` | Summarizes task state |

---

## Validation

Run task validation with:

```bash
bash ~/.sdd-kit/tools/validation/validate-tasks.sh sdd/wip/[feature]
```

**Checks performed**:
- All required fields present
- No circular dependencies
- Valid status values
- Acceptance criteria count (min 2 per task)
- Testing tasks exist (~20% of total)

---

## Migration from tasks.md (Deprecated)

If you have legacy `tasks.md` files:

1. The format was markdown with `#### TASK-NNN:` headers
2. Convert to JSON using the schema above
3. Delete the old `tasks.md` file
4. Run validation to verify

> **Note**: Some tools have backwards compatibility for `tasks.md` but this is deprecated and will be removed in v2.0.

---

## See Also

- [WORKFLOW.md](../WORKFLOW.md) - Overall workflow
- [sdd.plan SKILL.md](../skills/sdd.plan/SKILL.md) - Task generation command
- [sdd.build SKILL.md](../skills/sdd.build/SKILL.md) - Task execution command
