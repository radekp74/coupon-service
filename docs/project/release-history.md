# Historia checkpointów

## 2026-08-06 — `0.0.1-foundation`

### Zakres

- governance dokumentacji;
- backlog i current status;
- DoR/DoD;
- accepted refinement `EMP-001`;
- architektura, dane i API;
- trzy ADR-y;
- decision log i risk register;
- strategia testów;
- traceability wymagań;
- automatyczny checker i verify script;
- bezpieczny source export.

### Stan runtime

- aplikacja: `NOT_STARTED`;
- baza/migracje: `NOT_STARTED`;
- endpointy: `NOT_STARTED`;
- testy aplikacji: `NOT_STARTED`.

### Evidence

```text
make docs-check: PASS
make verify: PASS
```

### Decyzja

- `EMP-000`: `DONE_AND_VERIFIED`;
- `EMP-001`: `DONE_AND_VERIFIED`;
- `EMP-002`: `READY`;
- implementacja może rozpocząć się bez ponownego rozstrzygania kontraktu.

## 2026-08-06 — `0.0.2-foundation-tooling-contract`

### Zakres

- domyślna, nadpisywalna ścieżka Docker Desktop dla macOS;
- kontrola `make docker-check`;
- lokalne pakowanie przez `make package`;
- eksport ZIP-a do przesłania przez `make export-source`;
- konfigurowalny `SOURCE_EXPORT_DIR`;
- rozszerzone filtrowanie sekretów, logów, artefaktów i katalogów IDE;
- raportowanie pełnej ścieżki i SHA-256 paczki;
- walidacja kontraktu Makefile w `make verify`.

### Evidence

```text
make docs-check: PASS
make verify: PASS
make package: PASS
make export-source SOURCE_EXPORT_DIR=<temporary-directory>: PASS
clean archive re-unpack and make verify: PASS
```

### Stan runtime

Bez zmiany: kod aplikacji pozostaje `NOT_STARTED`, a aktywnym zadaniem jest `EMP-002`.

## 2026-08-06 — `0.0.3-emp-002-candidate`

### Zakres

- Java 21 i Spring Boot 3.5.16;
- przypięty Maven 3.9.16 z walidacją SHA-512;
- Maven Enforcer dla Java 21 i Maven 3.9.16;
- PostgreSQL driver, Flyway core i moduł PostgreSQL;
- migracja V1 dla kuponów i rejestru użyć;
- constrainty limitu, canonical code, kraju i jednego użycia użytkownika;
- Testcontainers 2.0.5 z nowym pakietem `org.testcontainers.postgresql`;
- integracyjny test migracji i wybranych ograniczeń na PostgreSQL 18.4;
- statyczny checker EMP-002;
- rozszerzony `make verify`.

### Evidence dostępny w tym checkpointcie

```text
make docs-check: PASS
make bootstrap-check: PASS
XML/shell/Python static validation: PASS
```

### Evidence wymagany lokalnie

```text
make verify: PENDING
./mvnw -B -ntp clean verify: PENDING
PostgreSQL 18 Testcontainers startup: PENDING
Spring Boot artifact: PENDING
```

### Decyzja

- `EMP-002`: `IN_PROGRESS`;
- kod bootstrapu jest przygotowany, lecz nie jest jeszcze opisany jako `DONE_AND_VERIFIED`;
- następne zadanie nie przechodzi do `READY` przed uzyskaniem pełnego outputu lokalnej bramki.


## 2026-08-06 — `0.0.4-emp-002-candidate-docker-fix`

### Zakres

- wieloetapowy `Dockerfile` dla Java 21;
- nieuprzywilejowany użytkownik runtime;
- kontenerowy health check oparty o Spring Actuator;
- `.dockerignore` z minimalnym build context;
- `docker-compose.yml` dla aplikacji i PostgreSQL 18.4;
- health checki obu usług i zależność aplikacji od gotowej bazy;
- targety Make do config/build/up/down/logs/smoke;
- runtime smoke z automatycznym sprzątaniem kontenerów i wolumenu;
- rozszerzony statyczny checker oraz pełna bramka `make verify`.

### Przyczyna korekty

Pierwsza wersja EMP-002 błędnie traktowała użycie Docker Desktop przez Testcontainers jako wystarczający zakres bootstrapu. Review wykazał brak artefaktu uruchomieniowego aplikacji i kompletnego lokalnego stosu.

### Evidence dostępny w tym checkpointcie

```text
make docs-check: PASS
make bootstrap-check: PASS
Dockerfile/docker-compose static contract: PASS
XML/shell/Python static validation: PASS
```

### Evidence wymagany lokalnie

```text
make verify: PENDING
Docker image build: PENDING
Docker Compose health smoke: PENDING
```

## 2026-08-06 — `0.0.5-emp-002-verified`

### Zakres

- usunięcie zależności od zewnętrznego Dockerfile frontend;
- cache BuildKit Maven i pojedyncze pakowanie artefaktu;
- zgodny z PostgreSQL 18 mount named volume;
- potwierdzony lokalny Maven/Testcontainers i Docker Compose smoke.

### Evidence

```text
make docs-check: PASS
make bootstrap-check: PASS
make compose-config: PASS
make docker-build: PASS (drugi build: CACHED)
make verify: PASS
make export-source: PASS
```

### Decyzja

- `EMP-002`: `DONE_AND_VERIFIED`;
- `EMP-003`: `READY`.


## 2026-08-06 — `0.0.6-emp-003-candidate`

### Zakres

- zaakceptowany refinement i review EMP-003;
- value objects kodu i kraju;
- create use case z wstrzykiwanym UUID i czasem;
- pojedynczy parametryzowany insert przez `JdbcClient`;
- `POST /api/v1/coupons`;
- Problem Details dla 400, 409 i 500;
- unit, HTTP/PostgreSQL i concurrent create tests;
- statyczny checker EMP-003 i synchronizacja dokumentacji.

### Evidence dostępny w kandydacie

```text
make docs-check: PASS
make emp003-check: PASS
source/static syntax checks: PASS
```

### Evidence wymagany lokalnie

```text
make verify: PENDING
Testcontainers HTTP/concurrency suite: PENDING
Docker runtime smoke: PENDING
make export-source: PENDING
```

### Decyzja

- `EMP-003`: `IN_PROGRESS`;
- status nie zostanie podniesiony przed pełnym lokalnym gate.

## 2026-08-06 — `0.0.7-emp-003-verified`

### Zakres

- `POST /api/v1/coupons` z canonicalizacją kodu i walidacją kraju;
- atomowe wykrywanie case-insensitive duplicate przez constraint PostgreSQL;
- Problem Details dla 400 i 409;
- deterministyczny test współbieżny bez `Thread.sleep`.

### Evidence

```text
make docs-check: PASS
make bootstrap-check: PASS
make emp003-check: PASS
make verify: PASS
make verify time: 76.52 s (Maven clean verify: 36.206 s)
unit tests: 6/6 PASS
DatabaseMigrationIT: 4/4 PASS
CreateCouponApiIT: 4/4 PASS
concurrent create: 3 × (1 created, 23 conflicts, 1 record) PASS
Docker Compose HTTP: health UP, create 201, duplicate 409, invalid 400 PASS
```

### Decyzja

- `EMP-003`: `DONE_AND_VERIFIED`;
- `EMP-004`: `REFINEMENT`; implementacja wymaga własnego accepted refinementu.
