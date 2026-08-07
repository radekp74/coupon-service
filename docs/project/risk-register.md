# Rejestr ryzyk

| ID | Poziom | Status | Ryzyko | Mitigacja | Docelowy evidence |
|---|---|---|---|---|---|
| R-001 | CRITICAL | MITIGATED | przekroczenie `max_uses` przy równoległości | row lock + DB checks + test 100/10 | EMP-004: 3 × exact 10/90, counter=records=10 |
| R-002 | HIGH | MITIGATED | wielokrotne użycie przez tego samego użytkownika | unique constraint + kolejność walidacji | EMP-004: exact 1 success / 19 already redeemed, counter=records=1 |
| R-003 | MEDIUM | OPEN | hot coupon powoduje kontencję | krótka transakcja, brak calli sieciowych pod lockiem | timing test / review |
| R-004 | HIGH | MITIGATED | GeoIP timeout, limit lub awaria | provider port, HTTPS, 500 ms connect, 1 s response, brak retry, 503 | EMP-006 WireMock: timeout/status/success=false/body limit |
| R-005 | HIGH | MITIGATED | spoofing `Forwarded` / `X-Forwarded-For` | direct default, CIDR trust, right-to-left chain, malformed header fail-closed | EMP-006 resolver: direct spoofing, field-lines, precedence i trusted chain |
| R-006 | HIGH | OPEN | H2 ukrywa różnice PostgreSQL | wyłącznie Testcontainers PostgreSQL | EMP-008 |
| R-007 | HIGH | MITIGATED | case-insensitive duplikat w race condition | canonical field + unique constraint + exact-count concurrency test | EMP-003: 3 × 24 prób, każdorazowo 1 sukces, 23 konflikty, 1 rekord |
| R-008 | MEDIUM | MITIGATED | governance większe niż samo zadanie | jeden refinement MVP i ograniczony zestaw dokumentów | docs-check PASS |
| R-009 | MEDIUM | OPEN | timeout klienta po commit prowadzi do retry | naturalny unique user conflict i stabilny error code | EMP-004/007; EMP-005 jest scalone z EMP-004 |
| R-010 | MEDIUM | MITIGATED | licznik i redemption records się rozjadą | jedna transakcja + invariant integration test | EMP-004: insert/update rollback i invariant po każdej ścieżce fault |
| R-011 | HIGH | MITIGATED | utrwalanie lub logowanie IP zwiększa ryzyko prywatności | memory-only raw IP, brak hash/storage, redaction i provider data minimization | EMP-006 source/privacy review + testy wyjątków bez IP |
| R-012 | MEDIUM | OPEN | darmowy provider ma limit i brak SLA | port, external HTTPS config, jawny limit, brak deklaracji production readiness | EMP-006/011 |
| R-013 | MEDIUM | MITIGATED | flaky concurrency test | barrier start, bounded executor, exact assertions, brak sleep-based sync | EMP-004: 3 × 100/10, same-user i last-slot na PostgreSQL Testcontainers |
| R-014 | MEDIUM | OPEN | nadmierne użycie AI bez zrozumienia | małe checkpointy, ADR-y, review każdej decyzji | final technical review |
| R-015 | HIGH | MITIGATED | lokalny GeoIP stub zostaje przypadkowo aktywowany w produkcji | bean/profile guard + startup failure poza `local`/`test` + configuration test | EMP-006 profile startup tests |
| R-016 | HIGH | MITIGATED | OpenAPI i runtime API rozchodzą się | jeden canonical YAML pakowany do artefaktu + checker + HTTP test | EMP-007 |
| R-017 | MEDIUM | OPEN | mechaniczne komentarze obniżają czytelność i szybko się dezaktualizują | Javadoc tylko dla kontraktów i nieoczywistych decyzji + DocLint + review | EMP-007/011 |

| R-018 | HIGH | MITIGATED | parser IP wykonuje DNS lub akceptuje hostname | strict literal parser, brak `getByName` na niezweryfikowanym tekście, testy hostname/zone ID | EMP-006 literal parser tests |
| R-019 | HIGH | MITIGATED | prywatny lub specjalny adres jest wysyłany do publicznego GeoIP | IANA special-purpose policy przed adapterem HTTP | EMP-006 table-driven special-purpose tests |
| R-020 | MEDIUM | MITIGATED | długi łańcuch proxy powoduje koszt CPU lub niejednoznaczność | 4096 znaków, maks. 20 hopów, fail-closed | EMP-006 header boundary tests |
| R-021 | HIGH | MITIGATED | wielokrotne field-lines lub redirect/body dostawcy omijają granicę zaufania | fail-closed, redirect disabled, 16 KiB bounded read, testy WireMock | EMP-006 field-line, redirect exactly-one-request i body boundary tests |

| R-022 | HIGH | OPEN | GeoIP lub Client IP zostaje omyłkowo wykonane pod row lockiem | osobny non-transactional orchestrator i osobny proxied transaction bean | EMP-004 transaction boundary tests/review |
| R-023 | HIGH | OPEN | błędna kolejność already/exhausted/country daje niestabilny kontrakt | zamrożona precedence i kombinacyjne testy API | EMP-004 |
| R-024 | MEDIUM | OPEN | client-asserted userId może zostać podszyty bez auth | jawne ograniczenie zadania, bez deklaracji prawdziwej tożsamości | README/EMP-011 |
| R-025 | HIGH | OPEN | insert redemption commitnie się bez incrementu albo odwrotnie | jedna transakcja, conditional update i fault-injection rollback tests | EMP-004 |
| R-026 | MEDIUM | OPEN | retry po utracie 201 jest mylony z nowym użyciem | stabilny 409 `COUPON_ALREADY_REDEEMED`, jawny brak idempotency key | EMP-004/011 |
| R-027 | MEDIUM | OPEN | kontrakt UserId rozchodzi się między EMP-001, aplikacją i V1 | zaakceptowany amendment EMP-001 zamraża `^[!-~]{1,128}$` bez trimowania/normalizacji; implementacja EMP-004 musi dodać zgodne Bean Validation, PostgreSQL `CHECK`, OpenAPI i testy | EMP-004 implementation evidence |
| R-028 | MEDIUM | MITIGATED | formalne concurrency evidence może nie klasyfikować wszystkich wyników albo stanów końcowych | EMP-009 sprawdza exact 90 `COUPON_EXHAUSTED`, 10 unikalnych userId i dokładnie jednego last-slot usera; checker + pełny gate | EMP-009 verified evidence |

## Zasada statusu

Ryzyko nie jest oznaczane `MITIGATED` tylko dlatego, że istnieje plan. Status zmienia się dopiero po pojawieniu się dowodu w kodzie, teście lub zweryfikowanym procesie.
