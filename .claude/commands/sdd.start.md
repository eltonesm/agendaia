---
name: sdd.start
description: Initialize new feature in SDD Kit framework. Use when user wants to begin a new feature, set up the sdd/wip/ directory structure, and configure project metadata. Supports --reopen for archived features.
model: sonnet
argument-hint: "[feature-description] [--express|--lite|--audio|--from-backlog|--reopen]"
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

# Command: /sdd.start

**Description**: Initialize new feature in SDD Kit framework

**Usage**:
- `/sdd.start "feature-description"` → Standard mode (default)
- `/sdd.start "feature-description" --express` → Express mode (minimal interaction)

- `/sdd.start "feature-description" --lite` → Lite template (~80 lines, combined spec)
- `/sdd.start --audio` → Record feature description via microphone
- `/sdd.start --from-backlog <ID>` → Create from backlog item
- `/sdd.start --rename [new-name]` → Rename current feature
- `/sdd.start --reopen [feature-name]` → Reopen completed feature for iteration
- `/sdd.start --reopen [feature-name] --phase N` → Reopen to specific phase

---

## Quick Help

> `/sdd.start help` → Shows this summary

**Syntax**: `/sdd.start [feature-description] [flags]`

| Argument | Description |
|----------|-------------|
| `feature-description` | Brief description of what you want to build (in natural language) |

| Flag | Description |
|------|-------------|
| (none) | Standard mode (confirmations at key points) |
| `--express` | Express mode (minimal interaction) |

| `--lite` | Lite template (~80 lines, combined spec) |
| `--audio` | Record feature description via microphone |
| `--from-backlog <ID>` | Create feature from backlog item |
| `--rename [new-name]` | Rename current feature (updates folder and meta.md) |
| `--reopen [name]` | Reopen a completed feature from `sdd/features/` back to WIP |
| `--phase N` | (with `--reopen`) Target phase: 1=Functional, 2=Technical, 3=Tasks, 4=Implementation |

**Examples**:
```bash
# Describe what you want to build (RECOMMENDED)
/sdd.start "user authentication with OAuth"
/sdd.start "payment retry mechanism for failed transactions"
/sdd.start "inventory sync from external API"

# Record voice description
/sdd.start --audio

# Or use a short feature name
/sdd.start "payment-gateway"
/sdd.start "user-auth" --express
/sdd.start "cache-layer" --lite

# Rename an existing feature
/sdd.start --rename "oauth-login"

# Reopen a completed feature for iteration
/sdd.start --reopen user-auth              # By name
/sdd.start --reopen 20260120-user-auth --phase 2  # By full name, direct to technical spec phase
```

**What happens**:
1. Agent infers a kebab-case feature name from your description
2. Detects your application name (from `.fury` file or folder)
3. Creates `sdd/wip/[YYYYMMDD-feature-name]/` directory structure
4. Initializes `meta.md` with feature metadata

**See also**: `/sdd.help start` for detailed documentation

---

CRITICAL: USER INTERACTION RULES
When this skill shows JSON for AskUserQuestion, you MUST:
  1. CALL the AskUserQuestion TOOL with that exact JSON
  2. DO NOT print options using Bash (no echo, cat, printf)
  3. DO NOT ask "Which option?" as text
  4. Tables marked "REFERENCE ONLY" are for docs - do NOT print


> **Application Name** vs **Feature Name**:
> - **Application**: The app you're working on (e.g., `items-api`, `payments-core`)
> - **Feature**: What you're building within that app (e.g., `user-auth`, `payment-retry`)

---

## Modes

| Mode | Flag | Behavior |
|------|------|----------|
| **Express** | `--express` | Minimal interaction, auto-advances |
| **Standard** | (default) | Confirmations at key points |


## Templates

| Template | Flag | Lines | Use For |
|----------|------|-------|---------|
| **Full** | (default) | ~1,100 | Production, compliance-heavy |
| **Lite** | `--lite` | ~80 | MVPs, prototypes, internal tools |

---

## Workflow (Steps in Order)

### Step 0: User Profile Check (BLOCKING - NEVER SKIP)

> **⛔ This step runs BEFORE anything else. No profile = must ask.**

```bash
profile_file="$HOMEsdd-kit/framework/user-profile.yaml"
if [ -f "$profile_file" ]; then
    profile=$(grep "^profile:" "$profile_file" | cut -d: -f2 | tr -d ' ')
    echo "✓ Using saved profile: $profile"
else
    # ⛔ MANDATORY: Ask user for profile. Do NOT continue without it.
    # → Go to "First-time Profile Selection" in Step 5.5 below
    # → Save result to $profile_file BEFORE proceeding
fi
```

If no profile file exists, you MUST:
1. Display the profile options (Business/Product vs Technical) using AskUserQuestion
2. If Technical: ask Plan Mode preferences
3. Save `sdd-kit/framework/user-profile.yaml`
4. Only then continue to Step 1

### Step 1: Validate Input (BLOCKING)

**Must pass before ANY file creation:**

1. **Detect input type**:
   - Valid name? (3-100 chars, kebab-case) → Continue
   - Looks like prompt? (>5 words, sentences) → Convert to name automatically, proceed
     - Show: "✓ Feature name inferred: `{suggested-name}` (from your description)"
     - Store original description for use as initial context in /sdd.spec
     - Do NOT ask for confirmation, continue to Step 2
   - Invalid format? → Reject, ask for correction

2. **Check uniqueness**: Feature must not exist in `sdd/wip/`

### Step 2: Platform Detection + Frontend Skills Check

> **Run these two bash commands sequentially. Check the output of each before continuing.**

**Step 2a** — detect platform:
```bash
stack_result=$(bash sdd-kit/framework/tools/detection/detect-stack.sh . --json 2>/dev/null)
platform=$(echo "$stack_result" | grep -o '"platform":[^,}]*' | grep -o '"[^"]*"$' | tr -d '"')
([ "$platform" = "android" ] || [ "$platform" = "ios" ]) && IS_MOBILE=true || IS_MOBILE=false
echo "platform=$platform IS_MOBILE=$IS_MOBILE"
```

**Step 2b** — validate frontend skill (run this independently):
```bash
bash sdd-kit/framework/tools/shared/check-frontend-skill.sh "$(pwd)" "$stack_result"
```
> If `$stack_result` is not available in this shell, run: `bash sdd-kit/framework/tools/shared/check-frontend-skill.sh "$(pwd)"` instead (the script will re-run detect-stack internally).

> **If Step 2b output contains `❌`, STOP. Do not proceed.**

---

### Step 2.5:  Prerequisites Validation (Deterministic) ⭐ v2.8.0

> **SKIP if `IS_MOBILE=true`** — Mobile apps don't use  CLI, VPN, or Zero Trust.

```bash
if [ "$IS_MOBILE" = false ]; then
    # Run prerequisites check (FIRST - verify  CLI is ready)
    prereq_result=$(bash sdd-kit/framework/tools/shared/fury-prereqs.sh --json)
    fury_ready=$(echo "$prereq_result" | grep -o '"fury_ready":[^,}]*' | cut -d: -f2)
    needs_login=$(echo "$prereq_result" | grep -o '"needs_login":[^,}]*' | cut -d: -f2)

    if [ "$fury_ready" != "true" ]; then
        echo "❌  prerequisites not met:"
        echo "$prereq_result" | grep -o '"issues":\[[^]]*\]'

        if [ "$needs_login" = "true" ]; then
            echo "Running: "
            
        fi
    fi
fi
```

**Prerequisites checked** (backend/web only):
-  CLI installed and accessible
- Zero Trust token valid (not expired)
- Git configured with user.name and user.email

