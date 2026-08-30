---
name: sdd-validator
description: Build and compliance validator for SDD Kit. This is a SKILL (invoke via Skill tool, NOT Task/subagent). Distinct from sdd-validator-runner agent. Use for build validation, test execution, code compliance checks, and coverage analysis during /sdd.build and /sdd.finish. **TRIGGER ON** build validation, test execution, coverage, code compliance, CI pipeline.
---

# SDD Validator

> **SKILL**: Run mechanical validation tasks - build, tests, coverage, code compliance. Invoke with `Skill("sdd-validator")`. Do NOT confuse with the `sdd-validator-runner` subagent.

---

## When to Use

1. **After Task Implementation** (`/sdd.build`) - Build and test
2. **Final Validation** (`/sdd.build` end) - Full validation suite
3. **Before Archive** (`/sdd.finish`) - Compliance verification

---

## Validation Steps

### 0. Platform Detection (FIRST — gates all subsequent steps)

```bash
# Use detect-stack.sh — mobile detection runs before language detection
stack_result=$(bash ~/.sdd-kit/tools/detection/detect-stack.sh . --json 2>/dev/null)
platform=$(echo "$stack_result" | grep -o '"platform":"[^"]*"' | cut -d'"' -f4)
```

**Mobile projects (`platform = android | ios`) skip code compliance entirely:**

| Check | Android | iOS | Backend/Web |
|-------|---------|-----|-------------|
| code compliance (Dockerfile, /ping) | ❌ Skip | ❌ Skip | ✅ Run |
| CI Pipeline (RP MCP) | ❌ Skip | ❌ Skip | ✅ Run |
| Mobile build validation | ✅ Run | ✅ Run | ❌ Skip |
| Andes/Everest compliance | ✅ Run | ✅ Run | ❌ Skip |

**Mobile build commands:**

| Platform | Build | Test |
|----------|-------|------|
| Android | `./gradlew assembleDebug` | `./gradlew test` |
| iOS | `xcodebuild build -workspace *.xcworkspace -scheme <scheme> -destination 'platform=iOS Simulator,name=iPhone 16'` | `xcodebuild test -workspace *.xcworkspace -scheme <scheme> -destination 'platform=iOS Simulator,name=iPhone 16'` |

**Mobile Andes/Everest compliance scan (replace code compliance for mobile):**

```bash
# Android — verify no banned libraries
grep -rn "import com.google.android.material\." app/src/main/ --include="*.kt" && echo "❌ Material components found — use Andes only" || echo "✅ No Material components"
grep -rn "import coil\.\|import com.bumptech.glide\.\|import com.squareup.picasso\." app/src/main/ --include="*.kt" && echo "❌ Banned image lib — use EverestCanvas" || echo "✅ No banned image libs"
grep -rn "import dagger\.\|import org.koin\.\|import com.google.dagger\." app/src/main/ --include="*.kt" && echo "❌ Banned DI lib — use CrabDI" || echo "✅ No banned DI libs"

# iOS — verify no banned libraries
grep -rn "^import Kingfisher\|^import SDWebImage\|^import Nuke\|^import PinRemoteImage" . --include="*.swift" && echo "❌ Banned image lib — use EverestCanvas" || echo "✅ No banned image libs"
grep -rn "^import Swinject\|^import Resolver" . --include="*.swift" && echo "❌ Banned DI lib — use CrabDI" || echo "✅ No banned DI libs"
grep -rn "URLSession.shared" . --include="*.swift" && echo "⚠️ Direct URLSession — consider MLNetworking (Everest)" || echo "✅ No direct URLSession"
```

**Mobile validation output format:**

```markdown
## Mobile Validation Results — Android | iOS

### Build
- [x] `./gradlew assembleDebug` — PASSED

### Tests
- [x] `./gradlew test` — 42/42 passing

### Andes/Everest Compliance
- [x] No Material components (use Andes)
- [x] No banned image libs (use EverestCanvas)
- [x] No banned DI libs (use CrabDI)

### Verdict: PASSED ✅
```

> **After mobile validation passes** → proceed directly to spec sync and archive. No  steps.

---

### 1. Technology Detection (backend/web only)

> **SKIP if `platform = android | ios`** — mobile detection already handled in Step 0.

Detect stack by checking for:

| File | Technology | Build | Test |
|------|------------|-------|------|
| `pom.xml` | Java (Maven) | `mvn compile` | `mvn test` |
| `build.gradle` | Java (Gradle) | `./gradlew build` | `./gradlew test` |
| `package.json` | Node.js | `npm run build` | `npm test` |
| `go.mod` | Go | `go build ./...` | `go test -race ./...` |
| `pyproject.toml` | Python | `pip install -e .` | `pytest` |
| `Cargo.toml` | Rust | `cargo build` | `cargo test` |

