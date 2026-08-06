# Review checklist EMP-004

## Kompletność

- [x] endpoint, request i response są określone;
- [x] path code i userId mają mierzalną walidację;
- [x] Client IP i GeoIP wykorzystują zweryfikowane porty EMP-006;
- [x] zewnętrzny call jest poza transakcją;
- [x] snapshot lookup poprzedza GeoIP;
- [x] osobny proxied bean tworzy krótką transakcję;
- [x] `SELECT ... FOR UPDATE` i `READ COMMITTED` są opisane;
- [x] precedence błędów jest jawna;
- [x] insert i increment są atomowe;
- [x] unique constraint jest ostatnią ochroną user-once;
- [x] rollback i fault injection są opisane;
- [x] retry po utracie odpowiedzi ma jawny kontrakt;
- [x] OpenAPI, Swagger UI i Javadoc są częścią DoD;
- [x] exact-count concurrency nie używa `Thread.sleep`;
- [x] prywatność IP i userId jest opisana;
- [x] wymagany evidence closeoutu jest jawny;
- [x] OpenAPI nie jest zmieniane przed implementacją;
- [x] `CODEX_PROMPT.md` pozostaje zabroniony.

## Decyzje właściciela

- [x] EMP-005 zostaje włączone do EMP-004;
- [x] userId jest opaque, case-sensitive `^[!-~]{1,128}$`, bez trimowania i normalizacji; amendment EMP-001 wymaga zgodnego DB enforcement;
- [x] retry tego samego userId daje 409 bez replay;
- [x] zaakceptowano precedence błędów;
- [x] zaakceptowano READ COMMITTED + row lock bez retry/lock timeout.

## Metadata

- [x] `Stan-Refinementu: ACCEPTED`;
- [x] `Implementation-Allowed: YES`;
- [x] data i osoba akceptująca;
- [x] `Scope-Frozen: YES`;
- [x] `Review-Result: PASS`;
- [x] sekcja blokująca deklaruje `Brak.`.

## Wynik

`PASS`. Refinement jest zaakceptowany; implementacja może rozpocząć się wyłącznie w osobnym checkpointcie.
