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
Evidence screenshots:

    evidence/baseline-nplusone-sql/

### Result
The N+1 behavior was successfully reproduced and verified.

### Decision
Freeze this implementation as the controlled test case.

---

## Experiment 2 — Gemini One-Shot Fix

### Goal
Establish a simple one-shot AI baseline for identifying and fixing the
N+1 query problem.

### Method
Gemini Pro was given the Scenario A Java source code and a single prompt
asking it to:

- identify whether an N+1 problem exists
- explain why it occurs
- identify the responsible code
- propose one concrete Spring Data JPA fix
- provide the required code changes
- explain the expected query reduction
- discuss relevant trade-offs

The prompt explicitly instructed Gemini not to redesign the application,
introduce an AI agent, add unnecessary dependencies, modify unrelated
files, or claim runtime verification.

### Proposed solution
Gemini correctly identified the N+1 problem and proposed using:

    @EntityGraph(attributePaths = "books")

with a dedicated repository method:

    findAllWithBooks()

The controller was changed from:

    authorRepository.findAll()

to:

    authorRepository.findAllWithBooks()

### Verification
The proposed fix was implemented in the application.

Tests were run using:

    .\mvnw.cmd test

The test suite passed successfully.

The `/authors` endpoint was then executed and Hibernate SQL logging
was inspected.

### Observed behavior
The N+1 pattern was replaced by a single SQL query using a LEFT JOIN:

    select
        a1_0.id,
        b1_0.author_id,
        b1_0.id,
        b1_0.title,
        a1_0.name
    from
        author a1_0
    left join
        book b1_0
            on a1_0.id=b1_0.author_id

The original baseline required:

    1 author query + 5 book queries = 6 queries

The Gemini fix reduced this to:

    1 query

### Evidence
Evidence screenshots:

    evidence/baseline-gemini-fix/

The complete Gemini prompt and response are recorded in:

    trajectories/baseline-scenario-a.md

### Result
The Gemini one-shot solution successfully identified and fixed the
N+1 problem, reducing the observed SQL query count from 6 to 1.

### Limitation
Gemini provided a one-shot solution and did not independently perform
an observe → modify → verify → retry loop.

This establishes the simple baseline solution against which the advanced
agent will later be compared.

---

## Experiment 3 — Restore N+1 Baseline

### Goal
Restore Scenario A to its original N+1 state after evaluating the
Gemini one-shot fix, while preserving the previous experiment history
and evidence.

### Method
The application code was restored using Git from the original baseline
commit:

    78f89c5 — baseline: reproduce N+1 query problems

Only the two application files changed by the Gemini fix were restored:

- `AuthorRepository.java`
- `AuthorController.java`

The Gemini trajectory, documentation, and previous evidence were kept.

No AI agent was used for the restoration.

### Verification
Tests were run using:

    .\mvnw.cmd test

The test suite passed successfully.

The application was then started and the `/authors` endpoint was called
again.

### Observed behavior
The restored application again generated:

- 1 query to load all authors
- 5 separate queries to load books
- Total: 6 SQL queries

The five book queries were executed with author IDs:

    1, 2, 3, 4, 5

Therefore:

    1 + N
    1 + 5 = 6 queries

### Evidence
Fresh evidence was captured in:

    evidence/baseline-nplusone-sql-restored/

The evidence contains:

- `author-book-queries.png`
- `author-id-bindings1.png`
- `author-id-bindings2.png`

### Result
The original N+1 baseline was successfully restored and independently
verified after the Gemini experiment.

### Decision
Keep the original N+1 implementation as the controlled test case.

The Gemini one-shot fix remains documented as the simple baseline
solution, while Scenario A is returned to the broken state so that the
advanced agent can be evaluated against the same problem.