#!/bin/bash
# SDD SDD Kit - Backlog Manager
# CRUD operations for meli/backlog.md
# Replaces LLM-based backlog management (saves ~2,000 tokens per operation)
#
# Usage: manage-backlog.sh <command> [options]
#   Commands:
#     list [--category TODO|DEBT|IDEA] [--json]
#     add <category> <title> [--priority HIGH|MEDIUM|LOW] [--description "text"]
#     remove <id>
#     update <id> [--status done|wip] [--priority HIGH|MEDIUM|LOW]
#     convert <id> --to-feature  # Convert to feature spec

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ============================================
# Configuration
# ============================================

BACKLOG_FILE="meli/backlog.md"
COMMAND="${1:-list}"
OUTPUT_JSON=false

shift 2>/dev/null || true

# ============================================
# Parse Arguments
# ============================================

CATEGORY=""
TITLE=""
PRIORITY="MEDIUM"
DESCRIPTION=""
ITEM_ID=""
STATUS=""
TO_FEATURE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --json) OUTPUT_JSON=true; shift ;;
        --category|-c) CATEGORY="$2"; shift 2 ;;
        --priority|-p) PRIORITY="$2"; shift 2 ;;
        --description|-d) DESCRIPTION="$2"; shift 2 ;;
        --status|-s) STATUS="$2"; shift 2 ;;
        --to-feature) TO_FEATURE=true; shift ;;
        TODO|DEBT|IDEA) CATEGORY="$1"; shift ;;
        HIGH|MEDIUM|LOW) PRIORITY="$1"; shift ;;
        *)
            if [ -z "$TITLE" ] && [ "$COMMAND" = "add" ]; then
                # First positional after command+category is title
                if [ -n "$CATEGORY" ]; then
                    TITLE="$1"
                else
                    CATEGORY="$1"
                fi
            elif [ -z "$ITEM_ID" ] && [[ "$COMMAND" =~ ^(remove|update|convert)$ ]]; then
                ITEM_ID="$1"
            fi
            shift
            ;;
    esac
done

# ============================================
# Initialize Backlog File
# ============================================

init_backlog() {
    if [ ! -f "$BACKLOG_FILE" ]; then
        mkdir -p "$(dirname "$BACKLOG_FILE")"
        cat > "$BACKLOG_FILE" << 'EOF'
# Backlog

## TODO
<!-- High-priority items to implement -->

## DEBT
<!-- Technical debt to address -->

## IDEA
<!-- Future ideas to explore -->
EOF
    fi
}

# ============================================
# List Items
# ============================================

list_items() {
    init_backlog

    if [ ! -f "$BACKLOG_FILE" ]; then
        if [ "$OUTPUT_JSON" = true ]; then
            echo '{"items":[],"count":0}'
        else
            echo "No backlog file found"
        fi
        return
    fi

    python3 << PYTHON_SCRIPT
import re
import json

backlog_file = "$BACKLOG_FILE"
filter_category = "$CATEGORY".upper() if "$CATEGORY" else None

items = []
current_category = None

with open(backlog_file, 'r') as f:
    content = f.read()

# Parse sections
sections = re.split(r'^## (TODO|DEBT|IDEA)', content, flags=re.MULTILINE)

i = 1
while i < len(sections):
    category = sections[i]
    section_content = sections[i + 1] if i + 1 < len(sections) else ""

    if filter_category and category != filter_category:
        i += 2
        continue

    # Parse items in section
    # Format: - [ ] [PRIORITY] ITEM-XXX: Title
    # or: - [x] [PRIORITY] ITEM-XXX: Title
    pattern = r'^- \[([ x])\] \[?(HIGH|MEDIUM|LOW)?\]?\s*(?:([A-Z]+-\d+):?\s*)?(.+?)$'

    for line_num, line in enumerate(section_content.split('\n')):
        match = re.match(pattern, line.strip())
        if match:
            done = match.group(1) == 'x'
            priority = match.group(2) or 'MEDIUM'
            item_id = match.group(3) or f"{category[:1]}{len(items)+1:03d}"
            title = match.group(4).strip()

            items.append({
                'id': item_id,
                'category': category,
                'title': title,
                'priority': priority,
                'done': done,
                'status': 'done' if done else 'pending'
            })

    i += 2

# Sort by priority
priority_order = {'HIGH': 0, 'MEDIUM': 1, 'LOW': 2}
items.sort(key=lambda x: (priority_order.get(x['priority'], 1), x['id']))

output_json = "$OUTPUT_JSON" == "true"
if output_json:
    print(json.dumps({
        'items': items,
        'count': len(items),
        'by_category': {
            'TODO': len([i for i in items if i['category'] == 'TODO']),
            'DEBT': len([i for i in items if i['category'] == 'DEBT']),
            'IDEA': len([i for i in items if i['category'] == 'IDEA'])
        }
    }, indent=2))
else:
    print("=== Backlog ===\n")
    for cat in ['TODO', 'DEBT', 'IDEA']:
        cat_items = [i for i in items if i['category'] == cat]
        if cat_items or not filter_category:
            print(f"## {cat} ({len(cat_items)} items)")
            for item in cat_items:
                status = '✓' if item['done'] else ' '
                print(f"  [{status}] [{item['priority']}] {item['id']}: {item['title']}")
            print()
PYTHON_SCRIPT
}

