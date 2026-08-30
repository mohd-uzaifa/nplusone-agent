# micro-nplusone-agent

An agentic software engineering system built for the **micro1 Frontier Engineering Challenge 2026**.

This project uses a deliberately reproducible **Spring Boot N+1 query problem** as a controlled software-engineering task and progressively evolves from a simple LLM-based solution into a tool-using agent capable of inspecting real project files, making a restricted source-code modification, running verification, and reasoning from actual build and SQL evidence.

The project compares:

1. a **simple Gemini one-shot baseline**, and
2. a **controlled advanced agent** with file inspection, restricted file modification, and automated verification.

The repository also documents the use of **GitHub Copilot** during the development process. Copilot was used to generate and modify the initial project implementation and to apply the Gemini one-shot baseline fix. The resulting work was then tested, restored, extended, and evaluated through the documented experiments.

---

# 1. Problem and User Value

## Who has this problem?

The intended user is a **software engineer or development team working on a Java/Spring Boot application** who needs to identify and fix database-performance problems such as Hibernate/JPA N+1 queries.

N+1 problems are particularly easy to introduce accidentally because the application code can appear correct while ORM behavior generates many additional database queries at runtime.

## What is the bottleneck?

A developer may need to:

* inspect entity relationships,
* understand JPA fetch behavior,
* identify the code path causing lazy loading,
* determine whether an N+1 query exists,
* modify the repository or query strategy,
* run the application,
* inspect generated SQL,
* verify that the query count actually improved.

A static code review can suggest a fix without proving that the fix works at runtime.

## Why is solving it valuable?

A useful engineering agent should not stop at generating plausible code.

It should connect:

```text
source-code reasoning
        ↓
controlled modification
        ↓
actual build/test execution
        ↓
runtime evidence
        ↓
engineering decision
```

The value of this project is therefore not simply producing an `@EntityGraph` annotation. The important improvement is moving toward an **evidence-driven engineering workflow** where the proposed change can be tested against the real application.

---

# 2. Challenge Alignment

The micro1 challenge asks participants to build an agentic workflow where the solution is:

* purposeful,
* technically sound,
* reproducible,
* measurable,
* clearly documented,
* supported by agent trajectories and evidence.

This repository follows that structure through:

* a controlled baseline,
* an advanced agent,
* an improvement changelog,
* experimental evidence,
* reproducible commands,
* agent trajectories,
* explicit tool restrictions,
* measured SQL query reduction.

The challenge guidance recommends using ten or more evaluation cases when the task allows it.

This implementation currently uses **one controlled Scenario A evaluation case** with a deterministic dataset of five authors and three books per author. No additional cases or results are fabricated in this repository. Expanding the evaluation to multiple N+1 patterns is a possible future improvement.

---

# 3. Baseline Task

The application intentionally contains an N+1 query problem involving `Author` and `Book` entities.

The `/authors` endpoint performs:

```text
authorRepository.findAll()
        ↓
author.getBooks().size()
```

The `books` relationship is lazily loaded.

With five authors, the baseline produces:

```text
1 query to load authors
+
5 queries to load books
=
6 SQL queries
```

The five book queries correspond to author IDs:

```text
1, 2, 3, 4, 5
```

Therefore:

```text
1 + N
1 + 5
= 6 queries
```

This implementation is intentionally preserved as the controlled test case.

---

# 4. Primary Evaluation Metric

The primary metric is:

> **Observed SQL query count required to serve `GET /authors`.**

This metric directly represents the performance problem being solved.

## Success criterion

The baseline behavior is:

```text
6 SQL queries
```

A successful fix should produce:

```text
1 SQL query
```

using a joined fetch strategy.

## Final observed change

```text
Baseline:        6 SQL queries
Agent-fixed:     1 SQL query

Reduction:       5 queries
Relative reduction: approximately 83.3%
```

The comparison uses the same Scenario A application and dataset.

The SQL output is used as runtime evidence rather than relying only on the LLM's claim that the fix should work.

