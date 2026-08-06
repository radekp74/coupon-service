# Reguły projektu

## 1. Język

- dokumentacja projektowa i biznesowa jest po polsku;
- kod, testy i techniczne identyfikatory są po angielsku;
- nazwy plików technicznych pozostają po angielsku.

## 2. Refinement przed implementacją

- zadanie nie może przejść do `READY` ani `IN_PROGRESS` bez wskazania zaakceptowanego refinementu;
- `EMP-001` może pokrywać ograniczone zadania MVP wymienione w jego planie realizacji;
- zmiana publicznego API, modelu danych, transakcji, bezpieczeństwa, prywatności, GeoIP lub stosu wymaga amendmentu i ponownej akceptacji;
- brak decyzji nie może być ukryty jako „szczegół implementacyjny”.

## 3. Źródła prawdy

- statusy zadań: `docs/project/backlog.md`;
- bieżący stan: `docs/project/current-status.md`;
- zakres realizacyjny: zaakceptowany refinement;
- trwałe decyzje: `docs/project/decision-log.md` i `docs/adr/`;
- ryzyka: `docs/project/risk-register.md`;
- zmiany: `CHANGELOG.md` i `docs/project/release-history.md`.

## 4. Jakość i uczciwość stanu

- plan nie może być opisany jako działająca funkcja;
- `DONE` wymaga dowodów, a `DONE_AND_VERIFIED` przejścia zapisanych bramek;
- dokumentacja jest aktualizowana w tym samym checkpointcie co kod;
- każda linia wygenerowana z pomocą AI musi być zrozumiana i możliwa do obrony;
- nie dodajemy technologii bez konkretnego problemu, który rozwiązują.

## 5. Dane i bezpieczeństwo

- sekrety, tokeny i pliki `.env` nie trafiają do repozytorium;
- surowy adres IP nie jest zapisywany w bazie ani logowany;
- zewnętrzny dostawca GeoIP jest wywoływany poza transakcją bazodanową;
- ograniczenia integralności są egzekwowane również przez PostgreSQL;
- nagłówki proxy są honorowane wyłącznie w zaufanej konfiguracji wdrożeniowej.

## 6. Weryfikacja

Przed każdym checkpointem:

```bash
make docs-check
make verify
```

Po bootstrapie aplikacji `make verify` musi obejmować Maven, testy, migracje i statyczne bramki jakości.