# ============================================
# Add Item
# ============================================

add_item() {
    init_backlog

    if [ -z "$CATEGORY" ] || [ -z "$TITLE" ]; then
        echo "Usage: manage-backlog.sh add <TODO|DEBT|IDEA> <title> [--priority HIGH|MEDIUM|LOW]"
        exit 1
    fi

    CATEGORY=$(echo "$CATEGORY" | tr '[:lower:]' '[:upper:]')

    # Generate ID
    local prefix="${CATEGORY:0:1}"
    local count
    count=$(grep -c "^- \[" "$BACKLOG_FILE" 2>/dev/null || echo "0")
    local new_id="${prefix}$(printf '%03d' $((count + 1)))"

    # Build item line
    local item_line="- [ ] [$PRIORITY] $new_id: $TITLE"

    # Add description if provided
    if [ -n "$DESCRIPTION" ]; then
        item_line="$item_line\n  $DESCRIPTION"
    fi

    # Insert into correct section
    python3 << PYTHON_SCRIPT
import re

backlog_file = "$BACKLOG_FILE"
category = "$CATEGORY"
item_line = "$item_line"

with open(backlog_file, 'r') as f:
    content = f.read()

# Find the section and add item
pattern = f'^## {category}.*?(?=^## |\\Z)'
match = re.search(pattern, content, re.MULTILINE | re.DOTALL)

if match:
    section = match.group(0)
    # Add after section header and any comment
    lines = section.split('\n')
    insert_idx = 1
    for i, line in enumerate(lines[1:], 1):
        if line.strip() and not line.strip().startswith('<!--'):
            insert_idx = i
            break
        elif line.strip().startswith('<!--'):
            insert_idx = i + 1

    lines.insert(insert_idx, item_line)
    new_section = '\n'.join(lines)
    content = content[:match.start()] + new_section + content[match.end():]

with open(backlog_file, 'w') as f:
    f.write(content)

print(f"Added: {item_line.split(chr(10))[0]}")
PYTHON_SCRIPT

    if [ "$OUTPUT_JSON" = true ]; then
        echo "{\"success\":true,\"id\":\"$new_id\",\"category\":\"$CATEGORY\",\"title\":\"$TITLE\"}"
    fi
}

# ============================================
# Remove Item
# ============================================

remove_item() {
    if [ -z "$ITEM_ID" ]; then
        echo "Usage: manage-backlog.sh remove <id>"
        exit 1
    fi

    python3 << PYTHON_SCRIPT
import re

backlog_file = "$BACKLOG_FILE"
item_id = "$ITEM_ID"

with open(backlog_file, 'r') as f:
    lines = f.readlines()

found = False
new_lines = []
skip_description = False

for line in lines:
    if item_id in line and line.strip().startswith('- ['):
        found = True
        skip_description = True
        continue
    elif skip_description and line.startswith('  ') and not line.strip().startswith('- ['):
        continue
    else:
        skip_description = False
        new_lines.append(line)

if found:
    with open(backlog_file, 'w') as f:
        f.writelines(new_lines)
    print(f"Removed: {item_id}")
else:
    print(f"Item not found: {item_id}")
    exit(1)
PYTHON_SCRIPT
}

