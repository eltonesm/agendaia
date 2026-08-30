# {{FEATURE_NAME}} - Functional Spec

**Status**: draft | in-review | approved
**Owner**: [Name]
**Created**: {{DATE}}
**Last Updated**: {{DATE}}

---

> **Purpose**: This spec defines the **user experience** - WHAT we're building and WHY.
> It describes the product from the user's perspective and serves as the contract for validating that the final implementation delivers the intended experience.

---

## Elegance Guidelines

> **Target size**: 1-2 pages per feature. See [elegance-principle.md](../standards/elegance-principle.md)

**Keep it elegant**:
- ✅ Focus on **outcomes**, not step-by-step obvious flows
- ✅ Include only **critical edge cases** AI wouldn't infer
- ✅ Use diagrams/wireframes instead of verbose descriptions
- ❌ Don't describe how login/CRUD/forms work (AI knows)
- ❌ Don't add "just in case" requirements

**Example - Bloated vs Elegant**:
```yaml
# ❌ Bloated: 50 lines describing login flow steps
acceptance_criteria: |
  1. User clicks login button
  2. Modal appears with form
  3. Form has email field...

# ✅ Elegant: 5 lines with what matters
acceptance_criteria:
  - "Valid credentials → redirect to dashboard"
  - "Invalid → error message, stay on page"
  - "Rate limit: 5 attempts per 15 min"
```

---

## Spec Reference Annotations (Single Source of Truth)

> **v2.1.0**: If this feature modifies existing behavior from other features, add explicit reference annotations.

**Reference Types**:
- `<!-- overrides: path#section -->` - Completely replaces existing behavior
- `<!-- extends: path#section -->` - Adds to existing behavior (backward compatible)
- `<!-- deprecates: path#section -->` - Marks existing behavior as obsolete

**Example usage**:
```markdown
## User Stories

<!-- overrides: sdd/features/auth-v1/functional-spec.md#login-user-story -->
As a user, I can now log in using Google OAuth in addition to email/password.

## Business Rules

<!-- extends: sdd/features/payment-v1/functional-spec.md#refund-rules -->
Additional refund rules for international transactions are added.
```

**When to use**: Only add annotations when this feature intentionally modifies, extends, or deprecates functionality defined in another feature's spec.

---

## Problem Statement

> **Optional for technical/internal features.** Required for user-facing features that need stakeholder alignment.

Describe the problem you're solving:
- What is the current state?
- What pain points do users experience?
- Why is this important to solve?
- **Business Impact** (if applicable): Revenue impact, user impact, frequency

For simple technical features, a one-liner is sufficient:
**Problem**: [Describe in 1-2 sentences]

---

## Objectives

> **Alignment Check**: Objectives must align with the **Project Vision** defined in `sdd/PROJECT.md`.
> If the project has defined vision principles, ensure these objectives support them.

Define 2-5 SMART objectives (Specific, Measurable, Achievable, Relevant, Time-bound):

1. [Objective 1 with measurable target]
2. [Objective 2 with measurable target]
3. [Objective 3 with measurable target]

---

## Scope

> **Vision-Driven Scoping**: Use the **anti_goals** and **principles** from `sdd/PROJECT.md` vision
> to guide scope decisions. If something conflicts with the product vision, it's out of scope.

### In Scope

List what WILL be included in this feature:
- Functionality 1
- Functionality 2
- Functionality 3

### Out of Scope

List what will NOT be included (and why):
- Functionality X - Reason: [Will be addressed in future iteration]
- Functionality Y - Reason: [Not aligned with current objectives]
- Functionality Z - Reason: [Conflicts with project vision/anti-goals]

---

## User Stories

### US-1: [User Story Title]

**As a** [type of user]
**I want** [action/capability]
**So that** [benefit/value]

**Acceptance Criteria**:
- [ ] AC-1: [Specific, testable criterion]
- [ ] AC-2: [Specific, testable criterion]
- [ ] AC-3: [Specific, testable criterion]

**Priority**: High | Medium | Low
**Complexity**: S | M | L | XL

---

### US-2: [User Story Title]

**As a** [type of user]
**I want** [action/capability]
**So that** [benefit/value]

