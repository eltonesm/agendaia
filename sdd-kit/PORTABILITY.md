# Portability Notes

> Ground truth about which parts of this kit actually pull weight, and which
> don't — from running the **full** cycle (`start → spec → plan → build →
> finish`) twice, end to end, on a real project (AgendaIA, Java/Spring Boot
> monolith, two features, 28 tasks, 254 tests). Not opinion — every verdict
> below was tested against the live installation, not read off a doc.
>
> **Read this before copying `sdd-kit/` into a new project.** It tells you
> what to expect on day one instead of rediscovering it by trial and error.

---

## The part that's actually the kit

Everything of durable value here is the **workflow**, not the tooling around
it: five commands (`sdd.start`, `sdd.spec`, `sdd.plan`, `sdd.build`,
`sdd.finish`), an explicit approval gate at every phase transition (captured
with who approved and when — `git config user.name`, not "AI Agent"), and an
archive step that moves a feature from `wip/` to `features/` with a
generated `README.md` and `implementation-summary.md`. That combination —
used on *every* feature, with zero exceptions across two full cycles —
produced a real paper trail: two archived features, each with a rich
`meta.md` recording exactly which decisions were made, by whom, and why.

If you deleted every script, skill, and agent below and kept nothing but
this loop, you'd keep most of the value. Everything else is instrumentation
around that loop, of uneven quality.

---

## Scripts (`framework/tools/`)

| Script | Verdict | What we actually found |
|---|---|---|
| `validate-functional.sh` | ✅ **Works, keep** | Caught 4 real errors in a functional spec (missing `## Success Metrics`, non-numbered `## Objectives`, wrong-language `Acceptance Criteria` heading). Fast, no LLM cost. |
| `display-tasks.sh` | ✅ **Works, keep** | Clean deterministic task table before every approval. No issues found. |
| `detect-stack.sh` | ✅ **Works, keep** | Correctly identifies language/build tool. |
| `scan-features.sh` | ✅ **Works, keep** | Lists wip/completed features correctly. Untested until this audit — works fine. |
| `detect-language.sh` | ✅ **Works, keep** | Correctly detects Java/Maven/JUnit. Minor: suggests `mvn` commands, not a project's actual wrapper (`./mvnw`) — cosmetic, not wrong. |
| `validate-code.sh` | 🔧 **Fixed — was 100% broken, now works** | This is a generic OWASP/performance/quality grep scanner (SQL injection, hardcoded secrets, N+1, empty catch, etc.) — **not** company-specific despite what `sdd-validator`'s own doc implies. It crashed with exit 1 on *every* invocation, including clean runs, because `[ -z "$VAR" ] && VAR=0` as a function's last statement returns non-zero whenever `$VAR` was already set — combined with `set -e` and a bare `fn &&` call site, the whole script died the instant `scan_security()` finished, before ever printing a verdict. Fixed with an explicit `return 0` at the end of each `scan_*` function. Verified both directions: 0 findings → `APPROVED`; a file with a hardcoded password and `SELECT *` → `CANNOT_PROCEED` with the right line numbers. |
| `validate-complete.sh` | 🔧 **Fixed — was silently lying** | Counted total tasks with `grep -c "^#### TASK-[0-9]" "$TASKS_FILE"` where `$TASKS_FILE` is **JSON**, not markdown — always 0. Completed tasks were counted from `progress.md` instead, requiring a specific markdown heading shape that isn't the only reasonable way to write that file. Net effect: reported "0/0 completed" and passed trivially, regardless of real state. Fixed to count both totals directly from `tasks.json`'s own `"id"`/`"status"` fields (no `jq` dependency — `/sdd.plan` always pretty-prints one field per line, so `grep -c` is reliable). Verified against both archived features (17/17, 11/11 — the real numbers) and against a synthetic incomplete task set (correctly reports 10/11 and names the missing one). |
| `detect-phase.sh` | 🔧 **Fixed — template/parser mismatch** | The kit's own `templates/meta.md` writes `**Current Stage**: x` (markdown-bold); the script's regex was `^Current Stage:` (no bold tolerance) and never matched, silently falling through to a YAML-inference fallback that only recognized `status: in-progress` — never `completed`. A fully finished, archived feature was reported as still being in the `tasks` phase. Fixed both: the regex now tolerates optional `**`, and the fallback recognizes `completed` as `implementation` too. |
| `manage-backlog.sh` | ❌ **Removed** | Two independent, compounding defects: (1) hardcoded `BACKLOG_FILE="meli/backlog.md"` — not configurable via flag or env var, and that path won't exist in any project that doesn't share that exact convention; (2) its parser expects a flat checkbox list (`grep -c "^- \["`), incompatible with a prose-based backlog (`### TODO-N: Title` + bulleted fields), which is a genuinely richer format for carrying context per item. Confirmed the damage directly: running `manage-backlog.sh list` on this repo silently created a stray `meli/` folder with an empty stub backlog, fully disconnected from the real one — a script that fails this quietly is worse than no script. Edit the backlog file by hand; it works, and it's what both features here actually did throughout. |

**Known caveat, not fixed**: `validate-code.sh`'s exclusion filter
(`grep -v "...test\|Test..."`) matches **any substring** "test", not a path
segment. A file or class name containing "test" as a substring in a
non-test context — plausible in a Portuguese codebase (`teste`, `atestado`,
`protesto`, `contestar`) — would be silently excluded from scanning. No
collision exists in this project today; worth a word-boundary-safe rewrite
before trusting it on a codebase where that's a live risk.

---

## Skills (`.claude/skills/`)

