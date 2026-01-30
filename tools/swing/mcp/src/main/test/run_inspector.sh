#!/bin/bash
echo "Starting MCP Inspector..."
echo "Make sure:"
echo "  1. You have built the bridge: ./gradlew :codion-tools-swing-mcp:installDist"
echo "  2. Your Codion app is running with MCP enabled"
echo ""

# Change to the test directory
cd "$(dirname "$0")"

# Run the inspector
echo "Running: npx @modelcontextprotocol/inspector test_inspector.json"
exec npx @modelcontextprotocol/inspector test_inspector.json