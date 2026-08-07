# Coupon Service — zadanie rekrutacyjne backend

REST-owy serwis do tworzenia i bezpiecznego wykorzystywania kuponów rabatowych.


## Aktualny stan

- **Checkpoint:** `0.0.22-emp-010-refinement-draft`
- **Data stanu:** `2026-08-07`
- **Termin oddania:** `2026-08-10`, koniec dnia
- **EMP-000–EMP-003:** `DONE_AND_VERIFIED`
- **EMP-007:** `DONE_AND_VERIFIED`
- **EMP-006:** `DONE_AND_VERIFIED`
- **Aktywne zadanie:** EMP-010 — implementacja CI/delivery/observability po zaakceptowanym refinemencie
- **Refinement EMP-006:** `ACCEPTED`
- **Implementation EMP-006:** `DONE_AND_VERIFIED`
- **EMP-004:** `DONE_AND_VERIFIED`; refinement i implementation `DONE_AND_VERIFIED`
- **EMP-005:** `MERGED_INTO_EMP-004`; user-once pozostaje obowiązkowym invariantem i evidence EMP-004
- **EMP-008:** `DONE_AND_VERIFIED`, refinement `ACCEPTED`; implementation `DONE_AND_VERIFIED`, implementation allowed `YES`; coverage evidence `MEASURED_AND_VERIFIED`
- **EMP-009:** `DONE_AND_VERIFIED`, refinement `ACCEPTED`; evidence `COMPLETE`, implementation `DONE_AND_VERIFIED`
- **EMP-010:** `READY`, refinement `ACCEPTED`; implementation `NOT_STARTED`, `Implementation-Allowed: YES`; CI/delivery/observability evidence `NOT_MEASURED`
- **Kod biznesowy:** `CREATE_COUPON_DONE_AND_VERIFIED`
- **OpenAPI/Swagger UI:** `DONE_AND_VERIFIED`
- **Javadoc/DocLint policy:** `ACTIVE_AND_VERIFIED`
- **Weryfikacja runtime EMP-007:** `PASS`

EMP-004 dostarcza transakcyjny endpoint redemption z server-side Client IP/GeoIP, row lockiem PostgreSQL i exact-count concurrency. Swagger UI oraz canonical OpenAPI opisują oba istniejące endpointy.

EMP-008 zamyka quality gate bez exclusions: JaCoCo 0.8.15 jest częścią Maven `verify`; finalny pomiar wynosi globalnie **96.07% LINE / 86.27% BRANCH** oraz **96.46% LINE / 88.81% BRANCH** dla critical aggregate. Pełny gate przeszedł przy 106 testach unit i 22 integration. DocLint ma 0 błędów; świadomie pozostawiono 5 ostrzeżeń o niskiej wartości dokumentacyjnej (3 implicit default constructors klas frameworkowych i 2 prywatne pola wyjątków), mieszcząc się w zaakceptowanym budget <=5 bez dodawania mechanicznego Javadoc.

EMP-010 ma formalnie zaakceptowany refinement. Zamrożony kontrakt obejmuje pojedynczy GitHub Actions gate korzystający z istniejącego `make verify`, deterministyczny source export, immutable references dla actions/base images oraz minimalną obserwowalność: `X-Request-Id`, structured JSON logs i `/actuator/prometheus` z niskokardynalnymi metrykami. Implementacja jest dozwolona, ale jeszcze `NOT_STARTED`; evidence pozostaje `NOT_MEASURED`.


## Zweryfikowany zakres EMP-003

- `POST /api/v1/coupons`;
- trim i case-insensitive canonicalizacja kodu przez `Locale.ROOT`;
- walidacja kraju ISO 3166-1 alpha-2;
- `currentUses = 0` przy utworzeniu;
- pojedynczy parametryzowany `INSERT` przez Spring `JdbcClient`;
- unikalność gwarantowana przez PostgreSQL `UNIQUE(normalized_code)`;
- brak podatnego na race condition `existsByCode`;
- 400 `INVALID_REQUEST`, 409 `COUPON_CODE_CONFLICT`, 500 `INTERNAL_ERROR` jako Problem Details;
- wstrzykiwane `Clock` i generator UUID;
- unit tests domeny/use case;
- HTTP integration tests na PostgreSQL 18.4 przez Testcontainers;
- test 24 równoległych wariantów case z dokładnie jednym sukcesem;
- machine-readable `docs/api/openapi.yaml` dla operacji create.

