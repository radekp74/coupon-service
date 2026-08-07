# Podsumowanie refinementu EMP-008

- task: `EMP-008`; status: `READY`; stan: `ACCEPTED`;
- implementation: `NOT_STARTED`; implementation allowed: `YES`;
- baseline testów: 60 unit + 22 integration; JaCoCo: `NOT_CONFIGURED`;
- DocLint: 0 errors; warnings baseline: 42.

Warning baseline został sklasyfikowany: 18 realnych braków kontraktu (A), 3 techniczne bootstrap warnings (B), 2 warnings małej wartości (C) i 19 wymagających decyzji per typ (D). Przyszła implementacja naprawia A, przegląda D pojedynczo i zamraża wyłącznie uzasadnioną resztę przy zasadzie `new warnings = 0`.

Właściciel zaakceptował JaCoCo `0.8.15` w przyszłym Maven `verify`, raport HTML/XML, globalne LINE >= 80% i BRANCH >= 70% oraz jedno krytyczne minimum LINE >= 75% / BRANCH >= 65%. `Coverage-Evidence: NOT_MEASURED`: nie deklaruje aktualnego wyniku coverage i pierwszy raport musi zostać ręcznie przejrzany przed closeoutem.

Aktualne PostgreSQL/WireMock/HTTP/concurrency evidence pokrywa krytyczne invariants po remediation EMP-004 i closeoucie EMP-009. Nie stwierdzono krytycznej luki behavior coverage. Potencjalne B-luki to niezależne HTTP evidence malformed JSON i unknown field; nie są automatycznym scope bez raportu JaCoCo lub manualnego gap review.

Właściciel zaakceptował brak default exclusions, finalny justified warning budget <=5, `new warnings = 0`, testy dodatkowe wyłącznie z realnego report review oraz PIT `OUT_OF_SCOPE`. EMP-008 nie zmienia API, kodu produkcyjnego ani testów w tym checkpointcie.
