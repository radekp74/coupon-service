# Kontrakt REST API

## Stan implementacji

Oba endpointy zadania są `DONE_AND_VERIFIED` i znajdują się w canonical machine-readable spec [openapi.yaml](openapi.yaml), serwowanej jako `/openapi.yaml` oraz przez Swagger UI pod `/swagger-ui`:

```text
POST /api/v1/coupons
POST /api/v1/coupons/{code}/redemptions
```

`docs/api/openapi.yaml` jest źródłem prawdy dla requestów, odpowiedzi, statusów, schema i przykładów. Ten dokument opisuje semantykę i najważniejsze reguły integracyjne.

## Zasady wspólne

- base path: `/api/v1`;
- format: JSON UTF-8;
- czas: ISO-8601 UTC;
- identyfikatory: UUID;
- błędy: `application/problem+json` z rozszerzeniem `code`;
- `X-Request-Id`: bezpieczny pojedynczy token wejściowy jest zachowywany, w przeciwnym razie serwer generuje UUID; rzeczywista wartość jest zwracana w odpowiedzi;
- uwierzytelnianie: poza zakresem zgodnie z treścią zadania;
- tekst `detail` nie jest kontraktem integracyjnym; klient integruje się przez HTTP status i `code`.

## Utworzenie kuponu

```http
POST /api/v1/coupons
Content-Type: application/json
```

Request:

```json
{
  "code": "WIOSNA",
  "maxUses": 100,
  "countryCode": "PL"
}
```

Walidacja:

- `code`: 3–64, `[A-Za-z0-9_-]`, trimowany; unikalność jest case-insensitive przez canonical `normalized_code`;
- `maxUses`: 1–1 000 000;
- `countryCode`: istniejący ISO 3166-1 alpha-2, normalizowany do uppercase.

Sukces:

```http
201 Created
```

```json
{
  "id": "149b508d-3797-466a-859f-5fb0770dcb0d",
  "code": "WIOSNA",
  "createdAt": "2026-08-06T12:30:00Z",
  "maxUses": 100,
  "currentUses": 0,
  "countryCode": "PL"
}
```

## Wykorzystanie kuponu

```http
POST /api/v1/coupons/{code}/redemptions
Content-Type: application/json
```

Request:

```json
{
  "userId": "customer-123"
}
```

Walidacja i semantyka:

- `code` podlega tej samej canonicalizacji co przy tworzeniu;
- `userId`: opaque, case-sensitive, 1–128 widocznych znaków ASCII U+0021–U+007E, regex `^[!-~]{1,128}$`, bez trimowania i normalizacji;
- Client IP pochodzi z połączenia albo jawnie zaufanego proxy, nigdy z body;
- GeoIP jest wykonywane przed krótką transakcją PostgreSQL;
- pod `SELECT ... FOR UPDATE` obowiązuje kolejność: country → already redeemed → exhausted;
- insert redemption i increment `current_uses` są atomowe.

Sukces:

```http
201 Created
```

```json
{
  "redemptionId": "c5f51cd5-c77e-4a5b-a6b7-b5085358a4b1",
  "couponCode": "WIOSNA",
  "userId": "customer-123",
  "redeemedAt": "2026-08-06T12:32:00Z",
  "remainingUses": 99
}
```

## Problem Details

Przykład:

```json
{
  "type": "urn:problem:coupon-exhausted",
  "title": "Coupon usage limit reached",
  "status": 409,
  "detail": "The coupon usage limit has been reached.",
  "instance": "/api/v1/coupons/WIOSNA/redemptions",
  "code": "COUPON_EXHAUSTED"
}
```

## Stabilne kody błędów

| HTTP | `code` | Znaczenie |
|---:|---|---|
| 400 | `INVALID_REQUEST` | niepoprawny JSON, pole lub format |
| 403 | `COUNTRY_NOT_ALLOWED` | kraj IP nie odpowiada krajowi kuponu; oczekiwany i rozpoznany kraj nie są ujawniane |
| 404 | `COUPON_NOT_FOUND` | canonical code nie istnieje |
| 409 | `COUPON_CODE_CONFLICT` | kod istnieje bez względu na wielkość liter |
| 409 | `COUPON_ALREADY_REDEEMED` | użytkownik wykorzystał kupon wcześniej |
| 409 | `COUPON_EXHAUSTED` | osiągnięto `maxUses` |
| 503 | `GEOLOCATION_UNAVAILABLE` | nie można wiarygodnie ustalić kraju |
| 500 | `INTERNAL_ERROR` | nieoczekiwany błąd bez ujawniania szczegółów |

## Reguły bezpieczeństwa odpowiedzi

- brak stack trace i nazw tabel w odpowiedzi;
- brak surowego IP i kraju rozpoznanego przez GeoIP w payloadzie błędu;
- brak treści odpowiedzi providera GeoIP;
- odpowiedź zawiera `X-Request-Id` do korelacji;
- nieznane błędy są logowane z correlation ID, ale publiczny Problem Details pozostaje ogólny.

## Elementy poza zakresem

- GET/lista kuponów;
- update/delete;
- data ważności;
- auth i role;
- `Idempotency-Key`;
- bulk redemption.
