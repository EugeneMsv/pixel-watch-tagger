#!/bin/bash

# Coverage verification script - ensures changed source code has 60% test coverage

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

MINIMUM_COVERAGE=60
PROJECT_DIR=$(git rev-parse --show-toplevel)
COVERAGE_REPORT="$PROJECT_DIR/app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"

print_success() { echo -e "${GREEN}✓ $1${NC}"; }
print_error() { echo -e "${RED}✗ $1${NC}"; }
print_info() { echo -e "${BLUE}$1${NC}"; }

get_changed_source_files() {
    git diff --cached --name-only --diff-filter=ACM | \
        grep -E '^app/src/main/.*\.kt$' | \
        grep -v 'Test\.kt$' || true
}

main() {
    print_info "🔍 Checking test coverage for changed files..."

    local changed_files=$(get_changed_source_files)

    if [[ -z "$changed_files" ]]; then
        print_success "No source code changes detected"
        exit 0
    fi

    print_info "Changed files: $(echo "$changed_files" | wc -l)"

    # Run tests and generate coverage
    cd "$PROJECT_DIR"
    print_info "📊 Running: ./gradlew testDebugUnitTest jacocoTestReport"

    if ! ./gradlew testDebugUnitTest jacocoTestReport --quiet; then
        print_error "Tests failed - fix failing tests before committing"
        exit 1
    fi

    if [[ ! -f "$COVERAGE_REPORT" ]]; then
        print_error "Coverage report not generated"
        exit 1
    fi

    # Extract overall coverage percentage from XML report
    local coverage_line=$(grep -o 'type="INSTRUCTION".*covered="[0-9]*".*missed="[0-9]*"' "$COVERAGE_REPORT" | head -1)
    local covered=$(echo "$coverage_line" | sed -n 's/.*covered="\([0-9]*\)".*/\1/p')
    local missed=$(echo "$coverage_line" | sed -n 's/.*missed="\([0-9]*\)".*/\1/p')

    if [[ -z "$covered" ]] || [[ -z "$missed" ]]; then
        print_error "Could not parse coverage report"
        exit 1
    fi

    local total=$((covered + missed))
    local coverage_percent=$((covered * 100 / total))

    print_info "Coverage: ${coverage_percent}% (minimum: ${MINIMUM_COVERAGE}%)"

    if [[ $coverage_percent -lt $MINIMUM_COVERAGE ]]; then
        echo ""
        print_error "COMMIT BLOCKED: Coverage ${coverage_percent}% is below minimum ${MINIMUM_COVERAGE}%"
        echo ""
        echo -e "${BLUE}Add more tests to improve coverage${NC}"
        echo -e "${BLUE}View report: app/build/reports/jacoco/jacocoTestReport/html/index.html${NC}"
        echo -e "${BLUE}Bypass: git commit --no-verify${NC}"
        echo ""
        exit 1
    fi

    print_success "Coverage requirement met: ${coverage_percent}%"
    exit 0
}

main
