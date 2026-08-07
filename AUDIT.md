# Audyt wzorców governance wykorzystanych w projekcie

## Cel

Zastosować do małego zadania rekrutacyjnego najlepsze elementy dojrzałego procesu dokumentacyjnego bez kopiowania skali właściwej dla dużego produktu.

## Wzorce zachowane

1. Jedno źródło prawdy dla statusów, bieżącego stanu, decyzji, ryzyk i zmian.
2. Obowiązkowy refinement przed implementacją.
3. Rozdzielenie `PLANNED`, `READY`, `IN_PROGRESS`, `DONE` i `DONE_AND_VERIFIED`.
4. Mierzalne kryteria akceptacji oraz jawna matryca testów.
5. Równorzędne traktowanie zakresu i elementów poza zakresem.
6. Oddzielenie decyzji, ryzyka i lesson learned.
7. Aktualizacja dokumentacji w tym samym checkpointcie co implementacja.
8. Automatyczna kontrola linków, indeksu, statusów i refinementów.
9. Closeout oparty na dowodach, nie na deklaracji.
10. Zakaz przedstawiania planów jako istniejących funkcji.
11. Jawny, nadpisywalny kontrakt lokalnych narzędzi zamiast założenia o globalnym `PATH`.
12. Powtarzalny i bezpieczny eksport źródeł do zewnętrznej analizy.

## Wzorce świadomie uproszczone

- jeden zaakceptowany refinement rozwiązania może pokrywać małe, jednoznaczne zadania MVP;
- nie ma osobnego pliku stanu maszynowego ani rozbudowanej dependency matrix;
- nie ma generatora PDF dokumentacji;
- nie ma wieloetapowego protokołu commitów i checksumów release;
- nie ma osobnego ADR-u dla każdej drobnej biblioteki;
- nie ma setek obowiązków właściwych dla produkcyjnej platformy wielodomenowej.

## Kryterium proporcjonalności

Dokumentacja ma umożliwić oceniającemu zrozumienie projektu w kilka minut. Każdy dokument musi odpowiadać na inne pytanie i nie może dublować źródła prawdy.

## Wynik

Powstał lekki, zamknięty przepływ:

```text
wymaganie
→ backlog
→ refinement
→ review i akceptacja
→ implementacja
→ testy i evidence
→ status
→ changelog i release history
→ decyzje, ryzyka i lessons learned
```

## Zastosowanie wzorca w EMP-002

Checkpoint bootstrapu celowo rozdziela trzy stany:

1. źródła przygotowane;
2. statyczny kontrakt zweryfikowany;
3. runtime zweryfikowany na lokalnym Docker Desktop.

Dzięki temu brak dostępu wykonawczego do lokalnego daemona nie prowadzi do fałszywego `DONE_AND_VERIFIED`. Backlog pozostaje `IN_PROGRESS`, a dokładna komenda i wymagany evidence są zapisane przed uruchomieniem gate.


## Korekta kompletności EMP-002

Review checkpointu wykazał, że bootstrap aplikacji zawierał Testcontainers, lecz nie zawierał jawnego artefaktu uruchomieniowego dla całego serwisu. Zostało to skorygowane przez dodanie wieloetapowego `Dockerfile`, `.dockerignore`, `docker-compose.yml` oraz deterministycznego smoke testu.

Wniosek procesowy: wykorzystanie Dockera przez testy nie zastępuje kontraktu konteneryzacji aplikacji. Oba dowody są odrębne i oba muszą być widoczne w Definition of Done bootstrapu.

## Korekta czasu budowy obrazu

Pierwszy rzeczywisty build na Docker Desktop wykazał, że oddzielny krok `dependency:go-offline` jest nieproporcjonalnie kosztowny dla tego repozytorium. Rozwiązuje on szerszy zbiór artefaktów niż potrzebuje pojedynczy build obrazu. Zastąpiono go jednym `mvn package` wykorzystującym trwały cache BuildKit dla lokalnego repozytorium Maven. Statyczna bramka wymaga teraz cache mount i zabrania regresji do `dependency:go-offline`.

## Zastosowanie wzorca w EMP-003

