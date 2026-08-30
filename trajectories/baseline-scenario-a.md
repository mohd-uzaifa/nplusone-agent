# Baseline Trajectory — Gemini One-Shot Fix

## Experiment

**Scenario:** A — Author/Book N+1 Query

**Tool:** Gemini Pro

**Approach:** One-shot baseline

## Tool Purpose

Establish the simple one-shot AI baseline for identifying and fixing the N+1 query problem.

The baseline receives the existing application source code and provides one proposed fix based on static inspection. It does not perform an autonomous observe → modify → verify → retry loop.

## Agent Context

The baseline application contains an intentional N+1 query problem involving `Author` and `Book` entities.

The application loads all authors and then accesses each author's books.

The relevant execution path is:

```text
authorRepository.findAll()

        ↓

author.getBooks().size()

        ↓

1 author query + N book queries
```

With 5 authors, the expected baseline behavior is:

```text
1 + 5 = 6 SQL queries
```

## Prompt

The following prompt was used with Gemini Pro:

```text
You are reviewing a Spring Boot application for a database performance problem.

The application has Author and Book entities. The application loads all authors and then accesses each author's books.

Inspect the provided Java source files and determine whether the application contains an N+1 query problem.

If an N+1 problem exists:

1. Explain exactly why it occurs.
2. Identify the exact code responsible.
3. Propose ONE concrete Spring Data JPA fix.
4. Show the exact code changes required.
5. Explain why your proposed change should reduce the number of SQL queries.
6. Explain any important trade-offs or considerations.

Do not redesign the application.

Do not introduce an AI agent.

Do not add unnecessary dependencies.

Do not modify unrelated files.

Do not claim that the fix has been verified because you cannot run the application.

This is a one-shot baseline experiment. Give your best solution based only on the provided source code.
```

## Gemini Analysis

Gemini correctly identified the application as containing a classic N+1 query problem.

### Why the N+1 Problem Occurs

The `Author` entity contains a lazy-loaded `books` collection.

When the application executes:

```java
authorRepository.findAll()
```

Hibernate first loads all authors with one SQL query.

The controller then accesses:

```java
author.getBooks().size()
```

for each author.

Because the `books` collection is lazy-loaded, Hibernate performs an additional query when the collection for each author is accessed.

For 5 authors, the resulting pattern is:

```text
1 author query
+
5 book queries
=
6 SQL queries
```

Therefore, the application exhibits the expected `1 + N` query pattern.

## Exact Code Responsible

The N+1 behavior is caused by the combination of:

```java
authorRepository.findAll()
```

and:

```java
author.getBooks().size()
```

The first operation loads the authors.

The second operation forces the lazy `books` collection to be initialized separately for each author.

## Proposed Fix

Gemini proposed using Spring Data JPA's `@EntityGraph` to fetch the books together with the authors.

The proposed repository change was:

```java
package com.example.nplusone;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    @EntityGraph(attributePaths = "books")
    @Query("SELECT a FROM Author a")
    List<Author> findAllWithBooks();

}
```

The controller was changed to call:

```java
authorRepository.findAllWithBooks()
```

instead of:

```java
authorRepository.findAll()
```

## Why the Fix Works

The `@EntityGraph(attributePaths = "books")` annotation instructs Hibernate to fetch the `books` association as part of the repository operation.

In the observed execution, Hibernate generated a single SQL query containing a `LEFT JOIN` between the `author` and `book` tables.

This avoids the separate book-loading query for each author.

The observed query count changed from:

```text
Baseline:       6 SQL queries
Gemini fix:     1 SQL query
```

This represents a reduction of:

```text
6 → 1
```

or approximately **83.3% fewer SQL queries** for this specific five-author scenario.

## Trade-offs Identified by Gemini

Gemini identified several considerations:

* Joining a collection can increase the size of the database result set because author data may be repeated for multiple book rows.
* Fetching multiple collections through joins can create large result sets or Cartesian-product effects.
* A dedicated fetch method such as `findAllWithBooks()` keeps the additional data fetching targeted to operations that actually need the books.

## Human Verification

The Gemini response itself did not claim runtime verification.

The proposed changes were subsequently applied to the application and verified separately using the Maven Wrapper and the running Spring Boot application.

The original implementation produced:

```text
1 author query + 5 book queries = 6 SQL queries
```

After applying the Gemini-proposed fix, the `/authors` endpoint produced a single SQL query using a `LEFT JOIN`.

The observed result was:

```text
Original N+1 implementation: 6 SQL queries
Gemini one-shot fix:          1 SQL query
```

## Verification

The application tests passed after applying the proposed fix:

```text
Tests run: 1
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

The runtime SQL generated for the `/authors` request included:

```sql
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
        on a1_0.id = b1_0.author_id
```

This confirms that the applied `@EntityGraph` approach resulted in a single joined query for the author/book data in Scenario A.

## Evidence

### Original N+1 Evidence

```text
evidence/baseline_nplusone_sql/

├── author-book-queries.png
├── author-id-bindings1.png
└── author-id-bindings2.png
```

These screenshots document the original author query followed by the separate book queries and their bound author IDs.

### Gemini Fix Evidence

```text
evidence/baseline_gemini_fix/

├── baseline-gemini-response.png
└── baseline-gemini-sql.png
```

`baseline-gemini-response.png` captures the Gemini one-shot analysis and proposed solution.

`baseline-gemini-sql.png` captures the resulting Hibernate SQL showing the `LEFT JOIN` between `author` and `book`.

## Baseline Limitation

Although the Gemini one-shot baseline successfully identified and fixed the N+1 problem, it did not close an autonomous engineering feedback loop.

Gemini was given the source code and produced a proposed solution, but it did not independently:

1. inspect runtime SQL,
2. measure the original query count,
3. modify the project,
4. run the application,
5. verify the resulting query count, or
6. retry when verification could fail.

The implementation and runtime verification were performed separately.

Therefore, this experiment establishes a **one-shot AI solution baseline**, rather than a fully autonomous software engineering agent.

This limitation motivated the development of Experiment 4 — Advanced Agent.

## Baseline Conclusion

For Scenario A, the one-shot Gemini solution successfully reduced the observed query count from:

```text
6 queries → 1 query
```

The experiment establishes a measurable baseline for comparison with the advanced agent.

The advanced agent is evaluated not only on whether it can produce a correct fix, but also on whether it can:

```text
observe
  ↓
diagnose
  ↓
modify
  ↓
verify
  ↓
determine success
```

using actual project and runtime feedback.
