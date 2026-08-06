# Reguły projektu

## 1. Język

- dokumentacja projektowa i biznesowa jest po polsku;
- kod, testy i techniczne identyfikatory są po angielsku;
- nazwy plików technicznych pozostają po angielsku.

## 2. Refinement przed implementacją

- zadanie nie może przejść do `READY` ani `IN_PROGRESS` bez wskazania zaakceptowanego refinementu;
- `EMP-001` pozostaje umbrella contract, ale każde kolejne zadanie implementacyjne ma własny refinement;
- zmiana publicznego API, modelu danych, transakcji, bezpieczeństwa, prywatności, GeoIP lub stosu wymaga amendmentu i ponownej akceptacji;
- brak decyzji nie może być ukryty jako „szczegół implementacyjny”.

## 3. Źródła prawdy

- statusy zadań: `docs/project/backlog.md`;
- bieżący stan: `docs/project/current-status.md`;
- zakres realizacyjny: zaakceptowany refinement;
- trwałe decyzje: `docs/project/decision-log.md` i `docs/adr/`;
- ryzyka: `docs/project/risk-register.md`;
- zmiany: `CHANGELOG.md` i `docs/project/release-history.md`;
- publiczny kontrakt REST API: `docs/api/openapi.yaml`.

## 4. Jakość i uczciwość stanu

- plan nie może być opisany jako działająca funkcja;
- `DONE` wymaga dowodów, a `DONE_AND_VERIFIED` przejścia zapisanych bramek;
- dokumentacja jest aktualizowana w tym samym checkpointcie co kod;
- każda linia wygenerowana z pomocą AI musi być zrozumiana i możliwa do obrony;
- nie dodajemy technologii bez konkretnego problemu, który rozwiązują.

## 5. Dokumentowanie kodu

- publiczne kontrakty domenowe i aplikacyjne mają znaczący Javadoc;
- Javadoc opisuje semantykę, invariants, skutki uboczne, failure modes i wymagania współbieżności;
- `@param`, `@return` i `@throws` są używane wtedy, gdy dodają wiedzę ponad samą sygnaturę;
- komentarze wyjaśniają **dlaczego** istnieje nieoczywiste rozwiązanie, a nie powtarzają **co** robi kod;
- zmienne lokalne i oczywiste metody prywatne nie wymagają komentarzy;
- komentarze dublujące typy, nazwy lub oczywiste instrukcje są zabronione;
- dokumentacja kodu jest aktualizowana razem ze zmianą zachowania.

## 6. OpenAPI dla testerów

- każdy publiczny endpoint, request, response, status HTTP, stabilny kod błędu i przykład jest opisany w `docs/api/openapi.yaml`;
- specyfikacja OpenAPI jest wersjonowana razem z implementacją;
- lokalny runtime udostępnia czytelny interfejs Swagger UI oraz machine-readable specyfikację;
- Swagger UI korzysta z wersjonowanego pliku `docs/api/openapi.yaml`, aby nie powstały dwa niezależne źródła prawdy;
- zmiana publicznego API bez aktualizacji OpenAPI powoduje niepowodzenie bramki jakości;
- endpoint nie może otrzymać `DONE_AND_VERIFIED`, jeśli OpenAPI nie odpowiada jego rzeczywistemu zachowaniu.

## 7. Dane i bezpieczeństwo

- sekrety, tokeny i pliki `.env` nie trafiają do repozytorium;
- surowy adres IP nie jest zapisywany w bazie ani logowany;
- zewnętrzny dostawca GeoIP jest wywoływany poza transakcją bazodanową;
- ograniczenia integralności są egzekwowane również przez PostgreSQL;
- nagłówki proxy są honorowane wyłącznie w zaufanej konfiguracji wdrożeniowej.

## 8. Weryfikacja

Przed każdym checkpointem:

```bash
make docs-check
make verify
```

Po bootstrapie aplikacji `make verify` obejmuje Maven, testy, migracje, DocLint, OpenAPI runtime smoke i statyczne bramki jakości.
