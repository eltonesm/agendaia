#!/bin/bash
# SDD SDD Kit - Language & Build Tool Detector
# Detects primary language and build/test commands
# Replaces LLM-based project analysis (saves ~2,000 tokens per detection)
#
# Usage: detect-language-buildtool.sh [path] [--json]
# Returns: JSON with language, build tool, and commands

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ============================================
# Configuration
# ============================================

PROJECT_PATH="${1:-.}"
OUTPUT_JSON=false

shift 2>/dev/null || true
while [[ $# -gt 0 ]]; do
    case $1 in
        --json) OUTPUT_JSON=true; shift ;;
        *) shift ;;
    esac
done

# ============================================
# Detection Functions
# ============================================

detect_language() {
    local path=$1

    # Check for language-specific files
    if [ -f "$path/pom.xml" ] || [ -f "$path/build.gradle" ] || [ -f "$path/build.gradle.kts" ]; then
        # Check for Kotlin
        if find "$path" -maxdepth 3 -name "*.kt" 2>/dev/null | head -1 | grep -q .; then
            echo "kotlin"
        else
            echo "java"
        fi
    elif [ -f "$path/package.json" ]; then
        # Check for TypeScript
        if [ -f "$path/tsconfig.json" ]; then
            echo "typescript"
        else
            echo "javascript"
        fi
    elif [ -f "$path/go.mod" ]; then
        echo "go"
    elif [ -f "$path/requirements.txt" ] || [ -f "$path/setup.py" ] || [ -f "$path/pyproject.toml" ]; then
        echo "python"
    elif [ -f "$path/Cargo.toml" ]; then
        echo "rust"
    else
        # Fallback to file counting
        local java_count=$(find "$path" -maxdepth 5 -name "*.java" 2>/dev/null | wc -l | tr -d ' ')
        local ts_count=$(find "$path" -maxdepth 5 -name "*.ts" -not -name "*.d.ts" 2>/dev/null | wc -l | tr -d ' ')
        local js_count=$(find "$path" -maxdepth 5 -name "*.js" -not -path "*/node_modules/*" 2>/dev/null | wc -l | tr -d ' ')
        local go_count=$(find "$path" -maxdepth 5 -name "*.go" 2>/dev/null | wc -l | tr -d ' ')
        local py_count=$(find "$path" -maxdepth 5 -name "*.py" 2>/dev/null | wc -l | tr -d ' ')

        local max_count=0
        local detected="unknown"

        if [ "$java_count" -gt "$max_count" ]; then max_count=$java_count; detected="java"; fi
        if [ "$ts_count" -gt "$max_count" ]; then max_count=$ts_count; detected="typescript"; fi
        if [ "$js_count" -gt "$max_count" ]; then max_count=$js_count; detected="javascript"; fi
        if [ "$go_count" -gt "$max_count" ]; then max_count=$go_count; detected="go"; fi
        if [ "$py_count" -gt "$max_count" ]; then max_count=$py_count; detected="python"; fi

        echo "$detected"
    fi
}

detect_build_tool() {
    local path=$1
    local language=$2

    case "$language" in
        java|kotlin)
            if [ -f "$path/pom.xml" ]; then
                echo "maven"
            elif [ -f "$path/build.gradle" ] || [ -f "$path/build.gradle.kts" ]; then
                echo "gradle"
            else
                echo "unknown"
            fi
            ;;
        typescript|javascript)
            if [ -f "$path/yarn.lock" ]; then
                echo "yarn"
            elif [ -f "$path/pnpm-lock.yaml" ]; then
                echo "pnpm"
            elif [ -f "$path/package-lock.json" ] || [ -f "$path/package.json" ]; then
                echo "npm"
            else
                echo "unknown"
            fi
            ;;
        go)
            echo "go"
            ;;
        python)
            if [ -f "$path/pyproject.toml" ]; then
                echo "poetry"
            elif [ -f "$path/Pipfile" ]; then
                echo "pipenv"
            else
                echo "pip"
            fi
            ;;
        rust)
            echo "cargo"
            ;;
        *)
            echo "unknown"
            ;;
    esac
}

detect_test_framework() {
    local path=$1
    local language=$2

    case "$language" in
        java|kotlin)
            if grep -r "org.junit.jupiter" "$path" --include="pom.xml" --include="*.gradle*" 2>/dev/null | head -1 | grep -q .; then
                echo "junit5"
            elif grep -r "org.junit" "$path" --include="pom.xml" --include="*.gradle*" 2>/dev/null | head -1 | grep -q .; then
                echo "junit4"
            else
                echo "junit"
            fi
            ;;
        typescript|javascript)
            if [ -f "$path/jest.config.js" ] || [ -f "$path/jest.config.ts" ] || grep -q "jest" "$path/package.json" 2>/dev/null; then
                echo "jest"
            elif grep -q "mocha" "$path/package.json" 2>/dev/null; then
                echo "mocha"
            elif grep -q "vitest" "$path/package.json" 2>/dev/null; then
                echo "vitest"
            else
                echo "jest"
            fi
            ;;
        go)
            echo "go-test"
            ;;
        python)
            if [ -f "$path/pytest.ini" ] || [ -f "$path/pyproject.toml" ] && grep -q "pytest" "$path/pyproject.toml" 2>/dev/null; then
                echo "pytest"
            else
                echo "pytest"
            fi
            ;;
        rust)
            echo "cargo-test"
            ;;
        *)
            echo "unknown"
            ;;
    esac
}

