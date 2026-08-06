# Podsumowanie refinementu EMP-001

## Decyzja

Refinement został zaakceptowany 2026-08-06. Implementacja MVP jest dozwolona.

## Zakres MVP

- tworzenie kuponu;
- case-insensitive unikalność;
- wykorzystanie kuponu z limitem;
- ograniczenie kraju na podstawie IP;
- jedno użycie przez użytkownika;
- trwały zapis PostgreSQL;
- stabilne błędy REST;
- testy jednostkowe, integracyjne, API i concurrency;
- Docker Compose, CI i dokumentacja uruchomienia.

## Najważniejsze decyzje

1. Java 21, Spring Boot 3.5.16, Maven, PostgreSQL 18.
2. Modularny monolit i Spring JDBC.
3. Kod canonicalizowany do `normalized_code` i chroniony unique constraintem.
4. Wykorzystanie serializowane na rekordzie przez `SELECT FOR UPDATE`.
5. Jedno użycie chronione przez `UNIQUE(coupon_id,user_id)`.
6. GeoIP wykonywane poza transakcją przez wymienny adapter.
7. Surowy IP nie jest zapisywany.
8. Błąd dostawcy GeoIP zwraca 503, nie fałszywe 403.
9. Lokalny stub działa wyłącznie w profilach `local`/`test`, bez publicznego bypassu.
10. Testy integracyjne używają PostgreSQL Testcontainers, nie H2.
11. Redis, Kafka, mikroserwisy i Kubernetes są poza zakresem.

## Krytyczny dowód końcowy

Dla kuponu `maxUses=10`, przy 100 równoległych requestach różnych użytkowników:

- dokładnie 10 odpowiedzi sukcesu;
- dokładnie 90 odpowiedzi `COUPON_EXHAUSTED`;
- `current_uses = 10`;
- liczba redemption records = 10;
- brak przekroczenia constraintu.

## Następny krok

`EMP-002 — bootstrap aplikacji`, status `READY`.

Pełny kontrakt: [EMP-001.md](EMP-001.md).
