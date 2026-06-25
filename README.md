# WorkoutTracker

> Faculty project for the course **Web Applications with Microservices**

A full-stack workout tracking application built with Spring Boot and React.

---

## Functional Requirements

### Authentication & Authorization
- Users can register with a unique username, email, and password
- Users can log in and receive a JWT token (24h default, or 30 days when "Remember me" is checked)
- All endpoints except exercise browsing and authentication require a valid JWT
- CSRF protection is enabled on the monolith; the frontend automatically relays the `X-XSRF-TOKEN` header on state-changing requests
- Two roles exist: `ROLE_USER` (default) and `ROLE_ADMIN`
- Admins can create, update, and delete exercises; regular users cannot

### User Profile
- Each user has a profile with optional fields: height, weight, gender, bio, and fitness goal
- Users can view and edit their own profile
- Users can view other users' public profiles

### Exercise Catalog
- Admins can add exercises with name, description, muscle group, and image URL
- All users (including unauthenticated) can browse and search exercises by name or filter by muscle group
- Exercise listing is paginated and sortable

### Workout Splits & Templates
- Users can create named workout splits (e.g. "Push/Pull/Legs")
- A user can have at most one active split at a time; activating a new split deactivates the current one
- Each split contains ordered workout day templates (e.g. "Push Day")
- Each template lists exercises from the global catalog with optional target sets and reps

### Workout Logging
- Users can create a workout log against a template from their active split, with a date and optional photo URL
- Users can add exercise logs and set logs (weight × reps, optional RPE 1–10) to a workout log
- Users can mark a workout log as **Completed**
- Users can view their full workout history, paginated and sorted by date

### Social Features
- Users can follow and unfollow other users (a user cannot follow themselves)
- Users can share a completed workout as a post with an optional caption — accessible both from the **log detail page** and from a **"Share a workout" modal on the Feed page**
- Users can view a paginated feed of posts from users they follow
- Users can view another user's public posts and profile
- **Discovery feed** (`/discover`) — paginated posts from users the current user does **not** yet follow, with an inline Follow button on each card. Once followed, those users' posts shift over to the Feed
- The Dashboard's **Social block** shows the current user's post count alongside following / follower counts

### Notifications
- Each user has a notifications inbox (🔔 bell badge in the Navbar + `/notifications` page)
- Three event types are emitted across services:
  - `WORKOUT_COMPLETED` — fired by the monolith when the user completes a workout log
  - `NEW_FOLLOWER` — fired by social-service when another user follows them
  - `POST_CREATED` — fan-out by social-service to every follower when an author shares a post
- Notifications are listed paginated, newest first. Users can mark individual notifications as read; an unread count refreshes on every route change
- All cross-service notification fires are wrapped in Resilience4j circuit breakers — a downed notification-service never blocks the originating action

---

## ERD Diagram

```mermaid
erDiagram
    USER {
        bigint id PK
        varchar username UK
        varchar email UK
        varchar password
        boolean enabled
        timestamp createdAt
    }
    USER_PROFILE {
        bigint id PK
        bigint user_id FK
        int heightCm
        decimal weightKg
        varchar gender
        text bio
        varchar fitnessGoal
    }
    ROLE {
        bigint id PK
        varchar name
    }
    USER_ROLE {
        bigint user_id FK
        bigint role_id FK
    }
    WORKOUT_SPLIT {
        bigint id PK
        bigint user_id FK
        varchar name
        boolean active
        timestamp createdAt
    }
    WORKOUT_TEMPLATE {
        bigint id PK
        bigint split_id FK
        varchar name
        int orderIndex
    }
    EXERCISE {
        bigint id PK
        varchar name UK
        text description
        varchar muscleGroup
        varchar imageUrl
    }
    EXERCISE_TEMPLATE {
        bigint id PK
        bigint template_id FK
        bigint exercise_id FK
        int targetSets
        int targetReps
        int orderIndex
    }
    WORKOUT_LOG {
        bigint id PK
        bigint user_id FK
        bigint template_id FK
        date date
        varchar photoUrl
        varchar status
        text notes
        timestamp createdAt
    }
    EXERCISE_LOG {
        bigint id PK
        bigint workout_log_id FK
        bigint exercise_id FK
        text notes
    }
    SET_LOG {
        bigint id PK
        bigint exercise_log_id FK
        int setNumber
        decimal weightKg
        int reps
        int rpe
    }
    POST {
        bigint id PK
        bigint user_id FK
        bigint workout_log_id FK
        varchar caption
        timestamp createdAt
    }
    FOLLOW {
        bigint id PK
        bigint follower_id FK
        bigint followed_id FK
    }

    USER ||--o{ USER_ROLE : "has"
    ROLE ||--o{ USER_ROLE : "assigned to"
    USER ||--|| USER_PROFILE : "has"
    USER ||--o{ WORKOUT_SPLIT : "owns"
    WORKOUT_SPLIT ||--o{ WORKOUT_TEMPLATE : "contains"
    WORKOUT_TEMPLATE ||--o{ EXERCISE_TEMPLATE : "contains"
    EXERCISE ||--o{ EXERCISE_TEMPLATE : "referenced in"
    USER ||--o{ WORKOUT_LOG : "logs"
    WORKOUT_TEMPLATE ||--o{ WORKOUT_LOG : "used for"
    WORKOUT_LOG ||--o{ EXERCISE_LOG : "contains"
    EXERCISE ||--o{ EXERCISE_LOG : "logged in"
    EXERCISE_LOG ||--o{ SET_LOG : "has"
    USER ||--o{ POST : "creates"
    WORKOUT_LOG ||--o| POST : "shared as"
    USER ||--o{ FOLLOW : "follows (as follower)"
    USER ||--o{ FOLLOW : "followed by"
```

