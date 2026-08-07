#!/usr/bin/env python3
"""Validate the static and measured JaCoCo contracts required by EMP-008."""
from __future__ import annotations

import argparse
import copy
from dataclasses import dataclass
from pathlib import Path
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
POM = ROOT / "pom.xml"
NS = {"m": "http://maven.apache.org/POM/4.0.0"}

GLOBAL_THRESHOLDS = {"LINE": 0.80, "BRANCH": 0.70}
CRITICAL_THRESHOLDS = {"LINE": 0.75, "BRANCH": 0.65}
CRITICAL_PATTERNS = [
    "pl/radoslawpiatek/couponservice/coupon/domain/**",
    "pl/radoslawpiatek/couponservice/coupon/application/**",
    "pl/radoslawpiatek/couponservice/coupon/adapters/persistence/**",
    "pl/radoslawpiatek/couponservice/geolocation/domain/**",
    "pl/radoslawpiatek/couponservice/geolocation/adapters/**",
]
CRITICAL_PREFIXES = tuple(pattern.removesuffix("/**") for pattern in CRITICAL_PATTERNS)


@dataclass(frozen=True)
class Counter:
    """One JaCoCo counter used to calculate an exact covered ratio."""

    missed: int
    covered: int

    @property
    def total(self) -> int:
        return self.missed + self.covered

    @property
    def ratio(self) -> float:
        return self.covered / self.total if self.total else 1.0


def text(element: ET.Element, path: str) -> str | None:
    node = element.find(path, NS)
    return node.text.strip() if node is not None and node.text else None


def execution(plugin: ET.Element, execution_id: str) -> ET.Element | None:
    for item in plugin.findall("m:executions/m:execution", NS):
        if text(item, "m:id") == execution_id:
            return item
    return None


def static_contract_errors() -> list[str]:
    errors: list[str] = []
    root = ET.parse(POM).getroot()
    properties = root.find("m:properties", NS)
    if properties is None or text(properties, "m:jacoco-maven-plugin.version") != "0.8.15":
        errors.append("pom.xml must pin jacoco-maven-plugin.version to 0.8.15")

    plugins = root.findall("m:build/m:plugins/m:plugin", NS)
    jacoco = next(
        (
            plugin
            for plugin in plugins
            if text(plugin, "m:groupId") == "org.jacoco"
            and text(plugin, "m:artifactId") == "jacoco-maven-plugin"
        ),
        None,
    )
    if jacoco is None:
        errors.append("missing org.jacoco:jacoco-maven-plugin")
    else:
        if text(jacoco, "m:version") != "${jacoco-maven-plugin.version}":
            errors.append("JaCoCo plugin must use the pinned version property")
        if jacoco.findall(".//m:excludes", NS):
            errors.append("EMP-008 forbids JaCoCo exclusions")

        prepare = execution(jacoco, "jacoco-prepare-agent")
        if prepare is None or text(prepare, "m:goals/m:goal") != "prepare-agent":
            errors.append("missing jacoco-prepare-agent execution")
        elif (
            text(prepare, "m:configuration/m:destFile") != "${project.build.directory}/jacoco.exec"
            or text(prepare, "m:configuration/m:append") != "true"
        ):
            errors.append("prepare-agent must append to target/jacoco.exec")

        report = execution(jacoco, "jacoco-report")
        if report is None or text(report, "m:phase") != "verify" or text(report, "m:goals/m:goal") != "report":
            errors.append("JaCoCo report must run during verify")

        expected = {
            "jacoco-check-global": ("0.80", "0.70", []),
            "jacoco-check-critical": ("0.75", "0.65", CRITICAL_PATTERNS),
        }
        for execution_id, (line, branch, includes) in expected.items():
            check = execution(jacoco, execution_id)
            if check is None or text(check, "m:phase") != "verify" or text(check, "m:goals/m:goal") != "check":
                errors.append(f"missing {execution_id} verify check")
                continue
            if text(check, "m:configuration/m:rules/m:rule/m:element") != "BUNDLE":
                errors.append(f"{execution_id} must use one BUNDLE aggregate")
            limits = {
                (text(limit, "m:counter"), text(limit, "m:value")): text(limit, "m:minimum")
                for limit in check.findall("m:configuration/m:rules/m:rule/m:limits/m:limit", NS)
            }
            if limits.get(("LINE", "COVEREDRATIO")) != line or limits.get(("BRANCH", "COVEREDRATIO")) != branch:
                errors.append(f"{execution_id} must enforce LINE {line} and BRANCH {branch}")
            actual_includes = [
                node.text.strip()
                for node in check.findall("m:configuration/m:includes/m:include", NS)
                if node.text
            ]
            if actual_includes != includes:
                errors.append(f"{execution_id} has incorrect critical includes")

    pom_text = POM.read_text(encoding="utf-8").lower()
    if "pitest" in pom_text:
        errors.append("EMP-008 forbids PIT")
    if list(ROOT.rglob("CODEX_PROMPT.md")):
        errors.append("forbidden CODEX_PROMPT.md")

    if (ROOT / ".git").exists():
        tracked_files = subprocess.run(
            ["git", "ls-files"], cwd=ROOT, check=True, text=True, capture_output=True
        ).stdout.splitlines()
        if any(path.startswith("target/") for path in tracked_files):
            errors.append("generated target artifacts must not be tracked")

    gitignore = (ROOT / ".gitignore").read_text(encoding="utf-8") if (ROOT / ".gitignore").exists() else ""
    if not any(line.strip().rstrip("/") == "target" for line in gitignore.splitlines()):
        errors.append(".gitignore must exclude target")
    return errors


