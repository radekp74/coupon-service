# Review checklist EMP-001

- **Data review:** 2026-08-06
- **Wynik:** PASS
- **Reviewer:** kandydat / właściciel rozwiązania (self-review)

## Wymagania i zakres

- [x] Wszystkie wymagania z treści zadania mają mapowanie traceability.
- [x] Zakres MVP jest jawny.
- [x] Elementy poza zakresem są jawne.
- [x] Opcjonalne jedno użycie przez użytkownika zostało świadomie włączone.
- [x] Nie dodano funkcji biznesowych niezwiązanych z zadaniem.

## Architektura i dane

- [x] Wybrano język, framework, build tool i bazę.
- [x] Uzasadniono modularny monolit.
- [x] Model danych ma constrainty integralności.
- [x] Case-insensitive uniqueness jest broniona przez bazę.
- [x] Canonicalizacja kodu jest jednoznaczna.
- [x] Granica transakcji jest jawna.
- [x] Kolejność błędów w transakcji jest jawna.
- [x] Hot coupon trade-off jest zapisany.

## GeoIP, bezpieczeństwo i prywatność

- [x] GeoIP ma port i wymienny adapter.
- [x] Timeouty są określone.
- [x] Brak retry w krytycznej ścieżce jest świadomą decyzją.
- [x] Awaria techniczna nie udaje błędu biznesowego.
- [x] Zaufanie do proxy jest ograniczone.
- [x] Surowy adres IP nie jest utrwalany ani logowany.
- [x] Lokalny stub jest ograniczony do profili `local`/`test`.
- [x] Publiczne API nie ma nagłówka pozwalającego narzucić kraj.
- [x] Darmowy provider i brak SLA są zapisane jako ryzyko.

## API i błędy

- [x] Endpointy i schema request/response są określone.
- [x] Statusy HTTP są określone.
- [x] Machine-readable error codes są stabilne.
- [x] Dane techniczne nie są ujawniane klientowi.

## Testy i operacje

- [x] Istnieje matryca unit/integration/API/concurrency.
- [x] Krytyczny exact-count test współbieżności jest mierzalny.
- [x] Test jednego użycia przez użytkownika jest mierzalny.
- [x] PostgreSQL Testcontainers jest obowiązkowy.
- [x] H2 jest wyłączone z integracyjnej ścieżki jakości.
- [x] Rollback i spójność licznika mają testy.
- [x] Finalny clean-checkout build ma kryterium akceptacji.

## Governance

- [x] Backlog wskazuje accepted refinement.
- [x] Current status nie przedstawia planu jako wykonania.
- [x] Ryzyka wysokie i krytyczne mają mitigację.
- [x] Brak otwartych pytań blokujących.
- [x] Zmiana contract boundary wymaga amendmentu.

## Decyzja review

Refinement jest kompletny, proporcjonalny do zadania i gotowy do implementacji.
