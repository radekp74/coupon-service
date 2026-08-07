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

## 2026-08-07 — `0.0.17-emp-004-verification-remediation`

### Korekta evidence

Audyt EMP-008/EMP-009 wykazał, że checkpoint `0.0.16` przedwcześnie oznaczył EMP-004 jako zweryfikowane: refinement wymagał jeszcze dowodów same-user, last-slot, per-row locking, rollbacków, innego constraintu, unitów i HTTP 403/503. Kontrakt biznesowy nie został zmieniony; status został czasowo wznowiony wyłącznie dla remediation evidence.

### Evidence

```text
Maven clean verify: PASS (60 unit, 22 integration; 63.84 s)
UserId unit: ASCII boundaries, punctuation, whitespace/control/Unicode, no trim/no normalization: PASS
RedeemCouponService unit: ordered snapshot → Client IP → GeoIP → transaction and early failures: PASS
HTTP: COUNTRY_NOT_ALLOWED 403 and GEOLOCATION_UNAVAILABLE 503 without sensitive details: PASS
PostgreSQL concurrency: 3 × 100/10 exact 10/90; same-user exact 1/19; last-slot exact 1/1: PASS
Per-row lock: locked coupon-A did not block coupon-B redemption: PASS
PostgreSQL faults: insert rollback, update-after-insert rollback and non-user unique constraint → INTERNAL_ERROR: PASS
DocLint: 0 errors (42 pre-existing Maven Javadoc warnings)
```

### Decyzja

- `EMP-004`: `DONE_AND_VERIFIED`; implementation `DONE_AND_VERIFIED`; verification remediation `COMPLETED`;
- `EMP-005`: `DONE`, `MERGED_INTO_EMP-004`, evidence owner EMP-004;
- `EMP-008` i `EMP-009`: `PLANNED`; EMP-009 wymaga osobnego refinementu/mapowania przed ewentualnym closeoutem.

## 2026-08-07 — EMP-009 refinement draft

Dokumentacyjny draft mapuje istniejące Testcontainers evidence: EMP-003 ma `CreateCouponApiIT.concurrentCaseVariantsProduceExactlyOneCreatedCoupon` z 3 × 24 prób, a EMP-004 ma 3 × 100/10, same-user 1/19, last-slot 1/1 i row-lock coupon-A/coupon-B. Review oznaczył `EVIDENCE_PARTIAL`: nie wolno zamknąć EMP-009 bez późniejszej asercji kodu konfliktu i unikalnych userId 100/10 oraz dokładnie jednego nowego usera last-slot. Nie uruchomiono Maven ani `make verify`, ponieważ ten checkpoint nie zmienia kodu/testów.

## 2026-08-07 — EMP-009 refinement accepted

Radosław Piątek zaakceptował refinement przy `Evidence-State: PARTIAL`. Zamrożony przyszły checkpoint może zmienić wyłącznie trzy brakujące exact assertions w istniejących testach oraz dodać `scripts/check_emp009.py` i jego bramkę. Nie zaakceptowano nowych scenariuszy, kodu produkcyjnego, endpointów, OpenAPI, JaCoCo ani EMP-008. EMP-009 jest `READY`, implementation `NOT_STARTED`; nie jest to evidence DONE.

## 2026-08-07 — EMP-009 implementation started

Rozpoczęto wyłącznie zatwierdzony zakres: trzy missing exact assertions w istniejących testach współbieżności oraz implementacyjny checker. EMP-009 jest `IN_PROGRESS`, a `Evidence-State` pozostaje `PARTIAL` do czasu pełnego Maven/Testcontainers/Docker gate; nie dodano funkcji biznesowej ani nowego scenariusza testowego.

## 2026-08-07 — 0.0.18-emp-009-verified

