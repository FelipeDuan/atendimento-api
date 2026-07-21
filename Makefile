GRADLE := ./gradlew
TEST ?= com.felipeduan.atendimento

.PHONY: help test test-fresh test-class boot run build clean format format-check compile

help:
	@echo "Targets:"
	@echo "  make test              roda a suíte completa"
	@echo "  make test-fresh        limpa e roda a suíte completa"
	@echo "  make test-class TEST=… roda uma classe ou método (@Test)"
	@echo "  make boot              sobe a API (profile dev)"
	@echo "  make build             compila e roda testes"
	@echo "  make compile           compila sem testes"
	@echo "  make format            aplica spotless"
	@echo "  make format-check      verifica spotless"
	@echo "  make clean             limpa build"

test:
	$(GRADLE) test

test-fresh:
	$(GRADLE) clean test --rerun-tasks --no-build-cache

test-class:
	$(GRADLE) test --tests "$(TEST)" --rerun-tasks

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

clean:
	$(GRADLE) clean
