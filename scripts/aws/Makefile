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

.PHONY: help status ssh-prerelease bahmni-up bahmni-down bahmni-status bahmni-logs bahmni-logs-openmrs bahmni-restart create-ami check-ec2-setup openvpn-check openvpn-setup openvpn-edit-credentials openvpn-start openvpn-stop openvpn-restart openvpn-status openvpn-logs

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
	@echo "=== OpenVPN ==="
	@echo "  make openvpn-check         - Check OpenVPN installation and config"
	@echo "  make openvpn-setup         - Setup OpenVPN configuration from .ovpn file"
	@echo "  make openvpn-edit-credentials - Edit VPN username/password credentials"
	@echo "  make openvpn-start         - Start OpenVPN as background service"
	@echo "  make openvpn-stop          - Stop OpenVPN service"
	@echo "  make openvpn-restart       - Restart OpenVPN service"
	@echo "  make openvpn-status        - Show OpenVPN service and connection status"
	@echo "  make openvpn-logs          - View OpenVPN logs"
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

# ============================================================================
# OpenVPN Commands
# ============================================================================

openvpn-check:
	@echo "=== OpenVPN Configuration Check ===" && \
	echo "OpenVPN binary:" && \
	which openvpn && \
	echo "" && \
	echo "OpenVPN version:" && \
	openvpn --version | head -1 && \
	echo "" && \
	echo "Configuration files:" && \
	ls -la /opt/homebrew/etc/openvpn/ && \
	echo "" && \
	echo "Credentials file:" && \
	if [ -f "/opt/homebrew/etc/openvpn/credentials.txt" ]; then \
		if [ -s "/opt/homebrew/etc/openvpn/credentials.txt" ]; then \
			echo "Credentials file exists and has content"; \
		else \
			echo "WARNING: Credentials file exists but is empty"; \
		fi; \
	else \
		echo "WARNING: No credentials file found"; \
	fi && \
	echo "" && \
	echo "Service status:" && \
	brew services list | grep openvpn || echo "OpenVPN service not found"

openvpn-setup:
	@echo "=== Setting up OpenVPN Configuration ===" && \
	if [ -f "/opt/homebrew/etc/openvpn/JSS_Prod_himesh.ovpn" ]; then \
		echo "Source .ovpn file found" && \
		if [ -f "/opt/homebrew/etc/openvpn/openvpn.conf" ]; then \
			echo "Configuration already exists at openvpn.conf"; \
		else \
			echo "Creating openvpn.conf from JSS_Prod_himesh.ovpn" && \
			cp /opt/homebrew/etc/openvpn/JSS_Prod_himesh.ovpn /opt/homebrew/etc/openvpn/openvpn.conf; \
		fi && \
		if [ -f "/opt/homebrew/etc/openvpn/openvpn.conf" ]; then \
			if grep -q "auth-user-pass" /opt/homebrew/etc/openvpn/openvpn.conf && ! grep -q "auth-user-pass.*credentials.txt" /opt/homebrew/etc/openvpn/openvpn.conf; then \
				echo "Updating config to use credentials.txt file" && \
				sed -i '' 's/auth-user-pass/auth-user-pass \/opt\/homebrew\/etc\/openvpn\/credentials.txt/' /opt/homebrew/etc/openvpn/openvpn.conf; \
			fi; \
		fi && \
		if [ ! -f "/opt/homebrew/etc/openvpn/credentials.txt" ]; then \
			echo "Creating credentials template file" && \
			echo "# Add your JSS VPN username on first line, password on second line" > /opt/homebrew/etc/openvpn/credentials.txt && \
			echo "# Edit this file with your credentials before starting OpenVPN" >> /opt/homebrew/etc/openvpn/credentials.txt; \
		fi && \
		echo "Setting proper permissions for certificate files" && \
		sudo chmod 644 /opt/homebrew/etc/openvpn/JSS_Prod_himesh*.p12 /opt/homebrew/etc/openvpn/JSS_Prod_himesh*-tls.key 2>/dev/null || true && \
		sudo chown root:wheel /opt/homebrew/etc/openvpn/JSS_Prod_himesh*.p12 /opt/homebrew/etc/openvpn/JSS_Prod_himesh*-tls.key 2>/dev/null || true && \
		if [ -f "/opt/homebrew/etc/openvpn/credentials.txt" ]; then \
			sudo chmod 600 /opt/homebrew/etc/openvpn/credentials.txt && \
			sudo chown root:wheel /opt/homebrew/etc/openvpn/credentials.txt; \
		fi; \
	else \
		echo "ERROR: JSS_Prod_himesh.ovpn not found in /opt/homebrew/etc/openvpn/"; \
		exit 1; \
	fi

