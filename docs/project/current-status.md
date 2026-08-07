# Aktualny status projektu

- **Data:** 2026-08-07
- **Termin:** 2026-08-10, koniec dnia
- **Faza:** `CONCURRENCY_EVIDENCE_DONE_AND_VERIFIED`
- **Active task:** `EMP-008 — refinement coverage/JaCoCo`
- **EMP-004 refinement:** `ACCEPTED`
- **Implementation EMP-004:** `DONE_AND_VERIFIED`; verification remediation `COMPLETED`
- **Implementation allowed:** `YES` dla `EMP-004` na podstawie accepted własnego refinementu
- **EMP-005:** `MERGED_INTO_EMP-004`; implementation i evidence ownerem jest EMP-004
- **EMP-006:** `DONE_AND_VERIFIED`
- **EMP-006 refinement:** `ACCEPTED`
- **Implementation EMP-006:** `DONE_AND_VERIFIED`
- **Implementation allowed EMP-006:** `YES`
- **EMP-007:** `DONE_AND_VERIFIED`
- **EMP-008:** `PLANNED`; JaCoCo i warning budget pozostają poza aktywnym scope
- **EMP-009:** `DONE_AND_VERIFIED`; refinement `ACCEPTED`, evidence `COMPLETE`, implementation `DONE_AND_VERIFIED`, implementation allowed `YES`
- **Kod aplikacji:** create coupon, Client IP/GeoIP i transactional redemption są zaimplementowane oraz lokalnie zweryfikowane
- **OpenAPI/Swagger UI:** canonical `/openapi.yaml` zawiera `createCoupon` i `redeemCoupon`; Swagger UI używa tej specyfikacji
- **Javadoc/DocLint policy:** `ACTIVE_AND_VERIFIED`
- **Runtime verification:** `LOCAL_EMP009_GATE_PASS`
- **Historyczne evidence bootstrapu:** `BOOTSTRAP_DONE_AND_VERIFIED`, `LOCAL_DOCKER_GATE_PASS`
- **Historyczne evidence EMP-003:** `CREATE_COUPON_DONE_AND_VERIFIED`, `LOCAL_EMP003_GATE_PASS`
- **Historyczne evidence EMP-007:** `OPENAPI_DOCUMENTATION_DONE_AND_VERIFIED`, `LOCAL_EMP007_GATE_PASS`
- **Evidence EMP-009:** `CONCURRENCY_EVIDENCE_DONE_AND_VERIFIED`, `LOCAL_EMP009_GATE_PASS`

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

EMP-008 — refinement coverage/JaCoCo. EMP-009 zakończyło ograniczony scope: exact 90 exhausted, 10 unikalnych userId, dokładnie jeden last-slot user oraz checker; pełny gate potwierdził evidence `COMPLETE`.

## Blokery

Brak blockerów. EMP-008 pozostaje `PLANNED` do własnego refinementu; EMP-009 jest `DONE_AND_VERIFIED`.
