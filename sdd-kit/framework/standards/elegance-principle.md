# The Elegance Principle

> **Specs should be as small and elegant as possible** - containing only what's necessary for AI agents and humans to succeed.

---

## Core Philosophy

Every line in a specification should serve a purpose. AI agents are smart enough to infer obvious implementation details - your job is to communicate **intent**, **constraints**, and **edge cases**, not step-by-step instructions.

**Think of it like giving directions to a smart colleague**:
- ❌ "Go 500 feet north on Main St, turn 90 degrees clockwise at the traffic signal..."
- ✅ "Coffee shop on Main and 5th"

**They know how to get there!**

---

## What to Include vs Exclude

### Include (Signal)

| Include | Why |
|---------|-----|
| What the product does and why | Core intent |
| Clear acceptance criteria | Measurable success |
| Edge cases that must be handled | Prevent bugs |
| Performance targets that matter | Non-negotiable constraints |
| Security requirements | Critical for compliance |
| Business rules | Domain-specific logic |
| Diagrams and wireframes | Visual > 500 lines of description |

### Exclude (Noise)

| Exclude | Why |
|---------|-----|
| Obvious implementation details | AI will infer them |
| Boilerplate that adds no clarity | Wastes context |
| Repetitive information | Single source of truth |
| "Just in case" requirements | Add when actually needed |
| Over-specification | Constrains unnecessarily |
| Step-by-step obvious flows | AI knows how login works |

---

## Size Benchmarks

**Target sizes for a typical feature**:

| Phase | Target Size | Notes |
|-------|-------------|-------|
| **Functional Spec** | 1-2 pages | Per feature, not per story |
| **Technical Spec** | 2-3 pages | Architecture + API + Data |
| **Tasks** | 0.5 page | 5-15 tasks with clear deliverables |
| **Total Feature** | 4-6 pages | NOT 50 pages! |

**For the entire project**:

| Document | Target Size |
|----------|-------------|
| Non-Functional Requirements | 1 page |
| Security Requirements | 1-2 pages |
| Testing Strategy | 1 page |

**Rule of thumb**: If you can't explain it in a 30-second verbal summary, your spec is too complex.

---

## Bloated vs Elegant Examples

### Example 1: User Stories

```yaml
# ❌ BLOATED (unnecessary detail)
user_story:
  id: "US-001"
  epic_id: "EPIC-001"
  sprint: "Sprint 23"
  story_points: 5
  created_by: "John Doe"
  created_date: "2024-01-15"
  priority_score: 8.5
  title: "As a user I want to create a task"
  description: |
    This user story describes the functionality where a user
    who is authenticated and has appropriate permissions can
    create a new task in the system by filling out a form...
  acceptance_criteria: |
    1. Given that the user is logged in
       And the user is on the task board page
       And the user has permission to create tasks
       When the user clicks the "New Task" button
       Then a modal dialog should appear
       And the modal should contain a form
       ... (300 more lines of obvious detail)

# ✅ ELEGANT (just enough)
user_story: "User can create tasks with title, description, priority, and due date"

acceptance_criteria:
  - "Title required (1-200 chars)"
  - "Description optional (markdown)"
  - "Priority: high/medium/low"
  - "Due date optional, cannot be past"

edge_cases:
  - "Empty title → validation error"
  - "Offline → queue locally, sync when online"
```

**The difference**: 10 lines instead of 300. AI infers the obvious.

### Example 2: Technical Specs

```yaml
# ❌ BLOATED (implementation details)
database:
  connection_pooling:
    pool_size: 20
    max_overflow: 10
    pool_timeout: 30
    recycle: 3600
  query_optimization:
    always_use_prepared_statements: true
    enable_query_cache: true
    cache_ttl: 300
  indexes:
    - table: users
      columns: [email]
      type: btree
      unique: true
    ... (200 more lines)

# ✅ ELEGANT (what matters)
database:
  engine: " MySQL"
  key_entities: ["User", "Task", "Project"]
  critical_queries:
    - "Get tasks by user (< 50ms)"
    - "Search tasks by title (< 100ms)"
```

### Example 3: Security

```yaml
# ❌ BLOATED (how, not what)
authentication:
  implementation: |
    We will implement JWT authentication using the RS256
    algorithm. The private key will be a 2048-bit RSA key
    stored in AWS Secrets Manager. The token payload will
    include: sub, iat, exp, aud, user_id, email, roles.
    The expiration will be 3600 seconds. Token transmitted
    via Authorization header with Bearer scheme...
    (500 more lines)

# ✅ ELEGANT (requirements only)
authentication:
  method: "JWT with RS256"
  token_expiry: "1 hour"
  storage: "HttpOnly cookies"

authorization:
  model: "RBAC"
  roles: [admin, member, viewer]
```

