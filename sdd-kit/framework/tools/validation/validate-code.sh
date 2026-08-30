#!/bin/bash
# SDD SDD Kit - Deterministic Code Validator
# Pattern-based security, performance, and quality scanning
# Replaces LLM-based pattern detection in sdd-validator-runner, sdd-code-reviewer, sdd-performance-expert
#
# Usage: validate-code.sh [path] [--json] [--security] [--performance] [--quality] [--all]
# Returns: JSON with findings organized by category and severity

set -e

# Source utilities if available
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/../shared/validator-utils.sh" ]; then
    source "$SCRIPT_DIR/../shared/validator-utils.sh"
fi

# ============================================
# Configuration
# ============================================

PROJECT_PATH="${1:-.}"
OUTPUT_JSON=false
SCAN_SECURITY=false
SCAN_PERFORMANCE=false
SCAN_QUALITY=false

shift || true
while [[ $# -gt 0 ]]; do
    case $1 in
        --json) OUTPUT_JSON=true; shift ;;
        --security) SCAN_SECURITY=true; shift ;;
        --performance) SCAN_PERFORMANCE=true; shift ;;
        --quality) SCAN_QUALITY=true; shift ;;
        --all) SCAN_SECURITY=true; SCAN_PERFORMANCE=true; SCAN_QUALITY=true; shift ;;
        *) shift ;;
    esac
done

# Default: scan all
if [ "$SCAN_SECURITY" = false ] && [ "$SCAN_PERFORMANCE" = false ] && [ "$SCAN_QUALITY" = false ]; then
    SCAN_SECURITY=true
    SCAN_PERFORMANCE=true
    SCAN_QUALITY=true
fi

# Temporary files for results
SECURITY_FINDINGS=$(mktemp)
PERFORMANCE_FINDINGS=$(mktemp)
QUALITY_FINDINGS=$(mktemp)
trap "rm -f $SECURITY_FINDINGS $PERFORMANCE_FINDINGS $QUALITY_FINDINGS" EXIT

# Counters
SECURITY_CRITICAL=0
SECURITY_WARNING=0
PERF_CRITICAL=0
PERF_WARNING=0
QUALITY_CRITICAL=0
QUALITY_WARNING=0

# ============================================
# Helper: Scan for pattern
# ============================================

scan_pattern() {
    local pattern=$1
    local severity=$2
    local category=$3
    local description=$4
    local cwe=$5
    local output_file=$6

    # Find matches (exclude common false positive paths)
    local matches=$(grep -rn --include="*.java" --include="*.ts" --include="*.js" --include="*.py" --include="*.go" \
        -E "$pattern" "$PROJECT_PATH" 2>/dev/null | \
        grep -v "node_modules\|vendor\|\.git\|test\|Test\|spec\|Spec\|mock\|Mock" || true)

    if [ -n "$matches" ]; then
        while IFS= read -r match; do
            local file=$(echo "$match" | cut -d: -f1)
            local line=$(echo "$match" | cut -d: -f2)
            local code=$(echo "$match" | cut -d: -f3-)
            echo "{\"file\":\"$file\",\"line\":$line,\"severity\":\"$severity\",\"category\":\"$category\",\"description\":\"$description\",\"cwe\":\"$cwe\",\"code\":\"$(echo "$code" | sed 's/"/\\"/g' | head -c 100)\"}" >> "$output_file"
        done <<< "$matches"
    fi
}

# ============================================
# Security Patterns (OWASP Top 10)
# ============================================