Endpoint wykorzystania kuponu jest zaimplementowany i opisany w canonical OpenAPI/Swagger UI. EMP-009 nie zmienił API; zamknął deterministyczne evidence współbieżności po exact assertions i pełnym gate.

## Zamrożony kierunek techniczny

- modularny monolit z portami i adapterami;
- aplikacja bezstanowa, skalowalna horyzontalnie;
- PostgreSQL 18 jako source of truth;
- spójność limitu przez transakcję i blokadę rekordu kuponu;
- zewnętrzny GeoIP ukryty za portem, z timeoutem i bez przechowywania surowego IP;
- integracyjne testy wyłącznie na PostgreSQL przez Testcontainers, bez H2.

Pełne uzasadnienie znajduje się w [zaakceptowanym refinemencie EMP-001](docs/project/refinements/EMP-001.md) oraz w [ADR-ach](docs/adr/README.md).

## Wymagania lokalne

- macOS lub Linux;
- Java 21;
- Docker Desktop z uruchomionym daemonem;
- `curl` albo `wget`, `unzip` oraz `shasum` albo `sha512sum` przy pierwszym uruchomieniu Wrappera;
- Python 3.9+ dla lekkich bramek repozytorium.

Lokalna instalacja Maven nie jest wymagana.

## Pierwsza pełna walidacja

```bash
make verify
```

Bramka wykonuje kolejno:

1. governance dokumentacji;
2. statyczne kontrakty bootstrapu i EMP-003;
3. statyczne kontrakty accepted refinementów i implementacji EMP-004, EMP-006, EMP-007 oraz EMP-009, a także kontrolę składni skryptów i Makefile;
4. kontrolę Java 21;
5. kontrolę skonfigurowanego klienta i daemona Docker;
6. `./mvnw -B -ntp clean verify`;
7. integracyjny test Flyway na PostgreSQL 18 przez Testcontainers;
8. kontrolę powstania wykonywalnego artefaktu Spring Boot;
9. walidację `docker-compose.yml`;
10. zbudowanie obrazu aplikacji;
11. uruchomienie aplikacji i PostgreSQL na dynamicznym porcie loopback oraz potwierdzenie `/actuator/health`, `/swagger-ui` i `/openapi.yaml`;
12. bezpieczne usunięcie stosu i wolumenu testowego.

Lżejsze bramki bez Dockera i pobierania zależności:

```bash
make docs-check
make bootstrap-check
make emp003-check
make emp004-refinement-check
make emp004-check
make emp006-refinement-check
make emp006-check
make emp007-check
make emp008-refinement-check
make emp008-check
make emp009-refinement-check
make emp009-check
make emp010-refinement-check
```

Po `./mvnw -B -ntp clean verify` raport JaCoCo jest dodatkowo weryfikowany niezależnym checkerem repozytorium:

```bash
make emp008-report-check
```

Checker ponownie liczy globalne 80/70 i critical aggregate 75/65 z `target/site/jacoco/jacoco.xml` oraz wykonuje fail-closed self-testy na kopiach tymczasowych raportu.

## OpenAPI i Swagger UI dla testerów

Po uruchomieniu stosu na izolowanym porcie:

```bash
make docker-up APP_PORT=18080
```

tester otrzymuje:

```text
Swagger UI:       http://localhost:18080/swagger-ui
OpenAPI YAML:     http://localhost:18080/openapi.yaml
```

Swagger UI korzysta bezpośrednio z wersjonowanego `docs/api/openapi.yaml`. Tester może przejrzeć istniejące endpointy, requesty, odpowiedzi, walidację i przykłady. Specyfikacja opisuje tylko endpointy rzeczywiście zaimplementowane. Ewentualny generated `/v3/api-docs` jest jedynie diagnostycznym widokiem runtime i nie zastępuje wersjonowanego kontraktu. Redemption oraz server-side GeoIP są zaimplementowane; EMP-008 nie zmienia API.

## Docker na macOS

Repozytorium używa nadpisywalnej zmiennej Make:

```make
DOCKER ?= /Applications/Docker.app/Contents/Resources/bin/docker
```

Kontrola lokalnego klienta i daemona Docker:

```bash
make docker-check
```

