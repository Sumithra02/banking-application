# Design Document: Account Email Lookup

## Overview

This feature adds an `email` field to the existing `Account` entity and exposes a new REST endpoint `GET /account/email/{email}` so that accounts can be retrieved by email address instead of only by numeric ID.

The change touches four layers of the existing layered architecture:

| Layer | Change |
|---|---|
| Entity | Add `email` field with `@Email`, `@NotBlank`, `@Column(unique=true)` |
| Repository | Add `findByEmail(String email)` derived query |
| Service | Add `getAccountByEmail(String email)` method |
| Controller | Add `GET /account/email/{email}` endpoint; update `updateAccount` to preserve email when not supplied |
| Exception | Add `DuplicateEmailException`; register handler in `GlobalExceptionHandler` |

No new libraries are required. All validation is handled by the already-present `spring-boot-starter-validation` dependency, and the duplicate-email database constraint is enforced via JPA `@Column(unique = true)`.

---

## Architecture

The feature follows the existing Controller → Service → Repository layered architecture. No new architectural patterns are introduced.

```mermaid
flowchart TD
    Client -->|HTTP GET /account/email/{email}| AC[AccountController]
    Client -->|HTTP POST /account| AC
    Client -->|HTTP PUT /account/{id}| AC
    AC -->|getAccountByEmail| AS[AccountService]
    AC -->|createAccount / updateAccount| AS
    AS -->|findByEmail| AR[AccountRepository]
    AS -->|findById / save| AR
    AR -->|SQL SELECT / INSERT / UPDATE| DB[(MySQL)]

    AC -->|throws| EH[GlobalExceptionHandler]
    AS -->|throws AccountNotFoundException\nDuplicateEmailException\nInvalidAmountException| EH
```

The `DuplicateEmailException` is a new custom exception that maps to HTTP 409. A `DataIntegrityViolationException` fallback is also handled in `GlobalExceptionHandler` to catch race-condition duplicate inserts at the database level.

---

## Components and Interfaces

### Account Entity (`com.example.banking.Entity.Account`)

**New field added:**
```java
@Email(message = "Email format is invalid")
@NotBlank(message = "Email is required")
@Column(unique = true, nullable = false, length = 320)
private String email;
```

The `@Column(unique = true)` creates a unique index in the `accounts` table. The `@NotBlank` + `@Email` Jakarta constraints handle format validation before the database is reached.

### AccountRepository (`com.example.banking.Repository.AccountRepository`)

**New derived query method:**
```java
Optional<Account> findByEmail(String email);
boolean existsByEmail(String email);
```

`existsByEmail` is used in `AccountService.createAccount` to detect duplicates before the INSERT and throw `DuplicateEmailException` with a clear message, avoiding a raw `DataIntegrityViolationException` reaching the client in the normal path.

### AccountService (`com.example.banking.Service.AccountService`)

**New method:**
```java
public Account getAccountByEmail(String email) {
    return accountRepository.findByEmail(email)
        .orElseThrow(() -> new AccountNotFoundException(
            "No account found for email: " + email));
}
```

**Modified `createAccount`:** checks `existsByEmail` before save; throws `DuplicateEmailException` on conflict.

**Modified `updateAccount`:** only replaces `email` when the incoming value is non-null and non-blank, preserving the existing email otherwise (Requirement 3.2).

### AccountController (`com.example.banking.Controller.AccountController`)

**New endpoint:**
```java
@GetMapping("/email/{email}")
public ResponseEntity<Account> getAccountByEmail(@PathVariable String email) {
    // validation: blank check + format check via @Email on path variable
    Account account = accountService.getAccountByEmail(email);
    return ResponseEntity.ok(account);
}
```

Path-variable validation uses `@Validated` at the class level and `@Email @NotBlank` annotations on the `email` parameter so that malformed emails return HTTP 400 before reaching the service layer.

### DuplicateEmailException (`com.example.banking.Exception.DuplicateEmailException`)

```java
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String message) { super(message); }
}
```