scan_security() {
    echo "🔒 Scanning security patterns..." >&2

    # SQL Injection (CWE-89)
    scan_pattern 'executeQuery\s*\([^)]*\+' "CRITICAL" "sql_injection" "Potential SQL injection - string concatenation in query" "CWE-89" "$SECURITY_FINDINGS"
    scan_pattern 'createQuery\s*\([^)]*\+' "CRITICAL" "sql_injection" "Potential SQL injection - dynamic query creation" "CWE-89" "$SECURITY_FINDINGS"
    scan_pattern '\$\{.*\}.*SELECT|SELECT.*\$\{' "CRITICAL" "sql_injection" "Potential SQL injection - template in SQL" "CWE-89" "$SECURITY_FINDINGS"
    scan_pattern "f['\"].*SELECT.*\{|f['\"].*INSERT.*\{|f['\"].*UPDATE.*\{" "CRITICAL" "sql_injection" "Potential SQL injection - f-string in SQL (Python)" "CWE-89" "$SECURITY_FINDINGS"

    # XSS (CWE-79)
    scan_pattern 'innerHTML\s*=' "WARNING" "xss" "Potential XSS - innerHTML assignment" "CWE-79" "$SECURITY_FINDINGS"
    scan_pattern 'dangerouslySetInnerHTML' "WARNING" "xss" "Potential XSS - dangerouslySetInnerHTML" "CWE-79" "$SECURITY_FINDINGS"
    scan_pattern 'v-html\s*=' "WARNING" "xss" "Potential XSS - v-html directive" "CWE-79" "$SECURITY_FINDINGS"

    # Command Injection (CWE-78)
    scan_pattern 'Runtime\.getRuntime\(\)\.exec\s*\([^)]*\+' "CRITICAL" "command_injection" "Potential command injection - exec with concatenation" "CWE-78" "$SECURITY_FINDINGS"
    scan_pattern 'ProcessBuilder.*\+' "CRITICAL" "command_injection" "Potential command injection - ProcessBuilder with concatenation" "CWE-78" "$SECURITY_FINDINGS"
    scan_pattern 'os\.system\s*\([^)]*\+|subprocess\.(call|run|Popen)\s*\([^)]*\+' "CRITICAL" "command_injection" "Potential command injection - subprocess with concatenation" "CWE-78" "$SECURITY_FINDINGS"
    scan_pattern 'exec\s*\([^)]*\+' "WARNING" "command_injection" "Potential command injection - exec with concatenation" "CWE-78" "$SECURITY_FINDINGS"

    # Path Traversal (CWE-22)
    scan_pattern 'new File\s*\([^)]*\+' "WARNING" "path_traversal" "Potential path traversal - File with concatenation" "CWE-22" "$SECURITY_FINDINGS"
    scan_pattern 'Paths\.get\s*\([^)]*\+' "WARNING" "path_traversal" "Potential path traversal - Paths.get with concatenation" "CWE-22" "$SECURITY_FINDINGS"
    scan_pattern 'open\s*\([^)]*\+.*["\x27]r' "WARNING" "path_traversal" "Potential path traversal - file open with concatenation" "CWE-22" "$SECURITY_FINDINGS"

    # Hardcoded Secrets (CWE-798)
    scan_pattern 'password\s*=\s*["\x27][^"\x27]{8,}["\x27]' "CRITICAL" "hardcoded_secret" "Hardcoded password detected" "CWE-798" "$SECURITY_FINDINGS"
    scan_pattern 'api[_-]?key\s*=\s*["\x27][A-Za-z0-9]{16,}["\x27]' "CRITICAL" "hardcoded_secret" "Hardcoded API key detected" "CWE-798" "$SECURITY_FINDINGS"
    scan_pattern 'secret\s*=\s*["\x27][^"\x27]{8,}["\x27]' "CRITICAL" "hardcoded_secret" "Hardcoded secret detected" "CWE-798" "$SECURITY_FINDINGS"
    scan_pattern 'private[_-]?key\s*=\s*["\x27]' "CRITICAL" "hardcoded_secret" "Hardcoded private key detected" "CWE-798" "$SECURITY_FINDINGS"
    scan_pattern 'AWS_SECRET|GITHUB_TOKEN|SLACK_TOKEN' "WARNING" "hardcoded_secret" "Potential hardcoded token" "CWE-798" "$SECURITY_FINDINGS"

    # Insecure Deserialization (CWE-502)
    scan_pattern 'ObjectInputStream|readObject\(\)' "WARNING" "insecure_deserialization" "Potential insecure deserialization" "CWE-502" "$SECURITY_FINDINGS"
    scan_pattern 'pickle\.load|pickle\.loads' "WARNING" "insecure_deserialization" "Potential insecure deserialization - pickle" "CWE-502" "$SECURITY_FINDINGS"
    scan_pattern 'yaml\.load\s*\([^)]*\)(?!.*Loader)' "WARNING" "insecure_deserialization" "Insecure YAML load - use safe_load" "CWE-502" "$SECURITY_FINDINGS"

    # IDOR hints (CWE-639) - simplified check
    scan_pattern '@(Get|Delete|Put)Mapping.*\{.*[iI]d\}' "INFO" "idor_hint" "Endpoint with ID parameter - verify authorization" "CWE-639" "$SECURITY_FINDINGS"

    # CSRF (CWE-352) - endpoints without CSRF protection
    scan_pattern '@(Post|Put|Delete)Mapping(?!.*csrf)' "WARNING" "csrf" "POST/PUT/DELETE endpoint - verify CSRF protection" "CWE-352" "$SECURITY_FINDINGS"
    scan_pattern 'app\.(post|put|delete)\s*\([^)]*(?!.*csrf)' "WARNING" "csrf" "Mutating endpoint - verify CSRF token validation" "CWE-352" "$SECURITY_FINDINGS"

    # SSRF (CWE-918) - Server-Side Request Forgery
    scan_pattern 'URL\s*\([^)]*\+|new URL\s*\([^)]*\+' "WARNING" "ssrf" "URL with concatenation - potential SSRF" "CWE-918" "$SECURITY_FINDINGS"
    scan_pattern 'fetch\s*\([^)]*\+|axios\s*\.\w+\s*\([^)]*\+' "WARNING" "ssrf" "HTTP call with concatenation - potential SSRF" "CWE-918" "$SECURITY_FINDINGS"
    scan_pattern 'requests\.(get|post)\s*\([^)]*\+' "WARNING" "ssrf" "requests with concatenation - potential SSRF" "CWE-918" "$SECURITY_FINDINGS"
    scan_pattern 'http\.Get\s*\([^)]*\+|http\.Post\s*\([^)]*\+' "WARNING" "ssrf" "Go http with concatenation - potential SSRF" "CWE-918" "$SECURITY_FINDINGS"

    # Weak Cryptography (CWE-327)
    scan_pattern 'MD5|SHA1(?!256|384|512)' "WARNING" "weak_crypto" "Weak hash algorithm (MD5/SHA1) - use SHA-256+" "CWE-327" "$SECURITY_FINDINGS"
    scan_pattern 'DES|3DES|RC4' "WARNING" "weak_crypto" "Weak encryption algorithm - use AES-256" "CWE-327" "$SECURITY_FINDINGS"

    # Missing TLS verification (CWE-295)
    scan_pattern 'verify\s*=\s*False|InsecureSkipVerify\s*:\s*true|rejectUnauthorized\s*:\s*false' "CRITICAL" "tls_verify" "TLS verification disabled - security risk" "CWE-295" "$SECURITY_FINDINGS"

    # Count findings
    SECURITY_CRITICAL=$(grep -c '"severity":"CRITICAL"' "$SECURITY_FINDINGS" 2>/dev/null | tr -d '\n' || echo 0)
    SECURITY_WARNING=$(grep -c '"severity":"WARNING"' "$SECURITY_FINDINGS" 2>/dev/null | tr -d '\n' || echo 0)
    [ -z "$SECURITY_CRITICAL" ] && SECURITY_CRITICAL=0
    [ -z "$SECURITY_WARNING" ] && SECURITY_WARNING=0
}

