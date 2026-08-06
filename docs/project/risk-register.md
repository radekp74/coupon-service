# Rejestr ryzyk

| ID | Poziom | Status | Ryzyko | Mitigacja | Docelowy evidence |
|---|---|---|---|---|---|
| R-001 | CRITICAL | OPEN | przekroczenie `max_uses` przy równoległości | row lock + DB checks + test 100/10 | EMP-009 |
| R-002 | HIGH | OPEN | wielokrotne użycie przez tego samego użytkownika | unique constraint + kolejność walidacji | EMP-005/009 |
| R-003 | MEDIUM | OPEN | hot coupon powoduje kontencję | krótka transakcja, brak calli sieciowych pod lockiem | timing test / review |
| R-004 | HIGH | MITIGATED | GeoIP timeout, limit lub awaria | provider port, HTTPS, 500 ms connect, 1 s response, brak retry, 503 | EMP-006 WireMock: timeout/status/success=false/body limit |
| R-005 | HIGH | MITIGATED | spoofing `Forwarded` / `X-Forwarded-For` | direct default, CIDR trust, right-to-left chain, malformed header fail-closed | EMP-006 resolver: direct spoofing, field-lines, precedence i trusted chain |
| R-006 | HIGH | OPEN | H2 ukrywa różnice PostgreSQL | wyłącznie Testcontainers PostgreSQL | EMP-008 |
| R-007 | HIGH | MITIGATED | case-insensitive duplikat w race condition | canonical field + unique constraint + exact-count concurrency test | EMP-003: 3 × 24 prób, każdorazowo 1 sukces, 23 konflikty, 1 rekord |
| R-008 | MEDIUM | MITIGATED | governance większe niż samo zadanie | jeden refinement MVP i ograniczony zestaw dokumentów | docs-check PASS |
| R-009 | MEDIUM | OPEN | timeout klienta po commit prowadzi do retry | naturalny unique user conflict i stabilny error code | EMP-005/007 |
| R-010 | MEDIUM | OPEN | licznik i redemption records się rozjadą | jedna transakcja + invariant integration test | EMP-004/008 |
| R-011 | HIGH | MITIGATED | utrwalanie lub logowanie IP zwiększa ryzyko prywatności | memory-only raw IP, brak hash/storage, redaction i provider data minimization | EMP-006 source/privacy review + testy wyjątków bez IP |
| R-012 | MEDIUM | OPEN | darmowy provider ma limit i brak SLA | port, external HTTPS config, jawny limit, brak deklaracji production readiness | EMP-006/011 |
| R-013 | MEDIUM | OPEN | flaky concurrency test | barrier start, bounded executor, exact assertions, brak sleep-based sync | EMP-009 |
| R-014 | MEDIUM | OPEN | nadmierne użycie AI bez zrozumienia | małe checkpointy, ADR-y, review każdej decyzji | final technical review |
| R-015 | HIGH | MITIGATED | lokalny GeoIP stub zostaje przypadkowo aktywowany w produkcji | bean/profile guard + startup failure poza `local`/`test` + configuration test | EMP-006 profile startup tests |
| R-016 | HIGH | MITIGATED | OpenAPI i runtime API rozchodzą się | jeden canonical YAML pakowany do artefaktu + checker + HTTP test | EMP-007 |
| R-017 | MEDIUM | OPEN | mechaniczne komentarze obniżają czytelność i szybko się dezaktualizują | Javadoc tylko dla kontraktów i nieoczywistych decyzji + DocLint + review | EMP-007/011 |

| R-018 | HIGH | MITIGATED | parser IP wykonuje DNS lub akceptuje hostname | strict literal parser, brak `getByName` na niezweryfikowanym tekście, testy hostname/zone ID | EMP-006 literal parser tests |
| R-019 | HIGH | MITIGATED | prywatny lub specjalny adres jest wysyłany do publicznego GeoIP | IANA special-purpose policy przed adapterem HTTP | EMP-006 table-driven special-purpose tests |
| R-020 | MEDIUM | MITIGATED | długi łańcuch proxy powoduje koszt CPU lub niejednoznaczność | 4096 znaków, maks. 20 hopów, fail-closed | EMP-006 header boundary tests |
| R-021 | HIGH | MITIGATED | wielokrotne field-lines lub redirect/body dostawcy omijają granicę zaufania | fail-closed, redirect disabled, 16 KiB bounded read, testy WireMock | EMP-006 field-line, redirect exactly-one-request i body boundary tests |

## Zasada statusu

Ryzyko nie jest oznaczane `MITIGATED` tylko dlatego, że istnieje plan. Status zmienia się dopiero po pojawieniu się dowodu w kodzie, teście lub zweryfikowanym procesie.
