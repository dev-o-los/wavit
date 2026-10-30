#!/usr/bin/env bash
# ==============================================================================
# Wavit Backend API Verification Script
# Tests all Phase 1 endpoints against the running Ktor server (http://localhost:8080)
# ==============================================================================

BASE_URL="http://localhost:8080"
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

test_endpoint() {
    local name="$1"
    local endpoint="$2"
    echo -n "Testing $name ($endpoint)... "
    response=$(curl -s -w "\n%{http_code}" "$BASE_URL$endpoint")
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        echo -e "${GREEN}PASSED ($http_code)${NC}"
        # Print a snippet of the json response
        echo "$body" | head -n 5
        echo "..."
    else
        echo -e "${RED}FAILED ($http_code)${NC}"
        echo "$body" | head -n 5
    fi
    echo "--------------------------------------------------------"
}

echo "========================================================"
echo "Starting Wavit Backend API Endpoint Tests"
echo "========================================================"

# 1. Health check
test_endpoint "Server Health & Endpoints List" "/"

# 2. Search suggestions
test_endpoint "Search Suggestions" "/search/suggestions?q=blinding"

# 3. Search query
test_endpoint "Search Songs" "/search?q=blinding+lights&filter=songs"

# 4. Home feed
test_endpoint "Home Recommended Content" "/home"

# 5. Explore moods and genres
test_endpoint "Explore Moods and Genres" "/explore"

# 6. Song details
test_endpoint "Song Metadata" "/song/4NRXx6U8ABQ"

# 7. Song extended info
test_endpoint "Song Extended Info (views/likes)" "/song/4NRXx6U8ABQ/info"

# 8. Related songs
test_endpoint "Related Songs / Radio Mix" "/song/4NRXx6U8ABQ/related"

# 9. Lyrics
test_endpoint "Song Lyrics Transcript" "/song/4NRXx6U8ABQ/lyrics"

# 10. Stream resolution
test_endpoint "Audio Stream URL Resolution" "/stream/4NRXx6U8ABQ"

# 11. Queue snapshot
test_endpoint "Playback Queue Snapshot" "/queue"

echo "API testing completed!"

