# EMP-010 — podsumowanie refinementu

EMP-010 ma przenieść zweryfikowany lokalny gate do GitHub Actions, utwardzić źródłowy artefakt dostawy oraz dodać minimalną obserwowalność zapisaną już w EMP-001/006. Refinement pozostaje zaakceptowany i zamrożony. EMP-010 jest `IN_PROGRESS`, implementation `IN_PROGRESS`, `Implementation-Allowed: YES`, a CI/delivery/observability evidence pozostają `NOT_MEASURED` do zakończenia pełnych gate’ów.

## Zaakceptowany kontrakt

- jeden GitHub Actions job na `ubuntu-24.04`, Java 21, uruchamiający `DOCKER=docker make verify` oraz `make delivery-check`;
- minimalne `contents: read`, brak secrets/publish/deploy, wszystkie `uses:` przypięte do pełnych upstream SHA;
- immutable digesty dwóch baz Eclipse Temurin;
- byte-reproducible source ZIP z tracked files, bez timestampu ściennego i bez mutowania repo;
- Prometheus registry oraz `/actuator/prometheus`;
- sześć low-cardinality meter families bez IP/userId/coupon code/request ID/country w labels;
- strict `X-Request-Id`, fallback UUID, response header, MDC i cleanup;
- JSON Logstash przez wbudowane structured logging Spring Boot w runtime kontenerowym;
- bez Grafany, OpenTelemetry, alertów, registry publish, SBOM/signing i deploymentu.

## Review

Właściciel Radosław Piątek zaakceptował refinement i wszystkie osiem decyzji 2026-08-07. Stan końcowy checkpointu refinementu: `READY / ACCEPTED / NOT_STARTED / Implementation-Allowed: YES`. Evidence pozostaje uczciwie `NOT_MEASURED`; dopiero osobny checkpoint może dodać workflow, Prometheus, request ID, metryki i delivery implementation.

## Checkpoint implementacyjny

Rozpoczęto implementację wyłącznie zaakceptowanego zakresu: CI, immutable Docker bases, deterministyczny source ZIP, `X-Request-Id`, structured Logstash JSON oraz Prometheus/Micrometer. Closeout i evidence wymagają jeszcze pełnego lokalnego gate, delivery check i rzeczywistego GitHub Actions run.
