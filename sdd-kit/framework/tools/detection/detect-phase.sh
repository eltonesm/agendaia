#!/bin/bash
# detect-phase.sh - Detect current phase from meta.md
# Usage: detect-phase.sh <FEATURE_PATH> [--json]

set -e

FEATURE_PATH="${1:-.}"
JSON_OUTPUT=false
[[ "$2" == "--json" ]] && JSON_OUTPUT=true

META_FILE="$FEATURE_PATH/meta.md"
[ ! -f "$META_FILE" ] && { echo "❌ meta.md not found at $META_FILE"; exit 1; }

# Method 1: Read "Current Stage:" field
current_stage=$(grep -i "^Current Stage:" "$META_FILE" 2>/dev/null | head -1 | cut -d: -f2 | tr -d ' ' | tr '[:upper:]' '[:lower:]')

# Method 2: Infer from stages YAML if field missing
if [ -z "$current_stage" ]; then
    impl_status=$(grep -A3 "implementation:" "$META_FILE" 2>/dev/null | grep "status:" | head -1 | sed 's/.*: *//')
    tasks_status=$(grep -A3 "tasks:" "$META_FILE" 2>/dev/null | grep "status:" | head -1 | sed 's/.*: *//')
    tech_status=$(grep -A3 "technical:" "$META_FILE" 2>/dev/null | grep "status:" | head -1 | sed 's/.*: *//')

    if [ "$impl_status" = "in-progress" ]; then current_stage="implementation"
    elif [ "$tasks_status" = "approved" ] || [ "$tasks_status" = "in-progress" ]; then current_stage="tasks"
    elif [ "$tech_status" = "approved" ] || [ "$tech_status" = "in-progress" ]; then current_stage="technical"
    else current_stage="functional"; fi
fi

# Map stage to phase number
case "$current_stage" in
    functional) phase=1 ;;
    technical) phase=2 ;;
    tasks) phase=3 ;;
    implementation) phase=4 ;;
    *) echo "❌ Unknown stage: $current_stage"; exit 1 ;;
esac

# Determine available layers
case $phase in
    1) layers="functional" ;;
    2) layers="functional technical" ;;
    3) layers="functional technical tasks" ;;
    4) layers="functional technical tasks code" ;;
esac

if [ "$JSON_OUTPUT" = true ]; then
    echo "{\"phase\":$phase,\"stage\":\"$current_stage\",\"layers\":\"$layers\"}"
else
    echo "📊 Phase: $phase ($current_stage)"
    echo "   Available layers: $layers"
fi
exit 0
