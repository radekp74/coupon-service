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

## 2026-08-06 — `0.0.8-emp-007-candidate`

### Zakres

- accepted refinement `EMP-007`;
- canonical OpenAPI jako jedyne źródło prawdy;
- Swagger UI dla testerów;
- canonical YAML pakowany do artefaktu;
- Springdoc 2.9.0 zgodny z Spring Boot 3.5.16;
- znaczący Javadoc dla publicznych kontraktów;
- Maven DocLint;
- test HTTP UI/YAML;
- checker `EMP-007`;
- jawne zablokowanie EMP-004 do GeoIP/API prerequisites.

### Evidence dostępny w kandydacie

```text
make docs-check: PASS
make bootstrap-check: PASS
make emp003-check: PASS
make emp007-check: PASS
source/static validation: PASS
```

### Evidence wymagany lokalnie

```text
./mvnw -B -ntp clean verify: PENDING
OpenApiDocumentationIT: PENDING
JAR static/openapi.yaml: PENDING
Docker runtime /openapi.yaml and /swagger-ui: PENDING
make verify: PENDING
make export-source: PENDING
```

### Decyzja

- `EMP-007`: `IN_PROGRESS`;
- `EMP-004`: `BLOCKED`;
- status `DONE_AND_VERIFIED` wymaga pełnego lokalnego gate.

## 2026-08-06 — `0.0.9-emp-007-verified`

### Evidence

```text
make docs-check: PASS
make bootstrap-check: PASS
make emp003-check: PASS
make emp007-check: PASS
make verify: PASS
Maven clean verify: PASS (10 testów, w tym OpenApiDocumentationIT)
DocLint: PASS bez błędów
JAR: BOOT-INF/classes/static/openapi.yaml obecny
runtime: health UP, /openapi.yaml, /swagger-ui i swagger-config url=/openapi.yaml: PASS
HTTP: create 201, case-insensitive duplicate 409 COUPON_CODE_CONFLICT: PASS
cleanup: usunięty wyłącznie własny stos Compose i wolumen
```

### Decyzja

- `EMP-007`: `DONE_AND_VERIFIED`;
- `EMP-006`: `REFINEMENT`, implementacja niedozwolona do accepted własnego refinementu;
- `EMP-004`: `BLOCKED`.


## 2026-08-06 — `0.0.10-emp-006-refinement-candidate`

### Zakres

- własny draft refinementu EMP-006;
- direct i trusted-proxy Client IP contract;
- strict IPv4/IPv6 parsing bez DNS;
- right-to-left trusted chain, limity nagłówków i fail-closed;
- provider-neutral GeoIP z demo adapterem HTTPS, timeoutami i bez retry;
- local/test stub guard;
- minimalizacja raw IP;
- test matrix, acceptance criteria i checker refinementu.

### Evidence dostępny w kandydacie

```text
make docs-check: PASS
make emp006-refinement-check: PASS
existing static gates: PASS
```

### Decyzja

- `EMP-006`: `REFINEMENT`;
- refinement: `DRAFT_READY_FOR_OWNER_REVIEW`;
- implementation: `NOT_ALLOWED`;
- pięć decyzji blokujących wymaga akceptacji właściciela przed przejściem do `READY`.

### Security amendment po review

Pierwszy review refinementu: `REJECT`. Zidentyfikowano pięć luk bezpieczeństwa: wielokrotne field-lines, redirecty dostawcy, brak liczbowego limitu body, niejednoznaczną składnię IPv6/portów oraz niedostateczny boundary proxy contract. Amendment wprowadza fail-closed dla wielu field-lines, wyłączone redirecty, limit 16 384 bajtów, ścisły podzbiór składni oraz obowiązki infrastruktury. Refinement nadal jest `DRAFT`; ponowny review i decyzja właściciela oczekują.

## 2026-08-06 — `0.0.11-emp-006-refinement-accepted`

### Formalna decyzja właściciela

Pierwszy review `0.0.10` zachowuje wynik `REJECT` dla pięciu luk bezpieczeństwa. Security amendment został następnie formalnie zaakceptowany przez Radosława Piątka, wraz z decyzjami: `ipwho.is` jako wymienny adapter demonstracyjny, wspólne `503 GEOLOCATION_UNAVAILABLE`, brak cache/retry/fallbacku, fail-closed dla błędnego `Forwarded` bez fallbacku do XFF oraz stub `PL` wyłącznie dla profili `local` i `test`.

### Evidence

```text
make docs-check: PASS
make bootstrap-check: PASS
make emp003-check: PASS
make emp006-refinement-check: PASS
make emp007-check: PASS
git diff --check: PASS
```

### Stan

- `EMP-006`: `READY`;
- refinement: `ACCEPTED`;
- implementation: `NOT_STARTED`;
- `EMP-004`: `BLOCKED`.

Ten checkpoint jest wyłącznie formalnym closeoutem refinementu; nie zawiera implementacji Client IP, GeoIP ani redemption.

## 2026-08-06 — EMP-006 implementation started

Zaakceptowany refinement jest realizowany w jednym checkpointcie implementacyjnym. `EMP-006` ma status `IN_PROGRESS`; Client IP, GeoIP i redemption nie są jeszcze deklarowane jako dostarczone. `EMP-004` pozostaje `BLOCKED`.

## 2026-08-06 — `0.0.12-emp-006-implementation-candidate`

### Evidence

```text
./mvnw -B -ntp clean verify: PASS (53 unit tests, 10 integration tests)
EMP-006 parser/policy/configuration/WireMock tests: 47 PASS
WireMock: redirect exactly one request, Content-Length 16385, streaming 16385 and exact 16384: PASS
DocLint: PASS without errors
Docker: build PASS; isolated Compose health UP, Swagger UI and /openapi.yaml PASS
```

