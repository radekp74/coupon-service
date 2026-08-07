# EMP-011 — podsumowanie refinementu

EMP-011 ma domknąć zadanie rekrutacyjne bez rozszerzania funkcji biznesowych. Draft powstał po audycie stanu po EMP-010 i identyfikuje dziesięć konkretnych niespójności/punktów finalnego review.

## Najważniejsze znaleziska

- README nadal pokazuje EMP-010 `IN_PROGRESS` i błędnie mówi, że source export regeneruje checksumy;
- `api-contract.md` jednocześnie twierdzi, że redemption nie istnieje i opisuje jego endpoint;
- `architecture/overview.md` nadal przedstawia EMP-004/redemption jako plan;
- `test-strategy.md` ma historyczne 106+22 i pre-EMP-010 coverage zamiast finalnych 112 unit + 23 integration i 95.76/86.39 / 95.06/88.21;
- rejestr refinementów i risk register nie zostały w pełni zreconciliowane po EMP-010;
- finalny README musi lepiej pokazać reviewerowi decyzje, przykłady, ograniczenia i recovery.

## Proponowana granica

Po akceptacji zmiany są ograniczone do README, bieżącej dokumentacji, risk reconciliation, finalnego checkera i governance closeoutu. Produkcyjne Java, POM, migracje, OpenAPI, DB, GeoIP, concurrency, observability i CI topology pozostają zamrożone.

Warningi Flyway/PostgreSQL 18.4, Mockito dynamic agent oraz deprecacja `@MockBean` pozostają udokumentowanym maintenance debt; zielone Java 21 i GitHub Actions nie uzasadniają dependency churn tuż przed oddaniem.

## Finalny dowód

Closeout ma wymagać `make verify`, czystego committed tree, `make delivery-check`, deterministycznego source export, pushu SSH, zielonego GitHub Actions na finalnym SHA oraz Swaggera pokazującego dwa POST-y z canonical `/openapi.yaml`.

## Stan

`READY / ACCEPTED / NOT_STARTED / Implementation-Allowed: YES`. Właściciel zaakceptował wszystkie osiem decyzji 2026-08-07; scope jest zamrożony przed implementacją finalnego review.
