include bahmni/bahmni.mk
include externalDB.mk
include util/util.mk

help:
	@IFS=$$'\n' ; \
	help_lines=(`fgrep -h "##" $(MAKEFILE_LIST) | fgrep -v fgrep | sed -e 's/\\$$//'`); \
	for help_line in $${help_lines[@]}; do \
	    IFS=$$'#' ; \
	    help_split=($$help_line) ; \
	    help_command=`echo $${help_split[0]} | sed -e 's/^ *//' -e 's/ *$$//'` ; \
	    help_info=`echo $${help_split[2]} | sed -e 's/^ *//' -e 's/ *$$//'` ; \
	    printf "%-30s %s\n" $$help_command $$help_info ; \
	done

SU:=$(shell id -un)
DB=avni_int
ADMIN_USER=avni_int
postgres_user := $(shell id -un)
application_jar=integrator-0.0.2-SNAPSHOT.jar
dbPort=5432

define _build_db
	-psql -h localhost -p $(dbPort) -U $(postgres_user) -d postgres -c "create user $(ADMIN_USER) with password 'password' createrole";
	-psql -h localhost -p $(dbPort) -U $(postgres_user) -d postgres -c 'create database $1 with owner $(ADMIN_USER)';
	-psql -h localhost -p $(dbPort) -U $(postgres_user) -d $1 -c 'create extension if not exists "uuid-ossp"';
endef

define _drop_db
    -psql -h localhost -p $(dbPort) -U $(SU) -d postgres -c "SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pg_stat_activity.datname = '$1' AND pid <> pg_backend_pid()"
    -psql -h localhost -p $(dbPort) -U $(SU) -d postgres -c 'drop database $1';
endef

_drop_roles:
	-psql -h localhost -p $(dbPort) -U $(postgres_user) -d postgres -c 'drop role avni_int';

define _run_server
	java -jar --enable-preview integrator/build/libs/$(application_jar) --app.cron.main="0/3 * * * * ?" --app.cron.full.error="0 1 * * * ?" --avni.api.url=https://staging.avniproject.org/ --avni.impl.username=test-user@bahmni_ashwini --avni.impl.password=password
endef

define _debug_server
	java -Xmx2048m -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -jar --enable-preview integrator/build/libs/$(application_jar) --app.cron.main="0/3 * * * * ?" --app.cron.full.error="0 1 * * * ?" --avni.api.url=https://staging.avniproject.org/ --avni.impl.username=test-user@bahmni_ashwini --avni.impl.password=password
endef


define _run_migrator
    . ./conf/local-test.conf
	java -jar --enable-preview metadata-migrator/build/libs/metadata-migrator-0.0.2-SNAPSHOT.jar run
endef

define _alert_success
	$(call _alert_message,Script Completed)
endef

######## DATABASE LOCAL
# hashed password when password is password = $2a$10$RipvsoEJg4PtXOExTjg7Eu2WzHH1SBntIkuR.bzmZeU2TrbQoFtMW
# kept here for emergency purposes as we are not developing the entire login functionality
rebuild-db: drop-db build-db

rebuild-db-schema: rebuild-db build-db-schema

rebuild-test-db-schema: rebuild-test-db build-test-db-schema

build-db:
	$(call _build_db,avni_int)

build-test-db:
	$(call _build_db,avni_int_test)

build-db-schema:
	./gradlew --stacktrace :integration-data:migrateDb
	psql -h localhost -p $(dbPort) -U avni_int -d avni_int < integration-data/src/main/resources/db/util/superadmin.sql;

build-test-db-schema:
	./gradlew --stacktrace :integration-data:migrateTestDb
	psql -h localhost -p $(dbPort) -U avni_int -d avni_int_test < integration-data/src/main/resources/db/util/superadmin.sql;

drop-db:
	$(call _drop_db,avni_int)

create-test-db:
	$(call _build_db,avni_int_test)

drop-test-db:
	$(call _drop_db,avni_int_test)

rebuild-test-db: drop-test-db build-test-db

