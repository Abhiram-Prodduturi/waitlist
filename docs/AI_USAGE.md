# AI Prompt and Use Record

AI assistant: Kiro IDE agent (model selection "Auto"). Entries are added as
the work happens. Prompts are recorded as sent, with typos kept. No
credentials, secrets, or personal data were included in any prompt.

Decision key: **Accepted** (used as proposed), **Edited** (used after changes),
**Rejected** (not used).

The `.kiro/` and `.cursor/` folders this record refers to (AI skills, steering
context, and the Sonar pre-check script) were kept local and are not in the
repository. The assessment PDF is also not included.

---

## Entry 1 — Port code-generation skills from Cursor to Kiro

- **Activity:** Tooling setup.
- **Prompt:**
  > skills added in .cursor, can you add it to kiro too
  > which is needed for our code generation
- **AI response (summary):** Read all 11 files under `.cursor/skills/` and
  recreated them under `.kiro/skills/` with the same layout. Changed path
  references in `java-solution-generator`, `java-sonar-codegen`, and
  `java-test-generation` from `.cursor/skills` and `.cursor/rules` to
  `.kiro/skills` and `.kiro/steering`. Made `sonar_precheck.sh` executable.
- **Decision:** Accepted.
- **Files informed:** `.kiro/skills/**`.
- **Verification:** Listed `.kiro/skills` and compared it to the source (11
  files, same layout). Ran `sonar_precheck.sh` to confirm it executes (`exit 0`
  on a tree with no Java files). The Kiro skill list now shows all five skills.
- **Open item raised by AI:** The skills cite Phenom-Java rule files
  (`java-style`, `java-bugs`, and others) that do not exist in this workspace.
  Not resolved; the hot rules inside `java-sonar-codegen` are used instead.

## Entry 2 — Capture the assessment brief as persistent context

- **Activity:** Requirements analysis.
- **Prompt 2a:**
  > Read this PDF, which is an Assessment asked, set the context here
- **AI response (summary):** Wrote `.kiro/steering/assessment-context.md`
  (always included) summarizing the objective, problem, acceptance criteria,
  scope limits, and deliverables. Checked the workspace and found no starter
  project and no `Course-Registration-Rubric-Score.md`. Flagged that the Java
  skills' defaults (Redis, Testcontainers, config-switched implementations)
  conflict with the scope limits, and listed five open decisions.
- **Decision:** Edited (see 2b).
- **Prompt 2b:**
  > lets keep the delvirables intact and candidate expectations met here.
- **AI response (summary):** Replaced the paraphrased Deliverables with the
  PDF's wording, added the Candidate Expectations and Suggested Timebox
  sections verbatim, and added a table mapping each deliverable and
  expectation to a file and a "done when" check.
- **Decision:** Accepted.
- **Files informed:** `.kiro/steering/assessment-context.md`.
- **Verification:** Compared the steering file's Deliverables and Candidate
  Expectations sections with the PDF text line by line.
- **What changed because of human review:** The first version paraphrased the
  deliverables and folded the candidate expectations into "working rules".
  The candidate required them verbatim so nothing could be lost in summary.

## Entry 3 — Gap analysis and design options

- **Activity:** Design (option analysis and trade-offs).
- **Prompt 3a:**
  > lets come to the questions/ gaps present the design and plan a solution
  > for one by one.
- **AI response (summary):** For each open question, listed options with
  consequences and a recommendation:
  1. Prerequisite rule: completed-course set vs. a boolean flag; check at
     enroll only vs. enroll and promotion. Recommended the course set, checked
     at both points, because criteria 3 and 4 only make sense if a learner can
     become ineligible while waiting.
  2. Ineligible at head: block vs. skip-and-keep vs. skip-and-remove.
     Recommended skip-and-remove because it keeps "free seat ⇒ empty
     waitlist" strictly true.
  3. Duplicate and invalid requests: a table of outcomes, plus the ambiguity
     in "a learner may enroll only once".
  4. Waitlist cap: recommended none.
  5. Delivery: recommended a Spring Boot REST service with a framework-free
     domain.
  6. Concurrency (gap not in the PDF): per-offering `ReentrantLock` vs.
     compare-and-swap on immutable state. Recommended the lock.
  7. Missing rubric file.
