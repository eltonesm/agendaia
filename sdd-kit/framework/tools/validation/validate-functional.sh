#!/bin/bash

# SDD SDD Kit - Functional Spec Validator
# Validates functional spec completeness and quality

FEATURE_PATH=$1

if [ -z "$FEATURE_PATH" ]; then
    echo "❌ Error: Feature path required"
    echo "Usage: $0 sdd/wip/[feature-name]"
    exit 1
fi

SPEC_FILE="$FEATURE_PATH/1-functional/spec.md"

if [ ! -f "$SPEC_FILE" ]; then
    echo "❌ Error: Functional spec not found at $SPEC_FILE"
    exit 1
fi

echo "🔍 Validating functional spec for $(basename $FEATURE_PATH)..."
echo ""

errors=0
warnings=0

# ============================================
# Required Sections Check
# ============================================

required_sections=(
    "## Problem Statement"
    "## Objectives"
    "## Scope"
    "## User Stories"
    "## User Experience"
    "## Success Metrics"
)

echo "📋 Checking required sections..."

for section in "${required_sections[@]}"; do
    if grep -q "^$section" "$SPEC_FILE"; then
        echo "  ✅ $section"
    else
        echo "  ❌ Missing required section: $section"
        ((errors++))
    fi
done

echo ""

# ============================================
# User Stories Validation
# ============================================

echo "👤 Validating user stories..."

# Count user stories
us_count=$(grep -c "^### US-" "$SPEC_FILE")

if [ "$us_count" -eq 0 ]; then
    echo "  ❌ No user stories found (expected format: ### US-1: Title)"
    ((errors++))
elif [ "$us_count" -lt 2 ]; then
    echo "  ⚠️  Warning: Only $us_count user story (typically 3-10 expected)"
    ((warnings++))
else
    echo "  ✅ Found $us_count user stories"
fi

# Check acceptance criteria
ac_count=$(grep -c "^**Acceptance Criteria" "$SPEC_FILE")

if [ "$ac_count" -lt "$us_count" ]; then
    echo "  ❌ Some user stories missing acceptance criteria"
    echo "     Found: $ac_count acceptance criteria sections for $us_count user stories"
    ((errors++))
else
    echo "  ✅ All user stories have acceptance criteria sections"
fi

# Check that each story has "As a", "I want", "So that"
as_a_count=$(grep -c "^\*\*As a\*\*" "$SPEC_FILE")
i_want_count=$(grep -c "^\*\*I want\*\*" "$SPEC_FILE")
so_that_count=$(grep -c "^\*\*So that\*\*" "$SPEC_FILE")

if [ "$as_a_count" -ne "$us_count" ] || [ "$i_want_count" -ne "$us_count" ] || [ "$so_that_count" -ne "$us_count" ]; then
    echo "  ⚠️  Warning: Some user stories don't follow standard format"
    echo "     Expected: 'As a', 'I want', 'So that' in each story"
    ((warnings++))
else
    echo "  ✅ User stories follow standard format"
fi

# Check for priority and complexity
priority_count=$(grep -c "^\*\*Priority\*\*:" "$SPEC_FILE")
if [ "$priority_count" -lt "$us_count" ]; then
    echo "  ⚠️  Warning: Some user stories missing Priority"
    ((warnings++))
fi

echo ""

# ============================================
# Objectives Validation
# ============================================

echo "🎯 Validating objectives..."

# Count objectives (looking for numbered lists under Objectives section)
objectives_section=$(sed -n '/^## Objectives$/,/^##/p' "$SPEC_FILE")
objective_count=$(echo "$objectives_section" | grep -c "^[0-9]\+\.")

if [ "$objective_count" -eq 0 ]; then
    echo "  ❌ No objectives found (expected format: 1. Objective text)"
    ((errors++))
elif [ "$objective_count" -lt 2 ]; then
    echo "  ⚠️  Warning: Only $objective_count objective (typically 2-5)"
    ((warnings++))
else
    echo "  ✅ Found $objective_count objectives"
fi

# Check for measurable objectives (should contain numbers or percentages)
measurable_count=$(echo "$objectives_section" | grep -E "[0-9]+%|[0-9]+ |\$[0-9]" | grep -c "^[0-9]\+\.")
if [ "$measurable_count" -lt "$objective_count" ]; then
    echo "  ⚠️  Warning: Some objectives may not be measurable"
    echo "     Objectives should include numbers, percentages, or specific targets"
    ((warnings++))
else
    echo "  ✅ Objectives appear measurable"
fi

echo ""

# ============================================
# Success Metrics Validation
# ============================================

echo "📊 Validating success metrics..."

metrics_count=$(grep -A 20 "## Success Metrics" "$SPEC_FILE" | grep -c "^-\|^\*\*Metric")

if [ "$metrics_count" -eq 0 ]; then
    echo "  ❌ No success metrics defined"
    ((errors++))
elif [ "$metrics_count" -lt 2 ]; then
    echo "  ⚠️  Warning: Only $metrics_count metric (recommend 3-5)"
    ((warnings++))
else
    echo "  ✅ Found $metrics_count success metrics"
fi

# Check for baseline and target in metrics
metrics_with_target=$(grep -A 20 "## Success Metrics" "$SPEC_FILE" | grep -ic "target:\|baseline:")
if [ "$metrics_with_target" -lt "$metrics_count" ]; then
    echo "  ⚠️  Warning: Some metrics may be missing baseline or target values"
    ((warnings++))
fi

echo ""

# ============================================
# Scope Validation
# ============================================

echo "🎯 Validating scope..."