openvpn-start:
	@echo "Starting OpenVPN as background service..." && \
	sudo brew services start openvpn && \
	echo "OpenVPN service started. Check status with: make openvpn-status"

openvpn-stop:
	@echo "Stopping OpenVPN service..." && \
	sudo brew services stop openvpn && \
	echo "OpenVPN service stopped"

openvpn-restart:
	@echo "Restarting OpenVPN service..." && \
	sudo brew services restart openvpn && \
	echo "OpenVPN service restarted"

openvpn-status:
	@echo "=== OpenVPN Service Status ===" && \
	brew services list | grep openvpn && \
	echo "" && \
	echo "=== OpenVPN Process Status ===" && \
	ps aux | grep openvpn | grep -v grep || echo "No OpenVPN process running" && \
	echo "" && \
	echo "=== Network Interface Status ===" && \
	if ifconfig tun0 2>/dev/null | grep -q "inet"; then \
		echo "VPN interface found:" && \
		ifconfig tun0 | grep "inet"; \
	else \
		ifconfig | grep -A 5 tun || echo "No tun interface found - VPN not connected"; \
	fi

openvpn-logs:
	@echo "=== OpenVPN Logs ===" && \
	if [ -f "/opt/homebrew/var/log/openvpn.log" ]; then \
		tail -50 /opt/homebrew/var/log/openvpn.log; \
	else \
		echo "OpenVPN log file not found. Checking system logs..." && \
		log show --predicate 'process == "openvpn"' --last 1h || echo "No logs found"; \
	fi && \
	echo "" && \
	echo "=== Manual test command ===" && \
	echo "To test manually: sudo /opt/homebrew/sbin/openvpn --config /opt/homebrew/etc/openvpn/openvpn.conf --verb 3"

openvpn-edit-credentials:
	@echo "=== Editing OpenVPN Credentials ===" && \
	if [ -f "/opt/homebrew/etc/openvpn/credentials.txt" ]; then \
		echo "Opening credentials file for editing..." && \
		nano /opt/homebrew/etc/openvpn/credentials.txt || vim /opt/homebrew/etc/openvpn/credentials.txt || open -e /opt/homebrew/etc/openvpn/credentials.txt; \
	else \
		echo "Creating credentials file first..." && \
		echo "# Add your JSS VPN username on first line, password on second line" > /opt/homebrew/etc/openvpn/credentials.txt && \
		echo "# Edit this file with your credentials before starting OpenVPN" >> /opt/homebrew/etc/openvpn/credentials.txt && \
		nano /opt/homebrew/etc/openvpn/credentials.txt || vim /opt/homebrew/etc/openvpn/credentials.txt || open -e /opt/homebrew/etc/openvpn/credentials.txt; \
	fi && \
	echo "Setting secure permissions..." && \
	sudo chmod 600 /opt/homebrew/etc/openvpn/credentials.txt && \
	sudo chown root:wheel /opt/homebrew/etc/openvpn/credentials.txt && \
	echo "Credentials updated. Restart OpenVPN with: make openvpn-restart"