def counter(element: ET.Element, counter_type: str) -> Counter | None:
    for item in element.findall("counter"):
        if item.get("type") == counter_type:
            try:
                return Counter(missed=int(item.get("missed", "")), covered=int(item.get("covered", "")))
            except ValueError:
                return None
    return None


def is_critical_package(name: str) -> bool:
    return any(name == prefix or name.startswith(prefix + "/") for prefix in CRITICAL_PREFIXES)


def aggregate_package_counter(packages: list[ET.Element], counter_type: str) -> Counter:
    missed = 0
    covered = 0
    for package in packages:
        current = counter(package, counter_type)
        if current is None:
            continue
        missed += current.missed
        covered += current.covered
    return Counter(missed=missed, covered=covered)


def validate_report_tree(root: ET.Element) -> tuple[list[str], dict[str, Counter]]:
    errors: list[str] = []
    metrics: dict[str, Counter] = {}
    if root.tag != "report":
        return ["JaCoCo XML root element must be report"], metrics

    for counter_type, minimum in GLOBAL_THRESHOLDS.items():
        current = counter(root, counter_type)
        if current is None or current.total == 0:
            errors.append(f"global {counter_type} counter is missing or empty")
            continue
        metrics[f"GLOBAL_{counter_type}"] = current
        if current.ratio < minimum:
            errors.append(
                f"global {counter_type} coverage {current.ratio:.4%} is below {minimum:.0%}"
            )

    all_packages = root.findall("package")
    critical_packages = [package for package in all_packages if is_critical_package(package.get("name", ""))]
    if not critical_packages:
        errors.append("JaCoCo report contains no critical packages")
    else:
        present_names = {package.get("name", "") for package in critical_packages}
        for prefix in CRITICAL_PREFIXES:
            if not any(name == prefix or name.startswith(prefix + "/") for name in present_names):
                errors.append(f"JaCoCo report is missing critical package group {prefix}")

        for counter_type, minimum in CRITICAL_THRESHOLDS.items():
            current = aggregate_package_counter(critical_packages, counter_type)
            if current.total == 0:
                errors.append(f"critical {counter_type} counter is empty")
                continue
            metrics[f"CRITICAL_{counter_type}"] = current
            if current.ratio < minimum:
                errors.append(
                    f"critical {counter_type} coverage {current.ratio:.4%} is below {minimum:.0%}"
                )

    class_names = {
        clazz.get("name", "")
        for package in all_packages
        for clazz in package.findall("class")
    }
    required_classes = {
        "coupon persistence": "pl/radoslawpiatek/couponservice/coupon/adapters/persistence/JdbcCouponRedemptionRepository",
        "transactional redemption": "pl/radoslawpiatek/couponservice/coupon/application/TransactionalCouponRedemptionService",
        "geolocation adapter": "pl/radoslawpiatek/couponservice/geolocation/adapters/ServletClientIpResolver",
    }
    for label, class_name in required_classes.items():
        if class_name not in class_names:
            errors.append(f"JaCoCo report is missing {label} evidence class {class_name}")

    return errors, metrics