- **Candidate decisions:**
  > 1. use rule (a), checked at both points
  > 2. skip and remove
  > 4. lets have no limit
  > 5. b
  > 6. a
  > 7. there is no rubric file as of now
  >
  > I am not able to plan the outcome of 3, explain it cleary and possible
  > outcomes.
- **Prompt 3b (follow-up on decision 3):** as quoted above.
- **AI response (summary):** Split decision 3 into sub-questions 3a–3f, each
  shown with one worked scenario (2 seats, two enrolled, one waitlisted) and
  the result of each option. Identified the common choice as "harmless no-op
  vs. error, with no state change either way", and noted that a repeated drop
  must never free a second seat.
- **Candidate decisions:**
  > 3a: i / 3b no op / 3c: no op / 3d: no op / 3e: no op
- **Decision:** Accepted, with decision 3 clarified through the follow-up.
- **Options rejected during analysis:**
  - Blocking the queue on an ineligible head: rejected because the seat stays
    empty, contradicting criterion 3.
  - Moving a learner who re-requests while waitlisted to the back: rejected
    because it breaks FIFO fairness.
  - "Enroll once ever" (3a ii): rejected because it requires history and
    locks out learners who dropped by mistake.
  - Config-switchable policies, as the code-generation skills would suggest:
    rejected because the assessment forbids a configurable policy engine.
- **Files informed:** `docs/DESIGN.md` sections 2, 3, 6, 7, 9.
- **Verification:** Mapped every acceptance criterion to a requirement and a
  planned test (`DESIGN.md` sections 1 and 15). Behaviour will be confirmed
  by those tests; this entry will be updated with results.

## Entry 4 — Write the design specification

- **Activity:** Design documentation.
- **Prompt:** the candidate's decisions in Entry 3 (no separate prompt).
- **AI response (summary):** Wrote `docs/DESIGN.md` using the
  `java-hld-design` skill, cut down to the size of this feature: requirements,
  decisions, API and outcome table, domain model, components, invariants
  I-1..I-6, promotion algorithm, concurrency approach, configuration,
  limitations, package layout, diagrams, test plan, and implementation plan.
- **Deviations from the skill, made deliberately:**
  - No capacity estimates or latency targets: one in-memory offering; the
    assessment grades correctness. Stated in the document.
  - No metrics, health checks, or config-selected implementations: out of
    scope and against the "no policy engine" rule.
  - No `Clock`: no logic depends on time.
  - File named `DESIGN.md` (the deliverable name) instead of `HLD.md`.
- **Review prompt from AI:** Before approval, the AI listed eight choices it
  had made that were not discussed (enroll check order, no re-check for
  enrolled learners, missing learner treated as ineligible, no endpoint to
  change completions, 422 body, lock-order FIFO, the extra `GET` endpoint,
  Maven coordinates) and asked the candidate to confirm them.
- **Candidate response:**
  > Approved
- **Decision:** Accepted, including all eight listed choices.
- **Files informed:** `docs/DESIGN.md`.
- **Verification:** Candidate review. Checked that each criterion has a
  requirement and a test:

  | Criterion | Requirement | Planned test |
  |-----------|-------------|--------------|
  | 1 | FR-1, FR-4 | Available capacity |
  | 2 | FR-2, FR-3 | Full capacity, duplicate requests |
  | 3 | FR-5 | FIFO promotion |
  | 4 | FR-4, FR-6 | Prerequisite failure, ineligible at head |
  | 5 | FR-8 | Invalid and repeated drops, invariant I-6 |
  | 6 | Section 15 | All rows |
  | 7 | This file | — |

## Entry 5 — Scaffold, domain model, and domain tests (DESIGN.md steps 1–4)

- **Activity:** Code generation and test generation.
- **Prompt:**
  > Approved
  
  (The approved design defined the scope: implementation plan steps 1–4.)
- **AI response (summary):** Generated, following the `java-sonar-codegen`
  and `java-test-generation` skills:
  - `pom.xml` (Spring Boot 3.5.16, the latest 3.x patch on Maven Central when
    checked; web, validation, test; JaCoCo line coverage gate at 80%),
    `WaitlistApplication`, and `application.yml` with the seeded offering.
  - Domain: `LearnerId`, `OfferingId`, `CourseCode` (shared validation in
    `Identifiers`), `Learner`, the sealed result types `EnrollResult` and
    `DropResult`, `OfferingSnapshot`, the two not-found exceptions, and the
    `Offering` aggregate.
  - Tests: `OfferingTest` (23 cases covering every domain row in the
    criterion 6 test plan, with invariants I-1..I-3 checked in `@AfterEach`
    and at intermediate steps), `LearnerTest`, `IdentifiersTest`, and
    `ResultTypesTest`.
