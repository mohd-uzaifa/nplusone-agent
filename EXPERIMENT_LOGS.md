# Experiment Log

## Experiment 1 — Baseline N+1 Reproduction

### Goal
Create a reproducible Spring Boot application containing an intentional
N+1 query problem.

### Environment
- Java: 25.0.3
- Spring Boot: 4.0.1
- Database: H2
- ORM: Spring Data JPA / Hibernate
- Build: Maven Wrapper

### Test
Called:

    GET /authors

The application contains 5 authors, each with 3 books.

### Observed behavior
Hibernate generated:

- 1 query to load all authors
- 5 separate queries to load books
- Total: 6 SQL queries

The five book queries were executed with author IDs:

    1, 2, 3, 4, 5

Therefore:

    1 + N
    1 + 5 = 6 queries

### Evidence
Evidence screenshot:

    evidence/baseline-nplusone-sql

### Result
The N+1 behavior was successfully reproduced and verified.

### Decision
Freeze this implementation as the baseline.

The next experiment will investigate an AI agent capable of
detecting this N+1 problem.