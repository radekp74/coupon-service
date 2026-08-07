#!/usr/bin/env python3
"""Validate EMP-010 implementation and deterministic delivery evidence."""
from __future__ import annotations

import argparse
import hashlib
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
CHECKOUT_SHA = "3d3c42e5aac5ba805825da76410c181273ba90b1"
SETUP_JAVA_SHA = "b6effb05e454b25005698d916606bdc6ffcbf961"
JDK_DIGEST = "55fb9bf738f5d9b4a6c01b39337e3070d3e27370dd3c478fd1d5d3cd2233c6d8"
JRE_DIGEST = "3097cbbebb7d490494a98aed2301f284b38f79eba158eef098c6fc8c8af11c23"
CRITICAL_METERS = {
    "coupon.create", "coupon.redemption", "client.ip.resolution",
    "geolocation.resolution", "geolocation.provider", "coupon.redemption.transaction",
}
FORBIDDEN_LABEL_TOKENS = ["userId", "couponCode", "requestId", "resolvedCountry", "countryCode", "rawIp"]


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def require(condition: bool, message: str, errors: list[str]) -> None:
    if not condition:
        errors.append(message)


def static_check() -> list[str]:
    errors: list[str] = []
    workflow = read(".github/workflows/ci.yml")
    require("pull_request:" in workflow and "workflow_dispatch:" in workflow and "branches: [main]" in workflow,
            "CI triggers are incomplete", errors)
    require("runs-on: ubuntu-24.04" in workflow and "timeout-minutes: 30" in workflow,
            "CI runner/timeout contract missing", errors)
    require("permissions:\n  contents: read" in workflow, "CI permissions must be contents: read", errors)
    require("pull_request_target" not in workflow, "pull_request_target is forbidden", errors)
    require(f"actions/checkout@{CHECKOUT_SHA}" in workflow and "persist-credentials: false" in workflow,
            "checkout must use the frozen full SHA with credentials disabled", errors)
    require(f"actions/setup-java@{SETUP_JAVA_SHA}" in workflow and "distribution: temurin" in workflow and "java-version: '21'" in workflow,
            "setup-java must use the frozen full SHA for Temurin 21", errors)
    for match in re.findall(r"uses:\s*([^\s]+)", workflow):
        require(bool(re.fullmatch(r"[^@\s]+@[0-9a-f]{40}", match)), f"mutable GitHub Action reference: {match}", errors)
    require("DOCKER=docker make verify" in workflow and "make delivery-check" in workflow and "git diff --exit-code" in workflow,
            "CI must run canonical verify, tracked diff check and delivery gate", errors)
    require("services:" not in workflow, "CI must not define a PostgreSQL service container", errors)
    require("secrets." not in workflow and "write" not in workflow, "CI must not require secrets or write permissions", errors)
    require("cancel-in-progress: true" in workflow, "CI concurrency cancellation missing", errors)

    dockerfile = read("Dockerfile")
    require(f"eclipse-temurin:21-jdk-jammy@sha256:{JDK_DIGEST}" in dockerfile, "JDK base image digest mismatch", errors)
    require(f"eclipse-temurin:21-jre-jammy@sha256:{JRE_DIGEST}" in dockerfile, "JRE base image digest mismatch", errors)

    pom = read("pom.xml")
    require("<artifactId>micrometer-registry-prometheus</artifactId>" in pom, "Prometheus registry dependency missing", errors)
    block = re.search(r"<dependency>\s*<groupId>io\.micrometer</groupId>\s*<artifactId>micrometer-registry-prometheus</artifactId>(.*?)</dependency>", pom, re.S)
    require(block is not None and "<version>" not in block.group(1), "Prometheus dependency must use Spring Boot BOM", errors)

    app = read("src/main/resources/application.yml")
    require("include: health,info,prometheus" in app, "Prometheus actuator exposure missing", errors)
    require("show-details: never" in app, "health detail privacy contract changed", errors)
    compose = read("docker-compose.yml")
    require("LOGGING_STRUCTURED_FORMAT_CONSOLE: logstash" in compose, "container Logstash JSON configuration missing", errors)

    request_filter = read("src/main/java/pl/radoslawpiatek/couponservice/observability/web/RequestIdFilter.java")
    for token in ["X-Request-Id", "^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$", "MDC.put", "MDC.remove", "finally"]:
        require(token in request_filter, f"request-ID contract missing token {token!r}", errors)

    metrics = read("src/main/java/pl/radoslawpiatek/couponservice/observability/CouponServiceMetrics.java")
    for meter in CRITICAL_METERS:
        require(f'"{meter}"' in metrics, f"missing frozen meter {meter}", errors)
    for value in [
        "success", "conflict", "not_found", "country_not_allowed", "already_redeemed", "exhausted",
        "geolocation_unavailable", "internal_error", "direct", "forwarded", "x_forwarded_for", "failure",
        "ipwhois", "stub", "timeout", "rate_limited", "provider_error", "invalid_response", "non_public_ip", "database_error",
    ]:
        require(f'"{value}"' in metrics, f"missing frozen low-cardinality tag value {value}", errors)
    for token in FORBIDDEN_LABEL_TOKENS:
        require(f'.tag("{token}"' not in metrics, f"forbidden metric label key {token}", errors)

    create_service = read("src/main/java/pl/radoslawpiatek/couponservice/coupon/application/CreateCouponService.java")
    redeem_service = read("src/main/java/pl/radoslawpiatek/couponservice/coupon/application/RedeemCouponService.java")
    transaction_service = read("src/main/java/pl/radoslawpiatek/couponservice/coupon/application/TransactionalCouponRedemptionService.java")
    client_resolver = read("src/main/java/pl/radoslawpiatek/couponservice/geolocation/adapters/ServletClientIpResolver.java")
    provider_resolver = read("src/main/java/pl/radoslawpiatek/couponservice/geolocation/adapters/IpWhoisGeoLocationResolver.java")
    require("recordCreate" in create_service and "CreateOutcome.CONFLICT" in create_service,
            "create terminal outcome instrumentation missing", errors)
    for outcome in ["SUCCESS", "NOT_FOUND", "COUNTRY_NOT_ALLOWED", "ALREADY_REDEEMED",
                    "EXHAUSTED", "GEOLOCATION_UNAVAILABLE", "INTERNAL_ERROR"]:
        require(f"RedemptionOutcome.{outcome}" in redeem_service,
                f"redemption outcome mapping missing: {outcome}", errors)
    require("startTransactionTimer" in transaction_service and "stopTransactionTimer" in transaction_service,
            "transaction timer instrumentation missing", errors)
    require("clientIpResolver.resolve(request)" in redeem_service and "transaction.redeem" in redeem_service,
            "redemption orchestration boundary changed unexpectedly", errors)
    require("recordClientIp" in client_resolver and "ClientIpSource.FORWARDED" in client_resolver
            and "ClientIpSource.X_FORWARDED_FOR" in client_resolver,
            "client-IP outcome/source mapping missing", errors)
    require("httpClient.send" in provider_resolver and provider_resolver.count("httpClient.send") == 1,
            "GeoIP adapter must keep exactly one provider send site", errors)
    require("startGeolocationTimer" in provider_resolver and "stopGeolocationTimer" in provider_resolver,
            "GeoIP provider timer instrumentation missing", errors)

    domain_sources = "\n".join(
        path.read_text(encoding="utf-8")
        for directory in [ROOT / "src/main/java/pl/radoslawpiatek/couponservice/coupon/domain",
                          ROOT / "src/main/java/pl/radoslawpiatek/couponservice/geolocation/domain"]
        for path in directory.glob("*.java")
    )
    require("io.micrometer" not in domain_sources and "CouponServiceMetrics" not in domain_sources,
            "domain layer must remain free of Micrometer/observability dependencies", errors)

    request_test = read("src/test/java/pl/radoslawpiatek/couponservice/observability/web/RequestIdFilterTest.java")
    for token in ["reusesOneValidIncomingValue", "missingInvalidAndMultipleValuesGenerateFreshUuids",
                  "cleanupRunsEvenWhenDownstreamFails", "MDC.get"]:
        require(token in request_test, f"request-ID test evidence missing: {token}", errors)
    client_test = read("src/test/java/pl/radoslawpiatek/couponservice/geolocation/adapters/ServletClientIpResolverTest.java")
    require("recordsExactBoundedSourceAndOutcomeMetrics" in client_test
            and '"x_forwarded_for", "outcome", "success"' in client_test
            and '"forwarded", "outcome", "failure"' in client_test,
            "client-IP metric mapping test evidence missing", errors)
    provider_test = read("src/test/java/pl/radoslawpiatek/couponservice/geolocation/adapters/IpWhoisGeoLocationResolverTest.java")
    for outcome in ["success", "rate_limited", "invalid_response", "provider_error", "timeout", "non_public_ip"]:
        require(f'"{outcome}"' in provider_test, f"GeoIP metric mapping test evidence missing: {outcome}", errors)
    require('LOCAL_STUB_RESPONSE_TIMEOUT = Duration.ofSeconds(1)' in provider_test
            and 'http://127.0.0.1:' in provider_test
            and 'http://localhost:' not in provider_test,
            "GeoIP WireMock tests must use deterministic IPv4 loopback and a test-only response margin", errors)

    observability_it = read("src/test/java/pl/radoslawpiatek/couponservice/observability/ObservabilityApiIT.java")
    require('management.endpoints.web.exposure.include' in observability_it
            and 'health,info,prometheus' in observability_it,
            "ObservabilityApiIT must explicitly expose Prometheus because test application.yml shadows main application.yml", errors)
    require("org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability" in observability_it
            and "@AutoConfigureObservability" in observability_it,
            "ObservabilityApiIT must enable real metrics exporters under @SpringBootTest", errors)
    require("java.net.http.HttpClient" in observability_it
            and "HttpClient.newHttpClient()" in observability_it
            and "TestRestTemplate" not in observability_it,
            "ObservabilityApiIT must use an uninstrumented JDK HTTP client so test request URLs cannot pollute the Prometheus scrape", errors)

    geolocation_configuration_test = read("src/test/java/pl/radoslawpiatek/couponservice/geolocation/configuration/GeolocationConfigurationTest.java")
    require("withBean(CouponServiceMetrics.class" in geolocation_configuration_test
            and "SimpleMeterRegistry" in geolocation_configuration_test,
            "GeolocationConfigurationTest must provide the observability dependency in its isolated context", errors)

    openapi = read("docs/api/openapi.yaml")
    require("X-Request-Id:" in openapi and "#/components/headers/RequestId" in openapi,
            "canonical OpenAPI request-ID response header missing", errors)

    package_py = read("scripts/package_source.py")
    for token in ["git\", \"ls-files", "FIXED_TIME", "CHECKSUMS.sha256", "strict_timestamps=True", "compresslevel=9"]:
        require(token in package_py, f"deterministic packaging token missing: {token}", errors)
    require("date -u" not in package_py and "time.time" not in package_py, "packaging must not use wall-clock time", errors)

    smoke = read("scripts/docker_smoke.sh")
    for token in ["/actuator/prometheus", "X-Request-Id: smoke-request-010",
                  "container structured JSON logging detected"]:
        require(token in smoke, f"Docker smoke observability evidence missing: {token}", errors)

    makefile = read("Makefile")
    require(re.search(r"^emp010-check:\s*$", makefile, re.M) is not None, "make emp010-check missing", errors)
    require(re.search(r"^delivery-check:\s*$", makefile, re.M) is not None, "make delivery-check missing", errors)
    require("scripts/check_emp010.py" in makefile, "Makefile does not invoke EMP-010 checker", errors)

    require(not list(ROOT.rglob("CODEX_PROMPT.md")), "CODEX_PROMPT.md is forbidden", errors)
    return errors