- **Changes made during AI self-review, before running anything:**
  - `Offering.enroll` was first sketched as six early returns. It was
    restructured into `existingRecord → prerequisiteFailure → admit` so the
    check order from DESIGN.md section 3 is visible in one expression, and to
    stay within the Sonar return-count limit (S1142).
  - Promotion first removed the waitlist head and then checked eligibility.
    It was changed to check before removing, so a lookup that throws leaves
    that learner on the waitlist.
  - The eligibility rule stays in the domain: `Offering.drop` receives a
    learner lookup function, not a yes/no eligibility check. Otherwise the
    caller would own the prerequisite rule.
  - A dead store in `existingRecord` (an initial value always overwritten,
    S1854) was removed after the first compile.
- **Defect found by verification:** The first `mvn verify` run had 15 errors,
  all thrown by the test helper `assertInvariants`: AssertJ's
  `doesNotContainAnyElementsOf` throws when given an empty list, and the
  waitlist is often empty. The production code was not at fault. Fixed by
  using `noneMatch(waitlist::contains)`.
- **Decision:** Accepted after the edits above.
- **Files informed:** `pom.xml`, `src/main/resources/application.yml`,
  `src/main/java/.../WaitlistApplication.java`,
  `src/main/java/.../domain/*`, `src/test/java/.../domain/*`.
- **Verification evidence:**

  | Check | Command | Result |
  |-------|---------|--------|
  | Sonar pre-filter | `bash .kiro/skills/java-sonar-codegen/scripts/sonar_precheck.sh src` | `clean (16 files)` |
  | Build, tests, coverage gate | `mvn -B verify` | 45 tests, 0 failures, 0 errors; `All coverage checks have been met`; `BUILD SUCCESS` |
  | Domain coverage | JaCoCo report (`target/site/jacoco/jacoco.csv`) | Line 100%, branch 97.1% |
  | Mutation 1: promotion ignores eligibility (`if (eligible \|\| head != null)`) | `mvn test -Dtest=OfferingTest` | 4 failures, all in the ineligible-at-head tests (D-3, A-2). Reverted. |
  | Mutation 2: capacity off by one (`enrolled.size() <= capacity`) | `mvn test -Dtest=OfferingTest` | 10 failures, including the I-1 invariant check and the full-capacity, FIFO, and re-enroll tests. Reverted. |
  | Mutations removed | `grep` for both mutated expressions, then `mvn -B verify` | No matches; 45 tests pass |

  The mutation runs show the tests detect wrong promotion and wrong capacity
  handling, not just that they execute the code.

## Entry 6 — Adapters, service, concurrency, config, REST layer, README (DESIGN.md steps 5–10)

- **Activity:** Code generation, test generation, documentation.
- **Prompt:**
  > yes

  (A reply to "Shall I continue?" covering implementation plan steps 5–10.)
- **AI response (summary):**
  - Ports `OfferingStore` and `LearnerDirectory`; adapters
    `InMemoryOfferingStore` (one fair `ReentrantLock` per offering, released
    in `finally`) and `InMemoryLearnerDirectory`.
  - `EnrollmentService` (plain Java), which runs each request, including the
    learner lookups, inside the offering lock.
  - `WaitlistProperties` (validated record; duplicate learner IDs rejected) and
    `WaitlistConfiguration` (seeds and wires the beans).
  - `EnrollmentController`, the response DTOs, the `Outcome` enum, and
    `GlobalExceptionHandler` (`ProblemDetail` with an `outcome` property).
  - Tests: `InMemoryOfferingStoreTest`, `InMemoryLearnerDirectoryTest`,
    `EnrollmentServiceTest`, `EnrollmentServiceConcurrencyTest`,
    `WaitlistConfigurationTest`, `EnrollmentApiTest`.
  - `README.md`; final pass on `DESIGN.md`.
