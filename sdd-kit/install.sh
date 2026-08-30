#!/usr/bin/env bash
# =============================================================================
# SDD Kit — Installer
#
# Installs SDD Kit into any project by copying agentic files to .claude/
# and registering CLAUDE.md reference.
#
# Usage:
#   bash sdd-kit/install.sh                  # install in current directory
#   bash sdd-kit/install.sh /path/to/project # install in specific project
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET_DIR="${1:-$(pwd)}"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

ok()   { echo -e "  ${GREEN}✓${NC} $1"; }
warn() { echo -e "  ${YELLOW}⚠${NC} $1"; }
fail() { echo -e "  ${RED}✗${NC} $1"; exit 1; }

echo ""
echo "============================================================"
echo "  SDD Kit — Installer"
echo "============================================================"
echo "  Target: $TARGET_DIR"
echo ""

# ── Validate target ───────────────────────────────────────────────────────────
[[ -d "$TARGET_DIR" ]] || fail "Target directory does not exist: $TARGET_DIR"

# ── Create .claude directories ────────────────────────────────────────────────
echo "Creating .claude directories..."
mkdir -p "$TARGET_DIR/.claude/commands"
mkdir -p "$TARGET_DIR/.claude/skills"
mkdir -p "$TARGET_DIR/.claude/agents"
ok ".claude/ structure ready"

# ── Copy commands ─────────────────────────────────────────────────────────────
echo ""
echo "Installing commands..."
if [[ -d "$SCRIPT_DIR/.claude/commands" ]]; then
    cp -r "$SCRIPT_DIR/.claude/commands/". "$TARGET_DIR/.claude/commands/"
    COUNT=$(ls "$TARGET_DIR/.claude/commands/"*.md 2>/dev/null | wc -l | tr -d ' ')
    ok "$COUNT commands installed → .claude/commands/"
else
    warn "commands directory not found in sdd-kit"
fi

# ── Copy skills ───────────────────────────────────────────────────────────────
echo ""
echo "Installing skills..."
if [[ -d "$SCRIPT_DIR/.claude/skills" ]]; then
    for skill_dir in "$SCRIPT_DIR/.claude/skills/"/*/; do
        skill_name="$(basename "$skill_dir")"
        mkdir -p "$TARGET_DIR/.claude/skills/$skill_name"
        cp -r "$skill_dir". "$TARGET_DIR/.claude/skills/$skill_name/"
        ok "  skill: $skill_name"
    done
else
    warn "skills directory not found in sdd-kit"
fi

# ── Copy agents ───────────────────────────────────────────────────────────────
echo ""
echo "Installing agents..."
if [[ -d "$SCRIPT_DIR/.claude/agents" ]]; then
    cp -r "$SCRIPT_DIR/.claude/agents/". "$TARGET_DIR/.claude/agents/"
    COUNT=$(ls "$TARGET_DIR/.claude/agents/"*.md 2>/dev/null | wc -l | tr -d ' ')
    ok "$COUNT agents installed → .claude/agents/"
else
    warn "agents directory not found in sdd-kit"
fi

# ── Register CLAUDE.md reference ──────────────────────────────────────────────
echo ""
echo "Registering CLAUDE.md reference..."

CLAUDE_MD="$TARGET_DIR/CLAUDE.md"
SDD_REF="@sdd-kit/CLAUDE.md"

if [[ -f "$CLAUDE_MD" ]]; then
    if grep -qF "$SDD_REF" "$CLAUDE_MD"; then
        warn "CLAUDE.md already references sdd-kit — skipping"
    else
        echo "" >> "$CLAUDE_MD"
        echo "# SDD Kit" >> "$CLAUDE_MD"
        echo "$SDD_REF" >> "$CLAUDE_MD"
        ok "Added @sdd-kit/CLAUDE.md reference to CLAUDE.md"
    fi
else
    cat > "$CLAUDE_MD" << EOF
# Project AI Configuration

$SDD_REF
EOF
    ok "Created CLAUDE.md with sdd-kit reference"
fi

# ── Create sdd/ working directories ──────────────────────────────────────────
echo ""
echo "Creating sdd/ working directories..."
mkdir -p "$TARGET_DIR/sdd/wip"
mkdir -p "$TARGET_DIR/sdd/features"

# Add .gitkeep to keep empty dirs in git
touch "$TARGET_DIR/sdd/wip/.gitkeep"
touch "$TARGET_DIR/sdd/features/.gitkeep"
ok "sdd/wip/ and sdd/features/ created"

# ── Update .gitignore ─────────────────────────────────────────────────────────
echo ""
echo "Updating .gitignore..."
GITIGNORE="$TARGET_DIR/.gitignore"
GITIGNORE_ENTRIES=(
    "# SDD Kit"
    "sdd/wip/*/progress.md"
)

if [[ -f "$GITIGNORE" ]]; then
    for entry in "${GITIGNORE_ENTRIES[@]}"; do
        if ! grep -qF "$entry" "$GITIGNORE"; then
            echo "$entry" >> "$GITIGNORE"
        fi
    done
    ok ".gitignore updated"
else
    printf '%s\n' "${GITIGNORE_ENTRIES[@]}" > "$GITIGNORE"
    ok ".gitignore created"
fi

# ── Done ──────────────────────────────────────────────────────────────────────
echo ""
echo "============================================================"
echo -e "  ${GREEN}✓ SDD Kit installed successfully!${NC}"
echo "============================================================"
echo ""
echo "  Next steps:"
echo "  1. Open your project in Claude Code"
echo "  2. Run: /sdd.project   (configure your project)"
echo "  3. Run: /sdd.go \"my first feature\""
echo ""
