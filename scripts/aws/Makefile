# JSS Avni-Bahmni Integration - AWS Infrastructure Makefile
# Usage: make <target>

# Configuration
SSH_KEY := ~/.ssh/openchs-infra.pem
SSH_USER := ubuntu
AWS_REGION := ap-south-1

# Prerelease Environment
PRERELEASE_HOST := jss-bahmni-prerelease.avniproject.org
PRERELEASE_DB_HOST := jss-db-prerelease.avniproject.org
PRERELEASE_DB_USER := openmrs_admin
PRERELEASE_DB_PASS := OpenMRS_JSS2024
PRERELEASE_DB_NAME := openmrs
PRERELEASE_RDS_ID := jss-prerelease-mysql

# Backup file location (use quotes in commands, not escapes)
BACKUP_FILE := /Users/himeshr/integration-setup/JSS Avni Bahmni Integration/openmrsdb_backup.sql.gz
REMOTE_BACKUP_DIR := /home/ubuntu/backups

.PHONY: help status ssh-prerelease copy-backup restore-backup create-snapshot full-restore setup-prerelease cleanup-prerelease

help:
	@echo "JSS Avni-Bahmni AWS Infrastructure Commands"
	@echo ""
	@echo "=== Infrastructure Setup ==="
	@echo "  make setup-prerelease      - Setup complete prerelease environment"
	@echo "  make status                - Show all JSS infrastructure status"
	@echo "  make cleanup-prerelease    - Delete prerelease infrastructure"
	@echo ""
	@echo "=== SSH Access ==="
	@echo "  make ssh-prerelease        - SSH to prerelease EC2 instance"
	@echo ""
	@echo "=== Database Backup/Restore ==="
	@echo "  make copy-backup           - Copy backup file to prerelease EC2"
	@echo "  make restore-backup        - Restore OpenMRS backup to prerelease RDS"
	@echo "  make create-snapshot       - Create RDS snapshot after restore"
	@echo "  make full-restore          - Copy backup + restore + create snapshot"
	@echo ""
	@echo "=== Bahmni Docker Setup ==="
	@echo "  make bahmni-full-setup     - Full Bahmni setup using repo scripts"
	@echo "  make setup-bahmni          - Clone Bahmni Docker repo on EC2"
	@echo "  make bahmni-up             - Start Bahmni Docker containers"
	@echo "  make bahmni-down           - Stop Bahmni Docker containers"
	@echo "  make bahmni-restart        - Restart Bahmni containers"
	@echo "  make bahmni-status         - Show Bahmni container status"
	@echo "  make bahmni-logs           - View all Bahmni logs"
	@echo "  make bahmni-logs-openmrs   - View OpenMRS logs only"

# ============================================================================
# Infrastructure Commands
# ============================================================================

status:
	@./jss-infra-setup.sh status

setup-prerelease:
	@./jss-infra-setup.sh setup-prerelease "$(PRERELEASE_DB_PASS)"
	@./jss-infra-setup.sh wait-rds $(PRERELEASE_RDS_ID)
	@./jss-infra-setup.sh dns-rds-prerelease

cleanup-prerelease:
	@./jss-infra-setup.sh cleanup-prerelease

# ============================================================================
# SSH Access
# ============================================================================

ssh-prerelease:
	@ssh -i $(SSH_KEY) $(SSH_USER)@$(PRERELEASE_HOST)

# ============================================================================
# Database Backup/Restore Commands
# ============================================================================

copy-backup:
	@echo "Creating remote backup directory..."
	@ssh -i $(SSH_KEY) $(SSH_USER)@$(PRERELEASE_HOST) "mkdir -p $(REMOTE_BACKUP_DIR)"
	@echo "Copying backup file to EC2 (this may take a while)..."
	scp -i $(SSH_KEY) '$(BACKUP_FILE)' $(SSH_USER)@$(PRERELEASE_HOST):$(REMOTE_BACKUP_DIR)/openmrsdb_backup.sql.gz
	@echo "Backup file copied successfully!"
	@ssh -i $(SSH_KEY) $(SSH_USER)@$(PRERELEASE_HOST) "ls -lh $(REMOTE_BACKUP_DIR)/"

restore-backup:
	@echo "Restoring OpenMRS backup to RDS..."
	@ssh -i $(SSH_KEY) $(SSH_USER)@$(PRERELEASE_HOST) '\
		echo "Creating database if not exists..." && \
		mysql -h $(PRERELEASE_DB_HOST) -u $(PRERELEASE_DB_USER) -p"$(PRERELEASE_DB_PASS)" -e "CREATE DATABASE IF NOT EXISTS $(PRERELEASE_DB_NAME);" && \
		echo "Restoring backup (this will take several minutes)..." && \
		gunzip -c $(REMOTE_BACKUP_DIR)/openmrsdb_backup.sql.gz | mysql -h $(PRERELEASE_DB_HOST) -u $(PRERELEASE_DB_USER) -p"$(PRERELEASE_DB_PASS)" $(PRERELEASE_DB_NAME) && \
		echo "Verifying restore..." && \
		mysql -h $(PRERELEASE_DB_HOST) -u $(PRERELEASE_DB_USER) -p"$(PRERELEASE_DB_PASS)" $(PRERELEASE_DB_NAME) -e "SELECT COUNT(*) as table_count FROM information_schema.tables WHERE table_schema = \"$(PRERELEASE_DB_NAME)\";" && \
		echo "Restore completed successfully!"'

create-snapshot:
	@echo "Creating RDS snapshot..."
	@aws rds create-db-snapshot \
		--db-instance-identifier $(PRERELEASE_RDS_ID) \
		--db-snapshot-identifier $(PRERELEASE_RDS_ID)-baseline-$$(date +%Y%m%d) \
		--region $(AWS_REGION)
	@echo "Snapshot creation initiated. Check status with: aws rds describe-db-snapshots --db-snapshot-identifier $(PRERELEASE_RDS_ID)-baseline-$$(date +%Y%m%d)"

