# Coding Standards

**Version**: 1.0.0
**Last Updated**: 2025-11-25
**Applies To**: All code generated via SDD Kit

---

## General Principles

### 1. Code is Read More Than Written

Write code for humans first, machines second:
- ✅ Clear variable and function names
- ✅ Meaningful comments for complex logic
- ✅ Consistent formatting
- ❌ Clever one-liners that sacrifice clarity

### 2. Fail Fast and Explicitly

- ✅ Validate inputs at boundaries
- ✅ Throw descriptive errors
- ✅ Use type systems to catch errors at compile time
- ❌ Silent failures or ignored errors

### 3. Test-Driven Development

- ✅ Write tests alongside code, not after
- ✅ Test behavior, not implementation
- ✅ Aim for >80% coverage, >90% for critical paths
- ❌ Tests that just call the function without assertions

---

## Language-Specific Standards

### TypeScript / JavaScript

#### File Naming
- **Components**: PascalCase - `PaymentForm.tsx`
- **Utilities**: camelCase - `formatCurrency.ts`
- **Constants**: UPPER_SNAKE_CASE - `API_ENDPOINTS.ts`
- **Types**: PascalCase - `Payment.types.ts`

#### Code Style

```typescript
// ✅ Good: Clear function with types
function calculateTotal(
  items: CartItem[],
  taxRate: number = 0.1
): Money {
  const subtotal = items.reduce((sum, item) => sum + item.price, 0);
  const tax = subtotal * taxRate;
  return { amount: subtotal + tax, currency: 'USD' };
}

// ❌ Bad: Unclear, no types
function calc(items, tax) {
  let t = 0;
  items.forEach(i => t += i.p);
  return t + (t * tax);
}
```

#### Naming Conventions

```typescript
// Variables and functions: camelCase
const userId = 'user-123';
function getUserById(id: string) { }

// Classes and interfaces: PascalCase
class PaymentService { }
interface Payment { }

// Constants: UPPER_SNAKE_CASE
const MAX_RETRIES = 3;
const API_BASE_URL = 'https://api.example.com';

// Private class members: prefix with _
class Service {
  private _cache: Map<string, any>;
}

// Boolean variables: is/has/can prefix
const isActive = true;
const hasPermission = false;
const canEdit = checkPermission();
```

#### Comments

```typescript
// ✅ Good: Explains WHY
// Retry logic needed because external API is unreliable during peak hours
async function fetchWithRetry() { }

// ❌ Bad: Explains WHAT (code already shows this)
// This function fetches with retry
async function fetchWithRetry() { }

// ✅ Good: JSDoc for public APIs
/**
 * Processes a payment with retry logic
 *
 * @param paymentData - Payment details including amount and method
 * @param options - Optional configuration (retries, timeout)
 * @returns Payment result with transaction ID
 * @throws PaymentError if all retries fail
 */
async function processPayment(
  paymentData: PaymentData,
  options?: PaymentOptions
): Promise<PaymentResult> { }
```

#### Error Handling

```typescript
// ✅ Good: Specific error types
class PaymentDeclinedError extends Error {
  constructor(public reason: string, public code: string) {
    super(`Payment declined: ${reason}`);
    this.name = 'PaymentDeclinedError';
  }
}

throw new PaymentDeclinedError('Insufficient funds', 'INSUFFICIENT_FUNDS');

// ❌ Bad: Generic errors
throw new Error('Payment failed');
```

---

### Java/Kotlin

#### Java Servlet API Selection

> **Rule**: Use `jakarta.*` by default (`javax.*` is deprecated for EE APIs)

**Detection Strategy** (BEFORE generating Java code):
1. Check existing imports: `grep -r "import javax\." src/` and `grep -r "import jakarta\." src/`
   - Found `javax.servlet`, `javax.ws.rs` → Use javax (legacy project)
   - Found `jakarta.servlet`, `jakarta.ws.rs` → Use jakarta
2. Check Spring Boot version in pom.xml:
   - Boot 3.x+ → jakarta (required)
   - Boot 2.x → javax
3. No existing code → Default to **jakarta** (modern)

**Important Notes**:
- `javax.sql.*` stays as `javax.sql` (Java SE, not Jakarta EE)
- NEVER mix javax and jakarta servlet/ws APIs in the same project
- Show: "Using {jakarta/javax} imports (detected from project)"

**Jakarta Imports** (modern - default):
```java
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.annotation.PreDestroy;
```

**Javax Imports** (legacy - only if project uses them):
```java
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.annotation.PreDestroy;
```

---

### Python (If Used)

#### File Naming
- **Modules**: snake_case - `payment_service.py`
- **Classes**: PascalCase - `class PaymentService`
- **Constants**: UPPER_SNAKE_CASE - `MAX_RETRIES = 3`

#### Code Style (PEP 8)

