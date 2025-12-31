#!/bin/bash
# JSS Bahmni Docker Setup Script
# This script sets up Bahmni Docker on an EC2 instance with external RDS
# Usage: ./setup-bahmni.sh [prerelease|prod]

set -e

ENVIRONMENT=${1:-prerelease}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BAHMNI_DIR="/home/ubuntu/bahmni-docker"

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Install Loki Docker plugin for centralized logging
install_loki_plugin() {
    log_info "Installing Loki Docker logging plugin..."
    if docker plugin ls | grep -q loki; then
        log_info "Loki plugin already installed"
    else
        docker plugin install grafana/loki-docker-driver:latest --alias loki --grant-all-permissions || {
            log_warn "Failed to install Loki plugin. Using default logging."
            return 1
        }
        log_info "Loki plugin installed successfully"
    fi
}

# Clone Bahmni Docker repository
clone_bahmni_docker() {
    log_info "Cloning Bahmni Docker repository..."
    if [ -d "$BAHMNI_DIR" ]; then
        log_info "Bahmni Docker directory exists, pulling latest..."
        cd "$BAHMNI_DIR"
        git pull || true
    else
        git clone https://github.com/JanSwasthyaSahyog/bahmni-docker.git "$BAHMNI_DIR"
    fi
}

# Clone JSS Config repository
clone_jss_config() {
    log_info "Cloning JSS Config repository..."
    if [ -d "$BAHMNI_DIR/jss-config" ]; then
        log_info "JSS Config directory exists, pulling latest..."
        cd "$BAHMNI_DIR/jss-config"
        git pull || true
    else
        cd "$BAHMNI_DIR"
        git clone https://github.com/JanSwasthyaSahyog/jss-config.git
    fi
}

# Generate self-signed SSL certificates
generate_ssl_certs() {
    local CERT_DIR="$BAHMNI_DIR/certs"
    log_info "Generating SSL certificates..."
    mkdir -p "$CERT_DIR"
    
    if [ -f "$CERT_DIR/cert.pem" ] && [ -f "$CERT_DIR/key.pem" ]; then
        log_info "SSL certificates already exist"
        return 0
    fi
    
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout "$CERT_DIR/key.pem" \
        -out "$CERT_DIR/cert.pem" \
        -subj "/C=IN/ST=Chhattisgarh/L=Bilaspur/O=JSS/CN=jss-bahmni-${ENVIRONMENT}.avniproject.org"
    
    log_info "SSL certificates generated"
}

# Copy environment configuration
setup_environment() {
    log_info "Setting up environment configuration..."
    
    # Copy environment file
    if [ -f "$SCRIPT_DIR/.env.${ENVIRONMENT}" ]; then
        cp "$SCRIPT_DIR/.env.${ENVIRONMENT}" "$BAHMNI_DIR/.env"
        log_info "Copied .env.${ENVIRONMENT} to $BAHMNI_DIR/.env"
    else
        log_error "Environment file .env.${ENVIRONMENT} not found in $SCRIPT_DIR"
        exit 1
    fi
    
    # Copy docker-compose override
    if [ -f "$SCRIPT_DIR/docker-compose.override.yml" ]; then
        cp "$SCRIPT_DIR/docker-compose.override.yml" "$BAHMNI_DIR/docker-compose.override.yml"
        log_info "Copied docker-compose.override.yml"
    fi
}

# Fix docker-compose.yml for default logging (if Loki not available)
fix_logging_config() {
    log_info "Configuring logging..."
    cd "$BAHMNI_DIR"
    
    # Check if Loki plugin is installed
    if docker plugin ls 2>/dev/null | grep -q loki; then
        log_info "Loki plugin available, using Loki logging"
    else
        log_warn "Loki plugin not available, switching to default logging"
        # Change loki logging to default
        sed -i 's/<<: \*loki/<<: *default/g' docker-compose.yml
    fi
}

# Pull Docker images
pull_images() {
    log_info "Pulling Docker images (this may take a while)..."
    cd "$BAHMNI_DIR"
    docker compose --profile emr pull
}

# Start Bahmni containers
start_bahmni() {
    log_info "Starting Bahmni containers..."
    cd "$BAHMNI_DIR"
    docker compose --profile emr up -d
    
    log_info "Waiting for containers to start..."
    sleep 10
    docker compose ps
}

# Health check
health_check() {
    log_info "Performing health check..."
    local MAX_ATTEMPTS=30
    local ATTEMPT=1
    
    while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
        if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/openmrs/ | grep -q "200\|302"; then
            log_info "OpenMRS is responding!"
            return 0
        fi
        log_info "Waiting for OpenMRS to start (attempt $ATTEMPT/$MAX_ATTEMPTS)..."
        sleep 30
        ATTEMPT=$((ATTEMPT + 1))
    done
    
    log_warn "OpenMRS did not respond within expected time. Check logs with: docker logs bahmni-docker-openmrs-1"
    return 1
}

# Main setup function
main() {
    log_info "=========================================="
    log_info "  JSS Bahmni Docker Setup - ${ENVIRONMENT}"
    log_info "=========================================="
    
    # Install Loki plugin (optional, continue if fails)
    install_loki_plugin || true
    
    # Clone repositories
    clone_bahmni_docker
    clone_jss_config
    
    # Generate SSL certificates
    generate_ssl_certs
    
    # Setup environment
    setup_environment
    
    # Fix logging if needed
    fix_logging_config
    
    # Pull images
    pull_images
    
    # Start containers
    start_bahmni
    
    log_info "=========================================="
    log_info "  Setup Complete!"
    log_info "=========================================="
    log_info ""
    log_info "Bahmni is starting up. First startup with restored DB may take 5-10 minutes"
    log_info "due to Liquibase migrations checking the database schema."
    log_info ""
    log_info "Access URLs:"
    log_info "  - Bahmni EMR: https://jss-bahmni-${ENVIRONMENT}.avniproject.org/bahmni/home"
    log_info "  - OpenMRS:    https://jss-bahmni-${ENVIRONMENT}.avniproject.org/openmrs"
    log_info ""
    log_info "Commands:"
    log_info "  - View logs:    docker logs -f bahmni-docker-openmrs-1"
    log_info "  - Check status: docker compose ps"
    log_info "  - Stop:         docker compose --profile emr down"
    log_info "  - Restart:      docker compose --profile emr restart"
    log_info ""
    
    # Optional health check
    if [ "${2:-}" == "--wait" ]; then
        health_check
    fi
}

# Run main function
main "$@"
