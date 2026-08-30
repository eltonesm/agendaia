# Project Governance

**Framework**: SDD Kit
**Version**: 1.0.0
**Last Updated**: 2025-11-27

---

## Purpose

This document establishes the governance principles, development standards, and decision-making framework for this project. All features developed using SDD Kit must adhere to these principles.

---

## Core Principles

### 1. Specification-First Development

**Principle**: All code must originate from complete, validated specifications.

**What this means**:
- No coding begins until functional spec is approved
- No implementation starts until technical spec is validated
- All changes must update specs first, then code

**Why**: Reduces rework, improves predictability, enables AI to generate higher-quality code.

---

### 2. AI-Human Collaboration

**Principle**: AI assists, humans decide.

**What this means**:
- AI helps draft specs, humans review and approve
- AI generates tasks, humans refine and prioritize
- AI implements code and ensures technical quality, humans validate correctness and alignment

**Why**: Leverages AI efficiency while maintaining human oversight and domain expertise.

---

### 3. Quality Gates Are Mandatory

**Principle**: Cannot skip phase validation.

**What this means**:
- Functional spec must pass validation before moving to technical
- Technical spec must pass validation before moving to tasks
- Tasks must pass validation before implementation starts
- Implementation must pass validation before archival

**Why**: Prevents incomplete work from propagating, ensures consistent quality.

---

### 3.1 Phase Iteration Limits

**Principle**: Iterate with purpose, not indefinitely.

**Maximum Iterations Per Phase**:

| Phase | Max Iterations | Warning Threshold |
|-------|---------------|-------------------|
| Functional Spec Clarify | 5 | 3 |
| Technical Spec Clarify | 5 | 3 |
| Task Refinement | 3 | 2 |
| Implementation Review | 3 | 2 |

**Decision Framework - When to Stop Iterating**:

✅ **Proceed to next phase when**:
- All MUST requirements are addressed
- No blocking ambiguities remain
- Stakeholders have approved
- Validation passes

⚠️ **Warning signs of over-iteration**:
- Same feedback appearing multiple times
- Diminishing changes between iterations
- Scope creep through "improvements"
- Stakeholder fatigue

❌ **Stop and escalate when**:
- Maximum iterations reached without resolution
- Fundamental disagreement persists
- Requirements keep changing significantly
- Technical feasibility in question

**Enforcement**:
- Validators track iteration count in `meta.md`
- Warning issued at threshold
- Escalation required at maximum
- Document reason if proceeding beyond limit

---

### 4. Testing is Mandatory (Non-Negotiable)

**Principle**: Every feature MUST include tests. No exceptions.

**What this means**:
- Every task that produces code MUST have corresponding tests
- Tests are NOT optional or "nice to have" - they are REQUIRED
- No feature can be completed without passing test validation
- Test tasks are AUTO-GENERATED and CANNOT be removed

**Framework defaults** (customizable via `sdd/PROJECT.md`):
- Test coverage minimum: 80%
- Unit tests: 60% of test suite
- Integration tests: 30% of test suite
- E2E tests: 10% of test suite

> See [CONFIGURATION.md](../CONFIGURATION.md) for how to customize these values.

**Enforcement**:
- `/sdd.plan` automatically adds test tasks
- `/sdd.finish` BLOCKS if tests are missing or failing
- `validate-tests.sh` runs before feature archival

**Why**: Untested code is broken code waiting to happen. Tests are the only way to ensure code works as intended and continues to work after changes.

---

### 5. Continuous Documentation

**Principle**: Documentation is not optional, it's a core requirement.

**What this means**:
- All design decisions must be documented with rationale
- All deviations from standards must be explained
- All technical debt must be tracked
- All specs must remain up-to-date with implementation

**Why**: Enables knowledge transfer, facilitates maintenance, supports future development.

---

### 6. your team Standards Compliance

**Principle**: your platform is the default choice.

**What this means**:
- Invoke `Skill("sdd-system-designer")` (fury-services plugin) for existing services before designing any solution
- Use  services (IAM, Messaging, DataStore, etc.) for all standard needs
- Document all  services used in technical specs
- Follow Meli standards (validated via code review tool)

**Why**: Consistency across your team projects, reduced duplication, battle-tested solutions.

---

### 7. Command Safety (Non-Negotiable)

**Principle**: Destructive operations ALWAYS require explicit human approval.

> **See also**: `standards/boundaries.md` for the complete 3-tier boundary system (✅ Always Do / ⚠️ Ask First / 🚫 Never Do)

**What this means**:
- AI agents MUST NEVER execute dangerous commands without human confirmation
- Destructive operations are BLOCKED until a human explicitly approves
- Even in auto-approve mode, dangerous operations require manual approval

**Dangerous commands include** (but are not limited to):

| Category | Examples |
|----------|----------|
| File deletion | `rm -rf`, `rm -r`, `del /s` |
| Elevated privileges | `sudo`, `su`, `runas` |
| Git destructive | `git push --force`, `git reset --hard`, `git clean -fd` |
| System modification | `chmod 777`, `chown -R`, format commands |
| Database operations | `DROP TABLE`, `DELETE FROM` without WHERE, `TRUNCATE` |
| Network exposure | Commands that expose ports publicly |
| Credential handling | Commands that output or transmit secrets |

