#!/usr/bin/env python3
"""Validate the EMP-002 bootstrap contract without downloading dependencies."""

from __future__ import annotations

import os
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Iterable, List, Set, Tuple

ROOT = Path(__file__).resolve().parents[1]
NS = {"m": "http://maven.apache.org/POM/4.0.0"}

REQUIRED_FILES = [
    ROOT / "pom.xml",
    ROOT / "mvnw",
    ROOT / "mvnw.cmd",
    ROOT / ".mvn" / "wrapper" / "maven-wrapper.properties",
    ROOT / "src" / "main" / "java" / "pl" / "radoslawpiatek" / "couponservice" / "CouponServiceApplication.java",
    ROOT / "src" / "main" / "resources" / "application.yml",
    ROOT / "src" / "main" / "resources" / "db" / "migration" / "V1__create_coupon_tables.sql",
    ROOT / "src" / "test" / "java" / "pl" / "radoslawpiatek" / "couponservice" / "DatabaseMigrationIT.java",
    ROOT / "Dockerfile",
    ROOT / ".dockerignore",
    ROOT / "docker-compose.yml",
    ROOT / "scripts" / "docker_smoke.sh",
]

REQUIRED_DEPENDENCIES: Set[Tuple[str, str]] = {
    ("org.springframework.boot", "spring-boot-starter-web"),
    ("org.springframework.boot", "spring-boot-starter-validation"),
    ("org.springframework.boot", "spring-boot-starter-jdbc"),
    ("org.springframework.boot", "spring-boot-starter-actuator"),
    ("org.flywaydb", "flyway-core"),
    ("org.flywaydb", "flyway-database-postgresql"),
    ("org.postgresql", "postgresql"),
    ("org.springframework.boot", "spring-boot-starter-test"),
    ("org.testcontainers", "testcontainers-junit-jupiter"),
    ("org.testcontainers", "testcontainers-postgresql"),
}

REQUIRED_PLUGINS: Set[Tuple[str, str]] = {
    ("org.springframework.boot", "spring-boot-maven-plugin"),
    ("org.apache.maven.plugins", "maven-enforcer-plugin"),
    ("org.apache.maven.plugins", "maven-failsafe-plugin"),
}


class ValidationError(Exception):
    pass


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def child_text(node: ET.Element, name: str) -> str:
    child = node.find(f"m:{name}", NS)
    return "" if child is None or child.text is None else child.text.strip()


def coordinates(nodes: Iterable[ET.Element]) -> Set[Tuple[str, str]]:
    return {(child_text(node, "groupId"), child_text(node, "artifactId")) for node in nodes}


def validate_pom(errors: List[str]) -> None:
    try:
        root = ET.parse(ROOT / "pom.xml").getroot()
    except ET.ParseError as exc:
        errors.append(f"pom.xml is not valid XML: {exc}")
        return

    parent = root.find("m:parent", NS)
    if parent is None or child_text(parent, "artifactId") != "spring-boot-starter-parent":
        errors.append("pom.xml does not use spring-boot-starter-parent")
    elif child_text(parent, "version") != "3.5.16":
        errors.append("Spring Boot must be pinned to 3.5.16")

    properties = root.find("m:properties", NS)
    if properties is None:
        errors.append("pom.xml has no properties")
    else:
        java_version = properties.find("m:java.version", NS)
        release = properties.find("m:maven.compiler.release", NS)
        testcontainers = properties.find("m:testcontainers.version", NS)
        if java_version is None or java_version.text != "21":
            errors.append("java.version must be 21")
        if release is None or release.text != "21":
            errors.append("maven.compiler.release must be 21")
        if testcontainers is None or testcontainers.text != "2.0.5":
            errors.append("Testcontainers must be pinned to 2.0.5")

    dependencies = coordinates(root.findall("m:dependencies/m:dependency", NS))
    for dependency in sorted(REQUIRED_DEPENDENCIES - dependencies):
        errors.append(f"missing Maven dependency: {dependency[0]}:{dependency[1]}")

    plugins = coordinates(root.findall("m:build/m:plugins/m:plugin", NS))
    for plugin in sorted(REQUIRED_PLUGINS - plugins):
        errors.append(f"missing Maven plugin: {plugin[0]}:{plugin[1]}")

    pom_text = read(ROOT / "pom.xml")
    for token in ("[21,22)", "[3.9.16,4.0.0)", "integration-test", "verify"):
        if token not in pom_text:
            errors.append(f"pom.xml is missing required build token: {token}")