Mimo że `EMP-001` zamrażał kontrakt MVP, przed pierwszym endpointem biznesowym utworzono osobny refinement `EMP-003`. Dokument rozstrzyga granicę transakcji, canonicalizację, mapowanie SQLSTATE i deterministyczny test wyścigu. Sama obecność testu nie jest traktowana jako dowód jego przejścia. EMP-003 został zamknięty jako `DONE_AND_VERIFIED` dopiero po pełnym lokalnym `make verify`, runtime HTTP i exact-count concurrency test.

## Korekta kolejności przed EMP-004

Refinement wykazał, że publiczny endpoint wykorzystania kuponu zależy od wiarygodnego ustalenia kraju i kompletnego kontraktu dla testerów. Próba realizacji EMP-004 przed EMP-006/007 wymagałaby utrwalania fikcyjnego kraju, publicznego bypassu albo wystawienia niekompletnego API. Zgodnie z planem fal w EMP-001 aktywne staje się EMP-007, następnie EMP-006, a EMP-004 pozostaje czasowo `BLOCKED`.

## Polityka komentarzy i OpenAPI

Nie wprowadzono wymogu komentarza dla każdej zmiennej. Publiczne kontrakty otrzymują Javadoc opisujący semantykę, invariants, skutki uboczne i błędy. Canonical `docs/api/openapi.yaml` jest pakowany do aplikacji i wyświetlany testerom przez Swagger UI, dzięki czemu dokumentacja i runtime korzystają z jednego źródła prawdy.

## Evidence closeoutu EMP-007

EMP-007 zamknięto dopiero po lokalnym `make verify`: Maven `clean verify` przeszedł z `OpenApiDocumentationIT` i DocLint bez błędów, a JAR zawiera `BOOT-INF/classes/static/openapi.yaml`. Runtime Docker potwierdził `UP` na `/actuator/health`, canonical YAML na `/openapi.yaml`, działające `/swagger-ui` oraz `swagger-config` z `url=/openapi.yaml`. Przykładowy create zwrócił 201, a case-insensitive duplicate 409 `COUPON_CODE_CONFLICT`; własny stos został następnie usunięty.

## Refinement EMP-006

EMP-006 pozostaje wyłącznie checkpointem dokumentacyjnym. Draft rozdziela trzy odpowiedzialności: wiarygodne ustalenie Client IP, mapowanie publicznego IP na kraj oraz przyszłą decyzję domenową podczas redemption. Dzięki temu zewnętrzny call nie znajdzie się pod blokadą bazy, a infrastrukturalny 503 nie zostanie pomylony z biznesowym 403.

Review objęło także granicę zaufania proxy. Direct mode ignoruje nagłówki klienta. Trusted mode wymaga CIDR, analizuje łańcuch od prawej i nie fallbackuje z błędnego `Forwarded` do XFF. Ścisły parser nie może wykonywać DNS. Publiczny adapter nie otrzymuje adresów specjalnego przeznaczenia, a raw IP pozostaje memory-only.

Darmowy provider jest opisany jako adapter demonstracyjny, nie produkcyjne SLA. Pierwszy review pozostawił refinement w stanie `DRAFT` i odrzucił go z pięcioma lukami bezpieczeństwa.

## Security amendment EMP-006

Pierwszy review EMP-006 zakończył się `REJECT` dla pięciu luk: wielokrotnych field-lines, redirectów dostawcy, limitu body, IPv6/portów i boundary proxy. Dokumentacyjny amendment doprecyzował fail-closed, 16 KiB bounded read, brak śledzenia `Location`, bezpieczny podzbiór składni i obowiązek deploymentu.

## Formalna akceptacja refinementu EMP-006

2026-08-06 Radosław Piątek zaakceptował amendment i pięć decyzji właściciela: demonstracyjny adapter `ipwho.is`, wspólny 503 `GEOLOCATION_UNAVAILABLE`, brak cache/retry/fallbacku, fail-closed dla błędnego `Forwarded` oraz stub `PL` wyłącznie w profilach `local` i `test`. Refinement ma status `ACCEPTED`, EMP-006 jest `READY`, a implementacja pozostaje `NOT_STARTED`. Nie jest to dowód istnienia Client IP, GeoIP ani redemption w runtime.