### CI Pipeline Validation (Release Process MCP)

> Build, tests, coverage, and CI are validated by the Release Process MCP local CI pipeline.
> This runs as build.md Step 6D. The sdd-validator skill no longer runs these checks independently.

The RP MCP pipeline covers: compilation, test execution, coverage analysis (>=80%), dependency scan, and static analysis.

See `build.md` Step 6D for invocation details.

### 4.  Compliance Checks

| Check | How to Verify | Expected |
|-------|---------------|----------|
| Dockerfile | `ls Dockerfile` | Exists, uses `your-registry/base-image |
| Dockerfile.runtime | `ls Dockerfile.runtime` | Exists, uses `your-registry/base-image |
| /ping endpoint | `grep -r "/ping"` | Implemented |
| Health check | `grep -r "health"` | Configured |
| .fury file | `ls .fury` | Exists (read-only) |

**Dockerfile Validation**:
```bash
# Check Dockerfile uses authorized image
grep -E "^FROM\s+hub\.furycloud\.io/mercadolibre/" Dockerfile || echo "INVALID"
grep -E "^FROM\s+hub\.furycloud\.io/mercadolibre/" Dockerfile.runtime || echo "INVALID"
```

**Authorized Images**:
| Technology | Build Image | Runtime Image |
|------------|-------------|---------------|
| Java 21 | `your-registry/base-image | `your-registry/base-image |
| Node.js 24 | `your-registry/base-image | `your-registry/base-image |
| Go 1.25 | `your-registry/base-image | `your-registry/base-image |
| Python 3.13 | `your-registry/base-image | `your-registry/base-image |

### 5. Dependency Validation (Tech-Specific)

**Java (Maven)**:
```bash
# Check Nexus repository is configured
grep -q "maven.artifacts.furycloud.io" pom.xml || echo "MISSING:  Nexus repository"

# Check melitk-bom version
grep -q "java-melitk-bom" pom.xml && grep -q "2.2.1" pom.xml || echo "INVALID: java-melitk-bom must be 2.2.1"
```

**Node.js**:
```bash
# Check for  registry in .npmrc
grep -q "registry.fury" .npmrc 2>/dev/null || echo "CHECK: .npmrc for @meli/* packages"
```

---

## Output Format

```markdown
## Validation Results

### Summary
| Check | Status | Details |
|-------|--------|---------|
| CI Pipeline (RP MCP) | PASSED/FAILED | build, test, coverage, deps, SCA |
|  Compliance | PASSED/FAILED | X/5 checks |

###  Compliance
- [x] Dockerfile exists (your-registry/base-image
- [x] Dockerfile.runtime exists (your-registry/base-image
- [ ] /ping endpoint - MISSING
- [x] Health check configured
- [x] .fury file present

### Verdict
[ ] PASSED - All validations passed
[ ] WARNING - Non-critical issues found
[ ] FAILED - Critical issues must be fixed
```

---

## Important Rules

1. **Be Fast**: Run only necessary checks
2. **Be Clear**: Report pass/fail unambiguously
3. **Be Specific**: Show exact errors and locations
4. **No False Positives**: Verify findings before reporting
5. **CI Pipeline (RP MCP) is MANDATORY**: Cannot skip for /sdd.finish
6. **Iterate until passing**: Pipeline failures must be fixed

---

## Go-Specific Requirements

> **CRITICAL**: Go projects require race detection for CI/CD parity.

### Race Detection is MANDATORY

| Requirement | Command | Rationale |
|-------------|---------|-----------|
| Tests | `go test -race ./...` | Detects data races that cause flaky tests and production bugs |
| Coverage | `go test -race -cover ./...` | Race detection during coverage analysis |

**Why?** CI/CD pipelines run with `-race` flag. Tests that pass locally without `-race` may fail in CI/CD when races are detected.

### Coverage Must Be Measurable

Coverage validation is **BLOCKING** (not a warning):
- If coverage cannot be determined, validation **FAILS**
- Coverage must be >= 80% threshold
- Use `-coverprofile=coverage.out` for reliable measurement

### Common Go Concurrency Issues Caught

| Issue | Symptom | Fix |
|-------|---------|-----|
| Map write race | `concurrent map writes` | Use `sync.Map` or `sync.Mutex` |
| Shared variable | `DATA RACE` on variable | Use channels or mutexes |
| Goroutine leak | Memory growth over time | Use `context.Context` for cancellation |

---

## Layer Execution Guide

Tasks execute in layers. This validator runs at specific layers:

### Layer 1: Local (Code Implementation)

| Step | Validator Role |
|------|----------------|
| Write code | None (implementation) |
| Build check | **Run build validation** |
| Unit tests | **Run test execution** |

