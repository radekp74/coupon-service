# Review checklist EMP-006

## Status

- [x] task ma stabilne ID i własny refinement;
- [x] cel oraz poza zakresem są jawne;
- [x] rozdzielono Client IP od GeoIP i domeny kuponu;
- [x] opisano direct mode i trusted proxy mode;
- [x] opisano kolejność `Forwarded` / `X-Forwarded-For`;
- [x] opisano algorytm right-to-left i failure policy;
- [x] parser nie może wykonywać DNS;
- [x] istnieją limity długości i hopów;
- [x] wiele fizycznych field-lines failuje bez niejawnego scalania;
- [x] redirect 300–399 nie śledzi `Location`;
- [x] limit body 16 KiB obejmuje Content-Length i streaming;
- [x] IPv6/porty mają ścisły bezpieczny podzbiór;
- [x] boundary proxy contract wymaga jednego kanonicznego nagłówka i trusted CIDR;
- [x] adresy specjalnego przeznaczenia są blokowane przed publicznym providerem;
- [x] dostawca jest ukryty za portem;
- [x] timeouty, brak retry i 429/5xx są rozstrzygnięte;
- [x] local/test stub ma guard profilu;
- [x] raw IP nie jest utrwalany ani logowany;
- [x] opisano testy bez publicznego Internetu;
- [x] OpenAPI nie deklaruje przedwcześnie redemption;
- [x] kryteria akceptacji są mierzalne;
- [x] wymagany evidence jest jawny;
- [x] `CODEX_PROMPT.md` jest zabroniony;
- [x] właściciel zaakceptował domyślnego providera;
- [x] właściciel zaakceptował wspólny kod 503;
- [x] właściciel zaakceptował brak cache/retry/fallback;
- [x] właściciel zaakceptował fail-closed dla błędnego `Forwarded`;
- [x] właściciel zaakceptował local stub `PL` w profilach `local`/`test`;
- [x] metadata zmienione na `ACCEPTED` i `Implementation-Allowed: YES`.

## Wynik

`ACCEPTED`. Implementacja EMP-006 może rozpocząć się w osobnym checkpointcie i jest obecnie `NOT_STARTED`.
