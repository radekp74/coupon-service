#!/usr/bin/env python3
"""Validate the implemented EMP-006 trusted-client-IP and GeoIP contract."""

from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/pl/radoslawpiatek/couponservice/geolocation"
TEST = ROOT / "src/test/java/pl/radoslawpiatek/couponservice/geolocation"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def require(path: Path, tokens: list[str], errors: list[str]) -> None:
    if not path.is_file():
        errors.append(f"missing EMP-006 implementation file: {path.relative_to(ROOT)}")
        return
    text = read(path)
    for token in tokens:
        if token not in text:
            errors.append(f"{path.relative_to(ROOT)} missing token: {token}")


def main() -> int:
    errors: list[str] = []
    require(JAVA / "domain/ClientIpAddress.java", ["parseLiteral", "InetAddress.getByName", "never resolves host names"], errors)
    require(JAVA / "ports/ClientIpResolver.java", ["HttpServletRequest", "ClientIpResolutionException"], errors)
    require(JAVA / "ports/GeoLocationResolver.java", ["GeolocationUnavailableException", "CountryCode"], errors)
    require(JAVA / "adapters/ServletClientIpResolver.java", [
        'request.getHeaders("Forwarded")', 'request.getHeaders("X-Forwarded-For")',
        "forwarded.size() > 1 || xForwardedFor.size() > 1", "properties.maxHeaderLength()",
        "properties.maxForwardedHops()", "properties.mode() == ClientIpProperties.Mode.DIRECT",
    ], errors)
    require(JAVA / "adapters/ForwardedHeaderParser.java", ['value.startsWith("[")', "validPort", "maximumHops"], errors)
    require(JAVA / "adapters/CidrBlock.java", ["prefixLength", "candidate.length != network.length"], errors)
    require(JAVA / "adapters/PublicIpAddressPolicy.java", ["100.64.0.0/10", "2001:db8::/32", "fc00::/7"], errors)
    require(JAVA / "adapters/IpWhoisGeoLocationResolver.java", [
        "HttpClient", "HttpResponse.BodyHandlers.ofInputStream()",
        "Accept-Encoding", "maximumResponseBodyBytes + 1", "Content-Length", "GeolocationUnavailableException",
        "fields=success,country_code,message",
    ], errors)
    require(JAVA / "configuration/GeolocationConfiguration.java", [
        "HttpClient.Redirect.NEVER", 'profile.equals("local") || profile.equals("test")', "StubGeoLocationResolver",
    ], errors)
    require(ROOT / "src/main/resources/application.yml", ["client-ip:", "mode: direct", "maximum-response-body-bytes: 16384"], errors)
    require(ROOT / "src/main/resources/application-local.yml", ["provider: stub", "stub-country: PL"], errors)
    require(TEST / "adapters/ServletClientIpResolverTest.java", [
        "failsClosedForMultiplePhysicalHeaderLinesBeforePrecedence", "directModeIgnoresSpoofedForwardingHeaders",
        "supportsBracketedIpv6WithValidatedPortInForwarded", "forwardedWinsOverConflictingXffAndNeverFallsBackAfterError",
    ], errors)
    require(TEST / "adapters/IpWhoisGeoLocationResolverTest.java", [
        "WireMockServer", "redirectDoesNotFollowLocationAndMakesExactlyOneRequest",
        "rejectsOversizedDeclaredAndStreamingBodiesWithoutLeakage", "acceptsExactlySixteenKiBBeforeJsonParsing",
        "responseTimeoutDoesNotRetryOrRevealAddress",
    ], errors)
    require(TEST / "configuration/GeolocationConfigurationTest.java", ["stubRequiresLocalOrTestProfile", "ipwhoisStartsWithoutPerformingAProviderRequest"], errors)

    smoke = ROOT / "scripts/docker_smoke.sh"
    require(smoke, [
        'APP_PORT="${APP_PORT:-0}"',
        'COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:-coupon-service-verify-$$}"',
        "compose -p \"$COMPOSE_PROJECT_NAME\" -f docker-compose.yml port app 8080",
        'base_url="http://127.0.0.1:${smoke_port}"',
        "trap cleanup EXIT",
        "down --volumes --remove-orphans",
    ], errors)
    smoke_text = read(smoke) if smoke.is_file() else ""
    if "18080" in smoke_text:
        errors.append("Docker smoke must not reserve a fixed port")
    if "docker stop" in smoke_text or "docker rm" in smoke_text:
        errors.append("Docker smoke must clean up only its own Compose project")

    compose = read(ROOT / "docker-compose.yml")
    if '127.0.0.1:${APP_PORT:-8080}:8080' not in compose:
        errors.append("docker-compose.yml must bind the application port to loopback")

    pom = read(ROOT / "pom.xml")
    for token in ["<wiremock.version>3.13.2</wiremock.version>", "<artifactId>wiremock-standalone</artifactId>"]:
        if token not in pom:
            errors.append(f"pom.xml missing EMP-006 dependency token: {token}")
    openapi = read(ROOT / "docs/api/openapi.yaml")
    current_status = read(ROOT / "docs/project/current-status.md")
    if "/api/v1/coupons/{code}/redemptions" in openapi and not any(
            f"Implementation EMP-004:** `{state}`" in current_status for state in {"IN_PROGRESS", "DONE_AND_VERIFIED"}):
        errors.append("EMP-006 must not add redemption before EMP-004 implementation starts")
    if list(ROOT.rglob("CODEX_PROMPT.md")):
        errors.append("repository contains forbidden CODEX_PROMPT.md")
    for path in JAVA.rglob("*.java"):
        text = read(path)
        if "Logger" in text or "MDC" in text or "raw IP" in text and "Javadoc" not in text:
            errors.append(f"privacy review required for geolocation source: {path.relative_to(ROOT)}")

    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1
    print("SUCCESS: EMP-006 trusted client IP and GeoIP implementation contract valid")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