- **Choices made during implementation, beyond the approved design:**

  | Choice | Reason | Status |
  |--------|--------|--------|
  | Port method named `withLock` instead of `update` | Reads also use it, so `update` would mislead | Edited DESIGN.md sections 5, 9, 14 to match |
  | `Identifiers.PATTERN_TEXT` made public | Web and config validation reuse the domain ID pattern, so the rule exists once | Accepted |
  | API tests use `@SpringBootTest` + MockMvc, not `@WebMvcTest` with a mocked service | Exercises the real wiring, seed data, and status mapping end to end. Cost: each test rebuilds the context (about 14 s for 17 tests) | Edited DESIGN.md section 15 to match |
  | No class-level `@Validated` on the controller | It would add an AOP proxy that throws `ConstraintViolationException` (500 unless mapped). Spring's built-in method validation raises `HandlerMethodValidationException`, which is mapped to 400 | Accepted; confirmed by `testInvalidPathIdentifierReturns400` |
  | `202 Accepted` for `WAITLISTED` | As in the approved design | Listed as a limitation |

- **Changes made after verification:**
  - The first `WaitlistConfigurationTest` asserted only `hasFailed()` for bad
    seed data. That would also pass if startup failed for an unrelated reason.
    Changed it to also assert that the failure names the offending property
    (`offering.capacity`, `learners[1].id`, "Duplicate learner id", and so on).
  - The pre-check flagged two field `@Autowired` fields in `EnrollmentApiTest`.
    Changed them to constructor injection.
- **Tool issue during verification:** A batch of `curl` calls against the
  running jar returned only the last response to the AI. The service state
  showed all requests had run. The smoke run was repeated with the output
  written to a file and then read in full.
- **Evidence-handling mistake, caught in the final check:** The first smoke
  output and service log were saved under `target/`. A later
  `mvn clean package` deleted them. The smoke run was repeated on a fresh start
  and saved under `docs/evidence/` so it ships with the submission. The results
  were identical.
- **Decision:** Accepted after the edits above.
- **Files informed:** `src/main/java/.../application/**`,
  `src/main/java/.../adapter/**`, `src/main/java/.../config/**`,
  `src/main/java/.../domain/Identifiers.java`, the matching test files,
  `README.md`, `docs/DESIGN.md`.
- **Verification evidence:**

  | Check | Command | Result |
  |-------|---------|--------|
  | Sonar pre-filter | `bash .kiro/skills/java-sonar-codegen/scripts/sonar_precheck.sh src` | `clean (35 files)` after the fix above |
  | Build, tests, coverage gate | `mvn -B clean verify` | 86 tests, 0 failures, 0 errors; coverage gate met; `BUILD SUCCESS` |
  | Coverage by package | JaCoCo CSV | domain 100% line / 97.1% branch; application 100/100; memory adapters 100/100; web 100/100; config 100% / 91.7%; total 99.4% line. Only `WaitlistApplication.main` is uncovered |
  | Mutation 3: lock removed (lock acquired then released immediately, before the operation) | `mvn test -Dtest='EnrollmentServiceConcurrencyTest,InMemoryOfferingStoreTest'`, 3 runs | Every run failed all 4 concurrency tests (mutual exclusion, capacity under contention, concurrent duplicates, concurrent FIFO drops) with wrong counts, `NullPointerException`, or `NoSuchElementException` from the unsynchronized `LinkedHashSet`. Reverted |
  | Mutation 4: a duplicate enroll reported as `201 ENROLLED` | `mvn test -Dtest=EnrollmentApiTest` | `testDuplicateEnrollRequestsReturn200WithoutNewRecords` failed (`expected 200 but was 201`). Reverted |
  | Mutations removed | `grep` for the mutated code, then `mvn -B clean verify` | No matches; 86 tests pass |
  | Running service | `java -jar target/course-waitlist-0.1.0-SNAPSHOT.jar`, then 11 `curl` requests ([evidence/smoke-run.txt](evidence/smoke-run.txt)) | Each status and body matches DESIGN.md section 3 (201, 202, 200 ALREADY_WAITLISTED, 422, 200 DROPPED with promotion, 200 NOT_ENROLLED, 404, 400); final state `enrolled [L-002, L-003]` |
  | Log lines | [evidence/service-log.txt](evidence/service-log.txt) | One INFO line per state change: enrolled, waitlisted, dropped, promoted; IDs only |
  | README commands | `mvn clean package`, `java -jar … --server.port=9090`, `mvn spring-boot:run` | All three served `GET /offerings/CS201-2026F` with HTTP 200; processes stopped afterwards |

## Final check against the brief