| Skill | Verdict | Evidence |
|---|---|---|
| `sdd-code-reviewer` | ✅ **Real value** | Invoked 4 times (code + security review, once per feature). The value isn't magic in the skill — it's the forced discipline of writing a structured verdict file, which made the review actually happen instead of "looks fine." |
| `sdd-validator` | ⚠️ **Marginal on top of what you already do** | Invoked at every `/sdd.finish`. Most of its documented checklist ( Docker registry compliance, `/ping` endpoints, Nexus config) never applied here; the part that mattered (confirm build/test/coverage) was already independently verified via `./mvnw clean verify` before the skill was even called. |
| `java-spring-expert` | 🔧 **Patched — was a live risk, not just unused** | Its trigger keywords ("Java", "Spring Boot", "Maven", "Repository"...) are generic enough to fire in **any** Java conversation, and its documented stack is pinned to Spring Boot **3.x** — actively wrong for a Boot 4+ project (10 confirmed breaking changes hit during this project: `@MockBean`→`@MockitoBean`, `AutoConfigureMockMvc`'s package, `HttpStatus.UNPROCESSABLE_ENTITY`→`UNPROCESSABLE_CONTENT`, `SecurityProperties`→`SecurityFilterProperties` in a different jar, and more). Not removed — a future project *could* be on Boot 3 — but patched with a mandatory version-verification block at the top: confirm the real `pom.xml`/`build.gradle` version and check the actual jar before trusting any snippet below. |
| `sdd-kit-expert` | ❓ **Never invoked, in two complete cycles** | Its stated purpose is "framework knowledge and guidance" — but reading each `/sdd.*` command's own markdown directly, every time, worked fine without it. That's not proof it's useless everywhere, but it's proof it wasn't load-bearing here. |
| `sdd-performance-expert` | ❓ **Never invoked** | The actual performance work (confirming index usage) was done with `EXPLAIN ANALYZE` directly against Postgres — more concrete and verifiable than a generic checklist would have been. |
| `context-guardian` | ❓ **Never needed** | Context never reached a level where active management mattered. |

---

## Agents (`.claude/agents/`)

| Agent | Verdict | Evidence |
|---|---|---|
| `sdd-layer-analyzer` | ⚠️ **Real value, needs independent verification** | Invoked twice for the pre-archive consistency check. Second run: fully accurate, one legitimate LOW finding. First run: three legitimate findings **mixed with one false HIGH** — it claimed two files "were never committed" based on a remembered `git status`, when both had been in `HEAD` for hours (confirmed independently via `git ls-tree`). **Never act on a claim from this agent about git/file state without verifying it yourself first** — it does not always run `git` itself before asserting what git says. |
| `sdd-implementer`, `sdd-small-test-writer`, `sdd-system-designer`, `sdd-validator-runner` | ➖ **Unused by design, not by defect** | Built for aggressive per-task delegation to preserve the orchestrator's context window. Under an operating instruction that avoids spawning subagents unless asked, all implementation happened inline instead — so this whole delegation layer sat idle across ~28 tasks. Worth keeping installed: a large enough feature under a context-constrained operating mode would actually need them. |
| `sdd-backlog-manager`, `sdd-debugger`, `sdd-explorer`, `sdd-project-wizard` | ➖ **Not exercised** | `sdd-project-wizard` did real work exactly once (initial `PROJECT.md` creation) — a legitimate one-time-use tool. The other three never had a matching situation arise (no brownfield exploration needed, no bug required a dedicated root-cause agent, backlog edited by hand throughout). |

---

## Commands (`.claude/commands/`)

Of 18 installed, **5 were used, every single time, with zero exceptions**:
`sdd.start`, `sdd.spec`, `sdd.plan`, `sdd.build`, `sdd.finish`. That's the
whole core loop.

Never invoked, but plausibly useful the day the situation arises:
`sdd.check`, `sdd.fix`, `sdd.rollback`, `sdd.list`, `sdd.backlog`,
`sdd.doctor`, `sdd.project`.

Structurally not applicable to a single-repo, non-hub project with no
external specs to import and no legacy code needing reverse engineering:
`sdd.hub` (multi-repo coordination), `sdd.import` (external spec ingestion),
`sdd.reverse-eng` (this only matters for brownfield code that predates its
specs — moot for a project that had specs from day one).

**The elephant in every command file**: most of the 18 command markdown
files carry large blocks of instructions assuming a specific origin
company's internal tooling (a CLI named `fury`, an internal MCP for
provisioning services, an internal auth flow, an internal CI pipeline, a
mobile design-system skill, a frontend framework skill) — none of which
exists outside that company. Stripping this from all 18 files was judged
out of scope for one pass: too large, too easy to break cross-command
references without a second real project to test against. The workaround
that already worked twice, without needing to touch the command files at
all: **document what was skipped and why, per feature, in that feature's
own `tasks.json` under an `"adaptations"` array** — visible, deliberate,
and it costs nothing to repeat.

---

## If you're starting a new project from this kit

1. Copy `sdd-kit/` in, run `install.sh`.
2. Read this file once.
3. Expect to write an `"adaptations"` array in your first feature's
   `tasks.json` — you will not be the exception.
4. Don't run `manage-backlog.sh` — it's gone, and for good reason above.
5. If your stack is Java, read `java-spring-expert`'s version-check block
   before trusting anything it says.
6. Never act on an `sdd-layer-analyzer` claim about git state without
   checking `git` yourself.

---

*Last updated: 2026-08-31, after archiving two features on AgendaIA
(`cadastro-estabelecimento-login`, `cadastro-profissional`).*
