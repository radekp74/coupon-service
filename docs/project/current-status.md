# Aktualny status projektu

- **Data:** 2026-08-06
- **Termin:** 2026-08-10, koniec dnia
- **Faza:** `CLIENT_IP_GEOIP_REFINEMENT_ACCEPTED`
- **Active task:** `EMP-006 — implementation`
- **EMP-006 refinement:** `ACCEPTED`
- **Implementation allowed:** `YES` dla `EMP-006` na podstawie zaakceptowanego własnego refinementu
- **Implementation EMP-006:** `NOT_STARTED`
- **Kod aplikacji:** `CREATE_COUPON_DONE_AND_VERIFIED`
- **OpenAPI/Swagger UI:** `DONE_AND_VERIFIED`
- **Javadoc/DocLint policy:** `ACTIVE_AND_VERIFIED`
- **Runtime verification:** `LOCAL_EMP007_GATE_PASS`
- **Historyczne evidence EMP-007:** `OPENAPI_DOCUMENTATION_DONE_AND_VERIFIED`

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

## EMP-006 — accepted refinement

- rozdzielono wiarygodne ustalenie Client IP od zewnętrznego GeoIP i domeny kuponu;
- default `direct` ignoruje spoofowane nagłówki;
- tryb trusted proxy wymaga CIDR i wybiera pierwszy niezaufany hop od prawej;
- zaplanowano ścisłe parsowanie IPv4/IPv6 bez DNS, limity nagłówków i fail-closed;
- security amendment po pierwszym review doprecyzował field-lines, redirecty, body 16 KiB, IPv6/porty i boundary proxy;
- provider pozostaje za portem, z HTTPS, 500 ms connect, 1 s response i bez retry;
- raw IP nie jest utrwalany ani logowany;
- local/test używa profilowego stubu bez publicznego bypassu;
- pierwszy review został odrzucony z powodu pięciu luk bezpieczeństwa, a security amendment je doprecyzował;
- właściciel formalnie zaakceptował amendment i pięć decyzji: adapter demonstracyjny `ipwho.is`, publiczne `503 GEOLOCATION_UNAVAILABLE`, brak cache/retry/fallbacku, fail-closed dla błędnego `Forwarded` oraz stub `PL` tylko w `local`/`test`;
- refinement jest `ACCEPTED`, natomiast implementacja Client IP i GeoIP nadal ma status `NOT_STARTED`.

## Następny krok

Rozpocząć osobny checkpoint implementacyjny `EMP-006`. Po zakończeniu EMP-006 wymagany będzie osobny refinement `EMP-004`.

## Blokery

Brak blockerów EMP-007. Port 8080 pozostaje zajęty przez Splitino; lokalne bramki używają izolowanego portu 18080.
