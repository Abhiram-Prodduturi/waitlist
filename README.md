# Course Waitlist

Enrollment, drop, and FIFO waitlist promotion for **one course offering**,
built as a small Spring Boot 3 REST service with an in-memory store. No starter
project was supplied, so this uses the minimal-service fallback from the
assessment brief.

| Document | Contents |
|----------|----------|
| [docs/DESIGN.md](docs/DESIGN.md) | Design specification: service boundary, API and outcomes, invariants, promotion rule, concurrency, assumptions, trade-offs, and **limitations and follow-up risks (section 12)** |
| [docs/AI_USAGE.md](docs/AI_USAGE.md) | AI prompt and use record, with accepted/edited/rejected decisions and verification evidence |

## Policy in one paragraph

A learner is eligible if they have completed every prerequisite course of the
offering (CS101 in the seed data). Eligibility is checked when they request
enrollment and again when they reach the front of the waitlist. An ineligible
learner at the front is **removed from the waitlist** and the next learner is
considered. A learner holds at most one active record, enrolled or waitlisted.
Repeated requests are no-ops with an explicit outcome, and a repeated drop
never frees a second seat. Full rationale: DESIGN.md section 2.

## Requirements

- Java 21
- Maven 3.8 or newer (tested with 3.8.7)
- Network access to Maven Central for the first build

No database, Docker, or other services are needed.

## Build and test

```bash
mvn clean verify
```

This compiles, runs all 86 tests, and enforces the JaCoCo line-coverage gate
(80%; current coverage is 99.4%). Coverage report:
`target/site/jacoco/index.html`.

Run one test class:

```bash
mvn test -Dtest=OfferingTest
mvn test -Dtest=EnrollmentServiceConcurrencyTest
```

### Where each required scenario is tested

| Scenario (acceptance criterion 6) | Test |
|-----------------------------------|------|
| Available capacity | `OfferingTest.testEnrollsEligibleLearnerWhenSeatIsFree`, `EnrollmentApiTest.testEnrollWithFreeSeatReturns201` |
| Full capacity | `OfferingTest.testWaitlistsEligibleLearnersInJoinOrderWhenFull`, `EnrollmentApiTest.testEnrollWhenFullReturns202WithPosition` |
| Duplicate requests | `OfferingTest.testRepeatedEnrollWhile*`, `EnrollmentApiTest.testDuplicateEnrollRequestsReturn200WithoutNewRecords` |
| FIFO promotion | `OfferingTest.testDropPromotesWaitlistedLearnersInFifoOrder`, `EnrollmentApiTest.testDropPromotesEarliestWaitlistedLearner` |
| Prerequisite failure | `OfferingTest.testRejectsLearnerWithoutPrerequisite`, `EnrollmentApiTest.testPrerequisiteFailureReturns422ProblemDetail` |
| Ineligible learner at the head (skip and remove) | `OfferingTest.testDropRemovesIneligibleHeadAndPromotesNext`, `...WithOnlyIneligibleWaitersLeavesSeatFree`, `EnrollmentApiTest.testDropRemovesIneligibleHeadAndPromotesNext` |
| Invalid or repeated drops | `OfferingTest.testRepeatedDropIsNoOpAndPromotesNobody`, `...DropOfLearnerWithNoRecordIsNoOp`, `EnrollmentApiTest.testRepeatedDropReturnsNotEnrolledAndPromotesNobody`, `...UnknownLearnerReturns404`, `...InvalidPathIdentifierReturns400` |
| Concurrency | `EnrollmentServiceConcurrencyTest` (capacity under contention, concurrent duplicates, concurrent drops), `InMemoryOfferingStoreTest.testNeverRunsTwoOperationsOnTheSameOfferingAtOnce` |

`OfferingTest` checks invariants I-1 to I-3 (DESIGN.md section 6) after every
test.

## Run the service

```bash
mvn clean package
java -jar target/course-waitlist-0.1.0-SNAPSHOT.jar
```

Or, without building the jar first: `mvn spring-boot:run`. The service listens
on port 8080; use `--server.port=9090` to change it. State is in memory and
resets on restart.