```python
# ✅ Good: Type hints, clear names
def calculate_total(items: list[CartItem], tax_rate: float = 0.1) -> Decimal:
    """Calculate total with tax."""
    subtotal = sum(item.price for item in items)
    tax = subtotal * Decimal(tax_rate)
    return subtotal + tax

# ❌ Bad: No types, unclear
def calc(items, tax):
    t = sum(i.p for i in items)
    return t + (t * tax)
```

---

## Testing Standards

### Test File Organization

```
src/
├── services/
│   ├── PaymentService.ts
│   └── __tests__/
│       └── PaymentService.test.ts

# Or alternatively:
src/
├── services/
│   └── PaymentService.ts
├── __tests__/
│   └── services/
│       └── PaymentService.test.ts
```

### Test Naming

```typescript
describe('PaymentService', () => {
  describe('processPayment', () => {
    it('should create payment successfully with valid data', async () => {
      // Arrange
      const paymentData = createMockPaymentData();

      // Act
      const result = await service.processPayment(paymentData);

      // Assert
      expect(result.status).toBe('completed');
      expect(result.id).toBeDefined();
    });

    it('should throw PaymentDeclinedError when card is declined', async () => {
      // Arrange
      const paymentData = createMockPaymentData({ cardDeclined: true });

      // Act & Assert
      await expect(service.processPayment(paymentData))
        .rejects
        .toThrow(PaymentDeclinedError);
    });

    it('should retry 3 times before failing', async () => {
      // ...
    });
  });
});
```

**Test Naming Convention**:
- Start with `should`
- Describe expected behavior
- Include context (when X, should Y)
- Be specific, not generic

**Good**:
- ✅ `should create payment successfully with valid data`
- ✅ `should throw PaymentDeclinedError when card is declined`
- ✅ `should retry 3 times before failing on network error`

**Bad**:
- ❌ `test payment`
- ❌ `it works`
- ❌ `payment creation test`

### Test Structure (AAA Pattern)

```typescript
it('should calculate discount correctly', () => {
  // Arrange - Set up test data
  const originalPrice = 100;
  const discountPercent = 20;

  // Act - Execute the function
  const finalPrice = calculateDiscount(originalPrice, discountPercent);

  // Assert - Verify the result
  expect(finalPrice).toBe(80);
});
```

### Coverage Targets

- **Overall**: Minimum 80%
- **Business Logic**: Minimum 90%
- **Utils/Helpers**: Minimum 85%
- **UI Components**: Minimum 70%
- **Configuration**: Can skip (static)

---

## Git Standards ( GitFlow)

> **Referencia**: https://furydocs.io/release-process/latest/guide/#/lang-es/workflows/02_gitflow

### Branch Prefixes (Estándar  - No configurables)

Estos prefijos están definidos por GitFlow de  y **no son configurables** a nivel de proyecto:

| Branch | Uso |
|--------|-----|
| `master` | Rama principal de producción |
| `develop` | Rama de desarrollo/integración |
| `feature/*` | Nuevas funcionalidades |
| `enhancement/*` | Mejoras a funcionalidades existentes |
| `fix/*` | Corrección de bugs |
| `bugfix/*` | Alias de fix (corrección de bugs) |
| `hotfix/*` | Corrección crítica en producción |
| `release/*` | Preparación de release |
| `migration/*` | Migraciones de datos/schema |
| `revert-*` | Reversión de cambios |

### Branch Naming

```
<prefix>/[descripcion]
```

**Examples**:
- ✅ `feature/payment-integration`
- ✅ `enhancement/improve-checkout-flow`
- ✅ `fix/payment-validation-error`
- ✅ `bugfix/null-pointer-exception`
- ✅ `hotfix/security-vulnerability`
- ✅ `release/2.0.0`
- ✅ `migration/add-user-status-column`
- ✅ `revert-abc123`
- ❌ `dev-branch`
- ❌ `johns-changes`
- ❌ `test-feature` (usar `feature/test-feature`)

### Commit Messages (Conventional Commits - Estándar )

> **Nota**: El formato de commits es **Conventional Commits** y es obligatorio en . No es configurable a nivel de proyecto.

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Types** (obligatorios):
- `feat`: Nueva funcionalidad
- `fix`: Corrección de bug
- `refactor`: Refactoring sin cambio funcional
- `test`: Agregando o modificando tests
- `docs`: Documentación
- `chore`: Mantenimiento, dependencias
- `perf`: Mejora de performance
- `style`: Cambios de formato (no afectan lógica)
- `ci`: Cambios en CI/CD

**Examples**:

```bash
# ✅ Good commits
feat(payment): add MercadoPago integration
fix(payment): handle declined card errors
test(payment): add unit tests for PaymentService
docs(api): update payment endpoint documentation

# ❌ Bad commits
update stuff
fix
WIP
asdf
```

