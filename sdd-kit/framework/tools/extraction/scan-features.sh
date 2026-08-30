#!/bin/bash
# scan-features.sh - List features with sorting and migration
# Usage: scan-features.sh [--migrate] [--json]

set -e

MIGRATE=false
JSON_OUTPUT=false
while [[ $# -gt 0 ]]; do
    case $1 in
        --migrate) MIGRATE=true; shift ;;
        --json) JSON_OUTPUT=true; shift ;;
        *) shift ;;
    esac
done

# Detect features without date prefix (don't start with 8 digits)
unnumbered_wip=$(ls -1 sdd/wip/ 2>/dev/null | grep -vE '^[0-9]{8}-' || true)
unnumbered_features=$(ls -1 sdd/features/ 2>/dev/null | grep -vE '^[0-9]{8}-' || true)

if [ -n "$unnumbered_wip" ] || [ -n "$unnumbered_features" ]; then
    echo "⚠️  Features without date prefix detected" >&2
    if [ "$MIGRATE" = true ]; then
        echo "🔄 Migrating..." >&2
        today=$(date +%Y%m%d)

        # Migrate WIP
        for f in $unnumbered_wip; do
            # Strip legacy NNN- prefix if present
            name=$(echo "$f" | sed 's/^[0-9]\{3\}-//')
            new_name="${today}-${name}"
            mv "sdd/wip/$f" "sdd/wip/$new_name" 2>/dev/null && echo "  $f → $new_name" >&2
        done
    fi
fi

# Collect features
wip_count=0
completed_count=0

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔄 WIP Features"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
if [ -d "sdd/wip" ]; then
    for f in $(ls -1 sdd/wip 2>/dev/null | sort); do
        feat_date=$(echo "$f" | grep -oE '^[0-9]{8}' || echo "--------")
        name=$(echo "$f" | sed 's/^[0-9]*-//')
        stage=$(grep -i "Current Stage:" "sdd/wip/$f/meta.md" 2>/dev/null | cut -d: -f2 | tr -d ' ' || echo "unknown")
        echo "  $feat_date  $name  ($stage)"
        wip_count=$((wip_count + 1))
    done
fi
echo "Total WIP: $wip_count"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ Completed Features"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
if [ -d "sdd/features" ]; then
    for f in $(ls -1 sdd/features 2>/dev/null | sort); do
        feat_date=$(echo "$f" | grep -oE '^[0-9]{8}' || echo "--------")
        name=$(echo "$f" | sed 's/^[0-9]*-//')
        echo "  $feat_date  $name"
        completed_count=$((completed_count + 1))
    done
fi
echo "Total Completed: $completed_count"
echo ""
echo "Total: $((wip_count + completed_count)) features"