### Step 2.7:  App Verification

**Check `.fury` file existence:**

| Scenario | Action |
|----------|--------|
| `.fury` exists | Extract app name, continue |
| `.fury` missing | Auto-detect from folder: `app_name=$(basename $(pwd))`, verify with  |
| `.fury` missing + folder name not found | Ask user for app name (AskUserQuestion) |

**Auto-detect flow** (when `.fury` is missing):
1. `app_name=$(basename $(pwd))` — derive from current folder name
2. Verify with  — if the app exists in , proceed silently
3. Only fall through to AskUserQuestion if  doesn't find the app

**If app not found** (use AskUserQuestion):
1. Create new app
2. Correct the name
3. Continue in local validation mode (Not Recommended)

**If user chooses "Create new app"** → ask app name with AskUserQuestion:
- **Option 1 MUST be the folder name** (`basename $(pwd)`) — this is the most likely intended name
- Options 2-3: alternative suggestions based on feature description (shorter/clearer variants)
- Option 4: Type custom name

**If app EXISTS in ** (but `.fury` missing locally):
- Proceed to clone repository
- `app_created_via_puma=false` (we didn't create it)
- Technology will be detected from project files (see section 3.5)
- Branch check determines if freshly scaffolded

**If creating app**:
- Ask project type first (Prototype/MVP/Production)
- Get technologies and app types via `mcp____technologies()` and `mcp____application_types()`
- Auto-discover project_code via TeamsMCP (see algorithm below)
- Create via `mcp____create_application()`
- **⚠️ CRITICAL: Set `app_created_via_puma=true` and remember selected `technology`**
- These values are REQUIRED in Step 3 to use ``

#### Auto-discover project_code via TeamsMCP

```bash
# 1. Get username from home folder (auto-detected, no confirmation needed)
username=$(basename $HOME)

# 2. Call TeamsMCP with auto-detected username
result=$(mcp__TeamsMCP__get_user_collaborations(username="$username"))

# 3. IF TeamsMCP returns error OR no teams found:
#    - Ask user: "Could not find teams for '[username]'. Enter your  username:"
#    - Retry with user-provided username
if [ -z "$result" ] || [ "$(echo "$result" | jq '.total_teams')" = "0" ]; then
    # Use AskUserQuestion to get correct username
    # "Could not find teams for '$username'. Enter your  username:"
    # Then retry: mcp__TeamsMCP__get_user_collaborations(username="<user-provided>")
fi

# 4. Extract and deduplicate project codes from response
#    - Collect all unique project_code values across all teams
#    - Sort alphabetically

# 4.5 MANDATORY: Display projects table BEFORE asking
#     Show ALL projects the user has access to
echo ""
echo "📋 Available Projects:"
echo "┌─────────────────────┬──────────────────────────────────────────┐"
echo "│ Project Code        │ Description                              │"
echo "├─────────────────────┼──────────────────────────────────────────┤"
for project in $projects; do
    printf "│ %-19s │ %-40s │\n" "$project_code" "$project_name"
done
echo "└─────────────────────┴──────────────────────────────────────────┘"
echo ""

# 5. Present projects to user:
#    ⚠️ CRITICAL: The projects table MUST be displayed BEFORE asking
#    ⚠️ NOTE: The folder name is the APP name, NOT the project code.
#            Do NOT offer folder name as a project option.

#    AskUserQuestion options (in order):
#    1. project-from-teams-1 (most common team's project)
#    2. project-from-teams-2
#    3. project-from-teams-3
#    4. project-from-teams-4
#    (Other: type custom project code)

# 6. If no projects found, inform user:
#    "No projects found. You may be new or removed from teams."
#    Ask user to type their project code manually
```

#### System Selection (Required for web/fda/mcp apps)

> **APPLIES TO**: Application types `web`, `fda`, `mcp`, `edge-proxy`
> **SKIP FOR**: `library`, `mobile-application`, and other types

**When creating an app that requires system**:

1. **Call  to get systems**:
   ```bash
   systems=$(mcp____list_systems())
   ```

2. **MANDATORY: Display systems table BEFORE asking**:
   ```
   📋 Available Systems:
   ┌─────┬──────────────────────────┬─────────────────────────────────────────────┐
   │ ID  │ System Name              │ Responsibility                              │
   ├─────┼──────────────────────────┼─────────────────────────────────────────────┤
   │ 322 │  Library             │ Documentación de aplicaciones.              │
   │ 331 │ Documentation            │ Documentation platform.                     │
   │ 659 │ Tiendas Oficiales        │ Tiendas Oficiales                           │
   │ ... │ ...                      │ ...                                         │
   └─────┴──────────────────────────┴─────────────────────────────────────────────┘
   ```

3. **Then ask user with AskUserQuestion**:
   - Question: "Which system should this application belong to?"
   - Options: Top 3-4 systems from the list (sorted by relevance or alphabetically)
   - Include: "(Other: type system name or ID)"

4. **Pass system_id to create_application**:
   ```bash
   mcp____create_application(
       name="...",
       system_id=$selected_system_id,
       ...
   )
   ```

### Step 3: Clone/Scaffold Repository

#### 3.1 Determine Template (for new apps)

```bash
case "$technology" in
    java*)   template="web-maven-spring" ;;
    go*)     template="web" ;;
    node*|typescript*) template="web" ;;
    python*) template="web" ;;
    *)       template="web" ;;
esac
```

#### 3.2 Run fury get (CRITICAL: Template Detection)

> **⚠️ CRITICAL**: If app was just created via  in Step 2, you MUST use `--template`.
> Without `--template`, the repo will be EMPTY (no Dockerfile, no code, nothing).

**How to know if app was created via Puma:**
- You called `mcp____create_application()` in Step 2
- Set a flag: `app_created_via_puma=true`
- The technology was selected during app creation

```bash
# ⚠️ CRITICAL DECISION POINT
if [ "$app_created_via_puma" = true ]; then
    # ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    # NEW APP: MUST use --template (repo is EMPTY without it)
    # ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    fury get <app-name> --template "$template" 2>&1

    # This creates: Dockerfile, pom.xml/go.mod, PingController, etc.
else
    # ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    # EXISTING APP: just clone (already has code)
    # ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    fury get <app-name> 2>&1
fi
```

**NEVER do this for new apps:**
```bash
# ❌ WRONG - repo will be empty!
fury get <new-app-name>

# ✅ CORRECT - scaffolding applied
fury get <new-app-name> --template web-maven-spring
```

**Handle auth error**: If "Zero Trust token expired" → run ``, retry.

#### 3.3 Handle Race Condition (CRITICAL)

> Repository creation takes 15-30 seconds after app creation.
> **ALWAYS wait before the first attempt** to avoid a guaranteed failure.

```bash
# MANDATORY: Wait 15 seconds before first fury get attempt
echo "⏳ Waiting 15s for repository creation..."
sleep 15

max_retries=20      # 20 attempts
retry_interval=30   # 30 seconds between retries
# Total max wait: ~10 minutes

for i in $(seq 1 $max_retries); do
    output=$(fury get <app-name> --template "$template" 2>&1)

    if [ -d "<app-name>" ]; then
        echo "✅ Repository cloned successfully"
        break
    fi

    if echo "$output" | grep -qi "not found\|does not exist"; then
        echo "⏳ Repository not available yet. Retrying in ${retry_interval}s... (attempt $i/$max_retries)"
        sleep $retry_interval
    fi
done

# CRITICAL: Verify scaffolding succeeded
if [ ! -d "<app-name>" ]; then
    echo "❌ CRITICAL: Could not clone repository after 10 minutes."
    exit 1
fi
```

#### 3.4 Move Contents to Current Directory

```bash
shopt -s dotglob  # Include hidden files
mv ./<app-name>/* ./
rmdir ./<app-name>
```

#### 3.5 Detect Scaffolding Status

> **PURPOSE**: Detect if app was created externally but is freshly scaffolded.
> **IMPORTANT**: If `sdd/specs` already exists, it's brownfield - NEVER cleanup.

After moving contents, use the `detect-scaffolding-status.sh` script:

```bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../tools" && pwd)"
result=$("$SCRIPT_DIR/detect-scaffolding-status.sh" "." --json)

# Parse JSON result
project_mode=$(echo "$result" | grep -o '"project_mode":"[^"]*"' | cut -d'"' -f4)
freshly_scaffolded=$(echo "$result" | grep -o '"freshly_scaffolded":[^,}]*' | cut -d: -f2)
detected_tech=$(echo "$result" | grep -o '"technology":"[^"]*"' | cut -d'"' -f4)
reason=$(echo "$result" | grep -o '"reason":"[^"]*"' | cut -d'"' -f4)

# Use detected technology if not already set
if [ -z "$technology" ] && [ -n "$detected_tech" ] && [ "$detected_tech" != "unknown" ]; then
    technology="$detected_tech"
    echo "   → Detected technology: $technology"
fi

echo "🔍 Scaffolding Status: $project_mode (freshly_scaffolded=$freshly_scaffolded)"
echo "   Reason: $reason"
```

#### 3.5.1 Detect Full Stack (Deterministic)

Use script for comprehensive stack detection - Saves ~2,000-3,000 tokens vs LLM inference.

```bash
# Detect full technology stack (language, framework, database,  services, platform)
# NOTE: if IS_MOBILE=true, stack_result was already fetched in Step 2 — reuse it
[ -z "$stack_result" ] && stack_result=$(bash sdd-kit/framework/tools/detection/detect-stack.sh . --json)

# Parse JSON result
language=$(echo "$stack_result" | grep -o '"language":"[^"]*"' | cut -d'"' -f4)
build_tool=$(echo "$stack_result" | grep -o '"buildTool":"[^"]*"' | cut -d'"' -f4)
framework=$(echo "$stack_result" | grep -o '"framework":"[^"]*"' | cut -d'"' -f4)
database=$(echo "$stack_result" | grep -o '"database":"[^"]*"' | cut -d'"' -f4)
fury_services=$(echo "$stack_result" | grep -o '"furyServices":\[[^]]*\]')
# platform already set in Step 2; fallback extraction if needed
[ -z "$platform" ] && platform=$(echo "$stack_result" | grep -o '"platform":"[^"]*"' | cut -d'"' -f4)

echo "📊 Stack Detection:"
echo "   Language: $language"
echo "   Platform: ${platform:-backend}"
echo "   Build Tool: $build_tool"
echo "   Framework: ${framework:-none}"
echo "   Database: ${database:-none}"
echo "    Services: $fury_services"
```

**Use stack info in meta.md**:
- Pre-populate `technology:` field
- **Pre-populate `platform:` field** → `android | ios | backend | web` (from detect-stack.sh)
- Set appropriate build/test commands
- Identify  services to configure (skip for mobile)

**Logic**:
| Scenario | `sdd/specs` | Branch | `freshly_scaffolded` | Action |
|----------|--------------|--------|----------------------|--------|
| Created via MCP | No | `feature/initial-scaffolding` | `true` | Cleanup + greenfield |
| Created externally (web UI) | No | `feature/initial-scaffolding` | `true` | Cleanup + greenfield |
| Brownfield with SDD history | Yes | any | `false` | **No cleanup** + brownfield |
| Existing app with commits | No | `main`/`master`/other | `false` | No cleanup + brownfield check |

#### 3.6 Cleanup Scaffolding Examples (CONDITIONAL)

> **WHEN TO RUN**: If `app_created_via_puma=true` OR `freshly_scaffolded=true`

```bash
if [ "$app_created_via_puma" = true ] || [ "$freshly_scaffolded" = true ]; then
    echo "🧹 Running scaffolding cleanup..."

    case "$technology" in
        java*)
            # Remove Java sample files
            rm -rf src/main/java/com/mercadolibre/*/beans/ 2>/dev/null
            rm -rf src/main/java/com/mercadolibre/*/dtos/ 2>/dev/null
            rm -rf src/test/java/com/mercadolibre/*/unit/beans/ 2>/dev/null
            ;;
        kotlin*)
            # Remove Kotlin sample files
            rm -rf src/main/kotlin/com/mercadolibre/*/beans/ 2>/dev/null
            rm -rf src/main/kotlin/com/mercadolibre/*/dtos/ 2>/dev/null
            rm -rf src/test/kotlin/com/mercadolibre/*/unit/beans/ 2>/dev/null
            ;;
        go*)
            rm -f telemetry/example_test.go 2>/dev/null
            ;;
        python*)
            rm -rf app/dummy/ 2>/dev/null
            ;;
        node*|typescript*)
            rm -rf src/routes/example*.ts src/controllers/example*.ts 2>/dev/null
            ;;
    esac

    # Commit cleanup changes
    git add -A
    git commit -m "chore: cleanup scaffolding samples"

    echo "✅ Cleanup complete"
    echo "   Preserved: Dockerfile, pom.xml/go.mod, .fury, PingController"
    echo "   Deleted: example/sample files"
else
    echo "ℹ️  Skipping cleanup (existing app with code)"
fi
```

#### 3.7 Verify Essential Files

```bash
# Mobile projects don't have Dockerfile or .fury — skip  file checks
if [ "$IS_MOBILE" = true ]; then
    echo "📱 Mobile project — skipping  file checks"
    case "$platform" in
        android)
            required_files=("app/src/main/AndroidManifest.xml" "build.gradle.kts")
            for file in "${required_files[@]}"; do
                [ ! -e "$file" ] && echo "⚠️ Missing: $file"
            done
            ;;
        ios)
            # xcodeproj/xcworkspace are directories with variable names — must use ls, not
            # array glob literal ([ ! -e "*.xcodeproj" ] is never true as written)
            ls -d *.xcodeproj 2>/dev/null | head -1 | grep -q . || echo "⚠️ Missing: *.xcodeproj (Xcode project directory)"
            ls -d *.xcworkspace 2>/dev/null | head -1 | grep -q . || echo "⚠️ Missing: *.xcworkspace (Xcode workspace directory)"
            ;;
    esac
else
    case "$technology" in
        java*)   required_files=("pom.xml" "Dockerfile" ".fury") ;;
        go*)     required_files=("go.mod" "Dockerfile" ".fury") ;;
        python*) required_files=("pyproject.toml" "Dockerfile" ".fury") ;;
        node*)   required_files=("package.json" "Dockerfile" ".fury") ;;
    esac
    for file in "${required_files[@]}"; do
        [ ! -e "$file" ] && echo "⚠️ Missing: $file"
    done
fi
```

### Step 4: Detect Project Mode

| Origin | project_mode |
|--------|--------------|
|  create_application | `greenfield` |
| fury get + branch `feature/initial-scaffolding` | `greenfield` |
| fury get (existing repo with code) | Check content |

**Detection logic**:

> **NOTE**: `sdd/specs` was already verified in "Detect Scaffolding Status" (3.5).
> If it existed → `freshly_scaffolded=false` and NO cleanup was performed.

```bash
if [ "$app_created_via_puma" = true ]; then
    project_mode="greenfield"
    echo "🆕 App created via Puma → Greenfield mode"
elif [ "$freshly_scaffolded" = true ]; then
    project_mode="greenfield"
    echo "🆕 Freshly scaffolded (external creation) → Greenfield mode"
elif [ -d "sdd/specs" ] || [ -d "sdd/features" ]; then
    # Este caso: freshly_scaffolded=false porque sdd/specs existía
    project_mode="brownfield"
    echo "🔍 Existing specs found → Brownfield mode"
else
    if has_implementation_code; then
        project_mode="brownfield"
        echo "🔍 Existing code found → Brownfield mode"
    else
        project_mode="greenfield"
        echo "🆕 Empty project → Greenfield mode"
    fi
fi
```

#### Step 4.2: Brownfield Without Specs Warning

> **INFO**: Non-blocking recommendation for better results.

When brownfield is detected WITHOUT existing specs:

```bash
if [ "$project_mode" = "brownfield" ] && [ ! -d "sdd/specs" ] && [ ! -d "sdd/extracted" ]; then
    # Show informative warning
fi
```

**Display to user**:

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ⚠️  BROWNFIELD DETECTED WITHOUT SPECS                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  This repository has existing code but no generated specs.               │
│                                                                          │
│  For better results, consider running:                                   │
│                                                                          │
│    /sdd.reverse-eng                                                     │
│                                                                          │
│  This will:                                                              │
│  • Extract specs from existing code                                      │
│  • Document current architecture                                         │
│  • Identify patterns and conventions                                     │
│  • Help avoid conflicts with new features                                │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

**⛔ INVOKE TOOL (do not print this, CALL the tool)**:

```
AskUserQuestion(
  questions=[{
    "question": "How would you like to proceed?",
    "header": "Setup",
    "options": [
      {"label": "Run /sdd.reverse-eng first (Recommended)", "description": "Generate specs from existing code before starting"},
      {"label": "Continue without specs", "description": "Feature will work but with less context"},
      {"label": "What does reverse-eng do?", "description": "Show brief explanation"}
    ],
    "multiSelect": false
  }]
)
```

**Behavior by choice**:

| Choice | Action |
|--------|--------|
| Run reverse-eng first | Exit `/sdd.start`, show: "Run `/sdd.reverse-eng` then retry `/sdd.start`" |
| Continue without specs | Proceed to Step 4.1, add warning to meta.md |
| What does it do? | Explain benefits, re-ask question |

**If user chooses "Continue without specs"**, add to meta.md:

```yaml
brownfield_context:
  has_specs: false
  warning_acknowledged: true
  recommendation: "Consider running /sdd.reverse-eng for better context"
```

#### Step 4.1: Analyze Existing Structure (Brownfield Only)

> **FOR BROWNFIELD**: Analyze existing codebase structure to understand patterns.

```bash
if [ "$project_mode" = "brownfield" ]; then
    echo "🔍 Analyzing existing codebase structure..."

    # Run structure analysis
    structure_result=$(bash sdd-kit/framework/tools/extraction/analyze-structure.sh . --json)

    # Extract key information
    entry_points=$(echo "$structure_result" | grep -o '"entry_points":\[[^]]*\]')
    patterns=$(echo "$structure_result" | grep -o '"patterns":\[[^]]*\]')
    dependencies=$(echo "$structure_result" | grep -o '"external_dependencies":\[[^]]*\]')
    test_patterns=$(echo "$structure_result" | grep -o '"test_patterns":\[[^]]*\]')

    echo "📊 Structure Analysis:"
    echo "   Entry points: $entry_points"
    echo "   Patterns detected: $patterns"
    echo "   External dependencies: $dependencies"
    echo "   Test patterns: $test_patterns"

    # Store in meta.md for use in /sdd.spec
    # This provides context about existing code patterns
fi
```

**Structure analysis provides**:
- Entry points (main classes, handlers, routes)
- Code patterns (MVC, layered, hexagonal)
- External service dependencies
- Test organization patterns
- Package/module structure

### Step 5: Project Type Selection (MOVED UP)

> **SKIP IF**: Already selected during app creation (Step 2)
> **WHY FIRST**: Prototypes skip PROJECT.md prompt, saving time for quick POCs

**⚠️ MUST SHOW this comparison table before asking:**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    📋 PROJECT TYPE COMPARISON                               │
├─────────────┬───────────────┬───────────────┬───────────────────────────────┤
│   Aspecto   │   Prototype   │      MVP      │         Production            │
├─────────────┼───────────────┼───────────────┼───────────────────────────────┤
│ app    │ demo=true     │ demo=false    │ demo=false                    │
│ Unit Tests  │ ❌ Skip       │ ⚠️ Critical   │ ✅ Full coverage              │
│ Coverage    │ 0%            │ Varies        │ 80%+                          │
│ fury test   │ Optional      │ Required      │ Required                      │
│ Code Review │ ❌ Skip       │ ✅ Yes        │ ✅ Yes                        │
│ E2E/LTP     │ ❌ Skip       │ ❌ Skip       │ ✅ Opt-in                     │
│ Best for    │ Quick POC,    │ Internal      │ Customer-facing,              │
│             │ experiments   │ tools, MVPs   │ compliance required           │
└─────────────┴───────────────┴───────────────┴───────────────────────────────┘
```

**⛔ INVOKE TOOL (do not print this, CALL the tool)**:

```
AskUserQuestion(
  questions=[{
    "question": "What type of project is this?",
    "header": "Type",
    "options": [
      {"label": "Prototype", "description": "Demo app, no tests, quick validation"},
      {"label": "MVP", "description": "Tests for critical paths only"},
      {"label": "Production (Recommended)", "description": "Full coverage 80%+, quality checks"}
    ],
    "multiSelect": false
  }]
)
```

### Step 5.5: User Profile Selection

> **PURPOSE**: Determine technical vs non-technical profile ONCE and persist for future sessions.
> **CRITICAL**: Non-technical profile triggers EXPRESS MODE automatically.

**Check for existing profile (hierarchy)**:

```bash
# 1. Check global user preference (persistent across all projects)
profile_file="$HOMEsdd-kit/framework/user-profile.yaml"
if [ -f "$profile_file" ]; then
    profile=$(grep "^profile:" "$profile_file" | cut -d: -f2 | tr -d ' ')
    if [ -n "$profile" ]; then
        echo "✓ Using saved profile: $profile"
        # Skip asking, use existing
    fi
fi

# 2. If no global profile, check PROJECT.md defaults
# 3. If still no profile, ask user
```

#### First-time Profile Selection (if no existing profile)

**BEFORE ASKING**, display this behavior summary:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
👤 User Profile Configuration
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Your profile determines how much technical detail you see.

┌─────────────────────────────────────────────────────────────────┐
│ BUSINESS/PRODUCT FOCUS                                          │
├─────────────────────────────────────────────────────────────────┤
│ • Focus on WHAT to build, agent handles HOW                     │
│ • Simplified output (no layers, services, code snippets)        │
│ • Agent makes all technical decisions automatically             │
│ • Express mode always active (fastest flow)                     │
│ • Simplified effort labels instead of complexity ratings        │
│ • Questions in plain language                                   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ TECHNICAL FOCUS                                                 │
├─────────────────────────────────────────────────────────────────┤
│ • Full control over architecture decisions                      │
│ • See layers,  services, code snippets                      │
│ • Choose execution mode (express/standard)                      │
│ • Complexity ratings (Low/Medium/High)                          │
│ • Plan Mode for complex operations (configurable)               │
│ • Detailed error messages with stack traces                     │
└─────────────────────────────────────────────────────────────────┘
```

**⛔ INVOKE TOOL (do not print this, CALL the tool)**:

```
AskUserQuestion(
  questions=[{
    "question": "Which profile matches how you want to work?",
    "header": "Profile",
    "options": [
      {"label": "Business/Product focus", "description": "Focus on WHAT to build. Agent handles technical decisions."},
      {"label": "Technical focus", "description": "Full control. See layers, services, architecture details."}
    ],
    "multiSelect": false
  }]
)
```

#### Step 5.5.1: Plan Mode Preferences (Technical Profile Only)

> **SKIP IF**: User selected "Business/Product focus" (non-technical)

If user selects "Technical focus", show Plan Mode configuration:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
⚙️ Plan Mode Preferences
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Plan Mode pauses before complex operations for your approval.
(Only available in Claude Code CLI)
```

**⛔ INVOKE TOOL (do not print this, CALL the tool)**:

```
AskUserQuestion(
  questions=[{
    "question": "Enable Plan Mode for which scenarios?",
    "header": "Plan Mode",
    "options": [
      {"label": "Complex bug fixes (Recommended)", "description": "Pause before investigating DESIGN_FLAW or FEATURE_GAP errors"},
      {"label": "Technical spec in brownfield (Recommended)", "description": "Explore existing code before architecture decisions"},
      {"label": "Complex build tasks", "description": "Pause before high-complexity tasks or Layer 2"},
      {"label": "None", "description": "Never pause - implement directly"}
    ],
    "multiSelect": true
  }]
)
```

> **Default behavior**: If user selects "None", all plan_mode options set to false.
> If user doesn't select "None", selected options are enabled.

#### Step 5.5.2: Store Profile with Plan Mode Settings

**Store profile persistently**:

```bash
# Create sdd-kit/framework/user-profile.yaml
mkdir -p "$HOME/.sdd-kit"

if [ "$selected_profile" = "technical" ]; then
    # Technical profile with plan_mode settings
    cat > "$HOMEsdd-kit/framework/user-profile.yaml" << EOF
# SDD Kit User Profile
# Generated: $(date -Iseconds)
#
# To update these settings:
#   /sdd.project profile        → View current settings
#   /sdd.project profile --edit → Interactive update
#   Delete this file            → Re-select from scratch

profile: technical  # technical | non-technical

# Plan Mode settings (technical profile only)
# These control when the agent pauses for your approval
plan_mode:
  fix_complex_bugs: $fix_complex_bugs           # DESIGN_FLAW, FEATURE_GAP errors
  spec_technical_brownfield: $spec_brownfield   # Explore code before architecture
  build_complex_tasks: $build_complex           # High complexity, >5 files, Layer 2
  build_layer_transitions: false                # L1→L2, context >50%, 10+ tasks
  build_fury_test_recovery: false               # Ambiguous fury test failures
EOF
else
    # Non-technical profile (no plan_mode)
    cat > "$HOMEsdd-kit/framework/user-profile.yaml" << EOF
# SDD Kit User Profile
# Generated: $(date -Iseconds)
#
# To update these settings:
#   /sdd.project profile        → View current settings
#   /sdd.project profile --edit → Interactive update
#   Delete this file            → Re-select from scratch

profile: non-technical  # technical | non-technical

# Non-technical profile: Express mode always active
# Agent handles all technical decisions automatically
EOF
fi
```

#### Step 5.5.3: Confirmation Message

**After saving profile, show confirmation**:

For **Technical profile**:
```
✅ Profile saved: technical

📋 Your settings:
   • Full technical detail in outputs
   • Plan Mode enabled for: [list enabled options]
   • Execution mode: your choice per feature

💡 To update later:
   /sdd.project profile        → View current settings
   /sdd.project profile --edit → Change settings
```

For **Non-technical profile**:
```
✅ Profile saved: non-technical

📋 Your settings:
   • Simplified output (business focus)
   • Express mode always active
   • Agent handles all technical decisions

💡 To update later:
   /sdd.project profile        → View current settings
   /sdd.project profile --edit → Change settings
```

**Profile behavior mapping**:

| Profile | Execution Mode | Technical Questions | Display |
|---------|----------------|---------------------|---------|
| `non-technical` | **AUTO-EXPRESS** | Agent decides | Simplified |
| `technical` | User's choice | Ask user | Full detail |

**⚠️ CRITICAL: Non-Technical = Express Mode**:

```
IF user_profile == "non-technical":
    execution_mode = "express"  # FORCED, no matter what flag was passed
    show: "📋 Express mode activated (non-technical profile)"
    show: "   Agent will handle all technical decisions automatically"
```

**Store in meta.md**:

```yaml
user_profile:
  type: non-technical  # or technical
  source: global       # global | project | selected
  selected_at: 2026-01-22T10:30:00Z
```

### Step 6: Load PROJECT.md (CONDITIONAL)

**If `project_type == "prototype"`**:
  → Skip PROJECT.md prompt entirely
  → Use framework defaults for all settings
  → Show: "⏭️ PROJECT.md skipped (prototype mode)"
  → Continue to Step 7

**If `sdd/PROJECT.md` exists**:
  → Load defaults (ltp_enabled, atlassian_mcp_enabled, etc.)
  → **Validate PROJECT.md** (GenAI Offloaded):

```bash
# Validate PROJECT.md via GenAI Gateway
validation_result=$(bash sdd-kit/framework/tools/genai/genai-validate-project.sh .)
genai_exit=$?

if [ "$genai_exit" -eq 0 ]; then
    status=$(echo "$validation_result" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    if [ "$status" != "PASSED" ]; then
        echo "PROJECT.md validation: $status"
        echo "$validation_result" | grep -o '"recommendations":\[[^]]*\]'
    fi
elif [ "$genai_exit" -eq 2 ]; then
    # Fallback to deterministic validation
    validation_result=$(bash sdd-kit/framework/tools/validation/validate-project.sh sdd/PROJECT.md --json)
    is_valid=$(echo "$validation_result" | grep -o '"valid":[^,}]*' | cut -d: -f2)
    if [ "$is_valid" != "true" ]; then
        echo "PROJECT.md validation warnings:"
        echo "$validation_result" | grep -o '"warnings":\[[^]]*\]'
    fi
fi
# Continue regardless - not blocking for start
```

**If missing AND `project_type != "prototype"`**:
  → Use AskUserQuestion:
    1. Create PROJECT.md now (delegate to `sdd-project-wizard` subagent)
    2. Continue with framework defaults
    3. What is PROJECT.md?

#### Step 6.1: Doctor Tip (Non-blocking, ⭐ v1.7.3)

> **Purpose**: surface project-config health issues that would otherwise inflate specs.
> **Behavior**: deterministic, fast (<1s), prints at most one line, NEVER blocks `/sdd.start`.

```bash
# Run the doctor's heuristic scanner only. No LLM, no questions.
doctor_result=$(bash sdd-kit/framework/tools/doctor/scan-config.sh . --json 2>/dev/null || echo '{}')

# Trigger the tip if any of these heuristics fire:
#   - any phrase hit on rule X1 or X4 (anti-elegance / disables kit)
#   - any shadowed command (O1) or agent (O2)
#   - combined always-on footprint over the soft threshold (S3)
trip_x=$(echo "$doctor_result" | grep -oE '"rule":"(X1|X4)"' | head -1)
trip_o=$(echo "$doctor_result" | grep -oE '"rule":"(O1|O2)"' | head -1)
footprint=$(echo "$doctor_result" | grep -o '"always_on_lines":[0-9]*' | cut -d: -f2)
threshold=$(echo "$doctor_result" | grep -o '"threshold":[0-9]*' | head -1 | cut -d: -f2)

if [ -n "$trip_x" ] || [ -n "$trip_o" ] || { [ -n "$footprint" ] && [ -n "$threshold" ] && [ "$footprint" -gt "$threshold" ]; }; then
    echo "💡 Tip: tu config de proyecto puede estar afectando al kit (footprint=${footprint} líneas o conflicto detectado). Considera ejecutar /sdd.doctor antes de continuar."
fi
# Continue regardless — this tip is informational only.
```

> **Rules**:
> - This step does NOT pause, does NOT use AskUserQuestion, and does NOT modify anything.
> - If the scanner fails or is missing, silently skip (the empty JSON fallback ensures all triggers stay false).

### Step 6.5: Configure Local MCPs (if needed)

> **SKIP IF**: `project_type == "prototype"` (already skipped PROJECT.md)

After loading PROJECT.md, check for optional MCP configurations:

1. **Check `atlassian_mcp_enabled` setting**
2. **If `true`**:
   - Check if `.mcp.json` exists in project root
   - Add AtlassianMCP to local `.mcp.json`:

   ```json
   {
     "mcpServers": {
       "AtlassianMCP": {
         "command": "npx",
         "args": ["-y", "mcp-remote", "https://mcp.atlassian.com/v1/sse"]
       }
     }
   }
   ```

   - If `.mcp.json` exists: Merge AtlassianMCP into existing config
   - If `.mcp.json` doesn't exist: Create it with AtlassianMCP only
   - Show: "✓ AtlassianMCP configured locally for this project"
   - Note: "First use will require OAuth login to Atlassian"

3. **If `false` or missing**: Skip (AtlassianMCP not needed)

### Step 7: Create Feature Structure

**Validate name uniqueness** (BLOCKING):
```bash
# Check no other feature has the same name (regardless of date prefix)
existing=$(ls -1 sdd/wip sdd/features sdd/cancelled 2>/dev/null | sed 's/^[0-9]*-//' | sed 's/_[0-9]*$//' | grep -x "$feature_name" | head -1)
if [ -n "$existing" ]; then
    # Find the full directory name for context
    full_match=$(ls -1 sdd/wip sdd/features sdd/cancelled 2>/dev/null | grep -- "-${feature_name}\$" | head -1)
    echo "❌ Feature name '${feature_name}' already exists: ${full_match}"
fi
```

**On collision**: Do NOT stop and ask the user. Instead:
1. Derive a more specific name from the original user description (e.g., `auth` → `oauth-login`, `payment` → `payment-retry-mechanism`)
2. If the description is too vague to differentiate, append a qualifier: `{name}-v2`, `{name}-refactor`, `{name}-migration`
3. Show the user the alternative name and proceed: `⚠️ Name 'auth' already in use (20250601-auth). Using 'oauth-login' instead.`
4. If unable to derive a meaningful alternative, ask the user with AskUserQuestion

**Generate date prefix**:
```bash
feature_date=$(date +%Y%m%d)
feature_folder="${feature_date}-${feature_name}"
```

> **CRITICAL — folder naming format**: The folder name MUST be `YYYYMMDD-feature-name` (e.g., `20260326-user-auth`).
> Do NOT use sequential numbers like `001-`, `002-`, `003-` even if existing folders in `sdd/wip/` or `sdd/features/` use that format.
> Those are legacy folders from an older version. New features ALWAYS use the date prefix from `date +%Y%m%d`.

**Date Prefix Rules**:
- Date prefix is **organizational only** (for chronological ordering) — it is NOT an identifier
- Features are identified by **name** (e.g., `user-auth`) or **full name** (e.g., `20260325-user-auth`)
- Feature names are **globally unique** across `wip/`, `features/`, and `cancelled/`
- Date prefix **never changes** when feature moves from `wip/` to `features/`

**Create folders**:
```bash
mkdir -p "sdd/wip/$feature_folder"/{1-functional,2-technical,3-tasks,4-implementation/artifacts}
```

### Step 8: Create meta.md

Use template from `sdd-kit/framework/templates/meta.md` with:
- Feature name, date prefix, ID
- Project mode (greenfield/brownfield)
- Execution mode (express/standard)
- Framework version
- Project type and testing config
- **Spec language**: Read `language.specs` from `sdd/PROJECT.md` and set `spec_language` field in meta.md

```bash
# Read spec language from PROJECT.md (fallback to en)
spec_lang=$(grep "specs:" sdd/PROJECT.md 2>/dev/null | head -1 | awk '{print $2}')
if [ -z "$spec_lang" ]; then spec_lang="en"; fi
# Write to meta.md spec_language field
```

**Conditional**: Delete "Brownfield Context" section if greenfield.

### Step 9: Git Branch Management

```bash
current_branch=$(git rev-parse --abbrev-ref HEAD)

case "$current_branch" in
    main|master)
        # On main/master → Create feature branch
        git checkout -b "feature/$feature_name"
        ;;
    feature/*)
        # Already on feature branch → Ask user
        # Option 1: Switch to new branch
        # Option 2: Stay on current branch

        # ⚠️ SAFETY CHECK: Before switching branches
        if ! git diff --quiet || ! git diff --cached --quiet; then
            echo "⚠️ You have uncommitted changes."
            echo "Please commit or stash them before switching branches."
            # BLOCK - do not switch
        else
            git checkout -b "feature/$feature_name"
        fi
        ;;
    *)
        # Other branch → Ask what to do
        # Same safety check before switching
        ;;
esac
```

**Safety Rules**:
- NEVER switch branches with uncommitted changes
- Always verify clean working tree before `git checkout -b`

### Step 9.5: CLAUDE.md Integration (Claude Code Only)

> **PURPOSE**: Inject SDD Kit section into CLAUDE.md for session-bootstrap language enforcement.

**Precondition**: Only execute if `.claude/` directory exists (indicates Claude Code project).

```pseudocode
IF .claude/ directory exists:
    # Resolve language name for display
    spec_lang = resolved from Step 8
    lang_names = { "en": "English", "es": "Spanish (Español)", "pt": "Portuguese (Português)" }
    lang_name = lang_names[spec_lang] or "English"

    IF CLAUDE.md does NOT exist:
        → Generate CLAUDE.md with SDD Kit section (see template below)
    ELSE IF CLAUDE.md exists but does NOT contain "## SDD Kit":
        → Append SDD Kit section to end of file (preserve existing content)
    ELSE:
        → Replace existing "## SDD Kit" section with updated version
           (e.g., language may have changed)
```

**SDD Kit section template**:

```markdown
## SDD Kit

This project uses **SDD Kit** for spec-driven development.

### Spec Language
All specifications MUST be written in **[lang_name]** (`[spec_lang]`).
Do not mix languages in specs. Technical terms (API, REST, CRUD) stay in English.

### Quick Reference
- Framework expert: `Skill("sdd-kit-expert")`
- Workflow: `/sdd.start` → `/sdd.spec` → `/sdd.plan` → `/sdd.build` → `/sdd.finish`
- Project conventions: `sdd/PROJECT.md`
- Discovered patterns: `sdd/PATTERNS.md`

### Rules
- Never create files under `sdd/specs/`, `sdd/wip/`, or `sdd/features/` manually
- Always go through the `/sdd.start` workflow
- Respect the phased workflow — don't skip phases
```

> **CONDITIONAL — Mobile Implementation Rule** (append ONLY when `platform = android` or `platform = ios`):
>
> After the base template above, if `$platform` is `android` or `ios`, append a **platform-specific** section.
> Read `$platform` from the `$IS_MOBILE` flag or `detect-stack.sh` output (already resolved in Step 2).
> Do NOT append for backend, web, or empty platform.
>
> Generate the section by substituting `[platform]` with the actual value (`android` or `ios`),
> `[lang]` with `Kotlin/Android` or `Swift/iOS`, and `[skill]` with the exact skill name.
> Do NOT include the other platform's skill name — keep it project-specific.

**If platform = android**, append:

```markdown
## Mobile Implementation Rule

This project is **Android** — MANDATORY before any Kotlin/Android code:

1. Invoke `Skill("meli-frontender-android")`
2. Read `$SKILL_PATH/SKILL.md` — single source of truth for all documentation navigation
3. Follow the documentation navigation workflows referenced in SKILL.md for Everest and Andes
4. Build Confirmed Imports Registry from the skill docs before writing any code

**When it applies**: spec creation, task planning, implementation, code review — any step that touches Kotlin/Android.

**For subagents**: include as step 0 in the prompt of any subagent that works on Android code:
```
⚠️ STEP 0 — MANDATORY:
Skill("meli-frontender-android")
cat "$SKILL_PATH/SKILL.md"
Follow the documentation navigation workflows referenced in SKILL.md for Everest libraries and Andes components.
Build Confirmed Imports Registry. Only then read the task and write code.
```

`meli-frontender-android` is the ONLY authoritative source for Everest library APIs and Andes
component APIs. Pre-training knowledge about Android libraries MUST be overridden by the skill docs.
```

**If platform = ios**, append:

```markdown
## Mobile Implementation Rule

This project is **iOS** — MANDATORY before any Swift/iOS code:

1. Invoke `Skill("meli-frontender-ios")`
2. Read `$SKILL_PATH/SKILL.md` — single source of truth for all documentation navigation
3. Follow the documentation navigation workflows referenced in SKILL.md for Everest and Andes
4. Build Confirmed Imports Registry from the skill docs before writing any code

**When it applies**: spec creation, task planning, implementation, code review — any step that touches Swift/iOS.

**For subagents**: include as step 0 in the prompt of any subagent that works on iOS code:
```
⚠️ STEP 0 — MANDATORY:
Skill("meli-frontender-ios")
cat "$SKILL_PATH/SKILL.md"
Follow the documentation navigation workflows referenced in SKILL.md for Everest libraries and Andes components.
Build Confirmed Imports Registry. Only then read the task and write code.
```

`meli-frontender-ios` is the ONLY authoritative source for Everest library APIs and Andes
component APIs. Pre-training knowledge about iOS libraries MUST be overridden by the skill docs.
```

**Section replacement rules**:
- Framework ONLY owns the `## SDD Kit` section — never touch the rest of CLAUDE.md
- If user ran `/init` before, their content is preserved; we just append our section
- The section is idempotent: if `## SDD Kit` exists, replace from that header to the next `##` header (or end of file)
- If user runs `/init` after, they can re-run `/sdd.start` to re-inject the section

### Step 10: Load PATTERNS.md

If `sdd/PATTERNS.md` exists, load accumulated wisdom from previous features.

**Display pattern summary with origins**:

```
📋 Project Patterns Loaded:
   • N from feature learnings (auto-promoted via /sdd.finish)
   • M from team conventions (manually added via /sdd.project patterns)
   Total: X patterns influencing this feature
```

**Pattern counting algorithm**:

```bash
# Count patterns in "Team Conventions (Manually Added)" section
manual_count=$(grep -c "^\*\*.*\*\*:" sdd/PATTERNS.md | head -1)

# Count patterns in other sections (technology sections from /sdd.finish)
# Sections: Go, Database Patterns, BigQueue, Testing, etc.
auto_count=$(grep -c "^\*\*.*\*\*:" sdd/PATTERNS.md)
auto_count=$((auto_count - manual_count))

total=$((manual_count + auto_count))
```

**If no PATTERNS.md exists**: Skip this output (no patterns to load).

**Example output**:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📋 Project Patterns Loaded
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

   • 3 from team conventions (manual)
   • 6 from feature learnings (auto)
   Total: 9 patterns influencing this feature

   Tip: Add more conventions with /sdd.project patterns --add
```

### Step 11: Output Success Message

```
✅ Feature '[feature-name]' initialized (YYYYMMDD)
   📁 Location: sdd/wip/[YYYYMMDD]-[feature-name]/

[Mode-specific guidance]
```

**Conditional (only for Prototype projects)**:
```
   💡 For rapid prototyping: /sdd.go --resume (switches to express mode)
```

### Step 12: Interactive Next Steps

> **MANDATORY**: Always offer interactive selection, never just show text.

After displaying success message, use **AskUserQuestion** to offer next actions:

**Determine options based on context**:

```pseudocode
if saved_description exists in meta.md:
    option_1_label = "/sdd.spec (with saved context)"
    option_1_description = "Uses your description to seed the spec"
else:
    option_1_label = "/sdd.spec (Recommended)"
    option_1_description = "Start spec creation interactively"
```

**⛔ INVOKE TOOL (do not print this, CALL the tool)** - options vary by context:

```
AskUserQuestion(
  questions=[{
    "question": "Feature initialized. What would you like to do next?",
    "header": "Next",
    "options": [
      {"label": "/sdd.spec (Recommended)", "description": "Start spec creation interactively"},
      {"label": "/sdd.spec --audio", "description": "Describe your feature by voice"},
      {"label": "/sdd.check", "description": "View feature status"}
    ],
    "multiSelect": false
  }]
)
```

> **Note**: If saved description exists in meta.md, first option label should be "/sdd.spec (with saved context)" with description "Uses your description to seed the spec".

**On user selection**:

| Selection | Action |
|-----------|--------|
| /sdd.spec (with saved context) | `Skill(skill="sdd.spec")` - description auto-loaded from meta.md |
| /sdd.spec (Recommended) | `Skill(skill="sdd.spec")` |
| /sdd.spec --audio | `Skill(skill="sdd.spec", args="--audio")` |
| /sdd.check | `Skill(skill="sdd.check")` |
| Other | User types custom input (e.g., `/sdd.spec "nueva descripción"`, questions, etc.) |

> **NOTE**: AskUserQuestion ALWAYS includes "Other" option automatically.
> Users can write ANY text: another command, a question, feedback, etc.

---

## Validations

### Pre-execution (BLOCKING)

| Validation | Blocking | Recovery |
|------------|----------|----------|
| Not inside `sdd-kit/framework/` | YES | Ask user to change directory |
| Input is valid name (not prompt) | YES | Convert → suggest → confirm |
| Valid name format (kebab-case) | YES | Ask for valid name |
| Feature doesn't exist in `wip/` | YES | Ask for different name |
| Logged in to  CLI | AUTO-RETRY | Run `` |
| app exists | USER CHOICE | Create / Correct / Local mode |

### Post-execution

- [ ] Folder `sdd/wip/[YYYYMMDD-feature-name]/` created
- [ ] File `meta.md` exists with mode set
- [ ] Execution mode recorded

---

## References

- **Meta.md template**: `sdd-kit/framework/templates/meta.md`
- **Lite spec template**: `sdd-kit/framework/templates/lite/spec.md`
- **Gitignore templates**: `sdd-kit/framework/templates/gitignore/`
- **PROJECT.md wizard**: `sdd-project-wizard` subagent
- ****: `standards/mandatory-standards.md`

---

## AI Agent Instructions

### Help Flag Detection

**WHEN** the user runs `/sdd.start help`:
1. Output ONLY the "Quick Help" section (not full documentation)
2. Do NOT execute start logic
3. Keep response concise (~15 lines)

### Execution Order (MANDATORY)

```
STEP 0: USER PROFILE CHECK (⛔ BLOCKING - NEVER SKIP)
├─ sdd-kit/framework/user-profile.yaml exists? → Read profile, continue
└─ Missing? → ⛔ STOP. Ask profile (Business vs Technical), save file, THEN continue

STEP 1: VALIDATE INPUT (BLOCKING)
├─ Is it a prompt/description? → Auto-infer name, show message, continue
├─ Is it valid kebab-case? → Continue
└─ NEVER create files until validation passes

STEP 2: FURY VERIFICATION
├─ .fury exists? → Extract app name
└─ .fury missing?
   ├─ Auto-detect from folder: basename $(pwd)
   ├─ Verify with 
   └─ Only ask user if auto-detect fails

STEP 3: CREATE STRUCTURE (only after Steps 0-2 pass)

STEP 4: OUTPUT SUCCESS MESSAGE
```

### Auto-Inference Pattern

```
✅ CORRECT (v1.2.6+):
User: /sdd.start I want a payment system with refunds
AI: ✓ Feature name inferred: `payment-refunds` (from your description)
    [Continues automatically to Step 2...]

❌ WRONG:
User: /sdd.start I want a payment system with refunds
AI: OK, creating payment service... [starts implementing without feature name]
```

### Key Rules

1. **PROFILE FIRST** - If `sdd-kit/framework/user-profile.yaml` is missing, ask BEFORE anything else
2. **VALIDATE FIRST** - Never skip input validation
3. **Prompt ≠ Name** - If >5 words or punctuation, it's a description → auto-infer name
4. **Auto-infer, don't ask** - Infer feature name from description and proceed automatically
5. **Delegate heavy ops** - Use `sdd-project-wizard` for PROJECT.md
6. **TRACK APP CREATION** - If you call `create_application()`, set `app_created_via_puma=true` and ALWAYS use `` in Step 3

### --audio Flag Detection

### Example 1: Standard Mode (default)
```
User: /sdd.start payment-gateway

AI: ✅ Feature #003 'payment-gateway' initialized
    📁 Location: sdd/wip/20260325-payment-gateway/

    📋 Standard Mode Active

    [AskUserQuestion displayed]:
    ┌─ Next ─────────────────────────────────────────────┐
    │ Feature initialized. What would you like to do    │
    │ next?                                             │
    │                                                   │
    │ ○ /sdd.spec (Recommended)                        │
    │   Start spec creation interactively               │
    │                                                   │
    │ ○ /sdd.spec --audio                              │
    │   Describe your feature by voice                  │
    │                                                   │
    │ ○ /sdd.check                                     │
    │   View feature status                             │
    │                                                   │
    │ ○ Other...                                        │
    └───────────────────────────────────────────────────┘

User: [Selects "/sdd.spec (Recommended)"]

AI: [Invokes Skill(skill="sdd.spec")]
    [/sdd.spec starts automatically...]
```

### Example 2: Description Auto-Inferred
```
User: /sdd.start I want to create a REST API that handles user authentication

AI: ✓ Feature name inferred: `user-authentication-api` (from your description)
    📝 Description saved for /sdd.spec context

    [Continues to Step 2:  App Verification...]

    ✅ Feature #003 'user-authentication-api' initialized
       📁 Location: sdd/wip/20260325-user-authentication-api/

    [AskUserQuestion displayed]:
    ┌─ Next ─────────────────────────────────────────────┐
    │ Feature initialized. What would you like to do    │
    │ next?                                             │
    │                                                   │
    │ ○ /sdd.spec (with saved context)                 │
    │   Uses your description to seed the spec          │
    │                                                   │
    │ ○ /sdd.spec --audio                              │
    │   Describe your feature by voice                  │
    │                                                   │
    │ ○ /sdd.check                                     │
    │   View feature status                             │
    │                                                   │
    │ ○ Other...                                        │
    └───────────────────────────────────────────────────┘

User: [Selects "/sdd.spec (with saved context)"]

AI: [Invokes Skill(skill="sdd.spec") - description auto-loaded from meta.md]
```

### Example 3: New App Created via 
```
User: /sdd.start new-feature
# (user is in folder ~/Dev/my-cool-api/)

AI: ⚠️ Application 'my-cool-api' not found.

    What would you like to do?
    1. 🆕 Create new app
    2. ✏️  Correct the name
    3. 💻 Continue without app (local mode)

User: 1

AI: What should the application be named?
    1. my-cool-api          ← folder name (Option 1 always)
    2. cool-api-service      ← alternative
    3. my-cool-api-service   ← alternative

User: 1

AI: 📋 Project Type?
    1. Prototype (no tests)
    2. MVP (critical tests only)
    3. Production (full coverage)

User: 3

AI: [Gets technologies via ]
    [Gets user's projects via TeamsMCP]
    [Creates app 'my-cool-api' with  web-maven-spring]
    [Cleans scaffolding samples]

    ✅ Feature #001 'new-feature' initialized
       📁 Location: sdd/wip/001-new-feature/
       🏭 Production mode: Full test coverage

    [AskUserQuestion: "Feature initialized. What would you like to do next?"]
    Options: /sdd.spec (Recommended), /sdd.spec --audio, /sdd.check

User: [Selects "/sdd.spec (Recommended)"]

AI: [Invokes Skill(skill="sdd.spec")]
```

---

## Command Flow

```
/sdd.start ─────► Validate Input ─────►  Verification
                        │                       │
                        ▼                       ▼
                   Create Structure ◄───── Clone/Scaffold
                        │
                        ▼
                   Output Success
                        │
                        ▼
              /sdd.spec (standard)
              /sdd.go (express)
```

---

## From Backlog Option

`/sdd.start --from-backlog <ID>`:
1. Read item from `sdd/backlog.md`
2. Suggest feature name from title
3. Pre-populate context (problem, affected files)
4. Mark backlog item as `in-progress`
5. Add `from_backlog: <ID>` to meta.md

---

## --reopen Flag

> **Lazy-loaded**: When `--reopen` flag is present, Read `references/reopen-workflow.md` for the complete feature reopen workflow (R1-R7).

---

## --rename Flag

> **PURPOSE**: Rename a feature after it has been created.

### Usage

```bash
/sdd.start --rename [new-name]
```

### What it does

1. **Validates new name** is kebab-case (lowercase, hyphens only)
2. **Renames folder** `sdd/wip/[old-name]/` → `sdd/wip/[new-name]/`
3. **Updates meta.md** with new feature name
4. **Updates tasks.json** references (if exists)

### Example

```bash
# Current feature: user-authentication
/sdd.start --rename oauth-login

# Result:
# ✓ Renamed: sdd/wip/20260320-user-authentication/ → sdd/wip/20260320-oauth-login/
# ✓ Updated: meta.md feature_name
# ✓ Updated: tasks.json references (if applicable)
```

### Restrictions

| Restriction | Reason |
|-------------|--------|
| Cannot rename if tasks in_progress | Avoid breaking active implementation |
| New name must be unique | Cannot conflict with existing features in `wip/` |
| Must be kebab-case | Framework naming convention |

### AI Agent Instructions for --rename

**WHEN** user runs `/sdd.start --rename [new-name]`:

1. **Find current feature** in `sdd/wip/`
2. **Validate new name**:
   - Is kebab-case? (lowercase, hyphens, 3-50 chars)
   - Is unique? (doesn't exist in `wip/`)
3. **Check for active tasks**:
   - Read `tasks.json` if exists
   - If any task has `status: "in_progress"` → BLOCK with message
4. **Perform rename**:
   ```bash
   old_path="sdd/wip/XXX-old-name"
   new_path="sdd/wip/XXX-new-name"
   mv "$old_path" "$new_path"
   ```
5. **Update meta.md**:
   - Change `feature_name:` field
6. **Update tasks.json** (if exists):
   - Update any references to old feature name
7. **Output success message**:
   ```
   ✓ Feature renamed: old-name → new-name
     📁 Location: sdd/wip/XXX-new-name/
   ```