---

# 5. Technology Stack

* Java 25.0.3
* Spring Boot 4.0.1
* Spring Data JPA
* Hibernate
* H2 in-memory database
* Maven Wrapper
* Google Gemini API
* Git / GitHub
* GitHub Copilot

---

# 6. What Existed Before the Competition vs What Was Added

The controlled Spring Boot N+1 problem is the foundation of the project.

The agentic engineering workflow was then developed around it.

## Existing / starting components

The starting application contained:

* Spring Boot application
* `Author` entity
* `Book` entity
* lazy `Author → Book` relationship
* `AuthorRepository`
* `AuthorController`
* H2 database
* sample data
* `/authors` endpoint
* Maven build/test configuration

## Added during the project

The project was progressively extended with:

* Gemini integration
* agent reasoning workflow
* `FileTools.readFile()`
* `FileTools.writeFile()`
* restricted write boundary
* `FileTools.runVerification()`
* agent trajectories
* experiment evidence
* improvement changelog
* reproducibility documentation

GitHub Copilot was used during development to generate and modify code, including the initial baseline implementation and the application of the Gemini one-shot fix. These uses are explicitly disclosed in the trajectory documentation.

---

# 7. Project Structure

```text
micro-nplusone-agent/

├── .gitignore
├── EXPERIMENT_LOGS.md
├── README.md
├── pom.xml
├── mvnw
├── mvnw.cmd
│
├── .mvn/
│   └── wrapper/
│       ├── maven-wrapper.jar
│       └── maven-wrapper.properties
│
├── evidence/
│   ├── baseline_gemini_fix/
│   ├── baseline_nplusone_sql/
│   ├── baseline_nplusone_sql_restored/
│   ├── experiment4_iteration1_llm_analyzer/
│   ├── experiment4_iteration2_read_files/
│   ├── experiment4_iteration3_write_file/
│   └── experiment4_iteration4_verification/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/nplusone/
│   │   │       ├── Author.java
│   │   │       ├── AuthorController.java
│   │   │       ├── AuthorRepository.java
│   │   │       ├── Book.java
│   │   │       ├── NPlusOneApplication.java
│   │   │       ├── SampleDataConfig.java
│   │   │       │
│   │   │       └── agent/
│   │   │           ├── FileTools.java
│   │   │           ├── GeminiTest.java
│   │   │           └── NPlusOneAgent.java
│   │   │
│   │   └── resources/
│   │       └── application.properties
│   │
│   └── test/
│       └── java/
│           └── com/example/nplusone/
│               └── NPlusOneApplicationTests.java
│
└── trajectories/
    ├── baseline-scenario-a.md
    ├── copilot-baseline.md
    └── experiment-4-advanced-agent.md
```

The `target/` directory is generated by Maven and is excluded through `.gitignore`.

---

# 8. Improvement Changelog

The project was developed through a sequence of controlled experiments.

```text
Experiment 1
Baseline N+1 reproduction
        ↓
Experiment 2
Gemini one-shot fix
        ↓
Experiment 3
Restore controlled baseline
        ↓
Experiment 4 — Iteration 1
LLM analyzer
        ↓
Experiment 4 — Iteration 2
Real file inspection
        ↓
Experiment 4 — Iteration 3
Restricted file modification
        ↓
Experiment 4 — Iteration 4
Automated verification
```

---

## Experiment 1 — Baseline N+1 Reproduction

### What was tried

A deterministic Spring Boot application was created containing:

* 5 authors
* 3 books per author
* lazy `Author → Book` relationship
* `GET /authors`

The purpose was to create a small, reproducible N+1 engineering problem.

### Tool disclosure

GitHub Copilot was used during the development of the initial application and baseline implementation.

The Copilot trajectory is documented in:

```text
trajectories/copilot-baseline.md
```

### Result

The application produced:

```text
1 author query
+
5 book queries
=
6 SQL queries
```

### Evidence

