# Coupon Service — zadanie rekrutacyjne backend

REST-owy serwis do tworzenia i bezpiecznego wykorzystywania kuponów rabatowych.

## Aktualny stan

- **Checkpoint:** `0.0.4-emp-002-candidate-docker-fix`
- **Data stanu:** `2026-08-06`
- **Termin oddania:** `2026-08-10`, koniec dnia
- **Governance dokumentacji:** `DONE_AND_VERIFIED`
- **Refinement rozwiązania:** `ACCEPTED`
- **Kod aplikacji:** `BOOTSTRAP_IMPLEMENTED`
- **Aktywne zadanie:** `EMP-002 — bootstrap aplikacji`
- **Weryfikacja runtime:** `PENDING_LOCAL_DOCKER_GATE`
- **Implementacja dozwolona:** `YES`, w granicach zaakceptowanego refinementu `EMP-001`

Checkpoint zawiera już kompilowalny kontrakt źródłowy aplikacji, migrację V1 oraz integracyjny test migracji na PostgreSQL 18 przez Testcontainers. `EMP-002` pozostaje jednak `IN_PROGRESS`, dopóki pełne `make verify` nie przejdzie na lokalnym macOS z działającym Docker Desktop. Dokumentacja nie przedstawia niewykonanej bramki runtime jako sukcesu.

## Zaimplementowane w EMP-002

- Java 21 i Spring Boot 3.5.16;
- Maven 3.9.16 uruchamiany przez repozytoryjny `./mvnw`;
- weryfikacja SHA-512 pobieranej dystrybucji Maven;
- Spring Web MVC, Validation, JDBC/`JdbcClient` i Actuator;
- PostgreSQL driver oraz Flyway z modułem PostgreSQL;
- migracja `V1__create_coupon_tables.sql`;
- constrainty chroniące limit, canonical code i jedno użycie przez użytkownika;
- Testcontainers 2.0.5 z PostgreSQL `18.4-alpine`;
- integracyjny test startu Spring context, migracji i wybranych constraintów;
- statyczny checker kontraktu `EMP-002`;
- wieloetapowy `Dockerfile` budujący aplikację i uruchamiający ją jako użytkownik bez uprawnień root;
- `docker-compose.yml` uruchamiający aplikację i PostgreSQL 18.4 z health checkami i trwałym wolumenem;
- `.dockerignore` ograniczający build context wyłącznie do plików potrzebnych do kompilacji;
- targety `make compose-config`, `docker-build`, `docker-up`, `docker-down`, `docker-logs` i `docker-smoke`;
- pełna bramka `make verify`, wymagająca Java 21, Docker daemon, Maven `clean verify` oraz zdrowego stosu Docker Compose.

Endpointy biznesowe, GeoIP i właściwa logika wykorzystania kuponu nie są jeszcze zaimplementowane.

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
2. statyczny kontrakt bootstrapu;
3. kontrolę składni skryptów i Makefile;
4. kontrolę Java 21;
5. kontrolę skonfigurowanego klienta i daemona Docker;
6. `./mvnw -B -ntp clean verify`;
7. integracyjny test Flyway na PostgreSQL 18 przez Testcontainers;
8. kontrolę powstania wykonywalnego artefaktu Spring Boot;
9. walidację `docker-compose.yml`;
10. zbudowanie obrazu aplikacji;
11. uruchomienie aplikacji i PostgreSQL oraz potwierdzenie `UP` na `/actuator/health`;
12. bezpieczne usunięcie stosu i wolumenu testowego.

Lżejsze bramki bez Dockera i pobierania zależności:

```bash
make docs-check
make bootstrap-check
```

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
3. [Pełny refinement EMP-001](docs/project/refinements/EMP-001.md)
4. [Backlog](docs/project/backlog.md)
5. [Kontrakt API](docs/api/api-contract.md)
6. [Architektura](docs/architecture/overview.md)
7. [Strategia testów](docs/testing/test-strategy.md)
8. [Rejestr ryzyk](docs/project/risk-register.md)
9. [Pełny indeks dokumentacji](docs/DOCUMENTATION_INDEX.md)

## Konwencje językowe

- dokumentacja projektowa i biznesowa: język polski;
- kod, testy, nazwy klas, metod, pakietów i komunikaty techniczne: język angielski;
- klienci API będą integrować się przez stabilne `error.code`, nie przez tekst `detail`.

## Status implementacji biznesowej

Nie zaimplementowano jeszcze endpointów tworzenia i wykorzystania kuponu, resolvera adresu klienta ani adaptera GeoIP. Ich istnienie zostanie zadeklarowane dopiero po przejściu właściwych kryteriów Definition of Done.
