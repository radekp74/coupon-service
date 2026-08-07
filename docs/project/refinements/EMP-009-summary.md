# Podsumowanie refinementu EMP-009

- task: `EMP-009`; status: `READY`; stan: `ACCEPTED`;
- implementation: `NOT_STARTED`; implementation allowed: `YES`; evidence: `PARTIAL`;
- cel: formalny standard deterministycznego concurrency evidence, nie nowa funkcja;
- evidence reuse: EMP-003 create oraz EMP-004 redemption zachowują własność implementacji;
- wynik review: `EVIDENCE_PARTIAL`.

Istniejące Testcontainers/latch/future/cleanup evidence spełnia większość kontraktu, w tym concurrent create 24 prób, 3 × 100/10, same-user 1/19, last-slot 1/1 i row-lock niezależnych kuponów. Przed closeoutem pozostają trzy małe, mierzalne luki: kod 90 konfliktów 100/10, liczba unikalnych userId i zapis dokładnie jednego nowego użytkownika last-slot.

Właściciel Radosław Piątek zaakceptował 2026-08-07 wyłącznie mały checkpoint: trzy wskazane asercje w istniejących testach oraz przyszły `scripts/check_emp009.py` / `make emp009-check`. Rollback i mapping innego constraintu są evidence EMP-004, nie podstawowym scope EMP-009. JaCoCo i 42 ostrzeżenia Javadoc są poza zakresem; EMP-008 pozostaje `PLANNED`.
