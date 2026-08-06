# Aktualny status projektu

- **Data:** 2026-08-06
- **Termin:** 2026-08-10, koniec dnia
- **Faza:** `OPENAPI_DOCUMENTATION_DONE_AND_VERIFIED`
- **Active task:** `EMP-006`
- **Accepted refinement:** brak
- **Implementation allowed:** `NO` dla `EMP-006` do czasu accepted własnego refinementu
- **Kod aplikacji:** `CREATE_COUPON_DONE_AND_VERIFIED`
- **OpenAPI/Swagger UI:** `DONE_AND_VERIFIED`
- **Javadoc/DocLint policy:** `ACTIVE_AND_VERIFIED`
- **Runtime verification:** `LOCAL_EMP007_GATE_PASS`

## Ukończone i zweryfikowane

- `EMP-000`, `EMP-001`, `EMP-002` i `EMP-003` mają status `DONE_AND_VERIFIED`;
- Java 21, Spring Boot, Maven Wrapper, PostgreSQL 18, Flyway, Testcontainers, Dockerfile i Docker Compose przeszły lokalny pełny gate;
- `POST /api/v1/coupons` przeszedł unit, HTTP/PostgreSQL, concurrent create i Docker runtime smoke;
- publiczny branch przed EMP-007 wskazuje commit `f268556c6c5f2ddbcf52ff567d14befc76221381`.
- evidence bootstrapu: `BOOTSTRAP_DONE_AND_VERIFIED`, `LOCAL_DOCKER_GATE_PASS`;
- evidence EMP-003: `LOCAL_EMP003_GATE_PASS`.

## EMP-007 — zweryfikowane

- canonical `docs/api/openapi.yaml`;
- Swagger UI skonfigurowany do użycia `/openapi.yaml`;
- Maven pakuje canonical spec do classpath;
- publiczne kontrakty EMP-003 otrzymują znaczący Javadoc;
- DocLint i statyczny checker EMP-007 są częścią bramki;
- `OpenApiDocumentationIT`, DocLint, Maven clean verify i Docker smoke przeszły lokalnie.

## Zmiana kolejności

`EMP-004` jest czasowo `BLOCKED`. Publiczny endpoint redemption wymaga najpierw `EMP-006` (client IP i GeoIP) oraz aktualnego kontraktu tester-facing. Jest to zgodne z kolejnością fal zaakceptowaną w `EMP-001`.

## Następny krok

Przygotować i zaakceptować własny refinement `EMP-006`; implementacja nie jest jeszcze dozwolona. Po EMP-006 wymagany jest refinement `EMP-004`.

## Blokery

Brak blockerów EMP-007. Port 8080 pozostaje zajęty przez Splitino; lokalne bramki używają izolowanego portu 18080.
