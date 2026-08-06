# ADR-0003 — GeoIP, zaufanie do adresu klienta i prywatność

- **Status:** Accepted
- **Data:** 2026-08-06
- **Powiązane zadania:** EMP-006, EMP-007

## Kontekst

Kraj kuponu ma ograniczać jego użycie na podstawie adresu IP. Adres IP może pochodzić bezpośrednio z połączenia albo z nagłówków reverse proxy. Jest również daną, której nie należy utrwalać bez potrzeby. Darmowy dostawca GeoIP może mieć limit, brak SLA lub okresową niedostępność.

## Decyzja

### Porty

Logika aplikacyjna korzysta z dwóch abstrakcji:

```java
interface ClientIpResolver {
    InetAddress resolve(HttpServletRequest request);
}

interface GeoLocationResolver {
    CountryCode resolve(InetAddress ipAddress);
}
```

### Dostawca

Domyślny adapter HTTP użyje konfigurowalnego endpointu `ipwho.is` i pobierze wyłącznie pola potrzebne do określenia `country_code`. Adapter jest wymienny i nie przenika do domeny.

Dla lokalnego uruchomienia istnieje osobny adapter `stub`, aktywowany wyłącznie profilem `local` lub `test` i wymagający jawnego `GEOLOCATION_STUB_COUNTRY`. Profil produkcyjny nie może uruchomić się ze stubem. Nie powstaje żaden publiczny nagłówek pozwalający klientowi narzucić kraj.

### Budżety

- connect timeout: 500 ms;
- response timeout: 1 s;
- brak automatycznego retry w ścieżce requestu;
- timeout, HTTP 429/5xx, błędny JSON lub `success=false` mapują się na `503 GEOLOCATION_UNAVAILABLE`;
- redirecty 300–399 są odrzucane bez śledzenia `Location`, a body odpowiedzi ma limit 16 KiB;
- awaria dostawcy nigdy nie jest mapowana na `COUNTRY_NOT_ALLOWED`.

### Zaufanie do proxy

- domyślnie źródłem jest remote address połączenia;
- `Forwarded` i `X-Forwarded-For` są honorowane wyłącznie, gdy aplikacja działa za jawnie skonfigurowanym zaufanym proxy;
- wielokrotne fizyczne field-lines są odrzucane, a boundary proxy usuwa klientskie nagłówki i generuje jeden kanoniczny;
- klient publiczny nie może sam wymusić kraju nagłówkiem;
- konfiguracja zaufanego proxy jest częścią wdrożenia, nie logiki domenowej.

### Prywatność

- surowy adres IP jest przetwarzany tylko w pamięci na czas requestu;
- baza przechowuje jedynie rozpoznany kod kraju i czas użycia;
- surowy IP nie jest logowany;
- nie jest tworzony trwały hash IP, ponieważ nie jest potrzebny do wymagań biznesowych;
- przyszła produkcja wymaga przeglądu prywatności i umowy z dostawcą GeoIP.

## Rozważone alternatywy

### Przechowywanie IP przy każdym użyciu

Odrzucone. Nie jest potrzebne do reguł zadania i zwiększa zakres danych osobowych.

### Lokalna baza GeoLite

Odłożona. Usuwa zależność runtime od API, ale wymaga procesu pobierania, aktualizacji i zgodności licencyjnej.

### Cache GeoIP

Odłożony do pomiarów. Cache zwiększa złożoność i wymaga decyzji o kluczu, TTL oraz prywatności. Port umożliwia dodanie go bez zmiany domeny.

### Nagłówek testowy kraju

Odrzucony w publicznym API. Testy korzystają z fake adaptera, WireMock albo lokalnego adaptera profilowego, nie z bypassu dostępnego klientom.

## Konsekwencje

- serwis odmawia użycia kuponu, gdy nie potrafi wiarygodnie określić kraju;
- darmowy provider jest wystarczający do demonstracji, ale nie ma gwarantowanej dostępności produkcyjnej;
- testy są deterministyczne i nie wykonują publicznych requestów;
- lokalny Docker Compose może demonstrować regułę kraju dla adresu loopback bez osłabiania publicznego API;
- aplikacja minimalizuje utrwalane dane.