def file_sha(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def run_export(repo: Path, output: Path) -> tuple[Path, str]:
    env = os.environ.copy()
    env["SOURCE_EXPORT_DIR"] = str(output)
    completed = subprocess.run(["bash", "scripts/package_source.sh"], cwd=repo, env=env,
                               text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    if completed.returncode != 0:
        raise RuntimeError("source export failed:\n" + completed.stdout)
    zips = list(output.glob("*.zip"))
    if len(zips) != 1:
        raise RuntimeError(f"expected exactly one ZIP, found {len(zips)}")
    return zips[0], file_sha(zips[0])


def assert_archive_hygiene(archive: Path) -> None:
    with zipfile.ZipFile(archive) as zf:
        names = zf.namelist()
    forbidden = [name for name in names if any(part in name.split("/") for part in
                 [".git", "target", "build", "dist", "coverage", "__pycache__"])
                 or name.endswith("CODEX_PROMPT.md") or name.endswith(".zip")]
    if forbidden:
        raise RuntimeError("forbidden archive entries: " + ", ".join(forbidden))


def delivery_check(repo: Path) -> None:
    if subprocess.check_output(["git", "status", "--porcelain=v1", "-z"], cwd=repo):
        raise RuntimeError("delivery-check requires a clean Git working tree")
    with tempfile.TemporaryDirectory(prefix="emp010-delivery-") as temp:
        temp_path = Path(temp)
        first, first_sha = run_export(repo, temp_path / "one")
        second, second_sha = run_export(repo, temp_path / "two")
        if first_sha != second_sha:
            raise RuntimeError(f"deterministic exports differ: {first_sha} != {second_sha}")
        assert_archive_hygiene(first)
        assert_archive_hygiene(second)

        clone = temp_path / "mutation-repo"
        subprocess.check_call(["git", "clone", "--quiet", "--no-hardlinks", str(repo), str(clone)])
        subprocess.check_call(["git", "config", "user.name", "EMP-010 self-test"], cwd=clone)
        subprocess.check_call(["git", "config", "user.email", "emp010@example.invalid"], cwd=clone)
        with (clone / "README.md").open("a", encoding="utf-8") as handle:
            handle.write("\nEMP-010 controlled checksum mutation.\n")
        subprocess.check_call(["git", "add", "README.md"], cwd=clone)
        subprocess.check_call(["git", "commit", "--quiet", "-m", "controlled stale checksum mutation"], cwd=clone)
        env = os.environ.copy()
        env["SOURCE_EXPORT_DIR"] = str(temp_path / "mutation-out")
        mutated = subprocess.run(["bash", "scripts/package_source.sh"], cwd=clone, env=env,
                                 text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        if mutated.returncode == 0 or "stale checksum" not in mutated.stdout:
            raise RuntimeError("stale-checksum mutation did not fail closed as expected:\n" + mutated.stdout)
    print(f"SUCCESS: deterministic delivery reproduced byte-for-byte: {first_sha}")
    print("SUCCESS: stale tracked-source checksum mutation fails closed")


def candidate_delivery_check() -> None:
    """Exercise delivery logic from a dirty implementation candidate in an isolated Git repo."""
    with tempfile.TemporaryDirectory(prefix="emp010-candidate-") as temp:
        clone = Path(temp) / "repo"
        shutil.copytree(ROOT, clone, ignore=shutil.ignore_patterns(".git", "target", "build", "dist", "__pycache__", "*.zip"))
        subprocess.check_call(["git", "init", "-q"], cwd=clone)
        subprocess.check_call(["git", "config", "user.name", "EMP-010 candidate"], cwd=clone)
        subprocess.check_call(["git", "config", "user.email", "emp010@example.invalid"], cwd=clone)
        subprocess.check_call([sys.executable, "scripts/generate_checksums.py"], cwd=clone)
        subprocess.check_call(["git", "add", "-A"], cwd=clone)
        subprocess.check_call(["git", "commit", "--quiet", "-m", "candidate"], cwd=clone)
        delivery_check(clone)
    print("SUCCESS: candidate deterministic delivery self-test passed")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--delivery-check", action="store_true")
    parser.add_argument("--candidate-delivery-check", action="store_true")
    args = parser.parse_args()
    errors = static_check()
    if errors:
        for error in errors:
            print("ERROR: " + error, file=sys.stderr)
        return 1
    print("SUCCESS: EMP-010 static implementation contract valid")
    try:
        if args.delivery_check:
            delivery_check(ROOT)
        if args.candidate_delivery_check:
            candidate_delivery_check()
    except (RuntimeError, subprocess.CalledProcessError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
