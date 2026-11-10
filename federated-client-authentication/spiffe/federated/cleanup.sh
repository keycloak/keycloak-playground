#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")"
# Stop the foreground processes before deleting their local data.
rm -rf .runtime
