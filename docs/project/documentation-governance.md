# Governance dokumentacji

## Dokumenty źródłowe

- [current-status.md](current-status.md) — jedyne miejsce z bieżącą oceną stanu;
- [backlog.md](backlog.md) — jedyny rejestr task ID, priorytetów i statusów;
- [refinements/](refinements/README.md) — zaakceptowane kontrakty realizacyjne;
- [decision-log.md](decision-log.md) i [ADR-y](../adr/README.md) — trwałe decyzje;
- [risk-register.md](risk-register.md) — ryzyka i mitigacje;
- [lessons-learned.md](lessons-learned.md) — tylko wnioski potwierdzone wykonaniem;
- [CHANGELOG.md](../../CHANGELOG.md) — skrócony rejestr zmian;
- [release-history.md](release-history.md) — evidence checkpointów;
- [definition-of-ready-and-done.md](definition-of-ready-and-done.md) — warunki wejścia i zamknięcia.

## Słownik statusów

- `PLANNED` — zadanie opisane, ale niegotowe do rozpoczęcia;
- `REFINEMENT` — kontrakt jest przygotowywany lub zmieniany;
- `READY` — istnieje zaakceptowany refinement i brak blokera;
- `IN_PROGRESS` — implementacja trwa;
- `BLOCKED` — praca zatrzymana przez jawny bloker;
- `DONE` — zakres zaimplementowany, lecz pełny evidence może być otwarty;
- `DONE_AND_VERIFIED` — wszystkie bramki i dokumenty są zielone.

## Lekki model refinementu

Małe zadanie rekrutacyjne nie wymaga osobnego wielostronicowego refinementu dla każdej technicznej czynności. Zaakceptowany `EMP-001` pokrywa bounded tasks `EMP-002`–`EMP-011`, ponieważ:

- zadania są jawnie wymienione w planie realizacji;
- ich API, dane, transakcje, ryzyka i testy są zamrożone;
- backlog wskazuje `EMP-001` w kolumnie `Refinement`.

Osobny dokument lub amendment jest obowiązkowy, gdy pojawia się zmiana:

- publicznego API;
- modelu danych lub migracji;
- algorytmu współbieżności;
- reguł błędów;
- dostawcy i kontraktu GeoIP;
- przetwarzania danych osobowych;
- stosu technologicznego;
- zakresu MVP.

## Aktualizacja po zadaniu

1. Zmień status w backlogu.
2. Uaktualnij current status.
3. Dopisz changelog i release history.
4. Dodaj lesson wyłącznie po potwierdzeniu.
5. Zmień ryzyko, decyzję lub ADR, jeśli wpływ jest trwały.
6. Uruchom `make docs-check` i `make verify`.
7. Commituj dokumentację razem z kodem.

## Zasady jakości

- plan nie jest faktem wykonanym;
- `DONE` wymaga dowodu w teście, logu lub commicie;
- `DONE_AND_VERIFIED` wymaga pełnej bramki;
- linki względne prowadzą do istniejących plików;
- każdy dokument w `docs/` znajduje się w [indeksie](../DOCUMENTATION_INDEX.md);
- identyfikatory backlogu, decyzji i ryzyk są unikalne;
- dane stanu używają formatu `YYYY-MM-DD`;
- dokumentacja nie zawiera sekretów ani surowych danych użytkownika.
