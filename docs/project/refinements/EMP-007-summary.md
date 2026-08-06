# Podsumowanie refinementu EMP-007

## Decyzja

`EMP-007` jest zaakceptowany do implementacji. Testerzy otrzymają Swagger UI oparte na jednym canonical pliku `docs/api/openapi.yaml`, a publiczne kontrakty Java otrzymają proporcjonalny Javadoc.

## Najważniejsze granice

- OpenAPI opisuje wyłącznie działający endpoint create coupon;
- `/openapi.yaml` jest pakowane z canonical pliku z repozytorium;
- Swagger UI używa canonical spec, nie niezależnie generowanego kontraktu;
- Swagger UI jawnie używa `/openapi.yaml`; ewentualny generated `/v3/api-docs` jest wyłącznie diagnostyczny;
- nie komentujemy każdej zmiennej ani oczywistych instrukcji;
- redemption pozostaje zablokowane do EMP-006 i własnego refinementu EMP-004.

## Evidence

Closeout wymaga `make emp007-check`, Maven DocLint, testu HTTP UI/YAML, pełnego `make verify` i source exportu.
