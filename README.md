# N+1 Query Baseline

This is the initial investigation project for the micro1 Frontier Engineering Challenge 2026. It intentionally reproduces an N+1 query pattern in a small Spring Boot application. It does not contain an AI solution or an N+1 fix.

## Requirements

- JDK 25
- No globally installed Maven is required; use the Maven Wrapper

## Run the tests

```powershell
.\mvnw.cmd test
```

## Run the application

```powershell
.\mvnw.cmd spring-boot:run
```

In another PowerShell window, call the endpoint:

```powershell
Invoke-RestMethod http://localhost:8080/authors
```

The response contains 5 authors, each with a `bookCount` of 3. Hibernate SQL is logged in the application window. Look for one query selecting all authors followed by five separate queries selecting books by `author_id`, for example:

```text
select ... from author
select ... from book ... where ... author_id=?
select ... from book ... where ... author_id=?
select ... from book ... where ... author_id=?
select ... from book ... where ... author_id=?
select ... from book ... where ... author_id=?
```

The `Author.books` collection uses the default lazy fetch behavior for `@OneToMany`. The controller then calls `author.getBooks().size()` once for every author, which initializes each collection separately and intentionally creates the N+1 pattern.