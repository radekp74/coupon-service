# Coupon Service — zadanie rekrutacyjne backend

Mały REST-owy serwis do tworzenia i transakcyjnego wykorzystywania kuponów rabatowych. Rozwiązanie jest zbudowane jako modularny monolit w Java 21 / Spring Boot, z PostgreSQL jako jedynym source of truth dla limitów, unikalności i współbieżności.

## Zakres

Zaimplementowane są dokładnie dwa publiczne endpointy wymagane przez zadanie:

```text
POST /api/v1/coupons
POST /api/v1/coupons/{code}/redemptions
```

Pierwszy tworzy kupon z case-insensitive unikalnym kodem. Drugi rejestruje wykorzystanie kuponu dla `userId`, sprawdza kraj na podstawie Client IP/GeoIP i atomowo respektuje `maxUses` oraz regułę jednego użycia przez użytkownika.

Canonical kontrakt API znajduje się w [`docs/api/openapi.yaml`](docs/api/openapi.yaml), jest serwowany jako `/openapi.yaml` i renderowany przez Swagger UI.

## Najważniejsze decyzje architektoniczne

- **Modularny monolit zamiast mikroserwisów** — dwa use case'y nie uzasadniają narzutu sieciowego i operacyjnego.
- **PostgreSQL jako source of truth** — unikalność i invariants są bronione constraintami bazy, nie pamięcią procesu.
- **`SELECT ... FOR UPDATE` dla redemption** — wiele instancji aplikacji może bezpiecznie konkurować o ten sam kupon; hot coupon świadomie serializuje się na jednym rekordzie.
- **Krótka transakcja** — Client IP i GeoIP są rozwiązywane przed wejściem do proxied transaction bean; pod row lockiem nie ma HTTP.
- **Brak H2 w integration tests** — testy używają PostgreSQL 18 przez Testcontainers, aby rzeczywiście sprawdzać locking, constrainty i typy bazy.
- **Provider-neutral GeoIP** — logika domenowa nie zależy od konkretnego dostawcy; runtime używa `ipwho.is`, a local/test deterministycznego stubu.
- **Privacy by design** — surowy IP jest memory-only, nie jest utrwalany ani dodawany do metryk/logów.
- **Stabilne błędy** — publiczny kontrakt używa Problem Details i machine-readable `code`, a nie treści `detail`.

Więcej: [ADR-0001](docs/adr/ADR-0001-modular-monolith-and-technology-stack.md), [ADR-0002](docs/adr/ADR-0002-concurrency-and-redemption-consistency.md), [ADR-0003](docs/adr/ADR-0003-geolocation-client-ip-and-privacy.md).

## Szybki start

Wymagania: Java 21, Docker Desktop/Engine, Python 3.9+ oraz standardowe narzędzia shell. Globalny Maven nie jest wymagany — repo używa Maven Wrappera 3.9.16.

Pełna lokalna bramka jakości:

```bash
make verify
```

Uruchomienie aplikacji na porcie 18080:

```bash
make docker-up APP_PORT=18080
```

Po starcie:

```text
Application:  http://localhost:18080
Health:       http://localhost:18080/actuator/health
Swagger UI:   http://localhost:18080/swagger-ui
OpenAPI YAML: http://localhost:18080/openapi.yaml
Prometheus:   http://localhost:18080/actuator/prometheus
```

Zatrzymanie środowiska:

```bash
make docker-down
```

Na macOS Makefile domyślnie używa `/Applications/Docker.app/Contents/Resources/bin/docker`. W CI/Linux można nadpisać klienta bez zmiany repozytorium, np. `DOCKER=docker make verify`.

## Przykład — utworzenie kuponu

```bash
curl -i \
  -H 'Content-Type: application/json' \
  -H 'X-Request-Id: demo-create-001' \
  -d '{"code":"WIOSNA","maxUses":100,"countryCode":"PL"}' \
  http://localhost:18080/api/v1/coupons
```

Sukces zwraca `201 Created`, `X-Request-Id` oraz kupon z `currentUses: 0`.

## Przykład — wykorzystanie kuponu

Profil `local` używany przez Docker Compose ma deterministyczny GeoIP stub `PL`, więc kupon z krajem `PL` można wykorzystać bez publicznego requestu GeoIP:

