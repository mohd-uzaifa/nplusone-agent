# N+1 Query Baseline

This is the initial investigation and baseline project for the
micro1 Frontier Engineering Challenge 2026.

The project intentionally reproduces an **N+1 query problem** in a small
Spring Boot application. It establishes a measurable baseline that will
later be used to evaluate an advanced agent-based solution.

This baseline does **not** contain an AI agent or an N+1 query fix.

---

## Problem

When an application loads a collection of related entities lazily and then
accesses that collection separately for every parent entity, Hibernate can
execute one additional SQL query for each parent.

For example, if the application loads 5 authors and then loads the books for
each author separately, the database receives:

```text
1 query to load authors
+
5 queries to load books
=
6 total queries