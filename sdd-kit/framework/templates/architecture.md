# {{FEATURE_NAME}} - Architecture Diagrams

**Feature**: {{FEATURE_NAME}}
**Created**: {{DATE}}
**Last Updated**: {{DATE}}

---

## System Context Diagram (C4 Level 1)

Shows how this feature fits into the broader system.

```mermaid
graph TB
    subgraph "External Systems"
        Users([Users])
        PaymentProvider[Payment Provider<br/>MercadoPago]
    end

    subgraph "Our System"
        subgraph "This Feature"
            UI[UI Layer]
            Backend[Backend Service]
        end
        Database[(Database)]
        FuryServices[ Services]
    end

    Users --> UI
    UI --> Backend
    Backend --> Database
    Backend --> FuryServices
    Backend <--> PaymentProvider

    style UI fill:#e3f2fd
    style Backend fill:#e3f2fd
    style Database fill:#fff3e0
    style FuryServices fill:#e8f5e9
    style PaymentProvider fill:#fce4ec
```

---

## Container Diagram (C4 Level 2)

Shows the main containers/applications involved.

```mermaid
graph TB
    User([User])
    User --> WebApp[Web Application<br/>Nordic + Andes]
    User --> MobileApp[Mobile App<br/>React Native]

    WebApp --> API[REST API Gateway<br/>Node.js/Express]
    MobileApp --> API

    API --> PaymentService[Payment Service<br/>Business Logic]
    API --> FuryIAM[ IAM<br/>Authentication]

    PaymentService --> PaymentDB[(Payment Database<br/> MySQL)]
    PaymentService --> FuryCache[( Cache)]
    PaymentService --> FuryMessaging[ Messaging<br/>BigQueue]
    PaymentService --> MercadoPagoAPI[MercadoPago REST API<br/>External]

    FuryMessaging --> Analytics[Analytics Service]
    FuryMessaging --> Notifications[Notification Service]

    style PaymentService fill:#e1f5ff
    style PaymentDB fill:#ffe1e1
    style FuryIAM fill:#e8f5e9
    style FuryMessaging fill:#e8f5e9
```

---

## Component Diagram (C4 Level 3)

Shows components within the main containers.

```mermaid
graph TB
    subgraph "Frontend (React)"
        UI1[PaymentForm Component]
        UI2[PaymentList Component]
        UI3[PaymentStatus Component]
        State[State Management<br/>Context + useReducer]

        UI1 --> State
        UI2 --> State
        UI3 --> State
    end

    subgraph "Backend REST API"
        Routes[REST API Routes<br/>Express Router]
        Controller[Payment Controller]
        Middleware[Auth Middleware]

        Routes --> Middleware
        Middleware --> Controller
    end

    subgraph "Service Layer"
        PaymentService[Payment Service]
        RefundService[Refund Service]
        ValidationService[Validation Service]

        Controller --> PaymentService
        Controller --> RefundService
        PaymentService --> ValidationService
    end

    subgraph "Data Layer"
        Repository[Payment Repository]
        PaymentModel[Payment Model<br/>Domain Entity]

        PaymentService --> Repository
        RefundService --> Repository
        Repository --> PaymentModel
    end

    subgraph "External Integrations"
        FuryIAM[ IAM Client]
        FuryMessaging[ Messaging Client]
        MercadoPagoClient[MercadoPago Client]

        Middleware --> FuryIAM
        PaymentService --> MercadoPagoClient
        PaymentService --> FuryMessaging
    end

    State <--> Routes
    Repository --> DB[( MySQL)]
    PaymentService --> Cache[( Cache)]
```

---

## Data Flow Diagram

Shows how data moves through the system.

```mermaid
sequenceDiagram
    participant User
    participant UI as Frontend
    participant API as REST API Gateway
    participant Service as Payment Service
    participant DB as Database
    participant MercadoPago as MercadoPago REST API
    participant Events as  Messaging

    User->>UI: Enter payment details
    UI->>UI: Validate inputs
    UI->>API: POST /api/v1/payments
    API->>API: Verify JWT ( IAM)
    API->>Service: createPayment(data)
    Service->>Service: Validate business rules
    Service->>DB: Save payment record (status: pending)
    DB-->>Service: Payment ID
    Service->>MercadoPago: Process payment
    MercadoPago-->>Service: Payment result
    Service->>DB: Update status (completed/failed)
    Service->>Events: Publish payment.completed event
    Events-->>Analytics: Event received
    Events-->>Notifications: Event received
    Service-->>API: Payment response
    API-->>UI: Success response
    UI-->>User: Show confirmation
```

---

## Database Entity-Relationship Diagram

```mermaid
erDiagram
    USERS ||--o{ PAYMENTS : makes
    PAYMENTS ||--o{ REFUNDS : has
    PAYMENTS ||--o{ PAYMENT_EVENTS : tracks

    USERS {
        uuid id PK
        string email
        string name
        timestamp created_at
    }

    PAYMENTS {
        uuid id PK
        uuid user_id FK
        decimal amount
        string currency
        string status
        string payment_method
        timestamp created_at
        timestamp updated_at
        jsonb metadata
    }

    REFUNDS {
        uuid id PK
        uuid payment_id FK
        decimal amount
        string reason
        string status
        timestamp created_at
    }

    PAYMENT_EVENTS {
        uuid id PK
        uuid payment_id FK
        string event_type
        jsonb event_data
        timestamp created_at
    }
```

---

## Infrastructure Diagram

