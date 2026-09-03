# N+1 Query Fixing Agent

An agentic software engineering tool powered by **Google Gemini** that detects, fixes, and verifies **Hibernate/JPA N+1 query problems** in Spring Boot applications.

The project combines LLM reasoning with real source-code inspection, controlled code modification, automated verification, and runtime SQL evidence.

## Overview

The **N+1 Query Problem** is a common database performance issue in ORM-based applications.

Instead of fetching related data efficiently in a single query, an application may execute:

* 1 query to fetch the parent entities
* N additional queries to fetch related entities for each parent

For example, fetching 5 authors and then lazily loading their books can result in:

```text
1 query → fetch authors
5 queries → fetch books for each author

Total = 6 SQL queries
```

This project builds an agent that can inspect the Spring Boot codebase, identify the cause of the N+1 problem, apply a targeted fix, and verify the result using actual Hibernate SQL output.

---

## Key Result

In the controlled test scenario:

| Implementation      | SQL Queries |
| ------------------- | ----------: |
| Baseline N+1        |       **6** |
| Gemini one-shot fix |       **1** |
| Restored baseline   |       **6** |
| Agentic fix         |       **1** |

### Improvement

**6 → 1 SQL queries**

* 5 fewer database queries
* **83.3% reduction**
* Verified using actual Hibernate SQL logs

The important part is that success is determined from **runtime SQL evidence**, not simply from whether the project compiles.

---

## The Problem

The test application contains two entities:

```text
Author
 └── books → Book
```

The relationship is configured for lazy loading.

The `/authors` endpoint retrieves authors and accesses their books:

```java
authorRepository.findAll();

author.getBooks().size();
```

Because the books are lazily loaded, Hibernate performs an additional query for each author.

With 5 authors:

```text
SELECT ... FROM author;

SELECT ... FROM book WHERE author_id = 1;
SELECT ... FROM book WHERE author_id = 2;
SELECT ... FROM book WHERE author_id = 3;
SELECT ... FROM book WHERE author_id = 4;
SELECT ... FROM book WHERE author_id = 5;
```

Result:

```text
1 + 5 = 6 queries
```

---

# How the Agent Works

The agent follows a controlled software-engineering workflow:

```text
┌──────────────────────┐
│ Inspect Source Code  │
└──────────┬───────────┘
           ↓
┌──────────────────────┐
│ Analyze Root Cause   │
└──────────┬───────────┘
           ↓
┌──────────────────────┐
│ Generate Targeted Fix│
└──────────┬───────────┘
           ↓
┌──────────────────────┐
│ Modify Source File   │
└──────────┬───────────┘
           ↓
┌──────────────────────┐
│ Run Maven Tests      │
└──────────┬───────────┘
           ↓
┌──────────────────────┐
│ Inspect SQL Evidence │
└──────────┬───────────┘
           ↓
┌──────────────────────┐
│ Determine Result     │
└──────────────────────┘
```

The agent is powered by Gemini for reasoning, but the actual software-engineering workflow is implemented by the project.

---

# Agent Architecture

The agent is implemented in Java and uses the Google Gemini API.

### Main components

```text
NPlusOneAgent
│
├── FileTools
│   ├── readFile()
│   ├── writeFile()
│   └── runVerification()
│
├── Gemini
│   └── reasoning / code generation
│
└── Maven + Hibernate
    └── build / tests / SQL evidence
```

### Workflow

### 1. Inspect

The agent reads the relevant application files:

* `AuthorController.java`
* `AuthorRepository.java`
* `Author.java`

This gives the model actual context from the codebase instead of asking it to solve the problem from a generic description.

### 2. Analyze

Gemini analyzes the source and identifies the lazy-loading pattern responsible for the N+1 queries.

The agent predicts the expected query pattern:

```text
1 + N queries
```

### 3. Generate a Fix

The agent generates a targeted repository-level modification.

For this scenario, the selected solution uses:

