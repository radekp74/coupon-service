# Podsumowanie refinementu EMP-006

## Status

- task: `EMP-006`;
- stan refinementu: `ACCEPTED`;
- implementacja: `NOT_STARTED`;
- review: właściciel zaakceptował security amendment i pięć decyzji.

## Problem

Przyszłe wykorzystanie kuponu musi ustalić kraj na podstawie IP. Bez osobnego kontraktu aplikacja mogłaby zaufać spoofowanemu `X-Forwarded-For`, wykonać DNS dla danych klienta, wysyłać prywatne adresy do dostawcy, mylić awarię GeoIP z niedozwolonym krajem albo trwale przechowywać IP.

## Zamrożony kierunek proponowany

- bezpieczny default `direct` oparty na remote address;
- forwarded headers tylko od jawnie zaufanego immediate peer;
- wybór pierwszego niezaufanego hopu od prawej;
- `Forwarded` przed XFF, bez fallbacku po błędzie `Forwarded`;
- strict IPv4/IPv6 literal parsing bez DNS;
- publiczny GeoIP za portem;
- domyślny demo adapter `https://ipwho.is` z minimalnymi polami;
- 500 ms connect, 1 s response, brak retry;
- fail-closed jako 503 `GEOLOCATION_UNAVAILABLE`;
- wielokrotne fizyczne field-lines `Forwarded`/XFF są odrzucane, bez niejawnego scalania;
- redirecty 300–399 są odrzucane bez śledzenia `Location`;
- body dostawcy ma bounded limit 16 KiB przez `Content-Length` i streaming;
- wspierany jest wyłącznie ścisły podzbiór IPv6/portów oraz deployment contract boundary proxy;
- local/test stub bez publicznego bypass header;
- brak storage, hash i logowania raw IP;
- brak cache i multi-provider fallback w MVP.

## Dlaczego provider-neutral

Darmowy endpoint IPWhois deklaruje brak klucza, limit 1 000 requestów dziennie i brak gwarancji uptime. Port pozwala użyć go w demonstracji, nie wiążąc domeny ani przyszłego deploymentu z jednym dostawcą.

## Zaakceptowane decyzje właściciela

1. `ipwho.is` jako adapter demonstracyjny.
2. Jeden publiczny błąd 503 dla failure Client IP i GeoIP.
3. Brak cache, retry i drugiego providera.
4. Fail-closed dla błędnego `Forwarded` od zaufanego proxy.
5. Profil `local` w Compose ze stub country `PL`.

## Security amendment po pierwszym review

Pierwszy review otrzymał wynik `REJECT` z pięcioma lukami: wielokrotne field-lines, redirecty dostawcy, nieokreślony limit body, niejednoznaczne IPv6/porty i niedostateczny boundary proxy contract. Amendment doprecyzował fail-closed, brak redirectów, 16 384 bajty, składnię oraz obowiązek infrastruktury. Ponowna rekomendacja `ACCEPT` i formalna decyzja właściciela zostały udzielone 2026-08-06.

Refinement jest `ACCEPTED`, EMP-006 jest `READY`, a implementacja pozostaje `NOT_STARTED`.
