---
inclusion: always
---
# Project Structure

This is a Spring Boot banking backend application.

## Main Package Structure

The application follows a layered architecture:

- Controller: Exposes REST endpoints and handles HTTP requests and responses.
- Service: Contains business logic and transaction handling.
- Repository: Handles database access using Spring Data JPA.
- Entity: Contains JPA entity classes mapped to database tables.
- DTO: Contains request and response objects when required.
- Exception: Contains custom exceptions and centralized exception handling.

## Current Domain

The main domain entity is Account.

Account contains:
- id
- name
- balance
- createdDate

## API

The current Account REST API uses the `/account` endpoint.

The application supports:
- Create account
- Get account
- Update account
- Delete account
- Deposit
- Withdraw
- Transfer

## Important Rule

Before creating new files or packages, inspect the existing project structure and follow the existing naming and package conventions.
<!------------------------------------------------------------------------------------
   Add rules to this file or a short description and have Kiro refine them for you.
   
   Learn about inclusion modes: https://kiro.dev/docs/steering/#inclusion-modes
-------------------------------------------------------------------------------------> 