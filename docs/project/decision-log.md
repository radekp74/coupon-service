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