### GlobalExceptionHandler (`com.example.banking.Exception.GlobalExceptionHandler`)

**New handlers:**
```java
@ExceptionHandler(DuplicateEmailException.class)
public ResponseEntity<String> handleDuplicateEmail(DuplicateEmailException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
}

@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<String> handleDataIntegrity(DataIntegrityViolationException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body("Email address is already associated with an existing account");
}

@ExceptionHandler(ConstraintViolationException.class)
public ResponseEntity<String> handleConstraintViolation(ConstraintViolationException ex) {
    String message = ex.getConstraintViolations().stream()
        .map(v -> v.getMessage())
        .collect(Collectors.joining(", "));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
}
```

The `ConstraintViolationException` handler is required so that `@Email`/`@NotBlank` violations on path variables (not request body fields) are also caught and returned as HTTP 400.

---

## Data Models

### Account Entity (updated)

| Field | Java Type | DB Column | Constraints |
|---|---|---|---|
| `id` | `Long` | `id` BIGINT PK | Auto-generated |
| `name` | `String` | `name` VARCHAR | `@NotBlank` |
| `balance` | `double` | `balance` DOUBLE | `@PositiveOrZero` |
| `createdDate` | `LocalDateTime` | `created_date` DATETIME | Set on persist |
| `email` | `String` | `email` VARCHAR(320) | `@NotBlank`, `@Email`, UNIQUE, NOT NULL |

Hibernate `ddl-auto=update` (already configured) will add the new `email` column and its unique index automatically when the application starts against an existing schema. If the table already contains rows without an email value, a manual migration script is needed to backfill emails before the NOT NULL constraint can be enforced; this is documented in the Error Handling section.

### JSON Request / Response Shape

**POST /account (create)** — request body:
```json
{
  "name": "Alice",
  "balance": 1000.00,
  "email": "alice@example.com"
}
```

**GET /account/{id}**, **GET /account/email/{email}**, **PUT /account/{id}** — response body (all include `email`):
```json
{
  "id": 1,
  "name": "Alice",
  "balance": 1000.00,
  "createdDate": "2025-07-14T10:00:00",
  "email": "alice@example.com"
}
```

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Invalid email inputs are always rejected

*For any* string that does not conform to valid email format (blank, whitespace-only, missing `@`, missing domain, or exceeding 320 characters), submitting it as an email value in a create request, an update request, or as the email path parameter on `GET /account/email/{email}` SHALL always return HTTP 400 and no account SHALL be created or modified as a result.

**Validates: Requirements 1.2, 1.3, 2.3, 2.4, 3.3**

---

### Property 2: Email round-trip — create and retrieve preserves the email

*For any* valid email address `e`, if an account is successfully created with email `e`, then every retrieval endpoint (`GET /account/{id}`, `GET /account/email/{e}`, `POST /account` response) SHALL return a JSON body containing an `email` field whose value equals `e` exactly.

**Validates: Requirements 1.1, 2.1, 3.1**

---

### Property 3: Email uniqueness is enforced end-to-end

*For any* valid email address `e` that is already associated with an existing account, any subsequent create or update request that supplies `e` as the email SHALL return HTTP 409 and no new account SHALL be persisted with that email.

**Validates: Requirements 1.4, 1.5**

---

### Property 4: Account update correctly manages the email field

*For any* account with existing email `e1`: (a) if an update request supplies a valid, non-null email `e2 ≠ e1`, the account's email SHALL be replaced with `e2`; (b) if an update request supplies a null or absent email field, the account's email SHALL remain `e1` unchanged. In both cases the HTTP response SHALL be 200 and SHALL include the current email value.

**Validates: Requirements 3.2, 3.4**

---

## Error Handling

### HTTP Response Map

| Scenario | HTTP Status | Message |
|---|---|---|
| Blank / null email in request body | 400 Bad Request | "Email is required" |
| Malformed email in request body | 400 Bad Request | "Email format is invalid" |
| Blank / missing email path param on GET | 400 Bad Request | "Email is required" |
| Malformed email path param on GET | 400 Bad Request | "Email format is invalid" |
| Email not found on GET by email | 404 Not Found | "No account found for email: {email}" |
| Account not found by ID | 404 Not Found | "Account not found" |
| Duplicate email on create/update | 409 Conflict | "Email address is already associated with an existing account" |
| DB-level duplicate (race condition) | 409 Conflict | "Email address is already associated with an existing account" |