W innym środowisku ścieżkę można nadpisać bez zmiany repozytorium:

```bash
make verify DOCKER=/usr/local/bin/docker
```

Wszystkie targety korzystające z Dockera wywołują `$(DOCKER)`, a nie zakładają obecności polecenia `docker` w `PATH`.

Walidacja pliku Compose:

```bash
make compose-config
```

Uruchomienie kompletnego stosu:

```bash
make docker-up
```

Aplikacja jest wtedy dostępna pod `http://localhost:8080`, a health check pod `http://localhost:8080/actuator/health`. Port można zmienić bez edycji pliku:

```bash
make docker-up APP_PORT=18080
```

Logi i zatrzymanie stosu:

```bash
make docker-logs
make docker-down
```

Jednorazowy test obrazu i stosu, który po sukcesie sam sprząta kontenery oraz wolumen:

```bash
make docker-smoke APP_PORT=18080 COMPOSE_PROJECT_NAME=coupon-service-smoke
```

### Cache zależności Maven w obrazie

Build obrazu nie uruchamia `dependency:go-offline`. Ten cel rozwiązuje również pluginy, raporty i ich zależności, przez co pierwszy build niewielkiej aplikacji może trwać nieproporcjonalnie długo. `Dockerfile` używa cache mount BuildKit:

```dockerfile
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    ./mvnw -B -ntp -DskipTests package
```

Pierwszy build nadal musi pobrać zależności, ale kolejne kompilacje wykorzystują trwały cache Maven.

## Maven Wrapper

Pierwsze uruchomienie:

```bash
./mvnw --version
```

Wrapper pobiera przypiętą dystrybucję Maven 3.9.16 do `~/.m2/wrapper/dists`, sprawdza jej SHA-512 i dopiero wtedy ją uruchamia. Kolejne wykonania korzystają z lokalnego cache.

## Eksport źródeł do analizy

Paczka gotowa do przesłania, domyślnie zapisywana w `~/Downloads`:

```bash
make export-source
```

Katalog można wskazać jawnie:

```bash
make export-source SOURCE_EXPORT_DIR="$HOME/Desktop"
```

Eksport pomija m.in. `.git`, `target`, `build`, `dist`, katalogi IDE, logi, ZIP-y, prywatne pliki `.env`, klucze i certyfikaty. Przed pakowaniem uruchamia bramkę dokumentacji i statyczny checker bootstrapu, odświeża `CHECKSUMS.sha256`, a następnie podaje pełną ścieżkę oraz SHA-256 archiwum.

## Dokumentacja

Najlepsza kolejność czytania:

1. [Aktualny status](docs/project/current-status.md)
2. [Podsumowanie refinementu EMP-001](docs/project/refinements/EMP-001-summary.md)
3. [Zaakceptowany refinement EMP-004](docs/project/refinements/EMP-004.md)
4. [Zaakceptowany refinement EMP-006](docs/project/refinements/EMP-006.md)
5. [Refinement EMP-007](docs/project/refinements/EMP-007.md)
6. [Refinement EMP-003](docs/project/refinements/EMP-003.md)
7. [Pełny refinement EMP-001](docs/project/refinements/EMP-001.md)
8. [Backlog](docs/project/backlog.md)
9. [Kontrakt API](docs/api/api-contract.md)
10. [Architektura](docs/architecture/overview.md)
11. [Strategia testów](docs/testing/test-strategy.md)
12. [Rejestr ryzyk](docs/project/risk-register.md)
13. [Pełny indeks dokumentacji](docs/DOCUMENTATION_INDEX.md)

## Konwencje językowe

- dokumentacja projektowa i biznesowa: język polski;
- kod, testy, nazwy klas, metod, pakietów i komunikaty techniczne: język angielski;
- klienci API będą integrować się przez stabilne `error.code`, nie przez tekst `detail`.

## Przykład utworzenia kuponu

Po `make docker-up APP_PORT=18080`:

```bash
curl -i   -H 'Content-Type: application/json'   -d '{"code":"WIOSNA","maxUses":100,"countryCode":"PL"}'   http://localhost:18080/api/v1/coupons
```

Zweryfikowane zachowanie: pierwszy request zwraca `201 Created`, a ponowienie z kodem `wiosna` zwraca `409 COUPON_CODE_CONFLICT`.
