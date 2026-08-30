#!/bin/bash
# Display tasks from tasks.json for approval
# Usage: display-tasks.sh <tasks.json>
#
# Outputs a markdown table with all tasks for user review
# Works without jq (uses portable grep/sed)

set -e

# Get tasks file path (support glob patterns)
if [ -n "$1" ]; then
    # Expand glob pattern if needed
    TASKS_FILE=$(ls -1 $1 2>/dev/null | head -1)
else
    # Default: find tasks.json in current wip feature
    TASKS_FILE=$(ls -1 sdd/wip/*/3-tasks/tasks.json 2>/dev/null | head -1)
fi

if [ -z "$TASKS_FILE" ] || [ ! -f "$TASKS_FILE" ]; then
    echo "Error: No tasks.json found"
    echo "Usage: display-tasks.sh <path-to-tasks.json>"
    exit 1
fi

echo "## Tasks for Approval"
echo ""
echo "**Source**: \`$TASKS_FILE\`"
echo ""
echo "| ID | Title | Layer | Complexity | Dependencies |"
echo "|----|-------|-------|------------|--------------|"

# Parse JSON without jq using grep/sed (portable)
# Extract each task block and parse fields

# Count tasks
total=0

# Read file and process line by line
in_task=false
current_id=""
current_title=""
current_layer=""
current_complexity=""
current_deps=""

while IFS= read -r line; do
    # Detect start of task object
    if echo "$line" | grep -q '"id"[[:space:]]*:'; then
        current_id=$(echo "$line" | sed 's/.*"id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/')
        in_task=true
    fi

    if [ "$in_task" = true ]; then
        # Extract title
        if echo "$line" | grep -q '"title"[[:space:]]*:'; then
            current_title=$(echo "$line" | sed 's/.*"title"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/')
            # Truncate long titles
            if [ ${#current_title} -gt 50 ]; then
                current_title="${current_title:0:47}..."
            fi
        fi

        # Extract layer
        if echo "$line" | grep -q '"layer"[[:space:]]*:'; then
            current_layer=$(echo "$line" | sed 's/.*"layer"[[:space:]]*:[[:space:]]*\([0-9]*\).*/\1/')
        fi

        # Extract complexity (if present)
        if echo "$line" | grep -q '"complexity"[[:space:]]*:'; then
            current_complexity=$(echo "$line" | sed 's/.*"complexity"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/')
        fi

        # Extract dependencies (simplified - just count)
        if echo "$line" | grep -q '"depends_on"[[:space:]]*:'; then
            deps_line=$(echo "$line" | sed 's/.*"depends_on"[[:space:]]*:[[:space:]]*\[\([^]]*\)\].*/\1/')
            if [ -z "$deps_line" ] || [ "$deps_line" = "[]" ] || [ "$deps_line" = "$line" ]; then
                current_deps="-"
            else
                # Extract task IDs from deps
                current_deps=$(echo "$deps_line" | tr ',' '\n' | sed 's/[" ]//g' | tr '\n' ',' | sed 's/,$//')
                if [ -z "$current_deps" ]; then
                    current_deps="-"
                fi
            fi
        fi

        # Detect end of task object (look for closing brace or next id)
        # Output when we have all required fields
        if [ -n "$current_id" ] && [ -n "$current_title" ] && [ -n "$current_layer" ]; then
            # Check if we hit a new task or end of tasks array
            if echo "$line" | grep -qE '\}[,]?[[:space:]]*$'; then
                # Set defaults for optional fields
                [ -z "$current_complexity" ] && current_complexity="Medium"
                [ -z "$current_deps" ] && current_deps="-"

                # Output row
                echo "| $current_id | $current_title | $current_layer | $current_complexity | $current_deps |"
                total=$((total + 1))

                # Reset for next task
                in_task=false
                current_id=""
                current_title=""
                current_layer=""
                current_complexity=""
                current_deps=""
            fi
        fi
    fi
done < "$TASKS_FILE"

echo ""
echo "**Total: $total tasks**"

# Show layer summary
echo ""
echo "### Layer Summary"
layer1=$(grep -o '"layer"[[:space:]]*:[[:space:]]*1' "$TASKS_FILE" | wc -l | tr -d ' ')
layer2=$(grep -o '"layer"[[:space:]]*:[[:space:]]*2' "$TASKS_FILE" | wc -l | tr -d ' ')
layer3=$(grep -o '"layer"[[:space:]]*:[[:space:]]*3' "$TASKS_FILE" | wc -l | tr -d ' ')

echo "- Layer 1 (Local): $layer1 tasks"
echo "- Layer 2 (): $layer2 tasks"
echo "- Layer 3 (Quality): $layer3 tasks"
