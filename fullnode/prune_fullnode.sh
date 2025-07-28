#!/bin/bash

# Configuration
BASE_PATH="${BASE_PATH:-$(pwd)}"  # Use environment variable or current directory
COMPOSE_FILE="$BASE_PATH/compose.yaml"
TOOLKIT_JAR="$BASE_PATH/Toolkit.jar"
OUTPUT_DIR="$BASE_PATH/output-directory"
TMP_DIR="$BASE_PATH/tmp"
DATABASE_DIR="database"
SNAPSHOT_DIR="snapshot"


# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') $1"
}
log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Error handling
set -e
trap 'log_error "Script failed at line $LINENO. Exit code: $?"' ERR

# Display configuration
show_configuration() {
    log_info "Script Configuration:"
    log_info "BASE_PATH: $BASE_PATH"
    log_info "COMPOSE_FILE: $COMPOSE_FILE"
    log_info "TOOLKIT_JAR: $TOOLKIT_JAR"
    log_info "OUTPUT_DIR: $OUTPUT_DIR"
    log_info "TMP_DIR: $TMP_DIR"
    echo
}

# Cleanup function
cleanup() {
    log_info "Performing cleanup..."
    if [ -d "$TMP_DIR" ]; then
        rm -rf "$TMP_DIR"
        log_info "Temporary directory cleaned up"
    fi
}

# Set trap for cleanup on exit
trap cleanup EXIT

# Validation functions
check_prerequisites() {
    log_info "Checking prerequisites..."

    # Check if docker compose is available
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed or not in PATH"
        exit 1
    fi

    # Check if toolkit jar exists
    if [ ! -f "$TOOLKIT_JAR" ]; then
        log_error "Toolkit.jar not found in current directory"
        exit 1
    fi

    # Check if docker-compose.yml exists
    if [ ! -f "$COMPOSE_FILE" ]; then
        log_error "docker-compose.yml not found in current directory"
        exit 1
    fi

    # Check if Java is available
    if ! command -v java &> /dev/null; then
        log_error "Java is not installed or not in PATH"
        exit 1
    fi

    log_info "All prerequisites met"
}

# Main execution functions
stop_services() {
    log_info "Stopping Docker Compose services..."
    if docker compose -f "$COMPOSE_FILE" down; then
        log_info "Services stopped successfully"
    else
        log_error "Failed to stop services"
        exit 1
    fi
}

run_toolkit() {
    log_info "Running Toolkit.jar for database processing..."

    # Create temporary directory if it doesn't exist
    mkdir -p "$TMP_DIR"

    if java -jar "$TOOLKIT_JAR" db lite -o split -t snapshot \
        --fn-data-path "$OUTPUT_DIR/$DATABASE_DIR" \
        --dataset-path "$TMP_DIR"; then
        log_info "Toolkit execution completed successfully"
    else
        log_error "Toolkit execution failed"
        exit 1
    fi
}

reorganize_directories() {
    log_info "Reorganizing directories..."

    # Check if snapshot directory was created
    if [ ! -d "$TMP_DIR/$SNAPSHOT_DIR" ]; then
        log_error "Expected snapshot directory not found: $TMP_DIR/$SNAPSHOT_DIR"
        exit 1
    fi

    # Rename snapshot to database directory
    if mv "$TMP_DIR/$SNAPSHOT_DIR/" "$TMP_DIR/$DATABASE_DIR/"; then
        log_info "Moved snapshot contents to database directory"
#        rmdir "$TMP_DIR/$SNAPSHOT_DIR"
    else
        log_error "Failed to rename snapshot to database"
        exit 1
    fi

    # Remove old output directory if it exists
    if [ -d "$OUTPUT_DIR" ]; then
        log_info "Removing old output directory..."
        rm -rf "$OUTPUT_DIR"
    fi

    # Rename tmp to output directory
    if mv "$TMP_DIR" "$OUTPUT_DIR"; then
        log_info "Successfully renamed temporary directory to output directory"
    else
        log_error "Failed to rename temporary directory"
        exit 1
    fi
}

start_services() {
    log_info "Starting Docker Compose services..."
    if docker compose -f "$COMPOSE_FILE" up -d; then
        log_info "Services started successfully"

        # Wait a moment and check if services are running
        sleep 3
        if docker compose ps --filter "status=running" --quiet | grep -q .; then
            log_info "Services are running"
        else
            log_warn "Services may not be running properly. Check with 'docker compose ps'"
        fi
    else
        log_error "Failed to start services"
        exit 1
    fi
}

# Main execution
main() {
    log_info "Starting database processing and service restart..."

    show_configuration
    check_prerequisites
    stop_services
    run_toolkit
    reorganize_directories
    start_services

    log_info "Database processing and service restart completed successfully!"
}

# Run main function
main "$@"