package org.avni_integration_service.service;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.MappingGroup;
import org.avni_integration_service.integration_data.domain.MappingMetaData;
import org.avni_integration_service.integration_data.domain.MappingType;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfig;
import org.avni_integration_service.integration_data.domain.error.ErrorType;
import org.avni_integration_service.integration_data.domain.bundle.BundleFile;
import org.avni_integration_service.integration_data.repository.ErrorTypeRepository;
import org.avni_integration_service.integration_data.repository.MappingGroupRepository;
import org.avni_integration_service.integration_data.repository.MappingMetaDataRepository;
import org.avni_integration_service.integration_data.repository.MappingTypeRepository;
import org.avni_integration_service.integration_data.repository.config.IntegrationSystemConfigRepository;
import org.avni_integration_service.util.BundleFileName;
import org.avni_integration_service.util.ObjectMapperSingleton;
import org.avni_integration_service.web.contract.ErrorTypeContract;
import org.avni_integration_service.web.contract.IntegrationSystemConfigContract;
import org.avni_integration_service.web.contract.MappingMetadataContract;
import org.avni_integration_service.web.contract.NamedEntityContract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class BundleService {
    private static final Logger logger = Logger.getLogger(BundleService.class);

    private final MappingMetaDataRepository mappingMetaDataRepository;
    private final MappingGroupRepository mappingGroupRepository;
    private final MappingTypeRepository mappingTypeRepository;
    private final ErrorTypeRepository errorTypeRepository;
    private final IntegrationSystemConfigRepository integrationSystemConfigRepository;
    private final MappingTypeService mappingTypeService;
    private final MappingGroupService mappingGroupService;
    private final MappingMetadataService mappingMetadataService;
    private final ErrorTypeService errorTypeService;
    private final IntegrationSystemConfigService integrationSystemConfigService;
    private final List<BundleFileName> fileSequence = new ArrayList<>() {{
        add(BundleFileName.MAPPING_TYPES);
        add(BundleFileName.MAPPING_GROUPS);
        add(BundleFileName.MAPPING_METADATA);
        add(BundleFileName.ERROR_TYPES);
        add(BundleFileName.INTEGRATION_SYSTEM_CONFIG);
    }};
    private static final int BUFFER_SIZE = 2048;
    private final ObjectMapper objectMapper;


    @Autowired
    public BundleService(MappingMetaDataRepository mappingMetaDataRepository, MappingGroupRepository mappingGroupRepository, MappingTypeRepository mappingTypeRepository, ErrorTypeRepository errorTypeRepository, IntegrationSystemConfigRepository integrationSystemConfigRepository, MappingTypeService mappingTypeService, MappingGroupService mappingGroupService, MappingMetadataService mappingMetadataService, ErrorTypeService errorTypeService, IntegrationSystemConfigService integrationSystemConfigService, ObjectMapper objectMapper) {
        this.mappingMetaDataRepository = mappingMetaDataRepository;
        this.mappingGroupRepository = mappingGroupRepository;
        this.mappingTypeRepository = mappingTypeRepository;
        this.errorTypeRepository = errorTypeRepository;
        this.integrationSystemConfigRepository = integrationSystemConfigRepository;
        this.mappingTypeService = mappingTypeService;
        this.mappingGroupService = mappingGroupService;
        this.mappingMetadataService = mappingMetadataService;
        this.errorTypeService = errorTypeService;
        this.integrationSystemConfigService = integrationSystemConfigService;
        this.objectMapper = objectMapper;
    }

    public void exportMappingMetadataAsJsonToZip(IntegrationSystem integrationSystem, ZipOutputStream zos) throws IOException {
        logger.debug("Processing MappingMetadata");
        List<MappingMetaData> mappingMetaDataList = mappingMetaDataRepository.findAllByIntegrationSystem(integrationSystem);
        List<MappingMetadataContract> mappingMetadataContracts = mappingMetaDataList.stream().map(MappingMetadataContract::fromMappingMetadata).toList();
        addFileToZip(zos, BundleFileName.MAPPING_METADATA.getBundleFileName(), mappingMetadataContracts);
    }

    public void exportMappingGroupAsJsonToZip(IntegrationSystem integrationSystem, ZipOutputStream zos) throws IOException {
        logger.debug("Processing MappingGroup");
        List<MappingGroup> mappingGroups = mappingGroupRepository.findAllByIntegrationSystem(integrationSystem);
        List<NamedEntityContract> mappingGroupContracts = mappingGroups.stream().map(NamedEntityContract::fromNamedEntity).toList();
        addFileToZip(zos, BundleFileName.MAPPING_GROUPS.getBundleFileName(), mappingGroupContracts);
    }

    public void exportMappingTypeAsJsonToZip(IntegrationSystem integrationSystem, ZipOutputStream zos) throws IOException {
        logger.debug("Processing MappingType");
        List<MappingType> mappingTypes = mappingTypeRepository.findAllByIntegrationSystem(integrationSystem);
        List<NamedEntityContract> mappingTypeContracts = mappingTypes.stream().map(NamedEntityContract::fromNamedEntity).toList();
        addFileToZip(zos, BundleFileName.MAPPING_TYPES.getBundleFileName(), mappingTypeContracts);
    }

    public void exportErrorTypeAsJsonToZip(IntegrationSystem integrationSystem, ZipOutputStream zos) throws IOException {
        logger.debug("Processing ErrorType");
        List<ErrorType> errorTypes = errorTypeRepository.findAllByIntegrationSystem(integrationSystem);
        List<ErrorTypeContract> errorTypeContracts = errorTypes.stream().map(ErrorTypeContract::fromErrorType).toList();
        addFileToZip(zos, BundleFileName.ERROR_TYPES.getBundleFileName(), errorTypeContracts);
    }

    public void exportIntegrationSystemConfigAsJsonToZip(IntegrationSystem integrationSystem, ZipOutputStream zos) throws IOException {
        logger.debug("Processing IntegrationSystemConfig");
        List<IntegrationSystemConfig> integrationSystemConfigs = integrationSystemConfigRepository.getAllByIntegrationSystem(integrationSystem);
        List<IntegrationSystemConfigContract> integrationSystemConfigContracts = integrationSystemConfigs
            .stream()
            .map(IntegrationSystemConfigContract::fromIntegrationSystemConfig).toList();
        addFileToZip(zos, BundleFileName.INTEGRATION_SYSTEM_CONFIG.getBundleFileName(), integrationSystemConfigContracts);
    }

    public byte[] exportBundle(IntegrationSystem currentIntegrationSystem) throws IOException {
        logger.info("Starting bundle export for " + currentIntegrationSystem.getName());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            exportMappingMetadataAsJsonToZip(currentIntegrationSystem, zos);
            exportMappingGroupAsJsonToZip(currentIntegrationSystem, zos);
            exportMappingTypeAsJsonToZip(currentIntegrationSystem, zos);
            exportErrorTypeAsJsonToZip(currentIntegrationSystem, zos);
            exportIntegrationSystemConfigAsJsonToZip(currentIntegrationSystem, zos);
        }
        logger.info("Completed bundle export for " + currentIntegrationSystem.getName());
        return baos.toByteArray();
    }

    public void importBundle(IntegrationSystem currentIntegrationSystem, MultipartFile bundleZip) throws IOException {
        logger.info("Starting bundle import for " + currentIntegrationSystem.getName());

        String bundleZipFilePath = copyBundleToTmp(bundleZip);
        Map<BundleFileName, BundleFile> bundleFiles = unzipBundle(bundleZipFilePath);

        for (BundleFileName fileName : fileSequence) {
            logger.debug("Checking bundle for " + fileName.getBundleFileName());
            if (bundleFiles.get(fileName) != null) {
                logger.debug("Found " + fileName.getBundleFileName());
                importFile(fileName, new String(bundleFiles.get(fileName).getContent(), StandardCharsets.UTF_8), currentIntegrationSystem);
            }
        }
        logger.info("Completed bundle import for " + currentIntegrationSystem.getName());
    }

    private String copyBundleToTmp(MultipartFile bundleZip) throws IOException {
        logger.debug("Started copyBundleToTmp");
        String filePath = "/tmp/" + UUID.randomUUID() + ".zip";
        File targetZip = new File(filePath);
        bundleZip.transferTo(targetZip);
        logger.debug("Copied input file to " + filePath);
        return filePath;
    }

    private Map<BundleFileName, BundleFile> unzipBundle(String zipFilePath) throws IOException {
        logger.debug("Unzipping " + zipFilePath);
        byte[] buffer = new byte[BUFFER_SIZE];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry zipEntry = zis.getNextEntry();
        Map<BundleFileName, BundleFile> bundleFiles = new HashMap<>();

        while (zipEntry != null) {
            BundleFileName bundleFileName = BundleFileName.fromString(zipEntry.getName());
            if (bundleFileName == null) {
                logger.error("Found unsupported fileName in Bundle. Skipping " + zipEntry.getName());
            } else {
                logger.debug("Unzipping " + zipEntry.getName());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                BundleFile bundleFile = new BundleFile(zipEntry.getName(), baos.toByteArray());
                baos.close();

                bundleFiles.put(bundleFileName, bundleFile);
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
        return bundleFiles;
    }

    public void importFile(BundleFileName fileName, String fileData, IntegrationSystem integrationSystem) throws IOException {
        logger.debug("Importing " + fileName.getBundleFileName());
        switch (fileName) {
            case MAPPING_TYPES -> {
                NamedEntityContract[] mappingTypeContracts = convertString(fileData, NamedEntityContract[].class);
                for (NamedEntityContract mappingTypeContract : mappingTypeContracts) {
                    mappingTypeService.createOrUpdateMappingType(mappingTypeContract, integrationSystem);
                }
            }
            case MAPPING_GROUPS -> {
                NamedEntityContract[] mappingGroupContracts = convertString(fileData, NamedEntityContract[].class);
                for (NamedEntityContract mappingGroupContract : mappingGroupContracts) {
                    mappingGroupService.createOrUpdateMappingGroup(mappingGroupContract, integrationSystem);
                }
            }
            case MAPPING_METADATA -> {
                MappingMetadataContract[] mappingMetadataContracts = convertString(fileData, MappingMetadataContract[].class);
                for (MappingMetadataContract mappingMetadataContract : mappingMetadataContracts) {
                    mappingMetadataService.createOrUpdateMappingMetadata(mappingMetadataContract, integrationSystem);
                }
            }
            case ERROR_TYPES -> {
                ErrorTypeContract[] errorTypeContracts = convertString(fileData, ErrorTypeContract[].class);
                for (ErrorTypeContract errorTypeContract : errorTypeContracts) {
                    errorTypeService.createOrUpdateErrorType(errorTypeContract, integrationSystem);
                }
            }
            case INTEGRATION_SYSTEM_CONFIG -> {
                IntegrationSystemConfigContract[] integrationSystemConfigContracts = convertString(fileData, IntegrationSystemConfigContract[].class);
                for (IntegrationSystemConfigContract integrationSystemConfigContract : integrationSystemConfigContracts) {
                    integrationSystemConfigService.createOrUpdateIntegrationSystemConfig(integrationSystemConfigContract, integrationSystem);
                }
            }
        }
        logger.debug("Imported " + fileName.getBundleFileName());
    }

    private <T> T convertString(String data, Class<T> convertTo) throws IOException {
        return objectMapper.readValue(data, convertTo);
    }

    private void addFileToZip(ZipOutputStream zos, String fileName, Object fileContent) throws IOException {
        logger.debug("Adding to zip " + fileName);
        ZipEntry entry = new ZipEntry(fileName);
        zos.putNextEntry(entry);
        if (fileContent != null) {
            PrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
            byte[] bytes = ObjectMapperSingleton.getObjectMapper().writer(prettyPrinter).writeValueAsBytes(fileContent);
            zos.write(bytes);
        }
        zos.closeEntry();
    }
}
