# Backlog

Ten plik jest jedynym źródłem prawdy dla identyfikatorów, priorytetów i statusów zadań.

| ID | Parent | Priority | Status | Refinement | Zadanie | Docelowy dowód |
|---|---|---:|---|---|---|---|
| EMP-000 | — | P0 | DONE_AND_VERIFIED | EMP-001 | Ustanowić lekkie governance dokumentacji | `make docs-check`, `make verify` |
| EMP-001 | — | P0 | DONE_AND_VERIFIED | EMP-001 | Zamrozić kompletny kontrakt rozwiązania kuponowego | accepted refinement + review checklist |
| EMP-002 | EMP-001 | P0 | DONE_AND_VERIFIED | EMP-001 | Bootstrap Java/Spring Boot/PostgreSQL/Flyway/Maven | `make verify`, runtime Compose i eksport źródeł |
| EMP-003 | EMP-001 | P0 | READY | EMP-001 | Tworzenie kuponu i case-insensitive uniqueness | API + concurrent duplicate tests |
| EMP-004 | EMP-001 | P0 | PLANNED | EMP-001 | Transakcyjne wykorzystanie kuponu | integration + rollback tests |
| EMP-005 | EMP-001 | P1 | PLANNED | EMP-001 | Jedno użycie kuponu przez użytkownika | unique constraint + concurrency test |
| EMP-006 | EMP-001 | P0 | PLANNED | EMP-001 | Client IP i provider-neutral GeoIP | WireMock + timeout/failure tests |
| EMP-007 | EMP-001 | P0 | PLANNED | EMP-001 | Stabilny error contract i OpenAPI | schema + negative API tests |
| EMP-008 | EMP-001 | P0 | PLANNED | EMP-001 | Testy jednostkowe i integracyjne | JaCoCo + Testcontainers |
| EMP-009 | EMP-001 | P0 | PLANNED | EMP-001 | Deterministyczne testy współbieżności | exact-success-count evidence |
| EMP-010 | EMP-001 | P1 | PLANNED | EMP-001 | CI, delivery hardening i podstawowe metryki | green CI + reproducible delivery gate |
| EMP-011 | EMP-001 | P0 | PLANNED | EMP-001 | Finalny review, README i closeout | final `make verify` + public repo |

## Reguły przejścia

- `EMP-002` jest `DONE_AND_VERIFIED`: pełny gate Maven/Testcontainers i Docker Compose przeszedł lokalnie 2026-08-06.
- `EMP-003` jest `READY`; kolejne zadania pozostają w kolejności backlogu i bez zmiany kontraktu.
- Zmiana contract boundary wymaga amendmentu `EMP-001`.
- `DONE_AND_VERIFIED` wymaga dowodów wskazanych w ostatniej kolumnie.
