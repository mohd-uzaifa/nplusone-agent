# Copilot Trajectory — Baseline Construction and Fix Application

## Purpose

GitHub Copilot was used as the coding agent during the initial construction and execution of the baseline experiments.

Copilot was responsible for generating and modifying the project code based on the experiment requirements, while Gemini Pro was used as the reasoning model for the dedicated one-shot AI baseline in Experiment 2.

This trajectory documents the Copilot-assisted implementation work so that the use of coding agents is transparent and reproducible.

---

# Experiment 1 — Baseline N+1 Reproduction

## Objective

Create a small Spring Boot application that intentionally reproduces an N+1 query problem involving `Author` and `Book` entities.

## Copilot Role

GitHub Copilot was used to construct the initial project and implement the baseline scenario.

The requested implementation included:

* Spring Boot application
* Spring Data JPA
* H2 database
* `Author` entity
* `Book` entity
* lazy `@OneToMany` relationship
* five authors with three books each
* `GET /authors` endpoint
* Hibernate SQL logging
* Maven-based tests

## Copilot Actions

Copilot explored the project directory and generated the initial application structure.

The implementation created the required files and configured the application for the N+1 experiment.

Key files included:

```text
pom.xml
Author.java
Book.java
AuthorController.java
AuthorRepository.java
SampleDataConfig.java
NPlusOneApplication.java
NPlusOneApplicationTests.java
application.properties
```

## Baseline Implementation

The application intentionally used the normal repository operation:

```java
authorRepository.findAll()
```

The controller then accessed the books collection:

```java
author.getBooks().size()
```

This created the intended lazy-loading pattern:

```text
Load authors
     ↓
authorRepository.findAll()
     ↓
author.getBooks().size()
     ↓
Separate book query for each author
```

## Copilot Verification

Copilot ran the Maven test suite:

```text
mvnw.cmd test
```

The tests passed successfully.

Copilot also executed the application and verified the `/authors` endpoint.

The endpoint returned five authors, each with a book count of three.

Hibernate SQL logging showed:

```text
1 author query
+
5 book queries
=
6 SQL queries
```

The five book queries used the individual author IDs.

## Result

The intended N+1 problem was successfully reproduced.

The measured baseline was:

```text
5 authors
1 + 5 queries
6 total SQL queries
```

This implementation was then preserved as the controlled N+1 baseline.

---

# Experiment 2 — Gemini One-Shot Fix

## Objective

Establish a simple one-shot AI baseline for identifying and fixing the N+1 query problem.

## Separation of Responsibilities

This experiment used two AI tools with different roles:

```text
GitHub Copilot
    ↓
Applied and verified the generated code changes

Gemini Pro
    ↓
Performed the one-shot analysis and proposed the fix
```

Gemini was the reasoning component for the one-shot baseline.

Copilot acted as the coding agent that applied the proposed changes to the repository.

## Gemini Input

Gemini was provided with the relevant Spring Boot source code and was instructed to:

1. determine whether an N+1 problem existed,
2. explain why it occurred,
3. identify the responsible code,
4. propose one concrete Spring Data JPA fix,
5. provide the required code changes,
6. explain the expected query reduction,
7. discuss important trade-offs.

Gemini was explicitly instructed not to claim runtime verification.

## Gemini Proposed Fix

Gemini proposed an `@EntityGraph`-based solution.

The repository was changed to use:

```java
@EntityGraph(attributePaths = "books")
```

with a dedicated repository method:

```java
findAllWithBooks()
```

The controller was then changed to call:

```java
authorRepository.findAllWithBooks()
```

instead of:

```java
authorRepository.findAll()
```

## Copilot Actions

GitHub Copilot explored the existing project structure and identified the two files requiring modification:

```text
AuthorRepository.java
AuthorController.java
```

Copilot then applied the Gemini-proposed changes.

The repository was updated with the `@EntityGraph` fetch strategy and the controller was updated to use the new repository method.

## Copilot Verification

After applying the changes, Copilot ran:

```text
mvnw.cmd test
```

The test suite completed successfully:

```text
BUILD SUCCESS

Tests run: 1
Failures: 0
Errors: 0
Skipped: 0
```

Copilot then reviewed the resulting SQL behavior.

Hibernate generated a single query using a `LEFT JOIN`:

```sql
SELECT
    a1_0.id,
    b1_0.author_id,
    b1_0.id,
    b1_0.title,
    a1_0.name
FROM
    author a1_0
LEFT JOIN
    book b1_0
        ON a1_0.id = b1_0.author_id
```

## Result

The Gemini-proposed fix, applied through Copilot, reduced the observed query count from:

```text
Baseline:       6 SQL queries
Gemini fix:     1 SQL query
```

Therefore:

```text
6 → 1
```

The N+1 behavior was successfully eliminated for the Scenario A dataset.

## Evidence

The baseline and Gemini-fix evidence is stored in:

```text
evidence/baseline_nplusone_sql/
```

and:

```text
evidence/baseline_gemini_fix/
```

The Gemini reasoning itself is documented separately in:

```text
trajectories/baseline-scenario-a.md
```

---

# Agent Roles Summary

The responsibilities of the two coding/AI tools in the baseline experiments were intentionally different.

| Tool           | Role                                                                                          |
| -------------- | --------------------------------------------------------------------------------------------- |
| GitHub Copilot | Project construction, code implementation, code modification, test execution and verification |
| Gemini Pro     | One-shot static analysis and proposed N+1 fix                                                 |

The important distinction is that Gemini's one-shot solution was not presented as an autonomous agent.

The actual code modification and runtime verification were performed through the development workflow using GitHub Copilot and the local project environment.

---

# Baseline Limitation

The combined baseline workflow still did not provide a fully autonomous feedback loop.

Gemini generated its solution from source-code context but did not independently:

```text
inspect runtime
    ↓
modify files
    ↓
run verification
    ↓
inspect actual results
    ↓
retry after failure
```

Copilot was used to implement and verify the proposed solution, but the baseline was not designed as a self-correcting engineering agent.

This limitation motivated Experiment 4 — Advanced Agent.

The advanced agent therefore introduced explicit project tools for:

```text
read
  ↓
reason
  ↓
write
  ↓
verify
  ↓
inspect results
  ↓
determine success
```

with a restricted write boundary and automated verification.
