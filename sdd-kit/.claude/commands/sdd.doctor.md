---
name: sdd.doctor
description: Diagnose whether the project's configuration lets the SDD kit do its job. Detects external instructions that contradict, duplicate, or compete with the kit (CLAUDE.md, AGENTS.md, .claude/*, sdd/PROJECT.md, sdd/PATTERNS.md). Use when specs come out bloated, when the agent ignores kit directives, or after major edits to instruction files.
model: sonnet
argument-hint: "[--apply] [--scope all|kit|claude] [--strict] [--deep] [--heuristic-only] [--json] [--explain <id>]"
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

# Command: /sdd.doctor

**Description**: Diagnose project configuration health for SDD kit usage

**Usage**:
- `/sdd.doctor` → Scan + reporte (read-only). Heurística + análisis semántico en archivos always-on.
- `/sdd.doctor --apply` → Para cada issue fixable, pide y/N.
- `/sdd.doctor --heuristic-only` → Solo etapa 1 (determinista). Modo CI.
- `/sdd.doctor --deep` → Análisis semántico en TODOS los archivos en scope.
- `/sdd.doctor --explain <id>` → Razonamiento completo de un finding semántico.
- `/sdd.doctor --scope kit|claude|all` → Filtra archivos analizados.
- `/sdd.doctor --strict` → Umbrales más bajos (CI/pre-merge).
- `/sdd.doctor --json` → Output estructurado, exit code por severidad.

---

## Quick Help

> `/sdd.doctor help` → Shows this summary

**Syntax**: `/sdd.doctor [flags]`

| Flag | Description |
|------|-------------|
| (none) | Heurística + semántico en always-on. Reporte read-only. |
| `--apply` | Pide y/N por issue fixable. |
| `--heuristic-only` | Solo etapa 1 (determinista). |
| `--deep` | Semántico extendido a todos los archivos en scope. |
| `--explain <id>` | Muestra el razonamiento del finding. |
| `--scope kit\|claude\|all` | Filtra qué se analiza. |
| `--strict` | Umbrales más bajos (CI). |
| `--json` | Output estructurado. |

**Examples**:
```bash
/sdd.doctor                       # Diagnóstico completo
/sdd.doctor --apply               # Revisar y aplicar fixes
/sdd.doctor --heuristic-only      # Modo determinista (CI)
/sdd.doctor --explain X2          # Ver razonamiento de un issue
```

**See also**: `/sdd.help doctor` for detailed documentation.

---

CRITICAL: USER INTERACTION RULES
When this skill shows JSON for AskUserQuestion, you MUST:
  1. CALL the AskUserQuestion TOOL with that exact JSON
  2. DO NOT print options using Bash (no echo, cat, printf)
  3. DO NOT ask "Which option?" as text
  4. Tables marked "REFERENCE ONLY" are for docs - do NOT print

---

## Purpose

`/sdd.doctor` valida que la **configuración del proyecto deje al SDD kit hacer su trabajo**. No es un limpiador estético: es un check de salud que pregunta tres cosas por cada regla externa encontrada:

1. **¿Contradice al kit?** → ERROR. El kit gana; la regla externa debe alinearse o removerse.
2. **¿Duplica al kit?** → WARN. Removable: el kit ya lo provee.
3. **¿Le roba contexto al kit?** → WARN. Mover a `docs/` y referenciar, o consolidar en `sdd/PROJECT.md`.

**Tesis**: el contexto del agente debe gastarse en el kit y en lo específico del proyecto, no en re-instruir lo que el kit ya define. Cuando devs reportan specs de 1000+ líneas, la causa raíz suele estar en `CLAUDE.md`/`AGENTS.md` con reglas que sobreescriben el principio de elegancia (`standards/elegance-principle.md`).