def validate_wrapper(errors: List[str]) -> None:
    properties = read(ROOT / ".mvn" / "wrapper" / "maven-wrapper.properties")
    required = {
        "apache-maven/3.9.16/apache-maven-3.9.16-bin.zip",
        "distributionSha512Sum=ed41650d42485cfc243fad22158caf9cbb5dc408ce7a09ddb94dd42a019de929ca43065bfa450612cf12bf78b5cafa3884b96c090de326ff590448c933454af3",
        "wrapperVersion=3.3.4",
    }
    for token in sorted(required):
        if token not in properties:
            errors.append(f"Maven Wrapper properties missing token: {token}")

    if not os.access(ROOT / "mvnw", os.X_OK):
        errors.append("mvnw is not executable")
    script = read(ROOT / "mvnw")
    if "verify_sha512" not in script or "exec \"$MAVEN_BIN\" \"$@\"" not in script:
        errors.append("mvnw does not enforce the pinned Maven distribution")


def validate_migration(errors: List[str]) -> None:
    sql = read(ROOT / "src" / "main" / "resources" / "db" / "migration" / "V1__create_coupon_tables.sql")
    required = [
        "CREATE TABLE coupons",
        "CREATE TABLE coupon_redemptions",
        "UNIQUE (normalized_code)",
        "UNIQUE (coupon_id, user_id)",
        "normalized_code = upper(code)",
        "current_uses >= 0 AND current_uses <= max_uses",
        "FOREIGN KEY (coupon_id)",
        "resolved_country_code",
    ]
    for token in required:
        if token not in sql:
            errors.append(f"V1 migration missing invariant: {token}")

    forbidden = ["ip_address", "client_ip", " INET", " inet"]
    for token in forbidden:
        if token in sql:
            errors.append(f"V1 migration persists a forbidden raw IP field/type: {token}")


def validate_java_contract(errors: List[str]) -> None:
    app = read(REQUIRED_FILES[4])
    if "@SpringBootApplication" not in app or "SpringApplication.run" not in app:
        errors.append("CouponServiceApplication is not a valid Spring Boot entry point")

    test = read(REQUIRED_FILES[7])
    required = [
        "@Testcontainers",
        "PostgreSQLContainer",
        'postgres:18.4-alpine',
        "org.testcontainers.postgresql.PostgreSQLContainer",
        "@DynamicPropertySource",
        "flywayCreatesTheCouponSchemaOnPostgreSql",
        "databaseEnforcesCaseInsensitiveCanonicalCodeUniqueness",
        "databaseRejectsAnInconsistentCanonicalCode",
        "databaseRejectsUsageCountAboveTheCouponLimit",
    ]
    for token in required:
        if token not in test:
            errors.append(f"DatabaseMigrationIT missing token: {token}")
    if "org.testcontainers.containers.PostgreSQLContainer" in test:
        errors.append("DatabaseMigrationIT uses the removed Testcontainers 1.x PostgreSQL package")


