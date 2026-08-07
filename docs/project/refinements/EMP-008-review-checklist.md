# Review checklist EMP-008

- [x] Refinement nie dodaje funkcji, endpointu ani JaCoCo w tym checkpointcie.
- [x] Potwierdzono brak JaCoCo w aktualnym POM i brak baseline’u LINE/BRANCH.
- [x] Zmapowano aktualne 60 unit i 22 integration tests zamiast kopiować luki sprzed remediation EMP-004.
- [x] Rozdzielono A/B/C/D dla luk: brak A, B dla malformed JSON/unknown field tylko po raporcie.
- [x] Zaproponowano HTML/XML, `verify` gate, LINE/BRANCH, integration Surefire/Failsafe i ochronę pakietów krytycznych.
- [x] Zakazano automatycznego obniżania progów, suppressowania failure i exclusions business/security logic.
- [x] Zdefiniowano manualny missed-branch review oraz jakość testów zgodną z EMP-004/009.
- [x] DocLint errors=0 pozostaje obowiązkowe; 42 warnings sklasyfikowano jako A=18, B=3, C=2, D=19 i ustalono policy `new warnings = 0`.
- [x] PIT jest jawnie `OUT_OF_SCOPE`.
- [x] Właściciel zaakceptował 0.8.15, global 80/70, critical 75/65, no-default-exclusions, warning budget <=5, report-driven test remediation i PIT `OUT_OF_SCOPE`.
- [x] `Coverage-Evidence: NOT_MEASURED`; `READY` oznacza wyłącznie dozwoloną implementację, nie osiągnięty threshold.
- [x] `ACCEPTED` ustawia `Implementation-Allowed: YES`, a implementation pozostaje `NOT_STARTED`.

## Rekomendacja

`ACCEPTED`: owner Radosław Piątek, 2026-08-07. Przyszła implementacja jest ograniczona do JaCoCo, report-driven wartościowych testów, Javadoc remediation i implementacyjnego checkera; nie rozszerza API ani biznesowego scope.