`/sdd.doctor` opera **solo** sobre archivos de instrucción al agente (CLAUDE.md, AGENTS.md, .claude/*, sdd/PROJECT.md, sdd/PATTERNS.md). No mira specs WIP ni features (eso es `/sdd.check`).

---

## Files in Scope

| File | Why it matters |
|------|----------------|
| `CLAUDE.md` (root) | Always-on instructions; biggest source of context theft and kit conflict |
| `AGENTS.md` (root) | Security/agent rules; can disable kit behaviors |
| `.claude/CLAUDE.md` | Always-on (Claude Code IDE config) |
| `.claude/commands/*.md` | Custom commands; can shadow `/sdd.*` |
| `.claude/agents/*.md` | Custom agents; can shadow `meli-*` |
| `.claude/settings.json` | Hooks, permissions; can block kit operations |
| `sdd/PROJECT.md` | Project overrides; checked for non-kit-specific content |
| `sdd/PATTERNS.md` | Accumulated learnings; checked for size and empty sections |

`--scope kit` → solo `sdd/PROJECT.md`, `sdd/PATTERNS.md`.
`--scope claude` → solo `CLAUDE.md`, `AGENTS.md`, `.claude/*`.

---

## Execution Flow

```
┌──────────────────────────────────────────────────────────────────────┐
│                            /sdd.doctor                              │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. RUN HEURISTIC SCAN (deterministic, always)                       │
│     bash sdd-kit/framework/tools/doctor/scan-config.sh . --json        │
│     bash sdd-kit/framework/tools/doctor/detect-duplication.sh . --json │
│                                                                      │
│  2. RUN SEMANTIC ANALYSIS (default ON, skip with --heuristic-only)   │
│     Load kit comparators (standards/, templates/, skills frontmatter)│
│     For each in-scope file that is always-on, >30 lines, or already  │
│     flagged: prompt LLM with the template in semantic-prompt.md      │
│                                                                      │
│  3. MERGE & DEDUPLICATE                                              │
│     Combine heuristic + semantic; drop dups on (file, line, axis)    │
│     Apply severity floor (semantic without kit_reference → INFO)     │
│                                                                      │
│  4. RENDER REPORT                                                    │
│     Format per output-examples.md; tag each issue (heuristic|semantic)│
│                                                                      │
│  5. (--apply) ITERATE FIXABLE ISSUES                                 │
│     For each fixable: show recipe, ask y/N, apply or skip            │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Step 1 — Heuristic Scan (Deterministic)

> **MUST run first.** Even when `--deep` or semantic are active. Heuristic gives the deterministic floor.

```bash
# Scan: files, footprints, phrase hits, shadowed commands/agents, empty sections
scan_result=$(bash sdd-kit/framework/tools/doctor/scan-config.sh . --json $([ "$strict" = true ] && echo "--strict"))

# Duplication: shingle overlap across pairs of always-on files
dup_result=$(bash sdd-kit/framework/tools/doctor/detect-duplication.sh . --json)
```

The scanner outputs structured JSON with:
- `files.*` — path, exists, line counts per file in scope
- `footprint.always_on_lines` and thresholds
- `shadowed_commands[]`, `shadowed_agents[]`
- `phrase_hits[]` — one per regex match from `phrases.json`
- `issues[]` — pre-built findings (one per phrase hit or threshold breach)

Each pre-built issue uses the shape: `{rule, file, line, evidence, severity, fixable}`. The skill resolves `rule → axis / kit_reference / recipe` by re-reading `phrases.json` and the rule catalog at `references/rules.md`.

**D1/D2** findings come from `detect-duplication.sh.pairs[]` — for any pair above the threshold the skill creates a WARN issue tagged `D2` with both file:line ranges.

---

## Step 2 — Semantic Analysis (LLM-driven, default ON)

> **Skip when**: `--heuristic-only` is passed, OR the scope has no in-scope files matching the entry criteria.

### Entry criteria for each file

A file is sent to semantic analysis when **at least one** of these holds:
- Path is always-on (`CLAUDE.md`, `AGENTS.md`, `.claude/CLAUDE.md`)
- File has > 30 lines (smaller files rarely contain semantic conflicts)
- File already has at least one heuristic finding
- `--deep` flag is active (forces all in-scope files)

### Comparator (kit context loaded once per run)

Read these once and keep in context for ALL semantic prompts:
- `sdd-kit/framework/standards/elegance-principle.md`
- `sdd-kit/framework/standards/coding-standards.md`
- `sdd-kit/framework/standards/testing-strategy.md`
- `sdd-kit/framework/standards/fury-guidelines.md` (skip if missing)
- `sdd-kit/framework/standards/security*.md` (glob; whichever exist)
- Skill frontmatter from `sdd-kit/framework/skills/sdd.*/SKILL.md` (purpose lines only)
- `sdd-kit/framework/templates/functional-spec.md`, `technical-spec.md`, `project.md`, `PATTERNS.md`

### Prompt template

Use the template at `references/semantic-prompt.md`. Key invariants:
- **Evidence required**: each finding must quote the exact line(s) from the user's file and cite which kit document it conflicts with.
- **Severity floor**: if no `kit_reference` is provided, severity drops to INFO automatically.
- **Bias toward false negatives**: when in doubt, do not report.
- **Output format**: strict JSON conforming to the issue shape (rule starts with `SEM-`).

### Cost & parallelism

- Default: ~3-5 files × 1 sonnet call each. Total ≤ 30s.
- For large projects (many `.claude/commands/`), if combined comparator + user files exceeds ~30k tokens, delegate semantic analysis to the `general-purpose` subagent so its context stays isolated. Pattern is the same as `/sdd.check --sync` delegating to `sdd-layer-analyzer`.

---

## Step 3 — Merge, Dedupe, Render

1. Concatenate heuristic + semantic issues into one list.
2. Dedupe on tuple `(file, line, axis)` — keep heuristic if both report the same spot.
3. Sort by severity (ERROR → WARN → INFO), then by file.
4. Tag each issue with its source. Semantic issues are printed as `[ID] (semantic)`.
5. Render using the layout in `references/output-examples.md`.

Exit codes (when `--json`):
- `0` — no issues OR only INFO
- `1` — WARN present (no ERROR)
- `2` — at least one ERROR

---

## Step 4 — Apply Mode (`--apply`)

When `--apply` is passed:

1. After printing the report, list the **fixable** issues (`fixable: true`).
2. For each fixable issue, present a one-screen block:

   ```
   ▸ [O1] Custom command shadows /sdd.spec
     File:    .claude/commands/spec.md
     Recipe:  mv .claude/commands/spec.md .claude/commands/team-spec.md

     Apply this fix?
   ```

3. Use `AskUserQuestion` with options: `Apply` / `Skip` / `Skip the rest`.

4. Apply only after explicit y. Never batch.

5. After all fixable issues are processed, print a final summary:

   ```
   Applied: 2 fixes (O1, K2)
   Skipped: 1 fix (D3)
   Not auto-fixable: 4 issues — see report for recipes.
   ```

6. The non-fixable issues are **never** modified. The recipe is shown in the report; the dev decides.

---

## Step 5 — Explain Mode (`--explain <id>`)

When the user passes `--explain X2` (or any semantic issue id):

1. Re-read the file and the cited `kit_reference`.
2. Print:
   - The exact quote from the user file (with surrounding 2-3 lines of context).
   - The exact quote from the kit reference.
   - The axis (`contradicts` / `duplicates` / `steals_context`).
   - The reasoning chain — why these two pieces of text conflict.
   - The recipe with concrete alternatives.

This makes every semantic finding falsifiable. If the user disagrees, they can ignore it.

---

## Integration with `/sdd.start`

`/sdd.start` runs `scan-config.sh` after PROJECT.md validation and prints a non-blocking one-line tip if certain heuristics trip. **This is owned by `/sdd.start`, not by `/sdd.doctor`.** The doctor is opt-in.

---

## Output Format

See `references/output-examples.md` for the canonical layouts. The default report has:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🩺 sdd.doctor — Kit Health Check
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Project: <name>
Always-on footprint: N lines  ✓/⚠️  (target: <400)
Issues: K (E errors, W warnings, I info)

❌ ERROR [<id>] (heuristic|semantic)  <title>
  Location:    <file>:<line>
  Evidence:    "<quote>"
  Conflicts:   <kit_reference>
  Effect:      <one-sentence consequence>
  Recipe:      <how to fix>
  [fixable] / [run /sdd.doctor --explain <id>]

…

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Source breakdown: H heuristic · S semantic
What to do next:
  /sdd.doctor --apply             (when fixable issues exist)
  /sdd.doctor --explain <id>      (for semantic findings)
```

If `--json` is set, emit a structured object instead:

```json
{
  "version": "1.0",
  "footprint": { "always_on_lines": 1247, "threshold": 400 },
  "issues": [
    {
      "id": "X1",
      "source": "heuristic",
      "axis": "contradicts",
      "severity": "error",
      "file": "CLAUDE.md",
      "line": 42,
      "evidence": "Always be exhaustive in specs...",
      "kit_reference": "standards/elegance-principle.md",
      "recipe": "Remove this line. ...",
      "fixable": false
    }
  ],
  "summary": { "errors": 2, "warnings": 3, "info": 1 }
}
```

---

## AI Agent Instructions

### Help Flag Detection

**WHEN** the user runs `/sdd.doctor help`:
1. Print only the "Quick Help" section (~15 lines).
2. Do NOT execute a scan.
3. Stop.

### Key Behaviors

1. **No arguments**: full default flow (heuristic + semantic on always-on).
2. **`--apply`**: run default flow, then enter the apply loop (Step 4).
3. **`--heuristic-only`**: skip Step 2 entirely.
4. **`--deep`**: same as default but Step 2 entry criteria includes all in-scope files.
5. **`--explain <id>`**: skip Steps 1-3; jump directly to Step 5 with the cached run if available, or run heuristic again and locate the issue.
6. **`--json`**: print only the JSON object, no human-readable wrappers.
7. **`--strict`**: pass through to `scan-config.sh --strict`. Lowers thresholds.

### Critical Rules

- **Always tag the source** of each issue in human output (`(semantic)` suffix for semantic; nothing extra for heuristic).
- **Never auto-apply** without user confirmation, even in `--apply` mode.
- **Never modify code or specs** — only the 8 config files in scope are eligible for fixable changes, and only for `O1`, `O2`, and `K2` rules.
- **Never invent kit references**. If the user file conflicts with something not in the loaded comparator, drop severity to INFO and explain that the rule looks unusual without claiming the kit forbids it.
- **Be transparent about cost** in `--deep`: print a one-line warning if the run will take > 60s (estimate from file count).

### Workflow Integration

When the user runs `/sdd.doctor` after a fresh `/sdd.start` that already showed a tip:
- Acknowledge: "Running diagnostic (you saw the tip during /sdd.start)."
- Proceed normally. Do not skip steps.

---

## Related Commands

- `/sdd.check --project` — Validates `PROJECT.md` schema (used internally by `/sdd.start`).
- `/sdd.check --sync` — Validates feature-level layer consistency.
- `/sdd.fix` — Fixes errors across all layers (specs/tasks/code).
- `/sdd.help` — Command reference.

---

## Why this skill exists

Devs reported specs of 1000+ lines saturated with over-explanation, violating the elegance principle. Investigation showed the root cause was almost always external config: `CLAUDE.md` directives telling the agent to be exhaustive, custom commands shadowing `/sdd.spec`, or architecture rules conflicting with `fury-guidelines.md`. `/sdd.check` validates features, not project config. `/sdd.doctor` fills that gap — it diagnoses whether the project's always-on instructions let the kit do its job.
