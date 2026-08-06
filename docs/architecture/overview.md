# Architektura rozwiązania

## Cel

Dostarczyć mały, czytelny i produkcyjnie świadomy serwis REST bez infrastruktury, która nie wynika z wymagań.

## Styl

- modularny monolit;
- pakiety według obszaru domenowego;
- logika biznesowa niezależna od HTTP, SQL i dostawcy GeoIP;
- porty i adaptery tylko tam, gdzie istnieje realna granica wymiany;
- jedna transakcyjna baza jako source of truth;
- runtime bezstanowy.

## Główne komponenty

```text
HTTP API
  │
  ├── CreateCouponUseCase
  │     └── CouponRepository
  │            └── PostgreSQL
  │
  └── RedeemCouponUseCase
        ├── ClientIpResolver
        ├── GeoLocationResolver
        │      ├── ipwho.is adapter (runtime)
        │      └── guarded stub adapter (local/test)
        ├── CouponRepository
        └── RedemptionRepository
               └── PostgreSQL
```

## Granice odpowiedzialności

### Domena kuponu

Odpowiada za:

- canonicalizację kodu;
- dodatni limit użyć;
- zgodność kraju;
- kontrolę wyczerpania;
- semantykę jednego użycia przez użytkownika.

Nie zna Springa, HTTP, JSON ani dostawcy GeoIP.

### Warstwa aplikacyjna

Odpowiada za:

- orkiestrację przypadków użycia;
- kolejność walidacji;
- granicę transakcji;
- mapowanie wyników domenowych na stabilne outcome'y.

### Adapter HTTP

Odpowiada za:

- wersjonowane endpointy;
- walidację składni requestu;
- Problem Details;
- odczyt request ID;
- brak logowania danych wrażliwych.

### Adapter PostgreSQL

Odpowiada za:

- jawny SQL;
- blokady rekordów;
- mapowanie constraint violations;
- migracje Flyway;
- spójność liczników i rejestru użyć.

### Adapter GeoIP

Odpowiada za:

- budżet czasowy HTTP;
- format odpowiedzi dostawcy;
- normalizację kraju;
- klasyfikację awarii technicznych.

## Przepływ tworzenia kuponu

```text
request
→ walidacja DTO
→ CouponCode.normalize
→ CountryCode.validate
→ INSERT coupon
→ mapowanie unique violation na COUPON_CODE_CONFLICT
→ 201 Created
```

## Przepływ wykorzystania kuponu

```text
request
→ walidacja userId i code
→ lekki odczyt kuponu
→ ustalenie client IP
→ GeoIP poza transakcją
→ transaction
   → SELECT coupon FOR UPDATE
   → country check
   → duplicate user check
   → usage limit check
   → INSERT redemption
   → UPDATE current_uses
→ commit
→ 201 Created
```

## Skalowanie

- kolejne instancje aplikacji nie przechowują sesji ani licznika lokalnego;
- PostgreSQL koordynuje współbieżność między instancjami;
- różne kupony mogą być obsługiwane równolegle;
- hot coupon jest świadomym punktem serializacji;
- brak Redis/Kafka upraszcza spójność i recovery;
- optymalizacje będą uzasadniane pomiarami, nie założeniem.

## Powiązane dokumenty

- [Model danych](data-model.md)
- [Kontrakt API](../api/api-contract.md)
- [ADR-0001](../adr/ADR-0001-modular-monolith-and-technology-stack.md)
- [ADR-0002](../adr/ADR-0002-concurrency-and-redemption-consistency.md)
- [ADR-0003](../adr/ADR-0003-geolocation-client-ip-and-privacy.md)

## Stan implementacji

W EMP-002 istnieje entry point Spring Boot, konfiguracja runtime, migracja V1, test bootstrapu bazy, wieloetapowy Dockerfile oraz lokalny stos Docker Compose z health checkami. Warstwy domenowe, przypadki użycia oraz adaptery HTTP i GeoIP pozostają planem zaakceptowanym w EMP-001, a nie kodem istniejącym w tym checkpointcie.