# ============================================
# Performance Patterns
# ============================================

scan_performance() {
    echo "⚡ Scanning performance patterns..." >&2

    # N+1 Query patterns
    scan_pattern 'for.*\{[^}]*repository\.|for.*\{[^}]*Repository\.' "CRITICAL" "n_plus_one" "Potential N+1 query - repository call inside loop" "PERF-001" "$PERFORMANCE_FINDINGS"
    scan_pattern 'for.*\{[^}]*\.find\(|for.*\{[^}]*\.get\(' "WARNING" "n_plus_one" "Potential N+1 - find/get inside loop" "PERF-001" "$PERFORMANCE_FINDINGS"
    scan_pattern '\.forEach\s*\([^)]*=>\s*\{[^}]*await' "WARNING" "n_plus_one" "Potential N+1 - await inside forEach" "PERF-001" "$PERFORMANCE_FINDINGS"

    # Unbounded collections
    scan_pattern '\.findAll\s*\(\s*\)' "WARNING" "unbounded_collection" "Unbounded findAll - consider pagination" "PERF-002" "$PERFORMANCE_FINDINGS"
    scan_pattern 'SELECT \* FROM(?!.*LIMIT|.*WHERE)' "WARNING" "unbounded_collection" "SELECT * without LIMIT - consider pagination" "PERF-002" "$PERFORMANCE_FINDINGS"

    # String concatenation in loops
    scan_pattern 'for.*\{[^}]*\+\s*=\s*["\x27]|while.*\{[^}]*\+\s*=\s*["\x27]' "WARNING" "string_concat_loop" "String concatenation in loop - use StringBuilder/join" "PERF-003" "$PERFORMANCE_FINDINGS"

    # Regex compilation in methods (not static)
    scan_pattern 'Pattern\.compile\s*\(' "INFO" "regex_compilation" "Pattern.compile - ensure cached (static final)" "PERF-004" "$PERFORMANCE_FINDINGS"
    scan_pattern 'new RegExp\s*\(' "INFO" "regex_compilation" "RegExp creation - consider caching" "PERF-004" "$PERFORMANCE_FINDINGS"

    # Synchronous calls in async context
    scan_pattern '\.get\(\)\s*;.*CompletableFuture|\.join\(\)\s*;' "WARNING" "blocking_call" "Blocking call (.get/.join) - may block event loop" "PERF-005" "$PERFORMANCE_FINDINGS"
    scan_pattern 'Sync\s*\(' "WARNING" "blocking_call" "Synchronous function call in potentially async context" "PERF-005" "$PERFORMANCE_FINDINGS"

    # Memory leak patterns
    scan_pattern 'static\s+(final\s+)?(List|Set|Map|ArrayList|HashMap)\s*<' "INFO" "static_collection" "Static collection - ensure bounded or cleared" "PERF-006" "$PERFORMANCE_FINDINGS"
    scan_pattern 'addEventListener\s*\(' "INFO" "event_listener" "Event listener - ensure removed when done" "PERF-006" "$PERFORMANCE_FINDINGS"

    # Inefficient lookups
    scan_pattern '\.contains\s*\(.*\).*for|for.*\.contains\s*\(' "WARNING" "inefficient_lookup" "contains() in loop - consider Set for O(1) lookup" "PERF-007" "$PERFORMANCE_FINDINGS"
    scan_pattern '\.indexOf\s*\(.*\).*for|for.*\.indexOf\s*\(' "WARNING" "inefficient_lookup" "indexOf() in loop - consider Map for O(1) lookup" "PERF-007" "$PERFORMANCE_FINDINGS"

    # Long Thread.sleep - potential blocking issue
    scan_pattern 'Thread\.sleep\s*\(\s*[0-9]{5,}' "WARNING" "long_sleep" "Long Thread.sleep (>= 10s) - consider async/scheduled" "PERF-008" "$PERFORMANCE_FINDINGS"
    scan_pattern 'time\.sleep\s*\(\s*[0-9]{2,}' "WARNING" "long_sleep" "Long time.sleep - consider async execution" "PERF-008" "$PERFORMANCE_FINDINGS"

    # Large memory allocations
    scan_pattern 'byte\[\]\s+\w+\s*=\s*new\s+byte\[\d{6,}' "WARNING" "large_allocation" "Large byte array (>= 1MB) - consider streaming" "PERF-009" "$PERFORMANCE_FINDINGS"
    scan_pattern 'make\s*\(\s*\[\s*\]\s*byte\s*,\s*\d{6,}' "WARNING" "large_allocation" "Large slice allocation (Go) - consider streaming" "PERF-009" "$PERFORMANCE_FINDINGS"

    # Recursive patterns without memoization
    scan_pattern 'def\s+\w+\s*\([^)]*\):[^}]*return\s+\w+\s*\(' "INFO" "potential_recursion" "Recursive function - verify base case and consider memoization" "PERF-010" "$PERFORMANCE_FINDINGS"

    # Database connections in loops
    scan_pattern 'for.*\{[^}]*getConnection\s*\(|while.*getConnection\s*\(' "CRITICAL" "connection_loop" "Database connection inside loop - use connection pool" "PERF-011" "$PERFORMANCE_FINDINGS"
    scan_pattern 'for.*\{[^}]*DriverManager\.' "CRITICAL" "connection_loop" "DriverManager inside loop - severe performance issue" "PERF-011" "$PERFORMANCE_FINDINGS"

    # Count findings
    PERF_CRITICAL=$(grep -c '"severity":"CRITICAL"' "$PERFORMANCE_FINDINGS" 2>/dev/null | tr -d '\n' || echo 0)
    PERF_WARNING=$(grep -c '"severity":"WARNING"' "$PERFORMANCE_FINDINGS" 2>/dev/null | tr -d '\n' || echo 0)
    [ -z "$PERF_CRITICAL" ] && PERF_CRITICAL=0
    [ -z "$PERF_WARNING" ] && PERF_WARNING=0
}

