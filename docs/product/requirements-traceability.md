# Traceability wymagań zadania

| Wymaganie | Decyzja rozwiązania | Zadanie | Dowód docelowy |
|---|---|---|---|
| utworzenie kuponu | `POST /api/v1/coupons` | EMP-003 | test API + migracja |
| rejestracja użycia | `POST /api/v1/coupons/{code}/redemptions` | EMP-004 | test API + DB |
| unikalny kod bez względu na case | `normalized_code` + unique constraint | EMP-003 | concurrent duplicate test |
| data utworzenia | serwerowy `Clock`, UTC | EMP-003 | unit/integration test |
| maksymalna liczba użyć | `max_uses` + check constraint | EMP-003/004 | migration + tests |
| bieżąca liczba użyć | transakcyjny `current_uses` | EMP-004 | invariant test |
| kraj kuponu | `country_code` ISO alpha-2 | EMP-003 | validation test |
| kto pierwszy, ten lepszy | `SELECT FOR UPDATE` | EMP-004/009 | 100 request concurrency test |
| tylko użytkownicy z kraju | trusted Client IP + provider-neutral GeoIP; porównanie dopiero w EMP-004 | EMP-006/004 | parser/proxy/WireMock tests + API test |
| informacja o wyczerpaniu | 409 `COUPON_EXHAUSTED` | EMP-004/007 | API test |
| informacja o braku kodu | 404 `COUPON_NOT_FOUND` | EMP-004/007 | API test |
| informacja o złym kraju | 403 `COUNTRY_NOT_ALLOWED` | EMP-006/004/007 | API test |
| jedno użycie użytkownika | unique `(coupon_id,user_id)` w atomowej transakcji redemption; EMP-005 jest scalone z EMP-004 | EMP-004 | duplicate + same-user concurrency test |
| dane w bazie | PostgreSQL + Flyway | EMP-002 | Testcontainers |
| Java lub Kotlin | Java 21 | EMP-002 | Maven build |
| Maven lub Gradle | Maven Wrapper | EMP-002 | `./mvnw verify` |
| skalowalność | stateless app + DB coordination | EMP-004/010 | architecture + concurrency test |
| czytelność i jakość | modularny monolit, quality gates | wszystkie | review + static gates |
| testy | unit, integration, API, concurrency | EMP-008/009 | CI report |
| README z uzasadnieniem | recruiter-first README + ADR + jawne trade-offy/ograniczenia | EMP-011 | final checker + canonical gate |
| publiczne repozytorium | clean Git history + CI + deterministyczny source export bez sekretów | EMP-010/011 | green GitHub Actions + delivery-check + package hygiene |

## Wymagania dodane świadomie

- stabilne machine-readable error codes;
- brak utrwalania surowego IP;
- timeout i fail-closed dla GeoIP;
- zaufane proxy zamiast bezwarunkowego `X-Forwarded-For`;
- jawny test współbieżności;
- brak H2 w testach integracyjnych;
- bramka dokumentacyjna.

Dodatki nie zmieniają domeny zadania i wspierają oceniane cechy: jakość, architekturę, testy i produkcyjną świadomość.


## Wymaganie dodatkowe — dokumentacja dla testerów

- canonical OpenAPI: `docs/api/openapi.yaml`;
- runtime machine-readable spec: `/openapi.yaml`;
- interaktywny Swagger UI: `/swagger-ui`;
- każdy kolejny publiczny endpoint aktualizuje spec i testy w tym samym checkpointcie.


## Traceability EMP-006

- spoofing forwarded headers → direct default, CIDR trust i boundary proxy contract;
- brak DNS → ścisły parser literalnego IPv4/IPv6;
- awaria darmowego providera → timeout, brak retry i 503 `GEOLOCATION_UNAVAILABLE`;
- prywatne adresy → public provider nie jest wywoływany; local/test korzysta ze stubu;
- minimalizacja danych → raw IP tylko w pamięci, bez storage, hash i logowania;
- tester-facing API → EMP-006 nie publikuje przedwcześnie redemption w canonical OpenAPI.
