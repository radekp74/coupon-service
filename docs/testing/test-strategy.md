# Strategia testów

## Stan realizacji

EMP-002, EMP-003, EMP-004, EMP-006, EMP-007 i EMP-009 są `DONE_AND_VERIFIED`. EMP-004 ma PostgreSQL evidence 100/10 w trzech rundach, same-user 1/19, last-slot 1/1, per-row locking oraz rollbacków; EMP-009 sformalizował i zweryfikował jego exact concurrency evidence.

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
- spec opisuje wyłącznie zaimplementowane create i redemption;
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

## EMP-009 — verified concurrency evidence

EMP-009 jest `DONE_AND_VERIFIED` i nie stworzył nowych scenariuszy biznesowych. Formalnie mapuje `CreateCouponApiIT.concurrentCaseVariantsProduceExactlyOneCreatedCoupon` oraz metody concurrency `CouponRedemptionApiIT`; 100/10 klasyfikuje 90 odpowiedzi po publicznym `code=COUPON_EXHAUSTED` i sprawdza 10 unikalnych userId, a last-slot potwierdza zapis dokładnie jednego konkurenta. `make emp009-check` analizuje to evidence, a pełny gate uruchamia checker. JaCoCo i 42 warnings Javadoc są scope EMP-008, nie EMP-009.

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

Smoke publikuje aplikację wyłącznie na dynamicznym porcie `127.0.0.1`, odczytanym przez `docker compose port`; unikalna nazwa projektu i trap sprzątają wyłącznie własny stos.

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


## EMP-006 — Client IP i GeoIP

Testy implementacji EMP-006 nie mogą wykonywać requestów do publicznego Internetu. Parser, CIDR i trusted chain są testowane jednostkowo. Adapter HTTP używa lokalnego WireMock lub równoważnego stubu dla sukcesu, timeoutów, 429, 5xx, `success=false`, błędnego JSON i limitu body. Konfiguracja Spring potwierdza startup failure dla pustej trust listy, HTTP base URL oraz stubu poza `local`/`test`.

Wymagane asercje bezpieczeństwa:

- spoofed header od niezaufanego peer nie zmienia IP;
- `Forwarded` ma pierwszeństwo i nie fallbackuje po błędzie;
- parser nie wykonuje DNS;
- adresy specjalnego przeznaczenia nie docierają do provider stubu;
- failure wykonuje dokładnie jedno wywołanie, bez retry;
- exception i log contract nie zawierają raw IP;
- canonical OpenAPI zawiera redemption, ponieważ EMP-004 jest zaimplementowane i zweryfikowane.
- wielokrotne physical `Forwarded`/XFF, conflict obu nagłówków i trusted boundary proxy są fail-closed zgodnie z kontraktem;
- redirect 300–399 nie wykonuje drugiego requestu, a limit 16 KiB obejmuje Content-Length i streaming;


## EMP-004 — zweryfikowane redemption

Refinement wymaga snapshot lookup przed GeoIP, osobnego proxied transaction bean, `READ COMMITTED`, `SELECT ... FOR UPDATE`, named unique constraint mapping oraz conditional increment. Canonical OpenAPI i Swagger UI są rozszerzone razem z endpointem.

`UserId` będzie testowany jako opaque i case-sensitive `^[!-~]{1,128}$`, bez trimowania i normalizacji. Testy integracyjne muszą potwierdzić zgodność Bean Validation, PostgreSQL `CHECK` i OpenAPI; ta migracja nie należy do dokumentacyjnego checkpointu.

Nowe wymagane scenariusze po akceptacji:

- 404 nie wywołuje GeoIP;
- GeoIP failure i wrong country nie rozpoczynają transakcji;
- precedence pod lockiem: country, already redeemed, exhausted;
- 100 requestów / limit 10 daje dokładnie 10 sukcesów i 90 exhausted;
- 20 requestów tego samego userId daje dokładnie 1 sukces i 19 already redeemed;
- wyścig o ostatnie miejsce daje dokładnie jeden sukces;
- fault injection po insercie dowodzi rollbacku;
- `current_uses == count(coupon_redemptions)`;
- retry tego samego userId daje 409 bez replay;
- wszystkie testy używają PostgreSQL i deterministycznego GeoIP stubu, bez `Thread.sleep`.
