# Backlog

Ten plik jest jedynym źródłem prawdy dla identyfikatorów, priorytetów i statusów zadań.

| ID | Parent | Priority | Status | Refinement | Zadanie | Docelowy dowód |
|---|---|---:|---|---|---|---|
| EMP-000 | — | P0 | DONE_AND_VERIFIED | EMP-001 | Ustanowić lekkie governance dokumentacji | `make docs-check`, `make verify` |
| EMP-001 | — | P0 | DONE_AND_VERIFIED | EMP-001 | Zamrozić kompletny kontrakt rozwiązania kuponowego | accepted refinement + review checklist |
| EMP-002 | EMP-001 | P0 | DONE_AND_VERIFIED | EMP-001 | Bootstrap Java/Spring Boot/PostgreSQL/Flyway/Maven | `make verify`, runtime Compose i eksport źródeł |
| EMP-003 | EMP-001 | P0 | DONE_AND_VERIFIED | EMP-003 | Tworzenie kuponu i case-insensitive uniqueness | `make verify`, runtime HTTP i exact-count concurrency test |
| EMP-004 | EMP-001 | P0 | REFINEMENT | EMP-004 | Transakcyjne wykorzystanie kuponu | accepted EMP-004 refinement po gotowym GeoIP/API |
| EMP-005 | EMP-001 | P1 | PLANNED | EMP-001 | Jedno użycie kuponu przez użytkownika | unique constraint + concurrency test |
| EMP-006 | EMP-001 | P0 | DONE_AND_VERIFIED | EMP-006 | Client IP i provider-neutral GeoIP | accepted EMP-006 refinement + implementation evidence + `make emp006-check` |
| EMP-007 | EMP-001 | P0 | DONE_AND_VERIFIED | EMP-007 | Stabilny error contract, OpenAPI, Swagger UI i Javadoc | UI/YAML HTTP test + DocLint + `make verify` |
| EMP-008 | EMP-001 | P0 | PLANNED | EMP-001 | Testy jednostkowe i integracyjne | JaCoCo + Testcontainers |
| EMP-009 | EMP-001 | P0 | PLANNED | EMP-001 | Deterministyczne testy współbieżności | exact-success-count evidence |
| EMP-010 | EMP-001 | P1 | PLANNED | EMP-001 | CI, delivery hardening i podstawowe metryki | green CI + reproducible delivery gate |
| EMP-011 | EMP-001 | P0 | PLANNED | EMP-001 | Finalny review, README i closeout | final `make verify` + public repo |

## Reguły przejścia

- `EMP-002` i `EMP-003` są `DONE_AND_VERIFIED`.
- `EMP-007` jest `DONE_AND_VERIFIED` na podstawie lokalnego pełnego gate.
- `EMP-006` jest `DONE_AND_VERIFIED` po pełnym `make verify` z izolowanym Docker smoke.
- `EMP-004` jest `REFINEMENT`; jego implementacja jest niedozwolona do czasu accepted własnego refinementu.
- Zmiana contract boundary wymaga amendmentu `EMP-001`.
- `DONE_AND_VERIFIED` wymaga dowodów wskazanych w ostatniej kolumnie.
