#!/bin/bash

# Coverage verification script for changed source files
# Ensures that any changed source code has unit tests with at least 60% coverage

set -e

#──────────────────────────────────────────────────────────────────────────────
# CONFIGURATION
#──────────────────────────────────────────────────────────────────────────────

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

MINIMUM_COVERAGE=60
PROJECT_DIR=$(git rev-parse --show-toplevel)
COVERAGE_REPORT="$PROJECT_DIR/app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"

#──────────────────────────────────────────────────────────────────────────────
# UTILITY FUNCTIONS
#──────────────────────────────────────────────────────────────────────────────

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${BLUE}$1${NC}"
}

#──────────────────────────────────────────────────────────────────────────────
# CORE LOGIC
#──────────────────────────────────────────────────────────────────────────────

get_changed_source_files() {
    # Get staged Kotlin source files (exclude test files)
    git diff --cached --name-only --diff-filter=ACM | \
        grep -E '^app/src/main/.*\.kt$' | \
        grep -v 'Test\.kt$' || true
}

has_test_file() {
    local source_file=$1
    # Convert source path to test path
    # app/src/main/java/com/example/Foo.kt -> app/src/test/java/com/example/FooTest.kt
    local test_file=$(echo "$source_file" | sed 's/src\/main/src\/test/' | sed 's/\.kt$/Test.kt/')

    if [[ -f "$PROJECT_DIR/$test_file" ]]; then
        return 0
    fi

    # Also check if there's a test file staged
    if git diff --cached --name-only | grep -q "^$test_file$"; then
        return 0
    fi

    return 1
}

run_tests_and_coverage() {
    print_info "📊 Running tests and generating coverage report..."

    cd "$PROJECT_DIR"

    # Run tests with coverage
    if ! ./gradlew testDebugUnitTest jacocoTestReport --quiet 2>&1 | grep -E "(FAILED|ERROR)" > /dev/null; then
        if [[ ! -f "$COVERAGE_REPORT" ]]; then
            print_warning "Coverage report not generated. This may mean no tests were run."
            return 1
        fi
        return 0
    else
        print_error "Tests failed. Please fix failing tests before committing."
        ./gradlew testDebugUnitTest
        return 1
    fi
}

extract_class_coverage() {
    local source_file=$1
    local class_name=$(basename "$source_file" .kt)

    if [[ ! -f "$COVERAGE_REPORT" ]]; then
        echo "0"
        return
    fi

    # Extract coverage for the specific class from XML report
    # This is a simplified version - in production you might want to use xmllint or python
    local coverage=$(grep -A 5 "name=\"$class_name\"" "$COVERAGE_REPORT" 2>/dev/null | \
        grep "INSTRUCTION" | \
        sed -n 's/.*covered="\([0-9]*\)".*missed="\([0-9]*\)".*/\1 \2/p' | \
        head -1)

    if [[ -z "$coverage" ]]; then
        echo "0"
        return
    fi

    local covered=$(echo "$coverage" | awk '{print $1}')
    local missed=$(echo "$coverage" | awk '{print $2}')
    local total=$((covered + missed))

    if [[ $total -eq 0 ]]; then
        echo "0"
        return
    fi

    local percentage=$((covered * 100 / total))
    echo "$percentage"
}

print_blocked_message() {
    local missing_tests=("$@")

    echo ""
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${RED}✗ COMMIT BLOCKED: Unit tests required${NC}"
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo -e "${YELLOW}📝 The following source files require unit tests:${NC}"
    echo ""

    for file in "${missing_tests[@]}"; do
        echo "  • $file"
    done

    echo ""
    echo -e "${BLUE}Requirements:${NC}"
    echo "  • Each source file must have corresponding unit tests"
    echo "  • Minimum code coverage: ${MINIMUM_COVERAGE}%"
    echo ""
    echo -e "${BLUE}Test file naming convention:${NC}"
    echo "  • Source: app/src/main/java/com/example/Foo.kt"
    echo "  • Test:   app/src/test/java/com/example/FooTest.kt"
    echo ""
    echo -e "${BLUE}Emergency bypass (use sparingly):${NC}"
    echo "  ${GREEN}git commit --no-verify${NC}"
    echo ""
}

print_low_coverage_message() {
    local low_coverage_files=("$@")

    echo ""
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${RED}✗ COMMIT BLOCKED: Insufficient test coverage${NC}"
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo -e "${YELLOW}📊 The following files have less than ${MINIMUM_COVERAGE}% coverage:${NC}"
    echo ""

    for entry in "${low_coverage_files[@]}"; do
        echo "  $entry"
    done

    echo ""
    echo -e "${BLUE}Requirements:${NC}"
    echo "  • Minimum code coverage: ${MINIMUM_COVERAGE}%"
    echo "  • Add more test cases to improve coverage"
    echo ""
    echo -e "${BLUE}View detailed coverage report:${NC}"
    echo "  ${GREEN}open app/build/reports/jacoco/jacocoTestReport/html/index.html${NC}"
    echo ""
    echo -e "${BLUE}Emergency bypass (use sparingly):${NC}"
    echo "  ${GREEN}git commit --no-verify${NC}"
    echo ""
}

#──────────────────────────────────────────────────────────────────────────────
# MAIN
#──────────────────────────────────────────────────────────────────────────────

main() {
    print_info "🔍 Checking test coverage requirements..."

    local changed_files=$(get_changed_source_files)

    if [[ -z "$changed_files" ]]; then
        print_success "No source code changes detected, skipping coverage check"
        exit 0
    fi

    print_info "Found changed source files:"
    echo "$changed_files" | while read -r file; do
        echo "  • $file"
    done
    echo ""

    # Check for missing test files
    local missing_tests=()
    while IFS= read -r file; do
        if ! has_test_file "$file"; then
            missing_tests+=("$file")
        fi
    done <<< "$changed_files"

    if [[ ${#missing_tests[@]} -gt 0 ]]; then
        print_blocked_message "${missing_tests[@]}"
        exit 1
    fi

    print_success "All changed files have corresponding test files"

    # Run tests and generate coverage
    if ! run_tests_and_coverage; then
        exit 1
    fi

    print_success "Tests passed, checking coverage..."

    # Check coverage for each changed file
    local low_coverage_files=()
    while IFS= read -r file; do
        local coverage=$(extract_class_coverage "$file")
        local class_name=$(basename "$file" .kt)

        if [[ $coverage -lt $MINIMUM_COVERAGE ]]; then
            low_coverage_files+=("• $class_name: ${coverage}% (minimum: ${MINIMUM_COVERAGE}%)")
        else
            print_success "$class_name: ${coverage}% coverage"
        fi
    done <<< "$changed_files"

    if [[ ${#low_coverage_files[@]} -gt 0 ]]; then
        print_low_coverage_message "${low_coverage_files[@]}"
        exit 1
    fi

    print_success "All changed files meet coverage requirements"
    exit 0
}

main