### Schema Migration Note

When deploying to an existing database that already has rows in the `accounts` table, Hibernate's `ddl-auto=update` cannot add a NOT NULL column without default data. Before deploying, run the following migration:

```sql
ALTER TABLE accounts ADD COLUMN email VARCHAR(320);
-- backfill with placeholder unique values if needed
UPDATE accounts SET email = CONCAT('placeholder-', id, '@migrate.local') WHERE email IS NULL;
ALTER TABLE accounts MODIFY COLUMN email VARCHAR(320) NOT NULL UNIQUE;
```

This is a one-time operational step outside the application code.

---

## Testing Strategy

### Unit Tests (Service Layer — Mockito)

Test `AccountService` in isolation with a mocked `AccountRepository`:

| Test Case | Verifies |
|---|---|
| `createAccount` with valid email saves and returns account | Req 1.1 |
| `createAccount` with duplicate email throws `DuplicateEmailException` | Req 1.5 |
| `getAccountByEmail` with existing email returns account | Req 2.1 |
| `getAccountByEmail` with unknown email throws `AccountNotFoundException` | Req 2.2 |
| `updateAccount` with null email preserves existing email | Req 3.2 |
| `updateAccount` with valid new email replaces existing email | Req 3.4 |

### Unit Tests (Controller Layer — MockMvc)

Test `AccountController` with `@WebMvcTest`, mocking `AccountService`:

| Test Case | Expected Status |
|---|---|
| POST `/account` with valid email | 200 |
| POST `/account` with blank email | 400 |
| POST `/account` with malformed email (`notanemail`) | 400 |
| POST `/account` with duplicate email (service throws `DuplicateEmailException`) | 409 |
| GET `/account/email/alice@example.com` — found | 200 with email in body |
| GET `/account/email/alice@example.com` — not found | 404 |
| GET `/account/email/notanemail` (malformed path param) | 400 |
| PUT `/account/{id}` with null email — email field preserved in response | 200 |

### Integration Tests (Repository Layer — `@DataJpaTest`)

Test `AccountRepository` against an in-memory H2 database:

| Test Case | Verifies |
|---|---|
| Save two accounts with same email throws constraint violation | Req 1.4 |
| `findByEmail` returns correct account | Req 2.1 |
| `findByEmail` returns empty Optional for unknown email | Req 2.2 |
| `existsByEmail` returns true/false correctly | Repository contract |

### Property-Based Tests

This feature has universal properties that benefit from randomized input generation, primarily around email validation and uniqueness rules. Use **jqwik** (available on the classpath alongside JUnit 5 in Spring Boot's test scope) for property-based tests.

Each property test should be configured with at least 100 tries (`@Property(tries = 100)`).

| Property Test | Generator | Tag |
|---|---|---|
| Property 1: Invalid emails always produce HTTP 400 | Arbitrary blank / whitespace strings + syntactically invalid email strings | `Feature: account-email-lookup, Property 1: Invalid email inputs are always rejected` |
| Property 2: Email round-trip on create + retrieve | Arbitrary valid RFC 5321 email strings (length ≤ 320) | `Feature: account-email-lookup, Property 2: Email round-trip — create and retrieve preserves the email` |
| Property 3: Duplicate email always produces HTTP 409 | Arbitrary valid email strings used twice | `Feature: account-email-lookup, Property 3: Email uniqueness is enforced end-to-end` |
| Property 4: Update email field management | Pairs of distinct valid email strings | `Feature: account-email-lookup, Property 4: Account update correctly manages the email field` |

Property tests for Properties 2, 3, and 4 should be run as `@DataJpaTest` integration tests against an in-memory H2 database to keep cost low while testing realistic persistence behavior.