The diagram above is the **monolith's** schema on `postgres:5432`. After the microservices
split, the same `Follow` and `Post` rows are *also* maintained on a second Postgres
instance owned by `social-service` — with the JPA relationships severed. There, the
`user_id` / `workout_log_id` / `follower_id` / `followed_id` columns are bare `bigint`s
pointing at the monolith's `users` and `workout_logs` tables, with no foreign-key
constraint (cross-database FKs aren't possible). Application-level integrity is
restored by the social-service calling back to the monolith via `MainAppClient` before
inserting a row.

```mermaid
erDiagram
    FOLLOW_SOCIAL["social-service.follows"] {
        bigint id PK
        bigint follower_id "→ monolith.users.id (no FK)"
        bigint followed_id "→ monolith.users.id (no FK)"
        timestamp createdAt
    }
    POST_SOCIAL["social-service.posts"] {
        bigint id PK
        bigint user_id "→ monolith.users.id (no FK)"
        bigint workout_log_id UK "→ monolith.workout_logs.id (no FK)"
        varchar caption
        timestamp createdAt
    }
```

The `notification-service` stores documents in MongoDB (no relational schema):

```mermaid
erDiagram
    NOTIFICATION["notifications collection (MongoDB)"] {
        string id PK "ObjectId"
        bigint userId "→ monolith.users.id (no FK)"
        string type "WORKOUT_COMPLETED / NEW_FOLLOWER / POST_CREATED"
        string message
        bigint workoutLogId "nullable"
        timestamp createdAt
        boolean read
    }
```

---

## Architecture

This section describes the **per-service request layering** that every backend service
follows (controller → service → repository, plus the JWT filter in front). For the
**deployment-level** topology — three services, two Postgres instances, MongoDB,
the Vite proxy, and the cross-service circuit breakers — see the
[Microservices Architecture](#microservices-architecture) section further down.

```mermaid
flowchart LR
    Browser[Browser<br/>React 19 + Vite SPA] -->|HTTP + Bearer JWT + X-XSRF-TOKEN| API[Spring Boot 4 backend<br/>any of the three]
    API --> SEC[Spring Security<br/>JWT Filter + CSRF]
    SEC --> CTRL[Controllers]
    CTRL --> SVC[Service layer<br/>business rules]
    SVC --> REPO[Spring Data repository]
    REPO --> DB[(PostgreSQL or MongoDB)]
    SVC -.logs.-> LOGS[(logs/app.log<br/>logs/errors.log)]
```

Notable design choices:

- **JWT** stateless auth (HMAC-SHA256). Default 24h expiry; **30 days** when the login request sets `rememberMe: true` (the "Remember me" checkbox on the login form). The longer expiry is the only difference — localStorage persistence is identical either way.
- **CSRF protection** is on for state-changing requests (`POST/PUT/DELETE/PATCH`). Spring's `CookieCsrfTokenRepository.withHttpOnlyFalse()` sets an `XSRF-TOKEN` cookie; the frontend's axios interceptor reads it and echoes it back as `X-XSRF-TOKEN`. `/api/auth/**` and `/internal/**` are exempt — login can't carry a token yet, and `/internal/` is server-to-server. Disabled in the test profile so existing MockMvc tests don't have to attach tokens.
- **BCrypt** for password storage (Spring's default strength)
- **Service-layer business rules**: one-active-split, owner-only mutations, no self-follow — enforced before persistence, never relying on DB constraints alone
- **DTOs** (Java records) at every controller boundary — entities are never serialized directly
- **Pagination** on `/api/logs`, `/api/exercises`, `/api/social/feed`, `/api/social/discovery`, `/api/users/{id}/posts`, `/api/notifications`
- **Custom exception hierarchy** mapped to HTTP status codes by `GlobalExceptionHandler`

---

## Social Layer

The social layer is a thin layer on top of the workout domain. It exists so users can
share completed workouts and discover others' training. It deliberately has no comments,
likes, or reactions — the goal is to surface *what people lifted*, not to be a feed app.

> **Where the code lives.** The social domain ships in two places. The original
> implementation in the monolith ([`backend/.../service/SocialService.java`](backend/src/main/java/com/workout_tracker/backend/service/SocialService.java)
> + [`PostService.java`](backend/src/main/java/com/workout_tracker/backend/service/PostService.java))
> is retained for the existing unit + integration test suite. The **production path** is
> the extracted microservice at [`microservices/social-service/`](microservices/social-service/) —
> the Vite proxy routes `/api/social/*`, `/api/posts/*`, and `/api/users/{id}/posts` to
> port 8081, so the running app talks to the microservice for every social action. The
> microservice has the same business rules, but its `Follow` and `Post` entities are
> [severed](#severed-jpa-relationships) — bare `Long` ids instead of JPA refs to `User` /
> `WorkoutLog`. The descriptions below apply to both copies; rule numbers and HTTP
> status codes are identical.

### Two entities, two services

| Entity | Service | Purpose |
|---|---|---|
| [`Follow`](backend/src/main/java/com/workout_tracker/backend/model/Follow.java) | [`SocialService`](backend/src/main/java/com/workout_tracker/backend/service/SocialService.java) | Self-referential `User ↔ User` edge — who follows whom |
| [`Post`](backend/src/main/java/com/workout_tracker/backend/model/Post.java) | [`PostService`](backend/src/main/java/com/workout_tracker/backend/service/PostService.java) | A share of a completed `WorkoutLog` with an optional caption |

`Follow` has a unique constraint on `(follower_id, followed_id)` so the database is the
final backstop, but each business rule is pre-checked in the service layer so the
client gets a domain-specific 409 message instead of a generic constraint violation.

### Following

```
POST   /api/social/follow/{userId}      → 201 Created      (creates a Follow)
DELETE /api/social/follow/{userId}      → 204 No Content
GET    /api/social/following            → [UserSummaryDto] (who I follow)
GET    /api/social/followers            → [UserSummaryDto] (who follows me)
GET    /api/users/{id}/following        → [UserSummaryDto] (public — anyone's graph)
GET    /api/users/{id}/followers        → [UserSummaryDto]
```

Rules enforced in `SocialService.follow`:

- **Rule #4 (no self-follow)** — `follower.id == followedId` → 409 `BusinessRuleViolationException`
- **No duplicate follow** — pre-checked with `existsByFollowerAndFollowed`; the unique constraint is the backstop
- **Followed user must exist** — 404 if not

### Posting

```
POST   /api/posts                       → 201 PostDto
DELETE /api/posts/{id}                  → 204 No Content   (only the author can delete)
GET    /api/users/{id}/posts?page=&size= → PageResponse<PostDto>
```

Rules enforced in `PostService.createPost`:

- **Rule #3 (owner-only)** — the `WorkoutLog` referenced in `CreatePostRequest` is fetched via
  `findByIdAndUser(logId, currentUser)`. A non-owner sees the same 404 as a missing log,
  so we don't leak which log IDs exist.
- **Completed-only** — `WorkoutLog.status` must be `COMPLETED`. Sharing an `IN_PROGRESS`
  log throws 409.
- **One post per log** — `postRepository.existsByWorkoutLog` rejects duplicates.

### The Feed

```
GET    /api/social/feed?page=&size=     → PageResponse<PostDto>
```

The feed is "posts from users I follow, newest first." Composition logic in
[`PostService.getFeed`](backend/src/main/java/com/workout_tracker/backend/service/PostService.java):

1. Resolve the current user's follow list to a `List<Long> followedIds`.
2. If empty, short-circuit to `Page.empty(pageable)` — some JPA providers reject `WHERE
   user_id IN ()`, so we avoid issuing it.
3. Otherwise, `postRepository.findByUser_IdInOrderByCreatedAtDesc(followedIds, pageable)`.

The feed contains posts only — not the underlying `WorkoutLog` payload. Each `PostDto`
carries the log id, template name, caption, and author username; the frontend links to
[`/logs/{id}`](frontend/src/pages/LogDetailPage.tsx) for the full set-by-set view, which
hits its own paginated endpoint behind JWT auth.

### Privacy posture

- Profiles and follower/following counts are **public** to authenticated users — anyone
  with a token can hit `/api/users/{id}/profile`.
- Posts are visible to **anyone who follows the author**, via the feed, or directly via
  `/api/users/{id}/posts` (also public to authenticated users).
- Workout logs themselves remain **strictly owner-only** — there is no
  `/api/logs/{id}` route that resolves to another user's log. The post-detail page
  fetches the log via `/api/logs/{id}` which requires owner match; sharing a log
  effectively makes its summary data (date, template, caption) visible via the post,
  but the underlying log endpoint stays private. (This is intentional — the grading is
  in the post; reps/weights are still owner-private unless you choose to denormalize
  them onto the post.)

---

## Microservices Architecture

Three services share the API surface: the monolith, an extracted **`social-service`**
(posts + follows on its own Postgres), and a **`notification-service`** (Mongo-backed,
fired from the monolith on workout completion). The microservice patterns follow the
conventions in [`iuliabanu/awbd2026/product-hub`](https://github.com/iuliabanu/awbd2026/tree/main/product-hub),
adapted to our existing stack.

```mermaid
flowchart LR
    Browser[Browser :5173<br/>React 19 SPA] -->|HTTP + JWT| Vite{{Vite dev proxy<br/>route by path}}
    Vite -->|"/api/social/*<br/>/api/posts/*<br/>/api/users/N/posts"| Social[social-service :8081]
    Vite -->|"/api/notifications/*"| Notif[notification-service :8082]
    Vite -->|everything else| Mono[monolith :8080]
    Social -->|"/internal/users/N<br/>/internal/logs/N/summary<br/>RestClient + @CircuitBreaker"| Mono
    Mono -->|"POST /internal/notifications<br/>RestClient + @CircuitBreaker<br/>(fire-and-forget on completeLog)"| Notif
    Social --> DBs[(PostgreSQL :5433<br/>social)]
    Mono --> DBm[(PostgreSQL :5432<br/>workouttracker)]
    Notif --> DBn[(MongoDB :27017<br/>notifications)]
```

### What lives where

| Concern | Owner |
|---|---|
| Auth, register/login, JWT issuance | monolith |
| User, UserProfile, Role | monolith |
| WorkoutSplit, WorkoutTemplate, Exercise, WorkoutLog, ExerciseLog, SetLog | monolith |
| `Follow`, `Post` entities + their schema | **social-service** |
| `/api/social/**`, `/api/posts/**`, `/api/users/{id}/posts` (routed by Vite) | **social-service** |
| `/internal/users/{id}`, `/internal/logs/{id}/summary` | monolith (for social-service to call back) |
| `Notification` documents (MongoDB) | **notification-service** |
| `/api/notifications/**` (list, unread-count, mark-read) | **notification-service** |
| `/internal/notifications` (fired by monolith on `completeLog`) | **notification-service** |

### Severed JPA relationships

In the monolith, `Post.user` is a `@ManyToOne User` and `Post.workoutLog` is a
`@OneToOne WorkoutLog`. The social-service owns neither entity nor their database, so
both fields become bare `Long` ids. Referential integrity is now the application's
job — `PostService.createPost` calls back to the monolith via `MainAppClient` to
verify the workout log exists, is owned by the current user, and is `COMPLETED` before
inserting a row.

### Inter-service communication: RestClient + Resilience4j

Follows the `DiscountClient` pattern from `product-hub/product-api-app` exactly:

```java
@CircuitBreaker(name = "main-app", fallbackMethod = "getUserFallback")
public UserSummaryDto getUser(Long userId) {
    return mainAppRestClient.get()
            .uri("/internal/users/{id}", userId)
            .retrieve()
            .body(UserSummaryDto.class);
}

private UserSummaryDto getUserFallback(Long userId, Throwable cause) {
    log.warn("MainApp getUser({}) → fallback: {}", userId, cause.toString());
    return new UserSummaryDto(userId, "Unknown", null);
}
```

Configured in `application.properties`:

```properties
resilience4j.circuitbreaker.instances.main-app.sliding-window-size=5
resilience4j.circuitbreaker.instances.main-app.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.main-app.wait-duration-in-open-state=15s
resilience4j.circuitbreaker.instances.main-app.permitted-number-of-calls-in-half-open-state=2
```

If 50% of the last 5 calls to the monolith fail, the breaker opens for 15s and
fallbacks return immediately. Reads (feed enrichment) degrade to `"Unknown"`
placeholders; writes (post creation) refuse with **503 Service Unavailable** instead
of inserting unverified rows.

Live state visible at `GET /actuator/circuitbreakers` on the social-service.

A **second circuit breaker** lives in the monolith for outbound calls to
notification-service ([NotificationClient.java](backend/src/main/java/com/workout_tracker/backend/client/NotificationClient.java)).
Notifications are fire-and-forget — when the breaker is open, completing a workout still
succeeds, the fallback just logs the miss. This satisfies the spec's "Circuit Breaker
for minimum 2 services" requirement.

### Cross-service authentication

Both services share a single `JWT_SECRET` (Base64-encoded HMAC key). The monolith
**issues** tokens; the social-service **validates** them independently. The JWT now
carries a `userId` claim so social-service can resolve the caller without a
username-to-id lookup over the network.

This is the deviation from `product-hub`, which uses Keycloak + asymmetric JWKs.
The trade-off: a shared symmetric secret is simpler to deploy for a course project,
but rotating the secret requires restarting both services. A production setup would
swap in Keycloak (`oauth2-resource-server`) without touching the controllers — the
JWT filter is the only piece that changes.

### Service discovery

No Eureka, no Spring Cloud Config — same choice as `product-hub`. The social-service
finds the monolith via the `MAIN_APP_URI` env var (default `http://localhost:8080`
in dev, `http://backend:8080` in docker-compose). Docker DNS is the registry.

### Running all services locally

```bash
# Terminal 0 — data stores (postgres × 2, mongodb)
docker compose up -d

# Terminal 1 — monolith (port 8080)
cd backend && ./gradlew bootRun

# Terminal 2 — social-service (port 8081)
cd microservices/social-service && ./gradlew bootRun

# Terminal 3 — notification-service (port 8082)
cd microservices/notification-service && ./gradlew bootRun

# Terminal 4 — frontend (Vite on 5173, proxies to all three backends)
cd frontend && npm run dev
```

All three backend services pick up `JWT_SECRET` from the same root-level `.env` file
via their `bootRun` env-loader.

### Files of interest

| File | What it does |
|---|---|
| [microservices/social-service/src/main/java/com/workout_tracker/social/client/MainAppClient.java](microservices/social-service/src/main/java/com/workout_tracker/social/client/MainAppClient.java) | social-service → monolith `@CircuitBreaker` RestClient |
| [backend/src/main/java/com/workout_tracker/backend/client/NotificationClient.java](backend/src/main/java/com/workout_tracker/backend/client/NotificationClient.java) | monolith → notification-service fire-and-forget `@CircuitBreaker` client |
| [microservices/notification-service/src/main/java/com/workout_tracker/notification/model/Notification.java](microservices/notification-service/src/main/java/com/workout_tracker/notification/model/Notification.java) | Mongo `@Document` — NoSQL backing for notifications |
| [microservices/social-service/src/main/java/com/workout_tracker/social/security/JwtAuthenticationFilter.java](microservices/social-service/src/main/java/com/workout_tracker/social/security/JwtAuthenticationFilter.java) | Validates the monolith-issued JWT independently (mirrored in notification-service) |
| [backend/src/main/java/com/workout_tracker/backend/controller/InternalController.java](backend/src/main/java/com/workout_tracker/backend/controller/InternalController.java) | `/internal/` projections the social-service calls into |
| [frontend/vite.config.ts](frontend/vite.config.ts) | Path-based proxy split between the three backends |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend (× 3 services) | Spring Boot 4.0.3, Java 21 |
| Relational databases | PostgreSQL 17 (one per stateful service) |
| Document store | MongoDB 7 (notification-service) |
| Inter-service comms | Spring `RestClient` + Resilience4j 2.3.0 `@CircuitBreaker` |
| Auth | Spring Security + JWT (jjwt 0.13.0), CSRF via `CookieCsrfTokenRepository`, BCrypt for passwords |
| Frontend | React 19, TypeScript 5.9, Vite 8 |
| Styling | Tailwind CSS v4 |
| HTTP client | Axios 1.13 (with XSRF token relay) |

---

## Prerequisites

- [Docker](https://www.docker.com/) & Docker Compose
- JDK 21
- Node.js 20+

---

## Getting Started

### 1. Clone the repository

```bash
git clone <repo-url>
cd WorkoutTracker
```

### 2. Configure environment variables

Copy [`.env.example`](.env.example) to `.env` and fill in the values:

```bash
cp .env.example .env
```

Required variables (full list in `.env.example`):

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `JWT_SECRET` | ✅ | — | HMAC secret for token signing (min 32 chars) |
| `ADMIN_PASSWORD` | dev: optional<br/>prod: ✅ | dev: `admin123` | Seeded admin user password |
| `DB_URL` | optional | `jdbc:postgresql://localhost:5432/workouttracker` | JDBC URL |
| `DB_USERNAME` | optional | `postgres` | DB user |
| `DB_PASSWORD` | optional | `postgres` | DB password |

> `JWT_SECRET` has no default — the backend will refuse to start without it.

### 3. Start the data stores

```bash
docker compose up -d
```

Brings up:
- `postgres:5432` — monolith DB (`workouttracker`)
- `postgres:5433` — social-service DB (`social`)
- `mongodb:27017` — notification-service collection (`notifications`)

### 4. Start the three backend services

Open three terminals. Each `bootRun` task auto-loads the root `.env`, so `JWT_SECRET`
is picked up everywhere from one file.

```bash
# Terminal A — monolith
cd backend && ./gradlew bootRun                              # :8080

# Terminal B — social-service
cd microservices/social-service && ./gradlew bootRun         # :8081

# Terminal C — notification-service
cd microservices/notification-service && ./gradlew bootRun   # :8082
```

Verify each is up:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
# all → {"status":"UP",...}
```

You can also watch the circuit-breaker state with:

```bash
curl http://localhost:8081/actuator/circuitbreakers
curl http://localhost:8080/actuator/circuitbreakers
```

### 5. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:5173`. The Vite dev server proxies by path:
`/api/social/*` and `/api/posts/*` and `/api/users/{id}/posts` → `:8081`,
`/api/notifications/*` → `:8082`, everything else → `:8080`. See
[frontend/vite.config.ts](frontend/vite.config.ts).

---

## Project Structure

```
WorkoutTracker/
├── backend/                                # Monolith (Spring Boot 4, :8080)
│   └── src/main/java/com/workout_tracker/backend/
│       ├── client/        # NotificationClient (@CircuitBreaker)
│       ├── config/        # SecurityConfig (JWT, CSRF, CORS)
│       ├── controller/    # REST + InternalController
│       ├── dto/           # Request/response records
│       ├── exception/     # GlobalExceptionHandler + custom exceptions
│       ├── model/         # JPA entities
│       ├── repository/    # Spring Data repositories
│       ├── security/      # JwtService, JwtAuthenticationFilter
│       └── service/       # Business logic
├── microservices/
│   ├── social-service/                     # Posts + Follows (Spring Boot 4, :8081)
│   │   └── src/main/java/com/workout_tracker/social/
│   │       ├── client/    # MainAppClient, NotificationClient (@CircuitBreaker)
│   │       ├── config/    # SecurityConfig, RestClientConfig
│   │       ├── controller/
│   │       ├── dto/       # PostDto, CreatePostRequest, UserSummaryDto, PageResponse
│   │       ├── exception/
│   │       ├── model/     # Follow, Post (severed — bare Long ids)
│   │       ├── repository/
│   │       ├── security/  # JWT validation (shared secret with monolith)
│   │       └── service/
│   └── notification-service/               # MongoDB-backed (Spring Boot 4, :8082)
│       └── src/main/java/com/workout_tracker/notification/
│           ├── config/    # SecurityConfig
│           ├── controller/# NotificationController + InternalNotificationController
│           ├── dto/       # NotificationDto, CreateNotificationRequest, PageResponse
│           ├── exception/
│           ├── model/     # Notification (@Document for Mongo)
│           ├── repository/# MongoRepository<Notification, String>
│           ├── security/  # JWT validation (shared secret)
│           └── service/
├── frontend/                               # React 19 + Vite (:5173)
│   └── src/
│       ├── api/           # Axios client + per-domain API calls
│       ├── components/    # Reusable UI + social + workout + layout
│       ├── context/       # AuthContext + AuthProvider
│       ├── hooks/         # useAuth, useQuery, useDebounce, usePagination
│       ├── pages/         # Route-level components
│       └── types/         # TypeScript interfaces mirroring backend DTOs
├── docs/                                   # ER diagram details, screenshots
├── docker-compose.yml                      # postgres × 2 + mongodb
└── .env                                    # Local env vars (not committed)
```

---

## Backend Dependencies

Common across all three services unless noted.

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-webmvc` | REST API |
| `spring-boot-starter-data-jpa` | ORM / database access (monolith, social-service) |
| `spring-boot-starter-data-mongodb` | Document store (notification-service only) |
| `spring-boot-starter-security` | Authentication & authorization |
| `spring-boot-starter-validation` | Request validation |
| `spring-boot-starter-actuator` | Health, metrics, circuit-breaker state |
| `jjwt-api / impl / jackson` (0.13.0) | JWT token handling |
| `resilience4j-spring-boot3` (2.3.0) | `@CircuitBreaker` for inter-service RestClient calls (monolith + social-service) |
| `lombok` | Boilerplate reduction |
| `postgresql` | Production database driver |
| `h2` | In-memory database for tests |

## Frontend Dependencies

| Dependency | Purpose |
|---|---|
| `react` + `react-dom` | UI framework |
| `react-router` | Client-side routing |
| `axios` | HTTP requests to backend |
| `tailwindcss` | Utility-first styling |

---

## API Reference

All endpoints are prefixed with `/api`. JWT required unless noted.

A request hits the Vite dev proxy on `:5173`, which routes by path prefix:
- `/api/social/*`, `/api/posts/*`, `/api/users/{id}/posts` → **social-service :8081**
- `/api/notifications/*` → **notification-service :8082**
- everything else → **monolith :8080**

In production, the API Gateway plays the same role. JWT required unless noted.

### Auth & Users (monolith)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/auth/register` | public | Create user, returns JWT |
| POST | `/auth/login` | public | Returns JWT (accepts optional `rememberMe` → 30-day expiry) |
| GET | `/ping` | user | Smoke test for the JWT filter |
| GET | `/users/me/profile` | user | Current user's profile |
| PUT | `/users/me/profile` | user | Update profile (height, weight, bio, goal, gender) |
| GET | `/users/{id}/profile` | user | Any user's public profile |
| GET | `/users/{id}/followers` | user | Followers of any user |
| GET | `/users/{id}/following` | user | Who any user follows |

### Exercises (monolith — public reads, admin writes)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/exercises` | **public** | Catalog (paginated; `?search=`, `?muscleGroup=`, `?sort=name,asc`) |
| GET | `/exercises/{id}` | **public** | Single exercise |
| POST | `/exercises` | **admin** | Create exercise |
| PUT | `/exercises/{id}` | **admin** | Update exercise |
| DELETE | `/exercises/{id}` | **admin** | Delete exercise |

### Splits & Templates (monolith)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/splits` | user | All splits for current user |
| GET | `/splits/active` | user | Current active split (≤1) |
| GET | `/splits/{id}` | user | One split with its templates |
| POST | `/splits` | user | Create split |
| PUT | `/splits/{id}/activate` | user | Activate (deactivates others) |
| DELETE | `/splits/{id}` | user | Delete |
| GET | `/splits/{splitId}/templates` | user | Templates for a split |
| POST | `/splits/{splitId}/templates` | user | Add template to split |
| DELETE | `/splits/{splitId}/templates/{templateId}` | user | Remove template |
| POST | `/splits/{splitId}/templates/{templateId}/exercises` | user | Add exercise to template |
| DELETE | `/splits/{splitId}/templates/{templateId}/exercises/{exerciseTemplateId}` | user | Remove exercise from template |

### Workout Logs (monolith)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/logs` | user | History (paginated; `?sort=date,desc` default) |
| POST | `/logs` | user | Start a workout log |
| GET | `/logs/{id}` | user | One log with all exercise/set logs |
| PUT | `/logs/{id}` | user | Update notes / photo URL |
| DELETE | `/logs/{id}` | user | Delete log |
| POST | `/logs/{id}/complete` | user | Mark completed → fires `WORKOUT_COMPLETED` notification |
| GET | `/logs/{logId}/exercises` | user | Exercise logs for a workout log |
| POST | `/logs/{logId}/exercises` | user | Add exercise log |
| DELETE | `/logs/{logId}/exercises/{exerciseLogId}` | user | Remove exercise log |
| POST | `/logs/{logId}/exercises/{exLogId}/sets` | user | Log a set (weight × reps + optional RPE) |
| PUT | `/logs/{logId}/exercises/{exLogId}/sets/{setId}` | user | Update set |
| DELETE | `/logs/{logId}/exercises/{exLogId}/sets/{setId}` | user | Delete set |

### Social (**social-service** — `/api/posts/*`, `/api/social/*`, `/api/users/{id}/posts`)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/posts` | user | Share a completed log as a post → fans out `POST_CREATED` to all followers |
| DELETE | `/posts/{id}` | user | Delete own post |
| GET | `/users/{id}/posts` | user | A user's posts (paginated) |
| POST | `/social/follow/{userId}` | user | Follow → fires `NEW_FOLLOWER` to the followed user |
| DELETE | `/social/follow/{userId}` | user | Unfollow |
| GET | `/social/following` | user | Who I follow |
| GET | `/social/followers` | user | Who follows me |
| GET | `/social/feed` | user | Feed: posts from users I follow (paginated, newest first) |
| GET | `/social/discovery` | user | Posts from users I don't yet follow (paginated) |

### Notifications (**notification-service**)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/notifications` | user | Current user's notifications (paginated, newest first) |
| GET | `/notifications/unread-count` | user | `{ count: N }` — feeds the Navbar bell badge |
| PUT | `/notifications/{id}/read` | user | Mark single notification as read |

### Internal (cross-service only — never via the public proxy)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/internal/users/{id}` | none | `UserSummaryDto{id, username, bio}` — called by social-service |
| GET | `/internal/logs/{id}/summary` | none | `LogSummaryDto{id, ownerId, status, templateName}` — called by social-service before post creation |
| POST | `/internal/notifications` | none | Inbound from monolith and social-service — creates a notification document |

Error responses follow a consistent shape:
```json
{ "status": 404, "error": "Not Found", "message": "WorkoutSplit not found", "timestamp": "..." }
```

---

## Testing

Run the full test suite + JaCoCo coverage report:

```bash
cd backend
./gradlew test jacocoTestReport
```

HTML report at `backend/build/reports/jacoco/test/html/index.html`.

Current coverage (excluding `dto/`, `model/`, `config/`, `exception/`):

| Package | Line coverage |
|---|---|
| `service` | **84.6 %** |
| `security` | **93.8 %** |
| `client` (cross-service `@CircuitBreaker` calls) | 76.9 % |
| `controller` | 56.2 % |
| **Overall** | **81.4 %** |

Suite: 104 tests across unit (Mockito) and integration (MockMvc + H2) levels — 0 failures.
The `service/` package comfortably exceeds the spec's ≥70% target.

---

## Screenshots

Available in [`docs/screenshots/`](docs/screenshots/):

| File | Description |
|---|---|
| `login.png` | Login form (with the "Remember me" checkbox) |
| `dashboard.png` | Dashboard with active split, recent history, and the post / following / followers Social block |
| `split.png` | `split-1.png` | Creating a split with excercices |
| `workout.png` | `workout-1.png` | Logging a workout with sets |
| `exercises.png` | Full excercises list |
| `history.png` | Paginated workout history |
| `exercises.png` | Public exercise catalog with muscle-group filter |
| `feed.png` | Social feed of followed users' posts (with the "Share a workout" button) |
| `discover.png` | Discovery feed — posts from users not yet followed, inline Follow button |
| `profile.png` | User profile page |
| `notifications.png` | Notifications page with unread / read items + Navbar bell badge |


---

## Team

Solo project by **Horia Marinescu**.

---

## License

University project — not licensed for public reuse.