```text
evidence/baseline_nplusone_sql/
```

### Decision

The implementation was frozen as the controlled baseline.

---

## Experiment 2 — Gemini One-Shot Fix

### What was tried

Gemini Pro was given the Scenario A Java source code and asked to:

1. identify the N+1 problem,
2. explain why it occurs,
3. identify the responsible code,
4. propose one concrete Spring Data JPA fix,
5. provide the required code changes,
6. explain the expected query reduction,
7. discuss relevant trade-offs.

The prompt explicitly prevented unnecessary redesign and instructed Gemini not to claim runtime verification.

### Proposed solution

Gemini proposed:

```java
@EntityGraph(attributePaths = "books")
```

with a repository method:

```text
findAllWithBooks()
```

The controller was changed to use that method.

### Tool disclosure

GitHub Copilot was used to apply the Gemini-generated code changes to the repository.

This is documented in:

```text
trajectories/copilot-baseline.md
```

The actual Gemini reasoning is documented in:

```text
trajectories/baseline-scenario-a.md
```

### Result

The application produced:

```text
Baseline:         6 queries
Gemini solution:  1 query
```

Hibernate generated a single `LEFT JOIN` query.

### Evidence

```text
evidence/baseline_gemini_fix/
```

### Decision

Keep the Gemini one-shot approach as the simple baseline against which the advanced agent is compared.

### Limitation

Gemini did not independently perform:

```text
observe
→
modify
→
verify
→
retry
```

The runtime verification was performed separately.

---

## Experiment 3 — Restore Controlled Baseline

### What was tried

After evaluating the Gemini solution, the original N+1 implementation was restored using Git.

The restoration used the original baseline commit:

```text
78f89c5 — baseline: reproduce N+1 query problems
```

The Gemini trajectory and evidence were preserved.

### Result

The application again produced:

```text
1 author query
+
5 book queries
=
6 SQL queries
```

### Evidence

```text
evidence/baseline_nplusone_sql_restored/
```

### Decision

Keep the original N+1 implementation as the controlled evaluation state for the advanced agent.

---

# 9. Experiment 4 — Advanced Agent

Experiment 4 progressively transformed the workflow from an LLM analyzer into a controlled tool-using software engineering agent.

---

## Iteration 1 — LLM Analyzer

### Goal

Determine whether Gemini could correctly reason about the N+1 problem when given the relevant source-code context.

### Agent capability

The agent could:

* receive source-code context,
* analyze the code,
* identify the N+1 problem,
* propose a Spring Data JPA fix,
* identify what should be verified.

### Result

Gemini correctly identified:

```text
authorRepository.findAll()
+
author.getBooks().size()
```

as the N+1 path.

It correctly predicted:

```text
1 + N queries
```

and proposed an `@EntityGraph` solution.

### Evidence

```text
evidence/experiment4_iteration1_llm_analyzer/
```

### Limitation

At this stage the agent could not:

* inspect the filesystem independently,
* modify source files,
* run the application,
* measure runtime SQL,
* verify the proposed fix.

### Decision

Introduce real file inspection.

---

## Iteration 2 — Real File Inspection

### Change

Added:

```java
FileTools.readFile()
```

The agent could now read the actual project files:

```text
Author.java
AuthorController.java
AuthorRepository.java
```

### Workflow

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

### Result

Gemini again correctly identified the N+1 problem and proposed an `@EntityGraph` solution.

### Evidence

```text
evidence/experiment4_iteration2_read_files/
```

### Verification

The project compiled successfully using:

```text
.\mvnw.cmd compile
```

### Observation

Gemini also suggested an optional DTO aggregation query as an alternative.

This did not invalidate the diagnosis, but it showed that the agent needed stronger output constraints when only one concrete fix was desired.

### Decision

Introduce controlled file modification.

---

## Iteration 3 — Controlled File Modification

### Change

Added:

```java
FileTools.writeFile()
```

The write operation was deliberately restricted.

