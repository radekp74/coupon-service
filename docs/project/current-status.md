# Aktualny status projektu

- **Data:** 2026-08-06
- **Termin:** 2026-08-10, koniec dnia
- **Faza:** `CREATE_COUPON_DONE_AND_VERIFIED`
- **Active task:** `EMP-004`
- **Accepted refinement:** `EMP-003`; `EMP-004` wymaga własnego accepted refinementu
- **Implementation allowed:** `NO` dla `EMP-004` do czasu zaakceptowania jego refinementu
- **Kod aplikacji:** `CREATE_COUPON_DONE_AND_VERIFIED`
- **Runtime verification:** `LOCAL_EMP003_GATE_PASS`

## Ukończone i zweryfikowane

- `EMP-000`, `EMP-001` i `EMP-002` mają status `DONE_AND_VERIFIED`; historyczny stan EMP-002: `BOOTSTRAP_DONE_AND_VERIFIED`, evidence: `LOCAL_DOCKER_GATE_PASS`;
- Java 21, Spring Boot, Maven Wrapper, PostgreSQL 18, Flyway, Testcontainers, Dockerfile i Docker Compose przeszły lokalny pełny gate;
- publiczny branch `main` wskazuje commit `e58d094` i był czysty przed rozpoczęciem EMP-003;
- refinement `EMP-003` został zaakceptowany przed implementacją.
- `EMP-003` przeszedł lokalny `make verify`: 6 testów unit, 4 testy `DatabaseMigrationIT` i 4 testy `CreateCouponApiIT` bez błędów;
- test współbieżny wykonał 3 rundy po 24 próby: w każdej dokładnie 1 odpowiedź `201`, 23 odpowiedzi `409` i 1 rekord;
- runtime Compose na porcie 18080 potwierdził `UP`, create `201`, duplicate `409 COUPON_CODE_CONFLICT` i bezpieczne `400 INVALID_REQUEST`.

## Zweryfikowany EMP-003

- `POST /api/v1/coupons`;
- `CouponCode` z trimem i canonicalizacją `Locale.ROOT`;
- `CountryCode` walidowany względem ISO 3166-1 alpha-2;
- niezmienny model `Coupon`;
- use case z wstrzykiwanym `Clock` i generatorem UUID;
- port repozytorium oraz adapter `JdbcClient` wykonujący pojedynczy `INSERT`;
- mapowanie PostgreSQL SQLSTATE `23505` na `COUPON_CODE_CONFLICT`;
- Problem Details dla invalid request, conflict i unexpected error;
- unit tests domeny i use case;
- HTTP integration tests z realnym PostgreSQL;
- concurrent create: barrier, bounded executor, exact success/conflict counts;
- statyczny checker kontraktu EMP-003.

## Następny krok

Przygotować i zaakceptować refinement `EMP-004` przed implementacją transakcyjnego wykorzystania kuponu.

## Blokery

Brak blockerów projektowych. Port 8080 pozostaje zajęty przez Splitino; wszystkie bramki EMP-003 użyły izolowanego portu 18080.