EMP-009 jest `DONE_AND_VERIFIED`. `CouponRedemptionApiIT.concurrentUsersRespectExactCapacityInThreeRounds` wykonał 3 rundy po 100 requestów: w każdej dokładnie 10 × 201, 90 × `COUPON_EXHAUSTED`, zero other/unknown, `current_uses=10`, 10 redemption i `COUNT(DISTINCT user_id)=10`. `sameUserConcurrentRetriesProduceExactlyOneSuccessAndNineteenConflicts` zachował 1 × 201, 19 × `COUPON_ALREADY_REDEEMED`, 0 exhausted oraz uses/records=1. `twoDifferentUsersCompeteForTheLastSlotWithExactOutcomes` zachował 1 × 201, 1 × exhausted i dokładnie jednego zapisanego z `user-A`/`user-B`. `rowLockOnOneCouponDoesNotGloballySerializeAnotherCoupon` potwierdził zakończenie coupon-B przed release latcha coupon-A; concurrent create pozostał 3 × 24 z jednym rekordem.

`scripts/check_emp009.py` i `make emp009-check` zostały uruchomione także przez `verify.sh`; kontrolowane mutacje każdej z trzech nowych asercji na kopii tymczasowej zostały odrzucone. `./mvnw -B -ntp clean verify` przeszło w 57.47 s (60 unit + 22 integration; DocLint 0 errors, 42 warnings). `make verify` przeszło w 90.98 s; izolowany Docker smoke użył `127.0.0.1:55006`, zweryfikował health/OpenAPI/Swagger UI i usunął wyłącznie własny stos.

## 2026-08-07 — EMP-008 refinement draft

Po aktualnym audycie coverage/test completeness utworzono dokumentacyjny draft. Potwierdza on realne 60 unit i 22 integration tests oraz brak JaCoCo i zmierzonego baseline’u; nie zmienia Java, POM, testów, OpenAPI ani Docker. Proponuje przyszłe JaCoCo `verify` z HTML/XML, LINE 80%, BRANCH 70%, ochroną pakietów krytycznych, manualnym missed-branch review i polityką 42 warnings. EMP-008 jest `REFINEMENT`, `DRAFT`, `Implementation-Allowed: NO`; nie wykonano Maven ani Docker gate dla dokumentacyjnego checkpointu.

## 2026-08-07 — `0.0.20-emp-008-refinement-accepted`

Radosław Piątek formalnie zaakceptował refinement EMP-008. Zamrożono przyszły `org.jacoco:jacoco-maven-plugin:0.8.15`, globalne LINE >=80% i BRANCH >=70%, jedną logiczną grupę krytyczną 75%/65%, brak default exclusions, manualny missed line/branch review, report-driven test remediation oraz PIT `OUT_OF_SCOPE`. Dla Javadoc ustalono: naprawić 18 A, indywidualnie rozstrzygnąć 19 D, `DocLint errors=0`, `new warnings=0` i finalny jawnie uzasadniony budget <=5. `Coverage-Evidence` pozostaje `NOT_MEASURED`: w tym checkpointcie nie wdrożono JaCoCo ani nie uruchomiono Maven/Docker gate. EMP-008 jest `READY`, refinement `ACCEPTED`, implementation `NOT_STARTED`, `Implementation-Allowed: YES`.

## 2026-08-07 — EMP-008 phase 1 measurement in progress

Dodano JaCoCo 0.8.15 z `append=true`, pełnym HTML/XML reportem oraz globalnym BUNDLE 80/70 i critical BUNDLE 75/65. Pierwszy `clean verify` przeszedł: 60 unit, 22 integration, globalnie LINE 431/483 (89.23%) i BRANCH 215/306 (70.26%), critical LINE 349/396 (88.13%) i BRANCH 189/268 (70.52%). Raport obejmuje Failsafe: persistence, transactional redemption, controller i handler mają rzeczywiste coverage. DocLint nadal ma 0 errors i 42 warnings. EMP-008 pozostaje `IN_PROGRESS`; nie dodano testów, exclusions ani Javadoc remediation i nie wykonano closeoutu.

## 2026-08-07 — EMP-008 phase 2 remediation candidate