### Seed data (`src/main/resources/application.yml`)

| Item | Value |
|------|-------|
| Offering | `CS201-2026F`, 2 seats, prerequisite `CS101` |
| Learners with CS101 | `L-001`, `L-002`, `L-003`, `L-004` |
| Learner without CS101 | `L-005` |

Change capacity, prerequisites, or learners there. Invalid values (capacity
below 1, malformed IDs, duplicate learner IDs) stop the service at startup.

### Example session

The responses below were captured from the running jar. The full transcript
is in [docs/evidence/smoke-run.txt](docs/evidence/smoke-run.txt), and the
matching log lines are in
[docs/evidence/service-log.txt](docs/evidence/service-log.txt).

```bash
B=http://localhost:8080/offerings/CS201-2026F

curl -s -X POST $B/enrollments -H 'Content-Type: application/json' -d '{"learnerId":"L-001"}'
# 201 {"outcome":"ENROLLED","learnerId":"L-001",...}

curl -s -X POST $B/enrollments -H 'Content-Type: application/json' -d '{"learnerId":"L-002"}'
# 201 {"outcome":"ENROLLED","learnerId":"L-002",...}

curl -s -X POST $B/enrollments -H 'Content-Type: application/json' -d '{"learnerId":"L-003"}'
# 202 {"outcome":"WAITLISTED","learnerId":"L-003","waitlistPosition":1,...}

curl -s -X POST $B/enrollments -H 'Content-Type: application/json' -d '{"learnerId":"L-003"}'
# 200 {"outcome":"ALREADY_WAITLISTED","learnerId":"L-003","waitlistPosition":1,...}

curl -s -X POST $B/enrollments -H 'Content-Type: application/json' -d '{"learnerId":"L-005"}'
# 422 {"title":"Prerequisites not met","outcome":"PREREQUISITES_NOT_MET","missingPrerequisites":["CS101"],...}

curl -s -X DELETE $B/enrollments/L-001
# 200 {"outcome":"DROPPED","learnerId":"L-001","promotedLearnerId":"L-003","removedIneligibleLearnerIds":[]}

curl -s -X DELETE $B/enrollments/L-001
# 200 {"outcome":"NOT_ENROLLED","learnerId":"L-001",...}

curl -s -X DELETE $B/enrollments/L-999
# 404 {"title":"Learner not found","outcome":"LEARNER_NOT_FOUND",...}

curl -s $B
# 200 {"offeringId":"CS201-2026F","capacity":2,"seatsAvailable":0,"prerequisites":["CS101"],
#      "enrolled":["L-002","L-003"],"waitlist":[]}
```

Every outcome, its status code, and its body are listed in DESIGN.md
section 3.

## API summary

| Method | Path | Body |
|--------|------|------|
| `POST` | `/offerings/{offeringId}/enrollments` | `{"learnerId": "L-001"}` |
| `DELETE` | `/offerings/{offeringId}/enrollments/{learnerId}` | none |
| `GET` | `/offerings/{offeringId}` | none |

## Limitations

The full list is in DESIGN.md section 12. The main ones:

- In-memory state only; lost on restart; one JVM (the lock does not protect
  multiple instances).
- No authentication: any caller can enroll or drop any learner ID.
- A learner removed from the waitlist as ineligible loses their place, even if
  the problem was temporary.
- Enrolled learners are not re-checked if a completion is revoked.
- There is no endpoint to change completed courses, so "ineligible at the head
  of the waitlist" is only reachable from tests.
- Promotion does not roll back if a learner lookup throws; this cannot happen
  with the in-memory directory but would matter with a remote one.

## Project layout

```
src/main/java/com/example/registration/waitlist
├── domain            Offering rules, value objects, result types (no Spring)
├── application       EnrollmentService and the outbound ports
├── adapter/out/memory  In-memory offering store (per-offering lock) and learner directory
├── adapter/in/web    REST controller, DTOs, error mapping
└── config            Seed-data binding and bean wiring
```