## Evidence closeoutu EMP-006

EMP-006 zamknięto po pełnym `make verify` (53 unit i 10 integration tests; 47 nowych testów EMP-006), DocLint bez błędów, WireMock bez publicznej sieci i Docker runtime. Testy potwierdziły fail-closed dla wielokrotnych field-lines, direct-mode spoofing, trusted chain, bracketed IPv6 z portem, policy special-purpose, redirect z dokładnie jednym requestem oraz granice body 16 384/16 385. Runtime Compose potwierdził health `UP`, Swagger UI i canonical OpenAPI bez redemption na dynamicznym porcie loopback; cleanup usunął wyłącznie stos smoke, a istniejący `coupon-service-app-1` na 18080 pozostał healthy.

## Remediation evidence EMP-004

Audyt zakresu EMP-008/EMP-009 wykrył rzeczywistą lukę: historyczny closeout EMP-004 zawierał podstawowy runtime i 100/10, lecz nie komplet dowodów wymaganych przez jego accepted refinement. Status weryfikacji został wznowiony bez zmiany kontraktu. Remediation dodało rzeczywiste testy `UserId`, orchestratora, HTTP 403 `COUNTRY_NOT_ALLOWED` i 503 `GEOLOCATION_UNAVAILABLE`, same-user 1/19, last-slot 1/1, niezależność blokad coupon-A/coupon-B oraz testowe rollbacki PostgreSQL dla INSERT, UPDATE po INSERT i innego constraintu.

Pełne lokalne `./mvnw -B -ntp clean verify` przeszło: 60 testów unit i 22 integracyjne. DocLint miał 0 błędów (42 istniejące ostrzeżenia Javadoc). Dopiero to evidence pozwala ponownie ustawić EMP-004 jako `DONE_AND_VERIFIED`; EMP-009 pozostaje planowanym, odrębnym review formalnego mapowania evidence współbieżności.

## Draft EMP-009

EMP-009 nie uznaje poprzednich deklaracji za automatyczny closeout. Mapa kodu przypisuje konkretne metody Testcontainers z EMP-003 i EMP-004, ale oznacza `EVIDENCE_PARTIAL`: 100/10 nie klasyfikowało kodu 90 konfliktów ani 10 unikalnych userId, a last-slot nie potwierdzał jednego zapisanego nowego użytkownika. Radosław Piątek formalnie zaakceptował 2026-08-07 ograniczony checkpoint trzech asercji i implementacyjnego checkera; EMP-009 jest `IN_PROGRESS`, evidence pozostaje `PARTIAL` do pełnego gate i zadanie nie jest zamknięte.

## Closeout EMP-009

Minimalna implementacja zmieniła wyłącznie istniejące testy współbieżności i dodała `scripts/check_emp009.py`. Każda z trzech rund 100/10 klasyfikuje teraz dokładnie 10 × 201, 90 × `COUPON_EXHAUSTED`, zero other/unknown oraz potwierdza `current_uses=10`, 10 rekordów i 10 unikalnych `user_id`. Last-slot potwierdza 1 × 201, 1 × exhausted i zapis dokładnie jednego z dwóch konkurujących użytkowników. Checker failuje po kontrolowanym usunięciu każdej z tych trzech asercji na kopii tymczasowej.

Pełne `./mvnw -B -ntp clean verify` zakończyło się sukcesem: 60 testów unit i 22 integration, DocLint 0 błędów (42 istniejące ostrzeżenia). Pełne `make verify` przeszło z własnym projektem Compose i dynamicznym loopback `127.0.0.1:55006`; health, canonical OpenAPI i Swagger UI przeszły, a stack został usunięty. To jest evidence dla `EMP-009 = DONE_AND_VERIFIED`; nie zmienia historii remediation EMP-004.

## Accepted refinement EMP-008

