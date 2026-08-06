# Definition of Ready i Definition of Done

## Definition of Ready

Zadanie może przejść do `READY`, gdy:

1. ma stabilny identyfikator i właściciela zakresu;
2. backlog wskazuje zaakceptowany refinement;
3. cel, wynik, zakres i poza zakresem są jawne;
4. API, model danych i błędy są określone, jeśli dotyczą;
5. zachowanie współbieżne i transakcja są określone, jeśli dotyczą;
6. zależności zewnętrzne mają port, timeout i failure policy;
7. ryzyka wysokie i krytyczne mają mitigację;
8. kryteria akceptacji są mierzalne;
9. matryca testów pokrywa happy path, błędy i race conditions;
10. nie ma otwartych pytań blokujących;
11. `make docs-check` przechodzi.

## Definition of Done — każde zadanie

1. Zakres został zaimplementowany zgodnie z accepted refinement.
2. Kod jest czytelny, sformatowany i przechodzi statyczne bramki.
3. Odpowiednie testy są zielone.
4. Migracje działają na czystej bazie PostgreSQL.
5. Constrainty bazodanowe chronią krytyczne invariants.
6. Błędy nie ujawniają danych technicznych ani osobowych.
7. Backlog, current status, changelog i release history są aktualne.
8. Decyzje i ryzyka są zsynchronizowane.
9. Lesson learned jest dodawany tylko, gdy został potwierdzony.
10. Nie ma sekretów, artefaktów builda ani danych środowiskowych.
11. `make verify` przechodzi.
12. Working tree jest czysty po checkpointcie.

## Definition of Done — endpoint

Dodatkowo:

- operation i schema są w OpenAPI;
- walidacja requestu ma testy;
- happy path i wszystkie stabilne error codes mają testy API;
- content type jest poprawny;
- endpoint nie wykonuje nieograniczonego calla zewnętrznego;
- logi nie zawierają surowego IP ani pełnych danych requestu;
- kontrakt jest pokazany w README lub dokumentacji API.

## Definition of Done — użycie kuponu

Dodatkowo:

- test 100 równoległych requestów dla limitu 10 daje dokładnie 10 sukcesów;
- `current_uses` nigdy nie przekracza `max_uses`;
- licznik odpowiada liczbie redemption records;
- równoległe requesty tego samego użytkownika dają dokładnie jeden sukces;
- błędny kraj i awaria GeoIP nie zwiększają licznika;
- rollback nie pozostawia częściowego redemption record;
- test działa na PostgreSQL przez Testcontainers, nie na H2.

## Definition of Done — finalne oddanie

- czysty checkout buduje się `./mvnw -B clean verify`;
- `docker compose up --build` uruchamia aplikację i PostgreSQL;
- health check jest zielony;
- README zawiera instrukcję i uzasadnienia;
- publiczne repozytorium nie zawiera sekretów;
- CI jest zielone;
- OpenAPI jest aktualne;
- finalny source package przechodzi `make verify`;
- current status nie zawiera planu przedstawionego jako wykonany.
