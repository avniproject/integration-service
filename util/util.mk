restore-prod-dump:
ifndef dumpFile
	@echo "Provde the dumpFile variable"
	exit 1
else
	$(call _drop_db,avni_int_prod)
	$(call _build_db,avni_int_prod)
	psql -U avni_int -d avni_int_prod < $(dumpFile)
endif
