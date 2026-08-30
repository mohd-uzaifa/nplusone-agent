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

## Next Planned Iteration

### Iteration 3 — Controlled File Modification

The agent will be given a restricted `write_file` capability.

The goal is to allow the agent to:

1. reason about a fix,
2. identify the file that should change,
3. apply the change through a controlled tool,
4. preserve the frozen baseline outside the agent workspace.

The change will then be followed by compilation and verification.