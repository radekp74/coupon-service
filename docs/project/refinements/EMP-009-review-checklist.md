# Review checklist EMP-009

- [x] EMP-009 nie dodaje funkcji biznesowej ani nie zmienia API.
- [x] Zdefiniowano AC-01–AC-14 z exact counts, DB state i deterministycznością.
- [x] Mapa wskazuje istniejące klasy/metody zamiast kopiować evidence.
- [x] Rozdzielono ownership EMP-003, EMP-004 i EMP-009.
- [x] Wymagane są PostgreSQL/Testcontainers, brak H2/publicznej sieci/`Thread.sleep`.
- [x] Wymagane są latch, timeout, wszystkie futures i cleanup executora.
- [x] Rollback pozostaje evidence EMP-004.
- [x] JaCoCo i Javadoc warnings pozostają scope EMP-008.
- [x] Zidentyfikowano luki AC-02, AC-03 i AC-07.
- [x] Właściciel zaakceptował `Implementation-Allowed: YES` wyłącznie dla trzech asercji i implementacyjnego checkera.
- [x] EMP-009 nadal nie może zostać zamknięte: evidence jest `PARTIAL`, implementation `NOT_STARTED`.

## Rekomendacja

Historyczna rekomendacja: `REJECT` dla closeoutu EMP-009 bez zmian testów Java. Właściciel zaakceptował refinement 2026-08-07, zachowując `EVIDENCE_PARTIAL`; następny checkpoint może wyłącznie uzupełnić trzy exact assertions i dodać implementacyjny checker.