**Acceptance Criteria**:
- [ ] AC-1: [Specific, testable criterion]
- [ ] AC-2: [Specific, testable criterion]

**Priority**: High | Medium | Low
**Complexity**: S | M | L | XL

---

[Add more user stories as needed]

---

## Business Rules

> **Purpose**: Document explicit business logic, calculations, and validation rules that the system must enforce.
> These rules are critical for correct implementation - AI cannot infer them.

### Core Rules

| Rule ID | Rule | Example |
|---------|------|---------|
| BR-1 | [Business rule description] | If X=100, then Y=10 |
| BR-2 | [Business rule description] | Maximum allowed: 1000 |
| BR-3 | [Business rule description] | [Concrete example] |

### Calculations (if applicable)

> Provide concrete examples with real numbers to prevent incorrect implementations.

**Calculation**: [Name]
- **Formula**: [e.g., final_price = base_price * (1 - discount_rate)]
- **Example**: base_price=100, discount_rate=0.15 → final_price=85
- **Constraints**: [e.g., discount_rate ≤ 0.50, final_price ≥ 1]

### Validation Invariants

> Conditions that must ALWAYS be true. Violations indicate bugs.

- **INV-1**: [What must always be true, e.g., "amount > 0"]
- **INV-2**: [Another invariant, e.g., "end_date > start_date"]
- **INV-3**: [e.g., "status transitions: draft → active → completed (no skip)"]

### Exceptions

| Exception | Condition | Handling |
|-----------|-----------|----------|
| EX-1 | [When this exception applies] | [What to do instead] |
| EX-2 | [Condition] | [Handling] |

---

## Data Model

> **Purpose**: Define the structure of input and output data. Real examples prevent AI from inventing incorrect schemas.

### Input Data

**Source**: [BigQueue topic / REST API / User Form / Scheduled Job]

**Example** (real or representative):
```json
{
  "field1": "example_value",
  "field2": 123,
  "nested": {
    "subfield": true
  }
}
```

**Field Definitions**:

| Field | Type | Required | Validation | Description |
|-------|------|----------|------------|-------------|
| field1 | string | Yes | max 100 chars | [Purpose] |
| field2 | number | Yes | > 0 | [Purpose] |
| nested.subfield | boolean | No | - | [Purpose] |

### Output Data

**Destination**: [QKVS / External API / User response / Database]

**Example**:
```json
{
  "result_field": "processed_value",
  "status": "success",
  "metadata": {}
}
```

### Data Transformations

| Input | Transformation | Output |
|-------|----------------|--------|
| [Input field/value] | [What happens] | [Output field/value] |

---

## User Experience

> This section defines the complete user journey. The experience described here is what we commit to deliver and what E2E tests will validate.

### User Personas

**Primary User**: [Who is the main user?]
- Context: [When/where do they use this?]
- Goal: [What are they trying to accomplish?]
- Pain points: [Current frustrations]

**Secondary User** (if applicable): [Other user types]

---

### User Journey Map

**Entry Point**: [How does the user arrive at this feature?]

**Main Flow (Happy Path)**:

| Step | User Action | System Response | User Sees/Feels |
|------|-------------|-----------------|-----------------|
| 1 | [Action] | [Response] | [What user perceives] |
| 2 | [Action] | [Response] | [What user perceives] |
| 3 | [Action] | [Response] | [What user perceives] |
| ... | ... | ... | ... |

**Exit Point**: [How does the user know they're done? What's the success state?]

---

### Alternative Flows

**Alt-1: [Error/Edge Case Name]**

*Trigger*: [What causes this flow]
*User expectation*: [What does the user expect to happen?]

| Step | What Happens | User Sees |
|------|--------------|-----------|
| 1 | [Event] | [Feedback to user] |
| 2 | [Recovery action] | [Resolution state] |

**Alt-2: [Another Alternative]**

*Trigger*: [What causes this flow]
*User expectation*: [What does the user expect?]

| Step | What Happens | User Sees |
|------|--------------|-----------|
| 1 | [Event] | [Feedback] |
| 2 | [Resolution] | [Final state] |

---

### Edge Cases & Error Handling

> **Purpose**: Define behavior for system-level edge cases that AI cannot infer.
> Critical for data integrity and system reliability.

