# Kontrakt REST API

## Stan implementacji

`POST /api/v1/coupons` jest `DONE_AND_VERIFIED`. Jego canonical machine-readable schema znajduje się w [openapi.yaml](openapi.yaml) i jest udostępniana testerom jako `/openapi.yaml` oraz przez Swagger UI pod `/swagger-ui`. Endpoint wykorzystania kuponu pozostaje niezaimplementowany.

## Zasady wspólne

- base path: `/api/v1`;
- format: JSON UTF-8;
- czas: ISO-8601 UTC;
- identyfikatory: UUID;
- błędy: `application/problem+json` z rozszerzeniem `code`;
- uwierzytelnianie: poza zakresem zgodnie z treścią zadania;
- tekst `detail` nie jest kontraktem integracyjnym; kontraktem jest `status` i `code`.

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

- `code`: 3–64, `[A-Za-z0-9_-]`, trimowany;
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

Walidacja:

- `code` w ścieżce podlega tej samej canonicalizacji co przy tworzeniu;
- `userId`: po trimie 1–128 znaków, bez znaków kontrolnych;
- IP pochodzi z połączenia lub zaufanego proxy, nie z body.

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
  "detail": "Coupon WIOSNA has reached its usage limit.",
  "instance": "/api/v1/coupons/WIOSNA/redemptions",
  "code": "COUPON_EXHAUSTED"
}
```

## Stabilne kody błędów

| HTTP | `code` | Znaczenie |
|---:|---|---|
| 400 | `INVALID_REQUEST` | niepoprawny JSON, pole lub format |
| 403 | `COUNTRY_NOT_ALLOWED` | kraj IP nie odpowiada krajowi kuponu |
| 404 | `COUPON_NOT_FOUND` | canonical code nie istnieje |
| 409 | `COUPON_CODE_CONFLICT` | kod istnieje bez względu na wielkość liter |
| 409 | `COUPON_ALREADY_REDEEMED` | użytkownik wykorzystał kupon wcześniej |
| 409 | `COUPON_EXHAUSTED` | osiągnięto `maxUses` |
| 503 | `GEOLOCATION_UNAVAILABLE` | nie można wiarygodnie ustalić kraju |
| 500 | `INTERNAL_ERROR` | nieoczekiwany błąd bez ujawniania szczegółów |

## Reguły bezpieczeństwa odpowiedzi

- brak stack trace i nazw tabel w odpowiedzi;
- brak surowego IP;
- brak treści odpowiedzi dostawcy GeoIP;
- request ID może zostać zwrócony nagłówkiem `X-Request-Id`;
- nieznane błędy są logowane z correlation ID, ale odpowiedź pozostaje ogólna.

## Elementy poza MVP

- GET kuponu;
- lista kuponów;
- update/delete;
- data ważności;
- auth i role;
- idempotency key;
- bulk redemption.
