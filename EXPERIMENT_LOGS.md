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

---

---

## Experiment 4 — Advanced Agent

### Objective

Build an advanced software engineering agent that can inspect the Spring Boot N+1 test case, reason about the problem, apply a controlled code change, and verify the result using actual build and runtime evidence.

The goal was to improve on the Gemini one-shot baseline by introducing tool use and an autonomous engineering workflow.

---

### Iteration 1 — LLM Analyzer

#### Goal

Test whether Gemini could correctly reason about the N+1 query problem when given the relevant source-code context.

#### Agent Capability

The agent was able to:

* receive source-code context,
* analyze the code,
* identify the N+1 query problem,
* propose a Spring Data JPA fix,
* identify what should be verified afterward.

#### Input

The agent was given:

* `Author.java`
* `AuthorController.java`
* `AuthorRepository.java`

The original baseline implementation contained:

```text
authorRepository.findAll()
        ↓
author.getBooks().size()
        ↓
1 author query + N book queries
```

#### Result

Gemini correctly identified the N+1 query problem.

It identified `author.getBooks().size()` as the operation that triggers lazy loading for each author and correctly predicted the `1 + N` query pattern.

It proposed an `@EntityGraph`-based solution and identified SQL query count and functional correctness as verification requirements.

#### Evidence

```text
evidence/experiment4_iteration1_llm_analyzer/
    gemini-analysis1.png
    gemini-analysis2.png
```

#### Limitation

This iteration was primarily an LLM analyzer.

The agent did not:

* inspect the filesystem independently,
* modify source files,
* run the application,
* execute tests as part of its workflow,
* measure the actual SQL query count,
* verify whether its proposed fix worked.

Therefore, the observe → modify → verify loop was not yet complete.

#### Decision

Keep Gemini as the reasoning component and introduce controlled filesystem access in the next iteration.

---

### Iteration 2 — Real File Inspection

#### Goal

Replace hard-coded source context with source code retrieved directly from the actual project files.

#### Change

Added `FileTools.readFile()`.

The agent now reads:

* `AuthorController.java`
* `AuthorRepository.java`
* `Author.java`

directly from the project and provides the retrieved content to Gemini.

#### Agent Workflow

```text
Actual project files
        ↓
FileTools.readFile()
        ↓
Source content
        ↓
Gemini
        ↓
N+1 analysis
```

#### Result

Gemini again correctly identified the N+1 query problem.

It correctly identified the relevant code path:

```text
authorRepository.findAll()
        +
author.getBooks().size()
```

and explained the expected `1 + N` query behavior.

It proposed an `@EntityGraph` solution and identified runtime SQL query count and functional correctness as verification requirements.

#### Observation

Gemini also suggested an optional DTO aggregation query as an alternative, even though the agent was intended to produce one concrete fix.

This did not affect the correctness of the diagnosis but showed that stronger output constraints would be useful for future iterations.

#### Evidence

```text
evidence/experiment4_iteration2_read_files/
    agent-analysis1.png
    agent-analysis2.png
    agent-analysis3.png
```

#### Verification

The project compiled successfully using:

```text
.\mvnw.cmd compile
```

The agent also successfully executed against the real project files and returned the expected N+1 analysis.

#### Limitation

The agent could inspect real source files but still could not:

* modify source files,
* run verification as an agent action,
* inspect runtime SQL,
* determine whether its proposed fix actually worked.

#### Decision

Introduce a controlled `writeFile()` capability.

---

### Iteration 3 — Controlled File Modification

#### Goal

Allow the agent to apply its proposed fix through a restricted filesystem operation.

#### Change

Added `FileTools.writeFile()` with a restricted write boundary.

The agent is permitted to modify only:

```text
src/main/java/com/example/nplusone/AuthorRepository.java
```

This prevents the agent from arbitrarily modifying unrelated project files.

#### Agent Workflow

