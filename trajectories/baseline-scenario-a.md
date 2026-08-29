# Baseline Trajectory — Gemini

## Tool
Gemini Pro

## Purpose
Establish the simple one-shot baseline for identifying and fixing
the N+1 query problem.

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

The issue stems from the interaction between how the `Author` entity maps its `books` collection and how the controller accesses that data. By default, JPA `@OneToMany` relationships are lazily loaded, meaning the associated `books` are not retrieved from the database until they are explicitly accessed.

When the application retrieves all authors, it executes a single SQL query (`1`). Then, it iterates through each author to get the count of their books. Because the collection is lazy-loaded, Hibernate issues an additional SQL query to fetch the books for *each* author (`N`). If there are 5 authors, the application executes 1 + 5 = 6 queries.

### 2. Exact Code Responsible

The problem is triggered in `AuthorController.java` within the `getAuthors()` method:

* **The "1" Query:** `authorRepository.findAll()` fetches all authors in one query.


* **The "N" Queries:** Inside the stream map operation, calling `author.getBooks().size()` forces the initialization of the lazy collection for each individual author, triggering a new query each time.



### 3. Proposed Spring Data JPA Fix

The most idiomatic Spring Data JPA solution is to use `@EntityGraph` to define an eager fetch plan for this specific operation. By specifying `attributePaths = "books"`, we instruct Hibernate to fetch the authors and their associated books in a single SQL query using a `LEFT OUTER JOIN`.

### 4. Required Code Changes

**Modify `AuthorRepository.java` (Source 3):**
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

**Modify `AuthorController.java` (Source 2):**
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
        // Change findAll() to findAllWithBooks()
        return authorRepository.findAllWithBooks().stream()
                .map(author -> new AuthorResponse(author.getId(), author.getName(), author.getBooks().size()))
                .toList();
    }

    public record AuthorResponse(Long id, String name, int bookCount) {
    }
}

```

### 5. Why This Reduces SQL Queries

The `@EntityGraph` instructs the JPA provider (Hibernate) to ignore the default lazy fetch strategy for the `books` collection just for this specific method call. Hibernate will generate a single SQL statement with a `LEFT OUTER JOIN` between the `author` and `book` tables. This retrieves all authors and their corresponding books in exactly 1 query, eliminating the N subsequent queries entirely.

### 6. Trade-offs and Considerations

* **Memory Consumption:** Joining a collection duplicates the root entity's data in the JDBC result set for every child row. If an author has 1000 books, the author's information is returned 1000 times in the raw SQL result set before Hibernate deduplicates it in memory. This can consume excess memory if collections are massive.
* **Multiple Collections:** You should generally avoid fetching multiple `List` collections simultaneously with joins, as it results in a Cartesian product (e.g., an author with 10 books and 10 awards would yield a result set of 100 rows per author).
* **Targeted Fetching:** This is why it is best practice to create a dedicated method (like `findAllWithBooks()`) rather than overriding the default `findAll()`. It ensures you only pay the performance cost of fetching books when you actually need them.

Do you have any questions about how `@EntityGraph` works under the hood compared to standard JPQL `JOIN FETCH`?

## Human Observation

Gemini correctly identified the N+1 query in the existing Spring Boot
application and proposed an @EntityGraph-based fix.

The response was a one-shot recommendation. It did not independently
measure the runtime SQL queries, apply the change, run the application,
or verify the resulting query count.

## Baseline Limitation

The baseline provides a proposed solution but does not close the
observe → modify → verify loop.