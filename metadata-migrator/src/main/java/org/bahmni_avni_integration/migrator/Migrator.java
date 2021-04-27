package org.bahmni_avni_integration.migrator;

import org.apache.log4j.Logger;
import org.bahmni_avni_integration.migrator.domain.OpenMRSForm;
import org.bahmni_avni_integration.migrator.service.AvniToBahmniService;
import org.bahmni_avni_integration.migrator.service.BahmniToAvniService;
import org.bahmni_avni_integration.migrator.service.IntegrationDataService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootApplication
@EnableJpaRepositories("org.bahmni_avni_integration.integration_data.repository")
@EntityScan(basePackages = {"org.bahmni_avni_integration.integration_data.*", "org.bahmni_avni_integration.integration_data.domain.*"})
@ComponentScan(basePackages = {"org.bahmni_avni_integration.integration_data", "org.bahmni_avni_integration.migrator.*"})
public class Migrator implements CommandLineRunner {
    private final BahmniToAvniService bahmniToAvniService;
    private final IntegrationDataService integrationDataService;
    private final AvniToBahmniService avniToBahmniService;

    private static final Logger logger = Logger.getLogger(Migrator.class);

    public Migrator(BahmniToAvniService bahmniToAvniService, IntegrationDataService integrationDataService, AvniToBahmniService avniToBahmniService) {
        this.bahmniToAvniService = bahmniToAvniService;
        this.integrationDataService = integrationDataService;
        this.avniToBahmniService = avniToBahmniService;
    }

    public static void main(String[] args) {
        SpringApplication.run(Migrator.class, args).close();
    }

    @Override
    public void run(String... args) throws Exception {
        List<String> nonSpringArguments = Arrays.stream(args).filter(s -> !s.startsWith("--")).collect(Collectors.toList());
        if (nonSpringArguments.size() == 0) return;
        switch (MigratorDirection.valueOf(nonSpringArguments.get(0))) {
            case Adhoc -> {
                runAdhoc();
            }
            case BahmniToAvni -> {
                bahmniToAvni();
            }
            case AvniToBahmni -> {
                avniToBahmni();
            }
        }

    }

    private void avniToBahmni() throws SQLException {
        logger.debug("Migrating metadata from Avni to Bahmni");
        avniToBahmniService.cleanup();
        integrationDataService.cleanup();

        avniToBahmniService.createStandardMetadata();
        avniToBahmniService.migrateForms();

        integrationDataService.createConstants();
        integrationDataService.createStandardMappings();

        System.exit(0);
    }

    private void bahmniToAvni() {
        try {
            bahmniToAvniService.cleanup();
            integrationDataService.cleanup();

            bahmniToAvniService.migratePatientAttributes();
            bahmniToAvniService.migrateConcepts();
            bahmniToAvniService.createStandardMetadata();
            bahmniToAvniService.migrateForms();

            integrationDataService.createConstants();
            integrationDataService.createStandardMappings();
            System.exit(0);
        } catch (Exception e) {
            logger.error("Migrator failed", e);
            System.exit(1);
        }
    }

    private void runAdhoc() throws SQLException {
        OpenMRSForm openMRSForm = new OpenMRSForm();
        openMRSForm.setFormId(13);
        openMRSForm.setFormName("Diagnosis (Hospital)");
        openMRSForm.setType("Encounter");
        bahmniToAvniService.migrateForms(Arrays.asList(openMRSForm));
        System.exit(0);
    }
}