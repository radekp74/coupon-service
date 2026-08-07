# Changelog

Wszystkie istotne zmiany projektu są rejestrowane w tym pliku. Format jest inspirowany Keep a Changelog, ale dostosowany do małego zadania rekrutacyjnego.

## [Unreleased]

### Added

- draft refinement EMP-009: formalne mapowanie concurrency evidence EMP-003/EMP-004, standard deterministyczności i `make emp009-refinement-check`; wskazane trzy pozostałe exact assertions przed closeoutem;
- formalnie zaakceptowany refinement EMP-009: przyszły checkpoint jest ograniczony do trzech exact assertions w istniejących testach i implementacyjnego checkera, bez JaCoCo, API ani zmian produkcyjnych;
- rozpoczęto EMP-009 wyłącznie w zaakceptowanym zakresie trzech exact assertions i checkera; evidence pozostaje `PARTIAL` do pełnego gate;
- zakończone i zweryfikowane EMP-009: 100/10 klasyfikuje exact 90 `COUPON_EXHAUSTED` i 10 unikalnych userId, last-slot sprawdza jednego zapisanego konkurenta, a `make emp009-check` jest częścią pełnego gate;

- remediation EMP-004 po audycie EMP-008/EMP-009: bezpośrednie testy `UserId` i orchestratora, HTTP 403/503, exact same-user 1/19, last-slot 1/1, dowód braku globalnego JVM locka oraz PostgreSQL rollback/constraint evidence;
- checker EMP-004 wymaga teraz rzeczywistych testów remediation, a checkery późniejszych checkpointów rozpoznają jawny stan `IMPLEMENTED` podczas wznowionej weryfikacji;

- zaakceptowany refinement EMP-004 dla transakcyjnego redemption: kontrakt HTTP, snapshot/GeoIP poza transakcją, row lock, atomowy insert/increment, rollback, retry semantics i exact-count concurrency;
- formalny amendment EMP-001: opaque, case-sensitive `userId` `^[!-~]{1,128}$`, bez trimowania i normalizacji, do zgodnego enforcement w Bean Validation, PostgreSQL i OpenAPI podczas implementacji;
- EMP-005 jest `MERGED_INTO_EMP-004`; user-once jest obowiązkowym invariantem wspólnej transakcji i evidence;
- `make emp004-refinement-check` pilnujący kompletności draftu i zakazu przedwczesnej implementacji/OpenAPI;

- formalnie zaakceptowany refinement EMP-006 wraz z security amendmentem i pięcioma decyzjami właściciela;
- ukończone i zweryfikowane EMP-006: strict Client IP, trusted CIDR proxy, IANA special-purpose policy, ipwho.is adapter, local/test stub i WireMock evidence bez publicznej sieci;
- `make verify` używa dynamicznego portu loopback i unikalnego projektu Compose, więc nie zatrzymuje ani nie koliduje z lokalnymi stosami;
- security amendment draftu EMP-006: fail-closed field-lines, redirecty bez `Location`, body limit 16 KiB, IPv6/porty i boundary proxy contract;
- draft refinement EMP-006 z precyzyjnym kontraktem Client IP, trusted proxy chain, provider-neutral GeoIP, prywatnością, failure policy i matrycą testów;
- `make emp006-refinement-check` chroniący zakaz implementacji przed akceptacją oraz kompletność decyzji EMP-006;
- zakończone i zweryfikowane EMP-007: canonical OpenAPI, Swagger UI, runtime `/openapi.yaml`, Maven DocLint, `make emp007-check` i znaczący Javadoc publicznych kontraktów EMP-003;
- `POST /api/v1/coupons` z request/response contract;
- value objects `CouponCode` i `CountryCode` oraz model `Coupon`;
- create use case, port repozytorium i adapter PostgreSQL `JdbcClient`;
- mapowanie unique violation `23505` na 409 `COUPON_CODE_CONFLICT`;
- Problem Details dla invalid request, conflict i internal error;
- unit tests oraz `CreateCouponApiIT` z concurrent create exact-count evidence;
- zaakceptowany refinement, review checklist i statyczny checker EMP-003;
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

- backlog wskazuje teraz własny refinement EMP-006; refinement jest `ACCEPTED`, a implementacja jest dozwolona wyłącznie w osobnym checkpointcie;
- po zweryfikowaniu prerequisites EMP-006/007, `EMP-004` przeszło z historycznego `BLOCKED` do własnego draftu `REFINEMENT`;
- OpenAPI is now a mandatory part of Definition of Done for every public endpoint;
- zoptymalizowano `Dockerfile`: usunięto kosztowne `dependency:go-offline`, a build Maven korzysta z trwałego cache BuildKit `/root/.m2`;
- usunięto zewnętrzną dyrektywę Dockerfile frontend, aby build nie zależał od pobierania `docker/dockerfile:1.7`;
- poprawiono punkt montowania wolumenu PostgreSQL 18 na `/var/lib/postgresql`;
- `docker-up` uruchamia istniejący obraz bez wymuszania zbędnego ponownego builda;
- statyczny checker zabrania ponownego wprowadzenia `dependency:go-offline` do budowy obrazu i wymaga cache mount Maven;
- `EMP-002` przeszedł do `DONE_AND_VERIFIED` po lokalnym `make verify`, Compose smoke i eksporcie źródeł;
- `make verify` nie pomija już Maven, gdy istnieje kod aplikacji, lecz wymaga pełnego clean build i Testcontainers;
- source export wykonuje także statyczną walidację bootstrapu;
- `make package` i `make export-source` korzystają ze wspólnego, rozszerzonego filtra artefaktów i sekretów.
- naprawiono Spring proxy dla adaptera `JdbcCouponRepository` i transakcyjnego `CreateCouponService`, usuwając niezgodne modyfikatory `final`.
- `EMP-003` przeszedł pełny lokalny gate, w tym exact-count concurrent create i runtime HTTP Compose.

### Planned

- JaCoCo i scope EMP-008;
- CI.

## [0.0.1-foundation] — 2026-08-06

### Added

- draft refinement EMP-004 dla transakcyjnego redemption: kontrakt HTTP, snapshot/GeoIP poza transakcją, row lock, atomowy insert/increment, rollback, retry semantics i exact-count concurrency;
- `make emp004-refinement-check` pilnujący kompletności draftu i zakazu przedwczesnej implementacji/OpenAPI;

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