```text
Read source files
        ↓
Gemini reasoning
        ↓
Proposed @EntityGraph fix
        ↓
Restricted FileTools.writeFile()
        ↓
AuthorRepository.java modified
        ↓
Compile
        ↓
Runtime request
        ↓
Hibernate SQL verification
```

#### Result

Gemini generated a complete replacement for `AuthorRepository.java` using:

```java
@EntityGraph(attributePaths = "books")
```

The agent successfully passed the generated content to `FileTools.writeFile()`.

The tool returned:

```text
SUCCESS: File written: src/main/java/com/example/nplusone/AuthorRepository.java
```

#### Compilation

The modified project compiled successfully using:

```text
.\mvnw.cmd compile
```

#### Runtime Verification

The `/authors` endpoint was executed with the agent-generated fix.

Hibernate produced a single SQL query using a `LEFT JOIN` between the `author` and `book` tables.

Observed query count:

```text
1 SQL query
```

The original baseline required:

```text
1 author query + 5 book queries = 6 queries
```

The agent-generated fix therefore reduced the observed query count:

```text
6 → 1
```

#### Evidence

```text
evidence/experiment4_iteration3_write_file/
    agent-write-proposal.png
    agent-runtime-sql.png
```

#### Decision

The tool-using agent successfully completed:

```text
read → reason → write → compile → runtime verification
```

The next iteration moved verification into the agent itself.

---

### Iteration 4 — Autonomous Verification

#### Goal

Extend the agent so that verification is performed automatically rather than manually after the agent modifies the source.

#### Change

Added `FileTools.runVerification()`.

The tool runs:

```text
.\mvnw.cmd test
```

The verification output is summarized so that the agent can inspect:

* process exit code,
* relevant SQL evidence,
* test result,
* build status.

#### Final Agent Workflow

```text
Read
  ↓
Analyze
  ↓
Propose Fix
  ↓
Restricted Write
  ↓
Run Verification
  ↓
Inspect SQL/Test Results
  ↓
Determine Success
```

#### Result

At the beginning of this iteration, `AuthorRepository.java` already contained the fix introduced during Iteration 3:

```java
@EntityGraph(attributePaths = "books")
```

The agent correctly recognized that the N+1 problem was already resolved and did not introduce an unnecessary second modification.

It then automatically ran the verification process.

#### Verification Evidence

The automated verification returned:

```text
EXIT_CODE: 0

SQL EVIDENCE:

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

TEST RESULT:

Tests run: 1, Failures: 0, Errors: 0, Skipped: 0

BUILD SUCCESS
```

The SQL evidence shows that authors and their books were retrieved through a single `LEFT JOIN` query rather than separate queries for each author.

#### Evidence

```text
evidence/experiment4_iteration4_verification/
    terminal-verification1.png
    terminal-verification2.png
```

#### Final Verification

The agent determined:

```text
Verification: Successful
Build: Successful
Tests: 1 passed, 0 failures, 0 errors
SQL evidence: Single LEFT JOIN query
Further investigation: Not required
```

#### Experiment 4 Outcome

Experiment 4 successfully transformed the initial LLM-only workflow into a controlled agentic engineering workflow.

The final agent can:

1. inspect real project files,
2. reason about the N+1 problem,
3. generate a concrete fix,
4. modify a restricted source file,
5. run automated Maven verification,
6. inspect runtime SQL evidence,
7. inspect test/build results,
8. determine whether the change succeeded.

The progression was:

```text
LLM Analysis
     ↓
Real File Inspection
     ↓
Controlled Modification
     ↓
Automated Verification
```

The final demonstrated result was:

```text
Baseline: 6 SQL queries
Agent-fixed implementation: 1 SQL query

Query reduction: 6 → 1
```

#### Decision

Keep the final Experiment 4 implementation as the advanced agent.

The restricted write boundary and automated verification provide the main improvement over the Gemini one-shot baseline.

A future iteration could add explicit retry behavior when verification fails, but this was not required to demonstrate the completed autonomous verification workflow.