```bash
curl -i \
  -H 'Content-Type: application/json' \
  -H 'X-Request-Id: demo-redeem-001' \
  -d '{"userId":"customer-123"}' \
  http://localhost:18080/api/v1/coupons/WIOSNA/redemptions
```

Sukces zwraca `201 Created` z `redemptionId`, `couponCode`, `userId`, `redeemedAt` i `remainingUses`.

Najważniejsze błędy redemption:

| HTTP | `code` | Znaczenie |
|---:|---|---|
| 403 | `COUNTRY_NOT_ALLOWED` | rozpoznany kraj nie odpowiada krajowi kuponu |
| 404 | `COUPON_NOT_FOUND` | kupon nie istnieje |
| 409 | `COUPON_ALREADY_REDEEMED` | ten `userId` użył kuponu wcześniej |
| 409 | `COUPON_EXHAUSTED` | osiągnięto `maxUses` |
| 503 | `GEOLOCATION_UNAVAILABLE` | kraju nie można wiarygodnie ustalić |

Pełny kontrakt: [docs/api/api-contract.md](docs/api/api-contract.md).

## Współbieżność i spójność

Redemption wykonuje się w dwóch fazach:

```text
request
→ snapshot lookup
→ Client IP
→ GeoIP poza transakcją
→ krótka transakcja PostgreSQL
   → SELECT coupon ... FOR UPDATE
   → country check
   → already-redeemed check
   → exhausted check
   → INSERT redemption
   → UPDATE current_uses
→ commit
```

Kolejność publicznych decyzji jest zamrożona jako: `not found → GeoIP unavailable → country → already redeemed → exhausted`; pod lockiem: `country → already redeemed → exhausted`.

Constraint `UNIQUE(coupon_id, user_id)` broni reguły jednego użycia również wtedy, gdy aplikacja popełni błąd. Fault-injection tests potwierdzają rollback całej transakcji, a testy współbieżności używają exact counts zamiast probabilistycznych asercji.

## Status końcowy

`EMP-011 = DONE_AND_VERIFIED`. Finalny implementation/review SHA `5c7d3f5b9e48ff88a90f11047f45b249b4ee7e65` przeszedł lokalny canonical `make verify`, deterministyczny delivery oraz zielony GitHub Actions `CI #5` na `main`. Publiczne repozytorium i reviewer-facing dokumentacja są finalnym stanem zadania.

## Evidence jakości

Finalny canonical gate po EMP-010/EMP-011 opiera się na:

- **112 unit tests** — zero failures/errors/skips;
- **23 integration tests** na PostgreSQL/Testcontainers — zero failures/errors/skips;
- JaCoCo globalnie: **95.76% LINE / 86.39% BRANCH**;
- JaCoCo critical aggregate: **95.06% LINE / 88.21% BRANCH**;
- exact concurrency evidence: m.in. 100 requestów przy limicie 10 daje dokładnie 10 sukcesów i 90 `COUPON_EXHAUSTED`;
- Docker runtime smoke: health, canonical OpenAPI, Swagger UI, Prometheus, request ID i structured JSON logging;
- GitHub Actions na `ubuntu-24.04` uruchamia ten sam canonical `DOCKER=docker make verify`;
- source delivery jest byte-reproducible na tym samym clean commicie i failuje przy stale checksum manifest.

DocLint kończy bez błędów. Pozostaje 5 świadomie zaakceptowanych warningów o niskiej wartości dokumentacyjnej; nie dodano mechanicznych komentarzy ani pustych konstruktorów tylko po to, aby wyzerować licznik.


### Zweryfikowany zakres historycznych gate'ów

- **EMP-000–EMP-003:** `DONE_AND_VERIFIED`;
- **EMP-007:** `DONE_AND_VERIFIED`;
- **OpenAPI/Swagger UI:** `DONE_AND_VERIFIED`;
- create endpoint, case-insensitive uniqueness i exact-count concurrent create pozostają objęte historycznym checkerem EMP-003;
- bieżący README nie używa checkpointów jako głównej narracji, ale zachowuje te krótkie markery dla traceability istniejących gate'ów.

### Zweryfikowany zakres EMP-003

