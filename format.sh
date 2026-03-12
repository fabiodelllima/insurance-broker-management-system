#!/usr/bin/env bash
# Format all Java sources with google-java-format.
# Usage: ./format.sh
set -euo pipefail
apps/api/mvnw -f apps/api/pom.xml com.spotify.fmt:fmt-maven-plugin:format "$@"
