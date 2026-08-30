---
name: context-guardian
description: Context monitoring skill to prevent token exhaustion. Monitors context usage and recommends actions.
---

# Context Guardian - Context Monitoring Skill

**Version**: 1.4.0
**Added**: v2.4.0
**Purpose**: Monitor context usage and recommend actions to prevent exhaustion

---

## When to Use This Skill

Invoke this skill:
- Before starting a new phase (`/sdd.spec`, `/sdd.plan`, `/sdd.build`)
- After reading multiple large files (5+)
- After multiple MCP queries (3+)
- When you feel the conversation is getting long
- User says: "check context", "how much context", "context status"

---

## How to Invoke

```
Skill(skill="context-guardian")
```

---

## Assessment Protocol

When invoked, perform this assessment:

### Step 1: Estimate Context Usage

Count approximate token usage from:

| Source | Estimation Method | Typical Cost |
|--------|-------------------|--------------|
| **Conversation turns** | ~500 tokens per turn average | Count turns x 500 |
| **Files read** | ~20 tokens per line | Sum lines read x 20 |
| **MCP responses** | ~1500 tokens average | Count MCP calls x 1500 |
| **Large operations** | Variable | Track heavy operations |

### Step 2: Calculate Percentage

```
estimated_tokens = (turns x 500) + (lines_read x 20) + (mcp_calls x 1500) + heavy_ops
estimated_percentage = (estimated_tokens / 200000) x 100  # Assuming 200K context
```

### Step 3: Determine Status

| Percentage | Status | Color |
|------------|--------|-------|
| 0-40% | `NORMAL` | Green |
| 40-60% | `ELEVATED` | Yellow |
| 60-80% | `DELEGATE_MODE` | Orange |
| 80%+ | `CRITICAL` | Red |

---

## Output Format

```
+---------------------------------------------------------------+
| CONTEXT GUARDIAN REPORT                                         |
+---------------------------------------------------------------+
|                                                                 |
| Estimated Usage: ~[XX]% ([STATUS])                              |
|                                                                 |
| Breakdown:                                                      |
|   - Conversation history: ~[X] turns (~[Y]%)                   |
|   - Files read: [N] files (~[Y]%)                               |
|   - MCP responses: [N] calls (~[Y]%)                            |
|   - Heavy operations: [list if any]                             |
|                                                                 |
| Recommendations:                                                |
|   [Numbered list based on status]                               |
|                                                                 |
| Status: [STATUS] - [One-line description]                       |
+---------------------------------------------------------------+
```

---

## Recommendations by Status

### NORMAL (0-40%)

```
Recommendations:
  1. All operations can proceed inline
  2. No immediate action needed
  3. Continue with current workflow

Status: NORMAL - Plenty of context available
```

### ELEVATED (40-60%)

```
Recommendations:
  1. Prefer subagents for heavy operations
  2. Use  for MCP queries
  3. Use Explore agent for file searches
  4. Consider completing current phase soon
  5. At phase transitions: Consider /clear — specs contain all decisions, fresh context produces higher quality

Status: ELEVATED - Prefer delegation for heavy operations
```

### DELEGATE_MODE (60-80%)

```
Recommendations:
  1. MANDATORY: Use subagents for all heavy operations
  2. Use  for ANY MCP query
  3. Use sdd-validator-runner for validation
  4. Avoid reading large files directly
  5. Strongly consider /clear at next phase transition — specs are source of truth, fresh context = higher quality
  6. If not at phase boundary: consider compaction

Status: DELEGATE_MODE - Mandatory subagent delegation
```

### CRITICAL (80%+)

```
Recommendations:
  1. IMMEDIATE: /clear recommended — specs contain all decisions, start fresh
  2. If /clear not possible: Run genai-compact-state.sh to compress state
  3. Complete current task then optimize context
  4. Alternative: Start new session with state file

Status: CRITICAL - Context optimization recommended

To clear and restart:
  /clear then resume with /sdd.build (specs are source of truth)

To compact instead:
  bash ~/.sdd-kit/tools/genai/genai-compact-state.sh sdd/wip/[feature] --level MINIMAL
```