- **EMP-000–EMP-003:** `DONE_AND_VERIFIED`;
- create endpoint, case-insensitive uniqueness i exact-count concurrent create pozostają objęte historycznym checkerem EMP-003;
- bieżący README nie używa checkpointów jako głównej narracji, ale zachowuje ten marker dla traceability istniejącego gate.

## Obserwowalność

- `X-Request-Id`: bezpieczny pojedynczy token jest zachowywany, brak/błędna/wielokrotna wartość daje UUID;
- MDC zawiera tylko `requestId` i jest czyszczone w `finally`;
- logi kontenerowe są strukturalnym JSON-em w formacie Logstash;
- `/actuator/prometheus` eksportuje niskokardynalne metryki create/redemption, Client IP, GeoIP i czasu transakcji;
- IP, `userId`, coupon code, request ID i rozpoznany kraj nie są labelami metryk.

## Świadome ograniczenia

To rozwiązanie jest celowo proporcjonalne do zadania rekrutacyjnego:

- brak auth/roles — `userId` jest **client-asserted opaque identifier**, nie zweryfikowaną tożsamością;
- brak `Idempotency-Key`; retry po utracie odpowiedzi 201 dla tego samego coupon/user daje stabilny 409 `COUPON_ALREADY_REDEEMED`, nie replay pierwotnej odpowiedzi;
- darmowy `ipwho.is` nie ma gwarantowanego produkcyjnego SLA ani budżetu dla dużego ruchu; port umożliwia późniejszą wymianę providera;
- hot coupon jest punktem serializacji na jednym row locku — dla tego zakresu correctness ma pierwszeństwo przed maksymalnym throughputem;
- `/actuator/prometheus` pokazuje gotowość do monitoringu, ale repo nie instaluje Grafany, alertingu ani SLO;
- hasło PostgreSQL w `docker-compose.yml` jest wyłącznie lokalnym/dev credentialem;
- brak automatycznych down migrations, deploymentu, Kubernetes/Terraform, SBOM/signingu i security-scanner stacku.

Znane warningi Flyway/PostgreSQL 18.4, Mockito dynamic-agent i deprecacja `@MockBean` są maintenance debt, nie blockerem: canonical Java 21 gate i GitHub Actions są zielone. Nie wykonywano ryzykownego dependency churn tylko dla kosmetycznego wyciszenia warningów.

## Recovery / rollback

- lokalnie `make docker-down` usuwa kontenery i wolumen projektu; oznacza to utratę lokalnych danych dev;
- produkcyjnego rollbacku schematu nie symulujemy automatycznym down migration;
- dla realnego środowiska recovery powinno opierać się na backup/restore albo forward fix zależnie od incydentu;
- aplikacja jest bezstanowa, więc jej rollback to wdrożenie poprzedniego zweryfikowanego artefaktu przy zachowaniu zgodności ze schematem DB.

## CI i deterministyczny eksport źródeł

GitHub Actions ma jeden główny job `verify`, minimalne `contents: read`, Java 21 i przypięte pełne SHA używanych actions. Obrazy bazowe Dockerfile są przypięte po digest.

Eksport:

```bash
make export-source
```

jest **read-only względem `CHECKSUMS.sha256`**. Packaging nie regeneruje manifestu; wymaga czystego commita i aktualnych checksumów, a przy stale/missing checksum lub zabronionym tracked file failuje. Archiwum zawiera wyłącznie tracked source files z deterministyczną kolejnością, timestampami, uprawnieniami i kompresją.

`make checksums` jest osobnym świadomym krokiem maintenance po legalnej zmianie tracked files — nie jest częścią eksportu.

## Dokumentacja

Najbardziej użyteczne dokumenty do review:

1. [Kontrakt API](docs/api/api-contract.md)
2. [Canonical OpenAPI](docs/api/openapi.yaml)
3. [Architektura](docs/architecture/overview.md)
4. [Strategia testów](docs/testing/test-strategy.md)
5. [Traceability wymagań](docs/product/requirements-traceability.md)
6. [Rejestr ryzyk](docs/project/risk-register.md)
7. [ADR-y](docs/adr/README.md)
8. [Aktualny status](docs/project/current-status.md)
9. [Indeks dokumentacji](docs/DOCUMENTATION_INDEX.md)

Historyczne refinementy i release history pozostają w repo jako evidence decyzji; README celowo opisuje finalne rozwiązanie zamiast historii checkpointów.
