#!/bin/bash

# SDD SDD Kit - Completion Validator
# Validates feature is ready for archival

FEATURE_PATH=$1

if [ -z "$FEATURE_PATH" ]; then
    echo "❌ Error: Feature path required"
    echo "Usage: $0 sdd/wip/[feature-name]"
    exit 1
fi

PROGRESS_FILE="$FEATURE_PATH/4-implementation/progress.md"
TASKS_FILE="$FEATURE_PATH/3-tasks/tasks.json"

if [ ! -f "$PROGRESS_FILE" ]; then
    echo "❌ Error: Progress file not found at $PROGRESS_FILE"
    echo "   Has implementation started?"
    exit 1
fi

echo "🔍 Validating feature completion for $(basename $FEATURE_PATH)..."
echo ""

errors=0
warnings=0

# ============================================
# Task Completion Check
# ============================================

echo "✅ Checking task completion..."

# tasks.json is JSON, not markdown — counting "#### TASK-" headers in it
# always returns 0 and made this check pass trivially (0 == 0) no matter how
# many tasks actually existed. Count straight from the JSON's own fields
# instead; no jq dependency needed since /sdd.plan always pretty-prints one
# field per line.
total_tasks=$(grep -c '"id": *"TASK-' "$TASKS_FILE")
completed_tasks=$(grep -c '"status": *"completed"' "$TASKS_FILE")

echo "  Tasks: $completed_tasks / $total_tasks completed"

if [ "$completed_tasks" -ne "$total_tasks" ]; then
    incomplete=$((total_tasks - completed_tasks))
    echo "  ❌ $incomplete task(s) incomplete"

    # List incomplete tasks straight from tasks.json (id + status pairs).
    echo ""
    echo "  Incomplete tasks:"
    awk -F'"' '/"id": *"TASK-/{id=$4} /"status":/{if ($4!="completed") print "  • "id" ("$4")"}' "$TASKS_FILE"

    ((errors++))
else
    echo "  ✅ All $total_tasks tasks completed"
fi

echo ""

# ============================================
# Test Results Validation
# ============================================

echo "🧪 Validating test results..."

# Look for test results in progress.md
if grep -q "Tests passing:" "$PROGRESS_FILE"; then
    # Extract test pass rate
    test_line=$(grep "Tests passing:" "$PROGRESS_FILE" | tail -1)
    passing=$(echo "$test_line" | grep -oE "[0-9]+/[0-9]+" | cut -d/ -f1)
    total=$(echo "$test_line" | grep -oE "[0-9]+/[0-9]+" | cut -d/ -f2)

    if [ -n "$passing" ] && [ -n "$total" ]; then
        pass_rate=$((passing * 100 / total))

        echo "  Tests: $passing/$total passing ($pass_rate%)"

        if [ "$pass_rate" -lt 95 ]; then
            echo "  ❌ Test pass rate below 95% (found: $pass_rate%)"
            ((errors++))
        else
            echo "  ✅ Test pass rate: $pass_rate% (target: >95%)"
        fi
    fi
else
    echo "  ❌ No test results found in progress.md"
    ((errors++))
fi

# Check coverage
if grep -q "Coverage:" "$PROGRESS_FILE"; then
    coverage=$(grep "Coverage:" "$PROGRESS_FILE" | tail -1 | grep -oE "[0-9]+%" | tr -d '%')

    if [ -n "$coverage" ]; then
        echo "  Coverage: $coverage%"

        if [ "$coverage" -lt 80 ]; then
            echo "  ❌ Coverage below 80% (found: $coverage%)"
            ((errors++))
        else
            echo "  ✅ Coverage: $coverage% (target: >80%)"
        fi
    fi
else
    echo "  ⚠️  Warning: No coverage report found"
    ((warnings++))
fi

echo ""

# ============================================
# Code Quality Validation
# ============================================

echo "🔍 Validating code quality..."

# Check for linter errors in progress.md
if grep -q "Linter.*errors: 0\|Linter: 0 errors" "$PROGRESS_FILE"; then
    echo "  ✅ Linter: 0 errors"
else
    if grep -qE "Linter.*errors: [1-9]|Linter: [1-9]+ errors" "$PROGRESS_FILE"; then
        linter_errors=$(grep -oE "Linter.*errors: [0-9]+|Linter: [0-9]+ errors" "$PROGRESS_FILE" | grep -oE "[0-9]+" | head -1)
        echo "  ❌ Linter errors found: $linter_errors"
        ((errors++))
    else
        echo "  ⚠️  Warning: No linter results in progress.md"
        ((warnings++))
    fi
fi

# Check for type errors
if grep -q "Type errors: 0" "$PROGRESS_FILE"; then
    echo "  ✅ Type errors: 0"
