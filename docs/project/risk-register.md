# Rejestr ryzyk

| ID | Poziom | Status | Ryzyko | Mitigacja | Docelowy evidence |
|---|---|---|---|---|---|
| R-001 | CRITICAL | OPEN | przekroczenie `max_uses` przy równoległości | row lock + DB checks + test 100/10 | EMP-009 |
| R-002 | HIGH | OPEN | wielokrotne użycie przez tego samego użytkownika | unique constraint + kolejność walidacji | EMP-005/009 |
| R-003 | MEDIUM | OPEN | hot coupon powoduje kontencję | krótka transakcja, brak calli sieciowych pod lockiem | timing test / review |
| R-004 | HIGH | OPEN | GeoIP timeout, limit lub awaria | 500 ms connect, 1 s response, brak retry, 503 | EMP-006 |
| R-005 | HIGH | OPEN | spoofing `X-Forwarded-For` | trusted proxy config, remote address default | EMP-006 |
| R-006 | HIGH | OPEN | H2 ukrywa różnice PostgreSQL | wyłącznie Testcontainers PostgreSQL | EMP-008 |
| R-007 | HIGH | MITIGATED | case-insensitive duplikat w race condition | canonical field + unique constraint + exact-count concurrency test | EMP-003: 3 × 24 prób, każdorazowo 1 sukces, 23 konflikty, 1 rekord |
| R-008 | MEDIUM | MITIGATED | governance większe niż samo zadanie | jeden refinement MVP i ograniczony zestaw dokumentów | docs-check PASS |
| R-009 | MEDIUM | OPEN | timeout klienta po commit prowadzi do retry | naturalny unique user conflict i stabilny error code | EMP-005/007 |
| R-010 | MEDIUM | OPEN | licznik i redemption records się rozjadą | jedna transakcja + invariant integration test | EMP-004/008 |
| R-011 | HIGH | OPEN | utrwalanie IP zwiększa ryzyko prywatności | brak kolumny IP i redaction logów | schema + log tests |
| R-012 | MEDIUM | OPEN | darmowy provider nie nadaje się do dużej produkcji | port, external config, jawne ograniczenie w README | EMP-006/011 |
| R-013 | MEDIUM | OPEN | flaky concurrency test | barrier start, bounded executor, exact assertions, brak sleep-based sync | EMP-009 |
| R-014 | MEDIUM | OPEN | nadmierne użycie AI bez zrozumienia | małe checkpointy, ADR-y, review każdej decyzji | final technical review |
| R-015 | HIGH | OPEN | lokalny GeoIP stub zostaje przypadkowo aktywowany w produkcji | profile guard + startup failure poza `local`/`test` + configuration test | EMP-006/010 |

| R-016 | HIGH | MITIGATED | OpenAPI i runtime API rozchodzą się | jeden canonical YAML pakowany do artefaktu + checker + HTTP test | EMP-007 |
| R-017 | MEDIUM | OPEN | mechaniczne komentarze obniżają czytelność i szybko się dezaktualizują | Javadoc tylko dla kontraktów i nieoczywistych decyzji + DocLint + review | EMP-007/011 |

## Zasada statusu

Ryzyko nie jest oznaczane `MITIGATED` tylko dlatego, że istnieje plan. Status zmienia się dopiero po pojawieniu się dowodu w kodzie, teście lub zweryfikowanym procesie.
