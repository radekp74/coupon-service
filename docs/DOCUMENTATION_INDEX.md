# Pełny indeks dokumentacji

Każdy plik Markdown w `docs/` musi być wymieniony poniżej. Indeks jest sprawdzany przez `make docs-check`.

## Start

- [Dokumentacja projektu](README.md)

## Zarządzanie projektem

- [Backlog](project/backlog.md)
- [Aktualny status projektu](project/current-status.md)
- [Rejestr decyzji](project/decision-log.md)
- [Definition of Ready i Definition of Done](project/definition-of-ready-and-done.md)
- [Governance dokumentacji](project/documentation-governance.md)
- [Lessons learned](project/lessons-learned.md)
- [Proces refinementu](project/refinement-process.md)
- [Review checklist EMP-001](project/refinements/EMP-001-review-checklist.md)
- [Review checklist EMP-003](project/refinements/EMP-003-review-checklist.md)
- [Review checklist EMP-004](project/refinements/EMP-004-review-checklist.md)
- [Review checklist EMP-006](project/refinements/EMP-006-review-checklist.md)
- [Review checklist EMP-007](project/refinements/EMP-007-review-checklist.md)
- [Podsumowanie refinementu EMP-001](project/refinements/EMP-001-summary.md)
- [Podsumowanie refinementu EMP-003](project/refinements/EMP-003-summary.md)
- [Podsumowanie refinementu EMP-004](project/refinements/EMP-004-summary.md)
- [Podsumowanie refinementu EMP-006](project/refinements/EMP-006-summary.md)
- [Podsumowanie refinementu EMP-007](project/refinements/EMP-007-summary.md)
- [Refinement EMP-001 — kontrakt rozwiązania serwisu kuponowego](project/refinements/EMP-001.md)
- [Refinement EMP-003 — tworzenie kuponu i unikalność case-insensitive](project/refinements/EMP-003.md)
- [Refinement EMP-004 — transakcyjne wykorzystanie kuponu](project/refinements/EMP-004.md)
- [Refinement EMP-006 — Client IP i provider-neutral GeoIP](project/refinements/EMP-006.md)
- [Refinement EMP-007 — OpenAPI, Swagger UI i Javadoc](project/refinements/EMP-007.md)
- [Rejestr refinementów](project/refinements/README.md)
- [Historia checkpointów](project/release-history.md)
- [Rejestr ryzyk](project/risk-register.md)

## Architektura

- [Model danych](architecture/data-model.md)
- [Architektura rozwiązania](architecture/overview.md)

## API

- [Kontrakt REST API](api/api-contract.md)
- [OpenAPI — canonical public API contract](api/openapi.yaml)

## Testy

- [Strategia testów](testing/test-strategy.md)

## Wymagania produktu

- [Traceability wymagań zadania](product/requirements-traceability.md)

## Decyzje architektoniczne

- [ADR-0001 — modularny monolit i stos technologiczny](adr/ADR-0001-modular-monolith-and-technology-stack.md)
- [ADR-0002 — współbieżność i spójność wykorzystania kuponu](adr/ADR-0002-concurrency-and-redemption-consistency.md)
- [ADR-0003 — GeoIP, zaufanie do adresu klienta i prywatność](adr/ADR-0003-geolocation-client-ip-and-privacy.md)
- [Architecture Decision Records](adr/README.md)