if grep -q "### In Scope" "$SPEC_FILE" && grep -q "### Out of Scope" "$SPEC_FILE"; then
    echo "  ✅ Both In Scope and Out of Scope defined"

    in_scope_count=$(sed -n '/### In Scope/,/### Out of Scope/p' "$SPEC_FILE" | grep -c "^-")
    out_scope_count=$(sed -n '/### Out of Scope/,/^##/p' "$SPEC_FILE" | grep -c "^-")

    echo "     In Scope: $in_scope_count items"
    echo "     Out of Scope: $out_scope_count items"

    if [ "$out_scope_count" -eq 0 ]; then
        echo "  ⚠️  Warning: No items in 'Out of Scope' (setting boundaries is important)"
        ((warnings++))
    fi
else
    echo "  ❌ Missing 'In Scope' or 'Out of Scope' sections"
    ((errors++))
fi

echo ""

# ============================================
# Quality Checks
# ============================================

echo "🔍 Quality checks..."

# Check for TODO markers
todos=$(grep -ic "TODO\|TBD\|FIXME" "$SPEC_FILE")
if [ "$todos" -gt 0 ]; then
    echo "  ⚠️  Warning: $todos TODO/TBD markers found"
    echo "     Resolve before approving or document as open questions"
    ((warnings++))
else
    echo "  ✅ No TODO markers"
fi

# Check for open questions
open_questions=$(grep -c "^- \[ \]" "$SPEC_FILE")
if [ "$open_questions" -gt 0 ]; then
    echo "  ⚠️  Warning: $open_questions open questions/checkboxes"
    echo "     Consider resolving critical questions before approval"
    ((warnings++))
else
    echo "  ✅ No open questions"
fi

# Check for ambiguous terms
ambiguous_terms=("fast" "slow" "easy" "hard" "many" "few" "better" "worse" "soon" "later")
ambiguous_found=0

for term in "${ambiguous_terms[@]}"; do
    count=$(grep -iw "$term" "$SPEC_FILE" | grep -v "^#" | wc -l)
    if [ "$count" -gt 0 ]; then
        ((ambiguous_found+=$count))
    fi
done

if [ "$ambiguous_found" -gt 0 ]; then
    echo "  ⚠️  Warning: $ambiguous_found ambiguous terms found (fast, easy, many, etc.)"
    echo "     Consider quantifying: 'fast' → '< 200ms', 'many' → '1000+ users'"
    ((warnings++))
else
    echo "  ✅ No ambiguous terms detected"
fi

# Check for user journey
journey_exists=$(grep -c "### User Journey Map\|### Main Flow\|### Happy Path" "$SPEC_FILE")
if [ "$journey_exists" -eq 0 ]; then
    echo "  ⚠️  Warning: No user journey documented"
    ((warnings++))
else
    echo "  ✅ User journey documented"
fi

# Check for UI/UX references
ui_refs=$(grep -c "### UI/UX References\|Figma\|wireframe\|prototype" "$SPEC_FILE")
if [ "$ui_refs" -eq 0 ]; then
    echo "  ⚠️  Warning: No UI/UX references found (Figma, wireframes, etc.)"
    ((warnings++))
else
    echo "  ✅ UI/UX references documented"
fi

echo ""

# ============================================
# E2E Test Scenarios Validation
# ============================================

echo "🧪 Validating E2E test scenarios..."

# Check for E2E section
if grep -q "## Critical E2E Test Scenarios" "$SPEC_FILE"; then
    echo "  ✅ Critical E2E Test Scenarios section exists"

    # Count E2E scenarios
    e2e_count=$(grep -c "^### E2E-" "$SPEC_FILE")

    if [ "$e2e_count" -eq 0 ]; then
        echo "  ❌ No E2E scenarios defined (expected format: ### E2E-1: Title)"
        ((errors++))
    elif [ "$e2e_count" -lt 2 ]; then
        echo "  ⚠️  Warning: Only $e2e_count E2E scenario (recommend at least 2 critical scenarios)"
        ((warnings++))
    else
        echo "  ✅ Found $e2e_count E2E scenarios"
    fi

    # Check for critical priority scenarios
    critical_count=$(grep -c "🔴 Critical" "$SPEC_FILE")
    if [ "$critical_count" -eq 0 ]; then
        echo "  ⚠️  Warning: No critical (🔴) E2E scenarios marked"
        ((warnings++))
    else
        echo "  ✅ $critical_count critical E2E scenario(s) marked"
    fi

    # Check for E2E summary table
    if grep -q "### E2E Test Summary" "$SPEC_FILE"; then
        echo "  ✅ E2E Test Summary table exists"
    else
        echo "  ⚠️  Warning: Missing E2E Test Summary table"
        ((warnings++))
    fi
else
    echo "  ❌ Missing 'Critical E2E Test Scenarios' section"
    echo "     E2E scenarios are required to define what must be tested before release"
    ((errors++))
fi

echo ""

# ============================================
# Summary
# ============================================

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ "$errors" -eq 0 ]; then
    echo "✅ Functional Spec Validation PASSED"

    if [ "$warnings" -gt 0 ]; then
        echo ""
        echo "⚠️  $warnings warning(s) found (non-blocking)"
        echo "   Consider addressing before approval"
    fi

    echo ""
    echo "Next: /sdd.spec functional --approve"
    exit 0
else
    echo "❌ Functional Spec Validation FAILED"
    echo ""
    echo "   Errors: $errors (must fix)"
    echo "   Warnings: $warnings (optional)"
    echo ""
    echo "How to fix:"
    echo "• Edit: $SPEC_FILE"
    echo "• Use: /sdd.spec functional --include to fill gaps"
    echo "• Re-validate: $0 $FEATURE_PATH"
    exit 1
fi
