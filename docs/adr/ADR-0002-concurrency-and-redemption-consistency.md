# ADR-0002 — współbieżność i spójność wykorzystania kuponu

- **Status:** Accepted
- **Data:** 2026-08-06
- **Powiązane zadania:** EMP-004, EMP-005, EMP-009

## Kontekst

Wymaganie „kto pierwszy, ten lepszy” oznacza, że przy wielu równoległych requestach liczba zaakceptowanych użyć nie może przekroczyć `max_uses`. Sprawdzenie licznika w pamięci aplikacji lub zwykły odczyt i późniejszy zapis prowadzą do race condition.

## Decyzja

PostgreSQL jest jedynym źródłem prawdy. Wykorzystanie kuponu odbywa się w krótkiej transakcji `READ COMMITTED`:

1. przed transakcją aplikacja odczytuje kupon i rozwiązuje kraj klienta;
2. rozpoczyna transakcję;
3. pobiera kupon po `normalized_code` z `SELECT ... FOR UPDATE`;
4. ponownie waliduje kraj;
5. sprawdza, czy `(coupon_id, user_id)` już istnieje;
6. sprawdza `current_uses < max_uses`;
7. zapisuje `coupon_redemptions`;
8. zwiększa `current_uses` o jeden;
9. zatwierdza transakcję.

Zewnętrzne GeoIP nigdy nie jest wywoływane podczas trzymania blokady.

## Kolejność błędów po uzyskaniu blokady

1. `COUPON_NOT_FOUND`;
2. `COUNTRY_NOT_ALLOWED`;
3. `COUPON_ALREADY_REDEEMED`;
4. `COUPON_EXHAUSTED`;
5. sukces.

Ta kolejność zapewnia, że użytkownik, który już wykorzystał kupon, otrzyma stabilną informację także wtedy, gdy kupon został później wyczerpany.

## Ochrona wielowarstwowa

- `UNIQUE (normalized_code)` chroni kod kuponu;
- `UNIQUE (coupon_id, user_id)` chroni jedno użycie przez użytkownika;
- `CHECK (current_uses BETWEEN 0 AND max_uses)` chroni licznik;
- blokada rekordu serializuje konkurujące użycia tego samego kuponu;
- kontrola liczby zmienionych wierszy wykrywa niespodziewane naruszenie kontraktu.

## Rozważone alternatywy

### Synchronizacja w JVM

Odrzucona, ponieważ nie działa między wieloma instancjami aplikacji.

### Optymistyczna blokada z retry

Możliwa, lecz przy gorącym kuponie generowałaby wiele konfliktów i wymagała polityki retry. Pessimistic lock jest prostszy do wyjaśnienia i deterministyczny dla tego zakresu.

### Atomowy warunkowy `UPDATE ... RETURNING`

Poprawna alternatywa o krótszej ścieżce blokowania. Nie została wybrana, ponieważ wymagane jest również deterministyczne sprawdzenie wcześniejszego użycia przez użytkownika. Może być przyszłą optymalizacją po pomiarach.

### Redis jako licznik

Odrzucony. Wymagałby rozwiązania spójności z trwałym rejestrem użyć i recovery po częściowej awarii.

## Konsekwencje

- wszystkie instancje aplikacji dzielą spójność PostgreSQL;
- równoległe użycia różnych kuponów nie blokują się wzajemnie;
- użycia jednego bardzo popularnego kuponu są serializowane;
- transakcja musi być krótka i pozbawiona wywołań sieciowych;
- test współbieżności jest obowiązkową częścią Definition of Done.
