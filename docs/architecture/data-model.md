# Model danych

## `coupons`

| Kolumna | Typ | Znaczenie |
|---|---|---|
| `id` | `uuid` | techniczny identyfikator kuponu |
| `code` | `varchar(64)` | kod w formie prezentacyjnej |
| `normalized_code` | `varchar(64)` | canonical code używany do lookupu i unikalności |
| `created_at` | `timestamptz` | czas utworzenia w UTC |
| `max_uses` | `integer` | maksymalna liczba użyć |
| `current_uses` | `integer` | liczba zatwierdzonych użyć |
| `country_code` | `char(2)` | ISO 3166-1 alpha-2 |

Ograniczenia zaimplementowane w migracji `V1__create_coupon_tables.sql`:

```sql
PRIMARY KEY (id)
UNIQUE (normalized_code)
CHECK (max_uses > 0 AND max_uses <= 1000000)
CHECK (current_uses >= 0 AND current_uses <= max_uses)
CHECK (code ~ '^[A-Za-z0-9_-]{3,64}$' AND code = btrim(code))
CHECK (normalized_code ~ '^[A-Z0-9_-]{3,64}$')
CHECK (normalized_code = upper(code))
CHECK (country_code ~ '^[A-Z]{2}$')
```

## `coupon_redemptions`

| Kolumna | Typ | Znaczenie |
|---|---|---|
| `id` | `uuid` | identyfikator użycia |
| `coupon_id` | `uuid` | referencja do kuponu |
| `user_id` | `varchar(128)` | identyfikator użytkownika z requestu |
| `resolved_country_code` | `char(2)` | kraj ustalony w momencie użycia |
| `redeemed_at` | `timestamptz` | czas zatwierdzonego użycia |

Ograniczenia zaimplementowane w migracji `V1__create_coupon_tables.sql`:

```sql
PRIMARY KEY (id)
FOREIGN KEY (coupon_id) REFERENCES coupons(id)
UNIQUE (coupon_id, user_id)
CHECK (resolved_country_code ~ '^[A-Z]{2}$')
```

V1 sprawdza dla `user_id` wyłącznie niepustość po trimie. Zaakceptowany amendment EMP-001 dla endpointu redemption wymaga podczas implementacji EMP-004 osobnej migracji z `CHECK` równoważnym `^[!-~]{1,128}$`; `userId` będzie opaque, case-sensitive, bez trimowania i normalizacji.

## Canonicalizacja kodu

Publiczny kod:

1. jest trimowany;
2. musi składać się z 3–64 znaków ASCII: litery, cyfry, `_` lub `-`;
3. `normalized_code` powstaje przez `toUpperCase(Locale.ROOT)`;
4. `WIOSNA`, `wiosna` i `WiOsNa` wskazują ten sam kupon;
5. oryginalna wartość po trimie może zostać zachowana w `code` do prezentacji.

Ograniczenie do ASCII jest świadomym uproszczeniem kuponów technicznych i eliminuje zależność od collation oraz locale-specific case folding.

## Spójność licznika

`current_uses` jest denormalizowanym licznikiem wymaganym przez domenę i odpowiedzi API. Każde zwiększenie następuje w tej samej transakcji co zapis `coupon_redemptions`. Testy integracyjne sprawdzają zgodność:

```text
current_uses == count(coupon_redemptions for coupon)
```

## Prywatność

Surowy adres IP nie jest utrwalany. Rejestr użycia przechowuje wyłącznie rozpoznany kraj, ponieważ jest to minimalny zakres potrzebny do audytu reguły biznesowej.

## Stan wdrożenia

Schemat V1 oraz runtime evidence EMP-002/003 są zweryfikowane. `CreateCouponApiIT` potwierdza zapis prezentacyjnego i canonical code oraz exact-count concurrent create. Tabela `coupon_redemptions` i jej constrainty istnieją, lecz use case redemption nie jest jeszcze zaimplementowany. Zaakceptowany amendment EMP-001 wymaga, aby implementacja EMP-004 dodała migrację egzekwującą pełny kontrakt `userId`; obecny V1 nie jest samodzielnym dowodem takiego enforcementu.
