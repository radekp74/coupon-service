# Changelog

Wszystkie istotne zmiany projektu są rejestrowane w tym pliku. Format jest inspirowany Keep a Changelog, ale dostosowany do małego zadania rekrutacyjnego.

## [Unreleased]

### Added

- bootstrap Java 21 / Spring Boot 3.5.16 i główna klasa aplikacji;
- przypięty Maven 3.9.16 z repozytoryjnym launcherem oraz walidacją SHA-512 dystrybucji;
- zależności Spring Web, Validation, JDBC, Actuator, Flyway i PostgreSQL;
- migracja Flyway V1 dla `coupons` i `coupon_redemptions`;
- bazodanowe unique/check/foreign-key constraints chroniące podstawowe invariants, w tym zgodność `normalized_code = upper(code)`;
- Testcontainers 2.0.5 i integracyjny `DatabaseMigrationIT` na PostgreSQL 18.4;
- `make bootstrap-check`, `make java-check` i `make maven-verify`;
- wieloetapowy `Dockerfile` z przypiętym Java 21, nieuprzywilejowanym użytkownikiem runtime i health checkiem;
- `.dockerignore` ograniczający kontekst budowy do Maven Wrappera, `pom.xml` i źródeł;
- `docker-compose.yml` dla aplikacji i PostgreSQL 18.4 z health checkami, zależnością readiness i trwałym wolumenem;
- targety `compose-config`, `docker-build`, `docker-up`, `docker-down`, `docker-logs` i `docker-smoke`;
- pełna bramka `make verify` wymagająca Docker daemon, Maven `clean verify`, budowy obrazu i zdrowego runtime Compose;
- nadpisywalny kontrakt `DOCKER ?= /Applications/Docker.app/Contents/Resources/bin/docker`;
- target `make docker-check` walidujący skonfigurowaną binarkę Docker;
- target `make export-source`, który zapisuje bezpieczny ZIP źródeł domyślnie w `~/Downloads`;
- konfigurowalny `SOURCE_EXPORT_DIR` oraz SHA-256 generowanej paczki.

### Changed

- zoptymalizowano `Dockerfile`: usunięto kosztowne `dependency:go-offline`, a build Maven korzysta z trwałego cache BuildKit `/root/.m2`;
- usunięto zewnętrzną dyrektywę Dockerfile frontend, aby build nie zależał od pobierania `docker/dockerfile:1.7`;
- poprawiono punkt montowania wolumenu PostgreSQL 18 na `/var/lib/postgresql`;
- `docker-up` uruchamia istniejący obraz bez wymuszania zbędnego ponownego builda;
- statyczny checker zabrania ponownego wprowadzenia `dependency:go-offline` do budowy obrazu i wymaga cache mount Maven;
- `EMP-002` przeszedł do `DONE_AND_VERIFIED` po lokalnym `make verify`, Compose smoke i eksporcie źródeł;
- `make verify` nie pomija już Maven, gdy istnieje kod aplikacji, lecz wymaga pełnego clean build i Testcontainers;
- source export wykonuje także statyczną walidację bootstrapu;
- `make package` i `make export-source` korzystają ze wspólnego, rozszerzonego filtra artefaktów i sekretów.

### Planned

- endpoint tworzenia kuponu;
- endpoint wykorzystania kuponu;
- integracja GeoIP;
- testy jednostkowe, integracyjne i współbieżności;
- OpenAPI i CI.

## [0.0.1-foundation] — 2026-08-06

### Added

- lekkie governance dokumentacji i jednoznaczne źródła prawdy;
- backlog z priorytetami, statusami i mapowaniem do refinementu;
- Definition of Ready i Definition of Done;
- zaakceptowany refinement `EMP-001` dla całego ograniczonego MVP;
- review checklist i podsumowanie refinementu;
- architektura, model danych i kontrakt API;
- ADR-y dotyczące stosu, współbieżności i GeoIP;
- rejestry decyzji, ryzyk i lessons learned;
- strategia testów i traceability wymagań;
- automatyczny checker dokumentacji, `Makefile` i `verify.sh`;
- bezpieczny skrypt eksportu źródeł.

### Verified

- komplet wymaganych dokumentów;
- unikalność identyfikatorów backlogu;
- zgodność statusów z dozwolonym słownikiem;
- obecność zaakceptowanego refinementu dla zadania `READY`;
- spójność linków lokalnych i indeksu dokumentacji;
- brak deklarowania kodu aplikacji jako ukończonego.
