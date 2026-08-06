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