else
    if grep -qE "Type errors: [1-9]" "$PROGRESS_FILE"; then
        type_errors=$(grep -oE "Type errors: [0-9]+" "$PROGRESS_FILE" | grep -oE "[0-9]+" | head -1)
        echo "  ❌ Type errors found: $type_errors"
        ((errors++))
    else
        echo "  ⚠️  Warning: No type check results in progress.md"
        ((warnings++))
    fi
fi

echo ""

# ============================================
# Check for Blockers
# ============================================

echo "🚧 Checking for blockers..."

blocked_tasks=$(grep -c "⏸️ Blocked" "$PROGRESS_FILE")

if [ "$blocked_tasks" -gt 0 ]; then
    echo "  ❌ $blocked_tasks task(s) still blocked"
    echo ""
    echo "  Blocked tasks:"
    grep "⏸️ Blocked" "$PROGRESS_FILE" | grep "^#### TASK-" | sed 's/^#### /  • /'
    echo ""
    echo "  Resolve all blockers before completing"
    ((errors++))
else
    echo "  ✅ No blocked tasks"
fi

echo ""

# ============================================
# Documentation Check
# ============================================

echo "📚 Validating documentation..."

# Check if README or API docs were updated (look for doc tasks)
doc_tasks_done=$(grep -i "documentation\|docs\|readme" "$PROGRESS_FILE" | grep -c "✅ Completed")

if [ "$doc_tasks_done" -eq 0 ]; then
    echo "  ⚠️  Warning: No documentation tasks completed"
    echo "     Consider if API docs, README, or runbooks need updating"
    ((warnings++))
else
    echo "  ✅ Documentation tasks completed: $doc_tasks_done"
fi

echo ""

# ============================================
# Commits Validation
# ============================================

echo "📦 Validating commits..."

commits=$(grep -c "Commit: \`" "$PROGRESS_FILE")

if [ "$commits" -eq 0 ]; then
    echo "  ❌ No commits found in progress.md"
    echo "     Each completed task should have associated commit"
    ((errors++))
elif [ "$commits" -ne "$completed_tasks" ]; then
    echo "  ⚠️  Warning: $commits commits for $completed_tasks tasks"
    echo "     Some tasks may share commits, or commits are missing"
    ((warnings++))
else
    echo "  ✅ Commits tracked: $commits"
fi

echo ""

# ============================================
# Velocity and Metrics Check
# ============================================

echo "📊 Checking metrics..."

if grep -q "## Velocity Metrics" "$PROGRESS_FILE"; then
    echo "  ✅ Velocity metrics tracked"

    # Check for estimation accuracy
    if grep -qi "estimation accuracy" "$PROGRESS_FILE"; then
        echo "  ✅ Estimation accuracy recorded"
    else
        echo "  ⚠️  Warning: Estimation accuracy not calculated"
        ((warnings++))
    fi
else
    echo "  ⚠️  Warning: Velocity metrics not tracked"
    ((warnings++))
fi

echo ""

# ============================================
# Quality Checks
# ============================================

echo "🔍 Final quality checks..."

# Check for TODO markers in progress.md (indicating incomplete work)
todos=$(grep -ic "TODO\|TBD\|FIXME" "$PROGRESS_FILE")
if [ "$todos" -gt 0 ]; then
    echo "  ⚠️  Warning: $todos TODO markers in progress.md"
    echo "     Resolve before completing"
    ((warnings++))
else
    echo "  ✅ No TODO markers"
fi

# Check for recent activity (last updated recently)
if grep -q "Last Updated.*$(date +%Y-%m-%d)" "$PROGRESS_FILE"; then
    echo "  ✅ Progress updated today"
else
    echo "  ⚠️  Warning: Progress.md may not be up to date"
    ((warnings++))
fi

echo ""

# ============================================
# Summary
# ============================================

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ "$errors" -eq 0 ]; then
    echo "✅ Feature Completion Validation PASSED"

    if [ "$warnings" -gt 0 ]; then
        echo ""
        echo "⚠️  $warnings warning(s) found (non-blocking)"
        echo ""
        echo "Warnings can be addressed or accepted. Feature is ready to complete."
    fi

    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "🎉 Feature is ready for archival!"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "Summary:"
    echo "• All $total_tasks tasks completed ✅"
    echo "• Tests passing ✅"
    echo "• Quality checks passed ✅"
    echo ""
    echo "Next: /sdd.finish"
    exit 0
else
    echo "❌ Feature Completion Validation FAILED"
    echo ""
    echo "   Errors: $errors (must fix)"
    echo "   Warnings: $warnings (optional)"
    echo ""
    echo "How to fix:"
    echo "• Complete all pending tasks"
    echo "• Fix failing tests"
    echo "• Resolve blockers"
    echo "• Fix linter/type errors"
    echo "• Re-validate: $0 $FEATURE_PATH"
    echo ""
    echo "Or use --force to override (not recommended):"
    echo "• /sdd.finish --force"
    exit 1
fi
