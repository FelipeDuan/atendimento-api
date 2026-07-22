GRADLE := ./gradlew
TEST ?= com.felipeduan.atendimento

.PHONY: help test test-fresh test-fresh-log test-class check boot run build clean format format-check compile

help:
	@echo "Targets:"
	@echo "  make test              roda a suíte completa"
	@echo "  make test-fresh        limpa e roda a suíte completa"
	@echo "  make test-fresh-log    test-fresh com SQL em logs/test-sql.log"
	@echo "  make test-class TEST=… roda uma classe ou método (@Test)"
	@echo "  make boot              sobe a API (profile dev)"
	@echo "  make build             compila e roda testes"
	@echo "  make compile           compila sem testes"
	@echo "  make format            aplica spotless"
	@echo "  make format-check      verifica spotless"
	@echo "  make check             spotless + checkstyle + testes (mesmo gate do CI)"
	@echo "  make clean             limpa build"

test:
	$(GRADLE) test

test-fresh:
	$(GRADLE) clean test --rerun-tasks --no-build-cache

test-fresh-log:
	@mkdir -p logs
	$(GRADLE) clean test --rerun-tasks --no-build-cache 2>&1 | tee logs/test-sql.log

test-class:
	$(GRADLE) --rerun-tasks test --tests "$(TEST)"

boot:
	$(GRADLE) bootRun

run: boot

build:
	$(GRADLE) build

compile:
	$(GRADLE) classes testClasses

format:
	$(GRADLE) spotlessApply

format-check:
	$(GRADLE) spotlessCheck

check:
	$(GRADLE) check

clean:
	$(GRADLE) clean
