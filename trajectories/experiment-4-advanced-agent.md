# Experiment 4 — Advanced Agent Trajectory

## Objective

Build an advanced software engineering agent that can inspect the
Spring Boot N+1 test case, apply a fix, and verify the result through
runtime evidence.

The advanced agent is intended to improve on the one-shot Gemini
baseline by introducing tool use, execution, verification, and
eventually a retry loop.

---

## Iteration 1 — LLM Analyzer

### Goal

Test whether Gemini can correctly reason about the N+1 query problem
when given relevant source-code context.

### Agent Capability

- Send source-code context to Gemini
- Analyze the code
- Recommend a possible fix
- Identify what should be verified

### Input

The agent was given the relevant source from:

- `Author.java`
- `AuthorController.java`
- `AuthorRepository.java`

The original baseline implementation was preserved:

    authorRepository.findAll()
            ↓
    author.getBooks().size()
            ↓
    1 author query + N book queries

### Agent Prompt

The agent was instructed to:

1. Determine whether an N+1 problem exists.
2. Identify the exact code causing it.
3. Explain the expected query behavior.
4. Propose one possible fix.
5. Explain what should be verified afterward.
6. Not modify files or claim runtime verification.

### Result

Gemini correctly identified the N+1 query problem.

It identified `author.getBooks().size()` as the operation that
triggers lazy loading for each author.

It correctly predicted `1 + N` queries and proposed an
`@EntityGraph`-based solution.

It also correctly identified SQL query count and data correctness as
things that should be verified after applying the fix.

### Evidence

- `evidence/experiment4_iteration1_llm_analyzer/gemini-analysis-1.png`
- `evidence/experiment4_iteration1_llm_analyzer/gemini-analysis-2.png` 

### Limitation

This iteration was still primarily an LLM analyzer.

The agent:

- did not inspect the filesystem independently,
- did not modify source files,
- did not run the application,
- did not execute tests as part of its reasoning,
- did not measure the actual SQL query count,
- did not verify whether its proposed fix worked.

Therefore, this iteration did not close the
observe → modify → verify loop.

### Decision

Keep Gemini as the reasoning component, but add controlled tool use.

The next iteration introduces a `read_file` capability so the agent can
obtain source code from the actual project instead of relying on
hard-coded source text.

---

## Iteration 2 — Real File Inspection

### Goal

Replace hard-coded source context with information retrieved from the
actual project files.

### Change

Added a local `FileTools.readFile()` capability.

The agent now reads:

- `src/main/java/com/example/nplusone/AuthorController.java`
- `src/main/java/com/example/nplusone/AuthorRepository.java`
- `src/main/java/com/example/nplusone/Author.java`

and provides the retrieved contents to Gemini.

### Agent Workflow

    Actual project files
            ↓
    FileTools.readFile()
            ↓
    Source content
            ↓
    Gemini
            ↓
    N+1 analysis

### Result

Gemini again correctly identified the N+1 query problem.

It identified `authorRepository.findAll()` combined with
`author.getBooks().size()` as the relevant code path and correctly
explained the expected `1 + N` query behavior.

It proposed an `@EntityGraph` fix and described runtime SQL query count
and functional correctness as verification requirements.

### Observation

Gemini also supplied an optional DTO aggregation query as a second
alternative, even though the prompt requested one concrete fix.

This did not invalidate the diagnosis, but it demonstrates that the
agent still needs stronger output constraints as the workflow becomes
more autonomous.

### Evidence

- `evidence/experiment4_iteration2_read_files/agent-analysis-1.png`
- `evidence/experiment4_iteration2_read_files/agent-analysis-2.png`
- `evidence/experiment4_iteration2_read_files/agent-analysis-3.png`


### Verification Status

The project compiled successfully with:

    .\mvnw.cmd compile

The agent also executed successfully against the real project files and
returned a correct N+1 analysis.

### Limitation

The agent can now obtain real source code, but it still cannot:

