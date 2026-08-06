# Rejestr refinementów

## Wymagane metadane

```text
Task-ID: EMP-000
Stan-Refinementu: DRAFT | ACCEPTED
Właściciel: ...
Zaakceptował: ...
Data-Akceptacji: YYYY-MM-DD | N/A
Implementation-Allowed: YES | NO
```

Dokument ze stanem `DRAFT` nie zezwala na implementację.

## Aktualne refinementy

- [EMP-001 — kontrakt rozwiązania serwisu kuponowego](EMP-001.md) — `ACCEPTED`
- [EMP-001 — podsumowanie](EMP-001-summary.md)
- [EMP-001 — review checklist](EMP-001-review-checklist.md) — `PASS`
- [EMP-003 — tworzenie kuponu](EMP-003.md) — `ACCEPTED`
- [EMP-003 — podsumowanie](EMP-003-summary.md)
- [EMP-003 — review checklist](EMP-003-review-checklist.md) — `PASS`
- [EMP-004 — transakcyjne wykorzystanie kuponu](EMP-004.md) — `DRAFT`
- [EMP-004 — podsumowanie](EMP-004-summary.md)
- [EMP-004 — review checklist](EMP-004-review-checklist.md) — `NOT_STARTED`
- [EMP-006 — Client IP i provider-neutral GeoIP](EMP-006.md) — `ACCEPTED`
- [EMP-006 — podsumowanie](EMP-006-summary.md)
- [EMP-006 — review checklist](EMP-006-review-checklist.md) — `PASS`
- [EMP-007 — OpenAPI, Swagger UI i Javadoc](EMP-007.md) — `ACCEPTED`
- [EMP-007 — podsumowanie](EMP-007-summary.md)
- [EMP-007 — review checklist](EMP-007-review-checklist.md) — `PASS`

## Pokrycie zadań

`EMP-001` zamraża kontrakt całego MVP. `EMP-003` uszczegóławia tworzenie kuponu, `EMP-007` tester-facing OpenAPI i zasady dokumentacji kodu, a zaakceptowany `EMP-006` definiuje granicę zaufania Client IP oraz provider-neutral GeoIP. `EMP-004` pozostaje zablokowane do ukończenia implementacji EMP-006. Zasady amendmentu opisuje [proces refinementu](../refinement-process.md).