```java
@EntityGraph(attributePaths = {"books"})
```

This allows the books to be fetched together with the authors.

### 4. Modify the Code

The agent writes the generated repository implementation into the project.

For safety, the write operation is restricted to the intended repository file rather than allowing unrestricted modifications across the entire project.

### 5. Verify

The agent runs the project's Maven verification process.

It checks:

* Maven exit status
* test results
* build result
* Hibernate SQL output

### 6. Validate Runtime Behavior

Compilation alone is not considered sufficient evidence.

The important question is:

> Did the number of SQL queries actually decrease?

The successful agent run produced a single SQL statement using a join between the author and book tables.

---

# Agent Tools

The agent has a small set of purpose-built tools.

## `readFile()`

Reads source files from the project so Gemini can reason about the actual implementation.

## `writeFile()`

Writes the generated fix to the designated repository file.

The write scope is intentionally restricted to:

```text
AuthorRepository.java
```

## `runVerification()`

Runs the verification workflow and returns summarized evidence including:

```text
EXIT_CODE
SQL EVIDENCE
TEST RESULT
BUILD STATUS
```

This allows the agent to reason about whether its change actually worked.

---

# Example Fix

### Before

The repository uses the normal `findAll()` behavior:

```java
public interface AuthorRepository extends JpaRepository<Author, Long> {
}
```

Authors are retrieved first, while their books are lazily loaded later.

This produces:

```text
6 SQL queries
```

### Agent-generated approach

The agent adds an entity graph:

```java
@EntityGraph(attributePaths = {"books"})
@Override
List<Author> findAll();
```

Hibernate can then retrieve the authors and their books together.

Result:

```text
1 SQL query
```

---

# Results

The project tested the same controlled scenario across multiple implementations.

| Experiment   | Description                           |    Result |
| ------------ | ------------------------------------- | --------: |
| Experiment 1 | Baseline N+1 reproduction             | 6 queries |
| Experiment 2 | Gemini one-shot fix                   |   1 query |
| Experiment 3 | Baseline restored                     | 6 queries |
| Experiment 4 | Agent inspection → fix → verification |   1 query |

### Final comparison

```text
Baseline

Authors query       ████████████████████  1
Book queries         ██████████████████████████████████████████████████  5
Total                                      6


Agent Fix

Combined query      ████████████████████  1
Total                                      1
```

**Query reduction: 83.3%**

---

# Why This Is an Agent

The project is not simply a prompt sent to an LLM.

The agent performs an engineering workflow around the LLM:

```text
LLM reasoning
     +
real codebase inspection
     +
controlled tool usage
     +
source-code modification
     +
automated verification
     +
runtime evidence
```

This distinction is important.

An LLM suggesting:

> "Use @EntityGraph"

is different from an agent that:

1. Inspects the repository
2. Identifies the N+1 cause
3. Generates the change
4. Modifies the actual source file
5. Runs the application/tests
6. Inspects SQL output
7. Determines whether the fix actually reduced queries

---

# Experiments

The project was developed and evaluated through several controlled experiments.

### Experiment 1 — Baseline

The original lazy-loading implementation was executed.

```text
Expected: 1 + N
Observed: 6 queries
```

### Experiment 2 — One-Shot Gemini Fix

Gemini was given the relevant code and asked to fix the N+1 problem directly.

The resulting `@EntityGraph` solution reduced the query count:

```text
6 → 1
```

### Experiment 3 — Baseline Restoration

The baseline implementation was restored to ensure that the comparison was not accidentally performed against an already-fixed application.

```text
Observed: 6 queries
```

### Experiment 4 — Agentic Workflow

The autonomous workflow was executed:

```text
Inspect
  ↓
Analyze
  ↓
Generate fix
  ↓
Write
  ↓
Verify
  ↓
Inspect SQL
```

The agent successfully produced the same repository-level optimization and runtime evidence showed:

```text
1 SQL query
```

