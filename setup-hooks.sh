#!/bin/bash

# Git Hooks Setup Script
# This script configures the repository to use custom git hooks

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  Git Hooks Setup for pixel-watch-tagger${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Check if we're in a git repository
if [ ! -d ".git" ]; then
    echo -e "${RED}✗ Error: Not in a git repository${NC}"
    echo "Please run this script from the root of the repository"
    exit 1
fi

# Check if .githooks directory exists
if [ ! -d ".githooks" ]; then
    echo -e "${RED}✗ Error: .githooks directory not found${NC}"
    exit 1
fi

# Configure git to use .githooks directory
echo -e "${BLUE}1. Configuring git hooks path...${NC}"
git config core.hooksPath .githooks
echo -e "${GREEN}   ✓ Git hooks path set to .githooks${NC}"
echo ""

# Make hooks executable
echo -e "${BLUE}2. Making hooks executable...${NC}"
chmod +x .githooks/*
echo -e "${GREEN}   ✓ Hooks are now executable${NC}"
echo ""

# Check for Claude Code installation
echo -e "${BLUE}3. Checking for Claude Code...${NC}"
if command -v claude &> /dev/null; then
    CLAUDE_VERSION=$(claude --version 2>/dev/null || echo "unknown")
    echo -e "${GREEN}   ✓ Claude Code found: $CLAUDE_VERSION${NC}"

    # Ask if user wants to enable auto-update
    echo ""
    read -p "   Enable automatic documentation updates with Claude Code? (y/n): " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        git config --local hooks.enableClaude true
        echo -e "${GREEN}   ✓ Claude Code auto-update enabled${NC}"
        echo -e "${YELLOW}   Note: You can disable it later with: git config --local hooks.enableClaude false${NC}"
    else
        git config --local hooks.enableClaude false
        echo -e "${YELLOW}   ℹ Claude Code auto-update disabled${NC}"
        echo -e "${YELLOW}   Enable it later with: git config --local hooks.enableClaude true${NC}"
    fi
else
    echo -e "${YELLOW}   ⚠️  Claude Code not found in PATH${NC}"
    echo -e "${YELLOW}   Hooks will work in validation-only mode${NC}"
    echo -e "${YELLOW}   Install Claude Code from: https://claude.ai/code${NC}"
    git config --local hooks.enableClaude false
fi
echo ""

# Verify setup
echo -e "${BLUE}4. Verifying setup...${NC}"
HOOKS_PATH=$(git config --get core.hooksPath)
CLAUDE_ENABLED=$(git config --get hooks.enableClaude)

echo -e "   Hooks path: ${GREEN}$HOOKS_PATH${NC}"
echo -e "   Claude auto-update: ${GREEN}$CLAUDE_ENABLED${NC}"
echo ""

# Success message
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  ✓ Git hooks setup complete!${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BLUE}What happens now:${NC}"
echo "  • Every commit will check if CHANGELOG.md needs updating"
echo "  • Structural changes will also require CLAUDE.md updates"
if [ "$CLAUDE_ENABLED" = "true" ]; then
    echo "  • Claude Code will automatically update documentation when needed"
else
    echo "  • You'll need to manually update documentation before committing"
fi
echo ""
echo -e "${BLUE}Useful commands:${NC}"
echo "  • Enable Claude auto-update:  git config --local hooks.enableClaude true"
echo "  • Disable Claude auto-update: git config --local hooks.enableClaude false"
echo "  • Bypass hook (emergency):    git commit --no-verify"
echo ""
