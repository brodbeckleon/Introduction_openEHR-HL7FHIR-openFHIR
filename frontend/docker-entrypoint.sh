#!/bin/sh
# Keeps the container's node_modules in step with package-lock.json.
#
# /app/node_modules is a named volume, so it survives image rebuilds and is not the directory an
# `npm install` on the host writes to — the volume is mounted over it. Without this, adding a
# dependency works on the host and leaves the container with "Failed to resolve import", which
# looks like a code error and is not one.
#
# The lockfile's checksum is stamped into the volume, so a start with nothing to do costs one
# md5sum rather than a full install.
set -e

STAMP=/app/node_modules/.lock-stamp
CURRENT=$(md5sum /app/package-lock.json | cut -d' ' -f1)

if [ ! -f "$STAMP" ] || [ "$(cat "$STAMP")" != "$CURRENT" ]; then
    echo "package-lock.json changed (or node_modules is empty) — installing"
    npm install
    echo "$CURRENT" > "$STAMP"
else
    echo "node_modules is in step with package-lock.json"
fi

exec "$@"
