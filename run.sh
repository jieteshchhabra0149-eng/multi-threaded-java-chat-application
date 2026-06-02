#!/bin/bash
# NexusChat — Quick Start Script
# Requires: Java 21+, Maven 3.8+

set -e

echo ""
echo "  ╔═══════════════════════════════════════╗"
echo "  ║        NexusChat — Quick Start        ║"
echo "  ║  Multithreaded Java Chat Application  ║"
echo "  ╚═══════════════════════════════════════╝"
echo ""

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -1 | awk -F '"' '{print $2}' | cut -d'.' -f1)
echo "  ✓ Java version: $JAVA_VERSION"

if [ "$JAVA_VERSION" -lt 21 ] 2>/dev/null; then
    echo "  ✗ Java 21+ required for Virtual Threads"
    echo "    Download: https://adoptium.net"
    exit 1
fi

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo "  ✗ Maven not found. Install: https://maven.apache.org"
    exit 1
fi

echo "  ✓ Maven: $(mvn -version 2>&1 | head -1)"
echo ""
echo "  Building application..."
echo ""

mvn clean package -q -DskipTests

echo ""
echo "  ✓ Build successful!"
echo ""
echo "  Starting NexusChat on http://localhost:8080"
echo ""
echo "  Features:"
echo "    • Real-time WebSocket messaging (STOMP)"
echo "    • Java 21 Virtual Threads for high concurrency"
echo "    • Multiple chat rooms"
echo "    • Typing indicators & presence"
echo "    • Thread-safe message broadcasting"
echo ""
echo "  Press Ctrl+C to stop"
echo ""

java -jar target/multithreaded-chat-1.0.0.jar