---

## Spec Review Checklist

Before approving any specification, verify:

### Completeness Check
- [ ] Can AI generate working code from this?
- [ ] Can a developer understand what to build?
- [ ] Are critical edge cases covered?
- [ ] Are acceptance criteria measurable?

### Minimalism Check
- [ ] Is every line necessary?
- [ ] Have I removed obvious details AI will infer?
- [ ] Is it under the size benchmark?
- [ ] Could this be shorter without losing clarity?

### Clarity Check
- [ ] Is it unambiguous?
- [ ] Would two people read this the same way?
- [ ] Are terms defined clearly?
- [ ] Are examples provided for complex concepts?

### Actionability Check
- [ ] Can AI start working immediately?
- [ ] Is it clear what "done" means?
- [ ] Are dependencies identified?

---

## Anti-Patterns: Spec Bloat

### 1. Copy-Paste from Other Docs

**Problem**: Copying requirements from PRDs, design docs without condensing.

**Solution**: Extract only essential information. Summarize, don't duplicate.

### 2. Future-Proofing

**Problem**: Adding requirements for features you "might" need later.

```yaml
# ❌ Don't
Task:
  id: UUID
  title: String
  custom_field_1: String  # Might need later
  custom_field_2: String  # Future customization
  custom_field_3: String

# ✅ Do
Task:
  id: UUID
  title: String
  # Add fields when actually needed
```

**Solution**: YAGNI - You Aren't Gonna Need It. Add when required.

### 3. Over-Explaining to AI

**Problem**: Treating AI like it knows nothing.

```yaml
# ❌ Don't
authentication:
  explanation: |
    Authentication is the process of verifying identity.
    JWT stands for JSON Web Tokens. A JWT is a compact,
    URL-safe means of representing claims...

# ✅ Do
authentication:
  method: "JWT (RS256)"
  expiry: "1 hour"
```

**AI already knows what JWT is!**

### 4. Premature Optimization

**Problem**: Specifying optimizations before measuring.

```yaml
# ❌ Don't (first version)
performance:
  caching:
    strategy: "Cache-aside pattern"
    ttl: "5 minutes"
    cache_warming: "Pre-load top 100 items"
    eviction_policy: "LRU"

# ✅ Do (first version)
performance:
  api_response: "< 200ms (p95)"
  # Add caching if measurements show it's needed
```

**Solution**: Start with targets, optimize when measurements prove the need.

### 5. Describing Images in Words

**Problem**: Writing 500 lines describing UI when a wireframe would suffice.

```markdown
# ❌ Don't
The task card component shall be a rectangular container
with rounded corners (border-radius: 8px) and a subtle
shadow (box-shadow: 0 2px 4px rgba(0,0,0,0.1))...
(500 more lines of CSS details)

# ✅ Do
![Task Card Wireframe](./wireframes/task-card.png)

Interactions:
- Click → Open details
- Drag → Change status
```

---

## Why Minimal Specs Work Better

**Counter-intuitive insight**: Shorter specs often produce better AI output because:

1. **Less noise**: AI focuses on what matters, not boilerplate
2. **Faster processing**: AI handles context more efficiently
3. **Easier to maintain**: Changes are quick and obvious
4. **Clearer intent**: High signal-to-noise ratio
5. **Room for AI intelligence**: Let AI make smart choices

---

## Applying to SDD Kit Phases

### Phase 1: Functional Spec

**Focus on**: User outcomes, acceptance criteria, edge cases
**Skip**: Obvious UI flows, implementation hints

### Phase 2: Technical Spec

**Focus on**: Architecture decisions, API contracts, data model, constraints
**Skip**: Implementation details, code structure, obvious patterns

### Phase 3: Tasks

**Focus on**: Clear deliverables, dependencies, acceptance criteria
**Skip**: Step-by-step instructions, obvious sub-tasks

### Phase 4: Implementation

**Focus on**: Edge cases found, decisions made, blockers
**Skip**: Documenting every line of code written

---

## Summary

> **Maximum clarity with minimum words. Let AI do the heavy lifting.**

| Metric | Target |
|--------|--------|
| Total spec size | 4-6 pages per feature |
| Effort to write | Low-Medium |
| Effort for AI to code | Low-Medium |
| Clarification requests | < 3 per feature |
