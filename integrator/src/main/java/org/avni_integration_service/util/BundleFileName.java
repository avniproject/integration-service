package org.avni_integration_service.util;

public enum BundleFileName {
    MAPPING_TYPES("mappingTypes.json"),
    MAPPING_GROUPS("mappingGroups.json"),
    MAPPING_METADATA("mappingMetadata.json"),
    ERROR_TYPES("errorTypes.json"),
    INTEGRATION_SYSTEM_CONFIG("integrationSystemConfigs.json");

    private final String bundleFileName;

    public String getBundleFileName() {
        return bundleFileName;
    }

    BundleFileName(String bundleFileName) {
        this.bundleFileName = bundleFileName;
    }
}
