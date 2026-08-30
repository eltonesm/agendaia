---
name: sdd-performance-expert
stack: core
description: Performance expert for reviewing code for performance issues. This is a SKILL (invoke via Skill tool, NOT Task/subagent). Use after implementation to detect N+1 queries, inefficient algorithms, memory leaks, and other performance anti-patterns. **TRIGGER ON** N+1 query, performance, memory leak, inefficient algorithm, slow query, bottleneck, optimization.
---

# SDD Performance Expert

> **SKILL**: Detect performance anti-patterns BEFORE they reach production. Invoke with `Skill("sdd-performance-expert")`.

---

## When to Use

1. **After Task Implementation** (`/sdd.build`) - After each task
2. **Final Validation** (`/sdd.build` end) - Full codebase scan
3. **On Demand** - When performance problems are suspected

---

## Critical Performance Anti-Patterns

### 1. N+1 Query Problems

```java
// BAD: N+1 queries - Query per iteration!
for (Order order : orders) {
    User user = userRepository.findById(order.getUserId());
}

// GOOD: Batch fetch
Set<Long> userIds = orders.stream().map(Order::getUserId).collect(toSet());
Map<Long, User> users = userRepository.findByIds(userIds);
```

**Detection**: Look for repository/database calls inside loops.

### 2. Regex in Hot Paths

```java
// BAD: Compiling regex per call
public boolean validate(String input) {
    return input.matches("^[a-zA-Z0-9]+$"); // Compiles every time!
}

// GOOD: Pre-compiled pattern
private static final Pattern VALID_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
public boolean validate(String input) {
    return VALID_PATTERN.matcher(input).matches();
}
```

**Detection**: Look for `.matches()`, `Pattern.compile()` in methods called frequently.

### 3. String Concatenation in Loops

```java
// BAD: O(n²) string building
String result = "";
for (String item : items) {
    result += item + ","; // Creates new String each time
}

// GOOD: StringBuilder O(n)
StringBuilder sb = new StringBuilder();
for (String item : items) {
    sb.append(item).append(",");
}
```

**Detection**: Look for `+=` on strings inside loops.

### 4. Unbounded Collections

```java
// BAD: Loading all records into memory
List<User> allUsers = userRepository.findAll(); // Could be millions!

// GOOD: Pagination
Page<User> users = userRepository.findAll(PageRequest.of(0, 100));
```

**Detection**: Look for `.findAll()` without pagination.

### 5. Missing Indexes (SQL)

```sql
-- BAD: Query without index
SELECT * FROM orders WHERE user_id = ? AND status = ?;
-- If no index on (user_id, status), full table scan!

-- GOOD: Composite index
CREATE INDEX idx_orders_user_status ON orders(user_id, status);
```

**Detection**: Check queries against entity fields without `@Index`.

### 6. Blocking I/O in Async Context

```typescript
// BAD: Blocking in async context
app.get('/data', (req, res) => {
    const data = fs.readFileSync('/large-file.json'); // Blocks event loop!
    res.json(JSON.parse(data));
});

// GOOD: Non-blocking
app.get('/data', async (req, res) => {
    const data = await fs.promises.readFile('/large-file.json');
    res.json(JSON.parse(data));
});
```

**Detection**: Look for `*Sync` methods in Node.js async handlers.

### 7. Missing Connection Pooling

```java
// BAD: New connection per request
Connection conn = DriverManager.getConnection(url);

// GOOD: Connection pool
@Autowired
private DataSource dataSource; // HikariCP pool
```

**Detection**: Look for `DriverManager.getConnection()` calls.

### 8. Inefficient Data Structures

```java
// BAD: O(n) lookup
List<User> users = loadUsers();
User found = users.stream()
    .filter(u -> u.getId().equals(targetId))
    .findFirst().orElse(null); // O(n) each lookup

// GOOD: O(1) lookup
Map<Long, User> usersById = loadUsers().stream()
    .collect(toMap(User::getId, u -> u));
User found = usersById.get(targetId); // O(1)
```

**Detection**: Look for `.stream().filter().findFirst()` patterns on lists used for lookups.

### 9. Memory Leaks