The agent could modify only:

```text
src/main/java/com/example/nplusone/AuthorRepository.java
```

### Safety rationale

The restriction prevents the model from arbitrarily modifying unrelated files.

### Workflow

```text
Read source
    ↓
Gemini reasoning
    ↓
Generate fix
    ↓
Restricted write
    ↓
Compile
    ↓
Runtime verification
```

### Result

Gemini generated the repository fix using:

```java
@EntityGraph(attributePaths = "books")
```

The write operation returned:

```text
SUCCESS: File written:
src/main/java/com/example/nplusone/AuthorRepository.java
```

### Runtime result

The `/authors` endpoint produced a single `LEFT JOIN` query.

Observed result:

```text
6 queries → 1 query
```

### Evidence

```text
evidence/experiment4_iteration3_write_file/
```

### Decision

The agent could now read and modify a real project through a controlled tool boundary.

The next step was to move verification into the agent workflow itself.

---

## Iteration 4 — Autonomous Verification

### Change

Added:

```java
FileTools.runVerification()
```

The tool executes:

```text
.\mvnw.cmd test
```

and returns summarized verification information including:

* exit code,
* relevant SQL evidence,
* test result,
* build status.

### Final workflow

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

### Result

At the beginning of this iteration, the repository already contained the successful `@EntityGraph` fix from Iteration 3.

The agent correctly recognized that the N+1 problem had already been resolved and did not introduce an unnecessary second modification.

The verification process returned:

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

### Evidence

```text
evidence/experiment4_iteration4_verification/
```

### Decision

Keep the final tool-using agent as the advanced solution.

---

# 10. Baseline vs Advanced Solution

| Capability                       | Gemini One-Shot Baseline | Advanced Agent |
| -------------------------------- | ------------------------ | -------------- |
| Static source analysis           | Yes                      | Yes            |
| Identify N+1 problem             | Yes                      | Yes            |
| Propose fix                      | Yes                      | Yes            |
| Read actual project files        | No                       | Yes            |
| Modify project files             | No                       | Yes            |
| Restricted write boundary        | No                       | Yes            |
| Run automated verification       | No                       | Yes            |
| Inspect SQL evidence             | No                       | Yes            |
| Inspect test/build result        | No                       | Yes            |
| Autonomous verification decision | No                       | Yes            |
| Automatic retry/recovery         | No                       | Not yet        |

The main improvement is therefore not simply that both approaches can produce the same `@EntityGraph` fix.

The advanced agent adds an engineering feedback loop around the change:

```text
real files
→
reasoning
→
controlled modification
→
verification
→
evidence
→
decision
```

---

# 11. Final Evaluation

## Evaluation case

The current controlled evaluation uses Scenario A:

```text
5 authors
3 books per author
GET /authors
```

## Results

| Stage             | SQL Queries | Result                         |
| ----------------- | ----------: | ------------------------------ |
| Baseline N+1      |           6 | Intentional N+1                |
| Gemini one-shot   |           1 | N+1 eliminated                 |
| Restored baseline |           6 | Controlled evaluation restored |
| Advanced agent    |           1 | N+1 eliminated                 |

### Primary metric

```text
SQL query count
```

### Final measured improvement

```text
6 → 1 SQL query
```

This represents:

```text
5 fewer queries
≈83.3% reduction
```

for this specific controlled dataset.

---

# 12. Challenging Case

The challenging case in this project is **Experiment 4 — Iteration 4: Autonomous Verification**.

At the beginning of this iteration, `AuthorRepository.java` already contained the `@EntityGraph(attributePaths = "books")` fix introduced during Iteration 3.

Instead of blindly generating and applying another modification, the agent first inspected the current project state and recognized that the N+1 problem had already been resolved.

The agent therefore did not introduce an unnecessary second modification and proceeded directly to automated verification.

The workflow was:

