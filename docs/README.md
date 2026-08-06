# Dokumentacja projektu

Dokumentacja jest częścią rozwiązania i podlega tym samym bramkom jakości co kod.

## Nawigacja

- [Pełny indeks](DOCUMENTATION_INDEX.md)
- [Aktualny status](project/current-status.md)
- [Backlog](project/backlog.md)
- [Refinement EMP-001](project/refinements/EMP-001.md)
- [Zaakceptowany refinement EMP-006](project/refinements/EMP-006.md)
- [Architektura](architecture/overview.md)
- [Kontrakt API](api/api-contract.md)
- [Strategia testów](testing/test-strategy.md)
- [Traceability wymagań](product/requirements-traceability.md)

## Zasady

- Markdown jest źródłem prawdy.
- Każdy nowy dokument musi zostać dodany do `DOCUMENTATION_INDEX.md`.
- Linki lokalne muszą prowadzić do istniejących plików.
- Zmiana stanu implementacji wymaga aktualizacji backlogu, current status, changelogu i release history.
- Szczegóły procesu opisuje [governance dokumentacji](project/documentation-governance.md).

## Aktualny checkpoint

EMP-007 udostępnia canonical OpenAPI testerom przez `/openapi.yaml` i Swagger UI pod `/swagger-ui`. Zaakceptowany refinement EMP-006 definiuje bezpieczne rozpoznawanie Client IP, zaufane proxy, provider-neutral GeoIP i prywatność; implementacja EMP-006 może rozpocząć się w osobnym checkpointcie i ma obecnie status `NOT_STARTED`.