Shows deployment architecture.

```mermaid
graph TB
    subgraph "Load Balancer"
        LB[Application Load Balancer]
    end

    subgraph "Application Tier (Auto-scaling)"
        App1[API Instance 1]
        App2[API Instance 2]
        App3[API Instance N]
    end

    subgraph "Data Tier"
        Primary[(Primary DB<br/> MySQL)]
        Replica[(Read Replica)]
         Cache[( Cache Cluster)]
    end

    subgraph "External Services"
        [ Platform<br/>IAM, Messaging, Monitoring]
        MercadoPago[MercadoPago REST API]
    end

    subgraph "Monitoring"
        DD[DataDog<br/>Logs, Metrics, Traces]
    end

    LB --> App1
    LB --> App2
    LB --> App3

    App1 --> Primary
    App2 --> Primary
    App3 --> Replica

    App1 -->  Cache
    App2 -->  Cache
    App3 -->  Cache

    App1 --> 
    App2 --> 
    App3 --> 

    App1 --> MercadoPago
    App2 --> MercadoPago
    App3 --> MercadoPago

    App1 --> DD
    App2 --> DD
    App3 --> DD
    Primary --> DD
     Cache --> DD
```

---

## Technology Stack

### Frontend
- **Framework**: Nordic (React) with TypeScript
- **State**: `nordic/store` (8.16.0+) / Context + useReducer (Zustand as fallback)
- **UI Library**: Andes Web Design System
- **Testing**: Vitest + React Testing Library

### Backend
- **Runtime**: Node.js 20+
- **Framework**: Express or Fastify
- **Language**: TypeScript
- **ORM**: Prisma or TypeORM

### Data Storage
- **Primary Database**:  MySQL 15+
- **Caching**:  Cache 7+
- **Session Store**:  Cache

### External Services
- **Auth**:  IAM (OAuth2/JWT)
- **Messaging**:  Messaging (BigQueue)
- **Monitoring**:  DataDog
- **Payments**: MercadoPago REST API

### DevOps
- **Containers**: Docker
- **Orchestration**: Kubernetes
- **CI/CD**: GitHub Actions or GitLab CI
- **IaC**: Terraform

---

## Security Architecture

```mermaid
graph LR
    User[User] -->|HTTPS| CDN[CloudFront CDN]
    CDN --> WAF[Web Application Firewall]
    WAF --> LB[Load Balancer]
    LB --> App[Application<br/>TLS 1.3]

    App -->|Encrypted| DB[(Database<br/>Encrypted at Rest)]
    App -->|mTLS| [ Services]
    App -->|TLS| MercadoPago[MercadoPago REST API]

    App --> Vault[Secrets Manager<br/> Vault]
```

**Security Layers**:
1. **Transport**: HTTPS/TLS 1.3 end-to-end
2. **Authentication**:  IAM JWT validation
3. **Authorization**: Role-based access control (RBAC)
4. **Data**: Encryption at rest (AES-256)
5. **Secrets**: Managed via  Vault (no hardcoded secrets)
6. **WAF**: DDoS protection, SQL injection prevention

---

## Deployment Architecture

### Environments

```mermaid
graph LR
    subgraph "Development"
        Dev[Development<br/>• Local DB<br/>• Mock APIs<br/>• Hot reload]
    end

    subgraph "Staging"
        Stg[Staging<br/>• DB Snapshot<br/>• Similar to prod<br/>• Integration tests]
    end

    subgraph "Production"
        Prod[Production<br/>• Multi-AZ<br/>• Auto-scale<br/>• Full suite]
    end

    Dev -->|Git Flow| Stg
    Stg -->|Git Flow| Prod

    style Dev fill:#e3f2fd
    style Stg fill:#fff3e0
    style Prod fill:#e8f5e9
```

### Deployment Pipeline

```mermaid
graph TB
    Push[Code Push] --> Build[Build]
    Build --> Unit[Unit Tests]
    Unit --> Integration[Integration Tests]
    Integration --> DeployStaging[Deploy to Staging]
    DeployStaging --> E2E[E2E Tests on Staging]
    E2E --> Approval{Manual Approval}
    Approval -->|Approved| Canary[Canary Deploy 10%]
    Canary --> Monitor[Monitor Metrics 24h]
    Monitor --> Rollout[Full Rollout 100%]
    Approval -->|Rejected| Push

    style Push fill:#e3f2fd
    style Build fill:#e3f2fd
    style Unit fill:#fff3e0
    style Integration fill:#fff3e0
    style DeployStaging fill:#e8f5e9
    style E2E fill:#e8f5e9
    style Approval fill:#ffecb3
    style Canary fill:#f3e5f5
    style Monitor fill:#f3e5f5
    style Rollout fill:#c8e6c9
```

---

## Scalability Considerations

### Horizontal Scaling

- **API**: Stateless, can scale to N instances
- **Database**: Read replicas for read-heavy operations
- **Caching**:  Cache cluster with sharding

### Performance Bottlenecks

| Component | Potential Bottleneck | Mitigation |
|-----------|---------------------|------------|
| Database writes | High payment volume | Connection pooling, batch writes |
| External API | Stripe rate limits | Request queuing, retry logic |
| Cache invalidation | Stale data | TTL + event-driven invalidation |

---

## References

- Technical Spec: `../technical-spec.md`
- Functional Spec: `../../1-functional/spec.md`
- Standards: `../../../~/.sdd-kit/standards/architecture-patterns.md`