**Commit Body** (optional but recommended):

```
feat(payment): add MercadoPago integration

Implements payment processing using MercadoPago REST API with retry logic.
Includes validation for card data and error handling for common
decline scenarios.

Task: TASK-006
Closes: #123
```

---

## Documentation Standards

### Code Documentation

#### Public APIs (Always Document)

```typescript
/**
 * Processes a payment transaction
 *
 * @param paymentData - Payment details including amount, currency, and method
 * @param options - Optional configuration for retry behavior and timeout
 * @returns Promise resolving to payment result with transaction ID
 * @throws {PaymentDeclinedError} When payment method is declined
 * @throws {ValidationError} When payment data is invalid
 * @throws {NetworkError} When external service is unreachable after retries
 *
 * @example
 * ```typescript
 * const result = await processPayment({
 *   amount: 99.99,
 *   currency: 'USD',
 *   method: { type: 'card', token: 'tok_xxx' }
 * });
 * ```
 */
export async function processPayment(
  paymentData: PaymentData,
  options?: PaymentOptions
): Promise<PaymentResult> {
  // Implementation
}
```

#### Complex Logic (Add Inline Comments)

```typescript
function calculateRefund(payment: Payment, reason: RefundReason): Refund {
  // Refunds within 14 days are automatic, after that require manual approval
  const daysSincePurchase = differenceInDays(new Date(), payment.createdAt);
  const requiresApproval = daysSincePurchase > 14;

  // Partial refunds for subscription cancellations use prorated amount
  const amount = reason === 'subscription_cancel'
    ? calculateProratedAmount(payment)
    : payment.amount;

  return {
    paymentId: payment.id,
    amount,
    requiresApproval,
    reason
  };
}
```

### README Documentation

Every module/feature should have a README:

```markdown
# Payment Integration

## Overview
Brief description of what this module does.

## Usage
```typescript
import { PaymentService } from '@/services/PaymentService';

const service = new PaymentService();
const result = await service.processPayment(data);
```

## Architecture
[Link to your architecture diagram or brief description]

## Configuration
Environment variables needed, configuration options, etc.

## Testing
How to run tests for this module.
```

---

## Spec Traceability Comments

> **v2.3.0**: Bidirectional traceability between specs and implementation code.

### Overview

Code generated by `/sdd.build` MUST include `@spec` comments that reference back to the technical specification. This enables:
- Tracing code back to its specification
- Understanding why code exists
- Keeping specs and code synchronized

### Format

```
@spec [feature-name]#[section]
@implements [US-1, US-2, ...]
```

- **`@spec`**: Required. Points to the feature's technical spec section
- **`@implements`**: Optional. Lists User Stories being implemented
- **`feature-name`**: The feature directory name (stable between `sdd/wip/` and `sdd/features/`)

### TypeScript / JavaScript

```typescript
/**
 * @spec user-auth#authentication
 * @implements US-1, US-2
 *
 * Service for handling user authentication via OAuth2.
 */
export class AuthService {
  // ...
}

/**
 * @spec payment-gateway#refund-logic
 * @implements US-7
 */
async function processRefund(paymentId: string): Promise<Refund> {
  // ...
}
```

### Python

```python
"""
@spec user-auth#authentication
@implements US-1, US-2

Service for handling user authentication via OAuth2.
"""

class AuthService:
    pass


def process_refund(payment_id: str) -> Refund:
    """
    @spec payment-gateway#refund-logic
    @implements US-7

    Process a refund for the given payment.
    """
    pass
```

### Go

```go
// @spec user-auth#authentication
// @implements US-1, US-2
//
// AuthService handles user authentication via OAuth2.
type AuthService struct {
    // ...
}

// @spec payment-gateway#refund-logic
// @implements US-7
//
// ProcessRefund handles refund processing for a payment.
func ProcessRefund(paymentID string) (*Refund, error) {
    // ...
}
```

### Java

```java
/**
 * @spec user-auth#authentication
 * @implements US-1, US-2
 *
 * Service for handling user authentication via OAuth2.
 */
public class AuthService {
    // ...
}
```

### When to Use

| Scenario | Required? |
|----------|-----------|
| New files created by `/sdd.build` | **Yes** |
| Major classes/modules | **Yes** |
| Utility functions | No (optional) |
| Test files | No |
| Configuration files | No |

### Path Resolution

The `@spec` reference uses only the feature name (not full path):
- During development: resolves to `sdd/wip/[feature-name]/2-technical/spec.md`
- After completion: resolves to `sdd/features/[feature-name]/2-technical/spec.md`

This makes references stable across the feature lifecycle.

---

## Security Standards

### Never Commit Secrets

