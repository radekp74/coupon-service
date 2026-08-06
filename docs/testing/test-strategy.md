# Strategia testów

## Stan realizacji

EMP-002 i EMP-003 są `DONE_AND_VERIFIED`. Kandydat EMP-007 dodaje test runtime canonical OpenAPI i Swagger UI oraz DocLint publicznych kontraktów. Redemption, GeoIP i ich concurrency evidence pozostają w kolejnych zadaniach.

## Cel

Testy mają dowodzić invariants i zachowania przy współbieżności, a nie wyłącznie zwiększać coverage.

## Warstwy

### Unit tests

Bez Spring context:

- `CouponCode` canonicalization i invalid characters;
- `CountryCode` normalization i invalid country;
- `UserId` validation;
- max uses bounds;
- domain rejection precedence;
- GeoIP response mapping;
- error mapping dla znanych wyjątków.

### Repository integration tests

Na PostgreSQL 18 przez Testcontainers:

- Flyway na pustej bazie;
- unique `normalized_code`;
- unique `(coupon_id,user_id)`;
- check constraints;
- `FOR UPDATE` serializuje jeden kupon;
- rollback insertu i incrementu;
- zgodność licznika z redemption records.

H2 jest zabronione jako substytut integracyjnej bazy.

### OpenAPI documentation tests

- canonical `/openapi.yaml` jest dostępne z artefaktu;
- Swagger UI jest dostępny i wskazuje canonical spec;
- Petstore jest wyłączony;
- spec nie zawiera niezaimplementowanego redemption;
- JAR zawiera `static/openapi.yaml`.

### HTTP/API tests

Przez pełny Spring context i realny PostgreSQL:

- create happy path;
- duplicate code variants;
- invalid request fields;
- redemption happy path;
- not found;
- wrong country;
- already redeemed;
- exhausted;
- GeoIP unavailable;
- content type i Problem Details schema;
- brak stack trace, SQL i IP w odpowiedzi.

### GeoIP adapter tests

WireMock:

- 200 + valid country;
- 200 + `success=false`;
- brak `country_code`;
- malformed JSON;
- HTTP 429;
- HTTP 500;
- timeout;
- IPv4 i IPv6 URL encoding;
- startup failure, gdy adapter `stub` zostanie wybrany poza profilem `local`/`test`;
- lokalny stub zwraca jawnie skonfigurowany kraj bez publicznego requestu.

### Concurrency tests

#### Limit

- utwórz kupon `maxUses=10`;
- przygotuj 100 unikalnych użytkowników;
- użyj `CountDownLatch`/barrier do wspólnego startu;
- wykonaj requesty przez bounded executor;
- assert exactly 10 created;
- assert exactly 90 exhausted;
- assert DB counter = 10;
- assert redemption count = 10.

#### Ten sam użytkownik

- limit wystarczająco duży;
- 20 równoległych requestów tego samego usera;
- dokładnie 1 sukces;
- pozostałe `COUPON_ALREADY_REDEEMED`;
- counter i records = 1.

#### Concurrent create

- 20 requestów z wariantami `WIOSNA`, `wiosna`, `WiOsNa`;
- dokładnie 1 sukces;
- pozostałe `COUPON_CODE_CONFLICT`;
- jeden rekord w bazie.

## Stabilność testów concurrency

- bez `Thread.sleep` jako mechanizmu synchronizacji;
- jawny timeout całego testu;
- executor zamykany w `finally`;
- niezależny container/schema per suite;
- test może być powtórzony kilka razy w dedykowanym profilu;
- assertions opierają się na exact counts, nie „co najmniej”.

## Pokrycie

- JaCoCo line minimum 80%;
- JaCoCo branch minimum 70%;
- krytyczne invariants muszą mieć jawne scenariusze nawet po osiągnięciu progu;
- excluded mogą być tylko trivial configuration/generated OpenAPI, z uzasadnieniem.

## Bramka

```bash
./mvnw -B clean verify
```

Finalnie `make verify` uruchamia tę komendę wraz z governance dokumentacji oraz kontenerowym smoke testem kompletnego stosu.

## Matryca

| Scenariusz | Unit | Repository | HTTP | Concurrency |
|---|---:|---:|---:|---:|
| normalizacja kodu | tak | tak | tak | tak |
| duplikat kodu | częściowo | tak | tak | tak |
| limit | tak | tak | tak | tak |
| jeden user | tak | tak | tak | tak |
| kraj | tak | nie | tak | nie |
| GeoIP failure | tak | nie | tak | nie |
| rollback | nie | tak | pośrednio | tak |
| error contract | tak | nie | tak | nie |