def validate_container_contract(errors: List[str]) -> None:
    dockerfile = read(ROOT / "Dockerfile")
    for pattern in (
        r"^FROM eclipse-temurin:21-jdk-jammy(?:@sha256:[0-9a-f]{64})? AS build$",
        r"^FROM eclipse-temurin:21-jre-jammy(?:@sha256:[0-9a-f]{64})? AS runtime$",
    ):
        if not re.search(pattern, dockerfile, re.MULTILINE):
            errors.append(f"Dockerfile missing compatible base-image contract: {pattern}")

    for token in (
        "--mount=type=cache,target=/root/.m2,sharing=locked",
        "./mvnw -B -ntp -DskipTests package",
        "USER 10001:10001",
        "HEALTHCHECK",
        "/actuator/health",
        "ENTRYPOINT",
    ):
        if token not in dockerfile:
            errors.append(f"Dockerfile missing token: {token}")

    if "dependency:go-offline" in dockerfile:
        errors.append("Dockerfile must not use dependency:go-offline in the image build")

    dockerignore = read(ROOT / ".dockerignore")
    for token in ("**", "!pom.xml", "!mvnw", "!src/**"):
        if token not in dockerignore:
            errors.append(f".dockerignore missing token: {token}")

    compose = read(ROOT / "docker-compose.yml")
    for token in (
        "postgres:18.4-alpine",
        "condition: service_healthy",
        "DATABASE_URL: jdbc:postgresql://postgres:5432/coupon_service",
        '127.0.0.1:${APP_PORT:-8080}:8080',
        "coupon-postgres-data",
    ):
        if token not in compose:
            errors.append(f"docker-compose.yml missing token: {token}")

    makefile = read(ROOT / "Makefile")
    for token in (
        'DOCKER ?= /Applications/Docker.app/Contents/Resources/bin/docker',
        '"$(DOCKER)" compose',
        "compose-config:",
        "docker-build:",
        "docker-up:",
        "docker-down:",
        "docker-smoke:",
    ):
        if token not in makefile:
            errors.append(f"Makefile missing Docker contract token: {token}")

    smoke = read(ROOT / "scripts" / "docker_smoke.sh")
    for token in (
        'APP_PORT="${APP_PORT:-0}"',
        'COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:-coupon-service-verify-$$}"',
        "--wait-timeout 180",
        "compose -p \"$COMPOSE_PROJECT_NAME\" -f docker-compose.yml port app 8080",
        'base_url="http://127.0.0.1:${smoke_port}"',
        "/actuator/health",
        "down --volumes --remove-orphans",
        "trap cleanup EXIT",
    ):
        if token not in smoke:
            errors.append(f"docker_smoke.sh missing token: {token}")
    if "18080" in smoke:
        errors.append("docker_smoke.sh must not reserve a fixed host port")
    if "docker stop" in smoke or "docker rm" in smoke:
        errors.append("docker_smoke.sh must clean up only through its own Compose project")

    verify = read(ROOT / "verify.sh")
    for token in ('APP_PORT=0', 'COMPOSE_PROJECT_NAME="coupon-service-verify-$$"'):
        if token not in verify:
            errors.append(f"verify.sh missing isolated Docker smoke token: {token}")
    if "APP_PORT=18080" in verify:
        errors.append("verify.sh must not reserve host port 18080")


def validate_runtime_configuration(errors: List[str]) -> None:
    config = read(ROOT / "src" / "main" / "resources" / "application.yml")
    for token in (
        "${DATABASE_URL:",
        "${DATABASE_USERNAME:",
        "${DATABASE_PASSWORD:",
        "validate-migration-naming: true",
        "shutdown: graceful",
        "include: health,info",
    ):
        if token not in config:
            errors.append(f"application.yml missing token: {token}")


def validate_project_state(errors: List[str]) -> None:
    backlog = read(ROOT / "docs" / "project" / "backlog.md")
    if "| EMP-002 | EMP-001 | P0 | DONE_AND_VERIFIED |" not in backlog:
        errors.append("EMP-002 must be DONE_AND_VERIFIED after local runtime verification passes")

    status = read(ROOT / "docs" / "project" / "current-status.md")
    for token in (
        "BOOTSTRAP_DONE_AND_VERIFIED",
        "LOCAL_DOCKER_GATE_PASS",
        "EMP-002",
    ):
        if token not in status:
            errors.append(f"current status missing verified bootstrap state: {token}")


def main() -> int:
    errors: List[str] = []
    for path in REQUIRED_FILES:
        if not path.is_file():
            errors.append(f"missing required bootstrap file: {path.relative_to(ROOT)}")

    if errors:
        print("FAILED: EMP-002 bootstrap contract")
        for error in errors:
            print(f"- {error}")
        return 1

    validate_pom(errors)
    validate_wrapper(errors)
    validate_migration(errors)
    validate_java_contract(errors)
    validate_container_contract(errors)
    validate_runtime_configuration(errors)
    validate_project_state(errors)

    if errors:
        print("FAILED: EMP-002 bootstrap contract")
        for error in errors:
            print(f"- {error}")
        return 1

    print("SUCCESS: EMP-002 static bootstrap contract valid")
    return 0


if __name__ == "__main__":
    sys.exit(main())