❌ **Never**:
```typescript
const MERCADOPAGO_ACCESS_TOKEN = 'APP_USR-xxx';  // NO!
const DB_PASSWORD = 'password123';         // NO!
```

✅ **Always**:
```typescript
const MERCADOPAGO_ACCESS_TOKEN = process.env.MERCADOPAGO_ACCESS_TOKEN;
const DB_PASSWORD = process.env.DB_PASSWORD;

// With validation
if (!MERCADOPAGO_ACCESS_TOKEN) {
  throw new Error('MERCADOPAGO_ACCESS_TOKEN environment variable required');
}
```

### Input Validation

```typescript
// ✅ Good: Validate at boundary
function createPayment(data: unknown): Payment {
  // Validate with Zod or similar
  const validated = PaymentSchema.parse(data);

  // Now safe to use
  return processPayment(validated);
}

// ❌ Bad: Trust external input
function createPayment(data: any) {
  return processPayment(data);  // Dangerous!
}
```

### Sanitization

```typescript
// ✅ Good: Sanitize user input
import { escape } from 'validator';

function createNote(userInput: string): Note {
  const sanitized = escape(userInput);
  return { content: sanitized };
}

// ❌ Bad: Direct use of user input
function createNote(userInput: string): Note {
  return { content: userInput };  // XSS risk!
}
```

---

## Performance Standards

### Avoid N+1 Queries

```typescript
// ❌ Bad: N+1 query problem
async function getUsersWithPosts(userIds: string[]) {
  const users = await db.user.findMany({ where: { id: { in: userIds } } });

  for (const user of users) {
    user.posts = await db.post.findMany({ where: { userId: user.id } });  // N queries!
  }

  return users;
}

// ✅ Good: Single query with join
async function getUsersWithPosts(userIds: string[]) {
  return db.user.findMany({
    where: { id: { in: userIds } },
    include: { posts: true }  // Single query!
  });
}
```

### Pagination

```typescript
// ✅ Good: Always paginate lists
async function getPayments(userId: string, page = 1, limit = 50) {
  const offset = (page - 1) * limit;

  return db.payment.findMany({
    where: { userId },
    take: limit,
    skip: offset,
    orderBy: { createdAt: 'desc' }
  });
}
```

### Caching

```typescript
// ✅ Good: Cache expensive operations
async function getUserProfile(userId: string): Promise<Profile> {
  const cacheKey = `user:profile:${userId}`;

  // Check cache first
  const cached = await cache.get(cacheKey);
  if (cached) return JSON.parse(cached);

  // Cache miss - fetch from DB
  const profile = await db.user.findUnique({ where: { id: userId } });

  // Store in cache (TTL: 5 minutes)
  await cache.setex(cacheKey, 300, JSON.stringify(profile));

  return profile;
}
```

---

## Code Review Checklist

Before submitting code for review, verify:

### Functionality
- [ ] Code does what the task acceptance criteria specify
- [ ] Edge cases handled
- [ ] Error cases handled gracefully

### Code Quality
- [ ] Follows naming conventions
- [ ] No magic numbers (use named constants)
- [ ] No commented-out code
- [ ] No console.log() in production code
- [ ] DRY (Don't Repeat Yourself) - no copy-paste code

### Testing
- [ ] Unit tests written and passing
- [ ] Integration tests if needed
- [ ] Coverage meets thresholds
- [ ] Tests are meaningful, not just for coverage

### Security
- [ ] No secrets in code
- [ ] User input validated
- [ ] SQL injection prevented (use parameterized queries)
- [ ] XSS prevented (sanitize output)

### Performance
- [ ] No N+1 queries
- [ ] Pagination for lists
- [ ] Caching where appropriate
- [ ] No blocking operations on main thread (frontend)

### Documentation
- [ ] Public APIs have JSDoc
- [ ] Complex logic has inline comments
- [ ] README updated if needed

---

## Linting Configuration

### ESLint (TypeScript/JavaScript)

```json
{
  "extends": [
    "eslint:recommended",
    "plugin:@typescript-eslint/recommended",
    "plugin:react/recommended",
    "plugin:react-hooks/recommended"
  ],
  "rules": {
    "no-console": "error",
    "no-unused-vars": "error",
    "@typescript-eslint/explicit-function-return-type": "warn",
    "@typescript-eslint/no-explicit-any": "error",
    "complexity": ["warn", 10],
    "max-lines-per-function": ["warn", 50]
  }
}
```

### Prettier (Formatting)

```json
{
  "semi": true,
  "singleQuote": true,
  "tabWidth": 2,
  "trailingComma": "es5",
  "printWidth": 100,
  "arrowParens": "always"
}
```

---

## References

- TypeScript Style Guide: https://google.github.io/styleguide/tsguide.html
- Conventional Commits: https://www.conventionalcommits.org/
- Clean Code Principles: Robert C. Martin's "Clean Code"
