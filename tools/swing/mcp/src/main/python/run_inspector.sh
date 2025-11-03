#!/bin/bash
echo "Starting MCP Inspector..."
echo "Make sure your Codion app is running with MCP enabled"
echo ""

# Change to the python directory
cd "$(dirname "$0")"

# Run the inspector
echo "Running: npx @modelcontextprotocol/inspector test_inspector.json"
exec npx @modelcontextprotocol/inspector test_inspector.json