Done before reporting completion, against
`.kiro/steering/assessment-context.md`.

### Acceptance criteria

| # | Criterion | Evidence |
|---|-----------|----------|
| 1 | Enroll succeeds with capacity and prerequisites | `OfferingTest.testEnrollsEligibleLearnerWhenSeatIsFree`; API 201 in smoke run |
| 2 | Full: eligible learner waitlisted once; duplicates create no records | `testWaitlistsEligibleLearnersInJoinOrderWhenFull`, `testRepeatedEnrollWhile*`, `testConcurrentDuplicateEnrollsCreateOneRecord`; API 202 then 200 `ALREADY_WAITLISTED` |
| 3 | Drop opens a seat and promotes the earliest eligible learner | `testDropPromotesWaitlistedLearnersInFifoOrder`, `testConcurrentDropsPromoteInFifoOrder`; API `promotedLearnerId: L-003` |
| 4 | Ineligible learner never enrolled or promoted; skip-and-remove applied consistently | `testRejectsLearnerWithoutPrerequisite`, `testDropRemovesIneligibleHeadAndPromotesNext`, `testDropWithOnlyIneligibleWaitersLeavesSeatFree`, `testPromotionReadsCurrentLearnerRecord`; mutation 1 caught |
| 5 | Invalid requests and duplicate drops give clear results with no state change | `testRepeatedDropIsNoOpAndPromotesNobody`, `testDropOfLearnerWithNoRecordIsNoOp`, API 400/404 tests with state checked afterwards |
| 6 | Tests cover all listed scenarios, plus concurrency | README "Where each required scenario is tested"; `EnrollmentServiceConcurrencyTest`; mutation 3 caught |
| 7 | AI use recorded with prompts, decisions, and verification | This file, Entries 1–6 |

### Deliverables and candidate expectations

| Item | Where | Status |
|------|-------|--------|
| Runnable code + focused tests | `src/`, `pom.xml` | `mvn clean verify` green; jar runs |
| Design specification | `docs/DESIGN.md` | Covers boundary, API, invariants, promotion rule, concurrency, assumptions, trade-offs |
| AI prompt and use record | `docs/AI_USAGE.md` | Entries 1–6 with verification evidence |
| Setup and run instructions | `README.md` | Every command was executed |
| Limitations and follow-up risks | `docs/DESIGN.md` section 12; summary in `README.md` | 7 design-time and 7 implementation-time items |
| Prerequisite and waitlist assumptions stated | `docs/DESIGN.md` section 2; README policy paragraph | Matched by tests |
| No frontend, catalog, payments, identity, database, or policy engine | Whole repo | None present. Dependencies: Spring web, validation, the configuration processor (optional, compile-time), and Spring test |
| No credentials or sensitive data in prompts | This file | Prompts contain only task instructions; learner IDs are synthetic |

### Not verified

- No SonarQube server analysis was run. `sonar_precheck.sh` is a regex-based
  pre-filter, not a substitute.
- The Phenom-Java rule files the skills reference (`java-style`, `java-bugs`,
  and others) are not in the workspace, so only the hot rules inside the
  `java-sonar-codegen` skill were applied.
- No rubric file (`Course-Registration-Rubric-Score.md`) was available to
  score against.

## Entry 7 — Prepare the public GitHub repository

- **Activity:** Repository setup.
- **Prompts (account details omitted):** the candidate gave their GitHub
  account, then answered the AI's questions: public repository; set the commit
  email in this repository's local git config only; exclude `.kiro/`,
  `.cursor/`, `target/`, and the assessment PDF; remove the README's pre-check
  command (it pointed into the excluded `.kiro/`); push over HTTPS.
- **AI response (summary):** Found no git repository, a global git identity
  using a work email, no GitHub CLI, and only a Bitbucket SSH key. Asked before
  publishing anything. Then ran `git init`, set the commit email locally (global
  config unchanged), added `.gitignore`, removed the README pre-check command,
  added a note at the top of this file that the AI tooling folders and the PDF
  are not in the repository, made the commit, and added the remote. The push
  is left to the candidate so the access token never passes through the AI.
- **Decision:** Accepted.
- **Files informed:** `.gitignore`, `README.md`, `docs/AI_USAGE.md`.
- **Verification:** `git add --dry-run` and `git ls-files` listings reviewed
  before the commit; `git config --global user.email` confirmed unchanged.