**Enforcement**:
- AI agents MUST pause and ask for explicit confirmation before executing
- The confirmation must include a clear explanation of what will be affected
- If the human declines, the agent must propose a safer alternative
- All dangerous command requests must be logged

**Example interaction**:
```
AI: I need to run `rm -rf ./old-migrations/` to clean up old files.
    This will permanently delete 47 files in ./old-migrations/

    Do you approve this action? (yes/no)

Human: yes

AI: [Executes command]
```

**Why**: Irreversible operations can cause significant damage. Human oversight ensures intentionality and prevents accidents.

---

## Decision-Making Framework

### When to Use  Services

**Always use  when service exists and**:
- ✅ Meets functional requirements
- ✅ Performance characteristics are acceptable
- ✅ Integration effort is acceptable (< 2 days setup)

**Document all  services used in technical spec.**

---

### When to Deviate from Patterns

**Acceptable deviations**:
- ✅ Significant performance improvement (with benchmarks)
- ✅ Critical business requirement not supported by standard
- ✅ Experimental feature exploring new approach
- ✅ External integration constraints

**Unacceptable deviations**:
- ❌ "It's faster to code this way" without documentation
- ❌ Personal preference without technical justification
- ❌ Ignorance of existing patterns

**Always document deviations in technical spec with clear rationale.**

---

### When to Update Specs

**Update specs when**:
- ✅ Requirements change during development
- ✅ Technical constraints discovered during implementation
- ✅ Architecture decisions change
- ✅ Scope adjusted by stakeholders

**Process**:
1. Update the spec document (functional or technical)
2. Re-run validation (`/sdd.check --validate`)
3. Get re-approval from stakeholders
4. Proceed with implementation

---

## Quality Standards

These are **framework defaults**. Customize via `sdd/PROJECT.md` (see [CONFIGURATION.md](../CONFIGURATION.md)).

### Code Quality

- **Test Coverage**: Minimum 80% (default, customizable)
- **Linting**: Zero linter errors
- **Type Safety**: Zero type errors (TypeScript/Flow)
- **Code Review**: All code reviewed via code review tool before merge (default, customizable)

### Specification Quality

- **Completeness**: All required sections present
- **Clarity**: No ambiguous terms (fast, easy, many, etc.)
- **Measurability**: Success metrics are quantifiable
- **Testability**: Acceptance criteria are verifiable

### Documentation Quality

- **REST API Documentation**: All public REST APIs documented
- **Architecture Diagrams**: Up-to-date with implementation
- **Runbooks**: Operational procedures documented
- **Decision Records**: All major decisions tracked

---

## Phase Responsibilities

### Phase 1: Functional Spec

**Focus**: Define WHAT to build and WHY
**Human responsibilities**:
- Define problem statement and goals
- Write user stories with acceptance criteria
- Define success metrics
- Approve functional spec

**Cannot proceed without**: Complete and approved functional spec

---

### Phase 2: Technical Spec

**Focus**: Define HOW to build technically
**Human responsibilities**:
- Make architectural decisions with rationale
- Plan  service integration
- Define performance and scalability targets
- Approve technical spec

**Cannot proceed without**: Approved functional spec

---

### Phase 3 & 4: Tasks & Implementation

**Focus**: Break down and implement
**Human responsibilities**:
- Refine AI-generated tasks
- Validate task estimations
- Review AI-generated code
- Validate implementation quality

**Cannot proceed without**: Approved technical spec and task list

---

### AI Agent (All Phases)

**Assists with**:
- Drafting specs through interviews
- Generating granular tasks
- Implementing code
- Running tests and validations

**Limitations**:
- Cannot approve specs (human approval required)
- Cannot skip quality gates
- Cannot deviate from governance principles
- Cannot execute dangerous commands without human approval (see Principle 7)

---

## Conflict Resolution

### When Specs and Code Diverge

1. **Identify the divergence** (spec vs actual implementation)
2. **Determine correct state**:
   - If spec is correct: Update code to match
   - If implementation is correct: Update spec with rationale
3. **Re-validate** the updated artifact
4. **Document** the divergence and resolution

### When Stakeholders Disagree

1. **Escalate** through appropriate channels
2. **Decision criteria**: Aligned with business goals, technical feasibility, timeline impact
3. **Documentation**: Record disagreement, decision, and rationale in specs

---

## Amendment Process

This governance document can be amended when:
- Project requirements significantly change
- New patterns emerge that should be standardized
- Governance issues discovered that need addressing

**Amendment process**:
1. Propose change with rationale
2. Review with team
3. Update governance document
4. Communicate changes to all stakeholders
5. Apply to new features immediately

---

## Enforcement

**Quality gates automatically enforce**:
- Spec completeness
- Phase transitions
- Test coverage
- Code quality

**Human review enforces**:
- Adherence to principles
- Decision rationale quality
- Documentation completeness
- Deviation justifications

**AI agent constraints enforce**:
- Dangerous command protection (Principle 7)
- Approval requirements before destructive operations

**Consequences of non-compliance**:
- Specs rejected at validation gates
- Implementation blocked until specs updated
- Technical debt tracked and prioritized

---

## References

- [SDD Kit Workflow](../WORKFLOW.md)
- [ Guidelines](./fury-guidelines.md)
