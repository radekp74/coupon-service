# Proces refinementu

## Cel

Refinement zamienia treść zadania w testowalny kontrakt realizacyjny. Nie jest implementacją ani miejscem na pozostawianie ukrytych decyzji.

## Przepływ

1. Każde zadanie implementacyjne otrzymuje własny dokument refinementu, stabilne ID i status `REFINEMENT`; umbrella refinement nie zastępuje refinementu zadania.
2. Powstaje dokument zawierający cel, zakres, poza zakresem, dane, API, błędy, współbieżność, bezpieczeństwo, failure modes i testy.
3. Każde ryzyko wysokie lub krytyczne ma mitigację albo blokuje akceptację.
4. Otwarta decyzja jest rozstrzygana albo jawnie oznaczona jako blocker.
5. Uruchamiany jest `make docs-check`.
6. Po self-review ustawiane są `Stan-Refinementu: ACCEPTED`, data i `Implementation-Allowed: YES`.
7. Dopiero wtedy zadanie może przejść do `READY` lub `IN_PROGRESS`.

## Akceptacja w zadaniu indywidualnym

Akceptacja oznacza zakończony, udokumentowany self-review właściciela rozwiązania. Nie jest akceptacją firmy rekrutującej. Jej celem jest zamrożenie decyzji i zapobieganie niekontrolowanemu zmienianiu założeń podczas kodowania.

## Amendment

Po akceptacji zmiana kontraktu wymaga:

1. ustawienia affected task na `REFINEMENT` lub `BLOCKED`;
2. aktualizacji `EMP-001` albo utworzenia dokumentu amendmentu;
3. aktualizacji decision log, risk register i ADR;
4. ponownego review;
5. nowej daty akceptacji;
6. dopiero potem wznowienia implementacji.

## Zasady jakości

- kryteria akceptacji są mierzalne;
- poza zakresem jest jawne;
- provider ma port, timeout i failure policy;
- publiczny endpoint ma statusy oraz schema błędów;
- transakcja ma określoną granicę i zachowanie przy race condition;
- testy obejmują happy path, negatywne przypadki i concurrency;
- dokument nie używa „TBD” w stanie `ACCEPTED`.
