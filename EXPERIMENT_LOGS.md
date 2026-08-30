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

## Experiment 4 — Advanced Autonomous Agent

### Goal

Extend the simple Gemini one-shot approach into an autonomous software engineering workflow capable of inspecting source code, proposing a fix, modifying the permitted file, running verification, and determining whether the fix succeeded.

### Method

A Java-based agent was implemented using the Gemini Java SDK.

The agent was given controlled file-system capabilities:

* `readFile()` — reads project source files
* `writeFile()` — writes the generated fix
* `runVerification()` — executes the Maven test suite

The write capability was deliberately restricted to:

```
src/main/java/com/example/nplusone/AuthorRepository.java
```

The agent inspected:

* `AuthorController.java`
* `AuthorRepository.java`
* `Author.java`

Gemini was instructed to analyze the N+1 problem, propose a concrete fix, provide the complete replacement contents of `AuthorRepository.java`, and avoid modifying unrelated files.

### Iteration 1 — LLM Analyzer

The agent successfully inspected the provided source code and identified the N+1 query problem.

It correctly identified:

```
author.getBooks().size()
```

inside the author iteration as the operation that could trigger additional lazy-loading queries.

The agent proposed using:

```
@EntityGraph(attributePaths = "books")
```

Evidence was captured in:

```
evidence/experiment4_iteration1_llm_analyzer/
```

### Iteration 2 — Read Files

The agent was extended to inspect the actual project files through the controlled `readFile()` capability rather than relying on manually supplied source code.

The agent successfully read the relevant controller, repository, and entity files and generated an analysis based on the actual project state.

Evidence was captured in:

```
evidence/experiment4_iteration2_read_files/
```

### Iteration 3 — Restricted Write

The agent was extended with a restricted `writeFile()` capability.

Gemini generated a concrete `AuthorRepository.java` implementation using:

```
@EntityGraph(attributePaths = "books")
```

The agent successfully wrote the generated contents to the permitted repository file.

The write operation was restricted so that the agent could not modify arbitrary project files.

### Iteration 4 — Autonomous Verification

The agent was extended with automated verification using:

```
.\mvnw.cmd test
```

After applying the generated repository change, the agent automatically ran the project test suite and inspected the resulting output.

The final verification returned:

```
EXIT_CODE: 0

Tests run: 1, Failures: 0, Errors: 0, Skipped: 0

BUILD SUCCESS
```

Runtime SQL evidence showed a single query using a `LEFT JOIN` between `author` and `book`:

```
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
```

The agent therefore determined that the N+1 query pattern had been eliminated and that no further investigation was required.

Evidence was captured in:

```
evidence/experiment4_iteration4_verification/
```

### Agent Workflow

The completed workflow is:

```
Read Files
    ↓
Analyze Problem
    ↓
Propose Fix
    ↓
Restricted Write
    ↓
Run Verification
    ↓
Inspect SQL / Test Results
    ↓
Determine Success
```

### Result

Experiment 4 successfully demonstrated a controlled autonomous software engineering loop.

Unlike the Experiment 2 one-shot Gemini solution, the advanced agent could interact with the project through controlled tools, modify an explicitly permitted file, execute automated verification, inspect runtime SQL evidence, and determine whether the generated change succeeded.

The final implementation successfully reduced the observed N+1 behavior from:

```
1 + 5 = 6 queries
```

to:

```
1 query
```

while maintaining:

```
BUILD SUCCESS
```

and:

```
0 test failures
```

### Evidence

Experiment 4 evidence is organized under:

```
evidence/experiment4_iteration1_llm_analyzer/
evidence/experiment4_iteration2_read_files/
evidence/experiment4_iteration3_write_file/
evidence/experiment4_iteration4_verification/
```

The complete agent trajectory is recorded in:

```
trajectories/experiment-4-advanced-agent.md
```