drop-roles:
	-psql -h localhost -p $(dbPort) -U $(postgres_user) -d postgres -c 'drop role $(ADMIN_USER)';
#######

####### BUILD, TEST, LOCAL RUN
build-server: ## Builds the jar file
	./gradlew clean build -x test

setup-log-dir:
	-sudo mkdir /var/log/avni-int-service
	-sudo chown $(SU) /var/log/avni-int-service

start-server: build-db build-server
	$(call _run_server)

debug-server: build-db build-server
	$(call _debug_server)

run-server-without-background: build-server
	java -jar --enable-preview integrator/build/libs/$(application_jar) --app.cron.main="0 0 6 6 9 ? 2035" --avni.api.url=https://example.com/ --avni.impl.username=foo --avni.impl.password=bar

run-migrator: build-server
	$(call _run_migrator)

test-server-only:
	-touch amrit/src/test/resources/amrit-secret.properties
	-touch goonj/src/test/resources/goonj-secret.properties
	-touch goonj/src/test/resources/avni-secret.properties
	-touch lahi/src/test/resources/lahi-secret.properties
	-touch rwb/src/test/resources/rwb-secret.properties
	./gradlew clean build

test-server-starts:
	AVNI_INT_DATASOURCE=jdbc:postgresql://localhost:5432/avni_int_test AVNI_INT_AUTO_CLOSE=true java -jar --enable-preview integrator/build/libs/$(application_jar)

test-server: drop-test-db build-test-db test-server-only test-server-starts

setup-external-test-db: drop-test-db create-test-db
	sudo -u ${postgres_user} psql avni_int_test -f dump.sql

test-server-external: drop-test-db setup-external-test-db
	./gradlew clean build

open-test-results-integrator:
	open integrator/build/reports/tests/test/index.html

open-test-results-util:
	open util/build/reports/tests/test/index.html

open-test-results-bahmni:
	open bahmni/build/reports/tests/test/index.html

open-test-results-avni:
	open avni/build/reports/tests/test/index.html

open-test-results-goonj:
	open goonj/build/reports/tests/test/index.html

open-test-results-amrit:
	open amrit/build/reports/tests/test/index.html

open-test-results-lahi:
	open lahi/build/reports/tests/test/index.html

open-test-results-migrator:
	open metadata-migrator/build/reports/tests/test/index.html

open-test-results-integration-data:
	open integration-data/build/reports/tests/test/index.html
#######


####### Tunnels
tunnel-staging-db:
	ssh avni-int -L 6015:stagingdb.openchs.org:5432

tunnel-server-debug-vagrant:
	ssh -p 2222 -i ~/.vagrant.d/insecure_private_key vagrant@127.0.0.1 -L 6031:localhost:6031
#######


####### SOURCE CONTROL
tag-release:
ifndef version
	$(error ERROR: version not provided.)
endif
	git tag -a v$(version) -m "version $(version)"
	git push origin --tags
#######

####### Deployment
deploy-to-vagrant-only:
	echo vagrant | pbcopy
	scp -P 2222 -i ~/.vagrant.d/insecure_private_key integrator/build/libs/$(application_jar) root@127.0.0.1:/root/source/abi-host/

deploy-to-vagrant: build-server deploy-to-vagrant-only
#######

### Setup
setup: setup-log-dir
	touch goonj/src/test/resources/goonj-secret.properties
	touch goonj/src/test/resources/avni-secret.properties
	touch bahmni/src/test/resources/bahmni-secret.properties
	touch amrit/src/test/resources/amrit-secret.properties
	touch lahi/src/test/resources/lahi-secret.properties
	touch rwb/src/test/resources/rwb-secret.properties

create-test-db-extensions:
	-psql -h localhost -U avni_int -d avni_int_test -c 'create extension if not exists "uuid-ossp"';

generatePasswordHash:
ifndef password
	$(error ERROR: password not provided.)
endif
ifndef base_url
	$(error ERROR: base_url not provided.)
endif
	curl -d '{"password":"$(password)"}' -H "Content-Type: application/json" -X POST $(base_url)/int/test/passwordHash
