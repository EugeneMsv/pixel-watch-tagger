#!/bin/bash

# Pre-commit hook for mandatory CHANGELOG.md updates
# Delegates documentation updates to the changelog-tracker skill

set -e

#──────────────────────────────────────────────────────────────────────────────
# CONFIGURATION
#──────────────────────────────────────────────────────────────────────────────

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

ENABLE_CLAUDE=$(git config --get hooks.enableClaude || echo "false")
CLAUDE_TIMEOUT=120

# Files that don't require changelog updates
EXCLUDED_PATTERNS=('.idea/' '*.iml' '.gitignore' '.gitattributes' '*.md' 'CHANGELOG.md')

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

run_spotless_apply() {
    print_info "🎨 Running spotless code formatter..."

    # Check if gradlew exists
    if [ ! -f "./gradlew" ]; then
        print_warning "Gradle wrapper not found, skipping spotless"
        return 0
    fi

    # Run spotlessApply
    if ./gradlew spotlessApply --quiet 2>&1; then
        # Check if there are any unstaged changes after spotlessApply
        if [ -n "$(git diff --name-only)" ]; then
            print_info "Spotless made formatting changes, staging them..."
            # Stage all modified files that were already staged or modified
            git diff --name-only | while IFS= read -r file; do
                if [ -f "$file" ]; then
                    git add "$file"
                fi
            done
            print_success "Formatted files staged automatically"
        else
            print_success "Code already formatted correctly"
        fi
        return 0
    else
        print_error "Spotless formatting failed"
        return 1
    fi
}

is_excluded() {
    local file=$1
    for pattern in "${EXCLUDED_PATTERNS[@]}"; do
        if [[ "$file" == $pattern ]] || [[ "$file" == *"$pattern"* ]]; then
            return 0
        fi
    done
    return 1
}

has_substantive_changes() {
    local staged_files=$(git diff --cached --name-only --diff-filter=ACM)

    while IFS= read -r file; do
        if ! is_excluded "$file"; then
            return 0
        fi
    done <<< "$staged_files"

    return 1
}

is_changelog_staged() {
    git diff --cached --name-only | grep -q "^CHANGELOG.md$"
}

invoke_changelog_tracker() {
    print_info "🤖 Invoking changelog-tracker skill..."

    if ! command -v claude &> /dev/null; then
        print_warning "Claude Code not found in PATH"
        return 1
    fi

    # Use headless mode with proper skill invocation and auto-approve edits
    local output
    if output=$(timeout ${CLAUDE_TIMEOUT}s claude -p "/changelog-tracker" --permission-mode acceptEdits 2>&1); then
        # Check if CHANGELOG.md was actually modified
        if git diff --name-only | grep -q "^CHANGELOG.md$" || git diff --cached --name-only | grep -q "^CHANGELOG.md$"; then
            # Stage CHANGELOG.md if it was modified but not staged
            if ! is_changelog_staged; then
                git add CHANGELOG.md
                print_success "CHANGELOG.md updated and staged automatically"
            else
                print_success "CHANGELOG.md already staged"
            fi
            return 0
        else
            print_warning "Skill completed but CHANGELOG.md was not updated"
            echo "$output"
            return 1
        fi
    else
        print_warning "Skill execution failed or timed out"
        echo "$output"
        return 1
    fi
}

print_blocked_message() {
    echo ""
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${RED}✗ COMMIT BLOCKED: CHANGELOG.md update required${NC}"
    echo -e "${RED}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo -e "${YELLOW}📝 Please update documentation for your changes${NC}"
    echo ""
    echo -e "${BLUE}Options:${NC}"
    echo "  1. Run the skill manually:"
    echo "     ${GREEN}claude -p \"/changelog-tracker\" --permission-mode acceptEdits${NC}"
    echo ""
    echo "  2. Enable automatic updates:"
    echo "     ${GREEN}git config --local hooks.enableClaude true${NC}"
    echo ""
    echo "  3. Emergency bypass (use sparingly):"
    echo "     ${GREEN}git commit --no-verify${NC}"
    echo ""
}

#──────────────────────────────────────────────────────────────────────────────
# MAIN
#──────────────────────────────────────────────────────────────────────────────

main() {
    # Run spotless formatting first
    if ! run_spotless_apply; then
        print_error "Spotless formatting failed, commit aborted"
        exit 1
    fi

    print_info "🔍 Checking documentation requirements..."

    # No substantive changes? Allow commit
    if ! has_substantive_changes; then
        print_success "No substantive changes detected, skipping documentation check"
        exit 0
    fi

    # CHANGELOG.md already staged? Allow commit
    if is_changelog_staged; then
        print_success "CHANGELOG.md is staged, proceeding with commit"
        exit 0
    fi

    # Try auto-update if enabled
    if [ "$ENABLE_CLAUDE" = "true" ]; then
        if invoke_changelog_tracker; then
            print_success "Documentation automatically updated and staged"
            exit 0
        fi
        print_warning "Auto-update failed, falling back to manual requirement"
    fi

    # Block commit and show instructions
    print_blocked_message
    exit 1
}

main