# ============================================
# Quality Patterns
# ============================================

scan_quality() {
    echo "📋 Scanning quality patterns..." >&2

    # Debug/console statements
    scan_pattern 'console\.log\s*\(' "WARNING" "debug_statement" "console.log in production code" "QUAL-001" "$QUALITY_FINDINGS"
    scan_pattern 'System\.out\.print' "WARNING" "debug_statement" "System.out in production code" "QUAL-001" "$QUALITY_FINDINGS"
    scan_pattern 'print\s*\([^)]*\)' "INFO" "debug_statement" "print statement - use logger" "QUAL-001" "$QUALITY_FINDINGS"

    # TODO/FIXME comments
    scan_pattern '//\s*TODO|#\s*TODO' "INFO" "todo_comment" "TODO comment found" "QUAL-002" "$QUALITY_FINDINGS"
    scan_pattern '//\s*FIXME|#\s*FIXME' "WARNING" "fixme_comment" "FIXME comment found - needs attention" "QUAL-002" "$QUALITY_FINDINGS"
    scan_pattern '//\s*HACK|#\s*HACK' "WARNING" "hack_comment" "HACK comment found - technical debt" "QUAL-002" "$QUALITY_FINDINGS"

    # Empty catch blocks
    scan_pattern 'catch\s*\([^)]*\)\s*\{\s*\}' "WARNING" "empty_catch" "Empty catch block - swallows errors" "QUAL-003" "$QUALITY_FINDINGS"
    scan_pattern 'except:\s*pass' "WARNING" "empty_catch" "Bare except: pass - swallows errors" "QUAL-003" "$QUALITY_FINDINGS"

    # Magic numbers
    scan_pattern 'sleep\s*\(\s*[0-9]{4,}\s*\)' "INFO" "magic_number" "Magic number in sleep - use named constant" "QUAL-004" "$QUALITY_FINDINGS"
    scan_pattern 'timeout\s*[:=]\s*[0-9]{4,}' "INFO" "magic_number" "Magic number in timeout - use named constant" "QUAL-004" "$QUALITY_FINDINGS"

    # Commented out code (simplified detection)
    scan_pattern '//\s*(if|for|while|return|function|class|def)\s' "INFO" "commented_code" "Potentially commented out code" "QUAL-005" "$QUALITY_FINDINGS"

    # Count findings
    QUALITY_CRITICAL=$(grep -c '"severity":"CRITICAL"' "$QUALITY_FINDINGS" 2>/dev/null | tr -d '\n' || echo 0)
    QUALITY_WARNING=$(grep -c '"severity":"WARNING"' "$QUALITY_FINDINGS" 2>/dev/null | tr -d '\n' || echo 0)
    [ -z "$QUALITY_CRITICAL" ] && QUALITY_CRITICAL=0
    [ -z "$QUALITY_WARNING" ] && QUALITY_WARNING=0
}

