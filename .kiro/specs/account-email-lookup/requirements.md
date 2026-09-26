# Requirements Document

## Introduction

This feature extends the existing banking application by adding an email address field to the Account entity and exposing a REST API endpoint to retrieve an account by its email address. The change includes persistent storage of email, uniqueness enforcement, input validation, and appropriate error handling for lookup scenarios.

## Glossary

- **Account**: The core banking entity representing a customer's bank account, identified by a unique numeric ID.
- **Account_Service**: The Spring service layer responsible for account business logic.
- **Account_Controller**: The Spring REST controller that exposes HTTP endpoints for account operations.
- **Account_Repository**: The Spring Data JPA repository responsible for database access for Account entities.
- **Email**: A unique, non-null string in valid RFC 5321 email format associated with an Account.
- **Validator**: The Jakarta Bean Validation mechanism that enforces field-level constraints before persistence.

---

## Requirements

### Requirement 1: Add Email Field to Account Entity

**User Story:** As a bank administrator, I want each account to store an email address, so that customers can be identified and contacted by email.

#### Acceptance Criteria

1. THE Account_Service SHALL store an email address as part of every Account record.
2. IF an Account creation or update request contains a blank or null email field, THEN THE Validator SHALL reject the request and THE Account_Controller SHALL return an HTTP 400 Bad Request response with an error message indicating that the email field is required.
3. IF an Account creation or update request contains an email value that does not conform to valid email format (RFC 5321, maximum 320 characters total), THEN THE Validator SHALL reject the request and THE Account_Controller SHALL return an HTTP 400 Bad Request response with an error message indicating that the email format is invalid.
4. THE Account_Repository SHALL enforce uniqueness of the email field at the database level so that no two accounts share the same email address.
5. WHEN an Account creation request is submitted with a duplicate email address, THE Account_Controller SHALL return an HTTP 409 Conflict response with an error message indicating that the email address is already associated with an existing account.

---

### Requirement 2: Retrieve Account by Email

**User Story:** As a bank administrator, I want to look up an account using an email address, so that I can quickly find a customer's account without knowing their account ID.

#### Acceptance Criteria

1. WHEN a GET request is made to `/account/email/{email}` with a properly formatted, non-blank email address that matches an existing Account, THE Account_Controller SHALL return an HTTP 200 response containing the matching account's details.
2. WHEN a GET request is made to `/account/email/{email}` with a properly formatted email address that does not match any Account, THE Account_Controller SHALL return an HTTP 404 response with an error message indicating no account exists for the provided email.
3. IF the email path parameter in a GET request to `/account/email/{email}` is blank or missing, THEN THE Account_Controller SHALL return an HTTP 400 response with an error message indicating that the email parameter is required.
4. IF the email path parameter in a GET request to `/account/email/{email}` is syntactically malformed (does not conform to RFC 5321 format), THEN THE Account_Controller SHALL return an HTTP 400 response with an error message indicating that the email format is invalid.

---

### Requirement 3: Email Field in Account Responses

**User Story:** As an API consumer, I want the email address to be included in all account responses, so that I can confirm the email associated with an account.

#### Acceptance Criteria

1. THE Account_Controller SHALL include the email field in the JSON response body for all endpoints that return Account data (create, get by ID, update, get by email).
2. IF an account update request does not include an email field (null or absent), THEN THE Account_Service SHALL preserve the account's existing email value unchanged.
3. IF an account update request includes an email field with an invalid format, THEN THE Validator SHALL reject the request and THE Account_Controller SHALL return an HTTP 400 Bad Request response with an error message indicating that the email format is invalid.
4. IF an account update request includes a valid, non-null email field value, THEN THE Account_Service SHALL replace the account's existing email with the new value provided.