```text
Existing fixed repository
        ↓
Agent reads current state
        ↓
Recognizes N+1 issue is already resolved
        ↓
No unnecessary modification
        ↓
Run automated verification
        ↓
Inspect SQL and test results
        ↓
Determine success
```

The automated verification returned:

```text
EXIT_CODE: 0

TEST RESULT:
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0

BUILD SUCCESS
```

The SQL evidence showed a single `LEFT JOIN` query between the `author` and `book` tables rather than one author query followed by separate book queries.

The observed result remained:

```text
Baseline: 6 SQL queries
Fixed implementation: 1 SQL query
```

### What This Case Revealed

This case demonstrated that the agent was not simply designed to modify a file whenever it received an N+1 diagnosis.

It could inspect the current state of the repository, recognize that the proposed optimization was already present, avoid an unnecessary modification, and use verification to confirm the existing implementation.

This is important for an engineering agent because repeatedly applying the same fix without checking the current state could introduce unnecessary changes or create new problems.

The case therefore tested an additional capability beyond simply generating a correct fix:

```text
state awareness → avoid redundant modification → verify actual result
```

The evidence for this case is documented in:

```text
evidence/experiment4_iteration4_verification/
```

The detailed workflow is documented in:

```text
trajectories/experiment-4-advanced-agent.md
```

This is the challenging case actually demonstrated in the repository. No additional unimplemented test cases are claimed.


---

# 13. Agent Architecture

The main agent implementation is located in:

```text
src/main/java/com/example/nplusone/agent/
```

## `NPlusOneAgent.java`

Coordinates the agent workflow and Gemini communication.

It is responsible for moving through the engineering process and using the available tools.

## `FileTools.java`

Provides controlled project operations:

```text
readFile()
writeFile()
runVerification()
```

### `readFile()`

Retrieves source code from the actual project.

### `writeFile()`

Writes agent-generated source code subject to a restricted path boundary.

### `runVerification()`

Runs the Maven verification command and returns relevant evidence to the agent.

## `GeminiTest.java`

Provides the Gemini API integration/testing component.

---

# 14. Tool Control and Safety

The advanced agent does not receive unrestricted filesystem access.

The write operation is explicitly restricted to:

```text
src/main/java/com/example/nplusone/AuthorRepository.java
```

This limits the blast radius of an incorrect model-generated change.

Verification is also exposed through a controlled tool rather than allowing arbitrary command execution:

```text
.\mvnw.cmd test
```

The project uses a synthetic H2 dataset and does not require private user data.

No credentials or API keys are included in the repository.

Any Gemini API credential must be supplied through the developer's local environment/configuration and must not be committed to Git.

---

# 15. Agent Trajectories

The challenge requires representative trajectories for every agent used.

This repository documents the following:

## GitHub Copilot

```text
trajectories/copilot-baseline.md
```

Documents Copilot's role in:

* generating the initial baseline application,
* creating the intentional N+1 scenario,
* applying the Gemini one-shot fix.

## Gemini One-Shot Baseline

```text
trajectories/baseline-scenario-a.md
```

Documents:

* the Gemini prompt,
* the source-code reasoning,
* the proposed `@EntityGraph` solution,
* trade-offs,
* the human/runtime verification,
* the final result.

## Advanced Agent

```text
trajectories/experiment-4-advanced-agent.md
```

Documents:

* agent instructions,
* file inspection,
* Gemini reasoning,
* tool responses,
* restricted modification,
* verification,
* SQL evidence,
* final decision.

The trajectories are intended to make the agent's reasoning and tool interactions traceable rather than presenting only the final code.

---

# 16. Evidence

Each major experiment has corresponding evidence.

```text
evidence/

├── baseline_gemini_fix/
│
├── baseline_nplusone_sql/
│
├── baseline_nplusone_sql_restored/
│
├── experiment4_iteration1_llm_analyzer/
│
├── experiment4_iteration2_read_files/
│
├── experiment4_iteration3_write_file/
│
└── experiment4_iteration4_verification/
```

The evidence is used to support the claims made in the experiment log.

