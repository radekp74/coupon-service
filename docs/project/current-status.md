# Aktualny status projektu

- **Data:** 2026-08-06
- **Termin:** 2026-08-10, koniec dnia
- **Faza:** `BOOTSTRAP_DONE_AND_VERIFIED`
- **Active task:** `EMP-003`
- **Accepted refinement:** `EMP-001`
- **Implementation allowed:** `YES`
- **Kod aplikacji:** `BOOTSTRAP_DONE_AND_VERIFIED`
- **Runtime verification:** `LOCAL_DOCKER_GATE_PASS`

## Ukończone i zweryfikowane

- wymagania zadania zostały rozłożone na mierzalny backlog;
- ustanowiono źródła prawdy i słownik statusów;
- zaakceptowano kontrakt architektury, danych, API i błędów;
- rozstrzygnięto model współbieżności, jedno użycie przez użytkownika i GeoIP;
- dokumentacja przechodzi automatyczną walidację;
- Makefile ma jawny, nadpisywalny kontrakt ścieżki Docker dla lokalnego macOS;
- dostępny jest bezpieczny eksport źródeł przez `make export-source`;
- statyczny checker potwierdza obecność i spójność kontraktu źródłowego EMP-002.
- `make verify` przeszedł lokalnie: Maven `clean verify`, Testcontainers PostgreSQL 18.4, build obrazu i Compose runtime smoke;
- Docker Compose uruchomił healthy PostgreSQL i aplikację, a `/actuator/health` zwrócił `UP`;
- Flyway zastosował V1, tworząc `coupons` i `coupon_redemptions`.

## Zaimplementowane i zweryfikowane w EMP-002

- `pom.xml` dla Java 21 i Spring Boot 3.5.16;
- repozytoryjny Maven launcher z przypiętym Maven 3.9.16 i SHA-512;
- główna klasa Spring Boot;
- konfiguracja datasource, Flyway, graceful shutdown i Actuator;
- migracja V1 tworząca `coupons` i `coupon_redemptions`;
- bazodanowe constrainty krytycznych invariants;
- Testcontainers 2.0.5 i integracyjny test PostgreSQL 18;
- wieloetapowy `Dockerfile` z nieuprzywilejowanym użytkownikiem runtime i health checkiem;
- `docker-compose.yml` dla aplikacji oraz PostgreSQL 18.4 z health checkami i trwałym wolumenem;
- `.dockerignore` minimalizujący build context;
- targety Make do walidacji, budowy, uruchomienia, logów, sprzątania i runtime smoke;
- pełna bramka `make verify` wymagająca działającego Docker daemon.

## Niezaimplementowane

- endpoint tworzenia kuponu;
- endpoint wykorzystania kuponu;
- przypadki użycia i repozytoria domenowe;
- integracja GeoIP i trusted proxy;
- Problem Details i OpenAPI;
- właściwe testy concurrency;
- CI oraz finalne utwardzenie procesu dostarczenia.

## Zweryfikowany gate EMP-002

Lokalnie 2026-08-06 przeszły `make docs-check`, `make bootstrap-check`, `make compose-config`, dwa buildy obrazu z cache BuildKit, `make verify` oraz `make export-source`. Runtime Compose potwierdził healthy PostgreSQL i aplikację, Actuator `UP`, Flyway V1 oraz obie tabele domenowe. `EMP-002` ma status `DONE_AND_VERIFIED`.

## Następny krok

Rozpocząć `EMP-003`: tworzenie kuponu i case-insensitive uniqueness bez zmiany accepted contract.

## Blokery

Brak blockerów projektowych. Lokalny port 8080 był zajęty przez obcy kontener `splitino-dev-nginx`; konfiguracja nadal domyślnie używa 8080, a smoke wykonano na izolowanym porcie 18080.

## Korekta wydajności budowy obrazu

Usunięto zewnętrzną dyrektywę `# syntax=docker/dockerfile:1.7`, więc Docker Desktop nie pobiera już frontendu `docker/dockerfile:1.7`. Pojedynczy build Maven używa trwałego cache BuildKit dla `/root/.m2`; drugi build bez zmian miał wszystkie kroki Maven i artefaktu `CACHED`.