```java
// BAD: Static collection that grows forever
private static final List<Request> requestHistory = new ArrayList<>();
public void logRequest(Request r) {
    requestHistory.add(r); // Never cleared!
}

// GOOD: Bounded cache
private final Cache<String, Request> recentRequests =
    CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build();
```

**Detection**: Look for static collections that only have `add()` without `remove()` or bounds.

---

## Review Output Format

```markdown
## Performance Review

### Critical Issues (Must Fix)
#### Issue 1: [N+1 Query in OrderService]
- **Location**: `src/service/OrderService.java:45`
- **Problem**: Database query inside loop, O(n) queries
- **Impact**: Response time scales linearly with data size
- **Fix**: Use batch fetch with `findByIds()`
- **Example**:
  ```java
  // Before
  for (Order o : orders) {
      userRepo.findById(o.getUserId());
  }
  // After
  Map<Long, User> users = userRepo.findByIds(orderUserIds);
  ```

### Warnings (Should Fix)
#### Warning 1: [Pre-compile Regex]
- **Location**: `src/validator/InputValidator.java:23`
- **Problem**: Regex compiled on every validation call
- **Impact**: ~10x slower than pre-compiled
- **Fix**: Use static `Pattern.compile()`

### Recommendations (Nice to Have)
#### Recommendation 1: [Consider Caching]
- **Location**: `src/service/UserService.java:67`
- **Observation**: Same user fetched multiple times per request
- **Suggestion**: Add request-scoped cache

## Summary
- Critical: X issues
- Warnings: Y issues
- Recommendations: Z items
```

---

## Patterns to Search For

Use grep/search to find these patterns:

```bash
# N+1: Repository calls in loops
grep -rn "for.*{" --include="*.java" | xargs -I {} sh -c 'grep -l "Repository\|\.find" {}'

# String concat in loops
grep -rn '+=.*"' --include="*.java" --include="*.ts"

# Sync methods in Node
grep -rn "Sync(" --include="*.ts" --include="*.js"

# findAll without pagination
grep -rn "\.findAll()" --include="*.java"
```

---

## Important Rules

1. **Identify hot paths**: Focus on APIs, loops, frequently called methods
2. **Check database access**: N+1, missing indexes, large result sets
3. **Review string operations**: Concatenation, regex compilation
4. **Analyze memory usage**: Unbounded collections, leaks
5. **Check I/O patterns**: Blocking calls, missing pooling

---

## Verdict Output (MANDATORY)

> **v2.0.0**: After completing the review, you MUST write a verdict file.

### Verdict File Location

```
sdd/wip/<feature>/verdicts/performance.json
```

### Write Verdict After Review

**After completing all checks, create the verdict file**:

```json
{
  "skill": "sdd-performance-expert",
  "verdict": "APPROVED",
  "findings": {
    "critical": 0,
    "warnings": 0,
    "recommendations": 2
  },
  "patterns_checked": ["n+1", "regex", "string_concat", "unbounded_collections", "memory_leaks"],
  "timestamp": "2026-01-19T12:00:00Z"
}
```

### Verdict Values

| Verdict | Condition | Task Completion |
|---------|-----------|-----------------|
| `APPROVED` | 0 critical issues | Allowed |
| `CAN_PROCEED_WITH_WARNINGS` | 0 critical, warnings ≤ 3 | Allowed |
| `CANNOT_PROCEED` | Any critical issue | BLOCKED |

### Critical vs Warning

| Severity | Examples | Verdict Impact |
|----------|----------|----------------|
| **Critical** | N+1 query in production path, unbounded memory growth | CANNOT_PROCEED |
| **Warning** | Regex in moderate-frequency path, minor inefficiency | CAN_PROCEED_WITH_WARNINGS |
| **Recommendation** | Optimization suggestions, nice-to-haves | APPROVED |

### Verdict Writing Instructions

1. **Create verdicts directory** if it doesn't exist:
   ```bash
   mkdir -p sdd/wip/<feature>/verdicts
   ```

2. **Write the verdict file** with current findings count

3. **Verdict determines if Layer 3 task can be completed**:
   - `APPROVED` → Task can be marked complete
   - `CANNOT_PROCEED` → Must fix issues and re-run this skill

> **CRITICAL**: The enforcement hook checks this file before allowing task completion.