Detailed experiment history is documented in:

```text
EXPERIMENT_LOGS.md
```

---

# 17. Reproduction Guide

This section is written for a developer starting from a clean environment.

## Requirements

Install:

* Java 25
* Git
* a working internet connection for Maven dependency resolution

The repository includes the Maven Wrapper, so Maven does not need to be installed separately.

## Clone the repository

```text
git clone https://github.com/mohd-uzaifa/micro1-nplusone-agent.git
cd micro1-nplusone-agent
```

## Verify Java

```text
java -version
```

Expected major version:

```text
25
```

## Compile

Windows:

```text
.\mvnw.cmd compile
```

Expected result:

```text
BUILD SUCCESS
```

## Run tests

```text
.\mvnw.cmd test
```

Expected result:

```text
Tests run: 1
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

## Run the application

```text
.\mvnw.cmd spring-boot:run
```

The application exposes:

```text
GET /authors
```

The endpoint can be requested using a browser or HTTP client.

---

# 18. Reproducing the Baseline

To reproduce the original N+1 behavior, the controlled baseline implementation must be present.

The relevant code path is:

```text
authorRepository.findAll()
        ↓
author.getBooks().size()
```

Run:

```text
.\mvnw.cmd test
```

Then start the application:

```text
.\mvnw.cmd spring-boot:run
```

Call:

```text
GET /authors
```

Inspect the Hibernate SQL logs.

Expected behavior:

```text
1 author query
+
5 book queries
=
6 SQL queries
```

The five book queries should correspond to:

```text
author IDs: 1, 2, 3, 4, 5
```

---

# 19. Reproducing the Fixed Implementation

The fixed repository method uses:

```java
@EntityGraph(attributePaths = "books")
```

The controller calls:

```text
findAllWithBooks()
```

Run:

```text
.\mvnw.cmd test
```

Then start the application:

```text
.\mvnw.cmd spring-boot:run
```

Call:

```text
GET /authors
```

Expected Hibernate behavior:

```text
1 SQL query
```

The query should contain a join between:

```text
author
book
```

Expected shape:

```sql
select
    ...
from
    author
left join
    book
        on author.id = book.author_id
```

---

# 20. Reproducing the Advanced Agent Verification

The advanced agent uses:

```text
FileTools.readFile()
FileTools.writeFile()
FileTools.runVerification()
```

The intended workflow is:

```text
Read source
    ↓
Analyze
    ↓
Generate proposed fix
    ↓
Restricted write
    ↓
Run verification
    ↓
Inspect SQL/test/build evidence
    ↓
Determine success
```

The final verification command is:

```text
.\mvnw.cmd test
```

The documented successful run returned:

```text
EXIT_CODE: 0

Tests run: 1
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

---

# 21. Versions, Runtime and Cost

## Versions

```text
Java:          25.0.3
Spring Boot:   4.0.1
Database:      H2
ORM:           Hibernate
Build:         Maven Wrapper
LLM:           Google Gemini
Coding agent:  GitHub Copilot
```

## Runtime

The baseline Maven test run completed successfully.

The Gemini baseline test run recorded approximately:

```text
12.988 seconds
```

Actual runtime can vary depending on machine and dependency-cache state.

The runtime of the application itself is small because it uses an in-memory H2 database and a five-author synthetic dataset.

## Cost

The application itself does not require a paid database or cloud runtime.

The project uses external AI tooling:

* GitHub Copilot
* Google Gemini API

Any model/API cost depends on the participant's account, plan, model, and usage.

No claim of a fixed API cost is made because the actual provider-side billing depends on the user's account configuration.

---

# 22. Current Limitations

## Single evaluation scenario

The current evaluation focuses on one controlled Scenario A.

The challenge guidance suggests ten or more cases when the task allows it, but this repository does not currently claim to have executed ten distinct cases.

Expanding the benchmark to multiple N+1 patterns would provide stronger evidence of generalization.

