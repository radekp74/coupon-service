# Aktualny status projektu

- **Data:** 2026-08-07
- **Termin:** 2026-08-10, koniec dnia
- **Faza:** `EMP011_FINAL_REVIEW`
- **Active task:** `EMP-011 — final review, README i closeout`
- **EMP-004 refinement:** `ACCEPTED`
- **Implementation EMP-004:** `DONE_AND_VERIFIED`; verification remediation `COMPLETED`
- **Implementation allowed:** `YES` dla `EMP-004` na podstawie accepted własnego refinementu
- **EMP-005:** `MERGED_INTO_EMP-004`; implementation i evidence ownerem jest EMP-004
- **EMP-006:** `DONE_AND_VERIFIED`
- **EMP-006 refinement:** `ACCEPTED`
- **Implementation EMP-006:** `DONE_AND_VERIFIED`
- **Implementation allowed EMP-006:** `YES`
- **EMP-007:** `DONE_AND_VERIFIED`
- **EMP-008:** `DONE_AND_VERIFIED`; refinement `ACCEPTED`, JaCoCo implementation `DONE_AND_VERIFIED`, coverage evidence `MEASURED_AND_VERIFIED`, implementation allowed `YES`
- **EMP-009:** `DONE_AND_VERIFIED`; refinement `ACCEPTED`, evidence `COMPLETE`, implementation `DONE_AND_VERIFIED`, implementation allowed `YES`
- **EMP-010:** `DONE_AND_VERIFIED`; refinement `ACCEPTED`, implementation `DONE_AND_VERIFIED`, `Implementation-Allowed: YES`, CI/delivery/observability evidence `MEASURED_AND_VERIFIED`, finalny verified SHA `35fa7c7e07ac341a410fad38c8ced030ac30ed25`
- **EMP-011:** `IN_PROGRESS`; refinement `ACCEPTED`, implementation `IN_PROGRESS`, `Implementation-Allowed: YES`, final review evidence `MEASURED`, public repo evidence `NOT_MEASURED`
- **Kod aplikacji:** create coupon, Client IP/GeoIP i transactional redemption są zaimplementowane oraz lokalnie zweryfikowane
- **OpenAPI/Swagger UI:** canonical `/openapi.yaml` zawiera `createCoupon` i `redeemCoupon`; Swagger UI używa tej specyfikacji
- **Javadoc/DocLint policy:** `ACTIVE_AND_VERIFIED`
- **Runtime verification:** `LOCAL_EMP010_GATE_PASS`, `GITHUB_CI_EMP010_PASS`
- **Historyczne evidence bootstrapu:** `BOOTSTRAP_DONE_AND_VERIFIED`, `LOCAL_DOCKER_GATE_PASS`
- **Historyczne evidence EMP-003:** `CREATE_COUPON_DONE_AND_VERIFIED`, `LOCAL_EMP003_GATE_PASS`
- **Historyczne evidence EMP-007:** `OPENAPI_DOCUMENTATION_DONE_AND_VERIFIED`, `LOCAL_EMP007_GATE_PASS`
- **Evidence EMP-009:** `CONCURRENCY_EVIDENCE_DONE_AND_VERIFIED`, `LOCAL_EMP009_GATE_PASS`
- **Evidence EMP-010:** `CI_DELIVERY_OBSERVABILITY_DONE_AND_VERIFIED`, `LOCAL_EMP010_GATE_PASS`, `GITHUB_CI_35fa7c7_PASS`

## Ukończone i zweryfikowane

- `EMP-000`, `EMP-001`, `EMP-002`, `EMP-003`, `EMP-004`, `EMP-006` i `EMP-007` mają status `DONE_AND_VERIFIED`;
- `POST /api/v1/coupons` działa z case-insensitive uniqueness;
- Client IP, trusted proxy, public-IP policy, adapter GeoIP i local/test stub przeszły pełny gate;
- Swagger UI pokazuje canonical OpenAPI wyłącznie dla endpointów istniejących w runtime;
- Docker smoke używa dynamicznego portu loopback i sprząta wyłącznie własny stos.

## EMP-004 — verification remediation completed

Audyt EMP-008/EMP-009 uczciwie wykrył, że wcześniejszy closeout nie zawierał całego evidence wymaganego przez accepted refinement. Kontrakt nie zmienił się. Remediation dodało testy value object i orchestratora, HTTP 403/503, exact 1/19 same-user, exact 1/1 last-slot, dowód blokady per-row, rollbacki PostgreSQL i kontrolę innego constraintu; pełny lokalny gate przeszedł.

Implementacja już dostarczyła:

- `POST /api/v1/coupons/{code}/redemptions`;
- snapshot lookup przed GeoIP;
- Client IP i GeoIP poza transakcją;
- osobny proxied bean dla krótkiej transakcji;
- PostgreSQL `READ COMMITTED` i `SELECT ... FOR UPDATE`;
- precedence `country → already redeemed → exhausted` pod lockiem;
- atomowy insert redemption i conditional increment;
- rollback, named unique constraint i complete exact-count concurrency evidence;
- OpenAPI, Swagger UI i Javadoc jako obowiązkowy element implementacji.

## Zaakceptowane decyzje właściciela

1. Włączenie EMP-005 do implementacji i closeoutu EMP-004.
2. Opaque, case-sensitive UserId `^[!-~]{1,128}$`, bez trimowania i normalizacji; amendment EMP-001 wymaga zgodnego PostgreSQL, Bean Validation i OpenAPI.
3. Retry tego samego userId jako 409, bez replay pierwotnego 201.
4. Precedence: not found → GeoIP → country → already redeemed → exhausted.
5. `READ COMMITTED + SELECT FOR UPDATE`, bez custom lock timeout i automatycznego retry DB.

## Następny krok

Dokończyć EMP-011 w zamrożonym zakresie: finalny checker, pełny `make verify`, commit, post-commit `make delivery-check`, deterministyczny source export, push SSH i zielony GitHub Actions dla finalnego SHA. Dopiero po tym EMP-011 może przejść do `DONE_AND_VERIFIED`.

## Blokery

Brak blokera technicznego. EMP-010 jest `DONE_AND_VERIFIED`; EMP-011 nie zmienia kodu produkcyjnego ani publicznego API.

- **Evidence EMP-011:** `LOCAL_EMP011_GATE_PASS`; finalny GitHub Actions pozostaje wymagany do closeoutu.