get_commands() {
    local language=$1
    local build_tool=$2
    local test_framework=$3

    local build_cmd="" test_cmd="" lint_cmd="" run_cmd=""

    case "$build_tool" in
        maven)
            build_cmd="mvn clean package -DskipTests"
            test_cmd="mvn test"
            lint_cmd="mvn checkstyle:check"
            run_cmd="mvn spring-boot:run"
            ;;
        gradle)
            build_cmd="./gradlew build -x test"
            test_cmd="./gradlew test"
            lint_cmd="./gradlew check"
            run_cmd="./gradlew bootRun"
            ;;
        npm)
            build_cmd="npm run build"
            test_cmd="npm test"
            lint_cmd="npm run lint"
            run_cmd="npm start"
            ;;
        yarn)
            build_cmd="yarn build"
            test_cmd="yarn test"
            lint_cmd="yarn lint"
            run_cmd="yarn start"
            ;;
        pnpm)
            build_cmd="pnpm build"
            test_cmd="pnpm test"
            lint_cmd="pnpm lint"
            run_cmd="pnpm start"
            ;;
        go)
            build_cmd="go build ./..."
            test_cmd="go test -race ./..."
            lint_cmd="golangci-lint run"
            run_cmd="go run main.go"
            ;;
        pip|poetry|pipenv)
            build_cmd="python -m build"
            test_cmd="pytest"
            lint_cmd="flake8 ."
            run_cmd="python -m app"
            ;;
        cargo)
            build_cmd="cargo build"
            test_cmd="cargo test"
            lint_cmd="cargo clippy"
            run_cmd="cargo run"
            ;;
    esac

    echo "BUILD:$build_cmd"
    echo "TEST:$test_cmd"
    echo "LINT:$lint_cmd"
    echo "RUN:$run_cmd"
}

# ============================================
# -specific Detection
# ============================================

detect_commands() {
    local path=$1

    # Check if this is a  project
    if [ -f "$path/Dockerfile" ] && grep -q "fury" "$path/Dockerfile" 2>/dev/null; then
        echo "FURY:true"
        echo "FURY_BUILD:fury execute build"
        echo "FURY_TEST:fury test"
    elif [ -f "$path/.fury/config.yml" ] || [ -f "$path/fury.yml" ]; then
        echo "FURY:true"
        echo "FURY_BUILD:fury execute build"
        echo "FURY_TEST:fury test"
    else
        echo "FURY:false"
    fi
}

# ============================================
# Main
# ============================================

main() {
    if [ ! -d "$PROJECT_PATH" ]; then
        echo "Error: Path not found: $PROJECT_PATH"
        exit 1
    fi

    local language build_tool test_framework
    language=$(detect_language "$PROJECT_PATH")
    build_tool=$(detect_build_tool "$PROJECT_PATH" "$language")
    test_framework=$(detect_test_framework "$PROJECT_PATH" "$language")

    local commands
    commands=$(get_commands "$language" "$build_tool" "$test_framework")

    local build_cmd test_cmd lint_cmd run_cmd
    build_cmd=$(echo "$commands" | grep "^BUILD:" | cut -d: -f2-)
    test_cmd=$(echo "$commands" | grep "^TEST:" | cut -d: -f2-)
    lint_cmd=$(echo "$commands" | grep "^LINT:" | cut -d: -f2-)
    run_cmd=$(echo "$commands" | grep "^RUN:" | cut -d: -f2-)

    local info
    info=$(detect_commands "$PROJECT_PATH")
    local is_fury=$(echo "$info" | grep "^FURY:" | cut -d: -f2)
    local build=$(echo "$info" | grep "^FURY_BUILD:" | cut -d: -f2-)
    local test=$(echo "$info" | grep "^FURY_TEST:" | cut -d: -f2-)

    if [ "$OUTPUT_JSON" = true ]; then
        cat << EOF
{
  "language": "$language",
  "build_tool": "$build_tool",
  "test_framework": "$test_framework",
  "commands": {
    "build": $([ -n "$build_cmd" ] && echo "\"$build_cmd\"" || echo "null"),
    "test": $([ -n "$test_cmd" ] && echo "\"$test_cmd\"" || echo "null"),
    "lint": $([ -n "$lint_cmd" ] && echo "\"$lint_cmd\"" || echo "null"),
    "run": $([ -n "$run_cmd" ] && echo "\"$run_cmd\"" || echo "null")
  },
  "fury": {
    "is_project": $is_fury,
    "build_command": $([ -n "$build" ] && echo "\"$build\"" || echo "null"),
    "test_command": $([ -n "$test" ] && echo "\"$test\"" || echo "null")
  }
}
EOF
    else
        echo "=== Project Detection ==="
        echo ""
        echo "Language: $language"
        echo "Build Tool: $build_tool"
        echo "Test Framework: $test_framework"
        echo ""
        echo "Commands:"
        echo "  Build: $build_cmd"
        echo "  Test: $test_cmd"
        echo "  Lint: $lint_cmd"
        echo "  Run: $run_cmd"

        if [ "$is_fury" = "true" ]; then
            echo ""
            echo " Project: Yes"
            echo "   Build: $build"
            echo "   Test: $test"
        fi
    fi
}

main
