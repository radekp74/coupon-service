# Rejestr decyzji

| ID | Data | Status | Decyzja | Uzasadnienie / konsekwencja |
|---|---|---|---|---|
| D-001 | 2026-08-06 | ACCEPTED | Dokumentacja jest częścią Definition of Done | stan i uzasadnienia nie mogą być odtwarzane po fakcie |
| D-002 | 2026-08-06 | ACCEPTED | Java 21 LTS | dojrzały toolchain i szeroka znajomość podczas rozmowy |
| D-003 | 2026-08-06 | ACCEPTED | Spring Boot 3.5.16 | świadomie konserwatywny maintained line zamiast novelty major |
| D-004 | 2026-08-06 | ACCEPTED | Maven Wrapper | powtarzalny build bez zależności od lokalnej wersji Maven |
| D-005 | 2026-08-06 | ACCEPTED | PostgreSQL 18 i Flyway | trwałość, constrainty i transakcyjne blokady |
| D-006 | 2026-08-06 | ACCEPTED | Spring JDBC / JdbcClient zamiast JPA | jawny SQL i czytelny mechanizm concurrency |
| D-007 | 2026-08-06 | ACCEPTED | Modularny monolit | brak uzasadnienia dla mikroserwisów w dwóch use case'ach |
| D-008 | 2026-08-06 | ACCEPTED | ASCII canonical code + `normalized_code` | stabilny case-insensitive lookup bez zależności od collation |
| D-009 | 2026-08-06 | ACCEPTED | `SELECT ... FOR UPDATE` w krótkiej transakcji | spójność między wieloma instancjami i prosty model „first come” |
| D-010 | 2026-08-06 | ACCEPTED | `UNIQUE(coupon_id,user_id)` | baza broni jednego użycia niezależnie od kodu aplikacji |
| D-011 | 2026-08-06 | ACCEPTED | GeoIP za portem, domyślnie ipwho.is | wymienność i deterministyczne testy |
| D-012 | 2026-08-06 | ACCEPTED | Surowy IP nie jest utrwalany ani logowany | minimalizacja danych i mniejszy zakres prywatności |
| D-013 | 2026-08-06 | ACCEPTED | `application/problem+json` + stabilny `code` | czytelny kontrakt błędów niezależny od tekstu |
| D-014 | 2026-08-06 | ACCEPTED | Testcontainers PostgreSQL, bez H2 | testy muszą odtwarzać locking, INET/UUID i constrainty produkcyjne |
| D-015 | 2026-08-06 | ACCEPTED | Brak Redis, Kafka i Kubernetes w MVP | prostota i brak problemu wymagającego tych komponentów |
| D-016 | 2026-08-06 | ACCEPTED | `EMP-001` pokrywa bounded tasks MVP | zachowanie dyscypliny bez biurokracji wielu refinementów |
| D-017 | 2026-08-06 | ACCEPTED | Awaria GeoIP daje 503, nie 403 | błąd infrastruktury nie może udawać decyzji biznesowej |
| D-018 | 2026-08-06 | ACCEPTED | Jedno użycie przez użytkownika jest częścią core | opcjonalne wymaganie ma wysoką wartość demonstracyjną i niski koszt domenowy |
| D-019 | 2026-08-06 | ACCEPTED | Lokalny GeoIP stub działa tylko w profilach `local`/`test` | umożliwia demo dla loopback bez publicznego bypassu i bez ryzyka produkcyjnego |
| D-020 | 2026-08-06 | ACCEPTED | Makefile używa `DOCKER ?= /Applications/Docker.app/Contents/Resources/bin/docker` | lokalny Docker Desktop nie musi być dostępny w `PATH`; zmienna pozostaje nadpisywalna dla CI i innych środowisk |
| D-021 | 2026-08-06 | ACCEPTED | `make export-source` tworzy filtrowany ZIP w `SOURCE_EXPORT_DIR` | przesyłana paczka jest powtarzalna, nie zawiera sekretów ani artefaktów i raportuje SHA-256 |
| D-022 | 2026-08-06 | ACCEPTED | Maven 3.9.16 jest przypięty wraz z SHA-512 dystrybucji | build nie zależy od globalnego Maven, a pierwszy download podlega kontroli integralności |
| D-023 | 2026-08-06 | ACCEPTED | Testcontainers 2.0.5 używa modułu i pakietu `org.testcontainers.postgresql` | kod odpowiada aktualnej strukturze 2.x, a nie niekompatybilnym przykładom 1.x |
| D-024 | 2026-08-06 | ACCEPTED | EMP-002 zawiera wieloetapowy Dockerfile i Docker Compose | Testcontainers weryfikuje integrację testową, lecz nie zastępuje powtarzalnego artefaktu uruchomienia całej aplikacji; obraz działa bez roota, a Compose zapewnia PostgreSQL i health gate |
| D-025 | 2026-08-06 | ACCEPTED | Create wykonuje pojedynczy `INSERT` bez preflight `existsByCode` | constraint PostgreSQL pozostaje jedynym autorytetem także przy wielu instancjach |
| D-026 | 2026-08-06 | ACCEPTED | Kod prezentacyjny jest zachowywany po trimie, a unikalność używa osobnego canonical code | czytelna odpowiedź API bez zależności spójności od collation |
| D-027 | 2026-08-06 | ACCEPTED | Każde kolejne zadanie implementacyjne otrzymuje własny refinement | EMP-001 pozostaje umbrella contract, ale nie zastępuje szczegółowego kontraktu tasku |
| D-028 | 2026-08-06 | ACCEPTED | `docs/api/openapi.yaml` jest canonical spec i jest serwowane przez Swagger UI | tester widzi dokładnie wersjonowany kontrakt; nie powstają dwa niezależne źródła prawdy |
| D-029 | 2026-08-06 | ACCEPTED | Znaczący Javadoc jest wymagany dla publicznych kontraktów, nie dla każdej zmiennej | dokumentujemy semantykę i ryzyka bez zalewania kodu komentarzami |
| D-030 | 2026-08-06 | ACCEPTED | EMP-007 i EMP-006 poprzedzają implementację EMP-004 | publiczny redemption nie może istnieć bez wiarygodnego kraju i tester-facing kontraktu; kolejność odpowiada EMP-001 |
| D-031 | 2026-08-06 | ACCEPTED | Client IP używa trybu `direct` jako bezpiecznego defaultu | nagłówki klienta nie mogą zmienić kraju bez jawnej granicy zaufania |
| D-032 | 2026-08-06 | ACCEPTED | Trusted proxy chain jest analizowany od prawej do lewej | pierwszy niezaufany hop ogranicza spoofing i wspiera wiele proxy |
| D-033 | 2026-08-06 | ACCEPTED | `Forwarded` ma pierwszeństwo, a błędna wartość nie fallbackuje do XFF | unika header confusion i zachowuje fail-closed |
| D-034 | 2026-08-06 | ACCEPTED | Domyślny adapter demo używa `https://ipwho.is` z minimalnym `fields` | brak klucza ułatwia review, a port zachowuje wymienność |
| D-035 | 2026-08-06 | ACCEPTED | GeoIP ma 500 ms connect, 1 s response i brak retry | request nie może długo blokować przyszłej transakcji redemption |
| D-036 | 2026-08-06 | ACCEPTED | Client IP i provider failures używają jednego publicznego 503 `GEOLOCATION_UNAVAILABLE` | klient nie potrzebuje szczegółów infrastruktury, a 403 pozostaje decyzją biznesową |
| D-037 | 2026-08-06 | ACCEPTED | Raw IP jest memory-only i nie powstaje jego hash | minimalizacja danych bez niepotrzebnego quasi-identyfikatora |
| D-038 | 2026-08-06 | ACCEPTED | Local/test używa profilowego stubu `PL`, bez bypass header | deterministyczne demo dla prywatnego IP kontenera bez osłabiania API |
| D-039 | 2026-08-06 | ACCEPTED | Brak cache, retry i multi-provider fallback w EMP-006 | ograniczony zakres zadania; rozszerzenia wymagają pomiarów i osobnej decyzji |
| D-040 | 2026-08-06 | ACCEPTED | Wielokrotne field-lines failują, redirecty są wyłączone, a body ma limit 16 KiB | usuwa header confusion, request do Location i nieograniczony odczyt odpowiedzi |
| D-041 | 2026-08-06 | ACCEPTED | Automatyczny Docker smoke używa dynamicznego portu loopback i unikalnego projektu Compose | lokalna bramka nie rezerwuje portu hosta i cleanup nie dotyka obcych stosów |
| D-042 | 2026-08-06 | ACCEPTED | EMP-005 jest scalone z atomowym scope i closeoutem EMP-004 | user-once jest invariantem tej samej transakcji i istniejącego constraintu, nie osobnym endpointem; implementation i evidence ownerem pozostaje EMP-004 |
| D-043 | 2026-08-06 | ACCEPTED | `userId` jest opaque, case-sensitive i spełnia `^[!-~]{1,128}$` bez trimowania ani normalizacji | formalny amendment EMP-001: Bean Validation, PostgreSQL `CHECK` i OpenAPI muszą w przyszłej implementacji egzekwować identyczny kontrakt; V1 sprawdza tylko niepustość |
| D-044 | 2026-08-06 | ACCEPTED | Retry tego samego coupon/userId zwraca 409 `COUPON_ALREADY_REDEEMED` | brak replay pierwotnego 201, Idempotency-Key i pełnej idempotencji; brak publicznego GET nie pozwala bezpiecznie odtworzyć odpowiedzi |
| D-045 | 2026-08-06 | ACCEPTED | Precedence redemption: not found → GeoIP unavailable → country → already redeemed → exhausted | ogranicza zbędne calle i daje stabilne, prywatnościowo świadome wyniki; pod lockiem obowiązuje country → already redeemed → exhausted |
| D-046 | 2026-08-06 | ACCEPTED | `READ COMMITTED + SELECT FOR UPDATE`, bez HTTP pod lockiem, custom lock timeout i retry DB | Client IP i GeoIP pozostają poza transakcją; brak JVM/Redis/distributed lock utrzymuje model spójności w PostgreSQL |
| D-047 | 2026-08-07 | PROPOSED | EMP-009 formalizuje reuse evidence EMP-003/EMP-004 bez kopiowania testów | po review jest wymagany accepted refinement; obecne evidence jest częściowe z trzema brakującymi exact assertions |
| D-048 | 2026-08-07 | ACCEPTED | EMP-009 implementuje wyłącznie trzy exact assertions i checker | brak nowego scenariusza, kodu produkcyjnego, OpenAPI, JaCoCo lub scope EMP-008; evidence pozostaje PARTIAL do pełnego gate |
| D-049 | 2026-08-07 | VERIFIED | EMP-009 closeout następuje po exact assertions, checkerze i pełnym gate | trzy uzupełnione asercje oraz Maven/Testcontainers/Docker evidence dają `Evidence-State: COMPLETE`; EMP-008 pozostaje osobnym scope coverage |
| D-050 | 2026-08-07 | PROPOSED | EMP-008 proponuje JaCoCo w `verify`: global LINE 80%, BRANCH 70%, jedno minimum krytycznych pakietów i `new warnings = 0` | aktualny baseline JaCoCo nie istnieje; progi, plugin version, exclusions i warning budget wymagają formalnej decyzji właściciela |
| D-051 | 2026-08-07 | ACCEPTED | EMP-008 użyje JaCoCo 0.8.15, global 80/70 i jednej krytycznej grupy 75/65 | brak default exclusions; realny raport i manualny review poprzedzają test remediation; zmiana progów lub version wymaga amendmentu |
| D-052 | 2026-08-07 | ACCEPTED | EMP-008 redukuje Javadoc warning debt do maks. 5 uzasadnionych warnings | 18 A jest obligatoryjne, 19 D wymaga review per typ, `new warnings = 0`, DocLint errors=0; PIT pozostaje OUT_OF_SCOPE |
| D-053 | 2026-08-07 | VERIFIED | EMP-008 zamyka coverage gate bez exclusions i akceptuje finalny Javadoc budget 5 | full gate: 106 unit + 22 integration; global 96.07/86.27, critical 96.46/88.81; 0 DocLint errors; 5 warnings pozostaje, bo ich mechaniczne usunięcie wymagałoby pustych konstruktorów lub tautologicznych komentarzy prywatnych pól |
| D-054 | 2026-08-07 | ACCEPTED | EMP-010 używa jednego GitHub Actions job na `ubuntu-24.04`, Java 21 i canonical `DOCKER=docker make verify` | minimalizuje drift między lokalnym i CI gate; brak matrix i osobnego PostgreSQL service |
| D-055 | 2026-08-07 | ACCEPTED | GitHub Actions mają minimalne `contents: read` i wszystkie `uses:` przypięte do pełnego upstream commit SHA | ogranicza supply-chain i token risk bez wprowadzania sekretów/deploymentu |
| D-056 | 2026-08-07 | ACCEPTED | Delivery EMP-010 przypina Docker base images po digest oraz wymaga byte-reproducible source ZIP z tracked files | powtarzalność i audytowalność artefaktu bez deklarowania bit-reproducible container image |
| D-057 | 2026-08-07 | ACCEPTED | `X-Request-Id` akceptuje tylko pojedynczy safe token, inaczej generuje UUID; zawsze response header + MDC cleanup | korelacja bez log injection, odrzucania requestu i high-cardinality metrics |
| D-058 | 2026-08-07 | ACCEPTED | Observability EMP-010 używa wbudowanego JSON Logstash, Prometheus registry i zamkniętych low-cardinality meter vocabularies | spełnia EMP-001/006 bez Grafany/OTel/alertingu i bez wrażliwych labeli |

| D-059 | 2026-08-07 | ACCEPTED | Observability scope EMP-010 nie obejmuje Grafany, OpenTelemetry, alertów, SLO ani centralnego log stacku | minimalny zakres rekrutacyjny pozostaje proporcjonalny i nie tworzy dodatkowej infrastruktury |
| D-060 | 2026-08-07 | ACCEPTED | Flyway/PostgreSQL i Mockito/ByteBuddy warnings są odroczone do EMP-011, o ile nie staną się realnym blockerem CI | brak scope creep bez potwierdzonego wpływu na canonical gate |
