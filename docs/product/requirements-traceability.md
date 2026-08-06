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
| tylko użytkownicy z kraju | GeoIP port i adapter | EMP-006 | WireMock API tests |
| informacja o wyczerpaniu | 409 `COUPON_EXHAUSTED` | EMP-007 | API test |
| informacja o braku kodu | 404 `COUPON_NOT_FOUND` | EMP-007 | API test |
| informacja o złym kraju | 403 `COUNTRY_NOT_ALLOWED` | EMP-006/007 | API test |
| jedno użycie użytkownika | unique `(coupon_id,user_id)` | EMP-005 | duplicate + concurrency test |
| dane w bazie | PostgreSQL + Flyway | EMP-002 | Testcontainers |
| Java lub Kotlin | Java 21 | EMP-002 | Maven build |
| Maven lub Gradle | Maven Wrapper | EMP-002 | `./mvnw verify` |
| skalowalność | stateless app + DB coordination | EMP-004/010 | architecture + concurrency test |
| czytelność i jakość | modularny monolit, quality gates | wszystkie | review + static gates |
| testy | unit, integration, API, concurrency | EMP-008/009 | CI report |
| README z uzasadnieniem | źródłowa dokumentacja i ADR | EMP-011 | final review |
| publiczne repozytorium | czysty source export, bez sekretów | EMP-010/011 | repository URL + package gate |

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
