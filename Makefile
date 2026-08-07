DOCKER ?= /Applications/Docker.app/Contents/Resources/bin/docker
MAVEN ?= ./mvnw
SOURCE_EXPORT_DIR ?= $(HOME)/Downloads
APP_PORT ?= 8080
COMPOSE_PROJECT_NAME ?= coupon-service

.PHONY: help docs-check bootstrap-check emp003-check emp004-refinement-check emp004-check emp006-refinement-check emp006-check emp007-check emp008-refinement-check emp008-check emp008-report-check emp009-refinement-check emp009-check emp010-refinement-check java-check docker-check compose-config docker-build docker-up docker-down docker-logs docker-smoke maven-verify verify checksums package export-source clean

help:
	@printf '%s\n' \
		'make docs-check      - validate documentation governance' \
		'make bootstrap-check - validate the EMP-002 source contract without network access' \
		'make emp003-check   - validate the EMP-003 create-coupon source contract' \
		'make emp004-refinement-check - validate the EMP-004 redemption refinement contract' \
		'make emp004-check   - validate the EMP-004 redemption implementation contract' \
		'make emp006-refinement-check - validate the EMP-006 refinement contract' \
		'make emp006-check   - validate the EMP-006 implementation contract' \
		'make emp007-check   - validate OpenAPI, Swagger UI and Javadoc contracts' \
		'make emp008-refinement-check - validate the EMP-008 coverage refinement' \
		'make emp008-check   - validate the EMP-008 JaCoCo implementation contract' \
		'make emp008-report-check - validate measured JaCoCo coverage and checker fail-closed behavior' \
		'make emp009-refinement-check - validate the EMP-009 concurrency-evidence refinement' \
		'make emp009-check   - validate the EMP-009 concurrency evidence implementation' \
		'make emp010-refinement-check - validate EMP-010 CI/delivery/observability refinement' \
		'make java-check      - require Java 21' \
		'make docker-check    - verify the configured Docker CLI and daemon' \
		'make compose-config  - validate docker-compose.yml' \
		'make docker-build    - build the application container image' \
		'make docker-up       - start PostgreSQL and the application' \
		'make docker-down     - stop containers and remove the local volume' \
		'make docker-logs     - follow application and PostgreSQL logs' \
		'make docker-smoke    - build, start, health-check and clean the stack' \
		'make maven-verify    - run the Maven clean verify gate with Testcontainers' \
		'make verify          - run all project gates' \
		'make package         - create a safe source ZIP in ./dist' \
		'make export-source   - create a safe source ZIP in SOURCE_EXPORT_DIR' \
		'make checksums       - regenerate CHECKSUMS.sha256' \
		'make clean           - remove generated local artifacts'

docs-check:
	python3 scripts/check_documentation.py

bootstrap-check:
	python3 scripts/check_bootstrap.py

emp003-check:
	python3 scripts/check_emp003.py

emp004-refinement-check:
	python3 scripts/check_emp004_refinement.py

emp004-check:
	python3 scripts/check_emp004.py

emp006-refinement-check:
	python3 scripts/check_emp006_refinement.py

emp006-check:
	python3 scripts/check_emp006.py

emp007-check:
	python3 scripts/check_emp007.py

emp008-refinement-check:
	python3 scripts/check_emp008_refinement.py

emp008-check:
	python3 scripts/check_emp008.py

emp008-report-check:
	python3 scripts/check_emp008.py --report target/site/jacoco/jacoco.xml --self-test

emp009-refinement-check:
	python3 scripts/check_emp009_refinement.py

emp009-check:
	python3 scripts/check_emp009.py

emp010-refinement-check:
	python3 scripts/check_emp010_refinement.py

java-check:
	@command -v java >/dev/null 2>&1 || { echo 'ERROR: Java is not available in PATH' >&2; exit 1; }
	@version="$$(java -version 2>&1 | awk -F '"' '/version/ { print $$2; exit }')"; \
	major="$${version%%.*}"; \
	if [ "$$major" != '21' ]; then \
		echo "ERROR: Java 21 is required, found: $$version" >&2; \
		exit 1; \
	fi; \
	echo "SUCCESS: Java $$version"

docker-check:
	@docker_bin='$(DOCKER)'; \
	case "$$docker_bin" in \
		*/*) test -x "$$docker_bin" || { \
			echo "ERROR: Docker CLI is not executable: $$docker_bin" >&2; \
			echo 'Override it with: make docker-check DOCKER=/path/to/docker' >&2; \
			exit 1; \
		} ;; \
		*) docker_bin="$$(command -v "$$docker_bin" 2>/dev/null || true)"; \
			test -n "$$docker_bin" || { echo 'ERROR: Docker CLI was not found' >&2; exit 1; } ;; \
	esac; \
	"$$docker_bin" version

compose-config: docker-check
	"$(DOCKER)" compose -p "$(COMPOSE_PROJECT_NAME)" -f docker-compose.yml config --quiet

docker-build: compose-config
	APP_PORT="$(APP_PORT)" "$(DOCKER)" compose -p "$(COMPOSE_PROJECT_NAME)" -f docker-compose.yml build app

docker-up: compose-config
	APP_PORT="$(APP_PORT)" "$(DOCKER)" compose -p "$(COMPOSE_PROJECT_NAME)" -f docker-compose.yml up -d --wait --wait-timeout 180
	@printf 'Application: http://localhost:%s\n' '$(APP_PORT)'
	@printf 'Health:      http://localhost:%s/actuator/health\n' '$(APP_PORT)'
	@printf 'Swagger UI:  http://localhost:%s/swagger-ui\n' '$(APP_PORT)'
	@printf 'OpenAPI:     http://localhost:%s/openapi.yaml\n' '$(APP_PORT)'

docker-down: docker-check
	APP_PORT="$(APP_PORT)" "$(DOCKER)" compose -p "$(COMPOSE_PROJECT_NAME)" -f docker-compose.yml down --volumes --remove-orphans

docker-logs: docker-check
	APP_PORT="$(APP_PORT)" "$(DOCKER)" compose -p "$(COMPOSE_PROJECT_NAME)" -f docker-compose.yml logs --follow --tail=200

docker-smoke: docker-check
	DOCKER="$(DOCKER)" APP_PORT="$(APP_PORT)" COMPOSE_PROJECT_NAME="$(COMPOSE_PROJECT_NAME)" bash scripts/docker_smoke.sh

maven-verify: java-check docker-check
	"$(MAVEN)" -B -ntp clean verify

verify:
	DOCKER="$(DOCKER)" MAVEN="$(MAVEN)" bash verify.sh

checksums:
	python3 scripts/generate_checksums.py

package:
	SOURCE_EXPORT_DIR="$(CURDIR)/dist" bash scripts/package_source.sh

export-source:
	SOURCE_EXPORT_DIR="$(SOURCE_EXPORT_DIR)" bash scripts/package_source.sh

clean:
	rm -rf build dist target coverage
	find . -type d -name __pycache__ -prune -exec rm -rf {} +
