# Backlog

Ten plik jest jedynym źródłem prawdy dla identyfikatorów, priorytetów i statusów zadań.

| ID | Parent | Priority | Status | Refinement | Zadanie | Docelowy dowód |
|---|---|---:|---|---|---|---|
| EMP-000 | — | P0 | DONE_AND_VERIFIED | EMP-001 | Ustanowić lekkie governance dokumentacji | `make docs-check`, `make verify` |
| EMP-001 | — | P0 | DONE_AND_VERIFIED | EMP-001 | Zamrozić kompletny kontrakt rozwiązania kuponowego | accepted refinement + review checklist |
| EMP-002 | EMP-001 | P0 | DONE_AND_VERIFIED | EMP-001 | Bootstrap Java/Spring Boot/PostgreSQL/Flyway/Maven | `make verify`, runtime Compose i eksport źródeł |
| EMP-003 | EMP-001 | P0 | DONE_AND_VERIFIED | EMP-003 | Tworzenie kuponu i case-insensitive uniqueness | `make verify`, runtime HTTP i exact-count concurrency test |
| EMP-004 | EMP-001 | P0 | DONE_AND_VERIFIED | EMP-004 | Transakcyjne wykorzystanie kuponu — verification remediation completed | pełny `make verify`, PostgreSQL rollback i exact-count concurrency evidence |
| EMP-005 | EMP-004 | P1 | DONE | EMP-004 | Jedno użycie kuponu przez użytkownika — `MERGED_INTO_EMP-004` | implementation i evidence należą do EMP-004 |
| EMP-006 | EMP-001 | P0 | DONE_AND_VERIFIED | EMP-006 | Client IP i provider-neutral GeoIP | accepted EMP-006 refinement + implementation evidence + `make emp006-check` |
| EMP-007 | EMP-001 | P0 | DONE_AND_VERIFIED | EMP-007 | Stabilny error contract, OpenAPI, Swagger UI i Javadoc | UI/YAML HTTP test + DocLint + `make verify` |
| EMP-008 | EMP-001 | P0 | DONE_AND_VERIFIED | EMP-008 | Coverage, JaCoCo, test completeness i quality evidence | JaCoCo `verify` gate + measured report checker + manual review + full local gate |
| EMP-009 | EMP-001 | P0 | DONE_AND_VERIFIED | EMP-009 | Deterministyczne testy współbieżności — evidence `COMPLETE` | `make emp009-check` + pełny Maven/Testcontainers/Docker gate |
| EMP-010 | EMP-001 | P1 | DONE_AND_VERIFIED | EMP-010 | CI, delivery hardening i podstawowe metryki | accepted EMP-010 refinement + local gate + green CI #2 on `35fa7c7` + reproducible delivery gate |
| EMP-011 | EMP-001 | P0 | READY | EMP-011 | Finalny review, README i closeout | accepted EMP-011 refinement + final `make verify` + delivery + green CI + public repo |

## Reguły przejścia

- `EMP-002` i `EMP-003` są `DONE_AND_VERIFIED`.
- `EMP-007` jest `DONE_AND_VERIFIED` na podstawie lokalnego pełnego gate.
- `EMP-006` jest `DONE_AND_VERIFIED` po pełnym `make verify` z izolowanym Docker smoke.
- `EMP-004` jest `DONE_AND_VERIFIED`; refinement jest `ACCEPTED`, a implementation i verification remediation mają pełne lokalne evidence.
- `EMP-005` ma disposition `MERGED_INTO_EMP-004`; user-once pozostaje obowiązkowym invariantem i evidence EMP-004.
- `EMP-009` jest `DONE_AND_VERIFIED`; refinement jest `ACCEPTED`, implementation `DONE_AND_VERIFIED`, a evidence `COMPLETE` po exact assertions, checkerze i pełnym gate.
- `EMP-008` jest `DONE_AND_VERIFIED`; refinement jest `ACCEPTED`, implementation `DONE_AND_VERIFIED`, `Implementation-Allowed: YES`, a coverage evidence `MEASURED_AND_VERIFIED` po pełnym JaCoCo/Maven/Docker gate.
- `EMP-010` jest `DONE_AND_VERIFIED`; refinement `ACCEPTED`, `Implementation-Allowed: YES`, implementation `DONE_AND_VERIFIED`, a CI/delivery/observability evidence są `MEASURED_AND_VERIFIED` po lokalnym canonical gate, reproducible delivery i zielonym GitHub Actions `CI #2` dla `35fa7c7`.
- Zmiana contract boundary wymaga amendmentu `EMP-001`.
- `DONE_AND_VERIFIED` wymaga dowodów wskazanych w ostatniej kolumnie.