def validate_report(path: Path) -> tuple[list[str], dict[str, Counter]]:
    if not path.is_file() or path.stat().st_size == 0:
        return [f"JaCoCo XML report is missing or empty: {path}"], {}
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError as exception:
        return [f"JaCoCo XML report is malformed: {exception}"], {}
    return validate_report_tree(root)


def print_metrics(metrics: dict[str, Counter]) -> None:
    for scope in ("GLOBAL", "CRITICAL"):
        for counter_type in ("LINE", "BRANCH"):
            value = metrics.get(f"{scope}_{counter_type}")
            if value is not None:
                print(
                    f"{scope} {counter_type}: {value.covered}/{value.total} "
                    f"({value.ratio:.2%})"
                )


def write_tree(root: ET.Element, path: Path) -> None:
    ET.ElementTree(root).write(path, encoding="utf-8", xml_declaration=True)


def run_negative_self_test(report: Path) -> list[str]:
    """Mutate temporary report copies and require report validation to fail closed."""
    failures: list[str] = []
    original = ET.parse(report).getroot()

    def expect_failure(name: str, mutator) -> None:
        root = copy.deepcopy(original)
        mutator(root)
        with tempfile.NamedTemporaryFile(prefix="emp008-jacoco-", suffix=".xml", delete=False) as handle:
            path = Path(handle.name)
        try:
            write_tree(root, path)
            errors, _ = validate_report(path)
            if not errors:
                failures.append(f"negative self-test did not fail closed: {name}")
        finally:
            path.unlink(missing_ok=True)

    def set_root_counter(counter_type: str, missed: int, covered: int) -> None:
        def mutate(root: ET.Element) -> None:
            item = next(node for node in root.findall("counter") if node.get("type") == counter_type)
            item.set("missed", str(missed))
            item.set("covered", str(covered))
        expect_failure(f"global {counter_type} below threshold", mutate)

    set_root_counter("LINE", 21, 79)
    set_root_counter("BRANCH", 31, 69)

    for counter_type, missed, covered in (("LINE", 26, 74), ("BRANCH", 36, 64)):
        def mutate(root: ET.Element, ct=counter_type, m=missed, c=covered) -> None:
            critical = [package for package in root.findall("package") if is_critical_package(package.get("name", ""))]
            for package in critical:
                item = counter(package, ct)
                node = next((node for node in package.findall("counter") if node.get("type") == ct), None)
                if node is not None:
                    node.set("missed", "0")
                    node.set("covered", "0")
            first = critical[0]
            node = next(node for node in first.findall("counter") if node.get("type") == ct)
            node.set("missed", str(m))
            node.set("covered", str(c))
        expect_failure(f"critical {counter_type} below threshold", mutate)

    def remove_critical(root: ET.Element) -> None:
        for package in list(root.findall("package")):
            if is_critical_package(package.get("name", "")):
                root.remove(package)
    expect_failure("missing critical packages", remove_critical)

    with tempfile.NamedTemporaryFile(prefix="emp008-jacoco-malformed-", suffix=".xml", delete=False) as handle:
        malformed = Path(handle.name)
        handle.write(b"<report>")
    try:
        errors, _ = validate_report(malformed)
        if not errors:
            failures.append("negative self-test did not reject malformed XML")
    finally:
        malformed.unlink(missing_ok=True)

    return failures


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--report", type=Path, help="validate a generated JaCoCo XML report")
    parser.add_argument(
        "--self-test",
        action="store_true",
        help="with --report, verify fail-closed behavior on controlled temporary mutations",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    errors = static_contract_errors()
    if args.self_test and args.report is None:
        errors.append("--self-test requires --report")

    metrics: dict[str, Counter] = {}
    if args.report is not None:
        report_errors, metrics = validate_report(args.report)
        errors.extend(report_errors)
        if not report_errors and args.self_test:
            errors.extend(run_negative_self_test(args.report))

    if errors:
        print("\n".join(f"ERROR: {error}" for error in errors), file=sys.stderr)
        return 1

    if args.report is None:
        print("SUCCESS: EMP-008 JaCoCo static implementation contract valid")
    else:
        print_metrics(metrics)
        print("SUCCESS: EMP-008 measured JaCoCo report satisfies global and critical gates")
        if args.self_test:
            print("SUCCESS: EMP-008 report checker negative self-tests fail closed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