## No automatic retry loop

The current advanced agent demonstrates:

```text
read
→
reason
→
write
→
verify
→
decide
```

It does not yet automatically perform:

```text
verification failure
        ↓
diagnose failure
        ↓
generate new fix
        ↓
rewrite
        ↓
verify again
```

This is the most significant missing capability.

## Limited write boundary

The write tool intentionally modifies only:

```text
AuthorRepository.java
```

This improves safety and reproducibility but also limits the range of problems the agent can solve.

A broader engineering agent would need a more sophisticated approval and sandbox model.

---

# 23. Experiment Removed / Not Kept

The project intentionally did not evolve the system into an unrestricted autonomous coding agent.

Instead, the write capability was kept behind a narrow path restriction.

This was a deliberate engineering decision.

The project prioritizes:

```text
controlled actions
+
reproducibility
+
evidence
```

over giving the model unrestricted access to the repository.

This also keeps the experiment aligned with the challenge requirement to keep consequential actions controlled.

---

# 24. Main Failure Mode

The main failure mode observed during development is:

> **A plausible LLM-generated fix can be correct without the LLM itself having evidence that it is correct.**

Gemini successfully proposed the `@EntityGraph` fix.

However, its one-shot response could not independently establish:

```text
actual query count
actual runtime behavior
test result
build result
```

The advanced workflow addresses this by returning real project and verification evidence to the agent.

The project therefore treats **verification as part of the engineering task rather than an optional final step**.

---

# 25. Hot Take

> **The most important upgrade to a coding agent is not giving it more freedom; it is giving it better feedback.**

A model can already generate a plausible N+1 fix.

The more interesting engineering capability is allowing the model to:

```text
inspect
→
change
→
measure
→
reason from evidence
```

while keeping consequential actions constrained.

In this experiment, the largest improvement came from moving verification closer to the agent rather than simply changing the model prompt.

The next generation of this system should therefore prioritize **failure-aware verification and automatic recovery**, rather than simply increasing the amount of code the model is allowed to edit.

---

# 26. Final Result

The project demonstrates the following progression:

```text
LLM Analysis
      ↓
Real File Inspection
      ↓
Controlled File Modification
      ↓
Automated Verification
```

The final demonstrated result is:

```text
Baseline N+1:       6 SQL queries
Gemini one-shot:    1 SQL query
Restored baseline:  6 SQL queries
Advanced agent:     1 SQL query
```

Therefore:

```text
6 → 1 SQL query
```

for the controlled Scenario A evaluation.

The advanced agent successfully demonstrated:

* real project-file inspection,
* N+1 diagnosis,
* concrete fix generation,
* restricted source modification,
* automated Maven verification,
* SQL evidence inspection,
* test/build result inspection,
* engineering success determination.

---

# 27. Repository Documentation

The main supporting documents are:

```text
README.md
    ↓
Project overview, reproduction and final result

EXPERIMENT_LOGS.md
    ↓
Detailed experiment history

trajectories/copilot-baseline.md
    ↓
GitHub Copilot development trajectory

trajectories/baseline-scenario-a.md
    ↓
Gemini one-shot baseline trajectory

trajectories/experiment-4-advanced-agent.md
    ↓
Advanced agent trajectory

evidence/
    ↓
Screenshots and runtime evidence
```

---

# 28. Conclusion

This project demonstrates a controlled progression from a simple LLM coding workflow to a tool-using agentic software engineering workflow.

The key lesson is that an engineering agent should not be evaluated only on whether it can produce plausible code.

It should be evaluated on whether it can:

```text
understand the problem
        ↓
inspect the real system
        ↓
make a controlled change
        ↓
run the system
        ↓
inspect evidence
        ↓
determine whether the change worked
```

For the controlled N+1 query task, the final measured result is:

```text
6 SQL queries
      ↓
1 SQL query
```

The repository provides the code, experiments, evidence, trajectories, and reproduction instructions needed to inspect and reproduce that result.