### Layer 2:  Platform

| Step | Validator Role |
|------|----------------|
| code compliance | **Run code compliance checks** |
| CI Pipeline | **Run CI via Release Process MCP** |
| Dependencies | **Run dependency validation** |

### Layer 3: Quality Gates

| Step | Validator Role | Subagent |
|------|----------------|----------|
| Performance | Report only | `sdd-performance-expert` |
| Security | Report only | `sdd-code-reviewer` |
| Code Review | Report only | `sdd-code-reviewer` |

**Note**: Layer 3 uses specialized subagents. This validator only reports if they were invoked.

---

## Quality Gates Workflow

Quality gates are **MANDATORY** after each task implementation:

### Gate Execution Order

```
Per-Task Completion:
1. sdd-validator → Build + Tests (Layer 1-2)
2. sdd-performance-expert → Performance check (Layer 3)
3. sdd-code-reviewer → Security check (Layer 3)
4. sdd-code-reviewer → Code review (Layer 3)
```

### Quality Gate Status

| Gate | Pass Criteria | Blocking? |
|------|---------------|-----------|
| CI Pipeline (RP MCP) | Build, tests, coverage (>=80%), deps, SCA pass | YES |
| Performance | No critical findings | YES |
| Security | No HIGH/CRITICAL vulns | YES |
| Code Review | No blocking comments | NO |

### Reporting Quality Gates

After all gates run, report:

```markdown
## Quality Gates Summary

| Gate | Status | Details |
|------|--------|---------|
| Build | PASSED | Clean compile |
| Tests | PASSED | 15/15 passing |
| Coverage | WARNING | 72% (threshold: 80%) |
| CI Pipeline (RP MCP) | PASSED | CI simulation OK |
| Performance | PASSED | No N+1 queries found |
| Security | PASSED | No vulnerabilities |
| Code Review | PASSED | 2 suggestions (non-blocking) |

**Verdict**: PASSED (1 warning)
```

---

## Validation Timing

| Command | When to Validate |
|---------|------------------|
| `/sdd.build` per-task | After each task implementation |
| `/sdd.build` end | Full validation suite |
| `/sdd.finish` | Final compliance verification |

---

## Subagent Integration

This validator coordinates with specialized subagents:

| Subagent | Purpose | Invocation |
|----------|---------|------------|
| `sdd-validator-runner` | Isolated validation (no bias) | Heavy validation |
| `sdd-performance-expert` | N+1 queries, memory leaks | Layer 3 |
| `sdd-code-reviewer` | OWASP, injections | Layer 3 |
| `sdd-code-reviewer` | Code quality | Layer 3 |

**When to delegate to sdd-validator-runner**:
- Context > 60% (DELEGATE_MODE)
- Need unbiased validation (didn't write the code)
- Full validation suite

---

## Relationship with sdd-validator-runner Agent

> **Architecture Pattern**: Skill = Coordinator/Documentation, Agent = Executor

```
sdd-validator (SKILL) = Coordinator/Documentation
  └── Delegates to sdd-validator-runner (AGENT) = Executor
```

### When to Use Which

| Use Case | Use This | Why |
|----------|----------|-----|
| Quick inline build check | **sdd-validator** (skill) | Low context overhead, simple validation |
| Context > 60% | **sdd-validator-runner** (agent) | Isolated context preserves main context |
| Unbiased validation needed | **sdd-validator-runner** (agent) | No knowledge of implementation decisions |
| Full validation suite | **sdd-validator-runner** (agent) | Consolidates all checks in isolated context |
| Layer 3 quality gates | **sdd-validator-runner** (agent) | Performance + security + code-review in one pass |
| Documentation lookup | **sdd-validator** (skill) | Reference for validation rules |

### Invocation Patterns

**From /sdd.build - Simple Validation**:
```python
# Use skill for quick inline check
Skill("sdd-validator")  # ~500 tokens in main context
```

**From /sdd.build - Full Validation with Quality Gates**:
```python
# Delegate to agent for isolated context
Task(
    subagent_type="sdd-validator-runner",
    prompt="""
    Validate files: [list]
    Run Layer 3 quality gates: performance, security, code-review
    Return unified JSON verdict.
    """
)
# Returns: ~300 token verdict JSON, saves ~5700 tokens vs inline skills
```

### Token Savings

| Approach | Token Cost | When |
|----------|------------|------|
| Skill inline | ~500-2000 tokens | Quick checks |
| Agent (build+tests) | ~300 token result | Standard validation |
| Agent (+ quality gates) | ~300 token result | Saves ~5700 tokens vs 3 inline skills |

**Recommendation**: For Layer 3 quality gates, ALWAYS delegate to `sdd-validator-runner` with quality gate prompts to preserve main context
