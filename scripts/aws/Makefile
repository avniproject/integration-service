# JSS Avni-Bahmni Integration - AWS Infrastructure Makefile
# Usage: make <target>

# Configuration
SSH_KEY := ~/.ssh/openchs-infra.pem
SSH_USER := ubuntu
AWS_REGION := ap-south-1

# Prerelease Environment
PRERELEASE_HOST := jss-bahmni-prerelease.avniproject.org
PRERELEASE_INSTANCE_ID := i-0e128ab9da4c8d30f
PRERELEASE_DB_USER := root
PRERELEASE_DB_PASS := OpenMRS_JSS2024
PRERELEASE_DB_NAME := openmrs

# Backup file location (use quotes in commands, not escapes)
BACKUP_FILE := /Users/himeshr/integration-setup/JSS Avni Bahmni Integration/openmrsdb_backup.sql.gz
REMOTE_BACKUP_DIR := /home/ubuntu/backups

.PHONY: help status ssh-prerelease bahmni-up bahmni-down bahmni-status bahmni-logs bahmni-logs-openmrs bahmni-restart create-ami check-ec2-setup

help:
	@echo "JSS Avni-Bahmni AWS Infrastructure Commands"
	@echo ""
	@echo "=== SSH Access ==="
	@echo "  make ssh-prerelease        - SSH to prerelease EC2 instance"
	@echo ""
	@echo "=== Bahmni Docker ==="
	@echo "  make bahmni-status         - Show Bahmni container status"
	@echo "  make bahmni-up             - Start Bahmni Docker containers"
	@echo "  make bahmni-down           - Stop Bahmni Docker containers"
	@echo "  make bahmni-restart        - Restart Bahmni containers"
	@echo "  make bahmni-logs           - View all Bahmni logs"
	@echo "  make bahmni-logs-openmrs   - View OpenMRS logs only"
	@echo "  make bahmni-wait           - Wait for OpenMRS to be ready"
	@echo ""
	@echo "=== Infrastructure ==="
	@echo "  make status                - Show all JSS infrastructure status"
	@echo "  make check-ec2-setup       - Check Docker/MySQL on EC2"
	@echo "  make create-ami            - Create AMI snapshot of instance"

# ============================================================================
# Infrastructure Commands
# ============================================================================

status:
	@./jss-infra-setup.sh status

create-ami:
	@echo "Creating AMI snapshot of prerelease instance..."
	@aws ec2 create-image --instance-id $(PRERELEASE_INSTANCE_ID) \
		--name "bahmni-prerelease-$$(date +%Y%m%d-%H%M%S)" \
		--description "Bahmni prerelease snapshot" --no-reboot --region $(AWS_REGION)
	@echo "AMI creation initiated. Check status with: aws ec2 describe-images --owners self"

# ============================================================================
# SSH Access
# ============================================================================

ssh-prerelease:
	@ssh -i $(SSH_KEY) $(SSH_USER)@$(PRERELEASE_HOST)

# ============================================================================
# Bahmni Wait Command
# ============================================================================

bahmni-wait:
	@echo "Waiting for OpenMRS to be ready (this may take 10-15 minutes)..."
	@ssh -i $(SSH_KEY) $(SSH_USER)@$(PRERELEASE_HOST) '\
		while ! curl -s http://localhost:8080/openmrs/ws/rest/v1/session -u admin:test > /dev/null 2>&1; do \
			echo "Waiting for OpenMRS... $$(date +%H:%M:%S)"; \
			sleep 30; \
		done; \
		echo "OpenMRS is ready!"'

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
		if [ ! -d "jss-config" ]; then \
			git clone https://github.com/JanSwasthyaSahyog/jss-config.git; \
		fi && \
		cd bahmni-docker && \
		git pull && \
		echo "Bahmni Docker repo ready. Copy .env and docker-compose.override.yml, then run: make bahmni-up"'

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
	@echo "Testing database connection to local MySQL container..."
	@ssh -i $(SSH_KEY) $(SSH_USER)@$(PRERELEASE_HOST) '\
		docker exec bahmni-docker-openmrsdb-1 mysql -u $(PRERELEASE_DB_USER) -p"$(PRERELEASE_DB_PASS)" -e "SELECT VERSION();" && \
		echo "Database connection successful!"'

verify-restore:
	@echo "Verifying database restore..."
	@ssh -i $(SSH_KEY) $(SSH_USER)@$(PRERELEASE_HOST) '\
		docker exec bahmni-docker-openmrsdb-1 mysql -u $(PRERELEASE_DB_USER) -p"$(PRERELEASE_DB_PASS)" $(PRERELEASE_DB_NAME) -e "\
			SELECT COUNT(*) as total_tables FROM information_schema.tables WHERE table_schema = \"$(PRERELEASE_DB_NAME)\"; \
			SELECT COUNT(*) as patient_count FROM patient; \
		"'
