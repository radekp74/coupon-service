# Podsumowanie refinementu EMP-003

EMP-003 dostarcza pierwszy endpoint biznesowy: `POST /api/v1/coupons`.

Najważniejsze decyzje:

- kod prezentacyjny jest trimowany i zachowywany;
- canonical code to uppercase z `Locale.ROOT`;
- unikalność rozstrzyga `UNIQUE(normalized_code)` w PostgreSQL;
- create nie wykonuje `existsByCode`;
- UUID i czas są wstrzykiwane;
- conflict jest mapowany ze stanu PostgreSQL `23505` na 409 `COUPON_CODE_CONFLICT`;
- test concurrency wymaga dokładnie jednego sukcesu, bez `Thread.sleep`;
- operacja create ma machine-readable OpenAPI; pełny closeout wymaga lokalnego `make verify` z Docker Desktop.

Pełny kontrakt: [EMP-003.md](EMP-003.md).