#### Data Edge Cases

| Scenario | Expected Behavior | Retry? | Notes |
|----------|-------------------|--------|-------|
| Duplicate event/request | [Idempotent - ignore / Process again / Error] | N/A | [How to detect duplicates] |
| Out-of-order events | [Buffer / Reject / Process anyway] | No | [Ordering requirements] |
| Concurrent modifications | [Lock / Last-write-wins / Merge / Error] | If conflict | [Conflict resolution] |
| Missing required field | [Reject with 400 / Use default / Skip record] | No | [Default values if any] |
| Invalid data format | [Reject / Transform / Log and skip] | No | [Validation details] |

#### System Failures

| Failure | Expected Behavior | Retry Policy | Fallback |
|---------|-------------------|--------------|----------|
| External API timeout | [Fail / Queue for later / Use cached] | 3x with exponential backoff | [Fallback action] |
| External API 4xx error | [Fail / Log / Retry once] | Depends on code | [4xx is usually not retryable] |
| External API 5xx error | [Retry / Queue / Alert] | 3x with backoff | [Fallback if all retries fail] |
| Database unavailable | [Fail / Use cache / Queue] | 5x with backoff | [Degraded mode?] |
| Message queue unavailable | [Fail / Store locally / Alert] | Infinite with backoff | [Guaranteed delivery?] |

#### Retry Configuration

| Operation | Max Retries | Backoff | Circuit Breaker |
|-----------|-------------|---------|-----------------|
| [External API call] | 3 | Exponential (1s, 2s, 4s) | Open after 5 failures |
| [Database write] | 5 | Linear (100ms) | N/A |
| [Message publish] | ∞ | Exponential (max 30s) | N/A |

---

### UI/UX References

> Visual references that define the experience. These are the source of truth for how the feature should look and feel.
> The Figma Selection Hash enables automatic component extraction during `/sdd.spec technical` and code generation during `/sdd.build`.

**Figma Design** (REQUIRED for frontend features):

| Campo | Valor | Propósito |
|-------|-------|-----------|
| **URL** | `https://www.figma.com/design/...` | Link visual para stakeholders |
| **Selection Hash** | `fileKey_nodeId_contentHash` | ID para MCP tools (ver formato abajo) |

**Formato del Selection Hash**:
```
{fileKey}_{nodeId}_{contentHash}

Ejemplo: Nv7pk23R1ZnlZwJ2Qi3bnO_8017:9903_11e8b1351563dec2
         |_____fileKey_____|_nodeId_|_____contentHash_____|
```

> **Cómo obtener el Selection Hash**: 
> 1. En Figma, selecciona el frame/componente principal del diseño
> 2. Usa el plugin "Frontender" para copiar el hash automáticamente
> 3. O extrae manualmente desde la URL: combina `fileKey`, `nodeId` y `contentHash`

**Componentes principales** (listar los frames de Figma):
- [Frame 1]: [Descripción y propósito]
- [Frame 2]: [Descripción y propósito]

- **Prototype**: [Link to interactive prototype if available]
- **Screenshots/Wireframes**: [Attach or link]

**Key UI Elements**:
- [Element 1]: [Purpose and behavior]
- [Element 2]: [Purpose and behavior]

**Interaction Patterns**:
- [Pattern 1]: [e.g., "Inline validation on form fields"]
- [Pattern 2]: [e.g., "Loading skeleton while fetching data"]

---

## Critical E2E Test Scenarios

> **CONDITIONAL SECTION**: This section is only included if user opted in for LTP E2E testing during `/sdd.spec`.
> Check `meta.md` → `ltp.enabled: true` before including this section.
>
> If `ltp.enabled: false`, skip this entire section (from here to "## Non-Functional Requirements").

---

> **Purpose**: These scenarios validate that the **user experience defined above** works correctly end-to-end.
> Each scenario should trace back to the User Journey and verify the experience from the user's perspective.

⚠️ **These are the acceptance tests for the product experience.** If these pass, the feature delivers what was promised.
⚠️ **LTP Integration**: These scenarios will be used by E2E test framework to auto-generate Cucumber/Playwright tests.

### E2E-1: [Scenario Name - Happy Path]