- modify source files,
- run tests as an agent action,
- run the application for verification,
- inspect actual SQL output,
- determine whether a proposed fix really works.

### Decision

The next iteration will introduce a controlled `write_file` capability
so the agent can apply a proposed code change.

---

## Iteration 3 — Controlled File Modification

### Goal

Allow the agent to apply its proposed fix through a controlled
filesystem operation.

### Change

Added `FileTools.writeFile()` with a restricted write boundary.

The tool permits writing only to:

`src/main/java/com/example/nplusone/AuthorRepository.java`

This prevents the agent from arbitrarily modifying unrelated project
files.

### Agent Workflow

    Actual project files
            ↓
    FileTools.readFile()
            ↓
    Gemini reasoning
            ↓
    Proposed @EntityGraph fix
            ↓
    FileTools.writeFile()
            ↓
    AuthorRepository.java modified
            ↓
    Maven compile
            ↓
    Runtime request
            ↓
    Hibernate SQL verification

### Result

Gemini generated a complete replacement for `AuthorRepository.java`
using:

`@EntityGraph(attributePaths = "books")`

The agent successfully passed the generated content to
`FileTools.writeFile()`.

The tool returned:

`SUCCESS: File written: src/main/java/com/example/nplusone/AuthorRepository.java`

### Compilation

The modified project compiled successfully with:

    .\mvnw.cmd compile

### Runtime Verification

The `/authors` endpoint was executed with the agent-generated fix.

Hibernate produced a single SQL statement using a `LEFT JOIN` between
the `author` and `book` tables.

Observed query count:

**1 SQL query**

The original restored baseline produces 6 queries, so the agent
successfully eliminated the observed N+1 behavior.

### Evidence

- Agent proposal and write result:
  `evidence/experiment4_iteration3_write_file/agent-write-proposal.png`
- Runtime SQL:
  `evidence/experiment4_iteration3_write_file/agent-runtime-sql.png`

### Decision

The basic tool-using agent successfully completed:

**read → reason → write → compile → runtime verification**

The next iteration should move verification into the agent itself.

---

## Iteration 4 — Autonomous Verification

### Objective

Extend the agent with controlled execution and verification capabilities so that it can perform a more complete autonomous engineering loop.

### Agent Workflow

The agent was extended to:

1. inspect the relevant source files,
2. analyze the N+1 query problem,
3. generate a concrete Spring Data JPA fix,
4. write the proposed fix,
5. run automated Maven verification,
6. inspect the resulting SQL evidence,
7. inspect the test result and build status,
8. determine whether the fix succeeded.

### Tools Added

#### `FileTools.readFile()`

Allows the agent to inspect project source files.

#### `FileTools.writeFile()`

Provides a restricted write capability.

The agent is allowed to modify only:

```text
src/main/java/com/example/nplusone/AuthorRepository.java
```

This prevents the LLM from arbitrarily modifying other project files.

#### `FileTools.runVerification()`

Runs:

```text
.\mvnw.cmd test
```

The verification output is summarized to expose:

* exit code,
* relevant SQL evidence,
* test result,
* build status.

### Agent Result

The agent analyzed the project and identified that `AuthorRepository` already contained:

```java
@EntityGraph(attributePaths = "books")
```

It therefore determined that the N+1 problem had already been resolved by the previous iteration and did not require another code change.

### Verification Evidence

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

The SQL evidence demonstrates that authors and books were retrieved through a single `LEFT JOIN` query rather than separate queries for each author.

### Final Verification

The verification agent concluded:

* **Verification:** Successful
* **Build:** Successful
* **Tests:** 1 passed, 0 failures, 0 errors
* **SQL evidence:** Single `LEFT JOIN` query
* **Further investigation:** Not required

### Iteration Outcome

Iteration 4 successfully transformed the previous workflow into a more complete autonomous engineering loop:

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

This demonstrates controlled agentic software engineering with restricted file modification and automated verification.