full-restore: copy-backup restore-backup create-snapshot
	@echo ""
	@echo "=========================================="
	@echo "  Full Restore Complete!"
	@echo "=========================================="
	@echo "Database: $(PRERELEASE_DB_NAME)"
	@echo "RDS Host: $(PRERELEASE_DB_HOST)"
	@echo "Snapshot: $(PRERELEASE_RDS_ID)-baseline-$$(date +%Y%m%d)"

# ============================================================================
# Bahmni Docker Commands
# ============================================================================

setup-bahmni:
	@echo "Setting up Bahmni Docker on EC2..."
	@ssh -i $(SSH_KEY) $(SSH_USER)@$(PRERELEASE_HOST) '\
		cd /home/ubuntu && \
		if [ ! -d "bahmni-docker" ]; then \
			git clone https://github.com/JanSwasthyaSahyog/bahmni-docker.git; \
		fi && \
		cd bahmni-docker && \
		git pull && \
		echo "Bahmni Docker repo ready. Configure .env file and run: make start-bahmni"'

start-bahmni:
	@ssh -i $(SSH_KEY) $(SSH_USER)@$(PRERELEASE_HOST) '\
		cd /home/ubuntu/bahmni-docker && \
		docker-compose up -d'

stop-bahmni:
	@ssh -i $(SSH_KEY) $(SSH_USER)@$(PRERELEASE_HOST) '\
		cd /home/ubuntu/bahmni-docker && \
		docker-compose down'

bahmni-logs:
	@ssh -i $(SSH_KEY) $(SSH_USER)@$(PRERELEASE_HOST) '\
		cd /home/ubuntu/bahmni-docker && \
		docker compose logs -f --tail=100'

bahmni-logs-openmrs:
	@ssh -i $(SSH_KEY) $(SSH_USER)@$(PRERELEASE_HOST) '\
		docker logs -f --tail=200 bahmni-docker-openmrs-1'

bahmni-status:
	@ssh -i $(SSH_KEY) $(SSH_USER)@$(PRERELEASE_HOST) '\
		cd /home/ubuntu/bahmni-docker && \
		docker compose ps'

bahmni-restart:
	@ssh -i $(SSH_KEY) $(SSH_USER)@$(PRERELEASE_HOST) '\
		cd /home/ubuntu/bahmni-docker && \
		docker compose --profile emr restart'

bahmni-down:
	@ssh -i $(SSH_KEY) $(SSH_USER)@$(PRERELEASE_HOST) '\
		cd /home/ubuntu/bahmni-docker && \
		docker compose --profile emr down'

bahmni-up:
	@ssh -i $(SSH_KEY) $(SSH_USER)@$(PRERELEASE_HOST) '\
		cd /home/ubuntu/bahmni-docker && \
		docker compose --profile emr up -d'

# Full Bahmni setup using scripts from this repo
bahmni-full-setup:
	@echo "Copying Bahmni setup files to EC2..."
	@scp -i $(SSH_KEY) -r bahmni/ $(SSH_USER)@$(PRERELEASE_HOST):/home/ubuntu/bahmni-setup/
	@echo "Running Bahmni setup script..."
	@ssh -i $(SSH_KEY) $(SSH_USER)@$(PRERELEASE_HOST) '\
		chmod +x /home/ubuntu/bahmni-setup/setup-bahmni.sh && \
		/home/ubuntu/bahmni-setup/setup-bahmni.sh prerelease'

# ============================================================================
# Utility Commands
# ============================================================================

check-ec2-setup:
	@echo "Checking EC2 setup status..."
	@ssh -i $(SSH_KEY) $(SSH_USER)@$(PRERELEASE_HOST) '\
		echo "=== Setup Status ===" && \
		cat /home/ubuntu/setup-complete.txt 2>/dev/null || echo "Setup not complete yet" && \
		echo "" && \
		echo "=== Docker Version ===" && \
		docker --version 2>/dev/null || echo "Docker not installed" && \
		echo "" && \
		echo "=== Docker Compose Version ===" && \
		docker-compose --version 2>/dev/null || echo "Docker Compose not installed" && \
		echo "" && \
		echo "=== MySQL Client ===" && \
		mysql --version 2>/dev/null || echo "MySQL client not installed" && \
		echo "" && \
		echo "=== Disk Space ===" && \
		df -h /'

test-db-connection:
	@echo "Testing database connection..."
	@ssh -i $(SSH_KEY) $(SSH_USER)@$(PRERELEASE_HOST) '\
		mysql -h $(PRERELEASE_DB_HOST) -u $(PRERELEASE_DB_USER) -p"$(PRERELEASE_DB_PASS)" -e "SELECT VERSION();" && \
		echo "Database connection successful!"'

verify-restore:
	@echo "Verifying database restore..."
	@ssh -i $(SSH_KEY) $(SSH_USER)@$(PRERELEASE_HOST) '\
		mysql -h $(PRERELEASE_DB_HOST) -u $(PRERELEASE_DB_USER) -p"$(PRERELEASE_DB_PASS)" $(PRERELEASE_DB_NAME) -e "\
			SELECT COUNT(*) as total_tables FROM information_schema.tables WHERE table_schema = \"$(PRERELEASE_DB_NAME)\"; \
			SHOW TABLES LIKE \"patient%\"; \
			SHOW TABLES LIKE \"person%\"; \
			SHOW TABLES LIKE \"obs%\"; \
			SELECT COUNT(*) as patient_count FROM patient; \
		"'
