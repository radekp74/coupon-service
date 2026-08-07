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
- [EMP-004 — transakcyjne wykorzystanie kuponu](EMP-004.md) — `ACCEPTED`
- [EMP-004 — podsumowanie](EMP-004-summary.md)
- [EMP-004 — review checklist](EMP-004-review-checklist.md) — `PASS`
- [EMP-006 — Client IP i provider-neutral GeoIP](EMP-006.md) — `ACCEPTED`
- [EMP-006 — podsumowanie](EMP-006-summary.md)
- [EMP-006 — review checklist](EMP-006-review-checklist.md) — `PASS`
- [EMP-007 — OpenAPI, Swagger UI i Javadoc](EMP-007.md) — `ACCEPTED`
- [EMP-007 — podsumowanie](EMP-007-summary.md)
- [EMP-007 — review checklist](EMP-007-review-checklist.md) — `PASS`
- [EMP-008 — coverage, JaCoCo i quality evidence](EMP-008.md) — `ACCEPTED`, implementation `DONE_AND_VERIFIED`
- [EMP-008 — podsumowanie](EMP-008-summary.md)
- [EMP-008 — review checklist](EMP-008-review-checklist.md) — `ACCEPTED`
- [EMP-009 — deterministyczne concurrency evidence](EMP-009.md) — `ACCEPTED`, implementation `DONE_AND_VERIFIED`
- [EMP-009 — podsumowanie](EMP-009-summary.md)
- [EMP-009 — review checklist](EMP-009-review-checklist.md) — `PASS`
- [EMP-010 — CI, delivery hardening i obserwowalność](EMP-010.md) — `ACCEPTED`, `DONE_AND_VERIFIED`
- [EMP-010 — podsumowanie](EMP-010-summary.md)
- [EMP-010 — review checklist](EMP-010-review-checklist.md) — `PASS`
- [EMP-011 — finalny review, README i closeout](EMP-011.md) — `ACCEPTED`, `DONE_AND_VERIFIED`
- [EMP-011 — podsumowanie](EMP-011-summary.md)
- [EMP-011 — review checklist](EMP-011-review-checklist.md) — `PASS`

## Pokrycie zadań

`EMP-001` zamraża kontrakt całego MVP i zawiera amendment UserId dla EMP-004. EMP-003 i EMP-004 są zweryfikowanymi właścicielami implementacji create/redemption. EMP-009 sformalizował concurrency evidence; EMP-008 jest zakończonym i zweryfikowanym quality gate z JaCoCo oraz measured evidence. EMP-010 jest `DONE_AND_VERIFIED` z measured CI/delivery/observability evidence. EMP-011 jest `DONE_AND_VERIFIED`; final review i public repo evidence są `MEASURED_AND_VERIFIED`, bez zmian funkcjonalnych. Zasady amendmentu opisuje [proces refinementu](../refinement-process.md).