Nie wykonano publicznego requestu do ipwho.is w testach. Port 18080 był zajęty przez istniejący kontener coupon-service, więc własny, wcześniej nieudany stos usunięto i runtime potwierdzono na izolowanym porcie 18081. Pełny `make verify` nie przeszedł jeszcze na tym checkpointcie, dlatego następny krok to dynamiczne przydzielanie portu dla własnego smoke.

### Decyzja

- `EMP-006`: `IN_PROGRESS`;
- `EMP-004`: `BLOCKED`;
- OpenAPI nadal nie opisuje redemption.

## 2026-08-06 — `0.0.13-emp-006-verified`

### Evidence

```text
make verify: PASS (86.83 s)
Maven: 53 unit tests + 10 integration tests PASS
EMP-006 parser/policy/configuration/WireMock tests: 47 PASS
DocLint: PASS without errors (15 existing technical warnings)
WireMock: redirect exactly one request, Content-Length 16385, streaming 16385 and exact 16384: PASS
Docker smoke: dynamic 127.0.0.1:55001, health UP, /swagger-ui and /openapi.yaml PASS
```

Port 18080 pozostał zajęty przez istniejący, healthy `coupon-service-app-1`. Smoke użył własnego projektu `coupon-service-verify-75419`, automatycznie przydzielonego portu loopback i trapem usunął wyłącznie własne kontenery, sieć oraz wolumen. Zewnętrzny kontener nie został zatrzymany ani zrestartowany. Testy nie wykonały publicznego requestu do ipwho.is.

### Decyzja

- `EMP-006`: `DONE_AND_VERIFIED`;
- refinement: `ACCEPTED`, implementation: `DONE_AND_VERIFIED`;
- `EMP-004`: `REFINEMENT`, implementation allowed `NO` do accepted własnego refinementu;
- canonical OpenAPI nadal nie opisuje redemption.


## 2026-08-06 — `0.0.14-emp-004-refinement-candidate`

### Zakres

- kompletny draft własnego refinementu EMP-004;
- endpoint/request/response i stabilne Problem Details;
- snapshot przed GeoIP i brak network calla pod lockiem;
- osobny proxied transaction bean;
- `READ COMMITTED`, `SELECT ... FOR UPDATE`, named unique constraint i conditional increment;
- rollback, retry semantics, OpenAPI/Javadoc DoD i exact-count concurrency;
- propozycja atomowego włączenia EMP-005;
- `make emp004-refinement-check`.

### Stan

- `EMP-004`: `REFINEMENT`;
- refinement: `DRAFT_READY_FOR_OWNER_REVIEW`;
- implementation: `NOT_STARTED`;
- implementation allowed: `NO`;
- canonical OpenAPI nadal nie zawiera redemption.

### Decyzje oczekujące

Włączenie EMP-005, format userId wraz z amendmentem EMP-001 i decyzją o DB enforcement, retry jako 409, precedence błędów oraz `READ COMMITTED + row lock` bez retry/lock timeout.

## 2026-08-06 — `0.0.15-emp-004-refinement-accepted`

### Formalna decyzja właściciela

Radosław Piątek formalnie zaakceptował refinement EMP-004 i security/contract amendment po historycznej rekomendacji `REJECT`. Utrzymano zapis pierwotnego draftu oraz przyczyny `REJECT`: niespójność `userId` z EMP-001 i potrzebę rozdzielenia odpowiedzialności kontrolera od orchestratora.

Zaakceptowane decyzje:

- EMP-005 jest `MERGED_INTO_EMP-004`; jego user-once pozostaje invariantem, kryterium i evidence wspólnej transakcji;
- `userId` jest opaque i case-sensitive, spełnia `^[!-~]{1,128}$`, bez trimowania i normalizacji; formalny amendment EMP-001 wymaga zgodnych Bean Validation, PostgreSQL `CHECK`, OpenAPI i testów integracyjnych;
- pierwszy sukces zwraca `201`, a retry tego samego `coupon/userId` zwraca `409 COUPON_ALREADY_REDEEMED`, bez replay, Idempotency-Key i deklaracji pełnej idempotencji;
- publiczna precedence to not found → GeoIP unavailable → wrong country → already redeemed → exhausted, a pod lockiem country → already redeemed → exhausted;
- transakcja użyje PostgreSQL `READ COMMITTED` i `SELECT ... FOR UPDATE`; Client IP i GeoIP pozostają poza nią, bez HTTP pod lockiem, JVM/Redis/distributed locków, automatycznego retry i custom lock timeout.

### Stan

- `EMP-004`: `READY`;
- refinement: `ACCEPTED`, implementation: `NOT_STARTED`, implementation allowed: `YES`;
- `EMP-005`: `DONE` z disposition `MERGED_INTO_EMP-004`; ownerem implementation i evidence jest EMP-004;
- endpoint redemption, migracja i canonical OpenAPI nadal nie są zmienione.

Ten checkpoint jest wyłącznie formalnym closeoutem refinementu i amendmentu; nie zawiera implementacji ani runtime evidence redemption.

## 2026-08-07 — `0.0.16-emp-004-verified`

`EMP-004` zamknięto po pełnym `make verify` (85.13 s), Maven/Testcontainers, DocLint bez błędów i dynamicznym Docker smoke na `127.0.0.1:55003`. Testy migracji potwierdziły constraint visible ASCII, a trzy rundy 100 requestów/limit 10 dały dokładnie 10 `201`, 90 exhausted oraz zgodność counter z records. Runtime HTTP potwierdził 201, retry 409 already redeemed, exhausted 409, missing 404 i invalid userId 400. `EMP-005` pozostaje `DONE` z disposition `MERGED_INTO_EMP-004`.