---

## Delegation & Compaction References

Based on the assessed status, load the relevant reference:

| Status | Action |
|--------|--------|
| NORMAL / ELEVATED | No reference needed — use inline recommendations above |
| DELEGATE_MODE | Read `references/delegation-rules.md` for mandatory delegation rules |
| CRITICAL | Read `references/compaction-guide.md` for compression level selection |
| Phase transition | Read `references/compaction-guide.md` for auto-compact triggers |

---

## Heavy Operations Reference

Operations that consume significant context:

| Operation | Est. Tokens | Delegation |
|-----------|-------------|------------|
| PROJECT.md wizard | ~15,000 | sdd-project-wizard |
| Full MCP SDK docs | ~3,000 |  |
| MCP API specs | ~5,000 |  |
| Code review (full) | ~2,000 | sdd-validator-runner |
| System design | ~5,000 | sdd-system-designer |
| Large file (500+ lines) | ~10,000 | Explore agent |
| Multiple file search | ~3,000 | Explore agent |

---

## Tracking Tips

### What to Track Mentally

1. **Conversation length**: Long back-and-forth = high usage
2. **Files read**: Each `Read` adds to context
3. **MCP calls**: Each direct MCP call is expensive
4. **Errors/retries**: Failed operations still consume context

### Signs of High Context (Without Counting)

- Agent responses becoming slower
- Agent "forgetting" earlier decisions
- Need to repeat context from earlier
- Complex tasks feeling harder than they should

---

## Integration with Commands

### Automatic Check Points

The following commands should check context:

| Command | When to Check | Threshold for Warning |
|---------|---------------|----------------------|
| `/sdd.start` | Before PROJECT.md wizard | 30% |
| `/sdd.spec technical` | Before MCP queries | 40% |
| `/sdd.plan` | Before task generation | 50% |
| `/sdd.build` | Before implementation | 40% |
| `/sdd.finish` | Before final validation | 60% |

### Example Integration

```markdown
## In /sdd.build

Before starting implementation:

1. Estimate current context usage
2. If > 60%: Show context advisory
3. If > 80%: Recommend compaction first
4. Proceed with appropriate delegation level
```

---

## User Triggers

Natural language phrases that should invoke this skill:

- "check context"
- "context status"
- "how much context"
- "am I running low on context"
- "context guardian"
- "check memory"

---

## Example Report

```
+---------------------------------------------------------------+
| CONTEXT GUARDIAN REPORT                                         |
+---------------------------------------------------------------+
|                                                                 |
| Estimated Usage: ~67% (DELEGATE_MODE)                           |
|                                                                 |
| Breakdown:                                                      |
|   - Conversation history: ~45 turns (~35%)                      |
|   - Files read: 8 files (~20%)                                  |
|   - MCP responses: 4 calls (~12%)                               |
|   - Heavy operations: system-designer invoked                   |
|                                                                 |
| Recommendations:                                                |
|   1. MANDATORY: Use subagents for all heavy operations          |
|   2. Use  for ANY MCP query                     |
|   3. Use sdd-validator-runner for validation                   |
|   4. Avoid reading large files directly                         |
|   5. Consider compaction before /sdd.build                     |
|                                                                 |
| Status: DELEGATE_MODE - Mandatory subagent delegation           |
+---------------------------------------------------------------+
```

---

## Related Components

- **genai-compact-state.sh**: Compresses context when CRITICAL
- **CONTEXT_STEWARD.md**: Full system documentation
- **Context Budget Protocol**: Monitor token usage and trigger compaction when needed

---

## Version History

- **v1.4.0** (2026-03-03): Refactored — extracted delegation rules and compaction guide to `references/` for lazy-loading
- **v1.3.0** (2026-01-21): Added auto-compact recommendations for phase transitions
- **v1.2.0** (2026-01-21): Added compression level selection (MINIMAL/STANDARD/FULL)
- **v1.1.0** (2026-01-19): Added MCP delegation rules
- **v1.0.0** (2026-01-12): Initial context-guardian skill