Aktualny audyt po remediation EMP-004 i closeoucie EMP-009 potwierdza 60 unit i 22 integration tests z realnym evidence domeny, GeoIP/WireMock, PostgreSQL/Flyway, HTTP, rollbacków, OpenAPI i runtime Docker. Nie ma skonfigurowanego JaCoCo ani zmierzonego LINE/BRANCH baseline’u. Radosław Piątek zaakceptował przyszłe JaCoCo `0.8.15`, globalne `LINE >= 80%` i `BRANCH >= 70%`, krytyczną grupę `75%/65%`, brak default exclusions, manualny review oraz finalny justified Javadoc budget <=5. `Coverage-Evidence` pozostaje `NOT_MEASURED`; refinement jest `ACCEPTED`, a implementation dozwolona, lecz `NOT_STARTED`.


## Refinement EMP-004

Po zweryfikowaniu EMP-006 przygotowano własny draft transakcyjnego redemption. Dokument rozdziela snapshot/GeoIP od krótkiej transakcji, wymaga osobnego proxied bean, zamraża `SELECT ... FOR UPDATE`, kolejność błędów, atomowy insert/increment, rollback i exact-count concurrency. Pierwszy review dał rekomendację `REJECT`, ponieważ `userId` był niespójny z EMP-001, a granica kontroler/orchestrator wymagała doprecyzowania. 2026-08-06 właściciel zaakceptował amendment EMP-001 dla opaque, case-sensitive `userId` `^[!-~]{1,128}$`, konsolidację EMP-005, retry 409, precedence i model transakcyjny. Refinement jest `ACCEPTED`; endpoint, migracja i canonical OpenAPI nadal nie istnieją.

## EMP-008 Phase 2 candidate — 2026-08-07

Po realnym pomiarze 89.23% LINE / 70.26% BRANCH globalnie i 88.13% / 70.52% dla critical aggregate nie zmieniono progów ani exclusions. Kandydat uzupełnia tylko istotne missed security/domain branches, wzmacnia checker o niezależne parsowanie `jacoco.xml` i naprawia znaczące braki Javadoc bez modyfikacji produkcyjnego behavior. Status pozostaje `IN_PROGRESS` do pełnego Maven/Testcontainers/DocLint/Docker gate.

## Closeout EMP-008 — 2026-08-07

Phase 2 przeszło pełny lokalny gate. Maven/Testcontainers wykonał 106 testów unit i 22 integration bez failures/errors. JaCoCo 0.8.15 zmierzył globalnie 96.07% LINE / 86.27% BRANCH oraz 96.46% / 88.81% dla critical aggregate; oba Maven checks i niezależny report checker z fail-closed self-testami przeszły. Nie ma exclusions ani PIT. Docker smoke przeszedł na dynamicznym `127.0.0.1:55008` i posprzątał własny stos.

DocLint zakończył się z 0 errors i 5 warnings, redukując baseline 42. Finalny budget 5 został zaakceptowany jako świadome zastosowanie polityki „nie dokumentuj mechanicznie”: trzy warnings dotyczą implicit default constructors klas frameworkowych, a dwa prywatnych pól wyjątków, których publiczne kontrakty są już udokumentowane. Nie dodano pustych konstruktorów ani tautologicznych komentarzy wyłącznie dla wyzerowania licznika. EMP-008 ma `DONE_AND_VERIFIED`, coverage `MEASURED_AND_VERIFIED`.


## Zaakceptowany refinement EMP-010 — CI, delivery i obserwowalność

Po zamknięciu quality gate przygotowano i formalnie zaakceptowano własny refinement Wave 6 zamiast bezpośrednio dodawać GitHub Actions i metryki. Refinement rozdziela trzy dowody: zgodność CI z lokalnym `make verify`, powtarzalność artefaktu źródłowego oraz obserwowalność bez naruszenia privacy contract. CI ma jeden job na Ubuntu 24.04, minimalne permissions i immutable action SHA; delivery ma bazować na tracked files, digest-pinned base images i dwóch identycznych SHA-256 ZIP-a; Prometheus/request ID/logging mają wyłącznie zamknięte, niskokardynalne pola bez IP, userId i coupon code.

To nadal nie jest runtime evidence: EMP-010 jest `READY/ACCEPTED`, implementation `NOT_STARTED`, `Implementation-Allowed: YES`, a CI/delivery/observability evidence pozostają `NOT_MEASURED`. Akceptacja właściciela z 2026-08-07 zamroziła osiem decyzji bez uruchamiania implementacji.