**Priority**: 🔴 Critical
**Related User Story**: US-1
**Preconditions**:
- [Precondition 1]
- [Precondition 2]

**Steps**:
1. [User action 1]
2. [Expected system response 1]
3. [User action 2]
4. [Expected system response 2]

**Expected Result**: [Final expected state]

---

### E2E-2: [Scenario Name - Key Alternative Flow]

**Priority**: 🔴 Critical | 🟡 High
**Related User Story**: US-X
**Preconditions**:
- [Precondition 1]

**Steps**:
1. [Step 1]
2. [Step 2]

**Expected Result**: [Final expected state]

---

### E2E-3: [Scenario Name - Error Handling]

**Priority**: 🟡 High
**Related User Story**: US-X
**Preconditions**:
- [Precondition that leads to error]

**Steps**:
1. [Step that triggers error]
2. [Expected error handling]

**Expected Result**: [User sees appropriate error, system remains stable]

---

[Add more E2E scenarios as needed - focus on critical business paths]

### E2E Test Summary

| ID | Scenario | Priority | User Story |
|----|----------|----------|------------|
| E2E-1 | [Name] | 🔴 Critical | US-1 |
| E2E-2 | [Name] | 🔴 Critical | US-2 |
| E2E-3 | [Name] | 🟡 High | US-3 |

---

## Non-Functional Requirements

### Performance
- Response time: [e.g., < 200ms for 95% of requests]
- Throughput: [e.g., Support 1000 concurrent users]
- Availability: [e.g., 99.9% uptime]

### Security
- Authentication: [Required? What method?]
- Authorization: [Role-based? Permissions?]
- Data encryption: [At rest? In transit?]
- Compliance: [PCI, GDPR, etc.]

### Usability
- Accessibility: [WCAG 2.1 Level AA compliance]
- Browser support: [Chrome, Firefox, Safari, Edge]
- Mobile support: [Responsive? Native app?]

### Scalability
- User growth: [Expected growth rate]
- Data growth: [Expected data volume]

---

## Success Metrics

> Define how you'll measure success. Include baselines (current state) and targets (desired state).

### Business Metrics

| Metric | Baseline | Target | Timeframe | How to Measure |
|--------|----------|--------|-----------|----------------|
| [Metric name] | [Current value] | [Target value] | [When to achieve] | [Dashboard/tool] |
| [Metric name] | [Current value] | [Target value] | [When to achieve] | [Dashboard/tool] |

### User Metrics

| Metric | Baseline | Target | Timeframe |
|--------|----------|--------|-----------|
| [User satisfaction/NPS] | [Current] | [Target] | [Timeframe] |
| [Task completion rate] | [Current] | [Target] | [Timeframe] |

### Technical Metrics

| Metric | Baseline | Target | Alert Threshold |
|--------|----------|--------|-----------------|
| [Error rate] | [Current %] | [Target %] | [Alert at %] |
| [Latency P95] | [Current ms] | [Target ms] | [Alert at ms] |
| [Availability] | [Current %] | [Target %] | [Alert at %] |

---

## Dependencies

List dependencies on other features, teams, or external factors:

- **Dependency 1**: [Description] - Owner: [Team/Person] - ETA: [Date]
- **Dependency 2**: [Description] - Owner: [Team/Person] - Status: [Blocked/In Progress/Complete]

---

## Risks

| Risk ID | Description | Impact | Probability | Mitigation Strategy |
|---------|-------------|--------|-------------|---------------------|
| RISK-1 | [Risk description] | High/Med/Low | High/Med/Low | [How to mitigate or avoid] |
| RISK-2 | [Risk description] | High/Med/Low | High/Med/Low | [How to mitigate or avoid] |

---

## Open Questions

Track unresolved questions that need answers:

- [ ] Question 1: [What needs to be decided?] - Owner: [Who will decide]
- [ ] Question 2: [What needs to be decided?] - Owner: [Who will decide]

---

## Assumptions

List assumptions made during spec creation:

1. [Assumption 1]
2. [Assumption 2]
3. [Assumption 3]

---

## References

- Related features: [Links to other features in sdd/features/]
- External documentation: [Links]
- Research: [User research, competitor analysis, etc.]