---

# Project Structure

```text
nplusone-agent/
│
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/example/nplusone/
│   │           ├── Author.java
│   │           ├── Book.java
│   │           ├── AuthorController.java
│   │           ├── AuthorRepository.java
│   │           │
│   │           └── agent/
│   │               ├── FileTools.java
│   │               ├── GeminiTest.java
│   │               └── NPlusOneAgent.java
│   │
│   └── test/
│
├── evidence/
│
├── trajectories/
│
├── EXPERIMENT_LOGS.md
├── README.md
├── pom.xml
├── mvnw
└── mvnw.cmd
```

---

# Tech Stack

### Application

* Java 25
* Spring Boot 4
* Spring Data JPA
* Hibernate
* H2 Database
* Maven

### Agent

* Google Gemini API
* Java Gemini SDK

### Development

* Git
* GitHub
* IntelliJ IDEA / VS Code

---

# Running the Project

## Requirements

* Java 25
* Git
* Internet connection for Maven dependency resolution
* Gemini API key for running the agent

Check Java:

```bash
java -version
```

The project expects Java 25.

---

## Clone

```bash
git clone https://github.com/mohd-uzaifa/nplusone-agent.git
cd nplusone-agent
```

---

## Compile

### Windows

```powershell
.\mvnw.cmd compile
```

### macOS / Linux

```bash
./mvnw compile
```

---

## Run Tests

### Windows

```powershell
.\mvnw.cmd test
```

### macOS / Linux

```bash
./mvnw test
```

---

## Run the Spring Boot Application

### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

### macOS / Linux

```bash
./mvnw spring-boot:run
```

---

# Running the Agent

The agent requires a Gemini API key.

Set the environment variable before running the agent.

### Windows PowerShell

```powershell
$env:GEMINI_API_KEY="YOUR_API_KEY"
```

### macOS / Linux

```bash
export GEMINI_API_KEY="YOUR_API_KEY"
```

Do **not** commit API keys to GitHub.

The agent can then be executed using the project's Java/Maven configuration.

---

# Reproducibility

The project uses an H2 in-memory database and a deterministic test scenario:

```text
Authors: 5
Books per author: 3
Relationship: Author → Books
Loading: Lazy
```

The baseline consistently demonstrates:

```text
1 author query
+
5 book queries
=
6 SQL queries
```

The optimized implementation produces:

```text
1 SQL query
```

The query count is determined from Hibernate SQL output rather than inferred from source code alone.

---

# Limitations

This project is intentionally a controlled demonstration of an agentic software-engineering workflow.

Current limitations include:

* Evaluation uses a single primary N+1 scenario.
* The database is an in-memory H2 environment.
* The write tool is intentionally restricted to a specific repository file.
* The agent does not currently perform unrestricted repository-wide refactoring.
* There is no fully automated retry/recovery loop for every possible LLM or verification failure.
* The measured 6 → 1 improvement is demonstrated on a small synthetic dataset and should not be interpreted as a production benchmark.

---

# Future Improvements

Possible extensions include:

* Support for multiple N+1 patterns
* Automatic detection of N+1 problems from SQL traces
* Repository-wide code analysis
* Approval workflow before modifying files
* Automatic retry and recovery after failed verification
* Larger evaluation suites
* Before/after performance benchmarking
* Support for additional Hibernate performance problems
* More advanced query optimization strategies
* Integration with CI/CD pipelines

---

# Key Takeaway

The main idea behind this project is simple:

> **An LLM suggesting code is not the same as an agent fixing software.**

The useful part is the complete loop:

```text
Understand the code
       ↓
Identify the problem
       ↓
Make a targeted change
       ↓
Run the software
       ↓
Measure the result
       ↓
Use the evidence to determine success
```

This project demonstrates how an LLM can be integrated into a controlled software-engineering workflow rather than being used only as a code-generation interface.

---

## License

This project is intended for educational and portfolio purposes.
