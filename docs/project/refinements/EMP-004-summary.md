# Podsumowanie refinementu EMP-004

## Status

- task: `EMP-004`;
- stan refinementu: `ACCEPTED`;
- review: `PASS`;
- implementation allowed: `YES`;
- EMP-005: `MERGED_INTO_EMP-004`; user-once jest obowiązkowym invariantem tej samej atomowej transakcji i evidence.

## Problem

Redemption łączy zewnętrzne GeoIP, regułę kraju, ograniczony licznik, jedno użycie przez użytkownika i zapis dwóch tabel. Naiwne `find → if → save`, call GeoIP pod lockiem albo osobne commity dla insertu i licznika prowadzą do przekroczenia limitu, długich blokad albo driftu danych.

## Proponowany kierunek

- snapshot kuponu przed GeoIP, aby 404 nie wykonywało calla zewnętrznego;
- Client IP i GeoIP poza transakcją;
- osobny proxied bean dla krótkiej transakcji;
- PostgreSQL `READ COMMITTED` i `SELECT ... FOR UPDATE`;
- kolejność pod lockiem: country, already redeemed, exhausted;
- insert redemption i conditional increment w jednym commit;
- named unique constraint jako ostatnia ochrona user-once;
- retry tego samego userId daje 409, bez replay pierwotnego 201;
- exact-count concurrency na PostgreSQL bez `Thread.sleep`;
- canonical OpenAPI i Swagger UI aktualizowane razem z endpointem.

## Najważniejsze invariants

```text
current_uses <= max_uses
current_uses == count(coupon_redemptions)
max one redemption for (coupon_id, user_id)
```

## Zaakceptowane decyzje właściciela

1. Włączyć EMP-005 do implementacji i closeoutu EMP-004.
2. Użyć opaque, case-sensitive `userId` `^[!-~]{1,128}$`, bez trimowania i normalizacji; Bean Validation, PostgreSQL, OpenAPI i testy muszą być zgodne.
3. Zwracać 409 dla retry, bez idempotentnego replay.
4. Przyjąć precedence not found → GeoIP unavailable → wrong country → already redeemed → exhausted.
5. Użyć READ COMMITTED + row lock, bez retry i custom lock timeout.

Refinement jest zamrożony. EMP-004 ma status `READY`, a implementacja pozostaje `NOT_STARTED` do osobnego checkpointu.