Po pierwszym zielonym pomiarze właściciel utrzymał globalne 80/70, critical 75/65 i brak exclusions. Kandydat dodaje behavior-driven testy bezpiecznego podzbioru `Forwarded`/XFF, strict IP/CIDR, trusted-proxy fail-closed, wybranych provider failure branches i jawnych invariantów domeny. `scripts/check_emp008.py` otrzymał report-mode przeliczający rzeczywisty `jacoco.xml` oraz kontrolowane fail-closed self-testy; `verify.sh` uruchamia ten pomiar po Mavenie. Javadoc remediation naprawia brakujące kontrakty A/D wyłącznie komentarzami i `{@inheritDoc}`. Ten wpis nie jest closeoutem: finalne coverage, DocLint warning count, Testcontainers i Docker smoke oczekują pełnego gate.

## 2026-08-07 — `0.0.21-emp-008-done-and-verified`

EMP-008 zamknięto po pełnym lokalnym gate bez obniżania progów i bez exclusions. Pierwszy measured baseline 89.23% LINE / 70.26% BRANCH globalnie i 88.13% / 70.52% dla critical aggregate uruchomił manualny missed-branch review. Dodano wyłącznie wartościowe testy istniejących security/domain contracts: `Forwarded`/XFF, strict IP/CIDR, trusted proxy, provider failure/body oraz invariants domenowe. Produkcyjna logika nie została zmieniona; zmiany `src/main/java` w Phase 2 były wyłącznie Javadoc/comments.

Finalny evidence:

```text
Maven/Testcontainers: PASS
Unit: 106/106
Integration: 22/22
All: 128/128
Global JaCoCo LINE: 464/483 = 96.07%
Global JaCoCo BRANCH: 264/306 = 86.27%
Critical LINE: 382/396 = 96.46%
Critical BRANCH: 238/268 = 88.81%
JaCoCo exclusions: 0
EMP-008 report checker: PASS
Negative report self-tests: PASS
DocLint: 0 errors, 5 justified warnings
Docker smoke: PASS, 127.0.0.1:55008
Cleanup: PASS
```

Warning budget 5 został świadomie zaakceptowany zamiast dodawania mechanicznych konstruktorów/komentarzy. Trzy warnings dotyczą implicit default constructors (`ApiExceptionHandler`, `CoreConfiguration`, `CouponServiceApplication`), dwa prywatnych pól (`CouponCodeConflictException.normalizedCode`, `InvalidCouponValueException.field`) z już udokumentowanym publicznym kontraktem. To spełnia zaakceptowane `<=5` i zachowuje zasadę `new warnings = 0`.

Stan: `EMP-008 = DONE_AND_VERIFIED`, `Coverage-Evidence = MEASURED_AND_VERIFIED`. Następny checkpoint: refinement EMP-010; EMP-009 pozostaje `DONE_AND_VERIFIED`.

## Zaakceptowany refinement EMP-010 — 2026-08-07

Po `EMP-008 = DONE_AND_VERIFIED` przygotowano i 2026-08-07 formalnie zaakceptowano dokumentacyjny refinement CI/delivery/observability. Audyt potwierdził brak workflow `.github/workflows/ci.yml`, brak Prometheus registry/request-ID runtime, mutable Docker base tags oraz niedeterministyczne metadane source ZIP. Zamrożony kontrakt przewiduje jeden Ubuntu 24.04 GitHub Actions job uruchamiający istniejące `make verify`, pełne action SHA, digest-pinned base images, read-only byte-reproducible export i minimalną obserwowalność zgodną z privacy contract.

Nie uruchomiono GitHub CI ani nie zaimplementowano metryk, workflow, request-ID, digest pinów i nowego packagera. `EMP-010 = READY`, refinement `ACCEPTED`, implementation `NOT_STARTED`, `Implementation-Allowed: YES`; evidence CI/delivery/observability pozostaje `NOT_MEASURED`. Właściciel zaakceptował wszystkie osiem decyzji i scope jest zamrożony.