# ============================================
# Update Item
# ============================================

update_item() {
    if [ -z "$ITEM_ID" ]; then
        echo "Usage: manage-backlog.sh update <id> [--status done|wip] [--priority HIGH|MEDIUM|LOW]"
        exit 1
    fi

    python3 << PYTHON_SCRIPT
import re

backlog_file = "$BACKLOG_FILE"
item_id = "$ITEM_ID"
new_status = "$STATUS"
new_priority = "$PRIORITY" if "$PRIORITY" != "MEDIUM" or "$STATUS" else None

with open(backlog_file, 'r') as f:
    content = f.read()

# Find and update the item
pattern = rf'^(- \[)([x ])\](\s*\[)?(HIGH|MEDIUM|LOW)?(\]?\s*{re.escape(item_id)}:?.*)$'

def replace_item(match):
    prefix = match.group(1)
    current_done = match.group(2)
    bracket = match.group(3) or ' ['
    current_priority = match.group(4) or 'MEDIUM'
    rest = match.group(5)

    # Update status
    if new_status == 'done':
        done_char = 'x'
    elif new_status == 'wip':
        done_char = ' '
    else:
        done_char = current_done

    # Update priority
    priority = new_priority or current_priority

    return f"{prefix}{done_char}] [{priority}]{rest}"

new_content, count = re.subn(pattern, replace_item, content, flags=re.MULTILINE)

if count > 0:
    with open(backlog_file, 'w') as f:
        f.write(new_content)
    print(f"Updated: {item_id}")
else:
    print(f"Item not found: {item_id}")
    exit(1)
PYTHON_SCRIPT
}

# ============================================
# Convert to Feature
# ============================================

convert_to_feature() {
    if [ -z "$ITEM_ID" ]; then
        echo "Usage: manage-backlog.sh convert <id> --to-feature"
        exit 1
    fi

    # Extract item details
    local item_info
    item_info=$(python3 << PYTHON_SCRIPT
import re
import json

backlog_file = "$BACKLOG_FILE"
item_id = "$ITEM_ID"

with open(backlog_file, 'r') as f:
    content = f.read()

# Find the item
pattern = rf'^- \[[ x]\] \[?(HIGH|MEDIUM|LOW)?\]?\s*{re.escape(item_id)}:?\s*(.+?)$'
match = re.search(pattern, content, re.MULTILINE)

if match:
    print(json.dumps({
        'id': item_id,
        'priority': match.group(1) or 'MEDIUM',
        'title': match.group(2).strip()
    }))
else:
    print('{}')
PYTHON_SCRIPT
)

    if [ "$item_info" = "{}" ]; then
        echo "Item not found: $ITEM_ID"
        exit 1
    fi

    local title
    title=$(echo "$item_info" | python3 -c "import json,sys; print(json.load(sys.stdin).get('title',''))")

    echo "To convert '$title' to a feature, run:"
    echo ""
    echo "  /sdd.start \"$title\""
    echo ""
    echo "After creating the feature, remove from backlog with:"
    echo "  manage-backlog.sh remove $ITEM_ID"
}

# ============================================
# Main
# ============================================

case "$COMMAND" in
    list|ls)
        list_items
        ;;
    add)
        add_item
        ;;
    remove|rm|delete)
        remove_item
        ;;
    update|edit)
        update_item
        ;;
    convert)
        if [ "$TO_FEATURE" = true ]; then
            convert_to_feature
        else
            echo "Usage: manage-backlog.sh convert <id> --to-feature"
            exit 1
        fi
        ;;
    *)
        echo "Unknown command: $COMMAND"
        echo ""
        echo "Usage: manage-backlog.sh <command> [options]"
        echo "Commands: list, add, remove, update, convert"
        exit 1
        ;;
esac