# ============================================
# Run Scans
# ============================================

[ "$SCAN_SECURITY" = true ] && scan_security
[ "$SCAN_PERFORMANCE" = true ] && scan_performance
[ "$SCAN_QUALITY" = true ] && scan_quality

# ============================================
# Calculate Totals
# ============================================

TOTAL_CRITICAL=$((SECURITY_CRITICAL + PERF_CRITICAL + QUALITY_CRITICAL))
TOTAL_WARNING=$((SECURITY_WARNING + PERF_WARNING + QUALITY_WARNING))

# Determine verdict
VERDICT="APPROVED"
if [ "$TOTAL_CRITICAL" -gt 0 ]; then
    VERDICT="CANNOT_PROCEED"
elif [ "$TOTAL_WARNING" -gt 5 ]; then
    VERDICT="CAN_PROCEED_WITH_WARNINGS"
fi

# ============================================
# Output Results
# ============================================

if [ "$OUTPUT_JSON" = true ]; then
    cat << EOF
{
  "verdict": "$VERDICT",
  "summary": {
    "critical": $TOTAL_CRITICAL,
    "warning": $TOTAL_WARNING,
    "security": {"critical": $SECURITY_CRITICAL, "warning": $SECURITY_WARNING},
    "performance": {"critical": $PERF_CRITICAL, "warning": $PERF_WARNING},
    "quality": {"critical": $QUALITY_CRITICAL, "warning": $QUALITY_WARNING}
  },
  "findings": {
    "security": [$(cat "$SECURITY_FINDINGS" 2>/dev/null | tr '\n' ',' | sed 's/,$//')],
    "performance": [$(cat "$PERFORMANCE_FINDINGS" 2>/dev/null | tr '\n' ',' | sed 's/,$//')],
    "quality": [$(cat "$QUALITY_FINDINGS" 2>/dev/null | tr '\n' ',' | sed 's/,$//')]
  }
}
EOF
else
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "📊 Code Validation Results"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "Security:    $SECURITY_CRITICAL critical, $SECURITY_WARNING warnings"
    echo "Performance: $PERF_CRITICAL critical, $PERF_WARNING warnings"
    echo "Quality:     $QUALITY_CRITICAL critical, $QUALITY_WARNING warnings"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Verdict: $VERDICT"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    # Show critical findings
    if [ "$TOTAL_CRITICAL" -gt 0 ]; then
        echo ""
        echo "🚫 CRITICAL FINDINGS:"
        grep '"severity":"CRITICAL"' "$SECURITY_FINDINGS" "$PERFORMANCE_FINDINGS" "$QUALITY_FINDINGS" 2>/dev/null | \
            while IFS= read -r finding; do
                file=$(echo "$finding" | grep -o '"file":"[^"]*"' | cut -d'"' -f4)
                line=$(echo "$finding" | grep -o '"line":[0-9]*' | cut -d: -f2)
                desc=$(echo "$finding" | grep -o '"description":"[^"]*"' | cut -d'"' -f4)
                echo "  ❌ $file:$line - $desc"
            done
    fi
fi

# Exit with appropriate code
if [ "$VERDICT" = "CANNOT_PROCEED" ]; then
    exit 1
else
    exit 0
fi
