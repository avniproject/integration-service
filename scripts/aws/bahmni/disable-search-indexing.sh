#!/bin/bash
# Script to disable Hibernate Search indexing in openmrs-runtime.properties
# This prevents deadlocks when indexing large restored databases

set -e

PROPS_FILE="/openmrs/data/openmrs-runtime.properties"
MAX_WAIT=300
ELAPSED=0

echo "Waiting for openmrs-runtime.properties to be created..."
while [ ! -f "$PROPS_FILE" ] && [ $ELAPSED -lt $MAX_WAIT ]; do
    sleep 2
    ELAPSED=$((ELAPSED + 2))
    echo "Waiting... ($ELAPSED/$MAX_WAIT seconds)"
done

if [ ! -f "$PROPS_FILE" ]; then
    echo "ERROR: openmrs-runtime.properties not found after $MAX_WAIT seconds"
    exit 1
fi

echo "Found openmrs-runtime.properties, disabling search indexing..."

# Add properties to disable Hibernate Search
cat >> "$PROPS_FILE" << 'EOF'

# Disable Hibernate Search indexing to prevent deadlocks with large restored DB
hibernate.search.autoregister_listeners=false
hibernate.search.enabled=false
EOF

echo "Search indexing disabled successfully"
cat "$PROPS_FILE" | grep -E "hibernate.search|connection.url"
