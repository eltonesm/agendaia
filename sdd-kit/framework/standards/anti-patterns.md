# SDD Kit — Anti-Patterns

Common mistakes that reduce the value of Spec-Driven Development. Avoid these patterns.

---

## Spec Anti-Patterns

### AP-01: Coding Before Specifying

**Anti-Pattern**: Starting implementation before functional and technical specs are approved.

**Why it's bad**: Misalignment is discovered late — after code is already written. Rework cost is 5-10x higher than clarifying specs upfront.

**Correct approach**: Always complete and approve `/sdd.spec` before `/sdd.plan` or `/sdd.build`.

---

### AP-02: Vague Acceptance Criteria

**Anti-Pattern**: Writing acceptance criteria like "the feature should work correctly" or "performance should be good."

**Why it's bad**: No shared definition of done. Leads to endless scope creep.

**Correct approach**: Every user story must have testable, measurable acceptance criteria:
- ❌ "The login should be fast"
- ✅ "Login response time < 500ms for 95% of requests"

---

### AP-03: Giant Specs

**Anti-Pattern**: Writing a 50-page functional spec before any validation.

**Why it's bad**: Specs change when users see working software. Over-specified upfront work gets thrown away.

**Correct approach**: Use the Lite mode (`/sdd.spec --lite`) for uncertain features. Expand only after validation.

---

### AP-04: Skipping Technical Spec

**Anti-Pattern**: Going straight from functional spec to implementation without a technical spec.

**Why it's bad**: Architecture decisions aren't documented. Future developers don't understand the WHY.

**Correct approach**: Always create a technical spec. Even a short one captures key decisions.

---

## Task Anti-Patterns

### AP-05: Tasks Without Acceptance Criteria

**Anti-Pattern**: Tasks like "Implement authentication" with no clear definition of done.

**Why it's bad**: Impossible to validate. The task is "done" when someone decides it is.

**Correct approach**: Every task in `tasks.json` must have `acceptance_criteria` that can be verified.

---

### AP-06: Giant Tasks

**Anti-Pattern**: A single task that takes more than 1-2 days.

**Why it's bad**: Progress is invisible. Blockers hide inside big tasks.

**Correct approach**: Break large tasks into subtasks of 2-4 hours each.

---

### AP-07: No Task Dependencies

**Anti-Pattern**: Listing tasks without `depends_on` references.

**Why it's bad**: Tasks are executed in wrong order. Blockers discovered at implementation time.

**Correct approach**: Map all dependencies in `tasks.json` before starting `/sdd.build`.

---

## Implementation Anti-Patterns

### AP-08: Skipping Tests

**Anti-Pattern**: Marking a task as done without writing tests.

**Why it's bad**: Regressions are undetectable. Technical debt accumulates.

**Correct approach**: Every task that modifies behavior must include unit tests. Minimum 80% coverage for new code.

---

### AP-09: Spec Drift

**Anti-Pattern**: Implementing differently from the technical spec without updating the spec.

**Why it's bad**: Specs become documentation lies. Future developers are misled.

**Correct approach**: If implementation diverges, update the spec first. Use `/sdd.fix` to sync layers.

---

### AP-10: Ignoring Quality Gates

**Anti-Pattern**: Pushing code that fails linting or tests and marking it as done.

**Why it's bad**: Broken builds block other developers. Technical debt compounds.

**Correct approach**: Run `sdd-validator` before marking any task complete. Don't skip quality gates.

---

### AP-11: Hardcoded Credentials

**Anti-Pattern**: Embedding API keys, passwords, or secrets in source code.

**Why it's bad**: Security vulnerability. Credentials get committed to version control.

**Correct approach**: Always use environment variables or a secrets manager.

---

## Collaboration Anti-Patterns

### AP-12: No Review Before Merging

**Anti-Pattern**: Self-approving pull requests or merging without any review.

**Why it's bad**: Bugs escape to production. Knowledge siloing.

**Correct approach**: At least one other team member reviews every PR. Use `sdd-code-reviewer` for AI-assisted review.

---

### AP-13: Skipping `/sdd.finish`

**Anti-Pattern**: Stopping work after merging the PR without running `/sdd.finish`.

**Why it's bad**: Feature stays in `sdd/wip/` forever. State is unclear for the team.

**Correct approach**: Always run `/sdd.finish` after the PR is merged. It archives the feature and updates metrics.

---

## Summary Table

| ID | Anti-Pattern | Severity |
|----|-------------|---------|
| AP-01 | Coding before specifying | 🔴 Critical |
| AP-02 | Vague acceptance criteria | 🔴 Critical |
| AP-03 | Giant specs | 🟡 High |
| AP-04 | Skipping technical spec | 🔴 Critical |
| AP-05 | Tasks without acceptance criteria | 🔴 Critical |
| AP-06 | Giant tasks | 🟡 High |
| AP-07 | No task dependencies | 🟡 High |
| AP-08 | Skipping tests | 🔴 Critical |
| AP-09 | Spec drift | 🟡 High |
| AP-10 | Ignoring quality gates | 🔴 Critical |
| AP-11 | Hardcoded credentials | 🔴 Critical |
| AP-12 | No review before merging | 🟡 High |
| AP-13 | Skipping /sdd.finish | 🟢 Medium |