## EMP-010 implementation candidate — 2026-08-07

Rozpoczęto implementację wyłącznie zaakceptowanego zakresu EMP-010: jeden immutable-SHA GitHub Actions gate, przypięte Docker base digests, deterministyczny tracked-file source ZIP, strict `X-Request-Id`, Logstash JSON oraz sześć rodzin metryk Micrometer/Prometheus. Stan pozostaje `IN_PROGRESS`; brak closeoutu i brak deklaracji measured evidence przed pełnym lokalnym gate, delivery-check i rzeczywistym GitHub Actions run.

## Closeout EMP-010 — 2026-08-07

EMP-010 zakończono po finalnym repair `35fa7c7e07ac341a410fad38c8ced030ac30ed25`. Lokalny canonical gate wykonał 112 unit i 23 integration tests bez failures/errors, zachował progi JaCoCo (95.76% LINE / 86.39% BRANCH globalnie; 95.06% / 88.21% critical) oraz potwierdził Docker runtime: health, canonical OpenAPI, Swagger UI, Prometheus, `X-Request-Id` i structured JSON logging.

Post-commit `make delivery-check` potwierdził byte-for-byte reproducibility oraz fail-closed dla stale checksum. Deterministyczny source artifact `coupon-service-source-35fa7c7e07ac.zip` ma SHA-256 `ed3791e735485bb452209c3c4c8e2bdd32a9eab8df36f1e28f3375d770b8e3fa`.

Pierwszy zdalny run dla `43b4331` wykrył nieprzenośny CRLF regex w smoke checku, mimo że runtime zwracał poprawny header. Minimalna korekta oraz stabilizacja testowego GeoIP transportu nie zmieniły produkcyjnego kontraktu. GitHub Actions `CI #2` dla `35fa7c7` na `main` zakończył się zielonym wynikiem w 2m48s. Stan: `EMP-010 = DONE_AND_VERIFIED`, CI/delivery/observability evidence `MEASURED_AND_VERIFIED`. Następne zadanie: EMP-011.

## EMP-011 — final review checkpoint (2026-08-07)

Refinement zaakceptowany przez właściciela. Final review aktualizuje bieżącą dokumentację i recruiter-facing README, dodaje checker anty-regresyjny oraz reconciliuje ryzyka. Funkcjonalność aplikacji pozostaje zamrożona; closeout wymaga local `make verify`, delivery-check, source export i zielonego CI finalnego SHA.

Local EMP-011 canonical `make verify` zakończył się sukcesem; implementation checkpoint może zostać zacommitowany i wypchnięty, ale status pozostaje `IN_PROGRESS` do zielonego CI finalnego SHA.

## 2026-08-07 — EMP-011 DONE_AND_VERIFIED

Zaakceptowany refinement został wykonany bez zmian funkcjonalnych. Commit `799a4bd` zapisał akceptację EMP-011, a finalny implementation/review SHA `5c7d3f5b9e48ff88a90f11047f45b249b4ee7e65` przebudował recruiter-facing README, usunął stale-current-state dokumentacji, zreconciliował risk register i dodał finalny checker. Produkcyjne Java, POM, migracje, Dockerfile, Compose i canonical OpenAPI pozostały zamrożone.

Lokalny `make verify` zakończył się PASS: 112 unit + 23 integration, JaCoCo 95.76% LINE / 86.39% BRANCH globalnie i 95.06% / 88.21% critical, Docker health/OpenAPI/Swagger/Prometheus/request-ID/structured logging PASS. `make delivery-check` odtworzył byte-for-byte source SHA-256 `579f49576318bba67d8a2a553cb548b0fa6118b05058c4bd730ad83d6b897d63`. GitHub Actions `CI #5` dla `5c7d3f5` na `main` zakończył się zielono w 2m05s. Stan końcowy: `EMP-011 = DONE_AND_VERIFIED`, final review/public repo evidence `MEASURED_AND_VERIFIED`.
