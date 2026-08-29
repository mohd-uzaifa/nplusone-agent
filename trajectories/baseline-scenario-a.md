# Baseline Trajectory — Gemini

## Experiment

**Scenario:** A — Author/Book N+1 Query

**Tool:** Gemini Pro

**Approach:** One-shot baseline

## Tool Purpose

Establish the simple one-shot baseline for identifying and fixing the
N+1 query problem.

The baseline receives the existing application source code and provides
one proposed fix based only on static inspection. It does not perform an
observe → modify → verify → retry loop.

## Prompt

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

## Gemini Response

Yes, the provided application contains a classic N+1 query problem.

### 1. Why the N+1 Problem Occurs

The issue stems from the interaction between how the `Author` entity maps its
`books` collection and how the controller accesses that data. By default, JPA
`@OneToMany` relationships are lazily loaded, meaning the associated `books`
are not retrieved from the database until they are explicitly accessed.

When the application retrieves all authors, it executes a single SQL query (`1`).
Then, it iterates through each author to get the count of their books. Because
the collection is lazy-loaded, Hibernate issues an additional SQL query to fetch
the books for each author (`N`). If there are 5 authors, the application executes
1 + 5 = 6 queries.

### 2. Exact Code Responsible

The problem is triggered in `AuthorController.java` within the `getAuthors()`
method:

- **The "1" Query:** `authorRepository.findAll()` fetches all authors in one query.
- **The "N" Queries:** Inside the stream map operation, calling
  `author.getBooks().size()` forces the initialization of the lazy collection for
  each individual author, triggering a new query each time.

### 3. Proposed Spring Data JPA Fix

The most idiomatic Spring Data JPA solution is to use `@EntityGraph` to define
an eager fetch plan for this specific operation. By specifying
`attributePaths = "books"`, we instruct Hibernate to fetch the authors and their
associated books in a single SQL query using a `LEFT OUTER JOIN`.

### 4. Required Code Changes

**Modify `AuthorRepository.java`:**

Add a new method that uses `@EntityGraph` to eagerly fetch the books.

```java
package com.example.nplusone;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    @EntityGraph(attributePaths = "books")
    @Query("SELECT a FROM Author a")
    List<Author> findAllWithBooks();
}
```

**Modify `AuthorController.java`:**

Update the controller to call the new method instead of `findAll()`.

```java
package com.example.nplusone;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthorController {

    private final AuthorRepository authorRepository;

    public AuthorController(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @GetMapping("/authors")
    @Transactional(readOnly = true)
    public List<AuthorResponse> getAuthors() {
        return authorRepository.findAllWithBooks().stream()
                .map(author -> new AuthorResponse(
                        author.getId(),
                        author.getName(),
                        author.getBooks().size()))
                .toList();
    }

    public record AuthorResponse(Long id, String name, int bookCount) {
    }
}
```

### 5. Why This Reduces SQL Queries

The `@EntityGraph` instructs the JPA provider (Hibernate) to ignore the
default lazy fetch strategy for the `books` collection just for this specific
method call. Hibernate will generate a single SQL statement with a
`LEFT OUTER JOIN` between the `author` and `book` tables. This retrieves all
authors and their corresponding books in one query, eliminating the N
subsequent queries.

### 6. Trade-offs and Considerations

- **Memory Consumption:** Joining a collection duplicates the root entity's
  data in the JDBC result set for every child row. If an author has 1000 books,
  the author's information is returned 1000 times in the raw SQL result set
  before Hibernate deduplicates it in memory. This can consume excess memory
  if collections are massive.

- **Multiple Collections:** You should generally avoid fetching multiple
  `List` collections simultaneously with joins, as it can result in a Cartesian
  product.

- **Targeted Fetching:** A dedicated method such as `findAllWithBooks()` ensures
  that the additional data is fetched only when it is actually needed.

## Human Observation

Gemini correctly identified the N+1 query in the existing Spring Boot
application and proposed an `@EntityGraph`-based fix.

The proposed fix was then applied to the application and verified separately
using the existing Maven Wrapper and running application.

The original implementation produced:

```text
1 author query + 5 book queries = 6 SQL queries
```

After applying Gemini's proposed fix, the `/authors` endpoint produced a
single SQL query using a `LEFT JOIN` between `author` and `book`.

The observed result was therefore:

```text
Original N+1 implementation: 6 SQL queries
Gemini one-shot fix:          1 SQL query
```

This represents a reduction of 5 SQL queries, or approximately 83.3% for
this specific scenario.

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

This confirms that the proposed `@EntityGraph` approach resulted in a single
joined query for the author/book data in Scenario A.

## Evidence

### Original N+1 Evidence

The original implementation is documented in:

```text
evidence/baseline_nplusone_sql/
├── author-book-queries.png
├── author-id-bindings1.png
└── author-id-bindings2.png
```

These screenshots show the author query followed by the separate book queries
and their bound author IDs.

### Gemini Fix Evidence

The result of the one-shot Gemini fix is documented in:

```text
evidence/baseline_gemini_fix/
├── baseline-gemini-response.png
└── baseline-gemini-sql.png
```

`baseline-gemini-sql.png` contains the Hibernate SQL showing the `LEFT JOIN`
between `author` and `book`.

## Baseline Limitation

Although the one-shot Gemini baseline successfully identified and fixed the
N+1 problem in Scenario A, it does not close an autonomous feedback loop.

Gemini was given the source code and produced a proposed solution, but it did
not independently:

1. inspect runtime SQL,
2. measure the original query count,
3. apply the change,
4. run the application,
5. verify the resulting query count, or
6. retry if the proposed solution failed.

Therefore, the one-shot baseline demonstrates that an LLM can provide a
reasonable static fix, but it does not demonstrate autonomous diagnosis and
verification.

This limitation motivates the advanced agent that will be developed in the
next phase.

## Baseline Conclusion

For Scenario A, the simple one-shot Gemini baseline reduced the observed
query count from:

```text
6 queries → 1 query
```

The result establishes a measurable baseline for comparison with the advanced
agent.

The advanced agent will be evaluated not only on whether it can produce a fix,
but also on whether it can **observe, diagnose, modify, verify, and retry**
using actual feedback from the application.
