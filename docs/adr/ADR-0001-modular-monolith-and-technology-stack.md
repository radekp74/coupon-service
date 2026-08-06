# ADR-0001 — modularny monolit i stos technologiczny

- **Status:** Accepted
- **Data:** 2026-08-06
- **Powiązane zadania:** EMP-001, EMP-002

## Kontekst

Rozwiązanie ma być czytelne, możliwe do uruchomienia lokalnie, skalowalne na wiele instancji i łatwe do obrony podczas rozmowy technicznej. Zakres obejmuje dwa główne przypadki użycia, dlatego mikroserwisy lub rozbudowany framework domenowy zwiększyłyby koszt bez proporcjonalnej wartości.

## Decyzja

Powstanie pojedyncza aplikacja Spring Boot w formie modularnego monolitu:

- Java 21 LTS;
- Spring Boot 3.5.16;
- Maven Wrapper;
- PostgreSQL 18;
- Flyway;
- Spring JDBC i `JdbcClient`;
- pakiety organizowane według funkcji domenowej, z wyraźnymi portami i adapterami;
- Testcontainers dla PostgreSQL;
- WireMock dla HTTP GeoIP;
- Spring Actuator dla podstawowego health checku;
- wieloetapowy Dockerfile i Docker Compose jako lokalny, powtarzalny kontrakt uruchomienia aplikacji z PostgreSQL.

Spring Boot 3.5.x został wybrany świadomie zamiast najnowszej głównej linii 4.x: zadanie jest krótkie, Java 21 jest dojrzałym celem, a stabilność i znajomość kodu są ważniejsze od demonstracji najnowszego major release.

## Planowana struktura

```text
pl.radoslawpiatek.couponservice
├── coupon
│   ├── domain
│   ├── application
│   ├── port
│   └── adapter
├── geolocation
│   ├── application
│   ├── port
│   └── adapter
├── api
└── configuration
```

## Rozważone alternatywy

### Mikroserwisy

Odrzucone. Brak niezależnych cykli wdrożeniowych i granic skalowania uzasadniających koszt komunikacji sieciowej oraz spójności rozproszonej.

### Spring Data JPA

Odrzucone dla głównej ścieżki zapisu. Jawny SQL ułatwia pokazanie blokady rekordu, kolejności operacji i warunków integralności. JPA nie jest błędne, ale w tym zadaniu ukrywa kluczowy mechanizm oceniany przez zespół techniczny.

### Redis/Kafka

Odrzucone. PostgreSQL jest wystarczającym source of truth, a dodatkowe komponenty zwiększyłyby powierzchnię awarii i utrudniły lokalne uruchomienie.

### Spring Boot 4.x / Java 25

Odłożone. Są poprawną opcją dla nowego produktu, lecz nie dostarczają wartości biznesowej wymaganej przez zadanie i zwiększają ryzyko rozmowy o nowościach zamiast o właściwej spójności domeny.

## Konsekwencje

### Pozytywne

- prosty deployment i lokalne uruchomienie przez Maven albo Docker Compose;
- obraz runtime uruchamiany bez uprawnień root i sprawdzany przez Actuator health;
- jawne transakcje i SQL;
- łatwe testy integracyjne;
- brak zależności od infrastruktury rozproszonej;
- możliwość horyzontalnego skalowania bez sesji w pamięci aplikacji.

### Negatywne

- popularny pojedynczy kupon pozostaje punktem kontencji na rekordzie bazy;
- ręczne mapowanie SQL wymaga dyscypliny;
- jedna aplikacja ma wspólny cykl wydania.
