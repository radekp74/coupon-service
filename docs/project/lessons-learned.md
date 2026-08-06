# Lessons learned

## LL-001 — Testcontainers 2.x zmienił pakiet kontenera PostgreSQL

- **Data:** 2026-08-06
- **Powiązane zadanie:** EMP-002
- **Obserwacja:** przykłady i pamięć z Testcontainers 1.x często prowadzą do importu `org.testcontainers.containers.PostgreSQLContainer`, który nie odpowiada strukturze modułu 2.x.
- **Dowód:** źródło Testcontainers 2.0.5 deklaruje klasę w pakiecie `org.testcontainers.postgresql`; statyczny checker zabrania starego importu.
- **Zmiana procesu lub kodu:** zależność została przypięta do `testcontainers-postgresql:2.0.5`, test używa nowego pakietu, a `scripts/check_bootstrap.py` wykrywa regresję.
- **Status:** `CONFIRMED`

## LL-002 — Testcontainers nie zastępuje konteneryzacji aplikacji

- **Data:** 2026-08-06
- **Powiązane zadanie:** EMP-002
- **Obserwacja:** pierwszy kandydat bootstrapu korzystał z Docker Desktop przez Testcontainers, ale nie zawierał `Dockerfile` ani `docker-compose.yml`.
- **Dowód:** review zawartości paczki wykazał brak obu artefaktów mimo deklaracji produkcyjnie świadomego bootstrapu.
- **Zmiana procesu lub kodu:** dodano wieloetapowy obraz, nieuprzywilejowany runtime, Compose z PostgreSQL i health checkami, targety Make oraz automatyczny runtime smoke w `make verify`.
- **Status:** `CONFIRMED`

## Zasada

Kolejny wpis powstaje dopiero po potwierdzeniu przez:

- rzeczywisty błąd;
- wynik testu;
- problem integracyjny;
- nieudane założenie;
- pomiar wydajności;
- review, które zmieniło kontrakt.

Planowana wiedza, ogólna dobra praktyka ani przewidywane ryzyko nie są lesson learned.

## Szablon

```text
LL-XXX — krótki tytuł
Data:
Powiązane zadanie:
Obserwacja:
Dowód:
Zmiana procesu lub kodu:
Status:
```

## LL-003 — `dependency:go-offline` nie jest właściwą optymalizacją pierwszego builda obrazu

- **Data:** 2026-08-06
- **Powiązane zadanie:** EMP-002
- **Obserwacja:** pierwszy `make docker-up` pozostawał przez około 15 minut na kroku Maven `dependency:go-offline`.
- **Dowód:** lokalny output Docker BuildKit wskazał `898.3s` na pojedynczym kroku rozwiązywania zależności przed właściwą kompilacją.
- **Zmiana procesu lub kodu:** usunięto `dependency:go-offline` z `Dockerfile`, właściwy `package` korzysta z `--mount=type=cache,target=/root/.m2,sharing=locked`, a checker blokuje regresję.
- **Status:** `CONFIRMED`

## LL-004 — PostgreSQL 18 wymaga nowego punktu montowania trwałych danych

- **Data:** 2026-08-06
- **Powiązane zadanie:** EMP-002
- **Obserwacja:** `postgres:18.4-alpine` kończył się restartem, gdy wolumen Compose był montowany w `/var/lib/postgresql/data`.
- **Dowód:** log entrypointu wskazał, że PostgreSQL 18 wymaga pojedynczego mountu `/var/lib/postgresql`; po zmianie oba healthchecki Compose przeszły, a Flyway zastosował V1.
- **Zmiana procesu lub kodu:** `docker-compose.yml` montuje nazwany wolumen `coupon-postgres-data` w `/var/lib/postgresql`.
- **Status:** `CONFIRMED`
